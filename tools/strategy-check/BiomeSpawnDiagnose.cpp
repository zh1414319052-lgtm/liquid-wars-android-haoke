// SPDX-License-Identifier: GPL-3.0-or-later
// Real State/Move/Map, no alternate game renderer or model provider.
#include "state.hpp"
#include "move.hpp"
#include "model_observation.hpp"
#include "spawn.hpp"
#include <memory>
#include <fstream>
#include <iostream>
#include <set>
#include <sstream>
#include <algorithm>
#include <stdexcept>
HostAssets assets;HostAssets* acAssets=&assets;static uint64_t checks=0;
static void ok(bool v,const char* why){checks++;if(!v)throw std::runtime_error(why);}
static uint64_t hash(const State& s){uint64_t h=1469598103934665603ull;auto mix=[&](int v){h^=uint32_t(v);h*=1099511628211ull;};
 for(auto d:s.dots){mix(d->x);mix(d->y);mix(d->team);mix(d->health);mix(d->damageQuarter);}for(const auto& p:s.players){mix(p.score);for(int k=0;k<5;k++){mix(p.x[k]);mix(p.y[k]);}}mix(s.moveRandom->getSeed());mix(s.aiRandom->getSeed());return h;}
static void validate(State& s,int seed,int n){
 ok(s.dots.size()==size_t(6*n),"count");ok(s.moveRandom->getSeed()==seed&&s.aiRandom->getSeed()==seed,"spawn/flip consumed gameplay RNG");
 std::vector<int> owner(384000,-1);
 for(int t=0;t<6;t++){
  ok(s.players[t].score==n,"initial score");ok(!s.map->isWall(s.birthX[t],s.birthY[t]),"birth in wall");ok(s.players[t].x[0]==s.birthX[t]&&s.players[t].y[0]==s.birthY[t],"cursor follows actual birth");
  for(int k=0;k<n;k++){const Dot* d=s.dots[t*n+k];ok(d->team==t,"dot order");ok(!s.map->isWall(d->x,d->y),"dot in wall");ok(s.field[d->x][d->y]==d,"field identity");int p=d->y*800+d->x;ok(owner[p]<0,"dot overlap");owner[p]=t;}
 }
 for(const Dot* d:s.dots)for(int dy=-2;dy<=2;dy++)for(int dx=-2;dx<=2;dx++){int x=d->x+dx,y=d->y+dy;if(x<0||y<0||x>=800||y>=480)continue;int other=owner[y*800+x];ok(other<0||other==d->team,"teams interleaved inside2px margin");}
}
int main(int argc,char** argv){assets.root=argv[1];int map=std::stoi(argv[2]);for(int seed=23;seed<24;seed++)for(int n:{0,100,400,4500,1000}){std::cout<<"CREATE map="<<map<<" seed="<<seed<<" n="<<n<<std::endl;auto s=std::make_unique<State>(0,map,seed,n);validate(*s,seed,n);}std::cout<<"PASS"<<std::endl;return 0;}
