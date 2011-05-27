package com.luciddreamingapp.beta.util;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.AsyncTask;
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
	private static final boolean D = false;
	private static final String TAG = "Lucid Dreaming App Science Uploader";
	
	private static final String OK = "OK";//ok result from the server
	private static final String D_ ="D_";//date
	private static final String T_ ="_T_";//time
	private static final String U_ ="_U_";//uuid prefix
	private static final String EXT = ".txt.gzip";//extension
	
	private static final int graphSize = 10;//kb minimum upload
	
	private static GlobalApp A;
	
	
	
	private NotificationManager mNM;
	private int NOTIFICATION_ID =92;
	private Notification notification;
	
	private FileUploader uploader;
	
	private List<File> graphList;
	
	
	
	UploadConfig config;
	
	
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
	}

	@Override
	public void onDestroy() {
		// TODO Auto-generated method stub
		super.onDestroy();
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

		
		
		for(File f: graphList){
			if(verifyFile(f)){
				
				//fileUpload(f);

			}
			
		}

		if(D)Log.e(TAG,"uploaded in: "+(System.currentTimeMillis()-timestamp)/1000+" sec" );
		return true;
		}catch(Exception e){
			if(D)e.printStackTrace();
			return false;
		}
	}
	
	/**
	 * Uploads a temp file using the name of the file provided
	 * @param f
	 * @param filename
	 */
	private void tempFileUpload(File f, String filename){
		//upload and check the result, reformat the name
		uploader.openConnection();
		//ftpUpload(f,processName(f));
		String result = uploader.upload(f, processName(filename));
		if(D)Log.e(TAG,"Result: "+result);
		if(result!=null && result.equals(OK)){
			if(D)Log.e(TAG,"Uploaded: "+filename);
			//remember this upload
			config.getFilenames().add(filename);
		}else{
								
			if(D)Log.e(TAG,"did not upload: "+filename);
		}
		
		uploader.closeConnection();
	}
	
	private void fileUpload(File f){
		//upload and check the result, reformat the name
		uploader.openConnection();
		//ftpUpload(f,processName(f));
		String result = uploader.upload(f, processName(f));
		if(D)Log.e(TAG,"Result: "+result);
		if(result!=null && result.equals(OK)){
			if(D)Log.e(TAG,"Uploaded: "+f.getName());
			//remember this upload
			config.getFilenames().add(f.getName());
		}else{
								
			if(D)Log.e(TAG,"did not upload: "+f.getName());
		}
		
		uploader.closeConnection();
	}
	
	private void stringUpload (String s, File f){
		//upload and check the result, reformat the name
		uploader.openConnection();
		//ftpUpload(f,processName(f));
		String result = uploader.upload(s, processName(f));
		if(D)Log.e(TAG,"Result: "+result);
		if(result!=null && result.equals(OK)){
			if(D)Log.e(TAG,"Uploaded: "+f.getName());
			//remember this upload
			config.getFilenames().add(f.getName());
		}else{
								
			if(D)Log.e(TAG,"did not upload: "+f.getName());
		}
		
		uploader.closeConnection();
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
		
		if(f.length()/1024>graphSize
				&&!config.getFilenames().contains(f.getName())){
		
			
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
				processJSON(temp,f);
				//process all format
				
		//		if(D)e.printStackTrace();
				return true;
			}
			
			if(version ==2){
				//process new format
				return true;
			}else{
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
	JSONArray activity=null,sleepScore=null,totalSleep=null,audioLevel=null,audioKurtosis=null;
	try{
	 activity = json.getJSONArray("xyzActivityCount");
	 sleepScore = json.getJSONArray("coleSleepScore");
	 totalSleep  = json.getJSONArray("totalSleepDuration");
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
			LucidDreamingApp.APP_HOME_FOLDER, "tempfile.txt", nightJSON	);
	
	File temp = new File(filename);
	
	tempFileUpload(temp,f.getName());
	temp.delete();

	
	
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
		return ((GlobalApp)getApplication()).getConnectionStatus();
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
	
	
	
	
}
