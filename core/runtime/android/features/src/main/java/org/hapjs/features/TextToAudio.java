/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.features;

import static org.hapjs.bridge.Response.CODE_FEATURE_ERROR;
import static org.hapjs.bridge.Response.CODE_GENERIC_ERROR;
import static org.hapjs.bridge.Response.CODE_ILLEGAL_ARGUMENT;
import static org.hapjs.bridge.Response.CODE_SUCCESS;
import static org.hapjs.bridge.Response.ERROR;

import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.text.TextUtils;
import android.util.Log;
import java.io.File;
import java.io.IOException;
import java.util.Locale;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Semaphore;
import org.hapjs.bridge.CallbackContext;
import org.hapjs.bridge.CallbackHybridFeature;
import org.hapjs.bridge.FeatureExtension;
import org.hapjs.bridge.Request;
import org.hapjs.bridge.Response;
import org.hapjs.bridge.annotation.ActionAnnotation;
import org.hapjs.bridge.annotation.FeatureExtensionAnnotation;
import org.hapjs.common.executors.Executors;
import org.json.JSONException;
import org.json.JSONObject;

@FeatureExtensionAnnotation(
        name = TextToAudio.FEATURE_NAME,
        actions = {
                @ActionAnnotation(name = TextToAudio.ACTION_IS_SPEAKING, mode = FeatureExtension.Mode.SYNC),
                @ActionAnnotation(name = TextToAudio.ACTION_STOP, mode = FeatureExtension.Mode.SYNC),
                @ActionAnnotation(name = TextToAudio.ACTION_SPEAK, mode = FeatureExtension.Mode.ASYNC),
                @ActionAnnotation(
                        name = TextToAudio.ACTION_TEXT_TO_AUDIO_FILE,
                        mode = FeatureExtension.Mode.ASYNC),
                @ActionAnnotation(
                        name = TextToAudio.ACTION_LANGUAGE_AVAILABLE,
                        mode = FeatureExtension.Mode.ASYNC),
                @ActionAnnotation(
                        name = TextToAudio.EVENT_ON_TTS_STATE_CHANGE,
                        mode = FeatureExtension.Mode.CALLBACK,
                        type = FeatureExtension.Type.EVENT,
                        alias = TextToAudio.EVENT_ON_TTS_STATE_CHANGE_ALIAS),
        })
public class TextToAudio extends CallbackHybridFeature {
    protected static final String FEATURE_NAME = "service.texttoaudio";
    protected static final String ACTION_SPEAK = "speak";
    protected static final String ACTION_TEXT_TO_AUDIO_FILE = "textToAudioFile";
    protected static final String ACTION_STOP = "stop";
    protected static final String ACTION_IS_SPEAKING = "isSpeaking";
    protected static final String ACTION_LANGUAGE_AVAILABLE = "isLanguageAvailable";
    protected static final String PARAM_KEY_LANG = "lang";
    protected static final String PARAM_KEY_CONTENT = "content";
    protected static final String PARAM_KEY_PITCH = "pitch";
    protected static final String PARAM_KEY_RATE = "rate";
    protected static final String PARAM_KEY_LANG_CN = "zh_CN";
    protected static final String PARAM_KEY_LANG_US = "en_US";
    protected static final String EVENT_ON_TTS_STATE_CHANGE = "__onttsstatechange";
    protected static final String EVENT_ON_TTS_STATE_CHANGE_ALIAS = "onttsstatechange";
    private static final String TAG = "TextToAudio";
    private static final String RESULT_STATE = "state";
    private static final String RESULT_UTTERANCE_ID = "utteranceId";
    private static final String RESULT_FILE_PATH = "filePath";
    private static final String RESULT_IS_AVAILABLE = "isAvailable";
    private static final String PRE_FIX_UTTERANCE_ID_SPEAK = "speak";
    private static final String PRE_FIX_UTTERANCE_ID_SPEAK_AUDIO = "speakAudio";

    private static final float NORMAL_SPEECH_RATE = 1;
    private static final float NORMAL_PITCH = 1;

    private static final int TTS_CODE_START = 1;
    private static final int TTS_CODE_DONE = 2;
    private static final int TTS_CODE_STOP = 3;
    private static final int TTS_CODE_ERROR = 4;

    private static final int ERROR_CODE_TTS_INTERNAL_ERROR = CODE_FEATURE_ERROR;
    private static final int ERROR_CODE_TTS_FEATURE_NOT_AVAILABLE = CODE_FEATURE_ERROR + 1;
    private static final int ERROR_CODE_TTS_INIT_FAIL = CODE_FEATURE_ERROR + 2;
    private static final int ERROR_CODE_TTS_CONTENT_LENGTH_LIMIT = CODE_FEATURE_ERROR + 3;
    private static final int ERROR_CODE_TTS_LANGUAGE_NOT_AVAILABLE = CODE_FEATURE_ERROR + 4;
    private static final int ERROR_CODE_TTS_LANGUAGE_IO_EXCEPTION = CODE_FEATURE_ERROR + 5;

    private Semaphore mSemaphore = new Semaphore(1);
    private volatile TextToSpeech mTextToSpeech;
    private volatile boolean mInit = false;

    @Override
    protected Response invokeInner(Request request) throws Exception {
        String action = request.getAction();
        if (ACTION_IS_SPEAKING.equals(action)) {
            return new Response(isSpeaking());
        } else if (ACTION_STOP.equals(action)) {
            return new Response(stopTTS());
        }

        if (ensureInit(request)) {
            if (ACTION_SPEAK.equals(action)) {
                speak(request);
            } else if (ACTION_TEXT_TO_AUDIO_FILE.equals(action)) {
                textToAudioFile(request);
            } else if (EVENT_ON_TTS_STATE_CHANGE.equals(action)) {
                handleEventRequest(request);
            } else if (ACTION_LANGUAGE_AVAILABLE.equals(action)) {
                isLanguageAvailable(request);
            }
        } else {
            request.getCallback()
                    .callback(new Response(CODE_GENERIC_ERROR, ERROR_CODE_TTS_INIT_FAIL));
        }
        return Response.SUCCESS;
    }

    private boolean isSpeaking() {
        if (mInit && mTextToSpeech != null) {
            return mTextToSpeech.isSpeaking();
        }
        return false;
    }

    private int stopTTS() {
        if (mInit && mTextToSpeech != null) {
            return mTextToSpeech.stop();
        }
        return TextToSpeech.SUCCESS;
    }

    private boolean ensureInit(Request request) {
        if (mInit) {
            return mInit;
        }

        try {
            mSemaphore.acquire();
        } catch (InterruptedException e) {
            Log.e(TAG, "Semaphore acquire error when ensureInit", e);
            return false;
        }

        try {
            if (!mInit) {
                CountDownLatch countDownLatch = new CountDownLatch(1);
                mTextToSpeech =
                        new TextToSpeech(
                                request.getApplicationContext().getContext(),
                                new TextToSpeech.OnInitListener() {
                                    @Override
                                    public void onInit(int status) {
                                        if (status == mTextToSpeech.SUCCESS) {
                                            mTextToSpeech.setSpeechRate(NORMAL_SPEECH_RATE);
                                            mTextToSpeech.setPitch(NORMAL_PITCH);
                                            mInit = true;
                                        }
                                        countDownLatch.countDown();
                                    }
                                });
                countDownLatch.await();
            }
        } catch (InterruptedException e) {
            Log.e(TAG, "CountDownLatch await error", e);
        } finally {
            mSemaphore.release();
        }

        return mInit;
    }

    private void isLanguageAvailable(Request request) {
        JSONObject params = null;
        try {
            params = request.getJSONParams();
        } catch (JSONException e) {
            Log.e(TAG, "JSONException when get params", e);
        }
        if (params == null) {
            request.getCallback()
                    .callback(new Response(CODE_ILLEGAL_ARGUMENT, "get parameter failed"));
            return;
        }
        String lang = params.optString(PARAM_KEY_LANG);
        if (TextUtils.isEmpty(lang)) {
            request
                    .getCallback()
                    .callback(new Response(CODE_ILLEGAL_ARGUMENT,
                            "request params is not available!"));
            return;
        }

        int result = TextToSpeech.LANG_NOT_SUPPORTED;
        if (PARAM_KEY_LANG_CN.equals(lang)) {
            result = mTextToSpeech.isLanguageAvailable(Locale.CHINA);
        } else if (PARAM_KEY_LANG_US.equals(lang)) {
            result = mTextToSpeech.isLanguageAvailable(Locale.US);
        }
        if (result == TextToSpeech.LANG_COUNTRY_AVAILABLE
                || result == TextToSpeech.LANG_AVAILABLE) {
            request
                    .getCallback()
                    .callback(new Response(CODE_SUCCESS, makeIsLanguageAvailableResult(true)));
        } else if (result == TextToSpeech.LANG_NOT_SUPPORTED) {
            request
                    .getCallback()
                    .callback(new Response(CODE_SUCCESS, makeIsLanguageAvailableResult(false)));
        } else {
            request.getCallback().callback(ERROR);
        }
    }

    private void speak(Request request) {
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.LOLLIPOP) {
            request
                    .getCallback()
                    .callback(new Response(ERROR_CODE_TTS_FEATURE_NOT_AVAILABLE, "api < 21"));
            return;
        }
        JSONObject params = null;
        try {
            params = request.getJSONParams();
        } catch (JSONException e) {
            Log.e(TAG, "JSONException when get params", e);
        }

        if (params == null) {
            request.getCallback()
                    .callback(new Response(CODE_ILLEGAL_ARGUMENT, "get parameter failed"));
            return;
        }
        String lang = params.optString(PARAM_KEY_LANG);
        String content = params.optString(PARAM_KEY_CONTENT);
        if ((!PARAM_KEY_LANG_CN.equals(lang) && !PARAM_KEY_LANG_US.equals(lang))
                || TextUtils.isEmpty(content)) {
            request
                    .getCallback()
                    .callback(new Response(CODE_ILLEGAL_ARGUMENT,
                            "request params is not available!"));
            return;
        }
        if (content.length() >= mTextToSpeech.getMaxSpeechInputLength()) {
            request
                    .getCallback()
                    .callback(
                            new Response(
                                    ERROR_CODE_TTS_CONTENT_LENGTH_LIMIT,
                                    "content length >= "
                                            + mTextToSpeech.getMaxSpeechInputLength()));
            return;
        }

        double rate = params.optDouble(PARAM_KEY_RATE);
        double pitch = params.optDouble(PARAM_KEY_PITCH);
        setTTSRate(rate);
        setTTSPitch(pitch);
        int result =
                mTextToSpeech
                        .setLanguage(PARAM_KEY_LANG_CN.equals(lang) ? Locale.CHINA : Locale.US);
        if (result == TextToSpeech.LANG_COUNTRY_AVAILABLE
                || result == TextToSpeech.LANG_AVAILABLE) {
            String utteranceId = PRE_FIX_UTTERANCE_ID_SPEAK + System.currentTimeMillis();
            int code = mTextToSpeech.speak(content, TextToSpeech.QUEUE_FLUSH, null, utteranceId);
            if (TextToSpeech.SUCCESS == code) {
                request.getCallback()
                        .callback(new Response(CODE_SUCCESS, makeSpeakResult(utteranceId)));
            } else {
                request
                        .getCallback()
                        .callback(
                                new Response(
                                        ERROR_CODE_TTS_INTERNAL_ERROR,
                                        "speak fail, internal code is " + code));
            }
        } else {
            request
                    .getCallback()
                    .callback(
                            new Response(ERROR_CODE_TTS_LANGUAGE_NOT_AVAILABLE,
                                    "the language is not supported"));
        }
    }

    private void textToAudioFile(Request request) {
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.LOLLIPOP) {
            request
                    .getCallback()
                    .callback(new Response(ERROR_CODE_TTS_FEATURE_NOT_AVAILABLE, "api < 21"));
            return;
        }
        JSONObject params = null;
        try {
            params = request.getJSONParams();
        } catch (JSONException e) {
            Log.e(TAG, "JSONException when get params", e);
        }

        if (params == null) {
            request.getCallback()
                    .callback(new Response(CODE_ILLEGAL_ARGUMENT, "get parameter failed"));
            return;
        }
        String lang = params.optString(PARAM_KEY_LANG);
        String content = params.optString(PARAM_KEY_CONTENT);
        if ((!PARAM_KEY_LANG_CN.equals(lang) && !PARAM_KEY_LANG_US.equals(lang))
                || TextUtils.isEmpty(content)) {
            request
                    .getCallback()
                    .callback(new Response(CODE_ILLEGAL_ARGUMENT,
                            "request params is not available!"));
            return;
        }
        if (content.length() >= mTextToSpeech.getMaxSpeechInputLength()) {
            request
                    .getCallback()
                    .callback(
                            new Response(
                                    ERROR_CODE_TTS_CONTENT_LENGTH_LIMIT,
                                    "content length >= "
                                            + mTextToSpeech.getMaxSpeechInputLength()));
            return;
        }

        double rate = params.optDouble(PARAM_KEY_RATE);
        double pitch = params.optDouble(PARAM_KEY_PITCH);
        setTTSRate(rate);
        setTTSPitch(pitch);
        int result =
                mTextToSpeech
                        .setLanguage(PARAM_KEY_LANG_CN.equals(lang) ? Locale.CHINA : Locale.US);
        if (result == TextToSpeech.LANG_COUNTRY_AVAILABLE
                || result == TextToSpeech.LANG_AVAILABLE) {
            try {
                File file =
                        request
                                .getApplicationContext()
                                .createTempFile(PRE_FIX_UTTERANCE_ID_SPEAK_AUDIO, ".wav");
                if (file == null) {
                    request.getCallback().callback(ERROR);
                    return;
                }
                String utteranceId = PRE_FIX_UTTERANCE_ID_SPEAK_AUDIO + System.currentTimeMillis();
                int code = mTextToSpeech.synthesizeToFile(content, null, file, utteranceId);
                if (TextToSpeech.SUCCESS == code) {
                    String resultUri = request.getApplicationContext().getInternalUri(file);
                    request
                            .getCallback()
                            .callback(new Response(
                                    makeTextToAudioFileResult(resultUri, utteranceId)));
                } else {
                    request
                            .getCallback()
                            .callback(
                                    new Response(
                                            ERROR_CODE_TTS_INTERNAL_ERROR,
                                            "textToAudioFile fail, internal code is " + code));
                }
            } catch (IOException e) {
                request
                        .getCallback()
                        .callback(
                                new Response(ERROR_CODE_TTS_LANGUAGE_IO_EXCEPTION,
                                        "IOException when make file"));
            }
        } else {
            request
                    .getCallback()
                    .callback(
                            new Response(ERROR_CODE_TTS_LANGUAGE_NOT_AVAILABLE,
                                    "the language is not supported"));
        }
    }

    @Override
    public void dispose(boolean force) {
        super.dispose(force);
        if (force) {
            releaseTTS();
        }
    }

    @Override
    public String getName() {
        return FEATURE_NAME;
    }

    private void handleEventRequest(Request request) {
        if (request.getCallback().isValid()) {
            putCallbackContext(new TTSCallbackContext(request));
        } else {
            removeCallbackContext(request.getAction());
        }
    }

    private void releaseTTS() {
        Executors.io()
                .execute(
                        new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    mSemaphore.acquire();
                                } catch (InterruptedException e) {
                                    Log.e(TAG, "Semaphore acquire error when releaseTTS", e);
                                    return;
                                }
                                try {
                                    if (mInit && mTextToSpeech != null) {
                                        mTextToSpeech.stop();
                                        mTextToSpeech.shutdown();
                                        mTextToSpeech.setOnUtteranceProgressListener(null);
                                        mInit = false;
                                    }
                                } finally {
                                    mSemaphore.release();
                                }
                            }
                        });
    }

    private void setTTSRate(double rate) {
        rate = rate > 0 ? rate : NORMAL_SPEECH_RATE;
        if (mInit && mTextToSpeech != null) {
            mTextToSpeech.setSpeechRate((float) rate);
        }
    }

    private void setTTSPitch(double pitch) {
        pitch = pitch > 0 ? pitch : NORMAL_PITCH;
        if (mInit && mTextToSpeech != null) {
            mTextToSpeech.setPitch((float) pitch);
        }
    }

    private JSONObject makeIsLanguageAvailableResult(boolean isAvailable) {
        JSONObject result = new JSONObject();
        try {
            result.put(RESULT_IS_AVAILABLE, isAvailable);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
        return result;
    }

    private JSONObject makeSpeakResult(String utteranceId) {
        JSONObject result = new JSONObject();
        try {
            result.put(RESULT_UTTERANCE_ID, utteranceId);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
        return result;
    }

    private JSONObject makeTextToAudioFileResult(String filePath, String utteranceId) {
        JSONObject result = new JSONObject();
        try {
            result.put(RESULT_FILE_PATH, filePath);
            result.put(RESULT_UTTERANCE_ID, utteranceId);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
        return result;
    }

    private class TTSCallbackContext extends CallbackContext {

        public TTSCallbackContext(Request request) {
            super(TextToAudio.this, request.getAction(), request, true);
        }

        @Override
        public void onCreate() {
            super.onCreate();
            switch (getAction()) {
                case EVENT_ON_TTS_STATE_CHANGE:
                    if (mInit && mTextToSpeech != null) {
                        mTextToSpeech.setOnUtteranceProgressListener(
                                new UtteranceProgressListener() {
                                    @Override
                                    public void onStart(String utteranceId) {
                                        runCallbackContext(
                                                EVENT_ON_TTS_STATE_CHANGE,
                                                TTS_CODE_START,
                                                new Response(makeUtteranceProgressResult("onStart",
                                                        utteranceId)));
                                    }

                                    @Override
                                    public void onDone(String utteranceId) {
                                        runCallbackContext(
                                                EVENT_ON_TTS_STATE_CHANGE,
                                                TTS_CODE_DONE,
                                                new Response(makeUtteranceProgressResult("onDone",
                                                        utteranceId)));
                                    }

                                    @Override
                                    public void onStop(String utteranceId, boolean interrupted) {
                                        runCallbackContext(
                                                EVENT_ON_TTS_STATE_CHANGE,
                                                TTS_CODE_STOP,
                                                new Response(makeUtteranceProgressResult("onStop",
                                                        utteranceId)));
                                    }

                                    @Override
                                    public void onError(String utteranceId) {
                                        runCallbackContext(
                                                EVENT_ON_TTS_STATE_CHANGE,
                                                TTS_CODE_ERROR,
                                                new Response(makeUtteranceProgressResult("onError",
                                                        utteranceId)));
                                    }
                                });
                    } else {
                        Log.e(TAG, "tts is not initialized");
                    }
                    break;
                default:
                    break;
            }
        }

        @Override
        public void callback(int what, Object obj) {
            switch (what) {
                case TTS_CODE_START:
                case TTS_CODE_DONE:
                case TTS_CODE_STOP:
                case TTS_CODE_ERROR:
                    mRequest.getCallback().callback((Response) obj);
                    break;
                default:
                    break;
            }
        }

        @Override
        public void onDestroy() {
            super.onDestroy();
            switch (getAction()) {
                case EVENT_ON_TTS_STATE_CHANGE:
                    if (mTextToSpeech != null) {
                        mTextToSpeech.setOnUtteranceProgressListener(null);
                    }
                    break;
                default:
                    break;
            }
        }

        private JSONObject makeUtteranceProgressResult(String state, String utteranceId) {
            JSONObject result = new JSONObject();
            try {
                result.put(RESULT_STATE, state);
                result.put(RESULT_UTTERANCE_ID, utteranceId);
            } catch (JSONException e) {
                Log.e(TAG, "makeUtteranceProgressResult error", e);
            }
            return result;
        }
    }
}
