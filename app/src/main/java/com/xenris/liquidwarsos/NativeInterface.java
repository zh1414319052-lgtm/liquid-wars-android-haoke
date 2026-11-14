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

import android.content.res.AssetManager;

public class NativeInterface {
    static {
        System.loadLibrary("nativeinterface");
    }

    public static native void onSurfaceCreated();
    public static native int[] sceneInfo();
    /** Read-only deterministic rule diagnostic; does not include camera or consume RNG. */
    public static native long stateHash();
    public static native void setCamera(float left,float bottom,float width,float height,boolean spectator);
    public static native void onDrawFrame();
    public static native void onSurfaceChanged(int width, int height);
    public static native void init(AssetManager assetManager);
    public static native void uninit();
    public static native void stepDots();
    public static native void createGame(int team, int map, int seed, int dotsPerTeam);
    public static native void destroyGame();
    /** Initialization only: actual native spawn cursors, interleaved x/y. */
    public static native short[] initialPositions();
    public static native void setPlayerPosition(int team, short[] xs, short[] ys);
    public static native int getNearestDot(int p, short px, short py);
    /** Read-only planner output: six teams x (five x, five y), only used by solo/host. */
    public static native boolean updateAiTargets(short[] targets,int localMask,int difficulty);
    public static native boolean updateStrategicTargets(short[] targets,int[] strategy);
    public static native int[] aiSnapshot();
    /** Game-thread-only immutable copies for external model observers. */
    public static native int[] modelSnapshot();
    public static native byte[] modelTerrain();
    public static native void configureAlliances(int[] groups);
    public static native int teamScore(int p);
    public static native int totalParticles();
    public static native int playTimeMillis();
    public static native boolean areAllied(int a,int b);
    public static native int allianceScore(int team);
    public static native int terrainRevision();
    /** Immutable authoritative pickups, effects, event and score copy. */
    public static native int[] itemSnapshot();
    public static native void configureOperations(int mode);
    public static native int[] operationsSnapshot(int viewer,boolean spectator);
    public static native int operationWinner();
    public static native int operationScore(int team);
    /** Display-only allied scouting; never writes rule state or RNG. */
    public static native int[] minimapSnapshot(boolean spectator);
    public static native void setTimeSidebar(float t);
}
