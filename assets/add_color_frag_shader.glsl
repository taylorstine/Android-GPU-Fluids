precision mediump float;
varying vec2 itexcoord;

uniform float dt;
uniform float sigma;
uniform sampler2D forceSample;


void main(){
  /*float A = 1.0;
  vec4 force = texture2D( forceSample, itexcoord ).zw;
  float c = A * exp( -( pow(force.x - itexcoord.x, 2.0)/ 2.0*pow(sigma,2.0) +
			pow(force.y - itexcoord.y, 2.0)/ 2.0*pow(sigma,2.0) ) );
			gl_FragColor = vec4(1.0 * c, 0.0, 0.0, 1.0);*/
}
