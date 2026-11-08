// SPDX-License-Identifier: GPL-3.0-or-later
package com.xenris.liquidwarsos.activities;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import androidx.core.app.ActivityCompat;
import com.xenris.liquidwarsos.R;
import com.xenris.liquidwarsos.net.BluetoothAccess;
import com.xenris.liquidwarsos.net.BluetoothPeers;
import com.xenris.liquidwarsos.net.BluetoothPermissions;
import com.xenris.liquidwarsos.net.BluetoothTransport;
import java.util.ArrayList;
import java.util.Set;

public final class BluetoothMenuActivity extends LocalizedActivity {
    private enum Action { REFRESH, SCAN, HOST, JOIN }
    private static final int PERMISSION_REQUEST = 71, ENABLE_REQUEST = 72;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final BluetoothPeers directory = new BluetoothPeers();
    private final ArrayList<BluetoothPeers.Peer> peers = new ArrayList<>();
    private BluetoothAdapter bluetooth;
    private ArrayAdapter<BluetoothPeers.Peer> adapter;
    private TextView status;
    private Action pending;
    private BluetoothPeers.Peer pendingPeer;
    private boolean active, registered, permissionPending, enablePending, ownScan, joining;

    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) {
            if (!active || isFinishing()) return;
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action) || BluetoothDevice.ACTION_BOND_STATE_CHANGED.equals(action)) {
                BluetoothDevice device = Build.VERSION.SDK_INT >= 33
                        ? intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice.class)
                        : intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                addDevice(device);
                renderPeers();
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                ownScan = false;
                handler.removeCallbacksAndMessages(null);
                updateStatus();
            } else if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)) {
                refreshKnown();
            }
        }
    };

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.bluetooth_menu);
        status = findViewById(R.id.bt_status);
        bluetooth = BluetoothAccess.adapter(this);
        adapter = new ArrayAdapter<BluetoothPeers.Peer>(this, R.layout.lan_room_row, android.R.id.text1, peers) {
            @Override public View getView(int position, View reusable, ViewGroup parent) {
                View row = super.getView(position, reusable, parent);
                BluetoothPeers.Peer peer = getItem(position);
                ((TextView)row.findViewById(android.R.id.text1)).setText(peer.name);
                ((TextView)row.findViewById(android.R.id.text2)).setText(getString(
                        peer.paired ? R.string.bt_paired_peer : R.string.bt_new_peer, peer.address));
                return row;
            }
        };
        ListView list = findViewById(R.id.bt_devices);
        list.setAdapter(adapter);
        list.setEmptyView(findViewById(R.id.bt_empty));
        list.setOnItemClickListener((parent, view, position, id) -> {
            if (position < 0 || position >= peers.size()) return;
            pendingPeer = peers.get(position);
            requestAction(Action.JOIN);
        });
    }

    @Override public void onResume() {
        super.onResume();
        active = true;
        joining = false;
        registerBluetoothReceiver();
        refreshKnown();
        if (pending != null) continueAction();
    }

    private void registerBluetoothReceiver() {
        if (!active || registered || bluetooth == null
                || !BluetoothAccess.has(this, BluetoothPermissions.Action.CONNECT)) return;
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        filter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        try {
            if (Build.VERSION.SDK_INT >= 33) registerReceiver(receiver, filter, Context.RECEIVER_EXPORTED);
            else registerReceiver(receiver, filter);
            registered = true;
        } catch (RuntimeException denied) { status.setText(R.string.bt_permission_denied); }
    }

    @Override public void onPause() {
        active = false;
        stopScan();
        if (registered) { unregisterReceiver(receiver); registered = false; }
        super.onPause();
    }

    @Override public void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        super.onDestroy();
    }

    public void refreshPaired(View view) { requestAction(Action.REFRESH); }
    public void scanDevices(View view) { requestAction(Action.SCAN); }
    public void createBluetoothRoom(View view) { requestAction(Action.HOST); }
    public void bluetoothSettings(View view) {
        try { startActivity(new Intent(Settings.ACTION_BLUETOOTH_SETTINGS)); }
        catch (RuntimeException unavailable) { status.setText(R.string.bt_unavailable); }
    }
    public void permissionSettings(View view) {
        startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + getPackageName())));
    }

    private BluetoothPermissions.Action permissionAction() {
        if (pending == Action.HOST) return BluetoothPermissions.Action.HOST;
        if (pending == Action.SCAN && Build.VERSION.SDK_INT >= 31) return BluetoothPermissions.Action.SCAN;
        return BluetoothPermissions.Action.CONNECT;
    }

    private void requestAction(Action action) {
        if (!active || joining || permissionPending || enablePending) return;
        pending = action;
        continueAction();
    }

    private void continueAction() {
        if (!active || pending == null || permissionPending || enablePending || joining) return;
        if (bluetooth == null) { pending = null; status.setText(R.string.bt_unavailable); return; }
        String[] missing = BluetoothAccess.missing(this, permissionAction());
        if (missing.length != 0) {
            permissionPending = true;
            ActivityCompat.requestPermissions(this, missing, PERMISSION_REQUEST);
            return;
        }
        try {
            registerBluetoothReceiver();
            if (!bluetooth.isEnabled()) {
                enablePending = true;
                startActivityForResult(new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE), ENABLE_REQUEST);
                return;
            }
            Action action = pending;
            pending = null;
            if (action == Action.REFRESH) { stopScan(); refreshKnown(); }
            else if (action == Action.SCAN) beginScan();
            else if (action == Action.HOST) {
                joining = true;
                stopScan();
                Intent host = new Intent(this, MultiplayerGameSetupActivity.class);
                host.putExtra(BluetoothTransport.EXTRA_MODE, true);
                startActivity(host);
            } else if (action == Action.JOIN && pendingPeer != null) {
                joining = true;
                stopScan();
                Intent client = new Intent(this, ClientGameSetupActivity.class);
                client.putExtra(BluetoothTransport.EXTRA_MODE, true);
                client.putExtra("ip", pendingPeer.address);
                client.putExtra("name", pendingPeer.name);
                startActivity(client);
            }
        } catch (RuntimeException unavailable) {
            enablePending = false;
            pending = null;
            joining = false;
            status.setText(R.string.bt_operation_failed);
        }
    }

    private void beginScan() {
        stopScan();
        if (Build.VERSION.SDK_INT < 31) {
            status.setText(R.string.bt_legacy_pairing);
            bluetoothSettings(null); // No app location permission on API26–30.
            return;
        }
        refreshKnown();
        try {
            ownScan = bluetooth.startDiscovery();
            if (!ownScan) { status.setText(R.string.bt_scan_failed); return; }
            status.setText(R.string.bt_scanning);
            handler.postDelayed(() -> { stopScan(); updateStatus(); }, 20000);
        } catch (SecurityException denied) { status.setText(R.string.bt_permission_denied); }
    }

    private void stopScan() {
        handler.removeCallbacksAndMessages(null);
        if (ownScan && bluetooth != null && BluetoothAccess.has(this, BluetoothPermissions.Action.SCAN)) {
            try { bluetooth.cancelDiscovery(); } catch (SecurityException denied) { }
        }
        ownScan = false;
    }

    private void refreshKnown() {
        directory.clear();
        if (bluetooth != null && BluetoothAccess.has(this, BluetoothPermissions.Action.CONNECT)) {
            try {
                if (bluetooth.isEnabled()) {
                    Set<BluetoothDevice> known = bluetooth.getBondedDevices();
                    if (known != null) for (BluetoothDevice device : known) addDevice(device);
                }
            } catch (SecurityException denied) { }
        }
        renderPeers();
    }

    private void addDevice(BluetoothDevice device) {
        if (device == null || !BluetoothAccess.has(this, BluetoothPermissions.Action.CONNECT)) return;
        try {
            directory.put(new BluetoothPeers.Peer(device.getAddress(), device.getName(),
                    device.getBondState() == BluetoothDevice.BOND_BONDED));
        } catch (SecurityException | IllegalArgumentException unavailable) { }
    }

    private void renderPeers() {
        peers.clear();
        peers.addAll(directory.snapshot());
        adapter.notifyDataSetChanged();
        updateStatus();
    }

    private void updateStatus() {
        if (bluetooth == null) status.setText(R.string.bt_unavailable);
        else if (!BluetoothAccess.has(this, BluetoothPermissions.Action.CONNECT)) status.setText(R.string.bt_permission_hint);
        else {
            try {
                if (!bluetooth.isEnabled()) status.setText(R.string.bt_enable_hint);
                else if (ownScan) status.setText(R.string.bt_scanning);
                else status.setText(getString(R.string.bt_devices_found, peers.size()));
            } catch (SecurityException denied) { status.setText(R.string.bt_permission_denied); }
        }
    }

    @Override public void onRequestPermissionsResult(int request, String[] permissions, int[] grants) {
        super.onRequestPermissionsResult(request, permissions, grants);
        if (request != PERMISSION_REQUEST) return;
        permissionPending = false;
        if (BluetoothAccess.has(this, permissionAction())) {
            if (active) continueAction(); // Otherwise onResume continues the same requested action.
        } else {
            pending = null;
            status.setText(R.string.bt_permission_denied);
        }
    }

    @Override protected void onActivityResult(int request, int result, Intent data) {
        super.onActivityResult(request, result, data);
        if (request != ENABLE_REQUEST) return;
        enablePending = false;
        if (result == RESULT_OK) { if (active) continueAction(); }
        else { pending = null; status.setText(R.string.bt_enable_hint); }
    }
}
