package app.routia;

import android.content.Context;
import android.graphics.*;
import android.view.View;
import org.json.*;
import java.time.*;
import java.util.*;

/** Native charts draw only recorded measurements and counts; missing measurements are gaps. */
final class ReportVisuals extends View {
    static final int SERIES=0,WEEKDAY=1,ARROW=2;
    final int kind;final JSONObject facts;final ReportChartData series;
    final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);final Path triangle=new Path();
    ReportVisuals(Context c){this(c,new JSONObject(),SERIES);}
    ReportVisuals(Context c,JSONObject data,int type){
        super(c);facts=data;kind=type;series=chartData(data);setLayerType(View.LAYER_TYPE_SOFTWARE,null);
        String description=type==ARROW?"提案する行動から、試したあとの確認へ":type==WEEKDAY?"曜日ごとの達成日 / 対象日。対象日の達成済み記録も含みます。":series==null?"記録を表示できません":series.numeric?"実施量の推移。未記録をゼロとして描きません。":"累計達成日数の推移。チケットは加算しません。";
        if(type==SERIES&&series!=null){
            StringBuilder details=new StringBuilder(description);
            for(ReportChartData.Day day:series.days){
                details.append(" ").append(shortDate(day.day())).append("：");
                Double value=series.value(day);
                if(series.numeric&&value==null)details.append(day.pending()?"未確定":day.achieved()?"量未記録":day.ticket()?"チケット":"未記録");
                else details.append(Ui.number(value)).append(series.numeric?series.unit:"日");
            }description=details.toString();
        }
        setContentDescription(description);setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_YES);
    }
    static ReportChartData chartData(JSONObject facts){
        try{
            LocalDate created=LocalDate.parse(facts.getString("created")),asOf=LocalDate.parse(facts.getString("as_of"));
            java.util.List<ReportChartData.Entry> entries=new ArrayList<>();JSONArray days=facts.optJSONArray("recent_days");
            if(days!=null)for(int i=0;i<days.length();i++){
                JSONObject day=days.optJSONObject(i);if(day==null)continue;
                try{
                    Double amount=day.has("amount")&&!day.isNull("amount")?Double.valueOf(day.getDouble("amount")):null;
                    entries.add(new ReportChartData.Entry(LocalDate.parse(day.getString("day")),day.optInt("level"),day.optBoolean("ticket"),amount,day.optString("unit")));
                }catch(JSONException|DateTimeException ignored){/* An unreadable daily value is not zero. */}
            }
            return new ReportChartData(created,asOf,facts.optInt("total"),facts.optString("kind"),facts.optString("unit"),entries);
        }catch(JSONException|DateTimeException|IllegalArgumentException e){return null;}
    }
    float d(float x){return x*getResources().getDisplayMetrics().density;}
    void text(Canvas canvas,String s,float x,float y,int color,int size,Paint.Align align){
        p.setStyle(Paint.Style.FILL);p.setColor(Ui.ink(getContext(),color));p.setTextAlign(align);p.setTextSize(size*getResources().getDisplayMetrics().scaledDensity);
        p.setTypeface(Typeface.create("sans-serif",Typeface.NORMAL));canvas.drawText(s,x,y,p);
    }
    @Override protected void onDraw(Canvas c){
        super.onDraw(c);
        if(kind==ARROW){
            p.setColor(Ui.ink(getContext(),Ui.GREEN));p.setStrokeWidth(d(2));float x=getWidth()/2f;c.drawLine(x,0,x,getHeight()-d(6),p);
            triangle.reset();triangle.moveTo(x-d(4),getHeight()-d(9));triangle.lineTo(x+d(4),getHeight()-d(9));triangle.lineTo(x,getHeight()-d(3));triangle.close();c.drawPath(triangle,p);return;
        }
        if(series==null){text(c,"記録を表示できません",getWidth()/2f,getHeight()/2f,Ui.MUTED,13,Paint.Align.CENTER);return;}
        if(kind==SERIES)history(c);else weekday(c);
    }
    static String shortDate(LocalDate date){return date.getMonthValue()+"/"+date.getDayOfMonth();}
    float x(int index,float left,float width){return series.days.size()==1?left+width/2:left+width*index/(series.days.size()-1);}
    void history(Canvas c){
        boolean empty=series.numeric?series.measured==0:series.achieved==0&&series.days.get(series.days.size()-1).cumulative()==0;
        if(empty){text(c,"表示できる実記録がまだありません",getWidth()/2f,getHeight()/2f,Ui.MUTED,13,Paint.Align.CENTER);return;}
        float left=d(38),right=getWidth()-d(14),top=d(26),base=getHeight()-d(35),width=Math.max(d(30),right-left),height=Math.max(d(30),base-top);
        double maximum=1;
        for(ReportChartData.Day day:series.days){Double value=series.value(day);if(value!=null)maximum=Math.max(maximum,value);}
        double scale=maximum<=5?Math.ceil(maximum):Math.ceil(maximum/5)*5;
        if(!series.numeric&&scale%2!=0)scale++;
        for(int i=0;i<3;i++){
            float y=base-height*i/2f;p.setColor(Ui.surface(getContext(),0xffDDE5DB));p.setStrokeWidth(d(1));c.drawLine(left,y,right,y,p);
            text(c,Ui.number(scale*i/2),left-d(8),y+d(4),Ui.MUTED,11,Paint.Align.RIGHT);
        }
        Double previous=null;float px=0,py=0;
        for(int i=0;i<series.days.size();i++){
            ReportChartData.Day day=series.days.get(i);Double value=series.value(day);float x=x(i,left,width);
            if(value==null){
                p.setColor(Ui.ink(getContext(),Ui.MUTED));p.setStrokeWidth(d(2));c.drawLine(x-d(2),base+d(9),x+d(2),base+d(9),p);previous=null;continue;
            }
            float y=base-height*(float)(value/scale);
            if(previous!=null){p.setColor(Ui.ink(getContext(),Ui.GREEN));p.setStrokeWidth(d(2.5f));c.drawLine(px,py,x,y,p);}
            p.setColor(Ui.ink(getContext(),day.achieved()?Ui.GREEN:Ui.MUTED));p.setStrokeWidth(d(1.5f));p.setStyle(day.achieved()?Paint.Style.FILL:Paint.Style.STROKE);
            c.drawCircle(x,y,d(3.5f),p);p.setStyle(Paint.Style.FILL);
            if(series.days.size()<=7||i==series.days.size()-1)text(c,Ui.number(value),x,Math.max(d(14),y-d(9)),Ui.INK,12,Paint.Align.CENTER);
            previous=value;px=x;py=y;
        }
        int size=series.days.size();int[] labels=size==1?new int[]{0}:size<=7?new int[]{0,size-1}:new int[]{0,(size-1)/2,size-1};
        for(int index:labels){
            Paint.Align align=size==1?Paint.Align.CENTER:index==0?Paint.Align.LEFT:index==size-1?Paint.Align.RIGHT:Paint.Align.CENTER;
            text(c,shortDate(series.days.get(index).day()),x(index,left,width),base+d(30),Ui.MUTED,11,align);
        }
    }
    void weekday(Canvas c){
        float col=getWidth()/7f,base=getHeight()-d(48),top=d(28),height=Math.max(d(30),base-top);
        p.setStrokeWidth(d(1));p.setColor(Ui.surface(getContext(),0xffDDE5DB));c.drawLine(0,base,getWidth(),base,p);
        String[] labels={"月","火","水","木","金","土","日"};
        for(int i=0;i<7;i++){
            int n=series.weekdayDone[i],den=series.weekdayEligible[i];float ratio=den==0?0:Math.min(1,n/(float)den),x=col*(i+.5f);
            p.setColor(Ui.surface(getContext(),Ui.PALE));c.drawRoundRect(x-d(11),top,x+d(11),base,d(5),d(5),p);
            if(ratio>0){p.setColor(Ui.ink(getContext(),Ui.GREEN));c.drawRoundRect(x-d(11),base-height*ratio,x+d(11),base,d(5),d(5),p);}
            text(c,den==0?"—":Math.round(ratio*100)+"%",x,d(17),Ui.GREEN,10,Paint.Align.CENTER);
            text(c,labels[i],x,base+d(19),Ui.INK,12,Paint.Align.CENTER);text(c,n+"/"+den,x,base+d(37),Ui.MUTED,10,Paint.Align.CENTER);
        }
    }
}
