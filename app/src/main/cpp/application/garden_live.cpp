// SPDX-License-Identifier: GPL-3.0-or-later
// Garden GLES2: real meshes, moving water/leaves, live shadows and sampled motion.
#include <GLES2/gl2.h>
#include "garden3d.hpp"
#include "state.hpp"
#include "expedition.hpp"
#include "minimap_sight.hpp"
#include "visual_palette.hpp"
#include "expedition.hpp"
#include <android/log.h>
#include <chrono>
#include <unordered_map>
#include <cstring>
#include <cmath>
#include <algorithm>
#include <string>
#include <mutex>
#include <memory>
#include <atomic>
namespace garden3d { namespace {
double now(){return std::chrono::duration<double,std::milli>(std::chrono::steady_clock::now().time_since_epoch()).count();}
struct V {float x,y,z,nx,ny,nz,r,g,b,wind,rough;};
struct Mesh {GLuint vbo=0;int count=0,kind=0;float lo[3]{},hi[3]{};};
struct Point {float x=0,y=0;int at=0;};
struct Track {std::array<Point,12> p{};int size=0,seen=0,team=-1;};
struct PresentedDot {Point a,b;std::array<Point,11> path{};float r=0,jx=0,jy=0,color[3]{},code=0;bool concealed=false;};
struct PresentedHud {int clock=0;std::array<std::vector<int>,4> facts;};
struct PresentedState {std::vector<PresentedDot> dots;std::vector<float> sites,props;std::shared_ptr<const PresentedHud> hud;int clock=0,epoch=0,own=0,viewer=0,seats=0,birthX=0,birthY=0,total=0;double sampled=0;bool warmup=false,remote=false;};
struct ArmyVfx {double x=0,y=0;int count=0,mask=0;float minx=1e9f,miny=1e9f,maxx=-1e9f,maxy=-1e9f;
 void add(float px,float py,int effects){x+=px;y+=py;count++;mask|=effects;minx=std::min(minx,px);miny=std::min(miny,py);maxx=std::max(maxx,px);maxy=std::max(maxy,py);}};
void appendArmyVfx(PresentedState& frame,const std::array<ArmyVfx,MAX_TEAMS>& armies,const std::vector<int>& exp){
 auto prop=[&](int kind,int id,float x,float y,float scale,int team,float status,float progress){float values[]={float(kind),float(id),x,y,scale,float(team),status,progress};frame.props.insert(frame.props.end(),values,values+8);};
 for(int t=0;t<MAX_TEAMS;t++){const auto& a=armies[t];if(!a.count)continue;float x=a.x/a.count,y=a.y/a.count,r=std::max(8.f,std::min(65.f,std::max(a.maxx-a.minx,a.maxy-a.miny)*.5f+3));
  prop(50,-2000-t,x,y,r,t,0,1);
  for(int k=0;k<22;k++)if(a.mask&(1<<k))prop(60+k,-3000-t*32-k,x,y,r,t,0,1);
 }
 if(exp.size()<11)return;for(int t=0;t<exp[4];t++){int base=11+t*63;if(exp[base+42]!=0x4c485031||exp[base+17]<=0)continue;
  int packed=exp[base+26],kind=packed&31,xy=exp[base+25];if(kind>21)continue;
  prop(100+kind,-1000000-exp[base+17],float(xy&4095)+.5f,float((xy>>12)&4095)+.5f,1,t,exp[base+18]/1000.f,float((packed>>9)-32768));
 }
}
std::mutex presentationMutex;
std::shared_ptr<const PresentedState> presentation;
std::atomic<bool> presentationPaused{false};
std::atomic<bool> presentationActive{false};
std::vector<Mesh> meshes;std::vector<float> heights;std::unordered_map<uint64_t,Track> history;
std::vector<V> moving,sphere;
GLuint program=0,shadowFbo=0,shadowTex=0,shadowDepth=0,dynamicVbo=0,sphereEbo=0;
int sphereIndexCount=0;
GLint viewLoc=-1,shadowViewLoc=-1,timeLoc=-1,passLoc=-1,kindLoc=-1;
int ready=0,epoch=-1,sequence=0,draws=0,visibleCount=0,triangles=0,lastClock=0,resets=0;
double sampledAt=0,previousFrame=0,frameCpu=0,frameGap=0,envTime=0,totalCpu=0,maxCpu=0;
bool paused=false;float river[6]{};constexpr int SHADOW=1024;
float clamp(float x,float a,float b){return std::max(a,std::min(b,x));}
float elevation(float x,float y){
 x=clamp(x-.5f,0,ARENA_WIDTH-1.001f);y=clamp(y-.5f,0,ARENA_HEIGHT-1.001f);int ix=int(x),iy=int(y);float fx=x-ix,fy=y-iy;
 float a=heights[iy*ARENA_WIDTH+ix],b=heights[iy*ARENA_WIDTH+ix+1],c=heights[(iy+1)*ARENA_WIDTH+ix],d=heights[(iy+1)*ARENA_WIDTH+ix+1];
 if(std::max({a,b,c,d})-std::min({a,b,c,d})>2)return heights[int(clamp(y+.5f,0,ARENA_HEIGHT-1))*ARENA_WIDTH+int(clamp(x+.5f,0,ARENA_WIDTH-1))];
 return (a*(1-fx)+b*fx)*(1-fy)+(c*(1-fx)+d*fx)*fy;
}
std::string source(const char* file){int n=0;auto p=acAssets->getFile(file,&n);if(!p)return {};std::string s(reinterpret_cast<char*>(p),n);free(p);return s;}
GLuint shader(GLenum kind,const char* file){auto s=source(file);const char* text=s.c_str();GLuint id=glCreateShader(kind);glShaderSource(id,1,&text,nullptr);glCompileShader(id);GLint ok=0;glGetShaderiv(id,GL_COMPILE_STATUS,&ok);if(!ok){char log[2048];glGetShaderInfoLog(id,sizeof(log),nullptr,log);__android_log_print(ANDROID_LOG_ERROR,"LWGarden3D","shader %s: %s",file,log);glDeleteShader(id);return 0;}return id;}
void bind(GLuint buffer){glBindBuffer(GL_ARRAY_BUFFER,buffer);const int sizes[]={3,3,3,2},offsets[]={0,12,24,36};for(int a=0;a<4;a++){glEnableVertexAttribArray(a);glVertexAttribPointer(a,sizes[a],GL_FLOAT,GL_FALSE,sizeof(V),reinterpret_cast<void*>(size_t(offsets[a])));}}
void upload(const std::vector<V>& v,int kind){if(v.empty())return;glUniform1f(kindLoc,float(kind));bind(dynamicVbo);glBufferData(GL_ARRAY_BUFFER,v.size()*sizeof(V),v.data(),GL_STREAM_DRAW);if(kind==2||kind==3){glBindBuffer(GL_ELEMENT_ARRAY_BUFFER,sphereEbo);glDrawElements(GL_TRIANGLES,int(v.size()/sphere.size())*sphereIndexCount,GL_UNSIGNED_SHORT,nullptr);glBindBuffer(GL_ELEMENT_ARRAY_BUFFER,0);}else glDrawArrays(GL_TRIANGLES,0,v.size());}
void palette(int team,float* c){const float colors[10][3]={{.008f,.40f,.27f},{.08f,.23f,.65f},{.80f,.14f,.065f},{.70f,.67f,.52f},{.72f,.38f,.025f},{.33f,.10f,.5f},{.7f,.18f,.34f},{.18f,.30f,.4f},{.32f,.5f,.08f},{.45f,.22f,.1f}};for(int k=0;k<3;k++)c[k]=colors[std::max(0,std::min(9,team))][k];}
void geometry(float x,float y,float w,float h,bool shadow){
 float m=shadow?110:4;
 for(auto& o:meshes){if(shadow&&o.kind==2)continue;if(o.lo[0]>x+w+m||o.hi[0]<x-m||o.lo[1]>y+h+m||o.hi[1]<y-m)continue;glUniform1f(kindLoc,o.kind==2?1.f:0.f);bind(o.vbo);glDrawArrays(GL_TRIANGLES,0,o.count);}
}
void makeSphere(){
 sphere.clear();const int lon=10,lat=5;
 auto v=[](float a,float b){float x=std::sin(a)*std::cos(b),y=std::sin(a)*std::sin(b),z=std::cos(a);return V{x,y,z,x,y,z,1,1,1,0,.58f};};
 sphere.push_back(v(0,0));for(int j=1;j<lat;j++)for(int i=0;i<lon;i++)sphere.push_back(v(float(M_PI)*j/lat,float(M_PI)*2*i/lon));sphere.push_back(v(float(M_PI),0));
 std::vector<GLushort> unit,indices;auto face=[&](int a,int b,int c){unit.push_back(a);unit.push_back(b);unit.push_back(c);};
 for(int i=0;i<lon;i++){int next=(i+1)%lon;face(0,1+i,1+next);face(41,31+next,31+i);for(int j=0;j<3;j++){int a=1+j*lon+i,d=1+j*lon+next;face(a,a+lon,d);face(a+lon,d+lon,d);}}
 sphereIndexCount=unit.size();for(int i=0;i<1024;i++)for(auto index:unit)indices.push_back(GLushort(i*sphere.size()+index));
 glGenBuffers(1,&sphereEbo);glBindBuffer(GL_ELEMENT_ARRAY_BUFFER,sphereEbo);glBufferData(GL_ELEMENT_ARRAY_BUFFER,indices.size()*2,indices.data(),GL_STATIC_DRAW);glBindBuffer(GL_ELEMENT_ARRAY_BUFFER,0);
}
void ball(float x,float y,float z,float r,float vertical,const float* color){for(auto v:sphere){v.x=x+v.x*r;v.y=y+v.y*r;v.z=z+v.z*r*vertical;v.r=color[0];v.g=color[1];v.b=color[2];moving.push_back(v);}}
Point position(const Dot& d,float at){
 auto it=history.find(d.visualId);Point current{float(d.x)+.5f,float(d.y)+.5f,0};if(it==history.end()||it->second.team!=d.team)return current;
 const auto& t=it->second;if(!t.size)return current;
 if(std::abs(t.p[t.size-1].x-current.x)>2||std::abs(t.p[t.size-1].y-current.y)>2)return current;
 auto out=t.p[0];for(int k=1;k<t.size;k++){const auto a=t.p[k-1],b=t.p[k];if(at>=b.at){out=b;continue;}float f=clamp((at-a.at)/std::max(1,b.at-a.at),0,1);out={a.x+(b.x-a.x)*f,a.y+(b.y-a.y)*f,0};break;}
 return state->map->isWall(int(out.x),int(out.y))?current:out;
}
void particles(float x,float y,float w,float h,bool spectator){
 moving.clear();visibleCount=0;float at=float(envTime*1000-21);
 for(auto d:state->dots){if(d->x<x-4||d->y<y-4||d->x>x+w+4||d->y>y+h+4||(!spectator&&state->items->concealedFrom(*state,state->me,d->team)))continue;
  Point p=position(*d,at);uint64_t hash=d->visualId*0x9e3779b97f4a7c15ull;
  // Larger readable grains, still fully inside their collision cell: .405+.085 < .5.
  float jx=(int((hash>>32)&255)-127)*(.085f/128),jy=(int((hash>>40)&255)-127)*(.085f/128),r=.385f+float((hash>>48)&255)*(.02f/255);
  float c[3];palette(d->team,c);for(auto& v:c)v*=.83f+.17f*d->health/MAX_HEALTH;
  ball(p.x+jx,p.y+jy,elevation(p.x+jx,p.y+jy)+.13f,r,.88f,c);visibleCount++;
  if(moving.size()>=1024*sphere.size()){upload(moving,2);moving.clear();}
 }upload(moving,2);moving.clear();
}
void sites(){moving.clear();for(const auto& s:state->operations->sites){float c[3]={.16f,.26f,.19f};if(s.owner>=0)palette(s.owner,c);float z=elevation(s.x+.5f,s.y+.5f);ball(s.x,s.y,z+1.3f,7,.24f,c);float ivory[]={.66f,.63f,.44f};for(int i=0;i<3;i++){float a=i*float(M_PI)*2/3;ball(s.x+std::cos(a)*6,s.y+std::sin(a)*6,z+3.2f,4,.55f,ivory);}}upload(moving,3);moving.clear();}
}
void resetVisual(){presentationActive=false;history.clear();epoch=-1;sequence=0;sampledAt=now();lastClock=0;envTime=0;paused=false;presentationPaused=false;previousFrame=0;draws=0;totalCpu=maxCpu=0;resets++;std::lock_guard<std::mutex> lock(presentationMutex);presentation.reset();}
void beginMatch(){resetVisual();ready=0;presentationActive=true;}
bool hudSnapshot(int kind,std::vector<int>& out){
 if(!presentationActive)return false;
 std::shared_ptr<const PresentedState> p;{std::lock_guard<std::mutex> lock(presentationMutex);p=presentation;}
 if(p){if(kind==4)out={p->clock};else if(p->hud&&kind>=0&&kind<4)out=p->hud->facts[kind];}
 return true;
}
void sampleStep(){
 if(!state||!state->garden3D)return;int time=state->items->elapsed;
 if(epoch!=state->items->epoch||time<lastClock){history.clear();epoch=state->items->epoch;resets++;}lastClock=time;sampledAt=now();sequence++;
 for(auto d:state->dots){auto& t=history[d->visualId];Point p{float(d->x)+.5f,float(d->y)+.5f,time};
  if(t.team!=d->team||(t.size&&(std::abs(p.x-t.p[t.size-1].x)>1.01f||std::abs(p.y-t.p[t.size-1].y)>1.01f)))t.size=0;
  if(t.size){auto a=t.p[t.size-1];if(int(a.x)!=int(p.x)&&int(a.y)!=int(p.y)&&(state->map->isWall(int(a.x),int(p.y))||state->map->isWall(int(p.x),int(a.y))))t.size=0;}
  if(t.size&&t.p[t.size-1].at==time)t.p[t.size-1]=p;else{if(t.size==12){for(int k=1;k<12;k++)t.p[k-1]=t.p[k];t.size=11;}t.p[t.size++]=p;}t.seen=sequence;t.team=d->team;
 }
 for(auto it=history.begin();it!=history.end();)if(it->second.seen!=sequence)it=history.erase(it);else ++it;
 // Publish only a completed simulation tick. The render thread never waits on
 // the simulation mutex, navigation rebuild or AI. The short swap is independent
 // of cameras; all real units are retained at their original size and quality.
 auto frame=std::make_shared<PresentedState>();frame->clock=time;frame->epoch=epoch;frame->sampled=sampledAt;frame->warmup=state->items->warmup>0;frame->own=state->players[state->me].score;
 {std::lock_guard<std::mutex> lock(presentationMutex);if(presentation)frame->hud=presentation->hud;}
 if(!frame->hud||time-frame->hud->clock>=70||time<frame->hud->clock){
  auto hud=std::make_shared<PresentedHud>();hud->clock=time;
  if(!state->expedition)hud->facts[0]=state->items->snapshot(*state);
  hud->facts[1]=state->operations->snapshot(*state,state->me,false);
  hud->facts[2]=minimap_sight::snapshot(*state,false);
  if(state->expedition)hud->facts[3]=state->expedition->snapshot(*state,state->me);
  frame->hud=std::move(hud);
 }
 if(state->expedition)state->expedition->refreshSight(*state);
 std::array<ArmyVfx,MAX_TEAMS> armies{};frame->dots.reserve(state->dots.size());
 for(auto d:state->dots){PresentedDot p;p.a=position(*d,float(time-21));p.b=position(*d,float(time-14));uint64_t hash=d->visualId*0x9e3779b97f4a7c15ull;
  p.r=.44f+float((hash>>48)&255)*(.01f/255);p.jx=(int((hash>>32)&255)-127)*(.035f/128);p.jy=(int((hash>>40)&255)-127)*(.035f/128);
  for(auto& c:p.color)c=visual::healthLevel(d->health/float(MAX_HEALTH));
  if(state->items->active(d->team,Items::FREEZE)){p.color[0]=105/255.f;p.color[1]=205/255.f;p.color[2]=1;}
  if(state->items->active(d->team,Items::PHASE)){p.color[0]=210/255.f;p.color[1]=170/255.f;p.color[2]=1;}
  if(state->items->active(d->team,Items::INVINCIBLE)){p.color[0]=1;p.color[1]=245/255.f;p.color[2]=155/255.f;}
  p.code=(d->team+(state->items->active(d->team,Items::INVISIBLE)?.144f:.32f))/16.f;p.concealed=state->items->concealedFrom(*state,state->me,d->team)||(state->expedition&&d->team!=state->me&&!state->expedition->enemyVisible(*state,state->me,d->x,d->y));frame->dots.push_back(p);
  if(!p.concealed){int effects=0;for(int k=0;k<Items::COUNT;k++)if(state->items->active(d->team,k))effects|=1<<k;if(state->items->immunity[d->team]>time)effects|=1<<21;armies[d->team].add(d->x+.5f,d->y+.5f,effects);}
 }
 for(const auto& site:state->operations->sites){float record[]={float(site.x)+.5f,float(site.y)+.5f,float(site.owner),float(site.progress),float(site.contested),float(site.born),float(state->operations->clock-site.pulseAt)};frame->sites.insert(frame->sites.end(),record,record+7);}
 if(state->expedition){auto& e=*state->expedition;
  auto prop=[&](int type,int id,int x,int y,float scale,int owner,float status,float progress){float record[]={float(type),float(id),float(x)+.5f,float(y)+.5f,scale,float(owner),status,progress};frame->props.insert(frame->props.end(),record,record+8);};
  for(const auto& p:e.loot){int eater=p.eater;if(eater!=state->me&&(eater<0||!e.enemyVisible(*state,state->me,p.x,p.y)))eater=-1;prop(p.kind==2?8:p.kind<2?p.kind:20+p.kind-3,p.id,p.x,p.y,p.kind==2?3.f:3.6f,eater,p.eater>=0?1:0,p.hp/float(Items::PICKUP_HP));}
  for(auto o:e.obstacles)if(!o.removed){bool visible=e.obstacleVisible(*state,state->me,o);prop(o.id%2?13:14,o.id,o.x,o.y,4.f,-1,visible&&o.wardUntil>e.clock?1:0,visible?o.hp/120.f:1.f);}
  for(size_t i=0;i<state->operations->sites.size();i++){auto p=state->operations->sites[i];prop(11+i,-1-int(i),p.x,p.y,7.f,p.owner,float(p.contested),p.progress/4000.f);}
 }
 if(state->expedition)appendArmyVfx(*frame,armies,frame->hud->facts[3]);
 {std::lock_guard<std::mutex> lock(presentationMutex);presentation=std::move(frame);}
}
// Packed header(16), MultiMesh transforms/color(16 per live visible dot), sites(8).
// All data is sampled under nativeStateMutex. No Godot callback mutates rules.
std::vector<float> godotFrame(float x,float y,float w,float h,bool spectator){
 const double requested=now();std::shared_ptr<const PresentedState> frame;
 {std::lock_guard<std::mutex> lock(presentationMutex);frame=presentation;}
 const double locked=now();if(!frame)return {};
 if(heights.empty()){
  int n=0;auto raw=acAssets->getFile("garden3d/height.bin",&n);
  if(!raw||n!=12+ARENA_WIDTH*ARENA_HEIGHT*4){free(raw);return {};}
  heights.resize(ARENA_WIDTH*ARENA_HEIGHT);std::memcpy(heights.data(),raw+12,heights.size()*4);free(raw);
 }
 const double start=now();frameGap=previousFrame?start-previousFrame:0;previousFrame=start;
 float phase=(!frame->remote&&presentationPaused)||frame->warmup?0.f:float(std::min(frame->remote?70.:7.,std::max(0.,start-frame->sampled)));
 envTime=std::max(0.0,(frame->clock+phase-(frame->remote?70:0))/1000.);
 std::vector<float> out(16,0);out.reserve(16+frame->dots.size()*16+frame->sites.size()/7*8);
 out[1]=float(envTime);out[2]=float(frame->epoch);out[3]=frame->sites.size()/7;out[11]=float(locked-requested);out[13]=float(frame->own);out[15]=float(frame->remote?frame->total:frame->dots.size());out[14]=float(frame->props.size()/8);
 out[4]=x;out[5]=y;out[6]=w;out[7]=h;out[8]=1;
 visibleCount=0;
 for(const auto& d:frame->dots){
  Point p{d.a.x+(d.b.x-d.a.x)*phase/7,d.a.y+(d.b.y-d.a.y)*phase/7,0};
  if(frame->remote){int i=std::min(9,int(phase/7));float f=(phase-i*7)/7;p={d.path[i].x+(d.path[i+1].x-d.path[i].x)*f,d.path[i].y+(d.path[i+1].y-d.path[i].y)*f,0};}
  if(p.x<x-3||p.y<y-3||p.x>x+w+3||p.y>y+h+3||(!spectator&&d.concealed))continue;
  const float r=d.r,jx=d.jx,jy=d.jy;
  const float record[]={r,0,0,p.x+jx, 0,r*.88f,0,elevation(p.x+jx,p.y+jy)+.13f, 0,0,r,-p.y-jy, d.color[0],d.color[1],d.color[2],d.code};
  out.insert(out.end(),record,record+16);visibleCount++;
 }
 out[0]=visibleCount;
 for(size_t i=0;i<frame->sites.size();i+=7){const auto* site=frame->sites.data()+i;
  const float record[]={site[0],elevation(site[0],site[1]),-site[1],site[2],site[3],site[4],site[5],site[6]};
  out.insert(out.end(),record,record+8);
 }
 for(size_t i=0;i<frame->props.size();i+=8){const float* p=frame->props.data()+i;const float record[]={p[0],p[1],p[2],elevation(p[2],p[3]),-p[3],p[4],p[5],p[6],p[7],0};out.insert(out.end(),record,record+10);}
 ready=1;frameCpu=now()-start;draws++;totalCpu+=frameCpu;maxCpu=std::max(maxCpu,frameCpu);out[12]=float(frameCpu);
 return out;
}

std::vector<int> networkFrame(int viewer){
 if(!state||!state->expedition||viewer<0||viewer>=state->teamCount)return {};
 auto& s=*state;auto& e=*s.expedition;
 auto ops=s.operations->snapshot(s,viewer,false),exp=e.snapshot(s,viewer);
 int original=s.me;s.me=viewer;auto mini=minimap_sight::snapshot(s,false);s.me=original;
 // Header + three bounded integer schemas + only normally visible real units.
 std::vector<int> out{0x193a193b,193,e.clock,viewer,s.teamCount,int(s.dots.size()),s.items->epoch,s.birthX[viewer],s.birthY[viewer],int(ops.size()),int(exp.size()),int(mini.size()),0};
 for(const auto* data:{&ops,&exp,&mini})out.insert(out.end(),data->begin(),data->end());
 int count=0;
 for(auto d:s.dots){if(s.items->concealedFrom(s,viewer,d->team)||(d->team!=viewer&&!e.enemyVisible(s,viewer,d->x,d->y)))continue;count++;
  out.push_back(int(d->visualId));out.push_back(int(d->visualId>>32));out.push_back(d->team);int effects=0;for(int k=0;k<Items::COUNT;k++)if(s.items->active(d->team,k))effects|=1<<k;if(s.items->immunity[d->team]>e.clock)effects|=1<<21;out.push_back(d->health|(effects<<8));
  for(int i=0;i<=10;i++){auto p=position(*d,float(e.clock-70+i*7));int x=int(p.x*64),y=int(p.y*64);out.push_back(x);out.push_back(y);}
 }
 out[12]=count;return out;
}
void beginRemote(int viewer,int seats){
 resetVisual();presentationActive=true;auto frame=std::make_shared<PresentedState>();frame->remote=true;frame->viewer=viewer;frame->seats=seats;frame->birthX=ARENA_WIDTH/2;frame->birthY=ARENA_HEIGHT/2;frame->clock=-1;
 std::lock_guard<std::mutex> lock(presentationMutex);presentation=frame;
}
bool remoteScene(std::vector<int>& out){
 std::lock_guard<std::mutex> lock(presentationMutex);if(!presentation||!presentation->remote)return false;
 out={ARENA_WIDTH,ARENA_HEIGHT,MAP_ID,0,presentation->birthX,presentation->birthY,0,0,0,0,0};return true;
}
bool acceptNetworkFrame(const std::vector<int>& in){
 if(in.size()<13||in[0]!=0x193a193b||in[1]!=193||in[2]<0||in[3]<0||in[4]<2||in[4]>10||in[3]>=in[4]||in[5]<0||in[5]>20000||in[12]<0||in[12]>20000)return false;
 for(int k:{9,10,11})if(in[k]<0||in[k]>100000)return false;
 size_t tail=13u+in[9]+in[10]+in[11];if(tail+size_t(in[12])*26!=in.size())return false;
 auto frame=std::make_shared<PresentedState>();frame->remote=true;frame->clock=in[2];frame->viewer=in[3];frame->seats=in[4];frame->epoch=in[6];frame->birthX=in[7];frame->birthY=in[8];frame->sampled=now();
 auto hud=std::make_shared<PresentedHud>();hud->clock=in[2];size_t cursor=13;
 for(int index:{1,3,2}){int length=in[index==1?9:index==3?10:11];hud->facts[index].assign(in.begin()+cursor,in.begin()+cursor+length);cursor+=length;}
 auto& ops=hud->facts[1];auto& exp=hud->facts[3];auto& mini=hud->facts[2];
 if(ops.size()<size_t(7+in[4])||ops[0]!=0x4c574f32||ops[4]!=in[4]||ops[5]!=2||exp.size()<size_t(11+in[4]*63)||exp[0]!=193||exp[4]!=in[4]||mini.size()<5||mini[0]!=0x4c575349||mini[4]<0||mini.size()!=size_t(5+mini[4]*4))return false;
 for(int k:{7,8,9,10})if(exp[k]<0||exp[k]>(k==7||k==8?40:10))return false;
 if(exp.size()!=size_t(11+in[4]*63+exp[7]*5+exp[8]*8+(exp[9]+exp[10])*4))return false;
 size_t opAt=7+in[4]+16;
 for(int t=0;t<in[4];t++){opAt+=4;for(int g=0;g<3;g++){if(opAt+8>ops.size()||ops[opAt+7]<0||ops[opAt+7]>24)return false;opAt+=8+ops[opAt+7]*2;}}
 if(opAt!=ops.size())return false;
 frame->own=exp[11+in[3]*63];frame->total=in[5];frame->hud=hud;
 auto prop=[&](int kind,int id,int x,int y,float scale,int owner,float status,float progress){const float p[]={float(kind),float(id),float(x)+.5f,float(y)+.5f,scale,float(owner),status,progress};frame->props.insert(frame->props.end(),p,p+8);};
 for(int i=0;i<2;i++){int at=7+in[4]+i*8;const float p[]={float(ops[at])+.5f,float(ops[at+1])+.5f,float(ops[at+2]),float(ops[at+4]),float(ops[at+5]),float(ops[at+6]),float(in[2]-ops[at+7])};frame->sites.insert(frame->sites.end(),p,p+7);prop(11+i,-1-i,ops[at],ops[at+1],7,ops[at+2],ops[at+5],ops[at+4]/4000.f);}
 int lootAt=11+in[4]*63;
 for(int i=0;i<exp[7];i++){int b=lootAt+i*5;if(exp[b+1]<0||exp[b+1]>22)return false;
  int hp=Items::PICKUP_HP,eater=-1,active=0,base=11+(i/4)*63,index=i%4;
  if(exp[base+42]==0x4c485031){int packed=exp[base+13+index];hp=packed&2047;eater=((packed>>11)&15)-1;active=(packed>>15)&1;if(exp[base+21+index]!=exp[b]||packed<0||packed>65535||hp>Items::PICKUP_HP||eater>=in[4])return false;}
  prop(exp[b+1]==2?8:exp[b+1]<2?exp[b+1]:20+exp[b+1]-3,exp[b],exp[b+2],exp[b+3],exp[b+1]==2?3.f:3.6f,eater,active,hp/float(Items::PICKUP_HP));}
 for(int i=0;i<exp[8];i++){int b=lootAt+exp[7]*5+i*8;if(!exp[b+7])prop(exp[b]%2?13:14,exp[b],exp[b+1],exp[b+2],4,-1,exp[b+5]>in[2]?1:0,exp[b+4]/120.f);}
 std::array<ArmyVfx,MAX_TEAMS> armies{};frame->dots.reserve(in[12]);
 for(int n=0;n<in[12];n++){int at=int(tail)+n*26,team=in[at+2],health=in[at+3]&255,effects=in[at+3]>>8;if(team<0||team>=in[4]||health<1||health>10)return false;uint64_t id=uint32_t(in[at])|(uint64_t(uint32_t(in[at+1]))<<32),hash=id*0x9e3779b97f4a7c15ull;PresentedDot dot;
  dot.r=.44f+float((hash>>48)&255)*(.01f/255);dot.jx=(int((hash>>32)&255)-127)*(.035f/128);dot.jy=(int((hash>>40)&255)-127)*(.035f/128);for(auto& c:dot.color)c=visual::healthLevel(health/float(MAX_HEALTH));
  if(effects&(1<<Items::FREEZE)){dot.color[0]=105/255.f;dot.color[1]=205/255.f;dot.color[2]=1;}
  if(effects&(1<<Items::PHASE)){dot.color[0]=210/255.f;dot.color[1]=170/255.f;dot.color[2]=1;}
  if(effects&(1<<Items::INVINCIBLE)){dot.color[0]=1;dot.color[1]=245/255.f;dot.color[2]=155/255.f;}
  dot.code=(team+(effects&(1<<Items::INVISIBLE)?.144f:.32f))/16.f;
  for(int i=0;i<=10;i++){int x=in[at+4+i*2],y=in[at+5+i*2];if(x<0||y<0||x>=ARENA_WIDTH*64||y>=ARENA_HEIGHT*64)return false;dot.path[i]={x/64.f,y/64.f,0};}
  frame->dots.push_back(dot);armies[team].add(dot.path[10].x,dot.path[10].y,effects);
 }
 appendArmyVfx(*frame,armies,exp);
 std::lock_guard<std::mutex> lock(presentationMutex);
 if(!presentation||!presentation->remote||presentation->viewer!=in[3]||presentation->seats!=in[4]||(presentation->epoch==in[6]&&presentation->clock>=in[2]))return false;
 presentation=std::move(frame);return true;
}
void pauseVisual(bool p){paused=p;presentationPaused=p;history.clear();sampledAt=now();previousFrame=0;if(state&&state->garden3D)sampleStep();}
int status(){return ready;}
void diagnostics(double* out){out[0]=ready;out[1]=draws;out[2]=frameCpu;out[3]=frameGap;out[4]=envTime;out[5]=visibleCount;out[6]=triangles;out[7]=history.size();out[8]=draws?totalCpu/draws:0;out[9]=maxCpu;out[10]=resets;out[11]=2;}
void surface(){
 ready=0;meshes.clear();heights.clear();program=shadowFbo=shadowTex=shadowDepth=dynamicVbo=sphereEbo=0;triangles=0;history.clear();while(glGetError()!=GL_NO_ERROR){}
 int n=0;auto raw=acAssets->getFile("garden3d/height.bin",&n);if(!raw||n!=12+ARENA_WIDTH*ARENA_HEIGHT*4){free(raw);ready=-1;return;}heights.resize(ARENA_WIDTH*ARENA_HEIGHT);std::memcpy(heights.data(),raw+12,heights.size()*4);free(raw);
 raw=acAssets->getFile("garden3d/live/river.bin",&n);if(!raw||n!=28){free(raw);ready=-2;return;}std::memcpy(river,raw+4,24);free(raw);
 GLuint vs=shader(GL_VERTEX_SHADER,"garden3d/live/world.vert"),fs=shader(GL_FRAGMENT_SHADER,"garden3d/live/world.frag");if(!vs||!fs){ready=-3;return;}
 program=glCreateProgram();glAttachShader(program,vs);glAttachShader(program,fs);const char* names[]={"aPos","aNormal","aColor","aMaterial"};for(int i=0;i<4;i++)glBindAttribLocation(program,i,names[i]);glLinkProgram(program);glDeleteShader(vs);glDeleteShader(fs);GLint ok=0;glGetProgramiv(program,GL_LINK_STATUS,&ok);
 if(!ok){char log[2048];glGetProgramInfoLog(program,sizeof(log),nullptr,log);__android_log_print(ANDROID_LOG_ERROR,"LWGarden3D","link %s",log);ready=-4;return;}
 glUseProgram(program);viewLoc=glGetUniformLocation(program,"uView");shadowViewLoc=glGetUniformLocation(program,"uShadowView");timeLoc=glGetUniformLocation(program,"uTime");passLoc=glGetUniformLocation(program,"uShadowPass");kindLoc=glGetUniformLocation(program,"uKind");
 glUniform4f(glGetUniformLocation(program,"uRiver"),river[0],river[1],river[2],river[3]);glUniform2f(glGetUniformLocation(program,"uOffset"),river[4],river[5]);glUniform1i(glGetUniformLocation(program,"uShadow"),0);glUniform1f(glGetUniformLocation(program,"uShadowTexel"),1.f/SHADOW);
 raw=acAssets->getFile("garden3d/live/meshes.bin",&n);if(!raw||n<8||std::memcmp(raw,"LWD2",4)){free(raw);ready=-5;return;}uint32_t count=0;std::memcpy(&count,raw+4,4);size_t at=8;if(count>512){free(raw);ready=-6;return;}
 for(unsigned i=0;i<count;i++){
  if(at+32>size_t(n)){ready=-7;break;}Mesh m;std::memcpy(&m.count,raw+at,4);std::memcpy(&m.kind,raw+at+4,4);std::memcpy(m.lo,raw+at+8,12);std::memcpy(m.hi,raw+at+20,12);at+=32;
  if(m.count<=0||at+size_t(m.count)*sizeof(V)>size_t(n)){ready=-8;break;}glGenBuffers(1,&m.vbo);glBindBuffer(GL_ARRAY_BUFFER,m.vbo);glBufferData(GL_ARRAY_BUFFER,m.count*sizeof(V),raw+at,GL_STATIC_DRAW);at+=m.count*sizeof(V);triangles+=m.count/3;meshes.push_back(m);
 }free(raw);if(ready<0)return;glGenBuffers(1,&dynamicVbo);makeSphere();
 glGenTextures(1,&shadowTex);glActiveTexture(GL_TEXTURE0);glBindTexture(GL_TEXTURE_2D,shadowTex);glTexImage2D(GL_TEXTURE_2D,0,GL_RGBA,SHADOW,SHADOW,0,GL_RGBA,GL_UNSIGNED_BYTE,nullptr);glTexParameteri(GL_TEXTURE_2D,GL_TEXTURE_MIN_FILTER,GL_NEAREST);glTexParameteri(GL_TEXTURE_2D,GL_TEXTURE_MAG_FILTER,GL_NEAREST);glTexParameteri(GL_TEXTURE_2D,GL_TEXTURE_WRAP_S,GL_CLAMP_TO_EDGE);glTexParameteri(GL_TEXTURE_2D,GL_TEXTURE_WRAP_T,GL_CLAMP_TO_EDGE);
 glGenRenderbuffers(1,&shadowDepth);glBindRenderbuffer(GL_RENDERBUFFER,shadowDepth);glRenderbufferStorage(GL_RENDERBUFFER,GL_DEPTH_COMPONENT16,SHADOW,SHADOW);glGenFramebuffers(1,&shadowFbo);glBindFramebuffer(GL_FRAMEBUFFER,shadowFbo);glFramebufferTexture2D(GL_FRAMEBUFFER,GL_COLOR_ATTACHMENT0,GL_TEXTURE_2D,shadowTex,0);glFramebufferRenderbuffer(GL_FRAMEBUFFER,GL_DEPTH_ATTACHMENT,GL_RENDERBUFFER,shadowDepth);
 bool complete=glCheckFramebufferStatus(GL_FRAMEBUFFER)==GL_FRAMEBUFFER_COMPLETE;glBindFramebuffer(GL_FRAMEBUFFER,0);auto error=glGetError();ready=complete&&error==GL_NO_ERROR?1:-9;
 __android_log_print(ANDROID_LOG_INFO,"LWGarden3D","LIVE GLES=%s GPU=%s ready=%d triangles=%d chunks=%zu GL=%u",glGetString(GL_VERSION),glGetString(GL_RENDERER),ready,triangles,meshes.size(),error);
}
void draw(float x,float y,float w,float h,bool spectator){
 const double start=now();frameGap=previousFrame?start-previousFrame:0;previousFrame=start;if(ready!=1)return;int time=state->items->elapsed;
 envTime=(time+(paused||state->items->warmup>0?0:std::min(7.,std::max(0.,start-sampledAt))))/1000.;
 glUseProgram(program);glEnable(GL_DEPTH_TEST);glDepthFunc(GL_LEQUAL);glDepthMask(GL_TRUE);glDisable(GL_CULL_FACE);glDisable(GL_BLEND);glUniform4f(viewLoc,x,y,w,h);glUniform4f(shadowViewLoc,x-100,y-100,w+200,h+200);glUniform1f(timeLoc,float(envTime));
 glActiveTexture(GL_TEXTURE0);glBindTexture(GL_TEXTURE_2D,0);glUniform1f(passLoc,1);glBindFramebuffer(GL_FRAMEBUFFER,shadowFbo);glViewport(0,0,SHADOW,SHADOW);glClearColor(1,1,1,1);glClear(GL_COLOR_BUFFER_BIT|GL_DEPTH_BUFFER_BIT);geometry(x,y,w,h,true);sites();if(w<1000)particles(x,y,w,h,spectator);
 glBindFramebuffer(GL_FRAMEBUFFER,0);glViewport(0,0,state->displayWidth,state->displayHeight);glBindTexture(GL_TEXTURE_2D,shadowTex);glUniform1f(passLoc,0);glClearColor(.22f,.28f,.27f,1);glClear(GL_COLOR_BUFFER_BIT|GL_DEPTH_BUFFER_BIT);geometry(x,y,w,h,false);sites();particles(x,y,w,h,spectator);
 for(int a=0;a<4;a++)glDisableVertexAttribArray(a);glBindBuffer(GL_ARRAY_BUFFER,0);glDisable(GL_DEPTH_TEST);GLenum error=glGetError();if(error){ready=-int(error);__android_log_print(ANDROID_LOG_ERROR,"LWGarden3D","live draw GL=%u",error);}frameCpu=now()-start;draws++;totalCpu+=frameCpu;maxCpu=std::max(maxCpu,frameCpu);
}
void rebuildSurface(){
 for(auto& m:meshes)if(m.vbo)glDeleteBuffers(1,&m.vbo);
 if(dynamicVbo)glDeleteBuffers(1,&dynamicVbo);
 if(sphereEbo)glDeleteBuffers(1,&sphereEbo);
 if(shadowFbo)glDeleteFramebuffers(1,&shadowFbo);
 if(shadowDepth)glDeleteRenderbuffers(1,&shadowDepth);
 if(shadowTex)glDeleteTextures(1,&shadowTex);
 if(program)glDeleteProgram(program);
 surface();
}
}
