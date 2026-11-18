// Test APK only. Controlled inventory/positions, never linked into game APK.
#include "nativeinterface/state_guard.hpp"
#include "application/state.hpp"
#include "application/expedition.hpp"
#include "application/garden3d.hpp"
#include <jni.h>
#include <algorithm>
extern "C" JNIEXPORT void JNICALL Java_com_xenris_liquidwarsos_Audio206ProbeTest_expeditionFixture(JNIEnv*,jclass,jint mode){
 std::lock_guard<std::mutex> guard(nativeStateMutex);if(!state||!state->expedition)return;auto& s=*state;auto& i=*s.items;
 if(mode==0){i.warmup=0;s.expedition->loot.clear();s.expedition->nextLoot=INT32_MAX;for(auto& seat:s.expedition->seats)seat.bot=false;}
 if(mode==1){Dot* a=nullptr;Dot* d=nullptr;for(auto dot:s.dots){if(dot->team==0)a=dot;if(dot->team==1)d=dot;}if(a&&d){d->health=1;for(int n=0;n<20&&d->team==1;n++)i.contact(s,*d,*a);}}
 if(mode==2){i.grant(s,0,Items::ACTIVE_BLACK_HOLE,0,0);i.activate(s,0,1,Items::BLACK_HOLE_SLOT);}
}
extern "C" JNIEXPORT void JNICALL Java_com_xenris_liquidwarsos_Classic193VisualTest_fixture(JNIEnv*,jclass,jint mode){
 std::lock_guard<std::mutex> guard(nativeStateMutex);if(!state||state->expedition)return;auto& s=*state;auto& i=*s.items;
 if(mode==0){i.stock[0]={2,2};i.ready[0]={0,0};i.nextWave=INT32_MAX;for(int t=0;t<6;t++){i.until[t].fill(0);i.immunity[t]=0;s.alliances[t]=t;}}
 else if(mode==1){i.thaw(s,1,0);i.freezeEnemies(s,1,Items::FREEZE_MS);}
 else if(mode==3){
  Dot* attacker=nullptr;Dot* defender=nullptr;
  for(auto d:s.dots){if(d->team==0)attacker=d;if(d->team==1)defender=d;}
  if(attacker&&defender){defender->health=1;for(int n=0;n<20&&defender->team==1;n++)i.contact(s,*defender,*attacker);}
 }
 else {
  i.stock[0]={0,1,2};i.ready[0].fill(0);i.nextWave=i.elapsed+14000;
  // Isolate suction from AI combat. Freeze does not block black-hole attraction.
  for(int t=1;t<s.teamCount;t++)i.until[t][Items::FREEZE]=i.elapsed+20000;
  i.until[0][Items::INVINCIBLE]=i.elapsed+20000;
  for(int t=0;t<s.teamCount;t++){s.operations->clearTeam(s,t);s.players[t].x[0]=s.players[t].y[0]=-2;}
  // Give the test time to tap Cleanse before the caster reaches the core.
  int x=s.map->width/2,y=s.map->height/2;
  for(int dx=340;dx>=260;dx-=10)if(!s.map->isWall(x+dx,y)){
   i.grant(s,0,Items::TELEPORT,x,y);
   if(i.command(s,0,i.nonce[0],x+dx,y))break;
  }
  for(int dx=-15;dx<=15;dx++)for(int dy=-15;dy<=15;dy++){
   int d2=dx*dx+dy*dy;
   if(d2<49||d2>225||(dx+dy)%3!=0||s.map->isWall(x+dx,y+dy)||s.field[x+dx][y+dy])continue;
   auto* dot=new Dot(x+dx,y+dy,0);s.dots.push_back(dot);s.field[dot->x][dot->y]=dot;++s.players[0].score;
  }
  s.points.resize(s.dots.size()*3);s.colours.resize(s.dots.size()*4);
  // Keep absorption witnesses still during the new warning interval. The real
  // player remains free to steer; this is only the instrumented test fixture.
  // All teams idle: otherwise the real bots can finish this fixture's battle
  // while the UI test waits for the slower warning/ramp, freezing its clock.
  for(int t=0;t<s.teamCount;t++)for(auto& route:s.operations->routes[t]){route=Operations::Route{};route.enabled=route.hold=true;}
 }
}
extern "C" JNIEXPORT jintArray JNICALL Java_com_xenris_liquidwarsos_Guest193VisualTest_fixture(JNIEnv* env,jclass,jint mode){
 std::lock_guard<std::mutex> guard(nativeStateMutex);if(!state||!state->expedition)return nullptr;auto& s=*state;auto& e=*s.expedition;
 if(mode==1){
  for(int t=0;t<s.teamCount;t++){int x=s.birthX[t],y=s.birthY[t];e.seats[t].bot=false;
   for(int r=0;r<48&&s.players[t].score<2000;r++)for(int dy=-r;dy<=r&&s.players[t].score<2000;dy++)for(int dx=-r;dx<=r&&s.players[t].score<2000;dx++){
    if(std::max(std::abs(dx),std::abs(dy))!=r||s.map->isWall(x+dx,y+dy)||s.field[x+dx][y+dy])continue;
    auto d=new Dot(x+dx,y+dy,t);s.dots.push_back(d);s.field[d->x][d->y]=d;s.players[t].score++;
   }
   s.operations->order(s,t,3,x,y);
  }
  s.items->warmup=0;s.items->epoch++;s.points.resize(s.dots.size()*3);s.colours.resize(s.dots.size()*4);garden3d::resetVisual();garden3d::sampleStep();
  jint v[]={s.birthX[1],s.birthY[1],0,0,0,0};auto out=env->NewIntArray(6);env->SetIntArrayRegion(out,0,6,v);return out;
 }
 // Find a real broad, connected clearing. Never clear map collision to fit test.
 int x=-1,y=-1;for(int yy=150;yy<s.map->height-150&&x<0;yy+=24)for(int xx=150;xx<s.map->width-150&&x<0;xx+=24){bool ok=true;for(int b=-30;b<=30&&ok;b++)for(int a=-110;a<=110;a++)if(s.map->isWall(xx+a,yy+b)){ok=false;break;}if(ok){x=xx;y=yy;}}
 if(x<0)return nullptr;
 for(auto d:s.dots)delete d;s.dots.clear();s.field.clear();for(auto& p:s.players)p=Player{};
 std::vector<std::pair<int,int>> offsets;for(int b=-19;b<=19;b++)for(int a=-19;a<=19;a++)if(a*a+b*b<=361)offsets.push_back({a,b});
 std::stable_sort(offsets.begin(),offsets.end(),[](auto a,auto b){return a.first*a.first+a.second*a.second<b.first*b.first+b.second*b.second;});
 for(int t=0;t<2;t++){int xx=x+(t? -42:42);s.birthX[t]=xx;s.birthY[t]=y;
  for(int i=0;i<1000;i++){auto d=new Dot(xx+offsets[i].first,y+offsets[i].second,t);s.dots.push_back(d);s.field[d->x][d->y]=d;s.players[t].score++;}
  e.seats[t]=Expedition::Seat{};e.seats[t].bot=false;s.items->stock[t]={2,2};s.items->ready[t]={0,0};s.items->immunity[t]=0;s.items->until[t].fill(0);
  s.operations->clearTeam(s,t);
  s.players[t].x[0]=xx;s.players[t].y[0]=y;
  s.operations->order(s,t,3,xx,y);
  s.operations->routes[t][0].hold=true;
 }
 e.clock=130004;s.operations->clock=e.clock;e.cutoff=-1;e.loot.clear();e.anchors.clear();e.chills.clear();e.nextLoot=e.clock+90000;
 // Four actual coin pickups, consumed by production pickup logic on next steps.
 for(int k=0;k<4;k++)e.loot.push_back({++e.sequence,2,x-42+k,y,e.clock});
 e.loot.push_back({++e.sequence,3+Items::DOUBLE,x-42,y,e.clock});
 for(int k=0;k<4;k++)e.loot.push_back({++e.sequence,k==3?3+Items::INVISIBLE:k,x+55+k*7,y+30,e.clock});
 auto& site=s.operations->sites[0];site.x=x-42;site.y=y;site.owner=site.claim=-1;site.progress=site.born=0;site.spawnAt=e.clock;
 // A pre-existing eligible object is moved to a legal empty edge of the clearing.
 auto& o=e.obstacles[0];s.map->temporaryObstacle(o.x,o.y,o.radius,false);o.x=x-12;o.y=y+22;o.hp=120;o.removed=false;o.wardUntil=0;
 if(!s.map->temporaryObstacle(o.x,o.y,o.radius,true))return nullptr;
 s.items->warmup=0;s.items->elapsed=e.clock;s.items->epoch++;s.points.resize(s.dots.size()*3);s.colours.resize(s.dots.size()*4);
 garden3d::resetVisual();garden3d::sampleStep();
 jint v[]={x-42,y,x+42,y,o.x,o.y};auto out=env->NewIntArray(6);env->SetIntArrayRegion(out,0,6,v);return out;
}
