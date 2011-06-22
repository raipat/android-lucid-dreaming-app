package com.luciddreamingapp.actigraph;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Timer;
import java.util.TimerTask;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.preference.PreferenceManager;
import android.util.Log;

import com.luciddreamingapp.actigraph.filter.KalmanFilter;
import com.luciddreamingapp.actigraph.filter.SimpleFilter;


public class ActigraphyService extends Service implements SensorEventListener, OnSharedPreferenceChangeListener{



	private static final String TAG = "Actigraphy Service Standalong";
	private static final boolean D = false;//debug
	public static final int ONGOING_NOTIFICATION = 37;
	
	public static final int MAX_CONTRIBUTION = 20;
	
	//Static string constants, which I hope are more efficient than re-creating strings each time
	private static final String s_epoch = "epoch";
	private static final String s_epochLength = "epochLength";
	private static final String s_accelerometerEvents = "accelerometerEvents";
	private static final String s_xActivityCount = "xActivityCount";
	private static final String s_yActivityCount = "yActivityCount";
	private static final String s_zActivityCount = "zActivityCount";
	
	
	
	public final static String START_SERVICE_INTENT = "com.luciddreamingapp.actigraph.START_SERVICE";
	
	
	public static boolean running = false;
	
	
	private int epochLength = 60;//seconds
	private int epochLengthMs =epochLength*1000;
	
	
	private String accelerometerAccuracy = "";
	
	private boolean calibrationCompleted = false;
	private boolean readyToProcess = false;
	private boolean timerStarted = false;
	
	private int accelerometerEventCounter = 1;
	private int epochCounter =1;
	
	private float x,y,z;
	private double xPrev,yPrev,zPrev;
	
	private double diff;
	
	private int xActivityCount,yActivityCount,zActivityCount;
	
	private float xSensitivity=  0.0004F;	
	private float ySensitivity = 0.00006F;
	private float zSensitivity =0.0001F;
	private float xStep,yStep,zStep;
	
	private SimpleFilter xFilter;
	private SimpleFilter yFilter;
	private SimpleFilter zFilter;
	
	private double xNoiseVarianceR,yNoiseVarianceR,zNoiseVarianceR;
	private double xFiltered,yFiltered,zFiltered;
	
	
	private SensorManager mSensorManager;
    private Sensor mAccelerometer;
	private Timer timer;
	private WakeLock wakeLock;
	
	private boolean stopFlag = false;
		
	
	public static String NEW_EPOCH = "com.luciddreamingapp.actigraph.NEW_EPOCH";
	
	
	  
	
    
	@Override
	public IBinder onBind(Intent arg0) {
		if(D)Log.d(TAG, "onBind");
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void onCreate() {
		// TODO Auto-generated method stub
		super.onCreate();
		if(D)Log.d(TAG, "onCreate");
		
		
        mNM = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
        try {
            mStartForeground = getClass().getMethod("startForeground",
                    mStartForegroundSignature);
            mStopForeground = getClass().getMethod("stopForeground",
                    mStopForegroundSignature);
        } catch (NoSuchMethodException e) {
            // Running on an older platform.
            mStartForeground = mStopForeground = null;
        }
        
        startForeground(this.ONGOING_NOTIFICATION,getNotification());
		 xFilter = new SimpleFilter();
	      yFilter = new SimpleFilter();
	      zFilter = new SimpleFilter();

	    		
	    		timer = new Timer(true); //daemon timer
	    		
	    		
	    		try{
		        	//acquire application context and get a reference to power service
		           Context context = getApplicationContext();
		            PowerManager pm = (PowerManager)context.getSystemService(
		                    Context.POWER_SERVICE);
		          
		            //create a wake lock to prevent phone from sleeping (screen dim/cpu on)
		                      
		            wakeLock = pm.newWakeLock(
		                PowerManager.SCREEN_DIM_WAKE_LOCK,"Actigraphy Service");
		                wakeLock.acquire();
		               
		                if(D){System.out.println("Wakelock held: "+wakeLock.isHeld());}
		               
		        }catch(Exception e){
		        	if(D)Log.e(TAG,"Error aquiring wakelock");
		        	e.printStackTrace();
		        }
		        
		      
		  
		    		
	}

private Notification getNotification(){
	Notification notification = new Notification(android.R.drawable.stat_notify_error
			, "Starting actigraphy data collection",  System.currentTimeMillis());
					Intent notificationIntent = new Intent(this, ActigraphyService.class);
					notificationIntent.putExtra("stopService", true);
					PendingIntent pendingIntent = PendingIntent.getService(this, 0, notificationIntent, 0);
					notification.setLatestEventInfo(this, "Lucid Dreaming App Actigraphy Service",
					       "Collects motion data. Touch to stop", pendingIntent);
					return notification;
					
//					05-15 13:29:27.353: INFO/ActivityManager(152): Starting activity: Intent { act=android.intent.action.MAIN cmp=com.android.settings/.RunningServices }

}
	
	
	   private static final Class[] mStartForegroundSignature = new Class[] {
	        int.class, Notification.class};
	    private static final Class[] mStopForegroundSignature = new Class[] {
	        boolean.class};
	    
	    private NotificationManager mNM;
	    private Method mStartForeground;
	    private Method mStopForeground;
	    private Object[] mStartForegroundArgs = new Object[2];
	    private Object[] mStopForegroundArgs = new Object[1];
	    

	    /**
	     * This is a wrapper around the new startForeground method, using the older
	     * APIs if it is not available.
	     */
	    void startForegroundCompat(int id, Notification notification) {
	        // If we have the new startForeground API, then use it.
	        if (mStartForeground != null) {
	            mStartForegroundArgs[0] = Integer.valueOf(id);
	            mStartForegroundArgs[1] = notification;
	            try {
	                mStartForeground.invoke(this, mStartForegroundArgs);
	            } catch (InvocationTargetException e) {
	                // Should not happen.
	                Log.w("MyApp", "Unable to invoke startForeground", e);
	            } catch (IllegalAccessException e) {
	                // Should not happen.
	                Log.w("MyApp", "Unable to invoke startForeground", e);
	            }
	            return;
	        }
	        
	        // Fall back on the old API.
	        setForeground(true);
	        mNM.notify(id, notification);
	    }
	    
	    /**
	     * This is a wrapper around the new stopForeground method, using the older
	     * APIs if it is not available.
	     */
	    void stopForegroundCompat(int id) {
	        // If we have the new stopForeground API, then use it.
	        if (mStopForeground != null) {
	            mStopForegroundArgs[0] = Boolean.TRUE;
	            try {
	                mStopForeground.invoke(this, mStopForegroundArgs);
	            } catch (InvocationTargetException e) {
	                // Should not happen.
	                Log.w("MyApp", "Unable to invoke stopForeground", e);
	            } catch (IllegalAccessException e) {
	                // Should not happen.
	                Log.w("MyApp", "Unable to invoke stopForeground", e);
	            }
	            return;
	        }
	        
	        // Fall back on the old API.  Note to cancel BEFORE changing the
	        // foreground state, since we could be killed at that point.
	        mNM.cancel(id);
	        setForeground(false);
	    }
	
	

	@Override
	public void onStart(Intent intent, int startId) {
		// TODO Auto-generated method stub
		super.onStart(intent, startId);
		if(D)Log.d(TAG, "onStart");
		
		try {
			if(intent.getExtras()!=null){
				if(intent.getExtras().getBoolean("stopService")){
					this.stopSelf();
				}
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			if(D)e.printStackTrace();
		}
		
		SharedPreferences prefs = PreferenceManager
        .getDefaultSharedPreferences(getBaseContext());
		setupDefaultPreferences(prefs);
		registerAccelerometerListener(prefs);
		setupCustomSharedPreferences();
		
	    	startTimer();
	
	
   
		
	}
	
	
	
	
	
	

	protected void setupDefaultPreferences(SharedPreferences prefs){
		
		//TODO change to false after testing 
		 calibrationCompleted = prefs.getBoolean("calibration_completed", true);
		
	     
	}
	
	protected void setupCustomSharedPreferences(){
		 //Get the filter parameters from preferences
		//changing the step can be done on the fly
		 //step = digital accelerometer unit of measurement in Gs
		
        SharedPreferences mySharedPreferences = getSharedPreferences(
                "LucidDreamingAppPreferences", Activity.MODE_PRIVATE);
       //register ourselves as a listener
        mySharedPreferences.registerOnSharedPreferenceChangeListener(this);
        
        
	      KalmanFilter.setupFilter(xFilter, mySharedPreferences.getFloat("xR", xSensitivity), 0);
	    	//	InMotionCalibration.setupFilter(yFilter, ySensitivity);
	      KalmanFilter.setupFilter(yFilter, mySharedPreferences.getFloat("yR", ySensitivity), 0);
	    	//	InMotionCalibration.setupFilter(zFilter, zSensitivity);
	      KalmanFilter.setupFilter(zFilter, mySharedPreferences.getFloat("zR", zSensitivity), 10);
        
	    if(D)Log.e(TAG, "x R: "+mySharedPreferences.getFloat("xR", xSensitivity));
		if(D)Log.e(TAG, "y R: "+mySharedPreferences.getFloat("yR", ySensitivity));
		if(D)Log.e(TAG, "z R: "+mySharedPreferences.getFloat("zR", zSensitivity));
        
        xStep=mySharedPreferences.getFloat("xStep", 0.04086106F);
		yStep=mySharedPreferences.getFloat("yStep", 0.04086106F);
		zStep=mySharedPreferences.getFloat("zStep", 0.04086106F);
		if(D)Log.e(TAG, "xStep: "+xStep);
		if(D)Log.e(TAG, "yStep: "+yStep);
		if(D)Log.e(TAG, "zStep: "+zStep);
		
		//variance of noise around the mean 
		xNoiseVarianceR=mySharedPreferences.getFloat("xNoiseVarianceR", (3*xStep)*(3*xStep) );
		yNoiseVarianceR=mySharedPreferences.getFloat("yNoiseVarianceR", (3*yStep)*(3*yStep));
		zNoiseVarianceR=mySharedPreferences.getFloat("zNoiseVarianceR", (3*zStep)*(3*zStep));
		

		if(D)Log.e(TAG, "xNoiseVarianceR: "+xNoiseVarianceR);
		if(D)Log.e(TAG, "yNoiseVarianceR: "+yNoiseVarianceR);
		if(D)Log.e(TAG, "zNoiseVarianceR: "+zNoiseVarianceR);
	}
	
	protected void registerAccelerometerListener(SharedPreferences prefs){
		
		//just in case, avoid duplicate listeners
		try{
			if(mSensorManager!=null && mAccelerometer!=null){
				mSensorManager.unregisterListener(this);
			}
		}catch(Exception e){}
		
		 int samplingRate =1200;
			try{
		        //no idea why I cant call .intValue() directly on the parsed int here
		        String samplingRatePref = prefs.getString("accelerometer_sampling_rate", "1200");
		        Integer integer= (Integer.parseInt(samplingRatePref));	        
		        samplingRate =integer.intValue();
		        }catch(Exception e){  samplingRate = 1200; }
		
		   try{	
		        mSensorManager = (SensorManager)getSystemService(Context.SENSOR_SERVICE);
		        mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
		       	        
		        switch(samplingRate){
		        case 600:
		        	  mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_UI);
		        	  if(D) Log.e(TAG, "Normal sampling rate");
		        	break;
		        case 2400:
		        	  mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_FASTEST);
		        	  if(D) Log.e(TAG, "Fastest Sampling rate");
		        	break;	
		        	
		        case 1200:
		        	  mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_GAME);
		        	  if(D) Log.e(TAG, "Game sampling rate");
		        	break;
		        	
		        
		        default:
		        	  mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_GAME);
		        }
	            
	        Intent intent = new Intent();
	       
	        
	        //TODO remove after testing!
	     //   mSensorManager.registerListener(filterHandler, mAccelerometer, SensorManager.SENSOR_DELAY_GAME);
	        }catch(Exception e){}
		
	}
	
	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
			String key) {
		if(key.equals("calibration_completed")){
			 calibrationCompleted = sharedPreferences.getBoolean("calibration_completed", false);
			if(D)Log.e(TAG, "Preference changes ");
		}
		
		//changing sampling rate is rare, specifically check if it has happened.
		else 	if(key.equals("accelerometer_sampling_rate")){
			registerAccelerometerListener(sharedPreferences);
		}
		
		//change the steps ont the fly if they change in preferences
		else if(key.equals("xStep")){	xStep= sharedPreferences.getFloat("xStep", 0.04086106F);
		//xNoiseVarianceR=(3*xStep)*(3*xStep);
		}
		else if(key.equals("yStep")){	yStep= sharedPreferences.getFloat("yStep", 0.04086106F);
		//yNoiseVarianceR=(3*yStep)*(3*yStep);
		}
		else if(key.equals("zStep")){	zStep= sharedPreferences.getFloat("zStep", 0.04086106F);
		//zNoiseVarianceR=(3*zStep)*(3*zStep);
		}
		
		 
		else if(key.equals("xNoiseVarianceR"))xNoiseVarianceR= sharedPreferences.getFloat("xNoiseVarianceR", (3*xStep)*(3*xStep));
		else if(key.equals("yNoiseVarianceR"))yNoiseVarianceR=sharedPreferences.getFloat("yNoiseVarianceR", (3*yStep)*(3*yStep));
		else if(key.equals("zNoiseVarianceR"))zNoiseVarianceR=sharedPreferences.getFloat("zNoiseVarianceR", (3*zStep)*(3*zStep));
			

	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		// TODO Auto-generated method stub
		super.onStartCommand(intent, flags, startId);
		if(D)Log.d(TAG, "ON START COMMAND");
		
		//prevent service restarts on crash
		return START_NOT_STICKY;
	}

	

	@Override
	public void onSensorChanged(SensorEvent event) {

		if(calibrationCompleted){ //only loop if calibration was completed at least once
		//TODO fix this method			
		float[]temp = event.values;
		x=temp[0];
		y=temp[1];
		z=temp[2];
		
		if(!readyToProcess){
			//if(D)Log.e(TAG, "*Not Ready To Process*");
			//set up the R matrix. Set it to squared the analog to digital converter step
			//to eliminate random step switching due to noise
	
			
		
			//TODO pass the xPrev throught settings if needed
		//	readyToProcess = true;
			accelerometerEventCounter=0;
			
		}
		
		if(readyToProcess){
			
			
			//Get the guess of the true position of the system
			xFiltered = KalmanFilter.KalmanFilter(xFilter, x, xNoiseVarianceR);
			yFiltered = KalmanFilter.KalmanFilter(yFilter, y, yNoiseVarianceR);
			zFiltered = KalmanFilter.KalmanFilter(zFilter, z, zNoiseVarianceR);
			
			//create data to show user 
			//createGraphDataJSON();
			
		//	debugLog.appendEntry(getDebugLogEntry());
			
			
			xActivityCount +=processValues(x,xFiltered,xPrev,xStep);
			yActivityCount +=processValues(y,yFiltered,yPrev,yStep);
			zActivityCount +=processValues(z,zFiltered,zPrev,zStep);
			
//			//attempts to compute how far x lies from the filtered value and uses
//			//the rate of change of the difference to estimate the intensity of activity
//			diff = Math.abs((x-xFiltered)-xPrev)/xStep;
//			if(diff>2){
//				
//				diff-=2;
//				//diff = diff/accelerometerResolution;
//			xActivityCount +=Math.min(Math.floor(diff),MAX_CONTRIBUTION);
//			//xActivityCount +=Math.min(Math.floor(diff*diff),25);
//			
//				}
//			xPrev = x-xFiltered;
//			
//			
//			diff = Math.abs((y-yFiltered)-yPrev)/yStep;
//			if(diff>2){
//				diff-=2;
//				yActivityCount +=Math.min(Math.floor(diff),MAX_CONTRIBUTION);
//				//yActivityCount +=Math.min(Math.floor(diff*diff),25);
//				
//			}
//			yPrev = y-yFiltered;
//			
//			
//			 diff = Math.abs((z-zFiltered)-zPrev)/zStep;
//			 if(diff>3){
//					diff-=3;
//					zActivityCount +=Math.min(Math.floor(diff),MAX_CONTRIBUTION);
//					//zActivityCount +=Math.min(Math.floor(diff*diff),25);
//					
//				}
//			zPrev = z-zFiltered;
//		
	
		}
		//TODO remove after testing. Used to check register/unregister listener
		accelerometerEventCounter++;
		
		}
	
		
	}//end on sensor changed event

	protected void startTimer(){
		
		if(timerStarted){
			//avoid duplicate timers! 
		}else{
			
			running = true;
			timerStarted = true;
			
			if(D)Log.w(TAG,"*Timer Started*");
			timer = new Timer();
			readyToProcess = true;
			EpochCounterTask task = new EpochCounterTask();
			AutoSaveTask saveTask = new AutoSaveTask();
			
			//give statMagnitude enough time to get values
			//after 60 sec, every 60 sec
			timer.scheduleAtFixedRate(task,15000, epochLengthMs);
			timer.scheduleAtFixedRate(saveTask,645000, 600000);
			
			}
		
	}
	
	class AutoSaveTask extends TimerTask{
		
		public void run(){		
				
				if(D)Log.e(TAG,"Autosaving graph data");
		
				
			
		}
		
	}
	
	class EpochCounterTask extends TimerTask{
		//TODO REWRITE USING main thread's handler method
		
		//ensure the main thread does data processing and updates views
		public void run() {
			//readyToProcess = true;
			if(calibrationCompleted){
			processData();
			}else{
				resetEpochVariables();
			}
			
		}
	}		
	
	private void processData(){
		if(D) Log.e(TAG,"processing data");
		//creates a skeleton of the sleep data point with only the activity counts, passes it to the parent for future processing
//		SleepDataPoint epoch = new SleepDataPoint();
//		
//		Calendar temp = Calendar.getInstance();
//		epoch.setCalendar(temp);
//		epoch.setSleepEpoch(epochCounter);	
//		int count = xActivityCount+yActivityCount+zActivityCount;		
//		epoch.setActivityCount(count);
//		epoch.setEventCount(accelerometerEventCounter);		
//		epoch.setXActivityCount(xActivityCount);	
//		epoch.setYActivityCount(yActivityCount);
//		epoch.setZActivityCount(zActivityCount);
//		epoch.setAccelerometerAccuracy(accelerometerAccuracy);
//		
//		if(D) Log.e(TAG,"Time: "+temp.getTime().toLocaleString());
//		if(D) Log.e(TAG,"activity count: "+count);
//		if(D) Log.e(TAG,"Accelerometer events: "+accelerometerEventCounter);
//		
//		parent.processData(epoch);
		
		
		//create an intent and give it a pre-defined name for our intent filter
		Intent intent = new Intent(NEW_EPOCH);		
		
		//create a new data bundle
		Bundle bundle = new Bundle();
		bundle.putInt(s_epoch, epochCounter);
		bundle.putInt(s_epochLength, epochLength);
		bundle.putInt(s_accelerometerEvents,accelerometerEventCounter );
		bundle.putInt(s_xActivityCount, xActivityCount);
		bundle.putInt(s_yActivityCount, yActivityCount);
		bundle.putInt(s_zActivityCount, zActivityCount);
		bundle.putString("accuracy", accelerometerAccuracy);
		intent.putExtra("epochData", bundle);
		
		//send intent over 
		ActigraphyService.this.sendBroadcast(intent);
		
		//increment event counter
			epochCounter++;	
			
			
		/*For some reason there's a class loader exception when unmarshalling a parcellable within another application
		 * it seems to work fine within the same package
		 * 
		 */
		//create a parcel with data from this epoch, 
//		ActigraphParcel parcel = new ActigraphParcel(epochCounter , epochLength, accelerometerEventCounter,
//				xActivityCount,yActivityCount,zActivityCount);
		//intent.putExtra("epochData", parcel);
		
		
		
		//get ready for a new epoch
		resetEpochVariables();
	}
	


	
	
	
	void resetEpochVariables(){
		xActivityCount=0;
	
		yActivityCount=0;
		
		zActivityCount=0;
	
    	accelerometerEventCounter=0;
    	
	}
	
	
	private static final String LOW_ACCURACY = "Low Accuracy";
	private static final String MEDIUM_ACCURACY = "Medium Accuracy";
	private static final String HIGH_ACCURACY = "High Accuracy";
	private static final String UNRELIABLE_ACCURACY = "Unreliable Accuracy";
	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
		// TODO Auto-generated method stub
		try{
		String message = "";
		switch (accuracy){
			case SensorManager.SENSOR_STATUS_ACCURACY_HIGH:
				//if(D) Log.w(TAG,sensor.getName()+ message);
				message = HIGH_ACCURACY;
				break;
			case SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM:
				//if(D) Log.w(TAG,sensor.getName()+ message);
				message = MEDIUM_ACCURACY;
				break;
			case SensorManager.SENSOR_STATUS_ACCURACY_LOW:
				//if(D) Log.w(TAG,sensor.getName()+ message);
				message = LOW_ACCURACY;
				break;
			case SensorManager.SENSOR_STATUS_UNRELIABLE:
				//if(D) Log.w(TAG,sensor.getName()+ message);
				message = UNRELIABLE_ACCURACY;
				
		}
		
		if(!accelerometerAccuracy.equals(message)){
			accelerometerAccuracy= message;
			if(D) Log.w(TAG,sensor.getName()+ message);
		}
		}catch(Exception e){
			if(D)e.printStackTrace();
		}
		//	
	}
	@Override
	public void onDestroy() {
		// TODO Auto-generated method stub
		super.onDestroy();
		if(D)Log.d(TAG, "onDestroy");
		try{	
	        mSensorManager.unregisterListener(this);
	        
	        //TODO remove after testing!
	     //   mSensorManager.registerListener(filterHandler, mAccelerometer, SensorManager.SENSOR_DELAY_GAME);
	        
	        }catch(Exception e){
	        	e.printStackTrace();
	        }
	        
	        try{
	        	timer.cancel();
	        	timer.purge();
	        	timer = null;
	        	if(wakeLock.isHeld()){wakeLock.release();}
	        }catch(NullPointerException e){}
	        timerStarted=false;
	        running = false;
	}
	
	private double processValues(float value,double valueFiltered, double prevValue, double step){
		double diff = Math.abs((value-valueFiltered)-prevValue)/step;
		if(diff>2){			
			diff-=2;	
		//xPrev = x-xFiltered;
		return Math.min(Math.floor(diff),MAX_CONTRIBUTION);
	}else return 0;
	}
	
	
}
