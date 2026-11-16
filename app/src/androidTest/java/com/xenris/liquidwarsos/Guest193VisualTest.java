package com.xenris.liquidwarsos;
import android.app.*;import android.content.*;import android.os.*;import android.view.*;
import androidx.test.platform.app.InstrumentationRegistry;import androidx.test.ext.junit.runners.AndroidJUnit4;
import org.junit.*;import org.junit.runner.RunWith;import org.json.*;import java.io.*;import java.util.*;
import okhttp3.*;import okio.ByteString;import com.xenris.liquidwarsos.online.*;

/** Two actual Android processes through PUBLIC HTTPS/WSS. Fixture inventory is explicit.
 * Does not represent multiple physical devices or natural resource economy. */
@RunWith(AndroidJUnit4.class)
public class Guest193VisualTest {
 static {System.loadLibrary("nativeinterface");System.loadLibrary("guest193probe");}
 public static native int[] fixture(int mode);
 final Instrumentation inst=InstrumentationRegistry.getInstrumentation();final Context ctx=inst.getTargetContext();
 volatile boolean live,joined;volatile WebSocket socket;Thread simulation;String code;PlatformApi host,guest;int[] place;int packets,commands;Throwable failure;JSONArray users=new JSONArray();float width=2340,height=1080;
 interface Condition{boolean yes()throws Exception;}
 void until(String label,Condition c,long timeout)throws Exception{long end=SystemClock.uptimeMillis()+timeout;while(SystemClock.uptimeMillis()<end){if(failure!=null)throw new AssertionError(failure);if(c.yes())return;Thread.sleep(40);}Assert.fail(label);}
 void status(String s){Bundle b=new Bundle();b.putString("stream","\nVISUAL193 "+s+"\n");inst.sendStatus(0,b);}
 PlatformApi login()throws Exception{PlatformApi p=new PlatformApi(ctx);Assert.assertEquals(PlatformApi.ORIGIN,p.origin);byte[] secret=new byte[32];new java.security.SecureRandom().nextBytes(secret);JSONObject j=p.call("POST","/auth/guest",PlatformApi.object("installation_secret",android.util.Base64.encodeToString(secret,11)));p.save(j.getString("access_token"));users.put(j.getJSONObject("user").getString("id"));return p;}
 int[] facts(){int[] w=NativeInterface.expeditionWire(1);return Arrays.copyOfRange(w,13+w[9],13+w[9]+w[10]);}
 int[] hostFacts(){int[] w=NativeInterface.expeditionWire(0);return Arrays.copyOfRange(w,13+w[9],13+w[9]+w[10]);}
 int at(){return 11+63;}
 void tap(float x,float y){long d=SystemClock.uptimeMillis();for(int a:new int[]{0,1}){MotionEvent e=MotionEvent.obtain(d,SystemClock.uptimeMillis(),a,x,y,0);inst.sendPointerSync(e);e.recycle();}}
 void slot(int k){float u=width/960f,h=height/u;tap((960-142+k*66+30)*u,(h-38)*u);}
 JSONObject observed()throws Exception{return new JSONObject(new String(java.nio.file.Files.readAllBytes(new File(ctx.getCacheDir(),"expedition192-observation.json").toPath()),java.nio.charset.StandardCharsets.UTF_8));}
 void world(int x,int y)throws Exception{JSONObject j=observed();JSONArray c=j.getJSONArray("camera"),v=j.getJSONArray("viewport");float px=(float)((x-c.getDouble(0))/c.getDouble(2)*v.getInt(2));float py=(float)((c.getDouble(1)+c.getDouble(3)-1-y)/c.getDouble(3)*v.getInt(3));Assert.assertTrue("on battlefield",px>80&&px<width-400&&py>150&&py<height-180);tap(px,py);}
 void shot(String name)throws Exception{android.graphics.Bitmap b=inst.getUiAutomation().takeScreenshot();Assert.assertNotNull(b);width=b.getWidth();height=b.getHeight();try(FileOutputStream out=new FileOutputStream(new File(ctx.getExternalFilesDir(null),"guest193-"+name+".png"))){b.compress(android.graphics.Bitmap.CompressFormat.PNG,100,out);}b.recycle();}
 @Test public void publicRenderedGuestAndCounters()throws Exception{
  Assert.assertTrue(BuildConfig.DEBUG&&ctx.getPackageName().endsWith("garden186"));
  try(FileWriter out=new FileWriter(new File(ctx.getCacheDir(),"guest193-public-fixture"))){out.write("isolated preferences, public service");}
  boolean load="yes".equals(InstrumentationRegistry.getArguments().getString("load"));int seats=load?10:2;
  host=login();JSONObject room=host.call("POST","/expedition/rooms",PlatformApi.object("protocol",193,"build",63,"seats",seats)).getJSONObject("room");code=room.getString("code");guest=login();
  try(FileWriter out=new FileWriter(new File(ctx.getExternalFilesDir(null),"guest193-test-users.json"))){out.write(users.toString());}status("test identities "+users);
  guest.call("POST","/expedition/rooms/"+code+"/join",PlatformApi.object("protocol",193,"build",63,"seats",seats));
  for(PlatformApi p:new PlatformApi[]{host,guest})p.call("POST","/expedition/rooms/"+code+"/ready",PlatformApi.object("ready",true));host.call("POST","/expedition/rooms/"+code+"/start",null);
  NativeInterface.init(ctx.getAssets());Assert.assertTrue(NativeInterface.createExpeditionGame(0,seats,room.getInt("seed")));place=fixture(load?1:0);Assert.assertNotNull(place);
  JSONObject ticket=host.call("POST","/expedition/rooms/"+code+"/ticket",null);
  socket=PlatformApi.HTTP.newWebSocket(new Request.Builder().url("wss://cleanbizai.com/ws/expedition?ticket="+ticket.getString("ticket")).build(),new WebSocketListener(){
   @Override public void onMessage(WebSocket w,String text){try{JSONObject m=new JSONObject(text);String t=m.optString("type");if(t.equals("joined")){joined=true;w.send("{\"type\":\"started\"}");}if(t.equals("command")){short[] x=new short[5],y=new short[5];for(int i=0;i<5;i++){x[i]=(short)m.getJSONArray("xs").getInt(i);y[i]=(short)m.getJSONArray("ys").getInt(i);}NativeInterface.expeditionRemoteCommand(1,m.getInt("kind"),m.getInt("group"),m.getInt("x"),m.getInt("y"),x,y);commands++;}}catch(Exception e){failure=e;}}
   @Override public void onFailure(WebSocket w,Throwable e,Response r){if(live)failure=e;}
  });
  live=true;simulation=new Thread(()->{long step=SystemClock.uptimeMillis(),send=step,ping=step;try{while(live){long now=SystemClock.uptimeMillis();int n=0;while(now>=step&&n++<8){NativeInterface.stepDots();step+=7;}if(joined&&now>=send){byte[] compressed=ExpeditionNetwork.encode(NativeInterface.expeditionWire(1));byte[] data=new byte[compressed.length+1];data[0]=1;System.arraycopy(compressed,0,data,1,compressed.length);socket.send(ByteString.of(data));packets++;send=now+70;}if(now>=ping){socket.send("{\"type\":\"ping\"}");ping=now+3000;}Thread.sleep(1);}}catch(Throwable e){failure=e;}},"Public fixture native host");simulation.start();
  try(FileWriter out=new FileWriter(new File(ctx.getCacheDir(),"expedition192-observe"))){out.write("1");}
  long launchWall=System.currentTimeMillis();ctx.startActivity(new Intent().setClassName(ctx.getPackageName(),"com.xenris.liquidwarsos.activities.GodotGardenActivity").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK).putExtra("garden3d",true).putExtra("expedition_seats",seats).putExtra("exp_room",code).putExtra("exp_host",false).putExtra("exp_slot",1).putExtra("exp_seed",room.getInt("seed")));
  until("public rendered guest",()->{try{JSONObject j=observed();return joined&&packets>30&&new File(ctx.getCacheDir(),"expedition192-observation.json").lastModified()>launchWall&&j.getJSONArray("exp").getInt(0)==193&&j.getJSONArray("exp").getInt(4)==seats&&Math.abs(j.getJSONArray("camera").getDouble(0)+200-place[0])<35;}catch(Exception e){return false;}},45000);
  Thread.sleep(1500);shot(load?"load20000":"public-ready");status("RENDERED_PUBLIC_READY");
  if(!load){
   Assert.assertEquals(40,facts()[at()+4]);Assert.assertTrue("actual automatic growth pickup",facts()[at()]>1000);
   int before=facts()[at()+11];slot(0);Thread.sleep(1000);status("freeze check stock="+facts()[at()+11]+" commands="+commands+" result="+facts()[at()+8]+" freezeEnd="+hostFacts()[11+27]+" time="+facts()[1]);until("global freeze one tap",()->facts()[at()+11]==before-1&&hostFacts()[11+27]>facts()[1],5000);shot("global-freeze10");slot(0);Thread.sleep(400);Assert.assertEquals(before-1,facts()[at()+11]);
   until("enemy ten-second freeze expires",()->hostFacts()[11+27]<=facts()[1],12000);
   // Same production authority freezes guest; actual on-screen cleanse travels over WSS.
   NativeInterface.expeditionCommand(0,1,0,3,0,0);until("incoming global freeze",()->facts()[at()+27]>facts()[1],2000);Thread.sleep(700);shot("frozen-can-cleanse");int clean=facts()[at()+12];slot(1);
   until("cleanse while frozen",()->facts()[at()+12]==clean-1&&facts()[at()+27]<=facts()[1]&&facts()[at()+30]>facts()[1],5000);shot("cleanse-confirmed");
   float u=width/960f,h=height/u;tap((960-75)*u,65*u);Thread.sleep(500);shot("expanded-map");tap((960-75)*u,70*u);Thread.sleep(500);
   world(place[0]-26,place[1]);Thread.sleep(3500);shot("continuous-movement");status("RECORD_MOVEMENT_NOW");
   for(int k=0;k<4;k++){tap(224*u,24*u);Thread.sleep(1200);}tap(170*u,24*u);Thread.sleep(1500);shot("overview");tap(170*u,24*u);Thread.sleep(1500);
   inst.sendKeyDownUpSync(KeyEvent.KEYCODE_HOME);Thread.sleep(1600);ctx.startActivity(new Intent().setClassName(ctx.getPackageName(),"com.xenris.liquidwarsos.activities.GodotGardenActivity").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_REORDER_TO_FRONT));Thread.sleep(3500);shot("background-restored");
  }else{Assert.assertEquals(20000,NativeInterface.totalParticles());status("LOAD_READY_20000");}
  Thread.sleep(load?25000:12000);Assert.assertNull(failure);Assert.assertTrue(packets>50);if(!load)Assert.assertTrue(commands>=3);
  if(!load)until("real simulation reaches ranked end",()->facts()[3]>=0,130000);
  socket.send("{\"type\":\"finished\"}");Thread.sleep(1200);shot("public-ended");
  try(FileWriter out=new FileWriter(new File(ctx.getExternalFilesDir(null),load?"guest193-load.json":"guest193-public-result.json"))){out.write(new JSONObject().put("result","PASS").put("origin",PlatformApi.ORIGIN).put("packets",packets).put("commands",commands).put("users",users).put("scope","Two Android processes, one MuMu instance, public HTTPS/WSS; fixture inventory and positions, real simulation/UI/renderer; not two physical devices").toString(2));}status("PASS PUBLIC_GAMEPLAY");
 }
 @After public void cleanup()throws Exception{live=false;if(simulation!=null)simulation.join(3000);if(socket!=null)socket.close(1000,"test complete");if(host!=null&&code!=null)try{host.call("POST","/expedition/rooms/"+code+"/leave",null);}catch(Exception ignored){}NativeInterface.destroyGame();NativeInterface.uninit();}
}
