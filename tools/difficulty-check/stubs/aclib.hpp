// Test-only host asset adapter. No window or replacement game renderer.
#pragma once
#include <cmath>
#include <cstdlib>
#include <string>
#include <fstream>
#include "lodepng.hpp"
struct HostAssets {
    std::string root;
    unsigned char* getFile(const char* path,int* length) {
        std::ifstream stream(root+"/"+path,std::ios::binary|std::ios::ate);
        if(!stream){*length=0;return nullptr;}
        *length=int(stream.tellg());stream.seekg(0);
        auto data=static_cast<unsigned char*>(malloc(*length));stream.read(reinterpret_cast<char*>(data),*length);return data;
    }
};
extern HostAssets* acAssets;
