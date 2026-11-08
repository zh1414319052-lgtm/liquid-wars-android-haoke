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

extern "C" void Java_com_xenris_liquidwarsos_NativeInterface_destroyGame(JNIEnv* env, jclass jobj) {
    std::lock_guard<std::mutex> guard(nativeStateMutex);
    resetAiTargets();
    destroyGame();
}

extern "C" void Java_com_xenris_liquidwarsos_NativeInterface_stepDots(JNIEnv* env, jclass jobj) {
    std::lock_guard<std::mutex> guard(nativeStateMutex);
    stepDots();
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
