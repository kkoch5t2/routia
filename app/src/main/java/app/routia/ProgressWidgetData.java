package app.routia;
import java.time.*;
import java.util.*;
/** Monthly widget totals share achievement and contribution rules with the results screen. */
public final class ProgressWidgetData {
    public record Point(LocalDate day,int level,Double amount,String unit){}
    public record WeekDay(LocalDate day,int level,Double amount,boolean available,boolean ticket){}
    public record Report(int monthDays,int totalDays,int streak,double monthAmount,int monthAmounts,double totalAmount,int totalAmounts,int[] shades,List<WeekDay> week){}
    public static Report build(List<Point> points,Set<LocalDate> tickets,LocalDate created,LocalDate today,String kind,String unit,double target){
        LocalDate start=today.withDayOfMonth(1);Map<LocalDate,Point> records=new HashMap<>();Set<LocalDate> protectedDays=new HashSet<>();
        for(Point p:points)if(!p.day().isBefore(created)&&!p.day().isAfter(today)&&p.level()>0&&p.level()<=3)records.put(p.day(),p);
        for(LocalDate day:tickets)if(!day.isBefore(created)&&day.isBefore(today))protectedDays.add(day);
        int monthDays=0,monthAmounts=0,totalAmounts=0;double monthAmount=0,totalAmount=0,scale=Math.max(1,target);
        for(Point p:records.values()){boolean month=!p.day().isBefore(start);if(month)monthDays++;if(p.amount()!=null&&unit.equals(p.unit())&&Double.isFinite(p.amount())&&p.amount()>=0){totalAmounts++;totalAmount+=p.amount();if(month){monthAmounts++;monthAmount+=p.amount();scale=Math.max(scale,p.amount());}}}
        int[] shades=new int[today.lengthOfMonth()];for(int at=0;at<shades.length;at++){LocalDate day=start.plusDays(at);if(day.isBefore(created)||day.isAfter(today)){shades[at]=-1;continue;}Point p=records.get(day);Double amount=p!=null&&unit.equals(p.unit())&&p.amount()!=null&&Double.isFinite(p.amount())&&p.amount()>=0?p.amount():null;shades[at]=p==null&&protectedDays.contains(day)?5:Progress.contribution(kind.equals("check"),p!=null,amount,scale,p==null?0:p.level());}
        List<WeekDay> week=new ArrayList<>();LocalDate monday=today.minusDays(today.getDayOfWeek().getValue()-1);
        for(int i=0;i<7;i++){LocalDate day=monday.plusDays(i);Point p=records.get(day);boolean available=!day.isBefore(created)&&!day.isAfter(today);Double amount=p!=null&&unit.equals(p.unit())&&p.amount()!=null&&Double.isFinite(p.amount())&&p.amount()>=0?p.amount():null;week.add(new WeekDay(day,p==null?0:p.level(),amount,available,p==null&&protectedDays.contains(day)));}
        return new Report(monthDays,records.size(),Progress.streak(records.keySet(),protectedDays,today),monthAmount,monthAmounts,totalAmount,totalAmounts,shades,List.copyOf(week));
    }
    public static String summary(Report report){return Rules.summary(report.streak(),report.totalDays());}
    private ProgressWidgetData(){}
}
