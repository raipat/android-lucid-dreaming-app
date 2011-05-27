package com.luciddreamingapp.beta;

import java.io.File;
import java.io.IOException;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

import com.luciddreamingapp.beta.util.ColorPickerDialog;
import com.luciddreamingapp.beta.util.gesturebuilder.GestureBuilderActivity;
import com.luciddreamingapp.beta.util.state.SmartTimerActivity;


public class Preferences extends PreferenceActivity  implements ColorPickerDialog.OnColorChangedListener{
	public static final boolean D = false;
	public static final String TAG = "Lucid dreaming App Preferences";
	public static final int REQUEST_SELECT_FILE = 1;
	public static final int REQUEST_SELECT_FILE_BUILT_IN = 2;
	
	public static final int REQUEST_SELECT_SMART_TIMER_CONFIG_FILE = 3;
	public static final int REQUEST_SELECT_WILD_TIMER_CONFIG_FILE = 4;
	
	private MediaPlayer mp;
	
	private ColorPickerDialog.OnColorChangedListener colorListener;
	
	private Context context;
	private Context thisContext;
	
        @Override
        protected void onCreate(Bundle savedInstanceState) {
        	mp = new MediaPlayer();
                super.onCreate(savedInstanceState);
              
                addPreferencesFromResource(R.xml.preferences);
                colorListener= this;
                context = this.getApplicationContext();
                thisContext = this;//otherwise showing color picker dialog fails!
                // Get the custom preference
                Preference customPref = findPreference("filePicker");
                customPref
                                .setOnPreferenceClickListener(new OnPreferenceClickListener() {
 
                                        @Override
										public boolean onPreferenceClick(Preference preference) {
                                                
                                        	
                                        	SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
                                        	boolean useFileSelection = prefs.getBoolean("old_file_selection_pref", false);
                                        	
                                        	Intent oiManagerExists =getPackageManager().getLaunchIntentForPackage("org.openintents.filemanager");
                                         	
                                        	if(oiManagerExists==null ||useFileSelection){
                                        		//if the user is having trouble opening MP3s through URIs, use the default file selector
                                        		try{
                                        		Intent selectFileIntent = new Intent(context, Mp3FileSelector.class);
                                                 startActivityForResult(selectFileIntent, REQUEST_SELECT_FILE_BUILT_IN);
                                        		}catch(Exception e){
                                        			if(D)e.printStackTrace();
                                        		}
                                        		
                                        	}else{
                                        		//give the user more freedom to pick any URI
                                        		Intent intent = new Intent("org.openintents.action.PICK_FILE");
                                            	intent.putExtra("org.openintents.extra.TITLE", "Pick reminder .Mp3, .Mp4 or .Wav file");
                                            	intent.putExtra("org.openintents.extra.BUTTON_TEXT", "Pick");
                                            	
                                            	intent.setData(Uri.parse("file:///sdcard/Recordings"));
                                            	try{
                                            		startActivityForResult(intent, REQUEST_SELECT_FILE);
                                            		//sendShortToast("Opening file manager");
                                            		}
                                            	catch(ActivityNotFoundException e){
                                            		
//                                            		AlertDialog.Builder builder = new AlertDialog.Builder(context);
//                                            		builder.setMessage("OI File manager not found. Would you like to install it from Android Market? You can always use the built-in funtion to select files")
//                                            		       .setCancelable(true)
//                                            		       .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
//                                            		           public void onClick(DialogInterface dialog, int id) {
//                                            		        	   
//                                            		        	 Intent  getOIFileManager = new Intent(Intent.ACTION_VIEW);
//                                                           		getOIFileManager.setData(Uri.parse("market://details?id=org.openintents.filemanager"));
//                                                           		//sendToast("Please install OI FIle manager from market");                                        		
//                                            		        	   
//                                            		        	   finish();
//                                            		           }
//                                            		       })
//                                            		       .setNegativeButton("No", new DialogInterface.OnClickListener() {
//                                            		           public void onClick(DialogInterface dialog, int id) {
//                                            		                dialog.cancel();
//                                            		           }
//                                            		       });
//                                            		builder.create().show();
                                            		
                                            		sendToast("OI File Manager not found, please use built-in file selector");
                                            		if(D)e.printStackTrace();
                                            	}catch(Exception e){
                                            		sendToast("Exception working with IO File Manager");
                                            		if(D)e.printStackTrace();
                                            	}
                                        	}
                                        	                                      	                                      	
                                        	                                                
                                          return true;
                                        }
 
                                });
                
              
                
                
                Preference playReminderPref = findPreference("play_reminder_file_pref");
                playReminderPref
                                .setOnPreferenceClickListener(new OnPreferenceClickListener() {
 
                                        @Override
										public boolean onPreferenceClick(Preference preference) {
                                               //stop media player if the user clicks the preference again while it is playing 
                                        	
                                        	
                                        	if(mp!= null &&mp.isPlaying()){//stop playback
                                        		try{
                                        			sendShortToast("Stopping playback");
                                        		mp.stop();
                                        		mp.release();
                                        		mp = null;
                                        		}catch(Exception e){}
                                        	}else{
                                        		
                                        		SharedPreferences mySharedPreferences = getSharedPreferences(
                                                    "LucidDreamingAppPreferences", Context.MODE_PRIVATE);
                                    	String mp3reminderFileUri = mySharedPreferences.getString("reminderFileUri", "/sdcard/Recordings/lucid.mp3");
                                    	String mp3reminderFilepath = mySharedPreferences.getString("reminderFilepath", "/Recordings/lucid.mp3");
                                    	
                                    	//determine if the user wants to select files using filepaths rather than URIs
                                    	SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
                                    	boolean useFileSelection = prefs.getBoolean("old_file_selection_pref", false);
                                    	
                                    	
                                      
                                        		mp = new MediaPlayer();
                                        		//depending on the way the user picked an audio file
                                        		if(useFileSelection){
                                        			
                                        			//file selection using absolute filepaths
                                        		  	try{
                                        			sendToast("playing: "+mp3reminderFilepath); //absolute path to file
                                        			mp.setDataSource(mp3reminderFilepath);
                                        			mp.prepare();
                                            		mp.start();}
                                        		  	catch(IOException e){ 
                                        		  		if(D)e.printStackTrace();
                                        		  	File dataFile = new File(mp3reminderFilepath);
                                        				try{
                                        				if(dataFile.canRead() && dataFile.exists()){
                                        					sendToast("File "+mp3reminderFilepath+" exists and can be read, but cannot be played. Wrong format? ");
                                        				}}catch(SecurityException ex){
                                        					if(D)ex.printStackTrace();
                                        					//in case the user does not have permissions?
                                        					sendToast("App does not have permissions to access "+mp3reminderFilepath);}
                                        			
                                        				if(!dataFile.exists()){
                                        					sendToast(mp3reminderFilepath+" does not exist");
                                        				}	
                                        		  		
                                        		  	}           
                                        		  	catch(IllegalArgumentException e){
                                        		  		if(D)e.printStackTrace();
                                        		  		sendToast("Incorrect path: "+mp3reminderFilepath);
                                            			
                                        			}	//end exception handling
                                        			catch(Exception e){
                                        				if(D)e.printStackTrace();
                                        			}	//end exception handling
                                        					
                                        				
                                        			
                                        		}else{
                                        			//file selection using Uris
                                        			try{
                                        			sendToast("playing: "+mp3reminderFileUri);
                                        			mp.setDataSource(mp3reminderFileUri);
                                        			mp.prepare();
                                        			mp.start();
                                        			if(D)Log.w(TAG,((mp3reminderFileUri)));
                                        			}
                                        			catch(IOException e){
                                        				//File dataFile = new File(URI.create((mp3reminderFileUri)));
                                        				File dataFile = new File(mp3reminderFileUri);
                                        				try{
                                        				if(dataFile.canRead() && dataFile.exists()){
                                        					sendToast("File exists and can be read, but cannot be played. Wrong format?");
                                        				}}catch(SecurityException ex){
                                        					if(D)ex.printStackTrace();
                                        					//in case the user does not have permissions?
                                        					sendToast("App does not have permissions to access "+mp3reminderFileUri);}
                                        			
                                        				if(!dataFile.exists()){
                                        					sendToast(mp3reminderFileUri+" does not exist");
                                        				}	
                                        			}catch(IllegalArgumentException e){
                                        				if(D)e.printStackTrace();
                                        				sendToast("Incorrect path: "+mp3reminderFilepath);
                                        			}catch(Exception e){
                                        				if(D)e.printStackTrace();
                                        				
                                        			}
                                        		}
                                        		  
                                      
                                    		
                                    	}	   //end else if not playing                                     		
                                        		
                                        	return true;
                                        	}//end on preference click
                                                           
                                    	 
 
                                });
                
                
                Preference getMP3RecorderPref = findPreference("get_voice_recorder_pref");
                getMP3RecorderPref
                                .setOnPreferenceClickListener(new OnPreferenceClickListener() {
 
                                        @Override
										public boolean onPreferenceClick(Preference preference) {
                                        	//default intent is to get from market
                                        	Intent getStartRecorder;
                                        	
                                    		
                                    		//
                                    		//if it is installed, start the app
                                        	try{
                                        		getStartRecorder =	getPackageManager().getLaunchIntentForPackage("yuku.mp3recorder.lite");
                                        		
                                        		//check if the lite version is not present
                                        		if(getStartRecorder ==null){
                                        			
                                        			//check for full version
                                        			getStartRecorder =	getPackageManager().getLaunchIntentForPackage("yuku.mp3recorder.full");
                                        			if(getStartRecorder ==null){
                                        			
                                        				//get it from market
                                        			getStartRecorder = new Intent(Intent.ACTION_VIEW);
                                            		getStartRecorder.setData(Uri.parse("market://details?id=yuku.mp3recorder.lite"));
                                            		sendToast("Please install HI-Q MP3 Recorder from market");                                        				
                                        			}
                                        			
                                        		}else{
                                        			//start the file normally
                                        			sendToast("Starting HI-Q MP3 Recorder");
                                        		}
                                        		
                                        	}catch(Exception e){
                                        		e.printStackTrace();
                                        		getStartRecorder = new Intent(Intent.ACTION_VIEW);
                                        		getStartRecorder.setData(Uri.parse("market://details?id=yuku.mp3recorder.lite"));
                                        		
                                        	}
                                        	
                                        	
                                			startActivity(getStartRecorder);
                                        	
                                           return true;
                                        }
 
                                });
                
                Preference sleepCycleInstructions = findPreference("sleep_cycle_rem_instructions");
                sleepCycleInstructions
                                .setOnPreferenceClickListener(new OnPreferenceClickListener() {
 
                                        @Override
										public boolean onPreferenceClick(Preference preference) {
                                        	
                                        	Uri uri = Uri.parse( "http://luciddreamingapp.com/help-how-to/detect-sleep-cycles/" );
                                			startActivity( new Intent( Intent.ACTION_VIEW, uri ) );
                                        	
                                           return true;
                                        }
 
                                });
                
                Preference clockColorPreference = findPreference("clock_color_preference");
                clockColorPreference
                                .setOnPreferenceClickListener(new OnPreferenceClickListener() {
 
                                        @Override
										public boolean onPreferenceClick(Preference preference) {
                                        	
                                        	SharedPreferences prefs = PreferenceManager
                                        	.getDefaultSharedPreferences(getBaseContext());
                                        	
                                        	
                                        	new ColorPickerDialog(thisContext, colorListener, prefs.getInt("clockColor", Color.GREEN)).show();
                                        	
                                           return true;
                                        }
 
                                });
                
                
                Preference switchSmartTimerConfig = findPreference("switch_smart_timer_config");
                switchSmartTimerConfig
                                .setOnPreferenceClickListener(new OnPreferenceClickListener() {
 
                                        @Override
										public boolean onPreferenceClick(Preference preference) {
                                                
                                        	
                                        
                                        	 Intent selectFileIntent = new Intent(context, DataFileSelector.class);
                                      		 selectFileIntent.putExtra("filepath", LucidDreamingApp.APP_HOME_FOLDER);
                                      		 //match smart followed or preceeded by anything with an extension of txt or txt.gzip
                                      		 selectFileIntent.putExtra("filterString","(.)*?[Ss]mart(.)*(txt)([.]gzip)??");
                                      		 startActivityForResult(selectFileIntent, REQUEST_SELECT_SMART_TIMER_CONFIG_FILE);
                                        	                                          
                                          return true;
                                        }
 
                                });
                
                Preference switchWILDTimerConfig = findPreference("switch_wild_timer_config");
                switchWILDTimerConfig
                                .setOnPreferenceClickListener(new OnPreferenceClickListener() {
 
                                        @Override
										public boolean onPreferenceClick(Preference preference) {
                                                
                                        	
                                        
                                        	 Intent selectFileIntent = new Intent(context, DataFileSelector.class);
                                      		 selectFileIntent.putExtra("filepath", LucidDreamingApp.APP_HOME_FOLDER);
                                      		 //match wild followed or preceeded by anything with an extension of txt or txt.gzip
                                      		 selectFileIntent.putExtra("filterString","(.)*?[Ww][Ii][Ll][Dd](.)*(txt)([.]gzip)??");
                                      		 startActivityForResult(selectFileIntent, REQUEST_SELECT_WILD_TIMER_CONFIG_FILE);
                                        	                                          
                                          return true;
                                        }
 
                                });
                
                
//                Preference configureSmartTimer = findPreference("configure_smart_timer");
//                configureSmartTimer
//                                .setOnPreferenceClickListener(new OnPreferenceClickListener() {
// 
//                                        @Override
//										public boolean onPreferenceClick(Preference preference) {
//                                        	
//                                        	Intent smartTimerIntent = new Intent(context, SmartTimerActivity.class);
//                                        	smartTimerIntent.putExtra("eventType", SmartTimerActivity.REM_EVENT);
//                                        	smartTimerIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//                                			startActivity( smartTimerIntent);
//                                        	
//                                           return true;
//                                        }
// 
//                                });
                
//                Preference configureWILDTimer = findPreference("configure_wild_timer");
//                configureWILDTimer
//                                .setOnPreferenceClickListener(new OnPreferenceClickListener() {
// 
//                                        @Override
//										public boolean onPreferenceClick(Preference preference) {
//                                        	
//                                        	Intent smartTimerIntent = new Intent(context, SmartTimerActivity.class);
//                                        	smartTimerIntent.putExtra("eventType", SmartTimerActivity.WILD_EVENT);
//                                        	smartTimerIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//                                			startActivity( smartTimerIntent);
//                                        	
//                                           return true;
//                                        }
// 
//                                });
                
                
                                Preference recalculateColeConstantPref = findPreference("recalculate_cole_constant");
                                recalculateColeConstantPref
                                                .setOnPreferenceClickListener(new OnPreferenceClickListener() {
                 
                                                        @Override
														public boolean onPreferenceClick(Preference preference) {
                                                               //stop media player if the user clicks the preference again while it is playing 
                                                                        
                                                        	SharedPreferences prefs = PreferenceManager
                                                        	.getDefaultSharedPreferences(getBaseContext());
                                                 		
                                                 		 
                                                 	//	editor.putFloat("cole_constant_pref", (float)coleConstantVeryHighThreshold);
                                                 		int currentValue = prefs.getInt("activity_threshold", 13 );
                                                 		
                                                 		if(currentValue ==0){
                                                 			sendToast("Minimum activity threshold is 1");
                                                 			
                                                 		}else{
                                                 			double tempConstant = InMotionCalibration.calculateColeConstant(currentValue);
                                                 			sendToast("New Cole Constant: "+InMotionCalibration.calculateColeConstant(currentValue)+
                                                 					" single peak of: "+InMotionCalibration.calculateActivityPeakDelayed(tempConstant)+" considered awakening");
                                                 		
                                                 		
                                                 		SharedPreferences customSharedPreference = getSharedPreferences(
                                                                "LucidDreamingAppPreferences", Context.MODE_PRIVATE);
                                                 		//editor.putInt("activity_threshold", currentValue +125);
                                                 		Editor editor = customSharedPreference.edit();
                                                 		
                                                 		
                                                 		 editor.putFloat("coleConstantVeryHighThreshold",
                                                 				 (float)InMotionCalibration.calculateColeConstant(currentValue));
                                                 		
                                                 		editor.commit();
                                                 		editor = null;
                                                 		}
                                                 		
                                                        	
                                                        	
                                                                return true;
                                                        }
                 
                                                });
                                
                                //edit gestures
                                Preference gestureBuilderPref = findPreference("gesture_builder_pref");
                                gestureBuilderPref
                                                .setOnPreferenceClickListener(new OnPreferenceClickListener() {
                 
                                                        @Override
														public boolean onPreferenceClick(Preference preference) {
                                                               //stop media player if the user clicks the preference again while it is playing 
                                                     
                                                        	Intent editGesturesIntent = new Intent(context, GestureBuilderActivity.class);
                                                        	editGesturesIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);                                                    
                                                            
                                                        	startActivity(editGesturesIntent);
                                                        	
                                                                return true;
                                                        }
                 
                                                });
                                
              
                
                
                Preference sensorCalibrationPreference = findPreference("calibrate_sensor");
                sensorCalibrationPreference
                                .setOnPreferenceClickListener(new OnPreferenceClickListener() {
 
                                        @Override
										public boolean onPreferenceClick(Preference preference) {
                                                

                                        	SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
                                        	try{
                                        	int  accelerometer_calibration_duration_min = prefs.getInt("accelerometer_calibration_duration_min", 15);
                                        	sendToast("Calibration duration : "+accelerometer_calibration_duration_min+" min");
                                        //	Intent calibrateAccelerometerIntent = new Intent(context, InMotionCalibration.class);
                                        	Intent calibrateAccelerometerIntent = new Intent(context, InMotionCalibration.class);
                                        	calibrateAccelerometerIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                            calibrateAccelerometerIntent.putExtra("accelerometer_calibration_duration_min", accelerometer_calibration_duration_min);
                                            
                                        	startActivity(calibrateAccelerometerIntent);
                                        	}catch(Exception e){}
                                        	
                                           
                                            
                                                return true;
                                        }
 
                                });
                
                
                
                
        }
        
        
        @Override
        public void onActivityResult(int requestCode, int resultCode, Intent data) {
           // if(D) {System.out.println("onActivityResult " + resultCode);}
          	
            switch(requestCode){
            
            
            case REQUEST_SELECT_WILD_TIMER_CONFIG_FILE:
            	if(resultCode==Activity.RESULT_OK){
            		 String configFilepath=data.getExtras().getString("filepath");
            		 File f = new File(configFilepath);
         			Toast.makeText(getBaseContext(),
               "Selected: "+configFilepath,
               Toast.LENGTH_LONG).show();
         			
         			 SharedPreferences customSharedPreference = getSharedPreferences(
                             "LucidDreamingAppPreferences", Context.MODE_PRIVATE);
       			      SharedPreferences.Editor editor = customSharedPreference
       			                      .edit();
       			      editor.putString("WILDTimerConfigFilepath",
       			     		 f.getAbsolutePath());
       			   editor.commit();

                  	Intent smartTimerIntent = new Intent(context, SmartTimerActivity.class);
                  	smartTimerIntent.putExtra("eventType", SmartTimerActivity.WILD_EVENT);
                  	
                  	smartTimerIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
          			startActivity( smartTimerIntent);
            	}
            	break;
            
            case REQUEST_SELECT_SMART_TIMER_CONFIG_FILE:
            	
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

                      	Intent smartTimerIntent = new Intent(context, SmartTimerActivity.class);
                      	smartTimerIntent.putExtra("eventType", SmartTimerActivity.REM_EVENT);
                      	
                      	smartTimerIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
              			startActivity( smartTimerIntent);
	       			      
	         			
	         			
            	 }
            	 else{
            		 
            	 }
            break;
            	
            //using the Mp3FileSelector in the Recordings folder
            	case REQUEST_SELECT_FILE_BUILT_IN:
            		 if(resultCode==Activity.RESULT_OK){	
            	try{
            		String reminderFilepath=data.getExtras().getString("filepath");
            		File f = new File(reminderFilepath);
            			Toast.makeText(getBaseContext(),
                  "Selected: "+reminderFilepath,
                  Toast.LENGTH_LONG).show();
               		 SharedPreferences customSharedPreference = getSharedPreferences(
                      "LucidDreamingAppPreferences", Context.MODE_PRIVATE);
			      SharedPreferences.Editor editor = customSharedPreference
			                      .edit();
			      editor.putString("reminderFilepath",
			     		 f.getAbsolutePath());
			      
			      editor.putString("reminderFileUri",
			    		  f.getAbsolutePath());
			      
			      editor.commit();
			            			
            			
            		}catch(Exception e){
            			if(D) e.printStackTrace();
            			//TODO store the file
            		}
            		 }else if(resultCode ==Activity.RESULT_CANCELED){
            			 
            			 try{
                     		String message = data.getExtras().getString("message");
                     		sendToast(message);}
                     		catch(Exception e){
                     			if(D) e.printStackTrace();
                     			sendToast("File selection cancelled");
                     		}
            			 
            		 }
            			break;
            			
            		//using OI File manager and URIs	
            		case REQUEST_SELECT_FILE:
            			 if(resultCode==Activity.RESULT_OK){
            				 Uri uri =data.getData();
            				 Toast.makeText(getBaseContext(),
            						 "Selected: "+uri.getPath(),
            						 Toast.LENGTH_LONG).show();
            		//TODO remove after testing
//            		mp = new MediaPlayer();
//            		try{
//		        	mp.setDataSource(uri.getPath());
//		        	mp.prepare();
//		        	mp.start();
//            		}catch(Exception e){
//            			sendToast("could not play "+uri);
//            			if(D)e.printStackTrace();
            				 
//            		}
            				 File f = new File(uri.getPath());
            		if(D)System.out.println("uri String "+uri.toString());
            		if(D)System.out.println("uri Path "+uri.getPath());
            		if(D)System.out.println("file absolute Path "+f.getAbsolutePath());
            				 
              		 SharedPreferences customSharedPreference = getSharedPreferences(
                             "LucidDreamingAppPreferences", Context.MODE_PRIVATE);
       			      SharedPreferences.Editor editor = customSharedPreference
       			                      .edit();
       			     
       			      
       			      		editor.putString("reminderFileUri", uri.getPath());
       			      	editor.putString("reminderFilepath",
       			      									uri.getPath());
       			      		
       			      		editor.commit();
            			 }else if(resultCode ==Activity.RESULT_CANCELED){
            				 sendShortToast("File selection cancelled");
            			 }
            			break;
            		
            		
            		
            		}

            	
            }
        
        protected void sendToast(String message){
        	Context context = getApplicationContext();
    		  Toast toast = Toast.makeText(context, message,Toast.LENGTH_LONG);
    		  toast.show();
        }
        
        protected void sendShortToast(String message){
        	
        	Toast.makeText(getBaseContext(),
                    message,
                    Toast.LENGTH_SHORT).show();
        	
        }
        
        public void colorChanged(int color) {
        	SharedPreferences prefs = PreferenceManager
        	.getDefaultSharedPreferences(getBaseContext());
        	
        	Editor editor = prefs.edit();
        	editor.putInt("clockColor", color);
        	editor.commit();
        	sendShortToast("Enjoy your new clock color");
        	if(D)System.out.println("Clock color: "+color);
        	
	    }

        
}