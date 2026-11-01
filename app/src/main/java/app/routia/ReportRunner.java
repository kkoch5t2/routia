package app.routia;

import android.content.*;
import java.io.*;
import java.net.*;
import javax.net.ssl.HttpsURLConnection;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import org.json.*;

final class ReportRunner {
    interface Transport { JSONObject post(String path,String key,JSONObject body)throws IOException,JSONException; }
    private static final ExecutorService WORK=Executors.newSingleThreadExecutor();
    private static final AtomicBoolean INITIALIZED=new AtomicBoolean();
    static void initialize(Context context){
        if(INITIALIZED.compareAndSet(false,true))try(ReportStore db=new ReportStore(context)){db.recoverInterrupted();}
    }
    static void evaluate(Context context) {
        synchronized(ReportSettings.class){evaluateLocked(context);}
    }
    private static void evaluateLocked(Context context) {
        if(!ReportSettings.enabled(context))return;
        Store store=Store.get(context);LocalDate today=LocalDate.now();
        try(ReportStore db=new ReportStore(context)){
            for(Store.Habit h:store.habits()){
                if(!ReportSettings.active(context,h.id()))continue;
                LocalDate enabled=ReportSettings.enabledFrom(context,h.id());if(!enabled.isBefore(today))continue;
                LocalDate begin=Rules.week(today.minusDays(1)).minusDays(1);if(begin.isBefore(enabled))begin=enabled;
                LocalDate start=h.created().isAfter(begin)?h.created():begin;
                for(LocalDate day:Rules.penaltyDays(start,today,store.records(h.id(),start,today.minusDays(1)))){
                    if(!Rules.week(day).equals(Rules.week(today.minusDays(1))))continue;
                    String week=Rules.week(day).toString();
                    if(!db.hasWeek(h.id(),week))db.enqueue(h.id(),week,h.goal(),ReportData.snapshot(store,h,today,db).toString());
                }
            }
        }catch(JSONException ignored){/* No report is sent if facts could not be prepared. */}
    }
    static long enqueue(Context context,Store.Habit habit)throws JSONException{
        return enqueue(context,habit,ReportSettings.options(context));
    }
    static long enqueue(Context context,Store.Habit habit,ReportBudget.Options approved)throws JSONException{
        synchronized(ReportSettings.class){return enqueueLocked(context,habit,approved);}
    }
    private static long enqueueLocked(Context context,Store.Habit habit,ReportBudget.Options approved)throws JSONException{
        if(!ReportSettings.active(context,habit.id())||!approved.equals(ReportSettings.options(context)))
            throw new IllegalArgumentException("設定が変わりました。もう一度確認してください");
        try(ReportStore db=new ReportStore(context)){
            android.database.sqlite.SQLiteDatabase sql=db.getWritableDatabase();sql.beginTransaction();
            try{
                long id=db.enqueue(habit.id(),Rules.week(LocalDate.now()).toString(),habit.goal(),ReportData.snapshot(Store.get(context),habit,LocalDate.now(),db).toString());
                if(!db.confirmManual(id,approved))throw new IllegalArgumentException("今週分は作成済み、実行中、または送信結果が不明です");
                sql.setTransactionSuccessful();return id;
            }finally{sql.endTransaction();}
        }
    }
    static void work(Context context,Runnable finished) {
        Context app=context.getApplicationContext();initialize(app);
        WORK.execute(()->{
            try{
                evaluate(app);
                if(ReportSettings.enabled(app))try(ReportStore db=new ReportStore(app)){
                    for(int at=0;at<3;at++){ReportStore.Item item=db.next(ReportSettings.enabled(app));if(item==null)break;if(db.claim(item.id()))generate(app,db,item);}
                }
            }finally{app.sendBroadcast(new Intent("app.routia.CHANGED").setPackage(app.getPackageName()),app.getPackageName()+".INTERNAL");if(finished!=null)finished.run();}
        });
    }
    private static void generate(Context c,ReportStore db,ReportStore.Item item) {
        generate(c,db,item,ReportRunner::post);
    }
    static void generate(Context c,ReportStore db,ReportStore.Item item,Transport transport) {
        boolean submitted=false;double estimate=0;
        try{
            ReportBudget.Options options;ReportSettings.Credential credential;
            synchronized(ReportSettings.class){
                if(!ReportSettings.active(c,item.habit()))throw new IllegalArgumentException("この目標の継続レビューがオフ、または未接続です");
                options=ReportSettings.options(c);
                if(item.manual()&&(!item.model().equals(options.model())||item.budget()!=options.limitYen()||!item.detail().equals(options.detail().id)))
                    throw new IllegalArgumentException("確認後にモデル・上限・詳しさが変わったため停止しました。もう一度料金を確認してください");
                if(!db.recordOptions(item.id(),options))throw new IOException("レポートが取り消されました");
                credential=ReportSettings.credential(c);if(credential.value.isEmpty())throw new IOException("APIキーが未設定です");
            }
            JSONObject request=ReportData.request(new JSONObject(item.snapshot()),options);
            JSONObject count=transport.post("responses/input_tokens",credential.value,request);
            int input=count.getInt("input_tokens");
            ReportBudget.Reservation budget=ReportBudget.reserve(options,input,LocalDate.now());
            request.put("max_output_tokens",budget.maxOutput()).put("store",false).put("service_tier","default")
                .put("reasoning",new JSONObject().put("effort","medium").put("mode","standard"));
            synchronized(ReportSettings.class){
                if(!ReportSettings.active(c,item.habit())||!options.equals(ReportSettings.options(c))||!ReportSettings.currentCredential(c,credential))throw new IllegalArgumentException("設定が変わったため、有料生成前に停止しました");
                if(!db.sending(item.id(),budget.reservedYen().doubleValue()))throw new IOException("レポートが取り消されました");
            }
            // After this point the provider may charge, even if the connection drops. Never retry automatically.
            submitted=true;
            JSONObject response=transport.post("responses",credential.value,request);
            JSONObject usage=response.getJSONObject("usage");
            int usedInput=usage.getInt("input_tokens"),usedOutput=usage.getInt("output_tokens");
            if(usedInput<0||usedOutput<0||usedInput>input||usedOutput>budget.maxOutput())throw new IOException("利用量を確認できませんでした");
            JSONObject details=usage.optJSONObject("input_tokens_details");
            int writes=details==null?0:details.optInt("cache_write_tokens",0);
            estimate=ReportBudget.estimate(options.model(),usedInput,usedOutput,writes).doubleValue();
            if(estimate>budget.reservedYen().doubleValue()||!options.model().equals(response.optString("model"))||!"default".equals(response.optString("service_tier","default")))
                throw new IOException("料金またはモデルの確認が必要です");
            if(!"completed".equals(response.optString("status")))throw new IOException("生成が完了しませんでした");
            JSONObject result=new JSONObject(ReportData.text(response));complete(c,db,item.id(),result,estimate);
        }catch(Exception failure){
            String message=submitted?"生成結果を確認できなかったため停止しました。費用が発生している可能性があるため、自動再送はしません。":"接続・キー・料金情報を確認してください。有料生成は開始していません。";
            if(!submitted&&failure instanceof IllegalArgumentException)message=failure.getMessage();
            if(submitted)db.finish(item.id(),"stopped","",message,estimate);
            else db.stopCounting(item.id(),message);
        }
    }
    static void complete(Context c,ReportStore db,long id,JSONObject result,double estimate)throws JSONException{
        ReportStore.Item before=db.item(id);if(before==null)return;
        ReportData.validate(result,before.detail().isEmpty()?ReportDetail.STANDARD:ReportDetail.fromId(before.detail()));
        db.finish(id,"complete",result.toString(),"",estimate);
        ReportNotices.deliver(c,db,db.item(id));
        try{c.sendBroadcast(new Intent("app.routia.CHANGED").setPackage(c.getPackageName()),c.getPackageName()+".INTERNAL");}
        catch(RuntimeException ignored){/* A saved report remains complete even if UI refresh is unavailable. */}
    }
    private static JSONObject post(String path,String key,JSONObject body)throws IOException,JSONException {
        HttpsURLConnection connection=(HttpsURLConnection)new URL("https://api.openai.com/v1/"+path).openConnection();
        try{
            connection.setInstanceFollowRedirects(false);connection.setConnectTimeout(20000);connection.setReadTimeout(120000);
            connection.setRequestMethod("POST");connection.setRequestProperty("Authorization","Bearer "+key);connection.setRequestProperty("Content-Type","application/json");
            connection.setDoOutput(true);byte[] bytes=body.toString().getBytes(StandardCharsets.UTF_8);connection.setFixedLengthStreamingMode(bytes.length);
            try(OutputStream out=connection.getOutputStream()){out.write(bytes);}
            int status=connection.getResponseCode();if(status<200||status>=300)throw new IOException("API接続を確認してください ("+status+")");
            try(InputStream in=connection.getInputStream();ByteArrayOutputStream out=new ByteArrayOutputStream()){
                byte[] buffer=new byte[8192];int n;while((n=in.read(buffer))!=-1){if(out.size()+n>2_000_000)throw new IOException("Response too large");out.write(buffer,0,n);}
                return new JSONObject(out.toString(StandardCharsets.UTF_8.name()));
            }
        }finally{connection.disconnect();}
    }
    private ReportRunner(){}
}
