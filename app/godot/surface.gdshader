shader_type spatial;
render_mode cull_disabled;
// COLOR contains linear authored albedo, UV2.x original Blender roughness.
// No clock, deformation, screen-space filter or animated normal on solids.
uniform bool reference_190 = false;
uniform bool foliage = false;
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
        } else if (authored > 0.935) {
            // Retain the authored roads, banks and meadow variations, but lift
            // muddy midtones into a quiet sage/ivory range independently of units.
            base = mix(vec3(0.52,0.57,0.50), base, 0.22);
        } else if (authored < 0.81) {
            base = vec3(0.50,0.52,0.49); // chalk rock
        } else if (authored < 0.86) {
            base = vec3(0.58,0.57,0.53); // limestone bridges
        } else if (authored < 0.90) {
            base = vec3(0.51,0.49,0.43); // sandstone edges
        } else {
            base = vec3(0.28,0.24,0.18); // warm tree trunks
        }
        ALBEDO = base;
        ROUGHNESS = foliage ? 0.88 : 0.92;
        SPECULAR = 0.20;
    }
}
