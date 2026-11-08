// SPDX-License-Identifier: GPL-3.0-or-later
package com.xenris.liquidwarsos;
import android.content.*;import android.os.*;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.*;import org.junit.runner.RunWith;
import org.json.*;import java.io.*;import java.lang.reflect.*;
import com.xenris.liquidwarsos.activities.GameActivity;
import com.xenris.liquidwarsos.cloud.*;
/** Explicitly invoked MuMu probe. No config/key reads, no saved-settings writes, no rule changes. */
@RunWith(AndroidJUnit4.class) public class ModelControlProbeTest {
 private static Object field(Object o,String name)throws Exception{Field f=o.getClass().getDeclaredField(name);f.setAccessible(true);return f.get(o);}
 @Test public void observeConfiguredModel()throws Exception {
  Bundle args=InstrumentationRegistry.getArguments();int seed=Integer.parseInt(args.getString("seed","921")),map=Integer.parseInt(args.getString("map","0")),seconds=Integer.parseInt(args.getString("seconds","120"));String label=args.getString("label","probe");Assert.assertTrue(label.matches("[A-Za-z0-9_-]{1,64}"));
  int difficulty=AiDifficulty.valid(Integer.parseInt(args.getString("difficulty","1")));boolean dense="1".equals(args.getString("metrics","0"));
  boolean paired="1".equals(args.getString("paired","0"));
  Context context=InstrumentationRegistry.getInstrumentation().getTargetContext();StaticBits.init();StaticBits.seed=seed;StaticBits.map=map;StaticBits.dotsPerTeam=1000;StaticBits.timeLimit=seconds;StaticBits.aiDifficulty=difficulty;StaticBits.alliances=paired?AllianceRules.pairings().get(0):AllianceRules.solo();
  Intent intent=new Intent(context,GameActivity.class).putExtra("spectator",true).putExtra("qa_model_teams",4).putExtra("qa_paid_budget",true).putExtra("qa_budget_ceiling_micros",Long.parseLong(args.getString("budgetCeiling","9000000"))).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
  try(ActivityScenario<GameActivity> scenario=ActivityScenario.launch(intent);BufferedWriter writer=new BufferedWriter(new OutputStreamWriter(context.openFileOutput("lw-"+label+".jsonl",Context.MODE_PRIVATE),"UTF-8"))){
   final GameActivity[] activity={null};scenario.onActivity(a->activity[0]=a);ModelMatch match=(ModelMatch)field(field(activity[0],"models"),"match");long start=SystemClock.elapsedRealtime();
   Assert.assertEquals("QA requires red model only; no stored config changes",4,match.configuredMask());
   int stallAt=Integer.parseInt(args.getString("stallAt","-1")),stallFor=3;boolean stalled=false,resumed=false;long lastRequest=-1,lastAccepted=-1;
   writer.write(new JSONObject().put("event","start").put("seed",seed).put("map",map).put("difficulty",difficulty).put("paired",paired).put("alliances",new JSONArray(StaticBits.alliances)).put("modelMask",match.configuredMask()).put("particles",1000).put("seconds",seconds).put("model",match.summary(false)).toString());writer.newLine();
   while(SystemClock.elapsedRealtime()-start<(seconds+2)*1000L+MatchIntro.DURATION_MS){
    long elapsed=SystemClock.elapsedRealtime()-start;
    if(stallAt>=0&&!stalled&&elapsed>=stallAt*1000L){match.pause(true);stalled=true;writer.write(new JSONObject().put("event","model_pause").put("ms",elapsed).toString());writer.newLine();}
    if(stalled&&!resumed&&elapsed>=(stallAt+stallFor)*1000L){match.pause(false);resumed=true;writer.write(new JSONObject().put("event","model_resume").put("ms",elapsed).toString());writer.newLine();}
    JSONObject row=new JSONObject().put("ms",elapsed).put("warming",!(Boolean)field(activity[0],"introDone"));
    synchronized(match){row.put("mask",match.inputs().mask);for(String name:new String[]{"count","accepted","expired","failed","frames","responseMs","submittedMs","expires","sampled","status"})row.put(name,new JSONArray(field(match,name)));int[] snap=(int[])field(match,"previous");if(snap!=null)row.put("snapshot",new JSONArray(java.util.Arrays.copyOf(snap,Math.min(120,snap.length))));JSONArray actions=new JSONArray();for(ModelAction a:(ModelAction[])field(match,"actions"))actions.put(a==null?JSONObject.NULL:new JSONObject().put("points",new JSONArray(a.points())).put("hold_ms",a.holdMs));row.put("actions",actions);for(String metric:new String[]{"headersMs","firstTextMs","actionMs"})try{row.put(metric,new JSONArray(field(match,metric)));}catch(NoSuchFieldException ignored){}}
    short[][] xs=(short[][])field(activity[0],"xs"),ys=(short[][])field(activity[0],"ys");synchronized(xs){row.put("input_x",new JSONArray(xs)).put("input_y",new JSONArray(ys));}
    if(dense){JSONArray scores=new JSONArray();for(int p=0;p<6;p++)scores.put(NativeInterface.teamScore(p));row.put("actualScores",scores);JSONArray decisions=new JSONArray();for(ModelAction action:(ModelAction[])field(match,"actions"))decisions.put(action==null?JSONObject.NULL:new JSONObject().put("enemy",action.enemy).put("intent",action.intent));row.put("decisions",decisions);}
    if(dense){row.put("live",match.liveDiagnostics());long sent=((long[])field(match,"count"))[2],valid=((long[])field(match,"accepted"))[2];if(sent!=lastRequest||valid!=lastAccepted){row.put("strategy",match.diagnostics());lastRequest=sent;lastAccepted=valid;}}
    if(stalled&&!resumed&&elapsed>stallAt*1000L+400){Assert.assertEquals(-2,xs[2][0]);Assert.assertEquals(-2,ys[2][0]);Assert.assertEquals(0,row.getInt("mask")&4);}
    writer.write(row.toString());writer.newLine();writer.flush();SystemClock.sleep(100);
   }
   JSONArray scores=new JSONArray();for(int p=0;p<6;p++)scores.put(NativeInterface.teamScore(p));writer.write(new JSONObject().put("event","end").put("scores",scores).put("summary",match.summary(false)).toString());writer.newLine();writer.flush();
   if(stalled){Assert.assertTrue("resume executed",resumed);Assert.assertTrue("fresh command after resume",((long[])field(match,"count"))[2]>1);}
   System.out.println("Probe complete: files/lw-"+label+".jsonl; credentials untouched.");
  }
 }
}
