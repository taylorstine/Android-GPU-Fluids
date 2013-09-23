precision mediump float;
varying highp vec2 itexcoord;
uniform sampler2D texSample;

vec4 biject( vec4 vector ){
  return vector * 2.0 - 1.0;
}

vec4 unbiject( vec4 vector ){
  return (vector + 1.0)/2.0;
}

void main(){
  gl_FragColor = unbiject(biject(texture2D( texSample, itexcoord )));
  //gl_FragColor = vec4(.2, .3, .1, 1.0);
  
}
