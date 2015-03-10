package com.javacodegeeks.androidaccelerometerexample;

import android.content.Context;
import android.speech.tts.TextToSpeech;
import android.widget.Toast;

import java.util.Locale;

/**
 * Created by chase on 3/9/15.
 */
public class UKTextToSpeech {
    TextToSpeech ttobj;
    Context context;
    public UKTextToSpeech(Context context) {
        context = context;
        ttobj = new TextToSpeech(context,
                new TextToSpeech.OnInitListener() {
                    @Override
                    public void onInit(int status) {
                        if(status != TextToSpeech.ERROR){
                            ttobj.setLanguage(Locale.UK);
                        }
                    }
                });

    }
    public void speakText(String toSpeak){

//        Toast.makeText(context, toSpeak,
//                Toast.LENGTH_SHORT).show();
        ttobj.speak(toSpeak, TextToSpeech.QUEUE_FLUSH, null);

    }

}
