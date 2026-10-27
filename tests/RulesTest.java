import app.routia.Rules;
import java.time.*;
public class RulesTest {
    private static int count=0;
    private static void check(boolean condition,String message){count++;if(!condition)throw new AssertionError(message);}
    public static void main(String[] args){

        LocalDate today=LocalDate.of(2026,10,24);
        check(!Rules.recovery(today,today,0),"Creation day must not be recovery");
        check(Rules.recovery(today.minusDays(1),today,0),"Missing yesterday permits recovery");
        check(!Rules.recovery(today.minusDays(1),today,1),"Minimum counts as a successful yesterday");
        check(!Rules.recovery(today.minusDays(1),today,2),"Full counts as a successful yesterday");
        check(Rules.merge(0,1)==1,"Minimum saved");
        check(Rules.merge(1,2)==2,"Minimum can be upgraded");
        check(Rules.merge(2,1)==2,"A stale minimum action cannot downgrade full");
        check(Rules.merge(2,2)==2,"Repeated action is idempotent");
        check(!Rules.validActionDate("2026-10-23",today),"Old notifications are rejected");
        check(!Rules.validActionDate(null,today),"Missing date is rejected");
        check(Rules.validActionDate("2026-10-24",today),"Today's action accepted");
        ZonedDateTime now=ZonedDateTime.of(2026,10,24,20,0,0,0,ZoneId.of("Asia/Tokyo"));
        check(Rules.nextReminder(now,21,0).getDayOfMonth()==24,"Future reminder is today");
        check(Rules.nextReminder(now,19,0).getDayOfMonth()==25,"Passed reminder is tomorrow");
        check(Rules.nextReminder(now,20,0).getDayOfMonth()==25,"Exact current minute schedules next day");
        ZonedDateTime dst=ZonedDateTime.of(2026,3,8,0,0,0,0,ZoneId.of("America/New_York"));
        check(Rules.nextReminder(dst,2,30).getHour()==3,"Nonexistent DST hour resolves forward");
        ZonedDateTime beforeDst=ZonedDateTime.of(2026,3,7,22,0,0,0,ZoneId.of("America/New_York"));
        check(Rules.nextReminder(beforeDst,21,0).getHour()==21,"Daily time remains local across DST");
        try { Rules.merge(1,0);throw new AssertionError("Invalid achievement accepted"); }catch(IllegalArgumentException expected){count++;}
        check(Rules.merge(2,3)==3,"Upgrade to extra");check(Rules.merge(3,2)==3,"Full cannot downgrade extra");check(Rules.merge(3,1)==3,"Minimum cannot downgrade extra");
        java.util.Map<LocalDate,Integer> records=new java.util.HashMap<>();
        LocalDate mon=LocalDate.of(2026,10,19);
        check(Rules.penaltyDays(mon,mon,records).isEmpty(),"Today is never a missed day");
        check(Rules.penaltyDays(mon,mon.plusDays(1),records).isEmpty(),"One missed day is free");
        check(Rules.penaltyDays(mon,mon.plusDays(2),records).equals(java.util.List.of(mon.plusDays(1))),"Two concluded missing days trigger once");
        check(Rules.penaltyDays(mon,mon.plusDays(7),records).size()==1,"One pledge per habit week");
        records.put(mon.plusDays(1),1);
        check(Rules.penaltyDays(mon,mon.plusDays(3),records).equals(java.util.List.of(mon.plusDays(2))),"Two nonconsecutive misses also trigger");
        records.put(mon,2);records.put(mon.plusDays(2),2);
        check(Rules.penaltyDays(mon,mon.plusDays(3),records).isEmpty(),"Minimum and full both avoid penalty");
        records.clear();records.put(mon.plusDays(5),2);
        check(Rules.penaltyDays(mon.plusDays(6),mon.plusDays(8),records).equals(java.util.List.of(mon.plusDays(7))),"Consecutive misses across week boundary");
        check(Rules.week(mon.plusDays(6)).equals(mon),"Week ends Sunday");
        check(Rules.week(mon.plusDays(7)).equals(mon.plusDays(7)),"New week Monday");
        check(Rules.penaltyDays(mon.plusDays(5),mon.plusDays(5),records).isEmpty(),"No retroactive misses before opt-in");
        check(Rules.amountLevel(0,15)==0,"Zero numeric input cannot complete a day");
        check(Rules.amountLevel(.05,0)==2,"Fractional effort counts without a target");
        check(Rules.amountLevel(5,15)==1,"Positive effort below a target still counts");
        check(Rules.amountLevel(15,15)==2&&Rules.amountLevel(16,15)==2,"Target and above need no extra selection");
        for(double invalid:new double[]{-1,Double.NaN,Double.POSITIVE_INFINITY,1000001})try{Rules.amountLevel(invalid,1);throw new AssertionError("Invalid amount");}catch(IllegalArgumentException expected){count++;}

        check(!Rules.celebrationEligible("check",0,1,null),"Mini achievement cannot show celebration");
        check(Rules.celebrationEligible("check",0,2,null)&&Rules.celebrationEligible("check",0,3,null),"Goal and challenge may celebrate");
        check(!Rules.celebrationEligible("time",0,2,10.0),"No target means no numeric celebration");
        check(!Rules.celebrationEligible("count",15,1,14.0),"Below target never celebrates");
        check(Rules.shouldCelebrate("time",15,1,14.0,2,15.0),"First target crossing celebrates");
        check(!Rules.shouldCelebrate("time",15,2,15.0,2,20.0),"Above-target edits cannot replay automatically");
        check(!Rules.shouldCelebrate("count",15,0,null,1,3.0),"First small numeric record has no scene");
        check(!Rules.shouldCelebrate("check",0,2,null,2,null),"Repeated goal tap has no scene");
        check(Rules.shouldCelebrate("check",0,2,null,3,null),"New challenge can celebrate");
        check(Rules.summary(12000,15000).equals("🔥 連続12000日 / ✅ 累計15000日"),"Consistent readable long-running summary");
        System.out.println("PASS "+count+" domain assertions");
    }
}
