shader_type spatial;
render_mode cull_disabled;
// 1.98: quiet natural material, no white islands or Voronoi paving.
uniform bool reference_190=false;
uniform bool foliage=false;
uniform bool sample_enabled=false;
uniform vec2 sample_origin=vec2(-5000.0);
uniform sampler2D chalk_soil : source_color, filter_linear_mipmap_anisotropic, repeat_enable;
uniform sampler2D broadleaf : source_color, filter_linear_mipmap_anisotropic, repeat_disable;
uniform sampler2D terrain_zones : filter_linear_mipmap, repeat_disable;
uniform sampler2D material_fields : filter_linear_mipmap, repeat_enable;
varying vec3 world_pos;
varying vec3 world_normal;
void vertex(){
    world_pos=(MODEL_MATRIX*vec4(VERTEX,1.0)).xyz;
    world_normal=normalize(MODEL_NORMAL_MATRIX*NORMAL);
}
float sample_mask(vec2 p){
    float d=length((p-sample_origin)/vec2(490.0,350.0));
    return sample_enabled?1.0-smoothstep(.82,1.12,d):0.0;
}
void fragment(){
    vec2 p=vec2(world_pos.x,-world_pos.z);
    float rough=UV2.x;
    vec3 base=COLOR.rgb;
    if(reference_190){
        if(!foliage && rough>.935)base=vec3(.48514994,.55201140,.36625260);
        ALBEDO=base*vec3(.60,.66,.70);
        ROUGHNESS=foliage?.85:.90;SPECULAR=.5;
    }else{
        vec3 grain=texture(chalk_soil,p/210.0).rgb;
        vec3 fields=texture(material_fields,p/190.0).rgb;
        vec3 zones=texture(terrain_zones,p/vec2(3264.0,2048.0)).rgb;
        if(foliage){
            vec3 leaf=texture(broadleaf,UV).rgb;
            base=mix(COLOR.rgb,leaf*vec3(.48,.52,.62),.75);
            ROUGHNESS=.96;SPECULAR=.08;
        }else if(rough>.935){
            float tone=dot(grain,vec3(.2126,.7152,.0722));
            vec3 soil=vec3(.465,.445,.385)+vec3((tone-.78)*.15);
            vec3 path=vec3(.505,.479,.419)+vec3((tone-.78)*.11);
            vec3 patch=mix(soil,path,zones.r*.85);
            float moss=zones.g*(1.0-zones.r*.8)*smoothstep(.28,.76,fields.g);
            patch=mix(patch,vec3(.285,.335,.205),moss*.63);
            patch*=1.0-zones.g*.06;
            patch=mix(patch,vec3(.355,.395,.365)+vec3((tone-.78)*.10),zones.b*.26);
            vec3 legacy=vec3(.49,.43,.35)+vec3((tone-.78)*.06);
            base=mix(legacy,patch,sample_mask(p));
            ROUGHNESS=.97;SPECULAR=.04;
        }else if(rough<.81){
            float detail=(dot(grain,vec3(.333))-.78)*.09;
            base=vec3(.455,.467,.428)+vec3(detail);
            float side=1.0-smoothstep(.35,.85,world_normal.y);
            base=mix(base,vec3(.28,.33,.22),zones.g*side*.20);
            ROUGHNESS=.93;SPECULAR=.10;
        }else if(rough<.86){
            base=vec3(.48,.45,.38)+vec3((grain.r-.78)*.065);
            ROUGHNESS=.94;SPECULAR=.08;
        }else if(rough<.90){
            base=vec3(.43,.42,.36)+vec3((grain.r-.78)*.065);
            ROUGHNESS=.94;SPECULAR=.08;
        }else{
            base=vec3(.245,.205,.145);
            ROUGHNESS=.96;SPECULAR=.06;
        }
        ALBEDO=base;
    }
}
