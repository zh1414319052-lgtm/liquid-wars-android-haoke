// SPDX-License-Identifier: GPL-3.0-or-later
#define main old_main180
#include "check_items_core.cpp"
#undef main
#include "state_hash.hpp"
#include "local_tactics.hpp"
#include <chrono>
std::unique_ptr<State> arena(){auto s=std::make_unique<State>(0,1,180,0);s->dotsPerTeam=100;for(int y=4;y<s->map->height-4;y++)for(int x=16;x<s->map->width-16;x++)s->map->excavate(x,y);for(auto& p:s->players)p.x[0]=p.y[0]=-2;s->items->nextWave=INT_MAX;return s;}
void army(State& s,int t,int x,int y,int count){for(int k=0;k<count;k++)add(s,t,x+k%10,y+k/10);}
int packet=0;
void order(State& s,int team,int op,int group,std::vector<Operations::Point> pts={}){
 short x[5]={-5,short(++packet),-1,-1,-1},y[5]={short(op*4+group),short(s.operations->generation[team]),-1,-1,-1};for(size_t i=0;i<pts.size()&&i<3;i++){x[i+2]=short(pts[i].x);y[i+2]=short(pts[i].y);}check(s.operations->command(s,team,x,y),"command recognized");
}
void route(State& s,int team,int group,std::vector<Operations::Point> pts){order(s,team,1,group,pts);order(s,team,3,group);}
void tests(){
 auto s=arena();army(*s,0,100,100,100);army(*s,1,650,350,100);s->items->capture(*s);s->operations->configure(*s,0);
 order(*s,0,5,3);check(s->operations->groups[0][0].count==50&&s->operations->groups[0][1].count==25,"stable split 50/25/25");
 route(*s,0,1,{{180,100},{180,180}});route(*s,0,2,{{100,200}});step(*s,1300);valid(*s);check(s->operations->groups[0][1].y>150,"drawn route corner traversed");check(s->operations->groups[0][2].y>165,"sequential second squad order persists");check(s->operations->groups[0][0].x<120,"main force keeps independent hold");
 order(*s,0,4,1);auto h=hash(*s);std::vector<std::pair<int,int>> positions;for(auto d:s->dots)if(d->team==0&&d->squad==1)positions.push_back({d->x,d->y});step(*s,20);size_t at=0;for(auto d:s->dots)if(d->team==0&&d->squad==1)check(positions[at++]==std::make_pair(int(d->x),int(d->y)),"stop holds exact selected units");(void)h;
 route(*s,0,0,{{0,0}});check(s->operations->feedback[0]==Operations::INVALID,"wall destination rejected");
 s->operations->configure(*s,1);check(s->operations->sites.size()==3,"three sites ordinary map");auto& site=s->operations->sites[0];
 s->items->grant(*s,0,Items::TELEPORT,100,100);check(s->items->command(*s,0,s->items->nonce[0],site.x,site.y),"teleport legal");order(*s,0,4,3);step(*s,600);check(site.owner==0,"sustained contact captures site");int before=s->players[0].score;step(*s,320);check(s->players[0].score>before&&site.born>0,"site produces actual units");check(s->operations->points[0]>0,"control score accrues");valid(*s);
 // Real hostile occupants deny income without an omniscient AI shortcut.
 int hostile=0;for(int y=site.y-20;y<=site.y+20&&hostile<15;y++)for(int x=site.x-20;x<=site.x+20&&hostile<15;x++)if(!s->field[x][y]&&!s->map->isWall(x,y)){add(*s,1,x,y);hostile++;}int score=s->operations->points[0];step(*s,10);check(site.contested,"enemy contact contests");step(*s,150);check(s->operations->points[0]==score,"contested site stops income");
 auto snap=s->operations->snapshot(*s,1,false);check(!snap.empty(),"private order snapshot");
 s->items->rewind(*s);check(s->operations->points[0]==0&&!s->operations->split[0],"rewind clears score and squads");for(auto p:s->operations->sites)check(p.owner==-1,"rewind restores neutral sites");check(s->operations->routes[0][1].points.empty(),"rewind clears routes");valid(*s);
 std::vector<uint64_t> first;for(int pass=0;pass<2;pass++){packet=0;auto q=arena();army(*q,0,100,100,100);army(*q,1,650,350,100);q->operations->configure(*q,1);order(*q,0,5,3);route(*q,0,1,{{180,100},{180,180}});for(int k=0;k<80;k++){step(*q,10);uint64_t h2=simulationHash(*q);if(!pass)first.push_back(h2);else check(h2==first[k],"same command stream deterministic");}}
}
void edgeCases(){
 auto s=arena();army(*s,0,100,100,100);army(*s,1,650,350,100);s->operations->configure(*s,1);auto& op=*s->operations;
 route(*s,0,0,{{200,100}});order(*s,0,1,0,{{250,110}});check(op.routes[0][0].points[0].x==200,"partial upload does not replace active order");order(*s,0,1,0,{{0,0}});order(*s,0,3,0);check(op.routes[0][0].points[0].x==200,"invalid order retains previous route");
 int oldGeneration=op.generation[0];s->items->grant(*s,0,Items::TELEPORT,100,100);check(s->items->command(*s,0,s->items->nonce[0],300,100),"teleport fixture accepted");check(!op.routes[0][0].enabled&&op.generation[0]!=oldGeneration,"teleport clears routes and invalidates in-flight orders");short x[5]={-5,short(++packet),200,-1,-1},y[5]={4,short(oldGeneration),100,-1,-1};op.command(*s,0,x,y);order(*s,0,3,0);check(!op.routes[0][0].enabled,"stale route cannot revive after teleport");
 order(*s,0,5,3);int token=packet;route(*s,0,1,{{350,100}});x[1]=short(token);y[0]=20;y[1]=short(op.generation[0]);op.command(*s,0,x,y); // old sequence may arrive only through ordered history; duplicate latest tested below
 x[1]=short(op.lastPacket[0]);uint64_t hashBefore=op.hash();op.command(*s,0,x,y);check(op.hash()==hashBefore,"identical lockstep packet is idempotent");
 auto privateSnap=op.snapshot(*s,1,false);int offset=13+int(op.sites.size())*8;check(privateSnap[offset]==0&&privateSnap[offset+4]==0&&privateSnap[offset+5]==-1,"enemy subgroup count and position hidden");
 auto& site=op.sites[0];site.owner=0;for(auto& p:op.sites){p.owner=-1;p.claim=-1;p.progress=0;}site.owner=0;site.x=500;site.y=200;
 s->dotsPerTeam=50;int population=s->players[0].score;step(*s,600);check(s->players[0].score==population,"site production respects twice-initial population cap");s->dotsPerTeam=100;step(*s,290);check(s->players[0].score<=population+3,"cap does not accumulate deferred production");
 op.points[0]=179;site.scoreAt=op.clock-1000;step(*s,10);check(op.winningTeam(*s)==0,"control threshold wins");
 s->items->grant(*s,1,Items::DEATH,650,350);check(s->items->command(*s,1,s->items->nonce[1],0,0),"death effect accepted");step(*s,600);check(s->players[0].score==0&&site.owner==-1,"eliminated faction cannot respawn from owned site");valid(*s);
 auto actual=std::make_unique<State>(0,54,18054,100);actual->operations->configure(*actual,1);actual->items->capture(*actual);order(*actual,0,5,3);actual->items->rewind(*actual);check(actual->operations->mode==1&&actual->operations->sites.size()==5,"rewind retains mode and large-map objectives");valid(*actual);
}
void placement(){for(int map=1;map<=59;map++){auto s=std::make_unique<State>(0,map,18000+map,100);s->operations->configure(*s,1);check(s->operations->sites.size()==size_t(map>=50?5:3),"site count on every map");for(auto p:s->operations->sites)check(!s->map->isWall(p.x,p.y),"site on real floor");std::cout<<"MAP "<<map<<" SITES "<<s->operations->sites.size()<<"\n";}}
void captureEdges(){
 auto s=arena();army(*s,0,190,230,12);army(*s,1,650,350,12);s->operations->configure(*s,1);s->operations->sites.resize(1);auto& p=s->operations->sites[0];p.x=196;p.y=236;order(*s,0,4,3);order(*s,1,4,3);step(*s,580);check(p.owner==0,"four second capture");
 s->items->grant(*s,0,Items::TELEPORT,0,0);check(s->items->command(*s,0,s->items->nonce[0],100,100),"owner leaves through teleport");order(*s,0,4,3);s->items->grant(*s,1,Items::TELEPORT,0,0);check(s->items->command(*s,1,s->items->nonce[1],p.x,p.y),"raider reaches site");order(*s,1,4,3);int control=s->operations->points[0];step(*s,580);check(p.owner==-1&&p.claim==1,"enemy site neutralized before takeover");check(s->operations->points[0]==control,"hostile capture suspends previous owner's income");step(*s,580);check(p.owner==1,"second capture period transfers ownership");
 auto ally=arena();army(*ally,0,193,230,4);army(*ally,1,198,230,4);army(*ally,2,600,350,20);ally->alliances[1]=ally->alliances[0];ally->operations->configure(*ally,1);ally->operations->sites.resize(1);auto& a=ally->operations->sites[0];a.x=196;a.y=236;
 ally->items->grant(*ally,2,Items::FREEZE,0,0);step(*ally,600);check(a.owner==-1&&a.progress==0,"frozen allied occupants cannot capture");ally->items->grant(*ally,0,Items::THAW,0,0);step(*ally,580);check(a.owner==0&&!a.contested,"allied sub-threshold forces combine for capture");int before=ally->players[0].score;ally->items->grant(*ally,2,Items::ANTIGROW,0,0);step(*ally,600);check(ally->players[0].score==before,"anti-growth counters site production too");
 ally->items->grant(*ally,0,Items::INVISIBLE,0,0);check(ally->items->concealedFrom(*ally,2,0),"existing invisibility maintained");auto publicSite=ally->operations->snapshot(*ally,2,false);check(publicSite[15]==a.owner,"site ownership stays public during invisibility");check(!ally->items->active(0,Items::INVINCIBLE)&&!ally->items->active(0,Items::PHASE),"invisibility does not grant immunity or phase");ally->items->grant(*ally,2,Items::REVEAL,0,0);check(!ally->items->concealedFrom(*ally,2,0),"reveal counters concealment");ally->items->until[2][Items::REVEAL]=0;ally->items->grant(*ally,0,Items::INVISIBLE,0,0);ally->items->grant(*ally,2,Items::INVINCIBLE,0,0);Dot* attacker=nullptr;Dot* defender=nullptr;for(auto d:ally->dots){if(d->team==0)attacker=d;if(d->team==2)defender=d;}check(attacker&&defender,"combat reveal fixture");ally->items->contact(*ally,*defender,*attacker);check(!ally->items->concealedFrom(*ally,2,0),"hostile contact reveals even against invincibility");valid(*ally);
}
void bench(int profile,int map,int count,int swap){
 packet=0;auto s=std::make_unique<State>(0,map,18000+map,count);for(auto& p:s->players)p.score=0;std::vector<Dot*> keep;for(auto d:s->dots){if(d->team<2){keep.push_back(d);s->players[d->team].score++;}else{s->field[d->x][d->y]=nullptr;delete d;}}s->dots=std::move(keep);s->items->nextWave=INT_MAX;s->operations->configure(*s,1);s->items->capture(*s);int me=swap,enemy=1-me;
 for(int t=0;t<6;t++)s->players[t].x[0]=s->players[t].y[0]=-2;
 if(profile==1||profile==3)order(*s,me,5,3);LocalTactics ai;auto start=std::chrono::steady_clock::now();
 for(int ms=0;ms<60000;ms+=70){
  if(ms%700==0&&s->players[me].score>0){auto& op=*s->operations;int groups=op.split[me]?3:1;
   for(int g=0;g<groups;g++){int chosen=-1;long best=LONG_MAX;for(size_t k=0;k<op.sites.size();k++){auto p=op.sites[k];auto mass=op.groups[me][g];long v=std::abs(p.x-mass.x)+std::abs(p.y-mass.y);if(profile==2){v=std::abs(p.x-s->birthX[me])+std::abs(p.y-s->birthY[me]);}else {if(p.owner==me)v+=500;if(groups>1&&int(k)%3!=g)v+=220;if(profile==3&&p.owner==enemy)v-=320;}if(v<best){best=v;chosen=int(k);}}
    if(chosen>=0){auto p=op.sites[chosen];auto& rt=op.routes[me][g];if(rt.points.empty()||rt.points[0].x!=p.x||rt.points[0].y!=p.y)route(*s,me,g,{{p.x,p.y}});}}
  }
  if(ms%280==0&&s->players[enemy].score>0){auto input=ai.plan(*s,1u<<enemy,2);for(int k=0;k<5;k++){s->players[enemy].x[k]=input[enemy*10+k];s->players[enemy].y[k]=input[enemy*10+5+k];}}
  step(*s,10);if(s->players[0].score==0||s->players[1].score==0||s->operations->winningTeam(*s)>=0)break;
 }
 valid(*s);int a=s->operations->points[me],b=s->operations->points[enemy];double seconds=std::chrono::duration<double>(std::chrono::steady_clock::now()-start).count();
 std::cout<<"{\"profile\":"<<profile<<",\"map\":"<<map<<",\"initial\":"<<count<<",\"swap\":"<<swap<<",\"control\":"<<a<<",\"opponent_control\":"<<b<<",\"units\":"<<s->players[me].score<<",\"opponent_units\":"<<s->players[enemy].score<<",\"simulation_ms\":"<<s->items->elapsed<<",\"host_seconds\":"<<seconds<<"}\n";
}
int main(int argc,char** argv){try{check(argc>1,"assets");assets.root=argv[1];if(argc>2&&std::string(argv[2])=="placement")placement();else if(argc>2&&std::string(argv[2])=="bench"){check(argc==7,"bench args");bench(std::stoi(argv[3]),std::stoi(argv[4]),std::stoi(argv[5]),std::stoi(argv[6]));}else{tests();edgeCases();captureEdges();}state=nullptr;std::cout<<"PASS "<<checks<<" operations production-core assertions\n";return 0;}catch(const std::exception& e){std::cerr<<"FAIL "<<e.what()<<" checks="<<checks<<"\n";return 1;}}
