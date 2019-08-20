#version 150
in vec3 vertColor; // input from the previous pipeline stage
in vec4 depthTexCoord;//souradnice depthTextry
in vec2 texCoord;//souradnice textury
in vec3 normal;
in vec3 lightDirection;
in vec3 viewDirection;


uniform sampler2D textureID;
uniform sampler2D depthTexture;

vec4 position;

out vec4 outColor;// (vždy jediný) výstup z fragment shaderu

//Variables Chrome
float kA = 0.25; // koeficient odrazu okolniho svetla
float kS = 0.4;
float kD = 0.774597;
float h = 76.8;
vec3 r;


void main() {



	vec3 NdotL = vec3(dot(normal, lightDirection));
	vec4 ambient = vec4(1.0, 1.0, 1.0, 1.0) * kA; //bile svetlo * koeficient odrazu okolniho svetla
	vec4 diffuse = vec4(normalize(NdotL) * vec3(1.0, 1.0, 1.0) * kD, 1.0);


	vec3 halfVector = normalize(normalize(lightDirection) + normalize(viewDirection)); // H = normalize( L + V );
	float NdotH = max(0, dot(normalize(normal), halfVector)); //NdotH = max(0, dot(N,H));
	vec4 specular = vec4(pow(NdotH, h)) * vec4(1.0, 1.0, 1.0, 1.0) * kS;

	//float dist = length(lightDirection);



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
