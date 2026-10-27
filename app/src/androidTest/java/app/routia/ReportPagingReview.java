package app.routia;

import android.app.*;
import android.content.*;
import android.content.res.Configuration;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.*;
import android.os.*;
import android.view.*;
import android.widget.*;
import java.io.*;
import java.time.*;
import java.util.*;

final class ReportPagingReview {
    final Instrumentation i;final ReportReview views;final ReportHubReview hub;int n;
    ReportPagingReview(Instrumentation i){this.i=i;views=new ReportReview(i);hub=new ReportHubReview(i);}
    void check(boolean ok,String message){n++;if(!ok)throw new AssertionError(message);}
    View decor(Activity a){return a.getWindow().getDecorView();}
    View tagged(Activity a,String tag){return views.findTag(decor(a),tag);}
    View control(Activity a,String tag){return views.findTag(tagged(a,"report_pager_top"),tag);}
    String status(Activity a){return ((TextView)control(a,"page_status")).getText().toString();}
    void click(View v){check(v!=null,"page control exists");i.runOnMainSync(v::performClick);i.waitForIdleSync();}
    void jump(MainActivity a,String value){
        click(control(a,"page_jump"));i.runOnMainSync(()->{a.reportPageInput.setText(value);a.reportPageDialog.getButton(AlertDialog.BUTTON_POSITIVE).performClick();});i.waitForIdleSync();
    }
    long insert(SQLiteDatabase db,long habit,String week,String snapshot,String result,long completed,String state){
        ContentValues values=new ContentValues();values.put("habit",habit);values.put("week",week);values.put("goal","表示テスト・ページング");values.put("snapshot",snapshot);values.put("result",result);values.put("state",state);values.put("completed_at",completed);values.put("created","2026-10-15T12:00:00");
        return db.insertOrThrow("reports",null,values);
    }
    int run()throws Exception{
        Context c=i.getTargetContext();
        check((Build.HARDWARE.contains("ranchu")||Build.HARDWARE.contains("goldfish"))&&c.getPackageName().endsWith(".reportqa"),"dedicated emulator only");
        Store store=Store.get(c);check(store.habits().isEmpty()&&!ReportSettings.configured(c),"fresh QA app with no credentials");
        boundaries(c);layouts(c);
        long first=store.saveConfiguredHabit(0,"朝食後","表示テスト・ページング","1分","少し続ける","余裕があれば","check",0,"");
        long second=store.saveConfiguredHabit(0,"帰宅後","表示テスト・別のタスク","一歩","踊る","余裕があれば","check",0,"");
        long empty=store.saveConfiguredHabit(0,"","表示テスト・レポートなし","一歩","読む","","check",0,"");
        List<Long> ids=new ArrayList<>();ZoneId zone=ZoneId.systemDefault();long when=LocalDate.of(2026,10,15).atTime(12,0).atZone(zone).toInstant().toEpochMilli();
        try(ReportStore reports=new ReportStore(c)){
            String snapshot=ReportData.snapshot(store,store.habit(first),LocalDate.now(),reports).toString();
            SQLiteDatabase db=reports.getWritableDatabase();db.beginTransaction();
            try{
                for(int at=1;at<=1005;at++)ids.add(insert(db,first,"paging-"+at,snapshot,ReportReview.personalFixture().put("headline","保存ページ "+at).toString(),when,"complete"));
                for(int at=0;at<25;at++)insert(db,second,"other-"+at,snapshot,ReportReview.personalFixture().toString(),LocalDate.of(2026,1,15).atStartOfDay(zone).toInstant().toEpochMilli(),"complete");
                insert(db,99999,"orphan",snapshot,"{}",when,"complete");insert(db,first,"queued",snapshot,"{}",when,"queued");
                db.setTransactionSuccessful();
            }finally{db.endTransaction();}
            ReportCatalog.Page start=reports.catalogPage(List.of(first,second),first,0,0,0,zone);
            check(start.total()==1005&&start.entries().size()==10&&start.entries().get(0).id()==ids.get(1004),"DB loads ten newest metadata rows, stable same-time ordering");
            ReportCatalog.Page hundred=reports.catalogPage(List.of(first,second),first,2026,10,99,zone);
            check(hundred.pages()==101&&hundred.entries().size()==10&&hundred.entries().get(0).id()==ids.get(14),"DB can fetch page 100 without preceding rows");
            ReportCatalog.Page last=reports.catalogPage(List.of(first,second),first,0,0,Long.MAX_VALUE,zone);
            check(last.index()==100&&last.entries().size()==5&&last.entries().get(4).id()==ids.get(0),"last partial page and stale index clamp");
            check(reports.catalogPage(List.of(first,second),0,0,0,0,zone).total()==1030,"orphan and queued report excluded");
        }
        MainActivity main=(MainActivity)i.startActivitySync(new Intent(c,MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));i.waitForIdleSync();hub.clickTag(main,"nav_review");
        check(hub.rows(decor(main))==10&&status(main).equals("1 / 103 ページ"),"large catalog renders ten rows with bounded pager");
        hub.select(main,"継続レビューのタスク",1);
        check(status(main).equals("1 / 101 ページ")&&!control(main,"page_previous").isEnabled(),"task filter starts at first page");
        hub.selectLabel(main,"継続レビューの生成年","2026年");hub.selectLabel(main,"継続レビューの生成月","10月");
        check(hub.choices(main,"継続レビューの生成月").equals(List.of("すべての月","10月"))&&status(main).equals("1 / 101 ページ"),"only the month with completed reports appears, independent of spinner index");
        click(control(main,"page_next"));check(status(main).equals("2 / 101 ページ")&&tagged(main,"saved_review_"+ids.get(994))!=null,"next changes rows instead of appending");
        click(control(main,"page_previous"));check(status(main).equals("1 / 101 ページ")&&hub.rows(decor(main))==10,"previous returns to first ten rows");
        jump(main,"0");check(main.reportPageDialog!=null&&main.reportPageInput.getError()!=null,"zero page rejected without closing dialog");
        MainActivity owner=main;
        i.runOnMainSync(()->{owner.reportPageInput.setText("102");owner.reportPageDialog.getButton(AlertDialog.BUTTON_POSITIVE).performClick();});i.waitForIdleSync();
        check(main.reportPageDialog!=null&&main.reportPageInput.getError()!=null,"beyond-last page rejected");
        i.runOnMainSync(()->{owner.reportPageInput.setText("9223372036854775808");owner.reportPageDialog.getButton(AlertDialog.BUTTON_POSITIVE).performClick();});i.waitForIdleSync();
        check(main.reportPageDialog!=null&&main.reportPageInput.getError()!=null,"overflowing page input rejected");
        i.runOnMainSync(()->owner.reportPageInput.setText("100"));
        main=(MainActivity)views.recreatePage(main);
        long until=SystemClock.uptimeMillis()+4000;
        while((main.reportPageDialog==null||!main.reportPageDialog.isShowing())&&SystemClock.uptimeMillis()<until){SystemClock.sleep(50);i.waitForIdleSync();}
        check(main.reportPageDialog!=null&&main.reportPageInput.getText().toString().equals("100"),"jump draft survives recreation");
        MainActivity restored=main;i.runOnMainSync(()->restored.reportPageDialog.getButton(AlertDialog.BUTTON_POSITIVE).performClick());i.waitForIdleSync();views.awaitFocus(main);
        check(status(main).equals("100 / 101 ページ")&&hub.rows(decor(main))==10&&tagged(main,"saved_review_"+ids.get(14))!=null,"direct jump to page 100");
        check(((ViewGroup)tagged(main,"report_pager_top")).getChildCount()==2,"pager never creates one control per page");
        views.shot(main,"paging-100");
        main=(MainActivity)views.recreatePage(main);views.awaitFocus(main);
        check(status(main).equals("100 / 101 ページ")&&hub.selector(main,"継続レビューの生成月").getSelectedItem().equals("10月"),"selected page and filters survive recreation");
        Activity reader=views.openTaggedPage(main,"saved_review_"+ids.get(14),ReportActivity.class);
        check(views.find(decor(reader),"保存ページ 15")!=null,"selected old page opens exact saved report");
        i.runOnMainSync(reader::finish);i.waitForIdleSync();views.awaitFocus(main);
        check(status(main).equals("100 / 101 ページ"),"reader returns to same page");
        hub.click(main,"成果");hub.clickTag(main,"nav_review");check(status(main).equals("100 / 101 ページ"),"page retained across tab return");
        click(control(main,"page_next"));check(status(main).equals("101 / 101 ページ")&&hub.rows(decor(main))==5&&!control(main,"page_next").isEnabled(),"last page is bounded and disables Next");
        hub.select(main,"継続レビューのタスク",3);
        check(hub.rows(decor(main))==0&&tagged(main,"report_pager_top")==null&&!hub.selector(main,"継続レビューの生成月").isEnabled(),"empty task hides paging and month choices without stale rows");
        hub.select(main,"継続レビューのタスク",1);hub.selectLabel(main,"継続レビューの生成年","2026年");hub.selectLabel(main,"継続レビューの生成月","10月");check(status(main).equals("1 / 101 ページ"),"filter change resets page");
        jump(main,"100");hub.select(main,"継続レビューのタスク",2);
        check(status(main).equals("1 / 3 ページ")&&hub.selector(main,"継続レビューの生成年").getSelectedItem().equals("2026年")&&hub.selector(main,"継続レビューの生成月").getSelectedItemPosition()==0&&hub.choices(main,"継続レビューの生成月").equals(List.of("すべての月","1月")),"task change retains available year but removes unavailable month and resets page");
        hub.selectLabel(main,"継続レビューの生成月","1月");hub.select(main,"継続レビューのタスク",0);
        check(status(main).equals("1 / 3 ページ")&&hub.selector(main,"継続レビューの生成月").getSelectedItem().equals("1月")&&hub.choices(main,"継続レビューの生成月").equals(List.of("すべての月","1月","10月")),"all-task month menu is the union for the year and preserves an available month");
        hub.click(main,"最新を表示");check(status(main).equals("1 / 103 ページ"),"reset returns to newest all-task page");
        try(ReportStore db=new ReportStore(c)){check(db.catalogPage(List.of(first,second),0,0,0,0,zone).total()==1030,"paging and reading do not create or submit paid reports");}
        hub.select(main,"継続レビューのタスク",1);hub.selectLabel(main,"継続レビューの生成年","2026年");hub.selectLabel(main,"継続レビューの生成月","10月");jump(main,"100");
        try(ReportStore db=new ReportStore(c)){db.getWritableDatabase().execSQL("UPDATE reports SET completed_at=? WHERE habit=? AND state='complete'",new Object[]{LocalDate.of(2026,3,15).atStartOfDay(zone).toInstant().toEpochMilli(),first});}
        main=(MainActivity)views.recreatePage(main);views.awaitFocus(main);
        check(status(main).equals("1 / 101 ページ")&&hub.selector(main,"継続レビューの生成月").getSelectedItemPosition()==0&&hub.choices(main,"継続レビューの生成月").equals(List.of("すべての月","3月")),"removed selected month resets month and stale page on recreation");
        check(!ReportSettings.configured(c),"no API credentials or paid generation used");
        MainActivity end=main;i.runOnMainSync(end::finish);i.waitForIdleSync();return n+hub.n+views.n;
    }
    void boundaries(Context c){
        String name="routia-paging-boundaries-test.db";c.deleteDatabase(name);ZoneId tokyo=ZoneId.of("Asia/Tokyo");
        try(ReportStore db=new ReportStore(c,name)){
            SQLiteDatabase sql=db.getWritableDatabase();
            insert(sql,1,"before","{}","{}",Instant.parse("2025-12-31T14:59:59Z").toEpochMilli(),"complete");
            long start=insert(sql,1,"start","{}","{}",Instant.parse("2025-12-31T15:00:00Z").toEpochMilli(),"complete");
            long end=insert(sql,1,"last","{}","{}",Instant.parse("2026-01-31T14:59:59Z").toEpochMilli(),"complete");
            insert(sql,1,"after","{}","{}",Instant.parse("2026-01-31T15:00:00Z").toEpochMilli(),"complete");
            insert(sql,1,"legacy","{}","{}",0,"complete");
            insert(sql,2,"second","{}","{}",Instant.parse("2026-01-15T00:00:00Z").toEpochMilli(),"complete");
            insert(sql,999,"orphan","{}","{}",Instant.parse("2026-01-15T00:00:00Z").toEpochMilli(),"complete");
            insert(sql,1,"queued","{}","{}",Instant.parse("2026-01-15T00:00:00Z").toEpochMilli(),"queued");
            insert(sql,2,"march","{}","{}",Instant.parse("2026-03-15T00:00:00Z").toEpochMilli(),"complete");
            insert(sql,999,"orphan-june","{}","{}",Instant.parse("2026-06-15T00:00:00Z").toEpochMilli(),"complete");
            insert(sql,1,"queued-may","{}","{}",Instant.parse("2026-05-15T00:00:00Z").toEpochMilli(),"queued");
            insert(sql,3,"zone-boundary","{}","{}",Instant.parse("2026-03-31T23:30:00Z").toEpochMilli(),"complete");
            check(db.catalogMonths(List.of(1L,2L),1,2026,tokyo).equals(List.of(1,2)),"month candidates use local year/month boundaries and exclude queued reports");
            check(db.catalogMonths(List.of(1L,2L),2,2026,tokyo).equals(List.of(1,3)),"month candidates are scoped to selected task");
            check(db.catalogMonths(List.of(1L,2L),0,2026,tokyo).equals(List.of(1,2,3)),"all-task months deduplicate and exclude orphan reports");
            check(db.catalogMonths(List.of(1L),1,2025,tokyo).equals(List.of(12)),"months are scoped to selected year");
            check(db.catalogMonths(List.of(1L),1,2026,ZoneId.of("UTC")).equals(List.of(1)),"month candidates respect viewing timezone rather than SQL UTC month");
            check(db.catalogMonths(List.of(3L),3,2026,tokyo).equals(List.of(4))&&db.catalogMonths(List.of(3L),3,2026,ZoneId.of("America/New_York")).equals(List.of(3)),"month candidates handle timezone offsets across month boundary");
            check(db.catalogMonths(List.of(1L,2L),1,-1,tokyo).isEmpty()&&db.catalogMonths(List.of(1L,2L),1,0,tokyo).isEmpty(),"unknown/all years have no month candidates");
            check(db.catalogMonths(List.of(),0,2026,tokyo).isEmpty()&&db.catalogMonths(List.of(1L),999,2026,tokyo).isEmpty(),"deleted/empty task scope cannot offer orphan months");
            ReportCatalog.Page january=db.catalogPage(List.of(1L,2L),1,2026,1,0,tokyo);
            check(january.total()==2&&january.entries().get(0).id()==end&&january.entries().get(1).id()==start,"SQL local month uses inclusive start/exclusive end");
            check(db.catalogPage(List.of(1L,2L),0,2026,1,0,tokyo).total()==3,"SQL combines allowed tasks with month");
            check(db.catalogPage(List.of(1L,2L),0,-1,0,0,tokyo).total()==1,"legacy unknown time remains independently selectable");
            check(db.catalogYears(List.of(1L,2L),1,tokyo).equals(List.of(2026,2025,-1)),"years are distinct, descending and scoped");
            check(db.catalogPage(List.of(),0,0,0,0,tokyo).total()==0&&db.catalogPage(List.of(1L),999,0,0,0,tokyo).total()==0,"empty/deleted task scope reveals no orphan reports");
        }finally{c.deleteDatabase(name);}
    }
    void layouts(Context context)throws Exception{
        for(float scale:new float[]{1f,1.8f})for(long pages:new long[]{100,1000000,922337203685477581L}){
            Configuration config=new Configuration(context.getResources().getConfiguration());config.fontScale=scale;Context c=context.createConfigurationContext(config);
            final ReportPagerView[] control={null};final boolean[] fits={true};final Bitmap[] image={null};
            i.runOnMainSync(()->{
                ReportPagerView p=new ReportPagerView(c,pages,pages,()->{},()->{},()->{});control[0]=p;
                p.measure(View.MeasureSpec.makeMeasureSpec(Ui.dp(c,240),View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(0,View.MeasureSpec.UNSPECIFIED));p.layout(0,0,p.getMeasuredWidth(),p.getMeasuredHeight());
                TextView status=(TextView)views.findTag(p,"page_status");
                android.text.Layout text=status.getLayout();
                fits[0]=text!=null&&text.getLineEnd(text.getLineCount()-1)==status.length();
                for(int line=0;text!=null&&line<text.getLineCount();line++)fits[0]&=text.getEllipsisCount(line)==0&&text.getLineWidth(line)<=status.getWidth()+1;
                ViewGroup row=(ViewGroup)p.getChildAt(1);
                for(int at=0;at<row.getChildCount();at++){View child=row.getChildAt(at);fits[0]&=child.getLeft()>=0&&child.getRight()<=row.getWidth()&&child.getWidth()>=Ui.dp(c,48);}
                image[0]=Bitmap.createBitmap(p.getWidth(),p.getHeight(),Bitmap.Config.ARGB_8888);p.draw(new Canvas(image[0]));
            });
            check(fits[0],"narrow-width pager fits full label and controls at font scale "+scale+" with "+pages+" pages");
            check(control[0].getHeight()<=Ui.dp(c,330)&&((ViewGroup)control[0].getChildAt(1)).getChildCount()==3,"bounded pager: pages="+pages+", scale="+scale+", heightDp="+control[0].getHeight()/c.getResources().getDisplayMetrics().density+", controls="+((ViewGroup)control[0].getChildAt(1)).getChildCount());
            if(scale==1.8f&&pages==1000000)try(FileOutputStream out=new FileOutputStream(new File(context.getExternalFilesDir(null),"report-pager-large.png"))){image[0].compress(Bitmap.CompressFormat.PNG,100,out);}
            image[0].recycle();
        }
    }
}
