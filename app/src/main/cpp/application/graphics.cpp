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

#include "graphics.hpp"
#include "visual_texture.hpp"
#include "visual_palette.hpp"
#include <algorithm>
#include <cstdint>
#include <cmath>
#include <cstring>
#include <android/log.h>
#include <mutex>
#include "render_camera.hpp"

namespace {
std::mutex cameraMutex;
float cameraX=0,cameraY=0,cameraW=WIDTH,cameraH=HEIGHT;
bool cameraSpectator=false,drawSpectator=false;
GLuint particleTexture = 0;
float pointPixels = 1, halfWidth = 1, halfHeight = 1;
// 4 vertices/particle, at most65532 vertices per16-bit indexed draw.
const size_t BATCH_CAPACITY = 16383;
std::vector<GLfloat> quadPositions, quadUVs;
std::vector<GLubyte> quadColours;
std::vector<GLushort> quadIndices;
std::vector<GLubyte> quadTeams;
size_t capacity = 0;

void allocateBatch(size_t n) {
    if (capacity >= n) return;
    n=std::min(BATCH_CAPACITY,((n+4095)/4096)*4096);
    quadPositions.resize(n*8); quadUVs.resize(n*8); quadColours.resize(n*16); quadIndices.resize(n*6);
    quadTeams.resize(n,255);
    const unsigned indices[] = {0,1,2,0,2,3};
    for(size_t i=0;i<n;i++) {
        for(int k=0;k<6;k++)quadIndices[i*6+k]=GLushort(i*4+indices[k]);
    }
    capacity = n;
}
void legacyPoints() {
    for(size_t i=0;i<state->dots.size();i++) {
        state->points[i*3]=state->dots[i]->x;state->points[i*3+1]=state->dots[i]->y;
        float rgb[3];visual::teamColour(state->dots[i]->team,state->dots[i]->health/float(MAX_HEALTH),rgb);
        for(int c=0;c<3;c++)state->colours[i*4+c]=rgb[c];
        state->colours[i*4+3]=state->items->concealedFrom(*state,state->me,state->dots[i]->team)&&!drawSpectator?0:state->items->active(state->dots[i]->team,Items::INVISIBLE)?.45f:1;
    }
    glColorPointer(4,GL_FLOAT,0,state->colours.data());glVertexPointer(3,GL_FLOAT,0,state->points.data());
    glDrawArrays(GL_POINTS,0,GLsizei(state->dots.size()));
}
void drawParticles() {
    if(state->dots.empty())return;
    glEnableClientState(GL_VERTEX_ARRAY);glEnableClientState(GL_COLOR_ARRAY);
    bool round = particleTexture != 0;
    if(round) allocateBatch(std::min(state->dots.size(),BATCH_CAPACITY));
    if(!round)legacyPoints();
    else {
        glEnable(GL_TEXTURE_2D);glBindTexture(GL_TEXTURE_2D,particleTexture);
        glEnableClientState(GL_TEXTURE_COORD_ARRAY);
        glTexCoordPointer(2,GL_FLOAT,0,quadUVs.data());glVertexPointer(2,GL_FLOAT,0,quadPositions.data());
        glColorPointer(4,GL_UNSIGNED_BYTE,0,quadColours.data());
        // Each team/health/effect combination is identical across thousands of droplets.
        // Compute it once per frame; preserve the previous byte rounding and effect order.
        GLubyte colours[6][MAX_HEALTH+1][4];
        for(int team=0;team<6;team++){
            auto& fx=*state->items;
            bool freeze=fx.active(team,Items::FREEZE),phase=fx.active(team,Items::PHASE),invincible=fx.active(team,Items::INVINCIBLE);
            for(int health=0;health<=MAX_HEALTH;health++){
                GLubyte level=GLubyte(std::lround(255*visual::healthLevel(health/float(MAX_HEALTH))));
                GLubyte* rgba=colours[team][health];rgba[0]=rgba[1]=rgba[2]=level;rgba[3]=255;
                if(freeze){rgba[0]=105;rgba[1]=205;rgba[2]=255;}
                if(phase){rgba[0]=210;rgba[1]=170;rgba[2]=255;rgba[3]=155;}
                if(invincible){rgba[0]=255;rgba[1]=245;rgba[2]=155;}
                if(fx.enabled&&fx.active(team,Items::INVISIBLE))rgba[3]=!drawSpectator&&fx.concealedFrom(*state,state->me,team)?0:115;
            }
        }
        for(size_t base=0;base<state->dots.size();base+=BATCH_CAPACITY) {
            size_t count=std::min(BATCH_CAPACITY,state->dots.size()-base);
            for(size_t i=0;i<count;i++) {
                Dot* dot=state->dots[base+i];float x=dot->x,y=dot->y;
                const GLfloat xy[]={x-halfWidth,y-halfHeight,x+halfWidth,y-halfHeight,x+halfWidth,y+halfHeight,x-halfWidth,y+halfHeight};
                std::memcpy(quadPositions.data()+i*8,xy,sizeof(xy));
                const int team=std::max(0,std::min(5,int(dot->team)));
                if(quadTeams[i]!=team){
                    const float u=float(team%4)*.25f,v=float(team/4)*.5f;
                    const GLfloat uv[]={u,v,u+.25f,v,u+.25f,v+.5f,u,v+.5f};
                    std::memcpy(quadUVs.data()+i*8,uv,sizeof(uv));quadTeams[i]=GLubyte(team);
                }
                const GLubyte* rgba=colours[team][std::min(MAX_HEALTH,int(dot->health))];
                for(int v=0;v<4;v++)std::memcpy(quadColours.data()+i*16+v*4,rgba,4);
            }
            glDrawElements(GL_TRIANGLES,GLsizei(count*6),GL_UNSIGNED_SHORT,quadIndices.data());
        }
        glDisableClientState(GL_TEXTURE_COORD_ARRAY);glDisable(GL_TEXTURE_2D);
    }
    glDisableClientState(GL_VERTEX_ARRAY);glDisableClientState(GL_COLOR_ARRAY);
}
void uploadParticle(GLuint& texture) {
    GLint maximum=0;glGetIntegerv(GL_MAX_TEXTURE_SIZE,&maximum);
    unsigned tileSize=64;
    while(tileSize>=8 && tileSize*4>unsigned(std::max(0,maximum)))tileSize/=2;
    if(tileSize<8)return; // Points remain usable if an atlas is unsupported.
    auto sprite=visual::porcelainParticles(tileSize,pointPixels);
    if(!texture)glGenTextures(1,&texture);
    glBindTexture(GL_TEXTURE_2D,texture);
    glTexParameteri(GL_TEXTURE_2D,GL_TEXTURE_MIN_FILTER,GL_LINEAR);
    glTexParameteri(GL_TEXTURE_2D,GL_TEXTURE_MAG_FILTER,GL_LINEAR);
    glTexParameteri(GL_TEXTURE_2D,GL_TEXTURE_WRAP_S,GL_CLAMP_TO_EDGE);
    glTexParameteri(GL_TEXTURE_2D,GL_TEXTURE_WRAP_T,GL_CLAMP_TO_EDGE);
    glTexEnvi(GL_TEXTURE_ENV,GL_TEXTURE_ENV_MODE,GL_MODULATE);
    glTexImage2D(GL_TEXTURE_2D,0,GL_RGBA,sprite.width,sprite.height,0,GL_RGBA,GL_UNSIGNED_BYTE,sprite.rgba.data());
    GLenum error=glGetError();
    if(error!=GL_NO_ERROR) {
        __android_log_print(ANDROID_LOG_WARN,"LWGraphics","porcelain atlas unavailable GL=%u; points fallback",error);
        glDeleteTextures(1,&texture);texture=0;
    } else {
        __android_log_print(ANDROID_LOG_INFO,"LWGraphics","porcelain atlas %ux%u, body %.2fpx, single-pass batches",sprite.width,sprite.height,pointPixels);
    }
}
}

void setRenderCamera(float x,float y,float w,float h,bool spectator){
    if(!std::isfinite(x)||!std::isfinite(y)||!std::isfinite(w)||!std::isfinite(h)||w<1||h<1)return;
    std::lock_guard<std::mutex> guard(cameraMutex);cameraX=x;cameraY=y;cameraW=w;cameraH=h;cameraSpectator=spectator;
}

void setColour(int p);

void onSurfaceCreated() {
    if(state == NULL)
        return;

    glHint(GL_PERSPECTIVE_CORRECTION_HINT, GL_FASTEST);
    glShadeModel(GL_SMOOTH);

    glEnable(GL_BLEND);
    glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

    // onSurfaceCreated belongs to a fresh EGL context; old names were released with it.
    particleTexture = 0;
    capacity = 0;
    quadTeams.clear();

    state->map->loadTexture();
}

void onDrawFrame() {
    if(state == NULL)
        return;

    state->currentlyDrawing = true;
    float cx,cy,cw,ch;{std::lock_guard<std::mutex> guard(cameraMutex);cx=cameraX;cy=cameraY;cw=cameraW;ch=cameraH;drawSpectator=cameraSpectator;}
    cw=std::min(cw,2.f*state->map->width);ch=std::min(ch,2.f*state->map->height);
    cx=cw>state->map->width?(state->map->width-cw)/2:std::max(0.f,std::min(float(state->map->width)-cw,cx));
    cy=ch>state->map->height?(state->map->height-ch)/2:std::max(0.f,std::min(float(state->map->height)-ch,cy));
    if(state->map->width>WIDTH)halfHeight=halfWidth*ch*state->displayWidth/(cw*std::max(1,state->displayHeight));
    acLib->glOrthogonal(cx,cx+cw,cy,cy+ch,-1,1);

    glClear(GL_COLOR_BUFFER_BIT);
    glLoadIdentity();
    glColor4f(1, 1, 1, 1);

    glPushMatrix();
    glTranslatef(state->map->width/2.f,state->map->height/2.f,0);
    glScalef(state->map->width/2.f,state->map->height/2.f,1);
    state->map->draw(cx,cy,cw,ch);
    glPopMatrix();

    glTranslatef(.5f, .5f, 0); // Particle center matches its authority pixel center.

    drawParticles();

    const GLfloat line[] = { 0, 0, 0
                           , 0, 1, 0
                           , 1, 0, 0
                           , 1, 1, 0 };
    for(int p = 0; p < 6; p++) {
        glLoadIdentity();
        // Keep score bars on the flipped image's reserved13px frame strip.
        const float edge=cw - ((5-p) * 2) - 2;
        glTranslatef(cx+((state->map->transform()&1)?cw-edge-2:edge),cy,0);
        float s = 1 - (float)state->players[p].score/std::max(1,int(state->dots.size()));
        s = s*s*s;
        glScalef(2,ch*(1-s),1);

        setColour(p);

        glEnableClientState(GL_VERTEX_ARRAY);
        glVertexPointer(3, GL_FLOAT, 0, &line);
        glDrawArrays(GL_TRIANGLE_STRIP, 0, 4);
        glDisableClientState(GL_VERTEX_ARRAY);
    }

    glLoadIdentity();
    glTranslatef(cx+((state->map->transform()&1)?cw-2:0),cy,0);
    glScalef(2,ch*(1.0-state->timeSidebar),1);

    setColour(state->me);

    glEnableClientState(GL_VERTEX_ARRAY);
    glVertexPointer(3, GL_FLOAT, 0, &line);
    glDrawArrays(GL_TRIANGLE_STRIP, 0, 4);
    glDisableClientState(GL_VERTEX_ARRAY);

    state->currentlyDrawing = false;
}

void setColour(int p) {
    float rgb[3];visual::teamColour(p,1.f,rgb);
    glColor4f(rgb[0],rgb[1],rgb[2],1);
}

void onSurfaceChanged(int width, int height) {
    if(state == NULL)
        return;

    glViewport(0, 0, width, height);
    glClearColor(visual::BACKGROUND[0]/255.f,visual::BACKGROUND[1]/255.f,visual::BACKGROUND[2]/255.f,1);
    state->displayWidth = width;
    state->displayHeight = height;
    acLib->glOrthogonal(0, WIDTH, 0, HEIGHT, -1, 1);
    float pointSize = (float)width/WIDTH;
    if((float)height/HEIGHT > pointSize)
        pointSize = (float)height/HEIGHT;
    pointPixels = std::max(2.25f,pointSize*1.28f + .70f);
    glPointSize(pointPixels); // Compatible fallback if the coverage texture cannot be created.
    halfWidth = (pointPixels+1.8f)*.5f*WIDTH/std::max(1,width);
    halfHeight = (pointPixels+1.8f)*.5f*HEIGHT/std::max(1,height);
    // Clear earlier GL errors so a failed map upload cannot disable particle fallback.
    glGetError();
    uploadParticle(particleTexture);
}
