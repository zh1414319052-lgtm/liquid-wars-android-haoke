// SPDX-License-Identifier: GPL-3.0-or-later
package com.xenris.liquidwarsos;

import android.content.SharedPreferences;

/** Normalized per-side centres, shared across modes and display sizes. No game state. */
final class JoystickPlacement {
    static final float RADIUS_DP=116f; // User requested twice the 116dp preview diameter: 232dp.
    static final float MIN_SCALE=.5f,MAX_SCALE=1.25f;
    static final float MIN_SENSITIVITY=.5f,MAX_SENSITIVITY=2f;
    private float scale=1f;
    private float sensitivity=1f;
    private final float[] nx={Float.NaN,Float.NaN},ny={Float.NaN,Float.NaN};
    void load(SharedPreferences p){size(p.getFloat("stick-size",1f));sensitivity(p.getFloat("stick-sensitivity",1f));for(int i=0;i<2;i++){nx[i]=p.getFloat("stick-"+i+"-x",Float.NaN);ny[i]=p.getFloat("stick-"+i+"-y",Float.NaN);}}
    void save(SharedPreferences p){SharedPreferences.Editor e=p.edit().putFloat("stick-size",scale).putFloat("stick-sensitivity",sensitivity);for(int i=0;i<2;i++){e.putFloat("stick-"+i+"-x",nx[i]);e.putFloat("stick-"+i+"-y",ny[i]);}e.apply();}
    void defaults(){scale=sensitivity=1f;for(int i=0;i<2;i++)nx[i]=ny[i]=Float.NaN;}
    float size(){return scale;}
    void size(float value){scale=Float.isFinite(value)?clamp(value,MIN_SCALE,MAX_SCALE):1f;}
    float sensitivity(){return sensitivity;}
    void sensitivity(float value){sensitivity=Float.isFinite(value)?clamp(value,MIN_SENSITIVITY,MAX_SENSITIVITY):1f;}
    static float clamp(float v,float lo,float hi){return Math.max(lo,Math.min(Math.max(lo,hi),v));}
    float x(int side,float w,float r){float lo=side==0?r+8:w/2+r+6,hi=side==0?w/2-r-6:w-r-8;return clamp(Float.isFinite(nx[side])?nx[side]*w:(side==0?r+18:w-r-18),lo,hi);}
    float y(int side,float h,float r,float minY,float maxY){return clamp(Float.isFinite(ny[side])?ny[side]*h:maxY,minY,maxY);}
    void move(int side,float x,float y,float w,float h){nx[side]=x/Math.max(1,w);ny[side]=y/Math.max(1,h);}
}
