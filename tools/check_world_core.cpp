// SPDX-License-Identifier: GPL-3.0-or-later
#define main old_items_main
#include "check_items_core.cpp"
#undef main
#include "ai_tactics.hpp"
int main(int argc,char** argv){try{
 check(argc>=2,"assets required");assets.root=argv[1];if(argc>2){findSeeds();return 0;}effects();replay();
 std::set<int> allKinds,allCounts;int waves=0,resets=0;
 for(int seed=0;seed<128;seed++){
  auto a=std::make_unique<State>(0,1,seed,100),b=std::make_unique<State>(0,1,seed,100);std::set<int> prior;
  for(int wave=0;wave<12;wave++){
   if(wave==3||wave==7){for(auto s:{a.get(),b.get()}){auto rng=s->moveRandom->getSeed(),ai=s->aiRandom->getSeed(),items=s->items->randomState;s->items->grant(*s,0,Items::RESET,200,200);check(s->items->randomState!=items,"rewind does not rewind item stream");check(s->moveRandom->getSeed()==rng&&s->aiRandom->getSeed()==ai,"rewind keeps original movement/AI init state");while(s->items->warmup>0)s->items->begin(*s);}resets++;}
   for(auto s:{a.get(),b.get()}){s->items->elapsed=s->items->nextWave-7;s->items->begin(*s);}
   check(hash(*a)==hash(*b),"host/client identical random wave and rewind");int count=0;std::set<int> current,positions;
   for(auto p:a->items->pickups)if(p.kind>=0){count++;check(p.kind<18,"known kind");check(current.insert(p.kind).second,"no duplicate kind in wave");check(!prior.count(p.kind),"no immediate previous-wave kind");check(!a->map->isWall(p.x,p.y)&&p.x>=16&&p.y>=8,"actual floor item");check(positions.insert(p.y*a->map->width+p.x).second,"distinct random positions");if(wave==3||wave==7)check(p.kind!=Items::RESET,"no immediate reset loop");allKinds.insert(p.kind);}
   check(count>=1&&count<=6,"one through six per wave");allCounts.insert(count);prior=current;waves++;
  }
 }
 check(allKinds.size()==18&&allCounts.size()==6,"all eighteen kinds and six quantities covered");
 for(int map:{50,51})for(int count:{0,100,400,4500})for(int seed=0;seed<4;seed++){
  auto s=std::make_unique<State>(2,map,seed,count);state=s.get();check(s->map->width*s->map->height==5*800*480,"actual world is five times area");valid(*s);
  for(auto d:s->dots){check(d->x<2000&&d->y<960,"wide position in range");check(s->field[d->x][d->y]==d,"wide authoritative field identity");}
  auto snapshot=model_observation::snapshot(*s);check(snapshot.size()==217+s->dots.size(),"model full population retained");for(size_t i=217;i<snapshot.size();i++){unsigned v=snapshot[i];check((v&2047)==s->dots[i-217]->x&&((v>>11)&1023)==s->dots[i-217]->y&&((v>>21)&7)==s->dots[i-217]->team,"wide snapshot exact coordinates");}
  if(count==100&&seed==0){auto g=std::array<int,6>{{0,1,2,3,4,5}};auto wall=[&](int x,int y){return s->map->isWall(x,y);};AiTactics cached(2000,960,wall,g);for(int f=0;f<3;f++){AiTactics fresh(2000,960,wall,g);check(cached.plan(s->dots)==fresh.plan(s->dots),"cached topology gives same tactics");step(*s,10);}}
 }
 state=nullptr;std::cout<<"PASS "<<checks<<" core assertions; "<<waves<<" waves, "<<resets<<" resets; quantities1..6,18kinds; actual5x worlds and wide model words\n";return 0;
}catch(const std::exception& e){std::cerr<<"FAIL "<<e.what()<<" after "<<checks<<" checks\n";return 1;}}
