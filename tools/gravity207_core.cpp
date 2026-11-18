// SPDX-License-Identifier: GPL-3.0-or-later
#define BLACK_HOLE_TEST_LIBRARY
#include "black_hole205_core.cpp"

static void reaction(int seats){
 auto match=std::make_unique<State>(0,seats?1001:1,207,100,seats>0,seats);
 auto& s=*match;auto& i=*s.items;i.warmup=0;i.nextWave=INT_MAX;
 if(s.expedition){s.expedition->loot.clear();s.expedition->nextLoot=s.expedition->nextAI=INT_MAX;for(auto& seat:s.expedition->seats)seat.bot=false;}
 for(int t=0;t<s.teamCount;t++){s.operations->clearTeam(s,t);s.players[t].x[0]=s.players[t].y[0]=-2;}
 Dot* core=nullptr;const int cx=s.map->width/2,cy=s.map->height/2;
 for(int dy=-5;dy<=5&&!core;dy++)for(int dx=-5;dx<=5;dx++)if(dx*dx+dy*dy<=25&&!s.map->isWall(cx+dx,cy+dy)&&!s.field[cx+dx][cy+dy]){core=add(s,0,cx+dx,cy+dy);break;}
 check(core!=nullptr,"real legal core cell");
 i.grant(s,0,Items::ACTIVE_BLACK_HOLE,0,0);i.grant(s,0,Items::ACTIVE_CLEANSE,0,0);
 check(i.activate(s,0,1,Items::BLACK_HOLE_SLOT)==Expedition::OK,"cast");
 const auto alive=[&]{return std::find(s.dots.begin(),s.dots.end(),core)!=s.dots.end();};
 step(s,200);
 check(i.elapsed-i.holeStart==1400&&alive()&&i.holeAbsorbed==0,"even inside the core a player has 1.4s to react");
 check(i.activate(s,0,2,1)==Expedition::OK&&!i.pulling(0),"cleanse works during warning and removes own gravity cue");
 step(s,230);
 check(alive()&&i.blackHoleActive()&&i.pulling(1),"early cleanse protects the caster while others remain affected");
 i.rewind(s);check(!i.pulling(0)&&!i.pulling(1),"rewind removes all gravity strands");
 state=nullptr;std::cout<<(seats?"3D":"2D")<<" reaction window + early cleanse PASS\n";
}
int main(int argc,char** argv){try{check(argc==2,"assets required");assets.root=argv[1];reaction(0);reaction(2);checkMode(0);checkMode(2);std::cout<<"PASS "<<checks<<" checks\n";return 0;}catch(const std::exception& e){std::cerr<<"FAIL "<<e.what()<<" after "<<checks<<" checks\n";return 1;}}
