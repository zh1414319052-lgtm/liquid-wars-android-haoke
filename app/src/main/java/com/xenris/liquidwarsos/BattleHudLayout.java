// SPDX-License-Identifier: GPL-3.0-or-later
package com.xenris.liquidwarsos;
import android.graphics.RectF;
/** Shared logical geometry for the 2D large-map and 3D expedition HUDs. */
final class BattleHudLayout {
 static float unit(float width,float height){return Math.min(width/960f,height/440f);}
 static float utilityX(float width,int index){return width-368+(index==0?0:index==1?56:110);}
 static RectF mini(float width,int worldW,int worldH){return new RectF(width-194,8,width-16,8+178f*worldH/worldW);}
 static RectF expanded(float width,float height,int worldW,int worldH){float mh=height-150,mw=mh*worldW/worldH;if(mw>width-110){mw=width-110;mh=mw*worldH/worldW;}return new RectF((width-mw)/2,58+(height-150-mh)/2,(width+mw)/2,58+(height-150+mh)/2);}
 private BattleHudLayout(){}
}
