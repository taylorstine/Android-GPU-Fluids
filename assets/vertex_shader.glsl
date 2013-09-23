uniform mat4 mvp_matrix;

attribute vec3 vPosition;
attribute vec2 vTexcoord;

varying highp vec2 itexcoord;
//invariant gl_Position;

uniform int gridSize;
 
void main(){
  itexcoord = vTexcoord;// float(gridSize);
  gl_Position = vPosition;//mvp_matrix * vec4( vPosition, 1.0 );
}
