// SPDX-License-Identifier: GPL-3.0-or-later
package com.xenris.liquidwarsos.activities;

import android.os.Bundle;
import com.xenris.liquidwarsos.*;
import com.xenris.liquidwarsos.R;
import org.godotengine.godot.*;
import org.godotengine.godot.plugin.GodotPlugin;
import java.util.*;

/**
 * A dedicated :garden3d process isolates Godot's process-wide engine lifetime.
 * Classic menus/network/2D remain in the normal process. Replay resets the
 * original simulation in place; exiting destroys only this owned 3D process.
 */
public final class GodotGardenActivity extends GameActivity implements GodotHost {
    private GodotFragment fragment;
    @Override public void onCreate(Bundle state){
        getIntent().putExtra(GardenSession.EXTRA,true);
        super.onCreate(state);
        if(isFinishing())return;
        fragment=new GodotFragment();
        getSupportFragmentManager().beginTransaction().replace(R.id.mySurfaceView,fragment,"godot-battle").commitNow();
        getOnBackPressedDispatcher().addCallback(this,new androidx.activity.OnBackPressedCallback(true){
            @Override public void handleOnBackPressed(){requestLeaveMatch();}
        });
    }
    @Override public boolean dispatchKeyEvent(android.view.KeyEvent event){
        // Godot's focused surface must not turn Android Back into engine quit.
        if(event.getKeyCode()==android.view.KeyEvent.KEYCODE_BACK){
            if(event.getAction()==android.view.KeyEvent.ACTION_DOWN&&event.getRepeatCount()==0)requestLeaveMatch();
            return true;
        }
        return super.dispatchKeyEvent(event);
    }
    @Override public android.app.Activity getActivity(){return this;}
    @Override public Godot getGodot(){return fragment==null?null:fragment.getGodot();}
    @Override public List<String> getCommandLine(){
        return Arrays.asList("--main-pack","res://battle.pck","--rendering-method","gl_compatibility","--rendering-driver","opengl3");
    }
    @Override public Set<GodotPlugin> getHostPlugins(Godot engine){
        return Collections.singleton(new GodotBattlePlugin(engine,findViewById(R.id.mySurfaceView)));
    }
    @Override public void onGodotForceQuit(Godot engine){runOnUiThread(this::finish);}
    @Override public void onDestroy(){
        super.onDestroy();
        // Godot cannot be started twice in a process after native termination.
        // This PID belongs only to :garden3d, never the emulator or the menu.
        android.os.Process.killProcess(android.os.Process.myPid());
    }
}
