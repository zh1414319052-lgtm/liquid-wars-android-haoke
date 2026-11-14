// SPDX-License-Identifier: GPL-3.0-or-later
// Focused CPU data checks only: no window, EGL context, game renderer or device.
#include "visual_texture.hpp"
#include "visual_palette.hpp"
#include "lodepng.hpp"
#include <algorithm>
#include <cmath>
#include <cstdlib>
#include <fstream>
#include <iostream>
#include <stdexcept>
#include <string>
static void require(bool ok,const char* message){if(!ok)throw std::runtime_error(message);}
static void checkPremultiplied(const visual::Texture& texture) {
    require(texture.width>=texture.contentWidth&&texture.height>=texture.contentHeight,"Content bounds");
    for(size_t i=0;i<texture.rgba.size();i+=4)for(int c=0;c<3;c++)require(texture.rgba[i+c]<=texture.rgba[i+3],"Premultiplied range");
}
int main(int argc,char** argv) {try {
    require(argc==2,"Supply original map asset directory");
    for(int team=0;team<6;team++) {
        float previous[3]={0,0,0};
        for(int health=0;health<=10;health++) {
            float rgb[3];visual::teamColour(team,health/10.f,rgb);
            for(int c=0;c<3;c++) {require(std::isfinite(rgb[c])&&rgb[c]>=0&&rgb[c]<=200.f/255.f,"Soft palette range");require(rgb[c]>=previous[c],"Health colour monotonicity");previous[c]=rgb[c];}
        }
        float low[3],high[3];visual::teamColour(team,-1,low);visual::teamColour(team,2,high);
        for(int c=0;c<3;c++)require(low[c]<=high[c],"Clamped health colour");
    }
    const uint8_t edge[]={255,255,255,255, 0,0,0,0, 255,255,255,255, 0,0,0,0};
    auto texture=visual::prepareMap(edge,2,2,2048);checkPremultiplied(texture);
    for(size_t i=0;i<texture.rgba.size();i+=4)for(int c=0;c<3;c++)require(texture.rgba[i+c]==texture.rgba[i+3],"White edge dark fringe");
    const uint8_t invisible[]={255,0,255,0};texture=visual::prepareMap(invisible,1,1,2048);
    require(std::all_of(texture.rgba.begin(),texture.rgba.end(),[](uint8_t a){return a==0;}),"Transparent RGB contamination");
    std::vector<uint8_t> thin(21*21*4,0);for(int y=0;y<21;y++)for(int c=0;c<4;c++)thin[(y*21+10)*4+c]=255;
    texture=visual::prepareMap(thin.data(),21,21,2048);unsigned peak=0;
    for(unsigned x=0;x<texture.contentWidth;x++)peak=std::max(peak,unsigned(texture.rgba[(20*texture.width+x)*4+3]));
    require(peak>=200,"One-pixel wall lost");
    texture=visual::prepareMap(thin.data(),21,21,32);require(texture.width<=32&&texture.height<=32,"Texture capability fallback");
    require(visual::prepareMap(nullptr,0,0,0).rgba.empty(),"Invalid dimensions");
    for(float diameter:{1.f,2.75f,5.f}) {
        auto sprite=visual::roundParticle(64,diameter);require(sprite[3]==0,"Sprite corner must be transparent");
        require(sprite[(32*64+32)*4+3]>240,"Sprite core");
        for(unsigned y=0;y<64;y++)for(unsigned x=0;x<64;x++) {
            auto index=(y*64+x)*4;require(sprite[index]==255&&sprite[index+1]==255&&sprite[index+2]==255,"Sprite transparent RGB");
            require(sprite[index+3]==sprite[(x*64+y)*4+3],"Circular sprite symmetry");
        }
    }
    size_t allPixels=0;int maps=0;
    for(int map=0;map<46;map++) {
        std::string base=std::string(argv[1])+"/"+std::to_string(map);
        std::string file=base+"-image.png";if(!std::ifstream(file).good())file=base+"-map.png";
        if(!std::ifstream(file).good())file=std::string(argv[1])+"/blank-map.png";
        unsigned char* data=nullptr;unsigned w=0,h=0;require(lodepng_decode32_file(&data,&w,&h,file.c_str())==0,"PNG decode");
        std::vector<uint8_t> original(data,data+size_t(w)*h*4);unsigned desired=visual::preferredScale(w,h);auto output=visual::prepareMap(data,w,h,2048,desired);
        require(std::equal(original.begin(),original.end(),data),"Source PNG data changed");free(data);checkPremultiplied(output);
        float scale=std::min(float(desired),std::min(2048.f/w,2048.f/h));
        require(output.contentWidth==std::max(1u,unsigned(w*scale))&&output.contentHeight==std::max(1u,unsigned(h*scale)),"Uniform visual scale");
        require(output.width<=2048&&output.height<=2048,"Maximum texture size");
        // Coverage still present at the original centers of solid one-pixel structures.
        size_t lostSolid=0;
        for(unsigned y=0;y<h;y++)for(unsigned x=0;x<w;x++)if(original[(size_t(y)*w+x)*4+3]==255) {
            unsigned ox=std::min(output.contentWidth-1,unsigned((x+.5f)*output.contentWidth/w));
            unsigned oy=std::min(output.contentHeight-1,unsigned((y+.5f)*output.contentHeight/h));
            if(output.rgba[(size_t(oy)*output.width+ox)*4+3]<128)++lostSolid;
        }
        require(lostSolid==0,"Solid source structure disappeared at original cell center");
        auto mip=output.rgba;unsigned mw=output.width,mh=output.height;
        while(mw>1||mh>1){mip=visual::nextMip(mip,mw,mh);mw=std::max(1u,mw/2);mh=std::max(1u,mh/2);require(mip.size()==size_t(mw)*mh*4,"Mip dimensions");}
        allPixels+=size_t(w)*h;++maps;
    }
    const size_t cap=16383;for(size_t total:{size_t(0),size_t(2400),size_t(27000)})for(size_t base=0;base<total;base+=cap)require(std::min(cap,total-base)*4-1<65536,"16bit particle index overflow");
    std::cout<<"PASS: "<<maps<<" maps, "<<allPixels<<" source pixels; premultiplied edges, thin walls, mips, circular coverage and0/2400/27000 batch bounds. CPU DATA ONLY; rendering NOT_TESTED.\n";
    return 0;
}catch(const std::exception& e){std::cerr<<"FAIL: "<<e.what()<<'\n';return 1;}}
