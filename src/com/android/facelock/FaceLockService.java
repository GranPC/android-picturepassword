package com.android.facelock;

import android.animation.Animator;
import android.animation.Animator.AnimatorListener;
import android.animation.ValueAnimator;
import android.animation.ValueAnimator.AnimatorUpdateListener;
import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.Handler.Callback;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.view.WindowManager.LayoutParams;

import com.android.facelock.PicturePasswordUtils.Preferences;
import com.android.facelock.PicturePasswordView.OnFingerUpListener;
import com.android.internal.policy.IFaceLockCallback;
import com.android.internal.policy.IFaceLockInterface.Stub;

public class FaceLockService extends Service implements Callback
{
	private final int MSG_SERVICE_CONNECTED = 0;
	private final int MSG_SERVICE_DISCONNECTED = 1;
	private final int MSG_UNLOCK = 2;
	private final int MSG_CANCEL = 3;
	private final int MSG_REPORT_FAILED_ATTEMPT = 4;
	private final int MSG_POKE_WAKELOCK = 5;
	
	private final int MSG_ENABLE = 47;
	
	private final Stub binder;
	protected LayoutParams mLayoutParams; 
	protected IFaceLockCallback mCallback;
	
	private View mView;
	private WindowManager mWindowManager;
	
	public FaceLockService()
	{
		binder = new Stub()
		{
			@Override
			public void startUi( IBinder windowToken, int x, int y, int w, int h, boolean useLiveliness ) throws RemoteException
			{
				DisplayMetrics metrics = new DisplayMetrics();
				mWindowManager.getDefaultDisplay().getRealMetrics( metrics );
				
				Log.d( "PicturePassword", "Appearing at " + ( y + h ) );
				if ( y + h > metrics.heightPixels ) return;

				LayoutParams p = new LayoutParams( LayoutParams.TYPE_APPLICATION_PANEL );
				p.flags = LayoutParams.FLAG_HARDWARE_ACCELERATED | LayoutParams.FLAG_NOT_FOCUSABLE;
				p.token = windowToken;
				p.x = x;
				p.y = y;
				p.width = w;
				p.height = h;
				p.gravity = 8388659; // TODO: decompose, i have no idea what this means
				
				FaceLockService.this.mLayoutParams = p;
				FaceLockService.this.mHandler.obtainMessage( MSG_SERVICE_CONNECTED, p ).sendToTarget();
				
				if ( PicturePasswordUtils.getLockedOut( FaceLockService.this ) )
				{
					lockOut( false );
				}
			}

			@Override
			public void stopUi() throws RemoteException
			{
				FaceLockService.this.mHandler.sendEmptyMessage( MSG_SERVICE_DISCONNECTED );
			}

			@Override
			public void registerCallback( IFaceLockCallback callback ) throws RemoteException
			{
				FaceLockService.this.mCallback = callback;
			}

			@Override
			public void unregisterCallback( IFaceLockCallback callback ) throws RemoteException
			{
				FaceLockService.this.mCallback = null;
			}
		};
	}
	
	Handler mHandler;
	private PicturePasswordView mPicturePassword;
	private long mLastFailure;
	private int mFailureCounter = 0;
	private int mTotalFailureCounter = 0;
	
	private ValueAnimator mFadeOut;
	
	@Override
	public void onCreate()
	{
		mHandler = new Handler( this );
		
		Preferences prefs = new Preferences();
		PicturePasswordUtils.getUnlockData( prefs, this );
		
		mView = View.inflate( this, R.layout.service, null );
		mWindowManager = ( WindowManager ) getSystemService( "window" );
		
		final PicturePasswordView picturePassword = ( PicturePasswordView ) mView.findViewById( R.id.picture_password );
		picturePassword.setImageBitmap( prefs.image );
		picturePassword.setUnlockNumber( prefs.unlockNumber, prefs.unlockNumberX, prefs.unlockNumberY );
		picturePassword.setRandomize( prefs.randomize );
		picturePassword.setGridSize( prefs.gridSize );
		
		picturePassword.setOnFingerUpListener( new OnFingerUpListener()
		{
			@Override
			public void onFingerUp( PicturePasswordView picturePassword, boolean shouldUnlock )
			{
				if ( shouldUnlock )
				{
					if ( FaceLockService.this.mCallback != null )
					{
						try
						{
							FaceLockService.this.mCallback.unlock();
						}
						catch ( RemoteException e )
						{
							// i hope this never happens
							e.printStackTrace();
						}
					}
				}
				else
				{
					if ( System.currentTimeMillis() - mLastFailure < 800 )
					{
						mFailureCounter++;
						
						if ( mFailureCounter >= 2 )
						{
							FaceLockService.this.lockForSeconds( 5 );
							mFailureCounter = 0;
						}
					}
					
					mLastFailure = System.currentTimeMillis();
					
					mTotalFailureCounter++;
					PicturePasswordUtils.setFailureCounter( FaceLockService.this, mTotalFailureCounter );
					
   					if ( mTotalFailureCounter > 9 )
					{
						lockOut( true );
					}

					picturePassword.reset();
				}
			}
		} );
		
		mFadeOut = ValueAnimator.ofFloat( 1, 0 );

		mFadeOut.addListener( new AnimatorListener()
		{
			
			@Override
			public void onAnimationStart( Animator animation )
			{
			}
			
			@Override
			public void onAnimationRepeat( Animator animation )
			{
			}
			
			@Override
			public void onAnimationEnd( Animator animation )
			{
				FaceLockService.this.mHandler.sendEmptyMessage( MSG_CANCEL );
			}
			
			@Override
			public void onAnimationCancel( Animator animation )
			{
			}
		} );

		mFadeOut.addUpdateListener( new AnimatorUpdateListener()
		{
			
			@Override
			public void onAnimationUpdate( ValueAnimator animation )
			{
				if ( FaceLockService.this.mLayoutParams != null )
				{
					FaceLockService.this.mLayoutParams.flags = LayoutParams.FLAG_NOT_FOCUSABLE | LayoutParams.FLAG_NOT_TOUCHABLE;
					FaceLockService.this.mLayoutParams.alpha = ( Float ) animation.getAnimatedValue();
					FaceLockService.this.mWindowManager.updateViewLayout( FaceLockService.this.mView, FaceLockService.this.mLayoutParams );
				}
			}
		} );
		
		mPicturePassword = picturePassword;
		
		if ( PicturePasswordUtils.getWaitTime( this ) > System.currentTimeMillis() / 1000 )
		{
			mPicturePassword.setShowNumbers( false );
			lockForSeconds( PicturePasswordUtils.getWaitTime( this ) - System.currentTimeMillis() / 1000 );
		}
		
		mTotalFailureCounter = PicturePasswordUtils.getFailureCounter( this );
	}
	
	public void lockOut( boolean animate )
	{
		Log.d( "PicturePassword", "Locking out." );
		PicturePasswordUtils.setLockedOut( this, true );
		
		if ( animate )
		{
			mFadeOut.start();
		}
		else
		{
			mHandler.sendEmptyMessage( MSG_CANCEL );
		}
	}
	
	public void lockForSeconds( long seconds )
	{
		mPicturePassword.setEnabled( false );
		
		final long sleepTime = seconds * 1000;
		final PicturePasswordView view = mPicturePassword;
		final Handler handler = mHandler;

		PicturePasswordUtils.setWaitTime( this, seconds );
		
		if ( view.isShowNumbers() )
		{
			view.setShowNumbers( false, false );
		}
		
		Thread thread = new Thread()
		{
			@Override
			public void run()
			{
				try
				{
					sleep( sleepTime );
					
					handler.sendEmptyMessage( MSG_ENABLE );
				}
				catch ( InterruptedException e )
				{
					e.printStackTrace();
				}
			}
		};

		thread.start();
	}

	@Override
	public IBinder onBind( Intent intent )
	{
		Log.d( "PicturePasswordServ", "onBind" );
		return binder;
	}

	@Override
	public boolean handleMessage( Message msg )
	{
		switch ( msg.what )
		{
			case MSG_SERVICE_CONNECTED:
				mView.setVisibility( View.VISIBLE );
				mWindowManager.addView( mView, ( LayoutParams ) msg.obj );
				return true;
				
			case MSG_SERVICE_DISCONNECTED:
				if ( mView != null )
				{
					mView.setVisibility( View.GONE );
					mWindowManager.removeView( mView );
				}
				return true;
				
			case MSG_CANCEL:
				if ( mCallback != null )
				{
					try
					{
						mCallback.cancel();
					}
					catch ( RemoteException e )
					{
						// welp
						e.printStackTrace();
					}
				}
				return true;
				
			case MSG_ENABLE:
				mPicturePassword.reset();
				mPicturePassword.setShowNumbers( true, true );
				mPicturePassword.setEnabled( true );
				return true;
		}
		return false;
	}
}
