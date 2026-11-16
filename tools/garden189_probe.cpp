// Test APK only. Controlled locations, real 1000/1000 armies and production rules.
// Never linked into the delivered game APK.
#include "nativeinterface/state_guard.hpp"
#include "application/state.hpp"
#include "application/garden3d.hpp"
#include <jni.h>
#include <cmath>
extern "C" JNIEXPORT jintArray JNICALL Java_com_xenris_liquidwarsos_Garden189Probe_fixture(JNIEnv* env,jclass,jint kind){
 std::lock_guard<std::mutex> guard(nativeStateMutex);if(!state||!state->garden3D)return nullptr;auto& s=*state;
 int x=2105,y=1092;if(kind==1){x=s.operations->sites[2].x;y=s.operations->sites[2].y;}
 auto clear=[&](int a,int b){for(int j=-20;j<=20;j++)for(int i=-20;i<=20;i++)if(i*i+j*j<=400&&s.map->isWall(a+i,b+j))return false;return true;};
 if(!clear(x,y)){bool found=false;for(int r=1;r<180&&!found;r++)for(int a=0;a<16&&!found;a++){int xx=x+int(std::cos(a*3.14159265/8)*r),yy=y+int(std::sin(a*3.14159265/8)*r);if(clear(xx,yy)){x=xx;y=yy;found=true;}}if(!found)return nullptr;}
 int ex=2375,ey=1092;if(kind==1){ex=2800;ey=1100;}if(!clear(ex,ey))return nullptr;
 for(auto d:s.dots)delete d;s.dots.clear();s.field.clear();for(auto& p:s.players)p=Player{};
 std::vector<std::pair<int,int>> offsets;for(int j=-19;j<=19;j++)for(int i=-19;i<=19;i++)if(i*i+j*j<=361)offsets.push_back({i,j});
 std::stable_sort(offsets.begin(),offsets.end(),[](const std::pair<int,int>& a,const std::pair<int,int>& b){return a.first*a.first+a.second*a.second<b.first*b.first+b.second*b.second;});
 for(int t:{0,2}){int xx=t?ex:x,yy=t?ey:y;s.birthX[t]=xx;s.birthY[t]=yy;s.players[t].x[0]=xx;s.players[t].y[0]=yy;
  for(int i=0;i<1000;i++){auto d=new Dot(xx+offsets[i].first,yy+offsets[i].second,t);s.dots.push_back(d);s.field[d->x][d->y]=d;s.players[t].score++;}}
 s.points.resize(s.dots.size()*3);s.colours.resize(s.dots.size()*4);s.items->elapsed=0;s.items->warmup=0;s.items->epoch++;s.operations->reset(s);garden3d::resetVisual();garden3d::sampleStep();
 jint p[]={x,y,ex,ey};auto out=env->NewIntArray(4);env->SetIntArrayRegion(out,0,4,p);return out;
}
extern "C" JNIEXPORT void JNICALL Java_com_xenris_liquidwarsos_Garden189Probe_rewind(JNIEnv*,jclass){std::lock_guard<std::mutex> g(nativeStateMutex);if(state&&state->garden3D){state->items->rewind(*state);garden3d::sampleStep();}}
extern "C" JNIEXPORT void JNICALL Java_com_xenris_liquidwarsos_Garden189Probe_surface(JNIEnv*,jclass){std::lock_guard<std::mutex> g(nativeStateMutex);garden3d::rebuildSurface();}
extern "C" JNIEXPORT jint JNICALL Java_com_xenris_liquidwarsos_Garden189Probe_growth(JNIEnv*,jclass){std::lock_guard<std::mutex> g(nativeStateMutex);if(!state||!state->garden3D)return 0;state->items->grow(*state,0,2000);state->items->grow(*state,2,2000);garden3d::sampleStep();return state->dots.size();}
