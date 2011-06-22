package com.luciddreamingapp.beta.util.state;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.math.stat.descriptive.DescriptiveStatistics;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.os.Handler;
import android.util.Log;

import com.google.gson.Gson;
import com.luciddreamingapp.beta.GlobalApp;
import com.luciddreamingapp.beta.NightGUIActivity;
import com.luciddreamingapp.beta.util.DataManagerObserver;
import com.luciddreamingapp.beta.util.JSONLoader;
import com.luciddreamingapp.beta.util.MorseCodeConverter;
import com.luciddreamingapp.beta.util.SleepAnalyzer;
import com.luciddreamingapp.beta.util.SleepDataManager;
import com.luciddreamingapp.beta.util.SleepDataPoint;

public class SmartTimer implements DataManagerObserver {
	private static final boolean D = false;
	private static final String TAG = "SmartTimer";
	
	private SmartTimerState remConfirmState = new REMConfirmState();
	private SmartTimerState sleepCycleAdjustState = new SleepCycleAdjustState();

	
	SleepDataPoint tempEpoch = null;
	SleepAnalyzer analyzer;
	SleepDataManager dataManager;
	
	//used to filter out repetitive on motion events
	int	lastReminderPlayedEpoch=0, minimumReminderSpacing=1;
	
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
	
	private boolean reminderDelivered = false;
	private boolean remGuessMade = false;
	
	private boolean remGuessStartEnd = false;
	private boolean remGuessStartEndMade = false;
	
	private int shiftDelay = 0;//by how many minutes the SC is shifted from the original start times
	
	private NightGUIActivity parent;
	private Handler parentHandler;//updating views can only be done by the same thread that created them
	private GlobalApp globalApp;
	

	public SmartTimer(String title,String configFilepath){
		if(D)Log.w(TAG, "Created");
		
		analyzer = SleepAnalyzer.getInstance();
		dataManager = SleepDataManager.getInstance();
		setupEvents(configFilepath);
		if(D)
			for(SleepCycleEventVO vo: list){
				Log.i(TAG, vo.toString());
			}
			
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
		if(reminderDelivered){
			//this will be called each minute, make sure subsequent calls will not 
			//get the old value
			reminderDelivered = false;
			return true;
			
		}return false;
	}
	
	/** A workaround method to prevent multiple smart timer interaction invocations
	 * 
	 * @return
	 */
	public boolean guessStartEndREM(){
		if(remGuessStartEnd){
			//this will be called each minute, make sure subsequent calls will not 
			//get the old value
			remGuessStartEnd = false;
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
		
		if(list!=null && index>=list.size()){
			return getListItem(list.size()-1);
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
				state =remConfirmState;
				//state = getListItem(SleepCycleEventIndex).state;
				remStartTime = newEpoch;
				state.epochChanged(newEpoch);
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
			
		//	if(newEpoch ==remStartTime||newEpoch == remStartTime+1){
				if(D)Log.w(TAG, "REM CONFIRM epoch: "+newEpoch+" SC: "+SleepCycleEventIndex+" REM Start time: "
						+remStartTime+" SS Sum: "+sleepScoreSum);
		//	}
			
			
				
				switch(getListItem(SleepCycleEventIndex).deliveryMode){
				case SleepCycleEventVO.REMINDER_DELIVERY_MODE_REM_START:
					if(!remGuessStartEndMade&&getListItem(SleepCycleEventIndex).reminderSet){
						
						remGuessStartEnd = true;
						remGuessStartEndMade = true;
						
						if(D)Log.e(TAG,"START REM INTERACTION at epoch: "+ newEpoch 
								+"  SC start: "+getListItem(SleepCycleEventIndex).startMinute+
								"  SC duration: "+getListItem(SleepCycleEventIndex).duration);
						startInteraction();
					}
					break;
					
					//if a user wants a reminder for this sleep cycle and no guess has been made yet
					//make a guess before leaving the cycle
				case SleepCycleEventVO.REMINDER_DELIVERY_MODE_REM_END:
					if(!remGuessStartEndMade && newEpoch >=remStartTime+getListItem(SleepCycleEventIndex).duration){
						remGuessStartEnd = true;
						remGuessStartEndMade = true;
						if(D)Log.e(TAG,"END REM INTERACTION at epoch: "+ newEpoch 
								+"  start: "+getListItem(SleepCycleEventIndex).startMinute+
								"  duration: "+getListItem(SleepCycleEventIndex).duration);
						startInteraction();//TODO add sleep analyzer filter for spacing
					}
					
					
					break;
				case SleepCycleEventVO.REMINDER_DELIVERY_MODE_MOVEMENT:
					break;
				}
				//change state at the last minute of the duration
//				if(newEpoch >=remStartTime+((getListItem(SleepCycleEventIndex).duration>0)?
//							getListItem(SleepCycleEventIndex).duration-1:
//							getListItem(SleepCycleEventIndex).duration)){
					
				if(D)Log.w(TAG,"REM START: "+remStartTime);
				if(D)Log.w(TAG,"REM DURATION: "+getListItem(SleepCycleEventIndex).duration);
				
					if(newEpoch >=remStartTime+getListItem(SleepCycleEventIndex).duration){
					state = sleepCycleAdjustState;
					if(parentHandler!=null){parentHandler.post(new nonREMRunnable());}
					adjustTime=newEpoch;
					sleepScoreSum = 0;
					statSS.clear();
					sleepScoreCheckPoints=1;
					SleepCycleEventIndex++;
					remGuessStartEndMade = false;
					remGuessStartEnd = false;
					
					//notify the next state of this minute
					//epochChanged(epoch);
					
					//if(D)Log.e(TAG,"ADVANCING SC to index: "+SleepCycleEventIndex+" start: "+ getListItem(SleepCycleEventIndex).startMinute+" "+getListItem(SleepCycleEventIndex).duration);
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
				remGuessStartEndMade = false;
				remGuessStartEnd = false;
				
				//notify the next state of this minute
				//epochChanged(epoch);
				
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
						
						if((tempEpoch.getSleepEpoch()-(lastReminderPlayedEpoch+minimumReminderSpacing))>=0)
						{
							startInteraction();
						}
						
						reminderDelivered=true;
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
			//int delay = getListItem(SleepCycleEventIndex-1).nextEventDelayMinute;
			int delay =0;
			if(SleepCycleEventIndex>0){
				delay =  getListItem(SleepCycleEventIndex).startMinute -
			(getListItem(SleepCycleEventIndex-1).startMinute +
					getListItem(SleepCycleEventIndex-1).duration);
			}
			
			if(D)Log.w(TAG,"EPOCH:"+newEpoch);		
			if(D)Log.w(TAG,"DELAY:"+delay);
			if(D)Log.w(TAG,"ADJUST:"+adjustTime);

			if(D)Log.e(TAG,"SC ADJUST: "+newEpoch+" expectedREM at: "+(adjustTime+delay)+" SS Sum: "+sleepScoreSum+" SS last 12 sum: "+statSS.getSum());
			//reduce spam
//			if(newEpoch ==adjustTime||newEpoch==adjustTime+1){
//			if(D)Log.w(TAG,"Epoch: "+newEpoch+" expectedREM at: "+(adjustTime+delay));
//			}
			
			if(newEpoch >=adjustTime+delay+shiftDelay){
				state = remConfirmState;
				//state = getListItem(SleepCycleEventIndex).state; 
				if(parentHandler!=null){parentHandler.post(new remRunnable());}
				remGuessMade = false;
				remGuessStartEndMade = false;
				sleepScoreSum = 0;
				remStartTime = newEpoch;
				
				//state.epochChanged(epoch);
				
				if(D){Log.e(TAG,epoch+",CHANGING STATE to REM Confirm at: "+epoch);
				//debugLog.appendEntry(epoch+",SleepCycle Adjust,Changing state to REMConfirm");
				}
			}
			
		}

		@Override
		public void userEvent() {
			
			//reset the sleep score sum, as it has to be re-calculated after user events
			sleepScoreSum = 0;
			//int delay = getListItem(SleepCycleEventIndex-1).nextEventDelayMinute;
			
			int delay = getListItem(SleepCycleEventIndex).startMinute -
			(getListItem(SleepCycleEventIndex-1).startMinute +
					getListItem(SleepCycleEventIndex-1).duration);
			
			if(D)Log.w(TAG,"current start "+getListItem(SleepCycleEventIndex).startMinute);
			if(D)Log.w(TAG,"prev start "+getListItem(SleepCycleEventIndex-1).startMinute);
			if(D)Log.w(TAG,"prev Duration "+getListItem(SleepCycleEventIndex-1).duration);
			
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
				remGuessStartEndMade = false;
				sleepScoreSum = 0;
				
				//notify the next state of this minute
				//epochChanged(epoch);
				
				if(D)Log.w(TAG,epoch+",CHANGING STATE to REM Confirm due to movement at : "+epoch);
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
			(getListItem(SleepCycleEventIndex-1).startMinute +
			getListItem(SleepCycleEventIndex-1).duration);
			
						
			if(statSS.getSum()>=12&&(adjustTime+delay)-epoch<13){
				//remGuess=true;//to do or not to do?
				state = getListItem(SleepCycleEventIndex).state; 
				//state = remConfirmState;//advances to the next state
				if(parentHandler!=null){	parentHandler.post(new remRunnable());}
				remGuessMade = false;
				remGuessStartEndMade = false;
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
		SleepCycleEventVO lastVO = getListItem(list.size()-1);
		
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
	
	public JSONObject describeEvents(){
		
		JSONObject temp = new JSONObject();
		
				
		for(SleepCycleEventVO vo :list){
			//	if(D)Log.w(TAG,"Smart Timer VO start: "+vo.startMinute+" duration "+vo.duration);
				
			//fill in the gaps between the events with zeroes
//			if(prev!=null){
//				for(int i =prev.startMinute+prev.duration;i<vo.startMinute;i++){
//					try{temp.accumulate("inactive", (new Tuple(i,0)));}catch(JSONException e){}
//					try{temp.accumulate("active", (new Tuple(i,0)));}catch(JSONException e){}
//					
//				}
//				
//				prev = vo;
//			}else{
//				prev = vo;
//			}
			
				for(int i = vo.startMinute;i<(vo.startMinute+vo.duration);i++){
					//if the event is active
					if(vo.reminderSet){
						//red line for active events
						try{temp.accumulate("active", (new Tuple(i,15)));}catch(JSONException e){}
//						try{temp.accumulate("inactive", (new Tuple(i,0)));}catch(JSONException e){}
					}else{
						//gray line for inactive events
						try{temp.accumulate("inactive", (new Tuple(i,15)));}catch(JSONException e){}
//						try{temp.accumulate("active", (new Tuple(i,0)));}catch(JSONException e){}
					}
				}//end for the duration
				
				

				
				//add fuzzy logic events 
				
				
					
					//todo potentially add reminder text and filepahts to a separate array at the same indices as the markers for
					//these events. Then using on click event for the marker the user would be able to see the corresponding reminder
					
				
			}
		
		//fixing a crazy scary gui bug with reminders showing up at randome events, >separate loop
		for(int i = 0;i<list.size();i++){
			SleepCycleEventVO vo=list.get(i);
			if(D){
				Log.i(TAG,"voStart: "+vo.startMinute);
				Log.i(TAG,"voDuration: "+vo.duration);
				Log.i(TAG,"voDuration: "+vo.deliveryMode);
			}
			
			//add dummy data points to prevent NASTY flot plotting bug!
			try{temp.accumulate("voiceReminder", (new Tuple (0, 19)));}catch(JSONException e){} ;
			try{temp.accumulate("vibrateReminder", (new Tuple (0, 18)));}catch(JSONException e){} ;
			try{temp.accumulate("strobeReminder", (new Tuple (0, 17)));}catch(JSONException e){} ;
			
				switch(vo.deliveryMode){
				
				case SleepCycleEventVO.REMINDER_DELIVERY_MODE_REM_START:
					//add markers at the beginning
					addVOEvents(vo,temp, vo.startMinute);
					break;
				case SleepCycleEventVO.REMINDER_DELIVERY_MODE_REM_END:
					//add markers at the end
					addVOEvents(vo,temp, vo.startMinute+vo.duration);
					break;
				case SleepCycleEventVO.REMINDER_DELIVERY_MODE_MOVEMENT:
					//add markers in the middle
					addVOEvents(vo,temp, vo.startMinute+vo.duration/2);
					break;
					default:
						break;
				
				}//end delivery mode
		}
		
		
		for (int i = 0; i <list.size();i++){
			SleepCycleEventVO voCurrent,voNext;
			try{
			voCurrent = list.get(i);
			voNext = list.get(i+1);
			
			}catch(Exception e){
				//terminate loop when we hit out of bounds
				break;
			}
			//from the current event end, until the next event start, we have this many minutes
			int delay = voNext.startMinute - (voCurrent.startMinute+voCurrent.duration);
			
			
			
			//user event fuzzy logic display
			//from the start +duration/2 till the end +delay/2 
			for(int j =voCurrent.startMinute+voCurrent.duration/2;
			j<(voCurrent.startMinute+voCurrent.duration+delay/2);j++){
				try {
					//if(D)Log.i(TAG,"j: "+j );
					temp.accumulate("fuzzyLogicUserEvent",(new Tuple(j,14)));
				} catch (JSONException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			
			
			//for all but the first event, and if the spacing is at least 12 minutes, add fuzzy logic behavior
			if(i!=0 && delay >12){
				for(int j = voNext.startMinute-12; j<voNext.startMinute;j++){
					try {
						temp.accumulate("fuzzyLogicMotion",(new Tuple(j,14)));
					} catch (JSONException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
			
			
		}
		
		try {
			temp.put("activeLabel", "Active events");
			temp.put("inactiveLabel", "Deactivated events");
			temp.put("fuzzyLogicMotionLabel", "Motion fuzzy logic");
			temp.put("fuzzyLogicUserEventLabel", "User event fuzzy logic");
			
			temp.put("voiceReminderLabel", "Sound Reminder");
			temp.put("vibrateReminderLabel", "Vibration Reminder");			
			temp.put("strobeReminderLabel", "Light Reminder");
			
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
		return temp;
		
	}
	
	
	private void addVOEvents(SleepCycleEventVO vo, JSONObject temp, int minute){
		
		if(D){
			Log.i(TAG,"Minute: "+minute);
			
		}
		
		if(vo.reminderSet){
		//if the reminder is set, filepath is not null, and the file at filepath exists
		if (vo.useVoiceReminder && vo.reminderFilepath!=null ){ 
			if(D)Log.i(TAG,"adding voice event");
			try{temp.accumulate("voiceReminder", (new Tuple (minute, 19)));}catch(JSONException e){} ;}
		
		//if use vibrate reminder, and the string is present, and the vibration time is greater than 0
		if (vo.useVibrateReminder &&vo.vibrateMessage!=null && !vo.vibrateMessage.equals("") &&vo.vibrateDotDuration>0){
			if(D)Log.i(TAG,"adding vibrate event");
			try{temp.accumulate("vibrateReminder", (new Tuple (minute, 18)));}catch(JSONException e){} ;}
		
		//if use strobe, strobe text is present and strobe time is >0
		if (vo.useStrobe &&vo.flashMessage!=null && !vo.flashMessage.equals("")&&vo.flashDotDuration>0){
			if(D)Log.i(TAG,"adding flash event");
			try{temp.accumulate("strobeReminder", (new Tuple (minute, 17)));}catch(JSONException e){} ;}
		}
		
		if(D){
			try{
			Log.e(TAG,"light : "+temp.getJSONArray("voiceReminder").toString());
			Log.e(TAG,"vibrate : "+temp.getJSONArray("vibrateReminder").toString());
			Log.e(TAG,"strobe : "+temp.getJSONArray("strobeReminder").toString());
			
			}catch(JSONException e){
				if(D)e.printStackTrace();
			}
		}
	}
	
	/**
	 * This class allows adding values to JSONObject using accumulate
	 * @author Alex
	 *
	 */
	class Tuple extends JSONArray {
		
		Tuple(int x, int y){
			this.put(x*60000);
			this.put(y);
			
		}
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
			//smart timer will know about this one minute later. By then the state would change!
			
				vo = getListItem(SleepCycleEventIndex);
			
		}catch(Exception e){if(D)e.printStackTrace();return;}
		
		if(D){
			Log.w(TAG,"globalApp: "+globalApp);
			Log.w(TAG,"vo.reminderSet: "+vo.reminderSet);
			Log.w(TAG,"vo use voice: "+vo.useVoiceReminder);
			Log.w(TAG,"vo use vibrate: "+vo.useVibrateReminder);
			Log.w(TAG,"vo use strobe: "+vo.useStrobe);
		}
		
		
		
		if(vo==null|| globalApp==null || !vo.reminderSet){return;}
		
		if(vo.useVoiceReminder &&vo.reminderFilepath!=null){
			if(D)Log.w(TAG,"starting voice Interaction "+vo.reminderFilepath);
			 globalApp.voiceInteractAsync(vo.reminderFilepath);
			//parentHandler.post(new VoiceReminder(vo.reminderFilepath));
			 reminderDelivered = true;//predict REM at the beginning of the rem event
		}
		if(vo.useVibrateReminder && vo.vibrateMessage!=null&& !vo.vibrateMessage.equals("")){
			
			//vibration reminder
			//parentHandler.post(new VibrationReminder());
			long temp = 10;//10 ms minimum
			   if(vo.vibrateDotDuration>1)temp =vo.vibrateDotDuration; 
			   if(D)Log.w(TAG,"starting vibrate Interaction: "+temp+" vibrate message"+vo.vibrateMessage );
			globalApp.vibrateInteractAsync(MorseCodeConverter.pattern(vo.vibrateMessage, temp));
			reminderDelivered = true;//predict REM at the beginning of the rem event
		}
		if(vo.useStrobe &&vo.flashMessage!=null &&!vo.flashMessage.equals("")){
			long temp = 10;
			   if(vo.flashDotDuration>1)temp =vo.flashDotDuration; 
			   if(D)Log.w(TAG,"starting strobe Interaction: "+temp+" strobe message"+vo.flashMessage );
			globalApp.strobeInteractAsync(MorseCodeConverter.pattern(vo.flashMessage, temp));
			reminderDelivered = true;//predict REM at the beginning of the rem event
		}
		
		if(reminderDelivered)
		{
			tempEpoch.setReminderPlayed(true);
			
			lastReminderPlayedEpoch = tempEpoch.getSleepEpoch();
		
			int temp = dataManager.getStatistics().getNumberOfVoiceReminders();
			dataManager.getStatistics().setNumberOfVoiceReminders(temp+1);
		}
		
	}
	
	public void setupEvents(String filepath){
	if(D)Log.i(TAG,"Setting up events from: "+filepath);
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
						//if(D)Log.w(TAG, "Smart Timer VO: "+vo.toString());
						
					}catch(Exception e){}
				}
				//just in case - resort the array in ascending order by startTime
				Arrays.sort(array, new ListEventComparator());
				for(int i = 0;i<array.length;i++){
					list.add(array[i]);
				}
				
				for(SleepCycleEventVO vo :list){
					if(D)Log.v(TAG,vo.toString());
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
		reminderDelivered = false;
		remGuessMade = false;
		 adjustTime = 0;//a reference point indicating when the state changed to adjustState. Makes it easier to make decisions
		 remStartTime = 0;
		
		 sleepScoreSum = 0;//keeps track of user activity within each event
		sleepScoreCheckPoints=1;//interesting points of increased activity within REM cycle
		sleepScoreThreshold = 10;
		shiftDelay=0;
		epoch = 0;
	}

	@Override
	public void dataPointAdded(SleepDataPoint epoch) {
		// TODO Auto-generated method stub
		
		if(D)Log.e(TAG, "dataPointAdded called");
		//respond to new events and epoch changes
		epochChanged(epoch.getSleepEpoch());
		if(epoch.getUserEvent()!=epoch.NO_EVENT){
			userEvent();
		}
		
		tempEpoch= epoch;
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


