// SPDX-License-Identifier: GPL-3.0-or-later
package com.xenris.liquidwarsos;
import static com.xenris.liquidwarsos.World170ProbeTest.*;
import static com.xenris.liquidwarsos.Operations180ProbeTest.*;
import android.graphics.Rect;import android.os.*;import androidx.test.core.app.ActivityScenario;import androidx.test.ext.junit.runners.AndroidJUnit4;import org.junit.*;import org.junit.runner.RunWith;import org.json.*;import com.xenris.liquidwarsos.activities.GameActivity;
@RunWith(AndroidJUnit4.class) public class Operations180RegressionTest {
 @Test public void legacyFiveFingersAndCamera()throws Exception{
  StaticBits.operationsMode=0;
  try(ActivityScenario<GameActivity> s=ActivityScenario.launch(prepare(54,18054,400,true,false))){
   frozen(s);Rect gl=new Rect(),hud=new Rect();ready(s,gl,hud);button(s,hud,0);button(s,hud,0); // explicit FOLLOW
   int[] ids={0,3,7,11,19};float[] xx={gl.centerX(),gl.left+gl.width()*.35f,gl.left+gl.width()*.43f,gl.left+gl.width()*.6f,gl.left+gl.width()*.7f};float[] yy={gl.centerY(),gl.centerY()+20,gl.centerY()-20,gl.centerY()+40,gl.centerY()-40};long down=SystemClock.uptimeMillis();
   for(int k=0;k<5;k++)Controls173ProbeTest.fingers(down,k==0?0:5|(k<<8),k+1,ids,xx,yy);SystemClock.sleep(180);
   s.onActivity(a->{try{short[][] xs=(short[][])get(a,"xs"),ys=(short[][])get(a,"ys");MyGLSurfaceView v=a.findViewById(R.id.mySurfaceView);for(int k=0;k<5;k++){double error=Math.hypot(gl.left+v.camera.screenX(xs[StaticBits.team][k],gl.width())-xx[k],gl.top+v.camera.screenY(ys[StaticBits.team][k],gl.height())-yy[k]);Assert.assertTrue("five fingers retain distinct world coordinates",error<3.2);}}catch(Exception e){throw new AssertionError(e);}});
   Controls173ProbeTest.fingers(down,3,5,ids,xx,yy);s.onActivity(a->{Assert.assertFalse(((MyGLSurfaceView)a.findViewById(R.id.mySurfaceView)).camera.touching());});
   short[][] before=World170ProbeTest.targets(s);s.onActivity(a->{MyGLSurfaceView v=a.findViewById(R.id.mySurfaceView);v.camera.overview=true;});SystemClock.sleep(120);Assert.assertArrayEquals("camera view does not issue a movement command",before[0],World170ProbeTest.targets(s)[0]);picture("ops180-legacy.jpg");write("ops180-legacy.json",new JSONObject().put("status","PASS").put("five_fingers",true).put("explicit_follow_mode",true).put("camera_preserves_orders",true));
  }
 }
 @Test public void highPopulationNormalWorker()throws Exception{
  android.content.Intent intent=prepare(54,18055,4500,false,false);StaticBits.operationsMode=1;
  try(ActivityScenario<GameActivity> s=ActivityScenario.launch(intent)){
   Rect gl=new Rect(),hud=new Rect();ready(s,gl,hud);long deadline=SystemClock.uptimeMillis()+15000;while(NativeInterface.playTimeMillis()==0&&SystemClock.uptimeMillis()<deadline)SystemClock.sleep(50);Assert.assertTrue(NativeInterface.playTimeMillis()>0);
   s.onActivity(a->{MyGLSurfaceView v=a.findViewById(R.id.mySurfaceView);v.camera.overview=true;v.camera.focus(800,480);});SystemClock.sleep(150);button(s,hud,5);SystemClock.sleep(250);Assert.assertTrue(ops().split(StaticBits.team));
   for(int group=0;group<3;group++){button(s,hud,group+2);OperationsSnapshot data=ops();int at=data.site(group+1);float[] xy=screen(s,gl,data.raw[at],data.raw[at+1]);tap(xy[0],xy[1]);SystemClock.sleep(200);}
   long[] frames={0};s.onActivity(a->frames[0]=((MyGLSurfaceView)a.findViewById(R.id.mySurfaceView)).framesDrawn());int sim=NativeInterface.playTimeMillis(),particles=NativeInterface.totalParticles();long started=SystemClock.elapsedRealtime();SystemClock.sleep(12000);long wall=SystemClock.elapsedRealtime()-started;long[] count={0};s.onActivity(a->count[0]=((MyGLSurfaceView)a.findViewById(R.id.mySurfaceView)).framesDrawn()-frames[0]);int advanced=NativeInterface.playTimeMillis()-sim;Assert.assertTrue("large battle simulation progresses normally",advanced>4000);Assert.assertTrue("renderer continues drawing",count[0]>60);Debug.MemoryInfo memory=new Debug.MemoryInfo();Debug.getMemoryInfo(memory);picture("ops180-load.jpg");write("ops180-load.json",new JSONObject().put("status","PASS").put("normal_worker",true).put("initial_per_team",4500).put("particles_at_sample",particles).put("particles_after",NativeInterface.totalParticles()).put("wall_ms",wall).put("sim_ms",advanced).put("drawn_frames",count[0]).put("mean_fps",1000.0*count[0]/wall).put("pss_kib",memory.getTotalPss()).put("human_play",false).put("physical_phone",false));
  }finally{StaticBits.operationsMode=0;}
 }
}
