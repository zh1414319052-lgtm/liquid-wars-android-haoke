// Original material implementation; art direction informed by Cartoon Terrain
// Studio (idlerunner00, MIT). Offline authored masks, not a screen-space image.
uniform sampler2D terrain_zones : filter_linear_mipmap, repeat_disable;
uniform sampler2D limestone : source_color, filter_linear_mipmap_anisotropic, repeat_enable;
uniform sampler2D limestone_normal : hint_normal, filter_linear_mipmap_anisotropic, repeat_enable;
uniform sampler2D earth_texture : source_color, filter_linear_mipmap_anisotropic, repeat_enable;
uniform sampler2D material_fields : filter_linear_mipmap, repeat_enable;
uniform vec2 sample_origin = vec2(-5000.0);
uniform bool sample_enabled = false;
varying vec3 stone_world;
varying vec3 stone_world_normal;

float sample_weight(vec2 p) {
    // Fixed birth anchor. Neither camera, movement, TIME nor simulation modifies it.
    float d = length((p-sample_origin)/vec2(490.0,350.0));
    float edge = texture(material_fields,p/530.0).r*0.12;
    return sample_enabled ? 1.0-smoothstep(0.80+edge,1.08+edge,d) : 0.0;
}

vec3 rock_sample(vec3 previous,vec3 wp,float authored) {
    vec2 p=vec2(wp.x,-wp.z);
    float strength=sample_weight(p);
    vec3 zones=texture(terrain_zones,p/vec2(3264.0,2048.0)).rgb;
    vec3 fields=texture(material_fields,p/256.0).rgb;
    vec3 stone=texture(limestone,p/256.0).rgb*.67;
    float cap=smoothstep(0.35,0.85,stone_world_normal.y);
    // Horizontal mineral beds track world height on real boulder faces.
    float bed=wp.y/7.8+dot(p,vec2(0.012,0.007))+(fields.r-.5)*.65;
    float joint=1.0-smoothstep(.025,.10,abs(fract(bed)-.5));
    vec3 mineral=mix(vec3(.39,.40,.37),stone,cap*.65);
    mineral*=1.0-joint*(1.0-cap)*.07;
    mineral+=vec3((fields.g-.5)*.038);
    float growth=zones.g*smoothstep(.34,.72,fields.g)*.55;
    mineral=mix(mineral,vec3(.235,.285,.135),growth);
    // Bridge and cliff remain distinguishable from boulders.
    if(authored>=.81 && authored<.86) mineral=mix(mineral,vec3(.45,.40,.33),.25);
    return mix(previous,mineral,strength);
}
