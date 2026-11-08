// SPDX-License-Identifier: GPL-3.0-or-later
package com.xenris.liquidwarsos;

/** UI-only camera. Player intent supplies bounded look-ahead; the real army supplies travel. */
public final class WorldCamera {
 public int width=800,height=480,id=-1,flip;
 public float left,bottom,viewWidth=800,viewHeight=480;
 public boolean overview=true,following=true;
 private final boolean[] touchProjection=new boolean[32];
 private final int[] touchOrder={-1,-1,-1,-1,-1};
 private final float[] touchNX=new float[32],touchNY=new float[32];
 private float aspect=2.2f,goalX,goalY,armyX,armyY,velocityX,velocityY;
 private boolean hasTarget;private long lastFrame;
 public boolean touching(){for(boolean held:touchProjection)if(held)return true;return false;}
 public boolean large(){return width>800||height>480;}
 private static float clamp(float v,float a,float b){return Math.max(a,Math.min(b,v));}
 private static float soft(float v,float zone){return Math.copySign(Math.max(0,Math.abs(v)-zone),v);}
 // A smooth shoulder keeps thumb tremor in the central36% of the view from panning.
 private static float lead(float v){float t=clamp((Math.abs(v)-.18f)/.26f,0,1);return Math.copySign(.14f*t*t*(3-2*t),v);}
 public void viewport(float ratio){if(ratio>0&&Math.abs(ratio-aspect)>.0001f){float x=left+viewWidth/2,y=bottom+viewHeight/2;aspect=ratio;focus(x,y);}}
 public void world(int w,int h,int map,int transform,boolean spectator,float x,float y){
  endTouch();width=w;height=h;id=map;flip=transform;overview=!large()||spectator;following=true;
  armyX=x;armyY=y;hasTarget=false;focus(x,y);
 }
 public void focus(float x,float y){goalX=x;goalY=y;resetSmoothing();place(x,y);}
 private void place(float x,float y){
  if(!large()){viewWidth=width;viewHeight=height;left=bottom=0;return;}
  viewWidth=overview?Math.max(width,height*aspect):Math.min(800,width);
  viewHeight=overview?Math.max(height,width/aspect):Math.min(height,viewWidth/aspect);
  left=overview?(width-viewWidth)/2:clamp(x-viewWidth/2,0,width-viewWidth);
  bottom=overview?(height-viewHeight)/2:clamp(y-viewHeight/2,0,height-viewHeight);
 }
 /** Read-only centroid from the existing shared snapshot, including while held or inspecting. */
 public void follow(float x,float y){if(Float.isFinite(x)&&Float.isFinite(y)){armyX=clamp(x,0,width-1);armyY=clamp(y,0,height-1);}}
 public void frame(long nanos){
  if(lastFrame==0){lastFrame=nanos;return;}
  float dt=(nanos-lastFrame)/1e9f;lastFrame=nanos;
  if(!large()||overview||!following||dt<=0){velocityX=velocityY=0;return;}
  // Lost frames never turn into catch-up jumps. Restart gently after lifecycle stalls.
  if(dt>.20f){velocityX=velocityY=0;return;}dt=Math.min(dt,.05f);
  float x=left+viewWidth/2,y=bottom+viewHeight/2;
  float lx=0,ly=0;
  if(touching()&&touchOrder[0]>=0){int p=touchOrder[0];lx=lead(touchNX[p]-.5f);ly=lead(.5f-touchNY[p]);}
  else if(hasTarget){lx=lead((goalX-armyX)/viewWidth);ly=lead((goalY-armyY)/viewHeight);}
  float wantedX=clamp(armyX+lx*viewWidth,viewWidth/2,width-viewWidth/2);
  float wantedY=clamp(armyY+ly*viewHeight,viewHeight/2,height-viewHeight/2);
  float ex=soft(wantedX-x,.025f*viewWidth),ey=soft(wantedY-y,.035f*viewHeight);
  float wantVX=ex/.32f,wantVY=ey/.32f;
  // Faster following is allowed only when real troops approach the view edge,
  // never because the finger has been held longer or the projected target is farther away.
  float reachableX=clamp(armyX,viewWidth/2,width-viewWidth/2),reachableY=clamp(armyY,viewHeight/2,height-viewHeight/2);
  float armyOffset=Math.max(Math.abs(reachableX-x)/viewWidth,Math.abs(reachableY-y)/viewHeight);
  float speed=viewWidth*(.12f+.10f*clamp((armyOffset-.22f)/.16f,0,1));
  float length=(float)Math.hypot(wantVX,wantVY);if(length>speed){wantVX*=speed/length;wantVY*=speed/length;}
  float dvx=wantVX-velocityX,dvy=wantVY-velocityY,dv=(float)Math.hypot(dvx,dvy);
  float accel=viewWidth*.60f*dt;if(dv>accel){dvx*=accel/dv;dvy*=accel/dv;}
  float nx=velocityX+dvx,ny=velocityY+dvy;
  float mx=(velocityX+nx)*.5f*dt,my=(velocityY+ny)*.5f*dt;
  // Do not overshoot the soft stable region or retain velocity against map boundaries.
  float rx=wantedX-x,ry=wantedY-y;
  if(mx*rx>0&&Math.abs(mx)>Math.abs(rx)){mx=rx;nx=0;}if(my*ry>0&&Math.abs(my)>Math.abs(ry)){my=ry;ny=0;}
  place(x+mx,y+my);velocityX=Math.abs(left+viewWidth/2-x-mx)>.01f?0:nx;velocityY=Math.abs(bottom+viewHeight/2-y-my)>.01f?0:ny;
 }
 public void touchTarget(float x,float y){
  if(!Float.isFinite(x)||!Float.isFinite(y))return;
  goalX=clamp(x,0,width-1);goalY=clamp(y,0,height-1);hasTarget=true;
  if(large()&&!overview)following=true;
 }
 public void followTarget(float x,float y){if(Float.isFinite(x)&&Float.isFinite(y)){goalX=clamp(x,0,width-1);goalY=clamp(y,0,height-1);hasTarget=true;}}
 /** Explicit discontinuities (teleport/rewind), not ordinary following. */
 public void relocate(float x,float y){endTouch();armyX=x;armyY=y;hasTarget=false;following=true;focus(x,y);}
 public void clearTarget(){endTouch();hasTarget=false;resetSmoothing();}
 public void beginTouch(){beginTouch(0);}
 public void beginTouch(int pointer){if(pointer>=0&&pointer<32)touchProjection[pointer]=true;}
 public void trackTouch(int slot,int pointer,float nx,float ny){if(slot<0||slot>=5||pointer<0||pointer>=32)return;touchOrder[slot]=pointer;touchNX[pointer]=clamp(nx,0,1);touchNY[pointer]=clamp(ny,0,1);}
 public void finishTouchTargets(int count){for(int k=count;k<5;k++)touchOrder[k]=-1;}
 public boolean fillTouchTargets(short[] xs,short[] ys){
  if(!large()||overview||!following||!touching()||touchOrder[0]<0)return false;boolean changed=false;
  for(int k=0;k<5;k++){int p=touchOrder[k];short x=p<0?(short)-1:mapX(touchNX[p],1),y=p<0?(short)-1:mapY(touchNY[p],1);changed|=xs[k]!=x||ys[k]!=y;xs[k]=x;ys[k]=y;}return changed;
 }
 public void endTouch(){java.util.Arrays.fill(touchProjection,false);java.util.Arrays.fill(touchOrder,-1);}
 public void endTouch(int pointer){if(pointer>=0&&pointer<32)touchProjection[pointer]=false;if(!touching())java.util.Arrays.fill(touchOrder,-1);}
 public short touchX(float px,float pixels){return mapX(px,pixels);}
 public short touchY(float py,float pixels){return mapY(py,pixels);}
 public short touchX(float px,float pixels,int pointer){return mapX(px,pixels);}
 public short touchY(float py,float pixels,int pointer){return mapY(py,pixels);}
 public void resetSmoothing(){lastFrame=0;velocityX=velocityY=0;}
 public void pan(float x,float y){endTouch();overview=false;following=false;focus(x,y);}
 public void toggle(float x,float y){endTouch();if(overview||!following){overview=false;following=true;}else overview=true;hasTarget=false;focus(x,y);}
 public short mapX(float px,float pixels){return (short)clamp((int)(left+px*viewWidth/Math.max(1,pixels)),0,width-1);}
 public short mapY(float py,float pixels){return (short)clamp((int)Math.ceil(bottom+viewHeight-1-py*viewHeight/Math.max(1,pixels)),0,height-1);}
 public float screenX(float x,float pixels){return (x+.5f-left)/viewWidth*pixels;}
 public float screenY(float y,float pixels){return (bottom+viewHeight-.5f-y)/viewHeight*pixels;}
}
