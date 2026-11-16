// SPDX-License-Identifier: GPL-3.0-or-later
#define main previous_items_main
#include "check_items_core.cpp"
#undef main
#include "garden3d.hpp"
#include "local_tactics.hpp"
#include <chrono>
void order188(State& s,int op,int x=-1,int y=-1){static int packet=0;short xx[5]={-5,short(++packet),short(x),-1,-1},yy[5]={short(op*4+3),short(s.operations->generation[0]),short(y),-1,-1};s.operations->command(s,0,xx,yy);}
int main(int argc,char** argv){try{
 check(argc==2,"asset path required");assets.root=argv[1];auto begin=std::chrono::steady_clock::now();
 auto s=std::make_unique<State>(0,1001,188,1000,true);check(s->map->width==3264&&s->map->height==2048,"new world bounds");
 std::set<int> births;int minX=10000,maxX=0,minY=10000,maxY=0;bool high=false;
 for(int seed=0;seed<32;seed++){
  auto rng=s->moveRandom->getSeed(),ai=s->aiRandom->getSeed();garden3d::place(*s,seed);valid(*s);
  check(s->players[0].score==1000&&s->players[2].score==1000&&s->dots.size()==2000,"equal complete two-team opening");
  auto snap=model_observation::snapshot(*s);
  for(size_t k=0;k<s->dots.size();k++){
   auto d=s->dots[k];int dx=int(d->x)-s->birthX[d->team],dy=int(d->y)-s->birthY[d->team];check(dx*dx+dy*dy<=19*19,"opening stays a compact disk");
   unsigned v=unsigned(snap[217+k]);check(int(v&4095)==d->x&&int((v>>12)&4095)==d->y&&int((v>>24)&7)==d->team&&int((v>>27)&15)==d->health,"tall-world snapshot roundtrip");high|=d->y>1023;
  }
  check(rng==s->moveRandom->getSeed()&&ai==s->aiRandom->getSeed(),"independent placement RNG");
  auto first=hash(*s);garden3d::place(*s,seed);check(first==hash(*s),"same-seed compact starts identical");
  for(int t:{0,2}){births.insert(s->birthY[t]*3264+s->birthX[t]);minX=std::min(minX,s->birthX[t]);maxX=std::max(maxX,s->birthX[t]);minY=std::min(minY,s->birthY[t]);maxY=std::max(maxY,s->birthY[t]);}
 }
 check(high&&births.size()>50&&maxX-minX>2200&&maxY-minY>1100,"random starts across mainland, not fixed seats");
 s->operations->configure(*s,1);check(s->operations->sites.size()==5,"five new-map production sites");
 for(auto p:s->operations->sites)check(!s->map->isWall(p.x,p.y),"sites reachable land");
 s->items->capture(*s);auto opening=hash(*s);step(*s,40);s->items->rewind(*s);valid(*s);check(s->dots.size()==2000,"rewind restores real compact armies");check(s->items->nextWave==INT_MAX,"garden rewind keeps random pickups disabled");
 for(auto d:s->dots){int dx=d->x-s->birthX[d->team],dy=d->y-s->birthY[d->team];check(dx*dx+dy*dy<=19*19,"rewind retains compact geometry");}
 // Real bridge traversal, normal movement and shared navigation. Fixture only
 // changes starting occupants; never replaces map, bridge heights or rules.
 for(auto d:s->dots)delete d;s->dots.clear();s->field.clear();for(auto& p:s->players){p.score=0;for(int k=0;k<5;k++)p.x[k]=p.y[k]=-2;}
 int bx=2246,by=1092; // recomputed from middle crossing source below
 {int n=0;auto raw=acAssets->getFile("garden3d/bridges.bin",&n);check(raw&&n==12,"bridge fixture metadata");int16_t x,y;std::memcpy(&x,raw+4,2);std::memcpy(&y,raw+6,2);bx=x;by=y;free(raw);}
 check(!s->map->isWall(bx,by),"middle bridge floor");check(s->map->isWall(bx,by+70),"river away from bridge blocks units");
 for(int y=-5;y<5;y++)for(int x=-5;x<5;x++)add(*s,0,bx-85+x,by+y);
 add(*s,2,bx+120,by+90);s->items->warmup=0;s->operations->reset(*s);order188(*s,1,bx+85,by);order188(*s,3);
 check(s->operations->feedback[0]==0,"cross-river path accepted");step(*s,1700);valid(*s);
 int crossed=0;for(auto d:s->dots)if(d->team==0&&d->x>bx+40)crossed++;check(crossed>=85,"army actually crosses bridge using real movement");
 order188(*s,1,0,0);check(s->operations->feedback[0]==Operations::INVALID,"sea destination rejected");
 // Original classic map remains on its historical dimensions/placement.
 s.reset();auto classic=std::make_unique<State>(0,1,188,50);valid(*classic);check(classic->map->width==800&&classic->map->height==480&&classic->dots.size()==300,"2D neighbor preserved");state=nullptr;
 std::cout<<"PASS "<<checks<<" assertions; 32 seeds; compact clusters; deterministic RNG; tall snapshots; five sites; rewind; bridge crossing; sea rejection; 2D neighbor; elapsed_ms="<<std::chrono::duration_cast<std::chrono::milliseconds>(std::chrono::steady_clock::now()-begin).count()<<"\n";return 0;
 }catch(const std::exception& e){std::cerr<<"FAIL "<<e.what()<<" checks="<<checks<<"\n";return 1;}}
