import app.routia.HabitDefaults;
public class HabitDefaultsTest {
 static int n;static void check(boolean ok,String message){n++;if(!ok)throw new AssertionError(message);}
 public static void main(String[] args){
  for(String blank:new String[]{null,""," ","　","\n\t"}){check(HabitDefaults.title(blank).equals("今日の習慣"),"Blank name works");check(HabitDefaults.minimum(blank).equals("少しでも取り組む"),"Generic mini");check(HabitDefaults.full(blank).equals("今日の目標に取り組む"),"Generic goal");check(HabitDefaults.extra(blank).equals("目標を超えて取り組む"),"Challenge always has a criterion");check(HabitDefaults.unit("count",blank).equals("回"),"Blank unit is usable");}
  check(HabitDefaults.minimum("音声を1本聞く").equals("音声を1本聞く"),"Explicit criterion remains exact");check(HabitDefaults.title("  読書  ").equals("読書"),"Whitespace cleaned");check(HabitDefaults.unit("count","km").equals("km"),"Explicit unit preserved");check(HabitDefaults.unit("time","").equals("分"),"Time unit is minutes");check(HabitDefaults.unit("check","").isEmpty(),"Check has no numeric unit");System.out.println("PASS "+n+" optional-default assertions");
 }
}
