// SPDX-License-Identifier: GPL-3.0-or-later
package com.xenris.liquidwarsos.cloud;
import java.util.*;import org.json.*;
/** Per-controller view; the authority keeps its unmodified native snapshot. */
public final class ModelVisibility {
 public static int hiddenMask(int viewer,int[] items,int[] alliances){if(items==null||items.length<209)return 0;int mask=0;boolean reveal=false;for(int t=0;t<6;t++)if(items[13+t]>0&&alliances[t]==alliances[viewer]&&items[25+t*20+19]>0)reveal=true;
 if(!reveal)for(int t=0;t<6;t++)if(alliances[t]!=alliances[viewer]&&items[25+t*20+18]>0)mask|=1<<t;return mask;}
 public static int[] filter(int[] raw,int width,int hidden){if(hidden==0)return raw;int shift=width>2048?22:width>1024?21:19,n=0;for(int k=217;k<raw.length;k++)if((hidden&(1<<((raw[k]>>>shift)&7)))==0)n++;
 int[] out=Arrays.copyOf(raw,217+n);out[216]=n;int at=217;for(int k=217;k<raw.length;k++)if((hidden&(1<<((raw[k]>>>shift)&7)))==0)out[at++]=raw[k];
 for(int t=0;t<6;t++){out[t*8+7]=0;if((hidden&(1<<t))==0)continue;Arrays.fill(out,t*8,t*8+8,0);Arrays.fill(out,48+t*12,48+t*12+12,0);Arrays.fill(out,156+t*10,156+t*10+10,-1);for(int p=0;p<6;p++)out[120+t*6+p]=out[120+p*6+t]=0;}return out;}
 public static JSONArray hiddenTeams(int hidden){JSONArray a=new JSONArray();for(int t=0;t<6;t++)if((hidden&(1<<t))!=0)a.put(t);return a;}
}
