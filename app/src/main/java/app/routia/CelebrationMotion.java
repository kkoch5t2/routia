package app.routia;
/** Clap, turn forearms upright, then lift into a hands-above-elbows celebration pose. */
public final class CelebrationMotion {
 public static final float UPPER_ARM=19,FOREARM=19,PAW_CENTER=4;
 public record Arm(float shoulderX,float shoulderY,float elbowX,float elbowY,float wristX,float wristY,float foreDepth,float pawAngle){
  public float pawX(){return wristX-PAW_CENTER*(float)Math.sin(Math.toRadians(pawAngle));}
  public float pawY(){return wristY+PAW_CENTER*(float)Math.cos(Math.toRadians(pawAngle));}
 }
 public record Pose(float x,float groundY,float rise,float scaleX,float scaleY,boolean grounded,float body,float head,float tail,Arm leftArm,Arm rightArm,float leftFoot,float rightFoot,boolean laugh){}
 static float clamp(float n){return Math.max(0,Math.min(1,n));}
 static float ease(float n){n=clamp(n);return n*n*(3-2*n);}
 static float mix(float a,float b,float t){return a+(b-a)*ease(t);}
 static Arm arm(int side,float upper,float bend,float roll,float flex){
  double u=Math.toRadians(upper),b=Math.toRadians(bend),r=Math.toRadians(roll);
  float sx=side*28,sy=-88,ux=side*(float)Math.sin(u),uy=(float)Math.cos(u),vx=-side*(float)Math.cos(u),vy=(float)Math.sin(u);
  float ex=sx+UPPER_ARM*ux,ey=sy+UPPER_ARM*uy,along=(float)Math.cos(b),across=(float)(Math.sin(b)*Math.cos(r));
  float fx=FOREARM*(along*ux+across*vx),fy=FOREARM*(along*uy+across*vy),depth=FOREARM*(float)(Math.sin(b)*Math.sin(r));
  float angle=(float)Math.toDegrees(Math.atan2(-fx,fy))-side*flex;
  return new Arm(sx,sy,ex,ey,ex+fx,ey+fy,depth,angle);
 }
 public static Pose at(float phase){
  float p=clamp(phase),rise=0,sx=1,sy=1,body=0,head=0,lf=0,rf=0,upper=50,bend=65,roll=0,flex=0,rightUpper,rightBend,rightFlex;
  if(p<.12f){float t=p/.12f;head=mix(0,-7,t);body=mix(0,2,t);upper=mix(50,70,t);bend=mix(65,125,t);}
  else if(p<.32f){float t=(p-.12f)/.20f,clap=.5f-.5f*(float)Math.cos(t*Math.PI*4);upper=70-65*clap;bend=125-20*clap;flex=10*clap;head=-7+4*(float)Math.sin(t*Math.PI*4);body=2-2*t;}
  // First lower the elbows close to the torso and turn the palms/forearms up.
  else if(p<.37f){float t=(p-.32f)/.05f;upper=mix(70,30,t);bend=125;roll=mix(0,60,t);head=mix(-7,-5.5f,t);}
  // Only then raise the upper arms. Wrist stays above elbow through this lift.
  else if(p<.43f){float t=(p-.37f)/.06f;upper=mix(30,80,t);bend=mix(125,85,t);roll=mix(60,180,t);flex=mix(0,10,t);head=mix(-5.5f,-3,t);}
  else if(p<.62f){float t=(p-.43f)/.19f,q=(float)Math.sin(t*Math.PI);rise=14*4*t*(1-t);upper=80+3*q;bend=85;roll=180;flex=10;head=-3;}
  else if(p<.70f){float t=(p-.62f)/.08f,q=(float)Math.sin(t*Math.PI);sy=1-.09f*q;sx=1+.05f*q;upper=80;bend=85;roll=180;flex=10;head=-3+4*q;}
  else if(p<.94f){float t=(p-.70f)/.24f,w=(float)Math.sin(t*Math.PI*4),envelope=(float)Math.sin(t*Math.PI);body=4*w*envelope;head=-3-6*w*envelope;upper=80+3*w*envelope;bend=85+2*w*envelope;roll=180;flex=10+6*w*envelope;lf=Math.max(0,w)*5*envelope;rf=Math.max(0,-w)*5*envelope;}
  else{float t=(p-.94f)/.06f;upper=mix(80,78,t);bend=85;roll=180;flex=mix(10,8,t);head=-3;}
  rightUpper=upper;rightBend=bend;rightFlex=flex;
  if(p>.70f&&p<.94f){float t=(p-.70f)/.24f,w=(float)Math.sin(t*Math.PI*4+.8)*(float)Math.sin(t*Math.PI);rightUpper=80-3*w;rightBend=85-2*w;rightFlex=10-6*w;}
  float energy=ease(p/.10f)*(1-ease((p-.90f)/.10f)),tail=(float)Math.sin(p*Math.PI*12-.5)*12*energy;
  return new Pose(200,292,rise,sx,sy,p<=.43f||p>=.62f,body,head,tail,arm(-1,upper,bend,roll,flex),arm(1,rightUpper,rightBend,roll,rightFlex),lf,rf,p>=.13f&&!(p>.765f&&p<.825f));
 }
 public static float clapX(Pose p){double a=Math.toRadians(p.body());float x=(p.leftArm().pawX()+p.rightArm().pawX())*.5f*p.scaleX(),y=((p.leftArm().pawY()+p.rightArm().pawY())*.5f+12)*p.scaleY();return p.x()+x*(float)Math.cos(a)-y*(float)Math.sin(a);}
 public static float clapY(Pose p){double a=Math.toRadians(p.body());float x=(p.leftArm().pawX()+p.rightArm().pawX())*.5f*p.scaleX(),y=((p.leftArm().pawY()+p.rightArm().pawY())*.5f+12)*p.scaleY();return p.groundY()-p.rise()-12+x*(float)Math.sin(a)+y*(float)Math.cos(a);}
 private CelebrationMotion(){}
}
