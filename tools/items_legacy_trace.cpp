// SPDX-License-Identifier: GPL-3.0-or-later
// Compile unchanged against1.64 AND1.65 production sources, before first wave.
#include "state.hpp"
#include "move.hpp"
#include <iostream>
#include <memory>
HostAssets assets;HostAssets* acAssets=&assets;
uint64_t hash(State& s){uint64_t h=1469598103934665603ull;auto mix=[&](int n){h^=uint32_t(n);h*=1099511628211ull;};for(auto d:s.dots){mix(d->x);mix(d->y);mix(d->team);mix(d->health);mix(d->damageQuarter);}for(auto& p:s.players){mix(p.score);for(int k=0;k<5;k++){mix(p.x[k]);mix(p.y[k]);}}mix(s.moveRandom->getSeed());mix(s.aiRandom->getSeed());return h;}
int main(int argc,char** argv){if(argc!=2)return 1;assets.root=argv[1];std::cout<<"map,step,hash\n";for(int m=1;m<=49;m++){
 auto s=std::make_unique<State>(2,m,16500+m,100);state=s.get();std::cout<<m<<",0,"<<hash(*s)<<'\n';
 for(int step=0;step<140;step++){for(int t=0;t<6;t++){s->players[t].x[0]=(step*7+t*91)%780+10;s->players[t].y[0]=(step*3+t*63)%460+10;}Move::stepDots();if(step%10==9)std::cout<<m<<','<<step+1<<','<<hash(*s)<<'\n';}
 }state=nullptr;return 0;}
