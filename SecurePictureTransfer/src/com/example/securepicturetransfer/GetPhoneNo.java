package com.example.securepicturetransfer;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

public class GetPhoneNo extends Activity
{

	public EditText phoneNumberField;
	@Override
	public void onCreate(Bundle savedInstanceState) 
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.input_phone_number);
		phoneNumberField = (EditText)findViewById(R.id.number);

		
	}
	
	@Override
	public void onBackPressed() {
		
		Log.i("back presses", "back Button pressed; killing the process");
		android.os.Process.killProcess(android.os.Process.myPid());
	}
	
	public void saveNo(View view)
	{
		showDialog();
		

	}

	private void showDialog() 
	{
		new AlertDialog.Builder(this)
		.setTitle("Please conform")
		.setMessage("Is " +phoneNumberField.getText().toString() +" your number?")
		.setIcon(android.R.drawable.ic_dialog_alert)
		.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {

		    public void onClick(DialogInterface dialog, int whichButton) 
		    {
		    	Log.i("test",  "test etst");
				SharedPreferences.Editor e = MainActivity.s.edit();
				e.putString("phone_number", phoneNumberField.getText().toString());
				Log.i("currentPhoneNo",  phoneNumberField.getText().toString());
				while (!e.commit());
				finish();
		    }})
		 .setNegativeButton(android.R.string.no, null).show();
		
	}

}
