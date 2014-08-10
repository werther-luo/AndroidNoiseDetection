package com.example.andd.utils;

import android.util.Log;

public class UpdateTicker extends Thread {

	private static String TAG = UpdateTicker.class.getSimpleName();
	private boolean enable = false;
	private int delay = 1000;
	private AudioAnalyser aa;
	
	public UpdateTicker(AudioAnalyser aa) {
		super();
		Log.d(TAG, "Start the Ticker.");
		enable = true;
		this.aa = aa;
		this.delay = aa.getUpdateDelay() * 1000;
		aa.measureStart();
		start();
	}
	
	// Stoping the thread
	public void kill(){
		enable = false;
		aa.stopRecorder();
	}
	
	public void run(){
		while(enable){
			update();
			if(delay != 0)try{
				sleep(delay);
			}catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	public void update(){
		aa.update();
	}
	
	public void setDelay(int value){
		this.delay = value;
	}
	
}
