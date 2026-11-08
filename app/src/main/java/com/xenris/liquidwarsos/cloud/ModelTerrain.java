// SPDX-License-Identifier: GPL-3.0-or-later
package com.xenris.liquidwarsos.cloud;
/** Immutable collision data copied on the simulation thread, read by network workers. */
public final class ModelTerrain {
    private final byte[] walls;
    public final int width,height;
    public final String description;
    public final ModelNavigation navigation;
    public ModelTerrain(byte[] walls){
        width=widthForSize(walls==null?0:walls.length);height=walls.length/width;
        if(walls==null||walls.length!=width*height)throw new IllegalArgumentException("Terrain dimensions");this.walls=walls.clone();
        navigation=new ModelNavigation(this.walls,width,height);description=navigation.description;
    }
    public static int widthForSize(int n){if(n==3200*960)return 3200;if(n==1600*960)return 1600;if(n==800*480)return 800;throw new IllegalArgumentException("Terrain dimensions");}
    public boolean allows(ModelAction action){short[] p=action.points();for(int i=0;i<5;i++)if(p[i]>=0&&(p[i]>=width||p[i+5]>=height||walls[p[i+5]*width+p[i]]!=0))return false;return true;}
}
