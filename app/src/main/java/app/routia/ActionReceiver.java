package app.routia;
import android.app.NotificationManager;
import android.content.*;
import android.widget.Toast;
import java.time.LocalDate;

public class ActionReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context c,Intent intent) {
        long id=intent.getLongExtra("habit",-1);int level=intent.getIntExtra("level",0);
        if(WidgetClickData.ACTION.equals(intent.getAction())){
            java.util.Optional<WidgetClickData.Click> click=WidgetClickData.parse(intent.getAction(),intent.getDataString(),id,level,intent.getStringExtra("shownDay"),LocalDate.now(),widget->WidgetCollections.ownedRecordWidget(c,widget));
            if(click.isEmpty()){HabitWidget.update(c);Toast.makeText(c,"ウィジェットを更新しました。今日のボタンで記録してください。",Toast.LENGTH_LONG).show();return;}
            intent=new Intent(intent).putExtra("day",click.get().day().toString());
        }
        if(!Rules.validActionDate(intent.getStringExtra("day"),LocalDate.now())) {
            c.getSystemService(NotificationManager.class).cancel("habit"+id,1);HabitWidget.update(c);Reminders.refreshVisible(c);Toast.makeText(c,"日付が変わりました。今日のボタンで記録してください。",Toast.LENGTH_LONG).show();return;
        }
        try {
            Store.Result result=Store.get(c).record(id,LocalDate.now(),level,false);
            c.getSystemService(NotificationManager.class).cancel("habit"+id,1);
            HabitWidget.update(c);Reminders.refreshVisible(c);
            if(result.changed()) Feedback.deliver(c,null,result.recovery(),Rules.feedback(result.recovery(),result.level()));
            else Toast.makeText(c,"今日はすでに達成しています",Toast.LENGTH_SHORT).show();
            c.sendBroadcast(new Intent("app.routia.CHANGED").setPackage(c.getPackageName()));
        } catch(IllegalArgumentException ex) { HabitWidget.update(c);Reminders.refreshVisible(c);Toast.makeText(c,ex.getMessage(),Toast.LENGTH_SHORT).show(); }
    }
}
