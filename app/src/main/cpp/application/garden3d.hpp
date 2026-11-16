// SPDX-License-Identifier: GPL-3.0-or-later
#pragma once
#include <vector>
class State;
namespace garden3d {
constexpr int MAP_ID=1001,ARENA_WIDTH=3264,ARENA_HEIGHT=2048,START=1000;
// Container includes non-playable margins. The original-scale mainland footprint is 10x.
bool assetsValid();
std::vector<float> godotFrame(float x,float y,float w,float h,bool spectator);
// Presentation-only completed HUD facts. False means a non-Godot match.
bool hudSnapshot(int kind,std::vector<int>& out);
// v192 viewer-filtered projection. Guests hold presentation only, never a second simulation.
std::vector<int> networkFrame(int viewer);
bool acceptNetworkFrame(const std::vector<int>& data);
void beginRemote(int viewer,int seats);
bool remoteScene(std::vector<int>& out);
void place(State&,int seed);
void surface();
void draw(float x,float y,float w,float h,bool spectator);
int status();
void resetVisual();
void beginMatch();
void sampleStep();
void pauseVisual(bool paused);
void diagnostics(double* out);
void rebuildSurface(); // GL thread, same live context; releases owned objects first.
}
