package app.routia;

import android.content.Context;
import android.content.Intent;

/** A separate chart screen reuses the existing local history rendering and per-task periods. */
public final class AchievementChartsActivity extends MainActivity {
    static Intent intent(Context c,long habit){return new Intent(c,AchievementChartsActivity.class).putExtra("chart_habit",habit);}
}
