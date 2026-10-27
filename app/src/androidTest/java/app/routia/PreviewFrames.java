package app.routia;
import android.app.*;
import android.os.*;
import android.content.*;
import android.graphics.*;
import android.view.*;
import android.widget.*;
import java.time.*;
import java.io.*;
/** Repeatable visual fixtures, guarded to prevent writes on physical devices. */
final class PreviewFrames {
    static View find(View v,String label){if(v instanceof TextView&&label.contentEquals(((TextView)v).getText()))return v;if(v instanceof ViewGroup){ViewGroup g=(ViewGroup)v;for(int i=0;i<g.getChildCount();i++){View result=find(g.getChildAt(i),label);if(result!=null)return result;}}return null;}
    static ScrollView scroll(View v){if(v instanceof ScrollView)return (ScrollView)v;if(v instanceof ViewGroup){ViewGroup g=(ViewGroup)v;for(int i=0;i<g.getChildCount();i++){ScrollView result=scroll(g.getChildAt(i));if(result!=null)return result;}}return null;}
    static int wideCharts(View v){int n=0;if(v instanceof HorizontalScrollView){HorizontalScrollView h=(HorizontalScrollView)v;if(h.getChildAt(0).getWidth()>h.getWidth()*2)n++;}if(v instanceof ViewGroup){ViewGroup g=(ViewGroup)v;for(int i=0;i<g.getChildCount();i++)n+=wideCharts(g.getChildAt(i));}return n;}
    static void capture(Instrumentation i,String name)throws Exception{SystemClock.sleep(500);Bitmap b=i.getUiAutomation().takeScreenshot();try(FileOutputStream out=new FileOutputStream(new File(i.getTargetContext().getExternalFilesDir(null),name+".png"))){b.compress(Bitmap.CompressFormat.PNG,100,out);}b.recycle();}
    static void run(Instrumentation i)throws Exception{
        if(!Build.HARDWARE.contains("ranchu")&&!Build.HARDWARE.contains("goldfish"))throw new AssertionError("Emulator only");Context c=i.getTargetContext();Store s=Store.get(c);if(s.habits().isEmpty())throw new AssertionError("Run emulator UI suite first");Store.Habit h=s.habits().get(0);s.saveHabit(h.id(),"",h.goal(),"1分だけ","time",30,"分");int n=0;for(Store.Entry e:s.entries(h.id(),h.created(),LocalDate.now()))s.getWritableDatabase().execSQL("UPDATE records SET amount=?,unit='分' WHERE habit_id=? AND day=?",new Object[]{1+(n++*7)%65,h.id(),e.day().toString()});for(int k=0;k<18;k++)s.getWritableDatabase().execSQL("INSERT OR REPLACE INTO records(habit_id,day,level,amount,unit,note) VALUES(?,?,2,?,'分',?)",new Object[]{h.id(),LocalDate.now().minusDays(k).toString(),15+k*2,"リスニングを練習。少しずつ聞き取れるようになった。"});
        Activity a=i.startActivitySync(new Intent(c,MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK));capture(i,"qa-v3-today");i.runOnMainSync(()->find(a.getWindow().getDecorView(),"成果").performClick());i.waitForIdleSync();SystemClock.sleep(500);if(wideCharts(a.getWindow().getDecorView())<2)throw new AssertionError("Long grass and chart must scroll instead of shrinking");i.runOnMainSync(()->scroll(a.getWindow().getDecorView()).scrollTo(0,Ui.dp(a,290)));capture(i,"qa-v3-history");i.runOnMainSync(()->scroll(a.getWindow().getDecorView()).scrollTo(0,Ui.dp(a,520)));capture(i,"qa-v3-chart");i.runOnMainSync(()->find(a.getWindow().getDecorView(),"今日").performClick());i.runOnMainSync(()->find(a.getWindow().getDecorView(),"記録を修正").performClick());capture(i,"qa-v3-edit");
    }
}
