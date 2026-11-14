// SPDX-License-Identifier: GPL-3.0-or-later
#define main unused_items_main182
#include "check_items_core.cpp"
#undef main
#include "state_hash.hpp"
int serial182=0;
void order182(State& s,int op,int group,int x=-1,int y=-1){short xx[5]={-5,short(++serial182),short(x),-1,-1},yy[5]={short(op*4+group),short(s.operations->generation[0]),short(y),-1,-1};check(s.operations->command(s,0,xx,yy),"recognized authoritative order");}
void lifetimes182(){
 check(Items::EFFECT_MS==10000,"configured ten seconds");
 for(int kind:{1,2,5,6,8,9,10,11,12,13,14,15,16,17,18,19}){
  auto s=fixture();auto& i=*s->items;i.grant(*s,0,kind,0,0);int team=kind==2?1:0;if(kind==8)check(i.command(*s,0,i.nonce[0],1,0),"choose pact target");
  check(i.until[team][kind]-i.elapsed==10000,"every timed effect lasts10s");int remain=i.snapshot(*s)[25+team*20+kind];check(remain==10000,"HUD/model snapshot starts10000ms");
  step(*s,1428);check(i.elapsed==9996&&i.active(team,kind),"active at9996ms");if(kind==8)check(s->allied(0,1),"pact active before expiry");
  step(*s,1);check(i.elapsed==10003&&!i.active(team,kind),"expired at first step after10s");check(i.snapshot(*s)[25+team*20+kind]==0,"countdown reaches zero");if(kind==8)check(!s->allied(0,1),"pact expired");valid(*s);
 }
 auto s=fixture();auto& i=*s->items;i.grant(*s,0,Items::INVINCIBLE,0,0);step(*s,500);i.grant(*s,0,Items::INVINCIBLE,0,0);check(i.until[0][Items::INVINCIBLE]-i.elapsed==10000,"reacquisition refreshes not stacks");
 i.grant(*s,1,Items::SHATTER,0,0);check(!i.active(0,Items::INVINCIBLE),"counter still cancels");i.grant(*s,0,Items::INVINCIBLE,0,0);check(!i.active(0,Items::INVINCIBLE),"counter still blocks");step(*s,1429);i.grant(*s,0,Items::INVINCIBLE,0,0);check(i.active(0,Items::INVINCIBLE),"counter expires at10s");
 i.grant(*s,0,Items::PHASE,0,0);Dot* d=s->dots[0];int wx=-1,wy=-1;for(int y=8;y<470&&wx<0;y++)for(int x=20;x<780;x++)if(s->map->isWall(x,y)){wx=x;wy=y;break;}check(wx>=0,"internal wall fixture");s->field[d->x][d->y]=nullptr;d->x=wx;d->y=wy;s->field[wx][wy]=d;i.elapsed=i.until[0][Items::PHASE]-4;step(*s,1);check(!s->map->isWall(d->x,d->y),"10s phase expiry safely exits wall");valid(*s);
 i.rewind(*s);for(auto row:i.until)for(int n:row)check(n==0,"rewind clears every timer");
}
std::unique_ptr<State> open182(){auto s=std::make_unique<State>(0,1,182,0);s->dotsPerTeam=100;for(int y=4;y<476;y++)for(int x=16;x<784;x++)s->map->excavate(x,y);for(auto& p:s->players)p.x[0]=p.y[0]=-2;for(int k=0;k<100;k++)add(*s,0,100+k%10,100+k/10);for(int k=0;k<100;k++)add(*s,1,650+k%10,350+k/10);s->items->nextWave=INT_MAX;s->items->capture(*s);s->operations->configure(*s,0);return s;}
void following182(){
 auto s=open182();auto& op=*s->operations;order182(*s,Operations::FOLLOW,3,210,110);step(*s,220);check(op.groups[0][0].x>140,"army moves before finger release");check(op.routes[0][0].distance.empty(),"direct finger avoids per-move navigation field");int x=op.groups[0][0].x;order182(*s,Operations::FOLLOW,3,80,110);step(*s,120);check(op.groups[0][0].x<x,"drag reversal redirects actual particles");
 order182(*s,Operations::SPLIT,3);int ax=op.groups[0][0].x,cx=op.groups[0][2].x;order182(*s,Operations::FOLLOW,1,260,120);step(*s,200);check(op.groups[0][1].x>ax+20,"selected B follows");check(op.groups[0][0].x==ax&&op.groups[0][2].x==cx,"A and C keep prior orders");
 int before=op.groups[0][1].x;step(*s,80);check(op.groups[0][1].x>before,"last target persists without further packets");order182(*s,Operations::STOP,1);before=op.groups[0][1].x;step(*s,100);check(op.groups[0][1].x==before,"stop cancels direct following");
 order182(*s,Operations::BEGIN,1,200,200);order182(*s,Operations::COMMIT,1);check(!op.routes[0][1].tracking&&!op.routes[0][1].points.empty(),"route replaces manual target");order182(*s,Operations::FOLLOW,1,250,220);check(op.routes[0][1].tracking,"follow replaces route");auto h=simulationHash(*s);op.routes[0][1].tracking=false;check(simulationHash(*s)!=h,"follow mode participates in authoritative hash");op.routes[0][1].tracking=true;
 short xx[5]={-5,short(++serial182),120,-1,-1},yy[5]={short(7*4+1),short(op.generation[0]),120,-1,-1};op.clearTeam(*s,0);op.command(*s,0,xx,yy);check(!op.routes[0][1].enabled,"stale command after teleport generation rejected");valid(*s);
 std::vector<uint64_t> first;for(int pass=0;pass<2;pass++){serial182=0;auto q=open182();order182(*q,5,3);for(int k=0;k<30;k++){order182(*q,7,k%3,150+k*3,150+(k%5)*5);step(*q,10);auto hash=simulationHash(*q);if(!pass)first.push_back(hash);else check(hash==first[k],"same follow packets same simulation hash");}}
}
int main(int argc,char** argv){try{check(argc>1,"assets path");assets.root=argv[1];lifetimes182();following182();state=nullptr;std::cout<<"PASS "<<checks<<" follow/timed-item production-core assertions\n";return 0;}catch(const std::exception& e){std::cerr<<"FAIL "<<e.what()<<" after "<<checks<<"\n";return 1;}}
