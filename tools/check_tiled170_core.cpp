// SPDX-License-Identifier: GPL-3.0-or-later
#define main old_items_main
#include "check_items_core.cpp"
#undef main
#include "ai_tactics.hpp"
#include "minimap_sight.hpp"
#include "state_hash.hpp"
int main(int argc,char** argv){try{
 check(argc>=2,"assets required");assets.root=argv[1];if(argc>2&&std::string(argv[2])=="fixtures"){findSeeds();return 0;}effects();replay();
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
 for(int map:{50,51})for(int count:{0,100,400,4500})for(int seed=0;seed<128;seed++){
  auto s=std::make_unique<State>(2,map,seed,count);state=s.get();check(s->map->width*s->map->height==4*800*480,"actual world is four times area");valid(*s);
  for(auto d:s->dots){check(d->x<1600&&d->y<960,"wide position in range");check(s->field[d->x][d->y]==d,"wide authoritative field identity");}
  auto snapshot=model_observation::snapshot(*s);check(snapshot.size()==217+s->dots.size(),"model full population retained");for(size_t i=217;i<snapshot.size();i++){unsigned v=snapshot[i];check((v&2047)==s->dots[i-217]->x&&((v>>11)&1023)==s->dots[i-217]->y&&((v>>21)&7)==s->dots[i-217]->team,"wide snapshot exact coordinates");}
  if(count==100&&seed==0){auto g=std::array<int,6>{{0,1,2,3,4,5}};auto wall=[&](int x,int y){return s->map->isWall(x,y);};AiTactics cached(1600,960,wall,g);for(int f=0;f<3;f++){AiTactics fresh(1600,960,wall,g);check(cached.plan(s->dots)==fresh.plan(s->dots),"cached topology gives same tactics");step(*s,10);}}
 }
 for(int map:{1,50,51}){auto s=std::make_unique<State>(2,map,1701,400);auto before=simulationHash(*s);auto fog=minimap_sight::snapshot(*s,false),all=minimap_sight::snapshot(*s,true);check(fog[4]<=all[4],"fog never exposes extra markers");check(before==simulationHash(*s),"sight reads no RNG and changes no authoritative state");for(size_t i=5;i+3<fog.size();i+=4){bool actual=false;for(auto d:s->dots)if(d->team==fog[i]&&d->x==fog[i+1]&&d->y==fog[i+2]){actual=true;break;}check(actual,"visible markers are actual particles");}}
 if(argc>2){auto saved=assets.root;assets.root=argv[2];auto s=std::make_unique<State>(0,1,7,1);s->field.clear();int x[6]={360,440,650,690,80,330},y[6]={200,200,350,350,80,220};for(auto d:s->dots){d->x=x[d->team];d->y=y[d->team];s->field[d->x][d->y]=d;}int group[6]={0,1,0,2,1,2};for(int i=0;i<6;i++)s->alliances[i]=group[i];auto hash0=simulationHash(*s);auto seen=minimap_sight::snapshot(*s,false);std::set<int> visible;for(size_t i=5;i<seen.size();i+=4)visible.insert(seen[i]);check(visible==std::set<int>({0,2,3,5}),"own/ally scout reveal, far hide and wall occlusion");check(hash0==simulationHash(*s),"sight fixture leaves exact state hash unchanged");s->dots[5]->x=80;s->dots[5]->y=400;seen=minimap_sight::snapshot(*s,false);visible.clear();for(size_t i=5;i<seen.size();i+=4)visible.insert(seen[i]);check(!visible.count(5),"enemy leaves vision and disappears");s->alliances[1]=0;seen=minimap_sight::snapshot(*s,false);visible.clear();for(size_t i=5;i<seen.size();i+=4)visible.insert(seen[i]);check(visible.count(1),"new ally shown even behind wall");auto full=minimap_sight::snapshot(*s,true);check(full[4]==6,"spectator sees all6");assets.root=saved;}
 state=nullptr;std::cout<<"PASS "<<checks<<" core assertions; "<<waves<<" waves, "<<resets<<" resets; quantities1..6,18kinds; actual4x worlds and wide model words\n";return 0;
}catch(const std::exception& e){std::cerr<<"FAIL "<<e.what()<<" after "<<checks<<" checks\n";return 1;}}
