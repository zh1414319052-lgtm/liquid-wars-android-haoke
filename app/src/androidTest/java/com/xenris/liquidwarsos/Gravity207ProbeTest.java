package com.xenris.liquidwarsos;
import android.content.*;
import android.graphics.Rect;
import android.os.SystemClock;
import androidx.test.core.app.ActivityScenario;
import androidx.test.platform.app.InstrumentationRegistry;
import com.xenris.liquidwarsos.activities.GameActivity;
import org.junit.*;

/** Tests real early input, not a synthetic clock advance. */
public class Gravity207ProbeTest {
 @Test public void warningAndEarlyCleanse()throws Exception{
  Context c=InstrumentationRegistry.getInstrumentation().getTargetContext();
  GardenSession.expeditionSeats=0;StaticBits.init();StaticBits.team=0;StaticBits.map=1;StaticBits.seed=193;StaticBits.dotsPerTeam=400;StaticBits.operationsMode=0;StaticBits.alliances=AllianceRules.solo();StaticBits.timeLimit=240;
  try(ActivityScenario<GameActivity> scene=ActivityScenario.launch(new Intent(c,GameActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))){
   long until=SystemClock.uptimeMillis()+20000;while(NativeInterface.playTimeMillis()<100&&SystemClock.uptimeMillis()<until)Thread.sleep(100);
   Assert.assertTrue(NativeInterface.playTimeMillis()>0);Classic193VisualTest.fixture(2);Thread.sleep(350);
   Rect r=new Rect();scene.onActivity(a->a.findViewById(R.id.item_overlay).getGlobalVisibleRect(r));float dp=c.getResources().getDisplayMetrics().density;
   Classic193VisualTest input=new Classic193VisualTest();input.tap(r.right-46*dp,r.bottom-38*dp);Thread.sleep(500);
   int[] facts=NativeInterface.audioSnapshot(0,false);Assert.assertTrue("live reaction window",facts[1]-facts[2]<1500&&facts[3]>facts[1]);
   int[] world=NativeInterface.itemSnapshot();int at=209;while(at+6<world.length&&world[at]!=BlackHoleVisual.FACTS)at++;
   Assert.assertTrue("warning cannot swallow",at+6<world.length&&world[at+5]==0);
   input.shot("gravity207-warning");input.tap(r.right-112*dp,r.bottom-38*dp);Thread.sleep(220);
   Assert.assertEquals("cleanse immediately exempts player",1,input.active(0)[9]);
   Thread.sleep(1300);Assert.assertTrue("others remain under a live hole",NativeInterface.blackHoleActive());
   Assert.assertEquals(1,input.active(0)[9]);input.shot("gravity207-cleanse");
  }
 }
}
