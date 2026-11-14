// SPDX-License-Identifier: GPL-3.0-or-later
// Production State/Random/Map; only map selection changed. No alternate renderer.
#include "state.hpp"
#include "map_catalog.hpp"
#include <memory>
#include <set>
#include <iostream>
#include <stdexcept>
HostAssets assets;HostAssets* acAssets=&assets;
static void ok(bool v,const char* s){if(!v)throw std::runtime_error(s);}
static void same(const State& a,const State& b){
 ok(a.dots.size()==b.dots.size(),"Count differs");
 for(size_t i=0;i<a.dots.size();i++){auto x=a.dots[i],y=b.dots[i];ok(x->x==y->x&&x->y==y->y&&x->team==y->team&&x->health==y->health&&x->damageQuarter==y->damageQuarter,"Stable-map placement differs");}
 ok(a.map->transform()==b.map->transform(),"Flip differs");
}
int main(int argc,char** argv){try{
 ok(argc==2,"Asset root required");assets.root=argv[1];ok(NUMBER_OF_MAPS==49&&map_catalog::COUNT==49,"Native count mismatch");
 std::set<int> active;for(unsigned i=0;i<49;i++){int id=map_catalog::idAt(i);ok(id>=1&&id<=49,"Retired/invalid map selected");active.insert(id);}ok(active.size()==49,"Catalog duplicates");
 std::set<int> drawn;
 for(int n=0;n<64;n++){
  int seed=n*1009+16400;Random expected(seed);int id=map_catalog::idAt(expected.next()%NUMBER_OF_MAPS);drawn.insert(id);
  auto random=std::make_unique<State>(0,-1,seed,100),fixed=std::make_unique<State>(0,id,seed,100);
  ok(random->aiRandom->getSeed()==expected.getSeed(),"Random selection consumed extra AI RNG");ok(random->moveRandom->getSeed()==unsigned(seed),"Random selection consumed movement RNG");same(*random,*fixed);
 }
 auto retired=std::make_unique<State>(0,0,16400,100),first=std::make_unique<State>(0,1,16400,100);same(*retired,*first);ok(retired->aiRandom->getSeed()==16400,"Retired fallback consumed RNG");
 std::cout<<"PASS49 unique stable IDs;64 real State random selections used one AI draw and no movement RNG; retired0 safely resolves to first active only for invalid native input. Sampled "<<drawn.size()<<" map IDs.\n";return 0;
}catch(const std::exception& e){std::cerr<<"FAIL "<<e.what()<<'\n';return 1;}}
