package com.example.andd.utils;

import java.util.ArrayList;
import java.util.List;

import android.util.Log;

import com.example.andd.io.AudioReader;

public class AudioAnalyser {
	private static String TAG = AudioAnalyser.class.getSimpleName();

	// sampling rate for analyzing, in samples/sec
	private int sampleRate = 8000;

	// Audio input block size, in samples
	private int inputBlockSize = 256;

	// The desired decimation rate for this analyser. Only 1 in
	// sampleDecimate blocks will actually be processed.
	private int sampleDecimate = 1;

	// The desired histogram averaging window. 1 means no averaging.
	// private int historyLen = 4;

	// Our audio input device.
	private final AudioReader audioReader;

	// Buffered audio data, and sequence number of the latest block.
	private short[] audioData;
	private long audioSequence = 0;

	// If we got a read error, the error code.
	private int readError = AudioReader.Listener.ERR_OK;

	// Sequence number of the last block we processed.
	private long audioProcessed = 0;

	// Current signal power level, in dB relative to max. input power.
	private double currentPower = 0f;

	// Counter for updated number
	private int countUpdate = 0;

	// Number of average divisor
	private int countNumber;

	// Array saved every update value for calculate the average value
	private List<Double> powerSaved = new ArrayList<Double>();

	private AveragePowerListner avgListener;

	public int getCountNumber() {
		return countNumber;
	}

	public void setCountNumber(int countNumber) {
		this.countNumber = countNumber;
	}

	public int getUpdateDelay() {
		return updateDelay;
	}

	public void setUpdateDelay(int updateDelay) {
		this.updateDelay = updateDelay;
	}

	private int updateDelay;

	public AudioAnalyser(AveragePowerListner listener) {
		super();
		audioReader = new AudioReader();
		this.avgListener = listener;
	}

	public void setSampleRate(int sampleRate) {
		this.sampleRate = sampleRate;
	}

	public void setInputBlockSize(int inputBlockSize) {
		this.inputBlockSize = inputBlockSize;
	}

	public void setSampleDecimate(int sampleDecimate) {
		this.sampleDecimate = sampleDecimate;
	}

	public void setCurrentPower(double currentPower) {
		this.currentPower = currentPower;
	}

	/**
	 * start measurements
	 */
	public void measureStart() {
		audioProcessed = audioSequence = 0;
		readError = AudioReader.Listener.ERR_OK;

		audioReader.startReader(sampleRate, inputBlockSize * sampleDecimate,
				new AudioReader.Listener() {
					@Override
					public final void onReadComplete(short[] buffer) {
						receiveAudioInput(buffer);
					}

					@Override
					public void onReadError(int error) {
						handleErrorInput(error);
					}
				});
	}
	
	/**
	 * Stop recorder
	 */
	public void stopRecorder(){
		audioReader.stopReader();
	}

	/**
	 * Processing Audio just input Handle audio, This is called on the thread of
	 * the audio reader
	 * 
	 * @param buffer
	 *            Audio data that was just read
	 */
	private final void receiveAudioInput(short[] buffer) {
		synchronized (this) {
			audioData = buffer;
			++audioSequence;
		}
	}

	private final void handleErrorInput(int error) {
		synchronized (this) {
			readError = error;
		}
	}

	/**
	 * Update the state of the power value This method is invoked from the
	 * View's update method Since this is called frequently, we first check
	 * whether new audio data has actually arrived.
	 * 
	 * @param now
	 */
	public final void update() {
		short[] buffer = null;
		synchronized (this) {
			if (audioData != null && audioSequence > audioProcessed) {
				audioProcessed = audioSequence;
				buffer = audioData;
			}
		}

		// process it if got right data
		if (buffer != null) {
			processAudio(buffer);
		}

		if (readError != AudioReader.Listener.ERR_OK) {
			processError(readError);
		}
	}

	/**
	 * Handle audio input, This is called on the view update thread
	 * 
	 * @param buffer
	 */
	private final void processAudio(short[] buffer) {
		// Process the buffer. While reading it, it need to be locked.
		synchronized (buffer) {
			// Calculate the sound power, while we have the input buffer
			final int len = buffer.length;

			// Calculate the signal power
			currentPower = SignalPower.calculatePowerDb(buffer, 0, len);

			// notify reader buffer is released
			buffer.notify();

		}

		Log.d(TAG, "Updated power:" + currentPower);

		if (countUpdate == countNumber) {
			double total = 0.0;
			for (double power : powerSaved) {
				total += power;
			}
			double avg = total / countUpdate;
			avgListener.onAverageOut(avg);

			powerSaved.clear();
			countUpdate = 0;
		} else {
			powerSaved.add(currentPower);
			countUpdate++;
		}
	}

	/**
	 * Handle the audio input error
	 * 
	 * @param error
	 */
	private final void processError(int error) {
	}

	/**
	 * Listener for ervery ten data average out
	 * 
	 * @author LuoHanLin
	 * 
	 */
	public static abstract class AveragePowerListner {

		/**
		 * Next ten data average calculate complete
		 * 
		 * @param avgPower
		 */
		public abstract void onAverageOut(double avgPower);
	}

}
