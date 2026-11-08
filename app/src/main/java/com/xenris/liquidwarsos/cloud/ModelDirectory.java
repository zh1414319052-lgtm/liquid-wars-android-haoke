// SPDX-License-Identifier: GPL-3.0-or-later
package com.xenris.liquidwarsos.cloud;

import org.json.*;
import javax.net.ssl.HttpsURLConnection;
import java.net.*;
import java.io.*;
import java.util.*;

/** Read-only model discovery; no inference request, redirection, retries or key in a URL. */
public final class ModelDirectory {
    private final ModelHttp.Connector connector;
    private volatile boolean cancelled;
    private volatile HttpsURLConnection active;
    public ModelDirectory(){this(url->(HttpsURLConnection)url.openConnection());}
    public ModelDirectory(ModelHttp.Connector connector){this.connector=connector;}
    public void cancel(){cancelled=true;HttpsURLConnection c=active;if(c!=null)c.disconnect();}
    public static final class Result {
        public final List<String> models; public final boolean truncated;
        Result(List<String> models,boolean truncated){this.models=models;this.truncated=truncated;}
    }
    public Result fetch(ModelConfig config)throws Exception{
        new ModelConfig(true,config.protocol,config.base,"discovery",config.key).validate();
        LinkedHashSet<String> found=new LinkedHashSet<>();String cursor="";boolean more=false;long start=System.nanoTime();
        for(int page=0;page<3;page++){
            if(cancelled||(System.nanoTime()-start)>15000000000L)throw new IOException("Cancelled/timeout");
            String url=config.base+(config.protocol==ModelConfig.SIMPLETOOL?"/health":"/models");
            if(config.protocol==ModelConfig.GEMINI)url+="?pageSize=100"+(cursor.isEmpty()?"":"&pageToken="+URLEncoder.encode(cursor,"UTF-8"));
            else if(config.protocol==ModelConfig.CLAUDE)url+="?limit=100"+(cursor.isEmpty()?"":"&after_id="+URLEncoder.encode(cursor,"UTF-8"));
            HttpsURLConnection c=connector.open(new URL(url));active=c;
            JSONObject response;
            try{
                if(cancelled)throw new IOException("Cancelled");
                c.setInstanceFollowRedirects(false);c.setUseCaches(false);c.setConnectTimeout(5000);c.setReadTimeout(8000);c.setRequestMethod("GET");c.setRequestProperty("Accept","application/json");
                if(config.protocol==ModelConfig.GEMINI)c.setRequestProperty("x-goog-api-key",config.key);
                else if(config.protocol==ModelConfig.CLAUDE){c.setRequestProperty("x-api-key",config.key);c.setRequestProperty("anthropic-version","2023-06-01");}
                else if(!config.key.isEmpty())c.setRequestProperty("Authorization","Bearer "+config.key);
                int status=c.getResponseCode();if(status!=200)throw new ModelHttp.Failure(status);
                ByteArrayOutputStream body=new ByteArrayOutputStream();
                try(InputStream in=c.getInputStream()){byte[] buffer=new byte[4096];int n;while((n=in.read(buffer))!=-1){if(cancelled||body.size()+n>262144||(System.nanoTime()-start)>15000000000L)throw new IOException("Response limit");body.write(buffer,0,n);}}
                response=new JSONObject(body.toString("UTF-8"));
            }finally{c.disconnect();active=null;}
            if(cancelled)throw new IOException("Cancelled");
            found.addAll(parse(config,response));
            if(found.size()>512){found=new LinkedHashSet<>(new ArrayList<>(found).subList(0,512));more=true;break;}
            String next=config.protocol==ModelConfig.GEMINI?response.optString("nextPageToken"):config.protocol==ModelConfig.CLAUDE&&response.optBoolean("has_more")?response.optString("last_id"):"";
            if(next.length()>2048)throw new IOException("Cursor limit");
            more=!next.isEmpty();if(!more)break;if(next.equals(cursor))throw new IOException("Repeated cursor");cursor=next;
        }
        if(found.isEmpty())throw new IOException("No text models");
        return new Result(new ArrayList<>(found),more);
    }
    public static List<String> parse(ModelConfig config,JSONObject root)throws Exception{
        if(config.protocol==ModelConfig.SIMPLETOOL){String model=root.optString("model");if(!root.optBoolean("loaded")||!"ok".equals(root.optString("status"))||!model.matches("[A-Za-z0-9_.:/-]{1,160}"))throw new IOException("Backend not ready");return Collections.singletonList(model);}
        JSONArray rows=root.getJSONArray(config.protocol==ModelConfig.GEMINI?"models":"data");
        if(rows.length()>2000)throw new IOException("Model count limit");
        LinkedHashSet<String> result=new LinkedHashSet<>();String host=new URI(config.base).getHost();
        for(int i=0;i<rows.length();i++){
            JSONObject row=rows.optJSONObject(i);if(row==null)continue;
            String id=row.optString(config.protocol==ModelConfig.GEMINI?"name":"id").replaceFirst("^models/","");
            if(!id.matches("[A-Za-z0-9_.:/-]{1,160}"))continue;
            String lower=id.toLowerCase(Locale.ROOT);
            if(lower.matches(".*(embed|rerank|moderation|whisper|tts|speech|audio|realtime|dall-e|image|imagen|video|veo-|flux|stable-diffusion|ocr|codex|computer-use).*"))continue;
            if("retired".equals(row.optString("lifecycle")))continue;
            String shutdown=row.optString("shutdown_date","");
            if(shutdown.matches("[0-9]{4}-[0-9]{2}-[0-9]{2}")&&shutdown.compareTo(java.time.LocalDate.now(java.time.ZoneOffset.UTC).toString())<=0)continue;
            if(host.equals("api.openai.com")&&!lower.matches("^(gpt-|chatgpt-|o[0-9]).*"))continue;
            JSONObject capabilities=row.optJSONObject("capabilities");
            if(capabilities!=null&&capabilities.has("completion_chat")&&!capabilities.optBoolean("completion_chat"))continue;
            if(config.protocol==ModelConfig.GEMINI){JSONArray methods=row.optJSONArray("supportedGenerationMethods");boolean ok=false;if(methods!=null)for(int k=0;k<methods.length();k++)if("generateContent".equals(methods.optString(k)))ok=true;if(!ok)continue;}
            JSONArray outputs=row.optJSONArray("output_modalities");if(outputs!=null){boolean text=false;for(int k=0;k<outputs.length();k++)if("text".equals(outputs.optString(k)))text=true;if(!text)continue;}
            result.add(id);
        }
        return new ArrayList<>(result);
    }
}
