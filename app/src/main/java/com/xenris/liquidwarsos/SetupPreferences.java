// SPDX-License-Identifier: GPL-3.0-or-later
package com.xenris.liquidwarsos;

import android.app.Activity;
import android.content.SharedPreferences;
import android.widget.Spinner;

/** Stable values, not translated labels/resource integers. Session/peer state is never saved. */
public final class SetupPreferences {
    private SetupPreferences(){}
    public static SharedPreferences store(Activity a){return a.getSharedPreferences("match-setup",0);}
    public static String scope(Activity a){
        if(a instanceof com.xenris.liquidwarsos.activities.MultiplayerGameSetupActivity)return "room";
        return a.getIntent().getBooleanExtra("largeMode",false)?"large":"classic";
    }
    public static void restore(Activity a,boolean large){
        SharedPreferences p=store(a);String s=scope(a)+".";
        Spinner map=a.findViewById(R.id.map_spinner),time=a.findViewById(R.id.timeout_spinner),size=a.findViewById(R.id.teamsize_spinner),team=a.findViewById(R.id.team_spinner);
        if(p.contains(s+"map")){
            int wanted=p.getInt(s+"map",-1);
            for(int i=0;i<map.getCount();i++)if((large?MapCatalog.largeIdAt(i):MapCatalog.mapIdForSelection(i))==wanted){map.setSelection(i);break;}
        }
        if(p.contains(s+"time")){int wanted=p.getInt(s+"time",120);for(int i=0;i<time.getCount();i++)if(Util.intToTime(i)==wanted){time.setSelection(i);break;}}
        if(p.contains(s+"population")){int wanted=p.getInt(s+"population",400);for(int i=0;i<size.getCount();i++)if(String.valueOf(size.getItemAtPosition(i)).equals(String.valueOf(wanted))){size.setSelection(i);break;}}
        if(team!=null){int selected=p.getInt(s+"team",0);team.setSelection(Math.max(0,Math.min(team.getCount()-1,selected)));}
    }
    public static void remember(Activity a,int id){
        String key;int value;
        if(id==R.id.map_spinner){key="map";value=StaticBits.map;}
        else if(id==R.id.timeout_spinner){key="time";value=StaticBits.timeLimit;}
        else if(id==R.id.teamsize_spinner){key="population";value=StaticBits.dotsPerTeam;}
        else if(id==R.id.team_spinner){key="team";value=StaticBits.team;}
        else return;
        store(a).edit().putInt(scope(a)+"."+key,value).apply();
    }
}
