package com.example.securepicturetransfer;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.EditText;
import android.widget.ListView;

public class SelectContact extends Activity 
{
	private ListView mainListView ;  
	private CustomListViewAdapter listAdapter ;
	private List<RowItem> rowItems;  
    private EditText inputSearch;
    
    
	@Override  
	public void onCreate(Bundle savedInstanceState) 
	{
		super.onCreate(savedInstanceState);  
	    setContentView(R.layout.contacts);  
	    
	    
	 // Find the ListView resource.   
	    mainListView = (ListView) findViewById( R.id.mainListView );  
	  
	    rowItems = new ArrayList<RowItem>();
	    
	    listAdapter = new CustomListViewAdapter(this, R.layout.simplerow, rowItems);
	    inputSearch = (EditText) findViewById(R.id.inputSearch);
	    
	    readContacts();
	    // Set the ArrayAdapter as the ListView's adapter.  
	    mainListView.setAdapter( listAdapter );     
	    mainListView.setOnItemClickListener(new OnItemClickListener()
	    {
	       @Override
		public void onItemClick(AdapterView<?> parent, View view, int position,
				long id) {
			RowItem value = (RowItem)parent.getItemAtPosition(position); 
            Log.d("selected item", value.getName()+" : "+value.getNumber());
            
            Intent intent = new Intent(getBaseContext(), DisplayOptions.class);
            intent.putExtra("name",value.getName());
            intent.putExtra("number",value.getNumber());
            startActivity(intent);
			
		}
	    });
	    
	    inputSearch.addTextChangedListener(new TextWatcher() {
	        
	        @Override
	        public void onTextChanged(CharSequence cs, int arg1, int arg2, int arg3) 
	        {
	            // When user changed the Text
	            //MainActivity.this.listAdapter.getFilter().filter(cs);   
	        	Log.d("*** Search value changed: " , cs.toString());
	        	listAdapter.doFilter(mainListView, cs.toString(), rowItems);
	        }
	         
	        @Override
	        public void beforeTextChanged(CharSequence arg0, int arg1, int arg2,
	                int arg3) {
	            // TODO Auto-generated method stub
	             
	        }
	         
			@Override
			public void afterTextChanged(Editable s) {
				Log.d("*** Search value changedjfjfgjfgjfgjfgj: " , s.toString());
	        	listAdapter.doFilter(mainListView, s.toString(), rowItems);      
				
			}

			
	    });

	   
	}


	private void readContacts() 
	{
		ContentResolver cr = getContentResolver();
		Cursor cur = cr.query(ContactsContract.Contacts.CONTENT_URI,null, null, null, ContactsContract.Contacts.DISPLAY_NAME + " ASC");

		if (cur.getCount() > 0)
		{
			while (cur.moveToNext())
			{
				String id = cur.getString(cur.getColumnIndex(ContactsContract.Contacts._ID));
                String name = cur.getString(cur.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME));

             // Using the contact ID now we will get contact phone number
                Cursor cursorPhone = getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                        new String[]{ContactsContract.CommonDataKinds.Phone.NUMBER},
         
                        ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ? AND " +
                                ContactsContract.CommonDataKinds.Phone.TYPE + " = " +
                                ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE,
         
                        new String[]{id},
                        null);
         
                String contactNumber = "000";
                if (cursorPhone.moveToFirst()) {
                    contactNumber = cursorPhone.getString(cursorPhone.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
                }
                //Log.i("number", name +" : " + contactNumber);
                cursorPhone.close();
         
                int image = R.drawable.ic_contact_picture;
                
                
                RowItem item = new RowItem(image, name, contactNumber);
            	rowItems.add(item);                
                
                
                
			}
		}
		
	}

}
