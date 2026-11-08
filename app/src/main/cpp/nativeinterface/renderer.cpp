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

#include "renderer.hpp"
#include "../application/state.hpp"
#include "../application/render_camera.hpp"
#include "../application/state_hash.hpp"

extern "C" jlong Java_com_xenris_liquidwarsos_NativeInterface_stateHash(JNIEnv*,jclass){std::lock_guard<std::mutex> guard(nativeStateMutex);return state?jlong(simulationHash(*state)):0;}

extern "C" void Java_com_xenris_liquidwarsos_NativeInterface_setCamera(JNIEnv*,jclass,jfloat x,jfloat y,jfloat w,jfloat h,jboolean all){setRenderCamera(x,y,w,h,all);}
extern "C" jintArray Java_com_xenris_liquidwarsos_NativeInterface_sceneInfo(JNIEnv* env,jclass){
    std::lock_guard<std::mutex> guard(nativeStateMutex);if(!state)return nullptr;
    auto texture=state->map->textureInfo();jint values[]={state->map->width,state->map->height,state->map->id,state->map->transform(),state->birthX[state->me],state->birthY[state->me],texture[0],texture[1],texture[2],texture[3],texture[4]};
    jintArray out=env->NewIntArray(11);if(out)env->SetIntArrayRegion(out,0,11,values);return out;
}

extern "C" void Java_com_xenris_liquidwarsos_NativeInterface_onSurfaceCreated(JNIEnv * env,
                                                                              jclass jobj) {
    std::lock_guard<std::mutex> guard(nativeStateMutex);
    onSurfaceCreated();
}

extern "C" void Java_com_xenris_liquidwarsos_NativeInterface_onDrawFrame(JNIEnv * env, jclass jobj) {
    std::lock_guard<std::mutex> guard(nativeStateMutex);
    onDrawFrame();
}

extern "C" void Java_com_xenris_liquidwarsos_NativeInterface_onSurfaceChanged(JNIEnv * env,
                                                                              jclass jobj, jint width, jint height) {
    std::lock_guard<std::mutex> guard(nativeStateMutex);
    onSurfaceChanged(width, height);
}
