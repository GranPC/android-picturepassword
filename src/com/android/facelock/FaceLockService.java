package com.android.facelock;

import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.Handler.Callback;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;
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
					picturePassword.reset();
				}
			}
		} );
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
				mView.setVisibility( View.INVISIBLE );
				mWindowManager.removeView( mView );
				return true;
		}
		return false;
	}
}
