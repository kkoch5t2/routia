package app.routia;

import android.app.*;
import java.util.*;

final class ReportConsent {
    static String price(ReportBudget.Options o){
        return "AI："+ReportBudget.model(o.model()).label()+"\nレポートの詳しさ："+o.detail().label+"\n1回の上限："+o.limitYen()+"円相当\n金額の目安（仮条件）："+ReportBudget.referenceLabel(o.model(),o.detail())+"\n実測前の試算です。この幅を超える場合があります。";
    }
    static AlertDialog manual(Activity a,String goal,ReportBudget.Options o,Runnable accept){
        return new AlertDialog.Builder(a).setTitle("有料でレビューを生成しますか？")
            .setMessage("⚠️ OpenAIのAPI利用料が発生します。\n\n対象："+goal+"\n"+price(o)+"\n\n今週1回分として生成します。実費のみで、上限額が必ず請求されるわけではありません。通信が切れても費用が発生する場合があります。")
            .setNegativeButton("戻る",null).setPositiveButton("料金を確認して生成",(d,w)->accept.run()).show();
    }
    static AlertDialog enable(Activity a,ReportBudget.Options o,List<String> goals,Runnable accept){
        String total=String.format(Locale.JAPAN,"%,d",goals.size()*o.limitYen());
        String targets=goals.isEmpty()?"未選択（共通設定で対象を選んでください）":String.join("・",goals);
        return new AlertDialog.Builder(a).setTitle("継続レビューをオンにしますか？")
            .setMessage("⚠️ 対象目標の未達の条件を満たすと、自動でOpenAIのAPI利用料が発生します。\n\n対象："+targets+"\n"+price(o)+"\n\n1週間分の予算：最大"+total+"円相当\n"+o.limitYen()+"円 × "+goals.size()+"目標 × 週1回\n\n自動判定は明日から再開します。オフにしていた期間の未達分はまとめて生成しません。APIキー未設定・対象未選択では生成しません。保存済みレビューを読むだけなら追加料金はかかりません。")
            .setNegativeButton("戻る",null).setPositiveButton("料金を確認してオン",(d,w)->accept.run()).show();
    }
    static AlertDialog settings(Activity a,ReportBudget.Options o,List<String> goals,Runnable accept){
        String total=String.format(Locale.JAPAN,"%,d",goals.size()*o.limitYen());
        return new AlertDialog.Builder(a).setTitle("有料の自動生成設定を保存しますか？")
            .setMessage("⚠️ 未達の条件を満たすと、自動でOpenAIのAPI利用料が発生します。\n\n対象："+String.join("・",goals)+"\n"+price(o)+"\n\n1週間分の予算：最大"+total+"円相当\n"+o.limitYen()+"円 × "+goals.size()+"目標 × 週1回\n\n未達の出費を自分への戒めにする設定です。実費のみで、上限額が必ず請求されるわけではありません。すでに条件を満たす目標は、保存後に実行される場合があります。")
            .setNegativeButton("戻る",null).setPositiveButton("料金を確認して保存",(d,w)->accept.run()).show();
    }
    private ReportConsent(){}
}
