package com.xenris.liquidwarsos;
import android.app.*;import android.content.*;import android.os.*;import android.graphics.*;import android.view.*;
import androidx.test.core.app.ActivityScenario;import androidx.test.platform.app.InstrumentationRegistry;import com.xenris.liquidwarsos.activities.GameActivity;import org.junit.*;import java.io.*;
public class Classic193VisualTest {
 static {System.loadLibrary("nativeinterface");System.loadLibrary("guest193probe");}
 public static native void fixture(int mode);
 final Instrumentation inst=InstrumentationRegistry.getInstrumentation();final Context c=inst.getTargetContext();
 int[] active(int team){int[] a=NativeInterface.itemSnapshot();for(int b=209;b<a.length;b++)if(a[b]==0x4c574131)return java.util.Arrays.copyOfRange(a,b+3+team*8,b+11+team*8);throw new AssertionError("active state missing");}
 void shot(String name)throws Exception{Bitmap b=inst.getUiAutomation().takeScreenshot();Assert.assertNotNull(b);try(FileOutputStream o=new FileOutputStream(new File(c.getExternalFilesDir(null),"guest193-classic-"+name+".png"))){b.compress(Bitmap.CompressFormat.PNG,100,o);}b.recycle();}
 void tap(float x,float y){long d=SystemClock.uptimeMillis();for(int a:new int[]{0,1}){MotionEvent e=MotionEvent.obtain(d,SystemClock.uptimeMillis(),a,x,y,0);inst.sendPointerSync(e);e.recycle();}}
 @Test public void actualClassicSlots()throws Exception{
  GardenSession.expeditionSeats=0;StaticBits.init();StaticBits.team=0;StaticBits.map=1;StaticBits.seed=193;StaticBits.dotsPerTeam=400;StaticBits.operationsMode=0;StaticBits.alliances=AllianceRules.solo();StaticBits.timeLimit=240;
  try(ActivityScenario<GameActivity> scene=ActivityScenario.launch(new Intent(c,GameActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))){
   long end=SystemClock.uptimeMillis()+20000;while(NativeInterface.playTimeMillis()<100&&SystemClock.uptimeMillis()<end)Thread.sleep(100);Assert.assertTrue(NativeInterface.playTimeMillis()>0);fixture(0);Thread.sleep(700);
   Rect r=new Rect();scene.onActivity(a->a.findViewById(R.id.item_overlay).getGlobalVisibleRect(r));float dp=c.getResources().getDisplayMetrics().density;
   shot("ready");tap(r.right-112*dp,r.bottom-38*dp);Thread.sleep(500);Assert.assertEquals(1,active(0)[0]);int[] visible=NativeInterface.itemSnapshot();for(int t=1;t<6;t++)if(visible[13+t]>0)Assert.assertTrue("living enemy frozen, seat "+t,visible[25+t*20+2]>8500);shot("global-freeze");
   fixture(1);Thread.sleep(500);Assert.assertTrue(active(0)[4]>NativeInterface.playTimeMillis());shot("frozen");tap(r.right-46*dp,r.bottom-38*dp);Thread.sleep(500);Assert.assertEquals(1,active(0)[1]);Assert.assertTrue(active(0)[4]<=NativeInterface.playTimeMillis());Assert.assertTrue(active(0)[5]>NativeInterface.playTimeMillis());shot("cleanse");
  }
 }
}
