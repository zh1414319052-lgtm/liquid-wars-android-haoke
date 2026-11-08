// SPDX-License-Identifier: GPL-3.0-or-later
package com.xenris.liquidwarsos.cloud;
import org.json.*;
import java.util.Arrays;

/** A validated five-finger command, never executable model text. */
public final class ModelAction {
    private final short[] points;
    private final int[] route;
    public final int holdMs,enemy;
    public int itemTeam=-1;public short[] itemTo=null;
    public final String intent,plan;
    public ModelAction(short[] points,int holdMs){this(points,holdMs,-1,"move");}
    public ModelAction(short[] points,int holdMs,int enemy,String intent) {
        this(points,holdMs,enemy,intent,null,"");
    }
    public ModelAction(short[] points,int holdMs,int enemy,String intent,int[] route,String plan) {this(points,holdMs,enemy,intent,route,plan,800,480);}
    private ModelAction(short[] points,int holdMs,int enemy,String intent,int[] route,String plan,int width,int height){
        if(points.length!=10||holdMs<100||holdMs>(route==null?2000:6000))throw new IllegalArgumentException("Action bounds");
        for(int i=0;i<5;i++)if(!((points[i]==-1&&points[i+5]==-1)||(points[i]>=0&&points[i]<width&&points[i+5]>=0&&points[i+5]<height)))throw new IllegalArgumentException("Point bounds");
        if(enemy< -1||enemy>5||!java.util.Arrays.asList("move","attack","flank","retreat","hold","regroup","stop").contains(intent))throw new IllegalArgumentException("Action intent");
        if(route!=null&&(route.length>64||Arrays.stream(route).anyMatch(v->v<0)))throw new IllegalArgumentException("Route bounds");
        if(plan==null||plan.codePointCount(0,plan.length())>40||plan.chars().anyMatch(Character::isISOControl))throw new IllegalArgumentException("Plan bounds");
        if("hold".equals(intent)&&points[0]<0)throw new IllegalArgumentException("Hold needs active destination");
        if("stop".equals(intent)&&points[0]>=0)throw new IllegalArgumentException("Stop must have empty points");
        this.points=points.clone();this.holdMs=holdMs;this.enemy=enemy;this.intent=intent;this.route=route==null?null:route.clone();this.plan=plan;
    }
    public short[] points(){return points.clone();}
    public int[] route(){return route==null?null:route.clone();}
    static int integer(Object value)throws JSONException{
        if(!(value instanceof Number))throw new JSONException("Integer required");
        double n=((Number)value).doubleValue();if(!Double.isFinite(n)||n!=Math.rint(n)||n<Integer.MIN_VALUE||n>Integer.MAX_VALUE)throw new JSONException("Integer required");return (int)n;
    }
    public static ModelAction parse(JSONObject object)throws JSONException{return parse(object,800,480);}
    public static ModelAction parse(JSONObject object,int width,int height)throws JSONException{
        if(!((width==800&&height==480)||(width==1600&&height==960)||(width==3200&&height==960)))throw new JSONException("World bounds");
        boolean stop="stop".equals(object.optString("intent","move"));
        JSONArray a=object.optJSONArray("points");if(a==null)a=new JSONArray();
        if(object.has("to")&&object.isNull("to")&&!stop)throw new JSONException("Destination pair");
        if(object.has("to")&&!object.isNull("to")){JSONArray to=object.getJSONArray("to");if(to.length()!=2)throw new JSONException("Destination pair");JSONArray combined=new JSONArray().put(to);for(int i=0;i<a.length();i++)combined.put(a.get(i));a=combined;}
        if(!object.has("points")&&!object.has("route")&&!object.has("to"))throw new JSONException("Action destination required");
        if(a.length()>5)throw new JSONException("Five fingers maximum");
        short[] p=new short[10];Arrays.fill(p,(short)-1);
        for(int i=0;i<a.length();i++){JSONArray pair=a.getJSONArray(i);if(pair.length()!=2)throw new JSONException("Point pair");int x=integer(pair.get(0)),y=integer(pair.get(1));if(x<0||x>=width||y<0||y>=height)throw new JSONException("Point bounds");p[i]=(short)x;p[i+5]=(short)y;}
        int[] route=null;if(object.has("route")){JSONArray r=stop&&object.isNull("route")?new JSONArray():object.getJSONArray("route");if(r.length()>64)throw new JSONException("Route length");route=new int[r.length()];for(int i=0;i<r.length();i++)route[i]=integer(r.get(i));}
        ModelAction result=new ModelAction(p,integer(object.get("hold_ms")),object.has("enemy")?integer(object.get("enemy")):-1,object.optString("intent","move"),route,object.optString("plan",""),width,height);
        if(object.has("item_team")){int t=integer(object.get("item_team"));if(t<0||t>=6)throw new JSONException("Item team");result.itemTeam=t;}
        if(object.has("item_to")){JSONArray to=object.getJSONArray("item_to");if(to.length()!=2)throw new JSONException("Item destination");int x=integer(to.get(0)),y=integer(to.get(1));if(x<16||x>=width-16||y<4||y>=height-4)throw new JSONException("Item destination bounds");result.itemTo=new short[]{(short)x,(short)y};}
        return result;
    }
    public static ModelAction simple(JSONObject object)throws JSONException{return simple(object,800,480);}
    public static ModelAction simple(JSONObject object,int width,int height)throws JSONException{
        if(!object.getBoolean("success")||!"move_points".equals(object.getString("function")))throw new JSONException("Function required");
        JSONObject args=object.getJSONObject("args");JSONArray points=new JSONArray();
        for(int i=1;i<=5;i++){String pair=args.getString("p"+i);if("-".equals(pair))continue;if(!pair.matches("[0-9]{1,4},[0-9]{1,4}"))throw new JSONException("Point pair");String[] xy=pair.split(",");points.put(new JSONArray().put(Integer.parseInt(xy[0])).put(Integer.parseInt(xy[1])));}
        return parse(new JSONObject().put("points",points).put("hold_ms",args.get("hold_ms")),width,height);
    }
}
