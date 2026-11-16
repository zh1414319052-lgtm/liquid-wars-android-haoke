// SPDX-License-Identifier: GPL-3.0-or-later
package com.xenris.liquidwarsos;

/** Versioned, viewer-filtered authority facts, never a second game simulation. */
public final class ExpeditionSnapshot {
 public static final int ITEMS=2,STRIDE=63;
 public final int[] raw;
 public final int teams,time,duration,cutoff,recon,reconRemaining,loot,obstacles,anchors,chills;
 public final int lootBase,obstacleBase,anchorBase,chillBase;
 public ExpeditionSnapshot(int[] d){
  if(d==null||d.length<11||d[0]!=193||d[4]<2||d[4]>10)throw new IllegalArgumentException("Expedition header");
  raw=d;time=d[1];duration=d[2];cutoff=d[3];teams=d[4];recon=d[5];reconRemaining=d[6];loot=d[7];obstacles=d[8];anchors=d[9];chills=d[10];
  if(loot<0||loot>40||obstacles<0||obstacles>40||anchors<0||anchors>10||chills<0||chills>10)throw new IllegalArgumentException("Expedition bounds");
  lootBase=11+teams*STRIDE;obstacleBase=lootBase+loot*5;anchorBase=obstacleBase+obstacles*8;chillBase=anchorBase+anchors*4;
  if(chillBase+chills*4!=d.length)throw new IllegalArgumentException("Expedition tail");
 }
 public int seat(int t){return 11+t*STRIDE;}
 public int count(int t){return raw[seat(t)];}
 public int bag(int t,int k){return raw[seat(t)+11+k];}
 public int cooldown(int t,int k){return Math.max(0,raw[seat(t)+19+k]-time);}
 public String ranking(){
  Integer[] order=new Integer[teams];for(int i=0;i<teams;i++)order[i]=i;
  java.util.Arrays.sort(order,(a,b)->Integer.compare(raw[seat(b)+1],raw[seat(a)+1]));
  StringBuilder text=new StringBuilder(com.xenris.liquidwarsos.BattleText.t("远征结束 · 按真实粒子数排名\n\n","Expedition ended · Ranked by actual particle count\n\n"));
  int rank=1,previous=-1;
  for(int i=0;i<teams;i++){
   int t=order[i],n=raw[seat(t)+1];if(n!=previous)rank=i+1;previous=n;
   text.append(rank).append(". ").append(t==StaticBits.team?com.xenris.liquidwarsos.BattleText.t("你","You"):com.xenris.liquidwarsos.BattleText.t("席位 ","Seat ")+(t+1)).append(raw[seat(t)+10]!=0?com.xenris.liquidwarsos.BattleText.t(" · 电脑"," · AI"):com.xenris.liquidwarsos.BattleText.t(" · 真人"," · Human")).append("　").append(n).append(com.xenris.liquidwarsos.BattleText.t("颗\n"," units\n"));
  }
  return text.toString();
 }
}
