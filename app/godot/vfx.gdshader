shader_type spatial;
render_mode unshaded, cull_disabled, depth_draw_never, shadows_disabled;
varying vec4 tint;
void vertex(){tint=COLOR;}
void fragment(){
 vec2 q=(UV-.5)*2.;
 float edge=1.-smoothstep(.62,1.,length(q));
 ALBEDO=tint.rgb;
 ALPHA=tint.a*edge;
}
