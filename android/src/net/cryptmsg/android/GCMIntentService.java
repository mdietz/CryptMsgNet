package net.cryptmsg.android;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;

import com.google.android.gcm.GCMBaseIntentService;

public class GCMIntentService extends GCMBaseIntentService {
	
	private void createNotification(Context c, String keyId, String guid){
		Uri defaultNotificationUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
		
		NotificationCompat.Builder mBuilder =
		        new NotificationCompat.Builder(this)
		        .setSmallIcon(R.drawable.ic_launcher)
		        .setContentTitle("New Message!")
		        .setTicker("New Message!")
		        .setSound(defaultNotificationUri)
		        .setContentText(keyId)
		        .setAutoCancel(true);
		// Creates an explicit intent for an Activity in your app
		Intent resultIntent = new Intent(this, MainActivity.class);
		Uri read_uri = Uri.parse(MainActivity.baseUrl + "api/read/" + keyId + "/" + guid);
		resultIntent.setData(read_uri);

		// The stack builder object will contain an artificial back stack for the
		// started Activity.
		// This ensures that navigating backward from the Activity leads out of
		// your application to the Home screen.
		TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
		// Adds the back stack for the Intent (but not the Intent itself)
		stackBuilder.addParentStack(MainActivity.class);
		// Adds the Intent that starts the Activity to the top of the stack
		stackBuilder.addNextIntent(resultIntent);
		PendingIntent resultPendingIntent =
		        stackBuilder.getPendingIntent(
		            0,
		            PendingIntent.FLAG_UPDATE_CURRENT
		        );
		mBuilder.setContentIntent(resultPendingIntent);
		NotificationManager mNotificationManager =
		    (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		// mId allows you to update the notification later on.
		mNotificationManager.notify(0, mBuilder.build());
		
	}
	
	public GCMIntentService(){
		super("186445326083");
	}

	@Override
	protected void onError(Context c, String errorId) {
		System.err.println(errorId);	
	}

	@Override
	protected void onMessage(Context c, Intent intent) {
		System.out.println(intent.getExtras().toString());
		String keyid = intent.getStringExtra("keyid");
		String guid = intent.getStringExtra("guid");
		if(MainActivity.isActivityVisible())
		{
			MainActivity.decFromURL(MainActivity.baseUrl + "api/read/" + keyid + "/" + guid);
		}
		else
			createNotification(c, keyid, guid);
	}

	@Override
	protected void onRegistered(Context c, String regId) {
		String privKeyId = MainActivity.LongToHex(MainActivity.keyData.getSecretKeyId());
		String full_uri = MainActivity.baseUrl + "api/register/" + privKeyId + "/" + regId;
		new PostRequestTask().execute(full_uri, "");
	}

	@Override
	protected void onUnregistered(Context c, String arg1) {
		// TODO Auto-generated method stub
		
	}

}
