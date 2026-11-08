// SPDX-License-Identifier: GPL-3.0-or-later
#ifndef LW_AI_TACTICS_HPP
#define LW_AI_TACTICS_HPP
#include "dot.hpp"
#include <array>
#include <functional>
#include <vector>

// Read-only controller. Returns player inputs, never changes a dot, map or RNG.
class AiTactics {
    struct Cell { short x=-1,y=-1; unsigned char edges=0; };
    struct Bucket { int count=0,sx=0,sy=0; };
    static const int CELL=8;
    int width,height,columns,rows;
    std::array<int,6> alliances;
    std::function<bool(int,int)> wall;
    std::vector<Cell> cells;
    std::vector<Bucket> buckets;
    std::vector<int> distance,parent,queue;
    bool clear(int x,int y,int tx,int ty) const;
    int sourceCell(int x,int y) const;
public:
    AiTactics(int width,int height,std::function<bool(int,int)> wall,std::array<int,6> groups={{0,1,2,3,4,5}});
    std::array<short,60> plan(const std::vector<Dot*>& dots,const std::array<int,12>* strategy=nullptr,const std::array<unsigned,6>* hiddenMasks=nullptr);
};
#endif
