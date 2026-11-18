// Copyright (c) 2026 Haoke. Executes the production simulation, without GL/device claims.
#define main historical_items_main
#include "check_items_core.cpp"
#undef main
#include "expedition.hpp"
#include <chrono>
#include <numeric>
static void invariant(State& s){
 std::array<int,MAX_TEAMS> count{};std::set<int> occupied;
 for(auto* d:s.dots){check(d&&d->team<s.teamCount&&d->health>0&&d->health<=MAX_HEALTH,"valid unit");check(s.field[d->x][d->y]==d,"field identity");check(occupied.insert(d->y*s.map->width+d->x).second,"no overlaps");check(!s.map->isWall(d->x,d->y)||s.items->active(d->team,Items::PHASE),"solid obstacles");count[d->team]++;}
 for(int t=0;t<s.teamCount;t++)check(count[t]==s.players[t].score,"score equals actual army");
}
static void ready(State& s){s.items->warmup=0;s.items->nextWave=INT_MAX;if(s.expedition){s.expedition->loot.clear();s.expedition->nextLoot=s.expedition->nextAI=INT_MAX;for(auto& seat:s.expedition->seats)seat.bot=false;}}
static std::unique_ptr<State> empty(){
 auto s=std::make_unique<State>(0,1,208,0);delete s->map;s->map=new Map(999);s->field.reset(s->map->width,s->map->height);ready(*s);return s;
}
static void commands(){
 auto s=empty();auto d=add(*s,0,300,300);add(*s,1,1000,700);
 // A U-shaped wall requires initial movement away from the destination.
 for(int y=260;y<=340;y+=8)check(s->map->temporaryObstacle(340,y,8,true),"barrier");
 for(int x=280;x<=336;x+=8){check(s->map->temporaryObstacle(x,260,8,true),"barrier");check(s->map->temporaryObstacle(x,340,8,true),"barrier");}
 s->players[0].x[0]=410;s->players[0].y[0]=300;
 step(*s,480);check(std::abs(int(d->x)-410)<5&&std::abs(int(d->y)-300)<5,"finger-follow routes around U wall");invariant(*s);
 short x[5]={-5,1,-1,-1,-1},y[5]={short(Operations::STOP*4+3),short(s->operations->generation[0]),-1,-1,-1};
 check(s->operations->command(*s,0,x,y),"STOP accepted");int px=d->x,py=d->y;step(*s,100);check(d->x==px&&d->y==py,"STOP holds position");
 x[1]=2;x[2]=600;y[0]=Operations::FOLLOW*4+3;y[2]=300;check(s->operations->command(*s,0,x,y),"replace STOP with follow");step(*s,230);check(d->x>550,"follow replacement executes");
 s->items->grant(*s,1,Items::FREEZE,0,0);px=d->x;py=d->y;int until=s->items->until[0][Items::FREEZE];step(*s,1400);check(d->x==px&&d->y==py&&until>s->items->elapsed,"freeze immobile for 9.8 seconds");
 s->items->grant(*s,0,Items::ACTIVE_CLEANSE,0,0);check(s->items->activate(*s,0,4,1)==Expedition::OK,"cleanse while frozen");check(!s->items->active(0,Items::FREEZE),"cleanse immediate");invariant(*s);std::cout<<"Direct/follow U-barrier, stop/replace, 10s freeze and cleanse PASS\n";
}
static void contact(){
 auto s=empty();for(int t=0;t<2;t++){s->players[t].x[0]=320-t*20;s->players[t].y[0]=300;for(int y=295;y<305;y++)for(int x=0;x<8;x++)add(*s,t,290+t*40+x,y);}
 for(int tick=0;tick<400;tick++)step(*s,1);
 check(s->items->audioCaptures[0]>0,"combat creates actual captures/audio events");check(s->dots.size()==160,"capture conserves total population");invariant(*s);std::cout<<"Two real armies fight, captures and score/occupancy consistency PASS\n";
}
static void worlds(){
 for(int map:{1,50,1001}){
  int seats=map==1001?10:0;auto a=std::make_unique<State>(0,map,208,400,map==1001,seats),b=std::make_unique<State>(1,map,208,400,map==1001,seats);ready(*a);ready(*b);
  check(a->dots.size()==size_t(a->teamCount)*a->dotsPerTeam,"full initial population");invariant(*a);
  for(int t=0;t<a->teamCount;t++)for(State* s:{a.get(),b.get()}){s->players[t].x[0]=s->birthX[(t+1)%s->teamCount];s->players[t].y[0]=s->birthY[(t+1)%s->teamCount];}
  auto began=std::chrono::steady_clock::now();
  for(int f=0;f<300;f++){step(*a,1);step(*b,1);if(f%30==0)check(hash(*a)==hash(*b),"deterministic replay after core rebuild");}
  auto ms=std::chrono::duration_cast<std::chrono::milliseconds>(std::chrono::steady_clock::now()-began).count();
  invariant(*a);invariant(*b);std::cout<<"map="<<map<<" teams="<<a->teamCount<<" units="<<a->dots.size()<<" 600 host steps ms="<<ms<<" (not device FPS) PASS\n";
 }
}
int main(int argc,char**argv){try{check(argc==2,"assets");assets.root=argv[1];commands();contact();worlds();state=nullptr;std::cout<<"PASS "<<checks<<" core assertions\n";return 0;}catch(const std::exception&e){std::cerr<<"FAIL "<<e.what()<<" after "<<checks<<" checks\n";return 1;}}
