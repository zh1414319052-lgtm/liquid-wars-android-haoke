// SPDX-License-Identifier: GPL-3.0-or-later
#include "operations.hpp"
#include "state.hpp"
#include <algorithm>
#include <climits>
#include <cmath>
#include <queue>
namespace {int sq(int x){return x*x;}constexpr int INF=INT_MAX/4;}
Operations::Operations(State& s){epoch=s.items->epoch;generation.fill(epoch*256);}
void Operations::reset(State& s){int saved=mode;*this=Operations(s);mode=saved;for(auto d:s.dots)d->squad=0;if(mode){build(s);locateSites(s);}stats(s);}
void Operations::configure(State& s,int m){mode=m==1?1:0;reset(s);}
bool Operations::clear(const State& s,int x,int y,int tx,int ty)const{
 int n=std::max(std::abs(tx-x),std::abs(ty-y));int px=x,py=y;
 for(int i=0;i<=n;i++){int xx=n?x+(tx-x)*i/n:x,yy=n?y+(ty-y)*i/n:y;
  if(s.map->isWall(xx,yy)||s.items->blockedByPickup(xx,yy))return false;
  if(xx!=px&&yy!=py&&(s.map->isWall(xx,py)||s.map->isWall(px,yy)))return false;px=xx;py=yy;
 }return true;
}
void Operations::build(State& s){
 cols=(s.map->width+GRID-1)/GRID;rows=(s.map->height+GRID-1)/GRID;nodes.assign(cols*rows,Node{});
 for(int cy=0;cy<rows;cy++)for(int cx=0;cx<cols;cx++){
  auto& n=nodes[cy*cols+cx];int best=INF;
  for(int dy=0;dy<GRID;dy++)for(int dx=0;dx<GRID;dx++){int x=cx*GRID+dx,y=cy*GRID+dy;if(s.map->isWall(x,y)||s.items->blockedByPickup(x,y))continue;
   int score=sq(dx-GRID/2)+sq(dy-GRID/2);if(score<best){best=score;n.x=short(x);n.y=short(y);}}
 }
 const int delta[]={1,-1,cols,-cols};
 for(int i=0;i<int(nodes.size());i++){auto& a=nodes[i];if(a.x<0)continue;for(int k=0;k<4;k++){int j=i+delta[k];if(j<0||j>=int(nodes.size())||(k<2&&j/cols!=i/cols)||nodes[j].x<0)continue;auto b=nodes[j];if(clear(s,a.x,a.y,b.x,b.y))a.edges|=1u<<k;}}
 int component=0;std::vector<int> q;q.reserve(nodes.size());
 for(int i=0;i<int(nodes.size());i++)if(nodes[i].x>=0&&nodes[i].component<0){q.clear();q.push_back(i);nodes[i].component=component;for(size_t a=0;a<q.size();a++)for(int k=0;k<4;k++)if(nodes[q[a]].edges&(1u<<k)){int j=q[a]+delta[k];if(nodes[j].component<0){nodes[j].component=component;q.push_back(j);}}component++;}
 navHash=1469598103934665603ull;for(auto n:nodes)for(int v:{int(n.x),int(n.y),int(n.edges),n.component}){navHash^=uint32_t(v);navHash*=1099511628211ull;}
 terrain=s.map->revision();wave=s.items->wave;builtAt=clock;navVersion++;
}
int Operations::nodeAt(const State& s,int x,int y)const{
 if(nodes.empty())return -1;int cx=x/GRID,cy=y/GRID,best=-1,score=INF;
 for(int yy=std::max(0,cy-2);yy<std::min(rows,cy+3);yy++)for(int xx=std::max(0,cx-2);xx<std::min(cols,cx+3);xx++){int i=yy*cols+xx;auto n=nodes[i];if(n.x<0)continue;int d=sq(n.x-x)+sq(n.y-y);if(d<score&&clear(s,x,y,n.x,n.y)){score=d;best=i;}}
 return best;
}
void Operations::flow(State& s,Route& r){
 r.version=navVersion;r.distance.assign(nodes.size(),INF);r.goal=-1;r.flowHash=0;
 if(r.points.empty()||r.at>=int(r.points.size()))return;auto p=r.points[r.at];int goal=nodeAt(s,p.x,p.y);if(goal<0){r.status=BLOCKED;return;}
 r.goal=goal;std::vector<int> q;q.reserve(nodes.size());q.push_back(goal);r.distance[goal]=0;const int delta[]={1,-1,cols,-cols};
 for(size_t at=0;at<q.size();at++){int n=q[at];for(int k=0;k<4;k++)if(nodes[n].edges&(1u<<k)){int j=n+delta[k];if(r.distance[j]==INF){r.distance[j]=r.distance[n]+GRID;q.push_back(j);}}}
 r.flowHash=1469598103934665603ull;for(int v:r.distance){r.flowHash^=uint32_t(v);r.flowHash*=1099511628211ull;}r.status=MOVING;
}
void Operations::direct(int t){if(t<0||t>=6)return;for(auto& r:routes[t])r=Route{};staged[t].clear();stagingGroup[t]=-1;feedback[t]=0;}
void Operations::clearTeam(State& s,int t){direct(t);generation[t]++;split[t]=false;for(auto d:s.dots)if(d->team==t)d->squad=0;}
bool Operations::command(State& s,int t,const short* x,const short* y){
 if(x[0]!=-5)return false;if(t<0||t>=6)return true;
 if(y[1]!=generation[t]||x[1]<=0||x[1]==lastPacket[t])return true;lastPacket[t]=x[1];
 if(s.players[t].score<=0)return true;int op=y[0]/4,g=y[0]%4;if(op<BEGIN||op>FOLLOW||g<0)return true;
 // Direct guiding uses the same movement/contact rules as fingers, with squad ownership.
 // It is atomic and cheap: never rebuild a navigation field for every touch move.
 if(op==FOLLOW){
  if(x[2]<0||y[2]<0||x[2]>=s.map->width||y[2]>=s.map->height){feedback[t]=INVALID;return true;}
  staged[t].clear();stagingGroup[t]=-1;feedback[t]=0;
  for(int k=0;k<GROUPS;k++)if(g==3||g==k){auto& r=routes[t][k];if(!r.tracking)r=Route{};r.enabled=r.tracking=true;r.hold=false;r.status=MOVING;r.points.assign(1,Point{int(x[2]),int(y[2])});}return true;
 }
 if(nodes.empty())build(s);stats(s);feedback[t]=0;
 if(op==SPLIT){int n=0;direct(t);split[t]=true;for(auto d:s.dots)if(d->team==t){int k=n++%4;d->squad=k<2?0:k-1;}for(auto& r:routes[t]){r.enabled=true;r.hold=true;}stats(s);return true;}
 if(op==MERGE){int tx=groups[t][0].x,ty=groups[t][0].y;if(!groups[t][0].count){for(auto d:s.dots)if(d->team==t){tx=d->x;ty=d->y;break;}}int token=generation[t];clearTeam(s,t);generation[t]=token;auto& r=routes[t][0];r.enabled=true;r.points.push_back({tx,ty});flow(s,r);return true;}
 if(op==STOP){staged[t].clear();stagingGroup[t]=-1;for(int k=0;k<GROUPS;k++)if(g==3||k==g){routes[t][k]=Route{};routes[t][k].enabled=routes[t][k].hold=true;}return true;}
 if(op==BEGIN){staged[t].clear();stagingGroup[t]=g;}
 if(stagingGroup[t]!=g){feedback[t]=INVALID;return true;}
 if(op==BEGIN||op==APPEND){for(int i=2;i<5;i++)if(x[i]>=0&&y[i]>=0){if(staged[t].size()>=MAX_POINTS||s.map->isWall(x[i],y[i])){staged[t].clear();stagingGroup[t]=-1;feedback[t]=INVALID;return true;}staged[t].push_back({x[i],y[i]});}return true;}
 if(op==COMMIT){
  if(staged[t].empty()){feedback[t]=INVALID;stagingGroup[t]=-1;return true;}
  // Validate every leg before replacing any live order. Failed edits are atomic.
  int component=-1;bool valid=true;
  for(auto p:staged[t]){int n=nodeAt(s,p.x,p.y);if(n<0||(component>=0&&component!=nodes[n].component)){valid=false;break;}component=nodes[n].component;}
  for(int k=0;k<GROUPS&&valid;k++)if((g==3||g==k)&&groups[t][k].count){int n=nodeAt(s,groups[t][k].x,groups[t][k].y);if(n<0||nodes[n].component!=component)valid=false;}
  if(valid){for(int k=0;k<GROUPS;k++)if(g==3||g==k){auto& r=routes[t][k];r=Route{};r.enabled=true;r.points=staged[t];flow(s,r);}}else feedback[t]=BLOCKED;
  staged[t].clear();stagingGroup[t]=-1;
 }return true;
}
void Operations::stats(State& s){
 for(auto& row:groups)for(auto& g:row)g=Group{};
 for(auto d:s.dots){int k=split[d->team]?std::min(2,int(d->squad)):0;auto& g=groups[d->team][k];g.count++;g.x+=d->x;g.y+=d->y;}
 for(auto& row:groups)for(auto& g:row)if(g.count){g.x/=g.count;g.y/=g.count;}
 // Representatives are actual floor occupants, never an average inside a wall.
 std::array<std::array<int,3>,6> best;for(auto& row:best)row.fill(INF);
 auto centers=groups;for(auto d:s.dots){int k=split[d->team]?std::min(2,int(d->squad)):0;auto c=centers[d->team][k];int v=sq(d->x-c.x)+sq(d->y-c.y);if(v<best[d->team][k]){best[d->team][k]=v;groups[d->team][k].x=d->x;groups[d->team][k].y=d->y;}}
}
bool Operations::target(const State& s,const Dot& d,int& x,int& y)const{
 int k=split[d.team]?std::min(2,int(d.squad)):0;const auto& r=routes[d.team][k];if(!r.enabled)return false;
 x=y=-1;if(r.hold||r.points.empty())return true;
 if(r.tracking){x=r.points[0].x;y=r.points[0].y;return true;}
 if(r.goal<0)return true;
 auto end=r.points[r.at];if(std::abs(int(d.x)-end.x)<GRID*2&&std::abs(int(d.y)-end.y)<GRID*2&&clear(s,d.x,d.y,end.x,end.y)){x=end.x;y=end.y;return true;}
 int n=(int(d.y)/GRID)*cols+int(d.x)/GRID;if(n<0||n>=int(nodes.size())||r.distance.size()!=nodes.size())return true;
 // At most five local candidates; no particle performs an individual search.
 const int delta[]={0,1,-1,cols,-cols};int best=-1,value=INF;
 for(int a=0;a<5;a++){int j=n+delta[a];if(j<0||j>=int(nodes.size())||((a==1||a==2)&&j/cols!=n/cols))continue;const auto& p=nodes[j];if(p.x<0||r.distance[j]==INF)continue;int cost=r.distance[j]*4+std::abs(int(d.x)-p.x)+std::abs(int(d.y)-p.y);if(cost<value&&clear(s,d.x,d.y,p.x,p.y)){best=j;value=cost;}}
 if(best>=0){x=nodes[best].x;y=nodes[best].y;}return true;
}
void Operations::locateSites(State& s){
 sites.clear();if(nodes.empty())return;int count=0;for(const auto& n:nodes)count=std::max(count,n.component+1);std::vector<int> sizes(count);for(auto n:nodes)if(n.component>=0)sizes[n.component]++;if(sizes.empty())return;int component=int(std::max_element(sizes.begin(),sizes.end())-sizes.begin());
 std::vector<Point> anchors=s.map->width>800?std::vector<Point>{{s.map->width/4,s.map->height/4},{s.map->width*3/4,s.map->height/4},{s.map->width/2,s.map->height/2},{s.map->width/4,s.map->height*3/4},{s.map->width*3/4,s.map->height*3/4}}:std::vector<Point>{{s.map->width/4,s.map->height/2},{s.map->width/2,s.map->height/2},{s.map->width*3/4,s.map->height/2}};
 for(auto a:anchors){int best=-1,value=INF;for(int i=0;i<int(nodes.size());i++){auto n=nodes[i];if(n.component!=component)continue;bool valid=true;for(auto p:sites)if(sq(p.x-n.x)+sq(p.y-n.y)<sq(RADIUS*3)){valid=false;break;}if(!valid)continue;
   for(int dx:{-8,0,8})for(int dy:{-8,0,8})if(s.map->isWall(n.x+dx,n.y+dy))valid=false;if(!valid)continue;
   int score=sq(n.x-a.x)+sq(n.y-a.y);for(int t=0;t<6;t++)if(sq(n.x-s.birthX[t])+sq(n.y-s.birthY[t])<sq(64))score+=50000;if(score<value){value=score;best=i;}}
  if(best>=0){Site p;p.x=nodes[best].x;p.y=nodes[best].y;sites.push_back(p);}}
}
int Operations::spawn(State& s,Site& p,int amount){
 int team=p.owner;if(team<0||s.players[team].score<=0||s.items->opposed(s,team,Items::ANTIGROW))return 0;
 amount=std::min({amount,Items::MAX_PARTICLES-int(s.dots.size()),std::max(0,s.dotsPerTeam*2-s.players[team].score)});if(amount<=0)return 0;
 const int R=30,W=R*2+1;std::array<unsigned char,W*W> seen{};std::vector<Point> q;q.push_back({p.x,p.y});seen[R*W+R]=1;int born=0;
 for(size_t at=0;at<q.size()&&born<amount;at++){auto c=q[at];auto d=s.field[c.x][c.y];if(d&&!s.allied(d->team,team))continue;if(s.map->isWall(c.x,c.y)||s.items->blockedByPickup(c.x,c.y))continue;
  if(!d){auto dot=new Dot(c.x,c.y,team);s.field[c.x][c.y]=dot;s.dots.push_back(dot);s.players[team].score++;born++;}
  for(auto v:std::array<Point,4>{{{1,0},{-1,0},{0,1},{0,-1}}}){int x=c.x+v.x,y=c.y+v.y,lx=x-p.x+R,ly=y-p.y+R;if(lx<0||ly<0||lx>=W||ly>=W||seen[ly*W+lx]||s.map->isWall(x,y))continue;seen[ly*W+lx]=1;q.push_back({x,y});}}
 if(born){p.born+=born;p.pulseAt=clock;s.points.resize(s.dots.size()*3);s.colours.resize(s.dots.size()*4,1);}return born;
}
void Operations::economy(State& s){
 int need=std::max(6,std::min(24,s.dotsPerTeam/40));
 for(auto& p:sites){std::array<int,6> presence{};for(auto d:s.dots)if(sq(int(d->x)-p.x)+sq(int(d->y)-p.y)<=RADIUS*RADIUS&&!s.items->active(d->team,Items::FREEZE)&&clear(s,d->x,d->y,p.x,p.y))presence[d->team]++;
  if(p.owner>=0&&s.players[p.owner].score==0){int successor=-1;for(int t=0;t<6;t++)if(s.alliances[t]==s.alliances[p.owner]&&s.players[t].score>0){successor=t;break;}p.owner=successor;p.claim=-1;p.progress=0;}
  int claimant=-1,largest=0;for(int t=0;t<6;t++)if(presence[t]>largest){largest=presence[t];claimant=t;}
  int friendly=0,hostile=0;if(claimant>=0)for(int t=0;t<6;t++){if(s.allied(t,claimant))friendly+=presence[t];else hostile+=presence[t];}
  p.contested=hostile>=3;
  if(claimant>=0&&friendly>=need&&!p.contested){
   if(p.owner>=0&&s.alliances[p.owner]==s.alliances[claimant]){p.claim=-1;p.progress=0;}
   else{if(p.claim<0||s.alliances[p.claim]!=s.alliances[claimant]){p.claim=claimant;p.progress=0;}p.progress+=70;if(p.progress>=4000){if(p.owner>=0){p.owner=-1;p.progress=0;}else{p.owner=p.claim;p.claim=-1;p.progress=0;p.spawnAt=p.scoreAt=clock;}revision++;}}
  }else if(!p.contested){p.progress=std::max(0,p.progress-70);if(!p.progress)p.claim=-1;}
  bool raided=p.claim>=0||p.contested;
  if(p.owner>=0&&!raided){if(clock-p.scoreAt>=1000){points[p.owner]++;p.scoreAt=clock;}if(clock-p.spawnAt>=2000){spawn(s,p,std::max(3,std::min(20,s.dotsPerTeam/100)));p.spawnAt=clock;}}
  else {p.spawnAt=p.scoreAt=clock;}
 }
}
void Operations::step(State& s){
 if(s.items->warmup>0)return;clock+=7;bool active=mode!=0;for(auto& row:routes)for(auto& r:row)active|=r.enabled;if(!active){if(clock%140==0)stats(s);return;}
 bool needsNav=mode!=0;for(auto& row:routes)for(auto& r:row)needsNav|=r.enabled&&!r.hold&&!r.tracking;
 if(needsNav&&(nodes.empty()||terrain!=s.map->revision()||wave!=s.items->wave||(s.items->hasPickups()&&clock-builtAt>=1000)))build(s);
 if(clock%70==0){stats(s);if(mode)economy(s);
  for(int t=0;t<6;t++){if(s.players[t].score<=0){direct(t);continue;}for(int k=0;k<GROUPS;k++){auto& r=routes[t][k];auto g=groups[t][k];if(!r.enabled||r.hold||r.points.empty()||!g.count)continue;if(!r.tracking&&r.version!=navVersion)flow(s,r);auto p=r.points[r.at];int near=0;int radius=std::max(18,std::min(55,int(std::sqrt(float(g.count)))*2));for(auto d:s.dots)if(d->team==t&&(split[t]?std::min(2,int(d->squad)):0)==k&&sq(int(d->x)-p.x)+sq(int(d->y)-p.y)<radius*radius)near++;
    if(near*100>=g.count*60){if(r.at+1<int(r.points.size())){r.at++;flow(s,r);}else r.status=ARRIVED;}else if(r.tracking)r.status=MOVING;else {int n=nodeAt(s,g.x,g.y);if(n<0||r.goal<0||r.distance[n]==INF)r.status=BLOCKED;else r.status=MOVING;}
  }}
 }
}
int Operations::alliancePoints(const State& s,int t)const{int score=0;for(int i=0;i<6;i++)if(s.alliances[i]==s.alliances[t])score+=points[i];return score;}
int Operations::winningTeam(const State& s)const{if(!mode)return -1;for(int t=0;t<6;t++)if(s.players[t].score>0&&alliancePoints(s,t)>=WIN_SCORE)return t;return -1;}
std::vector<int> Operations::snapshot(const State& s,int viewer,bool all)const{
 std::vector<int> out{0x4c574f50,mode,clock,s.items->epoch,WIN_SCORE,int(sites.size()),winningTeam(s)};for(int p:points)out.push_back(p);
 for(auto p:sites)for(int v:{p.x,p.y,p.owner,p.claim,p.progress,p.contested,p.born,p.pulseAt})out.push_back(v);
 // Orders and subgroup positions are private; observer may inspect everyone.
 for(int t=0;t<6;t++){bool visible=all||t==viewer;out.push_back(visible?int(split[t]):0);out.push_back(visible?feedback[t]:0);out.push_back(visible?lastPacket[t]:0);out.push_back(visible?generation[t]:0);for(int k=0;k<GROUPS;k++){auto g=groups[t][k];const auto& r=routes[t][k];for(int v:{visible?g.count:0,visible?g.x:-1,visible?g.y:-1,visible?int(r.enabled):0,visible?int(r.hold):0,visible?r.status:0,visible?r.at:0,visible?int(r.points.size()):0})out.push_back(v);if(visible)for(auto p:r.points){out.push_back(p.x);out.push_back(p.y);}}}return out;
}
uint64_t Operations::hash()const{
 uint64_t h=1469598103934665603ull;auto mix=[&](int v){h^=uint32_t(v);h*=1099511628211ull;};mix(mode);mix(clock);mix(epoch);mix(terrain);mix(wave);mix(builtAt);mix(navVersion);mix(revision);mix(int(navHash));mix(int(navHash>>32));
 for(auto p:sites)for(int v:{p.x,p.y,p.owner,p.claim,p.progress,p.contested,p.spawnAt,p.scoreAt,p.born,p.pulseAt})mix(v);
 for(int t=0;t<6;t++){for(int v:{int(split[t]),lastPacket[t],feedback[t],points[t],stagingGroup[t],generation[t]})mix(v);mix(int(staged[t].size()));for(auto p:staged[t]){mix(p.x);mix(p.y);}for(const auto& r:routes[t]){mix(int(r.flowHash));mix(int(r.flowHash>>32));for(int v:{int(r.enabled),int(r.hold),int(r.tracking),r.at,r.status,r.version,r.goal,int(r.points.size())})mix(v);for(auto p:r.points){mix(p.x);mix(p.y);}}}return h;
}
