package app.routia;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

/** Saved-review metadata; no generated content or approximate dates in the catalog. */
public final class ReportCatalog {
    public static final String UNKNOWN="unknown";
    public record Entry(long id,long habit,String goal,long completedAt,String week,String detail,double estimated) {}
    public static String day(Entry entry,ZoneId zone){
        return entry.completedAt()>0?Instant.ofEpochMilli(entry.completedAt()).atZone(zone).toLocalDate().toString():UNKNOWN;
    }
    public static String generated(long time,ZoneId zone){
        return time>0?Instant.ofEpochMilli(time).atZone(zone).format(DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm",Locale.JAPAN)):"生成日不明（旧レポート）";
    }
    public static List<String> dates(List<Entry> entries,long habit,ZoneId zone){
        Set<String> dates=new TreeSet<>(Comparator.reverseOrder());boolean unknown=false;
        for(Entry entry:entries)if(habit==0||entry.habit()==habit){
            String key=day(entry,zone);if(key.equals(UNKNOWN))unknown=true;else dates.add(key);
        }
        List<String> result=new ArrayList<>(dates);if(unknown)result.add(UNKNOWN);return result;
    }
    public static List<Entry> filter(List<Entry> entries,long habit,String date,ZoneId zone){
        List<Entry> result=new ArrayList<>();
        for(Entry entry:entries)if((habit==0||entry.habit()==habit)&&(date.isEmpty()||day(entry,zone).equals(date)))result.add(entry);
        result.sort(Comparator.comparingLong(Entry::completedAt).reversed().thenComparing(Comparator.comparingLong(Entry::id).reversed()));
        return result;
    }
    public static final int PAGE_SIZE=10;
    public record Range(long start,long end) {}
    public record Page(long total,long index,List<Entry> entries) {
        public Page { entries=List.copyOf(entries); }
        public long pages(){return pageCount(total);}
        public long first(){return total==0?0:index*PAGE_SIZE+1;}
        public long last(){return index*PAGE_SIZE+entries.size();}
    }
    /** Calendar boundaries in the phone's zone, including DST and local New Year. */
    public static Range range(int year,int month,ZoneId zone){
        if(year<1||year>9999||month<0||month>12)throw new IllegalArgumentException("Invalid report period");
        LocalDate start=LocalDate.of(year,month==0?1:month,1);
        LocalDate end=month==0?start.plusYears(1):start.plusMonths(1);
        return new Range(start.atStartOfDay(zone).toInstant().toEpochMilli(),end.atStartOfDay(zone).toInstant().toEpochMilli());
    }
    public static long pageCount(long total){
        if(total<0)throw new IllegalArgumentException("Negative total");
        return total==0?1:(total-1)/PAGE_SIZE+1;
    }
    public static long pageIndex(long requested,long total){return Math.max(0,Math.min(requested,pageCount(total)-1));}
    public static long requestedPage(String text,long pages){
        try{long number=Long.parseLong(text.trim());return number>=1&&number<=pages?number-1:-1;}
        catch(NumberFormatException invalid){return -1;}
    }
    private ReportCatalog(){}
}
