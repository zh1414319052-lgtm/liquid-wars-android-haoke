"""Local socket and API-double checks. No device/emulator or external network. GPL-3.0-or-later."""
from pathlib import Path
import hashlib,json,subprocess,xml.etree.ElementTree as ET

ROOT=Path(__file__).resolve().parents[1]
OUT=ROOT/'docs/game/lan-1.50'
BUILD=ROOT/'build/lan-check'
JAVA=Path(r'C:/Program Files/Microsoft/jdk-21.0.11.10-hotspot/bin')
BASE=ROOT/'app/src/main/java/com/xenris/liquidwarsos/server'
baseline=json.loads((OUT/'baseline-files.json').read_text(encoding='utf8'))
allowed={
 'app/build.gradle','app/src/main/res/layout/multiplayer_menu.xml',
 *(f'app/src/main/java/com/xenris/liquidwarsos/activities/{n}.java' for n in ['MainMenuActivity','MultiplayerMenuActivity','MultiplayerGameSetupActivity','ClientGameSetupActivity']),
 *(f'app/src/main/java/com/xenris/liquidwarsos/server/{n}.java' for n in ['Server','ServerFinder','NetInfo'])}
changed=[]
for rel,old in baseline.items():
    if hashlib.sha256((ROOT/rel).read_bytes()).hexdigest()!=old:
        assert rel in allowed,('unexpected source change',rel)
        changed.append(rel)
assert set(changed)==allowed
keys=None
for language in ['values','values-zh','values-b+zh+Hant']:
    nodes=ET.parse(ROOT/f'app/src/main/res/{language}/lan_discovery.xml').getroot()
    current={n.attrib['name'] for n in nodes}
    assert all(n.text for n in nodes)
    if keys is None:keys=current
    assert current==keys
layout=ET.parse(ROOT/'app/src/main/res/layout/multiplayer_menu.xml').getroot()
actions={n.attrib['{http://schemas.android.com/apk/res/android}onClick'] for n in layout.iter() if '{http://schemas.android.com/apk/res/android}onClick' in n.attrib}
assert actions=={'startNewGame','refreshRooms','manualConnect'}
menu=(ROOT/'app/src/main/java/com/xenris/liquidwarsos/activities/MultiplayerMenuActivity.java').read_text(encoding='utf8')
assert 'ServerFinder.stopSharing' not in menu and 'ServerFinder.search(this' in menu
assert 'generation != scanGeneration' in menu and 'if (!active || joining)' in menu
host=(ROOT/'app/src/main/java/com/xenris/liquidwarsos/activities/MultiplayerGameSetupActivity.java').read_text(encoding='utf8')
assert 'if (StaticBits.server.startAccepting()) updateRoomAdvertisement()' in host
assert 'connectedClientCount() < StaticBits.NUMBER_OF_TEAMS - 1' in host
assert 'roomVisible = false;' in host and 'ServerFinder.stopSharing();' in host

sources={
'android/content/Context.java':'''package android.content;
import android.net.nsd.NsdManager;
public class Context {public static final String NSD_SERVICE="nsd"; public NsdManager nsd=new NsdManager();
 public Context getApplicationContext(){return this;} public Object getSystemService(String key){return nsd;}}
''',
'android/os/Looper.java':'''package android.os; public class Looper {public static Looper getMainLooper(){return new Looper();}}''',
'android/os/Build.java':'''package android.os; public class Build {public static class VERSION {public static int SDK_INT=34;}}''',
'android/os/SystemClock.java':'''package android.os; public class SystemClock {public static long elapsedRealtime(){return Handler.now;}}''',
'android/os/Handler.java':'''package android.os;
import java.util.*;
public class Handler {
 public static long now; static long order;
 static class Job {Handler owner;Runnable task;long due,id;Job(Handler h,Runnable r,long d){owner=h;task=r;due=d;id=order++;}}
 static final ArrayList<Job> jobs=new ArrayList<>(); public Handler(Looper l){}
 public synchronized boolean post(Runnable r){return postDelayed(r,0);}
 public synchronized boolean postDelayed(Runnable r,long ms){synchronized(jobs){jobs.add(new Job(this,r,now+ms));}return true;}
 public void removeCallbacks(Runnable r){synchronized(jobs){jobs.removeIf(j->j.owner==this&&j.task==r);}}
 public void removeCallbacksAndMessages(Object token){synchronized(jobs){jobs.removeIf(j->j.owner==this);}}
 public static void drain(){for(int n=0;n<10000;n++){Job next=null;synchronized(jobs){for(Job j:jobs)if(j.due<=now&&(next==null||j.id<next.id))next=j;if(next!=null)jobs.remove(next);}if(next==null)return;next.task.run();}throw new AssertionError("Unbounded callbacks");}
 public static void advance(long ms){now+=ms;drain();}
}''',
'android/util/Log.java':'''package android.util; public class Log {public static int w(String a,String b){return 0;}public static int w(String a,String b,Throwable t){return 0;}}''',
'android/net/nsd/NsdServiceInfo.java':'''package android.net.nsd;
import java.net.*;import java.util.*;import java.nio.charset.StandardCharsets;
public class NsdServiceInfo {
 String name,type;int port;InetAddress host;Map<String,byte[]> attrs=new HashMap<>();
 public void setServiceName(String n){name=n;} public String getServiceName(){return name;}
 public void setServiceType(String t){type=t;} public String getServiceType(){return type;}
 public void setPort(int p){port=p;} public int getPort(){return port;}
 public void setHost(InetAddress h){host=h;} public InetAddress getHost(){return host;}
 public void setAttribute(String k,String v){attrs.put(k,v.getBytes(StandardCharsets.UTF_8));}
 public Map<String,byte[]> getAttributes(){return attrs;}
}''',
'android/net/nsd/NsdManager.java':'''package android.net.nsd;
import java.util.*;
public class NsdManager {
 public static final int PROTOCOL_DNS_SD=1;public boolean fail;public int stopped,unregistered,cancelled;
 public DiscoveryListener discovery;public RegistrationListener registration;public NsdServiceInfo advertised;
 public List<NsdServiceInfo> resolving=new ArrayList<>();public List<ResolveListener> resolvers=new ArrayList<>();
 public void registerService(NsdServiceInfo s,int p,RegistrationListener l){if(fail)throw new SecurityException();registration=l;advertised=s;}
 public void unregisterService(RegistrationListener l){unregistered++;}
 public void discoverServices(String t,int p,DiscoveryListener l){if(fail)throw new IllegalArgumentException();discovery=l;}
 public void stopServiceDiscovery(DiscoveryListener l){stopped++;}
 public void resolveService(NsdServiceInfo s,ResolveListener l){resolving.add(s);resolvers.add(l);}
 public void stopServiceResolution(ResolveListener l){cancelled++;}
 public interface RegistrationListener {void onServiceRegistered(NsdServiceInfo i);void onRegistrationFailed(NsdServiceInfo i,int e);void onServiceUnregistered(NsdServiceInfo i);void onUnregistrationFailed(NsdServiceInfo i,int e);}
 public interface DiscoveryListener {void onDiscoveryStarted(String t);void onStartDiscoveryFailed(String t,int e);void onDiscoveryStopped(String t);void onStopDiscoveryFailed(String t,int e);void onServiceFound(NsdServiceInfo i);void onServiceLost(NsdServiceInfo i);}
 public interface ResolveListener {void onResolveFailed(NsdServiceInfo i,int e);void onServiceResolved(NsdServiceInfo i);}
}''',
'com/xenris/liquidwarsos/StaticBits.java':'''package com.xenris.liquidwarsos; public class StaticBits {public static final int VERSION_CODE=12;public static int PORT_NUMBER=51055;}''',
'com/xenris/liquidwarsos/server/NetInfo.java':'''package com.xenris.liquidwarsos.server;
import android.content.Context;import java.net.*;import java.util.*;
public class NetInfo {public static List<InetAddress> broadcastAddresses(Context c){return Collections.singletonList(InetAddress.getLoopbackAddress());}}''',
'com/xenris/liquidwarsos/server/LanDiscoveryCheck.java':'''package com.xenris.liquidwarsos.server;
import android.content.Context;import android.net.nsd.*;import android.os.*;
import java.net.*;import java.util.*;import java.util.concurrent.*;import java.util.concurrent.atomic.*;
import java.nio.charset.StandardCharsets;
public class LanDiscoveryCheck {
 static void yes(boolean v,String s){if(!v)throw new AssertionError(s);}
 static NsdServiceInfo service(String name,String ip,int version)throws Exception {
  NsdServiceInfo s=new NsdServiceInfo();s.setServiceName(name);s.setServiceType(NsdRoomDiscovery.TYPE);s.setPort(51055);
  s.setHost(InetAddress.getByName(ip));s.setAttribute("app","liquidwarsos");s.setAttribute("v",""+version);return s;
 }
 static void directory(){
  RoomDirectory d=new RoomDirectory();LanRoom a=new LanRoom("同名房间","192.168.1.2",51055),b=new LanRoom("同名房间","192.168.1.3",51055);
  d.foundUdp(a,0);d.foundNsd("A",a);d.foundNsd("B",b);yes(d.snapshot(1).size()==2,"Endpoint dedup and same names");
  d.lostNsd("A");yes(d.snapshot(2).size()==2,"Fresh UDP survives NSD loss");yes(d.snapshot(6500).size()==1,"UDP expiry");
  d.foundNsd("B",new LanRoom("改名房间",b.ip,b.port));yes(d.snapshot(6501).get(0).name.equals("改名房间"),"Rename");
  d.lostNsd("B");yes(d.snapshot(6502).isEmpty(),"Service lost");
  d.foundNsd("old-registration",a);d.foundNsd("new-registration",new LanRoom("新房间名",a.ip,a.port));
  yes(d.snapshot(6503).size()==1&&d.snapshot(6503).get(0).name.equals("新房间名"),"Latest name during registration overlap");
  d.lostNsd("old-registration");d.lostNsd("new-registration");
  for(int i=0;i<100;i++)d.foundUdp(new LanRoom("x","10.0.0."+(i+1),51055),7000);
  yes(d.snapshot(7000).size()==64,"Bounded discovery cache");
  yes(LanRoom.cleanName("中".repeat(50)+"\\n").getBytes(StandardCharsets.UTF_8).length<=60,"UTF8 service label limit");
  yes(!LanRoom.cleanName("a\\nb\\tc").contains("\\n"),"Room name controls");
 }
 static void udp()throws Exception {
  byte[] data=LegacyLanDiscovery.response("中文房间🏠");
  yes(LegacyLanDiscovery.decode(data,data.length,"192.168.1.2",51055).name.equals("中文房间🏠"),"UTF8 wire roundtrip");
  for(int n=0;n<13;n++)yes(LegacyLanDiscovery.decode(data,n,"192.168.1.2",51055)==null,"Short response");
  byte[] bad=data.clone();bad[0]=0;yes(LegacyLanDiscovery.decode(bad,bad.length,"192.168.1.2",51055)==null,"Wrong prefix");
  bad=Arrays.copyOf(LegacyLanDiscovery.RESPONSE,14);bad[13]=(byte)0xff;yes(LegacyLanDiscovery.decode(bad,bad.length,"x",51055)==null,"Malformed UTF8");
  int port;try(DatagramSocket probe=new DatagramSocket(0)){port=probe.getLocalPort();}
  for(int cycle=0;cycle<8;cycle++){
   final String name="房间"+cycle;CountDownLatch found=new CountDownLatch(1);
   try(LegacyLanDiscovery.Publisher pub=new LegacyLanDiscovery.Publisher(port,name);
       LegacyLanDiscovery.Browser browser=new LegacyLanDiscovery.Browser(port,51055,()->Collections.singletonList(InetAddress.getLoopbackAddress()),room->{if(room.name.equals(name))found.countDown();})){
    yes(found.await(3,TimeUnit.SECONDS),"Real loopback discovery cycle "+cycle);
   }
  }
  // Send a short datagram before a long valid response to exercise receive-buffer reset.
  try(DatagramSocket responder=new DatagramSocket(0)){
   responder.setSoTimeout(2000);Thread thread=new Thread(()->{try{
    DatagramPacket q=new DatagramPacket(new byte[64],64);responder.receive(q);
    responder.send(new DatagramPacket(new byte[]{1},1,q.getAddress(),q.getPort()));
    byte[] longReply=LegacyLanDiscovery.response("一个比较长的房间名称🏠");responder.send(new DatagramPacket(longReply,longReply.length,q.getAddress(),q.getPort()));
   }catch(Exception e){throw new RuntimeException(e);}});thread.start();CountDownLatch found=new CountDownLatch(1);
   try(LegacyLanDiscovery.Browser browser=new LegacyLanDiscovery.Browser(responder.getLocalPort(),51055,()->Collections.singletonList(InetAddress.getLoopbackAddress()),room->found.countDown())){
    yes(found.await(3,TimeUnit.SECONDS),"Short then long response must not truncate");
   }thread.join(2500);yes(!thread.isAlive(),"Responder stopped");
  }
 }
 static void nsd()throws Exception {
  Context c=new Context();List<List<LanRoom>> events=new ArrayList<>();
  NsdRoomDiscovery.Browser browser=new NsdRoomDiscovery.Browser(c,events::add);Handler.drain();
  NsdServiceInfo a=service("A","192.168.1.2",12),b=service("B","192.168.1.3",12);
  c.nsd.discovery.onServiceFound(a);c.nsd.discovery.onServiceFound(b);Handler.drain();
  yes(c.nsd.resolvers.size()==1,"Serial NSD resolution");
  c.nsd.resolvers.get(0).onServiceResolved(a);Handler.drain();Handler.advance(100);
  yes(c.nsd.resolvers.size()==2&&events.get(events.size()-1).size()==1,"Next queued service");
  c.nsd.discovery.onServiceLost(b);Handler.drain();c.nsd.resolvers.get(1).onServiceResolved(b);Handler.drain();
  yes(events.get(events.size()-1).size()==1,"Lost before resolve cannot reappear");
  browser.acceptUdp(new LanRoom("A", "192.168.1.2",51055));Handler.drain();
  yes(events.get(events.size()-1).size()==1,"NSD/UDP merge");
  c.nsd.discovery.onServiceLost(a);Handler.drain();yes(events.get(events.size()-1).size()==1,"UDP retention after service loss");
  Handler.advance(6500);yes(events.get(events.size()-1).isEmpty(),"UDP expiry timer");
  browser.close();int count=events.size();c.nsd.discovery.onServiceFound(a);Handler.drain();yes(events.size()==count,"Closed session ignores callbacks");
  c.nsd.discovery.onDiscoveryStarted(NsdRoomDiscovery.TYPE);Handler.drain();yes(c.nsd.stopped>=2,"Cancel-before-start handles late start");
  Context wrong=new Context();List<List<LanRoom>> wrongEvents=new ArrayList<>();NsdRoomDiscovery.Browser other=new NsdRoomDiscovery.Browser(wrong,wrongEvents::add);
  NsdServiceInfo old=service("old","192.168.1.4",11);wrong.nsd.discovery.onServiceFound(old);Handler.drain();wrong.nsd.resolvers.get(0).onServiceResolved(old);Handler.drain();yes(wrongEvents.isEmpty(),"Incompatible NSD version filtered");other.close();
  Context timeout=new Context();NsdRoomDiscovery.Browser slow=new NsdRoomDiscovery.Browser(timeout,x->{});timeout.nsd.discovery.onServiceFound(a);Handler.drain();Handler.advance(5000);yes(timeout.nsd.cancelled==1,"Bounded NSD resolution timeout");slow.close();
  Context publisher=new Context();NsdRoomDiscovery.Publisher pub=new NsdRoomDiscovery.Publisher(publisher,51055,"中文房间");pub.close();publisher.nsd.registration.onServiceRegistered(publisher.nsd.advertised);yes(publisher.nsd.unregistered>=2,"Late registration must unregister");
  Context unavailable=new Context();unavailable.nsd.fail=true;NsdRoomDiscovery.Browser fallback=new NsdRoomDiscovery.Browser(unavailable,x->{});fallback.acceptUdp(new LanRoom("fallback","192.168.1.5",51055));Handler.drain();fallback.close();
 }
 static class Host extends Context implements Server.ServerCallbacks {
  final AtomicInteger count=new AtomicInteger();
  public void onClientConnected(int id){count.incrementAndGet();}public void onClientDisconnected(int id){count.decrementAndGet();}
  public void onClientMessageReceived(int id,int argc,int[] args){}
 }
 static void facade()throws Exception {
  int port;try(DatagramSocket probe=new DatagramSocket(0)){port=probe.getLocalPort();}
  com.xenris.liquidwarsos.StaticBits.PORT_NUMBER=port-1;
  Context context=new Context();ServerFinder.share(context,port,"独立发布");
  ServerFinder.search(context,x->{});ServerFinder.stopSearching();
  yes(context.nsd.unregistered==0,"Stopping browser must not unregister publisher");
  try(DatagramSocket query=new DatagramSocket()){
   query.setSoTimeout(2000);query.send(new DatagramPacket(LegacyLanDiscovery.QUERY,LegacyLanDiscovery.QUERY.length,InetAddress.getLoopbackAddress(),port));
   DatagramPacket reply=new DatagramPacket(new byte[128],128);query.receive(reply);
   yes(LegacyLanDiscovery.decode(reply.getData(),reply.getLength(),"127.0.0.1",port-1).name.equals("独立发布"),"Publisher socket survives stopping search");
  }
  List<List<LanRoom>> events=new ArrayList<>();ServerFinder.search(context,events::add);int before=context.nsd.stopped;
  ServerFinder.stopSharing();yes(context.nsd.stopped==before,"Stopping publication must not stop search");
  NsdServiceInfo room=service("仍可发现","192.168.1.20",12);context.nsd.discovery.onServiceFound(room);Handler.drain();
  int last=context.nsd.resolvers.size()-1;context.nsd.resolvers.get(last).onServiceResolved(room);Handler.drain();
  yes(!events.isEmpty(),"Browser callback survives stopping publisher");ServerFinder.stopSearching();
 }
 static void waitCount(Host host,int expected)throws Exception{long end=System.nanoTime()+TimeUnit.SECONDS.toNanos(3);while(host.count.get()!=expected&&System.nanoTime()<end)Thread.sleep(5);yes(host.count.get()==expected,"TCP peer count "+expected);}
 static int id(Socket socket)throws Exception{socket.setSoTimeout(2000);return socket.getInputStream().read();}
 static void tcp()throws Exception {
  int port;try(ServerSocket probe=new ServerSocket(0)){port=probe.getLocalPort();}Host host=new Host();Server server=new Server(host,port);
  try{
   yes(server.startAccepting()&&server.isAccepting(),"Bind before advertise");
   try(Socket first=new Socket(InetAddress.getLoopbackAddress(),port)){
    int firstId=id(first);yes(firstId>0,"Legacy one-byte ID");waitCount(host,1);yes(server.connectedClientCount()==1,"Thread-safe room population");
    for(int n=0;n<8;n++){server.stopAccepting();yes(server.startAccepting(),"Fast listener restart");try(Socket next=new Socket(InetAddress.getLoopbackAddress(),port)){yes(id(next)!=firstId,"Restart must not reuse active ID");waitCount(host,2);}waitCount(host,1);}
    java.lang.reflect.Field cursor=Server.class.getDeclaredField("nextClientId");cursor.setAccessible(true);cursor.setInt(server,255);
    try(Socket edge=new Socket(InetAddress.getLoopbackAddress(),port)){yes(id(edge)==255,"Unsigned ID boundary");waitCount(host,2);}
    waitCount(host,1);
    ArrayList<Socket> peers=new ArrayList<>();try{
     for(int n=0;n<4;n++){Socket peer=new Socket(InetAddress.getLoopbackAddress(),port);peers.add(peer);int assigned=id(peer);yes(assigned>0&&assigned<=255&&assigned!=firstId,"Wrapped IDs skip active host peers");}
     waitCount(host,5);try(Socket full=new Socket(InetAddress.getLoopbackAddress(),port)){yes(id(full)==-1,"Five-client capacity preserved");}
    }finally{for(Socket socket:peers)socket.close();}
    waitCount(host,1);
    Server conflict=new Server(new Host(),port);yes(!conflict.startAccepting(),"Bind failure returns false");conflict.destroy();
   }waitCount(host,0);
  }finally{server.destroy();}
 }
 public static void main(String[] args)throws Exception {
  directory();System.out.println("PASS directory: source merge, same-name rooms, rename/loss/expiry, bounded cache and UTF8 labels");
  udp();System.out.println("PASS real localhost UDP: UTF8/malformed packets, short-long receive, 8 stop/restart cycles");
  nsd();System.out.println("PASS NSD API doubles: serial resolve, stale callbacks, loss, version filter, timeout and cancellation");
  facade();System.out.println("PASS combined facade: stopping/restarting search and publication cannot close one another");
  tcp();System.out.println("PASS real localhost TCP listener: bind readiness/failure, 8 restarts, active ID preservation/wrap, 5-client limit");
  System.out.println("NOT_TESTED: Android NSD stack, Wi-Fi broadcasts/multicast, actual phones, UI, multi-device play, performance");
 }
}'''}
for name,source in sources.items():
    p=BUILD/name;p.parent.mkdir(parents=True,exist_ok=True);p.write_text(source,encoding='utf8')
classes=BUILD/'classes';classes.mkdir(parents=True,exist_ok=True)
actual=['LanRoom','RoomDirectory','LegacyLanDiscovery','NsdRoomDiscovery','ServerFinder','Server','SubServer']
files=[str(BUILD/name) for name in sources]+[str(BASE/(name+'.java')) for name in actual]
subprocess.run([str(JAVA/'javac.exe'),'-encoding','UTF-8','-d',str(classes),*files],check=True)
subprocess.run([str(JAVA/'java.exe'),'-cp',str(classes),'com.xenris.liquidwarsos.server.LanDiscoveryCheck'],check=True,timeout=45)
report=dict(status='PASS',baseline='f3726bc/1.49-QA',baseline_files=len(baseline),changed_files=changed,
            real_localhost_udp='PASS',real_localhost_tcp_listener='PASS',nsd_java_api_doubles='PASS',
            cpp_maps_touch_result_protocol_unchanged=True,localized_keys=sorted(keys),
            device='NOT_TESTED',android_nsd='NOT_TESTED',same_wifi_discovery='NOT_TESTED',actual_ui='NOT_TESTED',multiplayer_game='NOT_TESTED',performance='NOT_TESTED')
(OUT/'focused-check.json').write_text(json.dumps(report,ensure_ascii=False,indent=2)+'\n',encoding='utf8')
print('PASS source invariants, layout wiring and localized resources. No new test dependencies.')
