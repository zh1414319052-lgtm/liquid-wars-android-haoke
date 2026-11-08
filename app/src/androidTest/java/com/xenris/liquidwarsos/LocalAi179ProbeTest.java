// SPDX-License-Identifier: GPL-3.0-or-later
package com.xenris.liquidwarsos;
import static com.xenris.liquidwarsos.World170ProbeTest.*;
import android.graphics.Rect;import android.os.*;import android.view.*;
import androidx.test.core.app.ActivityScenario;import androidx.test.ext.junit.runners.AndroidJUnit4;import androidx.test.platform.app.InstrumentationRegistry;
import com.xenris.liquidwarsos.activities.GameActivity;import org.junit.*;import org.junit.runner.RunWith;import org.json.*;import java.util.*;
@RunWith(AndroidJUnit4.class) public class LocalAi179ProbeTest {
 @Test public void battleSample()throws Exception {
  Bundle args=InstrumentationRegistry.getArguments();String label=args.getString("label","before");int map=Integer.parseInt(args.getString("map","1")),dots=Integer.parseInt(args.getString("dots","1000")),seed=Integer.parseInt(args.getString("seed","17901")),seconds=Integer.parseInt(args.getString("seconds","22"));
  HandlerThread metrics=new HandlerThread("AI179 frame metrics");metrics.start();List<Long> frames=Collections.synchronizedList(new ArrayList<>());Window.OnFrameMetricsAvailableListener listener=(w,m,l)->frames.add(m.getMetric(FrameMetrics.TOTAL_DURATION));
  try(ActivityScenario<GameActivity> s=ActivityScenario.launch(prepare(map,seed,dots,false,true))){
   MyGLSurfaceView[] gl={null};s.onActivity(a->{gl[0]=a.findViewById(R.id.mySurfaceView);a.getWindow().addOnFrameMetricsAvailableListener(listener,new Handler(metrics.getLooper()));});long deadline=SystemClock.uptimeMillis()+18000;while(NativeInterface.playTimeMillis()==0&&SystemClock.uptimeMillis()<deadline)SystemClock.sleep(20);Assert.assertTrue(NativeInterface.playTimeMillis()>0);
   int initial=NativeInterface.playTimeMillis();long start=SystemClock.elapsedRealtime(),glStart=gl[0].framesDrawn(),worker=PerfProbeTest.workerCpu(),glCpu=PerfProbeTest.glCpu(gl[0]);JSONArray trace=new JSONArray();frames.clear();
   while(SystemClock.elapsedRealtime()-start<seconds*1000L){int[] snap=NativeInterface.itemSnapshot();JSONArray row=new JSONArray().put(SystemClock.elapsedRealtime()-start).put(snap[0]).put(snap[3]);for(int t=0;t<6;t++)row.put(snap[13+t]);trace.put(row);SystemClock.sleep(250);}
   JSONObject plan=new JSONObject();s.onActivity(a->{try{java.lang.reflect.Field f=GameActivity.class.getDeclaredField("aiController");f.setAccessible(true);AiController ai=(AiController)f.get(a);for(String n:new String[]{"planningCalls","planningNanos","maxPlanningNanos"}){try{java.lang.reflect.Field field=AiController.class.getDeclaredField(n);field.setAccessible(true);plan.put(n,field.getLong(ai));}catch(NoSuchFieldException old){plan.put(n,"NOT_AVAILABLE_BASELINE");}}}catch(Exception e){throw new AssertionError(e);}});
   Debug.MemoryInfo mem=new Debug.MemoryInfo();Debug.getMemoryInfo(mem);int advanced=NativeInterface.playTimeMillis()-initial;Assert.assertTrue(advanced>0);picture("ai179-"+label+"-"+map+"-"+dots+".jpg");
   write("ai179-"+label+"-"+map+"-"+dots+".json",new JSONObject().put("planner",plan).put("status","PASS").put("version",context().getPackageManager().getPackageInfo(context().getPackageName(),0).versionName).put("map",map).put("seed",seed).put("dots",dots).put("difficulty","HARD").put("normal_worker",true).put("spectator",true).put("wall_ms",SystemClock.elapsedRealtime()-start).put("sim_advanced_ms",advanced).put("ui_frame_ns",PerfProbeTest.array(new ArrayList<>(frames))).put("gl_completions",gl[0].framesDrawn()-glStart).put("worker_cpu_ns",PerfProbeTest.workerCpu()-worker).put("gl_cpu_ns",PerfProbeTest.glCpu(gl[0])-glCpu).put("pss_kib",mem.getTotalPss()).put("display_hz",gl[0].getDisplay().getRefreshRate()).put("trace_columns","wall_ms,sim_ms,snapshot3,counts0..5").put("trace",trace).put("paid_requests",0).put("config_read",false).put("human_play",false));s.onActivity(a->a.getWindow().removeOnFrameMetricsAvailableListener(listener));
  }finally{metrics.quitSafely();metrics.join();}
 }
}
