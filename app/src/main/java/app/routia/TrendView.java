package app.routia;
import android.content.*;
import android.graphics.*;
import android.view.*;
import java.time.*;
import java.util.*;
public class TrendView extends View {
    static final String[] GRADE_LABELS={"未記録","ミニ","目標","挑戦"};
    final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);final double[] values;final boolean[] present;final LocalDate start;final String unit;final double target;final String endLabel;final Path path=new Path();final DashPathEffect dash;
    TrendView(Context c,double[] v,boolean[] has,LocalDate start,String unit,double target){super(c);this.values=v;present=has;this.start=start;this.unit=unit;this.target=target;dash=new DashPathEffect(new float[]{Ui.dp(c,4),Ui.dp(c,4)},0);LocalDate end=start.plusDays(v.length-1);endLabel=end.getMonthValue()+"/"+end.getDayOfMonth();StringBuilder desc=new StringBuilder("日別の推移。単位 "+unit+"。");for(int i=0;i<v.length;i++)if(has[i])desc.append(start.plusDays(i)).append(" ").append(Ui.number(v[i])).append(unit).append("。 ");setContentDescription(desc);setFocusable(true);}
    public TrendView(Context c){this(c,new double[14],new boolean[14],LocalDate.now().minusDays(13),"",0);}
    float d(float n){return Ui.dp(getContext(),n);}
    @Override protected void onDraw(Canvas c){super.onDraw(c);boolean grades=unit.equals("達成グレード");float left=d(grades?56:40),top=d(20),right=getWidth()-d(12),bottom=getHeight()-d(34);double max=Math.max(1,target);for(double v:values)max=Math.max(max,v);max=grades?3:Math.ceil(max*1.15);
        p.setTypeface(Typeface.create("sans-serif",Typeface.NORMAL));p.setTextSize(d(10));p.setStrokeWidth(d(1));
        for(int i=0;i<=3;i++){float y=bottom-(bottom-top)*i/3;p.setColor(Ui.surface(getContext(),0xffDDE5DB));c.drawLine(left,y,right,y,p);p.setColor(Ui.ink(getContext(),Ui.MUTED));p.setTextAlign(Paint.Align.RIGHT);c.drawText(grades?GRADE_LABELS[i]:Ui.number(max*i/3),left-d(7),y+d(3),p);}
        if(target>0){float y=bottom-(float)(target/max)*(bottom-top);p.setColor(0xffC29446);p.setPathEffect(dash);c.drawLine(left,y,right,y,p);p.setPathEffect(null);}
        path.reset();boolean started=false;p.setColor(Ui.ink(getContext(),Ui.GREEN));p.setStrokeWidth(d(2.5f));
        for(int i=0;i<values.length;i++){float x=left+(right-left)*i/Math.max(1,values.length-1),y=bottom-(float)(values[i]/max)*(bottom-top);if(!present[i]){started=false;continue;}if(started)path.lineTo(x,y);else{path.moveTo(x,y);started=true;}c.drawCircle(x,y,d(3),p);}
        p.setStyle(Paint.Style.STROKE);c.drawPath(path,p);p.setStyle(Paint.Style.FILL);p.setColor(Ui.ink(getContext(),Ui.MUTED));p.setTextAlign(Paint.Align.LEFT);
        float reserved=values.length>1?p.measureText(endLabel)+d(10):0;
        for(int i=0;i<values.length;i+=7){LocalDate day=start.plusDays(i);String label=day.getMonthValue()+"/"+day.getDayOfMonth();float x=left+(right-left)*i/Math.max(1,values.length-1);if(values.length==1||x+p.measureText(label)<right-reserved)c.drawText(label,x,bottom+d(23),p);}
        if(values.length>1){p.setTextAlign(Paint.Align.RIGHT);c.drawText(endLabel,right,bottom+d(23),p);}
    }
}
