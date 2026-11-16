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

#include "state.hpp"
#include "spawn.hpp"
#include "map_catalog.hpp"
#include "garden3d.hpp"
#include <climits>
#include "expedition.hpp"

State* state;

State::State(int team, int mapId, int seed, int dotsPerTeam, bool garden,int expeditionSeats):garden3D(garden),teamCount(expeditionSeats?std::max(2,std::min(MAX_TEAMS,expeditionSeats)):NUMBER_OF_TEAMS),expeditionMode(garden&&expeditionSeats>0) {
    me = team;

    currentlyDrawing = false;

    timeSidebar = 0;

    this->dotsPerTeam = dotsPerTeam;

    for(int i = 0; i < MAX_TEAMS; i++)
        players[i].score = i < teamCount ? dotsPerTeam : 0;

    dots.resize(NUMBER_OF_TEAMS*dotsPerTeam);
    points.resize(NUMBER_OF_TEAMS*dotsPerTeam*3);
    colours.resize(NUMBER_OF_TEAMS*dotsPerTeam*4);



    for(int i = 0; i < NUMBER_OF_TEAMS*dotsPerTeam*4; i++)
        colours[i] = 1;

    moveRandom = new Random(seed);
    aiRandom = new Random(seed);

    // Keep one existing AI RNG draw for random-map choice. Stable asset IDs
    // preserve explicit map+seed births/flips for every surviving map.
    const int actualMap = garden3D ? garden3d::MAP_ID : mapId == -1 ? map_catalog::idAt(aiRandom->next() % NUMBER_OF_MAPS)
                                      : map_catalog::resolveExplicit(mapId);
    map = new Map(actualMap, garden3D?0:spawn::mapFlip(seed, actualMap),garden3D);

    field.reset(map->width,map->height);
    placeTeams(seed, actualMap);
    items=new Items(seed,actualMap);
    if(garden3D)items->nextWave=INT_MAX; // This local visual slice has no random items.
    operations=new Operations(*this);
    if(expeditionMode)expedition=new Expedition(*this,seed);
}

State::~State() {
    delete expedition;
    for(auto dot:dots)delete dot;
    delete items;
    delete operations;
    delete(map);
    delete(moveRandom);
    delete(aiRandom);
}

void State::placeTeams(int seed, int mapId) {
    if(garden3D)garden3d::place(*this,seed);else spawn::place(*this, seed, mapId);
}
