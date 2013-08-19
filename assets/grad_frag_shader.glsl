precision mediump float;
varying vec2 itexcoord;

uniform float halfrdx;
uniform sampler2D p_sample;
uniform sampler2D u_sample;

vec4 biject( vec4 vector ){
  return vector * 2.0 - 1.0;
}

vec4 unbiject( vec4 vector ){
  return (vector + 1.0)/2.0;
}

void main(){
  float pL = biject(texture2D( p_sample, itexcoord + vec2(-1.0, 0.0 ))).x;
  float pR = biject(texture2D( p_sample, itexcoord + vec2( 1.0, 0.0 ))).x;
  float pB = biject(texture2D( p_sample, itexcoord + vec2( 0.0, -1.0 ) )).x;
  float pT = biject(texture2D( p_sample, itexcoord + vec2( 0.0, 1.0) )).x;
  
  vec4 uNew = biject(texture2D( u_sample, itexcoord ));
  uNew.xy -= halfrdx * vec2( pR-pL, pT-pB);
  gl_FragColor = unbiject(uNew);
}
