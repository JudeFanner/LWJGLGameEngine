#version 330 core
out vec4 FragColor;

in vec3 Normal;
in vec3 FragPos;

uniform vec3 lightPos;
uniform vec3 viewPos;
uniform vec3 lightColor;
uniform vec3 objectColor;

uniform vec3 fogColor;
uniform float fogStart;
uniform float fogEnd;

void main()
{
    // checkerboard pattern
    float scale = 4.0;
    vec2 checkPos = floor(Normal.yz * scale);
    float pattern = mod(checkPos.x + checkPos.y, 2.0);

    vec3 color1 = objectColor;
    vec3 color2 = vec3(1.0) - objectColor;
    vec3 checkerColor = mix(color1, color2, pattern);

    // ambient
    float ambientStrength = 0.2;
    vec3 ambient = ambientStrength * lightColor;

    // diffuse
    vec3 norm = normalize(Normal);
    vec3 lightDir = normalize(lightPos - FragPos);
    float diff = max(dot(norm, lightDir), 0.0);
    vec3 diffuse = diff * lightColor;

    // specular
    float specularStrength = 0.5;
    vec3 viewDir = normalize(viewPos - FragPos);
    vec3 reflectDir = reflect(-lightDir, norm);
    float spec = pow(max(dot(viewDir, reflectDir), 0.0), 32);
    vec3 specular = specularStrength * spec * lightColor;

    vec3 result = (ambient + diffuse + specular) * checkerColor;

    // spherical fog calculation
    float distance = length(FragPos - viewPos);
    float fogFactor = 1.0 - clamp((fogEnd - distance) / (fogEnd - fogStart), 0.0, 1.0);
    fogFactor = fogFactor * fogFactor;

    result = mix(result, fogColor, fogFactor);

    FragColor = vec4(result, 1.0);
}