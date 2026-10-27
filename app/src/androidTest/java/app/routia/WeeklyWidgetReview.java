package app.routia;
import android.app.*;
import android.content.*;
import android.graphics.*;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.view.*;
import android.view.inspector.WindowInspector;
import android.widget.*;
import java.time.*;
import java.io.*;
import java.util.*;
/** Synthetic display fixtures only. Never run on a physical device or touch its habits. */
final class WeeklyWidgetReview {
    final Instrumentation instrumentation;int checks;
    WeeklyWidgetReview(Instrumentation i){instrumentation=i;}
    void check(boolean value,String message){checks++;if(!value)throw new AssertionError(message);}
    int run()throws Exception{
        if(!Build.HARDWARE.contains("ranchu")&&!Build.HARDWARE.contains("goldfish"))throw new AssertionError("Emulator only");
        Context c=instrumentation.getTargetContext();rollover(c);palettes(c);LocalDate today=LocalDate.of(2026,10,24),created=today.minusYears(100),monday=today.minusDays(5);
        List<ProgressWidgetData.Point> points=new ArrayList<>();
        for(int i=0;i<6;i++)if(i!=2)points.add(new ProgressWidgetData.Point(monday.plusDays(i),i%3+1,i==3?null:i==4?0d:5d*(i+1),"分"));
        var base=ProgressWidgetData.build(points,Set.of(monday.plusDays(2)),created,today,"check","",0);
        var longRun=new ProgressWidgetData.Report(31,36524,36524,0,0,0,0,base.shades(),base.week());
        c.getSharedPreferences("routia_widget",Context.MODE_PRIVATE).edit().putInt("background",6).commit();
        render(c,"weekly-check",288,"英語を聞く",new Store.Metric("check",0,""),longRun);
        render(c,"weekly-narrow",188,"寝る前に英語のニュースを聞く",new Store.Metric("check",0,""),longRun);
        c.getSharedPreferences("routia_widget",Context.MODE_PRIVATE).edit().putInt("background",0).commit();
        var numeric=ProgressWidgetData.build(points,Set.of(monday.plusDays(2)),created,today,"time","分",15);
        render(c,"weekly-time",288,"英語を15分聞く",new Store.Metric("time",15,"分"),numeric);
        String description=ProgressWidget.weekDescription(numeric.week());
        check(description.contains("折れ線"),"Chart is described as achievement line graph");
        check(description.contains("ミニ達成"),"Missing numeric data retains recorded achievement");
        check(description.contains("継続チケット"),"Rest does not read as success");
        check(description.contains("対象外"),"Future day is distinguished");
        var never=ProgressWidgetData.build(List.of(),Set.of(),today,today,"time","分",15);
        render(c,"line-zero",288,"まだ一度も達成していない習慣",new Store.Metric("time",15,"分"),never);
        check(ProgressWidgetData.summary(never).equals("🔥 連続0日 / ✅ 累計0日"),"No achievement is invented in zero state");
        c.getSharedPreferences("routia_widget",Context.MODE_PRIVATE).edit().putInt("background",1).commit();render(c,"line-white",288,"英語を聞く",new Store.Metric("check",0,""),base);
        picker(c);
        return checks;
    }
    void rollover(Context c)throws Exception{
        Store store=Store.get(c);check(store.habits().isEmpty(),"Requires an empty emulator app");
        LocalDate today=LocalDate.now(),yesterday=today.minusDays(1);
        long first=store.saveHabit(0,"","昨日だけ達成した習慣","1回","check",0,""),empty=store.saveHabit(0,"","未達成の習慣","1回","time",15,"分");
        try{
            store.getWritableDatabase().execSQL("UPDATE habits SET created=? WHERE id=?",new Object[]{yesterday.toString(),first});
            store.getWritableDatabase().execSQL("INSERT INTO records(habit_id,day,level) VALUES(?,?,2)",new Object[]{first,yesterday.toString()});
            var items=ProgressWidget.snapshot(c);
            var prior=items.stream().filter(item->item.habit().id()==first).findFirst().orElseThrow().report();
            var never=items.stream().filter(item->item.habit().id()==empty).findFirst().orElseThrow().report();
            check(prior.totalDays()==1&&prior.week().stream().filter(day->day.day().equals(today)).allMatch(day->day.level()==0),"Yesterday does not copy into today");
            check(never.totalDays()==0&&never.streak()==0,"New habit has no achievement");
            c.getSharedPreferences("widget_refresh",Context.MODE_PRIVATE).edit().putString("day",yesterday.toString()).commit();
            new SystemReceiver().onReceive(c,new Intent(Intent.ACTION_DATE_CHANGED));
            check(c.getSharedPreferences("widget_refresh",Context.MODE_PRIVATE).getString("day","").equals(today.toString()),"Date broadcast refreshes collections");
            check(store.level(first,today)==0&&store.level(empty,today)==0,"Refresh does not record today's success");
            instrumentation.runOnMainSync(()->{
                FrameLayout parent=new FrameLayout(c);View row=HabitWidget.row(c,store.habit(first),today,false).apply(c,parent);
                check(!((TextView)row.findViewById(R.id.row_min_button)).getText().toString().contains("達成済"),"New-day minimum button is unachieved");
                check(!((TextView)row.findViewById(R.id.row_full_button)).getText().toString().contains("達成済"),"New-day normal button is unachieved");
                View widget=HabitWidget.layout(c,0,false).apply(c,parent);
                check(((TextView)widget.findViewById(R.id.widget_period)).getText().toString().startsWith(today.getMonthValue()+"/"+today.getDayOfMonth()),"Recording widget has current date");
            });
            new ReminderReceiver().onReceive(c,new Intent(Reminders.MIDNIGHT));WidgetRefresh.refresh(c,false);
            check(store.records(first,yesterday,today).size()==1&&store.level(first,today)==0,"Midnight does not fabricate success");
        }finally{store.delete(first);store.delete(empty);}
    }
    void palettes(Context c){
        int original=AppColors.selected(c),widgetOriginal=WidgetColors.selected(c);
        try{
            check(WidgetColors.NAMES.length==7,"Black is an additional palette");
            check(Color.red(WidgetColors.COLORS[0])-Color.blue(WidgetColors.COLORS[0])>=40,"Cream is visibly warm");
            check(WidgetColors.COLORS[1]==Color.WHITE,"White is white");
            check(WidgetColors.COLORS[6]==0xff121212,"Black is neutral");
            for(int color=0;color<WidgetColors.NAMES.length;color++){
                final int selected=color;instrumentation.runOnMainSync(()->{
                    LinearLayout preview=WidgetColors.preview(c,selected);
                    check(((GradientDrawable)preview.getBackground()).getColor().getDefaultColor()==WidgetColors.COLORS[selected],"Preview matches swatch");
                    AppColors.save(c,selected);
                    check(Ui.night(c)==WidgetColors.dark(selected),"Dark foreground classification");
                    check(Ui.surface(c,Ui.BG)==WidgetColors.COLORS[selected],"App background matches swatch");
                    check(WidgetColors.selected(c)==widgetOriginal,"App color leaves widget preference alone");
                    double contrast=(Math.max(Color.luminance(WidgetColors.ink(selected)),Color.luminance(WidgetColors.COLORS[selected]))+.05)/(Math.min(Color.luminance(WidgetColors.ink(selected)),Color.luminance(WidgetColors.COLORS[selected]))+.05);
                    check(contrast>=4.5,"Readable contrast");
                });
            }
            AppColors.save(c,6);int card=Ui.surface(c,Color.WHITE),border=Ui.surface(c,0xffDDE5DB);
            check(Color.red(card)==Color.green(card)&&Color.green(card)==Color.blue(card),"Black cards use gray");
            check(Color.red(border)==Color.green(border)&&Color.green(border)==Color.blue(border),"Black borders use gray");
        }finally{AppColors.save(c,original);}
    }
    void picker(Context c)throws Exception{
        int original=WidgetColors.selected(c);UxReview ui=new UxReview(instrumentation);
        ui.a=instrumentation.startActivitySync(new Intent(c,MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK));
        ui.mainTap("設定");ui.mainTap("背景色を選ぶ · "+WidgetColors.NAMES[original]);
        instrumentation.runOnMainSync(()->{
            View window=null;for(View candidate:WindowInspector.getGlobalWindowViews())if(PreviewFrames.find(candidate,"ウィジェットの背景色")!=null){window=candidate;break;}
            if(window==null)throw new AssertionError("Missing color picker");
            for(String name:WidgetColors.NAMES)check(PreviewFrames.find(window,name)!=null,"Picker includes "+name);
            PreviewFrames.find(window,"ブラック").performClick();ScrollView scroll=PreviewFrames.scroll(window);if(scroll!=null)scroll.fullScroll(View.FOCUS_DOWN);
            check(WidgetColors.selected(c)==original,"Preview does not save prematurely");
        });
        ui.shot("seven-color-picker");ui.tap("この色で保存");check(WidgetColors.selected(c)==6,"Black can be saved through the actual picker");
        WidgetColors.save(c,original);instrumentation.runOnMainSync(()->ui.a.finish());
    }
    void render(Context c,String name,int width,String title,Store.Metric metric,ProgressWidgetData.Report report)throws Exception{
        final Throwable[] error={null};
        instrumentation.runOnMainSync(()->{try{
            var habit=new Store.Habit(1,"",title,"",LocalDate.of(1926,10,24),0,0,false);
            RemoteViews rv=ProgressWidget.row(c,new ProgressWidget.Item(habit,metric,report),false,width);
            FrameLayout parent=new FrameLayout(c);View v=rv.apply(c,parent);
            int px=Ui.dp(c,width);v.measure(View.MeasureSpec.makeMeasureSpec(px,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(0,View.MeasureSpec.UNSPECIFIED));v.layout(0,0,px,v.getMeasuredHeight());
            TextView goal=v.findViewById(R.id.progress_goal),amount=v.findViewById(R.id.progress_amount);
            check(goal.getTextSize()>amount.getTextSize(),"Habit title takes visual priority");
            check(goal.getLayout().getEllipsisCount(goal.getLineCount()-1)==0,"Representative title fits");
            check(amount.getLayout().getEllipsisCount(0)==0&&amount.getLayout().getLineEnd(0)==amount.getText().length(),"Decades of daily progress fits");
            check(amount.getLineCount()==1,"Summary is exactly one line");
            check(amount.getText().toString().equals(ProgressWidgetData.summary(report)),"Only the two requested counts are shown");
            ImageView chart=v.findViewById(R.id.progress_week);
            check(chart.getWidth()==px,"Chart uses the entire available width");
            check(chart.getDrawable()!=null,"Weekly chart has rendered bitmap");
            check(chart.getTop()>=amount.getBottom(),"Chart never overlaps summary");
            Bitmap b=Bitmap.createBitmap(px,v.getHeight(),Bitmap.Config.ARGB_8888);Canvas canvas=new Canvas(b);canvas.drawColor(WidgetColors.COLORS[WidgetColors.selected(c)]);v.draw(canvas);
            try(FileOutputStream out=new FileOutputStream(new File(c.getExternalFilesDir(null),name+".png"))){b.compress(Bitmap.CompressFormat.PNG,100,out);}b.recycle();
        }catch(Throwable t){error[0]=t;}});
        if(error[0]!=null)throw new AssertionError(error[0]);
    }
}
