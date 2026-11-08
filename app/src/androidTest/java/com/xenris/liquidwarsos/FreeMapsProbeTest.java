// SPDX-License-Identifier: GPL-3.0-or-later
package com.xenris.liquidwarsos;
import android.content.*;import android.os.*;import android.graphics.Bitmap;
import androidx.test.core.app.ActivityScenario;import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;import org.junit.*;import org.junit.runner.RunWith;import org.json.*;
import java.io.*;import java.lang.reflect.*;import java.util.Arrays;
import com.xenris.liquidwarsos.activities.GameActivity;import com.xenris.liquidwarsos.cloud.ModelMatch;

/** Only explicitly authorized MuMu; every model masked BEFORE game startup.
 * Records raw actual App screenshots and verifies warmup freeze, not gameplay
 * quality, human usability, performance or real multiplayer. */
@RunWith(AndroidJUnit4.class) public class FreeMapsProbeTest {
 private static Object field(Object o,String name)throws Exception{Field f=o.getClass().getDeclaredField(name);f.setAccessible(true);return f.get(o);}
 private static void picture(Context c,String name)throws Exception{Bitmap im=InstrumentationRegistry.getInstrumentation().getUiAutomation().takeScreenshot();Assert.assertNotNull(im);try(OutputStream out=c.openFileOutput(name,Context.MODE_PRIVATE)){im.compress(Bitmap.CompressFormat.PNG,100,out);}im.recycle();}
 @Test public void freeStartsAndCountdown()throws Exception{
  Bundle args=InstrumentationRegistry.getArguments();int map=Integer.parseInt(args.getString("map","0")),seed=Integer.parseInt(args.getString("seed","921")),trials=Integer.parseInt(args.getString("trials","1")),seconds=Integer.parseInt(args.getString("seconds","6"));
  Assert.assertTrue(trials>0&&trials<=8);boolean spectator="1".equals(args.getString("spectator","0"));Context c=InstrumentationRegistry.getInstrumentation().getTargetContext();JSONArray report=new JSONArray();
  for(int trial=0;trial<trials;trial++){
   StaticBits.init();StaticBits.team=2;StaticBits.map=map;StaticBits.seed=seed+trial;StaticBits.dotsPerTeam=1000;StaticBits.timeLimit=120;StaticBits.aiDifficulty=AiDifficulty.HARD;StaticBits.alliances=AllianceRules.solo();
   Intent intent=new Intent(c,GameActivity.class).putExtra("qa_model_teams",0).putExtra("spectator",spectator).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
   try(ActivityScenario<GameActivity> scenario=ActivityScenario.launch(intent)){
    final GameActivity[] activity={null};scenario.onActivity(a->activity[0]=a);ModelMatch m=(ModelMatch)field(field(activity[0],"models"),"match");Assert.assertEquals("No paid model lanes during visual QA",0,m.configuredMask());
    long began=SystemClock.elapsedRealtime();int[] initial=NativeInterface.modelSnapshot();short[] starts=NativeInterface.initialPositions();SystemClock.sleep(200);
    if(!spectator){
     final android.graphics.Rect bounds=new android.graphics.Rect();scenario.onActivity(a->a.findViewById(R.id.mySurfaceView).getGlobalVisibleRect(bounds));int px=Math.min(775,starts[4]+40),py=starts[5];float x=bounds.left+(px+.5f)*bounds.width()/800f,y=bounds.top+(479-py+.5f)*bounds.height()/480f;
     long t=SystemClock.uptimeMillis();android.view.MotionEvent down=android.view.MotionEvent.obtain(t,t,android.view.MotionEvent.ACTION_DOWN,x,y,0),up=android.view.MotionEvent.obtain(t,t+30,android.view.MotionEvent.ACTION_UP,x,y,0);down.setSource(android.view.InputDevice.SOURCE_TOUCHSCREEN);up.setSource(android.view.InputDevice.SOURCE_TOUCHSCREEN);
     Assert.assertTrue(InstrumentationRegistry.getInstrumentation().getUiAutomation().injectInputEvent(down,true));Assert.assertTrue(InstrumentationRegistry.getInstrumentation().getUiAutomation().injectInputEvent(up,true));down.recycle();up.recycle();
     short[][] xs=(short[][])field(activity[0],"xs"),ys=(short[][])field(activity[0],"ys");synchronized(xs){Assert.assertEquals("Preaim touch reaches game below intro overlay",px,xs[2][0]);Assert.assertEquals(py,ys[2][0]);}
    }
    SystemClock.sleep(600);Assert.assertArrayEquals("Native state unchanged during countdown",initial,NativeInterface.modelSnapshot());
    long freezeChecked=SystemClock.elapsedRealtime()-began,captureBegan=SystemClock.elapsedRealtime();
    // PNG readback/compression can outlast the remaining countdown on a busy emulator.
    // Keep the strict state assertion before that I/O; record capture time honestly.
    picture(c,"free-start-"+map+"-"+(seed+trial)+".png");
    JSONObject row=new JSONObject().put("map",map).put("seed",seed+trial).put("starts",new JSONArray(starts)).put("warmup_frozen",true).put("preaim_touch_verified",!spectator).put("spectator",spectator).put("models",m.configuredMask()).put("freeze_check_elapsed_ms",freezeChecked).put("initial_png_capture_ms",SystemClock.elapsedRealtime()-captureBegan).put("freeze_checked_before_png_io",true);
    while(SystemClock.elapsedRealtime()-began<MatchIntro.DURATION_MS+seconds*1000L)SystemClock.sleep(100);
    Assert.assertFalse("Simulation begins after warmup",Arrays.equals(initial,NativeInterface.modelSnapshot()));picture(c,"free-running-"+map+"-"+(seed+trial)+".png");
    // Six separate JNI score reads are not an atomic snapshot during conversion.
    // Use the ordinary background lifecycle to quiesce the worker, not a looser total.
    scenario.moveToState(androidx.lifecycle.Lifecycle.State.CREATED);SystemClock.sleep(150);
    Assert.assertTrue((Boolean)field(activity[0],"paused"));
    JSONArray scores=new JSONArray();int total=0;for(int p=0;p<6;p++){int n=NativeInterface.teamScore(p);scores.put(n);total+=n;}Assert.assertEquals(NativeInterface.totalParticles(),total);Assert.assertTrue("Items may grow or eliminate; live total remains bounded",total>=0&&total<=108000);row.put("scores",scores).put("scores_sampled_after_lifecycle_pause",true);report.put(row);
   }
  }
  try(Writer out=new OutputStreamWriter(c.openFileOutput("free-probe-"+map+"-"+seed+".json",Context.MODE_PRIVATE),"UTF-8")){out.write(report.toString(2));}
 }
}
