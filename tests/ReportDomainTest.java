import app.routia.*;
import java.time.*;
import java.util.*;
import java.math.*;

public final class ReportDomainTest {
    static int checks;
    static void check(boolean value,String label){checks++;if(!value)throw new AssertionError(label);}
    public static void main(String[] args){
        LocalDate today=LocalDate.of(2026,10,25),start=today.minusDays(12);
        Set<LocalDate> done=new HashSet<>(Arrays.asList(start,start.plusDays(1),start.plusDays(3),today.minusDays(2),today.minusDays(1),today));
        Set<LocalDate> tickets=Set.of(start.plusDays(2));
        ReportFacts f=new ReportFacts(start,today,done,tickets);
        check(f.total==6,"actual days include today");
        check(f.recentEligible==12&&f.recentDone==5,"today excluded from concluded rate");
        check(f.streak==3&&f.best==3,"ticket bridges without adding achieved day");
        check(f.runs.size()==2&&f.runs.get(0).achieved()==3&&f.runs.get(0).ended(),"bridged ended run");
        check(!f.runs.get(1).ended(),"current run not declared stopped");
        check(f.dayState(today)==-1,"today not shown as a missed day");
        check(f.dayState(start.plusDays(2))==2,"ticket distinguished from achievement");
        check(Arrays.stream(f.weekdayDone).sum()==5&&Arrays.stream(f.weekdayEligible).sum()==12,"weekday denominators exclude today");
        check(f.lastRuns(1).size()==1,"short timeline");
        ReportFacts empty=new ReportFacts(today,today,Set.of(),Set.of());
        check(empty.rate()==0&&empty.runs.isEmpty()&&empty.recentEligible==0,"new habit no invented history");
        Set<LocalDate> polluted=Set.of(start.minusDays(1),today.plusDays(1),today);
        check(new ReportFacts(start,today,polluted,Set.of()).total==1,"out of bounds excluded");
        for(int input=1;input<=ReportBudget.INPUT_LIMIT;input+=37){
            ReportBudget.Reservation reserve=ReportBudget.reserve(input,today);
            check(reserve.maxOutput()>=6000&&reserve.maxOutput()<=ReportBudget.DESIRED_OUTPUT,"output allowance");
            check(reserve.reservedYen().compareTo(new BigDecimal("300"))<=0,"reserve never above cap");
            BigDecimal actual=ReportBudget.estimate(input,reserve.maxOutput(),0);
            check(actual.compareTo(reserve.reservedYen())<=0,"actual cost within reservation");
        }
        try{ReportBudget.reserve(16001,today);throw new AssertionError("unbounded input");}catch(IllegalArgumentException good){checks++;}
        try{ReportBudget.reserve(100,today.plusDays(31));throw new AssertionError("stale price");}catch(IllegalArgumentException good){checks++;}
        check(ReportBudget.estimate(1000,100,1000).compareTo(ReportBudget.ceiling(1000,100))<=0,"cache writes counted once");
        check(ReportBudget.MODEL.equals("gpt-6.1-sol"),"requested model contract");
        check(ReportBudget.estimate(1000,1000,0).compareTo(new BigDecimal("3.12"))==0,"Sol uncached token prices and FX margin");
        check(ReportBudget.estimate(1000,1000,1000).compareTo(new BigDecimal("3.90"))==0,"Sol highest cache-write price counted once");
        check(ReportBudget.reserve(16000,today).maxOutput()==20000,"Sol preserves full output allowance");
        check(ReportBudget.reserve(16000,today).reservedYen().compareTo(new BigDecimal("72.80"))==0,"Sol worst reservation below 300 yen");
        for(ReportBudget.Model m:ReportBudget.MODELS)for(int limit=100;limit<=1000;limit+=50)for(int input:new int[]{1,8000,16000}){
            ReportBudget.Options options=new ReportBudget.Options(m.id(),limit);
            try{
                ReportBudget.Reservation r=ReportBudget.reserve(options,input,today);
                check(r.reservedYen().compareTo(new BigDecimal(limit))<=0,"each selected model respects selected cap");
                check(r.maxOutput()>=6000&&r.maxOutput()<=20000,"no extra output to spend budget");
                check(ReportBudget.estimate(m.id(),input,r.maxOutput(),input).compareTo(r.reservedYen())<=0,"cache writes fit per-model reserve");
            }catch(IllegalArgumentException e){
                check(ReportBudget.ceiling(m.id(),input,6000).compareTo(new BigDecimal(limit))>0,"only insufficient budget combinations rejected");
            }
        }
        for(int invalid:new int[]{0,99,101,1001,5000}){
            try{new ReportBudget.Options(ReportBudget.MODEL,invalid);throw new AssertionError("invalid cap");}catch(IllegalArgumentException e){checks++;}
        }
        try{new ReportBudget.Options("unknown-model",300);throw new AssertionError("unknown model");}catch(IllegalArgumentException e){checks++;}
        // Independently calculated scenarios, including internal reasoning and JSON output.
        String[][] low={{"0.30","0.37","0.56"},{"5.98","7.28","11.18"},{"29.90","36.40","55.90"}};
        String[][] high={{"1.04","1.17","1.50"},{"20.80","23.40","29.90"},{"104.00","117.00","149.50"}};
        String[][] labels={{"約0.3〜1.1円 / 回","約0.4〜1.2円 / 回","約0.6〜1.5円 / 回"},
            {"約6〜21円 / 回","約7〜24円 / 回","約11〜30円 / 回"},
            {"約30〜105円 / 回","約36〜120円 / 回","約56〜150円 / 回"}};
        String[] ids={"gpt-6-luna",ReportBudget.MODEL,"gpt-6-astra"};
        for(int model=0;model<ids.length;model++)for(ReportDetail depth:ReportDetail.values()){
            int k=depth.ordinal();ReportBudget.CostRange r=ReportBudget.referenceRange(ids[model],depth);
            check(r.low().compareTo(new BigDecimal(low[model][k]))==0,"scenario low uses input plus reasoning and visible output");
            check(r.high().compareTo(new BigDecimal(high[model][k]))==0,"scenario high uses same published input prices");
            check(r.label().equals(labels[model][k]),"readable currency range and rounding");
            ReportBudget.Options o=new ReportBudget.Options(ids[model],300,depth);
            ReportBudget.Reservation reservation=ReportBudget.reserve(o,8000,today);
            check(reservation.reservedYen().compareTo(new BigDecimal("300"))<=0,"guide does not change paid ceiling");
        }
        String[] overallLabels={"約0.3〜1.5円 / 回","約6〜30円 / 回","約30〜150円 / 回"};
        for(int model=0;model<ids.length;model++)check(ReportBudget.allDepthLabel(ids[model]).equals(overallLabels[model]),"model comparison combines all report depths");
        check(ReportBudget.referenceRange(ReportBudget.MODEL,ReportDetail.DETAILED).high().compareTo(ReportBudget.ceiling(ReportBudget.MODEL,16000,20000))<0,"scenario endpoint differs from maximum processing cost");
        try{ReportBudget.referenceRange("unknown-model",ReportDetail.STANDARD);throw new AssertionError("unknown guide model");}catch(IllegalArgumentException expected){checks++;}
        check(ReportBudget.ceiling("gpt-6-astra",16000,20000).compareTo(new BigDecimal("364.00"))==0,"Astra separate highest cache rate");
        System.out.println("PASS "+checks+" report facts and budget assertions");
    }
}
