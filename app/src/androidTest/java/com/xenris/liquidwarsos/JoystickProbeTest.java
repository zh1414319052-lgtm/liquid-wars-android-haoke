// SPDX-License-Identifier: GPL-3.0-or-later
package com.xenris.liquidwarsos;
import android.app.AlertDialog;import android.content.*;import android.graphics.*;import android.os.*;import android.view.*;
import androidx.test.core.app.ActivityScenario;import androidx.test.ext.junit.runners.AndroidJUnit4;import androidx.test.platform.app.InstrumentationRegistry;
import com.xenris.liquidwarsos.activities.GameActivity;import com.xenris.liquidwarsos.cloud.*;
import org.junit.*;import org.junit.runner.RunWith;import org.json.*;import java.io.*;import java.lang.reflect.*;import java.util.Arrays;
/** Actual touch/real production native core and GLES. No API lanes or invented renderers. */
@RunWith(AndroidJUnit4.class) public class JoystickProbeTest {
 static Object get(Object o,String n)throws Exception{Field f=o.getClass().getDeclaredField(n);f.setAccessible(true);return f.get(o);}
 static void set(Object o,String n,Object v)throws Exception{Field f=o.getClass().getDeclaredField(n);f.setAccessible(true);f.set(o,v);}
 static void touch(long start,int action,float x,float y){MotionEvent e=MotionEvent.obtain(start,SystemClock.uptimeMillis(),action,x,y,0);e.setSource(InputDevice.SOURCE_TOUCHSCREEN);Assert.assertTrue(InstrumentationRegistry.getInstrumentation().getUiAutomation().injectInputEvent(e,true));e.recycle();}
 static short[][] targets(ActivityScenario<GameActivity> s,GameActivity a)throws Exception{short[][][] out={null};s.onActivity(v->{try{short[][] xs=(short[][])get(a,"xs"),ys=(short[][])get(a,"ys");synchronized(xs){out[0]=new short[][]{xs[StaticBits.team].clone(),ys[StaticBits.team].clone()};}}catch(Exception e){throw new RuntimeException(e);}});return out[0];}
 static void picture(Context c,String name)throws Exception{Bitmap b=InstrumentationRegistry.getInstrumentation().getUiAutomation().takeScreenshot();try(OutputStream out=c.openFileOutput(name,0)){b.compress(Bitmap.CompressFormat.JPEG,92,out);}b.recycle();}
 static double mean(int[] s,int team,int x,int y){double sum=0;int n=0;for(int k=ModelWorld.HEADER;k<s.length;k++){int z=s[k];if((z>>19&7)==team){sum+=Math.hypot((z&1023)-x,(z>>10&511)-y);n++;}}return sum/Math.max(1,n);}
 @Test public void fixedStickMovesTouchTarget()throws Exception{
  Context c=InstrumentationRegistry.getInstrumentation().getTargetContext();StaticBits.init();StaticBits.map=28;StaticBits.seed=16701;StaticBits.team=2;StaticBits.dotsPerTeam=1000;StaticBits.timeLimit=240;StaticBits.alliances=AllianceRules.solo();
  Intent intent=new Intent(c,GameActivity.class).putExtra("qa_model_teams",0).putExtra("qa_ruins_ready",true).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
  try(ActivityScenario<GameActivity> s=ActivityScenario.launch(intent)){
   GameActivity[] a={null};s.onActivity(v->a[0]=v);SystemClock.sleep(450);Assert.assertEquals(0,((ModelMatch)get(get(a[0],"models"),"match")).configuredMask());
   s.onActivity(v->{try{set(v,"resumed",false);((AlertDialog)get(v,"dialog")).dismiss();v.findViewById(R.id.match_intro).setVisibility(View.GONE);}catch(Exception e){throw new RuntimeException(e);}});
   short[] idle={-2,-1,-1,-1,-1};for(int t=0;t<6;t++)NativeInterface.setPlayerPosition(t,idle,idle);
   Rect overlay=new Rect(),map=new Rect();s.onActivity(v->{v.findViewById(R.id.item_overlay).getGlobalVisibleRect(overlay);v.findViewById(R.id.mySurfaceView).getGlobalVisibleRect(map);});float dp=c.getResources().getDisplayMetrics().density;
   int[] facts=NativeInterface.aiSnapshot();int mx=facts[2*8+4],my=facts[2*8+5];byte[] walls=NativeInterface.modelTerrain();int best=0;double angle=0;
   for(int k=0;k<32;k++){double rad=k*Math.PI/16;int distance=0;for(int d=1;d<=88;d++){int x=mx+(int)(Math.cos(rad)*d),y=my+(int)(Math.sin(rad)*d);if(x<0||y<0||x>=800||y>=480||walls[y*800+x]!=0)break;distance=d;}if(distance>best){best=distance;angle=rad;}}
   Assert.assertTrue("Fixture has a clear lane",best>=35);float vx=(float)Math.cos(angle),vy=-(float)Math.sin(angle),x=overlay.left+84*dp,y=overlay.bottom-66*dp;long began=SystemClock.uptimeMillis();
   touch(began,MotionEvent.ACTION_DOWN,x,y);short[][] start=targets(s,a[0]);Assert.assertTrue(Math.hypot(start[0][0]-mx,start[1][0]-my)<5);picture(c,"world169-joystick-start.jpg");
   float pixelDx=vx*map.width()/800f,pixelDy=vy*map.height()/480f,length=(float)Math.hypot(pixelDx,pixelDy);float fingerX=x+pixelDx/length*96*dp,fingerY=y+pixelDy/length*96*dp;touch(began,MotionEvent.ACTION_MOVE,fingerX,fingerY);SystemClock.sleep(280);
   s.onActivity(v->{try{ItemOverlay ui=v.findViewById(R.id.item_overlay);Assert.assertEquals(76*dp,((Number)get(ui,"padX")).floatValue(),1);Assert.assertEquals(ui.getHeight()-66*dp,((Number)get(ui,"padY")).floatValue(),1);Assert.assertTrue(Math.hypot(((Number)get(ui,"knobX")).floatValue(),((Number)get(ui,"knobY")).floatValue())<=42*dp+1);}catch(Exception e){throw new RuntimeException(e);}});
   short[][] moved=targets(s,a[0]);Assert.assertTrue("Stick moves ordinary touch near army",Math.hypot(moved[0][0]-mx,moved[1][0]-my)>20);Assert.assertTrue(Math.hypot(moved[0][0]-mx,moved[1][0]-my)<=89);Assert.assertTrue(moved[0][0]>=0&&moved[1][0]>=0);Assert.assertEquals(-1,moved[0][1]);picture(c,"world169-joystick-drag.jpg");
   touch(began,MotionEvent.ACTION_UP,fingerX,fingerY);SystemClock.sleep(50);short[][] held=targets(s,a[0]);SystemClock.sleep(220);short[][] after=targets(s,a[0]);Assert.assertArrayEquals(held[0],after[0]);Assert.assertArrayEquals(held[1],after[1]);
   int tx=held[0][0],ty=held[1][0];double before=mean(NativeInterface.modelSnapshot(),2,tx,ty);NativeInterface.setPlayerPosition(2,held[0],held[1]);for(int n=0;n<230;n++){NativeInterface.stepDots();if("1".equals(InstrumentationRegistry.getArguments().getString("record_demo","0")))SystemClock.sleep(7);}double distance=mean(NativeInterface.modelSnapshot(),2,tx,ty);Assert.assertTrue("After finger lifts particles still chase fixed touch",distance<before);Assert.assertArrayEquals(held[0],targets(s,a[0])[0]);picture(c,"world169-joystick-follow.jpg");
   // Other corner takes over the same target. Cancel does not become model-idle.
   x=overlay.right-84*dp;y=overlay.bottom-66*dp;began=SystemClock.uptimeMillis();touch(began,MotionEvent.ACTION_DOWN,x,y);touch(began,MotionEvent.ACTION_MOVE,x-65*dp,y-15*dp);SystemClock.sleep(150);picture(c,"world169-joystick-right.jpg");touch(began,MotionEvent.ACTION_CANCEL,x-65*dp,y-15*dp);short[][] cancelled=targets(s,a[0]);Assert.assertTrue(cancelled[0][0]>=0);SystemClock.sleep(150);Assert.assertArrayEquals(cancelled[0],targets(s,a[0])[0]);
   // Direct map click remains the exact original coordinate transformation.
   float cx=map.left+390.5f*map.width()/800f,cy=map.top+239.5f*map.height()/480f;began=SystemClock.uptimeMillis();touch(began,MotionEvent.ACTION_DOWN,cx,cy);touch(began,MotionEvent.ACTION_UP,cx,cy);short[][] clicked=targets(s,a[0]);Assert.assertEquals(390,clicked[0][0]);Assert.assertEquals(240,clicked[1][0]);picture(c,"world169-joystick-click.jpg");
   int[] frozen=NativeInterface.itemSnapshot();s.moveToState(androidx.lifecycle.Lifecycle.State.CREATED);SystemClock.sleep(200);Assert.assertArrayEquals(frozen,NativeInterface.itemSnapshot());Assert.assertArrayEquals(clicked[0],targets(s,a[0])[0]);
   JSONObject report=new JSONObject().put("status","PASS").put("models",0).put("map",28).put("seed",16701).put("particles_per_team",1000).put("real_touch",true).put("controlled_native_steps",true).put("start",new JSONArray(start[0])).put("moved",new JSONArray(moved[0])).put("held_x",tx).put("held_y",ty).put("mean_distance_before",before).put("mean_distance_after",distance).put("knob_clamped_to_fixed_base",true).put("base_fixed",true).put("release_keeps_target",true).put("cancel_keeps_target",true).put("ordinary_click",true).put("paused_clock",true).put("human_play",false);
   try(Writer out=new OutputStreamWriter(c.openFileOutput("world169-joystick-report.json",0),"UTF-8")){out.write(report.toString(2));}
  }
 }
 @Test public void liveWorkerUsesSameTouchPoint()throws Exception{
  Context c=InstrumentationRegistry.getInstrumentation().getTargetContext();StaticBits.init();StaticBits.map=28;StaticBits.seed=16701;StaticBits.team=2;StaticBits.dotsPerTeam=1000;StaticBits.timeLimit=120;StaticBits.alliances=AllianceRules.solo();Intent intent=new Intent(c,GameActivity.class).putExtra("qa_model_teams",0).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
  try(ActivityScenario<GameActivity> s=ActivityScenario.launch(intent)){
   GameActivity[] a={null};s.onActivity(v->a[0]=v);Assert.assertEquals(0,((ModelMatch)get(get(a[0],"models"),"match")).configuredMask());SystemClock.sleep(MatchIntro.DURATION_MS+250);
   Assert.assertTrue("Normal worker running after countdown",NativeInterface.playTimeMillis()>0);Rect b=new Rect();s.onActivity(v->v.findViewById(R.id.item_overlay).getGlobalVisibleRect(b));float dp=c.getResources().getDisplayMetrics().density;
   android.os.ParcelFileDescriptor record=InstrumentationRegistry.getInstrumentation().getUiAutomation().executeShellCommand("screenrecord --time-limit 10 --size 1920x872 --bit-rate 5000000 /sdcard/Download/world169-joystick-live.mp4");
   float x=b.left+84*dp,y=b.bottom-66*dp;long t=SystemClock.uptimeMillis();touch(t,MotionEvent.ACTION_DOWN,x,y);touch(t,MotionEvent.ACTION_MOVE,x+70*dp,y-10*dp);SystemClock.sleep(600);picture(c,"world169-joystick-live-drag.jpg");touch(t,MotionEvent.ACTION_UP,x+70*dp,y-10*dp);short[][] held=targets(s,a[0]);SystemClock.sleep(500);short[] applied=NativeInterface.initialPositions();Assert.assertEquals(held[0][0],applied[StaticBits.team*2]);Assert.assertEquals(held[1][0],applied[StaticBits.team*2+1]);Assert.assertArrayEquals(held[0],targets(s,a[0])[0]);Assert.assertTrue(NativeInterface.playTimeMillis()>=500);picture(c,"world169-joystick-live-follow.jpg");
   try(InputStream in=new android.os.ParcelFileDescriptor.AutoCloseInputStream(record)){byte[] bytes=new byte[1024];while(in.read(bytes)>=0){}}
   JSONObject result=new JSONObject().put("status","PASS").put("normal_worker",true).put("controlled_native_steps",false).put("models",0).put("native_touch_x",applied[StaticBits.team*2]).put("native_touch_y",applied[StaticBits.team*2+1]).put("release_keeps_target",true).put("recording_seconds_requested",10).put("human_play",false);try(Writer out=new OutputStreamWriter(c.openFileOutput("world169-joystick-live-report.json",0),"UTF-8")){out.write(result.toString(2));}
  }
 }

}
