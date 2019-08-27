#version 150
in vec2 inPosition;// input from the vertex buffer
in vec3 inColor;

uniform float time;
uniform mat4 viewLight;
uniform mat4 projLight;
uniform int mode;
uniform vec3 lightPosition;
uniform mat4 translaceLight;


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

void main() {
    vec2 pos = inPosition * 2 - 1;// máme od 0 do 1 a chceme od -1 do 1 (funkce pro ohyb gridu s tím počítají
    vec3 finalPos;
    if (mode == 0) {
        finalPos = mix(getBall(pos), getWall(pos), (time+ 1) / 2);// +1 /2 je prepocitani kvuli Cos a jeho zapornym hodnotam
    } else {
        finalPos = getWall(pos);
    }

    gl_Position = projLight * viewLight * translaceLight * vec4(finalPos, 1.0);
} 
