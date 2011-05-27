package com.luciddreamingapp.beta;

import java.io.File;
import java.io.FilenameFilter;
import java.util.List;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.AdapterView.OnItemClickListener;

import com.luciddreamingapp.beta.R;
import com.luciddreamingapp.beta.util.FileHelper;

public class DataFileSelector extends Activity{
	
	 private static final String TAG = "FileSelector";
	 private static final boolean D = false;
	
	private List<File> files;
	private ListView fileListView;
	private ArrayAdapter<String> fileListAdapter;
	  @Override
	    public void onCreate(Bundle savedInstanceState) {
	        super.onCreate(savedInstanceState);
	        
	        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, 
                    WindowManager.LayoutParams.FLAG_FULLSCREEN);

		//initialize fileselector.xml layout file 
		//(must be called prior to getting views by ID)
		setContentView(R.layout.fileselector);
		//create the file list
    	fileListView =((ListView)findViewById(R.id.file_list));
    	String filepath=null;
    	String filterString = null;
    	Bundle extras = getIntent().getExtras(); 
    	if(extras !=null)
    	{
    		filepath = extras.getString("filepath");
    		filterString =extras.getString("filterString");
    	}

    	
    	
    	//adapter holds the data for the list
    	fileListAdapter =new ArrayAdapter<String>(this,R.layout.message);
    	fileListView.setAdapter(fileListAdapter);
    	
    	File path = new File(Environment.getExternalStorageDirectory(),filepath);
    	if(path.exists()){
    	FilenameFilter filter = FileHelper.createFileFilter(filterString);
    	//FilenameFilter filter = FileHelper.createFileFilter(".*(txt)([.]gzip)??");
    	files = FileHelper.getFiles(path.getAbsolutePath(), filter);
    	//System.out.println(path.getAbsolutePath());

    	for(File f: files){
    		fileListAdapter.add(f.getName());
    	}
    	OnItemClickListener fileListListener = new OnItemClickListener() {
            
			@Override
			public void onItemClick(AdapterView<?> adapterView, View arg1, int adapterPosition,
					long rowID) {
				
	           Intent activityResult = new Intent();
	           activityResult.putExtra("filepath",  files.get((int)rowID).getAbsolutePath());
	          // System.out.println("got rowID: "+rowID);
	            // Set result and finish this Activity
	            setResult(Activity.RESULT_OK, activityResult);
	            finish();
				
				
			}
        };
		
		fileListView.setOnItemClickListener(fileListListener);
    	}else{
    		if(D){
    		System.out.println(path.getAbsolutePath());
    		System.out.println("directory does not exist");
    		}
    		setResult(Activity.RESULT_CANCELED, null);
            finish();
    	}
	  }
}
