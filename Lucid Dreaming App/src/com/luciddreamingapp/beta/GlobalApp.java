package com.luciddreamingapp.beta;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.UUID;

import org.json.JSONObject;

import android.app.Activity;
import android.app.Application;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Picture;
import android.media.MediaPlayer;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Environment;
import android.os.Handler;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.Toast;

import com.luciddreamingapp.actigraph.ActigraphyService;
import com.luciddreamingapp.beta.actigraph.parcels.ActigraphReceiver;
import com.luciddreamingapp.beta.util.AutomaticUploaderService;
import com.luciddreamingapp.beta.util.JSONLoader;
import com.luciddreamingapp.beta.util.MorseCodeConverter;
import com.luciddreamingapp.beta.util.SleepAnalyzer;
import com.luciddreamingapp.beta.util.SleepDataManager;
import com.luciddreamingapp.beta.util.SleepDataPoint;
import com.luciddreamingapp.beta.util.state.SmartTimer;

public class GlobalApp extends Application implements OnSharedPreferenceChangeListener, Interactive {
	
	private static final String TAG = "Global Lucid Dreaming App";
	private static final boolean D = false;//debug
	
	private static final boolean BRACELET_DEBUG = true;
		
	private int userEvent = 0;
	String accelerometerAccuracy ="";
	
	private SleepDataManager dataManager;
	private SleepAnalyzer sleepAnalyzer;
	private SmartTimer smartTimer;

	private int epochCount;
	
	private boolean autoSaveEnabled = true;
	
	private boolean playSoundReminders = true;
	private String mp3reminderFilepath = "/sdcard/Recordings/lucid.mp3";
	
	//file uploader destination
	public static String serverUrl = "http://luciddreamingapp.com/fileuploads/uploader";
	public static String serverUrlManual = "http://luciddreamingapp.com/fileuploads/uploader_manual.php";
	public static final String UPLOAD_INTENT = "com.luciddreamingapp.uploader.START_UPLOAD";
	
	
	private MediaPlayer mp;
	
	private HashMap<Integer,Long> map = new HashMap<Integer,Long>();
	
	public static String uuid;
	
	SharedPreferences prefs;
	SharedPreferences mySharedPreferences;
	
	private Handler handler;
	
	private NightGUIActivity guiActivity;
	
	//ensures actigraph events are delivered to the receiver
	private static IntentFilter filter;
	//listens to actigraph events
	private static ActigraphReceiver receiver;
	
	
	@Override
	public void onCreate() {
		// TODO Auto-generated method stub
		super.onCreate();
		if(D)Log.e(TAG, "+++ON CREATE+++");
		dataManager = SleepDataManager.getInstance();
		
		SleepDataManager.globalApp = this;
		sleepAnalyzer = SleepAnalyzer.getInstance();
		if(D)sleepAnalyzer.describeSettings(D); //print out all configuration parameters
		sleepAnalyzer.setDataManager(dataManager);
		
		prefs = PreferenceManager
        .getDefaultSharedPreferences(getBaseContext());
		prefs.registerOnSharedPreferenceChangeListener(this);
		 mySharedPreferences = getSharedPreferences(
                "LucidDreamingAppPreferences", Activity.MODE_PRIVATE);
		 mySharedPreferences.registerOnSharedPreferenceChangeListener(this);
		this.setupCustomSharedPreferences();
		this.setupDefaultSharedPreferences(prefs);
		
		handler = new Handler();
		
		
		 //assign a UUID to this installation
        uuid = prefs.getString("UUID", "");
        int uploadDay = prefs.getInt("uploadDay", 0);
        
        if(uuid.equals("")){
       	 Editor editor = prefs.edit();
       	 uuid = UUID.randomUUID().toString();
       	 editor.putString("UUID", uuid);
       	 editor.commit();
        }
        //assign a default upload day. During this day the data will be uploaded
        if(uploadDay ==0){
       	 Editor editor = prefs.edit();
       	 editor.putInt("uploadDay", (int)(Math.random()*7+1));
       	 editor.commit();
        }
        
        serverUrl += uploadDay+".php";
    	if(D)Log.i(TAG, serverUrl );
       	if(D)Log.i(TAG, uuid );
        //add uuid
		dataManager.setUuid(uuid);
		
		
		//create a new intent filter to listen to actigraph events
		filter = new IntentFilter("com.luciddreamingapp.actigraph.NEW_EPOCH");
		receiver = new ActigraphReceiver(this);
		
		//connect the reciver and the filter
		 registerReceiver(receiver, filter);
		 	
		 
		
	}

	

//
//	public void addEpoch(int epoch,long timestamp){
//		Log.e("Global App", "received "+epoch+ " at "+timestamp);
//		map.put(epoch, timestamp);
//		//implement producer - consumer pattern with a thread waking up every so many seconds to check if data is available. 
//	}
	
	//returns the database key(timestamp) for the given epoch
	public Long getEpoch(int epoch){
		return map.get(epoch);
	}
	
	public void setUserEvent(int event){
		userEvent = event;
		
	}

	
	public String getAccelerometerAccuracy() {
		return accelerometerAccuracy;
	}

	public void setAccelerometerAccuracy(String accelerometerAccuracy) {
		this.accelerometerAccuracy = accelerometerAccuracy;
	}

	public void autoSaveData(){
		if(autoSaveEnabled){
			if(D)Log.w(TAG, "AutoSaving data ");
			saveData();
		}
	}
	
	public void saveData(){
		if(D)Log.w(TAG, "Saving data ");
//		if(dataManager.getStatistics().getTotalTimeInBed()>5){
    		//retrieves a hash map (min, epoch value) and converts to json object
    		try{
    		JSONObject temp = dataManager.saveDataJSON();
    		if(temp!=null){
    			
    			
    			
    					JSONLoader.saveJSONData(temp.toString());
    			}else{
    				//sendShortToast("No active graphs to save");
    				}
    		}catch(Exception e){if(D) e.printStackTrace();}
    		
    		
//    	}else{
//    		}
	}
	
	public void processData(SleepDataPoint epoch){
		
		
		if(D)Log.w(TAG, "Processing Data: ");
		try{
			if(epoch!=null){
		epoch.setUserEvent(userEvent);
		userEvent = 0;
		//notify smart timer
		if(smartTimer!=null){
			smartTimer.dataPointAdded(epoch);
		}
		
		
		dataManager.addSleepDataPoint(epoch);
		if(D)Log.w(TAG, epoch.toDetailedString());
		
		epochCount=epoch.getSleepEpoch();
		sleepAnalyzer.analyze(epochCount-3);
			
		if(D)Log.w(TAG, dataManager.getSleepDataPoint(epochCount-3).toDetailedString());
			}
			
		}catch(NullPointerException e){
			if(D)e.printStackTrace();
		}
		
	}

	public int getEpochCount() {
		return epochCount;
		
	}

	protected void setupDefaultSharedPreferences(SharedPreferences prefs){

		autoSaveEnabled  = prefs.getBoolean("graph_data_autosave_pref",true);
		
		dataManager.setActivityCountYMax(prefs.getInt("activity_count_y_axis_max", 2500));
		dataManager.setSleepScoreYMax(prefs.getInt("sleep_score_y_axis_max", 35));
		
		
		sleepAnalyzer.setRemPredictionActivityThreshold(prefs.getInt("rem_prediction_activity_threshold", 10));
		sleepAnalyzer.setRemPredictionActivityThreshold(prefs.getInt("minimum_reminder_spacing", 15));
		
		
		sleepAnalyzer.setEnableREMPrediction(prefs.getBoolean("enable_smart_timer", true));
	      sleepAnalyzer.setPlayLateReminders(prefs.getBoolean("play_sound_pref_non_rem", true));
	        
	      sleepAnalyzer.setEarliest120(prefs.getInt("deep_sleep_120_earliest", 45));
	      sleepAnalyzer.setLatest120(prefs.getInt("deep_sleep_120_latest", 55));
	        
	      sleepAnalyzer.setEarliest120to180(prefs.getInt("deep_sleep_150_earliest", 30));
	      sleepAnalyzer.setLatest120to180(prefs.getInt("deep_sleep_150_latest", 45));
	        
	      sleepAnalyzer.setEarliest180(prefs.getInt("deep_sleep_180_earliest", 20));
	      sleepAnalyzer.setLatest180(prefs.getInt("deep_sleep_180_latest", 25));
      
	       
	        sleepAnalyzer.setRemindersToPlay(prefs.getInt("number_of_reminders", 1));
	         if(D) sleepAnalyzer.describeSettings(D);
	       
	        
	        boolean loggingEnabled = prefs.getBoolean("log_data_pref",true);
	     
	        dataManager.setLoggingEnabled(loggingEnabled);
	      
	}
	
	protected void setupCustomSharedPreferences(){
		
		
		
	
		mp3reminderFilepath = mySharedPreferences.getString("reminderFilepath", "/sdcard/Recordings/lucid.mp3");
		
		 //get cole constant, default corresponds to 13 activity counts per minute average, 131 will trigger the reminder
		  float tempColeConstant = mySharedPreferences.getFloat("coleConstantVeryHighThreshold", 0.003341F);
	        SleepAnalyzer.setColeConstant(tempColeConstant);
	        
	      
			 String configFilepath = mySharedPreferences.getString("configFilepath",new File(Environment.getExternalStorageDirectory(),new File(LucidDreamingApp.APP_HOME_FOLDER, "SmartTimerConfig.txt.gzip").getAbsolutePath()).getAbsolutePath());
			setupSmartTimer(configFilepath);
	        
	        int activityThreshold = InMotionCalibration.calculateActivityPeakDelayed(tempColeConstant);
	        sleepAnalyzer.setActivityThreshold(activityThreshold);
	        
	    
	}
	
	
	protected void setupSmartTimer(String configFilepath){
		
//		if(sleepAnalyzer.getSmartTimer()!=null){
//			dataManager.unregisterObserver(sleepAnalyzer.getSmartTimer(), SleepDataManager.DATA_POINT_ADDED);
//		}
		 smartTimer =new SmartTimer(Calendar.getInstance().getTime().toLocaleString(),configFilepath);
		 smartTimer.setGlobalApp(this);
		sleepAnalyzer.setSmartTimer(smartTimer);
		//start responding to new data points in real time
//		dataManager.addObserver(smartTimer, SleepDataManager.DATA_POINT_ADDED);
	}
	
	@Override
	public void onSharedPreferenceChanged(SharedPreferences prefs,
			String key) {
		//TODO implement changes to sleep analyzer and data manager preferences
		if(key.equals("graph_data_autosave_pref"))autoSaveEnabled  = prefs.getBoolean("graph_data_autosave_pref",true);
	
		else if(key.equals("configFilepath")){
			String configFilepath = prefs.getString("configFilepath",new File(Environment.getExternalStorageDirectory(),new File(LucidDreamingApp.APP_HOME_FOLDER, "SmartTimerConfig.txt.gzip").getAbsolutePath()).getAbsolutePath());
			 
			sleepAnalyzer.getSmartTimer().setupEvents(configFilepath);
	        
		}
		
		else if(key.equals("activity_count_y_axis_max"))	dataManager.setActivityCountYMax(prefs.getInt("activity_count_y_axis_max", 2500));
		else if(key.equals("sleep_score_y_axis_max"))	dataManager.setSleepScoreYMax(prefs.getInt("sleep_score_y_axis_max", 35));
	
		else if(key.equals("rem_prediction_activity_threshold"))sleepAnalyzer.setRemPredictionActivityThreshold(prefs.getInt("rem_prediction_activity_threshold", 10));
		else if(key.equals("minimum_reminder_spacing"))sleepAnalyzer.setRemPredictionActivityThreshold(prefs.getInt("minimum_reminder_spacing", 15));
		
		else if(key.equals("enable_smart_timer"))sleepAnalyzer.setEnableREMPrediction(prefs.getBoolean("enable_smart_timer", false));
		else if(key.equals("play_sound_pref_non_rem"))sleepAnalyzer.setPlayLateReminders(prefs.getBoolean("play_sound_pref_non_rem", true));
	   
	     
		else if(key.equals("deep_sleep_120_earliest")) sleepAnalyzer.setEarliest120(prefs.getInt("deep_sleep_120_earliest", 45));
		else if(key.equals("deep_sleep_120_latest")) sleepAnalyzer.setLatest120(prefs.getInt("deep_sleep_120_latest", 55));
	        
		else if(key.equals("deep_sleep_150_earliest")) sleepAnalyzer.setEarliest120to180(prefs.getInt("deep_sleep_150_earliest", 30));
		else if(key.equals("deep_sleep_150_latest"))  sleepAnalyzer.setLatest120to180(prefs.getInt("deep_sleep_150_latest", 45));
	        
		else if(key.equals("deep_sleep_180_earliest")) sleepAnalyzer.setEarliest180(prefs.getInt("deep_sleep_180_earliest", 20));
		else if(key.equals("deep_sleep_180_latest"))  sleepAnalyzer.setLatest180(prefs.getInt("deep_sleep_180_latest", 25));
    
		else if(key.equals("number_of_reminders"))    sleepAnalyzer.setRemindersToPlay(prefs.getInt("number_of_reminders", 1));
		
		else if(key.equals("coleConstantVeryHighThreshold")){
		 float tempColeConstant = prefs.getFloat("coleConstantVeryHighThreshold", 0.003341F);
	        SleepAnalyzer.setColeConstant(tempColeConstant);
	        
	      int activityThreshold = InMotionCalibration.calculateActivityPeakDelayed(tempColeConstant);
	      sleepAnalyzer.setActivityThreshold(activityThreshold);
	      if(D)Log.e(TAG, "Cole constant changed " +tempColeConstant);
		}
		
	}


	
	

	@Override
	public void onTerminate() {
		// TODO Auto-generated method stub
		super.onTerminate();
		try{
		stopService(new Intent(this, ActigraphyService.class));
		unregisterReceiver(receiver);
		}catch(Exception e){if(D)e.printStackTrace();}
	}



	public NightGUIActivity getGuiActivity() {
		return guiActivity;
	}



	public void setGuiActivity(NightGUIActivity guiActivity) {
		this.guiActivity = guiActivity;
		if( sleepAnalyzer!=null){
			sleepAnalyzer.getSmartTimer().setGlobalApp(this);
			sleepAnalyzer.getSmartTimer().setParent(guiActivity);
		}
	}
	
	  @Override
	    public  void startVibrateInteraction(String message){
			startVibrateInteraction(MorseCodeConverter.pattern(message,120));
			
		}	  
	    
	 /**Turns off bluetooth, making a bluetooth enabled bracelet vibrate 
	  * re-enables bluetooth after 15 seconds.
	  *  
	  */
	  public void braceletVibrate(){
//		  try{
//		  
//		handler.post(new Runnable(){
//			
//			
//			public void run(){
//				BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();   
//				if (mBluetoothAdapter.isEnabled()) {
//					mBluetoothAdapter.disable();	
//					
//					handler.postDelayed(new Runnable(){
//						
//						
//						public void run(){
//							 if(D)Log.w(TAG, "Resuming bluetooth");
//							BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();   
//							if (mBluetoothAdapter.isEnabled()) {
//								mBluetoothAdapter.disable();	
//							
//								
//							}else{
//								//in case there's an error enabling the adapter
//								mBluetoothAdapter.enable();
//							
//							
//							}
//						}
//						
//					},10000)	;
//					
//				}else{
//					//in case there's an error enabling the adapter
//					mBluetoothAdapter.enable();
//				
//				
//				}
//			}
//			
//		})	;
//  	
//		  }catch(Exception e){
//			  if(D)e.printStackTrace();
//		  }
	  }
	  
	  
	  
	  private class BluetoothVibrateRunnable implements Runnable{
			 public void run(){
				try{
				 BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();   
					mBluetoothAdapter.enable();
				}catch(Exception e){
					if(D)e.printStackTrace();
				}
			 }
		 }
	  
	    public void startVibrateInteraction(long[] pattern){
//	    	if (BRACELET_DEBUG){
//	    		braceletVibrate();
//	    		return;
//	    	}
//	    	
	    	
	    	//trying to fix sound not playing bug
			SharedPreferences prefs = PreferenceManager
	        .getDefaultSharedPreferences(getBaseContext());
							
			playSoundReminders = prefs.getBoolean("play_sound_pref", true);
			
	    	if(playSoundReminders){
			//long[] pattern = MorseCodeConverter.pattern(message,300);
//
	    		if(D)System.out.println(Arrays.toString(pattern));
//			Intent strobeIntent = new Intent(this, Strobe.class);
//  		strobeIntent.putExtra("strobeTiming", pattern);
//  		startActivity(strobeIntent);
  		
          // Start the vibration. Requires VIBRATE permission
          Vibrator vibrator = (Vibrator)GlobalApp.this.getSystemService(Context.VIBRATOR_SERVICE);
          
          vibrator.vibrate(pattern, -1);
	    	}
	    }
	    
	    
	  
		
		@Override
		public String getSleepStatus() {
			// TODO Auto-generated method stub
			return null;
		}



		@Override
		public void setSleepStatus(String message) {
			// TODO Auto-generated method stub
			
		}


		


		@Override
		public void startInteraction() {
			
			startInteraction(mp3reminderFilepath);
		}




		@Override
		public void startInteraction(String filepath) {
			// TODO Auto-generated method stub
			if(D)Log.e(TAG, "Start interaction called"+filepath+ " play reminders: "+playSoundReminders);
	    	
			//trying to fix sound not playing bug
			SharedPreferences prefs = PreferenceManager
	        .getDefaultSharedPreferences(getBaseContext());
							
			playSoundReminders = prefs.getBoolean("play_sound_pref", true);
			
			
			
	    	//load the sound file and prepare to play it
	        try{
	        if(playSoundReminders){
	        	if( mp!=null && mp.isPlaying()){
	        	try{	
	        		mp.stop();
	        		mp.reset();
	        		mp.release();}catch(Exception e){}		        		
		        	mp = new MediaPlayer();
	        		mp.setDataSource(filepath);
		        	mp.prepare();
		        	mp.start();	
		        	if(D)Log.e(TAG, "Sound played: "+filepath);
	        	}else{
	        		
	        		//throw exception to mask media player error codes if file is not present!
	        		if(!(new File(filepath)).exists()){
	        			throw new Exception("File not found");
	        		}
	        		
	        		
	        	mp = new MediaPlayer();		        	
	        	mp.setDataSource(filepath);
	        	mp.prepare();
	        	mp.start();		
	        	if(D)Log.e(TAG, "Sound played: "+filepath);
	        	}
	        }
	        }catch(Exception e){
	       if(D){ Log.w(TAG,"Start PlaybackException!\n"+e.getMessage());
	        e.printStackTrace();
	       }
	        //play default alarm instead
	        try{
	        
	        	MediaPlayer player = MediaPlayer.create(this,
	    			    Settings.System.DEFAULT_ALARM_ALERT_URI);
	    			player.start();
	        }catch(Exception ee){
	        	
	        }
	        
	        
	        
	        }	 
		}
	
	
	 public void voiceInteractAsync(String filepath){
		 //must use handler because this method is being called from threads
		 handler.post(new VoiceRunnable(filepath));
		 
	 }
	 
	
	 
	 private class VoiceRunnable implements Runnable {
		 final String filepath;
		 VoiceRunnable(String filepath){
			 this.filepath = filepath;
		 }
		 @Override
		 public void run(){
			  new VoiceTask().execute(filepath,null,null);
		 }
	 }
	 
	 public void vibrateInteractAsync(long[] timing){
		 handler.post(new VibrateRunnable(timing));
	 }
	 
	 private class VibrateRunnable implements Runnable {
		 final long[] timing;
		 VibrateRunnable(long[] timing){
			 this.timing = timing;
		 }
		 @Override
		 public void run(){
			 
			 //send a broadcast in case there's a vibration bracelet:
			 Intent intent = new Intent("com.luciddreamingapp.beta.START_BRACELET_VIBRATION");
			 sendBroadcast(intent);
			 
			 new VibrateTask().execute(timing, null,null);
		 }
	 }
	 
	 
	 public void strobeInteractAsync(long[] timing){
		handler.post(new StrobeRunnable(timing));
	 }
	 
	 private class StrobeRunnable implements Runnable {
		 final long[] timing;
		 StrobeRunnable(long[] timing){
			 this.timing = timing;
		 }
		 @Override
		 public void run(){
			 new StrobeTask().execute(timing,null,null);
		 }
	 }
	 
	
	 
	 private class VoiceTask extends AsyncTask<String, Void, Void> {
	     protected Void doInBackground(String... urls) {
	        if(D)Log.e(TAG, "voice task executing");
	    	 startInteraction(urls[0]);
	         return null;
	     }

	     protected void onPostExecute() {
	         
	     }
	 }
	 
	 private class VibrateTask extends AsyncTask<long[], Void, Void> {
	     protected Void doInBackground(long[]... urls) {
	        if(D)Log.e(TAG, "vibrate task executing");
	    	 startVibrateInteraction(urls[0]);
	         return null;
	     }

	     protected void onPostExecute() {
	    		
	     }
	 }
	 
	 private class StrobeTask extends AsyncTask<long[], Void, Void> {
	     
		 protected Void doInBackground(long[]... urls) {
			   if(D)Log.e(TAG, "vibrate task executing");
		      
			   Intent strobeIntent = new Intent(GlobalApp.this, Strobe.class);
		        
		        strobeIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);		     

        		strobeIntent.putExtra("strobeTiming", urls[0]);
        		startActivity(strobeIntent);
		       
	        return null;
	     }

	     
	 }
	 
	 
	
	 
	public boolean savePicture(Picture picture, String filepath, int quality) {

 		Bitmap b = Bitmap.createBitmap(picture.getWidth(), picture.getHeight(),
 				Bitmap.Config.ARGB_8888);

 		Canvas c = new Canvas(b);

 		picture.draw(c);

 		FileOutputStream fos = null;

 		try {

 			fos = new FileOutputStream(	filepath);

 			if (fos != null) {
 				b.compress(Bitmap.CompressFormat.PNG, quality, fos);

 				fos.close();
 			}

 		} catch (Exception e) {
 			return false;
 		}
 		return true;
         
     
	}
	
	
	 public boolean getConnectionStatus(){

			ConnectivityManager mConnectivity = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
			TelephonyManager mTelephony = (TelephonyManager)getSystemService(Context.TELEPHONY_SERVICE);
			// Skip if no connection, or background data disabled
			NetworkInfo info = mConnectivity.getActiveNetworkInfo();
			if (info == null ||
			        !mConnectivity.getBackgroundDataSetting()) {
			    return false;
			}
			
			// Only update if WiFi or 3G is connected and not roaming
			int netType = info.getType();
			int netSubtype = info.getSubtype();
			if (netType == ConnectivityManager.TYPE_WIFI) {
			    return info.isConnected();
			} else if (netType == ConnectivityManager.TYPE_MOBILE
			    && netSubtype == TelephonyManager.NETWORK_TYPE_UMTS
			    && !mTelephony.isNetworkRoaming()) {
			        return info.isConnected();
			} else {
			    return false;
			}
		}
	 
	 public SharedPreferences getPreferences(){
		 return PreferenceManager
	        .getDefaultSharedPreferences(getBaseContext());
	 }
	 
	 
	 class ToastRunnable implements Runnable{
		 final String message;
		 ToastRunnable(String message){
			 this.message = message;
		 }
		 
		 public void run(){
			 Context context = getApplicationContext();
			  Toast toast = Toast.makeText(context, message,Toast.LENGTH_SHORT);
			  
			  toast.show();
		 }
	 }
	   public void sendShortToast(String message){
		   
		   handler.post(new ToastRunnable(message));
	    	
	    }
	   
	   
	   
	   
	
	

	   
}
