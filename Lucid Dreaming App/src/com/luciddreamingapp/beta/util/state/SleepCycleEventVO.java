package com.luciddreamingapp.beta.util.state;

import com.luciddreamingapp.beta.util.MorseCodeConverter;

public class SleepCycleEventVO {
	//reminder delivery modes. Controls when the reminder is delivered
	public static final int REMINDER_DELIVERY_MODE_REM_START  = 0;//wbtb, wild
	public static final int REMINDER_DELIVERY_MODE_MOVEMENT  = 1;//deild, dild
	public static final int REMINDER_DELIVERY_MODE_REM_END  = 2;//dream recall
	
	public static final CharSequence[] choices = {"At Event Start (WILD, WBTB)","After Movement (DILD, DEILD)",  "At Event End (Dream recall)"};
	
	
	//indicates if the user wants a reminder for this event
	public boolean reminderSet = false;
	//TODO implement reminder type
	//
	public int startMinute = 0;
	//
	public int duration = 0;
	//
	public int nextEventDelayMinute = 0;
	
	public String reminderFilepath = "/sdcard/Recordings/lucid.mp3";
	
	public boolean useVoiceReminder = true;
	public boolean useVibrateReminder = false;
	public boolean useStrobe = false;
	
	//vibrate this message in morse code
	public String vibrateMessage = "morse code";
	public String flashMessage = "morse code";
	
	public int vibrateDotDuration =120;
	public int flashDotDuration = 360;
	
	public int deliveryMode = REMINDER_DELIVERY_MODE_MOVEMENT;
	
	public SmartTimerState state =null;
	
	/**Intended to represent a sleep cycle event. A night is a progression of sleep cycle events
	 * 
	 * @param startMinute when the event starts
	 * @param duration how long the event lasts
	 * @param nextEventDelayMinute how long after the end of this event till the start of the new event.
	 */
	public SleepCycleEventVO(){
		
	}
	
	public SleepCycleEventVO(int startMinute, int duration,int nextEventDelayMinute,SmartTimerState state ){
		this.startMinute = startMinute;
		this.duration=duration;
		this.nextEventDelayMinute = nextEventDelayMinute;
		this.state = state;
	}
	
	public static String computeDuration(String message,long speedBase){
		long[] arr =MorseCodeConverter.pattern(message, speedBase);
		long temp = 0;
		for(int i = 0;i<arr.length;i++){
			temp += arr[i];
		}
		
		return String.format(String.format("%02d s", temp/1000));
		
	}
	
	@Override
	public String toString(){
		StringBuilder sb = new StringBuilder();
		
		sb.append("REM event at [");
		sb.append(String.format("%02d", startMinute/60));
		sb.append(":"+String.format("%02d", startMinute%60)+"] ");
		sb.append(" ("+startMinute+") min ");
		sb.append(" duration: ");
		sb.append(String.format("%02d min;", duration));
		sb.append((reminderSet)?" [Active]\n":" [Not Active]\n");
		
		sb.append((useVoiceReminder)?"Sound: "+reminderFilepath+"\n":"No Sound\n");
		sb.append((useVibrateReminder)?"Vibrate: \""+vibrateMessage+"\" [" +
				computeDuration(vibrateMessage,vibrateDotDuration)+" @"+vibrateDotDuration+"ms]\n":"No vibration\n");
		
		sb.append((useStrobe)?"Light: \""+this.flashMessage+"\" [" +
				computeDuration(flashMessage,flashDotDuration)+" @"+flashDotDuration+"ms]":"No Light ");
		sb.append("\n"+choices[deliveryMode]);
		
		
		return sb.toString();
	}
	
}
