/*
 * Copyright (C) 2009 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.luciddreamingapp.beta.util.gesturebuilder;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.view.MotionEvent;
import android.gesture.GestureOverlayView;
import android.gesture.Gesture;
import android.gesture.GestureLibrary;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;

import com.luciddreamingapp.beta.LucidDreamingApp;
import com.luciddreamingapp.beta.R;

public class CreateGestureActivity extends Activity {
    private static final float LENGTH_THRESHOLD = 120.0f;

    private Gesture mGesture;
    private View mDoneButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        setContentView(R.layout.create_gesture);

        mDoneButton = findViewById(R.id.done);

        GestureOverlayView overlay = (GestureOverlayView) findViewById(R.id.gestures_overlay);
        overlay.addOnGestureListener(new GesturesProcessor());
        
        sendToast("Available tags: awake, no dream, lucid dream, normal dream");
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        
        if (mGesture != null) {
            outState.putParcelable("gesture", mGesture);
        }
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        
        mGesture = savedInstanceState.getParcelable("gesture");
        if (mGesture != null) {
            final GestureOverlayView overlay =
                    (GestureOverlayView) findViewById(R.id.gestures_overlay);
            overlay.post(new Runnable() {
                @Override
				public void run() {
                    overlay.setGesture(mGesture);
                }
            });

            mDoneButton.setEnabled(true);
        }
    }

    @SuppressWarnings({"UnusedDeclaration"})
    public void addGesture(View v) {
        if (mGesture != null) {
            final TextView input = (TextView) findViewById(R.id.gesture_name);
            final CharSequence name = input.getText();
            if (name.length() == 0) {
                input.setError(getString(R.string.error_missing_name));
                return;
            }

            final GestureLibrary store = GestureBuilderActivity.getStore();
            store.addGesture(name.toString(), mGesture);
            store.save();

            setResult(RESULT_OK);
            File dir = new File(Environment.getExternalStorageDirectory(),LucidDreamingApp.GESTURES_LOCATION);
            final String path = new File(dir,"Lucid Dreaming App.gestures").getAbsolutePath();
            Toast.makeText(this, getString(R.string.save_success, path), Toast.LENGTH_LONG).show();
        } else {
            setResult(RESULT_CANCELED);
        }

        finish();
        
    }
    
    @SuppressWarnings({"UnusedDeclaration"})
    public void cancelGesture(View v) {
        setResult(RESULT_CANCELED);
        finish();
    }
    
    private class GesturesProcessor implements GestureOverlayView.OnGestureListener {
        @Override
		public void onGestureStarted(GestureOverlayView overlay, MotionEvent event) {
            mDoneButton.setEnabled(false);
            mGesture = null;
        }

        @Override
		public void onGesture(GestureOverlayView overlay, MotionEvent event) {
        }

        @Override
		public void onGestureEnded(GestureOverlayView overlay, MotionEvent event) {
            mGesture = overlay.getGesture();
            if (mGesture.getLength() < LENGTH_THRESHOLD) {
                overlay.clear(false);
            }
            mDoneButton.setEnabled(true);
        }

        @Override
		public void onGestureCancelled(GestureOverlayView overlay, MotionEvent event) {
        }
        
   	
        
    }
    
    protected void sendToast(String message){
    	Context context = getApplicationContext();
		  Toast toast = Toast.makeText(context, message,Toast.LENGTH_LONG);
		  
		  toast.show();
    }
    
}
