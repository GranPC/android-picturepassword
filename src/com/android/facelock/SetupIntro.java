package com.android.facelock;

import java.io.FileDescriptor;

import com.android.facelock.PicturePasswordView.OnFingerUpListener;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.PointF;
import android.net.Uri;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.util.DisplayMetrics;
import android.util.SparseIntArray;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView.ScaleType;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

public class SetupIntro extends Activity implements View.OnClickListener
{
	private static final int STEP_INTRO = 0;
	private static final int STEP_CHOOSE_NUMBER = 1;
	private static final int STEP_CONFIRM_NUMBER = 2;
	
	private static final int LOAD_IMAGE_CODE = 1;
	
	private int mStep;
	
	private SparseIntArray mButtonIds;
	private Dialog mDialog;
	private int mChosenNumber;
	
	private Bitmap mBitmap;
	private PointF mUnlockPosition;
	private int mGridSize;
	private boolean mRandomize;

	@Override
	protected void onCreate( Bundle savedInstanceState )
	{
		mStep = STEP_INTRO;
		
		super.onCreate( savedInstanceState );
		setContentView( R.layout.activity_setup_intro );
		
		mButtonIds = new SparseIntArray();
		
		// there must be a better way to do this. I don't think R
		// IDs are guaranteed to stay in order though, so a loop
		// wouldn't really cut it.

		mButtonIds.put( R.id.button0, 0 );
		mButtonIds.put( R.id.button1, 1 );
		mButtonIds.put( R.id.button2, 2 );
		mButtonIds.put( R.id.button3, 3 );
		mButtonIds.put( R.id.button4, 4 );
		mButtonIds.put( R.id.button5, 5 );
		mButtonIds.put( R.id.button6, 6 );
		mButtonIds.put( R.id.button7, 7 );
		mButtonIds.put( R.id.button8, 8 );
		mButtonIds.put( R.id.button9, 9 );
		
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
					mBitmap = image;
					
					imageview.setImageBitmap( image );
					
					imageview.setGridSize( 5 );
					
					final SeekBar gridSize = ( SeekBar ) findViewById( R.id.grid_size );
					gridSize.setMax( 8 - 4 );
					gridSize.setProgress( imageview.getGridSize() - 4 );
					
					final String sizeTextOriginal = getResources().getString( R.string.grid_size );
					final TextView sizeText = ( TextView ) findViewById( R.id.grid_size_text );
					
					sizeText.setText( sizeTextOriginal + " " + imageview.getGridSize() + "x" + imageview.getGridSize() );
					
					gridSize.setOnSeekBarChangeListener( new OnSeekBarChangeListener()
					{
						@Override
						public void onStopTrackingTouch( SeekBar seekBar )
						{
						}

						@Override
						public void onProgressChanged( SeekBar seekBar, int progress, boolean fromUser )
						{
							if ( fromUser )
							{
								imageview.setGridSize( progress + 4 );
								sizeText.setText( sizeTextOriginal + " " + ( progress + 4 ) + "x" + ( progress + 4 ) );
							}
						}

						@Override
						public void onStartTrackingTouch( SeekBar seekBar )
						{
						}
					} );
					
					setListeners();
					
					AlertDialog.Builder builder = new AlertDialog.Builder( this );
					LayoutInflater inflater = getLayoutInflater();
					
					builder.setView( inflater.inflate( R.layout.popup_select_number, null ) );
					
					Dialog dlg = builder.create();
					dlg.setCancelable( false );
					dlg.show();
					
					mDialog = dlg;
					
					for ( int i = 0; i < 10; i++ )
					{
						( ( Button ) dlg.findViewById( mButtonIds.keyAt( mButtonIds.indexOfValue( i ) ) ) ).setOnClickListener( this );
					}
				}
				
				return;
			}
		}
		finish();
	}
	
	private void confirmNumber()
	{
		final TextView title = ( TextView ) findViewById( R.id.headerText );
		title.setText( R.string.confirm_title );
		
		final Button nextButton = ( Button ) findViewById( R.id.next_button );
		nextButton.setEnabled( false );
		
		final SeekBar gridSize = ( SeekBar ) findViewById( R.id.grid_size );
		gridSize.setEnabled( false );
		
		final CheckBox randomize = ( CheckBox ) findViewById( R.id.randomize );
		randomize.setEnabled( false );
		
		PicturePasswordView passwordView = ( PicturePasswordView ) findViewById( R.id.chosenImage );
		mGridSize = passwordView.getGridSize();
		mRandomize = randomize.isChecked();
				
		int unlockNumber = mChosenNumber;
		PointF unlockPosition = passwordView.getHighlightPosition();
		mUnlockPosition = unlockPosition;
		
		passwordView.setFocusNumber( -1 );
		passwordView.reset();
		passwordView.setUnlockNumber( unlockNumber, unlockPosition.x, unlockPosition.y );
		passwordView.setOnFingerUpListener( new OnFingerUpListener()
		{
			@Override
			public void onFingerUp( PicturePasswordView picturePassword, boolean shouldUnlock )
			{
				if ( shouldUnlock )
				{
					nextButton.setEnabled( true );
					picturePassword.setEnabled( false );
					picturePassword.setHighlightUnlockNumber( true );
					title.setText( R.string.done_title );
				}
				else
				{
					title.setText( R.string.try_again );
					picturePassword.reset();
				}
			}
		} );
		
		mStep = STEP_CONFIRM_NUMBER;
	}
	
	private void saveData()
	{
		if ( !PicturePasswordUtils.saveUnlockData( this, mBitmap, mGridSize, mRandomize, mChosenNumber, mUnlockPosition ) )
		{
			// uh oh
			finish();
		}
		else
		{
			Intent chooseIntent = new Intent();
			chooseIntent.setClassName( "com.android.settings", "com.android.settings.ChooseLockGeneric" );
			chooseIntent.putExtra( "lockscreen.biometric_weak_fallback", true );
			startActivity( chooseIntent );
			finish();
		}
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
				confirmNumber();
			}
			else if ( mStep == STEP_CONFIRM_NUMBER )
			{
				saveData();
			}
		}
		else if ( mButtonIds.indexOfKey( which.getId() ) > -1 )
		{
			mDialog.dismiss();
			mChosenNumber = mButtonIds.get( which.getId() );

			final PicturePasswordView imageview = ( PicturePasswordView ) findViewById( R.id.chosenImage );
			imageview.setFocusNumber( mChosenNumber );
			imageview.setShowNumbers( true, true );
		}
	}
}
