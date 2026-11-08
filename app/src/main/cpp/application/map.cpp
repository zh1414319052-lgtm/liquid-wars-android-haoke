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

#include "map.hpp"
#include "visual_texture.hpp"
#include "map_catalog.hpp"
#include <cstdlib>
#include <android/log.h>
#include <algorithm>
#include <utility>

Map::Map(int mapID, int transform):flip(transform&3),width(map_catalog::width(mapID)),height(map_catalog::height(mapID)),id(mapID) {
    path = "maps/";
    std::stringstream ss;
    ss << mapID;
    path += ss.str();

    int pngDataSize;
    unsigned char* pngData = (unsigned char*)acAssets->getFile((path + "-map.png").data(), &pngDataSize);
    if(pngData == NULL)
        pngData = (unsigned char*)acAssets->getFile("maps/blank-map.png", &pngDataSize);

    lodepng_decode32((unsigned char**)&imageMapData, &imageMapWidth, &imageMapHeight, pngData, pngDataSize);

    free(pngData);

    imageMapWidthRatio = (float)imageMapWidth/width;
    imageMapHeightRatio = (float)imageMapHeight/height;
    walls.resize(width*height);
    for(int y=0;y<height;y++)for(int x=0;x<width;x++)walls[y*width+x]=originalWall(x,y)?1:0;
    baseWalls=walls;
}

Map::~Map() {
    free(imageMapData);
    glDeleteTextures(1, &glTextureID);
    glDeleteTextures(8,tileTextures.data());
    glDeleteTextures(1, &patchTexture);
}

void Map::loadTexture() {
    tiled=false;tileTextures.fill(0);glTextureID=0;
    ground.resize(width*height*3);
    tileColumns=2;tileRows=2;preparePatch();
    if(id>=50&&loadTiles())return;
    int pngSize = 0;
    unsigned char* png = acAssets->getFile((path + "-image.png").c_str(), &pngSize);
    unsigned char* decoded = NULL;
    unsigned artWidth = imageMapWidth, artHeight = imageMapHeight;
    if (png) {
        unsigned error = lodepng_decode32(&decoded, &artWidth, &artHeight, png, pngSize);
        free(png);
        if (error) { free(decoded); decoded = NULL; artWidth = imageMapWidth; artHeight = imageMapHeight; }
    }
    const uint8_t* source = decoded ? decoded : reinterpret_cast<const uint8_t*>(imageMapData);
    // Visual-only floor samples in world orientation. Rule terrain never reads RGB.
    ground.resize(width*height*3);
    for(int y=0;y<height;y++)for(int x=0;x<width;x++){
        int xx=(flip&1)?width-1-x:x,yy=(flip&2)?y:height-1-y;
        size_t p=(size_t(yy)*artHeight/height*artWidth+size_t(xx)*artWidth/width)*4;
        for(int c=0;c<3;c++)ground[(y*width+x)*3+c]=source[p+c];
    }
    GLint maximum = 0; glGetIntegerv(GL_MAX_TEXTURE_SIZE, &maximum);
    glEnable(GL_TEXTURE_2D);
    glGenTextures(1, &glTextureID);
    glBindTexture(GL_TEXTURE_2D, glTextureID);
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR_MIPMAP_LINEAR);
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
    glTexEnvi(GL_TEXTURE_ENV, GL_TEXTURE_ENV_MODE, GL_MODULATE);
    // One lower-resolution retry if the larger visual texture cannot be uploaded.
    const unsigned scale = visual::preferredScale(artWidth, artHeight);
    for (unsigned attempt = 0; attempt < 2; ++attempt) {
        unsigned limit = attempt == 0 ? std::max(1, maximum) : std::min(1024, std::max(1, maximum));
        visual::Texture texture = visual::prepareMap(source, artWidth, artHeight, limit, scale);
        textureU = float(texture.contentWidth) / texture.width;
        textureV = float(texture.contentHeight) / texture.height;
        unsigned w = texture.width, h = texture.height;
        std::vector<uint8_t> mip = std::move(texture.rgba);
        for (unsigned level = 0;; ++level) {
            glTexImage2D(GL_TEXTURE_2D, level, GL_RGBA, w, h, 0, GL_RGBA, GL_UNSIGNED_BYTE, mip.data());
            if (w == 1 && h == 1) break;
            mip = visual::nextMip(mip, w, h); w = std::max(1u, w/2); h = std::max(1u, h/2);
        }
        GLenum error = glGetError();
        if (error == GL_NO_ERROR) break;
        __android_log_print(ANDROID_LOG_WARN, "LWGraphics", "map texture upload limit=%u failed GL=%u", limit, error);
        if (attempt == 1) { glDeleteTextures(1, &glTextureID); glTextureID = 0; }
    }
    glDisable(GL_TEXTURE_2D);
    free(decoded);
}

void Map::draw(float viewX,float viewY,float viewW,float viewH) {
    if (!glTextureID&&!tiled) return;
    glColor4f(1, 1, 1, 1);
    glBlendFunc(GL_ONE, GL_ONE_MINUS_SRC_ALPHA);

    const float u0=(flip&1)?textureU:0,u1=(flip&1)?0:textureU;
    const float v0=(flip&2)?textureV:0,v1=(flip&2)?0:textureV;
    const GLfloat texCoords[] = { u0, v0, u1, v0, u0, v1, u1, v1 };

    static const GLfloat vertices[] = {-1.0, 1.0, 0.0
                                      , 1.0, 1.0, 0.0
                                      ,-1.0,-1.0, 0.0
                                      , 1.0,-1.0, 0.0 };

    glEnable(GL_TEXTURE_2D);
    glEnableClientState(GL_VERTEX_ARRAY);
    glEnableClientState(GL_TEXTURE_COORD_ARRAY);

    if(tiled){
        for(int k=0;k<tileColumns*tileRows;k++){
            int col=k%tileColumns,row=k/tileColumns;float tw=float(width)/tileColumns,th=float(height)/tileRows;
            float wx=((flip&1)?tileColumns-1-col:col)*tw,wy=((flip&2)?row:tileRows-1-row)*th;
            if(viewW>0&&(wx>=viewX+viewW||wx+tw<=viewX||wy>=viewY+viewH||wy+th<=viewY))continue;
            float x0=-1.f+2.f*col/tileColumns,x1=x0+2.f/tileColumns,y1=1.f-2.f*row/tileRows,y0=y1-2.f/tileRows,sx=(flip&1)?-1.f:1.f,sy=(flip&2)?-1.f:1.f;
            GLfloat quad[]={x0*sx,y1*sy,0,x1*sx,y1*sy,0,x0*sx,y0*sy,0,x1*sx,y0*sy,0};auto uv=tileUV[k];
            GLfloat coords[]={uv[0],uv[1],uv[2],uv[1],uv[0],uv[3],uv[2],uv[3]};
            glBindTexture(GL_TEXTURE_2D,tileTextures[k]);glTexCoordPointer(2,GL_FLOAT,0,coords);glVertexPointer(3,GL_FLOAT,0,quad);glDrawArrays(GL_TRIANGLE_STRIP,0,4);
        }
        glVertexPointer(3,GL_FLOAT,0,vertices);
    }else{
        glBindTexture(GL_TEXTURE_2D,glTextureID);glTexCoordPointer(2,GL_FLOAT,0,texCoords);glVertexPointer(3,GL_FLOAT,0,vertices);glDrawArrays(GL_TRIANGLE_STRIP,0,4);
    }

    drawOpenings();

    glDisableClientState(GL_VERTEX_ARRAY);
    glDisableClientState(GL_TEXTURE_COORD_ARRAY);
    glDisable(GL_TEXTURE_2D);
    glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
}

bool Map::originalWall(int x, int y) {
    if(flip&1)x=(width-1)-x;
    if(flip&2)y=(height-1)-y;
    y = (height-1) - y;

    if(x < 0)
        return true;
    if(x >= width)
        return true;
    if(y < 0)
        return true;
    if(y >= height)
        return true;

    x *= imageMapWidthRatio;
    y *= imageMapHeightRatio;

    const int i = y*imageMapWidth + x;
    const unsigned int pixel = imageMapData[i];
    return (alpha(pixel) > 3);
}

bool Map::isWall(int x,int y){return x<0||y<0||x>=width||y>=height||walls[y*width+x]!=0;}
bool Map::excavate(int x,int y){
    // Keep the external enclosure and both possible flipped score strips.
    if(x<16||x>=width-16||y<4||y>=height-4||!walls[y*width+x])return false;
    walls[y*width+x]=0;opened.push_back(y*width+x);terrainRevision++;return true;
}
void Map::drawOpenings(){
    if(opened.empty()||ground.empty())return;
    if(!patchTexture){glGenTextures(1,&patchTexture);glBindTexture(GL_TEXTURE_2D,patchTexture);
        glTexParameteri(GL_TEXTURE_2D,GL_TEXTURE_MIN_FILTER,GL_NEAREST);glTexParameteri(GL_TEXTURE_2D,GL_TEXTURE_MAG_FILTER,GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D,GL_TEXTURE_WRAP_S,GL_CLAMP_TO_EDGE);glTexParameteri(GL_TEXTURE_2D,GL_TEXTURE_WRAP_T,GL_CLAMP_TO_EDGE);}
    glBindTexture(GL_TEXTURE_2D,patchTexture);
    if(uploadedRevision!=int(opened.size())){
        size_t start=uploadedRevision<0?0:std::min(opened.size(),size_t(uploadedRevision));
        for(size_t k=start;k<opened.size();k++){
            int p=opened[k],x=p%width,y=p/width,sample=-1;
            for(int radius:{1,2,4,8,16,32,64,128}){
                for(int dy=-1;dy<=1&&sample<0;dy++)for(int dx=-1;dx<=1;dx++){
                    int nx=x+dx*radius,ny=y+dy*radius;if(nx>=0&&ny>=0&&nx<width&&ny<height&&!baseWalls[ny*width+nx]){sample=ny*width+nx;break;}}
                if(sample>=0)break;
            }
            size_t at=(((height-1-y)*patchContentH/height)*patchWidth+x*patchContentW/width)*4;
            unsigned grain=(unsigned(x)*73856093u^unsigned(y)*19349663u)%13;
            for(int c=0;c<3;c++)patchPixels[at+c]=uint8_t(std::max(0,std::min(255,(sample<0?52:int(ground[sample*3+c]))+int(grain)-6)));
            patchPixels[at+3]=255;
        }
        glTexImage2D(GL_TEXTURE_2D,0,GL_RGBA,patchWidth,patchHeight,0,GL_RGBA,GL_UNSIGNED_BYTE,patchPixels.data());uploadedRevision=int(opened.size());
    }
    const GLfloat uv[]={0,0,float(patchContentW)/patchWidth,0,0,float(patchContentH)/patchHeight,float(patchContentW)/patchWidth,float(patchContentH)/patchHeight};
    glTexCoordPointer(2,GL_FLOAT,0,uv);glDrawArrays(GL_TRIANGLE_STRIP,0,4);
}

unsigned int Map::alpha(unsigned int pixel) {
    return ((pixel >> 24) & 0xFF);
}

void Map::restore(){walls=baseWalls;opened.clear();terrainRevision++;uploadedRevision=-1;std::fill(patchPixels.begin(),patchPixels.end(),0);}

void Map::preparePatch(){
    GLint maximum=0;glGetIntegerv(GL_MAX_TEXTURE_SIZE,&maximum);maximum=std::max(1,maximum);
    auto pot=[](int n){int v=1;while(v<n)v*=2;return v;};patchContentW=width;patchContentH=height;
    while(pot(patchContentW)>maximum||pot(patchContentH)>maximum){patchContentW=std::max(1,(patchContentW+1)/2);patchContentH=std::max(1,(patchContentH+1)/2);}
    patchWidth=pot(patchContentW);patchHeight=pot(patchContentH);patchTexture=0;uploadedRevision=-1;patchPixels.assign(patchWidth*patchHeight*4,0);
}
// Native-resolution source paintings; one compatible POT texture per piece.
bool Map::loadTiles(){
    const unsigned gutter=8;GLint maximum=0;glGetIntegerv(GL_MAX_TEXTURE_SIZE,&maximum);bool ok=true;
    glEnable(GL_TEXTURE_2D);glTexEnvi(GL_TEXTURE_ENV,GL_TEXTURE_ENV_MODE,GL_MODULATE);
    for(int k=0;k<tileColumns*tileRows&&ok;k++){
        int bytes=0;auto name=path+"-tile"+std::to_string(k)+".png";unsigned char* png=acAssets->getFile(name.c_str(),&bytes),*raw=nullptr;unsigned w=0,h=0;
        if(!png){ok=false;break;}unsigned error=lodepng_decode32(&raw,&w,&h,png,bytes);free(png);
        if(error||w<=gutter*2||h<=gutter*2){free(raw);ok=false;break;}
        for(int y=0;y<height/tileRows;y++)for(int x=0;x<width/tileColumns;x++){
            int gx=k%tileColumns*(width/tileColumns)+x,gy=k/tileColumns*(height/tileRows)+y,wx=(flip&1)?width-1-gx:gx,wy=(flip&2)?gy:height-1-gy;
            size_t at=(size_t(gutter+y*(h-2*gutter)/(height/tileRows))*w+gutter+x*(w-2*gutter)/(width/tileColumns))*4;
            for(int c=0;c<3;c++)ground[(wy*width+wx)*3+c]=raw[at+c];
        }
        glGenTextures(1,&tileTextures[k]);glBindTexture(GL_TEXTURE_2D,tileTextures[k]);
        glTexParameteri(GL_TEXTURE_2D,GL_TEXTURE_MIN_FILTER,GL_LINEAR_MIPMAP_LINEAR);glTexParameteri(GL_TEXTURE_2D,GL_TEXTURE_MAG_FILTER,GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D,GL_TEXTURE_WRAP_S,GL_CLAMP_TO_EDGE);glTexParameteri(GL_TEXTURE_2D,GL_TEXTURE_WRAP_T,GL_CLAMP_TO_EDGE);
        bool uploaded=false;
        for(int attempt=0;attempt<2;attempt++){
            auto t=visual::prepareMap(raw,w,h,attempt==0?std::max(1,maximum):std::min(1024,std::max(1,maximum)),1);
            const float u=float(t.contentWidth)/t.width,v=float(t.contentHeight)/t.height;
            tileUV[k]={{gutter*u/w,gutter*v/h,(w-gutter)*u/w,(h-gutter)*v/h}};
            tileContentW=int(t.contentWidth)*(w-2*gutter)/w;tileContentH=int(t.contentHeight)*(h-2*gutter)/h;tileStorageW=t.width;tileStorageH=t.height;
            unsigned mw=t.width,mh=t.height;auto mip=std::move(t.rgba);glGetError();
            for(unsigned level=0;;level++){glTexImage2D(GL_TEXTURE_2D,level,GL_RGBA,mw,mh,0,GL_RGBA,GL_UNSIGNED_BYTE,mip.data());if(mw==1&&mh==1)break;mip=visual::nextMip(mip,mw,mh);mw=std::max(1u,mw/2);mh=std::max(1u,mh/2);}
            if(glGetError()==GL_NO_ERROR){uploaded=true;break;}
        }
        free(raw);ok=uploaded;
        if(ok)__android_log_print(ANDROID_LOG_INFO,"LWGraphics","tile%d source%ux%u detail%dx%d storage%dx%d",k,w,h,tileContentW,tileContentH,tileStorageW,tileStorageH);
    }
    glDisable(GL_TEXTURE_2D);tiled=ok;
    if(!ok){glDeleteTextures(8,tileTextures.data());tileTextures.fill(0);__android_log_print(ANDROID_LOG_WARN,"LWGraphics","tiled art unavailable; compatible preview fallback");}
    return ok;
}
