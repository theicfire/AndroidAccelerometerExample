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
    private final int sampleRate = 40000;
    private final int numSamples = sampleRate;
    private final double freqOfTone = 2400;
    private final byte generatedSnd[] = new byte[2 * numSamples];
    private final int MAX_SIGNED_16_BIT = (int) Math.pow(2, 15) - 1;
    final AudioTrack audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC,
            sampleRate, AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT, generatedSnd.length,
            AudioTrack.MODE_STREAM);


    public ToneGenerator() {
        if (freqOfTone >= numSamples / 2) throw new AssertionError("Sample rate must be high enough to support Nyquist theorom");
        if (freqOfTone % (sampleRate / numSamples) != 0) throw new AssertionError("frequency must be a multiple of " + (sampleRate / numSamples) + " to keep streaming continuous");
        genTone();
    }

    public void play() {
        if (audioTrack.getPlayState() == AudioTrack.PLAYSTATE_PLAYING) {
            return;
        }
        new Thread(new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, "Startplay");
                audioTrack.play();
                while (audioTrack.getPlayState() == AudioTrack.PLAYSTATE_PLAYING) {
                    audioTrack.write(generatedSnd, 0, generatedSnd.length);
                }
                Log.d(TAG, "Endplay");
            }
        }).start();
    }

    public void stop() {
        audioTrack.pause();
        audioTrack.flush();
    }

    private void genTone(){
        // fill out the array
        for (int i = 0; i < numSamples; ++i) {
            double sam = Math.sin(2 * Math.PI * i / (sampleRate/freqOfTone));

            // scale to maximum amplitude
            final short val = (short) ((sam * MAX_SIGNED_16_BIT));
            // in 16 bit wav PCM, first byte is the low order byte
            generatedSnd[i * 2] = (byte) (val & 0x00ff);
            generatedSnd[i * 2 + 1] = (byte) ((val & 0xff00) >>> 8);
        }
    }
}



