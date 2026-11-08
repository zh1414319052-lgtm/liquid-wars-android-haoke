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
import android.view.Window;
import android.view.WindowManager;
import android.view.MotionEvent;
import android.view.KeyEvent;
import android.view.Gravity;
import android.widget.TextView;
import android.widget.Toast;
import android.content.Context;
import android.content.DialogInterface;


import com.xenris.liquidwarsos.Client;
import com.xenris.liquidwarsos.MatchResultDialog;
import com.xenris.liquidwarsos.MyGLSurfaceView;
import com.xenris.liquidwarsos.TouchTargets;
import com.xenris.liquidwarsos.NativeInterface;
import com.xenris.liquidwarsos.R;
import com.xenris.liquidwarsos.StaticBits;
import com.xenris.liquidwarsos.Util;

import java.lang.Thread;
import java.util.ArrayList;

public class GameClientActivity extends LocalizedActivity implements Client.ClientCallbacks, Runnable, MyGLSurfaceView.SurfaceCallbacks {
    private MyGLSurfaceView myGLSurfaceView = null;
    private volatile boolean running;
    private int itemEpoch;
    private int gameStep = -1;
    private short[] xs = new short[5];
    private short[] ys = new short[5];
    private Context context;
    private final Object nativeStateLock = new Object();
    private volatile boolean gameFinished;
    private volatile boolean lostGame;
    private AlertDialog dialog;
    private boolean leavingMatch;
    private boolean leaveRoom;
    private com.xenris.liquidwarsos.MatchIntroView intro;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        context = this;

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        final int fullscreen = WindowManager.LayoutParams.FLAG_FULLSCREEN;
        final int keepOn = WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON;
        getWindow().addFlags(fullscreen | keepOn);

        setContentView(R.layout.game);
        com.xenris.liquidwarsos.GameWindow.apply(this);
        myGLSurfaceView = (MyGLSurfaceView)findViewById(R.id.mySurfaceView);
        myGLSurfaceView.setSurfaceCallbacks(this);

        NativeInterface.init(getAssets());
        NativeInterface.createGame(StaticBits.team, StaticBits.map, StaticBits.seed, StaticBits.dotsPerTeam);
        short[] starts=NativeInterface.initialPositions();
        java.util.Arrays.fill(xs,(short)-1);java.util.Arrays.fill(ys,(short)-1);
        xs[0]=starts[StaticBits.team*2];ys[0]=starts[StaticBits.team*2+1];
        NativeInterface.configureAlliances(StaticBits.alliances);
        intro=findViewById(R.id.match_intro);
        intro.bind(StaticBits.team,false,starts);
        if(!java.util.Arrays.equals(StaticBits.alliances,com.xenris.liquidwarsos.AllianceRules.solo())){android.widget.TextView rules=findViewById(R.id.model_status);rules.setVisibility(android.view.View.VISIBLE);rules.setText(com.xenris.liquidwarsos.AllianceRules.label(StaticBits.alliances));}

        StaticBits.client.setCallbacks(this);

        running = true;
        new Thread(this).start();
    }

    @Override
    public void onPause() {
        super.onPause();
        com.xenris.liquidwarsos.ItemOverlay controls=findViewById(R.id.item_overlay);if(controls!=null)controls.releasePointerGesture();
        if(myGLSurfaceView != null)
            myGLSurfaceView.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
        if(myGLSurfaceView != null)
            myGLSurfaceView.onResume();
    }

    @Override
    public void onDestroy() {
        if(!isFinishing())
            finish();
        super.onDestroy();
        if(myGLSurfaceView != null)
            myGLSurfaceView.onPause();
        running = false;
        if(dialog != null)
            dialog.dismiss();
    }

    @Override
    public void run() {
        gameFinished = false;
        lostGame = false;
        StaticBits.client.send(StaticBits.CLIENT_READY, 0);
        while(running) {
            StaticBits.client.send(StaticBits.CLIENT_CURRENT_GAMESTEP, gameStep);
            synchronized (nativeStateLock) {
                checkForWinner();
                checkIfLost();
            }
            try { Thread.sleep(50); } catch (InterruptedException ie) { }
        }
        if (!StaticBits.gameWasDisconnected && StaticBits.client != null)
            StaticBits.client.send(StaticBits.CLIENT_EXIT, 0);
        runOnUiThread(() -> {
            // Stop GL and drain an in-flight network step before destroying native state.
            if (myGLSurfaceView != null) myGLSurfaceView.onPause();
            synchronized (nativeStateLock) {
                NativeInterface.destroyGame();
                NativeInterface.uninit();
            }
            if (StaticBits.client != null)
                StaticBits.client.setCallbacks(StaticBits.clientGameSetupActivity);
            if (leaveRoom && StaticBits.clientGameSetupActivity != null)
                StaticBits.clientGameSetupActivity.finish();
            if (!isFinishing()) finish();
        });
    }

    private void checkIfLost() {
        if (gameFinished || lostGame) return;
        if (com.xenris.liquidwarsos.AllianceRules.score(StaticBits.team) == 0) {
            lostGame = true;
            showResult(getString(R.string.result_loss), true);
        }
    }

    private void checkForWinner() {
        if (gameFinished) return;
        for (int i = 0; i < 6; i++) {
            if (NativeInterface.totalParticles() > 0 && com.xenris.liquidwarsos.AllianceRules.score(i) == NativeInterface.totalParticles()) {
                gameFinished = true;
                showResult(com.xenris.liquidwarsos.AllianceRules.allied(i,StaticBits.team) ? getString(R.string.result_win)
                        : getString(R.string.result_team_win, com.xenris.liquidwarsos.AllianceSettings.name(this, i)), false);
                break;
            }
        }
    }


    private void showResult(final CharSequence message, final boolean spectating) {
        runOnUiThread(() -> {
            if (!running || isFinishing() || isDestroyed() || leavingMatch
                    || (spectating && gameFinished)) return;
            if (dialog != null) dialog.dismiss();
            CharSequence detail = message + "\n\n" + getString(R.string.room_next_match_hint);
            dialog = MatchResultDialog.show(this, detail, R.string.return_to_room,
                    () -> leaveMatch(false), R.string.exit_room, () -> leaveMatch(true),
                    spectating ? () -> {} : null);
        });
    }

    private void leaveMatch(boolean exitRoom) {
        if (leavingMatch || isFinishing() || isDestroyed()) return;
        leavingMatch = true;
        leaveRoom = exitRoom;
        running = false;
        // The worker tears native state down before restoring the room and finishing.
    }

    private void sendPlayerData() {
        int[] data = new int[5 + 5 + 1];
        synchronized(xs){for(int i = 0; i < 5; i++) {
            data[i+1] = xs[i];
            data[i+5+1] = ys[i];
        }}
        data[0] = StaticBits.PLAYER_POSITION_DATA;
        StaticBits.client.send(data.length, data);
    }

    @Override public void onControls(short[] x,short[] y){if(!running||gameFinished)return;synchronized(xs){System.arraycopy(x,0,xs,0,5);System.arraycopy(y,0,ys,0,5);}sendPlayerData();}

    @Override
    public void onTouch(MotionEvent event) {
        if (!running || gameFinished) return;
        synchronized(xs){TouchTargets.update(event, myGLSurfaceView.getWidth(), myGLSurfaceView.getHeight(), xs, ys,myGLSurfaceView.camera);((com.xenris.liquidwarsos.ItemOverlay)findViewById(R.id.item_overlay)).noteTargets(xs,ys);}
        sendPlayerData();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if((keyCode == KeyEvent.KEYCODE_BACK) && !gameFinished) {
            DialogInterface.OnClickListener clicker = new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    leaveMatch(false);
                }
            };
            if(dialog != null)
                dialog.dismiss();
            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setMessage(R.string.leave_match);
            builder.setPositiveButton(R.string.leave, clicker);
            builder.setNegativeButton(R.string.keep_playing, null);
            dialog = builder.show();
            TextView messageText = (TextView)dialog.findViewById(android.R.id.message);
            messageText.setGravity(Gravity.CENTER);
            return true;
        }
        else if (keyCode == KeyEvent.KEYCODE_BACK) {
            leaveMatch(false);
            return true;
        } else {
            return super.onKeyDown(keyCode, event);
        }
    }

    @Override
    public void onServerMessageReceived(int argc, int[] args) {
        synchronized (nativeStateLock) {
            if (!running) return;
            if(args[0] == StaticBits.STEP_GAME) {
                if(running) {
                    if(args[2] == (gameStep + 1)) {
                        if(!com.xenris.liquidwarsos.MatchIntro.warming(args[2]))loadPlayerPositions(args, 3);
                        stepGame(args[1]);
                    } else if(args[2] > (gameStep + 1)) {
                        StaticBits.client.send(StaticBits.RESEND_STEPS, gameStep + 1);
                    }
                }
            } else if(args[0] == StaticBits.CLIENT_READY_QUERY) {
                if(running)
                    StaticBits.client.send(StaticBits.CLIENT_READY, 0);
            } else if(args[0] == StaticBits.KILL_GAME) {
                ToastOnUiThread(getString(R.string.game_cancelled));
                running = false;
            } else if(args[0] == StaticBits.BACK_TO_MENU) {
                running = false;
            } else if(args[0] == StaticBits.OUT_OF_TIME) {
                outOfTimeMessage(args[1]);
            } else if(args[0] == StaticBits.TIME_DIFF) {
                NativeInterface.setTimeSidebar((float)args[1]/(float)StaticBits.timeLimit);
            }
        }
    }

    @Override
    public void onServerConnectionMade(int id, String ip) {
    }

    @Override
    public void onServerConnectionFailed(String ip) {
        StaticBits.gameWasDisconnected = true;
        ToastOnUiThread(getString(R.string.connection_lost));
        running = false;
    }

    @Override
    public void onServerConnectionClosed(String ip) {
        StaticBits.gameWasDisconnected = true;
        ToastOnUiThread(getString(R.string.connection_lost));
        running = false;
    }

    private void ToastOnUiThread(final String s) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(context, s, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void outOfTimeMessage(final int winningTeam) {
        if (gameFinished) return;
        gameFinished = true;
        showResult(com.xenris.liquidwarsos.AllianceRules.allied(winningTeam,StaticBits.team) ? getString(R.string.timeout_win)
                : getString(R.string.timeout_other_win, com.xenris.liquidwarsos.AllianceSettings.name(this, winningTeam)), false);
    }

    private void loadPlayerPositions(int[] data, int offset) {
        short[] tempxs = new short[5];
        short[] tempys = new short[5];
        for(int p = 0; p < 6; p++) {
            for(int xy = 0; xy < 5; xy++) {
                tempxs[xy] = (short)data[offset++];
                tempys[xy] = (short)data[offset++];
            }
            NativeInterface.setPlayerPosition(p, tempxs, tempys);
        }
    }

    private void syncRewind(){int[] d=NativeInterface.itemSnapshot();if(d==null||d.length<209||d[146]==itemEpoch)return;itemEpoch=d[146];lostGame=false;short[] b=NativeInterface.initialPositions();synchronized(xs){java.util.Arrays.fill(xs,(short)-1);java.util.Arrays.fill(ys,(short)-1);xs[0]=b[StaticBits.team*2];ys[0]=b[StaticBits.team*2+1];}sendPlayerData();runOnUiThread(()->{com.xenris.liquidwarsos.ItemOverlay v=findViewById(R.id.item_overlay);if(v!=null)v.resetControl();if(dialog!=null){dialog.dismiss();dialog=null;}});}

    private void stepGame(int speed) {
        if(com.xenris.liquidwarsos.MatchIntro.warming(gameStep+1)){
            intro.showMillis(com.xenris.liquidwarsos.MatchIntro.remaining(gameStep+1));
            if(speed==StaticBits.REGULATED_STEP)for(int i=0;i<10;i++){long before=System.nanoTime();Util.regulateSpeed(before,StaticBits.GAME_SPEED);}
            gameStep++;return;
        }
        intro.showMillis(0);
        for(int i = 0; i < 10; i++) {
            long previousTime = System.nanoTime();
            NativeInterface.stepDots();
            if(speed == StaticBits.REGULATED_STEP)
                Util.regulateSpeed(previousTime, StaticBits.GAME_SPEED);
        }
        gameStep++;
        syncRewind();
    }
}
