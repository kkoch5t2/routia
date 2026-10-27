package app.routia;
import java.time.*;
import java.util.*;
/** Validated daily slots in chronological order; zero slots disables daily reminders. */
public final class ReminderTimes {
    public static final int MAX=10;
    public static int[] defaults(){return new int[]{720,900,1080,1200,1320};}
    public static int[] validate(int[] input){
        if(input==null||input.length>MAX)throw new IllegalArgumentException("通知は0〜10回で選んでください");
        int[] result=input.clone();Arrays.sort(result);
        for(int i=0;i<result.length;i++)if(result[i]<0||result[i]>=1440||i>0&&result[i]==result[i-1])throw new IllegalArgumentException("通知時刻は00:00〜23:59で、重複しないように選んでください");
        return result;
    }
    public static int[] parse(String value){
        if(value==null)return defaults();if(value.isEmpty())return new int[0];
        String[] parts=value.split(",",-1);int[] result=new int[parts.length];
        for(int i=0;i<parts.length;i++)result[i]=Integer.parseInt(parts[i]);return validate(result);
    }
    public static String encode(int[] times){int[] sorted=validate(times);StringJoiner out=new StringJoiner(",");for(int minute:sorted)out.add(Integer.toString(minute));return out.toString();}
    /** Retain selected times and suggest unused whole-hour times for added slots. */
    public static int[] resize(int[] selected,int count){
        if(selected==null||count<0||count>MAX)throw new IllegalArgumentException("通知は0〜10回で選んでください");
        int[] result=Arrays.copyOf(selected,count);int[] candidates={720,900,1080,1200,1320,480,540,600,660,780,840,960,1020,1140,1260,1380,0};
        for(int at=selected.length;at<count;at++){
            for(int candidate:candidates){boolean used=false;for(int j=0;j<at;j++)if(result[j]==candidate){used=true;break;}if(!used){result[at]=candidate;break;}}
        }
        Arrays.sort(result);return result;
    }
    public static int stage(int slot,int count){if(slot<0||slot>=count||count<1||count>MAX)return 0;if(count==1)return 0;return slot==count-1?2:slot>=count/2?1:0;}
    public static int stageAt(int minute,int[] times){int stage=0;for(int at=0;at<times.length;at++)if(minute>=times[at])stage=stage(at,times.length);else break;return stage;}
    public static boolean due(LocalDateTime now,LocalDate date,int minute){if(minute<0||minute>=1440||!now.toLocalDate().equals(date))return false;long delay=Duration.between(date.atStartOfDay().plusMinutes(minute),now).toMinutes();return !now.isBefore(date.atStartOfDay().plusMinutes(minute))&&delay<90;}
    public static String format(int minute){return String.format(Locale.ROOT,"%02d:%02d",minute/60,minute%60);}
    private ReminderTimes(){}
}
