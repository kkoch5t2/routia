package app.routia;
import android.content.*;
import android.widget.*;
import java.util.*;
/** Android 8–11 fallback. Access is restricted by BIND_REMOTEVIEWS. */
public class WidgetRowsService extends RemoteViewsService {
    @Override public RemoteViewsFactory onGetViewFactory(Intent intent){return new Factory(getApplicationContext(),WidgetCollections.PROGRESS.equals(intent.getStringExtra("kind"))?WidgetCollections.PROGRESS:WidgetCollections.RECORD_COMPACT.equals(intent.getStringExtra("kind"))?WidgetCollections.RECORD_COMPACT:WidgetCollections.RECORD,intent.getIntExtra("widgetId",-1));}
    static final class Factory implements RemoteViewsFactory {
        final Context context;final String kind;final int widgetId;List<WidgetCollections.Row> rows=List.of();
        Factory(Context context,String kind,int widgetId){this.context=context;this.kind=kind;this.widgetId=widgetId;}
        public void onCreate(){load();}public void onDataSetChanged(){load();}void load(){rows=WidgetCollections.rows(context,kind,true,WidgetCollections.chartWidth(context,widgetId));}
        public void onDestroy(){rows=List.of();}public int getCount(){return rows.size();}
        public RemoteViews getViewAt(int position){return position>=0&&position<rows.size()?rows.get(position).view():null;}
        public RemoteViews getLoadingView(){return null;}public int getViewTypeCount(){return WidgetCollections.PROGRESS.equals(kind)?1:4;}
        public long getItemId(int position){return position>=0&&position<rows.size()?rows.get(position).id():-1;}public boolean hasStableIds(){return true;}
    }
}
