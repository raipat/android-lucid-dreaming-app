package com.luciddreamingapp.beta;

import java.util.Calendar;
import java.util.Date;
import java.util.Random;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.provider.Settings.SettingNotFoundException;
import android.telephony.TelephonyManager;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.WebView;
import android.widget.Chronometer;
import android.widget.DigitalClock;
import android.widget.TextView;
import android.widget.Toast;

import com.luciddreamingapp.beta.util.AutomaticUploaderService;
import com.luciddreamingapp.beta.util.ColorPickerDialog;
import com.luciddreamingapp.beta.util.analysis.AnalysisTester;


public class TestActivity extends Activity  implements ColorPickerDialog.OnColorChangedListener, SensorEventListener{
	
	private static final String TAG = "Test activity";
	private static final boolean D = true;
	
	private static final int COGNITION_ENHANCER = 1;
	private static final int ALERT = 2;
	WebView helpView;
	Typeface lcdFont;
	
private static final int CUSTOM_ALERT = 3;
	private final static int VISUAL = 0;
	private final static int CHALLENGING = 1;
	private final static int REAL_NOT_REAL = 2;
	Chronometer mChronometer;
    Chronometer mChronometer2;
	DigitalClock clock;
	
	private SensorManager mSensorManager;
	 private  Sensor myLightSensor;
	
    
    View layout;
    
    Random r;
    //59 is 17th prime
    int[] primes = {2, 3, 5, 7, 11, 13, 17, 19, 23, 29, 31, 37, 41, 43, 47, 53, 59, 61, 67, 71, 73, 79, 83, 89, 97};

    private static final long challenge = (((60+10+1)*60+20)*1000);//default challenge
    private static final long dayMillis = 24*60*60*1000;
    private static final String challengeString="01:11:20";
    private boolean result = false;
    private boolean challengingPuzzle = false;
    TextView puzzle;
    
    
    private Handler serviceHandler;
	
    
   
    
	 @Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		
		
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		
		setContentView(R.layout.help_layout);
		r=new Random();
		 lcdFont = Typeface.createFromAsset(getBaseContext().getAssets(),
         "fonts/liquidcrystal.ttf");
		 
		 SharedPreferences prefs = PreferenceManager
        .getDefaultSharedPreferences(getBaseContext());
		 challengingPuzzle= prefs.getBoolean("challenging_math_puzzle", false);
		 
		helpView = (WebView)findViewById(R.id.help_view);
		
		
		helpView.getSettings().setJavaScriptEnabled(true);
		helpView.getSettings().setLoadWithOverviewMode(true);
		helpView.getSettings().setUseWideViewPort(true);
		helpView.getSettings().setBuiltInZoomControls(true);		
		

		helpView.loadUrl("file:///android_asset/html/instructions.html");
		
		LayoutInflater inflater = (LayoutInflater) this.getSystemService(LAYOUT_INFLATER_SERVICE);
    	layout = inflater.inflate(R.layout.chronometer,
    	                               (ViewGroup) findViewById(R.id.layout_root));

    	puzzle = (TextView) layout.findViewById(R.id.puzzle);
    	
    	puzzle.setTypeface(lcdFont);
    	puzzle.setTextSize(24);
    	//puzzle.setTextColor(Color.GREEN);
    	
    	mChronometer = (Chronometer) layout.findViewById(R.id.chronometer);
        mChronometer.setTypeface(lcdFont);
        mChronometer.setTextSize(24);
       // mChronometer.setTextColor(Color.GREEN);
        mChronometer2 = (Chronometer) layout.findViewById(R.id.chronometer2);
        mChronometer2.setTypeface(lcdFont);
        mChronometer2.setTextSize(24);
       // mChronometer2.setTextColor(Color.GREEN);
		
        int color = prefs.getInt("clockColor", Color.GREEN);  
        //clock.setTextColor(color);
        mChronometer.setTextColor(color);
        mChronometer2.setTextColor(color);
        
        
        serviceHandler = new Handler();
        
        try {
			int brightnessMode = Settings.System.getInt(getContentResolver(), Settings.System.SCREEN_BRIGHTNESS_MODE);
			if (brightnessMode == Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC) {
			    Settings.System.putInt(getContentResolver(), Settings.System.SCREEN_BRIGHTNESS_MODE, Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL);
			}
		} catch (SettingNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}

	 
	 


	 @Override
	protected void onPause() {
		// TODO Auto-generated method stub
		super.onPause();
		setBright(-1.0f);
		Settings.System.putInt(getContentResolver(), Settings.System.SCREEN_BRIGHTNESS_MODE, Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC);
				
			
		
	}





	public void colorChanged(int color) {
	        clock.setTextColor(color);
	        mChronometer.setTextColor(color);
	        mChronometer2.setTextColor(color);
	    }



	public boolean onOptionsItemSelected(MenuItem item) {
	        switch (item.getItemId()) {

	        
	      
	        	
	        	case R.id.menu_start_app_help:
	        		
//	        	new UploadTask().execute((Void[])null);
//	        	
	        	Intent startUploader = new Intent(this,AutomaticUploaderService.class);	
	        	startService(startUploader);	
	        		
	        	//should stop itself
	        //	stopService(startUploader);

	        	case R.id.menu_load_data_help:
	        	new AnalysisTask().execute();
//	        	try{
//	        		UploadConfig config = UploadConfig.getInstance();
//	        		for(String s:config.getFilenames()){
//	        			Log.w(TAG,s);
//	        		}
//	        		for(int i = 0;i<10;i++){
//	        			config.getFilenames().add("String"+i);
//	        		}
//	        		config.saveConfig();
//	        	}catch(Exception e){
//	        		if(D)e.printStackTrace();
//	        	}
	        		
	        	return true;
        	
	        	case R.id.menu_web_help:
	        		try{
	        			showDialog(CUSTOM_ALERT);
	        		
	        		}catch(Exception e){
	        			e.printStackTrace();
	        			try{mChronometer.stop();
	        			mChronometer2.stop();
	        			
	        			}catch(Exception ex){
	        				ex.printStackTrace();
	        			}
	        		}
	        		
	        		
	        	return true;
        	
	        	case R.id.menu_preferences:
	        		Intent editorIntent = new Intent(this, EventEditorActivity.class);
	        		
	        		startActivityForResult(editorIntent,1);
	        		
	        	return true;
	        	
	        	
	        }
	        return false;
	        
	    }
	
	
	protected Dialog onCreateDialog(int id) {
	    Dialog dialog;
	    switch(id) {
	    case ALERT:
//	    	AlertDialog.Builder builder = new AlertDialog.Builder(this);
//	    	builder.setMessage("Are you sure you want to exit?")
//	    	       .setCancelable(false)
//	    	       .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
//	    	           public void onClick(DialogInterface dialog, int id) {
//	    	                TestActivity.this.finish();
//	    	           }
//	    	       })
//	    	       .setNegativeButton("No", new DialogInterface.OnClickListener() {
//	    	           public void onClick(DialogInterface dialog, int id) {
//	    	                dialog.cancel();
//	    	           }
//	    	       });
//	    	
//	    	
//	    	
//	    	
//	    	AlertDialog alert = builder.create();
//	    	dialog = alert;
//	    	return alert;
	    	return null;
	    	
	    	
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

	    	System.out.println("chronometer 1 base: "+mChronometer.getBase());
	    	System.out.println("alertDialog "+mChronometer2.getBase());
	    	
	    	builder = new AlertDialog.Builder(this);
	    	
	    	builder.setMessage("Do the numbers add up?")
	 	       .setCancelable(true)
	 	       .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
	 	           public void onClick(DialogInterface dialog, int id) {
	 	                TestActivity.this.displayResults(true);
	 	           }
	 	       })	 	       
	 	       .setNegativeButton("No", new DialogInterface.OnClickListener() {
	 	           public void onClick(DialogInterface dialog, int id) {
	 	        	  TestActivity.this.displayResults(false);
	 	           }
	 	       });
	    	
	    	
	    	builder.setView(layout);
	    	alertDialog = builder.create();
	    	System.out.println("alertDialog "+alertDialog);
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
	
	
	protected void displayResults(boolean answer){
		mChronometer.stop();
		mChronometer2.stop();
		if(result && result==answer){
			sendShortToast("You are right! These numbers do add up");
		}else if(!result && result==answer){
			sendShortToast("Numbers do not add up, you are correct!");
		}else if(result &&!(result == answer) ){
			sendShortToast("These number do add up, try again");
		}else if(!result &&!(result == answer) ){
			sendShortToast("Numbers do not add up, try again");
		}
	}
	
	
    protected void sendToast(String message){
    	Context context = getApplicationContext();
		  Toast toast = Toast.makeText(context, message,Toast.LENGTH_LONG);
		  
		  toast.show();
    }
    
    
    //##################################Menu Section
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.help_menu, menu);
        return true;
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
			
			Date d = new Date(temp);
			
			
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
    protected void sendShortToast(String message){
    	Context context = getApplicationContext();
		  Toast toast = Toast.makeText(context, message,Toast.LENGTH_SHORT);
		  
		  toast.show();
    }
	
    public void setBright(float value) {
//    	android.provider.Settings.System.putInt(getContentResolver(), 
//                android.provider.Settings.System.SCREEN_BRIGHTNESS, value);
    	
        Window mywindow = getWindow();

        WindowManager.LayoutParams lp = mywindow.getAttributes();

                lp.screenBrightness = value;

                mywindow.setAttributes(lp);
    }
    private float brightness=0.25f;
    
    class IncreaseTask implements Runnable{
    	public void run() {                
            if (brightness <1f) {
                    setBright(brightness); //start at 10% bright and go to 0 (screen off)

                    brightness+=0.1;
                    serviceHandler.postDelayed(new IncreaseTask(), 500L);
            }
            else {
                    //setBright((float) 0.0); 

                   // brightness = 1;//put bright back
            }
    }
    }
    class Task implements Runnable {
    	
        public void run() {                
                if (brightness > 0) {
                        setBright(brightness); //start at 10% bright and go to 0 (screen off)

                        brightness-=0.01;
                        serviceHandler.postDelayed(new Task(), 300L);
                }
                else {
                        //setBright((float) 0.0); 

                        //brightness = 1;//put bright back
                }
        }
    }
	@Override
	public void onAccuracyChanged(Sensor arg0, int arg1) {
		// TODO Auto-generated method stub
		
	}





	@Override
	public void onSensorChanged(SensorEvent arg0) {
		// TODO Auto-generated method stub
		
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
    


	private class AnalysisTask extends AsyncTask<Void, Void, Void> {
  		 
		 protected Void doInBackground(Void... urls) {
				
     		AnalysisTester tester = new AnalysisTester();
     		tester.analyze();
			 
	        return null;
	     }

		@Override
		protected void onPostExecute(Void result) {
			// TODO Auto-generated method stub
			super.onPostExecute(result);
			
		}
		
	 }
    
}
