shader_type spatial;
render_mode cull_disabled;
#include "res://stone_sample.gdshaderinc"
// COLOR contains linear authored albedo, UV2.x original Blender roughness.
// No clock, deformation, screen-space filter or animated normal on solids.
uniform bool reference_190 = false;
uniform bool foliage = false;
// CC0 Aerial Grass Rock by Rob Tuytel / Poly Haven. World mapping is anchored
// across spatial chunks and camera changes; it never uses TIME or screen UVs.
uniform sampler2D meadow_albedo : source_color, filter_linear_mipmap_anisotropic, repeat_enable;
uniform sampler2D meadow_normal : hint_normal, filter_linear_mipmap_anisotropic, repeat_enable;
varying vec2 ground_uv;
void vertex() {
    stone_world = (MODEL_MATRIX * vec4(VERTEX,1.0)).xyz;
    stone_world_normal = normalize(MODEL_NORMAL_MATRIX * NORMAL);
    ground_uv = (MODEL_MATRIX * vec4(VERTEX,1.0)).xz / 256.0;
    if (!foliage && UV2.x > 0.935) {
        TANGENT = vec3(1.0,0.0,0.0);
        BINORMAL = vec3(0.0,0.0,1.0);
    }
}
void fragment() {
    vec3 base = COLOR.rgb;
    float authored = UV2.x;
    if (reference_190) {
        if (!foliage && authored > 0.935) {
            base = vec3(0.48514994,0.55201140,0.36625260);
        }
        ALBEDO = base * vec3(0.60,0.66,0.70);
        ROUGHNESS = foliage ? 0.85 : 0.90;
        SPECULAR = 0.5;
    } else {
        if (foliage) {
            base = base * vec3(0.88,0.98,1.15) + vec3(0.045,0.060,0.045);
            float sw=sample_weight(vec2(stone_world.x,-stone_world.z));
            base=mix(base,base*vec3(.95,.97,.72),sw*.70);
        } else if (authored > 0.935) {
            // Reuse real soil/gravel relief, but derive COLOR ONLY from value:
            // the photographed grass must not tint the battlefield green.
            vec3 grain = texture(meadow_albedo,ground_uv).rgb;
            vec3 broad = texture(meadow_albedo,ground_uv.yx*0.437+vec2(0.31,0.67)).rgb;
            float lightness = dot(grain,vec3(0.2126,0.7152,0.0722));
            float broad_value = dot(broad,vec3(0.2126,0.7152,0.0722));
            float detail = clamp((lightness-0.16)*0.16,-0.022,0.020);
            // Warm stone-grey and light brown earth, in linear color space.
            // Broad patches stay quiet; fine pale gravel no longer sparkles.
            vec3 earth = mix(vec3(0.46,0.39,0.31),vec3(0.53,0.47,0.39),
                             smoothstep(0.04,0.35,broad_value));
            earth += vec3(detail);
            // Preserve authored paths/banks as value, never as an olive tint.
            base = earth + vec3((dot(base,vec3(0.2126,0.7152,0.0722))-0.5)*0.12);
            NORMAL_MAP = texture(meadow_normal,ground_uv).rgb;
            NORMAL_MAP_DEPTH = 0.14;
            vec2 p=vec2(stone_world.x,-stone_world.z);
            vec3 zones=texture(terrain_zones,p/vec2(3264.0,2048.0)).rgb;
            vec3 fields=texture(material_fields,p/256.0).rgb;
            float macro_patch=texture(material_fields,p/690.0+vec2(.23,.48)).r;
            // Material islands are legible within a 400-grid camera, including
            // random coastal births; their boundary is worn, not a broad blur.
            float rockiness=smoothstep(.485,.505,fields.r*.65+macro_patch*.35);
            float stone_mix=rockiness*(1.0-zones.r*.99);
            // Reflectance budget calibrated in Android Compatibility daylight;
            // keep rock highlights below white without changing scene exposure.
            vec3 stone=texture(limestone,p/256.0).rgb*.67;
            vec3 soil=texture(earth_texture,p/256.0).rgb*.72;
            vec3 region=mix(soil,stone,stone_mix);
            // Visible worn shelf boundary and pocket growth; no uniform green wash.
            float lip=smoothstep(.06,.26,stone_mix)*(1.0-smoothstep(.38,.65,stone_mix));
            region*=1.0-lip*.07;
            float moss=zones.g*(1.0-zones.r)*smoothstep(.22,.66,fields.g);
            region=mix(region,vec3(.29,.335,.17)*(0.91+fields.b*.18),moss*.86);
            region=mix(region,soil*vec3(.89,.94,.97),zones.b*.35);
            float sw=sample_weight(p);
            base=mix(base,region,sw);
            vec3 relief=texture(limestone_normal,p/256.0).rgb;
            // Existing ground tangent uses +Godot Z, whereas map Y points -Z.
            relief.g=1.0-relief.g;
            NORMAL_MAP=mix(NORMAL_MAP,mix(vec3(.5,.5,1.0),relief,stone_mix),sw);
            NORMAL_MAP_DEPTH=mix(.14,.30,sw);
        } else if (authored < 0.81) {
            base = vec3(0.50,0.52,0.49); // chalk rock
        } else if (authored < 0.86) {
            base = vec3(0.58,0.57,0.53); // limestone bridges
        } else if (authored < 0.90) {
            base = vec3(0.51,0.49,0.43); // sandstone edges
        } else {
            base = vec3(0.28,0.24,0.18); // warm tree trunks
        }
        if (!foliage && authored < .90) {
            base=rock_sample(base,stone_world,authored);
        }
        ALBEDO = base;
        ROUGHNESS = foliage ? 0.88 : 0.92;
        SPECULAR = (!foliage && authored > 0.935) ? 0.06 : 0.20;
    }
}
