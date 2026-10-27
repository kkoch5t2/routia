package app.routia;
import android.app.*;
import android.content.*;
public class FocusReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context c,Intent i){
        if(!Focus.complete(c))return;
        NotificationManager nm=c.getSystemService(NotificationManager.class);nm.createNotificationChannel(new NotificationChannel("focus_end","集中タイマーの終了",NotificationManager.IMPORTANCE_DEFAULT));
        if(!Reminders.allowed(c))return;
        PendingIntent open=PendingIntent.getActivity(c,90,new Intent(c,FocusActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
        try{nm.notify("focus",2,new Notification.Builder(c,"focus_end").setSmallIcon(R.drawable.ic_notification).setContentTitle("集中タイム、完走！").setContentText("積み重ねた時間を確認しましょう。").setContentIntent(open).setAutoCancel(true).setVisibility(Notification.VISIBILITY_PRIVATE).build());}catch(SecurityException ignored){}
    }
}
