package app.routia;

import android.content.*;
import android.database.Cursor;
import android.database.sqlite.*;
import java.time.*;
import java.util.*;

final class ReportStore extends SQLiteOpenHelper {
    record Item(long id,long habit,String week,String goal,String snapshot,String result,String state,String problem,double reserved,double estimated,String created,String model,int budget,boolean manual,String detail,long completedAt,boolean notified) {}
    ReportStore(Context c){this(c,"routia-reports.db");}
    ReportStore(Context c,String name){super(c,name,null,4);}
    @Override public void onCreate(SQLiteDatabase db){
        db.execSQL("CREATE TABLE reports(id INTEGER PRIMARY KEY AUTOINCREMENT,habit INTEGER NOT NULL,week TEXT NOT NULL,goal TEXT NOT NULL,snapshot TEXT NOT NULL,result TEXT NOT NULL DEFAULT '',state TEXT NOT NULL DEFAULT 'queued',problem TEXT NOT NULL DEFAULT '',reserved REAL NOT NULL DEFAULT 0,estimated REAL NOT NULL DEFAULT 0,requested INTEGER NOT NULL DEFAULT 0,model TEXT NOT NULL DEFAULT '',budget INTEGER NOT NULL DEFAULT 0,detail TEXT NOT NULL DEFAULT '',completed_at INTEGER NOT NULL DEFAULT 0,notified INTEGER NOT NULL DEFAULT 0,created TEXT NOT NULL,UNIQUE(habit,week))");
    }
    @Override public void onUpgrade(SQLiteDatabase db,int old,int next){if(old<2){db.execSQL("ALTER TABLE reports ADD COLUMN model TEXT NOT NULL DEFAULT ''");db.execSQL("ALTER TABLE reports ADD COLUMN budget INTEGER NOT NULL DEFAULT 0");}if(old<3)db.execSQL("ALTER TABLE reports ADD COLUMN detail TEXT NOT NULL DEFAULT ''");if(old<4){db.execSQL("ALTER TABLE reports ADD COLUMN completed_at INTEGER NOT NULL DEFAULT 0");db.execSQL("ALTER TABLE reports ADD COLUMN notified INTEGER NOT NULL DEFAULT 0");}}
    @Override public void onOpen(SQLiteDatabase db){
        super.onOpen(db);
        if(!db.isReadOnly())db.execSQL("CREATE INDEX IF NOT EXISTS reports_catalog_completed ON reports(state,completed_at DESC,id DESC,habit)");
    }
    private record CatalogScope(String where,List<String> args) {}
    private CatalogScope catalogScope(List<Long> allowed,long selected){
        List<Long> ids=selected==0?allowed:(allowed.contains(selected)?List.of(selected):List.of());
        List<String> args=new ArrayList<>();for(long id:ids)args.add(Long.toString(id));
        String where=ids.isEmpty()?"0":"state='complete' AND habit IN ("+String.join(",",Collections.nCopies(ids.size(),"?"))+")";
        return new CatalogScope(where,args);
    }
    List<Integer> catalogYears(List<Long> allowed,long habit,ZoneId zone){
        CatalogScope scope=catalogScope(allowed,habit);
        Set<Integer> years=new TreeSet<>(Comparator.reverseOrder());boolean unknown=false;
        // Stream only timestamps to build the small year selector, never report bodies.
        try(Cursor c=getReadableDatabase().rawQuery("SELECT DISTINCT completed_at FROM reports WHERE "+scope.where(),scope.args().toArray(new String[0]))){
            while(c.moveToNext()){long time=c.getLong(0);if(time<=0)unknown=true;else years.add(Instant.ofEpochMilli(time).atZone(zone).getYear());}
        }
        List<Integer> result=new ArrayList<>(years);if(unknown)result.add(-1);return result;
    }
    List<Integer> catalogMonths(List<Long> allowed,long habit,int year,ZoneId zone){
        if(year<=0)return Collections.emptyList();
        CatalogScope scope=catalogScope(allowed,habit);ReportCatalog.Range range=ReportCatalog.range(year,0,zone);
        List<String> args=new ArrayList<>(scope.args());args.add(Long.toString(range.start()));args.add(Long.toString(range.end()));
        Set<Integer> months=new TreeSet<>();
        // Only completed timestamps in the selected local year/task scope; never report bodies.
        try(Cursor c=getReadableDatabase().rawQuery("SELECT DISTINCT completed_at FROM reports WHERE "+scope.where()+" AND completed_at>=? AND completed_at<?",args.toArray(new String[0]))){
            while(c.moveToNext())months.add(Instant.ofEpochMilli(c.getLong(0)).atZone(zone).getMonthValue());
        }
        return new ArrayList<>(months);
    }
    ReportCatalog.Page catalogPage(List<Long> allowed,long habit,int year,int month,long requested,ZoneId zone){
        CatalogScope scope=catalogScope(allowed,habit);String where=scope.where();List<String> args=new ArrayList<>(scope.args());
        if(year==-1)where+=" AND completed_at<=0";
        else if(year>0){
            ReportCatalog.Range range=ReportCatalog.range(year,month,zone);
            where+=" AND completed_at>=? AND completed_at<?";args.add(Long.toString(range.start()));args.add(Long.toString(range.end()));
        }
        SQLiteDatabase db=getReadableDatabase();db.beginTransactionNonExclusive();
        try{
            long total;try(Cursor c=db.rawQuery("SELECT COUNT(*) FROM reports WHERE "+where,args.toArray(new String[0]))){c.moveToFirst();total=c.getLong(0);}
            long index=ReportCatalog.pageIndex(requested,total);List<ReportCatalog.Entry> entries=new ArrayList<>();
            List<String> pageArgs=new ArrayList<>(args);pageArgs.add(Integer.toString(ReportCatalog.PAGE_SIZE));pageArgs.add(Long.toString(index*ReportCatalog.PAGE_SIZE));
            try(Cursor c=db.rawQuery("SELECT id,habit,goal,completed_at,week,detail,estimated FROM reports WHERE "+where+" ORDER BY completed_at DESC,id DESC LIMIT ? OFFSET ?",pageArgs.toArray(new String[0]))){
                while(c.moveToNext())entries.add(new ReportCatalog.Entry(c.getLong(0),c.getLong(1),c.getString(2),c.getLong(3),c.getString(4),c.getString(5),c.getDouble(6)));
            }
            db.setTransactionSuccessful();return new ReportCatalog.Page(total,index,entries);
        }finally{db.endTransaction();}
    }
    long enqueue(long habit,String week,String goal,String snapshot){return enqueue(habit,week,goal,snapshot,false);}
    long enqueue(long habit,String week,String goal,String snapshot,boolean requested){
        ContentValues v=new ContentValues();v.put("habit",habit);v.put("week",week);v.put("goal",goal);v.put("snapshot",snapshot);v.put("requested",requested?1:0);v.put("created",LocalDateTime.now().toString());
        long id=getWritableDatabase().insertWithOnConflict("reports",null,v,SQLiteDatabase.CONFLICT_IGNORE);
        if(id>=0)return id;
        if(requested){ContentValues manual=new ContentValues();manual.put("requested",1);getWritableDatabase().update("reports",manual,"habit=? AND week=? AND state='queued'",new String[]{""+habit,week});}
        try(Cursor c=getReadableDatabase().rawQuery("SELECT id FROM reports WHERE habit=? AND week=?",new String[]{""+habit,week})){return c.moveToFirst()?c.getLong(0):-1;}
    }
    List<Item> all(long habit){
        List<Item> items=new ArrayList<>();
        try(Cursor c=getReadableDatabase().rawQuery("SELECT id,habit,week,goal,snapshot,result,state,problem,reserved,estimated,created,model,budget,requested,detail,completed_at,notified FROM reports WHERE habit=? ORDER BY id DESC",new String[]{""+habit})){while(c.moveToNext())items.add(read(c));}
        return items;
    }
    List<ReportCatalog.Entry> completedCatalog(){
        List<ReportCatalog.Entry> entries=new ArrayList<>();
        try(Cursor c=getReadableDatabase().rawQuery("SELECT id,habit,goal,completed_at,week,detail,estimated FROM reports WHERE state='complete' ORDER BY completed_at DESC,id DESC",null)){
            while(c.moveToNext())entries.add(new ReportCatalog.Entry(c.getLong(0),c.getLong(1),c.getString(2),c.getLong(3),c.getString(4),c.getString(5),c.getDouble(6)));
        }return entries;
    }
    Item item(long id){
        try(Cursor c=getReadableDatabase().rawQuery("SELECT id,habit,week,goal,snapshot,result,state,problem,reserved,estimated,created,model,budget,requested,detail,completed_at,notified FROM reports WHERE id=?",new String[]{""+id})){return c.moveToFirst()?read(c):null;}
    }
    private Item read(Cursor c){return new Item(c.getLong(0),c.getLong(1),c.getString(2),c.getString(3),c.getString(4),c.getString(5),c.getString(6),c.getString(7),c.getDouble(8),c.getDouble(9),c.getString(10),c.getString(11),c.getInt(12),c.getInt(13)!=0,c.getString(14),c.getLong(15),c.getInt(16)!=0);}
    Item next(boolean automatic){
        try(Cursor c=getReadableDatabase().rawQuery("SELECT id FROM reports WHERE state='queued'"+(automatic?"":" AND requested=1")+" ORDER BY id LIMIT 1",null)){return c.moveToFirst()?item(c.getLong(0)):null;}
    }
    boolean confirmManual(long id,ReportBudget.Options options){
        ContentValues v=new ContentValues();v.put("model",options.model());v.put("budget",options.limitYen());v.put("detail",options.detail().id);
        v.put("requested",1);v.put("state","queued");v.put("problem","");
        return getWritableDatabase().update("reports",v,"id=? AND reserved=0 AND state IN ('queued','stopped')",new String[]{""+id})==1;
    }
    boolean claim(long id){
        ContentValues v=new ContentValues();v.put("state","counting");
        return getWritableDatabase().update("reports",v,"id=? AND state='queued'",new String[]{""+id})==1;
    }
    boolean recordOptions(long id,ReportBudget.Options options){
        ContentValues v=new ContentValues();v.put("model",options.model());v.put("budget",options.limitYen());v.put("detail",options.detail().id);
        return getWritableDatabase().update("reports",v,"id=? AND state='counting' AND reserved=0",new String[]{""+id})==1;
    }
    void cancelUnsent(long habit){
        ContentValues v=new ContentValues();v.put("state","stopped");v.put("problem","この目標の継続レビューをオフにしたため、送信前に停止しました。");
        getWritableDatabase().update("reports",v,"habit=? AND state IN ('queued','counting') AND reserved=0",new String[]{""+habit});
    }
    void cancelAllUnsent(){
        cancelAllUnsent("継続レビュー全体をオフにしたため、送信前に停止しました。");
    }
    void cancelAllUnsent(String reason){
        ContentValues v=new ContentValues();v.put("state","stopped");v.put("problem",reason);
        getWritableDatabase().update("reports",v,"state IN ('queued','counting') AND reserved=0",null);
    }
    void stopCounting(long id,String problem){
        ContentValues v=new ContentValues();v.put("state","stopped");v.put("problem",problem);
        // A cancelled attempt must not overwrite a newly approved queued retry.
        getWritableDatabase().update("reports",v,"id=? AND state='counting' AND reserved=0",new String[]{""+id});
    }
    boolean sending(long id,double reserve){ContentValues v=new ContentValues();v.put("state","sending");v.put("reserved",reserve);return getWritableDatabase().update("reports",v,"id=? AND state='counting'",new String[]{""+id})==1;}
    void finish(long id,String state,String result,String problem,double estimate){
        ContentValues v=new ContentValues();v.put("state",state);v.put("result",result);v.put("problem",problem);v.put("estimated",estimate);if(state.equals("complete"))v.put("completed_at",System.currentTimeMillis());
        getWritableDatabase().update("reports",v,"id=? AND state<>'complete'",new String[]{""+id});
    }
    Item latestCompletedOn(long habit,LocalDate day){
        long start=day.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
        long end=day.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
        try(Cursor c=getReadableDatabase().rawQuery("SELECT id FROM reports WHERE habit=? AND state='complete' AND completed_at>=? AND completed_at<? ORDER BY completed_at DESC,id DESC LIMIT 1",new String[]{""+habit,""+start,""+end})){
            return c.moveToFirst()?item(c.getLong(0)):null;
        }
    }
    List<Item> pendingNotices(LocalDate day){
        long start=day.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
        long end=day.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();List<Item> result=new ArrayList<>();
        try(Cursor c=getReadableDatabase().rawQuery("SELECT id FROM reports WHERE state='complete' AND requested=0 AND notified=0 AND completed_at>=? AND completed_at<? ORDER BY id",new String[]{""+start,""+end})){
            while(c.moveToNext())result.add(item(c.getLong(0)));
        }return result;
    }
    boolean claimNotice(long id){
        ContentValues v=new ContentValues();v.put("notified",1);
        return getWritableDatabase().update("reports",v,"id=? AND state='complete' AND requested=0 AND notified=0 AND completed_at>0",new String[]{""+id})==1;
    }
    void releaseNotice(long id){ContentValues v=new ContentValues();v.put("notified",0);getWritableDatabase().update("reports",v,"id=?",new String[]{""+id});}
    void recoverInterrupted(){
        getWritableDatabase().execSQL("UPDATE reports SET state='stopped',problem='通信結果を確認できなかったため停止しました。自動で再送しません。' WHERE state IN ('sending','counting')");
    }
    void dropHabit(long habit){getWritableDatabase().delete("reports","habit=?",new String[]{""+habit});}
    boolean retryUnsent(long id){
        ContentValues v=new ContentValues();v.put("state","queued");v.put("problem","");
        return getWritableDatabase().update("reports",v,"id=? AND state='stopped' AND reserved=0",new String[]{""+id})==1;
    }
    boolean hasWeek(long habit,String week){
        try(Cursor c=getReadableDatabase().rawQuery("SELECT 1 FROM reports WHERE habit=? AND week=?",new String[]{""+habit,week})){return c.moveToFirst();}
    }
}
