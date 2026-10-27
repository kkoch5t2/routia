import app.routia.CelebrationMotion;
/** User-reference regression: elbows outside, forearms upright, paws above wrists. */
public class RaisedArmsTest{
 static int n;static void check(boolean ok,String why){n++;if(!ok)throw new AssertionError(why);}
 static boolean upright(CelebrationMotion.Arm a){float dx=a.wristX()-a.elbowX(),dy=a.wristY()-a.elbowY();return dy<-16&&Math.abs(dx)*2<-dy;}
 public static void main(String[] args){
  boolean up=true,vertical=true,palms=true,elbows=true,lift=true;
  for(int k=430;k<=1000;k++){var p=CelebrationMotion.at(k/1000f);
   for(var a:new CelebrationMotion.Arm[]{p.leftArm(),p.rightArm()}){up&=a.wristY()<a.elbowY()-16;vertical&=upright(a);palms&=a.pawY()<a.wristY()-3;elbows&=Math.abs(a.elbowX())>Math.abs(a.shoulderX())+15&&Math.abs(a.elbowY()-a.shoulderY())<8;}
  }
  for(int k=370;k<=430;k++){var p=CelebrationMotion.at(k/1000f);lift&=p.leftArm().wristY()<p.leftArm().elbowY()&&p.rightArm().wristY()<p.rightArm().elbowY();}
  check(up,"Raised hands must be ABOVE elbows throughout the hold, hop, landing and finish");
  check(vertical,"Raised forearms must point mostly upward, not sideways/downward");
  check(palms,"Raised paws must point above wrists, not hang down");
  check(elbows,"Raised elbows sit outside the shoulders with upper arms nearly level");
  check(lift,"Forearms are turned upward before lifting the upper arms");
  var end=CelebrationMotion.at(1);check(upright(end.leftArm())&&upright(end.rightArm()),"Reduced-motion final still must use the reference pose");
  System.out.println("PASS "+n+" upward-forearm reference assertions");
 }
}
