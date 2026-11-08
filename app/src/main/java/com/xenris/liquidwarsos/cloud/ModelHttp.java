// SPDX-License-Identifier: GPL-3.0-or-later
package com.xenris.liquidwarsos.cloud;
import javax.net.ssl.HttpsURLConnection;
import org.json.JSONObject;
import java.net.URL;
import java.io.*;
import java.nio.charset.StandardCharsets;

/** One cancellable request; never logs secrets, responses or prompts. */
public final class ModelHttp {
    public interface Connector{HttpsURLConnection open(URL url)throws IOException;}
    public static final class Failure extends IOException{
        public final int status;public final long retryMs;
        public Failure(int status){this(status,0);}
        public Failure(int status,long retryMs){super("API status "+status);this.status=status;this.retryMs=retryMs;}
    }
    public static final class BackendMismatch extends IOException{}
    private final Connector connector;
    private volatile HttpsURLConnection active;
    private volatile boolean cancelled;
    private QaModelBudget qaBudget;private long reservation;private boolean settled;
    public long billingTailMs;
    public void qaBudget(QaModelBudget budget){this.qaBudget=budget;}
    public volatile long headersMs=-1,firstTextMs=-1,actionMs=-1;private long started;
    private static long now(){return System.nanoTime()/1000000;}
    public ModelHttp(){this(url->(HttpsURLConnection)url.openConnection());}
    public ModelHttp(Connector connector){this.connector=connector;}
    public void cancel(){cancelled=true;HttpsURLConnection c=active;if(c!=null)c.disconnect();}
    private void check()throws IOException{if(cancelled)throw new IOException("Cancelled");}
    private HttpsURLConnection open(ModelConfig c,String url,String method)throws Exception{
        check();HttpsURLConnection h=connector.open(new URL(url));active=h;
        try{check();h.setInstanceFollowRedirects(false);h.setConnectTimeout(2000);h.setReadTimeout(4000);h.setRequestMethod(method);h.setUseCaches(false);
            if(c.protocol==ModelConfig.GEMINI)h.setRequestProperty("x-goog-api-key",c.key);
            else if(c.protocol==ModelConfig.CLAUDE){h.setRequestProperty("x-api-key",c.key);h.setRequestProperty("anthropic-version","2023-06-01");}
            else if(!c.key.isEmpty())h.setRequestProperty("Authorization","Bearer "+c.key);return h;
        }catch(Exception e){h.disconnect();active=null;throw e;}
    }
    private void status(HttpsURLConnection h)throws IOException{
        int code=h.getResponseCode();if(code==200)return;long retry=0;String s=h.getHeaderField("Retry-After");
        if(s!=null)try{retry=Math.min(60000,Math.max(0,Long.parseLong(s)*1000));}catch(NumberFormatException ignored){try{retry=Math.max(0,Math.min(60000,java.time.ZonedDateTime.parse(s,java.time.format.DateTimeFormatter.RFC_1123_DATE_TIME).toInstant().toEpochMilli()-System.currentTimeMillis()));}catch(Exception ignoredDate){}}
        throw new Failure(code,retry);
    }
    private String json(InputStream in,int limit)throws IOException{
        ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] b=new byte[2048];int n;while((n=in.read(b))!=-1){check();if(out.size()+n>limit)throw new IOException("Response limit");out.write(b,0,n);}check();return out.toString("UTF-8");
    }
    public void verifyBackend(ModelConfig config)throws Exception{
        HttpsURLConnection c=open(config,config.base+"/health","GET");
        try{status(c);JSONObject health;try(InputStream in=c.getInputStream()){health=new JSONObject(json(in,16384));}if(!health.optBoolean("loaded")||!"ok".equals(health.optString("status"))||(!"server-loaded".equals(config.model)&&!config.model.equals(health.optString("model"))))throw new BackendMismatch();}
        finally{c.disconnect();active=null;}
    }
    public ModelAction call(ModelConfig config,String observation,String terrain,boolean verifyBackend)throws Exception{
        started=now();config.validate();int worldWidth=800,worldHeight=480;
        java.util.regex.Matcher dimensions=java.util.regex.Pattern.compile("\"world\"\\s*:\\s*\\[\\s*(\\d+)\\s*,\\s*(\\d+)").matcher(terrain);if(dimensions.find()){worldWidth=Integer.parseInt(dimensions.group(1));worldHeight=Integer.parseInt(dimensions.group(2));}
        if(qaBudget!=null&&(!"api.deepseek.com".equals(new java.net.URI(config.base).getHost())||!"deepseek-v4-pro".equals(config.model)||config.protocol!=ModelConfig.CHAT))throw new QaModelBudget.Exhausted("QA budget requires verified DeepSeek Chat tariff");
        if(config.protocol==ModelConfig.SIMPLETOOL&&verifyBackend)verifyBackend(config);
        String rawRequest=ModelProtocol.request(config,observation,terrain);int maximumOutput=0;
        if(qaBudget!=null){JSONObject request=new JSONObject(rawRequest).put("stream_options",new JSONObject().put("include_usage",true));maximumOutput=request.optInt("max_tokens",2048);rawRequest=request.toString();}
        // Preserve literal positional SimpleTool property order in normal calls.
        byte[] body=rawRequest.getBytes(StandardCharsets.UTF_8);
        if(qaBudget!=null)reservation=qaBudget.reserve(body.length,maximumOutput);
        HttpsURLConnection c=open(config,ModelProtocol.url(config),"POST");
        try{
            c.setDoOutput(true);c.setRequestProperty("Content-Type","application/json; charset=utf-8");c.setRequestProperty("Accept",config.protocol==ModelConfig.SIMPLETOOL?"application/json":"text/event-stream, application/json");
            c.setFixedLengthStreamingMode(body.length);try(OutputStream out=c.getOutputStream()){check();out.write(body);}status(c);headersMs=now()-started;
            try(InputStream input=c.getInputStream()){
                String type=c.getContentType();ModelAction result;
                if(type!=null&&type.toLowerCase(java.util.Locale.ROOT).contains("text/event-stream"))result=stream(input,config.protocol,worldWidth,worldHeight);
                else{String raw=json(input,65536);settle(new JSONObject(raw).optJSONObject("usage"));result=ModelProtocol.parse(config,raw,worldWidth,worldHeight);}check();return result;
            }
        }finally{c.disconnect();active=null;}
    }
    private ModelAction stream(InputStream input,int protocol,int width,int height)throws Exception{
        ModelStream decoder=new ModelStream(protocol,width,height);Reader reader=new BufferedReader(new InputStreamReader(input,StandardCharsets.UTF_8),8192);
        StringBuilder line=new StringBuilder(),data=new StringBuilder();int count=0,ch;
        ModelAction complete=null;long completedAt=0;
        try{while((ch=reader.read())!=-1){check();if(++count>262144)throw new IOException("Stream limit");if(ch=='\r')continue;
            if(ch!='\n'){if(line.length()>=65536)throw new IOException("Event line limit");line.append((char)ch);continue;}
            if(line.length()==0){if(data.length()>0){String raw=data.toString();if(qaBudget!=null&&!"[DONE]".equals(raw))settle(new JSONObject(raw).optJSONObject("usage"));if(complete==null){ModelAction a=decoder.event(raw);if(firstTextMs<0&&decoder.hasText())firstTextMs=now()-started;if(a!=null){actionMs=now()-started;if(qaBudget==null)return a;complete=a;completedAt=now();if(active!=null)active.setReadTimeout(250);}}data.setLength(0);if(complete!=null&&(settled||now()-completedAt>=250)){billingTailMs=now()-completedAt;return complete;}}}
            else if(line.indexOf("data:")==0){String part=line.substring(5);if(part.startsWith(" "))part=part.substring(1);if(data.length()+part.length()>65536)throw new IOException("Event limit");if(data.length()>0)data.append('\n');data.append(part);}line.setLength(0);
        }}catch(IOException e){if(complete!=null){billingTailMs=now()-completedAt;return complete;}throw e;}
        if(complete!=null){billingTailMs=now()-completedAt;return complete;}
        throw new IOException("No complete action");
    }
    private void settle(JSONObject usage)throws IOException{if(qaBudget!=null&&!settled&&usage!=null){qaBudget.settle(reservation,usage);settled=true;}}
}
