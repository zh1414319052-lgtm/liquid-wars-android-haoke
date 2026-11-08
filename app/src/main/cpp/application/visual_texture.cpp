// SPDX-License-Identifier: GPL-3.0-or-later
#include "visual_texture.hpp"
#include "visual_palette.hpp"
#include <algorithm>
#include <cmath>
namespace visual {
static float clamp(float x, float a, float b) { return std::max(a, std::min(b, x)); }
// Monotone cubic: retains thin lines without negative-lobe ringing or overshoot.
static float cubic(float a, float b, float c, float d, float t) {
    float delta = c-b, m0=(c-a)*.5f, m1=(d-b)*.5f;
    if (delta == 0) return b;
    if (m0*delta <= 0) m0=0;
    if (m1*delta <= 0) m1=0;
    float limit=3*std::abs(delta);
    m0=clamp(m0,-limit,limit);m1=clamp(m1,-limit,limit);
    float t2=t*t,t3=t2*t;
    return clamp((2*t3-3*t2+1)*b+(t3-2*t2+t)*m0+(-2*t3+3*t2)*c+(t3-t2)*m1,
                 std::min(b,c),std::max(b,c));
}
static unsigned powerOfTwo(unsigned n) { unsigned result=1;while(result<n)result*=2;return result; }
unsigned preferredScale(unsigned width,unsigned height) { return width<=1024&&height<=512?2u:1u; }
Texture prepareMap(const uint8_t* source, unsigned width, unsigned height,
                   unsigned maximumTextureSize, unsigned desiredScale) {
    if (!source || !width || !height || !maximumTextureSize || !desiredScale)
        return Texture{};
    unsigned limit=1;while(limit<=maximumTextureSize/2)limit*=2;
    float scale=std::min(float(desiredScale),std::min(float(limit)/width,float(limit)/height));
    Texture out{};out.contentWidth=std::max(1u,unsigned(width*scale));out.contentHeight=std::max(1u,unsigned(height*scale));
    out.width=powerOfTwo(out.contentWidth);out.height=powerOfTwo(out.contentHeight);
    if(out.contentWidth==width && out.contentHeight==height) {
        // New HD artwork already has coverage AA. Copy/premultiply without redundant upscaling.
        out.rgba.resize(size_t(out.width)*out.height*4);
        for(unsigned y=0;y<out.height;y++)for(unsigned x=0;x<out.width;x++) {
            const uint8_t* p=source+(size_t(std::min(y,height-1))*width+std::min(x,width-1))*4;
            uint8_t* q=out.rgba.data()+(size_t(y)*out.width+x)*4;q[3]=p[3];
            for(int c=0;c<3;c++)q[c]=uint8_t((unsigned(p[c])*p[3]+127)/255);
        }
        return out;
    }
    // Work in premultiplied RGBA throughout. Transparent RGB cannot create dark fringes.
    std::vector<float> horizontal(size_t(out.contentWidth)*height*4);
    for(unsigned x=0;x<out.contentWidth;x++) {
        float at=(x+.5f)*width/out.contentWidth-.5f;int ix=int(std::floor(at));float t=at-ix;
        unsigned xx[4];for(int k=0;k<4;k++)xx[k]=unsigned(std::max(0,std::min(int(width)-1,ix+k-1)));
        for(unsigned y=0;y<height;y++)for(int channel=0;channel<4;channel++) {
            float p[4];for(int k=0;k<4;k++){const uint8_t* s=source+(size_t(y)*width+xx[k])*4;p[k]=channel==3?s[3]:s[channel]*(s[3]/255.f);}
            horizontal[(size_t(y)*out.contentWidth+x)*4+channel]=cubic(p[0],p[1],p[2],p[3],t);
        }
    }
    out.rgba.resize(size_t(out.width)*out.height*4);
    for(unsigned y=0;y<out.contentHeight;y++) {
        float at=(y+.5f)*height/out.contentHeight-.5f;int iy=int(std::floor(at));float t=at-iy;
        unsigned yy[4];for(int k=0;k<4;k++)yy[k]=unsigned(std::max(0,std::min(int(height)-1,iy+k-1)));
        for(unsigned x=0;x<out.contentWidth;x++) {
            float rgba[4];for(int c=0;c<4;c++)rgba[c]=cubic(horizontal[(size_t(yy[0])*out.contentWidth+x)*4+c],horizontal[(size_t(yy[1])*out.contentWidth+x)*4+c],horizontal[(size_t(yy[2])*out.contentWidth+x)*4+c],horizontal[(size_t(yy[3])*out.contentWidth+x)*4+c],t);
            uint8_t* pixel=out.rgba.data()+(size_t(y)*out.width+x)*4;
            pixel[3]=uint8_t(std::lround(clamp(rgba[3],0,255)));
            for(int c=0;c<3;c++)pixel[c]=uint8_t(std::lround(clamp(rgba[c],0,pixel[3])));
        }
        // Extrude the last texel into POT padding; UVs only address the content rectangle.
        for(unsigned x=out.contentWidth;x<out.width;x++)for(int c=0;c<4;c++)
            out.rgba[(size_t(y)*out.width+x)*4+c]=out.rgba[(size_t(y)*out.width+out.contentWidth-1)*4+c];
    }
    for(unsigned y=out.contentHeight;y<out.height;y++)
        std::copy_n(out.rgba.data()+size_t(out.contentHeight-1)*out.width*4,size_t(out.width)*4,out.rgba.data()+size_t(y)*out.width*4);
    return out;
}
std::vector<uint8_t> nextMip(const std::vector<uint8_t>& source,unsigned width,unsigned height) {
    unsigned w=std::max(1u,width/2),h=std::max(1u,height/2);std::vector<uint8_t> out(size_t(w)*h*4);
    for(unsigned y=0;y<h;y++)for(unsigned x=0;x<w;x++)for(unsigned c=0;c<4;c++) {
        unsigned sum=0;for(unsigned yy=0;yy<2;yy++)for(unsigned xx=0;xx<2;xx++)sum+=source[(size_t(std::min(height-1,y*2+yy))*width+std::min(width-1,x*2+xx))*4+c];
        out[(size_t(y)*w+x)*4+c]=uint8_t((sum+2)/4);
    }
    return out;
}
std::vector<uint8_t> roundParticle(unsigned size,float diameterPixels) {
    std::vector<uint8_t> out(size_t(size)*size*4,255);float radius=std::max(.5f,diameterPixels*.5f),extent=radius+.5f;
    for(unsigned y=0;y<size;y++)for(unsigned x=0;x<size;x++) {
        float dx=((x+.5f)/size*2-1)*extent,dy=((y+.5f)/size*2-1)*extent;
        out[(size_t(y)*size+x)*4+3]=uint8_t(std::lround(clamp(radius+.5f-std::sqrt(dx*dx+dy*dy),0,1)*255));
    }
    return out;
}
std::vector<uint8_t> pearlParticle(unsigned size,float diameterPixels,bool highlight) {
    auto out=roundParticle(size,diameterPixels);
    const float radius=std::max(.5f,diameterPixels*.5f),extent=radius+.5f;
    for(unsigned y=0;y<size;y++)for(unsigned x=0;x<size;x++) {
        // Texture row zero is the lower edge of the world-space quad.
        float nx=((x+.5f)/size*2-1)*extent/radius;
        float ny=((y+.5f)/size*2-1)*extent/radius;
        float r2=nx*nx+ny*ny,nz=std::sqrt(std::max(0.f,1.f-r2));
        uint8_t* p=out.data()+(size_t(y)*size+x)*4;
        if(highlight) {
            // Broad, weak sheen avoids a subpixel white specular point flashing in motion.
            float dx=nx+.28f,dy=ny-.32f;
            float sheen=.18f*std::exp(-(dx*dx+dy*dy)/.30f)*nz;
            p[3]=uint8_t(std::lround(p[3]*sheen));
        } else {
            float diffuse=clamp(-.36f*nx+.44f*ny+.82f*nz,0.f,1.f);
            float shade=clamp(.70f+.28f*diffuse+.035f*(1.f-nz),0.f,1.f);
            // Retain meaningful RGB even in transparent texels: linear filtering cannot
            // introduce a black fringe. Alpha remains the same one-pixel coverage ramp.
            p[0]=p[1]=p[2]=uint8_t(std::lround(255*shade));
        }
    }
    return out;
}
Texture porcelainParticles(unsigned tileSize,float diameterPixels) {
    if(tileSize<8 || tileSize>256 || (tileSize&(tileSize-1)) ||
       !std::isfinite(diameterPixels) || diameterPixels<=0) return Texture{};
    Texture out{};
    out.width=out.contentWidth=tileSize*4;
    out.height=out.contentHeight=tileSize*2;
    out.rgba.resize(size_t(out.width)*out.height*4,0);
    const float radius=std::max(.5f,diameterPixels*.5f),extent=radius+.9f;
    for(unsigned team=0;team<6;team++)for(unsigned y=0;y<tileSize;y++)for(unsigned x=0;x<tileSize;x++) {
        const float dx=((x+.5f)/tileSize*2-1)*extent;
        const float dy=((y+.5f)/tileSize*2-1)*extent;
        const float r=std::sqrt(dx*dx+dy*dy),nx=dx/radius,ny=dy/radius;
        const float nz=std::sqrt(std::max(0.f,1.f-nx*nx-ny*ny));
        // Narrow physical-pixel coverage ramp. The interior stays opaque;
        // terrain cannot wash out team colour as it did with pastel soft dots.
        const float coverage=clamp((radius+.36f-r)/.72f,0.f,1.f);
        const float shadowRadius=std::sqrt((dx-.10f)*(dx-.10f)+(dy+.18f)*(dy+.18f));
        const float shadow=.20f*clamp((radius+.30f-shadowRadius)/.6f,0.f,1.f);
        const float alpha=coverage+shadow*(1.f-coverage);
        const float diffuse=clamp(-.36f*nx+.44f*ny+.82f*nz,0.f,1.f);
        const float shade=.64f+.36f*diffuse;
        const float rim=.72f*clamp((r/radius-.72f)/.28f,0.f,1.f);
        const float hx=nx+.29f,hy=ny-.34f;
        // A fixed broad glint, not a moving or subpixel flashing sparkle.
        const float glint=.32f*std::exp(-(hx*hx+hy*hy)/.12f)*nz;
        const unsigned atX=(team%4)*tileSize+x,atY=(team/4)*tileSize+y;
        uint8_t* p=out.rgba.data()+(size_t(atY)*out.width+atX)*4;
        for(unsigned c=0;c<3;c++) {
            const float edge=22.f+c*5.f;
            float body=TEAM_RGB[team][c]*shade;
            body=body*(1.f-rim)+edge*rim;
            body=body*(1.f-glint)+(c==2?239.f:250.f)*glint;
            // Straight alpha for the existing SRC_ALPHA blend mode. Meaningful
            // RGB outside the circle prevents filtering a black gutter fringe.
            const float rgb=alpha>0?(body*coverage+edge*shadow*(1.f-coverage))/alpha:edge;
            p[c]=uint8_t(std::lround(clamp(rgb,0.f,255.f)));
        }
        p[3]=uint8_t(std::lround(alpha*255));
    }
    return out;
}
}
