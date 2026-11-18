// SPDX-License-Identifier: GPL-3.0-or-later
#pragma once
#include <array>
#include <vector>
#include <cstdint>
#include "info.hpp"
#include "expedition_balance.hpp"
class State;class Dot;
// New-mode rules live beside the original simulation, never in Godot/Java.
class Expedition {
public:
 enum Item {FREEZE,CLEANSE,BLACK_HOLE,ITEMS};
 static constexpr int COIN_KIND=2,AUTO_BASE=3,BLACK_HOLE_LOOT=23;
 static constexpr int activeSlotForLoot(int kind){return kind==BLACK_HOLE_LOOT?BLACK_HOLE:kind>=0&&kind<COIN_KIND?kind:-1;}
 enum Result {OK,EMPTY,COOLDOWN,INVALID,OUT_OF_RANGE,BLOCKED,NO_SPACE,NO_COINS,MAX_LEVEL,FINISHED,STALE};
 static constexpr int PROTOCOL=193,HOME_RADIUS=expedition_balance::HomeRadius,SIGHT=expedition_balance::SightRadius,MAX_LEVELS=3;
 struct Seat {
  int coins=0,shoes=0,armor=0,lastCommand=0,lastResult=0,teleports=0;
  bool bot=true;
  int deathCaster=-1,deathAt=0,lifeUntil=0,deathAllowedAt=0;

  std::array<int,3> frozen{},immune{},devourUntil{},devourBudget{};
 };
 struct Loot {int id=0,kind=0,x=0,y=0,born=0,hp=1200,eater=-1;std::array<int,MAX_TEAMS> damage{};}; // 0 freeze,1 cleanse,2 coin,3..22 automatic,23 black hole
 struct Obstacle {int id=0,x=0,y=0,radius=5,hp=120,wardUntil=0,owner=-1;bool removed=false;};
 struct Zone {int owner=0,x=0,y=0,until=0;};
 struct Chill {int owner=0,x=0,y=0,at=0;};
 int clock=0,duration=0,nextLoot=0,sequence=0,nextAI=0,cutoff=-1;
 uint32_t rng=1;
 std::array<Seat,MAX_TEAMS> seats;
 std::array<int,MAX_TEAMS> finalCounts{};
 std::vector<Loot> loot;
 std::vector<Obstacle> obstacles;
 std::vector<Zone> anchors;
 std::vector<Chill> chills;
 explicit Expedition(State&,int seed);
 void begin(State&);void end(State&);
 int moves(const State&,Dot&)const;
 void contact(State&,Dot& defender,Dot& attacker);
 int command(State&,int actor,int sequence,int kind,int group,int x,int y);
 int purchase(State&,int actor,int sequence,int equipment);
 int reconSeat(const State&)const;
 bool enemyVisible(const State&,int viewer,int x,int y,bool minimap=false)const;
 bool obstacleVisible(const State&,int viewer,const Obstacle&)const;
 void refreshSight(const State&)const;
 bool frozen(const Dot&)const;
 bool inHome(const State&,const Dot&)const;
 bool blockedByLoot(int x,int y)const;
 std::vector<int> snapshot(const State&,int viewer,bool authority=false)const;
 uint64_t hash(const State&)const;
 void finish(State&);

private:
 struct Scout {int x,y,next;};
 mutable int sightClock=-1,sightCols=0,sightRows=0;
 mutable std::vector<int> sightHeads;
 mutable std::array<std::array<int,4>,MAX_TEAMS> sightBounds{};
 mutable std::vector<Scout> scouts;
 std::array<std::vector<int>,MAX_TEAMS> birthDistances;
 unsigned random(unsigned n);
 bool spot(State&,int&,int&,int radius,int awayFromSpawn=0,int resourceSector=-1);
 void spawnLoot(State&,bool refreshCoins=true);
 int spawn(State&,int team,int count,int x,int y);
 void economy(State&);
 void bots(State&);


 bool reachable(const State&,int,int,int,int)const;
};
