import app.routia.ReportCatalog;
import java.time.*;
import java.util.*;

public final class ReportCatalogTest{
    static int n;static void check(boolean ok,String message){n++;if(!ok)throw new AssertionError(message);}
    public static void main(String[] args){
        ZoneId tokyo=ZoneId.of("Asia/Tokyo"),utc=ZoneOffset.UTC;
        var a=new ReportCatalog.Entry(1,10,"英語",Instant.parse("2026-10-25T14:59:00Z").toEpochMilli(),"week","standard",2);
        var b=new ReportCatalog.Entry(2,20,"走る",Instant.parse("2026-10-25T15:01:00Z").toEpochMilli(),"week","detailed",3);
        var legacy=new ReportCatalog.Entry(3,10,"英語（旧）",0,"2026-10-26","",1);
        var later=new ReportCatalog.Entry(4,10,"英語",b.completedAt(),"week","brief",2);
        List<ReportCatalog.Entry> entries=List.of(a,legacy,b,later);
        check(ReportCatalog.day(a,tokyo).equals("2026-10-25")&&ReportCatalog.day(b,tokyo).equals("2026-10-26"),"completion filters follow local midnight");
        check(ReportCatalog.day(b,utc).equals("2026-10-25"),"timezone is explicit");
        check(ReportCatalog.day(legacy,tokyo).equals(ReportCatalog.UNKNOWN),"legacy date not inferred from target week");
        check(ReportCatalog.dates(entries,0,tokyo).equals(List.of("2026-10-26","2026-10-25","unknown")),"dates unique, newest first, unknown last");
        check(ReportCatalog.dates(entries,20,tokyo).equals(List.of("2026-10-26")),"task determines available dates");
        var all=ReportCatalog.filter(entries,0,"",tokyo);
        check(all.get(0).id()==4&&all.get(1).id()==2&&all.get(3).id()==3,"stable newest order and known dates before unknown");
        check(ReportCatalog.filter(entries,10,"2026-10-26",tokyo).size()==1,"task and exact generation date combine");
        check(ReportCatalog.filter(entries,20,"2026-10-25",tokyo).isEmpty(),"no matching report is an empty result");
        check(ReportCatalog.filter(entries,0,"unknown",tokyo).get(0).id()==3,"legacy reports stay selectable");
        check(entries.get(0).id()==1,"filter never mutates input");
        check(ReportCatalog.generated(b.completedAt(),tokyo).equals("2026/10/26 00:01"),"reader and list use full actual generation timestamp");
        check(ReportCatalog.generated(0,tokyo).equals("生成日不明（旧レポート）"),"unknown timestamp explicitly labelled");
        check(ReportCatalog.filter(List.of(),0,"",tokyo).isEmpty()&&ReportCatalog.dates(List.of(),0,tokyo).isEmpty(),"empty collection supported");
        check(ReportCatalog.pageCount(0)==1&&ReportCatalog.pageCount(1)==1&&ReportCatalog.pageCount(10)==1,"empty and first page");
        check(ReportCatalog.pageCount(11)==2&&ReportCatalog.pageCount(1000)==100&&ReportCatalog.pageCount(1001)==101,"page boundaries through 100 pages");
        check(ReportCatalog.pageIndex(100,1005)==100&&ReportCatalog.pageIndex(999,1005)==100,"last page and oversized saved index clamp");
        check(ReportCatalog.pageIndex(-5,1005)==0&&ReportCatalog.pageIndex(100,0)==0,"negative and empty clamp");
        check(ReportCatalog.pageCount(Long.MAX_VALUE)==922337203685477581L,"long count does not overflow");
        check(ReportCatalog.pageIndex(Long.MAX_VALUE,Long.MAX_VALUE)*ReportCatalog.PAGE_SIZE==9223372036854775800L,"long offset does not overflow");
        check(ReportCatalog.requestedPage("100",101)==99&&ReportCatalog.requestedPage(" 1 ",101)==0&&ReportCatalog.requestedPage("101",101)==100,"one-based direct page selection");
        for(String invalidPage:List.of("","0","-1","102","1.5","abc","9223372036854775808"))check(ReportCatalog.requestedPage(invalidPage,101)==-1,"invalid page rejected");
        ReportCatalog.Range year=ReportCatalog.range(2026,0,tokyo);
        check(year.start()==Instant.parse("2025-12-31T15:00:00Z").toEpochMilli()&&year.end()==Instant.parse("2026-12-31T15:00:00Z").toEpochMilli(),"local year endpoints");
        ReportCatalog.Range january=ReportCatalog.range(2026,1,tokyo);
        check(january.end()==Instant.parse("2026-01-31T15:00:00Z").toEpochMilli(),"month end is exclusive local midnight");
        check(ReportCatalog.range(2024,2,utc).end()-ReportCatalog.range(2024,2,utc).start()==29L*86400000,"leap February");
        ReportCatalog.Range dst=ReportCatalog.range(2026,3,ZoneId.of("America/New_York"));
        check(dst.end()-dst.start()==31L*86400000-3600000,"month range respects daylight saving");
        for(int month:new int[]{-1,13})try{ReportCatalog.range(2026,month,tokyo);throw new AssertionError("invalid month accepted");}catch(IllegalArgumentException expected){n++;}
        var partial=new ReportCatalog.Page(1005,100,List.of(a,b,legacy,later,a));
        check(partial.pages()==101&&partial.first()==1001&&partial.last()==1005,"last-page visible range");
        System.out.println("PASS "+n+" report catalog assertions");
    }
}
