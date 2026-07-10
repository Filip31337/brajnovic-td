#ifdef GL_ES
#define LOWP lowp
precision mediump float;
#else
#define LOWP
#endif

varying LOWP vec4 v_color;
varying vec2 v_texCoords;
uniform sampler2D u_texture;

// v_color here is repurposed by GameScreen: rgb = tint target color, a = blend strength (0 = untinted).
// Regular alpha/opacity is not needed for enemy sprites, so this is safe only for draws bound to this shader.
void main() {
    vec4 texColor = texture2D(u_texture, v_texCoords);
    vec3 tinted = mix(texColor.rgb, v_color.rgb, v_color.a);
    gl_FragColor = vec4(tinted, texColor.a);
}
