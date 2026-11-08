// SPDX-License-Identifier: GPL-3.0-or-later
package com.xenris.liquidwarsos;

/** Match-worker only. Human targets are never touched; clients do not instantiate this. */
public final class AiController {
    private final int level;
    private final short[] planned=new short[60];
    private final short[] refreshed=new short[60];
    private int tick;
    private int readyMask;
    // Package-private diagnostics read only by instrumentation; no logging/network.
    volatile long planningNanos,planningCalls,maxPlanningNanos;
    private int cloudMask,activeMask;
    private short[] modelPoints;
    public static final short MODEL_IDLE=-2;
    public void setModelInputs(int configured,int active,short[] points){cloudMask=configured&63;activeMask=active&cloudMask;modelPoints=points;}
    public void applyModelInputs(short[][] xs,short[][] ys,int controlledMask){
        for(int p=0;p<6;p++)if((cloudMask&controlledMask&(1<<p))!=0){
            if((activeMask&(1<<p))!=0&&modelPoints!=null&&(modelPoints[p*10]>=0||modelPoints[p*10+4]==-3)){System.arraycopy(modelPoints,p*10,xs[p],0,5);System.arraycopy(modelPoints,p*10+5,ys[p],0,5);if(xs[p][0]<0)xs[p][0]=ys[p][0]=MODEL_IDLE;}
            else{java.util.Arrays.fill(xs[p],(short)-1);java.util.Arrays.fill(ys[p],(short)-1);xs[p][0]=ys[p][0]=MODEL_IDLE;}
        }
    }
    private void chooseLocalItems(short[][] xs,short[][] ys,int controlled){
        int local=controlled&~cloudMask&63;if(local==0)return;int[] s=NativeInterface.itemSnapshot();if(s==null||s.length<209)return;byte[] walls=null;
        for(int t=0;t<6;t++){if((local&(1<<t))==0||s[13+t]<=0)continue;int at=147+t*10,kind=s[at];if(kind!=3&&kind!=7&&kind!=8)continue;int target=-1,best=-1;
            for(int e=0;e<6;e++)if(s[13+e]>0&&s[147+e*10+8]!=s[at+8]){if(kind==7&&(s[25+e*20+6]>0||s[25+e*20+16]>0))continue;if(kind==8&&s[25+e*20+17]>0)continue;if(s[13+e]>best){best=s[13+e];target=e;}}
            if(target<0)continue;int x=target,y=0;
            if(kind==3){if(walls==null)walls=NativeInterface.modelTerrain();if(walls==null)continue;int ww=com.xenris.liquidwarsos.cloud.ModelTerrain.widthForSize(walls.length),hh=walls.length/ww;int gx=xs[t][0]>=0?xs[t][0]:s[147+target*10+2],gy=ys[t][0]>=0?ys[t][0]:s[147+target*10+3],cell=-1;
                for(int radius=12;radius<=60&&cell<0;radius+=8)for(int k=0;k<16;k++){double a=k*Math.PI/8;int xx=gx+(int)(Math.cos(a)*radius),yy=gy+(int)(Math.sin(a)*radius);if(xx>=16&&xx<(ww-16)&&yy>=4&&yy<(hh-4)&&walls[yy*ww+xx]==0){cell=yy*ww+xx;break;}}
                if(cell<0)continue;x=cell%ww;y=cell/ww;
            }
            xs[t][3]=(short)x;ys[t][3]=(short)y;xs[t][4]=-3;ys[t][4]=(short)s[at+1];
        }
    }
    public AiController(int difficulty) { level=AiDifficulty.valid(difficulty); }
    public void update(short[][] xs,short[][] ys,int controlledMask) {
        if((controlledMask&0x3f)==0)return; // A six-human room needs no AI work.
        int local=controlledMask&~cloudMask&63;
        readyMask&=local;
        int due=0,cadence=level==AiDifficulty.HARD?4:8;
        for(int p=0;p<6;p++)if(p%cadence==tick%cadence)due|=local&(1<<p);
        if(level!=AiDifficulty.RELAXED && due!=0){
            // Spread independent teams across worker ticks; do not hold the shared
            // render/state mutex for six route searches in a single frame.
            long started=System.nanoTime();boolean available=NativeInterface.updateAiTargets(refreshed,due,level);
            long elapsed=System.nanoTime()-started;planningNanos+=elapsed;planningCalls++;maxPlanningNanos=Math.max(maxPlanningNanos,elapsed);
            if(available){for(int p=0;p<6;p++)if((due&(1<<p))!=0)System.arraycopy(refreshed,p*10,planned,p*10,10);readyMask|=due;}
            else readyMask&=~due;
        }
        for(int team=0;team<6;team++) {
            if((controlledMask&~cloudMask&(1<<team))==0)continue;
            // Retain the old call order/cadence and RNG path even when a controller
            // holds its cursor or uses a read-only strategic target instead.
            int nearest=NativeInterface.getNearestDot(team,xs[team][0],ys[team][0]);
            if(level==AiDifficulty.RELAXED && (tick<64 || tick%12!=0))continue;
            if(level!=AiDifficulty.RELAXED && (readyMask&(1<<team))!=0 && planned[team*10]>=0 && planned[team*10+5]>=0) {
                System.arraycopy(planned,team*10,xs[team],0,5);
                System.arraycopy(planned,team*10+5,ys[team],0,5);
            } else {
                // Hard/standard never fall back to the old omniscient target.
                if(level==AiDifficulty.RELAXED){xs[team][0]=(short)(nearest>>>16);ys[team][0]=(short)nearest;}
                else if((readyMask&(1<<team))!=0)xs[team][0]=ys[team][0]=MODEL_IDLE;
                for(int slot=1;slot<5;slot++)xs[team][slot]=ys[team][slot]=-1;
            }
        }
        if(level==AiDifficulty.RELAXED)chooseLocalItems(xs,ys,controlledMask);
        applyModelInputs(xs,ys,controlledMask);
        tick=tick==Integer.MAX_VALUE?64:tick+1;
    }
}
