// SPDX-License-Identifier: GPL-3.0-or-later
#include "state.hpp"
#include "move.hpp"
#include "model_observation.hpp"
#include "spawn.hpp"
#include <iostream>
#include <memory>
#include <set>
#include <stdexcept>
#include <cstdint>
HostAssets assets;HostAssets* acAssets=&assets;
static int checks;
static void ok(bool v,const char* why){checks++;if(!v)throw std::runtime_error(why);}
static uint64_t hash(const State& s){uint64_t h=1469598103934665603ull;auto mix=[&](uint32_t v){h^=v;h*=1099511628211ull;};
 for(const Dot* d:s.dots){mix(d->x);mix(d->y);mix(d->team);mix(d->health);mix(d->damageQuarter);}for(const auto& p:s.players){mix(p.score);for(int k=0;k<5;k++){mix(p.x[k]);mix(p.y[k]);}}mix(s.moveRandom->getSeed());mix(s.aiRandom->getSeed());return h;}
static void valid(State& s,int n){std::set<int> cells;ok(s.dots.size()==6u*n,"dot count");for(int t=0;t<6;t++){
 ok(s.players[t].score==n,"initial score");ok(!s.map->isWall(s.players[t].x[0],s.players[t].y[0]),"cursor on floor");
 for(int k=0;k<n;k++){auto d=s.dots[t*n+k];ok(d->team==t,"dot order");ok(!s.map->isWall(d->x,d->y),"wall collision");ok(s.field[d->x][d->y]==d,"field identity");ok(cells.insert(d->x*480+d->y).second,"no overlap");
 ok(abs(d->x-s.players[t].x[0])<=90&&abs(d->y-s.players[t].y[0])<=90,"bounded sparse spiral");
 }}
 for(const Dot* d:s.dots)for(int dx=-1;dx<=1;dx++)for(int dy=-1;dy<=1;dy++){int x=d->x+dx,y=d->y+dy;if(x<0||y<0||x>=800||y>=480)continue;const Dot* other=s.field[x][y];ok(!other||other->team==d->team,"no enemy contact at spawn");}}
int main(int argc,char** argv){try{assets.root=argv[1];ok(NUMBER_OF_MAPS==50,"map range");
 for(int map=0;map<50;map++){
  for(int n:{0,100,400,4500}){auto s=std::make_unique<State>(0,map,921,n);valid(*s,n);ok(s->moveRandom->getSeed()==921&&s->aiRandom->getSeed()==921,"spawn did not consume gameplay RNG");}
  auto host=std::make_unique<State>(0,map,12345,400),client=std::make_unique<State>(5,map,12345,400);
  std::vector<std::pair<int,int>> heldPositions;for(int k=800;k<1200;k++)heldPositions.emplace_back(host->dots[k]->x,host->dots[k]->y);
  ok(hash(*host)==hash(*client),"same initial state for different local player");
  std::set<int> redPositions;std::set<int> redSectors;
  for(int seed=0;seed<128;seed++){auto pts=spawn::choose(seed,map,[&](int x,int y){return host->map->isWall(x,y);});int x=pts[2].first,y=pts[2].second;redPositions.insert(x*480+y);int best=0,dist=1000000;for(int k=0;k<6;k++){int dx=x-spawn::deployment[map][k][0],dy=y-spawn::deployment[map][k][1],d=dx*dx+dy*dy;if(d<dist){dist=d;best=k;}}redSectors.insert(best);}
  ok(redPositions.size()>32&&redSectors.size()==6,"128-seed position and region diversity");
  for(int step=0;step<120;step++){
   for(State* s:{host.get(),client.get()}){state=s;for(int t=0;t<6;t++){s->players[t].x[0]=390;s->players[t].y[0]=240;}s->players[2].x[0]=s->players[2].y[0]=-2;Move::stepDots();}
   ok(hash(*host)==hash(*client),"host/client input replay");
  }
  for(int k=800;k<1200;k++)if(host->dots[k]->team==2)ok(std::make_pair(host->dots[k]->x,host->dots[k]->y)==heldPositions[k-800],"model idle survivors never moved");
  auto before=hash(*host);auto snap=model_observation::snapshot(*host);auto terrain=model_observation::terrain(*host);ok(before==hash(*host),"model observation stays readonly");
  std::cout<<"map="<<map+1<<" PASS 0/100/400/4500 placement, randomness and replay\n";
 }
 auto all=std::make_unique<State>(0,49,876,400);state=all.get();for(auto& p:all->players)p.x[0]=p.y[0]=-2;auto before=hash(*all);for(int i=0;i<80;i++)Move::stepDots();ok(hash(*all)==before,"all-model idle no motion or RNG");
 std::cout<<"PASS "<<checks<<" native assertions; actual map loading, placement and movement; no graphics/device\n";return 0;
 }catch(const std::exception& e){std::cerr<<"FAIL "<<e.what()<<'\n';return 1;}}
