#version 150
in vec3 vertColor; // input from the previous pipeline stage
in vec4 depthTexCoord;//souradnice depthTextry
in vec2 texCoord;//souradnice textury
in vec3 normal;
in vec3 light;
in vec3 viewDirection;
in vec3 NdotL;

uniform sampler2D textureID;
uniform sampler2D depthTexture;

vec4 position;

out vec4 outColor;// (vždy jediný) výstup z fragment shaderu

void main() {

	vec4 ambient = vec4(1.0, 0.0, 0.0, 1.0);
	vec4 diffuse = vec4(normalize(NdotL) * vec3(0.0, 1.0, 0.0), 1.0);

	vec3 halfVector = normalize(normalize(light) + normalize(viewDirection));
	float NdotH = dot(normalize(normal), halfVector);
	vec4 specular = vec4(pow(NdotH, 16) * vec4(0.0, 0.0, 1.0, 1.0));

	vec4 color = ambient + diffuse + specular;

	vec4 texColor = texture(textureID, texCoord);

	// nejbližší pixel z pohledu světla
	float z1 = texture(depthTexture, depthTexCoord.xy / depthTexCoord.w).r;// nutná dehomogenizace
	// r -> v light.frag uládáme gl_FragCoord.zzz, takže jsou všechny hodnoty stejné

	// aktuální "z" podle podle z pozice světla
	float z2 = depthTexCoord.z / depthTexCoord.w;

	bool shadow = z1 < z2 - 0.0001;

	if (shadow) {
		//		outColor = vec4(1, 0, 0, 1);
		outColor = texColor * ambient;
	} else {
		//		outColor = vec4(0, 1, 0, 1);
		outColor = texColor * color;
	}

}
