package app.routia;
import android.content.*;
import android.database.Cursor;
import android.database.sqlite.*;
import java.time.*;
import java.util.*;
final class TicketStoreTest {
    int n;void check(boolean value,String label){n++;if(!value)throw new AssertionError(label);}
    void seed(Store s,long id,LocalDate created,LocalDate next,List<LocalDate> days){
        s.getWritableDatabase().execSQL("UPDATE habits SET created=? WHERE id=?",new Object[]{created.toString(),id});
        s.getWritableDatabase().execSQL("UPDATE ticket_state SET next_day=? WHERE habit_id=?",new Object[]{next.toString(),id});
        for(LocalDate day:days)s.getWritableDatabase().execSQL("INSERT INTO records(habit_id,day,level) VALUES(?,?,2)",new Object[]{id,day.toString()});
    }
    int run(Context c){
        String name="routia-auto-ticket-test.db";c.deleteDatabase(name);Store s=new Store(c,name);LocalDate day=LocalDate.of(2026,10,10);
        long id=s.saveHabit(0,"","英語","1文","check",0,"");
        seed(s,id,day.minusDays(9),day.minusDays(6),List.of(day.minusDays(9),day.minusDays(8),day.minusDays(7)));
        s.evaluateTickets(day);check(s.ticketDays(id).equals(Set.of(day.minusDays(6),day.minusDays(5))),"Two automatic tickets only");
        check(s.ticketsUsed(id,YearMonth.from(day))==2,"Monthly budget used");check(s.records(id,day.minusDays(9),day).size()==3,"Tickets are not achievements");
        s.evaluateTickets(day);s.evaluateTickets(day.plusDays(1));check(s.ticketDays(id).size()==2,"Repeated and later evaluation idempotent");
        s.evaluateTickets(day.minusDays(1));check(s.ticketDays(id).size()==2,"Clock rollback cannot refund tickets");
        try{s.getWritableDatabase().execSQL("INSERT INTO tickets VALUES(?,?,?)",new Object[]{id,day.minusDays(4).toString(),day.toString()});throw new AssertionError("Database allowed third ticket");}catch(SQLiteConstraintException expected){n++;}
        long second=s.saveHabit(0,"","読書","1行","check",0,"");LocalDate october=LocalDate.of(2026,10,1);
        seed(s,second,october.minusDays(2),october.minusDays(1),List.of(october.minusDays(2)));
        s.evaluateTickets(october.plusDays(1));check(s.ticket(second,october.minusDays(1))&&s.ticket(second,october),"Month boundary protected");
        check(s.ticketsUsed(second,YearMonth.of(2026,9))==1&&s.ticketsUsed(second,YearMonth.of(2026,10))==1,"Quota belongs to missed day month");
        check(s.ticketsUsed(id,YearMonth.of(2026,10))==2,"Budgets independent by habit");
        s.close();s=new Store(c,name);s.evaluateTickets(october.plusDays(1));check(s.ticketDays(second).size()==2,"Tickets and cursor survive reopen");
        s.delete(second);check(s.ticketDays(second).isEmpty(),"Habit deletion cascades tickets");
        long empty=s.saveHabit(0,"","まだ未達","1回","check",0,"");seed(s,empty,day.minusDays(5),day.minusDays(5),List.of());s.evaluateTickets(day);
        check(s.ticketDays(empty).isEmpty(),"No tickets wasted before streak starts");check(s.ticketDays(id).size()==2,"Other habit tickets retained");
        try(Cursor rows=s.getReadableDatabase().rawQuery("PRAGMA foreign_key_check",null)){check(!rows.moveToFirst(),"Foreign keys intact");}
        s.close();c.deleteDatabase(name);

        name="routia-ticket-v3-migration.db";c.deleteDatabase(name);s=new Store(c,name);LocalDate today=LocalDate.now();
        id=s.saveHabit(0,"","移行前の時間","1分","time",30,"分");s.getWritableDatabase().execSQL("INSERT INTO records(habit_id,day,level,amount,unit,note) VALUES(?,?,3,17.5,'分','残しておくメモ')",new Object[]{id,today.toString()});
        s.getWritableDatabase().execSQL("UPDATE habits SET created=? WHERE id=?",new Object[]{today.minusDays(4).toString(),id});
        for(int offset:new int[]{3,2})s.getWritableDatabase().execSQL("INSERT INTO records(habit_id,day,level) VALUES(?,?,2)",new Object[]{id,today.minusDays(offset).toString()});
        long session=s.newSession(id);s.finishSession(session,60,true);s.getWritableDatabase().execSQL("INSERT INTO pledges(habit_id,goal,week,day,reason,yen) VALUES(?,?,?,?,?,500)",new Object[]{id,"移行前",Rules.week(today).toString(),today.toString(),"test"});s.close();
        SQLiteDatabase old=c.openOrCreateDatabase(name,0,null);old.execSQL("DROP TABLE tickets");old.execSQL("DROP TABLE ticket_state");old.execSQL("ALTER TABLE habits DROP COLUMN full_goal");old.execSQL("ALTER TABLE habits DROP COLUMN extra_goal");old.execSQL("DROP TRIGGER habit_limit");old.execSQL("DROP TABLE IF EXISTS habit_settings");old.execSQL("CREATE TRIGGER habit_limit BEFORE INSERT ON habits WHEN (SELECT count(*) FROM habits)>=3 BEGIN SELECT RAISE(ABORT,'Maximum three habits'); END");old.setVersion(3);old.close();
        s=new Store(c,name);check(s.getReadableDatabase().getVersion()==7,"Version 3 upgrades to 7");check(s.habits().size()==1&&s.entry(id,today).level()==3,"Habit and grade kept");
        check(s.entry(id,today).amount()==17.5&&s.entry(id,today).note().equals("残しておくメモ"),"Numeric details kept");check(s.focused(id,today,today)==60&&s.pledges().size()==1,"Sessions and pledge ledger kept");
        s.evaluateTickets(today);check(s.ticketDays(id).equals(Set.of(today.minusDays(1))),"Upgrade automatically covers yesterday only");
        check(s.records(id,today.minusDays(4),today).size()==3,"Migration ticket does not alter achievements");
        check(Progress.streak(s.records(id,today.minusDays(4),today).keySet(),s.ticketDays(id),today)==3,"Migrated streak bridged without extra day");
        check(!s.recovery(s.habit(id),today),"Protected rest is not a broken-streak return");
        s.close();s=new Store(c,name);s.evaluateTickets(today);check(s.ticketDays(id).size()==1,"Upgrade evaluation not repeated");
        s.delete(id);check(s.ticketDays(id).isEmpty()&&s.pledges().size()==1,"Delete cascades tickets, preserves pledge ledger");s.close();c.deleteDatabase(name);
        return n;
    }
}
