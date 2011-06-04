package com.luciddreamingapp.beta.actigraph.parcels;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

/**Class containing actigraph data for a given epoch
 * 
 * @author Alex
 *
 */
public class ActigraphParcel implements Parcelable
{

	
	/*This method is required to reconstruct the parcel
	 * 
	 */
    public static final Parcelable.Creator<ActigraphParcel> CREATOR
    = new Parcelable.Creator<ActigraphParcel>() {   	
    	
    	    	
    	   @Override
    		 public ActigraphParcel createFromParcel(Parcel source) {
    		  
    	        return new ActigraphParcel(source);
    	        
    	        
    	  }
    		   
    		   @Override
    	  public ActigraphParcel[] newArray(int size) {
    	        return new ActigraphParcel[size];
    	  }
    	
};
	
	
	
	public ActigraphParcel(){
		//empty parcel
		epochLength = 60;
		numEvents = 0;
		xActivityCount = 0;
		yActivityCount = 0;
		zActivityCount = 0;
	}
	
	public ActigraphParcel(int epoch, int epochLength, int numEvents, int xActivityCount, int yActivityCount, int zActivityCount){
		this.epoch = epoch; 
		this.epochLength = epochLength;
		this.numEvents = numEvents;
		this.xActivityCount = xActivityCount;
		this.yActivityCount = yActivityCount;
		this.zActivityCount = zActivityCount;
	}
	
	
	//this method is required by the parcel creator
	public ActigraphParcel(Parcel source){
        /*
         * Reconstruct from the Parcel
         */   
     
        
        //read variables in order?
        epoch= source.readInt();
        
        epochLength = source.readInt();
        
        numEvents = source.readInt();
     
        
        xActivityCount = source.readInt();
        yActivityCount = source.readInt();
        zActivityCount = source.readInt();
       
  }
	
	//epoch number in sequence
	public int epoch; 
	
	//notify the recepient how much data is in this parcel 
	//in seconds
	public int epochLength;
	
	//notify the recepient of the number of events used to create this parcel
	public int numEvents;
	
	

	//activity counts over the threshold for x,y,z
	public int xActivityCount,yActivityCount,zActivityCount;

	@Override
	public int describeContents() {
		
		return this.hashCode();
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		
		dest.writeInt(epoch);
		//	save length
		dest.writeInt(epochLength);
		
		//save number of events
		dest.writeInt(numEvents);
		
		//save activity count
		dest.writeInt(xActivityCount);
		dest.writeInt(yActivityCount);
		dest.writeInt(zActivityCount);
		
		
	}
	
	@Override
	public String toString(){
		StringBuilder sb =  new StringBuilder();
		
		sb.append("epoch:"+epoch);
		sb.append("Epoch Length:"+epochLength);
		sb.append(" Accelerometer Events"+numEvents);
		
		sb.append(" xActivityCount: "+xActivityCount);
		sb.append(" yActivityCount: "+yActivityCount);
		sb.append(" zActivityCount: "+zActivityCount);
		
		return sb.toString();
		
		
	}

}
