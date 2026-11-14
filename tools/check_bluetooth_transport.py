"""Focused local transport tests. Android Bluetooth is doubled, never a real radio. GPL-3.0-or-later."""
from pathlib import Path
import hashlib,json,subprocess,xml.etree.ElementTree as ET

ROOT=Path(__file__).resolve().parents[1];OUT=ROOT/'docs/game/bluetooth-1.51';BUILD=ROOT/'build/bluetooth-check'
JAVA=Path(r'C:/Program Files/Microsoft/jdk-21.0.11.10-hotspot/bin');SRC=ROOT/'app/src/main/java/com/xenris/liquidwarsos'
baseline=json.loads((OUT/'baseline-files.json').read_text(encoding='utf8'))
allowed={'app/build.gradle','app/src/main/AndroidManifest.xml',
 *(f'app/src/main/java/com/xenris/liquidwarsos/{x}.java' for x in ['Client','server/Server','server/SubServer','activities/ClientGameSetupActivity','activities/MultiplayerGameSetupActivity','activities/MultiplayerMenuActivity']),
 'app/src/main/res/layout/multiplayer_menu.xml','app/src/main/res/layout/multi_game_setup.xml'}
changed=[]
for rel,sha in baseline.items():
    if hashlib.sha256((ROOT/rel).read_bytes()).hexdigest()!=sha:
        assert rel in allowed,('unexpected source change',rel)
        changed.append(rel)
assert set(changed)==allowed
ns='{http://schemas.android.com/apk/res/android}'
manifest=ET.parse(ROOT/'app/src/main/AndroidManifest.xml').getroot()
permissions={n.attrib[ns+'name']:n.attrib for n in manifest.findall('uses-permission')}
assert permissions['android.permission.BLUETOOTH'][ns+'maxSdkVersion']=='30'
assert permissions['android.permission.BLUETOOTH_ADMIN'][ns+'maxSdkVersion']=='30'
assert permissions['android.permission.BLUETOOTH_SCAN'][ns+'usesPermissionFlags']=='neverForLocation'
assert not any('LOCATION' in n for n in permissions)
assert any(n.attrib.get(ns+'name')=='android.hardware.bluetooth' and n.attrib.get(ns+'required')=='false' for n in manifest.findall('uses-feature'))
keys=None
for language in ['values','values-zh','values-b+zh+Hant']:
    nodes=ET.parse(ROOT/f'app/src/main/res/{language}/bluetooth.xml').getroot();current={n.attrib['name'] for n in nodes}
    assert all(n.text for n in nodes)
    if keys is None:keys=current
    assert current==keys
ui=(SRC/'activities/BluetoothMenuActivity.java').read_text(encoding='utf8')
assert 'Build.VERSION.SDK_INT < 31' in ui and 'bluetoothSettings(null)' in ui
assert 'permissionPending' in ui and 'ENABLE_REQUEST' in ui and 'unregisterReceiver(receiver)' in ui
assert 'bluetooth.cancelDiscovery()' in ui and 'stopScan();' in ui
clientui=(SRC/'activities/ClientGameSetupActivity.java').read_text(encoding='utf8')
assert clientui.index('StaticBits.client = roomClient')<clientui.index('roomClient.start()')
hostui=(SRC/'activities/MultiplayerGameSetupActivity.java').read_text(encoding='utf8')
assert 'onListeningChanged(boolean ready)' in hostui and '!bluetoothRoom && roomVisible' in hostui

sources={
'android/content/Context.java':'''package android.content;public class Context {}''',
'android/util/Log.java':'''package android.util;public class Log {public static int w(String a,String b,Throwable t){return 0;}}''',
# The Bluetooth API double uses real localhost sockets to exercise the actual
# adapter and shared client/server, including blocking close. No radio/OS pairing.
'android/bluetooth/BluetoothAdapter.java':'''package android.bluetooth;
import java.util.*;import java.util.concurrent.*;import java.io.*;
public class BluetoothAdapter {
 public final String address;public boolean enabled=true,denied,blockConnect;public UUID lastUuid;public String listenerThread;
 static final ConcurrentHashMap<String,BluetoothServerSocket> services=new ConcurrentHashMap<>();
 public BluetoothAdapter(String address){this.address=address;}
 public boolean isEnabled(){if(denied)throw new SecurityException();return enabled;}
 public BluetoothServerSocket listenUsingRfcommWithServiceRecord(String name,UUID uuid)throws IOException{
  if(denied)throw new SecurityException();lastUuid=uuid;listenerThread=Thread.currentThread().getName();
  BluetoothServerSocket s=new BluetoothServerSocket(address+uuid);services.put(address+uuid,s);return s;
 }
 public BluetoothDevice getRemoteDevice(String address){if(denied)throw new SecurityException();return new BluetoothDevice(this,address);}
}''',
'android/bluetooth/BluetoothDevice.java':'''package android.bluetooth;
import java.util.*;public class BluetoothDevice {final BluetoothAdapter adapter;final String address;
 BluetoothDevice(BluetoothAdapter a,String d){adapter=a;address=d;}
 public BluetoothSocket createRfcommSocketToServiceRecord(UUID uuid){adapter.lastUuid=uuid;return new BluetoothSocket(adapter,address+uuid);}
}''',
'android/bluetooth/BluetoothServerSocket.java':'''package android.bluetooth;
import java.net.*;import java.io.*;
public class BluetoothServerSocket implements Closeable {
 final ServerSocket server;final String key;public BluetoothServerSocket(String key)throws IOException{this.key=key;server=new ServerSocket(0,8,InetAddress.getLoopbackAddress());}
 public BluetoothSocket accept()throws IOException{return new BluetoothSocket(server.accept());}
 public void close()throws IOException{BluetoothAdapter.services.remove(key,this);server.close();}
}''',
'android/bluetooth/BluetoothSocket.java':'''package android.bluetooth;
import java.net.*;import java.io.*;
public class BluetoothSocket implements Closeable {
 Socket socket;BluetoothAdapter adapter;String key;volatile boolean closed;
 BluetoothSocket(Socket socket){this.socket=socket;}
 BluetoothSocket(BluetoothAdapter adapter,String key){this.adapter=adapter;this.key=key;}
 public void connect()throws IOException{
  synchronized(this){while(adapter.blockConnect&&!closed)try{wait();}catch(InterruptedException e){throw new IOException(e);}if(closed)throw new IOException("closed");socket=new Socket();}
  BluetoothServerSocket target=BluetoothAdapter.services.get(key);if(target==null)throw new IOException("No matching service UUID");
  socket.connect(new InetSocketAddress(InetAddress.getLoopbackAddress(),target.server.getLocalPort()),1000);
 }
 public InputStream getInputStream()throws IOException{
  InputStream input=socket.getInputStream();return new FilterInputStream(input){public int read(byte[] b,int off,int len)throws IOException{return super.read(b,off,Math.min(len,3));}};
 }
 public OutputStream getOutputStream()throws IOException{return socket.getOutputStream();}
 public synchronized void close()throws IOException{closed=true;notifyAll();if(socket!=null)socket.close();}
}''',
'BluetoothTransportCheck.java':'''import com.xenris.liquidwarsos.net.*;
import com.xenris.liquidwarsos.server.*;import com.xenris.liquidwarsos.Client;
import android.content.Context;import android.bluetooth.*;
import java.io.*;import java.net.*;import java.nio.*;import java.util.*;import java.util.concurrent.*;import java.util.concurrent.atomic.*;
public class BluetoothTransportCheck {
 static void yes(boolean v,String m){if(!v)throw new AssertionError(m);}
 static void expectIo(Throwing body)throws Exception{try{body.run();throw new AssertionError("IOException required");}catch(IOException expected){}}
 interface Throwing{void run()throws Exception;}
 static void await(java.util.function.BooleanSupplier condition,String name)throws Exception{long end=System.nanoTime()+TimeUnit.SECONDS.toNanos(3);while(!condition.getAsBoolean()&&System.nanoTime()<end)Thread.sleep(3);yes(condition.getAsBoolean(),name);}
 static void wire()throws Exception {
  int[] payload={0x72,-1,Integer.MIN_VALUE,Integer.MAX_VALUE};ByteArrayOutputStream bytes=new ByteArrayOutputStream();GameWire.write(bytes,payload.length,payload);
  byte[] expected=ByteBuffer.allocate(1+payload.length*4).put((byte)(payload.length*4)).putInt(0x72).putInt(-1).putInt(Integer.MIN_VALUE).putInt(Integer.MAX_VALUE).array();
  yes(Arrays.equals(bytes.toByteArray(),expected),"Original wire bytes");
  InputStream fragments=new FilterInputStream(new ByteArrayInputStream(expected)){int turn;public int read(byte[] b,int off,int len)throws IOException{if(turn++%3==0)return 0;return super.read(b,off,Math.min(len,2));}};
  yes(Arrays.equals(GameWire.read(fragments),payload),"Fragmented/zero-progress stream");yes(GameWire.read(fragments)==null,"Clean EOF");
  for(int invalid:new int[]{0,1,3,255})expectIo(()->GameWire.read(new ByteArrayInputStream(new byte[]{(byte)invalid})));
  expectIo(()->GameWire.read(new ByteArrayInputStream(new byte[]{8,0,0,0,1})));expectIo(()->GameWire.write(bytes,64,new int[64]));
  int[] largest=new int[63];for(int n=0;n<63;n++)largest[n]=n*65537;bytes.reset();GameWire.write(bytes,63,largest);yes(Arrays.equals(GameWire.read(new ByteArrayInputStream(bytes.toByteArray())),largest),"63-int gameplay boundary");
  ByteArrayOutputStream concurrent=new ByteArrayOutputStream();ArrayList<Thread> writers=new ArrayList<>();
  for(int p=0;p<4;p++){final int id=p;Thread t=new Thread(()->{try{for(int n=0;n<50;n++)GameWire.write(concurrent,3,new int[]{0x71,id,n});}catch(IOException e){throw new RuntimeException(e);}});writers.add(t);t.start();}
  for(Thread writer:writers)writer.join();InputStream in=new ByteArrayInputStream(concurrent.toByteArray());Set<String> seen=new HashSet<>();int[] frame;
  while((frame=GameWire.read(in))!=null){yes(frame.length==3&&frame[0]==0x71,"Atomic concurrent packets");seen.add(frame[1]+":"+frame[2]);}yes(seen.size()==200,"No lost/interleaved writes");
 }
 static void permissionsAndPeers(){
  for(int sdk:new int[]{26,30})for(BluetoothPermissions.Action a:BluetoothPermissions.Action.values())yes(BluetoothPermissions.required(sdk,a).length==0,"Legacy system pairing has no runtime/location permission");
  for(int sdk:new int[]{31,34}){
   yes(BluetoothPermissions.required(sdk,BluetoothPermissions.Action.CONNECT).length==1,"Connect only");
   yes(BluetoothPermissions.required(sdk,BluetoothPermissions.Action.SCAN).length==2,"Scan scoped permission");
   yes(BluetoothPermissions.required(sdk,BluetoothPermissions.Action.HOST).length==2,"Host scoped permission");
  }
  BluetoothPeers peers=new BluetoothPeers();peers.put(new BluetoothPeers.Peer("aa:bb:cc:dd:ee:01","Phone",false));peers.put(new BluetoothPeers.Peer("AA:BB:CC:DD:EE:01","Renamed",true));
  peers.put(new BluetoothPeers.Peer("AA:BB:CC:DD:EE:02","Phone",false));yes(peers.snapshot().size()==2&&peers.snapshot().get(0).name.equals("Renamed"),"Address dedup and paired priority");
  for(int i=0;i<100;i++)peers.put(new BluetoothPeers.Peer(String.format("AA:BB:CC:DD:00:%02X",i),"Other",false));yes(peers.snapshot().size()==64,"Bounded Bluetooth list");
 }
 static class Host extends Context implements Server.ServerCallbacks {
  Server server;final BlockingQueue<int[]> messages=new LinkedBlockingQueue<>();final AtomicInteger lastArgc=new AtomicInteger();final AtomicInteger failures=new AtomicInteger();
  public void onListeningChanged(boolean ready){if(!ready)failures.incrementAndGet();}
  public void onClientConnected(int id){server.sendToOne(id,0x7E,1);}
  public void onClientDisconnected(int id){}
  public void onClientMessageReceived(int id,int argc,int[] args){lastArgc.set(argc);messages.add(args);}
 }
 static class Peer implements Client.ClientCallbacks {
  final CountDownLatch made=new CountDownLatch(1),failed=new CountDownLatch(1),closed=new CountDownLatch(1);final BlockingQueue<int[]> messages=new LinkedBlockingQueue<>();volatile int id;
  public void onServerConnectionMade(int id,String address){this.id=id;made.countDown();}
  public void onServerConnectionFailed(String address){failed.countDown();}
  public void onServerConnectionClosed(String address){closed.countDown();}
  public void onServerMessageReceived(int argc,int[] args){yes(argc==args.length,"Client int argc");messages.add(args);}
 }
 static void exchange(Host host,Client client,Peer peer)throws Exception {
  client.start();yes(peer.made.await(3,TimeUnit.SECONDS),"Connection and legacy ID handshake");
  yes(Arrays.equals(peer.messages.poll(3,TimeUnit.SECONDS),new int[]{0x7E,1}),"Initial setup frame before peer reader");
  int[] step=new int[63];step[0]=0x72;for(int i=1;i<63;i++)step[i]=i%3==0?-1:i;
  client.send(63,step);yes(Arrays.equals(host.messages.poll(3,TimeUnit.SECONDS),step),"Client-to-host exact gameplay payload");yes(host.lastArgc.get()==252,"Legacy server byte argc preserved");
  host.server.sendToAll(63,step);yes(Arrays.equals(peer.messages.poll(3,TimeUnit.SECONDS),step),"Host-to-client exact gameplay payload");
  host.server.stopAccepting();host.server.startAccepting();await(host.server::isAccepting,"Reopened listener with retained peer");
  client.send(2,new int[]{0x79,0});yes(Arrays.equals(host.messages.poll(3,TimeUnit.SECONDS),new int[]{0x79,0}),"Connected stream survives listener close/reopen");
 }
 static void tcp()throws Exception {
  int port;try(ServerSocket free=new ServerSocket(0)){port=free.getLocalPort();}Host host=new Host();host.server=new Server(host,port);host.server.startAccepting();await(host.server::isAccepting,"TCP ready");
  Peer peer=new Peer();Client client=new Client(peer,"127.0.0.1",port);
  try{
   exchange(host,client,peer);yes(host.server.clientCapacity()==5,"Wi-Fi five-client capacity");
   ArrayList<Socket> extras=new ArrayList<>();try{
    for(int n=0;n<4;n++){Socket socket=new Socket(InetAddress.getLoopbackAddress(),port);socket.setSoTimeout(2000);extras.add(socket);yes(socket.getInputStream().read()>0,"Additional TCP ID");GameWire.read(socket.getInputStream());}
    await(()->host.server.connectedClientCount()==5,"Five active Wi-Fi clients");
    try(Socket full=new Socket(InetAddress.getLoopbackAddress(),port)){full.setSoTimeout(2000);yes(full.getInputStream().read()==-1,"Capacity rejects sixth client");}
   }finally{for(Socket socket:extras)socket.close();}
   host.server.destroy();yes(peer.closed.await(3,TimeUnit.SECONDS),"Server loss callback");
  }finally{client.destroy();client.join(2000);host.server.destroy();yes(!client.isAlive(),"TCP worker stopped");}
 }
 static void bluetooth()throws Exception {
  BluetoothAdapter hostAdapter=new BluetoothAdapter("AA:BB:CC:DD:EE:01"),guestAdapter=new BluetoothAdapter("AA:BB:CC:DD:EE:02");
  Host host=new Host();host.server=new Server(host,BluetoothTransport.listen(hostAdapter));host.server.startAccepting();await(host.server::isAccepting,"RFCOMM listener ready via API double");
  yes(host.server.clientCapacity()==1,"Bluetooth two-phone limit");yes(hostAdapter.listenerThread.contains("room listener"),"SDP/listen work off UI thread");
  Peer peer=new Peer();Client client=new Client(peer,"AA:BB:CC:DD:EE:01",new BluetoothTransport.Connector(guestAdapter,"AA:BB:CC:DD:EE:01"));
  try{
   exchange(host,client,peer);yes(BluetoothTransport.SERVICE_UUID.equals(hostAdapter.lastUuid)&&hostAdapter.lastUuid.equals(guestAdapter.lastUuid),"Same secure service UUID");
   Peer extraPeer=new Peer();Client extra=new Client(extraPeer,"AA:BB:CC:DD:EE:01",new BluetoothTransport.Connector(guestAdapter,"AA:BB:CC:DD:EE:01"));extra.start();
   yes(extraPeer.failed.await(3,TimeUnit.SECONDS),"Bluetooth room full rejected");extra.destroy();extra.join(2000);
   host.server.destroy();yes(peer.closed.await(3,TimeUnit.SECONDS),"RFCOMM link loss propagates");
  }finally{client.destroy();client.join(2000);host.server.destroy();}
  BluetoothAdapter blocked=new BluetoothAdapter("AA:BB:CC:DD:EE:03");blocked.blockConnect=true;
  BluetoothTransport.Connector pending=new BluetoothTransport.Connector(blocked,"AA:BB:CC:DD:EE:01");Peer cancelled=new Peer();Client connecting=new Client(cancelled,"test",pending);connecting.start();Thread.sleep(30);connecting.destroy();connecting.join(2000);yes(!connecting.isAlive()&&cancelled.failed.getCount()==1,"Explicit cancel closes blocking connect without stale callback");
  BluetoothTransport.Connector slow=new BluetoothTransport.Connector(blocked,"AA:BB:CC:DD:EE:01");GameTransport.Connector deadline=new GameTransport.Connector(){public GameTransport.Connection connect()throws IOException{return slow.connect();}public void close()throws IOException{slow.close();}public long timeoutMillis(){return 40;}};
  Peer timed=new Peer();Client timeout=new Client(timed,"test",deadline);timeout.start();yes(timed.failed.await(2,TimeUnit.SECONDS),"Connection deadline releases blocking SDK connect");timeout.join(2000);
  BluetoothAdapter denied=new BluetoothAdapter("AA:BB:CC:DD:EE:04");denied.denied=true;expectIo(()->BluetoothTransport.listen(denied).open());expectIo(()->new BluetoothTransport.Connector(denied,"AA:BB:CC:DD:EE:01").connect());
 }
 static void lateListener()throws Exception {
  Host host=new Host();CountDownLatch opening=new CountDownLatch(1),release=new CountDownLatch(1);AtomicInteger opens=new AtomicInteger(),closes=new AtomicInteger();
  GameTransport.ListenerFactory factory=()->{int n=opens.incrementAndGet();if(n==1){opening.countDown();try{release.await();}catch(InterruptedException e){throw new IOException(e);}}return new GameTransport.Listener(){volatile boolean stop;public GameTransport.Connection accept()throws IOException{while(!stop)try{Thread.sleep(2);}catch(InterruptedException ignored){}throw new IOException("closed");}public void close(){stop=true;closes.incrementAndGet();}};};
  host.server=new Server(host,factory);host.server.startAccepting();yes(opening.await(2,TimeUnit.SECONDS),"Opening started");host.server.stopAccepting();host.server.startAccepting();await(host.server::isAccepting,"New listener ready");release.countDown();await(()->closes.get()>=1,"Late old listener released");yes(host.server.isAccepting(),"Old completion cannot clear new listener");host.server.destroy();
 }
 public static void main(String[] args)throws Exception {
  wire();System.out.println("PASS original frame bytes, 63-int boundary, fragments/zero reads/EOF/invalid sizes, 200 concurrent packets");
  permissionsAndPeers();System.out.println("PASS API26/30/31/34 permission plans, no location, paired priority/address dedup/cache bounds");
  tcp();System.out.println("PASS actual localhost TCP Client/Server/SubServer, bidirectional game frames, listener restart, five clients and disconnect");
  bluetooth();System.out.println("PASS actual RFCOMM adapter with localhost-backed Android API doubles, secure UUID, single client, cancellation/timeout/permission denial");
  lateListener();System.out.println("PASS stop/restart while open is pending; old listener cannot overwrite new state");
  System.out.println("NOT_TESTED: real Bluetooth radio, pairing/permissions UI, phones, game synchronization, latency/distance/battery");
 }
}'''}
for name,source in sources.items():
    p=BUILD/name;p.parent.mkdir(parents=True,exist_ok=True);p.write_text(source,encoding='utf8')
classes=BUILD/'classes';classes.mkdir(parents=True,exist_ok=True)
actual=['Client','server/Server','server/SubServer','server/LanRoom',*(f'net/{n}' for n in ['GameTransport','GameWire','TcpTransport','BluetoothTransport','BluetoothPermissions','BluetoothPeers'])]
files=[str(BUILD/name) for name in sources]+[str(SRC/(name+'.java')) for name in actual]
subprocess.run([str(JAVA/'javac.exe'),'-encoding','UTF-8','-d',str(classes),*files],check=True)
subprocess.run([str(JAVA/'java.exe'),'-cp',str(classes),'BluetoothTransportCheck'],check=True,timeout=40)
report=dict(status='PASS',baseline='543830a/1.50-QA',baseline_files=len(baseline),changed_files=changed,
 wire_bytes='PASS',tcp_integration='PASS',rfcomm_android_api_doubles='PASS',cancel_timeout='PASS',permission_plan='PASS',
 native_maps_rules_touch_wifi_discovery_result_unchanged=True,localized_keys=sorted(keys),
 device='NOT_TESTED',bluetooth_radio='NOT_TESTED',pairing_ui='NOT_TESTED',actual_ui='NOT_TESTED',multi_device_game='NOT_TESTED',performance='NOT_TESTED')
(OUT/'focused-check.json').write_text(json.dumps(report,ensure_ascii=False,indent=2)+'\n',encoding='utf8')
print('PASS source invariants, resource/permission contracts. No new test framework or dependency.')
