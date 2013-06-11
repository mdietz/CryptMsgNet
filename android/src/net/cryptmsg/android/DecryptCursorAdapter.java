package net.cryptmsg.android;

import java.util.HashMap;
import java.util.Map.Entry;
import java.util.Vector;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

public class DecryptCursorAdapter extends  BaseAdapter {
	HashMap<String, Vector<String> > hashmap;
	
	public DecryptCursorAdapter(HashMap<String, Vector<String> > map) {
		super();
		hashmap = map;
	}

	@Override
	public int getCount() {
		return hashmap.size();
	}

	@Override
	public Entry<String, Vector<String> > getItem(int position) {
		return (Entry<String, Vector<String>>) hashmap.entrySet().toArray()[position];
	}

	@Override
	public long getItemId(int position) {
		return position;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		Context ctx = parent.getContext();
		LayoutInflater inflater = (LayoutInflater)ctx.getSystemService
			      (Context.LAYOUT_INFLATER_SERVICE);
		View v = inflater.inflate(R.layout.top_list_entry, null);
		
		String user = getItem(position).getKey();
		String message = getItem(position).getValue().lastElement();
		
		Log.i("ENTRY", user + " : " + message);
		if(user != null)
			((TextView)v.findViewById(R.id.userText)).setText(user);
		else
			((TextView)v.findViewById(R.id.userText)).setText(ctx.getString(R.string.selSecret));
		((TextView)v.findViewById(R.id.msgText)).setText(message);
		return v;
	}

}
