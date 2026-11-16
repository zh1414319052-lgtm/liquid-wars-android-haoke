precision highp float;
uniform sampler2D uShadow;
uniform vec4 uRiver; // native scale, Blender center, design scale
uniform vec2 uOffset;
uniform float uTime;
uniform float uShadowPass;
uniform float uKind;
uniform float uShadowTexel;
varying vec3 vWorld;
varying vec3 vNormal;
varying vec3 vColor;
varying float vRough;
varying vec3 vShadow;
vec2 packDepth(float d){vec2 v=fract(d*vec2(1.,255.));v.x-=v.y/255.;return v;}
float sampleShade(vec2 p){vec2 v=texture2D(uShadow,p).rg;float z=v.x+v.y/255.;return vShadow.z-.0016>z?.65:1.;}
float shade(){if(vShadow.x<0.||vShadow.y<0.||vShadow.x>1.||vShadow.y>1.)return 1.;vec2 s=vec2(uShadowTexel*.6);return .25*(sampleShade(vShadow.xy+s)+sampleShade(vShadow.xy-s)+sampleShade(vShadow.xy+vec2(s.x,-s.y))+sampleShade(vShadow.xy+vec2(-s.x,s.y)));}
float hash(vec2 p){return fract(sin(dot(p,vec2(127.1,311.7)))*43758.5453);}
float noise(vec2 p){vec2 i=floor(p),f=fract(p);f=f*f*(3.-2.*f);return mix(mix(hash(i),hash(i+vec2(1.,0.)),f.x),mix(hash(i+vec2(0.,1.)),hash(i+vec2(1.,1.)),f.x),f.y);}
void main(){
 if(uShadowPass>.5){gl_FragColor=vec4(packDepth(gl_FragCoord.z),0.,1.);return;}
 vec3 n=normalize(vNormal);if(!gl_FrontFacing)n=-n;
 vec3 base=vColor;float rough=vRough;
 if(uKind>.5&&uKind<1.5){
   vec2 p=(vWorld.xy-vec2(1632.,1024.))/uRiver.x+uRiver.yz;
   float q=(p.y-uOffset.y)/uRiver.w;
   float center=uRiver.w*(14.+3.5*sin(q*.19)+1.7*sin(q*.41))+uOffset.x;
   float across=p.x-center;
   float phase=p.y*2.8+uTime*1.8+.32*sin(p.y*.6)+across*.24;
   float rip=sin(phase)*.55+sin(p.y*6.4+uTime*2.6-across*.65)*.19;
   float nx=cos(across*2.7+p.y*.3-uTime*.5)*.13;
   float ny=cos(phase)*.14+cos(p.y*6.4+uTime*2.6-across*.65)*.07;
   n=normalize(vec3(nx,ny,1.));
   float edge=smoothstep(.55,1.5,abs(across));
   float crest=pow(max(0.,sin(phase+.4*sin(across*1.7+p.y*.6))),12.);
   float streak=pow(max(0.,sin(p.y*5.3+uTime*2.3+.7*sin(across*1.9))),20.);
   base=mix(vec3(.085,.265,.255),vec3(.24,.43,.35),edge)*(.98+.11*rip);
   base+=vec3(.11,.15,.125)*(crest*.60+streak*.28)*(1.-edge*.65);
   rough=.22;

 }else if(uKind<.5){
   float grit=(noise(vWorld.xy*.10)-.5)*.035+(noise(vWorld.xy*.75)-.5)*.018;
   base*=1.+grit;
 }
 vec3 light=normalize(vec3(-.40,.55,1.));
 float direct=max(0.,dot(n,light));
 float shadow=shade();
 vec3 lit=base*(vec3(.39,.44,.47)+vec3(.77,.73,.63)*direct*shadow);
 float spec=pow(max(0.,dot(n,normalize(light+vec3(0.,0.,1.)))),mix(62.,9.,rough));
 lit+=vec3(1.,.97,.86)*spec*(1.-rough)*.17*shadow;
 // Subtle leaf transmission, no neon emission or whole-scene bloom.
 if(base.g>base.r*1.15&&uKind<.5)lit+=base*.06;
 gl_FragColor=vec4(pow(max(lit,vec3(0.)),vec3(1./2.2)),1.);
}
