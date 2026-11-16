package com.xenris.liquidwarsos;
import android.app.*;import android.content.*;import android.graphics.*;import android.os.*;import android.view.*;
import androidx.test.core.app.ActivityScenario;import androidx.test.platform.app.InstrumentationRegistry;
import com.xenris.liquidwarsos.activities.GameActivity;import org.junit.*;import java.io.*;

/** Actual large-map UI, no network accounts or production fixtures. */
public class Hud194LargeMapTest {
 final Instrumentation inst=InstrumentationRegistry.getInstrumentation();final Context ctx=inst.getTargetContext();
 ExpeditionOverlay overlay;MyGLSurfaceView surface;LargeMapHud hud;float unit,w,h;Rect rect=new Rect();
 void tap(float x,float y){long down=SystemClock.uptimeMillis();for(int action:new int[]{0,1}){MotionEvent e=MotionEvent.obtain(down,SystemClock.uptimeMillis(),action,rect.left+x*unit,rect.top+y*unit,0);inst.sendPointerSync(e);e.recycle();}SystemClock.sleep(300);}
 void shot(String name)throws Exception{Bitmap image=inst.getUiAutomation().takeScreenshot();try(FileOutputStream out=new FileOutputStream(new File(ctx.getExternalFilesDir(null),"hud194-"+name+".png"))){image.compress(Bitmap.CompressFormat.PNG,100,out);}image.recycle();}
 @Test public void largeMapLayoutAndMapCommands()throws Exception{
  Assert.assertTrue(ctx.getPackageName().endsWith("garden186"));GardenSession.expeditionSeats=0;StaticBits.init();StaticBits.team=0;StaticBits.map=50;StaticBits.seed=194;StaticBits.dotsPerTeam=400;StaticBits.operationsMode=1;StaticBits.alliances=AllianceRules.solo();StaticBits.timeLimit=240;
  try(ActivityScenario<GameActivity> scene=ActivityScenario.launch(new Intent(ctx,GameActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))){
   long until=SystemClock.uptimeMillis()+20000;while(NativeInterface.playTimeMillis()<100&&SystemClock.uptimeMillis()<until)SystemClock.sleep(100);
   scene.onActivity(a->{overlay=a.findViewById(R.id.expedition_overlay);surface=a.findViewById(R.id.mySurfaceView);overlay.getGlobalVisibleRect(rect);});
   java.util.concurrent.atomic.AtomicBoolean ready=new java.util.concurrent.atomic.AtomicBoolean();
   until=SystemClock.uptimeMillis()+20000;while(!ready.get()&&SystemClock.uptimeMillis()<until){scene.onActivity(a->ready.set(a.findViewById(R.id.match_intro).getVisibility()!=View.VISIBLE));SystemClock.sleep(100);}
   Assert.assertTrue("intro is finished before real screen commands",ready.get());
   java.lang.reflect.Field f=ExpeditionOverlay.class.getDeclaredField("largeHud");f.setAccessible(true);hud=(LargeMapHud)f.get(overlay);
   unit=BattleHudLayout.unit(rect.width(),rect.height());w=rect.width()/unit;h=rect.height()/unit;Assert.assertTrue(hud.active());Assert.assertEquals(800,surface.camera.viewWidth,.1);
   shot("large-ready");tap(w-90,65);Assert.assertTrue(hud.expanded());shot("large-expanded");
   tap(w/2,h-55);RectF map=BattleHudLayout.expanded(w,h,surface.camera.width,surface.camera.height);tap(map.centerX(),map.centerY());Assert.assertFalse("locating does not follow army",surface.camera.following);
   tap(w/2+110,h-55);OperationsSnapshot before=new OperationsSnapshot(NativeInterface.operationsSnapshot(0,false));int b=before.group(0,0),tx=before.raw[b+1],ty=before.raw[b+2];
   tap(map.left+(tx+.5f)*map.width()/surface.camera.width,map.top+(surface.camera.height-.5f-ty)*map.height()/surface.camera.height);
   OperationsSnapshot after=new OperationsSnapshot(NativeInterface.operationsSnapshot(0,false));Assert.assertTrue("map command enters real command pipeline",after.raw[after.group(0,0)+3]!=0);
   tap(w-75,26);Assert.assertFalse(hud.expanded());tap(224,24);Assert.assertEquals(400,surface.camera.viewWidth,.1);tap(224,24);Assert.assertEquals(800,surface.camera.viewWidth,.1);
   tap(w-90,65);Assert.assertTrue(hud.expanded());inst.sendKeyDownUpSync(KeyEvent.KEYCODE_BACK);SystemClock.sleep(300);Assert.assertFalse("back closes map before leaving match",hud.expanded());
   shot("large-returned");
  }
 }
}
