package com.example.andd;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.Window;
import android.view.WindowManager;

import com.example.ambientnoisedetectiondemo.R;
import com.example.ambientnoisedetectiondemo.R.layout;

public class WelcomeActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		// Remove tile, set full screen
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
				WindowManager.LayoutParams.FLAG_FULLSCREEN);

		setContentView(R.layout.activity_welcome);

		// FIXME:Launcher splash vew. This view will show a launcher icon and copy right message,
		// you can change the view in layout
		// or just change the string to what you wan in values/string.xml

		// stay for 4 seconds to open the main activity
		new Handler().postDelayed(new Runnable() {
			@Override
			public void run() {

				Intent toMainActivity = new Intent();
				toMainActivity.setClass(WelcomeActivity.this,
						MainActivity.class);
				startActivity(toMainActivity);
				finish();

			}
		}, 4000); // FIXME: Time stay in Welcome view. Change here to set the delay time, in /ms
	}

}
