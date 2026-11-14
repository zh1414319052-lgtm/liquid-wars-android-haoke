// SPDX-License-Identifier: GPL-3.0-or-later
// Real Move and collision, isolated1000-unit doorway fixture; other lanes idle
// and absent from field so they cannot contaminate this passage measurement.
#include "state.hpp"
#include "move.hpp"
#include "spawn.hpp"
#include <fstream>
#include <sstream>
#include <iostream>
#include <memory>
#include <queue>
#include <cmath>
HostAssets assets;HostAssets* acAssets=&assets;
int main(int argc,char**argv){assets.root=argv[1];std::ifstream input(argv[2]);std::ofstream out(argv[3]);std::string line;std::getline(input,line);out<<"map,width_sample,direction,t90_s,passed20s,initial1000\n";int fail=0;
while(std::getline(input,line)){std::replace(line.begin(),line.end(),',',' ');std::istringstream row(line);int map,gx,gy,sx,sy,tx,ty;double width;row>>map>>gx>>gy>>width>>sx>>sy>>tx>>ty;
for(int rev=0;rev<2;rev++){if(rev){std::swap(sx,tx);std::swap(sy,ty);}int seed=0;while(spawn::mapFlip(seed,map)!=0)seed++;auto owned=std::make_unique<State>(0,map,seed,0);state=owned.get();state->dotsPerTeam=1000;state->dots.resize(6000);double dx=tx-sx,dy=ty-sy;
std::vector<int> dist(384000,1<<29);using E=std::pair<int,int>;std::priority_queue<E,std::vector<E>,std::greater<E>> q;dist[sy*800+sx]=0;q.push({0,sy*800+sx});
while(!q.empty()){auto [d,p]=q.top();q.pop();if(d!=dist[p])continue;int x=p%800,y=p/800;for(int yy=-1;yy<=1;yy++)for(int xx=-1;xx<=1;xx++){int nx=x+xx,ny=y+yy;if(state->map->isWall(nx,ny)||(nx-gx)*dx+(ny-gy)*dy>=0)continue;int id=ny*800+nx,nd=d+(xx&&yy?14:10);if(nd<dist[id]){dist[id]=nd;q.push({nd,id});}}}
int n=0;for(int c=0;n<1000&&c<160000;c++){int x,y;Spiral::getNextXY(sx,sy,c,&x,&y);if(state->map->isWall(x,y)||state->field[x][y]||dist[y*800+x]>std::hypot(double(x-sx),double(y-sy))*12.5+60)continue;auto d=new Dot(x,y,0);state->dots[n++]=d;state->field[x][y]=d;}
if(n!=1000){std::cerr<<"fixture insufficient source area "<<map<<'\n';return 2;}for(int k=1000;k<6000;k++)state->dots[k]=new Dot(0,0,k/1000);for(int t=0;t<6;t++){state->players[t].score=1000;for(int k=0;k<5;k++)state->players[t].x[k]=state->players[t].y[k]=k?-1:-2;}
double t90=-1;int passed=0;for(int step=0;step<2858;step++){state->players[0].x[0]=step*.007<3?gx:tx;state->players[0].y[0]=step*.007<3?gy:ty;Move::stepDots();if(step%10==0){passed=0;for(int i=0;i<1000;i++){auto d=state->dots[i];passed+=(int(d->x)-gx)*dx+(int(d->y)-gy)*dy>0;}if(passed>=900){t90=(step+1)*.007;break;}}}out<<map<<','<<width<<','<<rev<<','<<t90<<','<<passed<<','<<n<<'\n';out.flush();if(t90<0)fail++;}}
std::cout<<"failed "<<fail<<" directional gate cases; raw failures retained\n";return 0;}
