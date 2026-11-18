// SPDX-License-Identifier: GPL-3.0-or-later
#define main base_items_main
#include "check_items_core.cpp"
#undef main
#include "expedition.hpp"
#include "state_hash.hpp"

static void checkMode(int seats) {
    auto match = std::make_unique<State>(0, seats ? 1001 : 1, 205, 100, seats > 0, seats);
    State& s = *match;
    Items& i = *s.items;
    i.warmup = 0;
    i.nextWave = INT_MAX;
    if (s.expedition) {
        s.expedition->loot.clear();
        s.expedition->nextLoot = s.expedition->nextAI = INT_MAX;
        for (auto& seat : s.expedition->seats) seat.bot = false;
    }
    for (auto* dot : s.dots) {
        s.field[dot->x][dot->y] = nullptr;
        delete dot;
    }
    s.dots.clear();
    for (auto& player : s.players) {
        player.score = 0;
        player.x[0] = player.y[0] = -2;
    }

    const int cx = s.map->width / 2, cy = s.map->height / 2;
    int nearX = 0, nearY = 0, farX = 0, farY = 0, wallX = 0, wallY = 0;
    for (int edge = 35; edge <= 180 && !nearX; edge++) {
        for (const auto& direction : {std::pair{1,0}, std::pair{-1,0}, std::pair{0,1}, std::pair{0,-1}}) {
            int dx = direction.first, dy = direction.second;
            int wx = cx + edge*dx, wy = cy + edge*dy;
            int nx = wx + 14*dx, ny = wy + 14*dy;
            int fx = wx + 150*dx, fy = wy + 150*dy;
            if (s.map->isWall(nx,ny) || s.map->isWall(fx,fy) || s.map->isWall(nx-dy,ny+dx)) continue;
            if (!s.map->temporaryObstacle(wx,wy,4,true)) continue;
            nearX = nx; nearY = ny; farX = fx; farY = fy; wallX = wx; wallY = wy;
            break;
        }
    }
    check(nearX != 0, "wall fixture along attraction ray");
    Dot* close = add(s, 1, nearX, nearY);
    Dot* distant = add(s, 0, farX, farY);
    Dot* sacrifice=nullptr;
    for(int dy=-7;dy<=7&&!sacrifice;dy++)for(int dx=-7;dx<=7;dx++){
        if(dx*dx+dy*dy<=36||dx*dx+dy*dy>64||s.map->isWall(cx+dx,cy+dy))continue;
        int inX=dx-dx/std::max(1,std::abs(dx)),inY=dy-dy/std::max(1,std::abs(dy));
        if(inX*inX+inY*inY>36||s.map->isWall(cx+inX,cy+inY)||s.map->isWall(cx+dx,cy+inY)||s.map->isWall(cx+inX,cy+dy))continue;
        sacrifice=add(s,0,cx+dx,cy+dy);break;
    }
    check(sacrifice!=nullptr,"playable core-entry fixture");
    auto owns = [&](const Dot* dot) {return std::find(s.dots.begin(),s.dots.end(),dot)!=s.dots.end();};
    auto range = [&](const Dot& dot) {return std::max(std::abs(dot.x-cx),std::abs(dot.y-cy));};
    i.capture(s);
    i.grant(s, 1, Items::ACTIVE_CLEANSE, 0, 0);
    const int slot = Items::BLACK_HOLE_SLOT;
    check(i.activate(s,0,1,slot)==Expedition::EMPTY,"empty activation leaves battlefield unchanged");
    for (int n=0;n<12;n++) i.grant(s,0,Items::ACTIVE_BLACK_HOLE,0,0);
    check(i.stock[0][slot]==10 && !i.pickupAllowed(0,Items::ACTIVE_BLACK_HOLE),"inventory caps at ten");
    check(i.activate(s,0,2,slot)==Expedition::OK && i.stock[0][slot]==9,"one cast consumes one charge");
    check(i.holeUntil-i.holeStart==10000 && i.last.x==cx && i.last.y==cy,"cast duration and whole-map center");
    check(i.pulling(0) && i.pulling(1),"caster and opponent are pulled");
    check(i.activate(s,0,2,slot)==Expedition::STALE && i.stock[0][slot]==9,"duplicate cast preserves inventory");
    check(i.activate(s,0,3,slot)==Expedition::BLOCKED && i.stock[0][slot]==9,"active hole preserves extra charges");
    const int nearBefore = range(*close), farBefore = range(*distant);
    step(s,Items::BLACK_HOLE_WARNING_MS/Items::STEP_MS);
    check(range(*close)==nearBefore&&range(*distant)==farBefore&&owns(sacrifice),"warning preserves idle positions and prevents core death");
    step(s,60);
    check(nearBefore-range(*close)>farBefore-range(*distant) && farBefore>range(*distant),"near pull stronger; far army still moves");
    step(s,150);
    check(!s.map->isWall(close->x,close->y)&&range(*close)==std::max(std::abs(wallX-cx),std::abs(wallY-cy))+5,"solid wall stops every substep before the far side");
    check(!owns(sacrifice)&&s.players[0].score==1&&i.holeAbsorbed==1&&i.lastHoleAbsorb>=i.holeStart,"actual caster death triggers one absorption event");
    i.until[1][Items::FREEZE] = i.elapsed+10000;
    check(i.activate(s,1,7,Items::ACTIVE_CLEANSE-Items::COUNT)==Expedition::OK,"cleanse usable while frozen");
    check(!i.pulling(1) && i.pulling(0) && !i.active(1,Items::FREEZE),"cleanse releases only own army");
    check(!s.map->isWall(close->x,close->y) && s.field[close->x][close->y]==close,"cleansing does not teleport or change collision");
    const int savedX=close->x, savedY=close->y;
    step(s,140);
    check(owns(close) && close->x==savedX && close->y==savedY,"cleansed army is not grabbed again by this hole");
    while (i.elapsed+Items::STEP_MS<i.holeUntil) step(s,1);
    check(i.blackHoleActive(),"pull stays active up to the next tick at ten seconds");
    step(s,1);
    check(!i.blackHoleActive() && !i.holeExempt[1] && i.holeAbsorbed==0,"expiration clears pull, immunity and flash state");
    valid(s);
    if (s.expedition) {
        const auto snapshot = s.expedition->snapshot(s,0);
        check(snapshot[snapshot.size()-7]==Items::BLACK_HOLE_FACTS && snapshot[snapshot.size()-4]==9,"expedition footer reports local inventory");
    }
    i.stock[0][slot]=10; // The neighboring team cannot take either fixture pickup.
    for(int count : {9,10}){
        i.stock[1][slot]=count;
        if(s.expedition){
            s.expedition->loot.clear();
            s.expedition->loot.push_back({9931,Expedition::BLACK_HOLE_LOOT,close->x,close->y,i.elapsed,1});
            s.expedition->end(s);
            check(std::none_of(s.expedition->loot.begin(),s.expedition->loot.end(),[](auto p){return p.id==9931;})==(count<10),"expedition leaves pickups when bag is full");
        }else{
            auto& pickup=i.pickups[0];
            pickup=Items::Pickup{};pickup.kind=Items::ACTIVE_BLACK_HOLE;pickup.x=close->x;pickup.y=close->y;pickup.hp=1;
            i.end(s);
            check((pickup.kind<0)==(count<10),"classic leaves pickups when bag is full");
        }
        check(i.stock[1][slot]==10,"actual pickup grants one black hole up to ten");
    }
    auto beforeHash=simulationHash(s);
    i.stock[0][slot]--;
    check(simulationHash(s)!=beforeHash,"black hole stock enters consistency hash");
    i.rewind(s);
    check(!i.blackHoleActive() && i.stock[0][slot]==0 && !i.holeExempt[1],"rewind clears black hole state and inventory");
    state=nullptr;
    std::cout<<(seats ? "3D expedition" : "2D classic")<<": activation, solid obstacles, cleanse, live absorption events, expiry and rewind passed\n";
}

#ifndef BLACK_HOLE_TEST_LIBRARY
int main(int argc,char** argv) {
    try {
        check(argc==2,"asset root required");
        assets.root=argv[1];
        checkMode(0);
        checkMode(2);
        return 0;
    } catch(const std::exception& error) {
        std::cerr<<"FAIL after "<<checks<<" checks: "<<error.what()<<'\n';
        return 1;
    }
}
#endif
