shader_type spatial;
render_mode diffuse_lambert, specular_disabled, cull_disabled, depth_prepass_alpha;
uniform bool reference_190 = false;
uniform float display_scale = 0.75;
uniform sampler2D classic_atlas : source_color, filter_linear_mipmap, repeat_disable;
#include "res://expedition_palette.gdshaderinc"
varying vec4 tint;
varying vec4 palette;
void vertex() { VERTEX *= display_scale; tint=COLOR; palette=expedition_palette(floor(COLOR.a*16.0+0.001)); }
void fragment() {
    float seat=floor(tint.a*16.0+0.001);
    vec2 cell=vec2(mod(palette.x,4.0),floor(palette.x/4.0));
    // Preserve the classic sprite's fractional edge/shadow coverage. Scissoring
    // promoted its translucent dark border to opaque, making it shimmer in motion.
    // Bound the mip level within each 64px atlas tile: never mix adjacent teams.
    vec2 footprint = max(abs(dFdx(UV)),abs(dFdy(UV))) * 64.0;
    float lod = clamp(log2(max(max(footprint.x,footprint.y),1.0)),0.0,4.0);
    vec4 tex=textureLod(classic_atlas,(UV+cell)/4.0,lod);
    ALBEDO=tex.rgb*tint.rgb*palette.yzw;
    ALPHA=tex.a*clamp(fract(tint.a*16.0)/0.32,0.0,1.0);
    ROUGHNESS=1.0;
}
