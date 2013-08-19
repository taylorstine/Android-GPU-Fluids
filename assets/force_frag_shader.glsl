precision mediump float;
varying vec2 itexcoord;

uniform sampler2D forceSample;
uniform sampler2D uSample;
uniform float forceScale;

vec4 unbiject( vec4 vector ){
  return (vector + 1.0)/2.0;
}

void main(){

  vec2 force = texture2D( forceSample, itexcoord ).xy;
  vec4 u = texture2D( uSample, itexcoord);
  gl_FragColor = u + forceScale * vec4(force, 0.0, 0.0);
}
