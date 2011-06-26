package com.luciddreamingapp.beta;

import java.io.File;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.view.View;
import android.view.Window;
import android.webkit.WebView;
import android.widget.Toast;

import com.luciddreamingapp.actigraph.Calibration;
import com.luciddreamingapp.beta.util.gesturebuilder.GestureBuilderActivity;
import com.luciddreamingapp.beta.util.state.SmartTimer;
import com.luciddreamingapp.beta.util.state.SmartTimerActivity;

public class QuickstartActivity extends Activity {

	private WebView extra_app_view,calibrate_view,gather_data_view,sleep_cycle_view,change_config_view,smart_timer_view, wild_timer_view;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.quickstart);		
		
		extra_app_view = (WebView)findViewById(R.id.extra_app_view);
		calibrate_view = (WebView)findViewById(R.id.calibrate_view);
		gather_data_view = (WebView)findViewById(R.id.gather_data_view);
		sleep_cycle_view = (WebView)findViewById(R.id.sleep_cycle_view);
		change_config_view = (WebView)findViewById(R.id.change_config_view);	
		smart_timer_view =(WebView)findViewById(R.id.smart_timer_view);	
		wild_timer_view=(WebView)findViewById(R.id.wild_timer_view);	
		//chaining loading pages after setup causes weird flicker
		setupWebView(extra_app_view);
		extra_app_view.loadUrl("file:///android_asset/html/quickstart_extra_apps.html");
		setupWebView(calibrate_view);
		calibrate_view.loadUrl("file:///android_asset/html/quickstart_calibration.html");
		setupWebView(gather_data_view);
		gather_data_view.loadUrl("file:///android_asset/html/quickstart_gather_data.html");
		setupWebView(sleep_cycle_view);
		sleep_cycle_view.loadUrl("file:///android_asset/html/quickstart_sleep_cycle.html");
		
		setupWebView(wild_timer_view);
		wild_timer_view.loadUrl("file:///android_asset/html/quickstart_wild_timer.html");
		
		setupWebView(smart_timer_view);
		smart_timer_view.loadUrl("file:///android_asset/html/quickstart_smart_timer_page.html");
		//this is the only view that is resized to reduce flicker
		setupWebView(change_config_view);
		change_config_view.loadUrl("file:///android_asset/html/smart_timer_gui.html");
		change_config_view.getSettings().setLoadWithOverviewMode(true);
		change_config_view.getSettings().setUseWideViewPort(true);
		new GUITask().execute(null);
	}
	
	private WebView setupWebView(WebView view){
		
		view.getSettings().setJavaScriptEnabled(true);
		//view.getSettings().setLoadWithOverviewMode(true);
		//view.getSettings().setUseWideViewPort(true);
		//view.getSettings().setBuiltInZoomControls(true);		
		
		return view;
		
	}
	

	@Override
	protected void onStart() {
		// TODO Auto-generated method stub
		super.onStart();
	}

	
	public void startApp(View view){
		  Intent monitorAccelerometerIntent = new Intent(getBaseContext(), NightGUIActivity.class);
      	 monitorAccelerometerIntent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
      	       
            startActivity(monitorAccelerometerIntent);
	}
	
	public void getDataUploader(View view){
		sendShortToast("Coming soon");
		//	market.android.com/details?id=org.openintents.filemanager
		Intent 	getFileManagerIntent = new Intent(Intent.ACTION_VIEW);
		getFileManagerIntent.setData(Uri.parse("market://details?id=com.luciddreamingapp.uploader"));
		startActivity(getFileManagerIntent);
	}
	
	public void getSoundRecorder(View view){
		
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
        		sendShortToast("Please install HI-Q MP3 Recorder from market");                                        				
    			}
    			
    		}else{
    			//start the file normally
    			sendShortToast("HI-Q MP3 Recorder already installed");
    		}
    		
    	}catch(Exception e){
    		e.printStackTrace();
    		getStartRecorder = new Intent(Intent.ACTION_VIEW);
    		getStartRecorder.setData(Uri.parse("market://details?id=yuku.mp3recorder.lite"));
    		
    	}
    	
    	
		startActivity(getStartRecorder);
    	
		
	}
	
	public void getFileManager(View view){
		
	//	market.android.com/details?id=org.openintents.filemanager
	Intent 	getFileManagerIntent = new Intent(Intent.ACTION_VIEW);
	getFileManagerIntent.setData(Uri.parse("market://details?id=org.openintents.filemanager"));
	startActivity(getFileManagerIntent);
	}


	public void startCalibration(View view){
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
    	try{
    	int  accelerometer_calibration_duration_min = prefs.getInt("accelerometer_calibration_duration_min", 15);
    	sendShortToast("Calibration duration : "+accelerometer_calibration_duration_min+" min");
    //	Intent calibrateAccelerometerIntent = new Intent(context, InMotionCalibration.class);
    	Intent calibrateAccelerometerIntent = new Intent(getBaseContext(), Calibration.class);
    	calibrateAccelerometerIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        calibrateAccelerometerIntent.putExtra("accelerometer_calibration_duration_min", accelerometer_calibration_duration_min);
        
    	startActivity(calibrateAccelerometerIntent);
    	}catch(Exception e){}
	}
	
public void disableReminders(View view){
		
	SharedPreferences prefs = PreferenceManager
    .getDefaultSharedPreferences(getBaseContext());
				
	Editor editor = prefs.edit();
	editor.putBoolean("play_sound_pref", false);
	editor.commit();
	
	}

public void showGestures(View view){

	Intent editGesturesIntent = new Intent(getBaseContext(), GestureBuilderActivity.class);
	editGesturesIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);                                                    
    
	startActivity(editGesturesIntent);
}

public void viewData(View view){
	Intent graphingActivity = new Intent(getBaseContext(),
            GraphViewingActivity.class);
	graphingActivity.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
		startActivity(graphingActivity);
}

public void viewHistory(View view){
 	Intent historyIntent = new Intent(getBaseContext(),
            HistoryViewingActivity.class);
 		startActivity(historyIntent);
}

public void changeConfig(View view){
	//sendShortToast("change config");
	
	
    
	 Intent selectFileIntent = new Intent(getBaseContext(), DataFileSelector.class);
		 selectFileIntent.putExtra("filepath", LucidDreamingApp.APP_HOME_FOLDER);
		 //match smart followed or preceeded by anything with an extension of txt or txt.gzip
		 selectFileIntent.putExtra("filterString","(.)*?[Ss]mart(.)*(txt)([.]gzip)??");
		 startActivityForResult(selectFileIntent, Preferences.REQUEST_SELECT_SMART_TIMER_CONFIG_FILE);
	                               
	
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

			   new GUITask().execute(null);
			   
          	Intent smartTimerIntent = new Intent(getBaseContext(), SmartTimerActivity.class);
          	smartTimerIntent.putExtra("eventType", SmartTimerActivity.REM_EVENT);
          	
          	smartTimerIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
  			startActivity( smartTimerIntent);
			      
 			
 			
	 }
    	break;
    	
    }
    
}



public void smartTimerPreferences(View view){
//	Intent settingsActivity = new Intent(getBaseContext(),
//            Preferences.class);
	Intent settingsActivity = new Intent(getBaseContext(),
            MiniPreferenceActivity.class);
		settingsActivity.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
		startActivity(settingsActivity);
}
	


public void advancedOptions(View view){
	Intent settingsActivity = new Intent(getBaseContext(),
            Preferences.class);
	
		settingsActivity.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
		startActivity(settingsActivity);
}
	

protected void sendShortToast(String message){
	
	Toast.makeText(getBaseContext(),
            message,
            Toast.LENGTH_SHORT).show();
	
}


private class GUITask extends AsyncTask<Void, Void, Void> {
	 
	 protected Void doInBackground(Void... urls) {
			


   		SmartTimer smartTimer = new SmartTimer("title",SmartTimerActivity.getSmartTimerConfigFilepath(QuickstartActivity.this));
   		JSONDataHandler jsonHandler  = new JSONDataHandler();
   		jsonHandler.setJson1(smartTimer.describeEvents());
   		change_config_view.addJavascriptInterface(jsonHandler, "javahandler");
   		change_config_view.loadUrl("file:///android_asset/html/smart_timer_gui.html");
		 
			
		 
       return null;
    }

	@Override
	protected void onPostExecute(Void result) {
		// TODO Auto-generated method stub
		super.onPostExecute(result);
		
	}
	
}
	
}
