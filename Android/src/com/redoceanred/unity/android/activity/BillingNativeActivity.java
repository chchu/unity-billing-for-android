/*
 * Copyright 2013 nishino.keiichiro@gmail.com
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.redoceanred.unity.android.activity;

import com.redoceanred.unity.android.BillingPlugin;
import com.unity3d.player.*;

import android.annotation.TargetApi;
import android.app.NativeActivity;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

@TargetApi(Build.VERSION_CODES.GINGERBREAD)
public class BillingNativeActivity extends NativeActivity
{
	private static final String TAG = "activity";

	private BillingPlugin mBillingPlugin;
	
	public void setBillingPlugin(BillingPlugin plugin) {
		mBillingPlugin = plugin;
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d(TAG, "onActivityResult(" + requestCode + "," + resultCode + "," + data);

        if ((mBillingPlugin != null) && !mBillingPlugin.handleActivityResult(requestCode, resultCode, data)) {
            super.onActivityResult(requestCode, resultCode, data);
        }
        else {
            Log.d(TAG, "onActivityResult handled by IABUtil.");
        }
	}
	
    // Unity Original.
	protected UnityPlayer mUnityPlayer;		// don't change the name of this variable; referenced from native code

	// UnityPlayer.init() should be called before attaching the view to a layout. 
	// UnityPlayer.quit() should be the last thing called; it will terminate the process and not return.
	protected void onCreate (Bundle savedInstanceState)
	{
		mUnityPlayer = new UnityPlayer(this);

		requestWindowFeature(Window.FEATURE_NO_TITLE);

		super.onCreate(savedInstanceState);
		
		getWindow().takeSurface(null);
		setTheme(android.R.style.Theme_NoTitleBar_Fullscreen);
		getWindow().setFormat(PixelFormat.RGB_565);

		if (mUnityPlayer.getSettings ().getBoolean ("hide_status_bar", true))
			getWindow ().setFlags (WindowManager.LayoutParams.FLAG_FULLSCREEN,
			                       WindowManager.LayoutParams.FLAG_FULLSCREEN);

		int glesMode = mUnityPlayer.getSettings().getInt("gles_mode", 1);
		boolean trueColor8888 = false;
		mUnityPlayer.init(glesMode, trueColor8888);

		View playerView = mUnityPlayer.getView();
		setContentView(playerView);
		playerView.requestFocus();
	}
	protected void onDestroy ()
	{
		super.onDestroy();
		mUnityPlayer.quit();

		// Add Billing.
        Log.d(TAG, "Destroying billing plugin.");
        if (mBillingPlugin != null) {
        	mBillingPlugin.dispose();
            mBillingPlugin = null;
        }
	}

	// onPause()/onResume() must be sent to UnityPlayer to enable pause and resource recreation on resume.
	protected void onPause()
	{
		super.onPause();
		mUnityPlayer.pause();
		if (isFinishing())
			mUnityPlayer.quit();
	}
	protected void onResume()
	{
		super.onResume();
		mUnityPlayer.resume();
	}
	public void onConfigurationChanged(Configuration newConfig)
	{
		super.onConfigurationChanged(newConfig);
		mUnityPlayer.configurationChanged(newConfig);
	}
	public void onWindowFocusChanged(boolean hasFocus)
	{
		super.onWindowFocusChanged(hasFocus);
		mUnityPlayer.windowFocusChanged(hasFocus);
	}
	public boolean dispatchKeyEvent(KeyEvent event)
	{
		if (event.getAction() == KeyEvent.ACTION_MULTIPLE)
			return mUnityPlayer.onKeyMultiple(event.getKeyCode(), event.getRepeatCount(), event);
		return super.dispatchKeyEvent(event);
	}
}
