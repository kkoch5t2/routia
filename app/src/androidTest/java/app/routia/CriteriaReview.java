package app.routia;
import android.app.*;
import android.content.*;
import android.database.sqlite.*;
import android.graphics.*;
import android.graphics.drawable.*;
import android.os.*;
import android.view.*;
import android.view.inspector.WindowInspector;
import android.widget.*;
import java.time.*;
import java.io.*;
import java.util.*;
/** Optional setup, always-present challenge and coherent Ruti identity. Emulator only. */
final class CriteriaReview {
 final Instrumentation i;final Context c;int checks;
 CriteriaReview(Instrumentation i){this.i=i;c=i.getTargetContext();}
 void check(boolean ok,String message){checks++;if(!ok)throw new AssertionError(message);}
 boolean contains(View v,String text){if(v instanceof TextView&&((TextView)v).getText().toString().contains(text))return true;if(v instanceof ViewGroup)for(int k=0;k<((ViewGroup)v).getChildCount();k++)if(contains(((ViewGroup)v).getChildAt(k),text))return true;return false;}
 TextView button(View v,String label){if(v instanceof TextView&&v.isClickable()&&((TextView)v).getText().toString().equals(label))return (TextView)v;if(v instanceof ViewGroup)for(int k=0;k<((ViewGroup)v).getChildCount();k++){TextView b=button(((ViewGroup)v).getChildAt(k),label);if(b!=null)return b;}return null;}
 View window(String text){for(View v:WindowInspector.getGlobalWindowViews())if(contains(v,text))return v;throw new AssertionError("Missing own window "+text);}
 List<EditText> inputs(View v){List<EditText> all=new ArrayList<>();if(v instanceof EditText)all.add((EditText)v);if(v instanceof ViewGroup)for(int k=0;k<((ViewGroup)v).getChildCount();k++)all.addAll(inputs(((ViewGroup)v).getChildAt(k)));return all;}
 void tap(View v,String label){i.runOnMainSync(()->{TextView b=button(v,label);if(b==null)throw new AssertionError(label);b.performClick();});i.waitForIdleSync();}
 void invoke(MainActivity a,String name,Class<?>[] types,Object... args){i.runOnMainSync(()->{try{var m=MainActivity.class.getDeclaredMethod(name,types);m.setAccessible(true);m.invoke(a,args);}catch(Exception e){throw new AssertionError(e);}});i.waitForIdleSync();}
 void capture(View target,String name){i.runOnMainSync(()->{try{Bitmap image=Bitmap.createBitmap(target.getWidth(),target.getHeight(),Bitmap.Config.ARGB_8888);Canvas canvas=new Canvas(image);canvas.drawColor(Ui.surface(c,Ui.BG));target.draw(canvas);try(FileOutputStream out=new FileOutputStream(new File(c.getExternalFilesDir(null),name+".png"))){image.compress(Bitmap.CompressFormat.PNG,100,out);}image.recycle();}catch(Exception e){throw new AssertionError(e);}});}
 int run()throws Exception{
  if(!Build.HARDWARE.contains("ranchu")&&!Build.HARDWARE.contains("goldfish"))throw new AssertionError("Emulator only");
  migration();descriptionMigration();Store s=Store.get(c);check(s.habits().isEmpty(),"Requires empty app");long id=0,numeric=0;MainActivity a=null;
  try{
   a=(MainActivity)i.startActivitySync(new Intent(c,MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK));View decor=a.getWindow().getDecorView();MainActivity active=a;
   check(decor.findViewById(R.id.ruti_header) instanceof Mascot,"Ruti is present before any achievement");check(contains(decor,"こんにちは、ルティです。"),"Initial state introduces guide");capture(decor,"ruti-welcome");
   i.runOnMainSync(()->decor.findViewById(R.id.ruti_header).performClick());i.waitForIdleSync();View intro=window("よろしく、ルティ");check(intro.findViewById(R.id.ruti_guide)!=null,"Explanation uses same mascot");check(contains(intro,"どんぐりを少しずつ蓄える"),"Character role is explained");capture(intro,"ruti-introduction");tap(intro,"よろしく、ルティ");
   invoke(a,"editor",new Class<?>[]{Store.Habit.class,String.class},null,null);View form=window("すべて任意です。");check(!contains(form,"【必須】"),"No required setup labels");check(contains(form,HabitDefaults.EXTRA),"Challenge has visible default");i.runOnMainSync(()->{for(EditText input:inputs(form))input.setText("");});tap(form,"保存");
   check(s.habits().size()==1,"Completely blank editor saves");id=s.habits().get(0).id();check(s.habit(id).goal().equals(HabitDefaults.TITLE),"Blank name gets generic title");check(s.goals(id).minimum().equals(HabitDefaults.MINIMUM)&&s.goals(id).full().equals(HabitDefaults.FULL)&&s.goals(id).extra().equals(HabitDefaults.EXTRA),"All three blank criteria get disclosed defaults");check(s.level(id,LocalDate.now())==0,"Setup never fabricates achievement");check(s.description(id).isEmpty(),"Blank optional description saves as empty");
   TextView mini=button(decor,Rules.grade(1)),full=button(decor,Rules.grade(2)),extra=button(decor,Rules.grade(3));check(mini!=null&&full!=null&&extra!=null,"All three grades always appear");int[] m=new int[2],f=new int[2],e=new int[2];mini.getLocationOnScreen(m);full.getLocationOnScreen(f);extra.getLocationOnScreen(e);check(m[1]<f[1]&&f[1]<e[1],"All grades ascend");capture(decor,"ruti-defaults");
   numeric=s.saveConfiguredHabit(0,"","","","","","count",0,"");check(s.metric(numeric).unit().equals("回")&&s.metric(numeric).target()==0,"Blank count setup supplies unit and leaves target unset");s.recordAmount(numeric,LocalDate.now(),2,null,false);check(!Rules.celebrationEligible("count",0,s.level(numeric,LocalDate.now()),2d),"Generic setup never invents a numeric animation target");
   s.record(id,LocalDate.now(),1,false);invoke(a,"render",new Class<?>[0]);check(button(decor,"今日の達成を見る")==null,"Mini has no replay");check(decor.findViewById(R.id.ruti_header)!=null,"Mascot remains outside celebration");
   invoke(a,"correct",new Class<?>[]{Store.Habit.class},s.habit(id));View correction=window("今日の記録を修正");RadioGroup choices=UxReview.type(correction,RadioGroup.class);check(choices.getChildCount()==3,"Correction also always has challenge");for(int k=0;k<3;k++)check((Integer)choices.getChildAt(k).getTag()==k+1,"Correction order");check(button(correction,"今日の記録を取り消す")!=null,"Check correction still offers cancellation");tap(correction,"戻る");
   for(boolean compact:new boolean[]{false,true})i.runOnMainSync(()->{View widget=HabitWidget.layout(c,0,compact).apply(c,new FrameLayout(c));ImageView image=widget.findViewById(R.id.widget_mascot);check(image!=null&&image.getDrawable()!=null,"Recording widget has Ruti portrait");});
   i.runOnMainSync(()->{View widget=ProgressWidget.views(c,0).apply(c,new FrameLayout(c));check(((ImageView)widget.findViewById(R.id.progress_mascot)).getDrawable()!=null,"Results widget has same mascot");});
   i.runOnMainSync(()->{Dialog explanation=Ui.sheet(active,"","記録の見方","テスト用の説明です。","わかった",null),danger=Ui.sheet(active,"","記録を取り消す？","テストです。","取り消す",null);Ui.show(explanation,active);check(explanation.findViewById(R.id.ruti_guide)!=null,"Informational popup has guide");explanation.dismiss();Ui.show(danger,active);check(danger.findViewById(R.id.ruti_guide)==null,"Destructive confirmation stays focused");danger.dismiss();});
   Drawable icon=c.getPackageManager().getApplicationIcon(c.getPackageName());check(icon instanceof AdaptiveIconDrawable,"Adaptive launcher icon is installed");if(Build.VERSION.SDK_INT>=33)check(((AdaptiveIconDrawable)icon).getMonochrome()!=null,"Themed icon has a monochrome mascot");i.runOnMainSync(()->{Bitmap image=Bitmap.createBitmap(216,216,Bitmap.Config.ARGB_8888);icon.setBounds(0,0,216,216);icon.draw(new Canvas(image));try(FileOutputStream out=new FileOutputStream(new File(c.getExternalFilesDir(null),"ruti-icon.png"))){image.compress(Bitmap.CompressFormat.PNG,100,out);}catch(Exception ex){throw new AssertionError(ex);}image.recycle();});
   s.saveConfiguredHabit(id,"朝食後","英語","自分の小さな基準","番組1本","字幕なしで聞く","check",0,"");check(s.goals(id).extra().equals("字幕なしで聞く")&&s.level(id,LocalDate.now())==1,"Custom criteria and existing progress stay intact");
   String instructions="英語ニュースを10分聞く\n新しい表現を3つ書く";
   s.saveConfiguredHabit(id,"朝食後","英語","自分の小さな基準","番組1本","字幕なしで聞く","check",0,"",instructions);
   check(s.description(id).equals(instructions),"Goal instructions persist independently from records");
   s.saveConfiguredHabit(id,"朝食後","英語","自分の小さな基準","番組1本","字幕なしで聞く","check",0,"");
   check(s.description(id).equals(instructions),"Legacy updates preserve goal instructions");
   invoke(a,"render",new Class<?>[0]);check(contains(decor,"やることの詳細"),"Instructions accessible on Today");
   invoke(a,"editor",new Class<?>[]{Store.Habit.class,String.class},s.habit(id),null);
   View instructionsForm=window("やることの詳細（任意）");
   EditText instructionsInput=inputs(instructionsForm).get(1);
   check(instructionsInput.getText().toString().equals(instructions),"Goal editor restores previously saved instructions");
   i.runOnMainSync(()->instructionsInput.setText("変更後の手順"));
   tap(instructionsForm,"保存");
   check(s.description(id).equals("変更後の手順")&&s.level(id,LocalDate.now())==1,"Goal editor updates instructions without changing progress");

   return checks;
  }finally{if(id>0)s.delete(id);if(numeric>0)s.delete(numeric);if(a!=null){MainActivity closing=a;i.runOnMainSync(closing::finish);}}
 }
 void descriptionMigration(){
  String name="routia-description-v7.db";c.deleteDatabase(name);
  LocalDate today=LocalDate.now();long habit;
  try(Store previous=new Store(c,name)){
   habit=previous.saveConfiguredHabit(0,"朝食後","移行前の習慣","少し取り組む","通常達成","挑戦","check",0,"");
   previous.record(habit,today,2,false);
   SQLiteDatabase db=previous.getWritableDatabase();
   db.execSQL("ALTER TABLE habits DROP COLUMN description");
   db.setVersion(7);
  }
  try(Store upgraded=new Store(c,name)){
   check(upgraded.getReadableDatabase().getVersion()==8,"v7 upgrades to v8");
   check(upgraded.description(habit).isEmpty(),"Old goals receive empty optional instructions");
   check(upgraded.habit(habit).goal().equals("移行前の習慣")&&upgraded.goals(habit).full().equals("通常達成")&&upgraded.level(habit,today)==2,"v7 upgrade preserves goals, criteria and records");
   upgraded.saveConfiguredHabit(habit,"朝食後","移行前の習慣","少し取り組む","通常達成","挑戦","check",0,"","復習をする");
   check(upgraded.description(habit).equals("復習をする"),"Instructions writable after migration");
  }
  try(Store reopened=new Store(c,name)){check(reopened.description(habit).equals("復習をする"),"Instructions survive database reopening");}
  c.deleteDatabase(name);
 }
 void migration(){
  String name="routia-criteria-migration.db";c.deleteDatabase(name);Store old=new Store(c,name);long id=old.saveHabit(0,"","英語を聞く","既存の最小基準","check",0,"");LocalDate day=LocalDate.now();old.record(id,day,2,false);SQLiteDatabase db=old.getWritableDatabase();db.execSQL("ALTER TABLE habits DROP COLUMN description");db.execSQL("ALTER TABLE habits DROP COLUMN full_goal");db.execSQL("ALTER TABLE habits DROP COLUMN extra_goal");db.execSQL("DROP TRIGGER habit_limit");db.execSQL("DROP TABLE IF EXISTS habit_settings");db.execSQL("CREATE TRIGGER habit_limit BEFORE INSERT ON habits WHEN (SELECT count(*) FROM habits)>=3 BEGIN SELECT RAISE(ABORT,'Maximum three habits'); END");db.setVersion(4);old.close();Store upgraded=new Store(c,name);
  try{check(upgraded.getReadableDatabase().getVersion()==8,"Existing v4 upgrades with fixed ten-goal capacity");check(upgraded.goals(id).minimum().equals("既存の最小基準")&&upgraded.goals(id).full().equals("英語を聞く"),"Existing explicit criteria preserved");check(upgraded.goals(id).extra().equals(HabitDefaults.EXTRA),"Legacy blank challenge has generic visible criterion");check(upgraded.level(id,day)==2,"History preserved");}finally{upgraded.close();c.deleteDatabase(name);}
 }
}
