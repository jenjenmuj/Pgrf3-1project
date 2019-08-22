#version 150
in vec2 inPosition;// input from the vertex buffer

uniform mat4 projection;
uniform mat4 view;
uniform vec3 lightPosition; //pozice svetla

const float PI = 3.1415;

vec3 getSun(vec2 xy) {
    float az = xy.x * PI;
    float ze = xy.y * PI/2;// máme od -1 do 1 a chceme od -PI/2 do PI/2
    float r = 0.5;

    float x = (cos(az) * cos(ze) * r ) + lightPosition.x;// presuneme na souranice svetla
    float y = (sin(az) * cos(ze) * r ) + lightPosition.y;
    float z = (sin(ze) * r) + lightPosition.z;
    return vec3(x, y, z);

}

void main() {
    vec2 pos = inPosition * 2 - 1;// máme od 0 do 1 a chceme od -1 do 1 (funkce pro ohyb gridu s tím počítají
    vec3 finalPos;

    finalPos = getSun(pos);

    gl_Position = projection * view * vec4(finalPos, 1.0);
} 
