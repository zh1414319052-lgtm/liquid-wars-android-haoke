// SPDX-License-Identifier: GPL-3.0-or-later
package com.xenris.liquidwarsos.cloud;
import java.util.*;import org.json.*;

/** Mechanical host-only execution of the exact model-selected door sequence.
 * Never selects an enemy, a different door, a pursuit target or a new route. */
public final class ModelRouteExecutor {
 private final ModelNavigation nav;
 private final int[] alliances;
 private ModelAction action;
 private List<int[]> path=Collections.emptyList();
 private int index,initialCount,startRoom,lastWaitDoor=-1;
 private long issuedAt,arrivedAt;
 private final ArrayDeque<long[]> history=new ArrayDeque<>();
 public ModelRouteExecutor(ModelNavigation nav){this(nav,new int[]{0,1,2,3,4,5});}
 public ModelRouteExecutor(ModelNavigation nav,int[] alliances){this.nav=nav;this.alliances=alliances.clone();}
 public boolean sameRoute(ModelAction next){return action!=null&&action.route()!=null&&next.route()!=null&&next.route().length>0&&Arrays.equals(action.route(),next.route());}
 public int startRoom(){return startRoom;}
 public void install(ModelAction next,ModelWorld world,int team,long now){
  boolean keep=sameRoute(next);action=next;
  if(!keep){index=0;arrivedAt=0;lastWaitDoor=-1;issuedAt=now;initialCount=world.count(team);startRoom=world.massRoom[team];
   path=next.route()==null||"stop".equals(next.intent)?Collections.emptyList():nav.waypoints(startRoom,next.route());
  }
 }
 public void clear(){action=null;path=Collections.emptyList();index=0;history.clear();}
 public void resetHistory(){history.clear();}
 public void observe(ModelWorld world,int team,long now){if(!history.isEmpty()&&history.peekLast()[0]==now)return;history.add(new long[]{now,world.massX[team],world.massY[team]});while(history.size()>1&&history.peekFirst()[0]<now-2200)history.removeFirst();}
 public short[] points(ModelWorld world,int team,long now){
  short[] out=new short[10];Arrays.fill(out,(short)-1);if(action==null||"stop".equals(action.intent))return out;
  if(action.route()==null)return action.points();
  int x=world.massX[team],y=world.massY[team];short[] terminal=action.points();
  while(index+1<path.size()){
   int[] at=path.get(index),next=path.get(index+1);boolean reached=Math.hypot(x-at[0],y-at[1])<12||nav.clear(x,y,next[0],next[1]);if(!reached)break;
   // Allow a bounded catch-up interval at a crossed door; never wait forever.
   if(at[2]>=0&&at[2]!=lastWaitDoor){
    if(world.population[team][world.massRoom[team]]<world.count(team)*.65){if(arrivedAt==0)arrivedAt=now;if(now-arrivedAt<500)break;}
    lastWaitDoor=at[2];
   }
   index++;arrivedAt=0;
  }
  if(index<path.size()){
   int[] at=path.get(index);if(index==path.size()-1&&nav.clear(x,y,terminal[0],terminal[5]))index=path.size();
   else{out[0]=(short)at[0];out[5]=(short)at[1];return out;}
  }
  if(Math.hypot(x-terminal[0],y-terminal[5])>20){out[0]=terminal[0];out[5]=terminal[5];return out;}
  return terminal;
 }
 public JSONObject facts(ModelWorld world,int team,long now,short[] target)throws JSONException{
  JSONObject out=new JSONObject().put("plan",action==null?"":action.plan).put("enemy",action==null?-1:action.enemy).put("intent",action==null?"none":action.intent).put("route",action==null||action.route()==null?new JSONArray():new JSONArray(action.route())).put("waypoint_index",index).put("waypoint_count",path.size()).put("age_ms",action==null?0:now-issuedAt).put("count_change",action==null?0:world.count(team)-initialCount).put("legal_doors",new JSONArray(nav.legalDoors(world.massRoom[team])));
  long[] first=history.peekFirst(),last=history.peekLast();double moved=first==null||last==null?0:Math.hypot(last[1]-first[1],last[2]-first[2]);long span=first==null||last==null?0:last[0]-first[0];int contacts=0;for(int p=0;p<6;p++)if(alliances[p]!=alliances[team])contacts+=world.raw[120+team*6+p];
  boolean blocked=target!=null&&target[0]>=0&&!nav.clear(world.massX[team],world.massY[team],target[0],target[5]);
  return out.put("main_displacement_2s",Math.round(moved)).put("movement_sample_ms",span).put("blocked_line_to_touch",blocked).put("wall_stalled",span>=1900&&moved<20&&blocked&&contacts==0).put("at",new JSONArray().put(world.massX[team]).put(world.massY[team])).put("touch",target==null?new JSONArray():new JSONArray().put(target[0]).put(target[5]));
 }
}
