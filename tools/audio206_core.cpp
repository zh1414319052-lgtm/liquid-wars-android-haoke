// SPDX-License-Identifier: GPL-3.0-or-later
#define BLACK_HOLE_TEST_LIBRARY
#include "black_hole205_core.cpp"

static void audioMode(int seats){
 auto match=std::make_unique<State>(0,seats?1001:1,206,100,seats>0,seats);
 auto& s=*match;auto& i=*s.items;i.warmup=0;
 Dot* own=nullptr;Dot* enemy=nullptr;
 for(auto d:s.dots){if(d->team==0)own=d;if(d->team==1)enemy=d;}
 check(own&&enemy,"two actual armies");
 auto before=simulationHash(s);auto facts=i.audioSnapshot(s,0);
 check(facts.size()==6&&facts[4]==0&&simulationHash(s)==before,"read-only sound snapshot");
 i.contact(s,*enemy,*own);
 check(i.audioSnapshot(s,0)[4]==0,"ordinary damage without capture is silent");
 enemy->health=1;
 for(int n=0;n<20&&enemy->team==1;n++)i.contact(s,*enemy,*own);
 check(enemy->team==0&&i.audioSnapshot(s,0)[4]==1&&i.audioSnapshot(s,1)[4]==1,"actual capture audible to both participants");
 check(i.audioSnapshot(s,0,true)[4]==1,"spectator counts each conversion once");
 i.grow(s,0,5);
 check(i.audioSnapshot(s,0)[4]==1,"production is not swallowing");
 i.grant(s,0,Items::ACTIVE_BLACK_HOLE,0,0);i.activate(s,0,1,Items::BLACK_HOLE_SLOT);
 facts=i.audioSnapshot(s,0);
 check(facts[3]-facts[2]==10000,"both modes expose ten-second audio timeline");
 check(facts[4]==1,"casting black hole does not imitate particle combat");
 i.rewind(s);facts=i.audioSnapshot(s,0);
 check(facts[4]==0&&facts[3]==0,"rewind clears counters and long sound");
 state=nullptr;
 std::cout<<(seats?"3D":"2D")<<" actual-capture audio facts PASS\n";
}
int main(int argc,char** argv){try{check(argc==2,"assets required");assets.root=argv[1];audioMode(0);audioMode(2);checkMode(0);checkMode(2);std::cout<<"PASS "<<checks<<" checks\n";return 0;}catch(const std::exception& e){std::cerr<<"FAIL "<<e.what()<<" after "<<checks<<" checks\n";return 1;}}
