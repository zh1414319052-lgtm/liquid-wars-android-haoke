// SPDX-License-Identifier: GPL-3.0-or-later
// Test-only, read-only full replicated-state comparison. The host AI random
// generator produces commands; clients replay commands and do not run that AI.
#include "nativeinterface/state_guard.hpp"
#include "application/state_hash.hpp"
#include <jni.h>
extern "C" JNIEXPORT jlongArray JNICALL Java_com_xenris_liquidwarsos_Online184StateProbe_hashes(JNIEnv* env,jclass){
 std::lock_guard<std::mutex> guard(nativeStateMutex);if(!state)return nullptr;const State& s=*state;
 uint64_t h=1469598103934665603ull;auto mix=[&](int v){h^=uint32_t(v);h*=1099511628211ull;};
 mix(s.map->id);mix(s.map->width);mix(s.map->height);mix(s.map->transform());
 for(auto d:s.dots)for(int v:{int(d->x),int(d->y),int(d->team),int(d->health),int(d->damageQuarter),int(d->armorCarry)})mix(v);
 for(auto& p:s.players){mix(p.score);for(int k=0;k<5;k++){mix(p.x[k]);mix(p.y[k]);}}
 mix(s.moveRandom->getSeed());mix(s.items->randomState);
 for(int v:s.items->snapshot(s))mix(v);for(int v:s.map->openings())mix(v);
 for(auto d:s.dots)mix(d->squad);uint64_t commands=s.operations->hash();mix(int(commands));mix(int(commands>>32));
 jlong values[]={jlong(h),jlong(simulationHash(s)),jlong(s.aiRandom->getSeed()),jlong(s.moveRandom->getSeed()),jlong(commands)};
 auto result=env->NewLongArray(5);env->SetLongArrayRegion(result,0,5,values);return result;
}
