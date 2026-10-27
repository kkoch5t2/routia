package app.routia;
import android.content.*;
import android.graphics.Color;
import android.os.Build;
import android.view.Gravity;
import android.widget.*;
/** Explicit widget palette; colors remain readable independently of the app's night mode. */
final class WidgetColors {
    static final String[] NAMES={"クリーム","ホワイト","ミント","スカイ","ローズ","ダーク","ブラック"};
    static final int[] COLORS={0xffFFF0D0,0xffFFFFFF,0xffDFEFE5,0xffE2EEF9,0xffFBE6E9,0xff243B32,0xff121212};
    static final int[] DRAWABLES={R.drawable.widget_bg,R.drawable.widget_bg_white,R.drawable.widget_bg_mint,R.drawable.widget_bg_sky,R.drawable.widget_bg_rose,R.drawable.widget_bg_dark,R.drawable.widget_bg_black};
    static int selected(Context c){int value=c.getSharedPreferences("routia_widget",Context.MODE_PRIVATE).getInt("background",0);return value>=0&&value<NAMES.length?value:0;}
    static boolean dark(int color){return color==5||color==6;}
    static int buttonResource(int color){return color==6?R.drawable.widget_button_black:color==5?R.drawable.widget_button_dark:R.drawable.widget_button;}
    static int ink(int color){return color==6?0xffF5F5F5:color==5?0xffF0F6F1:0xff203C32;}
    static int muted(int color){return color==6?0xffC4C4C4:color==5?0xffB6CBBE:0xff50685A;}
    static int button(int color){return color==6?0xff2C2C2C:color==5?0xff3E5E4B:0xffDCEADC;}
    static int buttonText(int color){return color==6?0xffF5F5F5:color==5?0xffE5F3E8:0xff254D3D;}
    static void save(Context c,int color){if(color<0||color>=NAMES.length)throw new IllegalArgumentException("背景色を選んでください");c.getSharedPreferences("routia_widget",Context.MODE_PRIVATE).edit().putInt("background",color).apply();HabitWidget.update(c);}
    static LinearLayout preview(Context c,int color){
        LinearLayout box=Ui.col(c);box.setPadding(Ui.dp(c,16),Ui.dp(c,12),Ui.dp(c,16),Ui.dp(c,14));box.setBackground(Ui.fixedBg(c,COLORS[color],24));if(Build.VERSION.SDK_INT>=29)box.setForceDarkAllowed(false);
        LinearLayout identity=new LinearLayout(c);identity.setGravity(Gravity.CENTER_VERTICAL);identity.addView(new Mascot(c,Mascot.AVATAR),new LinearLayout.LayoutParams(Ui.dp(c,30),Ui.dp(c,32)));identity.addView(Ui.fixedText(c,"Routia",20,ink(color),true));box.addView(identity);Ui.gap(box,10);
        java.util.List<Store.Habit> habits=Store.get(c).habits();String goal=habits.isEmpty()?"英語を聞く":habits.get(0).goal();
        box.addView(Ui.fixedText(c,goal,15,ink(color),true));Ui.gap(box,4);box.addView(Ui.fixedText(c,"今日の一歩を、ホーム画面から。",12,muted(color),false));Ui.gap(box,10);
        if(!habits.isEmpty()&&!Store.get(c).metric(habits.get(0).id()).kind().equals("check")){
            TextView input=Ui.fixedText(c,"数値を入力して記録",13,buttonText(color),true);input.setGravity(Gravity.CENTER);input.setMinHeight(Ui.dp(c,48));input.setBackground(Ui.fixedBg(c,button(color),14));box.addView(input,new LinearLayout.LayoutParams(-1,-2));return box;
        }
        LinearLayout row=new LinearLayout(c);
        int grade=0;for(String title:new String[]{"🌱 ミニ","🏆 目標","🚀 挑戦"}){int level=++grade;TextView button=Ui.fixedText(c,title,12,AchievementColors.foreground(level),true);button.setGravity(Gravity.CENTER);button.setBackground(Ui.fixedBg(c,AchievementColors.background(level),14));LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,Ui.dp(c,42),1);p.setMargins(0,0,Ui.dp(c,6),0);row.addView(button,p);}
        box.addView(row);return box;
    }
    private WidgetColors(){}
}
