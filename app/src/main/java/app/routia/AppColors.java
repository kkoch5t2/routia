package app.routia;
import android.content.*;
import android.content.res.Configuration;
import android.widget.*;
/** App appearance is independent from the home-screen widget palette. */
final class AppColors {
    static int selected(Context c){int value=c.getSharedPreferences("routia_appearance",Context.MODE_PRIVATE).getInt("background",-1);return value>=-1&&value<WidgetColors.NAMES.length?value:-1;}
    static String name(int color){return color<0?"端末に合わせる":WidgetColors.NAMES[color];}
    static boolean systemNight(Context c){return (c.getResources().getConfiguration().uiMode&Configuration.UI_MODE_NIGHT_MASK)==Configuration.UI_MODE_NIGHT_YES;}
    static boolean black(Context c){return selected(c)==6;}
    static boolean night(Context c){int color=selected(c);return color<0?systemNight(c):WidgetColors.dark(color);}
    static int background(Context c){int color=selected(c);return color<0?(night(c)?0xff111C17:Ui.BG):WidgetColors.COLORS[color];}
    static void applyTheme(android.app.Activity a){int color=selected(a);if(color>=0)a.setTheme(color==6?R.style.RoutiaBlack:WidgetColors.dark(color)?R.style.RoutiaDark:R.style.RoutiaExplicitLight);}
    static void save(Context c,int color){if(color< -1||color>=WidgetColors.NAMES.length)throw new IllegalArgumentException("背景色を選んでください");c.getSharedPreferences("routia_appearance",Context.MODE_PRIVATE).edit().putInt("background",color).apply();}
    static LinearLayout preview(Context c,int color){
        boolean dark=color<0?systemNight(c):WidgetColors.dark(color);int background=color<0?(dark?0xff111C17:Ui.BG):WidgetColors.COLORS[color],ink=color==6?0xffF5F5F5:dark?0xffE8F2EB:Ui.INK,accent=dark?0xff9DD8B5:Ui.GREEN;
        LinearLayout panel=Ui.col(c);panel.setPadding(Ui.dp(c,16),Ui.dp(c,14),Ui.dp(c,16),Ui.dp(c,14));panel.setBackground(Ui.fixedBg(c,background,24));if(android.os.Build.VERSION.SDK_INT>=29)panel.setForceDarkAllowed(false);
        panel.addView(Ui.fixedText(c,"Routia",20,accent,true));Ui.gap(panel,10);
        LinearLayout card=Ui.col(c);card.setPadding(Ui.dp(c,14),Ui.dp(c,12),Ui.dp(c,14),Ui.dp(c,12));android.graphics.drawable.GradientDrawable surface=Ui.fixedBg(c,color==6?0xff202020:dark?0xff1C2B24:android.graphics.Color.WHITE,16);surface.setStroke(Ui.dp(c,1),color==6?0xff555555:dark?0xff526C5D:0xffDDE5DB);card.setBackground(surface);
        card.addView(Ui.fixedText(c,"小さな一歩を、今日も。",16,ink,true));Ui.gap(card,5);card.addView(Ui.fixedText(c,"🔥 積み重ねが、育っていく",13,accent,false));panel.addView(card);return panel;
    }
    private AppColors(){}
}
