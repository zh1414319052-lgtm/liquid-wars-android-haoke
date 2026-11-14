// SPDX-License-Identifier: GPL-3.0-or-later
package com.xenris.liquidwarsos;
import static com.xenris.liquidwarsos.World170ProbeTest.*;
import static com.xenris.liquidwarsos.Operations180ProbeTest.button;
import static com.xenris.liquidwarsos.Operations180ProbeTest.ready;
import static com.xenris.liquidwarsos.Operations180ProbeTest.screen;
import android.graphics.Rect;import android.os.SystemClock;import androidx.lifecycle.Lifecycle;import androidx.test.core.app.ActivityScenario;import androidx.test.ext.junit.runners.AndroidJUnit4;import org.junit.*;import org.junit.runner.RunWith;import org.json.*;import com.xenris.liquidwarsos.activities.GameActivity;import com.xenris.liquidwarsos.cloud.SnapshotItems;
@RunWith(AndroidJUnit4.class) public class Audio181ProbeTest {
 @Test public void pickupsAndAudioLifecycle()throws Exception{
  StaticBits.operationsMode=0;GameAudio[] sound={null};boolean[] original={true};
  try(ActivityScenario<GameActivity> s=ActivityScenario.launch(prepare(1,18101,400,true,false))){
   frozen(s);Rect gl=new Rect(),hud=new Rect();ready(s,gl,hud);s.onActivity(a->{sound[0]=((MyGLSurfaceView)a.findViewById(R.id.mySurfaceView)).audio();original[0]=sound[0].enabled();if(!original[0])sound[0].toggle();});
   long until=SystemClock.uptimeMillis()+5000;while(sound[0].loaded<8&&SystemClock.uptimeMillis()<until)SystemClock.sleep(50);Assert.assertEquals("all eight actual SoundPool decodes",8,sound[0].loaded);Assert.assertEquals(0,sound[0].loadErrors);
   short[] idle={-2,-1,-1,-1,-1};for(int t=0;t<6;t++)NativeInterface.setPlayerPosition(t,idle,idle);for(int n=0;n<2500&&SnapshotItems.count(NativeInterface.itemSnapshot())==0;n++)NativeInterface.stepDots();Assert.assertTrue("natural real pickups",SnapshotItems.count(NativeInterface.itemSnapshot())>0);SystemClock.sleep(180);picture("audio181-pickups.jpg");
   long hash=NativeInterface.stateHash();JSONArray outputs=new JSONArray();for(int cue=0;cue<8;cue++){SystemClock.sleep(1800);final int which=cue;boolean[] played={false};s.onActivity(a->played[0]=sound[0].play(which));Assert.assertTrue("native stream accepted cue "+cue,played[0]);outputs.put(new JSONObject().put("cue",AudioEvents.FILES[cue]).put("stream_accepted",played[0]));}
   float dp=context().getResources().getDisplayMetrics().density;int before=sound[0].played[AudioEvents.ITEM];tap(hud.right-216*dp,hud.top+26*dp);Assert.assertFalse("actual sound button mutes",sound[0].enabled());s.onActivity(a->Assert.assertFalse(sound[0].play(AudioEvents.ITEM)));Assert.assertEquals(before,sound[0].played[AudioEvents.ITEM]);picture("audio181-muted.jpg");tap(hud.right-216*dp,hud.top+26*dp);Assert.assertTrue(sound[0].enabled());
   Assert.assertEquals("presentation/audio does not mutate rules",hash,NativeInterface.stateHash());s.moveToState(Lifecycle.State.CREATED);s.onActivity(a->Assert.assertFalse("background cannot play",sound[0].play(AudioEvents.ITEM)));s.moveToState(Lifecycle.State.RESUMED);frozen(s);SystemClock.sleep(400);s.onActivity(a->Assert.assertTrue("foreground playback resumes",sound[0].play(AudioEvents.ITEM)));
   s.onActivity(a->{if(sound[0].enabled()!=original[0])sound[0].toggle();});write("audio181-native.json",new JSONObject().put("status","PASS").put("loaded",sound[0].loaded).put("play_errors",sound[0].playErrors).put("cues",outputs).put("mute_and_background",true).put("simulation_hash_unchanged",true).put("preferences_restored",true).put("actual_listening",false));
  }finally{StaticBits.operationsMode=0;if(sound[0]!=null&&sound[0].enabled()!=original[0]){context().getSharedPreferences("liquid_audio",0).edit().putBoolean("enabled",original[0]).commit();}}
 }
 @Test public void realCaptureAndProductionCues()throws Exception{
  android.content.Intent intent=prepare(1,18001,400,true,false);StaticBits.operationsMode=1;
  try(ActivityScenario<GameActivity> s=ActivityScenario.launch(intent)){
   frozen(s);Rect gl=new Rect(),hud=new Rect();ready(s,gl,hud);GameAudio[] sound={null};boolean[] original={true};s.onActivity(a->{sound[0]=((MyGLSurfaceView)a.findViewById(R.id.mySurfaceView)).audio();original[0]=sound[0].enabled();if(!original[0])sound[0].toggle();});SystemClock.sleep(300);
   OperationsSnapshot data=new OperationsSnapshot(NativeInterface.operationsSnapshot(StaticBits.team,false));int t=StaticBits.team,g=data.group(t,0),chosen=0;long best=Long.MAX_VALUE;for(int k=0;k<data.sites();k++){int at=data.site(k);long d=(long)(data.raw[at]-data.raw[g+1])*(data.raw[at]-data.raw[g+1])+(long)(data.raw[at+1]-data.raw[g+2])*(data.raw[at+1]-data.raw[g+2]);if(d<best){best=d;chosen=k;}}
   int at=data.site(chosen);float[] xy=screen(s,gl,data.raw[at],data.raw[at+1]);tap(xy[0],xy[1]);for(int k=0;k<100&&sound[0].played[AudioEvents.CAPTURE]==0;k++){Operations180ProbeTest.advance(s,100);SystemClock.sleep(90);}Assert.assertTrue("real captured site triggers cue",sound[0].played[AudioEvents.CAPTURE]>0);for(int k=0;k<8&&sound[0].played[AudioEvents.PRODUCE]==0;k++){Operations180ProbeTest.advance(s,100);SystemClock.sleep(220);}Assert.assertTrue("actual birth triggers production cue",sound[0].played[AudioEvents.PRODUCE]>0);Assert.assertTrue("tap uses single-droplet confirmation",sound[0].played[AudioEvents.ORDER]>0);picture("audio181-conquest.jpg");
   write("audio181-events.json",new JSONObject().put("status","PASS").put("own_capture_cues",sound[0].played[AudioEvents.CAPTURE]).put("own_production_cues",sound[0].played[AudioEvents.PRODUCE]).put("real_touch_and_core",true).put("play_errors",sound[0].playErrors));s.onActivity(a->{if(sound[0].enabled()!=original[0])sound[0].toggle();});
  }finally{StaticBits.operationsMode=0;}
 }
}
