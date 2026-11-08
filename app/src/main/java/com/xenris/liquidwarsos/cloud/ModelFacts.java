// SPDX-License-Identifier: GPL-3.0-or-later
package com.xenris.liquidwarsos.cloud;
import org.json.*;import java.util.*;

/** Compact objective observations. No opponent scoring or recommended action. */
public final class ModelFacts {
 public static JSONObject observation(int team,ModelWorld world,ModelWorld previous,long dt,int[] alliances,ModelNavigation nav,ModelAction[] actions,ModelRouteExecutor executor,short[] touch,long now,long remaining)throws JSONException{
  JSONObject self=army(team,world,previous,dt,alliances).put("rank",world.rank(team)).put("room_population",world.distribution(team));
  JSONArray enemies=new JSONArray(),allies=new JSONArray();int start=world.massRoom[team];
  for(int p=0;p<6;p++)if(p!=team&&world.count(p)>0){JSONObject army=army(p,world,previous,dt,alliances);
   if(alliances[p]==alliances[team]){ModelAction a=actions[p];army.put("current_touch",new JSONArray().put(world.raw[156+p*10]).put(world.raw[161+p*10])).put("route",a==null||a.route()==null?new JSONArray():new JSONArray(a.route())).put("plan",a==null?"unshared local plan":a.plan);allies.put(army);continue;}
   List<ModelNavigation.Route> routes=nav.routes(start,world.massRoom[p],3);JSONArray candidates=new JSONArray();double shortest=Double.POSITIVE_INFINITY;
   for(ModelNavigation.Route route:routes){ModelNavigation.Room from=nav.rooms.get(start),to=nav.rooms.get(world.massRoom[p]);double length=route.length+Math.hypot(world.massX[team]-from.x,world.massY[team]-from.y)+Math.hypot(world.massX[p]-to.x,world.massY[p]-to.y);
    if(route.doors.length==0&&nav.clear(world.massX[team],world.massY[team],world.massX[p],world.massY[p]))length=Math.hypot(world.massX[p]-world.massX[team],world.massY[p]-world.massY[team]);
    shortest=Math.min(shortest,length);JSONArray troops=new JSONArray();for(int q=0;q<6;q++)if(q!=team){int count=0;for(int room:route.rooms)count+=world.population[q][room];if(count>0)troops.put(new JSONArray().put(q).put(count));}
    candidates.put(new JSONObject().put("doors",new JSONArray(route.doors)).put("length",Math.round(length)).put("eta_s_at_140",Math.round(length/14.)/10.).put("troops_in_route_regions",troops));
   }
   army.put("walk_distance",Double.isFinite(shortest)?Math.round(shortest):-1).put("eta_s_at_140",Double.isFinite(shortest)?Math.round(shortest/14.)/10.:-1).put("direct_clear",nav.clear(world.massX[team],world.massY[team],world.massX[p],world.massY[p])).put("routes_by_length",candidates);enemies.put(army);
  }
  int[] nearby=new int[6];for(int i=ModelWorld.HEADER;i<world.raw.length;i++){int v=world.raw[i],x=world.x(v),y=world.y(v),p=world.team(v);if((x-world.massX[team])*(x-world.massX[team])+(y-world.massY[team])*(y-world.massY[team])<=64*64)nearby[p]++;}
  return new JSONObject().put("remaining_s",Math.max(0,remaining/1000)).put("YOU",self).put("ENEMIES",enemies).put("ALLIES",allies).put("troops_within64_of_your_mass_by_team",new JSONArray(nearby)).put("previous_command",executor.facts(world,team,now,touch));
 }
 private static JSONObject army(int p,ModelWorld w,ModelWorld previous,long dt,int[] alliances)throws JSONException{
  JSONObject out=new JSONObject().put("id",p).put("count",w.count(p)).put("hp",w.raw[p*8+1]).put("mass",new JSONArray().put(w.massX[p]).put(w.massY[p]).put(w.massCount[p])).put("room",w.massRoom[p]).put("room_population",w.distribution(p)).put("adjacent_hostile_contacts",w.contacts(p,alliances));
  if(previous!=null&&dt>0)out.put("count_change",w.count(p)-previous.count(p)).put("sample_ms",dt).put("velocity",new JSONArray().put((w.massX[p]-previous.massX[p])*1000/dt).put((w.massY[p]-previous.massY[p])*1000/dt));return out;
 }
}
