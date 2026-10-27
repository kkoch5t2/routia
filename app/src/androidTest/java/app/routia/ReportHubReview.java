package app.routia;

import android.app.*;
import android.content.*;
import android.os.*;
import android.view.*;
import android.widget.*;
import java.time.*;
import java.util.*;

final class ReportHubReview {
    final Instrumentation i;final ReportReview views;int n;
    ReportHubReview(Instrumentation i){this.i=i;views=new ReportReview(i);}
    void check(boolean ok,String message){n++;if(!ok)throw new AssertionError(message);}
    View decor(Activity a){return a.getWindow().getDecorView();}
    View find(Activity a,String text){return views.find(decor(a),text);}
    View tag(Activity a,String text){return views.findTag(decor(a),text);}
    Spinner selector(Activity a,String description){return (Spinner)views.findDescription(decor(a),description);}
    void click(Activity a,String text){View v=find(a,text);check(v!=null,"control exists: "+text);i.runOnMainSync(v::performClick);i.waitForIdleSync();}
    void select(Activity a,String description,int at){Spinner v=selector(a,description);check(v!=null,"selector exists");i.runOnMainSync(()->v.setSelection(at));i.waitForIdleSync();}
    void selectLabel(Activity a,String description,String label){
        Spinner v=selector(a,description);int at=-1;for(int x=0;x<v.getCount();x++)if(label.equals(v.getItemAtPosition(x).toString()))at=x;
        check(at>=0,"filter option exists: "+label);select(a,description,at);
    }
    List<String> choices(Activity a,String description){Spinner s=selector(a,description);List<String> out=new ArrayList<>();for(int at=0;at<s.getCount();at++)out.add(s.getItemAtPosition(at).toString());return out;}
    void clickTag(Activity a,String tag){View v=tag(a,tag);check(v!=null,"tagged control exists: "+tag);i.runOnMainSync(v::performClick);i.waitForIdleSync();}
    void ribbon(Activity a){
        View label=tag(a,"nav_review"),frame=(View)label.getParent();
        check(label.getContentDescription().toString().equals("継続レビュー（有料）"),"paid label is announced by accessibility");
        check(label.getHeight()>=Ui.dp(a,48)&&label.getWidth()>=Ui.dp(a,48),"paid tab retains a full touch target");
        final android.graphics.Bitmap[] picture={null};
        i.runOnMainSync(()->{picture[0]=android.graphics.Bitmap.createBitmap(frame.getWidth(),frame.getHeight(),android.graphics.Bitmap.Config.ARGB_8888);frame.draw(new android.graphics.Canvas(picture[0]));});
        android.graphics.Bitmap b=picture[0];double sx=0,sy=0,sxy=0;int count=0;
        for(int y=0;y<b.getHeight();y++)for(int x=0;x<b.getWidth();x++)if(b.getPixel(x,y)==0xff8A4B00){count++;sx+=x;sy+=y;sxy+=(double)x*y;}
        check(count>Ui.dp(a,100)&&sx/count>b.getWidth()*.6&&sy/count<b.getHeight()*.4,"paid ribbon is visible in the upper-right corner");
        check(sxy/count-(sx/count)*(sy/count)>0,"ribbon slopes from upper left to lower right");
        check(b.getPixel(0,0)!=0xff8A4B00,"ribbon does not cover other tab areas");b.recycle();
    }
    int rows(View v){
        int result=v.getTag() instanceof String&&v.getTag().toString().startsWith("saved_review_")?1:0;
        if(v instanceof ViewGroup g)for(int x=0;x<g.getChildCount();x++)result+=rows(g.getChildAt(x));return result;
    }
    int run()throws Exception{
        Context c=i.getTargetContext();
        check((Build.HARDWARE.contains("ranchu")||Build.HARDWARE.contains("goldfish"))&&c.getPackageName().endsWith(".reportqa"),"dedicated emulator only");
        Store store=Store.get(c);check(store.habits().isEmpty()&&!ReportSettings.configured(c),"empty QA app without credentials");
        long first=store.saveConfiguredHabit(0,"朝食後","表示テスト・英語の練習","1分だけ","30分取り組む","余裕がある日だけ","time",30,"分");
        long second=store.saveConfiguredHabit(0,"帰宅後","表示テスト・ダンス","一歩","踊る","余裕があれば","check",0,"");
        long empty=store.saveConfiguredHabit(0,"就寝前","表示テスト・読書","1行","読む","余裕があれば","check",0,"");
        LocalDate today=LocalDate.now();ZoneId zone=ZoneId.systemDefault();
        store.getWritableDatabase().execSQL("UPDATE habits SET created=?",new Object[]{today.minusDays(35).toString()});
        for(int day=0;day<10;day++)store.getWritableDatabase().execSQL("INSERT INTO records(habit_id,day,level,amount,unit,note) VALUES(?,?,2,30,'分','')",new Object[]{first,today.minusDays(day).toString()});
        long[] ids=new long[6];long queued;
        try(ReportStore db=new ReportStore(c)){
            for(int at=0;at<ids.length;at++){
                long habit=at==2||at==4?second:first;
                String goal=store.habit(habit).goal();
                ids[at]=db.enqueue(habit,"hub-fixture-"+at,goal,ReportData.snapshot(store,store.habit(habit),today,db).toString());
                db.finish(ids[at],"complete",ReportReview.personalFixture().put("headline","保存したレポート "+at).toString(),"",2);
                long time=at==0?0:(at>=3?today:today.minusYears(1)).atTime(10+at,0).atZone(zone).toInstant().toEpochMilli();
                db.getWritableDatabase().execSQL("UPDATE reports SET completed_at=? WHERE id=?",new Object[]{time,ids[at]});
            }
            queued=db.enqueue(first,"hub-incomplete","表示テスト・英語の練習","{}");
            check(db.completedCatalog().size()==6&&db.all(first).size()+db.all(second).size()==7,"catalog includes completed metadata only");
        }
        Activity main=i.startActivitySync(new Intent(c,MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));i.waitForIdleSync();
        check(((ViewGroup)tag(main,"main_navigation")).getChildCount()==4,"four main navigation tabs");
        ribbon(main);
        click(main,"成果");
        check(tag(main,"paid_review_hub")==null&&rows(decor(main))==0,"Achievements does not contain the paid report catalog");
        check(tag(main,"summary_"+first)!=null&&find(main,"✅ 今月の達成")!=null,"summary and metrics always present");
        check(views.findType(decor(main),TrendView.class)==null&&views.findType(decor(main),GrassView.class)==null,"Achievements does not render inline graphs");
        views.shot(main,"achievement-tab");
        clickTag(main,"nav_review");
        check(tag(main,"paid_review_hub")!=null&&tag(main,"summary_"+first)==null&&tag(main,"nav_review").isSelected(),"dedicated selected tab contains saved reviews only");
        ribbon(main);
        check(rows(decor(main))==6&&tag(main,"saved_review_"+ids[5])!=null&&tag(main,"saved_review_"+queued)==null,"completed reports fit within the ten-report first page");
        check(!selector(main,"継続レビューの生成月").isEnabled()&&selector(main,"継続レビューの生成月").getCount()==1,"all years has only a disabled month placeholder");
        views.shot(main,"hub-top");
        check(tag(main,"report_pager_top")==null&&views.findStartsWith(decor(main),"さらに表示")==null,"one-page catalog needs no pagination or expanding list");

        select(main,"継続レビューのタスク",1);
        check(rows(decor(main))==4&&tag(main,"saved_review_"+ids[2])==null&&tag(main,"saved_review_"+ids[4])==null,"task filter excludes other task reports");
        String current=today.getYear()+"年";
        selectLabel(main,"継続レビューの生成年",current);
        selectLabel(main,"継続レビューの生成月",today.getMonthValue()+"月");
        check(rows(decor(main))==2&&tag(main,"saved_review_"+ids[5])!=null&&tag(main,"saved_review_"+ids[3])!=null,"task, generation year and month combine");
        check(choices(main,"継続レビューの生成月").equals(List.of("すべての月",today.getMonthValue()+"月"))&&selector(main,"継続レビューの生成年").getCount()==4,"month menu includes only existing task/year month and All");
        ScrollView sc=(ScrollView)views.findType(decor(main),ScrollView.class);
        i.runOnMainSync(()->sc.scrollTo(0,Ui.dp(c,120)));i.waitForIdleSync();int before=sc.getScrollY();
        Activity reader=views.openTaggedPage(main,"saved_review_"+ids[3],ReportActivity.class);
        check(reader.getIntent().getLongExtra("report",-1)==ids[3]&&find(reader,"保存したレポート 3")!=null,"opens exact selected older report");
        check(views.findStartsWith(decor(reader),"生成日：")!=null,"reader labels actual generation timestamp");
        i.runOnMainSync(reader::finish);i.waitForIdleSync();views.awaitFocus(main);views.awaitScroll(main,before);
        check(selector(main,"継続レビューのタスク").getSelectedItemPosition()==1&&selector(main,"継続レビューの生成年").getSelectedItem().equals(current),"filters retained after reading");
        views.awaitScroll(main,before);
        main=views.recreatePage(main);views.awaitFocus(main);
        check(selector(main,"継続レビューのタスク").getSelectedItemPosition()==1&&selector(main,"継続レビューの生成年").getSelectedItem().equals(current)&&rows(decor(main))==2,"filters and results survive recreation");

        click(main,"成果");check(tag(main,"summary_"+first)!=null&&tag(main,"paid_review_hub")==null,"summary remains on its own tab");
        clickTag(main,"nav_review");
        check(selector(main,"継続レビューのタスク").getSelectedItemPosition()==1&&selector(main,"継続レビューの生成年").getSelectedItem().equals(current),"filters retained through tab round trip");
        selectLabel(main,"継続レビューの生成年","生成年不明（旧）");
        check(rows(decor(main))==1&&tag(main,"saved_review_"+ids[0])!=null&&find(main,"生成日不明（旧レポート）")!=null,"legacy reports remain findable with honest date label");
        check(!selector(main,"継続レビューの生成月").isEnabled()&&selector(main,"継続レビューの生成月").getCount()==1,"unknown completion year never invents month choices");
        select(main,"継続レビューのタスク",2);
        check(selector(main,"継続レビューの生成年").getSelectedItemPosition()==0&&rows(decor(main))==2,"unknown year resets when task changes");
        select(main,"継続レビューのタスク",3);
        check(!selector(main,"継続レビューの生成月").isEnabled()&&selector(main,"継続レビューの生成月").getCount()==1,"task without reports offers no month values");
        check(rows(decor(main))==0&&find(main,"この条件に合うレポートはありません。")!=null,"task with no saved reports shows useful empty state");
        click(main,"最新を表示");
        check(rows(decor(main))==6&&selector(main,"継続レビューのタスク").getSelectedItemPosition()==0,"reset returns to newest saved reports");

        click(main,"成果");
        Activity charts=views.openTaggedPage(main,"charts_"+first,AchievementChartsActivity.class);
        check(find(charts,"記録のグラフ")!=null&&find(charts,"端末の実記録から集計 · 追加料金なし")!=null,"local graphs clearly labeled on separate screen");
        check(tag(charts,"paid_review_hub")==null&&views.findType(decor(charts),TrendView.class)!=null,"graph screen contains graphs without paid report hub");
        select(charts,"表示期間："+store.habit(first).goal(),2);
        views.shot(charts,"charts-separated");
        charts=views.recreatePage(charts);views.awaitFocus(charts);
        check(selector(charts,"表示期間："+store.habit(first).goal()).getSelectedItemPosition()==2,"graph selection survives recreation");
        i.runOnMainSync(charts::finish);i.waitForIdleSync();views.awaitFocus(main);
        android.content.SharedPreferences periods=c.getSharedPreferences("routia_history",Context.MODE_PRIVATE);
        Progress.Range month=Progress.range(1,today,store.habit(first).created(),today);
        check(views.findType(decor(main),Spinner.class)==null&&find(main,"✅ 今月の達成")!=null&&find(main,month.start()+" 〜 "+month.end())!=null,"summary stays on current month with no selectors after a yearly chart");
        check(periods.getInt("period_"+first,1)==2&&periods.getInt("period_"+second,1)==1,"chart periods remain independent of the summary");
        check(views.findType(decor(main),TrendView.class)==null&&tag(main,"paid_review_hub")==null&&tag(main,"summary_"+first)!=null,"return keeps Achievements separate from paid catalog and charts");
        clickTag(main,"nav_review");
        AppColors.save(c,6);main=views.recreatePage(main);views.awaitFocus(main);ribbon(main);
        check(tag(main,"nav_review").isSelected()&&tag(main,"paid_review_hub")!=null,"dedicated tab survives recreation and dark palette");
        views.shot(main,"paid-tab-dark");
        try(ReportStore db=new ReportStore(c)){
            check(db.completedCatalog().size()==6&&db.all(first).size()+db.all(second).size()==7&&db.item(queued).state().equals("queued"),"browsing and filters never create or submit paid requests");
        }
        check(!ReportSettings.configured(c),"no live API credentials required to read saved reports");
        Activity finishMain=main;i.runOnMainSync(finishMain::finish);i.waitForIdleSync();
        return n+views.n;
    }
}
