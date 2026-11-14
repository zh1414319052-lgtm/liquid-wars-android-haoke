// SPDX-License-Identifier: GPL-3.0-or-later
#include "state.hpp"
#include <memory>
#include <chrono>
#include <iostream>
HostAssets assets;HostAssets* acAssets=&assets;
static uint64_t h(const State& s){uint64_t v=1469598103934665603ull;auto mix=[&](int q){v^=uint32_t(q);v*=1099511628211ull;};for(auto d:s.dots){mix(d->x);mix(d->y);mix(d->team);mix(d->health);}for(int t=0;t<6;t++){mix(s.birthX[t]);mix(s.birthY[t]);}return v;}
int main(int argc,char** argv){assets.root=argv[1];for(int n:{0,100,400,4500}){auto at=std::chrono::steady_clock::now();auto s=std::make_unique<State>(0,0,921,n);std::cout<<n<<" particles/team, init_ms="<<std::chrono::duration<double,std::milli>(std::chrono::steady_clock::now()-at).count()<<", fallback_teams="<<s->spawnFallbacks<<", hash="<<h(*s)<<std::endl;} }
