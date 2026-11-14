//    This file is part of Liquid Wars.
//
//    Copyright (C) 2013 Henry Shepperd (hshepperd@gmail.com)
//
//    Liquid Wars is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, either version 3 of the License, or
//    (at your option) any later version.
//
//    Liquid Wars is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with Liquid Wars.  If not, see <http://www.gnu.org/licenses/>.

#ifndef STATE_HPP
#define STATE_HPP

#include <aclib.hpp>
#include <vector>
#include <algorithm>
#include "map.hpp"
#include "dot.hpp"
#include "player.hpp"
#include "info.hpp"
#include "random.hpp"
#include "spiral.hpp"
#include "items.hpp"
#include "operations.hpp"

class State {
    public:
        int me;
        bool currentlyDrawing;
        float timeSidebar;
        int displayWidth;
        int displayHeight;
        Player players[NUMBER_OF_TEAMS];
        Map* map;
        std::vector<Dot*> dots;
        struct Field {
            int stride=0;std::vector<Dot*> cells;
            void reset(int w,int h){stride=h;cells.assign(size_t(w)*h,nullptr);}
            void clear(){std::fill(cells.begin(),cells.end(),nullptr);}
            Dot** operator[](int x){return cells.data()+size_t(x)*stride;}
            Dot* const* operator[](int x)const{return cells.data()+size_t(x)*stride;}
        } field;
        std::vector<float> points;
        std::vector<float> colours;
        Random* moveRandom;
        Random* aiRandom;
        int dotsPerTeam;
        Items* items=nullptr;
        Operations* operations=nullptr;
        int birthX[6]={},birthY[6]={},spawnFallbacks=0;
        int alliances[6]={0,1,2,3,4,5};
        bool allied(int a,int b) const {return alliances[a]==alliances[b]||(items&&items->truceUntil>items->elapsed&&((alliances[a]==items->truceA&&alliances[b]==items->truceB)||(alliances[b]==items->truceA&&alliances[a]==items->truceB)));}
        State(int team, int mapId, int seed, int dotsPerTeam);
        ~State();
        void placeTeams(int seed, int mapId);
};

extern State* state;

#endif
