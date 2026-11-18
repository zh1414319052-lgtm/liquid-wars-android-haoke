// SPDX-License-Identifier: GPL-3.0-or-later
package com.xenris.liquidwarsos.updates;

import android.app.Activity;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.*;
import android.widget.*;
import androidx.appcompat.app.AlertDialog;
import com.xenris.liquidwarsos.BuildConfig;
import com.xenris.liquidwarsos.R;
import com.xenris.liquidwarsos.ui.MenuSkin;
import java.util.function.Consumer;

/** Bundled local notes, independent of server manifests and network availability. */
public final class ReleaseNotesHost {
    static final String PREFERENCES="local-release-notes";
    private final Activity activity;
    private final SharedPreferences prefs;
    private final String seenKey;
    private final Consumer<Boolean> blockUpdates;
    private boolean resumed, closed;
    private AlertDialog dialog;
    public ReleaseNotesHost(Activity a,Consumer<Boolean> block){this(a,block,a.getSharedPreferences(PREFERENCES,0),BuildConfig.VERSION_CODE);}
    ReleaseNotesHost(Activity a,Consumer<Boolean> block,SharedPreferences p,int version){activity=a;blockUpdates=block;prefs=p;seenKey="shown_"+version;publishBlock();}
    boolean pending(){return !prefs.getBoolean(seenKey,false);}
    public void resume(){resumed=true;refresh();}
    public void focus(boolean focused){if(focused)refresh();}
    public void pause(){resumed=false;if(dialog!=null)dialog.dismiss();}
    public void close(){closed=true;pause();}
    public void manual(){if(ready()&&dialog==null)show();}
    private boolean ready(){return !closed&&resumed&&!activity.isFinishing()&&!activity.isDestroyed()&&activity.hasWindowFocus();}
    private void publishBlock(){blockUpdates.accept(!closed&&(pending()||dialog!=null));}
    private void refresh(){if(ready()&&pending()&&dialog==null)show();else publishBlock();}
    private void show(){
        MenuSkin s=new MenuSkin(activity);LinearLayout panel=s.column();panel.setPadding(s.u(24),s.u(18),s.u(24),s.u(16));panel.setBackground(s.surface(MenuSkin.PANEL,16,MenuSkin.LINE));
        LinearLayout head=s.row();TextView title=s.text(activity.getString(R.string.notes_title,BuildConfig.VERSION_NAME),24,MenuSkin.INK,true);title.setAccessibilityHeading(true);
        head.addView(title,new LinearLayout.LayoutParams(0,-2,1));
        Button close=s.button(activity.getString(R.string.notes_close),()->{if(dialog!=null)dialog.dismiss();});close.setId(R.id.notes_close);head.addView(close);panel.addView(head);
        s.add(panel,s.text(activity.getString(R.string.notes_first_only),13,MenuSkin.MUTED,false),8);
        ScrollView scroll=new ScrollView(activity);LinearLayout body=s.column();scroll.addView(body);
        s.add(body,s.text(activity.getString(R.string.notes_changes),19,MenuSkin.ACCENT,true),18);
        s.add(body,s.text(activity.getString(R.string.notes_changes_body),16,MenuSkin.INK,false),8);
        s.add(body,s.text(activity.getString(R.string.notes_tips),19,MenuSkin.ACCENT,true),18);
        for(int text:new int[]{R.string.notes_freeze,R.string.notes_cleanse,R.string.notes_blackhole})s.add(body,s.text(activity.getString(text),16,MenuSkin.INK,false),10);
        s.add(body,s.text(activity.getString(R.string.notes_stock),14,MenuSkin.MUTED,false),14);
        panel.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        AlertDialog candidate=new AlertDialog.Builder(activity).setView(panel).create();
        candidate.setCanceledOnTouchOutside(true);
        candidate.setOnDismissListener(d->{if(dialog==candidate){dialog=null;publishBlock();}});
        dialog=candidate;publishBlock();
        try{
            candidate.show();
            Window window=candidate.getWindow();
            if(window!=null){
                window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
                android.util.DisplayMetrics m=activity.getResources().getDisplayMetrics();
                window.setLayout(Math.min(m.widthPixels-s.dp(48),s.u(760)),Math.min(m.heightPixels-s.dp(48),s.u(460)));
                window.setDimAmount(.65f);
            }
            // apply updates memory immediately and persists asynchronously. Android drains it on stop.
            // Mark actual display, not creation/close/animation, so a restart never nags again.
            prefs.edit().putBoolean(seenKey,true).apply();
        }catch(WindowManager.BadTokenException unavailable){dialog=null;candidate.setOnDismissListener(null);publishBlock();}
    }
}
