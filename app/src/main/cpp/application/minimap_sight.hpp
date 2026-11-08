// SPDX-License-Identifier: GPL-3.0-or-later
#pragma once
#include "state.hpp"
#include <array>
#include <vector>
#include <cmath>
namespace minimap_sight {
constexpr int MAGIC=0x4c575349,BIN=32,RADIUS=144;
struct Point {int x=-1,y=-1;};
struct Cell {int count[6]={};Point enemy[6];std::array<Point,4> scouts;};
inline bool clear(const State& s,int x,int y,int tx,int ty){int dx=std::abs(tx-x),dy=-std::abs(ty-y),sx=x<tx?1:-1,sy=y<ty?1:-1,error=dx+dy;while(x!=tx||y!=ty){int e=2*error;if(e>=dy){error+=dy;x+=sx;}if(e<=dx){error+=dx;y+=sy;}if(s.map->isWall(x,y))return false;}return true;}
// Read-only sight; enemy markers are actual particles, never hidden-army centres.
inline std::vector<int> snapshot(const State& s,bool all){
 const int w=s.map->width,h=s.map->height,cols=(w+BIN-1)/BIN,rows=(h+BIN-1)/BIN;std::vector<Cell> cells(cols*rows);
 for(auto d:s.dots){int t=d->team,cx=d->x/BIN,cy=d->y/BIN;auto& c=cells[cy*cols+cx];c.count[t]++;if(c.enemy[t].x<0)c.enemy[t]={d->x,d->y};if(s.allied(s.me,t)){int at=(d->x%BIN>=16?1:0)+(d->y%BIN>=16?2:0);if(c.scouts[at].x<0)c.scouts[at]={d->x,d->y};}}
 std::vector<int> out={MAGIC,w,h,s.items->elapsed,0};int count=0;
 for(int cy=0;cy<rows;cy++)for(int cx=0;cx<cols;cx++){auto& c=cells[cy*cols+cx];for(int t=0;t<6;t++){
  if(!c.count[t]||(!all&&s.items->concealedFrom(s,s.me,t)))continue;bool visible=all||s.allied(s.me,t);Point target=c.enemy[t];
  if(!visible)for(int yy=std::max(0,cy-5);yy<=std::min(rows-1,cy+5)&&!visible;yy++)for(int xx=std::max(0,cx-5);xx<=std::min(cols-1,cx+5)&&!visible;xx++)for(auto p:cells[yy*cols+xx].scouts){if(p.x<0)continue;int dx=p.x-target.x,dy=p.y-target.y;if(dx*dx+dy*dy<=RADIUS*RADIUS&&clear(s,p.x,p.y,target.x,target.y)){visible=true;break;}}
  if(visible){out.push_back(t);out.push_back(target.x);out.push_back(target.y);out.push_back(c.count[t]);count++;}
 }}
 out[4]=count;return out;
}
}
