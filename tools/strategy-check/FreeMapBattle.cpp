// SPDX-License-Identifier: GPL-3.0-or-later
// Actual rule core and HARD planner; no model requests, no rendering.
#include "state.hpp"
#include "move.hpp"
#include "ai.hpp"
#include "ai_tactics.hpp"
#include <memory>
#include <fstream>
#include <iostream>
#include <sstream>
#include <algorithm>
#include <array>
#include <queue>
#include <set>
#include <limits>
#include <chrono>
HostAssets assets;HostAssets* acAssets=&assets;
static std::array<double,6> nearest(const State& s){
 std::array<double,6> out;std::vector<int> dist(384000);std::vector<unsigned char> wall(384000);for(int y=0;y<480;y++)for(int x=0;x<800;x++)wall[y*800+x]=s.map->isWall(x,y);
 for(int t=0;t<6;t++){
  std::fill(dist.begin(),dist.end(),1<<29);int start=s.birthY[t]*800+s.birthX[t];dist[start]=0;using P=std::pair<int,int>;std::priority_queue<P,std::vector<P>,std::greater<P>> q;q.push({0,start});double best=1e9;
  while(!q.empty()){auto item=q.top();q.pop();int d=item.first,p=item.second;if(d!=dist[p])continue;bool found=false;for(int u=0;u<6;u++)if(u!=t&&p==s.birthY[u]*800+s.birthX[u]){found=true;break;}if(found){best=d/10.;break;}
   int x=p%800,y=p/800;for(int dy=-1;dy<=1;dy++)for(int dx=-1;dx<=1;dx++){if(!dx&&!dy)continue;int nx=x+dx,ny=y+dy;if(nx<0||ny<0||nx>=800||ny>=480)continue;int v=ny*800+nx,nd=d+(dx&&dy?14:10);if(!wall[v]&&nd<dist[v]){dist[v]=nd;q.push({nd,v});}}
  }out[t]=best;
 }return out;
}
static bool contact(const State& s,int& x,int& y){for(auto d:s.dots)for(int dy=-1;dy<=1;dy++)for(int dx=-1;dx<=1;dx++){int nx=d->x+dx,ny=d->y+dy;if(nx<0||ny<0||nx>=800||ny>=480)continue;auto other=s.field[nx][ny];if(other&&other->team!=d->team){x=nx;y=ny;return true;}}return false;}
static int redFace(const State& s){for(int k=2000;k<3000;k++){const Dot* d=s.dots[k];for(int dy=-5;dy<=5;dy++)for(int dx=-5;dx<=5;dx++){int x=d->x+dx,y=d->y+dy;if(x<0||y<0||x>=800||y>=480)continue;auto other=s.field[x][y];if(other&&other->team!=2)return other->team;}}return -1;}
int main(int argc,char** argv){
 assets.root=argv[1];int first=std::stoi(argv[2]),last=std::stoi(argv[3]);std::string path=argv[4];std::set<std::pair<int,int>> done;
 {std::ifstream in(path);std::string l;std::getline(in,l);while(std::getline(in,l)){std::replace(l.begin(),l.end(),',',' ');std::istringstream s(l);int m,k;if(s>>m>>k)done.insert({m,k});}}
 std::ofstream out(path,std::ios::app);if(done.empty()){out<<"map,seed,flip,first_contact_s,contact_x,contact_y,end_s,winner,tied,nearest_credit,farthest_credit,red_face_enemy,spawn_fallbacks";for(int t=0;t<6;t++)out<<",score"<<t;for(int t=0;t<6;t++)out<<",nearest_walk"<<t;for(int t=0;t<6;t++)out<<",x"<<t<<",y"<<t;out<<'\n';}
 for(int map=first;map<=last;map++){
  auto at=std::chrono::steady_clock::now();int seeds=(map==0||map==12||map==23||map==37||map==49)?24:16;
  for(int k=0;k<seeds;k++){int seed=921+k;if(done.count({map,seed}))continue;auto owned=std::make_unique<State>(0,map,seed,1000);state=owned.get();auto walks=nearest(*state);int face=redFace(*state),fallbacks=state->spawnFallbacks;
   AiTactics planner(800,480,[&](int x,int y){return state->map->isWall(x,y);});std::array<short,60> planned;planned.fill(-1);int tick=0,delay=6,step=0,fx=-1,fy=-1;double firstContact=-1;
   for(;step<17140;step+=10){for(int i=0;i<10;i++)Move::stepDots();if(firstContact<0&&contact(*state,fx,fy))firstContact=(step+10)*.007;bool won=false;for(int t=0;t<6;t++)won|=state->players[t].score==6000;if(won)break;
    if(delay--<0){if(tick%8==0)planned=planner.plan(state->dots);for(int t=0;t<6;t++){int n=AI::getNearestDot(t,state->players[t].x[0],state->players[t].y[0]);if(planned[t*10]>=0&&planned[t*10+5]>=0){for(int i=0;i<5;i++){state->players[t].x[i]=planned[t*10+i];state->players[t].y[i]=planned[t*10+5+i];}}else{state->players[t].x[0]=short(uint32_t(n)>>16);state->players[t].y[0]=short(n);for(int i=1;i<5;i++)state->players[t].x[i]=state->players[t].y[i]=-1;}}tick++;}
   }
   int winner=0;for(int t=1;t<6;t++)if(state->players[t].score>state->players[winner].score)winner=t;int tops=0,close=0,far=0,closeWin=0,farWin=0;double lo=*std::min_element(walks.begin(),walks.end()),hi=*std::max_element(walks.begin(),walks.end());
   for(int t=0;t<6;t++){bool top=state->players[t].score==state->players[winner].score;tops+=top;bool near=std::abs(walks[t]-lo)<.01,distant=std::abs(walks[t]-hi)<.01;close+=near;far+=distant;closeWin+=top&&near;farWin+=top&&distant;}
   out<<map<<','<<seed<<','<<state->map->transform()<<','<<firstContact<<','<<fx<<','<<fy<<','<<std::min(120.,(step+10)*.007)<<','<<winner<<','<<(tops>1)<<','<<double(closeWin)/(tops*close)<<','<<double(farWin)/(tops*far)<<','<<face<<','<<fallbacks;
   for(int t=0;t<6;t++)out<<','<<state->players[t].score;for(double d:walks)out<<','<<d;for(int t=0;t<6;t++)out<<','<<state->birthX[t]<<','<<state->birthY[t];out<<'\n';out.flush();
  }std::cout<<"map="<<map<<" games="<<seeds<<" wall_s="<<std::chrono::duration<double>(std::chrono::steady_clock::now()-at).count()<<std::endl;
 }return 0;
}
