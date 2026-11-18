package com.xenris.liquidwarsos;
import android.app.*;import android.content.*;import android.os.*;import android.graphics.*;import android.view.*;
import androidx.test.core.app.ActivityScenario;import androidx.test.platform.app.InstrumentationRegistry;import com.xenris.liquidwarsos.activities.GameActivity;import org.junit.*;import java.io.*;
public class Classic193VisualTest {
 static {System.loadLibrary("nativeinterface");System.loadLibrary("guest193probe");}
 public static native void fixture(int mode);
 final Instrumentation inst=InstrumentationRegistry.getInstrumentation();final Context c=inst.getTargetContext();
 int[] active(int team){int[] a=NativeInterface.itemSnapshot();for(int b=209;b<a.length;b++)if(a[b]==0x4c574131)return java.util.Arrays.copyOfRange(a,b+3+team*10,b+13+team*10);throw new AssertionError("active state missing");}
 void shot(String name)throws Exception{Bitmap b=inst.getUiAutomation().takeScreenshot();Assert.assertNotNull(b);try(FileOutputStream o=new FileOutputStream(new File(c.getExternalFilesDir(null),"guest193-classic-"+name+".png"))){b.compress(Bitmap.CompressFormat.PNG,100,o);}b.recycle();}
 void tap(float x,float y){long d=SystemClock.uptimeMillis();for(int a:new int[]{0,1}){MotionEvent e=MotionEvent.obtain(d,SystemClock.uptimeMillis(),a,x,y,0);inst.sendPointerSync(e);e.recycle();}}
 @Test public void actualBlackHoleSlots() throws Exception {
  GardenSession.expeditionSeats=0;StaticBits.init();StaticBits.team=0;StaticBits.map=1;StaticBits.seed=193;StaticBits.dotsPerTeam=400;StaticBits.operationsMode=0;StaticBits.alliances=AllianceRules.solo();StaticBits.timeLimit=240;
  try(ActivityScenario<GameActivity> scene=ActivityScenario.launch(new Intent(c,GameActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))){
   long end=SystemClock.uptimeMillis()+20000;
   while(NativeInterface.playTimeMillis()<100&&SystemClock.uptimeMillis()<end)Thread.sleep(100);
   Assert.assertTrue(NativeInterface.playTimeMillis()>0);
   scene.onActivity(a->a.onControls(new short[]{-2,-1,-1,-1,-1},new short[]{-2,-1,-1,-1,-1}));
   fixture(2);Thread.sleep(500);
   Rect bounds=new Rect();scene.onActivity(a->a.findViewById(R.id.item_overlay).getGlobalVisibleRect(bounds));
   float dp=c.getResources().getDisplayMetrics().density;
   shot("black-hole-ready");
   tap(bounds.right-46*dp,bounds.bottom-38*dp);
   // Match the simulation clock rather than emulator wall-clock speed.
   long waitUntil=SystemClock.uptimeMillis()+12000;
   while(SystemClock.uptimeMillis()<waitUntil){int[] a=NativeInterface.itemSnapshot();boolean absorbed=false;for(int b=209;b+6<a.length;b++)if(a[b]==BlackHoleVisual.FACTS&&a[b+5]>0)absorbed=true;if(absorbed)break;Thread.sleep(50);}
   Assert.assertTrue(NativeInterface.blackHoleActive());Assert.assertEquals(1,active(0)[8]);
   int[] facts=NativeInterface.itemSnapshot();
   int marker=209;
   while(marker+6<facts.length&&facts[marker]!=BlackHoleVisual.FACTS)marker++;
   Assert.assertTrue("Live core reported absorption; audio="+java.util.Arrays.toString(NativeInterface.audioSnapshot(0,false))+" hole="+java.util.Arrays.toString(java.util.Arrays.copyOfRange(facts,marker,Math.min(facts.length,marker+7))),marker+6<facts.length&&facts[marker+5]>0);
   Assert.assertTrue("Fixture must keep the caster alive to tap cleanse: "+facts[13],facts[13]>0);
   tap(bounds.right-112*dp,bounds.bottom-38*dp);Thread.sleep(300);
   Assert.assertTrue(NativeInterface.blackHoleActive());Assert.assertEquals("cleanse after absorption: "+java.util.Arrays.toString(active(0)),1,active(0)[9]);Assert.assertEquals(0,active(0)[1]);
   shot("black-hole-cleansed");
  }
 }
 @Test public void actualClassicSlots()throws Exception{
  GardenSession.expeditionSeats=0;StaticBits.init();StaticBits.team=0;StaticBits.map=1;StaticBits.seed=193;StaticBits.dotsPerTeam=400;StaticBits.operationsMode=0;StaticBits.alliances=AllianceRules.solo();StaticBits.timeLimit=240;
  try(ActivityScenario<GameActivity> scene=ActivityScenario.launch(new Intent(c,GameActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))){
   long end=SystemClock.uptimeMillis()+20000;while(NativeInterface.playTimeMillis()<100&&SystemClock.uptimeMillis()<end)Thread.sleep(100);Assert.assertTrue(NativeInterface.playTimeMillis()>0);fixture(0);Thread.sleep(700);
   Rect r=new Rect();scene.onActivity(a->a.findViewById(R.id.item_overlay).getGlobalVisibleRect(r));float dp=c.getResources().getDisplayMetrics().density;
   shot("ready");tap(r.right-178*dp,r.bottom-38*dp);Thread.sleep(500);Assert.assertEquals(1,active(0)[0]);int[] visible=NativeInterface.itemSnapshot();for(int t=1;t<6;t++)if(visible[13+t]>0)Assert.assertTrue("living enemy frozen, seat "+t,visible[25+t*20+2]>8500);shot("global-freeze");
   fixture(1);Thread.sleep(500);Assert.assertTrue(active(0)[4]>NativeInterface.playTimeMillis());shot("frozen");tap(r.right-112*dp,r.bottom-38*dp);Thread.sleep(500);Assert.assertEquals(1,active(0)[1]);Assert.assertTrue(active(0)[4]<=NativeInterface.playTimeMillis());Assert.assertTrue(active(0)[5]>NativeInterface.playTimeMillis());shot("cleanse");
  }
 }
}
