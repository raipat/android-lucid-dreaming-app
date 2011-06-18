package com.luciddreamingapp.beta;

import java.io.File;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Vibrator;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.TimePicker;
import android.widget.Toast;

import com.luciddreamingapp.beta.util.MorseCodeConverter;
import com.luciddreamingapp.beta.util.state.SleepCycleEventVO;
import com.luciddreamingapp.beta.util.state.SmartTimerActivity;
import com.luciddreamingapp.beta.util.state.WILDEventVO;

public class EventEditorActivity extends PreferenceActivity implements OnSharedPreferenceChangeListener {
	public static final String TAG = "EventEditor";
	public static final boolean D = true;

	public static final int START_TIME_DIALOG_ID =1;
	public static final int DURATION_TIME_DIALOG_ID =2;
	
private GlobalApp A;
	private SleepCycleEventVO vo;
	
	
	private  int durationHour = 0;
	private  int durationMinute = 10;
	private  int eventDuration = durationHour*60 + durationMinute;
	
	private MediaPlayer mp;
	SharedPreferences prefs;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		
	
		super.onCreate(savedInstanceState);
		
		A = (GlobalApp)getApplication();
		
		if(SmartTimerActivity.sleepCycleEventVO==null){
			finish();
		}else{
			if(!(vo instanceof WILDEventVO)){	
			vo =SmartTimerActivity.sleepCycleEventVO;
			}else{
				vo =(WILDEventVO)SmartTimerActivity.sleepCycleEventVO;
			}
			
			
			if(D)System.out.println(vo);
			try{
				prefs = PreferenceManager
		        .getDefaultSharedPreferences(getBaseContext());
			prefs.registerOnSharedPreferenceChangeListener(this);
			
			Editor editor = prefs.edit();
			
			editor.putBoolean("reminderSet", vo.reminderSet);
//			editor.remove("deliveryMode");
//			editor.remove("vibrateDotDuration");
//			editor.remove("flashDotDuration");
			//time and duration are controlled by dialogs
			editor.putString("deliveryMode", ""+vo.deliveryMode);
			//editor.putInt("deliveryMode", vo.deliveryMode);
			editor.putBoolean("useVoiceReminder", vo.useVoiceReminder);
			editor.putBoolean("useVibrateReminder", vo.useVibrateReminder);
			editor.putBoolean("useStrobe", vo.useStrobe);	
			
			editor.putString("flashMessage", vo.flashMessage);
			editor.putString("vibrateMessage", vo.vibrateMessage);
			
			editor.putInt("vibrateDotDuration", vo.vibrateDotDuration);
			editor.putInt("flashDotDuration", vo.flashDotDuration);
			
			
			editor.commit();
			
			}catch(Exception e){
				if(D)e.printStackTrace();}
			//TODO: link these against the stuff in smart timer
			try{
				
				if(!(vo instanceof WILDEventVO)){	
			 addPreferencesFromResource(R.xml.edit_event);
				}else{
					addPreferencesFromResource(R.xml.edit_wild_event);
				}
			
			 
			 
			}catch(Exception e){
				if(D)e.printStackTrace();
				finish();
			}
			
			
			OnPreferenceClickListener listener = new OnPreferenceClickListener() {

                @Override
					public boolean onPreferenceClick(Preference preference) {
               	 SharedPreferences prefs = PreferenceManager
    		        .getDefaultSharedPreferences(getBaseContext());
               	 vo.reminderSet = prefs.getBoolean("reminderSet", true);
               	 vo.useVoiceReminder=prefs.getBoolean("useVoiceReminder", true);
               	 vo.useStrobe=prefs.getBoolean("useStrobe", false);
               	 vo.useVibrateReminder=prefs.getBoolean("useVibrateReminder", false);
               	 
               	 vo.vibrateMessage =prefs.getString("vibrateMessage", "morse code");
               	 vo.flashMessage =prefs.getString("flashMessage", "morse code");
               	 
               	 
               	 vo.vibrateDotDuration =prefs.getInt("vibrateDotDuration", 120);
               	 vo.flashDotDuration =prefs.getInt("flashDotDuration", 360);
               	 
               	 setResult(Activity.RESULT_OK);
                    finish();
                   return true;
                }   
			};
              
			
			//finish activity
			 Preference commitPreference = findPreference("commitChanges");
			 commitPreference.setOnPreferenceClickListener( listener);
				//finish activity
			 Preference commitPreference2 = findPreference("commitChanges2");
			 commitPreference2.setOnPreferenceClickListener( listener);
 			
			 Preference startTimePreference = findPreference("startTime");
			 startTimePreference.setOnPreferenceClickListener(new OnPreferenceClickListener() {

                                     @Override
										public boolean onPreferenceClick(Preference preference) {
                                    	 sendShortToast("StartTime");
                                    	               					
                     					showStartTimeDialog();
                                    	 
                                    	
                                        return true;
                                     }   });
		
			 //do not add this preference for WILD events

			 
			 Preference testReminderPreference = findPreference("testReminder");
			 testReminderPreference.setOnPreferenceClickListener(new OnPreferenceClickListener() {

                                     @Override
										public boolean onPreferenceClick(Preference preference) {
                                    	testReminders();
                                        return true;
                                     }   });
			 
			 //do not add these preference for WILD events
				if(!(vo instanceof WILDEventVO)){
					
					 Preference durationPreference = findPreference("duration");
					 durationPreference.setOnPreferenceClickListener(new OnPreferenceClickListener() {

		                                     @Override
												public boolean onPreferenceClick(Preference preference) {
		                                    	 sendShortToast("Duration");
		                                    	durationHour= vo.duration/60;
		                     	        		durationMinute = vo.duration%60;
		                     	        		showDurationDialog();
		                                        return true;
		                                     }   });
					
					
					
					
			 Preference reminderDelivery = findPreference("deliveryMode");
			 reminderDelivery.setOnPreferenceClickListener(new OnPreferenceClickListener() {

                                     @Override
										public boolean onPreferenceClick(Preference preference) {

                     	        		
                     	        		//show a dialog to select when the reminder should be played
                     	        		AlertDialog.Builder builder = new AlertDialog.Builder(EventEditorActivity.this);
                     	        		//
                     	        		
                     	        		builder.setTitle("When should the reminder be played?");
                     	        		builder.setItems(SleepCycleEventVO.choices, new DialogInterface.OnClickListener() {
                     	        		    public void onClick(DialogInterface dialog, int item) {
                     	        		       
                     	        		        vo.deliveryMode=item;
                     	        		                             	        		    }
                     	        		});
                     	        		AlertDialog alert = builder.create();
                     	        		alert.show();
                                        return true;
                                     }   });
				}//end exclude from WILD events
			 
			 Preference pickReminder = findPreference("filePicker");
			 pickReminder.setOnPreferenceClickListener(new OnPreferenceClickListener() {

                                     @Override
										public boolean onPreferenceClick(Preference preference) {
                                         
                                     		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
                                     	boolean useFileSelection = prefs.getBoolean("old_file_selection_pref", false);
                                     	

                                     	Intent oiManagerExists =getPackageManager().getLaunchIntentForPackage("org.openintents.filemanager");
                                     	if(oiManagerExists==null ||useFileSelection){
                                     		try{
                                         		Intent selectFileIntent = new Intent(EventEditorActivity.this, Mp3FileSelector.class);
                                                  startActivityForResult(selectFileIntent, SmartTimerActivity.REQUEST_SELECT_FILE_BUILT_IN);
                                         		}catch(Exception e){
                                         			if(D)e.printStackTrace();
                                         		}
                                     		//old file selection
                                     	}else{
                                     		Intent intent = new Intent("org.openintents.action.PICK_FILE");
                                         	intent.putExtra("org.openintents.extra.TITLE", "Pick reminder .Mp3, .Mp4 or .Wav file");
                                         	intent.putExtra("org.openintents.extra.BUTTON_TEXT", "Pick");
                                         	
                                         	intent.setData(Uri.parse("file:///sdcard/Recordings"));
                                         	try{
                                         		startActivityForResult(intent, SmartTimerActivity.REQUEST_SELECT_FILE);
                                         		//sendShortToast("Opening file manager");
                                         		}
                                         	catch(Exception e){if(D)e.printStackTrace();}
                                     	}
                                    	                 
                                        return true;
                                     }   });
			}
		}
	
	
	
	
	
	
	protected void showDurationDialog(){
		if(D)Log.e(TAG,"preparing duration dialog ");
		TimePickerDialog timePicker = new TimePickerDialog(this,
				durationListener, vo.duration/60, vo.duration%60, true) {
			
        	@Override
        	//Define a method to set meaningful title for the dialog when time changes
			public void onTimeChanged(TimePicker view, int hourOfDay,
					int minute) {
				StringBuilder sb = new StringBuilder();
				sb.append("Event start time: ");
				sb.append(hourOfDay*60+minute);
				sb.append(" minutes");
				this.setTitle(sb.toString());//refer to the time picker from within itself
				sb = null;

			}
        	
		};
		timePicker.show();
	}
	
	protected void showStartTimeDialog(){
		if(D)Log.e(TAG,"preparing duration dialog ");
	TimePickerDialog timePicker = new TimePickerDialog(this,
			mTimeSetListener, vo.startMinute/60, vo.startMinute%60, true) {
		
    	@Override
    	//Define a method to set meaningful title for the dialog when time changes
		public void onTimeChanged(TimePicker view, int hourOfDay,
				int minute) {
			StringBuilder sb = new StringBuilder();
			sb.append("Event start time: ");
			sb.append(hourOfDay*60+minute);
			sb.append(" minutes");
			this.setTitle(sb.toString());//refer to the time picker from within itself
			sb = null;

		}
	};
	timePicker.show();
	}
	

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
	   // if(D) {System.out.println("onActivityResult " + resultCode);}
		if(D)Log.e(TAG,"requestCode: "+requestCode+ " resultCode: "+resultCode);
	    switch(requestCode){
	    
	 
	    
	    //using the Mp3FileSelector in the Recordings folder
	    	case SmartTimerActivity.REQUEST_SELECT_FILE_BUILT_IN:
	    		 if(resultCode==Activity.RESULT_OK){	
	    	try{
	    		String reminderFilepath=data.getExtras().getString("filepath");
	    		
	    			Toast.makeText(getBaseContext(),
	          "Selected: "+reminderFilepath,
	          Toast.LENGTH_SHORT).show();
	    			
	    	vo.reminderFilepath = reminderFilepath;
	    	 		            			
	    			
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
	    		case SmartTimerActivity.REQUEST_SELECT_FILE:
	    			 if(resultCode==Activity.RESULT_OK){
	    				 Uri uri =data.getData();
	    				 Toast.makeText(getBaseContext(),
	    						 "Selected: "+uri.getPath(),
	    						 Toast.LENGTH_LONG).show();
	    	
//	    		}
	    				 
	    		File f = new File(uri.getPath());
	    		if(D)System.out.println("uri String "+uri.toString());
	    		if(D)System.out.println("uri Path "+uri.getPath());
	    		if(D)System.out.println("file absolute Path "+f.getAbsolutePath());
	    				 
	    		vo.reminderFilepath = f.getAbsolutePath();
	    	
	    		
	    			 }else if(resultCode ==Activity.RESULT_CANCELED){
	    				 sendShortToast("File selection cancelled");
	    			 }
	    			break;
	    		
//	    	case REQUEST_SELECT_FILE:
//       			 if(resultCode==Activity.RESULT_OK){
//       				 Uri uri =data.getData();
//       				 Toast.makeText(getBaseContext(),
//       						 "Selected: "+uri.getPath(),
//       						 Toast.LENGTH_LONG).show();
//       	
//       				 File f = new File(uri.getPath());
//       		if(D)System.out.println("uri String "+uri.toString());
//       		if(D)System.out.println("uri Path "+uri.getPath());
//       		if(D)System.out.println("file absolute Path "+f.getAbsolutePath());
//       			
//       		
//       		vo.reminderFilepath = f.getAbsolutePath();
//       		
//       			 }else if(resultCode ==Activity.RESULT_CANCELED){
//       				 sendShortToast("File selection cancelled");
//       			 }
//       			break;
	    		
	    		}
	    

	    	
	    }
	
	 private TimePickerDialog.OnTimeSetListener durationListener =
         new TimePickerDialog.OnTimeSetListener() {
             public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
             	
             	eventDuration =hourOfDay*60+minute;
             	vo.duration = eventDuration;       
             	
             }
         };
         
         
         // the callback received when the user "sets" the time in the dialog
         private TimePickerDialog.OnTimeSetListener mTimeSetListener =
             new TimePickerDialog.OnTimeSetListener() {
                 public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
                   
                	 vo.startMinute=hourOfDay*60+ minute;
                 	
                 	
                 	//assign default duration
                 	if(vo.duration==0){
                 		
                 		
                 	//TODO look at determining the event type	
                 	switch(SmartTimerActivity.eventType){
            			 
            			 case SmartTimerActivity.REM_EVENT:
            				 vo.duration=eventDuration;
            				sendShortToast("Setting event duration to: "+eventDuration);
            			
            			break;
            			 case SmartTimerActivity.WILD_EVENT:
            				vo.duration=1;
            			
            			 break;
            			 }
                 		
                 		
                 		 
                 	}
                 
                 	
            
                 }
             };
         


	@Override
	protected void onDestroy() {
		// TODO Auto-generated method stub
		super.onDestroy();
		if(D)Log.e("Edit event","ON DESTROY");
		if(prefs!=null){
			prefs.unregisterOnSharedPreferenceChangeListener(this);
		}
		
	}


	@Override
	protected void onPause() {
		// TODO Auto-generated method stub
		super.onPause();
		if(D)Log.e("Edit event","ON PAUSE");
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
	    
	    protected void checkPlaybackSettings(){

	  	  SharedPreferences prefs = PreferenceManager
	         .getDefaultSharedPreferences(getBaseContext());
	  	  boolean playSoundReminders = prefs.getBoolean("play_sound_pref", false);
	  	 
	  		  //check sound playback preferences
	  	        if(!playSoundReminders){
	  	        	sendShortToast("\"Play Voice Reminders\" preference not enabled");
	  	        }
	  	        //check if current event is active
	  	        if(!vo.reminderSet){
	  				  sendShortToast("REM Event not active");
	  			 }	
	  }
	    
	    protected void testReminders(){
	  	 
			 
				  checkPlaybackSettings();
				  new VoiceTask().execute(null,null,null);
				  new VibrateTask().execute(null,null,null);
				  new StrobeTask().execute(null,null,null);
			  
	    }
	    
	    
	    private class VoiceTask extends AsyncTask<String, Void, Void> {
		     protected Void doInBackground(String... urls) {
		        if(D)Log.e(TAG, "voice task executing");
		        
		     
		        try{
		        	if(vo.useVoiceReminder){
		        		
		        		   A.voiceInteractAsync(vo.reminderFilepath);
		        		
//					  if( mp!=null && mp.isPlaying()){
//					        	try{	
//					        		mp.stop();
//					        		mp.reset();
//					        		mp.release();}catch(Exception e){}		        		
//						        	mp = new MediaPlayer();
//					        		mp.setDataSource(vo.reminderFilepath);
//						        	mp.prepare();
//						        	mp.start();	
//					        	}else{
//					        	mp = new MediaPlayer();		        	
//					        	mp.setDataSource(vo.reminderFilepath);
//					        	mp.prepare();
//					        	mp.start();			        	
//					        	}
		        	}
					        }catch(Exception e){
					       // 	sendShortToast("Unable to play file");
					        	if(D)e.printStackTrace();
					        }
		        
		         return null;
		     }

		     protected void onPostExecute() {
		         
		     }
		 }
		 
		 private class VibrateTask extends AsyncTask<long[], Void, Void> {
		     protected Void doInBackground(long[]... urls) {
		        if(D)Log.e(TAG, "vibrate task executing");
		      
		        if(vo.useVibrateReminder && vo.vibrateMessage!=null && !vo.vibrateMessage.equals("")){
		        	
		        	  long temp = 10;
					   if(vo.vibrateDotDuration>1)temp =vo.vibrateDotDuration; 
		        	
					  long[] pattern = MorseCodeConverter.pattern(vo.vibrateMessage,temp);

			          // Start the vibration. Requires VIBRATE permission
					 // sendToast("Vibrating :"+vo.vibrateMessage);
					  
					  A.vibrateInteractAsync(pattern);
					  
//			          Vibrator vibrator = (Vibrator)getSystemService(Context.VIBRATOR_SERVICE);
//			          vibrator.vibrate(pattern, -1);
			         
				  }else {
					//  sendShortToast("No text to vibrate");
				  }
		         return null;
		     }

		     protected void onPostExecute() {
		         
		     }
		 }
		 
		 private class StrobeTask extends AsyncTask<long[], Void, Void> {
		     
			 protected Void doInBackground(long[]... urls) {
				   if(D)Log.e(TAG, "vibrate task executing");
				   if(vo.useStrobe && vo.flashMessage!=null&& !vo.flashMessage.equals("")){
					   //if the user sets the seekbar to 0, avoid that
					   long temp = 10;
					   if(vo.flashDotDuration>1)temp =vo.flashDotDuration; 
					   
					   A.strobeInteractAsync(MorseCodeConverter.pattern(vo.flashMessage,temp));
					   
//					   
//			        Intent strobeIntent = new Intent(EventEditorActivity.this, Strobe.class);
//	        		strobeIntent.putExtra("strobeTiming", MorseCodeConverter.pattern(vo.flashMessage,temp));
//
//	        		startActivity(strobeIntent);
				   }
		        return null;
		     }

		     protected void onPostExecute() {
		         
		     }
		 }

		 public static final String reminderSet= "reminderSet";
		 public static final String useVoiceReminder= "useVoiceReminder";
		 public static final String useStrobe= "useStrobe";
		 public static final String useVibrateReminder= "useVibrateReminder";
		 public static final String vibrateMessage= "vibrateMessage";
		 public static final String flashMessage= "flashMessage";		 
		 public static final String vibrateDotDuration= "vibrateDotDuration";		 
		 public static final String flashDotDuration= "flashDotDuration";
		 public static final String deliveryMode= "deliveryMode";
		
		 
		@Override
		public void onSharedPreferenceChanged(SharedPreferences prefs,
				String key) {
			// TODO Auto-generated method stub
			if(D)Log.e(TAG,"Preference changed: "+key);
			if(key.equals(reminderSet)){
				 vo.reminderSet = prefs.getBoolean(reminderSet, true);
			}else if(key.equals(useVoiceReminder)){
				vo.useVoiceReminder=prefs.getBoolean(useVoiceReminder, true);
				
			}else if(key.equals(useStrobe)){
				vo.useStrobe=prefs.getBoolean(useStrobe, false);
				
			}else if(key.equals(useVibrateReminder)){
				vo.useVibrateReminder=prefs.getBoolean(useVibrateReminder, false);
				
			}else if(key.equals(vibrateMessage)){
				vo.vibrateMessage=prefs.getString(vibrateMessage, "morse code");
				
			}else if(key.equals(flashMessage)){
				vo.flashMessage=prefs.getString(vibrateMessage, "morse code");
				
			}else if(key.equals(vibrateDotDuration)){
				vo.vibrateDotDuration=prefs.getInt(vibrateDotDuration, 120);
			}
			else if(key.equals(flashDotDuration)){
				vo.flashDotDuration=prefs.getInt(flashDotDuration, 360);
			}

          
			
			
		}
		 
		 
	    
	    
	
       
	}

	
	
	

