package net.cryptmsg.android;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;

import org.sufficientlysecure.keychain.integration.KeychainContentProviderHelper;
import org.sufficientlysecure.keychain.integration.KeychainData;
import org.sufficientlysecure.keychain.integration.KeychainIntentHelper;

import android.net.Uri;
import android.nfc.NfcAdapter;
import android.nfc.NfcManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.app.Activity;
import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.database.Cursor;
import android.text.Html;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gcm.GCMRegistrar;

class GetRequestTask extends AsyncTask<String, String, String>{

    @Override
    protected String doInBackground(String... params) {
        HttpClient httpclient = new DefaultHttpClient();
        HttpResponse response;
        String responseString = null;
        try {
            response = httpclient.execute(new HttpGet(params[0]));
            StatusLine statusLine = response.getStatusLine();
            if(statusLine.getStatusCode() == HttpStatus.SC_OK){
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                response.getEntity().writeTo(out);
                out.close();
                responseString = out.toString();
            } else{
                //Closes the connection.
                response.getEntity().getContent().close();
                throw new IOException(statusLine.getReasonPhrase());
            }
        } catch (ClientProtocolException e) {
            //TODO Handle problems..
        } catch (IOException e) {
            //TODO Handle problems..
        }
        return responseString;
    }

    @Override
    protected void onPostExecute(String result) {
        super.onPostExecute(result);
        //Do anything with response..
        Log.i("RESPONSE", result);
        MainActivity.doDecrypt(result);
    }
}

class PostRequestTask extends AsyncTask<String, String, String>{

    @Override
    protected String doInBackground(String... params) {
        HttpClient httpclient = new DefaultHttpClient();
        HttpResponse response;
        String responseString = null;
        try {
            String uri = params[0];
            String encMsgData = params[1];
            HttpPost httpPost = new HttpPost(uri);
            List<BasicNameValuePair> data = new ArrayList<BasicNameValuePair>();
            data.add(new BasicNameValuePair("MessageData", encMsgData));
            UrlEncodedFormEntity entity = new UrlEncodedFormEntity(data, "UTF-8");
            httpPost.setEntity(entity);
            response = httpclient.execute(httpPost);
            StatusLine statusLine = response.getStatusLine();
            if(statusLine.getStatusCode() == HttpStatus.SC_OK){
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                response.getEntity().writeTo(out);
                out.close();
                responseString = out.toString();
            } else{
                //Closes the connection.
                response.getEntity().getContent().close();
                throw new IOException(statusLine.getReasonPhrase());
            }
        } catch (ClientProtocolException e) {
            //TODO Handle problems..
        } catch (IOException e) {
            //TODO Handle problems..
        }
        return responseString;
    }

    @Override
    protected void onPostExecute(String result) {
        super.onPostExecute(result);
        //Do anything with response..
    }
}

public class MainActivity extends ListActivity implements OnClickListener, OnItemClickListener {
	
	static KeychainIntentHelper pgpIntentHelper;
	public static KeychainData keyData;
	DecryptCursorAdapter decAdapter;
	public static final String baseUrl = "http://cryptmsgnet.appspot.com/";
	
	static MessageStore msgStore;
	
	public static final String EXTRA_MSG_TEXT = "net.cryptmsg.android.message_text_extra";
	public static final String EXTRA_USER = "net.cryptmsg.android.user_extra";
	public static final String PREFS_FILE = "net.cryptmsg.android.preferences";
	public static final String KEY_IDENTITY = "net.cryptmsg.android.IDENTITY";
	public static final int COMPOSE_MSG_CODE = 52498;
	
	public static String LongToHex(Long in){
		String out = Long.toHexString(in);
		while(out.length() < 16){
			out = "0" + out;
		}
		return out;
	}
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		findViewById(R.id.recvMsgButton).setOnClickListener(this);
		findViewById(R.id.sendMsgButton).setOnClickListener(this);
		findViewById(R.id.sendAnonButton).setOnClickListener(this);
		findViewById(R.id.selSecretButton).setOnClickListener(this);
		findViewById(R.id.shareQrButton).setOnClickListener(this);
		
		NfcManager manager = (NfcManager) getSystemService(NFC_SERVICE);
		NfcAdapter adapter = manager.getDefaultAdapter();
		if (adapter != null && adapter.isEnabled()) {
			findViewById(R.id.shareNfcButton).setVisibility(View.VISIBLE);
			findViewById(R.id.shareNfcButton).setOnClickListener(this);
		}
		
		String dbpath = getDatabasePath(MessageStore.DB_NAME).toString();
		msgStore = new MessageStore(dbpath, this);
		pgpIntentHelper = new KeychainIntentHelper(this);
		keyData = new KeychainData();
		loadUserFromPrefs();
		
		decAdapter = new DecryptCursorAdapter(msgStore.getHashMap());
		this.setListAdapter(decAdapter);
		
		if(msgStore.getHashMap().isEmpty())
			loadFromDatabase();
		
		Intent i = getIntent();
		if(i.getData() != null && i.getData().getScheme().equals("http")){
			Log.i("REQ", getIntent().getDataString());
			decFromURL(getIntent().getDataString());
		}
		
		getListView().setOnItemClickListener(this);
	}
	
	

	private void loadUserFromPrefs() {
		SharedPreferences prefs = getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE);
		String user = prefs.getString(KEY_IDENTITY, null);
		setUser(user);
	}

	private void loadFromDatabase() {
		Cursor fingerCursor = msgStore.getFingerprintCursor();
		fingerCursor.moveToFirst();
		while(!fingerCursor.isAfterLast())
		{
			String finger = fingerCursor.getString(fingerCursor.getColumnIndex(msgStore.KEY_FINGERPRINT));
			if(finger == null)
			{
				fingerCursor.moveToNext();
				continue;
			}
			Cursor msgCursor = msgStore.getMsgCursorByFingerprint(finger);
			if(msgCursor.moveToFirst())
				doDecrypt(msgCursor.getString(msgCursor.getColumnIndex(msgStore.KEY_MSG)));
			fingerCursor.moveToNext();
		}
	}

	public static void decFromURL(String dataString) {
		GetRequestTask task = new GetRequestTask();
		task.execute(dataString);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.activity_main, menu);
		return true;
	}


	static String testData = "Hello World";

	@Override
	public void onClick(View v) {
		switch(v.getId())
		{
		case R.id.selSecretButton:
			pgpIntentHelper.selectSecretKey();
			break;
		case R.id.recvMsgButton:
			doDecrypt(keyData.getEncryptedData());
			loadUserFromPrefs();
			break;
		case R.id.sendMsgButton:
			pgpIntentHelper.selectPublicKeys(null, keyData);
			break;
		case R.id.sendAnonButton:
			setUser(null);
			pgpIntentHelper.selectPublicKeys(null, keyData);
			break;
		case R.id.shareNfcButton:
			if(keyData.getSecretKeyUserId() != null)
				pgpIntentHelper.shareWithNfc(keyData.getSecretKeyId());
			else
				Toast.makeText(this, "Cannot share anonymous...", Toast.LENGTH_SHORT).show();
			break;
		case R.id.shareQrButton:
			if(keyData.getSecretKeyUserId() != null)
				pgpIntentHelper.shareWithQrCode(keyData.getSecretKeyId());
			else
				Toast.makeText(this, "Cannot share anonymous...", Toast.LENGTH_SHORT).show();
			break;
		}
	}
	
	public static void doDecrypt(String data) {
		pgpIntentHelper.decrypt(data, true);
	}

	private void setUser(String user) {
		keyData.setSecretKeyUserId(user);
		SharedPreferences prefs = getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE);
		Editor prefsEdit = prefs.edit();
		prefsEdit.putString(KEY_IDENTITY, user);
		prefsEdit.commit();
		if(user != null)
		{
			((Button)findViewById(R.id.selSecretButton)).setText(Html.fromHtml("<i>"+user+"</i>"));
			
		}
		else
		{
			keyData.setSecretKeyId(0);
			((Button)findViewById(R.id.selSecretButton)).setText(Html.fromHtml("<b><font color=\"#C00000\">"+getString(R.string.selSecret)+"</font></b>"));
		}
	}



	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data){
		boolean result = pgpIntentHelper.onActivityResult(requestCode, resultCode, data,
                keyData);
		switch(requestCode)
		{
		case KeychainIntentHelper.DECRYPT_MESSAGE:
			//Log.i("DATA", keyData.getDecryptedData());
			if(resultCode == Activity.RESULT_OK)
			{
				msgStore.insert(keyData.getSecretKeyUserId(), keyData.getEncryptedData(), keyData.getDecryptedData());
				Cursor c = msgStore.getMsgCursor();
				Log.i("Cursor", "has " + new Integer(c.getCount()).toString() + "entries");
				decAdapter.notifyDataSetChanged();

			  loadUserFromPrefs();
			}
			break;
		case KeychainIntentHelper.ENCRYPT_MESSAGE:
			for(long elem : keyData.getPublicKeys()){
				String targetKeyId = LongToHex(elem);
				String randomUUID = UUID.randomUUID().toString().replace("-", "");
				Log.i("DATA", targetKeyId);
				Log.i("DATA", randomUUID);
				Log.i("DATA", keyData.getEncryptedData());
				String full_uri = "";
				String send_uri = "";
				if(keyData.getSecretKeyUserId() != null){
					full_uri = baseUrl + "api/write/" + targetKeyId + "/" + randomUUID;
					send_uri = baseUrl + targetKeyId + "/" + randomUUID;
				} else {
					full_uri = baseUrl + "api/write/" + randomUUID;
					send_uri = baseUrl + randomUUID;
				}
				Log.i("DATA", full_uri);
				new PostRequestTask().execute(full_uri, keyData.getEncryptedData());
				if(keyData.getSecretKeyUserId() == null){
					Intent sendIntent = new Intent(Intent.ACTION_SEND);         
					sendIntent.putExtra(Intent.EXTRA_TEXT, send_uri);
					sendIntent.setType("text/plain"); 
					startActivity(Intent.createChooser(sendIntent, "Choose how to send"));
				}
			}
			break;
		case KeychainIntentHelper.SELECT_PUBLIC_KEYRINGS:
			if(resultCode == RESULT_OK && keyData != null)
			{
				launchComposer();
			}
			break;
		case COMPOSE_MSG_CODE:
			if(resultCode == RESULT_OK && keyData != null)
			{
				long[] pubkeys = keyData.getPublicKeys();
				long secretkey = keyData.getSecretKeyId();
				pgpIntentHelper.encrypt(data.getStringExtra(EXTRA_MSG_TEXT), pubkeys, secretkey, true);
			}
			break;
		case KeychainIntentHelper.SELECT_SECRET_KEYRING:
			String identity = keyData.getSecretKeyUserId();
			setUser(identity);
			
			// Do GCM Registration Dance
			GCMRegistrar.checkDevice(this);
			GCMRegistrar.checkManifest(this);
			final String regId = GCMRegistrar.getRegistrationId(this);
			if (regId.equals("")) {
			  GCMRegistrar.register(this, "186445326083");
			} else {
			  Log.v("GCM", "Already registered");
			  if(keyData.getSecretKeyUserId() != null){
				  String privKeyId = LongToHex(keyData.getSecretKeyId());
				  String reg_uri = baseUrl + "api/register/" + privKeyId + "/" + regId;
				  new PostRequestTask().execute(reg_uri, "");
			  }
			}
			break;
		}
	}



	private void launchComposer() {
		Intent i = new Intent(this, ComposeActivity.class);
		i.putExtra(EXTRA_USER, keyData.getSecretKeyUserId());
		startActivityForResult(i, COMPOSE_MSG_CODE);
	}



	public static void updateList() {
		// TODO Auto-generated method stub
		
	}

	@Override
	protected void onResume() {
	  super.onResume();
	  MainActivity.activityResumed();
	}

	@Override
	protected void onPause() {
	  super.onPause();
	  
	  MainActivity.activityPaused();
	}
	  public static boolean isActivityVisible() {
		    return activityVisible;
		  }  

		  public static void activityResumed() {
		    activityVisible = true;
		  }

		  public static void activityPaused() {
		    activityVisible = false;
		  }

		  private static boolean activityVisible;

		@Override
		public void onItemClick(AdapterView<?> parent, View v, int pos, long id) {
			keyData.setPublicKeyIds(null);
			String user =  (String)((TextView)v.findViewById(R.id.userText)).getText();
			String email = user.replaceAll("(.*?<)(.+?)(>)", "$2");
			if(email != null && email.length() > 0)
			{
				Log.i("REPLY", email);
				KeychainContentProviderHelper contentProvider = new KeychainContentProviderHelper(this);
				keyData.setPublicKeyIds(contentProvider.getPublicKeyringIdsByEmail(email));
				launchComposer();
			}
		}
}

