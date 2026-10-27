package app.routia;

import android.app.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import java.time.LocalDate;

/** Automatic completion notices are independent of daily reminders and of paid generation. */
final class ReportNotices {
    static final String CHANNEL="continuing_reviews_v1";
    static void channel(Context c){
        NotificationChannel channel=new NotificationChannel(CHANNEL,"継続レビューの完成",NotificationManager.IMPORTANCE_DEFAULT);
        channel.setDescription("継続レビューの自動生成が完了したときにお知らせします。");
        c.getSystemService(NotificationManager.class).createNotificationChannel(channel);
    }
    static boolean allowed(Context c){
        channel(c);NotificationManager manager=c.getSystemService(NotificationManager.class);
        return (Build.VERSION.SDK_INT<33||c.checkSelfPermission("android.permission.POST_NOTIFICATIONS")==PackageManager.PERMISSION_GRANTED)
            &&manager.areNotificationsEnabled()&&manager.getNotificationChannel(CHANNEL).getImportance()!=NotificationManager.IMPORTANCE_NONE;
    }
    static Intent reportIntent(Context c,ReportStore.Item item){
        return ReportActivity.intent(c,item.habit(),item.id()).setData(Uri.parse("routia://review/"+item.id()))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_SINGLE_TOP);
    }
    static Notification notification(Context c,ReportStore.Item item){
        PendingIntent open=PendingIntent.getActivity(c,0,reportIntent(c,item),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
        Notification publicView=new Notification.Builder(c,CHANNEL).setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("継続レビューができました").setContentText("アプリで確認できます。").build();
        return new Notification.Builder(c,CHANNEL).setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("継続レビューができました").setContentText(item.goal()+"のレビューを確認できます。")
            .setContentIntent(open).setAutoCancel(true).setOnlyAlertOnce(true).setVisibility(Notification.VISIBILITY_PRIVATE)
            .setPublicVersion(publicView).build();
    }
    static void deliver(Context c,ReportStore db,ReportStore.Item item){
        if(item==null||item.manual()||!item.state().equals("complete")||item.completedAt()<=0||Store.get(c).habit(item.habit())==null)return;
        boolean claimed=false;
        try{
            if(!allowed(c))return;
            Notification notice=notification(c,item);
            if(!(claimed=db.claimNotice(item.id())))return;
            c.getSystemService(NotificationManager.class).notify("review:"+item.id(),1,notice);
        }catch(RuntimeException ignored){if(claimed)try{db.releaseNotice(item.id());}catch(RuntimeException ignoredAgain){/* Keep saved content intact. */}}
    }
    static void deliverPending(Context c){
        try(ReportStore db=new ReportStore(c)){for(ReportStore.Item item:db.pendingNotices(LocalDate.now()))deliver(c,db,item);}
        catch(RuntimeException ignored){/* Notifications never prevent opening the app or reading a saved review. */}
    }
    static void permission(Activity a){
        if(Build.VERSION.SDK_INT>=33&&a.checkSelfPermission("android.permission.POST_NOTIFICATIONS")!=PackageManager.PERMISSION_GRANTED){
            if(!Feedback.prefs(a).getBoolean("reviewNotificationAsked",false)){
                Feedback.prefs(a).edit().putBoolean("reviewNotificationAsked",true).apply();
                a.requestPermissions(new String[]{"android.permission.POST_NOTIFICATIONS"},303);return;
            }
            a.startActivity(new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE,a.getPackageName()));return;
        }
        channel(a);
        a.startActivity(new Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE,a.getPackageName()).putExtra(Settings.EXTRA_CHANNEL_ID,CHANNEL));
    }
    private ReportNotices(){}
}
