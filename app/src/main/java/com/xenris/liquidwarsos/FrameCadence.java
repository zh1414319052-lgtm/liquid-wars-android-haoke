// SPDX-License-Identifier: GPL-3.0-or-later
package com.xenris.liquidwarsos;

/** Pure timing policy; never advances the simulation or reads its random streams. */
final class FrameCadence {
    private long next;
    private long period=16666667;
    void reset(float displayHz,float targetHz){
        double display=Math.max(1,displayHz),target=Math.max(1,Math.min(display,targetHz));
        // Only whole display refreshes: no 90-on-120 alternating hold times.
        period=Math.round(1e9/display)*Math.max(1,(int)Math.ceil(display/target-.01));next=0;
    }
    boolean due(long frameTime,long now){
        if(now-frameTime>period*2)return false; // Skip obsolete UI callbacks, never catch up in a burst.
        if(next!=0&&frameTime+500000<next)return false;
        next=frameTime+period;return true;
    }
}
