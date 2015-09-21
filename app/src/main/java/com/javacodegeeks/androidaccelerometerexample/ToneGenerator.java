package com.javacodegeeks.androidaccelerometerexample;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;

import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by chase on 3/17/15.
 *
 */
public class ToneGenerator {
    private final static String TAG = ToneGenerator.class.getSimpleName();
    private final double duration = .05; // seconds.. make sure numSamples is going to be an int
    private final int sampleRate = 8000;
    private final int numSamples = (int) (duration * sampleRate);
    private final double sample[] = new double[numSamples];
    private final double freqOfTone = 15000; // hz
    private final byte generatedSnd[] = new byte[2 * numSamples];
    private boolean playing = true;

    public void play() {
        playing = true;
        genTone();
        new Thread(new Runnable() {
            @Override
            public void run() {
                playSound();
            }
        }).start();
    }

    public void stop() {
        playing = false;
    }

    private void genTone(){
        // fill out the array
        for (int i = 0; i < numSamples; ++i) {
            sample[i] = Math.sin(2 * Math.PI * i / (sampleRate/freqOfTone));
        }

        // convert to 16 bit pcm sound array
        // assumes the sample buffer is normalised.
        int idx = 0;
        for (final double dVal : sample) {
            // scale to maximum amplitude
            final short val = (short) ((dVal * 32767));
            // in 16 bit wav PCM, first byte is the low order byte
            generatedSnd[idx++] = (byte) (val & 0x00ff);
            generatedSnd[idx++] = (byte) ((val & 0xff00) >>> 8);

        }
    }

    private void playSound(){
        final AudioTrack audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC,
                sampleRate, AudioFormat.CHANNEL_CONFIGURATION_MONO,
                AudioFormat.ENCODING_PCM_16BIT, numSamples,
                AudioTrack.MODE_STREAM);
        audioTrack.play();
        while (playing) {
            audioTrack.write(generatedSnd, 0, generatedSnd.length);
        }
    }
}

