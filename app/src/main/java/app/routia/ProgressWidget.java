package app.routia;
import android.appwidget.*;
import android.content.*;
import android.graphics.*;
import android.view.*;
import android.widget.*;
import java.time.*;
import java.util.*;
/** Read-only progress collection: all habits remain reachable by scrolling. */
public class ProgressWidget extends AppWidgetProvider {
    static final String OPEN="app.routia.OPEN_PROGRESS",PINNED="app.routia.PROGRESS_WIDGET_PINNED";
    record Item(Store.Habit habit,Store.Metric metric,ProgressWidgetData.Report report){}
    static List<Item> snapshot(Context c){
        Store store=Store.get(c);LocalDate today=LocalDate.now();store.evaluateTickets(today);List<Item> items=new ArrayList<>();
        for(Store.Habit h:store.habits()){Store.Metric m=store.metric(h.id());List<ProgressWidgetData.Point> points=new ArrayList<>();for(Store.Entry e:store.entries(h.id(),h.created(),today))points.add(new ProgressWidgetData.Point(e.day(),e.level(),e.amount(),e.unit()));items.add(new Item(h,m,ProgressWidgetData.build(points,store.ticketDays(h.id()),h.created(),today,m.kind(),m.unit(),m.target())));}
        return items;
    }
    public static void update(Context c){AppWidgetManager manager=AppWidgetManager.getInstance(c);int[] ids=manager.getAppWidgetIds(new ComponentName(c,ProgressWidget.class));for(int id:ids)manager.updateAppWidget(id,views(c,id));WidgetCollections.refreshLegacy(manager,ids,R.id.progress_widget_rows);}
    @Override public void onUpdate(Context c,AppWidgetManager manager,int[] ids){for(int id:ids)manager.updateAppWidget(id,views(c,id));WidgetCollections.refreshLegacy(manager,ids,R.id.progress_widget_rows);Reminders.midnight(c);}
    @Override public void onAppWidgetOptionsChanged(Context c,AppWidgetManager manager,int id,android.os.Bundle options){manager.updateAppWidget(id,views(c,id));}
    @Override public void onReceive(Context c,Intent intent){if(PINNED.equals(intent.getAction())){update(c);Toast.makeText(c,"📈 成果ウィジェットを追加しました",Toast.LENGTH_LONG).show();return;}super.onReceive(c,intent);}
    static View preview(Context c){return WidgetCollections.preview(c,WidgetCollections.PROGRESS);}
    static RemoteViews views(Context c,int widgetId){
        int color=WidgetColors.selected(c);LocalDate today=LocalDate.now();List<WidgetCollections.Row> rows=WidgetCollections.rows(c,WidgetCollections.PROGRESS,true,WidgetCollections.chartWidth(c,widgetId));RemoteViews root=new RemoteViews(c.getPackageName(),R.layout.progress_widget);
        root.setImageViewBitmap(R.id.progress_mascot,Mascot.bitmap(c,Mascot.PROGRESS,96));root.setInt(R.id.progress_widget_root,"setBackgroundResource",WidgetColors.DRAWABLES[color]);root.setTextColor(R.id.progress_widget_title,WidgetColors.ink(color));root.setTextColor(R.id.progress_widget_empty,WidgetColors.muted(color));root.setTextColor(R.id.progress_widget_footer,WidgetColors.muted(color));
        root.setTextViewText(R.id.progress_widget_footer,"↕ スクロールで全"+rows.size()+"件");root.setViewVisibility(R.id.progress_widget_footer,rows.isEmpty()?View.GONE:View.VISIBLE);root.setEmptyView(R.id.progress_widget_rows,R.id.progress_widget_empty);
        root.setOnClickPendingIntent(R.id.progress_widget_title,WidgetCollections.open(c,true));root.setOnClickPendingIntent(R.id.progress_widget_empty,WidgetCollections.open(c,false));root.setPendingIntentTemplate(R.id.progress_widget_rows,WidgetCollections.open(c,true));
        WidgetCollections.bind(c,root,R.id.progress_widget_rows,widgetId,WidgetCollections.PROGRESS,rows);return root;
    }
    static RemoteViews row(Context c,Item item,boolean collection,int chartWidth){
        int color=WidgetColors.selected(c);ProgressWidgetData.Report r=item.report();RemoteViews row=new RemoteViews(c.getPackageName(),R.layout.progress_widget_row);
        row.setTextViewText(R.id.progress_goal,item.habit().goal());row.setTextColor(R.id.progress_goal,WidgetColors.ink(color));
        String summary=ProgressWidgetData.summary(r);
        row.setTextViewText(R.id.progress_amount,summary);row.setContentDescription(R.id.progress_amount,summary);row.setTextColor(R.id.progress_amount,WidgetColors.muted(color));
        row.setImageViewBitmap(R.id.progress_week,item.metric().kind().equals("check")?weeklyChart(r.week(),color,chartWidth):numericChart(r.week(),color,chartWidth,item.metric()));row.setContentDescription(R.id.progress_week,weekDescription(r.week(),item.metric()));
        if(collection)row.setOnClickFillInIntent(R.id.progress_row_root,new Intent());return row;
    }
    static String weekDescription(List<ProgressWidgetData.WeekDay> week){
        StringBuilder text=new StringBuilder("今週の達成状況の折れ線グラフ。ミニ達成も成功です。");
        for(ProgressWidgetData.WeekDay day:week){text.append(day.day().getMonthValue()).append("月").append(day.day().getDayOfMonth()).append("日、").append(!day.available()?"対象外":day.ticket()?"継続チケットで休み":day.level()==0?"未達成":grade(day.level())+"達成").append("。");}
        return text.toString();
    }
    static String weekDescription(List<ProgressWidgetData.WeekDay> week,Store.Metric metric){
        if(metric.kind().equals("check"))return weekDescription(week);
        StringBuilder text=new StringBuilder("今週の"+metric.unit()+"の折れ線グラフ。");
        for(var day:week)text.append(day.day().getMonthValue()).append("月").append(day.day().getDayOfMonth()).append("日、").append(!day.available()?"対象外":day.ticket()?"継続チケットで休み":day.amount()==null?day.level()>0?"過去の達成記録あり、数値なし":"未記録":Ui.number(day.amount())+metric.unit()).append("。");
        return text.toString();
    }
    static String tickLabel(double value){String text=Ui.number(value);return text.length()<=6?text:new java.text.DecimalFormat("0.#E0",java.text.DecimalFormatSymbols.getInstance(java.util.Locale.ROOT)).format(value);}
    static Bitmap numericChart(List<ProgressWidgetData.WeekDay> week,int color,int width,Store.Metric metric){
        Bitmap image=Bitmap.createBitmap(width*3,92*3,Bitmap.Config.ARGB_8888);Canvas canvas=new Canvas(image);canvas.scale(3,3);Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);paint.setTextSize(10);paint.setColor(WidgetColors.muted(color));
        var first=week.get(0).day();var last=week.get(6).day();canvas.drawText(first.getMonthValue()+"/"+first.getDayOfMonth()+"–"+last.getMonthValue()+"/"+last.getDayOfMonth()+" · "+metric.unit(),0,11,paint);
        double peak=Math.max(.001,metric.target());for(var day:week)if(day.amount()!=null)peak=Math.max(peak,day.amount());
        double magnitude=Math.pow(10,Math.floor(Math.log10(peak/3))),interval=Math.ceil(peak/3/magnitude)*magnitude,maximum=interval*3;
        float left=40,right=width-8,step=(right-left)/6f,base=62,height=36;int accent=WidgetColors.dark(color)?0xffA5D8B2:0xff37774F;
        for(int i=0;i<4;i++){float y=base-i*height/3;paint.setTextAlign(Paint.Align.LEFT);paint.setTextSize(8);paint.setColor(WidgetColors.muted(color));canvas.drawText(tickLabel(interval*i),0,y+3,paint);paint.setAlpha(55);paint.setStrokeWidth(.6f);canvas.drawLine(left,y,right,y,paint);paint.setAlpha(255);}
        if(metric.target()>0){paint.setColor(accent);paint.setAlpha(90);paint.setStrokeWidth(1);paint.setPathEffect(new DashPathEffect(new float[]{3,3},0));canvas.drawLine(left,base-(float)(metric.target()/maximum)*height,right,base-(float)(metric.target()/maximum)*height,paint);paint.setPathEffect(null);paint.setAlpha(255);}
        for(int i=1;i<7;i++){var previous=week.get(i-1);var day=week.get(i);if(previous.available()&&day.available()&&previous.amount()!=null&&day.amount()!=null){paint.setColor(accent);paint.setStrokeWidth(2);canvas.drawLine(left+(i-1)*step,base-(float)(previous.amount()/maximum)*height,left+i*step,base-(float)(day.amount()/maximum)*height,paint);}}
        String[] weekdays={"月","火","水","木","金","土","日"};for(int i=0;i<7;i++){
            var day=week.get(i);float x=left+i*step;
            if(day.available()&&day.amount()!=null){paint.setColor(accent);canvas.drawCircle(x,base-(float)(day.amount()/maximum)*height,2.6f,paint);}
            if(day.ticket()){paint.setTextAlign(Paint.Align.CENTER);paint.setTextSize(8);paint.setColor(WidgetColors.dark(color)?0xffEDAAA5:0xffB95850);canvas.drawText("休",x,base-3,paint);}
            paint.setTextAlign(Paint.Align.CENTER);paint.setTextSize(9);paint.setColor(WidgetColors.ink(color));paint.setAlpha(day.available()?255:105);paint.setTypeface(day.day().equals(LocalDate.now())?Typeface.DEFAULT_BOLD:Typeface.DEFAULT);canvas.drawText(String.valueOf(day.day().getDayOfMonth()),x,79,paint);canvas.drawText(weekdays[i],x,90,paint);paint.setAlpha(255);paint.setTypeface(Typeface.DEFAULT);
        }return image;
    }
    static String grade(int level){return level==3?"挑戦":level==2?"目標":"ミニ";}
    static Bitmap weeklyChart(List<ProgressWidgetData.WeekDay> week,int color,int width){
        // This graph represents recorded achievement grades for every habit; it never invents a numeric measurement.
        int height=92;Bitmap image=Bitmap.createBitmap(width*3,height*3,Bitmap.Config.ARGB_8888);Canvas canvas=new Canvas(image);canvas.scale(3,3);
        Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);paint.setTypeface(Typeface.DEFAULT);paint.setColor(WidgetColors.muted(color));paint.setTextSize(10);
        LocalDate first=week.get(0).day(),last=week.get(6).day();
        canvas.drawText(first.getMonthValue()+"/"+first.getDayOfMonth()+"–"+last.getMonthValue()+"/"+last.getDayOfMonth(),0,11,paint);
        float left=43,right=width-8,base=62,step=(right-left)/6f;String[] grades={"未達","ミニ","目標","挑戦"};
        for(int grade=0;grade<=3;grade++){
            float y=base-grade*12;paint.setTextAlign(Paint.Align.LEFT);paint.setTextSize(8);paint.setColor(WidgetColors.muted(color));canvas.drawText(grades[grade],0,y+3,paint);
            paint.setAlpha(55);paint.setStrokeWidth(0.6f);canvas.drawLine(left,y,right,y,paint);paint.setAlpha(255);
        }
        int accent=WidgetColors.dark(color)?0xffA5D8B2:0xff37774F,rest=WidgetColors.dark(color)?0xffEDAAA5:0xffB95850;
        for(int i=1;i<7;i++){
            var previous=week.get(i-1);var day=week.get(i);
            if(previous.available()&&day.available()&&!previous.ticket()&&!day.ticket()){
                paint.setColor(accent);paint.setStrokeWidth(2);canvas.drawLine(left+(i-1)*step,base-previous.level()*12,left+i*step,base-day.level()*12,paint);
            }
        }
        String[] weekdays={"月","火","水","木","金","土","日"};
        for(int i=0;i<7;i++){
            var day=week.get(i);float x=left+i*step,y=base-day.level()*12;
            if(day.available()){
                paint.setColor(day.ticket()?rest:accent);paint.setStyle(day.ticket()?Paint.Style.STROKE:Paint.Style.FILL);paint.setStrokeWidth(1.5f);canvas.drawCircle(x,day.ticket()?base:y,2.6f,paint);paint.setStyle(Paint.Style.FILL);
                if(day.ticket()){paint.setTextAlign(Paint.Align.CENTER);paint.setTextSize(8);canvas.drawText("休",x,base-6,paint);}
            }
            paint.setTextAlign(Paint.Align.CENTER);paint.setTextSize(9);paint.setTypeface(day.day().equals(LocalDate.now())?Typeface.DEFAULT_BOLD:Typeface.DEFAULT);paint.setColor(WidgetColors.ink(color));paint.setAlpha(day.available()?255:105);
            canvas.drawText(String.valueOf(day.day().getDayOfMonth()),x,79,paint);canvas.drawText(weekdays[i],x,90,paint);paint.setAlpha(255);paint.setTypeface(Typeface.DEFAULT);
        }
        return image;
    }
}
