package com.luciddreamingapp.beta.util.analysis;

public class AnalysisDataPoint {

	public int ep = 0;//sleep minute
	public int st = 0; //special 12-12 timestamp in minutes
	public int ac = 0;//activity count
	public int ss = 0;
	public int ts = 0;//total sleep time
	public int al = 0;//audio level
	public int ak = 0;//audio kurtosis
	public int ue = 0;//user event int
	public int s = 0;//sound reminder
	public int v = 0;//vibration duration ms
	public int l = 0;//light duration ms
	
	@Override
	public String toString(){
		StringBuilder sb = new StringBuilder();
		sb.append("epoch: ");
		sb.append(ep);sb.append("\n");
		
		sb.append("normalized minute of 12-12 day: ");
		sb.append(st);sb.append("\n");
		
		sb.append("activity count: ");
		sb.append(ac);sb.append("\n");
		
		sb.append("totalSleep: ");
		sb.append(ts);sb.append("\n");
		
		sb.append("audio level: ");
		sb.append(al);sb.append("\n");
		
		sb.append("audio kurtosis: ");
		sb.append(ak);sb.append("\n");
		
		sb.append("user event: ");
		sb.append(ue);sb.append("\n");
		sb.append("sleep score: "+ss);
		return sb.toString();
	}
	
}
