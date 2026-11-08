// SPDX-License-Identifier: GPL-3.0-or-later
package com.xenris.liquidwarsos.server;

import android.content.Context;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Log;

import com.xenris.liquidwarsos.StaticBits;

import java.io.Closeable;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/** Android's DNS-SD implementation. No third-party runtime or extra permissions. */
public final class NsdRoomDiscovery {
    public static final String TYPE = "_liquidwars._tcp.";
    private static final String TAG = "LiquidWarsLAN";
    private NsdRoomDiscovery() {}

    public static final class Publisher implements Closeable {
        private final NsdManager manager;
        private volatile boolean requested;
        private volatile boolean closed;
        private final NsdManager.RegistrationListener listener = new NsdManager.RegistrationListener() {
            @Override public void onServiceRegistered(NsdServiceInfo info) {
                if (closed) unregister(); // Cancellation may precede asynchronous registration.
            }
            @Override public void onRegistrationFailed(NsdServiceInfo info, int error) {
                requested = false;
                Log.w(TAG, "NSD registration failed: " + error);
            }
            @Override public void onServiceUnregistered(NsdServiceInfo info) { requested = false; }
            @Override public void onUnregistrationFailed(NsdServiceInfo info, int error) {
                Log.w(TAG, "NSD unregister failed: " + error);
            }
        };

        public Publisher(Context context, int gamePort, String name) {
            manager = (NsdManager) context.getApplicationContext().getSystemService(Context.NSD_SERVICE);
            if (manager == null) return;
            NsdServiceInfo service = new NsdServiceInfo();
            service.setServiceName(LanRoom.cleanName(name));
            service.setServiceType(TYPE);
            service.setPort(gamePort);
            service.setAttribute("app", "liquidwarsos");
            service.setAttribute("v", Integer.toString(StaticBits.VERSION_CODE));
            try {
                requested = true;
                manager.registerService(service, NsdManager.PROTOCOL_DNS_SD, listener);
            } catch (RuntimeException unavailable) {
                requested = false;
                Log.w(TAG, "NSD registration unavailable", unavailable);
            }
        }

        private void unregister() {
            if (manager != null && requested) {
                try { manager.unregisterService(listener); }
                catch (RuntimeException pendingOrStopped) { /* onRegistered retries after a late start. */ }
            }
        }
        @Override public void close() { closed = true; unregister(); }
    }

    public static final class Browser implements Closeable {
        private final Handler handler = new Handler(Looper.getMainLooper());
        private final NsdManager manager;
        private final RoomDirectory directory = new RoomDirectory();
        private final Map<String, NsdServiceInfo> seen = new HashMap<>();
        private final ArrayDeque<NsdServiceInfo> queue = new ArrayDeque<>();
        private Consumer<List<LanRoom>> callback;
        private List<LanRoom> previous = new ArrayList<>();
        private volatile boolean closed;
        private boolean requested;
        private boolean stalled;
        private NsdServiceInfo resolving;
        private NsdManager.ResolveListener resolver;
        private final Runnable resolveTimeout = () -> {
            if (closed || resolving == null) return;
            Log.w(TAG, "NSD resolution timed out; UDP remains active until refresh");
            stalled = true; // Never issue overlapping legacy resolve requests.
            cancelResolution();
            resolving = null;
            resolver = null;
            queue.clear();
        };
        private final Runnable tick = new Runnable() {
            @Override public void run() {
                if (closed) return;
                emit();
                handler.postDelayed(this, 1000);
            }
        };
        private final NsdManager.DiscoveryListener listener = new NsdManager.DiscoveryListener() {
            @Override public void onDiscoveryStarted(String type) {
                handler.post(() -> { if (closed) stopNsd(); });
            }
            @Override public void onStartDiscoveryFailed(String type, int error) {
                handler.post(() -> {
                    requested = false;
                    Log.w(TAG, "NSD discovery failed: " + error);
                });
            }
            @Override public void onDiscoveryStopped(String type) { }
            @Override public void onStopDiscoveryFailed(String type, int error) {
                Log.w(TAG, "NSD stop failed: " + error);
            }
            @Override public void onServiceFound(NsdServiceInfo service) {
                handler.post(() -> {
                    if (closed || stalled || !isType(service.getServiceType())) return;
                    String key = service.getServiceName();
                    if (seen.containsKey(key) || seen.size() >= 64) return;
                    seen.put(key, service);
                    queue.add(service);
                    resolveNext();
                });
            }
            @Override public void onServiceLost(NsdServiceInfo service) {
                handler.post(() -> {
                    if (closed) return;
                    seen.remove(service.getServiceName());
                    directory.lostNsd(service.getServiceName());
                    emit();
                });
            }
        };

        public Browser(Context context, Consumer<List<LanRoom>> callback) {
            this.callback = callback;
            manager = (NsdManager) context.getApplicationContext().getSystemService(Context.NSD_SERVICE);
            if (manager != null) {
                try {
                    requested = true;
                    manager.discoverServices(TYPE, NsdManager.PROTOCOL_DNS_SD, listener);
                } catch (RuntimeException unavailable) {
                    requested = false;
                    Log.w(TAG, "NSD discovery unavailable", unavailable);
                }
            }
            handler.post(tick);
        }

        private static boolean isType(String type) {
            return TYPE.equals(type) || "_liquidwars._tcp".equals(type);
        }

        private void resolveNext() {
            if (closed || stalled || resolving != null || manager == null) return;
            NsdServiceInfo next;
            do {
                next = queue.poll();
                if (next == null) return;
            } while (seen.get(next.getServiceName()) != next);
            final NsdServiceInfo candidate = next;
            resolving = candidate;
            resolver = new NsdManager.ResolveListener() {
                @Override public void onResolveFailed(NsdServiceInfo info, int error) {
                    handler.post(() -> complete(candidate, null));
                }
                @Override public void onServiceResolved(NsdServiceInfo info) {
                    handler.post(() -> complete(candidate, info));
                }
            };
            handler.postDelayed(resolveTimeout, 5000);
            try { manager.resolveService(candidate, resolver); }
            catch (RuntimeException unavailable) { complete(candidate, null); }
        }

        private void complete(NsdServiceInfo candidate, NsdServiceInfo info) {
            if (closed || resolving != candidate) return;
            handler.removeCallbacks(resolveTimeout);
            if (info != null && seen.get(candidate.getServiceName()) == candidate) {
                InetAddress host = info.getHost();
                Map<String, byte[]> attributes = info.getAttributes();
                String app = value(attributes.get("app"));
                String version = value(attributes.get("v"));
                if (host != null && !host.isAnyLocalAddress() && !host.isLoopbackAddress()
                        && !host.isMulticastAddress() && info.getPort() > 0 && info.getPort() <= 65535
                        && "liquidwarsos".equals(app)
                        && Integer.toString(StaticBits.VERSION_CODE).equals(version)) {
                    directory.foundNsd(candidate.getServiceName(), new LanRoom(info.getServiceName(),
                            host.getHostAddress(), info.getPort()));
                    emit();
                }
            }
            resolving = null;
            resolver = null;
            handler.postDelayed(this::resolveNext, 100);
        }

        private static String value(byte[] value) {
            return value == null ? "" : new String(value, StandardCharsets.UTF_8);
        }

        public void acceptUdp(LanRoom room) {
            handler.post(() -> {
                if (closed) return;
                directory.foundUdp(room, SystemClock.elapsedRealtime());
                emit();
            });
        }

        private void emit() {
            List<LanRoom> rooms = directory.snapshot(SystemClock.elapsedRealtime());
            if (!rooms.equals(previous)) {
                previous = rooms;
                if (callback != null) callback.accept(new ArrayList<>(rooms));
            }
        }

        private void stopNsd() {
            if (manager != null && requested) {
                try { manager.stopServiceDiscovery(listener); }
                catch (RuntimeException pendingOrStopped) { /* A late start callback retries cancellation. */ }
            }
        }

        private void cancelResolution() {
            if (Build.VERSION.SDK_INT >= 34 && manager != null && resolver != null) {
                try { manager.stopServiceResolution(resolver); }
                catch (RuntimeException alreadyFinished) { }
            }
        }

        @Override public void close() {
            closed = true;
            callback = null;
            handler.removeCallbacksAndMessages(null);
            stopNsd();
            cancelResolution();
            seen.clear();
            queue.clear();
        }
    }
}
