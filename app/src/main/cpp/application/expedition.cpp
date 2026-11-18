// SPDX-License-Identifier: GPL-3.0-or-later
#include "expedition.hpp"
#include "state.hpp"
#include "expedition_balance.hpp"
#include <algorithm>
#include <cmath>
#include <queue>
#include <unordered_set>
namespace {
using namespace expedition_balance;
int sq(int n){return n*n;}
int dist(int x,int y,int xx,int yy){return sq(x-xx)+sq(y-yy);}
constexpr int CYCLE=expedition_balance::ReconWindowMs,COIN=Expedition::COIN_KIND,POPULATION_MULTIPLIER=expedition_balance::MaxPopulationMultiplier;

constexpr auto prices=expedition_balance::UpgradePrice;
}
unsigned Expedition::random(unsigned n){rng^=rng<<13;rng^=rng>>17;rng^=rng<<5;return n?rng%n:0;}
bool Expedition::reachable(const State& s,int x,int y,int xx,int yy)const{
 int n=std::max(std::abs(xx-x),std::abs(yy-y));for(int i=0;i<=n;i++)if(s.map->isWall(n?x+(xx-x)*i/n:x,n?y+(yy-y)*i/n:y))return false;return true;
}
bool Expedition::spot(State& s,int& x,int& y,int r,int away,int sector){
 for(int attempt=0;attempt<8192;attempt++){
  x=int(random(s.map->width-2*r))+r;y=int(random(s.map->height-2*r))+r;
  if(sector>=0){int nearest=-1,best=INT32_MAX;for(int t=0;t<s.teamCount;t++){int d=s.operations->travelDistanceAt(s,birthDistances[t],x,y);if(d>=0&&d<best){best=d;nearest=t;}}if(nearest!=sector||best<80||best>800)continue;}
  bool valid=true;for(int dy=-r;dy<=r&&valid;dy++)for(int dx=-r;dx<=r;dx++)if(s.map->isWall(x+dx,y+dy)||s.field[x+dx][y+dy]){valid=false;break;}
  if(!valid)continue;
  for(int t=0;t<s.teamCount;t++)if(dist(x,y,s.birthX[t],s.birthY[t])<sq(away)){valid=false;break;}
  for(const auto& o:obstacles)if(!o.removed&&dist(x,y,o.x,o.y)<sq(r+o.radius+8))valid=false;
  for(const auto& p:s.operations->sites)if(dist(x,y,p.x,p.y)<sq(r+50))valid=false;
  if(valid){for(int t=0;t<s.teamCount;t++)if(!birthDistances[t].empty()&&s.operations->travelDistanceAt(s,birthDistances[t],x,y)<0){valid=false;break;}}
  if(valid)return true;
 }return false;
}
Expedition::Expedition(State& s,int seed):duration(s.teamCount*CYCLE*2),rng(uint32_t(seed)^0x192de37bu){
 if(!rng)rng=1;for(int t=0;t<s.teamCount;t++)seats[t].bot=t!=s.me;
 s.operations->configure(s,2);
 for(int t=0;t<s.teamCount;t++)birthDistances[t]=s.operations->travelDistances(s,s.birthX[t],s.birthY[t]);
 // One production and one attack site; solid-clear footprints reject narrow bridges.
 for(int type=0;type<2;type++){int bestX=-1,bestY=-1;float best=1e9f;
  for(int candidate=0;candidate<24;candidate++){int x=0,y=0;if(!spot(s,x,y,38,170))break;int nearest=INT32_MAX,farthest=0,total=0;for(int t=0;t<s.teamCount;t++){int d=s.operations->travelDistanceAt(s,birthDistances[t],x,y);nearest=std::min(nearest,d);farthest=std::max(farthest,d);total+=d;}float value=(farthest-nearest)/float(std::max(1,total/s.teamCount))+.0001f*farthest;if(value<best){best=value;bestX=x;bestY=y;}}
  if(bestX>=0){Operations::Site site;site.x=bestX;site.y=bestY;s.operations->sites.push_back(site);}}
 for(int i=0;i<s.teamCount*4;i++){int x=0,y=0;if(spot(s,x,y,10,75)&&s.map->temporaryObstacle(x,y,5,true))obstacles.push_back({++sequence,x,y,5,120,0,-1,false});}
 spawnLoot(s);nextLoot=expedition_balance::LootEveryMs;
}
bool Expedition::frozen(const Dot& d)const{return d.freezeUntil>clock;}
bool Expedition::inHome(const State& s,const Dot& d)const{return dist(d.x,d.y,s.birthX[d.team],s.birthY[d.team])<=sq(HOME_RADIUS);}
int Expedition::reconSeat(const State& s)const{
 // The fixed schedule does not reshuffle on elimination/disconnection: equal
 // scheduled opportunities and the cutoff remain stable. Dead seats are skipped
 // for display only; their window is not given to another player.
 int t=(clock/CYCLE)%s.teamCount;return s.players[t].score>0?t:-1;
}
bool Expedition::enemyVisible(const State& s,int viewer,int x,int y,bool mini)const{
 if(viewer<0||viewer>=s.teamCount)return false;
 if(mini)return reconSeat(s)==viewer;
 if(sightClock!=clock)refreshSight(s);
 const auto& bounds=sightBounds[viewer];
 if(x<bounds[0]-SIGHT||y<bounds[1]-SIGHT||x>bounds[2]+SIGHT||y>bounds[3]+SIGHT)return false;
 // Closest cells first: identical radius/occlusion result, but avoid tracing
 // 144-cell rays to distant scouts before finding an adjacent friendly particle.
 int bx=x/32,by=y/32,range=(SIGHT+31)/32;
 for(int ring=0;ring<=range;ring++)for(int dy=-ring;dy<=ring;dy++)for(int dx=-ring;dx<=ring;dx++){
  if(std::max(std::abs(dx),std::abs(dy))!=ring)continue;
  int cx=bx+dx,cy=by+dy;if(cx<0||cy<0||cx>=sightCols||cy>=sightRows)continue;
  for(int i=sightHeads[(viewer*sightRows+cy)*sightCols+cx];i>=0;i=scouts[i].next){const auto& p=scouts[i];if(dist(p.x,p.y,x,y)<=sq(SIGHT)&&reachable(s,p.x,p.y,x,y))return true;}
 }
 return false;
}
void Expedition::refreshSight(const State& s)const{
 sightCols=(s.map->width+31)/32;sightRows=(s.map->height+31)/32;sightHeads.assign(s.teamCount*sightCols*sightRows,-1);scouts.clear();scouts.reserve(s.dots.size());
 for(auto& b:sightBounds)b={{INT32_MAX/2,INT32_MAX/2,INT32_MIN/2,INT32_MIN/2}};
 for(auto d:s.dots){auto& bound=sightBounds[d->team];bound[0]=std::min(bound[0],int(d->x));bound[1]=std::min(bound[1],int(d->y));bound[2]=std::max(bound[2],int(d->x));bound[3]=std::max(bound[3],int(d->y));int b=(d->team*sightRows+d->y/32)*sightCols+d->x/32;scouts.push_back({d->x,d->y,sightHeads[b]});sightHeads[b]=int(scouts.size())-1;}sightClock=clock;
}
bool Expedition::obstacleVisible(const State& s,int viewer,const Obstacle& o)const{
 // The solid center occludes itself. Test the actual walkable perimeter, so a
 // nearby player sees protection/progress without revealing distant blockers.
 int r=o.radius+1;
 for(auto p:std::array<std::array<int,2>,8>{{{{r,0}},{{-r,0}},{{0,r}},{{0,-r}},{{r,r}},{{r,-r}},{{-r,r}},{{-r,-r}}}})
  if(!s.map->isWall(o.x+p[0],o.y+p[1])&&enemyVisible(s,viewer,o.x+p[0],o.y+p[1]))return true;
 return false;
}
void Expedition::spawnLoot(State& s,bool refreshCoins){
 // Live items never expire or get topped up. Coins have their own limited refresh.
 if(refreshCoins)loot.erase(std::remove_if(loot.begin(),loot.end(),[&](const Loot& p){return p.kind==COIN&&p.hp==Items::PICKUP_HP&&clock-p.born>=LootLifetimeMs;}),loot.end());
 bool newWave=std::none_of(loot.begin(),loot.end(),[](const Loot& p){return p.kind!=COIN;});
 std::array<int,MAX_TEAMS> coins{},powers{};
 for(const auto& p:loot){int sector=-1,best=INT32_MAX;for(int t=0;t<s.teamCount;t++){int d=s.operations->travelDistanceAt(s,birthDistances[t],p.x,p.y);if(d>=0&&d<best){best=d;sector=t;}}if(sector>=0)(p.kind==COIN?coins:powers)[sector]++;}
 for(int sector=0;sector<s.teamCount;sector++)for(int category=0;category<2;category++){
  if(category==0&&!refreshCoins)continue;if(category==1&&!newWave)continue;
  int& count=category==0?coins[sector]:powers[sector];int target=category==0?2:1;
  for(int attempt=0;count<target&&attempt<4&&int(loot.size())<s.teamCount*LootPerSeat;attempt++){
   int x=0,y=0;if(!spot(s,x,y,Items::PICKUP_RADIUS+2,48,sector))break;
   bool separate=true;for(const auto& p:loot)if(dist(x,y,p.x,p.y)<sq(30)){separate=false;break;}if(!separate)continue;
   int kind=COIN;if(category==1){int slot=int(random(ITEMS));kind=slot==BLACK_HOLE?BLACK_HOLE_LOOT:slot;if(random(3)==0)kind=AUTO_BASE+int(random(Items::COUNT));if(kind==AUTO_BASE+Items::RESET&&s.items->resetSpawned)kind=AUTO_BASE+Items::CAUSAL;if(kind==AUTO_BASE+Items::RESET)s.items->resetSpawned=true;}
   loot.push_back({++sequence,kind,x,y,clock});count++;
  }
 }
}
bool Expedition::blockedByLoot(int x,int y)const{
 for(const auto& p:loot)if(p.hp>0&&std::abs(x-p.x)<=Items::PICKUP_RADIUS&&std::abs(y-p.y)<=Items::PICKUP_RADIUS&&dist(x,y,p.x,p.y)<=sq(Items::radiusForHp(p.hp)))return true;
 return false;
}
int Expedition::moves(const State& s,Dot& d)const{
 if(cutoff>=0||s.items->warmup>0||frozen(d))return 0;
 d.expeditionSpeed+=ShoesPercent[seats[d.team].shoes];
 int n=1;if(d.expeditionSpeed>=100){d.expeditionSpeed-=100;n++;}return n;
}
void Expedition::contact(State& s,Dot& defender,Dot& attacker){
 if(attacker.team==defender.team||frozen(attacker)||cutoff>=0)return;
 int attack=100;if(s.operations->sites.size()>1&&s.operations->sites[1].owner==attacker.team&&!s.operations->sites[1].contested)attack=AttackPercent;
 int defense=ArmorPercent[seats[defender.team].armor]*(inHome(s,defender)?2:1);
 // Thousandth-health accumulator: even maximum 2.6x defense takes damage.
 defender.expeditionDamage+=uint16_t(750*attack/defense);
 while(defender.expeditionDamage>=1000){defender.expeditionDamage-=1000;if(--defender.health==0){
  s.players[defender.team].score--;s.players[attacker.team].score++;defender.team=attacker.team;defender.health=MAX_HEALTH/2;
  defender.expeditionDamage=defender.expeditionSpeed=0;defender.squad=attacker.squad;defender.damageQuarter=defender.armorCarry=0;defender.freezeUntil=defender.freezeImmuneUntil=0;break;
 }}
}
int Expedition::command(State& s,int t,int serial,int kind,int g,int x,int y){
 if(t<0||t>=s.teamCount)return INVALID;auto& seat=seats[t];
 if(serial<=seat.lastCommand)return STALE;seat.lastCommand=serial;
 if(kind==101){bool ok=s.items->command(s,t,g,x,y);return seat.lastResult=ok?OK:INVALID;}
 return seat.lastResult=s.items->activate(s,t,serial,kind);
}
int Expedition::purchase(State& s,int t,int serial,int equipment){
 if(t<0||t>=s.teamCount)return INVALID;auto& seat=seats[t];if(serial<=seat.lastCommand)return STALE;seat.lastCommand=serial;
 if(cutoff>=0||s.players[t].score<=0)return seat.lastResult=FINISHED;if(equipment<0||equipment>1)return seat.lastResult=INVALID;
 int& level=equipment?seat.armor:seat.shoes;if(level>=MAX_LEVELS)return seat.lastResult=MAX_LEVEL;
 if(seat.coins<prices[level+1])return seat.lastResult=NO_COINS;seat.coins-=prices[++level];return seat.lastResult=OK;
}
int Expedition::spawn(State& s,int t,int count,int x,int y){
 count=std::min({count,std::max(0,s.dotsPerTeam*POPULATION_MULTIPLIER-s.players[t].score),s.teamCount*s.dotsPerTeam*POPULATION_MULTIPLIER-int(s.dots.size())});
 if(count<=0||s.players[t].score<=0||s.items->opposed(s,t,Items::ANTIGROW))return 0;int born=0;
 for(int r=0;r<=28&&born<count;r++)for(int dy=-r;dy<=r&&born<count;dy++)for(int dx=-r;dx<=r&&born<count;dx++){
  if(std::max(std::abs(dx),std::abs(dy))!=r||s.map->isWall(x+dx,y+dy)||s.field[x+dx][y+dy]||!reachable(s,x,y,x+dx,y+dy))continue;
  auto d=new Dot(x+dx,y+dy,t);s.dots.push_back(d);s.field[d->x][d->y]=d;s.players[t].score++;born++;
 }
 return born;
}
void Expedition::economy(State& s){
 // Fixed capture time and garrison; extra hundreds cannot speed it up.
 for(size_t i=0;i<s.operations->sites.size();i++){
  auto& p=s.operations->sites[i];std::array<int,MAX_TEAMS> inside{};
  for(auto d:s.dots)if(!frozen(*d)&&dist(d->x,d->y,p.x,p.y)<=sq(SiteRadius)&&reachable(s,d->x,d->y,p.x,p.y))inside[d->team]++;
  if(p.owner>=0&&s.players[p.owner].score<=0){p.owner=p.claim=-1;p.progress=0;}
  int candidate=-1,opponents=0;for(int t=0;t<s.teamCount;t++){if(inside[t]>=Garrison)candidate=t;if(inside[t]>=ContestMinimum)opponents++;}p.contested=opponents>1;
  if(candidate>=0&&!p.contested&&p.owner!=candidate){if(p.claim!=candidate){p.claim=candidate;p.progress=0;}p.progress+=EconomyTickMs;if(p.progress>=CaptureMs){if(p.owner>=0)p.owner=-1;else p.owner=candidate;p.progress=0;p.claim=-1;p.spawnAt=clock;}}
  else if(!p.contested){p.progress=std::max(0,p.progress-EconomyTickMs);if(!p.progress)p.claim=-1;}
  if(i==0&&p.owner>=0&&!p.contested&&p.claim<0&&inside[p.owner]>=Garrison){
   if(clock-p.spawnAt>=SpawnEveryMs){int born=spawn(s,p.owner,SpawnUnits,p.x,p.y);p.born+=born;if(born)p.pulseAt=clock;p.spawnAt=clock;}
  }else p.spawnAt=clock;
 }
 for(auto& o:obstacles){
  if(o.removed||o.wardUntil>clock)continue;int owner=-1;
  for(auto d:s.dots){if(!frozen(*d)&&s.items->active(d->team,Items::QUARRY)&&s.items->quarryBudget[d->team]>0&&!s.items->opposed(s,d->team,Items::ROCKSEAL)&&dist(d->x,d->y,o.x,o.y)<=sq(o.radius+3)){owner=d->team;break;}}
  if(owner<0)continue;o.hp-=DevourDamage;o.owner=owner;
  if(o.hp<=0){o.removed=true;s.map->temporaryObstacle(o.x,o.y,o.radius,false);int award=std::min(ObstacleYield,s.items->quarryBudget[owner]);int born=spawn(s,owner,award,o.x,o.y);s.items->quarryBudget[owner]-=born;}
 }
}
void Expedition::begin(State& s){
 if(cutoff>=0)return;if(s.items->warmup>0){s.items->warmup=std::max(0,s.items->warmup-7);return;}
 clock+=7;s.items->elapsed=clock;
 if(clock>=duration){finish(s);return;}
 for(auto d:s.dots){d->freezeUntil=s.items->until[d->team][Items::FREEZE];d->freezeImmuneUntil=s.items->immunity[d->team];}
 if(clock>=nextLoot){spawnLoot(s);nextLoot=clock+LootEveryMs;}
 if(clock%EconomyTickMs==0)economy(s);
 if(clock>=nextAI){bots(s);nextAI=clock+700;}
}
void Expedition::end(State& s){
 if(cutoff>=0)return;
 bool collectedItem=false;
 for(auto it=loot.begin();it!=loot.end();){
  int slot=activeSlotForLoot(it->kind);
  Items::Pickup pickup;pickup.kind=it->kind==COIN?Items::DOUBLE:slot>=0?Items::COUNT+slot:it->kind-AUTO_BASE;
  pickup.x=it->x;pickup.y=it->y;pickup.hp=it->hp;pickup.damage=it->damage;
  int winner=s.items->devour(s,pickup);it->hp=pickup.hp;it->eater=pickup.eater;it->damage=pickup.damage;
  if(winner<0){++it;continue;}
  auto finished=*it;it=loot.erase(it);
  if(finished.kind==COIN)seats[winner].coins=std::min(999,seats[winner].coins+CoinValue);
  else{collectedItem=true;s.items->grant(s,winner,pickup.kind,pickup.x,pickup.y);if(s.items->rewindPending)return;}
 }
 if(collectedItem&&std::none_of(loot.begin(),loot.end(),[](const Loot& p){return p.kind!=COIN;}))spawnLoot(s,false);
 int alive=0;for(int t=0;t<s.teamCount;t++)alive+=s.players[t].score>0;if(alive==0||(alive==1&&!s.items->blackHoleActive()))finish(s);
}
void Expedition::finish(State& s){if(cutoff>=0)return;cutoff=clock;for(int t=0;t<s.teamCount;t++)finalCounts[t]=s.players[t].score;}
void Expedition::bots(State& s){
 for(int t=0;t<s.teamCount;t++){
  auto& seat=seats[t];if(!seat.bot||s.players[t].score<=0)continue;
  if(s.items->stock[t][CLEANSE]&&(s.items->active(t,Items::FREEZE)||s.items->pulling(t)))command(s,t,seat.lastCommand+1,CLEANSE,3,0,0);
  if(seat.coins>=40){int eq=seat.shoes<=seat.armor?0:1;purchase(s,t,seat.lastCommand+1,eq);}
  // AI uses the same public pickups/sites and sensed opponents as players.
  auto own=s.operations->groups[t][0];if(!own.count)continue;int tx=own.x,ty=own.y,best=0x3fffffff;
  for(auto p:loot){int slot=activeSlotForLoot(p.kind);if(slot>=0&&s.items->stock[t][slot]>=Items::STOCK_CAP)continue;int v=dist(own.x,own.y,p.x,p.y);if(v<best){best=v;tx=p.x;ty=p.y;}}
  for(auto p:s.operations->sites){int v=dist(own.x,own.y,p.x,p.y)/2;if(v<best){best=v;tx=p.x;ty=p.y;}}
  for(int enemy=0;enemy<s.teamCount;enemy++)if(!s.allied(t,enemy)&&s.players[enemy].score>0&&!s.items->concealedFrom(s,t,enemy)){Operations::Group p;int closest=INT32_MAX;for(auto d:s.dots)if(d->team==enemy){int distance=dist(own.x,own.y,d->x,d->y);if(distance<closest&&(enemyVisible(s,t,d->x,d->y,true)||enemyVisible(s,t,d->x,d->y))){closest=distance;p={1,d->x,d->y};}}if(!p.count)continue;
   if(dist(own.x,own.y,p.x,p.y)<sq(70)&&s.items->stock[t][FREEZE])command(s,t,seat.lastCommand+1,FREEZE,3,p.x,p.y);
   if(s.players[t].score>s.players[enemy].score*12/10&&dist(own.x,own.y,p.x,p.y)<sq(200)){tx=p.x;ty=p.y;break;}}
  int pending=s.items->pending[t];
  if(pending==Items::TELEPORT)s.items->command(s,t,s.items->nonce[t],tx,ty);
  else if(pending)for(int e=0;e<s.teamCount;e++)if(!s.allied(t,e)&&s.players[e].score>0&&!s.items->concealedFrom(s,t,e)){auto p=s.operations->groups[e][0];if(enemyVisible(s,t,p.x,p.y)||enemyVisible(s,t,p.x,p.y,true)){s.items->command(s,t,s.items->nonce[t],e,0);break;}}
  auto& route=s.operations->routes[t][0];if(route.points.empty()||dist(tx,ty,route.points.back().x,route.points.back().y)>sq(24))s.operations->order(s,t,3,tx,ty);
 }
}
std::vector<int> Expedition::snapshot(const State& s,int viewer,bool authority)const{
 auto visible=[&](int owner,int x,int y){return authority||viewer==owner||enemyVisible(s,viewer,x,y);};
 int na=0,nc=0;for(auto z:anchors)na+=visible(z.owner,z.x,z.y);for(auto c:chills)nc+=visible(c.owner,c.x,c.y);
 std::vector<int> out{PROTOCOL,clock,duration,cutoff,s.teamCount,reconSeat(s),CYCLE-clock%CYCLE,int(loot.size()),int(obstacles.size()),na,nc};
 for(int t=0;t<s.teamCount;t++){
  const auto& p=seats[t];bool own=authority||viewer==t;
  for(int v:{s.players[t].score,finalCounts[t],own?s.birthX[t]:-1,own?s.birthY[t]:-1,own?p.coins:0,own?p.shoes:0,own?p.armor:0,own?p.lastCommand:0,own?p.lastResult:0,own?p.teleports:0,int(p.bot)})out.push_back(v);
  for(auto array:{s.items->stock[t],s.items->ready[t]})for(int k=0;k<8;k++)out.push_back(own&&k<2?array[k]:0);
  for(int value:{s.items->until[t][Items::FREEZE],s.items->immunity[t],s.items->until[t][Items::QUARRY],s.items->quarryBudget[t]})for(int k=0;k<3;k++)out.push_back(own?value:0);
  for(int v:{s.items->pending[t],s.items->nonce[t],s.items->epoch})out.push_back(own?v:0);
  out.push_back(0x4c485031); // Public presentation capability, independent of private seat data.
  for(int k=0;k<Items::COUNT;k++)out.push_back(own?std::max(0,s.items->until[t][k]-clock):0);
 }
 // LHP1: four public pickup records per seat in unused active-slot padding.
 // +13..16 packed HP/eater/activity, +21..24 matching stable IDs; +42 marker.
 for(size_t k=0;k<loot.size();k++){const auto& p=loot[k];int base=11+int(k/4)*63,index=int(k%4);
  int eater=p.eater;bool seen=authority||eater==viewer||(eater>=0&&enemyVisible(s,viewer,p.x,p.y));
  if(!seen)eater=-1;out[base+13+index]=p.hp|((eater+1)<<11)|(p.eater>=0?1<<15:0);out[base+21+index]=p.id;
 }
 // Last visible event per team uses the remaining reserved padding (17,18,25,26).
 for(const auto& event:s.items->events){if(event.team<0||event.team>=s.teamCount||event.kind<0||event.kind>=Items::COUNT+Items::ACTIVE_COUNT||clock-event.at>2200)continue;
  int x=event.x,y=event.y;if(x==0&&y==0){auto own=s.operations->groups[event.team][0];x=own.x;y=own.y;}
  if(!authority&&(s.items->concealedFrom(s,viewer,event.team)||(event.team!=viewer&&!enemyVisible(s,viewer,x,y))))continue;
  if(event.target>=0&&event.target!=viewer&&!authority&&!enemyVisible(s,viewer,x,y))continue;
  int base=11+event.team*63;out[base+17]=event.serial;out[base+18]=event.at;out[base+25]=x|(y<<12);
  out[base+26]=event.kind|(event.stage<<5)|((std::max(-32768,std::min(32767,event.amount))+32768)<<9);
 }
 for(auto p:loot)for(int v:{p.id,p.kind,p.x,p.y,p.born})out.push_back(v);
 for(auto o:obstacles){bool seen=authority||obstacleVisible(s,viewer,o);for(int v:{o.id,o.x,o.y,o.radius,seen?o.hp:120,seen?o.wardUntil:0,seen?o.owner:-1,int(o.removed)})out.push_back(v);}
 for(auto z:anchors)if(visible(z.owner,z.x,z.y))for(int v:{z.owner,z.x,z.y,z.until})out.push_back(v);
 for(auto c:chills)if(visible(c.owner,c.x,c.y))for(int v:{c.owner,c.x,c.y,c.at})out.push_back(v);
 s.items->appendBlackHoleFacts(out,viewer);
 return out;
}
uint64_t Expedition::hash(const State& s)const{
 uint64_t h=1469598103934665603ull;auto mix=[&](int v){h^=uint32_t(v);h*=1099511628211ull;};
 mix(rng);mix(sequence);mix(nextLoot);mix(nextAI);for(const auto& p:loot){mix(p.hp);mix(p.eater);for(int v:p.damage)mix(v);}for(int v:snapshot(s,-1,true))mix(v);
 for(auto d:s.dots){mix(d->expeditionDamage);mix(d->expeditionSpeed);mix(d->freezeUntil);mix(d->freezeImmuneUntil);}return h;
}
