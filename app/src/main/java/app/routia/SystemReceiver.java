package app.routia;
import android.content.*;
import java.util.Arrays;
public class SystemReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context c,Intent i) {
        if(!Arrays.asList(Intent.ACTION_BOOT_COMPLETED,Intent.ACTION_MY_PACKAGE_REPLACED,Intent.ACTION_DATE_CHANGED,Intent.ACTION_TIME_CHANGED,Intent.ACTION_TIMEZONE_CHANGED,"android.app.action.SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED").contains(i.getAction()))return;
        WidgetRefresh.refresh(c,true);
    }
}
