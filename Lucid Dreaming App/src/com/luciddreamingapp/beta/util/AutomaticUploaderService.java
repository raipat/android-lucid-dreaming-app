package com.luciddreamingapp.beta.util;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.util.Log;

import com.google.gson.Gson;
import com.luciddreamingapp.beta.GlobalApp;
import com.luciddreamingapp.beta.HistoryViewingActivity;
import com.luciddreamingapp.beta.LucidDreamingApp;
import com.luciddreamingapp.beta.util.analysis.AnalysisDataPoint;
import com.luciddreamingapp.beta.util.analysis.AnalysisNight;
import com.luciddreamingapp.beta.util.analysis.AnalysisTester;

public class AutomaticUploaderService extends Service {
	
	//debug
	private static final boolean D = true;
	private static final String TAG = "Lucid Dreaming App Science Uploader";
	
	private static final String OK = "OK";//ok result from the server
	private static final String D_ ="D_";//date
	private static final String T_ ="_T_";//time
	private static final String U_ ="_U_";//uuid prefix
	private static final String EXT = ".txt";//extension
	
	private static final int graphSize = 10;//kb minimum upload
	
	private static GlobalApp A;
	
	
	//ensures actigraph events are delivered to the receiver
	private static IntentFilter filter;
	//listens to actigraph events
	private static SendCompletedReceiver receiver;
	
	
	private NotificationManager mNM;
	private int NOTIFICATION_ID =92;
	private Notification notification;
	
	private FileUploader uploader;
	
	private List<File> graphList; //list of graphs available
	
	List<String> filepathList;//list of files to upload
	HashMap<String,String> uploadedFiles;//a mapping of graph name to temp file names
	
	UploadConfig config;
	
	//timer to automatically terminate the service
	Timer timer;
	
	
	@Override
	public void onCreate() {
		// TODO Auto-generated method stub
		super.onCreate();
		A = (GlobalApp)getApplication();
		
		if(!checkConnectionStatus()){this.stopSelf();}
		
		if(D){
			Log.e(TAG, "CREATED");
			Log.w(TAG, "uploadToday:"+uploadToday());
			Log.w(TAG, "connected:"+checkConnectionStatus());
		}
		 mNM = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
		 uploadedFiles = new HashMap<String,String>();
	}

	@Override
	public void onDestroy() {
		// TODO Auto-generated method stub
		super.onDestroy();
		
		if(receiver!=null){
		unregisterReceiver(receiver);
		}
		
		try{
		if(timer!=null){
			timer.cancel();
			timer.purge();
		}}catch(Exception e){}
		
		UploadConfig.saveConfig();
		if(D){
			Log.e(TAG, "DESTROYED");
		}
	}
	
	
	private boolean uploadExperimental(){
	
		AnalysisTester tester = new AnalysisTester();
		for(int i = 0;i<30;i++){
		File f = tester.analyze();
		uploader.openConnection();
		//ftpUpload(f,processName(f));
		String result = uploader.upload(f, f.getName());
		if(D)Log.e(TAG,"Result: "+result);
		if(result!=null && result.equals(OK)){
			if(D)Log.e(TAG,"Uploaded: "+f.getName());
			//remember this upload
			//uploadedList.add(f.getName());
		}else{								
			if(D)Log.e(TAG,"did not upload: "+f.getName());
		}
		
		uploader.closeConnection();
		}
		return true;
	}
	
	private boolean uploadFiles(){
		if(graphList==null||graphList.size()==0)return false;
		long timestamp = System.currentTimeMillis();
		try{

		
			filepathList = new ArrayList<String>();
		for(File f: graphList){
			if(verifyFile(f)){
			//methods call each other 
			}
			
			
		}
		
		//if there are files to upload
		if(filepathList.size()>0){
			
			//listen to a broadcast notifying us that the data has been uploaded
			filter = new IntentFilter("com.luciddreamingapp.uploader.UPLOAD_COMPLETE");
			receiver = new SendCompletedReceiver();
			
			//connect the reciver and the filter
			 registerReceiver(receiver, filter);
			
			
			
			//intent with upload action, broadcast it
		Intent uploadIntent = new Intent("com.luciddreamingapp.uploader.START_UPLOAD");
		String[] array = new String[filepathList.size()];
		int counter = 0;
		for(String s: filepathList){
			if(D)Log.v(TAG,s);
			array[counter++] = s;
		}
		
		
		
		uploadIntent.putExtra("filepaths",array);
		uploadIntent.putExtra("uploader",3);
		sendBroadcast(uploadIntent);

		if(D)Log.e(TAG,"uploaded in: "+(System.currentTimeMillis()-timestamp)/1000+" sec" );
		}else{
			if(D)Log.e(TAG, "No files to upload");
		
			stopSelf();
		}
		
		return true;
		}catch(Exception e){
			if(D)e.printStackTrace();
			return false;
		}
	}
	

	/**Ensures that this is a graph file
	 * 
	 * @return
	 */
	private boolean verifyFile(File f){
		/*
		 * Opens the file
		 * catches exceptions
		 * checks version
		 * determines proper parsing method
		 * passes the file data to parser
		 */
		if(D)Log.i(TAG,"verify file");
		if(D)Log.i(TAG,f.getName()+" contains: "+config.getFilenames().contains(f.getName()));
		if(D)Log.i(TAG,"length: "+f.length()/1024);
		
		if(f.length()/1024>graphSize
				&&!config.getFilenames().contains(f.getName())){
			if(D)Log.i(TAG,"inside verify");
			
			JSONObject temp = null;
			try {
				 temp = JSONLoader.getData(f);
			} catch (IOException e) {
				///if there's an error reading, do not process, but mark as read
				config.getFilenames().add(f.getName());
				if(D)e.printStackTrace();
				return false;
			}
			
			//check filetype within the 
			String filetype =null;
			
			try {
				filetype = (String)temp.get("filetype");
				if(!filetype.equals(SleepDataManager.GRAPH_TYPE))return false;
			} catch (JSONException e) {
				
				//if no such string, this is not a correct file - do not process it
				//mark as read to speed up subsequent processing
				config.getFilenames().add(f.getName());
			
				if(D)e.printStackTrace();
				return false;
			}
			
			int version = 0;
			try {
				version = temp.getInt("version");
				
			} catch (JSONException e) {
				if(D)Log.i(TAG,"calling process JSON");
				processJSON(temp,f);
				//process all format
				
		//		if(D)e.printStackTrace();
				return true;
			}
			
			if(version ==2){
				processJSON(temp,f);
				//process new format
				return true;
			}else{
				if(D)Log.i(TAG,"calling process JSON");
				processJSON(temp,f);
				//process old format
				return true;
			}
			
		}
	
		
		return false;
	}
	
	
	/**Processes file to reduce its size and make further processing easier
	 * 
	 * @return
	 */
	private String processJSON(JSONObject json, File f){
		
		if(D)Log.e(TAG, "*process json called*");
		//rewrite old format into the new more space efficient format (cannot be easily displayed)
		AnalysisNight night = new AnalysisNight();
		
		try{
			
			//extracts statistics
			//gets first, last timestamp
			night.uuid = A.uuid;
			night.se = json.getInt("sleepEfficiency");
			night.sol =json.getInt("sleepOnsetLatency");
			night.numAw = json.getInt("numberOfAwakenings");//awakenings
			night.numD= json.getInt("numberOfDreams"); //dreams
			night.numDL= json.getInt("numberOfLucidDreams"); //lucid dreams
			night.ue = json.getInt("numberOfUserEvents"); //user events
			night.date = json.getString("Date Created:");
			night.el = SleepDataManager.epochLength; //epoch length
			night.tst = json.getInt("totalMinutesAsleep");
			night.ttib =json.getInt("totalTimeInBed");
			
			//find the end of the array and get the last timestamp
			night.endTS= json.getJSONArray("sleepEpoch")
			.getJSONArray(json.getJSONArray("sleepEpoch").length()-1).getLong(0);//get the last timestamp
			
			try{
				night.startTS=json.getLong("firstTimestamp");
				if(D)Log.e(TAG,"Timestamp"+(new Date(night.startTS)));
			}catch(Exception ee){
				//if no first timestamp - get it from the array
				night.startTS= json.getJSONArray("sleepEpoch")
				.getJSONArray(0).getLong(0);//get the first timestamp
				}
			night.tzOffset= json.getInt("timeZoneOffset");
			
									
		}catch(Exception e){
			if(D)e.printStackTrace();
		}
		
			//creates an array of AnalysisDataPoints
		
	AnalysisDataPoint dataPoint  = new AnalysisDataPoint();
	JSONArray activity=null,sleepScore=null,totalSleep=null,audioLevel=null,audioKurtosis=null,
				dreams = null, lucidDreams = null, awake = null, userEvents = null, noDream = null, reminder = null;
	try{
	 activity = json.getJSONArray("xyzActivityCount");
	 sleepScore = json.getJSONArray("coleSleepScore");
	 totalSleep  = json.getJSONArray("totalSleepDuration");
	 dreams = json.getJSONArray("userEventDream");	 
	 lucidDreams = json.getJSONArray("userEventLucidDream");	 
	 awake = json.getJSONArray("userEventAwake");	 
	 noDream = json.getJSONArray("userEventNoDream");	 
	 userEvents = json.getJSONArray("userEvent");
	 reminder  = json.getJSONArray("reminderPlayed");
	 
	}catch(Exception e){
		if(D)e.printStackTrace();
	}
	try{
	 audioLevel  = json.getJSONArray("audioLevel");
	 audioKurtosis  = json.getJSONArray("audioLevelKurtosis");
	}catch(Exception e){
		audioLevel = null;
		audioKurtosis = null;
		//these may not be present in older versions
	}
	//process user events differently, because they are not in order
//	JSONArray userEvents  = json.getJSONArray("xyzActivityCount");
	
	int[] userEventArray = new int[night.ttib];
	int[] reminderArray = new int[night.ttib];
	Arrays.fill(reminderArray, 0);
	Arrays.fill(userEventArray, SleepDataPoint.NO_EVENT);
	
	long firstTimeStamp = night.startTS;
	
	//expand the short 7-10 element array with (timestamp, y coordinate) into an
	//array encompassing whole night for easy iteration over it. 
	try{
	for (int i = 0; i<dreams.length();i++){
		if( (Integer)(dreams.getJSONArray(i).get(1))>0){
			long tempTimeStamp = (Long)(dreams.getJSONArray(i).get(0));
			userEventArray[(int)((tempTimeStamp-firstTimeStamp)/60000)]=SleepDataPoint.USER_EVENT_DREAM;
		}
	}}catch(Exception e){if(D)e.printStackTrace();}
	
	try{
	for (int i = 0; i<lucidDreams.length();i++){
		if( (Integer)(lucidDreams.getJSONArray(i).get(1))>0){
			long tempTimeStamp = (Long)(lucidDreams.getJSONArray(i).get(0));
			userEventArray[(int)((tempTimeStamp-firstTimeStamp)/60000)]=SleepDataPoint.USER_EVENT_LUCID_DREAM;
		}
	}}catch(Exception e){if(D)e.printStackTrace();}
	
	try{
	for (int i = 0; i<awake.length();i++){
		if( (Integer)(awake.getJSONArray(i).get(1))>0){
			long tempTimeStamp = (Long)(awake.getJSONArray(i).get(0));
			userEventArray[(int)((tempTimeStamp-firstTimeStamp)/60000)]=SleepDataPoint.USER_EVENT_AWAKE;
		}
	}}catch(Exception e){if(D)e.printStackTrace();}
	
	try{
	for (int i = 0; i<noDream.length();i++){
		if( (Integer)(noDream.getJSONArray(i).get(1))>0){
			long tempTimeStamp = (Long)(noDream.getJSONArray(i).get(0));
			userEventArray[(int)((tempTimeStamp-firstTimeStamp)/60000)]=SleepDataPoint.USER_EVENT_NO_DREAM;
		}
	}}catch(Exception e){if(D)e.printStackTrace();}
	
	try{
	for (int i = 0; i<userEvents.length();i++){
		if( (Integer)(userEvents.getJSONArray(i).get(1))>0){
			long tempTimeStamp = (Long)(userEvents.getJSONArray(i).get(0));
			userEventArray[(int)((tempTimeStamp-firstTimeStamp)/60000)]=SleepDataPoint.USER_EVENT_NOT_RECOGNIZED;
		}
	}}catch(Exception e){if(D)e.printStackTrace();}
	

	
	try{
		for (int i = 0; i<reminder.length();i++){
			if( (Integer)(reminder.getJSONArray(i).get(1))>0){
				long tempTimeStamp = (Long)(reminder.getJSONArray(i).get(0));
				reminderArray[(int)((tempTimeStamp-firstTimeStamp)/60000)]=1;
			}
		}}catch(Exception e){if(D)e.printStackTrace();}
		

	
	
	if(activity!=null && activity.length()>1){
		for(int i = 0; i<activity.length();i++){
			dataPoint = new AnalysisDataPoint();
			
			try {
				dataPoint.ac = activity.getJSONArray(i).getInt(1);
				
				long timestamp = activity.getJSONArray(i).getLong(0);
				//turn it in 0.5 day to 1.5 day timestamp
				timestamp = HistoryViewingActivity.rewriteTimestamp(timestamp);
				timestamp -= HistoryViewingActivity.halfDayMillis; //turn to 0 to 1 day millis
				//convert to minutes from 12PM to 12PM for easy comparison
				timestamp =timestamp/HistoryViewingActivity.minuteMillis;
				dataPoint.st =(int)timestamp;
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} //activity count
			try {
				if(audioLevel!=null)
				dataPoint.al = audioLevel.getJSONArray(i).getInt(1);
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			try {
				if(audioKurtosis!=null)
				dataPoint.ak = audioKurtosis.getJSONArray(i).getInt(1);
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			try {
				dataPoint.ss = sleepScore.getJSONArray(i).getInt(1);
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			try {
				dataPoint.ts = totalSleep.getJSONArray(i).getInt(1);
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			try{
			dataPoint.ue = userEventArray[i];
			dataPoint.v = reminderArray[i];
			}catch(Exception e){
				if(D)e.printStackTrace();
			}
			
			dataPoint.ep= i+1;
			
			
	
			
									
			night.list.add(dataPoint);
			
		}
		
	}
	
	
	/*
	 * Crazy inefficient logic to zip the file, read it and upload it, because I dont know how to manipulate streams well enoug
	 * too zip a string and upload it as a file
	 */
	if(D)Log.e(TAG, night.toString());
	Gson gson = new Gson();
	String nightJSON = gson.toJson(night);
	if(D)Log.e(TAG, nightJSON);
	
	String filename = LogManager.writeToAndZipFile(
			LucidDreamingApp.UPLOAD_LOCATION, this.processName(f.getName()), nightJSON	);
	
	uploadedFiles.put(filename, f.getName());//map graph name value to temp file key
	
	if(D)Log.e(TAG, "saved:"+filename);
	File temp = new File(filename);
	
	
	//remember the file that we just processed
	filepathList.add(filename);
	
	
	
	//temp is the file on disk
	//tempFileUpload(temp,temp.getName());
	//temp.delete();

	
	
		//creates analysis night object
		//writes to output
		
		
		
		return null;
	}

	



	/**Gives a new name to the file on the server
	 * 
	 * @return
	 */
	public String processName(String name){
	//names like	Data_20110520_0219
		//get rid of the extension
		name = name.replaceFirst("[.].*","");
		String[] temp = name.split("_");
		if(temp.length<3){
			return name+".gzip";
		}
		StringBuffer sb = new StringBuffer(64);
		sb.append(D_);	
		sb.append(temp[1]);//date
		sb.append(T_);
		sb.append(temp[2]);//time
		sb.append(U_);
		sb.append(A.uuid);
		sb.append(EXT);
		return sb.toString();
	}
	
	public String processName(File f){
		return processName(f.getName());
	}
	
	
	public boolean uploadToday(){
		//calendar and settings check
	
	int uploadDay =	((GlobalApp)getApplication()).getPreferences().getInt("uploadDay",3);
	int weekDay = Calendar.getInstance().get(Calendar.DAY_OF_WEEK);
	if(D)Log.w(TAG,"weekDay:"+weekDay);
	return uploadDay==weekDay;
	
	}
	
	
	public boolean checkConnectionStatus(){
		try{
			//TODO remove comments after testing
		//return ((GlobalApp)getApplication()).getConnectionStatus();
			return true;
		}catch(Exception e){
			if(D)e.printStackTrace();
			return false;
		}
	}
	
	
	public List<File> getGraphs(){
		return FileHelper.getFiles(new File(Environment.getExternalStorageDirectory(),LucidDreamingApp.GRAPH_DATA_LOCATION).getAbsolutePath(), FileHelper.createFileFilter(".*[.]gzip"));
	}


	@Override
	public IBinder onBind(Intent arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	
	private class UploadTask extends AsyncTask<Void, Void, Void> {
  		 
		 protected Void doInBackground(Void... urls) {
			if(checkConnectionStatus()){
			//	uploadExperimental();
    		uploadFiles();
			 }
	        return null;
	     }

		@Override
		protected void onPostExecute(Void result) {
			// TODO Auto-generated method stub
			super.onPostExecute(result);
			//A.sendShortToast("Upload completed");
			Intent notificationIntent = new Intent(AutomaticUploaderService.this, LucidDreamingApp.class);
			PendingIntent pendingIntent = PendingIntent.getActivity(AutomaticUploaderService.this, 0, notificationIntent, 0);
			notification.setLatestEventInfo(AutomaticUploaderService.this, "Lucid Dreaming App",
			       "Data upload completed", pendingIntent);
			mNM.notify(NOTIFICATION_ID, notification);
			mNM.cancel(NOTIFICATION_ID);		

			
			timer = new Timer();
			timer.schedule(new WatchDogTimer(), 600000);
			//stopSelf();
			
		
			
			
		}
		
	 }
	

	
	//stops the service after a timeout
	private class WatchDogTimer extends TimerTask{

		@Override
		public void run() {
			stopSelf();
			// TODO Auto-generated method stub
			
		}
		
	}
	
	
	private class CleanUpTask extends AsyncTask<Void, Void, Void> {
  		 
		 protected Void doInBackground(Void... urls) {
			
			 if(D)Log.v(TAG, "cleanup task ");
			 
		
			 for(int i = 0; i<filepaths.length;i++){
				 if(D)Log.v(TAG, filepaths[i]);
				 if(uploadedFiles.containsKey(filepaths[i])){
					 if(D)Log.v(TAG, "contains: true");
					 //remember the graph name that was uploaded
					 config.getFilenames().add(uploadedFiles.get(filepaths[i]));
					 
					 //delete the temporary file
					 File f = new File(filepaths[i]);
					 f.delete();				 
					 
					 
				 }
			 
			 }
			 			
			 //get a list of uploaded files, and delete them from the sdcard
//			 if(urls[0]!=null){
//			 String[] temp = urls[0];
//			 for(int i = 0; i<temp.length;i++){
//				 if(D)Log.v(TAG, "Received: "+temp[i]);
//				 
//				 if(D)Log.w(TAG,"map: "+uploadedFiles.get(temp[i]));
//			 	}
//			 
//			 }
	        return null;
	     }

		@Override
		protected void onPostExecute(Void result) {
			// TODO Auto-generated method stub
			super.onPostExecute(result);
		
			
			stopSelf();
		}
		
	 }



	@Override
	public void onStart(Intent intent, int startId) {
		// TODO Auto-generated method stub
		super.onStart(intent, startId);
		boolean automatic = true;
		try {
			if(intent.getExtras()!=null){
				automatic =intent.getExtras().getBoolean("automatic");				
			}
		} catch (Exception e) {		
			if(D)e.printStackTrace();
		}
		//exit if not connected
	
		
		if(automatic && uploadToday()){
			//do automatic upload
			uploader = new FileUploader(A.uuid,A.serverUrl);
		}else if(!automatic){
			//manual upload
			uploader = new FileUploader(A.uuid,A.serverUrlManual);
		}
				
		
		//remember what was uploaded before
		config = UploadConfig.getInstance();		
		for(String s: config.getFilenames()){
			if(D)Log.w(TAG,"Already uploaded: "+s);
		}
		
		graphList = getGraphs();
		for(File f: graphList){
						
		if(D)	Log.w(TAG,"filename: "+f.getName()+" size: "+f.length()/1024+" kb");
			if(config.getFilenames().contains(f.getName())){
				if(D)Log.w(TAG,"Contains: "+config.getFilenames().contains(f.getName()));
			}else{
				if(D)Log.w(TAG,"Does not contain: "+(f.getName()));
			//	config.getFilenames().add(f.getName());
			}
			
		}
		 notification = new Notification(android.R.drawable.stat_notify_sync


				, "Starting Data uploading",  System.currentTimeMillis());
				Intent notificationIntent = new Intent(AutomaticUploaderService.this, LucidDreamingApp.class);
				notificationIntent.putExtra("stopService", true);
				PendingIntent pendingIntent = PendingIntent.getActivity(AutomaticUploaderService.this, 0, notificationIntent, 0);
								notification.setLatestEventInfo(AutomaticUploaderService.this, "Lucid Dreaming App",
								       "Data is being uploaded", pendingIntent);
	
			mNM.notify(NOTIFICATION_ID, notification);
		//initialize the uploader
	//	config.saveConfig();
		//stopSelf();
		new UploadTask().execute(null);
		
		
		
		
	}
	
	
	String[] filepaths;
	
	class SendCompletedReceiver extends BroadcastReceiver{

		@Override
		public void onReceive(Context arg0, Intent intent) {
			// TODO Auto-generated method stub
			if(D)Log.e(TAG,"Received broadcast");
			
			Bundle extras =	intent.getExtras();
			
			//get the list of uploaded files and remember it
			if(extras!=null){
				filepaths = extras.getStringArray("filepaths");
				
				new CleanUpTask().execute(null);
			}else{
				stopSelf();
			}
			
		
			
		}
		
	}
	
	
	
	
}
