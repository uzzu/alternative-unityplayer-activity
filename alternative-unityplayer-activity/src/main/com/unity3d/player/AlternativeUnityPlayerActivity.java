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

	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		mUnityPlayer.configurationChanged(newConfig);
	}
	public void onWindowFocusChanged(boolean hasFocus) {
		super.onWindowFocusChanged(hasFocus);
		mUnityPlayer.windowFocusChanged(hasFocus);
	}

	// Pass any keys not handled by (unfocused) views straight to UnityPlayer
	public boolean onKeyMultiple(int keyCode, int count, KeyEvent event) {
		return mUnityPlayer.onKeyMultiple(keyCode, count, event);
	}

	public boolean onKeyDown(int keyCode, KeyEvent event) {
		return mUnityPlayer.onKeyDown(keyCode, event);
	}

	public boolean onKeyUp(int keyCode, KeyEvent event) {
		return mUnityPlayer.onKeyUp(keyCode, event);
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
	

	// UnityPlayer.init() should be called before attaching the view to a layout - it will load the native code.
	// UnityPlayer.quit() should be the last thing called - it will unload the native code.
	protected void onCreate (Bundle savedInstanceState) {
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
	protected void onDestroy () {
		mUnityPlayer.quit();
		super.onDestroy();
	}

	// onPause()/onResume() must be sent to UnityPlayer to enable pause and resource recreation on resume.
	protected void onPause() {
		super.onPause();
		mUnityPlayer.pause();
		mSensorManager.unregisterListener(this);
	}
	protected void onResume() {
		super.onResume();
		mUnityPlayer.resume();
		mSensorManager.registerListener(this, mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_FASTEST);
	}

	private int calcurateDeviceOrientation(SensorEvent sensorEvent, Display display) {
		float dx = sensorEvent.values[0];
		float dy = sensorEvent.values[1];
		float dz = sensorEvent.values[2];
		float ds = 1.0F / (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
		float vx = dx * ds;
		float vy = dy * ds;
		float vz = dz * ds;
		int orientation = display.getRotation();
		Point size = getSizeFrom(display);
		int tall = size.y <= size.x ? 0 : 1;
		float vMax = (float) tall;
		orientation = (orientation & 1) != 0 ? 0 : 1;
		if ((tall ^ orientation) != 0) {
			vMax = -vx;
			vx = vy;
			vy = vMax;
		}
		if (REVERSE_X_AXIS) {
			vx = -vx;
		}
		vMax = -1F;
		int result = 0;
		if (-1F < vy) {
			vMax = vy;
			result = 1;
		}
		if (vMax < -vy) {
			vMax = -vy;
			result = 2;
		}
		if (vMax < vx) {
			vMax = vx;
			result = 3;
		}
		if (vMax < -vx) {
			vMax = -vx;
			result = 4;
		}
		if (vMax < vz) {
			vMax = vz;
			result = 5;
		}
		if (vMax < -vz) {
			vMax = -vz;
			result = 6;
		}
		if ((double) vMax < ORIENTATION_THRESHOLD) {
			result = 0;
		}
		return result;
	}
	
	@SuppressWarnings("deprecation")
	private Point getSizeFrom(Display display) {
		final Point result = new Point();
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB_MR2) {
			result.x = display.getWidth();
			result.y = display.getHeight();
		} else {
			display.getSize(result);
		}
		return result;
	}
}

