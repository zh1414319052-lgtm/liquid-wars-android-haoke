// SPDX-License-Identifier: GPL-3.0-or-later
package com.xenris.liquidwarsos;

import org.godotengine.godot.Godot;
import org.godotengine.godot.plugin.GodotPlugin;
import org.godotengine.godot.plugin.UsedByGodot;

/** Read-only presentation bridge. Original C++ remains the sole simulation. */
public final class GodotBattlePlugin extends GodotPlugin {
    private final MyGLSurfaceView surface;
    private int diagnosticFrames;
    private long referenceCheckedAt;
    private boolean reference190;
    private int preparedFrames;
    public GodotBattlePlugin(Godot engine,MyGLSurfaceView surface){super(engine);this.surface=surface;}
    @Override public String getPluginName(){return "LiquidBattle";}
    /** Actual display pixels; Godot's render target can be supersampled. */
    @UsedByGodot public int[] displaySize(){return new int[]{surface.getWidth(),surface.getHeight()};}
    /** Immutable native birth, not a cursor; bounded polling also covers replay. */
    @UsedByGodot public int[] sampleOrigin(){
        int[] info=NativeInterface.sceneInfo();
        if(info==null||info.length<6||info[2]!=1001||info[4]<0||info[5]<0)
            return new int[0];
        return new int[]{info[4],info[5]};
    }
    @UsedByGodot public float[] frame(){
        float[] c=surface.godotCamera();
        surface.godotFrame();
        // The existing opaque match-intro covers a bounded full-scene warmup.
        // First-use mesh/shadow uploads therefore finish before camera controls
        // are exposed. No extra wait, geometry reduction or camera-state change.
        if(preparedFrames<3&&surface.camera.width>=3264){
            android.app.Activity activity=DisplayFrameRate.activity(surface.getContext());
            android.view.View intro=activity==null?null:activity.findViewById(R.id.match_intro);
            if(intro!=null&&intro.getVisibility()==android.view.View.VISIBLE){
                c=c.clone();
                float ratio=surface.getWidth()/(float)Math.max(1,surface.getHeight());
                c[3]=Math.max(surface.camera.height,surface.camera.width/ratio);
                c[2]=c[3]*ratio;c[0]=(surface.camera.width-c[2])/2;c[1]=(surface.camera.height-c[3])/2;
                preparedFrames++;
            }else preparedFrames=3;
        }
        float[] data=NativeInterface.godotFrame(c[0],c[1],c[2],c[3],c[4]!=0);
        if(data.length>=16){
            data[9]=surface.targetHz();
            // QA-only, private cache marker. No Intent/network endpoint, no
            // simulation writes. Check at most once/second on the render thread.
            if(BuildConfig.DEBUG){
                long now=android.os.SystemClock.uptimeMillis();
                if(now-referenceCheckedAt>=1000){
                    java.io.File marker=new java.io.File(surface.getContext().getCacheDir(),"color-reference-190");
                    reference190=false;
                    if(marker.isFile())try(java.io.FileInputStream input=new java.io.FileInputStream(marker)){
                        reference190=input.read()=='1';
                    }catch(java.io.IOException ignored){ /* Missing/unreadable = new palette. */ }
                    referenceCheckedAt=now;
                }
                data[10]=reference190?1f:0f;
            }
        }
        if(BuildConfig.DEBUG&&++diagnosticFrames%300==0&&data.length>=16){
            android.util.Log.i("LWGodot","STATE time="+data[1]+" own="+data[13]+" camera="+java.util.Arrays.toString(c));
            if(new java.io.File(surface.getContext().getCacheDir(),"expedition192-observe").isFile())try{
                org.json.JSONObject observed=new org.json.JSONObject().put("ops",new org.json.JSONArray(NativeInterface.operationsSnapshot(StaticBits.team,false))).put("exp",new org.json.JSONArray(NativeInterface.expeditionSnapshot())).put("camera",new org.json.JSONArray(c)).put("viewport",new org.json.JSONArray(new int[]{surface.getLeft(),surface.getTop(),surface.getWidth(),surface.getHeight()}));
                try(java.io.FileWriter out=new java.io.FileWriter(new java.io.File(surface.getContext().getCacheDir(),"expedition192-observation.json"))){out.write(observed.toString());}
            }catch(Exception ignored){}
        }
        return data;
    }
    @UsedByGodot public void ready(){
        surface.post(()->{
            surface.godotReady=true;
            surface.syncWorld();
            android.app.Activity a=DisplayFrameRate.activity(surface.getContext());
            if(a instanceof com.xenris.liquidwarsos.activities.GameActivity)
                ((com.xenris.liquidwarsos.activities.GameActivity)a).updatePauseState();
            if(BuildConfig.DEBUG)surface.postDelayed(()->{
                ItemOverlay hud=a.findViewById(R.id.item_overlay);
                android.view.View intro=a.findViewById(R.id.match_intro);
                int[] d=hud.facts();
                android.util.Log.i("LWGodot","HUD shown="+hud.isShown()+" size="+hud.getWidth()+"x"+hud.getHeight()+" facts="+(d==null?-1:d.length)+" intro="+intro.getVisibility()+" time="+NativeInterface.playTimeMillis()+" camera="+java.util.Arrays.toString(surface.godotCamera()));
            },6000);
        });
    }
}
