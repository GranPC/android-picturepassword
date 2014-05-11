package com.android.facelock;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.PointF;

public class PicturePasswordUtils
{
	public static class Preferences
	{
		Bitmap image;
		int unlockNumber;
		float unlockNumberX;
		float unlockNumberY;
		boolean randomize;
		
		int gridSize;
	}
	
	public static boolean saveUnlockData( Context context, Bitmap bitmap, int gridSize, boolean randomize, int chosenNumber, PointF unlockPosition )
	{
		File bitmapFile = new File( context.getFilesDir(), "image" );
		FileOutputStream bitmapStream;
		
		try
		{
			bitmapStream = new FileOutputStream( bitmapFile );
		}
		catch ( FileNotFoundException e )
		{
			e.printStackTrace();
			return false;
		}
		
		bitmap.compress( Bitmap.CompressFormat.PNG, 100, bitmapStream );
		
		try
		{
			bitmapStream.flush();
			bitmapStream.close();
		}
		catch ( IOException e )
		{
			e.printStackTrace();
			return false;
		}
		
		SharedPreferences.Editor prefs = context.getSharedPreferences( "com.peniscorp.picturepassword_prefs", 0 ).edit();
		
		prefs.putInt( "number", chosenNumber );
		prefs.putFloat( "numberx", unlockPosition.x );
		prefs.putFloat( "numbery", unlockPosition.y );
		
		prefs.putInt( "gridsize", gridSize );
		prefs.putBoolean( "randomize", randomize );
		
		prefs.commit();
		
		return true;
	}
	
	public static boolean getUnlockData( Preferences prefs, Context context )
	{
		File bitmapFile = new File( context.getFilesDir(), "image" );
		FileInputStream bitmapStream;

		try
		{
			bitmapStream = new FileInputStream( bitmapFile );
		}
		catch ( FileNotFoundException e )
		{
			e.printStackTrace();
			return false;
		}
		
		prefs.image = BitmapFactory.decodeStream( bitmapStream );
		
		SharedPreferences sharedPrefs = context.getSharedPreferences( "com.peniscorp.picturepassword_prefs", 0 );
		prefs.unlockNumber = sharedPrefs.getInt( "number", 0 );
		prefs.unlockNumberX = sharedPrefs.getFloat( "numberx", 0 );
		prefs.unlockNumberY = sharedPrefs.getFloat( "numbery", 0 );
		prefs.randomize = sharedPrefs.getBoolean( "randomize", false );
		
		prefs.gridSize = sharedPrefs.getInt( "gridsize", 0 );
		
		return true;
	}
	
	public static long getWaitTime( Context context )
	{
		SharedPreferences sharedPrefs = context.getSharedPreferences( "com.peniscorp.picturepassword_prefs", 0 );
		
		long time = sharedPrefs.getLong( "wait_until", 0 );
		
		if ( time > System.currentTimeMillis() / 1000 + 30 )
		{
			time = System.currentTimeMillis() / 1000 + 30;
		}
		
		return time;
	}
	
	public static void setWaitTime( Context context, long seconds )
	{
		SharedPreferences.Editor prefs = context.getSharedPreferences( "com.peniscorp.picturepassword_prefs", 0 ).edit();
		
		prefs.putLong( "wait_until", System.currentTimeMillis() / 1000 + seconds );
		
		prefs.commit();
	}
	
	public static int getFailureCounter( Context context )
	{
		SharedPreferences sharedPrefs = context.getSharedPreferences( "com.peniscorp.picturepassword_prefs", 0 );
		
		return sharedPrefs.getInt( "failure_counter", 0 );
	}
	
	public static void setFailureCounter( Context context, int count )
	{
		SharedPreferences.Editor prefs = context.getSharedPreferences( "com.peniscorp.picturepassword_prefs", 0 ).edit();
		
		prefs.putInt( "failure_counter", count );
		
		prefs.commit();
	}
	
	public static boolean getLockedOut( Context context )
	{
		SharedPreferences sharedPrefs = context.getSharedPreferences( "com.peniscorp.picturepassword_prefs", 0 );
		
		return sharedPrefs.getBoolean( "locked_out", false );
	}
	
	public static void setLockedOut( Context context, boolean lockedOut )
	{
		SharedPreferences.Editor prefs = context.getSharedPreferences( "com.peniscorp.picturepassword_prefs", 0 ).edit();
		
		prefs.putBoolean( "locked_out", lockedOut );
		
		prefs.commit();
	}
}
