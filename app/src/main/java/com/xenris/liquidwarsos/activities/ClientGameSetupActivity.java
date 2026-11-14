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
import android.view.Window;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.EditText;
import android.widget.Toast;
import android.graphics.drawable.Drawable;
import android.content.Context;
import android.content.Intent;


import com.xenris.liquidwarsos.Client;
import com.xenris.liquidwarsos.net.BluetoothAccess;
import com.xenris.liquidwarsos.net.BluetoothPermissions;
import com.xenris.liquidwarsos.net.BluetoothTransport;
import com.xenris.liquidwarsos.R;
import com.xenris.liquidwarsos.StaticBits;
import com.xenris.liquidwarsos.Util;

import java.io.InputStream;
import java.io.IOException;

public class ClientGameSetupActivity extends LocalizedActivity implements Client.ClientCallbacks {
    private EditText nameEditText;
    private Context context;
    private String serverIP;
    private String serverName;
    private int retries;
    private int myID = 0;
    private Client roomClient;
    private com.xenris.liquidwarsos.online.OnlineSession online;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        context = this;
        StaticBits.multiplayerGameSetupActivity = null;
        StaticBits.clientGameSetupActivity = this;
        retries = 4;
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.client_game_setup);
        StaticBits.init();
        refreshMapImage();

        Bundle extras = getIntent().getExtras();
        if (extras == null) { finish(); return; }
        serverIP = extras.getString("ip");
        serverName = extras.getString("name");
        setTextView(R.id.client_game_textview, getString(R.string.connecting_to, serverName));
        int port = extras.getInt("port", StaticBits.PORT_NUMBER);
        if (port < 1 || port > 65535) port = StaticBits.PORT_NUMBER;
        if(extras.getBoolean("online",false)){
            online=com.xenris.liquidwarsos.online.OnlineSession.active;
            if(online==null||online.host){finish();return;}
            roomClient=new Client(this,"cleanbizai.com",online.relay.connector());
            online.attach(this);
        } else if (extras.getBoolean(BluetoothTransport.EXTRA_MODE, false)) {
            if (!BluetoothAccess.has(this, BluetoothPermissions.Action.CONNECT)) {
                toast(getString(R.string.bt_permission_denied), Toast.LENGTH_LONG);
                finish();return;
            }
            roomClient = new Client(this, serverIP,
                    new BluetoothTransport.Connector(BluetoothAccess.adapter(this), serverIP));
        } else roomClient = new Client(this, serverIP, port);
        StaticBits.client = roomClient;
        roomClient.start(); // Publish the fully initialized client before any connection callback.
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (roomClient != null) roomClient.destroy();
        if(online!=null)online.close();
    }

    private void refreshMapImage() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                InputStream is = null;
                try {
                    if(StaticBits.map == -1)
                        is = getAssets().open("maps/random-map.png");
                    else
                        is = getAssets().open("maps/" + StaticBits.map +"-image.png");
                } catch(IOException e) {
                    try {
                        is = getAssets().open("maps/" + StaticBits.map +"-map.png");
                    } catch(IOException ex) { }
                }
                Drawable d = Drawable.createFromStream(is, null);
                ImageView iv = (ImageView)findViewById(R.id.map_imageview);
                iv.setImageDrawable(d);
            }
        });
    }

    @Override
    public void onServerMessageReceived(int argc, int[] args) {
        if(args[0] == StaticBits.UPDATE_SERVER_NAME) {
            byte[] nameBytes = new byte[argc - 1];
            for (int n = 1; n < argc; n++) nameBytes[n - 1] = (byte) args[n];
            serverName = new String(nameBytes, java.nio.charset.StandardCharsets.UTF_8);
            setTextView(R.id.client_game_textview, getString(R.string.connected_to, serverName));
        } else if(args[0] == StaticBits.SET_TEAM) {
            setTextView(R.id.team_textview, Util.teamToNameString(this, args[1]));
            StaticBits.team = args[1];
        } else if(args[0] == StaticBits.SET_TIME_LIMIT) {
            setTextView(R.id.timeout_textview, Util.getTimeoutString(this, args[1]));
            StaticBits.timeLimit = Util.intToTime(args[1]);
        } else if(args[0] == StaticBits.SET_MAP) {
            if(argc<2||!com.xenris.liquidwarsos.MapCatalog.validSelection(args[1])){finish();return;}
            setTextView(R.id.map_textview, Util.getMapName(this, args[1]));
            StaticBits.map = com.xenris.liquidwarsos.MapCatalog.mapIdForSelection(args[1]);
            refreshMapImage();
        } else if(args[0] == StaticBits.START_GAME) {
            if(argc!=11||args[10]<0||args[10]>1){checkVersionCompatibility(28);return;}
            if(!com.xenris.liquidwarsos.MapCatalog.validMapChoice(args[2])){finish();return;}
            int[] groups=java.util.Arrays.copyOfRange(args,4,10);
            if(!com.xenris.liquidwarsos.AllianceRules.valid(groups)){finish();return;}
            StaticBits.alliances=groups;
            StaticBits.seed = args[1];
            StaticBits.map = args[2];
            StaticBits.dotsPerTeam = args[3];
            StaticBits.operationsMode=args[10];
            StaticBits.client.setCallbacks(null);
            startClientGame();
        } else if(args[0] == StaticBits.SEND_VERSION_CODE) {
            if(args[1] != StaticBits.VERSION_CODE)
                checkVersionCompatibility(args[1]);
        } else if(args[0] == StaticBits.SET_OPERATIONS_MODE) {
            if(argc==2&&args[1]>=0&&args[1]<=1){StaticBits.operationsMode=args[1];setTextView(R.id.client_game_textview,serverName+(args[1]==1?" · 据点争夺 / Conquest":" · Classic"));}
        } else if(args[0] == StaticBits.SET_TEAM_SIZE) {
            StaticBits.dotsPerTeam = args[1];
            setTextView(R.id.teamsize_textview, args[1] + "");
        }
    }

    @Override
    public void onServerConnectionMade(int id, String ip) {
        myID = id;
        setTextView(R.id.client_game_textview, getString(R.string.connected_to, serverName));
        StaticBits.client.send(StaticBits.SEND_VERSION_CODE, StaticBits.VERSION_CODE);
    }

    @Override
    public void onServerConnectionFailed(String ip) {
        myID = 0;
        toast(getString(R.string.failed_to_connect_to, serverName), Toast.LENGTH_SHORT);
        finish();
    }

    @Override
    public void onServerConnectionClosed(String ip) {
        myID = 0;
        toast(getString(R.string.lost_connection_to, serverName), Toast.LENGTH_SHORT);
        finish();
    }

    private void checkVersionCompatibility(int v) {
        if(StaticBits.VERSION_CODE < v) {
            toast(getString(R.string.client_update_required), Toast.LENGTH_LONG);
            finish();
        } else if(StaticBits.VERSION_CODE > v) {
            toast(getString(R.string.host_update_required), Toast.LENGTH_LONG);
            finish();
        }
    }

    private void startClientGame() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (isFinishing() || isDestroyed()) return;
                Intent intent = new Intent(context, GameClientActivity.class);
                startActivity(intent);
            }
        });
    }

    private void setTextView(final int id, final String message) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (isFinishing() || isDestroyed()) return;
                TextView tv = (TextView)findViewById(id);
                tv.setText(message);
            }
        });
    }

    private void toast(final String message, final int length) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(context, message, length).show();
            }
        });
    }
}
