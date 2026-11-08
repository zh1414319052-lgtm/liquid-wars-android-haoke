// SPDX-License-Identifier: GPL-3.0-or-later
package com.xenris.liquidwarsos;
import android.app.Activity;
import android.view.View;
import android.view.WindowManager;

public final class GameWindow {
    private GameWindow() {}
    public static void apply(Activity activity) {
        activity.getWindow().setBackgroundDrawableResource(R.color.game_background_soft);
        activity.getWindow().getDecorView().setSystemUiVisibility(
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY | View.SYSTEM_UI_FLAG_FULLSCREEN
            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
        if (android.os.Build.VERSION.SDK_INT >= 28) {
            WindowManager.LayoutParams attributes = activity.getWindow().getAttributes();
            attributes.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
            activity.getWindow().setAttributes(attributes);
        }
    }
}
