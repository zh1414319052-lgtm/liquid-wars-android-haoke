#include "visual_texture.hpp"
#include "lodepng.hpp"
int main(int argc,char** argv){if(argc!=2)return 2;auto t=visual::porcelainParticles(64,5,10);return lodepng_encode32_file(argv[1],t.rgba.data(),t.width,t.height);}
