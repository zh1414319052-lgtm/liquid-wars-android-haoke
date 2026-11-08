// SPDX-License-Identifier: GPL-3.0-or-later
package com.xenris.liquidwarsos.cloud;
import java.io.*;import java.nio.file.*;import org.json.*;

/** Opt-in debug-test ledger only, never enabled by normal matches. Stores only
 * numeric cost/request counters; no endpoint, prompt, model config or secret.
 * Official peak-price upper bounds: miss9/hit0.30/output27 CNY per1M tokens.
 * UTF-8 bytes+1024 wrapper tokens are reserved before sending; refund only on
 * actual provider usage. Unknown/cancelled usage retains its full reservation. */
public final class QaModelBudget {
 public static final class Exhausted extends IOException{public Exhausted(String s){super(s);}}
 private final File file;private final long limit;private final int maxRequests;
 private long used,hitTokens,missTokens,outputTokens;private int requests,settledRequests;private final String campaign;
 public QaModelBudget(File file,String campaign,long limit,int maxRequests)throws IOException{
  if(limit<=0||limit>10000000||maxRequests<1||maxRequests>2000)throw new IllegalArgumentException("QA budget bounds");this.file=file;this.campaign=campaign;this.limit=limit;this.maxRequests=maxRequests;
  if(file!=null&&file.exists())try{JSONObject v=new JSONObject(new String(Files.readAllBytes(file.toPath()),java.nio.charset.StandardCharsets.UTF_8));if(!campaign.equals(v.getString("campaign")))throw new IOException("QA budget campaign mismatch");used=v.getLong("reserved_or_peak_cost_micros");requests=v.getInt("requests");hitTokens=v.optLong("hit_tokens",0);missTokens=v.optLong("miss_tokens",0);outputTokens=v.optLong("output_tokens",0);settledRequests=v.optInt("settled_requests",0);}catch(JSONException e){throw new IOException("Invalid QA budget ledger",e);}
 }
 public synchronized long reserve(int bytes,int output)throws IOException{
  long charge=(bytes+1024L)*9+output*27L;if(requests>=maxRequests||used+charge>limit)throw new Exhausted("QA budget reached");used+=charge;requests++;save();return charge;
 }
 public synchronized void settle(long reservation,JSONObject usage)throws IOException{
  if(usage==null)return;long prompt=usage.optLong("prompt_tokens",-1),output=usage.optLong("completion_tokens",-1);if(prompt<0||output<0)return;long hits=Math.max(0,Math.min(prompt,usage.optLong("prompt_cache_hit_tokens",0))),miss=prompt-hits;
  long actual=(hits*3+9)/10+miss*9+output*27;used=Math.max(0,used-reservation+actual);hitTokens+=hits;missTokens+=miss;outputTokens+=output;settledRequests++;save();
 }
 private void save()throws IOException{if(file==null)return;try{JSONObject v=new JSONObject().put("campaign",campaign).put("limit_micros",limit).put("request_limit",maxRequests).put("reserved_or_peak_cost_micros",used).put("requests",requests).put("settled_requests",settledRequests).put("hit_tokens",hitTokens).put("miss_tokens",missTokens).put("output_tokens",outputTokens).put("price_source","https://api-docs.deepseek.com/zh-cn/quick_start/pricing/");File temp=new File(file.getPath()+".tmp");try(FileOutputStream out=new FileOutputStream(temp)){out.write(v.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));out.getFD().sync();}try{Files.move(temp.toPath(),file.toPath(),StandardCopyOption.REPLACE_EXISTING,StandardCopyOption.ATOMIC_MOVE);}catch(AtomicMoveNotSupportedException e){Files.move(temp.toPath(),file.toPath(),StandardCopyOption.REPLACE_EXISTING);}}catch(JSONException e){throw new IOException(e);}}
 public synchronized long usedMicros(){return used;}
 public synchronized int requests(){return requests;}
}
