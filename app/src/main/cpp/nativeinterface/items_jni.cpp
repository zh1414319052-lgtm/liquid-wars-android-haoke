// SPDX-License-Identifier: GPL-3.0-or-later
#include "state_guard.hpp"
#include "../application/state.hpp"
#include <jni.h>
std::mutex nativeStateMutex;
extern "C" jintArray Java_com_xenris_liquidwarsos_NativeInterface_itemSnapshot(JNIEnv* env,jclass){
    std::lock_guard<std::mutex> guard(nativeStateMutex);
    if(!state)return nullptr;auto values=state->items->snapshot(*state);
    jintArray out=env->NewIntArray(values.size());if(out)env->SetIntArrayRegion(out,0,values.size(),values.data());return out;
}
extern "C" jint Java_com_xenris_liquidwarsos_NativeInterface_totalParticles(JNIEnv*,jclass){
    std::lock_guard<std::mutex> guard(nativeStateMutex);return state?int(state->dots.size()):0;
}
extern "C" jint Java_com_xenris_liquidwarsos_NativeInterface_terrainRevision(JNIEnv*,jclass){
    std::lock_guard<std::mutex> guard(nativeStateMutex);return state?state->map->revision():0;
}

extern "C" jint Java_com_xenris_liquidwarsos_NativeInterface_playTimeMillis(JNIEnv*,jclass){std::lock_guard<std::mutex> g(nativeStateMutex);return state?state->items->elapsed:0;}
extern "C" jboolean Java_com_xenris_liquidwarsos_NativeInterface_areAllied(JNIEnv*,jclass,jint a,jint b){std::lock_guard<std::mutex> g(nativeStateMutex);return state&&a>=0&&a<6&&b>=0&&b<6&&state->alliances[a]==state->alliances[b];}
extern "C" jint Java_com_xenris_liquidwarsos_NativeInterface_allianceScore(JNIEnv*,jclass,jint a){std::lock_guard<std::mutex> g(nativeStateMutex);if(!state||a<0||a>=6)return 0;int n=0;for(int t=0;t<6;t++)if(state->alliances[a]==state->alliances[t])n+=state->players[t].score;return n;}

#include "../application/minimap_sight.hpp"
extern "C" jintArray Java_com_xenris_liquidwarsos_NativeInterface_minimapSnapshot(JNIEnv* env,jclass,jboolean all){std::lock_guard<std::mutex> g(nativeStateMutex);if(!state)return nullptr;auto values=minimap_sight::snapshot(*state,all);jintArray out=env->NewIntArray(values.size());if(out)env->SetIntArrayRegion(out,0,values.size(),values.data());return out;}
