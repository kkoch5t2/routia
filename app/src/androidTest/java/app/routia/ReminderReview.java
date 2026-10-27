package app.routia;

import android.app.*;
import android.content.*;
import android.graphics.*;
import android.net.Uri;
import android.os.*;
import android.service.notification.StatusBarNotification;
import android.view.*;
import android.widget.*;
import java.time.*;
import java.util.*;
import java.util.function.BooleanSupplier;
import java.io.*;

/** Reminder scheduling and editor checks use only a fresh emulator app. */
final class ReminderReview {
    final Instrumentation i;final ReportReview views;int n;
    ReminderReview(Instrumentation i){this.i=i;views=new ReportReview(i);}
    void check(boolean ok,String label){n++;if(!ok)throw new AssertionError(label);}
    void await(BooleanSupplier ready,String label){long until=SystemClock.uptimeMillis()+3000;while(!ready.getAsBoolean()&&SystemClock.uptimeMillis()<until)SystemClock.sleep(20);check(ready.getAsBoolean(),label);i.waitForIdleSync();}
    View root(Activity a){return a.getWindow().getDecorView();}
    View window(MainActivity a){return a.reminderDialog.getWindow().getDecorView();}
    void click(View view){check(view!=null,"control exists");i.runOnMainSync(view::performClick);i.waitForIdleSync();}
    void count(MainActivity a,int count){Spinner s=(Spinner)views.findTag(window(a),"reminder_count");i.runOnMainSync(()->s.setSelection(count));await(()->a.reminderDraft.length==count,"editor count changes to "+count);}
    void open(MainActivity a){click(views.find(root(a),"回数・時刻を変更"));await(()->a.reminderDialog!=null&&a.reminderDialog.isShowing(),"reminder editor opens");}
    void pick(MainActivity a,int slot,int minute){click(views.findTag(window(a),"reminder_time_"+slot));await(()->a.reminderTimePicker!=null&&a.reminderTimePicker.isShowing(),"native time picker opens");TimePickerDialog dialog=a.reminderTimePicker;TimePicker picker=(TimePicker)views.findType(dialog.getWindow().getDecorView(),TimePicker.class);i.runOnMainSync(()->{picker.setHour(minute/60);picker.setMinute(minute%60);dialog.getButton(AlertDialog.BUTTON_POSITIVE).performClick();});i.waitForIdleSync();}
    void save(MainActivity a){click(views.find(window(a),"回数・時刻を保存"));}
    PendingIntent existing(Context c,int slot,int revision){Intent intent=new Intent(c,ReminderReceiver.class).setAction(Reminders.DAILY).setData(Uri.parse(revision==0?"routia://daily/"+slot:"routia://daily/"+revision+"/"+slot));return PendingIntent.getBroadcast(c,slot,intent,PendingIntent.FLAG_NO_CREATE|PendingIntent.FLAG_IMMUTABLE);}
    Notification daily(Context c){for(StatusBarNotification s:c.getSystemService(NotificationManager.class).getActiveNotifications())if("daily".equals(s.getTag()))return s.getNotification();return null;}
    void cancelled(PendingIntent pending){try{pending.send();throw new AssertionError("Old alarm can still fire");}catch(PendingIntent.CanceledException expected){n++;}}
    void shot(Dialog d,String name)throws Exception{Bitmap[] b={null};i.runOnMainSync(()->{View v=d.getWindow().getDecorView();b[0]=Bitmap.createBitmap(v.getWidth(),v.getHeight(),Bitmap.Config.ARGB_8888);v.draw(new Canvas(b[0]));});try(FileOutputStream out=new FileOutputStream(new File(i.getTargetContext().getExternalFilesDir(null),"report-"+name+".png"))){b[0].compress(Bitmap.CompressFormat.PNG,100,out);}b[0].recycle();}
    int run()throws Exception{
        Context c=i.getTargetContext();check((Build.HARDWARE.contains("ranchu")||Build.HARDWARE.contains("goldfish"))&&c.getPackageName().endsWith(".reportqa"),"fresh QA emulator only");Store store=Store.get(c);check(store.habits().isEmpty()&&!ReportSettings.configured(c),"no real data or API keys");
        check(Arrays.equals(Reminders.times(c),new int[]{720,900,1080,1200,1320}),"five requested defaults");
        Reminders.prefs(c).edit().clear().putString("times","750,1140,1260").putInt("revision",8).commit();PendingIntent legacy=Reminders.alarmVersion(c,1,8,LocalDate.now().toString());
        check(Arrays.equals(Reminders.times(c),ReminderTimes.defaults())&&Reminders.revision(c)==9,"old defaults migrate to five and invalidate old revision");cancelled(legacy);
        Reminders.prefs(c).edit().clear().putString("times","495,900,1380").putInt("revision",3).commit();check(Arrays.equals(Reminders.times(c),new int[]{495,900,1380}),"existing custom three times preserved");
        Reminders.prefs(c).edit().clear().putString("times","bad-data").commit();check(Arrays.equals(Reminders.times(c),ReminderTimes.defaults()),"corrupt legacy config falls back safely");
        long habit=store.saveConfiguredHabit(0,"","通知テストの習慣","一歩","続ける","","check",0,"");LocalDate today=LocalDate.now();
        try(InputStream in=new ParcelFileDescriptor.AutoCloseInputStream(i.getUiAutomation().executeShellCommand("pm grant "+c.getPackageName()+" android.permission.POST_NOTIFICATIONS"))){while(in.read()!=-1){}}
        check(Reminders.allowed(c),"notification permission enabled only in QA");
        int[] ten={480,540,600,660,720,780,900,1080,1200,1320};Reminders.saveTimes(c,ten);int revision=Reminders.revision(c);PendingIntent[] alarms=new PendingIntent[10];
        for(int slot=0;slot<10;slot++){alarms[slot]=existing(c,slot,revision);check(alarms[slot]!=null,"alarm operation registered for slot "+slot);if(Build.VERSION.SDK_INT>=31)check(alarms[slot].isImmutable(),"alarm intent remains immutable");}
        int[] reverse=ten.clone();for(int at=0;at<reverse.length/2;at++){int tmp=reverse[at];reverse[at]=reverse[reverse.length-1-at];reverse[reverse.length-1-at]=tmp;}Reminders.saveTimes(c,reverse);check(Reminders.revision(c)==revision,"unchanged chronological times do not reset revision");
        NotificationManager nm=c.getSystemService(NotificationManager.class);i.runOnMainSync(()->Reminders.showDaily(c,9,true));await(()->daily(c)!=null,"tenth reminder can display");Notification last=daily(c);
        check(last.extras.getCharSequence(Notification.EXTRA_TITLE).toString().contains("締め切り")&&last.extras.getCharSequence(Notification.EXTRA_TEXT).toString().contains("最後"),"last slot uses final reminder copy");
        check(last.actions.length==2,"check habit quick recording actions remain");nm.cancel("daily",1);i.runOnMainSync(()->Reminders.showDaily(c,9,true));check(daily(c)==null,"same daily slot cannot alert twice");
        i.runOnMainSync(()->Reminders.showDaily(c,6,false));await(()->daily(c)!=null,"middle reminder displayed");check(daily(c).extras.getCharSequence(Notification.EXTRA_TITLE).toString().startsWith("7回目"),"middle notification counts beyond three correctly");
        Reminders.saveTimes(c,new int[]{720});for(PendingIntent pending:alarms)cancelled(pending);check(daily(c)==null,"schedule change removes existing notification without alert");
        i.runOnMainSync(()->new ReminderReceiver().onReceive(c,new Intent(Reminders.DAILY).putExtra("slot",9).putExtra("revision",revision).putExtra("date",today.toString())));check(daily(c)==null,"old high slot is rejected after count reduction");
        i.runOnMainSync(()->Reminders.showDaily(c,9,false));check(daily(c)==null,"out of range slot never indexes schedule");
        i.runOnMainSync(()->Reminders.showDaily(c,0,false));await(()->daily(c)!=null,"single reminder displayed");check(!daily(c).extras.getCharSequence(Notification.EXTRA_TITLE).toString().contains("締め切り"),"single reminder uses gentle copy");
        store.record(habit,today,1,false);i.runOnMainSync(()->Reminders.showDaily(c,0,false));check(daily(c)==null,"all completed habits suppress reminders");store.record(habit,today,0,true);
        Reminders.saveTimes(c,new int[0]);check(Reminders.times(c).length==0&&Reminders.timesLabel(c).equals("通知オフ"),"zero is persisted notification off");for(int slot=0;slot<10;slot++)check(existing(c,slot,Reminders.revision(c))==null,"off leaves no daily alarm operation");
        i.runOnMainSync(()->Reminders.showDaily(c,0,false));check(daily(c)==null,"zero schedule never notifies");
        Reminders.saveTimes(c,ReminderTimes.defaults());
        MainActivity a=(MainActivity)i.startActivitySync(new Intent(c,MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));i.waitForIdleSync();click(views.findTag(root(a),"nav_settings"));open(a);
        check(a.reminderDraft.length==5&&views.findTag(window(a),"reminder_time_4")!=null&&views.findTag(window(a),"reminder_time_5")==null,"editor starts with five time rows");int before=Reminders.revision(c);count(a,10);
        check(views.findTag(window(a),"reminder_time_9")!=null,"all ten time controls exist");shot(a.reminderDialog,"reminders-ten-top");
        i.runOnMainSync(()->a.reminderDialog.formScroll.fullScroll(View.FOCUS_DOWN));i.waitForIdleSync();shot(a.reminderDialog,"reminders-ten-bottom");
        Rect saveRect=new Rect(),backRect=new Rect();View save=views.find(window(a),"回数・時刻を保存"),back=views.find(window(a),"戻る");check(save.getGlobalVisibleRect(saveRect)&&back.getGlobalVisibleRect(backRect)&&saveRect.bottom<=backRect.top,"save/back remain visible and separate with ten rows");
        pick(a,9,a.reminderDraft[0]);save(a);check(a.reminderDialog!=null&&a.reminderDialog.isShowing()&&Reminders.revision(c)==before,"duplicate times are rejected without persisting");
        pick(a,9,1439);save(a);check(Reminders.times(c).length==10&&Reminders.times(c)[9]==1439,"ten custom times persist sorted");check(views.find(root(a),"1日 10回")!=null,"Settings displays selected count");
        open(a);count(a,3);pick(a,0,435);click(views.findTag(window(a),"reminder_time_1"));TimePicker picker=(TimePicker)views.findType(a.reminderTimePicker.getWindow().getDecorView(),TimePicker.class);i.runOnMainSync(()->{picker.setHour(16);picker.setMinute(45);});
        MainActivity restored=(MainActivity)views.recreatePage(a);await(()->restored.reminderDialog!=null&&restored.reminderTimePicker!=null&&restored.reminderTimePicker.isShowing(),"editor and picker restore after rotation");
        TimePicker restoredPicker=(TimePicker)views.findType(restored.reminderTimePicker.getWindow().getDecorView(),TimePicker.class);check(restored.reminderDraft.length==3&&restored.reminderDraft[0]==435&&restoredPicker.getHour()==16&&restoredPicker.getMinute()==45,"unsaved count/time and picker draft survive recreation");
        i.runOnMainSync(()->restored.reminderTimePicker.getButton(AlertDialog.BUTTON_NEGATIVE).performClick());i.waitForIdleSync();click(views.find(window(restored),"戻る"));check(Reminders.times(c).length==10,"Back discards unsaved count and times");
        open(restored);count(restored,0);check(views.findTag(window(restored),"reminder_time_0")==null,"zero count hides time rows");save(restored);check(Reminders.times(c).length==0,"editor saves off");click(views.findTag(root(restored),"nav_today"));check(views.find(root(restored),"リマインダーはオフです。")!=null,"Today handles zero count without fixed indices");click(views.findTag(root(restored),"nav_settings"));open(restored);click(views.find(window(restored),"初期の回数・時刻に戻す"));check(Arrays.equals(restored.reminderDraft,ReminderTimes.defaults()),"reset restores requested five times");save(restored);check(Arrays.equals(Reminders.times(c),ReminderTimes.defaults()),"reset persists new defaults");i.runOnMainSync(restored::finish);i.waitForIdleSync();return n;
    }
}
