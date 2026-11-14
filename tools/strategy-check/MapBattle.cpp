// SPDX-License-Identifier: GPL-3.0-or-later
// Real unmodified rule engine and real AiTactics. The Java HARD input cadence
// is mirrored here; simulation seconds assume the configured 7000us rule tick.
#include "state.hpp"
#include "move.hpp"
#include "ai.hpp"
#include "ai_tactics.hpp"
#include <memory>
#include <fstream>
#include <sstream>
#include <iostream>
#include <set>
#include <chrono>
#include <algorithm>
HostAssets assets;HostAssets* acAssets=&assets;
static bool contact(const State& s){for(const Dot* d:s.dots)for(int dx=-1;dx<=1;dx++)for(int dy=-1;dy<=1;dy++){int x=int(d->x)+dx,y=int(d->y)+dy;if(x<0||y<0||x>=800||y>=480)continue;const Dot* q=s.field[x][y];if(q&&q->team!=d->team)return true;}return false;}
int main(int argc,char** argv){assets.root=argv[1];int first=std::stoi(argv[2]),last=std::stoi(argv[3]),seeds=std::stoi(argv[4]);std::string path=argv[5];std::set<std::pair<int,int>> complete;{
 std::ifstream in(path);std::string line;std::getline(in,line);while(std::getline(in,line)){std::replace(line.begin(),line.end(),',',' ');std::istringstream s(line);int map,seed;if(s>>map>>seed)complete.insert({map,seed});}}
 bool header=complete.empty();std::ofstream out(path,std::ios::app);if(header)out<<"map,seed,first_contact_s,end_s,winner,winning_spawn_x,winning_spawn_y,tied,score0,score1,score2,score3,score4,score5\n";
 for(int map=first;map<=last;map++){auto began=std::chrono::steady_clock::now();for(int k=0;k<seeds;k++){int seed=921+k;if(complete.count({map,seed}))continue;auto owned=std::make_unique<State>(0,map,seed,1000);state=owned.get();int starts[6][2];for(int t=0;t<6;t++){starts[t][0]=state->players[t].x[0];starts[t][1]=state->players[t].y[0];}
 AiTactics planner(800,480,[&](int x,int y){return state->map->isWall(x,y);});std::array<short,60> planned;planned.fill(-1);int tick=0,delay=6,step=0;double firstContact=-1;
 for(;step<17140;step+=10){for(int i=0;i<10;i++)Move::stepDots();if(firstContact<0&&contact(*state))firstContact=(step+10)*.007;bool winner=false;for(int t=0;t<6;t++)winner|=state->players[t].score==6000;if(winner)break;
  if(delay--<0){if(tick%8==0)planned=planner.plan(state->dots);for(int t=0;t<6;t++){int nearest=AI::getNearestDot(t,state->players[t].x[0],state->players[t].y[0]);if(planned[t*10]>=0&&planned[t*10+5]>=0){for(int i=0;i<5;i++){state->players[t].x[i]=planned[t*10+i];state->players[t].y[i]=planned[t*10+5+i];}}else{state->players[t].x[0]=short(uint32_t(nearest)>>16);state->players[t].y[0]=short(nearest);for(int i=1;i<5;i++)state->players[t].x[i]=state->players[t].y[i]=-1;}}tick++;}
 }
 int winner=0;for(int t=1;t<6;t++)if(state->players[t].score>state->players[winner].score)winner=t;bool tied=false;for(int t=0;t<6;t++)if(t!=winner&&state->players[t].score==state->players[winner].score)tied=true;
 out<<map<<','<<seed<<','<<firstContact<<','<<std::min(120.,(step+10)*.007)<<','<<winner<<','<<starts[winner][0]<<','<<starts[winner][1]<<','<<tied;for(int t=0;t<6;t++)out<<','<<state->players[t].score;out<<'\n';out.flush();
 }
 std::cout<<"map="<<map<<" complete; wall seconds="<<std::chrono::duration<double>(std::chrono::steady_clock::now()-began).count()<<std::endl;
 }
}
