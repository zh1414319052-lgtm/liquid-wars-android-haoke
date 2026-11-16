// Executes the production C++ simulation. These are isolated synthetic fixtures,
// not human playtesting or proof of public ten-player networking.
#define main previous_items_main
#include "check_items_core.cpp"
#undef main
#include "expedition.hpp"
#include "state_hash.hpp"
void valid192(State& s){
 std::array<int,MAX_TEAMS> counts{};std::set<int> cells;
 for(auto d:s.dots){check(d->team<s.teamCount&&d->health>0&&d->health<=10,"valid owner/health");check(!s.map->isWall(d->x,d->y),"no obstacle overlap");check(s.field[d->x][d->y]==d,"field identity");check(cells.insert(d->y*s.map->width+d->x).second,"unique cell");counts[d->team]++;}
 for(int t=0;t<s.teamCount;t++)check(counts[t]==s.players[t].score,"score counts real particles");
}
void clear192(State& s){for(auto d:s.dots)delete d;s.dots.clear();s.field.clear();for(auto& p:s.players){p.score=0;p.x[0]=p.y[0]=-2;}for(auto& seat:s.expedition->seats)seat.bot=false;}
int main(int argc,char** argv){try{
 check(argc==2,"asset path required");assets.root=argv[1];
 for(int seats:{2,10}){
  auto s=std::make_unique<State>(0,1001,192,1000,true,seats);auto& e=*s->expedition;
  check(s->dots.size()==size_t(seats*1000),"full actual army for every seat");valid192(*s);
  int coins=0,powers=0;for(auto p:e.loot){if(p.kind==Expedition::ITEMS)coins++;else powers++;}
  check(coins==seats&&powers==seats,"initial wave supplies both resource categories at every seat count");
  check(s->operations->sites.size()==2,"two distinct sites");check(e.duration==seats*120000,"two complete reconnaissance cycles");
  auto first=simulationHash(*s);auto twin=std::make_unique<State>(0,1001,192,1000,true,seats);check(first==simulationHash(*twin),"same seed/state deterministic");twin.reset();
  for(int t=0;t<seats;t++){e.clock=t*60000;check(e.reconSeat(*s)==t,"each slot receives one minute");}
  e.clock=0;auto& p=e.seats[0];p.coins=300;
  check(e.purchase(*s,0,1,0)==Expedition::OK&&p.shoes==1&&p.coins==260,"shoe purchase");
  check(e.purchase(*s,0,1,0)==Expedition::STALE&&p.coins==260,"duplicate purchase never charges twice");
  check(e.purchase(*s,0,2,0)==Expedition::OK&&e.purchase(*s,0,3,0)==Expedition::OK&&p.shoes==3,"three equipment levels");
  check(e.purchase(*s,0,4,0)==Expedition::MAX_LEVEL,"upgrade cap");
  auto d=s->dots[0];d->expeditionSpeed=0;int moves=0;for(int k=0;k<100;k++)moves+=e.moves(*s,*d);check(moves==120,"20 percent movement cadence");
  p.armor=3;Dot defender=*d;Dot enemy=*s->dots[1000];enemy.team=1;
  defender.x=s->birthX[0];defender.y=s->birthY[0];check(e.inHome(*s,defender),"home defense inside");
  for(int k=0;k<10;k++)e.contact(*s,defender,enemy);check(defender.health<MAX_HEALTH&&defender.team==0,"2.6x defense not invulnerability");
  defender.x+=43;check(!e.inHome(*s,defender),"leaving home removes bonus immediately");
  auto snapshot=e.snapshot(*s,1);check(snapshot[11+4]==0,"enemy inventory not exposed");
  e.clock=e.duration-7;e.begin(*s);check(e.cutoff==e.duration,"authoritative exact cutoff");auto final=e.finalCounts;e.finish(*s);check(final==e.finalCounts,"settlement idempotent");
 }
 auto s=std::make_unique<State>(0,1001,192,1000,true,2);auto& e=*s->expedition;
 int bx=s->birthX[0],by=s->birthY[0];clear192(*s);auto a=add(*s,0,bx,by);auto b=add(*s,1,bx+4,by);auto outside=add(*s,1,bx-12,by);
 e.seats[0].bag.fill(1);e.seats[1].bag.fill(1);e.seats[0].lastCommand=0;e.seats[1].lastCommand=0;
 check(e.command(*s,0,1,Expedition::FREEZE,3,bx+35,by)==Expedition::OK,"freeze cast");
 step(*s,90);check(e.frozen(*b)&&!e.frozen(*outside),"freeze is spatial, not whole enemy squad");
 int time=e.clock;step(*s,10);check(e.clock>time,"freeze never stops match clock");
 check(e.command(*s,1,1,Expedition::CLEANSE,3,0,0)==Expedition::OK&&!e.frozen(*b),"cleanse usable while frozen");
 e.seats[0].bag[Expedition::TELEPORT]=1;
 check(e.command(*s,0,2,Expedition::TELEPORT,3,-1,-1)==Expedition::INVALID&&e.seats[0].bag[2]==1,"invalid teleport retains inventory");
 check(e.command(*s,0,3,Expedition::TELEPORT,3,bx,by+10)==Expedition::OK,"atomic teleport to valid footprint");valid192(*s);
 e.anchors.push_back({1,bx,by, e.clock+8000});e.seats[0].bag[2]=1;e.seats[0].ready[2]=0;
 check(e.command(*s,0,4,Expedition::TELEPORT,3,bx+12,by)==Expedition::BLOCKED&&e.seats[0].bag[2]==1,"anchor blocks origin and keeps item");
 // Actual eligible obstruction, solid map before, real particles after consumption.
 clear192(*s);auto& o=e.obstacles[0];a=add(*s,0,o.x+o.radius+1,o.y);b=add(*s,1,bx,by);
 e.seats[0].bag[4]=1;e.seats[0].bag[5]=1;e.seats[0].ready.fill(0);
 check(e.command(*s,0,5,Expedition::DEVOUR,3,0,0)==Expedition::OK,"devour activation");
 check(e.command(*s,0,6,Expedition::FORTIFY,3,o.x,o.y)==Expedition::OK,"fortify valid obstacle");
 e.refreshSight(*s);auto guarded=e.snapshot(*s,0);int obstacleAt=11+s->teamCount*43+guarded[7]*5;
 check(guarded[obstacleAt+5]>e.clock,"solid object does not hide its own visible protection state");
 int hp=o.hp;step(*s,100);check(o.hp==hp&&!o.removed,"fortification prevents consumption");
 o.wardUntil=0;e.seats[0].devourUntil[0]=e.clock+8000;
 step(*s,400);check(o.removed&&!s->map->isWall(o.x,o.y)&&s->players[0].score==31,"removed obstacle gives exactly thirty real units");
 int count=s->players[0].score;step(*s,400);check(s->players[0].score==count,"removed obstacle cannot yield twice");valid192(*s);
 auto before=simulationHash(*s);s->items->rewind(*s);check(before==simulationHash(*s),"legacy rewind cannot restore expedition resources");
 // New death item removes real units only once, with a usable reaction counter.
 clear192(*s);e.cutoff=-1;e.clock=0;e.chills.clear();e.anchors.clear();
 add(*s,0,bx,by);
 for(int i=0;i<9;i++)add(*s,1,bx+10+i,by);
 for(auto& seat:e.seats){seat.lastCommand=0;seat.ready.fill(0);seat.bag.fill(1);seat.deathAt=0;seat.lifeUntil=0;seat.deathAllowedAt=0;}
 check(e.command(*s,0,1,Expedition::DEATH,3,1,0)==Expedition::OK,"death requires a real visible enemy seat");
 check(s->players[1].score==9&&e.seats[1].deathAt==1000,"warning preserves army until authoritative deadline");
 check(e.command(*s,1,1,Expedition::LIFE,3,0,0)==Expedition::OK,"life counter can cancel pending execution");
 e.clock=1100;e.resolveDeath(*s);check(s->players[1].score==9,"counter prevents removal");
 e.seats[0].bag[Expedition::DEATH]=1;e.seats[0].ready[Expedition::DEATH]=0;
 check(e.command(*s,0,2,Expedition::DEATH,3,1,0)==Expedition::BLOCKED&&e.seats[0].bag[Expedition::DEATH],"protected target rejects without charging");
 e.clock=8100;
 check(e.command(*s,0,3,Expedition::DEATH,3,1,0)==Expedition::OK,"death accepted after protection expires");
 e.clock=9100;e.resolveDeath(*s);check(s->players[1].score==5&&s->dots.size()==6,"odd count loses floor half, no transfer");valid192(*s);
 e.resolveDeath(*s);check(s->players[1].score==5,"repeated resolution cannot remove twice");
 e.seats[0].bag[Expedition::DEATH]=1;e.seats[0].ready[Expedition::DEATH]=0;
 check(e.command(*s,0,4,Expedition::DEATH,3,1,0)==Expedition::BLOCKED,"shared victim interval prevents chain execution");
 auto classic=std::make_unique<State>(0,1,192,50);valid(*classic);check(classic->teamCount==6&&!classic->expedition&&classic->items->snapshot(*classic).size()>209,"classic six-seat entry intact");
 state=nullptr;std::cout<<"PASS "<<checks<<" expedition production-core assertions; device/network/balance not implied\n";return 0;
 }catch(const std::exception& ex){std::cerr<<"FAIL "<<ex.what()<<" after "<<checks<<" checks\n";return 1;}}
