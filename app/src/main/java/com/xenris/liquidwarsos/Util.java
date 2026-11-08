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

import android.os.Handler;
import android.app.Dialog;
import android.content.Context;

public class Util {
    public static void loadPlayerInitialPositions(short[][] xs, short[][] ys) {
        for(int p = 0; p < 6; p++)
            for(int i = 0; i < 5; i++)
                xs[p][i] = ys[p][i] = -1;
        short[] points=NativeInterface.initialPositions();
        if(points==null||points.length!=12)throw new IllegalStateException("Create native match before reading spawn cursors");
        for(int p=0;p<6;p++){xs[p][0]=points[p*2];ys[p][0]=points[p*2+1];}
    }

    public static String teamToNameString(Context context, int team) {
        String[] names = context.getResources().getStringArray(R.array.teams_array);
        return team >= 0 && team < names.length ? names[team] : context.getString(R.string.team_unknown);
    }

    public static String getMapName(Context c, int m) {
        String[] maps = c.getResources().getStringArray(R.array.maps_array);
        return maps[m>=0&&m<maps.length?m:0];
    }

    public static String getTimeoutString(Context c, int t) {
        String[] timeout = c.getResources().getStringArray(R.array.timeout_array);
        return timeout[t];
    }

    public static int clientIdToPlayerNumber(int id) {
        for(int i = 0; i < 6; i++)
            if(StaticBits.teams[i] == id)
                return i;
        return -1;
    }

    public static int intToTime(int i) {
        switch(i) {
            case 0: return 30;
            case 1: return 60;
            case 2: return 60*2;
            case 3: return 60*3;
            case 4: return 60*5;
            case 5: return 60*10;
            case 6: return 60*60*24*23;
        }
        return 60*3;
    }

    private static final ThreadLocal<StepPacer> PACER=ThreadLocal.withInitial(StepPacer::new);
    public static void regulateSpeed(long previousTime,int totalDelay){
        long wait=PACER.get().delay(previousTime,System.nanoTime(),totalDelay);
        if(wait<=0)return;
        try{Thread.sleep(wait/1000000L,(int)(wait%1000000L));}catch(InterruptedException interrupted){PACER.get().reset();Thread.currentThread().interrupt();}
    }

    public static void makeDialogCancelableIn(final Dialog dialog, int millis) {
        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            public void run() {
            dialog.setCanceledOnTouchOutside(true);
        }}, millis);
    }

    public static void makeDialogDismissIn(final Dialog dialog, int millis) {
        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            public void run() {
                dialog.dismiss();
        }}, millis);
    }
}
