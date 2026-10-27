package app.routia;

import android.app.*;
import android.content.*;
import android.database.sqlite.SQLiteDatabase;
import android.os.*;
import android.service.notification.StatusBarNotification;
import android.view.*;
import android.widget.*;
import java.time.*;
import java.io.*;
import org.json.*;

final class ReportFlowReview {
    final Instrumentation i;final ReportReview views;int n;
    ReportFlowReview(Instrumentation i){this.i=i;views=new ReportReview(i);}
    void check(boolean ok,String message){n++;if(!ok)throw new AssertionError(message);}
    View decor(Activity a){return a.getWindow().getDecorView();}
    View find(Activity a,String text){return views.find(decor(a),text);}
    void click(Activity a,String text){View v=find(a,text);check(v!=null,"control exists: "+text);i.runOnMainSync(v::performClick);i.waitForIdleSync();}
    int notices(Context c){int count=0;for(StatusBarNotification s:c.getSystemService(NotificationManager.class).getActiveNotifications())if(s.getTag()!=null&&s.getTag().startsWith("review:"))count++;return count;}
    void awaitNotices(Context c,int expected){
        long until=SystemClock.uptimeMillis()+3000;while(notices(c)!=expected&&SystemClock.uptimeMillis()<until)SystemClock.sleep(50);
        check(notices(c)==expected,"expected notification count "+expected+", actual "+notices(c));
    }
    int run()throws Exception{
        Context c=i.getTargetContext();
        check((Build.HARDWARE.contains("ranchu")||Build.HARDWARE.contains("goldfish"))&&c.getPackageName().endsWith(".reportqa"),"dedicated emulator only");
        Store store=Store.get(c);check(store.habits().isEmpty()&&!ReportSettings.configured(c),"empty QA app without credentials");
        check(!ReportNotices.allowed(c),"start without notification permission");
        LocalDate today=LocalDate.now();
        long habit=store.saveConfiguredHabit(0,"朝食後","表示テスト・英語の練習","短く取り組む","30分取り組む","余裕がある日だけ","time",30,"分");
        store.getWritableDatabase().execSQL("UPDATE habits SET created=? WHERE id=?",new Object[]{today.minusDays(40).toString(),habit});
        long second=store.saveConfiguredHabit(0,"帰宅後","表示テスト・ダンス","一歩","踊る","余裕があれば","check",0,"");
        try(ReportStore db=new ReportStore(c)){
            JSONObject snapshot=ReportData.snapshot(store,store.habit(habit),today,db);
            check(!snapshot.getJSONObject("progression").getBoolean("eligible"),"insufficient history blocks step-up");
            check(ReportData.INSTRUCTIONS.contains("eligible=false")&&ReportData.INSTRUCTIONS.contains("目標を自動変更せず"),"prompt respects progression decision and user choice");
            long auto=db.enqueue(habit,"auto-review","表示テスト・英語の練習",snapshot.toString());
            ReportRunner.complete(c,db,auto,ReportReview.personalFixture(),2);
            check(db.item(auto).state().equals("complete")&&db.item(auto).completedAt()>0,"completion timestamp saved before notification");
            check(!db.item(auto).notified()&&notices(c)==0,"permission denial leaves report complete and notice pending");
            long completedAt=db.item(auto).completedAt();
            ReportRunner.complete(c,db,auto,ReportReview.personalFixture(),2);
            check(db.item(auto).completedAt()==completedAt,"repeat completion does not change completion day");
            check(db.latestCompletedOn(habit,today).id()==auto,"Today uses completion day rather than queued week");
            check(db.latestCompletedOn(habit,today.minusDays(1))==null,"report absent on other days");

            Activity report=i.startActivitySync(ReportActivity.intent(c,habit,auto).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));i.waitForIdleSync();
            check(find(report,"要点")!=null&&find(report,ReportReview.personalFixture().getString("headline"))!=null,"AI review opens immediately readable");
            check(find(report,"継続レビューの共通設定")!=null&&find(report,"戻る")!=null,"review settings and Back remain accessible");
            check(find(report,"レポートを表示")==null,"disclosure is not attached to the wrong AI screen");
            click(report,"改善策");check(find(report,"あなたにこの提案をする理由")!=null,"AI advice is directly accessible");
            Activity rotated=views.recreatePage(report);
            check(find(rotated,"改善策").isSelected(),"AI review tab survives recreation");
            i.runOnMainSync(rotated::finish);i.waitForIdleSync();

            Activity main=i.startActivitySync(new Intent(c,MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));i.waitForIdleSync();
            check(find(main,"💰️ 今日の継続レビューを見る")!=null,"Today button exists without notification permission");
            views.shot(main,"today-review");
            click(main,"成果");
            check(find(main,"📈 日別の推移")==null&&views.findTag(decor(main),"summary_"+habit)!=null,"summaries always visible without inline charts");
            check(views.findTag(decor(main),"paid_review_hub")==null&&views.findTag(decor(main),"nav_review")!=null,"paid reports have a dedicated tab outside Achievements");
            Activity charts=views.openTaggedPage(main,"charts_"+habit,AchievementChartsActivity.class);
            check(find(charts,"記録のグラフ")!=null&&find(charts,"📈 日別の推移")!=null,"charts open on a dedicated screen");
            check(views.findTag(decor(charts),"paid_review_hub")==null,"chart screen does not mix paid report controls");
            i.runOnMainSync(charts::finish);i.waitForIdleSync();
            check(find(main,"📈 日別の推移")==null&&views.findTag(decor(main),"summary_"+habit)!=null,"return preserves visible summary and hidden charts");
            main=views.recreatePage(main);
            check(views.findTag(decor(main),"summary_"+habit)!=null&&find(main,"📈 日別の推移")==null,"summary separation survives recreation");
            View reviewTab=views.findTag(decor(main),"nav_review");i.runOnMainSync(reviewTab::performClick);i.waitForIdleSync();
            Activity readable=views.openTaggedPage(main,"saved_review_"+auto,ReportActivity.class);
            check(find(readable,"要点")!=null,"top paid report entry opens readable AI review");
            i.runOnMainSync(readable::finish);i.waitForIdleSync();
            // Keep the app visible while simulating a user tap on its notification.
            try(InputStream in=new ParcelFileDescriptor.AutoCloseInputStream(i.getUiAutomation().executeShellCommand("pm grant "+c.getPackageName()+" android.permission.POST_NOTIFICATIONS"))){while(in.read()!=-1){}}
            check(ReportNotices.allowed(c),"permission can be enabled independently of paid settings");
            ReportNotices.deliverPending(c);awaitNotices(c,1);
            check(db.item(auto).notified(),"automatic completion notification recorded");
            ReportNotices.deliverPending(c);awaitNotices(c,1);
            Notification notice=ReportNotices.notification(c,db.item(auto));
            if(Build.VERSION.SDK_INT>=31)check(notice.contentIntent.isImmutable(),"notification intent immutable");
            check(notice.visibility==Notification.VISIBILITY_PRIVATE,"review content private on lock screen");
            Intent route=ReportNotices.reportIntent(c,db.item(auto));
            check(route.getLongExtra("report",-1)==auto&&route.getLongExtra("habit",-1)==habit,"notification selects exact report and goal");
            db.enqueue(habit,"newer-unfinished","表示テスト・英語の練習",snapshot.toString());
            Instrumentation.ActivityMonitor monitor=i.addMonitor(ReportActivity.class.getName(),null,false);
            notice.contentIntent.send();Activity fromNotice=i.waitForMonitorWithTimeout(monitor,5000);i.removeMonitor(monitor);
            check(fromNotice!=null,"notification opens review");
            i.waitForIdleSync();
            check(views.findStartsWith(decor(fromNotice),"AIのアドバイス：生成済み")!=null,"notification does not open newer incomplete report");
            check(find(fromNotice,"要点")!=null,"notification entry opens readable saved review");
            i.runOnMainSync(fromNotice::finish);i.waitForIdleSync();i.runOnMainSync(main::finish);i.waitForIdleSync();
            check(!ReportSettings.configured(c),"opening notice never configures key or starts paid generation");

            for(int day=1;day<=30;day++)store.getWritableDatabase().execSQL("INSERT INTO records(habit_id,day,level,amount,unit,note) VALUES(?,?,2,30,'分','')",new Object[]{habit,today.minusDays(day).toString()});
            JSONObject mature=ReportData.snapshot(store,store.habit(habit),today,db);
            check(mature.getJSONObject("progression").getBoolean("eligible")&&mature.getJSONObject("progression").getDouble("proposed_target")==31,"stable milestone provides bounded candidate to model");
            long manual=db.enqueue(habit,"manual-review","表示テスト・英語の練習",mature.toString());
            db.confirmManual(manual,ReportBudget.defaults());ReportRunner.complete(c,db,manual,ReportReview.personalFixture(),2);
            awaitNotices(c,1);check(!db.item(manual).notified(),"manual generation does not send automatic-completion notice");
            check(!ReportData.snapshot(store,store.habit(habit),today,db).getJSONObject("progression").getBoolean("eligible"),"same milestone is not offered again");
            check(store.metric(habit).target()==30,"progression advice never changes configured target");
            long stopped=db.enqueue(habit,"failed-review","表示テスト・英語の練習","{}");
            db.finish(stopped,"stopped","","test",0);
            check(db.item(stopped).completedAt()==0&&db.latestCompletedOn(habit,today).id()==manual,"stopped jobs do not replace Today completed review");

            long yesterday=today.minusDays(1).atTime(12,0).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
            db.getWritableDatabase().execSQL("UPDATE reports SET completed_at=? WHERE state='complete'",new Object[]{yesterday});
            check(db.latestCompletedOn(habit,today)==null&&db.latestCompletedOn(habit,today.minusDays(1))!=null,"completion date respects local day boundary");
            Activity next=i.startActivitySync(new Intent(c,MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));i.waitForIdleSync();
            check(find(next,"💰️ 今日の継続レビューを見る")==null,"Today button disappears after completion day");
            i.runOnMainSync(next::finish);i.waitForIdleSync();
        }
        migration(c);
        views.reviewMainUi(c,habit);n+=views.n;
        return n;
    }
    void migration(Context c)throws Exception{
        String name="routia-reports-v3-flow-test.db";c.deleteDatabase(name);
        try(SQLiteDatabase old=SQLiteDatabase.openOrCreateDatabase(c.getDatabasePath(name),null)){
            old.execSQL("CREATE TABLE reports(id INTEGER PRIMARY KEY AUTOINCREMENT,habit INTEGER NOT NULL,week TEXT NOT NULL,goal TEXT NOT NULL,snapshot TEXT NOT NULL,result TEXT NOT NULL DEFAULT '',state TEXT NOT NULL DEFAULT 'queued',problem TEXT NOT NULL DEFAULT '',reserved REAL NOT NULL DEFAULT 0,estimated REAL NOT NULL DEFAULT 0,requested INTEGER NOT NULL DEFAULT 0,model TEXT NOT NULL DEFAULT '',budget INTEGER NOT NULL DEFAULT 0,detail TEXT NOT NULL DEFAULT '',created TEXT NOT NULL,UNIQUE(habit,week))");
            old.execSQL("INSERT INTO reports(habit,week,goal,snapshot,result,state,estimated,detail,created) VALUES(1,'old','QA','{}',?,'complete',5,'standard','old')",new Object[]{ReportReview.personalFixture().toString()});old.setVersion(3);
        }
        try(ReportStore db=new ReportStore(c,name)){
            ReportStore.Item legacy=db.all(1).get(0);
            check(legacy.state().equals("complete")&&legacy.estimated()==5&&legacy.detail().equals("standard"),"v3 report content, costs and depth preserved");
            check(legacy.completedAt()==0&&!legacy.notified()&&db.latestCompletedOn(1,LocalDate.now())==null,"old reports do not get invented completion dates or notices");
        }c.deleteDatabase(name);
    }
}
