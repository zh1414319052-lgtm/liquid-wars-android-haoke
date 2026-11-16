// SPDX-License-Identifier: GPL-3.0-or-later
#include "garden3d.hpp"
#include "state.hpp"
#include "free_spawn.hpp"
#include <cstring>
#include <cstdlib>
#include <cmath>
#include <string>
namespace garden3d {
bool assetsValid(){
 for(const char* name:{"garden3d/height.bin","garden3d/arena-map.png","garden3d/arena-image.png","garden3d/sites.bin"}){int n=0;auto p=acAssets->getFile(name,&n);bool good=p&&n>=16;free(p);if(!good)return false;}
 for(const char* name:{"garden3d/live/meshes.bin","garden3d/live/river.bin","garden3d/live/world.vert","garden3d/live/world.frag"}){int n=0;auto p=acAssets->getFile(name,&n);bool good=p&&n>=8;free(p);if(!good)return false;}return true;
}
void place(State& s,int seed){
 s.field.clear();for(auto d:s.dots)delete d;s.dots.clear();s.spawnFallbacks=0;
 for(int t=0;t<MAX_TEAMS;t++){s.players[t]=Player{};s.players[t].score=0;s.birthX[t]=s.birthY[t]=-1;}
 // Uniform rejection sampling over all cells with space for one compact army.
 // Independent initialization RNG never advances the movement or AI streams.
 spawn::Random rng(seed,MAP_ID);int radius=int(std::ceil(std::sqrt(s.dotsPerTeam/3.141592653589793)))+3;
 std::vector<std::pair<int,int>> offsets;
 for(int dy=-radius;dy<=radius;dy++)for(int dx=-radius;dx<=radius;dx++)if(dx*dx+dy*dy<=radius*radius)offsets.push_back({dx,dy});
 std::stable_sort(offsets.begin(),offsets.end(),[](const std::pair<int,int>& a,const std::pair<int,int>& b){return a.first*a.first+a.second*a.second<b.first*b.first+b.second*b.second;});
 std::vector<int> teams;if(s.expeditionMode){for(int t=0;t<s.teamCount;t++)teams.push_back(t);}else teams={0,2};
 for(int t:teams){
  int x=0,y=0;bool found=false;
  for(int attempt=0;attempt<8192&&!found;attempt++){
   x=int(rng.bounded(ARENA_WIDTH));y=int(rng.bounded(ARENA_HEIGHT));
   if(s.map->isWall(x,y))continue;
   bool separate=true;for(int prev:teams){if(prev==t)break;int dx=x-s.birthX[prev],dy=y-s.birthY[prev];int gap=s.expeditionMode?180:2*radius+8;if(dx*dx+dy*dy<gap*gap){separate=false;break;}}if(!separate)continue;
   found=true;for(auto p:offsets)if(s.map->isWall(x+p.first,y+p.second)||s.field[x+p.first][y+p.second]){found=false;break;}
  }
  // Entry reports failure instead of placing a partial army in fixed seats.
  if(!found){s.spawnFallbacks=-1;return;}
  s.birthX[t]=x;s.birthY[t]=y;s.players[t].x[0]=short(x);s.players[t].y[0]=short(y);
  for(int i=0;i<s.dotsPerTeam;i++){auto p=offsets[i];auto d=new Dot(x+p.first,y+p.second,t);s.dots.push_back(d);s.field[d->x][d->y]=d;s.players[t].score++;}
 }
 s.points.assign(s.dots.size()*3,0);s.colours.assign(s.dots.size()*4,1);
}
}
