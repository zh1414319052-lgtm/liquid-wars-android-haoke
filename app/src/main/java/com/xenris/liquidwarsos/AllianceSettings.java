// SPDX-License-Identifier: GPL-3.0-or-later
package com.xenris.liquidwarsos;
import android.content.Context;
public final class AllianceSettings {
    public static int[] load(Context c){android.content.SharedPreferences p=c.getSharedPreferences("alliance_rules",0);if(!p.getBoolean("paired",false))return AllianceRules.solo();int n=p.getInt("pairing",0);return AllianceRules.pairings().get(Math.max(0,Math.min(14,n)));}
    public static String name(Context c,int team){StringBuilder b=new StringBuilder();for(int p=0;p<6;p++)if(AllianceRules.allied(team,p)){if(b.length()>0)b.append(" + ");b.append(Util.teamToNameString(c,p));}return b.toString();}
    public static void save(Context c,boolean paired,int option){c.getSharedPreferences("alliance_rules",0).edit().putBoolean("paired",paired).putInt("pairing",option).apply();}
}
