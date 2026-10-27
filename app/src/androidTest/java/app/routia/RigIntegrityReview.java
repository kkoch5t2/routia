package app.routia;
import android.app.Instrumentation;
import android.content.Context;
import android.graphics.*;
import android.os.Build;
import android.view.View;
import java.io.*;
import java.util.*;
/** Pixel connectivity, rather than coordinate-only tests: no limb may be a floating island. */
final class RigIntegrityReview {
 final Instrumentation i;final Context c;int checks;final File directory;
 RigIntegrityReview(Instrumentation i){this.i=i;c=i.getTargetContext();directory=new File(c.getExternalFilesDir(null),"rig-review");directory.mkdirs();}
 void save(Bitmap image,String name){try(FileOutputStream out=new FileOutputStream(new File(directory,name+".png"))){image.compress(Bitmap.CompressFormat.PNG,100,out);}catch(IOException e){throw new AssertionError(e);}}
 static List<Integer> components(Bitmap image){
  int w=image.getWidth(),h=image.getHeight();int[] pixels=new int[w*h],queue=new int[w*h];boolean[] seen=new boolean[w*h];image.getPixels(pixels,0,w,0,0,w,h);
  List<Integer> sizes=new ArrayList<>();
  for(int at=0;at<pixels.length;at++)if(!seen[at]&&Color.alpha(pixels[at])>=128){
   int head=0,tail=0;queue[tail++]=at;seen[at]=true;
   while(head<tail){int q=queue[head++],x=q%w,y=q/w;for(int dy=-1;dy<=1;dy++)for(int dx=-1;dx<=1;dx++){int xx=x+dx,yy=y+dy;if(xx<0||xx>=w||yy<0||yy>=h)continue;int n=yy*w+xx;if(!seen[n]&&Color.alpha(pixels[n])>=128){seen[n]=true;queue[tail++]=n;}}}
   if(tail>=12)sizes.add(tail);
  }
  sizes.sort(Collections.reverseOrder());return sizes;
 }
 /** Inspect the neck seam itself; whole-body connectivity misses small holes. */
 static int neckGapPixels(Bitmap image,CelebrationMotion.Pose p){
  Matrix transform=new Matrix();transform.setTranslate(p.x(),p.groundY()-p.rise());
  transform.preTranslate(0,-12);transform.preRotate(p.body());transform.preScale(p.scaleX(),p.scaleY());transform.preTranslate(0,12);
  int gaps=0;float[] point=new float[2];
  for(int y=-123;y<=-110;y++)for(int x=-22;x<=22;x++){
   point[0]=x;point[1]=y;transform.mapPoints(point);
   if(Color.alpha(image.getPixel(Math.round(point[0]),Math.round(point[1])))<250)gaps++;
  }
  return gaps;
 }
 int run(){
  if(!Build.HARDWARE.contains("ranchu")&&!Build.HARDWARE.contains("goldfish"))throw new AssertionError("Emulator only");
  RutiRig rig=new RutiRig(c);Bitmap bitmap=Bitmap.createBitmap(400,340,Bitmap.Config.ARGB_8888);List<String> broken=new ArrayList<>();
  List<String> neckGaps=new ArrayList<>();
  for(int frame=0;frame<=380;frame++){
   float phase=frame/380f;CelebrationMotion.Pose pose=CelebrationMotion.at(phase);bitmap.eraseColor(Color.TRANSPARENT);rig.draw(new Canvas(bitmap),pose,1);
   List<Integer> parts=components(bitmap);checks++;if(parts.size()!=1){if(broken.size()<8)broken.add("phase="+phase+" components="+parts);if(frame==380)save(bitmap,"disconnected-finish");}
   int gaps=neckGapPixels(bitmap,pose);checks++;if(gaps>0&&neckGaps.size()<8){neckGaps.add("phase="+phase+" gapPixels="+gaps);save(bitmap,"neck-gap-"+frame);}
  }
  bitmap.recycle();
  float[] phases={.08f,.17f,.22f,.27f,.30f,.33f,.35f,.37f,.39f,.41f,.43f,.48f,.55f,.66f,.76f,.82f,1};
  final Throwable[] error={null};i.runOnMainSync(()->{try{
   for(boolean dark:new boolean[]{false,true}){
    Celebration.Scene scene=new Celebration.Scene(c,dark,1,3,false);scene.measure(View.MeasureSpec.makeMeasureSpec(640,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(622,View.MeasureSpec.EXACTLY));scene.layout(0,0,640,622);
    for(int k=0;k<phases.length;k++){scene.phase=phases[k];Bitmap image=Bitmap.createBitmap(640,622,Bitmap.Config.ARGB_8888);Canvas cv=new Canvas(image);cv.drawColor(dark?0xff141414:0xffFFF9EF);scene.draw(cv);save(image,(dark?"black-":"cream-")+String.format(Locale.ROOT,"%02d",k));image.recycle();}
    for(int k=0;k<114;k++){scene.phase=k/113f;Bitmap image=Bitmap.createBitmap(480,466,Bitmap.Config.ARGB_8888);Canvas cv=new Canvas(image);cv.drawColor(dark?0xff141414:0xffFFF9EF);cv.scale(.75f,.75f);scene.draw(cv);save(image,(dark?"black-frame-":"cream-frame-")+String.format(Locale.ROOT,"%03d",k));image.recycle();}
   }
  }catch(Throwable t){error[0]=t;}});
  if(error[0]!=null)throw new AssertionError(error[0]);
  if(!neckGaps.isEmpty())throw new AssertionError("Background visible through neck seam: "+neckGaps);
  if(!broken.isEmpty())throw new AssertionError("Detached visible body parts: "+broken);
  return checks;
 }
}
