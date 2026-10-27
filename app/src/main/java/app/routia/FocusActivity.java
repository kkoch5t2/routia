package app.routia;
import android.app.*;
import android.content.*;
import android.graphics.*;
import android.os.*;
import android.view.*;
import android.widget.*;
import java.util.*;
public class FocusActivity extends Activity {
    private final Handler handler=new Handler(Looper.getMainLooper());private TextView clock,status,pinButton;private Ring ring;private boolean ending,pinRequested,hadPin;private long heldAt;
    private final Runnable tick=new Runnable(){public void run(){update();if(!ending){Focus.checkpoint(FocusActivity.this);handler.postDelayed(this,1000);}}};
    @Override public void onCreate(Bundle state){setTheme(R.style.RoutiaDark);super.onCreate(state);getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);getWindow().setStatusBarColor(0xff163C31);getWindow().setNavigationBarColor(0xff163C31);
        LinearLayout root=Ui.col(this);root.setGravity(Gravity.CENTER);root.setBackgroundColor(0xff163C31);root.setPadding(Ui.dp(this,28),Ui.dp(this,30),Ui.dp(this,28),Ui.dp(this,30));root.setFitsSystemWindows(true);setContentView(root);
        root.setOnApplyWindowInsetsListener((v,i)->{if(Build.VERSION.SDK_INT>=30){android.graphics.Insets b=i.getInsets(WindowInsets.Type.systemBars()|WindowInsets.Type.displayCutout());v.setPadding(b.left+Ui.dp(this,28),b.top+Ui.dp(this,24),b.right+Ui.dp(this,28),b.bottom+Ui.dp(this,24));}return i;});
        TextView brand=Ui.text(this,"ROUTIA  /  FOCUS",12,0xffD5E6CF,true);LinearLayout identity=new LinearLayout(this);identity.setGravity(Gravity.CENTER_VERTICAL);identity.addView(new Mascot(this,Mascot.FOCUS),new LinearLayout.LayoutParams(Ui.dp(this,58),Ui.dp(this,58)));identity.addView(brand);root.addView(identity);Ui.gap(root,20);
        Store.Habit h=Store.get(this).habit(Focus.prefs(this).getLong("habit",-1));TextView goal=Ui.text(this,h==null?"集中タイム":h.goal(),24,Color.WHITE,true);goal.setGravity(Gravity.CENTER);root.addView(goal);Ui.gap(root,18);
        FrameLayout timer=new FrameLayout(this);ring=new Ring(this);timer.addView(ring,new FrameLayout.LayoutParams(-1,-1));clock=Ui.text(this,"00:00",58,Color.WHITE,true);clock.setGravity(Gravity.CENTER);timer.addView(clock,new FrameLayout.LayoutParams(-1,-1));root.addView(timer,new LinearLayout.LayoutParams(-1,0,1));Ui.gap(root,20);
        status=Ui.text(this,"画面固定を確認しています…",13,0xffD1DFD0,false);status.setGravity(Gravity.CENTER);root.addView(status);Ui.gap(root,10);pinButton=Ui.button(this,"画面を固定する",false,this::requestPin);root.addView(pinButton,new LinearLayout.LayoutParams(-1,-2));Ui.gap(root,10);
        HoldButton emergency=new HoldButton(this);root.addView(emergency,new LinearLayout.LayoutParams(-1,-2));
        if(Build.VERSION.SDK_INT>=30)getWindow().getInsetsController().hide(WindowInsets.Type.systemBars());else getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN|View.SYSTEM_UI_FLAG_HIDE_NAVIGATION|View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
    }
    private void pauseAndExit(){ending=true;Focus.pause(this);unlock();startActivity(new Intent(this,MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_SINGLE_TOP));finish();}
    private final Runnable emergencyExit=()->{if(!ending&&SystemClock.elapsedRealtime()-heldAt>=2900)pauseAndExit();};
    @Override public void onResume(){super.onResume();if(!Focus.exists(this)){ending=true;finish();return;}Focus.resume(this);handler.removeCallbacks(tick);handler.post(tick);
        
    }
    private void requestPin(){if(Focus.remaining(this)==0||getSystemService(KeyguardManager.class).isKeyguardLocked())return;try{android.util.Log.i("RoutiaFocus","Screen pin request, task="+getTaskId());startLockTask();}catch(IllegalStateException|SecurityException e){android.util.Log.w("RoutiaFocus","Screen pin unavailable",e);status.setText("画面固定が使えません。タイマーのみ動作しています。");}}
    @Override public void onWindowFocusChanged(boolean focused){super.onWindowFocusChanged(focused);if(focused&&!pinRequested&&!ending&&Focus.remaining(this)>0){pinRequested=true;handler.postDelayed(this::requestPin,300);}}
    @Override public void onPause(){handler.removeCallbacks(tick);handler.removeCallbacks(emergencyExit);super.onPause();}
    @Override public void onStop(){if(!ending&&!isChangingConfigurations()&&Focus.exists(this)){Focus.pause(this);pinRequested=false;}super.onStop();}
    @Override public void onDestroy(){handler.removeCallbacksAndMessages(null);super.onDestroy();}
    @Override public void onBackPressed(){Toast.makeText(this,"集中中です。緊急時は下のボタンを3秒長押し。",Toast.LENGTH_SHORT).show();}
    private void update(){
        if(ending)return;long remaining=Focus.remaining(this);if(remaining==0){finishTimer();return;}
        int secs=(int)Math.ceil(remaining/1000.0);clock.setText(String.format(Locale.ROOT,"%02d:%02d",secs/60,secs%60));ring.progress=1f-(float)remaining/(Focus.prefs(this).getInt("minutes",1)*60000f);ring.invalidate();
        int pin=getSystemService(ActivityManager.class).getLockTaskModeState();if(pin!=ActivityManager.LOCK_TASK_MODE_NONE)hadPin=true;pinButton.setVisibility(pin==ActivityManager.LOCK_TASK_MODE_NONE?View.VISIBLE:View.GONE);
        status.setText(pin!=ActivityManager.LOCK_TASK_MODE_NONE?"画面固定中  ·  今は、この一歩だけ。":hadPin?"画面固定が解除されています。タイマーは継続中。":"OSの画面固定を許可すると、ホーム操作を抑えられます。");
    }
    private void unlock(){try{stopLockTask();}catch(IllegalStateException|SecurityException ignored){}}
    private void finishTimer(){ending=true;Focus.complete(this);long id=Focus.prefs(this).getLong("habit",-1);boolean sameDay=java.time.LocalDate.now().toString().equals(Focus.prefs(this).getString("day",""));boolean celebrate=Focus.prefs(this).getBoolean("celebrate",false);Focus.clear(this,false);unlock();
        Intent back=new Intent(this,MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_SINGLE_TOP).putExtra("focusDone",id).putExtra("focusSameDay",sameDay).putExtra("focusCelebrate",celebrate);startActivity(back);finish();}
    private class HoldButton extends TextView {
        HoldButton(Context c){super(c);setText("⏸ 一時停止 · 3秒長押し");setTextColor(Ui.ink(c,Ui.GREEN));setTextSize(16);setGravity(Gravity.CENTER);setPadding(Ui.dp(c,14),Ui.dp(c,15),Ui.dp(c,14),Ui.dp(c,15));setBackground(Ui.bg(c,Ui.PALE,16));setClickable(true);setFocusable(true);setOnClickListener(v->Toast.makeText(c,"3秒長押しで保存して一時停止。あとで続きから再開できます。",Toast.LENGTH_SHORT).show());setOnLongClickListener(v->{new AlertDialog.Builder(FocusActivity.this).setTitle("⏸ 一時停止する？").setMessage("残り時間を保存します。休止中の時間は加算せず、続きから再開できます。").setNegativeButton("戻る",null).setPositiveButton("保存して一時停止",(d,w)->pauseAndExit()).show();return true;});}
        @Override public boolean onTouchEvent(MotionEvent e){if(e.getAction()==MotionEvent.ACTION_DOWN){heldAt=SystemClock.elapsedRealtime();handler.postDelayed(emergencyExit,3000);setPressed(true);return true;}if(e.getAction()==MotionEvent.ACTION_UP||e.getAction()==MotionEvent.ACTION_CANCEL){handler.removeCallbacks(emergencyExit);setPressed(false);if(e.getAction()==MotionEvent.ACTION_UP)performClick();return true;}return true;}
        @Override public boolean performClick(){return super.performClick();}
    }
    private static class Ring extends View {float progress;final RectF rect=new RectF();final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);Ring(Context c){super(c);setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);}@Override protected void onDraw(Canvas c){float size=Math.min(getWidth(),getHeight())-Ui.dp(getContext(),24),x=getWidth()/2f,y=getHeight()/2f;p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(Ui.dp(getContext(),7));p.setStrokeCap(Paint.Cap.ROUND);rect.set(x-size/2,y-size/2,x+size/2,y+size/2);p.setColor(0xff355A48);c.drawOval(rect,p);p.setColor(0xffE9D799);c.drawArc(rect,-90,Math.max(1,progress*360),false,p);}}
}
