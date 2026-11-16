package com.xenris.liquidwarsos;
import android.app.*;import android.content.*;import android.os.*;import android.view.*;
import androidx.test.platform.app.InstrumentationRegistry;import androidx.test.ext.junit.runners.AndroidJUnit4;
import org.junit.*;import org.junit.runner.RunWith;import org.json.*;import java.io.*;
@RunWith(AndroidJUnit4.class)
public class Garden188DeviceTest {
 final Instrumentation inst=InstrumentationRegistry.getInstrumentation();final Context ctx=inst.getTargetContext();
 Activity menu,game;MyGLSurfaceView surface;
 void main(Runnable r){inst.runOnMainSync(r);}void waitMs(long n){SystemClock.sleep(n);}
 void shot(String name){Bundle b=new Bundle();b.putString("stream","\nGARDEN188_SHOT:"+name+"\n");inst.sendStatus(0,b);waitMs(1100);}
 void event(int a,float x,float y,long down){MotionEvent e=MotionEvent.obtain(down,SystemClock.uptimeMillis(),a,x,y,0);inst.sendPointerSync(e);e.recycle();}
 void tap(float x,float y){long t=SystemClock.uptimeMillis();event(0,x,y,t);waitMs(45);event(1,x,y,t);waitMs(200);}
 void point(int x,int y){int[] l=new int[2];main(()->surface.getLocationOnScreen(l));tap(l[0]+surface.camera.screenX(x,surface.getWidth()),l[1]+surface.camera.screenY(y,surface.getHeight()));}
 OperationsSnapshot facts(){return new OperationsSnapshot(NativeInterface.operationsSnapshot(0,true));}
 void open(){Instrumentation.ActivityMonitor m=inst.addMonitor("com.xenris.liquidwarsos.activities.GameActivity",null,false);main(()->menu.findViewById(R.id.garden_trial).performClick());game=m.waitForActivityWithTimeout(30000);inst.removeMonitor(m);Assert.assertNotNull(game);surface=game.findViewById(R.id.mySurfaceView);
  for(int k=0;k<200&&(NativeInterface.gardenRenderStatus()!=1||surface.camera.id!=1001||surface.framesDrawn()<2);k++)waitMs(100);Assert.assertEquals("tiled art and actual GLES initialize",1,NativeInterface.gardenRenderStatus());Assert.assertEquals("UI binds the new world",1001,surface.camera.id);Assert.assertTrue("visible frames complete",surface.framesDrawn()>=2);}
 @Test public void newMainlandRandomOpeningAndControls()throws Exception{
  Assert.assertEquals("1.88-QA",BuildConfig.VERSION_NAME);Assert.assertTrue(ctx.getPackageName().endsWith(".garden186"));
  menu=inst.startActivitySync(new Intent().setClassName(ctx.getPackageName(),"com.xenris.liquidwarsos.activities.MainMenuActivity").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));waitMs(500);
  int originalMap=StaticBits.map;long begin=SystemClock.uptimeMillis();open();long loaded=SystemClock.uptimeMillis()-begin;
  Assert.assertArrayEquals(new int[]{3264,2048,1001},java.util.Arrays.copyOf(NativeInterface.sceneInfo(),3));
  short[] births=NativeInterface.initialPositions();int[] particles=NativeInterface.modelSnapshot();int human=0;
  for(int i=217;i<particles.length;i++){int v=particles[i],t=(v>>>24)&7,x=v&4095,y=(v>>>12)&4095;Assert.assertTrue(x<3264&&y<2048&&t<6);if(t==0){human++;Assert.assertTrue("human opening remains one tight cluster",Math.hypot(x-births[0],y-births[1])<=22);}}
  Assert.assertEquals(1000,human);Assert.assertFalse(surface.camera.overview);Assert.assertEquals(400f,surface.camera.viewWidth,.01f);Assert.assertEquals(5,facts().sites());
  shot("compact-opening");
  for(int i=0;i<30&&NativeInterface.playTimeMillis()<300;i++)waitMs(200);
  byte[] walls=NativeInterface.modelTerrain();int tx=births[0],ty=births[1];boolean picked=false;
  for(int r=90;r>=35&&!picked;r-=10)for(int a=0;a<8&&!picked;a++){int x=births[0]+(int)(Math.cos(a*Math.PI/4)*r),y=births[1]+(int)(Math.sin(a*Math.PI/4)*r);boolean ok=x>=16&&x<3248&&y>=4&&y<2044;
   for(int k=0;k<=r&&ok;k++){int xx=births[0]+(x-births[0])*k/r,yy=births[1]+(y-births[1])*k/r;ok=walls[yy*3264+xx]==0;}if(ok){tx=x;ty=y;picked=true;}}
  Assert.assertTrue("nearby clear movement target",picked);point(tx,ty);waitMs(1500);OperationsSnapshot f=facts();int g=f.group(0,0);Assert.assertEquals("released tap persists",1,f.raw[g+3]);shot("moving-after-release");
  main(()->inst.callActivityOnPause(game));waitMs(200);int clock=NativeInterface.playTimeMillis();waitMs(700);Assert.assertEquals(clock,NativeInterface.playTimeMillis());long resumeAt=SystemClock.uptimeMillis();main(()->inst.callActivityOnResume(game));for(int k=0;k<80&&NativeInterface.playTimeMillis()<=clock;k++)waitMs(100);long resumeMs=SystemClock.uptimeMillis()-resumeAt;Assert.assertTrue("simulation resumes after GL lifecycle",NativeInterface.playTimeMillis()>clock);Assert.assertEquals(1,NativeInterface.gardenRenderStatus());
  float dp=ctx.getResources().getDisplayMetrics().density;tap(210*dp,26*dp);waitMs(500);Assert.assertTrue(surface.camera.overview);shot("mainland-overview");
  // A frozen identical full-map frame supports a baked-reference comparison.
  main(()->inst.callActivityOnPause(game));waitMs(200);JSONObject view=new JSONObject().put("left",surface.camera.left).put("bottom",surface.camera.bottom).put("width",surface.camera.viewWidth).put("height",surface.camera.viewHeight).put("pixels_w",surface.getWidth()).put("pixels_h",surface.getHeight());
  int[] raw=new int[219];raw[216]=2;raw[217]=1700|(1800<<12)|(0<<24)|(10<<27);raw[218]=2200|(1500<<12)|(2<<24)|(10<<27);
  int[] filtered=com.xenris.liquidwarsos.cloud.ModelVisibility.filter(raw,3264,2048,1<<2);Assert.assertEquals(218,filtered.length);Assert.assertEquals(raw[217],filtered[217]);
  main(()->game.finish());waitMs(900);Assert.assertFalse(GardenSession.active);Assert.assertEquals(originalMap,StaticBits.map);
  open();short[] second=NativeInterface.initialPositions();Assert.assertTrue("new round chooses a different random location",second[0]!=births[0]||second[1]!=births[1]);shot("different-random-opening");
  try(FileWriter out=new FileWriter(new File(ctx.getExternalFilesDir(null),"garden188-result.json"))){out.write(new JSONObject().put("result","PASS").put("world",new JSONArray(new int[]{3264,2048})).put("births_first",new JSONArray(births)).put("births_second",new JSONArray(second)).put("load_ms",loaded).put("resume_ms",resumeMs).put("view",view).put("checks","tiled GLES; real compact opening; 5 sites; tap/release; pause/resume; overview; tall visibility; settings restore; new randomized round").toString(2));}
 }
 @After public void cleanup(){if(game!=null&&!game.isFinishing())main(()->game.finish());if(menu!=null&&!menu.isFinishing())main(()->menu.finish());}
}
