#version 150
in vec2 inPosition;//vstup z vb

uniform mat4 projection;
uniform mat4 view;
uniform vec3 lightPosition;//pozice svetla
uniform float time;
uniform int mode;
uniform mat4 lightVP;
uniform vec3 eyePosition;
uniform mat4 model;
uniform mat4 translace;
uniform float textureBlend;


// vystupy
out vec4 depthTexCoord;
out vec2 texCoord;
out vec3 normal; // <<N>>
out vec3 lightDirection; // light direction vector <<L>>
out vec3 viewDirection;
out vec3 NdotL;
out float dist;
out float textureBlendFS;

const float PI = 3.1415;

// PARAMETRIC SURFACE
// wall
vec3 getWall(vec2 xy) {
    float x = xy.x - 0.5;
    float y = xy.y - 0.5;
    float z = 0.5;
    return vec3(x, y, z);
}
// snake
vec3 getSnake(vec2 xy) {
    float s = xy.x;
    float t =(xy.y + 1) * PI;

    float x = (1 - s) * (3 + cos(t)) * cos(2 * PI * s);
    float y = (1 - s) * (3 + cos(t)) * sin(2 * PI * s);
    float z = 3 * s + (1 - s) * sin(t);
    return vec3(x, y, z)*0.2;// *0.2 je zmenseni telesa

}

vec3 getMobiusBand(vec2 xy) {
    float s = (xy.y + 1) * PI;//mame od -1 do 1 a potrebujeme od 0 do 2*PI
    float t = xy.x * 0.5;// mame od -1 do 1 a potrebujeme od -0.5 do 0.5

    float x = 2 * cos(s) + t * cos(s / 2);
    float y = 2 * sin(s) + t * cos(s / 2);
    float z = t * sin(s / 2);
     return vec3(x, y, z);// *0.2 je zmenseni telesa
}

// PS with SPhERICAL COORDS
// ohnutí gridu do podoby elipsouidu (nepouzito)
vec3 getElipsoid(vec2 xy) {
    float az = xy.x * PI;// pridame pohzbujici se povrch
    float ze = xy.y * PI/2;// máme od -1 do 1 a chceme od -PI/2 do PI/2
    float r = 0.5;

    float x = cos(az) * cos(ze) * r;
    float y = 1.5 * sin(az) * cos(ze) * r;
    float z = sin(ze) * r;

    return vec3(x, y, z);
}

vec3 getBall(vec2 xy) {
    float az = xy.x * PI;
    float ze = xy.y * PI/2;// máme od -1 do 1 a chceme od -PI/2 do PI/2
    float r = 0.5;

    float x = cos(az) * cos(ze) * r;// presuneme na souranice svetla
    float y = sin(az) * cos(ze) * r;
    float z = sin(ze) * r;
    return vec3(x, y, z);

}

// Elephant head podle prednasky
vec3 getElephantHead(vec2 xy) {
    float ze = (xy.x + 1) / 2 * PI;// máme od -1 do 1 a chceme od 0 do PI
    float az = (xy.y + 1) * PI;// máme od -1 do 1 a chceme od 0 do 2*PI
    float r = (3 + cos(4 * az));

    float x = sin(ze) * cos(az) * r;
    float y = sin(ze) * sin(az) * r;
    float z = cos(ze) * r;

    return vec3(x, y, z) * 0.2;
}

vec3 selection(vec2 pos, int imode) {
    vec3 result;
    switch (imode) {
        default :
        case 0: return getBall(pos);
        case 1: return getElipsoid(pos);
        case 2: return getWall(pos);
        case 3: return getMobiusBand(pos);
        case 4: return getElephantHead(pos);
        case 5: return getSnake(pos);
    }
}

//normala k parametrickzm telesum
vec3 getNormal(vec2 xy, int imode) {
    float delta = 0.001;
    vec3 u = selection(xy + vec2(delta, 0), imode) - selection(xy - vec2(delta, 0), imode);
    vec3 v = selection(xy + vec2(0, delta), imode) - selection(xy - vec2(0, delta), imode);
    return cross(u, v);
}


void main() {

    textureBlendFS = textureBlend;

    vec2 pos = inPosition * 2 - 1;// prepocitani z <0, 1> do <-1, 1>
    vec3 finalPos;

    // mat3 normalMatrix = inverse(transpose(mat3(view)));


    if (mode == 0) {
        finalPos = mix(getBall(pos), getWall(pos), (time+ 1) / 2);// +1 /2 je prepocitani kvuli Cos a jeho zapornym hodnotam
        normal = mix(getNormal(pos, 0), getNormal(pos, 2), (time + 1) / 2);//<<N>>
    } else {
        finalPos = getWall(pos);
        normal = getNormal(pos, 2);//<<N>>
    }

    gl_Position = projection * view * translace * vec4(finalPos, 1.0);

    lightDirection = normalize(lightPosition - finalPos);// light direction vector <<L>>

    viewDirection = eyePosition - finalPos;

    texCoord = inPosition;

    dist = length(lightPosition - finalPos);

    depthTexCoord = lightVP * translace * vec4(finalPos, 1.0); //lightVP je kam svetlo kouka * pozice

    depthTexCoord.xyz = ((depthTexCoord.xyz) + 1) / 2;// obrazovka má rozsahy <-1;1> *** nepodarilo se premstit do FS

}
