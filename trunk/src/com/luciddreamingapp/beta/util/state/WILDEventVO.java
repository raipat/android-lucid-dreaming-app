package com.luciddreamingapp.beta.util.state;

public class WILDEventVO extends SleepCycleEventVO {
	
	
	

	
	
	WILDEventVO(int startMinute, int duration,int nextEventDelayMinute,SmartTimerState state){
		super( startMinute,  duration, nextEventDelayMinute, state);
		
	}
	
	@Override
	public String toString(){
StringBuilder sb = new StringBuilder();
		
		sb.append("WILD Event at [");
		sb.append(String.format("%02d", startMinute/60));
		sb.append(":"+String.format("%02d", startMinute%60)+"] ");
		sb.append(" ("+startMinute+") min ");
		
		sb.append((reminderSet)?" [Active]\n":" [Not Active]\n");
		
		sb.append((useVoiceReminder)?"Sound: "+reminderFilepath+"\n":"No Sound\n");
		sb.append((useVibrateReminder)?"Vibrate: \""+vibrateMessage+"\" [" +
				computeDuration(vibrateMessage,vibrateDotDuration)+" @"+vibrateDotDuration+"ms]\n":"No vibration\n");
		
		sb.append((useStrobe)?"Light: \""+this.flashMessage+"\" [" +
				computeDuration(flashMessage,flashDotDuration)+" @"+flashDotDuration+"ms]":"No Light ");		
		
		
		return sb.toString();
	}
	
	
}
