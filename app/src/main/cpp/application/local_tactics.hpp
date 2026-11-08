// SPDX-License-Identifier: GPL-3.0-or-later
#pragma once
#include "state.hpp"
#include <array>
#include <vector>

// Worker-owned read-only controller. No State writes, simulation RNG or network.
class LocalTactics {
public:
 enum Action { ATTACK, RETREAT, REGROUP, SUPPORT, PICKUP, EXPLORE, HOLD };
 struct Decision {int action=HOLD,enemy=-1,pickup=-1,x=-1,y=-1,since=0,progressAt=0,px=0,py=0,stuck=0;};
 std::array<Decision,6> decisions{};
 std::array<short,60> plan(const State&,unsigned mask,int level);
 void reset();
private:
 static constexpr int GRID=8,BIN=32;
 struct Node {short x=-1,y=-1;unsigned char edges=0;};
 struct Spot {int x=-1,y=-1,count=0,health=0;std::array<std::array<short,2>,4> scout{{{{-1,-1}},{{-1,-1}},{{-1,-1}},{{-1,-1}}}};};
 int width=0,height=0,cols=0,rows=0,bcols=0,brows=0,revision=-1,epoch=-1,builtAt=-10000;
 std::vector<Node> nodes;
 std::vector<Spot> spots;
 std::vector<int> cost,parent,visits;
 std::vector<unsigned char> known,traversal;
 std::vector<float> friendly,hostile;
 std::array<int,6> totals{},visibleTotals{};
 std::array<bool,6> allies{};
 void build(const State&);
 void observe(const State&,int team);
 bool clear(const State&,int,int,int,int,int ignorePickup=-1,bool phase=false)const;
 int nodeAt(const State&,int x,int y)const;
 void search(const State&,int start,int team,bool brave);
 float power(const std::vector<float>&,int,int,int radius=2)const;
 float itemValue(const State&,int team,int slot,float own,float danger)const;
 bool landing(const State&,int team,int x,int y,int& tx,int& ty)const;
};
