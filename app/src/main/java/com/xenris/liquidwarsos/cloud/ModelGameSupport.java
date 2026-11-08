// SPDX-License-Identifier: GPL-3.0-or-later
package com.xenris.liquidwarsos.cloud;
import android.app.Activity;
import android.app.AlertDialog;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.TextView;
import com.xenris.liquidwarsos.*;

public final class ModelGameSupport {
    private final ModelMatch match;
    private final TextView status;
    private final Handler ui=new Handler(Looper.getMainLooper());
    private final boolean zh;
    private final boolean spectator;
    private volatile boolean closed;
    private boolean hasTerrain;
    private ModelMatch.Inputs pending;
    private int controlled;
    private long lastObservation;
    private int terrainRevision=-1;private int itemEpoch=-1;
    private long lastTerrainBuild;
    private final java.util.concurrent.ExecutorService terrainWorker=java.util.concurrent.Executors.newSingleThreadExecutor(r->{Thread t=new Thread(r,"Terrain facts");t.setDaemon(true);return t;});
    private java.util.concurrent.Future<ModelTerrain> rebuilding;
    private int rebuildingRevision;
    public ModelGameSupport(Activity a,boolean spectator,int controlled){
        this.spectator=spectator;this.controlled=controlled;
        int qaMask=63;boolean debug=(a.getApplicationInfo().flags&android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE)!=0;
        if(debug&&a.getIntent().hasExtra("qa_model_teams"))qaMask=a.getIntent().getIntExtra("qa_model_teams",0)&63;
        // Pure-local instrumentation must not even load saved provider settings.
        ModelConfig[] configs=qaMask==0?new ModelConfig[6]:ModelStore.all(a);
        for(int p=0;p<6;p++)if((controlled&qaMask&(1<<p))==0)configs[p]=ModelConfig.empty();
        match=new ModelMatch(configs,StaticBits.alliances);status=a.findViewById(R.id.model_status);zh="zh".equals(a.getResources().getConfiguration().getLocales().get(0).getLanguage());
        match.timeLimit(StaticBits.timeLimit*1000L);
        match.prepare();
        if(debug&&a.getIntent().getBooleanExtra("qa_paid_budget",false))try{match.qaBudget(new QaModelBudget(new java.io.File(a.getFilesDir(),"qa-strategy160-budget.json"),"strategy160-after-topup-20261106",Math.min(9000000L,Math.max(100000L,a.getIntent().getLongExtra("qa_budget_ceiling_micros",9000000L))),1200));}catch(java.io.IOException e){throw new IllegalStateException("QA budget ledger unavailable",e);}
        if(match.configuredMask()!=0||spectator||!java.util.Arrays.equals(StaticBits.alliances,AllianceRules.solo())){status.setVisibility(View.VISIBLE);status.setOnClickListener(v->new AlertDialog.Builder(a).setMessage(match.summary(zh)).setPositiveButton(zh?"停止全部 API（本局）":"Stop all APIs for this match",(d,w)->close()).setNegativeButton(android.R.string.cancel,null).show());ui.post(refresh);}
    }
    private final Runnable refresh=new Runnable(){public void run(){if(closed){status.setText(zh?"API 已停止 · 模型队伍已停住":"API stopped · Model teams stopped");return;}String s=match.brief(zh);String local=spectator?(zh?"观战 · 本地 AI":"Spectating · Local AI"):(zh?"本地 AI":"Local AI");status.setText(AllianceRules.label(StaticBits.alliances)+"  "+(s.isEmpty()?local:s)+(zh?"  · 点此管理":"  · Tap to manage"));ui.postDelayed(this,250);}};
    public void update(AiController controller,int controlled){
        this.controlled=controlled;
        long now=android.os.SystemClock.elapsedRealtime();
        if(!closed&&match.configuredMask()!=0&&now-lastObservation>=120){
            int[] item=NativeInterface.itemSnapshot();boolean reset=item!=null&&item.length>208&&itemEpoch>=0&&itemEpoch!=item[146];if(item!=null&&item.length>208)itemEpoch=item[146];
            if(reset){if(rebuilding!=null)rebuilding.cancel(true);rebuilding=null;hasTerrain=false;match.replaceTerrain(new ModelTerrain(NativeInterface.modelTerrain()));}
            match.itemState(item);
            int revision=NativeInterface.terrainRevision();
            if(!hasTerrain){byte[] terrain=NativeInterface.modelTerrain();if(terrain!=null){match.terrain(terrain);hasTerrain=true;terrainRevision=revision;}}
            if(rebuilding!=null&&rebuilding.isDone()){
                try{match.replaceTerrain(rebuilding.get());terrainRevision=rebuildingRevision;}catch(Exception ignored){/* Retry the next immutable terrain copy. */}rebuilding=null;
            }
            // Excavation only removes walls, so previous routes stay traversable.
            // Coalesce new portals at most every2s, off the simulation/UI thread.
            if(hasTerrain&&revision!=terrainRevision&&rebuilding==null&&now-lastTerrainBuild>=2000){
                final byte[] walls=NativeInterface.modelTerrain();if(walls!=null){rebuildingRevision=revision;lastTerrainBuild=now;rebuilding=terrainWorker.submit(()->new ModelTerrain(walls));}
            }

            if(hasTerrain){int[] snapshot=NativeInterface.modelSnapshot();if(snapshot!=null){match.update(controlled,snapshot);lastObservation=now;}}
        }
        match.tickExecution();
        pending=match.inputs();controller.setModelInputs(match.configuredMask()&controlled,pending.mask,pending.points);
    }
    /** Game-thread call before solo JNI submission or host history/wire capture. */
    public void beforeInputs(AiController controller,short[][] xs,short[][] ys){
        if(controller==null)return;update(controller,controlled);pending=match.inputs();controller.setModelInputs(match.configuredMask()&controlled,pending.mask,pending.points);controller.applyModelInputs(xs,ys,controlled);
    }
    public void didApplyInputs(){match.submitted(pending);}
    public void beginPlay(){match.beginPlay();}
    public void pause(boolean pause){match.pause(pause);}
    public void close(){if(closed)return;closed=true;match.close();terrainWorker.shutdownNow();ui.removeCallbacks(refresh);ui.post(refresh);}
    public void dispose(){closed=true;match.close();terrainWorker.shutdownNow();ui.removeCallbacksAndMessages(null);}
}
