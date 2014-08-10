package com.example.andd;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;

import com.example.ambientnoisedetectiondemo.R;

public class MainActivity extends Activity {

	private static String TAG = MainActivity.class.getSimpleName();
	Button btnStart;
	Button btnStop;
	EditText etInterval;
	EditText etRelative;
	EditText etMax;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		btnStart = (Button) findViewById(R.id.btn_start_service);
		btnStop = (Button) findViewById(R.id.btn_stop_service);
		
		// setting values waiting for user input
		etInterval = (EditText) findViewById(R.id.et_interval);
		etRelative = (EditText) findViewById(R.id.et_relative_value);
		etMax = (EditText) findViewById(R.id.et_max_value);

		btnStart.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				Intent service = new Intent(MainActivity.this,
						NoiseDetectService.class);
				int interval = 1;
				double relative = 100;
				double max = 180;
				
				// get value from user input
				Log.d(TAG, "Get EditText value:" + etInterval.getText().toString());
				if (!etInterval.getText().toString().equals("")) {
					interval = Integer.parseInt(etInterval.getText().toString());
				}
				if (!etRelative.getText().toString().equals("")) {
					relative = Integer.parseInt(etRelative.getText().toString());
				}
				if (!etMax.getText().toString().equals("")) {
					max = Integer.parseInt(etMax.getText().toString());
				}

				// pass setting value to service
				service.putExtra(NoiseDetectService.EXTRA_INTERVAL, interval);
				service.putExtra(NoiseDetectService.EXTRA_RELATIVE, relative);
				service.putExtra(NoiseDetectService.EXTRA_MAX_VALUE, max);
				startService(service);
			}
		});

		btnStop.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				Intent service = new Intent(MainActivity.this,
						NoiseDetectService.class);
				stopService(service);
			}
		});
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

}
