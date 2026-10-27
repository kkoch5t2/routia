package app.routia;
import android.app.*;
import android.content.*;
import android.graphics.*;
import android.graphics.drawable.*;
import android.view.*;
import android.widget.*;
public final class Ui {
    static final int BG=0xffFFF0D0,INK=0xff203C32,GREEN=0xff28664F,MUTED=0xff60766A,PALE=0xffE0EBDD,RED=0xffA63F36;
    static boolean night(Context c){return AppColors.night(c);}
    static int ink(Context c,int color){if(!night(c))return color;if(color==INK)return AppColors.black(c)?0xffF5F5F5:0xffE8F2EB;if(color==GREEN)return 0xff9DD8B5;if(color==MUTED)return AppColors.black(c)?0xffC4C4C4:0xffACBFB2;if(color==RED)return 0xffFFB4AA;return color;}
    static int surface(Context c,int color){if(color==BG)return AppColors.background(c);if(!night(c))return color;if(color==Color.WHITE)return AppColors.black(c)?0xff202020:0xff1C2B24;if(color==BG)return 0xff111C17;if(color==PALE)return AppColors.black(c)?0xff303030:0xff304B3B;if(color==0xffDDE5DB||color==0xffB7CABB||color==0xffC7D3C8)return AppColors.black(c)?0xff555555:0xff526C5D;return color;}
    static GradientDrawable fixedBg(Context c,int color,int radius){GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(c,radius));return d;}
    static TextView fixedText(Context c,String s,int size,int color,boolean bold){TextView t=new TextView(c);t.setText(s);t.setTextSize(size);t.setTextColor(color);t.setLineSpacing(dp(c,3),1);if(bold)t.setTypeface(Typeface.create("sans-serif",Typeface.BOLD));return t;}
    static int dp(Context c,float n){return Math.round(n*c.getResources().getDisplayMetrics().density);}
    static GradientDrawable bg(Context c,int color,int radius){return fixedBg(c,surface(c,color),radius);}
    static TextView text(Context c,String s,int size,int color,boolean bold){return fixedText(c,s,size,ink(c,color),bold);}
    static LinearLayout col(Context c){LinearLayout l=new LinearLayout(c);l.setOrientation(LinearLayout.VERTICAL);return l;}
    static void gap(LinearLayout l,int h){l.addView(new View(l.getContext()),new LinearLayout.LayoutParams(1,dp(l.getContext(),h)));}
    static void divider(LinearLayout l){View line=new View(l.getContext());line.setBackgroundColor(surface(l.getContext(),0xffDDE5DB));l.addView(line,new LinearLayout.LayoutParams(-1,dp(l.getContext(),1)));}
    static TextView control(Context c,String s,int style,Runnable action){
        boolean filled=style==1;int ink=style==2?RED:GREEN;TextView t=text(c,s,16,filled?Color.WHITE:ink,true);t.setGravity(Gravity.CENTER);t.setMinHeight(dp(c,54));t.setPadding(dp(c,16),dp(c,14),dp(c,16),dp(c,14));GradientDrawable normal=bg(c,filled?GREEN:Color.WHITE,14);if(!filled)normal.setStroke(dp(c,1),surface(c,style==2?0xffE5BEB8:0xffB7CABB));
        StateListDrawable states=new StateListDrawable();GradientDrawable focused=bg(c,filled?0xff194A37:PALE,14);focused.setStroke(dp(c,2),ink(c,ink));states.addState(new int[]{android.R.attr.state_focused},focused);states.addState(new int[]{},normal);
        t.setBackground(new RippleDrawable(android.content.res.ColorStateList.valueOf(filled?0x66FFFFFF:0x33447755),states,bg(c,Color.WHITE,14)));t.setOnClickListener(v->{v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);action.run();});t.setFocusable(true);return t;
    }
    static TextView button(Context c,String s,boolean primary,Runnable action){return control(c,s,primary?1:0,action);}
    static TextView back(Context c,Runnable action){
        TextView back=button(c,"戻る",false,action);back.setTextSize(13);back.setSingleLine(true);back.setIncludeFontPadding(false);
        back.setLineSpacing(0,1);back.setGravity(Gravity.CENTER);back.setMinHeight(dp(c,48));back.setPadding(dp(c,10),0,dp(c,10),0);
        Drawable icon=new Drawable(){
            final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
            public void draw(Canvas canvas){
                Rect b=getBounds();canvas.save();canvas.translate(b.left,b.top);canvas.scale(b.width()/20f,b.height()/20f);
                paint.setColor(ink(c,GREEN));paint.setStrokeWidth(2);paint.setStrokeCap(Paint.Cap.ROUND);
                canvas.drawLine(12,5,6,10,paint);canvas.drawLine(6,10,12,15,paint);canvas.drawLine(6,10,16,10,paint);canvas.restore();
            }
            public void setAlpha(int alpha){paint.setAlpha(alpha);}
            public void setColorFilter(ColorFilter filter){paint.setColorFilter(filter);}
            public int getOpacity(){return PixelFormat.TRANSLUCENT;}
        };
        icon.setBounds(0,0,dp(c,20),dp(c,20));back.setCompoundDrawables(icon,null,null,null);
        back.setCompoundDrawablePadding(dp(c,5));back.setContentDescription("戻る");return back;
    }
    static void insetChoiceIndicator(RadioButton choice,int inset){
        Drawable current=choice.getButtonDrawable();
        if(current!=null)choice.setButtonDrawable(new InsetDrawable(current.mutate(),dp(choice.getContext(),inset),0,dp(choice.getContext(),4),0));
    }
    static TextView outline(Context c,String s,Runnable a){return control(c,s,0,a);}
    static TextView danger(Context c,String s,Runnable a){return control(c,s,2,a);}
    static TextView achievement(Context c,int level,Runnable action){
        TextView button=control(c,Rules.grade(level),0,action);int color=AchievementColors.background(level),ink=AchievementColors.foreground(level);
        button.setTextColor(ink);GradientDrawable base=fixedBg(c,color,14);
        button.setBackground(new RippleDrawable(android.content.res.ColorStateList.valueOf(level==3?0x55FFFFFF:0x33000000),base,fixedBg(c,Color.WHITE,14)));return button;
    }
    static void highlightGrades(RadioGroup group){
        for(int k=0;k<group.getChildCount();k++){RadioButton button=(RadioButton)group.getChildAt(k);int level=(Integer)button.getTag(),ink=AchievementColors.foreground(level);button.setTextColor(ink);button.setButtonTintList(android.content.res.ColorStateList.valueOf(ink));GradientDrawable background=fixedBg(group.getContext(),AchievementColors.background(level),14);button.setBackground(background);button.setTypeface(null,button.isChecked()?Typeface.BOLD:Typeface.NORMAL);}
    }
    static RadioButton choice(Context c,String title,String detail){RadioButton r=new RadioButton(c);r.setId(View.generateViewId());r.setText(detail.isEmpty()?title:c.getString(R.string.choice_detail,title,detail));r.setTextSize(16);r.setTextColor(ink(c,INK));r.setMinHeight(dp(c,70));r.setPadding(dp(c,12),dp(c,12),dp(c,12),dp(c,12));r.setButtonTintList(android.content.res.ColorStateList.valueOf(ink(c,GREEN)));RadioGroup.LayoutParams lp=new RadioGroup.LayoutParams(-1,-2);lp.bottomMargin=dp(c,10);r.setLayoutParams(lp);return r;}
    static void highlight(RadioGroup group){for(int i=0;i<group.getChildCount();i++){RadioButton r=(RadioButton)group.getChildAt(i);GradientDrawable background=bg(r.getContext(),r.isChecked()?PALE:BG,14);if(r.isChecked())background.setStroke(dp(r.getContext(),2),GREEN);r.setBackground(background);r.setTypeface(null,r.isChecked()?Typeface.BOLD:Typeface.NORMAL);}}
    static class FormDialog extends Dialog {
        TextView notice;ScrollView formScroll;String problem;View problemField;
        final java.util.List<RequiredField> requiredFields=new java.util.ArrayList<>();
        static final class RequiredField{final EditText view;final String label;final java.util.function.BooleanSupplier active;RequiredField(EditText v,String l,java.util.function.BooleanSupplier a){view=v;label=l;active=a;}}
        void bind(LinearLayout form,ScrollView scroll,TextView feedback){formScroll=scroll;notice=feedback;notice.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);notice.setOnClickListener(v->{for(RequiredField field:requiredFields)if(field.active.getAsBoolean()&&Inputs.clean(field.view.getText().toString()).isEmpty()){reveal(field.view);break;}});watch(form);}
        void watch(View view){if(view instanceof EditText){EditText input=(EditText)view;input.addTextChangedListener(new android.text.TextWatcher(){public void beforeTextChanged(CharSequence s,int start,int count,int after){}public void onTextChanged(CharSequence s,int start,int before,int count){input.setError(null);if(problemField==input){problem=null;problemField=null;}refreshNotice();}public void afterTextChanged(android.text.Editable e){}});}if(view instanceof CheckBox)((CheckBox)view).setOnCheckedChangeListener((v,on)->{if(problemField==v){problem=null;problemField=null;refreshNotice();}});if(view instanceof ViewGroup){ViewGroup group=(ViewGroup)view;for(int index=0;index<group.getChildCount();index++)watch(group.getChildAt(index));}}
        void require(EditText view,String label,java.util.function.BooleanSupplier active){requiredFields.add(new RequiredField(view,label,active));refreshNotice();}
        void refreshNotice(){if(notice==null)return;if(problem!=null){notice.setText(activity.getString(R.string.form_error,problem));notice.setTextColor(ink(activity,RED));notice.setVisibility(View.VISIBLE);return;}java.util.List<String> missing=new java.util.ArrayList<>();for(RequiredField field:requiredFields)if(field.active.getAsBoolean()&&Inputs.clean(field.view.getText().toString()).isEmpty())missing.add(field.label);if(requiredFields.isEmpty()){notice.setVisibility(View.GONE);return;}notice.setVisibility(View.VISIBLE);notice.setTextColor(ink(activity,missing.isEmpty()?GREEN:RED));notice.setText(missing.isEmpty()?activity.getString(R.string.form_ready):activity.getString(R.string.form_missing,String.join("・",missing)));}
        void showProblem(View view,String message){problem=message;problemField=view;refreshNotice();if(view instanceof EditText)((EditText)view).setError(message);if(notice!=null)notice.announceForAccessibility(activity.getString(R.string.form_error,message));if(view!=null)reveal(view);}
        void reveal(View view){view.requestFocus();if(view instanceof EditText)((android.view.inputmethod.InputMethodManager)activity.getSystemService(Context.INPUT_METHOD_SERVICE)).showSoftInput(view,android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT);view.postDelayed(()->view.requestRectangleOnScreen(new Rect(0,0,view.getWidth(),view.getHeight()),false),180);}
        final Activity activity;int desiredHeight,currentHeight=-1;boolean keyboard;
        final ViewTreeObserver.OnGlobalLayoutListener layout=this::fitVisibleArea;
        FormDialog(Activity a){super(a);activity=a;}
        @Override protected void onStart(){super.onStart();getWindow().getDecorView().getViewTreeObserver().addOnGlobalLayoutListener(layout);activity.getWindow().getDecorView().getViewTreeObserver().addOnGlobalLayoutListener(layout);getWindow().getDecorView().post(this::fitVisibleArea);}
        void fitVisibleArea(){
            Window window=getWindow();if(window==null||!isShowing()||desiredHeight<=0)return;int screen=activity.getResources().getDisplayMetrics().heightPixels;Rect visible=new Rect();activity.getWindow().getDecorView().getWindowVisibleDisplayFrame(visible);int ime=0;
            if(android.os.Build.VERSION.SDK_INT>=30){WindowInsets own=window.getDecorView().getRootWindowInsets(),parent=activity.getWindow().getDecorView().getRootWindowInsets();if(own!=null)ime=Math.max(ime,own.getInsets(WindowInsets.Type.ime()).bottom);if(parent!=null)ime=Math.max(ime,parent.getInsets(WindowInsets.Type.ime()).bottom);}
            int bottom=visible.bottom>0?Math.min(screen,visible.bottom):screen;if(ime>0)bottom=Math.min(bottom,screen-ime);boolean open=screen-bottom>dp(activity,100);int target=open?Math.min(desiredHeight,Math.max(dp(activity,180),bottom-visible.top-dp(activity,24))):desiredHeight;
            if(target==currentHeight&&keyboard==open)return;currentHeight=target;keyboard=open;WindowManager.LayoutParams attributes=window.getAttributes();attributes.height=target;attributes.gravity=open?Gravity.TOP|Gravity.CENTER_HORIZONTAL:Gravity.CENTER;attributes.y=open?dp(activity,12):0;window.setAttributes(attributes);
        }
        @Override protected void onStop(){getWindow().getDecorView().getViewTreeObserver().removeOnGlobalLayoutListener(layout);activity.getWindow().getDecorView().getViewTreeObserver().removeOnGlobalLayoutListener(layout);currentHeight=-1;super.onStop();}
    }
    static Dialog sheet(Activity a,String eyebrow,String title,String body,String action,Runnable run){
        Dialog d=new Dialog(a);LinearLayout box=col(a);box.setPadding(dp(a,24),dp(a,24),dp(a,24),dp(a,24));box.setBackground(bg(a,Color.WHITE,24));if(!action.contains("削除")&&!action.contains("取り消")){box.addView(Mascot.guideHeader(a,Mascot.forExplanation(eyebrow+" "+title+" "+body)));gap(box,10);}if(!eyebrow.isEmpty()){box.addView(text(a,eyebrow,11,GREEN,true));gap(box,12);}box.addView(text(a,title,24,INK,true));gap(box,14);box.addView(text(a,body,15,MUTED,false));gap(box,24);box.addView(control(a,action,action.contains("削除")||action.contains("取り消")?2:1,()->{d.dismiss();if(run!=null)run.run();}));gap(box,12);box.addView(outline(a,"閉じる",d::dismiss));ScrollView scroll=new ScrollView(a);scroll.addView(box);d.setContentView(scroll);d.getWindow().setBackgroundDrawableResource(android.R.color.transparent);return d;
    }
    static void show(Dialog d,Activity a){d.show();int width=Math.min(a.getResources().getDisplayMetrics().widthPixels-dp(a,32),dp(a,560));int height=d instanceof FormDialog?((FormDialog)d).desiredHeight:-2;d.getWindow().setLayout(width,height);d.getWindow().setDimAmount(.32f);}
    static void restoreScroll(ScrollView scroll,int y){
        scroll.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener(){
            @Override public void onGlobalLayout(){
                scroll.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                scroll.scrollTo(0,y);
            }
        });
    }
    static String number(double n){return java.math.BigDecimal.valueOf(n).round(new java.math.MathContext(12,java.math.RoundingMode.HALF_UP)).stripTrailingZeros().toPlainString();}
    private Ui(){}
}
