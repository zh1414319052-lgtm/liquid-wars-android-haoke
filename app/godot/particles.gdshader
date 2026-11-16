shader_type spatial;
render_mode diffuse_lambert, specular_disabled, cull_disabled;
uniform bool reference_190 = false;
uniform sampler2D classic_atlas : source_color, filter_linear;
varying vec4 tint;
void vertex() { tint=COLOR; }
void fragment() {
    float seat=floor(tint.a*16.0+0.001);
    vec2 cell=vec2(mod(seat,4.0),floor(seat/4.0));
    vec4 tex=texture(classic_atlas,(UV+cell)/4.0);
    ALBEDO=tex.rgb*tint.rgb;
    ALPHA=tex.a*clamp(fract(tint.a*16.0)/0.32,0.0,1.0);
    ALPHA_SCISSOR_THRESHOLD=0.12;
    ROUGHNESS=1.0;
}
