//package com.tstine.fluidapp;
import java.lang.Math;
import java.lang.Exception;
import java.lang.StackTraceElement;
import android.util.Log;

//import cern.colt.matrix.DoubleFactory2D;
//import cern.colt.matrix.DoubleMatrix2D;
//import cern.colt.matrix.DoubleFactory1D;
//import cern.colt.matrix.DoubleMatrix1D;


public class FluidSimulator{
	public static final String TAG = "FluidRenderer";

	public enum bnd_type{D_BND, U_BND, V_BND}
	public static boolean touched = false;
	public static final int N = 100;
	public static final int size = (N+2) * (N+2);
	public static float dt;
	public static float diffusivity;
	public static float viscosity;
	public static float force;
	public static float source_add;

	public static float death_rate;
 	
	public static float[] u;
	public static float[] v;
	public static float[] d;
	public static float[] u0;
	public static float[] v0;
	public static float[] d0;

	//public static DoubleMatrix2D A_diff;

	private static FluidSimulator simulator = new FluidSimulator();

	private FluidSimulator(){
		set_default_values();
		allocate_memory();
	}

	private static void allocate_memory(){
		try{
			u = new float[size];
			v = new float[size];
			d = new float[size];
			u0 = new float[size];
			v0 = new float[size];
			d0 = new float[size];
		}catch(Exception e){
			Log.e( TAG, (e.getStackTrace()).toString() );
		}catch( OutOfMemoryError e ){
			Log.e(TAG, "Out of memory");
		}
		/*
		DoubleFactory2D factory = DoubleFactory2D.sparse;
		A_diff = factory.make( size, size );

		double a = (double)dt * diffusivity;
		int row, col;
		for( row=0; row < size; row++){
			col = row;
			A_diff.set( row, col, 1+4*a );
			col = row + 1;
			if(col < size)
				A_diff.set( row, col, -1 );
			col = row - 1;
			if(col > -1 )
				A_diff.set( row, col, -1 );
			col = row + N+2;
			if( col < size )
				A_diff.set(row, col, -1 );
			col = row - (N+2);
			if( col > -1 )
				A_diff.set( row, col, -1 );
		}
		*/
		//Log.d( TAG, A_diff.toString( ) );
	}

	public static void set_default_values(){
		dt = .1f;
		diffusivity = .0000f;
		viscosity = 0.000f;
		force = 5.0f;
		source_add = 100.0f;
		death_rate = .01f;
	}
	public static int AT(int i, int j){return i+(N+2)*j;}

	private static void swap_d(){
		float[]temp = d;
		d = d0;
		d0 = temp;
	}
	private static void swap_u(){
		float[]temp = u;
		u = u0;
		u0 = temp;
	}
	
	private static void swap_v(){
		float[]temp = v;
		v = v0;
		v0 = temp;
	}

	public static void step(){
		step_density();
		step_velocity();
		kill_source();
	}

	private static void step_density(){
		add_source( d, d0 );

		swap_d( );
		diffuse( bnd_type.D_BND, d, d0, diffusivity );

		swap_d( );
		advect( bnd_type.D_BND, d, d0, u, v );
	}

	private static void step_velocity(){
		add_source( u,u0 );
		add_source( v,v0 );

		swap_u();
		diffuse( bnd_type.U_BND, u, u0, viscosity );

		swap_v( );
		diffuse( bnd_type.V_BND, v, v0, viscosity );

		project( );
		
		swap_u( );
		swap_v( );

		advect( bnd_type.U_BND, u, u0, u0, v0 );
		advect( bnd_type.V_BND, v, v0, u0, v0 );

		project( );
	}
	
	private static void add_source(float[] x, float[] src){
		int i = 0;
		for( i=0; i < size; i++ ){
			x[i] += dt * src[i];
		}
		
	}

	private static void kill_source(){
		int i = 0;
		for( i=0; i< size; i++ ){
			d[i] = Math.max( 0.0f, ( d[i] - dt*death_rate ) );
		}
	}
	
	private static void diffuse( bnd_type bnd,
															 float[]x, float[] x0, float diff ){
		float num = dt * diff * N * N;
		linear_solver(bnd, x, x0, num, 1+4*num);
		
	}

	private static void linear_solver(bnd_type bnd, float[]x, float[]x0,
																		float num, float denom){
		int i=0,j=0,k=0;
		float avg_chg = 0.0f, val = 0.0f, max_change = -1.0f, change = -1.0f;;

		for(k=0; k<4; k++){
			for(i=1; i<=N; i++){
				for(j=1; j<=N; j++){
					val = (x0[AT(i,j)] + num *(x[AT(i-1, j)] + x[AT(i+1,j)]+
																					x[AT(i, j-1)] + x[AT(i,j+1)] ))/denom;
					change = Math.abs( val - x[AT(i,j)] );
					if( change > max_change ){
						max_change = change;
					}
					x[AT(i, j)] = val;
				}
			}
			set_bnd(bnd, x);
			max_change = -1.0f;
		}


	}

	private static void advect(bnd_type bnd, 
														 float[] x, float[] x0,
														 float[] u, float[] v){
		int i, j, i1, j1, i0, j0;
		float dt0, x_pos, y_pos, s0, s1, t0, t1;
		dt0 = dt * N;
		for(i=1; i<=N; i++){
			for(j=1; j<=N; j++){

				x_pos = i - dt0 * u[AT(i,j)];
				y_pos = j - dt0 * v[AT(i,j)];
				
				if( x_pos < 0.5f )
					x_pos = 0.5f;
				else if( x_pos > N+0.5f )
					x_pos = N + 0.5f;
				i0=(int)x_pos;
				i1 = i0+1;
				
				if( y_pos < 0.5f )
					y_pos = 0.5f;
				if( y_pos > N+0.5f )
					y_pos = N + 0.5f;
				j0=(int)y_pos;
				j1 = j0+1;
				
				s1 = x_pos - i0;
				s0 = 1 - s1;
				t1 = y_pos - j0;
				t0 = 1 - t1;
				
				x[AT(i,j)] = s0*( t0 * x0[AT( i0,j0 )] + t1 * x0[AT( i0,j1 )] ) + 
					s1 * ( t0 * x0[AT( i1,j0 )] + t1 * x0[AT( i1,j1 )] );
			}
		}

		set_bnd( bnd,x );
	}

	private static void project(){
		int i=0, j=0;
		for(i=1; i<=N; i++){
			for(j=1; j<=N; j++){
				//p=u0, div=v0
				v0[ AT( i,j )] = -0.5f *( u[AT( i+1,j )] - u[AT( i-1,j )] +
																	v[AT( i,j+1 )] - v[AT( i,j-1 )] )/N;
				u0[AT( i,j )] = 0;
			}
		}
		set_bnd( bnd_type.D_BND, u0 );
		set_bnd( bnd_type.D_BND, v0 );
		linear_solver( bnd_type.D_BND, u0, v0, 1, 4 );
		
		for(i=1; i<=N; i++){
			for(j=1; j<=N; j++){
				u[AT( i,j )] -= 0.5f * N * ( u0[AT( i+1,j )]-u0[AT( i-1,j )] );
				v[AT( i,j )] -= 0.5f * N * ( u0[AT( i,j+1 )]-u0[AT( i,j-1 )] );
			}
		}
		set_bnd(bnd_type.U_BND, u);
		set_bnd(bnd_type.V_BND, v);
	}

	private static void set_bnd(bnd_type type, float[] x){
		int i=0;
		for(i=1; i<=N; i++){
			switch( type ){
			case U_BND:
				x[AT( 0,i )]   = -x[AT( 1,i )];
				x[AT( N+1,i )] = -x[AT( N,i )];
				x[AT( i,1 )]   = 0.0f;
				x[AT( i,N )]   = 0.0f;
				break;
			case V_BND:
				x[AT( 0,i )]   = 0.0f;
				x[AT( N+1,i )] = 0.0f;
				x[AT( i,0 )]   = -x[AT( i,1 )];
				x[AT( i,N+1 )] = -x[AT( i,N )];
				break;
			case D_BND:
				x[AT( 0,i )]   = x[AT( 1,i )];
				x[AT( N+1,i )] = x[AT( N,i )];
				x[AT( i,0 )]   = x[AT( i,1 )];
				x[AT( i,N+1 )] = x[AT( i,N )];
				break;
			}
		}
		x[AT( 0,0 )]     = 0.5f * ( x[AT( 1,0 )] + x[AT( 0,1 )] );
		x[AT( 0,N+1 )]   = 0.5f * ( x[AT( 1,N+1 )] + x[AT( 0,N )] );
		x[AT( N+1,0 )]   = 0.5f * ( x[AT( N,0 )] + x[AT( N+1,1 )] );
		x[AT( N+1,N+1 )] = 0.5f * ( x[AT( N,N+1 )] + x[AT( N+1,N )] );
	}
}
