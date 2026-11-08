// SPDX-License-Identifier: GPL-3.0-or-later
package com.xenris.liquidwarsos;
import static com.xenris.liquidwarsos.World170ProbeTest.*;
import static com.xenris.liquidwarsos.Large171ProbeTest.*;
import static com.xenris.liquidwarsos.Controls173ProbeTest.fingers;
import android.content.*;import android.graphics.*;import android.os.*;import android.view.*;
import androidx.test.core.app.ActivityScenario;import androidx.test.ext.junit.runners.AndroidJUnit4;
import com.xenris.liquidwarsos.activities.*;import com.xenris.liquidwarsos.cloud.*;import org.junit.*;import org.junit.runner.RunWith;import org.json.*;
@RunWith(AndroidJUnit4.class)
public class Controls175ProbeTest {
 @Before @After public void releaseAnyTouch(){
  // Exact virtual pointer set from our failed five-finger probe; do not touch physical input.
  for(int count:new int[]{5,1}){int[] ids={0,3,7,11,19};MotionEvent.PointerProperties[] p=new MotionEvent.PointerProperties[count];MotionEvent.PointerCoords[] c=new MotionEvent.PointerCoords[count];for(int k=0;k<count;k++){p[k]=new MotionEvent.PointerProperties();p[k].id=ids[k];p[k].toolType=MotionEvent.TOOL_TYPE_FINGER;c[k]=new MotionEvent.PointerCoords();c[k].x=400;c[k].y=300;c[k].pressure=1;}
   long now=SystemClock.uptimeMillis();MotionEvent e=MotionEvent.obtain(now,now,MotionEvent.ACTION_CANCEL,count,p,c,0,0,1,1,-1,0,InputDevice.SOURCE_TOUCHSCREEN,0);androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().getUiAutomation().injectInputEvent(e,true);e.recycle();}
 }
 @Test public void joystickWaitsForRealParticles()throws Exception{
  JSONArray records=new JSONArray();
  for(int map:new int[]{54,1})try(ActivityScenario<GameActivity> scene=ActivityScenario.launch(prepare(map,17354,400,true,false))){
   frozen(scene);idle();MyGLSurfaceView[] gl={null};ItemOverlay[] h={null};Rect hb=new Rect();
   scene.onActivity(a->{gl[0]=a.findViewById(R.id.mySurfaceView);h[0]=a.findViewById(R.id.item_overlay);h[0].getGlobalVisibleRect(hb);h[0].resetControl();});readyGL(gl[0],map==54?4:0);
   int[] initial=NativeInterface.itemSnapshot();int at=147+StaticBits.team*10,mx=initial[at+2],my=initial[at+3],ww=gl[0].camera.width,hh=gl[0].camera.height;byte[] floor=NativeInterface.modelTerrain();
   int[] dx={1,-1,0,0},dy={0,0,1,-1};int direction=0,best=-1;
   for(int d=0;d<4;d++){int clear=0;for(int step=1;step<=240;step++){int x=mx+dx[d]*step,y=my+dy[d]*step;if(x<1||x>=ww-1||y<1||y>=hh-1||floor[y*ww+x]!=0)break;clear++;}if(clear>best){best=clear;direction=d;}}
   float dp=context().getResources().getDisplayMetrics().density;StickLayout l=new StickLayout(hb.width(),hb.height(),dp);float px=hb.left+l.leftX,py=hb.top+l.y;long down=SystemClock.uptimeMillis();
   touch(down,0,px,py);touch(down,2,px+dx[direction]*l.travel,py-dy[direction]*l.travel);
   double maxLead=0;int firstX=-1,firstY=-1;JSONArray trace=new JSONArray();long begin=SystemClock.uptimeMillis();int simBegin=initial[0];boolean pauseStable=false;
   for(int step=0;step<840;step++){
    if(step%4==0){short[][] xy=targets(scene);if(xy[0][0]>=0){NativeInterface.setPlayerPosition(StaticBits.team,xy[0],xy[1]);if(firstX<0){firstX=xy[0][0];firstY=xy[1][0];}}}
    NativeInterface.stepDots();SystemClock.sleep(7);
    if(step%20==0){int[] snap=NativeInterface.itemSnapshot();short[][] xy=targets(scene);double lead=dx[direction]*(xy[0][0]-snap[at+2])+dy[direction]*(xy[1][0]-snap[at+3]);maxLead=Math.max(maxLead,lead);
     if(dx[direction]!=0)Assert.assertEquals("horizontal no y drift",firstY,xy[1][0]);else Assert.assertEquals("vertical no x drift",firstX,xy[0][0]);
     trace.put(new JSONArray().put(snap[0]).put(snap[at+2]).put(snap[at+3]).put(xy[0][0]).put(xy[1][0]).put(snap[at+9]));
    }
    if(step==279){SystemClock.sleep(180);short[][] stop=targets(scene);SystemClock.sleep(220);Assert.assertArrayEquals("paused native time holds target",stop[0],targets(scene)[0]);Assert.assertArrayEquals(stop[1],targets(scene)[1]);pauseStable=true;picture("controls175-"+map+"-paused-held.jpg");}
   }
   picture("controls175-"+map+"-paced.jpg");touch(down,1,px+dx[direction]*l.travel,py-dy[direction]*l.travel);short[][] released=targets(scene);SystemClock.sleep(130);Assert.assertArrayEquals("release retains point",released[0],targets(scene)[0]);
   int[] last=NativeInterface.itemSnapshot();int movement=dx[direction]*(last[at+2]-mx)+dy[direction]*(last[at+3]-my);Assert.assertTrue("real particles moved "+movement,movement>3);Assert.assertTrue("no ever-growing target lead "+maxLead,maxLead<=43);
   long hand=SystemClock.uptimeMillis();touch(hand,0,hb.left+l.rightX,py);SystemClock.sleep(80);Assert.assertArrayEquals("switch hand no jump",released[0],targets(scene)[0]);touch(hand,1,hb.left+l.rightX,py);
   records.put(new JSONObject().put("map",map).put("initial_mass_x",mx).put("initial_mass_y",my).put("direction",new JSONArray().put(dx[direction]).put(dy[direction])).put("initial_clear_line_cells",best).put("simulated_ms",last[0]-simBegin).put("wall_ms",SystemClock.uptimeMillis()-begin).put("army_forward_cells",movement).put("max_forward_lead_cells",maxLead).put("paused_time_holds",pauseStable).put("trace",trace));
  }
  write("controls175-paced.json",new JSONObject().put("status","PASS").put("real_touch",true).put("real_core_7ms_steps",true).put("controlled_steps_not_normal_worker",true).put("maps",records).put("models",0));
 }
 @Test public void normalWorkerPacesTarget()throws Exception{
  try(ActivityScenario<GameActivity> scene=ActivityScenario.launch(prepare(54,17554,400,false,false))){
   MyGLSurfaceView[] gl={null};ItemOverlay[] h={null};Rect hb=new Rect();scene.onActivity(a->{gl[0]=a.findViewById(R.id.mySurfaceView);h[0]=a.findViewById(R.id.item_overlay);h[0].getGlobalVisibleRect(hb);h[0].resetControl();});readyGL(gl[0],4);
   long started=SystemClock.uptimeMillis()+10000;while(NativeInterface.playTimeMillis()==0&&SystemClock.uptimeMillis()<started)SystemClock.sleep(25);Assert.assertTrue("opening countdown completed naturally",NativeInterface.playTimeMillis()>0);
   int[] before=NativeInterface.itemSnapshot();int at=147+StaticBits.team*10,sign=before[at+2]<800?1:-1;float dp=context().getResources().getDisplayMetrics().density;StickLayout l=new StickLayout(hb.width(),hb.height(),dp);
   long down=SystemClock.uptimeMillis();touch(down,0,hb.left+l.leftX,hb.top+l.y);touch(down,2,hb.left+l.leftX+sign*l.travel,hb.top+l.y);
   JSONArray trace=new JSONArray();double maxLead=0;
   for(int k=0;k<30;k++){SystemClock.sleep(100);int[] snap=NativeInterface.itemSnapshot();short[][] xy=targets(scene);double lead=sign*(xy[0][0]-snap[at+2]);maxLead=Math.max(maxLead,lead);trace.put(new JSONArray().put(snap[0]).put(snap[at+2]).put(snap[at+3]).put(xy[0][0]).put(xy[1][0]).put(snap[at+9]));}
   picture("controls175-normal-worker.jpg");touch(down,1,hb.left+l.leftX+sign*l.travel,hb.top+l.y);int advanced=NativeInterface.itemSnapshot()[0]-before[0];Assert.assertTrue("normal worker advanced",advanced>500);Assert.assertTrue("normal target stays near army "+maxLead,maxLead<=48);
   write("controls175-normal-worker.json",new JSONObject().put("status","PASS").put("normal_worker",true).put("no_manual_native_steps",true).put("simulated_ms",advanced).put("max_forward_lead_cells",maxLead).put("trace",trace).put("models",0).put("not_fps_measurement",true));
  }
 }
 @Test public void dragStaysUnderFingersThenFollowsOnRelease()throws Exception{
  try(ActivityScenario<GameActivity> scene=ActivityScenario.launch(prepare(54,17354,400,true,false))){
   frozen(scene);MyGLSurfaceView[] gl={null};ItemOverlay[] h={null};Rect b=new Rect(),hb=new Rect();scene.onActivity(a->{gl[0]=a.findViewById(R.id.mySurfaceView);h[0]=a.findViewById(R.id.item_overlay);gl[0].getGlobalVisibleRect(b);h[0].getGlobalVisibleRect(hb);gl[0].camera.focus(800,480);});readyGL(gl[0],4);
   long hash=NativeInterface.stateHash();float before=gl[0].camera.left,bottom=gl[0].camera.bottom;int[] ids={0,3,7,11,19};float[] xx={b.left+b.width()*.68f,b.left+b.width()*.35f,b.left+b.width()*.5f,b.left+b.width()*.45f,b.left+b.width()*.6f},yy={b.top+b.height()*.45f,b.top+b.height()*.55f,b.top+b.height()*.6f,b.top+b.height()*.35f,b.top+b.height()*.4f};long down=SystemClock.uptimeMillis();
   for(int k=0;k<5;k++){fingers(down,k==0?0:5|(k<<8),k+1,ids,xx,yy);SystemClock.sleep(60);}SystemClock.sleep(450);Assert.assertEquals(before,gl[0].camera.left,.01f);
   xx[0]+=b.width()*.10f;yy[0]-=b.height()*.06f;fingers(down,2,5,ids,xx,yy);SystemClock.sleep(100);short[][] targets=targets(scene);double maxError=0;
   for(int k=0;k<5;k++){float sx=b.left+gl[0].camera.screenX(targets[0][k],b.width()),sy=b.top+gl[0].camera.screenY(targets[1][k],b.height());maxError=Math.max(maxError,Math.hypot(sx-xx[k],sy-yy[k]));}
   double quantizationBound=.51*Math.hypot(b.width()/gl[0].camera.viewWidth,b.height()/gl[0].camera.viewHeight);Assert.assertTrue("finger-to-cell centre error "+maxError+" bound "+quantizationBound,maxError<=quantizationBound);Assert.assertEquals(before,gl[0].camera.left,.01f);Assert.assertEquals(bottom,gl[0].camera.bottom,.01f);picture("controls175-drag-five-precise.jpg");
   for(int count=5;count>=2;count--)fingers(down,6|((count-1)<<8),count,ids,xx,yy);fingers(down,1,1,ids,xx,yy);SystemClock.sleep(650);Assert.assertTrue(gl[0].camera.left>before+100);Assert.assertEquals(targets[0][0],targets(scene)[0][0]);Assert.assertFalse(gl[0].camera.touching());picture("controls175-release-follow.jpg");
   miniTap(h[0],hb,1250,700,false);SystemClock.sleep(150);Assert.assertFalse(gl[0].camera.following);float pan=gl[0].camera.left;SystemClock.sleep(150);Assert.assertEquals(pan,gl[0].camera.left,.01f);Assert.assertEquals(targets[0][0],targets(scene)[0][0]);
   tap(b.left+b.width()*.45f,b.top+b.height()*.5f);SystemClock.sleep(150);Assert.assertTrue(gl[0].camera.following);
   long cancel=SystemClock.uptimeMillis();touch(cancel,0,b.left+b.width()*.5f,b.top+b.height()*.5f);scene.onActivity(a->{gl[0].onWindowFocusChanged(false);h[0].onWindowFocusChanged(false);});Assert.assertFalse(gl[0].camera.touching());touch(cancel,3,b.left+b.width()*.5f,b.top+b.height()*.5f);scene.onActivity(a->{gl[0].onWindowFocusChanged(true);h[0].onWindowFocusChanged(true);});Assert.assertEquals(hash,NativeInterface.stateHash());
   write("controls175-drag.json",new JSONObject().put("status","PASS").put("pointer_ids",new JSONArray(ids)).put("max_target_error_screen_px",maxError).put("half_cell_diagonal_bound_px",quantizationBound).put("held_camera_stable",true).put("release_follows",true).put("minimap_no_command",true).put("focus_clears_gesture",true).put("rule_hash_unchanged",true).put("models",0));
  }
 }
 @Test public void healthAndDirectMinimapTeleport()throws Exception{
  Intent intent=prepare(1,101,100,true,false);StaticBits.team=1;
  try(ActivityScenario<GameActivity> scene=ActivityScenario.launch(intent)){
   frozen(scene);MyGLSurfaceView[] gl={null};ItemOverlay[] h={null};Rect bounds=new Rect();scene.onActivity(a->{gl[0]=a.findViewById(R.id.mySurfaceView);h[0]=a.findViewById(R.id.item_overlay);h[0].getGlobalVisibleRect(bounds);});readyGL(gl[0],0);idle();
   for(int n=0;n<2000&&NativeInterface.itemSnapshot()[2]==0;n++)NativeInterface.stepDots();int[] wave=NativeInterface.itemSnapshot();int slot=-1;for(int k=0;k<SnapshotItems.slots(wave);k++)if(wave[SnapshotItems.offset(wave,k)]==3)slot=k;Assert.assertTrue("natural teleport fixture",slot>=0);int at=SnapshotItems.offset(wave,slot),x=wave[at+1],y=wave[at+2];short[] tx=IDLE.clone(),ty=IDLE.clone();tx[0]=(short)x;ty[0]=(short)y;NativeInterface.setPlayerPosition(1,tx,ty);
   int[] contact=null;for(int n=0;n<1400;n++){NativeInterface.stepDots();int[] now=NativeInterface.itemSnapshot();if(SnapshotItems.hp(now,slot)<1200){contact=now;break;}}
   Assert.assertNotNull("contact reached",contact);Assert.assertEquals(3,contact[SnapshotItems.offset(contact,slot)]);Assert.assertEquals(0,contact[157]);Assert.assertTrue(SnapshotItems.hp(contact,slot)>=1196);SystemClock.sleep(150);picture("controls175-item-first-contact.jpg");
   for(int n=0;n<150;n++)NativeInterface.stepDots();int[] middle=NativeInterface.itemSnapshot();Assert.assertEquals(3,middle[SnapshotItems.offset(middle,slot)]);Assert.assertTrue(SnapshotItems.hp(middle,slot)>0&&SnapshotItems.hp(middle,slot)<1200);SystemClock.sleep(160);picture("controls175-item-half-hp.jpg");int[] held=NativeInterface.itemSnapshot();SystemClock.sleep(180);Assert.assertArrayEquals("paused HP does not tick",held,NativeInterface.itemSnapshot());
   for(int n=0;n<1500&&NativeInterface.itemSnapshot()[157]!=3;n++)NativeInterface.stepDots();Assert.assertEquals(3,NativeInterface.itemSnapshot()[157]);NativeInterface.setPlayerPosition(1,IDLE,IDLE);SystemClock.sleep(180);picture("controls175-minimap-teleport-ready.jpg");Assert.assertFalse((Boolean)get(h[0],"aiming"));
   byte[] wall=NativeInterface.modelTerrain();int wx=-1,wy=-1,fx=-1,fy=-1;for(int yy=20;yy<460;yy+=8)for(int xx=25;xx<775;xx+=8){boolean rock=true,floor=true;for(int dy=-3;dy<=3;dy++)for(int dx=-3;dx<=3;dx++){rock&=wall[(yy+dy)*800+xx+dx]!=0;floor&=wall[(yy+dy)*800+xx+dx]==0;}if(rock&&wx<0){wx=xx;wy=yy;}if(floor&&xx>450&&fx<0){fx=xx;fy=yy;}}Assert.assertTrue(wx>=0&&fx>=0);
   miniTap(h[0],bounds,wx,wy,true);apply(scene);NativeInterface.stepDots();Assert.assertEquals("wall keeps teleport",3,NativeInterface.itemSnapshot()[157]);SystemClock.sleep(180);picture("controls175-minimap-wall-rejected.jpg");
   int armyBefore=NativeInterface.itemSnapshot()[14];miniTap(h[0],bounds,fx,fy,true);short[][] selected=targets(scene);Assert.assertEquals(-3,selected[0][4]);Assert.assertEquals(fx,selected[0][3]);Assert.assertEquals(fy,selected[1][3]);apply(scene);NativeInterface.stepDots();int[] done=NativeInterface.itemSnapshot();Assert.assertEquals(0,done[157]);Assert.assertEquals(armyBefore,done[11]);NativeInterface.setPlayerPosition(1,IDLE,IDLE);SystemClock.sleep(180);picture("controls175-minimap-teleported.jpg");Assert.assertTrue(((Integer)get(h[0],"lastCameraTeleport"))>0);
   long before=gl[0].framesDrawn();scene.onActivity(a->{gl[0].onPause();gl[0].onResume();});long deadline=SystemClock.uptimeMillis()+5000;while(gl[0].framesDrawn()<=before&&SystemClock.uptimeMillis()<deadline)SystemClock.sleep(30);Assert.assertTrue(gl[0].framesDrawn()>before);Assert.assertArrayEquals(done,NativeInterface.itemSnapshot());
   write("controls175-health-teleport.json",new JSONObject().put("status","PASS").put("natural_pickup",true).put("first_contact_hp",SnapshotItems.hp(contact,slot)).put("middle_hp",SnapshotItems.hp(middle,slot)).put("paused_hp_stable",true).put("no_bottom_card_required",true).put("wall_rejected_retained",true).put("selected_xy",new JSONArray().put(fx).put(fy)).put("teleported",armyBefore).put("egl_stable",true).put("models",0).put("controlled_native_steps",true));
  }
 }

}
