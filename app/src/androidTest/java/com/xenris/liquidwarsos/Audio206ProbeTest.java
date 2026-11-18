// Test-only inventory setup, with real production contact and Android decoders.
package com.xenris.liquidwarsos;
import android.app.Instrumentation;
import android.content.*;
import android.graphics.Rect;
import android.os.SystemClock;
import androidx.test.core.app.ActivityScenario;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.lifecycle.Lifecycle;
import com.xenris.liquidwarsos.activities.GameActivity;
import org.junit.*;
import org.json.JSONObject;
import java.io.*;

public class Audio206ProbeTest {
 static {System.loadLibrary("nativeinterface");System.loadLibrary("guest193probe");}
 static native void expeditionFixture(int mode);
 @Test public void expeditionCachedAudioBridge()throws Exception{
  Instrumentation inst=InstrumentationRegistry.getInstrumentation();Context c=inst.getTargetContext();GameAudio[] sound={null};boolean original=c.getSharedPreferences("liquid_audio",0).getBoolean("enabled",true);
  // Real expedition simulation/cache and Android output; this is not a Godot visual test.
  try(ActivityScenario<com.xenris.liquidwarsos.activities.MainMenuActivity> menu=ActivityScenario.launch(new Intent(c,com.xenris.liquidwarsos.activities.MainMenuActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))){
   NativeInterface.init(c.getAssets());Assert.assertTrue(NativeInterface.createExpeditionGame(0,2,206));expeditionFixture(0);
   menu.onActivity(a->{sound[0]=new GameAudio(a);sound[0].foreground(true);if(!sound[0].enabled())sound[0].toggle();});
   for(int k=0;k<10;k++){for(int n=0;n<14;n++)NativeInterface.stepDots();menu.onActivity(a->sound[0].frame(null,null,0,false));Thread.sleep(100);}
   Assert.assertEquals(AudioEvents.ENABLED_COUNT,sound[0].loaded);expeditionFixture(1);
   for(int k=0;k<6;k++){for(int n=0;n<14;n++)NativeInterface.stepDots();menu.onActivity(a->sound[0].frame(null,null,0,false));Thread.sleep(100);}
   Assert.assertTrue("3D contact really converts units",NativeInterface.audioSnapshot(0,false)[4]>0);
   Assert.assertEquals("3D contact stays silent",0,sound[0].played[AudioEvents.SWALLOW]);
   expeditionFixture(2);
   for(int k=0;k<16;k++){for(int n=0;n<14;n++)NativeInterface.stepDots();menu.onActivity(a->sound[0].frame(null,null,0,false));Thread.sleep(100);}
   menu.onActivity(a->Assert.assertTrue("cached 3D black hole drives actual player",sound[0].hole.playing()));
   JSONObject report=new JSONObject().put("status","PASS").put("mode","real expedition cached HUD and Android audio; no Godot visual assertions").put("swallow_streams",sound[0].played[AudioEvents.SWALLOW]).put("hole_starts",sound[0].hole.starts).put("hole_errors",sound[0].hole.errors);
   Assert.assertEquals(0,sound[0].hole.errors);menu.onActivity(a->sound[0].release());
   try(FileWriter out=new FileWriter(new File(c.getExternalFilesDir(null),"audio211-expedition.json"))){out.write(report.toString(2));}
   NativeInterface.destroyGame();NativeInterface.uninit();
  }finally{if(sound[0]!=null)inst.runOnMainSync(()->sound[0].release());c.getSharedPreferences("liquid_audio",0).edit().putBoolean("enabled",original).commit();}
 }
 @Test public void selectedSoundsAndLifecycle()throws Exception{
  Instrumentation inst=InstrumentationRegistry.getInstrumentation();Context c=inst.getTargetContext();
  GardenSession.expeditionSeats=0;StaticBits.init();StaticBits.team=0;StaticBits.map=1;StaticBits.seed=193;StaticBits.dotsPerTeam=400;StaticBits.operationsMode=0;StaticBits.alliances=AllianceRules.solo();StaticBits.timeLimit=240;
  GameAudio[] sound={null};boolean[] original={true};
  try(ActivityScenario<GameActivity> scene=ActivityScenario.launch(new Intent(c,GameActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))){
   long end=SystemClock.uptimeMillis()+20000;
   while(NativeInterface.playTimeMillis()<100&&SystemClock.uptimeMillis()<end)Thread.sleep(100);
   Assert.assertTrue("actual running game",NativeInterface.playTimeMillis()>0);
   scene.onActivity(a->{sound[0]=((MyGLSurfaceView)a.findViewById(R.id.mySurfaceView)).audio();original[0]=sound[0].enabled();if(!original[0])sound[0].toggle();});
   Thread.sleep(600);Assert.assertEquals(AudioEvents.ENABLED_COUNT,sound[0].loaded);Assert.assertEquals(0,sound[0].loadErrors);
   Classic193VisualTest.fixture(0);Thread.sleep(250);int swallows=sound[0].played[AudioEvents.SWALLOW];
   Classic193VisualTest.fixture(3);Thread.sleep(350);
   Assert.assertTrue("2D contact really converts units",NativeInterface.audioSnapshot(0,false)[4]>0);
   Assert.assertEquals("2D contact stays silent",swallows,sound[0].played[AudioEvents.SWALLOW]);
   Classic193VisualTest.fixture(2);Thread.sleep(250);
   Rect bounds=new Rect();scene.onActivity(a->a.findViewById(R.id.item_overlay).getGlobalVisibleRect(bounds));float dp=c.getResources().getDisplayMetrics().density;
   new Classic193VisualTest().tap(bounds.right-46*dp,bounds.bottom-38*dp);
   Thread.sleep(1300);Assert.assertTrue(NativeInterface.blackHoleActive());
   scene.onActivity(a->Assert.assertTrue("actual MediaPlayer decoding dark vortex",sound[0].hole.playing()));
   int[] first=NativeInterface.audioSnapshot(0,false);int[] pos={0};scene.onActivity(a->pos[0]=sound[0].hole.playbackPosition());
   Assert.assertTrue("cue follows simulation offset",Math.abs(pos[0]-(first[1]-first[2]))<700);
   scene.onActivity(a->sound[0].toggle());Thread.sleep(200);scene.onActivity(a->Assert.assertFalse("mute stops long sound",sound[0].hole.playing()));
   scene.onActivity(a->sound[0].toggle());Thread.sleep(500);scene.onActivity(a->Assert.assertTrue("unmute resumes current hole",sound[0].hole.playing()));
   scene.moveToState(Lifecycle.State.CREATED);Thread.sleep(250);Assert.assertFalse(sound[0].hole.playing());
   scene.moveToState(Lifecycle.State.RESUMED);Thread.sleep(700);scene.onActivity(a->Assert.assertTrue("foreground resumes",sound[0].hole.playing()));
   new Classic193VisualTest().shot("audio206-playing");
   end=SystemClock.uptimeMillis()+16000;while(NativeInterface.blackHoleActive()&&SystemClock.uptimeMillis()<end)Thread.sleep(100);
   Assert.assertFalse("hole really expires",NativeInterface.blackHoleActive());Thread.sleep(850);
   scene.onActivity(a->Assert.assertFalse("tail ends without loop",sound[0].hole.playing()));
   Assert.assertEquals(0,sound[0].hole.errors);Assert.assertEquals(0,sound[0].playErrors);
   JSONObject report=new JSONObject().put("status","PASS").put("mode","classic real contact and touch cast").put("loaded",sound[0].loaded).put("swallow_streams",sound[0].played[AudioEvents.SWALLOW]).put("hole_starts",sound[0].hole.starts).put("hole_seeks",sound[0].hole.seeks).put("hole_errors",sound[0].hole.errors).put("mute_background_expiry",true).put("human_listening",false);
   try(FileWriter out=new FileWriter(new File(c.getExternalFilesDir(null),"audio211-device.json"))){out.write(report.toString(2));}
   scene.onActivity(a->{if(sound[0].enabled()!=original[0])sound[0].toggle();});
  }finally{c.getSharedPreferences("liquid_audio",0).edit().putBoolean("enabled",original[0]).commit();}
  Assert.assertFalse("leaving game releases long stream",sound[0].hole.playing());
 }
}
