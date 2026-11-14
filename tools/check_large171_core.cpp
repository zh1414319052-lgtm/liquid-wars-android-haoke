// SPDX-License-Identifier: GPL-3.0-or-later
#define main previous_items_main
#include "check_items_core.cpp"
#undef main
#include "state_hash.hpp"
#include "minimap_sight.hpp"
std::pair<int,int> openPatch(State& s){for(int y=30;y<s.map->height-30;y+=12)for(int x=35;x<s.map->width-40;x+=12){bool ok=true;for(int yy=-12;yy<=12&&ok;yy++)for(int xx=-12;xx<=12;xx++)if(s.map->isWall(x+xx,y+yy)||s.field[x+xx][y+yy]){ok=false;break;}if(ok)return {x,y};}throw std::runtime_error("No open fixture patch");}
void put(State& s,Dot* d,int x,int y){s.field[d->x][d->y]=nullptr;check(!s.field[x][y],"fixture overlap");d->x=x;d->y=y;s.field[x][y]=d;}
std::unique_ptr<State> healthFixture(int count=4){auto s=std::make_unique<State>(0,1,171,0);auto p=openPatch(*s);for(int k=0;k<count;k++)add(*s,0,p.first+k,p.second);for(auto& t:s->players)t.x[0]=t.y[0]=-2;s->items->nextWave=INT_MAX;s->items->pickups[0]={Items::DOUBLE,p.first,p.second};s->items->capture(*s);return s;}
void health(){
 auto s=healthFixture();auto& i=*s->items;auto a=s->moveRandom->getSeed(),b=s->aiRandom->getSeed();
 step(*s,1);check(i.pickups[0].hp==1196&&i.pickups[0].kind==Items::DOUBLE&&s->players[0].score==4,"touch only chips HP, never instant activation");step(*s,99);check(i.pickups[0].hp==800,"four contacts capped damage");
 auto away=openPatch(*s);int oldx=i.pickups[0].x,oldy=i.pickups[0].y;for(int k=0;k<4;k++)put(*s,s->dots[k],away.first+k,away.second);step(*s,100);check(i.pickups[0].hp==800,"leaving retains HP without ticking");
 for(int k=0;k<4;k++)put(*s,s->dots[k],oldx+k,oldy);i.until[0][Items::FREEZE]=i.elapsed+10000;step(*s,60);check(i.pickups[0].hp==800,"frozen army cannot devour regular pickup");i.until[0][Items::FREEZE]=0;
 int activeSteps=100;while(i.pickups[0].kind>=0&&activeSteps<400){step(*s,1);activeSteps++;}check(activeSteps==300&&s->players[0].score==8,"1200 HP needs300 active steps /2.1seconds then doubles once");step(*s,20);check(s->players[0].score==8&&i.last.serial==1,"finished pickup cannot trigger twice");check(s->moveRandom->getSeed()==a&&s->aiRandom->getSeed()==b,"devouring consumes neither rule random stream");valid(*s);
 auto one=healthFixture(1);step(*one,1199);check(one->items->pickups[0].hp==1&&one->players[0].score==1,"one particle requires sustained devouring");step(*one,1);check(one->players[0].score==2,"one particle finishes at8.4seconds");
 auto r=healthFixture();step(*r,30);r->items->grant(*r,0,Items::RESET,0,0);check(r->items->pickups[0].kind==-1&&r->items->pickups[0].hp==1200&&r->items->pickups[0].damage==std::array<int,6>{},"rewind clears pickup HP/contributions");auto before=simulationHash(*r);r->items->snapshot(*r);check(before==simulationHash(*r),"HP snapshots read only");
 // Enemy cannot steal a mostly eaten pickup with one last contact.
 auto c=healthFixture();auto p=c->items->pickups[0];step(*c,200);auto other=openPatch(*c);auto enemy=add(*c,1,other.first,other.second);for(int k=0;k<4;k++)put(*c,c->dots[k],other.first+k+1,other.second);put(*c,enemy,p.x,p.y);step(*c,400);check(c->items->pickups[0].kind<0&&c->players[0].score==8&&c->players[1].score==1,"greatest living contribution wins contested pickup");
 std::cout<<"PASS pickup HP/contact/leave/freeze/2.1s/8.4s/contest/rewind/read-only/RNG\n";
}
void phaseCombat(){
 auto s=fixture();int wx=-1,wy=-1;for(int y=8;y<472&&wx<0;y++)for(int x=18;x<782;x++)if(s->map->isWall(x,y)&&!s->map->isWall(x-1,y)&&!s->field[x-1][y]&&!s->field[x][y]){wx=x;wy=y;break;}check(wx>=0,"phase wall fixture");
 Dot* a=s->dots[0],*b=s->dots[100];put(*s,a,wx,wy);put(*s,b,wx-1,wy);s->items->grant(*s,0,Items::PHASE,wx,wy);
 check(!s->items->canEnter(*s,1,wx,wy)&&s->items->canContact(*s,1,wx,wy),"normal enemy attacks phased occupant in wall without walking through wall");check(s->items->canContact(*s,0,wx-1,wy),"phased attacker contacts normal enemy");
 int ah=a->health,bh=b->health;for(int k=0;k<4;k++){s->items->contact(*s,*a,*b);s->items->contact(*s,*b,*a);}check(a->health<ah&&b->health<bh,"phase alone no immunity either direction");
 s->alliances[1]=s->alliances[0];check(!s->items->canContact(*s,0,wx-1,wy),"phased ally remains nonhostile");auto hp=a->health;s->items->contact(*s,*a,*b);check(a->health==hp,"ally cannot swallow through wall");s->alliances[1]=1;s->items->grant(*s,0,Items::INVINCIBLE,wx,wy);s->items->contact(*s,*a,*b);check(a->health==hp,"separate invincibility still applies");
 std::cout<<"PASS phase only walls, bidirectional combat, allies and separate invincibility\n";
}
int main(int argc,char** argv){try{check(argc>=2,"assets path");assets.root=argv[1];health();phaseCombat();effects();schedule();replay();
 for(int count:{0,100,400,4500})for(int seed=0;seed<128;seed++){auto s=std::make_unique<State>(seed%6,52,seed,count);check(s->map->width==3200&&s->map->height==960,"8x real world");valid(*s);auto words=model_observation::snapshot(*s);for(size_t k=217;k<words.size();k++){unsigned v=words[k];auto d=s->dots[k-217];check((v&4095)==d->x&&((v>>12)&1023)==d->y&&((v>>22)&7)==d->team,"12-bit x model snapshot exact");}if(seed==0){auto h=simulationHash(*s);minimap_sight::snapshot(*s,false);check(h==simulationHash(*s),"8x sight read only");}}
 for(int map:{1,50,51,52}){auto a=std::make_unique<State>(0,map,17152,100),b=std::make_unique<State>(5,map,17152,100);for(int f=0;f<600;f++){for(auto s:{a.get(),b.get()}){if(f==100)s->items->pickups[0]={Items::TELEPORT,int(s->dots[0]->x),int(s->dots[0]->y)};for(int t=0;t<6;t++)s->players[t].x[0]=s->players[t].y[0]=-2;step(*s,1);}if(f%14==0)check(hash(*a)==hash(*b),"HP replay deterministic across viewpoints");}}
 state=nullptr;std::cout<<"PASS "<<checks<<" production-core assertions; not Android performance/human proof\n";return 0;
 }catch(const std::exception& e){std::cerr<<"FAIL "<<e.what()<<" after "<<checks<<" checks\n";return 1;}}
