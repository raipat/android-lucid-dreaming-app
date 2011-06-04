package com.luciddreamingapp.beta.util;


//inner class to hold values
public class SleepCycleVO {
		
	public SleepCycleVO(int sleepCycleNumber){
		this.sleepCycleNumber = sleepCycleNumber;
	}
	
		public int sleepCycleNumber;
		public	int sleepCycleStartMinute;
		public	int sleepCycleEndMinute;
		
		public	long sleepCycleStartTimestampUTC;
		public	long sleepCycleEndTimestampUTC;
		
		public String toString(){
			return sleepCycleNumber + " ["+sleepCycleStartMinute+" : "+sleepCycleEndMinute+"]";
		}
		
		public int getSleepCycleDuration(){
			return sleepCycleEndMinute -sleepCycleStartMinute ;
		}
		
	}
