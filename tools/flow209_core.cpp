// Copyright (c) 2026 Haoke. Test-only controlled fixtures, never shipped in APK.
#include "state.hpp"
#include "move.hpp"
#include "flow_motion.hpp"
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
 for(auto d:s.dots){require(indexed.count(d)==1,"index missing");require(d->qx/256==d->x&&d->qy/256==d->y,"derived coordinates");require(!s.map->isWall(d->x,d->y)||s.items->active(d->team,Items::PHASE),"wall penetration");require(flow_motion::vacant(s,d->qx,d->qy,d),"disc overlap");++counts[d->team];}
 for(int t=0;t<s.teamCount;++t)require(counts[t]==s.players[t].score,"population accounting");
}
double center(State& s){double n=0;for(auto d:s.dots)n+=d->centerX();return n/std::max(size_t(1),s.dots.size());}
void write(std::ofstream& out,const char* name,int step,State& s){for(size_t i=0;i<s.dots.size();++i){auto d=s.dots[i];out<<name<<','<<step<<','<<i<<','<<int(d->team)<<','<<d->centerX()<<','<<d->centerY()<<'\n';}}
void movement(){
 auto s=fixture();army(*s,160,220,16);auto n=s->dots.size();double start=center(*s);std::vector<std::pair<int,int>> positions;
 for(auto d:s->dots)positions.push_back({d->qx,d->qy});
 std::ofstream trace("docs/game/flow-2.09/trajectories.csv");trace<<"scene,tick,id,team,x,y\n";write(trace,"open",0,*s);
 aim(*s,0,540,230);for(int k=0;k<200;++k){tick(*s,1);if(k%20==19){valid(*s);write(trace,"open",k+1,*s);}}
 double spread=0,mean=center(*s)-start;int fractional=0;
 for(size_t k=0;k<n;++k){auto d=s->dots[k];double dx=(d->qx-positions[k].first)/256.;spread+=(dx-mean)*(dx-mean);if(d->qx%256!=128||d->qy%256!=128)++fractional;}
 std::cout<<"open: centre travel="<<mean<<" local displacement std="<<std::sqrt(spread/n)<<" fractional="<<fractional<<"/"<<n<<'\n';
 require(mean>20,"army stuck");require(std::sqrt(spread/n)>.5,"rigid translation");require(fractional>int(n/2),"grid locked");
 aim(*s,0,190,330);for(int k=0;k<160;++k){tick(*s,1);if(k%20==19){valid(*s);write(trace,"turn",k+1,*s);}}
 aim(*s,0,-1,-1);std::vector<std::pair<int,int>> stopped;for(auto d:s->dots)stopped.push_back({d->qx,d->qy});tick(*s,60);
 for(size_t k=0;k<n;++k)require(std::abs(stopped[k].first-s->dots[k]->qx)<=flow_motion::REST_RADIUS&&std::abs(stopped[k].second-s->dots[k]->qy)<=flow_motion::REST_RADIUS,"stop motility escapes bounded anchor");
 auto frozen=s->dots[0];aim(*s,0,600,300);s->items->until[0][Items::FREEZE]=s->items->elapsed+10000;int x=frozen->qx,y=frozen->qy;tick(*s,200);require(frozen->qx==x&&frozen->qy==y,"freeze movement");
 s->items->stock[0][1]=1;require(s->items->activate(*s,0,909,1)==0,"cleanse while frozen");tick(*s,60);require(frozen->qx!=x||frozen->qy!=y,"cleanse resumes");valid(*s);
}
void obstacles(){
 auto s=fixture();army(*s,160,240,13);for(int y=175;y<=305;y+=12)s->map->temporaryObstacle(270,y,10,true);
 aim(*s,0,410,240);std::ofstream out("docs/game/flow-2.09/obstacle.csv");out<<"scene,tick,id,team,x,y\n";
 for(int k=0;k<1200;++k){tick(*s,1);if(k%40==39){valid(*s);write(out,"wall",k+1,*s);}}
 int passed=0;for(auto d:s->dots)if(d->x>290)++passed;
 std::cout<<"wall: passed="<<passed<<"/"<<s->dots.size()<<" centre="<<center(*s)<<'\n';require(passed>int(s->dots.size()/2),"wall path stalls");
 // Real one-cell-wide barrier cannot be crossed by high-speed attraction.
 auto b=fixture();auto d=add(*b,300,240);for(int y=220;y<=260;++y)b->map->temporaryObstacle(305,y,1,true);
 for(int i=0;i<100;++i)flow_motion::translate(*b,*d,512,0,true);require(d->x<304,"swept gravity crosses wall");valid(*b);
}
void combatReplay(){
 auto a=fixture(),b=fixture();for(auto s:{a.get(),b.get()}){army(*s,250,240,10,0);army(*s,285,240,10,1);aim(*s,0,285,240);aim(*s,1,250,240);}
 auto n=a->dots.size();for(int i=0;i<240;++i){tick(*a,1);tick(*b,1);require(simulationHash(*a)==simulationHash(*b),"deterministic replay");if(i%20==0){valid(*a);valid(*b);}}
 require(a->dots.size()==n,"combat loses identities/count");require(a->items->audioCaptures[0]>0,"combat no actual conversions");
 std::cout<<"combat: captures="<<a->items->audioCaptures[0]<<" preserved="<<n<<'\n';
 auto x=fixture();auto d=add(*x,x->map->width/2+50,x->map->height/2);x->items->stock[0][2]=1;x->items->activate(*x,0,1,2);int q=d->qx;tick(*x,210);require(std::abs(d->qx-q)<=flow_motion::REST_RADIUS,"black hole warning lost");tick(*x,100);require(d->qx<q,"continuous gravity absent");x->items->stock[0][1]=1;x->items->activate(*x,0,2,1);q=d->qx;tick(*x,60);require(std::abs(d->qx-q)<=flow_motion::REST_RADIUS,"cleanse leaves residual gravity");valid(*x);
}
void individualActivity(){
 auto s=fixture();army(*s,400,300,12);aim(*s,0,400,300);tick(*s,180);
 std::vector<double> travel(s->dots.size(),0);double initial=center(*s);
 for(int k=0;k<120;++k){std::vector<std::pair<int,int>> old;for(auto d:s->dots)old.push_back({d->qx,d->qy});tick(*s,1);
  for(size_t i=0;i<old.size();++i)travel[i]+=flow_motion::length(s->dots[i]->qx-old[i].first,s->dots[i]->qy-old[i].second)/256.;
  if(k%20==0)valid(*s);
 }
 int active=0;double sum=0;for(double d:travel){sum+=d;if(d>1)++active;}
 std::cout<<"at goal: independently travelling >1 cell="<<active<<"/"<<travel.size()<<" mean path="<<sum/travel.size()<<" centre drift="<<center(*s)-initial<<'\n';
 require(active>int(travel.size()/2),"most individual grains jammed at goal");require(std::abs(center(*s)-initial)<3,"motility drifts army off goal");
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
void perf(int quantity){
 for(auto& v:flowProfile)v=0;
 auto s=std::make_unique<State>(0,1,209,quantity);s->items->warmup=0;s->items->nextWave=INT_MAX;
 for(int t=0;t<6;++t)aim(*s,t,400,240);std::vector<double> ms;
 for(int k=0;k<140;++k){auto a=std::chrono::steady_clock::now();tick(*s,1);auto b=std::chrono::steady_clock::now();if(k>=20)ms.push_back(std::chrono::duration<double,std::milli>(b-a).count());}
 std::sort(ms.begin(),ms.end());std::cout<<"host particles="<<s->dots.size()<<" step_ms_p50="<<ms[ms.size()/2]<<" p95="<<ms[ms.size()*95/100]<<" max="<<ms.back()<<'\n';valid(*s);for(auto v:flowProfile)std::cout<<v/140<<" ";std::cout<<" ops/nav/plan/solve/contact ms per step\n";
}
int main(int argc,char** argv){try{require(argc>1,"assets argument");assets.root=argv[1];movement();individualActivity();obstacles();combatReplay();lifecycle();perf(1000);perf(2000);state=nullptr;std::cout<<"PASS "<<checks<<" geometric/rule checks; not visual-quality or cross-device proof\n";return 0;}catch(const std::exception& e){std::cerr<<"FAIL "<<e.what()<<" checks="<<checks<<'\n';return 1;}}
