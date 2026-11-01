package app.routia;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.os.Build;
import android.view.*;

/** Protect credentials and fee consent without replacing normal click/accessibility handling. */
final class SensitiveActions {
    static void protectWindow(Window window){
        if(Build.VERSION.SDK_INT>=31)window.setHideOverlayWindows(true);
    }
    @SuppressLint("ClickableViewAccessibility") // Only rejects obscured input; normal clicks use View's handling.
    static void protect(View view){
        view.setFilterTouchesWhenObscured(true);
        view.setOnTouchListener((target,event)->obscured(event));
        if(Build.VERSION.SDK_INT>=34)view.setAccessibilityDataSensitive(View.ACCESSIBILITY_DATA_SENSITIVE_YES);
    }
    private static boolean obscured(MotionEvent event){
        if((event.getFlags()&MotionEvent.FLAG_WINDOW_IS_OBSCURED)!=0)return true;
        return Build.VERSION.SDK_INT>=29&&(event.getFlags()&MotionEvent.FLAG_WINDOW_IS_PARTIALLY_OBSCURED)!=0;
    }
    static AlertDialog protect(AlertDialog shown){
        protectWindow(shown.getWindow());protect(shown.getButton(AlertDialog.BUTTON_POSITIVE));return shown;
    }
    private SensitiveActions(){}
}
