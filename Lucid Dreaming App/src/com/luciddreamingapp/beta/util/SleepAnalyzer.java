package com.luciddreamingapp.beta.util;

import java.util.Calendar;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import com.luciddreamingapp.beta.Interactive;
import com.luciddreamingapp.beta.util.state.SmartTimer;

public class SleepAnalyzer {
	 private static final String TAG = "SleepAnalyzer";
	 private static final boolean D = false;

	private int totalMinutesAsleep = 0;
	private int prevLongestSleepEpisode=0; 
	
	private int activityThreshold;
	
	private SmartTimer smartTimer;
	private StringBuilder sb;
	
	public static float coleConstant = 0.012986566f;

	private int maxActivityCount = 1; //max activity count before the user is considered awake.
	private int progressBarActivityCount = 1; 
	boolean soundPlayed = false;
	
	private int countdown = 8;
	private boolean sleepScoringStarted = false;
	
	private static Context appContext=null; 
	private static boolean playLateReminders=false;
	private static boolean playEarlyReminders = true;
	
	private int earliest120=30;
	private int latest120=45;
	private int earliest180=10;
	private int latest180=15;
	private int earliest120to180=20;
	private int latest120to180=30;
	
	private int remindersToPlay = 1;
	private int remindersToPlayCopy=remindersToPlay;
	
	private boolean enableREMPrediction = false;
	private int minimumReminderSpacing = 15;
	private int lastReminderPlayedEpoch = 0;
	private int remPredictionActivityThreshold = 10;
	
	SleepDataManager dataManager;
	
	//private ArrayList<SleepDataPoint> sleepData;
	
	private static SleepAnalyzer instance= null;
	
	public static synchronized SleepAnalyzer getInstance(){
		if(instance == null){
			instance = new SleepAnalyzer();
			return instance;
		}else return instance;
	}
	
	private SleepAnalyzer(){
		
		//smartTimer = new SmartTimer(Calendar.getInstance().getTime().toLocaleString());
		
		dataManager = SleepDataManager.getInstance();
		//Log.e(TAG,"SA Data Manager"+dataManager.hashCode());
		
	}
	
	public  void addSleepDataPoint(SleepDataPoint epoch){
		dataManager.addSleepDataPoint(epoch);
	}
	
	/**Calculates sleep score for the given epoch, then performs REM analysis * 
	 * @param epoch
	 */
	public void analyze(int epoch){
		
		try{
			//Log.e(TAG,dataManager.getSleepDataString(epoch));
		if(dataManager.size()<8){
			if(D) Log.e(TAG, "Data manager size: "+dataManager.size());
			
			dataManager.setSleepStatus("Sleep scoring begins in "+(countdown--)+" minutes.<br/>");			
			dataManager.setSleepStatus(dataManager.getSleepDataPoint(epoch+2).toHTMLTableString()+"<br/>");
			
			
			if(epoch>=0){
			//smartTimer.epochChanged(epoch);
			dataManager.logEpoch(dataManager.getSleepDataPoint(epoch));
			}
			
		
			
			
		}else{
			if(D) Log.e(TAG, "Processing epoch: "+epoch);
			if(!sleepScoringStarted){
				sleepScoringStarted=true;
				
				dataManager.setSleepStatus("Sleep Scoring Started <br/><HR WIDTH=80%>");
				
			}
			
		if(D){Log.w("SleepAnalyzer", "Analyzing Epoch:"+epoch);}
		sb = new StringBuilder();
		sb.append("+++Analyzing epoch: "+epoch);
		
		//excel formula
	//	=(2.3*H8+(H4*1.06+H5*0.54+H6*0.58+H7*0.76)+(H9*0.74+H10*0.67))*coleConst
	//		coleConst = 0.001328
		//cole asleep when result <1
		
			
		//Point to analyze a(t)//can only be called from the future
		SleepDataPoint a0 = dataManager.getSleepDataPoint(epoch); 
		//if the value is constant
		a0.setColeConstant(coleConstant);		

		
		//2 epochs in the future (read a(t+1)
		SleepDataPoint a1=null,a2 = null;
		//4 epochs in the past (read a(t-1))
		
		try{a1 =dataManager.getSleepDataPoint(epoch+1);
			a2 =dataManager.getSleepDataPoint(epoch+2);}catch(Exception e){
			sb.append("Unable to score epoch: "+epoch);
			if(D){Log.e(TAG, "Unable to retrieve a1, a2: "+epoch);}
			if(D) e.printStackTrace();
			return;
		}
		
		SleepDataPoint an1=null,an2=null,an3=null,an4=null,an5=null;
		try{
			an1 = dataManager.getSleepDataPoint(epoch-1);
			an2 = dataManager.getSleepDataPoint(epoch-2);
			an3 = dataManager.getSleepDataPoint(epoch-3);
			an4 = dataManager.getSleepDataPoint(epoch-4);
			an5 = dataManager.getSleepDataPoint(epoch-5);
		}catch(Exception e){
			//TODO determine if we have to cancel if this happens
		}
		
		//calculates cole sleep score
		float coleSleepScore =0;
		
		coleSleepScore = 
			coleConstant*(2.3F*a0.activityCount+
					(1.06F*an4.activityCount+0.54F*an3.activityCount+
							0.58F*an2.activityCount+0.76F*an1.activityCount)+
							(0.74F*a1.activityCount+0.67F*a2.activityCount));
		
		progressBarActivityCount = (int)(2.3F*a1.activityCount+
				(1.06F*an3.activityCount+0.54F*an2.activityCount+
						0.58F*an1.activityCount+0.76F*a0.activityCount)+
						(0.74F*a2.activityCount));
		
		if(D){Log.w("SleepAnalyzer", "Epoch:"+epoch+" Sleep Score: "+coleSleepScore);}
		sb.append(" Sleep Score: "+coleSleepScore);
		
		//remember the sleep score
		a0.setColeSleepScore(coleSleepScore);
		
		
		
		//According to Cole's algorithm, a person 
		if(a0.coleSleepScore<1 && an1.coleSleepScore<1 && an2.coleSleepScore<1&&an3.coleSleepScore<1 
				&&an4.coleSleepScore<1&&an5.coleSleepScore<1){
			a0.coleAsleep = true;
		if(D){Log.w("SleepAnalyzer", "User is asleep: ");}
			sb.append("User is asleep! ");
			//check for contiguous sleep window
			if(an1.sleepEpisodeMinute>0){
					
					int tempSleepEpisodeDuration = an1.sleepEpisodeMinute+1;
				//it is a part of a continuous sleep episode
					a0.sleepEpisodeMinute = tempSleepEpisodeDuration;
					//check if this is the longest sleep episode yet
					if(prevLongestSleepEpisode<tempSleepEpisodeDuration){
						//remember it and update statistics
						prevLongestSleepEpisode=tempSleepEpisodeDuration;
						dataManager.getStatistics().setLongestSleepEpisode(tempSleepEpisodeDuration);
					}
					
				}
			else{
					a0.sleepEpisodeMinute=1;//otherwise, this is the first minute of a sleep episode
					totalMinutesAsleep= dataManager.getStatistics().getTotalMinutesAsleep();
				//if this is the very first minute of sleep, record it as sleep onset latency
					if(totalMinutesAsleep == 0){				
						dataManager.getStatistics().setSleepOnsetLatency(dataManager.size());
					}
			
			}
			
			//increment total sleep duration
				totalMinutesAsleep= dataManager.getStatistics().getTotalMinutesAsleep();
				dataManager.getStatistics().setTotalMinutesAsleep(totalMinutesAsleep+1);
												
				//a0.totalMinutesAsleep = totalMinutesAsleep; 
				if(D){Log.w("SleepAnalyzer", "Current sleep episode: "+a0.sleepEpisodeMinute);}
				sb.append("Current sleep episode: "+a0.sleepEpisodeMinute);
				if(D){Log.w("SleepAnalyzer", "Total sleep episode: "+a0.totalMinutesAsleep);
				if(D)Log.w(TAG, "Total sleep duration: "+a0.totalMinutesAsleep);}
		}else{
			//User is not asleep - update sleep time, 
			a0.coleAsleep = false;
			sb.append("User is not asleep; ");					
			
			//check if this is an awakening
			//from a previous sleep episode
			if(an1.coleAsleep){
				int temp = dataManager.getStatistics().getNumberOfAwakenings();
				dataManager.getStatistics().setNumberOfAwakenings(temp+1);
				
			}
		}
		
		//remember the total minutes asleep
		a0.totalMinutesAsleep = totalMinutesAsleep;
		a1.totalMinutesAsleep = totalMinutesAsleep;//remember this to avoid graph drop off at
		a2.totalMinutesAsleep = totalMinutesAsleep;//the last 2 minutes
		
		
		if(enableREMPrediction){
		//TODO make permanent or remove after testing
//		smartTimer.updateSleepScore(a0.coleSleepScore);
//		smartTimer.updateUserAsleep(a0.coleAsleep);
//		
//		//if on movement and adequate spacing or at the end or start of the event
//		if((smartTimer.guessREM()&&a0.sleepEpoch-(lastReminderPlayedEpoch+minimumReminderSpacing)>=0)
//				||(smartTimer.guessStartEndREM())
//				){
//			lastReminderPlayedEpoch = a0.sleepEpoch;
//			a0.reminderPlayed =true;
//			int temp = dataManager.getStatistics().getNumberOfVoiceReminders();
//			dataManager.getStatistics().setNumberOfVoiceReminders(temp+1);
//			smartTimer.startInteraction();
//		}
		
//		smartTimer.epochChanged(a0.sleepEpoch);
		}else{
		
		//predict the future. If the activity count is large enough to cause an awakening
		//(2.3*Y$3+(Y$3*1.06+Y$3*0.54+Y$3*0.58+Y$3*0.76)+(Y$3*0.74+Y$3*0.67)
//		if(1 < coleConstant*(a2.activityCount*2.3+a1.activityCount*0.76+a0.activityCount*0.58+an1.activityCount*0.54+
//				an2.activityCount*1.06))
//				{
//					//detect REM, predicting awakening
//					if(detectREM(a0,false)||detectLateREM(a0)){
//					
//					//if the reminder has not been played for this episode
//					parent.startInteraction();
//					
//					//remember that we played back the reminder to avoid duplicates
//						a2.reminderPlayed = true;
//						//remember this playback
//						int temp = dataManager.getStatistics().getNumberOfVoiceReminders();
//						dataManager.getStatistics().setNumberOfVoiceReminders(temp+1);
//						
//						//sb.append( "Starting sound playback:");
//				
//					}else{a0.reminderPlayed= false;}
//			
			
//		}
//		else{
			
			//if the user is still asleep during a0, and the reminder was not played due to "future foresight"
			//or if it is a time to play the reminder.
			if((detectREM(a0,a0.coleAsleep) &&(!a0.reminderPlayed || !a1.reminderPlayed))||playLateReminder(a0) ){
				
			//if the reminder has not been played for this episode
			
				//controls the sound playback when REM is detected
				a0.reminderPlayed = true;
				//remember this playback
				int temp = dataManager.getStatistics().getNumberOfVoiceReminders();
				dataManager.getStatistics().setNumberOfVoiceReminders(temp+1);
				
				
			
			}else{a0.reminderPlayed= false;}
			
//		}
			
		}//end if rem prediction not enabled
			try{
			//add the new calibrated value to the list
				
				dataManager.setSleepStatus(a0.toHTMLTableString()+"<br>");
				
			}catch(Exception e){if(D)e.printStackTrace();}
			//remember this data point
			
			dataManager.logEpoch(a0); //add the sleep epoch to data
			dataManager.updateEpochGraph(a0);//update graph with the value of sleep epoch.
		
			//ask data manager to notify observers
			dataManager.dataPointUpdated(a0);
			if(D)Log.w(TAG,sb.toString());
			sb = null;
	
		
		
		
		}//end else
		}catch(Exception e){
			if(D){Log.e(TAG, "Exception trying to analyze: "+epoch);}
			if(D) e.printStackTrace();
		}
		
	}//end analyze()
	
	
	public boolean playLateReminder(SleepDataPoint epoch){
		
		//latest interruption is based on the current epoch - just look at how long
		//the user has been asleep
		if(playLateReminders){
		
		if(epoch.totalMinutesAsleep<120 && epoch.sleepEpisodeMinute>=latest120){
			if(D){Log.w("SleepAnalyzer", "Trying to notify user1");}
//			sb.append("Trying to notify user1");
			if(remindersToPlay>0){
				remindersToPlay--;
				return true;
			}else return false;
			
		}else if(epoch.totalMinutesAsleep>=120 && epoch.sleepEpisodeMinute>=latest120to180){
			if(D){Log.w("SleepAnalyzer", "Trying to notify user2");
//			sb.append("Trying to notify user2");
			}
			if(remindersToPlay>0){
				remindersToPlay--;
				return true;
			}else return false;
		}else if(epoch.totalMinutesAsleep>180 && epoch.sleepEpisodeMinute>=latest180){
			if(D){Log.w("SleepAnalyzer", "Trying to notify user4");
//			sb.append("Trying to notify user4");
			}
			if(remindersToPlay>0){
				remindersToPlay--;
				return true;
			}else return false;
		}
		}
	
	return false;
	}
	
	
	public boolean detectREM(SleepDataPoint epoch, boolean coleAsleep){
		
		if(D){Log.w("SleepAnalyzer", "Detecting REM: ");}
		sb.append("Detecting REM: ");
		//TODO code these into user preferences
		//algorithm as of 04-02-2011
		//total minutes asleep; earliest interruption; latest interuption
		// <120; 30; 45
		// >=120; 20; 30
		//>150; 15; 20;
		//>180; 10; 15;
		
		try {
			//if a user has moved so much that cole no longer thinks the user is asleep
			//check the sleep duration to distinguish from random tossing and turning or periods of wakefulness
			
			if (!coleAsleep &&playEarlyReminders) {
				if(remindersToPlay<1){
					remindersToPlay = remindersToPlayCopy;//restore the reminders upon waking up.
				}
				 
				// look at the previous epoch and see long long the user was
				// asleep
				SleepDataPoint an1 = dataManager.getSleepDataPoint(epoch.sleepEpoch - 1);
				int sleepEpisodeMinute = an1.sleepEpisodeMinute;

				if (epoch.totalMinutesAsleep < 120 && sleepEpisodeMinute >= earliest120) {
					if(D){Log.w("SleepAnalyzer", "User awakened1");
					sb.append("User awakened1");}
					return true;
				}
				else if (epoch.totalMinutesAsleep >= 120 && sleepEpisodeMinute >= earliest120to180) {
					if(D){Log.w("SleepAnalyzer", "User awakened1");
					sb.append("User awakened2");}
					return true;
				}
				
				else if (epoch.totalMinutesAsleep >180 && sleepEpisodeMinute >= earliest180) {
					if(D){Log.w("SleepAnalyzer", "User awakened1");
					sb.append("User awakened4");}
					return true;
				}
				
				
			}
			
		} catch (Exception e) {
			return false;
		}
		//sb.append("Not Detected! ---");
		return false;
	}
	
	public void describeSettings(boolean D){
		if(D){Log.w("SleepAnalyzer", "Settings:");}
		if(D){Log.w("SleepAnalyzer", "Cole Constant: "+coleConstant);}
		if(D){Log.w("SleepAnalyzer", "earliest120: "+earliest120);}
		if(D){Log.w("SleepAnalyzer", "earliest120to180: "+earliest120to180);}
		if(D){Log.w("SleepAnalyzer", "earliest180: "+earliest180);}
		
		if(D){Log.w("SleepAnalyzer", "latest120: "+latest120);}
		if(D){Log.w("SleepAnalyzer", "earliest120to180: "+latest120to180);}
		if(D){Log.w("SleepAnalyzer", "earliest180: "+latest180);}
		
		if(D){Log.w("SleepAnalyzer", "remindersToPlay: "+remindersToPlay);}
		if(D){Log.w("SleepAnalyzer", "earliest120to180: "+earliest120to180);}
		if(D){Log.w("SleepAnalyzer", "earliest180: "+earliest180);}
		
		if(D){Log.w("SleepAnalyzer", "playLateReminders: "+playLateReminders);}
		if(D){Log.w("SleepAnalyzer", "playEarlyReminders: "+playEarlyReminders);}
	
		
	}
	
	public static String giveSleepRecommendation(SleepDataManager dm){
		SharedPreferences prefs = null;
		SleepStatistics stat = dm.getStatistics();
		boolean p = false;//can access preferences
		if(appContext!=null){
		 prefs = PreferenceManager
        .getDefaultSharedPreferences(appContext);
		p=true;
		}
		
		StringBuilder sb = new StringBuilder();
		
		if(stat.getNumberOfLucidDreams()>0){
			sb.append(" Congratulations on your lucid dream! ");
		}else{
			sb.append(" No lucid dreams. Try again tonight! ");			
		}
		
		if((float)stat.getSleepEfficiency()<0.5){
			sb.append("Your sleep efficiency is only ");
			sb.append(dm.getStatistics().getSleepEfficiencyString());
			sb.append(" and you got at most ");
			sb.append(String.format("%.1f", stat.getTotalTimeInBed()*stat.getSleepEfficiency()/60 ));
			sb.append(" hours of sleep this night");
			sb.append(" ,consider adjusting your caffeine intake late in the day or increase your time in bed. ");
			if(stat.getNumberOfVoiceReminders()>3){	sb.append("Try to reduce the use of this app for lucid dream induction early in the night ");}
			sb.append(" This will help you wake up more refreshed! \r\n");
					
		}else if((float)stat.getSleepEfficiency()>0.8 && stat.getTotalMinutesAsleep()>479 ){
			
			sb.append("Great job sleeping :) ! Your sleep efficiency is ");
			sb.append(dm.getStatistics().getSleepEfficiencyString());
			sb.append(" and you got full ");
			sb.append(String.format("%.1f",stat.getTotalMinutesAsleep()*stat.getSleepEfficiency()/60) );
			sb.append(" hours of sleep this night!");
		}
		if(stat.getNumberOfVoiceReminders()>3 && stat.getNumberOfAwakenings()>4){
			sb.append(" The app wakes you up too often. Consider changing your voice reminder to tell you to use DEILD induction method ");
		}
		
		if(p && prefs.getBoolean("enable_gestures", false)){
			sb.append(" [Consider enabling gestures to mark your sleep with user events]");
		}else if(stat.getNumberOfUserEvents()<1){
			sb.append(" You may draw gestures on screen to mark your dreams for easy graph analysis ");
		}
		if(stat.getNumberOfUserEvents()>5 && (stat.getNumberOfDreams()+stat.getNumberOfLucidDreams())<1 ){
			sb.append(" Please don't give up on using the app for lucid dream induction! ");
		}
		
		
		
		
		// if preferences are available, do preference analysis on early sleep periods
		 if(p &&playEarlyReminders){ 
			 if(stat.getLongestSleepEpisode()>prefs.getInt("deep_sleep_120_earliest", 30)){
				 sb.append(" Your early night app preferences are good. ");
		 }else{
			 sb.append(" Your sleep episodes are too short for your early night app preferences to detect. ");
		 }
		 	if(stat.getLongestSleepEpisode()>prefs.getInt("deep_sleep_150_earliest", 20)){
		 		
		 		 sb.append(" Your mid night app preferences are good. ");
			 
			 }else{
				 sb.append(" Your sleep episodes are too short for your middle of the night app preferences to detect. ");
			 }
		 	
		 	if(stat.getLongestSleepEpisode()>prefs.getInt("deep_sleep_180_earliest", 10)){
				 sb.append(" Your late night app preferences are good.  ");
			 }else{
				 sb.append("Your sleep episodes are too short for effective REM detection. Consider recalibrating the device or raising motion detection threshold");
			 }
		 }
		 
		// sb.append(" Please consider anonymously submitting your sleep data for science! ");

		 return sb.toString();
	}
	

	public SleepDataManager getDataManager() {
		return dataManager;
	}

	public void setDataManager(SleepDataManager dataManager) {
		this.dataManager = dataManager;
	}

	public static float getColeConstant() {
		return coleConstant;
	}

	public static void setColeConstant(float coleConstant) {
		//coleConstant = 0.001192f;
		SleepAnalyzer.coleConstant = coleConstant;
	
	}

	public int getEarliest120() {
		return earliest120;
	}

	public void setEarliest120(int earliest120) {
		this.earliest120 = earliest120;
	}

	public int getLatest120() {
		return latest120;
	}

	public void setLatest120(int latest120) {
		this.latest120 = latest120;
	}

	public int getEarliest180() {
		return earliest180;
	}

	public void setEarliest180(int earliest180) {
		this.earliest180 = earliest180;
	}

	public int getLatest180() {
		return latest180;
	}

	public void setLatest180(int latest180) {
		this.latest180 = latest180;
	}

	public int getEarliest120to180() {
		return earliest120to180;
	}

	public void setEarliest120to180(int earliest120to180) {
		this.earliest120to180 = earliest120to180;
	}

	public int getLatest120to180() {
		return latest120to180;
	}

	public void setLatest120to180(int latest120to180) {
		this.latest120to180 = latest120to180;
	}

	public boolean isPlayLateReminders() {
		return playLateReminders;
	}

	public void setPlayLateReminders(boolean playLateReminders) {
		this.playLateReminders = playLateReminders;
	}

	public boolean isPlayEarlyReminders() {
		return playEarlyReminders;
	}

	public void setPlayEarlyReminders(boolean playEarlyReminders) {
		this.playEarlyReminders = playEarlyReminders;
	}

	public int getRemindersToPlay() {
		return remindersToPlay;
	}

	public void setRemindersToPlay(int remindersToPlay) {
		this.remindersToPlay = remindersToPlay;
		remindersToPlayCopy = remindersToPlay;
	}

	/**
	 * @param appContext the appContext to set
	 */
	public void setAppContext(Context appContext) {
		this.appContext = appContext;
	}

	public int getActivityThreshold() {
		return activityThreshold;
	}

	public void setActivityThreshold(int activityThreshold) {
		this.activityThreshold = activityThreshold;
	}

	public int getProgressBarActivityCount() {
		return progressBarActivityCount;
	}

	public void setProgressBarActivityCount(int progressBarActivityCount) {
		this.progressBarActivityCount = progressBarActivityCount;
	}

	public int getMaxActivityCount() {
		return (int)(1/coleConstant);
	}

	public void setMaxActivityCount(int maxActivityCount) {
		this.maxActivityCount = maxActivityCount;
	}

	public boolean isEnableREMPrediction() {
		return enableREMPrediction;
	}

	public void setEnableREMPrediction(boolean enableREMPrediction) {
		this.enableREMPrediction = enableREMPrediction;
	}

	public int getMinimumReminderSpacing() {
		return minimumReminderSpacing;
	}

	public void setMinimumReminderSpacing(int minimumReminderSpacing) {
		this.minimumReminderSpacing = minimumReminderSpacing;
	}

	public int getRemPredictionActivityThreshold() {
		return remPredictionActivityThreshold;
	}

	public void setRemPredictionActivityThreshold(int remPredictionActivityThreshold) {
		this.remPredictionActivityThreshold = remPredictionActivityThreshold;
		smartTimer.setSleepScoreThreshold(remPredictionActivityThreshold);
	}

	public SmartTimer getSmartTimer() {
		return smartTimer;
	}

	public void setSmartTimer(SmartTimer smartTimer) {
		this.smartTimer = smartTimer;
	}
	
	public void reset(){
		countdown = 8;
		sleepScoringStarted = true;
		this.totalMinutesAsleep=0;
	}
	
	
}
