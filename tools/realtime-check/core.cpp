// Actual headless game core. BASELINE builds the same input fixture against 1.56.
#include "state.hpp"
#include "move.hpp"
#include <iostream>
#include <stdexcept>
#include <memory>
#include <array>
#include <cstdint>
#ifndef BASELINE
#include "model_observation.hpp"
#include "combat.hpp"
#endif
HostAssets assets;HostAssets* acAssets=&assets;
static int checks;
static void ok(bool value,const char* message){checks++;if(!value)throw std::runtime_error(message);}
static uint64_t hash(const State& s){uint64_t h=1469598103934665603ULL;auto mix=[&](uint32_t v){for(int i=0;i<4;i++){h^=(v>>(8*i))&255;h*=1099511628211ULL;}};
 for(const Dot* d:s.dots){mix(d->x);mix(d->y);mix(d->team);mix(d->health);
#ifndef BASELINE
 mix(d->damageQuarter);
#endif
 }for(const auto& p:s.players){mix(p.score);for(int k=0;k<5;k++){mix(p.x[k]);mix(p.y[k]);}}for(int a:s.alliances)mix(a);mix(s.moveRandom->getSeed());mix(s.aiRandom->getSeed());return h;}
static uint64_t battle(int seed,bool paired){auto owned=std::make_unique<State>(0,-2,seed,400);state=owned.get();if(paired)for(int p=0;p<6;p++)state->alliances[p]=0;
 state->players[0].x[0]=state->players[1].x[0]=100;state->players[0].y[0]=state->players[1].y[0]=240;
 int first=-1;for(int t=0;t<1800;t++){Move::stepDots();int changed=0;for(size_t i=0;i<state->dots.size();i++){const Dot* d=state->dots[i];changed+=d->team!=i/400;ok(d->health>=1&&d->health<=MAX_HEALTH,"health bounds");
#ifndef BASELINE
 ok(d->damageQuarter<4,"fraction bounded");if(paired)ok(d->damageQuarter==0,"friendly fractional damage");
#endif
 }int sum=0;for(const auto& p:state->players)sum+=p.score;ok(sum==2400,"score conservation");if(first<0&&changed)first=t+1;if(paired)ok(changed==0,"friendly conversion");
 if(!paired&&(t==599||t==1199||t==1799))std::cout<<"battle seed="<<seed<<" step="<<t+1<<" converted="<<changed<<" first="<<first<<'\n';}
 return hash(*state);
}
int main(int argc,char** argv){try{assets.root=argv[1];
#ifndef BASELINE
 {auto held=std::make_unique<State>(0,-2,13,400);state=held.get();for(auto& p:state->players)p.x[0]=p.y[0]=-2;auto before=hash(*state);for(int i=0;i<80;i++)Move::stepDots();ok(before==hash(*state),"all idle: no motion, attack, healing or random consumption");
   std::vector<std::pair<int,int>> positions;for(const Dot* d:state->dots)positions.emplace_back(d->x,d->y);
   state->players[0].x[0]=400;state->players[0].y[0]=50;for(int i=0;i<800;i++)Move::stepDots();ok(state->players[2].score<400,"idle team remains vulnerable to attacks");for(size_t i=0;i<state->dots.size();i++)if(state->dots[i]->team==2)ok(state->dots[i]->x==positions[i].first&&state->dots[i]->y==positions[i].second,"idle defenders did not move");}
 for(int n=0;n<46;n++){auto s=std::make_unique<State>(0,n,4567,400);state=s.get();auto before=hash(*s);auto dots=s->dots;auto observation=model_observation::snapshot(*s);auto terrain=model_observation::terrain(*s);ok(before==hash(*s)&&dots==s->dots,"observer changed simulation/order/RNG");for(int p=0;p<6;p++){ok(observation[p*8]==400,"snapshot counts");ok(observation[p*8+1]==50,"spawn health snapshot");}bool mask=true;for(int y=0;y<480;y++)for(int x=0;x<800;x++)mask&=bool(terrain[y*800+x])==s->map->isWall(x,y);ok(mask,"exact collision terrain");}
 for(int count:{0,4500}){auto s=std::make_unique<State>(0,-2,13,count);auto before=hash(*s);auto snap=model_observation::snapshot(*s);ok(before==hash(*s),"scale observation mutation");for(int p=0;p<6;p++)ok(snap[p*8]==count,"scale counts");}
 for(int hp:{1,5,10}){Dot d(10,10,1);d.health=hp;Player players[6];for(auto& p:players)p.score=0;players[1].score=1;int contacts=0;while(d.team==1){combat::contact(d,0,players);contacts++;ok(contacts<20,"conversion bounded");}ok(contacts==(hp*4+2)/3,"75 percent contact damage");ok(players[0].score==1&&players[1].score==0&&d.health==5&&d.damageQuarter==0,"conversion score and reset");std::cout<<"health="<<hp<<" contacts="<<contacts<<'\n';}
#endif
 auto a=battle(921,false),b=battle(921,false);ok(a==b,"same-seed host/client input replay");battle(921,true);
 std::cout<<"PASS "<<checks<<" actual C++ assertions, hash="<<std::hex<<a<<"; no GL/device.\n";return 0;
 }catch(const std::exception& e){std::cerr<<"FAIL "<<e.what()<<'\n';return 1;}}
