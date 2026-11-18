// SPDX-License-Identifier: GPL-3.0-or-later
package com.xenris.liquidwarsos;
/** Moves a world-space command point only. Never writes particle positions. */
public final class JoystickCursor {
 public static final float DEAD_ZONE=.12f,SPEED=150f;
 private float x,y,maxX,maxY,ax,ay;private boolean ready,held;
 public void bounds(int w,int h){maxX=Math.max(0,w-1);maxY=Math.max(0,h-1);x=clamp(x,maxX);y=clamp(y,maxY);}
 private static float clamp(float v,float max){return Math.max(0,Math.min(max,v));}
 public void target(float px,float py){if(!Float.isFinite(px)||!Float.isFinite(py))return;x=clamp(px,maxX);y=clamp(py,maxY);ready=true;}
 public void axis(float vx,float vy){ax=vx;ay=vy;held=true;}
 public void release(){ax=ay=0;held=false;}
 public void clear(){release();ready=false;}
 public boolean ready(){return ready;}public float x(){return x;}public float y(){return y;}
 public boolean advance(float dt){
  return advance(dt,1f);
 }
 public boolean advance(float dt,float sensitivity){
  float n=(float)Math.hypot(ax,ay);if(!ready||!held||!Float.isFinite(dt)||!Float.isFinite(n)||dt<=0||n<=DEAD_ZONE)return false;
  float gain=Float.isFinite(sensitivity)?Math.max(.5f,Math.min(2f,sensitivity)):1f;
  float strength=(Math.min(1,n)-DEAD_ZONE)/(1-DEAD_ZONE),travel=SPEED*gain*Math.min(.05f,dt)*strength*strength;
  float oldX=x,oldY=y;x=clamp(x+ax/n*travel,maxX);y=clamp(y+ay/n*travel,maxY);return oldX!=x||oldY!=y;
 }
}
