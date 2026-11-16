// Production simulation strategy comparison. Offline accelerated test, NOT footage.
#define main item_contract_main
#include "check_items_core.cpp"
#undef main
#include "expedition.hpp"
#include <chrono>
int main(int argc,char**argv){
 if(argc!=5)return 2;assets.root=argv[1];int count=std::atoi(argv[2]),seed=std::atoi(argv[3]),limit=std::atoi(argv[4]);
 auto s=std::make_unique<State>(0,1001,seed,1000,true,count);state=s.get();auto&e=*s->expedition;
 for(auto& seat:e.seats)seat.bot=false;s->items->warmup=0;
 auto started=std::chrono::steady_clock::now();int planned=-1000;
 while(e.cutoff<0&&e.clock<limit*1000){
  if(e.clock-planned>=1400){planned=e.clock;
   for(int t=0;t<count;t++){
    if(!s->players[t].score)continue;int strategy=(t+seed)%4;
    // 0: full army at production; 1: original-home defense;
    // 2: split production+attack+nearest resource; 3: mobile resource raiding.
    auto group=s->operations->groups[t][0];int x=s->birthX[t],y=s->birthY[t];
    if(strategy==0){x=s->operations->sites[0].x;y=s->operations->sites[0].y;}
    if(strategy>=2){int best=INT32_MAX;for(auto p:e.loot){int d=(p.x-group.x)*(p.x-group.x)+(p.y-group.y)*(p.y-group.y);if(d<best){best=d;x=p.x;y=p.y;}}}
    if(strategy==2){if(!s->operations->split[t]){/* Strategy fixture assigns an even split; no invented units. */s->operations->split[t]=true;int n=0;for(auto d:s->dots)if(d->team==t)d->squad=n++%3;}
      for(int g=0;g<3;g++)s->operations->order(*s,t,g,g<2?s->operations->sites[g].x:x,g<2?s->operations->sites[g].y:y);
    }else s->operations->order(*s,t,3,x,y);
    auto& seat=e.seats[t];if(seat.coins>=40)e.purchase(*s,t,seat.lastCommand+1,seat.shoes<=seat.armor?0:1);
   }
  }
  Move::stepDots();
 }
 std::array<int,MAX_TEAMS> counted{};for(auto d:s->dots){check(d->team<count&&d->health>0&&!s->map->isWall(d->x,d->y),"legal live unit");check(s->field[d->x][d->y]==d,"one cell per real unit");counted[d->team]++;}for(int t=0;t<count;t++)check(counted[t]==s->players[t].score,"real score");double seconds=std::chrono::duration<double>(std::chrono::steady_clock::now()-started).count();
 std::cout<<"{\"seed\":"<<seed<<",\"seats\":"<<count<<",\"game_ms\":"<<e.clock<<",\"cpu_seconds\":"<<seconds<<",\"cutoff\":"<<e.cutoff<<",\"players\":[";
 for(int t=0;t<count;t++){if(t)std::cout<<',';auto&a=e.seats[t];std::cout<<"{\"seat\":"<<t<<",\"strategy\":"<<(t+seed)%4<<",\"units\":"<<s->players[t].score<<",\"coins\":"<<a.coins<<",\"shoes\":"<<a.shoes<<",\"armor\":"<<a.armor<<'}';}std::cout<<"]}\n";state=nullptr;
}
