package app.routia;
/** Shared achievement palette: fixed light-to-dark greens for recordings and calendars on every background. */
public final class AchievementColors {
 public static final int EMPTY=0xffE8EDE5,MINI=0xffBDDBC3,GOAL=0xff7DB58F,MID=0xff478565,CHALLENGE=0xff20563F,TICKET=0xffF2B6B8;
 public static int background(int level){if(level<1||level>3)throw new IllegalArgumentException("達成段階を確認してください");return level==1?MINI:level==2?GOAL:CHALLENGE;}
 public static int foreground(int level){return level==3?0xffFFFFFF:0xff203C32;}
 public static int[] calendar(){return new int[]{EMPTY,MINI,GOAL,MID,CHALLENGE,TICKET};}
 private AchievementColors(){}
}
