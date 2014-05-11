package com.android.facelock;

import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;

import android.content.res.Resources;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

public class XposedHelper implements IXposedHookLoadPackage
{
	public void handleLoadPackage( final LoadPackageParam lpparam ) throws Throwable
	{
		try
		{
			if ( !lpparam.packageName.equals( "com.android.keyguard" ) ) return;
			
			// Disable cancel button
			findAndHookMethod( "com.android.keyguard.FaceUnlock", lpparam.classLoader, "initializeView", View.class, new XC_MethodHook()
			{
				@Override
				protected void afterHookedMethod( MethodHookParam param ) throws Throwable
				{
					View v = ( View ) param.args[ 0 ];
					Resources res = v.getResources();
					ImageButton cancel = ( ImageButton ) v.findViewById( res.getIdentifier( "face_unlock_cancel_button", "id", "com.android.keyguard" ) );
					cancel.setVisibility( View.GONE );
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
		}
		catch ( Throwable e )
		{
			XposedBridge.log( e );
		}
	}
}