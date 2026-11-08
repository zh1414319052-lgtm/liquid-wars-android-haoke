// SPDX-License-Identifier: GPL-3.0-or-later
package com.xenris.liquidwarsos.cloud;

import java.util.*;

/** Official endpoints are presets, not credentials. Other model IDs come from each account's list. */
public final class ModelProviders {
    public static final class Provider {
        public final String name,base; public final int protocol; public final String[] defaults;
        Provider(String name,String base,int protocol,String... defaults){this.name=name;this.base=base;this.protocol=protocol;this.defaults=defaults;}
    }
    public static final Provider[] ALL={
        new Provider("DeepSeek","https://api.deepseek.com",0,"deepseek-flash","deepseek-v4-pro"),
        new Provider("OpenAI","https://api.openai.com/v1",1),
        new Provider("Claude / Anthropic","https://api.anthropic.com/v1",2),
        new Provider("Gemini / Google","https://generativelanguage.googleapis.com/v1beta",3),
        new Provider("通义千问 / Qwen","https://dashscope.aliyuncs.com/compatible-mode/v1",0),
        new Provider("Kimi / Moonshot","https://api.moonshot.cn/v1",0),
        new Provider("智谱 / GLM","https://open.bigmodel.cn/api/paas/v4",0),
        new Provider("豆包 / Volcengine","https://ark.cn-beijing.volces.com/api/v3",0),
        new Provider("硅基流动 / SiliconFlow","https://api.siliconflow.cn/v1",0),
        new Provider("Grok / xAI","https://api.x.ai/v1",1),
        new Provider("Mistral","https://api.mistral.ai/v1",0),
        new Provider("SimpleTool / 自部署","https://",ModelConfig.SIMPLETOOL,"server-loaded"),
        new Provider("自定义 / Custom","https://",0)
    };
    public static int find(ModelConfig c){if(c.protocol==ModelConfig.SIMPLETOOL)return ALL.length-2;for(int i=0;i<ALL.length-1;i++)if(ALL[i].base.equals(c.base)&&ALL[i].protocol==c.protocol)return i;return ALL.length-1;}
    public static List<String> defaults(ModelConfig c){return new ArrayList<>(Arrays.asList(ALL[find(c)].defaults));}
    public static String title(String id){if(id.equals("server-loaded"))return "服务端当前模型 / Server-loaded";if(id.equals("deepseek-flash"))return "DeepSeek Flash";if(id.equals("deepseek-v4-pro"))return "DeepSeek V4 Pro";return id;}
    /** Prefer compact text variants; this is a UI default, never a price/strength guarantee. */
    public static String recommend(List<String> models){
        if(models.isEmpty())return "";
        List<String> ordered=new ArrayList<>(models);
        ordered.sort((a,b)->{int s=Integer.compare(rank(a),rank(b));return s!=0?s:b.compareToIgnoreCase(a);});
        return ordered.get(0);
    }
    private static int rank(String id){
        String s=id.toLowerCase(Locale.ROOT);
        if(s.equals("deepseek-flash")||s.equals("gpt-4.1-mini")||s.equals("qwen-flash")||s.equals("mistral-small-latest"))return 0;
        int rank=s.contains("flash-lite")||s.contains("haiku")?10:s.contains("flash")||s.contains("mini")||s.contains("lite")||s.contains("small")?20:50;
        if(s.contains("thinking")||s.contains("reasoner")||s.contains("preview")||s.contains("exp-"))rank+=60;
        return rank;
    }
}
