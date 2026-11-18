shader_type spatial;
render_mode unshaded, cull_disabled, depth_draw_never;
uniform int affected_mask = 0;
uniform vec2 gravity_center = vec2(1632.0,-1024.0);
uniform float pixel_world = 0.2;
varying float visible_alpha;
void vertex(){
    int seat=int(floor(COLOR.a*16.0+0.001));
    visible_alpha=((affected_mask & (1<<seat))!=0)?clamp(fract(COLOR.a*16.0)/0.32,0.0,1.0):0.0;
    vec3 origin=MODEL_MATRIX[3].xyz;
    vec2 delta=gravity_center-origin.xz;
    float dist=length(delta);
    vec2 forward=delta/max(dist,0.001);
    vec2 side=vec2(-forward.y,forward.x);
    float strand=min(dist,3.2+fract(origin.x*0.17+origin.z*0.31)*1.8);
    vec2 offset=forward*UV.y*strand+side*(UV.x-0.5)*pixel_world*1.25;
    // Explicit projection keeps per-instance scale from shrinking the thin ribbon.
    POSITION=PROJECTION_MATRIX*VIEW_MATRIX*vec4(origin+vec3(offset.x,0.04,offset.y),1.0);
}
void fragment(){
    float edge=1.0-smoothstep(0.18,0.5,abs(UV.x-0.5));
    ALBEDO=vec3(0.35,0.22,0.52);
    ALPHA=visible_alpha*edge*mix(0.55,0.04,UV.y);
}
