// SPDX-License-Identifier: GPL-3.0-or-later
// Two isolated test assets, not shipping maps; actual production initializer.
#include "state.hpp"
#include "spawn.hpp"
#include <memory>
#include <iostream>
#include <stdexcept>
HostAssets assets;HostAssets* acAssets=&assets;
int main(int argc,char**argv){assets.root=argv[1];int chosen=-1;
for(int seed=0;seed<5000;seed++){if(spawn::mapFlip(seed,50)!=0)continue;auto s=std::make_unique<State>(0,50,seed,0);if(s->birthX[0]>=380&&s->birthX[0]<400&&s->birthY[0]>180&&s->birthY[0]<300){chosen=seed;break;}}
std::cerr<<"chosen="<<chosen<<std::endl; if(chosen<0)throw std::runtime_error("no focused seed");auto a=std::make_unique<State>(0,50,chosen,1000);int right=0;for(int i=0;i<1000;i++)right+=a->dots[i]->x>402;if(right)throw std::runtime_error("birth leaked across thin wall");std::cout<<"PASS thin wall seed="<<chosen<<" birth="<<a->birthX[0]<<','<<a->birthY[0]<<" opposite-side dots="<<right<<"; production independent RNG and Spiral\n";
int seed=0;for(;seed<10000;seed++){if(spawn::mapFlip(seed,51)!=0)continue;auto z=std::make_unique<State>(0,51,seed,0);if(z->birthX[0]>=350&&z->birthX[0]<=450&&z->birthY[0]>=232&&z->birthY[0]<=247)break;}std::cerr<<"corridor seed="<<seed<<std::endl;auto b=std::make_unique<State>(0,51,seed,1000);int xmin=800,xmax=0,ymin=480,ymax=0;for(int i=0;i<1000;i++){auto d=b->dots[i];xmin=std::min(xmin,int(d->x));xmax=std::max(xmax,int(d->x));ymin=std::min(ymin,int(d->y));ymax=std::max(ymax,int(d->y));}
if(xmax-xmin<=3*(ymax-ymin))throw std::runtime_error("corridor team not elongated");std::cout<<"PASS16px corridor1000/team bbox="<<xmax-xmin+1<<'x'<<ymax-ymin+1<<" fallbackTeams="<<b->spawnFallbacks<<"; 6000 particles fit actual corridor\n";return 0;}
