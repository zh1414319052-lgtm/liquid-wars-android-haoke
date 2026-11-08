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

#ifndef MAP_HPP
#define MAP_HPP

#include <aclib.hpp>
#include <GLES/gl.h>
#include <string>
#include <sstream>
#include <vector>
#include <cstdint>
#include <array>
#include "info.hpp"

class Map {
    private:
        unsigned int* imageMapData;
        unsigned int imageMapWidth;
        unsigned int imageMapHeight;
        float imageMapWidthRatio;
        float imageMapHeightRatio;
        GLuint glTextureID = 0;
        float textureU = 1, textureV = 1;
        std::string path;
        int flip;
        std::vector<uint8_t> walls,baseWalls,patchPixels,ground;
        std::vector<int> opened;
        GLuint patchTexture=0;
        int patchWidth=1024,patchHeight=512,patchContentW=800,patchContentH=480;
        int terrainRevision=0,uploadedRevision=-1;
        std::array<GLuint,8> tileTextures{};
        std::array<std::array<float,4>,8> tileUV{};
        int tileColumns=2,tileRows=2;
        bool tiled=false;int tileContentW=0,tileContentH=0,tileStorageW=0,tileStorageH=0;
        bool loadTiles();
        void preparePatch();
        bool originalWall(int x,int y);
        void drawOpenings();
    public:
        const int width,height,id;
        Map(int mapID, int transform=0);
        int transform() const { return flip; }
        ~Map();
        void loadTexture();
        void draw(float viewX=0,float viewY=0,float viewW=0,float viewH=0);
        std::array<int,5> textureInfo() const {return {{tiled?tileColumns*tileRows:0,tileContentW,tileContentH,tileStorageW,tileStorageH}};}
        bool isWall(int width, int height);
        bool excavate(int x,int y);
        void restore();
        int revision() const {return terrainRevision;}
        const std::vector<int>& openings() const {return opened;}
        unsigned int alpha(unsigned int pixel);
};

#endif
