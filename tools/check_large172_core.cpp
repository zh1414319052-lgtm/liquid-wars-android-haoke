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

void waveGate(){
 std::unique_ptr<State> s;int slot=-1;
 for(int seed=0;seed<64&&slot<0;seed++){s=std::make_unique<State>(0,1,17200+seed,0);s->items->elapsed=s->items->nextWave-7;s->items->begin(*s);for(int k=0;k<6;k++)if(s->items->pickups[k].kind==Items::TELEPORT){slot=k;break;}}
 check(slot>=0,"natural targeted item wave fixture");auto& i=*s->items;check(i.wave==1&&i.nextWave==-1,"first wave waits for clearance");std::set<int> first;for(auto p:i.pickups)if(p.kind>=0)first.insert(p.kind);
 for(int k=0;k<6;k++)if(k!=slot)i.pickups[k].kind=-1;i.pickups[slot].hp=600;int x=i.pickups[slot].x,y=i.pickups[slot].y;auto rng=i.randomState,move=s->moveRandom->getSeed(),ai=s->aiRandom->getSeed();
 step(*s,10000);check(i.wave==1&&i.pickups[slot].kind==Items::TELEPORT&&i.pickups[slot].hp==600,"one half-eaten pickup survives70seconds without timer replacement");check(i.randomState==rng&&s->moveRandom->getSeed()==move&&s->aiRandom->getSeed()==ai,"waiting consumes no refresh/move/AI RNG");
 auto before=simulationHash(*s);i.snapshot(*s);check(before==simulationHash(*s),"read-only waiting HUD snapshot");add(*s,0,x,y);for(auto& p:s->players)p.x[0]=p.y[0]=-2;
 step(*s,599);check(i.pickups[slot].kind==Items::TELEPORT&&i.pickups[slot].hp==1&&i.wave==1,"last HP still blocks next wave");step(*s,1);check(!i.hasPickups()&&i.wave==1&&i.nextWave==i.elapsed+7&&i.pending[0]==Items::TELEPORT,"last HP finishes, pending card counts eaten, next step queued");step(*s,1);check(i.wave==2&&i.hasPickups()&&i.nextWave==-1&&i.pending[0]==Items::TELEPORT,"next1..6 spawn on next rule step without consuming stored card");int count=0;for(auto p:i.pickups)if(p.kind>=0){count++;check(!first.count(p.kind),"new wave avoids previous kinds");}check(count>=1&&count<=6,"quantity still random1..6");
 // Rewind is a deliberate whole-match restart, not an ordinary wave timeout.
 i.grant(*s,0,Items::RESET,x,y);check(i.wave==0&&i.elapsed==0&&i.warmup>0&&!i.hasPickups()&&i.nextWave>=8000&&i.nextWave<=12000,"rewind restores initial first-wave delay");std::cout<<"PASS clear-all wave gate, last HP,70s retention, pending card, RNG and rewind"<<std::endl;
 std::set<int> quantities,kinds;for(int seed=0;seed<128;seed++){auto a=std::make_unique<State>(0,1,17200+seed,0),b=std::make_unique<State>(5,1,17200+seed,0);for(int wave=0;wave<12;wave++){for(auto st:{a.get(),b.get()}){if(st->items->wave){for(auto& p:st->items->pickups)p.kind=-1;st->items->end(*st);}st->items->elapsed=st->items->nextWave-7;st->items->begin(*st);}check(hash(*a)==hash(*b),"cleared wave deterministic across views");int n=0;for(auto p:a->items->pickups)if(p.kind>=0){n++;kinds.insert(p.kind);}quantities.insert(n);}}
 check(quantities==std::set<int>({1,2,3,4,5,6})&&kinds.size()==18,"1536 cleared waves cover18 types and6 counts");std::cout<<"PASS1536 gated random waves /128seeds"<<std::endl;
}
int main(int argc,char** argv){try{check(argc>=2,"assets path");assets.root=argv[1];std::string mode=argc>2?argv[2]:"rules";
 if(mode=="rules"){health();phaseCombat();effects();replay();waveGate();}
 else if(mode=="fixture"){bool found=false;for(int seed=100;seed<2000&&!found;seed++){auto s=std::make_unique<State>(0,1,seed,100);for(auto& t:s->players)t.x[0]=t.y[0]=-2;s->items->elapsed=s->items->nextWave-7;s->items->begin(*s);if(std::count_if(std::begin(s->items->pickups),std::end(s->items->pickups),[](auto p){return p.kind>=0;})!=1)continue;auto p=*std::find_if(std::begin(s->items->pickups),std::end(s->items->pickups),[](auto p){return p.kind>=0;});if(p.kind==Items::RESET)continue;int nearest=INT_MAX;for(auto d:s->dots)nearest=std::min(nearest,(d->x-p.x)*(d->x-p.x)+(d->y-p.y)*(d->y-p.y));if(nearest<18*18)continue;for(int t=0;t<6&&!found;t++){int x=s->birthX[t],y=s->birthY[t],dx=p.x-x,dy=p.y-y,n=std::max(std::abs(dx),std::abs(dy));bool clear=true;for(int k=0;k<=n;k++)if(s->map->isWall(x+dx*k/std::max(1,n),y+dy*k/std::max(1,n)))clear=false;if(clear&&dx*dx+dy*dy<110*110){std::cout<<"FIXTURE seed="<<seed<<" team="<<t<<" kind="<<p.kind<<" x="<<p.x<<" y="<<p.y<<std::endl;found=true;}}}check(found,"natural single pickup fixture");}
 else if(mode=="maps"){for(int map=52;map<=59;map++){for(int count:{0,100,400,4500})for(int seed=0;seed<16;seed++){auto s=std::make_unique<State>(seed%6,map,17200+seed,count);check(s->map->width==1600&&s->map->height==960,"each new large map is4x");valid(*s);auto words=model_observation::snapshot(*s);for(size_t k=217;k<words.size();k++){unsigned v=words[k];auto d=s->dots[k-217];check((v&2047)==d->x&&((v>>11)&1023)==d->y&&((v>>21)&7)==d->team,"11-bit large snapshot exact");}}for(int f=0;f<4;f++){Map a(map,0),b(map,f);for(int y=9;y<951;y+=41)for(int x=17;x<1583;x+=47)check(b.isWall(x,y)==a.isWall((f&1)?1599-x:x,(f&2)?959-y:y),"four flips exact collision mirror");}std::cout<<"PASS map"<<map<<" placement16seeds x0/100/400/4500 and four transforms"<<std::endl;}}
 else throw std::runtime_error("unknown mode");state=nullptr;std::cout<<"PASS "<<checks<<" production-core assertions ("<<mode<<")"<<std::endl;return 0;
 }catch(const std::exception& e){std::cerr<<"FAIL "<<e.what()<<" after "<<checks<<" checks"<<std::endl;return 1;}}
