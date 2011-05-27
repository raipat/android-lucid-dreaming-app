package com.luciddreamingapp.beta.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.luciddreamingapp.beta.LucidDreamingApp;




public class JSONLoader {
JSONArray array ;
JSONArray arrayFromList;
JSONArray arrayFromArray;

	public JSONObject getJSONArray(){
		ArrayList list = new ArrayList();
		try{
		JSONArray arr = new JSONArray();
			for(int i =1000,j=0;i>0;i--,j++){
				JSONArray temp = new JSONArray();
				
				temp.put(j);
				temp.put(i);
				list.add(temp);
			}
			arrayFromList= new JSONArray(list);
			JSONObject json = new JSONObject();
			json.put("data", arrayFromList);
			json.put("label","fromList");
		//	saveData(json.toString());
			//Log.e("JSON Object from list", json.toString());
			return json;
		}catch(Exception e){
			e.printStackTrace();
		}
		return null;
		
	}
	
	public JSONArray getFromList(){
		return arrayFromList;
	}

	public static JSONObject generateData(){
		
		JSONObject json = new JSONObject();
		
		try{
		JSONArray arr = new JSONArray();
			
				
				JSONArray temp = new JSONArray();
				
				temp.put(0);
				temp.put(0);
				
				arr.put(0,temp );
				temp = new JSONArray();
				temp.put(1500);
				temp.put(0);
				
				arr.put(1500,temp );
			
			//Log.e("JSON Array", arr.toString());
			//array = arr;
			json.put("data", arr);
			json.put("label","fromJSONObject");
			//Log.e("JSON Object", json.toString());
			
			return json;
		
		}catch(Exception e){
			e.printStackTrace();
			//do nothing
			return null;
		}
		
	}
	
	public static JSONObject JSONFromHashMap(HashMap map){
		JSONObject data = new JSONObject(map);
		JSONObject temp = new JSONObject();
		try{temp.put("data", data);}
		catch(Exception e){
			return data;
		}
		
		return temp;
		//throw new UnsupportedOperationException("Not Supported yet");
	}
	
	public static String saveJSONData(String jsonString){
		String dirPath = LucidDreamingApp.GRAPH_DATA_LOCATION;
		try{
//		Calendar c = Calendar.getInstance();
//		StringBuilder sb = new StringBuilder();
//		sb.append("Data_");
//		sb.append(c.get(Calendar.YEAR));
//		sb.append("-");
//		sb.append(c.get(Calendar.MONTH+1));
//		sb.append("-");
//		sb.append(c.get(Calendar.DAY_OF_MONTH));
//		sb.append("_");
//		sb.append(c.get(Calendar.HOUR_OF_DAY));
//		sb.append("_");
//		sb.append(c.get(Calendar.MINUTE));
//		sb.append("_");
//		sb.append(c.get(Calendar.SECOND));
//		sb.append(".txt");
		String fileName = SleepDataManager.getInstance().getFileName(); 	
		
		saveJSONData(jsonString,dirPath,fileName);
		
		return dirPath+"/"+fileName+".gzip";}
		catch(Exception e){
			return "ERROR:"+e.getMessage();
		}
	}
	
	public static void saveJSONData(String input, String dirPath,String fileName){
		LogManager.writeToAndZipFile(dirPath, fileName, input);
		
	}
	
	public static JSONObject getZipData(File JSONData) throws IOException{
		
		String JSONString = LogManager.unzipFile(JSONData.getAbsolutePath());
		
		if(JSONString !=null && JSONString.length()>0){
			try{return new JSONObject(JSONString);}
			catch(JSONException e){
				try {
					JSONArray array = new JSONArray(JSONString);
					JSONObject object = new JSONObject();
					object.accumulate("array", array);
					return object;
				} catch (JSONException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
					throw new IOException("Error getting data from file");
				}
				
			}
			
		}else
			throw new IOException("File not found");
		
	}
	
	public static JSONObject getData(File JSONData) throws IOException{
		
		if(JSONData.getName().contains("gzip")){
		return getZipData(JSONData);
			
		}else{
		try{
		
		FileReader reader= new FileReader(JSONData);
		BufferedReader bReader= new BufferedReader(reader,8096);
		String line = "";
		StringBuilder sb = new StringBuilder(1024);
		while((line =bReader.readLine())!=null){
			sb.append(line);
		}
		
		
		bReader.close();
		reader.close();
		
		
		JSONObject temp = new JSONObject(sb.toString());
		return temp;
		}catch(JSONException e){
			//e.printStackTrace();
			throw new IOException("Cannot read file"+JSONData.getName());
			
			
		}
	}
	}
	
	
}
