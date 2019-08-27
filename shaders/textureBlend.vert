#version 150
in vec2 inPosition;//vstup z vb

uniform mat4 translace;
uniform mat4 projection;
uniform mat4 view;

// vystupy
out vec2 texCoord;

// wall
vec3 getWall(vec2 xy) {
    float x = xy.x - 0.5;
    float y = xy.y - 0.5;
    float z = 0.5;
    return vec3(x, y, z);
}

void main() {
    vec2 pos = inPosition * 2 - 1;// prepocitani z <0, 1> do <-1, 1>
    vec3 finalPos;

    finalPos = getWall(pos);

    gl_Position = projection * view * translace * vec4(finalPos, 1.0);

    texCoord = inPosition;
}
