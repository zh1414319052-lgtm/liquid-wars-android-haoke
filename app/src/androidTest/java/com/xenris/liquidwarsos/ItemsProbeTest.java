// SPDX-License-Identifier: GPL-3.0-or-later
package com.xenris.liquidwarsos;
import android.app.AlertDialog;
import android.content.*;
import android.graphics.*;
import android.os.*;
import android.view.*;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import com.xenris.liquidwarsos.activities.GameActivity;
import com.xenris.liquidwarsos.cloud.ModelMatch;
import java.io.*;
import java.lang.reflect.Field;
import java.util.Arrays;
import org.json.*;
import org.junit.*;
import org.junit.runner.RunWith;

/** Real native rules, GLES and touch dispatch. Controlled stepping fixture,
 * not a human match or performance benchmark. All paid lanes masked off. */
@RunWith(AndroidJUnit4.class) public class ItemsProbeTest {
 static Object get(Object o,String n)throws Exception{Field f=o.getClass().getDeclaredField(n);f.setAccessible(true);return f.get(o);}
 static void set(Object o,String n,Object v)throws Exception{Field f=o.getClass().getDeclaredField(n);f.setAccessible(true);f.set(o,v);}
 static void picture(Context c,String name)throws Exception{Bitmap b=InstrumentationRegistry.getInstrumentation().getUiAutomation().takeScreenshot();Assert.assertNotNull(b);try(OutputStream out=c.openFileOutput(name,0)){b.compress(Bitmap.CompressFormat.JPEG,92,out);}b.recycle();}
 static void aim(ActivityScenario<GameActivity> scenario,GameActivity a,int x,int y,int team)throws Exception{
  Rect bounds=new Rect();scenario.onActivity(v->v.findViewById(R.id.mySurfaceView).getGlobalVisibleRect(bounds));
  float px=bounds.left+(x+.5f)*bounds.width()/800f,py=bounds.top+(479-y+.5f)*bounds.height()/480f;long now=SystemClock.uptimeMillis();
  MotionEvent down=MotionEvent.obtain(now,now,MotionEvent.ACTION_DOWN,px,py,0),up=MotionEvent.obtain(now,now+35,MotionEvent.ACTION_UP,px,py,0);down.setSource(InputDevice.SOURCE_TOUCHSCREEN);up.setSource(InputDevice.SOURCE_TOUCHSCREEN);
  Assert.assertTrue(InstrumentationRegistry.getInstrumentation().getUiAutomation().injectInputEvent(down,true));Assert.assertTrue(InstrumentationRegistry.getInstrumentation().getUiAutomation().injectInputEvent(up,true));down.recycle();up.recycle();
  short[][] xs=(short[][])get(a,"xs"),ys=(short[][])get(a,"ys");synchronized(xs){Assert.assertEquals(x,xs[team][0]);Assert.assertEquals(y,ys[team][0]);NativeInterface.setPlayerPosition(team,xs[team],ys[team]);}
 }

 static final int[] SEEDS={106,102,100,101,137,125,101,116,126,125,108,137,103,101,110,104,117,100},TEAMS={1,0,5,1,1,3,1,5,3,2,0,1,1,5,1,3,0,3};
 static void tap(float x,float y)throws Exception{long t=SystemClock.uptimeMillis();for(int action:new int[]{MotionEvent.ACTION_DOWN,MotionEvent.ACTION_UP}){MotionEvent e=MotionEvent.obtain(t,SystemClock.uptimeMillis(),action,x,y,0);e.setSource(InputDevice.SOURCE_TOUCHSCREEN);Assert.assertTrue(InstrumentationRegistry.getInstrumentation().getUiAutomation().injectInputEvent(e,true));e.recycle();}SystemClock.sleep(150);}
 static void apply(GameActivity a,int team)throws Exception{short[][] xs=(short[][])get(a,"xs"),ys=(short[][])get(a,"ys");synchronized(xs){NativeInterface.setPlayerPosition(team,xs[team],ys[team]);}}
 @Test public void userItemsTargetsSticksAndEffects()throws Exception{
  String[] kinds=InstrumentationRegistry.getArguments().getString("kinds","2,3,4,5,6,7,8,11,16,17").split(",");Context c=InstrumentationRegistry.getInstrumentation().getTargetContext();JSONArray reports=new JSONArray();
  for(String value:kinds){int kind=Integer.parseInt(value),team=TEAMS[kind];StaticBits.init();StaticBits.map=1;StaticBits.seed=SEEDS[kind];StaticBits.team=team;StaticBits.dotsPerTeam=100;StaticBits.timeLimit=240;StaticBits.alliances=AllianceRules.solo();Intent intent=new Intent(c,GameActivity.class).putExtra("qa_model_teams",0).putExtra("qa_ruins_ready",true).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
   try(ActivityScenario<GameActivity> scenario=ActivityScenario.launch(intent)){
    GameActivity[] a={null};scenario.onActivity(v->a[0]=v);SystemClock.sleep(450);ModelMatch m=(ModelMatch)get(get(a[0],"models"),"match");Assert.assertEquals(0,m.configuredMask());
    scenario.onActivity(v->{try{set(v,"resumed",false);((AlertDialog)get(v,"dialog")).dismiss();v.findViewById(R.id.match_intro).setVisibility(View.GONE);}catch(Exception e){throw new RuntimeException(e);}});
    short[] idle={-2,-1,-1,-1,-1};for(int t=0;t<6;t++)NativeInterface.setPlayerPosition(t,idle,idle);
    int[] before=NativeInterface.itemSnapshot();SystemClock.sleep(120);Assert.assertArrayEquals(before,NativeInterface.itemSnapshot());
    if(kind==2){Rect bounds=new Rect();scenario.onActivity(v->v.findViewById(R.id.item_overlay).getGlobalVisibleRect(bounds));float dp=c.getResources().getDisplayMetrics().density;for(int side=0;side<2;side++){float x=bounds.left+(side==0?76*dp:bounds.width()-76*dp),y=bounds.bottom-66*dp;long t=SystemClock.uptimeMillis();for(int action:new int[]{MotionEvent.ACTION_DOWN,MotionEvent.ACTION_MOVE,MotionEvent.ACTION_UP}){MotionEvent e=MotionEvent.obtain(t,SystemClock.uptimeMillis(),action,x+(action==MotionEvent.ACTION_DOWN?0:45*dp),y,0);e.setSource(InputDevice.SOURCE_TOUCHSCREEN);Assert.assertTrue(InstrumentationRegistry.getInstrumentation().getUiAutomation().injectInputEvent(e,true));e.recycle();SystemClock.sleep(100);}short[][] xs=(short[][])get(a[0],"xs"),ys=(short[][])get(a[0],"ys");short px=xs[team][0],py=ys[team][0];Assert.assertTrue(px>=0&&px<800&&py>=0&&py<480);SystemClock.sleep(100);Assert.assertEquals(px,xs[team][0]);Assert.assertEquals(py,ys[team][0]);}}
    for(int n=0;n<1800&&NativeInterface.itemSnapshot()[2]==0;n++)NativeInterface.stepDots();int[] pair=NativeInterface.itemSnapshot();int at=-1;for(int slot=0;slot<com.xenris.liquidwarsos.cloud.SnapshotItems.slots(pair);slot++){int offset=com.xenris.liquidwarsos.cloud.SnapshotItems.offset(pair,slot);if(pair[offset]==kind)at=offset;}Assert.assertTrue(at>=0);Assert.assertEquals(kind,pair[at]);aim(scenario,a[0],pair[at+1],pair[at+2],team);
    for(int n=0;n<1100&&NativeInterface.itemSnapshot()[7]!=kind;n++)NativeInterface.stepDots();int[] taken=NativeInterface.itemSnapshot();Assert.assertEquals(kind,taken[7]);Assert.assertEquals(team,taken[8]);
    if(kind==3||kind==7||kind==8){Assert.assertEquals(kind,taken[147+team*10]);SystemClock.sleep(100);picture(c,"world169-items-pending-"+kind+".jpg");Rect overlay=new Rect();scenario.onActivity(v->v.findViewById(R.id.item_overlay).getGlobalVisibleRect(overlay));float dp=c.getResources().getDisplayMetrics().density;tap(overlay.centerX(),overlay.bottom-48*dp);
     if(kind==3){Assert.assertEquals(kind,NativeInterface.itemSnapshot()[147+team*10]);byte[] walls=NativeInterface.modelTerrain();int tx=-1,ty=-1;for(int y=150;y<330&&tx<0;y++)for(int x=300;x<500;x++)if(walls[y*800+x]==0){tx=x;ty=y;break;}Rect b=new Rect();scenario.onActivity(v->v.findViewById(R.id.mySurfaceView).getGlobalVisibleRect(b));tap(b.left+(tx+.5f)*b.width()/800f,b.top+(479-ty+.5f)*b.height()/480f);}
     else{SystemClock.sleep(150);picture(c,"world169-items-team-picker-"+kind+".jpg");scenario.onActivity(v->{View decor=v.getWindow().getDecorView();}); // Dialog button selected through UIAutomation below.
      android.app.UiAutomation ui=InstrumentationRegistry.getInstrumentation().getUiAutomation();android.view.accessibility.AccessibilityNodeInfo root=ui.getRootInActiveWindow();java.util.ArrayList<android.view.accessibility.AccessibilityNodeInfo> queue=new java.util.ArrayList<>();queue.add(root);boolean clicked=false;for(int q=0;q<queue.size()&&!clicked;q++){android.view.accessibility.AccessibilityNodeInfo node=queue.get(q);if(node==null)continue;if(node.isClickable()&&node.getText()!=null&&node.getText().toString().contains("100")){Rect b=new Rect();node.getBoundsInScreen(b);tap(b.centerX(),b.centerY());clicked=true;break;}for(int k=0;k<node.getChildCount();k++)queue.add(node.getChild(k));}Assert.assertTrue("Actual team button found",clicked);
     }
     apply(a[0],team);NativeInterface.stepDots();Assert.assertEquals("Explicit selection consumed",0,NativeInterface.itemSnapshot()[147+team*10]);
    }
    if(kind==4){Assert.assertEquals(1,taken[146]);Assert.assertEquals(0,taken[0]);Assert.assertEquals(600,taken[3]);}
    NativeInterface.setPlayerPosition(team,idle,idle);for(int n=0;n<28&&kind!=4;n++)NativeInterface.stepDots();SystemClock.sleep(200);picture(c,"world169-items-effect-"+kind+".jpg");int[] frozen=NativeInterface.itemSnapshot();SystemClock.sleep(100);Assert.assertArrayEquals("Pause keeps rule/effect time stable",frozen,NativeInterface.itemSnapshot());
    if(kind==2){for(int t=0;t<6;t++)if(t!=team)Assert.assertTrue(frozen[25+t*20+2]>9000);}
    if(kind==6){Assert.assertTrue(frozen[25+team*20+6]>19000);}
    if(kind==3){scenario.onActivity(v->{MyGLSurfaceView gl=v.findViewById(R.id.mySurfaceView);gl.onPause();gl.onResume();});SystemClock.sleep(200);Assert.assertArrayEquals(frozen,NativeInterface.itemSnapshot());}
    if(kind==17){scenario.onActivity(v->((ItemOverlay)v.findViewById(R.id.item_overlay)).showGuide());SystemClock.sleep(150);picture(c,"world169-items-guide.jpg");}
    if("1".equals(InstrumentationRegistry.getArguments().getString("record_demo","0"))){long end=SystemClock.elapsedRealtime()+1800;while(SystemClock.elapsedRealtime()<end){long began=SystemClock.elapsedRealtime();NativeInterface.stepDots();SystemClock.sleep(Math.max(1,7-(SystemClock.elapsedRealtime()-began)));}}
    JSONObject row=new JSONObject().put("kind",kind).put("seed",SEEDS[kind]).put("team",team).put("models",m.configuredMask()).put("real_touch",true).put("controlled_native_steps",true).put("after",new JSONArray(frozen));reports.put(row);
   }
  }
  try(Writer out=new OutputStreamWriter(c.openFileOutput("world169-items-report.json",0),"UTF-8")){out.write(reports.toString(2));}
 }
}
