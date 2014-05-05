package com.android.facelock;

import java.util.Random;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.widget.ImageView;

public class PicturePasswordView extends ImageView
{
	private int mSeed;
	
	private final boolean DEBUG = false;

	private static final int DEFAULT_GRID_SIZE = 10;
	private static final int FONT_SIZE = 20;
	
	private float mScrollX = 0;
	private float mScrollY = 0;
	
	private float mFingerX;
	private float mFingerY;
	
	private Rect mTextBounds;
	
	private Paint mPaint;
	
	private int mGridSize;
	
	private boolean mShowNumbers;
	
	private int getNumberForXY( int x, int y )
	{
		// TODO: still sucks

		return Math.abs( mSeed ^ ( x * 2138105 + 1 ) * ( y + 1 * 23490 ) ) % 10;
	}

	public PicturePasswordView( Context context, AttributeSet attrs )
	{
		super( context, attrs );
		
		Random rnd = new Random();
		mSeed = rnd.nextInt();
		
		mGridSize = DEFAULT_GRID_SIZE;
		
		final DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
		final float shadowOff = TypedValue.applyDimension( TypedValue.COMPLEX_UNIT_DIP, 2, displayMetrics );
		
		mPaint = new Paint( Paint.LINEAR_TEXT_FLAG );
		
		mPaint.setColor( Color.WHITE );
		
		mPaint.setShadowLayer( 10, shadowOff, shadowOff, Color.BLACK );
		mPaint.setTextSize( TypedValue.applyDimension( TypedValue.COMPLEX_UNIT_DIP, FONT_SIZE, displayMetrics ) );
		
		mPaint.setAntiAlias( true );
		
		mTextBounds = new Rect();
		mPaint.getTextBounds( "8", 0, 1, mTextBounds );
		
		TypedArray a = context.getTheme().obtainStyledAttributes( attrs, R.styleable.PicturePasswordView, 0, 0 );

		mShowNumbers = true;
		
		try
		{
			mShowNumbers = a.getBoolean( R.styleable.PicturePasswordView_showNumbers, true );
		}
		finally
		{
			a.recycle();
		}
	}
	
	public boolean isShowNumbers()
	{
		return mShowNumbers;
	}
	
	public void setShowNumbers( boolean show )
	{
		mShowNumbers = show;
		invalidate();
	}

	@Override
	protected void onDraw( Canvas canvas )
	{
		super.onDraw( canvas );
		
		if ( !mShowNumbers ) return;
		
		final float cellSize = canvas.getWidth() / mGridSize;
		
		float drawX = -cellSize / 1.5F;
		
		for ( int x = -1; x < mGridSize + 1; x++ )
		{
			float drawY = -mTextBounds.bottom + cellSize / 1.5F - cellSize;
			
			for ( int y = -1; y < mGridSize + 1; y++ )
			{
				if ( DEBUG )
				{
					if ( x == -1 || y == -1 || x == mGridSize || y == mGridSize )
					{
						mPaint.setColor( Color.RED );
					}
					else
					{
						mPaint.setColor( Color.WHITE );
					}
				}
				
				int cellX = ( int ) ( x - mScrollX / cellSize );
				int cellY = ( int ) ( y - mScrollY / cellSize );
				
				if ( mScrollX / cellSize <= 0 && cellX != 0 ) cellX--;
				if ( mScrollY / cellSize <= 0 && cellY != 0 ) cellY--;
			
				Integer number = getNumberForXY( cellX, cellY );
				
				canvas.drawText( number.toString(), drawX + mScrollX % cellSize, drawY + mScrollY % cellSize, mPaint );
				drawY += cellSize;
			}
			
			drawX += cellSize;
		}
		
		if ( DEBUG )
		{
			canvas.drawText( mScrollX / cellSize + "," + mScrollY / cellSize, 0, mTextBounds.bottom * 26.5f, mPaint );
		}
	}
	
	public void setGridSize( int size )
	{
		if ( size > 3 && size <= 8 )
		{
			mGridSize = size;
			invalidate();
		}
	}
	
	public int getGridSize()
	{
		return mGridSize;
	}
	
	@Override
	public boolean onTouchEvent( MotionEvent event )
	{
		float x = event.getX();
		float y = event.getY();
		
		
		switch( event.getAction() )
		{
			case MotionEvent.ACTION_DOWN:
				mFingerX = x;
				mFingerY = y;
				break;
				
			case MotionEvent.ACTION_MOVE:
				float diffx = x - mFingerX;
				float diffy = y - mFingerY;

				mScrollX += diffx;
				mScrollY += diffy;
				
				mFingerX = x;
				mFingerY = y;
				
				invalidate();
				break;
		}

		return true; // super.onTouchEvent( event );
	}
}
