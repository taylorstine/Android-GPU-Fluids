precision mediump float;
varying highp vec2 itexcoord;
uniform float dt;
uniform float rdx;

uniform sampler2D x_sample;
uniform sampler2D u_sample;

vec4 biject( vec4 vector ){
  return vector * 2.0 - 1.0;
}

vec4 unbiject( vec4 vector ){
  return (vector + 1.0)/2.0;
}

vec4 bilinearInterp( in sampler2D x_sample,
		    in vec2 position ){
  float xInt = floor(position.x);
  float yInt = floor(position.y);
  float xFrac = fract(position.x);
  float yFrac = fract(position.y);
  
  vec4 f00 = biject(texture2D( x_sample, vec2( xInt, yInt ) ));
  vec4 f10 = biject(texture2D( x_sample, vec2( xInt+1.0, yInt ) ));
  vec4 f01 = biject(texture2D( x_sample, vec2( xInt, yInt+1.0 ) ));
  vec4 f11 = biject(texture2D( x_sample, vec2( xInt+1.0, yInt+1.0 ) ));
  vec4 interp_val =
    f00 * (1.0-xFrac) * (1.0-yFrac) + 
    f10 * xFrac * (1.0-yFrac) +
    f01 * yFrac * (1.0-xFrac) +
    f11 * xFrac * yFrac;
  
  return unbiject( interp_val );
  
}

void main()
{
  //itexcoord = floor( itexcoord, .5 );
  //vec2 position = itexcoord -
  //dt * rdx * biject(texture2D( u_sample, itexcoord )).xy;
  //gl_FragColor = bilinearInterp( x_sample, position );
  //vec2 tc = itexcoord* 64.0;
  gl_FragColor = texture2D( u_sample, itexcoord + vec2(.2, 0) );
  //gl_FragColor = vec4(itexcoord.x, itexcoord.y, 0.0, 1.0);
  //gl_FragColor = vec4(itexcoord.x * 64.0, itexcoord.y * 64.0, 0.0, 1.0);
}
//write biject for all shaders i * 2 - 1
