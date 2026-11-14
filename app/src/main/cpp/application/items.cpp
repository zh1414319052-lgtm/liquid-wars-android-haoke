// SPDX-License-Identifier: GPL-3.0-or-later
#include "items.hpp"
#include "state.hpp"
#include "combat.hpp"
#include <algorithm>
#include <climits>
#include <cmath>
#include <cstring>
namespace {

int square(int x){return x*x;}
bool inside(const State& s,int x,int y){return x>=0&&x<s.map->width&&y>=0&&y<s.map->height;}
std::vector<int> area(State& s,int team,int need,int x=-1,int y=-1) {const int W=s.map->width,H=s.map->height;[[maybe_unused]] const int N=W*H;
    std::vector<unsigned char> seen(N);std::vector<int> queue,result;
    queue.reserve(N);result.reserve(need);
    auto add=[&](int xx,int yy){if(!inside(s,xx,yy))return;int p=yy*W+xx;
        if(seen[p]||s.map->isWall(xx,yy)||s.items->blockedByPickup(xx,yy))return;
        Dot* d=s.field[xx][yy];if(d&&d->team!=team)return;
        seen[p]=1;queue.push_back(p);};
    if(x>=0)add(x,y);else for(auto d:s.dots)if(d->team==team)add(d->x,d->y);
    for(size_t at=0;at<queue.size()&&int(result.size())<need;at++){
        int p=queue[at],xx=p%W,yy=p/W;
        if(!s.field[xx][yy])result.push_back(p);
        for(int dy=-1;dy<=1;dy++)for(int dx=-1;dx<=1;dx++){
            // No corner squeeze into a physically sealed diagonal gap.
            if(dx&&dy&&(s.map->isWall(xx+dx,yy)||s.map->isWall(xx,yy+dy)))continue;
            add(xx+dx,yy+dy);
        }
    }return result;
}

bool clear(State& s,int x,int y,int tx,int ty){int n=std::max(std::abs(tx-x),std::abs(ty-y));for(int i=0;i<=n;i++)if(s.map->isWall(n?x+(tx-x)*i/n:x,n?y+(ty-y)*i/n:y))return false;return true;}
}
Items::Items(int seed,int map):randomState(uint32_t(seed)^((uint32_t(map)+1)*0x85ebca6bu)^0x166a91d3u){deathOwner.fill(-1);if(!randomState)randomState=1;nextWave=8000+int(bounded(4001));}
uint32_t Items::next(){randomState^=randomState<<13;randomState^=randomState>>17;randomState^=randomState<<5;return randomState;}
unsigned Items::bounded(unsigned n){if(!n)return 0;unsigned low=unsigned(-n)%n,r;do{r=next();}while(r<low);return r%n;}
void Items::emit(int k,int t,int x,int y,int n,int target,int stage){last={last.serial+1,k,t,x,y,n,elapsed,target,stage};events.push_back(last);while(events.size()>32)events.pop_front();}
void Items::capture(State& s){if(captured)return;for(auto d:s.dots)boot.push_back(*d);for(int t=0;t<6;t++){bootPlayers[t]=s.players[t];for(int k=0;k<5;k++)bootPlayers[t].x[k]=bootPlayers[t].y[k]=-1;bootPlayers[t].x[0]=s.birthX[t];bootPlayers[t].y[0]=s.birthY[t];bootAlliances[t]=s.alliances[t];}bootMove=s.moveRandom->getSeed();bootAI=s.aiRandom->getSeed();bootItems=randomState;bootNext=nextWave;captured=true;}
void Items::rewind(State& s){
 capture(s);for(auto d:s.dots)delete d;s.dots.clear();s.field.clear();
 for(auto d:boot){auto p=new Dot(d);s.dots.push_back(p);s.field[p->x][p->y]=p;}
 for(int t=0;t<6;t++){s.players[t]=bootPlayers[t];s.alliances[t]=bootAlliances[t];until[t].fill(0);pending[t]=nonce[t]=attempt[t]=0;}
 s.moveRandom->setSeed(bootMove);s.aiRandom->setSeed(bootAI);s.map->restore();s.timeSidebar=0;
 next();nextWave=8000+int(bounded(4001));suppressResetWave=true;elapsed=wave=0;epoch++;warmup=3003;for(auto& p:pickups)p=Pickup{};
 truceA=truceB=-1;truceUntil=0;quarryBudget.fill(0);deathOwner.fill(-1);events.clear();s.points.resize(s.dots.size()*3,0);s.colours.resize(s.dots.size()*4,1);
 s.operations->reset(s);
}
void Items::spawnPair(State& s){
 const int W=s.map->width,H=s.map->height,STRIDE=W+1,R=PICKUP_RADIUS;std::vector<int> floor;floor.reserve(W*H);
 // Summed occupancy rejects complete footprints in O(1), never scanning an army per candidate.
 std::vector<int> occupied((W+1)*(H+1));
 for(int y=0;y<H;y++){int row=0;for(int x=0;x<W;x++){row+=s.map->isWall(x,y)||s.field[x][y];occupied[(y+1)*STRIDE+x+1]=occupied[y*STRIDE+x+1]+row;}}
 for(int y=R+1;y<H-R-1;y++)for(int x=std::max(16,R+1);x<W-std::max(16,R+1);x++){int l=x-R,t=y-R,r=x+R+1,b=y+R+1;if(occupied[b*STRIDE+r]-occupied[t*STRIDE+r]-occupied[b*STRIDE+l]+occupied[t*STRIDE+l]==0)floor.push_back(y*W+x);}
 for(auto& p:pickups)p=Pickup{};if(floor.empty())return;
 int wanted=1+int(bounded(MAX_PICKUPS));std::vector<int> positions;
 for(int k=0;k<wanted&&!floor.empty();k++){int p=floor[bounded(unsigned(floor.size()))];positions.push_back(p);floor.erase(std::remove_if(floor.begin(),floor.end(),[&](int q){return std::abs(q%W-p%W)<=2*R+1&&std::abs(q/W-p/W)<=2*R+1;}),floor.end());}
 int count=int(positions.size());std::vector<int> kinds,pairs;
 const int counter[10]={ANTIGROW,ROCKSEAL,THAW,ANCHOR,CAUSAL,PHASENAIL,SHATTER,WARD,LIBERTY,REVEAL};
 const int power[10]={DOUBLE,QUARRY,FREEZE,TELEPORT,RESET,PHASE,INVINCIBLE,DEATH,FRIEND,INVISIBLE};
 for(int k=0;k<10;k++)if(!previousKinds[power[k]]&&!previousKinds[counter[k]]&&(power[k]!=RESET||(!resetSpawned&&!suppressResetWave)))pairs.push_back(k);
 std::array<bool,COUNT> current{};
 for(int k=0;k<count/2;k++){unsigned at=bounded(unsigned(pairs.size()));int pair=pairs[at];pairs[at]=pairs.back();pairs.pop_back();kinds.push_back(power[pair]);kinds.push_back(counter[pair]);current[power[pair]]=current[counter[pair]]=true;}
 if(count%2){std::vector<int> defensive;for(int k:counter)if(!previousKinds[k]&&!current[k]&&(k!=CAUSAL||!resetSpawned))defensive.push_back(k);int kind=defensive[bounded(unsigned(defensive.size()))];kinds.push_back(kind);current[kind]=true;}
 for(int k=0;k<count;k++){int p=positions[k],kind=kinds[k];if(kind==RESET)resetSpawned=true;pickups[k]={kind,p%W,p/W};}
 previousKinds=current;suppressResetWave=false;wave++;
}
bool Items::concealedFrom(const State& s,int viewer,int team)const{return enabled&&active(team,INVISIBLE)&&!s.allied(viewer,team)&&!protectedBy(s,viewer,REVEAL);}
bool Items::opposed(const State& s,int team,int effect)const{for(int t=0;t<6;t++)if(s.players[t].score>0&&!s.allied(team,t)&&active(t,effect))return true;return false;}
bool Items::protectedBy(const State& s,int team,int effect)const{for(int t=0;t<6;t++)if(s.players[t].score>0&&s.allied(team,t)&&active(t,effect))return true;return false;}
bool Items::blockedByPickup(int x,int y)const{if(!enabled)return false;for(const auto& p:pickups)if(p.kind>=0&&std::abs(x-p.x)<=PICKUP_RADIUS&&std::abs(y-p.y)<=PICKUP_RADIUS&&square(x-p.x)+square(y-p.y)<=square(radiusForHp(p.hp)))return true;return false;}
bool Items::canEnter(State& s,int team,int x,int y)const{const int W=s.map->width,H=s.map->height;[[maybe_unused]] const int N=W*H;return inside(s,x,y)&&!blockedByPickup(x,y)&&(!s.map->isWall(x,y)||(active(team,PHASE)&&x>=16&&x<W-16&&y>=4&&y<H-4));}
bool Items::canContact(State& s,int team,int x,int y)const{
 if(!inside(s,x,y))return false;Dot* other=s.field[x][y];
 // Occupants remain solid enemies even inside a wall. Phase never grants armor.
 return other&&!s.allied(team,other->team)&&(!s.map->isWall(x,y)||active(team,PHASE)||active(other->team,PHASE));
}
void Items::begin(State& s){
 if(!enabled)return;capture(s);if(warmup>0){warmup=std::max(0,warmup-STEP_MS);return;}
 elapsed+=STEP_MS;
 for(int t=0;t<6;t++){
  if(until[t][PHASE]&&until[t][PHASE]<=elapsed){exitPhase(s,t);until[t][PHASE]=0;}
  if(until[t][DEATH]&&until[t][DEATH]<=elapsed)deathStrike(s,t);
  if(s.players[t].score<=0){until[t].fill(0);pending[t]=0;deathOwner[t]=-1;}
 }
 int before=epoch;
 for(int t=0;t<6;t++){Player& p=s.players[t];if(p.x[4]==-3)command(s,t,p.y[4],p.x[3],p.y[3]);if(epoch!=before)return;}
 if(!hasPickups()&&nextWave>=0&&elapsed>=nextWave){
  spawnPair(s);
  // Never replace a live wave. A full board retries without scanning every step.
  nextWave=hasPickups()?-1:elapsed+1000;
 }
}
int Items::moves(const State&,const Dot& d)const{return enabled&&(warmup>0||active(d.team,FREEZE))?0:1;}
void Items::contact(State& s,Dot& defender,Dot& attacker){
 if(s.allied(defender.team,attacker.team))return;
 // Hostile contact reveals both forces, even when a shield absorbs the hit.
 // Rendering, minimap and both AI observers read this same authoritative effect.
 if(enabled){until[defender.team][INVISIBLE]=0;until[attacker.team][INVISIBLE]=0;if(active(defender.team,INVINCIBLE))return;}
 int before=defender.team;combat::contact(defender,attacker.team,s.players);if(defender.team!=before)defender.squad=attacker.squad;
}
int Items::grow(State& s,int team,int count){const int W=s.map->width;if(team<0||team>=6||s.players[team].score==0||opposed(s,team,ANTIGROW))return 0;count=std::max(0,std::min(count,MAX_PARTICLES-int(s.dots.size())));if(!count)return 0;auto cells=area(s,team,count);s.dots.reserve(s.dots.size()+cells.size());for(int p:cells){Dot* d=new Dot(p%W,p/W,team);s.field[d->x][d->y]=d;s.dots.push_back(d);s.players[team].score++;}return int(cells.size());}
int Items::relocate(State& s,int team,int x,int y){const int W=s.map->width;
 if(!inside(s,x,y)||s.map->isWall(x,y))return 0;
 std::vector<Dot*> army;for(auto d:s.dots)if(d->team==team){army.push_back(d);s.field[d->x][d->y]=nullptr;}
 auto cells=area(s,team,int(army.size()),x,y);bool fits=!army.empty()&&cells.size()==army.size();
 for(size_t i=0;i<army.size();i++){Dot* d=army[i];if(fits){d->x=cells[i]%W;d->y=cells[i]/W;}s.field[d->x][d->y]=d;}if(fits)s.operations->clearTeam(s,team);return fits?int(army.size()):0;
}
void Items::exitPhase(State& s,int team){const int W=s.map->width,H=s.map->height;[[maybe_unused]] const int N=W*H;
 std::vector<int> nearest(N,-1),queue;queue.reserve(N);
 for(int y=4;y<H-4;y++)for(int x=16;x<W-16;x++)if(!s.map->isWall(x,y)&&!s.field[x][y]&&!blockedByPickup(x,y)){int p=y*W+x;nearest[p]=p;queue.push_back(p);}
 size_t floorCount=queue.size(),fallback=0;
 for(size_t at=0;at<queue.size();at++){int p=queue[at],y=p/W;for(auto delta:{-1,1,-W,W}){int q=p+delta;if(q<0||q>=N||(std::abs(delta)==1&&q/W!=y)||nearest[q]>=0)continue;nearest[q]=nearest[p];queue.push_back(q);}}
 for(auto d:s.dots)if(d->team==team&&s.map->isWall(d->x,d->y)){
  int p=nearest[d->y*W+d->x];if(p<0)continue;
  if(s.field[p%W][p/W]){int found=-1;for(int radius=1;radius<=24&&found<0;radius++)for(int dy=-radius;dy<=radius&&found<0;dy++)for(int dx=-radius;dx<=radius;dx++){if(std::abs(dx)!=radius&&std::abs(dy)!=radius)continue;int x=p%W+dx,y=p/W+dy;if(inside(s,x,y)&&!s.map->isWall(x,y)&&!s.field[x][y]&&!blockedByPickup(x,y)){found=y*W+x;break;}}
   if(found<0){while(fallback<floorCount&&s.field[queue[fallback]%W][queue[fallback]/W])fallback++;if(fallback<floorCount)found=queue[fallback++];}p=found;
  }
  if(p>=0){s.field[d->x][d->y]=nullptr;d->x=p%W;d->y=p/W;s.field[d->x][d->y]=d;}
 }
}
void Items::quarry(State& s,int team){const int W=s.map->width;
    if(opposed(s,team,ROCKSEAL))return;
    std::vector<int> cells;cells.reserve(64);
    for(auto d:s.dots){if(d->team!=team)continue;
        for(int dy=-1;dy<=1;dy++)for(int dx=-1;dx<=1;dx++){
            if(int(cells.size())>=64)break;int x=int(d->x)+dx,y=int(d->y)+dy;
            if(s.map->excavate(x,y))cells.push_back(y*W+x);
        }
        if(cells.size()>=64)break;
    }
    for(int p:cells)if(quarryBudget[team]>0&&!opposed(s,team,ANTIGROW)&&int(s.dots.size())<MAX_PARTICLES&&!s.field[p%W][p/W]&&!blockedByPickup(p%W,p/W)){
        Dot* d=new Dot(p%W,p/W,team);s.field[d->x][d->y]=d;s.dots.push_back(d);s.players[team].score++;quarryBudget[team]--;
    }
}

void Items::grant(State& s,int team,int kind,int x,int y){
 if(team<0||team>=6||kind<0||kind>=COUNT||s.players[team].score<=0)return;capture(s);int amount=0;
 switch(kind){
 case DOUBLE:amount=grow(s,team,std::min(std::max(1,s.players[team].score/4),std::max(1,s.dotsPerTeam/2)));break;
 case QUARRY:if(opposed(s,team,ROCKSEAL)){amount=-1;break;}until[team][QUARRY]=elapsed+EFFECT_MS;quarryBudget[team]=std::min(500,std::max(1,s.dotsPerTeam/4));break;
 case FREEZE:for(int t=0;t<6;t++)if(!s.allied(team,t)&&s.players[t].score>0&&!protectedBy(s,t,THAW))until[t][FREEZE]=elapsed+EFFECT_MS;break;
 case PHASE:if(opposed(s,team,PHASENAIL)){amount=-1;break;}until[team][PHASE]=elapsed+EFFECT_MS;break;
 case INVINCIBLE:if(opposed(s,team,SHATTER)){amount=-1;break;}until[team][INVINCIBLE]=elapsed+EFFECT_MS;break;
 case TELEPORT:case DEATH:case FRIEND:pending[team]=kind;nonce[team]=(last.serial+1)%30000+1;break;
 case INVISIBLE:if(opposed(s,team,REVEAL)){amount=-1;break;}until[team][INVISIBLE]=elapsed+EFFECT_MS;break;
 case REVEAL:until[team][REVEAL]=elapsed+EFFECT_MS;for(int t=0;t<6;t++)if(!s.allied(team,t))until[t][INVISIBLE]=0;break;
 case RESET:resetSpawned=true;if(opposed(s,team,CAUSAL)){amount=-1;break;}rewind(s);break;
 case ANTIGROW:case ROCKSEAL:case ANCHOR:case CAUSAL:until[team][kind]=elapsed+EFFECT_MS;break;
 case THAW:until[team][THAW]=elapsed+EFFECT_MS;for(int t=0;t<6;t++)if(s.allied(team,t))until[t][FREEZE]=0;break;
 case PHASENAIL:until[team][PHASENAIL]=elapsed+EFFECT_MS;for(int t=0;t<6;t++)if(!s.allied(team,t)&&active(t,PHASE)){exitPhase(s,t);until[t][PHASE]=0;}break;
 case SHATTER:until[team][SHATTER]=elapsed+EFFECT_MS;for(int t=0;t<6;t++)if(!s.allied(team,t))until[t][INVINCIBLE]=0;break;
 case WARD:until[team][WARD]=elapsed+EFFECT_MS;break;
 case LIBERTY:{until[team][LIBERTY]=elapsed+EFFECT_MS;if(s.alliances[team]==truceA||s.alliances[team]==truceB){truceUntil=0;for(auto& buffs:until)buffs[FRIEND]=0;}break;}
 default:break;
 }
 emit(kind,team,x,y,amount,-1,(kind==TELEPORT||kind==DEATH||kind==FRIEND)?1:0);
}
bool Items::command(State& s,int team,int token,int x,int y){const int W=s.map->width,H=s.map->height;
 int kind=pending[team];if(!kind||nonce[team]!=token||s.players[team].score<=0||active(team,FREEZE)||warmup>0)return false;
 int n=0,target=-1,fx=x,fy=y;int signature=(token*7919+x*479+y)*31+kind;
 auto reject=[&](){if(attempt[team]!=signature){attempt[team]=signature;emit(kind,team,std::max(0,std::min(W-1,x)),std::max(0,std::min(H-1,y)),-1,-1,3);}return false;};
 if(kind==TELEPORT){if(opposed(s,team,ANCHOR))return reject();n=relocate(s,team,x,y);if(!n)return reject();}
 else {target=x;if(target<0||target>=6||s.players[target].score<=0||s.allied(team,target))return reject();
  int sx=0,sy=0;for(auto d:s.dots)if(d->team==target){sx+=d->x;sy+=d->y;}fx=sx/s.players[target].score;fy=sy/s.players[target].score;
  if(concealedFrom(s,team,target))return reject();
  if(kind==DEATH){if(active(target,INVINCIBLE))return reject();pending[team]=0;deathOwner[target]=team;deathStrike(s,target);return true;}
  else if(kind==FRIEND){if(protectedBy(s,target,LIBERTY)||truceUntil>elapsed)return reject();truceA=s.alliances[team];truceB=s.alliances[target];truceUntil=elapsed+EFFECT_MS;until[team][FRIEND]=truceUntil;n=1;}

 }
 pending[team]=0;emit(kind,team,fx,fy,n,target,kind==DEATH?4:2);return true;
}
void Items::collect(State& s){for(auto& pickup:pickups){if(pickup.kind<0)continue;
 std::array<int,6> contact{},spent{};pickup.eater=-1;
 for(auto d:s.dots){if(active(d->team,FREEZE)&&pickup.kind!=THAW)continue;int dist=square(int(d->x)-pickup.x)+square(int(d->y)-pickup.y);if(dist<=square(radiusForHp(pickup.hp)+2)&&clear(s,d->x,d->y,pickup.x,pickup.y))contact[d->team]++;}
 const int rotation=(wave+elapsed/STEP_MS)%6;
 // Shared capped damage: even a dense army must devour for >=2.1 simulation seconds.
 int cursor=rotation;
 for(int hit=0;hit<MAX_DEVOUR_PER_STEP&&pickup.hp>0;hit++){
  int team=-1;for(int n=0;n<6;n++){int t=(cursor+n)%6;if(spent[t]<contact[t]){team=t;break;}}
  if(team<0)break;spent[team]++;pickup.damage[team]++;pickup.hp--;pickup.eater=team;cursor=(team+1)%6;
 }
 if(pickup.hp==0){int winner=-1,best=-1;
  for(int n=0;n<6;n++){int t=(rotation+n)%6;if(s.players[t].score>0&&pickup.damage[t]>best){winner=t;best=pickup.damage[t];}}
  if(winner>=0){int kind=pickup.kind,x=pickup.x,y=pickup.y;pickup.kind=-1;int before=epoch;grant(s,winner,kind,x,y);if(before!=epoch)return;}
 }
}}
void Items::deathStrike(State& s,int target){
 int owner=deathOwner[target];until[target][DEATH]=0;deathOwner[target]=-1;
 if(owner<0||s.players[owner].score<=0||s.players[target].score<=0||s.allied(owner,target))return;
 int x=0,y=0;for(auto d:s.dots)if(d->team==target){x+=d->x;y+=d->y;}x/=s.players[target].score;y/=s.players[target].score;
 if(active(target,WARD)){until[target][WARD]=0;emit(WARD,target,x,y,1,owner,2);emit(DEATH,owner,x,y,-1,target,5);return;}
 if(active(target,INVINCIBLE)){emit(DEATH,owner,x,y,-1,target,3);return;}
 int loss=s.players[target].score,removed=0;
 std::vector<Dot*> alive;alive.reserve(s.dots.size());for(auto d:s.dots)if(d->team==target&&removed<loss){s.field[d->x][d->y]=nullptr;delete d;removed++;}else alive.push_back(d);
 s.dots.swap(alive);s.players[target].score-=removed;until[target].fill(0);pending[target]=0;quarryBudget[target]=0;s.points.resize(s.dots.size()*3,0);s.colours.resize(s.dots.size()*4,1);emit(DEATH,owner,x,y,removed,target,2);
}
void Items::end(State& s){if(!enabled||warmup>0)return;if(elapsed/STEP_MS%14==0)for(int t=0;t<6;t++)if(active(t,QUARRY))quarry(s,t);collect(s);
 if(warmup==0&&wave>0&&!hasPickups()&&nextWave<0)nextWave=elapsed+STEP_MS;
 if(s.points.size()!=s.dots.size()*3){s.points.resize(s.dots.size()*3,0);s.colours.resize(s.dots.size()*4,1);}}
std::vector<int> Items::snapshot(const State& s)const{const int W=s.map->width,H=s.map->height;[[maybe_unused]] const int N=W*H;
 std::vector<int> out={elapsed,nextWave,wave,int(s.dots.size()),MAX_PARTICLES,s.map->revision(),last.serial,last.kind,last.team,last.x,last.y,last.amount,last.at,s.players[0].score,s.players[1].score,s.players[2].score,s.players[3].score,s.players[4].score,s.players[5].score};
 for(int k=0;k<2;k++){auto p=pickups[k];for(int v:{p.kind,p.x,p.y})out.push_back(v);}
 for(int t=0;t<6;t++)for(int k=0;k<BUFF_STRIDE;k++)out.push_back(k<COUNT?std::max(0,until[t][k]-elapsed):0);
 out.push_back(0);out.push_back(epoch);
 // Read-only statistics: one pass instead of rescanning the full army six times.
 int sx[6]={},sy[6]={},n[6]={},minx[6]={W,W,W,W,W,W},miny[6]={H,H,H,H,H,H},maxx[6]={},maxy[6]={};
 for(auto d:s.dots){int t=d->team;sx[t]+=d->x;sy[t]+=d->y;n[t]++;minx[t]=std::min(minx[t],int(d->x));miny[t]=std::min(miny[t],int(d->y));maxx[t]=std::max(maxx[t],int(d->x));maxy[t]=std::max(maxy[t],int(d->y));}
 for(int t=0;t<6;t++)for(int v:{pending[t],nonce[t],n[t]?sx[t]/n[t]:0,n[t]?sy[t]/n[t]:0,minx[t],miny[t],maxx[t],maxy[t],s.alliances[t],n[t]})out.push_back(v);
 out.push_back(warmup);out.push_back(int(events.size()));for(auto e:events)for(int v:{e.serial,e.kind,e.team,e.x,e.y,e.amount,e.at,e.target,e.stage})out.push_back(v);
 // Bounded actual particle samples for effect hulls/ice/shield/ghost trails.
 std::vector<int> samples;int counts[6]={};for(auto d:s.dots){int stride=std::max(1,s.players[d->team].score/72);if((counts[d->team]++%stride)==0&&int(samples.size())<6*96*3)for(int v:{int(d->team),int(d->x),int(d->y)})samples.push_back(v);}
 out.push_back(int(samples.size()/3));out.insert(out.end(),samples.begin(),samples.end());
 // Bounded balancing state; preserve fixed header and final trailer.
 out.push_back(0x4c573137);for(int v:{resetSpawned?1:0,truceA,truceB,truceUntil})out.push_back(v);for(int v:quarryBudget)out.push_back(v);for(int v:deathOwner)out.push_back(v);
 // Health block precedes the existing fixed six-slot trailer. Hash includes contributions.
 out.push_back(0x4c574850);out.push_back(MAX_PICKUPS);
 for(const auto& p:pickups){int leader=-1,best=0;for(int t=0;t<6;t++)if(p.damage[t]>best){leader=t;best=p.damage[t];}for(int v:{p.kind<0?0:p.hp,PICKUP_HP,leader,p.eater})out.push_back(v);for(int v:p.damage)out.push_back(v);}
 out.push_back(0x4c573136);out.push_back(MAX_PICKUPS);for(auto p:pickups)for(int v:{p.kind,p.x,p.y})out.push_back(v);int history=0;for(int k=0;k<COUNT;k++)if(previousKinds[k])history|=1<<k;out.push_back(history);out.push_back((suppressResetWave?1:0)|(resetSpawned?2:0));return out;
}
