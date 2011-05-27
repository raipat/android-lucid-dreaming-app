package com.luciddreamingapp.beta;

import org.json.JSONObject;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.gesture.Gesture;
import android.gesture.GestureLibraries;
import android.gesture.GestureLibrary;
import android.gesture.GestureOverlayView;
import android.gesture.GestureOverlayView.OnGesturePerformedListener;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.webkit.WebView;

import com.luciddreamingapp.beta.RealTimeGraphActivity.UpdateRunnable;
import com.luciddreamingapp.beta.util.ActigraphyService;
import com.luciddreamingapp.beta.util.DataManagerObserver;
import com.luciddreamingapp.beta.util.SleepDataManager;
import com.luciddreamingapp.beta.util.SleepDataPoint;
import com.luciddreamingapp.beta.util.gesturebuilder.GestureBuilderActivity;

public class RealTimeListActivity extends Activity implements OnGesturePerformedListener,OnSharedPreferenceChangeListener,DataManagerObserver {
	
	private static final String TAG = "Lucid Dreaming App List";//tag for LogCat and eclipse log
	private static final boolean D = false;//debug
	
	private  GestureLibrary gestureLibrary;     
	private  GestureOverlayView gestures; 
	private  WebView listView;
	
	private Handler handler;
	
	private boolean gesturesEnabled ;
	private boolean calibrationCompleted;
	
	private String sleepStatus = "";
	
	private SleepDataManager dataManager;
	 private WakeLock wakeLock;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		
		SharedPreferences prefs = PreferenceManager
        .getDefaultSharedPreferences(getBaseContext());
		setupViews(prefs);
		if(D)Log.e(TAG, "+ON CREATE+");
		handler = new Handler();
		dataManager = SleepDataManager.getInstance();
		
		dataManager.addObserver(this, SleepDataManager.LIST_UPDATED);
		sleepStatus = dataManager.getSleepStatus();
	}
	
	
	
	

	@Override
	protected void onStart() {
		// TODO Auto-generated method stub
		super.onStart();
		dataManager.addObserver(this, SleepDataManager.LIST_UPDATED);
		sleepStatus = dataManager.getSleepStatus();
	}



	


	@Override
	protected void onDestroy() {
		// TODO Auto-generated method stub
		super.onDestroy();
		if(D)Log.e(TAG, "-ON DESTROY-");
		dataManager.unregisterObserver(this, SleepDataManager.LIST_UPDATED);
	}



	@Override
	protected void onResume() {
		// TODO Auto-generated method stub
		super.onResume();
		if(D)Log.e(TAG, "+++ON RESUME+++");
		dataManager.addObserver(this, SleepDataManager.LIST_UPDATED);
		
	
//				try{
//	        	//acquire application context and get a reference to power service
//	           Context context = getApplicationContext();
//	            PowerManager pm = (PowerManager)context.getSystemService(
//	                    Context.POWER_SERVICE);
//	          
//	            //create a wake lock to prevent phone from sleeping (screen dim/cpu on)
//	                      
//	           wakeLock = pm.newWakeLock(
//	                PowerManager.SCREEN_DIM_WAKE_LOCK,"Lucid Dreaming List");
//	                wakeLock.acquire();
//	               
//	                if(D){System.out.println("Wakelock held: "+wakeLock.isHeld());}
//	              
//	        }catch(Exception e){
//	        	if(D)Log.e(TAG,"Error aquiring wakelock");
//	        	e.printStackTrace();
//	        }
		
		
	}
	
	@Override
	protected void onPostResume(){
		super.onPostResume();
		updateWebViewText();
	}



	@Override
	protected void onPause() {
		// TODO Auto-generated method stub
		super.onPause();
		if(D)Log.e(TAG, "---ON PAUSE---");
		dataManager.unregisterObserver(this, SleepDataManager.LIST_UPDATED);
		
//		if(wakeLock!=null &&wakeLock.isHeld()){
//			wakeLock.release();
//		}
	}



	@Override
	protected void onStop() {
		// TODO Auto-generated method stub
		super.onStop();
		if(D)Log.e(TAG, "--ON STOP--");
		
	}



	public void updateWebViewText(){
		//final String s = text;
        handler.post(new Runnable() {
        	
            public void run() {
            	if(D)Log.e(TAG, "--Update webview--");
            	if(calibrationCompleted){
            	listView.loadUrl("javascript:updateHTML()");
            	}
            	
            		
            }
        });
		
	
	}
	
	public String getSleepStatus(){
		return sleepStatus;
	}

	@Override
	public void onGesturePerformed(GestureOverlayView overlay, Gesture gesture) {
		// TODO Auto-generated method stub
		
	}

	//attempts to respond to preference changes and set up views accordingly
	@Override
	public void onSharedPreferenceChanged(SharedPreferences prefs, String name) {
		
//		if(name.equals("enable_gestures")){
//			boolean temp = prefs.getBoolean(name,false);
//			//if the gesture option is different, reset views
//			if(temp!=gesturesEnabled){				
//				setupViews(prefs);
//			}
//			
//			
//		}else
			if(name.equals("calibration_completed")){
			setupViews(prefs);
		}
	}
	
	
	protected void setupViews(SharedPreferences prefs){
		
			gesturesEnabled =prefs.getBoolean("enable_gestures", false);
			calibrationCompleted =prefs.getBoolean("calibration_completed", false);
//		if(gesturesEnabled){
//			  // with gestures
//			  setContentView(R.layout.accelerometer_list);			  
//	            try{
//	            gestureLibrary= GestureLibraries.fromFile(GestureBuilderActivity.GESTURES_FILE);
//	            
//	            gestures= (GestureOverlayView) findViewById(R.id.gestures);
//	          	         
//	            gestures.addOnGesturePerformedListener(this);	            
//	         
//	            gestureLibrary.load();
//	            gestures.setVisibility(View.VISIBLE);
//	           
//	            }catch(Exception e){if(D)e.printStackTrace();}
//	            }   else{
	            	//without gestures
	            	 setContentView(R.layout.accelerometer_list_no_gestures);
//	            	 }
		
				listView = (WebView)findViewById(R.id.list_view);
				  	
				  	
		        listView.getSettings().setJavaScriptEnabled(true);
		        listView.getSettings().setLoadWithOverviewMode(true);
		        listView.getSettings().setUseWideViewPort(true);
		        listView.getSettings().setBuiltInZoomControls(true);//disable zoom for list?
		        listView.addJavascriptInterface(this, "javahandler");
		        
		        if(!prefs.getBoolean("calibration_completed", false)){
		        	 listView.loadUrl("file:///android_asset/html/instructions.html");
		        }else{
   	        	 listView.loadUrl("file:///android_asset/html/epoch_activity_list.html");
		        }
		
	}

	@Override
	public void dataPointAdded(SleepDataPoint epoch) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void dataPointUpdated(SleepDataPoint epoch) {
		// TODO Auto-generated method stub
		
	}

class UpdateRunnable implements Runnable{
		
		
		UpdateRunnable(String innerHTML){
				sleepStatus = innerHTML;
		}
		public void run(){
					
		updateWebViewText();
		}
	}
	
	
	
	@Override
	public void listUpdated(String innerHTML) {
		handler.post(new UpdateRunnable(innerHTML));
			
	}

	@Override
	public void graphUpdated(JSONObject graphData) {
		
		
	}

	@Override
	public void dataReset() {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	 public boolean onCreateOptionsMenu(Menu menu) {
	        MenuInflater inflater = getMenuInflater();
	        inflater.inflate(R.menu.accelerometer_monitoring_activity_menu, menu);
	        menu.removeItem(R.id.menu_show_list);
	        return true;
	    }
	 @Override
	    public boolean onOptionsItemSelected(MenuItem item) {
	        switch (item.getItemId()) {

	        
	        case R.id.menu_show_list:
	        	finish();
	        	return true;
	        	
	        case R.id.menu_show_graph:
	        	  Intent graphIntent = new Intent(getBaseContext(),
		                    RealTimeGraphActivity.class);
	        	  graphIntent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
		        		startActivity(graphIntent); 	
	       // finish();
	       return true;
	       case R.id.menu_show_clock:
	    	   Intent clockIntent = new Intent(getBaseContext(),
	                    NightGUIActivity.class);
	    	   clockIntent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
	        		startActivity(clockIntent); 	
	      //  finish();
	            
	        	return true;
	        	
	        	
	     
	        //saves the currently displayed graph as a JSON File	
	        case R.id.menu_save_data:
	        	((GlobalApp)getApplication()).saveData();
	        	
	        	
	        	return true;
	        	
	        case R.id.menu_preferences:
	        		Intent settingsActivity = new Intent(getBaseContext(),
	                    Preferences.class);
	        		startActivity(settingsActivity);
	        	
	        	       	
	        	return true;
	        	
	        	
	       
	        	
	        case R.id.menu_exit_app:
	        	
	        	
//	        	stopService(new Intent(this, ActigraphyService.class));
//	        	dataManager.reset();
	        	//stopService(new Intent(this, ActigraphyService.class));
	        	
	        	//System.exit(0);
	        
	        	finish();       	
	        	return true;
	        	
	                
	        }
	        

	        
	  
	        return false;
	    }
	
	
}
