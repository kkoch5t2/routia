package app.routia;

import android.app.*;
import android.app.job.*;
import android.content.*;
import android.os.*;
import android.view.*;
import android.widget.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;

/** All credentials and history in this suite are fake, in a fresh emulator-only app. */
final class ReportMasterReview {
    final Instrumentation i;final ReportReview views;int n;
    ReportMasterReview(Instrumentation i){this.i=i;views=new ReportReview(i);}
    void check(boolean ok,String message){n++;if(!ok)throw new AssertionError(message);}
    View decor(Activity a){return a.getWindow().getDecorView();}
    View find(Activity a,String text){return views.find(decor(a),text);}
    void click(Activity a,String text){View v=find(a,text);check(v!=null,"control exists: "+text);i.runOnMainSync(v::performClick);i.waitForIdleSync();}
    Switch master(MainActivity a){return (Switch)views.findTag(decor(a),"review_feature_switch");}
    void toggle(MainActivity a,boolean on){i.runOnMainSync(()->master(a).setChecked(on));i.waitForIdleSync();}
    String message(AlertDialog d){return ((TextView)d.findViewById(android.R.id.message)).getText().toString();}
    void accept(AlertDialog d){check(d!=null&&d.isShowing(),"confirmation visible");i.runOnMainSync(()->d.getButton(AlertDialog.BUTTON_POSITIVE).performClick());i.waitForIdleSync();}
    ReportSettingsActivity form(Context c){ReportSettingsActivity a=(ReportSettingsActivity)i.startActivitySync(ReportSettingsActivity.intent(c).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));i.waitForIdleSync();return a;}
    MainActivity main(Context c){MainActivity a=(MainActivity)i.startActivitySync(new Intent(c,MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));i.waitForIdleSync();View nav=views.findTag(decor(a),"nav_settings");i.runOnMainSync(nav::performClick);i.waitForIdleSync();return a;}
    void shot(MainActivity a,String name)throws Exception{ScrollView sc=(ScrollView)views.findType(decor(a),ScrollView.class);i.runOnMainSync(()->sc.scrollTo(0,((View)master(a).getParent()).getTop()));i.waitForIdleSync();views.shot(a,name);}
    int run()throws Exception{
        Context c=i.getTargetContext();check((Build.HARDWARE.contains("ranchu")||Build.HARDWARE.contains("goldfish"))&&c.getPackageName().endsWith(".reportqa"),"dedicated emulator only");
        Store store=Store.get(c);check(store.habits().isEmpty()&&!ReportSettings.configured(c),"fresh app without real credentials");
        check(!ReportSettings.featureEnabled(c),"new installation defaults globally off");
        long first=store.saveConfiguredHabit(0,"","全体切替テスト・英語","一歩","続ける","","check",0,"");
        long second=store.saveConfiguredHabit(0,"","全体切替テスト・運動","一歩","続ける","","check",0,"");
        LocalDate today=LocalDate.now();
        store.getWritableDatabase().execSQL("UPDATE habits SET created=?",new Object[]{today.minusDays(40).toString()});
        JobScheduler scheduler=c.getSystemService(JobScheduler.class);
        MainActivity initial=main(c);check(master(initial)!=null&&!master(initial).isChecked(),"Settings has an off master switch");shot(initial,"master-off");
        toggle(initial,true);check(!ReportSettings.featureEnabled(c)&&!master(initial).isChecked(),"turning on waits for confirmation");
        check(initial.reviewFeatureDialog!=null&&message(initial.reviewFeatureDialog).contains("0目標")&&message(initial.reviewFeatureDialog).contains("API利用料"),"empty-target enable confirmation still explains cost");
        i.runOnMainSync(()->initial.reviewFeatureDialog.getButton(AlertDialog.BUTTON_NEGATIVE).performClick());i.waitForIdleSync();
        check(!ReportSettings.featureEnabled(c)&&scheduler.getPendingJob(3703)==null,"cancel enable keeps master off and no job");
        toggle(initial,true);ReportSettings.saveOptions(c,new ReportBudget.Options("gpt-6-astra",1000),Collections.emptyMap());accept(initial.reviewFeatureDialog);
        check(!ReportSettings.featureEnabled(c),"changed options invalidate enable confirmation");
        ReportSettings.saveOptions(c,ReportBudget.defaults(),Collections.emptyMap());
        i.runOnMainSync(initial::finish);i.waitForIdleSync();

        ReportSettingsActivity off=form(c);
        i.runOnMainSync(()->{
            views.findTag(decor(off),"gpt-6-astra").performClick();((SeekBar)views.findDescription(decor(off),"1回の上限金額")).setProgress(18);
            ((Switch)find(off,store.habit(first).goal())).setChecked(true);((Switch)find(off,store.habit(second).goal())).setChecked(true);
            ((EditText)views.findType(decor(off),EditText.class)).setText("qa-master-not-valid-api-key");
        });
        check(find(off,"継続レビュー：全体オフ（生成は停止中）")!=null,"form clearly labels globally off");views.shot(off,"master-form-off");click(off,"この設定を保存");
        check(off.isFinishing()&&off.consentDialog==null,"off form saves without fee confirmation");
        check(!ReportSettings.featureEnabled(c)&&ReportSettings.habitEnabled(c,first)&&ReportSettings.habitEnabled(c,second),"off save preserves selected targets without enabling feature");
        check(ReportSettings.configured(c)&&ReportSettings.options(c).limitYen()==1000,"key and options can be prepared while off");
        check(!ReportSettings.enabled(c)&&!ReportSettings.active(c,first)&&scheduler.getPendingJob(3703)==null,"configured off feature cannot generate or schedule");
        ReportSettingsActivity via=form(c);Instrumentation.ActivityMonitor monitor=i.addMonitor(ReportActivity.class.getName(),null,false);click(via,"保存してレビューを開く");
        Activity saved=i.waitForMonitorWithTimeout(monitor,5000);i.removeMonitor(monitor);i.waitForIdleSync();
        check(via.isFinishing()&&via.consentDialog==null&&saved!=null,"save-and-open also skips fee dialog when off");
        check(find(saved,"設定タブを開く")!=null&&find(saved,"今週のレポートを作る（有料）")==null,"manual generation unavailable while off");
        i.runOnMainSync(saved::finish);i.waitForIdleSync();
        MainActivity home=main(c);toggle(home,true);
        String warning=message(home.reviewFeatureDialog);
        check(warning.contains("2,000円")&&warning.contains("1000円 × 2目標")&&warning.contains(store.habit(first).goal())&&warning.contains("GPT-6 Astra"),"enable confirmation shows chosen targets/model/cap/weekly total");
        check(warning.contains("明日から")&&warning.contains("まとめて生成しません"),"enable explains no off-period catch-up");views.shotDialog(home.reviewFeatureDialog,"master-enable-confirmation");
        Switch beforeRefresh=master(home);c.sendBroadcast(new Intent("app.routia.CHANGED").setPackage(c.getPackageName()),c.getPackageName()+".INTERNAL");
        long refreshUntil=SystemClock.uptimeMillis()+2000;while(master(home)==beforeRefresh&&SystemClock.uptimeMillis()<refreshUntil)SystemClock.sleep(20);i.waitForIdleSync();
        check(master(home)!=beforeRefresh,"completion refresh can rebuild Settings while confirmation is open");accept(home.reviewFeatureDialog);
        check(ReportSettings.featureEnabled(c)&&ReportSettings.enabled(c)&&master(home).isChecked(),"confirmed enable persists and updates switch");
        JobInfo scheduled=scheduler.getPendingJob(3703);check(scheduled!=null&&scheduled.isPersisted()&&scheduled.getNetworkType()==JobInfo.NETWORK_TYPE_ANY,"master on schedules persisted network job");
        check(ReportSettings.enabledFrom(c,first).equals(today.plusDays(1)),"global enable starts automatic judgment tomorrow");shot(home,"master-on");
        try(ReportStore db=new ReportStore(c)){
            long queued=db.enqueue(first,"master-queued","QA","{}");
            long counting=db.enqueue(second,"master-counting","QA","{}");db.claim(counting);db.recordOptions(counting,ReportSettings.options(c));
            long manual=db.enqueue(first,"master-manual","QA","{}");db.confirmManual(manual,ReportSettings.options(c));
            long sending=db.enqueue(second,"master-sending","QA","{}");db.claim(sending);db.sending(sending,20);
            long complete=db.enqueue(first,"master-complete","QA",ReportData.snapshot(store,store.habit(first),today,db).toString());db.finish(complete,"complete",ReportReview.personalFixture().toString(),"",2);
            toggle(home,false);
            check(!ReportSettings.featureEnabled(c)&&!master(home).isChecked()&&home.reviewFeatureDialog==null,"off is immediate without fee dialog");
            check(scheduler.getPendingJob(3703)==null,"master off cancels scheduled work");
            check(ReportSettings.habitEnabled(c,first)&&ReportSettings.habitEnabled(c,second)&&ReportSettings.configured(c),"off preserves individual choices and encrypted key");
            check(db.item(queued).state().equals("stopped")&&db.item(counting).state().equals("stopped")&&db.item(manual).state().equals("stopped"),"off cancels automatic/manual queued and counting requests");
            check(!db.sending(counting,100),"cancelled token preflight cannot reach paid submission");
            check(db.item(sending).state().equals("sending")&&db.item(sending).reserved()==20,"already submitted request remains explicitly pending");
            check(db.item(complete).state().equals("complete")&&!db.item(complete).result().isEmpty(),"saved report survives master off");
            int rows=db.all(first).size()+db.all(second).size();
            try{ReportRunner.enqueue(c,store.habit(first));throw new AssertionError("off manual request accepted");}catch(IllegalArgumentException expected){n++;}
            ReportRunner.evaluate(c);check(db.all(first).size()+db.all(second).size()==rows,"off automatic evaluation creates nothing");
            CountDownLatch done=new CountDownLatch(1);ReportRunner.work(c,done::countDown);check(done.await(3,TimeUnit.SECONDS),"off worker finishes without network work");
            check(db.item(queued).state().equals("stopped")&&db.item(manual).reserved()==0,"off worker never revives cancelled requests");
            Activity reader=i.startActivitySync(ReportActivity.intent(c,first,complete).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));i.waitForIdleSync();
            check(find(reader,ReportReview.personalFixture().getString("headline"))!=null,"saved review still readable while globally off");i.runOnMainSync(reader::finish);i.waitForIdleSync();
            ReportSettings.prefs(c).edit().putString("habit_from_"+first,today.minusDays(30).toString()).commit();
            toggle(home,true);accept(home.reviewFeatureDialog);ReportRunner.evaluate(c);
            check(ReportSettings.enabledFrom(c,first).equals(today.plusDays(1)),"resume resets activation rather than billing old missed days");
            check(db.all(first).size()+db.all(second).size()==rows&&db.item(queued).state().equals("stopped"),"reenable does not replay cancelled or off-period work");
        }
        Activity manualGate=i.startActivitySync(ReportActivity.intent(c,first).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));i.waitForIdleSync();click(manualGate,"今週のレポートを作る（有料）");
        AlertDialog manualConsent=((ReportActivity)manualGate).consentDialog;check(manualConsent!=null,"manual cost confirmation opens while master on");
        ReportSettings.setFeatureEnabled(c,false);accept(manualConsent);
        try(ReportStore db=new ReportStore(c)){check(!db.hasWeek(first,Rules.week(today).toString()),"turning master off during manual confirmation prevents enqueue");}
        i.runOnMainSync(manualGate::finish);i.waitForIdleSync();i.runOnMainSync(home::finish);i.waitForIdleSync();
        MainActivity resumed=main(c);toggle(resumed,true);accept(resumed.reviewFeatureDialog);i.runOnMainSync(resumed::finish);i.waitForIdleSync();
        ReportSettingsActivity on=form(c);click(on,"この設定を保存");check(!on.isFinishing()&&on.consentDialog!=null,"on settings save requires cost confirmation");
        i.runOnMainSync(()->on.consentDialog.getButton(AlertDialog.BUTTON_NEGATIVE).performClick());i.waitForIdleSync();check(!on.isFinishing(),"cancel on save stays on form");
        click(on,"この設定を保存");accept(on.consentDialog);check(on.isFinishing(),"confirmed on settings save succeeds");
        ReportSettingsActivity changing=form(c);click(changing,"この設定を保存");ReportSettings.setFeatureEnabled(c,false);accept(changing.consentDialog);
        check(changing.isFinishing()&&!ReportSettings.featureEnabled(c),"accepting an old settings dialog never turns master back on");
        MainActivity restored=main(c);MainActivity recreated=(MainActivity)views.recreatePage(restored);
        check(!master(recreated).isChecked()&&((TextView)views.findTag(decor(recreated),"review_feature_status")).getText().toString().contains("全体オフ"),"master off survives activity recreation");i.runOnMainSync(recreated::finish);i.waitForIdleSync();
        String legacyFrom=today.minusDays(20).toString();ReportSettings.prefs(c).edit().remove("feature_enabled").putBoolean("habit_enabled_"+first,true).putString("habit_from_"+first,legacyFrom).commit();
        check(ReportSettings.featureEnabled(c)&&ReportSettings.enabledFrom(c,first).toString().equals(legacyFrom),"upgrade preserves existing approved active target and activation date");
        ReportSettings.setFeatureEnabled(c,false);check(!ReportSettings.featureEnabled(c)&&ReportSettings.habitEnabled(c,first),"explicit off overrides legacy active targets");
        ReportSettings.saveOptions(c,ReportSettings.options(c),Map.of(first,false,second,false));
        MainActivity empty=main(c);toggle(empty,true);accept(empty.reviewFeatureDialog);check(ReportSettings.featureEnabled(c)&&!ReportSettings.enabled(c)&&scheduler.getPendingJob(3703)==null,"on without selected targets performs no paid work");
        toggle(empty,false);i.runOnMainSync(empty::finish);i.waitForIdleSync();
        return n;
    }
}
