// Reproducible natural pickups for device validation. Does not alter spawn rules.
#define main unused_items_checks
#include "check_items_core.cpp"
#undef main
#include "expedition.hpp"
int main(){assets.root="app/src/main/assets";for(int seed=1;seed<=60;seed++){State s(0,1001,seed,1000,true,2);auto distance=[&](int t,int x,int y){return std::hypot(s.birthX[t]-x,s.birthY[t]-y);};for(auto p:s.expedition->loot)if(p.kind==Expedition::BLACK_HOLE_LOOT&&distance(0,p.x,p.y)<600&&distance(1,p.x,p.y)>distance(0,p.x,p.y)*2){std::cout<<"seed="<<seed<<" birth="<<s.birthX[0]<<","<<s.birthY[0]<<" hole="<<p.x<<","<<p.y<<" distance="<<distance(0,p.x,p.y)<<"\n";for(auto l:s.expedition->loot)std::cout<<l.id<<","<<l.kind<<","<<l.x<<","<<l.y<<"\n";return 0;}}return 1;}
