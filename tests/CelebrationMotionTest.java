import app.routia.CelebrationMotion;
public class CelebrationMotionTest{
 static int n;static void check(boolean ok,String message){n++;if(!ok)throw new AssertionError(message);}
 static double length(float x,float y){return Math.hypot(x,y);}
 static boolean joint(CelebrationMotion.Arm a){
  float ux=a.elbowX()-a.shoulderX(),uy=a.elbowY()-a.shoulderY(),fx=a.wristX()-a.elbowX(),fy=a.wristY()-a.elbowY();
  double bend=Math.toDegrees(Math.acos(Math.max(-1,Math.min(1,(ux*fx+uy*fy)/(19*19)))));
  return Math.abs(length(ux,uy)-19)<.001&&Math.abs(Math.sqrt(fx*fx+fy*fy+a.foreDepth()*a.foreDepth())-19)<.001&&a.foreDepth()>=-.001&&bend>=30&&bend<=130;
 }
 static double distance(CelebrationMotion.Arm a,CelebrationMotion.Arm b){return length(a.wristX()-b.wristX(),a.wristY()-b.wristY());}
 public static void main(String[] args){
  boolean fixed=true,bounds=true,ground=true,joints=true,chest=true,finite=true;float minTail=100,maxTail=-100,minHead=100,maxHead=-100,minBody=100,maxBody=-100,leftStep=0,rightStep=0;
  for(int k=0;k<=1000;k++){var p=CelebrationMotion.at(k/1000f);fixed&=p.x()==200&&p.groundY()==292;bounds&=p.rise()>=0&&p.rise()<=14.001&&p.scaleX()>.8&&p.scaleY()>.8;ground&=!p.grounded()||p.rise()==0;joints&=joint(p.leftArm())&&joint(p.rightArm());chest&=p.leftArm().pawY()<-45&&p.rightArm().pawY()<-45;finite&=Float.isFinite(p.leftArm().pawAngle())&&Float.isFinite(p.rightArm().pawAngle())&&Float.isFinite(p.tail());minTail=Math.min(minTail,p.tail());maxTail=Math.max(maxTail,p.tail());minHead=Math.min(minHead,p.head());maxHead=Math.max(maxHead,p.head());minBody=Math.min(minBody,p.body());maxBody=Math.max(maxBody,p.body());leftStep=Math.max(leftStep,p.leftFoot());rightStep=Math.max(rightStep,p.rightFoot());}
  check(fixed,"No lateral drift");check(bounds&&finite,"Transforms stay bounded");check(ground,"Grounded feet stay on floor");check(joints,"Physical bones retain their length and flexion during the outward turn");check(chest,"Paws never drop to the knees or floor");
  for(float beat:new float[]{.17f,.27f}){var p=CelebrationMotion.at(beat);check(p.rightArm().pawX()-p.leftArm().pawX()<16,"Paws meet for each clap");check(p.leftArm().pawY()<-65&&p.rightArm().pawY()<-65,"Clap occurs at the chest");check(CelebrationMotion.clapY(p)<235,"Clap light follows chest-height hands");}
  var open=CelebrationMotion.at(.22f);check(open.rightArm().pawX()-open.leftArm().pawX()>45,"Hands separate between claps");
  var raised=CelebrationMotion.at(.48f);check(raised.leftArm().wristY()<raised.leftArm().elbowY()-16&&raised.rightArm().wristY()<raised.rightArm().elbowY()-16,"Both forearms point above the elbows");check(maxTail-minTail>20&&maxHead-minHead>10&&maxBody-minBody>4,"Independent head/body/tail performance");check(leftStep>1&&rightStep>1,"Both feet step");
  for(float edge:new float[]{.12f,.32f,.37f,.43f,.62f,.70f,.94f}){var a=CelebrationMotion.at(edge-.00001f);var b=CelebrationMotion.at(edge+.00001f);check(distance(a.leftArm(),b.leftArm())<.03&&distance(a.rightArm(),b.rightArm())<.03&&Math.abs(a.head()-b.head())<.15,"Joint trajectories remain continuous");}
  check(!CelebrationMotion.at(0).laugh()&&CelebrationMotion.at(.20f).laugh(),"Anticipation changes to laughter");var end=CelebrationMotion.at(1);check(end.grounded()&&end.rise()==0&&end.leftFoot()==0&&end.rightFoot()==0&&end.laugh(),"Reduced-motion final pose is settled");System.out.println("PASS "+n+" articulated-performance assertions");
 }
}
