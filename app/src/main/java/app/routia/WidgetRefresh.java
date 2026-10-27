package app.routia;
import android.content.*;
import java.time.LocalDate;
/** A date guard avoids rebuilding collections on every clock tick or screen-on. */
final class WidgetRefresh {
    private static boolean updating;
    static synchronized void refresh(Context c,boolean force){
        if(updating)return;String day=LocalDate.now().toString();SharedPreferences prefs=c.getSharedPreferences("widget_refresh",Context.MODE_PRIVATE);
        if(!force&&day.equals(prefs.getString("day","")))return;
        updating=true;try{Reminders.all(c);HabitWidget.update(c);prefs.edit().putString("day",day).apply();}finally{updating=false;}
    }
    private WidgetRefresh(){}
}
