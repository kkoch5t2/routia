package app.routia;

import android.Manifest;
import android.app.*;
import android.appwidget.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.graphics.*;
import android.graphics.drawable.*;
import android.net.Uri;
import android.os.*;
import android.provider.Settings;
import android.text.InputFilter;
import android.view.*;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class MainActivity extends Activity {
    private int BG,INK,GREEN,MUTED,PALE;
    private LinearLayout root,content;private Store store;private int page;private boolean registered;
    private LocalDate displayedDay;
    private final Map<Long,HistorySelection> historySelections=new HashMap<>();
    private long chartHabit=-1,reportTask;
    private int reportYear,reportMonth,renderVersion;
    private long reportPage,reportPages=1;
    AlertDialog reportPageDialog;
    AlertDialog reviewFeatureDialog;
    private Runnable reviewFeatureRefresh;
    EditText reportPageInput;
    private int resumeScrollY;
    Dialog recordCorrectionDialog;
    Ui.FormDialog reminderDialog;
    TimePickerDialog reminderTimePicker;
    int[] reminderDraft;
    int reminderPickerSlot=-1;
    private Runnable reminderRowsRefresh;
    private static final class HistorySelection {int period;LocalDate anchor;HistorySelection(int p,LocalDate a){period=p;anchor=a;}}
    private final Handler handler=new Handler(Looper.getMainLooper());
    private final Runnable dayCheck=new Runnable(){public void run(){if(!LocalDate.now().equals(displayedDay)){Reminders.all(MainActivity.this);HabitWidget.update(MainActivity.this);render();}handler.postDelayed(this,15000);}};
    private final BroadcastReceiver changes=new BroadcastReceiver(){public void onReceive(Context c,Intent i){render();}};
    private int dp(float n){return Ui.dp(this,n);}private TextView text(String s,int size,int color,boolean bold){return Ui.text(this,s,size,color,bold);}
    private LinearLayout column(){return Ui.col(this);}private void space(LinearLayout l,int h){Ui.gap(l,h);}private android.graphics.drawable.GradientDrawable shape(int color,int radius){return Ui.bg(this,color,radius);}
    private LinearLayout row(){LinearLayout l=new LinearLayout(this);l.setGravity(Gravity.CENTER_VERTICAL);return l;}
    private TextView button(String s,boolean primary,Runnable a){return Ui.button(this,s,primary,a);}
    static Intent settingsIntent(Context c){return new Intent(c,MainActivity.class).setAction("app.routia.OPEN_SETTINGS").addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_SINGLE_TOP);}
    private void sub(LinearLayout l,String s){l.addView(text(s,13,MUTED,false));}
    private LinearLayout card(LinearLayout parent){LinearLayout l=column();l.setPadding(dp(18),dp(18),dp(18),dp(18));l.setBackground(shape(Color.WHITE,24));LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.bottomMargin=dp(14);parent.addView(l,p);return l;}
    private void header(String eyebrow,String title,String body){sub(content,eyebrow);space(content,7);content.addView(text(title,28,INK,true));space(content,10);sub(content,body);space(content,22);}
    @Override public void onCreate(Bundle b){AppColors.applyTheme(this);super.onCreate(b);BG=Ui.surface(this,Ui.BG);INK=Ui.ink(this,Ui.INK);GREEN=Ui.ink(this,Ui.GREEN);MUTED=Ui.ink(this,Ui.MUTED);PALE=Ui.surface(this,Ui.PALE);store=Store.get(this);if(b!=null){page=b.getInt("page");resumeScrollY=b.getInt("scroll");reportTask=b.getLong("report_task",0);reportYear=b.getInt("report_year",0);reportMonth=b.getInt("report_month",0);reportPage=Math.max(0,b.getLong("report_page",0));}
        if(this instanceof AchievementChartsActivity){chartHabit=getIntent().getLongExtra("chart_habit",-1);if(store.habit(chartHabit)==null){finish();return;}}
        getWindow().setStatusBarColor(BG);getWindow().setNavigationBarColor(BG);if(Build.VERSION.SDK_INT>=27)getWindow().getDecorView().setSystemUiVisibility(Ui.night(this)?0:View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR|View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
        root=column();root.setBackgroundColor(BG);root.setFitsSystemWindows(true);setContentView(root);
        root.setOnApplyWindowInsetsListener((v,i)->{if(Build.VERSION.SDK_INT>=30){android.graphics.Insets b1=i.getInsets(WindowInsets.Type.systemBars()|WindowInsets.Type.displayCutout()|WindowInsets.Type.ime());v.setPadding(b1.left,b1.top,b1.right,b1.bottom);}else v.setPadding(i.getSystemWindowInsetLeft(),i.getSystemWindowInsetTop(),i.getSystemWindowInsetRight(),i.getSystemWindowInsetBottom());return i;});
        openWidget(getIntent());Reminders.all(this);HabitWidget.update(this);render();handleFocus(getIntent());
        if(page==3&&b!=null&&b.containsKey("report_page_draft")){String draft=b.getString("report_page_draft");root.post(()->showPageJump(draft));}
        if(b!=null&&b.containsKey("reminder_draft")){int[] draft=b.getIntArray("reminder_draft");int y=b.getInt("reminder_scroll");Bundle picker=b.getBundle("reminder_picker");int slot=b.getInt("reminder_picker_slot",-1);if(draft!=null&&draft.length<=ReminderTimes.MAX)root.post(()->showReminderTimes(draft,y,picker,slot));}
    }
    @Override public void onNewIntent(Intent i){super.onNewIntent(i);setIntent(i);if(openWidget(i))render();else handleFocus(i);}
    private boolean openWidget(Intent i){if("app.routia.OPEN_SETTINGS".equals(i.getAction())){i.setAction(null);page=2;resumeScrollY=0;return true;}if(ProgressWidget.OPEN.equals(i.getAction())){i.setAction(null);page=1;return true;}if("app.routia.OPEN_TODAY".equals(i.getAction())){i.setAction(null);page=0;return true;}return false;}
    private void handleFocus(Intent i){long id=i.getLongExtra("focusDone",-1);if(id<0)return;i.removeExtra("focusDone");Store.Habit h=store.habit(id);if(h==null)return;render();
        if(i.getBooleanExtra("focusSameDay",true)){Feedback.deliver(this,root,false,"集中時間を保存しました");if(i.getBooleanExtra("focusCelebrate",false))Celebration.show(this,h,store.level(id,LocalDate.now()),false,()->{page=1;render();});}
        else Ui.show(Ui.sheet(this,"FOCUS COMPLETE","日付をまたいで、完走。","集中時間は開始した日へ保存しました。今日の達成はまだ記録していません。","OK",null),this);
    }
    @Override public void onSaveInstanceState(Bundle b){super.onSaveInstanceState(b);if(reminderDialog!=null&&reminderDialog.isShowing()){b.putIntArray("reminder_draft",reminderDraft.clone());b.putInt("reminder_scroll",reminderDialog.formScroll.getScrollY());if(reminderTimePicker!=null&&reminderTimePicker.isShowing()){b.putBundle("reminder_picker",reminderTimePicker.onSaveInstanceState());b.putInt("reminder_picker_slot",reminderPickerSlot);}}b.putLong("report_task",reportTask);b.putInt("report_year",reportYear);b.putInt("report_month",reportMonth);b.putLong("report_page",reportPage);if(reportPageDialog!=null&&reportPageDialog.isShowing()&&reportPageInput!=null)b.putString("report_page_draft",reportPageInput.getText().toString());b.putInt("page",page);b.putInt("scroll",content!=null&&content.getParent() instanceof ScrollView?((ScrollView)content.getParent()).getScrollY():resumeScrollY);}
    @Override public void onResume(){super.onResume();if(store==null)return;historySelections.clear();Reminders.all(this);HabitWidget.update(this);ReportNotices.deliverPending(this);render();if(content.getParent() instanceof ScrollView){ScrollView sc=(ScrollView)content.getParent();Ui.restoreScroll(sc,resumeScrollY);}IntentFilter f=new IntentFilter("app.routia.CHANGED");if(Build.VERSION.SDK_INT>=33)registerReceiver(changes,f,Context.RECEIVER_NOT_EXPORTED);else legacyRegister(f);registered=true;handler.postDelayed(dayCheck,15000);}
    @android.annotation.SuppressLint("UnspecifiedRegisterReceiverFlag") private void legacyRegister(IntentFilter f){registerReceiver(changes,f,getPackageName()+".INTERNAL",null);}
    @Override public void onPause(){if(content!=null&&content.getParent() instanceof ScrollView)resumeScrollY=((ScrollView)content.getParent()).getScrollY();if(reviewFeatureDialog!=null)reviewFeatureDialog.dismiss();handler.removeCallbacks(dayCheck);if(registered){unregisterReceiver(changes);registered=false;}super.onPause();}
    @Override protected void onDestroy(){if(reminderTimePicker!=null)reminderTimePicker.dismiss();if(reminderDialog!=null)reminderDialog.dismiss();if(recordCorrectionDialog!=null)recordCorrectionDialog.dismiss();if(reportPageDialog!=null)reportPageDialog.dismiss();super.onDestroy();Celebration.dismiss(this);handler.removeCallbacksAndMessages(null);}
    private void render(){if(root==null)return;renderVersion++;store.evaluateTickets(LocalDate.now());displayedDay=LocalDate.now();root.removeAllViews();LinearLayout head=row();head.setPadding(dp(23),dp(17),dp(23),dp(12));Mascot ruti=new Mascot(this,Mascot.AVATAR);ruti.setId(R.id.ruti_header);ruti.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_YES);ruti.setContentDescription("Routiaの相棒、ルティについて");ruti.setFocusable(true);ruti.setOnClickListener(v->Mascot.intro(this));LinearLayout.LayoutParams portrait=new LinearLayout.LayoutParams(dp(38),dp(42));portrait.rightMargin=dp(8);head.addView(ruti,portrait);LinearLayout brand=column();brand.addView(text("Routia",24,GREEN,true));brand.addView(text("ルティと、今日の一歩",10,MUTED,false));head.addView(brand,new LinearLayout.LayoutParams(0,-2,1));head.addView(text(displayedDay.format(DateTimeFormatter.ofPattern("yyyy年M月d日 EEEE",Locale.JAPANESE)),12,MUTED,false));root.addView(head);
        ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);content=column();content.setPadding(dp(20),dp(8),dp(20),dp(22));scroll.addView(content);root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        if(chartHabit>=0){
            chartHistory(store.habit(chartHabit));LinearLayout foot=column();foot.setPadding(dp(20),dp(8),dp(20),dp(16));
            foot.addView(button("戻る",false,this::finish));root.addView(foot);root.requestApplyInsets();return;
        }
        if(page==0)today();else if(page==1)history();else if(page==3)reviewHub();else settings();
        LinearLayout nav=row();nav.setTag("main_navigation");nav.setPadding(dp(10),dp(8),dp(10),dp(10));
        String[] labels={"今日","成果","継続レビュー","設定"},tags={"nav_today","nav_achievements","nav_review","nav_settings"};
        int[] pages={0,1,3,2};
        for(int j=0;j<labels.length;j++){
            final int at=pages[j];View tab;
            if(at==3)tab=new PaidReviewTab(this,page==at,()->{page=at;render();});
            else{
                TextView title=button(labels[j],page==at,()->{page=at;render();});
                title.setTag(tags[j]);title.setSelected(page==at);title.setTextSize(14);title.setMinHeight(dp(66));
                title.setPadding(dp(6),dp(10),dp(6),dp(10));tab=title;
            }
            LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,-2,1);lp.setMargins(dp(3),0,dp(3),0);nav.addView(tab,lp);
        }
        root.addView(nav);root.requestApplyInsets();
    }
    private void today(){
        java.util.List<Store.Habit> habits=store.habits();int capacity=Rules.MAX_HABITS,done=0;for(Store.Habit h:habits)if(store.level(h.id(),displayedDay)>0)done++;
        boolean all=!habits.isEmpty()&&done==habits.size();int hour=LocalTime.now().getHour();int minute=hour*60+LocalTime.now().getMinute();int[] notify=Reminders.times(this);int reminderStage=ReminderTimes.stageAt(minute,notify);int accent=all?GREEN:reminderStage>=2?0xffAD493D:reminderStage==1?0xffA66B29:GREEN;
        header("SMALL STEPS. REAL PROGRESS.",all?"今日の自分に、拍手。":reminderStage>=2?"今日の一歩、まだ間に合う。":"やることを、ひとつずつ。",all?"ミニ目標も、しっかり成功。\nここまでの積み重ねは、消えません。":"きっかけがなくても、始められる。\nまずは「何をやるか」だけ決めよう。");
        if(!habits.isEmpty()){
            LinearLayout summary=card(content);LinearLayout r=row();r.addView(text(all?"TODAY COMPLETE":"TODAY'S PROGRESS",11,accent,true),new LinearLayout.LayoutParams(0,-2,1));r.addView(text(done+" / "+habits.size(),25,accent,true));summary.addView(r);space(summary,12);
            ProgressBar progress=new ProgressBar(this,null,android.R.attr.progressBarStyleHorizontal);progress.setMax(habits.size());progress.setProgress(done);progress.setProgressTintList(android.content.res.ColorStateList.valueOf(accent));summary.addView(progress,new LinearLayout.LayoutParams(-1,dp(7)));space(summary,12);
            sub(summary,all?"このあとのリマインダーはお休みです。":notify.length==0?"リマインダーはオフです。":reminderStage>=2?"今日は24:00で区切ります。ミニ目標からでも始めよう。":"今日のお知らせ  "+Reminders.timesLabel(this));
        }
        if(Focus.exists(this)){LinearLayout f=card(content);long seconds=(Focus.remaining(this)+999)/1000;boolean paused=Focus.paused(this);f.addView(text(paused?"⏸ タイマーを一時停止中":"⏱ 集中タイマーが進行中",19,INK,true));space(f,8);sub(f,"残り "+seconds/60+"分 "+seconds%60+"秒 · "+(paused?"休止中の時間は加算しません":"続きから戻れます"));space(f,12);f.addView(button(paused?"▶ 続きから再開":"⏱ タイマーへ戻る",true,()->startActivity(new Intent(this,FocusActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))));space(f,8);f.addView(button("このタイマーを取り消す",false,()->Ui.show(Ui.sheet(this,"TIMER","タイマーを取り消す？","この回の残り時間を破棄します。完走済みの記録は残ります。","取り消す",()->{Focus.clear(this,true);render();}),this)));}
        for(Store.Habit h:habits)habitCard(h);
        if(habits.size()<capacity){if(habits.isEmpty()){LinearLayout c=card(content);c.addView(new Mascot(this,Mascot.WELCOME),new LinearLayout.LayoutParams(-1,dp(100)));c.addView(text("こんにちは、ルティです。",21,INK,true));space(c,10);sub(c,"小さな一歩を、未来の蓄えに。\nまずはそのまま習慣をつくれます。内容はあとから変えても大丈夫。");space(c,18);c.addView(button("最初の習慣をつくる",true,()->editor(null,null)));space(c,9);c.addView(button("例からつくる · 英語",false,()->editor(null,"英語を聞く")));}else content.addView(button("＋ 習慣を追加  ·  "+habits.size()+" / "+capacity,false,()->editor(null,null)));}
        else sub(content,"登録済み "+habits.size()+"個 / 上限 "+capacity+"個");
        space(content,16);int tip=displayedDay.getDayOfYear()%Learn.TIPS.length;content.addView(button(Learn.TIPS[tip][0],false,()->Learn.show(this,tip)));
        if(!habits.isEmpty()&&notify.length>0&&!Reminders.allowed(this)){space(content,12);content.addView(button("通知が無効です · 許可する",false,this::notificationPermission));}
    }
    private void habitCard(Store.Habit h){
        Store.Metric metric=store.metric(h.id());Store.Entry entry=store.entry(h.id(),displayedDay);boolean check=metric.kind().equals("check"),timed=metric.kind().equals("time");
        LinearLayout c=card(content),top=row();top.addView(text(check?(entry.level()>0?Rules.grade(entry.level()):"今日のチャレンジ"):"今日の記録",13,GREEN,true),new LinearLayout.LayoutParams(0,-2,1));top.addView(button("管理",false,()->manageHabit(h)));c.addView(top);space(c,10);c.addView(text(h.goal(),24,INK,true));space(c,10);
        Set<LocalDate> days=achieved(h);sub(c,Rules.summary(Progress.streak(days,store.ticketDays(h.id()),displayedDay),days.size()));space(c,18);
        if(!check){
            c.addView(text(entry.amount()==null?"今日の記録はまだありません":"今日 "+Ui.number(entry.amount())+metric.unit(),18,MUTED,false));
            if(metric.target()>0){space(c,5);sub(c,"今日の目標 "+Ui.number(metric.target())+metric.unit());}
            space(c,18);
            todayReviewButton(c,h);
            if(timed){c.addView(button(entry.amount()==null?"タイマーを始める":"タイマーで追加する",true,()->startFocus(h)));space(c,10);}
            c.addView(button(entry.amount()==null?(timed?"分数を入力する":"数量を入力する"):"今日の合計を修正する",!timed,()->details(h)));
            if(!entry.note().isEmpty()){space(c,12);sub(c,entry.note());}
            return;
        }
        Store.Goals goals=store.goals(h.id());
        sub(c,"🌱 ミニ達成： "+(goals.minimum().isEmpty()?"基準を設定してください":goals.minimum()));space(c,5);
        sub(c,"🏆 目標達成： "+(goals.full().isEmpty()?"基準を設定してください":goals.full()));if(!goals.extra().isEmpty()){space(c,5);sub(c,"🚀 チャレンジ達成： "+goals.extra());}space(c,14);
        todayReviewButton(c,h);
        if(entry.level()==0){
            for(int level:new int[]{1,2,3}){final int chosen=level;c.addView(Ui.achievement(this,level,()->mark(h,chosen)));space(c,10);}
        }else{
            if(Rules.celebrationEligible(metric.kind(),metric.target(),entry.level(),entry.amount())){c.addView(button("今日の達成を見る",true,()->Celebration.show(this,h,entry.level(),false,()->{page=1;render();})));space(c,10);}
            c.addView(button("記録を修正",false,()->correct(h)));space(c,10);c.addView(button(entry.note().isEmpty()?"メモを残す":"メモを編集",false,()->details(h)));
            if(!entry.note().isEmpty()){space(c,12);sub(c,entry.note());}
        }
    }
    private void todayReviewButton(LinearLayout c,Store.Habit h){
        try(ReportStore reports=new ReportStore(this)){
            ReportStore.Item ready=reports.latestCompletedOn(h.id(),displayedDay);
            if(ready!=null){c.addView(button("💰️ 今日の継続レビューを見る",false,()->startActivity(ReportActivity.intent(this,h.id(),ready.id()))));space(c,10);}
        }
    }
    private void manageHabit(Store.Habit h){
        Dialog d=new Dialog(this);LinearLayout box=column();box.setPadding(dp(24),dp(24),dp(24),dp(24));box.setBackground(shape(Color.WHITE,24));box.addView(text(h.goal(),23,INK,true));space(box,20);box.addView(button("習慣の内容を編集",true,()->{d.dismiss();editor(h,null);}));space(box,20);
        Switch reportSwitch=new Switch(this);reportSwitch.setText("💰️ この目標で継続レビューを使う");reportSwitch.setTextColor(INK);reportSwitch.setMinHeight(dp(56));reportSwitch.setChecked(ReportSettings.habitEnabled(this,h.id()));
        final boolean[] reportChanging={false};
        reportSwitch.setOnCheckedChangeListener((v,on)->{
            if(reportChanging[0])return;
            if(!on){ReportSettings.saveOptions(this,ReportSettings.options(this),java.util.Collections.singletonMap(h.id(),false));return;}
            synchronized(ReportSettings.class){
                if(!ReportSettings.featureEnabled(this)){ReportSettings.saveOptions(this,ReportSettings.options(this),java.util.Collections.singletonMap(h.id(),true));return;}
            }
            reportChanging[0]=true;reportSwitch.setChecked(false);reportChanging[0]=false;
            ReportBudget.Options options=ReportSettings.options(this);
            java.util.List<String> enabledGoals=new java.util.ArrayList<>();
            for(Store.Habit target:store.habits())if(target.id()==h.id()||ReportSettings.habitEnabled(this,target.id()))enabledGoals.add(target.goal());
            ReportConsent.settings(this,options,enabledGoals,()->{
                if(!options.equals(ReportSettings.options(this))){Toast.makeText(this,"設定が変わりました。もう一度確認してください",Toast.LENGTH_LONG).show();return;}
                ReportSettings.saveOptions(this,options,java.util.Collections.singletonMap(h.id(),true));
                reportChanging[0]=true;reportSwitch.setChecked(true);reportChanging[0]=false;
            });
        });
        box.addView(reportSwitch);space(box,8);sub(box,"全体がオンのとき、選択した目標だけが対象。未達が続くと、共通設定の範囲でAPI利用料が発生します。");
        space(box,10);box.addView(button("モデル・金額・APIの共通設定",false,()->{d.dismiss();openReportSettings();}));
        space(box,28);Ui.divider(box);space(box,24);box.addView(text("習慣の削除",16,Ui.RED,true));space(box,8);sub(box,"以下の操作には確認画面を表示します。");

        space(box,16);box.addView(Ui.danger(this,"この習慣を削除",()->{if(Focus.exists(this)&&Focus.prefs(this).getLong("habit",-1)==h.id()){Toast.makeText(this,"タイマーを完了または取り消してから削除してください",Toast.LENGTH_LONG).show();return;}Ui.show(Ui.sheet(this,"","この習慣を削除する？","「"+h.goal()+"」の達成・数値・メモ・集中履歴を削除します。元には戻せません。保存したAIレポートも削除します。","削除する",()->{Reminders.cancel(this,h.id());store.delete(h.id());ReportSettings.prefs(this).edit().remove("habit_enabled_"+h.id()).remove("habit_from_"+h.id()).apply();ReportJob.schedule(this);try(ReportStore reports=new ReportStore(this)){reports.dropHabit(h.id());}Reminders.all(this);HabitWidget.update(this);d.dismiss();render();}),this);}));
        space(box,28);box.addView(button("閉じる",false,d::dismiss));ScrollView scroll=new ScrollView(this);scroll.addView(box);d.setContentView(scroll);d.getWindow().setBackgroundDrawableResource(android.R.color.transparent);Ui.show(d,this);
    }
    private void mark(Store.Habit h,int level){try{Store.Result r=store.record(h.id(),LocalDate.now(),level,false);HabitWidget.update(this);Reminders.refreshVisible(this);render();if(r.changed()){Feedback.deliver(this,root,r.recovery(),Rules.feedback(r.recovery(),r.level()));if(r.level()>=Rules.FULL)Celebration.show(this,h,r.level(),r.recovery(),()->{page=1;render();});}}catch(IllegalArgumentException e){Toast.makeText(this,e.getMessage(),Toast.LENGTH_LONG).show();}}
    private void correct(Store.Habit h){
        if(!store.metric(h.id()).kind().equals("check")){details(h);return;}
        LocalDate day=LocalDate.now();Dialog[] dialog={null};LinearLayout form=column();sub(form,"変更後の達成状態を選んで保存してください。数値とメモは残ります。");space(form,16);RadioGroup choices=new RadioGroup(this);int current=store.level(h.id(),day);
        Store.Goals goals=store.goals(h.id());
        for(int level:new int[]{1,2,3}){RadioButton r=Ui.choice(this,Rules.grade(level),level==1?goals.minimum():level==2?goals.full():goals.extra());r.setTag(level);Ui.insetChoiceIndicator(r,12);choices.addView(r);if(current==level)choices.check(r.getId());}
        choices.setOnCheckedChangeListener((g,id)->Ui.highlightGrades(g));Ui.highlightGrades(choices);form.addView(choices);
        space(form,28);Ui.divider(form);space(form,20);sub(form,"今日の達成・数値・メモを取り消す場合はこちら。");space(form,12);
        form.addView(Ui.danger(this,"今日の記録を取り消す",()->Ui.show(Ui.sheet(this,"","今日の記録を取り消す？","今日の達成・数値・メモを削除します。完走済みタイマーの履歴は残ります。","取り消す",()->{
            if(!LocalDate.now().equals(day)){dialog[0].dismiss();render();Toast.makeText(this,"日付が変わりました。今日の記録を選び直してください",Toast.LENGTH_LONG).show();return;}
            store.record(h.id(),day,0,true);HabitWidget.update(this);Reminders.refreshVisible(this);dialog[0].dismiss();render();Toast.makeText(this,"今日の記録を取り消しました",Toast.LENGTH_LONG).show();
        }),this)));
        dialog[0]=formDialog("EDIT RECORD","今日の記録を修正",form,"変更を保存",()->{
            if(!LocalDate.now().equals(day)){dialog[0].dismiss();render();Toast.makeText(this,"日付が変わりました。今日の記録を選び直してください",Toast.LENGTH_LONG).show();return;}
            RadioButton selected=choices.findViewById(choices.getCheckedRadioButtonId());store.record(h.id(),day,(Integer)selected.getTag(),true);HabitWidget.update(this);Reminders.refreshVisible(this);dialog[0].dismiss();render();Toast.makeText(this,"✅ 記録の変更を保存しました",Toast.LENGTH_LONG).show();
        });recordCorrectionDialog=dialog[0];dialog[0].setOnDismissListener(d->recordCorrectionDialog=null);Ui.show(dialog[0],this);
    }
    private EditText input(LinearLayout l,String label,String hint,String value,int max){l.addView(text(label,13,GREEN,true));space(l,6);EditText e=new EditText(this);e.setTextSize(16);e.setTextColor(INK);e.setHint(hint);e.setHintTextColor(MUTED);e.setText(value);e.setMinHeight(dp(54));e.setPadding(dp(12),dp(11),dp(12),dp(11));e.setBackground(shape(BG,12));e.setInputType(android.text.InputType.TYPE_CLASS_TEXT|android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE);e.setMaxLines(5);e.setFilters(new InputFilter[]{new InputFilter.LengthFilter(max)});l.addView(e,new LinearLayout.LayoutParams(-1,-2));space(l,16);return e;}
    private EditText numeric(LinearLayout l,String label,String hint,String value){EditText e=input(l,label,hint,value,12);e.setInputType(android.text.InputType.TYPE_CLASS_NUMBER|android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);return e;}
    private Dialog formDialog(String eyebrow,String title,LinearLayout form,String action,Runnable save){
        Ui.FormDialog d=new Ui.FormDialog(this);LinearLayout shell=column();shell.setPadding(dp(22),dp(22),dp(22),dp(18));shell.setBackground(shape(Color.WHITE,24));shell.setFocusableInTouchMode(true);shell.requestFocus();shell.addView(text(title,24,INK,true));space(shell,18);
        ScrollView scroll=new ScrollView(this);scroll.setFillViewport(false);scroll.setScrollbarFadingEnabled(false);scroll.setClipToPadding(false);scroll.addView(form);shell.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));space(shell,12);Ui.divider(shell);space(shell,10);TextView notice=text("",13,Ui.RED,true);notice.setVisibility(View.GONE);shell.addView(notice);space(shell,10);
        shell.addView(button(action,true,()->{try{save.run();}catch(IllegalArgumentException e){d.showProblem(null,e.getMessage()==null?"入力内容を確認してください":e.getMessage());}catch(RuntimeException e){android.util.Log.w("RoutiaForm","Save failed: "+e.getClass().getSimpleName());d.showProblem(null,"保存できませんでした。登録数と入力内容を確認してください。");}}),new LinearLayout.LayoutParams(-1,-2));space(shell,12);shell.addView(button("戻る",false,d::dismiss),new LinearLayout.LayoutParams(-1,-2));
        int width=Math.min(getResources().getDisplayMetrics().widthPixels-dp(76),dp(516));form.measure(View.MeasureSpec.makeMeasureSpec(width,View.MeasureSpec.AT_MOST),View.MeasureSpec.makeMeasureSpec(0,View.MeasureSpec.UNSPECIFIED));d.desiredHeight=Math.min((int)(getResources().getDisplayMetrics().heightPixels*.90),Math.min(dp(760),form.getMeasuredHeight()+dp(310)));d.bind(form,scroll,notice);d.setContentView(shell);d.getWindow().setBackgroundDrawableResource(android.R.color.transparent);d.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE|WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);return d;
    }
    private void editor(Store.Habit h,String example){
        if(h==null&&store.habits().size()>=Rules.MAX_HABITS){Toast.makeText(this,"目標は10個まで登録できます。",Toast.LENGTH_LONG).show();return;}Dialog[] d={null};LinearLayout form=column();
        sub(form,"すべて任意です。そのまま保存でき、内容はあとから変えられます。");space(form,14);EditText goal=input(form,"習慣名（任意）","空欄なら「今日の習慣」",h!=null?h.goal():example!=null?example:"",120);
        Store.Metric old=h==null?new Store.Metric("check",0,""):store.metric(h.id());Store.Goals prior=h==null?new Store.Goals(HabitDefaults.MINIMUM,HabitDefaults.FULL,HabitDefaults.EXTRA):store.goals(h.id());
        form.addView(text("記録のしかた",18,INK,true));space(form,12);RadioGroup kinds=new RadioGroup(this);
        String[] names={"✅ できたかだけ","⏱ 時間（分）","🔢 回数・数量"},descriptions={"決めた基準を選んで、1タップで記録。","タイマーまたは分数だけで記録。","数値だけで記録。達成の選択は不要。"},values={"check","time","count"};
        for(int at=0;at<3;at++){RadioButton r=Ui.choice(this,names[at],descriptions[at]);r.setTag(at);kinds.addView(r);if(values[at].equals(old.kind()))kinds.check(r.getId());}form.addView(kinds);space(form,12);
        LinearLayout criteria=column();criteria.addView(text("達成の基準を決める",18,INK,true));space(criteria,6);sub(criteria,"毎日の記録では、ここで決めた基準を選ぶだけです。");space(criteria,14);
        EditText minimum=input(criteria,"🌱 ミニ達成の基準（任意）","空欄なら「少しでも取り組む」",prior.minimum(),120);
        EditText full=input(criteria,"🏆 目標達成の基準（任意）","空欄なら「今日の目標に取り組む」",prior.full(),120);
        EditText extra=input(criteria,"🚀 チャレンジ達成の基準（任意）","空欄なら「目標を超えて取り組む」",prior.extra(),120);
        sub(criteria,"3段階をいつでも選べます。空欄で保存した基準には共通の言葉を使います。ミニ達成も継続日数に含みます。");form.addView(criteria);
        LinearLayout metric=column();EditText target=numeric(metric,"1日の数値目標（任意）","例：30",old.target()>0?Ui.number(old.target()):"");
        LinearLayout unitBox=column();EditText unit=input(unitBox,"単位（任意・空欄なら回）","例：回 / ページ / km",old.kind().equals("count")?old.unit():"回",8);metric.addView(unitBox);sub(metric,"数値目標に届いたときだけ達成アニメーションを表示します。未設定なら数値の記録だけです。");form.addView(metric);
        Runnable selection=()->{RadioButton chosen=kinds.findViewById(kinds.getCheckedRadioButtonId());int at=(Integer)chosen.getTag();criteria.setVisibility(at==0?View.VISIBLE:View.GONE);metric.setVisibility(at==0?View.GONE:View.VISIBLE);unitBox.setVisibility(at==2?View.VISIBLE:View.GONE);((TextView)metric.getChildAt(0)).setText(at==1?"1日の目標（分・任意）":"1日の数値目標（任意）");Ui.highlight(kinds);if(d[0]!=null)((Ui.FormDialog)d[0]).refreshNotice();};kinds.setOnCheckedChangeListener((g,id)->selection.run());selection.run();
        space(form,16);TextView expand=button("＋ きっかけを設定（任意）",false,()->{});form.addView(expand);space(form,12);LinearLayout optional=column();EditText cue=input(optional,"きっかけ（任意）","例：朝ごはんのあと",h==null?"":h.cue(),120);optional.setVisibility(View.GONE);form.addView(optional);expand.setOnClickListener(v->{boolean open=optional.getVisibility()!=View.VISIBLE;optional.setVisibility(open?View.VISIBLE:View.GONE);expand.setText(open?"− きっかけを閉じる":"＋ きっかけを設定（任意）");});
        d[0]=formDialog("",h==null?"習慣をつくる":"習慣を編集",form,"保存",()->{
            Ui.FormDialog dialog=(Ui.FormDialog)d[0];RadioButton chosen=kinds.findViewById(kinds.getCheckedRadioButtonId());int at=(Integer)chosen.getTag();java.util.List<String> errors=new ArrayList<>();View first=null;Double amount=null;
            if(at!=0)try{amount=Inputs.number(target.getText().toString(),"1日の数値目標",0,1000000,false,true);}catch(IllegalArgumentException e){errors.add(e.getMessage());if(first==null)first=target;}
            if(!errors.isEmpty()){dialog.showProblem(first,String.join("\n",errors));return;}
            if(h!=null&&Focus.exists(this)&&Focus.prefs(this).getLong("habit",-1)==h.id()&&!old.kind().equals(values[at])){dialog.showProblem(kinds,"タイマーを完了または取り消してから記録方法を変更してください");return;}
            try{store.saveConfiguredHabit(h==null?0:h.id(),cue.getText().toString(),goal.getText().toString(),minimum.getText().toString(),full.getText().toString(),extra.getText().toString(),values[at],amount==null?0:amount,at==0?"":at==1?"分":Inputs.clean(unit.getText().toString()));Reminders.all(this);HabitWidget.update(this);d[0].dismiss();render();Toast.makeText(this,"✅ 習慣を保存しました",Toast.LENGTH_SHORT).show();}catch(IllegalArgumentException e){dialog.showProblem(kinds,e.getMessage());}
        });
        Ui.show(d[0],this);
    }
    private void details(Store.Habit h){
        if(!store.metric(h.id()).kind().equals("check")){startActivity(RecordActivity.intent(this,h.id()));return;}
        LocalDate day=LocalDate.now();Store.Entry entry=store.entry(h.id(),day);LinearLayout form=column();EditText note=input(form,"メモ（任意）","やったことや気づきを残す",entry.note(),1000);
        Dialog[] dialog={null};dialog[0]=formDialog("","メモを残す",form,"保存",()->{store.details(h.id(),day,null,note.getText().toString());HabitWidget.update(this);dialog[0].dismiss();render();});Ui.show(dialog[0],this);
    }
    private void startFocus(Store.Habit h){
        if(!store.metric(h.id()).kind().equals("time"))return;
        if(Focus.exists(this)){startActivity(new Intent(this,FocusActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));return;}
        Store.Metric metric=store.metric(h.id());LinearLayout form=column();EditText minutes=numeric(form,"計測する時間（分）","1〜180分",Ui.number(metric.target()>0?Math.min(180,metric.target()):25));
        sub(form,"完走すると分数を自動保存します。3秒長押しで一時停止できます。");space(form,14);
        form.addView(button("画面固定・一時停止の使い方",false,()->Ui.show(Ui.sheet(this,"","集中タイマーの使い方","Androidの画面固定はOSの確認後に有効になります。固定中はホーム操作が制限されます。解除はAndroidの案内に従い、端末設定によってPIN入力が必要です。\n\n下のボタンを3秒長押しすると、残り時間を保存して一時停止します。休止中は計測せず、続きから再開できます。\n\n日付をまたぐ完走時間は開始日の履歴に保存し、翌日の達成には加算しません。","わかった",null),this)));
        Dialog[] dialog={null};dialog[0]=formDialog("","集中タイマー",form,"計測を始める",()->{double value=Inputs.number(minutes.getText().toString(),"計測する時間",1,180,true,false);Focus.start(this,h.id(),(int)value);dialog[0].dismiss();startActivity(new Intent(this,FocusActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));});Ui.show(dialog[0],this);
    }
    private Set<LocalDate> achieved(Store.Habit h){Set<LocalDate> days=new HashSet<>();for(Store.Entry e:store.entries(h.id(),h.created(),LocalDate.now()))days.add(e.day());return days;}
    private void slideChart(LinearLayout parent,View chart,int width,int height,boolean latest){
        HorizontalScrollView scroll=new HorizontalScrollView(this);scroll.setHorizontalScrollBarEnabled(true);scroll.setFillViewport(true);chart.setMinimumWidth(Math.max(dp(280),width));scroll.addView(chart,new android.widget.FrameLayout.LayoutParams(Math.max(dp(280),width),dp(height)));parent.addView(scroll,new LinearLayout.LayoutParams(-1,dp(height)));if(latest)scroll.post(()->scroll.fullScroll(View.FOCUS_RIGHT));
    }
    private void history(){
        content.addView(text("記録のサマリ",20,INK,true));space(content,6);
        sub(content,"今月の達成状況。詳しいグラフは各タスクから開けます。");space(content,14);
        java.util.List<Store.Habit> habits=store.habits();
        if(habits.isEmpty()){sub(content,"タスクを作ると、ここに記録のサマリが表示されます。");return;}
        for(Store.Habit h:habits){
            LinearLayout c=card(content);c.setTag("summary_"+h.id());c.addView(text(h.goal(),22,INK,true));space(c,10);
            Set<LocalDate> done=achieved(h);
            sub(c,Rules.summary(Progress.streak(done,store.ticketDays(h.id()),displayedDay),done.size()));space(c,12);
            Progress.Range range=Progress.range(1,displayedDay,h.created(),displayedDay);
            java.util.List<Store.Entry> entries=store.entries(h.id(),range.start(),range.end());
            int eligible=(int)java.time.temporal.ChronoUnit.DAYS.between(range.start().isBefore(h.created())?h.created():range.start(),range.end())+1;
            sub(c,range.start()+" 〜 "+range.end());space(c,12);
            LinearLayout stats=row();metricTile(stats,"✅ 今月の達成",entries.size()+"日");
            metricTile(stats,"📊 達成率",(eligible<=0?0:Math.round(entries.size()*100f/eligible))+"%");c.addView(stats);
            Store.Metric m=store.metric(h.id());
            if(!m.kind().equals("check")){
                double total=0;int measured=0;
                for(Store.Entry e:entries)if(e.amount()!=null&&e.unit().equals(m.unit())){total+=e.amount();measured++;}
                space(c,10);sub(c,measured==0?"量の記録はまだありません":"今月の合計 "+Ui.number(total)+m.unit());
            }
            space(c,14);TextView charts=button("グラフを見る",false,()->startActivity(AchievementChartsActivity.intent(this,h.id())));
            charts.setTag("charts_"+h.id());charts.setContentDescription("グラフを見る："+h.goal());c.addView(charts);
        }
    }
    private void chartHistory(Store.Habit h){
        content.addView(text("記録のグラフ",22,INK,true));space(content,6);
        sub(content,"端末の実記録から集計 · 追加料金なし");space(content,16);
            LinearLayout c=card(content);c.addView(text(h.goal(),22,INK,true));HistorySelection selected=historySelection(h);space(c,12);historyControls(c,h,selected);Set<LocalDate> daysDone=achieved(h),ticketDays=store.ticketDays(h.id());int streak=Progress.streak(daysDone,ticketDays,displayedDay);
            Progress.Range range=Progress.range(selected.period,selected.anchor,h.created(),displayedDay);LocalDate start=range.start(),end=range.end();space(c,12);
            LinearLayout summary=row();TextView streakText=text(Rules.summary(streak,daysDone.size()),13,MUTED,false);streakText.setSingleLine(true);streakText.setAutoSizeTextTypeUniformWithConfiguration(10,13,1,android.util.TypedValue.COMPLEX_UNIT_SP);summary.addView(streakText,new LinearLayout.LayoutParams(0,-2,1));int remaining=TicketRules.MONTHLY_LIMIT-store.ticketsUsed(h.id(),java.time.YearMonth.from(displayedDay));TextView ticket=infoChip("🎟 "+remaining+"/2  ⓘ","継続チケットの説明",()->ticketInfo(h,start,end));LinearLayout.LayoutParams ticketParams=new LinearLayout.LayoutParams(-2,-2);ticketParams.leftMargin=dp(8);summary.addView(ticket,ticketParams);c.addView(summary);space(c,5);
            space(c,12);int days=(int)java.time.temporal.ChronoUnit.DAYS.between(start,end)+1;sub(c,start+" 〜 "+end);space(c,14);
            java.util.List<Store.Entry> entries=store.entries(h.id(),start,end);Map<LocalDate,Store.Entry> map=new HashMap<>();for(Store.Entry e:entries)map.put(e.day(),e);Store.Metric m=store.metric(h.id());int successes=entries.size(),eligible=0,amounts=0;double total=0,scale=Math.max(1,m.target());
            for(Store.Entry e:entries)if(e.amount()!=null&&e.unit().equals(m.unit())){total+=e.amount();amounts++;scale=Math.max(scale,e.amount());}
            double[] values=new double[days],grassValues=new double[days];boolean[] has=new boolean[days];int[] shades=new int[days];Arrays.fill(grassValues,-1);
            for(int i=0;i<days;i++){LocalDate d=start.plusDays(i);if(!d.isBefore(h.created()))eligible++;Store.Entry e=map.get(d);Double amount=e!=null&&e.unit().equals(m.unit())?e.amount():null;shades[i]=Progress.contribution(m.kind().equals("check"),e!=null,amount,scale,e==null?0:e.level());if(amount!=null)grassValues[i]=amount;
                if(m.kind().equals("check")){has[i]=!d.isBefore(h.created());values[i]=e==null?0:e.level();}else if(amount!=null){has[i]=true;values[i]=amount;}
            }
            LinearLayout stats=row();metricTile(stats,"✅ この期間の達成",successes+"日");metricTile(stats,"📊 達成率",(eligible==0?0:Math.round(successes*100f/eligible))+"%");c.addView(stats);space(c,18);contributionCalendar(c,h,m,start,end,selected.period);
            space(c,24);c.addView(text("📈 日別の推移",18,INK,true));space(c,8);sub(c,m.kind().equals("check")?"🌱 ミニ → 🏆 目標 → 🚀 チャレンジ":m.unit()+" · 点線＝目標");TrendView trend=new TrendView(this,values,has,start,m.kind().equals("check")?"達成グレード":m.unit(),m.kind().equals("check")?0:m.target());if(selected.period<=1)c.addView(trend,new LinearLayout.LayoutParams(-1,dp(205)));else slideChart(c,trend,dp(80+days*24),205,true);
            if(!m.kind().equals("check")){c.addView(text(amounts==0?"数値を記録すると、線が育ちます。":"💪 この期間の合計 "+Ui.number(total)+m.unit(),19,GREEN,true));}
            if(m.kind().equals("time")){space(c,16);sub(c,"⏱ タイマーで完走 "+Ui.number(store.focused(h.id(),start,end)/60.0)+"分");}
    }
    private void refreshCatalog(boolean top){
        int y=top?0:((ScrollView)content.getParent()).getScrollY();resumeScrollY=y;
        render();Ui.restoreScroll((ScrollView)content.getParent(),y);
    }
    private void showPageJump(String draft){
        if(reportPages<=1||isFinishing()||(reportPageDialog!=null&&reportPageDialog.isShowing()))return;
        LinearLayout body=column();body.setPadding(dp(24),dp(12),dp(24),0);
        sub(body,"1〜"+reportPages+"のページ番号を入力してください。");
        EditText input=new EditText(this);input.setTag("report_page_input");input.setContentDescription("移動先のページ番号");
        input.setTextSize(18);input.setSingleLine(true);input.setGravity(Gravity.CENTER);
        input.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);input.setFilters(new InputFilter[]{new InputFilter.LengthFilter(19)});
        input.setText(draft==null?Long.toString(reportPage+1):draft);input.selectAll();body.addView(input,new LinearLayout.LayoutParams(-1,dp(54)));
        AlertDialog dialog=new AlertDialog.Builder(this).setTitle("ページを指定").setView(body).setNegativeButton("キャンセル",null).setPositiveButton("移動",null).create();
        reportPageDialog=dialog;reportPageInput=input;
        dialog.setOnDismissListener(d->{reportPageDialog=null;reportPageInput=null;});
        dialog.setOnShowListener(d->{
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->{
                long requested=ReportCatalog.requestedPage(input.getText().toString(),reportPages);
                if(requested<0){input.setError("1〜"+reportPages+"の範囲で入力してください");return;}
                reportPage=requested;dialog.dismiss();refreshCatalog(true);
            });
            input.requestFocus();dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
        });dialog.show();
    }
    private void addReportPager(LinearLayout parent,ReportCatalog.Page result,String position){
        final int version=renderVersion;
        ReportPagerView pager=new ReportPagerView(this,result.index()+1,result.pages(),
            ()->{if(version!=renderVersion)return;reportPage=Math.max(0,reportPage-1);refreshCatalog(true);},
            ()->{if(version==renderVersion)showPageJump(null);},
            ()->{if(version!=renderVersion)return;reportPage=Math.min(result.pages()-1,reportPage+1);refreshCatalog(true);});
        pager.setTag("report_pager_"+position);parent.addView(pager,new LinearLayout.LayoutParams(-1,-2));
    }
    private Spinner catalogSpinner(LinearLayout parent,String label,String description,List<String> labels,int selected){
        parent.addView(text(label,13,GREEN,true),new LinearLayout.LayoutParams(dp(54),-2));Spinner spinner=new Spinner(this);spinner.setContentDescription(description);
        ArrayAdapter<String> adapter=new ArrayAdapter<>(this,android.R.layout.simple_spinner_item,labels);adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);spinner.setSelection(selected);parent.addView(spinner,new LinearLayout.LayoutParams(0,dp(48),1));return spinner;
    }
    private void reviewHub(){
        final int version=renderVersion;
        LinearLayout hub=card(content);hub.setTag("paid_review_hub");hub.addView(text("💰️ 継続レビュー",22,INK,true));space(hub,5);
        sub(hub,"AIが作成した有料レポート · 保存済みを読むだけなら追加料金なし");space(hub,14);
        List<Store.Habit> habits=store.habits();List<Long> existing=new ArrayList<>();for(Store.Habit h:habits)existing.add(h.id());
        if(reportTask!=0&&!existing.contains(reportTask)){reportTask=0;reportPage=0;}
        ZoneId zone=ZoneId.systemDefault();List<Integer> years,availableMonths;ReportCatalog.Page result;
        try(ReportStore db=new ReportStore(this)){
            years=db.catalogYears(existing,reportTask,zone);
            if(reportYear!=0&&!years.contains(reportYear)){reportYear=0;reportPage=0;}
            availableMonths=db.catalogMonths(existing,reportTask,reportYear,zone);
            if(reportMonth!=0&&!availableMonths.contains(reportMonth)){reportMonth=0;reportPage=0;}
            result=db.catalogPage(existing,reportTask,reportYear,reportMonth,reportPage,zone);
        }
        reportPage=result.index();reportPages=result.pages();
        LinearLayout taskBox=row(),yearBox=row(),monthBox=row();hub.addView(taskBox);space(hub,4);hub.addView(yearBox);space(hub,4);hub.addView(monthBox);
        List<Long> taskIds=new ArrayList<>();List<String> taskLabels=new ArrayList<>();taskIds.add(0L);taskLabels.add("すべてのタスク");
        for(Store.Habit h:habits){taskIds.add(h.id());taskLabels.add(h.goal());}
        Spinner tasks=catalogSpinner(taskBox,"タスク","継続レビューのタスク",taskLabels,taskIds.indexOf(reportTask));
        List<Integer> yearKeys=new ArrayList<>();List<String> yearLabels=new ArrayList<>();yearKeys.add(0);yearLabels.add("すべての年");
        for(int year:years){yearKeys.add(year);yearLabels.add(year==-1?"生成年不明（旧）":year+"年");}
        Spinner yearChoice=catalogSpinner(yearBox,"生成年","継続レビューの生成年",yearLabels,yearKeys.indexOf(reportYear));yearChoice.setEnabled(!years.isEmpty());
        List<Integer> monthKeys=new ArrayList<>();List<String> months=new ArrayList<>();monthKeys.add(0);
        months.add(reportYear==-1?"月は不明です":reportYear>0?(availableMonths.isEmpty()?"レポートのある月はありません":"すべての月"):"年を選んでください");
        for(int month:availableMonths){monthKeys.add(month);months.add(month+"月");}
        Spinner monthChoice=catalogSpinner(monthBox,"月","継続レビューの生成月",months,monthKeys.indexOf(reportMonth));boolean monthsEnabled=reportYear>0&&!availableMonths.isEmpty();monthChoice.setEnabled(monthsEnabled);monthChoice.setAlpha(monthsEnabled?1f:.5f);
        tasks.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){public void onNothingSelected(AdapterView<?> p){}public void onItemSelected(AdapterView<?> p,View v,int at,long id){if(version!=renderVersion)return;long next=taskIds.get(at);if(reportTask!=next){reportTask=next;reportPage=0;refreshCatalog(true);}}});
        yearChoice.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){public void onNothingSelected(AdapterView<?> p){}public void onItemSelected(AdapterView<?> p,View v,int at,long id){if(version!=renderVersion)return;int next=yearKeys.get(at);if(reportYear!=next){reportYear=next;reportMonth=0;reportPage=0;refreshCatalog(true);}}});
        monthChoice.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){public void onNothingSelected(AdapterView<?> p){}public void onItemSelected(AdapterView<?> p,View v,int at,long id){if(version!=renderVersion)return;int next=monthKeys.get(at);if(reportMonth!=next){reportMonth=next;reportPage=0;refreshCatalog(true);}}});
        space(hub,8);
        if(result.total()==0){
            sub(hub,reportTask==0&&reportYear==0?"生成完了したレポートはまだありません。":"この条件に合うレポートはありません。");
            space(hub,6);sub(hub,"生成が完了すると、タスクと年・月でここから選べます。");
        }else{
            sub(hub,result.total()+"件 · 新しい順"+(result.pages()>1?" · "+result.first()+"〜"+result.last()+"件":""));space(hub,8);
            if(result.pages()>1){addReportPager(hub,result,"top");space(hub,12);}
            for(ReportCatalog.Entry entry:result.entries()){
                LinearLayout item=row();item.setTag("saved_review_"+entry.id());item.setPadding(dp(12),dp(12),dp(12),dp(12));item.setBackground(shape(PALE,14));item.setMinimumHeight(dp(74));
                LinearLayout labels=column();labels.addView(text(entry.goal(),17,INK,true));space(labels,5);labels.addView(text(ReportCatalog.generated(entry.completedAt(),zone),12,MUTED,false));
                item.addView(labels,new LinearLayout.LayoutParams(0,-2,1));TextView read=text("読む ›",16,GREEN,true);LinearLayout.LayoutParams rp=new LinearLayout.LayoutParams(-2,-2);rp.leftMargin=dp(8);item.addView(read,rp);
                item.setContentDescription("継続レビューを読む："+entry.goal()+"、"+ReportCatalog.generated(entry.completedAt(),zone));item.setFocusable(true);item.setClickable(true);
                item.setOnClickListener(v->startActivity(ReportActivity.intent(this,entry.habit(),entry.id())));hub.addView(item,new LinearLayout.LayoutParams(-1,-2));space(hub,8);
            }
            if(result.pages()>1){space(hub,4);addReportPager(hub,result,"bottom");}
        }
        if(reportTask!=0||reportYear!=0||reportMonth!=0||reportPage!=0){space(hub,8);hub.addView(button("最新を表示",false,()->{reportTask=0;reportYear=0;reportMonth=0;reportPage=0;refreshCatalog(true);}));}
        space(hub,10);hub.addView(button("生成状況・新規作成",false,()->{
            if(habits.isEmpty()){Toast.makeText(this,"先にタスクを作成してください",Toast.LENGTH_SHORT).show();return;}
            if(reportTask!=0){openReport(reportTask);return;}
            if(habits.size()==1){openReport(habits.get(0).id());return;}
            String[] names=new String[habits.size()];for(int at=0;at<names.length;at++)names[at]=habits.get(at).goal();
            new AlertDialog.Builder(this).setTitle("タスクを選ぶ").setItems(names,(d,at)->openReport(habits.get(at).id())).setNegativeButton("戻る",null).show();
        }));space(hub,8);hub.addView(button("継続レビューの共通設定",false,this::openReportSettings));
    }

    private HistorySelection historySelection(Store.Habit habit){
        HistorySelection selected=historySelections.get(habit.id());if(selected!=null)return selected;
        SharedPreferences prefs=getSharedPreferences("routia_history",MODE_PRIVATE);
        int period=prefs.getInt("period_"+habit.id(),1);if(period<0||period>2)period=1;
        LocalDate anchor=LocalDate.now();
        try{anchor=LocalDate.parse(prefs.getString("anchor_"+habit.id(),anchor.toString()));}catch(java.time.DateTimeException ignored){}
        if(anchor.isAfter(LocalDate.now()))anchor=LocalDate.now();
        selected=new HistorySelection(period,anchor);historySelections.put(habit.id(),selected);return selected;
    }
    private void historyChanged(Store.Habit habit,HistorySelection selected){
        getSharedPreferences("routia_history",MODE_PRIVATE).edit().putInt("period_"+habit.id(),selected.period).putString("anchor_"+habit.id(),selected.anchor.toString()).apply();
        int y=content.getParent() instanceof ScrollView?((ScrollView)content.getParent()).getScrollY():0;
        render();if(content.getParent() instanceof ScrollView){ScrollView sc=(ScrollView)content.getParent();Ui.restoreScroll(sc,y);}
    }
    private void historyControls(LinearLayout parent,Store.Habit habit,HistorySelection selected){
        TextView label=text("📅 この目標の表示期間",13,GREEN,true);parent.addView(label);
        Spinner spinner=new Spinner(this);spinner.setContentDescription("表示期間："+habit.goal());
        spinner.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,new String[]{"週","月","年"}));spinner.setSelection(selected.period);
        parent.addView(spinner,new LinearLayout.LayoutParams(-1,dp(48)));
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){
            public void onNothingSelected(AdapterView<?> p){}
            public void onItemSelected(AdapterView<?> p,View v,int at,long id){if(selected.period!=at){selected.period=at;historyChanged(habit,selected);}}
        });
        Progress.Range range=Progress.range(selected.period,selected.anchor,habit.created(),displayedDay);
        String unit=new String[]{"週","月","年"}[selected.period];LinearLayout arrows=row();
        TextView previous=button("前の"+unit,false,()->{
            selected.anchor=selected.period==0?selected.anchor.minusWeeks(1):selected.period==1?selected.anchor.minusMonths(1):selected.anchor.minusYears(1);
            historyChanged(habit,selected);
        });previous.setContentDescription("前の"+unit+"："+habit.goal());previous.setEnabled(range.start().isAfter(habit.created()));previous.setAlpha(previous.isEnabled()?1:.4f);arrows.addView(previous,new LinearLayout.LayoutParams(0,-2,1));
        TextView next=button("次の"+unit,false,()->{
            selected.anchor=selected.period==0?selected.anchor.plusWeeks(1):selected.period==1?selected.anchor.plusMonths(1):selected.anchor.plusYears(1);
            historyChanged(habit,selected);
        });next.setContentDescription("次の"+unit+"："+habit.goal());next.setEnabled(range.end().isBefore(displayedDay));next.setAlpha(next.isEnabled()?1:.4f);
        LinearLayout.LayoutParams params=new LinearLayout.LayoutParams(0,-2,1);params.leftMargin=dp(10);arrows.addView(next,params);parent.addView(arrows);space(parent,8);
    }
    void openReport(long habit){startActivity(ReportActivity.intent(this,habit));}
    private void openReportSettings(){startActivity(ReportSettingsActivity.intent(this));}
    private void contributionCalendar(LinearLayout parent,Store.Habit h,Store.Metric metric,LocalDate selectedStart,LocalDate end,int period){
        boolean check=metric.kind().equals("check");Progress.Range bounds=Progress.calendarBounds(period,new Progress.Range(selectedStart,end));LocalDate calendarStart=bounds.start(),calendarEnd=bounds.end();int count=(int)java.time.temporal.ChronoUnit.DAYS.between(calendarStart,calendarEnd)+1;int[] shades=new int[count];double[] amounts=new double[count];Arrays.fill(amounts,-1);
        Set<LocalDate> frozen=store.ticketDays(h.id());Map<LocalDate,Store.Entry> records=new HashMap<>();double scale=Math.max(1,metric.target());for(Store.Entry e:store.entries(h.id(),selectedStart,end)){records.put(e.day(),e);if(e.amount()!=null&&e.unit().equals(metric.unit()))scale=Math.max(scale,e.amount());}
        for(int index=0;index<count;index++){LocalDate day=calendarStart.plusDays(index);if(day.isBefore(h.created())||day.isAfter(end)){shades[index]=-1;continue;}Store.Entry e=records.get(day);Double amount=e!=null&&e.unit().equals(metric.unit())?e.amount():null;shades[index]=e==null&&frozen.contains(day)?GrassView.TICKET:Progress.contribution(check,e!=null,amount,scale,e==null?0:e.level());if(amount!=null)amounts[index]=amount;}
        LinearLayout calendarHead=row();calendarHead.addView(text("🌿 活動カレンダー",19,INK,true),new LinearLayout.LayoutParams(0,-2,1));calendarHead.addView(infoChip("見方 ⓘ","成果の見方を開く",()->progressHelp(check)));parent.addView(calendarHead);space(parent,8);String heading=period==1?calendarStart.format(DateTimeFormatter.ofPattern("yyyy年M月")):period==2?calendarStart.getYear()+"年":calendarStart+" 〜 "+calendarEnd;parent.addView(text(heading,16,GREEN,true));space(parent,8);
        GrassView grass=new GrassView(this,calendarStart,calendarEnd,shades,amounts,!check,metric.unit());grass.onDayClick=day->{Store.Entry e=store.entry(h.id(),day);String body=day.isAfter(end)?"この日はまだ記録できません（未来の日付）。":day.isBefore(h.created())?"習慣を始める前の日です。":e.level()==0?(frozen.contains(day)?"🎟 継続チケットを自動で1枚使用しました。\n連続記録を守ったお休みの日です。達成日数は増えず、達成率も変わりません。":"この日の記録はまだありません。"):Rules.grade(e.level())+(e.amount()!=null?"\n"+Ui.number(e.amount())+e.unit():"")+(e.note().isEmpty()?"":"\n\n"+e.note());Ui.show(Ui.sheet(this,h.goal(),day.toString(),body,"確認した",null),this);};
        if(period<=1){int cell=Math.max(32,Math.min(48,(int)((getResources().getDisplayMetrics().widthPixels-dp(84))/getResources().getDisplayMetrics().density/7)));grass.compact(period==0?1:2,cell);parent.addView(grass,new LinearLayout.LayoutParams(grass.width(),grass.height()));}
        else{LinearLayout grid=row();int gridHeight=grass.height();grid.addView(new GrassView.WeekLabels(this),new LinearLayout.LayoutParams(dp(24),gridHeight));HorizontalScrollView scroll=new HorizontalScrollView(this);scroll.setFillViewport(true);scroll.setHorizontalScrollBarEnabled(true);grass.setMinimumWidth(grass.width());scroll.addView(grass,new android.widget.FrameLayout.LayoutParams(grass.width(),gridHeight));grid.addView(scroll,new LinearLayout.LayoutParams(0,gridHeight,1));parent.addView(grid);scroll.post(()->scroll.fullScroll(View.FOCUS_RIGHT));}
        int[] calendarColors=GrassView.palette(this);space(parent,12);LinearLayout legend=row();View empty=new View(this);empty.setBackground(shape(calendarColors[0],3));legend.addView(empty,new LinearLayout.LayoutParams(dp(12),dp(12)));legend.addView(text(" 未記録  ",11,MUTED,false));int[] colors=check?new int[]{calendarColors[1],calendarColors[2],calendarColors[4]}:Arrays.copyOfRange(calendarColors,1,5);String[] labels={" ミニ  "," 目標  "," 挑戦"};for(int at=0;at<colors.length;at++){View square=new View(this);square.setBackground(shape(colors[at],3));LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(dp(12),dp(12));lp.rightMargin=dp(3);legend.addView(square,lp);if(check)legend.addView(text(labels[at],11,MUTED,false));}if(!check)legend.addView(text(" 少 → 多",11,MUTED,false));parent.addView(legend);space(parent,8);LinearLayout ticketLegend=row();View ticketSquare=new View(this);ticketSquare.setBackground(shape(calendarColors[GrassView.TICKET],3));ticketLegend.addView(ticketSquare,new LinearLayout.LayoutParams(dp(12),dp(12)));ticketLegend.addView(text(" チケット",11,MUTED,false));parent.addView(ticketLegend);space(parent,10);
        if(period==2)sub(parent,"← 横にスワイプ");
    }
    private TextView infoChip(String title,String description,Runnable action){
        TextView chip=button(title,false,action);chip.setTextSize(13);chip.setMinHeight(dp(44));chip.setPadding(dp(10),dp(7),dp(10),dp(7));chip.setContentDescription(description);return chip;
    }
    private void ticketInfo(Store.Habit h,LocalDate start,LocalDate end){
        Set<LocalDate> done=achieved(h),usedDays=store.ticketDays(h.id());LocalDate first=done.stream().min(LocalDate::compareTo).orElse(null);
        int remaining=TicketRules.MONTHLY_LIMIT-store.ticketsUsed(h.id(),java.time.YearMonth.from(LocalDate.now()));
        long used=usedDays.stream().filter(day->!day.isBefore(start)&&!day.isAfter(end)).count();
        String status=first==null?"利用前：まだ達成記録がありません。":first.equals(LocalDate.now())?"明日から利用可能です。":"利用可能です。最初の達成："+first.format(DateTimeFormatter.ofPattern("M/d"));
        String body="今月の残り："+remaining+" / 2枚\nこの期間の自動使用："+used+"枚\n\n"+status+"\n\n1日達成した翌日以降、各習慣で月2枚使えます。未達が確定すると、確認なしで自動使用します。休んだ日は日数を増やさず、連続記録をつなぎます。\n\n赤いマスがチケットを使用した日です。未使用分は繰り越しません。達成数や達成率、寄付コミットの未達判定は変わりません。";
        Ui.show(Ui.sheet(this,h.goal(),"🎟 継続チケット",body,"わかった",null),this);
    }
    private void progressHelp(boolean check){
        String body="1マスは1日。タップで詳細を開けます。年表示は横にスワイプでき、前年・翌年で切り替えられます。背景になじむマスは未記録、さらに淡いマスは登録前・未来の日です。\n\n"+(check?"緑はミニ・目標・チャレンジ、の3段階です。":"緑の段階で時間・数量を表します（凡例の左から右へ少→多）。")+"赤いマスはチケットを使ったお休みです。\n\nグラフは日別の記録です。"+(check?"未記録の日は0として表示します。":"金色の点線は目標。数値がない日は線をつなぎません。")+"\n\n🌱 まずは66日継続を目指そう。日数は目安で、習慣になる早さには個人差があります。";
        Ui.show(Ui.sheet(this,"","成果の見方",body,"わかった",null),this);
    }
    private void widgetColors(){backgroundColors(true);}
    private void backgroundColors(boolean widget){
        LinearLayout form=column();sub(form,widget?"すべてのウィジェットに適用します。":"アプリだけに適用します。ウィジェットの色は別に設定できます。");space(form,14);FrameLayout preview=new FrameLayout(this);preview.setTag(widget?"widgetColorPreview":"appColorPreview");form.addView(preview,new LinearLayout.LayoutParams(-1,-2));space(form,18);
        int[] chosen={widget?WidgetColors.selected(this):AppColors.selected(this)};RadioButton[] cards=new RadioButton[WidgetColors.NAMES.length];RadioButton[] system={null};Runnable[] refresh={null};
        refresh[0]=()->{preview.removeAllViews();preview.addView(widget?WidgetColors.preview(this,chosen[0]):AppColors.preview(this,chosen[0]));preview.setContentDescription((widget?"色のプレビュー：":"アプリのプレビュー：")+AppColors.name(chosen[0]));for(int at=0;at<cards.length;at++)if(cards[at]!=null){boolean active=at==chosen[0];cards[at].setChecked(active);android.graphics.drawable.GradientDrawable bg=shape(active?PALE:Color.WHITE,12);bg.setStroke(dp(active?2:1),active?GREEN:Ui.surface(this,0xffC7D3C8));cards[at].setBackground(bg);}if(system[0]!=null){system[0].setChecked(chosen[0]<0);android.graphics.drawable.GradientDrawable bg=shape(chosen[0]<0?PALE:Color.WHITE,12);bg.setStroke(dp(chosen[0]<0?2:1),chosen[0]<0?GREEN:Ui.surface(this,0xffC7D3C8));system[0].setBackground(bg);}};
        if(!widget){system[0]=Ui.choice(this,"端末に合わせる","明るい／暗い設定に追従 · 初期設定");system[0].setContentDescription("アプリ背景色：端末に合わせる");system[0].setOnClickListener(v->{chosen[0]=-1;refresh[0].run();});form.addView(system[0]);space(form,12);}
        for(int row=0;row<(WidgetColors.NAMES.length+2)/3;row++){LinearLayout options=row();for(int col=0;col<3;col++){int index=row*3+col;if(index>=WidgetColors.NAMES.length){options.addView(new View(this),new LinearLayout.LayoutParams(0,dp(88),1));continue;}RadioButton card=new RadioButton(this);card.setText(WidgetColors.NAMES[index]);card.setContentDescription((widget?"ウィジェット背景色：":"アプリ背景色：")+WidgetColors.NAMES[index]);card.setTextSize(13);card.setTextColor(INK);card.setGravity(Gravity.CENTER);card.setSingleLine(false);card.setMaxLines(2);card.setButtonDrawable((android.graphics.drawable.Drawable)null);card.setPadding(dp(4),dp(9),dp(4),dp(7));card.setCompoundDrawablePadding(dp(5));android.graphics.drawable.GradientDrawable swatch=Ui.fixedBg(this,WidgetColors.COLORS[index],28);swatch.setStroke(dp(1),0xff91A69A);swatch.setBounds(0,0,dp(26),dp(26));card.setCompoundDrawables(null,swatch,null,null);if(Build.VERSION.SDK_INT>=29)card.setForceDarkAllowed(false);card.setOnClickListener(v->{chosen[0]=index;refresh[0].run();});cards[index]=card;LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,dp(88),1);if(col>0)p.leftMargin=dp(8);options.addView(card,p);}form.addView(options);space(form,10);}
        refresh[0].run();Dialog[] dialog={null};dialog[0]=formDialog("",widget?"ウィジェットの背景色":"アプリの背景色",form,"この色で保存",()->{if(widget)WidgetColors.save(this,chosen[0]);else AppColors.save(this,chosen[0]);dialog[0].dismiss();if(widget)render();else recreate();Toast.makeText(this,"✅ 背景色を保存しました",Toast.LENGTH_SHORT).show();});Ui.show(dialog[0],this);
    }
    private String versionLabel(){try{return "Routia "+getPackageManager().getPackageInfo(getPackageName(),0).versionName+" · リリース前";}catch(android.content.pm.PackageManager.NameNotFoundException e){return "Routia · リリース前";}}
    private void metricTile(LinearLayout row,String label,String value){LinearLayout l=column();l.addView(text(value,17,GREEN,true));l.addView(text(label,11,MUTED,false));row.addView(l,new LinearLayout.LayoutParams(0,-2,1));}
    private void settingSwitch(LinearLayout l,String title,String key,boolean initial){Switch s=new Switch(this);s.setText(title);s.setTextColor(INK);s.setMinHeight(dp(54));s.setChecked(Feedback.prefs(this).getBoolean(key,initial));s.setOnCheckedChangeListener((v,on)->Feedback.prefs(this).edit().putBoolean(key,on).apply());l.addView(s);}
    private List<Store.Habit> selectedReportHabits(){
        List<Store.Habit> selected=new ArrayList<>();for(Store.Habit h:store.habits())if(ReportSettings.habitEnabled(this,h.id()))selected.add(h);return selected;
    }
    private void refreshReviewFeature(){if(reviewFeatureRefresh!=null)reviewFeatureRefresh.run();}
    private void reviewFeatureControls(LinearLayout parent){
        Switch master=new Switch(this);master.setTag("review_feature_switch");master.setText("継続レビューを使う");master.setTextSize(16);master.setTextColor(INK);master.setMinHeight(dp(56));parent.addView(master);
        TextView status=text("",13,MUTED,false);status.setTag("review_feature_status");status.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);parent.addView(status);space(parent,10);
        boolean[] updating={false};
        Runnable sync=()->{boolean on=ReportSettings.featureEnabled(this);updating[0]=true;master.setChecked(on);updating[0]=false;status.setText(on?"全体オン · 選択した目標でレビューを生成します。":"全体オフ · 新しいレビューの生成は停止中です。設定と保存済みレビューは残ります。");};
        reviewFeatureRefresh=sync;sync.run();
        master.setOnCheckedChangeListener((v,on)->{
            if(updating[0])return;
            if(!on){try{ReportSettings.setFeatureEnabled(this,false);}catch(RuntimeException e){Toast.makeText(this,"設定の保存に失敗しました。状態を確認してください",Toast.LENGTH_LONG).show();}refreshReviewFeature();return;}
            updating[0]=true;master.setChecked(false);updating[0]=false;
            if(reviewFeatureDialog!=null&&reviewFeatureDialog.isShowing())return;
            ReportBudget.Options approved=ReportSettings.options(this);List<Store.Habit> targets=selectedReportHabits();List<String> names=new ArrayList<>();for(Store.Habit h:targets)names.add(h.goal());
            reviewFeatureDialog=ReportConsent.enable(this,approved,names,()->{
                synchronized(ReportSettings.class){
                if(!approved.equals(ReportSettings.options(this))||!targets.equals(selectedReportHabits())){Toast.makeText(this,"設定が変わりました。もう一度料金を確認してください",Toast.LENGTH_LONG).show();refreshReviewFeature();return;}
                try{ReportSettings.setFeatureEnabled(this,true);}catch(RuntimeException e){Toast.makeText(this,"設定を保存できませんでした",Toast.LENGTH_LONG).show();}refreshReviewFeature();
                }
            });
            reviewFeatureDialog.setOnDismissListener(d->{reviewFeatureDialog=null;refreshReviewFeature();});
        });
    }
    private void settings(){
        header("MAKE IT YOURS","続けやすさを、整える。","通知も記録も、続けやすい形に。\n通常の記録は、この端末の中に。");
        LinearLayout appearance=card(content);appearance.addView(text("🎨 アプリの見た目",20,INK,true));space(appearance,14);appearance.addView(button("アプリの背景色 · "+AppColors.name(AppColors.selected(this)),false,()->backgroundColors(false)));
        LinearLayout notification=card(content);notification.addView(text("🔔 習慣のリマインダー",20,INK,true));space(notification,12);int reminderCount=Reminders.times(this).length;notification.addView(text(reminderCount==0?"通知オフ":"1日 "+reminderCount+"回",21,GREEN,true));space(notification,6);sub(notification,Reminders.timesLabel(this));space(notification,10);sub(notification,"未達成の習慣をまとめてお知らせ。すべて達成したら、その日の通知はお休みです。");space(notification,18);notification.addView(button("回数・時刻を変更",true,this::notificationTimes));space(notification,12);notification.addView(button(Reminders.allowed(this)?"通知の許可・端末設定":"通知を許可する",false,this::notificationPermission));space(notification,14);sub(notification,Reminders.exact(this)?"指定時刻のアラームが許可されています。":"省電力制御などにより、通知が遅れる場合があります。");
        if(Build.VERSION.SDK_INT>=31&&!Reminders.exact(this)){space(notification,12);notification.addView(button("アラームとリマインダーを許可",false,()->{try{startActivity(new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,Uri.parse("package:"+getPackageName())));}catch(ActivityNotFoundException e){Toast.makeText(this,"端末の設定から許可してください",Toast.LENGTH_LONG).show();}}));}
        LinearLayout feedback=card(content);feedback.addView(text("🎉 達成したとき",20,INK,true));settingSwitch(feedback,"📳 振動","haptics",true);settingSwitch(feedback,"🔊 短い効果音","sound",false);sub(feedback,"紙吹雪・達成カードで、その一歩をお祝いします。音や振動は端末設定に従います。");
        LinearLayout analysis=card(content);analysis.addView(text("💰️ 継続レビュー",20,INK,true));space(analysis,10);
        sub(analysis,"未達が続いたらAIの利用料がかかる。その出費を戒めとして、次に続けるための短いレポートを作ります。");
        space(analysis,12);reviewFeatureControls(analysis);ReportBudget.Options reportOptions=ReportSettings.options(this);
        long reportTargets=store.habits().stream().filter(h->ReportSettings.habitEnabled(this,h.id())).count();
        sub(analysis,"選択済み："+reportTargets+"目標 · "+ReportBudget.model(reportOptions.model()).label()+"\n上限"+reportOptions.limitYen()+"円 / 回 · API実費のみ");
        space(analysis,14);analysis.addView(button("継続レビューの共通設定",true,()->openReportSettings()));
        space(analysis,8);sub(analysis,"APIキーはここから1回だけ設定。対象は目標ごとに選べます。全体オフでは、共通設定を保存しても生成しません。送信済みの処理は取り消せません。");
        LinearLayout learn=card(content);learn.addView(text("🧠 ちょっとだけ、習慣の科学。",20,INK,true));space(learn,12);for(int i=0;i<Learn.TIPS.length;i++){final int at=i;learn.addView(button(Learn.TIPS[i][0],false,()->Learn.show(this,at)));space(learn,7);}
        LinearLayout widget=card(content);widget.addView(text("📱 ホーム画面のウィジェット",20,INK,true));space(widget,10);sub(widget,"「記録」は1タップで達成。「成果」は連続日数・累計日数と週間の折れ線を確認できます。");space(widget,18);widget.addView(button("背景色を選ぶ · "+WidgetColors.NAMES[WidgetColors.selected(this)],false,this::widgetColors));space(widget,12);widget.addView(button("記録ウィジェットを追加",false,()->WidgetSetup.show(this)));space(widget,12);widget.addView(button("成果ウィジェットを追加",false,()->WidgetSetup.showProgress(this)));
        LinearLayout about=card(content);about.addView(text(versionLabel(),18,INK,true));space(about,12);about.addView(button("Routiaの相棒、ルティについて",false,()->Mascot.intro(this)));space(about,10);sub(about,"広告なし。通常の記録は端末内。AIレポートを使う場合のみ、対象の記録をOpenAIへ送ります。アンインストール・データ消去で失われます。\n\n集中タイマーはAndroidの画面固定を使います。通常のホーム操作は抑えられますが、OSで解除可能です。管理端末化や完全なロックは行いません。\n\n「目標数の上限10個」「ミニ目標」「2日連続・週2日」は設計上のルールです。効果の保証や医学的な基準ではありません。");
    }
    private void notificationTimes(){showReminderTimes(Reminders.times(this),0,null,-1);}
    private void showReminderTimes(int[] draft,int scrollY,Bundle pickerState,int pickerSlot){
        if(reminderDialog!=null&&reminderDialog.isShowing())return;reminderDraft=draft.clone();
        LinearLayout form=column();sub(form,"1日0〜10回、未達成の習慣をまとめて通知します。0回で通知オフ。時刻は早い順に保存し、重複する時刻は選べません。");space(form,16);
        form.addView(text("1日の通知回数",16,INK,true));space(form,8);Spinner count=new Spinner(this);count.setTag("reminder_count");count.setContentDescription("1日のリマインダー回数");count.setMinimumHeight(dp(52));
        String[] labels=new String[ReminderTimes.MAX+1];for(int n=0;n<labels.length;n++)labels[n]=n==0?"0回（通知しない）":n+"回 / 日";
        ArrayAdapter<String> adapter=new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,labels);count.setAdapter(adapter);count.setSelection(reminderDraft.length);form.addView(count,new LinearLayout.LayoutParams(-1,-2));space(form,16);
        LinearLayout slots=column();form.addView(slots);
        reminderRowsRefresh=()->{if(reminderDialog!=null){reminderDialog.problem=null;reminderDialog.problemField=null;reminderDialog.refreshNotice();}slots.removeAllViews();if(reminderDraft.length==0){sub(slots,"通知は停止します。回数を増やすと、時刻を設定できます。");space(slots,16);return;}for(int at=0;at<reminderDraft.length;at++){final int index=at;TextView time=button(getString(R.string.reminder_time_edit,at+1,ReminderTimes.format(reminderDraft[at])),false,()->showReminderTimePicker(index,null));time.setTag("reminder_time_"+at);slots.addView(time);space(slots,10);}};
        reminderRowsRefresh.run();count.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener(){public void onNothingSelected(android.widget.AdapterView<?> p){}public void onItemSelected(android.widget.AdapterView<?> p,View v,int position,long id){if(reminderDraft==null||position==reminderDraft.length)return;reminderDraft=ReminderTimes.resize(reminderDraft,position);reminderRowsRefresh.run();}});
        space(form,8);sub(form,"初期設定：5回 · 12:00 / 15:00 / 18:00 / 20:00 / 22:00");space(form,12);form.addView(button("初期の回数・時刻に戻す",false,()->{reminderDraft=ReminderTimes.defaults();count.setSelection(reminderDraft.length);reminderRowsRefresh.run();}));space(form,16);sub(form,"後半ほど少し強めに、最後は当日の残り時間をお知らせします。全習慣を達成した日は通知しません。当日すでに通知した回は、設定を変えても再通知しません。");
        reminderDialog=(Ui.FormDialog)formDialog("","リマインダーの回数・時刻",form,"回数・時刻を保存",()->{Reminders.saveTimes(this,reminderDraft);reminderDialog.dismiss();render();Toast.makeText(this,"リマインダーを保存しました",Toast.LENGTH_LONG).show();});
        reminderDialog.desiredHeight=Math.min((int)(getResources().getDisplayMetrics().heightPixels*.90),dp(760));
        reminderDialog.setOnDismissListener(d->{if(reminderTimePicker!=null)reminderTimePicker.dismiss();reminderDialog=null;reminderDraft=null;reminderRowsRefresh=null;});
        Ui.show(reminderDialog,this);Ui.restoreScroll(reminderDialog.formScroll,scrollY);
        if(pickerState!=null&&pickerSlot>=0&&pickerSlot<reminderDraft.length)reminderDialog.getWindow().getDecorView().post(()->showReminderTimePicker(pickerSlot,pickerState));
    }
    private void showReminderTimePicker(int index,Bundle state){
        if(reminderDialog==null||!reminderDialog.isShowing()||index<0||index>=reminderDraft.length)return;
        reminderPickerSlot=index;TimePickerDialog picker=new TimePickerDialog(this,(v,hour,minute)->{if(reminderDraft!=null&&index<reminderDraft.length){reminderDraft[index]=hour*60+minute;reminderRowsRefresh.run();}},reminderDraft[index]/60,reminderDraft[index]%60,true);
        reminderTimePicker=picker;picker.setTitle((index+1)+"回目の通知時刻");picker.setOnDismissListener(d->{if(reminderTimePicker==picker){reminderTimePicker=null;reminderPickerSlot=-1;}});picker.show();if(state!=null)picker.onRestoreInstanceState(state);
    }
    private void notificationPermission(){if(Build.VERSION.SDK_INT>=33&&checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED&&!Feedback.prefs(this).getBoolean("notificationAsked",false)){Feedback.prefs(this).edit().putBoolean("notificationAsked",true).apply();requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS},200);return;}try{startActivity(new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE,getPackageName()));}catch(ActivityNotFoundException e){startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,Uri.parse("package:"+getPackageName())));}}
}
