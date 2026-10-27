package app.routia;
import android.app.*;
import android.content.*;
import android.graphics.Color;
import android.net.Uri;
import android.os.*;
import android.view.*;
import android.view.inputmethod.*;
import android.widget.*;
import java.time.LocalDate;
/** A single numeric field, reached directly by app, widget, or notification. */
public final class RecordActivity extends Activity {
    static final String OPEN="app.routia.OPEN_AMOUNT";
    Store store;Store.Habit habit;Store.Metric metric;LocalDate day;EditText amount,note;TextView save;FrameLayout root;LinearLayout sheetShell,inputBody,inputFooter;boolean saving;
    static Intent intent(Context c,long id){return new Intent(c,RecordActivity.class).setAction(OPEN).setData(Uri.parse("routia://amount/"+id+"/"+LocalDate.now())).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK);}
    @Override public void onCreate(Bundle state){
        int color=AppColors.selected(this);setTheme(color==6?R.style.RecordSheetBlack:AppColors.night(this)?R.style.RecordSheetDark:R.style.RecordSheetLight);
        super.onCreate(state);store=Store.get(this);Intent intent=getIntent();long id=-1;day=LocalDate.now();
        if(WidgetClickData.ACTION.equals(intent.getAction())){
            var click=WidgetClickData.parse(intent.getAction(),intent.getDataString(),intent.getLongExtra("habit",-1),intent.getIntExtra("level",0),intent.getStringExtra("shownDay"),day,widget->WidgetCollections.ownedRecordWidget(this,widget));
            if(click.isEmpty()){HabitWidget.update(this);Toast.makeText(this,"日付が変わりました。更新後のウィジェットで記録してください。",Toast.LENGTH_SHORT).show();finish();return;}
            id=click.get().habit();habit=store.habit(id);if(habit==null){finish();return;}
            if(store.metric(id).kind().equals("check")){
                if(state==null){Store.Result result=store.record(id,day,click.get().level(),false);HabitWidget.update(this);Reminders.refreshVisible(this);if(result.changed()){Feedback.deliver(this,null,result.recovery(),Rules.feedback(result.recovery(),result.level()));if(result.level()>=Rules.FULL){setContentView(new FrameLayout(this));Celebration.show(this,habit,result.level(),result.recovery(),()->{startActivity(new Intent(this,MainActivity.class).setAction(ProgressWidget.OPEN).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_SINGLE_TOP));finish();},this::finish);return;}}}
                finish();overridePendingTransition(0,0);return;
            }
        }else{
            try{Uri uri=intent.getData();if(!OPEN.equals(intent.getAction())||uri==null||!"routia".equals(uri.getScheme())||!"amount".equals(uri.getHost())||uri.getPathSegments().size()!=2)throw new IllegalArgumentException();id=Long.parseLong(uri.getPathSegments().get(0));if(!day.equals(LocalDate.parse(uri.getPathSegments().get(1))))throw new IllegalArgumentException();}
            catch(RuntimeException e){HabitWidget.update(this);finish();return;}
        }
        habit=store.habit(id);metric=store.metric(id);if(habit==null||metric.kind().equals("check")){finish();return;}
        Store.Entry entry=store.entry(id,day);getWindow().setBackgroundDrawableResource(android.R.color.transparent);getWindow().addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);getWindow().setDimAmount(.48f);getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        root=new FrameLayout(this);if(Build.VERSION.SDK_INT>=30){getWindow().setDecorFitsSystemWindows(false);root.setOnApplyWindowInsetsListener((v,insets)->{android.graphics.Insets b=insets.getInsets(WindowInsets.Type.systemBars()|WindowInsets.Type.displayCutout()|WindowInsets.Type.ime());v.setPadding(b.left,b.top,b.right,b.bottom);v.post(this::fitSheet);return insets;});}else root.setFitsSystemWindows(true);
        root.setOnClickListener(v->finish());LinearLayout sheet=Ui.col(this);sheet.setPadding(Ui.dp(this,24),Ui.dp(this,18),Ui.dp(this,24),Ui.dp(this,22));sheet.setBackground(Ui.bg(this,Color.WHITE,28));sheet.setOnClickListener(v->{});
        LinearLayout top=new LinearLayout(this);top.setGravity(Gravity.CENTER_VERTICAL);Mascot ruti=new Mascot(this,Mascot.AVATAR);LinearLayout.LayoutParams companion=new LinearLayout.LayoutParams(Ui.dp(this,30),Ui.dp(this,34));companion.rightMargin=Ui.dp(this,8);top.addView(ruti,companion);TextView title=Ui.text(this,habit.goal(),21,Ui.INK,true);title.setMaxLines(2);top.addView(title,new LinearLayout.LayoutParams(0,-2,1));TextView close=Ui.text(this,"閉じる",13,Ui.MUTED,false);close.setGravity(Gravity.CENTER);close.setMinWidth(Ui.dp(this,58));close.setMinHeight(Ui.dp(this,48));close.setOnClickListener(v->finish());top.addView(close);sheet.addView(top);Ui.gap(sheet,12);
        sheet.addView(Ui.text(this,"今日の合計（"+metric.unit()+"）",13,Ui.MUTED,false));Ui.gap(sheet,4);
        amount=new EditText(this);amount.setId(R.id.amount_input);amount.setTextSize(18);amount.setTextColor(Ui.ink(this,Ui.INK));amount.setHintTextColor(Ui.ink(this,Ui.MUTED));amount.setHint("0");amount.setSingleLine(true);amount.setInputType(android.text.InputType.TYPE_CLASS_NUMBER|android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);amount.setImeOptions(EditorInfo.IME_ACTION_DONE);amount.setFilters(new android.text.InputFilter[]{new android.text.InputFilter.LengthFilter(12)});amount.setText(state!=null?state.getString("amount",""):entry.amount()==null?"":Ui.number(entry.amount()));amount.setSelectAllOnFocus(true);sheet.addView(amount,new LinearLayout.LayoutParams(-1,Ui.dp(this,52)));
        Ui.gap(sheet,7);sheet.addView(Ui.text(this,metric.kind().equals("time")?"タイマーの分数を含めた、今日の合計を保存します。":"追加分ではなく、今日の合計を保存します。",12,Ui.MUTED,false));Ui.gap(sheet,12);
        TextView memo=Ui.text(this,entry.note().isEmpty()?"＋ メモを追加（任意）":"メモを編集",13,Ui.GREEN,true);memo.setMinHeight(Ui.dp(this,42));memo.setGravity(Gravity.CENTER_VERTICAL);sheet.addView(memo);
        note=new EditText(this);note.setId(R.id.amount_note);note.setTextColor(Ui.ink(this,Ui.INK));note.setHintTextColor(Ui.ink(this,Ui.MUTED));note.setHint("やったこと・気づき");note.setText(state!=null?state.getString("note",entry.note()):entry.note());note.setInputType(android.text.InputType.TYPE_CLASS_TEXT|android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE);note.setMaxLines(3);note.setFilters(new android.text.InputFilter[]{new android.text.InputFilter.LengthFilter(1000)});note.setVisibility(state!=null&&state.getBoolean("noteOpen")?View.VISIBLE:View.GONE);sheet.addView(note,new LinearLayout.LayoutParams(-1,-2));memo.setOnClickListener(v->{note.setVisibility(note.getVisibility()==View.VISIBLE?View.GONE:View.VISIBLE);if(note.getVisibility()==View.VISIBLE)note.requestFocus();});
        inputBody=sheet;sheet.setBackground(null);sheetShell=Ui.col(this);sheetShell.setBackground(Ui.bg(this,Color.WHITE,28));sheetShell.setClipToOutline(true);ScrollView scroll=new ScrollView(this);scroll.addView(sheet);sheetShell.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        LinearLayout footer=Ui.col(this);inputFooter=footer;footer.setPadding(Ui.dp(this,24),Ui.dp(this,8),Ui.dp(this,24),Ui.dp(this,22));save=Ui.button(this,entry.level()>0?"変更を保存":"記録する",true,this::save);footer.addView(save,new LinearLayout.LayoutParams(-1,-2));if(entry.level()>0){Ui.gap(footer,8);TextView remove=Ui.danger(this,"今日の記録を取り消す",()->Ui.show(Ui.sheet(this,"","今日の記録を取り消す？","今日の数値・達成・メモを削除します。完走済みタイマーの履歴は残ります。","取り消す",this::remove),this));remove.setId(R.id.amount_remove);footer.addView(remove,new LinearLayout.LayoutParams(-1,-2));}sheetShell.addView(footer);
        FrameLayout.LayoutParams params=new FrameLayout.LayoutParams(-1,Ui.dp(this,430),Gravity.BOTTOM);root.addView(sheetShell,params);setContentView(root);root.addOnLayoutChangeListener((v,l,t,r,b,ol,ot,or,ob)->root.post(this::fitSheet));inputBody.addOnLayoutChangeListener((v,l,t,r,b,ol,ot,or,ob)->{if(b-t!=ob-ot)root.post(this::fitSheet);});root.post(this::fitSheet);
        amount.setOnEditorActionListener((v,action,event)->{if(action==EditorInfo.IME_ACTION_DONE){save();return true;}return false;});amount.requestFocus();amount.postDelayed(()->((InputMethodManager)getSystemService(INPUT_METHOD_SERVICE)).showSoftInput(amount,InputMethodManager.SHOW_IMPLICIT),180);
    }
    private void fitSheet(){if(root==null||sheetShell==null)return;int available=root.getHeight()-root.getPaddingTop()-root.getPaddingBottom();if(available<=0)return;int width=root.getWidth()-root.getPaddingLeft()-root.getPaddingRight();if(width<=0)return;inputBody.measure(View.MeasureSpec.makeMeasureSpec(width,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(0,View.MeasureSpec.UNSPECIFIED));inputFooter.measure(View.MeasureSpec.makeMeasureSpec(width,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(0,View.MeasureSpec.UNSPECIFIED));int height=Math.min(inputBody.getMeasuredHeight()+inputFooter.getMeasuredHeight(),available);android.view.ViewGroup.LayoutParams params=sheetShell.getLayoutParams();if(params.height!=height){params.height=height;sheetShell.setLayoutParams(params);}}
    private void save(){
        if(saving)return;
        try{
            double value=Inputs.number(amount.getText().toString(),"今日の合計",0,1000000,false,false);
            if(!day.equals(LocalDate.now()))throw new IllegalArgumentException("日付が変わりました。開き直してください");
            Store.Entry before=store.entry(habit.id(),day);Store.Result result=store.recordAmount(habit.id(),day,value,note.getText().toString(),false);
            saving=true;save.setEnabled(false);HabitWidget.update(this);Reminders.refreshVisible(this);((InputMethodManager)getSystemService(INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(amount.getWindowToken(),0);
            if(Rules.shouldCelebrate(metric.kind(),metric.target(),before.level(),before.amount(),result.level(),value)){
                root.setVisibility(View.INVISIBLE);Feedback.deliver(this,root,result.recovery(),"記録しました");Celebration.show(this,habit,result.level(),result.recovery(),()->{startActivity(new Intent(this,MainActivity.class).setAction(ProgressWidget.OPEN).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_SINGLE_TOP));finish();},this::finish);
            }else{Toast.makeText(this,value==0?"今日の記録を未達成に戻しました":Ui.number(value)+metric.unit()+"を保存しました",Toast.LENGTH_SHORT).show();finish();}
        }catch(IllegalArgumentException e){amount.setError(e.getMessage());amount.requestFocus();}
    }
    void remove(){
        if(saving)return;if(!day.equals(LocalDate.now())){Toast.makeText(this,"日付が変わりました。開き直してください",Toast.LENGTH_LONG).show();finish();return;}
        store.record(habit.id(),day,0,true);saving=true;HabitWidget.update(this);Reminders.refreshVisible(this);Toast.makeText(this,"今日の記録を取り消しました",Toast.LENGTH_SHORT).show();finish();
    }
    @Override protected void onDestroy(){super.onDestroy();Celebration.dismiss(this);}
    @Override public void onSaveInstanceState(Bundle state){super.onSaveInstanceState(state);if(amount!=null){state.putString("amount",amount.getText().toString());state.putString("note",note.getText().toString());state.putBoolean("noteOpen",note.getVisibility()==View.VISIBLE);}}
}
