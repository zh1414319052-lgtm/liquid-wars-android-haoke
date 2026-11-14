package com.xenris.liquidwarsos.cloud;
import java.util.*;import java.util.concurrent.atomic.*;import org.json.*;
public final class StrategyCheck {
 static int checks;static void ok(boolean v,String s){checks++;if(!v)throw new AssertionError(s);}
 interface Attempt{void run()throws Exception;}static void rejects(Attempt a,String why)throws Exception{try{a.run();}catch(Exception e){checks++;return;}throw new AssertionError(why);}
 static int[] snapshot(int x,int y){int[] s=new int[ModelWorld.HEADER+600];s[216]=600;for(int t=0;t<6;t++){int px=t==0?x:Math.min(750,500+t*30),py=t==0?y:240;int at=t*8;s[at]=100;s[at+1]=50;s[at+2]=s[at+4]=px;s[at+3]=s[at+5]=py;s[at+6]=100;for(int k=0;k<100;k++)s[ModelWorld.HEADER+t*100+k]=px|(py<<10)|(t<<19)|(5<<22);}return s;}
 public static void main(String[] args)throws Exception{
  byte[] mask=new byte[384000];for(int y=0;y<480;y++)if(y<80||y>130)for(int x=390;x<410;x++)mask[y*800+x]=1;
  ModelNavigation nav=new ModelNavigation(mask);ModelWorld world=new ModelWorld(snapshot(100,240),nav);int goal=nav.roomAt(700,240);ModelNavigation.Route route=nav.routes(world.massRoom[0],goal,3).get(0);ok(route.doors.length>0,"wall detour exists");
  ModelNavigation.Door end=nav.doors.get(route.doors[route.doors.length-1]);int tx=end.a==goal?end.ax:end.bx,ty=end.a==goal?end.ay:end.by;
  JSONObject json=new JSONObject().put("enemy",1).put("intent","flank").put("route",new JSONArray(route.doors)).put("to",new JSONArray().put(tx).put(ty)).put("plan","绕过门口，再接战").put("hold_ms",6000);
  ModelAction action=ModelAction.parse(json);ok(action.holdMs==6000&&action.plan.equals("绕过门口，再接战"),"routed duration and operative label");int[] clone=action.route();clone[0]=999;ok(action.route()[0]!=999,"route immutable");
  ok(nav.validate(world.massRoom[0],action)==null,"ordered adjacent route validated");
  rejects(()->ModelAction.parse(new JSONObject(json.toString()).put("hold_ms",6001)),"route lifetime bound");rejects(()->ModelAction.parse(new JSONObject(json.toString()).put("plan","x".repeat(41))),"plan bounded");rejects(()->ModelAction.parse(new JSONObject(json.toString()).put("route",new JSONArray().put(-1))),"negative door rejected");
  ModelAction bad=ModelAction.parse(new JSONObject(json.toString()).put("route",new JSONArray().put(999)));ok(nav.validate(world.massRoom[0],bad)!=null,"unknown door rejected");
  ModelNavigation.Door last=nav.doors.get(route.doors[route.doors.length-1]);int acrossX=last.a==goal?last.bx:last.ax,acrossY=last.a==goal?last.by:last.ay;
  ModelAction visibleAcross=ModelAction.parse(new JSONObject(json.toString()).put("to",new JSONArray().put(acrossX).put(acrossY)));
  ok(nav.roomAt(acrossX,acrossY)!=goal&&nav.validate(world.massRoom[0],visibleAcross)==null,"visible floor across a synthetic region boundary is legal");
  ModelNavigation open=new ModelNavigation(new byte[384000]);ModelWorld openWorld=new ModelWorld(snapshot(100,240),open);ModelAction direct=ModelAction.parse(new JSONObject().put("enemy",1).put("intent","attack").put("route",new JSONArray()).put("to",new JSONArray().put(700).put(240)).put("hold_ms",6000));
  ok(open.doors.isEmpty()&&open.validateFrom(openWorld.massRoom[0],100,240,direct)==null,"open map accepts empty route");ModelRouteExecutor directExecutor=new ModelRouteExecutor(open);directExecutor.install(direct,openWorld,0,0);ok(directExecutor.points(openWorld,0,0)[0]==700,"empty route goes directly, not via an invented door");
  rejects(()->ModelAction.parse(new JSONObject().put("intent","hold").put("points",new JSONArray()).put("hold_ms",1000)),"hold cannot mean passive stop");
  ModelAction stop=ModelAction.parse(new JSONObject().put("intent","stop").put("route",new JSONArray()).put("points",new JSONArray()).put("hold_ms",6000));ok(stop.points()[0]==-1,"stop releases all model fingers");
  ModelAction nullStop=ModelAction.parse(new JSONObject().put("intent","stop").put("route",JSONObject.NULL).put("to",JSONObject.NULL).put("points",JSONObject.NULL).put("hold_ms",6000));ok(nullStop.points()[0]==-1&&nullStop.route().length==0,"nullable unused stop fields remain a true stop");
  rejects(()->ModelAction.parse(new JSONObject().put("intent","attack").put("to",JSONObject.NULL).put("hold_ms",1000)),"active null destination is not a stop shortcut");
  short[] blockedTouch=new short[10];Arrays.fill(blockedTouch,(short)-1);blockedTouch[0]=700;blockedTouch[5]=240;
  ModelRouteExecutor tracking=new ModelRouteExecutor(nav,new int[]{0,0,2,3,4,5});int[] contactSnapshot=snapshot(100,240);contactSnapshot[121]=8;ModelWorld alliedContact=new ModelWorld(contactSnapshot,nav);
  tracking.observe(alliedContact,0,100);tracking.install(new ModelAction(blockedTouch,1000),alliedContact,0,100);tracking.observe(alliedContact,0,1100);tracking.install(new ModelAction(blockedTouch,1200),alliedContact,0,1100);tracking.observe(alliedContact,0,2100);
  ok(tracking.facts(alliedContact,0,2100,blockedTouch).getBoolean("wall_stalled"),"new commands do not hide2s stalls; allies do not count as hostile contact");tracking.resetHistory();ok(!tracking.facts(alliedContact,0,2100,blockedTouch).getBoolean("wall_stalled"),"warmup/pause reset the movement window");
  ModelRouteExecutor executor=new ModelRouteExecutor(nav);executor.install(action,world,0,0);short[] target=executor.points(world,0,0);ok(target[0]>=0&&target[1]<0,"single finger while traveling");int progress=executor.facts(world,0,0,target).getInt("waypoint_index");executor.install(action,world,0,1000);ok(executor.facts(world,0,1000,target).getInt("waypoint_index")==progress,"same route preserves progress");
  int[] allies={0,1,2,3,4,5};ModelAction[] actions=new ModelAction[6];actions[0]=action;JSONObject facts=ModelFacts.observation(0,world,null,0,allies,nav,actions,executor,target,1000,119000);
  ok(facts.getJSONObject("YOU").getJSONArray("room_population").getJSONArray(0).getInt(1)==100,"exact particle-room accounting");ok(facts.getJSONArray("ENEMIES").getJSONObject(0).getJSONArray("routes_by_length").length()>0,"objective routes available");ok(!facts.toString().contains("recommended"),"no recommended opponent");
  for(int p=0;p<4;p++){ModelConfig config=Check.conf(p,"fixture");ModelAction parsed=ModelProtocol.parse(config,Check.body(p,json.toString()));ok(Arrays.equals(parsed.route(),action.route()),"routed format protocol "+p);}
  AtomicLong clock=new AtomicLong(100);AtomicInteger requests=new AtomicInteger();ModelConfig[] configs={Check.conf(0,"fixture"),ModelConfig.empty(),ModelConfig.empty(),ModelConfig.empty(),ModelConfig.empty(),ModelConfig.empty()};
  try(ModelMatch match=new ModelMatch(configs,allies,clock::get,()->new ModelHttp(url->{int n=requests.incrementAndGet();return new Check.Connection(url,n==3?500:200,n==1?Check.body(0,json.toString()):Check.body(0,new JSONObject(json.toString()).put("route",new JSONArray().put(999)).toString()));}))){
   match.terrain(mask);match.pause(false);match.update(1,world.raw);Check.idle(match);ok(match.inputs().mask==1,"valid route active");clock.addAndGet(5200);match.tickExecution();ok(match.inputs().mask==1,"command lifetime independent of 5s reply age");
   match.update(1,world.raw);Check.idle(match);ok(match.inputs().mask==1,"semantic rejection preserves still-live instruction");ok(match.due(1),"semantic rejection retries without backoff");
   match.update(1,world.raw);Check.idle(match);ok(match.inputs().mask==0,"network failure stops model despite previous route");
  }
  ModelRouteExecutor defender=new ModelRouteExecutor(nav);defender.install(stop,world,0,0);ok(defender.points(world,0,0)[0]==-1,"no local fallback after explicit stop");
  QaModelBudget budget=new QaModelBudget(null,"test",100000,2);long reserved=budget.reserve(1000,100);ok(budget.usedMicros()==(2024*9+2700),"peak cost reserved before send");budget.settle(reserved,new JSONObject().put("prompt_tokens",100).put("prompt_cache_hit_tokens",80).put("completion_tokens",10));ok(budget.usedMicros()==80*3/10+20*9+10*27,"actual usage refund remains at peak tariff");budget.reserve(1000,100);rejects(()->budget.reserve(1,1),"hard request ceiling");QaModelBudget tiny=new QaModelBudget(null,"test",100,20);rejects(()->tiny.reserve(1,1),"hard money ceiling before transport");
  ModelConfig deepseek=new ModelConfig(true,0,"https://api.deepseek.com","deepseek-v4-pro","fixture");
  String billed="data: "+Check.event(0,Check.ACTION)+"\n\ndata: "+new JSONObject().put("usage",new JSONObject().put("prompt_tokens",100).put("prompt_cache_hit_tokens",80).put("completion_tokens",10))+"\n\ndata: [DONE]\n\n";
  QaModelBudget measured=new QaModelBudget(null,"test",1000000,10);ModelHttp http=new ModelHttp(url->new Check.Connection(url,200,billed){public String getContentType(){return "text/event-stream";}});http.qaBudget(measured);ok(http.call(deepseek,"{}","",false).points()[0]==120&&measured.usedMicros()==474,"stream usage settles after first complete action");
  AtomicInteger opened=new AtomicInteger();ModelHttp denied=new ModelHttp(url->{opened.incrementAndGet();return new Check.Connection(url,200,billed);});denied.qaBudget(tiny);rejects(()->denied.call(deepseek,"{}","",false),"budget denial stops transport");ok(opened.get()==0,"zero HTTP calls after budget denial");
  // 1.65: new item observations and terrain generations must invalidate stale routes.
  int[] itemState=new int[209];itemState[1]=20000;itemState[3]=54000;itemState[4]=108000;itemState[5]=7;itemState[19]=0;itemState[20]=120;itemState[21]=240;itemState[22]=-1;itemState[25+2]=9000;for(int t=0;t<6;t++)itemState[147+t*10+8]=allies[t];
  AtomicInteger itemCalls=new AtomicInteger();
  try(ModelMatch match=new ModelMatch(configs,allies,clock::get,()->new ModelHttp(url->{itemCalls.incrementAndGet();return new Check.Connection(url,200,Check.body(0,json.toString()));}))){
   match.terrain(mask);match.itemState(itemState);match.pause(false);match.update(1,world.raw);Check.idle(match);
   JSONObject observation=match.diagnostics().getJSONArray("teams").getJSONObject(0).getJSONObject("observation");
   ok(observation.getJSONObject("battle_items").getJSONArray("pickups_kind_x_y").getJSONArray(0).getInt(0)==0,"real pickup kind included in request facts");
   ok(observation.getJSONObject("battle_items").getInt("global_population")==54000,"dynamic count included");
   ok(match.inputs().mask==1,"pre-rebuild action active");
   match.replaceTerrain(new ModelTerrain(new byte[800*480]));
   ok(match.inputs().mask==0,"old door IDs never survive terrain replacement");
   ok(match.due(1),"new terrain can request its own model command");
  }
  ModelAction targeted=ModelAction.parse(new JSONObject().put("intent","stop").put("points",new JSONArray()).put("hold_ms",1000).put("item_team",2).put("item_to",new JSONArray().put(430).put(200)));
  ok(targeted.itemTeam==2&&targeted.itemTo[0]==430,"explicit targeted item arguments parsed");
  rejects(()->ModelAction.parse(new JSONObject().put("points",new JSONArray()).put("hold_ms",1000).put("item_team",6)),"invalid item target rejected");
  ModelConfig[] one=new ModelConfig[6];Arrays.fill(one,ModelConfig.empty());one[0]=deepseek;
  String itemReply=new JSONObject().put("intent","stop").put("points",new JSONArray()).put("hold_ms",1000).put("item_team",2).toString();
  ModelMatch.Factory itemFactory=()->new ModelHttp(url->new Check.Connection(url,200,Check.body(0,itemReply)));
  try(ModelMatch match=new ModelMatch(one,allies,clock::get,itemFactory)){
   int[] item=itemState.clone();item[147]=7;item[148]=19;match.terrain(mask);match.itemState(item);match.pause(false);match.update(1,world.raw);Check.idle(match);ModelMatch.Inputs cmd=match.inputs();ok(cmd.points[4]==-3&&cmd.points[9]==19&&cmd.points[3]==2,"model exact team choice becomes lockstep command");
   item[146]++;match.itemState(item);ok(match.inputs().mask==0,"rewind cancels old model generation");
  }
  int[] large=new int[ModelWorld.HEADER+108000];large[216]=108000;ok(ModelWorld.valid(large),"model accepts growth cap");large[216]=108001;ok(!ModelWorld.valid(large),"oversized or malformed snapshot rejected");
  System.out.println("PASS "+checks+" strategic route/action/observation/lifetime assertions; actual Java + fake HTTPS");
 }
}
