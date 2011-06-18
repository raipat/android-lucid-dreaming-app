package com.luciddreamingapp.beta.util.state;



import java.io.File;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ListActivity;
import android.app.TimePickerDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.webkit.WebView;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import com.google.gson.Gson;
import com.luciddreamingapp.beta.EventEditorActivity;
import com.luciddreamingapp.beta.JSONDataHandler;
import com.luciddreamingapp.beta.LucidDreamingApp;
import com.luciddreamingapp.beta.Mp3FileSelector;
import com.luciddreamingapp.beta.R;
import com.luciddreamingapp.beta.util.JSONLoader;
import com.luciddreamingapp.beta.util.LogManager;
import com.luciddreamingapp.beta.util.MorseCodeConverter;
import com.luciddreamingapp.beta.util.SleepAnalyzer;
import com.luciddreamingapp.beta.util.analysis.AnalysisTester;


public class SmartTimerActivity extends ListActivity {
	
	private static final boolean D = false;
	private static final String TAG = "SmartTimerActivity";
	
	public static final int START_TIME_DIALOG_ID =1;
	public static final int DURATION_TIME_DIALOG_ID =2;
	


	public static SleepCycleEventVO sleepCycleEventVO;
	public static WILDEventVO wildEventVO;
	
	
	private static final int MENU_ID_DISABLE =2;
	private static final int MENU_ID_ENABLE =3;
	private static final int MENU_ID_REMOVE =4;
	private static final int MENU_ID_SET_DURATION =5;
	private static final int MENU_ID_SET_REMINDER =6;
	
	private static final int MENU_ID_USE_VOICE_REMINDER =7;
	private static final int MENU_ID_USE_VIBRATE_REMINDER =8;
	
	private static final int MENU_ID_REMINDER_DELIVERY_MODE =9; //play reminder before, on movement or at the end of REM
//	private static final int MENU_ID_REMINDER_ON_MOVEMENT =10;
//	private static final int MENU_ID_REMINDER_ON_END =11;
	
	private static final int MENU_ID_TEST_REMINDER =10; //play reminder before, on movement or at the end of REM
	
	public static final int REQUEST_SELECT_FILE = 1;
	public static final int REQUEST_SELECT_FILE_BUILT_IN = 2;
	
	public static final int REQUEST_EDIT_EVENT = 11;
	
	
	
	
	 private ArrayAdapter<SleepCycleEventVO> listAdapter;
	 private ListEventComparator comparator;
	
	 private  LayoutInflater inflater;
	 private View layout;
	 private EditText messageEditor;
	 
	private ListView list;
		
	 int listPosition;
	private volatile int delayHour = 2;
	private volatile int delayMinute = 2;
	
	private volatile int durationHour = 0;
	private volatile int durationMinute = 10;
	private volatile int eventDuration = durationHour*60 + durationMinute;
	private SleepCycleEventVO currentVO;
	
	public static final int REM_EVENT = 1;
	public static final int WILD_EVENT = 2;
	
	public static int eventType = 1;
	public static int startedFromHistory = 0;
	
	private String configFilepath;
	
	private String mp3reminderFilepath;
	private MediaPlayer mp;
	
	private SmartTimer smartTimer;
	private WebView guiView;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		
		//remove title to save space
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		
		Bundle extras = getIntent().getExtras(); 
    	if(extras !=null)
    	{
    		eventType = extras.getInt("eventType", 1);
    		startedFromHistory = extras.getInt("startedFromHistory");
    		if(startedFromHistory!=0){
    			setResult(Activity.RESULT_OK);
    		}
    	}
		
    	 switch(eventType){
		 case REM_EVENT:
			 setContentView(R.layout.add_timer_layout);
				guiView = (WebView)findViewById(R.id.timer_gui);
				
				
				guiView.getSettings().setJavaScriptEnabled(true);
				guiView.getSettings().setLoadWithOverviewMode(true);
				guiView.getSettings().setUseWideViewPort(true);
				//guiView.getSettings().setBuiltInZoomControls(true);
				guiView.loadUrl("file:///android_asset/html/smart_timer_gui.html");
				
			 
			 break;
		 case WILD_EVENT:
			 setContentView(R.layout.wild_timer_layout);
			 break;
		 default:
			 break;
		 }
    	
    	
		
		 list = (ListView)findViewById(android.R.id.list);
		 listAdapter = new ArrayAdapter<SleepCycleEventVO>(this,R.layout.message);
		 
		 	 
		 setListAdapter(listAdapter);
		 comparator = new ListEventComparator();
		 		 		 
		 registerForContextMenu(getListView());
		 
		 
		 
		 
		 
	        OnItemClickListener listItemListener = new OnItemClickListener() {
	            
				@Override
				public void onItemClick(AdapterView<?> adapterView, View arg1,
						int adapterPosition, long rowID) {
					
					if(eventType == REM_EVENT){
					currentVO = listAdapter.getItem((int)rowID);
					sleepCycleEventVO =currentVO;
					Intent editorIntent = new Intent(SmartTimerActivity.this.getBaseContext(), EventEditorActivity.class);
					startActivityForResult(editorIntent,REQUEST_EDIT_EVENT);
					}else{
						
						currentVO = listAdapter.getItem((int)rowID);
						sleepCycleEventVO =currentVO;
						Intent editorIntent = new Intent(SmartTimerActivity.this.getBaseContext(), EventEditorActivity.class);
						startActivityForResult(editorIntent,REQUEST_EDIT_EVENT);
						
						
//					currentVO = listAdapter.getItem((int)rowID);
//					sleepCycleEventVO =currentVO;
//					delayHour = currentVO.startMinute/60;
//					delayMinute = currentVO.startMinute%60;
//					
//					showDialog(START_TIME_DIALOG_ID);
					}
//					//get the item from the list and remember it for callback
//					//sendShortToast("List item clicked:"+ rowID);
//					
//					//listAdapter.remove(currentVO);
//					
					
				}
			};
			
			 list.setOnItemClickListener(listItemListener);
			 
			 
			//get shared preferences and find the default reminder to assign to REM and WILD events
			 //(Used to upgrade for people who used a previous version of Smart Timer)
				SharedPreferences mySharedPreferences = getSharedPreferences(
		                "LucidDreamingAppPreferences", Activity.MODE_PRIVATE);
			
				 mp3reminderFilepath = mySharedPreferences.getString("reminderFilepath", "/sdcard/Recordings/lucid.mp3");
				 
				
				 configFilepath =this.getSmartTimerConfigFilepath(this); 
			 if(D)Log.e(TAG, "filepath: "+configFilepath);
			 //try to load the values from the json file
			 try{
				 File file =null;
				 switch(eventType){
				 case REM_EVENT:
					 file =new File(configFilepath);
					 break;
				 case WILD_EVENT:
					 configFilepath =this.getWILDTimerConfigFilepath(this); 
					 file =new File(configFilepath);
					 break;
				 default:
					 break;
				 }
				 
				 
			if(file.exists() && file.canRead()){
			JSONObject json =JSONLoader.getData(file);
			
			new GUITask().execute(null);

			Gson gson = new Gson();
			JSONArray eventObjects =json.getJSONArray("eventObjects");
			for(int i =0;i<eventObjects.length();i++){
				try{
					SleepCycleEventVO vo;
					 switch(eventType){
					 
					 case REM_EVENT:
						  vo=gson.fromJson(eventObjects.getString(i), SleepCycleEventVO.class) ;
						 //overwrite the default filepath if an updated one exists
						  if(vo.reminderFilepath.equals("/sdcard/Recordings/lucid.mp3")){
						  vo.reminderFilepath = mp3reminderFilepath;
						  }
						 listAdapter.add(vo);
						 break;
					 case WILD_EVENT:
						 vo =gson.fromJson(eventObjects.getString(i), WILDEventVO.class) ;
						 if(vo.reminderFilepath.equals("/sdcard/Recordings/lucid.mp3")){
							  vo.reminderFilepath = mp3reminderFilepath;
							  }
					
						 listAdapter.add(vo);
						 break;
					 default:
						 break;
					 }				
					
				}catch(Exception e){if(D)e.printStackTrace();}
			}
			}
			 }catch(Exception e){
				 if(D)e.printStackTrace();
				 switch(eventType){
				 
				 case REM_EVENT:
					 	listAdapter.add(new SleepCycleEventVO(1,0,0,null)); 
						listAdapter.add(new SleepCycleEventVO(2,0,0,null)); 
						listAdapter.add(new SleepCycleEventVO(3,0,0,null)); 
						listAdapter.add(new SleepCycleEventVO(4,0,0,null)); 
					 break;
				 case WILD_EVENT:
					 	listAdapter.add(new WILDEventVO(1,0,0,null)); 
						listAdapter.add(new WILDEventVO(2,0,0,null)); 
						listAdapter.add(new WILDEventVO(3,0,0,null)); 
						listAdapter.add(new WILDEventVO(4,0,0,null)); 
						
					 break;
				 default:
					 break;
				 }		
				
			 }
			 
			 listAdapter.sort(comparator);
			 checkForOverlaps();
			 
			 
			  inflater = (LayoutInflater) this.getSystemService(LAYOUT_INFLATER_SERVICE);
		    	


			 mp = new MediaPlayer();
			 
	}

	
	 @Override
	    public void onCreateContextMenu(ContextMenu menu, View v,
	            ContextMenu.ContextMenuInfo menuInfo) {

	        super.onCreateContextMenu(menu, v, menuInfo);
	        

	        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;
	        menu.setHeaderTitle(((TextView) info.targetView).getText());
	       
	        try {
				currentVO =listAdapter.getItem((int)info.id);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	       
	        switch(eventType){
			 
			 case REM_EVENT:
	        menu.add(0, MENU_ID_SET_DURATION, 0, "Set Event Duration"); //fall through and add wild event context options as well
	        menu.add(0, MENU_ID_REMINDER_DELIVERY_MODE, 0, "Reminder timing");
	        
			 case WILD_EVENT:
//			menu.add(0, MENU_ID_SET_REMINDER, 0, "Pick Reminder");
//			menu.add(0, this.MENU_ID_TEST_REMINDER, 0, "Test Current Reminder");
			if(currentVO.useVoiceReminder){
				//switch to vibrate reminder
//				menu.add(0, MENU_ID_USE_VIBRATE_REMINDER, 0, "Switch to Vibrate reminder");
				  
			}else{
				//switch to voice reminder
//				 menu.add(0, MENU_ID_USE_VOICE_REMINDER, 0, "Switch to voice reminder");
				}
			
			
			if(currentVO.reminderSet){
				   menu.add(0, MENU_ID_DISABLE, 0, "Deactivate Event");//possibly collapse into one button by querying the set property	        
			}else{
				menu.add(0, MENU_ID_ENABLE, 0, "Activate Event");
			}
	        
	     
	        menu.add(0, MENU_ID_REMOVE, 0, "Delete Event");
	        default:
	        	break;
	        }
	    }
	 
	 
	 @Override
	    public boolean onContextItemSelected(MenuItem item) {
	        final AdapterView.AdapterContextMenuInfo menuInfo = (AdapterView.AdapterContextMenuInfo)
	                item.getMenuInfo();
	   
	        currentVO =listAdapter.getItem((int)menuInfo.id);
	        switch (item.getItemId()) {
	        	case MENU_ID_SET_DURATION:
	        		//currentVO =listAdapter.getItem((int)menuInfo.id);
	        		durationHour= currentVO.duration/60;
	        		durationMinute = currentVO.duration%60;
	        		
	        		showDialog(DURATION_TIME_DIALOG_ID);
	        		 listAdapter.sort(comparator);
	             	listAdapter.notifyDataSetChanged();
              break;
                
	        	case MENU_ID_SET_REMINDER:
	        		if(currentVO.useVoiceReminder){
	        				pickVoiceReminder();
	        		}else{
	        			
	        			//show a dialog to select when the reminder should be played
		        		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		        		layout = inflater.inflate(R.layout.vibrate_text_dialog,
	                               (ViewGroup) findViewById(R.id.layout_root));
				    	messageEditor = (EditText) layout.findViewById(R.id.edit_message);
		        		//
		        		messageEditor.setText(currentVO.vibrateMessage);
		        		builder.setTitle("Enter a short text to vibrate in morse code")
		        		  .setPositiveButton("Set", new DialogInterface.OnClickListener() {
		   	 	           public void onClick(DialogInterface dialog, int id) {
		   	 	        	 currentVO.vibrateMessage = messageEditor.getText().toString();
		   	 	        	
		   	 	        	 
		   	 	           }
		   	 	       })	 	       
		   	 	       .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
		   	 	           public void onClick(DialogInterface dialog, int id) {
		   	 	        	 dialog.cancel();
		   	 	           }
		   	 	       });
		        		builder.setView(layout);
		        		
		        		AlertDialog alert = builder.create();
		        		alert.show();
	        			
	        			
	        		}
	        	
	        	
	        		 break;
	        		 
	        	case MENU_ID_TEST_REMINDER:
	        		if(currentVO.useVoiceReminder){
	        			testVoiceReminder();
	        		}else{
	        			testVibrateReminder();
	        		}
	        		
	        		break;
	        		
	        	case MENU_ID_USE_VIBRATE_REMINDER:
	        		currentVO.useVoiceReminder = false;
	        		 break;
	        	case MENU_ID_USE_VOICE_REMINDER:
	        		currentVO.useVoiceReminder = true;
	        		 break;
	        		 
	        	case MENU_ID_REMINDER_DELIVERY_MODE:
	        		
	        		
	        		//show a dialog to select when the reminder should be played
	        		AlertDialog.Builder builder = new AlertDialog.Builder(this);
	        		//
	        		
	        		builder.setTitle("When should the reminder be played?");
	        		builder.setItems(SleepCycleEventVO.choices, new DialogInterface.OnClickListener() {
	        		    public void onClick(DialogInterface dialog, int item) {
	        		       
	        		        currentVO.deliveryMode=item;
	        		        listAdapter.notifyDataSetChanged();
	        		    }
	        		});
	        		AlertDialog alert = builder.create();
	        		alert.show();
	        		
	        		break;
                
	            case MENU_ID_ENABLE:
	            	currentVO.reminderSet = true;
	               // listAdapter.getItem((int)menuInfo.id).reminderSet=true;
	               //listAdapter.notifyDataSetChanged();
	                break;
	            case MENU_ID_DISABLE:
	            	currentVO.reminderSet = false;
	            	//listAdapter.getItem((int)menuInfo.id).reminderSet=false;
	               // listAdapter.notifyDataSetChanged();
	                break;
	            case MENU_ID_REMOVE:
	            	SleepCycleEventVO vo =listAdapter.getItem((int)menuInfo.id);
	            	listAdapter.remove(vo);
	               // listAdapter.notifyDataSetChanged();
	                break;
	        }
	       
	        listAdapter.sort(comparator);
        	listAdapter.notifyDataSetChanged();
	        return super.onContextItemSelected(item);
	    }
	
	
	
	
	@Override
	protected void onPrepareDialog(int id, Dialog dialog) {
		
		if(D)Log.e(TAG,"on prepare dialog "+id);
		// TODO Auto-generated method stub
		super.onPrepareDialog(id, dialog);
        if(id ==START_TIME_DIALOG_ID){
        	if(D)Log.e(TAG,"preparing start time dialog ");
        	
		TimePickerDialog timePicker = new TimePickerDialog(this,
				mTimeSetListener, delayHour, delayMinute, true) {
			
        	@Override
        	//Define a method to set meaningful title for the dialog when time changes
			public void onTimeChanged(TimePicker view, int hourOfDay,
					int minute) {
				StringBuilder sb = new StringBuilder();
				sb.append("P Event start time: ");
				sb.append(hourOfDay*60+minute);
				sb.append(" minutes");
				this.setTitle(sb.toString());//refer to the time picker from within itself
				sb = null;

			}
		};
		
		timePicker.updateTime(delayHour, delayMinute );
		((TimePickerDialog)dialog).updateTime(delayHour, delayMinute );
		dialog = timePicker;
		}      else  if(id ==DURATION_TIME_DIALOG_ID){
			if(D)Log.e(TAG,"preparing duration dialog ");
			TimePickerDialog timePicker = new TimePickerDialog(this,
					durationListener, durationHour, durationMinute, true) {
				
	        	@Override
	        	//Define a method to set meaningful title for the dialog when time changes
				public void onTimeChanged(TimePicker view, int hourOfDay,
						int minute) {
					StringBuilder sb = new StringBuilder();
					sb.append("P Event start time: ");
					sb.append(hourOfDay*60+minute);
					sb.append(" minutes");
					this.setTitle(sb.toString());//refer to the time picker from within itself
					sb = null;

				}
			};
			((TimePickerDialog)dialog).updateTime(durationHour, durationMinute );
			timePicker.updateTime(durationHour, durationMinute );
			dialog = timePicker;
			}
        
        

	}




	//called from xml layout
	public void addTimer(View view){
		
		switch(eventType){
		 
		 case REM_EVENT:
			 SleepCycleEventVO temp =new SleepCycleEventVO(0,0,0,null);
			 temp.reminderFilepath = mp3reminderFilepath;
		listAdapter.add(temp);
		break;
		 case WILD_EVENT:
			 WILDEventVO temp2 =new WILDEventVO(0,0,0,null);
			 temp2.reminderFilepath = mp3reminderFilepath;
			 listAdapter.add(temp2);
		
		 break;
		 }
		 listAdapter.sort(comparator);
	}
	
	//called from xml layout
	public void saveEvents(View view){
		if(startedFromHistory==0)
		sendShortToast("Saving Settings");
	
		
		JSONArray objects = new JSONArray();
		Gson gson = new Gson();//GSON!!!! saves so much time
		
		
		for(int i = 0;i<listAdapter.getCount();i++){
			SleepCycleEventVO vo = listAdapter.getItem(i);
			
			String json=null;
			switch(eventType){
			
			 case REM_EVENT:
				 json =gson.toJson(vo);
			break;
			 case WILD_EVENT:
				 json =gson.toJson((WILDEventVO)vo);
			
			 break;
			 }
		
			  // serializes target to Json
			if(D)Log.e(TAG,json.toString());
		
			//save the serialized version of the object
			objects.put(json);
			
		}
		
		JSONObject smartTimerEvents = new JSONObject();
		try{smartTimerEvents.put("version", 1);//to be able to distinguish between different incarnations in the future
			smartTimerEvents.put("eventObjects", objects);
		
//			File filepath = new File(Environment.getExternalStorageDirectory(),new File(LucidDreamingApp.APP_HOME_FOLDER, "SmartTimerConfig.txt").getAbsolutePath());
//			try{
//			//write to json formatted file
//			FileWriter writer = new FileWriter(filepath);
//			BufferedWriter out = new BufferedWriter(writer,8096);
//			JsonWriter jsonWriter = new JsonWriter(writer);
//			jsonWriter.setIndent("    ");//4 spaces
//			jsonWriter.beginObject();
//			jsonWriter.value(smartTimerEvents.toString(4));
//			 jsonWriter.endObject();
//			 jsonWriter.close();
//			 out.close();
//			 writer.close();
			 String filepath= null;
			switch(eventType){
			 
			 case REM_EVENT:
				 filepath =	LogManager.writeToAndZipFile(LucidDreamingApp.APP_HOME_FOLDER,new File(configFilepath).getName().replace(".gzip",""),smartTimerEvents.toString(4));
					
				// filepath =	LogManager.writeToAndZipFile(LucidDreamingApp.APP_HOME_FOLDER, "SmartTimerConfig.txt",smartTimerEvents.toString(4));
			break;
			 case WILD_EVENT:
				 filepath =	LogManager.writeToAndZipFile(LucidDreamingApp.APP_HOME_FOLDER, new File(configFilepath).getName().replace(".gzip",""),smartTimerEvents.toString(4));
			
			 break;
			 }
			
        //zip the history file	
       
      // LogManager.unzipFile(filepath);
      
       	if(D)System.out.println(filepath);
       
        	if(filepath !=null){
        		
        		//try to update the smart timer with the latest events
        		try {
					SleepAnalyzer.getInstance().getSmartTimer().setupEvents(filepath);
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
      	
				if(startedFromHistory==0)		
				sendToast("Saved to:"+filepath);
				 
				}else{
					
				sendToast("Could not save events");
				}
		
	
			
		}catch(JSONException e){if(D)e.printStackTrace();}
		//TODO save as JSON
		
		new GUITask().execute(null);
		
		 if(startedFromHistory!=0){
			  if(D)Log.e(TAG,"finishing");
			  setResult(Activity.RESULT_OK);
			  finish();
		  }
		
	
	}
	
	public void help(View view){
		sendShortToast("Opening online help");
		try{
			 Uri  uri =  Uri.parse(("http://luciddreamingapp.com/help-how-to/configure-smart-timer/"));
			 if(D)Log.e(TAG, uri.toString());
			switch(eventType){
			 
			 case REM_EVENT:
				 //parsing a url with dashes turns them to 
				 uri =  Uri.parse(("http://luciddreamingapp.com/help-how-to/configure-smart-timer/"));
				
				 if(D)Log.e(TAG, uri.toString());
				 
			break;
			 case WILD_EVENT:
				 uri =   Uri.parse(("http://luciddreamingapp.com/help-how-to/configure-wild-timer/"));
				
				 if(D)Log.e(TAG, uri.toString());
			
			 break;
			 }
			startActivity( new Intent( Intent.ACTION_VIEW, uri ) );
		
			
		
		}catch(Exception e){
			if(D)e.printStackTrace();
		}
		//TODO add intent to view help
	}
	
	
    @Override
    protected Dialog onCreateDialog(int id) {
    	if(D)Log.e(TAG,"on create dialog "+id);
        switch (id) {
        case START_TIME_DIALOG_ID:
        	//creates a new 24 hour dialog to pick the delay time between reminders
            TimePickerDialog dialog = new TimePickerDialog(this,
					mTimeSetListener, delayHour, delayMinute, true) {
				
            	@Override
            	//Define a method to set meaningful title for the dialog when time changes
				public void onTimeChanged(TimePicker view, int hourOfDay,
						int minute) {
					StringBuilder sb = new StringBuilder();
					sb.append("New event at: ");
					sb.append(hourOfDay*60+minute);
					sb.append(" minutes");
					this.setTitle(sb.toString());//refer to the time picker from within itself
					sb = null;

				}
			};
            dialog.setTitle("Reschedule reminder"); 
           
           
            return dialog;
        case DURATION_TIME_DIALOG_ID:
        	//creates a new 24 hour dialog to pick the delay time between reminders
        	TimePickerDialog duration = new TimePickerDialog(this,
        			durationListener, durationHour, durationMinute, true) {
				
            	@Override
            	//Define a method to set meaningful title for the dialog when time changes
				public void onTimeChanged(TimePicker view, int hourOfDay,
						int minute) {
					StringBuilder sb = new StringBuilder();
					sb.append("Event duration: ");
					sb.append(hourOfDay*60+minute);
					sb.append(" minutes");
					this.setTitle(sb.toString());//refer to the time picker from within itself
					sb = null;

				}
			};
			duration.setTitle("Set duration"); 
           
           
            return duration;
        }
        return null;
    }
	
	
	 // the callback received when the user "sets" the time in the dialog
    private TimePickerDialog.OnTimeSetListener mTimeSetListener =
        new TimePickerDialog.OnTimeSetListener() {
            public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
              
            	currentVO.startMinute=hourOfDay*60+ minute;
            	
            	currentVO.reminderSet = true;
            	//assign default duration
            	if(currentVO.duration==0){
            		
            		currentVO.duration=eventDuration;
            		
            	switch(eventType){
       			 
       			 case REM_EVENT:
       				currentVO.duration=eventDuration;
       				sendShortToast("Setting event duration to: "+eventDuration);
       				if(!checkForOverlaps()){            		
                		
                		sendToast("New event after "+hourOfDay+" hours and "+minute+" minutes sleep duration");
                   	}
       			break;
       			 case WILD_EVENT:
       				currentVO.duration=1;
       			
       			 break;
       			 }
            		
            		
            		 
            	}
            	
            	 listAdapter.sort(comparator);
            	listAdapter.notifyDataSetChanged();
            	
            	
            	
            	//listAdapter.add(currentVO);
            	//  delayHour = hourOfDay;
           //     delayMinute = minute;
           ///     timerDelaySeconds = minute*60+hourOfDay*60*60;
           //     timerDelayMinutes = minute+hourOfDay*60;
             //  writeLogEntry("Time delay changed to "+timerDelayMinutes,"12");
                //cancel all states and go into the appropriate state
                
            //    parent.reset();
            //    parent.initializeState();
              
                
            }
        };
	
        // the callback received when the user "sets" the time in the dialog
        private TimePickerDialog.OnTimeSetListener durationListener =
            new TimePickerDialog.OnTimeSetListener() {
                public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
                	
                	eventDuration =hourOfDay*60+minute;
                	currentVO.duration = eventDuration;       
                	 listAdapter.sort(comparator);
                	listAdapter.notifyDataSetChanged();
                	if(!checkForOverlaps()){
                		sendShortToast("duration changed to: "+(hourOfDay*60+minute));
                	}
                   
                    
                }
            };
        
        
        
	
	
	
	 protected void sendToast(String message){
	    	Context context = getApplicationContext();
			  Toast toast = Toast.makeText(context, message,Toast.LENGTH_LONG);
			  
			  toast.show();
	    }
	    protected void sendShortToast(String message){
	    	
	    	Toast.makeText(getBaseContext(),
	                message,
	                Toast.LENGTH_SHORT).show();
	    	
	    }
	    
	    /**Checks if there are overlapping event start times present within the list adapter
	     * 
	     */
	    private boolean checkForOverlaps(){
	    	//only if more than 1 event
	    	if(listAdapter.getCount()>1 && eventType!= WILD_EVENT){
	    	for(int i = 0;i<listAdapter.getCount()-1;i++){
	    		//if the next ite
	    		if(listAdapter.getItem(i+1).startMinute<listAdapter.getItem(i).startMinute+listAdapter.getItem(i).duration){
	    			sendToast("Duration of "+listAdapter.getItem(i)+" overlaps next event start");
	    			return true;
	    		}
	    		
	    	}}
	    	return false;
	    }
	    
	   
	  

private void pickVoiceReminder(){

	SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
	boolean useFileSelection = prefs.getBoolean("old_file_selection_pref", false);
	
	
	if(useFileSelection){
		//if the user is having trouble opening MP3s through URIs, use the default file selector
		try{
		Intent selectFileIntent = new Intent(getBaseContext(), Mp3FileSelector.class);
         startActivityForResult(selectFileIntent, REQUEST_SELECT_FILE_BUILT_IN);
		}catch(Exception e){
			if(D)e.printStackTrace();
		}
		
	}else{
		//give the user more freedom to pick any URI
		Intent intent = new Intent("org.openintents.action.PICK_FILE");
    	intent.putExtra("org.openintents.extra.TITLE", "Pick reminder .Mp3, .Mp4 or .Wav file");
    	intent.putExtra("org.openintents.extra.BUTTON_TEXT", "Pick");
    	
    	intent.setData(Uri.parse("file:///sdcard/Recordings"));
    	try{
    		startActivityForResult(intent, REQUEST_SELECT_FILE);
    		//sendShortToast("Opening file manager");
    		}
    	catch(ActivityNotFoundException e){
    	  	
    		
    		sendToast("OI File Manager not found, please use built-in file selector");
    		if(D)e.printStackTrace();
    	}catch(Exception e){
    		sendToast("Exception working with IO File Manager");
    		if(D)e.printStackTrace();
    	}
	}
	                                      	                                      	
	                  
	
}

@Override
public void onActivityResult(int requestCode, int resultCode, Intent data) {
   // if(D) {System.out.println("onActivityResult " + resultCode);}
	if(D)Log.e(TAG,"requestCode: "+requestCode+ " resultCode: "+resultCode);
    switch(requestCode){
    
    case REQUEST_EDIT_EVENT:
    
   // 	new GUITask().execute(null);
    	
    currentVO = sleepCycleEventVO;
    listAdapter.notifyDataSetChanged();
	//  listAdapter.notifyDataSetChanged();
  	 if(D)Log.e(TAG,"Returned from event editor");
  	  
    break;
    
    //using the Mp3FileSelector in the Recordings folder
    	case REQUEST_SELECT_FILE_BUILT_IN:
    		 if(resultCode==Activity.RESULT_OK){	
    				new GUITask().execute(null);
    	try{
    		String reminderFilepath=data.getExtras().getString("filepath");    	
    			Toast.makeText(getBaseContext(),
          "Selected: "+reminderFilepath,
          Toast.LENGTH_SHORT).show();
    			
    	currentVO.reminderFilepath = reminderFilepath;
    	  listAdapter.notifyDataSetChanged();
	            			
    			
    		}catch(Exception e){
    			if(D) e.printStackTrace();
    			//TODO store the file
    		}
    		 }else if(resultCode ==Activity.RESULT_CANCELED){
    			 
    			 try{
             		String message = data.getExtras().getString("message");
             		sendToast(message);}
             		catch(Exception e){
             			if(D) e.printStackTrace();
             			sendToast("File selection cancelled");
             		}
    			 
    		 }
    			break;
    			
    		//using OI File manager and URIs	
    		case REQUEST_SELECT_FILE:
    			 if(resultCode==Activity.RESULT_OK){
    					new GUITask().execute(null);
    				 Uri uri =data.getData();
    				 Toast.makeText(getBaseContext(),
    						 "Selected: "+uri.getPath(),
    						 Toast.LENGTH_LONG).show();
    		//TODO remove after testing
//    		mp = new MediaPlayer();
//    		try{
//        	mp.setDataSource(uri.getPath());
//        	mp.prepare();
//        	mp.start();
//    		}catch(Exception e){
//    			sendToast("could not play "+uri);
//    			if(D)e.printStackTrace();
    				 
//    		}
    				 File f = new File(uri.getPath());
    		if(D)System.out.println("uri String "+uri.toString());
    		if(D)System.out.println("uri Path "+uri.getPath());
    		if(D)System.out.println("file absolute Path "+f.getAbsolutePath());
    				 
    		currentVO.reminderFilepath = f.getAbsolutePath();
    		  listAdapter.notifyDataSetChanged();//update list
    		
    			 }else if(resultCode ==Activity.RESULT_CANCELED){
    				 sendShortToast("File selection cancelled");
    			 }
    			break;
    		
    		
    		
    		}
    
    listAdapter.sort(comparator);
	listAdapter.notifyDataSetChanged();

    	
    }
protected void checkPlaybackSettings(){

	  SharedPreferences prefs = PreferenceManager
       .getDefaultSharedPreferences(getBaseContext());
	  boolean playSoundReminders = prefs.getBoolean("play_sound_pref", false);
	 
		  //check sound playback preferences
	        if(!playSoundReminders){
	        	sendShortToast("\"Play Voice Reminders\" preference not enabled");
	        }
	        //check if current event is active
	        if(!currentVO.reminderSet){
				  sendShortToast("REM Event not active");
			 }	
}
	  protected void testVoiceReminder(){
		  try{
			  checkPlaybackSettings();
		  if( mp!=null && mp.isPlaying()){
		        	try{	
		        		mp.stop();
		        		mp.reset();
		        		mp.release();}catch(Exception e){}		        		
			        	mp = new MediaPlayer();
		        		mp.setDataSource(currentVO.reminderFilepath);
			        	mp.prepare();
			        	mp.start();	
		        	}else{
		        	mp = new MediaPlayer();		        	
		        	mp.setDataSource(currentVO.reminderFilepath);
		        	mp.prepare();
		        	mp.start();			        	
		        	}
		        
		        }catch(Exception e){
		        	sendShortToast("Unable to play file");
		        	if(D)e.printStackTrace();
		        }
		  
		  
	  }
	  
	
	  
	  protected void testVibrateReminder(){
		  if(currentVO.vibrateMessage!=null && !currentVO.vibrateMessage.equals("")){
			  long[] pattern = MorseCodeConverter.pattern(currentVO.vibrateMessage);

	          // Start the vibration. Requires VIBRATE permission
			  sendToast("Vibrating :"+currentVO.vibrateMessage);
	          Vibrator vibrator = (Vibrator)getSystemService(Context.VIBRATOR_SERVICE);
	          vibrator.vibrate(pattern, -1);
	         
		  }else {
			  sendShortToast("No text to vibrate");
		  }
		  
		  checkPlaybackSettings();
		  
	  }
	    
	 public static String getSmartTimerConfigFilepath(Activity parent){
		 SharedPreferences customSharedPreference = parent.getSharedPreferences(
                 "LucidDreamingAppPreferences", Context.MODE_PRIVATE);
		 return customSharedPreference.getString("configFilepath",new File(Environment.getExternalStorageDirectory(),new File(LucidDreamingApp.APP_HOME_FOLDER, "SmartTimerConfig.txt.gzip").getAbsolutePath()).getAbsolutePath());
	 }
	 
	 public static String getWILDTimerConfigFilepath(Activity parent){
		 SharedPreferences customSharedPreference = parent.getSharedPreferences(
                 "LucidDreamingAppPreferences", Context.MODE_PRIVATE);
		 return customSharedPreference.getString("WILDTimerConfigFilepath",new File(Environment.getExternalStorageDirectory(),new File(LucidDreamingApp.APP_HOME_FOLDER, "WILDTimerConfig.txt.gzip").getAbsolutePath()).getAbsolutePath());
	 }


	@Override
	protected void onDestroy() {
		// TODO Auto-generated method stub
		super.onDestroy();
		saveEvents(this.getCurrentFocus());
	}
	 
	
	private class GUITask extends AsyncTask<Void, Void, Void> {
 		 
		 protected Void doInBackground(Void... urls) {
				
			 if(eventType == REM_EVENT){

	    		smartTimer = new SmartTimer("title",configFilepath);
	    		JSONDataHandler jsonHandler  = new JSONDataHandler();
	    		jsonHandler.setJson1(smartTimer.describeEvents());
	    		guiView.addJavascriptInterface(jsonHandler, "javahandler");
	    		guiView.loadUrl("file:///android_asset/html/smart_timer_gui.html");
			 }
				
			 
	        return null;
	     }

		@Override
		protected void onPostExecute(Void result) {
			// TODO Auto-generated method stub
			super.onPostExecute(result);
			
		}
		
	 }

	 
	 
	
}
