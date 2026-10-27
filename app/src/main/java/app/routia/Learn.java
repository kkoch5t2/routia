package app.routia;
import android.app.*;
import android.content.*;
import android.net.Uri;
import android.widget.*;
public final class Learn {
    static final String[][] TIPS={
        {"🌱 まずは66日、続けてみよう","毎日の一歩を、66日へ。","まずは66日継続を目指そう。ミニ目標も1日として数えます。\n\nある研究では、習慣が自動的になるまでの推定中央値は66日でした。18〜254日と個人差があり、66日は目安です。休んだ日があっても、また今日から積み重ねよう。","Lally ほか, 2010","https://doi.org/10.1002/ejsp.674"},
        {"🔗 いつもの行動につなげよう","続けるための、小さな工夫。","余裕があるときは「朝ごはんのあとに英語」のように、いつ・どこでやるかを決めてみよう。この工夫は目標達成を助ける研究があります。きっかけの設定は任意です。","Gollwitzer & Sheeran, 2006","https://doi.org/10.1016/S0065-2601(06)38002-1"},
        {"💪 休んだら、今日から再開","戻ってきたことも、成長。","1回休んでも、これまでの努力は消えません。Lallyらの研究でも、1回の実行機会を逃すことは習慣形成を大きく損ないませんでした。忙しい日はミニ目標から再開しよう。","Lally ほか, 2010","https://doi.org/10.1002/ejsp.674"},
        {"📈 記録で、小さな成長を見つけよう","積み重ねを、見える形に。","取り組んだ時間や回数を残してみよう。進み具合を記録する工夫は、目標達成を助ける傾向が報告されています。草やグラフで、できたことを振り返ろう。","Harkin ほか, 2016","https://doi.org/10.1037/bul0000025"}
    };
    static void show(Activity a,int index){String[] t=TIPS[Math.floorMod(index,TIPS.length)];Dialog d=Ui.sheet(a,"ROUTIA LAB  /  "+t[3],t[1],t[0]+"\n\n"+t[2],"なるほど",null);
        Ui.show(d,a);
    }
    private Learn(){}
}
