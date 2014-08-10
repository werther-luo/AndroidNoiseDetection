package com.example.andd;

import java.math.BigDecimal;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.util.Log;
import android.widget.RemoteViews;
import android.widget.TextView;

import com.example.ambientnoisedetectiondemo.R;
import com.example.andd.utils.AudioAnalyser;
import com.example.andd.utils.MyWindowManager;
import com.example.andd.utils.UpdateTicker;

public class NoiseDetectService extends Service {

	public static final String EXTRA_INTERVAL = "interval";
	public static final String EXTRA_RELATIVE = "relative";
	public static final String EXTRA_MAX_VALUE = "max_value";
	public static String TAG = NoiseDetectService.class.getSimpleName();

	// Values to calculate decibel
	private int interval = 1;
	private double relativeValue = 100;
	private double maxValue = 180;
	//FIXME: Array size to average, if TIMES=10, the output value will be the average of 10 interval(1s) value 
	private int TIMES = 10;

	// handler job between threads
	private Handler mHandler = new Handler();

	// Notification
	private NotificationManager mNM;
	private RemoteViews contentView;
	private Notification notification;
	private PendingIntent pendingIntent;
	private int NOTIFICATION_ID = R.string.foreground_service_started;

	// WAKE_LOCK
	private WakeLock wl;

	// Current average of last 10 noise power value
	private double currentAvgPower;

	// Looper for permanent listen sudio input
	private UpdateTicker ticker;

	@Override
	public void onCreate() {
		super.onCreate();
		setNotification();

		// start service as foreground component
		// since we need this service keep alive as long as phone is on
		startForeground(NOTIFICATION_ID, notification);
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		Log.d("Service",
				"Got intent extra:" + intent.getExtras().getInt("interval"));

		// get value for user input
		if (intent.hasExtra(EXTRA_INTERVAL))
			interval = intent.getExtras().getInt(EXTRA_INTERVAL);
		if (intent.hasExtra(EXTRA_RELATIVE))
			relativeValue = intent.getExtras().getDouble(EXTRA_RELATIVE);
		if (intent.hasExtra(EXTRA_MAX_VALUE))
			maxValue = intent.getExtras().getDouble(EXTRA_MAX_VALUE);

		// start to recording
		handleStart();

		return START_STICKY;
	}

	/**
	 * start recording audio and loop it
	 */
	synchronized void handleStart() {
		MyWindowManager.createSmallWindow(getApplicationContext());

		// Lock the cpu to prevent it from sleep
		PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
		wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "AN_RECORDER");
		wl.acquire();

		AudioAnalyser aa = new AudioAnalyser(
				new AudioAnalyser.AveragePowerListner() {

					@Override
					public void onAverageOut(double avgPower) {
						// This will be called every avg power come, use handler
						// to post a runnable
						// update any view we need to update
						Log.d(TAG, "Average for ten seconds value:" + avgPower);
						currentAvgPower = avgPower + relativeValue;
						mHandler.post(updateR);
					}
				});
		// set how many interval data collect to calculate the average
		aa.setCountNumber(TIMES);
		// set the interval audio update, in /seconds
		aa.setUpdateDelay(interval);

		if (ticker != null) {
			Log.d(TAG, "Ticker is still same one");
		} else {
			Log.d(TAG, "Ticker is null");
		}
		ticker = new UpdateTicker(aa);
	}

	// FIXME: New noise value arrived. Add here any thing you want to do while a new average value
	// comming
	// Update views, handle the data, etc
	Runnable updateR = new Runnable() {
		@Override
		public void run() {

			TextView tvWindow = MyWindowManager.getUpdateView();
			if (getValueRange() == 2) {
				tvWindow.setBackgroundResource(R.drawable.yellow);
				contentView.setImageViewResource(R.id.img_status,
						R.drawable.yellow);
			} else if (getValueRange() == 3) {
				tvWindow.setBackgroundResource(R.drawable.red);
				contentView.setImageViewResource(R.id.img_status,
						R.drawable.red);
			}

			contentView.setTextViewText(R.id.text, round(currentAvgPower, 2)
					+ "dB");
			tvWindow.setText(String.valueOf(round(currentAvgPower, 2)) + "dB");
			notification.contentView = contentView;

			mNM.notify(NOTIFICATION_ID, notification);
		}
	};

	private void setNotification() {
		mNM = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

		notification = new Notification(R.drawable.ic_launcher,
				"Ambient noise detector start...", System.currentTimeMillis());

		contentView = new RemoteViews(getPackageName(),
				R.layout.notification_layout);
		contentView.setImageViewResource(R.id.image, R.drawable.ic_launcher);
		contentView.setImageViewResource(R.id.img_status, R.drawable.green);
		contentView.setTextViewText(R.id.title, "Noise value:");
		contentView.setTextViewText(R.id.text, "wait...");
		notification.contentView = contentView;

		Intent main = new Intent(this, MainActivity.class);
		main.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
				| Intent.FLAG_ACTIVITY_SINGLE_TOP);
		pendingIntent = PendingIntent.getActivity(this, 0, main,
				PendingIntent.FLAG_UPDATE_CURRENT);

		notification.contentIntent = pendingIntent;
		notification.flags |= Notification.FLAG_ONGOING_EVENT
				| Notification.FLAG_FOREGROUND_SERVICE
				| Notification.FLAG_NO_CLEAR;
	}

	/**
	 * Utility function for rounding decimal values
	 */
	public double round(double d, int decimalPlace) {
		BigDecimal bd = new BigDecimal(Double.toString(d));
		bd = bd.setScale(decimalPlace, BigDecimal.ROUND_HALF_UP);
		return bd.doubleValue();
	}

	/**
	 * Calculate range of the current value according to max value Low - 0 to
	 * 30% of max level, Medium - 30 to 70%, High - 70 to 100% max level.
	 * 
	 * @return 1-low, 2-medium, 3-high
	 */
	private int getValueRange() {
		int range = 1;
		if (currentAvgPower > maxValue * 0.7) {
			range = 3;
		} else if (currentAvgPower > maxValue * 0.3) {
			range = 2;
		}

		return range;
	}

	@Override
	public void onDestroy() {
		mNM.cancel(NOTIFICATION_ID);
		stopForeground(true);
		MyWindowManager.removeSmallWindow(getApplicationContext());
		wl.release();

		ticker.kill();
		ticker = null;
		Log.d("Service", "Got on destroy");
		stopSelf();
		super.onDestroy();
	}

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

}
