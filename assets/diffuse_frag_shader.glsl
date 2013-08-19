precision mediump float;
varying vec2 itexcoord;

uniform float alpha;
uniform float rBeta;

uniform sampler2d x_sample;
uniform sampler2d b_sample;
//start here

void main(){

  vec4 xLeft = texture2d( x_sample, itexcoord - vec2( 1, 0 ) );
  vec4 xRight = texture2d( x_sample, itexcoord + vec2( 1, 0 ) );
  vec4 xBottom = texture2d( x_sample, itexcoord - vec2( 0, 1 ) );
  vec4 xTop = texture2d( x_sample, itexcoord + vec2( 0, 1 ) );

  vec4 bCol = sampler2d( b_sample, itexcoord );
  
  gl_FragColor =( xLeft + xRight + xTop + xBottom + alpha * bCol ) * rBeta;

}
