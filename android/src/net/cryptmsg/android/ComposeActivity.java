package net.cryptmsg.android;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.Html;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;

public class ComposeActivity extends Activity implements OnEditorActionListener, OnClickListener {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_compose);
		
		findViewById(R.id.replyButton).setOnClickListener(this);
		((EditText)findViewById(R.id.replyText)).setOnEditorActionListener(this);
		
		String user = getIntent().getStringExtra(MainActivity.EXTRA_USER);
		if(user != null)
			((TextView)findViewById(R.id.threadTextView)).setText(Html.fromHtml(getString(R.string.compose)+" " +user));
		else
			((TextView)findViewById(R.id.threadTextView)).setText(Html.fromHtml(getString(R.string.compose)+" <b><font color=\"#C00000\">"+getString(R.string.selSecret)+"</font></b>"));
	}

	@Override
	public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
		Log.i("Event","Action " + actionId);
        if (actionId == EditorInfo.IME_ACTION_DONE) {
        	Log.i("Event","Done hit");
            String msg = ((EditText)findViewById(R.id.replyText)).getText().toString();
    		Intent i = new Intent();
    		i.putExtra(MainActivity.EXTRA_MSG_TEXT, msg);
    		setResult(RESULT_OK, i);
    		finish();
            return true;	
        }
        return false;
    }
	
	@Override
    public void onClick(View v) {
            String msg = ((EditText)findViewById(R.id.replyText)).getText().toString();
            Intent i = new Intent();
            i.putExtra(MainActivity.EXTRA_MSG_TEXT, msg);
            setResult(RESULT_OK, i);
            finish();
    }
}
