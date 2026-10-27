import app.routia.ReportProgression;
import java.time.*;
import java.util.*;

public final class ReportProgressionTest{
    static int n;static void check(boolean ok,String label){n++;if(!ok)throw new AssertionError(label);}
    public static void main(String[] args){
        LocalDate now=LocalDate.of(2026,10,26);List<ReportProgression.Day> days=new ArrayList<>();
        for(int i=1;i<=28;i++)days.add(new ReportProgression.Day(now.minusDays(i),2,30.0,"分"));
        var mature=ReportProgression.assess("time","分",30,now.minusDays(40),now,30,days,0,null);
        check(mature.eligible()&&mature.proposedTarget()==31&&mature.full28()==28,"stable milestone allows one small optional increase");
        check(!ReportProgression.assess("time","分",30,now.minusDays(40),now,29,days,0,null).eligible(),"not before first milestone");
        check(!ReportProgression.assess("time","分",30,now.minusDays(20),now,30,days,0,null).eligible(),"recent habit cannot qualify");
        check(!ReportProgression.assess("time","分",30,now.minusDays(40),now,37,days,0,null).eligible(),"do not repeatedly suggest far beyond milestone");
        check(!ReportProgression.assess("time","分",30,now.minusDays(80),now,60,days,30,now.minusDays(20)).eligible(),"wait between suggestions");
        check(!ReportProgression.assess("time","分",30,now.minusDays(40),now,30,days,30,null).eligible(),"same milestone is not repeated");
        List<ReportProgression.Day> gap=new ArrayList<>(days);gap.remove(0);
        check(!ReportProgression.assess("time","分",30,now.minusDays(40),now,30,gap,0,null).eligible(),"recent missed day blocks increase");
        List<ReportProgression.Day> mini=new ArrayList<>();for(int i=1;i<=28;i++)mini.add(new ReportProgression.Day(now.minusDays(i),1,30.0,"分"));
        check(!ReportProgression.assess("time","分",30,now.minusDays(40),now,30,mini,0,null).eligible(),"minimum success alone does not prove spare capacity");
        List<ReportProgression.Day> unknown=new ArrayList<>();for(int i=1;i<=28;i++)unknown.add(new ReportProgression.Day(now.minusDays(i),2,null,"分"));
        check(!ReportProgression.assess("time","分",30,now.minusDays(40),now,30,unknown,0,null).eligible(),"unknown amounts do not qualify");
        check(!ReportProgression.assess("time","回",30,now.minusDays(40),now,30,days,0,null).eligible(),"mismatched units do not qualify");
        var checkHabit=ReportProgression.assess("check","",0,now.minusDays(40),now,30,days,0,null);
        check(checkHabit.eligible()&&checkHabit.proposedTarget()==null,"qualitative habit never gets an invented numeric target");
        List<ReportProgression.Day> small=new ArrayList<>();for(int i=1;i<=28;i++)small.add(new ReportProgression.Day(now.minusDays(i),2,3.0,"回"));
        check(!ReportProgression.assess("count","回",3,now.minusDays(40),now,30,small,0,null).eligible(),"indivisible increase above 5 percent is withheld");
        for(int i=1;i<=28;i++)days.add(new ReportProgression.Day(now.plusDays(i),2,500.0,"分"));
        check(ReportProgression.assess("time","分",30,now.minusDays(40),now,30,days,0,null).full28()==28,"future records are excluded");
        days.addAll(new ArrayList<>(days));
        check(ReportProgression.assess("time","分",30,now.minusDays(40),now,30,days,0,null).full28()==28,"duplicate days cannot inflate stability");
        check(!ReportProgression.assess("time","分",Double.NaN,now.minusDays(40),now,30,days,0,null).eligible(),"invalid target is not a basis for increase");
        System.out.println("PASS "+n+" conservative progression assertions");
    }
}
