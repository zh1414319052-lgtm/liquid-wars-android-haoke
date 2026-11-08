/*
 * Copyright 2014 Google Inc. All rights reserved.
 * Copyright 2026 Liquid Wars OS contributors.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy at http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software distributed
 * under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 *
 * Adapted from google/grafika ScheduledSwapActivity.doFrame, fixed commit
 * b1df331e89cffeab621f02b102d4c2c25eb6088a. Modifications: GLSurfaceView owns EGL;
 * coalesced render requests, lifecycle cancellation and stale callback rejection.
 * Original source, license and modification notice: third_party/grafika/.
 */
package com.xenris.liquidwarsos;
import android.view.Choreographer;

/** Runs only on the main thread, forwarding vsync ticks to the existing GL thread. */
final class VsyncPacer implements Choreographer.FrameCallback {
    interface Tick {void draw(long frameTime);}
    private final Choreographer choreographer=Choreographer.getInstance();
    private final FrameCadence cadence=new FrameCadence();
    private final Tick tick;
    private boolean active;
    VsyncPacer(Tick tick){this.tick=tick;}
    void rate(float displayHz,float targetHz){cadence.reset(displayHz,targetHz);}
    void start(){if(!active){active=true;choreographer.postFrameCallback(this);}}
    void stop(){active=false;choreographer.removeFrameCallback(this);}
    boolean active(){return active;}
    @Override public void doFrame(long frameTimeNanos){
        if(!active)return;
        choreographer.postFrameCallback(this);
        if(cadence.due(frameTimeNanos,System.nanoTime()))tick.draw(frameTimeNanos);
    }
}
