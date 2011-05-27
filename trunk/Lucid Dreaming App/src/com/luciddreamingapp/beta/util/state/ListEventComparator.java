package com.luciddreamingapp.beta.util.state;

import java.util.Comparator;

class ListEventComparator implements Comparator<SleepCycleEventVO>{
	   
	public ListEventComparator(){
		
	}
    public int compare(SleepCycleEventVO vo1,SleepCycleEventVO vo2){
   	    
    	int startTime1 = vo1.startMinute;
    	int startTime2 = vo2.startMinute;
        
    	if(startTime1>startTime2){
    		return 1;
    	}else if(startTime1<startTime2){
    		return -1;
    	}else{return 0;}
     
    }
   
}