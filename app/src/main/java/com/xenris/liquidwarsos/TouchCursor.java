// SPDX-License-Identifier: GPL-3.0-or-later
package com.xenris.liquidwarsos;
/** UI-only touch target. Actual army progress and simulation time bound joystick travel. */
public final class TouchCursor {
 public static final float SPEED=90f,LEAD=40f,DEAD_ZONE=.10f;
 private float maxX=799,maxY=479,x,y,massX,massY,axisX,axisY,scaleX=1,simCredit;
 private int observedMillis=-1;private boolean ready,active;
 public void bounds(int w,int h){maxX=Math.max(0,w-1);maxY=Math.max(0,h-1);x=clamp(x,maxX);y=clamp(y,maxY);}
 public boolean ready(){return ready;}public boolean active(){return active;}
 public float x(){return x;}public float y(){return y;}
 public short mapX(){return (short)Math.round(x);}public short mapY(){return (short)Math.round(y);}
 private static float clamp(float value,float max){return Math.max(0,Math.min(max,value));}
 public void clear(){ready=active=false;axisX=axisY=simCredit=0;observedMillis=-1;}
 public void target(float px,float py){if(!Float.isFinite(px)||!Float.isFinite(py))return;x=clamp(px,maxX);y=clamp(py,maxY);ready=true;active=false;axisX=axisY=simCredit=0;observedMillis=-1;}
 public void grab(float mx,float my){if(!ready)target(mx,my);massX=mx;massY=my;active=true;axisX=axisY=simCredit=0;observedMillis=-1;}
 public void mapScale(float ratio){if(Float.isFinite(ratio)&&ratio>0)scaleX=ratio;}
 public void axis(float dx,float dy){axisX=dx;axisY=dy;}
 /** Read-only centroid from the HUD snapshot. Does not pull a previously chosen point back. */
 public void followMass(float mx,float my){if(Float.isFinite(mx)&&Float.isFinite(my)){massX=mx;massY=my;}}
 public void observe(float mx,float my,int simulationMillis,boolean frozen){
  followMass(mx,my);
  if(active&&!frozen&&observedMillis>=0&&simulationMillis>observedMillis)
   simCredit=Math.min(.05f,simCredit+Math.min(.05f,(simulationMillis-observedMillis)/1000f));
  else if(!active||frozen||simulationMillis<observedMillis)simCredit=0;
  observedMillis=simulationMillis;
 }
 public void release(){active=false;axisX=axisY=simCredit=0;observedMillis=-1;}
 // Clip only the requested component, so reaching the lead limit never adds sideways motion.
 private static float limited(float at,float mass,float lead,float step){
  return step>0?Math.min(step,Math.max(0,mass+lead-at)):Math.max(step,-Math.max(0,at-(mass-lead)));
 }
 public boolean advance(float seconds){
  if(!active||!Float.isFinite(seconds)||seconds<=0)return false;
  float n=(float)Math.hypot(axisX,axisY);
  if(!Float.isFinite(n)||n<=DEAD_ZONE){simCredit=0;return false;}
  float dt=Math.min(simCredit,Math.min(.05f,seconds));simCredit-=dt;if(dt<=0)return false;
  float strength=(Math.min(1,n)-DEAD_ZONE)/(1-DEAD_ZONE);strength*=.35f+.65f*strength;
  float beforeX=x,beforeY=y,travel=strength*SPEED*dt;
  x=clamp(x+limited(x,massX,LEAD*scaleX,axisX/n*travel*scaleX),maxX);
  y=clamp(y+limited(y,massY,LEAD,axisY/n*travel),maxY);
  return x!=beforeX||y!=beforeY;
 }
}
