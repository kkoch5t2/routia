package app.routia;
import android.app.*;
import android.appwidget.*;
import android.content.*;
import android.graphics.*;
import android.widget.*;
/** Always opens a useful screen, including after a launcher rejects a request. */
final class WidgetSetup {
    static void show(Activity a){show(a,false);}
    static void showProgress(Activity a){show(a,true);}
    private static void show(Activity a,boolean progress){
        ComponentName provider=new ComponentName(a,progress?ProgressWidget.class:HabitWidget.class);
        AppWidgetManager manager=a.getSystemService(AppWidgetManager.class);
        TextView[] statusRef={null};Dialog dialog=new Dialog(a){@Override public void onWindowFocusChanged(boolean focused){super.onWindowFocusChanged(focused);if(focused&&statusRef[0]!=null){int count=manager.getAppWidgetIds(provider).length;if(count>0)statusRef[0].setText("✅ ホーム画面に設置済みです");}}};LinearLayout box=Ui.col(a);box.setPadding(Ui.dp(a,24),Ui.dp(a,24),Ui.dp(a,24),Ui.dp(a,24));box.setBackground(Ui.bg(a,Color.WHITE,24));
        box.addView(Ui.text(a,progress?"成果をホーム画面に追加":"ホーム画面に追加",24,Ui.INK,true));Ui.gap(box,12);
        TextView status=Ui.text(a,"",14,Ui.GREEN,true);box.addView(status);statusRef[0]=status;Ui.gap(box,16);
        box.addView(progress?ProgressWidget.preview(a):WidgetCollections.preview(a,WidgetCollections.RECORD));Ui.gap(box,18);
        Runnable refresh=()->{int count=manager.getAppWidgetIds(provider).length;status.setText(count>0?"✅ ホーム画面に "+count+"個 設置済み":"まだ設置されていません");};refresh.run();
        box.addView(Ui.button(a,"追加をリクエスト",true,()->{
            if(!manager.isRequestPinAppWidgetSupported()){status.setText("このホーム画面では、下の手順で手動追加してください。");return;}
            try{boolean sent=manager.requestPinAppWidget(provider,null,PendingIntent.getBroadcast(a,progress?92:91,new Intent().setComponent(provider).setAction(progress?ProgressWidget.PINNED:"app.routia.WIDGET_PINNED"),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE));
                status.setText(sent?"確認画面で「追加」を選んでください。表示されない場合は下の手順で追加できます。":"確認画面を開けませんでした。下の手順で追加してください。");
            }catch(IllegalStateException|SecurityException e){status.setText("確認画面を開けませんでした。下の手順で追加してください。");}
        }));Ui.gap(box,20);
        box.addView(Ui.text(a,"表示されない・拒否したとき",16,Ui.INK,true));Ui.gap(box,8);box.addView(Ui.text(a,"1. ホーム画面の空いている場所を長押し\n（反応しない場合は2本指でつまむ）\n2. 「ウィジェット」を開く\n3. 「"+(progress?"Routia · 成果":"Routia · 記録")+"」を選んで配置する",14,Ui.MUTED,false));Ui.gap(box,10);box.addView(Ui.text(a,"一度拒否しても、ここから再リクエストできます。通知の許可とは別の操作です。",13,Ui.MUTED,false));Ui.gap(box,20);box.addView(Ui.outline(a,"閉じる",dialog::dismiss));
        ScrollView scroll=new ScrollView(a);scroll.addView(box);dialog.setContentView(scroll);dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);Ui.show(dialog,a);
    }
    private WidgetSetup(){}
}
