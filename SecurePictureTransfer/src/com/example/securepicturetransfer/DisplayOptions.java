package com.example.securepicturetransfer;

import java.security.NoSuchAlgorithmException;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

/***
 * This class is displaying options after clicking on a contact's name
 * @author fa45
 *
 */
public class DisplayOptions  extends Activity
{	
	TextView nameT, numberT;
	public static TextView status;
	
	@Override  
	public void onCreate(Bundle savedInstanceState) 
	{
		super.onCreate(savedInstanceState);  
	    setContentView(R.layout.display_name_options);
	    
	    Intent myIntent = getIntent(); 
	    String name = myIntent.getStringExtra("name"); 
	    String number= myIntent.getStringExtra("number");
	    Log.d("selected item 2", name+" : "+number);
	    
	    nameT = (TextView)findViewById(R.id.nameHeading);
	    numberT = (TextView)findViewById(R.id.numberHeading);
	    status  = (TextView) findViewById(R.id.tv_status);

	    nameT.setText(name);
	    numberT.setText(number);
	    status.setText("");
	    
	    
	    final Button sms = (Button) findViewById(R.id.SMS);
	    final Button data = (Button) findViewById(R.id.Data);
	    
	    sms.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) 
            {
            	MainActivity.exchangeKeys(numberT.getText().toString());
            	/*Intent intent = new Intent(DisplayOptions.this, SMSActivity.class);
            	intent.putExtra("name",nameT.getText());
	             intent.putExtra("number",numberT.getText());
            	startActivity(intent);
                */
            }
        });
	    
	    /*data.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) 
            {
            	try 
            	{
            		MainActivity.exchangeKeys(numberT.getText().toString());
            		//while (!MainActivity.exchangeKeys);
            		
					
					
					
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
            }
        });*/
	     
	    
	}
	
	

}
