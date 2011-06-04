package com.luciddreamingapp.beta;

import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Random;
import java.util.Timer;

import org.json.JSONObject;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.gesture.Gesture;
import android.gesture.GestureLibraries;
import android.gesture.GestureLibrary;
import android.gesture.GestureOverlayView;
import android.gesture.GestureOverlayView.OnGesturePerformedListener;
import android.gesture.Prediction;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Typeface;
import android.media.MediaPlayer;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.PowerManager;
import android.os.SystemClock;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.provider.Settings.SettingNotFoundException;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Chronometer;
import android.widget.DigitalClock;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.luciddreamingapp.beta.util.ActigraphyService;
import com.luciddreamingapp.beta.util.AudioAnalyzerService;
import com.luciddreamingapp.beta.util.JSONLoader;
import com.luciddreamingapp.beta.util.MorseCodeConverter;
import com.luciddreamingapp.beta.util.SleepAnalyzer;
import com.luciddreamingapp.beta.util.SleepDataManager;
import com.luciddreamingapp.beta.util.SleepDataPoint;
import com.luciddreamingapp.beta.util.gesturebuilder.GestureBuilderActivity;
import com.luciddreamingapp.beta.util.state.WILDTimer;


public class NightGUIActivity extends Activity implements Interactive,OnGesturePerformedListener,OnSharedPreferenceChangeListener {

	//used to enable or disable console messages 
	private static final boolean D = false;//debug
	private static final boolean RTD = false;//real time debug through javascript
	 private static final String TAG = "Lucid Dreaming GUI";//tag for LogCat and eclipse log
	 private static final boolean C = true;
	
		
		private boolean gesturesEnabled = false;
		private boolean brightnessAdjustEnabled = false;
		private float defaultBrightness = 0.25f;
	
		int brightnessMode;
		
		public static float brightness = 0.25f;
		float tempBrightness = brightness+0.02f;
		
		private Typeface lcdFont;
		private DigitalClock clock;
		
		//used to display the math puzzle
		private static final int CUSTOM_ALERT = 3;
		private final static int VISUAL = 0;
		private final static int CHALLENGING = 1;
		private final static int REAL_NOT_REAL = 2;
		private Chronometer mChronometer;
		private Chronometer mChronometer2;
	    private TextView puzzle;
	    private View layout;
	    
	    private GlobalApp parent;
	    
	    private WILDTimer wildTimer;
	    private String WILDTimerConfigFilepath;
	    
	    private Random r;
	    //59 is 17th prime
	    int[] primes = {2, 3, 5, 7, 11, 13, 17, 19, 23, 29, 31, 37, 41, 43, 47, 53, 59, 61, 67, 71, 73, 79, 83, 89, 97};

	    private static final long challenge = (((60+10+1)*60+20)*1000);//default challenge
	    private static final long dayMillis = 24*60*60*1000;
	    private static final String challengeString="01:11:20";
	    private boolean result = false;
	    private boolean challengingPuzzle = false;
		
	    int maxActivityCount = 1;

		private  GestureLibrary gestureLibrary;  
		private  GestureOverlayView gestures2; 
		
	    public static PowerManager.WakeLock wakeLock;

//	private static final String EPOCH_LOG_HEADERS_NEW= "Accelerometer activity, averaged along all axis" +
//	"data summed into epochs of default lenth of around 60seconds\r\n"
//+"Timestamp,Local Date,Time,Hour,Minute,Second," +
//	"Sum of Epoch Activity,Number of Accelerometer events that minute,Threshold Count, Threshold,xAvg, yAvg, zAvg";
//	
//	
	//private static final String DETAIL_LOG_HEADERS ="Timestamp,Date,Time,Hour,Minute,Second,TotalActivity,epochActivity,epochLength";
		
	static final String SLEEP_SCORE_HEADERS ="";
	
	
	private SleepAnalyzer sleepAnalyzer;
	
	private int userEventInt = 0;

	
	//TODO clean up variable names for more clarity


	Timer timer;//used for initializing the application and data collection
	
	
	boolean timerStarted = false;
	
	private boolean playSoundReminders = true;
	private String mp3reminderFilepath = "/sdcard"+LucidDreamingApp.MP3_LOCATION;
	private String defaultFilepath = "/sdcard/Recordings/lucid.mp3";
	MediaPlayer mp;

	
	
	//Data processing handler
	Handler handler = new Handler();
	
	JSONDataHandler graphData; 
	SleepDataManager dataManager;
	
	
	
	private boolean calibrationCompleted= false;
	private boolean calibrationIntentStarted = false;
	
	
	
	public NightGUIActivity() {
		super();
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		// TODO Auto-generated method stub
		
		if(D)Log.e(TAG, "+ON CREATE+");
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		
		
		dataManager = SleepDataManager.getInstance();
		
		//add a reference to this class for interactions
		sleepAnalyzer = SleepAnalyzer.getInstance();		
		sleepAnalyzer.getSmartTimer().setParent(this);
        sleepAnalyzer.getSmartTimer().setParentHandler(handler);
		
		
		r=new Random();
		
			 parent =(GlobalApp)this.getApplication();
			 
			 LayoutInflater inflater = (LayoutInflater) this.getSystemService(LAYOUT_INFLATER_SERVICE);
		    	layout = inflater.inflate(R.layout.chronometer,
		    	                               (ViewGroup) findViewById(R.id.layout_root));
		    	
		    	//Math puzzle text view 
		    	puzzle = (TextView) layout.findViewById(R.id.puzzle);
		    	
		    	//initialize clock font
		    	try{
		    	
		   		 lcdFont = Typeface.createFromAsset(getBaseContext().getAssets(),
//		         "fonts/LCDN.TTF");}catch(Exception e){if(D)e.printStackTrace();}
		   		// "fonts/Liquidn.ttf");}catch(Exception e){if(D)e.printStackTrace();}
	         
		       "fonts/liquidcrystal.ttf");}catch(Exception e){if(D)e.printStackTrace();}
		       
		    	puzzle.setTypeface(lcdFont);
		    	puzzle.setTextSize(24);
		    	//colors are assigned in setupViews
		    	//puzzle.setTextColor(Color.GREEN);
		    	
		    	mChronometer = (Chronometer) layout.findViewById(R.id.chronometer);
		        mChronometer.setTypeface(lcdFont);
		        mChronometer.setTextSize(24);
		       // mChronometer.setTextColor(Color.GREEN);
		        mChronometer2 = (Chronometer) layout.findViewById(R.id.chronometer2);
		        mChronometer2.setTypeface(lcdFont);
		        mChronometer2.setTextSize(24);
		       // mChronometer2.setTextColor(Color.GREEN);
				
		        
		        //attempt to reset the countdown
		       sleepAnalyzer = SleepAnalyzer.getInstance();
		      
		   
		       
		       SharedPreferences customSharedPreference = getSharedPreferences(
		                 "LucidDreamingAppPreferences", Context.MODE_PRIVATE);
				// String configFilepath = customSharedPreference.getString("configFilepath",new File(Environment.getExternalStorageDirectory(),new File(LucidDreamingApp.APP_HOME_FOLDER, "SmartTimerConfig.txt.gzip").getAbsolutePath()).getAbsolutePath());
			//	 smartTimer=(new SmartTimer(Calendar.getInstance().getTime().toLocaleString(),configFilepath));
				 WILDTimerConfigFilepath=customSharedPreference.getString("WILDTimerConfigFilepath",new File(Environment.getExternalStorageDirectory(),new File(LucidDreamingApp.APP_HOME_FOLDER, "WILDTimerConfig.txt.gzip").getAbsolutePath()).getAbsolutePath());
		      // sleepAnalyzer.setSmartTimer(smartTimer);
		        
			 
		
			//if(D)Log.e(TAG,"DataManager"+dataManager.hashCode());

		
		
		//initialize kalman filters
//		xFilter = new SimpleFilter();
//		yFilter = new SimpleFilter();
//		zFilter = new SimpleFilter();
//		
		if(graphData ==null){
		graphData = new JSONDataHandler();
		}
	
		
		SharedPreferences prefs = PreferenceManager
        .getDefaultSharedPreferences(getBaseContext());
			gesturesEnabled =prefs.getBoolean("enable_gestures", false);
			prefs.registerOnSharedPreferenceChangeListener(this);
			
		playSoundReminders = prefs.getBoolean("play_sound_pref", true);
		calibrationCompleted = prefs.getBoolean("calibration_completed", false);
		
		//display the sensitivity message
		
		printSensitivityMessage();
		
	
		
		
		
		
	}

	

	/* (non-Javadoc)
	 * @see android.app.Activity#onStart()
	 */
	@Override
	protected void onStart() {
		
		super.onStart();
				if(D)Log.e(TAG, "++ON START++");
				
				
//				try{
//		        	//acquire application context and get a reference to power service
//		           Context context = getApplicationContext();
//		            PowerManager pm = (PowerManager)context.getSystemService(
//		                    Context.POWER_SERVICE);
//		          
//		            //create a wake lock to prevent phone from sleeping (screen dim/cpu on)
//		                      
//		            wakeLock = pm.newWakeLock(
//		                PowerManager.SCREEN_DIM_WAKE_LOCK,"Lucid Dreaming App");
//		        //        wakeLock.acquire();
//		               
//		                if(D){System.out.println("Wakelock held: "+wakeLock.isHeld()+" "+TAG);}
//		                sendToast(getString(R.string.toast_plug_in));
//		        }catch(Exception e){
//		        	if(D)Log.e(TAG,"Error aquiring wakelock");
//		        	e.printStackTrace();
//		        }
				
				
				
				SharedPreferences prefs = PreferenceManager
		         .getDefaultSharedPreferences(getBaseContext());
			
					gesturesEnabled =prefs.getBoolean("enable_gestures", false);
					
				calibrationCompleted = prefs.getBoolean("calibration_completed", false);  
		 		setupViews(gesturesEnabled);
		 
		 		
		 	       
		 	       //fixes the "cant use back button" bug with calibration intent jamming the back button
	            if(!calibrationCompleted && !calibrationIntentStarted){
	            	
	           	 //TODO disable application running
	           	 //startButton.setEnabled(false);
	           	// startButton.setText(R.string.main_menu_start_app_disabled);
	           try{	 this.sendToast(getString(R.string.toast_calibrate));
	           	 
	           	int  accelerometer_calibration_duration_min = prefs.getInt("accelerometer_calibration_duration_min", 7);
            	sendToast(getString(R.string.toast_calibration_duration,accelerometer_calibration_duration_min ));
            	Intent calibrateAccelerometerIntent = new Intent(this, InMotionCalibration.class);
            	calibrateAccelerometerIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                calibrateAccelerometerIntent.putExtra("accelerometer_calibration_duration_min", accelerometer_calibration_duration_min);
                
            	startActivity(calibrateAccelerometerIntent);
            	
	           }catch(Exception e){if(D)e.printStackTrace();}
            	calibrationIntentStarted =true;
	            }else if(!calibrationCompleted && calibrationIntentStarted){
	            	 //if the user backed out of calibration, just display the instructions, but do not do any data collection
	            }else{
	            	//restart the service
	            	if(!ActigraphyService.running){
	                sleepAnalyzer.reset();
	                sleepAnalyzer.getSmartTimer().reset();
	                startService(new Intent(this, AudioAnalyzerService.class));
	            	
	                
	              //  startService(new Intent(this, ActigraphyService.class));
	                
	                
	                
	            	}
	            	
	            	
	            	brightnessAdjustEnabled= prefs.getBoolean("enable_screen_brightness_adjustment", false);
	        		if(brightnessAdjustEnabled){
	        		  try {
	        			  //switch to manual brightness mode
	        			 int tempBrightnessMode = Settings.System.getInt(getContentResolver(), Settings.System.SCREEN_BRIGHTNESS_MODE);
	        				if (tempBrightnessMode == Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC) {
	        					brightnessMode=Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC;
	        				    Settings.System.putInt(getContentResolver(), Settings.System.SCREEN_BRIGHTNESS_MODE, Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL);
	        				}
	        			} catch (SettingNotFoundException e) {
	        				// TODO Auto-generated catch block
	        			if(D)	e.printStackTrace();
	        			}
	        			  
	        			  
	        			  //save the default brightness
	        			defaultBrightness =getBrightness();
	        			
	        			//set brightness
	        			int tempInt = (prefs.getInt("brightness_level", 10));
	        			if(tempInt>0){
	        				
	        				sendShortToast(getString(R.string.toast_brightness_level,tempInt)+"%");
	        				
	        			brightness = tempInt/100f;
	        			}else{
	        				
	        				//regular start **************************
	        				setupPreferences();
	        				
	        				sendShortToast(getString(R.string.toast_brightness_level,1)+"%");
	        				brightness = 0.01f;
	        				//pass the reference to the current smart timer.
	        				parent.setGuiActivity(this);
	        			}
	        			if(D)Log.e(TAG, "pref brightness "+brightness);
	        			
	        		//	setBrightness(brightness/100);
	        			
	        		}
	            	
	            }
	           
	            ((GlobalApp)getApplication()).setGuiActivity(this);
	            
	}
	
	
	


/* (non-Javadoc)
	 * @see android.app.Activity#onResume()
	 */
	@Override
	protected void onResume() {
		// TODO Auto-generated method stub
		super.onResume();
		if(D)Log.e(TAG, "+++ON RESUME+++");
		
		SharedPreferences prefs = PreferenceManager
        .getDefaultSharedPreferences(getBaseContext());
		 playSoundReminders = prefs.getBoolean("play_sound_pref", true);
		  
	     setupSmartTimer();
//	       
	        mp = new MediaPlayer();
	      
		
	}
	
	
	protected void setupPreferences(){

		//restore preferences
		SharedPreferences mySharedPreferences = getSharedPreferences(
                "LucidDreamingAppPreferences", Activity.MODE_PRIVATE);
		
		mySharedPreferences.registerOnSharedPreferenceChangeListener(this);
		
		mp3reminderFilepath = mySharedPreferences.getString("reminderFilepath", "/sdcard/Recordings/lucid.mp3");
		
		SharedPreferences prefs = PreferenceManager
         .getDefaultSharedPreferences(getBaseContext());
	
				
		//get the sound preference and notify the user if the file does not exist
		 playSoundReminders = prefs.getBoolean("play_sound_pref", false);
		 
		 if(mp3reminderFilepath.equals(defaultFilepath)){
			 if(playSoundReminders && !new File(defaultFilepath).exists()){
					sendToast(getString(R.string.toast_reminder_does_not_exist,mp3reminderFilepath));
				}	
		 }
	}
	
	
	
	
	@Override
	public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
		// TODO implement changes to the UI layout preferences here
		if(key.equals("reminderFilepath"))mp3reminderFilepath = prefs.getString("reminderFilepath", "/sdcard/Recordings/lucid.mp3");
		
		else if(key.equals("enable_gestures")){
			boolean tempGesturesEnabled= false;		
			tempGesturesEnabled=prefs.getBoolean("enable_gestures", false);
			if(gesturesEnabled!=tempGesturesEnabled){
				gesturesEnabled=tempGesturesEnabled;
				setupViews(gesturesEnabled);
			}
		}
		else if(key.equals("challenging_math_puzzle"))challengingPuzzle= prefs.getBoolean("challenging_math_puzzle", false);
		else if(key.equals("clock_digit_size"))clock.setTextSize(prefs.getInt("clock_digit_size", 128));
		else if(key.equals("clockColor")){
			int color =prefs.getInt("clockColor", Color.GREEN);
		
			//assign color to the clock and math puzzle
		 clock.setTextColor(color);
	     mChronometer.setTextColor(color);
	     mChronometer2.setTextColor(color);
		}
		
		else if(key.equals("play_sound_pref")) playSoundReminders = prefs.getBoolean("play_sound_pref", false);
		
	}

	@Override
	protected void onPostResume() {
//		 updateWebViewText();
		// TODO Auto-generated method stub
		super.onPostResume();
		if(this.brightnessAdjustEnabled){
		tempBrightness = brightness+0.02f;
		handler.postDelayed(new Task(),1000);
		}
	}

	//stop collecting data when we lost focus
	@Override
	public void onPause(){
		if(D)Log.e(TAG, "-ON PAUSE-");
		super.onPause();
	
	
	}
	
	

	@Override
	protected void onStop(){
		super.onStop();
		
		try{
			//restore automatic brightness
			//restore brightness
			setBrightness(-1.0f);
			
			if(this.brightnessMode == Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC){
			Settings.System.putInt(getContentResolver(), Settings.System.SCREEN_BRIGHTNESS_MODE, Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC);
			}
			

//			if(D)Log.e(TAG, "WakeLock released");
//			wakeLock.release();
		}catch(Exception e){}
		
	}
	
	/* (non-Javadoc)
	 * @see android.app.Activity#onDestroy()
	 */
	@Override
	protected void onDestroy() {
		// TODO Auto-generated method stub
		super.onDestroy();
		if(D)Log.e(TAG,"---ON DESTROY---");
		saveData();
		
		
		
		//stopService(new Intent(this, ActigraphyService.class));
//		try{	
//	       
//	        
//	        //TODO remove after testing!
//	     //   mSensorManager.registerListener(filterHandler, mAccelerometer, SensorManager.SENSOR_DELAY_GAME);
//	        if(wakeLock.isHeld()){  wakeLock.release();}
//	        }catch(Exception e){
//	        	e.printStackTrace();
//	        }
	        
		
	        
		
	}

	@Override
	 public boolean onCreateOptionsMenu(Menu menu) {
	        MenuInflater inflater = getMenuInflater();
	        inflater.inflate(R.menu.accelerometer_monitoring_activity_menu, menu);
	        menu.removeItem(R.id.menu_show_clock); //clock is already shown, remove that option
	        return true;
	    }
	
	private void setupViews(boolean gesturesEnabled){
		 
		 
  		
		if(gesturesEnabled){
			  // with gestures
			  setContentView(R.layout.accelerometer_main);			  
	            try{
	            gestureLibrary= GestureLibraries.fromFile(GestureBuilderActivity.GESTURES_FILE);
	            
	            gestures2= (GestureOverlayView) findViewById(R.id.gestures2);	            
	            gestures2.addOnGesturePerformedListener(this);
	            gestureLibrary.load();
	           
	           
	            
	            
	            }catch(Exception e){if(D)e.printStackTrace();}
	            }   else{
	            	//without gestures
	            	 setContentView(R.layout.lucid_dreaming_app_no_gestures);}
	            	       	
	   			  	//gestures only apply to listView	   			  	
		if(C){       
		//initialize clock, set color and scale text
		   clock = (DigitalClock)findViewById(R.id.digitalclock);
			SharedPreferences prefs = PreferenceManager
	      	.getDefaultSharedPreferences(getBaseContext());
	          
	       clock.setTextColor(prefs.getInt("clockColor", Color.GREEN));
	  		clock.setTextSize(prefs.getInt("clock_digit_size", 128));
	  		
	  		clock.setTypeface(lcdFont);
	  		
		        int color = prefs.getInt("clockColor", Color.GREEN);  
		        //clock.setTextColor(color);
		        
		        //math puzzle chronometers
		       		        
		        mChronometer.setTextColor(color);
		        mChronometer2.setTextColor(color);
		        challengingPuzzle= prefs.getBoolean("challenging_math_puzzle", false);
	  		
		}
		
	    	    
	    	         if(gesturesEnabled){
	    	          
	    	        	gestures2.setVisibility(View.VISIBLE);
	    	        	clock.setVisibility(View.VISIBLE);}	    	        
	    	        
	    	         	clock.setVisibility(View.VISIBLE);
	            
	    	         
	}
	
	

		
		//#########################MISC METHODS##################################
		@Override
		public void startInteraction(){
			
			startInteraction(mp3reminderFilepath);
			
		}
		
		//Methods for communicating with the javascript list
		@Override
		public String getSleepStatus(){
			
			return dataManager.getSleepStatus();
		}
		@Override
		public void setSleepStatus(String message){
			dataManager.setSleepStatus(message);
		
			
		}
		
	
		
		
		
		public void describeSettings(){
//			
//			if(D) Log.e(TAG,"+Starting App with the following parameters:+");
//			//TODO pull directly from preferences
		}
	
		public int getUserEventInt() {
			return userEventInt;
		}

		public void setUserEventInt(int userEventInt) {
			
			parent.setUserEvent(userEventInt);
			//this.userEventInt = userEventInt;
		}

		
	
	
	

	
	private void saveData(){
		
		//access the sleep data
    	if(dataManager.getStatistics().getTotalTimeInBed()>5){
    		//retrieves a hash map (min, epoch value) and converts to json object
    		try{
    		JSONObject temp = dataManager.saveDataJSON();
    		if(temp!=null){
    			
    			
    			sendToast(getString(R.string.toast_graph_saved,JSONLoader.saveJSONData(temp.toString())));
    			}else{sendShortToast("No active graphs to save");}
    		}catch(Exception e){if(D) e.printStackTrace();}
    		
    		
    	}else{sendShortToast(getString(R.string.toast_no_data_to_save));}
    		
    		
	}
	
	
	@Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        
    }
	
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {

        
        case R.id.menu_show_list:
        	Intent listIntent = new Intent(getBaseContext(),
                    RealTimeListActivity.class);
        	listIntent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        		startActivity(listIntent);        	
        	
        	return true;
        	
        case R.id.menu_show_graph:
        	Intent graphIntent = new Intent(getBaseContext(),
                    RealTimeGraphActivity.class);
        		graphIntent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        		startActivity(graphIntent);
            
        	return true;
        
        	//do nothing, already in clock view
       case R.id.menu_show_clock:
        	            
        	return true;
        	
        	
     
        //saves the currently displayed graph as a JSON File	
        case R.id.menu_save_data:
        	//TODO fix saving data
        saveData();
        	
        return true;
        	
        case R.id.menu_preferences:
        		Intent settingsActivity = new Intent(getBaseContext(),
                    Preferences.class);
        		startActivity(settingsActivity);
        	
        	       	
        	return true;
        	
        	
       
        	
        case R.id.menu_exit_app:
        	        
        	saveData();
        	stopService(new Intent(this, AudioAnalyzerService.class));
        	stopService(new Intent(this, ActigraphyService.class));
        	dataManager.reset();
        	//stopService(new Intent(this, ActigraphyService.class));
               	
        	
        	finish();
        	
        	return true;
        	
                
        }
        

        
  
        return false;
    }
	
	
	protected Dialog onCreateDialog(int id) {
	    Dialog dialog;
	    switch(id) {
	    	
	    case CUSTOM_ALERT:
	    	AlertDialog.Builder builder;
	    	AlertDialog alertDialog;
	    	
	    	
	    	
	        
	        if(challengingPuzzle){
	        result=	createPuzzle(CHALLENGING);
	        }else{
	        	result = createPuzzle(VISUAL);
	        }
	    	
	    	mChronometer.start();
	    	mChronometer2.start();

	    if(D)	System.out.println("chronometer 1 base: "+mChronometer.getBase());
	    	if(D)System.out.println("alertDialog "+mChronometer2.getBase());
	    	
	    	builder = new AlertDialog.Builder(this);
	    	
	    	builder.setMessage(getString(R.string.dialog_math_puzzle))
	 	       .setCancelable(true)
	 	       .setPositiveButton(getString(R.string.dialog_yes), new DialogInterface.OnClickListener() {
	 	           public void onClick(DialogInterface dialog, int id) {
	 	        	  NightGUIActivity.this.displayResults(true);
	 	           }
	 	       })	 	       
	 	       .setNegativeButton(getString(R.string.dialog_no), new DialogInterface.OnClickListener() {
	 	           public void onClick(DialogInterface dialog, int id) {
	 	        	  NightGUIActivity.this.displayResults(false);
	 	           }
	 	       });
	    	
	    	
	    	builder.setView(layout);
	    	alertDialog = builder.create();
	    if(D)System.out.println("alertDialog "+alertDialog);
	    	return alertDialog;
	    	
	    default:
	        dialog = null;
	    }
	 return dialog;   
	}
	
	protected void onPrepareDialog(int id, Dialog dialog) {
		
		
		super.onPrepareDialog(id, dialog);
		
		if(id == CUSTOM_ALERT){
			  if(challengingPuzzle){
			        result=	createPuzzle(CHALLENGING);
			        }else{
			        	result = createPuzzle(VISUAL);
			        }
			mChronometer.start();
	    	mChronometer2.start();
		}
		
}
	
	/**
	 * Displays puzzle results
	 * @param answer user's answer to the puzzle. If it matches the result of the puzzle creation, a 
	 * congratulations message is shown. Else a try again message is shown
	 */
	protected void displayResults(boolean answer){
		mChronometer.stop();
		mChronometer2.stop();
		if(result && result==answer){
			//<!-- Right answers  -->
			sendShortToast(getString(R.string.toast_true_right));
		}else if(!result && result==answer){
			sendShortToast(getString(R.string.toast_false_right));
			//Wrong answers
		}else if(result &&!(result == answer) ){
			sendShortToast(getString(R.string.toast_true_wrong));
		}else if(!result &&!(result == answer) ){
			sendShortToast(getString(R.string.toast_false_wrong));
		}
	}
	

    private boolean createPuzzle(int difficulty){
    	Calendar calendar =Calendar.getInstance();
    	long startTime = (
    	calendar.get(Calendar.HOUR_OF_DAY)*60*60+ //60 min and 60 seconds
    	calendar.get(Calendar.MINUTE)*60+//60 seconds
    	calendar.get(Calendar.SECOND))*1000;//add all and convert to milliseconds
    	
    	long puzzle=0;
    	
    	boolean returnResult = false;
    	
    	switch(difficulty){
    	case VISUAL:
    	
    		//true result 50% chance
    	if(r.nextBoolean()){
    			puzzle= challenge;//add 01:11:10 to the timer
    		//	sendShortToast("True");
    			returnResult=true;
    	
    	}else{ //tricky result 50% chance
    		int temp = r.nextInt(6)+1;
    		int switchInt = r.nextInt(100);
    		//may be tricky in 4 different ways
    		if(switchInt>=75){
    			puzzle= (((60+10*temp+1)*60+20)*1000);//increment by random 10 minute interval
    		}else if(switchInt>=50&&switchInt<75){
    			puzzle= (((60+10+1*temp)*60+20)*1000);//increment by random 1 minute interval
    		}else if(switchInt>=25&&switchInt<50){
    			puzzle= (((60+10+1)*60+20*temp)*1000);//increment by random 20seconds interval
    		}else{
    			puzzle= (((60+10*temp+1)*60+20*temp)*1000);//increment by random 10min and 20 sec interval
    		}
    		//sendShortToast("False");
    		returnResult=false;
    		}
    	
    		displayPuzzle(challengeString,null);
			mChronometer.setBase((SystemClock.elapsedRealtime()-startTime%dayMillis)); //current time
			mChronometer2.setBase((SystemClock.elapsedRealtime()-(startTime+puzzle)%dayMillis));//+real puzzle time
    		
    		return returnResult;
    	case CHALLENGING:
    		
    		//String first="01:11:20";
    		String second="00:00:00";
    	
    		
    		long temp =0;
    		temp += primes[r.nextInt(17)]*60*1000;//add a random prime of less than an hour
    		temp +=(r.nextInt(6)+1)*10*60*1000;//add a random multiple of 10 minutes
    		long hours = temp /(60*60*1000);
			long minutes = temp%(60*60*1000)/60000;
			long seconds = temp%60000;
			
		
			
			
			second = String.format(" %02d:%02d:%02d", (int)hours,(int)minutes,(int)seconds);
			System.out.println(temp);
    			System.out.println(second);
    			
    		if(r.nextBoolean()){
    			//add 01:11:10 to the timer
    			//add a random time to the timer
    			puzzle = 0;
    			puzzle += temp;
    			puzzle += challenge;
    			
    			returnResult=true;
    		//	sendShortToast("True");
    	
    	}else{ //tricky result
    		int offset = r.nextInt(6)+1;
    		int switchInt = r.nextInt(100);
    		
    		puzzle+=temp;
    		puzzle+=challenge;
    		
    		//may be tricky in 4 different ways
    		if(switchInt>=75){
    			puzzle+= offset*10*60*1000;//random 10 minutes
    		}else if(switchInt>=50&&switchInt<75){
    			puzzle+= offset*60*1000; //random 1 minute
    		}else if(switchInt>=25&&switchInt<50){
    			puzzle+= 10*offset*1000; //random 10 seconds
    		}else{
    			puzzle+= offset*10*60*1000+10*offset*1000; //+random 10 min 10 sec
    		}
    		//sendShortToast("False");
    		returnResult=false;
    		}
    	
    	//TODO mod by 24 hours to keep from showing incorrect times
    		displayPuzzle(challengeString,second);
			mChronometer.setBase((SystemClock.elapsedRealtime()-startTime%dayMillis)); //current time
			System.out.println("base: "+mChronometer.getBase());
			System.out.println("base % dayMillis : "+ mChronometer.getBase()%dayMillis);
			System.out.println("base-puzzle-start % dayMillis : "+ (mChronometer.getBase()%dayMillis-(startTime+puzzle)%dayMillis));
			System.out.println((SystemClock.elapsedRealtime()-startTime-puzzle));
			System.out.println((SystemClock.elapsedRealtime()-(startTime%dayMillis)));
			mChronometer2.setBase((SystemClock.elapsedRealtime()-(startTime+puzzle)%dayMillis));//+real puzzle time
    		
    		
    		
    		break;
    	case REAL_NOT_REAL:
    		
    		break;
    	
    	}
    	return returnResult;
    }
    
    private void displayPuzzle(String first, String second){
    	if(second!=null){puzzle.setText(" "+first+"\n"+second);}
    	else{puzzle.setText(first);}
    }
    
    public void notifyREM(boolean isInREM){
    try{
    	if(isInREM){
    		//change clock color to light blue while in REM
    		clock.setTextColor(-16732161);
    	}else{
    		//use default color
    		 clock = (DigitalClock)findViewById(R.id.digitalclock);
 			SharedPreferences prefs = PreferenceManager
 	      	.getDefaultSharedPreferences(getBaseContext());
 	          
 	       clock.setTextColor(prefs.getInt("clockColor", Color.GREEN));
    	}
    }catch(Exception e){if(D)e.printStackTrace();}
    }
	
	
	 @Override
		public void onGesturePerformed(GestureOverlayView overlay, Gesture gesture) {
	    	if(gesturesEnabled){
	    
	    	ArrayList<Prediction> predictions = gestureLibrary.recognize(gesture);

	    	    // We want at least one prediction
	    	    if (predictions.size() > 0) {
	    	       
	    	    	
	    	    	Prediction prediction = predictions.get(0);
	    	    	
	    	        // We want at least some confidence in the result
	    	        if (prediction.score > 2.1) {
	    	        	try{
	    	        	//TODO rewrite using sleep data point's static methods
	    	            String temp = prediction.name;
	    	        	if(temp.toLowerCase().matches(".*?"+getString(R.string.gesture_awake)+".*?")){
	    	        		setUserEventInt(SleepDataPoint.USER_EVENT_AWAKE);
	    	        	}else if(temp.toLowerCase().matches(".*?"+getString(R.string.gesture_no_dream)+".*?")){
	    	        		setUserEventInt(SleepDataPoint.USER_EVENT_NO_DREAM);
	    	        	}else if(temp.toLowerCase().matches(".*?"+getString(R.string.gesture_lucid_dream)+".*?")){
	    	        		setUserEventInt(SleepDataPoint.USER_EVENT_LUCID_DREAM);
	    	        	}else if(temp.toLowerCase().matches(".*?"+getString(R.string.gesture_normal_dream)+".*?")){
	    	        		setUserEventInt(SleepDataPoint.USER_EVENT_DREAM);
	    	        	}else if(temp.toLowerCase().matches(".*?"+getString(R.string.gesture_math_puzzle)+".*?")){
	    	        		showDialog(CUSTOM_ALERT);
	    	        		setUserEventInt(SleepDataPoint.USER_EVENT_NOT_RECOGNIZED);
	    	        		return;//do not show confirmation 
	    	        	}else if(temp.toLowerCase().matches(".*?"+getString(R.string.gesture_wild_timer)+".*?")){
	    	        	//TODO remove after testing
	    	        		startWILDEvent(wildEventStarted);
	    	        		wildEventStarted = !wildEventStarted;
	    	        		setUserEventInt(SleepDataPoint.USER_EVENT_NOT_RECOGNIZED);
	    	        		return;
	    	        	
	    	        		
	    	        	}
	    	        	else if(temp.toLowerCase().matches(".*?test+.*?")){
		    	        	doTest();
		    	        	setUserEventInt(SleepDataPoint.USER_EVENT_NOT_RECOGNIZED);
		    	        	return;
		    	        		
		    	        	}
	    	        	}catch(Exception e){
	    	        		//catching regex complie errors on Droid phones
	    	        		if(D)e.printStackTrace();
	    	        	}
	    	        	
	    	        	//TODO look up the gesture in a hashmap.
	    	        	try{
	    	        		//try to create a thumbnail from the gesture that is recognized
	    	        	final Resources resources = getResources();
	    	        	int  mPathColor = resources.getColor(R.color.gesture_color);
	    	           	int mThumbnailInset = (int) resources.getDimension(R.dimen.gesture_thumbnail_inset);
	    	            int mThumbnailSize = (int) resources.getDimension(R.dimen.gesture_repeat_size);
	    	        	final Bitmap bitmap = gesture.toBitmap(mThumbnailSize, mThumbnailSize,
                                mThumbnailInset, mPathColor);
	    	        	//getString(R.string.toast_gesture_recognized,prediction.name)
	    	        	showCustomGestureToast(bitmap,prediction.name); //short toast with the gesture name
	    	        	}catch(Exception e){
	    	        		if(D)e.printStackTrace();
	    	        		//in case the custom image creation fails
	    	        		Toast.makeText(this,prediction.name, Toast.LENGTH_SHORT).show();
	    	        	}
	    	        	
	    	        	
	    	            
	    	        }else{sendShortToast(getString(R.string.toast_gesture_not_recognized));
	    	        		setUserEventInt(SleepDataPoint.USER_EVENT_NOT_RECOGNIZED);
	    	        }
	    	      
	    	    }
	    	}
		}
	
	 protected void sendToast(String message){
	    	Context context = getApplicationContext();
			  Toast toast = Toast.makeText(context, message,Toast.LENGTH_LONG);
			  
			  toast.show();
	    }
	 
	    protected void sendShortToast(String message){
	    	Context context = getApplicationContext();
			  Toast toast = Toast.makeText(context, message,Toast.LENGTH_SHORT);
			  
			  toast.show();
	    }
	    
	    protected void showCustomGestureToast(Bitmap bm, String message){
			 LayoutInflater inflater = getLayoutInflater();
		        View layout = inflater.inflate(R.layout.gesture_toast_layout,
		            (ViewGroup) findViewById(R.id.tableLayout1));
		 
		        ImageView image = (ImageView) layout.findViewById(R.id.imageView1);
		       // image.setImageResource(R.drawable.icon);
		        image.setImageBitmap(bm);
		        TextView text = (TextView) layout.findViewById(R.id.textView1);
		        text.setTextColor(getResources().getColor(R.color.confirm_color));
		        text.setTextSize(32);
		        text.setText(message);
		 
		        Toast toast = new Toast(getApplicationContext());
		        toast.setGravity(Gravity.CENTER_VERTICAL|
		                Gravity.CENTER_HORIZONTAL, 0, 0);
		        toast.setDuration(Toast.LENGTH_LONG);
		        toast.setView(layout);
		        toast.show();
		 }
	
	    //##########################EXPERIMENTAL CLASSES###################################
	    @Override
	    public  void startVibrateInteraction(String message){
			startVibrateInteraction(MorseCodeConverter.pattern(message,120));
			
		}	  
	    
	    public void startVibrateInteraction(long[] pattern){

	    	//trying to fix sound not playing bug
			SharedPreferences prefs = PreferenceManager
	        .getDefaultSharedPreferences(getBaseContext());
							
			playSoundReminders = prefs.getBoolean("play_sound_pref", true);
			
	    	if(playSoundReminders){
			//long[] pattern = MorseCodeConverter.pattern(message,300);
//
//			System.out.println(Arrays.toString(pattern));
//			Intent strobeIntent = new Intent(this, Strobe.class);
//    		strobeIntent.putExtra("strobeTiming", pattern);
//    		startActivity(strobeIntent);
    		
            // Start the vibration. Requires VIBRATE permission
            Vibrator vibrator = (Vibrator)getSystemService(Context.VIBRATOR_SERVICE);
            
            vibrator.vibrate(pattern, -1);
	    	}
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
	        	mp = new MediaPlayer();		        	
	        	mp.setDataSource(filepath);
	        	mp.prepare();
	        	mp.start();		
	        	if(D)Log.e(TAG, "Sound played: "+filepath);
	        	}
	        }
	        }catch(Exception e){
	        if(D){ Log.w(TAG,"Start PlaybackException!\n"+e.getMessage());
	        e.printStackTrace();}
	        }	 
		}

		
	protected void	setupSmartTimer(){
			  SharedPreferences customSharedPreference = getSharedPreferences(
		                 "LucidDreamingAppPreferences", Context.MODE_PRIVATE);
				 String configFilepath = customSharedPreference.getString("configFilepath",new File(Environment.getExternalStorageDirectory(),new File(LucidDreamingApp.APP_HOME_FOLDER, "SmartTimerConfig.txt.gzip").getAbsolutePath()).getAbsolutePath());
			 //update the smart timer with latest events
				sleepAnalyzer.getSmartTimer().setupEvents(configFilepath);
				sleepAnalyzer.getSmartTimer().setParent(this);
				sleepAnalyzer.getSmartTimer().setParentHandler(handler);
		}



		boolean wildEventStarted = true;
		private void startWILDEvent(boolean eventStarted){
			if(eventStarted){
				//initializing wild timer here in case the 
				try{
					if(wildTimer!=null){wildTimer.cancelTimers();}
					
					  SharedPreferences customSharedPreference = getSharedPreferences(
				         "LucidDreamingAppPreferences", Context.MODE_PRIVATE);
						 WILDTimerConfigFilepath=customSharedPreference.getString("WILDTimerConfigFilepath",new File(Environment.getExternalStorageDirectory(),new File(LucidDreamingApp.APP_HOME_FOLDER, "WILDTimerConfig.txt.gzip").getAbsolutePath()).getAbsolutePath());
					
					
					wildTimer = new WILDTimer(WILDTimerConfigFilepath);
					wildTimer.setParent((GlobalApp)getApplication());
					}
					catch(Exception e){if(D)e.printStackTrace();}
				wildTimer.scheduleTimers();
				sendShortToast(getString(R.string.toast_wild_timers_scheduled));}
			else{
				wildTimer.cancelTimers();
				sendShortToast(getString(R.string.toast_wild_timers_cancelled));}
				
			
			
		}
	
		protected void printSensitivityMessage(){
			SharedPreferences mySharedPreferences = getSharedPreferences(
	                "LucidDreamingAppPreferences", Activity.MODE_PRIVATE);
			 float tempColeConstant = mySharedPreferences.getFloat("coleConstantVeryHighThreshold", 0.003341F);
		        SleepAnalyzer.setColeConstant(tempColeConstant);
		        maxActivityCount = (int)(1/(2.3*tempColeConstant));
//		       float tempThresholdDelayed = mySharedPreferences.getFloat("thresholdVeryHighSensitivity", 13);
		        
//		        
		        int activityThreshold = InMotionCalibration.calculateActivityPeakDelayed(tempColeConstant);
//		        sleepAnalyzer.setActivityThreshold(activityThreshold);
		        
		        //notify us of state changes
		       
		        
		        
		        StringBuilder sb = new StringBuilder(32);
		        sb.append("<br>");
		        sb.append(getString(R.string.list_activity_sensitivity_config_message,tempColeConstant));		 
		        sb.append(getString(R.string.list_activity_sensitivity_config_message_2,activityThreshold));
		        sb.append("</br></br>");
		        
		        setSleepStatus(sb.toString());
		}
		
		
		public void setBrightness(float brightness){

			 WindowManager.LayoutParams lp = getWindow().getAttributes();

            lp.screenBrightness = brightness;

            getWindow().setAttributes(lp);
		}
		
		public float getBrightness(){
			 return getWindow().getAttributes().screenBrightness;
		}
		
		
		 class Task implements Runnable {
		    	
		        public void run() {      
		        	
		        	if(D)Log.e(TAG,"reducing brightness ");
		        	if(D)Log.e(TAG,"reducing brightness "+tempBrightness + " "+brightness);
		                if (tempBrightness > brightness) {
		                	setBrightness(tempBrightness); //start at 10% bright and go to 0 (screen off)

		                	tempBrightness-=0.01;
		                        handler.postDelayed(new Task(), 75L);
		                }
		        }
		 }


		public Handler getHandler() {
			return handler;
		}

		public void setHandler(Handler handler) {
			this.handler = handler;
		}

		 private void doTest(){
			  if(D)Log.e(TAG, "Starting voice task ");
			 ((GlobalApp)getApplication()).voiceInteractAsync("/sdcard/Recordings/lucid.mp3");
			  if(D)Log.e(TAG, "Starting vibrate task");
			  ((GlobalApp)getApplication()).vibrateInteractAsync(MorseCodeConverter.pattern("test test test",120));
			  if(D)Log.e(TAG, "Starting strobe task");
			  ((GlobalApp)getApplication()).strobeInteractAsync(MorseCodeConverter.pattern("test test test",120));
		 }
		 
		
		 

}
