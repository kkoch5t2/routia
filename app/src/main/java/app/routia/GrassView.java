package app.routia;
import android.content.*;
import android.graphics.*;
import android.view.*;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.function.Consumer;
import java.util.ArrayList;
/** GitHub-style weekly columns, with a fixed weekday rail supplied by the parent. */
final class GrassView extends View {
    static final int[] COLORS=AchievementColors.calendar();
    static final int TICKET=5;
    static final int HEADER=36,STEP=22;
    static final String[] WEEKDAYS={"日","月","火","水","木","金","土"};
    final String[] monthLabels;final int[] monthColumns,monthRows;final int naturalWidth,weekCount;
    final int[] colors;final int surface;
    final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);final LocalDate start,end,sunday;final int[] shades;final double[] values;final boolean numeric;final String unit;
    Consumer<LocalDate> onDayClick;int selected=-1;float downX,downY;
    public GrassView(Context c){this(c,LocalDate.now(),LocalDate.now(),new int[]{0},new double[]{-1},false,"");}
    GrassView(Context c,LocalDate start,LocalDate end,int[] shades,double[] values,boolean numeric,String unit){super(c);colors=palette(c);surface=Ui.surface(c,Color.WHITE);this.start=start;this.end=end;this.shades=shades;this.values=values;this.numeric=numeric;this.unit=unit;sunday=start.minusDays(start.getDayOfWeek().getValue()%7);weekCount=(int)ChronoUnit.DAYS.between(sunday,end)/7+1;
        ArrayList<String> labels=new ArrayList<>();ArrayList<Integer> columns=new ArrayList<>(),rows=new ArrayList<>();p.setTextSize(d(11));p.setTypeface(Typeface.DEFAULT_BOLD);float[] nextLabel={0,0};float width=d(2+weekCount*STEP+4);
        for(LocalDate day=start;!day.isAfter(end);day=day.withDayOfMonth(1).plusMonths(1)){int col=(int)ChronoUnit.DAYS.between(sunday,day)/7;float x=d(2+col*STEP);String label=day.getYear()+"年"+day.getMonthValue()+"月";int row=x>=nextLabel[0]?0:1;labels.add(label);columns.add(col);rows.add(row);nextLabel[row]=x+p.measureText(label)+d(10);width=Math.max(width,nextLabel[row]);}
        monthLabels=labels.toArray(new String[0]);monthColumns=new int[columns.size()];monthRows=new int[columns.size()];for(int at=0;at<columns.size();at++){monthColumns[at]=columns.get(at);monthRows[at]=rows.get(at);}naturalWidth=(int)Math.ceil(width);
        setContentDescription(numeric?"1マスは1日。縦は日曜から土曜、横は1週間。緑の段階は取り組んだ量を表します。赤いマスは継続チケットを自動使用した日。左右にスクロールし、タップで記録を表示。":"1マスは1日。縦は日曜から土曜、横は1週間。緑の段階はミニ達成・目標達成・チャレンジ達成の3段階。灰色は未記録。赤いマスは継続チケットを自動使用した日。左右にスクロールし、タップで記録を表示。");setFocusable(true);setClickable(true);}
    int width(){if(layoutMode>0)return Ui.dp(getContext(),7*cell);return naturalWidth;}
    int layoutMode,cell=19;
    void compact(int mode,int size){layoutMode=mode;cell=size;setContentDescription((mode==1?"1週間のカレンダー。左から月曜〜日曜。":"月間カレンダー。左から日曜〜土曜。")+"日付をタップで、その日の記録を表示。赤いマスは継続チケットを自動使用した日。");}
    int height(){if(layoutMode==0)return Ui.dp(getContext(),HEADER+7*STEP+2);int offset=layoutMode==1?0:start.getDayOfWeek().getValue()%7;int rows=(offset+shades.length+6)/7;return Ui.dp(getContext(),26+rows*cell+8);}
    static int blend(int from,int to,float amount){
        return Color.rgb(Math.round(Color.red(from)*(1-amount)+Color.red(to)*amount),Math.round(Color.green(from)*(1-amount)+Color.green(to)*amount),Math.round(Color.blue(from)*(1-amount)+Color.blue(to)*amount));
    }
    static int[] palette(Context context){
        int background=Ui.surface(context,Color.WHITE);
        int empty=blend(blend(background,AppColors.background(context),.18f),Ui.ink(context,Ui.INK),.08f);
        return new int[]{empty,COLORS[1],COLORS[2],COLORS[3],COLORS[4],COLORS[5]};
    }
    int cellColor(int shade){return shade<0?blend(surface,colors[0],.42f):colors[shade];}
    void drawCompact(Canvas c){
        String[] labels=layoutMode==1?new String[]{"月","火","水","木","金","土","日"}:WEEKDAYS;
        p.setTextAlign(Paint.Align.CENTER);p.setTextSize(d(12));p.setTypeface(Typeface.create("sans-serif",Typeface.NORMAL));p.setColor(Ui.ink(getContext(),Ui.MUTED));
        for(int col=0;col<7;col++)c.drawText(labels[col],d(col*cell+cell/2f),d(17),p);
        int offset=layoutMode==1?0:start.getDayOfWeek().getValue()%7;
        for(int i=0;i<shades.length;i++){
            int shade=shades[i],col=(offset+i)%7,row=(offset+i)/7;float x=d(col*cell+3),y=d(26+row*cell+3),size=d(cell-6);
            p.setColor(cellColor(shade));c.drawRoundRect(x,y,x+size,y+size,d(5),d(5),p);
            int ink=shade<=0?Ui.ink(getContext(),Ui.MUTED):(shade>=3&&shade<TICKET?Color.WHITE:Ui.INK);
            p.setTextSize(d(13));p.setColor(ink);
            c.drawText(String.valueOf(start.plusDays(i).getDayOfMonth()),x+size/2,y+size/2+d(5),p);
            if(i==selected)c.drawCircle(x+size/2,y+size-d(4),d(1.5f),p);
        }
        p.setTextAlign(Paint.Align.LEFT);
    }
    float d(float n){return Ui.dp(getContext(),n);}
    @Override protected void onDraw(Canvas c){
        super.onDraw(c);if(layoutMode>0){drawCompact(c);return;}
        p.setTypeface(Typeface.DEFAULT_BOLD);p.setTextAlign(Paint.Align.LEFT);p.setTextSize(d(11));p.setColor(Ui.ink(getContext(),Ui.MUTED));
        for(int at=0;at<monthLabels.length;at++)c.drawText(monthLabels[at],d(2+monthColumns[at]*STEP),d(17+monthRows[at]*18),p);
        for(int index=0;index<shades.length;index++){
            LocalDate day=start.plusDays(index);int col=(int)ChronoUnit.DAYS.between(sunday,day)/7,row=day.getDayOfWeek().getValue()%7;
            float x=d(5+col*STEP),y=d(HEADER+row*STEP+3),size=d(STEP-6);
            p.setColor(cellColor(shades[index]));c.drawRoundRect(x,y,x+size,y+size,d(3),d(3),p);
            if(index==selected){p.setColor(Ui.ink(getContext(),Ui.GREEN));c.drawCircle(x+size/2,y+size/2,d(2),p);}
        }
    }
    @Override public boolean onTouchEvent(MotionEvent e){
        if(e.getAction()==MotionEvent.ACTION_DOWN){downX=e.getX();downY=e.getY();return true;}
        if(e.getAction()==MotionEvent.ACTION_UP){if(Math.abs(e.getX()-downX)>ViewConfiguration.get(getContext()).getScaledTouchSlop()||Math.abs(e.getY()-downY)>ViewConfiguration.get(getContext()).getScaledTouchSlop())return true;if(layoutMode>0){int col=(int)(e.getX()/d(cell)),row=(int)((e.getY()-d(26))/d(cell));int offset=layoutMode==1?0:start.getDayOfWeek().getValue()%7,index=row*7+col-offset;if(e.getX()>=0&&e.getY()>=d(26)&&col>=0&&col<7&&index>=0&&index<shades.length){selected=index;invalidate();if(onDayClick!=null)onDayClick.accept(start.plusDays(index));}performClick();return true;}int col=(int)((e.getX()-d(2))/d(STEP)),row=(int)((e.getY()-d(HEADER))/d(STEP));if(e.getX()>=d(2)&&e.getY()>=d(HEADER)&&col<weekCount&&row>=0&&row<7){LocalDate day=sunday.plusDays(col*7L+row);int index=(int)ChronoUnit.DAYS.between(start,day);if(index>=0&&index<shades.length){selected=index;invalidate();if(onDayClick!=null)onDayClick.accept(day);}}performClick();return true;}
        return true;
    }
    @Override public boolean performClick(){return super.performClick();}
    static final class WeekLabels extends View {
        final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);
        public WeekLabels(Context c){super(c);setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);}
        @Override protected void onDraw(Canvas c){p.setColor(Ui.ink(getContext(),Ui.MUTED));p.setTypeface(Typeface.DEFAULT_BOLD);p.setTextSize(Ui.dp(getContext(),12));for(int row=0;row<7;row++)c.drawText(WEEKDAYS[row],Ui.dp(getContext(),2),Ui.dp(getContext(),HEADER+row*STEP+15),p);}
    }
}
