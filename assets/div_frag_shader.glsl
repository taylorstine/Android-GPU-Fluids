precision mediump float;
varying vec2 itexcoord;

uniform float halfrdx;
uniform sampler2D tex_sample;

vec4 biject( vec4 vector ){
  return vector * 2.0 - 1.0;
}

vec4 unbiject( vec4 vector ){
  return (vector + 1.0)/2.0;
}

void main(){
  vec4 wL = biject(texture2D( tex_sample, itexcoord + vec2( -1.0, 0.0 )));
  vec4 wR = biject(texture2D( tex_sample, itexcoord + vec2( 1.0, 0.0 )));
  vec4 wB = biject(texture2D( tex_sample, itexcoord + vec2( 0.0, -1.0 )));
  vec4 wT = biject(texture2D( tex_sample, itexcoord + vec2( 0.0, 1.0 )));

  gl_FragColor = unbiject(vec4(halfrdx * ((wR.x - wL.x) + (wT.y - wB.y) )));

}
