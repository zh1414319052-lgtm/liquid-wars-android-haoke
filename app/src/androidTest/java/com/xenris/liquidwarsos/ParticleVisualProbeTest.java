// SPDX-License-Identifier: GPL-3.0-or-later
package com.xenris.liquidwarsos;

import android.app.AlertDialog;
import android.content.*;
import android.graphics.Bitmap;
import android.os.*;
import android.view.View;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import com.xenris.liquidwarsos.activities.GameActivity;
import com.xenris.liquidwarsos.cloud.ModelMatch;
import java.io.*;
import java.lang.reflect.Field;
import java.security.MessageDigest;
import java.util.Arrays;
import org.json.*;
import org.junit.*;
import org.junit.runner.RunWith;

/** Real GLES fixed-state captures. No fabricated renderer, credential access or API lanes.
 * Keeps lifecycle pause asserted for a controlled comparison; not a live-play screenshot. */
@RunWith(AndroidJUnit4.class) public class ParticleVisualProbeTest {
    static Object get(Object o,String n)throws Exception {Field f=o.getClass().getDeclaredField(n);f.setAccessible(true);return f.get(o);}
    static void set(Object o,String n,Object v)throws Exception {Field f=o.getClass().getDeclaredField(n);f.setAccessible(true);f.set(o,v);}
    static String hash(int[] values)throws Exception {
        MessageDigest d=MessageDigest.getInstance("SHA-256");
        for(int v:values)for(int s=0;s<32;s+=8)d.update((byte)(v>>>s));
        StringBuilder b=new StringBuilder();for(byte v:d.digest())b.append(String.format(java.util.Locale.ROOT,"%02x",v&255));return b.toString();
    }
    static Bitmap picture(Context c,String n)throws Exception {
        Bitmap b=InstrumentationRegistry.getInstrumentation().getUiAutomation().takeScreenshot();Assert.assertNotNull(b);
        try(OutputStream out=c.openFileOutput(n,0)){b.compress(Bitmap.CompressFormat.PNG,100,out);}return b;
    }
    @Test public void fixedStateFramesAndContextRecovery()throws Exception {
        Bundle args=InstrumentationRegistry.getArguments();
        int map=Integer.parseInt(args.getString("map","28")),seed=Integer.parseInt(args.getString("seed","1628")),dots=Integer.parseInt(args.getString("dots","1000"));
        String phase=args.getString("phase","after");Assert.assertTrue(phase.matches("[a-z0-9-]+"));
        Context c=InstrumentationRegistry.getInstrumentation().getTargetContext();
        StaticBits.init();StaticBits.map=map;StaticBits.seed=seed;StaticBits.team=2;StaticBits.dotsPerTeam=dots;
        StaticBits.aiDifficulty=AiDifficulty.CLASSIC;StaticBits.timeLimit=240;StaticBits.alliances=AllianceRules.solo();
        Intent intent=new Intent(c,GameActivity.class).putExtra("qa_model_teams",0).putExtra("qa_ruins_ready",true).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        String name="particles-"+phase+"-"+map+"-"+dots;
        try(ActivityScenario<GameActivity> scenario=ActivityScenario.launch(intent)) {
            GameActivity[] a={null};scenario.onActivity(v->a[0]=v);SystemClock.sleep(900);
            Assert.assertTrue((Boolean)get(a[0],"paused"));
            ModelMatch match=(ModelMatch)get(get(a[0],"models"),"match");Assert.assertEquals(0,match.configuredMask());
            int[] initial=NativeInterface.modelSnapshot();Assert.assertNotNull(initial);
            // Do not stop the worker: its normal shutdown destroys native State.
            // Keep the ordinary pause predicate true when dismissing the ready overlay.
            scenario.onActivity(v->{try {set(v,"resumed",false);((AlertDialog)get(v,"dialog")).dismiss();v.findViewById(R.id.match_intro).setVisibility(View.GONE);}catch(Exception e){throw new RuntimeException(e);}});
            SystemClock.sleep(550);Assert.assertArrayEquals(initial,NativeInterface.modelSnapshot());
            Bitmap first=picture(c,name+".png");SystemClock.sleep(450);Bitmap second=picture(c,name+"-repeat.png");
            Assert.assertTrue("Paused pixels do not shimmer",first.sameAs(second));
            scenario.onActivity(v->{MyGLSurfaceView surface=v.findViewById(R.id.mySurfaceView);surface.onPause();surface.onResume();});
            SystemClock.sleep(650);Bitmap recovered=picture(c,name+"-recovered.png");
            Assert.assertTrue("Fresh EGL context restores the same particle appearance",first.sameAs(recovered));
            first.recycle();second.recycle();recovered.recycle();Assert.assertArrayEquals(initial,NativeInterface.modelSnapshot());
            // Render thread is paused before deterministic native stepping. No local AI or provider runs.
            scenario.onActivity(v->((MyGLSurfaceView)v.findViewById(R.id.mySurfaceView)).onPause());
            JSONArray trajectory=new JSONArray();trajectory.put(hash(initial));
            for(int step=0;step<120;step++) {
                for(int team=0;team<6;team++) {
                    short[] xs={(short)((step*7+team*91)%780+10),-1,-1,-1,-1};
                    short[] ys={(short)((step*3+team*63)%460+10),-1,-1,-1,-1};
                    NativeInterface.setPlayerPosition(team,xs,ys);
                }
                NativeInterface.stepDots();trajectory.put(hash(NativeInterface.modelSnapshot()));
            }
            JSONObject out=new JSONObject().put("phase",phase).put("map",map).put("seed",seed).put("dots_per_team",dots)
                    .put("models",match.configuredMask()).put("frozen_state_sha256",hash(initial)).put("snapshot_trajectory_sha256",trajectory)
                    .put("pause_pixel_stability",true).put("egl_recovery_pixel_identical",true).put("real_gles_capture",true)
                    .put("controlled_lifecycle_pause",true).put("full_rng_hash","Not exposed by Java snapshot; see host core checks and protected source hashes");
            try(Writer w=new OutputStreamWriter(c.openFileOutput(name+".json",0),"UTF-8")){w.write(out.toString(2));}
        }
    }
}
