// SPDX-License-Identifier: GPL-3.0-or-later
package com.xenris.liquidwarsos;
import java.util.*;
public final class AllianceRules {
    public static int[] solo(){return new int[]{0,1,2,3,4,5};}
    public static boolean valid(int[] groups){if(groups==null||groups.length!=6)return false;if(Arrays.equals(groups,solo()))return true;int[] counts=new int[3];for(int g:groups){if(g<0||g>2)return false;counts[g]++;}return counts[0]==2&&counts[1]==2&&counts[2]==2;}
    public static boolean allied(int a,int b){return NativeInterface.areAllied(a,b);}
    public static int score(int team){return NativeInterface.allianceScore(team);}
    public static java.util.List<int[]> pairings(){java.util.List<int[]> out=new ArrayList<>();int[] a={-1,-1,-1,-1,-1,-1};pair(a,0,out);return out;}
    private static void pair(int[] a,int group,List<int[]> out){int first=0;while(first<6&&a[first]>=0)first++;if(first==6){out.add(a.clone());return;}a[first]=group;for(int p=first+1;p<6;p++)if(a[p]<0){a[p]=group;pair(a,group+1,out);a[p]=-1;}a[first]=-1;}
    public static String label(int[] a){if(Arrays.equals(a,solo()))return "FFA";StringBuilder b=new StringBuilder();for(int g=0;g<3;g++){if(g>0)b.append(" / ");boolean second=false;for(int p=0;p<6;p++)if(a[p]==g){if(second)b.append('+');b.append(p+1);second=true;}}return b.toString();}
}
