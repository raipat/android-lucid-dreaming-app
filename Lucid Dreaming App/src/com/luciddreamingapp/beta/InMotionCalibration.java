package com.luciddreamingapp.beta;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.commons.math.stat.descriptive.DescriptiveStatistics;
import org.apache.commons.math.stat.descriptive.SummaryStatistics;
import org.ejml.data.DenseMatrix64F;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;


import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.webkit.WebView;
import android.widget.Toast;

import com.luciddreamingapp.beta.util.LogManager;

public class InMotionCalibration extends Activity implements  SensorEventListener{

	
	
	double[] xValues = new double[3000];
	double[] yValues = new double[3000];
	double[] zValues = new double[3000];
	
	
	
	public static final boolean D = false;
	public static final boolean DD = false;
	private boolean manualCalibration = false;
	
	public static final boolean YDEBUG = false;
	public static final String TAG = "SensorCalibrationActivity";
	
	private float x,y,z;
	private double xFiltered,yFiltered,zFiltered;
	private int xActivityCount,yActivityCount,zActivityCount,magnitudeActivityCount;
	private double xActivitySum,yActivitySum,zActivitySum, magnitudeActivitySum;
	


	private double epochXYZActivityCount;

	boolean calibrated = false;
	String activityStatistics = "Please wait until the device is calibrated: "+calibrated;
	String prevActivityStatistics = "";

	private boolean hasToCalibrateAccelerometer = true;
	
	
	SimpleFilter xFilter;
	SimpleFilter yFilter;
	SimpleFilter zFilter;

	
	
	private int epochsToCalibrate;
	private int calibratedEpochs = 0;
	
	
	private boolean timerCounted60Seconds= false; //stop and go flag set by the timer task.
	private boolean dataProcessed = false;

	
	int activityCounter = 0;
	float epochActivitySum = 0;
	String epochActivitySumStr = "";
	String prevText = "";
	int eventCounter= 0;
	int prevEventCounter = 1200;
	private int calibrationCounter = 0;
	private int initialCalibrationWindow = 3000;
	
	double coleConstantLowThreshold;
	double coleConstantMediumThreshold;
	double coleConstantHighThreshold;
	double coleConstantVeryHighThreshold;
	
Handler handler;
	
//remember previous value for discrete comparison
private double xPrev, yPrev, zPrev;

	
	//stateless statistics for the XYZ - to determine filter properties
	
	SummaryStatistics statMagnitude = new SummaryStatistics();
	SummaryStatistics statX = new SummaryStatistics();
	SummaryStatistics statY = new SummaryStatistics();
	SummaryStatistics statZ = new SummaryStatistics();
	
	DescriptiveStatistics xTemp = new DescriptiveStatistics(300);
	DescriptiveStatistics yTemp = new DescriptiveStatistics(300);
	DescriptiveStatistics zTemp = new DescriptiveStatistics(300);
	
    private PowerManager.WakeLock wakeLock;
	
	
	private double xStep,yStep,zStep;
	
	//Reconsider using infinite window
	DescriptiveStatistics statEpochXYZSum = new DescriptiveStatistics(600);
	DescriptiveStatistics statEpochXYZCount = new DescriptiveStatistics(600);
	
	private int thresholdCounter = 0;
	StringBuilder debugBuffer;
	
	// LogManager FSTLog;
	 LogManager calibrationLog,debugLog;
	 WebView calibrationView;
	Timer timer;
	TimerTask countDownTask;
	
	private SensorManager mSensorManager;
    private Sensor mAccelerometer;
    private float accelerometerResolution;
	
//    private static final String HEADER = "Timestamp, Local Time," +
//	"magnitudeActivityCount,magnitudeActivitySum,xActivityCount,xActivitySum,yActivityCount," +
//	"yActivitySum,zActivityCount,zActivitySum,xCalibratedVariance,xCalibratedStDev,xCalibratedMean,yCalibratedVariance," +
//	"yCalibratedStDev,xCalibratedMean,zCalibratedVariance,zCalibratedStDev,xCalibratedMean,calibratedEpochs";
//	
    private static final String FILE_INFO ="Lucid Dreaming App calibration log. Contains accelerometer information and accelerometer readings for each calibration minute.";
    private static final String HEADER = "Timestamp, Date,xActivityCount,yActivityCount,zActivityCount";

	private String debugHeaders = "timestamp,x,xfiltered,xcount,xsum, y,yfiltered,ycount,ysum,z,zfiltered,zcount,zsum";

	private JSONArray xJSON,yJSON,zJSON;
	private JSONArray xFilteredJSON,yFilteredJSON,zFilteredJSON;
	private JSONObject graphData = new JSONObject();
	private JSONObject prevGraphData = new JSONObject();
	
	private JSONObject pageData = new JSONObject();

	double xNoiseVarianceR,yNoiseVarianceR,zNoiseVarianceR;
    
	/* (non-Javadoc)
	 * @see android.app.Activity#onCreate(android.os.Bundle)
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_PROGRESS);
		setContentView(R.layout.calibration_activity);
		setProgressBarVisibility(true);
		handler = new Handler();

		

	
	
	
		if(D) Log.e(TAG, "++ ON CREATE ++");
		
	
	
 // FSTLog =new LogManager(LucidDreamingApp.LOG_LOCATION,"FSTLog.txt","FastSine Transform");
	
	
	
	
		Bundle extras = getIntent().getExtras(); 
		if(extras !=null)
		{
		epochsToCalibrate = extras.getInt("accelerometer_calibration_duration_min")-2;
		if(epochsToCalibrate<5){
			epochsToCalibrate=5;
			sendToast("calibrating for 5 minutes minimum");}
		}else{epochsToCalibrate = 15;}
		
		 setProgress(0);
	     setSecondaryProgress(0);
		 
	   //TODO inject preferences for filter setup
			SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
				//setupFilter(magnitudeFilter);
//			try{
//			setupFilter(xFilter, Float.parseFloat(prefs.getString("x_sensitivity_pref", "0.00005")));}
//			catch(Exception e){}
//			try{
//				setupFilter(yFilter, Float.parseFloat(prefs.getString("y_sensitivity_pref", "0.00001")));}
//				catch(Exception e){}
//				try{
//					setupFilter(zFilter, Float.parseFloat(prefs.getString("z_sensitivity_pref", "0.0001")));}
//					catch(Exception e){}
			
					try{
						setupFilter(xFilter, 0.00004);}//0.00002
						catch(Exception e){}
						try{
							setupFilter(yFilter, 0.000006);}//0.000005
							catch(Exception e){}
							try{
								setupFilter(zFilter, 0.00001);}//0.00001
								catch(Exception e){}
						
					
		
	     	xValues[0]=0;
			yValues[0]=0;
			zValues[0]=0;
	         
	         calibrationLog= new LogManager(LucidDreamingApp.LOG_LOCATION,"calibrationLog.txt",FILE_INFO);
	         debugLog= new LogManager(LucidDreamingApp.LOG_LOCATION,"debug.txt","x,y,z");
	         debugBuffer = new StringBuilder(32);
	        
	        //calibrationLog.appendEntry(HEADER);
	         
	         
	         calibrationView = (WebView) findViewById(R.id.calibration_view);
	         calibrationView.getSettings().setJavaScriptEnabled(true);
	         calibrationView.getSettings().setLoadWithOverviewMode(true);
	         calibrationView.getSettings().setUseWideViewPort(true);
	         calibrationView.getSettings().setBuiltInZoomControls(true);
	         calibrationView.addJavascriptInterface(this, "javahandler");
	        
	        // calibrationView.loadUrl("file:///android_asset/html/running_calibration.html");
	     
	         
	       //  calibrationView.setWebChromeClient(new WebChromeClient());
	     // setWebViewText("<h1>Sensor Calibration In Progress</h1>");
	        try{
	       pageData.put("title", "Ready");
	        pageData.put("message", "Select (Calibrate) from the menu");
	       pageData.put("message2", "This phase will take up to 3 minutes based on the sampling rate you set in preferences");
	      // generateTestData();
	      // calibrationView.loadUrl("file:///android_asset/html/sensor_calibration_results_page.html");
	       
	       calibrationView.loadUrl("file:///android_asset/html/sensor_manual_calibration_page.html");
	       updatePage();
	         }catch(JSONException e){}
	         xJSON = new JSONArray();
	         yJSON = new JSONArray();
	         zJSON = new JSONArray();
	         
	         xFilteredJSON = new JSONArray();
	         yFilteredJSON = new JSONArray();
	         zFilteredJSON = new JSONArray();
	         
	         
	         //TODO Make the button start calibration, then disable it
	        // startCalibration();
			 
	}



	@Override
	protected void onStart() {
		// TODO Auto-generated method stub
		super.onStart();
		
		
	}


	
	@Override
	protected void onPostResume() {
		// TODO Auto-generated method stub
		super.onPostResume();
		updatePage();
	}



	/* (non-Javadoc)
	 * @see android.app.Activity#onCreateOptionsMenu(android.view.Menu)
	 */
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// TODO Auto-generated method stub
		 super.onCreateOptionsMenu(menu);
		
		        MenuInflater inflater = getMenuInflater();
		        inflater.inflate(R.menu.calibration_menu, menu);
		        return true;
		
	}
	
	public void startCalibration(View view){
	startCalibration();
	}
	public void cancelCalibration(View view){
		cancelCalibration();
		}
	
	
	  @Override
	    public boolean onOptionsItemSelected(MenuItem item) {
	        switch (item.getItemId()) {
	        case R.id.menu_start_quick_calibration:
	        	
	        	quickCalibration(this.getCurrentFocus());
	        	
	        	return true;
	        	
	        	
	        case R.id.menu_start_extended_calibration:
	        	
	        	startCalibration();
	        	
	        	return true;
	        	
	        case R.id.menu_cancel_calibration:
	        	
	        	cancelCalibration();
	        	
	        	return true;
	        
	        }
	        return false;
	  }

String accuracyMessage = "";
	@Override
	public void onAccuracyChanged(Sensor arg0, int accuracy) {
		String message = "";
		switch (accuracy){
			case SensorManager.SENSOR_STATUS_ACCURACY_HIGH:
				//if(D) Log.w(TAG,sensor.getName()+ message);
				message = "High accuracy";
				break;
			case SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM:
				//if(D) Log.w(TAG,sensor.getName()+ message);
				message = "Medium accuracy";
				break;
			case SensorManager.SENSOR_STATUS_ACCURACY_LOW:
				//if(D) Log.w(TAG,sensor.getName()+ message);
				message = "Low accuracy";
				break;
			case SensorManager.SENSOR_STATUS_UNRELIABLE:
				//if(D) Log.w(TAG,sensor.getName()+ message);
				message = "Unreliable accuracy";
				
		}
		if(message.equals(accuracyMessage)){
			
		}else{
			accuracyMessage = message;
			if(D)Log.w(TAG,"New Sensor Accuracy: "+accuracyMessage );
		}
		
	}

	
	public InMotionCalibration(){
	//get data source for calibrated values
		//dataSource = PhoneAccelerometerHandler.getInstance();
		
		//initialize kalman filters
		xFilter = new SimpleFilter();
		yFilter = new SimpleFilter();
		zFilter = new SimpleFilter();
	
		
			
	}
	
	
	
	
	static final String comma = ",";
	@Override
	public void onSensorChanged(SensorEvent arg0) {
		
		if(calibratedEpochs<epochsToCalibrate){
		eventCounter++;	
		
		float[] temp =arg0.values;
		x=temp[0];
		y=temp[1];
		z=temp[2];
		
//		if(DD){Log.e(TAG, " X:"+x +" Y:"+y+" Z:"+z);
//		debugLog.appendEntry(System.currentTimeMillis()+","+x +","+y+","+z);
//		}
		//compensate for the user pressing the menu button
		try{
			if(eventCounter >50){
			xValues[eventCounter]=x;
			yValues[eventCounter]=y;
			zValues[eventCounter]=z;
			}
		}catch (Exception e){
			//if(D)Log.e(TAG, e.getMessage());
		}
		
		
		//calibrate initial readings
		if(hasToCalibrateAccelerometer){
		//intended in case the app has to restart //TODO fix timers on restart
		//remember a certain number of values to calculate variance and standard deviation
			
			if(YDEBUG){
				debugLog.appendEntry(x+comma+y+comma+z+comma);
			}
				
		//	setWebViewText(""+calibrationCounter);

		xTemp.addValue(x);
		yTemp.addValue(y);
		zTemp.addValue(z);
		
		
		calibrationCounter++;
			
			setProgress(calibrationCounter*9900/initialCalibrationWindow);
		
		//if enough events were processed.
			if(calibrationCounter>initialCalibrationWindow){
				
				if(YDEBUG){
					debugLog.appendEntry("calibrationCounter>initialCalibrationWindow");
				}
				
				xPrev = x;
				yPrev = y;
				zPrev = z;
				
				xStep = step(xValues);
				yStep = step(yValues);
				zStep = step(zValues);
				
				double maxStep = Math.max(zStep, Math.max(xStep, yStep));
				xStep = maxStep;
				yStep = maxStep;
				zStep = maxStep;
				
				
				if(D)Log.w(TAG, "Setting xStep : "+xStep);
				if(D)Log.w(TAG, "Setting yStep : "+yStep);
				if(D)Log.w(TAG, "Setting zStep : "+zStep);
				
			
				
				
				
				
				
				accelerometerResolution=mAccelerometer.getResolution();

				//Pick the maximum value for the noise variance			
				if((3*xStep)<accelerometerResolution){
					xNoiseVarianceR=accelerometerResolution*accelerometerResolution;
				}else{	xNoiseVarianceR=(3*xStep)*(3*xStep);}
				
				if((3*yStep)<accelerometerResolution){
					yNoiseVarianceR=accelerometerResolution*accelerometerResolution;
				}else{yNoiseVarianceR=(3*yStep)*(3*yStep);}
				
				if((3*zStep)<accelerometerResolution){
					zNoiseVarianceR=accelerometerResolution*accelerometerResolution;
				}else{zNoiseVarianceR=(3*zStep)*(3*zStep);}
				
				if(D)Log.w(TAG, "Detected xNoiseVarianceR: "+xNoiseVarianceR);
				if(D)Log.w(TAG, "Detected yNoiseVarianceR: "+yNoiseVarianceR);
				if(D)Log.w(TAG, "Detected zNoiseVarianceR: "+zNoiseVarianceR);
				
				SharedPreferences customSharedPreference = getSharedPreferences(
	                     "LucidDreamingAppPreferences", Activity.MODE_PRIVATE);
		     SharedPreferences.Editor editor = customSharedPreference
		                     .edit();
		     
			 editor.putFloat("xStep", (float)xStep);             
			 editor.putFloat("yStep", (float)yStep);
			 editor.putFloat("zStep", (float)zStep);
		     editor.putFloat("xNoiseVarianceR", (float)xNoiseVarianceR);             
			 editor.putFloat("yNoiseVarianceR", (float)yNoiseVarianceR);
			 editor.putFloat("zNoiseVarianceR", (float)zNoiseVarianceR);
		     
		     
		     editor.commit();
				
				
				
				xPrev = mode(xValues);
				yPrev = mode(yValues);
				zPrev = mode(zValues);
				
				
				logStepNoiseVariance();
				
				xValues = null;
				yValues = null;
				zValues = null;				
				
				hasToCalibrateAccelerometer = false;
				//TODO include a notification of phase 1 sensor calibration complete
				
				if(manualCalibration){
					
					
					SharedPreferences prefs = PreferenceManager
			         .getDefaultSharedPreferences(getBaseContext());
					editor = prefs.edit();
					 
				//	editor.putFloat("cole_constant_pref", (float)coleConstantVeryHighThreshold);         
					editor.putBoolean("calibration_completed", true);
					
					
					editor.commit();
					

					  customSharedPreference = getSharedPreferences(
		                     "LucidDreamingAppPreferences", Activity.MODE_PRIVATE);
			      editor = customSharedPreference
			                     .edit();
			    //Calculate upper bound of what will be considered asleep
			      int countMean=0;
			      int countStandardDeviation=0;
					double thresholdLow = Math.max(39,countMean+3*countStandardDeviation);
					double thresholdMedium = Math.max(41,countMean+4*countStandardDeviation);
					double thresholdHigh = Math.max(43,countMean+5*countStandardDeviation);
					double thresholdVeryHigh = Math.max(45, countMean+6*countStandardDeviation);
					
					coleConstantLowThreshold = calculateColeConstant(thresholdLow);
					coleConstantMediumThreshold = calculateColeConstant(thresholdMedium);
					coleConstantHighThreshold = calculateColeConstant(thresholdHigh);
					coleConstantVeryHighThreshold = calculateColeConstant(thresholdVeryHigh);
					
					
					 editor.putFloat("coleConstantLowThreshold", (float)coleConstantLowThreshold);             
					 editor.putFloat("coleConstantMediumThreshold", (float)coleConstantMediumThreshold);
					 editor.putFloat("coleConstantHighThreshold", (float)coleConstantHighThreshold);
					 editor.putFloat("coleConstantVeryHighThreshold", (float)coleConstantVeryHighThreshold);
					 
					 editor.putFloat("thresholdLowSensitivity", (float)thresholdLow);             
					 editor.putFloat("thresholdMediumSensitivity", (float)thresholdMedium);
					 editor.putFloat("thresholdHighSensitivity", (float)thresholdHigh);
					 editor.putFloat("thresholdVeryHighSensitivity", (float)thresholdVeryHigh);
					if(D)Log.e(TAG,"cole constant"+coleConstantVeryHighThreshold);
			      editor.commit();
					//sendToast("Completed");
					sendToast("Calibration Completed");
					
					finish();
					return;
				}
				
				timer = new Timer();
				countDownTask = new CountDownTask();
				//setSecondaryProgress(10000);
				if(D)Log.w(TAG,"60 second Timer started");
				timer.scheduleAtFixedRate(countDownTask,0, 60000L);
			}
		
		}else{
			
			
			//Get the guess of the true position of the system
			xFiltered = KalmanFilter(xFilter, x, xNoiseVarianceR);
			yFiltered = KalmanFilter(yFilter, y, yNoiseVarianceR);
			zFiltered = KalmanFilter(zFilter, z, zNoiseVarianceR);
			
			//create data to show user 
			//createGraphDataJSON();
			
		//	debugLog.appendEntry(getDebugLogEntry());
			if(YDEBUG)debugBuffer.append(x+",");
			if(YDEBUG)	debugBuffer.append(y+",");
			if(YDEBUG)	debugBuffer.append(z+",");
			//attempts to compute how far x lies from the filtered value and uses
			//the rate of change of the difference to estimate the intensity of activity
			double diff = Math.abs((x-xFiltered)-xPrev)/xStep;
			if(diff>2){
				
				diff-=2;
				//diff = diff/accelerometerResolution;
			xActivityCount +=Math.min(Math.floor(diff),20);
			thresholdCounter++;
			//xActivityCount +=Math.min(Math.floor(diff*diff),25);
				if(D && eventCounter %250==0){
				if(D)	Log.w(TAG,"xDiff: "+diff);
				}
				}
			xPrev = x-xFiltered;
			if(YDEBUG)	debugBuffer.append(diff+",");
			
			diff = Math.abs((y-yFiltered)-yPrev)/yStep;
			if(diff>2){
				diff-=2;
				yActivityCount +=Math.min(Math.floor(diff),20);
				thresholdCounter++;
				//yActivityCount +=Math.min(Math.floor(diff*diff),25);
				if(D && eventCounter %250==0){
				if(D)	Log.w(TAG,"yDiff: "+diff);
				}
			}
			yPrev = y-yFiltered;
			if(YDEBUG)debugBuffer.append(diff+",");
			
			 diff = Math.abs((z-zFiltered)-zPrev)/zStep;
			 if(diff>3){
					diff-=3;
					zActivityCount +=Math.min(Math.floor(diff),20);
					thresholdCounter++;
					//zActivityCount +=Math.min(Math.floor(diff*diff),25);
					if(D && eventCounter %250==0){
				if(D)	Log.w(TAG,"zDiff: "+diff);
				}
				}
			zPrev = z-zFiltered;
			if(YDEBUG)	debugBuffer.append(diff+",");
			
			//remember activity count for statistical processing
			epochXYZActivityCount= xActivityCount+yActivityCount+zActivityCount;
	
		
			if(YDEBUG){
					debugBuffer.append(epochXYZActivityCount+",");
					debugBuffer.append(thresholdCounter+",");
				debugLog.appendEntry(debugBuffer.toString());
					
			}debugBuffer.delete(0, debugBuffer.length());
		
		//	setWebViewText(epochXYZActivityCount +" activity counts this minute ");
		
		
		try{
		setProgress((eventCounter*10000)/1400);
		}catch(Exception e){if(D)e.printStackTrace();}


		if(timerCounted60Seconds){
			
			if(YDEBUG){
				debugLog.appendEntry("60 seconds passed");
			}
			
			if(D){
			Log.w(TAG,"Processing epoch");
			Log.w(TAG,"xActivityCount"+xActivityCount);
			Log.w(TAG,"yActivityCount"+yActivityCount);
			Log.w(TAG,"zActivityCount"+zActivityCount);
			Log.e(TAG,"thresholdCounter"+thresholdCounter);
			Log.w(TAG,"eventCount"+eventCounter);		
			}
			
			timerCounted60Seconds= false;
			
			//reject first few data points as the filter settles down
			if(calibratedEpochs>3){
			
	//		reject values if they introduce high kurtosis
			try{
				statEpochXYZCount.addValue(epochXYZActivityCount);
				if(epochXYZActivityCount !=0 && statEpochXYZCount.getKurtosis()>10){
					statEpochXYZCount.removeMostRecentValue();
					calibrationLog.appendEntry("rejecting last minute's values due to high kurtosis");
				if(D)Log.e(TAG, "Rejecting calibration epoch due to noise");
				}
			}catch(Exception e){}
			
				}
				calibrationLog.appendEntry(getEpochCalibrationData());
//	7	graphData.put("zFilteredLabel", "Z Filtered");
//			    xJSON = new JSONArray();
//			    yJSON = new JSONArray();
//			    zJSON = new JSONArray();
//			    
//			    xFilteredJSON = new JSONArray();
//			    yFilteredJSON = new JSONArray();
//			    zFilteredJSON = new JSONArray();
//	}catch(Exception e){}	
//				prevGraphData = graphData;
//	
//				calibrationView.loadUrl("javascript:graphData("+  graphData +")");
//	Log.w(TAG,"JSON Data"+graphData);
	//			graphData = new JSONObject();

				
				
			//reset x,y,z for the next epoch
			xActivityCount=0;
			xActivitySum= 0;
			yActivityCount=0;
			yActivitySum= 0;
			zActivityCount=0;
			zActivitySum= 0;
			eventCounter=0;
			thresholdCounter = 0;
			setProgress(0);
				calibratedEpochs++;	
				setSecondaryProgress(calibratedEpochs*10000/epochsToCalibrate);
		}
		
		}
		}else{
			processCalibrationData();
			
		}
		

	}//end on sensor changed
	
	
	private void createGraphDataJSON(){
		
		JSONArray tempJSON = new JSONArray();
		try{
			tempJSON.put(eventCounter);
			tempJSON.put(x);
		xJSON.put(tempJSON);
		}catch(Exception e){}
		
		tempJSON = new JSONArray();
		try{tempJSON.put(eventCounter);
		tempJSON.put(xFiltered);
		xFilteredJSON.put(tempJSON);
		}catch(Exception e){}
		
		 tempJSON = new JSONArray();
		try{
			tempJSON.put(eventCounter);
			tempJSON.put(y);
		yJSON.put(tempJSON);
		}catch(Exception e){}
		
		tempJSON = new JSONArray();
		try{
			tempJSON.put(eventCounter);
			tempJSON.put(yFiltered);
		yFilteredJSON.put(tempJSON);
		}catch(Exception e){}
		
		 tempJSON = new JSONArray();
		try{tempJSON.put(eventCounter);
			tempJSON.put(z);
		zJSON.put(tempJSON);
		}catch(Exception e){}
		
		tempJSON = new JSONArray();
		try{tempJSON.put(eventCounter);
			tempJSON.put(zFiltered);
		zFilteredJSON.put(tempJSON);
		}catch(Exception e){
			if(D)e.printStackTrace();
		}
		tempJSON =null;
		
	}
	
//	private void updateSensorCalibrationData(){
//		
//		
//		xCalibratedVariance = statX.getVariance();
//		xCalibratedStDev=statX.getStandardDeviation();
//			
//		
//		yCalibratedVariance = statY.getVariance();
//		yCalibratedStDev = statY.getStandardDeviation();
//		
//	
//		zCalibratedVariance= statZ.getVariance();
//		zCalibratedStDev= statZ.getStandardDeviation();
//	}
	
	private void logStepNoiseVariance(){
		
		if(D)Log.w(TAG,"xStep:"+xStep);
		if(D)Log.w(TAG,"yStep:"+yStep);
		if(D)Log.w(TAG,"zStep:"+zStep);
		try{calibrationLog.appendEntry("Calibration of "+mAccelerometer.getName()+" accelerometer. Processed events: "+calibrationCounter+" Advertised resolution: "+mAccelerometer.getResolution());}
		catch(Exception e){};
		calibrationLog.appendEntry("Detected digital signal parameters:");
		calibrationLog.appendEntry("xStep,yStep,zStep,xMode,yMode,zMode");
		StringBuilder sb = new StringBuilder();
	
		sb.append(xStep);
		sb.append(",");
		sb.append(yStep);
		sb.append(",");
		sb.append(zStep);
		sb.append(",");
		
		sb.append(xPrev);
		sb.append(",");
		sb.append(yPrev);
		sb.append(",");
		sb.append(zPrev);
		sb.append(",");
		calibrationLog.appendEntry(sb.toString());
		calibrationLog.appendEntry("Timestamp, Date, X Activity Count, Y Activity Count, Z Activity Count, Accelerometer Events Count");
		try{
		pageData.put("title", "Calibration of"+mAccelerometer.getName()+" in progress");
		pageData.put("message", "xStep,yStep,zStep,xMode,yMode,zMode");
        pageData.put("message2", sb.toString());
		pageData.put("accelerometerStep", xStep);
		pageData.put("accelerometerResolution", mAccelerometer.getResolution());
		 calibrationView.loadUrl("file:///android_asset/html/sensor_calibration_page.html");
	       updatePage();
		
		}catch(JSONException e){}
		this.updatePage();
	
		
		
		
	}
	
	private void processCalibrationData(){
		if(!dataProcessed){
			double countStandardDeviation = 0;
			double countMean = 0;
			//get the standard deviation for the total counts
			if(statEpochXYZCount!=null&& statEpochXYZCount.getN()>0){
			 countStandardDeviation = statEpochXYZCount.getStandardDeviation();
			 countMean = statEpochXYZCount.getMean();
			}
			//Calculate upper bound of what will be considered asleep
			double thresholdLow = Math.max(39,countMean+3*countStandardDeviation);
			double thresholdMedium = Math.max(41,countMean+4*countStandardDeviation);
			double thresholdHigh = Math.max(43,countMean+5*countStandardDeviation);
			double thresholdVeryHigh = Math.max(45, countMean+6*countStandardDeviation);
			
			coleConstantLowThreshold = calculateColeConstant(thresholdLow);
			coleConstantMediumThreshold = calculateColeConstant(thresholdMedium);
			coleConstantHighThreshold = calculateColeConstant(thresholdHigh);
			coleConstantVeryHighThreshold = calculateColeConstant(thresholdVeryHigh);
			
			try{
			pageData.put("countMean", countMean);
			
			pageData.put("thresholdLowSensitivity", thresholdLow);
			pageData.put("thresholdMediumSensitivity", thresholdMedium);
			pageData.put("thresholdHighSensitivity", thresholdHigh);
			pageData.put("thresholdVeryHighSensitivity", thresholdVeryHigh);
			
			pageData.put("coleConstantLowThreshold", coleConstantLowThreshold);
			pageData.put("coleConstantMediumThreshold", coleConstantMediumThreshold);
			pageData.put("coleConstantHighThreshold", coleConstantHighThreshold);
			pageData.put("coleConstantVeryHighThreshold", coleConstantVeryHighThreshold);
			
			
			}catch(JSONException e){if(D)e.printStackTrace();}
			
		StringBuilder sb = new StringBuilder();
		
		String resultsHeader = "xyzCountStandardDeviation,xyzCountMean,coleConstantLowThreshold,coleConstantMediumThreshold," +
		"coleConstantHighThreshold,coleConstantVeryHighThreshold," +
		"thresholdLow," +
		"thresholdMedium,thresholdHigh," +
		"thresholdVeryHigh";
		//TODO include if logging is enabled
		calibrationLog.appendEntry(resultsHeader);
		if(D)Log.w(TAG, resultsHeader);
		sb.append(countStandardDeviation);
		sb.append(",");
		sb.append(countMean);
		sb.append(",");		
		sb.append(coleConstantLowThreshold);
		sb.append(",");
		sb.append(coleConstantMediumThreshold);
		sb.append(",");
		sb.append(coleConstantHighThreshold);
		sb.append(",");
		sb.append(coleConstantVeryHighThreshold);
		sb.append(",");
		sb.append(thresholdLow);
		sb.append(",");
		sb.append(thresholdMedium);
		sb.append(",");
		sb.append(thresholdHigh);
		sb.append(",");
		sb.append(thresholdVeryHigh);
		sb.append(",");
		
		calibrationLog.appendEntry(sb.toString());
		if(D)Log.w(TAG, sb.toString());
		
		updatePage();
		
		sb = null;
//TODO Write directly to preferences that can be user-visible
			

			 SharedPreferences customSharedPreference = getSharedPreferences(
                     "LucidDreamingAppPreferences", Activity.MODE_PRIVATE);
	     SharedPreferences.Editor editor = customSharedPreference
	                     .edit();
	     
//         editor.putFloat("xCalibratedVariance", (float)xCalibratedVariance);
//		 editor.putFloat("xCalibratedStDev", (float)xCalibratedStDev);
//		 
//		 
//		 editor.putFloat("yCalibratedVariance", (float)yCalibratedVariance);
//		 editor.putFloat("yCalibratedStDev", (float)yCalibratedStDev);
//		 
//		 editor.putFloat("zCalibratedVariance", (float)zCalibratedVariance);
//		 editor.putFloat("zCalibratedStDev", (float)zCalibratedStDev);
//		 
//		 editor.putFloat("magnitudeCalibratedVariance", (float)magnitudeCalibratedVariance);             
//		 editor.putFloat("magnitudeCalibratedStDev", (float)magnitudeCalibratedStDev);
		 
		 editor.putFloat("epochXYZCountStDev", (float)countStandardDeviation);             
		 editor.putFloat("epochXYZCountMean", (float)countMean);
		 
//		 editor.putFloat("epochXYZSumStDev", (float)statEpochXYZSum.getStandardDeviation());             
//		 editor.putFloat("epochXYZSumMean", (float)statEpochXYZSum.getMean());
		 
		 editor.putFloat("xStep", (float)xStep);             
		 editor.putFloat("yStep", (float)yStep);
		 editor.putFloat("zStep", (float)zStep);
		 
		 editor.putFloat("xNoiseVarianceR", (float)xNoiseVarianceR);             
		 editor.putFloat("yNoiseVarianceR", (float)yNoiseVarianceR);
		 editor.putFloat("zNoiseVarianceR", (float)zNoiseVarianceR);
		 
		// editor.putBoolean("calibrationCompleted", true);
		 editor.putString("calibrationDate", Calendar.getInstance().getTime().toLocaleString());
		 
		 editor.putFloat("coleConstantLowThreshold", (float)coleConstantLowThreshold);             
		 editor.putFloat("coleConstantMediumThreshold", (float)coleConstantMediumThreshold);
		 editor.putFloat("coleConstantHighThreshold", (float)coleConstantHighThreshold);
		 editor.putFloat("coleConstantVeryHighThreshold", (float)coleConstantVeryHighThreshold);
		 
		 editor.putFloat("thresholdLowSensitivity", (float)thresholdLow);             
		 editor.putFloat("thresholdMediumSensitivity", (float)thresholdMedium);
		 editor.putFloat("thresholdHighSensitivity", (float)thresholdHigh);
		 editor.putFloat("thresholdVeryHighSensitivity", (float)thresholdVeryHigh);
		 
		 editor.commit();
		 editor = null;
		 
	SharedPreferences prefs = PreferenceManager
         .getDefaultSharedPreferences(getBaseContext());
		editor = prefs.edit();
		 
	//	editor.putFloat("cole_constant_pref", (float)coleConstantVeryHighThreshold);         
		editor.putBoolean("calibration_completed", true);
		
		
		editor.commit();
		editor = null;
		 
		 
		 //TODO add preferences directly to the main preference screen
//		 SharedPreferences prefs = PreferenceManager
//	        .getDefaultSharedPreferences(getBaseContext());
//		 
//		 editor = prefs.edit();
//		 
//		 
		 
		 
		 
		 showCalibrationResults();
		 
		 
		 
//			
//			 Intent activityResult = new Intent();
//	         activityResult.putExtra("xCalibratedVariance", (float)xCalibratedVariance);
//	         activityResult.putExtra("xCalibratedStDev", (float)xCalibratedStDev);
//	         activityResult.putExtra("yCalibratedVariance", (float)yCalibratedVariance);
//	         activityResult.putExtra("yCalibratedStDev", (float)yCalibratedStDev);
//	         activityResult.putExtra("zCalibratedVariance", (float)zCalibratedVariance);
//	         activityResult.putExtra("zCalibratedStDev", (float)zCalibratedStDev);
//	         activityResult.putExtra("magnitudeCalibratedVariance", (float)magnitudeCalibratedVariance);
//	         activityResult.putExtra("magnitudeCalibratedStDev", (float)magnitudeCalibratedStDev);
//	         
//	         activityResult.putExtra("coleConstantLowThreshold", (float)coleConstantLowThreshold);
//	         activityResult.putExtra("coleConstantMediumThreshold", (float)coleConstantMediumThreshold);
//	         activityResult.putExtra("coleConstantHighThreshold", (float)coleConstantHighThreshold);
//	         activityResult.putExtra("coleConstantVeryHighThreshold", (float)coleConstantVeryHighThreshold);
//	         
//	         activityResult.putExtra("thresholdLowSensitivity", (float)thresholdLowSensitivity);
//	         activityResult.putExtra("thresholdMediumSensitivity", (float)thresholdMediumSensitivity);
//	         activityResult.putExtra("thresholdHighSensitivity", (float)thresholdHighSensitivity);
//	         activityResult.putExtra("thresholdVeryHighSensitivity", (float)thresholdVeryHighSensitivity);
//	         
//	         
//	         activityResult.putExtra("epochXYZCountStDev", (float)statEpochXYZCount.getStandardDeviation());
//	         activityResult.putExtra("epochXYZCountMean", (float)statEpochXYZCount.getMean());
//	         
//	         
//	         activityResult.putExtra("epochXYZSumStDev", (float)statEpochXYZSum.getStandardDeviation());
//	         activityResult.putExtra("epochXYZSumMean", (float)statEpochXYZSum.getMean());
//	  
	         
	         
	        // System.out.println("got rowID: "+rowID);
	          // Set result and finish this Activity
	         try{
	        	 mSensorManager.unregisterListener(this);
	         }catch(Exception e){
	        	 e.printStackTrace();
	         }
	         // setResult(Activity.RESULT_OK, activityResult);
	     //     finish();
						
			dataProcessed = true;
		}
			
	}
	
		
	
	
	/**
	 * @return the prevGraphData
	 */
	public JSONObject getPrevGraphData() {
		return prevGraphData;
	}



	public static double calculateColeConstant(double t){
		//Calculate cole constant low enough to consider each of these readings as asleep
		//=0.9999/(2.3*Y$3+(Y$3*1.06+Y$3*0.54+Y$3*0.58+Y$3*0.76)+(Y$3*0.74+Y$3*0.67))
		//where Y$3 is the threshold value(formula copied from excel)
		double coleConstant = 0.9999/(2.3*t+(t*1.06+t*0.54+t*0.58+t*0.76)+(t*0.74+t*0.67));
		return coleConstant;
	}
	
	public static int calculateActivityPeakCurrent(double coleConstant){
		
		//a single peak of this magnitude will be considered an awakening if it happens in the current minute
		return (int)(1.01/(0.67*coleConstant));
	}
	
	public static int calculateActivityPeakDelayed(double coleConstant){
		if(coleConstant>0){
		//a single peak of this magnitude will be considered an awakening if it happens in the current minute
		return (int)(1.01/(2.3*coleConstant));
		}else{
			return(int)(1.01/2.3*0.0033);
		}
	}
	
	
	
	public void setWebViewText(String text){
		final String s = text;
        handler.post(new Runnable() {
        	
            public void run() {
            	calibrationView.loadUrl("javascript:setText("+  s +")");
            }
        });
		
	
	}
	//Ask the page to update itself. Must set the pageData object first
	public void updatePage(){
//		
//        handler.post(new Runnable() {
//        	
//            public void run() {
//            	calibrationView.loadUrl("javascript:updatePage()");
//            }
//        });
		
	
	}
	
	public JSONObject getPageData(){
		return pageData;
	}
	

	/**Calculates the most frequent number on the list. 
	 * 	 * @param list of values to analyze
	 * @return Most frequently observed value on the list. 
	 */
	public static double mode(ArrayList<Double> list) {
		double maxValue = 0;
		int maxCount = 0;

		
		for (int i = 0; i < list.size(); ++i) {
			int count = 0;
			for (int j = 0; j < list.size(); ++j) {
				if (list.get(j).doubleValue() == list.get(i).doubleValue())
					++count;
			}
			if (count > maxCount) {
				maxCount = count;
				maxValue = list.get(i).doubleValue();
			}
		}

		return maxValue;
	}

	/** Calculates the resolution of a digital accelerometer from the passed in array of 
	 * accelerometer readings. Accelerometer randomly flips from one value to another one within
	 * this range, so readings within 1 step of the mode should not be trusted.
	 * @param readingsArray 
	 * @return
	 */
	public static double step(double[] readingsArray) {

		if (readingsArray.length > 1) {
			ArrayList<Double> frequencyList = new ArrayList<Double>();

			for (int i = 0; i < readingsArray.length; i++) {
				if (i != 0) {
					//calculate the difference and add to the array list
					double diff = Math.abs((readingsArray[i] - readingsArray[i - 1]));
					if (diff > 0) {
						frequencyList.add(diff);
						
					}

				}

			}
			return mode(frequencyList);

		}
		return 0;

	}
	public static double mode(double a[]) {
		double maxValue=0;
		int maxCount=0;

		for (int i = 0; i < a.length; ++i) {
		int count = 0;
		for (int j = 0; j < a.length; ++j) {
		if (a[j] == a[i]) ++count;
		}
		if (count > maxCount) {
		maxCount = count;
		maxValue = a[i];
		}
		}

		return maxValue;
		}
	

	private static HashMap<Double,Integer> countValues(double[] array){
		HashMap<Double,Integer> map = new HashMap<Double,Integer>();
 		for(int i = 0;i< array.length;i++){
			
 			
 			if (map.get(array[i])==null){
 				map.put(array[i],1);
 			}else{
 				map.put(array[i], map.get(array[i]).intValue()+1);
 			}
 			
			
		}
		return map;
	}
	
//	
//	private static double getDiscreteVariance(HashMap<Double,Integer> map){
//		
//		
//		double m = 0;
//		//value is count, key is the number that is counted
//		for(Entry<Double,Integer> e: map.entrySet() ){
//		//count/total*value	
//			m+= (e.getValue().intValue())/map.size()*e.getKey();
//		}
//		
//		double variance=0;
//		for(Entry<Double,Integer>e:map.entrySet()){
//			
//			variance += e.getValue().intValue()/map.size()*(e.getKey()-m)*(e.getKey()-m);
//		}
//		
//		
//		
//		return variance; 
//	}
//	
	

	private void showCalibrationResults(){
		mSensorManager.unregisterListener(this);
		//TODO add calibration results
		calibrationView.loadUrl("file:///android_asset/html/sensor_calibration_results_page.html");
		//updatePage();

		
		
	}
	public void startCalibration(){
		if(!this.manualCalibration){ calibrationView.loadUrl("file:///android_asset/html/sensor_calibration_page.html");}
		sendToast("Calibration Started");
		
		try{
        	//acquire application context and get a reference to power service
           Context context = getApplicationContext();
            PowerManager pm = (PowerManager)context.getSystemService(
                    Context.POWER_SERVICE);
          
            //create a wake lock to prevent phone from sleeping (screen dim/cpu on)
                      
            wakeLock = pm.newWakeLock(
                PowerManager.SCREEN_DIM_WAKE_LOCK,"Lucid Dreaming App");
            //keep the screen on for calibration window +5 minutes
                wakeLock.acquire();
         //      epochsToCalibrate*60*1000+5*60000
                if(D){System.out.println("Wakelock held: "+wakeLock.isHeld());}
                sendToast("Ensure the phone is plugged in for extended use");
        }catch(Exception e){
        	if(D) Log.e(TAG,"Error aquiring wakelock");
        	e.printStackTrace();
        }
		
		
		
		try{	
	         mSensorManager = (SensorManager)getSystemService(Context.SENSOR_SERVICE);
	         mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
	               
	         
	         mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_GAME);
	         
	         if(D)Log.e(TAG, "Accelerometer Statistics:");
	     	if(D)Log.e(TAG, "Name "+mAccelerometer.getName());
	     	if(D)Log.e(TAG, "Resolution"+mAccelerometer.getResolution());
	     	if(D)Log.e(TAG, "Vendor String:"+mAccelerometer.getVendor());
	     	if(D)Log.e(TAG, "Type:"+mAccelerometer.getType());
	     	if(D)Log.e(TAG, "Version:"+mAccelerometer.getVersion());
	        
	     	StringBuilder sb = new StringBuilder();

	    	sb.append(mAccelerometer.getVendor()+" ");
	    	sb.append(mAccelerometer.getName()+" Type: ");
	    	sb.append(mAccelerometer.getType()+" Version:");
	    	sb.append(mAccelerometer.getVersion());
	     	
	     	pageData.put("accelerometerInfo", sb.toString());
	     	pageData.put("resolution",mAccelerometer.getResolution());
	         }catch(Exception e){
	         	e.printStackTrace();
	         }
	         updatePage();
	}
	public void cancelCalibration(){
		try{
		if(!manualCalibration && calibratedEpochs<epochsToCalibrate){sendToast("Calibration Canceled");}
		else{sendToast("Calibration Completed");}
		dataProcessed = true;
		if(mSensorManager!=null){mSensorManager.unregisterListener(this);}
		calibrationView.loadUrl("file:///android_asset/html/sensor_calibration_cancelled_page.html");
		}catch(Exception e){}
		finish();
	}
	
	public void quickCalibration(View view){
		sendToast("Quick Calibration");
		
		calibrationView.reload();
		updatePage();
		manualCalibration = true;
		startCalibration();
		calibrationView.loadUrl("file:///android_asset/html/sensor_manual_calibration_page.html");
	}
	
	public JSONObject getGraphData(){
		return graphData;
	}
	
//	public void setUpGraphData(){
//		try{
//		graphData.put("xData",xJSON);
//		graphData.put("xFiltered",xFilteredJSON);
//		graphData.put("xLabel", "X");
//		graphData.put("xFilteredLabel", "X Filtered");
//		
//		graphData.put("yData",yJSON);
//		graphData.put("yFiltered",yFilteredJSON);
//		graphData.put("yLabel", "Y");
//		graphData.put("yFilteredLabel", "Y Filtered");
//		
//		graphData.put("zData",zJSON);
//		graphData.put("zFiltered",zFilteredJSON);
//		graphData.put("zLabel", "Z");
//		graphData.put("zFilteredLabel", "Z Filtered");
//		}catch(Exception e){
//			
//		}
//		
//	}
	
	
	
	class CountDownTask extends TimerTask{
			
		//TODO marker for this method
		@Override
		public void run() {
			timerCounted60Seconds=true;
			if(D) Log.w(TAG,"60 seconds counted");
		}
		}
	
public String getEpochCalibrationData(){
	StringBuilder sb = new StringBuilder(64);
	
	//String HEADER = ,," +
	//",,,," +;
	
	Calendar calendar = Calendar.getInstance();
	sb.append(calendar.getTimeInMillis());
	sb.append(",");
	sb.append((calendar.getTime().toLocaleString()).replaceAll(",",""));//remove commas from local time
	sb.append(",");	
	sb.append(xActivityCount);
	sb.append(",");	
	sb.append(yActivityCount);
	sb.append(",");
	sb.append(zActivityCount);
	sb.append(",");
	sb.append(this.eventCounter);
	sb.append(",");
	return sb.toString();
	
	
}
	private String getDebugLogEntry(){
		StringBuilder sb = new StringBuilder();
		Calendar calendar = Calendar.getInstance();
		sb.append(calendar.getTimeInMillis());
		sb.append(",");
		sb.append((calendar.getTime().toLocaleString()).replaceAll(",",""));//remove commas from local time
		sb.append(",");	
		sb.append(x);
		sb.append(",");	
		sb.append(xFiltered);
		sb.append(",");
		sb.append(xNoiseVarianceR);
		sb.append(",");		
		sb.append(y);
		sb.append(",");	
		sb.append(yFiltered);
		sb.append(",");
		sb.append(yNoiseVarianceR);
		sb.append(",");
		sb.append(z);
		sb.append(",");	
		sb.append(zFiltered);
		sb.append(",");
		sb.append(zNoiseVarianceR);
		sb.append(",");
		
		return sb.toString();
		
		
	}
	


	public String getPrevActivityStatistics() {
		return prevActivityStatistics;
	}



	/**Performs kalman filtering of the signal and returns the estimated value
	 * Filter must be set up with setupFilter before this method can be called
	 * Filter predicts the value and then is updated with the new measurement
	 * @param observedVariance less variance means more confidence in the observed value
	 * @param observedValue value + noise+reading noise to be filtered
	 * @return the Kalman Filter's best guess for where the observed value really is
	 */
	public static	double KalmanFilter(SimpleFilter filter, double observedValue, double observedVariance){
			
			//Observed Readings
			DenseMatrix64F z= new DenseMatrix64F(1,1);
			z.set(0, 0, observedValue); 
			//Observed readings variance
			DenseMatrix64F R= new DenseMatrix64F(1,1);
			R.set(0, 0, observedVariance);
			
			
			filter.predict(); //calculate the filter prediction
			filter.update(z, R); //get a new state from the observed values
//System.out.println("Observed: "+observedValue+" filtered:"+filter.getState().get(0,0));
			return filter.getState().get(0,0);
		
		}
	
	public static void updateFilter(SimpleFilter filter, double observedValue, double observedVariance){
		//Observed Readings
		DenseMatrix64F z= new DenseMatrix64F(1,1);
		z.set(0, 0, observedValue); 
		//Observed readings variance
		DenseMatrix64F R= new DenseMatrix64F(1,1);
		R.set(0, 0, observedVariance);	
		
		filter.update(z, R); //get a new state from the observed values
	}
	
	
	/**Sets up the passed in filter reference with the simple 1d matrices suitable for
	 * constant evaluation
	 * 
	 * @param filter filter to be set up
	 * @param processNoiseCovarianceQ the measure of confidence you have in how much the process noise affects the reading
	 */
	public static void setupFilter(SimpleFilter filter,double processNoiseCovarianceQ){

		setupFilter(filter,processNoiseCovarianceQ,0);
		
	}
	
	/**Sets up the passed in filter reference with the simple 1d matrices suitable for
	 * constant evaluation
	 * 
	 * @param filter filter to be set up
	 * @param processNoiseCovarianceQ the measure of confidence you have in how much the process noise affects the reading
	 */
	public static void setupFilter(SimpleFilter filter,double processNoiseCovarianceQ, double prediction){

		// TODO Auto-generated method stub
		
		DenseMatrix64F F= new DenseMatrix64F(1,1);
		F.set(0, 0, 1); //1 dimensional matrix, no change is taking place
		
		//process variance
		DenseMatrix64F Q= new DenseMatrix64F(1,1);
		Q.set(0, 0, processNoiseCovarianceQ); //controls how fast the filter follows the measurement
		
		//observation model dynamics
		DenseMatrix64F H= new DenseMatrix64F(1,1);
		H.set(0, 0, 1); 
		filter.configure(F, Q, H);

		//predicted estimate
		DenseMatrix64F x= new DenseMatrix64F(1,1);
		x.set(0,0,prediction);
		//predicted estimate covariance
		DenseMatrix64F P= new DenseMatrix64F(1,1);
		P.set(0, 0, 1); 
		filter.setState(x, P);
		
	}
	
	
	
	
//	
//	private int calculateActivityCount(float value, double valueFiltered,double stDev){
//		double tempStDev = stDev*scalingConstant;
//		int returnValue = 0;
//		if(value>= (valueFiltered+tempStDev)
//		|| value<=(valueFiltered-tempStDev)){
//			returnValue = 1; //1st dev away
//		
//			if(value>= (valueFiltered+2*tempStDev)
//					|| value<=(valueFiltered-2*tempStDev)){
//						returnValue = 4; //2 st dev away
//						if(value>= (valueFiltered+3*tempStDev)
//								|| value<=(valueFiltered-3*tempStDev)){
//									returnValue = 9; //3 st dev away
//									if(value>= (valueFiltered+4*tempStDev)
//											|| value<=(valueFiltered-4*tempStDev)){
//												returnValue = 16; //4st dev away
//												
//												if(value>= (valueFiltered+5*tempStDev)
//														|| value<=(valueFiltered-5*tempStDev)){
//															returnValue = 25; //5st dev away
//															
//															if(value>= (valueFiltered+6*tempStDev)
//																	|| value<=(valueFiltered-6*tempStDev)){
//																return 36;
//															}
//														}
//											}
//								}
//					}
//			String text ="Activity Count "+epochXYZActivityCount +" incremented by "+returnValue;
//			setWebViewText(text);
//			Log.w(TAG,text);
//			return returnValue;
//		}else{return 0;}
//	}
//		
//		private double calculateActivitySum(double value, double valueFiltered,double stDev){
//			double tempStDev = stDev*scalingConstant;
//			if(value>= (valueFiltered+tempStDev)
//					|| value<=(valueFiltered-tempStDev)){
//					return (value>=(valueFiltered + tempStDev))?
//				(value-(valueFiltered + tempStDev)) //if true
//				:Math.abs(value-(valueFiltered - tempStDev));//if false
//			
//					}
//			return 0;
			
			//if the reading is outside the envelope, see how far it is from the envelope
		//this becomes the new measure of activity
	
//		
//	}
	

	public float getX() {
		return x;
	}




	public void setX(float x) {
		this.x = x;
	}




	public float getY() {
		return y;
	}




	public void setY(float y) {
		this.y = y;
	}




	public float getZ() {
		return z;
	}




	public void setZ(float z) {
		this.z = z;
	}






	public double getXFiltered() {
		return xFiltered;
	}



	public void setXFiltered(double filtered) {
		xFiltered = filtered;
	}



	public double getYFiltered() {
		return yFiltered;
	}



	public void setYFiltered(double filtered) {
		yFiltered = filtered;
	}



	public double getZFiltered() {
		return zFiltered;
	}



	public void setZFiltered(double filtered) {
		zFiltered = filtered;
	}



	public int getXActivityCount() {
		return xActivityCount;
	}



	public void setXActivityCount(int activityCount) {
		xActivityCount = activityCount;
	}



	public int getYActivityCount() {
		return yActivityCount;
	}



	public void setYActivityCount(int activityCount) {
		yActivityCount = activityCount;
	}



	public int getZActivityCount() {
		return zActivityCount;
	}



	public void setZActivityCount(int activityCount) {
		zActivityCount = activityCount;
	}



	public int getMagnitudeActivityCount() {
		return magnitudeActivityCount;
	}



	public void setMagnitudeActivityCount(int magnitudeActivityCount) {
		this.magnitudeActivityCount = magnitudeActivityCount;
	}



	public double getXActivitySum() {
		return xActivitySum;
	}



	public void setXActivitySum(double activitySum) {
		xActivitySum = activitySum;
	}



	public double getYActivitySum() {
		return yActivitySum;
	}



	public void setYActivitySum(int activitySum) {
		yActivitySum = activitySum;
	}



	public double getZActivitySum() {
		return zActivitySum;
	}



	public void setZActivitySum(int activitySum) {
		zActivitySum = activitySum;
	}



	public double getMagnitudeActivitySum() {
		return magnitudeActivitySum;
	}



	public void setMagnitudeActivitySum(int magnitudeActivitySum) {
		this.magnitudeActivitySum = magnitudeActivitySum;
	}






	public boolean isAccelerometerCalibrated() {
		return calibrated;
	}

	public int isCalibratedInt() {
		if(calibrated){
			return 1;
		}else return 0;
	}

	
	
	


	@Override
	protected void onDestroy() {
		// TODO Auto-generated method stub
		super.onDestroy();
		
		try{
			wakeLock.release();
			if(D)Log.e(TAG, "On Pause Wakelock is held: "+wakeLock.isHeld());
			cancelCalibration();
		}catch(Exception e){}
		
		
	}



	@Override
	protected void onPause() {
		// TODO Auto-generated method stub
		super.onPause();
		try{
			wakeLock.release();
			if(D)Log.e(TAG, "On Pause Wakelock is held: "+wakeLock.isHeld());
		}catch(Exception e){}
		cancelCalibration();
		finish();
	}


	
	protected void generateTestData(){
	try{
		pageData.put("accelerometerInfo", "Test accelerometerInfo");
		pageData.put("accelerometerResolution", "Test accelerometerResolution");
		pageData.put("accelerometerStep", "Test accelerometerStep");
		pageData.put("countMean", "Test countMean");
		
		pageData.put("thresholdLowSensitivity", "Test thresholdLowSensitivity");
		pageData.put("thresholdMediumSensitivity", "Test thresholdMediumSensitivity");
		pageData.put("thresholdHighSensitivity", "Test thresholdHighSensitivity");
		pageData.put("thresholdVeryHighSensitivity", "Test thresholdVeryHighSensitivity");
		
		pageData.put("coleConstantLowThreshold", "Test coleConstantLowThreshold");
		pageData.put("coleConstantMediumThreshold", "Test coleConstantMediumThreshold");
		pageData.put("coleConstantHighThreshold", "Test coleConstantHighThreshold");
		pageData.put("coleConstantVeryHighThreshold", "Test coleConstantVeryHighThreshold");
	}catch(JSONException e){}
		
	}
	

	protected void sendToast(String message){
    	Context context = getApplicationContext();
		  Toast toast = Toast.makeText(context, message,Toast.LENGTH_SHORT);
		  toast.show();
    }
    
}
