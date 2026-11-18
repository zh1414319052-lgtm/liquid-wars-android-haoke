// Isolated tests of the production simulation, not a playability claim.
#define main previous_items_main
#include "check_items_core.cpp"
#undef main
#include "expedition.hpp"
#include "state_hash.hpp"
int main(int argc,char** argv){try{
 check(argc==2,"asset path required");assets.root=argv[1];
 for(int seats:{0,2,10}){
  auto s=std::make_unique<State>(0,seats?1001:0,193,1000,seats>0,seats);
  auto& i=*s->items; if(s->expedition)for(auto& p:s->expedition->seats)p.bot=false;
  i.capture(*s);i.nextWave=INT32_MAX;
  for(int t=0;t<s->teamCount;t++)s->alliances[t]=t;
  check(i.activate(*s,0,1,0)==Expedition::EMPTY,"empty rejected");
  for(int n=0;n<12;n++)i.grant(*s,0,Items::ACTIVE_FREEZE,0,0);
  check(i.stock[0][0]==10&&!i.pickupAllowed(0,Items::ACTIVE_FREEZE),"stack ten, full pickup retained");
  i.grant(*s,1,Items::ACTIVE_CLEANSE,0,0);i.grant(*s,1,Items::ACTIVE_CLEANSE,0,0);
  check(i.stock[1][1]==2,"two pickups two uses");
  check(i.activate(*s,0,2,0)==Expedition::OK&&i.stock[0][0]==9,"consume exactly one");
  check(!i.active(0,Items::FREEZE),"owner never frozen");
  for(int t=1;t<s->teamCount;t++)check(i.active(t,Items::FREEZE),"global all seats independent position");
  check(i.until[1][Items::FREEZE]-i.elapsed==10000,"user specified ten-second freeze in both modes");
  check(i.activate(*s,0,2,0)==Expedition::STALE&&i.stock[0][0]==9,"duplicate no consumption");
  check(i.activate(*s,0,3,0)==Expedition::COOLDOWN&&i.stock[0][0]==9,"cooldown independent inventory");
  int expires=i.until[1][Items::FREEZE];i.grant(*s,0,Items::FREEZE,0,0);check(i.until[1][Items::FREEZE]==expires,"automatic effect cannot refresh control");
  check(i.activate(*s,1,1,1)==Expedition::OK&&!i.active(1,Items::FREEZE)&&i.stock[1][1]==1,"cleanse while frozen, whole team");
  check(i.immunity[1]-i.elapsed==Items::CLEANSE_MS,"cleanse protection uses its declared duration");
  if(s->teamCount>2)check(i.active(2,Items::FREEZE),"cleanse does not thaw other armies");
  i.grant(*s,0,Items::FREEZE,0,0);check(!i.active(1,Items::FREEZE),"cleanse immunity resists automatic freeze");
  auto hash=simulationHash(*s);i.stock[1][1]++;check(hash!=simulationHash(*s),"inventory affects consistency hash");
  int clock=i.elapsed;step(*s,3);check(i.elapsed>clock,"frozen opponents do not stop game clock");
  if(s->expedition){auto snap=s->expedition->snapshot(*s,1);check(snap[0]==193&&snap[11+11]==0,"protocol and private stock");
   check(snap.size()==size_t(11+s->teamCount*63+snap[7]*5+snap[8]*8+(snap[9]+snap[10])*4+7),"snapshot exact structure");
   i.grant(*s,1,Items::DOUBLE,0,0);check(s->players[1].score>1000,"classic growth in expedition");
   i.grant(*s,1,Items::RESET,0,0);check(i.rewindPending,"expedition rewind deferred outside loot iterator");i.begin(*s);
   check(s->expedition->clock==0&&i.stock[0][0]==0&&s->players[1].score==1000,"rewind resets authority consistently");
  }
 }
 for(int seats:{0,2}){auto s=std::make_unique<State>(0,seats?1001:0,193,1000,seats>0,seats);auto& i=*s->items;
  if(s->expedition){s->expedition->loot.clear();for(auto& seat:s->expedition->seats)seat.bot=false;}
  Dot* d=s->dots[0];int team=d->team;
  for(int kind=0;kind<2;kind++)for(int amount:{0,1,2,10}){
   i.stock[team][kind]=amount;
   if(s->expedition){auto& ex=*s->expedition;ex.loot.clear();ex.loot.push_back({919,kind,d->x,d->y,ex.clock});ex.loot.back().hp=1;ex.end(*s);check(i.stock[team][kind]==std::min(10,amount+1),"actual expedition pickup count");check(std::none_of(ex.loot.begin(),ex.loot.end(),[](auto p){return p.id==919;})==(amount<10),"full expedition pickup remains");}
   else{auto& p=i.pickups[0];p=Items::Pickup{};p.kind=Items::COUNT+kind;p.x=d->x;p.y=d->y;p.hp=1;i.end(*s);check(i.stock[team][kind]==std::min(10,amount+1),"actual classic pickup count");check((p.kind<0)==(amount<10),"full classic pickup remains");}
  }
 }
 // Shared devouring pacing, retained partial HP and all-items-cleared waves.
 {auto s=std::make_unique<State>(0,1001,194,1000,true,2);auto& e=*s->expedition;auto& i=*s->items;i.warmup=0;i.capture(*s);for(auto& t:e.seats)t.bot=false;e.nextAI=INT32_MAX;
  Dot* d=s->dots[0];int t=d->team;e.loot.clear();e.loot.push_back({9001,0,d->x,d->y,0});e.loot.push_back({9002,2,20,20,0});
  e.end(*s);check(e.loot.front().hp>=1196&&i.stock[t][0]==0,"no instant collection; max four HP per seven ms");
  int hp=e.loot.front().hp;auto oldHash=simulationHash(*s);e.loot.front().damage[t]++;check(oldHash!=simulationHash(*s),"contribution enters authority hash");e.loot.front().damage[t]--;
  int itemX=e.loot.front().x,itemY=e.loot.front().y;e.loot.front().x=20;e.loot.front().y=20;e.clock=100000;i.elapsed=e.clock;e.nextLoot=e.clock;e.begin(*s);e.end(*s);
  check(e.loot.front().id==9001&&e.loot.front().hp==hp,"leaving retains progress, items never expire or refill");
  e.loot.front().x=itemX;e.loot.front().y=itemY;
  auto facts=e.snapshot(*s,t);check(facts[11+42]==0x4c485031&&(facts[11+13]&2047)==hp&&facts[11+21]==9001,"compatible LHP1 public progress");
  int iterations=0;while(std::any_of(e.loot.begin(),e.loot.end(),[](auto p){return p.id==9001;})&&iterations<1300){e.clock+=7;i.elapsed=e.clock;e.end(*s);iterations++;}
  check(iterations>=298&&i.stock[t][0]==1,"dense devouring requires classic duration and grants once");
  check(std::any_of(e.loot.begin(),e.loot.end(),[](auto p){return p.kind!=2&&p.id!=9001;}),"last item starts next wave despite remaining coins");
  e.loot.clear();e.loot.push_back({9100,2,d->x,d->y,e.clock});int coins=e.seats[t].coins;e.end(*s);check(e.seats[t].coins==coins&&e.loot[0].hp>=1196,"coin also needs devouring");
  e.loot[0].hp=1;e.end(*s);check(e.seats[t].coins==coins+10&&e.loot.empty(),"coin grants once without starting an item wave");
  e.loot.push_back({9200,1,d->x,d->y,e.clock});e.loot[0].hp=1;i.until[t][Items::FREEZE]=e.clock+10000;e.end(*s);check(i.stock[t][1]==1,"frozen army can devour cleanse");
 }
 std::cout<<"guest193 shared rules and timed waves/pickups OK\n";return 0;
 }catch(const std::exception& e){std::cerr<<e.what()<<"\n";return 1;}}
