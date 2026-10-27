package app.routia;

import android.app.*;
import android.content.*;
import android.graphics.Color;
import android.net.Uri;
import android.os.*;
import android.text.InputType;
import android.view.*;
import android.widget.*;
import java.time.*;
import java.util.*;
import org.json.*;

/** Four short, separately readable tabs. No generated HTML, scripts, or remote imagery. */
public final class ReportActivity extends ReportScreenActivity {
    private static final int TITLE_SP=18, BODY_SP=16, LABEL_SP=13;
    private Store store;private Store.Habit habit;private ReportStore reports;
    private LinearLayout root,body,tabs;private ScrollView scroll;private int tab;
    private long selected=-1;private String renderState="";
    private JSONObject facts,result;
    AlertDialog consentDialog;
    private final Handler handler=new Handler(Looper.getMainLooper());
    private final Runnable poll=new Runnable(){public void run(){if(reports!=null){ReportStore.Item item=selected<0?null:reports.item(selected);String state=item==null?"":item.state();if(!renderState.equals(state))render();}handler.postDelayed(this,1500);}};
    static Intent intent(Context c,long habit){return new Intent(c,ReportActivity.class).putExtra("habit",habit);}
    static Intent intent(Context c,long habit,long report){return intent(c,habit).putExtra("report",report);}
    private void selectRequested(Intent intent){
        long id=intent.getLongExtra("report",-1);ReportStore.Item item=id<0?null:reports.item(id);
        if(item!=null&&item.habit()==habit.id())selected=id;
    }
    private int restoreY=-1;
    @Override public void onCreate(Bundle state){
        super.onCreate(state);store=Store.get(this);habit=store.habit(getIntent().getLongExtra("habit",-1));
        if(habit==null){finish();return;}
        reports=new ReportStore(this);ReportRunner.initialize(this);
        if(state!=null){tab=state.getInt("tab");selected=state.getLong("report",-1);restoreY=state.getInt("scroll");}
        if(state==null)selectRequested(getIntent());
        if(selected<0&&!reports.all(habit.id()).isEmpty())selected=reports.all(habit.id()).get(0).id();
        root=screenRoot();render();
    }
    @Override public void onNewIntent(Intent intent){
        super.onNewIntent(intent);setIntent(intent);
        long id=intent.getLongExtra("habit",-1);
        if(habit!=null&&habit.id()==id){
            if(intent.hasExtra("report")){selectRequested(intent);tab=0;restoreY=0;render();}
            return;
        }
        habit=store.habit(id);if(habit==null){finish();return;}selected=-1;tab=0;restoreY=0;
        if(!reports.all(id).isEmpty())selected=reports.all(id).get(0).id();selectRequested(intent);render();
    }
    @Override public void onSaveInstanceState(Bundle state){
        super.onSaveInstanceState(state);state.putInt("tab",tab);state.putLong("report",selected);state.putInt("scroll",scroll==null?0:scroll.getScrollY());
    }
    @Override public void onResume(){
        super.onResume();if(reports==null)return;
        int y=restoreY>=0?restoreY:scroll.getScrollY();restoreY=-1;render();Ui.restoreScroll(scroll,y);handler.postDelayed(poll,1500);
    }
    @Override public void onPause(){if(scroll!=null)restoreY=scroll.getScrollY();handler.removeCallbacks(poll);super.onPause();}
    @Override protected void onDestroy(){
        handler.removeCallbacksAndMessages(null);if(consentDialog!=null)consentDialog.dismiss();
        if(reports!=null)reports.close();super.onDestroy();
    }
    LinearLayout row(){LinearLayout l=new LinearLayout(activity);l.setGravity(Gravity.CENTER_VERTICAL);return l;}
    void text(LinearLayout parent,String s,int size,int color,boolean bold){parent.addView(Ui.text(activity,s,size,color,bold));}
    void gap(LinearLayout l,int n){Ui.gap(l,n);}
    LinearLayout card(String title){return screenCard(body,title);}
    void paragraph(LinearLayout p,String s){text(p,s,14,Ui.MUTED,false);}
    void render(){
        root.removeAllViews();ReportStore.Item item=selected<0?null:reports.item(selected);renderState=item==null?"":item.state();result=null;
        try{facts=item==null?ReportData.snapshot(store,habit,LocalDate.now(),reports):new JSONObject(item.snapshot());if(item!=null&&item.state().equals("complete")){result=new JSONObject(item.result());ReportData.validate(result,item.detail().isEmpty()?ReportDetail.STANDARD:ReportDetail.fromId(item.detail()));}}
        catch(JSONException e){facts=new JSONObject();result=null;}
        LinearLayout head=screenHeader(item==null?habit.goal():item.goal());gap(head,5);
        text(head,result==null?"AIのアドバイス：未生成":"AIのアドバイス：生成済み · "+facts.optString("as_of")+" の記録",12,Ui.MUTED,false);
        if(item!=null&&item.state().equals("complete"))text(head,"生成日："+ReportCatalog.generated(item.completedAt(),ZoneId.systemDefault()),12,Ui.MUTED,false);
        root.addView(head);
        tabs=row();tabs.setPadding(dp(16),0,dp(16),dp(10));
        String[] names={"要点","傾向","改善策","根拠"};
        for(int at=0;at<4;at++){final int index=at;TextView b=Ui.button(activity,names[at],at==tab,()->{tab=index;render();});b.setTextSize(14);b.setPadding(dp(5),dp(11),dp(5),dp(11));b.setMinHeight(dp(48));b.setSelected(tab==at);b.setContentDescription(names[at]+(tab==at?"、選択中":"タブ"));LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,-2,1);p.setMargins(dp(3),0,dp(3),0);tabs.addView(b,p);}root.addView(tabs);
        scroll=new ScrollView(activity);body=Ui.col(activity);body.setPadding(dp(20),dp(5),dp(20),dp(24));scroll.addView(body);root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        if(tab==0)overview(item);else if(tab==1)trends();else if(tab==2)actions();else evidence();
        LinearLayout footer=screenFooter();
        footer.addView(Ui.button(activity,"継続レビューの共通設定",false,this::configure));gap(footer,8);
        addBack(footer);root.addView(footer);
        root.requestApplyInsets();
    }
    LinearLayout aiCard(String title){
        LinearLayout c=card(title);text(c,"🤖 AIが生成したアドバイス",LABEL_SP,Ui.GREEN,true);gap(c,8);return c;
    }
    void aiEmpty(){
        LinearLayout c=card("AIのアドバイスはまだありません");
        paragraph(c,"生成すると、あなたの記録をもとにした分析と、次回試す具体策がここに表示されます。");
    }
    void overview(ReportStore.Item item){
        if(result!=null){
            LinearLayout lead=aiCard("今回のアドバイス");text(lead,result.optString("headline"),BODY_SP,Ui.INK,false);
            JSONArray items=result.optJSONArray("actions");
            if(items!=null&&items.length()>0){
                JSONObject first=items.optJSONObject(0);gap(lead,14);text(lead,"次の1回で試すこと",LABEL_SP,Ui.GREEN,true);
                gap(lead,5);text(lead,first.optString("step"),BODY_SP,Ui.INK,false);gap(lead,12);
                lead.addView(Ui.button(activity,"提案の理由・やり方を見る",false,()->{tab=2;render();}));
                if(!first.has("why")){gap(lead,8);paragraph(lead,"表示中は以前の形式のレポートです。改善後の分析は次回の生成から反映されます。");}
            }
        }else aiEmpty();
        reviewControl();
        if(result==null&&item!=null){
            LinearLayout c=card("生成の状態");
            paragraph(c,item.state().equals("queued")||item.state().equals("counting")||item.state().equals("sending")?"生成しています。画面を閉じても処理は続きます。":item.state().equals("complete")?"保存済みレポートを開けませんでした。":item.problem());
            if(item.reserved()>0){gap(c,8);paragraph(c,item.estimated()>0?"利用料の計算目安："+Ui.number(item.estimated())+"円":"費用の確保枠："+Ui.number(item.reserved())+"円相当。実際の利用料は未確認です。");}
        }
        LinearLayout foot=card("生成と設定");
        if(item!=null&&item.state().equals("complete")){
            paragraph(foot,"このレポートの費用目安："+Ui.number(item.estimated())+"円相当");
            gap(foot,6);paragraph(foot,item.model().isEmpty()?"旧レポート：生成時のモデル・上限は未保存です。":"生成時："+ReportBudget.model(item.model()).label()+" · 上限"+item.budget()+"円");
            gap(foot,6);paragraph(foot,item.detail().isEmpty()?"レポートの詳しさ：未保存（以前のレポート）":"レポートの詳しさ："+ReportDetail.fromId(item.detail()).label);
        }else paragraph(foot,"記録の日数とグラフは「傾向」タブで確認できます。AIの分析とは分けて表示します。");

        List<ReportStore.Item> historyItems=reports.all(habit.id());
        if(historyItems.size()>1){gap(foot,10);foot.addView(Ui.button(activity,"過去のレポート",false,()->{
            String[] titles=new String[historyItems.size()];for(int i=0;i<titles.length;i++)titles[i]=(historyItems.get(i).state().equals("complete")?ReportCatalog.generated(historyItems.get(i).completedAt(),ZoneId.systemDefault()):"未完成")+" · "+historyItems.get(i).week()+" の週";
            new AlertDialog.Builder(activity).setTitle("過去のレポート").setItems(titles,(d,which)->{selected=historyItems.get(which).id();render();}).setNegativeButton("閉じる",null).show();
        }));}
    }
    void reviewControl(){
        ReportStore.Item current=null;String week=Rules.week(LocalDate.now()).toString();
        for(ReportStore.Item r:reports.all(habit.id()))if(r.week().equals(week)){current=r;break;}
        final ReportStore.Item job=current;
        LinearLayout controls=card("💰️ 今週のレビュー");
        ReportBudget.Options options=ReportSettings.options(activity);
        paragraph(controls,ReportBudget.model(options.model()).label()+" · 1回の上限 "+options.limitYen()+"円相当 · "+options.detail().label);
        paragraph(controls,"金額の目安（仮条件）："+ReportBudget.referenceLabel(options.model(),options.detail())+"。実測前の試算です。");
        gap(controls,8);
        if(job!=null&&(job.state().equals("complete")||job.reserved()>0||job.state().equals("queued")||job.state().equals("counting")||job.state().equals("sending"))){
            paragraph(controls,job.state().equals("complete")?"今週分は作成済みです。追加の有料生成は行いません。":job.state().equals("stopped")?"送信結果を確認できず停止しています。費用が発生した可能性があるため再送しません。":"今週分の処理を準備・実行中です。");
            gap(controls,12);controls.addView(Ui.button(activity,job.state().equals("complete")?"今週のレビューを見る":"処理状況を見る",true,()->{selected=job.id();render();}));
        }else{
            if(!ReportSettings.featureEnabled(activity)){
                paragraph(controls,"継続レビュー全体がオフです。新しいレビューは生成しません。設定タブからオンにできます。");gap(controls,12);
                controls.addView(Ui.button(activity,"設定タブを開く",false,()->startActivity(MainActivity.settingsIntent(activity))));return;
            }
            String reason=!ReportSettings.configured(activity)?(ReportSettings.habitEnabled(activity,habit.id())?"APIキーが未設定です。共通設定で登録してください。":"APIキーを登録し、この目標をオンにしてください。"):!ReportSettings.habitEnabled(activity,habit.id())?"この目標はオフです。共通設定でオンにしてください。":"未達を待たずに、手動で1回試せます。API実費が発生し、この目標の今週1回分に数えます。";
            paragraph(controls,reason);gap(controls,12);
            controls.addView(Ui.button(activity,"今週のレポートを作る（有料）",true,()->{
                if(!ReportSettings.active(activity,habit.id())){if(!ReportSettings.featureEnabled(activity))startActivity(MainActivity.settingsIntent(activity));else configure();return;}
                generate();
            }));
        }

    }
    void tile(LinearLayout row,String title,String value){
        LinearLayout c=Ui.col(activity);text(c,title,12,Ui.MUTED,false);gap(c,5);TextView t=Ui.text(activity,value,22,Ui.GREEN,true);t.setSingleLine(true);t.setAutoSizeTextTypeUniformWithConfiguration(12,22,1,android.util.TypedValue.COMPLEX_UNIT_SP);c.addView(t);row.addView(c,new LinearLayout.LayoutParams(0,-2,1));
    }
    void trends(){
        if(result==null)aiEmpty();
        else{
            JSONArray a=result.optJSONArray("insights");if(a!=null)for(int i=0;i<a.length();i++){
                JSONObject x=a.optJSONObject(i);LinearLayout insight=aiCard(x.optString("title"));
                text(insight,"記録の解釈・原因は仮説",11,Ui.MUTED,false);gap(insight,5);text(insight,x.optString("detail"),BODY_SP,Ui.INK,false);
            }
        }
        LinearLayout stats=card("📊 記録からの集計");LinearLayout tiles=row();
        tile(tiles,"今の連続",facts.optInt("streak")+"日");tile(tiles,"これまで",facts.optInt("total")+"日");tile(tiles,"最長",facts.optInt("best")+"日");stats.addView(tiles);
        gap(stats,8);paragraph(stats,"集計対象："+facts.optString("as_of")+"まで。"+(selected<0?"現在の記録です。":"レビュー作成時点の記録です。以降の追記・修正は反映されません。"));
        LinearLayout c=card("📊 曜日ごとの実記録");paragraph(c,"対象日までの直近28日。達成済みの当日も含みます。");gap(c,8);
        c.addView(new ReportVisuals(activity,facts,ReportVisuals.WEEKDAY),new LinearLayout.LayoutParams(-1,dp(210)));
        paragraph(c,"達成日 / 対象日。ミニ達成も含み、チケットは加算しません。");
        ReportChartData data=ReportVisuals.chartData(facts);
        if(data!=null){
            LinearLayout history=card(data.numeric?"📈 取り組み量の推移":"📈 累計達成日数の推移");
            paragraph(history,"直近28日 · "+shortDate(data.start.toString())+"〜"+shortDate(data.end.toString())+"の記録");
            if(data.numeric){
                gap(history,8);text(history,"合計 "+Ui.number(data.amountTotal)+data.unit+" · 量の記録 "+data.measured+"日",BODY_SP,Ui.INK,true);
            }
            boolean drawable=data.numeric?data.measured>0:facts.optInt("total")>0;
            if(drawable){gap(history,8);history.addView(new ReportVisuals(activity,facts,ReportVisuals.SERIES),new LinearLayout.LayoutParams(-1,dp(160)));}
            gap(history,8);
            paragraph(history,data.numeric?data.measured>0?"数値を記録した日の実施量です。未記録はゼロにせず、線をつなぎません。":"実施量の数値がまだ記録されていません。":drawable?"実際に達成した日だけ累計が増えます。チケットは加算しません。":"達成の記録が増えると推移を表示します。");
        }
        JSONArray runs=facts.optJSONArray("runs");if(runs!=null&&runs.length()>0){
            LinearLayout periods=card("📊 記録が続いた期間");for(int i=Math.max(0,runs.length()-3);i<runs.length();i++){
                JSONObject run=runs.optJSONObject(i);if(run==null)continue;
                text(periods,shortDate(run.optString("first"))+" → "+shortDate(run.optString("last")),14,Ui.INK,true);
                paragraph(periods,run.optInt("achieved")+"日達成 · "+(run.optBoolean("ended")?"その後に未記録日あり":"現在の期間"));gap(periods,10);
            }paragraph(periods,"実際にやめた理由や時刻を示すものではありません。");
        }
    }
    void actions(){
        if(result==null){aiEmpty();return;}
        JSONArray items=result.optJSONArray("actions");if(items==null)return;
        for(int i=0;i<items.length();i++){
            JSONObject x=items.optJSONObject(i);LinearLayout c=aiCard((i+1)+". "+x.optString("title"));
            if(x.has("why")){text(c,"あなたにこの提案をする理由",LABEL_SP,Ui.GREEN,true);gap(c,4);text(c,x.optString("why"),BODY_SP,Ui.INK,false);gap(c,14);}
            String context=x.optString("cue");
            if(!context.isEmpty()){
                flowBox(c,"試す場面",context,Ui.PALE);
                c.addView(new ReportVisuals(activity,facts,ReportVisuals.ARROW),new LinearLayout.LayoutParams(-1,dp(24)));
            }
            flowBox(c,"次の1回で変えること",x.optString("step"),Ui.BG);
            c.addView(new ReportVisuals(activity,facts,ReportVisuals.ARROW),new LinearLayout.LayoutParams(-1,dp(24)));
            flowBox(c,"試したあとに確かめること",x.optString("effect"),Ui.PALE);
        }
    }
    void flowBox(LinearLayout c,String label,String value,int color){
        LinearLayout box=Ui.col(activity);box.setPadding(dp(12),dp(10),dp(12),dp(10));box.setBackground(Ui.bg(activity,color,12));
        text(box,label,LABEL_SP,Ui.GREEN,true);gap(box,4);text(box,value,BODY_SP,Ui.INK,false);c.addView(box,new LinearLayout.LayoutParams(-1,-2));
    }
    void evidence(){
        if(result==null){aiEmpty();return;}
        if(result!=null){
            JSONArray lenses=result.optJSONArray("lenses");if(lenses!=null)for(int i=0;i<lenses.length();i++){
                JSONObject x=lenses.optJSONObject(i);LinearLayout c=aiCard(x.optString("name"));text(c,x.optString("detail"),BODY_SP,Ui.INK,false);gap(c,8);
                String source=x.optString("source_id");boolean known=false;
                for(String[] s:ReportData.SOURCES)if(s[0].equals(source)){known=true;text(c,"研究を本人の記録に当てはめた仮説",11,Ui.GREEN,true);gap(c,8);c.addView(Ui.button(activity,s[2]+" ↗",false,()->openSource(s[3])));break;}
                if(!known)text(c,"研究での裏付けが未確認の提案",11,Ui.MUTED,false);
            }
        }
        LinearLayout limits=card("この分析で分からないこと");
        JSONArray uncertainty=result==null?facts.optJSONArray("limitations"):result.optJSONArray("limits");
        if(uncertainty!=null)for(int i=0;i<Math.min(3,uncertainty.length());i++){paragraph(limits,"・"+uncertainty.optString(i));gap(limits,8);}
        LinearLayout basis=card("分析の見方");paragraph(basis,"日数とグラフは記録から計算。原因や改善策はAIの仮説です。改善策を試したあとの記録で、本人に合うか確かめます。");
        gap(basis,10);paragraph(basis,"参照資料は、習慣形成・実行計画・進捗記録の研究。出典リンクから内容を確認できます。");
    }
    String shortDate(String day){try{LocalDate d=LocalDate.parse(day);return d.getMonthValue()+"/"+d.getDayOfMonth();}catch(Exception e){return day;}}
    void openSource(String url){try{activity.startActivity(new Intent(Intent.ACTION_VIEW,Uri.parse(url)));}catch(ActivityNotFoundException e){Toast.makeText(activity,"リンクを開けるアプリがありません",Toast.LENGTH_LONG).show();}}
    void generate(){
        if(consentDialog!=null&&consentDialog.isShowing())return;
        if(!ReportSettings.active(activity,habit.id())){if(!ReportSettings.featureEnabled(activity))startActivity(MainActivity.settingsIntent(activity));else configure();return;}
        ReportBudget.Options approved=ReportSettings.options(activity);
        consentDialog=ReportConsent.manual(activity,habit.goal(),approved,()->{
            try{
                selected=ReportRunner.enqueue(activity,habit,approved);ReportRunner.work(activity,null);render();
            }catch(JSONException|IllegalArgumentException e){
                Toast.makeText(activity,"対象目標・設定・今週の生成状況を確認してください",Toast.LENGTH_LONG).show();render();
            }
        });
        consentDialog.setOnDismissListener(d->consentDialog=null);
    }
    void configure(){startActivity(ReportSettingsActivity.intent(this));}
}
