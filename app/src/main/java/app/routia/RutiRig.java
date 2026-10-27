package app.routia;
import android.content.Context;
import android.graphics.*;
/** A layered cutout puppet: head, torso, arms, feet and tail each have their own transform. */
final class RutiRig {
 static final Rect[] PARTS={new Rect(55,53,532,456),new Rect(631,106,992,455),new Rect(1086,17,1479,499),new Rect(221,484,353,748),new Rect(743,484,879,753),new Rect(1207,563,1371,765),new Rect(107,724,509,1024),new Rect(681,825,913,977),new Rect(1178,825,1411,977)};
 static Bitmap shared;final Bitmap atlas;final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG);final RectF box=new RectF();final Path laughingHead=new Path();final Path limb=new Path();
 RutiRig(Context c){atlas=atlas(c);laughingHead.addRect(0,0,114,300,Path.Direction.CW);laughingHead.addRect(246,0,402,300,Path.Direction.CW);laughingHead.addRect(114,26,246,300,Path.Direction.CW);}
 static synchronized Bitmap atlas(Context c){if(shared==null){BitmapFactory.Options o=new BitmapFactory.Options();o.inScaled=false;shared=BitmapFactory.decodeResource(c.getResources(),R.drawable.ruti_rig,o);}return shared;}
 void part(Canvas c,int at,float x,float y,float width,float height){box.set(x-width/2,y-height/2,x+width/2,y+height/2);paint.setColor(Color.WHITE);if(at==6){c.save();c.translate(box.left,box.top);c.scale(width/PARTS[at].width(),height/PARTS[at].height());c.clipPath(laughingHead);c.drawBitmap(atlas,-PARTS[at].left,-PARTS[at].top,paint);c.restore();}else c.drawBitmap(atlas,PARTS[at],box,paint);}
 static final Rect FUR=new Rect(251,539,321,638),LEFT_PAW=new Rect(250,657,351,747),RIGHT_PAW=new Rect(746,658,876,751);
 void bone(Canvas c,float x,float y,float endX,float endY,float width){
  float length=(float)Math.hypot(endX-x,endY-y),radius=width/2;
  c.save();c.translate(x,y);c.rotate((float)Math.toDegrees(Math.atan2(endY-y,endX-x))-90);
  box.set(-radius,-radius,radius,length+radius);limb.reset();limb.addRoundRect(box,radius,radius,Path.Direction.CW);c.clipPath(limb);paint.setColor(Color.WHITE);c.drawBitmap(atlas,FUR,box,paint);c.restore();
 }
 /** Opaque fur underlap stays behind both the tilted jaw and scarf. */
 void neck(Canvas c){
  c.save();limb.reset();limb.moveTo(-23,-136);limb.lineTo(23,-136);
  limb.cubicTo(26,-126,29,-115,30,-103);limb.lineTo(-30,-103);
  limb.cubicTo(-29,-115,-26,-126,-23,-136);limb.close();c.clipPath(limb);
  box.set(-30,-136,30,-103);paint.setColor(Color.WHITE);c.drawBitmap(atlas,FUR,box,paint);c.restore();
 }
 void upper(Canvas c,CelebrationMotion.Arm arm){bone(c,arm.shoulderX(),arm.shoulderY(),arm.elbowX(),arm.elbowY(),15);}
 void fore(Canvas c,CelebrationMotion.Arm arm){bone(c,arm.elbowX(),arm.elbowY(),arm.wristX(),arm.wristY(),12);}
 void paw(Canvas c,CelebrationMotion.Arm arm,boolean left){c.save();c.translate(arm.wristX(),arm.wristY());c.rotate(arm.pawAngle());box.set(-8,-3,8,11);paint.setColor(Color.WHITE);c.drawBitmap(atlas,left?LEFT_PAW:RIGHT_PAW,box,paint);c.restore();}
 void draw(Canvas c,CelebrationMotion.Pose pose,float alpha){
  c.save();c.translate(pose.x(),pose.groundY()-pose.rise());c.saveLayerAlpha(-190,-255,130,8,Math.round(Math.max(0,Math.min(1,alpha))*255));
  // Head artwork seats 3 units into the scarf to hide its hollow source opening.
  // Soles stay at the floor while alternate little steps lift one foot at a time.
  part(c,7,-23,-13.5f-pose.leftFoot(),43,27);part(c,8,23,-13.5f-pose.rightFoot(),43,27);
  c.save();c.translate(0,-12);c.rotate(pose.body());c.scale(pose.scaleX(),pose.scaleY());c.translate(0,12);
  c.save();c.translate(-27,-23);c.rotate(pose.tail());box.set(-144,-205,10,15);paint.setColor(Color.WHITE);c.drawBitmap(atlas,PARTS[2],box,paint);c.restore();
  upper(c,pose.leftArm());upper(c,pose.rightArm());neck(c);part(c,1,0,-62,102,112);
  c.save();c.translate(0,-112);c.rotate(pose.head());part(c,pose.laugh()?6:0,0,-57,154,134);c.restore();
  fore(c,pose.leftArm());fore(c,pose.rightArm());paw(c,pose.leftArm(),true);paw(c,pose.rightArm(),false);
  c.restore();c.restore();c.restore();
 }
 void acorn(Canvas c,float x,float y,float scale){c.save();c.translate(x,y);c.scale(scale,scale);part(c,5,0,0,25,31);c.restore();}
 static void avatar(Context c,Canvas canvas,RectF area,Paint paint){Rect source=PARTS[0];float scale=Math.min(area.width()/source.width(),area.height()/source.height());canvas.save();canvas.translate(area.centerX()-source.width()*scale/2,area.centerY()-source.height()*scale/2);canvas.scale(scale,scale);canvas.clipRect(0,0,source.width(),source.height());canvas.drawBitmap(atlas(c),-source.left,-source.top,paint);canvas.restore();}
}
