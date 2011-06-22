package com.luciddreamingapp.beta;


import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Calendar;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.webkit.WebView;
import android.widget.ImageButton;
import android.widget.Toast;

import com.luciddreamingapp.beta.util.AutomaticUploaderService;
import com.luciddreamingapp.beta.util.gesturebuilder.GestureBuilderActivity;
import com.luciddreamingapp.beta.util.state.SmartTimerActivity;




public class LucidDreamingApp extends Activity {
	 private static final String TAG = "LucidDreamingApp";
	 private static final boolean D = false;
	 
	 
	 
	 public static final String APP_HOME_FOLDER = "Application Data/Lucid Dreaming App";
	 public static final String LOG_LOCATION = "Application Data/Lucid Dreaming App/Log";
	 public static final String UPLOAD_LOCATION = "Application Data/Lucid Dreaming App/Upload";
	 public static final String GESTURES_LOCATION = "Application Data/Lucid Dreaming App/Gestures";
	 public static final String GRAPH_DATA_LOCATION = "Application Data/Lucid Dreaming App/Graph Data";
	 public static final String HISTORY_DATA_LOCATION = "Application Data/Lucid Dreaming App/History";
	 public static final String HTML_DATA_LOCATION = "Application Data/Lucid Dreaming App/html";
	 public static final String SCREENSHOT_LOCATION = "Application Data/Lucid Dreaming App/Screenshots";
	 public static final String MP3_LOCATION = "Recordings";
	
	 // Intent request codes
	    private static final int REQUEST_SELECT_FILE = 1;
		    

	WebView listView;
	WebView graphView;
	private ImageButton startButton,infoButton,configButton;
	ImageButton viewDataButton;    
    
    private boolean appStarted = false;
    private Context appContext;
    
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(D) Log.e(TAG, "+++ ON CREATE +++");
        requestWindowFeature(Window.FEATURE_NO_TITLE);
    
        
        appContext = getBaseContext();
        
         
         setContentView(R.layout.main_menu);
         
//         startButton = (ImageButton)findViewById(R.id.start_button); 
         infoButton =  (ImageButton)findViewById(R.id.info_button); ;
         configButton =  (ImageButton)findViewById(R.id.config_button); ;
         
         SharedPreferences prefs = PreferenceManager
         .getDefaultSharedPreferences(getBaseContext());
         calibrationCompleted = prefs.getBoolean("calibration_completed", false);
         
        
         
         if(!calibrationCompleted){
        	 //TODO disable application running        	
        	 this.sendToast(getString(R.string.toast_calibrate));
         }
        // Initialize the send button with a listener that for click events
	       
         initializeButtons();
         
         establishDirectories();
         
	        Intent uploaderIntent =getPackageManager().getLaunchIntentForPackage("com.luciddreamingapp.uploader");
	        if(uploaderIntent!=null &&prefs.getBoolean("connectionAvailable",false)){
	        	startUploadingTask();
	        }
         
       
    }
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        
    }
    
    
//      private  void startApp(){
//    	  try{     
//    		  if(gesturesEnabled){
//    	            try{
//    	            gestureLibrary= GestureLibraries.fromRawResource(this, R.raw.gestures);
//    	            
//    	            gestures= (GestureOverlayView) findViewById(R.id.gestures);
//    	            //gestures.setVisibility(gestures.INVISIBLE);
//    	         
//    	            gestures.addOnGesturePerformedListener(this);
//    	            gestureLibrary.load();
//    	            if(gesturesEnabled){  gestures.setVisibility(View.VISIBLE);};
//    	            }catch (Exception e){
//    	            	e.printStackTrace();
//    	            }
//    	            } }catch(Exception e){e.printStackTrace();}
//    	        
//    	        dataManager = SleepDataManager.getInstance();
//    	        
//    	       
//    	       phoneAccelerometerHandler =PhoneAccelerometerHandler.getInstance();
//    	       phoneAccelerometerHandler.setDataManager(dataManager);
//    	           	       
//    	       //pass preferences to classes that need them
//    	       injectPreferences();
//    	       
//    	       //TODO move filter handler to a separate activity
//    	     //  filterHandler  =    new FilterHandler();
//    	        
//    	        try{	
//    	        mSensorManager = (SensorManager)getSystemService(Context.SENSOR_SERVICE);
//    	        mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
//    	              
//    	        
//    	      //  mSensorManager.registerListener(phoneAccelerometerHandler, mAccelerometer, SensorManager.SENSOR_DELAY_GAME);
//    	        //TODO remove after testing!
//    	     //   mSensorManager.registerListener(filterHandler, mAccelerometer, SensorManager.SENSOR_DELAY_GAME);
//    	        }catch(Exception e){
//    	        	e.printStackTrace();
//    	        }
//    	        
//    	        
//    	       
//    	      //  wv.addJavascriptInterface(myhandler, "testhandler");
//    	       // listView.addJavascriptInterface(phoneAccelerometerHandler, "javahandler");
//    	        //graphView.addJavascriptInterface(filterHandler, "javahandler");
//    	        //webView.loadUrl("file:///android_asset/html/realtime_sum_single.html");
//    	       // webView.loadUrl("file:///android_asset/html/basic_plot_from_handler.html");
//    	      //  listView.loadUrl("file:///android_asset/html/epoch_activity_list.html");
//    	     //   graphView.loadUrl("file:///android_asset/html/instructions.html");
//    	       // webView.setVisibility(webView.VISIBLE);
//    	        
//    	      
//
//    	        
//    	        
//                listView.setVisibility(View.VISIBLE);
//                graphView.setVisibility(View.GONE);
//               
//        	appStarted = true;
//        	
//        }
    
    
    /* (non-Javadoc)
	 * @see android.app.Activity#onLowMemory()
	 */
	@Override
	public void onLowMemory() {
		// TODO Auto-generated method stub
		super.onLowMemory();
	}

	/* (non-Javadoc)
	 * @see android.app.Activity#onOptionsMenuClosed(android.view.Menu)
	 */
	@Override
	public void onOptionsMenuClosed(Menu menu) {
		// TODO Auto-generated method stub
		super.onOptionsMenuClosed(menu);
	}

	/* (non-Javadoc)
	 * @see android.app.Activity#onRestart()
	 */
	@Override
	protected void onRestart() {
		// TODO Auto-generated method stub
		super.onRestart();
		if(D) Log.e(TAG, "++ ON RESTART ++");
	}

	/* (non-Javadoc)
	 * @see android.app.Activity#onResume()
	 */
	@Override
	protected void onResume() {
		// TODO Auto-generated method stub
		super.onResume();
		
	}

	/* (non-Javadoc)
	 * @see android.app.Activity#onSaveInstanceState(android.os.Bundle)
	 */
	@Override
	protected void onSaveInstanceState(Bundle outState) {
		if(D) Log.e(TAG, "++ ON SAVE INSTANCE STATE ++");
		// TODO Auto-generated method stub
		super.onSaveInstanceState(outState);
	}

	/* (non-Javadoc)
	 * @see android.app.Activity#onStart()
	 */
	@Override
	protected void onStart() {
		
		
		
		// TODO Auto-generated method stub
		super.onStart();
		
		
		if(D) Log.e(TAG, "++ ON START ++");
	}

	/* (non-Javadoc)
	 * @see android.app.Activity#onStop()
	 */
	@Override
	protected void onStop() {
		
		// TODO Auto-generated method stub
		super.onStop();
		 if(D) Log.e(TAG, "-- ON STOP ---");
	}

	@Override
    public void onPause(){
    	 if(D) Log.e(TAG, "+++ ON PAUSE +++");
    	super.onPause();
    	try{
    		if(appStarted){
    			appStarted = false;
    //	mSensorManager.unregisterListener(phoneAccelerometerHandler);
    	//phoneAccelerometerHandler.stopTimer();
    	
    		}
    	}
    	catch(Exception e){}
    }
    
    @Override
    public void onDestroy(){
    	if(D) Log.e(TAG, "+++ ON DESTROY +++");
    	super.onDestroy();
    	try{
    	
    	
    }
    	catch(Exception e){}
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
    
    
    //##################################Menu Section
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);
        return true;
    }
    
       
    

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
       // if(D) {System.out.println("onActivityResult " + resultCode);}
      	
        switch(requestCode){
        case Preferences.REQUEST_SELECT_SMART_TIMER_CONFIG_FILE:
       	 if(resultCode==Activity.RESULT_OK){
    		 //get the config file and save its path
    		 String configFilepath=data.getExtras().getString("filepath");
        		 File f = new File(configFilepath);
     			Toast.makeText(getBaseContext(),
           "Selected: "+configFilepath,
           Toast.LENGTH_LONG).show();
     			
     			 SharedPreferences customSharedPreference = getSharedPreferences(
                         "LucidDreamingAppPreferences", Context.MODE_PRIVATE);
    			      SharedPreferences.Editor editor = customSharedPreference
    			                      .edit();
    			      editor.putString("configFilepath",
    			     		 f.getAbsolutePath());
    			   editor.commit();

    		
    			   
              	Intent smartTimerIntent = new Intent(getBaseContext(), SmartTimerActivity.class);
              	smartTimerIntent.putExtra("eventType", SmartTimerActivity.REM_EVENT);
              	
              	smartTimerIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
      			startActivity( smartTimerIntent);
    			      
     			
     			
    	 }
        	break;
        	
        }
        
    }
    
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {

        
        case R.id.menu_start_app:
        	//sendShortToast(getString(R.string.toast_starting_app));
         	  Intent monitorAccelerometerIntent = new Intent(appContext, NightGUIActivity.class);
         	 monitorAccelerometerIntent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
         	       
               startActivity(monitorAccelerometerIntent);
        	
        	         
        	return true;
        	
        	

        	
        case R.id.menu_preferences:
        	//sendToast("Opening Preferences");
        	Intent settingsActivity = new Intent(getBaseContext(),
                    Preferences.class);
        		settingsActivity.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        		startActivity(settingsActivity);
        	
        
        	return true;
 
            
        case R.id.menu_share_data:
        	
        	//get intent to start the uploader
        final Intent uploaderIntent =getPackageManager().getLaunchIntentForPackage("com.luciddreamingapp.uploader");
        	
        	AlertDialog.Builder builder = new AlertDialog.Builder(this);
        	//getString(R.string.dialog_math_puzzle)
        	//TODO pull out the text
	    	   
        	 builder.setCancelable(true);
        	
        	if(uploaderIntent ==null){
        		//get from market
        	builder.setMessage("To contribute data to science, you need a small data uploader app from market. Get it from market now?");
      	 	           		
        		
        		
  	 	       builder.setPositiveButton(getString(R.string.dialog_yes), new DialogInterface.OnClickListener() {
  	 	    	
  	 	           public void onClick(DialogInterface dialog, int id) {
  	 	        
  	 	        	final Intent intent = new Intent(Intent.ACTION_VIEW);
  	 	        	intent.setData(Uri.parse("market://details?id=com.luciddreamingapp.uploader"));
  	 	        	startActivity(intent);
  	 	        	  
  	 	           }
  	 	       });
        		
        	}else{
        		builder.setMessage("Anonymously contribute sleep data to science?");
        		  builder.setPositiveButton(getString(R.string.dialog_yes), new DialogInterface.OnClickListener() {
        	 	    	
     	 	           public void onClick(DialogInterface dialog, int id) {
     	 	        	   Intent intent = new Intent(LucidDreamingApp.this,AutomaticUploaderService.class);
     	 	        	   intent.putExtra("automatic", false);
     	 	        	startService(intent);
     	 	        	
//     	 	        	 Intent intent = new Intent("com.luciddreamingapp.uploader.START_UPLOAD");
//     	 	        	 sendBroadcast(intent);
     	 	           }
     	 	       });
        	}
        	
        	    
	 	      builder.setNegativeButton(getString(R.string.dialog_no), new DialogInterface.OnClickListener() {
	 	           public void onClick(DialogInterface dialog, int id) {
	 	        	  dialog.cancel();
	 	           }
	 	       });
		    	
		    	//builder.setView(layout);
		    	AlertDialog alertDialog = builder.create();
		    	alertDialog.show();
        	

        	
        	return true;

        case R.id.menu_load_data:
        	sendShortToast(getString(R.string.toast_starting_graph_viewer));
        	Intent graphingActivity = new Intent(getBaseContext(),
                    GraphViewingActivity.class);
        	graphingActivity.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        		startActivity(graphingActivity);
        	
        	
        	return true;
        	
        //saves the currently displayed graph as a JSON File	
       
//        	
//        case R.id.menu_share_data:
//        	sendToast("Feature not implemented yet");	
//        	       	
//        	return true;
        
        case R.id.menu_help:
        	//TODO move back to help before next release
        	
        	Intent helpIntent = new Intent(getBaseContext(),
                    HelpActivity.class);
        	
//        	Intent helpIntent = new Intent(getBaseContext(),
//                    TestActivity.class);
        		startActivity(helpIntent);
        		return true;
        	
        
        
        	
        	
        case R.id.menu_exit:
        	
	       	finish();
        	return true;
        
        }
  
        return false;
    }
    
    
    
    private void initializeButtons(){
	      
        //defines the listener as a parameter passed to the method
//       startButton.setOnClickListener(new OnClickListener() {
//            @Override
//			public void onClick(View v) {
//            	 //establish the plotting view
//    	   
//            	
//            	sendShortToast(getString(R.string.toast_starting_app));
//         	  Intent monitorAccelerometerIntent = new Intent(appContext, NightGUIActivity.class);
//         	   monitorAccelerometerIntent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT); 
//         	
//               startActivity(monitorAccelerometerIntent);
//            	
//             }
//            });
       			
 
       
       //defines the listener as a parameter passed to the method
       infoButton.setOnClickListener(new OnClickListener() {
            @Override
			public void onClick(View v) {
            	
            	Intent intent = new Intent(LucidDreamingApp.this,QuickstartActivity.class);
            	startActivity(intent);

             }
            });
       			
       
       //defines the listener as a parameter passed to the method
       configButton.setOnClickListener(new OnClickListener() {
            @Override
			public void onClick(View v) {
             	AlertDialog.Builder builder = new AlertDialog.Builder(LucidDreamingApp.this);
            	//getString(R.string.dialog_math_puzzle)
            	//TODO pull out the text
    	    	   
            	 builder.setCancelable(true);
            	
            	builder.setMessage("Would you like to apply easy config settings?\n" +
            			"Enable Reminders, gestures and Smart Timer\n" +            			
            			"Screen Brightness: 1%\n" +
            			"Open and edit Smart Timer config"
            			);
            	builder.setPositiveButton(getString(R.string.dialog_yes), new DialogInterface.OnClickListener() {
            	 	    	
         	 	           public void onClick(DialogInterface dialog, int id) {
         	 	        
         	 	        	sendShortToast("config positive");
         	 	        SharedPreferences prefs;
         	 	    
         	 	      	
         	 	        	prefs = PreferenceManager
         	 	          .getDefaultSharedPreferences(getBaseContext());
         	 	        	
         	 	        	Editor editor = prefs.edit();
         	 	        	
         	 	        	//enable sound
         	 	        	editor.putBoolean("play_sound_pref", true);
         	 	        	editor.putBoolean("enable_smart_timer", true);
         	 	        	editor.putBoolean("enable_gestures", true);
         	 	        	editor.putBoolean("enable_screen_brightness_adjustment", true);
         	 	        	editor.putInt("brightness_level", 1);
         	 	      	editor.commit();
         	 	  	
         	 	     Intent selectFileIntent = new Intent(getBaseContext(), DataFileSelector.class);
         			 selectFileIntent.putExtra("filepath", LucidDreamingApp.APP_HOME_FOLDER);
         			 //match smart followed or preceeded by anything with an extension of txt or txt.gzip
         			 selectFileIntent.putExtra("filterString","(.)*?[Ss]mart(.)*(txt)([.]gzip)??");
         			 startActivityForResult(selectFileIntent, Preferences.REQUEST_SELECT_SMART_TIMER_CONFIG_FILE);
         		           
         	 	        	  
         	 	           }
         	 	       });
            	
            	
            	    
    	 	      builder.setNegativeButton(getString(R.string.dialog_no), new DialogInterface.OnClickListener() {
    	 	           public void onClick(DialogInterface dialog, int id) {
    	 	        	  dialog.cancel();
    	 	           }
    	 	       });
    		    	
    		    	//builder.setView(layout);
    		    	AlertDialog alertDialog = builder.create();
    		    	alertDialog.show();
            	           	
             }
            });
       			
       
       
       
    }//end initialize buttons
    
    
    
  //preferences
//    boolean playSoundReminders;
//    boolean playLateSoundReminders;
//    int deep_sleep_120_earliest;
//    int deep_sleep_120_latest;
//    int deep_sleep_150_earliest;
//    int deep_sleep_150_latest;
//    int deep_sleep_180_earliest;
//    int deep_sleep_180_latest;
//  
//    int number_of_reminders;
//    String reminder_filepath;
//    float coleConstant;
//    int sensorCalibrationDuration;
//    float xCalibratedVariance,xCalibratedStDev, yCalibratedVariance, yCalibratedStDev,
//    zCalibratedVariance ,zCalibratedStDev ,magnitudeCalibratedVariance, magnitudeCalibratedStDev ,epochXYZCountStDev, epochXYZCountMean,
//    epochXYZSumStDev, epochXYZSumMean ,coleConstantLowThreshold, coleConstantMediumThreshold ,
//    coleConstantHighThreshold ,coleConstantVeryHighThreshold,thresholdLowSensitivity, thresholdMediumSensitivity , thresholdHighSensitivity, thresholdVeryHighSensitivity;
//    
//   
//    
//    float xStep,yStep,zStep;
  
//    float xSensitivity,ySensitivity,zSensitivity;
       boolean calibrationCompleted;   
    
    private void getPrefs() {
//    	
//    	float defValue = 0.01F;
//        // Get the xml/preferences.xml preferences
//        SharedPreferences prefs = PreferenceManager
//                        .getDefaultSharedPreferences(getBaseContext());
//        playSoundReminders = prefs.getBoolean("play_sound_pref", false);
//        playLateSoundReminders= prefs.getBoolean("play_sound_pref_non_rem", false);
//        
//        deep_sleep_120_earliest = prefs.getInt("deep_sleep_120_earliest", 30);
//        deep_sleep_120_latest  = prefs.getInt("deep_sleep_120_latest", 45);
//        
//        deep_sleep_150_earliest = prefs.getInt("deep_sleep_150_earliest", 20);
//        deep_sleep_150_latest  = prefs.getInt("deep_sleep_150_latest", 30);
//        
//        deep_sleep_180_earliest = prefs.getInt("deep_sleep_180_earliest", 10);
//        deep_sleep_180_latest  = prefs.getInt("deep_sleep_180_latest", 15);
//        
//        number_of_reminders  = prefs.getInt("number_of_reminders", 1);
//        
//        sensorCalibrationDuration= prefs.getInt("sensorCalibrationDuration", 60);
//        
//        try{
//        xSensitivity = prefs.getFloat("x_sensitivity_pref", 0.00005F);
//        ySensitivity = prefs.getFloat("y_sensitivity_pref", 0.00001F);
//        zSensitivity = prefs.getFloat("z_sensitivity_pref", 0.0001F);
//        }catch (Exception e){
//        	if(D) e.printStackTrace();
//        }
//      
//        
//        if(sensorCalibrationDuration<1){sensorCalibrationDuration=1;}
//        
//        
//        
//        
//        try{coleConstant  = prefs.getFloat("deep_sleep_180_latest", 15);
//        if(!validateColeConstant(coleConstant)){coleConstant = SleepAnalyzer.getColeConstant();}
//        }
//        catch(Exception e){
//        	sendToast("Error loading Sleep Scoring Sensitivity, using default value");
//        	coleConstant = SleepAnalyzer.getColeConstant();
//        }
//         
//        // Get the custom preference
//        SharedPreferences mySharedPreferences = getSharedPreferences(
//                        "LucidDreamingAppPreferences", Activity.MODE_PRIVATE);
//        reminder_filepath = mySharedPreferences.getString("reminderFilepath", "");
//        
//        
//        xStep=mySharedPreferences.getFloat("xStep", 0.04086106F);
//        yStep=mySharedPreferences.getFloat("yStep", 0.04086106F);
//        zStep=mySharedPreferences.getFloat("zStep", 0.04086106F);
//        calibrationCompleted = mySharedPreferences.getBoolean("calibrationCompleted", false);  
//        
//        xCalibratedVariance = mySharedPreferences.getFloat("xCalibratedVariance", 27.4215981975949F);
//        xCalibratedStDev = mySharedPreferences.getFloat("xCalibratedStDev", 5.236563587F);
//      
//
//        yCalibratedVariance = mySharedPreferences.getFloat("yCalibratedVariance", 0.001048004F);
//        yCalibratedStDev = mySharedPreferences.getFloat("yCalibratedVariance", 0.032372889F);
//        
//        zCalibratedVariance = mySharedPreferences.getFloat("zCalibratedVariance", 0.07498645f);
//        zCalibratedStDev = mySharedPreferences.getFloat("zCalibratedStDev", 0.005622968f);
//        magnitudeCalibratedVariance = mySharedPreferences.getFloat("magnitudeCalibratedVariance", defValue);
//        magnitudeCalibratedStDev = mySharedPreferences.getFloat("magnitudeCalibratedStDev", defValue);
//        
//        epochXYZCountStDev = mySharedPreferences.getFloat("epochXYZCountStDev", defValue);
//        epochXYZCountMean = mySharedPreferences.getFloat("epochXYZCountMean", defValue);
//        
//        epochXYZSumStDev = mySharedPreferences.getFloat("epochXYZSumStDev", defValue);
//        epochXYZSumMean = mySharedPreferences.getFloat("epochXYZSumMean", defValue);
//     
//        coleConstantLowThreshold = mySharedPreferences.getFloat("coleConstantLowThreshold", 0.0232160459894959F);
//        coleConstantMediumThreshold = mySharedPreferences.getFloat("coleConstantMediumThreshold", 0.018387991F);
//        coleConstantHighThreshold = mySharedPreferences.getFloat("coleConstantVeryHighThreshold", 0.015222325F);
//        coleConstantVeryHighThreshold = mySharedPreferences.getFloat("coleConstantVeryHighThreshold", 0.012986566F);
//        
//        thresholdLowSensitivity = mySharedPreferences.getFloat("thresholdLowSensitivity", 6.476593918f);
//        thresholdMediumSensitivity = mySharedPreferences.getFloat("thresholdMediumSensitivity", 8.177125224f);
//        thresholdHighSensitivity = mySharedPreferences.getFloat("thresholdHighSensitivity", 9.87765653f);
//        thresholdVeryHighSensitivity = mySharedPreferences.getFloat("thresholdVeryHighSensitivity", 11.57818784f);
//    
//        if(D)Log.w(TAG, ""+deep_sleep_120_earliest);
//        if(D)Log.w(TAG, ""+deep_sleep_120_latest);
//        if(D)Log.w(TAG, ""+deep_sleep_150_earliest);
//        if(D)Log.w(TAG, ""+deep_sleep_150_latest);
//        if(D)Log.w(TAG, ""+deep_sleep_180_earliest);
//        if(D)Log.w(TAG, ""+deep_sleep_180_latest);
//        if(D)Log.w(TAG, ""+coleConstant);
//        if(D)Log.w(TAG, "optimized cole constant"+coleConstantVeryHighThreshold);        
//       
//        
//        if(D)Log.w(TAG, "coleConstantLowThreshold "+coleConstantLowThreshold);
//        if(D)Log.w(TAG, "coleConstantMediumThreshold "+coleConstantMediumThreshold);
//        if(D)Log.w(TAG, "coleConstantHighThreshold "+coleConstantHighThreshold);        
//        if(D)Log.w(TAG, "coleConstantVeryHighThreshold "+coleConstantVeryHighThreshold);
//        
//        if(D)Log.w(TAG, "thresholdLowSensitivity "+thresholdLowSensitivity);
//        if(D)Log.w(TAG, "thresholdMediumSensitivity "+thresholdMediumSensitivity);
//        if(D)Log.w(TAG, "thresholdHighSensitivity "+thresholdHighSensitivity);            
//        if(D)Log.w(TAG, "thresholdVeryHighSensitivity "+thresholdVeryHighSensitivity);
//        
////        
////        if(D)Log.w(TAG, "xCalibratedVariance "+xCalibratedVariance);
////        if(D)Log.w(TAG, "xCalibratedStDev "+xCalibratedStDev);
////        
////        if(D)Log.w(TAG, "yCalibratedVariance "+yCalibratedVariance);
////        if(D)Log.w(TAG, "yCalibratedStDev "+yCalibratedStDev);
////        
////        if(D)Log.w(TAG, "zCalibratedVariance "+zCalibratedVariance);
////        if(D)Log.w(TAG, "zCalibratedStDev "+zCalibratedStDev);
}
    
   
    private void establishDirectories(){
    	//trying to solve null pointer exception when (probably) SDCard is not present
    	boolean errorMessageDelivered = false;
    	try{
    	try{
    	File logDir = new File(Environment.getExternalStorageDirectory(),LOG_LOCATION);
    	if(!logDir.exists()){
    		logDir.mkdirs();
    		
    	}}catch(NullPointerException e){
    		if(!errorMessageDelivered){
    			sendToast(getString(R.string.toast_sd_card_not_found));
    			errorMessageDelivered=true;
    		}
    	}
    	try{
    	File graphDir = new File(Environment.getExternalStorageDirectory(),GRAPH_DATA_LOCATION);
    	if(!graphDir.exists()){
    		graphDir.mkdirs();
    		if(graphDir.list().length<1){
    			try{copyFileTo(new File(graphDir+"/Data_sample_20110429_0020.txt.gzip"),
    					getResources().openRawResource(R.raw.data_sample));}
    			catch(Exception e){if(D)e.printStackTrace();}
    		}
    		
    	}}catch(NullPointerException e){
    		if(!errorMessageDelivered){
    			sendToast(getString(R.string.toast_sd_card_not_found));
    			errorMessageDelivered=true;
    		}
    	}
    	try{
    	File gesturesDir = new File(Environment.getExternalStorageDirectory(),GESTURES_LOCATION);
    	if(!gesturesDir.exists()){
    		gesturesDir.mkdirs();
    		
    	}}catch(NullPointerException e){
    		if(!errorMessageDelivered){
    			sendToast(getString(R.string.toast_sd_card_not_found));
    			errorMessageDelivered=true;
    		}
    	}
    	
    	try{
    	File recordingsDir = new File(Environment.getExternalStorageDirectory(),MP3_LOCATION);
    	if(!recordingsDir.exists()){
    		recordingsDir.mkdirs();
    		
    	}}catch(NullPointerException e){
    		if(!errorMessageDelivered){
    			sendToast(getString(R.string.toast_sd_card_not_found));
    			errorMessageDelivered=true;
    		}
    	}
    	
    	try{
    	File historyDir = new File(Environment.getExternalStorageDirectory(),HISTORY_DATA_LOCATION);
    	if(!historyDir.exists()){
    		historyDir.mkdirs();
    		if(historyDir.list().length<1){
    			try{copyFileTo(new File(historyDir+"/Hist_sample.txt.gzip"),
    					getResources().openRawResource(R.raw.hist_sample));}
    			catch(Exception e){if(D)e.printStackTrace();}
    		}
    		
    	}}catch(NullPointerException e){
    		if(!errorMessageDelivered){
    			sendToast(getString(R.string.toast_sd_card_not_found));
    			errorMessageDelivered=true;
    		}
    	}
    	
    	try{
    		File homeDir = new File(Environment.getExternalStorageDirectory(),APP_HOME_FOLDER);
    		File configFile = new File(homeDir+"/BlankSmartTimerConfig.txt.gzip");
    		//copy the config file if it does not exist
    		if(!configFile.exists()){
    		copyFileTo(configFile,
					getResources().openRawResource(R.raw.smart_timer_config_txt_gzip));
    		}
    	}catch(NullPointerException e){
    		if(!errorMessageDelivered){
    			sendToast(getString(R.string.toast_sd_card_not_found));
    			errorMessageDelivered=true;
    		}
    	}
    	
    	try{
    		File homeDir = new File(Environment.getExternalStorageDirectory(),APP_HOME_FOLDER);
    		File configFile = new File(homeDir+"/SmartTimerConfig.txt.gzip");
    		//copy the config file if it does not exist
    		if(!configFile.exists()){
    		copyFileTo(configFile,
					getResources().openRawResource(R.raw.smart_timer_config_alt));
    		}
    	}catch(NullPointerException e){
    		if(!errorMessageDelivered){
    			sendToast(getString(R.string.toast_sd_card_not_found));
    			errorMessageDelivered=true;
    		}
    	}
    	
    	try{
    		File homeDir = new File(Environment.getExternalStorageDirectory(),APP_HOME_FOLDER);
    		File configFile = new File(homeDir+"/WILDTimerConfig.txt.gzip");
    		//copy the wild timer config file if it does not exist
    		if(!configFile.exists()){
    		copyFileTo(configFile,
					getResources().openRawResource(R.raw.wild_timer_default_config));
    		}
    	}catch(NullPointerException e){
    		if(!errorMessageDelivered){
    			sendToast(getString(R.string.toast_sd_card_not_found));
    			errorMessageDelivered=true;
    		}
    	}
    	
    	try{
        	File gesturesDir = new File(Environment.getExternalStorageDirectory(),UPLOAD_LOCATION);
        	if(!gesturesDir.exists()){
        		gesturesDir.mkdirs();
        		
        	}}catch(NullPointerException e){
        		if(!errorMessageDelivered){
        			sendToast(getString(R.string.toast_sd_card_not_found));
        			errorMessageDelivered=true;
        		}
        	}
    	
    	
    	try{
        	File gesturesDir = new File(Environment.getExternalStorageDirectory(),GESTURES_LOCATION);
        	if(!gesturesDir.exists()){
        		gesturesDir.mkdirs();
        		
        	}}catch(NullPointerException e){
        		if(!errorMessageDelivered){
        			sendToast(getString(R.string.toast_sd_card_not_found));
        			errorMessageDelivered=true;
        		}
        	}
        	
        	try{
            	File screenshotsDir = new File(Environment.getExternalStorageDirectory(),SCREENSHOT_LOCATION);
            	if(!screenshotsDir.exists()){
            		screenshotsDir.mkdirs();
            		
            	}}catch(NullPointerException e){
            		if(!errorMessageDelivered){
            			sendToast(getString(R.string.toast_sd_card_not_found));
            			errorMessageDelivered=true;
            		}
            	}
    	
    	try{
    		File htmlDir = new File(Environment.getExternalStorageDirectory(),HTML_DATA_LOCATION);
    		if(!htmlDir.exists()){
    			htmlDir.mkdirs();
        		
        	}
    		
    		File strobeBright = new File(htmlDir+"/strobe_bright.html");
    		File strobeBrightImage = new File(htmlDir+"/lightning-bright.jpg");
    		
    		File strobeDark = new File(htmlDir+"/strobe_dark.html");
    		File strobeDarkImage = new File(htmlDir+"/lightning-dark.jpg");
    		//copy the wild timer config file if it does not exist
    		if(!strobeBright.exists()){
    		copyFileTo(strobeBright,
					getResources().openRawResource(R.raw.strobe_bright_html));
    		}
    		if(!strobeBrightImage.exists()){
        		copyFileTo(strobeBrightImage,
    					getResources().openRawResource(R.raw.lightning_bright_jpg));
        		}
    		if(!strobeDark.exists()){
        		copyFileTo(strobeDark,
    					getResources().openRawResource(R.raw.strobe_dark_html));
        		}
    		if(!strobeDarkImage.exists()){
        		copyFileTo(strobeDarkImage,
    					getResources().openRawResource(R.raw.lightning_dark_jpg));
        		}
    		
    		
    	}catch(NullPointerException e){
    		if(!errorMessageDelivered){
    			sendToast(getString(R.string.toast_sd_card_not_found));
    			errorMessageDelivered=true;
    		}
    	}
    	
    	try{
    		File homeDir = new File(Environment.getExternalStorageDirectory(),APP_HOME_FOLDER);
    		File configFile = new File(homeDir+"/WILDTimerReentryConfig.txt.gzip");
    		//copy the wild timer config file if it does not exist
    		if(!configFile.exists()){
    		copyFileTo(configFile,
					getResources().openRawResource(R.raw.wild_timer_reentry_config));
    		}
    	}catch(NullPointerException e){
    		if(!errorMessageDelivered){
    			sendToast(getString(R.string.toast_sd_card_not_found));
    			errorMessageDelivered=true;
    		}
    	}
    	
    	
    	
    	try{
       	if(!GestureBuilderActivity.GESTURES_FILE.exists()){
       		try{
       		GestureBuilderActivity.DIRECTORY.mkdirs();
       		copyFileTo(GestureBuilderActivity.GESTURES_FILE,getResources().openRawResource(R.raw.lucid_dreaming_app_default_gestures));
       		}catch(Exception e){if(D)e.printStackTrace();}
    	}}catch(NullPointerException e){
    		if(!errorMessageDelivered){
    			sendToast(getString(R.string.toast_sd_card_not_found));
    			errorMessageDelivered=true;
    		}
    	}
    	if(errorMessageDelivered){
    		sendToast(getString(R.string.toast_sd_card_not_found_disable_logging));
    	}
    	}catch(Exception e){
    		if(D)e.printStackTrace();
    	}
    
    }
    

    
	
	private boolean copyFileTo(File filepath,InputStream input){
		
		try{	
            //read a raw resource and copy it on sd card
		  InputStream inputStream =input; 
	      BufferedInputStream in2 = new BufferedInputStream(inputStream);
	     
	      //file to create
	     FileOutputStream out = new FileOutputStream(filepath);
         BufferedOutputStream out2 = new BufferedOutputStream(out);
        int chunk;
            while ((chunk = in2.read()) != -1) {
                    out2.write(chunk);
            }
          
            in2.close();
            inputStream.close();
            out2.close();
            out.close();
        return true;    
		}
	     catch(IOException e){
	    	 if((D))e.printStackTrace();
	    	 return false;
	     }
	    
	    }
	
    

	
   private boolean validateColeConstant(float input){
	   if(input >0 && input <1){
		   return true;
	   }else return false;
   }
   
   
   
   private void startUploadingTask(){
	   try{
	   new AutomaticUploadTask().execute(null);
	   }catch(Exception e){
		   if(D) e.printStackTrace();
	   }
   }
	 
	 private class AutomaticUploadTask extends AsyncTask<long[], Void, Void> {
	     protected Void doInBackground(long[]... urls) {
	        if(D)Log.e(TAG, "Automatic uploader running");
	        try{
	        	
	        	//if uploader is not installed, do nothing

	        	
	        	
	        SharedPreferences   prefs = PreferenceManager
	        .getDefaultSharedPreferences(getBaseContext());
	        
	        Editor editor = prefs.edit();
	        int weekDay =  Calendar.getInstance().get(Calendar.DAY_OF_WEEK);
	        editor.putInt("uploadDay", weekDay);
	        editor.putBoolean("connectionAvailable", false);
	        editor.commit();
	       if(D)Log.e(TAG, "Automatic uploader running, weekday to upload: "+(prefs.getInt("uploadDay", (int)(Math.random()*7+1)))+" today: "+weekDay);
	       //if there's an internet connection, today is the upload day and the 
	       //user consented to data upload, try automatic upload
	       if(
	    		  // ((GlobalApp)getApplication()).getConnectionStatus()
	    		   weekDay ==  (prefs.getInt("uploadDay", (int)(Math.random()*7+1)))
	    		  && prefs.getBoolean("data_upload_pref", true)
	    		 ){
	    	   		Intent intent = new Intent(LucidDreamingApp.this,AutomaticUploaderService.class);
			 	   intent.putExtra("automatic", true); 
			 	   
			 	  //catches activity not found exceptions
			 	   try {
					startService(intent);
				} catch (Exception e) {
					
					if(D)e.printStackTrace();
				}
			 	 
	    		 }
	    		 
	        }catch(Exception e){
	        	if(D)e.printStackTrace();
	        }
	         return null;
	     }

	     protected void onPostExecute() {
	    		sendShortToast("Automatic Upload completed");
	     }
	 }

	   
	   
}

