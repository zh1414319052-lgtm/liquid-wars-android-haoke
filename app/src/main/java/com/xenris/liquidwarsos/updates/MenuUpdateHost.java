// SPDX-License-Identifier: GPL-3.0-or-later
package com.xenris.liquidwarsos.updates;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AlertDialog;
import com.xenris.liquidwarsos.R;
import com.xenris.liquidwarsos.online.LobbyUi;

/** Explicitly owned only by main menu / instructions; no battle or lobby inheritance. */
public final class MenuUpdateHost implements HaokeUpdateUi.Host {
    private final Activity activity;
    private final boolean automatic;
    private final HaokeUpdateUi updates;
    private final LobbyUi skin;
    private boolean resumed;
    public MenuUpdateHost(Activity activity,boolean automatic){this.activity=activity;this.automatic=automatic;skin=new LobbyUi(activity);updates=new HaokeUpdateUi(activity,this);}
    public void resume(){resumed=true;updates.resume();}
    public void pause(){resumed=false;updates.pause();}
    public void close(){updates.close();}
    public void focus(boolean focused){if(focused&&resumed)updates.refresh();}
    public boolean isSafeForUpdates(boolean manual){return resumed&&activity.hasWindowFocus()&&(automatic||manual);}
    public void onDialogShown(AlertDialog dialog){
        TextView message=dialog.findViewById(android.R.id.message);
        if(message!=null)message.setTypeface(android.graphics.Typeface.createFromAsset(activity.getAssets(),"fonts/NotoSansSC-Lobby400.ttf"));
        for(int id:new int[]{-1,-2,-3}){Button b=dialog.getButton(id);if(b!=null){b.setMinHeight(skin.dp(48));b.setTypeface(android.graphics.Typeface.createFromAsset(activity.getAssets(),"fonts/NotoSansSC-Lobby600.ttf"));}}
    }
    public void addAboutControls(LinearLayout parent){
        skin.add(parent,skin.label(activity.getString(R.string.updates_title),20,LobbyUi.INK,true),20);
        skin.add(parent,skin.label(activity.getString(R.string.updates_privacy),13,LobbyUi.MUTED,false),8);
        skin.add(parent,skin.button(activity.getString(R.string.updates_check),true,updates::manual),12);
        skin.add(parent,skin.button(activity.getString(R.string.haoke_tools),false,()->{
            try{activity.startActivity(new Intent(Intent.ACTION_VIEW,Uri.parse("https://www.haoke.org/")));}
            catch(android.content.ActivityNotFoundException unavailable){Toast.makeText(activity,R.string.updates_browser_unavailable,Toast.LENGTH_SHORT).show();}
        }),8);
    }
}
