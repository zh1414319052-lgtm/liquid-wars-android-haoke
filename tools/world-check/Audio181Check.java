package com.xenris.liquidwarsos;
import java.util.*;
public final class Audio181Check {
 static int checks;static void check(boolean pass,String why){checks++;if(!pass)throw new AssertionError(why);}
 static int[] items(){int[] s=new int[400];s[13]=s[14]=100;for(int t=0;t<6;t++)s[147+t*10+8]=t;int h=316;s[h]=0x4c574850;s[h+1]=6;for(int i=0;i<6;i++){s[h+2+i*10]=s[h+3+i*10]=1200;s[h+4+i*10]=-1;s[h+5+i*10]=-1;s[380+i*3]=-1;}s[378]=0x4c573136;s[379]=6;return s;}
 static OperationsSnapshot ops(){int[] a=new int[189];a[0]=0x4c574f50;a[1]=1;a[4]=180;a[5]=1;a[6]=-1;a[13]=a[14]=200;a[15]=a[16]=-1;return new OperationsSnapshot(a);}
 static void event(int[] s,int team,int target,int amount){s[0]+=70;s[6]++;s[208]=1;int a=209;s[a]=s[6];s[a+1]=3;s[a+2]=team;s[a+5]=amount;s[a+6]=s[0];s[a+7]=target;s[a+8]=2;}
 public static void main(String[] args){
  AudioEvents e=new AudioEvents();int[] s=items();OperationsSnapshot o=ops();check(e.observe(s,o,0,false)==-1,"first snapshot is silent");
  event(s,1,-1,20);check(e.observe(s,o,0,false)==-1,"enemy event creates no positional sound");event(s,0,-1,20);check(e.observe(s,o,0,false)==AudioEvents.ITEM,"own item activation");check(e.observe(s,o,0,false)==-1,"same serial never replays");
  s[25+20+18]=20000;event(s,0,1,20);check(e.observe(s,o,0,false)==-1,"targeted hidden enemy is not exposed by sound");s[25+20+18]=0;
  s[380]=11;s[381]=200;s[382]=200;s[320]=0;s[321]=0;check(e.observe(s,o,0,false)==-1,"first pickup reading is silent");s[318]-=30;s[322]+=30;s[0]+=70;check(e.observe(s,o,0,false)==AudioEvents.DEVOUR,"HP loss with own contribution");s[321]=1;s[318]-=30;s[323]+=30;check(e.observe(s,o,0,false)==-1,"hostile devouring remains silent even when own team leads cumulative damage");
  o.raw[15]=0;check(e.observe(s,o,0,false)==AudioEvents.CAPTURE,"capture transition");o.raw[19]+=10;check(e.observe(s,o,0,false)==AudioEvents.PRODUCE,"actual production count increase");check(e.observe(s,o,0,false)==-1,"stationary ownership/born is silent");o.raw[18]=1;check(e.observe(s,o,0,false)==AudioEvents.ALERT,"public contest on own site");o.raw[15]=-1;check(e.observe(s,o,0,false)==AudioEvents.ALERT,"lost ownership");
  s[146]++;s[0]=0;o.raw[3]++;o.raw[15]=-1;o.raw[19]=0;check(e.observe(s,o,0,false)==AudioEvents.REWIND,"epoch changes once");check(e.observe(s,o,0,false)==-1,"rewind not replayed");e.reset();event(s,0,-1,4);o.raw[15]=0;check(e.observe(s,o,0,false)==-1,"resume baselines pending old events");
  o.raw[o.teamOffset[0]+1]=3;o.raw[o.teamOffset[0]+2]++;check(e.observe(s,o,0,false)==AudioEvents.ALERT,"new rejected route warns");check(e.observe(s,o,0,false)==-1,"rejection does not loop");
  AudioEvents rate=new AudioEvents();int played=0;for(long n=0;n<10000;n+=7)if(rate.allow(AudioEvents.DEVOUR,n))played++;check(played<=16,"dense swarm cannot cause per-particle sound");check(rate.allow(AudioEvents.ORDER,10000),"new command after quiet interval");check(!rate.allow(AudioEvents.PRODUCE,10010),"shared burst limiter");
  int[] before=s.clone(),opBefore=o.raw.clone();e.observe(s,o,0,false);check(Arrays.equals(s,before)&&Arrays.equals(o.raw,opBefore),"observer does not mutate snapshots");System.out.println("PASS "+checks+" audio presentation event checks; 10s flood => "+played+" devour cues");
 }
}
