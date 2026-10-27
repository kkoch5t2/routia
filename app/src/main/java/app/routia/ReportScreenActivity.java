package app.routia;
import android.app.Activity;
import android.graphics.Color;
import android.os.*;
import android.view.*;
import android.widget.*;
/** Shared full-screen chrome for the review and its configuration. */
abstract class ReportScreenActivity extends Activity {
    final Activity activity=this;
    int dp(float value){return Ui.dp(this,value);}
    @Override public void onCreate(Bundle state){AppColors.applyTheme(this);super.onCreate(state);}
    LinearLayout screenRoot(){
        LinearLayout root=Ui.col(this);root.setBackgroundColor(Ui.surface(this,Ui.BG));setContentView(root);
        getWindow().setStatusBarColor(Ui.surface(this,Ui.BG));getWindow().setNavigationBarColor(Ui.surface(this,Ui.BG));
        if(Build.VERSION.SDK_INT>=27)getWindow().getDecorView().setSystemUiVisibility(Ui.night(this)?0:View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR|View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
        root.setOnApplyWindowInsetsListener((v,insets)->{
            if(Build.VERSION.SDK_INT>=30){android.graphics.Insets bars=insets.getInsets(WindowInsets.Type.systemBars()|WindowInsets.Type.displayCutout()|WindowInsets.Type.ime());v.setPadding(bars.left,bars.top,bars.right,bars.bottom);}
            else v.setPadding(insets.getSystemWindowInsetLeft(),insets.getSystemWindowInsetTop(),insets.getSystemWindowInsetRight(),insets.getSystemWindowInsetBottom());
            return insets;
        });
        return root;
    }
    LinearLayout screenHeader(String subtitle){
        LinearLayout head=Ui.col(this);head.setPadding(dp(20),dp(16),dp(20),dp(12));
        head.addView(Ui.text(this,"継続レビュー",22,Ui.INK,true));Ui.gap(head,8);
        head.addView(Ui.text(this,subtitle,18,Ui.INK,true));return head;
    }
    LinearLayout screenFooter(){LinearLayout foot=Ui.col(this);foot.setPadding(dp(20),dp(8),dp(20),dp(16));return foot;}
    void addBack(LinearLayout footer){footer.addView(Ui.button(this,"戻る",false,this::finish));}
    LinearLayout screenCard(LinearLayout parent,String title){
        LinearLayout card=Ui.col(this);card.setPadding(dp(16),dp(16),dp(16),dp(16));card.setBackground(Ui.bg(this,Color.WHITE,20));
        LinearLayout.LayoutParams params=new LinearLayout.LayoutParams(-1,-2);params.bottomMargin=dp(12);parent.addView(card,params);
        if(!title.isEmpty()){card.addView(Ui.text(this,title,18,Ui.INK,true));Ui.gap(card,10);}return card;
    }
}
