package app.routia;

import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;

/** Conservative product rules for optional suggestions, not scientific or medical thresholds. */
public final class ReportProgression {
    public record Day(LocalDate day,int level,Double amount,String unit) {}
    public record Decision(boolean eligible,int milestone,int full28,int full14,int full7,Double proposedTarget,String reason) {}
    public static Decision assess(String kind,String unit,double target,LocalDate created,LocalDate asOf,int total,
        List<Day> entries,int previousMilestone,LocalDate previousOffer){
        int milestone=(Math.max(0,total)/30)*30;Set<LocalDate> full=new HashSet<>();
        for(Day e:entries){
            if(e.day()==null||e.day().isBefore(created)||e.day().isBefore(asOf.minusDays(28))||!e.day().isBefore(asOf)||e.level()<2)continue;
            if(!kind.equals("check")&&(!(target>0)||!Double.isFinite(target)||e.amount()==null||!Double.isFinite(e.amount())||!unit.equals(e.unit())||e.amount()<target))continue;
            full.add(e.day());
        }
        int f28=full.size(),f14=0,f7=0;
        for(LocalDate day:full){if(!day.isBefore(asOf.minusDays(14)))f14++;if(!day.isBefore(asOf.minusDays(7)))f7++;}
        String reason="";
        if(ChronoUnit.DAYS.between(created,asOf)<28||milestone<30)reason="まずは継続と再開を優先";
        else if(total-milestone>6)reason="節目の近くではないため現状維持";
        else if(f28<24||f14<12||f7<7)reason="未達やミニ達成、量の不明な日があり、増量より安定を優先";
        else if(previousMilestone>=milestone||(previousOffer!=null&&asOf.isBefore(previousOffer.plusDays(30))))reason="同じ節目や直近の提案からは増量を重ねない";
        Double proposed=null;
        if(reason.isEmpty()&&!kind.equals("check")){
            double candidate=Math.floor(target*1.05+1e-9);
            if(!(candidate>target)||!Double.isFinite(candidate)||candidate>target*1.05+1e-7)reason="5%以内で無理なく増やせる量を確定できないため現状維持";
            else proposed=candidate;
        }
        return new Decision(reason.isEmpty(),milestone,f28,f14,f7,proposed,reason.isEmpty()?"任意の小さな挑戦を相談できる節目":reason);
    }
    private ReportProgression(){}
}
