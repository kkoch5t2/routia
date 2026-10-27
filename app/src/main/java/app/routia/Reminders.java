package app.routia;

import android.app.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import java.time.*;
import java.util.*;

public final class Reminders {
    private static final String CHANNEL="daily_steps_v2";
    static final String MIDNIGHT="app.routia.MIDNIGHT";
    static final String DAILY="app.routia.DAILY";
    public static final int[] HOURS={12,15,18,20,22}, MINUTES={0,0,0,0,0};
    static android.content.SharedPreferences prefs(Context c){return c.getSharedPreferences("reminder_times",Context.MODE_PRIVATE);}
    static int revision(Context c){return prefs(c).getInt("revision",0);}
    private static void cancelVersion(Context c,int version){
        AlarmManager manager=c.getSystemService(AlarmManager.class);
        for(int slot=0;slot<ReminderTimes.MAX;slot++){PendingIntent pending=alarmVersion(c,slot,version,null);manager.cancel(pending);pending.cancel();}
    }
    public static synchronized int[] times(Context c){
        android.content.SharedPreferences p=prefs(c);
        if(p.getInt("schedule_format",0)<1){
            String old=p.getString("times",null);int[] chosen;
            try{chosen=old==null||old.equals("750,1140,1260")?ReminderTimes.defaults():ReminderTimes.parse(old);}catch(IllegalArgumentException e){chosen=ReminderTimes.defaults();}
            int prior=revision(c);
            if(p.edit().putString("times",ReminderTimes.encode(chosen)).putInt("schedule_format",1).putInt("revision",prior+1).commit())cancelVersion(c,prior);
            return chosen;
        }
        try{return ReminderTimes.parse(p.getString("times",null));}catch(IllegalArgumentException e){return ReminderTimes.defaults();}
    }
    public static String timesLabel(Context c){int[] times=times(c);if(times.length==0)return "通知オフ";StringJoiner out=new StringJoiner(" / ");for(int minute:times)out.add(ReminderTimes.format(minute));return out.toString();}
    @android.annotation.SuppressLint("ApplySharedPref")
    public static synchronized void saveTimes(Context c,int[] selected){
        int[] sorted=ReminderTimes.validate(selected);if(Arrays.equals(sorted,times(c)))return;int prior=revision(c);
        if(!prefs(c).edit().putString("times",ReminderTimes.encode(sorted)).putInt("schedule_format",1).putInt("revision",prior+1).commit())throw new IllegalStateException("通知設定を保存できませんでした");
        cancelVersion(c,prior);refreshVisible(c);all(c);
    }

    public static void channels(Context c) {
        NotificationChannel channel=new NotificationChannel(CHANNEL,"習慣のリマインダー",NotificationManager.IMPORTANCE_DEFAULT);
        channel.setDescription("選んだ回数・時刻に、未達成の習慣をまとめてお知らせします（1日最大10回）。");
        c.getSystemService(NotificationManager.class).createNotificationChannel(channel);
    }
    public static boolean allowed(Context c) { return (Build.VERSION.SDK_INT<33||c.checkSelfPermission("android.permission.POST_NOTIFICATIONS")==PackageManager.PERMISSION_GRANTED)&&c.getSystemService(NotificationManager.class).areNotificationsEnabled(); }
    public static boolean exact(Context c) { return Build.VERSION.SDK_INT<31||c.getSystemService(AlarmManager.class).canScheduleExactAlarms(); }
    private static PendingIntent legacy(Context c,long id) {
        Intent i=new Intent(c,ReminderReceiver.class).setData(Uri.parse("routia://reminder/"+id)).putExtra("habit",id);
        return PendingIntent.getBroadcast(c,0,i,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
    }
    static PendingIntent alarm(Context c,int slot){return alarmVersion(c,slot,revision(c),null);}
    static PendingIntent alarmVersion(Context c,int slot,int revision,String day){
        Intent i=new Intent(c,ReminderReceiver.class).setAction(DAILY).setData(Uri.parse(revision==0?"routia://daily/"+slot:"routia://daily/"+revision+"/"+slot)).putExtra("slot",slot).putExtra("revision",revision);
        if(day!=null)i.putExtra("date",day);
        return PendingIntent.getBroadcast(c,slot,i,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
    }
    public static void cancel(Context c,long id) { c.getSystemService(AlarmManager.class).cancel(legacy(c,id));c.getSystemService(NotificationManager.class).cancel("habit"+id,1); }
    public static void schedule(Context c,Store.Habit h) { all(c); }
    static synchronized void scheduleSlot(Context c,int slot) {
        int[] selected=times(c);if(slot<0||slot>=selected.length)return;
        int minute=selected[slot];ZonedDateTime scheduled=Rules.nextReminder(ZonedDateTime.now(),minute/60,minute%60);long next=scheduled.toInstant().toEpochMilli();PendingIntent pending=alarmVersion(c,slot,revision(c),scheduled.toLocalDate().toString());
        AlarmManager manager=c.getSystemService(AlarmManager.class);
        if(exact(c)) {
            try { manager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,next,pending);return; } catch(SecurityException ignored) { }
        }
        manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,next,pending);
    }
    public static synchronized void all(Context c) {
        Store.get(c).evaluateTickets(LocalDate.now());
        channels(c);List<Store.Habit> habits=Store.get(c).habits();for(Store.Habit h:habits)cancel(c,h.id());
        int[] selected=times(c);cancelVersion(c,revision(c));
        if(!habits.isEmpty())for(int slot=0;slot<selected.length;slot++)scheduleSlot(c,slot);
        Commitments.evaluate(c);midnight(c);
    }
    public static void midnight(Context c) {
        PendingIntent pi=PendingIntent.getBroadcast(c,0,new Intent(c,ReminderReceiver.class).setAction(MIDNIGHT),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
        long time=LocalDate.now().plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
        AlarmManager manager=c.getSystemService(AlarmManager.class);
        if(exact(c)){try{manager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,time,pi);return;}catch(SecurityException ignored){}}
        manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,time,pi);
    }
    public static PendingIntent action(Context c,long id,int level,String day,String source) {
        Intent i=new Intent(c,ActionReceiver.class).setAction("app.routia.RECORD").setData(Uri.parse("routia://record/"+id+"/"+day+"/"+level+"/"+source)).putExtra("habit",id).putExtra("level",level).putExtra("day",day).addFlags(Intent.FLAG_RECEIVER_FOREGROUND);
        return PendingIntent.getBroadcast(c,0,i,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
    }
    // Compatibility entry point, used by existing widget/integration checks.
    public static void show(Context c,Store.Habit h) {showDaily(c,0,false);}
    static synchronized void showDaily(Context c,int slot,boolean scheduled) {
        int count=times(c).length;if(slot<0||slot>=count||!allowed(c))return;
        int stage=ReminderTimes.stage(slot,count);boolean last=slot==count-1;
        LocalDate day=LocalDate.now();Store store=Store.get(c);
        List<Store.Habit> undone=new ArrayList<>();for(Store.Habit h:store.habits())if(store.level(h.id(),day)==0)undone.add(h);
        if(undone.isEmpty()){c.getSystemService(NotificationManager.class).cancel("daily",1);return;}
        String token=day+":"+slot;
        if(scheduled&&Feedback.prefs(c).getBoolean("sent:"+token,false))return;
        long remainingMinutes=Duration.between(ZonedDateTime.now(),day.plusDays(1).atStartOfDay(ZoneId.systemDefault())).toMinutes();
        String remaining=remainingMinutes>=60?(remainingMinutes/60)+"時間"+(remainingMinutes%60>0?remainingMinutes%60+"分":""):remainingMinutes+"分";
        String title=stage==2?"今日の締め切りまで、あと"+remaining+"。":slot==0?"今日の一歩、そろそろ始めよう。":(slot+1)+"回目のお知らせ。今日の一歩を、今。";
        StringBuilder body=new StringBuilder(stage==2?"まだ間に合う。今日の分をひとつ残そう。\n":"あと "+undone.size()+"つ。まずは小さく。\n");
        for(Store.Habit h:undone)body.append("・").append(h.goal()).append(" → ").append(store.metric(h.id()).kind().equals("check")?h.minimum():"数値を入力するだけ").append('\n');
        PendingIntent open=PendingIntent.getActivity(c,0,new Intent(c,MainActivity.class),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
        Notification.Builder b=new Notification.Builder(c,CHANNEL).setSmallIcon(R.drawable.ic_notification).setContentTitle(title).setContentText(undone.size()+"つ未達成 · "+(last?"最後のお知らせ":(slot+1)+" / "+count+"回目 · 今日の記録を残そう")).setStyle(new Notification.BigTextStyle().bigText(body.toString())).setContentIntent(open).setAutoCancel(true).setOnlyAlertOnce(false).setColor(stage==2?0xffB5473E:stage==1?0xffB86A24:0xff326651).setVisibility(Notification.VISIBILITY_PRIVATE).setTimeoutAfter(Math.max(1000,Duration.between(ZonedDateTime.now(),day.plusDays(1).atStartOfDay(ZoneId.systemDefault())).toMillis()));
        if(undone.size()==1){Store.Habit h=undone.get(0);
            if(store.metric(h.id()).kind().equals("check")){b.addAction(new Notification.Action.Builder(null,"🌱 ミニ達成",action(c,h.id(),1,day.toString(),"notification")).build());b.addAction(new Notification.Action.Builder(null,"🏆 目標達成",action(c,h.id(),2,day.toString(),"notification")).build());}
            else b.addAction(new Notification.Action.Builder(null,"数値を入力する",PendingIntent.getActivity(c,(int)h.id()+9100,RecordActivity.intent(c,h.id()),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE)).build());
        }else for(Store.Habit h:undone){
            boolean check=store.metric(h.id()).kind().equals("check");PendingIntent pending=check?action(c,h.id(),1,day.toString(),"notification"):PendingIntent.getActivity(c,(int)h.id()+9100,RecordActivity.intent(c,h.id()),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
            b.addAction(new Notification.Action.Builder(null,(check?"✅ ":"入力 ")+h.goal(),pending).build());
        }
        try {c.getSystemService(NotificationManager.class).notify("daily",1,b.build());if(scheduled)Feedback.prefs(c).edit().putBoolean("sent:"+token,true).apply();}catch(SecurityException ignored){}
    }
    static void refreshVisible(Context c) {
        // Update an existing card silently; recording must not generate an extra reminder.
        for(android.service.notification.StatusBarNotification n:c.getSystemService(NotificationManager.class).getActiveNotifications())if("daily".equals(n.getTag())) {
            c.getSystemService(NotificationManager.class).cancel("daily",1);break;
        }
    }
    private Reminders() {}
}
