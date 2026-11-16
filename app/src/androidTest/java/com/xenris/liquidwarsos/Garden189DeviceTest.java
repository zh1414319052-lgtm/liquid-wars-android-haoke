package com.xenris.liquidwarsos;
import android.app.*;import android.content.*;import android.os.*;import android.view.*;
import androidx.test.platform.app.InstrumentationRegistry;import androidx.test.ext.junit.runners.AndroidJUnit4;
import org.junit.*;import org.junit.runner.RunWith;import org.json.*;import java.io.*;
@RunWith(AndroidJUnit4.class)
public class Garden189DeviceTest {
 final Instrumentation inst=InstrumentationRegistry.getInstrumentation();final Context ctx=inst.getTargetContext();
 Activity menu,game;MyGLSurfaceView surface;TacticalOverlay controls;JSONArray perf=new JSONArray();
 void main(Runnable r){inst.runOnMainSync(r);}void waitMs(long n){SystemClock.sleep(n);}
 void shot(String name){android.graphics.Bitmap image=inst.getUiAutomation().takeScreenshot();Assert.assertNotNull(image);try(FileOutputStream out=new FileOutputStream(new File(ctx.getExternalFilesDir(null),name+".png"))){image.compress(android.graphics.Bitmap.CompressFormat.PNG,100,out);}catch(IOException e){throw new RuntimeException(e);}image.recycle();Bundle b=new Bundle();b.putString("stream","\nGARDEN189_SHOT:"+name+"\n");inst.sendStatus(0,b);waitMs(300);}
 void event(int a,float x,float y,long down){MotionEvent e=MotionEvent.obtain(down,SystemClock.uptimeMillis(),a,x,y,0);inst.sendPointerSync(e);e.recycle();}
 void tap(float x,float y){long t=SystemClock.uptimeMillis();event(0,x,y,t);waitMs(45);event(1,x,y,t);waitMs(200);}
 float[] screen(int x,int y){int[] l=new int[2];main(()->surface.getLocationOnScreen(l));return new float[]{l[0]+surface.camera.screenX(x,surface.getWidth()),l[1]+surface.camera.screenY(y,surface.getHeight())};}
 void point(int x,int y){float[] p=screen(x,y);tap(p[0],p[1]);}
 void button(int n){float dp=ctx.getResources().getDisplayMetrics().density;int[] l=new int[2];main(()->controls.getLocationOnScreen(l));float w=Math.min(66*dp,(controls.getWidth()-20*dp)/9f);tap(l[0]+10*dp+(n+.5f)*w,l[1]+controls.getHeight()-38*dp);}
 void camera(int x,int y){main(()->{surface.camera.pan(x,y);surface.applyCamera();});waitMs(350);}
 OperationsSnapshot facts(){return new OperationsSnapshot(NativeInterface.operationsSnapshot(0,true));}
 void open(){Instrumentation.ActivityMonitor m=inst.addMonitor("com.xenris.liquidwarsos.activities.GameActivity",null,false);main(()->menu.findViewById(R.id.garden_trial).performClick());game=m.waitForActivityWithTimeout(30000);inst.removeMonitor(m);Assert.assertNotNull(game);surface=game.findViewById(R.id.mySurfaceView);controls=game.findViewById(R.id.tactical_overlay);for(int i=0;i<200&&(NativeInterface.gardenRenderStatus()!=1||surface.camera.id!=1001||surface.framesDrawn()<2);i++)waitMs(100);Assert.assertEquals("Live shader/mesh/shadow framebuffer",1,NativeInterface.gardenRenderStatus());}
 void metrics()throws JSONException{perf.put(new JSONArray(NativeInterface.gardenDiagnostics()));}
 @Test public void actualDynamicScene()throws Exception{
  Assert.assertEquals("1.89-QA",BuildConfig.VERSION_NAME);Assert.assertTrue(ctx.getPackageName().endsWith(".garden186"));
  menu=inst.startActivitySync(new Intent().setClassName(ctx.getPackageName(),"com.xenris.liquidwarsos.activities.MainMenuActivity").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));int oldMap=StaticBits.map;long at=SystemClock.uptimeMillis();open();long load=SystemClock.uptimeMillis()-at;
  short[] births=NativeInterface.initialPositions();int[] particles=NativeInterface.modelSnapshot();int green=0;
  for(int i=217;i<particles.length;i++){int v=particles[i];if(((v>>>24)&7)==0){green++;Assert.assertTrue(Math.hypot((v&4095)-births[0],((v>>>12)&4095)-births[1])<=22);}}
  Assert.assertEquals(1000,green);shot("random-compact-live");for(int i=0;i<40&&NativeInterface.playTimeMillis()<200;i++)waitMs(100);
  float density=ctx.getResources().getDisplayMetrics().density;tap(285*density,26*density);Assert.assertFalse(surface.camera.detail);Assert.assertEquals(400f,surface.camera.viewWidth,.01f);
  int[] start=Garden189Probe.fixture(0);Assert.assertNotNull("bridge fixture has two complete legal armies",start);camera(2246,1092);shot("bridge-before-flow");waitMs(1800);shot("bridge-after-flow");
  float[] p=screen(start[0]+12,start[1]),q=screen(2268,1092);long down=SystemClock.uptimeMillis();event(0,p[0],p[1],down);waitMs(320);
  for(int k=1;k<=18;k++){event(2,p[0]+(q[0]-p[0])*k/18,p[1]+(q[1]-p[1])*k/18,down);waitMs(35);}event(1,q[0],q[1],down);waitMs(600);
  Assert.assertEquals(1,facts().raw[facts().group(0,0)+3]);camera(2246,1092);shot("held-release-crossing");point(2340,1092);camera(2246,1092);
  boolean crossed=false,combat=false;
  for(int i=0;i<110;i++){NativeInterface.setPlayerPosition(2,new short[]{2258,-1,-1,-1,-1},new short[]{1092,-1,-1,-1,-1});waitMs(100);metrics();int[] snap=NativeInterface.modelSnapshot();for(int k=217;k<snap.length;k++){int v=snap[k];if(((v>>>24)&7)==0&&(v&4095)>2255)crossed=true;}combat|=NativeInterface.teamScore(0)!=1000||NativeInterface.teamScore(2)!=1000;}
  shot("bridge-contact");try(FileWriter out=new FileWriter(new File(ctx.getExternalFilesDir(null),"garden189-bridge.json"))){out.write(new JSONObject().put("facts",new JSONArray(facts().raw)).put("crossed",crossed).put("combat",combat).put("perf",perf).toString(2));}Assert.assertTrue("real particles cross the bridge",crossed);Assert.assertTrue("real contact changes army ownership/count",combat);
  // Single-finger squad commands use actual overlay buttons/touches.
  button(5);Assert.assertTrue(facts().split(0));button(3);point(2200,1102);button(7);Assert.assertEquals(1,facts().raw[facts().group(0,1)+4]);button(6);Assert.assertFalse(facts().split(0));
  main(()->inst.callActivityOnPause(game));waitMs(250);int frozen=NativeInterface.playTimeMillis();double phase=NativeInterface.gardenDiagnostics()[4];waitMs(700);Assert.assertEquals(frozen,NativeInterface.playTimeMillis());Assert.assertEquals(phase,NativeInterface.gardenDiagnostics()[4],.00001);
  long resumeAt=SystemClock.uptimeMillis();main(()->inst.callActivityOnResume(game));for(int k=0;k<80&&NativeInterface.playTimeMillis()<=frozen;k++)waitMs(100);long resume=SystemClock.uptimeMillis()-resumeAt;Assert.assertTrue(NativeInterface.playTimeMillis()>frozen);
  // Recreate all shader/VBO/FBO resources on the actual GL thread.
  java.util.concurrent.CountDownLatch rebuilt=new java.util.concurrent.CountDownLatch(1);surface.queueEvent(()->{Garden189Probe.surface();rebuilt.countDown();});Assert.assertTrue(rebuilt.await(20,java.util.concurrent.TimeUnit.SECONDS));waitMs(600);Assert.assertEquals(1,NativeInterface.gardenRenderStatus());shot("resources-restored");
  Garden189Probe.rewind();waitMs(300);Assert.assertEquals(2000,NativeInterface.totalParticles());Assert.assertEquals(0,NativeInterface.playTimeMillis());
  start=Garden189Probe.fixture(1);Assert.assertNotNull(start);OperationsSnapshot f=facts();int site=0;double near=Double.MAX_VALUE;for(int i=0;i<f.sites();i++){int s=f.site(i);double d=Math.hypot(start[0]-f.raw[s],start[1]-f.raw[s+1]);if(d<near){near=d;site=i;}}
  int s=f.site(site),sx=f.raw[s],sy=f.raw[s+1];camera(sx,sy);point(sx,sy);camera(sx,sy);shot("capture-live");boolean produced=false;
  for(int i=0;i<220;i++){waitMs(100);metrics();f=facts();int ss=f.site(site);if(f.raw[ss+2]==0&&f.raw[ss+6]>0){produced=true;break;}}
  Assert.assertTrue("captured site emits real new particles",produced);tap(285*density,26*density);Assert.assertTrue(surface.camera.detail);Assert.assertEquals(200f,surface.camera.viewWidth,.01f);camera(sx,sy);waitMs(2000);shot("production-live");int stressCount=Garden189Probe.growth();Assert.assertTrue(stressCount>=5800);for(int i=0;i<70;i++){waitMs(100);metrics();}shot("growth-load");
  main(()->{for(int k=0;k<2&&!surface.camera.overview;k++)surface.camera.toggle(sx,sy);surface.applyCamera();});Assert.assertTrue(surface.camera.overview);
  waitMs(800);shot("whole-live-mainland");for(int i=0;i<40;i++){waitMs(100);metrics();}
  main(()->game.finish());waitMs(900);Assert.assertFalse(GardenSession.active);Assert.assertEquals(oldMap,StaticBits.map);
  // Original GLES1 map still renders after the GLES2 context has been destroyed.
  game=inst.startActivitySync(new Intent().setClassName(ctx.getPackageName(),"com.xenris.liquidwarsos.activities.GameActivity").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));waitMs(2000);Assert.assertTrue(NativeInterface.sceneInfo()[2]!=1001);shot("classic-gles1-neighbor");main(()->game.finish());waitMs(700);
  open();short[] second=NativeInterface.initialPositions();Assert.assertTrue(second[0]!=births[0]||second[1]!=births[1]);shot("new-random-live");
  try(FileWriter out=new FileWriter(new File(ctx.getExternalFilesDir(null),"garden189-result.json"))){out.write(new JSONObject().put("result","PASS").put("load_ms",load).put("resume_ms",resume).put("first_births",new JSONArray(births)).put("second_births",new JSONArray(second)).put("bridge_crossed",crossed).put("contact_changed_counts",combat).put("produced",produced).put("performance_samples",perf).put("note","Fixture only relocates complete armies for bridge/production checks. Touches, pathfinding, combat, capture and spawning are real. CPU draw durations/intervals are not GPU presentation timing.").toString(2));}
 }
 @After public void cleanup(){if(game!=null&&!game.isFinishing())main(()->game.finish());if(menu!=null&&!menu.isFinishing())main(()->menu.finish());}
}
