package com.android.internal.policy;

import android.graphics.Bitmap;
import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;
import android.util.Log;

public interface IFaceLockInterface extends IInterface
{
	public static abstract class Stub extends Binder implements IFaceLockInterface
	{
		public abstract void registerCallback( IFaceLockCallback callback ) throws RemoteException;

		public abstract void startUi( IBinder ibinder, int x, int y, int w, int h, boolean useLiveliness ) throws RemoteException;

		public abstract void stopUi() throws RemoteException;
		
		public Binder asBinder() { return this; } 

		public abstract void unregisterCallback( IFaceLockCallback callback ) throws RemoteException;
		
		public abstract Bitmap getImage() throws RemoteException;
	}
}
