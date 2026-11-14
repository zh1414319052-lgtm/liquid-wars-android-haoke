// SPDX-License-Identifier: GPL-3.0-or-later
package com.xenris.liquidwarsos;
public final class OperationsSnapshot {
 public final int[] raw;public final int[][] groupOffset=new int[6][3];public final int[] teamOffset=new int[6];
 public OperationsSnapshot(int[] data){if(data==null||data.length<13||data[0]!=0x4c574f50||data[5]<0||data[5]>5)throw new IllegalArgumentException("Operations snapshot");raw=data;int at=13+data[5]*8;
  for(int t=0;t<6;t++){teamOffset[t]=at;at+=4;for(int g=0;g<3;g++){if(at+8>data.length)throw new IllegalArgumentException("Group bounds");groupOffset[t][g]=at;int n=data[at+7];if(n<0||n>24)throw new IllegalArgumentException("Route bounds");at+=8+2*n;}}
  if(at!=data.length)throw new IllegalArgumentException("Snapshot tail");
 }
 public int generation(int t){return raw[teamOffset[t]+3];}
 public int epoch(){return raw[3];}public int mode(){return raw[1];}public int sites(){return raw[5];}public int site(int i){return 13+i*8;}
 public int group(int team,int group){return groupOffset[team][group];}
 public boolean split(int team){return raw[teamOffset[team]]!=0;}
 public int feedback(int team){return raw[teamOffset[team]+1];}
}
