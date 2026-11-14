// SPDX-License-Identifier: GPL-3.0-or-later
// Actual production sprite generator checks; never renders a fake gameplay image.
#include "visual_texture.hpp"
#include "visual_palette.hpp"
#include <fstream>
#include <iostream>
#include <stdexcept>
#include <cmath>
static void require(bool b,const char* s){if(!b)throw std::runtime_error(s);}
int main(int argc,char** argv){try{
    require(visual::porcelainParticles(0,3).rgba.empty(),"Invalid atlas dimensions");
    require(visual::porcelainParticles(63,3).rgba.empty(),"POT required");
    require(visual::porcelainParticles(64,NAN).rgba.empty(),"Invalid diameter");
    for(unsigned tile:{16u,32u,64u})for(float diameter:{2.25f,3.26f,5.2f}) {
        auto a=visual::porcelainParticles(tile,diameter),b=visual::porcelainParticles(tile,diameter);
        require(a.rgba==b.rgba,"Deterministic fixed material");
        require(a.width==tile*4&&a.height==tile*2&&a.rgba.size()==size_t(a.width)*a.height*4,"Atlas layout");
        for(unsigned team=0;team<6;team++) {
            auto at=[&](unsigned x,unsigned y,unsigned c){return a.rgba[((team/4*tile+y)*a.width+team%4*tile+x)*4+c];};
            require(at(tile/2,tile/2,3)==255,"Opaque colour core");
            for(unsigned k=0;k<tile;k++)require(at(k,0,3)==0&&at(k,tile-1,3)==0&&at(0,k,3)==0&&at(tile-1,k,3)==0,"Transparent atlas gutters; no tile bleed");
            unsigned partial=0,opaque=0;
            for(unsigned y=0;y<tile;y++)for(unsigned x=0;x<tile;x++) {auto alpha=at(x,y,3);partial+=alpha>0&&alpha<255;opaque+=alpha==255;}
            require(partial>0&&opaque>tile*tile/16,"Antialiased edge and opaque body");
            require(at(tile/2,tile/2,0)>30&&at(tile/2,tile/2,1)>30,"Core retains coloured body");
        }
    }
    require(visual::healthLevel(-1)==.9f&&visual::healthLevel(2)==1.f,"Clamped visual health");
    if(argc==2){auto a=visual::porcelainParticles(64,3.26f);std::ofstream out(argv[1],std::ios::binary);out.write(reinterpret_cast<const char*>(a.rgba.data()),a.rgba.size());}
    std::cout<<"PASS actual porcelain generator: six teams x3 tile sizes x3 physical diameters; deterministic, opaque, AA and gutters. No renderer/performance claim.\n";
    return 0;
}catch(const std::exception& e){std::cerr<<"FAIL "<<e.what()<<'\n';return 1;}}
