package edu.estgp.njoy;

import android.content.Context;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.util.Log;

import java.util.HashMap;
import java.util.Locale;


/**
 * Created by vrealinho on 08/03/18.
 *
 */
public class TTS {
    private static TextToSpeech tts = null;

    /**
     * TTS the message passed as argument
     * @param context
     * @param message to speak
     */
    public static void speak(Context context, final String message) {

        try {
            if (tts == null) {

                tts = new TextToSpeech(context.getApplicationContext(), new TextToSpeech.OnInitListener() {
                    @Override
                    public void onInit(int status) {
                        if (status == TextToSpeech.ERROR) {
                            Log.e("NJOY", "TTS init failed");
                        }
                        else if (status == TextToSpeech.SUCCESS) {
                            tts.setOnUtteranceProgressListener(new UtteranceProgressListener() {
                                @Override
                                public void onDone(String utteranceId) {
                                    tts.stop();
                                }

                                @Override
                                public void onError(String utteranceId) {
                                    Log.e("NJOY", "TTS error");
                                }

                                @Override
                                public void onStart(String utteranceId) {
                                }
                            });

                            int result = tts.setLanguage(Locale.getDefault());
                            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                                Log.e("NJOY", "This Language is not supported");
                            }
                        }
                    }
                });
            }
            else {
                // tts.speak(message, TextToSpeech.QUEUE_FLUSH, null);
                HashMap<String, String> params = new HashMap<String, String>();
                params.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "njoy-utteranceId");
                tts.speak(message, TextToSpeech.QUEUE_ADD, params);
            }
        }
        catch (Exception e) {
            e.printStackTrace();
            tts = null;
        }
    }

    public static boolean isSpeaking() {
        return tts.isSpeaking();
    }
}
