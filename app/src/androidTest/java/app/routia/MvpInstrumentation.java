package app.routia;
import android.app.*;
import android.content.*;
import android.database.sqlite.*;
import android.os.Bundle;
import android.os.Build;
import java.time.*;

public class MvpInstrumentation extends UiSmokeInstrumentation {
    private int checks=0;
    private void check(boolean ok,String message){checks++;if(!ok)throw new AssertionError(message);}
    private boolean readme,capacity,reminder,master,paging,reviewHub,reviewFlow,prices,calendar,reports,ui,preview,ux,revision,tickets,appearance,weeklyWidget,recording,criteria,performance,rig;
    @Override public void onCreate(Bundle b){readme=b!=null&&"readme".equals(b.getString("suite"));capacity=b!=null&&"habit-capacity".equals(b.getString("suite"));reminder=b!=null&&"reminders".equals(b.getString("suite"));master=b!=null&&"review-master".equals(b.getString("suite"));paging=b!=null&&"report-paging".equals(b.getString("suite"));reviewHub=b!=null&&"review-hub".equals(b.getString("suite"));reviewFlow=b!=null&&"review-flow".equals(b.getString("suite"));prices=b!=null&&"prices".equals(b.getString("suite"));calendar=b!=null&&"calendar".equals(b.getString("suite"));reports=b!=null&&"reports".equals(b.getString("suite"));rig=b!=null&&"rig".equals(b.getString("suite"));performance=b!=null&&"performance".equals(b.getString("suite"));criteria=b!=null&&"criteria".equals(b.getString("suite"));recording=b!=null&&"recording".equals(b.getString("suite"));weeklyWidget=b!=null&&"widget-week".equals(b.getString("suite"));revision=b!=null&&"revision".equals(b.getString("suite"));ux=b!=null&&"ux".equals(b.getString("suite"));ui=b!=null&&"ui".equals(b.getString("suite"));preview=b!=null&&"preview".equals(b.getString("suite"));appearance=b!=null&&"appearance".equals(b.getString("suite"));tickets=b!=null&&"tickets".equals(b.getString("suite"));super.onCreate(b);}
    @Override public void onStart(){
        if(readme){Bundle out=new Bundle();try{new ReadmeScreenshots(this).run();out.putString("stream","PASS public README screenshots\n");finish(Activity.RESULT_OK,out);}catch(Throwable failure){android.util.Log.e("RoutiaQA","README screenshots",failure);out.putString("stream","FAIL readme: "+failure);finish(Activity.RESULT_CANCELED,out);}return;}
        if(capacity){Bundle out=new Bundle();try{int count=new HabitCapacityReview(this).run();out.putString("stream","PASS "+count+" habit capacity/migration/UI assertions\n");finish(Activity.RESULT_OK,out);}catch(Throwable failure){android.util.Log.e("RoutiaQA","Habit capacity",failure);out.putString("stream","FAIL habit-capacity: "+failure);finish(Activity.RESULT_CANCELED,out);}return;}
        if(reminder){Bundle out=new Bundle();try{int count=new ReminderReview(this).run();out.putString("stream","PASS "+count+" reminder count/time/alarm/UI assertions\n");finish(Activity.RESULT_OK,out);}catch(Throwable failure){android.util.Log.e("RoutiaQA","Reminders",failure);out.putString("stream","FAIL reminders: "+failure);finish(Activity.RESULT_CANCELED,out);}return;}
        if(master){Bundle out=new Bundle();try{int count=new ReportMasterReview(this).run();out.putString("stream","PASS "+count+" master switch/consent/queue assertions\n");finish(Activity.RESULT_OK,out);}catch(Throwable failure){android.util.Log.e("RoutiaQA","Review master",failure);out.putString("stream","FAIL review-master: "+failure);finish(Activity.RESULT_CANCELED,out);}return;}
        if(paging){Bundle out=new Bundle();try{int count=new ReportPagingReview(this).run();out.putString("stream","PASS "+count+" report paging/large-catalog assertions\n");finish(Activity.RESULT_OK,out);}catch(Throwable failure){android.util.Log.e("RoutiaQA","Report paging",failure);out.putString("stream","FAIL report-paging: "+failure);finish(Activity.RESULT_CANCELED,out);}return;}
        if(reviewHub){Bundle out=new Bundle();try{int count=new ReportHubReview(this).run();out.putString("stream","PASS "+count+" paid report hub/navigation assertions\n");finish(Activity.RESULT_OK,out);}catch(Throwable failure){android.util.Log.e("RoutiaQA","Review hub",failure);out.putString("stream","FAIL review-hub: "+failure);finish(Activity.RESULT_CANCELED,out);}return;}
        if(reviewFlow){Bundle out=new Bundle();try{int count=new ReportFlowReview(this).run();out.putString("stream","PASS "+count+" review flow/notification assertions\n");finish(Activity.RESULT_OK,out);}catch(Throwable failure){android.util.Log.e("RoutiaQA","Review flow",failure);out.putString("stream","FAIL review-flow: "+failure);finish(Activity.RESULT_CANCELED,out);}return;}

        if(prices){Bundle out=new Bundle();try{
            android.content.Context c=getTargetContext();
            if(!Build.HARDWARE.contains("ranchu")&&!Build.HARDWARE.contains("goldfish"))throw new AssertionError("Emulator only");
            if(!c.getPackageName().endsWith(".reportqa")||ReportSettings.configured(c)||!Store.get(c).habits().isEmpty())throw new AssertionError("Fresh dedicated QA app only");
            ReportReview review=new ReportReview(this);review.reviewPriceGuides(c);
            out.putString("stream","PASS "+review.n+" pricing UI assertions\n");finish(Activity.RESULT_OK,out);
        }catch(Throwable error){out.putString("stream","FAIL prices: "+error);finish(Activity.RESULT_CANCELED,out);}return;}

        if(calendar){Bundle out=new Bundle();try{
            if(!Build.HARDWARE.contains("ranchu")&&!Build.HARDWARE.contains("goldfish"))throw new AssertionError("Emulator only");
            if(!getTargetContext().getPackageName().endsWith(".reportqa"))throw new AssertionError("Dedicated QA app only");
            ReportReview review=new ReportReview(this);review.reviewCalendars(getTargetContext());
            out.putString("stream","PASS "+review.n+" calendar rendering assertions\n");finish(Activity.RESULT_OK,out);
        }catch(Throwable error){out.putString("stream","FAIL calendar: "+error);finish(Activity.RESULT_CANCELED,out);}return;}

        if(reports){Bundle out=new Bundle();try{int n=new ReportReview(this).run();out.putString("stream","PASS "+n+" report schema/storage/UI assertions\n");finish(Activity.RESULT_OK,out);}catch(Throwable t){android.util.Log.e("RoutiaQA","Report review",t);out.putString("stream","FAIL reports: "+t);finish(Activity.RESULT_CANCELED,out);}return;}

        if(rig){Bundle out=new Bundle();try{int n=new RigIntegrityReview(this).run();out.putString("stream","PASS "+n+" rendered-rig connectivity assertions\n");finish(Activity.RESULT_OK,out);}catch(Throwable t){android.util.Log.e("RoutiaQA","Rig integrity review",t);out.putString("stream","FAIL rig: "+t);finish(Activity.RESULT_CANCELED,out);}return;}

        if(performance){Bundle out=new Bundle();try{int n=new PerformanceReview(this).run();out.putString("stream","PASS "+n+" performance/palette/variety assertions\n");finish(Activity.RESULT_OK,out);}catch(Throwable t){android.util.Log.e("RoutiaQA","Performance review",t);out.putString("stream","FAIL performance: "+t);finish(Activity.RESULT_CANCELED,out);}return;}

        if(criteria){Bundle out=new Bundle();try{int n=new CriteriaReview(this).run();out.putString("stream","PASS "+n+" criteria/ordering/migration assertions\n");finish(Activity.RESULT_OK,out);}catch(Throwable t){android.util.Log.e("RoutiaQA","Criteria review",t);out.putString("stream","FAIL criteria: "+t);finish(Activity.RESULT_CANCELED,out);}return;}

        if(recording){Bundle out=new Bundle();try{int n=new RecordingReview(this).run();out.putString("stream","PASS "+n+" numeric recording/animation assertions\n");finish(Activity.RESULT_OK,out);}catch(Throwable t){android.util.Log.e("RoutiaQA","Recording review",t);out.putString("stream","FAIL recording: "+t);finish(Activity.RESULT_CANCELED,out);}return;}

        if(weeklyWidget){Bundle out=new Bundle();try{int n=new WeeklyWidgetReview(this).run();out.putString("stream","PASS "+n+" weekly widget render assertions\n");finish(Activity.RESULT_OK,out);}catch(Throwable t){out.putString("stream","FAIL weekly widget: "+t);finish(Activity.RESULT_CANCELED,out);}return;}

        if(appearance){Bundle out=new Bundle();try{int n=new AppearanceReview(this).run();out.putString("stream","PASS "+n+" appearance/widget assertions\n");finish(Activity.RESULT_OK,out);}catch(Throwable t){out.putString("stream","FAIL appearance: "+t);finish(Activity.RESULT_CANCELED,out);}return;}

        if(tickets){Bundle out=new Bundle();try{int n=new TicketReview(this).run();out.putString("stream","PASS "+n+" ticket/record UX assertions\n");finish(Activity.RESULT_OK,out);}catch(Throwable t){out.putString("stream","FAIL ticket UX: "+t);finish(Activity.RESULT_CANCELED,out);}return;}

        if(revision){Bundle out=new Bundle();try{int n=new RevisionReview(this).run();out.putString("stream","PASS "+n+" revision assertions\n");finish(Activity.RESULT_OK,out);}catch(Throwable t){out.putString("stream","FAIL revision: "+t);finish(Activity.RESULT_CANCELED,out);}return;}
        if(ux){Bundle out=new Bundle();try{int n=new UxReview(this).run();out.putString("stream","PASS "+n+" UX assertions\n");finish(Activity.RESULT_OK,out);}catch(Throwable t){out.putString("stream","FAIL UX: "+t);finish(Activity.RESULT_CANCELED,out);}return;}
        if(preview){Bundle out=new Bundle();try{PreviewFrames.run(this);out.putString("stream","PASS visual fixtures\n");finish(Activity.RESULT_OK,out);}catch(Throwable t){out.putString("stream","FAIL preview: "+t);finish(Activity.RESULT_CANCELED,out);}return;}
        if(ui){super.onStart();return;}
        Bundle result=new Bundle();
        // A separate test APK's context gives this suite its own database.
        Context isolated=getTargetContext();
        isolated.deleteDatabase("routia-integration.db");
        try{
            Store s=new Store(isolated,"routia-integration.db");
            long id=s.save(0,"歯みがきのあと","英語15分","1文",21,0,true);
            LocalDate today=LocalDate.now();
            check(s.level(id,today)==0,"Starts empty");
            check(!s.recovery(s.habit(id),today),"First day is not recovery");
            check(s.record(id,today,1,false).changed(),"Minimum records");
            check(!s.record(id,today,1,false).changed(),"Duplicate idempotent");
            check(s.record(id,today,2,false).level()==2,"Upgrade");
            check(s.record(id,today,1,false).level()==2,"No stale downgrade");
            check(s.records(id,today,today).size()==1,"Only one record per day");
            s.record(id,today,0,true);check(s.level(id,today)==0,"Undo");
            SQLiteDatabase db=s.getWritableDatabase();
            db.execSQL("UPDATE habits SET created=? WHERE id=?",new Object[]{today.minusDays(4).toString(),id});
            Store.Result recovery=s.record(id,today,1,false);check(recovery.recovery(),"Return is rewarded");
            check(!s.record(id,today,2,false).recovery(),"Recovery bonus once");
            try{s.record(id,today.minusDays(1),1,false);throw new AssertionError("Past action accepted");}catch(IllegalArgumentException expected){checks++;}
            long b=s.save(0,"帰宅後","走る","着替え",18,30,false);
            long c=s.save(0,"就寝前","読書","1ページ",22,0,false);
            for(int at=4;at<=10;at++)s.save(0,"","追加テスト"+at,"一歩",0,0,false);
            try{s.save(0,"a","b","c",0,0,false);throw new AssertionError("Eleventh habit accepted");}catch(IllegalArgumentException expected){checks++;}
            check(s.habits().size()==10,"Fixed limit of ten enforced in database");
            s.save(id,"朝食後","新しい英語","新しい1文",8,15,false);check(s.level(id,today)==2,"Editing preserves history");
            check(s.habit(id).hour()==8&&!s.habit(id).reminder(),"Reminder edits persist");
            check(!Reminders.action(isolated,id,1,today.toString(),"widget").equals(Reminders.action(isolated,b,1,today.toString(),"widget")),"Action identity distinct per habit");
            check(!Reminders.action(isolated,id,1,today.toString(),"widget").equals(Reminders.action(isolated,id,2,today.toString(),"widget")),"Action identity distinct per level");
            s.delete(id);check(s.records(id,today.minusDays(5),today).isEmpty(),"Delete cascades history");
            check(s.habits().size()==9,"Delete frees slot");
            checks+=new FeaturesTest().run(isolated);checks+=new GradeMigrationTest().run(isolated);checks+=new TicketStoreTest().run(isolated);
            result.putString("stream","PASS "+checks+" Android integration assertions\n");
            finish(Activity.RESULT_OK,result);
        }catch(Throwable t){result.putString("stream","FAIL after "+checks+": "+t+"\n");finish(Activity.RESULT_CANCELED,result);}
    }
}
