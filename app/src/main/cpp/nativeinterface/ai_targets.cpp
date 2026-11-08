#include "state_guard.hpp"
// SPDX-License-Identifier: GPL-3.0-or-later
#include "ai_targets.hpp"
#include "../application/ai_tactics.hpp"
#include "../application/local_tactics.hpp"
#include "../application/state.hpp"
#include "../application/model_observation.hpp"
#include <jni.h>
#include <memory>
namespace { std::unique_ptr<AiTactics> planner; LocalTactics localPlanner; int plannerRevision=-1; std::array<int,6> plannerGroups{}; }
static std::array<int,6> groups(){std::array<int,6> g;for(int p=0;p<6;p++){g[p]=state->alliances[p];if(state->items->truceUntil>state->items->elapsed&&(g[p]==state->items->truceA||g[p]==state->items->truceB))g[p]=state->items->truceA;}return g;}
void resetAiTargets(){planner.reset();plannerRevision=-1;localPlanner.reset();}
extern "C" jshortArray Java_com_xenris_liquidwarsos_NativeInterface_initialPositions(JNIEnv* env,jclass) {
    std::lock_guard<std::mutex> guard(nativeStateMutex);
    if(!state)return nullptr;
    jshort out[12];for(int p=0;p<6;p++){out[p*2]=state->players[p].x[0];out[p*2+1]=state->players[p].y[0];}
    jshortArray result=env->NewShortArray(12);if(result)env->SetShortArrayRegion(result,0,12,out);return result;
}
extern "C" jintArray Java_com_xenris_liquidwarsos_NativeInterface_modelSnapshot(JNIEnv* env,jclass) {
    std::lock_guard<std::mutex> guard(nativeStateMutex);
    if(!state)return nullptr;auto out=model_observation::snapshot(*state);
    jintArray result=env->NewIntArray(out.size());if(result)env->SetIntArrayRegion(result,0,out.size(),out.data());return result;
}
extern "C" jbyteArray Java_com_xenris_liquidwarsos_NativeInterface_modelTerrain(JNIEnv* env,jclass) {
    std::lock_guard<std::mutex> guard(nativeStateMutex);
    if(!state)return nullptr;auto out=model_observation::terrain(*state);
    jbyteArray result=env->NewByteArray(out.size());if(result)env->SetByteArrayRegion(result,0,out.size(),reinterpret_cast<const jbyte*>(out.data()));return result;
}
static std::array<unsigned,6> hiddenMasks(){std::array<unsigned,6> masks{};for(int p=0;p<6;p++)for(int t=0;t<6;t++)if(state->items->concealedFrom(*state,p,t))masks[p]|=1u<<t;return masks;}
static void ensurePlanner(){if(!planner||plannerRevision!=state->map->revision()||plannerGroups!=groups()){Map* map=state->map;planner.reset(new AiTactics(map->width,map->height,[map](int x,int y){return map->isWall(x,y);},groups()));plannerRevision=map->revision();plannerGroups=groups();}}
extern "C" jboolean Java_com_xenris_liquidwarsos_NativeInterface_updateStrategicTargets(JNIEnv* env,jclass,jshortArray output,jintArray input) {
    std::lock_guard<std::mutex> guard(nativeStateMutex);
    if(!state||!output||!input||env->GetArrayLength(output)!=60||env->GetArrayLength(input)!=12)return JNI_FALSE;
    std::array<int,12> strategy;env->GetIntArrayRegion(input,0,12,strategy.data());if(env->ExceptionCheck())return JNI_FALSE;
    for(int p=0;p<6;p++)if(strategy[p*2]<-1||strategy[p*2]>=6||strategy[p*2]==p||strategy[p*2+1]<0||strategy[p*2+1]>2)return JNI_FALSE;
    ensurePlanner();auto hidden=hiddenMasks();auto targets=planner->plan(state->dots,&strategy,&hidden);env->SetShortArrayRegion(output,0,60,targets.data());return env->ExceptionCheck()?JNI_FALSE:JNI_TRUE;
}
extern "C" jintArray Java_com_xenris_liquidwarsos_NativeInterface_aiSnapshot(JNIEnv* env,jclass) {
    std::lock_guard<std::mutex> guard(nativeStateMutex);
    if(!state)return nullptr;
    std::array<int,48> out{};int groups[6][240]={};int sumsX[6][240]={},sumsY[6][240]={};
    for(const Dot* d:state->dots){int p=d->team;if(p>=6)continue;int at=p*8;out[at]++;out[at+1]+=d->health;out[at+2]+=d->x;out[at+3]+=d->y;int cell=(int(d->y)*12/state->map->height)*20+int(d->x)*20/state->map->width;groups[p][cell]++;sumsX[p][cell]+=d->x;sumsY[p][cell]+=d->y;}
    for(int p=0;p<6;p++){int at=p*8;if(!out[at])continue;out[at+1]=out[at+1]*100/(MAX_HEALTH*out[at]);out[at+2]/=out[at];out[at+3]/=out[at];int best=0;for(int c=1;c<240;c++)if(groups[p][c]>groups[p][best])best=c;out[at+4]=sumsX[p][best]/groups[p][best];out[at+5]=sumsY[p][best]/groups[p][best];out[at+6]=groups[p][best];
        for(const Dot* d:state->dots)if(!state->allied(d->team,p)&&!state->items->concealedFrom(*state,p,d->team)){int dx=int(d->x)-out[at+4],dy=int(d->y)-out[at+5];if(dx*dx+dy*dy<48*48)out[at+7]++;}}
    jintArray result=env->NewIntArray(48);if(result)env->SetIntArrayRegion(result,0,48,out.data());return result;
}
extern "C" jboolean Java_com_xenris_liquidwarsos_NativeInterface_updateAiTargets(JNIEnv* env,jclass,jshortArray output,jint mask,jint level) {
    std::lock_guard<std::mutex> guard(nativeStateMutex);
    if(!state||!output||env->GetArrayLength(output)!=60)return JNI_FALSE;
    auto targets=localPlanner.plan(*state,unsigned(mask)&63,level);
    env->SetShortArrayRegion(output,0,60,targets.data());
    return env->ExceptionCheck()?JNI_FALSE:JNI_TRUE;
}

extern "C" void Java_com_xenris_liquidwarsos_NativeInterface_configureAlliances(JNIEnv* env,jclass,jintArray input){
    std::lock_guard<std::mutex> guard(nativeStateMutex);
 if(!state||!input||env->GetArrayLength(input)!=6){env->ThrowNew(env->FindClass("java/lang/IllegalArgumentException"),"Invalid alliances");return;}
 std::array<int,6> g;env->GetIntArrayRegion(input,0,6,g.data());if(env->ExceptionCheck())return;
 bool solo=true,paired=true;int count[3]={};for(int p=0;p<6;p++){solo&=g[p]==p;if(g[p]<0||g[p]>2)paired=false;else count[g[p]]++;}
 paired=paired&&count[0]==2&&count[1]==2&&count[2]==2;
 if(!solo&&!paired){env->ThrowNew(env->FindClass("java/lang/IllegalArgumentException"),"Invalid pairs");return;}
 for(int p=0;p<6;p++)state->alliances[p]=g[p];resetAiTargets();
}
