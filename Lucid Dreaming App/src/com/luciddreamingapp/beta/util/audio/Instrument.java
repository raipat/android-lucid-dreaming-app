
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

import android.os.Bundle;


/**
 * An instrument which measures some quantity, or accesses or produces some
 * data, which can be displayed on one or more {@link Gauge} objects.
 */
public class Instrument
{

	// ******************************************************************** //
	// Constructor.
	// ******************************************************************** //
	


    // ******************************************************************** //
    // Run Control.
    // ******************************************************************** //

    /**
     * The application is starting.  Perform any initial set-up prior to
     * starting the application.
     */
	public void appStart() {
    }
    

    /**
     * We are starting the main run; start measurements.
     */
	public void measureStart() {
    }
    

    /**
     * We are stopping / pausing the run; stop measurements.
     */
	public void measureStop() {
    }
    

    /**
     * The application is closing down.  Clean up any resources.
     */
	public void appStop() {
    }
    

    // ******************************************************************** //
    // Main Loop.
    // ******************************************************************** //

    /**
     * Update the state of the instrument for the current frame.
     * 
     * <p>Instruments may override this, and can use it to read the
     * current input state.  This method is invoked in the main animation
     * loop -- i.e. frequently.
     * 
     * @param   now         Nominal time of the current frame in ms.
     */
    protected void doUpdate(long now) {
    }


	// ******************************************************************** //
	// Utilities.
	// ******************************************************************** //

	
	
    
    // ******************************************************************** //
    // Save and Restore.
    // ******************************************************************** //

    /**
     * Save the state of the game in the provided Bundle.
     * 
     * @param   icicle      The Bundle in which we should save our state.
     */
    protected void saveState(Bundle icicle) {
//      gameTable.saveState(icicle);
    }


    /**
     * Restore the game state from the given Bundle.
     * 
     * @param   icicle      The Bundle containing the saved state.
     */
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

	

}

