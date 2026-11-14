// SPDX-License-Identifier: GPL-3.0-or-later
#pragma once
#include <array>
#include <vector>
#include <cstdint>
class State;
class Dot;

// Simulation-owned orders. Commands travel through the existing lockstep frames.
class Operations {
public:
 static constexpr int GROUPS=3,MAX_POINTS=24,GRID=8,RADIUS=38,WIN_SCORE=180;
 enum Command { BEGIN=1,APPEND=2,COMMIT=3,STOP=4,SPLIT=5,MERGE=6,FOLLOW=7 };
 enum Status { IDLE=0,MOVING=1,ARRIVED=2,BLOCKED=3,INVALID=4 };
 struct Point {int x=0,y=0;};
 struct Route {bool enabled=false,hold=false,tracking=false;int at=0,status=IDLE,version=-1,goal=-1;uint64_t flowHash=0;std::vector<Point> points;std::vector<int> distance;};
 struct Group {int count=0,x=0,y=0;};
 struct Site {int x=0,y=0,owner=-1,claim=-1,progress=0,contested=0,spawnAt=0,scoreAt=0,born=0,pulseAt=-10000;};
 int mode=0,clock=0,epoch=0,revision=0;
 std::array<std::array<Route,GROUPS>,6> routes;
 std::array<std::array<Group,GROUPS>,6> groups{};
 std::array<bool,6> split{};
 std::array<int,6> lastPacket{},feedback{},points{},generation{};
 std::vector<Site> sites;
 explicit Operations(State&);
 void configure(State&,int);
 void reset(State&);
 bool command(State&,int,const short*,const short*);
 void direct(int);
 void clearTeam(State&,int);
 void step(State&);
 bool target(const State&,const Dot&,int&,int&)const;
 std::vector<int> snapshot(const State&,int viewer,bool all=false)const;
 uint64_t hash()const;
 int winningTeam(const State&)const;
 int alliancePoints(const State&,int)const;
private:
 struct Node {short x=-1,y=-1;unsigned char edges=0;int component=-1;};
 int cols=0,rows=0,terrain=-1,wave=-1,builtAt=-10000,navVersion=0;
 uint64_t navHash=0;
 std::vector<Node> nodes;
 std::array<std::vector<Point>,6> staged;
 std::array<int,6> stagingGroup{{-1,-1,-1,-1,-1,-1}};
 void build(State&);
 bool clear(const State&,int,int,int,int)const;
 int nodeAt(const State&,int,int)const;
 void flow(State&,Route&);
 void stats(State&);
 void locateSites(State&);
 void economy(State&);
 int spawn(State&,Site&,int);
};
