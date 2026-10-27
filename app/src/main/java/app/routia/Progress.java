package app.routia;
import java.time.*;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
/** Calendar and contribution rules, shared by the UI and deterministic tests. */
public final class Progress {
    public record Range(LocalDate start,LocalDate end){}
    public static Range range(int period,LocalDate anchor,LocalDate created,LocalDate today){
        LocalDate start=period==0?anchor.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)):period==1?anchor.withDayOfMonth(1):period==2?anchor.withDayOfYear(1):created;
        LocalDate end=period==0?start.plusDays(6):period==1?start.plusMonths(1).minusDays(1):period==2?start.plusYears(1).minusDays(1):today;
        return new Range(start,end.isAfter(today)?today:end);
    }
    public static Range calendarBounds(int period,Range selected){LocalDate start=selected.start(),end=selected.end();return new Range(start,period==0?start.plusDays(6):period==1?start.plusMonths(1).minusDays(1):period==2?start.plusYears(1).minusDays(1):end);}
    public static int contribution(boolean check,boolean achieved,Double amount,double scale,int level){return check?(achieved?level==3?4:level==2?2:1:0):shade(achieved,amount,scale,level);}
    public static int streak(Set<LocalDate> days,LocalDate today){return streak(days,Set.of(),today);}
    public static int streak(Set<LocalDate> days,Set<LocalDate> tickets,LocalDate today){LocalDate d=days.contains(today)||tickets.contains(today)?today:today.minusDays(1);int n=0;while(days.contains(d)||tickets.contains(d)){if(days.contains(d))n++;d=d.minusDays(1);}return n;}
    public static int best(Set<LocalDate> days){return best(days,Set.of());}
    public static int best(Set<LocalDate> days,Set<LocalDate> tickets){TreeSet<LocalDate> covered=new TreeSet<>(days);covered.addAll(tickets);int best=0,n=0;LocalDate prior=null;for(LocalDate d:covered){if(prior==null||!prior.plusDays(1).equals(d))n=0;if(days.contains(d))n++;best=Math.max(best,n);prior=d;}return best;}
    public static int shade(boolean achieved,Double amount,double scale,int level){if(!achieved)return 0;if(amount==null)return level==3?4:level==2?2:1;if(amount<=0)return 1;return Math.min(4,Math.max(1,(int)Math.ceil(amount/Math.max(1,scale)*4)));}
    private Progress(){}
}
