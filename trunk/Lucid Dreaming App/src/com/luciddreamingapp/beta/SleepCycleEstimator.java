package com.luciddreamingapp.beta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.math.stat.descriptive.DescriptiveStatistics;
import org.json.JSONArray;
import org.json.JSONObject;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.preference.PreferenceManager;
import android.util.Log;

import com.luciddreamingapp.beta.util.SleepCycleVO;

public class SleepCycleEstimator {

	private static final boolean D = false;
	private static final String TAG = "LD App Sleep Cycle Estimator";
	
	private static int variance = 25;
	private static int guess = 95;
	private static int stDev =5;
	
	private static SimpleFilter kalmanFilter;
	private static Context context;
	
	public static void analyzeSleepCycles(List<SleepCycleVO> sleepCycles,boolean updatePreferences){
		
		
		if(sleepCycles.size()>0){
			DescriptiveStatistics statSleepCycle = new DescriptiveStatistics();
			for(SleepCycleVO vo: sleepCycles){
				int duration = vo.getSleepCycleDuration();
				if(duration>80 && duration<120){
				System.out.println("Sleep cycle:"+ vo.sleepCycleNumber +" duration: "+vo.getSleepCycleDuration());
				statSleepCycle.addValue(vo.getSleepCycleDuration());
				}
			}
		
			stDev = (int)Math.max(1, Math.floor(statSleepCycle.getStandardDeviation()));
			if(D)Log.e(TAG,"stDev: "+stDev);
			guess = (int)Math.ceil(statSleepCycle.getMean());
			if(D)Log.e(TAG,"guess: "+guess);
			variance =stDev*stDev; 
				
			
			if(updatePreferences){
				SharedPreferences prefs = PreferenceManager
		         .getDefaultSharedPreferences(context);
				Editor editor = prefs.edit();
				 
			  
			editor.putInt("sleep_cycle_standard_deviation", stDev);
			editor.putInt("sleep_cycle_duration", guess);
				
				editor.commit();
				editor = null;
				
				}
				
			//TODO update preferences
			
			
//				if(D){
//					Log.e(TAG, "Mean SS: "+statSleepCycle.getMean() );
//					Log.e(TAG, "Max SS: "+statSleepCycle.getMax() );
//					Log.e(TAG, "Min SS: "+statSleepCycle.getMin() );
//					Log.e(TAG, "StDev SS: "+statSleepCycle.getStandardDeviation() );
//					Log.e(TAG, "Variance SS: "+statSleepCycle.getVariance() );
//				}
				
				
//				List<SleepCycleVO> estimatesList = new ArrayList<SleepCycleVO>();
				
				for(int i = 1;i<11;i++){
				for(SleepCycleVO vo: sleepCycles){
					int duration = vo.getSleepCycleDuration();
					//avoid outliers due to app closing or some other glitch
					if(duration>80 && duration<120){
					
						//process sleep cycles in order
						if(vo.sleepCycleNumber==i){
					if(D)System.out.print("SS: "+i+" ");		
						System.out.print("Observed: "+vo.getSleepCycleDuration());
						kalmanFilter.predict();
					if(D)	System.out.print("Predicted : "+
								kalmanFilter.getState().get(0,0));
						
						InMotionCalibration.updateFilter(kalmanFilter, vo.getSleepCycleDuration(), variance);
						}
					
					
					
				if(D)System.out.println();
					}//end if
				}//end inner for
				}//end outer for
		}
		
		
		
		
		
	}
	
	
	public static void initialize(Context context){
		SleepCycleEstimator.context = context;
		if(context!=null){
		SharedPreferences prefs = PreferenceManager
        .getDefaultSharedPreferences(context);
		
		stDev = prefs.getInt("sleep_cycle_standard_deviation", 5);
		variance = stDev*stDev;
		guess = prefs.getInt("sleep_cycle_duration", 95);
		
		kalmanFilter = new SimpleFilter();
		InMotionCalibration.setupFilter(kalmanFilter, stDev,guess);
		}else{throw new UnsupportedOperationException("pass in the app context before calling Initialize()");}
		
		
		if(D)System.out.println("stDev "+stDev);
		if(D)System.out.println("variance "+variance);
		if(D)System.out.println("guess "+guess);
	}
	
	
	
	
	
	public static List<SleepCycleVO> establishSleepCycles(JSONObject night,int nightCounter){
		List<SleepCycleVO> list = new ArrayList<SleepCycleVO>();
		
		int counter = 0;
		int nightDuration =0;
		
		try{nightDuration = night.getInt("totalTimeInBed");}catch(Exception e){if(D)e.printStackTrace();}
		
		
		
		
		if(nightDuration>0){
		
			
			
			
		try{
			JSONArray coleSleepScore = night.getJSONArray("coleSleepScore");
			JSONArray activityCount = night.getJSONArray("xyzActivityCount"); //activity is single spikes
			int sleepOnsetLatency = night.getInt("sleepOnsetLatency");
			System.out.println(createHypnogram(sleepOnsetLatency));
			if(D)System.out.println("sol : "+sleepOnsetLatency);
			if(D)System.out.println("night duration: "+nightDuration);
			
			
			//Find the max number of sleep cycles
			//last sleep cycle may not be complete if a person awakens before then
			int maxSleepCycles = (int)Math.ceil((nightDuration-sleepOnsetLatency)/guess);
			
			
			int sleepCycleCounter = 1;
			SleepCycleVO sleepCycle;
			
			//TODO implement backwards search
			
			//Used to step through the sleep score values
			int sleepCycleStart = sleepOnsetLatency;			
			int sleepCycleEnd = sleepCycleStart+guess-3*stDev;
			
			if(D)Log.e(TAG,sleepCycleStart+"");
			if(D)Log.e(TAG,sleepCycleEnd+"");
			
			HashMap<Integer,Double> map = new HashMap<Integer,Double>();
			DescriptiveStatistics statStart = new DescriptiveStatistics(15);
			try{
			if(D)System.out.println(night.get("filename"));
			}catch(Exception e){}
			
			JSONArray dreams = new JSONArray();
			JSONArray lucidDreams = new JSONArray();
			JSONArray userEvents = new JSONArray();
			JSONArray awake = new JSONArray();
			boolean[]dreamsArray = new boolean[nightDuration];
			Arrays.fill(dreamsArray, false);
			try{
				//convert from a timestamp to a minute since the time user went to sleep
				long firstTimeStamp = coleSleepScore.getJSONArray(0).getLong(0);
				dreams = night.getJSONArray("userEventDream");
				for (int i = 0; i<dreams.length();i++){
					if( (Integer)(dreams.getJSONArray(i).get(1))>0){
						long timestamp = (Long)(dreams.getJSONArray(i).get(0));
						dreamsArray[(int)((timestamp-firstTimeStamp)/60000)]=true;
					}
				}
				
				lucidDreams = night.getJSONArray("userEventLucidDream");
				userEvents = night.getJSONArray("userEventAwake");
				awake = night.getJSONArray("userEventDream");
				
				
			}catch(Exception e){}
			System.out.println("DreamsArray");
			for(int i =0;i<dreamsArray.length;i++){
				if(dreamsArray[i]){System.out.println("Dream at : "+i);}
			}
			
			//process the night and find sleep cycles
			//avoid index out of bounds exceptions
			counter =sleepOnsetLatency;
			
			double[]firstPass= new double[nightDuration];
			Arrays.fill(firstPass, 0);
			double[] secondPass=new double[nightDuration];
			Arrays.fill(secondPass,0);
			
			double sleepScore=0;
			for(int i = counter+10;i<nightDuration;i++){
				
				//iterate from -10 to +10 minutes from the minute examined
				//this is the average deviation of a sleep cycle 90-110 minutes from 100
				for(int j =i-10;j<j+10;j++){
					sleepScore = coleSleepScore.getJSONArray(j).getDouble(1);
					if(sleepScore>=1){
						firstPass[i] +=sleepScore;
					}
				}
				for(int j = i+11;j<j+40;j++){
					
				}
				
				
				
				
			}
			
			
			while(counter<nightDuration&& nightDuration>=4){
				
				
				
				
							
				
				double maxSleepScore = 1;
				int maxActivity = 0;
				int peakMinute = 0;
				 sleepScore=0;
				int activity=0;
				for(int i = (int)(nightDuration*0.25);i<nightDuration*0.75;i++){
					
					
					
					if(dreamsArray[i]){
						
						//look for the peaks 10 minutes before the dream
						for(int j = i;j>i-10;j--){
							sleepScore = coleSleepScore.getJSONArray(j).getDouble(1);
							activity = activityCount.getJSONArray(j).getInt(1);
							if(sleepScore>=maxSleepScore){
							maxSleepScore = sleepScore;
							peakMinute = j;
						
						}
							
						}
						
					}
									
				}
				//remember the max
				if(maxSleepScore >=1){
							
				map.put(peakMinute,maxSleepScore);
					}
				
				if(D){System.out.println("peakMinute: "+peakMinute+" maxSleepScore: "+maxSleepScore);
				System.out.println("["+(int)(peakMinute/60)+":"
						+(peakMinute%60)+" "+maxSleepScore+"]");
				}
				maxActivity = 1;
				//within 85 and 115 of the max peak we must find another peak.
				for(int i = peakMinute+85; i<peakMinute+115;i++){
					
						if(dreamsArray[i]){
						
						//look for the peaks 10 minutes before the dream
						for(int j = i;j>i-10;j--){
							sleepScore = coleSleepScore.getJSONArray(j).getDouble(1);
							activity = activityCount.getJSONArray(j).getInt(1);
							if(sleepScore>=maxSleepScore){
							maxSleepScore = sleepScore;
							peakMinute = j;
						
						}
							
						}
						
					}
					
					
				}
				
				
				
				
				if(D)break;
				try{
					Set<Integer> set = map.keySet();
				int[] temp = new int[set.size()];
				int count = 0;
				for(Integer i: set){
					temp[count] = i;
					count++;
				}
						Arrays.sort(temp);
				System.out.println(Arrays.toString(temp));
				
		
					if(D){
						//print out each of the candidates using 
					for(int i = 0;i<temp.length;i++){	
				//	System.out.println("["+entry.getKey()+" "+entry.getValue()+"]");
					//Hour: minute, value
					System.out.println("["+(int)(temp[i]/60)+":"
							+(temp[i]%60)+" "+map.get(temp[i])+"]");
					}
			
				
					}
				
				
			
				
				for(int i = 0;i<temp.length-1;i++){
					for(int j = i;j<temp.length;j++){
						System.out.println("Difference: "+(temp[j]-temp[i]));
					}
					
				}}catch(Exception e){if(D)e.printStackTrace();}
							
				if(D)System.out.println("*******************************************");
				if(D)break;
				
				
				
				sleepCycle = new SleepCycleVO(sleepCycleCounter++);
			
				//for the first x sleep cycles, find a better starting point
//				if(sleepCycleCounter==2){
//					
//					//Sleep onset latency is after 5 minutes of consecutive uninterrupted sleep
//					//examine the next 5 minutes after sleep onset latency for the baseline kurtosis
//					for(int i=sleepCycleStart;i<sleepCycleStart+6;i++){
//						if(i>nightDuration){break;}//avoid out of bounds exceptions
//						try{
//							//get sleep scores and add to statistics
//						double sleepScore= coleSleepScore.getJSONArray(i).getDouble(1);
//						statStart.addValue(sleepScore);
//						}catch(Exception e){if(D)e.printStackTrace();}
//					}	
//					//kurtosis is a value of how much single large peaks contribute to standard deviation
//					//high kurtosis indicates presence of very large peaks, which may indicate significant movement
//					//in this case, reset the sleep cycle start to the occurence of the movement
//					if(statStart.getN()>0){
//					double tempKurtosis = statStart.getKurtosis();
//					
//					for(int i = sleepCycleStart+6;i<sleepCycleStart+16;i++){
//						if(i>nightDuration){break;}
//						
//						try{
//							//add value, compute kurtosis, remember if it is larger than expected
//							statStart.addValue(coleSleepScore.getJSONArray(i).getDouble(1));
//							if( statStart.getKurtosis()>tempKurtosis&&statStart.getKurtosis()>3){
//								tempKurtosis = statStart.getKurtosis();
//								sleepCycleStart = i;	
//								sleepCycleEnd = sleepCycleStart+85;
//								
//							}statStart.removeMostRecentValue(); 
//							
//						}catch(Exception e){}
//						
//					}
//					}else{if(D)Log.e(TAG, "Not enough values in the night to adjust sleep cycle start");}
//				}
				
				
			//	double maxSleepScore = 2; //max sleep score 1 is awake
				boolean foundCycle = false;
				
				//search between 85 and 115 minutes
				for(int i = sleepCycleEnd;(i<sleepCycleEnd+6*stDev);i++){
					counter = i;
					if(i>=nightDuration){break;}
					try{
						//extract the sleep score
						 sleepScore = coleSleepScore.getJSONArray(i).getDouble(1);
						
						//find max activity over the interval in question and make it the end of the sleep cycle
						if(sleepScore>=maxSleepScore){
							maxSleepScore = sleepScore;
							sleepCycleEnd = i;
							//if at least one candidate point is present
							foundCycle = true;}
						}catch(Exception e){if(D)e.printStackTrace();}
						
			}
				if(!foundCycle){
					sleepCycleEnd= sleepCycleEnd+3*stDev;//set it back to average
				}
				//remember this sleep cycle
				sleepCycle.sleepCycleStartMinute = sleepCycleStart;
				sleepCycle.sleepCycleEndMinute = sleepCycleEnd;
				list.add(sleepCycle);
				if(D) System.out.println(sleepCycle);
				
				sleepCycleStart = sleepCycleEnd+5; //+findSleepCycleOffset() //TODO implement compensating for falling asleep.
				sleepCycleEnd = sleepCycleStart+guess-3*stDev;
		
		
		
		}//end while
				}//end try
		//TODO find own SOL
			catch(Exception e){	if(D)e.printStackTrace(); }
					
		
		}//end if
		
		
		
		return list;
		
		
		
		
	}
	class CandidateTuple{
		double value;
		int position;
		
	}
	
	class sleepCycleSet {
		//a list of list of Double
		List<List<Double>> candidates = new ArrayList<List<Double>>();
		
		
		
		
	}
	

	
	
	public double getVariance() {
		return variance;
	}



	public void setVariance(int variance) {
		SleepCycleEstimator.variance = variance;
	}



	public static int getGuess() {
		return guess;
	}



	public static void setGuess(int guess) {
	SleepCycleEstimator.guess = guess;
	}



	public static void update(double observedValue){
		InMotionCalibration.updateFilter(kalmanFilter, observedValue, variance);
		//updates the model
		
	}
			
	public static void predict(){
		
		//gets new guess
		kalmanFilter.predict();
		guess = (int)Math.ceil(kalmanFilter.getState().get(0,0));
	}
	
	public static JSONArray createHypnogram(int offset){
		JSONArray json = new JSONArray();
		try{
		json.put(new JSONArray("[0,60]"));
		json.put(new JSONArray("[10,60]"));
		json.put(new JSONArray("[10,40]"));
		json.put(new JSONArray("[20,40]"));
		json.put(new JSONArray("[20,30]"));
		json.put(new JSONArray("[30,30]"));
		json.put(new JSONArray("[30,20]"));
		json.put(new JSONArray("[40,20]"));
		json.put(new JSONArray("[40,10]"));
		json.put(new JSONArray("[90,10]"));
		json.put(new JSONArray("[90,50]"));
		json.put(new JSONArray("[100,50]"));
		
		//2nd sleep cycle		
		json.put(new JSONArray("[100,30]"));
		json.put(new JSONArray("[110,30]"));
		json.put(new JSONArray("[110,20]"));
		json.put(new JSONArray("[120,20]"));
		json.put(new JSONArray("[120,10]"));		
		json.put(new JSONArray("[150,10]"));		
		json.put(new JSONArray("[150,20]"));
		json.put(new JSONArray("[160,20]"));
		json.put(new JSONArray("[160,30]"));
		json.put(new JSONArray("[168,30]"));
		json.put(new JSONArray("[168,50]"));
		json.put(new JSONArray("[192,50]"));
		json.put(new JSONArray("[192,40]"));
		json.put(new JSONArray("[200,40]"));
		
		json.put(new JSONArray("[200,30]"));
		json.put(new JSONArray("[220,30]"));
		json.put(new JSONArray("[220,50]"));
		json.put(new JSONArray("[230,50]"));
		json.put(new JSONArray("[230,40]"));
		json.put(new JSONArray("[240,40]"));
		json.put(new JSONArray("[240,30]"));
		json.put(new JSONArray("[250,30]"));
		json.put(new JSONArray("[250,40]"));
		json.put(new JSONArray("[260,40]"));
		json.put(new JSONArray("[260,50]"));
		json.put(new JSONArray("[300,50]"));
		json.put(new JSONArray("[300,40]"));
		json.put(new JSONArray("[310,40]"));
		json.put(new JSONArray("[310,30]"));
		json.put(new JSONArray("[320,30]"));
		json.put(new JSONArray("[320,40]"));
		json.put(new JSONArray("[340,40]"));
		json.put(new JSONArray("[340,50]"));
		json.put(new JSONArray("[380,50]"));
		json.put(new JSONArray("[380,40]"));
		json.put(new JSONArray("[390,40]"));
		
//		json.put(new JSONArray("[430,40]"));
//		json.put(new JSONArray("[430,50]"));
//		json.put(new JSONArray("[470,50]"));		
//		json.put(new JSONArray("[470,40]"));
//		
//		json.put(new JSONArray("[515,40]"));
//		json.put(new JSONArray("[515,50]"));
//		
//		json.put(new JSONArray("[575,50]"));
//		json.put(new JSONArray("[575,40]"));
		
		//json.put(new JSONArray("[605,50]"));
		//json.put(new JSONArray("[655,50]"));
		
		
//		//my guess
//		json.put(new JSONArray("[410,40]"));
//		json.put(new JSONArray("[410,50]"));
//		json.put(new JSONArray("[450,50]"));
//		json.put(new JSONArray("[450,40]"));
//		json.put(new JSONArray("[480,40]"));
//		json.put(new JSONArray("[510,40]"));
//		json.put(new JSONArray("[510,50]"));
//		json.put(new JSONArray("[550,50]"));
//		
		//json with timestamps in millis as X values
		JSONArray jsonTimestamp = new JSONArray();
		//convert minutes to millis		
		for(int i = 0; i<json.length();i++){
			JSONArray temp = new JSONArray();
			temp.put((json.getJSONArray(i).getInt(0)+offset)*60000);
			temp.put(json.getJSONArray(i).getInt(1)*0.5);
			jsonTimestamp.put(temp);
		}
		return jsonTimestamp;
		}catch(Exception e){}
		return json;
		
	}
	
	
}
