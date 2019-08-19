#version 150
in vec2 inPosition;// input from the vertex buffer


uniform mat4 projection;
uniform mat4 view;
uniform vec3 lightPosition; //pozice svetla
//uniform mat4 lightVP;

const float PI = 3.1415;

// vystupy
//out vec4 depthTexCoord;
//out vec2 texCoord;
//out vec3 normal;
//out vec3 light;
//out vec3 viewDirection;
//out vec3 NdotL;

vec3 getSun(vec2 xy) {
    float az = xy.x * PI;
    float ze = xy.y * PI/2;// máme od -1 do 1 a chceme od -PI/2 do PI/2
    float r = 0.5;

    float x = cos(az) * cos(ze) * r + lightPosition.x;// presuneme na souranice svetla
    float y = sin(az) * cos(ze) * r + lightPosition.y;
    float z = sin(ze) * r + lightPosition.z;
    return vec3(x, y, z);

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

    gl_Position = projection * view * vec4(finalPos, 1.0);

    //light = lightPosition - finalPos;// light direction vector
   // NdotL = vec3(dot(normal, light));// spocitat ve frag shaderu / max (ndotL, 0) ///poslat si L - vektor ke svetlu  a N normala

    // získání pozice kamery z view matice
    // (kamera je pohled třetí osoby a tudíž její pozice je v počátku - proto nutné použít view matici)
   // mat4 invView = inverse(view);
   // vec3 eyePosition = vec3(invView[3][0], invView[3][1], invView[3][2]);

   // viewDirection = eyePosition - finalPos;

   // texCoord = inPosition;

 //   depthTexCoord = lightVP * vec4(finalPos, 1.0);
  //  depthTexCoord.xyz = (depthTexCoord.xyz + 1) / 2;// obrazovka má rozsahy <-1;1>
} 
