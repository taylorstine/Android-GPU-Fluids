precision mediump float;

varying vec3 icolor;
varying vec3 inormal;
varying vec2 itexcoord;

void main(){
  //gl_FragColor = vec4(0.0, 1.0, 0.0, 1.0);
  gl_FragColor = vec4( icolor, 1.0 );
  //gl_FragColor = vColor;
}
