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

package com.xenris.liquidwarsos.activities;

import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.content.Intent;


import com.xenris.liquidwarsos.R;

public class MainMenuActivity extends LocalizedActivity {
    private com.xenris.liquidwarsos.updates.MenuUpdateHost updates;
    private com.xenris.liquidwarsos.updates.ReleaseNotesHost notes;
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);

        setContentView(new com.xenris.liquidwarsos.ui.MainMenuView(this));
        updates=new com.xenris.liquidwarsos.updates.MenuUpdateHost(this,true);
        notes=new com.xenris.liquidwarsos.updates.ReleaseNotesHost(this,updates::setBlocked);
    }
    @Override public void onResume(){super.onResume();if(notes!=null)notes.resume();if(updates!=null)updates.resume();}
    @Override public void onPause(){if(updates!=null)updates.pause();if(notes!=null)notes.pause();super.onPause();}
    @Override public void onDestroy(){if(notes!=null)notes.close();if(updates!=null)updates.close();super.onDestroy();}
    @Override public void onWindowFocusChanged(boolean focused){super.onWindowFocusChanged(focused);if(notes!=null)notes.focus(focused);if(updates!=null)updates.focus(focused);}
    public void showReleaseNotes(View view){if(notes!=null)notes.manual();}

    public void singlePlayerMenu(View view) {
        Intent intent = new Intent(this, SinglePlayerGameSetupActivity.class);
        startActivity(intent);
    }
    public void largeMapMenu(View view){startActivity(new Intent(this,SinglePlayerGameSetupActivity.class).putExtra("largeMode",true));}
    public void garden3DMenu(View view){
        int pad=Math.round(16*getResources().getDisplayMetrics().density);
        android.widget.LinearLayout box=new android.widget.LinearLayout(this);box.setOrientation(1);box.setPadding(pad,pad/2,pad,pad/2);
        android.graphics.Typeface font=android.graphics.Typeface.createFromAsset(getAssets(),"fonts/NotoSansSC-Lobby600.ttf");
        android.widget.TextView title=new android.widget.TextView(this);title.setTypeface(font);title.setText(com.xenris.liquidwarsos.BattleText.t("3D远征","3D Expedition"));title.setTextSize(18);box.addView(title);
        android.widget.LinearLayout row=new android.widget.LinearLayout(this);row.setGravity(android.view.Gravity.CENTER_VERTICAL);
        android.widget.TextView label=new android.widget.TextView(this);label.setTypeface(font);label.setTextSize(14);label.setText(com.xenris.liquidwarsos.BattleText.t("本地对战席位","Local battle seats"));row.addView(label,new android.widget.LinearLayout.LayoutParams(0,-2,1));
        android.widget.Spinner seats=new android.widget.Spinner(this);
        android.widget.ArrayAdapter<String> choices=new android.widget.ArrayAdapter<String>(this,android.R.layout.simple_spinner_item,new String[]{"2","3","4","5","6","7","8","9","10"}){
            @Override public View getView(int p,View recycled,android.view.ViewGroup parent){View v=super.getView(p,recycled,parent);((android.widget.TextView)v).setTypeface(font);return v;}
            @Override public View getDropDownView(int p,View recycled,android.view.ViewGroup parent){View v=super.getDropDownView(p,recycled,parent);((android.widget.TextView)v).setTypeface(font);return v;}
        };
        choices.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);seats.setAdapter(choices);
        seats.setSelection(Math.max(2,Math.min(10,getSharedPreferences("battle-controls",0).getInt("expedition-seats",6)))-2);
        seats.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener(){
            @Override public void onItemSelected(android.widget.AdapterView<?> p,View v,int position,long id){getSharedPreferences("battle-controls",0).edit().putInt("expedition-seats",position+2).apply();}
            @Override public void onNothingSelected(android.widget.AdapterView<?> p){}
        });
        row.addView(seats,new android.widget.LinearLayout.LayoutParams(pad*5,pad*3));box.addView(row);
        android.widget.TextView localHint=new android.widget.TextView(this);localHint.setTypeface(font);localHint.setTextSize(12);localHint.setText(com.xenris.liquidwarsos.BattleText.t("本地由你和电脑参战；每队1000颗，按兵力排名","Local play with AI; 1,000 units per army, ranked by army size"));box.addView(localHint);
        android.widget.ScrollView scroll=new android.widget.ScrollView(this);scroll.addView(box);
        android.app.AlertDialog dialog=new android.app.AlertDialog.Builder(this).setView(scroll).setPositiveButton(com.xenris.liquidwarsos.BattleText.t("开始本地对战","Start local battle"),(d,k)->startActivity(new Intent(this,GodotGardenActivity.class).putExtra(com.xenris.liquidwarsos.GardenSession.EXTRA,true).putExtra("expedition_seats",seats.getSelectedItemPosition()+2))).setNegativeButton(com.xenris.liquidwarsos.BattleText.t("返回","Back"),null).create();dialog.show();for(int i:new int[]{-1,-2}){dialog.getButton(i).setTypeface(font);dialog.getButton(i).setTextColor(0xffb4ddc9);}
    }
    public void spectatorMenu(View view){startActivity(new Intent(this,SinglePlayerGameSetupActivity.class).putExtra("spectatorSetup",true));}

    public void multiplayerMenu(View view) {
        // Discovery does not require reading SSID/location or a legacy Wi-Fi IPv4 value.
        startActivity(new Intent(this, MultiplayerMenuActivity.class));
    }

    public void instructions(View view) {
        Intent intent = new Intent(this, InstructionsActivity.class);
        startActivity(intent);
    }

    public void supportGame(View view) {
        startActivity(new Intent(this, GameSupportActivity.class));
    }

    public void bluetoothMenu(View view) {
        startActivity(new Intent(this, BluetoothMenuActivity.class));
    }
}
