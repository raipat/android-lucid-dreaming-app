package com.luciddreamingapp.beta.util;

import org.json.JSONObject;

public interface DataManagerObserver {

	//listens to updates to list
	//graph
	//add data points
	
	public void dataPointAdded(SleepDataPoint epoch);
	public void dataPointUpdated(SleepDataPoint epoch);	
	
	
	public void listUpdated(String innerHTML);
		
	public void graphUpdated(JSONObject graphData);
	
	public void dataReset();
}
