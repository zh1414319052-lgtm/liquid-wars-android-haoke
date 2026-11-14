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
