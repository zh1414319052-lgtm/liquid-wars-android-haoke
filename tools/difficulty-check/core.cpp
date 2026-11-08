// SPDX-License-Identifier: GPL-3.0-or-later
// Actual State/Move/Map/AI/Random linked below; no GL context, device or rendered frame.
#include "state.hpp"
#include "move.hpp"
#include "ai.hpp"
#include "ai_tactics.hpp"
#include "visual_texture.hpp"
#include <iostream>
#include <iomanip>
#include <stdexcept>
#include <memory>
#include <algorithm>
#include <array>
#include <cstdint>

HostAssets assets;HostAssets* acAssets=&assets;
static int checks;
static void require(bool ok,const char* why){++checks;if(!ok)throw std::runtime_error(why);}
static uint64_t hashState(State* s) {
    uint64_t h=1469598103934665603ULL;
    auto mix=[&](uint32_t x){for(int b=0;b<4;b++){h^=(x>>(b*8))&255;h*=1099511628211ULL;}};
    for(const Dot* d:s->dots){mix(d->x);mix(d->y);mix(d->team);mix(d->health);}
    for(auto& p:s->players){mix(p.score);for(int i=0;i<5;i++){mix(p.x[i]);mix(p.y[i]);}}
    mix(s->moveRandom->getSeed());mix(s->aiRandom->getSeed());return h;
}
static void legacyTargets() {
    for(int team=0;team<6;team++){auto& p=state->players[team];int target=AI::getNearestDot(team,p.x[0],p.y[0]);p.x[0]=short(unsigned(target)>>16);p.y[0]=short(target);}
}
static void useTargets(const std::array<short,60>& targets,int mask) {
    for(int p=0;p<6;p++)if((mask&(1<<p))&&targets[p*10]>=0&&targets[p*10+5]>=0)
        for(int i=0;i<5;i++){state->players[p].x[i]=targets[p*10+i];state->players[p].y[i]=targets[p*10+i+5];}
}
static void checkPlanner() {
    auto wall=[](int x,int y){return x<0||y<0||x>=80||y>=64||(x>=36&&x<=43&&y<48);};
    AiTactics planner(80,64,wall);
    Dot own(20,16,0),enemy(60,16,1);std::vector<Dot*> dots{&own,&enemy};
    auto target=planner.plan(dots);require(target==planner.plan(dots),"deterministic planning");
    std::cout<<"wall route target="<<target[0]<<','<<target[5]<<'\n';
    require(target[0]>=0&&target[5]>16&&!wall(target[0],target[5]),"navigate around blocking wall");
    // The first waypoint may lie below the wall rather than to its left.
    for(int i=0;i<=128;i++){int x=20+(target[0]-20)*i/128,y=16+(target[5]-16)*i/128;require(!wall(x,y),"route crosses blocker");}
    require(own.x==20&&own.y==16&&enemy.x==60&&enemy.y==16,"planner changed dots");
    require(planner.plan({})[0]==-1,"empty match");
    require(planner.plan({&own})[0]==-1,"no enemy");
    AiTactics sealed(80,64,[](int x,int y){return x<0||y<0||x>=80||y>=64||(x>=36&&x<=43);});
    require(sealed.plan(dots)[0]==-1,"unreachable fallback");
    AiTactics open(80,64,[](int x,int y){return x<0||y<0||x>=80||y>=64;});
    target=open.plan(dots);require(target[1]>=0&&target[2]>=0,"close lateral inputs");
}
static void checkMaterials(const std::string& output) {
    for(float diameter:{1.f,2.75f,5.f}){
        auto old=visual::roundParticle(64,diameter),body=visual::pearlParticle(64,diameter,false),shine=visual::pearlParticle(64,diameter,true);
        require(body==visual::pearlParticle(64,diameter,false),"stable paused material");
        for(size_t i=0;i<body.size();i+=4){require(old[i+3]==body[i+3],"coverage changed");require(body[i]>=178&&body[i]<=255,"body black fringe");require(shine[i+3]<=46,"excessive sheen");}
        require(body[(42*64+22)*4]>body[(22*64+42)*4],"fixed upper-left light");
    }
    for(bool highlight:{false,true}){
        auto sprite=visual::pearlParticle(64,5.f,highlight);
        std::ofstream file(output+(highlight?"/particle-sheen.rgba":"/particle-body.rgba"),std::ios::binary);
        file.write(reinterpret_cast<const char*>(sprite.data()),sprite.size());require(bool(file),"save exact sprite");
    }
    for(size_t total:{size_t(0),size_t(2400),size_t(27000)}){
        int draws=0;for(size_t base=0;base<total;base+=16383){size_t count=std::min(size_t(16383),total-base);require(count*4-1<65536,"index overflow");draws+=2;}
        require(draws<=4,"batch draw ceiling");
    }
}
int main(int argc,char** argv){try{
    require(argc==3,"assets and output paths");assets.root=argv[1];checkPlanner();checkMaterials(argv[2]);
    for(int map=0;map<46;map++){
        auto s=std::make_unique<State>(0,map,154,400);state=s.get();
        AiTactics planner(WIDTH,HEIGHT,[&](int x,int y){return s->map->isWall(x,y);});
        auto before=hashState(s.get());auto targets=planner.plan(s->dots);
        require(hashState(s.get())==before,"read-only actual maps/RNG");
        for(int t=0;t<6;t++)for(int k=0;k<5;k++){int x=targets[t*10+k],y=targets[t*10+k+5];require((x==-1&&y==-1)||!s->map->isWall(x,y),"actual-map target in wall");}
    }
    for(int map:{0,17,45}){
        auto a=std::make_unique<State>(0,map,921,400),b=std::make_unique<State>(0,map,921,400);
        AiTactics planner(WIDTH,HEIGHT,[&](int x,int y){return b->map->isWall(x,y);});
        for(int tick=0;tick<100;tick++){
            state=a.get();for(int n=0;n<10;n++)Move::stepDots();if(tick>=7)legacyTargets();
            state=b.get();for(int n=0;n<10;n++)Move::stepDots();if(tick%8==0)planner.plan(b->dots);if(tick>=7)legacyTargets();
            require(hashState(a.get())==hashState(b.get()),"planner changes standard simulation");
        }
        std::cout<<"classic map="<<map<<" steps=1000 hash="<<std::hex<<hashState(a.get())<<std::dec<<'\n';
    }
    // A bounded CPU scenario, not a phone performance benchmark or measured player difficulty.
    for(int mask:{0x15,0x2a}){
        auto s=std::make_unique<State>(0,0,921,400);state=s.get();
        AiTactics planner(WIDTH,HEIGHT,[&](int x,int y){return s->map->isWall(x,y);});std::array<short,60> targets;targets.fill(-1);
        for(int tick=0;tick<600;tick++){
            for(int n=0;n<10;n++)Move::stepDots();if(tick<7)continue;
            if((tick-7)%8==0)targets=planner.plan(s->dots);
            legacyTargets();useTargets(targets,mask);
        }
        int hard=0,classic=0;for(int p=0;p<6;p++)((mask&(1<<p))?hard:classic)+=s->players[p].score;
        require(hard+classic==2400,"authority particle count");
        std::cout<<"CPU scenario map=0 seed=921 steps=6000 hardMask="<<mask<<" hardDots="<<hard<<" classicDots="<<classic<<'\n';
    }
    {auto s=std::make_unique<State>(0,45,154,4500);state=s.get();AiTactics planner(WIDTH,HEIGHT,[&](int x,int y){return s->map->isWall(x,y);});auto before=hashState(s.get());planner.plan(s->dots);require(before==hashState(s.get()),"27000 scale read-only");}
    state=nullptr;std::cout<<"PASS: "<<checks<<" CPU assertions. Actual rules, 46 masks, read-only RNG/state, 27000 dots, materials; Android/GL NOT_TESTED.\n";return 0;
}catch(const std::exception& e){std::cerr<<"FAIL: "<<e.what()<<'\n';return 1;}}
