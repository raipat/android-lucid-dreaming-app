package com.luciddreamingapp.beta.actigraph.parcels;


import java.util.Calendar;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import com.luciddreamingapp.beta.GlobalApp;
import com.luciddreamingapp.beta.util.SleepDataPoint;

public class ActigraphReceiver extends BroadcastReceiver  {

	
	private static final String TAG = "Actigraph Receiver";
	private static final boolean D = true;
	
	//Static string constants, which I hope are more efficient than re-creating strings each time
	private static final String s_epoch = "epoch";
	private static final String s_epochLength = "epochLength";
	private static final String s_accelerometerEvents = "accelerometerEvents";
	private static final String s_xActivityCount = "xActivityCount";
	private static final String s_yActivityCount = "yActivityCount";
	private static final String s_zActivityCount = "zActivityCount";
	
	
	private final GlobalApp parent;
	
	public ActigraphReceiver (GlobalApp parent){
		super();
		
	this.parent = parent;
	}
	
	
	@Override
	public void onReceive(Context context, Intent intent) {
		// TODO Auto-generated method stub
		
		
		Log.w(TAG,"Received intent");
//		
//		ActigraphParcel parcel = intent.getParcelableExtra("epochData");
//		Log.w("Actigraph Receiver", parcel.toString());
		
		
		if(intent.getExtras()!=null){
			
			Bundle b =	intent.getBundleExtra("epochData");
			SleepDataPoint dataPoint = new SleepDataPoint();
			
			//get data from the bundle
			dataPoint.setCalendar(Calendar.getInstance());
			dataPoint.setSleepEpoch(b.getInt(s_epoch));
			dataPoint.setEventCount(b.getInt(s_accelerometerEvents));
			dataPoint.setActivityCount(b.getInt(s_xActivityCount)
					+b.getInt(s_yActivityCount)
					+b.getInt(s_zActivityCount));
			
			parent.processData(dataPoint);
		
		if(D)Log.v(TAG,"sleep epoch: "+dataPoint.getSleepEpoch());
		if(D)Log.v(TAG,"event count: "+dataPoint.getEventCount());
		if(D)Log.v(TAG,"activity count: "+dataPoint.getActivityCount());
		
		}
	
	}
	
	
	   /*
	    * Methods for parcel creator interface
	    */
	 
	
	
}
