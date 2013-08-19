precision mediump float;
varying vec2 itexcoord;

uniform float alpha;
uniform float rBeta;

uniform sampler2D x_sample;
uniform sampler2D b_sample;
//start here

vec4 biject( vec4 vector ){
  return vector * 2.0 - 1.0;
}

vec4 unbiject( vec4 vector ){
  return (vector + 1.0)/2.0;
}

void main(){

  vec4 xLeft = biject(texture2D( x_sample, itexcoord + vec2( -1, 0 ) ));
  vec4 xRight = biject(texture2D( x_sample, itexcoord + vec2( 1, 0 ) ));
  vec4 xBottom = biject(texture2D( x_sample, itexcoord + vec2( 0, -1 ) ));
  vec4 xTop = biject(texture2D( x_sample, itexcoord + vec2( 0, 1 ) ));

  vec4 bCol = biject(texture2D( b_sample, itexcoord ));
  
  gl_FragColor = unbiject( ( xLeft + xRight + xTop + xBottom + alpha * bCol ) * rBeta );

}
