// SPDX-License-Identifier: GPL-3.0-or-later
package com.xenris.liquidwarsos;
/** The exact same aspect-preserving rectangle drives drawing and pointer conversion. */
public final class MiniMapLayout {
 public final float left,top,width,height;private final int worldW,worldH;
 public MiniMapLayout(float sw,float sh,float dp,int ww,int wh,boolean teleport){
  worldW=ww;worldH=wh;float w=teleport?Math.min(420*dp,sw*.62f):154*dp;
  float h=w*wh/ww,max=teleport?sh*.40f:94*dp;if(h>max){h=max;w=h*ww/wh;}
  width=w;height=h;left=sw-14*dp-w;top=Math.max(80*dp,sh-160*dp-h);
 }
 public boolean contains(float x,float y){return x>=left&&x<left+width&&y>=top&&y<top+height;}
 public int x(float px){return Math.max(0,Math.min(worldW-1,(int)((px-left)/width*worldW)));}
 public int y(float py){return Math.max(0,Math.min(worldH-1,worldH-1-(int)((py-top)/height*worldH)));}
}
