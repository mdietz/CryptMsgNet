package net.cryptmsg.android;

import org.sufficientlysecure.keychain.integration.KeychainData;

import android.app.Activity;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;

public class ThreadedActivity extends Activity implements OnClickListener, OnEditorActionListener{

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_threaded);
		
		findViewById(R.id.replyButton).setOnClickListener(this);
		((EditText)findViewById(R.id.replyText)).setOnEditorActionListener(this);
		
		KeychainData kcd = (KeychainData) getIntent().getSerializableExtra(MainActivity.EXTRA_KEYDATA);
	}

	@Override
	public void onClick(View v) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
		// TODO Auto-generated method stub
		return false;
	}
	
}
