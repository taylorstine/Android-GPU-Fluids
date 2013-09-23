precision mediump float;
varying vec2 itexcoord;

uniform vec2 offset;
uniform float scale;
uniform sampler2D bnd_sample;

vec4 biject( vec4 vector ){
  return vector * 2.0 - 1.0;
}

vec4 unbiject( vec4 vector ){
  return (vector + 1.0)/2.0;
}


void main(){
  gl_FragColor = unbiject(scale * biject(texture2D( bnd_sample, itexcoord + offset )));
}
