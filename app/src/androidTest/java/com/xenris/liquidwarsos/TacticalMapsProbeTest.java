// SPDX-License-Identifier: GPL-3.0-or-later
package com.xenris.liquidwarsos;
import android.content.*;import android.os.*;import android.graphics.Bitmap;
import androidx.test.core.app.ActivityScenario;import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.*;import org.junit.runner.RunWith;import org.json.*;
import java.io.*;import java.lang.reflect.*;
import com.xenris.liquidwarsos.activities.GameActivity;
import com.xenris.liquidwarsos.cloud.ModelMatch;

/** Explicit MuMu smoke: human red protects the existing configured red API. */
@RunWith(AndroidJUnit4.class) public class TacticalMapsProbeTest {
 private Object field(Object o,String name)throws Exception{Field f=o.getClass().getDeclaredField(name);f.setAccessible(true);return f.get(o);}
 @Test public void mapsAndRandomSpawn()throws Exception{
  Context c=InstrumentationRegistry.getInstrumentation().getTargetContext();JSONArray report=new JSONArray();
  for(int trial=0;trial<4;trial++){
   int map=new int[]{0,23,49,0}[trial],seed=trial==3?922:921;
   StaticBits.init();StaticBits.team=2;StaticBits.map=map;StaticBits.seed=seed;StaticBits.dotsPerTeam=1000;StaticBits.timeLimit=120;StaticBits.aiDifficulty=AiDifficulty.HARD;StaticBits.alliances=AllianceRules.solo();
   Intent intent=new Intent(c,GameActivity.class).putExtra("qa_model_teams",0).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
   try(ActivityScenario<GameActivity> scenario=ActivityScenario.launch(intent)){
    final GameActivity[] a={null};scenario.onActivity(v->a[0]=v);
    ModelMatch match=(ModelMatch)field(field(a[0],"models"),"match");Assert.assertEquals("Only configured red API must be excluded by human ownership",0,match.configuredMask());
    short[][] xs=(short[][])field(a[0],"xs"),ys=(short[][])field(a[0],"ys");
    JSONObject item=new JSONObject().put("map",map).put("seed",seed).put("humanRedStart",new JSONArray().put(xs[2][0]).put(ys[2][0]));
    JSONArray samples=new JSONArray();int seconds=trial==3?3:20;
    for(int elapsed=0;elapsed<=seconds;elapsed++){
     if(elapsed==1||elapsed==seconds){
      Bitmap screenshot=InstrumentationRegistry.getInstrumentation().getUiAutomation().takeScreenshot();Assert.assertNotNull(screenshot);
      try(OutputStream out=c.openFileOutput("tactical-"+map+"-"+seed+"-"+elapsed+".png",Context.MODE_PRIVATE)){screenshot.compress(Bitmap.CompressFormat.PNG,100,out);}screenshot.recycle();
      JSONArray scores=new JSONArray();int total=0;for(int p=0;p<6;p++){int score=NativeInterface.teamScore(p);total+=score;scores.put(score);}Assert.assertEquals(6000,total);
      samples.put(new JSONObject().put("second",elapsed).put("scores",scores));
     }
     SystemClock.sleep(1000);
    }
    item.put("samples",samples);report.put(item);
   }
  }
  Assert.assertNotEquals("Same map newly seeded match has different red spawn",report.getJSONObject(0).getJSONArray("humanRedStart").toString(),report.getJSONObject(3).getJSONArray("humanRedStart").toString());
  try(Writer out=new OutputStreamWriter(c.openFileOutput("tactical-smoke.json",Context.MODE_PRIVATE),"UTF-8")){out.write(report.toString(2));}
 }
}
