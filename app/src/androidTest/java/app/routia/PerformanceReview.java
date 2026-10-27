package app.routia;
import android.app.*;
import android.appwidget.*;
import android.content.*;
import android.graphics.*;
import android.graphics.drawable.*;
import android.os.*;
import android.view.*;
import android.widget.*;
import java.io.*;
import java.time.*;
import java.util.*;
/** Real rendered articulation, varied scenes, grade colors and third widget action. Emulator only. */
final class PerformanceReview {
 final Instrumentation i;final Context c;int checks;MainActivity extraActivity;
 PerformanceReview(Instrumentation i){this.i=i;c=i.getTargetContext();}
 void check(boolean ok,String why){checks++;if(!ok)throw new AssertionError(why);}
 TextView button(View v,String label){if(v instanceof TextView&&v.isClickable()&&((TextView)v).getText().toString().equals(label))return (TextView)v;if(v instanceof ViewGroup)for(int k=0;k<((ViewGroup)v).getChildCount();k++){TextView b=button(((ViewGroup)v).getChildAt(k),label);if(b!=null)return b;}return null;}
 int color(TextView v){Drawable drawable=v.getBackground();if(drawable instanceof RippleDrawable)drawable=((RippleDrawable)drawable).getDrawable(0);return ((GradientDrawable)drawable).getColor().getDefaultColor();}
 void noFrame(TextView view,int level){
  Drawable background=view.getBackground();if(background instanceof RippleDrawable)background=((RippleDrawable)background).getDrawable(0);Rect old=new Rect(background.getBounds());int width=Ui.dp(c,200),height=Ui.dp(c,54);
  Bitmap image=Bitmap.createBitmap(width,height,Bitmap.Config.ARGB_8888);background.setBounds(0,0,width,height);background.draw(new Canvas(image));check(image.getPixel(1,height/2)==AchievementColors.background(level),"Grade has solid fill to its edge with no outline");background.setBounds(old);image.recycle();
 }
 void invoke(MainActivity a,String name,Class<?>[] types,Object... args){i.runOnMainSync(()->{try{var m=MainActivity.class.getDeclaredMethod(name,types);m.setAccessible(true);m.invoke(a,args);}catch(Exception e){throw new AssertionError(e);}});i.waitForIdleSync();}
 void save(Bitmap image,String name){try(FileOutputStream out=new FileOutputStream(new File(c.getExternalFilesDir(null),name+".png"))){image.compress(Bitmap.CompressFormat.PNG,100,out);}catch(Exception e){throw new AssertionError(e);}}
 int run()throws Exception{
  if(!Build.HARDWARE.contains("ranchu")&&!Build.HARDWARE.contains("goldfish"))throw new AssertionError("Emulator only");
  Store s=Store.get(c);check(s.habits().isEmpty(),"Requires empty emulator app");long id=s.saveConfiguredHabit(0,"","英語を聞く","","","","check",0,"");int original=AppColors.selected(c);MainActivity a=null;
  try{
   a=(MainActivity)i.startActivitySync(new Intent(c,MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK));MainActivity active=a;CriteriaReview helpers=new CriteriaReview(i);View decor=a.getWindow().getDecorView();
   i.runOnMainSync(()->{for(int level=1;level<=3;level++){TextView grade=button(decor,Rules.grade(level));check(grade!=null,"Every grade is visible");check(color(grade)==GrassView.COLORS[Progress.contribution(true,true,null,0,level)],"All button backgrounds exactly match calendar grades");check(grade.getCurrentTextColor()==AchievementColors.foreground(level),"Grade text uses the contrast color");noFrame(grade,level);}});helpers.capture(decor,"performance-grades");
   for(boolean compact:new boolean[]{false,true})i.runOnMainSync(()->{View row=HabitWidget.row(c,s.habit(id),LocalDate.now(),true,compact).apply(c,new FrameLayout(c));int width=Ui.dp(c,compact?200:288);row.measure(View.MeasureSpec.makeMeasureSpec(width,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(0,View.MeasureSpec.UNSPECIFIED));row.layout(0,0,width,row.getMeasuredHeight());int level=0;for(int field:new int[]{R.id.row_min_button,R.id.row_full_button,R.id.row_extra_button}){TextView action=row.findViewById(field);check(action!=null&&color(action)==AchievementColors.background(++level),"All widget grades match calendar colors");check(action.getContentDescription().toString().contains(Rules.grade(level)),"Icon-only actions retain accessible grade names");}TextView goal=row.findViewById(R.id.row_goal);check(goal.getLayout().getEllipsisCount(goal.getLineCount()-1)==0,"Representative habit title still fits beside three compact actions");Bitmap b=Bitmap.createBitmap(width,row.getHeight(),Bitmap.Config.ARGB_8888);Canvas canvas=new Canvas(b);canvas.drawColor(0xffFFF0D0);row.draw(canvas);save(b,compact?"performance-widget-compact":"performance-widget");b.recycle();});
   s.record(id,LocalDate.now(),1,false);invoke(a,"correct",new Class<?>[]{Store.Habit.class},s.habit(id));View edit=helpers.window("今日の記録を修正");RadioGroup choices=UxReview.type(edit,RadioGroup.class);i.runOnMainSync(()->{for(int k=0;k<3;k++){check(color((TextView)choices.getChildAt(k))==AchievementColors.background(k+1),"Correction uses same palette");noFrame((TextView)choices.getChildAt(k),k+1);}});helpers.capture(edit,"performance-correction");helpers.tap(edit,"戻る");
   final Set<Integer> hashes=new HashSet<>();i.runOnMainSync(()->{for(int pose=0;pose<6;pose++){Bitmap image=Mascot.bitmap(c,pose,240);int[] pixels=new int[240*240];image.getPixels(pixels,0,240,0,0,240,240);hashes.add(Arrays.hashCode(pixels));save(image,"performance-scene-"+pose);image.recycle();}check(hashes.size()==6,"Six distinct context illustrations are delivered");});
   check(Mascot.forExplanation("集中タイマー")==Mascot.FOCUS&&Mascot.forExplanation("お休みチケット")==Mascot.CALM&&Mascot.forExplanation("成果の見方")==Mascot.PROGRESS,"Explanations select relevant character actions");
   i.runOnMainSync(()->{Celebration.Scene scene=new Celebration.Scene(c,false,7,3,false);scene.measure(View.MeasureSpec.makeMeasureSpec(480,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(466,View.MeasureSpec.EXACTLY));scene.layout(0,0,480,466);float[] phases={.08f,.17f,.22f,.27f,.40f,.52f,.76f,1};for(int k=0;k<phases.length;k++){scene.phase=phases[k];Bitmap image=Bitmap.createBitmap(480,466,Bitmap.Config.ARGB_8888);Canvas canvas=new Canvas(image);canvas.drawColor(0xffFFF9EF);scene.draw(canvas);save(image,"performance-pose-"+k);image.recycle();}});
   s.record(id,LocalDate.now(),0,true);AppColors.save(c,6);extraActivity=(MainActivity)i.startActivitySync(new Intent(c,MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK));View blackDecor=extraActivity.getWindow().getDecorView();i.runOnMainSync(()->{for(int level=1;level<=3;level++)check(color(button(blackDecor,Rules.grade(level)))==AchievementColors.background(level),"Black theme preserves grade color identity");});helpers.capture(blackDecor,"performance-black");AppColors.save(c,original);
   Drawable icon=c.getPackageManager().getApplicationIcon(c.getPackageName());i.runOnMainSync(()->{Bitmap image=Bitmap.createBitmap(240,240,Bitmap.Config.ARGB_8888);icon.setBounds(0,0,240,240);icon.draw(new Canvas(image));save(image,"performance-icon");image.recycle();if(Build.VERSION.SDK_INT>=33){Drawable mono=((AdaptiveIconDrawable)icon).getMonochrome();check(mono!=null,"Themed icon is supplied");Bitmap glyph=Bitmap.createBitmap(240,240,Bitmap.Config.ARGB_8888);Canvas cv=new Canvas(glyph);cv.drawColor(0xff20563F);mono.setTint(Color.WHITE);mono.setBounds(0,0,240,240);mono.draw(cv);save(glyph,"performance-icon-mono");glyph.recycle();}});
   widgetChallenge(s,id);
   return checks;
  }finally{AppColors.save(c,original);s.delete(id);if(extraActivity!=null)i.runOnMainSync(extraActivity::finish);if(a!=null){MainActivity closing=a;i.runOnMainSync(closing::finish);}}
 }
 void widgetChallenge(Store s,long habit)throws Exception{
  AppWidgetHost host=new AppWidgetHost(c,4690);int id=host.allocateAppWidgetId();i.getUiAutomation().adoptShellPermissionIdentity("android.permission.BIND_APPWIDGET");try{check(AppWidgetManager.getInstance(c).bindAppWidgetIdIfAllowed(id,new ComponentName(c,HabitWidget.class)),"Own challenge widget bound");}finally{i.getUiAutomation().dropShellPermissionIdentity();}
  try{WidgetCollections.recordTemplate(c,id).send(c,0,new Intent().putExtra("habit",habit).putExtra("level",3).putExtra("shownDay",LocalDate.now().toString()));SystemClock.sleep(600);check(s.level(habit,LocalDate.now())==3,"Actual third widget action records challenge");RecordingReview helper=new RecordingReview(i);check(helper.global(R.id.celebration_scene)!=null,"Challenge action opens animated performance");SystemClock.sleep(3900);helper.closeWin();}finally{host.deleteAppWidgetId(id);}
 }
}
