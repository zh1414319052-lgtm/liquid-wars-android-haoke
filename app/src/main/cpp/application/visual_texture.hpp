// SPDX-License-Identifier: GPL-3.0-or-later
#ifndef VISUAL_TEXTURE_HPP
#define VISUAL_TEXTURE_HPP
#include <vector>
#include <cstdint>
namespace visual {
struct Texture {
    unsigned width, height, contentWidth, contentHeight;
    std::vector<uint8_t> rgba;
};
// Visual-only copy. Never changes the PNG bytes used by Map::isWall.
Texture prepareMap(const uint8_t* rgba, unsigned width, unsigned height,
                   unsigned maximumTextureSize, unsigned desiredScale = 2);
unsigned preferredScale(unsigned width, unsigned height);
std::vector<uint8_t> nextMip(const std::vector<uint8_t>& rgba, unsigned width, unsigned height);
std::vector<uint8_t> roundParticle(unsigned size, float diameterPixels);
// Fixed lighting, straight-alpha sprites. No time, simulation state or RNG input.
std::vector<uint8_t> pearlParticle(unsigned size, float diameterPixels, bool highlight);
// Six baked-colour porcelain sprites,4x2 POT atlas, transparent gutters.
// diameterPixels is physical screen size, not simulation/grid size.
Texture porcelainParticles(unsigned tileSize, float diameterPixels);
}
#endif
