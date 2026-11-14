package com.xenris.liquidwarsos.cloud;
import org.json.*;
import java.util.*;
import java.net.*;
import java.io.*;
import java.util.concurrent.*;

/** Real setup state and discovery code, using bounded local HTTPS substitutes, never real keys. */
public final class SetupCheck {
    static int tests;
    static void ok(boolean value,String why){tests++;if(!value)throw new AssertionError(why);}
    interface Attempt{void run()throws Exception;}
    static void rejects(Attempt a,String why)throws Exception{boolean failed=false;try{a.run();}catch(Exception e){failed=true;}ok(failed,why);}
    static ModelConfig config(int p){return new ModelConfig(true,p,"https://example.org/v1","","fixture-secret");}
    static String list(String... ids){JSONArray a=new JSONArray();for(String id:ids)a.put(new JSONObject().put("id",id));return new JSONObject().put("data",a).toString();}
    static String gemini(String id,String cursor){return new JSONObject().put("models",new JSONArray().put(new JSONObject().put("name","models/"+id).put("supportedGenerationMethods",new JSONArray().put("generateContent")))).put("nextPageToken",cursor).toString();}
    public static void main(String[] args)throws Exception{
        ok(ModelProviders.ALL.length==13,"11 providers + SimpleTool + custom");
        for(int i=0;i<11;i++){
            ModelProviders.Provider p=ModelProviders.ALL[i];ModelSetupDraft d=new ModelSetupDraft(ModelConfig.empty());d.provider(i);
            ok(d.value().base.equals(p.base)&&d.value().protocol==p.protocol,"provider sets endpoint + protocol");
            ok(d.value().enabled&&d.value().key.isEmpty(),"provider starts without other key");
            ModelConfig entered=new ModelConfig(true,p.protocol,p.base,d.value().model,"fixture-secret");d.edit(entered);
            long epoch=d.revision();ok(d.directory(epoch,Arrays.asList("ordinary-chat","fast-mini")),"provider list applied");
            ok(!d.value().model.isEmpty(),"no model typing needed");d.value().validate();
        }
        ModelSetupDraft simple=new ModelSetupDraft(ModelConfig.empty());simple.provider(11);
        ok(simple.value().protocol==ModelConfig.SIMPLETOOL&&simple.value().model.equals("server-loaded"),"SimpleTool server model default");
        simple.edit(new ModelConfig(true,4,"https://self.example","server-loaded",""));simple.value().validate();
        ok(ModelProviders.find(simple.value())==11,"custom SimpleTool endpoint keeps provider identity");
        ModelDirectory.Result health=new ModelDirectory(u->{ok(u.getPath().equals("/health"),"server health model route");return new Check.Connection(u,200,"{\"status\":\"ok\",\"loaded\":true,\"model\":\"./models/RT-Qwen3\"}");}).fetch(simple.value());
        ok(health.models.equals(Arrays.asList("./models/RT-Qwen3")),"loaded model, not a false selectable catalogue");
        ModelSetupDraft blank=new ModelSetupDraft(ModelConfig.empty());ok(!blank.dirty()&&!blank.value().enabled,"empty teams remain local");ok(blank.value().model.equals("deepseek-flash"),"official Flash default");
        ModelConfig saved=new ModelConfig(false,ModelConfig.CLAUDE,"https://custom.example/v2","my-personal-model","fixture-saved-key");
        ModelSetupDraft d=new ModelSetupDraft(saved);ok(ModelSetupDraft.same(d.value(),saved),"legacy custom configuration preserved, including disabled key");
        long old=d.revision();d.edit(new ModelConfig(true,saved.protocol,saved.base,saved.model,"replacement-key"));
        ok(!d.directory(old,Arrays.asList("wrong-result")),"late key result rejected");
        ok(d.value().model.equals(saved.model),"refresh retains custom selection");
        d.edit(new ModelConfig(true,0,"https://different.example/v1","custom-model",d.value().key));ok(d.value().key.isEmpty(),"advanced endpoint change clears key");
        d.edit(new ModelConfig(true,0,d.value().base,"custom-model","new-fixture-key"));d.provider(2);ok(d.value().key.isEmpty(),"provider switch cannot leak prior key");
        ok(d.dirty(),"draft remembers unsaved state");d.saved();ok(!d.dirty(),"save clears dirty flag");
        ModelSetupDraft[] teams=new ModelSetupDraft[6];for(int i=0;i<6;i++)teams[i]=new ModelSetupDraft(ModelConfig.empty());
        teams[0].edit(new ModelConfig(true,0,teams[0].value().base,"deepseek-v4-pro","first-key"));teams[1].provider(1);teams[1].edit(new ModelConfig(true,1,teams[1].value().base,"second-model","second-key"));
        ok(teams[0].value().key.equals("first-key")&&teams[0].value().model.equals("deepseek-v4-pro"),"switching teams preserves first draft");
        ok(teams[1].value().key.equals("second-key")&&teams[2].value().key.isEmpty(),"independent team credentials");
        ok(ModelProviders.recommend(Arrays.asList("deepseek-v4-pro","deepseek-flash")).equals("deepseek-flash"),"rank default from available list");
        ok(ModelProviders.recommend(Arrays.asList("only-model")).equals("only-model"),"never invent absent model");

        for(int protocol=0;protocol<4;protocol++){
            final int type=protocol;List<Check.Connection> connections=new ArrayList<>();
            ModelDirectory directory=new ModelDirectory(u->{Check.Connection c=new Check.Connection(u,200,type==3?gemini("gemini-fast",""):list("text-mini","text-pro","embedding-v1"));connections.add(c);return c;});
            ModelDirectory.Result result=directory.fetch(config(protocol));Check.Connection c=connections.get(0);
            ok(!result.models.isEmpty()&&!result.models.contains("embedding-v1"),"text model filtering");
            ok(c.getRequestMethod().equals("GET")&&c.sent.size()==0,"no generation, no prompt, no output body");
            ok(c.headers.get(type==3?"x-goog-api-key":type==2?"x-api-key":"Authorization").contains("fixture-secret"),"protocol auth header");
            ok(!c.getURL().toString().contains("fixture-secret"),"no credential in URL");
            ok(!c.getInstanceFollowRedirects()&&!c.getUseCaches()&&c.disconnected,"redirect/cache off, connection closed");
            ok(c.getConnectTimeout()==5000&&c.getReadTimeout()==8000,"bounded socket timeouts");
        }
        List<URL> urls=new ArrayList<>();ModelDirectory.Result paged=new ModelDirectory(u->{urls.add(u);return new Check.Connection(u,200,gemini(urls.size()==1?"gemini-pro":"gemini-flash",urls.size()==1?"next / +":""));}).fetch(config(3));
        ok(paged.models.size()==2&&!paged.truncated,"Gemini follows bounded pagination");ok(urls.get(1).toString().contains("next+%2F+%2B"),"cursor encoded, fixed origin");
        int[] pages={0};ModelDirectory.Result bounded=new ModelDirectory(u->{pages[0]++;return new Check.Connection(u,200,gemini("gemini-"+pages[0],"page"+pages[0]));}).fetch(config(3));
        ok(pages[0]==3&&bounded.truncated,"three-page cap is visible");
        List<URL> claudeUrls=new ArrayList<>();new ModelDirectory(u->{claudeUrls.add(u);return new Check.Connection(u,200,new JSONObject(list("claude-haiku")).put("has_more",claudeUrls.size()==1).put("last_id","claude-haiku").toString());}).fetch(config(2));
        ok(claudeUrls.size()==2&&claudeUrls.get(1).toString().contains("after_id=claude-haiku"),"Claude cursor");
        rejects(()->new ModelDirectory(u->new Check.Connection(u,200,"{} ")).fetch(config(0)),"bad list rejected");
        rejects(()->new ModelDirectory(u->new Check.Connection(u,200,list("embedding-only","image-model"))).fetch(config(0)),"non-chat-only list rejected");
        rejects(()->new ModelDirectory(u->new Check.Connection(u,302,"redirect" )).fetch(config(0)),"no redirect credential forwarding");
        rejects(()->new ModelDirectory(u->new Check.Connection(u,200," ".repeat(262145))).fetch(config(0)),"response size cap");
        rejects(()->new ModelDirectory(u->new Check.Connection(u,200,gemini("gemini-pro","same"))).fetch(config(3)),"repeated cursor rejected");
        for(int status:new int[]{401,403,404,429,500}){int[] count={0};try{new ModelDirectory(u->{count[0]++;return new Check.Connection(u,status,"DO NOT EXPOSE SERVER CONTENT");}).fetch(config(0));throw new AssertionError("HTTP failure expected");}catch(ModelHttp.Failure e){ok(e.status==status&&!e.toString().contains("SERVER CONTENT"),"bounded non-secret error");}ok(count[0]==1,"no auto retry");}
        int[] opened={0};ModelDirectory cancelled=new ModelDirectory(u->{opened[0]++;return new Check.Connection(u,200,list("chat"));});cancelled.cancel();rejects(()->cancelled.fetch(config(0)),"cancel before start");ok(opened[0]==0,"cancel before sending");
        CountDownLatch reading=new CountDownLatch(1),released=new CountDownLatch(1);Check.Connection[] live={null};
        ModelDirectory running=new ModelDirectory(u->{live[0]=new Check.Connection(u,200,list("chat")){
            @Override public InputStream getInputStream(){reading.countDown();try{released.await(2,TimeUnit.SECONDS);}catch(InterruptedException e){Thread.currentThread().interrupt();}return super.getInputStream();}
            @Override public void disconnect(){super.disconnect();released.countDown();}
        };return live[0];});
        ExecutorService worker=Executors.newSingleThreadExecutor();Future<?> task=worker.submit(()->{try{running.fetch(config(0));throw new AssertionError("cancel returned a list");}catch(IOException expected){}catch(Exception e){throw new RuntimeException(e);}});
        ok(reading.await(1,TimeUnit.SECONDS),"read in flight");running.cancel();task.get(2,TimeUnit.SECONDS);ok(live[0].disconnected,"cancel closes live socket and rejects result");worker.shutdownNow();
        String[] many=new String[600];for(int i=0;i<many.length;i++)many[i]="text-"+i;
        ModelDirectory.Result limited=new ModelDirectory(u->new Check.Connection(u,200,list(many))).fetch(config(0));ok(limited.models.size()==512&&limited.truncated,"large valid catalogue remains usable, with truncation flag");
        ModelConfig official=new ModelConfig(true,1,"https://api.openai.com/v1","","key");
        JSONObject sample=new JSONObject(list("gpt-4.1-mini","whisper-1","gpt-image-1","gpt-realtime","o3-mini","davinci-002"));sample.getJSONArray("data").put(new JSONObject().put("id","gpt-retired").put("shutdown_date","2000-01-01"));
        ok(ModelDirectory.parse(official,sample).equals(Arrays.asList("gpt-4.1-mini","o3-mini")),"OpenAI excludes other endpoint/retired models");
        JSONObject g=new JSONObject().put("models",new JSONArray().put(new JSONObject().put("name","models/gemini-embedding").put("supportedGenerationMethods",new JSONArray().put("embedContent"))).put(new JSONObject().put("name","models/gemini-text").put("supportedGenerationMethods",new JSONArray().put("generateContent"))));
        ok(ModelDirectory.parse(config(3),g).equals(Arrays.asList("gemini-text")),"Gemini supported generation method");
        ok(ModelDirectory.parse(config(0),new JSONObject(list("chat-a","chat-a","bad id\n"))).equals(Arrays.asList("chat-a")),"sanitize and deduplicate IDs");
        for(String unsafe:new String[]{"http://example.org","https://user:secret@example.org","https://example.org?key=oops"})rejects(()->new ModelDirectory(u->{throw new AssertionError("unsafe request opened");}).fetch(new ModelConfig(true,0,unsafe,"","key")),"validate before any connection");
        System.out.println("PASS "+tests+" setup/discovery assertions; actual JVM code + mock HTTPS. No real API, phone, emulator or Android UI.");
    }
}
