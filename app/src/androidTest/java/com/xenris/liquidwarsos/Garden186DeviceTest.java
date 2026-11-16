package com.xenris.liquidwarsos;

import android.app.*;
import android.content.*;
import android.graphics.Bitmap;
import android.os.SystemClock;
import android.view.*;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import org.junit.*;
import org.junit.runner.RunWith;
import org.json.JSONObject;
import java.io.*;

/** Isolated MuMu playtest. No account, mail, web deployment or production data. */
@RunWith(AndroidJUnit4.class)
public class Garden186DeviceTest {
 @Before public void historicalVersionOnly(){org.junit.Assume.assumeTrue("1.86 small-map baseline; 1.87 uses sixfold geometry and user playtesting",BuildConfig.VERSION_NAME.equals("1.86-QA"));}
 final Instrumentation inst=InstrumentationRegistry.getInstrumentation();
 final Context ctx=inst.getTargetContext();
 Activity menu,game; MyGLSurfaceView surface; TacticalOverlay controls;
 void main(Runnable action){inst.runOnMainSync(action);}
 void waitMs(long ms){SystemClock.sleep(ms);}
 OperationsSnapshot facts(){return new OperationsSnapshot(NativeInterface.operationsSnapshot(0,true));}
 void event(int action,float x,float y,long down){MotionEvent e=MotionEvent.obtain(down,SystemClock.uptimeMillis(),action,x,y,0);inst.sendPointerSync(e);e.recycle();}
 void tap(float x,float y){long at=SystemClock.uptimeMillis();event(0,x,y,at);waitMs(40);event(1,x,y,at);waitMs(200);}
 float[] screen(int x,int y){int[] loc=new int[2];main(()->surface.getLocationOnScreen(loc));return new float[]{loc[0]+surface.camera.screenX(x,surface.getWidth()),loc[1]+surface.camera.screenY(y,surface.getHeight())};}
 void point(int x,int y){float[] p=screen(x,y);tap(p[0],p[1]);}
 void button(int n){float dp=ctx.getResources().getDisplayMetrics().density;int[] loc=new int[2];main(()->controls.getLocationOnScreen(loc));float w=Math.min(66*dp,(controls.getWidth()-20*dp)/9f);tap(loc[0]+10*dp+(n+.5f)*w,loc[1]+controls.getHeight()-38*dp);}
 void shot(String name)throws Exception{android.os.Bundle status=new android.os.Bundle();status.putString("stream","\nGARDEN_SHOT:"+name+"\n");inst.sendStatus(0,status);waitMs(1200);}
 Activity launch(String activity){return inst.startActivitySync(new Intent().setClassName(ctx.getPackageName(),"com.xenris.liquidwarsos.activities."+activity).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));}
 void openGarden() {
  Instrumentation.ActivityMonitor monitor=inst.addMonitor("com.xenris.liquidwarsos.activities.GameActivity",null,false);
  main(()->menu.findViewById(R.id.garden_trial).performClick());game=monitor.waitForActivityWithTimeout(10000);inst.removeMonitor(monitor);Assert.assertNotNull(game);
  surface=game.findViewById(R.id.mySurfaceView);controls=game.findViewById(R.id.tactical_overlay);
  for(int i=0;i<20&&NativeInterface.gardenRenderStatus()!=1;i++)waitMs(500);
  Assert.assertEquals("GLES depth/mesh/assets must initialize",1,NativeInterface.gardenRenderStatus());
  for(int i=0;i<40&&NativeInterface.playTimeMillis()<500;i++)waitMs(500);
  Assert.assertTrue("match must pass intro and start ticking",NativeInterface.playTimeMillis()>=500);
 }
 @Test public void playableGardenAndClassicReturn()throws Exception {
  Assert.assertTrue("Test must never target the production identity",ctx.getPackageName().endsWith(".garden186"));
  menu=launch("MainMenuActivity");waitMs(1000);shot("menu");
  int originalMap=StaticBits.map,originalDots=StaticBits.dotsPerTeam,originalMode=StaticBits.operationsMode;
  openGarden();int[] scene=NativeInterface.sceneInfo();Assert.assertEquals(800,scene[0]);Assert.assertEquals(450,scene[1]);Assert.assertEquals(1001,scene[2]);
  Assert.assertEquals(0,NativeInterface.teamScore(1));Assert.assertEquals(0,NativeInterface.teamScore(3));Assert.assertEquals(3,facts().sites());
  Assert.assertTrue(NativeInterface.teamScore(0)>900);Assert.assertTrue(NativeInterface.teamScore(2)>900);shot("overview");
  point(280,165);OperationsSnapshot a=facts();int g=a.group(0,0);Assert.assertEquals("tap commits persistent order",1,a.raw[g+3]);
  waitMs(800);a=facts();Assert.assertEquals("release preserves order",1,a.raw[a.group(0,0)+3]);
  float[] p=screen(260,165),q=screen(320,155);long down=SystemClock.uptimeMillis();event(0,p[0],p[1],down);waitMs(300);event(2,q[0],q[1],down);waitMs(200);event(1,q[0],q[1],down);waitMs(400);
  a=facts();g=a.group(0,0);Assert.assertTrue(a.raw[g+7]>0);Assert.assertTrue("hold-drag endpoint follows finger",Math.abs(a.raw[g+8]-320)<8);
  button(5);a=facts();Assert.assertTrue("single finger split",a.split(0));button(3);point(503,103);a=facts();Assert.assertEquals(1,a.raw[a.group(0,1)+3]);
  button(0);p=screen(270,150);q=screen(340,115);down=SystemClock.uptimeMillis();event(0,p[0],p[1],down);for(int k=1;k<=8;k++){event(2,p[0]+(q[0]-p[0])*k/8,p[1]+(q[1]-p[1])*k/8,down);waitMs(30);}event(1,q[0],q[1],down);waitMs(500);a=facts();Assert.assertTrue("draw commits multiple waypoints",a.raw[a.group(0,1)+7]>=2);shot("route-squad");
  button(7);a=facts();Assert.assertEquals(1,a.raw[a.group(0,1)+4]);button(6);waitMs(300);Assert.assertFalse(facts().split(0));
  button(0);button(0); // DRAW -> legacy FOLLOW -> TAP
  main(()->inst.callActivityOnPause(game));waitMs(300);int clock=NativeInterface.playTimeMillis();waitMs(1200);Assert.assertEquals("pause freezes authoritative clock",clock,NativeInterface.playTimeMillis());main(()->inst.callActivityOnResume(game));waitMs(1800);Assert.assertTrue(NativeInterface.playTimeMillis()>clock);Assert.assertEquals(1,NativeInterface.gardenRenderStatus());shot("resumed");
  main(()->game.finish());waitMs(1000);Assert.assertFalse(GardenSession.active);Assert.assertEquals(originalMap,StaticBits.map);Assert.assertEquals(originalDots,StaticBits.dotsPerTeam);Assert.assertEquals(originalMode,StaticBits.operationsMode);
  game=launch("GameActivity");waitMs(5000);Assert.assertTrue("classic renderer remains available",NativeInterface.sceneInfo()[2]!=1001);Assert.assertTrue(NativeInterface.totalParticles()>0);shot("classic-neighbor");main(()->game.finish());waitMs(800);
  openGarden();Assert.assertEquals(1,NativeInterface.gardenRenderStatus());shot("reentered");
  try(FileWriter out=new FileWriter(new File(ctx.getExternalFilesDir(null),"garden186-result.json"))){out.write(new JSONObject().put("result","PASS").put("environment","MuMu x86_64").put("checks","menu entry; real GL mesh/depth; private map; two armies; tap release; hold drag; route; split/select/stop/merge; player capture and production; pause resume; settings restored; classic battle; second entry").toString(2));}
  main(()->game.finish());
 }
 @Test public void productionInFreshRound()throws Exception {
  Assert.assertTrue(ctx.getPackageName().endsWith(".garden186"));
  menu=launch("MainMenuActivity");waitMs(500);openGarden();
  OperationsSnapshot a=facts();int site=a.site(0),center=a.group(0,0);double nearest=Double.MAX_VALUE;
  for(int i=0;i<a.sites();i++){int candidate=a.site(i);double distance=Math.hypot(a.raw[candidate]-a.raw[center+1],a.raw[candidate+1]-a.raw[center+2]);if(distance<nearest){nearest=distance;site=candidate;}}
  point(a.raw[site],a.raw[site+1]);waitMs(600);shot("production-order");
  long sampledAt=SystemClock.uptimeMillis(),framesBefore=surface.framesDrawn();int simBefore=NativeInterface.playTimeMillis();
  boolean produced=false;long until=SystemClock.uptimeMillis()+20000;
  while(SystemClock.uptimeMillis()<until){waitMs(500);a=facts();for(int i=0;i<a.sites();i++){int s=a.site(i);produced|=a.raw[s+2]==0&&a.raw[s+6]>0;}if(produced)break;}
  shot("production-result");
  try(FileWriter out=new FileWriter(new File(ctx.getExternalFilesDir(null),"garden186-economy.json"))){out.write(new JSONObject().put("produced",produced).put("facts",new org.json.JSONArray(facts().raw)).put("elapsed",NativeInterface.playTimeMillis()).put("sample_wall_ms",SystemClock.uptimeMillis()-sampledAt).put("sample_sim_ms",NativeInterface.playTimeMillis()-simBefore).put("completed_draw_calls",surface.framesDrawn()-framesBefore).put("measurement_note","render completions, not SurfaceFlinger presentation FPS").toString(2));}
  Assert.assertTrue("actual player capture and real particle production",produced);shot("captured-production");
  float dp=ctx.getResources().getDisplayMetrics().density;tap(210*dp,26*dp);waitMs(800);Assert.assertFalse("follow zoom",surface.camera.overview);shot("close-up");tap(210*dp,26*dp);waitMs(300);Assert.assertTrue(surface.camera.overview);
  main(()->game.finish());
 }
 @After public void cleanup(){if(game!=null&&!game.isFinishing())main(()->game.finish());if(menu!=null)main(()->menu.finish());}
}
