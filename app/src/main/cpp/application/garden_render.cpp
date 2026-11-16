// SPDX-License-Identifier: GPL-3.0-or-later
// Real static 3D meshes with baked final-color light, plus dynamic sphere meshes.
// Fixed vertical camera. Simulation and visibility remain authoritative in State.
#include "garden3d.hpp"
#include "state.hpp"
#include "visual_texture.hpp"
#include <android/log.h>
#include <cmath>
#include <cstring>
#include <algorithm>
#include <cstdlib>
#include <string>
namespace garden3d {
namespace {
GLuint shadow=0;
struct Tile {GLuint texture=0,buffer=0;int count=0;};
std::array<Tile,8> tiles{};
std::vector<float> heights;
int ready=0;
constexpr float VISUAL_W=5333.333333f,VISUAL_H=2666.666667f;
constexpr int BATCH=1800,SEG=8,RINGS=3,SPHERE=26;
struct V {float x,y,z;};
std::vector<V> unit,positions,shadowPositions;
std::vector<GLubyte> colours;
std::vector<GLushort> indices,shadowIndices;
std::vector<float> shadowUV;
std::vector<Dot*> visible;
float elevation(int x,int y){return heights.size()==size_t(ARENA_WIDTH*ARENA_HEIGHT)?heights[std::max(0,std::min(ARENA_HEIGHT-1,y))*ARENA_WIDTH+std::max(0,std::min(ARENA_WIDTH-1,x))]:0;}
void palette(int t,float* c){static const float rgb[6][3]={{.02f,.68f,.54f},{.25f,.55f,.9f},{.94f,.36f,.23f},{.92f,.88f,.75f},{.93f,.67f,.15f},{.62f,.34f,.76f}};for(int i=0;i<3;i++)c[i]=rgb[std::max(0,std::min(5,t))][i];}
void makeSphere(){
 unit.clear();unit.push_back({0,0,1});
 for(int ring=1;ring<=RINGS;ring++){float a=float(M_PI)*ring/4;for(int k=0;k<SEG;k++){float b=float(M_PI)*2*k/SEG;unit.push_back({std::sin(a)*std::cos(b),std::sin(a)*std::sin(b),std::cos(a)});}}
 unit.push_back({0,0,-1});std::vector<GLushort> one;
 auto face=[&](int a,int b,int c){one.push_back(GLushort(a));one.push_back(GLushort(b));one.push_back(GLushort(c));};
 for(int k=0;k<SEG;k++){int j=(k+1)%SEG;face(0,1+k,1+j);face(25,17+j,17+k);for(int r=0;r<2;r++){int a=1+r*SEG+k,b=1+r*SEG+j;face(a,a+SEG,b+SEG);face(a,b+SEG,b);}}
 positions.resize(BATCH*SPHERE);colours.resize(BATCH*SPHERE*4);indices.clear();indices.reserve(BATCH*one.size());
 shadowPositions.resize(BATCH*4);shadowIndices.resize(BATCH*6);shadowUV.resize(BATCH*8);
 for(int i=0;i<BATCH;i++){
  for(auto n:one)indices.push_back(GLushort(i*SPHERE+n));
  const unsigned six[]={0,1,2,0,2,3};for(int k=0;k<6;k++)shadowIndices[i*6+k]=GLushort(i*4+six[k]);
  const float uv[]={0,0,1,0,1,1,0,1};std::memcpy(shadowUV.data()+i*8,uv,sizeof(uv));
 }
}
void particleBatch(size_t start,size_t count){
 for(size_t i=0;i<count;i++){
  const Dot* d=visible[start+i];float x=d->x+.5f,y=d->y+.5f;
  // Radius tracks local free space, keeping dense fighting units separated.
  bool packed=false;for(int dx=-1;dx<=1;dx++)for(int dy=-1;dy<=1;dy++)if((dx||dy)&&x+dx>=0&&y+dy>=0&&x+dx<ARENA_WIDTH&&y+dy<ARENA_HEIGHT&&state->field[d->x+dx][d->y+dy])packed=true;
  const float r=packed?.52f:1.30f,z=elevation(d->x,d->y)+r*.83f;
  float base[3];palette(d->team,base);float health=.7f+.3f*std::min(1.f,d->health/float(MAX_HEALTH));
  for(int k=0;k<SPHERE;k++){
   const auto n=unit[k];positions[i*SPHERE+k]={x+n.x*r,y+n.y*r,z+n.z*r*.82f};
   const float lambert=std::max(0.f,-.35f*n.x+.48f*n.y+.805f*n.z);
   const float spec=std::pow(std::max(0.f,-.19f*n.x+.26f*n.y+.947f*n.z),18.f)*.15f;
   for(int c=0;c<3;c++)colours[(i*SPHERE+k)*4+c]=GLubyte(std::min(255.f,255*(base[c]*(.60f+.38f*lambert)*health+spec)));
   colours[(i*SPHERE+k)*4+3]=255;
  }
  float sr=r*1.35f,sz=elevation(d->x,d->y)+.035f;
  shadowPositions[i*4]={x-sr+.25f,y-sr-.25f,sz};shadowPositions[i*4+1]={x+sr+.25f,y-sr-.25f,sz};
  shadowPositions[i*4+2]={x+sr+.25f,y+sr-.25f,sz};shadowPositions[i*4+3]={x-sr+.25f,y+sr-.25f,sz};
 }
 glEnable(GL_BLEND);glBlendFunc(GL_SRC_ALPHA,GL_ONE_MINUS_SRC_ALPHA);glDepthMask(GL_FALSE);
 glEnable(GL_TEXTURE_2D);glBindTexture(GL_TEXTURE_2D,shadow);glEnableClientState(GL_TEXTURE_COORD_ARRAY);
 glColor4f(1,1,1,1);glVertexPointer(3,GL_FLOAT,0,shadowPositions.data());glTexCoordPointer(2,GL_FLOAT,0,shadowUV.data());
 glDrawElements(GL_TRIANGLES,GLsizei(count*6),GL_UNSIGNED_SHORT,shadowIndices.data());
 glDisableClientState(GL_TEXTURE_COORD_ARRAY);glDisable(GL_TEXTURE_2D);glDepthMask(GL_TRUE);glDisable(GL_BLEND);
 glEnableClientState(GL_COLOR_ARRAY);glVertexPointer(3,GL_FLOAT,0,positions.data());glColorPointer(4,GL_UNSIGNED_BYTE,0,colours.data());
 glDrawElements(GL_TRIANGLES,GLsizei(count*144),GL_UNSIGNED_SHORT,indices.data());glDisableClientState(GL_COLOR_ARRAY);
}
void sites(){
 for(const auto& site:state->operations->sites){
  float rgb[3]={.40f,.50f,.43f};if(site.owner>=0)palette(site.owner,rgb);
  float z=elevation(site.x,site.y);V cap[11];cap[0]={float(site.x),float(site.y),z+3};
  for(int k=0;k<=9;k++){float a=float(M_PI)*2*k/9;cap[k+1]={site.x+9*std::cos(a),site.y+9*std::sin(a),z+3};}
  glColor4f(rgb[0]*.75f,rgb[1]*.75f,rgb[2]*.75f,1);glVertexPointer(3,GL_FLOAT,0,cap);glDrawArrays(GL_TRIANGLE_FAN,0,11);
  // Three dimensional ceramic petals with sloped shoulders.
  for(int k=0;k<3;k++){float a=float(M_PI)*2*k/3,dx=std::cos(a),dy=std::sin(a);V petal[4]={{site.x+dx*3,site.y+dy*3,z+5},{site.x+dx*10-dy*3,site.y+dy*10+dx*3,z+7},{site.x+dx*10+dy*3,site.y+dy*10-dx*3,z+7},{site.x+dx*12,site.y+dy*12,z+2}};
   const GLushort ix[]={0,1,2,1,3,2};glColor4f(.81f,.81f,.69f,1);glVertexPointer(3,GL_FLOAT,0,petal);glDrawElements(GL_TRIANGLES,6,GL_UNSIGNED_SHORT,ix);}
 }
}
}
int status(){return ready;}
void surface(){
 shadow=0;tiles={};ready=0;heights.clear();int total=0,size=0;
 auto raw=acAssets->getFile("garden3d/height.bin",&size);
 uint32_t hw=0,hh=0;if(raw&&size>=12){std::memcpy(&hw,raw+4,4);std::memcpy(&hh,raw+8,4);}
 if(raw&&hw==ARENA_WIDTH&&hh==ARENA_HEIGHT&&size==12+ARENA_WIDTH*ARENA_HEIGHT*4&&!std::memcmp(raw,"LWH1",4)){heights.resize(ARENA_WIDTH*ARENA_HEIGHT);std::memcpy(heights.data(),raw+12,heights.size()*4);}free(raw);
 if(heights.empty()){ready=-1;return;}
 GLint maximum=0;glGetIntegerv(GL_MAX_TEXTURE_SIZE,&maximum);
 for(int i=0;i<8;i++){
  auto& tile=tiles[i];std::string stem="garden3d/tile"+std::to_string(i);uint32_t count=0;
  raw=acAssets->getFile((stem+".bin").c_str(),&size);
  if(raw&&size>=8&&!std::memcmp(raw,"LWG1",4))std::memcpy(&count,raw+4,4);
  if(!raw||!count||count>600000||size_t(size)!=8+size_t(count)*20){free(raw);ready=-1;return;}
  std::vector<float> terrain(size_t(count)*5);std::memcpy(terrain.data(),raw+8,terrain.size()*4);free(raw);tile.count=int(count);total+=tile.count;
  raw=acAssets->getFile((stem+".png").c_str(),&size);unsigned char* rgba=nullptr;unsigned w=0,h=0;
  unsigned err=raw?lodepng_decode32(&rgba,&w,&h,raw,size):1;free(raw);if(err||!rgba){free(rgba);ready=-1;return;}
  glGenTextures(1,&tile.texture);glBindTexture(GL_TEXTURE_2D,tile.texture);glTexParameteri(GL_TEXTURE_2D,GL_TEXTURE_MIN_FILTER,GL_LINEAR);glTexParameteri(GL_TEXTURE_2D,GL_TEXTURE_MAG_FILTER,GL_LINEAR);glTexParameteri(GL_TEXTURE_2D,GL_TEXTURE_WRAP_S,GL_CLAMP_TO_EDGE);glTexParameteri(GL_TEXTURE_2D,GL_TEXTURE_WRAP_T,GL_CLAMP_TO_EDGE);
  bool uploaded=false;
  for(int attempt=0;attempt<2;attempt++){
   auto tex=visual::prepareMap(rgba,w,h,std::min(attempt?1024:2048,std::max(1,maximum)),1);
   glTexImage2D(GL_TEXTURE_2D,0,GL_RGBA,tex.width,tex.height,0,GL_RGBA,GL_UNSIGNED_BYTE,tex.rgba.data());
   if(glGetError()==GL_NO_ERROR){for(size_t j=0;j<terrain.size();j+=5){terrain[j+3]*=float(tex.contentWidth)/tex.width;terrain[j+4]*=float(tex.contentHeight)/tex.height;}uploaded=true;break;}
  }
  free(rgba);if(!uploaded){ready=-1;return;}
  glGenBuffers(1,&tile.buffer);glBindBuffer(GL_ARRAY_BUFFER,tile.buffer);glBufferData(GL_ARRAY_BUFFER,terrain.size()*4,terrain.data(),GL_STATIC_DRAW);glBindBuffer(GL_ARRAY_BUFFER,0);
 }
 GLubyte pixels[32*32*4];for(int y=0;y<32;y++)for(int x=0;x<32;x++){float q=std::hypot((x-15.5f)/16,(y-15.5f)/16);int i=(y*32+x)*4;pixels[i]=35;pixels[i+1]=55;pixels[i+2]=44;pixels[i+3]=GLubyte(std::max(0.f,1-q)*65);}
 glGenTextures(1,&shadow);glBindTexture(GL_TEXTURE_2D,shadow);glTexParameteri(GL_TEXTURE_2D,GL_TEXTURE_MIN_FILTER,GL_LINEAR);glTexParameteri(GL_TEXTURE_2D,GL_TEXTURE_MAG_FILTER,GL_LINEAR);glTexParameteri(GL_TEXTURE_2D,GL_TEXTURE_WRAP_S,GL_CLAMP_TO_EDGE);glTexParameteri(GL_TEXTURE_2D,GL_TEXTURE_WRAP_T,GL_CLAMP_TO_EDGE);glTexImage2D(GL_TEXTURE_2D,0,GL_RGBA,32,32,0,GL_RGBA,GL_UNSIGNED_BYTE,pixels);
 makeSphere();GLint depth=0;glGetIntegerv(GL_DEPTH_BITS,&depth);GLenum e=glGetError();ready=e==GL_NO_ERROR&&depth>=16?1:-1;
 __android_log_print(ANDROID_LOG_INFO,"LWGarden3D","surface ready=%d triangles=%d depth=%d GL=%u",ready,total/3,depth,e);
}
void draw(float x,float y,float w,float h,bool spectator){
 acLib->glOrthogonal(x,x+w,y,y+h,-250,250);glLoadIdentity();glClearColor(.70f,.74f,.64f,1);glClear(GL_COLOR_BUFFER_BIT|GL_DEPTH_BUFFER_BIT);
 if(ready!=1)return;
 glEnable(GL_DEPTH_TEST);glDepthFunc(GL_LEQUAL);glDepthMask(GL_TRUE);glDisable(GL_CULL_FACE);glDisable(GL_LIGHTING);glDisable(GL_BLEND);
 glEnableClientState(GL_VERTEX_ARRAY);glEnable(GL_TEXTURE_2D);glTexEnvi(GL_TEXTURE_ENV,GL_TEXTURE_ENV_MODE,GL_MODULATE);glColor4f(1,1,1,1);
 glEnableClientState(GL_TEXTURE_COORD_ARRAY);
 for(int i=0;i<8;i++){
  float left=ARENA_WIDTH*.5f-VISUAL_W*.5f+(i%4)*VISUAL_W/4,top=ARENA_HEIGHT*.5f+VISUAL_H*.5f-(i/4)*VISUAL_H/2;
  if(left>x+w||left+VISUAL_W/4<x||top<y||top-VISUAL_H/2>y+h)continue;
  const auto& tile=tiles[i];glBindTexture(GL_TEXTURE_2D,tile.texture);glBindBuffer(GL_ARRAY_BUFFER,tile.buffer);glVertexPointer(3,GL_FLOAT,20,nullptr);glTexCoordPointer(2,GL_FLOAT,20,reinterpret_cast<void*>(12));glDrawArrays(GL_TRIANGLES,0,tile.count);
 }
 glBindBuffer(GL_ARRAY_BUFFER,0);glDisableClientState(GL_TEXTURE_COORD_ARRAY);glDisable(GL_TEXTURE_2D);
 sites();visible.clear();visible.reserve(state->dots.size());
 for(auto d:state->dots)if(d->x>=x-4&&d->y>=y-4&&d->x<=x+w+4&&d->y<=y+h+4&&(spectator||!state->items->concealedFrom(*state,state->me,d->team)))visible.push_back(d);
 for(size_t at=0;at<visible.size();at+=BATCH)particleBatch(at,std::min(size_t(BATCH),visible.size()-at));
 glDisableClientState(GL_VERTEX_ARRAY);glDisable(GL_DEPTH_TEST);glEnable(GL_BLEND);glBlendFunc(GL_SRC_ALPHA,GL_ONE_MINUS_SRC_ALPHA);
 GLenum error=glGetError();if(error){ready=-int(error);__android_log_print(ANDROID_LOG_ERROR,"LWGarden3D","draw GL=%u",error);}
}
}
