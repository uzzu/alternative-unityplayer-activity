package com.unity3d.player;

import android.app.Activity;
import android.content.res.Configuration;
import android.graphics.Point;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Bundle;
import android.view.Display;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import com.unity3d.player.UnityPlayer;

public class AlternativeUnityPlayerActivity extends Activity implements SensorEventListener
{
	private static final boolean REVERSE_X_AXIS;
	private static final double ORIENTATION_THRESHOLD;
	private UnityPlayer mUnityPlayer;
	private SensorManager mSensorManager;
	private WindowManager mWindowManager;
	
	static {
		REVERSE_X_AXIS = Build.MANUFACTURER.equalsIgnoreCase("Amazon") && (Build.MODEL.equalsIgnoreCase("KFTT") || Build.MODEL.equalsIgnoreCase("KFJWI") || Build.MODEL.equalsIgnoreCase("KFJWA") || Build.MODEL.equalsIgnoreCase("KFSOWI") || Build.MODEL.equalsIgnoreCase("KFTHWA") || Build.MODEL.equalsIgnoreCase("KFTHWI") || Build.MODEL.equalsIgnoreCase("KFAPWA") || Build.MODEL.equalsIgnoreCase("KFAPWI"));
		ORIENTATION_THRESHOLD = 0.5D * Math.sqrt(3D);
	}

	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
		// nop
	}

	@Override
	public void onSensorChanged(SensorEvent event) {
		if (event.sensor.getType() != Sensor.TYPE_ACCELEROMETER) {
			return;
		}
		Display display = mWindowManager.getDefaultDisplay();
		int orientation = calcurateDeviceOrientation(event, display);
		mUnityPlayer.nativeDeviceOrientation(orientation);
	}

	// Pass any keys not handled by (unfocused) views straight to UnityPlayer
	public boolean onKeyMultiple(int keyCode, int count, KeyEvent event)
	{
		return mUnityPlayer.onKeyMultiple(keyCode, count, event);
	}

	public boolean onKeyDown(int keyCode, KeyEvent event)
	{
		return mUnityPlayer.onKeyDown(keyCode, event);
	}

	public boolean onKeyUp(int keyCode, KeyEvent event)
	{
		return mUnityPlayer.onKeyUp(keyCode, event);
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

	protected void onCreate (Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		requestWindowFeature(Window.FEATURE_NO_TITLE);

		mUnityPlayer = new UnityPlayer(this);
		if (mUnityPlayer.getSettings().getBoolean("hide_status_bar", true))
			getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
			                       WindowManager.LayoutParams.FLAG_FULLSCREEN);

		int glesMode = mUnityPlayer.getSettings().getInt("gles_mode", 1);
		boolean trueColor8888 = false;
		mUnityPlayer.init(glesMode, trueColor8888);

		View playerView = mUnityPlayer.getView();
		setContentView(playerView);
		playerView.requestFocus();

		mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
		mWindowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
	}

	protected void onDestroy ()
	{
		mUnityPlayer.quit();
		super.onDestroy();
	}

	// onPause()/onResume() must be sent to UnityPlayer to enable pause and resource recreation on resume.
	protected void onPause()
	{
		super.onPause();
		mUnityPlayer.pause();
		mSensorManager.unregisterListener(this);
	}

	protected void onResume()
	{
		super.onResume();
		mUnityPlayer.resume();
		mSensorManager.registerListener(this, mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_FASTEST);
	}

	private int calcurateDeviceOrientation(SensorEvent sensorEvent, Display display) {
		float fx = sensorEvent.values[0];
		float fy = sensorEvent.values[1];
		float fz = sensorEvent.values[2];
		float fSensor = 1.0F / (float) Math.sqrt(fx * fx + fy * fy + fz * fz);
		float sx = fx * fSensor;
		float sy = fy * fSensor;
		float sz = fz * fSensor;
		int orientation = display.getRotation();
		Point size = new Point();
		display.getSize(size);
		int tall = size.y <= size.x ? 0 : 1;
		float scala = (float) tall;
		orientation = (orientation & 1) != 0 ? 0 : 1;
		if ((tall ^ orientation) != 0) {
			scala = -sx;
			sx = sy;
			sy = scala;
		}
		if (REVERSE_X_AXIS) {
			sx = -sx;
		}
		scala = -1F;
		int result = 0;
		if (-1F < sy) {
			scala = sy;
			result = 1;
		}
		if (scala < -sy) {
			scala = -sy;
			result = 2;
		}
		if (scala < sx) {
			scala = sx;
			result = 3;
		}
		if (scala < -sx) {
			scala = -sx;
			result = 4;
		}
		if (scala < sz) {
			scala = sz;
			result = 5;
		}
		if (scala < -sz) {
			scala = -sz;
			result = 6;
		}
		if ((double) scala < ORIENTATION_THRESHOLD) {
			result = 0;
		}
		return result;
	}
}

