package com.luciddreamingapp.beta;

public interface Interactive {

	public String getSleepStatus();
		
		
	public void setSleepStatus(String message);
	
	public  void startInteraction();
	
	public  void startInteraction(String filepath);
	
	public  void startVibrateInteraction(String message);
	
}
