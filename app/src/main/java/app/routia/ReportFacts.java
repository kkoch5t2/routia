package app.routia;

import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;

/** Recorded facts only. An absent daily record is not proof of inactivity. */
public final class ReportFacts {
    public record Run(LocalDate first, LocalDate last, int achieved, boolean ended) {}
    public final LocalDate created, today, recentStart, closedEnd;
    public final Set<LocalDate> done, tickets;
    public final int total, streak, best, recentDone, recentEligible;
    public final int[] weekdayDone = new int[7], weekdayEligible = new int[7];
    public final List<Run> runs;

    public ReportFacts(LocalDate created, LocalDate today, Set<LocalDate> achievements, Set<LocalDate> ticketDays) {
        if (created == null || today == null || created.isAfter(today)) throw new IllegalArgumentException("日付を確認してください");
        this.created=created; this.today=today; closedEnd=today.minusDays(1);
        recentStart=created.isAfter(today.minusDays(28))?created:today.minusDays(28);
        TreeSet<LocalDate> actual=new TreeSet<>(), coveredTickets=new TreeSet<>();
        for(LocalDate d:achievements) if(!d.isBefore(created)&&!d.isAfter(today)) actual.add(d);
        for(LocalDate d:ticketDays) if(!d.isBefore(created)&&d.isBefore(today)&&!actual.contains(d)) coveredTickets.add(d);
        done=Collections.unmodifiableSet(actual); tickets=Collections.unmodifiableSet(coveredTickets);
        total=actual.size(); streak=Progress.streak(done,tickets,today); best=Progress.best(done,tickets);
        int count=0, eligible=0;
        for(LocalDate d=recentStart;d.isBefore(today);d=d.plusDays(1)){
            int w=d.getDayOfWeek().getValue()-1; weekdayEligible[w]++; eligible++;
            if(done.contains(d)){weekdayDone[w]++;count++;}
        }
        recentDone=count; recentEligible=eligible;
        List<Run> all=new ArrayList<>();
        LocalDate first=null,last=null; int n=0;
        for(LocalDate d=created;!d.isAfter(today);d=d.plusDays(1)){
            if(done.contains(d)){if(first==null)first=d;last=d;n++;}
            else if(!tickets.contains(d)&&d.isBefore(today)&&first!=null){
                all.add(new Run(first,last,n,true));first=null;last=null;n=0;
            }
        }
        if(first!=null)all.add(new Run(first,last,n,false));
        runs=Collections.unmodifiableList(all);
    }
    public List<Run> lastRuns(int maximum) {
        return runs.subList(Math.max(0,runs.size()-maximum),runs.size());
    }
    public int rate(){return recentEligible==0?0:(int)Math.round(recentDone*100d/recentEligible);}
    public int dayState(LocalDate d){return d.isBefore(created)||d.isAfter(closedEnd)?-1:done.contains(d)?1:tickets.contains(d)?2:0;}
    public long daysSinceStart(){return ChronoUnit.DAYS.between(created,today)+1;}
}
