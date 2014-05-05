package com.android.facelock;

import java.io.FileDescriptor;

import android.net.Uri;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.NumberPicker;
import android.widget.NumberPicker.*;

public class SetupIntro extends Activity implements View.OnClickListener
{
	private static final int STEP_INTRO = 0;
	private static final int STEP_CHOOSE_NUMBER = 1;
	
	private static final int LOAD_IMAGE_CODE = 1;
	
	private int mStep;

	@Override
	protected void onCreate( Bundle savedInstanceState )
	{
		mStep = STEP_INTRO;
		
		super.onCreate( savedInstanceState );
		setContentView( R.layout.activity_setup_intro );
		
		setListeners();
	}
	
	private void setListeners()
	{
		final Button btnCancel = ( Button ) findViewById( R.id.cancel_button );
		final Button btnNext = ( Button ) findViewById( R.id.next_button );
		
		btnCancel.setOnClickListener( this );
		btnNext.setOnClickListener( this );
	}
	
	private void chooseImage()
	{
		Intent intent = new Intent( Intent.ACTION_OPEN_DOCUMENT );
		intent.addCategory( Intent.CATEGORY_OPENABLE );
		intent.setType( "image/*" );
		startActivityForResult( intent, LOAD_IMAGE_CODE );
	}

	public static int calculateInSampleSize( BitmapFactory.Options options, int reqWidth, int reqHeight )
	{
		// Raw height and width of image
		final int height = options.outHeight;
		final int width = options.outWidth;
		int inSampleSize = 1;

		if ( height > reqHeight || width > reqWidth )
		{
			final int halfHeight = height / 2;
			final int halfWidth = width / 2;

			// Calculate the largest inSampleSize value that is a power of 2 and
			// keeps both height and width larger than the requested height and
			// width.
			
			while ( ( halfHeight / inSampleSize ) > reqHeight
					&& ( halfWidth / inSampleSize ) > reqWidth )
			{
				inSampleSize *= 2;
			}
		}

		return inSampleSize;
	}

	@Override
	public void onActivityResult( int requestCode, int resultCode, Intent resultData )
	{
		if ( requestCode == LOAD_IMAGE_CODE && resultCode == Activity.RESULT_OK )
		{
			Uri uri = null;
			if ( resultData != null )
			{
				Bitmap image = null;
				
				mStep = STEP_CHOOSE_NUMBER;
				setContentView( R.layout.activity_setup_number );
				
				final PicturePasswordView imageview = ( PicturePasswordView ) findViewById( R.id.chosenImage );
				
				DisplayMetrics metrics = getResources().getDisplayMetrics();
				
				int vw = ( int ) TypedValue.applyDimension( TypedValue.COMPLEX_UNIT_DIP, 300, metrics );
				int vh = vw;
				
				uri = resultData.getData();
				try
				{
					// TODO: move to another thread.
					
					ParcelFileDescriptor parcelFileDescriptor = getContentResolver().openFileDescriptor( uri, "r" );
					FileDescriptor fileDescriptor = parcelFileDescriptor.getFileDescriptor();
					
					BitmapFactory.Options opts = new BitmapFactory.Options();
					opts.inJustDecodeBounds = true;
					
					BitmapFactory.decodeFileDescriptor( fileDescriptor, null, opts );
					
					opts.inSampleSize = calculateInSampleSize( opts, vw, vh );
					opts.inJustDecodeBounds = false;
					
					image = BitmapFactory.decodeFileDescriptor( fileDescriptor, null, opts );
					
					parcelFileDescriptor.close();
				}
				catch ( Exception e )
				{
					e.printStackTrace();
				}
				
				if ( image != null )
				{
					imageview.setImageBitmap( image );
					imageview.setScaleType( ScaleType.CENTER_CROP );
					
					imageview.setGridSize( 5 );
					
					final NumberPicker gridSize = ( NumberPicker ) findViewById( R.id.grid_size );
					gridSize.setMinValue( 3 );
					gridSize.setMaxValue( 10 );
					gridSize.setValue( imageview.getGridSize() );
					gridSize.setOnValueChangedListener( new OnValueChangeListener()
					{
						@Override
						
						public void onValueChange( NumberPicker picker, int old, int newValue )
						{
							imageview.setGridSize( newValue );
						}
					} );
					
					setListeners();
				}
				
				return;
			}
		}
		finish();
	}
	
	public void onClick( View which )
	{
		if ( which.getId() == R.id.cancel_button )
		{
			finish();
		}
		else if ( which.getId() == R.id.next_button )
		{
			if ( mStep == STEP_INTRO )
			{
				chooseImage();
			}
			else if ( mStep == STEP_CHOOSE_NUMBER )
			{
				
			}
		}
	}
}
