package app.routia;
import android.animation.*;
import android.app.*;
import android.content.Context;
import android.graphics.*;
import android.os.*;
import android.view.*;
import android.widget.*;
import java.time.*;
import java.util.*;
/** Ruti performs two claps, raises her arms and celebrates with independent body parts. Saving is complete before this appears. */
public final class Celebration {
    private static final Map<Activity,Dialog> ACTIVE=new WeakHashMap<>();
    static void dismiss(Activity a){Dialog dialog=ACTIVE.remove(a);if(dialog!=null)dialog.dismiss();}
    static void show(Activity a,Store.Habit habit,int level,boolean recovery,Runnable results){show(a,habit,level,recovery,results,null);}
    static void show(Activity a,Store.Habit habit,int level,boolean recovery,Runnable results,Runnable closed){
        if(a.isFinishing()||a.isDestroyed())return;
        Store store=Store.get(a);LocalDate today=LocalDate.now();Set<LocalDate> days=new HashSet<>();for(Store.Entry e:store.entries(habit.id(),habit.created(),today))days.add(e.day());
        int streak=Progress.streak(days,store.ticketDays(habit.id()),today),total=days.size();Store.Metric metric=store.metric(habit.id());Store.Entry entry=store.entry(habit.id(),today);
        if(!Rules.celebrationEligible(metric.kind(),metric.target(),entry.level(),entry.amount()))return;
        boolean dark=Ui.night(a);int background=dark?0xff141414:0xffFFF9EF,ink=dark?0xffF5F5F3:0xff253046,muted=dark?0xffB4BECD:0xff677083;
        Dialog dialog=new Dialog(a,dark?R.style.RoutiaBlack:R.style.RoutiaExplicitLight);
        LinearLayout root=Ui.col(a);root.setBackgroundColor(background);root.setFitsSystemWindows(true);
        root.setOnApplyWindowInsetsListener((v,insets)->{if(Build.VERSION.SDK_INT>=30){android.graphics.Insets b=insets.getInsets(WindowInsets.Type.systemBars()|WindowInsets.Type.displayCutout());v.setPadding(b.left,b.top,b.right,b.bottom);}return insets;});
        LinearLayout header=new LinearLayout(a);header.setGravity(Gravity.CENTER_VERTICAL);header.setPadding(Ui.dp(a,24),Ui.dp(a,4),Ui.dp(a,12),0);
        header.addView(new Mascot(a,Mascot.AVATAR),new LinearLayout.LayoutParams(Ui.dp(a,30),Ui.dp(a,34)));TextView brand=Ui.fixedText(a,"Routia",17,ink,true);header.addView(brand,new LinearLayout.LayoutParams(0,-2,1));
        TextView replay=Ui.fixedText(a,"↻",27,muted,false);replay.setGravity(Gravity.CENTER);replay.setMinWidth(Ui.dp(a,48));replay.setMinHeight(Ui.dp(a,48));replay.setContentDescription("アニメーションをもう一度見る");header.addView(replay);
        TextView close=Ui.fixedText(a,"×",30,muted,false);close.setGravity(Gravity.CENTER);close.setMinWidth(Ui.dp(a,48));close.setMinHeight(Ui.dp(a,48));close.setContentDescription("達成画面を閉じる");close.setOnClickListener(v->dialog.dismiss());header.addView(close);root.addView(header);
        ScrollView scroll=new ScrollView(a);scroll.setFillViewport(true);scroll.setClipToPadding(false);LinearLayout body=Ui.col(a);body.setGravity(Gravity.CENTER_HORIZONTAL);body.setPadding(Ui.dp(a,24),Ui.dp(a,8),Ui.dp(a,24),Ui.dp(a,14));scroll.addView(body);root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        String headline=level==3?"チャレンジ達成！":recovery?"おかえり、目標達成！":streak>0&&streak%7==0?streak+"日継続、おめでとう！":"今日の目標、達成！";
        TextView title=Ui.fixedText(a,headline,27,ink,true);title.setGravity(Gravity.CENTER);title.setMaxLines(2);body.addView(title,new LinearLayout.LayoutParams(-1,-2));Ui.gap(body,8);
        TextView goal=Ui.fixedText(a,habit.goal(),22,ink,true);goal.setGravity(Gravity.CENTER);goal.setMaxLines(2);body.addView(goal,new LinearLayout.LayoutParams(-1,-2));
        Scene scene=new Scene(a,dark,streak,level,recovery);scene.setId(R.id.celebration_scene);int hero=Math.max(190,Math.min(320,(int)(a.getResources().getDisplayMetrics().heightPixels/a.getResources().getDisplayMetrics().density*.32f)));body.addView(scene,new LinearLayout.LayoutParams(-1,Ui.dp(a,hero)));
        String value=!metric.kind().equals("check")&&entry.amount()!=null?Ui.number(entry.amount())+metric.unit()+"を記録":"今日の一歩を記録";
        TextView stats=Ui.fixedText(a,"🔥 "+streak+"日連続",32,ink,true);stats.setId(R.id.celebration_streak);stats.setGravity(Gravity.CENTER);stats.setMaxLines(1);stats.setAutoSizeTextTypeUniformWithConfiguration(24,32,1,android.util.TypedValue.COMPLEX_UNIT_SP);body.addView(stats,new LinearLayout.LayoutParams(-1,-2));Ui.gap(body,4);
        TextView recorded=Ui.fixedText(a,value,16,muted,false);recorded.setId(R.id.celebration_recorded);recorded.setGravity(Gravity.CENTER);body.addView(recorded);Ui.gap(body,12);
        WeekRibbon ribbon=new WeekRibbon(a,days,store.ticketDays(habit.id()),today,dark);ribbon.setId(R.id.celebration_week);body.addView(ribbon,new LinearLayout.LayoutParams(-1,Ui.dp(a,60)));Ui.gap(body,4);
        TextView lifetime=Ui.fixedText(a,"累計 "+total+"日達成",12,muted,false);lifetime.setId(R.id.celebration_total);lifetime.setGravity(Gravity.CENTER);body.addView(lifetime);
        LinearLayout footer=Ui.col(a);footer.setPadding(Ui.dp(a,24),Ui.dp(a,10),Ui.dp(a,24),Ui.dp(a,14));
        TextView done=Ui.fixedText(a,"いいね、続けよう",17,dark?0xff102C24:Color.WHITE,true);done.setId(R.id.celebration_continue);done.setGravity(Gravity.CENTER);done.setMinHeight(Ui.dp(a,56));done.setBackground(Ui.fixedBg(a,dark?0xff9BE4BC:0xff237957,18));done.setOnClickListener(v->dialog.dismiss());footer.addView(done,new LinearLayout.LayoutParams(-1,-2));Ui.gap(footer,5);
        TextView history=Ui.fixedText(a,"成果を見る",14,muted,true);history.setGravity(Gravity.CENTER);history.setMinHeight(Ui.dp(a,44));history.setOnClickListener(v->{dialog.dismiss();if(results!=null)results.run();});footer.addView(history,new LinearLayout.LayoutParams(-1,-2));root.addView(footer);
        dialog.setContentView(root);Window window=dialog.getWindow();window.setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(background));window.setDimAmount(0);window.setStatusBarColor(background);window.setNavigationBarColor(background);
        boolean[] sounded={false};ValueAnimator[] running={null};
        Runnable play=()->{
            if(running[0]!=null)running[0].cancel();sounded[0]=false;
            if(!ValueAnimator.areAnimatorsEnabled()){scene.phase=1;ribbon.phase=1;scene.invalidate();ribbon.invalidate();title.setAlpha(1);recorded.setAlpha(1);return;}
            ValueAnimator animator=ValueAnimator.ofFloat(0,1);running[0]=animator;animator.setInterpolator(new android.view.animation.LinearInterpolator());animator.setDuration(3800);
            animator.addUpdateListener(v->{float phase=(float)v.getAnimatedValue();scene.phase=phase;ribbon.phase=phase;scene.invalidate();ribbon.invalidate();title.setAlpha(.45f+.55f*Scene.ease(phase/.2f));recorded.setAlpha(.35f+.65f*Scene.ease((phase-.35f)/.3f));
                if(phase>.40f&&!sounded[0]){sounded[0]=true;Feedback.celebrate(a,scene);}
            });animator.start();
        };
        replay.setOnClickListener(v->play.run());dialog.setOnDismissListener(d->{ACTIVE.remove(a);if(running[0]!=null)running[0].cancel();if(closed!=null&&!a.isDestroyed()&&!a.isChangingConfigurations())closed.run();});ACTIVE.put(a,dialog);dialog.show();window.setLayout(-1,-1);window.getDecorView().setSystemUiVisibility(dark?0:View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR|View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
        if(Build.VERSION.SDK_INT>=28)root.setAccessibilityPaneTitle("今日の達成");play.run();
    }
    static final class Scene extends View {
        final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG);final Path starPath=new Path();final RectF destination=new RectF();final Shader glow;
        
        static final float[] CONTACTS={.17f,.27f};final RutiRig rig;final boolean dark;final int streak,level;final boolean recovery;float phase=1;
        final float[] angles=new float[56],speeds=new float[56],spins=new float[56];final int[] colors={0xffFFA55C,0xffF6CF66,0xff57B6AE,0xffB7A3E6,0xffF09DAD};
        Scene(Context c,boolean dark,int streak,int level,boolean recovery){super(c);this.dark=dark;this.streak=streak;this.level=level;this.recovery=recovery;rig=new RutiRig(c);
            glow=new RadialGradient(180,169,165,new int[]{dark?0x554C3726:0xffFFF0CA,dark?0x00141414:0x00FFF9EF},null,Shader.TileMode.CLAMP);
            setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);Random random=new Random(71);for(int n=0;n<angles.length;n++){angles[n]=(float)(random.nextDouble()*Math.PI*2);speeds[n]=70+random.nextFloat()*110;spins[n]=random.nextFloat()*360;}
        }
        Scene(Context c){this(c,Ui.night(c),1,2,false);}
        static float clamp(float n){return Math.max(0,Math.min(1,n));}static float ease(float n){n=clamp(n);return n*n*(3-2*n);}
        void fill(int color){paint.setShader(null);paint.setStyle(Paint.Style.FILL);paint.setColor(color);}
        void stroke(int color,float width){fill(color);paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(width);paint.setStrokeCap(Paint.Cap.ROUND);}
        @Override protected void onDraw(Canvas c){
            super.onDraw(c);float scale=Math.min(getWidth()/360f,getHeight()/350f);c.save();c.translate(getWidth()/2f,getHeight()/2f);c.scale(scale,scale);c.translate(-180,-175);float p=clamp(phase);
            fill(Color.WHITE);paint.setShader(glow);c.drawCircle(180,169,165,paint);paint.setShader(null);
            // A quiet halo builds, then opens with the landing.
            stroke(dark?0xff39342B:0xffF0DEBB,2);c.drawArc(55,44,305,294,135,270,false,paint);
            stroke(dark?0xffD19F59:0xffDFB666,3);c.drawArc(55,44,305,294,135,ease(p/.54f)*270,false,paint);
            CelebrationMotion.Pose motion=CelebrationMotion.at(p);
            fill(dark?0xff282622:0xffF5E8CF);c.drawOval(66,285,294,304,paint);
            fill(dark?0x55000000:0x223D382F);float shadow=1-motion.rise()/100;c.drawOval(motion.x()-53*shadow,293,motion.x()+53*shadow,302,paint);
            rig.draw(c,motion,ease(p/.07f));float badge=ease((p-.34f)/.10f);if(badge>0){fill(0x66F8D995);c.drawCircle(200,25,18*badge,paint);rig.acorn(c,200,25,badge);}
            for(float contact:CONTACTS){float beat=clamp((p-contact)/.055f);if(p>=contact&&beat<1){stroke(0xffE9BB59,2);paint.setAlpha((int)((1-beat)*220));c.drawCircle(CelebrationMotion.clapX(motion),CelebrationMotion.clapY(motion),7+beat*18,paint);}}
            float impact=clamp((p-.62f)/.18f);if(impact>0&&impact<1){stroke(0xffEDBD60,3);paint.setAlpha((int)((1-impact)*190));c.drawOval(180-impact*155,298-impact*32,180+impact*155,298+impact*32,paint);}
            if(p>.40f){float t=clamp((p-.40f)/.44f);for(int n=0;n<angles.length;n++){float distance=speeds[n]*t,x=180+(float)Math.cos(angles[n])*distance,y=190+(float)Math.sin(angles[n])*distance*.78f+80*t*t;fill(colors[n%colors.length]);paint.setAlpha((int)(255*(1-t)));c.save();c.translate(x,y);c.rotate(spins[n]+t*220);if(n%5==0)star(c,0,0,5,2.2f,colors[n%colors.length],1-t);else c.drawRoundRect(-2,-4,2,4,1,1,paint);c.restore();}}
            for(int n=0;n<6;n++){float appear=ease((p-.68f-n*.025f)/.18f),angle=(float)(n*Math.PI/3-.6),x=180+(float)Math.cos(angle)*137,y=168+(float)Math.sin(angle)*111;c.save();c.translate(x,y);c.rotate(n*33);star(c,0,0,(n%2==0?7:4)*appear,2.5f*appear,n%2==0?0xffE9BC61:0xff65A99F,appear);c.restore();}
            c.restore();
        }
        void star(Canvas c,float x,float y,float outer,float inner,int color,float alpha){starPath.reset();for(int n=0;n<10;n++){double a=-Math.PI/2+n*Math.PI/5;float radius=n%2==0?outer:inner,xx=x+(float)Math.cos(a)*radius,yy=y+(float)Math.sin(a)*radius;if(n==0)starPath.moveTo(xx,yy);else starPath.lineTo(xx,yy);}starPath.close();fill(color);paint.setAlpha(Math.round(clamp(alpha)*255));c.drawPath(starPath,paint);}
    }
    static final class WeekRibbon extends View {
        final Set<LocalDate> days,tickets;final LocalDate today;final boolean dark;final LocalDate[] dates=new LocalDate[7];final String[] labels=new String[7];final Path tickPath=new Path();float phase=1;final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
        WeekRibbon(Context c,Set<LocalDate> days,Set<LocalDate> tickets,LocalDate today,boolean dark){super(c);this.days=days;this.tickets=tickets;this.today=today;this.dark=dark;String[] weekdays={"月","火","水","木","金","土","日"};for(int at=0;at<7;at++){dates[at]=today.minusDays(6-at);labels[at]=weekdays[dates[at].getDayOfWeek().getValue()-1];}setContentDescription("この7日間。"+days.stream().filter(d->!d.isBefore(today.minusDays(6))&&!d.isAfter(today)).count()+"日達成");}
        WeekRibbon(Context c){this(c,Collections.emptySet(),Collections.emptySet(),LocalDate.now(),Ui.night(c));}
        @Override protected void onDraw(Canvas canvas){float density=getResources().getDisplayMetrics().density,column=getWidth()/7f,r=12*density;paint.setTextAlign(Paint.Align.CENTER);paint.setTypeface(Typeface.DEFAULT);paint.setTextSize(10*getResources().getDisplayMetrics().scaledDensity);
            for(int i=0;i<7;i++){LocalDate day=dates[i];float x=column*(i+.5f),y=15*density;boolean success=days.contains(day),rest=tickets.contains(day);paint.setColor(dark?0xffA5B4C1:0xff75818D);canvas.drawText(labels[i],x,52*density,paint);float reveal=i==6?Scene.ease((phase-.74f)/.18f):1;paint.setColor(success?(dark?0xff9BE4BC:0xff237957):rest?0xffD9999E:dark?0xff293342:0xffE9EBDD);paint.setStyle(Paint.Style.FILL);canvas.drawCircle(x,y,r*(i==6?.75f+.25f*reveal:1),paint);
                if(success&&reveal>0){paint.setColor(dark?0xff183D2C:Color.WHITE);paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(2*density);paint.setStrokeCap(Paint.Cap.ROUND);Path tick=tickPath;tick.reset();tick.moveTo(x-4*density,y);tick.lineTo(x-1*density,y+3*density);tick.lineTo(x+5*density,y-4*density);canvas.drawPath(tick,paint);paint.setStyle(Paint.Style.FILL);}
                if(i==6){paint.setColor(0xffE3B55C);paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(1.5f*density);canvas.drawCircle(x,y,r+3*density,paint);paint.setStyle(Paint.Style.FILL);}
            }
        }
    }
    private Celebration(){}
}
