package app.routia;

import android.app.*;
import android.app.job.JobScheduler;
import android.content.*;
import android.os.*;
import android.view.*;
import android.widget.*;
import java.io.IOException;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import org.json.*;

/** Fake credentials and a local transport only; no provider request is made. */
final class SecurityReview {
    private final Instrumentation i;
    private int n;
    private Context c;
    private long habit;
    private static final String FIRST="qa-security-credential-first",SECOND="qa-security-credential-second";
    SecurityReview(Instrumentation i){this.i=i;}
    private void check(boolean ok,String message){n++;if(!ok)throw new AssertionError(message);}
    int run()throws Exception{
        i.waitForIdleSync(); // Finish Application.onCreate before enabling any fake review settings.
        c=i.getTargetContext();
        check((Build.HARDWARE.contains("ranchu")||Build.HARDWARE.contains("goldfish"))&&c.getPackageName().endsWith(".reportqa"),"dedicated emulator only");
        check(Store.get(c).habits().isEmpty()&&!ReportSettings.configured(c),"fresh test app without user data");
        habit=Store.get(c).saveConfiguredHabit(0,"","キー切替の架空目標","一歩","続ける","","check",0,"");
        Store.get(c).record(habit,LocalDate.now(),1,false);
        try{
            ReportSettings.saveKey(c,FIRST);
            // An installed legacy key without revision metadata stays usable until explicitly changed.
            ReportSettings.prefs(c).edit().remove("key_revision").commit();
            ReportSettings.Credential legacy=ReportSettings.credential(c);
            check(legacy.value.equals(FIRST)&&ReportSettings.currentCredential(c,legacy),"legacy encrypted key remains readable");
            check(!legacy.toString().contains(FIRST),"credential diagnostics are redacted");
            ReportSettings.saveOptions(c,ReportBudget.defaults(),Map.of(habit,true));ReportSettings.setFeatureEnabled(c,true);
            check(ReportSettings.prefs(c).getString("key_cipher","").indexOf(FIRST)<0,"stored key is encrypted");
            storedStates();
            scenario("replace",()->ReportSettings.saveKey(c,SECOND),false,false,false);
            scenario("delete",()->ReportSettings.deleteKey(c),false,false,false);
            scenario("delete-readd",()->{ReportSettings.deleteKey(c);ReportSettings.saveKey(c,FIRST);},false,false,false);
            scenario("same-value-save",()->ReportSettings.saveKey(c,FIRST),false,false,false);
            scenario("newly-approved",()->ReportSettings.saveKey(c,SECOND),true,false,false);
            scenario("unchanged",()->{},false,true,false);
            scenario("unknown-response",()->{},false,true,true);
            check(Store.get(c).level(habit,LocalDate.now())==1,"habit records survive key changes");
            ReportSettings.setFeatureEnabled(c,false);
            ReportSettings.saveKey(c,FIRST);
            consentUi();
        }finally{ReportSettings.setFeatureEnabled(c,false);ReportSettings.deleteKey(c);}
        return n;
    }
    private void storedStates()throws Exception{
        ReportSettings.Credential old=ReportSettings.credential(c);
        try(ReportStore db=new ReportStore(c)){
            long queued=db.enqueue(habit,"security-queued","QA","{}");
            long counting=db.enqueue(habit,"security-counting","QA","{}");db.claim(counting);
            long sent=db.enqueue(habit,"security-sent","QA","{}");db.claim(sent);db.sending(sent,20);
            long unknown=db.enqueue(habit,"security-unknown","QA","{}");db.claim(unknown);db.sending(unknown,20);db.finish(unknown,"stopped","","unknown",0);
            long saved=db.enqueue(habit,"security-saved","QA","{}");db.finish(saved,"complete","{}","",2);
            ReportSettings.saveKey(c,SECOND);
            check(!ReportSettings.currentCredential(c,old),"key replacement invalidates old revision");
            check(db.item(queued).state().equals("stopped")&&db.item(counting).state().equals("stopped"),"replacement cancels queued and counting work");
            check(!db.sending(counting,20),"cancelled preflight cannot submit");
            check(db.item(sent).state().equals("sending")&&db.item(sent).reserved()==20,"submitted reservation survives replacement");
            check(db.item(unknown).state().equals("stopped")&&db.item(unknown).reserved()==20&&!db.retryUnsent(unknown),"uncertain request remains non-retryable");
            check(db.item(saved).result().equals("{}")&&db.item(saved).state().equals("complete"),"saved review survives replacement");
            ReportSettings.deleteKey(c);
            check(!ReportSettings.configured(c)&&ReportSettings.readKey(c).isEmpty(),"deletion removes encrypted credential");
            check(ReportSettings.habitEnabled(c,habit)&&ReportSettings.featureEnabled(c)&&ReportSettings.options(c).equals(ReportBudget.defaults()),"deletion preserves targets and options");
            check(c.getSystemService(JobScheduler.class).getPendingJob(3703)==null,"deletion cancels periodic job");
            ReportSettings.saveKey(c,FIRST);
            check(!ReportSettings.currentCredential(c,old),"delete and readd cannot revive a legacy revision");
        }
    }
    private interface Change {void run()throws Exception;}
    private void scenario(String name,Change change,boolean reapprove,boolean expectPaid,boolean uncertain)throws Exception{
        ReportSettings.saveKey(c,FIRST);
        try(ReportStore db=new ReportStore(c)){
            long id=db.enqueue(habit,"security-"+name,"QA","{}");
            check(db.confirmManual(id,ReportSettings.options(c))&&db.claim(id),name+": prepared approved request");
            CountDownLatch counting=new CountDownLatch(1),resume=new CountDownLatch(1);
            AtomicInteger paid=new AtomicInteger();AtomicReference<Throwable> failure=new AtomicReference<>();
            ReportRunner.Transport transport=(path,key,request)->{
                if(path.equals("responses/input_tokens")){
                    counting.countDown();
                    try{if(!resume.await(30,TimeUnit.SECONDS))throw new IOException("mock preflight timeout");}
                    catch(InterruptedException e){Thread.currentThread().interrupt();throw new IOException("mock interrupted");}
                    return new JSONObject().put("input_tokens",1000);
                }
                paid.incrementAndGet();
                if(!expectPaid)throw new AssertionError(name+": obsolete credential reached paid transport");
                if(!FIRST.equals(key))throw new AssertionError("unexpected mock credential");
                if(uncertain)throw new IOException("mock connection loss after submission");
                return new JSONObject().put("model",ReportBudget.MODEL).put("status","completed").put("service_tier","default")
                    .put("usage",new JSONObject().put("input_tokens",1000).put("output_tokens",100))
                    .put("output",new JSONArray().put(new JSONObject().put("type","message").put("content",new JSONArray()
                        .put(new JSONObject().put("type","output_text").put("text",ReportReview.personalFixture().toString())))));
            };
            Thread worker=new Thread(()->{try{ReportRunner.generate(c,db,db.item(id),transport);}catch(Throwable t){failure.set(t);}},"mock-security-review");
            worker.start();
            try{
                check(counting.await(15,TimeUnit.SECONDS),name+": worker paused during preflight");
                change.run();
                if(reapprove)check(db.confirmManual(id,ReportSettings.options(c)),"new consent may queue a cancelled request");
            }finally{resume.countDown();worker.join(45000);}
            check(!worker.isAlive()&&failure.get()==null,name+": worker completed safely");
            check(paid.get()==(expectPaid?1:0),name+": paid submission count");
            ReportStore.Item result=db.item(id);
            if(reapprove)check(result.state().equals("queued")&&result.manual(),"old worker preserves newly approved queued retry");
            else if(!expectPaid)check(result.state().equals("stopped")&&result.reserved()==0,name+": stopped before fee reservation");
            else if(uncertain)check(result.state().equals("stopped")&&result.reserved()>0&&!db.retryUnsent(id),"unknown response never retries");
            else check(result.state().equals("complete")&&!result.result().isEmpty(),"unchanged credentials can still save a review");
        }
    }
    private MotionEvent touch(View v,int action,int flags){
        MotionEvent.PointerProperties pointer=new MotionEvent.PointerProperties();pointer.id=0;pointer.toolType=MotionEvent.TOOL_TYPE_FINGER;
        MotionEvent.PointerCoords point=new MotionEvent.PointerCoords();point.x=v.getWidth()/2f;point.y=v.getHeight()/2f;point.pressure=1;point.size=1;
        long now=SystemClock.uptimeMillis();
        return MotionEvent.obtain(now,now,action,1,new MotionEvent.PointerProperties[]{pointer},new MotionEvent.PointerCoords[]{point},0,0,1,1,0,0,InputDevice.SOURCE_TOUCHSCREEN,flags);
    }
    private void tap(View v,int flags){
        i.runOnMainSync(()->{
            MotionEvent down=touch(v,MotionEvent.ACTION_DOWN,flags),up=touch(v,MotionEvent.ACTION_UP,flags);
            try{v.dispatchTouchEvent(down);v.dispatchTouchEvent(up);}finally{down.recycle();up.recycle();}
        });i.waitForIdleSync();
    }
    private void consentUi()throws Exception{
        ReportSettingsActivity form=(ReportSettingsActivity)i.startActivitySync(ReportSettingsActivity.intent(c).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));i.waitForIdleSync();
        try{
            check((form.getWindow().getAttributes().flags&WindowManager.LayoutParams.FLAG_SECURE)!=0,"credential screen remains screenshot-protected");
            ReportReview views=new ReportReview(i);EditText input=(EditText)views.findType(form.getWindow().getDecorView(),EditText.class);
            check(input.getFilterTouchesWhenObscured()&&!input.isSaveEnabled(),"credential field rejects obscured touches and state saving");
            if(Build.VERSION.SDK_INT>=34)check(input.isAccessibilityDataSensitive(),"credential field protects sensitive accessibility data");
            for(int kind=0;kind<3;kind++){
                final int selected=kind;AtomicInteger accepted=new AtomicInteger();AlertDialog[] dialog={null};
                i.runOnMainSync(()->dialog[0]=selected==0?ReportConsent.manual(form,"架空の目標",ReportBudget.defaults(),accepted::incrementAndGet):
                    selected==1?ReportConsent.enable(form,ReportBudget.defaults(),List.of("架空の目標"),accepted::incrementAndGet):
                    ReportConsent.settings(form,ReportBudget.defaults(),List.of("架空の目標"),accepted::incrementAndGet));i.waitForIdleSync();
                Button positive=dialog[0].getButton(AlertDialog.BUTTON_POSITIVE);
                check(positive.getFilterTouchesWhenObscured(),"fee confirmation has touch protection");
                tap(positive,MotionEvent.FLAG_WINDOW_IS_OBSCURED);if(Build.VERSION.SDK_INT>=29)tap(positive,MotionEvent.FLAG_WINDOW_IS_PARTIALLY_OBSCURED);
                check(accepted.get()==0&&dialog[0].isShowing(),"full and partial obscuring cannot accept fee consent");
                tap(positive,0);check(accepted.get()==1&&!dialog[0].isShowing(),"ordinary touch still accepts explicit fee consent");
            }
            AtomicInteger accessible=new AtomicInteger();AlertDialog[] dialog={null};
            i.runOnMainSync(()->{dialog[0]=ReportConsent.manual(form,"架空の目標",ReportBudget.defaults(),accessible::incrementAndGet);dialog[0].getButton(AlertDialog.BUTTON_POSITIVE).performClick();});i.waitForIdleSync();
            check(accessible.get()==1,"standard accessible click remains available");
            MainActivity home=(MainActivity)i.startActivitySync(new Intent(c,MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));i.waitForIdleSync();
            View nav=views.findTag(home.getWindow().getDecorView(),"nav_settings");i.runOnMainSync(nav::performClick);i.waitForIdleSync();
            View master=views.findTag(home.getWindow().getDecorView(),"review_feature_switch");check(master.getFilterTouchesWhenObscured(),"global paid switch rejects obscured touches");
            i.runOnMainSync(home::finish);i.waitForIdleSync();
        }finally{i.runOnMainSync(form::finish);i.waitForIdleSync();}
    }
}
