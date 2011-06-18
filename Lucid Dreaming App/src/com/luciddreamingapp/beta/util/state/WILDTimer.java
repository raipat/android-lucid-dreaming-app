package com.luciddreamingapp.beta.util.state;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import org.json.JSONArray;
import org.json.JSONObject;

import android.app.Activity;
import android.util.Log;

import com.google.gson.Gson;
import com.luciddreamingapp.beta.GlobalApp;
import com.luciddreamingapp.beta.util.DataManagerObserver;
import com.luciddreamingapp.beta.util.JSONLoader;
import com.luciddreamingapp.beta.util.MorseCodeConverter;
import com.luciddreamingapp.beta.util.SleepDataPoint;

public class WILDTimer implements DataManagerObserver {

	private static final boolean D = false;//debug
	private static final String TAG = "WILD Timer";
	
	private GlobalApp parent = null;
	private Activity activityParent = null;
	private List<WILDEventVO> list;
	private Timer wildTimer;
	
	private SleepDataPoint epoch;
	
	private String configFilepath;
	
	public WILDTimer(String configFilepath){
		this.configFilepath = configFilepath;
		//get the config file and unpack it, creating objects from google GSON
		try{
			list =   new ArrayList<WILDEventVO>();
			File file = new File(configFilepath);
			//File file =new File(Environment.getExternalStorageDirectory(),new File(LucidDreamingApp.APP_HOME_FOLDER, "WILDTimerConfig.txt.gzip").getAbsolutePath());
			JSONObject json =JSONLoader.getData(file);
			
			Gson gson = new Gson();
			JSONArray eventObjects =json.getJSONArray("eventObjects");
			//array to sort objects by start time, just in case they do not come out in order
			WILDEventVO[] array = new WILDEventVO[eventObjects.length()];
			for(int i =0;i<eventObjects.length();i++){
				try{
					
					WILDEventVO vo =gson.fromJson(eventObjects.getString(i), WILDEventVO.class) ;
					array[i]=vo;
					
				}catch(Exception e){}
			}
			//just in case - resort the array in ascending order by startTime
			Arrays.sort(array, new ListEventComparator());
			for(int i = 0;i<array.length;i++){
				list.add(array[i]);
				if(D)System.out.println(array[i].startMinute);
			}
			
			wildTimer = new Timer();

			 }catch(Exception e){
				 
			 }
		
	}
	
	public boolean scheduleTimers(){
		try{
			wildTimer.cancel();
			wildTimer.purge();
			wildTimer = null;
		}catch(Exception e){}
		//start
		//8, 4, 8, 12, 16, 20, 20, 6
		wildTimer = new Timer();
		
		for(WILDEventVO vo: list){
			determineInteraction(vo);
		}
		
//		wildTimer.schedule(new WILDTimerVibrateTask(), 8*60*1000);
//		wildTimer.schedule(new WILDTimerVibrateTask(), (8+4)*60*1000);
//		wildTimer.schedule(new WILDTimerVibrateTask(), (8+4+12)*60*1000);
//		wildTimer.schedule(new WILDTimerVibrateTask(), (8+4+12+16)*60*1000);
//		wildTimer.schedule(new WILDTimerVibrateTask(), (8+4+12+16+20)*60*1000);
//		wildTimer.schedule(new WILDTimerVibrateTask(), (8+4+12+16+20+20)*60*1000);
//		wildTimer.schedule(new WILDTimerVibrateTask(), (8+4+12+16+20+20+6)*60*1000);
		return false;
	}
	
	public boolean cancelTimers(){
		
		try{
			wildTimer.cancel();
			wildTimer.purge();
			wildTimer = null;
			}catch(Exception e){if(D)e.printStackTrace();}
		
		return false;
	}
	
	class WILDTimerTask extends TimerTask{
		final WILDEventVO vo;
		WILDTimerTask(WILDEventVO vo){
			this.vo = vo;
		}
				
		@Override
		public void run() {
			//do nothing if the app crashed
			if(parent==null)return;
			
			boolean reminderPlayed = false;
			if(vo.useVoiceReminder &&vo.reminderFilepath!=null){
				if(D)Log.w(TAG,"starting voice Interaction "+vo.reminderFilepath);
				{				
				parent.voiceInteractAsync(vo.reminderFilepath);
				reminderPlayed = true;
				}
				
			}
				
			
			if(vo.useVibrateReminder && vo.vibrateMessage!=null&& !vo.vibrateMessage.equals("")){
				
				//vibration reminder
				//parentHandler.post(new VibrationReminder());
				long temp = 10;//10 ms minimum
				   if(vo.vibrateDotDuration>1)temp =vo.vibrateDotDuration; 
				   if(D)Log.w(TAG,"starting vibrate Interaction: "+temp+" vibrate message"+vo.vibrateMessage );
				   parent.vibrateInteractAsync(MorseCodeConverter.pattern(vo.vibrateMessage, temp));
				   reminderPlayed = true;
			
			}
			if(vo.useStrobe &&vo.flashMessage!=null &&!vo.flashMessage.equals("")){
				long temp = 10;
				   if(vo.flashDotDuration>1)temp =vo.flashDotDuration; 
				   if(D)Log.w(TAG,"starting strobe Interaction: "+temp+" strobe message"+vo.flashMessage );
				   parent.strobeInteractAsync(MorseCodeConverter.pattern(vo.flashMessage, temp));
				   reminderPlayed = true;
			
			}
			
			
			if(reminderPlayed&& epoch!=null){
				epoch.setReminderPlayed(true);
				int temp =epoch.getHistoryStatistics().getNumberOfVoiceReminders();
				epoch.getHistoryStatistics().setNumberOfVoiceReminders(temp+1);
			}
			
			
		}
		
	}
	
	public GlobalApp getParent() {
		return parent;
	}

	public void setParent(GlobalApp parent) {
		this.parent = parent;
	}
	
	public void determineInteraction(WILDEventVO vo){
		
		
		//return if the event is not active, there's no parent or no defined VO
		if(!vo.reminderSet||parent==null||vo==null){return;}
			
		if(D)Log.w(TAG,"determine called "+vo.startMinute);
		//if the event is active and it's start minute is defined
		
		
			wildTimer.schedule(new WILDTimerTask(vo),vo.startMinute*60000);
	
		
	}

	public Activity getActivityParent() {
		return activityParent;
	}

	public void setActivityParent(Activity activityParent) {
		this.activityParent = activityParent;
	}

	@Override
	public void dataPointAdded(SleepDataPoint epoch) {
		// TODO Auto-generated method stub
		this.epoch = epoch;
	}

	@Override
	public void dataPointUpdated(SleepDataPoint epoch) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void listUpdated(String innerHTML) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void graphUpdated(JSONObject graphData) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void dataReset() {
		// TODO Auto-generated method stub
		
	}
	
	
	
	
}
