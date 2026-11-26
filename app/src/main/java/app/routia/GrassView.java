package app.routia;
import android.content.*;
import android.graphics.*;
import android.os.Bundle;
import android.view.*;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityManager;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.accessibility.AccessibilityNodeProvider;
import android.widget.Button;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.Locale;
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
    private static final DateTimeFormatter SPOKEN_DATE =
        DateTimeFormatter.ofPattern("yyyy年M月d日 EEEE", Locale.JAPANESE);
    private static final int NO_DAY = Integer.MIN_VALUE;
    private final DayNodes dayNodes = new DayNodes();
    Consumer<LocalDate> onDayClick;
    Function<LocalDate, String> dayStatus;
    int selected=-1;
    float downX,downY;
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
    private boolean isDay(int index) {
        return index >= 0 && index < shades.length;
    }

    private int dayAt(float x, float y) {
        int left = layoutMode == 0 ? Ui.dp(getContext(), 2) : 0;
        int top = Ui.dp(getContext(), layoutMode == 0 ? HEADER : 26);
        if (x < left || y < top) return NO_DAY;
        float size = (layoutMode == 0 ? STEP : cell) * getResources().getDisplayMetrics().density;
        int col = (int) ((x - left) / size);
        int row = (int) ((y - top) / size);
        if (layoutMode > 0 && col < 7) {
            int offset = layoutMode == 1 ? 0 : start.getDayOfWeek().getValue() % 7;
            int index = row * 7 + col - offset;
            return isDay(index) ? index : NO_DAY;
        }
        if (layoutMode == 0 && col < weekCount && row < 7) {
            int index = (int) ChronoUnit.DAYS.between(start, sunday.plusDays(col * 7L + row));
            return isDay(index) ? index : NO_DAY;
        }
        return NO_DAY;
    }

    private Rect dayBounds(int index) {
        int col, row, size, left, top;
        if (layoutMode == 0) {
            int offset = (int) ChronoUnit.DAYS.between(sunday, start) + index;
            col = offset / 7;
            row = offset % 7;
            size = STEP;
            left = 2;
            top = HEADER;
        } else {
            int offset = index + (layoutMode == 1 ? 0 : start.getDayOfWeek().getValue() % 7);
            col = offset % 7;
            row = offset / 7;
            size = cell;
            left = 0;
            top = 26;
        }
        return new Rect(Ui.dp(getContext(), left + col * size), Ui.dp(getContext(), top + row * size),
            Ui.dp(getContext(), left + (col + 1) * size), Ui.dp(getContext(), top + (row + 1) * size));
    }

    private void openDay(int index) {
        selected = index;
        invalidate();
        dayNodes.send(index, AccessibilityEvent.TYPE_VIEW_CLICKED);
        if (onDayClick != null) onDayClick.accept(start.plusDays(index));
    }

    @Override public boolean onTouchEvent(MotionEvent event) {
        if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
            downX = event.getX();
            downY = event.getY();
        } else if (event.getActionMasked() == MotionEvent.ACTION_UP) {
            int slop = ViewConfiguration.get(getContext()).getScaledTouchSlop();
            if (Math.abs(event.getX() - downX) <= slop && Math.abs(event.getY() - downY) <= slop) {
                int index = dayAt(event.getX(), event.getY());
                if (isDay(index)) openDay(index);
                performClick();
            }
        }
        return true;
    }

    @Override public boolean performClick() { return super.performClick(); }

    @Override public AccessibilityNodeProvider getAccessibilityNodeProvider() {
        return dayStatus == null ? super.getAccessibilityNodeProvider() : dayNodes;
    }

    @Override public boolean dispatchHoverEvent(MotionEvent event) {
        AccessibilityManager manager = getContext().getSystemService(AccessibilityManager.class);
        if (dayStatus != null && manager != null && manager.isTouchExplorationEnabled()) {
            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_HOVER_ENTER:
                case MotionEvent.ACTION_HOVER_MOVE:
                    if (dayNodes.hover(dayAt(event.getX(), event.getY()))) return true;
                    break;
                case MotionEvent.ACTION_HOVER_EXIT:
                    if (dayNodes.hover(NO_DAY)) return true;
                    break;
                default:
                    break;
            }
        }
        return super.dispatchHoverEvent(event);
    }

    private final class DayNodes extends AccessibilityNodeProvider {
        private int focused = NO_DAY;
        private int hovered = NO_DAY;

        @Override public AccessibilityNodeInfo createAccessibilityNodeInfo(int index) {
            if (index == HOST_VIEW_ID) {
                AccessibilityNodeInfo host = AccessibilityNodeInfo.obtain(GrassView.this);
                onInitializeAccessibilityNodeInfo(host);
                host.setClickable(false);
                host.setFocusable(false);
                host.removeAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_CLICK);
                host.removeAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_FOCUS);
                for (int day = 0; day < shades.length; day++) host.addChild(GrassView.this, day);
                return host;
            }
            if (!isDay(index) || dayStatus == null) return null;
            AccessibilityNodeInfo day = AccessibilityNodeInfo.obtain();
            day.setSource(GrassView.this, index);
            day.setParent(GrassView.this);
            day.setClassName(Button.class.getName());
            day.setPackageName(getContext().getPackageName());
            day.setContentDescription(label(index));
            day.setEnabled(true);
            day.setClickable(true);
            day.setFocusable(true);
            day.setAccessibilityFocused(focused == index);
            day.addAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_CLICK);
            day.addAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_SHOW_ON_SCREEN);
            day.addAction(focused == index
                ? AccessibilityNodeInfo.AccessibilityAction.ACTION_CLEAR_ACCESSIBILITY_FOCUS
                : AccessibilityNodeInfo.AccessibilityAction.ACTION_ACCESSIBILITY_FOCUS);
            Rect bounds = dayBounds(index);
            day.setBoundsInParent(bounds);
            Rect visible = new Rect();
            if (isShown() && getLocalVisibleRect(visible) && visible.intersect(bounds)) {
                day.setVisibleToUser(true);
                bounds = visible;
            }
            int[] location = new int[2];
            getLocationOnScreen(location);
            bounds.offset(location[0], location[1]);
            day.setBoundsInScreen(bounds);
            return day;
        }

        @Override public AccessibilityNodeInfo findFocus(int type) {
            return type == AccessibilityNodeInfo.FOCUS_ACCESSIBILITY && isDay(focused)
                ? createAccessibilityNodeInfo(focused) : null;
        }

        @Override public boolean performAction(int index, int action, Bundle arguments) {
            if (index == HOST_VIEW_ID) return GrassView.this.performAccessibilityAction(action, arguments);
            if (!isDay(index) || dayStatus == null) return false;
            switch (action) {
                case AccessibilityNodeInfo.ACTION_ACCESSIBILITY_FOCUS:
                    if (focused == index) return false;
                    int previous = focused;
                    focused = index;
                    send(previous, AccessibilityEvent.TYPE_VIEW_ACCESSIBILITY_FOCUS_CLEARED);
                    send(index, AccessibilityEvent.TYPE_VIEW_ACCESSIBILITY_FOCUSED);
                    return true;
                case AccessibilityNodeInfo.ACTION_CLEAR_ACCESSIBILITY_FOCUS:
                    if (focused != index) return false;
                    focused = NO_DAY;
                    send(index, AccessibilityEvent.TYPE_VIEW_ACCESSIBILITY_FOCUS_CLEARED);
                    return true;
                case AccessibilityNodeInfo.ACTION_CLICK:
                    openDay(index);
                    return true;
                default:
                    return action == AccessibilityNodeInfo.AccessibilityAction.ACTION_SHOW_ON_SCREEN.getId()
                        && requestRectangleOnScreen(dayBounds(index));
            }
        }

        private String label(int index) {
            LocalDate day = start.plusDays(index);
            return day.format(SPOKEN_DATE) + "、" + dayStatus.apply(day);
        }

        private void send(int index, int type) {
            AccessibilityManager manager = getContext().getSystemService(AccessibilityManager.class);
            if (!isDay(index) || dayStatus == null || getParent() == null || manager == null || !manager.isEnabled()) return;
            AccessibilityEvent event = AccessibilityEvent.obtain(type);
            event.setSource(GrassView.this, index);
            event.setContentDescription(label(index));
            event.setClassName(Button.class.getName());
            event.setPackageName(getContext().getPackageName());
            event.setEnabled(true);
            getParent().requestSendAccessibilityEvent(GrassView.this, event);
        }

        private boolean hover(int index) {
            int previous = hovered;
            hovered = index;
            if (previous != index) {
                send(index, AccessibilityEvent.TYPE_VIEW_HOVER_ENTER);
                send(previous, AccessibilityEvent.TYPE_VIEW_HOVER_EXIT);
            }
            return isDay(index) || isDay(previous);
        }
    }

    static final class WeekLabels extends View {
        final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);
        public WeekLabels(Context c){super(c);setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);}
        @Override protected void onDraw(Canvas c){p.setColor(Ui.ink(getContext(),Ui.MUTED));p.setTypeface(Typeface.DEFAULT_BOLD);p.setTextSize(Ui.dp(getContext(),12));for(int row=0;row<7;row++)c.drawText(WEEKDAYS[row],Ui.dp(getContext(),2),Ui.dp(getContext(),HEADER+row*STEP+15),p);}
    }
}
