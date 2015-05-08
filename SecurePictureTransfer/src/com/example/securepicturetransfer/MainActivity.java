package com.example.securepicturetransfer;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.telephony.SmsManager;
import android.text.format.Time;
import android.util.Base64;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

public class MainActivity extends Activity 
{
	Button buttonLoadImage;
	Button send;
	Button cancel;

	public static Bitmap bitmap;
	public static SharedPreferences s;
	public static EncryptionManager encryptionManager;
	static ImageView imageView;

	public static boolean exchangeKeys = false;
	private static boolean exchangeKeysTrying = false;
	private static DatabaseHandler dbHandler;
	public static GPSTracker mGPS;

	public static EncryptImage encImage; 
	private static int RESULT_LOAD_IMAGE = 100;

	private static final String URL = "http://91.230.41.24:81//SMS_Server";
	public static String KEY_nonce = "nonce";
	public static String KEY_message_from = "messageFrom";
	public static String KEY_message_to = "messageTo";
	public static String KEY_message = "message";

	public static String separator = "`~~`";

	public static String private_key = "Private key";
	public static String public_key = "Public key";
	public static String AES_key = "AES key";
	public static String HEADER_DATA = "data";
	public static String HEADER_DATA_METADATA = "data-meta";

	public static String HEADER_METADATA = "meta";
	public static String HEADER_REPLY_METADATA = "rep-meta";
	public static String HEADER_VERIFY_META = "verify-meta";

	static Context context;
	private static Bitmap bmpForDecodedImg;
	private static String status = "";


	//for analysis//////////////////////////////////////////////////
	static boolean isDebugging = true;
	// Debugging
	private static final String TAG = "Bluetooth";
	private static final boolean D = true;

	// Message types sent from the BluetoothChatService Handler
	public static final int MESSAGE_STATE_CHANGE = 1;
	public static final int MESSAGE_READ = 2;
	public static final int MESSAGE_WRITE = 3;
	public static final int MESSAGE_DEVICE_NAME = 4;
	public static final int MESSAGE_TOAST = 5;

	// Key names received from the BluetoothChatService Handler
	public static final String DEVICE_NAME = "device_name";
	public static final String TOAST = "toast";

	// Intent request codes
	private static final int REQUEST_CONNECT_DEVICE = 1;
	private static final int REQUEST_ENABLE_BT = 2;

	// Name of the connected device
	private String mConnectedDeviceName = null;
	// Local Bluetooth adapter
	private BluetoothAdapter mBluetoothAdapter = null;
	// Member object for the chat services
	public static BluetoothCommandService mCommandService = null;


	@Override
	protected void onCreate(Bundle savedInstanceState) 
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		if (isDebugging)
		{
			// Get local Bluetooth adapter
			mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

			// If the adapter is null, then Bluetooth is not supported
			if (mBluetoothAdapter == null) {
				Toast.makeText(this, "Bluetooth is not available", Toast.LENGTH_LONG).show();
				finish();
				return;
			}
		}

		context = this;

		buttonLoadImage = (Button) findViewById(R.id.buttonLoadPicture);
		send = (Button) findViewById(R.id.send);
		cancel = (Button) findViewById(R.id.cancel);

		mGPS = new GPSTracker(this);
		imageView = (ImageView) findViewById(R.id.imgView);

		s= getPreferences(Activity.MODE_PRIVATE);

		encryptionManager = new EncryptionManager( s , "testpassword");

		dbHandler = new DatabaseHandler(this);

		if (s.getString("phone_number", null) == null)
		{
			Log.i("phone number", "getting phone number from user: should only happen once!!");
			getPhoneNo();
		}



		buttonLoadImage.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View arg0) {

				Intent i = new Intent(
						Intent.ACTION_PICK,
						android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);

				startActivityForResult(i, RESULT_LOAD_IMAGE);
			}
		});






	}

	@Override
	protected void onStart() 
	{
		super.onStart();

		if (isDebugging)
		{
			// If BT is not on, request that it be enabled.
			// setupCommand() will then be called during onActivityResult
			if (!mBluetoothAdapter.isEnabled()) 
			{
				Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
				startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
			}
			// otherwise set up the command service
			else 
			{
				if (mCommandService==null)
					setupCommand();
			}
		}


	}

	@Override
	protected void onResume()
	{
		super.onResume();

		if (isDebugging)
		{
			// Performing this check in onResume() covers the case in which BT was
			// not enabled during onStart(), so we were paused to enable it...
			// onResume() will be called when ACTION_REQUEST_ENABLE activity returns.
			if (mCommandService != null) 
			{
				if (mCommandService.getState() == BluetoothCommandService.STATE_NONE) 
				{
					mCommandService.start();
				}
			}
		}
	}

	private void setupCommand() 
	{
		// Initialize the BluetoothChatService to perform bluetooth connections
		mCommandService = new BluetoothCommandService(this, mHandler);
	}

	@Override
	protected void onDestroy() 
	{
		super.onDestroy();

		if (isDebugging)
		{
			if (mCommandService != null)
				mCommandService.stop();
		}
	}

	private void ensureDiscoverable() 
	{
		if (mBluetoothAdapter.getScanMode() !=
				BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
			Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
			discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
			startActivity(discoverableIntent);
		}
	}

	// The Handler that gets information back from the BluetoothChatService
	private final Handler mHandler = new Handler() {
		@Override
		public void handleMessage(android.os.Message msg) {
			switch (msg.what) {
			case MESSAGE_STATE_CHANGE:
				switch (msg.arg1) {
				case BluetoothCommandService.STATE_CONNECTED:
					/*mTitle.setText(R.string.title_connected_to);
                    mTitle.append(mConnectedDeviceName);*/
					Log.i("BT", "Connected to: " + mConnectedDeviceName);
					break;
				case BluetoothCommandService.STATE_CONNECTING:
					// mTitle.setText(R.string.title_connecting);
					Log.i("BT", "connecting...");

					break;
				case BluetoothCommandService.STATE_LISTEN:
				case BluetoothCommandService.STATE_NONE:
					//mTitle.setText(R.string.title_not_connected);
					Log.i("BT", "NOT CONNECTED");

					break;
				}
				break;
			case MESSAGE_DEVICE_NAME:
				// save the connected device's name
				mConnectedDeviceName = msg.getData().getString(DEVICE_NAME);
				Toast.makeText(getApplicationContext(), "Connected to "
						+ mConnectedDeviceName, Toast.LENGTH_SHORT).show();
				break;
			case MESSAGE_TOAST:
				Toast.makeText(getApplicationContext(), msg.getData().getString(TOAST),
						Toast.LENGTH_SHORT).show();
				break;
			}
		}
	};

	@Override
	public boolean onCreateOptionsMenu(Menu menu) 
	{
		if (isDebugging)
		{
			MenuInflater inflater = getMenuInflater();
			inflater.inflate(R.menu.option_menu, menu);

		}

		return true;

	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.scan:
			// Launch the DeviceListActivity to see devices and do scan
			Intent serverIntent = new Intent(this, DeviceListActivity.class);
			startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE);
			return true;
		case R.id.discoverable:
			// Ensure this device is discoverable by others
			ensureDiscoverable();
			return true;
		}
		return false;
	}


	private void getPhoneNo() 
	{
		Intent k = new Intent(this, GetPhoneNo.class);
		startActivity(k);

	}


	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		
		if (requestCode == RESULT_LOAD_IMAGE && resultCode == RESULT_OK && null != data) 
		{
			Uri selectedImage = data.getData();
			String[] filePathColumn = { MediaStore.Images.Media.DATA };

			Cursor cursor = getContentResolver().query(selectedImage,
					filePathColumn, null, null, null);
			cursor.moveToFirst();

			int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
			String picturePath = cursor.getString(columnIndex);
			cursor.close();


			bitmap = BitmapFactory.decodeFile(picturePath);
			imageView.setImageBitmap(bitmap);

			try 
			{
				MainActivity.encImage  = new EncryptImage(MainActivity.bitmap);
			} catch (NoSuchAlgorithmException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			buttonLoadImage.setVisibility(View.INVISIBLE);
			send.setVisibility(View.VISIBLE);
			cancel.setVisibility(View.VISIBLE);



			send.setOnClickListener(new View.OnClickListener() {

				@Override
				public void onClick(View arg0) 
				{
					Intent i = new Intent(MainActivity.this, SelectContact.class);
					startActivity(i);
					//checkKeyExchange();

					/*try 
					{
						encImage = new EncryptImage(bitmap, imageView);
						new Thread(encImage).start();

						while (!encImage.done);
						//Bitmap bmp = BitmapFactory.decodeByteArray(encImage.image, 0, encImage.image.length);
					    //imageView.setImageBitmap(bmp);


					    String AESencImage = encImage.AESEncryptImage();



					    //byte[] decodedImage = encImage.AESdecodeImage(AESencImage);

					    //Bitmap bmp = BitmapFactory.decodeByteArray(decodedImage, 0, encImage.image.length);
					    //imageView.setImageBitmap(bmp);

					    imageView.setImageBitmap(null);

					} catch (NoSuchAlgorithmException e) 
					{
						// TODO Auto-generated catch block
						e.printStackTrace();
					}*/


				}


			});

			cancel.setOnClickListener(new View.OnClickListener() {

				@Override
				public void onClick(View arg0) 
				{
					buttonLoadImage.setVisibility(View.VISIBLE);
					send.setVisibility(View.INVISIBLE);
					cancel.setVisibility(View.INVISIBLE);

				}
			});

		}
		else if (isDebugging)
		{
			switch (requestCode) 
			{
			case REQUEST_CONNECT_DEVICE:
				// When DeviceListActivity returns with a device to connect
				if (resultCode == Activity.RESULT_OK) {
					// Get the device MAC address
					String address = data.getExtras()
							.getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
					// Get the BLuetoothDevice object
					BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
					// Attempt to connect to the device
					mCommandService.connect(device);
				}
				break;
			case REQUEST_ENABLE_BT:
				// When the request to enable Bluetooth returns
				if (resultCode == Activity.RESULT_OK) {
					// Bluetooth is now enabled, so set up a chat session
					setupCommand();
				} else {
					// User did not enable Bluetooth or an error occured
					Toast.makeText(this, "R.string.bt_not_enabled_leaving", Toast.LENGTH_SHORT).show();
					finish();
				}
			}
		}


	}

	public static void exchangeKeys(String number) 
	{
		DisplayOptions.status.setText("Starting key exchange");

		exchangeKeysTrying = true;
		//generating nonce
		String nonce = getNonce();

		//generating meta-data
		String metadata = getMetaData(MainActivity.HEADER_METADATA);

		//fetching public key and public modulus
		String publicKey = getPublicKey();

		//store challenge with number
		String metaD =  metadata.replace(MainActivity.HEADER_METADATA, "");

		Log.i("meta after replacing", metaD);

		String challenge = metaD + "," + nonce;
		challenge = challenge.replace(MainActivity.separator, "");



		String challengeNo = number;
		MainActivity.addChallenge(challengeNo, challenge);

		String message = metadata + publicKey + nonce;

		Log.i("message to send", message);

		sendLongSMS(message, number);

	}


	private static void addChallenge(String no, String challenge) 
	{
		Log.i("number of the challange", no);
		Log.i("challange", challenge);

		dbHandler.addNewChallange(no, challenge);

	}


	private static String getPublicKey() 
	{
		return (MainActivity.encryptionManager.s.getString(MainActivity.public_key, null)+MainActivity.separator);
	}


	private static String getMetaData(String header) 
	{
		//phone No:
		String metadata = header;
		metadata += MainActivity.separator;
		metadata += MainActivity.s.getString("phone_number", null);
		metadata += "--";

		//location
		if(MainActivity.mGPS.canGetLocation )
		{
			double mLat = MainActivity.mGPS.getLatitude();
			double mLong = MainActivity.mGPS.getLongitude();

			metadata += Double.toString(mLat);
			metadata += ":";
			metadata += Double.toString(mLong);
			metadata += "--";
		}
		else
		{
			Log.e("Location", "can't get the location");
		}

		//DateTime
		final Calendar c = Calendar.getInstance();
		int mYear = c.get(Calendar.YEAR);
		int mMonth = c.get(Calendar.MONTH);
		int mDay = c.get(Calendar.DAY_OF_MONTH);
		metadata += mDay+"-"+mMonth+"-"+mYear;
		metadata += ":";

		Time time = new Time();
		time.setToNow();
		metadata += time.hour+":"+time.minute;
		metadata += MainActivity.separator;



		return metadata;
	}

	private static String getNonce() 
	{
		SecureRandom sr = new SecureRandom();
		byte[] _nonce = new byte[1024/8];
		sr.nextBytes(_nonce);

		return (_nonce.toString() + MainActivity.separator);
	}


	public static String getNonceP() 
	{
		SecureRandom sr = new SecureRandom();
		byte[] _nonce = new byte[1024/8];
		sr.nextBytes(_nonce);

		return (_nonce.toString());
	}

	

	public static void sendToServer(final String nonce2,final String from,final String to,
			final String msg,final String header) throws ClientProtocolException, IOException 
			{
		DisplayOptions.status.setText("Uploading picture to server");
		Log.i("nonce", nonce2);
		Log.i("from", from);
		Log.i("to", to);
		//Log.i("msg bytes", msg.getBytes() + "");

		Thread thread = new Thread() 
		{
			@Override
			public void run() 
			{
				try 
				{
					//while(true) 
					{
						String nonce = Base64.encodeToString(nonce2.getBytes(), Base64.DEFAULT);
						String fromE = Base64.encodeToString(from.getBytes(), Base64.DEFAULT);
						String toE = Base64.encodeToString(to.getBytes(), Base64.DEFAULT);
						String msgE = Base64.encodeToString(msg.getBytes(), Base64.DEFAULT);
						Log.i("server No", msg.length() + "");
						Log.i("server", msg);
						Log.i("server enc", msgE);

						Log.i("msg bytes", nonce.length() + "");
						Log.i("msg bytes", msgE.length() + "");

						HttpClient client = new DefaultHttpClient();
						HttpPost post = new HttpPost(MainActivity.URL + "/upload.php");
						List<NameValuePair> pairs = new ArrayList<NameValuePair>();
						pairs.add(new BasicNameValuePair(MainActivity.KEY_nonce, nonce));
						pairs.add(new BasicNameValuePair(MainActivity.KEY_message_from, fromE));
						pairs.add(new BasicNameValuePair(MainActivity.KEY_message_to, toE));
						pairs.add(new BasicNameValuePair(MainActivity.KEY_message, msgE));

						post.setEntity(new UrlEncodedFormEntity(pairs));
						
						if (isDebugging)
						{
							byte [] b= (Constants.uploadStart + "\n").getBytes();
							mCommandService.write(b);
						}

						HttpResponse response = client.execute(post);

						HttpEntity resEntityGet = response.getEntity();  
						if (resEntityGet != null) 
						{  
							// do something with the response
							String reply = EntityUtils.toString(resEntityGet);
							Log.i("GET RESPONSE", reply);

							if (!(reply.contains("success")))
							{
								Log.e("PHP error", "PHP upload fail \t" + reply);
								//break;
							}
							else
							{
								if (isDebugging)
								{
									byte [] b= (Constants.uploadEnd + "\n").getBytes();
									mCommandService.write(b);
								}

								sendNonce(to, nonce2, header);
								//break;
							}
						}

					}
				} catch (UnsupportedEncodingException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (ClientProtocolException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		};

		thread.start();



			}


	private static void sendNonce(String to, String nonce2, String header) 
	{
		String msg = header + MainActivity.separator + nonce2;

		//DisplayOptions.status.setText("Messaging nonce");
		status  = "Messaging nonce";
		myHandler.sendEmptyMessage(1);
		Log.i("sendNonce", "sending nonce here.....");
		if (isDebugging)
		{
			byte [] b= (Constants.sendNoce + "\n").getBytes();
			mCommandService.write(b);
		}

		sendLongSMS(msg, to); 
	}


	private static void sendLongSMS(String msg, String to) 
	{
		Log.i("long sms", msg + "    to    " + to);
		SmsManager smsManager = SmsManager.getDefault();
		ArrayList<String> parts = smsManager.divideMessage(msg); 
		smsManager.sendMultipartTextMessage(to, null, parts, null, null);

		//Toast.makeText(getBaseContext(), "Message Sent!", Toast.LENGTH_LONG).show();
	}


	public static void addMsg(final Message msg) throws Exception 
	{
		MainActivity.exchangeKeysTrying = true;

		//check what the message is about
		String[] split = msg.message.split(MainActivity.separator);
		int index = 0;

		String header = split[index++];
		if (header.equals(HEADER_METADATA))
		{
			Toast.makeText(context.getApplicationContext(), "Got meta data", Toast.LENGTH_LONG).show();
			exchangeKeysTrying = true;
			Log.i("metadata", "Got meta-data    " +msg.message);

			//send a reply: new metadata, new nonce, public key, public modulus, signed metadata + nonce of sender
			replyMetadata(msg.message);


		}
		else if (header.equals(HEADER_REPLY_METADATA))
		{
			DisplayOptions.status.setText("Got reply to meta data");
			Log.i("reply metadata", "Got reply meta-data");
			Log.i("reply meta data msg", msg.message);
			//metadata
			String encryptMsg = split[index++];
			//publickey
			String publicKeyOfSender = split[index++];
			//nonce
			encryptMsg += ",";
			encryptMsg += split[index++];

			//the encrytpted challenge reply
			String encChallange = split[index++];
			Log.i("got encrypted challange",encChallange );

			String challenge = encryptionManager.decrypt(encChallange, publicKeyOfSender);

			String actualChallenge = getChallenge(msg.number);

			dbHandler.deleteChallange(msg.number);

			Log.i("challenge",challenge);
			Log.i("actual challenge", actualChallenge);

			if (actualChallenge.equals(challenge))
			{
				DisplayOptions.status.setText("Passed challenge!!");
				Log.i("equal", "the challenge and the challenge reply are equal");

				RreplyMetadata(encryptMsg, msg.number);



				MainActivity.exchangeKeysTrying = false;
				MainActivity.exchangeKeys = true;
				sendPicture(msg.number);
				//connectionText.setText("Connection established!");
				MainActivity.dbHandler.addNewKey(msg.number, publicKeyOfSender);
			}
			else
			{
				DisplayOptions.status.setText("the challenge and the challenge reply are NOT equal");
				Log.i("not equal", "the challenge and the challenge reply are not equal");
				exchangeKeysTrying = false;
				exchangeKeys = false;
			}

		}
		else if (header.equals(HEADER_VERIFY_META))
		{
			Log.i("verify meta", "Got verify meta-data    "+msg.message);

			String encChallange = split[index++];

			String publicKey = dbHandler.getTempKey(msg.number);


			dbHandler.deleteTempKey(msg.number);
			String actualChallenge = getChallenge(msg.number);

			String challenge = encryptionManager.decrypt(encChallange, publicKey);

			dbHandler.deleteChallange(msg.number);

			Log.i("challenge",challenge);
			Log.i("actual challenge", actualChallenge);

			if (actualChallenge.equals(challenge))
			{
				Toast.makeText(context.getApplicationContext(), "EQUAL", Toast.LENGTH_LONG).show();

				Log.i("equal", "the challenge and the challenge reply are equal");


				MainActivity.exchangeKeysTrying = false;
				MainActivity.exchangeKeys = true;

				//connectionText.setText("Connection established!");
				MainActivity.dbHandler.addNewKey(msg.number, publicKey);
			}
			else
			{
				Toast.makeText(context.getApplicationContext(), "NOT EQUAL", Toast.LENGTH_LONG).show();

				Log.i("not equal", "the challenge and the challenge reply are not equal");
				exchangeKeys = false;
				exchangeKeysTrying = false;
			}


		}
		else if (header.equals(MainActivity.HEADER_DATA))
		{
			if (isDebugging)
			{
				byte [] b= (Constants.receiveNonce + "\n").getBytes();
				mCommandService.write(b);
			}

			
			Toast.makeText(context.getApplicationContext(), "Got a picture!!", Toast.LENGTH_LONG).show();

			Log.i("data", "Received picture data nocnce \t" + msg.message);

			//get message from server
			String nonce2 = split[index++];
			Log.i("nonce2", nonce2);
			final String nonce = Base64.encodeToString(nonce2.getBytes(), Base64.DEFAULT);

			String from 	= msg.number;
			Log.i("from", from);
			final String fromE = Base64.encodeToString(from.getBytes(), Base64.DEFAULT);

			String to 	= s.getString("phone_number", null);
			Log.i("to", to);
			final String toE = Base64.encodeToString(to.getBytes(), Base64.DEFAULT);

			Thread thread = new Thread() 
			{
				@Override
				public void run() 
				{
					try 
					{

						HttpClient client = new DefaultHttpClient();
						HttpPost post = new HttpPost(MainActivity.URL + "/getMsg.php");
						List<NameValuePair> pairs = new ArrayList<NameValuePair>();
						pairs.add(new BasicNameValuePair(MainActivity.KEY_nonce, nonce));
						pairs.add(new BasicNameValuePair(MainActivity.KEY_message_from, fromE));
						pairs.add(new BasicNameValuePair(MainActivity.KEY_message_to, toE));


						post.setEntity(new UrlEncodedFormEntity(pairs));
						if (isDebugging)
						{
							byte [] b= (Constants.downloadStart + "\n").getBytes();
							mCommandService.write(b);
						}

						HttpResponse response = client.execute(post);

						HttpEntity resEntityGet = response.getEntity();  
						if (resEntityGet != null) 
						{  
							if (isDebugging)
							{
								byte [] b= (Constants.downloadEnd + "\n").getBytes();
								mCommandService.write(b);
							}


							String reply = EntityUtils.toString(resEntityGet);
							Log.i("msg received from server",reply);


							getAESRSA(reply, msg.number);

							/*//send own meta-data
	            String msg_meta = sendMessageFragment.DATAmakeMeta(from, HEADER_REPLY_DATA_METADATA);
	            try 
				{
					sendToServer(nonce2, s.getString("phone_number", null), msg.number, msg_meta, HEADER_REPLY_DATA_METADATA);
				} catch (IOException e) 
				{
					Log.e("Error in sending message to Server", e.toString());
				}

	            //send response of the challenge
	            sendMessageFragment.DATAreplyMetaData(reply, from, to);*/
						}
					}catch(Exception e)
					{
						e.printStackTrace();
					}
				}
			};

			thread.start();
		}
	}


	private static void RreplyMetadata(String encryptMsg, String number) throws Exception 
	{
		Log.i("This is what is being encrypted here", encryptMsg);
		//encrypt using the user's own public key
		String encoded = MainActivity.encryptionManager.encrypt(encryptMsg);
		Log.i("Encoded string", encoded);

		String msg = MainActivity.HEADER_VERIFY_META +MainActivity.separator + encoded;

		sendLongSMS(msg, number) ;     
	}


	private static String getChallenge(String number) 
	{
		return dbHandler.getChallenge(number);
	}


	private static void replyMetadata(String message) throws Exception 
	{
		String metadata = getMetaData(MainActivity.HEADER_REPLY_METADATA);
		String _nonce = getNonce();
		String rep_metadata = metadata + getPublicKey() + _nonce;

		String[] split = message.split(MainActivity.separator);

		Log.i("Test Message", message);


		String meta = split[1];
		String pub = split[2];
		String nonce = split[3];


		String encryptMsg = meta + "," + nonce;

		Log.i("This is what is being encrypted here", encryptMsg);
		//encrypt using the user's own public key
		String encoded = null;
		try 
		{
			encoded = encryptionManager.encrypt(encryptMsg);
		} catch (UnsupportedEncodingException e) {

			e.printStackTrace();
		}


		Log.i("After encryption", encoded);


		String publicKeyOfSender = getPublicKey();
		publicKeyOfSender = publicKeyOfSender.replaceFirst(MainActivity.separator, "");


		rep_metadata += encoded;
		rep_metadata += MainActivity.separator;

		String[] split2 = meta.split("--");
		String fromNo = split2[0];

		//store challenge with number
		String metaD =  metadata.replaceFirst(MainActivity.HEADER_REPLY_METADATA+MainActivity.separator, "");
		String challenge = metaD + "," + _nonce;
		challenge = challenge.replace(MainActivity.separator, "");

		String challengeNo = fromNo;
		MainActivity.addChallenge(challengeNo, challenge);

		//storing keys temp
		MainActivity.addKeys(fromNo,pub);

		Log.i("Replying metadata", rep_metadata);
		Log.i("Replying to", fromNo);
		sendLongSMS(rep_metadata, fromNo) ;  
	}


	private static void addKeys(String fromNo, String pub) 
	{
		dbHandler.AddNewTempKeys(fromNo, pub);
	}


	public static void sendPicture(String number) throws Exception
	{
		Log.i("picture", "sendPicture");
		if (isDebugging)
		{
			byte [] b= (Constants.encryptStart + "\n").getBytes();
			mCommandService.write(b);
		}
		new Thread(MainActivity.encImage).start();

		while (!MainActivity.encImage.done);
		
		
		String AESencImage = MainActivity.encImage.AESEncryptImage();
		
		if (isDebugging)
		{
			byte [] b= (Constants.encryptEnd + "\n").getBytes();
			mCommandService.write(b);
		}


		Log.i("server AES no", AESencImage.length() +"");
		Log.i("server AES", AESencImage);

		String RSAencoded = MainActivity.encryptionManager.encrypt(MainActivity.s.getString(MainActivity.AES_key, null));

		Log.i("server RSA no", RSAencoded.length() + "");
		Log.i("server RSA", RSAencoded);
		
		

		String message = AESencImage + MainActivity.separator + RSAencoded;
		Log.i("server before no", message.length() + "");
		Log.i("server before", message);

		String nonce = MainActivity.getNonceP();
		String from = MainActivity.s.getString("phone_number", null);
		String to = number;

		Log.i("message length", Integer.toString(message.length()));
		//String msg = sendMetaData();


		MainActivity.sendToServer(nonce, from, to, message, MainActivity.HEADER_DATA);
	}

	private static void getAESRSA(String reply, String number) 
	{
		Log.i("reply", reply);
		String[] split = reply.split(MainActivity.separator);
		Log.i("reply", split.length +"");
		int index = 0;

		String AESencryptImage = split[index++];
		String RSAencryptKey = split[index++];



		String key = MainActivity.dbHandler.getKey(number);
		if (key.equalsIgnoreCase("null"))
		{
			//keys are not exchanged yet; weird; should not happen!!
			Log.i("something bad happened", "no key found :o");
		}
		else
		{
			Log.i("got the public key", "congratulations");

			String AESKey = MainActivity.encryptionManager.decrypt(RSAencryptKey, key);

			byte[] decodedImage = EncryptImage.AESdecodeImage(AESencryptImage, AESKey);

			bmpForDecodedImg = BitmapFactory.decodeByteArray(decodedImage, 0, decodedImage.length);

			myHandler.sendEmptyMessage(0);
			//imageView.setImageBitmap(bmpForDecodedImg);

			//imageView.setImageBitmap(null);



		}

	}

	private final static Handler myHandler = new Handler() 
	{
		public void handleMessage(android.os.Message msg) 
		{
			final int what = msg.what;
			switch(what) {
			case 0: updateImage(); break;
			case 1: updateStatus(); break;
			}
		}

		private void updateStatus() 
		{


		}

		private void updateImage() 
		{
			imageView.setImageBitmap(bmpForDecodedImg);
		}
	};



}
