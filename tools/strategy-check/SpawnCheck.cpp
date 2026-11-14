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
int main(int argc,char** argv){try{
 assets.root=argv[1];int first=std::stoi(argv[2]),last=std::stoi(argv[3]),seeds=std::stoi(argv[4]);std::string path=argv[5];std::set<std::pair<int,int>> done;
 {std::ifstream in(path);std::string l;std::getline(in,l);while(std::getline(in,l)){std::replace(l.begin(),l.end(),',',' ');std::istringstream r(l);int m,s;if(r>>m>>s)done.insert({m,s});}}
 std::ofstream out(path,std::ios::app);if(done.empty()){out<<"map,seed,flip,fallback4500,hash1000";for(int t=0;t<6;t++)out<<",x"<<t<<",y"<<t;out<<'\n';}
 for(int map=first;map<=last;map++){
  // Every pixel of all four orientations, including boundary rules.
  Map original(map);for(int f=0;f<4;f++){Map flipped(map,f);for(int y=0;y<480;y++)for(int x=0;x<800;x++)ok(flipped.isWall(x,y)==original.isWall((f&1)?799-x:x,(f&2)?479-y:y),"flip pixel mismatch");}
  for(int seed=0;seed<seeds;seed++){if(done.count({map,seed}))continue;int fallbacks=0;
   for(int n:{0,100,400,4500}){auto s=std::make_unique<State>(0,map,seed,n);validate(*s,seed,n);if(n==4500)fallbacks=s->spawnFallbacks;}
   auto s=std::make_unique<State>(0,map,seed,1000);validate(*s,seed,1000);out<<map<<','<<seed<<','<<s->map->transform()<<','<<fallbacks<<','<<hash(*s);for(int t=0;t<6;t++)out<<','<<s->birthX[t]<<','<<s->birthY[t];out<<'\n';out.flush();
  }
  auto a=std::make_unique<State>(0,map,12345,400),b=std::make_unique<State>(5,map,12345,400);ok(hash(*a)==hash(*b),"local player affected initialization");
  for(int frame=0;frame<120;frame++){
   for(State* s:{a.get(),b.get()}){state=s;for(int t=0;t<6;t++){s->players[t].x[0]=short((frame*7+t*91)%780+10);s->players[t].y[0]=short((frame*3+t*63)%460+10);}Move::stepDots();}
   ok(hash(*a)==hash(*b),"same input different hash");
  }
  state=a.get();for(auto& p:a->players)p.x[0]=p.y[0]=-2;auto before=hash(*a);for(int i=0;i<80;i++)Move::stepDots();ok(hash(*a)==before,"model idle moved or consumed RNG");auto snap=model_observation::snapshot(*a);auto terrain=model_observation::terrain(*a);ok(hash(*a)==before,"observation changed state");
  std::cout<<"map="<<map<<" PASS all4flips, "<<seeds<<"seeds x0/100/400/1000/4500, replay/idle/readonly; checks="<<checks<<std::endl;
 }
 std::cout<<"PASS "<<checks<<" actual native assertions, no rendering/device/human play"<<std::endl;return 0;
}catch(const std::exception& e){std::cerr<<"FAIL "<<e.what()<<std::endl;return 1;}}
