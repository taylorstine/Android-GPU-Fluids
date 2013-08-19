package com.tstine.fluidapp;

import android.app.Activity;
import android.os.Bundle;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.content.Context;

import android.view.MotionEvent;

import java.lang.Float;
import java.lang.Double;

import android.util.Log;


public class MainActivity extends Activity
{
	private GLSurfaceView gl_view;
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		gl_view = new FluidSurface(this);
		setContentView(gl_view);
	}
}

class FluidSurface extends GLSurfaceView{
	float mOmx, mOmy, mMx, mMy;
	FluidRenderer renderer;
	private final boolean DEBUG = true;

	public FluidSurface(Context context){
		super(context);
		renderer = new FluidRenderer(context);
		setEGLContextClientVersion(2);
		setRenderer( renderer );
		//setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
	}

	@Override
	public boolean onTouchEvent(MotionEvent e){
		float x = e.getX();
		float y = e.getY();
		float fx= 0.0f, fy= 0.0f;
		int input = e.getAction();
		switch( input ){
		case(MotionEvent.ACTION_DOWN):
			mMx = x;
			mMy = y;
			if(DEBUG){
				renderer.onTouchScreen( 0, 0, 0, 0 );
			}
			break;
		case(MotionEvent.ACTION_MOVE):
			mMx = x;
			mMy = y;
			fx = mMx - mOmx;
			fy = -(mMy - mOmy);
			renderer.onTouchScreen( mOmx, mOmy, fx, fy );
			mOmx = mMx;
			mOmy = mMy;
			break;
		}
		/*		switch(e.getAction()){
		case MotionEvent.ACTION_MOVE:
			//Log.d("FluidRenderer", "size of float: " + Float.SIZE);
			//Log.d("FluidRenderer", "size of double: " + Double.SIZE);
			//Log.d( "FluidRenderer", "Moving! ( " + x + ", " + y + " )" );
			break;
		case MotionEvent.ACTION_DOWN:
			Log.d( "FluidRenderer", "Down! ( " + x + ", " + y + " )" );
			break;
		case MotionEvent.ACTION_UP:

			Log.d( "FluidRenderer", "Up! ( " + x + ", " + y + " )" );
			break;
			}*/


		return true;
	}
}

