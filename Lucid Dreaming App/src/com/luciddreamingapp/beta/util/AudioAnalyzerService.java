package com.luciddreamingapp.beta.util;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Timer;
import java.util.TimerTask;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import com.luciddreamingapp.actigraph.ActigraphyService;
import com.luciddreamingapp.beta.GlobalApp;
import com.luciddreamingapp.beta.util.audio.SimpleAudioAnalyser;

public class AudioAnalyzerService extends Service {

	private static final String TAG = "Audio Analyzer";
	private static final boolean D = true;//debug
	 private SleepDataManager dataManager;
	public static final int NOTIFICATION_ID = 81;
	public static boolean running = false;
	
	SimpleAudioAnalyser analyzer;
	
	private Timer timer; 
	
	GlobalApp globalApp;
	
	@Override
	public IBinder onBind(Intent arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void onCreate() {
		// TODO Auto-generated method stub
		super.onCreate();

		
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
	        
	       
		startForegroundCompat(NOTIFICATION_ID,getNotification());
		try{
			
		analyzer = new SimpleAudioAnalyser();
		//start listening for data points added, and add sound to them
		dataManager = SleepDataManager.getInstance();
	       dataManager.addObserver(analyzer, SleepDataManager.DATA_POINT_ADDED);
		timer = new Timer();
		timer.scheduleAtFixedRate(new DelayTask(), 500, 1000);
		analyzer.measureStart();
		}catch(Exception e){
			stopSelf();
		}
		running = true;
		if(D)Log.e(TAG,"Created!");
		
		
		
	}

	@Override
	public void onDestroy() {
		// TODO Auto-generated method stub
		super.onDestroy();
		if(D)Log.e(TAG,"Destroyed");
		 if(timer!=null) timer.cancel();
		if(analyzer!=null){
			dataManager.unregisterObserver(analyzer, SleepDataManager.DATA_POINT_ADDED);
			analyzer.measureStop();
		
		}
	}

	@Override
	public void onStart(Intent intent, int startId) {
		// TODO Auto-generated method stub
		super.onStart(intent, startId);
		
		if(D)Log.e(TAG, "Received intent");
		try {
			if(intent.getExtras()!=null){
				if(intent.getExtras().getBoolean("stopService")){
					stopForeground(true);
					this.stopSelf();
				}
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			if(D)e.printStackTrace();
		}
		
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		// TODO Auto-generated method stub
		return START_NOT_STICKY;
	}

	class DelayTask extends TimerTask{
		
		public void run(){
			analyzer.doUpdate(1);
		}
	}
	
	private Notification getNotification(){
		Notification notification = new Notification(android.R.drawable.stat_notify_error
				, "Starting actigraphy data collection",  System.currentTimeMillis());
						Intent notificationIntent = new Intent(this, AudioAnalyzerService.class);
						notificationIntent.putExtra("stopService", true);
						PendingIntent pendingIntent = PendingIntent.getService(this, 0, notificationIntent, 0);
						notification.setLatestEventInfo(this, "Lucid Dreaming App Audio Analyzer",
								"Monitors microphone for noise levels. No audio is recorded. Touch to stop.", pendingIntent);
						return notification;
						
//						05-15 13:29:27.353: INFO/ActivityManager(152): Starting activity: Intent { act=android.intent.action.MAIN cmp=com.android.settings/.RunningServices }

	}
//
//	private Notification getNotification(){
//		Notification notification = new Notification(android.R.drawable.stat_sys_warning
//				, "Starting noise monitoring",  System.currentTimeMillis());
//						Intent notificationIntent = new Intent(this, AudioAnalyzerService.class);
//						notificationIntent.putExtra("stopService", true);
//						PendingIntent pendingIntent = PendingIntent.getService(this, 0, notificationIntent, 0);
//						notification.setLatestEventInfo(this, "Lucid Dreaming App Audio Analyzer",
//						       "Monitors microphone for noise levels. No audio is recorded. Touch to stop.", pendingIntent);
//						return notification;
//						
////						05-15 13:29:27.353: INFO/ActivityManager(152): Starting activity: Intent { act=android.intent.action.MAIN cmp=com.android.settings/.RunningServices }
//
//	}
//		
		
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
		
	
}
