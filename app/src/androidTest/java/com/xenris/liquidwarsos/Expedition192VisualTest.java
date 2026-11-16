package com.xenris.liquidwarsos;
import android.app.*;import android.content.*;import android.os.*;import android.view.*;import android.view.accessibility.*;
import androidx.test.platform.app.InstrumentationRegistry;import androidx.test.ext.junit.runners.AndroidJUnit4;
import org.junit.*;import org.junit.runner.RunWith;import org.json.*;import java.io.*;import java.util.*;import java.util.concurrent.*;
import okhttp3.*;import okio.ByteString;import com.xenris.liquidwarsos.online.*;

/** Separate Android processes: test-only native host + real Godot guest, loopback relay.
 * Inventory and relocation are explicit fixtures, not a normal random-start demonstration.
 */
@RunWith(AndroidJUnit4.class)
public class Expedition192VisualTest {
 static {System.loadLibrary("nativeinterface");System.loadLibrary("expedition192probe");}
 public static native int[] fixture(int mode);
 final Instrumentation inst=InstrumentationRegistry.getInstrumentation();final Context ctx=inst.getTargetContext();
 volatile boolean live,joined;volatile WebSocket socket;Thread simulation;String code;PlatformApi host,guest;int[] place;int packets,commands;Throwable failure;
 interface Condition{boolean yes()throws Exception;}
 void until(String label,Condition c,long timeout)throws Exception{long end=SystemClock.uptimeMillis()+timeout;while(SystemClock.uptimeMillis()<end){if(c.yes())return;Thread.sleep(40);}Assert.fail(label);}
 void pause(long ms)throws Exception{Thread.sleep(ms);}
 void status(String s){Bundle b=new Bundle();b.putString("stream","\nVISUAL192 "+s+"\n");inst.sendStatus(0,b);}
 PlatformApi login(String who)throws Exception{PlatformApi p=new PlatformApi(ctx);Assert.assertEquals("http://127.0.0.1:19280",p.origin);JSONObject c=p.call("POST","/auth/code",PlatformApi.object("email",who+"@example.com"));String id=c.getString("challenge_id");String otp=p.call("GET","/dev/mailbox?challenge_id="+id,null).getString("code");p.save(p.call("POST","/auth/verify",PlatformApi.object("challenge_id",id,"code",otp,"client","native")).getString("access_token"));return p;}
 int[] facts(){int[] w=NativeInterface.expeditionWire(1);return Arrays.copyOfRange(w,13+w[9],13+w[9]+w[10]);}
 int at(){return 11+43;}
 void tap(float x,float y){long d=SystemClock.uptimeMillis();for(int a:new int[]{0,1}){MotionEvent e=MotionEvent.obtain(d,SystemClock.uptimeMillis(),a,x,y,0);inst.sendPointerSync(e);e.recycle();}}
 void slot(int k){float u=2340f/960f,h=1080/u;tap((960-222+k%4*53+24)*u,(h-130+k/4*61+25)*u);}
 JSONObject observed()throws Exception{return new JSONObject(new String(java.nio.file.Files.readAllBytes(new File(ctx.getCacheDir(),"expedition192-observation.json").toPath()),java.nio.charset.StandardCharsets.UTF_8));}
 void world(int x,int y)throws Exception{JSONObject j=observed();JSONArray c=j.getJSONArray("camera"),v=j.getJSONArray("viewport");float px=(float)((x-c.getDouble(0))/c.getDouble(2)*v.getInt(2));float py=(float)((c.getDouble(1)+c.getDouble(3)-1-y)/c.getDouble(3)*v.getInt(3));Assert.assertTrue("aim on usable battlefield",px>80&&px<1800&&py>150&&py<830);tap(px,py);}
 void shot(String name)throws Exception{android.graphics.Bitmap b=inst.getUiAutomation().takeScreenshot();Assert.assertNotNull(b);try(FileOutputStream out=new FileOutputStream(new File(ctx.getExternalFilesDir(null),"visual192-"+name+".png"))){b.compress(android.graphics.Bitmap.CompressFormat.PNG,100,out);}b.recycle();}
 boolean clickText(String text){AccessibilityNodeInfo root=inst.getUiAutomation().getRootInActiveWindow();if(root==null)return false;for(AccessibilityNodeInfo n:root.findAccessibilityNodeInfosByText(text)){android.graphics.Rect r=new android.graphics.Rect();n.getBoundsInScreen(r);if(!r.isEmpty()){tap(r.centerX(),r.centerY());return true;}}return false;}
 @Test public void actualGuestAndItems()throws Exception{
  Assert.assertTrue(BuildConfig.DEBUG&&ctx.getPackageName().endsWith("garden186"));
  boolean load="yes".equals(InstrumentationRegistry.getArguments().getString("load"));int seats=load?10:2;
  host=login("qa192-visual-host");JSONObject room=host.call("POST","/expedition/rooms",PlatformApi.object("protocol",192,"build",62,"seats",seats)).getJSONObject("room");code=room.getString("code");
  guest=login("qa192-visual-guest");guest.call("POST","/expedition/rooms/"+code+"/join",PlatformApi.object("protocol",192,"build",62,"seats",seats));
  for(PlatformApi p:new PlatformApi[]{host,guest})p.call("POST","/expedition/rooms/"+code+"/ready",PlatformApi.object("ready",true));host.call("POST","/expedition/rooms/"+code+"/start",null);
  NativeInterface.init(ctx.getAssets());Assert.assertTrue(NativeInterface.createExpeditionGame(0,seats,room.getInt("seed")));place=fixture(load?1:0);Assert.assertNotNull(place);status("fixture "+Arrays.toString(place));
  JSONObject ticket=host.call("POST","/expedition/rooms/"+code+"/ticket",null);
  socket=PlatformApi.HTTP.newWebSocket(new Request.Builder().url("ws://127.0.0.1:19280/ws/expedition?ticket="+ticket.getString("ticket")).build(),new WebSocketListener(){
   @Override public void onMessage(WebSocket w,String text){try{JSONObject m=new JSONObject(text);String t=m.optString("type");if(t.equals("joined")){joined=true;w.send("{\"type\":\"started\"}");}if(t.equals("command")){short[] x=new short[5],y=new short[5];for(int i=0;i<5;i++){x[i]=(short)m.getJSONArray("xs").getInt(i);y[i]=(short)m.getJSONArray("ys").getInt(i);}NativeInterface.expeditionRemoteCommand(1,m.getInt("kind"),m.getInt("group"),m.getInt("x"),m.getInt("y"),x,y);commands++;}}catch(Exception e){failure=e;}}
   @Override public void onFailure(WebSocket w,Throwable e,Response r){failure=e;}
  });
  live=true;simulation=new Thread(()->{long step=SystemClock.uptimeMillis(),send=step,ping=step;try{while(live){long now=SystemClock.uptimeMillis();int n=0;while(now>=step&&n++<8){NativeInterface.stepDots();step+=7;}if(joined&&now>=send){byte[] compressed=ExpeditionNetwork.encode(NativeInterface.expeditionWire(1));byte[] data=new byte[compressed.length+1];data[0]=1;System.arraycopy(compressed,0,data,1,compressed.length);socket.send(ByteString.of(data));packets++;send=now+70;}if(now>=ping){socket.send("{\"type\":\"ping\"}");ping=now+3000;}Thread.sleep(1);}}catch(Throwable e){failure=e;}},"Isolated real C++ host");simulation.start();
  try(FileWriter out=new FileWriter(new File(ctx.getCacheDir(),"expedition192-observe"))){out.write("1");}
  ctx.startActivity(new Intent().setClassName(ctx.getPackageName(),"com.xenris.liquidwarsos.activities.GodotGardenActivity").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK).putExtra("garden3d",true).putExtra("expedition_seats",seats).putExtra("exp_room",code).putExtra("exp_host",false).putExtra("exp_slot",1).putExtra("exp_seed",room.getInt("seed")));
  if(load){until("real 20k load guest ready",()->{try{return observed().getJSONArray("exp").getInt(4)==10&&Math.abs(observed().getJSONArray("camera").getDouble(0)+200-place[0])<35;}catch(Exception e){return false;}},30000);pause(6000);Assert.assertEquals(20000,NativeInterface.totalParticles());shot("load-20000");status("LOAD_READY_20000");pause(85000);Assert.assertNull(failure);try(FileWriter out=new FileWriter(new File(ctx.getExternalFilesDir(null),"visual192-load.json"))){out.write(new JSONObject().put("result","PASS").put("real_units",NativeInterface.totalParticles()).put("host_packets",packets).put("sim_clock",facts()[1]).put("scope","20k real simulated-unit synthetic load, real Godot guest; one emulator, not ten phones").toString(2));}return;}
  until("real Godot guest ready",()->{try{JSONObject j=observed();return j.getJSONArray("exp").getInt(4)==2&&j.getJSONArray("exp").getInt(1)>130000&&Math.abs(j.getJSONArray("camera").getDouble(0)+200-place[0])<35;}catch(Exception e){return false;}},30000);pause(5500);status("guest rendered; fixture inventory, actual coin pickup and capture counts="+facts()[11]+","+facts()[at()]);shot("guest-ready");
  Assert.assertEquals(40,facts()[at()+4]);until("site produces actual new particles",()->facts()[at()]>1000,15000);shot("production");
  // Actual canvas shop and server command, no direct purchase call.
  tap(2040,717);pause(300);shot("shop-before");tap(880,650);until("guest purchase arrived and charged once",()->facts()[at()+5]==1&&facts()[at()+4]==0,3000);shot("shop-bought");tap(1170,830);pause(300);
  slot(6);pause(300);Assert.assertTrue("enemy selection dialog",clickText("席位 1"));until("death actual half removal",()->facts()[11]==500,3000);shot("death-half");status("death reduced 1000 to 500 via real guest command");
  NativeInterface.expeditionCommand(0,1,6,3,1,0);until("incoming death warning",()->facts()[at()+40]>facts()[1],1000);pause(100);slot(7);until("counter removes warning",()->facts()[at()+40]==0&&facts()[at()+41]>facts()[1],1200);pause(1100);Assert.assertTrue(facts()[at()]>1000);shot("life-counter");
  NativeInterface.expeditionCommand(0,2,0,3,place[0],place[1]);pause(850);slot(1);until("cleanse confirmed",()->facts()[at()+11+1]==0,2000);shot("cleanse");
  slot(2);pause(100);world(place[0]-26,place[1]);until("teleport confirmed",()->facts()[at()+9]==1,3000);pause(5500);shot("teleported");
  slot(3);pause(100);world(place[0]-26,place[1]);until("anchor confirmed",()->facts()[at()+11+3]==0,3000);shot("anchor");
  // Actual large map opens, drags, closes; it does not pause remote authority.
  int clock=facts()[1];tap(2100,320);pause(500);shot("map-expanded");tap(2155,165);Assert.assertTrue(facts()[1]>clock);
  // Eligible obstacle fortification target; return by actual movement command.
  world(place[4]-7,place[5]);pause(6000);slot(5);pause(100);world(place[4],place[5]);until("fortify confirmed",()->facts()[at()+11+5]==0,3000);shot("fortified");
  int wardEnd=facts()[at()+19+5]-25000+8000;
  until("authoritative eight-second ward expired",()->facts()[1]>wardEnd,25000);
  slot(4);until("devour confirmed",()->facts()[at()+11+4]==0,3000);
  until("real obstacle removed in filtered authoritative state",()->{int[] f=facts();int b=11+f[4]*43+f[7]*5;return f[b+7]==1;},16000);shot("devoured");
  slot(0);pause(100);world(place[2],place[3]);until("guest freeze targeting confirmed",()->facts()[at()+11]==0,3000);pause(900);shot("freeze-target");
  Assert.assertNull(failure);Assert.assertTrue(packets>150&&commands>=8);
  try(FileWriter out=new FileWriter(new File(ctx.getExternalFilesDir(null),"visual192-result.json"))){out.write(new JSONObject().put("result","PASS").put("host_packets",packets).put("actual_guest_commands",commands).put("test_scope","real native host + rendered Godot guest on one emulator; controlled 2000-unit positions/inventory; not ten phones or natural item economy").put("last_facts",new JSONArray(facts())).toString(2));}
  status("PASS actual guest/items/coin/production UI");
 }
 @After public void cleanup()throws Exception{live=false;if(simulation!=null)simulation.join(3000);if(socket!=null)socket.close(1000,"test complete");NativeInterface.destroyGame();NativeInterface.uninit();}
}
