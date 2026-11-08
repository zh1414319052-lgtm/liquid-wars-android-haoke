// SPDX-License-Identifier: GPL-3.0-or-later
package com.xenris.liquidwarsos;
import android.app.*;import android.content.*;import android.graphics.*;import android.os.*;import android.view.*;
import androidx.lifecycle.Lifecycle;import androidx.test.core.app.ActivityScenario;import androidx.test.ext.junit.runners.AndroidJUnit4;import androidx.test.platform.app.InstrumentationRegistry;
import com.xenris.liquidwarsos.activities.GameActivity;import com.xenris.liquidwarsos.cloud.*;import org.junit.*;import org.junit.runner.RunWith;import org.json.*;import java.io.*;import java.lang.reflect.*;import java.util.*;

@RunWith(AndroidJUnit4.class)
public class World169ProbeTest {
 static Context context(){return InstrumentationRegistry.getInstrumentation().getTargetContext();}
 static Object get(Object a,String n)throws Exception{Field f=a.getClass().getDeclaredField(n);f.setAccessible(true);return f.get(a);}
 static void set(Object a,String n,Object v)throws Exception{Field f=a.getClass().getDeclaredField(n);f.setAccessible(true);f.set(a,v);}
 static void invoke(Object a,String n)throws Exception{Method m=a.getClass().getDeclaredMethod(n);m.setAccessible(true);m.invoke(a);}
 static void picture(String name)throws Exception{Bitmap b=InstrumentationRegistry.getInstrumentation().getUiAutomation().takeScreenshot();try(OutputStream out=context().openFileOutput(name,0)){b.compress(Bitmap.CompressFormat.JPEG,90,out);}b.recycle();}
 static void write(String name,JSONObject value)throws Exception{try(Writer out=new OutputStreamWriter(context().openFileOutput(name,0),"UTF-8")){out.write(value.toString(2));}}
 static void touch(long start,int action,float x,float y){MotionEvent e=MotionEvent.obtain(start,SystemClock.uptimeMillis(),action,x,y,0);e.setSource(InputDevice.SOURCE_TOUCHSCREEN);Assert.assertTrue(InstrumentationRegistry.getInstrumentation().getUiAutomation().injectInputEvent(e,true));e.recycle();}
 static void tap(float x,float y){long now=SystemClock.uptimeMillis();touch(now,0,x,y);touch(now,1,x,y);}
 static Intent prepare(int map,int seed,int dots,boolean ready,boolean spectate){StaticBits.init();StaticBits.map=map;StaticBits.seed=seed;StaticBits.team=2;StaticBits.dotsPerTeam=dots;StaticBits.timeLimit=240;StaticBits.aiDifficulty=AiDifficulty.HARD;StaticBits.alliances=AllianceRules.solo();return new Intent(context(),GameActivity.class).putExtra("qa_model_teams",0).putExtra("qa_ruins_ready",ready).putExtra("spectator",spectate).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);}
 static void frozen(ActivityScenario<GameActivity> s)throws Exception{SystemClock.sleep(450);s.onActivity(a->{try{set(a,"resumed",false);invoke(a,"updatePauseState");AlertDialog d=(AlertDialog)get(a,"dialog");if(d!=null)d.dismiss();a.findViewById(R.id.match_intro).setVisibility(View.GONE);}catch(Exception e){throw new RuntimeException(e);}});SystemClock.sleep(250);}
 static short[][] targets(ActivityScenario<GameActivity> s)throws Exception{short[][][] out={null};s.onActivity(a->{try{short[][] xs=(short[][])get(a,"xs"),ys=(short[][])get(a,"ys");synchronized(xs){out[0]=new short[][]{xs[StaticBits.team].clone(),ys[StaticBits.team].clone()};}}catch(Exception e){throw new RuntimeException(e);}});return out[0];}
 @Test public void fixedControlsAndActualFiveTimesWorlds()throws Exception{
  JSONArray records=new JSONArray();for(int map:new int[]{50,51})try(ActivityScenario<GameActivity> s=ActivityScenario.launch(prepare(map,16900+map,1000,true,false))){
   frozen(s);MyGLSurfaceView[] gl={null};ItemOverlay[] hud={null};Rect b=new Rect(),screen=new Rect();s.onActivity(a->{gl[0]=a.findViewById(R.id.mySurfaceView);hud[0]=a.findViewById(R.id.item_overlay);hud[0].getGlobalVisibleRect(b);gl[0].getGlobalVisibleRect(screen);});
   long deadline=SystemClock.uptimeMillis()+6000;while(gl[0].camera.width!=2000&&SystemClock.uptimeMillis()<deadline)SystemClock.sleep(40);
   int[] info=NativeInterface.sceneInfo();Assert.assertEquals(2000,info[0]);Assert.assertEquals(960,info[1]);Assert.assertEquals(1920000,NativeInterface.modelTerrain().length);Assert.assertFalse(gl[0].camera.overview);
   int[] dots=NativeInterface.modelSnapshot();byte[] wall=NativeInterface.modelTerrain();HashSet<Integer> occupied=new HashSet<>();for(int i=ModelWorld.HEADER;i<dots.length;i++){int x=dots[i]&2047,y=dots[i]>>>11&1023;Assert.assertTrue(x<2000&&y<960);Assert.assertEquals(0,wall[y*2000+x]);Assert.assertTrue(occupied.add(y*2000+x));}Assert.assertEquals(6000,occupied.size());picture("world169-"+map+"-near.jpg");
   float dp=context().getResources().getDisplayMetrics().density,x=b.left+76*dp,y=b.bottom-66*dp;long t=SystemClock.uptimeMillis();touch(t,0,x+9*dp,y-5*dp);touch(t,2,x+110*dp,y-25*dp);SystemClock.sleep(320);
   s.onActivity(a->{try{Assert.assertEquals(76*dp,((Number)get(hud[0],"padX")).floatValue(),1);Assert.assertEquals(hud[0].getHeight()-66*dp,((Number)get(hud[0],"padY")).floatValue(),1);Assert.assertTrue(Math.hypot(((Number)get(hud[0],"knobX")).floatValue(),((Number)get(hud[0],"knobY")).floatValue())<=42*dp+1);}catch(Exception e){throw new RuntimeException(e);}});
   short[][] moved=targets(s);Assert.assertTrue(moved[0][0]>=0&&moved[0][0]<2000&&moved[1][0]>=0&&moved[1][0]<960);touch(t,1,x+110*dp,y-25*dp);short[][] released=targets(s);SystemClock.sleep(120);Assert.assertArrayEquals(released[0],targets(s)[0]);picture("world169-"+map+"-fixed.jpg");
   tap(b.right-155*dp+143*dp*.88f,b.bottom-235*dp+70*dp*.18f);SystemClock.sleep(140);Assert.assertFalse(gl[0].camera.following);Assert.assertTrue(gl[0].camera.left>=1000);picture("world169-"+map+"-pan.jpg");
   float px=screen.left+screen.width()*.5f,py=screen.top+screen.height()*.5f;int expectX=gl[0].camera.mapX(screen.width()*.5f,screen.width()),expectY=gl[0].camera.mapY(screen.height()*.5f,screen.height());tap(px,py);short[][] clicked=targets(s);Assert.assertEquals(expectX,clicked[0][0]);Assert.assertEquals(expectY,clicked[1][0]);Assert.assertTrue(clicked[0][0]>800);
   tap(b.left+210*dp,b.top+25*dp);SystemClock.sleep(80);tap(b.left+210*dp,b.top+25*dp);SystemClock.sleep(120);Assert.assertTrue(gl[0].camera.overview);Assert.assertTrue(gl[0].camera.viewWidth>=2000);Assert.assertEquals(gl[0].getWidth()/(float)gl[0].getHeight(),gl[0].camera.viewWidth/gl[0].camera.viewHeight,.001);picture("world169-"+map+"-full.jpg");
   long before=gl[0].framesDrawn();s.moveToState(Lifecycle.State.CREATED);SystemClock.sleep(100);Assert.assertFalse(gl[0].pacingActive());s.moveToState(Lifecycle.State.RESUMED);frozen(s);deadline=SystemClock.uptimeMillis()+4000;while(gl[0].framesDrawn()<=before&&SystemClock.uptimeMillis()<deadline)SystemClock.sleep(40);Assert.assertTrue(gl[0].framesDrawn()>before);Assert.assertTrue(gl[0].camera.overview);picture("world169-"+map+"-resumed.jpg");
   records.put(new JSONObject().put("map",map).put("width",info[0]).put("height",info[1]).put("area_ratio",5).put("particles",occupied.size()).put("fixed_base",true).put("near_target_kept_on_release",true).put("world_click",new JSONArray().put(expectX).put(expectY)).put("minimap_pan",true).put("overview",true).put("egl_resume",true));
  }write("world169-controls.json",new JSONObject().put("status","PASS").put("maps",records).put("models",0).put("real_touch",true).put("controlled_simulation",true).put("physical_phone",false).put("human_play",false));
 }
 @Test public void normalWorldsShortSamples()throws Exception{
  JSONArray records=new JSONArray();for(int map:new int[]{50,51})try(ActivityScenario<GameActivity> s=ActivityScenario.launch(prepare(map,16900+map,1000,false,true))){
   MyGLSurfaceView[] gl={null};s.onActivity(a->gl[0]=a.findViewById(R.id.mySurfaceView));long deadline=SystemClock.uptimeMillis()+20000;while(NativeInterface.playTimeMillis()==0&&SystemClock.uptimeMillis()<deadline)SystemClock.sleep(30);Assert.assertTrue(NativeInterface.playTimeMillis()>0);
   long began=System.nanoTime(),frames=gl[0].framesDrawn();int sim=NativeInterface.playTimeMillis();SystemClock.sleep(6000);long elapsed=System.nanoTime()-began;int advanced=NativeInterface.playTimeMillis()-sim;Assert.assertTrue(advanced>0&&gl[0].framesDrawn()>frames);Debug.MemoryInfo memory=new Debug.MemoryInfo();Debug.getMemoryInfo(memory);picture("world169-"+map+"-live.jpg");
   records.put(new JSONObject().put("map",map).put("sample_ns",elapsed).put("gl_render_completions",gl[0].framesDrawn()-frames).put("simulation_progress_ms",advanced).put("pss_kib",memory.getTotalPss()).put("display_hz",gl[0].getDisplay().getRefreshRate()));
  }write("world169-live.json",new JSONObject().put("status","PASS").put("normal_worker",true).put("maps",records).put("models",0).put("recording_during_sample",false).put("render_completions_not_present_fps",true).put("physical_phone",false).put("human_play",false));
 }
 @Test public void resetPickupDoesNotReplayWave()throws Exception{
  Bundle args=InstrumentationRegistry.getArguments();int seed=Integer.parseInt(args.getString("reset_seed","100")),team=Integer.parseInt(args.getString("reset_team","0"));Intent i=prepare(1,seed,100,true,false);StaticBits.team=team;
  try(ActivityScenario<GameActivity> s=ActivityScenario.launch(i)){
   frozen(s);short[] idle={-2,-1,-1,-1,-1};for(int p=0;p<6;p++)NativeInterface.setPlayerPosition(p,idle,idle);
   for(int step=0;step<2200&&NativeInterface.itemSnapshot()[2]==0;step++)NativeInterface.stepDots();int[] before=NativeInterface.itemSnapshot();int at=-1;for(int k=0;k<SnapshotItems.slots(before);k++)if(before[SnapshotItems.offset(before,k)]==4)at=SnapshotItems.offset(before,k);Assert.assertTrue("Fixture has RESET",at>=0);int x=before[at+1],y=before[at+2];
   short[] tx=idle.clone(),ty=idle.clone();tx[0]=(short)x;ty[0]=(short)y;NativeInterface.setPlayerPosition(team,tx,ty);for(int step=0;step<1600&&NativeInterface.itemSnapshot()[146]==0;step++)NativeInterface.stepDots();int[] reset=NativeInterface.itemSnapshot();Assert.assertEquals(1,reset[146]);Assert.assertTrue(reset[207]>0);picture("world169-reset-eaten.jpg");
   s.onActivity(a->{try{invoke(a,"syncRewind");}catch(Exception e){throw new RuntimeException(e);}});for(int p=0;p<6;p++)NativeInterface.setPlayerPosition(p,idle,idle);
   for(int step=0;step<2600&&NativeInterface.itemSnapshot()[2]==0;step++)NativeInterface.stepDots();int[] after=NativeInterface.itemSnapshot();int count=SnapshotItems.count(after);Assert.assertTrue(count>=1&&count<=6);JSONArray kinds=new JSONArray();for(int k=0;k<SnapshotItems.slots(after);k++){int offset=SnapshotItems.offset(after,k),kind=after[offset];if(kind>=0){Assert.assertNotEquals(4,kind);kinds.put(kind);}}SystemClock.sleep(100);picture("world169-reset-next.jpg");
   write("world169-reset.json",new JSONObject().put("status","PASS").put("actual_native_pickup",true).put("epoch_after",reset[146]).put("after_count",count).put("after_kinds",kinds).put("no_immediate_reset",true).put("models",0).put("real_core_controlled_steps",true).put("human_play",false));
  }
 }
 @Test public void nativeReplayHashesAreDeterministic()throws Exception{
  JSONArray rows=new JSONArray();try(ActivityScenario<GameActivity> s=ActivityScenario.launch(prepare(50,16950,100,true,true))){
   frozen(s);s.onActivity(a->((MyGLSurfaceView)a.findViewById(R.id.mySurfaceView)).onPause());NativeInterface.destroyGame();
   for(int map:new int[]{50,51})for(int seed=0;seed<4;seed++){
    long[] expected=null;for(int pass=0;pass<2;pass++){
     NativeInterface.createGame(2,map,16900+seed,100);NativeInterface.configureAlliances(AllianceRules.solo());ArrayList<Long> hashes=new ArrayList<>();
     for(int step=0;step<=1600;step++){
      if(step%100==0)hashes.add(NativeInterface.stateHash());if(step==1600)break;
      if(step%10==0)for(int t=0;t<6;t++){short[] x={(short)(10+(step*17+t*311)%1980),-1,-1,-1,-1},y={(short)(8+(step*9+t*133)%944),-1,-1,-1,-1};NativeInterface.setPlayerPosition(t,x,y);}NativeInterface.stepDots();
     }
     long[] actual=new long[hashes.size()];for(int k=0;k<actual.length;k++)actual[k]=hashes.get(k);
     if(pass==0)expected=actual;else Assert.assertArrayEquals("Exact JNI rule hash replay",expected,actual);NativeInterface.destroyGame();
    }JSONArray values=new JSONArray();for(long value:expected)values.put(Long.toHexString(value));rows.put(new JSONObject().put("map",map).put("seed",16900+seed).put("hashes",values));
   }
   NativeInterface.createGame(2,50,16950,100);write("world169-native-hashes.json",new JSONObject().put("status","PASS").put("actual_jni_stateHash",true).put("checkpoints",8*17).put("steps_per_pass",1600).put("cases",rows).put("models",0).put("physical_phone",false));
  }
 }

 @Test public void sixItemWaveAndWideVideo()throws Exception{
  int fixture=-1;JSONArray kinds=new JSONArray();
  try(ActivityScenario<GameActivity> s=ActivityScenario.launch(prepare(50,16950,100,true,false))){
   frozen(s);MyGLSurfaceView[] gl={null};Rect bounds=new Rect();s.onActivity(a->{gl[0]=a.findViewById(R.id.mySurfaceView);gl[0].onPause();a.findViewById(R.id.item_overlay).getGlobalVisibleRect(bounds);});NativeInterface.destroyGame();
   for(int seed=0;seed<64;seed++){
    NativeInterface.createGame(2,50,16900+seed,100);NativeInterface.configureAlliances(AllianceRules.solo());short[] idle={-2,-1,-1,-1,-1};for(int t=0;t<6;t++)NativeInterface.setPlayerPosition(t,idle,idle);
    for(int step=0;step<2200&&NativeInterface.itemSnapshot()[2]==0;step++)NativeInterface.stepDots();int[] snap=NativeInterface.itemSnapshot();
    if(SnapshotItems.count(snap)==6){fixture=16900+seed;for(int k=0;k<6;k++)kinds.put(snap[SnapshotItems.offset(snap,k)]);break;}NativeInterface.destroyGame();
   }
   Assert.assertTrue("Actual six-item wave fixture",fixture>=0);s.onActivity(a->gl[0].onResume());SystemClock.sleep(350);float dp=context().getResources().getDisplayMetrics().density;tap(bounds.left+210*dp,bounds.top+25*dp);SystemClock.sleep(180);Assert.assertTrue(gl[0].camera.overview);picture("world169-six-items.jpg");write("world169-six-items.json",new JSONObject().put("status","PASS").put("map",50).put("seed",fixture).put("count",6).put("kinds",kinds).put("real_native_wave",true).put("controlled_steps",true).put("models",0));
  }
  try(ActivityScenario<GameActivity> s=ActivityScenario.launch(prepare(50,16950,1000,false,false))){
   long deadline=SystemClock.uptimeMillis()+20000;while(NativeInterface.playTimeMillis()==0&&SystemClock.uptimeMillis()<deadline)SystemClock.sleep(30);Assert.assertTrue(NativeInterface.playTimeMillis()>0);Rect b=new Rect();s.onActivity(a->a.findViewById(R.id.item_overlay).getGlobalVisibleRect(b));float dp=context().getResources().getDisplayMetrics().density;
   android.os.ParcelFileDescriptor record=InstrumentationRegistry.getInstrumentation().getUiAutomation().executeShellCommand("screenrecord --time-limit 20 --size 1920x872 --bit-rate 5000000 /sdcard/Download/world169-map50-live.mp4");
   tap(b.left+210*dp,b.top+25*dp);SystemClock.sleep(2200);tap(b.left+210*dp,b.top+25*dp);float x=b.left+76*dp,y=b.bottom-66*dp;long t=SystemClock.uptimeMillis();touch(t,0,x,y);touch(t,2,x+65*dp,y-20*dp);SystemClock.sleep(2500);touch(t,1,x+65*dp,y-20*dp);SystemClock.sleep(1800);picture("world169-50-live-controls.jpg");
   tap(b.right-155*dp+143*dp*.65f,b.bottom-235*dp+70*dp*.45f);SystemClock.sleep(1800);tap(b.left+210*dp,b.top+25*dp);SystemClock.sleep(1500);
   try(InputStream in=new android.os.ParcelFileDescriptor.AutoCloseInputStream(record)){byte[] bytes=new byte[1024];while(in.read(bytes)>=0){}}write("world169-video.json",new JSONObject().put("status","PASS").put("map",50).put("world_width",2000).put("world_height",960).put("normal_worker",true).put("real_touch",true).put("models",0).put("requested_seconds",20).put("performance_sample",false).put("human_play",false));
  }
 }

}
