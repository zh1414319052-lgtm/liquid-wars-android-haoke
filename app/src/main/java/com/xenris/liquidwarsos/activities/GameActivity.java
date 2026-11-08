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
import android.os.SystemClock;
import android.widget.TextView;
import android.view.Window;
import android.view.WindowManager;
import android.view.MotionEvent;
import android.view.KeyEvent;
import android.view.Gravity;
import android.content.Context;
import android.content.Intent;
import android.content.DialogInterface;

import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import com.xenris.liquidwarsos.MatchResultDialog;
import com.xenris.liquidwarsos.MyGLSurfaceView;
import com.xenris.liquidwarsos.TouchTargets;
import com.xenris.liquidwarsos.NativeInterface;
import com.xenris.liquidwarsos.R;
import com.xenris.liquidwarsos.StaticBits;
import com.xenris.liquidwarsos.Util;

public class GameActivity extends LocalizedActivity implements Runnable, MyGLSurfaceView.SurfaceCallbacks {
    private MyGLSurfaceView myGLSurfaceView = null;
    private volatile boolean running;
    private volatile boolean paused = true;
    private Thread gameThread;
    private boolean resumed;
    private boolean windowFocused;
    private final Object clockLock = new Object();
    private long elapsedPlayMillis;
    private long resumedAt;
    private short[][] xs = new short[6][5];
    private short[][] ys = new short[6][5];
    private Context context;
    private volatile boolean gameFinished;
    private volatile boolean lostGame;
    private AlertDialog dialog;
    private boolean leavingMatch;
    private boolean spectator;
    private int itemEpoch;
    private int aiStartDelay=6;
    private com.xenris.liquidwarsos.MatchIntroView intro;
    private volatile boolean introDone;
    private long introStartedActive=-1;
    private com.xenris.liquidwarsos.cloud.ModelGameSupport models;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Window window = getWindow();
        WindowInsetsControllerCompat windowInsetsController =
                WindowCompat.getInsetsController(window, window.getDecorView());
        // Hide the system bars.
        windowInsetsController.hide(WindowInsetsCompat.Type.systemBars());
        super.onCreate(savedInstanceState);

        context = this;
        spectator=getIntent().getBooleanExtra("spectator",false);

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        final int fullscreen = WindowManager.LayoutParams.FLAG_FULLSCREEN;
        final int keepOn = WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON;
        getWindow().addFlags(fullscreen | keepOn);

        setContentView(R.layout.game);
        com.xenris.liquidwarsos.GameWindow.apply(this);
        myGLSurfaceView = (MyGLSurfaceView) findViewById(R.id.mySurfaceView);
        myGLSurfaceView.setSurfaceCallbacks(this);

        NativeInterface.init(getAssets());
        NativeInterface.createGame(StaticBits.team, StaticBits.map, StaticBits.seed, StaticBits.dotsPerTeam);
        Util.loadPlayerInitialPositions(xs, ys);
        NativeInterface.configureAlliances(StaticBits.alliances);

        models=new com.xenris.liquidwarsos.cloud.ModelGameSupport(this,spectator,spectator?0x3f:(0x3f & ~(1<<StaticBits.team)));
        intro=findViewById(R.id.match_intro);
        intro.bind(StaticBits.team,spectator,NativeInterface.initialPositions());
        running = true;
        gameThread = new Thread(this, "Solo match");
        gameThread.start();
        if((getApplicationInfo().flags&android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE)!=0
                &&getIntent().getBooleanExtra("qa_ruins_ready",false))myGLSurfaceView.post(this::showRuinsReady);
    }

    private void showRuinsReady(){
        if(!running||isFinishing()||isDestroyed())return;
        android.view.View content=getLayoutInflater().inflate(R.layout.ruins_trial_ready,null);
        android.graphics.Typeface font=android.graphics.Typeface.createFromAsset(getAssets(),"fonts/NotoSansSC-Intro.ttf");
        for(int id:new int[]{R.id.ruins_ready_title,R.id.ruins_ready_message,R.id.ruins_start,R.id.ruins_exit}){
            TextView text=content.findViewById(id);text.setTypeface(font);
            text.setFontVariationSettings(id==R.id.ruins_ready_message?"'wght' 400":"'wght' 600");
        }
        dialog=new AlertDialog.Builder(this).setView(content).setCancelable(false).create();
        dialog.setCanceledOnTouchOutside(false);
        content.findViewById(R.id.ruins_start).setOnClickListener(v->{if(dialog!=null){dialog.dismiss();dialog=null;}});
        content.findViewById(R.id.ruins_exit).setOnClickListener(v->finish());
        dialog.show();
    }

    @Override
    public void onPause() {
        super.onPause();
        com.xenris.liquidwarsos.ItemOverlay controls=findViewById(R.id.item_overlay);if(controls!=null)controls.releasePointerGesture();
        resumed = false;
        if(models!=null)models.pause(true);
        updatePauseState();
        if (myGLSurfaceView != null)
            myGLSurfaceView.onPause();
        if (isFinishing()) {
            stopMatch();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (myGLSurfaceView != null)
            myGLSurfaceView.onResume();
        resumed = true;
        if(models!=null)models.pause(false);
        updatePauseState();
    }

    @Override
    public void onDestroy() {
        if (!isFinishing())
            finish();
        stopMatch();
        if (dialog != null)
            dialog.dismiss();
        super.onDestroy();
    }

    private void stopMatch() {
        running = false;
        if(models!=null)models.dispose();
        if (gameThread == null) {
            return;
        }
        // Finish native teardown before the setup screen can launch the next match.
        boolean interrupted = false;
        while (gameThread.isAlive()) {
            try {
                gameThread.join();
            } catch (InterruptedException exception) {
                interrupted = true;
            }
        }
        gameThread = null;
        if (interrupted) {
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public void run() {
        aiStartDelay = 6;
        aiController = new com.xenris.liquidwarsos.AiController(StaticBits.aiDifficulty);
        while (running) {
            if (!paused && !gameFinished) {
                if(!introDone){
                    synchronized(xs){models.beforeInputs(aiController,xs,ys);}
                    if(introStartedActive<0)introStartedActive=elapsedActiveMillis();
                    long warmup=com.xenris.liquidwarsos.MatchIntro.DURATION_MS-(elapsedActiveMillis()-introStartedActive);
                    if(warmup>0){intro.showMillis(warmup);SystemClock.sleep(20);continue;}
                    introDone=true;intro.showMillis(0);models.beginPlay();
                }
                stepGame();
                syncRewind();
                final int timeDiff = elapsedPlaySeconds();
                NativeInterface.setTimeSidebar((float) timeDiff / StaticBits.timeLimit);
                checkTimeout(timeDiff);
                checkForWinner();
                checkIfLost();
                if(gameFinished)models.close();
                if (NativeInterface.playTimeMillis()>0 && aiStartDelay-- < 0)
                    updateAI();
            } else {
                // Only UI lifecycle/focus callbacks own API pause state. A late worker
                // pause here could overwrite onResume and leave requests paused forever.
                try {
                    Thread.sleep(100);
                } catch (InterruptedException ie) {
                }
            }
        }
        NativeInterface.destroyGame();
        NativeInterface.uninit();
    }

    private void checkTimeout(int timeDiff) {
        if (timeDiff >= StaticBits.timeLimit && !gameFinished) {
            gameFinished = true;
            int winningTeam = 0;
            for (int i = 0; i < 6; i++) {
                if (com.xenris.liquidwarsos.AllianceRules.score(i) > com.xenris.liquidwarsos.AllianceRules.score(winningTeam))
                    winningTeam = i;
            }
            showResult(!spectator && com.xenris.liquidwarsos.AllianceRules.allied(winningTeam,StaticBits.team) ? getString(R.string.timeout_win)
                    : getString(R.string.timeout_other_win, com.xenris.liquidwarsos.AllianceSettings.name(this, winningTeam)), false);
        }
    }

    private void checkIfLost() {
        if (spectator || gameFinished || lostGame) return;
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
                showResult(!spectator && com.xenris.liquidwarsos.AllianceRules.allied(i,StaticBits.team) ? getString(R.string.result_win)
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
            dialog = MatchResultDialog.show(this, message, R.string.play_again, this::replayMatch,
                    R.string.exit_match, () -> {
                        if (leavingMatch) return;
                        leavingMatch = true;
                        finish();
                    }, spectating ? () -> {} : null);
        });
    }

    private void replayMatch() {
        if (leavingMatch || !running || isFinishing()) return;
        leavingMatch = true;
        // Stop drawing and finish native teardown before another Activity creates state.
        if (myGLSurfaceView != null) myGLSurfaceView.onPause();
        stopMatch();
        StaticBits.newSeed();
        Intent next = new Intent(this, GameActivity.class);
        next.putExtra("spectator",spectator);
        // Keep explicit debug QA guards across the real replay button. Normal
        // launches have no such extras; stored player/API settings never change.
        if((getApplicationInfo().flags&android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE)!=0){
            if(getIntent().hasExtra("qa_model_teams"))next.putExtra("qa_model_teams",getIntent().getIntExtra("qa_model_teams",0));
            if(getIntent().getBooleanExtra("qa_paid_budget",false))next.putExtra("qa_paid_budget",true).putExtra("qa_budget_ceiling_micros",getIntent().getLongExtra("qa_budget_ceiling_micros",9000000L));
        }
        finish();
        startActivity(next);
    }

    private void updateAI() {
        if(gameFinished){models.close();return;}
        int mask=spectator?0x3f:(0x3f & ~(1<<StaticBits.team));
        aiController.update(xs, ys, mask);
    }

    private com.xenris.liquidwarsos.AiController aiController;

    private void syncRewind(){int[] d=NativeInterface.itemSnapshot();if(d==null||d.length<209||d[146]==itemEpoch)return;itemEpoch=d[146];lostGame=false;aiStartDelay=6;aiController=new com.xenris.liquidwarsos.AiController(StaticBits.aiDifficulty);synchronized(xs){Util.loadPlayerInitialPositions(xs,ys);}runOnUiThread(()->{com.xenris.liquidwarsos.ItemOverlay v=findViewById(R.id.item_overlay);if(v!=null)v.resetControl();if(dialog!=null){dialog.dismiss();dialog=null;}});}

    private void stepGame() {
        setPlayerPositions();
        for (int i = 0; i < 10; i++) {
            long previousTime = System.nanoTime();
            NativeInterface.stepDots();
            Util.regulateSpeed(previousTime, StaticBits.GAME_SPEED);
        }
    }

    private void setPlayerPositions() {
        synchronized (xs) {
            models.beforeInputs(aiController,xs,ys);
            for (int i = 0; i < 6; i++) {
                NativeInterface.setPlayerPosition(i, xs[i], ys[i]);
            }
            models.didApplyInputs();
        }
    }

    @Override public void onControls(short[] x,short[] y){if(spectator||!running||gameFinished)return;synchronized(xs){System.arraycopy(x,0,xs[StaticBits.team],0,5);System.arraycopy(y,0,ys[StaticBits.team],0,5);}}

    @Override
    public void onTouch(MotionEvent event) {
        if (spectator || !running || gameFinished) return;
        synchronized (xs) {
            int team = StaticBits.team;
            TouchTargets.update(event, myGLSurfaceView.getWidth(), myGLSurfaceView.getHeight(), xs[team], ys[team],myGLSurfaceView.camera);((com.xenris.liquidwarsos.ItemOverlay)findViewById(R.id.item_overlay)).noteTargets(xs[team],ys[team]);
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if ((keyCode == KeyEvent.KEYCODE_BACK) && !gameFinished && !lostGame) {
            DialogInterface.OnClickListener clicker = new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    finish();
                }
            };
            if (dialog != null)
                dialog.dismiss();
            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setMessage(R.string.leave_match);
            builder.setPositiveButton(R.string.leave, clicker);
            builder.setNegativeButton(R.string.keep_playing, null);
            dialog = builder.show();
            TextView messageText = (TextView) dialog.findViewById(android.R.id.message);
            messageText.setGravity(Gravity.CENTER);
            return true;
        } else {
            return super.onKeyDown(keyCode, event);
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        windowFocused = hasFocus;
        updatePauseState();
    }

    private void updatePauseState() {
        boolean shouldPause = !resumed || !windowFocused;
        if(models!=null)models.pause(shouldPause);
        synchronized (clockLock) {
            if (paused == shouldPause) {
                return;
            }
            long now = SystemClock.elapsedRealtime();
            if (shouldPause) {
                elapsedPlayMillis += now - resumedAt;
            } else {
                resumedAt = now;
            }
            paused = shouldPause;
        }
    }

    private int elapsedPlaySeconds() { return NativeInterface.playTimeMillis()/1000; }

    private long elapsedActiveMillis() {
        synchronized (clockLock) {
            long activeMillis = paused ? 0 : SystemClock.elapsedRealtime() - resumedAt;
            return elapsedPlayMillis + activeMillis;
        }
    }
}
