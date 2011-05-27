package com.luciddreamingapp.beta.util.analysis;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import android.util.Log;

import com.google.gson.Gson;
import com.luciddreamingapp.beta.LucidDreamingApp;
import com.luciddreamingapp.beta.util.LogManager;

public class AnalysisTester {
private static final String TAG = "Analyzer";
private static final boolean D = true;
	
	Random r;
	AnalysisDataPoint prev;
	AnalysisDataPoint base;
	
	public AnalysisTester(){
		
	}
	
	public File analyze(){
		r = new Random();
		//try to send a bunch of graphs at once
		Long operation = System.currentTimeMillis();
	
		AnalysisNight night;
		
		night= new AnalysisNight();
		for(int j =0;j<600;j++){
			//fill it up with data
		night.list.add(generateData(new AnalysisDataPoint()));
		}
		night.startTS = r.nextLong();
		night.endTS = r.nextLong();
		night.numAw =r.nextInt(20);
		night.numD = r.nextInt(5);
		night.numDL =r.nextInt(3);
		night.se =r.nextInt(100);
		night.sol = r.nextInt(30);
		night.tst =r.nextInt(600);
		night.ttib =500+r.nextInt(100);
		night.ue = r.nextInt(10);
		
		
		
		Long timestamp = System.currentTimeMillis();
		
		
		Gson gson = new Gson();
		String json = gson.toJson(night);
		if(D)Log.e(TAG,json.length()+"" );
		if(D)Log.e(TAG, "convert to json time:"+(System.currentTimeMillis() - timestamp));
		
		timestamp = System.currentTimeMillis();
		String filename = LogManager.writeToAndZipFile(
				LucidDreamingApp.APP_HOME_FOLDER, "night"+r.nextInt(2000)+"_"+r.nextInt(1000)+".txt", json	);
		File f = new File(filename);
		if(D)Log.e(TAG,"exists: "+f.exists()+"size :"+f.length()/1024+" kb");
		if(D)Log.e(TAG, "zip time"+(System.currentTimeMillis() - timestamp));
		
		timestamp = System.currentTimeMillis();
//		try {
//		filename =	LogManager.unzipFile(filename);
//		f = new File(filename);
//		if(D)Log.e(TAG,"exists: "+f.exists()+"size : "+f.length()+" ("+f.length()/1024+") kb");
//		
//		} catch (IOException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
	if(D)	Log.e(TAG, "unzip time"+(System.currentTimeMillis() - timestamp));
		return f;
		//if(D)Log.e(TAG, "time to process 100: "+(System.currentTimeMillis() - operation)/1000+" sec");
	}
		//create 600 analysis data points
		//add to a night, 
		//show size
		//zip, show times
		//show zip size
		//unzip show time
		//unzipped size
		
	
		
		
	
	private AnalysisDataPoint generateData(AnalysisDataPoint a){
		if(prev == null){
		r = new Random();
		a.ac= r.nextInt(5000);
		a.ak = r.nextInt(5);
		a.al = r.nextInt(30000);
		a.ss = 0;
		a.ts = 0;
		a.ue = 0;
		a.ep = 0;
		base = a;
		}else{
			//subsequent data points
			a.ep =prev.ep+1;		
			a.ac = base.ac+r.nextInt(200);
			a.ak = base.ak+r.nextInt(30);
			a.al = prev.al +((r.nextBoolean())?+r.nextInt(4000):-r.nextInt(4000));
			a.ss = prev.ss+((r.nextBoolean()&&r.nextBoolean())?+r.nextInt(10):0);//25% chance to add some ss
			a.ts = prev.ts+((r.nextBoolean()&&r.nextBoolean())?0:1);//25% chance not to raise
			a.ue = ((r.nextInt(100)>98)?r.nextInt(6):0);//2% of user event happening
		}
		prev = a;
		return a;
	}
	
	class TestBundle{
		List<AnalysisNight> list = new ArrayList();
	}
	
	
}
