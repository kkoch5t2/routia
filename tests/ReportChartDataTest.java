import app.routia.ReportChartData;
import app.routia.ReportChartData.Entry;
import java.time.*;
import java.util.*;

public final class ReportChartDataTest {
    static int n;
    static void check(boolean value,String label){n++;if(!value)throw new AssertionError(label);}
    static Entry day(LocalDate date,int level,Double amount,String unit){return new Entry(date,level,false,amount,unit);}
    public static void main(String[] args){
        LocalDate sunday=LocalDate.of(2026,10,25),saturday=sunday.minusDays(1);
        ReportChartData both=new ReportChartData(saturday,sunday,2,"time","分",Arrays.asList(day(saturday,2,20d,"分"),day(sunday,2,30d,"分")));
        check(both.days.size()==2&&both.days.get(1).day().equals(sunday),"completed as-of day included");
        check(both.weekdayDone[6]==1&&both.weekdayEligible[6]==1,"Sunday completion reflected in weekday numerator and denominator");
        check(both.weekdayDone[5]==1&&both.weekdayEligible[5]==1,"Saturday remains visible");
        check(both.measured==2&&both.amountTotal==50&&both.value(both.days.get(1))==30,"actual daily quantity and sum include Sunday");
        check(!both.days.get(1).pending(),"completed today not pending");
        ReportChartData pending=new ReportChartData(saturday,sunday,1,"time","分",Collections.singletonList(day(saturday,1,20d,"分")));
        check(pending.days.size()==2&&pending.days.get(1).pending(),"unfinished as-of day marked pending");
        check(pending.weekdayEligible[6]==0&&pending.weekdayDone[6]==0,"unfinished today not counted as failure");
        check(pending.value(pending.days.get(1))==null&&pending.amountTotal==20,"missing quantity remains absent rather than zero");
        ReportChartData legacy=new ReportChartData(saturday,sunday,2,"count","km",Arrays.asList(day(saturday,1,null,"km"),day(sunday,2,3d,"km")));
        check(legacy.achieved==2&&legacy.measured==1&&legacy.amountTotal==3,"legacy amountless achievement does not invent measurement");
        ReportChartData units=new ReportChartData(saturday,sunday,2,"count","km",Arrays.asList(day(saturday,1,1000d,"m"),day(sunday,2,3d,"km")));
        check(units.measured==1&&units.amountTotal==3,"different units are not added together");
        ReportChartData checkOnly=new ReportChartData(saturday,sunday,12,"check","",Arrays.asList(day(saturday,1,null,""),day(sunday,2,null,"")));
        check(!checkOnly.numeric&&checkOnly.value(checkOnly.days.get(0))==11&&checkOnly.value(checkOnly.days.get(1))==12,"check habit shows actual lifetime accumulation");
        ReportChartData ticket=new ReportChartData(saturday,sunday,1,"check","",Arrays.asList(day(saturday,1,null,""),new Entry(sunday,0,true,null,"")));
        check(ticket.days.get(1).ticket()&&ticket.value(ticket.days.get(1))==1,"ticket does not increase cumulative days");
        check(ticket.weekdayDone[6]==0&&ticket.weekdayEligible[6]==1,"ticket distinguished from achievement");
        ReportChartData first=new ReportChartData(sunday,sunday,1,"time","分",Collections.singletonList(day(sunday,2,30d,"分")));
        check(first.days.size()==1&&first.start.equals(first.end)&&first.measured==1,"one-day history has one correct date and measurement");
        ReportChartData empty=new ReportChartData(sunday,sunday,0,"check","",Collections.emptyList());
        check(empty.days.size()==1&&empty.achieved==0&&Arrays.stream(empty.weekdayEligible).sum()==0,"new empty habit invents no concluded days");
        ReportChartData window=new ReportChartData(sunday.minusDays(90),sunday,2,"time","分",Arrays.asList(day(sunday.minusDays(28),2,99d,"分"),day(sunday,2,30d,"分"),day(sunday.plusDays(1),2,55d,"分")));
        check(window.days.size()==28&&window.start.equals(sunday.minusDays(27)),"bounded inclusive 28-day window");
        check(window.measured==1&&window.amountTotal==30,"outside and future measurements excluded");
        ReportChartData invalid=new ReportChartData(saturday,sunday,2,"count","回",Arrays.asList(day(saturday,1,Double.NaN,"回"),day(sunday,2,-2d,"回")));
        check(invalid.measured==0&&invalid.value(invalid.days.get(0))==null,"invalid numeric values not plotted");
        ReportChartData nextDay=new ReportChartData(saturday,sunday.plusDays(1),2,"time","分",Arrays.asList(day(saturday,2,20d,"分"),day(sunday,2,30d,"分")));
        check(nextDay.weekdayDone[6]==1&&nextDay.measured==2&&nextDay.days.get(2).pending(),"Sunday remains visible after midnight");
        System.out.println("PASS "+n+" inclusive-report chart assertions");
    }
}
