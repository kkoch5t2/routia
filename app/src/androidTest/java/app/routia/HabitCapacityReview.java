package app.routia;

import android.app.*;
import android.content.*;
import android.database.Cursor;
import android.database.sqlite.*;
import android.os.*;
import android.view.*;
import android.widget.*;
import java.time.*;
import java.util.*;

/** Fixed-ten capacity and legacy upgrades use only an empty emulator QA app. */
final class HabitCapacityReview {
    final Instrumentation i;final ReportReview views;final ReportHubReview hub;int n;
    HabitCapacityReview(Instrumentation i){this.i=i;views=new ReportReview(i);hub=new ReportHubReview(i);}
    void check(boolean ok,String message){n++;if(!ok)throw new AssertionError(message);}
    View root(Activity a){return a.getWindow().getDecorView();}
    void click(View v){check(v!=null,"control exists");i.runOnMainSync(v::performClick);i.waitForIdleSync();}
    long add(Store s,String name){return s.saveConfiguredHabit(0,"",name,"一歩","取り組む","余裕があれば","check",0,"");}
    void rejected(Runnable action,String label){try{action.run();throw new AssertionError(label);}catch(IllegalArgumentException|SQLiteConstraintException expected){n++;}}
    void rawAdd(Store s){s.getWritableDatabase().execSQL("INSERT INTO habits(cue,goal,minimum,created) VALUES('','raw test','一歩',?)",new Object[]{LocalDate.now().toString()});}
    void addUi(MainActivity a,int existing){
        click(views.find(root(a),"＋ 習慣を追加  ·  "+existing+" / 10"));
        View form=new CriteriaReview(i).window("すべて任意です。");EditText title=new CriteriaReview(i).inputs(form).get(0);
        i.runOnMainSync(()->title.setText("目標テスト "+(existing+1)));click(views.find(form,"保存"));
    }
    void storage(Context c,int version,int selected){
        String name="habit-capacity-v"+version+"-"+selected+".db";c.deleteDatabase(name);long first;LocalDate today=LocalDate.now();
        try(Store s=new Store(c,name)){
            first=add(s,"移行前の目標");for(int at=2;at<=selected;at++)add(s,"移行前の目標"+at);
            s.record(first,today,2,false);long session=s.newSession(first);s.finishSession(session,120,true);
            s.getWritableDatabase().execSQL("INSERT INTO tickets(habit_id,day,used_on) VALUES(?,?,?)",new Object[]{first,today.minusDays(1).toString(),today.toString()});
            SQLiteDatabase db=s.getWritableDatabase();db.execSQL("DROP TRIGGER habit_limit");
            if(version==6){db.execSQL("CREATE TABLE habit_settings(id INTEGER PRIMARY KEY CHECK(id=1),capacity INTEGER NOT NULL CHECK(capacity BETWEEN 3 AND 10))");db.execSQL("INSERT INTO habit_settings VALUES(1,?)",new Object[]{selected});}
            db.execSQL("CREATE TRIGGER habit_limit BEFORE INSERT ON habits WHEN (SELECT count(*) FROM habits)>="+selected+" BEGIN SELECT RAISE(ABORT,'Legacy capacity'); END");db.setVersion(version);
        }
        try(Store s=new Store(c,name)){
            check(s.getReadableDatabase().getVersion()==7&&s.habits().size()==selected,"v"+version+" selected "+selected+" upgrades without losing habits");
            check(s.level(first,today)==2&&s.focused(first,today,today)==120&&s.ticket(first,today.minusDays(1)),"upgrade preserves achievement/session/ticket");
            check(s.goals(first).full().equals("取り組む"),"upgrade preserves explicit criteria");
            try(Cursor rows=s.getReadableDatabase().rawQuery("SELECT name FROM sqlite_master WHERE type='table' AND name='habit_settings'",null)){check(!rows.moveToFirst(),"obsolete configurable capacity removed");}
            for(int at=selected+1;at<=10;at++)add(s,"移行後の目標"+at);
            check(s.habits().size()==10,"old capacity never prevents reaching ten after upgrade");
            rejected(()->add(s,"eleventh"),"API blocks eleventh");rejected(()->rawAdd(s),"database trigger blocks raw eleventh");
            s.saveConfiguredHabit(first,"","編集後の目標","一歩","取り組む","余裕があれば","check",0,"");s.record(first,today,3,false);check(s.level(first,today)==3,"editing and recording still work at capacity");
        }
        try(Store s=new Store(c,name)){check(s.habits().size()==10&&s.habit(first).goal().equals("編集後の目標"),"ten goals and edit survive reopen");}
        c.deleteDatabase(name);
    }
    int run()throws Exception{
        Context c=i.getTargetContext();check((Build.HARDWARE.contains("ranchu")||Build.HARDWARE.contains("goldfish"))&&c.getPackageName().endsWith(".reportqa"),"dedicated emulator only");
        Store s=Store.get(c);check(s.habits().isEmpty()&&!ReportSettings.configured(c),"fresh app without real data or keys");check(!ReportSettings.featureEnabled(c),"fresh review master remains off");
        storage(c,5,3);for(int chosen:new int[]{3,7,10})storage(c,6,chosen);
        check(s.getReadableDatabase().getVersion()==7,"fresh app uses v7");for(int at=1;at<=3;at++)add(s,"目標テスト "+at);
        long third=s.habits().get(2).id();ReportSettings.prefs(c).edit().putBoolean("habit_enabled_"+third,true).commit();
        MainActivity a=(MainActivity)i.startActivitySync(new Intent(c,MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK));i.waitForIdleSync();
        check(views.find(root(a),"＋ 習慣を追加  ·  3 / 10")!=null,"fourth is available immediately without settings");
        addUi(a,3);check(s.habits().size()==4,"fourth added through real editor without setup");
        for(int at=5;at<=9;at++)add(s,"目標テスト "+at);
        hub.clickTag(a,"nav_settings");check(views.findTag(root(a),"habit_capacity_card")==null&&views.find(root(a),"目標数の上限を変更")==null,"capacity setting removed");
        views.shot(a,"fixed-ten-settings");hub.clickTag(a,"nav_today");addUi(a,9);
        check(s.habits().size()==10&&views.find(root(a),"目標テスト 10")!=null&&views.find(root(a),"登録済み 10個 / 上限 10個")!=null,"tenth is present and fixed maximum shown");
        check(views.find(root(a),"＋ 習慣を追加  ·  10 / 10")==null&&views.find(root(a),"設定で目標数の上限を変更")==null,"no eleventh or capacity setting link");
        rejected(()->add(s,"eleventh"),"fresh API enforces ten");rejected(()->rawAdd(s),"fresh raw SQL enforces ten");
        long tenth=s.habits().get(9).id();s.record(tenth,LocalDate.now(),2,false);
        check(ReportSettings.habitEnabled(c,third)&&!ReportSettings.habitEnabled(c,tenth)&&!ReportSettings.featureEnabled(c),"existing review choice preserved and new goals remain off");
        check(WidgetCollections.rows(c,WidgetCollections.RECORD,true,300).size()==10&&WidgetCollections.rows(c,WidgetCollections.RECORD_COMPACT,true,188).size()==10&&ProgressWidget.snapshot(c).size()==10,"all ten in recording/compact/progress widget datasets");
        hub.clickTag(a,"nav_achievements");for(Store.Habit h:s.habits())check(views.findTag(root(a),"summary_"+h.id())!=null,"each of ten summaries present");
        hub.clickTag(a,"nav_review");check(hub.selector(a,"継続レビューのタスク").getCount()==11,"ten task filters plus All");hub.selectLabel(a,"継続レビューのタスク","目標テスト 10");check(hub.selector(a,"継続レビューのタスク").getSelectedItem().toString().equals("目標テスト 10"),"tenth task selectable");
        MainActivity restored=(MainActivity)views.recreatePage(a);hub.clickTag(restored,"nav_today");check(views.find(root(restored),"目標テスト 10")!=null&&s.level(tenth,LocalDate.now())==2,"ten goals and achievement survive recreation");
        views.shot(restored,"fixed-ten-today");
        s.delete(s.habits().get(0).id());hub.clickTag(restored,"nav_settings");hub.clickTag(restored,"nav_today");check(views.find(root(restored),"＋ 習慣を追加  ·  9 / 10")!=null,"deleting one goal reopens a slot");addUi(restored,9);check(s.habits().size()==10,"freed slot can be reused");
        i.runOnMainSync(restored::finish);return n;
    }
}
