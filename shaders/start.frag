#version 150
in vec3 vertColor; // input from the previous pipeline stage
in vec4 depthTexCoord;//souradnice depthTextry
in vec2 texCoord;//souradnice textury
in vec3 normal;
in vec3 lightDirection;
in vec3 viewDirection;
in float dist;

uniform sampler2D textureID;
uniform sampler2D depthTexture;

vec4 position;

out vec4 outColor;// (vždy jediný) výstup z fragment shaderu

//Variables Chrome
float kA = 0.25; // koeficient odrazu okolniho svetla
float kS = 0.4;
float kD = 0.774597;
float h = 76.8;
float constantAttenuation = 0.5, linearAttenuation = 0.05, quadraticAttenuation = 0.005;
float spotCutOff = 50;
vec3 spotLightDirection = lightDirection;


void main() {
	vec3 NdotL = vec3(dot(normal, lightDirection));
	vec4 ambient = vec4(1.0, 1.0, 1.0, 1.0) * kA; //bile svetlo * koeficient odrazu okolniho svetla
	vec4 diffuse = vec4(normalize(NdotL) * vec3(1.0, 1.0, 1.0) * kD, 1.0);

	vec3 halfVector = normalize(normalize(lightDirection) + normalize(viewDirection)); // H = normalize( L + V );
	float NdotH = max(0, dot(normalize(normal), halfVector)); //NdotH = max(0, dot(N,H));
	vec4 specular = vec4(pow(NdotH, h)) * vec4(1.0, 1.0, 1.0, 1.0) * kS;

	// vypocet utlumu
	float att = 1.0 / (constantAttenuation + linearAttenuation * dist + quadraticAttenuation * dist * dist);

	// vypocet reflektoru
	float spotEffect = max(dot(normalize(spotLightDirection), normalize( - normalize(lightDirection))), 0);


	// nastavuji att na 0, aby se pocitala pouze ambientni slozka barvy
	if (spotEffect < spotCutOff) {
		//att = 0.0;
	}

    vec4 texColor = texture(textureID, texCoord);

	float z1 = texture(depthTexture, depthTexCoord.xy / depthTexCoord.w).r;// nutná dehomogenizace
	// r -> v light.frag uládáme gl_FragCoord.zzz, takže jsou všechny hodnoty stejné

	// aktuální "z" podle podle z pozice světla
	float z2 = depthTexCoord.z / depthTexCoord.w;

	bool shadow = z1 < z2 - 0.0001;

	if (shadow) {
		att = 0.0;
	}

    // osvtleni s rozmazanim
    float blend = clamp((spotEffect-spotCutOff)/(1-spotCutOff), 0.0, 1.0); //orezani na rozsah <0;1>

	vec4 color = ambient + (att * (diffuse + specular));

	vec4 readyToBlend = (texColor * color);
    outColor = mix(ambient, readyToBlend , blend); //blendovani
}
