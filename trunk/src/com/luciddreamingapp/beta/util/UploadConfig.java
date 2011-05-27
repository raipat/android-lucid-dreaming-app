package com.luciddreamingapp.beta.util;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONObject;

import android.os.Environment;

import com.google.gson.Gson;
import com.luciddreamingapp.beta.LucidDreamingApp;

public class UploadConfig {
protected static File configFile;	

protected static UploadConfig instance;
protected static ConfigWrapper wrapper;	

protected UploadConfig(){
	wrapper = new ConfigWrapper();
	
}

	protected UploadConfig(File f){
		
	
			
		try{
		instance = new UploadConfig();
		Gson gson = new Gson();
		JSONObject json = JSONLoader.getData(configFile);
	   wrapper = gson.fromJson(json.toString(), ConfigWrapper.class );
	  
		}catch(Exception e){
			e.printStackTrace();
			instance = new UploadConfig();
			
		}
	}
	
	
	
	public List<String> getFilenames() {
		return wrapper.filenames;
	}



	public void setFilenames(List<String> filenames) {
		wrapper.filenames = filenames;
	}



	public static UploadConfig getInstance(){
		if(instance == null){
			configFile = new File(Environment.getExternalStorageDirectory(),LucidDreamingApp.APP_HOME_FOLDER);
			configFile = new File(configFile,"UploadsList.txt.gzip");
			return new UploadConfig(configFile);}
		else{return instance;}
	}
	
	public static String saveConfig(){
		Gson gson = new Gson();//GSON!!!! saves so much time
		String json=null;
		 json =gson.toJson(wrapper);
		 System.out.println("JSON: "+json);
	return LogManager.writeToAndZipFile(
				LucidDreamingApp.APP_HOME_FOLDER, configFile.getName().replace(".gzip", ""), json	);
		
			
	}
	
	class ConfigWrapper {
		List<String> filenames = new ArrayList<String>();
		
		ConfigWrapper(){
			filenames.add("Data_sample_20110429_0020.txt.gzip");
		}
		
		
	}
	
	
}
