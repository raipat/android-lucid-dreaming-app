package com.luciddreamingapp.beta;

import org.json.JSONObject;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.provider.Settings.SettingNotFoundException;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.WebView;
import android.widget.Toast;

import com.luciddreamingapp.beta.util.DataManagerObserver;
import com.luciddreamingapp.beta.util.SleepDataManager;
import com.luciddreamingapp.beta.util.SleepDataPoint;

public class RealTimeGraphActivity extends Activity implements DataManagerObserver {

	private static final String TAG = "Lucid Dreaming App Graph";//tag for LogCat and eclipse log
	private static final boolean D = false;//debug
	
	private  WebView graphView;
	private SleepDataManager dataManager;
	
	private JSONDataHandler graphDataInterface; 
	
	private Handler handler;
	
	
	private boolean brightnessAdjustEnabled = false;
	int brightnessMode = 0;
	float brightness = 0.25f;
	float tempBrightness = brightness+0.02f;
	
	private JSONObject graphData;
	
	 private WakeLock wakeLock;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.accelerometer_graph);
		if(D)Log.e(TAG, "+ON CREATE+");
		dataManager = SleepDataManager.getInstance();
		dataManager.addObserver(this, SleepDataManager.GRAPH_UPDATED);
		
		handler = new Handler();
		
		 graphView = (WebView) findViewById(R.id.graph_view);
         graphView.getSettings().setJavaScriptEnabled(true);
         graphView.getSettings().setLoadWithOverviewMode(true);
         graphView.getSettings().setUseWideViewPort(true);
         graphView.getSettings().setBuiltInZoomControls(true);
         graphDataInterface= new JSONDataHandler();
         graphData = dataManager.getEpochGraphJSON();
         graphDataInterface.setJson1(graphData);
         graphView.addJavascriptInterface(graphDataInterface, "javahandler");
         graphView.loadUrl("file:///android_asset/html/black_show_data_from_file.html");
		
      
	}

	@Override
	protected void onDestroy() {
		// TODO Auto-generated method stub
		super.onDestroy();
		dataManager.unregisterObserver(this, SleepDataManager.GRAPH_UPDATED);
		if(D)Log.e(TAG, "---ON DESTROY---");
	}

	
	

	@Override
	protected void onResume() {
		// TODO Auto-generated method stub
		super.onResume();
		if(D)Log.e(TAG, "+++ON RESUME+++");
		dataManager.addObserver(this, SleepDataManager.GRAPH_UPDATED);
		
		SharedPreferences prefs = PreferenceManager
        .getDefaultSharedPreferences(getBaseContext());
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
  				e.printStackTrace();
  			}
  			  
  			 
  		
  			
  			//set brightness
  			int tempInt = (prefs.getInt("brightness_level", 10));
  			if(tempInt>0){
  				
  				sendShortToast(getString(R.string.toast_brightness_level,tempInt)+"%");
  				
  			brightness = tempInt/100f;
  			}else{
  				sendShortToast(getString(R.string.toast_brightness_level,1)+"%");
  				brightness = 0.01f;
  			}
  			if(D)Log.e(TAG, "pref brightness "+brightness);
  			
//  			if(NightGUIActivity.wakeLock!=null && !NightGUIActivity.wakeLock.isHeld() ){
//  				try{
//		        	//acquire application context and get a reference to power service
//		           Context context = getApplicationContext();
//		            PowerManager pm = (PowerManager)context.getSystemService(
//		                    Context.POWER_SERVICE);
//		          
//		            //create a wake lock to prevent phone from sleeping (screen dim/cpu on)
//		                      
//		           wakeLock = pm.newWakeLock(
//		                PowerManager.SCREEN_DIM_WAKE_LOCK,"Lucid Dreaming Graph");
//		                wakeLock.acquire();
//		               
//		                if(D){System.out.println("Wakelock held: "+wakeLock.isHeld());}
//		              
//		        }catch(Exception e){
//		        	if(D)Log.e(TAG,"Error aquiring wakelock");
//		        	e.printStackTrace();
//		        }
//  			}
  			
  			
  		//	setBrightness(brightness/100);
  			
  		}
	}
	
	@Override
	protected void onPostResume() {
		// TODO Auto-generated method stub
		super.onPostResume();
		
		brightness = NightGUIActivity.brightness;
        tempBrightness=  brightness+0.02f;
        handler.post(new ReduceBrightnessTask());
		if(D)Log.e(TAG, "++++ON POST RESUME++++");
		   handler.postDelayed(new firstUpdateRunnable(),2000);
	}

	@Override
	protected void onPause() {
		// TODO Auto-generated method stub
		super.onPause();
		dataManager.unregisterObserver(this, SleepDataManager.GRAPH_UPDATED);
		if(D)Log.e(TAG, "-ON PAUSE-");
		
//		if(wakeLock!=null &&wakeLock.isHeld()){
//			wakeLock.release();
//		}
	}
	

	
	
	@Override
	public void dataPointAdded(SleepDataPoint epoch) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void dataPointUpdated(SleepDataPoint epoch) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void listUpdated(String innerHTML) {
		// TODO Auto-generated method stub
		
	}

	class UpdateRunnable implements Runnable{
		
		
		UpdateRunnable(JSONObject graphData){
			RealTimeGraphActivity.this.graphData = graphData;
			updateGraph();
			if(D)Log.e(TAG, "Graph Updated");
		}
		public void run(){
			
		}
	}
	
	@Override
	public void graphUpdated(JSONObject graphData) {
		// TODO Auto-generated method stub
		
		handler.post(new UpdateRunnable(graphData));
		
	}

	@Override
	public void dataReset() {
		// TODO Auto-generated method stub
		
	}

	
	public void updateGraph(){
		//final String s = text;
        handler.post(new Runnable() {
        	
            public void run() {
            	if(D)Log.e(TAG, "Update Runnable");
            	sendShortToast("Graph is updating");
            	//load fresh graph data, add javascript interface and ask the page to update itself
            	graphDataInterface.setJson1(graphData);			
    			graphView.addJavascriptInterface(graphDataInterface, "javahandler");
    			graphView.reload();
            	
            		
            }
        });
		
	
	}
	
	@Override
	 public boolean onCreateOptionsMenu(Menu menu) {
	        MenuInflater inflater = getMenuInflater();
	        inflater.inflate(R.menu.accelerometer_monitoring_activity_menu, menu);
	        menu.removeItem(R.id.menu_show_graph);
	        return true;
	    }
	 @Override
	    public boolean onOptionsItemSelected(MenuItem item) {
	        switch (item.getItemId()) {

	        
	        case R.id.menu_show_list:
	        	Intent listIntent = new Intent(getBaseContext(),
	                    RealTimeListActivity.class);
	        	listIntent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
	        		startActivity(listIntent);
	        //	finish();
	        	return true;
	        	
	        case R.id.menu_show_graph:
	        //finish();
	        return true;
	       case R.id.menu_show_clock:
	    	   Intent clockIntent = new Intent(getBaseContext(),
	                    NightGUIActivity.class);

	    	   	clockIntent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
	        		startActivity(clockIntent);
	        	
	     //   finish();
	            
	        	return true;
	        	
	        	
	     
	        //saves the currently displayed graph as a JSON File	
	        case R.id.menu_save_data:
	        	((GlobalApp)getApplication()).saveData();
	        	//TODO fix saving data
	      //  saveData();
	        	
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
	    
	    

		public void setBrightness(float brightness){

			 WindowManager.LayoutParams lp = getWindow().getAttributes();

            lp.screenBrightness = brightness;

            getWindow().setAttributes(lp);
		}
		
		public float getBrightness(){
			 return getWindow().getAttributes().screenBrightness;
		}
		
		class firstUpdateRunnable implements Runnable{
			
			public void run(){
				
				updateGraph();
			}
			
		}
		
		 class ReduceBrightnessTask implements Runnable {
		    	
		        public void run() {      
		        	
		        	if(D)Log.e(TAG,"reducing brightness ");
		        	if(D)Log.e(TAG,"reducing brightness "+tempBrightness + " "+brightness);
		                if (tempBrightness > brightness) {
		                	setBrightness(tempBrightness); //start at 10% bright and go to 0 (screen off)

		                	tempBrightness-=0.01;
		                        handler.postDelayed(new ReduceBrightnessTask(), 75L);
		                }
		        }
		 }

	    
	    
	
}
