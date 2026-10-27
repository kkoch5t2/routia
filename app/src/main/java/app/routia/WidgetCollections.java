package app.routia;
import android.app.*;
import android.appwidget.*;
import android.content.*;
import android.net.Uri;
import android.os.Build;
import android.view.*;
import android.widget.*;
import java.time.LocalDate;
import java.util.*;
/** Native scrollable collection data for modern and legacy widget hosts. */
final class WidgetCollections {
    static final String RECORD="record",RECORD_COMPACT="record-compact",PROGRESS="progress";
    record Row(long id,RemoteViews view){}
    static int chartWidth(Context c,int widgetId){int width=AppWidgetManager.getInstance(c).getAppWidgetOptions(widgetId).getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH,320);return Math.max(188,Math.min(800,width-32));}
    static List<Row> rows(Context c,String kind,boolean clickable){return rows(c,kind,clickable,288);}
    static List<Row> rows(Context c,String kind,boolean clickable,int chartWidth){
        List<Row> result=new ArrayList<>();LocalDate today=LocalDate.now();
        if(PROGRESS.equals(kind)){for(ProgressWidget.Item item:ProgressWidget.snapshot(c))result.add(new Row(item.habit().id(),ProgressWidget.row(c,item,clickable,chartWidth)));}
        else{Store store=Store.get(c);store.evaluateTickets(today);for(Store.Habit h:store.habits())result.add(new Row(h.id(),HabitWidget.row(c,h,today,clickable,RECORD_COMPACT.equals(kind))));}
        return result;
    }
    static void bind(Context c,RemoteViews root,int listId,int widgetId,String kind,List<Row> rows){
        if(Build.VERSION.SDK_INT>=31){RemoteViews.RemoteCollectionItems.Builder items=new RemoteViews.RemoteCollectionItems.Builder().setHasStableIds(true).setViewTypeCount(PROGRESS.equals(kind)?1:4);for(Row row:rows)items.addItem(row.id(),row.view());root.setRemoteAdapter(listId,items.build());}
        else{Intent service=new Intent(c,WidgetRowsService.class).setData(Uri.parse("routia://widget-rows/"+kind+"/"+widgetId)).putExtra("kind",kind).putExtra("widgetId",widgetId);root.setRemoteAdapter(listId,service);}
    }
    @SuppressWarnings("deprecation")
    static void refreshLegacy(AppWidgetManager manager,int[] ids,int listId){if(Build.VERSION.SDK_INT<31&&ids.length>0)manager.notifyAppWidgetViewDataChanged(ids,listId);}
    static PendingIntent recordTemplate(Context c,int widgetId){
        Intent intent=new Intent(c,RecordActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK).setAction(WidgetClickData.ACTION).setData(Uri.parse("routia://widget-record/"+widgetId+"/"+LocalDate.now()));
        // Collection fill-in extras need mutability. Component/action/date/widget identity remain prefilled.
        int flags=PendingIntent.FLAG_UPDATE_CURRENT|(Build.VERSION.SDK_INT>=31?PendingIntent.FLAG_MUTABLE:0);
        return PendingIntent.getActivity(c,8000+widgetId,intent,flags);
    }
    static PendingIntent open(Context c,boolean progress){
        Intent intent=new Intent(c,MainActivity.class).setAction(progress?ProgressWidget.OPEN:"app.routia.OPEN_TODAY").addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_SINGLE_TOP);
        return PendingIntent.getActivity(c,progress?7300:7301,intent,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
    }
    static boolean ownedRecordWidget(Context c,int id){try{AppWidgetProviderInfo info=AppWidgetManager.getInstance(c).getAppWidgetInfo(id);return info!=null&&new ComponentName(c,HabitWidget.class).equals(info.provider);}catch(SecurityException e){return false;}}
    static View preview(Context c,String kind){
        boolean progress=PROGRESS.equals(kind);int color=WidgetColors.selected(c);LinearLayout box=Ui.col(c);box.setId(progress?R.id.progress_widget_root:R.id.widget_root);box.setBackgroundResource(WidgetColors.DRAWABLES[color]);if(Build.VERSION.SDK_INT>=29)box.setForceDarkAllowed(false);
        TextView title=Ui.fixedText(c,progress?"📈 Routiaの成果":"✅ Routiaの記録",18,WidgetColors.ink(color),true);title.setId(progress?R.id.progress_widget_title:R.id.widget_title);LinearLayout identity=new LinearLayout(c);identity.setGravity(android.view.Gravity.CENTER_VERTICAL);identity.addView(new Mascot(c,progress?Mascot.PROGRESS:Mascot.AVATAR),new LinearLayout.LayoutParams(Ui.dp(c,32),Ui.dp(c,32)));LinearLayout.LayoutParams heading=new LinearLayout.LayoutParams(0,-2,1);heading.leftMargin=Ui.dp(c,6);identity.addView(title,heading);box.addView(identity);Ui.gap(box,12);
        for(Row row:rows(c,kind,false)){View view=row.view().apply(c,box);view.setLayoutParams(new LinearLayout.LayoutParams(-1,-2));box.addView(view);Ui.gap(box,8);}
        return box;
    }
    private WidgetCollections(){}
}
