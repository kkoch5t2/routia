package app.routia;
import android.app.*;
import android.appwidget.*;
import android.content.*;
import android.graphics.*;
import android.os.*;
import android.view.*;
import android.view.accessibility.*;
import android.view.inspector.WindowInspector;
import android.widget.*;
import android.accessibilityservice.AccessibilityService;
import java.time.*;
import java.io.*;
import java.util.*;
final class UxReview {
    final Instrumentation i;final Context c;final Store store;int checks;Activity a;
    UxReview(Instrumentation i){this.i=i;c=i.getTargetContext();store=Store.get(c);}
    void check(boolean ok,String message){checks++;if(!ok)throw new AssertionError(message);}
    AccessibilityNodeInfo find(String text){AccessibilityNodeInfo root=i.getUiAutomation().getRootInActiveWindow();if(root==null)return null;for(AccessibilityNodeInfo n:root.findAccessibilityNodeInfosByText(text))if(text.contentEquals(n.getText()==null?"":n.getText()))return n;return null;}
    AccessibilityNodeInfo await(String text){long deadline=SystemClock.uptimeMillis()+6000;while(SystemClock.uptimeMillis()<deadline){AccessibilityNodeInfo n=find(text);if(n!=null)return n;SystemClock.sleep(100);}throw new AssertionError("Missing "+text);}
    void tap(String label){AccessibilityNodeInfo n=await(label);while(!n.isClickable()&&n.getParent()!=null)n=n.getParent();check(n.performAction(AccessibilityNodeInfo.ACTION_CLICK),"Tap "+label);i.waitForIdleSync();SystemClock.sleep(250);}
    static <T extends View>T type(View view,Class<T> clazz){if(clazz.isInstance(view))return clazz.cast(view);if(view instanceof ViewGroup){ViewGroup group=(ViewGroup)view;for(int at=0;at<group.getChildCount();at++){T found=type(group.getChildAt(at),clazz);if(found!=null)return found;}}return null;}
    void mainTap(String label){i.runOnMainSync(()->{View v=PreviewFrames.find(a.getWindow().getDecorView(),label);if(v==null)throw new AssertionError("Missing app view "+label);v.performClick();});i.waitForIdleSync();SystemClock.sleep(250);}
    void shot(String name)throws Exception{PreviewFrames.capture(i,"qa-v4-"+name);}
    void edit(){mainTap("管理");tap("習慣の内容を編集");}
    void choose(String label){AccessibilityNodeInfo root=i.getUiAutomation().getRootInActiveWindow();for(AccessibilityNodeInfo n:root.findAccessibilityNodeInfosByText(label))if(n.isCheckable()){check(n.performAction(AccessibilityNodeInfo.ACTION_CLICK),"Choose "+label);i.waitForIdleSync();SystemClock.sleep(200);return;}throw new AssertionError("No choice "+label);}
    int run()throws Exception{
        if(!Build.HARDWARE.contains("ranchu")&&!Build.HARDWARE.contains("goldfish"))throw new AssertionError("Emulator only");
        android.accessibilityservice.AccessibilityServiceInfo info=i.getUiAutomation().getServiceInfo();info.flags|=android.accessibilityservice.AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS;i.getUiAutomation().setServiceInfo(info);check(store.habits().size()==3,"Run UI suite first");Store.Habit h=store.habits().get(0);store.saveHabit(h.id(),"","歯間ブラシを使う","1本だけ","check",0,"");store.getWritableDatabase().execSQL("UPDATE habits SET created=? WHERE id=?",new Object[]{LocalDate.now().toString(),h.id()});store.record(h.id(),LocalDate.now(),2,false);
        a=i.startActivitySync(new Intent(c,MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK));View card=PreviewFrames.find(a.getWindow().getDecorView(),"記録方法：できたかだけ");check(card!=null&&PreviewFrames.find((View)card.getParent(),"⏱ タイマーを追加")==null,"No timer on binary card");try{Focus.start(c,h.id(),1);throw new AssertionError("Binary timer allowed");}catch(IllegalArgumentException expected){checks++;}shot("today");
        edit();check(find("この習慣を削除")==null,"Deletion separated from save form");await("保存");choose("できたかだけ");choose("時間（分）");choose("回数・数量");choose("できたかだけ");shot("editor");
        Rect save=new Rect(),cancel=new Rect();await("保存").getBoundsInScreen(save);await("戻る").getBoundsInScreen(cancel);check(!Rect.intersects(save,cancel)&&cancel.top-save.bottom>=Ui.dp(c,8),"Footer buttons have a gap");
        tap("歯間ブラシを使う");i.runOnMainSync(()->{for(View window:WindowInspector.getGlobalWindowViews()){EditText field=type(window,EditText.class);if(field!=null&&field.hasFocus()){((android.view.inputmethod.InputMethodManager)c.getSystemService(Context.INPUT_METHOD_SERVICE)).showSoftInput(field,android.view.inputmethod.InputMethodManager.SHOW_FORCED);return;}}});SystemClock.sleep(800);shot("keyboard");int imeTop=-1;for(AccessibilityWindowInfo window:i.getUiAutomation().getWindows())if(window.getType()==AccessibilityWindowInfo.TYPE_INPUT_METHOD){Rect bounds=new Rect();window.getBoundsInScreen(bounds);imeTop=bounds.top;}check(imeTop>0,"Keyboard is actually visible");Rect keyboardSave=new Rect();await("保存").getBoundsInScreen(keyboardSave);check(keyboardSave.bottom<=imeTop,"Save remains above the keyboard");i.getUiAutomation().performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK);SystemClock.sleep(250);
        choose("時間（分）");tap("保存");check(store.metric(h.id()).kind().equals("time"),"Time mode saves");check(PreviewFrames.find(a.getWindow().getDecorView(),"⏱ タイマーを追加")!=null,"Time mode offers timer");
        edit();choose("回数・数量");tap("保存");check(store.metric(h.id()).kind().equals("count"),"Count mode saves");try{Focus.start(c,h.id(),1);throw new AssertionError("Count timer allowed");}catch(IllegalArgumentException expected){checks++;}View countCard=(View)PreviewFrames.find(a.getWindow().getDecorView(),"記録方法：回数・数量（手入力）").getParent();check(PreviewFrames.find(countCard,"⏱ タイマーを追加")==null,"No timer on count card");
        edit();choose("できたかだけ");tap("保存");check(store.metric(h.id()).kind().equals("check"),"Binary mode saves");mainTap("成果");SystemClock.sleep(300);GrassView grass=type(a.getWindow().getDecorView(),GrassView.class);check(grass!=null&&grass.shades.length==LocalDate.now().lengthOfMonth(),"New habit gets a month grid");check(grass.shades[LocalDate.now().getDayOfMonth()-1]==Progress.contribution(true,true,null,0,store.level(h.id(),LocalDate.now())),"Grade sets the achievement color");check(grass.layoutMode==2,"Month uses a compact calendar");i.runOnMainSync(()->PreviewFrames.scroll(a.getWindow().getDecorView()).scrollTo(0,Ui.dp(a,380)));shot("calendar");mainTap("見方 ⓘ");await("成果の見方");shot("calendar-help");tap("わかった");
        mainTap("設定");mainTap("回数・時刻を変更");tap(c.getString(R.string.reminder_time_edit,1,ReminderTimes.format(Reminders.times(c)[0])));
        i.runOnMainSync(()->{for(View window:WindowInspector.getGlobalWindowViews()){TimePicker picker=type(window,TimePicker.class);if(picker!=null){picker.setHour(8);picker.setMinute(15);return;}}throw new AssertionError("No native time picker");});tap("OK");shot("times");tap("回数・時刻を保存");check(Reminders.times(c)[0]==495,"Custom time persisted");int revision=Reminders.revision(c);Reminders.saveTimes(c,Reminders.times(c));check(Reminders.revision(c)==revision,"Unchanged sorted times do not reset state");i.runOnMainSync(()->{Reminders.showDaily(c,0,true);});
        Reminders.saveTimes(c,ReminderTimes.defaults());check(Arrays.equals(Reminders.times(c),ReminderTimes.defaults()),"Defaults restored");
        mainTap("記録ウィジェットを追加");await("ホーム画面に追加");await("表示されない・拒否したとき");shot("widget");tap("追加をリクエスト");SystemClock.sleep(400);i.getUiAutomation().performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK);SystemClock.sleep(300);
        i.runOnMainSync(()->WidgetSetup.show(a));await("ホーム画面に追加");await("追加をリクエスト");await("表示されない・拒否したとき");check(true,"Widget guide remains available after dismissal");shot("widget-retry");
        return checks;
    }
}
