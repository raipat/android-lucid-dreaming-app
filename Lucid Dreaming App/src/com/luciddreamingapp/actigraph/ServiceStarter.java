package com.luciddreamingapp.actigraph;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class ServiceStarter extends BroadcastReceiver {

	//debug
	public static final String TAG = "Service Starter";
	public static final boolean D = false;
	
	@Override
	public void onReceive(Context context, Intent intent) {
		// TODO Auto-generated method stub
		
		if(D)Log.v(TAG,"Start service intent received"	 );
		
		if(!ActigraphyService.running){
			
			Intent  serviceIntent = new Intent(context, ActigraphyService.class);
			
			if(intent.getExtras()!=null){
				try{
					if(intent.getExtras().getBoolean("stopService")){
					context.stopService(serviceIntent)	;
					}
					serviceIntent.putExtra("stopService", intent.getExtras().getBoolean("stopService"));
				}catch(Exception e){}
			}
			
			context.startService(serviceIntent);

			if(D)Log.v(TAG,"Starting service" );
		}
		
		
	}

	
	
	
}
