package app.routia;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZonedDateTime;

/** Pure domain rules, independent of Android and the storage layer. */
public final class Rules {
    public static final int NONE = 0, MINIMUM = 1, FULL = 2, EXTRA = 3;
    public static final int MAX_HABITS = 10;
    private Rules() {}
    public static boolean recovery(LocalDate created, LocalDate day, int yesterday) {
        return created.isBefore(day) && yesterday == NONE;
    }
    public static int merge(int existing, int incoming) {
        if (incoming != MINIMUM && incoming != FULL && incoming != EXTRA) throw new IllegalArgumentException("Invalid achievement");
        return Math.max(existing, incoming);
    }
    /** Any positive numeric effort is a successful day; a target is optional. */
    public static int amountLevel(double amount,double target){
        if(!Double.isFinite(amount)||amount<0||amount>1000000||!Double.isFinite(target)||target<0)throw new IllegalArgumentException("数値を確認してください");
        return amount==0?NONE:target>0&&amount<target?MINIMUM:FULL;
    }
    public static boolean validActionDate(String date, LocalDate today) {
        return today.toString().equals(date);
    }
    public static ZonedDateTime nextReminder(ZonedDateTime now, int hour, int minute) {
        LocalTime time = LocalTime.of(hour, minute);
        ZonedDateTime next = now.toLocalDate().atTime(time).atZone(now.getZone());
        if (!next.isAfter(now)) next = now.toLocalDate().plusDays(1).atTime(time).atZone(now.getZone());
        return next;
    }
    public static LocalDate week(LocalDate d) {return d.minusDays(d.getDayOfWeek().getValue()-1);}
    /** At most one pledge per habit / Monday-Sunday week; today is never a missed day. */
    public static java.util.List<LocalDate> penaltyDays(LocalDate enabled,LocalDate today,java.util.Map<LocalDate,Integer> entries) {
        java.util.List<LocalDate> hits=new java.util.ArrayList<>();
        java.util.Set<LocalDate> charged=new java.util.HashSet<>();
        LocalDate currentWeek=null;int missed=0;boolean previousMissed=false;
        for(LocalDate d=enabled;d.isBefore(today);d=d.plusDays(1)) {
            LocalDate w=week(d);if(!w.equals(currentWeek)){currentWeek=w;missed=0;}
            boolean miss=entries.getOrDefault(d,0)==0;
            if(miss)missed++;
            if(miss&&(previousMissed||missed>=2)&&charged.add(w))hits.add(d);
            previousMissed=miss;
        }return hits;
    }
    public static String summary(int streak,int total){return "🔥 連続"+streak+"日 / ✅ 累計"+total+"日";}
    public static boolean celebrationEligible(String kind,double target,int level,Double amount){
        return level>=FULL && ("check".equals(kind)||target>0&&amount!=null&&Double.isFinite(amount)&&amount>=target);
    }
    public static boolean shouldCelebrate(String kind,double target,int beforeLevel,Double beforeAmount,int level,Double amount){
        if(!celebrationEligible(kind,target,level,amount))return false;
        return "check".equals(kind)?level>beforeLevel:!celebrationEligible(kind,target,beforeLevel,beforeAmount);
    }
    public static String grade(int level){return level==EXTRA?"🚀 チャレンジ達成":level==FULL?"🏆 目標達成":level==MINIMUM?"🌱 ミニ達成":"未記録";}
    public static String feedback(boolean recovery, int level) {
        return recovery ? "おかえりなさい。リカバリー成功！" : level == EXTRA ? "チャレンジ達成！目標の先まで進みました。" : level == FULL ? "目標達成！今日の一歩を記録しました。" : "ミニ達成。小さな一歩を記録しました。";
    }
}
