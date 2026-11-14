// SPDX-License-Identifier: GPL-3.0-or-later
package com.xenris.liquidwarsos;
import static com.xenris.liquidwarsos.World170ProbeTest.*;
import static com.xenris.liquidwarsos.Operations180ProbeTest.*;
import android.graphics.Rect;import android.os.*;import android.view.*;import androidx.lifecycle.Lifecycle;import androidx.test.core.app.ActivityScenario;import androidx.test.ext.junit.runners.AndroidJUnit4;import org.junit.*;import org.junit.runner.RunWith;import org.json.*;import com.xenris.liquidwarsos.activities.GameActivity;import com.xenris.liquidwarsos.cloud.SnapshotItems;
@RunWith(AndroidJUnit4.class) public class Follow182ProbeTest {
 // Always close injected gesture streams, including assertion/lifecycle failures.
 @Before @After public void releaseInjectedFinger(){long now=SystemClock.uptimeMillis();MotionEvent e=MotionEvent.obtain(now,now,MotionEvent.ACTION_CANCEL,0,0,0);e.setSource(InputDevice.SOURCE_TOUCHSCREEN);androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().getUiAutomation().injectInputEvent(e,true);e.recycle();}
 static final short[] IDLE={-2,-1,-1,-1,-1};
 static int[] end(int group){OperationsSnapshot o=ops();int a=o.group(StaticBits.team,group);Assert.assertEquals(1,o.raw[a+7]);return new int[]{o.raw[a+8],o.raw[a+9]};}
 @Test public void defaultHoldDragAndSquadOwnership()throws Exception {
  StaticBits.operationsMode=0;try(ActivityScenario<GameActivity> s=ActivityScenario.launch(prepare(1,18201,100,true,false))){
   frozen(s);Rect gl=new Rect(),hud=new Rect();ready(s,gl,hud);for(int t=0;t<6;t++)NativeInterface.setPlayerPosition(t,IDLE,IDLE);
   // No mode button: the user's default press must issue a live target before UP.
   float px=gl.centerX(),py=gl.centerY();long down=SystemClock.uptimeMillis();touch(down,0,px,py);SystemClock.sleep(240);advance(s,20);int[] first=end(0);Assert.assertEquals(OrderProtocol.FOLLOW,targets(s)[1][0]/4);
   touch(down,2,px+120,py-30);SystemClock.sleep(90);advance(s,20);int[] moved=end(0);Assert.assertTrue("world target moves while finger remains down",moved[0]>first[0]);picture("follow182-held.jpg");
   OperationsSnapshot before=ops();int a=before.group(StaticBits.team,0);int oldX=before.raw[a+1],oldY=before.raw[a+2];advance(s,100);OperationsSnapshot after=ops();a=after.group(StaticBits.team,0);Assert.assertTrue("real particles move before UP",Math.hypot(after.raw[a+1]-oldX,after.raw[a+2]-oldY)>2);
   touch(down,1,px+120,py-30);int[] release=end(0);advance(s,40);Assert.assertArrayEquals("release retains destination",release,end(0));
   button(s,hud,5);advance(s,20);button(s,hud,3); // B only
   down=SystemClock.uptimeMillis();touch(down,0,px,py);touch(down,2,px-100,py+25);SystemClock.sleep(90);advance(s,20);after=ops();Assert.assertEquals(1,after.raw[after.group(StaticBits.team,1)+3]);Assert.assertEquals(1,after.raw[after.group(StaticBits.team,0)+4]);Assert.assertEquals(1,after.raw[after.group(StaticBits.team,2)+4]);touch(down,1,px-100,py+25);picture("follow182-squad.jpg");
   button(s,hud,7);advance(s,20);Assert.assertEquals(1,ops().raw[ops().group(StaticBits.team,1)+4]);
   down=SystemClock.uptimeMillis();touch(down,0,px,py);touch(down,3,px,py);SystemClock.sleep(250);advance(s,20);Assert.assertEquals("cancelled pending hold never fires",1,ops().raw[ops().group(StaticBits.team,1)+4]);
   down=SystemClock.uptimeMillis();touch(down,0,px,py);SystemClock.sleep(220);advance(s,20);s.moveToState(Lifecycle.State.CREATED);s.moveToState(Lifecycle.State.RESUMED);frozen(s);int[] kept=end(1);SystemClock.sleep(250);advance(s,20);Assert.assertArrayEquals("resume does not restart stale touch",kept,end(1));
   write("follow182-default.json",new JSONObject().put("status","PASS").put("no_mode_switch",true).put("target_changed_before_up",true).put("actual_particles_moved_before_up",true).put("release_keeps_target",true).put("selected_B_only",true).put("stop_cancel_resume",true).put("controlled_core_steps",true));
  }
 }
 @Test public void heldTargetTracksMovingLargeCamera()throws Exception {
  StaticBits.operationsMode=0;try(ActivityScenario<GameActivity> s=ActivityScenario.launch(prepare(54,18254,100,true,false))){
   frozen(s);Rect gl=new Rect(),hud=new Rect();ready(s,gl,hud);float px=gl.centerX()+gl.width()*.17f,py=gl.centerY();long down=SystemClock.uptimeMillis();touch(down,0,px,py);SystemClock.sleep(230);advance(s,20);
   s.onActivity(a->{MyGLSurfaceView v=a.findViewById(R.id.mySurfaceView);v.camera.focus(Math.min(v.camera.width-450,v.camera.left+v.camera.viewWidth/2+160),v.camera.bottom+v.camera.viewHeight/2);});SystemClock.sleep(130);advance(s,10);
   int[] target=end(0);double[] error={99},budget={0};long[] age={0};s.onActivity(a->{try{MyGLSurfaceView v=a.findViewById(R.id.mySurfaceView);TacticalOverlay overlay=a.findViewById(R.id.tactical_overlay);age[0]=SystemClock.uptimeMillis()-((Number)get(overlay,"sentAt")).longValue();error[0]=Math.hypot(gl.left+v.camera.screenX(target[0],gl.width())-px,gl.top+v.camera.screenY(target[1],gl.height())-py);double quantization=Math.hypot(gl.width()/v.camera.viewWidth,gl.height()/v.camera.viewHeight)/2;budget[0]=quantization+gl.width()*.22*age[0]/1000+2;}catch(Exception e){throw new AssertionError(e);}});touch(down,1,px,py);Assert.assertTrue("bounded50ms updates including integer cell quantization: "+error[0]+" budget="+budget[0],age[0]<100&&error[0]<=budget[0]);picture("follow182-large.jpg");
   write("follow182-large.json",new JSONObject().put("status","PASS").put("projection_error_pixels",error[0]).put("last_target_age_ms",age[0]).put("quantization_and_max_camera_motion_budget_px",budget[0]).put("map",54).put("camera_changed_while_held",true).put("physical_phone",false));
  }
 }
 @Test public void naturallyCollectedTenSecondItem()throws Exception {
  StaticBits.operationsMode=0;android.content.Intent intent=prepare(1,127,100,true,false);StaticBits.team=0;
  try(ActivityScenario<GameActivity> s=ActivityScenario.launch(intent)){
   frozen(s);for(int t=0;t<6;t++)NativeInterface.setPlayerPosition(t,IDLE,IDLE);for(int n=0;n<2000&&NativeInterface.itemSnapshot()[2]==0;n++)NativeInterface.stepDots();int[] wave=NativeInterface.itemSnapshot();int slot=-1;for(int k=0;k<SnapshotItems.slots(wave);k++)if(wave[SnapshotItems.offset(wave,k)]==6)slot=k;Assert.assertTrue(slot>=0);int at=SnapshotItems.offset(wave,slot);short[] x=IDLE.clone(),y=IDLE.clone();x[0]=(short)wave[at+1];y[0]=(short)wave[at+2];NativeInterface.setPlayerPosition(0,x,y);
   int[] got=null;for(int n=0;n<2000;n++){NativeInterface.stepDots();int[] data=NativeInterface.itemSnapshot();if(data[31]>0){got=data;break;}}Assert.assertNotNull("real devouring produces timed effect",got);Assert.assertEquals(10000,got[31]);NativeInterface.setPlayerPosition(0,IDLE,IDLE);SystemClock.sleep(180);picture("follow182-item-10s.jpg");int[] paused=NativeInterface.itemSnapshot();SystemClock.sleep(250);Assert.assertArrayEquals(paused,NativeInterface.itemSnapshot());
   for(int n=0;n<1428;n++)NativeInterface.stepDots();Assert.assertEquals(4,NativeInterface.itemSnapshot()[31]);NativeInterface.stepDots();Assert.assertEquals(0,NativeInterface.itemSnapshot()[31]);SystemClock.sleep(120);picture("follow182-item-expired.jpg");
   for(String description:context().getResources().getStringArray(R.array.item_descriptions))Assert.assertFalse("UI contains no obsolete20s duration",description.contains("20秒")||description.contains("20s"));
   write("follow182-timer.json",new JSONObject().put("status","PASS").put("natural_pickup",true).put("initial_ms",10000).put("remaining_at9996",4).put("expired_at10003",true).put("pause_stable",true).put("controlled_core_steps",true));
  }
 }
}
