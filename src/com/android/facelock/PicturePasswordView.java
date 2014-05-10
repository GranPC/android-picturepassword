package com.android.facelock;

import java.util.Random;

import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;

public class PicturePasswordView extends ImageView
{
	public interface OnFingerUpListener
	{
		void onFingerUp( PicturePasswordView picturePassword, boolean shouldUnlock );
	}

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
	private Paint mCirclePaint;
	
	private int mGridSize;
	
	private boolean mShowNumbers;
	
	private float mScale;
	private ObjectAnimator mAnimator;
	
	private Random mRandom;
	
	private boolean mHighlight = false;
	private int mHighlightX;
	private int mHighlightY;
	
	private float mHighlightImageX;
	private float mHighlightImageY;
	
	private OnFingerUpListener mListener;

	private int mUnlockNumber = -1;
	private float mUnlockNumberX = -1;
	private float mUnlockNumberY = -1;
	
	private boolean mShouldUnlock = false;
	
	private boolean mHighlightUnlockNumber = false;
	
	private int getNumberForXY( int x, int y )
	{
		// TODO: still sucks

		return Math.abs( mSeed ^ ( x * 2138105 + 1 ) * ( y + 1 * 23490 ) ) % 10;
	}

	public PicturePasswordView( Context context, AttributeSet attrs )
	{
		super( context, attrs );
		
		setScaleType( ScaleType.CENTER_CROP );
		
		mRandom = new Random();
		mSeed = mRandom.nextInt();
		
		mGridSize = DEFAULT_GRID_SIZE;
		
		final DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
		final float shadowOff = TypedValue.applyDimension( TypedValue.COMPLEX_UNIT_DIP, 2, displayMetrics );
		
		mPaint = new Paint( Paint.LINEAR_TEXT_FLAG );
		
		mPaint.setColor( Color.WHITE );
		
		mPaint.setShadowLayer( 10, shadowOff, shadowOff, Color.BLACK );
		mPaint.setTextSize( TypedValue.applyDimension( TypedValue.COMPLEX_UNIT_DIP, FONT_SIZE, displayMetrics ) );
		
		mPaint.setAntiAlias( true );


		mCirclePaint = new Paint( Paint.ANTI_ALIAS_FLAG );

		mCirclePaint.setColor( Color.argb( 255, 0x33, 0xb5, 0xe5 ) );
		
		mCirclePaint.setStyle( Paint.Style.STROKE );
		mCirclePaint.setStrokeWidth( 5 );
		
		/* mCirclePaint.setShadowLayer( 5, 0, 0, Color.WHITE );
		
		setLayerType( LAYER_TYPE_SOFTWARE, mCirclePaint ); */ // this feels too laggy

		
		mTextBounds = new Rect();
		mPaint.getTextBounds( "8", 0, 1, mTextBounds );

		
		TypedArray a = context.getTheme().obtainStyledAttributes( attrs, R.styleable.PicturePasswordView, 0, 0 );

		mShowNumbers = true;
		mScale = 1.0f;
		
		mAnimator = new ObjectAnimator();
		mAnimator.setTarget( this );
		mAnimator.setFloatValues( 0, 1 );
		mAnimator.setPropertyName( "scale" );
		mAnimator.setDuration( 200 );
		
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
	
	public void reset()
	{
		mSeed = mRandom.nextInt();
		mScrollX = 0;
		mScrollY = 0;
		
		invalidate();
	}
	
	public void setShowNumbers( boolean show )
	{
		setShowNumbers( show, false );
		
		invalidate();
	}
	
	public void setScale( float scale )
	{
		if ( mScale != scale )
		{
			mScale = scale;
			invalidate();
		}
	}
	
	public float getScale()
	{
		return mScale;
	}
	
	public void setShowNumbers( boolean show, boolean animate )
	{
		mShowNumbers = show;
		
		if ( animate )
		{
			mAnimator.start();
		}
		else
		{
			mScale = show ? 1.0f : 0.0f;			
		}
		
		invalidate();
	}
	
	public void setFocusNumber( int number )
	{
		if ( number >= 0 && number <= 9 )
		{
			mHighlight = true;
			
			boolean found = false;
			
			mScrollX = mScrollY = 0;
			
			while ( !found )
			{
				for ( int x = 0; x < mGridSize; x++ )
				{
					for ( int y = 0; y < mGridSize; y++ )
					{
						if ( getNumberForXY( x, y ) == number )
						{
							mHighlightX = x;
							mHighlightY = y;
							found = true;
							break;
						}
					}
				}
				
				if ( !found )
				{
					mSeed = mRandom.nextInt();
				}
			}
		}
		else
		{
			mHighlight = false;
		}
	}
	
	public void setUnlockNumber( int number, float x, float y )
	{
		mUnlockNumber = number;
		mUnlockNumberX = x;
		mUnlockNumberY = y;
	}
	
	public void setOnFingerUpListener( OnFingerUpListener l )
	{
		mListener = l;
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
	
	public PointF getHighlightPosition()
	{
		if ( mHighlight == false ) return null;
		
		return new PointF( mHighlightImageX, mHighlightImageY );
	}
	
	public void setHighlightUnlockNumber( boolean highlight )
	{
		mHighlightUnlockNumber = highlight;
	}
	
	@Override
	protected void onDraw( Canvas canvas )
	{
		super.onDraw( canvas );
		
		if ( !mShowNumbers ) return;
	
		mPaint.setAlpha( ( int ) ( mScale * ( float ) ( ( mHighlight || mHighlightUnlockNumber ) ? 64 : 255 ) ) );

		final float cellSize = ( canvas.getWidth() / ( float ) mGridSize ) * ( mScale * 0.4f + 0.6f );
		
		final float xOffset = ( 1.0f - ( mScale * 0.4f + 0.6f ) ) * canvas.getWidth() / 2;
		final float yOffset = ( 1.0f - ( mScale * 0.4f + 0.6f ) ) * canvas.getWidth() / 2;
		
		float drawX = -cellSize / 1.5F + xOffset;
		
		mShouldUnlock = false;
		
		for ( int x = -1; x < mGridSize + 1; x++ )
		{
			float drawY = -mTextBounds.bottom + cellSize / 1.5F - cellSize + yOffset;
			
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
				
				int cellX = ( int ) ( x - Math.floor( mScrollX / cellSize ) );
				int cellY = ( int ) ( y - Math.floor( mScrollY / cellSize ) );
				
				if ( mScrollX / cellSize <= 0 && cellX != 0 && mScrollX != 0 ) cellX--;
				if ( mScrollY / cellSize <= 0 && cellY != 0 && mScrollY != 0 ) cellY--;
				
				float numX = drawX + mScrollX % cellSize;
				float numY = drawY + mScrollY % cellSize;
				
				Integer number = getNumberForXY( cellX, cellY );
				boolean shouldHighlight = false;
				
				if ( number == mUnlockNumber )
				{
					float unlockX = mUnlockNumberX * getWidth();
					float unlockY = mUnlockNumberY * getWidth();
					
					float dist = PointF.length( unlockX - numX, unlockY - numY );
					
					if ( dist < mTextBounds.right * 2.0f )
					{
						mShouldUnlock = true;
						
						if ( mHighlightUnlockNumber )
							shouldHighlight = true;
					}
				}
				
				if ( ( mHighlight && mHighlightX == cellX && mHighlightY == cellY ) || shouldHighlight )
				{
					mPaint.setAlpha( ( int ) ( mScale * 255 ) );
					canvas.drawCircle( numX + ( mTextBounds.right - mTextBounds.left ) / 2,
							numY + mTextBounds.top / 2,
							mPaint.getTextSize() / 1.5f, mCirclePaint );
				}
				
				canvas.drawText( number.toString(), numX, numY, mPaint );
				
				if ( ( mHighlight && mHighlightX == cellX && mHighlightY == cellY ) || shouldHighlight )
				{
					mHighlightImageX = numX / getWidth();
					mHighlightImageY = numY / getHeight();
					
					mPaint.setAlpha( ( int ) ( mScale * 64 ) );
				}

				drawY += cellSize;
			}
			
			drawX += cellSize;
		}
		
		if ( DEBUG )
		{
			canvas.drawText( mScrollX / cellSize + "," + mScrollY / cellSize, 0, mTextBounds.bottom * 26.5f, mPaint );
		}
	}
	
	@Override
	public boolean onTouchEvent( MotionEvent event )
	{
		if ( !isEnabled() ) return true;
		
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
				
			case MotionEvent.ACTION_UP:
				if ( mListener != null )
				{
					mListener.onFingerUp( this, mShouldUnlock );
				}
		}

		return true; // super.onTouchEvent( event );
	}
}
