package app.routia;

import java.math.*;
import java.time.LocalDate;
import java.util.*;

/** Prices are Standard, short context. No tools or automatic retries. */
public final class ReportBudget {
    public static final int LIMIT_YEN=300, MIN_YEN=100, MAX_YEN=1000, STEP_YEN=50, INPUT_LIMIT=16000, DESIRED_OUTPUT=20000;
    public static final String MODEL="gpt-6.1-sol", PRICE_CHECKED="2026-10-25";
    public static final BigDecimal YEN_PER_USD=new BigDecimal("200"), MARGIN=new BigDecimal("1.30");
    private static final BigDecimal MILLION=new BigDecimal("1000000");
    public record Model(String id,String label,String detail,BigDecimal input,BigDecimal write,BigDecimal output) {}
    public static final List<Model> MODELS=Collections.unmodifiableList(Arrays.asList(
        new Model("gpt-6-luna","GPT-6 Luna","低価格 · 出費を小さく",new BigDecimal("0.1"),new BigDecimal("0.25"),new BigDecimal("0.5")),
        new Model(MODEL,"GPT-6.1 Sol","バランス · 初期設定",new BigDecimal("2"),new BigDecimal("5"),new BigDecimal("10")),
        new Model("gpt-6-astra","GPT-6 Astra","高価格 · 同じ利用量でも出費が大きい",new BigDecimal("10"),new BigDecimal("25"),new BigDecimal("50"))));
    public record Options(String model,int limitYen,ReportDetail detail) {
        public Options(String model,int limitYen){this(model,limitYen,ReportDetail.STANDARD);}
        public Options { Objects.requireNonNull(detail); ReportBudget.model(model);if(limitYen<MIN_YEN||limitYen>MAX_YEN||(limitYen-MIN_YEN)%STEP_YEN!=0)throw new IllegalArgumentException("上限は100〜1,000円、50円刻みで選んでください"); }
    }
    public static Model model(String id) {for(Model m:MODELS)if(m.id().equals(id))return m;throw new IllegalArgumentException("モデルの設定を確認してください");}
    public static Options defaults(){return new Options(MODEL,LIMIT_YEN);}
    public record Reservation(int maxOutput, BigDecimal reservedYen) {}
    public static Reservation reserve(int inputTokens,LocalDate now){return reserve(defaults(),inputTokens,now);}
    public static Reservation reserve(Options options,int inputTokens,LocalDate now) {
        if(now.isBefore(LocalDate.parse(PRICE_CHECKED))||now.isAfter(LocalDate.parse(PRICE_CHECKED).plusDays(30)))
            throw new IllegalArgumentException("料金情報の更新が必要です。更新するまで有料生成は停止します");
        if(inputTokens<1||inputTokens>INPUT_LIMIT)throw new IllegalArgumentException("記録が多いため、送信する期間を短くしてください");
        Model m=model(options.model());
        BigDecimal usd=new BigDecimal(options.limitYen()).divide(YEN_PER_USD.multiply(MARGIN),16,RoundingMode.DOWN);
        BigDecimal available=usd.subtract(m.write().multiply(new BigDecimal(inputTokens)).divide(MILLION));
        int output=Math.min(DESIRED_OUTPUT,available.multiply(MILLION).divide(m.output(),0,RoundingMode.DOWN).intValue());
        if(output<6000)throw new IllegalArgumentException("このモデルと記録量では上限内で生成できません。モデルか上限を変更してください");
        return new Reservation(output,ceiling(m.id(),inputTokens,output));
    }
    public static BigDecimal ceiling(int input,int output){return ceiling(MODEL,input,output);}
    public static BigDecimal ceiling(String id,int input,int output) {
        if(input<0||output<0)throw new IllegalArgumentException("Invalid usage");
        Model m=model(id);return yen(m.write().multiply(new BigDecimal(input)).add(m.output().multiply(new BigDecimal(output))));
    }
    public static BigDecimal estimate(int input,int output,int cacheWrite){return estimate(MODEL,input,output,cacheWrite);}
    public static BigDecimal estimate(String id,int input,int output,int cacheWrite) {
        if(input<0||output<0||cacheWrite<0||cacheWrite>input)throw new IllegalArgumentException("Invalid usage");
        Model m=model(id);return yen(new BigDecimal(input-cacheWrite).multiply(m.input()).add(new BigDecimal(output).multiply(m.output())).add(new BigDecimal(cacheWrite).multiply(m.write())));
    }
    private static BigDecimal yen(BigDecimal usd){return usd.divide(MILLION).multiply(YEN_PER_USD).multiply(MARGIN).setScale(2,RoundingMode.CEILING);}
    /** Planning assumptions, not observations or spending limits. Visible includes JSON. */
    public record UsageScenario(int inputMin,int inputMax,int reasoningMin,int reasoningMax,int visibleMin,int visibleMax) {}
    public static UsageScenario scenario(ReportDetail detail){
        Objects.requireNonNull(detail);
        return switch(detail){
            case BRIEF -> new UsageScenario(4000,10000,1000,5000,500,1000);
            case STANDARD -> new UsageScenario(4000,10000,1000,5000,1000,2000);
            case DETAILED -> new UsageScenario(4000,10000,1000,5000,2500,4500);
        };
    }
    public record CostRange(BigDecimal low,BigDecimal high){
        public String label(){
            int scale=high.compareTo(BigDecimal.TEN)<0?1:0;
            BigDecimal step=high.compareTo(new BigDecimal("100"))>=0?new BigDecimal("5"):scale==1?new BigDecimal("0.1"):BigDecimal.ONE;
            String lower=low.setScale(scale,RoundingMode.HALF_UP).stripTrailingZeros().toPlainString();
            String upper=high.divide(step,0,RoundingMode.CEILING).multiply(step).stripTrailingZeros().toPlainString();
            return "約"+lower+"〜"+upper+"円 / 回";
        }
    }
    public static CostRange referenceRange(String id,ReportDetail detail){
        Model m=model(id);UsageScenario s=scenario(detail);
        return new CostRange(
            yen(m.input().multiply(new BigDecimal(s.inputMin())).add(m.output().multiply(new BigDecimal(s.reasoningMin()+s.visibleMin())))),
            yen(m.input().multiply(new BigDecimal(s.inputMax())).add(m.output().multiply(new BigDecimal(s.reasoningMax()+s.visibleMax())))));
    }
    public static String referenceLabel(String id,ReportDetail detail){return referenceRange(id,detail).label();}
    /** Overall model comparison spans every report depth, independently of the current selection. */
    public static CostRange allDepthRange(String id){
        BigDecimal low=null,high=null;
        for(ReportDetail d:ReportDetail.values()){
            CostRange range=referenceRange(id,d);
            low=low==null?range.low():low.min(range.low());
            high=high==null?range.high():high.max(range.high());
        }
        return new CostRange(low,high);
    }
    public static String allDepthLabel(String id){return allDepthRange(id).label();}

    private ReportBudget(){}
}
