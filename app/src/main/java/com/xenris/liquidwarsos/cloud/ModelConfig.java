// SPDX-License-Identifier: GPL-3.0-or-later
package com.xenris.liquidwarsos.cloud;

import java.net.URI;
import org.json.JSONObject;

/** One independent user-owned API identity per team; never serialized onto the game wire. */
public final class ModelConfig {
    public static final int CHAT=0, RESPONSES=1, CLAUDE=2, GEMINI=3, SIMPLETOOL=4;
    public final boolean enabled;
    public final int protocol;
    public final String base,model,key;
    public ModelConfig(boolean enabled,int protocol,String base,String model,String key){this.enabled=enabled;this.protocol=protocol;this.base=base.trim().replaceAll("/+$","");this.model=model.trim();this.key=key.trim();}
    public static ModelConfig empty(){return new ModelConfig(false,CHAT,"https://api.deepseek.com","","");}
    public void validate() throws Exception {
        URI u=new URI(base);
        if(!"https".equals(u.getScheme())||u.getHost()==null||u.getUserInfo()!=null||u.getQuery()!=null||u.getFragment()!=null||base.length()>512)throw new IllegalArgumentException("HTTPS base URL");
        if(protocol<0||protocol>SIMPLETOOL||!model.matches("[A-Za-z0-9_.:/-]{1,160}")||(protocol!=SIMPLETOOL&&key.isEmpty())||key.length()>4096||key.matches("(?s).*[\\r\\n].*"))throw new IllegalArgumentException("Model / key");
    }
    public JSONObject json() throws Exception {return new JSONObject().put("enabled",enabled).put("protocol",protocol).put("base",base).put("model",model).put("key",key);}
    public static ModelConfig from(JSONObject o){return new ModelConfig(o.optBoolean("enabled"),o.optInt("protocol"),o.optString("base"),o.optString("model"),o.optString("key"));}
}
