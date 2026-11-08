// SPDX-License-Identifier: GPL-3.0-or-later
package com.xenris.liquidwarsos.cloud;
import java.util.*;
import org.json.*;

/** Facts reconstructed from an immutable native copy, never simulation inputs. */
public final class ModelWorld {
 public static final int HEADER=217,MAX_PARTICLES=108000;
 public final int[] raw;private final boolean wide,huge;
 public int x(int v){return v&(huge?4095:wide?2047:1023);}public int y(int v){return (v>>>(huge?12:wide?11:10))&(wide?1023:511);}public int team(int v){return (v>>>(huge?22:wide?21:19))&7;}
 public final int[][] population;
 public final int[] massX=new int[6],massY=new int[6],massRoom=new int[6],massCount=new int[6];
 public static boolean valid(int[] s){return s!=null&&s.length>=HEADER&&s[216]>=0&&s[216]<=MAX_PARTICLES&&s.length==HEADER+s[216];}
 public ModelWorld(int[] snapshot,ModelNavigation nav){
  if(!valid(snapshot))throw new IllegalArgumentException("Snapshot dimensions");raw=snapshot;wide=nav.W>1024;huge=nav.W>2048;int cols=(nav.W+19)/20,rows=(nav.H+19)/20,binCount=cols*rows;
  population=new int[6][nav.rooms.size()];int[][] bx=new int[6][binCount],by=new int[6][binCount],bins=new int[6][binCount];
  for(int i=HEADER;i<snapshot.length;i++){int v=snapshot[i],x=x(v),y=y(v),t=team(v);if(t>=6||x>=nav.W||y>=nav.H)throw new IllegalArgumentException("Particle bounds");int r=nav.roomAt(x,y);if(r>=0)population[t][r]++;int c=y/20*cols+x/20;bins[t][c]++;bx[t][c]+=x;by[t][c]+=y;}
  for(int t=0;t<6;t++){
   // Largest connected occupied component, not one arbitrary 40x40 bucket.
   boolean[] seen=new boolean[binCount];int[] queue=new int[binCount];int best=0,sx=0,sy=0;
   for(int c=0;c<binCount;c++)if(bins[t][c]>0&&!seen[c]){int h=0,n=0,count=0,sumX=0,sumY=0;queue[n++]=c;seen[c]=true;
    while(h<n){int at=queue[h++];count+=bins[t][at];sumX+=bx[t][at];sumY+=by[t][at];for(int dy=-1;dy<=1;dy++)for(int dx=-1;dx<=1;dx++){int x=at%cols+dx,y=at/cols+dy;if(x<0||y<0||x>=cols||y>=rows)continue;int q=y*cols+x;if(seen[q]||bins[t][q]==0)continue;int ax=bx[t][at]/bins[t][at],ay=by[t][at]/bins[t][at],xx=bx[t][q]/bins[t][q],yy=by[t][q]/bins[t][q];if(nav.clear(ax,ay,xx,yy)){seen[q]=true;queue[n++]=q;}}}
    if(count>best){best=count;sx=sumX;sy=sumY;}
   }
   int cx=best>0?sx/best:raw[t*8+4],cy=best>0?sy/best:raw[t*8+5];int distance=Integer.MAX_VALUE,ax=cx,ay=cy;
   // Anchor the component centroid on an actual friendly particle.
   for(int i=HEADER;i<snapshot.length;i++){int v=snapshot[i];if(team(v)!=t)continue;int x=x(v),y=y(v),d=(x-cx)*(x-cx)+(y-cy)*(y-cy);if(d<distance){distance=d;ax=x;ay=y;}}
   massX[t]=ax;massY[t]=ay;massCount[t]=best;massRoom[t]=nav.roomAt(ax,ay);
  }
 }
 public int count(int t){return raw[t*8];}
 public int rank(int t){int n=1;for(int p=0;p<6;p++)if(count(p)>count(t))n++;return n;}
 public JSONArray distribution(int t){JSONArray a=new JSONArray();for(int r=0;r<population[t].length;r++)if(population[t][r]>0)a.put(new JSONArray().put(r).put(population[t][r]));return a;}
 public JSONArray contacts(int t,int[] alliances){JSONArray a=new JSONArray();for(int p=0;p<6;p++)if(alliances[p]!=alliances[t]&&raw[120+t*6+p]>0)a.put(new JSONArray().put(p).put(raw[120+t*6+p]));return a;}
}
