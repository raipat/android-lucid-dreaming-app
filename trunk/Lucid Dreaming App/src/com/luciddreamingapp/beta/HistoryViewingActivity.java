package com.luciddreamingapp.beta;

import java.io.File;
import java.io.IOException;
import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.TimeZone;

import org.apache.commons.math.stat.descriptive.DescriptiveStatistics;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnMultiChoiceClickListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.Window;
import android.webkit.WebView;
import android.widget.Toast;

import com.luciddreamingapp.beta.util.FileHelper;
import com.luciddreamingapp.beta.util.JSONLoader;
import com.luciddreamingapp.beta.util.LogManager;
import com.luciddreamingapp.beta.util.SleepCycleVO;
import com.luciddreamingapp.beta.util.state.SmartTimer;
import com.luciddreamingapp.beta.util.state.SmartTimerActivity;

public class HistoryViewingActivity extends Activity implements OnMultiChoiceClickListener {
	
	private static final int REQUEST_SELECT_SMART_TIMER_CONFIG_FILE = 11;
	private static final int REQUEST_EDIT_CONFIG = 22;
	private static final int REQUEST_LOAD_HISTORY_FILE = 3;
	
	private static final int REQUEST_CREATE_HISTORY = 1;
	private static final int REQUEST_SELECT_FILE_BUILT_IN =2;
	private static final boolean D = false;
	private static final String TAG = "LD App History Activity";
	
	private static final int graphFilesize = 5;//minimum size of a graph to analyze in kb
	
	JSONDataHandler handler;
	private WebView graphView;
	
	private Handler guiHandler;
	
	JSONObject historyObject; 
	 List<SleepCycleVO> sleepCycles;
	 
	 private int historyMaxSize = 7;
	 
	 private File directory;
	 
	 private SmartTimer smartTimer;
	 private String historyFilename;
	 
	long startTime;//debug timestamp to see how long it takes to create a history from files
	/* (non-Javadoc)
	 * @see android.app.Activity#onCreate(android.os.Bundle)
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		
		
		setContentView(R.layout.graph_viewer);
	
		startTime = System.currentTimeMillis();
		//possibly provide an example
		//handler.setJson1(new JSONObject(readRaw()));}catch(Exception e){e.printStackTrace();}
		graphView = (WebView) findViewById(R.id.graph_view);
        graphView.getSettings().setJavaScriptEnabled(true);
        graphView.getSettings().setLoadWithOverviewMode(true);
        graphView.getSettings().setUseWideViewPort(true);
        graphView.getSettings().setBuiltInZoomControls(true);
        graphView.addJavascriptInterface(handler, "javahandler");
        graphView.loadUrl("file:///android_asset/html/history_instructions.html");
     //  graphView.loadUrl("file:///android_asset/html/show_data_from_file.html");
		
        historyObject = new JSONObject();
       
    guiHandler = new Handler();
    
    
	}

	
	
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.history_viewing_activity_menu, menu);
        return true;
    }
    

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {

        
        case R.id.menu_change_config:
        	
       	 Intent selectConfigIntent = new Intent(this, DataFileSelector.class);
       	 selectConfigIntent.putExtra("filepath", LucidDreamingApp.APP_HOME_FOLDER);
     		 //match smart followed or preceeded by anything with an extension of txt or txt.gzip
       	selectConfigIntent.putExtra("filterString","(.)*?[Ss]mart(.)*(txt)([.]gzip)??");
     	startActivityForResult(selectConfigIntent, REQUEST_SELECT_SMART_TIMER_CONFIG_FILE);
       	                                          
      
       	break;
        	
        	case R.id.menu_history:
        	
        		historyFilename = getHistoryFileName();
            	//Intent intent = new Intent("org.openintents.action.PICK_DIRECTORY");
            	
            	Intent intent = new Intent("org.openintents.action.PICK_DIRECTORY");
            	
            	intent.putExtra("org.openintents.extra.TITLE", getString(R.string.pick_history_directory_window_title));
            	intent.putExtra("org.openintents.extra.BUTTON_TEXT", getString(R.string.pick_history_directory_button_text));
            	
            	
            	//pick the history directory
            	intent.setData(Uri.parse("file:///sdcard/"+LucidDreamingApp.GRAPH_DATA_LOCATION));
            	
            	Intent oiManagerExists =getPackageManager().getLaunchIntentForPackage("org.openintents.filemanager");
            	//using OI File manager
            	if(oiManagerExists!=null){
            		try{
            		sendShortToast(getString(R.string.toast_processing_please_wait));
            		startActivityForResult(intent, REQUEST_CREATE_HISTORY);
            		}catch(Exception e){
            			sendShortToast(getString(R.string.toast_error));
            		}
            	}else{
            		//or proces the default directory 
            		sendShortToast(getString(R.string.toast_processing_please_wait));
            		File directory = new File(Environment.getExternalStorageDirectory(),LucidDreamingApp.GRAPH_DATA_LOCATION);
            		try{
            		if(directory.exists() && directory.canRead()){
            			new HistoryTask().execute(directory);
            		//processFilesFromDirectory(directory);
            		}else{sendToast(getString(R.string.toast_error_file_not_found,directory.getAbsolutePath()));}
            		}catch(SecurityException ex){sendToast(getString(R.string.toast_error_security_exception));}
            	}
            	
            	 
        	return true;

        case R.id.menu_save_data:
        
        //zip the history file	
        String filepath =	LogManager.writeToAndZipFile(LucidDreamingApp.HISTORY_DATA_LOCATION, getHistoryFileName(), historyObject.toString());
        
        if(D)System.out.println(filepath);
        
        	if(filepath !=null){
        			
				sendToast(getString(R.string.toast_history_saved,filepath));
				
				}else{
				sendShortToast(getString(R.string.toast_error_history_not_saved));
				}
        		
        	
        	
        	return true;
        	
        	case R.id.menu_load_data:
        		
        		sendShortToast(getString(R.string.toast_load_history));
            	Intent loadHistoryIntent = new Intent("org.openintents.action.PICK_FILE");
            	
            	loadHistoryIntent.putExtra("org.openintents.extra.TITLE", getString(R.string.pick_history_file_window_title));
            	loadHistoryIntent.putExtra("org.openintents.extra.BUTTON_TEXT", getString(R.string.pick_history_file_button_text));
            	loadHistoryIntent.setData(Uri.parse("file:///sdcard/"+LucidDreamingApp.HISTORY_DATA_LOCATION));
            
            	try{
            		startActivityForResult(loadHistoryIntent, REQUEST_LOAD_HISTORY_FILE);
            		//sendShortToast("Opening file manager");
            		
            	}catch(Exception e){
            	
            		 Intent selectFileIntent = new Intent(this, DataFileSelector.class);
            		 selectFileIntent.putExtra("filepath",LucidDreamingApp.HISTORY_DATA_LOCATION);
            		 startActivityForResult(selectFileIntent, REQUEST_SELECT_FILE_BUILT_IN);
            	}
        	
        	
        	return true;
        
//            case R.id.menu_preferences:
//            	sendToast("Preferences");	
//            	Intent timerActivity = new Intent(getBaseContext(),
//                        SmartTimerActivity.class);
//            		startActivity(timerActivity);
//            	
            
//            	return true;
        	
        	case R.id.menu_screenshot:
        		new SaveScreenshotTask().execute(null);
        		break;
        case R.id.menu_preferences:
        	//sendToast("Preferences");	
        	Intent settingsActivity = new Intent(getBaseContext(),
                    Preferences.class);
        		startActivity(settingsActivity);
        	
        
        	return true;
        
        case R.id.menu_help:

        	graphView.loadUrl("file:///android_asset/html/history_instructions.html");
	       	
        	return true;
      
            
     
        	
        case R.id.menu_exit:
	      //	sendToast("Exiting");
        	finish();
	       	
        	return true;
        
        }
  
        return false;
    }
    
    
    public void onClick(DialogInterface arg0, int which,
			boolean isChecked) {
		// TODO Auto-generated method stub
  						
		sendToast("items:"+which+" checked"+isChecked);
	
	}
    
    
	@Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(D) {Log.e(TAG,"onActivityResult request: "+requestCode+" result: " + resultCode);}
        switch (requestCode) {
        
        case REQUEST_EDIT_CONFIG:
        	if(resultCode==Activity.RESULT_OK){
        		if(directory!=null){
        			new HistoryTask().execute(directory);
        			//(directory);
        			}
        	}
        	break;
        
        case REQUEST_SELECT_SMART_TIMER_CONFIG_FILE:
        	if(resultCode==Activity.RESULT_OK){
        		
        		
        		 String configFilepath=data.getExtras().getString("filepath");
        		 File f = new File(configFilepath);
     			Toast.makeText(getBaseContext(),
           "Selected: "+configFilepath,
           Toast.LENGTH_SHORT).show();
     			
     			 SharedPreferences customSharedPreference = getSharedPreferences(
                         "LucidDreamingAppPreferences", Context.MODE_PRIVATE);
   			      SharedPreferences.Editor editor = customSharedPreference
   			                      .edit();
   			      editor.putString("configFilepath",
   			     		 f.getAbsolutePath());
   			   editor.commit();
        		
              	Intent smartTimerIntent = new Intent(this, SmartTimerActivity.class);
              	smartTimerIntent.putExtra("eventType", SmartTimerActivity.REM_EVENT);
              	smartTimerIntent.putExtra("startedFromHistory", 1);
              	
             // 	smartTimerIntent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
      			startActivityForResult(smartTimerIntent,REQUEST_EDIT_CONFIG);
        	}
        	
        	break;
        
        case REQUEST_SELECT_FILE_BUILT_IN:
        	if(resultCode==Activity.RESULT_OK){
//        		sendShortToast("Loading...");
        		//get the filepath returned by the file selector and re-initialize the state
        		String filepath=data.getExtras().getString("filepath");
        		File historyFile = new File(filepath);
        		historyFilename = historyFile.getName().replace("[.]gzip",""); 
        		
        		try{
        			historyObject = JSONLoader.getData(historyFile);
        			
            		loadPage();
        			
        		}catch(IOException e){
        			sendToast(getString(R.string.pick_history_file_window_title,historyFile.getAbsolutePath()) );
        		}
        		}else if(resultCode ==Activity.RESULT_CANCELED){
            		sendShortToast(getString(R.string.toast_file_selection_canceled));
            	
//        		try{
//        	//	setTitle(dataFile.getName());
//        		handler = new JSONDataHandler();
//        		handler.setJson1(JSONLoader.getData(dataFile));
//        		}catch(Exception e){
//        			sendShortToast("Error opening file");
//        		}
//        		loadPage();
//        		
//        	}else if(resultCode ==Activity.RESULT_CANCELED){
//        		sendShortToast("File selection cancelled");
        	}
        	break;
        	
        	
        case REQUEST_CREATE_HISTORY:
        	startTime = System.currentTimeMillis();
        	if(resultCode==Activity.RESULT_OK){
        		sendShortToast(getString(R.string.toast_loading));
        		
        		
        		Uri uri =data.getData();
        		
        		//File directory = new File(URI.create((uri.toString())));
        		directory = new File(uri.getPath());
        		new HistoryTask().execute(directory);
        		//processFilesFromDirectory(directory);
            		
        		
        	}else if(resultCode ==Activity.RESULT_CANCELED){
        		sendShortToast(getString(R.string.toast_history_creation_canceled));
        	}
        	break;
        	
        	
        	case REQUEST_LOAD_HISTORY_FILE:
        		if(resultCode==Activity.RESULT_OK){
        		sendShortToast(getString(R.string.toast_loading));
        		Uri uri =data.getData();
        		
        		//File directory = new File(URI.create((uri.toString())));
        		File historyFile = new File(uri.getPath());
        		historyFilename = historyFile.getName().replace("[.]gzip",""); 
        		
        		try{
        			historyObject = JSONLoader.getData(historyFile);
        			
            		loadPage();
        			
        		}catch(IOException e){
        			sendToast(getString(R.string.toast_error_history_not_opened,historyFile.getAbsolutePath()) );
        		}
        		}else if(resultCode ==Activity.RESULT_CANCELED){
            		sendShortToast(getString(R.string.toast_file_selection_canceled));
            	}
        	break;
        	
        	
        }	
        }
    
	
	 private class HistoryTask extends AsyncTask<File, Void, Void> {
	     
		 protected Void doInBackground(File... urls) {
			 guiHandler.post(new Runnable(){
					
					public void run(){
						sendShortToast(getString(R.string.toast_loading));
					}
				});
			 processFilesFromDirectory(urls[0]);
	        return null;
	     }

	     @Override
		protected void onPostExecute(Void result) {
			// TODO Auto-generated method stub
			super.onPostExecute(result);
			
			 guiHandler.post(new Runnable(){
					
					public void run(){
						sendShortToast("Done loading");
					}
				});
		}

		
	 }
	
	private void processFilesFromDirectory(File directory){

		File[] files = directory.listFiles(FileHelper.createFileFilter(".*(txt)([.]gzip)?"));
		
	
		//try to sort them in order of modification, so they appear in order
		Arrays.sort(files, new Comparator<File>(){
		    public int compare(File f1, File f2)
		    {
		        return Long.valueOf(f1.lastModified()).compareTo(f2.lastModified());
		    } });
		
		
		List<File> list = new ArrayList<File>();
		for(int i = 0;i<files.length;i++){
		//	System.out.println(array[i].getName()+" "+array[i].length()/1024+" Kb");
			//more than 15kb may indicate a full night of sleep
			
			//*********************************************
			
			
			if(files[i].length()/1024 >graphFilesize){
				list.add(files[i]);
			}
	 	}
	if(D)System.out.println("Before processing, files: "+list.size());
	if(list.size()>0){
		
		if(D)Log.e(TAG, "list.size()"+list.size());
		if(D)Log.e(TAG, "list.sublist"+list.subList(list.size()-1,list.size()));
		
		if(list.size()>historyMaxSize){
			list = list.subList(list.size()-historyMaxSize-1, list.size());
		}
	//process data in the list	
	processData(list);
	}else{
		guiHandler.post(new Runnable(){
			
			public void run(){
				sendShortToast(getString(R.string.toast_error_no_files_found));
			}
		});
	
	}
		
		
	}
	
	
	/**History takes place on different days. This method rewrites the timestamps in such a way
	 * that the plotting method thinks they all took place on the same day. The plotting method will
	 * then pick the longest value as the X axis. All values will be plotted against the same axis, 
	 * resulting in the user being able to see when the user went to sleep and how quickly he/she accumulated sleep
	 * 
	 * @param history
	 */
	private void processData(List<File> history){
		
		List<JSONObject> historyList  = new ArrayList<JSONObject>();
		int longestNightDuration = 1;
		
		//TODO introduce size check for more than 10 days
		//process and unzip files
		if(D)System.out.println("processing file list");
		
	
		
		//initialize descriptive statistics
		initializeStatistics();
		historyObject = new JSONObject();
		try{historyObject.put("title", "Sleep data for: "+history.size() + " nights");}catch(JSONException e){}
		int nightCounter = 1;
		for(File file: history){
			try{
			
			historyList.add(JSONLoader.getData(file));
			}catch(Exception e){
				sendToast(getString(R.string.toast_error_reading_file,file.getName()));
				if(D)e.printStackTrace();
			}
		}
		
		long timestamp = 0;
		
		
		JSONArray temp = new JSONArray();
		JSONArray temp2 = new JSONArray();
		JSONArray formattedArray = new JSONArray();
		JSONArray remPredictionReminders = new JSONArray();
		
		//take into account the reminder spacing and only show the reminder that would actually be played
		SharedPreferences prefs = PreferenceManager
        .getDefaultSharedPreferences(getBaseContext());
		int reminderSpacing = prefs.getInt("minimum_reminder_spacing", 15);
		int lastReminder = 0;
		
		//****Process all nights********************************************************
		nightCounter = 1;
		if(D)System.out.println("Rewriting timestamps");
		
		sleepCycles = new ArrayList<SleepCycleVO>();
		if(historyList.size()>0){
		for(JSONObject night: historyList){
			lastReminder = 0;//reset the reminder played
			try{
			//retrieve the total sleep duration json array and iterate over it
			JSONArray totalSleepDuration = night.getJSONArray("totalSleepDuration");
			JSONArray dreams = night.getJSONArray("userEventDream");
			
			 HashMap<Integer,Integer> dreamsMap=null;
			 
		
			 
			//initialize arrays used to create graph data in X,Y format
			 temp = new JSONArray();
			 temp2 = new JSONArray();
			 //Array of X,Y datapoints
			 formattedArray = new JSONArray();
			 //Array used to store when smart timer thinks REM reminders should be played
			 remPredictionReminders = new JSONArray();
			
			 
			 //data analysis object
			 SharedPreferences customSharedPreference = getSharedPreferences(
	                 "LucidDreamingAppPreferences", Context.MODE_PRIVATE);
			 String configFilepath = customSharedPreference.getString("configFilepath",new File(Environment.getExternalStorageDirectory(),new File(LucidDreamingApp.APP_HOME_FOLDER, "SmartTimerConfig.txt.gzip").getAbsolutePath()).getAbsolutePath());
			 smartTimer=(new SmartTimer(night.getString("date"),configFilepath));
			
						 
			 
			 //get minutes for dreams from the timestamp
			 //so much trouble because dreams are stored as individual events, so we cannot 
			 //simply use their original index as the dream minute
			 boolean[]dreamsArray = new boolean[totalSleepDuration.length()];
				Arrays.fill(dreamsArray, false);
				try{
					//convert from a timestamp to a minute since the time user went to sleep
					long firstTimeStamp = totalSleepDuration.getJSONArray(0).getLong(0);
					dreams = night.getJSONArray("userEventDream");
					if(D) Log.e(TAG,"dreams array length:"+dreams.length());
					for (int i = 0; i<dreams.length();i++){
						if( (Integer)(dreams.getJSONArray(i).get(1))>0){
							long tempTimeStamp = (Long)(dreams.getJSONArray(i).get(0));
							dreamsArray[(int)((tempTimeStamp-firstTimeStamp)/60000)]=true;
						}
					}}catch(Exception e){if(D)e.printStackTrace();}
					
					
			//make sure this thing is working! 
			if(D){Log.e(TAG,Arrays.toString(dreamsArray));
			for(int i = 0; i<dreamsArray.length;i++){
				if(dreamsArray[i]){
					if(D)Log.e(TAG, "Dreams Array, dream at: "+i);
				}
			}
			
			}
			//find the longest night duration to scale the rem guess appropriately
			if(totalSleepDuration.length()>longestNightDuration){
				longestNightDuration=totalSleepDuration.length();
			}
			
			//process all sleep epochs and rewrite timestamps
			for(int i =0;i<totalSleepDuration.length();i++){
				//rewrite the timestamp to pretend that sleep happened between Jan1 1970 evening and Jan2 1970 morning
				temp = totalSleepDuration.getJSONArray(i);
				
				//rewrite timestamp
				timestamp = rewriteTimestamp(((Long)temp.get(0)).longValue());//timestamp
				
				//save the first and last timestamps, as they indicate time going to bed and getting out of bed
				if(i==0){
					statGoingToSleep.addValue(timestamp);
				}else if(i==totalSleepDuration.length()-1 ){
					statWakingUp.addValue(timestamp);
					
				}
				
				
				temp2 = new JSONArray();
				temp2.put(timestamp); //put the new timestamp - x value
				temp2.put(temp.get(1));//put the sleep duration - y value
				
				formattedArray.put(temp2);//save the new history array
							
				
			}
			
	
			//store the formatted night's data, along with it's label
			//sleep duration
			historyObject.put("night"+nightCounter, formattedArray);
			
			
			formattedArray = new JSONArray();
			long firstTimestamp = 0;
			try{
				//create a sleep score plot hour by hour to help with identifying sleep cycles
				JSONArray coleSleepScore = night.getJSONArray("coleSleepScore");
				 temp = new JSONArray();
				 temp2 = new JSONArray();
			
			for(int i = 0; i<coleSleepScore.length();i++){
				if(i == 0){
					//TODO include normalized first timestamp as 180 minutes before 3:00 dream
					firstTimestamp = (Long)coleSleepScore.getJSONArray(0).get(0);
					//firstTimestamp = normalizeTimestamp(dreams, firstTimestamp);
					//dreamsMap = getDreamEpochs(dreams,firstTimestamp);
					if(D)System.out.println(dreamsMap);
				}
				
				timestamp = (Long)coleSleepScore.getJSONArray(i).get(0);
				double value = coleSleepScore.getJSONArray(i).getDouble(1);
				
				//avoid negative dates
				if(timestamp-firstTimestamp>0){
				
				temp2 = new JSONArray();
				//new timestamp corresponds to minutes of sleep since sleep began
				temp2.put(timestamp-firstTimestamp); //put the new timestamp - x value
				temp2.put(value);//put the sleep duration - y value
				
				formattedArray.put(temp2);//save the new history array
				
				//now simulate this night and see how smart timer would respond
				
				smartTimer.updateSleepScore(value);
				if(dreamsArray[i]){
					if(D)System.out.println("Sending a dream event at "+i+ " to smart timer");
					smartTimer.userEvent();
				}
				
				//SmartTimer Test
					smartTimer.epochChanged(i);
				
					//now save when the smart timer thinks the reminder should be played
					temp2 = new JSONArray();
					temp2.put(timestamp - firstTimestamp);
					if(smartTimer.guessREM()){
						if(i>lastReminder+reminderSpacing){
							lastReminder=i;
							if(D)Log.e(TAG, "reminder at: "+i+ " minutes");
						temp2.put(25+nightCounter);//make sure each night is above the next
						}
					}else{
						//do not put anything to avoid cluttering the graph at around 0
					}				
				
					remPredictionReminders.put(temp2);
				}
				
			}
			}catch(Exception e){if(D)e.printStackTrace();}
			
			//sleep score
			historyObject.put("night"+nightCounter+"SleepScore", formattedArray);
			
			if(D)System.out.println(historyObject.get("night"+nightCounter+"SleepScore").toString());
			
			//##########process dreams and lucid dreams###########
		
			processUserEvent(night,historyObject,"userEventDream","dreams",-7,firstTimestamp);
			//processUserEvent(night,historyObject,"userEvent","dreams",+13,firstTimestamp);//TODO REMOVE after testing
			processUserEvent(night,historyObject,"userEventLucidDream","lucidDreams",-6,firstTimestamp);
			processUserEvent(night,historyObject,"userEventAwake","awake",-10,firstTimestamp);
			processUserEvent(night,historyObject,"userEventNoDream","awake",-10,firstTimestamp);
			
			accumulateStatistics(night);
			
			if(D)Log.e(TAG,"Establishing sleep cycles: ");
			
			//TODO Change the names of these after testing
			historyObject.put("sleepCycle"+nightCounter+"Label", "Reminder"+nightCounter);
			historyObject.put("sleepCycle"+nightCounter, remPredictionReminders);
			
//			SleepCycleEstimator.initialize(getBaseContext());
//			
//			List<SleepCycleVO> list = SleepCycleEstimator.establishSleepCycles(night,nightCounter);
//			
//			
//			
//			
//			
//			
//			if(list.size()>0){
//				
//				for(SleepCycleVO vo: list){
//					
//					if(vo.sleepCycleNumber==1){
//					//TODO print out the cycles
//					System.out.println(vo);
//					//sleep start
//					temp = new JSONArray();
//					temp.put(vo.sleepCycleStartMinute*60000);
//					temp.put(33+nightCounter);
//					try{
//					historyObject.accumulate("sleepCycle"+nightCounter, temp);}catch(JSONException e){if(D)e.printStackTrace();}
//					}
//					temp = new JSONArray();
//					temp.put(vo.sleepCycleEndMinute*60000);
//					temp.put(33+nightCounter);
//					try{
//					historyObject.accumulate("sleepCycle"+nightCounter, temp);}catch(JSONException e){if(D)e.printStackTrace();}
//					
//				}
//				
//			
//				
//				try{
//				historyObject.put("sleepCycle"+nightCounter+"Label", "Sleep Cycle "+nightCounter);
//				//historyObject.put("sleepCycleEndLabel", "Sleep Cycle End");
//				}catch(JSONException e){if(D)e.printStackTrace();}
//				
//				
//				
//				sleepCycles.addAll(list);
//			}
			
			
			historyObject.put("dreamsLabel", getString(R.string.graph_label_user_event_dream));
			historyObject.put("lucidDreamsLabel", getString(R.string.graph_label_user_event_lucid_dream));
			historyObject.put("awakeLabel", getString(R.string.graph_label_user_event_awake));
			
			
			historyObject.put("night"+nightCounter+"Label", getDate(night));
			
			
			if(D)System.out.println("processed night: "+"night"+nightCounter);
			if(D)System.out.println("processed night: "+"night"+nightCounter+"Label");	
			
			
			//historyObject.put("night"+nightCounter+"Label", "night"+nightCounter);
			nightCounter++;
			
			historyObject.put("hypnogram", SleepCycleEstimator.createHypnogram(0));
			
			historyObject.put("title", getString(R.string.graph_label_sleep_history));
			historyObject.put("filetype", "LucidDreamingApp History" );
			}catch(JSONException e){if(D)e.printStackTrace();}
			
			
		
		
		
		
		}
		if(D)System.out.println("historyObject "+historyObject);	
		handler = new JSONDataHandler();
		//SleepCycleEstimator.analyzeSleepCycles(sleepCycles,false);
		//processSleepCycles(sleepCycles);
		processStatistics(historyObject);	
		releaseStatistics();
		
		
		int[] remEvents = smartTimer.describeREMPeriods();
		formattedArray = new JSONArray();
		if(remEvents!=null){
			int length = 0;
			if(longestNightDuration>remEvents.length){
				length=remEvents.length;
			}else{length=longestNightDuration;}
			
		for(int i= 0;i<length;i++){
			//add a broken line for where REM periods are guessed to be
			//do not add 0 values to avoid cluttering the graph
			try{
			if(remEvents[i]>0){
				
			temp = new JSONArray();
			temp.put(i*60000);//timestamp
			temp.put(remEvents[i]);
			formattedArray.put(temp);
				
			}}catch(Exception e){}//do nothing if we step out of bounds
			
		}
				
		}else{if(D)Log.e(TAG,"REM Events array is null");}
		try{
			if(D)Log.e(TAG,"REM Events array: "+formattedArray.toString());
			historyObject.put("remEvents",formattedArray);}catch(JSONException e){}
		loadPage();
//		handler.setJson1(historyObject);
//	
//		 graphView.addJavascriptInterface(handler, "javahandler");        
//         graphView.loadUrl("file:///android_asset/html/history_from_file.html");
         
		}
	}
	
	
	private HashMap<Integer,Integer> getDreamEpochs(JSONArray dreams, long firstTimestamp){
		
		HashMap<Integer,Integer>map = new HashMap<Integer,Integer>();
		int dreamsCounter = 0;
		for(int i =0;i<dreams.length();i++){
			try{
			long temp = dreams.getJSONArray(i).getLong(0);
			
			if(temp-firstTimestamp>0){
				
				//now temp is a timestamp from the 1970 epoch in millis
				//convert to minutes
				temp = ((temp-firstTimestamp)-((temp-firstTimestamp)%60000)/60000); //strip out extra millis, convert to int
			
				map.put((int)temp, ++dreamsCounter);
			}
			
			
			}catch(Exception e){}
		
		}
		return map;
		
	}
	
	private long normalizeTimestamp(JSONArray dreams, long timestamp){
		
		
		for(int i =0;i<dreams.length();i++){
			try{
			long temp = dreams.getJSONArray(i).getLong(0);
			
			//get the dream's milliseconds since night's start.
			//if the dream within 2.5 and 3.5 hours from the start, normalize the timestamp to
			//think that this dream took place at 3:00
			if( (temp-timestamp)>9000000 && (temp-timestamp)<12600000 ){
				//temp +correction constant = timestamp +3hours
				//correction constant = (timestamp) +3 hours - temp
				//return timstamp +correction constant
				
				long correctionConstant = timestamp +3*60*60*1000-temp;
				return timestamp + correctionConstant;
			}
			
			}catch(JSONException e){}
		}
		
		return timestamp;
	}
	
	
	
	//calculate sleep statistics
	DescriptiveStatistics statSol;
	DescriptiveStatistics statAwakenings;
	DescriptiveStatistics statMinutesAsleep;
	DescriptiveStatistics statTimeInBed ;
	DescriptiveStatistics statUserEvents ;
	DescriptiveStatistics statDreams;
	DescriptiveStatistics statLucidDreams ;
	DescriptiveStatistics statLongestSleepEpisode;
	DescriptiveStatistics statRemindersPlayed ;
	
	DescriptiveStatistics statGoingToSleep,statWakingUp;
	
	private void initializeStatistics(){
		 statSol = new DescriptiveStatistics();
		 statAwakenings = new DescriptiveStatistics();
		 statMinutesAsleep = new DescriptiveStatistics();
		 statTimeInBed = new DescriptiveStatistics();
		 statUserEvents = new DescriptiveStatistics();
		 statDreams = new DescriptiveStatistics();
		 statLucidDreams = new DescriptiveStatistics();
		 statLongestSleepEpisode = new DescriptiveStatistics();
		 statRemindersPlayed = new DescriptiveStatistics();
		 
		 statGoingToSleep = new DescriptiveStatistics();
		 statWakingUp= new DescriptiveStatistics();
	}
	
	//allow them to be garbage collected
	private void releaseStatistics(){
		statSol=null;
		statAwakenings=null;
		statMinutesAsleep=null;
		statTimeInBed=null;
		statUserEvents=null;
		statDreams=null;
		statLucidDreams=null;
		statLongestSleepEpisode=null;
		statRemindersPlayed=null;
		
		statGoingToSleep=null;
		statWakingUp=null;
	}
	
	
	private void processStatistics(JSONObject json){
		try{
		json.put("averageTimeInBed", (int)statTimeInBed.getMean());
		}catch(Exception e){if(D)e.printStackTrace();}
		
		try{
			json.put("totalTimeInBed", (int)statTimeInBed.getSum());
			}catch(Exception e){if(D)e.printStackTrace();}
			
			try{
				json.put("averageMinutesAsleep", (int)statMinutesAsleep.getMean());
				}catch(Exception e){if(D)e.printStackTrace();}	
			try{
				json.put("totalMinutesAsleep", (int)statMinutesAsleep.getSum());
				}catch(Exception e){if(D)e.printStackTrace();}
		//Max 	
		try{
			json.put("longestSleepEpisode", (int)statLongestSleepEpisode.getMean());
			}catch(Exception e){if(D)e.printStackTrace();}
					
		try{
			json.put("sleepOnsetLatency", (int)statSol.getSum());
			}catch(Exception e){if(D)e.printStackTrace();}
			try{
				json.put("averageSleepOnsetLatency", (int)statSol.getMean());
				}catch(Exception e){if(D)e.printStackTrace();}
				
				try{
					json.put("numberOfAwakenings", (int)statAwakenings.getSum());
					}catch(Exception e){if(D)e.printStackTrace();}
					try{
						json.put("averageNumberOfAwakenings", (int)statAwakenings.getMean());
						}catch(Exception e){if(D)e.printStackTrace();}
									
						try{
							json.put("averageNumberOfUserEvents", (int)statUserEvents.getMean());
							}catch(Exception e){if(D)e.printStackTrace();}
			
						try{
							json.put("numberOfUserEvents", (int)statUserEvents.getSum());
							}catch(Exception e){if(D)e.printStackTrace();}
							
							
							try{
								json.put("averageNumberOfVoiceReminders", (int)statRemindersPlayed.getMean());
								}catch(Exception e){if(D)e.printStackTrace();}
								
								try{
									json.put("numberOfVoiceReminders", (int)statRemindersPlayed.getSum());
									}catch(Exception e){if(D)e.printStackTrace();}
									
									
									try{
										json.put("averageNumberOfDreams", (int)statDreams.getMean());
										}catch(Exception e){if(D)e.printStackTrace();}
										
										try{
											json.put("numberOfDreams", (int)statDreams.getSum());
											}catch(Exception e){if(D)e.printStackTrace();}
									
											try{
												json.put("averageNumberOfLucidDreams", (int)statLucidDreams.getMean());
												}catch(Exception e){if(D)e.printStackTrace();}
												
												try{
													json.put("numberOfLucidDreams", (int)statLucidDreams.getSum());
													}catch(Exception e){if(D)e.printStackTrace();}
					
													
													
			long averageGoingToSleepTime = (long)statGoingToSleep.getMean();	
			
			Date sleepStart = getDateInTimeZone(new Date(averageGoingToSleepTime),"UTC");
			if(D) System.out.println(sleepStart.toString());
			
			Format formatter = new SimpleDateFormat("HH:mm"); 
			formatter.format(sleepStart);
			
			
			try{
				json.put("averageGoingToSleepTime", formatter.format(sleepStart));
				}catch(Exception e){if(D)e.printStackTrace();}
			
			
			
			
			long averageWakeUpTime = (long)statWakingUp.getMean();	
			
			Date sleepEnd = getDateInTimeZone(new Date(averageWakeUpTime),"UTC");
			if(D) System.out.println(sleepEnd.toString());
			
			try{
				json.put("averageWakingUpTime", formatter.format(sleepEnd));
				}catch(Exception e){if(D)e.printStackTrace();}
			
			
			
													
	}
	
	

	
	

	
	
	private void processSleepCycles(List<SleepCycleVO> sleepCycles){
		
		
		if(D){
			System.out.println("Sleep cycles");
			System.out.println(sleepCycles.toString());
		}
		if(sleepCycles.size()>0){
		DescriptiveStatistics statBegin = new DescriptiveStatistics();
		DescriptiveStatistics statEnd = new DescriptiveStatistics();
		
		
		for(int i = 1;i<11;i++){//process up to 10 sleep cycles
			
			for(SleepCycleVO vo: sleepCycles){
				//populate the statistical objects
				if(vo.sleepCycleNumber==i){
					statBegin.addValue(vo.sleepCycleStartMinute);
					statEnd.addValue(vo.sleepCycleEndMinute);
				}	
				
				
			}//end inner for
			JSONArray temp = new JSONArray();
			if(statBegin.getN()>0){
			//create a sawtooth graph for sleep cycles
			
			temp.put((int)(statBegin.getMean()*60000)); //timestamp in milliseconds since 0
			temp.put(30);
			try{
			historyObject.accumulate("sleepCycleStart", temp);}catch(JSONException e){if(D)e.printStackTrace();}
			}
			if(statEnd.getN()>0){
			temp = new JSONArray();
			temp.put((int)(statEnd.getMean()*60000)); //timestamp in milliseconds since 0
			temp.put(40);
			try{
			historyObject.accumulate("sleepCycleStart", temp);}catch(JSONException e){if(D)e.printStackTrace();}
			}
			statBegin.clear();
			statEnd.clear();
			
			
			
			//TODO add average sleep cycle begin and end times to history object
			
		}//end outer for
		
		}//end if greater than 0
		
		
	}
	
	
	
	
	/**Accumulates datat to create history statistics from the nights passed in
	 * 
	 * @param night Night JSONObject to collect data from	 
	 */
	private void accumulateStatistics(JSONObject night){
		//TODO sum sleep statistics to be displayed below the graph
		
		try{
			statSol.addValue(night.getInt("sleepOnsetLatency"));
		}catch(Exception e){if(D)e.printStackTrace();}
		
		try{
			statAwakenings.addValue(night.getInt("numberOfAwakenings"));
		}catch(Exception e){if(D)e.printStackTrace();}
		
		try{
			statMinutesAsleep.addValue(night.getInt("totalMinutesAsleep"));
		}catch(Exception e){if(D)e.printStackTrace();}
		
		try{
			statTimeInBed.addValue(night.getInt("totalTimeInBed"));
		}catch(Exception e){if(D)e.printStackTrace();}
		
		try{
			statUserEvents.addValue(night.getInt("numberOfUserEvents"));
		}catch(Exception e){if(D)e.printStackTrace();}
		
		try{
			statDreams.addValue(night.getInt("numberOfDreams"));
		}catch(Exception e){if(D)e.printStackTrace();}
		
		try{
			statLucidDreams.addValue(night.getInt("numberOfLucidDreams"));
		}catch(Exception e){if(D)e.printStackTrace();}
		
		try{
			statLongestSleepEpisode.addValue(night.getInt("longestSleepEpisode"));
		}catch(Exception e){if(D)e.printStackTrace();}
		
		try{
			statRemindersPlayed.addValue(night.getInt("numberOfVoiceReminders"));
		}catch(Exception e){if(D)e.printStackTrace();}
		
	}
	
	
	private String getDate(JSONObject night){
		String returnStr = "";
		
		try{
		//get the timestamp when the app started	
		long timestamp = night.getLong("firstTimestamp");
		//long timezoneOffset = night.getLong("timeZoneOffset");
		
		//Crazy BS just to get time in UTC timezone...
		Calendar calendar = new GregorianCalendar();		
		calendar.setTimeInMillis(timestamp);
		calendar.setTimeZone(TimeZone.getTimeZone("UTC"));
		
		
		Date d = getDateInTimeZone(calendar.getTime(),"UTC");
		
		//get date components
		//String[] arr=calendar.getTime().toString().split("\\s");
		if(D)System.out.println(calendar.getTime().toString());
		if(D)System.out.println(d.toString());
		if(D)System.out.println(calendar.getTime().toGMTString());
		if(D)System.out.println(calendar.get(Calendar.HOUR_OF_DAY));
//		Date date = new Date();
//		date.setTime(timestamp+calendar.getTime().getTimezoneOffset());
		//
		//System.out.println("dow mon day"+arr[0]+" "+arr[1]+" "+arr[2]);
		
		Format formatter = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm"); 
		returnStr =formatter.format(d);
		
		}catch (JSONException e){
			try{returnStr = night.getString("date");}catch(JSONException ex){returnStr = "night";}
		}
		return returnStr;
	}
	
	
	
	public static Date getDateInTimeZone(Date currentDate, String timeZoneId)
	{
	//TimeZone tz = TimeZone.getTimeZone(timeZoneId);
	Calendar mbCal = new GregorianCalendar(TimeZone.getTimeZone(timeZoneId));
	mbCal.setTimeInMillis(currentDate.getTime());

	Calendar cal = Calendar.getInstance();
	cal.set(Calendar.YEAR, mbCal.get(Calendar.YEAR));
	cal.set(Calendar.MONTH, mbCal.get(Calendar.MONTH));
	cal.set(Calendar.DAY_OF_MONTH, mbCal.get(Calendar.DAY_OF_MONTH));
	cal.set(Calendar.HOUR_OF_DAY, mbCal.get(Calendar.HOUR_OF_DAY));
	cal.set(Calendar.MINUTE, mbCal.get(Calendar.MINUTE));
	cal.set(Calendar.SECOND, mbCal.get(Calendar.SECOND));
	cal.set(Calendar.MILLISECOND, mbCal.get(Calendar.MILLISECOND));

	return cal.getTime();
	}
	
	
	
	public static long dayMillis = 24*60*60*1000;//milliseconds in a day
	public static long halfDayMillis = dayMillis/2; //used to roll the day over
	public static long minuteMillis = 60000; //used to truncate seconds from minutes
	
	public static long rewriteTimestamp(long timestamp){
		//get a timestamp as of Jan 1 1970
		timestamp = (timestamp % dayMillis) -(timestamp%minuteMillis);
		
		//if Timestamp is before 12PM, pretend that the awakening is on January 2 1970
		//(add a full day)
		if(timestamp <halfDayMillis){
			timestamp += dayMillis;
		}//else the timestamp represents the evening of Jan1 1970
		
		return timestamp;
		
	}
	
	/**Generates an array of x,y pairs for user events. User events are normally stored as individual occurences
	 * 
	 * @param fromJSON
	 * @param toJSON
	 * @param fromField
	 * @param toField
	 * @param offset
	 * @param startTimestamp
	 */
	private void processUserEvent(JSONObject fromJSON, JSONObject toJSON, String fromField, String toField, int offset, long startTimestamp){
		JSONArray utilityArray = new JSONArray();
		JSONArray sleepScoreArray = new JSONArray();
		JSONArray temp = new JSONArray();
		JSONArray temp2 = new JSONArray();
		
		long timestamp;
		try{
		utilityArray = fromJSON.getJSONArray(fromField);
		sleepScoreArray = fromJSON.getJSONArray("coleSleepScore");
		if(utilityArray.length()>0){
			for(int i = 0;i<utilityArray.length();i++){
					
					temp =utilityArray.getJSONArray(i);//get the dreams timestamp and y position
					timestamp = ((Long)temp.get(0));
					
					
					//avoid negative dates
					if(timestamp-startTimestamp>0){
					temp2 = new JSONArray();
					temp2.put(timestamp-startTimestamp);
					
				
					int sleepMinute = (int)((timestamp-startTimestamp)/60000);//convert timestamp to minute
					
					if(D)Log.w(TAG,"processing user event at: "+sleepMinute);
					//get the sleep score value for that minute
					
					double value = sleepScoreArray.getJSONArray(sleepMinute).getDouble(1);
					if(D)Log.w(TAG,"Sleep Score value: "+value);
//					try{
//						temp2.put((int)value);
//					}
					try{temp2.put(((Integer)temp.get(1)).intValue()+offset);}
					catch (Exception e){temp2.put(temp.get(1));}//in case it is a float or something
					toJSON.accumulate(toField, temp2);
					}
				}
		}
		}catch(JSONException e){}
	}
    
    private void loadPage(){
    	
    	if(D)Log.w(TAG,"Time to load: " +(System.currentTimeMillis()-startTime)/1000 +" seconds");
    			try{
        	//	setTitle(dataFile.getName());
        		handler = new JSONDataHandler();
        		
	        		try{
	        		SharedPreferences prefs = PreferenceManager
	                .getDefaultSharedPreferences(getBaseContext());        		
	        		
	        		
	    		//overwrite the preferences with the current ones
	        		historyObject.put("activityCountYMax", prefs.getInt("activity_count_y_axis_max", 2500));
	        		historyObject.put("sleepScoreYMax", prefs.getInt("sleep_score_y_axis_max", 35));
	        		}catch(Exception e){if(D)e.printStackTrace();}
        		
        		
        		
        		handler.setJson1(historyObject);
        		
        		 graphView.addJavascriptInterface(handler, "javahandler");
        		 //graphView.loadUrl("file:///android_asset/html/history_from_file.html");
        		 graphView.loadUrl("file:///android_asset/html/dual_history_from_file.html");
        		 //graphView.loadUrl("file:///android_asset/html/zoom_show_data_from_file.html");
    			}catch(Exception e){
        			sendShortToast("Error displaying history");
        		}
    	
    	
    	
         // graphView.loadUrl("file:///android_asset/html/basic_plot_from_handler.html");
         
         // graphView.loadUrl("file:///android_asset/html/zooming.html");
    	
    }
	
	
    private String getHistoryFileName(){
    	
    		return getHistoryFileName(".txt");
    	
    }
    
    private String getHistoryFileName(String suffix){
    	SimpleDateFormat format = new SimpleDateFormat("yyyyMMdd_HHmm");
		
		StringBuilder sb = new StringBuilder();
		sb.append("Hist_");
		sb.append(format.format(Calendar.getInstance().getTime()));
		sb.append(suffix);
		return sb.toString();
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
    
    private class SaveScreenshotTask extends AsyncTask<Void, Void, Void> {
	     
		 boolean success = false;
		 File dir;
		 
		 protected Void doInBackground(Void... urls) {
			 try{
			 if(historyObject!=null && graphView!=null){
	        		dir = new File(Environment.getExternalStorageDirectory(),LucidDreamingApp.SCREENSHOT_LOCATION);
	        		success = ((GlobalApp)getApplication()).savePicture(graphView.capturePicture(), 
	        				dir+"/"+historyFilename.replaceAll("[.]txt",".png").replaceAll("[.]gzip","") , 50);
	        		}else{
	        			sendShortToast("Please load a graph first");
	        		}
			 
			 }catch(Exception e){
				 if(D)e.printStackTrace();
			 }
	        return null;
	     }

		@Override
		protected void onPostExecute(Void result) {
			// TODO Auto-generated method stub
			super.onPostExecute(result);
			
			if(success&&dir!=null){
				sendShortToast("Saved to: "+dir.getAbsolutePath());
			}else{
				sendShortToast("Could not save screenshot");
			}
		}
		 
		 

	     
	 }
	
	
}
