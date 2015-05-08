package com.example.securepicturetransfer;

import java.io.ByteArrayOutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;
import android.widget.ImageView;

public class EncryptImage implements Runnable 
{

	Bitmap bitmap;
	//ImageView imageView;
	MessageDigest md;
	StringBuffer SHAhexString;
	
	public boolean done = false;
	byte[] image;
	
	
	public EncryptImage(Bitmap bitmap) throws NoSuchAlgorithmException 
	{
		
	    this.bitmap = bitmap;
	    //this.imageView = imageView;
	    
	    md = MessageDigest.getInstance("SHA-256");
	    
	}
	
	

	@Override
	public void run() {
		
		
		ByteArrayOutputStream stream=new ByteArrayOutputStream();
		bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream);
	    
		image=stream.toByteArray();
		
		Log.i("picture", "image is in byte array");
	    
		done = true;
	    HashUsingSha1(image);
	    
	}
	
	public String AESEncryptImage()
	{
		Log.i("picture", "AES encrypt image");
		String key = MainActivity.encryptionManager.s.getString(MainActivity.AES_key, null);
		byte[] keyBytes = org.bouncycastle.util.encoders.Base64.decode(key);
		SecretKeySpec AESKey = new SecretKeySpec(keyBytes, "AES");
		
		String encryptedText = "";
		// Encode the original data with AES
		byte[] encodedBytes = null;
		try 
		{
			Cipher c = Cipher.getInstance("AES");
			c.init(Cipher.ENCRYPT_MODE, AESKey);
			encodedBytes = c.doFinal(image);
			
			Log.i("picture", "encoded bytes length is: "+encodedBytes.length);
			
			encryptedText = new String(org.bouncycastle.util.encoders.Base64.encode(encodedBytes));
		    
		    Log.i("AES encryptedText", encryptedText);
		    
		} catch (Exception e) 
		{
			Log.e("AES encryption error", "AES encryption error");
		}		
		
		return encryptedText;
	}
	
	public static byte[] AESdecodeImage(String imageEnc, String key)
	{
		// Decode the encoded data with AES
		byte[] encodedBytes = org.bouncycastle.util.encoders.Base64.decode(imageEnc);
		
		
		//String key = MainActivity.encryptionManager.s.getString(MainActivity.AES_key, null);
		byte[] keyBytes = org.bouncycastle.util.encoders.Base64.decode(key);
		SecretKeySpec AESKey = new SecretKeySpec(keyBytes, "AES");
		
		
		byte[] decodedBytes = null;
		
		try {
				Cipher c = Cipher.getInstance("AES");
				c.init(Cipher.DECRYPT_MODE, AESKey);
				decodedBytes = c.doFinal(encodedBytes);
			} catch (Exception e) 
			{
				Log.e("AES decryption error", "AES decryption error");
			}		
			
		return decodedBytes;
	}

	private void HashUsingSha1(byte[] image) 
	{
		
	    md.update(image);
	    
	    byte[] SHAbyteData = md.digest();
	    
	    Log.i("picture", "hash using SHA");
	    
	    SHAhexString = new StringBuffer();
    	for (int i=0;i<SHAbyteData.length;i++) 
    	{
    		String hex=Integer.toHexString(0xff & SHAbyteData[i]);
   	     	
    		if(hex.length()==1) 
   	     		SHAhexString.append('0');
   	     
   	     	SHAhexString.append(hex);
    	}

   	     	
   	     	
   	     	
		
	}

}
