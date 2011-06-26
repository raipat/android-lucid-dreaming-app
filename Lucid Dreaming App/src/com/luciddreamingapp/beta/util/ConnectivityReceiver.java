package com.luciddreamingapp.beta.util;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.preference.PreferenceManager;

public class ConnectivityReceiver extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent intent) {
		// TODO Auto-generated method stub
		
		SharedPreferences prefs = PreferenceManager
        .getDefaultSharedPreferences(context);
		Editor editor = prefs.edit();
		editor.putBoolean("connectionAvailable", true);
		editor.commit();
	}

	
	
}
