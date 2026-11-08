// SPDX-License-Identifier: GPL-3.0-or-later
package com.xenris.liquidwarsos;

import android.content.*;
import android.os.*;
import android.system.*;
import android.view.*;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import com.xenris.liquidwarsos.activities.GameActivity;
import org.junit.*;
import org.junit.runner.RunWith;
import org.json.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.*;

/** Six-second steady-state probe. Surface presentation and UI frame metrics stay separate. */
@RunWith(AndroidJUnit4.class)
public class PerfProbeTest {
    static String shell(String command)throws Exception {
        try(InputStream in=new ParcelFileDescriptor.AutoCloseInputStream(InstrumentationRegistry.getInstrumentation().getUiAutomation().executeShellCommand(command)); ByteArrayOutputStream out=new ByteArrayOutputStream()){
            byte[] buf=new byte[8192];int n;while((n=in.read(buf))>=0)out.write(buf,0,n);return out.toString("UTF-8");
        }
    }
    static String quote(String text){return "'"+text.replace("'","'\\''")+"'";}
    static long glCpu(MyGLSurfaceView view)throws Exception {
        long[] value={0};CountDownLatch latch=new CountDownLatch(1);
        view.queueEvent(()->{value[0]=Debug.threadCpuTimeNanos();latch.countDown();});
        Assert.assertTrue("GL thread responds",latch.await(4,TimeUnit.SECONDS));return value[0];
    }
    static long workerCpu()throws Exception {
        for(File f:new File("/proc/self/task").listFiles()){
            String name=read(new File(f,"comm")).trim();if(!"Solo match".equals(name))continue;
            String stat=read(new File(f,"stat"));String[] fields=stat.substring(stat.lastIndexOf(')')+2).trim().split("\\s+");
            return (Long.parseLong(fields[11])+Long.parseLong(fields[12]))*1000000000L/Os.sysconf(OsConstants._SC_CLK_TCK);
        }throw new AssertionError("No Solo match worker");
    }
    static String read(File file)throws Exception {try(InputStream in=new FileInputStream(file);ByteArrayOutputStream out=new ByteArrayOutputStream()){byte[] b=new byte[4096];int n;while((n=in.read(b))>=0)out.write(b,0,n);return out.toString("UTF-8");}}
    static JSONArray array(Collection<Long> values){JSONArray a=new JSONArray();for(long v:values)a.put(v);return a;}
    @Test public void steadyPresentation()throws Exception {
        Bundle args=InstrumentationRegistry.getArguments();String phase=args.getString("phase","baseline");int dots=Integer.parseInt(args.getString("dots","1000"));
        int map=Integer.parseInt(args.getString("map","28")),seed=Integer.parseInt(args.getString("seed","16828"));
        Context c=InstrumentationRegistry.getInstrumentation().getTargetContext();StaticBits.init();StaticBits.map=map;StaticBits.seed=seed;StaticBits.team=2;StaticBits.dotsPerTeam=dots;StaticBits.timeLimit=240;StaticBits.aiDifficulty=AiDifficulty.HARD;StaticBits.alliances=AllianceRules.solo();
        // The previous interrupted settings probe may leave a selected cap. Pin this
        // benchmark's display setting explicitly, without touching any API preferences.
        if(!phase.startsWith("baseline"))Assert.assertTrue(c.getSharedPreferences("display_quality",0).edit().putInt("frame_limit",0).commit());
        Intent intent=new Intent(c,GameActivity.class).putExtra("qa_model_teams",0).putExtra("spectator",true).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        List<Long> ui=Collections.synchronizedList(new ArrayList<>());HandlerThread metrics=new HandlerThread("Perf168 UI metrics");metrics.start();
        Window.OnFrameMetricsAvailableListener listener=(window,frame,lost)->ui.add(frame.getMetric(FrameMetrics.TOTAL_DURATION));
        try(ActivityScenario<GameActivity> scene=ActivityScenario.launch(intent)){
            MyGLSurfaceView[] view={null};Window[] window={null};scene.onActivity(a->{view[0]=a.findViewById(R.id.mySurfaceView);window[0]=a.getWindow();window[0].addOnFrameMetricsAvailableListener(listener,new Handler(metrics.getLooper()));});
            long deadline=SystemClock.uptimeMillis()+15000;while(NativeInterface.playTimeMillis()==0&&SystemClock.uptimeMillis()<deadline)SystemClock.sleep(20);
            Assert.assertTrue("Countdown finished and simulation active",NativeInterface.playTimeMillis()>0);SystemClock.sleep(200);
            String all=shell("dumpsys SurfaceFlinger --list");String layer="";String raw="";
            for(String line:all.split("\n"))if(line.contains("SurfaceView")&&line.contains("liquidwarsos")&&line.contains("BLAST")){
                List<String> candidates=new ArrayList<>();candidates.add(line.trim());
                String stripped=line.replaceFirst("^RequestedLayerState\\{","").replaceFirst(" parentId=.*$","").replaceFirst("\\}$","");candidates.add(stripped);
                candidates.add(stripped.replaceFirst("#[0-9]+$",""));
                for(String name:candidates){String result=shell("dumpsys SurfaceFlinger --latency "+quote(name));if(result.trim().split("\n").length>2){layer=name;raw=result;break;}}
                if(!layer.isEmpty())break;
            }
            long gpu0=glCpu(view[0]),worker0=workerCpu(),begin=System.nanoTime();long frameCount0=rendered(view[0]);int simulation0=NativeInterface.playTimeMillis();ui.clear();
            StringBuilder samples=new StringBuilder("# "+phase+" "+dots+"\n# layer="+layer+"\n# layers\n"+all+"\n");TreeSet<Long> present=new TreeSet<>();long period=0;int polls=0;
            while(System.nanoTime()-begin<6000000000L){
                if(!layer.isEmpty())raw=shell("dumpsys SurfaceFlinger --latency "+quote(layer));
                samples.append("# poll ").append(polls++).append(' ').append(System.nanoTime()).append('\n').append(raw).append('\n');
                String[] lines=raw.trim().split("\n");try{period=Long.parseLong(lines[0].trim());}catch(Exception ignored){}
                for(int i=1;i<lines.length;i++){String[] cols=lines[i].trim().split("\\s+");if(cols.length!=3)continue;try{long actual=Long.parseLong(cols[1]);if(actual>=begin&&actual<Long.MAX_VALUE)present.add(actual);}catch(Exception ignored){}}
                SystemClock.sleep(350);
            }
            long end=System.nanoTime(),gpu1=glCpu(view[0]),worker1=workerCpu();int simulation1=NativeInterface.playTimeMillis();
            present.removeIf(t->t>end);List<Long> intervals=new ArrayList<>();long previous=0;for(long t:present){if(previous!=0)intervals.add(t-previous);previous=t;}
            Debug.MemoryInfo memory=new Debug.MemoryInfo();Debug.getMemoryInfo(memory);android.view.Display display=view[0].getDisplay();JSONArray modes=new JSONArray();for(Display.Mode mode:display.getSupportedModes())modes.put(new JSONObject().put("id",mode.getModeId()).put("width",mode.getPhysicalWidth()).put("height",mode.getPhysicalHeight()).put("hz",mode.getRefreshRate()));
            JSONObject result=new JSONObject().put("version",c.getPackageManager().getPackageInfo(c.getPackageName(),0).versionName).put("phase",phase).put("status",intervals.size()>20?"MEASURED":"NOT_TESTED_SURFACE_PRESENTATION").put("map",map).put("seed",seed).put("particles_per_team",dots).put("models",0).put("sample_wall_ns",end-begin).put("simulation_ms_start",simulation0).put("simulation_ms_end",simulation1).put("gl_thread_cpu_ns",gpu1-gpu0).put("simulation_thread_cpu_ns",worker1-worker0).put("surface_layer",layer).put("display_hz",display.getRefreshRate()).put("display_modes",modes).put("refresh_period_ns",period).put("presentation_timestamps_ns",array(present)).put("presentation_intervals_ns",array(intervals)).put("ui_total_duration_ns",array(new ArrayList<>(ui))).put("pss_kib",memory.getTotalPss()).put("human_play",false).put("physical_phone",false).put("recording_during_sample",false);
            if(frameCount0>=0)result.put("gl_render_completions",rendered(view[0])-frameCount0).put("gl_completions_are_not_present_fps",true);
            result.put("configured_frame_limit",c.getSharedPreferences("display_quality",0).getInt("frame_limit",0));
            try(Writer out=new OutputStreamWriter(c.openFileOutput("perf168-"+phase+"-"+dots+".json",0),"UTF-8")){out.write(result.toString(2));}
            try(Writer out=new OutputStreamWriter(c.openFileOutput("perf168-"+phase+"-"+dots+"-raw.txt",0),"UTF-8")){out.write(samples.toString());}
            scene.onActivity(a->window[0].removeOnFrameMetricsAvailableListener(listener));
        }finally{metrics.quitSafely();metrics.join();}
    }
    static long rendered(MyGLSurfaceView view){try{return ((Number)view.getClass().getMethod("framesDrawn").invoke(view)).longValue();}catch(Exception ignored){return -1;}}
}
