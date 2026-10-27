package app.routia;
import java.time.*;
import java.util.*;
/** Automatically cover closed missed days; each habit gets two tickets per calendar month. */
public final class TicketRules {
    public static final int MONTHLY_LIMIT=2;
    public static List<LocalDate> plan(LocalDate start,LocalDate today,Set<LocalDate> achieved,Set<LocalDate> existing){
        List<LocalDate> result=new ArrayList<>();Map<YearMonth,Integer> used=new HashMap<>();
        for(LocalDate day:existing)used.merge(YearMonth.from(day),1,Integer::sum);
        LocalDate firstAchievement=achieved.stream().min(LocalDate::compareTo).orElse(null);if(firstAchievement==null)return result;
        LocalDate prior=start.minusDays(1);boolean covered=achieved.contains(prior)||existing.contains(prior);
        int streak=covered?Progress.streak(achieved,existing,prior):0;
        for(LocalDate day=start;day.isBefore(today);day=day.plusDays(1)){
            if(achieved.contains(day)){streak++;covered=true;continue;}
            if(existing.contains(day)){covered=true;continue;}
            YearMonth month=YearMonth.from(day);
            if(day.isAfter(firstAchievement)&&covered&&streak>0&&used.getOrDefault(month,0)<MONTHLY_LIMIT){result.add(day);used.merge(month,1,Integer::sum);covered=true;}
            else{streak=0;covered=false;}
        }
        return result;
    }
    private TicketRules(){}
}
