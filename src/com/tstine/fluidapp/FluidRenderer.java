package com.tstine.fluidapp;
//start here- it's time to compile
//you also need to fix the on user interaction thing
//to work with an array list
//note - an area of slow down may be from changing shaders
//you're not advecting color!

import android.os.Bundle;

import android.app.Activity;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import android.opengl.GLSurfaceView;
import android.opengl.GLES20;
import android.opengl.GLU;
import android.opengl.Matrix;

import java.nio.IntBuffer;
import java.nio.FloatBuffer;
import java.nio.ByteBuffer;
import java.nio.ShortBuffer;
import java.nio.*;

import android.util.Log;

import java.io.*;
import java.lang.*;
import java.util.Calendar;
import java.util.Random;
import java.util.ArrayList;
import java.util.Queue;
import java.util.LinkedList;
import java.util.Iterator;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.lang.Math;

import android.content.Context;
import android.content.res.AssetManager;

import java.lang.Exception;
import java.lang.StackTraceElement;


public class FluidRenderer implements GLSurfaceView.Renderer{
	public static final String TAG = "FluidRenderer";

	//Debug information
	private  final boolean DEBUG = true;
	private  boolean mDebugTouched = false;
	private int mDebugAdvectCount = 0;
	//end debug

	public  final int mGridSize = 64;

	public Context mCtx;
	public float mMx=0.0f, mMy=0.0f;
	public float mOmx=0.0f, mOmy=0.0f; 

	public  boolean mTouched = false;
	
	private  int mFrames = 0;
	private  long T0 = 0;

	private float[] mMvp_matrix = new float[16];
	private float[] mProjection_matrix = new float[16];
	private float[] mModelview_matrix = new float[16];

	private int mMvp_matrix_handle;

	public int mScreenWidth = 0;
	public int mScreenHeight = 0;

	private  final int SIZEOF_FLOAT = 4;
	private  final int SIZEOF_SHORT = 2;
	private  final int SIZEOF_INT = 4;
	private  final int SIZEOF_BYTE = 1;
	
	//vbo index constants
	//0 - quadIndicesBuffer	//1 - mQuadVertexBuffer
	//2 - line1IndicesBuffer	//3 - line1VertexBuffer
	//4 - line2IndicesBuffer	//5 - line2VertexBuffer
	//6 - line3IndicesBuffer	//7 - line3VertexBuffer
	//8 - line4IndicesBuffer	//9 - line4VertexBuffer
	private  final int QUAD_IND = 0, QUAD_VERT = 1,
		LINE1_IND = 2, LINE1_VERT = 3, LINE2_IND = 4, LINE2_VERT = 5,
		LINE3_IND = 6, LINE3_VERT = 7, LINE4_IND = 8, LINE4_VERT = 9;
	private  int[] mVboIds = new int[10];

	//texture index constants
	//0 - u //1 - u0
	//2 - p //3 - p0
	//4 - c //5 - c0
	private  final int U_TEX = 0, U0_TEX= 1, P_TEX= 2, DIV_TEX= 3,
		C_TEX= 4, C0_TEX= 5, P_TEMP_TEX = 6, FORCE_TEX = 7;
	private  int[] mTexIds = new int[8];

	//only one FBO
	private  int[] mFboIds = new int[1];

	//only one renderbuffer
	private int[] mRenderbuffer = new int[1];
	
	//shader index constants
	//0 - advection shader
	//1 - jacobi shader
	//2 - force shader
	//3 - divergence shader
	//4 - gradient shader
	//5 - boundary shader
	//6 - color injector shader
	private  final int ADVECT = 0, JACOBI = 1, FORCE = 2,
		DIV = 3, GRAD = 4, BND = 5, COLOR = 6, RENDER = 7, CLEAR = 8;
	private  int[] mShaderIds = new int[9];

	//system parameters
	private  final float mDx = 1.0f/mGridSize;
	private  final float mDt = 100.0f;
	private  final float mDiff = 0.1f;
	private  final float mVisc = 0.1f;
	private  final float mForceScale = 5.0f;
	private  final float mSourceAdd = 1.0f;
	private  final float mForceRadius = 5.0f;
	private  final int NUM_JACOBI_ITERS = 20;

	private  Queue<Force> mForceList =
		new ConcurrentLinkedQueue<Force>();
	
	public FluidRenderer(Context context){
		this.mCtx = context;
	}

	@Override
	public void onSurfaceCreated(GL10 gl, EGLConfig config){
		GLES20.glClearColor(0.0f, 0.0f, 1.0f, 1.0f);

		initializeVBO();
		initializeTextures();
		initializeFBO();
		initializeRenderbuffer();
		initializeShaderPrograms();
		//clearAllTextures();

		if( DEBUG ){
			ByteBuffer data = ByteBuffer.allocate(10 * 10 *4);
			int i = 10;
			int r = 0, g = 255;
			int c;
			while(data.position() < data.capacity()){
				if(i % 10 == 0 ){
					c = r;
					r = g;
					g = r;
				}
				data.put((byte)r);
				data.put((byte)g);
				data.put((byte)0);
				data.put((byte)255);
			}
			data.flip();
			GLES20.glBindTexture( GLES20.GL_TEXTURE_2D, mTexIds[U_TEX] );
			/*			GLES20.glTexSubImage2D( GLES20.GL_TEXTURE_2D, 0,
															0, mGridSize-11,
															10, 10, GLES20.GL_RGBA,
															GLES20.GL_UNSIGNED_BYTE,
															data );*/
		}

	}
	@Override 
	public void onDrawFrame(GL10 gl){
		if(DEBUG){
			if (mDebugTouched){
				simulate();
				calculate_fps(); 
				mDebugTouched = false;
			}
		}else{
			simulate();
			calculate_fps(); 
		}
		render(); 
		
	}

	@Override
	public void onSurfaceChanged(GL10 gl, int width, int height){
		mScreenWidth = width;
		mScreenHeight = height;
		GLES20.glViewport( 0, 0, width, height );
		float aspect = (float)width/height;
		Matrix.setIdentityM(mProjection_matrix, 0);
		Matrix.orthoM( mProjection_matrix,
									 0,
									 -1, 1,
									 -1, 1,
									 0.1f, 100.0f );
	}
	
	private  void initializeVBO(){

		int[] lineIndices = {0, 1};
		int[] quadIndices = { 0, 1, 2, 3};

		//(position, tex_coord, position, texcoord....)
		float[] line1Vertices = {	0.0f, 0.0f, 0.0f,
																0.0f, 0.0f,
																1.0f, 0.0f, 0.0f,
																(float)mGridSize, 0.0f };

		float[] line2Vertices = { 1.0f, 0.0f, 0.0f,
															(float)mGridSize, 0.0f,
															1.0f, 1.0f, 0.0f,
															(float)mGridSize, (float)mGridSize };
		float[] line3Vertices = { 1.0f, 1.0f, 0.0f,
															(float)mGridSize, (float)mGridSize,
															0.0f, 1.0f, 0.0f,
															0.0f, (float)mGridSize };
		float[] line4Vertices = { 0.0f, 1.0f, 0.0f,
															0.0f, (float)mGridSize,
															0.0f, 0.0f, 0.0f,
															0.0f, 0.0f };

		/*		float[] quadVertices = { 	-1.0f, -1.0f, 0.0f, 
															1.0f, 1.0f, 
															1.0f, -1.0f, 0.0f,  
															(float)(mGridSize-1), 1.0f,
															1.0f, 1.0f, 0.0f,
															(float)(mGridSize-1), (float)(mGridSize-1),
															-1.0f, 1.0f, 0.0f,
															1.0f, (float)(mGridSize-1) };*/
		float[] quadVertices = { 	-.75f, -.75f, 0.0f, 
															0.0f, 0.0f,
															.75f, -.75f, 0.0f,  
															mGridSize+10, 0.0f,
															.75f, .75f, 0.0f,
															mGridSize+10, mGridSize+10,
															-.75f, .75f, 0.0f,
															0.0f, mGridSize+10 };


		int coordsPerVertex = 3;
		int texcoordsPerVertex = 2;
		int verticesPerLine = 2;
		int verticesPerQuad = 4;
		
		int quadAttribLen = (coordsPerVertex + texcoordsPerVertex) * verticesPerQuad;
		int lineAttribLen = (coordsPerVertex + texcoordsPerVertex) * verticesPerLine;

		GLES20.glGenBuffers( 10, mVboIds, 0 ); 

		createIndicesBuffer( quadIndices, verticesPerQuad, QUAD_IND );
		createVerticesBuffer( quadVertices, quadAttribLen, QUAD_VERT );
		
		createIndicesBuffer( lineIndices, verticesPerLine, LINE1_IND );
		createVerticesBuffer( line1Vertices, lineAttribLen, LINE1_VERT );

		createIndicesBuffer( lineIndices, verticesPerLine, LINE2_IND );
		createVerticesBuffer( line2Vertices, lineAttribLen, LINE2_VERT );

		createIndicesBuffer( lineIndices, verticesPerLine, LINE3_IND );
		createVerticesBuffer( line3Vertices, lineAttribLen, LINE3_VERT );

		createIndicesBuffer( lineIndices, verticesPerLine, LINE4_IND );
		createVerticesBuffer( line4Vertices, lineAttribLen, LINE4_VERT );

		GLES20.glBindBuffer( GLES20.GL_ELEMENT_ARRAY_BUFFER, 0 );
		GLES20.glBindBuffer( GLES20.GL_ARRAY_BUFFER, 0 );
		
	}
	private  void createIndicesBuffer(int [] data,
																					int vertsPerPoly,
																					int index ){
		IntBuffer buffer = ByteBuffer.allocateDirect( vertsPerPoly * SIZEOF_INT )
			.order( ByteOrder.nativeOrder() ).asIntBuffer();
		buffer.put( data ).position(0);
		GLES20.glBindBuffer( GLES20.GL_ELEMENT_ARRAY_BUFFER, mVboIds[index]);
		GLES20.glBufferData( GLES20.GL_ELEMENT_ARRAY_BUFFER,
												 vertsPerPoly * SIZEOF_INT, buffer,
												 GLES20.GL_STATIC_DRAW );
	}
	private  void createVerticesBuffer( float[] data,
																						int attribLen,
																						int index){
		FloatBuffer buffer = ByteBuffer.allocateDirect( attribLen * SIZEOF_FLOAT)
			.order( ByteOrder.nativeOrder() ).asFloatBuffer();
		buffer.put( data ).position(0);
		GLES20.glBindBuffer( GLES20.GL_ARRAY_BUFFER, mVboIds[index] );
		GLES20.glBufferData( GLES20.GL_ARRAY_BUFFER,
												 attribLen * SIZEOF_FLOAT, buffer,
												 GLES20.GL_STATIC_DRAW );
	}

	private  void initializeTextures(){
		ByteBuffer data = ByteBuffer.allocate(mGridSize* mGridSize*4);
		if( DEBUG ){
			int cval = 10;
			int i = 0;
			int r = 0, g = 255;
			int c;
			while(data.position() < data.capacity()){
				if(i % 10 == 0 ){
					c = r;
					r = g;
					g = c;
				}
				data.put((byte)r);
				data.put((byte)g);
				data.put((byte)0);
				data.put((byte)255);
				i++;
			}
			data.flip();
		}
		GLES20.glPixelStorei( GLES20.GL_UNPACK_ALIGNMENT, 1);

		GLES20.glGenTextures( mTexIds.length, mTexIds, 0 );
		for(int i = 0; i< mTexIds.length; i++ ){
			createTexture( GLES20.GL_TEXTURE0 + i, mTexIds[i], data );
		}
		GLES20.glBindTexture( GLES20.GL_TEXTURE_2D, 0 );	 
	}

	private  void createTexture( int activeTex, int texId, Buffer data){
		GLES20.glActiveTexture( activeTex );
		GLES20.glBindTexture( GLES20.GL_TEXTURE_2D, texId );
		GLES20.glTexImage2D( GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, 
												 mGridSize, mGridSize, 0, GLES20.GL_RGBA,
												 GLES20.GL_UNSIGNED_BYTE, data );
		GLES20.glTexParameteri( GLES20.GL_TEXTURE_2D, 
														GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE );
		GLES20.glTexParameteri( GLES20.GL_TEXTURE_2D, 
														GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE );
		GLES20.glTexParameteri( GLES20.GL_TEXTURE_2D,
														GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR );
		GLES20.glTexParameteri( GLES20.GL_TEXTURE_2D,
														GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR );
	}

	private void initializeRenderbuffer(){
		GLES20.glGenRenderbuffers( 1, mRenderbuffer, 0 );
		GLES20.glBindRenderbuffer( GLES20.GL_RENDERBUFFER, mRenderbuffer[0] );
		GLES20.glRenderbufferStorage( GLES20.GL_RENDERBUFFER, GLES20.GL_DEPTH_COMPONENT16,
																	mGridSize, mGridSize );
	}
	private void initializeFBO(){
		GLES20.glGenFramebuffers( 1, mFboIds, 0 );
	}

	private void initializeShaderPrograms(){
		ShaderLoader loader = new ShaderLoader( mCtx );
		if(DEBUG){
			Log.d(TAG, "Shader id before: " + mShaderIds[ADVECT] );
		}

		mShaderIds[ADVECT] = loader.load( "simulate_vertex_shader.glsl",
																			"advect_frag_shader.glsl");
		if(DEBUG){
			Log.d(TAG,"Shader id after: " + mShaderIds[ADVECT] );
			if(!GLES20.glIsProgram( mShaderIds[ADVECT] ))
				 throw new RuntimeException("This isn't a program!");
		}
		mShaderIds[JACOBI] = loader.load( "simulate_vertex_shader.glsl",
																			"jacobi_frag_shader.glsl");
		mShaderIds[FORCE] = loader.load( "simulate_vertex_shader.glsl",
																		 "force_frag_shader.glsl" );
		mShaderIds[DIV] = loader.load( "simulate_vertex_shader.glsl",
																	 "div_frag_shader.glsl" );
		mShaderIds[GRAD] = loader.load( "simulate_vertex_shader.glsl",
																		"grad_frag_shader.glsl" );
		mShaderIds[BND] = loader.load( "simulate_vertex_shader.glsl",
																	 "boundary_frag_shader.glsl" );
		mShaderIds[COLOR] = loader.load( "simulate_vertex_shader.glsl",
																		 "add_color_frag_shader.glsl" );
		mShaderIds[RENDER] = loader.load( "render_vertex_shader.glsl",
																			"render_frag_shader.glsl");
		mShaderIds[CLEAR] = loader.load( "simulate_vertex_shader.glsl",
																		 "clear_frag_shader.glsl" );
	}

	private void clearAllTextures(){
		//In the shader, biject the values back
		GLES20.glBindFramebuffer( GLES20.GL_FRAMEBUFFER, mFboIds[0] );

		for(int i = 0; i < mTexIds.length; i++ ){
			clearTexture( mTexIds[i], .5f, 0.5f, 0.5f, 1.0f );
		}
		//clearTexture( mTexIds[C_TEX], 0.0f, 0.0f, 0.0f, 1.0f );

		GLES20.glBindFramebuffer( GLES20.GL_FRAMEBUFFER, 0 );
	}

	/**Note: assumes a Framebuffer object is bound
	 */
	private void clearTexture(int texId, float r, float g, float b, float a){
		//or you can use glClear
		/*GLES20.glUseProgram( mShaderIds[CLEAR]);
		GLES20.glFramebufferTexture2D( GLES20.GL_FRAMEBUFFER,
																	 GLES20.GL_COLOR_ATTACHMENT0,
																	 GLES20.GL_TEXTURE_2D, texId, 0 );
		int clearColorLoc = GLES20.glGetUniformLocation( mShaderIds[CLEAR],
																										 "clearColor" );
		GLES20.glUniform4f( clearColorLoc, r, g, b, a );
		checkFBOStatus();
		drawQuad();
		GLES20.glUseProgram( 0 );*/

		float[] clearColor = new float[4];
		GLES20.glGetFloatv(GLES20.GL_COLOR_CLEAR_VALUE, clearColor, 0);
		GLES20.glClearColor( r, g, b, a );
		GLES20.glFramebufferTexture2D( GLES20.GL_FRAMEBUFFER,
																	 GLES20.GL_COLOR_ATTACHMENT0,
																	 GLES20.GL_TEXTURE_2D, texId, 0 );
		GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);

		GLES20.glClearColor(clearColor[0], clearColor[1],
												clearColor[2], clearColor[3]);
	}
	

	public void render(){
		GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);

		GLES20.glUseProgram(mShaderIds[RENDER]);
		set_mvp_matrix();
		int sampleLoc = GLES20.glGetUniformLocation(mShaderIds[RENDER],
																								"texSample");
		int gridSizeLoc = GLES20.glGetUniformLocation( mShaderIds[RENDER],
																									 "gridSize" );
		GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
		GLES20.glBindTexture( GLES20.GL_TEXTURE_2D, mTexIds[U_TEX]);
		GLES20.glUniform1i(sampleLoc, 0 );
		GLES20.glUniform1i(gridSizeLoc, mGridSize );
		drawQuad(mShaderIds[RENDER]);
		GLES20.glUseProgram(0);

	}
	
	public void simulate(){
		float alpha, rBeta;
		
		GLES20.glBindFramebuffer( GLES20.GL_FRAMEBUFFER, mFboIds[0] );
		advect(U_TEX, U0_TEX);
		GLES20.glBindFramebuffer( GLES20.GL_FRAMEBUFFER, 0 );
		//swapTexIds( U_TEX, U0_TEX );


		/*
		setBoundary( U_TEX, U0_TEX );

		alpha = (mDx*mDx)/( mVisc * mDt );
		rBeta = 1 / ( 4 + alpha );
		diffuse(U_TEX, U_TEX, U0_TEX, alpha, rBeta);
		setBoundary( U_TEX, U0_TEX );

		project();
		setBoundary( U_TEX, U0_TEX );*/
		/*
		advect( C_TEX, C0_TEX );
		setBoundary( C_TEX, C0_TEX );

		alpha = (mDx*mDx)/( mDiff * mDt );
		rBeta = 1 / ( 4 + alpha );
		diffuse( C_TEX, C_TEX, C0_TEX, alpha, rBeta );
		setBoundary( C_TEX, C0_TEX );*/

		//addUserInteraction();
		
		
	}

	private void advect( int texId, int tempStorageTexId ){
				//advect u
		GLES20.glUseProgram( mShaderIds[ADVECT] );
		//render into temporary storage
		GLES20.glFramebufferTexture2D( GLES20.GL_FRAMEBUFFER,
																	 GLES20.GL_COLOR_ATTACHMENT0,
																	 GLES20.GL_TEXTURE_2D,
																	 mTexIds[U0_TEX], 0 );
		GLES20.glFramebufferRenderbuffer( GLES20.GL_FRAMEBUFFER,
																			GLES20.GL_DEPTH_ATTACHMENT,
																			GLES20.GL_RENDERBUFFER,
																			mRenderbuffer[0] );

		if( GLES20.glGetError() != GLES20.GL_NO_ERROR ){
			throw new RuntimeException("Error attaching texture to framebuffer!");
		}
		checkFBOStatus();
		GLES20.glClearColor(1.0f, 0.0f, 0.0f, 1.0f );
		GLES20.glClear( GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT );

		GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
		GLES20.glBindTexture( GLES20.GL_TEXTURE_2D, mTexIds[U_TEX ] );

		int uSampleLoc = 0;
		uSampleLoc = GLES20.glGetUniformLocation( mShaderIds[ADVECT], "u_sample" );
		//checkGlError("glGetUniformLocation");
		int xSampleLoc = GLES20.glGetUniformLocation( mShaderIds[ADVECT], "x_sample" );
		int dtLoc = GLES20.glGetUniformLocation( mShaderIds[ADVECT], "dt");
		int rdxLoc = GLES20.glGetUniformLocation( mShaderIds[ADVECT], "rdx" );
		int gridSizeLoc = GLES20.glGetUniformLocation( mShaderIds[ADVECT], "gridSize");
		//set dt and rdx
		GLES20.glUniform1f( dtLoc, mDt );
		GLES20.glUniform1f( rdxLoc, 1.0f );
		//pull from u to advect something by
		GLES20.glUniform1i( uSampleLoc, 0 );
		//the quantity we are advecting (i.e. U, Color, etc )
		GLES20.glUniform1i( xSampleLoc, texId );
		GLES20.glUniform1i( gridSizeLoc, mGridSize );
		drawQuad(mShaderIds[ADVECT]);
		//swap textures, so now U is updated and U0 is previous state
		swapTexIds( U_TEX, U0_TEX );


		if(DEBUG){
			mDebugAdvectCount++;
			Log.d(TAG, "Advect count: "+mDebugAdvectCount );
		}

	}

	private void diffuse( int xTexId, int bTexId,
												int tempStorageTexId, float alpha, float rBeta )
	{
		GLES20.glUseProgram( mShaderIds[JACOBI] );
		//write to U0 for temporary storage
		GLES20.glFramebufferTexture2D( GLES20.GL_FRAMEBUFFER,
																	 GLES20.GL_COLOR_ATTACHMENT0,
																	 GLES20.GL_TEXTURE_2D,
																	 mTexIds[ tempStorageTexId ], 0 );
		for( int i = 0; i < NUM_JACOBI_ITERS; i++ ){
			int xSampleLoc = GLES20.glGetUniformLocation( mShaderIds[JACOBI], "x_sample" );
			int bSampleLoc = GLES20.glGetUniformLocation( mShaderIds[JACOBI], "b_sample");
			int alphaLoc = GLES20.glGetUniformLocation( mShaderIds[JACOBI], "alpha" );
			int rBetaLoc = GLES20.glGetUniformLocation( mShaderIds[JACOBI], "rBeta" );
			int gridSizeLoc = GLES20.glGetUniformLocation( mShaderIds[JACOBI], "gridSize");
			//pull from U for both x and b
			GLES20.glUniform1i( xSampleLoc, xTexId );
			GLES20.glUniform1i( bSampleLoc, bTexId );
			GLES20.glUniform1f( alphaLoc, alpha );
			GLES20.glUniform1f( rBetaLoc, rBeta );
			GLES20.glUniform1i( gridSizeLoc, mGridSize );
			//I have to determine how the linear solver works, until I do
			//I'm not sure what the parameters alpha and rBeta should be
			checkFBOStatus();
			drawQuad(mShaderIds[JACOBI]);
			//set to be the updated texture
			swapTexIds( xTexId, tempStorageTexId );
		}
	}

	private void project(){
		calculateDivergence();
		setBoundary( DIV_TEX, P_TEMP_TEX );
		clearTexture( mTexIds[ P_TEX], 0.5f, 0.5f, 0.5f, 1.0f );
		//calculate pressure field
		diffuse( P_TEX, DIV_TEX, P_TEMP_TEX, -(mDx*mDx), 1.0f/4.0f );
		calculateDivergenceFreeVelocityField();
	}
	
	private void setBoundary( int texId, int tempStorageTexId ){
		int scale;
		if( texId == U_TEX) scale = -1;
		else scale = 1;
		GLES20.glUseProgram( mShaderIds[BND] );
				
		//render into temporary storage
		GLES20.glFramebufferTexture2D( GLES20.GL_FRAMEBUFFER,
																	 GLES20.GL_COLOR_ATTACHMENT0,
																	 GLES20.GL_TEXTURE_2D,
																	 mTexIds[tempStorageTexId], 0 );
		int offsetLoc = GLES20.glGetUniformLocation( mShaderIds[BND], "offset" );
		int bndSampleLoc = GLES20.glGetUniformLocation( mShaderIds[BND], "bnd_sample" );
		int scaleLoc = GLES20.glGetUniformLocation( mShaderIds[BND], "scale" );
		int gridSizeLoc = GLES20.glGetUniformLocation(mShaderIds[BND], "gridSize");
		checkFBOStatus();
		
		//pull from the velocity or presure field
		GLES20.glUniform1i( bndSampleLoc, texId );
		GLES20.glUniform1i( scaleLoc, scale );
		GLES20.glUniform1i( gridSizeLoc, mGridSize );

		GLES20.glUniform2i( offsetLoc, 0, 1 );
		drawLine( LINE1_VERT, LINE1_IND );

		GLES20.glUniform2i( offsetLoc, -1, 0 );
		drawLine( LINE2_VERT, LINE2_IND );

		GLES20.glUniform2i( offsetLoc, 0, -1 );
		drawLine( LINE3_VERT, LINE3_IND );

		GLES20.glUniform2i( offsetLoc, 1, 0 );
		drawLine( LINE4_VERT, LINE4_IND );

		swapTexIds( texId, tempStorageTexId );
	}

	private void calculateDivergence(){
		//compute pressure ( projection - divergence )
		GLES20.glUseProgram( mShaderIds[DIV] );
		//render into P0 for temporary storage
		GLES20.glFramebufferTexture2D( GLES20.GL_FRAMEBUFFER,
																	 GLES20.GL_COLOR_ATTACHMENT0,
																	 GLES20.GL_TEXTURE_2D,
																	 mTexIds[ DIV_TEX ], 0 );
		int texSampleLoc = GLES20.glGetUniformLocation( mShaderIds[DIV], "tex_sample" );
		int halfrdxLoc = GLES20.glGetUniformLocation( mShaderIds[DIV], "halfrdx" );
		int gridSizeLoc = GLES20.glGetUniformLocation( mShaderIds[DIV], "gridSize");
		//pull from the velocity texture
		GLES20.glUniform1i( texSampleLoc, U_TEX );
		GLES20.glUniform1f( halfrdxLoc, .5f/mDx );
		GLES20.glUniform1i( gridSizeLoc, mGridSize );
		checkFBOStatus();
		drawQuad();

	}

	private void calculateDivergenceFreeVelocityField(){
		//subtract to get gradient ( projection - gradient )
		GLES20.glUseProgram( mShaderIds[GRAD] );
		//render into velocity temporary storage
		GLES20.glFramebufferTexture2D( GLES20.GL_FRAMEBUFFER,
																	 GLES20.GL_COLOR_ATTACHMENT0,
																	 GLES20.GL_TEXTURE_2D, mTexIds[U0_TEX], 0 );
		int pSampleLoc = GLES20.glGetUniformLocation( mShaderIds[GRAD], "p_sample" );
		int uSampleLoc = GLES20.glGetUniformLocation( mShaderIds[GRAD], "u_sample" );
		int halfrdxLoc = GLES20.glGetUniformLocation( mShaderIds[GRAD], "halfrdx" );
		int gridSizeLoc = GLES20.glGetUniformLocation( mShaderIds[GRAD], "gridSize");
		//pull from velocity for the u field
		GLES20.glUniform1i( uSampleLoc, U_TEX );
		//pull from pressure for the p field
		GLES20.glUniform1i( pSampleLoc, P_TEX );
		GLES20.glUniform1f( halfrdxLoc, .5f/mDx );
		GLES20.glUniform1i( gridSizeLoc, mGridSize );
		checkFBOStatus();
		drawQuad();
		//set U to the updated velocity
		swapTexIds( U_TEX, U0_TEX );
	}

	void addUserInteraction(){
		/*		GLES20.glUseProgram( mShaderIds[FORCE] );
		//render into the temporary velocity texture
		GLES20.glFramebufferTexture2D( GLES20.GL_FRAMEBUFFER,
																	 GLES20.GL_COLOR_ATTACHMENT0,
																	 GLES20.GL_TEXTURE_2D,
																	 mTexIds[U0_TEX], 0 );
		int forceSampleLoc = GLES20.glGetUniformLocation( mShaderIds[FORCE],
																										"forceSample");
		int uSampleLoc = GLES20.glGetUniformLocation( mShaderIds[FORCE],
																									"uSample" );
		int forceScaleLoc = GLES20.glGetUniformLocation( mShaderIds[FORCE],
																										 "forceScale" );
		//pull from the force texture
		GLES20.glUniform1i( forceSampleLoc, FORCE_TEX );
		//pull from U texture
		GLES20.glUniform1i( uSampleLoc, U_TEX );
		GLES20.glUniform1f( forceScaleLoc, 100.0f );
		checkFBOStatus();
		drawQuad();
		//update U texture
		swapTexIds(U_TEX, U0_TEX);*/
		

		ShortBuffer colorBuff = ShortBuffer.allocate( 2 * 2 * 4 );
		ShortBuffer forceBuff = ShortBuffer.allocate(4);
		while( colorBuff.position() < colorBuff.capacity() ){
			colorBuff.put((short)200);
			colorBuff.put((short)0);
			colorBuff.put((short)0);
			colorBuff.put((short)0);
		}
		colorBuff.flip();
		GLES20.glBindTexture( GLES20.GL_TEXTURE_2D, mTexIds[C_TEX] );

		while( !mForceList.isEmpty() ){
			Force f = mForceList.poll();
			GLES20.glTexSubImage2D( GLES20.GL_TEXTURE_2D, 0,
															f.xCell, f.yCell,
															2, 2, GLES20.GL_RGBA,
															GLES20.GL_UNSIGNED_SHORT_5_5_5_1,
															colorBuff);

			forceBuff.put(f.fx).put(f.fy).put((short) 0).put((short) 0);
			forceBuff.flip();
			GLES20.glBindTexture( GLES20.GL_TEXTURE_2D, mTexIds[C_TEX]);
			GLES20.glTexSubImage2D( GLES20.GL_TEXTURE_2D, 0,
															f.xCell, f.yCell,
															1, 1, GLES20.GL_RGBA,
															GLES20.GL_UNSIGNED_SHORT_5_5_5_1,
															forceBuff );

			GLES20.glBindTexture( GLES20.GL_TEXTURE_2D, 0 );

		}

		GLES20.glBindTexture( GLES20.GL_TEXTURE_2D, 0 );
		
		/*
		GLES20.glUseProgram( mShaderIds[COLOR] );
		//render into color temporary storage
		GLES20.glFrameBufferTexture2D( GLES20.GL_FRAMEBUFFER
																	 GLES20.GL_COLOR_ATTACHMENT0,
																	 GLES20.GL_TEXTURE_2D,
																	 mTexIds[C_TEX] );
		int dtLoc = GLES20.glGetUniformLocation( mShaderIds[COLOR],
																						 "dt" );
		int forceSampleLoc = GLES20.glGetUniformLocation( mShaderIds[COLOR],
																								"forceSample" );
		int sigmaLoc = GLES20.glGetUniformLocation( mShaderIds[COLOR],
																								"sigma" );
		
		GLES20.glUniform1i( forceSampleLoc, FORCE_TEX );
		GLES20.glUniform1f( dtLoc, mDt );
		GLES20.glUniform1f( sigmaLoc, mForceRadius );
		checkFBOStatus();
		drawQuad();

		GLES20.glBindFramebuffer( GLES20.GL_FRAMEBUFFER, 0 );
		*/
	}



	private void drawQuad(int programId){
		/**Could be a problem here not binding the position and 
			 texcoord location indicies to the vertex shader,
			 I'm not sure if I need the code or not, but a call
			 to glBindAttribLocation should resolve it
		*/
		int positionLoc = 0, texcoordLoc = 1;
		int coordsPerVertex = 3;
		int texcoordsPerVertex = 2;
		int verticesPerQuad = 4;
		int stride = (coordsPerVertex +
									texcoordsPerVertex) * SIZEOF_FLOAT;
		int positionOffset = 0;
		int texcoordOffset = coordsPerVertex * SIZEOF_FLOAT;


		GLES20.glBindBuffer( GLES20.GL_ARRAY_BUFFER, mVboIds[QUAD_VERT] );
		GLES20.glBindBuffer( GLES20.GL_ELEMENT_ARRAY_BUFFER, mVboIds[QUAD_IND] );

		GLES20.glEnableVertexAttribArray( positionLoc );
		GLES20.glEnableVertexAttribArray( texcoordLoc );

		GLES20.glVertexAttribPointer( positionLoc, coordsPerVertex,
																	GLES20.GL_FLOAT, false,
																	stride, positionOffset );
		GLES20.glVertexAttribPointer( texcoordLoc, texcoordsPerVertex,
																	GLES20.GL_FLOAT, false,
																	stride, texcoordOffset );

		GLES20.glBindAttribLocation( mP
		
		GLES20.glDrawElements( GLES20.GL_TRIANGLE_FAN, 
													 verticesPerQuad * 1,
													 GLES20.GL_UNSIGNED_INT, 
													 0 );

		GLES20.glDisableVertexAttribArray( positionLoc );
		GLES20.glDisableVertexAttribArray( texcoordLoc );

		GLES20.glBindBuffer( GLES20.GL_ELEMENT_ARRAY_BUFFER, 0 );
		GLES20.glBindBuffer( GLES20.GL_ARRAY_BUFFER, 0 );
	}

	private void drawLine(int LINE_VERTS, int LINE_INDICES){
		//line 1
		int positionLoc = 0, texcoordLoc = 0;
		int coordsPerVertex = 3;
		int texcoordsPerVertex = 2;
		int verticesPerLine = 2;
		int stride = (coordsPerVertex +
									texcoordsPerVertex ) * SIZEOF_FLOAT;
		int positionOffset = 0;
		int texcoordOffset = coordsPerVertex * SIZEOF_FLOAT;

		GLES20.glBindBuffer( GLES20.GL_ARRAY_BUFFER, mVboIds[LINE_VERTS] );
		GLES20.glBindBuffer( GLES20.GL_ELEMENT_ARRAY_BUFFER, mVboIds[LINE_INDICES] );

		GLES20.glEnableVertexAttribArray( positionLoc );
		GLES20.glEnableVertexAttribArray( texcoordLoc );

		
		GLES20.glVertexAttribPointer( positionLoc, coordsPerVertex,
																	GLES20.GL_FLOAT, false,
																	stride, positionOffset );
		GLES20.glVertexAttribPointer( texcoordLoc, texcoordsPerVertex,
																	GLES20.GL_FLOAT, false,
																	stride, texcoordOffset );
		
		GLES20.glDrawElements( GLES20.GL_TRIANGLE_FAN,
													 verticesPerLine * 1,
													 GLES20.GL_UNSIGNED_INT,
													 0 );

		GLES20.glDisableVertexAttribArray( positionLoc );
		GLES20.glDisableVertexAttribArray( texcoordLoc );
		
		GLES20.glBindBuffer( GLES20.GL_ELEMENT_ARRAY_BUFFER, 0 );
		GLES20.glBindBuffer( GLES20.GL_ARRAY_BUFFER, 0 );

	}

	private void swapTexIds(int a, int b){
		int c = mTexIds[a];
		mTexIds[a] = mTexIds[b];
		mTexIds[b] = c;
	}

	private void checkFBOStatus() throws RuntimeException{
		int FBOStatus;
		FBOStatus = GLES20.glCheckFramebufferStatus( GLES20.GL_FRAMEBUFFER );
		if( FBOStatus != GLES20.GL_FRAMEBUFFER_COMPLETE ){
			Log.e(TAG, "Framebuffer incomplete!" );
			throw new RuntimeException();
		}
	}

	public void onTouchScreen(float mx, float my, float fx, float fy){
		if(DEBUG){
			mDebugTouched = true;
			Log.d(TAG, "mDebugTouched = true");
		}
		//some weird indexing happening here
			int x_cell = (int)( (mx/(float)mScreenWidth) * (mGridSize+1) ) -1;
			int y_cell = (int)( ( (mScreenHeight - my)/
														(float)mScreenHeight ) * (mGridSize+1) ) -1;

			x_cell = clamp( x_cell, 1, mGridSize );
			y_cell = clamp( y_cell, 1, mGridSize );
			//Log.d(TAG, "cell: (" + x_cell + ", " + y_cell + ")");
			short sfx = biject(fx);
			short sfy = biject(fy);
			//Log.d(TAG, "Force: ("+ sfx + ", " + sfy + ")" );
			/*			ShortBuffer forceBuff = ShortBuffer.allocate(4);
			forceBuff.put(sfx).put(sfy);
			forceBuff.flip();
			GLES20.glBindTexture( GLES20.GL_TEXTURE_2D, mTexIds[FORCE_TEX]);
			GLES20.glTexSubImage2D( GLES20.GL_TEXTURE_2D, 0,
															x_cell, y_cell,
															1, 1, GLES20.GL_RGBA,
															GLES20.GL_UNSIGNED_SHORT_5_5_5_1,
															forceBuff );

															GLES20.glBindTexture( GLES20.GL_TEXTURE_2D, 0 );*/

			mForceList.add(new Force( x_cell, y_cell, sfx, sfy ));
	}

	public void set_mvp_matrix(){
		Matrix.setIdentityM(mModelview_matrix, 0);
		Matrix.setLookAtM(mModelview_matrix, 0,
											0, 0, 3,
											0, 0, 0,
											0, 1, 0);
		Matrix.multiplyMM(mMvp_matrix, 0, mProjection_matrix,
											0, mModelview_matrix, 0);
		
		int mvpMatLoc = GLES20.glGetUn