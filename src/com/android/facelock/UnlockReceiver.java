package com.android.facelock;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class UnlockReceiver extends BroadcastReceiver
{
	@Override
	public void onReceive( Context context, Intent intent )
	{
		PicturePasswordUtils.setFailureCounter( context, 0 );
		PicturePasswordUtils.setLockedOut( context, false );
	}
}
