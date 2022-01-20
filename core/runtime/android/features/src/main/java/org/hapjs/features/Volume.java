/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.features;

import android.content.Context;
import android.media.AudioManager;
import org.hapjs.bridge.FeatureExtension;
import org.hapjs.bridge.Request;
import org.hapjs.bridge.Response;
import org.hapjs.bridge.annotation.ActionAnnotation;
import org.hapjs.bridge.annotation.FeatureExtensionAnnotation;
import org.json.JSONException;
import org.json.JSONObject;

@FeatureExtensionAnnotation(
        name = Volume.FEATURE_NAME,
        actions = {
                @ActionAnnotation(name = Volume.ACTION_SET_MEDIA_VALUE, mode = FeatureExtension.Mode.ASYNC),
                @ActionAnnotation(name = Volume.ACTION_GET_MEDIA_VALUE, mode = FeatureExtension.Mode.ASYNC)
        })
public class Volume extends FeatureExtension {
    protected static final String FEATURE_NAME = "system.volume";
    protected static final String ACTION_SET_MEDIA_VALUE = "setMediaValue";
    protected static final String ACTION_GET_MEDIA_VALUE = "getMediaValue";

    protected static final String PARAM_KEY_VALUE = "value";

    protected static final String RESULT_KEY_VALUE = "value";

    private AudioManager mAudioManager;
    private int mMaxVolume = -1;
    private double mCacheValue = -1;

    @Override
    public Response invokeInner(Request request) throws JSONException {
        String action = request.getAction();
        if (ACTION_SET_MEDIA_VALUE.equals(action)) {
            setMediaVolume(request);
        } else if (ACTION_GET_MEDIA_VALUE.equals(action)) {
            getMediaVolume(request);
        }
        return null;
    }

    private void setMediaVolume(Request request) throws JSONException {
        JSONObject params = new JSONObject(request.getRawParams());
        Context context = request.getNativeInterface().getActivity();
        AudioManager audioManager = getAudioManager(context);
        double value = params.getDouble(PARAM_KEY_VALUE);
        value = Math.max(0, Math.min(value, 1));
        mCacheValue = value;

        int volume = (int) Math.round(value * getMaxVolume(context));
        audioManager
                .setStreamVolume(AudioManager.STREAM_MUSIC, volume, AudioManager.FLAG_PLAY_SOUND);
        request.getCallback().callback(Response.SUCCESS);
    }

    private void getMediaVolume(Request request) throws JSONException {
        Context context = request.getNativeInterface().getActivity();
        AudioManager audioManager = getAudioManager(context);
        int currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);

        double cacheValue = mCacheValue;
        double value =
                Math.round(cacheValue * getMaxVolume(context)) == currentVolume
                        ? cacheValue
                        : (double) currentVolume / getMaxVolume(context);

        JSONObject result = new JSONObject();
        result.put(RESULT_KEY_VALUE, value);
        request.getCallback().callback(new Response(result));
    }

    private int getMaxVolume(Context context) {
        if (mMaxVolume < 0) {
            AudioManager audioManager = getAudioManager(context);
            mMaxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        }
        return mMaxVolume;
    }

    private AudioManager getAudioManager(Context context) {
        if (mAudioManager == null) {
            mAudioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        }
        return mAudioManager;
    }

    @Override
    public String getName() {
        return FEATURE_NAME;
    }
}
