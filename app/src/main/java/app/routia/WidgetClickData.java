package app.routia;
import java.net.URI;
import java.time.LocalDate;
import java.util.Optional;
import java.util.function.IntPredicate;
/** Validates a collection click; widget/date are fixed in the template URI, never extras. */
public final class WidgetClickData {
    public static final String ACTION="app.routia.WIDGET_COLLECTION_RECORD";
    public record Click(int widgetId,LocalDate day,long habit,int level){}
    public static Optional<Click> parse(String action,String raw,long habit,int level,String shownDay,LocalDate today,IntPredicate owned){
        if(!ACTION.equals(action)||raw==null||habit<=0||(level<1||level>3))return Optional.empty();
        try{URI uri=URI.create(raw);if(!"routia".equals(uri.getScheme())||!"widget-record".equals(uri.getHost())||uri.getPort()!=-1||uri.getUserInfo()!=null||uri.getQuery()!=null||uri.getFragment()!=null)return Optional.empty();
            String[] path=uri.getRawPath().split("/",-1);if(path.length!=3||!path[1].matches("[1-9][0-9]*"))return Optional.empty();
            int widget=Integer.parseInt(path[1]);LocalDate day=LocalDate.parse(path[2]);if(!day.equals(today)||!day.toString().equals(shownDay)||!owned.test(widget))return Optional.empty();return Optional.of(new Click(widget,day,habit,level));
        }catch(java.time.DateTimeException|IllegalArgumentException e){return Optional.empty();}
    }
    private WidgetClickData(){}
}
