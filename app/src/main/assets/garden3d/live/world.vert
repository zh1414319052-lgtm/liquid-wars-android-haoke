precision highp float;
attribute vec3 aPos;
attribute vec3 aNormal;
attribute vec3 aColor;
attribute vec2 aMaterial;
uniform vec4 uView;
uniform vec4 uShadowView;
uniform float uTime;
uniform float uShadowPass;
uniform float uKind;
varying vec3 vWorld;
varying vec3 vNormal;
varying vec3 vColor;
varying float vRough;
varying vec3 vShadow;
void main(){
 vec3 p=aPos;
 float bend=sin(uTime*1.4+p.x*.016+p.y*.011)+.35*sin(uTime*2.1+p.y*.06);
 p.xy+=vec2(.82,.31)*aMaterial.x*bend;
 vNormal=normalize(aNormal+vec3(bend*.055,0.,0.)*aMaterial.x);
 vWorld=p;vColor=aColor;vRough=aMaterial.y;
 vec2 light=p.xy+vec2(.40,-.55)*p.z;
 vShadow=vec3((light-uShadowView.xy)/uShadowView.zw,.5-p.z/500.);
 if(uShadowPass>.5)gl_Position=vec4(vShadow.xy*2.-1.,-p.z/250.,1.);
 else gl_Position=vec4((p.xy-uView.xy)/uView.zw*2.-1.,-p.z/250.,1.);
}
