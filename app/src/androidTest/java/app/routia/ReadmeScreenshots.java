package app.routia;

import android.app.*;
import android.content.*;
import android.graphics.*;
import android.os.*;
import android.view.*;
import android.widget.*;
import java.io.*;
import java.time.*;
import org.json.*;

/** Public documentation fixtures. Never run against a physical or production app. */
final class ReadmeScreenshots {
    final Instrumentation i;
    final ReportReview views;
    ReadmeScreenshots(Instrumentation i){this.i=i;views=new ReportReview(i);}
    View decor(Activity a){return a.getWindow().getDecorView();}
    void click(Activity a,String label){
        View v=views.find(decor(a),label);
        if(v==null)throw new AssertionError("Missing control: "+label);
        i.runOnMainSync(v::performClick);i.waitForIdleSync();
    }
    void nav(Activity a,String name){
        View v=views.findTag(decor(a),name);
        if(v==null)throw new AssertionError("Missing navigation");
        i.runOnMainSync(v::performClick);i.waitForIdleSync();
    }
    void capture(Activity a,String name)throws Exception{
        i.waitForIdleSync();SystemClock.sleep(650);
        Bitmap full=i.getUiAutomation().takeScreenshot();
        if(full==null)throw new AssertionError("Screenshot unavailable");
        final android.graphics.Rect[] bounds={null};
        i.runOnMainSync(()->{
            View v=decor(a);
            android.graphics.Insets insets=v.getRootWindowInsets().getInsets(WindowInsets.Type.systemBars()|WindowInsets.Type.displayCutout());
            bounds[0]=new android.graphics.Rect(insets.left,insets.top,full.getWidth()-insets.right,full.getHeight()-insets.bottom);
        });
        android.graphics.Rect r=bounds[0];
        Bitmap image=Bitmap.createBitmap(full,r.left,r.top,r.width(),r.height());
        File folder=new File(i.getTargetContext().getExternalFilesDir(null),"readme");folder.mkdirs();
        try(FileOutputStream out=new FileOutputStream(new File(folder,name+".png"))){image.compress(Bitmap.CompressFormat.PNG,100,out);}
        image.recycle();full.recycle();
    }
    void run()throws Exception{
        Context c=i.getTargetContext();
        if(!(Build.HARDWARE.contains("ranchu")||Build.HARDWARE.contains("goldfish"))||!c.getPackageName().endsWith(".reportqa"))
            throw new AssertionError("Dedicated emulator QA app only");
        Store s=Store.get(c);
        if(!s.habits().isEmpty()||ReportSettings.configured(c))throw new AssertionError("Fresh app without credentials required");
        AppColors.save(c,0);
        long reading=s.saveConfiguredHabit(0,"寝る前","本を読む","1ページ開く","10ページ読む","20ページ読む","check",0,"");
        long english=s.saveConfiguredHabit(0,"朝食のあと","英語を聞く","1分だけ聞く","15分聞く","30分聞く","time",15,"分");
        long stretch=s.saveConfiguredHabit(0,"お風呂のあと","ストレッチ","1回だけ","5回動く","10回動く","count",5,"回");
        LocalDate today=LocalDate.now();
        s.getWritableDatabase().execSQL("UPDATE habits SET created=?",new Object[]{today.minusDays(27).toString()});
        for(int day=1;day<=27;day++){
            if(day!=9&&day!=18)s.getWritableDatabase().execSQL("INSERT INTO records(habit_id,day,level,amount,unit,note) VALUES(?,?,?,NULL,'','')",new Object[]{reading,today.minusDays(day).toString(),day%5==0?1:day%4==0?3:2});
            if(day%6!=0)s.getWritableDatabase().execSQL("INSERT INTO records(habit_id,day,level,amount,unit,note) VALUES(?,?,2,?,'分','')",new Object[]{english,today.minusDays(day).toString(),8+(day*7)%22});
        }
        Activity a=i.startActivitySync(new Intent(c,MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK));
        capture(a,"today");
        click(a,"🏆 目標達成");
        SystemClock.sleep(4200);
        capture(a,"achievement");
        // Open the overview afresh; all saved records above are synthetic.
        i.runOnMainSync(()->Celebration.dismiss(a));
        nav(a,"nav_achievements");capture(a,"progress");
        Instrumentation.ActivityMonitor chartMonitor=i.addMonitor(AchievementChartsActivity.class.getName(),null,false);
        click(a,"グラフを見る");
        Activity chart=chartMonitor.waitForActivityWithTimeout(4000);i.removeMonitor(chartMonitor);
        if(chart==null)throw new AssertionError("Chart did not open");
        capture(chart,"calendar");
        i.runOnMainSync(chart::finish);i.waitForIdleSync();
        long saved;
        try(ReportStore db=new ReportStore(c)){
            saved=db.enqueue(reading,Rules.week(today).toString(),s.habit(reading).goal(),ReportData.snapshot(s,s.habit(reading),today,db).toString());
            JSONObject sample=ReportReview.personalFixture().put("headline","寝る前の読書を、小さく続ける");
            sample.getJSONArray("insights").getJSONObject(0).put("title","小さな達成も積み重ねに").put("detail","目標より少ない日にも記録があります。無理のない量で続けられるか、次の一週間で確かめましょう。");
            sample.getJSONArray("actions").getJSONObject(0).put("title","本を手の届く場所へ").put("step","寝る前に読む本を、枕元に1冊だけ置く").put("why","ミニ達成の日も続けられているので、読む量を増やす前に始めやすい準備を試すため。");
            db.finish(saved,"complete",sample.toString(),"",0);
        }
        Activity review=i.startActivitySync(ReportActivity.intent(c,reading,saved).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        capture(review,"review");
        i.runOnMainSync(review::finish);i.waitForIdleSync();
        nav(a,"nav_settings");
        click(a,"回数・時刻を変更");capture(a,"reminders");
        i.runOnMainSync(()->{if(((MainActivity)a).reminderDialog!=null)((MainActivity)a).reminderDialog.dismiss();});
        i.waitForIdleSync();
        AppColors.save(c,6);
        Instrumentation.ActivityMonitor darkMonitor=i.addMonitor(MainActivity.class.getName(),null,false);
        i.runOnMainSync(a::recreate);
        SystemClock.sleep(750);
        Activity dark=darkMonitor.waitForActivityWithTimeout(4000);i.removeMonitor(darkMonitor);
        if(dark==null)throw new AssertionError("Dark appearance did not open");
        nav(dark,"nav_today");capture(dark,"dark");
        if(ReportSettings.configured(c)||ReportSettings.featureEnabled(c))throw new AssertionError("Paid integration must stay off");
    }
}
