package app.routia;
import android.content.*;
import android.database.sqlite.*;
import java.time.*;
final class FeaturesTest {
    private int count;private void check(boolean ok,String msg){count++;if(!ok)throw new AssertionError(msg);}
    int run(Context c){
        String name="routia-v2-migration-test.db";c.deleteDatabase(name);
        SQLiteDatabase legacy=c.openOrCreateDatabase(name,0,null);
        legacy.execSQL("CREATE TABLE habits(id INTEGER PRIMARY KEY AUTOINCREMENT,cue TEXT NOT NULL,goal TEXT NOT NULL,minimum TEXT NOT NULL,created TEXT NOT NULL,hour INTEGER NOT NULL DEFAULT 21,minute INTEGER NOT NULL DEFAULT 0,reminder INTEGER NOT NULL DEFAULT 0)");
        legacy.execSQL("CREATE TABLE records(habit_id INTEGER NOT NULL REFERENCES habits(id) ON DELETE CASCADE,day TEXT NOT NULL,level INTEGER NOT NULL CHECK(level IN(1,2)),PRIMARY KEY(habit_id,day))");
        legacy.execSQL("CREATE TRIGGER habit_limit BEFORE INSERT ON habits WHEN (SELECT count(*) FROM habits)>=3 BEGIN SELECT RAISE(ABORT,'Maximum three habits'); END");
        LocalDate today=LocalDate.now();legacy.execSQL("INSERT INTO habits VALUES(1,'旧きっかけ','旧習慣','1文',?,21,0,0)",new Object[]{today.minusDays(14).toString()});legacy.execSQL("INSERT INTO records VALUES(1,?,2)",new Object[]{today.minusDays(1).toString()});legacy.setVersion(1);legacy.close();
        Store s=new Store(c,name);check(s.habits().size()==1,"Migration keeps habits");check(s.level(1,today.minusDays(1))==2,"Migration keeps records");check(s.metric(1).kind().equals("check"),"Legacy metric is optional");
        long id=s.saveHabit(0,"","走る","","time",30,"分");check(s.habit(id).cue().isEmpty(),"Cue optional");check(s.habit(id).minimum().isEmpty(),"No invented minimum criterion");check(s.metric(id).target()==30,"Target stored");
        s.recordAmount(id,today,12.5,"川沿いを走った",false);check(s.entry(id,today).level()==1,"Positive effort counts below optional target");s.recordAmount(id,today,17.5,null,true);check(s.entry(id,today).amount()==30,"Timer append reaches target");check(s.entry(id,today).note().equals("川沿いを走った"),"Timer append preserves note");s.recordAmount(id,today,0,null,true);check(s.entry(id,today).level()==2,"Repeated zero append cannot downgrade");try{s.record(id,today,2,false);throw new AssertionError("Numeric grade-only action accepted");}catch(IllegalArgumentException expected){count++;}
        try{s.details(id,today,Double.NaN,"");throw new AssertionError("NaN accepted");}catch(IllegalArgumentException expected){count++;}
        try{s.details(id,today,-1d,"");throw new AssertionError("Negative accepted");}catch(IllegalArgumentException expected){count++;}
        try{s.saveHabit(id,"","走る","","count",10,"回");throw new AssertionError("Mixed units accepted");}catch(IllegalArgumentException expected){count++;}
        s.saveHabit(id,"","走る改","","time",60,"分");check(s.entry(id,today).amount()==30,"Goal edits preserve history");
        long session=s.newSession(id);check(s.finishSession(session,120,true),"Focus first completion");check(!s.finishSession(session,120,true),"Focus completion idempotent");check(s.focused(id,today,today)==120,"Focus seconds not doubled");
        long abort=s.newSession(id);s.finishSession(abort,50,false);check(s.focused(id,today,today)==120,"Interrupted session not full focus");
        s.record(id,today,0,true);check(s.entry(id,today).amount()==null&&s.entry(id,today).note().isEmpty(),"Undo clears only daily details");check(s.focused(id,today,today)==120,"Undo does not erase focus sessions");
        s.evaluatePledges(today,today,500);check(s.pledges().isEmpty(),"No penalties for today");s.evaluatePledges(today,today.minusDays(14),500);int n=s.pledges().size();check(n>0,"Concluded missed days create pledges");s.evaluatePledges(today,today.minusDays(14),500);check(s.pledges().size()==n,"Pledges idempotent");
        long pledge=s.pledges().get(0).id();s.paid(pledge);s.paid(pledge);check(s.pledges().stream().anyMatch(p->p.id()==pledge&&p.paid()),"Manual status idempotent");
        s.delete(1);check(s.pledges().size()==n,"Deleting habit preserves pledge log");s.delete(id);check(s.focused(id,today,today)==0,"Deleting habit cascades focus history");s.close();c.deleteDatabase(name);return count;
    }
}
