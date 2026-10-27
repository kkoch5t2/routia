package app.routia;

import android.app.*;
import android.content.*;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.os.*;
import android.view.*;
import android.widget.*;
import java.io.File;
import java.io.FileOutputStream;
import java.time.*;
import java.util.*;
import org.json.*;

final class ReportReview {
    final Instrumentation i;int n;
    ReportReview(Instrumentation i){this.i=i;}
    void check(boolean value,String label){n++;if(!value)throw new AssertionError(label);}
    static JSONObject fixture()throws JSONException{
        return new JSONObject("""
            {"headline":"走れた日の条件を次回にも",
             "insights":[{"title":"週末の記録に偏り","detail":"曜日ごとの件数に差があります。平日と週末で、使える時間が違う可能性があります。"}],
             "actions":[{"title":"最初の一歩を固定","cue":"帰宅して靴を脱いだら","step":"ランニング用の服と靴を用意する","effect":"始めるまでの負担が軽くなるか確認"}],
             "lenses":[{"name":"行動のきっかけ","detail":"状況と小さな行動をつなげる計画を試します。本人への効果は今後の記録で確認します。","source_id":"plan"}],
             "limits":["実際にやめた理由や時刻は記録されていません"]}
            """);
    }
    static JSONObject personalFixture()throws JSONException{
        JSONObject result=fixture();
        result.getJSONArray("actions").getJSONObject(0)
            .put("why","達成が続いた期間のあとに未記録があり、再開するときの準備負担を一度減らして確かめるため。")
            .put("cue","");
        return result;
    }
    View decor(Activity a){return a.getWindow().getDecorView();}
    View decor(Dialog dialog){return dialog.getWindow().getDecorView();}
    void acceptSettings(Activity a){
        AlertDialog dialog=((ReportSettingsActivity)a).consentDialog;
        check(dialog!=null&&dialog.isShowing(),"enabled save always asks for paid automation confirmation");
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).performClick();
    }
    int run()throws Exception{
        Context c=i.getTargetContext();
        if(!Build.HARDWARE.contains("ranchu")&&!Build.HARDWARE.contains("goldfish"))throw new AssertionError("Emulator only");
        check(c.getPackageName().endsWith(".reportqa"),"dedicated QA application");
        check(!ReportSettings.configured(c),"no live credentials in QA");
        ReportSettings.saveKey(c,"qa-only-not-valid-api-key");
        check(ReportSettings.readKey(c).equals("qa-only-not-valid-api-key"),"keystore round trip");
        check(!ReportSettings.prefs(c).getString("key_cipher","").contains("qa-only"),"no plaintext preference");
        ReportSettings.prefs(c).edit().clear().commit();
        reviewCharts(c);
        String name="routia-report-integration.db";c.deleteDatabase(name);
        try(Store s=new Store(c,name);ReportStore reports=new ReportStore(c,"routia-report-store-test.db")){
            long id=s.saveConfiguredHabit(0,"帰宅後","ダンス","少し踊る","練習する","振付を通す","check",0,"");
            JSONObject snapshot=ReportData.snapshot(s,s.habit(id),LocalDate.now(),reports);
            check(snapshot.getInt("total")==0&&snapshot.getInt("recent_eligible")==0,"new habit has no invented past");
            check(!snapshot.toString().contains("note"),"private notes not sent");
            ReportData.validate(fixture());n++;
            ReportData.validate(personalFixture());n++;
            JSONObject badReason=personalFixture();badReason.getJSONArray("actions").getJSONObject(0).put("why","長".repeat(91));
            try{ReportData.validate(badReason);throw new AssertionError("long rationale accepted");}catch(JSONException expected){n++;}

            JSONObject invalid=fixture();invalid.put("headline","長".repeat(71));
            try{ReportData.validate(invalid);throw new AssertionError("long field accepted");}catch(JSONException expected){n++;}
            invalid=fixture();invalid.getJSONArray("lenses").getJSONObject(0).put("source_id","invented-study");
            try{ReportData.validate(invalid);throw new AssertionError("fake source accepted");}catch(JSONException expected){n++;}
            String week=Rules.week(LocalDate.now()).toString();
            long row=reports.enqueue(id,week,"ダンス",snapshot.toString());
            check(row==reports.enqueue(id,week,"ダンス",snapshot.toString()),"idempotent habit/week");
            check(reports.next(false)==null,"disabled automatic queue stays idle");
            reports.enqueue(id,week,"ダンス",snapshot.toString(),true);
            check(reports.next(false).id()==row,"manual request explicitly selects existing queued item");
            check(reports.claim(row)&&!reports.claim(row),"single sender");
            check(reports.recordOptions(row,new ReportBudget.Options("gpt-6-astra",750)),"options persisted before sending");
            check(reports.item(row).model().equals("gpt-6-astra")&&reports.item(row).budget()==750,"report keeps original options");
            reports.sending(row,220);
            check(!reports.confirmManual(row,new ReportBudget.Options("gpt-6.1-sol",300)),"submitted paid request cannot be confirmed for another submission");
            reports.recoverInterrupted();
            check(reports.item(row).state().equals("stopped")&&!reports.retryUnsent(row),"unknown paid request never retried");
            long unsent=reports.enqueue(id,"next-week","ダンス",snapshot.toString());reports.claim(unsent);reports.finish(unsent,"stopped","","not sent",0);
            check(reports.retryUnsent(unsent),"preflight can be retried without paid duplicate");
            reports.dropHabit(id);check(reports.all(id).isEmpty(),"delete removes snapshots");
        }
        c.deleteDatabase(name);c.deleteDatabase("routia-report-store-test.db");
        Store store=Store.get(c);check(store.habits().isEmpty(),"fresh QA app only");
        LocalDate today=LocalDate.now();
        long id=store.saveConfiguredHabit(0,"帰宅後","表示テスト・ランニング","着替える","走る","少し長く走る","count",3,"km");
        store.getWritableDatabase().execSQL("UPDATE habits SET created=? WHERE id=?",new Object[]{today.minusDays(35).toString(),id});
        for(int days=35;days>=0;days--){
            LocalDate day=today.minusDays(days);
            if(days%6<4){store.getWritableDatabase().execSQL("INSERT INTO records(habit_id,day,level,amount,unit,note) VALUES(?,?,2,3,'km','')",new Object[]{id,day.toString()});}
        }
        try(ReportStore reportStore=new ReportStore(c)){
            JSONObject snapshot=ReportData.snapshot(store,store.habit(id),today,reportStore);
            long report=reportStore.enqueue(id,Rules.week(today).toString(),store.habit(id).goal(),snapshot.toString());
            reportStore.finish(report,"complete",personalFixture().toString(),"",0);
        }
        Activity activity=i.startActivitySync(ReportActivity.intent(c,id).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        i.waitForIdleSync();
        for(String tab:new String[]{"要点","傾向","改善策","根拠"}){
            View target=find(decor(activity),tab);check(target!=null,"tab "+tab);
            i.runOnMainSync(target::performClick);i.waitForIdleSync();SystemClock.sleep(100);
            check(find(decor(activity),tab).isSelected(),"selection "+tab);
            shot(activity,tab);
            if(tab.equals("傾向")){
                check(find(decor(activity),"📈 取り組み量の推移")!=null,"numeric history uses a different visualization from weekdays");
                check(findStartsWith(decor(activity),"直近28日 · ")!=null,"line chart states its 28-day display window");
                ScrollView screen=(ScrollView)findType(decor(activity),ScrollView.class);
                i.runOnMainSync(()->screen.scrollTo(0,Ui.dp(c,600)));i.waitForIdleSync();shot(activity,"quantity-trend");
            }
        }
        check(find(decor(activity),"研究を本人の記録に当てはめた仮説")!=null,"hypothesis label");
        i.runOnMainSync(activity::finish);i.waitForIdleSync();
        AppColors.save(c,6);
        Activity dark=i.startActivitySync(ReportActivity.intent(c,id).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        i.waitForIdleSync();
        check(AppColors.black(dark),"black theme retained");
        View action=find(decor(dark),"改善策");
        i.runOnMainSync(action::performClick);i.waitForIdleSync();
        check(find(decor(dark),"ランニング用の服と靴を用意する")!=null,"flow survives dark theme");
        shot(dark,"black");
        i.runOnMainSync(dark::finish);AppColors.save(c,0);i.waitForIdleSync();
        reviewPriceGuides(c);
        reviewSettings(c,id);
        reviewEnabledSave(c,id);
        reviewManualConsent(c);
        reviewMigration(c);
        reviewDetail(c,id);
        reviewMainUi(c,id);
        reviewPageState(c,id);
        reviewCalendars(c);
        return n;
    }
    void reviewPriceGuides(Context c)throws Exception{
        Activity a=i.startActivitySync(ReportSettingsActivity.intent(c).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));i.waitForIdleSync();
        String[] ids={"gpt-6-luna","gpt-6.1-sol","gpt-6-astra"};
        String[][] labels={{"約0.3〜1.1円 / 回","約0.4〜1.2円 / 回","約0.6〜1.5円 / 回"},
            {"約6〜21円 / 回","約7〜24円 / 回","約11〜30円 / 回"},
            {"約30〜105円 / 回","約36〜120円 / 回","約56〜150円 / 回"}};
        ScrollView initialScroll=(ScrollView)findType(decor(a),ScrollView.class);
        i.runOnMainSync(()->{
            initialScroll.scrollTo(0,((View)findTag(decor(a),"gpt-6-luna").getParent().getParent()).getTop());
            for(String id:ids)findTag(decor(a),id).jumpDrawablesToCurrentState();
        });i.waitForIdleSync();SystemClock.sleep(300);shot(a,"model-all-depths");
        for(int model=0;model<ids.length;model++)for(ReportDetail depth:ReportDetail.values()){
            final String id=ids[model];final int mi=model;
            i.runOnMainSync(()->{findTag(decor(a),id).performClick();findTag(decor(a),depth.id).performClick();});i.waitForIdleSync();
            for(int other=0;other<ids.length;other++){
                String text=((TextView)findTag(decor(a),ids[other])).getText().toString();
                String[] overall={"約0.3〜1.5円 / 回","約6〜30円 / 回","約30〜150円 / 回"};
                check(text.contains(overall[other]),"model guides always span every depth");
            }
            for(ReportDetail other:ReportDetail.values()){
                String text=((TextView)findTag(decor(a),other.id)).getText().toString();
                check(text.contains(labels[mi][other.ordinal()]),"all depth guides track model");
            }
            check(Arrays.stream(ids).filter(x->((RadioButton)findTag(decor(a),x)).isChecked()).count()==1,"price comparison retains one selected model");
            check(Arrays.stream(ReportDetail.values()).filter(x->((RadioButton)findTag(decor(a),x.id)).isChecked()).count()==1,"price comparison retains one selected depth");
            check(findStartsWith(decor(a),"金額の目安（仮条件）："+labels[mi][depth.ordinal()])!=null,"selected cost preview stays synchronized");
            check(ReportSettings.options(c).equals(ReportBudget.defaults()),"previewing scenarios never persists drafts");
        }
        check(findStartsWith(decor(a),"実際の利用量は未計測です。")!=null,"unmeasured scenario is not presented as a guaranteed price interval");
        ScrollView sc=(ScrollView)findType(decor(a),ScrollView.class);
        View choice=findTag(decor(a),"detailed");
        i.runOnMainSync(()->sc.scrollTo(0,((View)choice.getParent().getParent()).getTop()));i.waitForIdleSync();
        i.runOnMainSync(()->{
            for(String id:ids)findTag(decor(a),id).jumpDrawablesToCurrentState();
            for(ReportDetail d:ReportDetail.values())findTag(decor(a),d.id).jumpDrawablesToCurrentState();
        });
        SystemClock.sleep(400);shot(a,"price-detail");
        final AlertDialog[] dialog={null};
        ReportBudget.Options detailed=new ReportBudget.Options("gpt-6-astra",300,ReportDetail.DETAILED);
        i.runOnMainSync(()->dialog[0]=ReportConsent.manual(a,"表示テスト",detailed,()->{throw new AssertionError("paid work not expected");}));i.waitForIdleSync();
        String message=((TextView)dialog[0].findViewById(android.R.id.message)).getText().toString();
        check(message.contains("約56〜150円 / 回")&&message.contains("詳細")&&message.contains("この幅を超える"),"manual consent uses same selected scenario and uncertainty");
        i.runOnMainSync(()->{dialog[0].dismiss();a.finish();});i.waitForIdleSync();
        check(ReportSettings.options(c).equals(ReportBudget.defaults()),"closing comparison leaves settings unchanged");
    }
    void reviewSettings(Context c,long id)throws Exception{
        ReportSettings.setFeatureEnabled(c,true);
        Store store=Store.get(c);long second=store.saveConfiguredHabit(0,"朝","表示テスト・ダンス","一歩","踊る","通す","check",0,"");
        check(!ReportSettings.habitEnabled(c,id)&&!ReportSettings.habitEnabled(c,second),"goals default off");
        Activity a=i.startActivitySync(ReportSettingsActivity.intent(c).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));i.waitForIdleSync();
        check((((ReportSettingsActivity)a).getWindow().getAttributes().flags&WindowManager.LayoutParams.FLAG_SECURE)!=0,"key dialog protected");
        check(countInputs(decor(a))==1,"one shared API key input");
        check(find(decor(a),"継続レビュー")!=null,"purpose first settings title");
        SeekBar slider=(SeekBar)findDescription(decor(a),"1回の上限金額");
        check(slider!=null&&slider.getMax()==18,"100 to 1000 in 50 yen steps");
        check(find(decor(a),"💴 1週間分のレビュー予算")!=null,"weekly budget prominent card");

        i.runOnMainSync(()->{findTag(decor(a),"gpt-6-astra").performClick();slider.setProgress(18);((Switch)find(decor(a),store.habit(id).goal())).setChecked(true);});i.waitForIdleSync();
        check(find(decor(a),"上限 1,000円 / 回")!=null,"slider refreshes amount");
        check(find(decor(a),"最大 1,000円相当")!=null,"weekly total tracks slider and target");
        check(find(decor(a),"1回 1000円 × 1目標 × 週1回")!=null,"weekly formula visible");
        check(find(decor(a),"1週間分の予算：1,000円相当（1目標）")!=null,"weekly total pinned beside save");

        SystemClock.sleep(350);shot(a,"settings-top");
        ScrollView scroll=(ScrollView)findType(decor(a),ScrollView.class);
        i.runOnMainSync(()->scroll.smoothScrollTo(0,1000));SystemClock.sleep(250);shot(a,"settings-budget");
        i.runOnMainSync(()->{
            findTag(decor(a),"gpt-6.1-sol").performClick();slider.setProgress(7);
            ((Switch)find(decor(a),store.habit(id).goal())).setChecked(true);
            ((Switch)find(decor(a),store.habit(second).goal())).setChecked(false);
            find(decor(a),"この設定を保存").performClick();
        });i.waitForIdleSync();
        check(!ReportSettings.habitEnabled(c,id),"settings not written before consent");
        check(!ReportSettings.options(c).equals(new ReportBudget.Options("gpt-6.1-sol",450)),"draft cost not applied before consent");
        i.runOnMainSync(()->acceptSettings(a));i.waitForIdleSync();
        check(ReportSettings.options(c).equals(new ReportBudget.Options("gpt-6.1-sol",450)),"selected shared options saved");
        check(ReportSettings.habitEnabled(c,id)&&!ReportSettings.habitEnabled(c,second),"goals independently enabled");
        check(ReportSettings.enabledFrom(c,id).equals(LocalDate.now().plusDays(1)),"activation starts tomorrow");
        check(!ReportSettings.enabled(c)&&!ReportSettings.configured(c),"no key means no paid work");
        for(ReportBudget.Model m:ReportBudget.MODELS){
            JSONObject request=ReportData.request(new JSONObject(),m.id());
            check(request.getString("model").equals(m.id())&&request.getJSONObject("text").getJSONObject("format").getBoolean("strict"),"selected model and strict format");
        }
        try{ReportRunner.enqueue(c,store.habit(second));throw new AssertionError("off goal enqueued");}catch(IllegalArgumentException expected){n++;}
        ReportSettings.saveOptions(c,ReportSettings.options(c),Collections.singletonMap(id,false));
        check(!ReportSettings.habitEnabled(c,id),"turn off persists");
        try(ReportStore db=new ReportStore(c)){
            long row=db.enqueue(second,"future-week",store.habit(second).goal(),"{}");db.claim(row);db.recordOptions(row,new ReportBudget.Options("gpt-6-astra",1000));
            db.cancelUnsent(second);
            check(db.item(row).state().equals("stopped")&&db.item(row).reserved()==0,"off cancels unpaid counting");
            check(!db.sending(row,1000),"cancelled preflight cannot send");db.dropHabit(second);
        }
        Activity cancel=i.startActivitySync(ReportSettingsActivity.intent(c).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));i.waitForIdleSync();
        i.runOnMainSync(()->{findTag(decor(cancel),"gpt-6-astra").performClick();cancel.finish();});i.waitForIdleSync();
        check(ReportSettings.options(c).model().equals("gpt-6.1-sol"),"cancel does not save draft");
        Activity manual=i.startActivitySync(ReportActivity.intent(c,second).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));i.waitForIdleSync();
        View button=find(decor(manual),"今週のレポートを作る（有料）");
        check(button!=null,"manual entry visible without key or goal enabled");
        check(find(decor(manual),"APIキーを登録し、この目標をオンにしてください。")!=null,"missing prerequisites explained");
        shot(manual,"manual-entry");i.runOnMainSync(manual::finish);i.waitForIdleSync();

        AppColors.save(c,6);Activity dark=i.startActivitySync(ReportSettingsActivity.intent(c).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));i.waitForIdleSync();
        check(AppColors.black(dark)&&find(decor(dark),"継続レビュー")!=null,"shared settings retain dark palette");
        shot(dark,"settings-black");i.runOnMainSync(dark::finish);AppColors.save(c,0);
    }
    void reviewEnabledSave(Context c,long id)throws Exception{
        Activity a=i.startActivitySync(ReportSettingsActivity.intent(c).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));i.waitForIdleSync();
        i.runOnMainSync(()->{
            ((EditText)findType(decor(a),EditText.class)).setText("qa-only-not-valid-api-key");
            ((Switch)find(decor(a),Store.get(c).habit(id).goal())).setChecked(true);
            find(decor(a),"この設定を保存").performClick();
        });i.waitForIdleSync();
        check(!ReportSettings.configured(c)&&!ReportSettings.habitEnabled(c,id),"key and goal are not saved before accepting fees");
        AlertDialog first=((ReportSettingsActivity)a).consentDialog;
        check(first!=null&&first.isShowing(),"first enabled save asks for cost consent");
        String message=((TextView)first.findViewById(android.R.id.message)).getText().toString();
        check(message.contains("API利用料が発生")&&message.contains("450円")&&message.contains(Store.get(c).habit(id).goal()),"save warning contains charge, amount and target");
        shotDialog(first,"settings-fee-confirmation");
        i.runOnMainSync(()->first.getButton(AlertDialog.BUTTON_NEGATIVE).performClick());i.waitForIdleSync();
        check(!a.isFinishing()&&!ReportSettings.configured(c)&&!ReportSettings.habitEnabled(c,id),"cancel leaves credentials and goal unchanged");
        i.runOnMainSync(()->find(decor(a),"この設定を保存").performClick());i.waitForIdleSync();
        i.runOnMainSync(()->acceptSettings(a));i.waitForIdleSync();
        check(a.isFinishing(),"API key and enabled goal save successfully together");
        check(ReportSettings.configured(c)&&ReportSettings.habitEnabled(c,id),"key and target remain saved");
        android.app.job.JobScheduler scheduler=c.getSystemService(android.app.job.JobScheduler.class);
        android.app.job.JobInfo job=scheduler.getPendingJob(3703);
        check(job!=null&&job.isPersisted()&&job.getNetworkType()==android.app.job.JobInfo.NETWORK_TYPE_ANY,"enabled save schedules persisted network job");
        Activity reopened=i.startActivitySync(ReportSettingsActivity.intent(c).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));i.waitForIdleSync();
        check(find(decor(reopened),"接続キー：保存済み")!=null,"saved key status survives reopening");
        check(((EditText)findType(decor(reopened),EditText.class)).getText().length()==0,"saved key is never redisplayed");
        i.runOnMainSync(()->find(decor(reopened),"この設定を保存").performClick());i.waitForIdleSync();
        check(!reopened.isFinishing(),"unchanged enabled settings still require confirmation on every save");
        i.runOnMainSync(()->acceptSettings(reopened));i.waitForIdleSync();
        check(reopened.isFinishing(),"enabled settings save again with blank replacement key");
        check(ReportSettings.readKey(c).equals("qa-only-not-valid-api-key"),"blank replacement preserves encrypted key");
        Activity via=i.startActivitySync(ReportSettingsActivity.intent(c).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));i.waitForIdleSync();
        i.runOnMainSync(()->find(decor(via),"保存してレビューを開く").performClick());i.waitForIdleSync();
        check(!via.isFinishing()&&((ReportSettingsActivity)via).consentDialog!=null,"save-and-open route also asks for fees");
        i.runOnMainSync(()->((ReportSettingsActivity)via).consentDialog.getButton(AlertDialog.BUTTON_NEGATIVE).performClick());i.waitForIdleSync();
        check(!via.isFinishing(),"cancel save-and-open stays on form");
        ReportSettings.saveOptions(c,ReportSettings.options(c),Collections.singletonMap(id,false));
        check(scheduler.getPendingJob(3703)==null,"disabling last target cancels scheduled job");
        i.runOnMainSync(via::finish);i.waitForIdleSync();
        Activity off=i.startActivitySync(ReportSettingsActivity.intent(c).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));i.waitForIdleSync();
        i.runOnMainSync(()->find(decor(off),"この設定を保存").performClick());i.waitForIdleSync();
        check(off.isFinishing()&&((ReportSettingsActivity)off).consentDialog==null,"all goals off save without paid automation confirmation");
        ReportSettings.prefs(c).edit().clear().commit();
    }
    void reviewManualConsent(Context c)throws Exception{
        ReportSettings.setFeatureEnabled(c,true);
        Store.Habit habit=Store.get(c).habits().get(1);
        ReportSettings.saveKey(c,"qa-only-not-valid-api-key");
        ReportSettings.saveOptions(c,new ReportBudget.Options("gpt-6.1-sol",300),Collections.singletonMap(habit.id(),true));
        Activity a=i.startActivitySync(ReportActivity.intent(c,habit.id()).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));i.waitForIdleSync();
        check(find(decor(a),"AIのアドバイスはまだありません")!=null,"uncreated report does not masquerade as AI advice");
        check(find(decor(a),"🤖 AIが生成したアドバイス")==null,"no AI badge before generation");
        for(String tab:new String[]{"改善策","根拠"}){
            i.runOnMainSync(()->find(decor(a),tab).performClick());i.waitForIdleSync();
            check(find(decor(a),"AIのアドバイスはまだありません")!=null,"no canned advice on uncreated "+tab);
        }
        i.runOnMainSync(()->find(decor(a),"要点").performClick());i.waitForIdleSync();
        i.runOnMainSync(()->find(decor(a),"今週のレポートを作る（有料）").performClick());i.waitForIdleSync();
        AlertDialog first=((ReportActivity)a).consentDialog;
        check(first!=null&&first.isShowing(),"manual generate requires confirmation");
        String message=((TextView)first.findViewById(android.R.id.message)).getText().toString();
        check(message.contains("API利用料が発生")&&message.contains("300円")&&message.contains("GPT-6.1 Sol"),"manual warning states charge, approved cap and model");
        try(ReportStore db=new ReportStore(c)){check(db.all(habit.id()).isEmpty(),"no request queued while confirmation is open");}
        shotDialog(first,"manual-fee-confirmation");
        i.runOnMainSync(()->first.getButton(AlertDialog.BUTTON_NEGATIVE).performClick());i.waitForIdleSync();
        try(ReportStore db=new ReportStore(c)){check(db.all(habit.id()).isEmpty(),"cancel manual generation queues nothing");}
        i.runOnMainSync(()->find(decor(a),"今週のレポートを作る（有料）").performClick());i.waitForIdleSync();
        i.runOnMainSync(()->((ReportActivity)a).consentDialog.getButton(AlertDialog.BUTTON_POSITIVE).performClick());i.waitForIdleSync();
        try(ReportStore db=new ReportStore(c)){
            List<ReportStore.Item> items=db.all(habit.id());check(items.size()==1,"accepted manual request queued exactly once");
            ReportStore.Item job=items.get(0);
            check(job.manual()&&job.model().equals("gpt-6.1-sol")&&job.budget()==300,"manual job keeps confirmed model and cap");
            check(job.reserved()==0,"offline QA never reaches paid submission");
        }
        ReportSettings.saveOptions(c,ReportSettings.options(c),Collections.singletonMap(habit.id(),false));
        i.runOnMainSync(a::finish);ReportSettings.prefs(c).edit().clear().commit();i.waitForIdleSync();
    }
    void shotDialog(AlertDialog dialog,String name)throws Exception{
        File file=new File(i.getTargetContext().getExternalFilesDir(null),"report-"+name+".png");
        final Bitmap[] frame={null};i.runOnMainSync(()->{View v=decor(dialog);frame[0]=Bitmap.createBitmap(v.getWidth(),v.getHeight(),Bitmap.Config.ARGB_8888);v.draw(new android.graphics.Canvas(frame[0]));});
        try(FileOutputStream out=new FileOutputStream(file)){frame[0].compress(Bitmap.CompressFormat.PNG,100,out);}frame[0].recycle();
    }
    void reviewCharts(Context c)throws Exception{
        JSONObject snapshot=new JSONObject().put("created","2026-10-24").put("as_of","2026-10-25")
            .put("kind","time").put("unit","分").put("total",2)
            .put("weekday_done",new JSONArray(new int[]{0,0,0,0,0,1,0}))
            .put("weekday_eligible",new JSONArray(new int[]{0,0,0,0,0,1,0}))
            .put("recent_days",new JSONArray()
                .put(new JSONObject().put("day","2026-10-24").put("level",2).put("amount",20).put("unit","分"))
                .put(new JSONObject().put("day","2026-10-25").put("level",2).put("amount",30).put("unit","分")));
        ReportChartData data=ReportVisuals.chartData(snapshot);
        check(data!=null&&data.weekdayDone[6]==1&&data.weekdayEligible[6]==1,"old snapshot weekday arrays cannot hide completed Sunday");
        check(data.measured==2&&data.amountTotal==50,"quantity series keeps both Oct 24 and Oct 25");
        final ReportVisuals[] view={null};
        i.runOnMainSync(()->view[0]=new ReportVisuals(c,snapshot,ReportVisuals.SERIES));
        check(view[0].getContentDescription().toString().contains("10/25：30分"),"accessible curve exposes Sunday's real measurement");
        shotChart(view[0],"two-day-quantity");
        AppColors.save(c,6);i.runOnMainSync(()->view[0]=new ReportVisuals(c,snapshot,ReportVisuals.SERIES));
        shotChart(view[0],"two-day-quantity-black");AppColors.save(c,0);
        snapshot.put("kind","check");
        ReportChartData counts=ReportVisuals.chartData(snapshot);
        check(!counts.numeric&&counts.value(counts.days.get(1))==2,"check series is cumulative actual achievement");
        i.runOnMainSync(()->view[0]=new ReportVisuals(c,snapshot,ReportVisuals.SERIES));
        shotChart(view[0],"two-day-cumulative");
        JSONObject malformed=new JSONObject(snapshot.toString()).put("as_of","not-a-date");
        check(ReportVisuals.chartData(malformed)==null,"invalid dates fail closed rather than crash or invent a date");
    }
    void shotChart(ReportVisuals view,String name)throws Exception{
        File file=new File(i.getTargetContext().getExternalFilesDir(null),"report-"+name+".png");final Bitmap[] frame={null};
        i.runOnMainSync(()->{
            int w=Ui.dp(i.getTargetContext(),300),h=Ui.dp(i.getTargetContext(),160);
            view.measure(View.MeasureSpec.makeMeasureSpec(w,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(h,View.MeasureSpec.EXACTLY));view.layout(0,0,w,h);
            frame[0]=Bitmap.createBitmap(w,h,Bitmap.Config.ARGB_8888);
            android.graphics.Canvas canvas=new android.graphics.Canvas(frame[0]);canvas.drawColor(Ui.surface(i.getTargetContext(),0xffFFFFFF));view.draw(canvas);
        });
        try(FileOutputStream out=new FileOutputStream(file)){frame[0].compress(Bitmap.CompressFormat.PNG,100,out);}frame[0].recycle();
    }
    void reviewMainUi(Context c,long firstId)throws Exception{
        Store store=Store.get(c);Store.Habit first=store.habit(firstId),second=store.habits().get(1);
        c.getSharedPreferences("routia_history",Context.MODE_PRIVATE).edit().clear().commit();
        Activity main=i.startActivitySync(new Intent(c,MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));i.waitForIdleSync();
        String date=LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy年M月d日 EEEE",Locale.JAPANESE));
        check(find(decor(main),date)!=null,"header shows year month day and full weekday");
        i.runOnMainSync(()->find(decor(main),"成果").performClick());i.waitForIdleSync();
        
        check(findType(decor(main),Spinner.class)==null,"summary has no period selector");
        android.content.SharedPreferences prefs=c.getSharedPreferences("routia_history",Context.MODE_PRIVATE);
        Activity firstCharts=openTaggedPage(main,"charts_"+first.id(),AchievementChartsActivity.class);
        Spinner firstPeriod=(Spinner)findDescription(decor(firstCharts),"表示期間："+first.goal());
        check(firstPeriod!=null&&findDescription(decor(firstCharts),"表示期間："+second.goal())==null,"period selection belongs to the goal chart screen");
        i.runOnMainSync(()->firstPeriod.setSelection(0));i.waitForIdleSync();
        check(prefs.getInt("period_"+first.id(),1)==0&&prefs.getInt("period_"+second.id(),1)==1,"changing first chart period leaves second unchanged");
        View previousControl=findDescription(decor(firstCharts),"前の週："+first.goal());i.runOnMainSync(previousControl::performClick);i.waitForIdleSync();
        String previous=LocalDate.now().minusWeeks(1).toString();
        check(prefs.getString("anchor_"+first.id(),"").equals(previous)&&!prefs.contains("anchor_"+second.id()),"previous period only moves first chart");
        i.runOnMainSync(firstCharts::finish);i.waitForIdleSync();awaitFocus(main);
        Activity secondCharts=openTaggedPage(main,"charts_"+second.id(),AchievementChartsActivity.class);
        Spinner secondPeriod=(Spinner)findDescription(decor(secondCharts),"表示期間："+second.goal());
        i.runOnMainSync(()->secondPeriod.setSelection(2));i.waitForIdleSync();
        check(prefs.getInt("period_"+first.id(),1)==0&&prefs.getString("anchor_"+first.id(),"").equals(previous)&&prefs.getInt("period_"+second.id(),1)==2,"second year chart preserves first week and anchor");
        i.runOnMainSync(secondCharts::finish);i.waitForIdleSync();awaitFocus(main);
        firstCharts=openTaggedPage(main,"charts_"+first.id(),AchievementChartsActivity.class);
        View nextControl=findDescription(decor(firstCharts),"次の週："+first.goal());i.runOnMainSync(nextControl::performClick);i.waitForIdleSync();
        check(prefs.getString("anchor_"+first.id(),"").equals(LocalDate.now().toString())&&prefs.getInt("period_"+second.id(),1)==2,"next period is independent too");
        i.runOnMainSync(firstCharts::finish);i.waitForIdleSync();awaitFocus(main);
        i.runOnMainSync(()->find(decor(main),"設定").performClick());i.waitForIdleSync();
        ReportSettingsActivity settings=(ReportSettingsActivity)openPage(main,"継続レビューの共通設定",ReportSettingsActivity.class);
        check(fullScreen(settings),"shared settings is a full-screen Activity");
        check((settings.getWindow().getAttributes().flags&WindowManager.LayoutParams.FLAG_SECURE)!=0,"shared settings retains screenshot protection");
        check((main.getWindow().getAttributes().flags&WindowManager.LayoutParams.FLAG_SECURE)==0,"ordinary app screen is not made permanently secure");
        check(countInputs(decor(settings))==1,"settings keeps one credential form");
        View back=find(decor(settings),"戻る");
        check(back instanceof TextView&&((TextView)back).getCompoundDrawables()[0]==null,"back matches plain text controls in other settings");
        check(below(back,find(decor(settings),"この設定を保存"))&&back.getWidth()==find(decor(settings),"この設定を保存").getWidth(),"settings back is full width beneath Save");
        shot(settings,"full-screen-settings");
        int[] settingsHeader=new int[2],settingsBack=new int[2];find(decor(settings),"継続レビュー").getLocationOnScreen(settingsHeader);back.getLocationOnScreen(settingsBack);
        i.runOnMainSync(back::performClick);i.waitForIdleSync();
        check(settings.isFinishing()&&!main.isFinishing(),"back returns to same main settings screen");
        i.runOnMainSync(()->find(decor(main),"成果").performClick());i.waitForIdleSync();
        check(findType(decor(main),Spinner.class)==null&&prefs.getInt("period_"+first.id(),1)==0&&prefs.getInt("period_"+second.id(),1)==2,"chart selections survive tab switches without showing summary controls");
        shot(main,"per-goal-periods");
        i.runOnMainSync(()->findTag(decor(main),"nav_review").performClick());i.waitForIdleSync();
        i.runOnMainSync(()->((Spinner)findDescription(decor(main),"継続レビューのタスク")).setSelection(1));i.waitForIdleSync();
        ScrollView catalog=(ScrollView)findType(decor(main),ScrollView.class);
        i.runOnMainSync(()->catalog.scrollTo(0,Ui.dp(c,240)));i.waitForIdleSync();
        int scrollBefore=catalog.getScrollY();
        long savedId;try(ReportStore reports=new ReportStore(c)){savedId=reports.all(first.id()).stream().filter(x->x.state().equals("complete")).findFirst().orElseThrow().id();}
        ReportActivity review=(ReportActivity)openTaggedPage(main,"saved_review_"+savedId,ReportActivity.class);
        check(fullScreen(review)&&!main.isFinishing(),"review opens as a full-screen Activity");
        check(below(find(decor(review),"戻る"),find(decor(review),"要点")),"review back remains below its content");
        int[] reviewHeader=new int[2],reviewBack=new int[2];find(decor(review),"継続レビュー").getLocationOnScreen(reviewHeader);find(decor(review),"戻る").getLocationOnScreen(reviewBack);
        check(Arrays.equals(settingsHeader,reviewHeader)&&Arrays.equals(settingsBack,reviewBack),"both full-screen pages align headers and bottom Back controls");
        shot(review,"full-screen-review");
        ScrollView reviewScroll=(ScrollView)findType(decor(review),ScrollView.class);
        i.runOnMainSync(()->reviewScroll.scrollTo(0,Ui.dp(c,160)));i.waitForIdleSync();int reviewY=reviewScroll.getScrollY();
        ReportSettingsActivity nested=(ReportSettingsActivity)openPage(review,"継続レビューの共通設定",ReportSettingsActivity.class);
        check(fullScreen(nested)&&!review.isFinishing(),"review opens the same full-screen configuration");
        i.runOnMainSync(()->find(decor(nested),"戻る").performClick());i.waitForIdleSync();
        awaitScroll(review,reviewY);check(!review.isFinishing()&&((ScrollView)findType(decor(review),ScrollView.class)).getScrollY()==reviewY,"settings back preserves review scroll: expected "+reviewY+", actual "+((ScrollView)findType(decor(review),ScrollView.class)).getScrollY());
        ReportSettingsActivity saveOpen=(ReportSettingsActivity)openPage(review,"継続レビューの共通設定",ReportSettingsActivity.class);
        i.runOnMainSync(()->find(decor(saveOpen),"保存してレビューを開く").performClick());i.waitForIdleSync();
        awaitFocus(review);awaitScroll(review,reviewY);
        check(saveOpen.isFinishing()&&review.hasWindowFocus(),"save-and-open reuses the existing review without stacking duplicates: closed="+saveOpen.isFinishing()+", focus="+review.hasWindowFocus()+", original finished="+review.isFinishing());
        check(((ScrollView)findType(decor(review),ScrollView.class)).getScrollY()==reviewY,"save-and-open keeps report position: expected "+reviewY+", actual "+((ScrollView)findType(decor(review),ScrollView.class)).getScrollY());
        i.runOnMainSync(()->review.startActivity(ReportActivity.intent(review,second.id()).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_SINGLE_TOP)));i.waitForIdleSync();
        // startActivity posts onNewIntent asynchronously; idle does not guarantee delivery.
        long switchDeadline=SystemClock.uptimeMillis()+3000;final boolean[] switched={false};
        do{
            i.runOnMainSync(()->switched[0]=find(decor(review),second.goal())!=null&&((ScrollView)findType(decor(review),ScrollView.class)).getScrollY()==0);
            if(switched[0])break;SystemClock.sleep(50);i.waitForIdleSync();
        }while(SystemClock.uptimeMillis()<switchDeadline);
        check(switched[0],"switching to another goal starts that review at its top: new title="+(find(decor(review),second.goal())!=null)+", scroll="+((ScrollView)findType(decor(review),ScrollView.class)).getScrollY());


        i.runOnMainSync(()->find(decor(review),"戻る").performClick());i.waitForIdleSync();
        awaitScroll(main,scrollBefore);check(review.isFinishing()&&((ScrollView)findType(decor(main),ScrollView.class)).getScrollY()==scrollBefore,"review back preserves catalog scroll");
        check(findTag(decor(main),"nav_review").isSelected()&&prefs.getInt("period_"+first.id(),1)==0,"review back preserves catalog tab and selected period");
        i.runOnMainSync(()->find(decor(main),"成果").performClick());i.waitForIdleSync();
        check(findType(decor(main),Spinner.class)==null&&prefs.getInt("period_"+first.id(),1)==0,"return to Achievements preserves chart choice without summary controls");
        i.runOnMainSync(main::finish);i.waitForIdleSync();
        Activity reopened=i.startActivitySync(new Intent(c,MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));i.waitForIdleSync();
        i.runOnMainSync(()->find(decor(reopened),"成果").performClick());i.waitForIdleSync();
        
        check(findType(decor(reopened),Spinner.class)==null&&prefs.getInt("period_"+first.id(),1)==0&&prefs.getInt("period_"+second.id(),1)==2,"independent chart preferences survive reopening app");
        i.runOnMainSync(()->store.record(second.id(),LocalDate.now(),2,true));
        i.runOnMainSync(()->find(decor(reopened),"今日").performClick());i.waitForIdleSync();
        i.runOnMainSync(()->find(decor(reopened),"記録を修正").performClick());i.waitForIdleSync();
        Dialog correction=((MainActivity)reopened).recordCorrectionDialog;
        check(correction!=null&&correction.isShowing(),"record correction uses a visible dialog");
        RadioGroup choices=(RadioGroup)findType(correction.getWindow().getDecorView(),RadioGroup.class);
        check(((RadioButton)choices.getChildAt(0)).getButtonDrawable() instanceof android.graphics.drawable.InsetDrawable,"correction indicator has a true left inset");
        shotView(correction.getWindow().getDecorView(),"correction-spacing");
        i.runOnMainSync(()->find(correction.getWindow().getDecorView(),"戻る").performClick());i.waitForIdleSync();
        check(store.level(second.id(),LocalDate.now())==2,"closing correction leaves achievement untouched");
        i.runOnMainSync(reopened::finish);i.waitForIdleSync();
    }
    void awaitScroll(Activity activity,int expected){
        long until=SystemClock.uptimeMillis()+3000;final boolean[] ready={false};
        do{
            i.runOnMainSync(()->{ScrollView view=(ScrollView)findType(decor(activity),ScrollView.class);ready[0]=view!=null&&view.isLaidOut()&&view.getScrollY()==expected;});
            if(ready[0])return;SystemClock.sleep(50);i.waitForIdleSync();
        }while(SystemClock.uptimeMillis()<until);
    }
    void awaitFocus(Activity activity){
        long until=SystemClock.uptimeMillis()+5000;final boolean[] focused={false};
        do{
            i.runOnMainSync(()->focused[0]=activity.hasWindowFocus());
            if(focused[0])return;SystemClock.sleep(50);i.waitForIdleSync();
        }while(SystemClock.uptimeMillis()<until);
    }
    Activity recreatePage(Activity old){
        Instrumentation.ActivityMonitor monitor=i.addMonitor(old.getClass().getName(),null,false);
        i.runOnMainSync(old::recreate);Activity next=i.waitForMonitorWithTimeout(monitor,5000);i.removeMonitor(monitor);
        check(next!=null&&next!=old,"configuration recreation creates the replacement Activity");i.waitForIdleSync();return next;
    }
    void reviewPageState(Context c,long id)throws Exception{
        Activity review=i.startActivitySync(ReportActivity.intent(c,id).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));i.waitForIdleSync();
        i.runOnMainSync(()->find(decor(review),"傾向").performClick());i.waitForIdleSync();
        ScrollView scroll=(ScrollView)findType(decor(review),ScrollView.class);
        i.runOnMainSync(()->scroll.scrollTo(0,Ui.dp(c,180)));i.waitForIdleSync();int y=scroll.getScrollY();
        Activity restored=recreatePage(review);awaitScroll(restored,y);
        check(find(decor(restored),"傾向").isSelected(),"review tab survives Activity recreation");
        check(((ScrollView)findType(decor(restored),ScrollView.class)).getScrollY()==y,"review reading position survives Activity recreation");
        i.runOnMainSync(restored::finish);i.waitForIdleSync();
        Activity settings=i.startActivitySync(ReportSettingsActivity.intent(c).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));i.waitForIdleSync();
        i.runOnMainSync(()->{
            findTag(decor(settings),"gpt-6-astra").performClick();
            ((SeekBar)findDescription(decor(settings),"1回の上限金額")).setProgress(18);
            ((EditText)findType(decor(settings),EditText.class)).setText("qa-unsaved-draft-not-valid");
        });i.waitForIdleSync();
        Activity restoredSettings=recreatePage(settings);
        check(((RadioButton)findTag(decor(restoredSettings),"gpt-6-astra")).isChecked()&&((SeekBar)findDescription(decor(restoredSettings),"1回の上限金額")).getProgress()==18,"settings restore nonsecret draft selections");
        check(((EditText)findType(decor(restoredSettings),EditText.class)).getText().length()==0,"unsaved key never survives Activity recreation");
        check((restoredSettings.getWindow().getAttributes().flags&WindowManager.LayoutParams.FLAG_SECURE)!=0,"recreated configuration remains protected");
        i.runOnMainSync(restoredSettings::finish);i.waitForIdleSync();
        check(!ReportSettings.configured(c)&&!ReportSettings.options(c).model().equals("gpt-6-astra"),"closing reconstructed draft does not save key or model");
    }
    Activity openTaggedPage(Activity source,String tag,Class<? extends Activity> target){
        Instrumentation.ActivityMonitor monitor=i.addMonitor(target.getName(),null,false);
        i.runOnMainSync(()->findTag(decor(source),tag).performClick());
        Activity result=i.waitForMonitorWithTimeout(monitor,5000);i.removeMonitor(monitor);
        check(result!=null,"tagged navigation opens "+target.getSimpleName());i.waitForIdleSync();return result;
    }
    Activity openPage(Activity source,String label,Class<? extends Activity> target){
        Instrumentation.ActivityMonitor monitor=i.addMonitor(target.getName(),null,false);
        i.runOnMainSync(()->find(decor(source),label).performClick());
        Activity result=i.waitForMonitorWithTimeout(monitor,5000);i.removeMonitor(monitor);
        check(result!=null,"navigation opens "+target.getSimpleName());i.waitForIdleSync();return result;
    }
    boolean fullScreen(Activity activity){
        android.util.TypedValue floating=new android.util.TypedValue();
        activity.getTheme().resolveAttribute(android.R.attr.windowIsFloating,floating,true);
        return floating.data==0&&decor(activity).getWidth()==activity.getResources().getDisplayMetrics().widthPixels;
    }
    boolean below(View lower,View upper){
        int[] a=new int[2],b=new int[2];lower.getLocationOnScreen(a);upper.getLocationOnScreen(b);return a[1]>=b[1]+upper.getHeight();
    }
    void reviewCalendars(Context c)throws Exception{
        for(int theme:new int[]{0,1,2,3,4,5,6}){
            AppColors.save(c,theme);
            final android.graphics.Bitmap[] bitmap={null};
            i.runOnMainSync(()->{
                LocalDate start=LocalDate.of(2026,10,1),end=LocalDate.of(2026,12,31);
                int count=(int)java.time.temporal.ChronoUnit.DAYS.between(start,end)+1;
                int[] shades=new int[count];double[] values=new double[count];Arrays.fill(values,-1);
                shades[23]=1;shades[24]=2;shades[25]=4;shades[26]=5;
                for(int at=27;at<count;at++)shades[at]=-1;
                GrassView grass=new GrassView(c,start,end,shades,values,false,"");
                grass.setBackgroundColor(Ui.surface(c,android.graphics.Color.WHITE));
                grass.measure(View.MeasureSpec.makeMeasureSpec(grass.width(),View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(grass.height(),View.MeasureSpec.EXACTLY));grass.layout(0,0,grass.width(),grass.height());
                bitmap[0]=Bitmap.createBitmap(grass.width(),grass.height(),Bitmap.Config.ARGB_8888);grass.draw(new android.graphics.Canvas(bitmap[0]));
                int surface=Ui.surface(c,android.graphics.Color.WHITE);
                for(int index:new int[]{23,24,25,26}){
                    LocalDate day=start.plusDays(index);int col=(int)java.time.temporal.ChronoUnit.DAYS.between(grass.sunday,day)/7,row=day.getDayOfWeek().getValue()%7;
                    int pixel=bitmap[0].getPixel(Ui.dp(c,9+col*GrassView.STEP),Ui.dp(c,GrassView.HEADER+row*GrassView.STEP+6));
                    check(pixel==GrassView.COLORS[shades[index]],"achievement/ticket pixels use the same colors in every theme "+theme);
                }

                // A previous horizontal grid boundary and inter-cell gap must now remain the bare card.
                check(bitmap[0].getPixel(Ui.dp(c,2),Ui.dp(c,GrassView.HEADER+GrassView.STEP))==surface,"calendar removes grid/row paint in theme "+theme);
                int empty=bitmap[0].getPixel(Ui.dp(c,12),Ui.dp(c,GrassView.HEADER+4*GrassView.STEP+10));
                check(empty==GrassView.palette(c)[0],"rendered unrecorded cell uses theme palette "+theme);
                int distance=Math.abs(android.graphics.Color.red(empty)-android.graphics.Color.red(surface))+Math.abs(android.graphics.Color.green(empty)-android.graphics.Color.green(surface))+Math.abs(android.graphics.Color.blue(empty)-android.graphics.Color.blue(surface));
                check(distance<85,"empty cells blend with card instead of flooding it with white "+theme);
                if(AppColors.night(c))check(android.graphics.Color.green(GrassView.palette(c)[2])>android.graphics.Color.green(empty)+40,"dark calendar achievements stand out "+theme);
            });
            try(FileOutputStream out=new FileOutputStream(new File(c.getExternalFilesDir(null),"report-calendar-theme-"+theme+".png"))){bitmap[0].compress(Bitmap.CompressFormat.PNG,100,out);}bitmap[0].recycle();
        }
        AppColors.save(c,0);
    }
    View findStartsWith(View v,String prefix){
        if(v instanceof TextView&&((TextView)v).getText().toString().startsWith(prefix))return v;
        if(v instanceof ViewGroup){ViewGroup g=(ViewGroup)v;for(int at=0;at<g.getChildCount();at++){View found=findStartsWith(g.getChildAt(at),prefix);if(found!=null)return found;}}return null;
    }
    void shotView(View view,String name)throws Exception{
        final Bitmap[] frame={null};i.runOnMainSync(()->{frame[0]=Bitmap.createBitmap(view.getWidth(),view.getHeight(),Bitmap.Config.ARGB_8888);view.draw(new android.graphics.Canvas(frame[0]));});
        try(FileOutputStream out=new FileOutputStream(new File(i.getTargetContext().getExternalFilesDir(null),"report-"+name+".png"))){frame[0].compress(Bitmap.CompressFormat.PNG,100,out);}frame[0].recycle();
    }
    JSONObject detailedFixture()throws JSONException{
        JSONObject r=personalFixture();
        r.getJSONArray("insights").getJSONObject(0).put("detail","記録上の違いを確認し、原因は仮説として扱います。".repeat(8));
        JSONObject a=r.getJSONArray("actions").getJSONObject(0);
        a.put("why","本人の実記録をもとに、準備にかかる負担を減らす方法を一度試す提案です。".repeat(5));
        a.put("step","次回は服と靴を先に用意し、無理のない範囲でいつもの手順を一つ減らして始めてみてください。".repeat(5));
        a.put("effect","次回の記録で再開できたかを確かめ、合わなければ手順を見直してください。".repeat(5));
        r.getJSONArray("lenses").getJSONObject(0).put("detail","研究は一般的な傾向を示すもので、本人に同じ効果があるかは次回の記録で確認します。".repeat(5));
        return r;
    }
    void reviewDetail(Context c,long habit)throws Exception{
        check(ReportSettings.options(c).detail()==ReportDetail.STANDARD,"legacy preferences default to standard");
        for(ReportDetail d:ReportDetail.values()){
            ReportBudget.Options o=new ReportBudget.Options(ReportBudget.MODEL,300,d);
            JSONObject request=ReportData.request(new JSONObject(),o);
            String instructions=request.getString("instructions");
            check(instructions.contains(d.label)&&instructions.contains(d.range)&&instructions.contains("水増し"),"depth controls analysis and permits shorter useful reports");
            JSONObject props=request.getJSONObject("text").getJSONObject("format").getJSONObject("schema").getJSONObject("properties");
            check(props.getJSONObject("headline").getInt("maxLength")==d.headline,"schema uses selected depth");
            ReportData.validate(personalFixture(),d);n++;
            JSONObject tooLong=personalFixture();tooLong.put("headline","長".repeat(d.headline+1));
            try{ReportData.validate(tooLong,d);throw new AssertionError("depth field overflow accepted");}catch(JSONException expected){n++;}
            ReportBudget.Reservation reservation=ReportBudget.reserve(o,8000,LocalDate.now());
            check(reservation.reservedYen().intValue()<=o.limitYen(),"depth never bypasses fee ceiling");
        }
        JSONObject detailed=detailedFixture();ReportData.validate(detailed,ReportDetail.DETAILED);n++;
        try{ReportData.validate(detailed);throw new AssertionError("detailed content accepted as standard");}catch(JSONException expected){n++;}
        JSONObject oversized=personalFixture();
        JSONObject bigInsight=new JSONObject().put("title","題".repeat(24)).put("detail","文".repeat(320));
        JSONObject bigAction=new JSONObject().put("title","題".repeat(28)).put("why","文".repeat(280)).put("cue","場".repeat(100)).put("step","手".repeat(300)).put("effect","確".repeat(220));
        JSONObject bigLens=new JSONObject().put("name","研".repeat(28)).put("detail","文".repeat(220)).put("source_id","none");
        oversized.put("headline","頭".repeat(100)).put("insights",new JSONArray().put(bigInsight).put(bigInsight))
            .put("actions",new JSONArray().put(bigAction).put(bigAction)).put("lenses",new JSONArray().put(bigLens).put(bigLens))
            .put("limits",new JSONArray().put("限".repeat(120)).put("限".repeat(120)));
        try{ReportData.validate(oversized,ReportDetail.DETAILED);throw new AssertionError("total overflow accepted");}catch(JSONException expected){n++;}

        Activity a=i.startActivitySync(ReportSettingsActivity.intent(c).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));i.waitForIdleSync();
        check(findDescription(decor(a),"レポートの詳しさ")!=null,"shared depth selector visible");
        check(((RadioButton)findTag(decor(a),"standard")).isChecked(),"standard initially selected");
        i.runOnMainSync(()->findTag(decor(a),"detailed").performClick());i.waitForIdleSync();
        check(ReportSettings.options(c).detail()==ReportDetail.STANDARD,"draft depth not saved immediately");
        check(((RadioButton)findTag(decor(a),"detailed")).isChecked()&&!((RadioButton)findTag(decor(a),"standard")).isChecked()&&!((RadioButton)findTag(decor(a),"brief")).isChecked(),"only the selected depth is checked");
        final Bundle draft=new Bundle();i.runOnMainSync(()->((ReportSettingsActivity)a).onSaveInstanceState(draft));
        check(draft.getString("detail").equals("detailed"),"depth draft survives rotation state");
        ScrollView sc=(ScrollView)findType(decor(a),ScrollView.class);
        i.runOnMainSync(()->sc.scrollTo(0,((View)findTag(decor(a),"detailed").getParent().getParent()).getTop()));i.waitForIdleSync();
        SystemClock.sleep(400);shot(a,"detail-settings");
        i.runOnMainSync(()->find(decor(a),"この設定を保存").performClick());i.waitForIdleSync();
        check(ReportSettings.options(c).detail()==ReportDetail.DETAILED,"depth survives save");
        Activity reopened=i.startActivitySync(ReportSettingsActivity.intent(c).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));i.waitForIdleSync();
        check(((RadioButton)findTag(decor(reopened),"detailed")).isChecked(),"depth restored on reopen");
        i.runOnMainSync(()->{findTag(decor(reopened),"brief").performClick();reopened.finish();});i.waitForIdleSync();
        check(ReportSettings.options(c).detail()==ReportDetail.DETAILED,"closing discards depth draft");

        try(ReportStore db=new ReportStore(c)){
            JSONObject facts=ReportData.snapshot(Store.get(c),Store.get(c).habit(habit),LocalDate.now(),db);
            long row=db.enqueue(habit,"detail-contract",Store.get(c).habit(habit).goal(),facts.toString());
            check(db.claim(row)&&db.recordOptions(row,new ReportBudget.Options(ReportBudget.MODEL,300,ReportDetail.DETAILED)),"depth stored before sending");
            db.finish(row,"complete",detailed.toString(),"",0);
            check(db.item(row).detail().equals("detailed"),"report keeps original depth");
            ReportSettings.saveOptions(c,new ReportBudget.Options(ReportBudget.MODEL,300,ReportDetail.BRIEF),Collections.emptyMap());
            Activity report=i.startActivitySync(ReportActivity.intent(c,habit).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));i.waitForIdleSync();
            check(find(decor(report),"AIのアドバイス：生成済み · "+facts.getString("as_of")+" の記録")!=null,"detailed saved report opens while current setting is brief");
            check(find(decor(report),"レポートの詳しさ：詳細")!=null,"saved depth shown independently of current setting");
            i.runOnMainSync(()->find(decor(report),"改善策").performClick());i.waitForIdleSync();
            check(find(decor(report),detailed.getJSONArray("actions").getJSONObject(0).getString("step"))!=null,"long advice renders without truncation");
            shot(report,"detail-advice");i.runOnMainSync(report::finish);i.waitForIdleSync();
            db.getWritableDatabase().delete("reports","id=?",new String[]{""+row});
        }
        ReportSettings.saveKey(c,"qa-only-not-valid-api-key");
        ReportBudget.Options approved=new ReportBudget.Options(ReportBudget.MODEL,300,ReportDetail.DETAILED);
        ReportSettings.setFeatureEnabled(c,true);
        ReportSettings.saveOptions(c,approved,Collections.singletonMap(habit,true));
        try(ReportStore db=new ReportStore(c)){
            long row=db.enqueue(habit,"detail-manual","QA","{}");
            check(db.confirmManual(row,approved)&&db.item(row).detail().equals("detailed"),"manual approval stores depth");
            db.getWritableDatabase().delete("reports","id=?",new String[]{""+row});
        }
        ReportSettings.saveOptions(c,new ReportBudget.Options(ReportBudget.MODEL,300,ReportDetail.BRIEF),Collections.singletonMap(habit,true));
        try{ReportRunner.enqueue(c,Store.get(c).habit(habit),approved);throw new AssertionError("stale depth approval accepted");}catch(IllegalArgumentException expected){n++;}
        ReportSettings.saveOptions(c,ReportBudget.defaults(),Collections.singletonMap(habit,false));
        ReportSettings.prefs(c).edit().clear().commit();ReportJob.schedule(c);

        String name="routia-report-v2-detail-migration.db";c.deleteDatabase(name);
        try(SQLiteDatabase old=SQLiteDatabase.openOrCreateDatabase(c.getDatabasePath(name),null)){
            old.execSQL("CREATE TABLE reports(id INTEGER PRIMARY KEY AUTOINCREMENT,habit INTEGER NOT NULL,week TEXT NOT NULL,goal TEXT NOT NULL,snapshot TEXT NOT NULL,result TEXT NOT NULL DEFAULT '',state TEXT NOT NULL DEFAULT 'queued',problem TEXT NOT NULL DEFAULT '',reserved REAL NOT NULL DEFAULT 0,estimated REAL NOT NULL DEFAULT 0,requested INTEGER NOT NULL DEFAULT 0,model TEXT NOT NULL DEFAULT '',budget INTEGER NOT NULL DEFAULT 0,created TEXT NOT NULL,UNIQUE(habit,week))");
            old.execSQL("INSERT INTO reports(habit,week,goal,snapshot,result,state,model,budget,created) VALUES(1,'old','QA','{}',?,'complete','gpt-6.1-sol',300,'old')",new Object[]{personalFixture().toString()});old.setVersion(2);
        }
        try(ReportStore db=new ReportStore(c,name)){
            ReportStore.Item old=db.all(1).get(0);
            check(old.detail().isEmpty()&&old.budget()==300&&old.state().equals("complete"),"v2 migration preserves report without inventing saved depth");
            ReportData.validate(new JSONObject(old.result()));n++;
        }c.deleteDatabase(name);
    }
    void reviewMigration(Context c)throws Exception{
        String name="routia-report-v1-migration.db";c.deleteDatabase(name);
        try(SQLiteDatabase old=SQLiteDatabase.openOrCreateDatabase(c.getDatabasePath(name),null)){
            old.execSQL("CREATE TABLE reports(id INTEGER PRIMARY KEY AUTOINCREMENT,habit INTEGER NOT NULL,week TEXT NOT NULL,goal TEXT NOT NULL,snapshot TEXT NOT NULL,result TEXT NOT NULL DEFAULT '',state TEXT NOT NULL DEFAULT 'queued',problem TEXT NOT NULL DEFAULT '',reserved REAL NOT NULL DEFAULT 0,estimated REAL NOT NULL DEFAULT 0,requested INTEGER NOT NULL DEFAULT 0,created TEXT NOT NULL,UNIQUE(habit,week))");
            old.execSQL("INSERT INTO reports(habit,week,goal,snapshot,result,state,reserved,estimated,created) VALUES(1,'old','旧レポート','{}',?,'complete',250,110,'old')",new Object[]{fixture().toString()});old.setVersion(1);
        }
        try(ReportStore db=new ReportStore(c,name)){
            ReportStore.Item item=db.all(1).get(0);
            check(item.state().equals("complete")&&item.estimated()==110&&item.reserved()==250,"old report amounts preserved in migration");
            check(item.model().isEmpty()&&item.budget()==0,"old model is unknown rather than invented");
            ReportData.validate(new JSONObject(item.result()));n++;
        }
        c.deleteDatabase(name);
    }
    int countInputs(View v){int n=v instanceof EditText?1:0;if(v instanceof ViewGroup){ViewGroup g=(ViewGroup)v;for(int k=0;k<g.getChildCount();k++)n+=countInputs(g.getChildAt(k));}return n;}
    View findTag(View v,String tag){if(tag.equals(v.getTag()))return v;if(v instanceof ViewGroup){ViewGroup g=(ViewGroup)v;for(int k=0;k<g.getChildCount();k++){View x=findTag(g.getChildAt(k),tag);if(x!=null)return x;}}return null;}
    View findDescription(View v,String label){if(label.contentEquals(v.getContentDescription()==null?"":v.getContentDescription()))return v;if(v instanceof ViewGroup){ViewGroup g=(ViewGroup)v;for(int k=0;k<g.getChildCount();k++){View x=findDescription(g.getChildAt(k),label);if(x!=null)return x;}}return null;}
    View findType(View v,Class<?> type){if(type.isInstance(v))return v;if(v instanceof ViewGroup){ViewGroup g=(ViewGroup)v;for(int k=0;k<g.getChildCount();k++){View x=findType(g.getChildAt(k),type);if(x!=null)return x;}}return null;}
    View find(View v,String label){
        if(v instanceof TextView&&label.contentEquals(((TextView)v).getText()))return v;
        if(v instanceof ViewGroup){ViewGroup g=(ViewGroup)v;for(int k=0;k<g.getChildCount();k++){View found=find(g.getChildAt(k),label);if(found!=null)return found;}}return null;
    }
    void shot(Activity a,String tab)throws Exception{
        File file=new File(i.getTargetContext().getExternalFilesDir(null),"report-"+tab+".png");
        final Bitmap[] frame={null};i.runOnMainSync(()->{View v=decor(a);frame[0]=Bitmap.createBitmap(v.getWidth(),v.getHeight(),Bitmap.Config.ARGB_8888);v.draw(new android.graphics.Canvas(frame[0]));});
        try(FileOutputStream out=new FileOutputStream(file)){frame[0].compress(Bitmap.CompressFormat.PNG,100,out);}frame[0].recycle();
    }
}
