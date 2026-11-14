// SPDX-License-Identifier: GPL-3.0-or-later
package com.xenris.liquidwarsos.cloud;
import java.util.*;
import java.util.concurrent.*;

/** Six independent, bounded request lanes. Only the game thread supplies world snapshots. */
public final class ModelMatch implements AutoCloseable {
    public static final long MAX_AGE_MS=5000,DEADLINE_MS=6000;
    public interface Clock{long now();}
    public interface Factory{ModelHttp create();}
    public static final class Inputs {
        public final int mask;public final short[] points;
        private final long[] frames;private final int generation;
        private Inputs(int mask,short[] points,long[] frames,int generation){this.mask=mask;this.points=points;this.frames=frames;this.generation=generation;}
    }
    private final ModelConfig[] configs;private final int[] alliances;private final Clock clock;private final Factory factory;
    private final ExecutorService workers=Executors.newFixedThreadPool(6,r->{Thread t=new Thread(r,"Model control");t.setDaemon(true);return t;});
    private final ScheduledExecutorService deadlines=Executors.newSingleThreadScheduledExecutor(r->{Thread t=new Thread(r,"Model deadlines");t.setDaemon(true);return t;});
    private final ModelHttp[] active=new ModelHttp[6];private final ModelAction[] actions=new ModelAction[6];
    private final long[] count=new long[6],accepted=new long[6],expired=new long[6],failed=new long[6],next=new long[6],sampled=new long[6],expires=new long[6],frames=new long[6],responseMs=new long[6],submittedMs=new long[6],submittedFrame=new long[6];
    private final long[] headersMs=new long[6],firstTextMs=new long[6],actionMs=new long[6];
    private final int[] fails=new int[6];private final boolean[] stopped=new boolean[6],verified=new boolean[6];private final String[] status=new String[6],feedback=new String[6];
    private final int[][] rejected=new int[6][];
    private final int[][] previousTeam=new int[6][];private final long[] previousTeamAt=new long[6];
    private final ModelWorld[] previousWorld=new ModelWorld[6];
    private final ModelRouteExecutor[] executors=new ModelRouteExecutor[6];
    private final short[] executedPoints=new short[60];private final long[] executionFrame=new long[6];
    private final String[] sentObservation=new String[6];
    private ModelWorld world;private long timeLimitMs=120000,playedMs,resumedAt,observeUs;
    private QaModelBudget qaBudget;private boolean preparing;
    public synchronized void prepare(){preparing=true;playedMs=0;}
    public synchronized void beginPlay(){if(preparing){preparing=false;playedMs=0;resumedAt=clock.now();for(ModelRouteExecutor executor:executors)if(executor!=null)executor.resetHistory();}}
    public synchronized void qaBudget(QaModelBudget budget){if(frame!=0)throw new IllegalStateException("QA budget must precede match");qaBudget=budget;}
    private ModelTerrain terrain;private int[] previous;private long previousAt,frame;private boolean paused=true,closed;private int generation;
    private int cloakMask;private ModelWorld[] views=new ModelWorld[6];
    private ModelWorld view(int team){return views[team]==null?world:views[team];}
    private void rebuildViews(){if(world==null||terrain==null)return;java.util.Map<Integer,ModelWorld> cache=new java.util.HashMap<>();cache.put(0,world);for(int p=0;p<6;p++)if(configs[p].enabled){int mask=ModelVisibility.hiddenMask(p,itemState,alliances);ModelWorld v=cache.get(mask);if(v==null){v=new ModelWorld(ModelVisibility.filter(world.raw,terrain.width,mask),terrain.navigation);cache.put(mask,v);}views[p]=v;}}
    private int itemEpoch=-1;private int[] itemState;
    private org.json.JSONObject itemFacts=new org.json.JSONObject();
    private org.json.JSONObject operationsFacts=new org.json.JSONObject();
    public synchronized void operationsState(int[] raw){try{com.xenris.liquidwarsos.OperationsSnapshot s=new com.xenris.liquidwarsos.OperationsSnapshot(raw);org.json.JSONArray sites=new org.json.JSONArray();for(int i=0;i<s.sites();i++){int a=s.site(i);sites.put(new org.json.JSONArray(java.util.Arrays.copyOfRange(raw,a,a+6)));}operationsFacts=new org.json.JSONObject().put("enabled",s.mode()==1).put("sites_x_y_owner_claim_capture_ms_contested",sites).put("control_scores",new org.json.JSONArray(java.util.Arrays.copyOfRange(raw,7,13))).put("victory_score",raw[4]).put("capture_required_ms",4000).put("capture_radius",38).put("production_every_ms",2000).put("production_population_cap_initial_multiple",2).put("control_per_site_per_second",1);}catch(Exception e){operationsFacts=new org.json.JSONObject();}}
    public synchronized void itemState(int[] s){
        if(s==null||s.length<209)return;
        int currentCloak=0;for(int t=0;t<6;t++){if(s[25+t*20+18]>0)currentCloak|=1<<t;if(s[25+t*20+19]>0)currentCloak|=1<<(t+6);}boolean changed=(itemEpoch>=0&&s[146]!=itemEpoch)||currentCloak!=cloakMask;cloakMask=currentCloak;for(int t=0;t<6;t++)changed|=alliances[t]!=SnapshotItems.group(s,t);
        if(changed){generation++;for(int t=0;t<6;t++)alliances[t]=SnapshotItems.group(s,t);for(int t=0;t<6;t++){if(active[t]!=null)active[t].cancel();active[t]=null;actions[t]=null;previousTeam[t]=null;previousWorld[t]=null;if("eliminated".equals(status[t])||"human".equals(status[t]))stopped[t]=false;alliances[t]=SnapshotItems.group(s,t);if(terrain!=null)executors[t]=new ModelRouteExecutor(terrain.navigation,alliances);}Arrays.fill(next,0);Arrays.fill(executedPoints,(short)-1);playedMs=s[0];resumedAt=clock.now();}
        itemEpoch=s[146];itemState=s.clone();if(changed)rebuildViews();
        try{
            org.json.JSONArray pickups=new org.json.JSONArray(),effects=new org.json.JSONArray(),zones=new org.json.JSONArray(),choices=new org.json.JSONArray(),health=new org.json.JSONArray();
            for(int t=0;t<6;t++)if(s[147+t*10]>0)choices.put(new org.json.JSONArray().put(t).put(s[147+t*10]));
            for(int k=0;k<SnapshotItems.slots(s);k++){int at=SnapshotItems.offset(s,k);if(s[at]>=0){pickups.put(new org.json.JSONArray().put(s[at]).put(s[at+1]).put(s[at+2]));health.put(new org.json.JSONArray().put(s[at]).put(s[at+1]).put(s[at+2]).put(SnapshotItems.hp(s,k)).put(SnapshotItems.maxHp(s,k)).put(SnapshotItems.leader(s,k)));}}
            for(int t=0;t<6;t++)for(int k=0;k<20;k++)if(s[25+t*20+k]>0)effects.put(new org.json.JSONArray().put(t).put(k).put(s[25+t*20+k]));
            for(int k=0;k<s[145]&&146+k*5+4<s.length;k++){org.json.JSONArray z=new org.json.JSONArray();for(int n=0;n<5;n++)z.put(s[146+k*5+n]);zones.put(z);}
            itemFacts=new org.json.JSONObject().put("pickups_kind_x_y",pickups).put("pickup_kind_x_y_hp_max_leader",health).put("team_kind_ms",effects).put("zones_kind_owner_x_y_ms",zones).put("terrain_revision",s[5]).put("refresh_when_all_eaten",true).put("rewind_once_per_match",true).put("rewind_spawned",SnapshotItems.rewindSpawned(s)).put("next_pair_ms",SnapshotItems.count(s)>0?-1:Math.max(0,s[1]-s[0])).put("next_wave_ms",SnapshotItems.count(s)>0?-1:Math.max(0,s[1]-s[0])).put("pickup_count",SnapshotItems.count(s)).put("global_population",s[3]).put("capacity",s[4]).put("pending_team_kind",choices).put("epoch",s[146]).put("elapsed_ms",s[0]);
        }catch(org.json.JSONException ignored){}
    }
    /** New portal IDs cannot be used with old replies. Keep this atomic with requests. */
    public synchronized void replaceTerrain(ModelTerrain next){
        if(closed)return;generation++;
        for(int p=0;p<6;p++){if(active[p]!=null)active[p].cancel();active[p]=null;actions[p]=null;previousTeam[p]=null;previousWorld[p]=null;executors[p]=new ModelRouteExecutor(next.navigation,alliances);}
        Arrays.fill(this.next,0);Arrays.fill(executedPoints,(short)-1);world=null;terrain=next;
    }
    public ModelMatch(ModelConfig[] configs,int[] alliances){this(configs,alliances,()->System.nanoTime()/1000000,ModelHttp::new);}
    public ModelMatch(ModelConfig[] configs,int[] alliances,Clock clock,Factory factory){this.configs=configs.clone();this.alliances=alliances.clone();this.clock=clock;this.factory=factory;Arrays.fill(responseMs,-1);Arrays.fill(submittedMs,-1);Arrays.fill(submittedFrame,-1);Arrays.fill(status,"ready");}
    public synchronized void terrain(byte[] data){if(terrain==null){terrain=new ModelTerrain(data);for(int p=0;p<6;p++)executors[p]=new ModelRouteExecutor(terrain.navigation,alliances);Arrays.fill(executedPoints,(short)-1);}}
    public synchronized void timeLimit(long ms){timeLimitMs=Math.max(0,ms);}
    public synchronized int configuredMask(){int mask=0;for(int p=0;p<6;p++)if(configs[p].enabled)mask|=1<<p;return mask;}
    public synchronized boolean due(int controlled){if(paused||closed)return false;long now=clock.now();for(int p=0;p<6;p++)if((controlled&(1<<p))!=0&&configs[p].enabled&&!stopped[p]&&active[p]==null&&now>=next[p])return true;return false;}
    public synchronized void update(int controlled,int[] snapshot){
        if(paused||closed||terrain==null)return;if(!ModelWorld.valid(snapshot))throw new IllegalArgumentException("Snapshot dimensions");long now=clock.now();long currentFrame=++frame;long begin=System.nanoTime();world=new ModelWorld(snapshot,terrain.navigation);rebuildViews();for(int p=0;p<6;p++)if(configs[p].enabled)executors[p].observe(view(p),p,now);observeUs=(System.nanoTime()-begin)/1000;
        for(int p=0;p<6;p++){
            if((controlled&(1<<p))==0||snapshot[p*8]==0){if(active[p]!=null)active[p].cancel();actions[p]=null;if(configs[p].enabled){stopped[p]=true;status[p]=snapshot[p*8]==0?"eliminated":"human";}continue;}
            if(!configs[p].enabled||stopped[p]||active[p]!=null||now<next[p])continue;
            final int team=p,epoch=generation;final int[] visibleSnapshot=view(p).raw;final String observation;
            try{short[] last=Arrays.copyOfRange(executedPoints,p*10,p*10+10);observation=ModelFacts.observation(p,view(p),previousWorld[p],previousWorld[p]==null?0:now-previousTeamAt[p],alliances,terrain.navigation,actions,executors[p],last,now,preparing?timeLimitMs:timeLimitMs-playedMs-(now-resumedAt)).put("hidden_enemy_ids",ModelVisibility.hiddenTeams(ModelVisibility.hiddenMask(p,itemState,alliances))).put("hidden_is_not_eliminated",true).put("battle_items",itemFacts).put("conquest",operationsFacts).put("preparing",preparing).put("frame",currentFrame).put("response_ms",Math.max(0,responseMs[p])).put("last_feedback",feedback[p]==null?"first command":feedback[p]).put("rejected_xy",rejected[p]==null?org.json.JSONObject.NULL:new org.json.JSONArray(rejected[p])).toString();sentObservation[p]=observation;previousTeam[p]=snapshot.clone();previousWorld[p]=view(p);previousTeamAt[p]=now;}catch(Exception e){status[p]="snapshot";continue;}
            final ModelTerrain requestTerrain=terrain;
            final ModelHttp request=factory.create();request.qaBudget(qaBudget);final boolean verify=!verified[p];active[p]=request;count[p]++;status[p]="waiting";
            final ScheduledFuture<?> timeout=deadlines.schedule(request::cancel,DEADLINE_MS,TimeUnit.MILLISECONDS);
            workers.execute(()->{
                ModelAction action=null;int[] badPoint=null;String result="failed";boolean fatal=false,semantic=false;long retry=0;
                try{action=request.call(configs[team],observation,requestTerrain.description,verify);if(action.enemy>=0&&(alliances[action.enemy]==alliances[team]||visibleSnapshot[action.enemy*8]<=0)){action=null;result="invalid enemy";semantic=true;}else if(!requestTerrain.allows(action)){short[] pts=action.points();badPoint=new int[10];for(int k=0;k<10;k++)badPoint[k]=pts[k];action=null;result="wall";semantic=true;}else result="active";}
                catch(ModelHttp.Failure e){result="HTTP "+e.status;retry=e.retryMs;fatal=e.status==400||e.status==401||e.status==402||e.status==403||e.status==404||e.status==422;}
                catch(ModelHttp.BackendMismatch e){result="model mismatch";fatal=true;}
                catch(QaModelBudget.Exhausted e){result="QA budget reached";fatal=true;}
                catch(IllegalArgumentException e){String reason=e.getMessage();result=Arrays.asList("Action bounds","Point bounds","Action intent","Route bounds","Plan bounds","Hold needs active destination","Stop must have empty points").contains(reason)?"invalid action: "+reason:"invalid action";semantic=true;}
                catch(org.json.JSONException e){result="invalid action format";String detail=e.getMessage();for(String key:new String[]{"to","points","route","hold_ms","enemy","plan"})if(detail!=null&&(detail.contains("[\""+key+"\"]")||detail.equals(key))){result="invalid action field: "+key;break;}semantic=true;}
                catch(Exception e){/* Bodies, prompts and keys are intentionally never logged. */}
                timeout.cancel(false);
                synchronized(ModelMatch.this){if(active[team]!=request)return;active[team]=null;if(closed||paused||generation!=epoch||stopped[team])return;
                    long received=clock.now();responseMs[team]=Math.max(0,received-now);headersMs[team]=request.headersMs;firstTextMs[team]=request.firstTextMs;actionMs[team]=request.actionMs;
                    if(action!=null&&action.route()!=null){String error=executors[team].sameRoute(action)?terrain.navigation.validate(executors[team].startRoom(),action):terrain.navigation.validateFrom(view(team).massRoom[team],view(team).massX[team],view(team).massY[team],action);if(error!=null){action=null;result="route rejected: "+error;semantic=true;}}
                    if(action!=null&&received-now<MAX_AGE_MS){actions[team]=action;executors[team].install(action,view(team),team,received);sampled[team]=now;expires[team]=received+action.holdMs;frames[team]=currentFrame;accepted[team]++;fails[team]=0;verified[team]=true;next[team]=received;}
                    else if(action!=null){expired[team]++;result="stale";next[team]=received;}
                    else{failed[team]++;if(semantic){next[team]=received;}else{actions[team]=null;executors[team].clear();fails[team]=Math.min(5,fails[team]+1);next[team]=received+Math.max(retry,Math.min(10000,1000L<<(fails[team]-1)));}}
                    stopped[team]=fatal;if(fatal)actions[team]=null;status[team]=result;feedback[team]=result;rejected[team]=badPoint;
                }
            });
        }
        previous=snapshot.clone();previousAt=now;
        tickExecution();
    }
    /** Game-worker call even between 100-200ms fresh observations. */
    public synchronized void tickExecution(){if(world==null||terrain==null)return;long now=clock.now();Arrays.fill(executedPoints,(short)-1);for(int p=0;p<6;p++)if(valid(p,now)){short[] points=executors[p].points(view(p),p,now);System.arraycopy(points,0,executedPoints,p*10,10);executionFrame[p]=frames[p];}}
    private boolean valid(int p,long now){return !paused&&!closed&&!stopped[p]&&actions[p]!=null&&now<expires[p];}
    public synchronized Inputs inputs(){int mask=0;long now=clock.now();short[] points=new short[60];Arrays.fill(points,(short)-1);for(int p=0;p<6;p++)if(valid(p,now)&&executionFrame[p]==frames[p]){mask|=1<<p;System.arraycopy(executedPoints,p*10,points,p*10,10);
 if(itemState!=null){int at=147+p*10,kind=itemState[at];ModelAction act=actions[p];int x=-1,y=0;if(kind==3&&act.itemTo!=null){x=act.itemTo[0];y=act.itemTo[1];}else if((kind==7||kind==8)&&act.itemTeam>=0)x=act.itemTeam;
 if(x>=0){points[p*10+3]=(short)x;points[p*10+8]=(short)y;points[p*10+4]=-3;points[p*10+9]=(short)itemState[at+1];}}
 }return new Inputs(mask,points,frames.clone(),generation);}
    /** Called only after native input submission, not when a worker merely receives JSON. */
    public synchronized void submitted(Inputs packet){if(packet==null||paused||closed||packet.generation!=generation)return;long now=clock.now();for(int p=0;p<6;p++)if((packet.mask&(1<<p))!=0&&packet.frames[p]==frames[p]&&submittedFrame[p]!=frames[p]){submittedMs[p]=Math.max(0,now-sampled[p]);submittedFrame[p]=frames[p];}}
    public synchronized String summary(boolean zh){StringBuilder b=new StringBuilder();long now=clock.now();for(int p=0;p<6;p++)if(configs[p].enabled){boolean valid=valid(p,now);String s=closed?"stopped":paused?"paused":status[p];if(!valid&&"active".equals(s))s="expired";if(zh)s=s.replace("model mismatch","服务端模型不符").replace("ready","待请求").replace("waiting","请求中").replace("active","已响应").replace("failed","网络/格式失败").replace("stopped","已停止").replace("expired","动作已过期").replace("stale","回复已过期").replace("paused","暂停").replace("eliminated","已淘汰").replace("human","真人接管").replace("wall","触点落在墙里").replace("snapshot","状态无效").replace("invalid enemy","目标不是存活敌队");
            b.append(p+1).append(" · ").append(configs[p].model).append(" · ").append(valid?(zh?"模型直控":"Model direct"):(zh?"等待模型 · 停住":"Waiting / stopped")).append("\n").append(s).append(zh?" | 响应 ":" | response ").append(responseMs[p]<0?"—":responseMs[p]+"ms").append(zh?" / 提交 ":" / submitted ").append(submittedMs[p]<0?"—":submittedMs[p]+"ms").append(valid?(zh?" / 指令年龄 ":" / age ")+(now-sampled[p])+"ms":"").append("\n").append(zh?"请求/有效/过期/失败 ":"sent/valid/stale/failed ").append(count[p]).append('/').append(accepted[p]).append('/').append(expired[p]).append('/').append(failed[p]).append("\n\n");}return b.toString().trim();}
    public synchronized String brief(boolean zh){int configured=Integer.bitCount(configuredMask()),direct=Integer.bitCount(inputs().mask);return configured==0?"":(zh?"模型直控 ":"Model direct ")+direct+"/"+configured+(zh?" · 无指令的模型队伍停住":" · no command = stopped");}
    public synchronized void pause(boolean value){if(closed||paused==value)return;long now=clock.now();if(value&&!preparing)playedMs+=Math.max(0,now-resumedAt);else resumedAt=now;paused=value;if(value){generation++;Arrays.fill(actions,null);Arrays.fill(next,0);previous=null;Arrays.fill(previousTeam,null);Arrays.fill(previousWorld,null);for(ModelRouteExecutor executor:executors)if(executor!=null)executor.clear();for(ModelHttp c:active)if(c!=null)c.cancel();}}
    public synchronized org.json.JSONObject diagnostics(){org.json.JSONObject out=new org.json.JSONObject();try{out.put("navigation_ms",terrain==null?-1:terrain.navigation.buildMs).put("observe_us",observeUs);org.json.JSONArray teams=new org.json.JSONArray();for(int p=0;p<6;p++)if(configs[p].enabled){org.json.JSONObject item=new org.json.JSONObject().put("team",p);if(sentObservation[p]!=null)item.put("observation",new org.json.JSONObject(sentObservation[p]));if(actions[p]!=null)item.put("action",new org.json.JSONObject().put("enemy",actions[p].enemy).put("intent",actions[p].intent).put("route",actions[p].route()==null?org.json.JSONObject.NULL:new org.json.JSONArray(actions[p].route())).put("plan",actions[p].plan).put("hold_ms",actions[p].holdMs));teams.put(item);}out.put("teams",teams);}catch(org.json.JSONException ignored){}return out;}
    public synchronized org.json.JSONObject liveDiagnostics(){org.json.JSONObject out=new org.json.JSONObject();try{out.put("navigation_ms",terrain==null?-1:terrain.navigation.buildMs).put("observe_us",observeUs);if(world!=null){org.json.JSONArray masses=new org.json.JSONArray(),commands=new org.json.JSONArray();for(int p=0;p<6;p++){masses.put(new org.json.JSONArray().put(world.massX[p]).put(world.massY[p]).put(world.massCount[p]).put(world.massRoom[p]));if(configs[p].enabled)commands.put(new org.json.JSONObject().put("team",p).put("progress",executors[p].facts(world,p,clock.now(),Arrays.copyOfRange(executedPoints,p*10,p*10+10))));}out.put("masses",masses).put("contacts",new org.json.JSONArray(Arrays.copyOfRange(world.raw,120,156))).put("commands",commands);}}catch(org.json.JSONException ignored){}return out;}
    @Override public synchronized void close(){if(closed)return;pause(true);closed=true;for(ModelHttp c:active)if(c!=null)c.cancel();workers.shutdownNow();deadlines.shutdownNow();}
}
