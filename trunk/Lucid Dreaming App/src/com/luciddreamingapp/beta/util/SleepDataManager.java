package com.luciddreamingapp.beta.util;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;

import org.json.JSONArray;
import org.json.JSONObject;

import android.util.Log;

import com.luciddreamingapp.beta.GlobalApp;
import com.luciddreamingapp.beta.LucidDreamingApp;
import com.luciddreamingapp.beta.R;

/**Serves as a central data repository for the lucid dreaming app. Keeps track of sleep
 * data minute by minute (epoch by epoch). Has the capability to save data accumulated while the app was running
 * 
 * @author Alex
 *
 */
public class SleepDataManager implements DataManagerObservable {

	 private static final String TAG = "SleepDataManager";
	 private static final boolean D = false;
	private static SleepDataManager instance = null;
	public static final String GRAPH_TYPE ="LucidDreamingApp Graph";
	
	
	public static GlobalApp globalApp;
	static int expectedEpochs = 1200;
	
	public static final int epochLength = 60000;//ms

	String info = "Android \"Lucid Dreaming App\" sleep data collected through actigraphy\r\n";
//timestamp	date	sleepEpoch	XYZ Activity Count	xAxis Activity Count	 yAxis Activity Count	 zAxis Activity Count	coleSleepScore	coleConstant	coleAsleep	 sleepEpisodeMinute	totalMinutesAsleep	reminderPlayed	userEvent

	String header ="UTC timestamp,date,XYZ Activity Count," +
			"xAxis Activity Count, yAxis Activity Count," +
			"zAxis Activity Count,coleSleepScore,coleConstant," +
			"coleAsleep,Time in Bed, totalMinutesAsleep,sleepEpisodeMinute,reminderPlayed,userEvent\r\n";;
	
	//holds sleep information up to 2 minutes in the past. 
	private JSONObject epochGraphJSON;
	
	//default values for displaying data. Intended to make displaying data easier
	private int activityCountYMax = 2500;
	private int sleepScoreYMax = 35;
	
	private String uuid = "";
	
	private boolean loggingEnabled= false;
	JSONArray audioLevelKurtosis = new JSONArray();
	JSONArray audioLevel = new JSONArray();
	JSONArray audioPowerKurtosis = new JSONArray();
	JSONArray audioPower = new JSONArray();
	JSONArray xyzActivityCount = new JSONArray();//activityCounter x
	JSONArray coleSleepScore = new JSONArray();//coleSleepScore x
	JSONArray sleepWakeEpisodes = new JSONArray();//coleAsleep x
	JSONArray totalSleepDuration= new JSONArray();;//totalMinutesAsleepx
	JSONArray reminderPlayed= new JSONArray();;//reminderPlayed
	JSONArray userEvent= new JSONArray();;//userEvent x
	JSONArray userEventDream= new JSONArray();;//userEvent x
	JSONArray userEventLucidDream= new JSONArray();;//userEvent x
	JSONArray userEventAwake= new JSONArray();;//userEvent x
	JSONArray userEventNoDream= new JSONArray();;//userEvent x
	
	
	JSONArray xActivityCount= new JSONArray(); //activity counts along 3 independent axis
	JSONArray yActivityCount= new JSONArray();
	JSONArray zActivityCount= new JSONArray();
	
	private static final String instructionsList = "<br>" +
	"<ul>"+
	"<li>Please put the phone on your mattress, 8\" (20cm) from your pillow. </li>" +
	"<li>Leave the phone there for the night.</li>" +
	"<li> App is working when black triangle is shown in the status bar</li>" +
	"<li> Using menu to exit the clock screen stops data collection</li>" +
	"<li> No real data is collected with screen turned off. Use preferences>display to dim screen</li>" +
	"<li> Sleep scores and graphs will be available after 6 minutes</li>" +
	"<li>After sleep scoring starts, data is delayed by 2 minutes</li>" +
	"<li> New data is appended on top</li>" +
	"<li> Press menu button to bring up menu and change preferences, most can be changed while the app is running</li>" +
	"<li> Happy Dreaming!</li>" +
	"</ul>";
	
	private String sleepStatus =instructionsList;
	
	 
	 //TODO consider copyonwrite array list
	private ArrayList<SleepDataPoint> sleepData;
	private SleepStatistics statistics;
	
	LogManager csvFile;
	public static synchronized SleepDataManager getInstance(){
		if(instance == null){
			return instance = new SleepDataManager(expectedEpochs);
		}else return instance;
	}
	
	protected SleepDataManager(int expectedEpochs){
		
		header = ""+info+header;
		csvFile= new LogManager(LucidDreamingApp.LOG_LOCATION,"Sleep_Score_Log.txt",header);
		//csvFile.appendEntry(header);//redundant, but helps with format changes

		reset();
		
		
	}
	
	public int size(){
		return sleepData.size();
	}
	
	public void logEpoch(SleepDataPoint epoch){
		if(loggingEnabled){csvFile.appendEntry(epoch.toString());}
	}
	
	
	public  void addSleepDataPoint(SleepDataPoint epoch){
		//TODO include ensure capacity algorithm
		
		statistics.setTotalTimeInBed(sleepData.size()+1);
		updateDreamsCount(epoch);
		epoch.setHistoryStatistics(statistics.deepCopy());
		sleepData.add(epoch);
		
		dataPointAdded(epoch);
		
		}
	
	


	
	public SleepDataPoint getSleepDataPoint(int epoch){
		try{return sleepData.get(epoch);}
		catch(Exception e){
			if(D){Log.e(TAG, "Epoch "+epoch+" not found. Total Epochs: "+sleepData.size());}
			if(D) e.printStackTrace();
			return null;
		}
	}
	
	public String getSleepDataString(int epoch){
				
		return sleepData.get(epoch).toString();
	}
	
	public String getSleepDataHTMLString(int epoch){
		return sleepData.get(epoch).toHTMLString();
	}
	
	
	
	
	
	
	
	public String getSleepStatus() {
		return sleepStatus;
	}

	public void setSleepStatus(String message) {
		sleepStatus = message+sleepStatus;	
		
		//notify observers
		listDataUpdated();
		
	}

	/**
	 * @return the loggingEnabled
	 */
	public boolean isLoggingEnabled() {
		return loggingEnabled;
	}

	/**
	 * @param loggingEnabled the loggingEnabled to set
	 */
	public void setLoggingEnabled(boolean loggingEnabled) {
		this.loggingEnabled = loggingEnabled;
	}

	private void updateDreamsCount(SleepDataPoint epoch){
		
		//check if a user has interacted with the device
//		if(a0.getUserEvent()!=0){
//			int temp = dataManager.getStatistics().getNumberOfUserEvents();
//			dataManager.getStatistics().setNumberOfUserEvents(temp+1);}
		int tempUserEvents = statistics.getNumberOfUserEvents();
		int tempDreams = statistics.getNumberOfDreams();
		int tempLucidDreams = statistics.getNumberOfLucidDreams();
		
		//TODO can use fall through for some of these events
		switch(epoch.userEvent){
		case SleepDataPoint.NO_EVENT: 
			break;
		case SleepDataPoint.USER_EVENT_DREAM: 
			tempDreams++;
			tempUserEvents++;
			break;
		case SleepDataPoint.USER_EVENT_NO_DREAM: 
			tempUserEvents++;
			break;
		case SleepDataPoint.USER_EVENT_AWAKE: 
			tempUserEvents++;
			break;
		case SleepDataPoint.USER_EVENT_LUCID_DREAM:
			tempLucidDreams++;
			tempUserEvents++;
			break;		
		case SleepDataPoint.USER_EVENT_NOT_RECOGNIZED: 
			tempUserEvents++;
			break;
		default: 
			break;
		}
		
		statistics.setNumberOfUserEvents(tempUserEvents);
		statistics.setNumberOfDreams(tempDreams);
		statistics.setNumberOfLucidDreams(tempLucidDreams);
		
		
	}
	
	
	public boolean saveAsCSV(){
		
		String filename = getFileName()+".csv";
		
		try{
		LogManager csvFile= new LogManager(LucidDreamingApp.APP_HOME_FOLDER,filename,header);
		
		for(SleepDataPoint epoch: sleepData){
			csvFile.appendEntry(epoch.toString()+"\r\n");
		}
		}catch(Exception e){
			if(D)e.printStackTrace();
			return false;
			}
		return true;
		}
		
	public void updateEpochGraph(SleepDataPoint epoch){
		
	
		//UTC timestamp, adjusted for the timezone and daylight savings time difference
		long timestamp = epoch.calendar.getTimeInMillis()+
		epoch.calendar.getTimeZone().getOffset(epoch.calendar.getTimeInMillis());
	
		JSONArray temp = new JSONArray();
		try{
				temp.put(timestamp);//epoch - X Value
				temp.put(epoch.activityCount);				
				xyzActivityCount.put(temp);}catch(Exception ex){
					
				if(D){	ex.printStackTrace();}}
				temp = new JSONArray();
				try{
						temp.put(timestamp);//epoch - X Value
						temp.put(epoch.coleSleepScore);				
						coleSleepScore.put(temp);}catch(Exception ex){
						if(D){	ex.printStackTrace();}}
						
						temp = new JSONArray();
						try{
								temp.put(timestamp);//epoch - X Value
								temp.put(100+epoch.audioPower);				
								audioPower.put(temp);}catch(Exception ex){
								if(D){	ex.printStackTrace();}}
								temp = new JSONArray();
								try{
										temp.put(timestamp);//epoch - X Value
										temp.put(epoch.audioPowerKurtosis);				
										audioPowerKurtosis.put(temp);}catch(Exception ex){
										if(D){	ex.printStackTrace();}}
						
						temp = new JSONArray();
						try{
								temp.put(timestamp);//epoch - X Value
								temp.put(epoch.audioLevel);				
								audioLevel.put(temp);}catch(Exception ex){
								if(D){	ex.printStackTrace();}}
								
								temp = new JSONArray();
								try{
										temp.put(timestamp);//epoch - X Value
										temp.put(epoch.audioLevelKurtosis);				
										audioLevelKurtosis.put(temp);}catch(Exception ex){
										if(D){	ex.printStackTrace();}}
						
						temp = new JSONArray();
						try{
								temp.put(timestamp);//epoch - X Value
								temp.put(epoch.totalMinutesAsleep);				
								totalSleepDuration.put(temp);}catch(Exception ex){
								if(D){	ex.printStackTrace();}}
								
								temp = new JSONArray();
								try{
										temp.put(timestamp);//epoch - X Value
										if(epoch.reminderPlayed){
											temp.put(25);
										}			
										reminderPlayed.put(temp);}catch(Exception ex){
										if(D){	ex.printStackTrace();}}
										
										temp = new JSONArray();
										try{
												temp.put(timestamp);//epoch - X Value
													
											if(epoch.coleAsleep){temp.put(20);
											}else if(epoch.coleSleepScore>1){temp.put(0);}
											else{temp.put(10);}		
											sleepWakeEpisodes.put(temp);}catch(Exception ex){
												if(D){	ex.printStackTrace();}}
											
											temp = new JSONArray();
											try{
												
												if(epoch.userEvent!=0){
													//do nothing for 0 event
													switch(epoch.userEvent){
													case SleepDataPoint.USER_EVENT_AWAKE:
														temp.put(timestamp);
														temp.put(27);
														userEventAwake.put(temp);
														break;
													case SleepDataPoint.USER_EVENT_DREAM:
														temp.put(timestamp);
														temp.put(27);
														userEventDream.put(temp);
														break;
													case SleepDataPoint.USER_EVENT_LUCID_DREAM:
														temp.put(timestamp);
														temp.put(27);
														userEventLucidDream.put(temp);
														break;
													case SleepDataPoint.USER_EVENT_NO_DREAM:
														temp.put(timestamp);
														temp.put(27);
														userEventNoDream.put(temp);
														break;
													case SleepDataPoint.USER_EVENT_NOT_RECOGNIZED:
														temp.put(timestamp);
														temp.put(27);
														userEvent.put(temp);
														break;
													}
													
													
												}															
												
											}catch(Exception ex){
												if(D){	ex.printStackTrace();}}

											
			
			generateEpochGraph();
										
	}
	


		
public JSONObject getEpochGraphJSON() {
		return epochGraphJSON;
	}

	
public synchronized void generateEpochGraph(){
	
	if(graphDataUpdatedList.size()<1){
		//if noone is watching the list
		return;
	}
		else{
	
	
		try{
		epochGraphJSON = new JSONObject();
		
		//try to save date for the first sleep epoch						
		// add all data
		
		epochGraphJSON.put("audioLevel",audioLevel);
		epochGraphJSON.put("audioLevelLabel", "Audio Level");
		
		epochGraphJSON.put("audioLevelKurtosis",audioLevelKurtosis);
		epochGraphJSON.put("audioLevelKurtosisLabel", "Audio Level Kurtosis");
		
		epochGraphJSON.put("audioPowerKurtosis",audioPowerKurtosis);
		epochGraphJSON.put("audioPowerKurtosisLabel", "Audio Power Kurtosis");
		
		epochGraphJSON.put("audioPower",audioPower);
		epochGraphJSON.put("audioPowerLabel", "Audio Power");
		
		

		epochGraphJSON.put("xyzActivityCount",xyzActivityCount);
		epochGraphJSON.put("xyzActivityCountLabel", "XYZ Activity Count");
		
		epochGraphJSON.put("coleSleepScore",coleSleepScore);
		epochGraphJSON.put("coleSleepScoreLabel", "Sleep Score");
		
		epochGraphJSON.put("sleepWakeEpisodes",sleepWakeEpisodes);
		epochGraphJSON.put("sleepWakeEpisodesLabel", "Sleep Episodes");
		
		epochGraphJSON.put("totalSleepDuration",totalSleepDuration);
		epochGraphJSON.put("totalSleepDurationLabel", "Sleep Duration");
		
		epochGraphJSON.put("reminderPlayed",reminderPlayed);
		epochGraphJSON.put("reminderPlayedLabel", "Reminder Played");
		
		epochGraphJSON.put("userEvent",userEvent);
		epochGraphJSON.put("userEventLabel", "User Event");
		
		epochGraphJSON.put("userEventDream",userEventDream);
		epochGraphJSON.put("userEventDreamLabel", "Dream");
		
		epochGraphJSON.put("userEventLucidDream",userEventLucidDream);
		epochGraphJSON.put("userEventLucidDreamLabel", "Lucid Dream");
		
		epochGraphJSON.put("userEventAwake",userEventAwake);
		epochGraphJSON.put("userEventAwakeLabel", "Awake");
		
		epochGraphJSON.put("userEventNoDream",userEventNoDream);
		epochGraphJSON.put("userEventNoDreamLabel", "No Dream");
		
		
			
		//TODO data for the pie chart
		epochGraphJSON.put("totalTimeInBed", statistics.getTotalTimeInBed());
		epochGraphJSON.put("totalMinutesAsleep", statistics.getTotalMinutesAsleep());
		epochGraphJSON.put("numberOfEpochs", sleepData.size());
		
		//data for the statistics table
		epochGraphJSON.put("sleepOnsetLatency", statistics.getSleepOnsetLatency());
		epochGraphJSON.put("numberOfAwakenings", statistics.getNumberOfAwakenings());
		epochGraphJSON.put("longestSleepEpisode", statistics.getLongestSleepEpisode());
		epochGraphJSON.put("sleepEfficiency", statistics.getSleepEfficiency());
		epochGraphJSON.put("numberOfUserEvents", statistics.getNumberOfUserEvents());
		epochGraphJSON.put("numberOfVoiceReminders", statistics.getNumberOfVoiceReminders());
		epochGraphJSON.put("numberOfDreams", statistics.getNumberOfDreams());
		epochGraphJSON.put("numberOfLucidDreams", statistics.getNumberOfLucidDreams());
		
		//TODO implement
		//analysis string to help the user get the best out of the app
		//analyzes data, settings, gives suggestions for the app use
		epochGraphJSON.put("analysis", " Available when data is saved");		
		
		}catch(Exception e){
			if(D){e.printStackTrace();}
		}
		graphDataUpdated();
	}
		
		//notify observers
		
		
	}
	
		
	
	
	public JSONObject saveDataJSON(){ 

		JSONArray audioPower = new JSONArray();
		JSONArray audioPowerKurtosis = new JSONArray();
		JSONArray audioLevel = new JSONArray();
		JSONArray audioLevelKurtosis = new JSONArray();
		JSONArray sleepEpoch = new JSONArray();//sleepEpoch x
		JSONArray epochActivity = new JSONArray();//epochActivitySum x
		JSONArray xyzActivityCount = new JSONArray();//activityCounter x
		JSONArray coleSleepScore = new JSONArray();//coleSleepScore x
		JSONArray sleepWakeEpisodes = new JSONArray();//coleAsleep x
		JSONArray totalSleepDuration= new JSONArray();;//totalMinutesAsleepx
		JSONArray reminderPlayed= new JSONArray();;//reminderPlayed
		JSONArray userEvent= new JSONArray();;//userEvent x
		JSONArray userEventDream= new JSONArray();;//userEvent x
		JSONArray userEventLucidDream= new JSONArray();;//userEvent x
		JSONArray userEventAwake= new JSONArray();;//userEvent x
		JSONArray userEventNoDream= new JSONArray();;//userEvent x
		
		JSONArray xActivityCount= new JSONArray(); //activity counts along 3 independent axis
		JSONArray yActivityCount= new JSONArray();
		JSONArray zActivityCount= new JSONArray();
		
//		JSONArray xActivitySum= new JSONArray();
//		JSONArray yActivitySum= new JSONArray();
//		JSONArray zActivitySum= new JSONArray();
		String date;
		long timeZoneOffset = 0;
		long firstTimestamp = 0; //for timestamp to sleep minute conversions
		
		try{
		date = sleepData.get(0).calendar.getTime().toLocaleString();
		}catch(Exception e){
			if(D)e.printStackTrace();
			date =Calendar.getInstance().getTime().toLocaleString();
			timeZoneOffset=Calendar.getInstance().getTimeZone()
			.getOffset(Calendar.getInstance().getTimeInMillis());
		}
		for(SleepDataPoint epoch: sleepData){
			JSONArray temp = new JSONArray(); //2d array, will resolve to X,Y Coordinates
			//pretend that we are in UTC time to plot accurately
			long timestamp = epoch.calendar.getTimeInMillis()+
					epoch.calendar.getTimeZone().getOffset(epoch.calendar.getTimeInMillis());
			
			if(firstTimestamp ==0){
				firstTimestamp = timestamp;
				timeZoneOffset=Calendar.getInstance().getTimeZone()
				.getOffset(Calendar.getInstance().getTimeInMillis());
			}
			
			//epochActivity
			//save epoch Activity
			try{temp.put(timestamp);//X value
				temp.put((int)epoch.epochActivitySum);//y Value			
				//try to insert values in order
				epochActivity.put(temp);}catch(Exception ex)
				{if(D){	ex.printStackTrace();}}
				
			//sleepWakeEpisodes	
			temp = new JSONArray();
				
				try{
					//save the sleep state - shows when the user is asleep according to cole
					temp.put(timestamp);
				if(epoch.coleAsleep){temp.put(20);
					}else if(epoch.coleSleepScore>1){temp.put(0);}
					else{temp.put(10);}
					sleepWakeEpisodes.put(temp);}catch(Exception ex){
					if(D){	ex.printStackTrace();}}
					
					temp = new JSONArray();
					//Overall audio intensity
					try{
						temp.put(timestamp);
						temp.put(100+epoch.audioPower);				
						audioPower.put(temp);}catch(Exception ex){
						if(D){	ex.printStackTrace();}}
						temp = new JSONArray();
						//Overall audio intensity
						try{
							temp.put(timestamp);
							temp.put(epoch.audioPowerKurtosis);				
							audioPowerKurtosis.put(temp);}catch(Exception ex){
							if(D){	ex.printStackTrace();}}
					
					temp = new JSONArray();
					//Overall audio intensity
					try{
						temp.put(timestamp);
						temp.put(epoch.audioLevel);				
						audioLevel.put(temp);}catch(Exception ex){
						if(D){	ex.printStackTrace();}}
						
						//level of peakedness of the audio
						temp = new JSONArray();
						try{
								temp.put(timestamp);
								temp.put(epoch.audioLevelKurtosis);				
								audioLevelKurtosis.put(temp);}catch(Exception ex){
								if(D){	ex.printStackTrace();}}
					
					
			//activityCounter		
			temp = new JSONArray();
				try{
						temp.put(timestamp);//epoch - X Value
						temp.put(epoch.activityCount);
						
						xyzActivityCount.put(temp);}catch(Exception ex){
						if(D){	ex.printStackTrace();}}
				
				//	coleSleepScore
				temp = new JSONArray();
				try{
					//Save sleep score data
					temp.put(timestamp);
					temp.put((int)epoch.coleSleepScore);
					coleSleepScore.put(temp);}catch(Exception ex){
					if(D){	ex.printStackTrace();}}
					
					temp = new JSONArray();
					try{
						//total sleep minutes
						temp.put(timestamp);
						temp.put(epoch.totalMinutesAsleep);
						totalSleepDuration.put(temp);}catch(Exception ex){
						if(D){	ex.printStackTrace();}}
					
						temp = new JSONArray();
						
							
							temp = new JSONArray();
							try{
								//sleep epoch number
								temp.put(timestamp);
								temp.put(epoch.sleepEpoch);
								sleepEpoch.put(temp);}catch(Exception ex){
								if(D){	ex.printStackTrace();}}
								
								temp = new JSONArray();
								try{
									
									if(epoch.userEvent!=0){
										//do nothing for 0 event
										switch(epoch.userEvent){
										case SleepDataPoint.USER_EVENT_AWAKE:
										case SleepDataPoint.USER_EVENT_NO_DREAM: //consider them identical
											temp.put(timestamp);
											temp.put(27);
											userEventNoDream.put(temp);
											temp.put(timestamp);
											temp.put(27);
											userEventAwake.put(temp);
											break;
										case SleepDataPoint.USER_EVENT_DREAM:
											temp.put(timestamp);
											temp.put(27);
											userEventDream.put(temp);
											break;
										case SleepDataPoint.USER_EVENT_LUCID_DREAM:
											temp.put(timestamp);
											temp.put(27);
											userEventLucidDream.put(temp);
											break;									
										case SleepDataPoint.USER_EVENT_NOT_RECOGNIZED:
											temp.put(timestamp);
											temp.put(27);
											userEvent.put(temp);
											break;
										}
										
										
									}															
									
								}catch(Exception ex){
									if(D){	ex.printStackTrace();}}
									
									temp = new JSONArray();
									try{
										//Reminder played
										temp.put(timestamp);
										if(epoch.reminderPlayed){
											temp.put(25);
										reminderPlayed.put(temp);}
										}catch(Exception ex){
										if(D){	ex.printStackTrace();}}
					
										temp = new JSONArray();
										try{
											//getXActivityCount
											temp.put(timestamp);
											temp.put(epoch.getXActivityCount());
											xActivityCount.put(temp);}catch(Exception ex){
											if(D){	ex.printStackTrace();}}
											
											temp = new JSONArray();
											try{
												//getYActivityCount
												temp.put(timestamp);
												temp.put(epoch.getYActivityCount());
												yActivityCount.put(temp);}catch(Exception ex){
												if(D){	ex.printStackTrace();}}
											
												temp = new JSONArray();
												try{
													//getZActivityCount
													temp.put(timestamp);
													temp.put(epoch.getZActivityCount());
													zActivityCount.put(temp);}catch(Exception ex){
													if(D){	ex.printStackTrace();}}
													
//													temp = new JSONArray();
//													try{
//														//getXActivitySum
//														temp.put(timestamp);
//														temp.put(epoch.getXActivitySum());
//														xActivitySum.put(temp);}catch(Exception ex){
//														if(D){	ex.printStackTrace();}}
//														try{
//															//getYActivitySum
//															temp.put(timestamp);
//															temp.put(epoch.getYActivitySum());
//															yActivitySum.put(temp);}catch(Exception ex){
//															if(D){	ex.printStackTrace();}}
//															try{
//																//getZActivitySum
//																temp.put(timestamp);
//																temp.put(epoch.getZActivitySum());
//																zActivitySum.put(temp);}catch(Exception ex){
//																if(D){	ex.printStackTrace();}}
				
			}//end for each sleep epoch
			
		/*
		JSONArray sleepEpoch = new JSONArray();//sleepEpoch x
		JSONArray epochActivity = new JSONArray();//epochActivitySum x
		JSONArray activityCounter = new JSONArray();//activityCounter x
		JSONArray coleSleepScore = new JSONArray();//coleSleepScore x
		JSONArray sleepWakeEpisodes = new JSONArray();//coleAsleep x
		JSONArray totalSleepDuration;//totalMinutesAsleepx
		JSONArray reminderPlayed;//reminderPlayed
		JSONArray userEvent;//userEvent x
		*/
		
		
		try{
		JSONObject json = new JSONObject();
		
		//try to save date for the first sleep epoch
		try{
		json.put("Date Created:",sleepData.get(0).getCalendar().getTime().toLocaleString());
		}catch(Exception e){
			try{
		json.put("Date Created:",sleepData.get(1).getCalendar().getTime().toLocaleString());
		}catch(Exception ee){};
		};
		
		json.put("UUID", uuid);
		
		json.put("File Info","Java Script Object Notation file from \"Lucid Dreaming App\" Used to plot data in the app");
		json.put("date", date );
		json.put("firstTimestamp", firstTimestamp);
		// add all data
		
		json.put("sleepEpoch",sleepEpoch);
		json.put("sleepEpochLabel", getString(R.string.graph_label_epoch));
		
		json.put("epochActivity",epochActivity);
		json.put("epochActivityLabel", getString(R.string.graph_label_activity_sum));
		
		json.put("xyzActivityCount",xyzActivityCount);
		json.put("xyzActivityCountLabel", getString(R.string.graph_label_activity_count));
		
		json.put("audioLevel",audioLevel);
		json.put("audioLevelLabel", "Audio Level");
		
		json.put("audioLevelKurtosis",audioLevelKurtosis);
		json.put("audioLevelKurtosisLabel", "Audio Level Kurtosis");
		
		json.put("audioPower",audioPower);
		json.put("audioPowerLabel", "Audio Power dB");
		
		json.put("audioPowerKurtosis",audioPowerKurtosis);
		json.put("audioPowerKurtosisLabel", "Audio Power Kurtosis");
		
		
		json.put("coleSleepScore",coleSleepScore);
		json.put("coleSleepScoreLabel", getString(R.string.graph_label_sleep_score));
		
		json.put("sleepWakeEpisodes",sleepWakeEpisodes);
		json.put("sleepWakeEpisodesLabel", getString(R.string.graph_label_sleep_episodes));
		
		json.put("totalSleepDuration",totalSleepDuration);
		json.put("totalSleepDurationLabel", getString(R.string.graph_label_sleep_duration));
		
		json.put("reminderPlayed",reminderPlayed);
		json.put("reminderPlayedLabel", getString(R.string.graph_label_reminder_played));
		
		json.put("userEvent",userEvent);
		json.put("userEventLabel", getString(R.string.graph_label_user_event));
		
		json.put("userEventDream",userEventDream);
		json.put("userEventDreamLabel", getString(R.string.graph_label_user_event_dream));
		
		json.put("userEventLucidDream",userEventLucidDream);
		json.put("userEventLucidDreamLabel", getString(R.string.graph_label_user_event_lucid_dream));
		
		json.put("userEventAwake",userEventAwake);
		json.put("userEventAwakeLabel", getString(R.string.graph_label_user_event_awake));
		
		json.put("userEventNoDream",userEventNoDream);
		json.put("userEventNoDreamLabel", getString(R.string.graph_label_user_event_no_dream));
		
		
		json.put("xActivityCount",xActivityCount);
		json.put("xActivityCountLabel", getString(R.string.graph_label_activity_count_x));
		json.put("yActivityCount",yActivityCount);
		json.put("yActivityCountLabel", getString(R.string.graph_label_activity_count_y));
		json.put("zActivityCount",zActivityCount);
		json.put("zActivityCountLabel", getString(R.string.graph_label_activity_count_z));
		
				
		//TODO data for the pie chart
		json.put("totalTimeInBed", statistics.getTotalTimeInBed());
		json.put("totalMinutesAsleep", statistics.getTotalMinutesAsleep());
		json.put("numberOfEpochs", sleepData.size());
		
		//data for the statistics table
		json.put("sleepOnsetLatency", statistics.getSleepOnsetLatency());
		json.put("numberOfAwakenings", statistics.getNumberOfAwakenings());
		json.put("longestSleepEpisode", statistics.getLongestSleepEpisode());
		json.put("sleepEfficiency", statistics.getSleepEfficiency());
		json.put("numberOfUserEvents", statistics.getNumberOfUserEvents());
		json.put("numberOfVoiceReminders", statistics.getNumberOfVoiceReminders());
		json.put("numberOfDreams", statistics.getNumberOfDreams());
		json.put("numberOfLucidDreams", statistics.getNumberOfLucidDreams());
		
		//TODO implement
		//analysis string to help the user get the best out of the app
		//analyzes data, settings, gives suggestions for the app use
		json.put("analysis", SleepAnalyzer.giveSleepRecommendation(this));
		json.put("filename", getFileName()+".gzip");
		json.put("filetype", GRAPH_TYPE );
		json.put("version", "2" );
		json.put("timeZoneOffset",timeZoneOffset);
		
		json.put("activityCountYMax", activityCountYMax);
		json.put("sleepScoreYMax",sleepScoreYMax);
		
		return json;
		}catch(Exception e){
			if(D){e.printStackTrace();}
		}
		return null;
	}
	
	
	
	/**
	 * @return the statistics
	 */
	public SleepStatistics getStatistics() {
		return statistics;
	}

	public  String getFileName(){
		
		 SimpleDateFormat format = new SimpleDateFormat("yyyyMMdd_HHmm");
		try{
			
			StringBuilder sb = new StringBuilder();
			sb.append("Data_");
			sb.append(format.format(sleepData.get(0).getCalendar().getTime()));
			sb.append(".txt");
			return sb.toString();
			
//			if(begin.before(end)){
				
//				
//				sb.append("Data from ");
//				sb.append(begin.get(Calendar.YEAR));
//				sb.append("-");
//				sb.append(begin.get(Calendar.MONTH)+1);
//				sb.append("-");
//				sb.append(begin.get(Calendar.DAY_OF_MONTH));
//				sb.append(" ");
//				sb.append(begin.get(Calendar.HOUR_OF_DAY));
//				sb.append("_");
//				sb.append(begin.get(Calendar.MINUTE));
//				sb.append("_");
//				sb.append(begin.get(Calendar.SECOND));
//				sb.append(" to ");
//				sb.append(end.get(Calendar.YEAR));
//				sb.append("-");
//				sb.append(end.get(Calendar.MONTH)+1);
//				sb.append("-");
//				sb.append(end.get(Calendar.DAY_OF_MONTH));
//				sb.append(" ");
//				sb.append(end.get(Calendar.HOUR_OF_DAY));
//				sb.append("_");
//				sb.append(end.get(Calendar.MINUTE));
//				sb.append("_");
//				sb.append(end.get(Calendar.SECOND));
//				sb.append(filename);
//				
//				filename = sb.toString();}
		
			
		}catch(Exception e){
				
			StringBuilder sb = new StringBuilder();
			sb.append("Data_");
			sb.append(format.format(Calendar.getInstance().getTime()));
			sb.append(".txt");
			
			return sb.toString();
		}
	
	}

	public int getActivityCountYMax() {
		return activityCountYMax;
	}

	public void setActivityCountYMax(int activityCountYMax) {
		this.activityCountYMax = activityCountYMax;
	}

	public int getSleepScoreYMax() {
		return sleepScoreYMax;
	}

	public void setSleepScoreYMax(int sleepScoreYMax) {
		this.sleepScoreYMax = sleepScoreYMax;
	}
	
	public void reset(){
		sleepData = new ArrayList<SleepDataPoint>(expectedEpochs);
		statistics = new SleepStatistics();	
		
		epochGraphJSON = new JSONObject();
		
		//empty sleep status
		sleepStatus =instructionsList;
		
		
		 xyzActivityCount = new JSONArray();//activityCounter x\
		 audioPower = new JSONArray();
		 audioPowerKurtosis = new JSONArray();
		 audioLevel = new JSONArray();
		 audioLevelKurtosis = new JSONArray();
		 coleSleepScore = new JSONArray();//coleSleepScore x
		 sleepWakeEpisodes = new JSONArray();//coleAsleep x
		 totalSleepDuration= new JSONArray();;//totalMinutesAsleepx
		 reminderPlayed= new JSONArray();;//reminderPlayed
		 userEvent= new JSONArray();;//userEvent x
		 userEventDream= new JSONArray();;//userEvent x
		 userEventLucidDream= new JSONArray();;//userEvent x
		 userEventAwake= new JSONArray();;//userEvent x
		 userEventNoDream= new JSONArray();;//userEvent x
		
		
		 xActivityCount= new JSONArray(); //activity counts along 3 independent axis
		 yActivityCount= new JSONArray();
		 zActivityCount= new JSONArray();
		
	}

	
	private ArrayList<DataManagerObserver> dataPointAddedList = new ArrayList<DataManagerObserver>(10);
	private ArrayList<DataManagerObserver> dataPointUpdatedList = new ArrayList<DataManagerObserver>(10);
	private ArrayList<DataManagerObserver> graphDataUpdatedList = new ArrayList<DataManagerObserver>(10);
	private ArrayList<DataManagerObserver> listDataUpdatedList = new ArrayList<DataManagerObserver>(10);
	
	@Override
	public void addObserver(DataManagerObserver observer, int whichUpdates) {
		
		if(observer==null){return;}
		
		switch(whichUpdates){
		case DataManagerObservable.DATA_POINT_ADDED:
			if(!dataPointAddedList.contains(observer)){
				dataPointAddedList.add(observer);
			}
			break;
		case DataManagerObservable.DATA_POINT_UPDATED:
			if(!dataPointUpdatedList.contains(observer)){
				dataPointUpdatedList.add(observer);
			}
			break;
		case DataManagerObservable.LIST_UPDATED:
			if(!listDataUpdatedList.contains(observer)){
				listDataUpdatedList.add(observer);
			}
			break;
		case DataManagerObservable.GRAPH_UPDATED:
			if(!graphDataUpdatedList.contains(observer)){
				graphDataUpdatedList.add(observer);
			}
			break;
			
			
		}
		
	}

	@Override
	public void unregisterObserver(DataManagerObserver observer,
			int whichUpdates) {
		
		if(observer==null){return;}
		
		switch(whichUpdates){
		case DataManagerObservable.DATA_POINT_ADDED:
			if(dataPointAddedList.contains(observer)){
				dataPointAddedList.remove(observer);
			}
			break;
		case DataManagerObservable.DATA_POINT_UPDATED:
			if(dataPointUpdatedList.contains(observer)){
				dataPointUpdatedList.remove(observer);
			}
			break;
		case DataManagerObservable.LIST_UPDATED:
			if(listDataUpdatedList.contains(observer)){
				listDataUpdatedList.remove(observer);
			}
			break;
		case DataManagerObservable.GRAPH_UPDATED:
			if(graphDataUpdatedList.contains(observer)){
				graphDataUpdatedList.remove(observer);
			}
			break;
		}
		
		
	}

	@Override
	public void dataPointAdded(SleepDataPoint epoch) {
		for(DataManagerObserver observer:dataPointAddedList ){
			observer.dataPointAdded(epoch);
		}
		
	}

	@Override
	public void dataPointUpdated(SleepDataPoint epoch) {
		for(DataManagerObserver observer:dataPointUpdatedList ){
			observer.dataPointUpdated(epoch);
		}
		
	}

	@Override
	public void graphDataUpdated() {
		for(DataManagerObserver observer:graphDataUpdatedList ){
			observer.graphUpdated(epochGraphJSON);
		}
		
	}

	@Override
	public void listDataUpdated() {
		for(DataManagerObserver observer:listDataUpdatedList ){
			observer.listUpdated(sleepStatus);
		}
		
	}

	@Override
	public void onReset() {
		// TODO Auto-generated method stub
		
	}
	
	protected String getString(int resId){
		return globalApp.getString(resId);
	}
	protected String getString(int resId, Object...formatArgs){
		return globalApp.getString(resId, formatArgs);
	}

	public String getUuid() {
		return uuid;
	}

	public void setUuid(String uuid) {
		this.uuid = uuid;
	}
	
	
	
}
