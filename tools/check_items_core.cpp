// SPDX-License-Identifier: GPL-3.0-or-later
// Tests the production State/Move/Map/Items code. Host GLES stubs never render.
#include "state.hpp"
#include "move.hpp"
#include "model_observation.hpp"
#include <memory>
#include <iostream>
#include <stdexcept>
#include <set>
#include <climits>
HostAssets assets;HostAssets* acAssets=&assets;
int checks=0;
void check(bool v,const char* why){checks++;if(!v)throw std::runtime_error(why);}
uint64_t hash(State& s){uint64_t h=1469598103934665603ull;auto mix=[&](int n){h^=uint32_t(n);h*=1099511628211ull;};
 for(auto d:s.dots){mix(d->x);mix(d->y);mix(d->team);mix(d->health);mix(d->damageQuarter);mix(d->armorCarry);}
 for(auto& p:s.players){mix(p.score);for(int k=0;k<5;k++){mix(p.x[k]);mix(p.y[k]);}}
 mix(s.moveRandom->getSeed());mix(s.aiRandom->getSeed());mix(s.items->randomState);
 for(int n:s.items->snapshot(s))mix(n);for(int n:s.map->openings())mix(n);
 return h;
}
Dot* add(State& s,int t,int x,int y){check(!s.map->isWall(x,y)&&!s.field[x][y],"fixture placement");auto d=new Dot(x,y,t);s.dots.push_back(d);s.field[x][y]=d;s.players[t].score++;return d;}
std::unique_ptr<State> fixture(){auto s=std::make_unique<State>(0,1,166,100);for(int t=0;t<6;t++)s->players[t].x[0]=s->players[t].y[0]=-2;s->items->nextWave=INT_MAX;s->items->capture(*s);return s;}
void valid(State& s){int counts[6]={};std::set<int> positions;
 for(auto d:s.dots){check(d->team<6&&d->health>0&&d->health<=10,"dot health/team");check((!s.map->isWall(d->x,d->y)||s.items->active(d->team,Items::PHASE)),"particle inside wall");check(s.field[d->x][d->y]==d,"field identity");check(positions.insert(d->y*s.map->width+d->x).second,"overlap");counts[d->team]++;}
 for(int t=0;t<6;t++)check(s.players[t].score==counts[t],"dynamic score mismatch");check(s.dots.size()<=Items::MAX_PARTICLES,"capacity");
}
void step(State& s,int n){state=&s;for(int k=0;k<n;k++)Move::stepDots();}

void effects(){
 auto s=fixture();auto& i=*s->items;auto d=s->dots[0];auto enemy=s->dots[100];int x=d->x,y=d->y;
 i.grant(*s,0,Items::DOUBLE,x,y);check(s->players[0].score==200,"double actual army");valid(*s);
 i.grant(*s,1,Items::ANTIGROW,enemy->x,enemy->y);i.grant(*s,0,Items::DOUBLE,x,y);check(s->players[0].score==200,"anti growth blocks double");i.until[1][Items::ANTIGROW]=0;
 i.grant(*s,0,Items::FREEZE,x,y);check(i.moves(*s,*enemy)==0&&i.moves(*s,*d)==1,"freeze enemies10s");int ex=enemy->x,ey=enemy->y;step(*s,100);check(enemy->x==ex&&enemy->y==ey,"frozen actually immobile");
 i.grant(*s,1,Items::THAW,ex,ey);check(i.moves(*s,*enemy)==1,"thaw immediate");i.grant(*s,0,Items::FREEZE,x,y);check(i.moves(*s,*enemy)==1,"thaw immunity");
 i.grant(*s,0,Items::INVINCIBLE,x,y);auto hp=d->health;for(int n=0;n<100;n++)i.contact(*s,*d,*enemy);check(d->health==hp&&d->team==0,"invincible immune capture");
 i.grant(*s,1,Items::SHATTER,ex,ey);check(!i.active(0,Items::INVINCIBLE),"shatter strips");i.grant(*s,0,Items::INVINCIBLE,x,y);check(!i.active(0,Items::INVINCIBLE)&&i.last.amount==-1,"shatter blocks new");
 i.until[0][Items::FREEZE]=0;i.until[1][Items::FREEZE]=0;
 i.grant(*s,0,Items::TELEPORT,x,y);int token=i.nonce[0];check(!i.command(*s,0,token+1,700,400),"wrong nonce rejected");int tx=enemy->x,ty=enemy->y;check(!i.command(*s,0,token,-1,0)&&i.pending[0]==Items::TELEPORT,"invalid target retains item");
 i.grant(*s,1,Items::ANCHOR,ex,ey);check(!i.command(*s,0,token,tx,ty)&&i.pending[0]==Items::TELEPORT,"spatial anchor blocks");i.until[1][Items::ANCHOR]=0;
 int dest=-1;for(int yy=10;yy<470&&dest<0;yy++)for(int xx=20;xx<780;xx++)if(!s->map->isWall(xx,yy)&&!s->field[xx][yy]){dest=yy*800+xx;break;}
 check(i.command(*s,0,token,dest%800,dest/800)&&i.last.amount==200,"designated whole army teleport");check(!i.command(*s,0,token,dest%800,dest/800),"duplicate command does not repeat");valid(*s);
 // Invincibility and one-use ward protect death; no local controller chooses targets.
 i.grant(*s,0,Items::DEATH,d->x,d->y);token=i.nonce[0];i.until[1][Items::SHATTER]=0;i.grant(*s,1,Items::INVINCIBLE,ex,ey);check(!i.command(*s,0,token,1,0),"invincible blocks death");i.until[1][Items::INVINCIBLE]=0;
 i.grant(*s,1,Items::WARD,ex,ey);check(i.command(*s,0,token,1,0)&&s->players[1].score==100&&!i.active(1,Items::WARD),"ward consumes attempt not army");
 i.grant(*s,0,Items::DEATH,d->x,d->y);token=i.nonce[0];std::vector<Dot*> order;for(auto a:s->dots)if(a->team!=1)order.push_back(a);check(i.command(*s,0,token,1,0)&&s->players[1].score==0,"selected death removes exact enemy");check(order==s->dots,"survivor dots retain order");valid(*s);
 i.grant(*s,0,Items::FRIEND,d->x,d->y);check(i.command(*s,0,i.nonce[0],2,0)&&s->allied(0,2),"friend merges alliance");auto ally=s->dots[100];check(ally->team==2,"fixture ally");hp=ally->health;for(int n=0;n<50;n++)i.contact(*s,*ally,*d);check(ally->team==2&&ally->health==hp,"new ally no swallowing");
 i.grant(*s,2,Items::LIBERTY,ally->x,ally->y);check(!s->allied(0,2),"liberty restores independence");i.grant(*s,0,Items::FRIEND,d->x,d->y);check(!i.command(*s,0,i.nonce[0],2,0),"liberty blocks friendship");
 // Actual wall fixture, no image/authority replacements.
 auto q=fixture();auto a=q->dots[0];int wx=-1,wy=-1;for(int yy=8;yy<472&&wx<0;yy++)for(int xx=17;xx<783;xx++)if(q->map->isWall(xx,yy)&&!q->map->isWall(xx-1,yy)&&!q->field[xx-1][yy]){wx=xx;wy=yy;break;}
 check(wx>=0,"wall adjacent floor");q->field[a->x][a->y]=nullptr;a->x=wx-1;a->y=wy;q->field[a->x][a->y]=a;
 q->items->grant(*q,0,Items::PHASE,a->x,a->y);check(q->items->canEnter(*q,0,wx,wy)&&!q->items->canEnter(*q,0,1,1),"phase only internal walls");
 q->field[a->x][a->y]=nullptr;a->x=wx;a->y=wy;q->field[wx][wy]=a;q->items->grant(*q,1,Items::PHASENAIL,200,200);check(!q->map->isWall(a->x,a->y)&&!q->items->active(0,Items::PHASE),"phase nail safely ejects");q->items->grant(*q,0,Items::PHASE,a->x,a->y);check(!q->items->active(0,Items::PHASE),"phase suppression");
 q->items->grant(*q,1,Items::ROCKSEAL,200,200);q->items->grant(*q,0,Items::QUARRY,a->x,a->y);check(!q->items->active(0,Items::QUARRY),"rock seal prevents quarry");q->items->until[1][Items::ROCKSEAL]=0;
 q->items->grant(*q,0,Items::QUARRY,a->x,a->y);int rev=q->map->revision();step(*q,28);check(q->map->revision()>rev&&q->players[0].score>100,"quarry actual excavation and growth");valid(*q);
 auto reset=fixture();auto initial=reset->dots.size();auto walls=model_observation::terrain(*reset);auto rng=reset->moveRandom->getSeed();reset->items->grant(*reset,0,Items::DOUBLE,200,200);reset->map->excavate(wx,wy);reset->items->grant(*reset,1,Items::CAUSAL,200,200);reset->items->grant(*reset,0,Items::RESET,200,200);check(reset->items->epoch==0&&reset->items->last.amount==-1,"causal lock blocks rewind");reset->items->until[1][Items::CAUSAL]=0;reset->items->grant(*reset,0,Items::RESET,200,200);
 check(reset->items->epoch==1&&reset->items->elapsed==0&&reset->items->warmup==3003,"rewind clock and warmup");check(reset->dots.size()==initial&&reset->moveRandom->getSeed()==rng&&model_observation::terrain(*reset)==walls,"rewind restores actual core/RNG/terrain");auto first=reset->dots[0];int ox=first->x,oy=first->y;step(*reset,428);check(first->x==ox&&first->y==oy&&reset->items->elapsed==0,"rewind countdown no movement");valid(*reset);
 std::cout<<"18 effects and nine explicit counter relations, exact targeting/nonce/restart PASS\n";
}
void schedule(){std::set<int> kinds,counts;for(int map:{1,12,28,49})for(int seed=0;seed<32;seed++){auto s=std::make_unique<State>(0,map,seed,0);auto rng=s->moveRandom->getSeed(),ai=s->aiRandom->getSeed();for(int w=0;w<3;w++){s->items->elapsed=s->items->nextWave-7;s->items->begin(*s);int count=0;std::set<int> wave;for(auto p:s->items->pickups)if(p.kind>=0){count++;check(p.kind<18&&!s->map->isWall(p.x,p.y),"only18 valid floor items");check(wave.insert(p.kind).second,"no duplicate kind");kinds.insert(p.kind);}check(count>=1&&count<=6,"random1..6 wave");counts.insert(count);}check(s->moveRandom->getSeed()==rng&&s->aiRandom->getSeed()==ai,"independent item RNG");}check(kinds.size()==18&&counts.size()==6,"all18 and all6 quantities covered");std::cout<<"128 seeds, random1..6 waves and all18 kinds PASS\n";}
void replay(){auto a=std::make_unique<State>(0,12,6606,100),b=std::make_unique<State>(5,12,6606,100);for(int f=0;f<1400;f++){for(auto s:{a.get(),b.get()}){if(f%70==0){int k=f/70%18;int team=0;if(s->players[team].score>0){s->items->grant(*s,team,k,200,200);if(k==Items::TELEPORT||k==Items::DEATH||k==Items::FRIEND){s->players[0].x[4]=-3;s->players[0].y[4]=s->items->nonce[0];s->players[0].x[3]=k==Items::TELEPORT?600:1;s->players[0].y[3]=k==Items::TELEPORT?300:0;}}}step(*s,1);}if(f%7==0)check(hash(*a)==hash(*b),"host/client full state lockstep");}valid(*a);valid(*b);std::cout<<"1400-step identical inputs all18 effects deterministic replay PASS\n";}
void findSeeds(){bool done[18]={};int total=0;for(int seed=100;seed<10000&&total<18;seed++){auto s=std::make_unique<State>(0,1,seed,100);s->items->elapsed=s->items->nextWave-7;s->items->begin(*s);for(auto item:s->items->pickups)if(item.kind>=0&&!done[item.kind])for(int t=0;t<6;t++){int x=s->birthX[t],y=s->birthY[t],dx=item.x-x,dy=item.y-y,dist=dx*dx+dy*dy,n=std::max(std::abs(dx),std::abs(dy));bool clear=true;for(int k=0;k<=n;k++)if(s->map->isWall(x+dx*k/std::max(1,n),y+dy*k/std::max(1,n)))clear=false;if(clear&&dist>=25*25&&dist<105*105){std::cout<<item.kind<<","<<seed<<","<<t<<","<<item.x<<","<<item.y<<"\n";done[item.kind]=true;total++;break;}}}check(total==18,"all natural fixtures found");}
int main(int argc,char** argv){try{check(argc>1,"assets required");assets.root=argv[1];if(argc>2){findSeeds();return 0;}effects();schedule();replay();state=nullptr;std::cout<<"PASS "<<checks<<" production-core assertions; not device/performance/human proof\n";return 0;}catch(const std::exception& e){std::cerr<<"FAIL "<<e.what()<<" after "<<checks<<" checks\n";return 1;}}
