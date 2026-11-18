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
 static constexpr int ACTIVE_FREEZE=20,ACTIVE_CLEANSE=21,ACTIVE_BLACK_HOLE=22,ACTIVE_COUNT=3,STOCK_CAP=10;
 static constexpr int BLACK_HOLE_SLOT=2,BLACK_HOLE_MS=10000,BLACK_HOLE_RADIUS=6;
 static constexpr int BLACK_HOLE_WARNING_MS=1500,BLACK_HOLE_RAMP_MS=1000;
 static constexpr int BLACK_HOLE_FACTS=0x4c574248;
 static constexpr int FREEZE_MS=10000,FREEZE_COOLDOWN=30000,CLEANSE_MS=6000,CLEANSE_COOLDOWN=8000,THAW_GRACE=5000;
 std::array<std::array<int,ACTIVE_COUNT>,MAX_TEAMS> stock{},ready{};
 int holeStart=0,holeUntil=0,holeAbsorbed=0,lastHoleAbsorb=-1;
 std::array<bool,MAX_TEAMS> holeExempt{};
 // Read-only presentation counters. Never used by rules or simulation RNG.
 std::array<int,MAX_TEAMS> audioCaptures{};
 std::vector<int> audioSnapshot(const State&,int viewer,bool all=false)const;
 bool blackHoleActive()const{return holeUntil>elapsed;}
 bool pulling(int team)const{return blackHoleActive()&&!holeExempt[team];}
 bool pull(State&,Dot&); // Returns true when the particle entered the core and was removed.
 void appendBlackHoleFacts(std::vector<int>&,int viewer)const;
 std::array<int,MAX_TEAMS> immunity{},activeSerial{},activeResult{};
 std::array<std::deque<int>,MAX_TEAMS> activeSeen;
 bool rewindPending=false;
 bool pickupAllowed(int team,int kind)const{return kind>=0&&(kind<COUNT||(kind<COUNT+ACTIVE_COUNT&&stock[team][kind-COUNT]<STOCK_CAP));}
 int activate(State&,int team,int serial,int kind);
 void freezeEnemies(State&,int team,int duration);
 void thaw(State&,int team,int duration);
 std::vector<int> activeSnapshot(const State&,int viewer=-1)const;
 static constexpr int EFFECT_MS=10000;
 static constexpr int STEP_MS=7,MAX_PARTICLES=108000,BUFF_STRIDE=20;
 static constexpr int PICKUP_RADIUS=8;
 static int radiusForHp(int hp){if(hp<=0)return 0;for(int r=2;r<PICKUP_RADIUS;r++)if(hp*PICKUP_RADIUS*PICKUP_RADIUS<=PICKUP_HP*r*r)return r;return PICKUP_RADIUS;}
 bool blockedByPickup(int x,int y)const;
 static constexpr int PICKUP_HP=1200,MAX_DEVOUR_PER_STEP=4;
 struct Pickup{int kind=-1,x=0,y=0,hp=PICKUP_HP,eater=-1;std::array<int,MAX_TEAMS> damage{};};
 struct Event{int serial=0,kind=-1,team=-1,x=0,y=0,amount=0,at=0,target=-1,stage=0;};
 int elapsed=0,nextWave=0,wave=0,epoch=0,warmup=0;
 uint32_t randomState;bool enabled=true,resetSpawned=false;
 static constexpr int MAX_PICKUPS=6;
 Pickup pickups[MAX_PICKUPS];
 int truceA=-1,truceB=-1,truceUntil=0;std::array<int,MAX_TEAMS> quarryBudget{},deathOwner{};
 std::array<std::array<int,COUNT>,MAX_TEAMS> until{};
 std::array<int,MAX_TEAMS> pending{},nonce{},attempt{};
 Event last;std::deque<Event> events;
 Items(int seed,int map);
 bool active(int team,int kind)const{return until[team][kind]>elapsed;}
 bool concealedFrom(const State&,int viewer,int team)const;
 bool hasPickups()const{for(const auto& p:pickups)if(p.kind>=0)return true;return false;}
 void begin(State&);void end(State&);
 int moves(const State&,Dot&)const;
 void contact(State&,Dot&,Dot&);
 void grant(State&,int team,int kind,int x,int y);
 int devour(State&,Pickup&); // shared timed collection, returns eligible winning team
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
 std::vector<Dot> boot;std::array<Player,MAX_TEAMS> bootPlayers;std::array<int,MAX_TEAMS> bootAlliances{};std::array<bool,COUNT> previousKinds{};
 unsigned bootMove=0,bootAI=0,bootItems=0;int bootNext=0;bool captured=false;bool suppressResetWave=false;
 void deathStrike(State&,int target);
 void resetPullOffsets(State&,int team=-1);
 void emit(int kind,int team,int x,int y,int amount=0,int target=-1,int stage=0);
 void spawnPair(State&);void collect(State&);void quarry(State&,int team);
 int relocate(State&,int team,int x,int y);void exitPhase(State&,int team);
};
