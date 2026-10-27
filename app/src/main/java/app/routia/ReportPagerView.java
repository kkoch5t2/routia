package app.routia;

import android.content.Context;
import android.view.*;
import android.widget.*;

/** Constant-size set of controls, independent of the number of pages. */
@android.annotation.SuppressLint("ViewConstructor")
final class ReportPagerView extends LinearLayout {
    ReportPagerView(Context context,long current,long pages,Runnable previous,Runnable jump,Runnable next){
        super(context);setOrientation(VERTICAL);
        TextView status=Ui.text(context,current+" / "+pages+" ページ",14,Ui.MUTED,false);
        status.setTag("page_status");status.setGravity(Gravity.CENTER);status.setPadding(0,Ui.dp(context,5),0,Ui.dp(context,8));
        status.setContentDescription(pages+"ページ中"+current+"ページ目");addView(status,new LayoutParams(-1,-2));
        LinearLayout row=new LinearLayout(context);row.setGravity(Gravity.CENTER_VERTICAL);addView(row,new LayoutParams(-1,-2));
        addControl(row,"前へ","page_previous",current>1,previous);
        addControl(row,"移動","page_jump",pages>1,jump);
        addControl(row,"次へ","page_next",current<pages,next);
    }
    private void addControl(LinearLayout row,String title,String tag,boolean enabled,Runnable action){
        Context c=getContext();TextView control=Ui.button(c,title,false,action);control.setTag(tag);
        if(tag.equals("page_jump")){control.setContentDescription("ページ番号を指定して移動");control.setTooltipText("ページ番号を指定して移動");}
        control.setTextSize(13);control.setPadding(Ui.dp(c,4),Ui.dp(c,10),Ui.dp(c,4),Ui.dp(c,10));control.setMinHeight(Ui.dp(c,48));
        control.setEnabled(enabled);control.setAlpha(enabled?1f:.38f);
        LayoutParams params=new LayoutParams(0,-2,1);params.setMargins(Ui.dp(c,2),0,Ui.dp(c,2),0);row.addView(control,params);
    }
}
