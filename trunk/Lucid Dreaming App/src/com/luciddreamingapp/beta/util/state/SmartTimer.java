package com.luciddreamingapp.beta.util.state;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.math.stat.descriptive.DescriptiveStatistics;
import org.json.JSONArray;
import org.json.JSONObject;

import android.os.Handler;
import android.util.Log;

import com.google.gson.Gson;
import com.luciddreamingapp.beta.GlobalApp;
import com.luciddreamingapp.beta.NightGUIActivity;
import com.luciddreamingapp.beta.util.JSONLoader;
import com.luciddreamingapp.beta.util.MorseCodeConverter;

public class SmartTimer {
	private static final boolean D = false;
	private static final String TAG = "SmartTimer";
	
	private SmartTimerState remConfirmState = new REMConfirmState();
	private SmartTimerState sleepCycleAdjustState = new SleepCycleAdjustState();

	
	private String configFilepath;
	//private LogManager debugLog;
	
	private int SleepCycleEventIndex = 0;
	
	private List<SleepCycleEventVO> list;
	SmartTimerState state;
	
	private int epoch;
	private int adjustTime = 0;//a reference point indicating when the state changed to adjustState. Makes it easier to make decisions
	private int remStartTime = 0;
	
	private double sleepScoreSum = 0;//keeps track of user activity within each event
	private int sleepScoreCheckPoints=1;//interesting points of increased activity within REM cycle
	private int sleepScoreThreshold = 10;//capture checkpoints for every X points increase in sleep cycle
	
	private DescriptiveStatistics statSS;//sleep score statistics
	
	private boolean remGuess = false;
	private boolean remGuessMade = false;
	
	private int shiftDelay = 0;//by how many minutes the SC is shifted from the original start times
	
	private NightGUIActivity parent;
	private Handler parentHandler;//updating views can only be done by the same thread that created them
	private GlobalApp globalApp;
	

	public SmartTimer(String title,String configFilepath){
		if(D)Log.w(TAG, "Created");
		
		setupEvents(configFilepath);
		
		//debugLog= new LogManager(LucidDreamingApp.LOG_LOCATION,"smartTimerDebug.txt","epoch,state,event");
		//debugLog.appendEntry(title);
		epoch = 0;
		//start out in the waiting state
		state = new WaitingState();
		statSS= new DescriptiveStatistics(12);//rolling activity statistics for the past 12 minutes
		//1, 90, 10, 68
    	//2, 168,24,30
    	//2.5, 220, 10, 30
    	//3, 260, 40, 40
    	//4, 340, 40, 40
    	//5, 420, 40, 40

	}
	
	public boolean guessREM(){
		if(remGuess){
			//this will be called each minute, make sure subsequent calls will not 
			//get the old value
			remGuess = false;
			return true;
			
		}return false;
	}
	
	public void epochChanged(int newEpoch){
		this.epoch = newEpoch;
		state.epochChanged(newEpoch);
	}
	
	public void userEvent(){
		state.userEvent();
	}
	
	/**Tell the smart timer the sleep score for the given epoch, let it work its magic
	 * must be called before the epoch changed
	 * 
	 * @param sleepScore sleep score for the provided epoch
	 */
	public void updateSleepScore(double sleepScore){
		state.updateActivity(sleepScore);
	}
	/**Update smart timer with the information about the user being asleep
	 * 
	 * @param userAsleep indicates if the user was asleep during the given epoch
	 */
	public void updateUserAsleep(boolean userAsleep){
		state.updateUserAsleep(userAsleep);
	}
	
	
	private SleepCycleEventVO getListItem(int index){
		
		if(list!=null && index>list.size()){
			return list.get(list.size()-1);
		}else{
			return list.get(index);
		}
		
	}
	
	
	class WaitingState implements SmartTimerState{

		@Override
		public void epochChanged(int newEpoch) {
			if(D)Log.w(TAG, "Waiting state: epoch:"+newEpoch +" next start: "+getListItem(SleepCycleEventIndex).startMinute);
			//simply waits to change the state to the state indicated by the sleepcyclevo
			if(newEpoch >=getListItem(SleepCycleEventIndex).startMinute){
				if(parentHandler!=null){parentHandler.post(new remRunnable());}
				state = getListItem(SleepCycleEventIndex).state;
				remStartTime = newEpoch;
				//debugLog.appendEntry(epoch+",Waiting,changing state");
		//	SleepCycleEventIndex++;
			}else{
			//	debugLog.appendEntry(epoch+",Waiting,##");
			}
		}

		@Override
		public void userEvent() {
			//debugLog.appendEntry(epoch+",Waiting,user event");
			
			
		}
		@Override
		public void updateActivity(double sleepScore){
			//sleepScoreSum+=sleepScore;
		}
		@Override
		public void updateUserAsleep(boolean userAsleep){}
		
	}
	
	class REMConfirmState implements SmartTimerState{

		@Override
		public void epochChanged(int newEpoch) {
			if(D)Log.w(TAG, "SmartTimer, REMConfrim epoch: "+epoch+" SC: "+SleepCycleEventIndex+" REM Start time: "+remStartTime+" SS Sum: "+sleepScoreSum);
			if(newEpoch ==remStartTime||newEpoch == remStartTime+1){
				if(D)Log.w(TAG, "SmartTimer, REMConfrim epoch: "+epoch+" SC: "+SleepCycleEventIndex+" REM Start time: "+remStartTime+" SS Sum: "+sleepScoreSum);
			}
			
			
				
				switch(getListItem(SleepCycleEventIndex).deliveryMode){
				case SleepCycleEventVO.REMINDER_DELIVERY_MODE_REM_START:
					if(!remGuessMade&&getListItem(SleepCycleEventIndex).reminderSet){
						
						remGuessMade = true;
						if(D)Log.w(TAG,"calling start interaction at the start of REM: "+ epoch 
								+"  start: "+getListItem(SleepCycleEventIndex).startMinute+
								"  duration: "+getListItem(SleepCycleEventIndex).duration);
						startInteraction();
					}
					break;
					
					//if a user wants a reminder for this sleep cycle and no guess has been made yet
					//make a guess before leaving the cycle
				case SleepCycleEventVO.REMINDER_DELIVERY_MODE_REM_END:
					if(newEpoch >=remStartTime+getListItem(SleepCycleEventIndex).duration){
						remGuess = true;
						remGuessMade = true;
						if(D)Log.w(TAG,"calling start interaction at the end of REM: "+ epoch 
								+"  start: "+getListItem(SleepCycleEventIndex).startMinute+
								"  duration: "+getListItem(SleepCycleEventIndex).duration);
						startInteraction();//TODO add sleep analyzer filter for spacing
					}
					
					
					break;
				case SleepCycleEventVO.REMINDER_DELIVERY_MODE_MOVEMENT:
					break;
				}
				
				if(newEpoch >=remStartTime+getListItem(SleepCycleEventIndex).duration){
					state = sleepCycleAdjustState;
					if(parentHandler!=null){parentHandler.post(new nonREMRunnable());}
					adjustTime=newEpoch;
					sleepScoreSum = 0;
					statSS.clear();
					sleepScoreCheckPoints=1;
					SleepCycleEventIndex++;
					if(D)Log.w(TAG,"SS: "+SleepCycleEventIndex+" start: "+ getListItem(SleepCycleEventIndex).startMinute+" "+getListItem(SleepCycleEventIndex).duration);
					//debugLog.appendEntry(epoch+",REM Confirm,changing state to Adjust state: "+SleepCycleEventIndex);
				}
			
			
	
			
			// TODO Auto-generated method stub
			
		}

		@Override
		public void userEvent() {
			
			
			if(D)Log.w(TAG, "SmartTimer, REMConfrim user event at : "+epoch);
			//if a user event occurs in the 2nd part of the duration, we consider the REM state to be over, and start adjusting for the next state
			if(epoch-remStartTime>=getListItem(SleepCycleEventIndex).duration/2){
				// TODO wait or adjust?
				state = sleepCycleAdjustState;
				if(parentHandler!=null){parentHandler.post(new nonREMRunnable());}
				adjustTime = epoch;
				sleepScoreSum = 0;
				statSS.clear();
				sleepScoreCheckPoints=1;
				SleepCycleEventIndex++;
				if(D)Log.w(TAG,"SS: "+SleepCycleEventIndex+" start: "+ getListItem(SleepCycleEventIndex).startMinute+" "+getListItem(SleepCycleEventIndex).duration);
				//debugLog.appendEntry(epoch+",REM Confirm,User event after duration/2, finishing REM ");
			}
			
			
		}
		
		@Override
		public void updateActivity(double sleepScore){
				//only do this if we want a reminder on movement
				if( getListItem(SleepCycleEventIndex).deliveryMode ==SleepCycleEventVO.REMINDER_DELIVERY_MODE_MOVEMENT){
					//keep track of user activity through sleep score
					if(sleepScore<4){
					sleepScoreSum+=sleepScore;
					//if sleep score increases by more than the 
					if(sleepScoreSum>sleepScoreThreshold*sleepScoreCheckPoints &&getListItem(SleepCycleEventIndex).reminderSet){
						remGuess=true;
						remGuessMade = true;
						
						if(D)Log.w(TAG,"SleepCycleCheckpoint reached "+sleepScoreCheckPoints);
						sleepScoreCheckPoints++;
					}
					}else{sleepScoreSum+=0;}//avoid NaN 
				}
			}
			
			
		@Override
		public void updateUserAsleep(boolean userAsleep){}
		
		
	}
	
	class wbtbState implements SmartTimerState{
		boolean reminderDelivered = false;
		
		@Override
		public void epochChanged(int newEpoch) {
		//play alarm if the event is active and the reminder has not been responded to yet
			if(getListItem(SleepCycleEventIndex).reminderSet && !reminderDelivered){
				//if(parentHandler!=null){parentHandler.post(new VibrationReminder());}
			}
			//proceed with next event at the end of this one (will be less accurate)
			if(newEpoch >=remStartTime+getListItem(SleepCycleEventIndex).duration){
				state = sleepCycleAdjustState;
				if(parentHandler!=null){parentHandler.post(new nonREMRunnable());}
				adjustTime=newEpoch;
				sleepScoreSum = 0;
				statSS.clear();
				sleepScoreCheckPoints=1;
				SleepCycleEventIndex++;
						
				
				if(D)Log.w(TAG,"SS: "+SleepCycleEventIndex+" start: "+ getListItem(SleepCycleEventIndex).startMinute+" "+getListItem(SleepCycleEventIndex).duration);
				//debugLog.appendEntry(epoch+",REM Confirm,changing state to Adjust state: "+SleepCycleEventIndex);
			}
			
			
			
		}
		
		@Override
		public void userEvent() {
			reminderDelivered= true;
		//stop playback
		}
		
		@Override
		public void updateActivity(double sleepScore){
			if(sleepScore>4){reminderDelivered= true;}
		//stop if greater than 1
		
		}

		@Override
		public void updateUserAsleep(boolean userAsleep) {
			if(!userAsleep){
				reminderDelivered = true;
			}
			// stop playback
			
		}
	}
	
	class SleepCycleAdjustState implements SmartTimerState{

		@Override
		public void epochChanged(int newEpoch) {
			//if(D)Log.w(TAG, "SmartTimer, SSAdjust State Epoch:"+newEpoch);
			//if it's time for the new REM cycle, make it so
			//previous cycle REM has ended. Get the delay period after that REM, the time when the REM ended
			//and add any shifting constants accumulated by the SC adjust method
			
			//find out how long to wait after the previos REM cycle
			//int delay = list.get(SleepCycleEventIndex-1).nextEventDelayMinute;
			int delay = getListItem(SleepCycleEventIndex).startMinute -
			(list.get(SleepCycleEventIndex-1).startMinute +
			list.get(SleepCycleEventIndex-1).duration);


			if(D)Log.w(TAG,"Epoch: "+newEpoch+" expectedREM at: "+(adjustTime+delay)+" SS Sum: "+sleepScoreSum+" SS last 12 sum: "+statSS.getSum());
			//reduce spam
//			if(newEpoch ==adjustTime||newEpoch==adjustTime+1){
//			if(D)Log.w(TAG,"Epoch: "+newEpoch+" expectedREM at: "+(adjustTime+delay));
//			}
			
			if(newEpoch >=adjustTime+delay+shiftDelay){
				state = getListItem(SleepCycleEventIndex).state; 
				if(parentHandler!=null){parentHandler.post(new remRunnable());}
				remGuessMade = false;
				sleepScoreSum = 0;
				remStartTime = newEpoch;
				if(D){Log.w(TAG,epoch+",SleepCycle Adjust,Changing state to REMConfirm");
				//debugLog.appendEntry(epoch+",SleepCycle Adjust,Changing state to REMConfirm");
				}
			}
			
		}

		@Override
		public void userEvent() {
			
			//reset the sleep score sum, as it has to be re-calculated after user events
			sleepScoreSum = 0;
			//int delay = list.get(SleepCycleEventIndex-1).nextEventDelayMinute;
			
			int delay = getListItem(SleepCycleEventIndex).startMinute -
			(list.get(SleepCycleEventIndex-1).startMinute +
			list.get(SleepCycleEventIndex-1).duration);
			
			if(D)Log.w(TAG,"current start "+getListItem(SleepCycleEventIndex).startMinute);
			if(D)Log.w(TAG,"prev start "+list.get(SleepCycleEventIndex-1).startMinute);
			if(D)Log.w(TAG,"prev Duration "+list.get(SleepCycleEventIndex-1).duration);
			
			if(D)Log.w(TAG, "SmartTimer, SC Adjust state User Event at:"+epoch+" delay: "+delay+ " ep -adj: "+(epoch - adjustTime));
			if(epoch - adjustTime<delay*0.5){
				adjustTime = epoch;//reset the adjust time, making future calls think that REM just ended
				if(D)Log.w(TAG,epoch+",SleepCycle Adjust,resetting SS boundary due to user event close to boundary");
				//debugLog.appendEntry(epoch+",SleepCycle Adjust,resetting SS boundary due to user event close to boundary");
				
			}else if(epoch-adjustTime>=delay*0.75){
				state = getListItem(SleepCycleEventIndex).state; 
				//state = remConfirmState;//advances to the next state
				if(parentHandler!=null){parentHandler.post(new remRunnable());}
				remStartTime = epoch;
				remGuessMade = false;
				sleepScoreSum = 0;
				if(D)Log.w(TAG,epoch+",SleepCycle Adjust,Fast forwarding to next REM cycle");
				//debugLog.appendEntry(epoch+",SleepCycle Adjust,Fast forwarding to next REM cycle");
			}else{
				//do nothing for 0.5 to 0.75
			}
			//get next REM start time
			//get prev REM next event time
			//look at how much time has passed since the last REM event
			
			// TODO Auto-generated method stub
			
		}
		@Override
		public void updateActivity(double sleepScore){
			//fast forward the next SC onset if high activity within the 12 minute window of the REM start.
			if(sleepScore<4){ // attempt to filter out purposeful activity
			sleepScoreSum+=sleepScore;
			statSS.addValue(sleepScore);
			int delay = getListItem(SleepCycleEventIndex).startMinute -
			(list.get(SleepCycleEventIndex-1).startMinute +
			list.get(SleepCycleEventIndex-1).duration);
			
			if(statSS.getSum()>=12&&(adjustTime+delay)-epoch<13){
				//remGuess=true;//to do or not to do?
				state = getListItem(SleepCycleEventIndex).state; 
				//state = remConfirmState;//advances to the next state
				if(parentHandler!=null){	parentHandler.post(new remRunnable());}
				remGuessMade = false;
				remStartTime = epoch;
				sleepScoreSum = 0;
				statSS.clear();
				if(D)Log.w(TAG,epoch+"High activity detected, reminder + fast forward to next REM cycle");
			}
			}else{
				statSS.addValue(0);
			}
			
		}
		@Override
		public void updateUserAsleep(boolean userAsleep){}
		
	}

	public int getSleepScoreThreshold() {
		return sleepScoreThreshold;
	}

	public void setSleepScoreThreshold(int sleepScoreThreshold) {
		this.sleepScoreThreshold = sleepScoreThreshold;
	}
	
	
	
	public int[] describeREMPeriods(){
		
		
		try{
		int[] remPeaks;
		SleepCycleEventVO lastVO = list.get(list.size()-1);
		
		int arrayLength = lastVO.startMinute+lastVO.duration;
		remPeaks = new int[arrayLength+1];
		
		Arrays.fill(remPeaks, 0);
		//create an array where each high value corresponds to a minute when REM is expected
		if(D)Log.w(TAG,"Smart Timer List size: "+arrayLength);
		for(SleepCycleEventVO vo :list){
		//	if(D)Log.w(TAG,"Smart Timer VO start: "+vo.startMinute+" duration "+vo.duration);
			
			for(int i = vo.startMinute;i<(vo.startMinute+vo.duration);i++){
				
				remPeaks[i] = 30;
				
			}
		}
		
		return remPeaks;
		
		}catch(Exception e){if(D)e.printStackTrace();
		return null;}
		
	}

	public NightGUIActivity getParent() {
		return parent;
	}

	public void setParent(NightGUIActivity parent) {
		this.parent = parent;
	}

	public Handler getParentHandler() {
		return parentHandler;
	}

	public void setParentHandler(Handler parentHandler) {
		this.parentHandler = parentHandler;
	}
	
	//changes the clock color to blue
	 class remRunnable implements Runnable{
	 		@Override
			public void run() {
	 			
	 			if(globalApp!=null && globalApp.getGuiActivity()!=null)
	 			globalApp.getGuiActivity().notifyREM(true);}		
	    	}
	 //changes the clock color to normal color
	 class nonREMRunnable implements Runnable{
	 		@Override
			public void run() {
	 		
	 			if(globalApp!=null && globalApp.getGuiActivity()!=null)
	 			globalApp.getGuiActivity().notifyREM(false);}		
	 		
	    	}
	 
	 class VibrationReminder implements Runnable{
		 final long[] timing;
		 
		 VibrationReminder(long[] timing){
			 this.timing = timing;
		 }
	 		@Override
			public void run() {
	 			//readyToProcess = false;
	 			//globalApp.getGuiActivity().startVibrateInteraction(getListItem(SleepCycleEventIndex).vibrateMessage);
	 			globalApp.vibrateInteractAsync(timing);
	 			}		
	    	}

	 
	 class VoiceReminder implements Runnable{
	 	final String filepath;
	 	
		 public VoiceReminder(String filepath){
			 this.filepath =filepath; //can only assign final variable once
		 }
		 @Override
			public void run() {
	 			//readyToProcess = false;
			 globalApp.voiceInteractAsync(filepath);}		
	   }
	 
	 class StrobeReminder implements Runnable{
		 	final String filepath;
		 	
			 public StrobeReminder(String filepath){
				 this.filepath =filepath; //can only assign final variable once
			 }
			 @Override
				public void run() {
		 			//readyToProcess = false;
				 globalApp.voiceInteractAsync(filepath);
				 
			 }		
		   }
	 
	
	public void startInteraction(){
		
		if(D)Log.w(TAG,"startInteraction called for timer:"+SleepCycleEventIndex);
		
		SleepCycleEventVO vo =null;
		try{
		vo = getListItem(SleepCycleEventIndex);
		}catch(Exception e){if(D)e.printStackTrace();return;}
		
		if(D){
			Log.w(TAG,"globalApp: "+globalApp);
			Log.w(TAG,"vo.reminderSet: "+vo.reminderSet);
			Log.w(TAG,"vo use voice: "+vo.useVoiceReminder);
			Log.w(TAG,"vo use vibrate: "+vo.useVibrateReminder);
			Log.w(TAG,"vo use strobe: "+vo.useStrobe);
		}
		
		if(globalApp!=null){
			parent = globalApp.getGuiActivity();
			//parentHandler = globalApp.getGuiActivity().getHandler();
		}
		
		if(vo==null|| globalApp==null || !vo.reminderSet){return;}
		
		if(vo.useVoiceReminder &&vo.reminderFilepath!=null){
			if(D)Log.w(TAG,"starting voice Interaction "+vo.reminderFilepath);
			 globalApp.voiceInteractAsync(vo.reminderFilepath);
			//parentHandler.post(new VoiceReminder(vo.reminderFilepath));
			 remGuess = true;//predict REM at the beginning of the rem event
		}
		if(vo.useVibrateReminder && vo.vibrateMessage!=null&& !vo.vibrateMessage.equals("")){
			
			//vibration reminder
			//parentHandler.post(new VibrationReminder());
			long temp = 10;//10 ms minimum
			   if(vo.vibrateDotDuration>1)temp =vo.vibrateDotDuration; 
			   if(D)Log.w(TAG,"starting vibrate Interaction: "+temp+" vibrate message"+vo.vibrateMessage );
			globalApp.vibrateInteractAsync(MorseCodeConverter.pattern(vo.vibrateMessage, temp));
			remGuess = true;//predict REM at the beginning of the rem event
		}
		if(vo.useStrobe &&vo.flashMessage!=null &&!vo.flashMessage.equals("")){
			long temp = 10;
			   if(vo.flashDotDuration>1)temp =vo.flashDotDuration; 
			   if(D)Log.w(TAG,"starting strobe Interaction: "+temp+" strobe message"+vo.flashMessage );
			globalApp.strobeInteractAsync(MorseCodeConverter.pattern(vo.flashMessage, temp));
			remGuess = true;//predict REM at the beginning of the rem event
		}
		
	}
	
	public void setupEvents(String filepath){
	
		//pull up the config file and initialize objects from GSON
		//classes do not seem to be saved, so assign the state manually
		 try{
			 	list = new ArrayList<SleepCycleEventVO>(15);
				File file =new File(filepath);
				JSONObject json =JSONLoader.getData(file);
				
				Gson gson = new Gson();
				JSONArray eventObjects =json.getJSONArray("eventObjects");
				SleepCycleEventVO[] array = new SleepCycleEventVO[eventObjects.length()];
				for(int i =0;i<eventObjects.length();i++){
					try{
						//TODO Remove after testing. Makes 3rd event a WBTB
						
						SleepCycleEventVO vo =gson.fromJson(eventObjects.getString(i), SleepCycleEventVO.class) ;
						vo.state = remConfirmState;
						array[i]=vo;
						if(D)Log.w(TAG, "Smart Timer VO: "+vo.toString());
						
					}catch(Exception e){}
				}
				//just in case - resort the array in ascending order by startTime
				Arrays.sort(array, new ListEventComparator());
				for(int i = 0;i<array.length;i++){
					list.add(array[i]);
				}
				 }catch(Exception e){
					 if(D)e.printStackTrace();
				 }
	}

	public GlobalApp getGlobalApp() {
		return globalApp;
	}

	public void setGlobalApp(GlobalApp globalApp) {
		this.globalApp = globalApp;
	}
	
	public void reset(){
		this.SleepCycleEventIndex=0;
		remGuess = false;
		remGuessMade = false;
		 adjustTime = 0;//a reference point indicating when the state changed to adjustState. Makes it easier to make decisions
		 remStartTime = 0;
		
		 sleepScoreSum = 0;//keeps track of user activity within each event
		sleepScoreCheckPoints=1;//interesting points of increased activity within REM cycle
		sleepScoreThreshold = 10;
		shiftDelay=0;
		epoch = 0;
	}
	
	
}


