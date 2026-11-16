// SPDX-License-Identifier: GPL-3.0-or-later
#include "state_guard.hpp"
#include "lifecycle.hpp"
#include "../application/state.hpp"
#include "../application/garden3d.hpp"
#include <chrono>

extern "C" jfloatArray Java_com_xenris_liquidwarsos_NativeInterface_godotFrame(
    JNIEnv* env,jclass,jfloat x,jfloat y,jfloat w,jfloat h,jboolean all) {
    // Reads a completed immutable presentation snapshot, never the live State.
    auto data=garden3d::godotFrame(x,y,w,h,all);
    auto result=env->NewFloatArray(data.size());
    if(result)env->SetFloatArrayRegion(result,0,data.size(),data.data());
    return result;
}
