package com.android.facelock;

import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.os.Parcel;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.RelativeLayout;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

public class XposedHelper implements IXposedHookLoadPackage
{
	public void handleLoadPackage( final LoadPackageParam lpparam ) throws Throwable
	{
		try
		{
			if ( !lpparam.packageName.equals( "com.android.keyguard" ) && !lpparam.packageName.equals( "com.android.facelock" ) ) return;
			
			if ( lpparam.packageName.equals( "com.android.facelock" ) )
			{
				findAndHookMethod( "com.android.facelock.FaceLockService", lpparam.classLoader, "shouldFadeIn", new XC_MethodReplacement()
				{
					
					@Override
					protected Object replaceHookedMethod( MethodHookParam param ) throws Throwable
					{
						return true;
					}
				} );
			}
			
			// Disable cancel button and change bg image 1/2
			findAndHookMethod( "com.android.keyguard.FaceUnlock", lpparam.classLoader, "initializeView", View.class, new XC_MethodHook()
			{
				@Override
				protected void afterHookedMethod( MethodHookParam param ) throws Throwable
				{
					View v = ( View ) param.args[ 0 ];
					Resources res = v.getResources();
					ImageButton cancel = ( ImageButton ) v.findViewById( res.getIdentifier( "face_unlock_cancel_button", "id", "com.android.keyguard" ) );
					cancel.setVisibility( View.GONE );
					
					// Change bg image
					XposedHelpers.callMethod( param.thisObject, "start" );
				}
			} );
			
			// Fix padlock button being impossible to press (??)
			findAndHookMethod( "com.android.keyguard.SlidingChallengeLayout", lpparam.classLoader, "onMeasure", int.class, int.class, new XC_MethodHook()
			{
				@Override
				protected void afterHookedMethod( MethodHookParam param ) throws Throwable
				{
					XposedHelpers.callMethod( param.thisObject, "setChallengeInteractive", true );
				}
			} );
			
			// Change background image to user's image 2/2
			findAndHookMethod( "com.android.keyguard.FaceUnlock", lpparam.classLoader, "handleServiceConnected", new XC_MethodHook()
			{
				@Override
				protected void afterHookedMethod( MethodHookParam param ) throws Throwable
				{
					View v = ( View ) XposedHelpers.getObjectField( param.thisObject, "mFaceUnlockView" );
					View spotlightMask = v.findViewById( v.getResources().getIdentifier( "spotlightMask", "id", "com.android.keyguard" ) );
					
					// This ensures we only run the code once!
					if ( spotlightMask.getVisibility() == View.GONE ) return;
					
					spotlightMask.setVisibility( View.GONE );
					
					Object binderProxy = XposedHelpers.getObjectField( XposedHelpers.getObjectField( param.thisObject, "mService" ), "mRemote" );
					
					Parcel data = Parcel.obtain();
					Parcel reply = Parcel.obtain();
					XposedHelpers.callMethod( binderProxy, "transact", 0x4747, data, reply, 0 );
					
					Bitmap img = ( Bitmap ) reply.readValue( null );
					
					// Create an ImageView so it doesn't stretch the image...
					ImageView imageView = new ImageView( v.getContext() );
					imageView.setScaleType( ScaleType.CENTER_CROP );
					imageView.setImageDrawable( new BitmapDrawable( v.getResources(), img ) );
					imageView.setLayoutParams( new RelativeLayout.LayoutParams( LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT ) );
					imageView.setVisibility( View.VISIBLE );
					
					// And add it to the layout
					( ( RelativeLayout ) v ).addView( imageView, 0, imageView.getLayoutParams() );
					
					data.recycle();
					reply.recycle();
				}
			} );
		}
		catch ( Throwable e )
		{
			XposedBridge.log( e );
		}
	}
}