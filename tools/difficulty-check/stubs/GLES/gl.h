// Link-only no-op API for unused Map draw methods. Never produces a screenshot.
#pragma once
using GLuint=unsigned;using GLenum=unsigned;using GLint=int;using GLfloat=float;
enum {GL_MAX_TEXTURE_SIZE,GL_TEXTURE_2D,GL_TEXTURE_MIN_FILTER,GL_LINEAR_MIPMAP_LINEAR,GL_TEXTURE_MAG_FILTER,GL_LINEAR,GL_TEXTURE_WRAP_S,GL_CLAMP_TO_EDGE,GL_TEXTURE_WRAP_T,GL_TEXTURE_ENV,GL_TEXTURE_ENV_MODE,GL_MODULATE,GL_RGBA,GL_UNSIGNED_BYTE,GL_ONE,GL_ONE_MINUS_SRC_ALPHA,GL_VERTEX_ARRAY,GL_TEXTURE_COORD_ARRAY,GL_FLOAT,GL_TRIANGLE_STRIP,GL_SRC_ALPHA,GL_NEAREST};
#define GL_NO_ERROR 0
inline void glDeleteTextures(int,const GLuint*){}
inline void glGetIntegerv(int,GLint* p){*p=2048;}
inline void glEnable(int){} inline void glDisable(int){}
inline void glGenTextures(int,GLuint* p){*p=1;}
inline void glBindTexture(int,GLuint){}
inline void glTexParameteri(int,int,int){} inline void glTexEnvi(int,int,int){}
inline void glTexImage2D(int,int,int,unsigned,unsigned,int,int,int,const void*){}
inline GLenum glGetError(){return GL_NO_ERROR;}
inline void glColor4f(float,float,float,float){} inline void glBlendFunc(int,int){}
inline void glEnableClientState(int){} inline void glDisableClientState(int){}
inline void glTexCoordPointer(int,int,int,const void*){} inline void glVertexPointer(int,int,int,const void*){}
inline void glDrawArrays(int,int,int){}
