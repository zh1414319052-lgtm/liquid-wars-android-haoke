// SPDX-License-Identifier: GPL-3.0-or-later
package com.xenris.liquidwarsos;
import java.util.Arrays;
import com.xenris.liquidwarsos.cloud.SnapshotItems;
/** Read-only presentation event detector. It never consumes simulation RNG or writes state. */
public final class AudioEvents {
 public static final int ORDER=0,ROUTE=1,DEVOUR=2,ITEM=3,CAPTURE=4,PRODUCE=5,ALERT=6,REWIND=7,COUNT=8;
 public static final String[] FILES={"order","route","devour","item","capture","produce","alert","rewind"};
 private int epoch=-1,time=-1,serial,siteCount,lastOrder;private final int[] hp=new int[6],damage=new int[6],key=new int[6],owner=new int[5],born=new int[5],contest=new int[5];
 private final long[] played=new long[COUNT];private long lastAny=-10000;
 private static final long[] GAP={100,180,650,300,650,1500,1800,1200};
 public AudioEvents(){reset();}
 public void reset(){epoch=-1;time=-1;serial=0;siteCount=-1;Arrays.fill(played,-10000);lastAny=-10000;}
 public boolean allow(int sound,long now){if(sound<0||sound>=COUNT||now-played[sound]<GAP[sound]||now-lastAny<65)return false;played[sound]=now;lastAny=now;return true;}
 public int observe(int[] s,OperationsSnapshot o,int viewer,boolean spectator){
  if(s==null||s.length<209||viewer<0||viewer>=6)return -1;
  boolean initial=epoch<0,reset=!initial&&(epoch!=s[146]||s[0]<time),baseline=initial||reset;int candidate=-1;
  if(reset)candidate=REWIND;
  if(!baseline){
   for(int n=0;n<s[208];n++){int a=209+n*9;if(a+8>=s.length)break;int age=s[0]-s[a+6],team=s[a+2],target=s[a+7];
    if(s[a]<=serial||age<0||age>350||(!spectator&&team!=viewer))continue;
    // An own targeted attempt must not expose a still concealed hostile faction.
    if(!spectator&&target>=0&&target<6&&hidden(s,viewer,target))continue;
    candidate=pick(candidate,s[a+5]<0?ALERT:ITEM);
   }
  }
  for(int i=0;i<SnapshotItems.slots(s);i++){int a=SnapshotItems.offset(s,i),kind=s[a],k=kind<0?-1:((s[a+1]*2048+s[a+2])*31+kind),health=SnapshotItems.hp(s,i);
   int ownDamage=SnapshotItems.contribution(s,i,viewer);
   if(!baseline&&key[i]==k&&kind>=0&&health<hp[i]&&(spectator||ownDamage>damage[i]))candidate=pick(candidate,DEVOUR);
   key[i]=k;hp[i]=health;damage[i]=ownDamage;
  }
  if(o!=null&&o.epoch()==s[146]){boolean siteBaseline=baseline||siteCount!=o.sites();
   int order=o.raw[o.teamOffset[viewer]+2];if(!baseline&&order!=lastOrder&&o.feedback(viewer)>=3)candidate=pick(candidate,ALERT);lastOrder=order;
   for(int i=0;i<o.sites();i++){int a=o.site(i),own=o.raw[a+2],made=o.raw[a+6],cont=o.raw[a+5];
    if(!siteBaseline){
     if(own!=owner[i]&&(spectator?own>=0:own==viewer))candidate=pick(candidate,CAPTURE);
     if(!spectator&&owner[i]==viewer&&own!=viewer)candidate=pick(candidate,ALERT);
     if(own==viewer&&contest[i]==0&&cont!=0)candidate=pick(candidate,ALERT);
     if(made>born[i]&&(spectator||own==viewer))candidate=pick(candidate,PRODUCE);
    }owner[i]=own;born[i]=made;contest[i]=cont;
   }siteCount=o.sites();
  }
  epoch=s[146];time=s[0];serial=s[6];return candidate;
 }
 private static int pick(int a,int b){return a<0||priority(b)>priority(a)?b:a;}
 public static int priority(int sound){return sound==REWIND?5:sound==ALERT?4:sound==CAPTURE||sound==ITEM?3:sound==ORDER||sound==ROUTE?2:1;}
 static boolean hidden(int[] s,int viewer,int team){if(s[25+team*20+18]<=0)return false;int own=SnapshotItems.group(s,viewer);if(own==SnapshotItems.group(s,team))return false;for(int t=0;t<6;t++)if(s[13+t]>0&&s[25+t*20+19]>0&&SnapshotItems.group(s,t)==own)return false;return true;}
}
