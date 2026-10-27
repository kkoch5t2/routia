package app.routia;
import android.app.Activity;
import android.content.Context;
import android.graphics.*;
import android.view.*;
import android.widget.*;
/** Ruti: the squirrel who gathers small daily actions, like acorns, into a future reserve. */
public final class Mascot extends View {
    public static final int WELCOME=0,GUIDE=1,CHEER=2,CALM=3,FOCUS=4,PROGRESS=5,AVATAR=6;
    static final Rect[] FRAMES={new Rect(28,8,531,529),new Rect(527,20,1048,527),new Rect(1018,8,1530,530),new Rect(35,528,520,1007),new Rect(550,530,997,998),new Rect(1017,533,1523,1001)};
    static final Path GUIDE_CLIP=new Path(),CHEER_CLIP=new Path();
    static{GUIDE_CLIP.moveTo(527,20);GUIDE_CLIP.lineTo(1015,20);GUIDE_CLIP.lineTo(1015,235);GUIDE_CLIP.lineTo(1048,235);GUIDE_CLIP.lineTo(1048,527);GUIDE_CLIP.lineTo(527,527);GUIDE_CLIP.close();CHEER_CLIP.moveTo(1018,8);CHEER_CLIP.lineTo(1530,8);CHEER_CLIP.lineTo(1530,530);CHEER_CLIP.lineTo(1050,530);CHEER_CLIP.lineTo(1050,235);CHEER_CLIP.lineTo(1018,235);CHEER_CLIP.close();}
    private static Bitmap shared;final int pose;final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG);final RectF destination=new RectF();
    public Mascot(Context c){this(c,WELCOME);}
    public Mascot(Context c,int pose){super(c);this.pose=Math.max(0,Math.min(6,pose));setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);}
    static synchronized Bitmap atlas(Context c){if(shared==null){BitmapFactory.Options options=new BitmapFactory.Options();options.inScaled=false;shared=BitmapFactory.decodeResource(c.getResources(),R.drawable.ruti_scenes,options);}return shared;}
    static void drawFit(Context context,Canvas canvas,int pose,RectF area,Paint paint){
        paint.setColor(Color.WHITE);paint.setAlpha(255);
        if(pose==AVATAR){RutiRig.avatar(context,canvas,area,paint);return;}
        Rect frame=FRAMES[pose];float scale=Math.min(area.width()/frame.width(),area.height()/frame.height()),w=frame.width()*scale,h=frame.height()*scale;
        canvas.save();canvas.translate(area.centerX()-w/2,area.centerY()-h/2);canvas.scale(scale,scale);canvas.translate(-frame.left,-frame.top);canvas.clipRect(frame);
        if(pose==GUIDE)canvas.clipPath(GUIDE_CLIP);if(pose==CHEER)canvas.clipPath(CHEER_CLIP);
        canvas.drawBitmap(atlas(context),0,0,paint);canvas.restore();
    }
    @Override protected void onDraw(Canvas canvas){super.onDraw(canvas);destination.set(0,0,getWidth(),getHeight());drawFit(getContext(),canvas,pose,destination,paint);}
    static Bitmap bitmap(Context c,int pose,int pixels){Bitmap image=Bitmap.createBitmap(pixels,pixels,Bitmap.Config.ARGB_8888);drawFit(c,new Canvas(image),pose,new RectF(0,0,pixels,pixels),new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG));return image;}
    static LinearLayout guideHeader(Context c){return guideHeader(c,GUIDE);}
    static int forExplanation(String text){if(text.contains("ルティについて"))return WELCOME;if(text.contains("集中")||text.contains("タイマー")||text.contains("画面固定"))return FOCUS;if(text.contains("再開")||text.contains("チケット")||text.contains("お休み"))return CALM;if(text.contains("成果")||text.contains("記録")||text.contains("66日"))return PROGRESS;return GUIDE;}
    static LinearLayout guideHeader(Context c,int pose){
        LinearLayout row=new LinearLayout(c);row.setGravity(Gravity.CENTER_VERTICAL);row.setId(R.id.ruti_guide);
        row.addView(new Mascot(c,pose),new LinearLayout.LayoutParams(Ui.dp(c,72),Ui.dp(c,72)));
        TextView label=Ui.text(c,"ルティのヒント",12,Ui.MUTED,true);LinearLayout.LayoutParams text=new LinearLayout.LayoutParams(0,-2,1);text.leftMargin=Ui.dp(c,10);row.addView(label,text);return row;
    }
    static void intro(Activity a){Ui.show(Ui.sheet(a,"ROUTIA / ルティについて","小さな一歩を、蓄えに。","Routiaの相棒、リスの「ルティ」です。\n\nどんぐりを少しずつ蓄える姿に、毎日の小さな行動が積み重なることを重ねました。ホームで迎え、ヒントで案内し、目標に届いたら一緒に喜びます。\n\nどんぐりは積み重ねのシンボルです。達成日数や時間・量は、実際の記録から表示します。","よろしく、ルティ",null),a);}
}
