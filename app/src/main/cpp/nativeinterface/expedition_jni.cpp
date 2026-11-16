// SPDX-License-Identifier: GPL-3.0-or-later
#include "state_guard.hpp"
#include "lifecycle.hpp"
#include "ai_targets.hpp"
#include "expedition_jni.hpp"
#include "../application/state.hpp"
#include "../application/expedition.hpp"
#include "../application/garden3d.hpp"
#include <deque>
namespace {
struct Command {int actor,serial,kind,group,x,y;};
std::mutex commandMutex;
std::deque<Command> commands;
}
void clearExpeditionCommands(){std::lock_guard<std::mutex> lock(commandMutex);commands.clear();}
void drainExpeditionCommands(){
 std::deque<Command> next;{std::lock_guard<std::mutex> lock(commandMutex);next.swap(commands);}
 if(!state||!state->expedition)return;
 for(auto c:next){
  if(c.serial<0)c.serial=state->expedition->seats[c.actor].lastCommand+1;
  if(c.kind==100)state->expedition->purchase(*state,c.actor,c.serial,c.x);
  else state->expedition->command(*state,c.actor,c.serial,c.kind,c.group,c.x,c.y);
 }
}
extern "C" void Java_com_xenris_liquidwarsos_NativeInterface_createExpeditionProxy(JNIEnv*,jclass,jint team,jint seats){
 std::lock_guard<std::mutex> lock(nativeStateMutex);if(state||team<0||team>=seats||seats<2||seats>10)return;garden3d::beginRemote(team,seats);
}
extern "C" jintArray Java_com_xenris_liquidwarsos_NativeInterface_expeditionWire(JNIEnv* env,jclass,jint viewer){
 std::vector<int> data;{std::lock_guard<std::mutex> lock(nativeStateMutex);data=garden3d::networkFrame(viewer);}
 auto out=env->NewIntArray(data.size());if(out)env->SetIntArrayRegion(out,0,data.size(),data.data());return out;
}
extern "C" jboolean Java_com_xenris_liquidwarsos_NativeInterface_expeditionReceive(JNIEnv* env,jclass,jintArray packet){
 int size=env->GetArrayLength(packet);if(size<13||size>1000000)return false;std::vector<int> values(size);env->GetIntArrayRegion(packet,0,size,values.data());return garden3d::acceptNetworkFrame(values);
}
extern "C" void Java_com_xenris_liquidwarsos_NativeInterface_expeditionBot(JNIEnv*,jclass,jint team,jboolean bot){
 std::lock_guard<std::mutex> lock(nativeStateMutex);if(state&&state->expedition&&team>=0&&team<state->teamCount){state->expedition->seats[team].bot=bot;if(!bot)state->operations->clearTeam(*state,team);}
}
extern "C" void Java_com_xenris_liquidwarsos_NativeInterface_expeditionRemoteCommand(JNIEnv* env,jclass,jint actor,jint kind,jint group,jint x,jint y,jshortArray jx,jshortArray jy){
 std::lock_guard<std::mutex> lock(nativeStateMutex);if(!state||!state->expedition||actor<=0||actor>=state->teamCount||state->expedition->seats[actor].bot)return;
 if(kind==-1){if(env->GetArrayLength(jx)!=5||env->GetArrayLength(jy)!=5)return;short xs[5],ys[5];env->GetShortArrayRegion(jx,0,5,xs);env->GetShortArrayRegion(jy,0,5,ys);setPlayerPosition(actor,xs,ys);}
 else{auto& e=*state->expedition;int serial=e.seats[actor].lastCommand+1;if(kind==100)e.purchase(*state,actor,serial,x);else e.command(*state,actor,serial,kind,group,x,y);}
}
extern "C" jboolean Java_com_xenris_liquidwarsos_NativeInterface_createExpeditionGame(JNIEnv*,jclass,jint team,jint seats,jint seed){
 std::lock_guard<std::mutex> lock(nativeStateMutex);
 if(state||team<0||seats<2||seats>MAX_TEAMS||team>=seats||!garden3d::assetsValid())return false;
 clearExpeditionCommands();resetAiTargets();
 state=new State(team,garden3d::MAP_ID,seed,garden3d::START,true,seats);
 if(state->dots.size()!=size_t(seats*garden3d::START)||state->operations->sites.size()!=2){delete state;state=nullptr;return false;}
 garden3d::beginMatch();garden3d::sampleStep();return true;
}
extern "C" jintArray Java_com_xenris_liquidwarsos_NativeInterface_expeditionSnapshot(JNIEnv* env,jclass){
 std::vector<int> values;garden3d::hudSnapshot(3,values);
 auto out=env->NewIntArray(values.size());if(out)env->SetIntArrayRegion(out,0,values.size(),values.data());return out;
}
extern "C" jboolean Java_com_xenris_liquidwarsos_NativeInterface_expeditionCommand(JNIEnv*,jclass,jint actor,jint serial,jint kind,jint group,jint x,jint y){
 if(actor<0||actor>=MAX_TEAMS||serial<=0)return false;
 std::lock_guard<std::mutex> lock(commandMutex);
 if(commands.size()>=64)return false;
 commands.push_back({actor,serial,kind,group,x,y});return true;
}
