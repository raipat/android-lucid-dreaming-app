package com.luciddreamingapp.beta;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.webkit.WebView;
import android.widget.DigitalClock;
import android.widget.Toast;



public class HelpActivity extends Activity {

	
	
	WebView helpView;
	
	
	
	
	 @Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.help_layout);
		
		helpView = (WebView)findViewById(R.id.help_view);
		
		
		helpView.getSettings().setJavaScriptEnabled(true);
		helpView.getSettings().setLoadWithOverviewMode(true);
		helpView.getSettings().setUseWideViewPort(true);
		helpView.getSettings().setBuiltInZoomControls(true);
		
		helpView.loadUrl("file:///android_asset/html/instructions.html");
		
		
	}







	public boolean onOptionsItemSelected(MenuItem item) {
	        switch (item.getItemId()) {

	        
	      
	        	
	        	case R.id.menu_load_data_help:
	        		
	        		helpView.loadUrl("file:///android_asset/html/graphing_instructions.html");
	        		
	        	return true;
	        	
	        	case R.id.menu_start_app_help:
        		
	        		helpView.loadUrl("file:///android_asset/html/instructions.html");
        		
	        		return true;
        	
	        	case R.id.menu_web_help:
	        		Uri uri = Uri.parse( "http://luciddreamingapp.com/lucid-dreaming-app-feature-quick-reference/" );
	    			startActivity( new Intent( Intent.ACTION_VIEW, uri ) );
	        		
	        		
	        	return true;
        	
	        	case R.id.menu_preferences:
//	        		
//	        		setContentView(R.layout.digital_clock_layout);
//	        		DigitalClock clock = (DigitalClock)findViewById(R.id.digitalclock);
	        		
	        		//clock.setTextAppearance(context, resid);
	        		//clock.sett
	        		
       	//	sendToast("Opening Preferences");
	            	Intent settingsActivity = new Intent(getBaseContext(),
	                        Preferences.class);
	            		startActivity(settingsActivity);
	        		
	        	return true;
	        	
	        	
	        }
	        return false;
	        
	    }
	
	
	
	
	
	
	
    protected void sendToast(String message){
    	Context context = getApplicationContext();
		  Toast toast = Toast.makeText(context, message,Toast.LENGTH_LONG);
		  
		  toast.show();
    }
    
    
    //##################################Menu Section
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.help_menu, menu);
        return true;
    }
	
	
}
