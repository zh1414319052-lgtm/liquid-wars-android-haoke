// SPDX-License-Identifier: GPL-3.0-or-later
package com.xenris.liquidwarsos;

/** First held pad owns the single follow point. The other pad can take over on release. */
final class DualStickInput {
    private final int[] pointers={-1,-1};
    private final float[] x={0,0},y={0,0};
    private int owner=-1;
    boolean press(int side,int pointer){
        if(side<0||side>1||pointer<0||pointers[side]>=0||sideOf(pointer)>=0)return false;
        pointers[side]=pointer;if(owner<0)owner=side;return true;
    }
    int sideOf(int pointer){for(int i=0;i<2;i++)if(pointers[i]==pointer&&pointer>=0)return i;return -1;}
    int pointer(int side){return pointers[side];}
    int owner(){return owner;}
    boolean held(){return owner>=0;}
    void axis(int side,float dx,float dy){if(!Float.isFinite(dx)||!Float.isFinite(dy))dx=dy=0;float n=(float)Math.hypot(dx,dy);x[side]=n>1?dx/n:dx;y[side]=n>1?dy/n:dy;}
    float x(int side){return x[side];}float y(int side){return y[side];}
    float x(){return owner<0?0:x[owner];}float y(){return owner<0?0:y[owner];}
    void lift(int pointer){int side=sideOf(pointer);if(side<0)return;pointers[side]=-1;x[side]=y[side]=0;if(owner==side)owner=pointers[1-side]>=0?1-side:-1;}
    void clear(){pointers[0]=pointers[1]=-1;x[0]=x[1]=y[0]=y[1]=0;owner=-1;}
}
