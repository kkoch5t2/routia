package app.routia;
import android.app.Application;
import android.content.*;
import android.os.*;
/** Refresh a stale home-screen day without opening an Activity or recording success. */
public final class RoutiaApp extends Application {
    @Override public void onCreate(){
        super.onCreate();
        ReportRunner.initialize(this);ReportJob.schedule(this);if(ReportSettings.enabled(this))ReportRunner.work(this,null);
        BroadcastReceiver receiver=new BroadcastReceiver(){@Override public void onReceive(Context c,Intent intent){String action=intent.getAction();WidgetRefresh.refresh(c,Intent.ACTION_TIME_CHANGED.equals(action)||Intent.ACTION_TIMEZONE_CHANGED.equals(action));}};
        IntentFilter filter=new IntentFilter();for(String action:new String[]{Intent.ACTION_DATE_CHANGED,Intent.ACTION_TIME_CHANGED,Intent.ACTION_TIMEZONE_CHANGED,Intent.ACTION_TIME_TICK,Intent.ACTION_SCREEN_ON,Intent.ACTION_USER_PRESENT})filter.addAction(action);
        if(Build.VERSION.SDK_INT>=33)registerReceiver(receiver,filter,Context.RECEIVER_NOT_EXPORTED);else registerLegacy(receiver,filter);
        new Handler(Looper.getMainLooper()).post(()->WidgetRefresh.refresh(this,false));
    }
    @SuppressWarnings("UnspecifiedRegisterReceiverFlag") private void registerLegacy(BroadcastReceiver receiver,IntentFilter filter){registerReceiver(receiver,filter);}
}
