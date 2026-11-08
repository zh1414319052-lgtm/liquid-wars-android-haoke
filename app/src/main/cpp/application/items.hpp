// SPDX-License-Identifier: GPL-3.0-or-later
#pragma once
#include <array>
#include <cstdint>
#include <vector>
#include <deque>
#include "dot.hpp"
#include "player.hpp"
class State;
class Items {
public:
 enum Kind { DOUBLE, QUARRY, FREEZE, TELEPORT, RESET, PHASE, INVINCIBLE, DEATH, FRIEND, ANTIGROW, ROCKSEAL, THAW, ANCHOR, CAUSAL, PHASENAIL, SHATTER, WARD, LIBERTY, INVISIBLE, REVEAL, COUNT };
 static constexpr int EFFECT_MS=20000;
 static constexpr int STEP_MS=7,MAX_PARTICLES=108000,BUFF_STRIDE=20;
 static constexpr int PICKUP_RADIUS=8;
 static int radiusForHp(int hp){if(hp<=0)return 0;for(int r=2;r<PICKUP_RADIUS;r++)if(hp*PICKUP_RADIUS*PICKUP_RADIUS<=PICKUP_HP*r*r)return r;return PICKUP_RADIUS;}
 bool blockedByPickup(int x,int y)const;
 static constexpr int PICKUP_HP=1200,MAX_DEVOUR_PER_STEP=4;
 struct Pickup{int kind=-1,x=0,y=0,hp=PICKUP_HP,eater=-1;std::array<int,6> damage{};};
 struct Event{int serial=0,kind=-1,team=-1,x=0,y=0,amount=0,at=0,target=-1,stage=0;};
 int elapsed=0,nextWave=0,wave=0,epoch=0,warmup=0;
 uint32_t randomState;bool enabled=true,resetSpawned=false;
 static constexpr int MAX_PICKUPS=6;
 Pickup pickups[MAX_PICKUPS];
 int truceA=-1,truceB=-1,truceUntil=0;std::array<int,6> quarryBudget{},deathOwner{};
 std::array<std::array<int,COUNT>,6> until{};
 std::array<int,6> pending{},nonce{},attempt{};
 Event last;std::deque<Event> events;
 Items(int seed,int map);
 bool active(int team,int kind)const{return until[team][kind]>elapsed;}
 bool concealedFrom(const State&,int viewer,int team)const;
 bool hasPickups()const{for(const auto& p:pickups)if(p.kind>=0)return true;return false;}
 void begin(State&);void end(State&);
 int moves(const State&,const Dot&)const;
 void contact(State&,Dot&,Dot&);
 void grant(State&,int team,int kind,int x,int y);
 int grow(State&,int team,int count);
 std::vector<int> snapshot(const State&)const;
 bool canEnter(State&,int team,int x,int y)const;
 bool canContact(State&,int team,int x,int y)const;
 bool opposed(const State&,int team,int effect)const;
 bool protectedBy(const State&,int team,int effect)const;
 bool command(State&,int team,int token,int x,int y);
 void capture(State&);void rewind(State&);
 uint32_t next();unsigned bounded(unsigned);
private:
 std::vector<Dot> boot;std::array<Player,6> bootPlayers;std::array<int,6> bootAlliances{};std::array<bool,COUNT> previousKinds{};
 unsigned bootMove=0,bootAI=0,bootItems=0;int bootNext=0;bool captured=false;bool suppressResetWave=false;
 void deathStrike(State&,int target);
 void emit(int kind,int team,int x,int y,int amount=0,int target=-1,int stage=0);
 void spawnPair(State&);void collect(State&);void quarry(State&,int team);
 int relocate(State&,int team,int x,int y);void exitPhase(State&,int team);
};
