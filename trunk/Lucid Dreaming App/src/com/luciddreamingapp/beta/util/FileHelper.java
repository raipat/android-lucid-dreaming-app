package com.luciddreamingapp.beta.util;

import java.io.File;
import java.io.FileFilter;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.List;

/**Auxilary class used to select files and folders
 * (can be replaced with better IO tools)
 * @author Alex
 *
 */
public class FileHelper {
	
	/**Returns a list of file names residing within a given directory
	 * 
	 * @param directoryName directory to find files in
	 * @param filter filter used to accept or reject files @see createFileFilter if null, returns all files
	 * @return List of filenames in the directory (used to display files in lists and such)
	 */
	public static List<String> getFileNames(String directoryName, FilenameFilter filter){
		if(directoryName==null||directoryName.equals("")){
			return null;
		}
		File dir = new File(directoryName);
		
		//if filter is null, list all files, else list only filtered files
		String[] children = (filter ==null)?dir.list():dir.list(filter);
		
		
		List<String> fileList = null;
	if (children == null) {
	    // Either dir does not exist or is not a directory
	} else {
		fileList= new ArrayList<String>(children.length);
	    for (int i=0; i<children.length; i++) {
	    	fileList.add(children[i]);
	    	
	    }
	}
	
		return fileList;
	}//end getFileNames
	
	
	/**Returns a list of files residing within a given directory
	 * 
	 * @param directoryName directory to find files in
	 * @param filter filter used to accept or reject files @see createFileFilter, if null, returns all files
	 * @return list of File objects present in the directory, null if no directory is given
	 */
	public static List<File> getFiles(String directoryName, FilenameFilter filter){
		if(directoryName==null||directoryName.equals("")){
			return null;
		}
		File dir = new File(directoryName);
		
		//if filter is null, list all files, else list only filtered files
		File[] children = (filter ==null)?dir.listFiles():dir.listFiles(filter);
		
		
		List<File> fileList = null;
	if (children == null) {
	    // Either dir does not exist or is not a directory
	} else {
		fileList= new ArrayList<File>(children.length);
	    for (int i=0; i<children.length; i++) {
	    	fileList.add(children[i]);
	    	
	    }
	}
	
		return fileList;
	}//end getFiles
	
	/**Returns a list of File objects representing directories present in the given directory
	 * 
	 * @param fromDirectory
	 * @return
	 */
	public static List<File> getDirectories(String fromDirectory){
		
		if(fromDirectory==null ||fromDirectory.equals("")){return null;}

		// This filter only returns directories
		FileFilter fileFilter = new FileFilter() {
		    @Override
			public boolean accept(File file) {
		        return file.isDirectory();
		    }
		};
		List<File> fileList = new ArrayList<File>();
		File[] files = new File(fromDirectory).listFiles(fileFilter);
		for(int i = 0; i<files.length;i++){
			fileList.add(files[i]);
		}
		return fileList;
		
		
	}
	
	
	public static FilenameFilter createFileFilter(String nameRegex){
		
		return new FilenameFilterImpl(nameRegex,false); 
		
	}
	
	public static FilenameFilter createFileFolderFilter(String nameRegex){
		return new FilenameFilterImpl(nameRegex,true); 
	}
	
	public static FilenameFilter createDirFilter(String nameRegex){
		throw new UnsupportedOperationException("not supported yet");
	}
	
	
	}
	
	class FilenameFilterImpl implements FilenameFilter{
		String regex;
		boolean wantFolders;
			FilenameFilterImpl(String regex, boolean wantFolders){
				this.regex = regex;
				this.wantFolders=wantFolders;}
			
		    @Override
			public boolean accept(File dir, String name) {
		    	//return file names and directories
		    	try{
		    	if(wantFolders){return name.toLowerCase().matches(regex)||dir.isDirectory();}
		    	else{return name.matches(regex); }
		    	}catch(NullPointerException e){
		    		//catches issues with regex compiling
		    		return true;
		    	}
		    }
}
