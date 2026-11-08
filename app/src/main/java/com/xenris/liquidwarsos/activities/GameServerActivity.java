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
import android.view.MotionEvent;
import android.view.Window;
import android.view.WindowManager;
import android.view.KeyEvent;
import android.view.Gravity;
import android.widget.TextView;
import android.content.Context;
import android.content.DialogInterface;


import com.xenris.liquidwarsos.MatchResultDialog;
import com.xenris.liquidwarsos.MyGLSurfaceView;
import com.xenris.liquidwarsos.TouchTargets;
import com.xenris.liquidwarsos.NativeInterface;
import com.xenris.liquidwarsos.PlayerHistory;
import com.xenris.liquidwarsos.R;
import com.xenris.liquidwarsos.server.Server;
import com.xenris.liquidwarsos.StaticBits;
import com.xenris.liquidwarsos.Util;

import java.lang.Thread;

public class GameServerActivity extends LocalizedActivity implements Server.ServerCallbacks, Runnable, MyGLSurfaceView.SurfaceCallbacks {
    private MyGLSurfaceView myGLSurfaceView = null;
    private volatile boolean running;
    private int itemEpoch;
    private int aiStartDelay=6;
    private int gameStep = 0;
    private int[] clientLags = new int[6];
    private PlayerHistory playerHistory = new PlayerHistory();
    private int playerWithMissedStepsId = -1;
    private int playerWithMissedStepsStep = 0;
    private short[][] xs = new short[6][5];
    private short[][] ys = new short[6][5];
    private boolean[] ready = new boolean[6];
    private Context context;
    private volatile boolean gameFinished;
    private volatile boolean lostGame;
    private volatile boolean frozen;
    private AlertDialog dialog;
    private boolean leavingMatch;
    private boolean leaveRoom;
    private com.xenris.liquidwarsos.cloud.ModelGameSupport models;
    private com.xenris.liquidwarsos.MatchIntroView intro;
    private volatile boolean introPaused=true;
    private boolean introDone;

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

        createReadyList();

        for(int i = 0; i < clientLags.length; i++)
            clientLags[i] = 0;

        NativeInterface.init(getAssets());
        NativeInterface.createGame(StaticBits.team, StaticBits.map, StaticBits.seed, StaticBits.dotsPerTeam);
        Util.loadPlayerInitialPositions(xs, ys);
        NativeInterface.configureAlliances(StaticBits.alliances);

        StaticBits.server.setCallbacks(this);

        int controlled=0;for(int p=0;p<6;p++)if(StaticBits.teams[p]==StaticBits.AI_PLAYER)controlled|=1<<p;
        models=new com.xenris.liquidwarsos.cloud.ModelGameSupport(this,false,controlled);
        intro=findViewById(R.id.match_intro);
        intro.bind(StaticBits.team,false,NativeInterface.initialPositions());
        running = true;
        new Thread(this).start();
    }

    @Override
    public void onPause() {
        super.onPause();
        com.xenris.liquidwarsos.ItemOverlay controls=findViewById(R.id.item_overlay);if(controls!=null)controls.releasePointerGesture();
        introPaused=true;
        if(models!=null)models.pause(true);
        if(myGLSurfaceView != null)
            myGLSurfaceView.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
        introPaused=false;
        if(models!=null)models.pause(false);
        if(myGLSurfaceView != null)
            myGLSurfaceView.onResume();
    }

    @Override
    public void onDestroy() {
        if(!isFinishing())
            finish();
        super.onDestroy();
        if(models!=null)models.dispose();
        running = false;
        if(dialog != null)
            dialog.dismiss();
    }

    @Override
    public void run() {
        gameFinished = false;
        lostGame = false;
        frozen = false;
        waitForEveryoneToBeReady();
        StaticBits.startTimestamp = 0;
        aiStartDelay = 6;
        aiController = new com.xenris.liquidwarsos.AiController(StaticBits.aiDifficulty);
        while(running) {
            if(introPaused&&com.xenris.liquidwarsos.MatchIntro.warming(gameStep)){
                android.os.SystemClock.sleep(25);continue;
            }
            synchronized (xs) {
                models.beforeInputs(aiController,xs,ys);
                playerHistory.savePlayerPositions(xs, ys);
            }
            if(!frozen) {
                boolean warming=com.xenris.liquidwarsos.MatchIntro.warming(gameStep);
                if(!warming&&!introDone){introDone=true;models.beginPlay();StaticBits.startTimestamp=System.currentTimeMillis();}
                sendStepData();
                stepGame();
                if(!warming){
                final int timeDiff = NativeInterface.playTimeMillis()/1000;
                if(!gameFinished) {
                    StaticBits.server.sendToAll(StaticBits.TIME_DIFF, timeDiff);
                    NativeInterface.setTimeSidebar((float)timeDiff/(float)StaticBits.timeLimit);
                }
                checkTimeout(timeDiff);
                checkForWinner();
                checkIfLost();
                if(gameFinished)models.close();
                if(NativeInterface.playTimeMillis()>0 && aiStartDelay-- < 0)
                    updateAI();
                }
                resendAnyLostSteps();
            }
            if (frozen) {
                resendAnyLostSteps();
                try { Thread.sleep(50); } catch (InterruptedException ignored) {}
            }
            waitForSlowClients();
        }
        models.dispose();
        runOnUiThread(() -> {
            // The simulation worker is done; stop GL before releasing shared native state.
            if (myGLSurfaceView != null) myGLSurfaceView.onPause();
            NativeInterface.destroyGame();
            NativeInterface.uninit();
            if (StaticBits.server != null)
                StaticBits.server.setCallbacks(StaticBits.multiplayerGameSetupActivity);
            if (leaveRoom && StaticBits.multiplayerGameSetupActivity != null)
                StaticBits.multiplayerGameSetupActivity.finish();
            if (!isFinishing()) finish();
        });
    }

    private void checkTimeout(int timeDiff) {
        if (timeDiff >= StaticBits.timeLimit && !gameFinished) {
            frozen = true;
            gameFinished = true;
            int winningTeam = 0;
            for (int i = 0; i < 6; i++) {
                if (com.xenris.liquidwarsos.AllianceRules.score(i) > com.xenris.liquidwarsos.AllianceRules.score(winningTeam))
                    winningTeam = i;
            }
            StaticBits.server.sendToAll(StaticBits.OUT_OF_TIME, winningTeam);
            showResult(com.xenris.liquidwarsos.AllianceRules.allied(winningTeam,StaticBits.team) ? getString(R.string.timeout_win)
                    : getString(R.string.timeout_other_win, com.xenris.liquidwarsos.AllianceSettings.name(this, winningTeam)), false);
        }
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
                frozen = true;
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
            CharSequence detail = message + "\n\n" + getString(spectating ? R.string.host_lost_actions_hint : R.string.room_next_match_hint);
            dialog = MatchResultDialog.show(this, detail, R.string.return_to_room,
                    () -> leaveMatch(false), R.string.exit_room, () -> leaveMatch(true),
                    spectating ? () -> {} : null);
        });
    }

    private void leaveMatch(boolean exitRoom) {
        if (leavingMatch || isFinishing() || isDestroyed()) return;
        leavingMatch = true;
        leaveRoom = exitRoom;
        if(models!=null)models.close();
        if (StaticBits.server != null)
            StaticBits.server.sendToAll(gameFinished ? StaticBits.BACK_TO_MENU : StaticBits.KILL_GAME, 0);
        running = false;
        // The worker tears native state down before restoring the room and finishing.
    }

    private void updateAI() {
        if(gameFinished){models.close();return;}
        int mask=0;
        for(int p=0;p<6;p++)if(StaticBits.teams[p]==StaticBits.AI_PLAYER)mask|=1<<p;
        aiController.update(xs,ys,mask);
    }

    private com.xenris.liquidwarsos.AiController aiController;

    private void waitForEveryoneToBeReady() {
        if((StaticBits.server == null) || (ready == null))
            return;

        int countdown = 10;
        while(running) {
            if(ready[0] && ready[1] && ready[2] && ready[3] && ready[4] && ready[5])
                break;
            if(countdown-- < 0) {
                StaticBits.server.sendToAll(StaticBits.CLIENT_READY_QUERY, 0);
                countdown = 10;
            }
            try { Thread.sleep(10); } catch (InterruptedException ie) { }
        }
    }

    private void createReadyList() {
        for(int i = 0; i < 6; i++) {
            if((StaticBits.teams[i] == StaticBits.AI_PLAYER) || (StaticBits.teams[i] == 0))
                ready[i] = true;
            else
                ready[i] = false;
        }

    }

    private void sendStepData() {
        int[] data = new int[3 + 6 * 5 * 2];
        data[0] = StaticBits.STEP_GAME;
        data[1] = StaticBits.REGULATED_STEP;
        data[2] = gameStep;
        playerHistory.serialiseCurrentPlayerState(data, 3);
        StaticBits.server.sendToAll(data.length, data);
    }

    private void resendAnyLostSteps() {
        int[] data = new int[3 + 6 * 5 * 2];
        if(playerWithMissedStepsId == -1)
            return;
        data[0] = StaticBits.STEP_GAME;
        data[1] = StaticBits.FAST_STEP;
        for(int i = playerWithMissedStepsStep; i <= gameStep; i++) {
            data[2] = i;
            playerHistory.serialiseHistoricalPlayerState(gameStep - i, data, 3);
            StaticBits.server.sendToOne(playerWithMissedStepsId, data.length, data);
        }
        playerWithMissedStepsId = -1;
    }

    private void waitForSlowClients() {
        if(playerWithMissedStepsId != -1)
            return;
        int biggestIndex = 0;
        int biggestValue = 0;
        for(int i = 0; i < 6; i++) {
            if(clientLags[i] > biggestValue) {
                biggestIndex = i;
                biggestValue = clientLags[i];
            }
        }
        biggestValue = biggestValue*biggestValue*biggestValue;
        for(int i = 0; i < biggestValue; i++) {
            try { Thread.sleep(1); } catch (InterruptedException ie) { }
            if(!running || clientLags[biggestIndex] < 4)
                break;
        }
    }

    private void syncRewind(){int[] d=NativeInterface.itemSnapshot();if(d==null||d.length<209||d[146]==itemEpoch)return;itemEpoch=d[146];lostGame=false;aiStartDelay=6;aiController=new com.xenris.liquidwarsos.AiController(StaticBits.aiDifficulty);synchronized(xs){Util.loadPlayerInitialPositions(xs,ys);}runOnUiThread(()->{com.xenris.liquidwarsos.ItemOverlay v=findViewById(R.id.item_overlay);if(v!=null)v.resetControl();if(dialog!=null){dialog.dismiss();dialog=null;}});}

    private void stepGame() {
        if(com.xenris.liquidwarsos.MatchIntro.warming(gameStep)){
            intro.showMillis(com.xenris.liquidwarsos.MatchIntro.remaining(gameStep));
            for(int i=0;i<10;i++){long before=System.nanoTime();Util.regulateSpeed(before,StaticBits.GAME_SPEED);}
            gameStep++;return;
        }
        intro.showMillis(0);
        setPlayerPositions();
        for(int i = 0; i < 10; i++) {
            long previousTime = System.nanoTime();
            NativeInterface.stepDots();
            Util.regulateSpeed(previousTime, StaticBits.GAME_SPEED);
        }
        gameStep++;
        syncRewind();
    }

    private void setPlayerPositions() {
        for(int i = 0; i < 6; i++) {
            short[] tempxs = playerHistory.playerX[playerHistory.historyIndex][i];
            short[] tempys = playerHistory.playerY[playerHistory.historyIndex][i];
            NativeInterface.setPlayerPosition(i, tempxs, tempys);
        }
        models.didApplyInputs();
    }

    @Override public void onControls(short[] x,short[] y){if(!running||gameFinished)return;synchronized(xs){int t=clientIdToPlayerNumber(0);if(t>=0){System.arraycopy(x,0,xs[t],0,5);System.arraycopy(y,0,ys[t],0,5);}}}

    @Override
    public void onTouch(MotionEvent event) {
        if (!running || gameFinished) return;
        synchronized (xs) {
            int team = clientIdToPlayerNumber(0);
            if (team >= 0) {
                TouchTargets.update(event, myGLSurfaceView.getWidth(), myGLSurfaceView.getHeight(), xs[team], ys[team],myGLSurfaceView.camera);((com.xenris.liquidwarsos.ItemOverlay)findViewById(R.id.item_overlay)).noteTargets(xs[team],ys[team]);
            }
        }
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
            builder.setMessage(R.string.leave_hosted_match);
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
    public void onClientMessageReceived(int id, int argc, int[] args) {
        if(args[0] == StaticBits.RESEND_STEPS) {
            if(playerWithMissedStepsId == -1)
                noteMissedSteps(id, args[1]);
        } else if(args[0] == StaticBits.PLAYER_POSITION_DATA) {
            final int p = clientIdToPlayerNumber(id);
            if (p < 0) {
                return;
            }
            synchronized (xs) {
                for (int i = 0; i < 5; i++) {
                    xs[p][i] = (short) args[i + 1];
                    ys[p][i] = (short) args[i + 6];
                }
            }
        } else if(args[0] == StaticBits.CLIENT_CURRENT_GAMESTEP) {
            final int p = clientIdToPlayerNumber(id);
            final int clientGameStep = args[1];
            clientLags[p] = gameStep - clientGameStep;
        } else if(args[0] == StaticBits.CLIENT_READY) {
            final int p = clientIdToPlayerNumber(id);
            ready[p] = true;
        } else if(args[0] == StaticBits.CLIENT_EXIT) {
            final int p = clientIdToPlayerNumber(id);
            if((p >= 0) && (p < 6))
                clientLags[p] = 0;
        }
    }

    @Override
    public void onClientConnected(int id) {
    }

    @Override
    public void onClientDisconnected(int id) {
        final int p = clientIdToPlayerNumber(id);
        if((p >= 0) && (p < 6))
            clientLags[p] = 0;
        StaticBits.multiplayerGameSetupActivity.onClientDisconnected(id);
        if (p >= 0 && p < ready.length) ready[p] = true;
    }

    private void noteMissedSteps(int id, int step) {
        playerWithMissedStepsStep = step;
        playerWithMissedStepsId = id;
    }

    private int clientIdToPlayerNumber(int id) {
        for(int i = 0; i < 6; i++)
            if(StaticBits.teams[i] == id)
                return i;
        return -1;
    }
}
