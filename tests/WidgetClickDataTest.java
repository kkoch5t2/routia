import app.routia.WidgetClickData;
import java.time.LocalDate;
public class WidgetClickDataTest {
    static int checks;static final LocalDate TODAY=LocalDate.of(2026,10,24);static final String BASE="routia://widget-record/7/2026-10-24";
    static void check(boolean ok,String label){checks++;if(!ok)throw new AssertionError(label);}
    static boolean valid(String uri,long habit,int level){return WidgetClickData.parse(WidgetClickData.ACTION,uri,habit,level,TODAY.toString(),TODAY,id->id==7).isPresent();}
    public static void main(String[] args){
        var click=WidgetClickData.parse(WidgetClickData.ACTION,BASE,42,2,TODAY.toString(),TODAY,id->id==7).orElseThrow();
        check(click.widgetId()==7&&click.habit()==42&&click.level()==2&&click.day().equals(TODAY),"Validated identity and payload are retained");
        check(valid(BASE,42,1),"Minimum action is allowed");
        check(!valid("routia://widget-record/7/2026-10-23",42,2),"A stale template cannot record today");
        check(!valid("routia://widget-record/7/2026-10-25",42,2),"A future template cannot record today");
        check(!WidgetClickData.parse("other",BASE,42,2,TODAY.toString(),TODAY,id->true).isPresent(),"Other actions cannot use collection validation");
        check(!valid("https://widget-record/7/2026-10-24",42,2),"Non-app URI scheme rejected");
        check(!valid("routia://other/7/2026-10-24",42,2),"Wrong URI authority rejected");
        check(!valid(BASE+"?day=2026-10-24",42,2),"Query override rejected");
        check(!valid(BASE+"#override",42,2),"Fragment override rejected");
        check(!valid(BASE+"/extra",42,2),"Extra identity segments rejected");
        check(!valid("routia://widget-record/8/2026-10-24",42,2),"Widget not owned by recording provider rejected");
        check(!WidgetClickData.parse(WidgetClickData.ACTION,BASE,42,2,TODAY.toString(),TODAY,id->false).isPresent(),"Deleted widget rejected");
        check(!valid("routia://widget-record/0/2026-10-24",42,2),"Zero widget identifier rejected");
        check(!valid("routia://widget-record/-7/2026-10-24",42,2),"Negative widget identifier rejected");
        check(!valid("routia://widget-record/99999999999999999/2026-10-24",42,2),"Overflow widget identifier rejected");
        check(!valid("routia://widget-record/%37/2026-10-24",42,2),"Encoded identity rejected");
        check(!valid("routia://widget-record/7/not-a-date",42,2),"Malformed date is rejected without throwing");
        check(!valid("routia://widget-record/7/2026-02-29",42,2),"Impossible date is rejected without throwing");
        check(!valid("routia://widget-record/7/2026-10-24T00:00",42,2),"Timestamp is not a daily identity");
        check(!valid(null,42,2),"Missing identity rejected");
        check(!valid(BASE,0,2)&&!valid(BASE,-1,2),"Missing or invalid habit rejected");
        check(valid(BASE,42,3),"Challenge action is allowed");check(!valid(BASE,42,0)&&!valid(BASE,42,4),"Only three supported grades are allowed");
        check(!valid("routia://user@widget-record/7/2026-10-24",42,2),"Unexpected user info rejected");
        check(!valid("routia://widget-record:123/7/2026-10-24",42,2),"Unexpected port rejected");
        check(WidgetClickData.parse(WidgetClickData.ACTION,BASE,42,2,"2026-10-23",TODAY,id->true).isEmpty(),"Old row cannot borrow a current-day template");
        check(WidgetClickData.parse(WidgetClickData.ACTION,BASE,42,2,null,TODAY,id->true).isEmpty(),"Missing displayed day is rejected");
        check(WidgetClickData.parse(WidgetClickData.ACTION,BASE,42,2,"2026-10-25",TODAY,id->true).isEmpty(),"Future row cannot use a current-day template");
        System.out.println("PASS "+checks+" collection-click assertions");
    }
}
