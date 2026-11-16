#include "state_guard.hpp"
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

#include "lifecycle.hpp"
#include "ai_targets.hpp"
#include "expedition_jni.hpp"
#include "../application/garden3d.hpp"
#include "../application/state.hpp"

extern "C" void Java_com_xenris_liquidwarsos_NativeInterface_init(JNIEnv* env, jclass jobj, jobject am) {
    std::lock_guard<std::mutex> guard(nativeStateMutex);
    acLib = new ACLib(env, jobj, am);
}

extern "C" void Java_com_xenris_liquidwarsos_NativeInterface_uninit(JNIEnv* env, jclass jobj) {
    std::lock_guard<std::mutex> guard(nativeStateMutex);
    acLib->destroy(env, jobj);
    delete(acLib);
    acLib = NULL;
}

extern "C" void Java_com_xenris_liquidwarsos_NativeInterface_createGame(JNIEnv* env, jclass jobj, jint team, jint map, jint seed, jint dotsPerTeam) {
    std::lock_guard<std::mutex> guard(nativeStateMutex);
    resetAiTargets();
    createGame(team, map, seed, dotsPerTeam);
}

extern "C" jboolean Java_com_xenris_liquidwarsos_NativeInterface_createGardenGame(JNIEnv*,jclass,jint seed){
    std::lock_guard<std::mutex> guard(nativeStateMutex);
    if(state||!garden3d::assetsValid())return JNI_FALSE;
    resetAiTargets();state=new State(0,garden3d::MAP_ID,seed,garden3d::START,true);
    garden3d::beginMatch();
    garden3d::sampleStep();
    if(state->players[0].score!=garden3d::START||state->players[2].score!=garden3d::START){delete state;state=nullptr;return JNI_FALSE;}return JNI_TRUE;
}
extern "C" jint Java_com_xenris_liquidwarsos_NativeInterface_gardenRenderStatus(JNIEnv*,jclass){
    std::lock_guard<std::mutex> guard(nativeStateMutex);return garden3d::status();
}
extern "C" void Java_com_xenris_liquidwarsos_NativeInterface_gardenPauseVisual(JNIEnv*,jclass,jboolean paused){std::lock_guard<std::mutex> g(nativeStateMutex);if(state&&state->garden3D)garden3d::pauseVisual(paused);}
extern "C" jdoubleArray Java_com_xenris_liquidwarsos_NativeInterface_gardenDiagnostics(JNIEnv* env,jclass){std::lock_guard<std::mutex> g(nativeStateMutex);double data[12]={};garden3d::diagnostics(data);auto out=env->NewDoubleArray(12);env->SetDoubleArrayRegion(out,0,12,data);return out;}

extern "C" void Java_com_xenris_liquidwarsos_NativeInterface_destroyGame(JNIEnv* env, jclass jobj) {
    std::lock_guard<std::mutex> guard(nativeStateMutex);
    resetAiTargets();
    clearExpeditionCommands();
    garden3d::resetVisual();
    destroyGame();
}

extern "C" void Java_com_xenris_liquidwarsos_NativeInterface_stepDots(JNIEnv* env, jclass jobj) {
    std::lock_guard<std::mutex> guard(nativeStateMutex);
    drainExpeditionCommands();
    stepDots();
    if(state&&state->garden3D)garden3d::sampleStep();
}

extern "C" void Java_com_xenris_liquidwarsos_NativeInterface_setPlayerPosition(JNIEnv* env, jclass jobj, jint team, jshortArray jxa, jshortArray jya) {
    std::lock_guard<std::mutex> guard(nativeStateMutex);
    short* x = env->GetShortArrayElements(jxa, NULL);
    short* y = env->GetShortArrayElements(jya, NULL);

    setPlayerPosition(team, x, y);

    env->ReleaseShortArrayElements(jxa, x, 0);
    env->ReleaseShortArrayElements(jya, y, 0);
}

extern "C" int Java_com_xenris_liquidwarsos_NativeInterface_getNearestDot(JNIEnv* env, jclass jobj, jint p, jshort px, jshort py) {
    std::lock_guard<std::mutex> guard(nativeStateMutex);
    return getNearestDot(p, px, py);
}

extern "C" int Java_com_xenris_liquidwarsos_NativeInterface_teamScore(JNIEnv* env, jclass jobj, jint p) {
    std::lock_guard<std::mutex> guard(nativeStateMutex);
    return teamScore(p);
}

extern "C" void Java_com_xenris_liquidwarsos_NativeInterface_setTimeSidebar(JNIEnv* env, jclass jobj, jfloat t) {
    std::lock_guard<std::mutex> guard(nativeStateMutex);
    setTimeSidebar(t);
}
