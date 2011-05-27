package com.luciddreamingapp.beta;

import java.util.Timer;
import java.util.TimerTask;

import com.luciddreamingapp.beta.TestActivity.IncreaseTask;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.provider.Settings;
import android.provider.Settings.SettingNotFoundException;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.WebView;

public class Strobe extends Activity {

	private static final boolean D = false;
	private static final String TAG = "Light Strobe for Lucid Dreaming App";
	private long[] timing =null;
	
	private Timer timer;
	//tasks to switch web views
	private StrobeOn strobeOn;
	private StrobeOff strobeOff;
	
	//switch web views to emulate flashing
	private WebView strobeOnView = null;	
	private WebView strobeOffView = null;
	
	private Handler handler;
	private float brightness=0.1f;
	
	private WakeLock wakeLock; 
	private int brightnessMode; 
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		//make window full screen before cranking up the brightness
		
		
		Bundle extras = getIntent().getExtras(); 
    	if(extras !=null)
    	{
    		 timing= extras.getLongArray("strobeTiming");
    		 
    	
    	}
    	if(timing!=null &&timing.length>0){
    		
    		requestWindowFeature(Window.FEATURE_NO_TITLE);
    		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, 
                    WindowManager.LayoutParams.FLAG_FULLSCREEN);
    		
    		try{
	        	//acquire application context and get a reference to power service
	           Context context = getApplicationContext();
	            PowerManager pm = (PowerManager)context.getSystemService(
	                    Context.POWER_SERVICE);
	          
	            //multiple flag wake lock, trying to turn on the screen and wake the phone from sleeping         
	            wakeLock = pm.newWakeLock(
	            		PowerManager.SCREEN_BRIGHT_WAKE_LOCK |PowerManager.ACQUIRE_CAUSES_WAKEUP | PowerManager.ON_AFTER_RELEASE,"Actigraphy Service");
	                wakeLock.acquire();
	               
	                if(D){System.out.println("Wakelock held: "+wakeLock.isHeld());}
	               
	        }catch(Exception e){
	        	if(D)Log.e(TAG,"Error aquiring wakelock");
	        	e.printStackTrace();
	        }
    		
    		try {
  			  //switch to manual brightness mode
  			 brightnessMode = Settings.System.getInt(getContentResolver(), Settings.System.SCREEN_BRIGHTNESS_MODE);
  				if (brightnessMode == Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC) {
  					brightnessMode=Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC;
  				    Settings.System.putInt(getContentResolver(), Settings.System.SCREEN_BRIGHTNESS_MODE, Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL);
  				}
  			} catch (SettingNotFoundException e) {
  				// TODO Auto-generated catch block
  			if(D)	e.printStackTrace();
  			}
    		
    		
    		
    		this.setContentView(R.layout.strobe);
    		strobeOnView = (WebView)this.findViewById(R.id.strobeOnView);    		
    		strobeOnView.getSettings().setLoadWithOverviewMode(true);
    		strobeOnView.getSettings().setUseWideViewPort(true);
    		strobeOnView.setVerticalScrollBarEnabled(false);
    		strobeOnView.setHorizontalScrollBarEnabled(false);
    	//	strobeOnView.loadUrl("http://farm5.static.flickr.com/4132/5048688508_841e859097_o.jpg");
    		strobeOnView.loadUrl("file:///mnt/sdcard/Application Data/Lucid Dreaming App/html/strobe_bright.html");
    		
    		strobeOffView = (WebView)this.findViewById(R.id.strobeOffView);    		
    		strobeOffView.getSettings().setLoadWithOverviewMode(true);
    		strobeOffView.getSettings().setUseWideViewPort(true);
    		strobeOffView.setVerticalScrollBarEnabled(false);
    		strobeOffView.setHorizontalScrollBarEnabled(false);
    		strobeOffView.loadUrl("file:///mnt/sdcard/Application Data/Lucid Dreaming App/html/strobe_dark.html");
    		
    		
    		//initialize timer and timer tasks
    		timer = new Timer();
    		handler = new Handler();
    		 handler.post(new IncreaseTask());
    		 
    		  long delaySum = 0;
    		  for(int i = 0; i<timing.length;i++){
    			  delaySum +=timing[i];
    			 
    			  if(i%2==1){
    				timer.schedule(new StrobeOff(), delaySum);  
    			  }else{
    				timer.schedule(new StrobeOn(), delaySum);  
    			  }
    		  }
    		  timer.schedule(new FinishTask(), delaySum+500);
    		  
    		
    	}else{
    		finish();
    	}
    	
    	
	}

	@Override
	protected void onDestroy() {
		// TODO Auto-generated method stub
		super.onDestroy();
		setBright(-1.0f);
		
		if(wakeLock!=null && wakeLock.isHeld()){
			wakeLock.release();
		}
		
		try{
			
		Settings.System.putInt(getContentResolver(), Settings.System.SCREEN_BRIGHTNESS_MODE, brightnessMode);
				
		}catch(Exception e){
			
		}
		
		
		try{
			
			
			
			timer.cancel();
			timer.purge();
			timer = null;
		}catch(Exception e){}
	}

	@Override
	protected void onPause() {
		// TODO Auto-generated method stub
		super.onPause();
		
	}

	class StrobeOn extends TimerTask{
		
		
		public void run(){
			//Hide the off view, show the on view
			handler.post(new Runnable(){
				public void run(){
					strobeOffView.setVisibility(View.GONE);
					strobeOnView.setVisibility(View.VISIBLE);
				}
			});
		
		}
	}
		class StrobeOff extends TimerTask{
		
		
		public void run(){
			//hide the on view, show off view
			
			handler.post(new Runnable(){
				public void run(){
					strobeOnView.setVisibility(View.GONE);	
					strobeOffView.setVisibility(View.VISIBLE);
				}
			});
	
		}
	}
		
		class FinishTask extends TimerTask{
			public void run(){
				//call outer class method
				Strobe.this.finish();
			}
		}
		
		   public void setBright(float value) {
//		    	android.provider.Settings.System.putInt(getContentResolver(), 
//		                android.provider.Settings.System.SCREEN_BRIGHTNESS, value);
		    	
		        Window mywindow = getWindow();

		        WindowManager.LayoutParams lp = mywindow.getAttributes();

		                lp.screenBrightness = value;

		                mywindow.setAttributes(lp);
		    }
		
	    class IncreaseTask implements Runnable{
	    	public void run() {                
	            if (brightness <1f) {
	                    setBright(brightness); //start at 10% bright and go to 0 (screen off)

	                    brightness+=0.2;
	                    handler.postDelayed(new IncreaseTask(), 300L);
	            }
	            else {
	                    //setBright((float) 0.0); 

	                   // brightness = 1;//put bright back
	            }
	    }
	    }
	
	
}
