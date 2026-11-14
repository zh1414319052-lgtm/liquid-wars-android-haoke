// SPDX-License-Identifier: GPL-3.0-or-later
package com.xenris.liquidwarsos.cloud;
/** Six-slot trailer, preserving the original fixed HUD/event/sample header offsets. */
public final class SnapshotItems {
 private SnapshotItems(){}public static final int MAGIC=0x4c573136,TRAILER=22;
 public static int slots(int[] s){int at=s==null?-1:s.length-TRAILER;return at>=209&&s[at]==MAGIC&&s[at+1]==6?6:2;}
 public static int offset(int[] s,int slot){return slots(s)==6?s.length-TRAILER+2+slot*3:19+slot*3;}
 public static int count(int[] s){if(s==null||s.length<25)return 0;int n=0;for(int i=0;i<slots(s);i++)if(s[offset(s,i)]>=0)n++;return n;}
 private static int health(int[] s,int slot){int at=s==null?-1:s.length-TRAILER-62;return at>=209&&slot>=0&&slot<6&&s[at]==0x4c574850&&s[at+1]==6?at+2+slot*10:-1;}
 public static int hp(int[] s,int slot){int at=health(s,slot);return at<0?1200:s[at];}
 public static int maxHp(int[] s,int slot){int at=health(s,slot);return at<0?1200:s[at+1];}
 public static int leader(int[] s,int slot){int at=health(s,slot);return at<0?-1:s[at+2];}
 public static int contribution(int[] s,int slot,int team){int at=health(s,slot);return at<0||team<0||team>=6?0:s[at+4+team];}
 private static int balance(int[] s){int at=s==null?-1:s.length-TRAILER-62-17;return at>=209&&s[at]==0x4c573137?at:-1;}
 public static boolean rewindSpawned(int[] s){int at=balance(s);return at>=0&&s[at+1]!=0;}
 public static int group(int[] s,int team){int g=s[147+team*10+8],at=balance(s);return at>=0&&s[at+4]>s[0]&&(g==s[at+2]||g==s[at+3])?s[at+2]:g;}
 public static boolean devouring(int[] s,int slot){int at=health(s,slot);return at>=0&&s[at+3]>=0;}
}
