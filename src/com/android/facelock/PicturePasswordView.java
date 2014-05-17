package com.android.facelock;

import java.util.Random;

import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.widget.ImageView;

public class PicturePasswordView extends ImageView
{
	public interface OnFingerUpListener
	{
		void onFingerUp( PicturePasswordView picturePassword, boolean shouldUnlock );
	}

	// The seed for the random number generator
	private int mSeed;
	
	private final boolean DEBUG = false;

	private static final int DEFAULT_GRID_SIZE = 10;
	private static final int FONT_SIZE = 20;
	
	private static final int COLOR_UNLOCK_CIRCLE_OFF = Color.rgb( 150, 150, 150 );
	private static final int COLOR_UNLOCK_CIRCLE_ON  = Color.rgb( 132, 212,  39 );
	
	// How far we have scrolled from (0, 0)
	private float mScrollX = 0;
	private float mScrollY = 0;
	
	// The current position of the finger, used for scrolling calculations
	private float mFingerX;
	private float mFingerY;
	
	// Size of the number 8.
	private Rect mTextBounds;
	
	// Paint objects
	private Paint mPaint;        // Paint for text
	private Paint mCirclePaint;  // Paint for circles
	private Paint mUnlockPaint;  // Paint for unlock circles
	
	// Grid size and grid size without randomization 
	private int mGridSize;
	private int mActualSize;
	
	// Whether we should show numbers or not
	private boolean mShowNumbers;
	
	// Scalar for fade animations
	private float mScale;
	private ObjectAnimator mAnimator;
	
	// PRNG object
	private Random mRandom;
	
	// Whether we should highlight any number, and if so, which number
	private boolean mHighlight = false;
	private int mHighlightX;
	private int mHighlightY;
	
	// The position in the image of the highlighted number (0..1)
	private float mHighlightImageX;
	private float mHighlightImageY;
	
	// Listener we should notify when the user lifts their finger
	private OnFingerUpListener mListener;

	// Number + position combo required to unlock device
	private int mUnlockNumber = -1;
	private float mUnlockNumberX = -1;
	private float mUnlockNumberY = -1;
	
	// Whether we should unlock next time the user lifts their finger
	private boolean mShouldUnlock = false;
	
	// Whether we should highlight our unlock number
	private boolean mHighlightUnlockNumber = false;
	
	// Whether we're resetting. Used in setScale to reset parameters when scale is 0
	private boolean mResetting = false;
	
	// Whether the user has enabled grid size randomization
	private boolean mRandomGridSize = false;
	
	// Number of unlock circles to show at the bottom
	private int mUnlockCircles = 0;
	
	// Number of filled unlock circles
	// The decimal part is used to fade in/out the rightmost circle
	private float mUnlockProgress = 0;
	
	// Size/spacing/padding of unlock circles
	private int mCircleSize;
	private int mCircleSpacing;
	private int mCirclePadding;
	
	// Animator for circle progress
	private ObjectAnimator mCircleAnimator;
	
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
		
		///////////////////////
		// Initialize Paints //
		///////////////////////
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
		
		mUnlockPaint = new Paint( Paint.ANTI_ALIAS_FLAG );
		
		mTextBounds = new Rect();
		mPaint.getTextBounds( "8", 0, 1, mTextBounds );
		
		///////////////////////////
		// Initialize animations //
		///////////////////////////

		mScale = 1.0f;
		
		mAnimator = new ObjectAnimator();
		mAnimator.setTarget( this );
		mAnimator.setFloatValues( 0, 1 );
		mAnimator.setPropertyName( "scale" );
		mAnimator.setDuration( 200 );
		
		mCircleAnimator = new ObjectAnimator();
		mCircleAnimator.setTarget( this );
		mCircleAnimator.setPropertyName( "internalUnlockProgress" ); // ugh!
		mCircleAnimator.setDuration( 300 );
		
		///////////////////////
		// Hide/show numbers //
		///////////////////////
		
		mShowNumbers = true;
		
		TypedArray a = context.getTheme().obtainStyledAttributes( attrs, R.styleable.PicturePasswordView, 0, 0 );
		
		try
		{
			mShowNumbers = a.getBoolean( R.styleable.PicturePasswordView_showNumbers, true );
		}
		finally
		{
			a.recycle();
		}
		
		//////////////////////
		// Initialize sizes //
		//////////////////////
		mCircleSize = ( int ) TypedValue.applyDimension( TypedValue.COMPLEX_UNIT_DIP, 6, displayMetrics );
		mCircleSpacing = ( int ) TypedValue.applyDimension( TypedValue.COMPLEX_UNIT_DIP, 5, displayMetrics );
		mCirclePadding = ( int ) TypedValue.applyDimension( TypedValue.COMPLEX_UNIT_DIP, 10, displayMetrics );
	}
	
	public boolean isShowNumbers()
	{
		return mShowNumbers;
	}
	
	public void reset()
	{
		mResetting = true;
		
		mAnimator.setDuration( 400 );
		
		// Repeat 0 to ensure setScale( 0 ) is called at least once
		mAnimator.setFloatValues( 1, 0, 0, 1 );

		mAnimator.start();
		
		setEnabled( false );
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
			
			if ( mResetting && ( scale == 0 || scale == 1 ) )
			{
				if ( scale == 0 )
				{
					mSeed = mRandom.nextInt();
					mScrollX = 0;
					mScrollY = 0;
					
					if ( mRandomGridSize )
					{
						setGridSize( mActualSize );
					}
				}
				else
				{
					mAnimator.setFloatValues( 0, 1 );
					mAnimator.setDuration( 200 );
					
					mResetting = false;
					setEnabled( true );
				}
			}
		}
	}
	
	public float getScale()
	{
		return mScale;
	}
	
	public void setShowNumbers( boolean show, boolean animate )
	{
		if ( animate )
		{
			mShowNumbers = true;

			if ( show )
				mAnimator.start();
			else
				mAnimator.reverse();
		}
		else
		{
			mShowNumbers = show;
			
			mScale = show ? 1.0f : 0.0f;			
		}
		
		invalidate();
	}
	
	public Point findNumberInGrid( int number )
	{
		if ( number < 0 || number > 9 ) return null;
		
		for ( int x = 0; x < mGridSize; x++ )
		{
			for ( int y = 0; y < mGridSize; y++ )
			{
				if ( getNumberForXY( x, y ) == number )
				{
					return new Point( x, y );
				}
			}
		}
		
		return null;
	}
	
	public void enforceNumber( int number )
	{
		if ( number < 0 || number > 9 ) return;
		
		while ( findNumberInGrid( number ) == null )
		{
			mSeed = mRandom.nextInt(); 
		}
	}
	
	public void setFocusNumber( int number )
	{
		if ( number >= 0 && number <= 9 )
		{
			mHighlight = true;
			
			mScrollX = mScrollY = 0;
			
			enforceNumber( number );
			
			Point position = findNumberInGrid( number );

			mHighlightX = position.x;
			mHighlightY = position.y;
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
			mGridSize = mActualSize = size;
			
			if ( mRandomGridSize )
			{
				mGridSize = mActualSize + mRandom.nextInt( 3 ) - 1;
			}
			
			invalidate();
		}
	}
	
	public void setRandomize( boolean randomize )
	{
		mRandomGridSize = randomize;
		setGridSize( mActualSize );
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
	
	public void setUnlockCircles( int circles )
	{
		mUnlockCircles = circles;
	}
	
	public void setInternalUnlockProgress( float progress )
	{
		mUnlockProgress = progress;
	}
	
	public float getInternalUnlockProgress()
	{
		return mUnlockProgress;
	}
	
	public void setUnlockProgress( int progress )
	{
		mCircleAnimator.setFloatValues( mUnlockProgress, progress );
		mCircleAnimator.start();
	}
	
	private static int lerp( int a, int b, float t )
	{
		return ( int ) ( t * ( b - a ) + a );
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
					
					if ( dist < mTextBounds.right * 1.3f )
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
		
		int circlesWidth = mCircleSize * mUnlockCircles + mCircleSpacing * ( mUnlockCircles - 1 );
		
		int x = canvas.getWidth() / 2 - circlesWidth / 2;
		int y = canvas.getHeight() - mCirclePadding - mCircleSize / 2;
		int fullCircles = ( int ) Math.floor( mUnlockProgress );
		float partCircles = mUnlockProgress - fullCircles;
		
		for ( int i = 1; i < mUnlockCircles + 1; i++ )
		{
			if ( i <= fullCircles )
			{
				mUnlockPaint.setColor( COLOR_UNLOCK_CIRCLE_ON );
			}
			else if ( i == fullCircles + 1 )
			{
				int r = lerp( Color.red( COLOR_UNLOCK_CIRCLE_OFF ), Color.red( COLOR_UNLOCK_CIRCLE_ON ), partCircles );
				int g = lerp( Color.green( COLOR_UNLOCK_CIRCLE_OFF ), Color.green( COLOR_UNLOCK_CIRCLE_ON ), partCircles );
				int b = lerp( Color.blue( COLOR_UNLOCK_CIRCLE_OFF ), Color.blue( COLOR_UNLOCK_CIRCLE_ON ), partCircles );
				
				mUnlockPaint.setColor( Color.rgb( r, g, b ) );
			}
			else
			{
				mUnlockPaint.setColor( COLOR_UNLOCK_CIRCLE_OFF );
			}
			
			mUnlockPaint.setAlpha( 150 );
			canvas.drawCircle( x + mCircleSize / 2, y, mCircleSize, mUnlockPaint );
			
			x += mCircleSize * 2 + mCircleSpacing;
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
