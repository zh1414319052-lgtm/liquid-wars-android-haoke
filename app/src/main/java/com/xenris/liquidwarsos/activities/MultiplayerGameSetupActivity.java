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

import android.app.AlertDialog;
import android.os.Bundle;
import android.bluetooth.BluetoothAdapter;
import android.os.Build;
import androidx.core.app.ActivityCompat;
import com.xenris.liquidwarsos.net.BluetoothAccess;
import com.xenris.liquidwarsos.net.BluetoothPermissions;
import com.xenris.liquidwarsos.net.BluetoothTransport;
import com.xenris.liquidwarsos.server.LanRoom;
import android.view.View;
import android.view.View.OnLongClickListener;
import android.view.Window;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.EditText;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.ArrayAdapter;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.Toast;
import android.graphics.drawable.Drawable;
import android.content.Context;
import android.content.Intent;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;

import androidx.appcompat.widget.AppCompatImageButton;

import com.xenris.liquidwarsos.server.NetInfo;
import com.xenris.liquidwarsos.R;
import com.xenris.liquidwarsos.server.Server;
import com.xenris.liquidwarsos.server.ServerFinder;
import com.xenris.liquidwarsos.StaticBits;
import com.xenris.liquidwarsos.Util;

import java.io.InputStream;
import java.io.IOException;

public class MultiplayerGameSetupActivity extends LocalizedActivity implements OnItemSelectedListener, OnLongClickListener, Server.ServerCallbacks {
    private Spinner teamSpinner;
    private Spinner mapSpinner;
    private Spinner timeoutSpinner;
    private Spinner teamSizeSpinner;
    private EditText nameEditText;
    private Context context;
    private TextView nametv;
    private int myID = 0;
    private boolean roomVisible;
    private volatile boolean startingMatch;
    private boolean bluetoothRoom;
    private com.xenris.liquidwarsos.online.OnlineSession online;
    private boolean offerVisibility;
    private Server roomServer;
    private static final int VISIBLE_REQUEST = 61, VISIBLE_PERMISSION = 62;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        context = this;
        StaticBits.multiplayerGameSetupActivity = this;
        StaticBits.clientGameSetupActivity = null;
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.multi_game_setup);
        StaticBits.init();
        if(getIntent().getBooleanExtra("online",false))StaticBits.operationsMode=getIntent().getIntExtra("onlineMode",0);
        refreshMapImage();
        initSpinners();
        initButtons();
        com.xenris.liquidwarsos.OperationMode.bind(this,true,()->{if(roomServer!=null)roomServer.sendToAll(StaticBits.SET_OPERATIONS_MODE,StaticBits.operationsMode);});
        com.xenris.liquidwarsos.ui.DifficultyPicker.bind(this, true, this::difficultyChanged);
        bluetoothRoom = getIntent().getBooleanExtra(BluetoothTransport.EXTRA_MODE, false);
        if(getIntent().getBooleanExtra("online",false)) {
            online=com.xenris.liquidwarsos.online.OnlineSession.active;
            if(online==null||!online.host){finish();return;}
            roomServer=new Server(this,online.relay.listener());
            online.attach(this);
        } else if (bluetoothRoom) {
            if (!BluetoothAccess.has(this, BluetoothPermissions.Action.HOST)) {
                toast(getString(R.string.bt_permission_denied), Toast.LENGTH_LONG);
                finish();
                return;
            }
            roomServer = new Server(this, BluetoothTransport.listen(BluetoothAccess.adapter(this)));
            findViewById(R.id.bluetooth_visible_button).setVisibility(View.VISIBLE);
            offerVisibility = savedInstanceState == null;
        } else roomServer = new Server(this, StaticBits.PORT_NUMBER);
        StaticBits.server = roomServer;
        updateMessageTextView();
        nametv = (TextView)findViewById(R.id.public_name_textview);
        nametv.setText(baseRoomName());
        TextView tv = (TextView)findViewById(R.id.team_textview);
        tv.setText(Util.teamToNameString(this, 0));
    }

    private final java.util.Set<Integer> compatiblePeers=java.util.concurrent.ConcurrentHashMap.newKeySet();
    private boolean apiTeamsConfigured;
    @Override
    public void onResume() {
        super.onResume();
        apiTeamsConfigured=false;for(com.xenris.liquidwarsos.cloud.ModelConfig c:com.xenris.liquidwarsos.cloud.ModelStore.all(this))apiTeamsConfigured|=c.enabled;
        difficultyChanged();
        roomVisible = true;
        startingMatch = false;
        if(online!=null)online.ended();
        StaticBits.newSeed();
        if(StaticBits.gameWasDisconnected) {
            if(StaticBits.client != null)
                StaticBits.client.destroy();
            StaticBits.client = null;
            myID = 0;
            for(int i = 1; i < 6; i++)
                StaticBits.teams[i] = StaticBits.AI_PLAYER;
            StaticBits.teams[0] = myID;
            StaticBits.gameWasDisconnected = false;
        }
        if (roomServer != null) roomServer.startAccepting();
        updateRoomAdvertisement();
    }

    @Override public void onPause() {
        roomVisible = false;
        ServerFinder.stopSharing();
        super.onPause();
    }

    private boolean roomReady() {
        if (roomServer == null) return false;
        return bluetoothRoom ? roomServer.connectedClientCount() > 0 : roomServer.isAccepting();
    }

    private void updateRoomAdvertisement() {
        if (isFinishing() || isDestroyed() || StaticBits.multiplayerGameSetupActivity != this) return;
        findViewById(R.id.start_button).setEnabled(!startingMatch && roomReady());
        if (online==null && !bluetoothRoom && roomVisible && !startingMatch && roomServer != null
                && roomServer.isAccepting() && roomServer.connectedClientCount() < roomServer.clientCapacity())
            ServerFinder.share(context, StaticBits.PORT_NUMBER + 1, roomName());
        else ServerFinder.stopSharing();
    }

    @Override public void onListeningChanged(boolean ready) {
        runOnUiThread(() -> {
            if (!roomVisible || isFinishing() || isDestroyed()) return;
            updateRoomAdvertisement();
            updateMessageTextView();
            if (ready && bluetoothRoom && offerVisibility) {
                offerVisibility = false;
                makeBluetoothVisible(null);
            } else if (!ready && !startingMatch && !roomReady()) {
                toast(getString(bluetoothRoom ? R.string.bt_host_failed : R.string.lan_host_failed), Toast.LENGTH_LONG);
            }
        });
    }

    public void makeBluetoothVisible(View view) {
        if (!bluetoothRoom || isFinishing()) return;
        String[] missing = BluetoothAccess.missing(this, BluetoothPermissions.Action.HOST);
        if (missing.length != 0) { ActivityCompat.requestPermissions(this, missing, VISIBLE_PERMISSION); return; }
        if (roomServer != null && !roomServer.isAccepting() && !roomServer.isStarting()) roomServer.startAccepting();
        try {
            Intent request = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
            request.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 120);
            startActivityForResult(request, VISIBLE_REQUEST);
        } catch (RuntimeException unavailable) { toast(getString(R.string.bt_host_failed), Toast.LENGTH_LONG); }
    }

    @Override public void onRequestPermissionsResult(int request, String[] permissions, int[] grants) {
        super.onRequestPermissionsResult(request, permissions, grants);
        if (request == VISIBLE_PERMISSION) {
            if (BluetoothAccess.has(this, BluetoothPermissions.Action.HOST)) makeBluetoothVisible(null);
            else toast(getString(R.string.bt_permission_denied), Toast.LENGTH_LONG);
        }
    }

    @Override protected void onActivityResult(int request, int result, Intent data) {
        super.onActivityResult(request, result, data);
        if (request == VISIBLE_REQUEST && result == RESULT_CANCELED)
            toast(getString(R.string.bt_visibility_declined), Toast.LENGTH_LONG);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (roomServer != null) roomServer.destroy();
        if(online!=null)online.close();
        ServerFinder.stopSharing();
    }

    public void changePublicName(View view) {
        OnClickListener clicker = new OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    String name = LanRoom.cleanName(nameEditText.getText().toString());
                    if((name.length() > 0) && (!baseRoomName().equals(name))) {
                        StaticBits.publicName = name;
                        ServerFinder.stopSharing();
                        updateRoomAdvertisement();
                        byte[] nameBytes = roomName().getBytes(java.nio.charset.StandardCharsets.UTF_8);
                        int[] data = new int[1 + nameBytes.length];
                        data[0] = StaticBits.UPDATE_SERVER_NAME;
                        for(int i = 1; i < data.length; i++)
                            data[i] = nameBytes[i-1];
                        roomServer.sendToAll(data.length, data);
                        nametv.setText(baseRoomName());
                    }
                }
            };
        nameEditText = new EditText(this);
        nameEditText.setText(nametv.getText());
        new AlertDialog.Builder(this)
            .setTitle(R.string.enter_room_name)
            .setPositiveButton(R.string.done, clicker)
            .setNegativeButton(R.string.cancel, null)
            .setView(nameEditText)
            .show();
    }

    private void initSpinners() {
        ArrayAdapter<CharSequence> adapter;
        final int simpleSpinnerItem = R.layout.ui_spinner_item;

        mapSpinner = (Spinner)findViewById(R.id.map_spinner);
        adapter = ArrayAdapter.createFromResource(this, R.array.maps_array, simpleSpinnerItem);
        adapter.setDropDownViewResource(R.layout.ui_spinner_dropdown_item);
        mapSpinner.setAdapter(adapter);
        mapSpinner.setOnItemSelectedListener(this);

        timeoutSpinner = (Spinner)findViewById(R.id.timeout_spinner);
        adapter = ArrayAdapter.createFromResource(this, R.array.timeout_array, simpleSpinnerItem);
        adapter.setDropDownViewResource(R.layout.ui_spinner_dropdown_item);
        timeoutSpinner.setAdapter(adapter);
        timeoutSpinner.setOnItemSelectedListener(this);
        timeoutSpinner.setSelection(2);

        teamSizeSpinner = (Spinner)findViewById(R.id.teamsize_spinner);
        adapter = ArrayAdapter.createFromResource(this, R.array.teamsize_array, simpleSpinnerItem);
        adapter.setDropDownViewResource(R.layout.ui_spinner_dropdown_item);
        teamSizeSpinner.setAdapter(adapter);
        teamSizeSpinner.setOnItemSelectedListener(this);
        teamSizeSpinner.setSelection(2);
    }

    private void initButtons() {
        AppCompatImageButton previousButton = (AppCompatImageButton)findViewById(R.id.previous_button);
        previousButton.setOnLongClickListener(this);
        AppCompatImageButton nextButton = (AppCompatImageButton)findViewById(R.id.next_button);
        nextButton.setOnLongClickListener(this);
    }

    public void start(View view) {
        if (startingMatch) return;
        if(roomServer!=null&&compatiblePeers.size()<roomServer.connectedClientCount()){toast(getString(R.string.model_version_wait),Toast.LENGTH_SHORT);return;}
        if (!roomReady()) {
            toast(getString(bluetoothRoom ? R.string.bt_host_failed : R.string.lan_host_failed), Toast.LENGTH_LONG);
            return;
        }
        startingMatch = true;
        if(online!=null){
            updateRoomAdvertisement();
            new Thread(()->{
                try{online.beginBattle();runOnUiThread(()->{if(!isFinishing()&&!isDestroyed())beginMatch();});}
                catch(Exception error){runOnUiThread(()->{startingMatch=false;updateRoomAdvertisement();toast(error.getMessage(),Toast.LENGTH_LONG);});}
            },"Authorize online battle").start();
            return;
        }
        beginMatch();
    }

    private void beginMatch() {
        ServerFinder.stopSharing();
        roomServer.stopAccepting();
        StaticBits.alliances=com.xenris.liquidwarsos.AllianceSettings.load(this);
        int[] args = new int[11];args[0]=StaticBits.START_GAME;args[1]=StaticBits.seed;args[2]=StaticBits.map;args[3]=StaticBits.dotsPerTeam;System.arraycopy(StaticBits.alliances,0,args,4,6);args[10]=StaticBits.operationsMode;
        roomServer.sendToAll(args.length, args);
        roomServer.setCallbacks(null);
        StaticBits.team = Util.clientIdToPlayerNumber(myID);

        Intent intent = new Intent(this, GameServerActivity.class);
        startActivity(intent);
    }

    @Override
    public boolean onLongClick(View view) {
        final int id = view.getId();
        if(id == R.id.next_button) {
            int pos = mapSpinner.getSelectedItemPosition();
            pos += 20;
            if(pos > StaticBits.NUMBER_OF_MAPS)
                pos = StaticBits.NUMBER_OF_MAPS;
            mapSpinner.setSelection(pos);
        } else if(id == R.id.previous_button) {
            int pos = mapSpinner.getSelectedItemPosition();
            pos -= 20;
            if(pos < 0)
                pos = 0;
            mapSpinner.setSelection(pos);
        }
        return true;
    }

    public void nextMap(View view) {
        int pos = mapSpinner.getSelectedItemPosition();
        pos++;
        if(pos > StaticBits.NUMBER_OF_MAPS)
            pos = StaticBits.NUMBER_OF_MAPS;
        mapSpinner.setSelection(pos);
    }

    public void previousMap(View view) {
        int pos = mapSpinner.getSelectedItemPosition();
        pos--;
        if(pos < 0)
            pos = 0;
        mapSpinner.setSelection(pos);
    }

    public void changeTeam(View view) {
        //TODO cycle through available teams.
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
        final int spinnerId = parent.getId();
        if(spinnerId == R.id.map_spinner) {
            StaticBits.map = com.xenris.liquidwarsos.MapCatalog.mapIdForSelection(pos);
            int m = mapSpinner.getSelectedItemPosition();
            roomServer.sendToAll(StaticBits.SET_MAP, pos);
            refreshMapImage();
        } else if(spinnerId == R.id.timeout_spinner) {
            StaticBits.timeLimit = Util.intToTime(pos);
            int t = timeoutSpinner.getSelectedItemPosition();
            roomServer.sendToAll(StaticBits.SET_TIME_LIMIT, pos);
        } else if(spinnerId == R.id.teamsize_spinner) {
            if(view != null) {
                StaticBits.dotsPerTeam = Integer.parseInt(((TextView)view).getText() + "");
            }
            roomServer.sendToAll(StaticBits.SET_TEAM_SIZE, StaticBits.dotsPerTeam);
        roomServer.sendToAll(StaticBits.SET_OPERATIONS_MODE,StaticBits.operationsMode);
        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) { }

    private void refreshMapImage() {
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

    @Override
    public void onClientMessageReceived(int id, int argc, int[] args) {
         if(args[0] == StaticBits.SEND_VERSION_CODE) {
            if(args[1] != StaticBits.VERSION_CODE){checkVersionCompatibility(args[1]);roomServer.disconnectPeer(id);}else compatiblePeers.add(id);
        }
    }

    private void checkVersionCompatibility(int v) {
        if(StaticBits.VERSION_CODE < v)
            toast(getString(R.string.client_update_required), Toast.LENGTH_LONG);
    }

    @Override
    public void onClientConnected(int id) {
        compatiblePeers.remove(id);
        if (startingMatch || isFinishing() || isDestroyed()
                || StaticBits.multiplayerGameSetupActivity != this) {
            if (roomServer != null) roomServer.disconnectPeer(id);
            return;
        }
        runOnUiThread(this::updateRoomAdvertisement);
        updateMessageTextView();
        for(int i = 0; i < 6; i++) {
            if(StaticBits.teams[i] == StaticBits.AI_PLAYER) {
                StaticBits.teams[i] = id;
                roomServer.sendToOne(id, StaticBits.SET_TEAM, i);
                roomServer.sendToOne(id, StaticBits.SEND_VERSION_CODE, StaticBits.VERSION_CODE);
                break;
            }
        }
        {
            byte[] roomNameBytes = LanRoom.cleanName(roomName()).getBytes(java.nio.charset.StandardCharsets.UTF_8);
            int[] nameMessage = new int[1 + roomNameBytes.length];
            nameMessage[0] = StaticBits.UPDATE_SERVER_NAME;
            for (int n = 0; n < roomNameBytes.length; n++) nameMessage[n + 1] = roomNameBytes[n];
            roomServer.sendToOne(id, nameMessage.length, nameMessage);
        }
        int t = timeoutSpinner.getSelectedItemPosition();
        roomServer.sendToOne(id, StaticBits.SET_TIME_LIMIT, t);
        int m = mapSpinner.getSelectedItemPosition();
        roomServer.sendToOne(id, StaticBits.SET_MAP, m);
        roomServer.sendToAll(StaticBits.SET_TEAM_SIZE, StaticBits.dotsPerTeam);
        roomServer.sendToAll(StaticBits.SET_OPERATIONS_MODE,StaticBits.operationsMode);
    }

    @Override
    public void onClientDisconnected(int id) {
        compatiblePeers.remove(id);
        runOnUiThread(this::updateRoomAdvertisement);
        updateMessageTextView();
        for(int i = 0; i < 6; i++) {
            if(StaticBits.teams[i] == id) {
                StaticBits.teams[i] = StaticBits.AI_PLAYER;
                break;
            }
        }
    }

    private void updateMessageTextView() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                TextView tv = (TextView)findViewById(R.id.multi_game_textview);
                String ip = NetInfo.getIPAddress(context);
                if (isFinishing() || isDestroyed()) return;
                int players = roomServer == null ? 1 : roomServer.connectedClientCount() + 1;
                if(online!=null){
                    tv.setText("公网房间 "+online.code+" · 真人 "+players+" / 电脑 "+(6-players)+"\n房主选择地图和规则后开始 · 长按表情按钮隐藏提示");
                } else if (bluetoothRoom) {
                    String deviceName = getString(R.string.bt_unknown_device);
                    try {
                        BluetoothAdapter adapter = BluetoothAccess.adapter(context);
                        if (adapter != null && BluetoothAccess.has(context, BluetoothPermissions.Action.CONNECT))
                            deviceName = adapter.getName();
                    } catch (SecurityException denied) { }
                    tv.setText(getString(R.string.bt_host_status, roomName(), players, deviceName));
                } else tv.setText(getString(R.string.lan_host_status, roomName(), players, ip));
            }
        });
    }

    private String roomName() {
        String label=getResources().getStringArray(R.array.ai_room_labels)[com.xenris.liquidwarsos.AiDifficulty.valid(StaticBits.aiDifficulty)];
        if(apiTeamsConfigured)label="API";
        int[] pairs=com.xenris.liquidwarsos.AllianceSettings.load(this);
        if(!java.util.Arrays.equals(pairs,com.xenris.liquidwarsos.AllianceRules.solo()))label+=" "+com.xenris.liquidwarsos.AllianceRules.label(pairs);
        return com.xenris.liquidwarsos.ui.DifficultyRoomName.format(baseRoomName(),label);
    }

    private String baseRoomName() {
        return StaticBits.publicName != null ? StaticBits.publicName : getString(R.string.default_room_name);
    }

    private void difficultyChanged() {
        if (roomServer==null || startingMatch || isFinishing() || isDestroyed()) return;
        ServerFinder.stopSharing();updateRoomAdvertisement();updateMessageTextView();
        byte[] name=roomName().getBytes(java.nio.charset.StandardCharsets.UTF_8);
        int[] message=new int[name.length+1];message[0]=StaticBits.UPDATE_SERVER_NAME;
        for(int i=0;i<name.length;i++)message[i+1]=name[i];
        roomServer.sendToAll(message.length,message);
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
