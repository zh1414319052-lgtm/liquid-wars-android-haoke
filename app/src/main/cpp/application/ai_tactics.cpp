// SPDX-License-Identifier: GPL-3.0-or-later
#include "ai_tactics.hpp"
#include <algorithm>
#include <cmath>
#include <limits>
#include <utility>

bool AiTactics::clear(int x,int y,int tx,int ty) const {
    int dx=std::abs(tx-x),dy=std::abs(ty-y),sx=x<tx?1:-1,sy=y<ty?1:-1,error=dx-dy;
    for(;;) {
        if(wall(x,y))return false;
        if(x==tx&&y==ty)return true;
        int twice=2*error;
        if(twice>-dy){error-=dy;x+=sx;}
        if(twice< dx){error+=dx;y+=sy;}
    }
}
AiTactics::AiTactics(int w,int h,std::function<bool(int,int)> query,std::array<int,6> groups)
    :width(w),height(h),columns((w+CELL-1)/CELL),rows((h+CELL-1)/CELL),alliances(groups),wall(std::move(query)),
     cells(columns*rows),buckets(columns*rows*6),distance(columns*rows),parent(columns*rows),queue(columns*rows) {
    for(int cy=0;cy<rows;cy++)for(int cx=0;cx<columns;cx++) {
        Cell& cell=cells[cy*columns+cx];int best=10000;
        int mx=std::min(width-1,cx*CELL+CELL/2),my=std::min(height-1,cy*CELL+CELL/2);
        for(int y=cy*CELL;y<std::min(height,(cy+1)*CELL);y++)for(int x=cx*CELL;x<std::min(width,(cx+1)*CELL);x++) {
            int cost=(x-mx)*(x-mx)+(y-my)*(y-my);
            if(cost<best&&!wall(x,y)){best=cost;cell.x=short(x);cell.y=short(y);}
        }
    }
    const int dx[]={1,-1,0,0},dy[]={0,0,1,-1};
    for(int y=0;y<rows;y++)for(int x=0;x<columns;x++) {
        Cell& cell=cells[y*columns+x];if(cell.x<0)continue;
        for(int d=0;d<4;d++) {
            int nx=x+dx[d],ny=y+dy[d];if(nx<0||ny<0||nx>=columns||ny>=rows)continue;
            const Cell& next=cells[ny*columns+nx];
            if(next.x>=0&&clear(cell.x,cell.y,next.x,next.y))cell.edges|=1u<<d;
        }
    }
}
int AiTactics::sourceCell(int x,int y) const {
    int found=-1,best=100000;
    for(int cy=std::max(0,y/CELL-2);cy<std::min(rows,y/CELL+3);cy++)for(int cx=std::max(0,x/CELL-2);cx<std::min(columns,x/CELL+3);cx++) {
        int index=cy*columns+cx;const Cell& c=cells[index];if(c.x<0)continue;
        int cost=(x-c.x)*(x-c.x)+(y-c.y)*(y-c.y);
        if(cost<best&&clear(x,y,c.x,c.y)){best=cost;found=index;}
    }
    return found;
}
std::array<short,60> AiTactics::plan(const std::vector<Dot*>& dots,const std::array<int,12>* strategy,const std::array<unsigned,6>* hiddenMasks) {
    std::array<short,60> result;result.fill(-1);
    std::fill(buckets.begin(),buckets.end(),Bucket{});
    int totals[6]={0};const int n=int(cells.size());
    for(const Dot* d:dots) {
        if(d->team>=6||d->x>=width||d->y>=height)continue;
        Bucket& b=buckets[d->team*n+(d->y/CELL)*columns+d->x/CELL];b.count++;b.sx+=d->x;b.sy+=d->y;totals[d->team]++;
    }
    const int offset[]={1,-1,columns,-columns};
    for(int team=0;team<6;team++) {
        if(!totals[team])continue;
        int mass=-1;
        for(int i=0;i<n;i++)if(buckets[team*n+i].count>0&&(mass<0||buckets[team*n+i].count>buckets[team*n+mass].count))mass=i;
        const Bucket& own=buckets[team*n+mass];int ox=own.sx/own.count,oy=own.sy/own.count;
        // The centroid can be inside an obstacle: anchor to an actual friendly dot.
        const Dot* anchor=nullptr;int closest=1000000;
        for(const Dot* d:dots)if(d->team==team) {int cost=(int(d->x)-ox)*(int(d->x)-ox)+(int(d->y)-oy)*(int(d->y)-oy);if(cost<closest){closest=cost;anchor=d;}}
        if(!anchor)continue;ox=anchor->x;oy=anchor->y;int start=sourceCell(ox,oy);if(start<0)continue;
        const int preferred=strategy?(*strategy)[team*2]:-1;
        const int style=strategy?(*strategy)[team*2+1]:0;
        if(style==2){result[team*10]=short(ox);result[team*10+5]=short(oy);continue;}
        std::fill(distance.begin(),distance.end(),-1);int head=0,tail=0;queue[tail++]=start;distance[start]=0;parent[start]=-1;
        while(head<tail) {
            int at=queue[head++];for(int d=0;d<4;d++)if(cells[at].edges&(1u<<d)) {
                int next=at+offset[d];if(distance[next]>=0)continue;
                distance[next]=distance[at]+1;parent[next]=at;queue[tail++]=next;
            }
        }
        int goal=-1,tx=0,ty=0;double best=std::numeric_limits<double>::max();
        for(int enemy=0;enemy<6;enemy++)if(alliances[enemy]!=alliances[team]&&(!hiddenMasks||!((*hiddenMasks)[team]&(1u<<enemy)))&&totals[enemy])for(int i=0;i<n;i++) {
            const Bucket& b=buckets[enemy*n+i];if(!b.count||distance[i]<0)continue;
            int x=b.sx/b.count,y=b.sy/b.count;
            if(!clear(cells[i].x,cells[i].y,x,y))continue;
            // Prefer reachable nearby groups and favourable fights, rather than stragglers
            // behind walls. No health/speed/count advantage and no human-only targeting.
            double cost=distance[i]*(1.0+.35*totals[enemy]/double(totals[team]+1))-.8*std::sqrt(double(b.count));
            if(preferred>=0&&preferred<6&&preferred!=team&&enemy!=preferred)cost+=100000;
            if(cost<best){best=cost;goal=i;tx=x;ty=y;}
        }
        if(goal<0)continue;
        int wx=tx,wy=ty;
        if(!clear(ox,oy,tx,ty)) {
            int at=goal;while(at>=0&&!clear(ox,oy,cells[at].x,cells[at].y))at=parent[at];
            if(at<0)continue;wx=cells[at].x;wy=cells[at].y;
        }
        result[team*10]=short(wx);result[team*10+5]=short(wy);
        // Nearby fights use close lateral targets; distant armies stay concentrated
        // around one navigable waypoint instead of splitting across the whole map.
        int dx=tx-ox,dy=ty-oy;float length=std::sqrt(float(dx*dx+dy*dy));
        if(length>2&&length<48&&wx==tx&&wy==ty)for(int side=0;side<2;side++) {
            float sign=side==0?-1.f:1.f;
            float spread=style==1?12.f:6.f;
            int x=int(std::lround(tx-sign*dy/length*spread)),y=int(std::lround(ty+sign*dx/length*spread));
            if(!wall(x,y)&&clear(ox,oy,x,y)){result[team*10+side+1]=short(x);result[team*10+side+6]=short(y);}
        }
    }
    return result;
}
