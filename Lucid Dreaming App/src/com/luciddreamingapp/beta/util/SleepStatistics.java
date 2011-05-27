package com.luciddreamingapp.beta.util;

import java.text.NumberFormat;
import java.util.Locale;

public class SleepStatistics {

	
	private int totalMinutesAsleep = 0;
	private int numberOfAwakenings = 0;
	private int sleepOnsetLatency = 0;
	private double sleepEfficiency = 0;
	private int totalTimeInBed = 0;
	private int numberOfUserEvents = 0; 
	
	private int numberOfVoiceReminders = 0;
	private int numberOfDreams = 0;
	private int numberOfLucidDreams = 0; 
	
	private int longestSleepEpisode = 0;

	/**
	 * @return the totalMinutesAsleep
	 */
	public int getTotalMinutesAsleep() {
		return totalMinutesAsleep;
	}

	/**
	 * @param totalMinutesAsleep the totalMinutesAsleep to set
	 */
	public void setTotalMinutesAsleep(int totalMinutesAsleep) {
		this.totalMinutesAsleep = totalMinutesAsleep;
	}

	/**
	 * @return the numberOfAwakenings
	 */
	public int getNumberOfAwakenings() {
		return numberOfAwakenings;
	}

	/**
	 * @param numberOfAwakenings the numberOfAwakenings to set
	 */
	public void setNumberOfAwakenings(int numberOfAwakenings) {
		this.numberOfAwakenings = numberOfAwakenings;
	}

	/**
	 * @return the sleepOnsetLatency
	 */
	public int getSleepOnsetLatency() {
		return sleepOnsetLatency;
	}

	/**
	 * @param sleepOnsetLatency the sleepOnsetLatency to set
	 */
	public void setSleepOnsetLatency(int sleepOnsetLatency) {
		this.sleepOnsetLatency = sleepOnsetLatency;
	}

	/**
	 * @return the sleepEfficiency
	 */
	public double getSleepEfficiency() {
				if(totalTimeInBed>0){
					return (double)totalMinutesAsleep/(double)totalTimeInBed;
				}
				else return 0;
	}
	
	public String getSleepEfficiencyString(){
		NumberFormat percentFormatter;
		String percentOut;
		percentFormatter = NumberFormat.getPercentInstance(new Locale("en_US"));//TODO replace to other locales later
		percentOut = percentFormatter.format(getSleepEfficiency());
		return percentOut;
	}

	/**
	 * @param sleepEfficiency the sleepEfficiency to set
	 */
	public void setSleepEfficiency(double sleepEfficiency) {
		this.sleepEfficiency = sleepEfficiency;
	}

	/**
	 * @return the numberOfUserEvents
	 */
	public int getNumberOfUserEvents() {
		return numberOfUserEvents;
	}

	/**
	 * @param numberOfUserEvents the numberOfUserEvents to set
	 */
	public void setNumberOfUserEvents(int numberOfUserEvents) {
		this.numberOfUserEvents = numberOfUserEvents;
	}

	/**
	 * @return the numberOfDreams
	 */
	public int getNumberOfDreams() {
		return numberOfDreams;
	}

	/**
	 * @param numberOfDreams the numberOfDreams to set
	 */
	public void setNumberOfDreams(int numberOfDreams) {
		this.numberOfDreams = numberOfDreams;
	}

	/**
	 * @return the numberOfLucidDreams
	 */
	public int getNumberOfLucidDreams() {
		return numberOfLucidDreams;
	}

	/**
	 * @param numberOfLucidDreams the numberOfLucidDreams to set
	 */
	public void setNumberOfLucidDreams(int numberOfLucidDreams) {
		this.numberOfLucidDreams = numberOfLucidDreams;
	}

	/**
	 * @return the longestSleepEpisode
	 */
	public int getLongestSleepEpisode() {
		return longestSleepEpisode;
	}

	/**
	 * @param longestSleepEpisode the longestSleepEpisode to set
	 */
	public void setLongestSleepEpisode(int longestSleepEpisode) {
		this.longestSleepEpisode = longestSleepEpisode;
	} 
	
	
	/**
	 * @return the totalTimeInBed
	 */
	public int getTotalTimeInBed() {
		return totalTimeInBed;
	}

	/**
	 * @param totalTimeInBed the totalTimeInBed to set
	 */
	public void setTotalTimeInBed(int totalTimeInBed) {
		this.totalTimeInBed = totalTimeInBed;
	}
	
	

	/**
	 * @return the numberOfVoiceReminders
	 */
	public int getNumberOfVoiceReminders() {
		return numberOfVoiceReminders;
	}

	/**
	 * @param numberOfVoiceReminders the numberOfVoiceReminders to set
	 */
	public void setNumberOfVoiceReminders(int numberOfVoiceReminders) {
		this.numberOfVoiceReminders = numberOfVoiceReminders;
	}

	public String toHTMLTableString(){
		StringBuilder sb = new StringBuilder(128);
		
		sb.append("<table><tr>");

		sb.append("<tr>");
		sb.append("<td>");sb.append("Sleep onset latency: "+sleepOnsetLatency);sb.append(" min.</td>");
		sb.append("<td>");sb.append("Number of awakenings: "+numberOfAwakenings);sb.append("; </td>");
		sb.append("<td>");sb.append("Number of user events: "+numberOfUserEvents);sb.append("; </td>");
		//sb.append("<td>");sb.append("Total time in bed: "+totalTimeInBed);sb.append("</td>");
		

		sb.append("<td>");sb.append("Sleep efficiency: ");sb.append(getSleepEfficiencyString());
		;sb.append("</td>");
		sb.append("</tr>");
		
		sb.append("</table>");	
		
		sb.append("</br><HR WIDTH=80%>");
		//TODO: convert to enumeration
		return sb.toString();
	}
	
	public SleepStatistics deepCopy(){
		
		SleepStatistics copy = new SleepStatistics();
		//10/10 copied
		copy.setLongestSleepEpisode(longestSleepEpisode);
		copy.setNumberOfAwakenings(numberOfAwakenings);
		copy.setNumberOfDreams(numberOfDreams);
		copy.setNumberOfLucidDreams(numberOfLucidDreams);
		copy.setNumberOfUserEvents(numberOfUserEvents);
		copy.setNumberOfVoiceReminders(numberOfVoiceReminders);
		copy.setSleepEfficiency(sleepEfficiency);
		copy.setSleepOnsetLatency(sleepOnsetLatency);
		copy.setTotalMinutesAsleep(totalMinutesAsleep);
		copy.setTotalTimeInBed(totalTimeInBed);
		return copy;
		
	}
	
	
}
