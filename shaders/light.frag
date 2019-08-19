#version 150
out vec4 outColor;

void main() {
    outColor = vec4(gl_FragCoord.zzz, 1.0);
}
