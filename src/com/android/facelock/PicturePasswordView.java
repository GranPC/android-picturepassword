package com.android.facelock;

import java.util.Random;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.widget.ImageView;

public class PicturePasswordView extends ImageView
{
	private int mSeed;
	
	private int mScrollX = 0;
	private int mScrollY = 0;
	
	private static final int GRID_SIZE = 10;
	private static final int FONT_SIZE = 20;
	
	private Paint mPaint;
	
	private int getNumberForXY( int x, int y )
	{
		// TODO: bad
		
		return ( ( mSeed ^ ( x + 32 ) ) * ( y + 124 ) ) % 10;
	}

	public PicturePasswordView( Context context, AttributeSet attrs )
	{
		super( context, attrs );
		
		Random rnd = new Random();
		mSeed = rnd.nextInt();
		
		final DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
		final float shadowOff = TypedValue.applyDimension( TypedValue.COMPLEX_UNIT_DIP, 2, displayMetrics );
		
		mPaint = new Paint( Paint.LINEAR_TEXT_FLAG );
		
		mPaint.setColor( Color.WHITE );
		
		mPaint.setShadowLayer( 10, shadowOff, shadowOff, Color.BLACK );
		mPaint.setTextSize( TypedValue.applyDimension( TypedValue.COMPLEX_UNIT_DIP, FONT_SIZE, displayMetrics ) );
		
		mPaint.setAntiAlias( true );
	}

	@Override
	protected void onDraw( Canvas canvas )
	{
		super.onDraw( canvas );
		
		float drawX = canvas.getWidth() / GRID_SIZE / 3.0f;
		
		for ( int x = 0; x < GRID_SIZE; x++ )
		{
			float drawY = canvas.getHeight() / GRID_SIZE / 1.5f;
			for ( int y = 0; y < GRID_SIZE; y++ )
			{
				canvas.drawText( "8", drawX, drawY, mPaint );
				drawY += canvas.getHeight() / GRID_SIZE;
			}
			
			drawX += canvas.getWidth() / GRID_SIZE;
		}
	}
}