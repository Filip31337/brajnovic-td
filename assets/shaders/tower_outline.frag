#ifdef GL_ES
#define LOWP lowp
precision mediump float;
#else
#define LOWP
#endif

varying LOWP vec4 v_color;
varying vec2 v_texCoords;
uniform sampler2D u_texture;
uniform vec2 u_texelSize;
// This region's own atlas sub-rect (u0, v0, u2, v2) - neighbor samples are clamped into it so the
// outline never bleeds into whatever frame happens to be packed next to this one in the sheet.
uniform vec4 u_frameBounds;
uniform vec4 u_outlineColor;

float sampleAlpha(vec2 offset) {
    vec2 coord = clamp(v_texCoords + offset, u_frameBounds.xy, u_frameBounds.zw);
    return texture2D(u_texture, coord).a;
}

void main() {
    vec4 texColor = texture2D(u_texture, v_texCoords);
    if (texColor.a > 0.1) {
        gl_FragColor = texColor * v_color;
        return;
    }

    float maxAlpha = 0.0;
    maxAlpha = max(maxAlpha, sampleAlpha(vec2( u_texelSize.x, 0.0)));
    maxAlpha = max(maxAlpha, sampleAlpha(vec2(-u_texelSize.x, 0.0)));
    maxAlpha = max(maxAlpha, sampleAlpha(vec2(0.0,  u_texelSize.y)));
    maxAlpha = max(maxAlpha, sampleAlpha(vec2(0.0, -u_texelSize.y)));
    maxAlpha = max(maxAlpha, sampleAlpha(vec2( u_texelSize.x,  u_texelSize.y)));
    maxAlpha = max(maxAlpha, sampleAlpha(vec2(-u_texelSize.x,  u_texelSize.y)));
    maxAlpha = max(maxAlpha, sampleAlpha(vec2( u_texelSize.x, -u_texelSize.y)));
    maxAlpha = max(maxAlpha, sampleAlpha(vec2(-u_texelSize.x, -u_texelSize.y)));
    maxAlpha = max(maxAlpha, sampleAlpha(vec2( u_texelSize.x * 2.0, 0.0)));
    maxAlpha = max(maxAlpha, sampleAlpha(vec2(-u_texelSize.x * 2.0, 0.0)));
    maxAlpha = max(maxAlpha, sampleAlpha(vec2(0.0,  u_texelSize.y * 2.0)));
    maxAlpha = max(maxAlpha, sampleAlpha(vec2(0.0, -u_texelSize.y * 2.0)));

    if (maxAlpha > 0.1) {
        gl_FragColor = u_outlineColor;
    } else {
        discard;
    }
}
