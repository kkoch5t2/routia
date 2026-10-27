package app.routia;
import android.app.*;
import android.content.*;
import android.graphics.*;
import android.graphics.drawable.GradientDrawable;
import android.os.*;
import android.view.*;
import android.view.inspector.WindowInspector;
import android.widget.*;
import java.time.*;
final class AppearanceReview {
    final Instrumentation i;final UxReview ui;final Store s;int n;
    AppearanceReview(Instrumentation i){this.i=i;ui=new UxReview(i);s=Store.get(i.getTargetContext());}
    void check(boolean ok,String message){n++;if(!ok)throw new AssertionError(message);}
    View description(View root,String text){if(text.contentEquals(root.getContentDescription()==null?"":root.getContentDescription()))return root;if(root instanceof ViewGroup)for(int at=0;at<((ViewGroup)root).getChildCount();at++){View found=description(((ViewGroup)root).getChildAt(at),text);if(found!=null)return found;}return null;}
    boolean contains(View root,String text){if(root instanceof TextView&&((TextView)root).getText().toString().contains(text))return true;if(root instanceof ViewGroup)for(int at=0;at<((ViewGroup)root).getChildCount();at++)if(contains(((ViewGroup)root).getChildAt(at),text))return true;return false;}
    View window(String text){for(View root:WindowInspector.getGlobalWindowViews())if(PreviewFrames.find(root,text)!=null)return root;throw new AssertionError("Window missing "+text);}
    void ticket(View scope){i.runOnMainSync(()->{View button=description(scope,"継続チケットの説明");if(button==null)throw new AssertionError("Missing ticket info");button.performClick();});i.waitForIdleSync();}
    void openColors(){ui.mainTap("背景色を選ぶ · "+WidgetColors.NAMES[WidgetColors.selected(ui.a)]);}
    void paletteModes(){for(boolean night:new boolean[]{false,true}){
        android.content.res.Configuration config=new android.content.res.Configuration(i.getTargetContext().getResources().getConfiguration());
        config.uiMode=(config.uiMode&~android.content.res.Configuration.UI_MODE_NIGHT_MASK)|(night?android.content.res.Configuration.UI_MODE_NIGHT_YES:android.content.res.Configuration.UI_MODE_NIGHT_NO);
        Context themed=new android.view.ContextThemeWrapper(i.getTargetContext().createConfigurationContext(config),R.style.AppTheme);
        check(Ui.night(themed)==night,"Palette follows configuration "+night);
        if(Build.VERSION.SDK_INT>=29){android.util.TypedValue attr=new android.util.TypedValue();check(themed.getTheme().resolveAttribute(android.R.attr.forceDarkAllowed,attr,true)&&attr.data==0,"Automatic recoloring disabled "+night);if(night)check(themed.getTheme().resolveAttribute(android.R.attr.isLightTheme,attr,true)&&attr.data==0,"Native dark theme declared");}
        int surface=Ui.surface(themed,Color.WHITE);
        for(int color:new int[]{Ui.INK,Ui.MUTED,Ui.GREEN}){int foreground=Ui.ink(themed,color);double contrast=(Math.max(Color.luminance(foreground),Color.luminance(surface))+.05)/(Math.min(Color.luminance(foreground),Color.luminance(surface))+.05);check(contrast>=4.5,"App text contrast "+night);}
        for(int color=0;color<WidgetColors.NAMES.length;color++){LinearLayout preview=WidgetColors.preview(themed,color);check(((GradientDrawable)preview.getBackground()).getColor().getDefaultColor()==WidgetColors.COLORS[color],"Exact preview background "+night+" "+color);check(((TextView)preview.getChildAt(0)).getCurrentTextColor()==WidgetColors.ink(color),"Exact preview foreground "+night+" "+color);}
    }}
    void explicitAppColors(){Context c=i.getTargetContext();int appOriginal=AppColors.selected(c),widgetOriginal=WidgetColors.selected(c);try{
        for(int color=0;color<WidgetColors.NAMES.length;color++){AppColors.save(c,color);check(AppColors.selected(c)==color,"App background persists "+color);check(Ui.surface(c,Ui.BG)==WidgetColors.COLORS[color],"App uses selected exact background "+color);check(Ui.night(c)==WidgetColors.dark(color),"Explicit app palette controls foreground "+color);check(WidgetColors.selected(c)==widgetOriginal,"App palette leaves widget preference unchanged "+color);
            LinearLayout preview=AppColors.preview(c,color);check(((GradientDrawable)preview.getBackground()).getColor().getDefaultColor()==WidgetColors.COLORS[color],"App preview matches selected background "+color);
        }
        AppColors.save(c,-1);check(Ui.night(c)==AppColors.systemNight(c),"System default restores device-following mode");
        if(Build.VERSION.SDK_INT>=29){Context light=new android.view.ContextThemeWrapper(c,R.style.RoutiaExplicitLight);android.util.TypedValue attr=new android.util.TypedValue();check(light.getTheme().resolveAttribute(android.R.attr.isLightTheme,attr,true)&&attr.data==0,"Explicit light palette resists expanded dark inversion");}
    }finally{AppColors.save(c,appOriginal);}}
    int run()throws Exception{
        if(!Build.HARDWARE.contains("ranchu")&&!Build.HARDWARE.contains("goldfish"))throw new AssertionError("Emulator only");
        i.runOnMainSync(this::paletteModes);i.runOnMainSync(this::explicitAppColors);check(s.habits().size()==1,"Run ticket fixture first");LocalDate today=LocalDate.now();long id=s.habits().get(0).id();s.record(id,today,2,false);s.details(id,today,20d,"今日読んだ本のメモ");
        ui.a=i.startActivitySync(new Intent(i.getTargetContext(),MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK));ui.mainTap("成果");View root=ui.a.getWindow().getDecorView();
        check(!contains(root,"連続記録があるときに自動使用")&&!contains(root,"この期間の自動使用"),"Ticket paragraphs are not on results");
        check(Color.red(GrassView.COLORS[GrassView.TICKET])>Color.green(GrassView.COLORS[GrassView.TICKET])+20,"Ticket cells use a red family");
        ui.shot("compact-results");ticket(root);ui.await("🎟 継続チケット");check(contains(window("🎟 継続チケット"),"1日達成した翌日以降"),"Popup explains first-day requirement");ui.shot("ticket-info");ui.tap("わかった");
        ui.mainTap("見方 ⓘ");ui.await("成果の見方");check(contains(window("成果の見方"),"赤いマス"),"Calendar help explains red cells");ui.tap("わかった");
        long fresh=s.saveHabit(0,"","初めての習慣","1回","check",0,"");ui.mainTap("成果");root=ui.a.getWindow().getDecorView();View freshCard=PreviewFrames.find(root,"初めての習慣").getParent() instanceof View?(View)PreviewFrames.find(root,"初めての習慣").getParent():null;
        ticket(freshCard);check(contains(window("🎟 継続チケット"),"まだ達成記録がありません"),"No-achievement popup is locked");ui.tap("わかった");
        s.record(fresh,today,1,false);ui.mainTap("成果");freshCard=(View)PreviewFrames.find(ui.a.getWindow().getDecorView(),"初めての習慣").getParent();ticket(freshCard);check(contains(window("🎟 継続チケット"),"明日から利用可能"),"First actual achievement unlocks from tomorrow");check(s.ticketDays(fresh).isEmpty(),"First achievement day does not consume a ticket");ui.tap("わかった");s.delete(fresh);
        ui.mainTap("設定");int original=WidgetColors.selected(ui.a);
        try{
            openColors();ui.choose("ダーク");check(WidgetColors.selected(ui.a)==original,"Preview does not change saved color");check(description(window("ウィジェットの背景色"),"色のプレビュー：ダーク")!=null,"Selected preview updates");ui.shot("widget-dark-preview");ui.tap("戻る");check(WidgetColors.selected(ui.a)==original,"Cancel keeps prior background");
            openColors();ui.choose("ダーク");ui.tap("この色で保存");check(WidgetColors.selected(ui.a)==5,"Save persists selected background");openColors();check(ui.await("ダーク").isChecked(),"Reopen restores selected swatch");ui.choose("ローズ");ui.shot("widget-rose-preview");ui.tap("戻る");check(WidgetColors.selected(ui.a)==5,"Draft swatch does not overwrite saved background");
            for(int color=0;color<WidgetColors.NAMES.length;color++){final int selected=color;WidgetColors.save(ui.a,color);i.runOnMainSync(()->{
                View rendered=WidgetCollections.preview(ui.a,WidgetCollections.RECORD);GradientDrawable background=(GradientDrawable)rendered.findViewById(R.id.widget_root).getBackground();
                check(background.getColor().getDefaultColor()==WidgetColors.COLORS[selected],"RemoteViews applies background "+selected);
                TextView title=rendered.findViewById(R.id.widget_title),goal=rendered.findViewById(R.id.row_goal),minimum=rendered.findViewById(R.id.row_min_button);
                check(title.getCurrentTextColor()==WidgetColors.ink(selected)&&goal.getCurrentTextColor()==WidgetColors.ink(selected),"RemoteViews foreground "+selected);
                check(minimum.getCurrentTextColor()==WidgetColors.buttonText(selected),"RemoteViews button text "+selected);
                double contrast=(Math.max(Color.luminance(WidgetColors.ink(selected)),Color.luminance(WidgetColors.COLORS[selected]))+.05)/(Math.min(Color.luminance(WidgetColors.ink(selected)),Color.luminance(WidgetColors.COLORS[selected]))+.05);
                check(contrast>=4.5,"Readable text contrast "+selected);
                if(Build.VERSION.SDK_INT>=29)check(!rendered.isForceDarkAllowed(),"Palette survives host night mode "+selected);
            });}
        }finally{WidgetColors.save(ui.a,original);}
        return n+ui.checks;
    }
}
