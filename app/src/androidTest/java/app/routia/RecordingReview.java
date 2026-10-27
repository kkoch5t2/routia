package app.routia;
import android.app.*;
import android.appwidget.*;
import android.content.*;
import android.graphics.*;
import android.os.*;
import android.view.*;
import android.view.inspector.WindowInspector;
import android.widget.*;
import java.time.*;
import java.io.*;
import java.util.*;
/** End-to-end numeric entry, real PendingIntent routing and original animation QA. Emulator only. */
final class RecordingReview {
    final Instrumentation i;final Context c;int checks;
    RecordingReview(Instrumentation i){this.i=i;c=i.getTargetContext();}
    void check(boolean ok,String message){checks++;if(!ok)throw new AssertionError(message);}
    View global(int id){for(View window:WindowInspector.getGlobalWindowViews()){View found=window.findViewById(id);if(found!=null&&found.isShown())return found;}return null;}
    View waitVisible(int id){long deadline=SystemClock.uptimeMillis()+3000;while(SystemClock.uptimeMillis()<deadline){final View[] found={null};i.runOnMainSync(()->found[0]=global(id));if(found[0]!=null)return found[0];SystemClock.sleep(50);}return null;}
    boolean contains(View root,String label){if(root instanceof TextView&&((TextView)root).getText().toString().contains(label))return true;if(root instanceof ViewGroup)for(int n=0;n<((ViewGroup)root).getChildCount();n++)if(contains(((ViewGroup)root).getChildAt(n),label))return true;return false;}
    RecordActivity open(long id){return (RecordActivity)i.startActivitySync(RecordActivity.intent(c,id));}
    void set(RecordActivity activity,String value){i.runOnMainSync(()->{activity.amount.setText(value);activity.save.performClick();});i.waitForIdleSync();}
    void closeWin(){i.runOnMainSync(()->{View close=global(R.id.celebration_continue);if(close!=null)close.performClick();});i.waitForIdleSync();SystemClock.sleep(150);}
    int run()throws Exception{
        if(!Build.HARDWARE.contains("ranchu")&&!Build.HARDWARE.contains("goldfish"))throw new AssertionError("Emulator only");
        Store s=Store.get(c);check(s.habits().isEmpty(),"Run with empty emulator app");LocalDate day=LocalDate.now();
        long timed=s.saveHabit(0,"","英語を聞く","1文","time",15,"分"),count=s.saveHabit(0,"","近所を歩く","外に出る","count",1,"km"),simple=s.saveHabit(0,"","本を開く","1行","check",0,"");
        try{
            try{s.record(timed,day,2,false);throw new AssertionError("Grade-only numeric action accepted");}catch(IllegalArgumentException expected){checks++;}
            check(s.level(timed,day)==0,"Rejected numeric action cannot complete day");
            i.runOnMainSync(()->new ActionReceiver().onReceive(c,new Intent("app.routia.RECORD").putExtra("habit",timed).putExtra("level",2).putExtra("day",day.toString())));
            check(s.level(timed,day)==0,"Old notification action cannot fabricate numeric achievement");
            for(boolean compact:new boolean[]{false,true})i.runOnMainSync(()->{
                View row=HabitWidget.row(c,s.habit(timed),day,false,compact).apply(c,new FrameLayout(c));
                check(!contains(row,"最低")&&!contains(row,"通常"),"Numeric widget has no grade controls");
                check(row.findViewById(R.id.row_full_button)==null,"Numeric widget has one input action");
                check(contains(row,"未記録"),"Unrecorded amount is not fake zero");
            });
            RecordActivity input=open(timed);i.waitForIdleSync();SystemClock.sleep(500);
            check(UxReview.type(input.getWindow().getDecorView(),RadioGroup.class)==null,"Numeric entry has no achievement picker");
            check(input.amount.getTextSize()<Ui.dp(c,21),"Input amount is subordinate to habit title");check(input.note.getVisibility()==View.GONE,"Memo stays optional and collapsed");
            Rect buttonBounds=new Rect(),visibleBounds=new Rect();input.save.getGlobalVisibleRect(buttonBounds);input.getWindow().getDecorView().getWindowVisibleDisplayFrame(visibleBounds);check(buttonBounds.height()>=Ui.dp(c,48)&&buttonBounds.bottom<=visibleBounds.bottom,"Save stays visible above keyboard");
            PreviewFrames.capture(i,"qa-070-one-field");
            set(input,"");check(s.level(timed,day)==0&&input.amount.getError()!=null,"Empty input keeps day unachieved");
            set(input,"-1");check(s.level(timed,day)==0,"Negative input cannot record");
            set(input,"10");check(s.entry(timed,day).amount()==10&&s.level(timed,day)==1,"One number saves amount and derives success");
            check(s.records(timed,day,day).size()==1,"Exactly one successful day");
            SystemClock.sleep(300);check(global(R.id.celebration_scene)==null,"Below-target numeric record has no scene");
            RecordActivity goalInput=open(timed);set(goalInput,"15");SystemClock.sleep(2900);check(global(R.id.celebration_scene)!=null,"Meeting numeric target displays articulated Ruti scene");
            exportAnimation();closeWin();reviewHierarchy(s,timed);SystemClock.sleep(200);s.recordAmount(timed,day,10,null,false);
            RecordActivity repeated=open(timed);set(repeated,"10");check(s.entry(timed,day).amount()==10&&s.records(timed,day,day).size()==1,"Repeated total does not append");
            RecordActivity fraction=open(count);set(fraction,"0.25");SystemClock.sleep(3200);check(s.entry(count,day).amount()==.25&&Ui.number(.25).equals("0.25"),"Fractional quantities remain precise");closeWin();
            RecordActivity zero=open(count);check(global(R.id.amount_remove)!=null,"Numeric edit exposes explicit record removal");i.runOnMainSync(zero::remove);check(s.level(count,day)==0&&s.entry(count,day).amount()==null,"Zero removes success");
            Focus.start(c,timed,1);Focus.prefs(c).edit().putLong("end",SystemClock.elapsedRealtime()-1).commit();
            check(Focus.complete(c),"Timer completion succeeds");check(!Focus.complete(c),"Duplicate timer completion is ignored");
            check(s.entry(timed,day).amount()==11&&s.records(timed,day,day).size()==1,"Timer appends once without grade selection");Focus.clear(c,false);
            s.recordAmount(timed,day,15,null,false);check(s.level(timed,day)==2,"Meeting target is derived automatically");
            s.recordAmount(timed,day,8,"メモを保持",false);check(s.level(timed,day)==1&&s.entry(timed,day).amount()==8,"Explicit correction can reduce a numeric total");
            s.recordAmount(timed,day,1,null,true);check(s.entry(timed,day).amount()==9&&s.entry(timed,day).note().equals("メモを保持"),"Background append preserves note");
            check(ProgressWidgetData.summary(ProgressWidget.snapshot(c).stream().filter(item->item.habit().id()==timed).findFirst().orElseThrow().report()).equals("🔥 連続1日 / ✅ 累計1日"),"Results summarize a single actual day");
            String description=ProgressWidget.weekDescription(ProgressWidget.snapshot(c).stream().filter(item->item.habit().id()==timed).findFirst().orElseThrow().report().week(),s.metric(timed));
            check(description.contains("9分")&&!description.contains("通常達成"),"Numeric result graph describes actual units");
            routeWidget(s,timed,simple,day);
            // Reduced motion must render its final artwork without delaying either action.
            float old=android.provider.Settings.Global.getFloat(c.getContentResolver(),android.provider.Settings.Global.ANIMATOR_DURATION_SCALE,1);
            i.getUiAutomation().adoptShellPermissionIdentity("android.permission.WRITE_SECURE_SETTINGS");
            try{android.provider.Settings.Global.putFloat(c.getContentResolver(),android.provider.Settings.Global.ANIMATOR_DURATION_SCALE,0);Activity a=i.startActivitySync(new Intent(c,MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK));s.recordAmount(timed,day,15,null,false);i.runOnMainSync(()->Celebration.show(a,s.habit(timed),2,false,null));SystemClock.sleep(300);check(((Celebration.Scene)global(R.id.celebration_scene)).phase==1,"Reduced motion shows final still");closeWin();}
            finally{android.provider.Settings.Global.putFloat(c.getContentResolver(),android.provider.Settings.Global.ANIMATOR_DURATION_SCALE,old);i.getUiAutomation().dropShellPermissionIdentity();}
            return checks;
        }finally{if(Focus.exists(c))Focus.clear(c,true);s.delete(timed);s.delete(count);s.delete(simple);}
    }
    void routeWidget(Store s,long numeric,long simple,LocalDate day)throws Exception{
        AppWidgetHost host=new AppWidgetHost(c,4670);int id=host.allocateAppWidgetId();AppWidgetManager manager=AppWidgetManager.getInstance(c);
        i.getUiAutomation().adoptShellPermissionIdentity("android.permission.BIND_APPWIDGET");
        try{check(manager.bindAppWidgetIdIfAllowed(id,new ComponentName(c,HabitWidget.class)),"Bind own QA widget");}finally{i.getUiAutomation().dropShellPermissionIdentity();}
        try{
            Activity visible=i.startActivitySync(new Intent(c,MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK));PendingIntent template=WidgetCollections.recordTemplate(c,id);
            check(template.isActivity(),"Widget input is a direct activity pending intent");
            template.send(c,0,new Intent().putExtra("habit",numeric).putExtra("level",1).putExtra("shownDay",day.toString()));SystemClock.sleep(500);
            check(waitVisible(R.id.amount_input)!=null,"Actual widget pending intent opens one-field input");
            check(s.entry(numeric,day).amount()==9,"Opening input does not alter stored amount");
            i.getUiAutomation().performGlobalAction(android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_BACK);SystemClock.sleep(150);
            i.getUiAutomation().performGlobalAction(android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_BACK);SystemClock.sleep(150);
            i.startActivitySync(new Intent(c,MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK));
            template.send(c,0,new Intent().putExtra("habit",simple).putExtra("level",2).putExtra("shownDay",day.toString()));SystemClock.sleep(350);
            check(s.level(simple,day)==2,"Check-only widget retains one-tap recording");check(waitVisible(R.id.celebration_scene)!=null,"Widget goal action displays achievement without main navigation");closeWin();
            template.send(c,0,new Intent().putExtra("habit",numeric).putExtra("level",2).putExtra("shownDay",day.minusDays(1).toString()));SystemClock.sleep(250);
            check(s.entry(numeric,day).amount()==9,"Stale widget date cannot write numeric data");
        }finally{host.deleteAppWidgetId(id);}
    }
    void reviewHierarchy(Store s,long id)throws Exception{
        int previous=AppColors.selected(c);
        try{for(int palette:new int[]{0,6}){
            AppColors.save(c,palette);Activity owner=i.startActivitySync(new Intent(c,MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK));
            i.runOnMainSync(()->Celebration.show(owner,s.habit(id),2,false,null));waitVisible(R.id.celebration_scene);SystemClock.sleep(3900);
            TextView streak=(TextView)global(R.id.celebration_streak),total=(TextView)global(R.id.celebration_total);View week=global(R.id.celebration_week);
            check(streak!=null&&streak.getText().toString().equals("🔥 1日連続"),"Achievement screen shows the actual current streak");
            check(streak.getTextSize()>=Ui.dp(c,30)&&streak.getTypeface().isBold(),"Streak is large and bold");
            check(total!=null&&total.getText().toString().equals("累計 1日達成"),"Lifetime total retains actual achieved days");
            check(total.getTextSize()<=Ui.dp(c,12)&&total.getTextSize()<streak.getTextSize()*.5f,"Lifetime total is visually subordinate");
            check(total.getTop()>=week.getBottom(),"Lifetime total sits below the weekday ribbon");
            Rect visible=new Rect();check(total.getGlobalVisibleRect(visible)&&visible.height()==total.getHeight(),"Small total is fully visible above the footer");
            new CriteriaReview(i).capture(streak.getRootView(),palette==6?"qa-094-celebration-black":"qa-094-celebration-cream");closeWin();i.runOnMainSync(owner::finish);
        }}finally{AppColors.save(c,previous);}
    }
    void exportAnimation()throws Exception{
        File directory=new File(c.getExternalFilesDir(null),"celebration-preview");directory.mkdirs();
        Bitmap screen=i.getUiAutomation().takeScreenshot();try(FileOutputStream out=new FileOutputStream(new File(directory,"screen.png"))){screen.compress(Bitmap.CompressFormat.PNG,100,out);}screen.recycle();
        final Throwable[] error={null};
        i.runOnMainSync(()->{try{
            Celebration.Scene scene=(Celebration.Scene)global(R.id.celebration_scene);check(scene!=null,"Scene exists for deterministic rendering");
            int[] position=new int[2];scene.getLocationOnScreen(position);int width=480,height=Math.max(1,Math.round(scene.getHeight()*width/(float)scene.getWidth()));
            try(FileWriter out=new FileWriter(new File(directory,"bounds.json"))){out.write("{\"x\":"+position[0]+",\"y\":"+position[1]+",\"width\":"+scene.getWidth()+",\"height\":"+scene.getHeight()+"}");}
            for(int frame=0;frame<114;frame++){scene.phase=frame/113f;Bitmap image=Bitmap.createBitmap(width,height,Bitmap.Config.ARGB_8888);Canvas canvas=new Canvas(image);canvas.drawColor(scene.dark?0xff10141F:0xffFFF8E9);canvas.scale(width/(float)scene.getWidth(),height/(float)scene.getHeight());scene.draw(canvas);try(FileOutputStream out=new FileOutputStream(new File(directory,String.format(java.util.Locale.ROOT,"frame-%03d.png",frame)))){image.compress(Bitmap.CompressFormat.PNG,100,out);}image.recycle();}scene.phase=1;scene.invalidate();
        }catch(Throwable t){error[0]=t;}});
        if(error[0]!=null)throw new AssertionError(error[0]);
    }
}
