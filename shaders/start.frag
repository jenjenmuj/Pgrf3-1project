#version 150
in vec3 vertColor; // input from the previous pipeline stage
in vec4 depthTexCoord;//souradnice depthTextry
in vec2 texCoord;//souradnice textury
in vec3 normal;
in vec3 lightDirection;
in vec3 viewDirection;
in float dist;
in float textureBlendFS;


uniform sampler2D textureID;
uniform sampler2D textureID1;
uniform sampler2D textureID2;
uniform sampler2D depthTexture;
uniform vec3 lightPosition;//pozice svetla
uniform float time;
uniform int mode;


vec4 position;

out vec4 outColor;// (vždy jediný) výstup z fragment shaderu

//Variables Chrome
float kA = 0.25; // koeficient odrazu okolniho svetla
float kS = 0.4;
float kD = 0.774597;
float h = 76.8;
float constantAttenuation = 0.5, linearAttenuation = 0.05, quadraticAttenuation = 0.005;
float spotCutOff = 0.5;
vec3 spotLightDirection = lightPosition;


void main() {
	vec3 NdotL = vec3 (dot(normal, lightDirection));
	vec4 ambient = vec4(1.0, 1.0, 1.0, 1.0) * kA; //bile svetlo * koeficient odrazu okolniho svetla
	vec4 diffuse = vec4(normalize(NdotL) * vec3(1.0, 1.0, 1.0) * kD, 1.0);

	vec3 halfVector = normalize(lightDirection + normalize(viewDirection)); // H = normalize( L + V );
	float NdotH = max(0, dot(normalize(normal), halfVector)); //NdotH = max(0, dot(N,H));
	vec4 specular = vec4(pow(NdotH, h)) * vec4(1.0, 1.0, 1.0, 1.0) * kS;

	// vypocet utlumu
	float att = 1.0 / (constantAttenuation + linearAttenuation * dist + quadraticAttenuation * dist * dist);


	// vypocet reflektoru
	float spotEffect = max(dot(normalize(spotLightDirection), lightDirection), 0);

	vec4 texColor = texture(textureID, texCoord);
    vec4 texColor1 = texture(textureID1, texCoord);
    vec4 texColor2 = texture(textureID2, texCoord);



	float z1 = texture(depthTexture, depthTexCoord.xy / depthTexCoord.w).r;// nutná dehomogenizace
	// r -> v light.frag uládáme gl_FragCoord.zzz, takže jsou všechny hodnoty stejné

	// aktuální "z" podle podle z pozice světla
	float z2 = depthTexCoord.z / depthTexCoord.w;

	bool shadow = z1 < z2 - 0.0001;

    if (textureBlendFS == 1) {
        texColor = mix(texColor1, texColor2, (time+1) / 2);
    }

    vec4 color = ambient + (att * ( diffuse + specular));
    color = att * (min(ambient + diffuse, 1)) + specular;

	if (shadow) {
		outColor = vec4(texColor.rgb*ambient.rgb * 0.5, 1);
	} else {
		outColor = (texColor * color);
	}
//    if (textureBlendFS == 1) {
//        outColor = mix(texColor1*color, texColor2*color, (time + 1)/2);
//    }

	//outColor = mix(texColor, vec4 (normal,1), time);
	//


}
