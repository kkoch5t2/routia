package app.routia;

import java.time.*;
import java.util.*;

/** Display-window facts include a recorded as-of day without treating an unfinished day as a miss. */
public final class ReportChartData {
    public record Entry(LocalDate day,int level,boolean ticket,Double amount,String unit) {}
    public record Day(LocalDate day,boolean achieved,boolean ticket,boolean pending,Double amount,int cumulative) {}
    public final LocalDate start,end;
    public final boolean numeric;
    public final String unit;
    public final List<Day> days;
    public final int[] weekdayDone=new int[7],weekdayEligible=new int[7];
    public final int achieved,measured;
    public final double amountTotal;
    public ReportChartData(LocalDate created,LocalDate asOf,int total,String kind,String unit,Collection<Entry> entries){
        if(created==null||asOf==null||created.isAfter(asOf))throw new IllegalArgumentException("Invalid display dates");
        end=asOf;start=created.isAfter(asOf.minusDays(27))?created:asOf.minusDays(27);
        numeric="time".equals(kind)||"count".equals(kind);this.unit=unit==null?"":unit;
        Map<LocalDate,Entry> map=new HashMap<>();
        for(Entry e:entries)if(e!=null&&e.day()!=null&&!e.day().isBefore(start)&&!e.day().isAfter(end))map.put(e.day(),e);
        int successes=0;for(Entry e:map.values())if(e.level()>0)successes++;
        int cumulative=Math.max(0,total-successes),known=0;double sum=0;
        List<Day> output=new ArrayList<>();
        for(LocalDate date=start;!date.isAfter(end);date=date.plusDays(1)){
            Entry e=map.get(date);boolean done=e!=null&&e.level()>0, ticket=!done&&e!=null&&e.ticket();
            boolean pending=date.equals(end)&&!done&&!ticket;int weekday=date.getDayOfWeek().getValue()-1;
            if(!pending){weekdayEligible[weekday]++;if(done)weekdayDone[weekday]++;}
            if(done)cumulative++;
            Double amount=null;
            if(numeric&&done&&e.amount()!=null&&Double.isFinite(e.amount())&&e.amount()>=0&&this.unit.equals(e.unit())){
                amount=e.amount();sum+=amount;known++;
            }
            output.add(new Day(date,done,ticket,pending,amount,cumulative));
        }
        days=Collections.unmodifiableList(output);achieved=successes;measured=known;amountTotal=sum;
    }
    public Double value(Day day){if(numeric)return day.amount();return Double.valueOf(day.cumulative());}
}
