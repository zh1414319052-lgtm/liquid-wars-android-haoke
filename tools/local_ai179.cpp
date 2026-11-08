// SPDX-License-Identifier: GPL-3.0-or-later
// Production core harness, no alternate movement/combat or renderer.
#define main unused_items_main179
#include "check_items_core.cpp"
#undef main
#include "local_tactics.hpp"
#include "ai_tactics.hpp"
#include "ai.hpp"
#include "state_hash.hpp"
#include <chrono>
#include <iomanip>
using Clock=std::chrono::steady_clock;
void apply(State& s,int team,const std::array<short,60>& p){for(int k=0;k<5;k++){s.players[team].x[k]=p[team*10+k];s.players[team].y[k]=p[team*10+5+k];}if(s.players[team].x[4]==-3)s.items->command(s,team,s.players[team].y[4],s.players[team].x[3],s.players[team].y[3]);}
// Exact1.78 Java chooseLocalItems policy ported for headless comparisons.
void oldItem(State& s,int t){auto& i=*s.items;int kind=i.pending[t];if(kind!=3&&kind!=7&&kind!=8)return;int target=-1,best=-1;
 for(int e=0;e<6;e++)if(s.players[e].score>0&&s.alliances[t]!=s.alliances[e]){if(kind==7&&(i.active(e,6)||i.active(e,16)))continue;if(kind==8&&i.active(e,17))continue;if(s.players[e].score>best){best=s.players[e].score;target=e;}}
 if(target<0)return;int x=target,y=0;
 if(kind==3){int gx=s.players[t].x[0],gy=s.players[t].y[0];if(gx<0){long sx=0,sy=0;int n=0;for(auto d:s.dots)if(d->team==target){sx+=d->x;sy+=d->y;n++;}gx=int(sx/std::max(1,n));gy=int(sy/std::max(1,n));}int found=-1;
  for(int r=12;r<=60&&found<0;r+=8)for(int k=0;k<16;k++){int xx=gx+int(std::cos(k*3.141592653589793/8)*r),yy=gy+int(std::sin(k*3.141592653589793/8)*r);if(xx>=16&&xx<s.map->width-16&&yy>=4&&yy<s.map->height-4&&!s.map->isWall(xx,yy)){found=yy*s.map->width+xx;break;}}
  if(found<0)return;x=found%s.map->width;y=found/s.map->width;
 }s.players[t].x[3]=short(x);s.players[t].y[3]=short(y);s.players[t].x[4]=-3;s.players[t].y[4]=short(i.nonce[t]);i.command(s,t,i.nonce[t],x,y);
}
std::array<int,6> groups(const State& s){std::array<int,6> g;for(int p=0;p<6;p++){g[p]=s.alliances[p];if(s.items->truceUntil>s.items->elapsed&&(g[p]==s.items->truceA||g[p]==s.items->truceB))g[p]=s.items->truceA;}return g;}
void battle(int map,int seed,int mode,int swap,int seconds,int dots){
 auto s=std::make_unique<State>(0,map,seed,dots);state=s.get();int mask=swap?42:21;
 if(mode==0){std::vector<Dot*> kept;for(auto d:s->dots){if(d->team<2)kept.push_back(d);else{s->field[d->x][d->y]=nullptr;delete d;}}s->dots.swap(kept);for(int t=2;t<6;t++)s->players[t].score=0;mask=swap?2:1;}
 if(mode==2){for(int t=0;t<6;t++)s->alliances[t]=t/2;int group=seed%3;mask=3<<(group*2);if(swap)mask=63^mask;}
 std::unique_ptr<AiTactics> old;int rev=-1;std::array<int,6> oldGroups{};LocalTactics modern;std::array<short,60> cachedOld{},cachedNew{};cachedOld.fill(-1);cachedNew.fill(-1);
 std::vector<double> newMs,oldMs;int actions[7]={},switches=0,stuck=0;int lastAction[6]={-1,-1,-1,-1,-1,-1};int newItems=0,oldItems=0,lastEvent=0;int frames=0,tick=0,delay=6,epoch=0;std::string termination="cap";int newUses=0,oldUses=0,newWaste=0,oldWaste=0;const int budget=(seconds*2000+5000)/70;
 auto collectEvents=[&](){for(const auto& e:s->items->events)if(e.serial>lastEvent){if(e.stage==0||e.stage==1){if(mask&(1<<e.team))newItems++;else oldItems++;}if(e.stage==2||e.stage==4){if(mask&(1<<e.team))newUses++;else oldUses++;}if(e.amount<0){if(mask&(1<<e.team))newWaste++;else oldWaste++;}lastEvent=std::max(lastEvent,e.serial);}};
 for(int f=0;f<budget;f++){frames=f+1;step(*s,10);collectEvents();bool enemy=false;int first=-1;for(int t=0;t<6;t++)if(s->players[t].score){if(first<0)first=t;else if(!s->allied(first,t))enemy=true;}if(!enemy){termination="elimination";break;}if(s->items->elapsed>=seconds*1000){termination="time_limit";break;}
  if(epoch!=s->items->epoch){epoch=s->items->epoch;tick=0;delay=6;modern.reset();old.reset();cachedNew.fill(-1);cachedOld.fill(-1);}
  if(s->items->elapsed<=0||delay-->=0)continue;
  if(tick%8==0){auto start=Clock::now();if(!old||rev!=s->map->revision()||oldGroups!=groups(*s)){oldGroups=groups(*s);old.reset(new AiTactics(s->map->width,s->map->height,[&](int x,int y){return s->map->isWall(x,y);},oldGroups));rev=s->map->revision();}std::array<unsigned,6> hidden{};for(int t=0;t<6;t++)for(int e=0;e<6;e++)if(s->items->concealedFrom(*s,t,e))hidden[t]|=1u<<e;cachedOld=old->plan(s->dots,nullptr,&hidden);oldMs.push_back(std::chrono::duration<double,std::milli>(Clock::now()-start).count());}
  int due=0;for(int t=0;t<6;t++)if(t%4==tick%4)due|=mask&(1<<t);if(due){auto start=Clock::now();auto fresh=modern.plan(*s,due,2);for(int t=0;t<6;t++)if(due&(1<<t))std::copy_n(fresh.begin()+t*10,10,cachedNew.begin()+t*10);newMs.push_back(std::chrono::duration<double,std::milli>(Clock::now()-start).count());for(int t=0;t<6;t++)if((due&(1<<t))&&s->players[t].score){auto d=modern.decisions[t];actions[d.action]++;if(lastAction[t]!=d.action&&lastAction[t]>=0)switches++;lastAction[t]=d.action;if(d.stuck)stuck++;}}
  for(int t=0;t<6;t++){int nearest=AI::getNearestDot(t,s->players[t].x[0],s->players[t].y[0]);auto& p=(mask&(1<<t))?cachedNew:cachedOld;
   if(p[t*10]>=0)apply(*s,t,p);else {if(!(mask&(1<<t))){s->players[t].x[0]=short(nearest>>16);s->players[t].y[0]=short(nearest);}for(int k=1;k<5;k++)s->players[t].x[k]=s->players[t].y[k]=-1;}
   if(!(mask&(1<<t)))oldItem(*s,t);
  }
  collectEvents();
  tick++;
 }
 int winner=-1,best=-1;int allianceScore[6]={};for(int t=0;t<6;t++)for(int e=0;e<6;e++)if(s->allied(t,e))allianceScore[t]+=s->players[e].score;for(int t=0;t<6;t++)if(allianceScore[t]>best){best=allianceScore[t];winner=t;}
 bool tied=false;for(int t=0;t<6;t++)if(!s->allied(t,winner)&&allianceScore[t]==best)tied=true;
 bool mixed=false;for(int t=0;t<6;t++)if(s->players[t].score&&s->allied(winner,t)&&bool(mask&(1<<t))!=bool(mask&(1<<winner)))mixed=true;
 auto pct=[](std::vector<double> v,double q){if(v.empty())return 0.;std::sort(v.begin(),v.end());return v[size_t((v.size()-1)*q)];};
 std::cout<<std::fixed<<std::setprecision(4)<<"{\"map\":"<<map<<",\"seed\":"<<seed<<",\"mode\":"<<mode<<",\"swap\":"<<swap<<",\"mask\":"<<mask<<",\"dots\":"<<dots<<",\"wall_steps_ms\":"<<frames*70<<",\"termination\":\""<<termination<<"\",\"new_uses\":"<<newUses<<",\"old_uses\":"<<oldUses<<",\"new_waste\":"<<newWaste<<",\"old_waste\":"<<oldWaste<<",\"sim_ms\":"<<s->items->elapsed<<",\"new_win\":"<<(termination=="cap"?-3:mixed?-2:tied?-1:((mask&(1<<winner))?1:0))<<",\"winner\":"<<winner<<",\"scores\":[";
 for(int t=0;t<6;t++)std::cout<<(t?",":"")<<s->players[t].score;std::cout<<"],\"actions\":[";for(int k=0;k<7;k++)std::cout<<(k?",":"")<<actions[k];
 std::cout<<"],\"switches\":"<<switches<<",\"stalled_decisions\":"<<stuck<<",\"new_pickups\":"<<newItems<<",\"old_pickups\":"<<oldItems<<",\"new_plan_p50_ms\":"<<pct(newMs,.5)<<",\"new_plan_p95_ms\":"<<pct(newMs,.95)<<",\"new_plan_max_ms\":"<<pct(newMs,1)<<",\"old_plan_p95_ms\":"<<pct(oldMs,.95)<<",\"hash\":\""<<std::hex<<simulationHash(*s)<<"\"}"<<std::endl;
}
std::unique_ptr<State> arena(){auto s=std::make_unique<State>(0,0,179,0);for(int y=4;y<s->map->height-4;y++)for(int x=16;x<s->map->width-16;x++)s->map->excavate(x,y);s->items->nextWave=INT_MAX;return s;}
void army(State& s,int t,int x,int y,int count){for(int k=0;k<count;k++)add(s,t,x+k%10,y+k/10);}
void focused(){
 auto s=arena();army(*s,0,100,100,100);army(*s,1,350,100,100);LocalTactics a;auto x=a.plan(*s,1,2);check(a.decisions[0].enemy==-1,"unseen opponent never targeted");
 auto other=arena();army(*other,0,100,100,100);army(*other,1,650,350,100);LocalTactics b;check(x==b.plan(*other,1,2),"moving unseen opponent cannot alter decisions");
 auto c=arena();army(*c,0,100,100,100);army(*c,1,140,100,100);LocalTactics d;d.plan(*c,1,2);check(d.decisions[0].action==LocalTactics::ATTACK,"visible reachable enemy engaged");
 c->items->until[1][Items::INVISIBLE]=20000;LocalTactics e;e.plan(*c,1,2);check(e.decisions[0].enemy==-1,"invisible enemy excluded");
 c->items->until[1][Items::INVISIBLE]=0;c->items->until[1][Items::INVINCIBLE]=20000;LocalTactics f;f.plan(*c,1,2);check(f.decisions[0].action==LocalTactics::RETREAT,"retreat from invincible nearby army");
 auto p=arena();army(*p,0,100,100,100);army(*p,1,350,300,100);p->items->pickups[0]={Items::FREEZE,160,105};LocalTactics g;auto pick=g.plan(*p,1,2);check(g.decisions[0].action==LocalTactics::PICKUP&&pick[0]==160,"useful item targeted at actual solid centre");
 p->items->pickups[0]={Items::RESET,160,105};LocalTactics h;h.plan(*p,1,2);check(h.decisions[0].action!=LocalTactics::PICKUP,"healthy opening army does not waste rewind");
 auto death=arena();army(*death,0,100,100,100);army(*death,1,140,100,100);army(*death,2,100,160,60);death->items->grant(*death,0,Items::DEATH,100,100);death->items->until[1][Items::WARD]=20000;LocalTactics i;auto cmd=i.plan(*death,1,2);check(cmd[4]==-3&&cmd[3]==2,"death avoids protected rival and selects seen rival");apply(*death,0,cmd);check(death->players[2].score==0&&death->players[1].score==100,"ordinary legal death command works");
 death->items->grant(*death,0,Items::FRIEND,100,100);death->items->until[1][Items::LIBERTY]=20000;LocalTactics j;cmd=j.plan(*death,1,2);check(cmd[4]!=-3,"do not waste friendship into liberty");death->items->until[1][Items::LIBERTY]=0;cmd=j.plan(*death,1,2);check(cmd[4]==-3&&cmd[3]==1,"legal local friendship selection");apply(*death,0,cmd);check(death->allied(0,1),"friend item follows current alliance rules");
 auto tp=arena();army(*tp,0,100,100,100);army(*tp,1,600,350,100);tp->items->pickups[0]={Items::FREEZE,400,100};tp->items->grant(*tp,0,Items::TELEPORT,100,100);LocalTactics k;cmd=k.plan(*tp,1,2);check(cmd[4]==-3,"teleport toward useful distant objective");apply(*tp,0,cmd);check(tp->items->pending[0]==0&&tp->items->last.amount==100,"teleport fits entire army using ordinary rule");
 auto hiddenTp=arena();army(*hiddenTp,0,100,100,100);army(*hiddenTp,1,416,100,100);hiddenTp->items->pickups[0]={Items::FREEZE,400,100};hiddenTp->items->grant(*hiddenTp,0,Items::TELEPORT,100,100);LocalTactics hiddenPlanner;
 auto clearTp=arena();army(*clearTp,0,100,100,100);army(*clearTp,1,600,350,100);clearTp->items->pickups[0]={Items::FREEZE,400,100};clearTp->items->grant(*clearTp,0,Items::TELEPORT,100,100);LocalTactics clearPlanner;
 check(hiddenPlanner.plan(*hiddenTp,1,2)==clearPlanner.plan(*clearTp,1,2),"teleport planning cannot scout unseen occupied cells");
 auto ra=std::make_unique<State>(0,54,17954,100),rb=std::make_unique<State>(5,54,17954,100);LocalTactics readonly;
 for(int tick=0;tick<350;tick++){for(auto p2:{ra.get(),rb.get()}){for(int t=0;t<6;t++){p2->players[t].x[0]=short(p2->birthX[t]+(tick%3)*6);p2->players[t].y[0]=short(p2->birthY[t]);}if(p2==ra.get()&&tick%4==0){auto before=simulationHash(*p2);readonly.plan(*p2,63,2);check(before==simulationHash(*p2),"interleaved planner is read-only");}step(*p2,1);}check(simulationHash(*ra)==simulationHash(*rb),"same real inputs same authority hash with/without planning");}
 std::cout<<"PASS visibility/hidden-coordinate independence/retreat/item counters/teleport/350-step authority replay"<<std::endl;
}
void unit(){
 for(int map:{1,8,28,49,50,54,59}){auto s=std::make_unique<State>(0,map,17900+map,1000);state=s.get();LocalTactics p;auto before=simulationHash(*s);auto a=p.plan(*s,63,2);check(simulationHash(*s)==before,"planner never mutates core/RNG/dots");for(int t=0;t<6;t++)check(a[t*10]>=0&&a[t*10+5]>=0,"all live teams receive world target");LocalTactics q;check(a==q.plan(*s,63,2),"deterministic new planner");for(int k=0;k<70;k++)Move::stepDots();before=simulationHash(*s);p.plan(*s,63,2);check(simulationHash(*s)==before,"subsequent plan read-only");s->items->rewind(*s);before=simulationHash(*s);p.plan(*s,63,2);check(simulationHash(*s)==before,"rewind cache reset read-only");}
 auto s=fixture();state=s.get();LocalTactics p;auto z=p.plan(*s,0,2);for(short v:z)check(v==-1,"no local mask means no commands");auto a=p.plan(*s,1,2);for(int t=1;t<6;t++)for(int k=0;k<10;k++)check(a[t*10+k]==-1,"models/humans excluded at native boundary");
 focused();std::cout<<"PASS "<<checks<<" targeted production-core checks"<<std::endl;
}
void traceEdge(){auto s=std::make_unique<State>(0,1,17901,1000);state=s.get();LocalTactics p;std::array<short,60> cache;cache.fill(-1);for(int f=0;f<360;f++){step(*s,10);if(f<7)continue;int tick=f-7,due=0;for(int t=0;t<6;t++)if(t%4==tick%4)due|=1<<t;auto fresh=p.plan(*s,due,2);for(int t=0;t<6;t++){if(due&(1<<t))std::copy_n(fresh.begin()+t*10,10,cache.begin()+t*10);AI::getNearestDot(t,s->players[t].x[0],s->players[t].y[0]);if(cache[t*10]>=0)apply(*s,t,cache);}if(f%14==0){auto d=p.decisions[3];long x=0,y=0;int n=0;for(auto a:s->dots)if(a->team==3){x+=a->x;y+=a->y;n++;}std::cout<<s->items->elapsed<<","<<x/std::max(1,n)<<","<<y/std::max(1,n)<<","<<s->players[3].x[0]<<","<<s->players[3].y[0]<<","<<d.action<<","<<d.x<<","<<d.y<<","<<d.stuck<<std::endl;}}}
int main(int argc,char** argv){try{assets.root=argv[1];if(argc<3||std::string(argv[2])=="unit")unit();else if(std::string(argv[2])=="trace")traceEdge();else battle(std::stoi(argv[2]),std::stoi(argv[3]),std::stoi(argv[4]),std::stoi(argv[5]),std::stoi(argv[6]),std::stoi(argv[7]));state=nullptr;return 0;}catch(const std::exception&e){std::cerr<<"FAIL "<<e.what()<<std::endl;return 1;}}
