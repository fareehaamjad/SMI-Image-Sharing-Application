package com.example.securepicturetransfer;


import java.util.ArrayList;
import java.util.List;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class DatabaseHandler extends SQLiteOpenHelper
{

	 // Database Version
     private static final int DATABASE_VERSION = 1;
 
     // Database Name
     private static final String DATABASE_NAME = "androidCrypto";
     
     // Table Names
     private static final String TABLE_msg = "msgs";
     private static final String TABLE_challange = "challange";
     private static final String TABLE_temp_key = "tempKeys";
     private static final String TABLE_bt_challange = "btChallange";
     private static final String TABLE_bt_temp_key = "btKeysTemp";
     private static final String TABLE_keys = "Keys";
     
     
     // Contacts Table Columns names
     private static final String KEY_ID = "id";
     private static final String KEY_PH_NO = "phone_number";
     private static final String KEY_message = "message";
     private static final String KEY_verify = "verify_metadata_nonce";
     private static final String KEY_bt_device_name = "bt_device_name";
     private static final String KEY_public_key = "public_key";
     private static final String KEY_data_ip = "data_IP";
     
	 public DatabaseHandler(Context context) 
	 {
	        super(context, DATABASE_NAME, null, DATABASE_VERSION);
	 }

	@Override
	public void onCreate(SQLiteDatabase db) 
	{
		String CREATE_STRORE_MESSAGES_TABLE = "CREATE TABLE " + TABLE_msg + "("
                + KEY_ID + " INTEGER PRIMARY KEY," 
                + KEY_PH_NO + " TEXT,"
                + KEY_message + " TEXT" + ")";
        db.execSQL(CREATE_STRORE_MESSAGES_TABLE);
        
        String CREATE_STORE_CHALLANGE_TABLE = "CREATE TABLE " + TABLE_temp_key + "("
                + KEY_ID + " INTEGER PRIMARY KEY," 
                + KEY_PH_NO + " TEXT,"
        		+ KEY_public_key + " TEXT" + ")";
        db.execSQL(CREATE_STORE_CHALLANGE_TABLE);
        
        String CREATE_STORE_KEY_TABLE = "CREATE TABLE " + TABLE_challange + "("
                + KEY_ID + " INTEGER PRIMARY KEY," 
                + KEY_PH_NO + " TEXT,"
                + KEY_verify + " TEXT" + ")";
        db.execSQL(CREATE_STORE_KEY_TABLE);
        
        String CREATE_STORE_BT_CHALLANGE_TABLE = "CREATE TABLE " + TABLE_bt_challange + "("
                + KEY_ID + " INTEGER PRIMARY KEY," 
                + KEY_bt_device_name + " TEXT,"
        		+ KEY_verify + " TEXT" + ")";
        db.execSQL(CREATE_STORE_BT_CHALLANGE_TABLE);
        
        String CREATE_STORE_BT_KEY_TABLE = "CREATE TABLE " + TABLE_bt_temp_key + "("
                + KEY_ID + " INTEGER PRIMARY KEY," 
                + KEY_bt_device_name + " TEXT,"
        		+ KEY_public_key + " TEXT" + ")";
        db.execSQL(CREATE_STORE_BT_KEY_TABLE);
        
        String CREATE_STORE_KEYS_TABLE = "CREATE TABLE " + TABLE_keys + "("
                + KEY_ID + " INTEGER PRIMARY KEY," 
                + KEY_PH_NO + " TEXT,"
        		+ KEY_public_key + " TEXT" + ")";
        db.execSQL(CREATE_STORE_KEYS_TABLE);
        
       
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) 
	{
		// Drop older table if existed
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_msg);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_challange);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_temp_key);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_bt_challange);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_bt_temp_key);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_keys);
        
        // Create tables again
        onCreate(db);
	}
	
	// Adding new message
    public void addNewMsg(Message msg) 
    {
        SQLiteDatabase db = this.getWritableDatabase();
 
        ContentValues values = new ContentValues();
        values.put(KEY_PH_NO, msg.number); // Contact Phone
        values.put(KEY_message, msg.message); // message
 
        // Inserting Row
        db.insert(TABLE_msg, null, values);
        db.close(); // Closing database connection
    }
    
    // Adding new keys[permanent]
    public void addNewKey(String number, String publicKey) 
    {
    	
    	SQLiteDatabase db = this.getReadableDatabase();
    	
    	Cursor cursor = db.query(TABLE_keys, new String[] { KEY_public_key }, KEY_PH_NO + "=?",
                new String[] { number }, null, null, null, null);
    	if (cursor.moveToFirst())
        {
    		Log.i("db", "Public key found");
    		
    		if (cursor.getString(cursor.getColumnIndex(KEY_public_key)).equalsIgnoreCase(publicKey))
    		{
    			//do nothing
    		}
    		else
    		{
    			Log.i("db", "Public key found but is different. Updating records");
    			
    			
    			ContentValues values = new ContentValues();
    		    values.put(KEY_public_key, publicKey);
    		 
    		    // updating row
    		    db.update(TABLE_keys, values, KEY_PH_NO + " = ?",
    		            new String[] { number });
    		    
    		    
    	        db.close(); // Closing database connection
    	        
    		}
        	
        }
        else
        {
        	Log.i("db", "No public key found; inserting the values in the database");
        	 ContentValues values = new ContentValues();
             values.put(KEY_PH_NO, number); // Contact Phone
             values.put(KEY_public_key, publicKey); // Key
      
             // Inserting Row
             db.insert(TABLE_keys, null, values);
             db.close(); // Closing database connection
            
        }
    	      
    }
    
    
    //get public Key[permanent]
    public String getKey(String no)
	{
		SQLiteDatabase db = this.getReadableDatabase();
    	
    	Cursor cursor = db.query(TABLE_keys, new String[] { KEY_public_key }, KEY_PH_NO + "=?",
                new String[] { no }, null, null, null, null);
    	if (cursor.moveToFirst())
        {
        	Log.i("db", "public key found");
        	 cursor.moveToFirst();
        	 // return challenge
        	 Log.i("db", cursor.getString(cursor.getColumnIndex(KEY_public_key)));
        	 db.close(); // Closing database connection
        	 
             return cursor.getString(cursor.getColumnIndex(KEY_public_key));
        }
        else
        {
        	Log.i("db", "No public key found");
        	db.close(); // Closing database connection
            return "null";
        }
	}
    
    
    //Adding new bt challenge
    public void addNewBTChallange(String device, String challenge) 
    {
    	SQLiteDatabase db = this.getWritableDatabase();
    	ContentValues values = new ContentValues();
        values.put(KEY_bt_device_name, device); // Contact Phone
        values.put(KEY_verify, challenge); // Challenge
        
        // Inserting Row
        db.insert(TABLE_bt_challange, null, values);
        db.close(); // Closing database connection
    }
    
    // Getting All Messages
    public List<Message> getAllContacts() 
    {
    	List<Message> allMsgs = new ArrayList<Message>();
        
    	// Select All Query
        String selectQuery = "SELECT  * FROM " + TABLE_msg;
 
        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, null);
 
        // looping through all rows and adding to list
        if (cursor.moveToFirst()) {
            do {
                Message msg = new Message();
                msg.number = cursor.getString(1);
                msg.message = cursor.getString(2);
                
                // Adding contact to list
                allMsgs.add(msg);
            } while (cursor.moveToNext());
        }
 
        db.close(); // Closing database connection
        // return contact list
        return allMsgs;
    }
 
    //Adding new challenge
    public void addNewChallange(String number, String challenge)
    {
    	SQLiteDatabase db = this.getWritableDatabase();
    	
    	 ContentValues values = new ContentValues();
    	 values.put(KEY_PH_NO, number); // Contact Phone
         values.put(KEY_verify, challenge); // Challenge
  
         // Inserting Row
         db.insert(TABLE_challange, null, values);
         db.close(); // Closing database connection
    	 
    }
    
    public void deleteChallange(String number) 
    {
        SQLiteDatabase db = this.getWritableDatabase();
    
        db.delete(TABLE_challange, KEY_PH_NO + " = ?",
        		new String[] { number });
               
        db.close();
    }
    
    //getting the challenge associated with the number
    public String getChallenge(String no)
    {
    	SQLiteDatabase db = this.getReadableDatabase();
    	
    	Cursor cursor = db.query(TABLE_challange, new String[] { KEY_verify }, KEY_PH_NO + "=?",
                new String[] { no }, null, null, null, null);

    	if (cursor.moveToFirst())
        {
        	Log.i("db", "number found");
        	 cursor.moveToFirst();
        	 // return challenge
        	 Log.i("db", cursor.getString(cursor.getColumnIndex(KEY_verify)));
        	 db.close(); // Closing database connection
        	 
             return cursor.getString(0);
        }
        else
        {
        	Log.i("db", "No number found");
        	db.close(); // Closing database connection
            return "null";
        }

    }

    public void AddNewTempKeys(String no, String publicKey)
	{
		SQLiteDatabase db = this.getWritableDatabase();
		
		ContentValues values = new ContentValues();
   	 	values.put(KEY_PH_NO, no); // Contact Phone
        values.put(KEY_public_key, publicKey); // pub key
 
        // Inserting Row
        db.insert(TABLE_temp_key, null, values);
        db.close(); // Closing database connection
	}
    
    
    
    public String getTempKey(String no)
	{
		SQLiteDatabase db = this.getReadableDatabase();
    	
    	Cursor cursor = db.query(TABLE_temp_key, new String[] { KEY_public_key }, KEY_PH_NO + "=?",
                new String[] { no }, null, null, null, null);
    	if (cursor.moveToFirst())
        {
        	Log.i("db", "public key found");
        	 cursor.moveToFirst();
        	 // return challenge
        	 Log.i("db", cursor.getString(cursor.getColumnIndex(KEY_public_key)));
        	 db.close(); // Closing database connection
        	 
             return cursor.getString(cursor.getColumnIndex(KEY_public_key));
        }
        else
        {
        	Log.i("db", "No public key found");
        	db.close(); // Closing database connection
            return "null";
        }
	}
    
    public void deleteTempKey(String no)
	{
		SQLiteDatabase db = this.getWritableDatabase();
	    
        db.delete(TABLE_temp_key, KEY_PH_NO + " = ?",
        		new String[] { no });
               
        db.close();
	}
    
	public void btAddNewChallange(String device, String challenge) 
	{
		SQLiteDatabase db = this.getWritableDatabase();
		
		ContentValues values = new ContentValues();
   	 	values.put(KEY_bt_device_name, device); // Contact Phone
        values.put(KEY_verify, challenge); // Challenge
 
        // Inserting Row
        db.insert(TABLE_bt_challange, null, values);
        db.close(); // Closing database connection
   	 
	}

	public String BTgetChallenge(String deviceName) 
	{
		SQLiteDatabase db = this.getReadableDatabase();
    	
    	Cursor cursor = db.query(TABLE_bt_challange, new String[] { KEY_verify }, KEY_bt_device_name + "=?",
                new String[] { deviceName }, null, null, null, null);
    	if (cursor.moveToFirst())
        {
        	Log.i("db", "number found");
        	 cursor.moveToFirst();
        	 // return challenge
        	 Log.i("db", cursor.getString(cursor.getColumnIndex(KEY_verify)));
        	 db.close(); // Closing database connection
        	 
             return cursor.getString(cursor.getColumnIndex(KEY_verify));
        }
        else
        {
        	Log.i("db", "No number found");
        	db.close(); // Closing database connection
            return "null";
        }
	}
	
	public void BTdeleteChallange(String deviceName) 
    {
        SQLiteDatabase db = this.getWritableDatabase();
    
        db.delete(TABLE_bt_challange, KEY_bt_device_name + " = ?",
        		new String[] { deviceName });
               
        db.close();
    }
   
	public void BTaddNewTempKeys(String deviceName, String publicKey)
	{
		SQLiteDatabase db = this.getWritableDatabase();
		
		ContentValues values = new ContentValues();
   	 	values.put(KEY_bt_device_name, deviceName); // Contact Phone

        values.put(KEY_public_key, publicKey); // pub key
 
        // Inserting Row
        db.insert(TABLE_bt_temp_key, null, values);
        db.close(); // Closing database connection
	}
	
	
	
	public String BTgetTempKey(String deviceName)
	{
		SQLiteDatabase db = this.getReadableDatabase();
    	
    	Cursor cursor = db.query(TABLE_bt_temp_key, new String[] { KEY_public_key }, KEY_bt_device_name + "=?",
                new String[] { deviceName }, null, null, null, null);
    	if (cursor.moveToFirst())
        {
        	Log.i("db", "public key found");
        	 cursor.moveToFirst();
        	 // return challenge
        	 Log.i("db", cursor.getString(cursor.getColumnIndex(KEY_public_key)));
        	 db.close(); // Closing database connection
        	 
             return cursor.getString(cursor.getColumnIndex(KEY_public_key));
        }
        else
        {
        	Log.i("db", "No public key found");
        	db.close(); // Closing database connection
            return "null";
        }
	}
	
	public void BTdeleteTempKey(String deviceName)
	{
		SQLiteDatabase db = this.getWritableDatabase();
	    
        db.delete(TABLE_bt_temp_key, KEY_bt_device_name + " = ?",
        		new String[] { deviceName });
               
        db.close();
	}

	
}
