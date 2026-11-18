package com.xenris.liquidwarsos;
import android.app.*;import android.content.*;import android.graphics.*;import android.os.*;import android.view.*;import androidx.lifecycle.Lifecycle;
import androidx.test.core.app.ActivityScenario;import androidx.test.platform.app.InstrumentationRegistry;import com.xenris.liquidwarsos.activities.GameActivity;import org.junit.*;import java.io.*;
public class Joystick207ProbeTest {
 final Instrumentation inst=InstrumentationRegistry.getInstrumentation();final Context c=inst.getTargetContext();
 void touch(long down,int action,float... xy){MotionEvent.PointerProperties[] pp=new MotionEvent.PointerProperties[xy.length/2];MotionEvent.PointerCoords[] pc=new MotionEvent.PointerCoords[pp.length];for(int i=0;i<pp.length;i++){pp[i]=new MotionEvent.PointerProperties();pp[i].id=i;pp[i].toolType=MotionEvent.TOOL_TYPE_FINGER;pc[i]=new MotionEvent.PointerCoords();pc[i].x=xy[2*i];pc[i].y=xy[2*i+1];pc[i].pressure=1;pc[i].size=1;}MotionEvent e=MotionEvent.obtain(down,SystemClock.uptimeMillis(),action,pp.length,pp,pc,0,0,1,1,0,0,InputDevice.SOURCE_TOUCHSCREEN,0);Assert.assertTrue(inst.getUiAutomation().injectInputEvent(e,true));e.recycle();}
 void shot(String name)throws Exception{Bitmap b=inst.getUiAutomation().takeScreenshot();try(FileOutputStream o=new FileOutputStream(new File(c.getExternalFilesDir(null),name))){b.compress(Bitmap.CompressFormat.PNG,100,o);}b.recycle();}
 @Test public void joystickMovesTargetAndSecondFingerCleanses()throws Exception{
  SharedPreferences pref=c.getSharedPreferences("battle-controls",0);int original=pref.getInt("command-mode",2);pref.edit().putInt("command-mode",3).commit();
  GardenSession.expeditionSeats=0;StaticBits.init();StaticBits.team=0;StaticBits.map=1;StaticBits.seed=193;StaticBits.dotsPerTeam=400;StaticBits.operationsMode=0;StaticBits.alliances=AllianceRules.solo();StaticBits.timeLimit=240;
  try(ActivityScenario<GameActivity> scene=ActivityScenario.launch(new Intent(c,GameActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))){
   long end=SystemClock.uptimeMillis()+20000;while(NativeInterface.playTimeMillis()<100&&SystemClock.uptimeMillis()<end)Thread.sleep(100);Assert.assertTrue(NativeInterface.playTimeMillis()>0);
   Classic193VisualTest.fixture(0);Thread.sleep(250);JoystickOverlay[] joy={null};Rect r=new Rect();float[] pad=new float[2];float dp=c.getResources().getDisplayMetrics().density;
   scene.onActivity(a->{joy[0]=a.findViewById(R.id.joystick_overlay);joy[0].getGlobalVisibleRect(r);pad[0]=r.left+joy[0].padX();pad[1]=r.top+joy[0].padY();Assert.assertTrue(((TacticalOverlay)a.findViewById(R.id.tactical_overlay)).joystickEnabled());});
   long down=SystemClock.uptimeMillis();touch(down,0,pad);float[] origin=new float[2];scene.onActivity(a->{origin[0]=joy[0].cursor().x();origin[1]=joy[0].cursor().y();});
   float x=pad[0]+34*dp,y=pad[1];touch(down,2,x,y);Thread.sleep(500);
   float[] moved=new float[2];scene.onActivity(a->{moved[0]=joy[0].cursor().x();moved[1]=joy[0].cursor().y();});Assert.assertTrue(moved[0]>origin[0]+10);Assert.assertEquals(origin[1],moved[1],.1);
   touch(down,1,x,y);Thread.sleep(180);float[] held=new float[2];scene.onActivity(a->{held[0]=joy[0].cursor().x();held[1]=joy[0].cursor().y();});Thread.sleep(240);scene.onActivity(a->Assert.assertEquals(held[0],joy[0].cursor().x(),0));
   OperationsSnapshot s=new OperationsSnapshot(NativeInterface.operationsSnapshot(0,false));int b=s.group(0,0);Assert.assertEquals(1,s.raw[b+7]);Assert.assertEquals(Math.round(held[0]),s.raw[b+8]);Assert.assertEquals(Math.round(held[1]),s.raw[b+9]);shot("joystick207-held.png");
   Classic193VisualTest.fixture(1);Thread.sleep(200);Assert.assertTrue(new Classic193VisualTest().active(0)[4]>NativeInterface.playTimeMillis());
   down=SystemClock.uptimeMillis();touch(down,0,pad);touch(down,2,x,y);float cx=r.right-112*dp,cy=r.bottom-38*dp;
   touch(down,MotionEvent.ACTION_POINTER_DOWN|(1<<8),x,y,cx,cy);touch(down,MotionEvent.ACTION_POINTER_UP|(1<<8),x,y,cx,cy);Thread.sleep(350);
   Assert.assertTrue(new Classic193VisualTest().active(0)[4]<=NativeInterface.playTimeMillis());Assert.assertEquals(1,new Classic193VisualTest().active(0)[1]);shot("joystick207-cleanse.png");
   touch(down,MotionEvent.ACTION_CANCEL,x,y);scene.moveToState(Lifecycle.State.CREATED);Thread.sleep(180);scene.moveToState(Lifecycle.State.RESUMED);Thread.sleep(250);float[] resumed={0};scene.onActivity(a->resumed[0]=joy[0].cursor().x());Thread.sleep(250);scene.onActivity(a->Assert.assertEquals(resumed[0],joy[0].cursor().x(),0));
   Assert.assertEquals(3,pref.getInt("command-mode",-1));
  }finally{pref.edit().putInt("command-mode",original).commit();}
 }
}
