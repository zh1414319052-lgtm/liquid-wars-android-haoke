shader_type spatial;
render_mode cull_disabled;
// Deliberately no TIME, VERTEX displacement, screen texture, or shared land material.
uniform float game_time = 0.0;
uniform bool reference_190 = false;
void fragment() {
    float across = UV.x;
    float along = UV.y;
    float wave = along * 2.8 + game_time * 1.8 + 0.25 * sin(along * 0.6) + across * 0.24;
    float fine = along * 5.3 + game_time * 2.3 + 0.7 * sin(across * 1.9);
    float edge = smoothstep(0.55,1.5,abs(across));
    float crest = pow(max(0.0,sin(wave)),12.0);
    float streak = pow(max(0.0,sin(fine)),20.0);
    ALBEDO = reference_190 ? mix(vec3(0.075,0.26,0.255),vec3(0.22,0.40,0.32),edge)
        : mix(vec3(0.12,0.38,0.43),vec3(0.34,0.56,0.53),edge);
    ALBEDO += vec3(0.10,0.14,0.12) * (crest * 0.55 + streak * 0.22);
    ROUGHNESS = 0.43;
    SPECULAR = 0.26;
    NORMAL_MAP = vec3(0.5 + cos(across * 2.7 + along * 0.3 - game_time * 0.5) * 0.055,
        0.5 + cos(wave) * 0.065, 1.0);
}
