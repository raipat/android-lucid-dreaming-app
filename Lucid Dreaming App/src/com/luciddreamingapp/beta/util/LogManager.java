package com.luciddreamingapp.beta.util;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import android.os.Environment;
import android.util.Log;

public class LogManager {
	
	protected static String logPath;
	protected File log;
	private static boolean D = false;
	private static String TAG = "LucidDreamingAppLogManager";
	
	
		
	public LogManager(String directoryPath,String logName,String headers){
		
		//get the path to the log
	    File root = Environment.getExternalStorageDirectory();
	    File file = new File(root, directoryPath+"/"+logName);
	    logPath = file.getAbsolutePath();
	    
				
		if(!logExists()){
			(new File(root,directoryPath)).mkdirs(); //try to create require directories
			System.out.println("Creating log");
			createLog(headers);
		}else{
			log = new File(logPath);
		}
		
	}
	
	//sample path
	//"Application Data/Lucid Night Light"
	//filename: "/eventLog.txt"
	protected void createLog(String headers){
    	
		try {
    	   
    	        log = new File(logPath);
    	        
    	        //open to append,just in case
    	        FileWriter writer = new FileWriter(log,true);
    	        BufferedWriter out = new BufferedWriter(writer,8096);//explicit buffer to suppress annoying warnings in console
    	       
    	      //  out.write("Night Light Log\r\n");
    	        out.write(headers+"\r\n");
    	        
    	        out.close();
    	        writer.close();
    	    
    	} catch (IOException e) {
    		e.printStackTrace();
    	    Log.e("LogManager"+logPath, "Could not write file " + e.getMessage());
    	}
	}
	
	//checks if a file exists before any operation
	protected static boolean logExists(){
		File f = new File(logPath);
		return (f.exists()&&f.canWrite());
	}
	
	
	public boolean appendEntry(String entry){
		try {
    	    	if(logExists()){
    	        
    	        //open to append
    	        FileWriter writer = new FileWriter(log,true);
    	        BufferedWriter out = new BufferedWriter(writer,1024);
    	       //append carriage return and a newline
    	       out.write(entry+"\r\n");
    	        
    	        out.close();
    	        writer.close();
    	        return true;
    	    	}else{return false;}
    	    
    	} catch (Exception e) {
    	if(D)	e.printStackTrace();
    	  if(D)  Log.e("LogManager "+logPath, "Could not write to log " + e.getMessage());
    	    return false;
    	}
    	
	}//end appendEntry
	
	public File getLog(String directoryPath,String logName,String headers){
		return log;
	}
	
	
	
	public static String writeToAndZipFile(String directoryPath,String fileName,String headers){

		//get the path to the log
	    File root = Environment.getExternalStorageDirectory();
	    File txtFile = new File(root, directoryPath+"/"+fileName);
	 	    
		String returnStr = null;
		if(!txtFile.exists()&&root.canWrite()){
			try{
				//create a new .txt file with JSON Data
			(new File(root,directoryPath)).mkdirs(); //try to create require directories
			if(D)System.out.println("Creating a new file");
			overwrite(headers,txtFile);}
			catch(Exception e){if(D) e.printStackTrace(); return returnStr;}
		}else{
			if(D)System.out.println("Overwriting a file");
			overwrite(headers,txtFile);
		}
		try{
			//zips the txt file and deletes the file on disk
		returnStr=	zipFile(txtFile.getAbsolutePath());
		txtFile.delete();
		}		
		catch(IOException e){
			return returnStr;
		}
		
		return returnStr;
	}
	
	
	
	 public static String zipFile(String fileName) throws IOException {
        
		 String outputFile = fileName + ".gzip";
		 FileInputStream in = new FileInputStream(fileName);
         BufferedInputStream in2 = new BufferedInputStream(in);
         FileOutputStream out = new FileOutputStream(outputFile);
         GZIPOutputStream zipOut = new GZIPOutputStream(out);
         BufferedOutputStream out2 = new BufferedOutputStream(zipOut,8096);
         int chunk;
         while ((chunk = in2.read()) != -1) {
                 out2.write(chunk);
         }
         out2.close();
         zipOut.close();
         out.close();
         return outputFile;
         //System.out.println("Wrote: " + fileName + ".zip");
 }
	 
	 public static String unzipFile(String fileName) throws IOException {
         FileInputStream fis = new FileInputStream(fileName);
         GZIPInputStream inZip = new GZIPInputStream(fis);
         BufferedInputStream bis = new BufferedInputStream(inZip,8096);
         InputStreamReader inputReader = new InputStreamReader(bis);
         BufferedReader bufferedReader = new BufferedReader(inputReader,8096);
         
         StringBuilder sb = new StringBuilder(8096);
         String line;
 		while((line =bufferedReader.readLine())!=null){
			sb.append(line);
		}
 		bufferedReader.close();
 		inputReader.close();
 		bis.close();
 		inZip.close();
 		fis.close();
         
       return sb.toString();
 }
	 
	
	
	protected static void overwrite(String content,File file){
		try {
	    	
	        
	        //open to append
	        FileWriter writer = new FileWriter(file,false);//overwrite contents
	        BufferedWriter out = new BufferedWriter(writer,8096);
	      //  GZIPOutputStream gzip= new GZIPOutputStream(writer);
	        
	       //append carriage return and a newline
	       out.write(content+"\r\n");
	        
	        out.close();
	        writer.close();
	      
	    	
	    
	} catch (IOException e) {
		e.printStackTrace();
	 if(D)   Log.e(TAG,"LogManager could not write to "+file.getAbsolutePath() + e.getMessage());
	    
	}catch(SecurityException ex){
		if(D)   ex.printStackTrace();
		
	}
	}
}
