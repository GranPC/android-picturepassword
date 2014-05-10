package com.android.facelock;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class SetupEndScreen extends Activity implements View.OnClickListener
{
	@Override
	protected void onCreate( Bundle savedInstanceState )
	{
		super.onCreate( savedInstanceState );
		setContentView( R.layout.activity_setup_end );
		
		setListeners();
	}
	
	public void setListeners()
	{
		final Button btnNext = ( Button ) findViewById( R.id.next_button );
		btnNext.setOnClickListener( this );
	}
	
	public void onClick( View which )
	{
		if ( which.getId() == R.id.next_button )
		{
			finish();
		}
	}
}
