// SPDX-License-Identifier: GPL-3.0-or-later
#pragma once
#include "state.hpp"
#include <array>
#include <algorithm>
#include <vector>
namespace model_observation {
// Header120 + adjacency matrix36 + actual cursor inputs60 + particle count1 +
// one immutable 26-bit x/y/team/health word per particle. No rule/RNG writes.
inline std::vector<int> snapshot(const State& s) {
    std::vector<int> out(217+s.dots.size(),0);
    out[216]=int(s.dots.size());
    for(int p=0;p<6;p++)for(int i=0;i<5;i++){out[156+p*10+i]=s.players[p].x[i];out[156+p*10+5+i]=s.players[p].y[i];}
    int counts[6][240]={},sx[6][240]={},sy[6][240]={};
    const int W=s.map->width,H=s.map->height;const bool wide=W>1024;
    size_t index=217;
    for(const Dot* d:s.dots){int p=d->team;if(p>=6||d->x>=W||d->y>=H)continue;out[index++]=W>2048?(int(d->x)|(int(d->y)<<12)|(p<<22)|(int(d->health)<<25)):wide?(int(d->x)|(int(d->y)<<11)|(p<<21)|(int(d->health)<<24)):(int(d->x)|(int(d->y)<<10)|(p<<19)|(int(d->health)<<22));int at=p*8;out[at]++;out[at+1]+=d->health;out[at+2]+=d->x;out[at+3]+=d->y;int c=(int(d->y)*12/H)*20+int(d->x)*20/W;counts[p][c]++;sx[p][c]+=d->x;sy[p][c]+=d->y;
        for(int dx=-1;dx<=1;dx++)for(int dy=-1;dy<=1;dy++){int x=int(d->x)+dx,y=int(d->y)+dy;if(x<0||x>=W||y<0||y>=H)continue;const Dot* other=s.field[x][y];if(other&&other->team<6&&other->team!=p)out[120+p*6+other->team]++;}}
    for(int p=0;p<6;p++){int at=p*8;if(!out[at])continue;out[at+1]=out[at+1]*100/(MAX_HEALTH*out[at]);out[at+2]/=out[at];out[at+3]/=out[at];
        std::array<int,240> order{};for(int c=0;c<240;c++)order[c]=c;
        std::partial_sort(order.begin(),order.begin()+4,order.end(),[&](int a,int b){return counts[p][a]!=counts[p][b]?counts[p][a]>counts[p][b]:a<b;});
        for(int k=0;k<4;k++){int c=order[k],n=counts[p][c],offset=48+p*12+k*3;if(!n)continue;out[offset]=sx[p][c]/n;out[offset+1]=sy[p][c]/n;out[offset+2]=n;}
        out[at+4]=out[48+p*12];out[at+5]=out[49+p*12];out[at+6]=out[50+p*12];
        for(const Dot* d:s.dots)if(d->team<6&&s.alliances[d->team]!=s.alliances[p]){int dx=int(d->x)-out[at+4],dy=int(d->y)-out[at+5];if(dx*dx+dy*dy<48*48)out[at+7]++;}
    }return out;
}
inline std::vector<unsigned char> terrain(const State& s){std::vector<unsigned char> out(s.map->width*s.map->height);for(int y=0;y<s.map->height;y++)for(int x=0;x<s.map->width;x++)out[y*s.map->width+x]=s.map->isWall(x,y)?1:0;return out;}
}
