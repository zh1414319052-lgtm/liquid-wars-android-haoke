// SPDX-License-Identifier: GPL-3.0-or-later
#include "local_tactics.hpp"
#include "minimap_sight.hpp"
#include <algorithm>
#include <cmath>
#include <limits>
#include <queue>
namespace {
int sq(int n){return n*n;}
float length(int x,int y,int xx,int yy){return std::sqrt(float(sq(x-xx)+sq(y-yy)));}
constexpr int INF=100000000;
}
void LocalTactics::reset(){width=height=0;revision=epoch=-1;builtAt=-10000;nodes.clear();visits.clear();decisions={};}
bool LocalTactics::clear(const State& s,int x,int y,int tx,int ty,int ignore,bool phase)const {
 if(tx<16||ty<4||tx>=width-16||ty>=height-4)return false;
 if(!phase&&!minimap_sight::clear(s,x,y,tx,ty))return false;
 if(!s.items->enabled)return true;
 const float dx=float(tx-x),dy=float(ty-y),d=dx*dx+dy*dy;
 for(int k=0;k<6;k++){const auto& p=s.items->pickups[k];if(p.kind<0||k==ignore)continue;float u=d?std::max(0.f,std::min(1.f,((p.x-x)*dx+(p.y-y)*dy)/d)):0;
  if(sq(Items::radiusForHp(p.hp))>=(x+u*dx-p.x)*(x+u*dx-p.x)+(y+u*dy-p.y)*(y+u*dy-p.y))return false;
 }return true;
}
void LocalTactics::build(const State& s){
 width=s.map->width;height=s.map->height;cols=(width+GRID-1)/GRID;rows=(height+GRID-1)/GRID;bcols=(width+BIN-1)/BIN;brows=(height+BIN-1)/BIN;
 nodes.assign(cols*rows,Node{});cost.resize(nodes.size());parent.resize(nodes.size());
 if(visits.size()!=size_t(bcols*brows*6))visits.assign(bcols*brows*6,-100000);
 for(int cy=0;cy<rows;cy++)for(int cx=0;cx<cols;cx++){auto& n=nodes[cy*cols+cx];int best=INF;
  // Build and execution must use the same playable control bounds. A floor
  // pixel under the edge/sidebar must never become an unreachable graph goal.
  for(int y=std::max(4,cy*GRID);y<std::min(height-4,(cy+1)*GRID);y++)for(int x=std::max(16,cx*GRID);x<std::min(width-16,(cx+1)*GRID);x++){int d=sq(x-cx*GRID-GRID/2)+sq(y-cy*GRID-GRID/2);if(d<best&&!s.map->isWall(x,y)){best=d;n.x=short(x);n.y=short(y);}}
 }
 const int dx[]={1,-1,0,0},dy[]={0,0,1,-1};
 for(int y=0;y<rows;y++)for(int x=0;x<cols;x++){auto& n=nodes[y*cols+x];if(n.x<0)continue;for(int d=0;d<4;d++){int xx=x+dx[d],yy=y+dy[d];if(xx<0||xx>=cols||yy<0||yy>=rows)continue;const auto& z=nodes[yy*cols+xx];if(z.x>=0&&minimap_sight::clear(s,n.x,n.y,z.x,z.y))n.edges|=1u<<d;}}
 revision=s.map->revision();builtAt=s.items->elapsed;
}
int LocalTactics::nodeAt(const State& s,int x,int y)const {
 int best=-1,dist=INF;
 for(int cy=std::max(0,y/GRID-2);cy<std::min(rows,y/GRID+3);cy++)for(int cx=std::max(0,x/GRID-2);cx<std::min(cols,x/GRID+3);cx++){
  int i=cy*cols+cx;const auto& n=nodes[i];int d=sq(n.x-x)+sq(n.y-y);if(n.x>=0&&d<dist&&clear(s,x,y,n.x,n.y)){best=i;dist=d;}}
 return best;
}
void LocalTactics::observe(const State& s,int team){
 const int size=bcols*brows;known.assign(size*6,0);friendly.assign(size,0);hostile.assign(size,0);visibleTotals.fill(0);
 for(int t=0;t<6;t++)allies[t]=s.allied(team,t);
 for(int i=0;i<size;i++)for(int t=0;t<6;t++){const auto& e=spots[t*size+i];if(!e.count||s.items->concealedFrom(s,team,t))continue;bool visible=allies[t];int cx=i%bcols,cy=i/bcols;
  for(int yy=std::max(0,cy-5);yy<std::min(brows,cy+6)&&!visible;yy++)for(int xx=std::max(0,cx-5);xx<std::min(bcols,cx+6)&&!visible;xx++)for(int a=0;a<6&&!visible;a++)if(allies[a]){
   for(auto p:spots[a*size+yy*bcols+xx].scout)if(p[0]>=0&&sq(p[0]-e.x)+sq(p[1]-e.y)<=144*144&&minimap_sight::clear(s,p[0],p[1],e.x,e.y)){visible=true;break;}
  }
  if(!visible)continue;known[t*size+i]=1;visibleTotals[t]+=e.count;
  float force=e.count*.5f+e.health/20.f;
  if(s.items->active(t,Items::FREEZE))force*=.2f;
  if(s.items->active(t,Items::INVINCIBLE))force*=4.f;
  (allies[t]?friendly:hostile)[i]+=force;
 }
}
float LocalTactics::power(const std::vector<float>& field,int x,int y,int radius)const {
 float sum=0;int cx=x/BIN,cy=y/BIN;
 for(int yy=std::max(0,cy-radius);yy<std::min(brows,cy+radius+1);yy++)for(int xx=std::max(0,cx-radius);xx<std::min(bcols,cx+radius+1);xx++)sum+=field[yy*bcols+xx];return sum;
}
void LocalTactics::search(const State& s,int start,int team,bool brave){
 std::fill(cost.begin(),cost.end(),INF);std::fill(parent.begin(),parent.end(),-1);cost[start]=0;
 // Dial queue: integer edge weights10..45, same shortest-path cost without a
 // heap allocation/comparison for every cell. Terrain edges were already checked.
 (void)s;std::array<std::vector<int>,46> open;std::array<size_t,46> read{};open[0].push_back(start);int pending=1,current=0;const int delta[]={1,-1,cols,-cols};
 float scale=std::max(80,totals[team]);std::vector<int> risk(bcols*brows,0);for(size_t b=0;b<risk.size();b++)if(!brave)risk[b]=int(std::min(35.f,std::max(0.f,hostile[b]-friendly[b]*.7f)*30.f/scale));
 while(pending){int bucket=current%46;if(read[bucket]>=open[bucket].size()){open[bucket].clear();read[bucket]=0;current++;continue;}int at=open[bucket][read[bucket]++];pending--;if(current!=cost[at])continue;
  for(int d=0;d<4;d++)if(traversal[at]&(1u<<d)){int to=at+delta[d];const auto& z=nodes[to];
   int b=(z.y/BIN)*bcols+z.x/BIN;int next=current+10+risk[b];
   if(next<cost[to]){cost[to]=next;parent[to]=at;open[next%46].push_back(to);pending++;}
  }
 }
}
float LocalTactics::itemValue(const State& s,int team,int slot,float own,float danger)const {
 const auto& p=s.items->pickups[slot];auto& it=*s.items;int k=p.kind;if(k<0)return -10000;
 const float base[]={145,95,250,85,0,75,230,255,100,45,35,50,40,25,35,45,65,35,90,40};float v=base[k];
 const int counter[]={9,10,11,12,13,14,15,16,17,-1,-1,-1,-1,-1,-1,-1,-1,-1,19,-1};
 if(counter[k]>=0&&it.opposed(s,team,counter[k]))v=8;
 if(it.active(team,k)&&it.until[team][k]-it.elapsed>8000)v*=.15f;
 if(k==Items::RESET)v=totals[team]<s.dotsPerTeam*.55f?220.f:-500.f;
 if(k==Items::THAW&&it.active(team,Items::FREEZE))v=400;
 if(k==Items::SHATTER||k==Items::REVEAL||k==Items::PHASENAIL||k==Items::WARD||k==Items::LIBERTY){
  int effect=k==Items::SHATTER?6:k==Items::REVEAL?18:k==Items::PHASENAIL?5:k==Items::WARD?7:8;
  for(int t=0;t<6;t++)if(!allies[t]&&visibleTotals[t]&&(it.active(t,effect)||it.pending[t]==effect))v+=130;
 }
 // Known pickups are on every player's minimap. Acquiring the counter before a
 // rival reaches its paired power is useful even before that power is active.
 for(const auto& q:it.pickups)if(q.kind>=0&&counter[q.kind]==k&&power(hostile,q.x,q.y)>0)v+=75;
 if((k==Items::TELEPORT||k==Items::DEATH||k==Items::FRIEND)&&it.pending[team])v*=.25f;
 if(danger>own*1.2f&&(k==Items::INVINCIBLE||k==Items::FREEZE||k==Items::FRIEND||k==Items::TELEPORT))v+=70;
 if(k==Items::DOUBLE&&s.dots.size()>=Items::MAX_PARTICLES)v=0;
 return v+35.f*(1.f-p.hp/float(Items::PICKUP_HP));
}
bool LocalTactics::landing(const State& s,int team,int x,int y,int& tx,int& ty)const {
 // A bounded placement check mirrors the rule's reachable-floor requirement,
 // without moving a particle or consuming the token on a known invalid spot.
 auto occupiedKnown=[&](int x2,int y2){const Dot* dot=s.field[x2][y2];if(!dot||dot->team==team)return false;int bucket=(y2/BIN)*bcols+x2/BIN;return known[dot->team*bcols*brows+bucket]!=0;};
 // Unknown occupancy is resolved by the ordinary item command. It may reject
 // the landing without consuming the item; never scout hidden enemies here.
 for(int radius=16;radius<=64;radius+=16)for(int k=0;k<8;k++){
  int xx=x+int(std::cos(k*3.14159265/4)*radius),yy=y+int(std::sin(k*3.14159265/4)*radius);
  if(xx<20||yy<8||xx>=width-20||yy>=height-8||s.map->isWall(xx,yy)||s.items->blockedByPickup(xx,yy))continue;
  if(power(hostile,xx,yy)>std::max(50,totals[team])*1.1f)continue;
  if(occupiedKnown(xx,yy))continue;
  std::vector<unsigned char> seen(width*height,0);std::vector<int> q;q.reserve(totals[team]+128);q.push_back(yy*width+xx);seen[q[0]]=1;int count=0;
  for(size_t at=0;at<q.size()&&count<totals[team];at++){int p=q[at];count++;for(int d:{-1,1,-width,width}){int v=p+d,vx=v%width,vy=v/width;if(vx<16||vx>=width-16||vy<4||vy>=height-4||seen[v]||s.map->isWall(vx,vy)||s.items->blockedByPickup(vx,vy)||occupiedKnown(vx,vy))continue;seen[v]=1;q.push_back(v);}}
  if(count>=totals[team]){tx=xx;ty=yy;return true;}
 }
 return false;
}
std::array<short,60> LocalTactics::plan(const State& s,unsigned mask,int level){
 std::array<short,60> out;out.fill(-1);if(!(mask&63))return out;
 const int now=s.items->elapsed;
 if(epoch!=s.items->epoch||width!=s.map->width||height!=s.map->height){reset();epoch=s.items->epoch;build(s);}
 else if(revision!=s.map->revision()&&now-builtAt>=420)build(s);
 traversal.resize(nodes.size());for(size_t n=0;n<nodes.size();n++)traversal[n]=nodes[n].edges;
 const int delta[]={1,-1,cols,-cols};
 for(const auto& p:s.items->pickups)if(p.kind>=0){int r=Items::PICKUP_RADIUS+GRID;
  for(int y=std::max(0,(p.y-r)/GRID);y<std::min(rows,(p.y+r)/GRID+1);y++)for(int x=std::max(0,(p.x-r)/GRID);x<std::min(cols,(p.x+r)/GRID+1);x++){int at=y*cols+x;const auto& a=nodes[at];for(int d=0;d<4;d++)if(traversal[at]&(1u<<d)){const auto& b=nodes[at+delta[d]];if(!clear(s,a.x,a.y,b.x,b.y,-1,true))traversal[at]&=~(1u<<d);}}
 }
 const int size=bcols*brows;spots.assign(size*6,Spot{});totals.fill(0);
 for(const Dot* d:s.dots){int t=d->team;if(t>=6)continue;int i=(d->y/BIN)*bcols+d->x/BIN;auto& b=spots[t*size+i];b.count++;b.health+=d->health;totals[t]++;if(b.x<0){b.x=d->x;b.y=d->y;}int corner=(d->x%BIN>=16?1:0)+(d->y%BIN>=16?2:0);if(b.scout[corner][0]<0)b.scout[corner]={{short(d->x),short(d->y)}};}
 for(int team=0;team<6;team++)if(mask&(1u<<team)){
  auto& memory=decisions[team];if(!totals[team]){memory=Decision{};continue;}observe(s,team);
  int mass=-1,bestMass=-1;
  for(int b=0;b<size;b++)if(spots[team*size+b].count){int sum=0,cx=b%bcols,cy=b/bcols;for(int yy=std::max(0,cy-1);yy<std::min(brows,cy+2);yy++)for(int xx=std::max(0,cx-1);xx<std::min(bcols,cx+2);xx++)sum+=spots[team*size+yy*bcols+xx].count;if(sum>bestMass){mass=b;bestMass=sum;}}
  const auto& main=spots[team*size+mass];int ox=main.x,oy=main.y;
  // Choose an actual friendly cell near the local mass centroid, not a point in a wall.
  long sx=0,sy=0;int near=0;for(const Dot* d:s.dots)if(d->team==team&&sq(int(d->x)-ox)+sq(int(d->y)-oy)<96*96){sx+=d->x;sy+=d->y;near++;}
  if(near){int cx=int(sx/near),cy=int(sy/near),best=INF;for(const Dot* d:s.dots)if(d->team==team){int q=sq(int(d->x)-cx)+sq(int(d->y)-cy);if(q<best){best=q;ox=d->x;oy=d->y;}}}
  int start=nodeAt(s,ox,oy);if(start<0){out[team*10]=short(ox);out[team*10+5]=short(oy);continue;}
  if(length(ox,oy,memory.px,memory.py)>160){memory=Decision{};memory.px=ox;memory.py=oy;memory.progressAt=now;}
  if(length(ox,oy,memory.px,memory.py)>10){memory.px=ox;memory.py=oy;memory.progressAt=now;memory.stuck=0;}
  else if(now-memory.progressAt>2100){memory.stuck++;memory.progressAt=now;}
  for(int b=0;b<size;b++)if(spots[team*size+b].count)visits[team*size+b]=now;
  float own=std::max(1.f,power(friendly,ox,oy)),threat=power(hostile,ox,oy);bool brave=s.items->active(team,Items::INVINCIBLE);
  search(s,start,team,brave);
  struct Candidate{int action,enemy,slot,x,y,node;float score;};Candidate chosen{EXPLORE,-1,-1,ox,oy,start,-100000};
  auto offer=[&](Candidate c){if(c.node<0||cost[c.node]==INF)return;c.score-=cost[c.node]*.08f;
   if(c.action==memory.action&&c.enemy==memory.enemy&&c.slot==memory.pickup&&length(c.x,c.y,memory.x,memory.y)<64&&memory.stuck==0)c.score+=24;
   if(c.action!=memory.action&&now-memory.since<840&&c.action!=RETREAT)c.score-=35;
   if(c.score>chosen.score)chosen=c;};
  int nearestEnemy=INF;
  for(int t=0;t<6;t++)if(!allies[t])for(int b=0;b<size;b++)if(known[t*size+b]){
   const auto& enemy=spots[t*size+b];int n=nodeAt(s,enemy.x,enemy.y);if(n<0||cost[n]==INF)continue;
   nearestEnemy=std::min(nearestEnemy,cost[n]);float foes=std::max(1.f,power(hostile,enemy.x,enemy.y));float ratio=own/foes;
   float value=105+std::min(30.f,ratio*15)+std::sqrt(float(enemy.count))*2;
   if(ratio<.8f&&!brave)value-=70*(.8f-ratio);if(s.items->active(t,Items::INVINCIBLE)&&!brave)value-=150;
   if(s.items->active(t,Items::FREEZE))value+=50;
   offer({ATTACK,t,-1,enemy.x,enemy.y,n,value});
  }
  for(int k=0;k<6;k++)if(s.items->pickups[k].kind>=0){const auto& p=s.items->pickups[k];int best=-1,travel=INF,r=Items::radiusForHp(p.hp)+3;
   for(int k2=0;k2<8;k2++){int x=p.x+int(std::cos(k2*3.14159265/4)*r),y=p.y+int(std::sin(k2*3.14159265/4)*r);int n=nodeAt(s,x,y);if(n>=0&&cost[n]<travel&&clear(s,nodes[n].x,nodes[n].y,p.x,p.y,k)){best=n;travel=cost[n];}}
   float danger=power(hostile,p.x,p.y);float value=itemValue(s,team,k,own,threat);if(danger>own&&!brave)value-=std::min(160.f,(danger-own)*.2f);offer({PICKUP,-1,k,p.x,p.y,best,value});
  }
  if(s.operations->mode)for(size_t k=0;k<s.operations->sites.size();k++){const auto& site=s.operations->sites[k];bool friendly=site.owner>=0&&s.alliances[site.owner]==s.alliances[team];float priority=friendly?(site.contested||site.claim>=0?210.f:0.f):200.f;float danger=power(hostile,site.x,site.y);if(danger>own*1.25f&&!brave)priority-=110;offer({EXPLORE,-1,-1,site.x,site.y,nodeAt(s,site.x,site.y),priority});}
  // Retreat is a short reachable escape from an unfavourable local fight, never
  // an endless flight from globally stronger armies on the other side of a wall.
  bool retreat=level>=2&&!brave&&nearestEnemy<150&&(threat>own*1.35f||(memory.action==RETREAT&&now-memory.since<1120&&threat>own*.85f));
  if(retreat){for(int n=0;n<int(nodes.size());n++)if(cost[n]>=35&&cost[n]<=150){const auto& p=nodes[n];float danger=power(hostile,p.x,p.y,1),support=power(friendly,p.x,p.y,1);float value=160-std::min(130.f,danger*.3f)+std::min(35.f,support*.04f);if(danger<threat*.7f)offer({RETREAT,-1,-1,p.x,p.y,n,value});}}
  if(level>=2&&bestMass<totals[team]*.5f&&nearestEnemy>100)offer({REGROUP,-1,-1,ox,oy,start,120});
  if(level>=2)for(int t=0;t<6;t++)if(t!=team&&allies[t])for(int b=0;b<size;b++)if(spots[t*size+b].count>10){auto a=spots[t*size+b];float danger=power(hostile,a.x,a.y);if(danger>20&&danger<own+power(friendly,a.x,a.y))offer({SUPPORT,-1,-1,a.x,a.y,nodeAt(s,a.x,a.y),120});}
  // Explore unseen ground using reachable, deterministic frontier choices. No
  // hidden enemy coordinate is used, even when all opponents are cloaked.
  for(int by=0;by<brows;by++)for(int bx=0;bx<bcols;bx++){int b=by*bcols+bx,n=(by*4+2)*cols+bx*4+2;if(n>=int(nodes.size())||nodes[n].x<0||cost[n]<70||cost[n]>420)continue;
   float age=std::min(1.f,(now-visits[team*size+b])/15000.f);unsigned tie=(unsigned(b)*2654435761u+unsigned(team+1)*2246822519u+unsigned(s.map->id)*3266489917u);
   offer({EXPLORE,-1,-1,nodes[n].x,nodes[n].y,n,60+age*20+(tie%29)*.5f});
  }
  if(memory.stuck>=2&&chosen.action!=PICKUP&&chosen.action!=ATTACK){memory=Decision{};}
  if(memory.action!=chosen.action||memory.enemy!=chosen.enemy||memory.pickup!=chosen.slot)memory.since=now;
  memory.action=chosen.action;memory.enemy=chosen.enemy;memory.pickup=chosen.slot;memory.x=chosen.x;memory.y=chosen.y;
  int tx=chosen.x,ty=chosen.y;bool phase=s.items->active(team,Items::PHASE)&&s.items->until[team][Items::PHASE]-now>5000&&length(ox,oy,tx,ty)<180;
  if(!clear(s,ox,oy,tx,ty,chosen.slot,phase)){int n=chosen.node;while(n>=0&&(!clear(s,ox,oy,nodes[n].x,nodes[n].y)||(memory.stuck>0&&cost[n]>40)))n=parent[n];if(n>=0){tx=nodes[n].x;ty=nodes[n].y;}else{tx=ox;ty=oy;}}
  out[team*10]=short(tx);out[team*10+5]=short(ty);
  // Close combat keeps a compact advancing front. Scatter targets are omitted
  // during regroup/retreat so the army does not keep fighting at an obsolete point.
  if(chosen.action==ATTACK&&level>=2&&length(ox,oy,tx,ty)<64&&clear(s,ox,oy,tx,ty)){
   float d=std::max(1.f,length(ox,oy,tx,ty)),spread=std::min(12.f,std::sqrt(float(totals[team]))*.3f);
   for(int side=0;side<2;side++){float sign=side?1.f:-1.f;int x=int(tx-sign*(ty-oy)/d*spread),y=int(ty+sign*(tx-ox)/d*spread);if(clear(s,ox,oy,x,y)){out[team*10+side+1]=short(x);out[team*10+side+6]=short(y);}}
  }
  int kind=s.items->pending[team],target=-1;float value=-1;
  if(!s.items->active(team,Items::FREEZE)&&s.items->warmup==0){
   if(kind==Items::DEATH||kind==Items::FRIEND){for(int t=0;t<6;t++)if(!allies[t]&&visibleTotals[t]){
    if(kind==Items::DEATH&&(s.items->active(t,Items::INVINCIBLE)||s.items->active(t,Items::WARD)))continue;
    if(kind==Items::FRIEND&&(s.items->protectedBy(s,t,Items::LIBERTY)||s.items->truceUntil>now))continue;
    float v=visibleTotals[t]+(t==chosen.enemy?100:0);if(v>value){value=v;target=t;}}
    if(target>=0){out[team*10+3]=short(target);out[team*10+8]=0;out[team*10+4]=-3;out[team*10+9]=short(s.items->nonce[team]);}
   }else if(kind==Items::TELEPORT&&!s.items->opposed(s,team,Items::ANCHOR)&&(cost[chosen.node]>180||chosen.action==RETREAT)){
    int x,y;if(landing(s,team,chosen.x,chosen.y,x,y)){out[team*10+3]=short(x);out[team*10+8]=short(y);out[team*10+4]=-3;out[team*10+9]=short(s.items->nonce[team]);}
   }
  }
 }
 return out;
}
