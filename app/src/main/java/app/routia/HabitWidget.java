package app.routia;
import android.appwidget.*;
import android.content.*;
import android.view.View;
import android.widget.RemoteViews;
import java.time.LocalDate;
import java.util.*;
public class HabitWidget extends AppWidgetProvider {
    public static void update(Context c){
        Store.get(c).evaluateTickets(LocalDate.now());AppWidgetManager manager=AppWidgetManager.getInstance(c);int[] ids=manager.getAppWidgetIds(new ComponentName(c,HabitWidget.class));
        for(int id:ids)manager.updateAppWidget(id,views(c,id));WidgetCollections.refreshLegacy(manager,ids,R.id.widget_rows);ProgressWidget.update(c);
    }
    @Override public void onReceive(Context c,Intent i){if("app.routia.WIDGET_PINNED".equals(i.getAction())){update(c);android.widget.Toast.makeText(c,"✅ ウィジェットを追加しました",android.widget.Toast.LENGTH_LONG).show();return;}super.onReceive(c,i);}
    @Override public void onUpdate(Context c,AppWidgetManager manager,int[] ids){for(int id:ids)manager.updateAppWidget(id,views(c,id));WidgetCollections.refreshLegacy(manager,ids,R.id.widget_rows);ProgressWidget.update(c);Reminders.midnight(c);}
    @Override public void onAppWidgetOptionsChanged(Context c,AppWidgetManager manager,int id,android.os.Bundle options){manager.updateAppWidget(id,views(c,id));}
    static RemoteViews views(Context c){return views(c,0);}
    static RemoteViews views(Context c,int widgetId){
        if(android.os.Build.VERSION.SDK_INT>=31){Map<android.util.SizeF,RemoteViews> layouts=new LinkedHashMap<>();layouts.put(new android.util.SizeF(220,80),layout(c,widgetId,true));layouts.put(new android.util.SizeF(220,180),layout(c,widgetId,false));return new RemoteViews(layouts);}
        int height=AppWidgetManager.getInstance(c).getAppWidgetOptions(widgetId).getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT,80);return layout(c,widgetId,height<180);
    }
    static RemoteViews layout(Context c,int widgetId,boolean compact){
        String kind=compact?WidgetCollections.RECORD_COMPACT:WidgetCollections.RECORD;List<WidgetCollections.Row> rows=WidgetCollections.rows(c,kind,true);LocalDate today=LocalDate.now();int color=WidgetColors.selected(c);
        RemoteViews root=new RemoteViews(c.getPackageName(),compact?R.layout.habit_widget_compact:R.layout.habit_widget);root.setImageViewBitmap(R.id.widget_mascot,Mascot.bitmap(c,Mascot.AVATAR,96));root.setInt(R.id.widget_root,"setBackgroundResource",WidgetColors.DRAWABLES[color]);root.setTextColor(R.id.widget_title,WidgetColors.ink(color));root.setTextColor(R.id.widget_period,WidgetColors.muted(color));root.setTextColor(R.id.widget_empty,WidgetColors.muted(color));root.setTextColor(R.id.widget_footer,WidgetColors.muted(color));
        if(compact)root.setViewPadding(R.id.widget_root,Ui.dp(c,10),Ui.dp(c,6),Ui.dp(c,10),Ui.dp(c,6));root.setTextViewText(R.id.widget_title,compact?"✅ 記録 · "+today.getMonthValue()+"/"+today.getDayOfMonth()+"  ↕ "+rows.size()+"件":"✅ Routiaの記録");root.setTextViewText(R.id.widget_period,today.getMonthValue()+"/"+today.getDayOfMonth()+" · 今日の一歩");
        root.setTextViewText(R.id.widget_footer,"↕ スクロールで全"+rows.size()+"件");root.setViewVisibility(R.id.widget_footer,compact||rows.isEmpty()?View.GONE:View.VISIBLE);root.setEmptyView(R.id.widget_rows,R.id.widget_empty);
        root.setOnClickPendingIntent(R.id.widget_title,WidgetCollections.open(c,false));root.setOnClickPendingIntent(R.id.widget_empty,WidgetCollections.open(c,false));
        root.setPendingIntentTemplate(R.id.widget_rows,WidgetCollections.recordTemplate(c,widgetId));WidgetCollections.bind(c,root,R.id.widget_rows,widgetId,kind,rows);return root;
    }
    static RemoteViews row(Context c,Store.Habit h,LocalDate today,boolean collection){return row(c,h,today,collection,false);}
    static RemoteViews row(Context c,Store.Habit h,LocalDate today,boolean collection,boolean compact){
        if(!Store.get(c).metric(h.id()).kind().equals("check"))return numericRow(c,h,today,collection,compact);
        int color=WidgetColors.selected(c);Store store=Store.get(c);RemoteViews row=new RemoteViews(c.getPackageName(),compact?R.layout.widget_row_compact:R.layout.widget_row);row.setTextColor(R.id.row_goal,WidgetColors.ink(color));row.setTextColor(R.id.row_minimum,WidgetColors.muted(color));int level=store.level(h.id(),today);boolean recovery=store.recovery(h,today);row.setTextViewText(R.id.row_goal,h.goal());row.setTextViewText(R.id.row_minimum,level>0?Rules.grade(level):"🌱 "+store.goals(h.id()).minimum()+" / 🏆 "+store.goals(h.id()).full());
        int[] ids={R.id.row_min_button,R.id.row_full_button,R.id.row_extra_button},backgrounds={R.drawable.widget_grade_mini,R.drawable.widget_grade_goal,R.drawable.widget_grade_challenge};String[] icons={"🌱","🏆","🚀"},labels={"🌱 ミニ","🏆 目標","🚀 挑戦"};
        for(int at=0;at<3;at++){int grade=at+1,button=ids[at];row.setTextColor(button,AchievementColors.foreground(grade));row.setInt(button,"setBackgroundResource",backgrounds[at]);row.setTextViewText(button,compact?icons[at]+(level>=grade?"✓":""):labels[at]);row.setContentDescription(button,h.goal()+" · "+Rules.grade(grade)+(level>=grade?" · 達成済み":""));if(collection)row.setOnClickFillInIntent(button,new Intent().putExtra("habit",h.id()).putExtra("level",grade).putExtra("shownDay",today.toString()));}return row;
    }
    static RemoteViews numericRow(Context c,Store.Habit h,LocalDate today,boolean collection,boolean compact){
        Store store=Store.get(c);Store.Metric metric=store.metric(h.id());Store.Entry entry=store.entry(h.id(),today);int color=WidgetColors.selected(c);
        RemoteViews row=new RemoteViews(c.getPackageName(),compact?R.layout.widget_numeric_row_compact:R.layout.widget_numeric_row);
        row.setTextViewText(R.id.row_goal,h.goal());row.setTextColor(R.id.row_goal,WidgetColors.ink(color));
        String value=entry.amount()==null?(entry.level()>0?"今日の数値を入力":"今日はまだ未記録"):"今日 "+Ui.number(entry.amount())+metric.unit();
        row.setTextViewText(R.id.row_minimum,value);row.setTextColor(R.id.row_minimum,WidgetColors.muted(color));
        row.setTextViewText(R.id.row_min_button,compact?"入力する":entry.amount()==null?(metric.kind().equals("time")?"分数を入力する":"数量を入力する"):"今日の合計を修正する");
        row.setTextColor(R.id.row_min_button,WidgetColors.buttonText(color));row.setInt(R.id.row_min_button,"setBackgroundResource",WidgetColors.buttonResource(color));row.setContentDescription(R.id.row_min_button,h.goal()+"。"+value+"。数値を一つ入力して記録");
        if(collection)row.setOnClickFillInIntent(R.id.row_min_button,new Intent().putExtra("habit",h.id()).putExtra("level",1).putExtra("shownDay",today.toString()));
        return row;
    }

}
