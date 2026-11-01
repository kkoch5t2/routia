package app.routia;
import android.content.*;
import android.database.Cursor;
import android.database.sqlite.*;
import java.time.*;
final class GradeMigrationTest {
    int n;void check(boolean ok,String label){n++;if(!ok)throw new AssertionError(label);}
    int run(Context c){
        String name="routia-grade-v2-test.db";c.deleteDatabase(name);LocalDate today=LocalDate.now();Store v2=new Store(c,name);
        long id=v2.saveHabit(0,"","移行前の時間","1分","time",30,"分");v2.getWritableDatabase().execSQL("INSERT INTO records(habit_id,day,level,amount,unit,note) VALUES(?,?,2,17.5,'分','消してはいけないメモ')",new Object[]{id,today.toString()});long session=v2.newSession(id);v2.finishSession(session,60,true);v2.getWritableDatabase().execSQL("INSERT INTO pledges(habit_id,goal,week,day,reason,yen,paid) VALUES(?,?,?,?,?,500,0)",new Object[]{id,"移行前の時間",Rules.week(today).toString(),today.toString(),"test"});v2.close();
        SQLiteDatabase old=c.openOrCreateDatabase(name,0,null);old.execSQL("ALTER TABLE records RENAME TO grade_records");old.execSQL("CREATE TABLE records(habit_id INTEGER NOT NULL REFERENCES habits(id) ON DELETE CASCADE,day TEXT NOT NULL,level INTEGER NOT NULL CHECK(level IN(1,2)),amount REAL,unit TEXT NOT NULL DEFAULT '',note TEXT NOT NULL DEFAULT '',PRIMARY KEY(habit_id,day))");old.execSQL("INSERT INTO records SELECT * FROM grade_records");old.execSQL("DROP TABLE grade_records");old.execSQL("ALTER TABLE habits DROP COLUMN description");old.execSQL("ALTER TABLE habits DROP COLUMN full_goal");old.execSQL("ALTER TABLE habits DROP COLUMN extra_goal");old.execSQL("DROP TRIGGER habit_limit");old.execSQL("DROP TABLE IF EXISTS habit_settings");old.execSQL("CREATE TRIGGER habit_limit BEFORE INSERT ON habits WHEN (SELECT count(*) FROM habits)>=3 BEGIN SELECT RAISE(ABORT,'Maximum three habits'); END");old.setVersion(2);old.close();
        Store s=new Store(c,name);check(s.getReadableDatabase().getVersion()==8,"Schema upgraded");check(s.habits().size()==1,"Habit kept");check(s.entry(id,today).level()==2,"Grade kept");check(s.entry(id,today).amount()==17.5,"Amount kept");check(s.entry(id,today).note().equals("消してはいけないメモ"),"Note kept");check(s.metric(id).target()==30,"Target kept");check(s.focused(id,today,today)==60,"Focus session kept");check(s.pledges().size()==1&&s.pledges().get(0).yen()==500,"Pledge kept");
        try{s.record(id,today,3,false);throw new AssertionError("Numeric grade action allowed");}catch(IllegalArgumentException expected){n++;}
        check(s.entry(id,today).amount()==17.5&&s.entry(id,today).note().equals("消してはいけないメモ")&&s.level(id,today)==2,"Rejected old action keeps legacy details");
        check(s.recordAmount(id,today,10,null,false).level()==1,"Numeric total correction derives grade");
        long binary=s.saveHabit(0,"","チェックの習慣","1回","check",0,"");check(s.record(binary,today,3,false).level()==3,"Check extra grade remains");s.record(binary,today,1,false);s.record(binary,today,2,false);check(s.level(binary,today)==3,"Check background action cannot downgrade");check(s.record(binary,today,1,true).level()==1,"Explicit check correction allowed");
        try{s.record(binary,today,4,true);throw new AssertionError("Invalid grade allowed");}catch(IllegalArgumentException expected){n++;}
        try(Cursor rows=s.getReadableDatabase().rawQuery("PRAGMA foreign_key_check",null)){check(!rows.moveToFirst(),"Foreign keys intact");}
        s.delete(id);check(s.focused(id,today,today)==0,"Cascade intact");s.close();c.deleteDatabase(name);return n;
    }
}
