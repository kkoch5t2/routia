package app.routia;

import android.app.*;
import android.content.*;
import android.graphics.Color;
import android.os.*;
import android.text.InputType;
import android.view.*;
import android.widget.*;
import java.time.*;
import java.util.*;

/** One shared form for credentials and spending; target switches are independent. */
public final class ReportSettingsActivity extends ReportScreenActivity {
    private LinearLayout body;
    private ScrollView scroll;
    AlertDialog consentDialog;
    private RadioGroup models,details;
    private SeekBar budget;
    private EditText keyInput;
    private TextView amount,preview,error,weeklyAmount,weeklyFormula,weeklyFooter,noticeStatus,featureStatus;
    private final Map<Long,Switch> targets=new LinkedHashMap<>();
    static Intent intent(Context c){return new Intent(c,ReportSettingsActivity.class);}
    @Override public void onCreate(Bundle state){
        super.onCreate(state);getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);SensitiveActions.protectWindow(getWindow());build();
        if(state!=null){
            String draftModel=state.getString("model",ReportSettings.options(this).model());
            for(int at=0;at<models.getChildCount();at++){RadioButton radio=(RadioButton)models.getChildAt(at);if(draftModel.equals(radio.getTag()))models.check(radio.getId());}
            String draftDetail=state.getString("detail",ReportSettings.options(this).detail().id);
            for(int at=0;at<details.getChildCount();at++){RadioButton radio=(RadioButton)details.getChildAt(at);if(draftDetail.equals(radio.getTag()))details.check(radio.getId());}
            budget.setProgress(state.getInt("budget",budget.getProgress()));
            for(Map.Entry<Long,Switch> target:targets.entrySet())target.getValue().setChecked(state.getBoolean("target_"+target.getKey(),target.getValue().isChecked()));
            refresh();Ui.restoreScroll(scroll,state.getInt("scroll"));
        }
    }
    @Override public void onSaveInstanceState(Bundle state){
        super.onSaveInstanceState(state);state.putString("model",selected().model());state.putString("detail",selected().detail().id);state.putInt("budget",budget.getProgress());state.putInt("scroll",scroll.getScrollY());
        for(Map.Entry<Long,Switch> target:targets.entrySet())state.putBoolean("target_"+target.getKey(),target.getValue().isChecked());
        // Unsaved credentials deliberately never enter saved instance state.
    }
    private void build(){
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        LinearLayout root=screenRoot();root.addView(screenHeader("共通設定"));
        scroll=new ScrollView(activity);body=Ui.col(activity);body.setPadding(dp(20),dp(5),dp(20),dp(24));scroll.addView(body);root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        LinearLayout purpose=card("未達の出費を、自分への戒めに。");
        text(purpose,"「やらなければ、お金がかかる」を続ける動機にする機能です。",16,Ui.GREEN,true);gap(purpose,8);
        note(purpose,"未達が続いたときのAPI利用料を戒めとし、その出費で次に続けるための分析レポートを作ります。");

        gap(purpose,10);featureStatus=text(purpose,"",14,Ui.GREEN,true);featureStatus.setTag("review_feature_status");
        note(purpose,"全体のオン・オフは、設定タブの「継続レビューを使う」から切り替えます。オフの間は設定だけを保存できます。");

        LinearLayout week=card("💴 1週間分のレビュー予算");
        weeklyAmount=text(week,"",28,Ui.GREEN,true);gap(week,8);
        weeklyFormula=text(week,"",15,Ui.INK,true);gap(week,10);
        note(week,"現在の設定を変えずに1週分作成した場合の上限目安です。固定の週額請求ではありません。生成が翌週になれば、請求される週もずれます。");

        ReportBudget.Options current=ReportSettings.options(activity);
        LinearLayout modelBox=card("🤖 1. 使うAIを選ぶ");models=new RadioGroup(activity);models.setContentDescription("使用するAIモデル");
        for(ReportBudget.Model m:ReportBudget.MODELS){
            RadioButton r=Ui.choice(activity,m.label(),m.detail());r.setTag(m.id());models.addView(r);
            if(m.id().equals(current.model()))models.check(r.getId());
        }modelBox.addView(models);gap(modelBox,8);
        note(modelBox,"高いモデルほど、同じ利用量で費用が増えます。金額の目安は簡潔〜詳細すべてを含む、仮条件での試算です。詳しさ別の金額は「レポートの詳しさ」で確認できます。実測値ではなく、この幅を超える場合があります。");

        LinearLayout limit=card("💴 2. 1回の上限を決める");
        amount=text(limit,"",30,Ui.GREEN,true);
        budget=new SeekBar(activity);budget.setContentDescription("1回の上限金額");budget.setMax((ReportBudget.MAX_YEN-ReportBudget.MIN_YEN)/ReportBudget.STEP_YEN);budget.setProgress((current.limitYen()-ReportBudget.MIN_YEN)/ReportBudget.STEP_YEN);limit.addView(budget,new LinearLayout.LayoutParams(-1,dp(52)));
        note(limit,"100円 ← 50円刻み → 1,000円");gap(limit,10);
        preview=text(limit,"",14,Ui.INK,true);gap(limit,10);
        note(limit,"上限は固定の罰金ではありません。APIの実費だけが発生します。上限だけを上げても、必ず出費が増えるわけではありません。");gap(limit,8);
        note(limit,"出費の大きさを変えたい場合はモデルも選んでください。上限を使い切るための追加生成は行いません。");

        LinearLayout detailBox=card("3. レポートの詳しさ");
        note(detailBox,"分析や改善策を、どこまで詳しくまとめるか選べます。金額の目安は仮条件での試算です。");gap(detailBox,8);
        details=new RadioGroup(activity);details.setContentDescription("レポートの詳しさ");
        for(ReportDetail d:ReportDetail.values()){
            RadioButton r=Ui.choice(activity,d.label,d.description+"\n目安："+d.range+"字");r.setTag(d.id);details.addView(r);
            if(d==current.detail())details.check(r.getId());
        }detailBox.addView(details);gap(detailBox,8);
        note(detailBox,"文字数は目安です。記録が少ないときは短くまとめます。詳しさによって利用料は変わりますが、設定した金額上限の範囲で生成します。");

        LinearLayout habits=card("🎯 4. 使う目標を選ぶ");
        note(habits,"オンにした目標だけが対象。APIキー・モデル・上限・詳しさはすべての目標で共通です。");gap(habits,8);
        for(Store.Habit h:Store.get(activity).habits()){
            Switch s=new Switch(activity);s.setText(h.goal());s.setTextColor(Ui.ink(activity,Ui.INK));s.setTextSize(16);s.setMinHeight(dp(56));s.setChecked(ReportSettings.habitEnabled(activity,h.id()));targets.put(h.id(),s);habits.addView(s);
            habits.addView(Ui.button(activity,"保存してレビューを開く",false,()->requestSave(()->{startActivity(ReportActivity.intent(this,h.id()).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_SINGLE_TOP));finish();})));gap(habits,12);
        }
        if(targets.isEmpty())note(habits,"目標を作ると、ここで対象を選べます。");
        gap(habits,8);note(habits,"オンにした翌日から判定。2日連続未達、または月〜日の週に2日未達になると、その翌日以降に自動生成します。例：月・火が未達 → 水曜0時以降。アプリ起動時やOSの定期処理で実行するため、通信・省電力で遅れる場合があります。ミニ達成も成功、各目標で週1回まで。過去の週はまとめて生成せず、送信済みの処理は取り消せません。");

        LinearLayout notices=card("自動生成の完了通知");
        note(notices,"継続レビューの自動生成が完了したときにお知らせします。生成当日は「今日」からも開けます。通知を押しても新たな有料生成は始まりません。");
        gap(notices,8);noticeStatus=text(notices,"",14,Ui.GREEN,true);
        notices.addView(Ui.button(activity,"完了通知の設定",false,()->ReportNotices.permission(this)));
        LinearLayout connection=card("🔑 5. OpenAIに接続");
        text(connection,ReportSettings.configured(activity)?"接続キー：保存済み":"接続キー：未設定",14,Ui.GREEN,true);gap(connection,8);
        keyInput=new EditText(activity);keyInput.setSingleLine(true);keyInput.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_PASSWORD);keyInput.setHint(ReportSettings.configured(activity)?"変更するときだけ貼り付け":"APIキーを貼り付け");keyInput.setTextColor(Ui.ink(activity,Ui.INK));keyInput.setSaveEnabled(false);SensitiveActions.protect(keyInput);connection.addView(keyInput,new LinearLayout.LayoutParams(-1,dp(56)));gap(connection,8);
        note(connection,"この入力欄1か所だけで設定します。キー未設定では通信・有料生成は行いません。利用料はOpenAIから請求されます。");
        if(ReportSettings.configured(activity)){gap(connection,12);connection.addView(Ui.button(activity,"接続キーを削除",false,()->SensitiveActions.protect(new AlertDialog.Builder(activity).setTitle("接続キーを削除する？").setMessage("送信前の生成を止めます。送信済みの処理は取り消せません。対象目標の選択とレポートは残ります。").setPositiveButton("削除",(d,w)->{
            try{ReportSettings.deleteKey(activity);finish();}
            catch(RuntimeException failure){Toast.makeText(activity,"キーを削除できませんでした。もう一度お試しください。",Toast.LENGTH_LONG).show();}
        }).setNegativeButton("戻る",null).show())));}

        LinearLayout info=card("ℹ️ 金額とデータについて");
        note(info,"金額の目安は、入力4,000〜10,000・内部推論1,000〜5,000トークンを仮定した試算です。本文とJSONの出力は、簡潔500〜1,000・標準1,000〜2,000・詳細2,500〜4,500トークンで計算しています。");gap(info,8);
        note(info,"通常の入力料金で計算し、キャッシュによる割引や書き込み費用は含めていません。1ドル200円・税や手数料等30%の余裕を含みます。これらはアプリの試算条件で、現在の為替や請求額を示すものではありません。");gap(info,8);
        note(info,"実際の利用量は未計測です。記録量や内部推論、キャッシュの扱いで金額は変わり、目安の幅を外れることがあります。目安の上端は請求の上限ではありません。設定した金額上限内で必要な処理枠を確保できなければ、有料生成前に停止します。");gap(info,8);
        note(info,"習慣名・目標・きっかけ・達成日・数値を送信。メモは送りません。省電力や通信状態で自動生成が遅れる場合があります。");
        error=Ui.text(activity,"",13,Ui.RED,false);
        LinearLayout foot=screenFooter();weeklyFooter=Ui.text(activity,"",16,Ui.GREEN,true);foot.addView(weeklyFooter);gap(foot,6);foot.addView(error);foot.addView(Ui.button(activity,"この設定を保存",true,this::save));gap(foot,12);addBack(foot);root.addView(foot);
        models.setOnCheckedChangeListener((g,id)->refresh());details.setOnCheckedChangeListener((g,id)->refresh());budget.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){public void onProgressChanged(SeekBar s,int p,boolean u){refresh();}public void onStartTrackingTouch(SeekBar s){}public void onStopTrackingTouch(SeekBar s){}});
        for(Switch s:targets.values())s.setOnCheckedChangeListener((v,on)->refresh());
        error.setVisibility(View.GONE);error.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE|WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
        refresh();root.requestApplyInsets();
    }
    private ReportBudget.Options selected(){
        RadioButton r=models.findViewById(models.getCheckedRadioButtonId());
        RadioButton d=details.findViewById(details.getCheckedRadioButtonId());
        return new ReportBudget.Options(r.getTag().toString(),ReportBudget.MIN_YEN+budget.getProgress()*ReportBudget.STEP_YEN,ReportDetail.fromId(d.getTag().toString()));
    }
    private void refresh(){
        featureStatus.setText(ReportSettings.featureEnabled(this)?"継続レビュー：全体オン":"継続レビュー：全体オフ（生成は停止中）");
        ReportBudget.Options o=selected();amount.setText(String.format(java.util.Locale.JAPAN,"上限 %,d円 / 回",o.limitYen()));
        for(int at=0;at<models.getChildCount();at++){
            RadioButton radio=(RadioButton)models.getChildAt(at);ReportBudget.Model m=ReportBudget.model(radio.getTag().toString());
            radio.setText(getString(R.string.choice_detail,m.label(),m.detail()+"\n金額の目安："+ReportBudget.allDepthLabel(m.id())));
        }
        for(int at=0;at<details.getChildCount();at++){
            RadioButton radio=(RadioButton)details.getChildAt(at);ReportDetail d=ReportDetail.fromId(radio.getTag().toString());
            radio.setText(getString(R.string.choice_detail,d.label,d.description+"\n目安："+d.range+"字\n金額の目安："+ReportBudget.referenceLabel(o.model(),d)));
        }
        long count=targets.values().stream().filter(Switch::isChecked).count();
        String total=String.format(java.util.Locale.JAPAN,"%,d",count*o.limitYen());
        weeklyAmount.setText("最大 "+total+"円相当");
        weeklyFormula.setText("1回 "+o.limitYen()+"円 × "+count+"目標 × 週1回");
        weeklyFooter.setText("1週間分の予算："+total+"円相当（"+count+"目標）");
        try{
            ReportBudget.Reservation r=ReportBudget.reserve(o,ReportBudget.INPUT_LIMIT,LocalDate.now());
            preview.setText("金額の目安（仮条件）："+ReportBudget.referenceLabel(o.model(),o.detail())
                +"\n処理枠をすべて使った場合：約"+r.reservedYen().setScale(0,java.math.RoundingMode.CEILING).toPlainString()+"円相当");
        }catch(IllegalArgumentException e){preview.setText(e.getMessage());}
        if(Build.VERSION.SDK_INT>=30)budget.setStateDescription(o.limitYen()+"円");
    }
    private void save(){requestSave(this::finish);}
    private void requestSave(Runnable after){
        if(consentDialog!=null&&consentDialog.isShowing())return;
        ReportBudget.Options options=selected();
        Map<Long,Boolean> enabled=new LinkedHashMap<>();List<String> goals=new ArrayList<>();
        for(Map.Entry<Long,Switch> e:targets.entrySet()){
            enabled.put(e.getKey(),e.getValue().isChecked());
            if(e.getValue().isChecked())goals.add(e.getValue().getText().toString());
        }
        if(!ReportSettings.featureEnabled(this)||goals.isEmpty()){persist(options,enabled,after,false);return;}
        consentDialog=ReportConsent.settings(activity,options,goals,()->persist(options,enabled,after,true));
        consentDialog.setOnDismissListener(d->consentDialog=null);
    }
    private void persist(ReportBudget.Options options,Map<Long,Boolean> enabled,Runnable after,boolean feesConfirmed){
        try{
            synchronized(ReportSettings.class){
                if(ReportSettings.featureEnabled(this)&&enabled.containsValue(true)&&!feesConfirmed)
                    throw new IllegalArgumentException("機能がオンに変わりました。もう一度保存して料金を確認してください");
                String value=keyInput.getText().toString().trim();
                if(!value.isEmpty())ReportSettings.saveKey(activity,value);
                ReportSettings.saveOptions(activity,options,enabled);
            }
            keyInput.setText("");Toast.makeText(activity,"設定を保存しました",Toast.LENGTH_SHORT).show();after.run();
        }catch(Exception e){
            android.util.Log.e("RoutiaReports","Settings save failed: "+e.getClass().getSimpleName());
            error.setText(e instanceof IllegalArgumentException?e.getMessage():"保存処理でエラーが発生しました。もう一度保存してください。");error.setVisibility(View.VISIBLE);
        }
    }
    @Override protected void onResume(){
        super.onResume();if(featureStatus!=null)refresh();if(noticeStatus!=null)noticeStatus.setText(ReportNotices.allowed(this)?"完了通知：有効":"完了通知：通知の許可または設定が必要です");
        ReportNotices.deliverPending(this);
    }
    @Override protected void onStop(){
        if(consentDialog!=null)consentDialog.dismiss();
        keyInput.setText("");super.onStop();
    }
    private LinearLayout card(String title){return screenCard(body,title);}
    private TextView text(LinearLayout p,String s,int n,int color,boolean bold){TextView t=Ui.text(activity,s,n,color,bold);p.addView(t);return t;}
    private void note(LinearLayout p,String s){text(p,s,14,Ui.MUTED,false);}
    private void gap(LinearLayout p,int n){Ui.gap(p,n);}
}
