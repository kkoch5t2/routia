package app.routia;

import android.content.Context;
import android.graphics.*;
import android.view.*;
import android.widget.*;

/** A normal navigation button with a decorative, non-interactive paid ribbon. */
// This navigation control is built programmatically, never inflated from XML.
@android.annotation.SuppressLint("ViewConstructor")
final class PaidReviewTab extends FrameLayout {
    private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path clip=new Path();
    private final RectF bounds=new RectF();
    private final float density;
    PaidReviewTab(Context context,boolean selected,Runnable action){
        super(context);density=context.getResources().getDisplayMetrics().density;
        setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        TextView title=Ui.button(context,"継続\nレビュー",selected,action);
        title.setTag("nav_review");title.setContentDescription("継続レビュー（有料）");
        title.setTextSize(13);title.setLineSpacing(0,1);title.setMinHeight(Ui.dp(context,66));
        title.setGravity(Gravity.CENTER);title.setIncludeFontPadding(false);
        title.setPadding(Ui.dp(context,6),Ui.dp(context,6),Ui.dp(context,6),Ui.dp(context,6));
        title.setSelected(selected);title.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_YES);
        addView(title,new FrameLayout.LayoutParams(-1,-1));
        setMinimumHeight(Ui.dp(context,66));
    }
    @Override protected void dispatchDraw(Canvas canvas){
        super.dispatchDraw(canvas);
        int save=canvas.save();
        bounds.set(0,0,getWidth(),getHeight());clip.reset();clip.addRoundRect(bounds,14*density,14*density,Path.Direction.CW);canvas.clipPath(clip);
        // Positive rotation in screen coordinates slopes from upper left to lower right.
        canvas.translate(getWidth()-18*density,18*density);canvas.rotate(45);
        paint.setStyle(Paint.Style.FILL);paint.setColor(0xff8A4B00);
        canvas.drawRect(-34*density,-7*density,34*density,7*density,paint);
        paint.setColor(Color.WHITE);paint.setTypeface(Typeface.create("sans-serif",Typeface.BOLD));paint.setTextAlign(Paint.Align.CENTER);
        float scale=Math.min(getResources().getConfiguration().fontScale,1.3f);
        paint.setTextSize(9*density*scale);
        Paint.FontMetrics metrics=paint.getFontMetrics();
        canvas.drawText("有料",0,-(metrics.ascent+metrics.descent)/2,paint);
        canvas.restoreToCount(save);
    }
}
