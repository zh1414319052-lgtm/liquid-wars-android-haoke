package com.xenris.liquidwarsos;

import android.app.*;import android.content.*;import android.graphics.*;import android.os.*;import android.view.*;
import androidx.lifecycle.Lifecycle;import androidx.test.core.app.ActivityScenario;
import androidx.test.platform.app.InstrumentationRegistry;
import com.xenris.liquidwarsos.activities.GameActivity;
import org.junit.*;import java.io.*;import java.lang.reflect.*;

/** Real injected touches in the isolated QA package; no production app preferences. */
public class Joystick213DeviceTest {
 final Instrumentation inst=InstrumentationRegistry.getInstrumentation();final Context c=inst.getTargetContext();
 JoystickOverlay joy;TacticalOverlay orders;ItemOverlay items;MyGLSurfaceView surface;Rect area=new Rect();float dp;
 int[] injectedIds=new int[0];float[] injectedXY=new float[0];
 void ui(Runnable r){inst.runOnMainSync(r);}
 void event(long down,int action,int[] ids,float... xy){
  MotionEvent.PointerProperties[] pp=new MotionEvent.PointerProperties[ids.length];MotionEvent.PointerCoords[] pc=new MotionEvent.PointerCoords[ids.length];
  for(int i=0;i<ids.length;i++){pp[i]=new MotionEvent.PointerProperties();pp[i].id=ids[i];pp[i].toolType=1;pc[i]=new MotionEvent.PointerCoords();pc[i].x=area.left+xy[i*2];pc[i].y=area.top+xy[i*2+1];pc[i].pressure=1;pc[i].size=1;}
  MotionEvent e=MotionEvent.obtain(down,SystemClock.uptimeMillis(),action,ids.length,pp,pc,0,0,1,1,-1,0,InputDevice.SOURCE_TOUCHSCREEN,0);boolean ok=inst.getUiAutomation().injectInputEvent(e,true);e.recycle();Assert.assertTrue("injected action="+action,ok);
  int masked=action&255;if(masked==MotionEvent.ACTION_UP||masked==MotionEvent.ACTION_CANCEL){injectedIds=new int[0];injectedXY=new float[0];}
  else if(masked==MotionEvent.ACTION_POINTER_UP){int removed=action>>8;injectedIds=new int[ids.length-1];injectedXY=new float[injectedIds.length*2];for(int i=0,k=0;i<ids.length;i++)if(i!=removed){injectedIds[k]=ids[i];injectedXY[k*2]=xy[i*2];injectedXY[k*2+1]=xy[i*2+1];k++;}}
  else{injectedIds=ids.clone();injectedXY=xy.clone();}
 }
 void tap(float x,float y){long t=SystemClock.uptimeMillis();event(t,0,new int[]{0},x,y);event(t,1,new int[]{0},x,y);SystemClock.sleep(150);}
 void drag(float x,float y,float xx,float yy){long t=SystemClock.uptimeMillis();event(t,0,new int[]{0},x,y);for(int i=1;i<=6;i++){event(t,2,new int[]{0},x+(xx-x)*i/6,y+(yy-y)*i/6);SystemClock.sleep(25);}event(t,1,new int[]{0},xx,yy);SystemClock.sleep(100);}
 float[] point(){float[] r=new float[2];ui(()->{r[0]=joy.cursor().x();r[1]=joy.cursor().y();});return r;}
 float[] pad(int side){float[] r=new float[2];ui(()->{r[0]=joy.padX(side);r[1]=joy.padY(side);});return r;}
 void shot(String name)throws Exception{Bitmap b=inst.getUiAutomation().takeScreenshot();File f=new File(c.getExternalFilesDir(null),name+".png");try(FileOutputStream out=new FileOutputStream(f)){b.compress(Bitmap.CompressFormat.PNG,100,out);}b.recycle();}
 void bind(ActivityScenario<GameActivity> scene){
  scene.onActivity(a->{joy=a.findViewById(R.id.joystick_overlay);orders=a.findViewById(R.id.tactical_overlay);items=a.findViewById(R.id.item_overlay);surface=a.findViewById(R.id.mySurfaceView);joy.getGlobalVisibleRect(area);dp=c.getResources().getDisplayMetrics().density;});
  long end=SystemClock.uptimeMillis()+20000;boolean[] ready={false};while(!ready[0]&&SystemClock.uptimeMillis()<end){ui(()->ready[0]=joy.hasWindowFocus()&&orders.facts()!=null&&((View)joy.getParent()).findViewById(R.id.match_intro).getVisibility()!=View.VISIBLE);SystemClock.sleep(100);}Assert.assertTrue("battle ready",ready[0]);clearInjectedStream();
 }
 void mode(){tap(30*dp,orders.controlsTop()+15*dp);}
 void settings(){RectF target=orders.settingsBounds();tap(target.centerX(),target.centerY());Assert.assertTrue(joy.editing());}
 void editor(int n){tap(area.width()/2f+(n-1.5f)*68*dp+31*dp,29*dp);}
 @Before public void clearInjectedStream(){
  // Android 15 requires cancellation to match virtual device and active pointer IDs.
  // ID 3 is the known held pointer left by the earlier failed version of this probe.
  int[] ids=injectedIds.length>0?injectedIds:new int[]{3};float[] xy=injectedXY.length>0?injectedXY:new float[]{0,0};
  MotionEvent.PointerProperties[] pp=new MotionEvent.PointerProperties[ids.length];MotionEvent.PointerCoords[] pc=new MotionEvent.PointerCoords[ids.length];
  for(int i=0;i<ids.length;i++){pp[i]=new MotionEvent.PointerProperties();pp[i].id=ids[i];pp[i].toolType=1;pc[i]=new MotionEvent.PointerCoords();pc[i].x=area.left+xy[i*2];pc[i].y=area.top+xy[i*2+1];}
  long t=SystemClock.uptimeMillis();MotionEvent e=MotionEvent.obtain(t,t,MotionEvent.ACTION_CANCEL,ids.length,pp,pc,0,0,1,1,-1,0,InputDevice.SOURCE_TOUCHSCREEN,0);inst.getUiAutomation().injectInputEvent(e,true);e.recycle();injectedIds=new int[0];injectedXY=new float[0];
 }
 @After public void endInjectedStream(){clearInjectedStream();}
 void init(int map){GardenSession.active=false;GardenSession.expeditionSeats=0;StaticBits.init();StaticBits.map=map;StaticBits.seed=213;StaticBits.dotsPerTeam=400;StaticBits.timeLimit=240;StaticBits.operationsMode=0;StaticBits.alliances=AllianceRules.solo();}
 @Test public void classicSamePointTwoPadsEditorAndReentry()throws Exception{
  SharedPreferences pref=c.getSharedPreferences("battle-controls",0);pref.edit().putInt("command-mode",2).apply();init(1);
  JoystickPlacement initialLayout=new JoystickPlacement();initialLayout.defaults();initialLayout.save(pref);
  float[] savedLeft=new float[2],savedRight=new float[2];
  try(ActivityScenario<GameActivity> scene=ActivityScenario.launch(new Intent(c,GameActivity.class))){
   bind(scene);Classic193VisualTest.fixture(0);Classic193VisualTest.fixture(4);tap(area.width()*.47f,area.height()*.44f);float[] touch=point();Assert.assertTrue(items.followCursor.ready());
   mode();Assert.assertTrue(orders.joystickEnabled());Assert.assertSame(items.followCursor,joy.cursor());Assert.assertArrayEquals(touch,point(),0);
   tap(area.width()*.67f,area.height()*.40f);Assert.assertArrayEquals("map taps cannot replace stick target",touch,point(),0);
   float[] l=pad(0),r=pad(1);float dx=joy.radius()*.66f;long t=SystemClock.uptimeMillis();
   event(t,0,new int[]{3},l[0],l[1]);event(t,2,new int[]{3},l[0]+dx,l[1]);SystemClock.sleep(400);float[] moved=point();Assert.assertTrue(moved[0]>touch[0]+15);shot("classic-left-held");
   event(t,MotionEvent.ACTION_POINTER_DOWN|(1<<8),new int[]{3,7},l[0]+dx,l[1],r[0]-dx,r[1]);SystemClock.sleep(250);float[] both=point();Assert.assertTrue(both[0]>moved[0]);Assert.assertEquals(0,joy.sticks.owner());
   event(t,MotionEvent.ACTION_POINTER_UP,new int[]{3,7},l[0]+dx,l[1],r[0]-dx,r[1]);SystemClock.sleep(400);Assert.assertEquals(1,joy.sticks.owner());Assert.assertTrue(point()[0]<both[0]-10);shot("classic-right-takeover");
   event(t,1,new int[]{7},r[0]-dx,r[1]);float[] released=point();SystemClock.sleep(250);Assert.assertArrayEquals(released,point(),0);
   settings();float largeRadius=joy.radius();tap(area.width()/2f-45*dp,93*dp);Assert.assertEquals(largeRadius*.75f,joy.radius(),.1);shot("position-editor");editor(2);Assert.assertEquals(.75f,pref.getFloat("stick-size",0),.001);
   settings();tap(area.width()/2f+135*dp,93*dp);Assert.assertTrue(joy.radius()>largeRadius);editor(1);Assert.assertEquals(largeRadius*.75f,joy.radius(),.1);
   settings();editor(0);Assert.assertEquals(largeRadius,joy.radius(),.1);editor(2);Assert.assertEquals(1,pref.getFloat("stick-size",0),.001);
   settings();tap(area.width()/2f+45*dp,153*dp);editor(2);Assert.assertEquals(1.5f,pref.getFloat("stick-sensitivity",0),.001);
   settings();tap(area.width()/2f-135*dp,153*dp);editor(1);Assert.assertEquals(1.5f,pref.getFloat("stick-sensitivity",0),.001);
   settings();drag(l[0],l[1],l[0]+40*dp,l[1]-25*dp);drag(r[0],r[1],r[0]-40*dp,r[1]-20*dp);float[] newL=pad(0),newR=pad(1);editor(2);Assert.assertFalse(joy.editing());
   Assert.assertTrue(pref.contains("stick-0-x"));Assert.assertTrue(pref.contains("stick-1-x"));Assert.assertEquals(newL[0],pad(0)[0],.1);
   settings();drag(newL[0],newL[1],newL[0]+30*dp,newL[1]);editor(1);Assert.assertArrayEquals(newL,pad(0),.1f);
   System.arraycopy(newL,0,savedLeft,0,2);System.arraycopy(newR,0,savedRight,0,2);shot("classic-saved-layout");
   l=pad(0);t=SystemClock.uptimeMillis();event(t,0,new int[]{0},l[0],l[1]);event(t,2,new int[]{0},l[0]+dx,l[1]);SystemClock.sleep(200);
   scene.onActivity(GameActivity::requestLeaveMatch);SystemClock.sleep(200);clearInjectedStream();
   Assert.assertFalse(joy.sticks.held());
   scene.onActivity(a->{try{Field f=GameActivity.class.getDeclaredField("dialog");f.setAccessible(true);((AlertDialog)f.get(a)).getButton(DialogInterface.BUTTON_NEGATIVE).performClick();}catch(Exception e){throw new AssertionError(e);}});
   SystemClock.sleep(250);
   float[] resumed=point();SystemClock.sleep(250);Assert.assertArrayEquals(resumed,point(),0);Assert.assertFalse(joy.sticks.held());
   Classic193VisualTest.fixture(1);SystemClock.sleep(180);
   Assert.assertTrue(new Classic193VisualTest().active(0)[4]>NativeInterface.playTimeMillis());
   l=pad(0);t=SystemClock.uptimeMillis();event(t,0,new int[]{3},l[0],l[1]);event(t,2,new int[]{3},l[0]+dx,l[1]);
   float cx=area.width()-112*dp,cy=area.height()-38*dp;
   event(t,MotionEvent.ACTION_POINTER_DOWN|(1<<8),new int[]{3,7},l[0]+dx,l[1],cx,cy);
   event(t,MotionEvent.ACTION_POINTER_UP|(1<<8),new int[]{3,7},l[0]+dx,l[1],cx,cy);SystemClock.sleep(250);
   long until=SystemClock.uptimeMillis()+2000;while(new Classic193VisualTest().active(0)[4]>NativeInterface.playTimeMillis()&&SystemClock.uptimeMillis()<until)SystemClock.sleep(40);
   shot("classic-cleanse-while-steering");Assert.assertTrue("Cleanse must survive continued movement packets: "+java.util.Arrays.toString(new Classic193VisualTest().active(0)),new Classic193VisualTest().active(0)[4]<=NativeInterface.playTimeMillis());Assert.assertTrue("shared target survives item use",joy.cursor().ready());
   event(t,1,new int[]{3},l[0]+dx,l[1]);shot("classic-cleanse-while-steering");
   mode();mode();mode();Assert.assertFalse(orders.joystickEnabled());tap(area.width()*.35f,area.height()*.42f);Assert.assertFalse(java.util.Arrays.equals(resumed,point()));
   mode();Assert.assertTrue(orders.joystickEnabled());
  }
   init(1);try(ActivityScenario<GameActivity> scene=ActivityScenario.launch(new Intent(c,GameActivity.class))){bind(scene);Assert.assertTrue(orders.joystickEnabled());Assert.assertArrayEquals(savedLeft,pad(0),.1f);Assert.assertArrayEquals(savedRight,pad(1),.1f);Assert.assertEquals(1.5f,pref.getFloat("stick-sensitivity",0),.001);shot("classic-reentered");}
 }
 @Test public void largeMapBothSticksAndNoTouchCommand()throws Exception{
  c.getSharedPreferences("battle-controls",0).edit().putInt("command-mode",3).apply();init(50);
  try(ActivityScenario<GameActivity> scene=ActivityScenario.launch(new Intent(c,GameActivity.class))){
   bind(scene);Assert.assertEquals(800,surface.camera.viewWidth,.1);float[] r=pad(1);long t=SystemClock.uptimeMillis();event(t,0,new int[]{0},r);event(t,2,new int[]{0},r[0]-joy.radius()*.66f,r[1]);SystemClock.sleep(400);event(t,1,new int[]{0},r[0]-joy.radius()*.66f,r[1]);float[] before=point();tap(area.width()*.55f,area.height()*.45f);Assert.assertArrayEquals(before,point(),0);
   shot("large-map-sticks");settings();editor(1);Assert.assertFalse(joy.editing());
   float u=BattleHudLayout.unit(area.width(),area.height());tap(area.width()-100*u,90*u);Assert.assertTrue(((ExpeditionOverlay)((View)joy.getParent()).findViewById(R.id.expedition_overlay)).modalOpen());inst.sendKeyDownUpSync(KeyEvent.KEYCODE_BACK);SystemClock.sleep(150);Assert.assertFalse(((ExpeditionOverlay)((View)joy.getParent()).findViewById(R.id.expedition_overlay)).modalOpen());
  }
 }
 @Test public void setupSelectionsSurviveReentryWithoutCrossModeReset(){
  Class<com.xenris.liquidwarsos.activities.SinglePlayerGameSetupActivity> type=com.xenris.liquidwarsos.activities.SinglePlayerGameSetupActivity.class;
  Intent classic=new Intent(c,type);
  try(ActivityScenario<com.xenris.liquidwarsos.activities.SinglePlayerGameSetupActivity> s=ActivityScenario.launch(classic)){
   s.onActivity(a->{((android.widget.Spinner)a.findViewById(R.id.map_spinner)).setSelection(3);((android.widget.Spinner)a.findViewById(R.id.team_spinner)).setSelection(2);((android.widget.Spinner)a.findViewById(R.id.timeout_spinner)).setSelection(4);((android.widget.Spinner)a.findViewById(R.id.teamsize_spinner)).setSelection(3);});inst.waitForIdleSync();
  }
  try(ActivityScenario<com.xenris.liquidwarsos.activities.SinglePlayerGameSetupActivity> s=ActivityScenario.launch(new Intent(c,type).putExtra("largeMode",true))){inst.waitForIdleSync();}
  try(ActivityScenario<com.xenris.liquidwarsos.activities.SinglePlayerGameSetupActivity> s=ActivityScenario.launch(classic)){
   inst.waitForIdleSync();s.onActivity(a->{Assert.assertEquals(3,((android.widget.Spinner)a.findViewById(R.id.map_spinner)).getSelectedItemPosition());Assert.assertEquals(2,((android.widget.Spinner)a.findViewById(R.id.team_spinner)).getSelectedItemPosition());Assert.assertEquals(4,((android.widget.Spinner)a.findViewById(R.id.timeout_spinner)).getSelectedItemPosition());Assert.assertEquals(3,((android.widget.Spinner)a.findViewById(R.id.teamsize_spinner)).getSelectedItemPosition());});
  }
 }
}
