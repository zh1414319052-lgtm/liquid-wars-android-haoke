// SPDX-License-Identifier: GPL-3.0-or-later
package com.xenris.liquidwarsos.online;
import com.xenris.liquidwarsos.*;
import com.xenris.liquidwarsos.activities.GameActivity;
import java.io.*;import java.nio.*;import java.util.*;import java.util.concurrent.*;import java.util.zip.*;
import okhttp3.*;import okio.ByteString;import org.json.*;

/** Host simulates once. Guests receive only their filtered, bounded presentation and private HUD. */
public final class ExpeditionNetwork implements AutoCloseable {
 public static volatile ExpeditionNetwork active;
 public final boolean host;public final int seat,seats;public final PlatformApi api;
 private final String code;private final GameActivity activity;private final ScheduledExecutorService io=Executors.newSingleThreadScheduledExecutor();
 private final Set<Integer> connected=ConcurrentHashMap.newKeySet();private final Set<Integer> humans=ConcurrentHashMap.newKeySet();
 private final long[] missing=new long[10];private final int[] sentClock=new int[10],sentEpoch=new int[10];
 private volatile WebSocket ws;private volatile boolean closed,ready,joined,started,ended;private volatile String status="连接远征房间…";
 private int serial,metricTicks;private long lastFrame,lastPing,joinAt,retryAt;private boolean firstView=true;private OnlineSession emotes;
 public ExpeditionNetwork(GameActivity a){activity=a;api=new PlatformApi(a);code=a.getIntent().getStringExtra("exp_room");host=a.getIntent().getBooleanExtra("exp_host",false);seat=a.getIntent().getIntExtra("exp_slot",0);seats=a.getIntent().getIntExtra("expedition_seats",6);Arrays.fill(sentClock,-1);active=this;io.execute(this::connect);io.schedule(this::cycle,70,TimeUnit.MILLISECONDS);}
 private void cycle(){long began=android.os.SystemClock.elapsedRealtime();try{tick();}finally{if(!closed&&!ended)try{io.schedule(this::cycle,Math.max(2,70-(android.os.SystemClock.elapsedRealtime()-began)),TimeUnit.MILLISECONDS);}catch(RejectedExecutionException ignored){}}}
 public boolean ready(){return ready;}
 public String status(){return status;}
 public void attach(){emotes=new OnlineSession(api,code,host,false);emotes.attach(activity,true);}
 private void connect(){if(closed||ended)return;try{JSONObject ticket=api.call("POST","/expedition/rooms/"+code+"/ticket",null);String url=api.origin.replace("https://","wss://").replace("http://","ws://")+ticket.getString("path")+"?ticket="+ticket.getString("ticket");ws=PlatformApi.HTTP.newWebSocket(new Request.Builder().url(url).build(),new WebSocketListener(){
  @Override public void onMessage(WebSocket socket,String text){try{io.execute(()->message(text));}catch(RejectedExecutionException ignored){}}
  @Override public void onMessage(WebSocket socket,ByteString bytes){if(host)return;try{io.execute(()->receive(bytes.toByteArray()));}catch(RejectedExecutionException ignored){}}
  @Override public void onFailure(WebSocket socket,Throwable error,Response response){disconnected();}
  @Override public void onClosed(WebSocket socket,int value,String reason){disconnected();}
 });}catch(Exception e){status="连接失败 · "+e.getMessage();retryAt=android.os.SystemClock.elapsedRealtime()+2000;}}
 private void disconnected(){if(closed||ended)return;joined=false;ready=false;status=host?"房主连接断开，对局安全结束":"断线重连中 · 原席位保留";if(host){ended=true;activity.runOnUiThread(()->activity.networkEnded(status));}else retryAt=android.os.SystemClock.elapsedRealtime()+1500;}
 private void message(String text){if(closed)return;try{JSONObject m=new JSONObject(text);String kind=m.optString("type");int slot=m.optInt("slot",-1);long now=android.os.SystemClock.elapsedRealtime();
  if(kind.equals("connected")){if(slot!=seat||m.getInt("seats")!=seats||m.getInt("protocol")!=193)throw new IOException("房间身份或协议不一致");serial=Math.max(serial,m.optInt("command_serial",0));joined=true;joinAt=now;lastFrame=now;retryAt=0;humans.clear();JSONArray h=m.getJSONArray("humans");for(int i=0;i<h.length();i++){int t=h.getInt(i);humans.add(t);if(host){NativeInterface.expeditionBot(t,false);missing[t]=now;}}connected.clear();connected.add(seat);status=host?"等待真人连接，缺席15秒后电脑接管":"正在接收权威战场";}
  else if(kind.equals("joined")&&host&&slot>0&&slot<seats){connected.add(slot);missing[slot]=0;sentClock[slot]=-1;NativeInterface.expeditionBot(slot,false);}
  else if(kind.equals("left")&&host&&slot>0&&slot<seats){connected.remove(slot);missing[slot]=now;}
  else if(kind.equals("command")&&host&&slot>0&&slot<seats){short[] x=new short[5],y=new short[5];JSONArray xx=m.getJSONArray("xs"),yy=m.getJSONArray("ys");for(int i=0;i<5;i++){x[i]=(short)xx.getInt(i);y[i]=(short)yy.getInt(i);}NativeInterface.expeditionRemoteCommand(slot,m.getInt("kind"),m.getInt("group"),m.getInt("x"),m.getInt("y"),x,y);}
  else if(kind.equals("ended")){ended=true;ready=false;status="对局已结束（"+m.optString("reason")+"）";activity.runOnUiThread(()->activity.networkEnded(status));}
 }catch(Exception e){status="联机消息无效，已停止接收";if(ws!=null)ws.close(1008,"invalid message");}}
 private void receive(byte[] compressed){if(closed||ended)return;try{int[] data=decode(compressed);if(NativeInterface.expeditionReceive(data)){lastFrame=android.os.SystemClock.elapsedRealtime();ready=true;status="";if(firstView){firstView=false;activity.runOnUiThread(()->{MyGLSurfaceView v=activity.findViewById(R.id.mySurfaceView);v.camera.id=-1;v.syncWorld();v.camera.relocate(data[7],data[8]);activity.updatePauseState();});}}}catch(Exception e){status="战场快照校验失败，等待下一帧";}}
 private void tick(){if(closed||ended)return;long now=android.os.SystemClock.elapsedRealtime();if(!joined){if(retryAt>0&&now>=retryAt){retryAt=now+5000;connect();}return;}WebSocket socket=ws;if(socket==null)return;
  if(now-lastPing>3000){lastPing=now;socket.send("{\"type\":\"ping\"}");}
  if(!host){if(now-lastFrame>3000)status="等待网络恢复 · 不预测兵力变化";return;}
  for(int t:humans)if(t!=seat&&missing[t]>0&&now-missing[t]>=15000){NativeInterface.expeditionBot(t,true);missing[t]=0;}
  if(!ready&&(connected.containsAll(humans)||now-joinAt>=15000)){ready=true;status="";activity.runOnUiThread(activity::updatePauseState);}
  if(ready&&!started){started=true;socket.send("{\"type\":\"started\"}");}
  long packStart=System.nanoTime(),wireNs=0,codecNs=0;int packets=0,bytesSent=0;for(int t:connected){if(t==seat)continue;try{long phase=System.nanoTime();int[] frame=NativeInterface.expeditionWire(t);wireNs+=System.nanoTime()-phase;if(frame.length<13||(frame[2]==sentClock[t]&&frame[6]==sentEpoch[t]))continue;if(socket.queueSize()>1024*1024){status="网络拥堵 · 等待发送最新状态";break;}phase=System.nanoTime();byte[] bytes=encode(frame);codecNs+=System.nanoTime()-phase;if(bytes.length>262144){status="快照超过传输预算";continue;}byte[] packet=new byte[bytes.length+1];packet[0]=(byte)t;System.arraycopy(bytes,0,packet,1,bytes.length);if(socket.send(ByteString.of(packet))){sentClock[t]=frame[2];sentEpoch[t]=frame[6];packets++;bytesSent+=bytes.length;}}catch(Exception error){status="状态发送失败";}}if(com.xenris.liquidwarsos.BuildConfig.DEBUG&&++metricTicks%30==0)android.util.Log.i("LWNet193","packets="+packets+" bytes="+bytesSent+" wire_ms="+wireNs/1e6+" codec_ms="+codecNs/1e6+" total_ms="+(System.nanoTime()-packStart)/1e6);
 }
 public synchronized void command(int kind,int group,int x,int y,short[] xs,short[] ys){if(host||closed||!joined||ws==null)return;try{JSONArray a=new JSONArray(),b=new JSONArray();for(int i=0;i<5;i++){a.put(xs==null?-1:xs[i]);b.put(ys==null?-1:ys[i]);}JSONObject msg=PlatformApi.object("type","command","serial",++serial,"kind",kind,"group",group,"x",x,"y",y,"xs",a,"ys",b);if(!ws.send(msg.toString()))status="命令未发送，请重新操作";}catch(Exception e){status="命令未发送";}}
 public void finished(){if(host&&!closed)try{io.schedule(()->{if(ws!=null&&!closed)ws.send("{\"type\":\"finished\"}");},1000,TimeUnit.MILLISECONDS);}catch(RejectedExecutionException ignored){}}
 @Override public void close(){if(closed)return;closed=true;ready=false;if(ws!=null)ws.close(1000,"leave");io.shutdownNow();if(emotes!=null)emotes.close();else new Thread(()->{try{api.call("POST","/expedition/rooms/"+code+"/leave",null);}catch(Exception ignored){}},"Leave expedition").start();if(active==this)active=null;}
 public static byte[] encode(int[] data)throws IOException{
  if(data.length<13||data.length>1000000)throw new IOException("projection bounds");
  ByteBuffer raw=ByteBuffer.allocate((data.length+1)*4).order(ByteOrder.BIG_ENDIAN);raw.putInt(data.length);raw.asIntBuffer().put(data);
  ByteArrayOutputStream out=new ByteArrayOutputStream();Deflater compressor=new Deflater(1);
  try(DeflaterOutputStream stream=new DeflaterOutputStream(out,compressor,16384)){stream.write(raw.array());}
  finally{compressor.end();} // Explicitly owns the supplied native compressor.
  return out.toByteArray();
 }
 public static int[] decode(byte[] bytes)throws IOException{
  if(bytes.length>262144)throw new IOException("compressed bounds");
  try(DataInputStream input=new DataInputStream(new InflaterInputStream(new ByteArrayInputStream(bytes)))){
   int size=input.readInt();if(size<13||size>1000000)throw new IOException("projection bounds");
   byte[] raw=new byte[size*4];input.readFully(raw);if(input.read()!=-1)throw new IOException("projection tail");
   int[] out=new int[size];ByteBuffer.wrap(raw).order(ByteOrder.BIG_ENDIAN).asIntBuffer().get(out);return out;
  }
 }
}
