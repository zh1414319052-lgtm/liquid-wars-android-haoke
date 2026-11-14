// SPDX-License-Identifier: GPL-3.0-or-later
// Android NDK regression executable; asset adapter only, never an alternate renderer.
#define main items_regression_main
#include "check_items_core.cpp"
#undef main
int main(int argc,char** argv){
 if(argc!=2)return 1;assets.root=argv[1];effects();schedule();replay();
 for(int map=1;map<=49;map++){
  auto s=std::make_unique<State>(2,map,16800+map,100);state=s.get();
  for(int f=0;f<=140;f++){
   if(f%10==0)std::cout<<map<<','<<f<<','<<hash(*s)<<'\n';
   if(f==140)break;for(int t=0;t<6;t++){s->players[t].x[0]=(f*7+t*91)%780+10;s->players[t].y[0]=(f*3+t*63)%460+10;}Move::stepDots();
  }
 }
 for(int seed=0;seed<128;seed++){
  auto s=std::make_unique<State>(0,1+seed%49,seed,seed%4==0?4500:400);state=s.get();
  for(int f=0;f<=21;f++){if(f%7==0)std::cout<<"scale,"<<seed<<','<<f<<','<<hash(*s)<<'\n';if(f<21)Move::stepDots();}
 }
 state=nullptr;std::cout<<"PASS Android NDK trace and "<<checks<<" core assertions\n";
}
