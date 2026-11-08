// SPDX-License-Identifier: GPL-3.0-or-later
#pragma once
#include <algorithm>
#include <cmath>
#include <cstdint>
#include <queue>
#include <cstdlib>
#include <utility>
#include <vector>
namespace spawn {
// Independent initialization stream, with unbiased bounded sampling.
class Random {
 uint32_t value;
public:
 Random(int seed,int map):value(uint32_t(seed)^(uint32_t(map)+1u)*0x9e3779b9u^0x159c4a71u){if(!value)value=1;}
 uint32_t next(){value^=value<<13;value^=value>>17;value^=value<<5;return value;}
 uint32_t bounded(uint32_t n){const uint32_t low=uint32_t(-n)%n;uint32_t r;do{r=next();}while(r<low);return r%n;}
};
inline int mapFlip(int seed,int map){Random rng(int(uint32_t(seed)^0x6d2b79f5u),map);return int(rng.bounded(4));}

/** Uniform eligible floor pixels; no seats, anchor separation or fairness.
 * Preserve the original Spiral density; reject unreachable/detour pixels.
 * Fallback fills in8-neighbor walking-distance order. Previous teams have a
 * two-pixel dot margin, so teams may crowd but cannot interleave.
 * Initialization only: never reads movement/AI RNG, never reorders dots. */
template<class S> void place(S& s,int seed,int mapId){
 const int W=s.map->width,H=s.map->height,N=W*H;Random rng(seed,mapId);
 std::vector<uint8_t> blocked(N);for(int y=0;y<H;y++)for(int x=0;x<W;x++)blocked[y*W+x]=s.map->isWall(x,y)?1:0;
 std::vector<int> label(N),sizes,queue;queue.reserve(N);
 std::vector<int> distance(N),order;order.reserve(N);
 if(s.dotsPerTeam==0){
  for(int p=0;p<N;p++)if(!blocked[p])queue.push_back(p);
  if(queue.empty())std::abort();
  for(int t=0;t<6;t++){int p=queue[rng.bounded(uint32_t(queue.size()))];s.birthX[t]=p%W;s.birthY[t]=p/W;s.players[t].x[0]=short(p%W);s.players[t].y[0]=short(p/W);}return;
 }
 for(int team=0;team<6;team++){
  std::fill(label.begin(),label.end(),-1);sizes.clear();
  for(int p=0;p<N;p++)if(!blocked[p]&&label[p]<0){
   int id=int(sizes.size());queue.clear();queue.push_back(p);label[p]=id;
   for(size_t at=0;at<queue.size();at++){int q=queue[at],x=q%W,y=q/W;
    for(int dy=-1;dy<=1;dy++)for(int dx=-1;dx<=1;dx++){int nx=x+dx,ny=y+dy;if(nx<0||ny<0||nx>=W||ny>=H)continue;int v=ny*W+nx;if(!blocked[v]&&label[v]<0){label[v]=id;queue.push_back(v);}}
   }sizes.push_back(int(queue.size()));
  }
  queue.clear();for(int p=0;p<N;p++)if(label[p]>=0&&sizes[label[p]]>=s.dotsPerTeam)queue.push_back(p);
  // Invalid assets are rejected offline with the actual core; never silently
  // move anchors into fixed safe seats if an asset violates capacity.
  if(queue.empty())std::abort();
  int start=queue[rng.bounded(uint32_t(queue.size()))],sx=start%W,sy=start/W;
  s.birthX[team]=sx;s.birthY[team]=sy;
  std::fill(distance.begin(),distance.end(),0x3fffffff);order.clear();distance[start]=0;
  using Entry=std::pair<int,int>;std::priority_queue<Entry,std::vector<Entry>,std::greater<Entry>> frontier;frontier.push({0,start});
  // Exact Dijkstra settled only as far as the next spiral sample can allow.
  // This gives the same pixels as a full-map flood without doing six full
  // distance maps before a small team. No change to placement acceptance.
  auto floodUntil=[&](int maximum){while(!frontier.empty()&&frontier.top().first<=maximum){
   auto item=frontier.top();frontier.pop();int cost=item.first,p=item.second;if(cost!=distance[p])continue;order.push_back(p);int x=p%W,y=p/W;
   for(int dy=-1;dy<=1;dy++)for(int dx=-1;dx<=1;dx++){
    if(!dx&&!dy)continue;int nx=x+dx,ny=y+dy;if(nx<0||ny<0||nx>=W||ny>=H)continue;int q=ny*W+nx,next=cost+(dx&&dy?14:10);
    if(!blocked[q]&&next<distance[q]){distance[q]=next;frontier.push({next,q});}
   }
  }};
  int placed=0;auto put=[&](int x,int y){Dot* dot=new Dot(x,y,team);s.dots[team*s.dotsPerTeam+placed++]=dot;s.field[x][y]=dot;};
  for(int c=0;placed<s.dotsPerTeam&&c<160000;c++){
   int x,y;Spiral::getNextXY(sx,sy,c,&x,&y);if(x<0||y<0||x>=W||y>=H)continue;int p=y*W+x;
   if(blocked[p]||s.field[x][y])continue;
   double direct=std::hypot(double(x-sx),double(y-sy));int maximum=int(direct*12.5+60);floodUntil(maximum);if(distance[p]>maximum)continue;put(x,y);
  }
  if(placed<s.dotsPerTeam){s.spawnFallbacks++;floodUntil(0x3ffffffe);for(int p:order){if(placed==s.dotsPerTeam)break;int x=p%W,y=p/W;if(!s.field[x][y])put(x,y);}}
  if(placed!=s.dotsPerTeam)std::abort();
  for(int i=0;i<s.dotsPerTeam;i++){const Dot* d=s.dots[team*s.dotsPerTeam+i];for(int dy=-2;dy<=2;dy++)for(int dx=-2;dx<=2;dx++){int x=d->x+dx,y=d->y+dy;if(x>=0&&y>=0&&x<W&&y<H)blocked[y*W+x]=1;}}
  s.players[team].x[0]=short(sx);s.players[team].y[0]=short(sy);
 }
}
}
