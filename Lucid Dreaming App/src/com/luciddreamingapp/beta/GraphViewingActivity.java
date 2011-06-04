package com.luciddreamingapp.beta;

import java.io.File;
import java.io.IOException;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.Window;
import android.webkit.WebView;
import android.widget.Toast;

import com.luciddreamingapp.beta.util.JSONLoader;

public class GraphViewingActivity extends Activity {

	private static final int REQUEST_SELECT_FILE = 1;
	private static final int REQUEST_SELECT_FILE_BUILT_IN =2;
	private static final boolean D = false;
	
	private File dataFile;
	
	private Handler guiHandler;
	
	private JSONDataHandler handler;
	private WebView graphView;
	/* (non-Javadoc)
	 * @see android.app.Activity#onCreate(android.os.Bundle)
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setTitle("Select a file to graph");
		
		setContentView(R.layout.graph_viewer);
		
		//possibly provide an example
		//handler.setJson1(new JSONObject(readRaw()));}catch(Exception e){e.printStackTrace();}
		graphView = (WebView) findViewById(R.id.graph_view);
        graphView.getSettings().setJavaScriptEnabled(true);
        graphView.getSettings().setLoadWithOverviewMode(true);
        graphView.getSettings().setUseWideViewPort(true);
        graphView.getSettings().setBuiltInZoomControls(true);
        graphView.addJavascriptInterface(handler, "javahandler");
        graphView.loadUrl("file:///android_asset/html/graphing_instructions.html");
     //  graphView.loadUrl("file:///android_asset/html/show_data_from_file.html");
		guiHandler = new Handler();
		
	}

	
	
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.graph_viewing_activity_menu, menu);
        return true;
    }
    

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {

        
      
        	
        	case R.id.menu_load_data:
        	
        		
            	Intent intent = new Intent("org.openintents.action.PICK_FILE");
            	intent.putExtra("org.openintents.extra.TITLE", getString(R.string.pick_file_window_title));
            	intent.putExtra("org.openintents.extra.BUTTON_TEXT", getString(R.string.pick_file_button_text));
            	
            	intent.setData(Uri.parse("file:///sdcard/"+LucidDreamingApp.GRAPH_DATA_LOCATION));
            	
            	//get the launcher intent for this package, if it is not null, the package is installed
            	Intent oiManagerExists =getPackageManager().getLaunchIntentForPackage("org.openintents.filemanager");
            	
            	if(oiManagerExists!=null){
            	try{
            		startActivityForResult(intent, REQUEST_SELECT_FILE);
            		//sendShortToast("Opening file manager");
            	}catch(Exception e){
            		sendToast(getString(R.string.toast_oi_file_manager_not_found));
              		 Intent selectFileIntent = new Intent(this, DataFileSelector.class);
              		 selectFileIntent.putExtra("filepath", LucidDreamingApp.GRAPH_DATA_LOCATION);
              		 selectFileIntent.putExtra("filterString",".*(txt)([.]gzip)??");
              		 
              		
              		 startActivityForResult(selectFileIntent, REQUEST_SELECT_FILE_BUILT_IN);
            	}
            	}else{
            		//use default
            	sendToast(getString(R.string.toast_oi_file_manager_not_found));
           		 Intent selectFileIntent = new Intent(this, DataFileSelector.class);
           		 selectFileIntent.putExtra("filepath", LucidDreamingApp.GRAPH_DATA_LOCATION);
           		 startActivityForResult(selectFileIntent, REQUEST_SELECT_FILE_BUILT_IN);
            	}
            	
        		
        		
        		
        		
        		
        	return true;

        	case R.id.menu_screenshot:
        		new SaveScreenshotTask().execute();
        		break;
        	
        case R.id.menu_preferences:
        	
        	Intent settingsActivity = new Intent(getBaseContext(),
                    Preferences.class);
        		startActivity(settingsActivity);
        	
        
        	return true;
        	
        	
        case R.id.menu_history:
        
        	Intent historyIntent = new Intent(getBaseContext(),
                   HistoryViewingActivity.class);
        		startActivity(historyIntent);
	       	
        	return true;
        	
        	
//        case R.id.menu_lab:
//        	sendToast("Lucid Lab");	
//        	Intent labIntent = new Intent(getBaseContext(),
//                    LucidLabActivity.class);
//        		startActivity(labIntent);
//	       	
//        	return true;
        
      
        case R.id.menu_help:
        	        	
        	
        	graphView.loadUrl("file:///android_asset/html/graphing_instructions.html");
	       	
        	return true;
       
     
        	
        case R.id.menu_exit:
	      	
        	finish();
	       	
        	return true;
        
        }
  
        return false;
    }
    
    
	@Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(D) {System.out.println("onActivityResult " + resultCode);}
        switch (requestCode) {
        case REQUEST_SELECT_FILE_BUILT_IN:
        	if(resultCode==Activity.RESULT_OK){
        		sendShortToast(getString(R.string.toast_loading_graph));
        		//get the filepath returned by the file selector and re-initialize the state
        		String dataFilepath=data.getExtras().getString("filepath");
        		dataFile = new File(dataFilepath);
        		try{
        			formatData(dataFile);
//        	//	setTitle(dataFile.getName());
//        		handler = new JSONDataHandler();
//        	
//        		JSONObject temp = JSONLoader.getData(dataFile);
//        		
//        		//tell the display page the limits of two y axis prone to having very large peaks
//        		//make the rest of the data illegible
//	        		SharedPreferences prefs = PreferenceManager
//	                .getDefaultSharedPreferences(getBaseContext());        		
//	        		
//	        		if(D)Log.e("Graph Viewer", ""+prefs.getInt("activity_count_y_axis_max", 2500));
//	        		if(D)Log.e("Graph Viewer", ""+prefs.getInt("sleep_score_y_axis_max", 35));
//        		
//        		//overwrite the preferences with the current ones
//	        		temp.put("activityCountYMax", prefs.getInt("activity_count_y_axis_max", 2500));
//	        		temp.put("sleepScoreYMax", prefs.getInt("sleep_score_y_axis_max", 35));
//	        		
//	        		handler.setJson1(temp);
//	        		loadPage();
        		}catch(JSONException ex){if(D)ex.printStackTrace();}
        		catch(IOException e){sendToast(getString(R.string.toast_error_opening_file));}
        		catch(Exception e){sendToast(getString(R.string.toast_error_opening_file));}
        		
        			
        	}else if(resultCode ==Activity.RESULT_CANCELED){
        		sendShortToast(getString(R.string.toast_file_selection_canceled));
        	}
        	break;
        	
        	
        case REQUEST_SELECT_FILE:
        	if(resultCode==Activity.RESULT_OK){
        		sendShortToast(getString(R.string.toast_loading_graph));
        		Uri uri =data.getData();
        		dataFile = new File(uri.getPath());
        		
        		try{
        			formatData(dataFile);
//        			
//            		setTitle(dataFile.getName());
//            		handler = new JSONDataHandler();
//            		handler.setJson1(JSONLoader.getData(dataFile));
            		
        		}catch(Exception e){
            			sendToast(getString(R.string.toast_error_opening_file));
            		}
//            		loadPage();
                   
            		
        		
        	}else if(resultCode ==Activity.RESULT_CANCELED){
        		sendShortToast(getString(R.string.toast_file_selection_canceled));
        	}
        	break;
        	
        	
        }	
        }
    
	/**Changes the axis settings for the graphs when they are plotted
	 * 
	 * @param dataFile file to add axis data to
	 * @throws JSONException 
	 * @throws IOException
	 */
	private void formatData(File dataFile) throws JSONException, IOException{
		//	setTitle(dataFile.getName());
		handler = new JSONDataHandler();
	
		JSONObject temp = JSONLoader.getData(dataFile);
		
		//tell the display page the limits of two y axis prone to having very large peaks
		//make the rest of the data illegible
    		SharedPreferences prefs = PreferenceManager
            .getDefaultSharedPreferences(getBaseContext());        		
    		
    		if(D)Log.e("Graph Viewer", ""+prefs.getInt("activity_count_y_axis_max", 2500));
    		if(D)Log.e("Graph Viewer", ""+prefs.getInt("sleep_score_y_axis_max", 35));
		
		//overwrite the preferences with the current ones
    		temp.put("activityCountYMax", prefs.getInt("activity_count_y_axis_max", 2500));
    		temp.put("sleepScoreYMax", prefs.getInt("sleep_score_y_axis_max", 35));
    		
    		handler.setJson1(temp);
    		loadPage();
		
	}
    
    private void loadPage(){
    	 graphView.addJavascriptInterface(handler, "javahandler");
         // graphView.loadUrl("file:///android_asset/html/basic_plot_from_handler.html");
          //graphView.loadUrl("file:///android_asset/html/zoom_show_data_from_file.html");
    	// graphView.loadUrl("file:///android_asset/html/black_show_data_from_file.html");
          graphView.loadUrl("file:///android_asset/html/zoom_show_data_from_file.html");
         // graphView.loadUrl("file:///android_asset/html/zooming.html");
    	
    }
	
	
    
    class ToastRunnable implements Runnable{
    	final String message;
    	ToastRunnable(String message){
    		this.message = message;
    	}
    	
    	public void run(){
    		sendShortToast(message);
    	}
    }
    
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
	
	 private class SaveScreenshotTask extends AsyncTask<long[], Void, Void> {
	     
		 boolean success = false;
		 File dir;
		 
		 protected Void doInBackground(long[]... urls) {
			 if(dataFile!=null &&graphView!=null){
	        		dir = new File(Environment.getExternalStorageDirectory(),LucidDreamingApp.SCREENSHOT_LOCATION);
	        		success = ((GlobalApp)getApplication()).savePicture(graphView.capturePicture(), 
	        				dir+"/"+dataFile.getName().replaceAll("[.].*",".png") , 50);
	        		}else{
	        			guiHandler.post(new ToastRunnable("Please load a graph first"));
	        		}
	        return null;
	     }

		@Override
		protected void onPostExecute(Void result) {
			// TODO Auto-generated method stub
			super.onPostExecute(result);
			
			if(success&&dir!=null){
				guiHandler.post(new ToastRunnable("Please load a graph first"));
				sendShortToast("Saved to: "+dir.getAbsolutePath());
			}else{
				guiHandler.post(new ToastRunnable("Please load a graph first"));
				sendShortToast("Could not save screenshot");
			}
		}
		 
		 

	     
	 }
	
}
