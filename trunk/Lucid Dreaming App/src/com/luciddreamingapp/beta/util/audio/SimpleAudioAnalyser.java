
/**
 * org.hermit.android.instrument: graphical instruments for Android.
 * <br>Copyright 2009 Ian Cameron Smith
 * 
 * <p>These classes provide input and display functions for creating on-screen
 * instruments of various kinds in Android apps.
 *
 * <p>This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2
 * as published by the Free Software Foundation (see COPYING).
 * 
 * <p>This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 */

/**Updates under Apache 2.0 Licence
 * Updated by Alexander Stone for the Lucid Dreaming App May 2011
 */

package com.luciddreamingapp.beta.util.audio;


import org.apache.commons.math.stat.descriptive.DescriptiveStatistics;
import org.json.JSONObject;

import android.os.Bundle;
import android.util.Log;

import com.luciddreamingapp.beta.util.DataManagerObserver;
import com.luciddreamingapp.beta.util.SleepDataManager;
import com.luciddreamingapp.beta.util.SleepDataPoint;


/**
 * An {@link Instrument} which analyses an audio stream in various ways.
 * 
 * <p>To use this class, your application must have permission RECORD_AUDIO.
 */
public class SimpleAudioAnalyser
	extends Instrument implements DataManagerObserver
{

	DescriptiveStatistics statSound;
	DescriptiveStatistics statPower;
	private static final boolean D = false; //debug
	
	
    // ******************************************************************** //
    // Constructor.
    // ******************************************************************** //

	/**
	 * Create a WindMeter instance.
	 * 
     * @param   parent          Parent surface.
	 */
    public SimpleAudioAnalyser() {
       
     //   parentSurface = parent;
       
        audioReader = new AudioReader();
        statSound = new DescriptiveStatistics(60);
        statPower = new DescriptiveStatistics(60);
        
       // spectrumAnalyser = new FFTTransformer(inputBlockSize, windowFunction);
    

        biasRange = new float[2];
    }


    // ******************************************************************** //
    // Configuration.
    // ******************************************************************** //

    /**
     * Set the sample rate for this instrument.
     * 
     * @param   rate        The desired rate, in samples/sec.
     */
    public void setSampleRate(int rate) {
        sampleRate = rate;        
      
    }
    

    /**
     * Set the input block size for this instrument.
     * 
     * @param   size        The desired block size, in samples.  Typical
     *                      values would be 256, 512, or 1024.  Larger block
     *                      sizes will mean more work to analyse the spectrum.
     */
    public void setBlockSize(int size) {
        inputBlockSize = size;

       // spectrumAnalyser = new FFTTransformer(inputBlockSize, windowFunction);

        // Allocate the spectrum data.
       
    }
    


    

    /**
     * Set the decimation rate for this instrument.
     * 
     * @param   rate        The desired decimation.  Only 1 in rate blocks
     *                      will actually be processed.
     */
    public void setDecimation(int rate) {
        sampleDecimate = rate;
    }
    
    
    /**
     * Set the histogram averaging window for this instrument.
     * 
     * @param   len         The averaging interval.  1 means no averaging.
     */
    public void setAverageLen(int len) {
        historyLen = len;
        
        // Set up the history buffer.
       
    }
    

    // ******************************************************************** //
    // Run Control.
    // ******************************************************************** //

    /**
     * The application is starting.  Perform any initial set-up prior to
     * starting the application.  We may not have a screen size yet,
     * so this is not a good place to allocate resources which depend on
     * that.
     */
    @Override
    public void appStart() {
    }


    /**
     * We are starting the main run; start measurements.
     */
    @Override
    public void measureStart() {
        audioProcessed = audioSequence = 0;
        readError = AudioReader.Listener.ERR_OK;
        
        audioReader.startReader(sampleRate, inputBlockSize * sampleDecimate, new AudioReader.Listener() {
            @Override
            public final void onReadComplete(short[] buffer) {
                receiveAudio(buffer);
            }
            @Override
            public void onReadError(int error) {
                handleError(error);
            }
        });
    }


    /**
     * We are stopping / pausing the run; stop measurements.
     */
    @Override
    public void measureStop() {
        audioReader.stopReader();
    }
    

    /**
     * The application is closing down.  Clean up any resources.
     */
    @Override
    public void appStop() {
    }
    

       
    

    // ******************************************************************** //
    // Audio Processing.
    // ******************************************************************** //

    /**
     * Handle audio input.  This is called on the thread of the audio
     * reader.
     * 
     * @param   buffer      Audio data that was just read.
     */
    private final void receiveAudio(short[] buffer) {
        // Lock to protect updates to these local variables.  See run().
        synchronized (this) {
            audioData = buffer;
            ++audioSequence;
        }
    }
    
    
    /**
     * An error has occurred.  The reader has been terminated.
     * 
     * @param   error       ERR_XXX code describing the error.
     */
    private void handleError(int error) {
        synchronized (this) {
            readError = error;
        }
    }


    // ******************************************************************** //
    // Main Loop.
    // ******************************************************************** //

    /**
     * Update the state of the instrument for the current frame.
     * This method must be invoked from the doUpdate() method of the
     * application's {@link SurfaceRunner}.
     * 
     * <p>Since this is called frequently, we first check whether new
     * audio data has actually arrived.
     * 
     * @param   now         Nominal time of the current frame in ms.
     */
    @Override
    public final void doUpdate(long now) {
        short[] buffer = null;
        synchronized (this) {
            if (audioData != null && audioSequence > audioProcessed) {
                //parentSurface.statsCount(1, (int) (audioSequence - audioProcessed - 1));
                audioProcessed = audioSequence;
                buffer = audioData;
            }
        }

        // If we got data, process it without the lock.
        if (buffer != null)
            processAudio(buffer);
        
        if (readError != AudioReader.Listener.ERR_OK)
            processError(readError);
    }


    /**
     * Handle audio input.  This is called on the thread of the
     * parent surface.
     * 
     * @param   buffer      Audio data that was just read.
     */
    private final void processAudio(short[] buffer) {
        // Process the buffer.  While reading it, it needs to be locked.
        synchronized (buffer) {
            // Calculate the power now, while we have the input
            // buffer; this is pretty cheap.
            final int len = buffer.length;

           
               SignalPower.biasAndRange(buffer, len - inputBlockSize, inputBlockSize, biasRange);
            double power =   SignalPower.calculatePowerDb(buffer, 0, len);
            if(power>0){
            	power = 0;
            }else if(power<-100){
            	power = -100;
            }else if(power == Double.NEGATIVE_INFINITY){
            	power = -100;
            }
            
            //flip the DB reading to make it make more sense
               statPower.addValue(power);
//      
           
               float range = biasRange[1];
//             
               
               //add range - bias to estimate the sound intensity
                statSound.addValue(range-biasRange[0]);
               
            // Tell the reader we're done with the buffer.
            buffer.notify();
        }

    }
    

    /**
     * Handle an audio input error.
     * 
     * @param   error       ERR_XXX code describing the error.
     */
    private final void processError(int error) {
      if(D)Log.e(TAG,"process ERROR");
    }
    

    // ******************************************************************** //
    // Save and Restore.
    // ******************************************************************** //

    /**
     * Save the state of the game in the provided Bundle.
     * 
     * @param   icicle      The Bundle in which we should save our state.
     */
    @Override
    protected void saveState(Bundle icicle) {
//      gameTable.saveState(icicle);
    }


    /**
     * Restore the game state from the given Bundle.
     * 
     * @param   icicle      The Bundle containing the saved state.
     */
    @Override
    protected void restoreState(Bundle icicle) {
//      gameTable.pause();
//      gameTable.restoreState(icicle);
    }
    

    // ******************************************************************** //
    // Class Data.
    // ******************************************************************** //

    // Debugging tag.
	@SuppressWarnings("unused")
	private static final String TAG = "instrument";

	
	// ******************************************************************** //
	// Private Data.
	// ******************************************************************** //

    // Our parent surface.
//    private SurfaceRunner parentSurface;

    // The desired sampling rate for this analyser, in samples/sec.
    private int sampleRate = 8000;

    // Audio input block size, in samples.
    private int inputBlockSize = 1024;
    
    // The selected windowing function.
   // private Window.Function windowFunction = Window.Function.BLACKMAN_HARRIS;

    // The desired decimation rate for this analyser.  Only 1 in
    // sampleDecimate blocks will actually be processed.
    private int sampleDecimate = 1;
   
    // The desired histogram averaging window.  1 means no averaging.
    private int historyLen = 4;

    // Our audio input device.
    private final AudioReader audioReader;

    // Fourier Transform calculator we use for calculating the spectrum.
//    private FFTTransformer spectrumAnalyser;
    
    // The gauges associated with this instrument.  Any may be null if not
    // in use.
//    private WaveformGauge waveformGauge = null;
//    private SpectrumGauge spectrumGauge = null;
//    private PowerGauge powerGauge = null;
    
    // Buffered audio data, and sequence number of the latest block.
    private short[] audioData;
    private long audioSequence = 0;
    
    // If we got a read error, the error code.
    private int readError = AudioReader.Listener.ERR_OK;
    
    // Sequence number of the last block we processed.
    private long audioProcessed = 0;

   
    // Temp. buffer for calculated bias and range.
    private float[] biasRange = null;


	@Override
	public void dataPointAdded(SleepDataPoint epoch) {
		// TODO Auto-generated method stub
		if(D){
			Log.e(TAG, "Sound level: " +statSound.getSum());
			Log.e(TAG,"Sound level Kurtosis"+statSound.getKurtosis());
			Log.e(TAG,"Signal power: "+statPower.getSum());
			Log.e(TAG,"Signal power Kurt: "+statPower.getKurtosis());
		
		}
		
		//update the epoch with the latest sound levels
		epoch.setAudioLevel((int)statSound.getSum());
		epoch.setAudioLevelKurtosis((int)statSound.getKurtosis());
		epoch.setAudioPower((int)statPower.getSum());
		epoch.setAudioPowerKurtosis((int)statPower.getKurtosis());
		statSound.clear();
		statPower.clear();
	}


	@Override
	public void dataPointUpdated(SleepDataPoint epoch) {
		// TODO Auto-generated method stub
		
	}


	@Override
	public void listUpdated(String innerHTML) {
		// TODO Auto-generated method stub
		
	}


	@Override
	public void graphUpdated(JSONObject graphData) {
		// TODO Auto-generated method stub
		
	}


	@Override
	public void dataReset() {
		// TODO Auto-generated method stub
		
	}
    
    

}

