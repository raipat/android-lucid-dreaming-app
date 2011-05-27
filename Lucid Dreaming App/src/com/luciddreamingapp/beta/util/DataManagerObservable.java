package com.luciddreamingapp.beta.util;

public interface DataManagerObservable {
	//the kind of events we want to register for
	int LIST_UPDATED= 1;
	int GRAPH_UPDATED= 2;
	int DATA_POINT_UPDATED= 3;
	int DATA_POINT_ADDED= 4;
	
	
	public void addObserver(DataManagerObserver observer, int whichUpdates);
	public void unregisterObserver(DataManagerObserver observer, int whichUpdates);
	public void dataPointAdded(SleepDataPoint epoch);
	public void dataPointUpdated(SleepDataPoint epoch);
	public void graphDataUpdated();
	public void listDataUpdated();
	public void onReset();
	
}
