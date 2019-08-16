#version 150
in vec2 inPosition;// input from the vertex buffer
uniform float time;
uniform mat4 viewLight;
uniform mat4 projLight;
uniform int mode;
uniform vec3 lightPosition;

const float PI = 3.1415;

mat4 rotationMatrix(vec3 axis, float angle) {
    axis = normalize(axis);
    float s = sin(angle);
    float c = cos(angle);
    float oc = 1.0 - c;

    return mat4(oc * axis.x * axis.x + c, oc * axis.x * axis.y - axis.z * s, oc * axis.z * axis.x + axis.y * s, 0.0,
    oc * axis.x * axis.y + axis.z * s, oc * axis.y * axis.y + c, oc * axis.y * axis.z - axis.x * s, 0.0,
    oc * axis.z * axis.x - axis.y * s, oc * axis.y * axis.z + axis.x * s, oc * axis.z * axis.z + c, 0.0,
    0.0, 0.0, 0.0, 1.0);
}

vec3 rotate(vec3 v, vec3 axis, float angle) {
    mat4 m = rotationMatrix(axis, angle);
    return (m * vec4(v, 1.0)).xyz;
}

vec3 getSun(vec2 xy) {
    float az = xy.x * PI;
    float ze = xy.y * PI/2;// máme od -1 do 1 a chceme od -PI/2 do PI/2
    float r = 0.5;

    float x = cos(az) * cos(ze) * r + lightPosition.x;// presuneme na souranice svetla
    float y = sin(az) * cos(ze) * r + lightPosition.y;
    float z = sin(ze) * r + lightPosition.z;
    return rotate(vec3(x, y, z), vec3(0, 0, 1), time);// rotujeme podle pocatku a osy z

}

void main() {
    vec2 pos = inPosition * 2 - 1;// máme od 0 do 1 a chceme od -1 do 1 (funkce pro ohyb gridu s tím počítají
    vec3 finalPos;
    finalPos = getSun(pos);

//    vec3 pom = getSun(finalPos.xy);
//
//    finalPos.x += pom.x;
//    finalPos.y += pom.y;
//    finalPos.z += pom.z;

    gl_Position = projLight * viewLight * vec4(finalPos, 1.0);
} 
