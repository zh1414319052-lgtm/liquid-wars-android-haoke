// SPDX-License-Identifier: GPL-3.0-or-later
package com.xenris.liquidwarsos;
import static com.xenris.liquidwarsos.World170ProbeTest.*;
import static com.xenris.liquidwarsos.Large171ProbeTest.*;
import android.graphics.*;import android.os.*;import android.view.*;
import androidx.test.core.app.ActivityScenario;import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import com.xenris.liquidwarsos.activities.*;import org.junit.*;import org.junit.runner.RunWith;import org.json.*;

@RunWith(AndroidJUnit4.class) public class Comfort178ProbeTest {
 @Before @After public void release(){new Controls176ProbeTest().releaseAnyTouch();}
 @Test public void comparisonGesture()throws Exception {
  String label=InstrumentationRegistry.getArguments().getString("label","after");
  try(ActivityScenario<GameActivity> s=ActivityScenario.launch(prepare(54,17554,1000,true,false))){
   frozen(s);idle();MyGLSurfaceView[] gl={null};ItemOverlay[] hud={null};Rect b=new Rect();
   s.onActivity(a->{gl[0]=a.findViewById(R.id.mySurfaceView);hud[0]=a.findViewById(R.id.item_overlay);gl[0].getGlobalVisibleRect(b);});readyGL(gl[0],4);
   int at=147+StaticBits.team*10;int[] initial=NativeInterface.itemSnapshot();
   s.onActivity(a->{hud[0].resetControl();gl[0].camera.focus(initial[at+2],initial[at+3]);});SystemClock.sleep(180);
   float right=initial[at+2]<800?.85f:.15f,left=1-right,px=b.left+b.width()*right,py=b.centerY();long down=SystemClock.uptimeMillis();
   picture("controls178-"+label+"-start.jpg");touch(down,0,px,py);JSONArray trace=new JSONArray();double maxError=0;
   long started=SystemClock.uptimeMillis();
   for(int step=0;step<1400;step++){
    if(step==700){px=b.left+b.width()*left;touch(down,2,px,py);picture("controls178-"+label+"-forward.jpg");}
    if(step==1050){px=b.centerX();touch(down,2,px,py);picture("controls178-"+label+"-reverse.jpg");}
    if(step==1200){touch(down,1,px,py);picture("controls178-"+label+"-release.jpg");}
    if(step>=1050&&step<1200&&step%25==0)touch(down,2,px+(step%50==0?2:-2),py);
    if(step%4==0){short[][] xy=targets(s);NativeInterface.setPlayerPosition(StaticBits.team,xy[0],xy[1]);}
    NativeInterface.stepDots();SystemClock.sleep(7);
    if(step%20==0){int[] snap=NativeInterface.itemSnapshot();short[][] xy=targets(s);float[] cam=new float[4];s.onActivity(a->{cam[0]=gl[0].camera.left;cam[1]=gl[0].camera.bottom;cam[2]=gl[0].camera.viewWidth;cam[3]=gl[0].camera.viewHeight;});
     trace.put(new JSONArray().put(SystemClock.uptimeMillis()-started).put(snap[0]).put(cam[0]).put(cam[1]).put(snap[at+2]).put(snap[at+3]).put(xy[0][0]).put(xy[1][0]).put(snap[at+9]).put(step));
    }
   }
   short[][] finalTarget=targets(s);SystemClock.sleep(200);Assert.assertArrayEquals(finalTarget[0],targets(s)[0]);Assert.assertFalse(gl[0].camera.touching());picture("controls178-"+label+"-end.jpg");
   write("controls178-"+label+"-trace.json",new JSONObject().put("status","PASS").put("label",label).put("map",54).put("seed",17554).put("particles_per_team",1000).put("screen",b.toString()).put("start_mass",new JSONArray().put(initial[at+2]).put(initial[at+3])).put("primary_first_nx",right).put("trace_columns","wall_ms,sim_ms,left,bottom,army_x,army_y,target_x,target_y,army_count,step").put("trace",trace).put("controlled_core_steps",true).put("normal_worker",false).put("models",0).put("human_play",false));
  }
 }

 @Test public void inputTransitions()throws Exception {
  JSONArray rows=new JSONArray();
  for(int map:new int[]{1,54})try(ActivityScenario<GameActivity> s=ActivityScenario.launch(prepare(map,17554,400,true,false))){
   frozen(s);idle();MyGLSurfaceView[] gl={null};ItemOverlay[] h={null};Rect b=new Rect(),hb=new Rect();
   s.onActivity(a->{gl[0]=a.findViewById(R.id.mySurfaceView);h[0]=a.findViewById(R.id.item_overlay);gl[0].getGlobalVisibleRect(b);h[0].getGlobalVisibleRect(hb);h[0].resetControl();});readyGL(gl[0],map==54?4:0);long hash=NativeInterface.stateHash();
   int[] ids={0,3,7,11,19};float[] xx={b.left+b.width()*.52f,b.left+b.width()*.35f,b.left+b.width()*.43f,b.left+b.width()*.6f,b.left+b.width()*.7f},yy={b.centerY(),b.centerY()+20,b.centerY()-20,b.centerY()+40,b.centerY()-40};long down=SystemClock.uptimeMillis();
   for(int k=0;k<5;k++)Controls173ProbeTest.fingers(down,k==0?0:5|(k<<8),k+1,ids,xx,yy);SystemClock.sleep(220);
   double[] error={0};s.onActivity(a->{try{short[][] x=(short[][])get(a,"xs"),y=(short[][])get(a,"ys");for(int k=0;k<5;k++)error[0]=Math.max(error[0],Math.hypot(b.left+gl[0].camera.screenX(x[StaticBits.team][k],b.width())-xx[k],b.top+gl[0].camera.screenY(y[StaticBits.team][k],b.height())-yy[k]));}catch(Exception e){throw new RuntimeException(e);}});Assert.assertTrue("5 precise touch cells "+error[0],error[0]<3.2);
   Controls173ProbeTest.fingers(down,6,5,ids,xx,yy);int[] ids4={3,7,11,19};float[] x4={xx[1],xx[2],xx[3],xx[4]},y4={yy[1],yy[2],yy[3],yy[4]};Controls173ProbeTest.fingers(down,2,4,ids4,x4,y4);SystemClock.sleep(80);Assert.assertEquals(-1,targets(s)[0][4]);Assert.assertTrue(gl[0].camera.touching());Controls173ProbeTest.fingers(down,3,4,ids4,x4,y4);Assert.assertFalse(gl[0].camera.touching());short[][] released=targets(s);SystemClock.sleep(120);Assert.assertArrayEquals(released[0],targets(s)[0]);Assert.assertEquals(hash,NativeInterface.stateHash());
   if(map==54){miniTap(h[0],hb,1500,850,false);SystemClock.sleep(100);Assert.assertFalse(gl[0].camera.following);float left=gl[0].camera.left;SystemClock.sleep(180);Assert.assertEquals(left,gl[0].camera.left,.01f);Assert.assertArrayEquals(released[0],targets(s)[0]);float dp=context().getResources().getDisplayMetrics().density;tap(hb.left+210*dp,hb.top+25*dp);SystemClock.sleep(80);Assert.assertTrue(gl[0].camera.following);tap(hb.left+210*dp,hb.top+25*dp);Assert.assertTrue(gl[0].camera.overview);tap(hb.left+210*dp,hb.top+25*dp);Assert.assertFalse(gl[0].camera.overview);}
   long t=SystemClock.uptimeMillis();touch(t,0,b.centerX(),b.centerY());s.onActivity(a->{gl[0].onWindowFocusChanged(false);h[0].onWindowFocusChanged(false);});Assert.assertFalse(gl[0].camera.touching());touch(t,3,b.centerX(),b.centerY());s.onActivity(a->{gl[0].onWindowFocusChanged(true);h[0].onWindowFocusChanged(true);});long frames=gl[0].framesDrawn();s.moveToState(androidx.lifecycle.Lifecycle.State.CREATED);Assert.assertFalse(gl[0].pacingActive());s.moveToState(androidx.lifecycle.Lifecycle.State.RESUMED);frozen(s);readyGL(gl[0],map==54?4:0);Assert.assertTrue(gl[0].framesDrawn()>frames);Assert.assertFalse(gl[0].camera.touching());if(map==1)Assert.assertEquals(0,gl[0].camera.left,0);
   picture("controls178-transitions-"+map+".jpg");rows.put(new JSONObject().put("map",map).put("five_pointer_error_px",error[0]).put("primary_lift",true).put("release_retains",true).put("minimap_no_command",true).put("focus_cancel",true).put("egl_resume",true).put("ui_only_hash_stable",true));
  }
  write("controls178-transitions.json",new JSONObject().put("status","PASS").put("rows",rows));
 }
 @Test public void normalWorkerAndSpectator()throws Exception {
  JSONArray cases=new JSONArray();for(boolean spectate:new boolean[]{false,true})try(ActivityScenario<GameActivity> s=ActivityScenario.launch(prepare(54,17554,400,false,spectate))){
   MyGLSurfaceView[] gl={null};Rect b=new Rect();s.onActivity(a->{gl[0]=a.findViewById(R.id.mySurfaceView);gl[0].getGlobalVisibleRect(b);});readyGL(gl[0],4);long limit=SystemClock.uptimeMillis()+10000;while(NativeInterface.playTimeMillis()==0&&SystemClock.uptimeMillis()<limit)SystemClock.sleep(25);Assert.assertTrue(NativeInterface.playTimeMillis()>0);int start=NativeInterface.playTimeMillis();long down=SystemClock.uptimeMillis();float px=b.left+b.width()*.2f,py=b.centerY();touch(down,0,px,py);SystemClock.sleep(1800);touch(down,2,b.left+b.width()*.8f,py);SystemClock.sleep(700);touch(down,1,b.left+b.width()*.8f,py);SystemClock.sleep(180);int advanced=NativeInterface.playTimeMillis()-start;Assert.assertTrue("normal simulation progressed",advanced>500);Assert.assertFalse(gl[0].camera.touching());if(spectate)Assert.assertTrue(gl[0].camera.overview);picture("controls178-normal-"+spectate+".jpg");cases.put(new JSONObject().put("spectator",spectate).put("sim_ms",advanced).put("no_manual_steps",true).put("countdown",true));
  }write("controls178-normal.json",new JSONObject().put("status","PASS").put("cases",cases).put("models",0));
 }
 @Test public void naturalRewindCancelsGesture()throws Exception {
  try(ActivityScenario<GameActivity> s=ActivityScenario.launch(prepare(1,105,100,true,false))){
   frozen(s);MyGLSurfaceView[] gl={null};Rect b=new Rect();s.onActivity(a->{gl[0]=a.findViewById(R.id.mySurfaceView);gl[0].getGlobalVisibleRect(b);gl[0].onPause();});int seedFound=-1,teamFound=-1,px=0,py=0;
   for(int seed=100;seed<148&&seedFound<0;seed++){NativeInterface.destroyGame();NativeInterface.createGame(2,1,seed,100);NativeInterface.configureAlliances(AllianceRules.solo());idle();for(int k=0;k<2200&&NativeInterface.itemSnapshot()[2]==0;k++)NativeInterface.stepDots();int[] d=NativeInterface.itemSnapshot();byte[] wall=NativeInterface.modelTerrain();for(int k=0;k<com.xenris.liquidwarsos.cloud.SnapshotItems.slots(d)&&seedFound<0;k++){int at=com.xenris.liquidwarsos.cloud.SnapshotItems.offset(d,k);if(d[at]!=4)continue;for(int team=0;team<6;team++){int own=147+team*10,ax=d[own+2],ay=d[own+3],n=Math.max(Math.abs(d[at+1]-ax),Math.abs(d[at+2]-ay));if(n<1||n>200)continue;boolean free=true;for(int q=0;q<=n;q++){int xx=ax+(d[at+1]-ax)*q/n,yy=ay+(d[at+2]-ay)*q/n;if(wall[yy*800+xx]!=0){free=false;break;}}if(free){seedFound=seed;teamFound=team;px=d[at+1];py=d[at+2];break;}}}}
   Assert.assertTrue("natural rewind found within bounded seeds",seedFound>=0);StaticBits.team=teamFound;NativeInterface.destroyGame();NativeInterface.createGame(teamFound,1,seedFound,100);NativeInterface.configureAlliances(AllianceRules.solo());idle();for(int k=0;k<2200&&NativeInterface.itemSnapshot()[2]==0;k++)NativeInterface.stepDots();short[] x=IDLE.clone(),y=IDLE.clone();x[0]=(short)px;y[0]=(short)py;NativeInterface.setPlayerPosition(teamFound,x,y);s.onActivity(a->gl[0].onResume());readyGL(gl[0],0);long down=SystemClock.uptimeMillis();touch(down,0,b.centerX(),b.centerY());
   for(int k=0;k<2800&&NativeInterface.itemSnapshot()[146]==0;k++)NativeInterface.stepDots();Assert.assertEquals(1,NativeInterface.itemSnapshot()[146]);SystemClock.sleep(140);s.onActivity(a->{try{invoke(a,"syncRewind");}catch(Exception e){throw new RuntimeException(e);}});SystemClock.sleep(100);Assert.assertFalse(gl[0].camera.touching());touch(down,3,b.centerX(),b.centerY());picture("controls178-rewind.jpg");write("controls178-rewind.json",new JSONObject().put("status","PASS").put("natural_seed",seedFound).put("team",teamFound).put("epoch",1).put("held_gesture_cleared",true));
  }
 }

 @Test public void largeMapNaturalTeleport()throws Exception {
  try(ActivityScenario<GameActivity> s=ActivityScenario.launch(prepare(54,100,100,true,false))){
   frozen(s);MyGLSurfaceView[] gl={null};ItemOverlay[] hud={null};Rect hb=new Rect();s.onActivity(a->{gl[0]=a.findViewById(R.id.mySurfaceView);hud[0]=a.findViewById(R.id.item_overlay);hud[0].getGlobalVisibleRect(hb);gl[0].onPause();});
   int chosenSeed=-1,chosenTeam=-1,px=0,py=0;
   for(int seed=100;seed<148&&chosenSeed<0;seed++){
    NativeInterface.destroyGame();NativeInterface.createGame(2,54,seed,100);NativeInterface.configureAlliances(AllianceRules.solo());idle();for(int k=0;k<2000&&NativeInterface.itemSnapshot()[2]==0;k++)NativeInterface.stepDots();int[] d=NativeInterface.itemSnapshot();byte[] terrain=NativeInterface.modelTerrain();
    for(int slot=0;slot<com.xenris.liquidwarsos.cloud.SnapshotItems.slots(d)&&chosenSeed<0;slot++){int at=com.xenris.liquidwarsos.cloud.SnapshotItems.offset(d,slot);if(d[at]!=3)continue;int ix=d[at+1],iy=d[at+2];
     for(int team=0;team<6;team++){int own=147+team*10,ax=d[own+2],ay=d[own+3],steps=Math.max(Math.abs(ax-ix),Math.abs(ay-iy));if(steps>210||steps<1)continue;boolean clear=true;for(int q=0;q<=steps;q++){int x=ax+(ix-ax)*q/steps,y=ay+(iy-ay)*q/steps;if(terrain[y*1600+x]!=0){clear=false;break;}}if(clear){chosenSeed=seed;chosenTeam=team;px=ix;py=iy;break;}}
    }
   }
   Assert.assertTrue("bounded natural large-map fixture search",chosenSeed>=0);StaticBits.team=chosenTeam;NativeInterface.destroyGame();NativeInterface.createGame(chosenTeam,54,chosenSeed,100);NativeInterface.configureAlliances(AllianceRules.solo());idle();for(int k=0;k<2000&&NativeInterface.itemSnapshot()[2]==0;k++)NativeInterface.stepDots();short[] x=IDLE.clone(),y=IDLE.clone();x[0]=(short)px;y[0]=(short)py;NativeInterface.setPlayerPosition(chosenTeam,x,y);int own=147+chosenTeam*10;
   for(int k=0;k<3200&&NativeInterface.itemSnapshot()[own]!=3;k++)NativeInterface.stepDots();Assert.assertEquals("naturally devoured teleport",3,NativeInterface.itemSnapshot()[own]);idle();s.onActivity(a->{gl[0].onResume();hud[0].resetControl();});readyGL(gl[0],4);SystemClock.sleep(150);
   byte[] floor=NativeInterface.modelTerrain();int fx=-1,fy=-1;for(int yy=760;yy<900&&fx<0;yy+=8)for(int xx=1250;xx<1500&&fx<0;xx+=8){boolean free=true;for(int dy=-5;dy<=5;dy++)for(int dx=-5;dx<=5;dx++)free&=floor[(yy+dy)*1600+xx+dx]==0;if(free){fx=xx;fy=yy;}}
   Assert.assertTrue("open distant landing",fx>=0);s.onActivity(a->gl[0].camera.pan(400,240));SystemClock.sleep(150);picture("controls178-large-teleport-ready.jpg");miniTap(hud[0],hb,fx,fy,true);apply(s);NativeInterface.stepDots();SystemClock.sleep(250);int[] done=NativeInterface.itemSnapshot();Assert.assertEquals(0,done[own]);Assert.assertTrue(gl[0].camera.following);Assert.assertFalse(gl[0].camera.touching());Assert.assertTrue("landing army visible",done[own+2]>=gl[0].camera.left&&done[own+2]<gl[0].camera.left+gl[0].camera.viewWidth&&done[own+3]>=gl[0].camera.bottom&&done[own+3]<gl[0].camera.bottom+gl[0].camera.viewHeight);picture("controls178-large-teleport-landed.jpg");write("controls178-large-teleport.json",new JSONObject().put("status","PASS").put("map",54).put("seed",chosenSeed).put("team",chosenTeam).put("natural_pickup",true).put("direct_minimap_tap",true).put("landing_visible",true).put("selected_x",fx).put("selected_y",fy));
  }
 }

 @Test public void movingTouchPrecision()throws Exception {
  try(ActivityScenario<GameActivity> s=ActivityScenario.launch(prepare(54,17554,400,true,false))){
   frozen(s);MyGLSurfaceView[] gl={null};Rect b=new Rect();s.onActivity(a->{gl[0]=a.findViewById(R.id.mySurfaceView);gl[0].getGlobalVisibleRect(b);});readyGL(gl[0],4);int[] d=NativeInterface.itemSnapshot();int own=147+StaticBits.team*10;s.onActivity(a->{((ItemOverlay)a.findViewById(R.id.item_overlay)).resetControl();gl[0].camera.focus(d[own+2],d[own+3]);});long hash=NativeInterface.stateHash();float initial=gl[0].camera.left,px=b.left+b.width()*.85f,py=b.centerY();long down=SystemClock.uptimeMillis();touch(down,0,px,py);double[] max={0};JSONArray trace=new JSONArray();
   for(int k=0;k<15;k++){SystemClock.sleep(60);s.onActivity(a->{try{short[][] x=(short[][])get(a,"xs"),y=(short[][])get(a,"ys");double e=Math.hypot(b.left+gl[0].camera.screenX(x[StaticBits.team][0],b.width())-px,b.top+gl[0].camera.screenY(y[StaticBits.team][0],b.height())-py);max[0]=Math.max(max[0],e);trace.put(new JSONArray().put(gl[0].camera.left).put(x[StaticBits.team][0]).put(e));}catch(Exception e){throw new RuntimeException(e);}});}
   Assert.assertTrue(gl[0].camera.left>initial+10);Assert.assertTrue("moving camera cell precision "+max[0],max[0]<3.2);touch(down,1,px,py);short[][] last=targets(s);SystemClock.sleep(200);Assert.assertArrayEquals(last[0],targets(s)[0]);Assert.assertEquals(hash,NativeInterface.stateHash());picture("controls178-moving-precision.jpg");write("controls178-moving-precision.json",new JSONObject().put("status","PASS").put("camera_advanced",gl[0].camera.left-initial).put("max_target_error_px",max[0]).put("ui_only_hash_unchanged",true).put("trace",trace));
  }
 }
}
