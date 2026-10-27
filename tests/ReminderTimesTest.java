import app.routia.ReminderTimes;
import app.routia.Progress;
import java.time.*;
import java.util.*;
public class ReminderTimesTest {
    static int n;static void check(boolean ok){n++;if(!ok)throw new AssertionError("Reminder check "+n);}
    static void invalid(int[] values){try{ReminderTimes.validate(values);throw new AssertionError("Accepted invalid schedule");}catch(IllegalArgumentException expected){n++;}}
    public static void main(String[] args){
        check(Arrays.equals(ReminderTimes.defaults(),new int[]{720,900,1080,1200,1320}));
        check(ReminderTimes.MAX==10);check(Arrays.equals(ReminderTimes.validate(new int[]{1439,0,60}),new int[]{0,60,1439}));
        int[] input={1260,750,1140};ReminderTimes.validate(input);check(input[0]==1260);
        check(ReminderTimes.validate(new int[0]).length==0);check(ReminderTimes.validate(new int[]{0}).length==1);
        invalid(new int[]{1,1,2});invalid(new int[]{-1,2,3});invalid(new int[]{2,3,1440});invalid(new int[11]);invalid(null);
        check(ReminderTimes.format(0).equals("00:00"));check(ReminderTimes.format(1439).equals("23:59"));
        check(ReminderTimes.parse("").length==0);check(Arrays.equals(ReminderTimes.parse(null),ReminderTimes.defaults()));
        check(Arrays.equals(ReminderTimes.parse("1260,750,1140"),new int[]{750,1140,1260}));check(ReminderTimes.encode(new int[0]).isEmpty());
        for(int count=0;count<=10;count++){
            int[] slots=new int[count];for(int at=0;at<count;at++)slots[at]=1439-at*60;
            int[] sorted=ReminderTimes.validate(slots);check(Arrays.equals(ReminderTimes.parse(ReminderTimes.encode(slots)),sorted));
            int[] resized=ReminderTimes.resize(new int[0],count);check(resized.length==count);check(ReminderTimes.validate(resized).length==count);
            check(ReminderTimes.stageAt(-1,resized)==0);
            if(count>1){check(ReminderTimes.stage(count-1,count)==2);check(ReminderTimes.stageAt(1439,resized)==2);}else check(ReminderTimes.stageAt(1439,resized)==0);
        }
        check(Arrays.equals(ReminderTimes.resize(new int[0],5),ReminderTimes.defaults()));
        int[] custom={435,780,1260};int[] enlarged=ReminderTimes.resize(custom,10);for(int minute:custom)check(Arrays.binarySearch(enlarged,minute)>=0);check(custom[0]==435);
        check(Arrays.equals(ReminderTimes.resize(custom,1),new int[]{435}));check(ReminderTimes.stage(0,10)==0);check(ReminderTimes.stage(5,10)==1);check(ReminderTimes.stage(9,10)==2);check(ReminderTimes.stageAt(0,new int[0])==0);
        for(String bad:new String[]{"1,1","-1","1440","12:00","1,","1,,2"})try{ReminderTimes.parse(bad);throw new AssertionError("Accepted malformed times");}catch(IllegalArgumentException expected){n++;}
        for(int count:new int[]{-1,11})try{ReminderTimes.resize(custom,count);throw new AssertionError("Accepted bad count");}catch(IllegalArgumentException expected){n++;}
        LocalDate d=LocalDate.of(2026,10,24);check(ReminderTimes.due(d.atTime(23,30),d,1410));check(ReminderTimes.due(d.atTime(23,59),d,1410));check(!ReminderTimes.due(d.plusDays(1).atTime(0,1),d,1410));check(!ReminderTimes.due(d.atTime(12,29,59),d,750));check(ReminderTimes.due(d.atTime(13,59,59),d,750));check(!ReminderTimes.due(d.atTime(14,0),d,750));check(ReminderTimes.due(d.atStartOfDay(),d,0));check(!ReminderTimes.due(d.atTime(0,1),d.minusDays(1),0));
        check(Progress.contribution(true,true,null,100,1)==1);check(Progress.contribution(true,true,null,100,2)==2);check(Progress.contribution(true,false,null,100,0)==0);check(Progress.contribution(false,true,25d,100,2)==1);check(Progress.calendarBounds(1,new Progress.Range(d.withDayOfMonth(1),d)).end().equals(LocalDate.of(2026,10,31)));check(Progress.calendarBounds(0,new Progress.Range(LocalDate.of(2026,10,19),d)).end().equals(LocalDate.of(2026,10,25)));check(Progress.contribution(true,true,null,100,3)==4);check(Progress.calendarBounds(3,new Progress.Range(d.minusYears(3),d)).start().equals(d.minusYears(3)));
        System.out.println("PASS "+n+" reminder/calendar assertions");
    }
}
