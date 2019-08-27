#version 150
in vec2 texCoord;//souradnice textury

uniform sampler2D textureID1;
uniform sampler2D textureID2;
uniform float time;

out vec4 outColor;// (vždy jediný) výstup z fragment shaderu

void main() {

    vec4 texColor1 = texture(textureID1, texCoord);
    vec4 texColor2 = texture(textureID2, texCoord);

    outColor = mix(texColor1, texColor2, (time+1) / 2);

//    if (textureBlendFS == 1) {
//        outColor = mix(texColor1*color, texColor2*color, (time + 1)/2);
//    }

	//outColor = mix(texColor, vec4 (normal,1), time);
	//


}
