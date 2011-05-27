package com.luciddreamingapp.beta.util.analysis;

import java.util.ArrayList;
import java.util.List;

public class AnalysisNight {
	
	public String date = "";
	public String uuid = "";
	public long startTS = 0;//start timestamp
	public long endTS = 0;//end timestamp
	public long tzOffset = 0;//timezone offset
	public int sol = 0;//sol
	public int se = 0;//sleep efficiency
	public int ttib = 0;//total time in bed
	public int tst = 0;//total sleep time up to that minute
	public int el = 60000;//epoch length	
	public int numD =0;//dreams
	public int numDL = 0;//LDs
	public int ue = 0;//user events
	public int numAw = 0;//number of awakenings
	
	
	public  List<AnalysisDataPoint> list =new ArrayList<AnalysisDataPoint>();
	
	public String toString(){
		StringBuilder sb = new StringBuilder();
		sb.append("Date: ");
		sb.append(date);sb.append("\n");
		
		sb.append("uuid: ");
		sb.append(uuid);sb.append("\n");
		
		sb.append("Start TS: ");
		sb.append(startTS);sb.append("\n");
		
		sb.append("End TS: ");
		sb.append(endTS);sb.append("\n");
		
		sb.append("TZ Offset: ");
		sb.append(tzOffset);sb.append("\n");
		
		sb.append("SOL: ");
		sb.append(sol);sb.append("\n");
		
		sb.append("Sleep Efficiency: ");
		sb.append(se);sb.append("%\n");
		
		sb.append("Time in bed: ");
		sb.append(tst);sb.append("\n");
		
		sb.append("SOL: ");
		sb.append(sol);sb.append("\n");
		
		sb.append("#Dreams ");
		sb.append(numD);sb.append("\n");
		
		sb.append("#Lucid Dreams: ");
		sb.append(numDL);sb.append("\n");
		
		sb.append("#User Events: ");
		sb.append(ue);sb.append("\n");
		
		sb.append("#Awakenings: ");
		sb.append(numAw);sb.append("\n");
		
		
		return sb.toString();
	}
	
}
