// SPDX-License-Identifier: GPL-3.0-or-later
package com.xenris.liquidwarsos;
import java.util.*;
/** Pure input codec, bounded world-space polyline reduction; no simulation writes. */
public final class OrderProtocol {
 public static final int BEGIN=1,APPEND=2,COMMIT=3,STOP=4,SPLIT=5,MERGE=6,FOLLOW=7,ALL=3,MAX_POINTS=24;
 private int serial;
 public short[][] packet(int op,int group,int epoch,List<int[]> points,int from){
  short[] x=new short[5],y=new short[5];Arrays.fill(x,(short)-1);Arrays.fill(y,(short)-1);
  x[0]=-5;y[0]=(short)(op*4+group);serial=serial%32767+1;x[1]=(short)serial;y[1]=(short)epoch;
  if(points!=null)for(int i=0;i<3&&from+i<points.size();i++){int[] p=points.get(from+i);x[i+2]=(short)p[0];y[i+2]=(short)p[1];}
  return new short[][]{x,y};
 }
 public List<short[][]> route(int group,int epoch,List<int[]> raw){
  List<int[]> points=simplify(raw);List<short[][]> out=new ArrayList<>();if(points.isEmpty())return out;
  for(int i=0;i<points.size();i+=3)out.add(packet(i==0?BEGIN:APPEND,group,epoch,points,i));out.add(packet(COMMIT,group,epoch,null,0));return out;
 }
 public static List<int[]> simplify(List<int[]> raw){
  List<int[]> out=new ArrayList<>();for(int[] p:raw)if(out.isEmpty()||distance(out.get(out.size()-1),p)>9)out.add(p.clone());
  if(!raw.isEmpty()&&!Arrays.equals(out.get(out.size()-1),raw.get(raw.size()-1)))out.add(raw.get(raw.size()-1).clone());
  while(out.size()>MAX_POINTS){int remove=1;double least=Double.MAX_VALUE;for(int i=1;i<out.size()-1;i++){int[] a=out.get(i-1),b=out.get(i),c=out.get(i+1);double area=Math.abs((long)(b[0]-a[0])*(c[1]-a[1])-(long)(b[1]-a[1])*(c[0]-a[0]));if(area<least){least=area;remove=i;}}out.remove(remove);}return out;
 }
 private static long distance(int[] a,int[] b){long x=a[0]-b[0],y=a[1]-b[1];return x*x+y*y;}
}
