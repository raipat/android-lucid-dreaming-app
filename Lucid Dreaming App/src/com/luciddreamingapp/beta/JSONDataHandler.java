package com.luciddreamingapp.beta;

import org.json.JSONObject;

import com.luciddreamingapp.beta.util.JSONLoader;

public class JSONDataHandler {
	
	private JSONObject json1;
	private JSONObject json2;
	private JSONObject json3;
	
	 
	int temp;
	JSONLoader loader;
	
	public JSONDataHandler(){
		
		loader = new JSONLoader();
	}

	/*
	public JSONObject getJSON1(){
		return json1;
	}
	public JSONObject getJSON2(){
		/*
		loader.getJSONArray();
		JSONObject temp = loader.getData();
		System.out.println(temp);
		
		return temp;
		
		return json2;
	}

*/
	public JSONObject getJson1() {
		return json1;
	}


	public void setJson1(JSONObject json1) {
		this.json1 = json1;
	}


	public JSONObject getJson2() {
		return json2;
		/*try{return new JSONObject("{\"data\":{\"2\":\"180.15108\",\"1\":\"143.91907\",\"0\":\"837.72186\"}}");}
		catch (Exception e){
			return null;
		}*/
		//return json2;
	}


	public void setJson2(JSONObject json2) {
		this.json2 = json2;
	}

	public JSONObject getJson3() {
		return json3;
	}

	public void setJson3(JSONObject json3) {
		this.json3 = json3;
	}
	
	
		
}
