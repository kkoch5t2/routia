package app.routia;

import android.content.*;
import android.database.Cursor;
import android.database.sqlite.*;
import java.time.LocalDate;
import java.util.*;

public final class Store extends SQLiteOpenHelper {
    public record Habit(long id, String cue, String goal, String minimum, LocalDate created, int hour, int minute, boolean reminder) {}
    public record Result(boolean changed, boolean recovery, int level) {}
    private static volatile Store instance;
    public static Store get(Context c) {
        if (instance == null) synchronized (Store.class) { if (instance == null) instance = new Store(c.getApplicationContext()); }
        return instance;
    }
    private Store(Context c) { this(c, "routia.db"); }
    Store(Context c, String databaseName) { super(c, databaseName, null, 8); }
    @Override public void onConfigure(SQLiteDatabase db) { db.setForeignKeyConstraintsEnabled(true); }
    @Override public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE habits(id INTEGER PRIMARY KEY AUTOINCREMENT,cue TEXT NOT NULL,goal TEXT NOT NULL,minimum TEXT NOT NULL,created TEXT NOT NULL,hour INTEGER NOT NULL DEFAULT 21,minute INTEGER NOT NULL DEFAULT 0,reminder INTEGER NOT NULL DEFAULT 0)");
        db.execSQL("CREATE TABLE records(habit_id INTEGER NOT NULL REFERENCES habits(id) ON DELETE CASCADE,day TEXT NOT NULL,level INTEGER NOT NULL CHECK(level IN (1,2,3)),PRIMARY KEY(habit_id,day))");
        habitCapacity(db);
        extras(db);tickets(db);goalSettings(db);habitDescription(db);
    }
    private void extras(SQLiteDatabase db) {
        db.execSQL("ALTER TABLE habits ADD COLUMN metric TEXT NOT NULL DEFAULT 'check'");
        db.execSQL("ALTER TABLE habits ADD COLUMN target REAL NOT NULL DEFAULT 0");
        db.execSQL("ALTER TABLE habits ADD COLUMN unit TEXT NOT NULL DEFAULT ''");
        db.execSQL("ALTER TABLE records ADD COLUMN amount REAL");
        db.execSQL("ALTER TABLE records ADD COLUMN unit TEXT NOT NULL DEFAULT ''");
        db.execSQL("ALTER TABLE records ADD COLUMN note TEXT NOT NULL DEFAULT ''");
        db.execSQL("CREATE TABLE sessions(id INTEGER PRIMARY KEY AUTOINCREMENT,habit_id INTEGER NOT NULL REFERENCES habits(id) ON DELETE CASCADE,day TEXT NOT NULL,seconds INTEGER NOT NULL,completed INTEGER NOT NULL DEFAULT 0)");
        db.execSQL("CREATE TABLE pledges(id INTEGER PRIMARY KEY AUTOINCREMENT,habit_id INTEGER NOT NULL,goal TEXT NOT NULL,week TEXT NOT NULL,day TEXT NOT NULL,reason TEXT NOT NULL,yen INTEGER NOT NULL,paid INTEGER NOT NULL DEFAULT 0,UNIQUE(habit_id,week))");
    }
    @Override public void onUpgrade(SQLiteDatabase db,int oldVersion,int newVersion) { if(oldVersion<2)extras(db);
        if(oldVersion<3){
            db.execSQL("ALTER TABLE records RENAME TO records_v2");
            db.execSQL("CREATE TABLE records(habit_id INTEGER NOT NULL REFERENCES habits(id) ON DELETE CASCADE,day TEXT NOT NULL,level INTEGER NOT NULL CHECK(level IN(1,2,3)),amount REAL,unit TEXT NOT NULL DEFAULT '',note TEXT NOT NULL DEFAULT '',PRIMARY KEY(habit_id,day))");
            db.execSQL("INSERT INTO records(habit_id,day,level,amount,unit,note) SELECT habit_id,day,level,amount,unit,note FROM records_v2");
            db.execSQL("DROP TABLE records_v2");
        } if(oldVersion<4)tickets(db);if(oldVersion<5)goalSettings(db);if(oldVersion<7)habitCapacity(db);if(oldVersion<8)habitDescription(db); }

    private void habitCapacity(SQLiteDatabase db){
        db.execSQL("DROP TRIGGER IF EXISTS habit_limit");
        db.execSQL("DROP TABLE IF EXISTS habit_settings");
        db.execSQL("CREATE TRIGGER habit_limit BEFORE INSERT ON habits WHEN (SELECT count(*) FROM habits)>="+Rules.MAX_HABITS+" BEGIN SELECT RAISE(ABORT,'Maximum ten habits'); END");
    }

    private void goalSettings(SQLiteDatabase db){
        db.execSQL("ALTER TABLE habits ADD COLUMN full_goal TEXT NOT NULL DEFAULT ''");
        db.execSQL("ALTER TABLE habits ADD COLUMN extra_goal TEXT NOT NULL DEFAULT ''");
        // Existing titles were the previous normal goal. Preserve that meaning and every record.
        db.execSQL("UPDATE habits SET full_goal=goal");
    }
    /** A permanent, optional instruction for the goal, separate from daily recording notes. */
    private void habitDescription(SQLiteDatabase db){
        db.execSQL("ALTER TABLE habits ADD COLUMN description TEXT NOT NULL DEFAULT ''");
    }
    public String description(long id){
        if(id<=0)return "";
        try(Cursor c=getReadableDatabase().rawQuery("SELECT description FROM habits WHERE id=?",new String[]{""+id})){
            return c.moveToFirst()?c.getString(0):"";
        }
    }
    public record Goals(String minimum,String full,String extra){}
    public Goals goals(long id){
        Habit h=habit(id);if(h==null)return new Goals("","","");
        try(Cursor c=getReadableDatabase().rawQuery("SELECT full_goal,extra_goal FROM habits WHERE id=?",new String[]{""+id})){
            return c.moveToFirst()?new Goals(HabitDefaults.minimum(h.minimum()),HabitDefaults.full(c.getString(0)),HabitDefaults.extra(c.getString(1))):new Goals(HabitDefaults.minimum(h.minimum()),HabitDefaults.FULL,HabitDefaults.EXTRA);
        }
    }
    /** Legacy callers must not clear an already-saved description. */
    public long saveConfiguredHabit(long id,String cue,String goal,String minimum,String full,String extra,String kind,double target,String unit){
        return saveConfiguredHabit(id,cue,goal,minimum,full,extra,kind,target,unit,description(id));
    }
    public long saveConfiguredHabit(long id,String cue,String goal,String minimum,String full,String extra,String kind,double target,String unit,String description){
        description=Inputs.clean(description);
        if(description.length()>1000)throw new IllegalArgumentException("やることの詳細は1000文字以内で入力してください");
        goal=HabitDefaults.title(goal);unit=HabitDefaults.unit(kind,unit);minimum=kind.equals("check")?HabitDefaults.minimum(minimum):Inputs.clean(minimum);full=HabitDefaults.full(full);extra=HabitDefaults.extra(extra);
        if(full.length()>120||extra.length()>120)throw new IllegalArgumentException("基準は120文字以内で入力してください");
        SQLiteDatabase db=getWritableDatabase();db.beginTransaction();
        try{long saved=saveHabit(id,cue,goal,minimum,kind,target,unit);ContentValues v=new ContentValues();v.put("full_goal",kind.equals("check")?full:"");v.put("extra_goal",kind.equals("check")?extra:"");v.put("description",description);db.update("habits",v,"id=?",new String[]{""+saved});db.setTransactionSuccessful();return saved;}finally{db.endTransaction();}
    }
    private void tickets(SQLiteDatabase db){
        db.execSQL("CREATE TABLE IF NOT EXISTS tickets(habit_id INTEGER NOT NULL REFERENCES habits(id) ON DELETE CASCADE,day TEXT NOT NULL,used_on TEXT NOT NULL,PRIMARY KEY(habit_id,day))");
        db.execSQL("CREATE TABLE IF NOT EXISTS ticket_state(habit_id INTEGER PRIMARY KEY REFERENCES habits(id) ON DELETE CASCADE,next_day TEXT NOT NULL)");
        db.execSQL("CREATE TRIGGER IF NOT EXISTS ticket_limit BEFORE INSERT ON tickets WHEN (SELECT count(*) FROM tickets WHERE habit_id=NEW.habit_id AND substr(day,1,7)=substr(NEW.day,1,7))>=2 BEGIN SELECT RAISE(ABORT,'Monthly ticket limit'); END");
        String begin=LocalDate.now().minusDays(1).toString();
        db.execSQL("INSERT OR IGNORE INTO ticket_state(habit_id,next_day) SELECT id,CASE WHEN created>? THEN created ELSE ? END FROM habits",new Object[]{begin,begin});
    }
    public Set<LocalDate> ticketDays(long id){
        Set<LocalDate> days=new HashSet<>();try(Cursor c=getReadableDatabase().rawQuery("SELECT day FROM tickets WHERE habit_id=?",new String[]{""+id})){while(c.moveToNext())days.add(LocalDate.parse(c.getString(0)));}return days;
    }
    public boolean ticket(long id,LocalDate day){
        try(Cursor c=getReadableDatabase().rawQuery("SELECT 1 FROM tickets WHERE habit_id=? AND day=?",new String[]{""+id,day.toString()})){return c.moveToFirst();}
    }
    public int ticketsUsed(long id,java.time.YearMonth month){
        try(Cursor c=getReadableDatabase().rawQuery("SELECT count(*) FROM tickets WHERE habit_id=? AND day>=? AND day<=?",new String[]{""+id,month.atDay(1).toString(),month.atEndOfMonth().toString()})){c.moveToFirst();return c.getInt(0);}
    }
    public void evaluateTickets(LocalDate today){
        SQLiteDatabase db=getWritableDatabase();
        try(Cursor pending=db.rawQuery("SELECT 1 FROM ticket_state WHERE next_day<? LIMIT 1",new String[]{today.toString()})){if(!pending.moveToFirst())return;}
        db.beginTransaction();
        try{
            for(Habit h:habits()){
                LocalDate start;try(Cursor state=db.rawQuery("SELECT next_day FROM ticket_state WHERE habit_id=?",new String[]{""+h.id()})){if(!state.moveToFirst())continue;start=LocalDate.parse(state.getString(0));}
                if(!start.isBefore(today))continue;if(start.isBefore(h.created()))start=h.created();
                Set<LocalDate> achieved=new HashSet<>(records(h.id(),h.created(),today.minusDays(1)).keySet());
                for(LocalDate day:TicketRules.plan(start,today,achieved,ticketDays(h.id()))){
                    ContentValues v=new ContentValues();v.put("habit_id",h.id());v.put("day",day.toString());v.put("used_on",today.toString());db.insertOrThrow("tickets",null,v);
                }
                ContentValues next=new ContentValues();next.put("next_day",today.toString());db.update("ticket_state",next,"habit_id=?",new String[]{""+h.id()});
            }
            db.setTransactionSuccessful();
        }finally{db.endTransaction();}
    }

    public record Metric(String kind,double target,String unit) {}
    public record Entry(LocalDate day,int level,Double amount,String unit,String note) {}
    public record Pledge(long id,String goal,String day,String reason,int yen,boolean paid) {}
    public Metric metric(long id) {
        try(Cursor c=getReadableDatabase().rawQuery("SELECT metric,target,unit FROM habits WHERE id=?",new String[]{""+id})) {
            return c.moveToFirst()?new Metric(c.getString(0),c.getDouble(1),c.getString(2)):new Metric("check",0,"");
        }
    }
    public long saveHabit(long id,String cue,String goal,String minimum,String kind,double target,String unit) {
        unit=Inputs.clean(unit);
        if(!Arrays.asList("check","time","count").contains(kind)||!Double.isFinite(target)||target<0||target>1000000||unit.length()>8||kind.equals("count")&&unit.trim().isEmpty())throw new IllegalArgumentException("目標の数値を確認してください");
        Metric old=metric(id);
        if(id>0&&hasAmounts(id)&&(!old.kind().equals(kind)||!old.unit().equals(unit)))throw new IllegalArgumentException("数値記録があるため、記録方法と単位は変更できません。新しい習慣として登録してください。");
        SQLiteDatabase db=getWritableDatabase();db.beginTransaction();
        try {long saved=save(id,cue,goal,minimum,21,0,true);ContentValues v=new ContentValues();v.put("metric",kind);if(id==0)v.put("full_goal",kind.equals("check")?Inputs.clean(goal):"");v.put("target",target);v.put("unit",unit);db.update("habits",v,"id=?",new String[]{""+saved});db.setTransactionSuccessful();return saved;}finally{db.endTransaction();}
    }
    private boolean hasAmounts(long id) {
        try(Cursor c=getReadableDatabase().rawQuery("SELECT 1 FROM records WHERE habit_id=? AND amount IS NOT NULL LIMIT 1",new String[]{""+id})){return c.moveToFirst();}
    }
    public Entry entry(long id,LocalDate day) {
        try(Cursor c=getReadableDatabase().rawQuery("SELECT level,amount,unit,note FROM records WHERE habit_id=? AND day=?",new String[]{""+id,day.toString()})) {
            return c.moveToFirst()?new Entry(day,c.getInt(0),c.isNull(1)?null:c.getDouble(1),c.getString(2),c.getString(3)):new Entry(day,0,null,"","");
        }
    }
    public List<Entry> entries(long id,LocalDate start,LocalDate end) {
        List<Entry> a=new ArrayList<>();
        try(Cursor c=getReadableDatabase().rawQuery("SELECT day,level,amount,unit,note FROM records WHERE habit_id=? AND day>=? AND day<=? ORDER BY day DESC",new String[]{""+id,start.toString(),end.toString()})) {
            while(c.moveToNext())a.add(new Entry(LocalDate.parse(c.getString(0)),c.getInt(1),c.isNull(2)?null:c.getDouble(2),c.getString(3),c.getString(4)));
        }return a;
    }
    public void details(long id,LocalDate day,Double amount,String note) {
        if(!day.equals(LocalDate.now())||level(id,day)==0||note.length()>1000||amount!=null&&(!Double.isFinite(amount)||amount<0||amount>1000000))throw new IllegalArgumentException("数値とメモを確認してください");
        ContentValues v=new ContentValues();if(amount==null)v.putNull("amount");else v.put("amount",amount);v.put("unit",metric(id).unit());v.put("note",note.trim());
        getWritableDatabase().update("records",v,"habit_id=? AND day=?",new String[]{""+id,day.toString()});
    }
    public long newSession(long id) {
        if(habit(id)==null)throw new IllegalArgumentException("習慣が見つかりません");
        ContentValues v=new ContentValues();v.put("habit_id",id);v.put("day",LocalDate.now().toString());v.put("seconds",0);return getWritableDatabase().insertOrThrow("sessions",null,v);
    }
    public boolean finishSession(long session,int seconds,boolean completed) {
        ContentValues v=new ContentValues();v.put("seconds",Math.max(0,seconds));v.put("completed",completed?1:-1);
        return getWritableDatabase().update("sessions",v,"id=? AND completed=0",new String[]{""+session})==1;
    }
    public int focused(long id,LocalDate start,LocalDate end) {
        try(Cursor c=getReadableDatabase().rawQuery("SELECT COALESCE(SUM(seconds),0) FROM sessions WHERE habit_id=? AND day>=? AND day<=? AND completed=1",new String[]{""+id,start.toString(),end.toString()})){c.moveToFirst();return c.getInt(0);}
    }
    public void evaluatePledges(LocalDate today,LocalDate enabled,int yen) {
        if(yen<=0)return;
        SQLiteDatabase db=getWritableDatabase();db.beginTransaction();try{
            for(Habit h:habits()) {
                LocalDate begin=h.created().isAfter(enabled)?h.created():enabled;
                Map<LocalDate,Integer> records=records(h.id(),begin.minusDays(1),today.minusDays(1));
                for(LocalDate d:Rules.penaltyDays(begin,today,records)) {
                    ContentValues v=new ContentValues();v.put("habit_id",h.id());v.put("goal",h.goal());v.put("week",Rules.week(d).toString());v.put("day",d.toString());v.put("reason","2日連続の未達 または 週2日の未達");v.put("yen",yen);db.insertWithOnConflict("pledges",null,v,SQLiteDatabase.CONFLICT_IGNORE);
                }
            }db.setTransactionSuccessful();
        }finally{db.endTransaction();}
    }
    public List<Pledge> pledges() {
        List<Pledge> a=new ArrayList<>();try(Cursor c=getReadableDatabase().rawQuery("SELECT id,goal,day,reason,yen,paid FROM pledges ORDER BY day DESC,id DESC",null)) {
            while(c.moveToNext())a.add(new Pledge(c.getLong(0),c.getString(1),c.getString(2),c.getString(3),c.getInt(4),c.getInt(5)==1));
        }return a;
    }
    public void paid(long id) {ContentValues v=new ContentValues();v.put("paid",1);getWritableDatabase().update("pledges",v,"id=?",new String[]{""+id});}

    public List<Habit> habits() {
        List<Habit> all = new ArrayList<>();
        try(Cursor c = getReadableDatabase().rawQuery("SELECT * FROM habits ORDER BY id", null)) { while(c.moveToNext()) all.add(read(c)); }
        return all;
    }
    private Habit read(Cursor c) { return new Habit(c.getLong(0),c.getString(1),c.getString(2),c.getString(3),LocalDate.parse(c.getString(4)),c.getInt(5),c.getInt(6),c.getInt(7)==1); }
    public Habit habit(long id) { for(Habit h:habits()) if(h.id()==id) return h; return null; }
    public long save(long id,String cue,String goal,String minimum,int hour,int minute,boolean reminder) {
        cue=Inputs.clean(cue); goal=Inputs.clean(goal); minimum=Inputs.clean(minimum);
        if(goal.isEmpty()||cue.length()>120||goal.length()>120||minimum.length()>120||hour<0||hour>23||minute<0||minute>59) throw new IllegalArgumentException("入力内容を確認してください");
        ContentValues v=new ContentValues(); v.put("cue",cue);v.put("goal",goal);v.put("minimum",minimum);v.put("hour",hour);v.put("minute",minute);v.put("reminder",reminder?1:0);
        if(id==0) { v.put("created",LocalDate.now().toString()); SQLiteDatabase db=getWritableDatabase();db.beginTransaction();try{if(android.database.DatabaseUtils.longForQuery(db,"SELECT count(*) FROM habits",null)>=Rules.MAX_HABITS)throw new IllegalArgumentException("目標は10個まで登録できます。登録済みの目標を見直してください。");long saved=db.insertOrThrow("habits",null,v);ContentValues state=new ContentValues();state.put("habit_id",saved);state.put("next_day",LocalDate.now().toString());db.insertOrThrow("ticket_state",null,state);db.setTransactionSuccessful();return saved;}finally{db.endTransaction();} }
        if(getWritableDatabase().update("habits",v,"id=?",new String[]{""+id})!=1) throw new IllegalArgumentException("習慣が見つかりません");
        return id;
    }
    public void delete(long id) { getWritableDatabase().delete("habits","id=?",new String[]{""+id}); }
    public int level(long id,LocalDate day) {
        try(Cursor c=getReadableDatabase().rawQuery("SELECT level FROM records WHERE habit_id=? AND day=?",new String[]{""+id,day.toString()})) { return c.moveToFirst()?c.getInt(0):Rules.NONE; }
    }
    public Map<LocalDate,Integer> records(long id, LocalDate start,LocalDate end) {
        Map<LocalDate,Integer> result=new HashMap<>();
        try(Cursor c=getReadableDatabase().rawQuery("SELECT day,level FROM records WHERE habit_id=? AND day>=? AND day<=?",new String[]{""+id,start.toString(),end.toString()})) { while(c.moveToNext()) result.put(LocalDate.parse(c.getString(0)),c.getInt(1)); }
        return result;
    }
    public boolean recovery(Habit h,LocalDate date) { return !ticket(h.id(),date.minusDays(1))&&Rules.recovery(h.created(),date,level(h.id(),date.minusDays(1))); }
    /** Manual achievement selection belongs only to check habits. Old numeric widget actions are rejected. */
    public Result record(long id,LocalDate day,int requested,boolean correction){
        if(requested!=0&&!metric(id).kind().equals("check"))throw new IllegalArgumentException("この習慣は時間・数量を入力して記録してください");
        return recordLevel(id,day,requested,correction);
    }
    /** Set today's total, or append an idempotently completed timer. Zero removes the achievement. */
    public Result recordAmount(long id,LocalDate day,double amount,String note,boolean append){
        if(!day.equals(LocalDate.now())||!Double.isFinite(amount)||amount<0||amount>1000000)throw new IllegalArgumentException("今日の数値を確認してください");
        SQLiteDatabase db=getWritableDatabase();db.beginTransaction();
        try{
            Habit habit=habit(id);Metric metric=metric(id);
            if(habit==null||metric.kind().equals("check"))throw new IllegalArgumentException("数値を記録する習慣を選んでください");
            Entry before=entry(id,day);double total=append?(before.amount()==null?0:before.amount())+amount:amount;
            int level=Rules.amountLevel(total,metric.target());String savedNote=note==null?before.note():note.trim();
            if(savedNote.length()>1000)throw new IllegalArgumentException("メモは1000文字以内で入力してください");
            Result result=recordLevel(id,day,level,!append||level==0);
            if(level>0)details(id,day,total,savedNote);
            db.setTransactionSuccessful();return result;
        }finally{db.endTransaction();}
    }
    private Result recordLevel(long id,LocalDate day,int requested,boolean correction) {
        if(!day.equals(LocalDate.now())) throw new IllegalArgumentException("今日の記録だけ変更できます");
        evaluateTickets(LocalDate.now());SQLiteDatabase db=getWritableDatabase();db.beginTransaction();
        try {
            Habit h=habit(id);if(h==null||day.isBefore(h.created())) throw new IllegalArgumentException("習慣が見つかりません");
            int before=level(id,day);
            int after=correction ? requested : Rules.merge(before,requested);
            if(after<0||after>3) throw new IllegalArgumentException("Invalid achievement");
            boolean recovery=recovery(h,day);
            if(after==0) db.delete("records","habit_id=? AND day=?",new String[]{""+id,day.toString()});
            else { ContentValues v=new ContentValues();v.put("level",after);
                if(before>0)db.update("records",v,"habit_id=? AND day=?",new String[]{""+id,day.toString()});
                else{v.put("habit_id",id);v.put("day",day.toString());db.insertOrThrow("records",null,v);}
            }
            db.setTransactionSuccessful();return new Result(before!=after,recovery&&before==0&&after>0,after);
        } finally { db.endTransaction(); }
    }
}
