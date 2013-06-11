package net.cryptmsg.android;

import java.util.HashMap;
import java.util.Vector;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.AsyncTask;

public class MessageStore {
	
	protected class myOpenHelper extends SQLiteOpenHelper
	{

		public myOpenHelper(Context context, String name,
				CursorFactory factory, int version) {
			super(context, name, factory, version);
			
		}

		@Override
		public void onCreate(SQLiteDatabase db) {
			db.execSQL(CREATE_USER_TABLE);
			db.execSQL(CREATE_MSG_TABLE);
		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			// Cannot upgrade, no new versions exist
			assert(false);
		}
		
	}
	
	public static final String USER_TABLE = "Users";
	public static final String KEY_USERID = "userid";
	public static final String KEY_METADATA = "data";
	public static final String KEY_FINGERPRINT = "finger";
	
	public static final String MSG_TABLE = "Messages";
	public static final String KEY_ROWID = "_id";
	public static final String KEY_MSG = "msg";
	
	private static final String CREATE_MSG_TABLE =
			  "CREATE TABLE if not exists " + MSG_TABLE + " (" +
			  KEY_ROWID + " integer PRIMARY KEY autoincrement," +
			  KEY_FINGERPRINT + " text, " +
			  KEY_MSG + " text, " +
			  " FOREIGN KEY ("+KEY_FINGERPRINT+") REFERENCES userids("+KEY_FINGERPRINT+") )";
	
	private static final String CREATE_USER_TABLE =
			  "CREATE TABLE if not exists " + USER_TABLE + " (" +
			  KEY_ROWID + " integer PRIMARY KEY autoincrement," +
			  KEY_FINGERPRINT + " text UNIQUE, "+
			  KEY_METADATA + " text )";
	
	public static final String DB_NAME = "MessageStore.sqlite";
	
	static SQLiteDatabase db;
	
	public static HashMap<String, Vector<String> > decryptedMessages;
	
	MessageStore(String fname, Context ctx)
	{
		assert(fname != null);
		myOpenHelper openhelper = new myOpenHelper(ctx, DB_NAME, null, 1);
		db = openhelper.getWritableDatabase();
		assert(db != null);
		
		decryptedMessages = new HashMap<String, Vector<String> >();
	}

	protected class DbInsert extends AsyncTask<Message, Void, Void>
	{
		@Override
		protected Void doInBackground(Message... params) {
			// TODO Auto-generated method stub
			return null;
		}
		
		@Override
		protected void onPostExecute(Void result)
		{
			MainActivity.updateList();
		}

	}

	public Cursor getMsgCursor() {
		return db.query(false, MSG_TABLE, new String[]{KEY_ROWID, KEY_FINGERPRINT,KEY_MSG}, null, null, null, null, null, null);
	}
	
	public Cursor getFingerprintCursor(){
		return db.query(true, MSG_TABLE, new String[]{KEY_FINGERPRINT}, null, null, null, null, null, null);
	}
	
	public Cursor getMsgCursorByFingerprint(String fingerprint) {
		return db.query(false, MSG_TABLE, new String[]{KEY_ROWID, KEY_FINGERPRINT,KEY_MSG}, KEY_FINGERPRINT+"=?", new String[]{fingerprint}, KEY_FINGERPRINT, null, "_id", null);
	}

	public void insert(String secretKeyUserId, String encryptedData, String decryptedData) {
		putDecrypted(secretKeyUserId, decryptedData);

		
		ContentValues cv = new ContentValues();
		cv.put(KEY_FINGERPRINT, secretKeyUserId);
		cv.put(KEY_METADATA, "");
		db.insert(USER_TABLE, null, cv);
		
		cv = new ContentValues();
		cv.put(KEY_FINGERPRINT, secretKeyUserId);
		cv.put(KEY_MSG, encryptedData);
		db.insert(MSG_TABLE, null, cv);
	}

	public void putDecrypted(String secretKeyUserId, String decryptedData) {
		if(!decryptedMessages.containsKey(secretKeyUserId))
			decryptedMessages.put(secretKeyUserId, new Vector<String>());
		decryptedMessages.get(secretKeyUserId).add(decryptedData);
	}

	public HashMap<String, Vector<String>> getHashMap() {
		return decryptedMessages;
	}
}
