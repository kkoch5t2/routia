package app.routia;
import android.app.*;
import android.content.*;
import android.os.*;
import android.provider.Settings;
import java.time.*;
@android.annotation.SuppressLint("ApplySharedPref") // Session boundaries must be durable before returning to another component.
public final class Focus {
    static SharedPreferences prefs(Context c){return c.getSharedPreferences("focus",Context.MODE_PRIVATE);}
    static int boot(Context c){return Settings.Global.getInt(c.getContentResolver(),Settings.Global.BOOT_COUNT,-1);}
    static boolean exists(Context c){return prefs(c).getLong("session",0)>0;}
    static boolean sameBoot(Context c){return prefs(c).getInt("boot",-2)==boot(c);}
    static synchronized boolean paused(Context c){reconcile(c);return prefs(c).getBoolean("paused",false);}
    static synchronized void reconcile(Context c){SharedPreferences p=prefs(c);if(exists(c)&&!sameBoot(c)){long saved=p.getLong("left",p.getInt("minutes",0)*60000L);p.edit().putBoolean("paused",true).putLong("left",saved).putInt("boot",boot(c)).commit();c.getSystemService(AlarmManager.class).cancel(alarm(c));}}
    static synchronized long remaining(Context c){reconcile(c);SharedPreferences p=prefs(c);return p.getBoolean("paused",false)?p.getLong("left",0):Math.max(0,p.getLong("end",0)-SystemClock.elapsedRealtime());}
    static synchronized void checkpoint(Context c){if(exists(c)&&!paused(c))prefs(c).edit().putLong("left",remaining(c)).commit();}
    static synchronized void pause(Context c){if(!exists(c)||prefs(c).getBoolean("finished",false))return;long left=remaining(c);prefs(c).edit().putBoolean("paused",true).putLong("left",left).commit();c.getSystemService(AlarmManager.class).cancel(alarm(c));}
    static synchronized void resume(Context c){if(!exists(c)||!paused(c))return;long now=SystemClock.elapsedRealtime(),left=remaining(c);prefs(c).edit().putBoolean("paused",false).putLong("end",now+left).putInt("boot",boot(c)).commit();schedule(c,now+left);}
    static void start(Context c,long habit,int minutes){
        if(!Store.get(c).metric(habit).kind().equals("time"))throw new IllegalArgumentException("タイマーは時間を記録する習慣で使えます");
        if(minutes<1||minutes>180)throw new IllegalArgumentException("1〜180分で設定してください");
        if(exists(c))throw new IllegalArgumentException("進行中のタイマーがあります");
        long session=Store.get(c).newSession(habit),now=SystemClock.elapsedRealtime();
        prefs(c).edit().putLong("session",session).putLong("habit",habit).putLong("start",now).putLong("end",now+minutes*60000L).putLong("left",minutes*60000L).putBoolean("paused",false).putInt("minutes",minutes).putInt("boot",boot(c)).putString("day",LocalDate.now().toString()).putBoolean("finished",false).commit();
        schedule(c,now+minutes*60000L);
    }
    private static void schedule(Context c,long end){
        AlarmManager am=c.getSystemService(AlarmManager.class);
        try{if(Reminders.exact(c))am.setExactAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP,end,alarm(c));else am.setAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP,end,alarm(c));}catch(SecurityException e){am.setAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP,end,alarm(c));}
    }
    static PendingIntent alarm(Context c){return PendingIntent.getBroadcast(c,90,new Intent(c,FocusReceiver.class),PendingIntent.FLAG_IMMUTABLE|PendingIntent.FLAG_UPDATE_CURRENT);}
    /** Idempotent: a receiver and the visible timer may finish simultaneously. */
    static synchronized boolean complete(Context c){
        SharedPreferences p=prefs(c);if(!exists(c)||paused(c)||remaining(c)>0||p.getBoolean("finished",false))return false;
        long id=p.getLong("habit",-1);Store s=Store.get(c);int seconds=p.getInt("minutes",0)*60;boolean sameDay=LocalDate.now().toString().equals(p.getString("day",""));
        android.database.sqlite.SQLiteDatabase db=s.getWritableDatabase();db.beginTransaction();boolean added=false;
        try{
            if(s.finishSession(p.getLong("session",0),seconds,true)){
                added=true;if(s.habit(id)!=null&&sameDay){
                    Store.Entry before=s.entry(id,LocalDate.now());
                    if(s.metric(id).kind().equals("time")){s.recordAmount(id,LocalDate.now(),seconds/60.0,null,true);Store.Entry after=s.entry(id,LocalDate.now());p.edit().putBoolean("celebrate",Rules.shouldCelebrate("time",s.metric(id).target(),before.level(),before.amount(),after.level(),after.amount())).commit();}
                }
            }db.setTransactionSuccessful();
        }finally{db.endTransaction();}
        p.edit().putBoolean("finished",true).commit();HabitWidget.update(c);Reminders.refreshVisible(c);return added;
    }
    static synchronized void clear(Context c,boolean abort){
        SharedPreferences p=prefs(c);
        if(abort&&exists(c)){int seconds=(int)Math.max(0,(p.getInt("minutes",0)*60000L-remaining(c))/1000);Store.get(c).finishSession(p.getLong("session",0),seconds,false);}
        c.getSystemService(AlarmManager.class).cancel(alarm(c));p.edit().clear().commit();c.getSystemService(NotificationManager.class).cancel("focus",2);
    }
    private Focus(){}
}
