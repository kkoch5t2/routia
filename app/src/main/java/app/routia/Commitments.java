package app.routia;
import android.content.*;
import java.time.*;
public final class Commitments {
    static void evaluate(Context c){
        // Donation integration was cancelled. Preserve its old ledger without creating new pledges.
        if(Feedback.prefs(c).getBoolean("pledgeEnabled",false))Feedback.prefs(c).edit().putBoolean("pledgeEnabled",false).apply();
    }
    private Commitments(){}
}
