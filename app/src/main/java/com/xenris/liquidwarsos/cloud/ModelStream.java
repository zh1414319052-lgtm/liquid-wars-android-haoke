// SPDX-License-Identifier: GPL-3.0-or-later
package com.xenris.liquidwarsos.cloud;
import org.json.*;
/** Extracts only answer text; thinking deltas never enter the action parser. */
public final class ModelStream {
    private final int protocol,width,height;
    private final StringBuilder text=new StringBuilder();
    public ModelStream(int protocol){this(protocol,800,480);}
    public ModelStream(int protocol,int width,int height){this.protocol=protocol;this.width=width;this.height=height;}
    public boolean hasText(){return text.length()>0;}
    public ModelAction event(String data)throws Exception{
        if(data.trim().isEmpty()||"[DONE]".equals(data.trim()))return null;
        JSONObject e=new JSONObject(data);if(e.has("error")||"error".equals(e.optString("type")))throw new JSONException("Provider error");
        String delta="";
        if(protocol==ModelConfig.RESPONSES){if("response.output_text.delta".equals(e.optString("type")))delta=e.getString("delta");}
        else if(protocol==ModelConfig.CLAUDE){JSONObject d=e.optJSONObject("delta");if("content_block_delta".equals(e.optString("type"))&&d!=null&&"text_delta".equals(d.optString("type")))delta=d.getString("text");}
        else if(protocol==ModelConfig.GEMINI){JSONArray candidates=e.optJSONArray("candidates");if(candidates!=null&&candidates.length()>0){JSONObject content=candidates.getJSONObject(0).optJSONObject("content");if(content!=null){JSONArray parts=content.optJSONArray("parts");if(parts!=null)for(int i=0;i<parts.length();i++){JSONObject p=parts.getJSONObject(i);if(!p.optBoolean("thought"))delta+=p.optString("text");}}}}
        else {JSONArray choices=e.optJSONArray("choices");if(choices!=null&&choices.length()>0){JSONObject d=choices.getJSONObject(0).optJSONObject("delta");if(d!=null&&d.opt("content") instanceof String)delta=d.getString("content");}}
        if(text.length()+delta.length()>4096)throw new JSONException("Oversized action");text.append(delta);String object=completeObject(text.toString());return object==null?null:ModelAction.parse(new JSONObject(object),width,height);
    }
    public static String completeObject(String input)throws JSONException{
        if(input.length()>4096)throw new JSONException("Oversized action");String s=input.trim();if(s.isEmpty())return null;
        if(s.startsWith("`")){int newline=s.indexOf('\n');if(newline<0){if(s.length()>10)throw new JSONException("Invalid fence");return null;}String fence=s.substring(0,newline).trim();if(!fence.equals("```json")&&!fence.equals("```"))throw new JSONException("Invalid fence");s=s.substring(newline+1).trim();if(s.isEmpty())return null;}
        if(s.charAt(0)!='{')throw new JSONException("JSON object required");int depth=0;boolean quoted=false,escaped=false;
        for(int i=0;i<s.length();i++){char c=s.charAt(i);if(quoted){if(escaped)escaped=false;else if(c=='\\')escaped=true;else if(c=='"')quoted=false;}else if(c=='"')quoted=true;else if(c=='{')depth++;else if(c=='}'&&--depth==0)return s.substring(0,i+1);}return null;
    }
}
