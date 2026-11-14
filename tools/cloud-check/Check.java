package com.xenris.liquidwarsos.cloud;
import org.json.*;import java.io.*;import java.net.*;import java.util.*;import java.util.concurrent.*;import java.util.concurrent.atomic.*;import javax.net.ssl.*;
import com.xenris.liquidwarsos.AllianceRules;
public final class Check {
 static int tests;static void ok(boolean v,String why){tests++;if(!v)throw new AssertionError(why);}
 interface Attempt{void run()throws Exception;}
 static void rejects(Attempt a,String why)throws Exception{boolean rejected=false;try{a.run();}catch(Exception e){rejected=true;}ok(rejected,why);}
 static final String ACTION="{\"points\":[[120,160],[200,240]],\"hold_ms\":500}";
 static String body(int p,String action){
  if(p==0)return new JSONObject().put("choices",new JSONArray().put(new JSONObject().put("finish_reason","stop").put("message",new JSONObject().put("content",action)))).toString();
  if(p==1)return new JSONObject().put("status","completed").put("output",new JSONArray().put(new JSONObject().put("type","message").put("content",new JSONArray().put(new JSONObject().put("type","output_text").put("text",action))))).toString();
  if(p==2)return new JSONObject().put("stop_reason","end_turn").put("content",new JSONArray().put(new JSONObject().put("type","text").put("text",action))).toString();
  return new JSONObject().put("candidates",new JSONArray().put(new JSONObject().put("finishReason","STOP").put("content",new JSONObject().put("parts",new JSONArray().put(new JSONObject().put("text",action)))))).toString();
 }
 static String event(int p,String text){
  if(p==0)return new JSONObject().put("choices",new JSONArray().put(new JSONObject().put("delta",new JSONObject().put("content",text)))).toString();
  if(p==1)return new JSONObject().put("type","response.output_text.delta").put("delta",text).toString();
  if(p==2)return new JSONObject().put("type","content_block_delta").put("delta",new JSONObject().put("type","text_delta").put("text",text)).toString();
  return new JSONObject().put("candidates",new JSONArray().put(new JSONObject().put("content",new JSONObject().put("parts",new JSONArray().put(new JSONObject().put("text",text)))))).toString();
 }
 static class Connection extends HttpsURLConnection {
  final String response;final int code;boolean disconnected;ByteArrayOutputStream sent=new ByteArrayOutputStream();Map<String,String> headers=new HashMap<>();
  Connection(URL u,int code,String response){super(u);this.code=code;this.response=response;}
  public void connect(){}public void disconnect(){disconnected=true;}public boolean usingProxy(){return false;}public String getCipherSuite(){return "TEST";}public java.security.cert.Certificate[] getLocalCertificates(){return null;}public java.security.cert.Certificate[] getServerCertificates(){return null;}
  public void setRequestProperty(String n,String v){headers.put(n,v);}public OutputStream getOutputStream(){return sent;}public int getResponseCode(){return code;}public InputStream getInputStream(){return new ByteArrayInputStream(response.getBytes(java.nio.charset.StandardCharsets.UTF_8));}
 }

 static ModelConfig conf(int proto,String model){return new ModelConfig(true,proto,"https://example.org/v1",model,"test-fixture-key");}
 static int[] snapshot(){int[] s=new int[ModelWorld.HEADER];for(int p=0;p<6;p++){s[p*8]=400;s[p*8+1]=100;s[p*8+2]=p*100;s[p*8+4]=p*100;s[p*8+6]=50;s[48+p*12+2]=50;}return s;}
 static void idle(ModelMatch m)throws Exception{for(int i=0;i<600;i++){if(!m.summary(false).contains("waiting")){m.tickExecution();return;}Thread.sleep(5);}throw new AssertionError("request did not finish");}
 static ModelConfig[] configs(){ModelConfig[] c=new ModelConfig[6];for(int p=0;p<6;p++)c[p]=conf(0,"team-"+p);return c;}
 static ModelMatch match(ModelConfig[] c,AtomicLong time,ModelHttp.Connector connector){ModelMatch m=new ModelMatch(c,new int[]{0,0,1,1,2,2},time::get,()->new ModelHttp(connector));m.terrain(new byte[800*480]);m.pause(false);return m;}
 public static void main(String[] args)throws Exception{
  for(int p=0;p<4;p++){
   final int proto=p;ModelConfig c=conf(p,"test-model");ModelStream stream=new ModelStream(p);
   for(int i=0;i<ACTION.length();i++){ModelAction a=stream.event(event(p,ACTION.substring(i,i+1)));ok((a!=null)==(i==ACTION.length()-1),"only complete streamed JSON action executes");if(a!=null)ok(a.points()[0]==120&&a.points()[6]==240,"five finger coordinate layout");}
   String events="data: "+event(p,ACTION.substring(0,15))+"\n\n: heartbeat\n\ndata: "+event(p,ACTION.substring(15))+"\n\n";
   Connection conn=new Connection(new URL(ModelProtocol.url(c)),200,events){public String getContentType(){return "text/event-stream; charset=utf-8";}public InputStream getInputStream(){return new ByteArrayInputStream(response.getBytes(java.nio.charset.StandardCharsets.UTF_8)){public synchronized int read(){int n=super.read();if(n<0)throw new AssertionError("must return first complete action without waiting for final event");return n;}};}};
   ModelAction a=new ModelHttp(u->conn).call(c,"{}","terrain",false);ok(a.points()[0]==120&&conn.disconnected,"SSE transport ends promptly");ok(!conn.getInstanceFollowRedirects(),"no redirect auth leak");
   JSONObject sent=new JSONObject(conn.sent.toString("UTF-8"));ok(p==3?conn.getURL().toString().contains(":streamGenerateContent?alt=sse"):sent.getBoolean("stream"),"stream enabled per protocol");
   ok(conn.headers.get(p==2?"x-api-key":p==3?"x-goog-api-key":"Authorization").contains("test-fixture-key"),"auth headers");ok(!sent.toString().contains("test-fixture-key"),"no key in prompt");
   ok(ModelProtocol.parse(c,body(p,ACTION)).points()[1]==200,"nonstream compatible fallback");
   rejects(()->ModelProtocol.parse(c,body(proto,ACTION).replace("stop","length").replace("end_turn","max_tokens").replace("STOP","MAX_TOKENS").replace("completed","incomplete")),"truncated fallback rejected");
   rejects(()->new ModelHttp(u->new Connection(u,200,"x".repeat(65537))).call(c,"{}","",false),"bounded body");
  }
  for(String bad:new String[]{"{\"points\":[[800,2]],\"hold_ms\":500}","{\"points\":[[2,480]],\"hold_ms\":500}","{\"points\":[[-1,2]],\"hold_ms\":500}","{\"points\":[[2.5,2]],\"hold_ms\":500}","{\"points\":[[2,2,3]],\"hold_ms\":500}","{\"points\":[],\"hold_ms\":99}","{\"points\":[],\"hold_ms\":2001}","{\"points\":[[1,1],[2,2],[3,3],[4,4],[5,5],[6,6]],\"hold_ms\":500}"})rejects(()->ModelAction.parse(new JSONObject(bad)),"invalid action bounds");
  ModelAction release=ModelAction.parse(new JSONObject("{\"points\":[],\"hold_ms\":100}"));ok(release.points()[0]==-1,"empty action releases fingers");short[] mutable=release.points();mutable[0]=500;ok(release.points()[0]==-1,"action immutable");
  byte[] walls=new byte[384000];walls[160*800+120]=1;ModelTerrain terrain=new ModelTerrain(walls);walls[160*800+120]=0;ok(!terrain.allows(ModelAction.parse(new JSONObject(ACTION)))&&terrain.allows(release),"exact immutable collision mask");
  ok(terrain.description.contains("STATIC_NAVIGATION")&&!terrain.description.contains("mixed cells"),"collision navigation replaces ambiguous character grid");ok(ModelStream.completeObject("```json\n"+ACTION+"\n```").equals(ACTION),"fenced response");ok(ModelStream.completeObject("{\"points\":[")==null,"partial cannot execute");rejects(()->ModelStream.completeObject("run code"),"prose rejected");
  String[] reasoning={"{\"choices\":[{\"delta\":{\"reasoning_content\":\"garbage\"}}]}","{\"type\":\"response.reasoning_text.delta\",\"delta\":\"garbage\"}","{\"type\":\"content_block_delta\",\"delta\":{\"type\":\"thinking_delta\",\"thinking\":\"garbage\"}}","{\"candidates\":[{\"content\":{\"parts\":[{\"thought\":true,\"text\":\"garbage\"}]}}]}"};for(int p=0;p<4;p++)ok(new ModelStream(p).event(reasoning[p])==null,"reasoning ignored");
  ModelConfig simple=new ModelConfig(true,4,"https://self.example","server-loaded","");String simpleRequest=ModelProtocol.request(simple,"{}","");int pos=-1;for(String key:new String[]{"p1","p2","p3","p4","p5","hold_ms"}){int at=simpleRequest.indexOf("\""+key+"\":",simpleRequest.indexOf("\"properties\""));ok(at>pos,"positional SimpleTool schema ordering");pos=at;}
  String argsJson="{\"p1\":\"120,160\",\"p2\":\"-\",\"p3\":\"-\",\"p4\":\"-\",\"p5\":\"200,240\",\"hold_ms\":500}";
  String reply="{\"success\":true,\"function\":\"move_points\",\"args\":"+argsJson+"}";AtomicInteger healthCalls=new AtomicInteger();
  ModelAction sa=new ModelHttp(u->{healthCalls.incrementAndGet();return new Connection(u,200,u.getPath().equals("/health")?"{\"status\":\"ok\",\"loaded\":true,\"model\":\"server-model\"}":reply);}).call(simple,"{}","",true);ok(sa.points()[1]==200&&healthCalls.get()==2,"SimpleTool health plus direct function");
  rejects(()->new ModelHttp(u->new Connection(u,200,"{\"status\":\"ok\",\"loaded\":true,\"model\":\"wrong\"}")).call(new ModelConfig(true,4,simple.base,"expected",""),"{}","",true),"server model mismatch rejected before action");
  for(String url:new String[]{"http://example.org","https://user:pw@example.org","https://example.org?key=test","https://example.org/#x"})rejects(()->new ModelConfig(true,4,url,"server-loaded","").validate(),"HTTPS endpoint validated");
  AtomicLong now=new AtomicLong(100);ModelConfig[] c=configs();CountDownLatch all=new CountDownLatch(6),go=new CountDownLatch(1);AtomicInteger opened=new AtomicInteger();
  try(ModelMatch m=match(c,now,u->{opened.incrementAndGet();return new Connection(u,200,body(0,ACTION)){public InputStream getInputStream(){all.countDown();try{if(!go.await(2,TimeUnit.SECONDS))throw new AssertionError("six lanes not parallel");}catch(InterruptedException e){throw new RuntimeException(e);}return super.getInputStream();}};})){
   m.update(63,snapshot());ok(all.await(1,TimeUnit.SECONDS),"all six requests concurrent");for(int i=0;i<20;i++)m.update(63,snapshot());ok(opened.get()==6,"at most one in-flight per team");now.set(140);go.countDown();idle(m);ok(m.inputs().mask==63,"six direct actions accepted");m.submitted(m.inputs());ok(m.summary(false).contains("submitted 40ms"),"actual submission timing");ok(m.due(63),"no success interval");
   for(int round=0;round<35;round++){now.addAndGet(70);m.update(63,snapshot());idle(m);}ok(opened.get()==216,"no 30-call quota");now.addAndGet(501);ok(m.inputs().mask==0,"hold expiry independent of next request");m.pause(true);ok(!m.due(63)&&m.inputs().mask==0,"pause clears actions");m.pause(false);ok(m.due(63),"resume immediate fresh request");
  }
  ModelConfig[] one={c[0],ModelConfig.empty(),ModelConfig.empty(),ModelConfig.empty(),ModelConfig.empty(),ModelConfig.empty()};
  try(ModelMatch m=match(one,now,u->{now.addAndGet(5001);return new Connection(u,200,body(0,ACTION));})){m.update(1,snapshot());idle(m);ok(m.inputs().mask==0&&m.summary(false).contains("stale"),"stale reply cannot control");}
  try(ModelMatch m=match(one,now,u->new Connection(u,429,"secret body"){public String getHeaderField(String name){return "2";}})){m.update(1,snapshot());idle(m);ok(!m.due(1)&&m.summary(false).contains("HTTP 429")&&!m.summary(false).contains("secret"),"rate-limit visible, secret body hidden");now.addAndGet(2001);ok(m.due(1),"retry-after recovery");}
  for(int status:new int[]{400,401,402,403,404,422})try(ModelMatch m=match(one,now,u->new Connection(u,status,"secret"))){m.update(1,snapshot());idle(m);now.addAndGet(60001);ok(!m.due(1)&&m.inputs().mask==0,"invalid credentials/config stops this team");}
  try(ModelMatch m=match(one,now,u->new Connection(u,500,"secret"))){for(int i=0;i<4;i++){m.update(1,snapshot());idle(m);now.addAndGet(10001);}ok(m.due(1),"transient failures do not permanently disable after three");}
  CountDownLatch started=new CountDownLatch(1),releaseRead=new CountDownLatch(1);AtomicInteger cancellations=new AtomicInteger();
  try(ModelMatch m=match(one,now,u->new Connection(u,200,body(0,ACTION)){public InputStream getInputStream(){started.countDown();try{releaseRead.await(2,TimeUnit.SECONDS);}catch(InterruptedException e){}return super.getInputStream();}public void disconnect(){cancellations.incrementAndGet();releaseRead.countDown();}})){
   m.update(1,snapshot());ok(started.await(1,TimeUnit.SECONDS),"live read");m.pause(true);m.pause(false);releaseRead.countDown();for(int i=0;i<200&&!m.due(1);i++)Thread.sleep(5);ok(m.due(1)&&m.inputs().mask==0&&cancellations.get()>0,"late paused request cannot revive, next request unblocked");
   int[] eliminated=snapshot();eliminated[0]=0;m.update(1,eliminated);ok(!m.due(1)&&m.summary(false).contains("eliminated"),"eliminated stops requests");
  }
  try(ModelMatch m=match(c,now,u->new Connection(u,200,body(0,ACTION)))){m.update(31,snapshot());idle(m);ok(m.inputs().mask==31,"human team never overridden");m.close();ok(!m.due(63)&&m.inputs().mask==0,"closed disables all");}
  rejects(()->ModelAction.parse(new JSONObject("{\"enemy\":6,\"intent\":\"attack\",\"points\":[[1,1]],\"hold_ms\":2000}")),"invalid selected enemy");
  rejects(()->ModelAction.parse(new JSONObject("{\"enemy\":1,\"intent\":\"execute\",\"points\":[[1,1]],\"hold_ms\":2000}")),"only typed intent, never executable instructions");
  short[] none=new short[10];Arrays.fill(none,(short)-1);JSONObject observation=new JSONObject(ModelProtocol.observation(2,snapshot(),snapshot(),1000,new int[]{0,1,2,3,4,5},none,1));
  ok(observation.getJSONObject("YOU").getInt("id")==2&&observation.getJSONArray("ENEMIES").length()==5,"own army separated from enemies");
  for(int i=0;i<5;i++)ok(observation.getJSONArray("ENEMIES").getJSONObject(i).getInt("id")!=2,"own coordinates never labelled enemy");
  JSONObject ds=new JSONObject(ModelProtocol.request(new ModelConfig(true,0,"https://api.deepseek.com","deepseek-v4-pro","fixture"),"{}",""));ok(ds.getInt("max_tokens")==384&&ds.getDouble("temperature")==0&&ds.getJSONObject("thinking").getString("type").equals("disabled"),"bounded non-thinking route output with plan headroom");
  String allyAction=new JSONObject(ACTION).put("enemy",1).put("intent","attack").toString();
  try(ModelMatch m=match(one,now,u->new Connection(u,200,body(0,allyAction)))){m.update(1,snapshot());idle(m);ok(m.inputs().mask==0&&m.summary(false).contains("invalid enemy"),"ally cannot be declared attack target");ok(m.due(1),"semantic correction has no network backoff");}
  AtomicInteger timeoutClosed=new AtomicInteger();CountDownLatch timeoutRead=new CountDownLatch(1),timeoutRelease=new CountDownLatch(1);
  try(ModelMatch m=match(one,now,u->new Connection(u,200,body(0,ACTION)){public InputStream getInputStream(){timeoutRead.countDown();try{timeoutRelease.await(8,TimeUnit.SECONDS);}catch(InterruptedException e){}return super.getInputStream();}public void disconnect(){timeoutClosed.incrementAndGet();timeoutRelease.countDown();}})){
   m.update(1,snapshot());ok(timeoutRead.await(1,TimeUnit.SECONDS),"deadline request started");ok(timeoutRelease.await(8,TimeUnit.SECONDS),"real deadline disconnects stalled stream");idle(m);ok(timeoutClosed.get()>0&&m.inputs().mask==0,"deadline cannot yield late action");
  }
  for(String response:new String[]{"data: "+event(0,"{\"points\":[")+"\n\n", "data: "+"x".repeat(65537)+"\n\n"})rejects(()->new ModelHttp(u->new Connection(u,200,response){public String getContentType(){return "text/event-stream";}}).call(c[0],"{}","",false),"truncated and oversize SSE rejected");
  ok(AllianceRules.solo().length==6&&AllianceRules.pairings().size()==15,"alliance choices retained");
  System.out.println("PASS "+tests+" realtime assertions: actual Java parser/transport/scheduler, fake HTTPS and clock; no provider or device.");
 }
}
