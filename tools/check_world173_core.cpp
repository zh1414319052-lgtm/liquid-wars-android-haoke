#define main previous_items_main
#include "check_items_core.cpp"
#undef main
#include "state_hash.hpp"
#include "minimap_sight.hpp"
#include "ai_tactics.hpp"
#include "ai.hpp"
void pairs(){
 const int power[]={0,1,2,3,4,5,6,7,8,18},counter[]={9,10,11,12,13,14,15,16,17,19};std::set<int> counts,kinds;
 for(int seed=0;seed<64;seed++){auto s=std::make_unique<State>(0,1,17300+seed,0);int resets=0;
  for(int wave=0;wave<32;wave++){for(auto& p:s->items->pickups)p.kind=-1;s->items->end(*s);s->items->elapsed=std::max(s->items->elapsed,s->items->nextWave-7);s->items->begin(*s);std::set<int> now;for(auto p:s->items->pickups)if(p.kind>=0){check(now.insert(p.kind).second,"no duplicate kind");kinds.insert(p.kind);if(p.kind==Items::RESET)resets++;}check(resets<=1,"RESET appears at most once despite rewind");
   for(int k=0;k<10;k++)if(now.count(power[k]))check(now.count(counter[k]),"every power has same-wave counter");counts.insert(int(now.size()));
   if(wave==12){bool locked=s->items->resetSpawned;s->items->rewind(*s);check(s->items->resetSpawned==locked,"rewind cannot restore appearance");s->items->warmup=0;}
  }
 }
 check(counts==std::set<int>({1,2,3,4,5,6})&&kinds.size()==20,"all quantities and20 kinds sampled");std::cout<<"PASS2048 paired waves/64seeds/once-reset across rewind/all20 kinds"<<std::endl;
}
void balance(){
 auto s=fixture();auto& i=*s->items;auto d=s->dots[0];auto enemy=s->dots[100];int x=d->x,y=d->y;
 i.grant(*s,0,Items::DOUBLE,x,y);check(s->players[0].score==125,"growth25 percent not doubling");valid(*s);
 i.grant(*s,0,Items::FREEZE,x,y);step(*s,357);check(i.moves(*s,*enemy)==0,"freeze2499ms");step(*s,1);check(i.moves(*s,*enemy)==1,"freeze ends2506ms");i.grant(*s,1,Items::THAW,enemy->x,enemy->y);i.grant(*s,0,Items::FREEZE,x,y);check(i.moves(*s,*enemy)==1,"thaw blocks");
 i.grant(*s,0,Items::QUARRY,x,y);check(i.quarryBudget[0]==25&&i.until[0][Items::QUARRY]-i.elapsed==5000,"dig growth/time budget");
 auto a=fixture();auto& z=*a->items;z.grant(*a,0,Items::DEATH,0,0);check(z.command(*a,0,z.nonce[0],1,0),"death mark accepted");check(a->players[1].score==100,"mark no immediate elimination");step(*a,428);check(a->players[1].score==100,"3s counterplay window");step(*a,1);check(a->players[1].score==80,"mark only20percent, no gift to caster");check(a->players[0].score==100,"no free capture from mark");valid(*a);
 auto b=fixture();b->items->grant(*b,0,Items::DEATH,0,0);b->items->command(*b,0,b->items->nonce[0],1,0);b->items->grant(*b,1,Items::WARD,0,0);step(*b,430);check(b->players[1].score==100&&!b->items->active(1,Items::WARD),"ward blocks delayed settlement once");
 auto c=fixture();c->items->grant(*c,0,Items::FRIEND,0,0);check(c->items->command(*c,0,c->items->nonce[0],1,0),"ceasefire accepted");check(c->allied(0,1)&&c->alliances[0]!=c->alliances[1],"temporary friendly not shared victory");auto hp=c->dots[0]->health;c->items->contact(*c,*c->dots[0],*c->dots[100]);check(c->dots[0]->health==hp,"pact prevents combat");step(*c,1143);check(!c->allied(0,1),"pact expires after8s");c->items->grant(*c,0,Items::FRIEND,0,0);check(c->items->command(*c,0,c->items->nonce[0],1,0),"new pact");c->items->grant(*c,1,Items::LIBERTY,0,0);check(!c->allied(0,1)&&!c->items->active(0,Items::FRIEND),"declaration clears pact and its displayed timer");c->alliances[1]=c->alliances[0];check(c->allied(0,1),"original teams preserved");valid(*c);
 std::cout<<"PASS growth cap/freeze/thaw/dig budget/death warning and finite loss/ward/temporary pact/permanent victory separation"<<std::endl;
}
void cloak(){
 auto s=fixture();auto& i=*s->items;i.grant(*s,1,Items::INVISIBLE,0,0);check(i.concealedFrom(*s,0,1)&&!i.concealedFrom(*s,1,1),"enemy hidden, own visible");auto before=simulationHash(*s);auto mini=minimap_sight::snapshot(*s,false);for(int k=0;k<mini[4];k++)check(mini[5+k*4]!=1,"hidden enemy absent minimap");auto all=minimap_sight::snapshot(*s,true);bool saw=false;for(int k=0;k<all[4];k++)saw|=all[5+k*4]==1;check(saw,"observer sees hidden army");check(before==simulationHash(*s),"visibility reads do not alter hash/RNG");
 auto d=s->dots[100];auto enemy=s->dots[0];int hp=d->health;for(int k=0;k<4;k++)i.contact(*s,*d,*enemy);check(d->health<hp,"cloak no invulnerability");
 s->alliances[0]=s->alliances[1];check(!i.concealedFrom(*s,0,1),"allies see cloak");s->alliances[0]=0;i.grant(*s,0,Items::REVEAL,0,0);check(!i.active(1,Items::INVISIBLE),"reveal clears cloak");i.grant(*s,1,Items::INVISIBLE,0,0);check(!i.active(1,Items::INVISIBLE)&&i.last.amount==-1,"reveal prevents fresh cloak");
 auto a=fixture();a->items->grant(*a,1,Items::INVISIBLE,0,0);step(*a,857);check(a->items->active(1,Items::INVISIBLE),"cloak5999ms");step(*a,1);check(!a->items->active(1,Items::INVISIBLE),"cloak expiry6006ms");std::cout<<"PASS cloak allies/enemies/spectator/minimap/normal combat/reveal/6s/read-only hash"<<std::endl;
}
void replay173(){std::unique_ptr<State> a,b;for(int pass=0;pass<2;pass++){auto s=std::make_unique<State>(pass*5,54,17354,100);for(int t=0;t<6;t++)s->players[t].x[0]=s->players[t].y[0]=-2;
 for(int n=0;n<1800;n++){if(n%90==0)s->items->grant(*s,(n/90)%6,(n/90)%20,200,200);step(*s,1);}valid(*s);if(pass==0)a=std::move(s);else b=std::move(s);}check(simulationHash(*a)==simulationHash(*b),"all20 effect same input same hash independent of viewer");std::cout<<"PASS1800step20effect cross-view hash replay"<<std::endl;}
void find173(){std::set<int> kinds;for(int seed=100;seed<3000&&kinds.size()<3;seed++){auto s=std::make_unique<State>(0,1,seed,100);s->items->elapsed=s->items->nextWave-7;s->items->begin(*s);for(auto p:s->items->pickups)if((p.kind==18||p.kind==3||p.kind==4)&&!kinds.count(p.kind))for(int t=0;t<6;t++){int x=s->birthX[t],y=s->birthY[t],dx=p.x-x,dy=p.y-y,n=std::max(abs(dx),abs(dy));bool clear=true;for(int k=0;k<=n;k++)if(s->map->isWall(x+dx*k/std::max(1,n),y+dy*k/std::max(1,n)))clear=false;if(clear&&dx*dx+dy*dy>25*25&&dx*dx+dy*dy<110*110){std::cout<<"FIXTURE "<<p.kind<<" seed "<<seed<<" team "<<t<<" xy "<<p.x<<","<<p.y<<std::endl;kinds.insert(p.kind);break;}}}check(kinds.size()==3,"natural cloak teleport reset fixtures");}
void ai173(){
 auto s=std::make_unique<State>(0,1,17301,0);int x=-1,y=-1;for(int yy=24;yy<450&&x<0;yy++)for(int xx=24;xx<750;xx++){bool free=true;for(int dy=-8;dy<=8&&free;dy++)for(int dx=-8;dx<=8;dx++)if(s->map->isWall(xx+dx,yy+dy)){free=false;break;}if(free){x=xx;y=yy;break;}}
 check(x>=0,"open AI sight fixture");add(*s,0,x-4,y);add(*s,1,x+4,y);state=s.get();s->items->grant(*s,1,Items::INVISIBLE,x+4,y);
 auto rng=s->aiRandom->getSeed();int nearest=AI::getNearestDot(0,short(x-4),short(y));check((nearest>>16)==x-4&&(nearest&65535)==y&&s->aiRandom->getSeed()==rng,"classic AI cannot track hidden target");
 AiTactics planner(800,480,[&](int xx,int yy){return s->map->isWall(xx,yy);});std::array<unsigned,6> mask{};mask[0]=2;
 auto hidden=planner.plan(s->dots,nullptr,&mask);check(hidden[0]==-1&&hidden[5]==-1,"hard AI has no goal for hidden-only opponent");check(hidden[10]>=0,"cloaked team still sees normal opponent");
 s->items->grant(*s,0,Items::REVEAL,x-4,y);mask[0]=0;auto revealed=planner.plan(s->dots,nullptr,&mask);check(revealed[0]>=0,"hard AI can reacquire after reveal");std::cout<<"PASS classic/hard AI actual hidden-target exclusion and reveal reacquisition"<<std::endl;
}
void counters173(){
 const int power[10]={0,1,2,3,4,5,6,7,8,18},counter[10]={9,10,11,12,13,14,15,16,17,19};
 for(int k=0;k<10;k++){auto s=fixture();auto& i=*s->items;i.grant(*s,1,counter[k],0,0);check(i.until[1][counter[k]]-i.elapsed==(counter[k]==16?10000:8000),"counter lifetime matches displayed table");i.grant(*s,0,power[k],0,0);
 switch(power[k]){
 case Items::DOUBLE:check(s->players[0].score==100,"anti growth prevents instant gain");break;
 case Items::QUARRY:check(!i.active(0,Items::QUARRY),"rock seal blocks digging");break;
 case Items::FREEZE:check(!i.active(1,Items::FREEZE),"thaw protects its holder");break;
 case Items::TELEPORT:check(!i.command(*s,0,i.nonce[0],s->birthX[0],s->birthY[0])&&i.pending[0]==3,"anchor blocks and retains teleport");break;
 case Items::RESET:check(i.epoch==0&&i.last.amount==-1,"causal prevents rewind, not replay appearance");break;
 case Items::PHASE:check(!i.active(0,Items::PHASE),"phase nail blocks crossing");break;
 case Items::INVINCIBLE:check(!i.active(0,Items::INVINCIBLE),"shatter blocks immunity");break;
 case Items::DEATH:check(i.command(*s,0,i.nonce[0],1,0),"death warning can be set before ward settles");step(*s,430);check(s->players[1].score==100&&!i.active(1,Items::WARD),"ward absorbs one delayed mark");break;
 case Items::FRIEND:check(!i.command(*s,0,i.nonce[0],1,0)&&!s->allied(0,1)&&i.pending[0]==8,"declaration blocks forced ceasefire and retains card");break;
 case Items::INVISIBLE:check(!i.active(0,Items::INVISIBLE),"reveal prevents cloak");break;
 }
 }
 for(auto row:std::vector<std::pair<int,int>>{{Items::QUARRY,5000},{Items::PHASE,6000},{Items::INVINCIBLE,4000},{Items::INVISIBLE,6000}}){auto s=fixture();s->items->grant(*s,0,row.first,0,0);check(s->items->until[0][row.first]-s->items->elapsed==row.second,"power durations match player-facing table");}
 std::cout<<"PASS10 real counter interactions and bounded effect lifetime table"<<std::endl;
}
int main(int argc,char**argv){try{assets.root=argv[1];if(argc>2){if(std::string(argv[2])=="ai")ai173();else if(std::string(argv[2])=="counters")counters173();else find173();}else{pairs();balance();cloak();replay173();}state=nullptr;std::cout<<"PASS "<<checks<<" production-core assertions"<<std::endl;return 0;}catch(const std::exception&e){std::cerr<<"FAIL "<<e.what()<<" after "<<checks<<std::endl;return 1;}}
