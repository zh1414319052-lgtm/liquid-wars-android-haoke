// SPDX-License-Identifier: GPL-3.0-or-later
package com.xenris.liquidwarsos;

/** Temporary local match settings, restored after native/GL teardown. No preferences written. */
public final class GardenSession implements AutoCloseable {
    public static final String EXTRA="garden3d";
    public static boolean active;
    public static int expeditionSeats;
    private final int team=StaticBits.team,map=StaticBits.map,seed=StaticBits.seed,
            dots=StaticBits.dotsPerTeam,mode=StaticBits.operationsMode,time=StaticBits.timeLimit,
            difficulty=StaticBits.aiDifficulty;
    private final int[] alliances=StaticBits.alliances.clone();
    private boolean closed;
    public GardenSession(){this(6);}
    public GardenSession(int seats,int player,int matchSeed){this(seats);if(player<0||player>=seats)throw new IllegalArgumentException("Seat");StaticBits.team=player;StaticBits.seed=matchSeed;}
    public GardenSession(int seats){
        expeditionSeats=seats==0?6:Math.max(2,Math.min(10,seats));
        active=true;StaticBits.team=0;StaticBits.map=1001;StaticBits.dotsPerTeam=1000;
        StaticBits.operationsMode=2;StaticBits.timeLimit=expeditionSeats*120;StaticBits.aiDifficulty=AiDifficulty.CLASSIC;
        StaticBits.alliances=AllianceRules.solo();StaticBits.newSeed();
    }
    @Override public void close(){
        if(closed)return;closed=true;active=false;expeditionSeats=0;
        StaticBits.team=team;StaticBits.map=map;StaticBits.seed=seed;StaticBits.dotsPerTeam=dots;
        StaticBits.operationsMode=mode;StaticBits.timeLimit=time;StaticBits.aiDifficulty=difficulty;
        StaticBits.alliances=alliances;
    }
}
