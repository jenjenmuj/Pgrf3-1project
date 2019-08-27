#version 150
in vec2 texCoord;//souradnice textury

uniform sampler2D textureID;

out vec4 outColor;// (vždy jediný) výstup z fragment shaderu

void main() {

	vec4 texColor = texture(textureID, texCoord);
	outColor = (texColor );

}
