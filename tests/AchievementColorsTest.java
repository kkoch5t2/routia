import app.routia.*;
public class AchievementColorsTest{
 static int n;static void check(boolean ok,String msg){n++;if(!ok)throw new AssertionError(msg);}
 static double channel(int n){double v=n/255.0;return v<=.04045?v/12.92:Math.pow((v+.055)/1.055,2.4);}
 static double luminance(int n){return .2126*channel((n>>16)&255)+.7152*channel((n>>8)&255)+.0722*channel(n&255);}
 public static void main(String[] args){int[] calendar=AchievementColors.calendar();for(int level=1;level<=3;level++){int shade=Progress.contribution(true,true,null,0,level),bg=AchievementColors.background(level),fg=AchievementColors.foreground(level);check(bg==calendar[shade],"Button exactly matches its calendar contribution shade");double a=luminance(bg),b=luminance(fg);check((Math.max(a,b)+.05)/(Math.min(a,b)+.05)>=4.5,"Grade label has accessible contrast");check((bg>>>24)==255,"All grades have an opaque background");}check(luminance(AchievementColors.background(1))>luminance(AchievementColors.background(2))&&luminance(AchievementColors.background(2))>luminance(AchievementColors.background(3)),"Achievement intensity darkens in order");System.out.println("PASS "+n+" shared-palette assertions");}
}
