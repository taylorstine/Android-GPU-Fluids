attribute vec3 vPosition;
attribute vec2 vTexcoord;

varying vec2 itexcoord;
//invariant gl_Position;

uniform int gridSize;

void main(){
  itexcoord = vTexcoord / float(gridSize);
  gl_Position = vec4(vPosition, 1.0);
}
