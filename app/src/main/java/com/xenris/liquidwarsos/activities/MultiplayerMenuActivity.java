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
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.content.Intent;
import android.text.InputType;
import com.xenris.liquidwarsos.R;
import com.xenris.liquidwarsos.StaticBits;
import com.xenris.liquidwarsos.server.LanRoom;
import com.xenris.liquidwarsos.server.ServerFinder;
import java.util.ArrayList;

public class MultiplayerMenuActivity extends LocalizedActivity {
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final ArrayList<LanRoom> rooms = new ArrayList<>();
    private ArrayAdapter<LanRoom> adapter;
    private TextView status;
    private boolean active;
    private boolean joining;
    private int scanGeneration;
    private long scanStarted;
    private AlertDialog manualDialog;
    private String lastManualIp = "";

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.multiplayer_menu);
        status = findViewById(R.id.lan_status);
        adapter = new ArrayAdapter<LanRoom>(this, R.layout.lan_room_row, android.R.id.text1, rooms) {
            @Override public View getView(int position, View reusable, ViewGroup parent) {
                View row = super.getView(position, reusable, parent);
                LanRoom room = getItem(position);
                ((TextView)row.findViewById(android.R.id.text1)).setText(room.name);
                ((TextView)row.findViewById(android.R.id.text2)).setText(getString(R.string.lan_room_address, room.ip));
                return row;
            }
        };
        ListView list = findViewById(R.id.lan_rooms);
        list.setAdapter(adapter);
        list.setEmptyView(findViewById(R.id.lan_empty));
        list.setOnItemClickListener((parent, view, position, id) -> {
            if (position >= 0 && position < rooms.size()) join(rooms.get(position));
        });
    }

    @Override public void onResume() {
        super.onResume();
        active = true;
        joining = false;
        startScan();
    }

    @Override public void onPause() {
        active = false;
        stopScan();
        if (manualDialog != null) manualDialog.dismiss();
        super.onPause();
    }

    @Override public void onDestroy() {
        stopScan();
        super.onDestroy();
    }

    private void stopScan() {
        scanGeneration++;
        handler.removeCallbacksAndMessages(null);
        ServerFinder.stopSearching(); // This page never owns a host's publisher.
    }

    private void startScan() {
        stopScan();
        if (!active || joining) return;
        rooms.clear();
        adapter.notifyDataSetChanged();
        status.setText(R.string.lan_searching);
        ((TextView)findViewById(R.id.lan_empty)).setText(R.string.lan_searching);
        scanStarted = SystemClock.elapsedRealtime();
        final int generation = scanGeneration;
        ServerFinder.search(this, discovered -> {
            if (!active || joining || generation != scanGeneration || isFinishing() || isDestroyed()) return;
            rooms.clear();
            rooms.addAll(discovered);
            adapter.notifyDataSetChanged();
            updateStatus();
        });
        handler.postDelayed(() -> {
            if (active && generation == scanGeneration) updateStatus();
        }, 8000);
    }

    private void updateStatus() {
        if (!rooms.isEmpty()) status.setText(getString(R.string.lan_rooms_found, rooms.size()));
        else {
            int message = SystemClock.elapsedRealtime() - scanStarted >= 8000
                    ? R.string.lan_no_rooms : R.string.lan_searching;
            status.setText(message);
            ((TextView)findViewById(R.id.lan_empty)).setText(message);
        }
    }

    public void refreshRooms(View view) { startScan(); }
    public void bluetoothMenu(View view) {
        if (!active || joining) return;
        joining = true;
        stopScan();
        startActivity(new Intent(this, BluetoothMenuActivity.class));
    }

    private void join(LanRoom room) {
        if (!active || joining) return;
        joining = true;
        stopScan();
        Intent intent = new Intent(this, ClientGameSetupActivity.class);
        intent.putExtra("ip", room.ip);
        intent.putExtra("port", room.port);
        intent.putExtra("name", room.name);
        startActivity(intent);
    }

    public void manualConnect(View view) {
        if (joining || (manualDialog != null && manualDialog.isShowing())) return;
        EditText input = new EditText(this);
        input.setSingleLine(true);
        input.setInputType(InputType.TYPE_CLASS_PHONE);
        input.setHint("192.168.1.10");
        input.setText(lastManualIp);
        manualDialog = new AlertDialog.Builder(this).setTitle(R.string.enter_ip)
                .setView(input).setPositiveButton(R.string.connect, null)
                .setNegativeButton(R.string.cancel, null).show();
        manualDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(button -> {
            String ip = input.getText().toString().trim();
            if (!validIpv4(ip)) { input.setError(getString(R.string.lan_invalid_ip)); return; }
            lastManualIp = ip;
            manualDialog.dismiss();
            join(new LanRoom(ip, ip, StaticBits.PORT_NUMBER));
        });
    }

    private static boolean validIpv4(String ip) {
        String[] parts = ip.split("\\.", -1);
        if (parts.length != 4 || ip.equals("0.0.0.0") || ip.equals("255.255.255.255")) return false;
        for (String part : parts) {
            if (!part.matches("[0-9]{1,3}")) return false;
            if (Integer.parseInt(part) > 255) return false;
        }
        return true;
    }

    public void startNewGame(View view) {
        if (!active || joining) return;
        joining = true;
        stopScan();
        startActivity(new Intent(this, MultiplayerGameSetupActivity.class));
    }
}
