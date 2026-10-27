package app.routia;
import android.content.*;
import java.time.*;
public class ReminderReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context c,Intent i){
        Store.get(c).evaluateTickets(LocalDate.now());
        if(Reminders.MIDNIGHT.equals(i.getAction())){WidgetRefresh.refresh(c,true);return;}
        if(!Reminders.DAILY.equals(i.getAction()))return;
        synchronized(Reminders.class){
        int[] selected=Reminders.times(c);int slot=i.getIntExtra("slot",-1);if(slot<0||slot>=selected.length||i.getIntExtra("revision",0)!=Reminders.revision(c))return;
        try{LocalDate date=LocalDate.parse(i.getStringExtra("date")==null?LocalDate.now().toString():i.getStringExtra("date"));if(ReminderTimes.due(LocalDateTime.now(),date,selected[slot]))Reminders.showDaily(c,slot,true);}catch(java.time.DateTimeException ignored){}
        Reminders.scheduleSlot(c,slot);Commitments.evaluate(c);
        }
    }
}
