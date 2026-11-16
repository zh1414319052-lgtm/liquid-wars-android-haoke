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
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);

        setContentView(R.layout.main_menu);
        ((android.widget.TextView)findViewById(R.id.garden_trial)).setTypeface(android.graphics.Typeface.createFromAsset(getAssets(),"fonts/NotoSansSC-Lobby600.ttf"));
        updates=new com.xenris.liquidwarsos.updates.MenuUpdateHost(this,true);
    }
    @Override public void onResume(){super.onResume();if(updates!=null)updates.resume();}
    @Override public void onPause(){if(updates!=null)updates.pause();super.onPause();}
    @Override public void onDestroy(){if(updates!=null)updates.close();super.onDestroy();}
    @Override public void onWindowFocusChanged(boolean focused){super.onWindowFocusChanged(focused);if(updates!=null)updates.focus(focused);}

    public void singlePlayerMenu(View view) {
        Intent intent = new Intent(this, SinglePlayerGameSetupActivity.class);
        startActivity(intent);
    }
    public void largeMapMenu(View view){startActivity(new Intent(this,SinglePlayerGameSetupActivity.class).putExtra("largeMode",true));}
    public void garden3DMenu(View view){
        android.widget.LinearLayout box=new android.widget.LinearLayout(this);box.setOrientation(1);box.setPadding(28,20,28,20);
        android.graphics.Typeface font=android.graphics.Typeface.createFromAsset(getAssets(),"fonts/NotoSansSC-Lobby600.ttf");
        android.widget.TextView title=new android.widget.TextView(this);title.setTypeface(font);title.setText(com.xenris.liquidwarsos.BattleText.t("3D远征 · 选择总席位\n本地由你和电脑参战；每队1000颗，按兵力排名","3D Expedition · Select seats\nPlay locally with AI; 1,000 units per army, ranked by army size"));title.setTextSize(17);box.addView(title);
        android.widget.NumberPicker seats=new android.widget.NumberPicker(this);seats.setMinValue(2);seats.setMaxValue(10);seats.setValue(Math.max(2,Math.min(10,getSharedPreferences("battle-controls",0).getInt("expedition-seats",6))));seats.setOnValueChangedListener((picker,oldValue,newValue)->getSharedPreferences("battle-controls",0).edit().putInt("expedition-seats",newValue).apply());box.addView(seats);android.widget.Button online=new android.widget.Button(this);online.setText(com.xenris.liquidwarsos.BattleText.t("联机远征 · 匹配与好友房","Online expedition · Matchmaking and friend rooms"));online.setTypeface(font);online.setTextColor(0xffc5e5d3);box.addView(online);
        android.app.AlertDialog dialog=new android.app.AlertDialog.Builder(this).setView(box).setPositiveButton(com.xenris.liquidwarsos.BattleText.t("开始远征","Start expedition"),(d,k)->startActivity(new Intent(this,GodotGardenActivity.class).putExtra(com.xenris.liquidwarsos.GardenSession.EXTRA,true).putExtra("expedition_seats",seats.getValue()))).setNegativeButton(com.xenris.liquidwarsos.BattleText.t("返回","Back"),null).create();dialog.show();online.setOnClickListener(v->{dialog.dismiss();startActivity(new Intent(this,ExpeditionLobbyActivity.class));});for(int i:new int[]{-1,-2}){dialog.getButton(i).setTypeface(font);dialog.getButton(i).setTextColor(0xffb4ddc9);}
    }
    public void spectatorMenu(View view){startActivity(new Intent(this,SinglePlayerGameSetupActivity.class).putExtra("spectatorSetup",true));}

    public void multiplayerMenu(View view) {
        // Discovery does not require reading SSID/location or a legacy Wi-Fi IPv4 value.
        startActivity(new Intent(this, MultiplayerMenuActivity.class));
    }
    public void onlineMenu(View view) {startActivity(new Intent(this,OnlineLobbyActivity.class));}

    public void instructions(View view) {
        Intent intent = new Intent(this, InstructionsActivity.class);
        startActivity(intent);
    }

    public void bluetoothMenu(View view) {
        startActivity(new Intent(this, BluetoothMenuActivity.class));
    }
}
