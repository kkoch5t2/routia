package app.routia;
import android.app.*;
import android.content.*;
import android.graphics.Rect;
import android.os.*;
import android.view.*;
import android.view.inspector.WindowInspector;
import android.widget.*;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;
final class TicketReview {
    final Instrumentation i;final UxReview ui;final Store s;int n;
    TicketReview(Instrumentation i){this.i=i;ui=new UxReview(i);s=Store.get(i.getTargetContext());}
    void check(boolean ok,String label){n++;if(!ok)throw new AssertionError(label);}
    boolean contains(View root,String text){if(root instanceof TextView&&((TextView)root).getText().toString().contains(text))return true;if(root instanceof ViewGroup)for(int at=0;at<((ViewGroup)root).getChildCount();at++)if(contains(((ViewGroup)root).getChildAt(at),text))return true;return false;}
    View form(){for(View root:WindowInspector.getGlobalWindowViews())if(PreviewFrames.find(root,"変更を保存")!=null)return root;throw new AssertionError("No correction dialog");}
    void tapDay(GrassView grass,LocalDate day)throws Exception{
        int index=(int)ChronoUnit.DAYS.between(grass.start,day);float x,y;int size;
        if(grass.layoutMode>0){int offset=grass.layoutMode==1?0:grass.start.getDayOfWeek().getValue()%7;int col=(offset+index)%7,row=(offset+index)/7;size=grass.cell;x=grass.d(col*size+size/2f);y=grass.d(26+row*size+size/2f);}
        else{int col=(int)ChronoUnit.DAYS.between(grass.sunday,day)/7,row=day.getDayOfWeek().getValue()%7;size=GrassView.STEP;x=grass.d(2+col*size+size/2f);y=grass.d(GrassView.HEADER+row*size+size/2f);}
        final float localX=x,localY=y;final int margin=Ui.dp(ui.a,size/2f);
        i.runOnMainSync(()->grass.requestRectangleOnScreen(new Rect((int)localX-margin,(int)localY-margin,(int)localX+margin,(int)localY+margin),false));i.waitForIdleSync();SystemClock.sleep(400);
        int[] point=new int[2];grass.getLocationOnScreen(point);long now=SystemClock.uptimeMillis();
        i.sendPointerSync(MotionEvent.obtain(now,now,MotionEvent.ACTION_DOWN,point[0]+x,point[1]+y,0));i.sendPointerSync(MotionEvent.obtain(now,now+60,MotionEvent.ACTION_UP,point[0]+x,point[1]+y,0));
    }
    int run()throws Exception{
        if(!Build.HARDWARE.contains("ranchu")&&!Build.HARDWARE.contains("goldfish"))throw new AssertionError("Emulator only");check(s.habits().isEmpty(),"Empty test app");
        LocalDate today=LocalDate.now();long id=s.saveHabit(0,"","読書","1ページ","count",10,"ページ");
        s.getWritableDatabase().execSQL("UPDATE habits SET created=? WHERE id=?",new Object[]{today.minusDays(4).toString(),id});
        for(int offset:new int[]{4,3})s.getWritableDatabase().execSQL("INSERT INTO records(habit_id,day,level) VALUES(?,?,2)",new Object[]{id,today.minusDays(offset).toString()});
        s.record(id,today,2,false);s.details(id,today,20d,"今日読んだ本のメモ");long session=s.newSession(id);s.finishSession(session,60,true);
        s.getWritableDatabase().execSQL("UPDATE ticket_state SET next_day=? WHERE habit_id=?",new Object[]{today.minusDays(2).toString(),id});
        ui.a=i.startActivitySync(new Intent(i.getTargetContext(),MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK));i.waitForIdleSync();
        check(s.ticketDays(id).equals(Set.of(today.minusDays(2),today.minusDays(1))),"App opening automatically consumes tickets");
        check(Progress.streak(s.records(id,today.minusDays(4),today).keySet(),s.ticketDays(id),today)==3,"Rest days preserve but do not increase streak");ui.await("🔥 3日連続！");
        ui.mainTap("成果");TextView streak=(TextView)PreviewFrames.find(ui.a.getWindow().getDecorView(),"🔥 3日連続！");check(streak.getTextSize()/ui.a.getResources().getDisplayMetrics().scaledDensity<=20.1f,"History streak is smaller");
        check(!contains(ui.a.getWindow().getDecorView(),"この期間の記録"),"History has no duplicate record list");
        check(s.records(id,today.minusDays(4),today).size()==3,"Success totals exclude tickets");
        LocalDate protectedDay=today.minusDays(1);if(protectedDay.getMonthValue()!=today.getMonthValue()){ui.tap("月");ui.tap("年");if(protectedDay.getYear()!=today.getYear())ui.mainTap("◀ 前年");}
        GrassView grass=UxReview.type(ui.a.getWindow().getDecorView(),GrassView.class);int index=(int)ChronoUnit.DAYS.between(grass.start,protectedDay);check(grass.shades[index]==GrassView.TICKET,"Protected day has distinct calendar shade");
        i.runOnMainSync(()->grass.requestRectangleOnScreen(new Rect(0,0,Math.min(grass.getWidth(),Ui.dp(ui.a,280)),grass.getHeight()),false));ui.shot("ticket-calendar");
        tapDay(grass,protectedDay);ui.await(protectedDay.toString());ui.await("🎟 継続チケットを自動で1枚使用しました。\n連続記録を守ったお休みの日です。達成日数は増えず、達成率も変わりません。");ui.tap("確認した");
        ui.mainTap("今日");ui.mainTap("管理");check(ui.find("今日の記録を取り消す")==null,"Manage no longer contains record cancellation");ui.tap("閉じる");
        ui.mainTap("記録を修正");ui.choose("最低ゴール達成");ui.tap("変更を保存");check(s.entry(id,today).level()==1&&s.entry(id,today).amount()==20d&&s.entry(id,today).note().equals("今日読んだ本のメモ"),"Normal correction preserves amount and note");
        ui.mainTap("記録を修正");i.runOnMainSync(()->PreviewFrames.scroll(form()).fullScroll(View.FOCUS_DOWN));i.waitForIdleSync();SystemClock.sleep(300);
        Rect cancel=new Rect(),save=new Rect();PreviewFrames.find(form(),"今日の記録を取り消す").getGlobalVisibleRect(cancel);PreviewFrames.find(form(),"変更を保存").getGlobalVisibleRect(save);check(save.top-cancel.bottom>=Ui.dp(ui.a,16),"Cancellation separated from save");ui.shot("ticket-correction");
        ui.tap("今日の記録を取り消す");check(s.level(id,today)==1,"Opening cancellation does not delete record");ui.tap("閉じる");check(s.level(id,today)==1,"Dismissing confirmation preserves record");
        ui.tap("今日の記録を取り消す");ui.tap("取り消す");check(s.level(id,today)==0&&s.entry(id,today).amount()==null&&s.entry(id,today).note().isEmpty(),"Confirmed cancellation clears achievement and details");
        check(s.focused(id,today,today)==60,"Completed timer history kept");check(s.ticketDays(id).size()==2&&!s.ticket(id,today),"No ticket spent on unfinished today");ui.await("🔥 2日連続！");
        return n+ui.checks;
    }
}
