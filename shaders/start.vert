#version 150
in vec2 inPosition;//vstup z vb

uniform mat4 projection;
uniform mat4 view;
uniform vec3 lightPosition;//pozice svetla
uniform float time;
uniform int mode;
uniform mat4 lightVP;

// vystupy
out vec4 depthTexCoord;
out vec2 texCoord;
out vec3 normal;
out vec3 light;
out vec3 viewDirection;
out vec3 NdotL;

const float PI = 3.1415;

// vztvorit novy shader pro slunicko, kde se bude zobrazovat teleso zvlast, budem posilat souradnice kamerz

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

// PARAMETRIC SURFACE
// wall
vec3 getWall(vec2 xy) {
    float x = xy.x - 0.5;
    float y = xy.y - 0.5;
    float z = 0.5;// + cos(time) pohybuje se v ose z nahoru a dolu
    return vec3(x, y, z);
}
// snake
vec3 getSpiningSnake(vec2 xy) {
    //float s = xy.x + ((sin(time))*0.1); - had se toci nahoru a dolu
    float s = xy.x;
    float t =(xy.y + 1) * PI;

    float x = (1 - s) * (3 + cos(t)) * cos(2 * PI * s);
    float y = (1 - s) * (3 + cos(t)) * sin(2 * PI * s);
    float z = 3 * s + (1 - s) * sin(t);
    vec3 pom = vec3(x, y, z) * 0.1;// *0.2 je zmenseni telesa
    pom = rotate(pom, vec3(0, 0, 1), time);// rotace v pocatku kolem osy z pred posunutim na souradnice
    pom.x += 0.5;// posunuti telesa
    pom.y += 0.5;
    pom.z += 1.5;
    return pom;
}

vec3 getMobiusBand(vec2 xy) {
    float s = (xy.y + 1) * PI;//mame od -1 do 1 a potrebujeme od 0 do 2*PI
    float t = xy.x * 0.5;// mame od -1 do 1 a potrebujeme od -0.5 do 0.5

    float x = 2 * cos(s) + t * cos(s / 2);
    float y = 2 * sin(s) + t * cos(s / 2);
    float z = t * sin(s / 2);
    vec3 pom = vec3(x, y, z) * 0.2;// *0.2 je zmenseni telesa
    //pom = rotate(pom, vec3(0,0, 1), time); // rotace v pocatku kolem osy z
    pom.x += 1;// posunuti telesa
    pom.y -= 1;
    pom.z += 0;
    return pom;
}

// PS with SPhERICAL COORDS
// ohnutí gridu do podoby elipsouidu (nepouzito)
vec3 getElipsoid(vec2 xy) {
    float az = xy.x * PI + time;// pridame pohzbujici se povrch
    float ze = xy.y * PI/2;// máme od -1 do 1 a chceme od -PI/2 do PI/2
    float r = 0.5;

    float x = cos(az) * cos(ze) * r + 1;
    float y = 1.5 * sin(az) * cos(ze) * r - 1;
    float z = sin(ze) * r;

    return vec3(x, y, z);
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

// Elephant head podle prednasky
vec3 getElephantHead(vec2 xy) {
    float ze = (xy.x + 1) / 2 * PI;// máme od -1 do 1 a chceme od 0 do PI
    float az = (xy.y + 1) * PI + time;// máme od -1 do 1 a chceme od 0 do 2*PI
    float r = (3 + cos(4 * az)) * 0.1;

    float x = sin(ze) * cos(az) * r;
    float y = sin(ze) * sin(az) * r;
    float z = cos(ze) * r - 1;
    return vec3(x, y, z);
}

// mouse
vec3 getMouse(vec2 xy) {
    float t = (xy.x + 1) / 2 * PI;// máme od -1 do 1 a chceme od 0 do PI
    float s = (xy.y + 1) * PI;// máme od -1 do 1 a chceme od 0 do 2*PI
    float r = (2 + cos(2 * t) * sin(s)) * 0.2;// *0.1 zmenseni celeho telesa
    float x = sin(r) * cos(s) * r - 1;
    float y = sin(r) * sin(s) * r  + 1;
    float z = cos(t) * r;
    vec3 pom = vec3(x, y, z);
    float uhel = 1.570;// 90 stupnu v radianech
    return rotate(pom, vec3(-1, 0, 0), uhel);
}

// PS with CZLINDRIC COODRS
// broken vase
vec3 getBrokenVase(vec2 xy) {
    float t = xy.x * PI;// máme od -1 do 1 a chceme od -PI do PI
    float s = (xy.y + 1) / 2 * 1.5 * PI;;// máme od -1 do 1 a chceme od 0 do 1.5*PI
    float r = cos(2*t)+2;

    float x = r * cos(s) - 1;
    float y = r * sin(s) - 1;
    float z = t;
    vec3 pom = vec3(x, y, z) * 0.2;
    pom.x -= 1;
    pom.y -= 1;
    pom.z -= 0.5;
    return pom;
}

// get ToyTop nepouzity
vec3 getToyTop(vec2 xy) {
    float t = xy.x * PI;// máme od -1 do 1 a chceme od -pi do pi
    float s = (xy.y + 1) * PI;;// máme od -1 do 1 a chceme od 0 do 2*PI
    float r = 1 + cos(t);

    float x = r * cos(s) - 1;
    float y = r * sin(s) - 1;
    float z = t;
    vec3 pom = vec3(x, y, z) * 0.2;
    pom.x -= 1;
    pom.y += 1;
    pom.z -= 0.5;
    return pom;
}

// Amphore
vec3 getAmphore(vec2 xy) {
    float t = xy.x * 1.3 * PI;// máme od -1 do 1 a chceme od -1.3*pi do 1.3*pi
    float s = (xy.y + 1) * PI;;// máme od -1 do 1 a chceme od 0 do 2*PI
    float r = 1.5+sin(t);

    float x = r * cos(s) + 1;
    float y = r * sin(s) - 1;
    float z = t*cos(t)+sin(t);
    vec3 pom = vec3(x, y, z) * 0.2;
    pom.x -= 1;
    pom.y += 1;
    pom.z -= 0.5;
    return pom;
}
vec3 selection(vec2 pos) {
    vec3 result;
    switch (mode) {
        default :
        case 0: return getWall(pos);
        case 1: return getSpiningSnake(pos);
        case 2: return getElephantHead(pos);
        case 3: return getMouse(pos);
        case 4: return getBrokenVase(pos);
        case 5: return getAmphore(pos);
        case 6: return getMobiusBand(pos);
        case 7: return getSun(pos);
    }
}

//normala k parametrickzm telesum
vec3 getNormal(vec2 xy) {
    float delta = 0.001;
    vec3 u = selection(xy + vec2(delta, 0)) - selection(xy - vec2(delta, 0));
    vec3 v = selection(xy + vec2(0, delta)) - selection(xy - vec2(0, delta));
    return cross(u, v);
}


void main() {

    vec2 pos = inPosition * 2 - 1;// prepocitani z <0, 1> do <-1, 1>
    vec3 finalPos;

    finalPos = selection(pos);
    normal = getNormal(pos);
    gl_Position = projection * view * vec4(finalPos, 1.0);

    light = lightPosition - finalPos;// light direction vector
    NdotL = vec3(dot(normal, light));// spocitat ve frag shaderu / max (ndotL, 0) ///poslat si L - vektor ke svetlu  a N normala

    // získání pozice kamery z view matice
    // (kamera je pohled třetí osoby a tudíž její pozice je v počátku - proto nutné použít view matici)
    mat4 invView = inverse(view);
    vec3 eyePosition = vec3(invView[3][0], invView[3][1], invView[3][2]);

    viewDirection = eyePosition - finalPos;

    texCoord = inPosition;

    depthTexCoord = lightVP * vec4(finalPos, 1.0);
    depthTexCoord.xyz = (depthTexCoord.xyz + 1) / 2;// obrazovka má rozsahy <-1;1>
}
