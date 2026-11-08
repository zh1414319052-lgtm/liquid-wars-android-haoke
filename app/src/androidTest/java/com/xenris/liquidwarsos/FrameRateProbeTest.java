// SPDX-License-Identifier: GPL-3.0-or-later
package com.xenris.liquidwarsos;
import android.content.*;import android.graphics.*;import android.os.*;import android.view.*;import android.view.accessibility.*;
import androidx.lifecycle.Lifecycle;import androidx.test.core.app.ActivityScenario;import androidx.test.ext.junit.runners.AndroidJUnit4;import androidx.test.platform.app.InstrumentationRegistry;
import com.xenris.liquidwarsos.activities.GameActivity;import org.junit.*;import org.junit.runner.RunWith;import org.json.*;import java.io.*;

@RunWith(AndroidJUnit4.class)
public class FrameRateProbeTest {
 static void picture(Context c,String name)throws Exception{Bitmap b=InstrumentationRegistry.getInstrumentation().getUiAutomation().takeScreenshot();try(OutputStream out=c.openFileOutput(name,0)){b.compress(Bitmap.CompressFormat.JPEG,90,out);}b.recycle();}
 static void tap(float x,float y){long t=SystemClock.uptimeMillis();for(int action:new int[]{MotionEvent.ACTION_DOWN,MotionEvent.ACTION_UP}){MotionEvent e=MotionEvent.obtain(t,SystemClock.uptimeMillis(),action,x,y,0);e.setSource(InputDevice.SOURCE_TOUCHSCREEN);Assert.assertTrue(InstrumentationRegistry.getInstrumentation().getUiAutomation().injectInputEvent(e,true));e.recycle();}}
 static void textTap(String text)throws Exception{long end=SystemClock.uptimeMillis()+2500;while(SystemClock.uptimeMillis()<end){AccessibilityNodeInfo root=InstrumentationRegistry.getInstrumentation().getUiAutomation().getRootInActiveWindow();if(root!=null){for(AccessibilityNodeInfo node:root.findAccessibilityNodeInfosByText(text))if(text.equals(String.valueOf(node.getText()))){Rect r=new Rect();node.getBoundsInScreen(r);tap(r.centerX(),r.centerY());return;}}SystemClock.sleep(30);}throw new AssertionError("Visible control not found: "+text);}
 @Test public void chooseRatesAndRestoreSurface()throws Exception{
  Context c=InstrumentationRegistry.getInstrumentation().getTargetContext();int old=DisplayFrameRate.preference(c);StaticBits.init();StaticBits.map=28;StaticBits.seed=16828;StaticBits.team=2;StaticBits.dotsPerTeam=1000;StaticBits.timeLimit=240;StaticBits.aiDifficulty=AiDifficulty.HARD;StaticBits.alliances=AllianceRules.solo();
  c.getSharedPreferences(DisplayFrameRate.PREFS,0).edit().putInt(DisplayFrameRate.KEY,0).commit();Intent intent=new Intent(c,GameActivity.class).putExtra("qa_model_teams",0).putExtra("spectator",true).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
  try(ActivityScenario<GameActivity> scene=ActivityScenario.launch(intent)){
   MyGLSurfaceView[] gl={null};ItemOverlay[] hud={null};Rect b=new Rect();scene.onActivity(a->{gl[0]=a.findViewById(R.id.mySurfaceView);hud[0]=a.findViewById(R.id.item_overlay);hud[0].getGlobalVisibleRect(b);});
   long deadline=SystemClock.uptimeMillis()+15000;while(NativeInterface.playTimeMillis()==0&&SystemClock.uptimeMillis()<deadline)SystemClock.sleep(30);Assert.assertTrue(NativeInterface.playTimeMillis()>0);Assert.assertTrue(gl[0].pacingActive());float dp=c.getResources().getDisplayMetrics().density;
   tap(b.right-145*dp,b.top+26*dp);SystemClock.sleep(180);Assert.assertFalse("Dialog removes focus and stops vsync",gl[0].pacingActive());int paused=NativeInterface.playTimeMillis();SystemClock.sleep(150);Assert.assertEquals(paused,NativeInterface.playTimeMillis());picture(c,"perf168-settings.jpg");
   textTap("60 FPS");SystemClock.sleep(180);Assert.assertEquals(60,DisplayFrameRate.preference(c));Assert.assertEquals(60,gl[0].targetHz(),.5f);Assert.assertTrue(gl[0].pacingActive());long start=gl[0].framesDrawn();SystemClock.sleep(600);long sixty=gl[0].framesDrawn()-start;Assert.assertTrue(sixty>0);picture(c,"perf168-60.jpg");
   tap(b.right-145*dp,b.top+26*dp);SystemClock.sleep(100);textTap("自动 · 优先高刷");SystemClock.sleep(180);Assert.assertEquals(0,DisplayFrameRate.preference(c));Assert.assertTrue(gl[0].targetHz()>=60);start=gl[0].framesDrawn();SystemClock.sleep(600);long auto=gl[0].framesDrawn()-start;Assert.assertTrue(auto>0);picture(c,"perf168-auto.jpg");
   scene.moveToState(Lifecycle.State.CREATED);SystemClock.sleep(180);Assert.assertFalse(gl[0].pacingActive());long background=gl[0].framesDrawn();paused=NativeInterface.playTimeMillis();SystemClock.sleep(250);Assert.assertEquals(background,gl[0].framesDrawn());Assert.assertEquals(paused,NativeInterface.playTimeMillis());
   scene.moveToState(Lifecycle.State.RESUMED);long resumeDeadline=SystemClock.uptimeMillis()+4000;
   boolean[] active={false};while(SystemClock.uptimeMillis()<resumeDeadline){scene.onActivity(a->active[0]=gl[0].pacingActive());if(active[0]&&gl[0].framesDrawn()>background&&NativeInterface.playTimeMillis()>paused)break;SystemClock.sleep(40);}
   picture(c,"perf168-resumed.jpg");Assert.assertTrue("Vsync resumes with focused visible surface",active[0]);Assert.assertTrue("GL redraw after EGL recreation",gl[0].framesDrawn()>background);Assert.assertTrue("Game clock resumes",NativeInterface.playTimeMillis()>paused);
   JSONObject report=new JSONObject().put("status","PASS").put("version","1.68-QA").put("real_touch",true).put("frame_options",true).put("target_60",true).put("target_auto_hz",gl[0].targetHz()).put("display_hz",gl[0].getDisplay().getRefreshRate()).put("render_completions_in_600ms_60",sixty).put("render_completions_in_600ms_auto",auto).put("render_completions_not_presentation_fps",true).put("dialog_pauses_solo",true).put("background_stops_callbacks",true).put("egl_resume",true).put("models",0).put("human_play",false).put("physical_phone",false);
   try(Writer out=new OutputStreamWriter(c.openFileOutput("perf168-lifecycle.json",0),"UTF-8")){out.write(report.toString(2));}
  }finally{c.getSharedPreferences(DisplayFrameRate.PREFS,0).edit().putInt(DisplayFrameRate.KEY,old).commit();}
 }
}
