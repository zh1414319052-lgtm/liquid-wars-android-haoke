// Copyright (c) 2026 Haoke. Test-only controlled fixtures, never shipped in APK.
#include "state.hpp"
#include "move.hpp"
#include "flow_motion.hpp"
#include "liquidfun_motion.hpp"
#include "state_hash.hpp"
#include <memory>
#include <iostream>
#include <fstream>
#include <chrono>
#include <stdexcept>
#include <climits>
#include <unordered_set>
HostAssets assets;HostAssets* acAssets=&assets;
int checks=0;
void require(bool ok,const char* why){++checks;if(!ok)throw std::runtime_error(why);}
std::unique_ptr<State> fixture(){
 auto s=std::make_unique<State>(0,1,209,0);delete s->map;s->map=new Map(999);
 s->field.reset(s->map->width,s->map->height);s->items->warmup=0;s->items->nextWave=INT_MAX;
 for(auto& p:s->players)for(int k=0;k<5;++k)p.x[k]=p.y[k]=-1;return s;
}
Dot* add(State& s,int x,int y,int team=0){auto d=new Dot(x,y,team);s.field.insert(d);s.dots.push_back(d);s.players[team].score++;return d;}
void army(State& s,int cx,int cy,int radius,int team=0){for(int y=-radius;y<=radius;++y)for(int x=-radius;x<=radius;++x)if(x*x+y*y<=radius*radius)add(s,cx+x,cy+y,team);}
void aim(State& s,int team,int x,int y){s.players[team].x[0]=x;s.players[team].y[0]=y;}
void tick(State& s,int count){state=&s;for(int i=0;i<count;++i)Move::stepDots();}
void valid(State& s){
 int counts[MAX_TEAMS]={};std::unordered_set<Dot*> indexed;
 for(auto head:s.field.cells)for(auto d=head;d;d=d->fieldNext)require(indexed.insert(d).second,"index duplicate/cycle");
 require(indexed.size()==s.dots.size(),"index count");
 for(auto d:s.dots){require(indexed.count(d)==1,"index missing");require(d->qx/256==d->x&&d->qy/256==d->y,"derived coordinates");require(!s.map->isWall(d->x,d->y)||s.items->active(d->team,Items::PHASE),"wall penetration");for(int xx=std::max(0,int(d->x)-1);xx<=std::min(s.map->width-1,int(d->x)+1);++xx)for(int yy=std::max(0,int(d->y)-1);yy<=std::min(s.map->height-1,int(d->y)+1);++yy)for(auto n=s.field[xx][yy];n;n=n->fieldNext)if(n!=d){int dx=n->qx-d->qx,dy=n->qy-d->qy;if(dx*dx+dy*dy<=64*64)std::cerr<<"overlap t="<<s.items->elapsed<<" positions "<<d->centerX()<<","<<d->centerY()<<" / "<<n->centerX()<<","<<n->centerY()<<" teams="<<int(d->team)<<","<<int(n->team)<<"\n";require(dx*dx+dy*dy>64*64,"severe particle interpenetration");}++counts[d->team];}
 for(int t=0;t<s.teamCount;++t)require(counts[t]==s.players[t].score,"population accounting");
}
double center(State& s){double n=0;for(auto d:s.dots)n+=d->centerX();return n/std::max(size_t(1),s.dots.size());}
void write(std::ofstream& out,const char* name,int step,State& s){for(size_t i=0;i<s.dots.size();++i){auto d=s.dots[i];out<<name<<','<<step<<','<<i<<','<<int(d->team)<<','<<d->centerX()<<','<<d->centerY()<<'\n';}}
void movement(){
 auto s=fixture();army(*s,160,220,16);auto n=s->dots.size();double start=center(*s);std::vector<std::pair<int,int>> positions;
 for(auto d:s->dots)positions.push_back({d->qx,d->qy});
 std::ofstream trace("docs/game/liquidfun-2.10/trajectories.csv");trace<<"scene,tick,id,team,x,y\n";write(trace,"open",0,*s);
 aim(*s,0,540,230);for(int k=0;k<200;++k){tick(*s,1);if(k%20==19){valid(*s);write(trace,"open",k+1,*s);}}
 double spread=0,mean=center(*s)-start;int fractional=0;
 for(size_t k=0;k<n;++k){auto d=s->dots[k];double dx=(d->qx-positions[k].first)/256.;spread+=(dx-mean)*(dx-mean);if(d->qx%256!=128||d->qy%256!=128)++fractional;}
 std::cout<<"open: centre travel="<<mean<<" local displacement std="<<std::sqrt(spread/n)<<" fractional="<<fractional<<"/"<<n<<'\n';
 require(mean>20,"army stuck");require(std::sqrt(spread/n)>.5,"rigid translation");require(fractional>int(n/2),"grid locked");
 aim(*s,0,190,330);for(int k=0;k<160;++k){tick(*s,1);if(k%20==19){valid(*s);write(trace,"turn",k+1,*s);}}
 aim(*s,0,-1,-1);std::vector<std::pair<int,int>> stopped;for(auto d:s->dots)stopped.push_back({d->qx,d->qy});tick(*s,60);
 double stopTravel=0;for(size_t k=0;k<n;++k)stopTravel=std::max(stopTravel,double(flow_motion::length(stopped[k].first-s->dots[k]->qx,stopped[k].second-s->dots[k]->qy))/256.);
 std::cout<<"stop settling max travel="<<stopTravel<<" cells\n";require(stopTravel<5,"stop fails to settle");

 auto frozen=s->dots[0];aim(*s,0,600,300);s->items->until[0][Items::FREEZE]=s->items->elapsed+10000;int x=frozen->qx,y=frozen->qy;tick(*s,200);require(frozen->qx==x&&frozen->qy==y,"freeze movement");
 s->items->stock[0][1]=1;require(s->items->activate(*s,0,909,1)==0,"cleanse while frozen");tick(*s,60);require(frozen->qx!=x||frozen->qy!=y,"cleanse resumes");valid(*s);
}
void obstacles(){
 auto s=fixture();army(*s,160,240,13);for(int y=175;y<=305;y+=12)s->map->temporaryObstacle(270,y,10,true);
 aim(*s,0,410,240);std::ofstream out("docs/game/liquidfun-2.10/obstacle.csv");out<<"scene,tick,id,team,x,y\n";
 for(int k=0;k<1200;++k){tick(*s,1);if(k%40==39){valid(*s);write(out,"wall",k+1,*s);}}
 int passed=0;for(auto d:s->dots)if(d->x>290)++passed;
 std::cout<<"wall: passed="<<passed<<"/"<<s->dots.size()<<" centre="<<center(*s)<<'\n';require(passed>int(s->dots.size()/2),"wall path stalls");

}
void wallTargets(){
 auto s=fixture();army(*s,220,240,9);s->map->temporaryObstacle(300,240,18,true);
 aim(*s,0,300,240);double initial=center(*s);tick(*s,450);valid(*s);
 require(center(*s)>initial+40,"wall click discarded instead of approaching edge");
 require(s->operations->feedback[0]==0,"wall click reported unreachable");
 aim(*s,0,385,240);tick(*s,600);valid(*s);int beyond=0;
 for(auto d:s->dots)if(d->x>323)++beyond;
 std::cout<<"wall target: approached then routed around="<<beyond<<"/"<<s->dots.size()<<"\n";
 require(beyond>int(s->dots.size()*9/10),"wall-to-far-side command does not route around");
}
void combatReplay(){
 auto a=fixture(),b=fixture();for(auto s:{a.get(),b.get()}){army(*s,250,240,10,0);army(*s,285,240,10,1);aim(*s,0,285,240);aim(*s,1,250,240);}
 auto n=a->dots.size();for(int i=0;i<240;++i){tick(*a,1);tick(*b,1);require(simulationHash(*a)==simulationHash(*b),"deterministic replay");if(i%20==0){valid(*a);valid(*b);}}
 require(a->dots.size()==n,"combat loses identities/count");require(a->items->audioCaptures[0]>0,"combat no actual conversions");
 std::cout<<"combat: captures="<<a->items->audioCaptures[0]<<" preserved="<<n<<'\n';
 auto x=fixture();auto d=add(*x,x->map->width/2+50,x->map->height/2);x->items->stock[0][2]=1;x->items->activate(*x,0,1,2);int q=d->qx;tick(*x,210);require(std::abs(d->qx-q)<=2*256,"black hole warning lost");tick(*x,100);require(d->qx<q,"continuous gravity absent");x->items->stock[0][1]=1;x->items->activate(*x,0,2,1);q=d->qx;tick(*x,60);require(std::abs(d->qx-q)<=2*256,"cleanse leaves residual gravity");valid(*x);
}
void physicalContact(){
 auto s=fixture();auto a=add(*s,250,240),b=add(*s,258,240);s->liquid=new LiquidMotion(*s);
 for(int k=0;k<140;k++){
  a->budget=b->budget=1;a->hasOrder=b->hasOrder=true;a->wishX=64;b->wishX=-64;a->wishY=b->wishY=0;s->liquid->step();
  require(a->qx<b->qx,"head-on grains pass through each other");
 }
 float gap=b->centerX()-a->centerX();std::cout<<"physical pair contact gap="<<gap<<" cells\n";
 require(gap>.55f&&gap<1.5f,"no compression contact response");
 // Remove the blocker: the driven grain must immediately enter the newly free space.
 int start=a->qx;s->field.erase(b);s->dots.pop_back();--s->players[0].score;delete b;
 for(int k=0;k<20;k++)s->liquid->step();require(a->qx-start>2*256,"released contact stays jammed");
 require(s->liquid->particleCount()==int(s->dots.size()),"solver death index mismatch");valid(*s);
}
void lifecycle(){
 auto s=fixture();s->dotsPerTeam=400;army(*s,350,240,11,0);army(*s,500,240,11,1);
 s->items->capture(*s);auto initial=s->dots.size();aim(*s,0,440,300);tick(*s,90);
 s->items->grant(*s,0,Items::DOUBLE,400,300);require(s->dots.size()>initial,"growth absent");valid(*s);
 s->items->grant(*s,0,Items::TELEPORT,400,300);require(s->items->command(*s,0,s->items->nonce[0],600,340),"teleport rejected");valid(*s);
 for(auto d:s->dots)if(d->team==0)require(d->qx==int(d->x)*256+128&&d->vx==0,"teleport retains old motion");
 s->items->grant(*s,0,Items::RESET,400,300);tick(*s,1);require(s->dots.size()==initial,"rewind population");valid(*s);
 auto epoch=s->items->epoch;std::vector<std::pair<int,int>> snapshot;for(auto d:s->dots)snapshot.push_back({d->qx,d->qy});tick(*s,100);
 require(s->items->epoch==epoch,"rewind repeats");for(size_t n=0;n<s->dots.size();++n)require(snapshot[n]==std::make_pair(s->dots[n]->qx,s->dots[n]->qy),"rewind warmup drifts");
 // Partial dynamic barrier / narrow opening: throughput must exist, not just legal positions.
 auto gate=fixture();army(*gate,220,240,12);for(int y=180;y<=233;y+=2)gate->map->temporaryObstacle(270,y,2,true);for(int y=247;y<=300;y+=2)gate->map->temporaryObstacle(270,y,2,true);
 aim(*gate,0,345,240);for(int k=0;k<800;++k){tick(*gate,1);if(k%100==0)valid(*gate);}
 int crossed=0;for(auto d:gate->dots)if(d->x>274)++crossed;std::cout<<"gate: crossed="<<crossed<<"/"<<gate->dots.size()<<'\n';require(crossed>int(gate->dots.size()/2),"narrow gate stalls");
}
extern double flowProfile[5];
extern double liquidProfile[5];
void perf(int quantity,bool garden=false){
 for(auto& v:liquidProfile)v=0;
 for(auto& v:flowProfile)v=0;
 auto s=std::make_unique<State>(0,1,209,quantity,garden,garden?10:0);s->items->warmup=0;s->items->nextWave=INT_MAX;
 for(int t=0;t<s->teamCount;++t)aim(*s,t,s->birthX[(t+1)%s->teamCount],s->birthY[(t+1)%s->teamCount]);std::vector<double> ms;
 for(int k=0;k<140;++k){auto a=std::chrono::steady_clock::now();tick(*s,1);auto b=std::chrono::steady_clock::now();if(k>=20)ms.push_back(std::chrono::duration<double,std::milli>(b-a).count());}
 std::sort(ms.begin(),ms.end());std::cout<<"host particles="<<s->dots.size()<<" step_ms_p50="<<ms[ms.size()/2]<<" p95="<<ms[ms.size()*95/100]<<" max="<<ms.back()<<'\n';valid(*s);for(auto v:flowProfile)std::cout<<v/140<<" ";std::cout<<" ops/nav/plan/solve/contact ms per step\n";for(auto v:liquidProfile)std::cout<<v/140<<" ";std::cout<<" geometry/sync/LiquidFun/sweep ms fixtures="<<s->liquid->fixtureCount()<<"\n";
}
int main(int argc,char** argv){try{require(argc>1,"assets argument");assets.root=argv[1];physicalContact();movement();obstacles();wallTargets();combatReplay();lifecycle();perf(1000);perf(2000);perf(1000,true);state=nullptr;std::cout<<"PASS "<<checks<<" LiquidFun contact/rule checks; not visual-quality or cross-device proof\n";return 0;}catch(const std::exception& e){std::cerr<<"FAIL "<<e.what()<<" checks="<<checks<<'\n';return 1;}}
