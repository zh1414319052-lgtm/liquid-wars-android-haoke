//    This file is part of Liquid Wars.
//
//    Copyright (C) 2013 Henry Shepperd (hshepperd@gmail.com)
//
//    Liquid Wars is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, either version 3 of the License, or
//    (at your option) any later version.
//
//    Liquid Wars is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with Liquid Wars.  If not, see <http://www.gnu.org/licenses/>.

package com.xenris.liquidwarsos;

import android.view.MotionEvent;

import java.util.Arrays;

public final class TouchTargets {
    private TouchTargets() {}

    public static void update(MotionEvent event, int viewWidth, int viewHeight, short[] xs, short[] ys) {
        update(event,viewWidth,viewHeight,xs,ys,null);
    }
    public static void update(MotionEvent event,int viewWidth,int viewHeight,short[] xs,short[] ys,WorldCamera camera){
        int action = event.getActionMasked();
        if (action == MotionEvent.ACTION_CANCEL) {
            // Keep the last primary destination, just as when the last finger is lifted.
            clearFrom(xs, ys, 1);
            if(camera!=null)camera.endTouch();
            return;
        }
        if (viewWidth <= 0 || viewHeight <= 0) {
            return;
        }

        if(camera!=null&&action==MotionEvent.ACTION_DOWN)camera.endTouch();
        if(camera!=null&&(action==MotionEvent.ACTION_DOWN||action==MotionEvent.ACTION_POINTER_DOWN))camera.beginTouch(event.getPointerId(event.getActionIndex()));
        int liftedIndex = action == MotionEvent.ACTION_POINTER_UP ? event.getActionIndex() : -1;
        int target = 0;
        for (int pointer = 0; pointer < event.getPointerCount() && target < xs.length; pointer++) {
            if (pointer == liftedIndex) {
                continue;
            }
            xs[target] = camera==null?GameViewport.mapX(event.getX(pointer),viewWidth):camera.touchX(event.getX(pointer),viewWidth,event.getPointerId(pointer));
            ys[target] = camera==null?(short)(MyRenderer.HEIGHT-1-scale(event.getY(pointer),viewHeight,MyRenderer.HEIGHT)):camera.touchY(event.getY(pointer),viewHeight,event.getPointerId(pointer));
            if(camera!=null)camera.trackTouch(target,event.getPointerId(pointer),event.getX(pointer)/viewWidth,event.getY(pointer)/viewHeight);
            target++;
        }
        clearFrom(xs, ys, target);
        if(camera!=null)camera.finishTouchTargets(target);
        if(camera!=null&&action==MotionEvent.ACTION_POINTER_UP)camera.endTouch(event.getPointerId(event.getActionIndex()));
        if(camera!=null&&action==MotionEvent.ACTION_UP)camera.endTouch();
    }

    private static void clearFrom(short[] xs, short[] ys, int fromIndex) {
        Arrays.fill(xs, fromIndex, xs.length, (short) -1);
        Arrays.fill(ys, fromIndex, ys.length, (short) -1);
    }

    private static short scale(float value, int viewSize, int mapSize) {
        int coordinate = (int) (value * mapSize / viewSize);
        return (short) Math.max(0, Math.min(mapSize - 1, coordinate));
    }
}
