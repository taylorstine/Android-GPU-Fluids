package com.tstine.fluidapp;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import android.util.Log;

import android.opengl.GLES20;
import android.opengl.GLU;

import java.nio.IntBuffer;

import android.content.Context;
import android.content.res.AssetManager;

import java.lang.Exception;
import java.lang.StackTraceElement;

import java.io.IOException;
import java.io.BufferedReader;
import java.io.InputStreamReader;

/**
	 ShaderLoader class, everything is prepared and loaded
	 here for the shader
 */
class ShaderLoader{
	public Context mCtx;
	private static final String TAG = "FluidRenderer";
	
	public ShaderLoader( Context context ){
		this.mCtx = context;
	}

	public int load(String vertex_filename,
												 String fragment_filename) throws RuntimeException{

		String vertex_code, fragment_code;
		int vertex_handle, fragment_handle;
		int program_handle;

		try{
			vertex_code = assetFileReader(vertex_filename);
		}catch (IOException e){
			Log.e(TAG, "ERROR OPENING SHADER CODE FILE: " + vertex_filename );
			throw new RuntimeException();
		}
		try{
			fragment_code = assetFileReader(fragment_filename);
		}catch(IOException e){
			Log.e(TAG, "ERROR OPENING FRAGMENT CODE FILE: " + fragment_filename);
			throw new RuntimeException();
		}
		try{
		vertex_handle = load_shader(vertex_code,
																GLES20.GL_VERTEX_SHADER);
		}catch(RuntimeException e){
			throw new RuntimeException("Vertex shader compile failed: " + vertex_filename );
		}

		try{
		fragment_handle = load_shader(fragment_code,
																	GLES20.GL_FRAGMENT_SHADER);
		}catch(RuntimeException e){
			throw new RuntimeException("Frag shader compile failed: " + fragment_filename );
		}

		program_handle = load_program( vertex_handle, fragment_handle );
		Log.d(TAG, "Program for " + vertex_filename + " " + fragment_filename + " loaded!");
		
		//call glDeleteShader here - it's still attached to the program so it won't be 
		//erased until it is unbound from the program
		return program_handle;
	}


	private String assetFileReader(String filename) throws IOException{
		BufferedReader reader =
			new BufferedReader(new InputStreamReader(mCtx.getAssets().open(filename)));
		StringBuffer sb = new StringBuffer();
		String line = null;
		while((line = reader.readLine()) != null){
			sb.append(line).append("\n");
		}
		reader.close();
		return sb.toString();
	}

	private int load_shader(String shader_code, int type) throws RuntimeException{
		int shader = GLES20.glCreateShader( type );
		if (shader == 0 ) throw new RuntimeException( "Shader creation failed");
		GLES20.glShaderSource( shader, shader_code );
		GLES20.glCompileShader( shader );
		int[] compiled = {100};
		GLES20.glGetShaderiv( shader, GLES20.GL_COMPILE_STATUS, compiled, 0 );
		while( compiled[0] == GLES20.GL_FALSE ){
			String shader_failure_info = GLES20.glGetShaderInfoLog( shader );
			Log.d(TAG, "Shader Failure info:\n " + shader_failure_info );
			GLES20.glDeleteShader( shader );
			throw new RuntimeException( "Shader Compile Failed");
		}
		return shader;
	}

	private int load_program(int vertex_handle, int fragment_handle) throws RuntimeException {
		int program_handle;
		program_handle = GLES20.glCreateProgram();
		if( program_handle == 0 ) throw new RuntimeException("Program creation failed!");
		GLES20.glAttachShader( program_handle, vertex_handle );
		GLES20.glAttachShader( program_handle, fragment_handle );
		GLES20.glLinkProgram( program_handle );
		int[] compiled = {100};
		GLES20.glGetProgramiv( program_handle,
													 GLES20.GL_LINK_STATUS, 
													 compiled,
													 0 );
		while( compiled[0] == GLES20.GL_FALSE ){
			Log.d(TAG, "Linking shader failed!");
			String program_failure_info = GLES20.glGetProgramInfoLog( Shader.program_handle);
			Log.d(TAG, "Program Failure Info:\n" + program_failure_info );
			throw new RuntimeException( "Linking Program Failed " );
		}
		return program_handle;
	}

}
