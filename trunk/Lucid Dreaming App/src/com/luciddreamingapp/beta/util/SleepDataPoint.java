package com.luciddreamingapp.beta.util;

import java.util.Calendar;


public class SleepDataPoint {
	
	//constants to describe user events
	public static final int USER_EVENT_DREAM = 1;
	public static final int USER_EVENT_LUCID_DREAM = 2;
	public static final int NO_EVENT = 0;
	public static final int USER_EVENT_NO_DREAM =-1;
	public static final int USER_EVENT_AWAKE =-2;
	public static final int USER_EVENT_NOT_RECOGNIZED =-3;
	
	//sum of absolute epoch activity over the threshold
	//(how far the activity deviates from the average)
	 double epochActivitySum = 0; 
	 
	 
	 int activityCount = 0; //count of activity over threshold
	
	 int eventCount = 0; //Number of sensor events used to calculate this point
	 int sleepEpoch = 0; //the order of the sleep epoch in the night
	 Calendar calendar = null;
	 
	 int audioLevel = 0;
	 int audioLevelKurtosis = 0;
	 
	 int audioPower = -100;
	 int audioPowerKurtosis = 0;
	 SleepStatistics historyStatistics = null;
	
	// int activityCounter = 0;
	 
	 double coleSleepScore = 1.00; //sleep score according to cole's algorithm
	 double coleConstant = SleepAnalyzer.coleConstant;//scaling constant for cole's algorithm
	
	 //asleep if asleep in the current epoch and 5 epochs before current
	 //can be used to calculate sleep latency
	 boolean coleAsleep = false;
	 
	 double sadehSleepScore = -1.00;
	 boolean sadehAsleep = false;
	 
	//number of minutes of sleep in a single sleep episode before this minute
	 int sleepEpisodeMinute = 0; 
	//total number of minutes of sleep before this minute
	 int totalMinutesAsleep = 0;
	//indicates if sound reminder has been played after this minute
	 boolean reminderPlayed = false;
	 
	 int userEvent =SleepDataPoint.NO_EVENT;
	 
	 //Debug variable = see if accelerometer resolution may be causing jittery output
	String accelerometerAccuracy = "";
		@Override
		public String toString(){
			StringBuilder sb = new StringBuilder(128);
			String timeNoCommas = ""+calendar.getTime().toLocaleString();
			timeNoCommas = timeNoCommas.replaceAll("," , "" );//prevent commas from messing up the CSV columns format
			sb.append(calendar.getTimeInMillis());
			sb.append(",");
			sb.append(timeNoCommas);
			sb.append(",");			
			sb.append(activityCount);
			sb.append(",");			
			sb.append(xActivityCount);
			sb.append(",");
			sb.append(yActivityCount);
			sb.append(",");
			sb.append(zActivityCount);
			sb.append(",");
			sb.append(coleSleepScore);
			sb.append(",");
			sb.append(coleConstant);
			sb.append(",");
			sb.append(coleAsleep);
			sb.append(",");
			sb.append(sleepEpoch);
			sb.append(",");		
			sb.append(totalMinutesAsleep);
			sb.append(",");
			sb.append(sleepEpisodeMinute);
			sb.append(",");			
			sb.append(reminderPlayed);
			sb.append(",");
			sb.append(checkUserEvent(userEvent));
			sb.append(",");
			
			
			return sb.toString();
		}
		
		
		public String toDetailedString(){
			StringBuilder sb = new StringBuilder(256);
			
			sb.append("Local time: ");
			sb.append(calendar.getTime().toLocaleString());
			sb.append(", Sleep epoch: ");
			sb.append(sleepEpoch);
			sb.append(", Activity count: ");
			sb.append(activityCount);			
			sb.append(", Activity sum: ");
			sb.append(epochActivitySum);
			sb.append(", Cole sleep score");
			sb.append(coleSleepScore);
			sb.append(",Cole sleep constant: ");
			sb.append(coleConstant);
			sb.append(", User asleep: ");
			sb.append(coleAsleep);
			sb.append(", Sleep episode minutes: ");
			sb.append(sleepEpisodeMinute);
			sb.append(", Total sleep time: ");
			sb.append(totalMinutesAsleep);
			sb.append(", Reminder played this epoch: ");
			sb.append(reminderPlayed);
			sb.append(", User event: ");
			sb.append(checkUserEvent(userEvent));//TODO: convert to enumeration
			sb.append(",");
			
			
			return sb.toString();
		}
		
		public String toHTMLString(){
			
			
			
			
			
			
			
			StringBuilder sb = new StringBuilder(128);
			
			sb.append("Local time: ");
			sb.append(calendar.getTime().toLocaleString());
			sb.append("<br>Sleep epoch: ");
			sb.append(sleepEpoch);
			sb.append("<br>Magnitude Activity count: ");
			sb.append(magnitudeActivityCount);
			
			sb.append("<br>Magnitude Activity sum: ");
			sb.append(magnitudeActivitySum);
			sb.append("<br>X Axis Activity count: ");
			sb.append(xActivityCount);
			sb.append("<br>X Axis Activity Sum: ");
			sb.append(xActivitySum);
			
			sb.append("<br>Y Axis Activity count: ");
			sb.append(yActivityCount);
			sb.append("<br>Y Axis Activity Sum: ");
			sb.append(yActivitySum);
			
			sb.append("<br>Z Axis Activity count: ");
			sb.append(zActivityCount);
			sb.append("<br>Z Axis Activity Sum: ");
			sb.append(zActivitySum);
						
			sb.append("<br>Cole sleep score");
			sb.append(coleSleepScore);
			sb.append("<br>Cole constant");
			sb.append(coleConstant);
			
			sb.append("<br>User asleep: ");
			sb.append(coleAsleep);
			sb.append("<br>Sleep episode minutes: ");
			sb.append(sleepEpisodeMinute);
			sb.append("<br>Total sleep time: ");
			sb.append(totalMinutesAsleep);
			sb.append("<br>Reminder played this epoch: ");
			sb.append(reminderPlayed);
			sb.append("<br>User event: ");
			sb.append(userEvent);
			
			return sb.toString();
		}
		public String toHTMLTableString(){
			StringBuilder sb = new StringBuilder(128);
			
			sb.append("<br>Local time: ");sb.append(calendar.getTime().toLocaleString());
			
			sb.append("<table><tr>");
			//sleep data row
			
			sb.append("<td>");sb.append("Time in Bed: ");sb.append(sleepEpoch);
			sb.append(" min</td>");
			;
			sb.append("<td>");	sb.append("Total sleep: ");
			sb.append(totalMinutesAsleep);sb.append(" min</td>");
			
			sb.append("<td>");sb.append("Sleep episode: ");
			sb.append(sleepEpisodeMinute);sb.append(" min</td>");
			
			sb.append("<td>");sb.append("Sleep efficiency: ");
			sb.append(historyStatistics.getSleepEfficiencyString());sb.append("</td>");
						
			
			sb.append("</tr>");
			
			sb.append("<tr>");
			sb.append("<td></td>");
			sb.append("<td>Total </td>");
			sb.append("<td>X Axis</td>");
			sb.append("<td>Y Axis</td>");
			sb.append("<td>Z Axis</td>");
			sb.append("</tr>");
		
			sb.append("<tr>");
			sb.append("<td>");sb.append("Activity count:");sb.append("</td>");
			sb.append("<td>");sb.append(activityCount);sb.append("</td>");
			sb.append("<td>");sb.append(xActivityCount);sb.append("</td>");
			sb.append("<td>");sb.append(yActivityCount);sb.append("</td>");
			sb.append("<td>");sb.append(zActivityCount);sb.append("</td>");
			sb.append("</tr>");
			
			sb.append("</table>");	
			sb.append("</br>Accelerometer Accuracy: "+accelerometerAccuracy);
			sb.append(" processed events: "+this.eventCount);
			sb.append("<table>");
			sb.append("<tr>");
			sb.append("<td>Cole sleep score: ");
			if(sleepEpoch<6 &&coleSleepScore==1){
				sb.append("N/A");sb.append("</td>");
			}else{
				sb.append(String.format("%.4f", coleSleepScore));sb.append("</td>");
			}
			
			sb.append("<td>Cole constant: ");
			sb.append(String.format("%.6f", coleConstant));sb.append("</td>");
			
			sb.append("<td>User asleep: ");
			sb.append(coleAsleep);sb.append("</td>");
			
			sb.append("<td>Reminder played this minute: ");
			sb.append(reminderPlayed);;sb.append("</td>");			
			sb.append("</tr>");
			
			sb.append("<tr>");
			sb.append("<td>User Event this min: ");sb.append(checkUserEvent(userEvent));sb.append("</td>");
			sb.append("<td>Total User Events: ");sb.append(historyStatistics.getNumberOfUserEvents());sb.append("</td>");
			sb.append("<td>Number of Awakenings: ");sb.append(historyStatistics.getNumberOfAwakenings());sb.append("</td>");
			sb.append("</tr>");
			sb.append("</table>");
			sb.append("Overall Audio Level Sum: "+audioLevel);
			sb.append("; Audio Level Peakedness (sudden noises): "+audioLevelKurtosis);
			sb.append("<HR WIDTH=80%>");
			//sb.append("</br></br>");
			return sb.toString();
		}
	 
		
		public static String checkUserEvent(int eventCode){
			switch(eventCode){
			case USER_EVENT_DREAM: return "Dream";
			
			case USER_EVENT_NO_DREAM: return "No Dream";
			case USER_EVENT_AWAKE: return "Awake";
			case USER_EVENT_LUCID_DREAM: return "Lucid Dream";
			case NO_EVENT: return "No Event";
			case USER_EVENT_NOT_RECOGNIZED: return "Screen Interaction";
			default: return "Undefined Event";
			}
		}
		
	public double getEpochActivitySum() {
		return epochActivitySum;
	}
	public void setEpochActivitySum(double epochActivitySum) {
		this.epochActivitySum = epochActivitySum;
	}
	public int getEventCount() {
		return eventCount;
	}
	public void setEventCount(int eventCount) {
		this.eventCount = eventCount;
	}
	public int getSleepEpoch() {
		return sleepEpoch;
	}
	public void setSleepEpoch(int sleepEpoch) {
		this.sleepEpoch = sleepEpoch;
	}
	public Calendar getCalendar() {
		return calendar;
	}
	public void setCalendar(Calendar calendar) {
		this.calendar = calendar;
	}
	
	public int getActivityCount() {
		return activityCount;
	}
	public void setActivityCount(int activityCount) {
		this.activityCount = activityCount;
	}
	

	public double getColeSleepScore() {
		return coleSleepScore;
	}
	public void setColeSleepScore(float coleSleepScore) {
		this.coleSleepScore = coleSleepScore;
	}
	public double getColeConstant() {
		return coleConstant;
	}
	public void setColeConstant(float coleConstant) {
		this.coleConstant = coleConstant;
	}
	public boolean isColeAsleep() {
		return coleAsleep;
	}
	public void setColeAsleep(boolean coleAsleep) {
		this.coleAsleep = coleAsleep;
	}
	public int getSleepEpisodeMinute() {
		return sleepEpisodeMinute;
	}
	public void setSleepEpisodeMinute(int sleepEpisodeMinute) {
		this.sleepEpisodeMinute = sleepEpisodeMinute;
	}

	
	public int getTotalMinutesAsleep() {
		return totalMinutesAsleep;
	}
	public void setTotalMinutesAsleep(int totalMinutesAsleep) {
		this.totalMinutesAsleep = totalMinutesAsleep;
	}
	public int getUserEvent() {
		return userEvent;
	}
	public void setUserEvent(int userEvent) {
		this.userEvent = userEvent;
	}
	public boolean isReminderPlayed() {
		return reminderPlayed;
	}
	public void setReminderPlayed(boolean reminderPlayed) {
		this.reminderPlayed = reminderPlayed;
	}



	
	
	private int xActivityCount,yActivityCount,zActivityCount,magnitudeActivityCount;
	private double xActivitySum,yActivitySum,zActivitySum, magnitudeActivitySum;
	
	double magnitudeFiltered =10F;
	double magnitude;

	
	

	/**
	 * @return the history
	 */
	public SleepStatistics getHistoryStatistics() {
		return historyStatistics;
	}


	/**
	 * @param history the history to set
	 */
	public void setHistoryStatistics(SleepStatistics historyStatistics) {
		this.historyStatistics = historyStatistics;
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


	public void setYActivitySum(double activitySum) {
		yActivitySum = activitySum;
	}


	public double getZActivitySum() {
		return zActivitySum;
	}


	public void setZActivitySum(double activitySum) {
		zActivitySum = activitySum;
	}


	public double getMagnitudeActivitySum() {
		return magnitudeActivitySum;
	}


	public void setMagnitudeActivitySum(double magnitudeActivitySum) {
		this.magnitudeActivitySum = magnitudeActivitySum;
	}


	public String getAccelerometerAccuracy() {
		return accelerometerAccuracy;
	}


	public void setAccelerometerAccuracy(String accelerometerAccuracy) {
		this.accelerometerAccuracy = accelerometerAccuracy;
	}


	public float getAudioLevel() {
		return audioLevel;
	}


	public void setAudioLevel(int audioLevel) {
		this.audioLevel = audioLevel;
	}


	public float getAudioLevelKurtosis() {
		return audioLevelKurtosis;
	}


	public void setAudioLevelKurtosis(int audioLevelKurtosis) {
		this.audioLevelKurtosis = audioLevelKurtosis;
	}


	public int getAudioPower() {
		return audioPower;
	}


	public void setAudioPower(int audioPower) {
		this.audioPower = audioPower;
	}


	public int getAudioPowerKurtosis() {
		return audioPowerKurtosis;
	}


	public void setAudioPowerKurtosis(int audioPowerKurtosis) {
		this.audioPowerKurtosis = audioPowerKurtosis;
	}
	

	
}
