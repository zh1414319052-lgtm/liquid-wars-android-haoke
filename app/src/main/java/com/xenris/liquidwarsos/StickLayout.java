// SPDX-License-Identifier: GPL-3.0-or-later
package com.xenris.liquidwarsos;
/** One geometry for drawing, hit testing and normalizing both fixed sticks. */
public final class StickLayout {
 public final float radius,travel,knob,leftX,rightX,y,hitRadius;
 public StickLayout(float width,float height,float density){
  float dp=Math.max(.5f,density);
  radius=Math.min(64*dp,Math.min(width*.14f,height*.23f));
  travel=radius*.75f;knob=radius*.375f;hitRadius=radius+8*dp;
  leftX=radius+28*dp;rightX=width-leftX;y=height-radius-24*dp;
 }
 public float x(int side){return side==0?leftX:rightX;}
 public int side(float px,float py){
  if(Math.hypot(px-leftX,py-y)<=hitRadius)return 0;
  return Math.hypot(px-rightX,py-y)<=hitRadius?1:-1;
 }
}
