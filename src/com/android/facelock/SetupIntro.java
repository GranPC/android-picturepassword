package com.android.facelock;

import android.net.Uri;
import android.os.Bundle;
import android.app.Activity;
import android.content.Intent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

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
	
	@Override
	public void onActivityResult( int requestCode, int resultCode, Intent resultData )
	{
		if ( requestCode == LOAD_IMAGE_CODE && resultCode == Activity.RESULT_OK )
		{
			Uri uri = null;
			if ( resultData != null )
			{
				uri = resultData.getData();
				
				mStep = STEP_CHOOSE_NUMBER;
				setContentView( R.layout.activity_setup_number );
				
				setListeners();
				
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