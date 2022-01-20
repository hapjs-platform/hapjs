/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.features.audio;

import android.content.Context;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import org.hapjs.bridge.CallbackContext;
import org.hapjs.bridge.CallbackContextHolder;
import org.hapjs.bridge.CallbackHybridFeature;
import org.hapjs.bridge.FeatureExtension;
import org.hapjs.bridge.Request;
import org.hapjs.bridge.Response;
import org.hapjs.bridge.annotation.ActionAnnotation;
import org.hapjs.bridge.annotation.FeatureExtensionAnnotation;
import org.hapjs.bridge.storage.file.InternalUriUtils;
import org.hapjs.common.net.NetworkReportManager;
import org.hapjs.common.resident.ResidentManager;
import org.hapjs.features.audio.service.MediaNotificationManager;
import org.hapjs.runtime.HapEngine;
import org.json.JSONException;
import org.json.JSONObject;

@FeatureExtensionAnnotation(
        name = Audio.FEATURE_NAME,
        residentType = FeatureExtensionAnnotation.ResidentType.RESIDENT_IMPORTANT,
        actions = {
                @ActionAnnotation(name = Audio.METHOD_PLAY, mode = FeatureExtension.Mode.ASYNC),
                @ActionAnnotation(name = Audio.METHOD_PAUSE, mode = FeatureExtension.Mode.ASYNC),
                @ActionAnnotation(name = Audio.METHOD_STOP, mode = FeatureExtension.Mode.ASYNC),
                @ActionAnnotation(name = Audio.METHOD_GET_PLAY_STATE, mode = FeatureExtension.Mode.ASYNC),
                @ActionAnnotation(
                        name = Audio.ATTR_GET_SRC,
                        mode = FeatureExtension.Mode.SYNC,
                        type = FeatureExtension.Type.ATTRIBUTE,
                        access = FeatureExtension.Access.READ,
                        alias = Audio.ATTR_SRC_ALIAS),
                @ActionAnnotation(
                        name = Audio.ATTR_SET_SRC,
                        mode = FeatureExtension.Mode.SYNC,
                        type = FeatureExtension.Type.ATTRIBUTE,
                        access = FeatureExtension.Access.WRITE,
                        alias = Audio.ATTR_SRC_ALIAS),
                @ActionAnnotation(
                        name = Audio.ATTR_GET_AUTOPLAY,
                        mode = FeatureExtension.Mode.SYNC,
                        type = FeatureExtension.Type.ATTRIBUTE,
                        access = FeatureExtension.Access.READ,
                        alias = Audio.ATTR_AUTOPLAY_ALIAS),
                @ActionAnnotation(
                        name = Audio.ATTR_SET_AUTOPLAY,
                        mode = FeatureExtension.Mode.SYNC,
                        type = FeatureExtension.Type.ATTRIBUTE,
                        access = FeatureExtension.Access.WRITE,
                        alias = Audio.ATTR_AUTOPLAY_ALIAS),
                @ActionAnnotation(
                        name = Audio.ATTR_GET_CURRENTTIME,
                        mode = FeatureExtension.Mode.SYNC,
                        type = FeatureExtension.Type.ATTRIBUTE,
                        access = FeatureExtension.Access.READ,
                        alias = Audio.ATTR_CURRENTTIME_ALIAS),
                @ActionAnnotation(
                        name = Audio.ATTR_SET_CURRENTTIME,
                        mode = FeatureExtension.Mode.SYNC,
                        type = FeatureExtension.Type.ATTRIBUTE,
                        access = FeatureExtension.Access.WRITE,
                        alias = Audio.ATTR_CURRENTTIME_ALIAS),
                @ActionAnnotation(
                        name = Audio.ATTR_GET_DURATION,
                        mode = FeatureExtension.Mode.SYNC,
                        type = FeatureExtension.Type.ATTRIBUTE,
                        access = FeatureExtension.Access.READ,
                        alias = Audio.ATTR_DURATION_ALIAS),
                @ActionAnnotation(
                        name = Audio.ATTR_GET_LOOP,
                        mode = FeatureExtension.Mode.SYNC,
                        type = FeatureExtension.Type.ATTRIBUTE,
                        access = FeatureExtension.Access.READ,
                        alias = Audio.ATTR_LOOP_ALIAS),
                @ActionAnnotation(
                        name = Audio.ATTR_SET_LOOP,
                        mode = FeatureExtension.Mode.SYNC,
                        type = FeatureExtension.Type.ATTRIBUTE,
                        access = FeatureExtension.Access.WRITE,
                        alias = Audio.ATTR_LOOP_ALIAS),
                @ActionAnnotation(
                        name = Audio.ATTR_GET_VOLUME,
                        mode = FeatureExtension.Mode.SYNC,
                        type = FeatureExtension.Type.ATTRIBUTE,
                        access = FeatureExtension.Access.READ,
                        alias = Audio.ATTR_VOLUME_ALIAS),
                @ActionAnnotation(
                        name = Audio.ATTR_SET_VOLUME,
                        mode = FeatureExtension.Mode.SYNC,
                        type = FeatureExtension.Type.ATTRIBUTE,
                        access = FeatureExtension.Access.WRITE,
                        alias = Audio.ATTR_VOLUME_ALIAS),
                @ActionAnnotation(
                        name = Audio.ATTR_GET_MUTED,
                        mode = FeatureExtension.Mode.SYNC,
                        type = FeatureExtension.Type.ATTRIBUTE,
                        access = FeatureExtension.Access.READ,
                        alias = Audio.ATTR_MUTED_ALIAS),
                @ActionAnnotation(
                        name = Audio.ATTR_SET_MUTED,
                        mode = FeatureExtension.Mode.SYNC,
                        type = FeatureExtension.Type.ATTRIBUTE,
                        access = FeatureExtension.Access.WRITE,
                        alias = Audio.ATTR_MUTED_ALIAS),
                @ActionAnnotation(
                        name = Audio.ATTR_GET_NOTIFICATION_VISIBLE,
                        mode = FeatureExtension.Mode.SYNC,
                        type = FeatureExtension.Type.ATTRIBUTE,
                        access = FeatureExtension.Access.READ,
                        alias = Audio.ATTR_NOTIFICATION_VISIBLE_ALIAS),
                @ActionAnnotation(
                        name = Audio.ATTR_SET_NOTIFICATION_VISIBLE,
                        mode = FeatureExtension.Mode.SYNC,
                        type = FeatureExtension.Type.ATTRIBUTE,
                        access = FeatureExtension.Access.WRITE,
                        alias = Audio.ATTR_NOTIFICATION_VISIBLE_ALIAS),
                @ActionAnnotation(
                        name = Audio.ATTR_GET_TITLE,
                        mode = FeatureExtension.Mode.SYNC,
                        type = FeatureExtension.Type.ATTRIBUTE,
                        access = FeatureExtension.Access.READ,
                        alias = Audio.ATTR_TITLE_ALIAS),
                @ActionAnnotation(
                        name = Audio.ATTR_SET_TITLE,
                        mode = FeatureExtension.Mode.SYNC,
                        type = FeatureExtension.Type.ATTRIBUTE,
                        access = FeatureExtension.Access.WRITE,
                        alias = Audio.ATTR_TITLE_ALIAS),
                @ActionAnnotation(
                        name = Audio.ATTR_GET_ARTIST,
                        mode = FeatureExtension.Mode.SYNC,
                        type = FeatureExtension.Type.ATTRIBUTE,
                        access = FeatureExtension.Access.READ,
                        alias = Audio.ATTR_ARTIST_ALIAS),
                @ActionAnnotation(
                        name = Audio.ATTR_SET_ARTIST,
                        mode = FeatureExtension.Mode.SYNC,
                        type = FeatureExtension.Type.ATTRIBUTE,
                        access = FeatureExtension.Access.WRITE,
                        alias = Audio.ATTR_ARTIST_ALIAS),
                @ActionAnnotation(
                        name = Audio.ATTR_GET_COVER,
                        mode = FeatureExtension.Mode.SYNC,
                        type = FeatureExtension.Type.ATTRIBUTE,
                        access = FeatureExtension.Access.READ,
                        alias = Audio.ATTR_COVER_ALIAS),
                @ActionAnnotation(
                        name = Audio.ATTR_SET_COVER,
                        mode = FeatureExtension.Mode.SYNC,
                        type = FeatureExtension.Type.ATTRIBUTE,
                        access = FeatureExtension.Access.WRITE,
                        alias = Audio.ATTR_COVER_ALIAS),
                @ActionAnnotation(
                        name = Audio.EVENT_PLAY,
                        mode = FeatureExtension.Mode.CALLBACK,
                        type = FeatureExtension.Type.EVENT,
                        alias = Audio.EVENT_PLAY_ALIAS),
                @ActionAnnotation(
                        name = Audio.EVENT_PAUSE,
                        mode = FeatureExtension.Mode.CALLBACK,
                        type = FeatureExtension.Type.EVENT,
                        alias = Audio.EVENT_PAUSE_ALIAS),
                @ActionAnnotation(
                        name = Audio.EVENT_LOADEDDATA,
                        mode = FeatureExtension.Mode.CALLBACK,
                        type = FeatureExtension.Type.EVENT,
                        alias = Audio.EVENT_LOADEDDATA_ALIAS),
                @ActionAnnotation(
                        name = Audio.EVENT_ENDED,
                        mode = FeatureExtension.Mode.CALLBACK,
                        type = FeatureExtension.Type.EVENT,
                        alias = Audio.EVENT_ENDED_ALIAS),
                @ActionAnnotation(
                        name = Audio.EVENT_DURATIONCHANGE,
                        mode = FeatureExtension.Mode.CALLBACK,
                        type = FeatureExtension.Type.EVENT,
                        alias = Audio.EVENT_DURATIONCHANGE_ALIAS),
                @ActionAnnotation(
                        name = Audio.EVENT_ERROR,
                        mode = FeatureExtension.Mode.CALLBACK,
                        type = FeatureExtension.Type.EVENT,
                        alias = Audio.EVENT_ERROR_ALIAS),
                @ActionAnnotation(
                        name = Audio.EVENT_TIMEUPDATE,
                        mode = FeatureExtension.Mode.CALLBACK,
                        type = FeatureExtension.Type.EVENT,
                        alias = Audio.EVENT_TIMEUPDATE_ALIAS),
                @ActionAnnotation(
                        name = Audio.EVENT_STOP,
                        mode = FeatureExtension.Mode.CALLBACK,
                        type = FeatureExtension.Type.EVENT,
                        alias = Audio.EVENT_STOP_ALIAS),
                @ActionAnnotation(
                        name = Audio.ATTR_GET_STREAM_TYPE,
                        mode = FeatureExtension.Mode.SYNC,
                        type = FeatureExtension.Type.ATTRIBUTE,
                        access = FeatureExtension.Access.READ,
                        alias = Audio.ATTR_STREAM_TYPE_ALIAS),
                @ActionAnnotation(
                        name = Audio.ATTR_SET_STREAM_TYPE,
                        mode = FeatureExtension.Mode.SYNC,
                        type = FeatureExtension.Type.ATTRIBUTE,
                        access = FeatureExtension.Access.WRITE,
                        alias = Audio.ATTR_STREAM_TYPE_ALIAS),
                @ActionAnnotation(
                        name = Audio.EVENT_PREVIOUS,
                        mode = FeatureExtension.Mode.CALLBACK,
                        type = FeatureExtension.Type.EVENT,
                        alias = Audio.EVENT_PREVIOUS_ALIAS),
                @ActionAnnotation(
                        name = Audio.EVENT_NEXT,
                        mode = FeatureExtension.Mode.CALLBACK,
                        type = FeatureExtension.Type.EVENT,
                        alias = Audio.EVENT_NEXT_ALIAS)
        })
public class Audio extends CallbackHybridFeature {

    protected static final String FEATURE_NAME = "system.audio";
    protected static final String ATTR_SRC_ALIAS = "src";
    protected static final String ATTR_GET_SRC = "__getSrc";
    protected static final String ATTR_SET_SRC = "__setSrc";
    protected static final String ATTR_AUTOPLAY_ALIAS = "autoplay";
    protected static final String ATTR_GET_AUTOPLAY = "__getAutoplay";
    protected static final String ATTR_SET_AUTOPLAY = "__setAutoplay";
    protected static final String ATTR_CURRENTTIME_ALIAS = "currentTime";
    protected static final String ATTR_GET_CURRENTTIME = "__getCurrentTime";
    protected static final String ATTR_SET_CURRENTTIME = "__setCurrentTime";
    protected static final String ATTR_DURATION_ALIAS = "duration";
    protected static final String ATTR_GET_DURATION = "__getDuration";
    protected static final String ATTR_LOOP_ALIAS = "loop";
    protected static final String ATTR_GET_LOOP = "__getLoop";
    protected static final String ATTR_SET_LOOP = "__setLoop";
    protected static final String ATTR_VOLUME_ALIAS = "volume";
    protected static final String ATTR_GET_VOLUME = "__getVolume";
    protected static final String ATTR_SET_VOLUME = "__setVolume";
    protected static final String ATTR_MUTED_ALIAS = "muted";
    protected static final String ATTR_GET_MUTED = "__getMuted";
    protected static final String ATTR_SET_MUTED = "__setMuted";
    protected static final String ATTR_NOTIFICATION_VISIBLE_ALIAS = "notificationVisible";
    protected static final String ATTR_GET_NOTIFICATION_VISIBLE = "__getNotificationVisible";
    protected static final String ATTR_SET_NOTIFICATION_VISIBLE = "__setNotificationVisible";
    protected static final String ATTR_STREAM_TYPE_ALIAS = "streamType";
    protected static final String ATTR_GET_STREAM_TYPE = "__getStreamType";
    protected static final String ATTR_SET_STREAM_TYPE = "__setStreamType";
    protected static final String ATTR_TITLE_ALIAS = "title";
    protected static final String ATTR_SET_TITLE = "__setTitle";
    protected static final String ATTR_GET_TITLE = "__getTitle";
    protected static final String ATTR_ARTIST_ALIAS = "artist";
    protected static final String ATTR_SET_ARTIST = "__setArtist";
    protected static final String ATTR_GET_ARTIST = "__getArtist";
    protected static final String ATTR_COVER_ALIAS = "cover";
    protected static final String ATTR_SET_COVER = "__setCover";
    protected static final String ATTR_GET_COVER = "__getCover";
    // method
    protected static final String METHOD_PLAY = "play";
    protected static final String METHOD_PAUSE = "pause";
    protected static final String METHOD_STOP = "stop";
    protected static final String METHOD_GET_PLAY_STATE = "getPlayState";
    // EVENT
    protected static final String EVENT_PLAY_ALIAS = "onplay";
    protected static final String EVENT_PLAY = "__onplay";
    protected static final String EVENT_PAUSE_ALIAS = "onpause";
    protected static final String EVENT_PAUSE = "__onpause";
    protected static final String EVENT_LOADEDDATA_ALIAS = "onloadeddata";
    protected static final String EVENT_LOADEDDATA = "__onloadeddata";
    protected static final String EVENT_ENDED_ALIAS = "onended";
    protected static final String EVENT_ENDED = "__onended";
    protected static final String EVENT_DURATIONCHANGE_ALIAS = "ondurationchange";
    protected static final String EVENT_DURATIONCHANGE = "__ondurationchange";
    protected static final String EVENT_ERROR_ALIAS = "onerror";
    protected static final String EVENT_ERROR = "__onerror";
    protected static final String EVENT_TIMEUPDATE_ALIAS = "ontimeupdate";
    protected static final String EVENT_TIMEUPDATE = "__ontimeupdate";
    protected static final String EVENT_STOP_ALIAS = "onstop";
    protected static final String EVENT_STOP = "__onstop";
    protected static final String EVENT_PREVIOUS_ALIAS = "onprevious";
    protected static final String EVENT_PREVIOUS = "__onprevious";
    protected static final String EVENT_NEXT_ALIAS = "onnext";
    protected static final String EVENT_NEXT = "__onnext";
    protected static final String STREAM_TYPE_MUSIC = "music";
    protected static final String STREAM_TYPE_VOICE_CALL = "voicecall";
    // result
    protected static final String RESULT_STATE = "state";
    protected static final String RESULT_SRC = "src";
    protected static final String RESULT_CURRENT_TIME = "currentTime";
    protected static final String RESULT_AUTO_PLAY = "autoplay";
    protected static final String RESULT_LOOP = "loop";
    protected static final String RESULT_VOLUME = "volume";
    protected static final String RESULT_MUTED = "muted";
    protected static final String RESULT_NOTIFICATION_VISIBLE = "notificationVisible";
    private static final String TAG = "Audio";
    private static final String DEFAULT_DURATION = "NaN";
    private static final int MSG_PLAY = 1;
    private static final int MSG_PAUSE = 2;
    private static final int MSG_STOP = 3;
    // attr
    private static final String ATTR_DEFAULE_PARAMS_KEY = "value";
    private Object mMediaPlayerLock = new Object();
    private AudioProxy mAudioProxy = null;
    private Handler mHandler;

    public Audio() {
        mHandler =
                new Handler(Looper.getMainLooper()) {
                    @Override
                    public void dispatchMessage(Message msg) {
                        switch (msg.what) {
                            case MSG_PLAY:
                                mAudioProxy.play();
                                break;
                            case MSG_PAUSE:
                                mAudioProxy.pause();
                                break;
                            case MSG_STOP:
                                mAudioProxy.stop(true);
                                break;
                            default:
                                break;
                        }
                    }
                };
    }

    @Override
    protected Response invokeInner(Request request) throws Exception {
        String action = request.getAction();
        checkNullAndInitAudioProxy(request);
        switch (action) {
            case ATTR_GET_SRC: {
                return getSrc();
            }
            case ATTR_SET_SRC: {
                setSrc(request);
                break;
            }
            case ATTR_GET_AUTOPLAY: {
                return getAutoplay();
            }
            case ATTR_SET_AUTOPLAY: {
                setAutoPlay(request);
                break;
            }
            case ATTR_GET_CURRENTTIME: {
                return getCurrentTime();
            }
            case ATTR_SET_CURRENTTIME: {
                setCurrentTime(request);
                break;
            }
            case ATTR_GET_DURATION: {
                return getDuration();
            }
            case ATTR_GET_LOOP: {
                return getLoop();
            }
            case ATTR_SET_LOOP: {
                setLoop(request);
                break;
            }
            case ATTR_GET_VOLUME: {
                return getVolume();
            }
            case ATTR_SET_VOLUME: {
                setVolume(request);
                break;
            }
            case ATTR_GET_MUTED: {
                return getMuted();
            }
            case ATTR_SET_MUTED: {
                setMuted(request);
                break;
            }
            case ATTR_GET_NOTIFICATION_VISIBLE: {
                return getNotificationVisible();
            }
            case ATTR_SET_NOTIFICATION_VISIBLE: {
                setNotificationVisible(request);
                break;
            }
            case ATTR_GET_STREAM_TYPE: {
                return getStreamType();
            }
            case ATTR_SET_STREAM_TYPE: {
                setStreamType(request);
                break;
            }
            case ATTR_SET_TITLE: {
                setTitle(request);
                break;
            }
            case ATTR_GET_TITLE: {
                return getTitle();
            }
            case ATTR_SET_ARTIST: {
                setArtist(request);
                break;
            }
            case ATTR_GET_ARTIST: {
                return getArtist();
            }
            case ATTR_SET_COVER: {
                setCover(request);
                break;
            }
            case ATTR_GET_COVER: {
                return getCover();
            }
            case METHOD_PLAY: {
                handlePlayRequest();
                break;
            }
            case METHOD_PAUSE: {
                handlePauseRequest();
                break;
            }
            case METHOD_STOP: {
                handleStopRequest();
                break;
            }
            case METHOD_GET_PLAY_STATE: {
                getPlayState(request);
                break;
            }
            case EVENT_PLAY:
            case EVENT_PAUSE:
            case EVENT_STOP:
            case EVENT_PREVIOUS:
            case EVENT_NEXT:
            case EVENT_LOADEDDATA:
            case EVENT_ENDED:
            case EVENT_DURATIONCHANGE:
            case EVENT_ERROR:
            case EVENT_TIMEUPDATE: {
                handleEventRequest(request);
                break;
            }
            default: {
                return Response.ERROR;
            }
        }
        return Response.SUCCESS;
    }

    private Response getStreamType() {
        int streamType = mAudioProxy.getStreamType();
        if (streamType == AudioManager.STREAM_MUSIC) {
            return new Response(STREAM_TYPE_MUSIC);
        } else if (streamType == AudioManager.STREAM_VOICE_CALL) {
            return new Response(STREAM_TYPE_VOICE_CALL);
        }
        throw new IllegalStateException("illegal streamType: " + streamType);
    }

    private void setStreamType(Request request) throws JSONException {
        final JSONObject params = request.getJSONParams();
        if (params == null) {
            return;
        }
        String value = params.getString(ATTR_DEFAULE_PARAMS_KEY);
        int streamType;
        if (STREAM_TYPE_MUSIC.equalsIgnoreCase(value)) {
            streamType = AudioManager.STREAM_MUSIC;
        } else if (STREAM_TYPE_VOICE_CALL.equalsIgnoreCase(value)) {
            streamType = AudioManager.STREAM_VOICE_CALL;
        } else {
            Log.e(TAG, "request audio: setStreamType has error params:" + value);
            return;
        }
        mAudioProxy.setStreamType(streamType);
    }

    private Response getMuted() {
        return new Response(mAudioProxy.getMuted());
    }

    private void setMuted(Request request) throws JSONException {
        final JSONObject mutedJson = request.getJSONParams();
        if (mutedJson == null) {
            return;
        }
        boolean muted = mutedJson.getBoolean(ATTR_DEFAULE_PARAMS_KEY);
        mAudioProxy.setMuted(muted);
    }

    private Response getNotificationVisible() {
        return new Response(mAudioProxy.isNotificationEnabled());
    }

    private void setNotificationVisible(Request request) throws JSONException {
        final JSONObject notificationJson = request.getJSONParams();
        if (notificationJson == null) {
            return;
        }
        boolean notificationVisible = notificationJson.getBoolean(ATTR_DEFAULE_PARAMS_KEY);
        mAudioProxy.setNotificationEnabled(notificationVisible);
    }

    private Response getVolume() {
        return new Response(mAudioProxy.getVolume());
    }

    private void setVolume(Request request) throws JSONException {
        final JSONObject volumeJson = request.getJSONParams();
        if (volumeJson == null) {
            return;
        }
        String volumeString = volumeJson.getString(ATTR_DEFAULE_PARAMS_KEY);
        try {
            float result = Float.parseFloat(volumeString);
            mAudioProxy.setVolume(result);
        } catch (NumberFormatException e) {
            Log.e(TAG, "request audio: setVolume has error params:" + volumeString);
        }
    }

    private Response getLoop() {
        return new Response(mAudioProxy.getLoop());
    }

    private void setLoop(Request request) throws JSONException {
        final JSONObject loopJson = request.getJSONParams();
        if (loopJson == null) {
            return;
        }
        boolean loop = loopJson.getBoolean(ATTR_DEFAULE_PARAMS_KEY);
        mAudioProxy.setLoop(loop);
    }

    private Response getDuration() {
        float duration = mAudioProxy.getDuration();
        return new Response(duration == -1 ? DEFAULT_DURATION : duration / 1000);
    }

    private Response getCurrentTime() {
        return new Response(mAudioProxy.getCurrentTime() / 1000f);
    }

    private void setCurrentTime(Request request) throws JSONException {
        final JSONObject currentTimeJson = request.getJSONParams();
        if (currentTimeJson == null) {
            return;
        }
        int currentTime = currentTimeJson.getInt(ATTR_DEFAULE_PARAMS_KEY);
        mAudioProxy.setCurrentTime(currentTime * 1000);
    }

    private Response getAutoplay() {
        return new Response(mAudioProxy.getAutoPlay());
    }

    private void setAutoPlay(Request request) throws JSONException {
        final JSONObject autoPlayJson = request.getJSONParams();
        if (autoPlayJson == null) {
            return;
        }
        boolean autoPlay = autoPlayJson.getBoolean(ATTR_DEFAULE_PARAMS_KEY);
        mAudioProxy.setAutoPlay(autoPlay);
    }

    private Response getSrc() {
        Uri uri = mAudioProxy.getSrc();
        String src = "";
        if (uri != null) {
            src = uri.toString();
        }
        return new Response(src);
    }

    private void setSrc(Request request) throws JSONException {
        final JSONObject srcJson = request.getJSONParams();
        String src;
        if (srcJson == null
                || (src = srcJson.getString(ATTR_DEFAULE_PARAMS_KEY)) == null
                || src.isEmpty()) {
            Log.w(TAG, "src is empty!");
            mAudioProxy.setSrc(null);
            return;
        }

        Uri uri = Uri.parse(src);
        if (uri.getScheme() == null) {
            uri =
                    HapEngine.getInstance(request.getApplicationContext().getPackage())
                            .getResourceManager()
                            .getResource(src);
        } else if (InternalUriUtils.isInternalUri(uri)) {
            uri = request.getApplicationContext().getUnderlyingUri(src);
        } else {
            NetworkReportManager.getInstance().reportNetwork(getName(), uri.toString());
        }
        mAudioProxy.setSrc(uri);
    }

    private Response getTitle() {
        return new Response(mAudioProxy.getTitle());
    }

    private void setTitle(Request request) throws JSONException {
        final JSONObject titleJson = request.getJSONParams();
        if (titleJson == null) {
            return;
        }
        String title = titleJson.optString(ATTR_DEFAULE_PARAMS_KEY);
        mAudioProxy.setTitle(title);
    }

    private Response getArtist() {
        return new Response(mAudioProxy.getArtist());
    }

    private void setArtist(Request request) throws JSONException {
        final JSONObject artistJson = request.getJSONParams();
        if (artistJson == null) {
            return;
        }
        String artist = artistJson.optString(ATTR_DEFAULE_PARAMS_KEY);
        mAudioProxy.setArtist(artist);
    }

    private Response getCover() {
        return new Response(mAudioProxy.getCover());
    }

    private void setCover(Request request) throws JSONException {
        final JSONObject coverJson = request.getJSONParams();
        if (coverJson == null) {
            return;
        }
        String cover = coverJson.optString(ATTR_DEFAULE_PARAMS_KEY);
        Uri coverUri = request.getApplicationContext().getUnderlyingUri(cover);
        if (coverUri == null) {
            Log.e(TAG, "coverUri path:" + cover + " is error!");
            return;
        }
        mAudioProxy.setCover(coverUri);
    }

    private void handlePlayRequest() {
        mHandler.removeMessages(MSG_PLAY);
        mHandler.sendEmptyMessage(MSG_PLAY);
    }

    private void handlePauseRequest() {
        mHandler.removeMessages(MSG_PAUSE);
        mHandler.sendEmptyMessage(MSG_PAUSE);
    }

    private void handleStopRequest() {
        mHandler.removeMessages(MSG_STOP);
        mHandler.sendEmptyMessage(MSG_STOP);
    }

    private void getPlayState(Request request) throws JSONException {
        JSONObject result = new JSONObject();
        String state = mAudioProxy.getTargetPlaybackState();
        String src = getSrc().getContent().toString();
        float currentTime = mAudioProxy.getCurrentTime() / 1000f;
        if ("stop".equals(state)) {
            src = "";
            currentTime = -1;
        }

        result.put(RESULT_STATE, state);
        result.put(RESULT_SRC, src);
        result.put(RESULT_CURRENT_TIME, currentTime);
        result.put(RESULT_AUTO_PLAY, getAutoplay().getContent());
        result.put(RESULT_LOOP, getLoop().getContent());
        result.put(RESULT_VOLUME, getVolume().getContent());
        result.put(RESULT_MUTED, getMuted().getContent());
        result.put(RESULT_NOTIFICATION_VISIBLE, getNotificationVisible().getContent());

        request.getCallback().callback(new Response(result));
    }

    private void handleEventRequest(Request request) {
        if (request.getCallback().isValid()) {
            PlayCallbackContext callbackContext = new PlayCallbackContext(this, request, true);
            putCallbackContext(callbackContext);
        } else {
            removeCallbackContext(request.getAction());
        }
    }

    private void checkNullAndInitAudioProxy(Request request) {
        String pkg = request.getApplicationContext().getPackage();
        if (mAudioProxy == null) {
            mAudioProxy =
                    createAudioProxy(
                            request.getApplicationContext().getContext(),
                            pkg,
                            request.getNativeInterface().getResidentManager());
        }
        if (pkg == null || !pkg.equals(mAudioProxy.getPackage())) {
            throw new IllegalStateException("request audio: package null or illegal");
        }
    }

    protected AudioProxy createAudioProxy(
            Context context, String pkg, ResidentManager residentManager) {
        return new AudioProxyImpl(context, pkg, this, residentManager);
    }

    @Override
    public String getName() {
        return FEATURE_NAME;
    }

    @Override
    public void onStopRunningInBackground() {
        handlePauseRequest();
    }

    @Override
    public boolean hasShownForegroundNotification() {
        return mAudioProxy.isNotificationEnabled();
    }

    @Override
    public String getForegroundNotificationStopAction() {
        return MediaNotificationManager.ACTION_STOP;
    }

    private class PlayCallbackContext extends CallbackContext
            implements AudioProxy.OnPlayListener,
            AudioProxy.OnPauseListener,
            AudioProxy.OnLoadedDataListener,
            AudioProxy.OnEndedListener,
            AudioProxy.OnDurationChangeListener,
            AudioProxy.OnErrorListener,
            AudioProxy.OnTimeUpdateListener,
            AudioProxy.OnStopListener,
            AudioProxy.OnPreviousListener,
            AudioProxy.OnNextListener {

        private static final int CODE_PLAY = 1;
        private static final int CODE_PAUSE = 2;
        private static final int CODE_LOADEDDATA = 3;
        private static final int CODE_ENDED = 4;
        private static final int CODE_DURATIONCHANGE = 5;
        private static final int CODE_ERROR = 6;
        private static final int CODE_TIMEUPDATA = 7;
        private static final int CODE_STOP = 8;
        private static final int CODE_PREVIOUS = 9;
        private static final int CODE_NEXT = 10;

        public PlayCallbackContext(CallbackContextHolder holder, Request request,
                                   boolean reserved) {
            super(holder, request.getAction(), request, reserved);
        }

        @Override
        public void onCreate() {
            super.onCreate();
            synchronized (mMediaPlayerLock) {
                String action = getAction();
                switch (action) {
                    case EVENT_PLAY: {
                        mAudioProxy.setOnPlayListener(this);
                        break;
                    }
                    case EVENT_PAUSE: {
                        mAudioProxy.setOnPauseListener(this);
                        break;
                    }
                    case EVENT_LOADEDDATA: {
                        mAudioProxy.setOnLoadedDataListener(this);
                        break;
                    }
                    case EVENT_ENDED: {
                        mAudioProxy.setOnEndedListener(this);
                        break;
                    }
                    case EVENT_DURATIONCHANGE: {
                        mAudioProxy.setOnDurationChangeListener(this);
                        break;
                    }
                    case EVENT_ERROR: {
                        mAudioProxy.setOnErrorListener(this);
                        break;
                    }
                    case EVENT_TIMEUPDATE: {
                        mAudioProxy.setOnTimeUpdateListener(this);
                        break;
                    }
                    case EVENT_STOP: {
                        mAudioProxy.setOnStopListener(this);
                        break;
                    }
                    case EVENT_PREVIOUS: {
                        mAudioProxy.setOnPreviousListener(this);
                        break;
                    }
                    case EVENT_NEXT: {
                        mAudioProxy.setOnNextListener(this);
                        break;
                    }
                    default:
                        break;
                }
            }
        }

        @Override
        public void callback(int what, Object obj) {
            switch (what) {
                case CODE_PLAY:
                case CODE_PAUSE:
                case CODE_STOP:
                case CODE_PREVIOUS:
                case CODE_NEXT:
                case CODE_LOADEDDATA:
                case CODE_ENDED:
                case CODE_DURATIONCHANGE:
                case CODE_ERROR:
                case CODE_TIMEUPDATA: {
                    getRequest().getCallback().callback((Response) obj);
                    break;
                }
                default:
                    break;
            }
        }

        @Override
        public void onDestroy() {
            super.onDestroy();
            synchronized (mMediaPlayerLock) {
                String action = getAction();
                switch (action) {
                    case EVENT_PLAY: {
                        mAudioProxy.setOnPlayListener(null);
                        break;
                    }
                    case EVENT_PAUSE: {
                        mAudioProxy.setOnPauseListener(null);
                        break;
                    }
                    case EVENT_LOADEDDATA: {
                        mAudioProxy.setOnLoadedDataListener(null);
                        break;
                    }
                    case EVENT_ENDED: {
                        mAudioProxy.setOnEndedListener(null);
                        break;
                    }
                    case EVENT_DURATIONCHANGE: {
                        mAudioProxy.setOnDurationChangeListener(null);
                        break;
                    }
                    case EVENT_ERROR: {
                        mAudioProxy.setOnErrorListener(null);
                        break;
                    }
                    case EVENT_TIMEUPDATE: {
                        mAudioProxy.setOnTimeUpdateListener(null);
                        break;
                    }
                    case EVENT_STOP: {
                        mAudioProxy.setOnStopListener(null);
                        break;
                    }
                    case EVENT_PREVIOUS: {
                        mAudioProxy.setOnPreviousListener(null);
                        break;
                    }
                    case EVENT_NEXT: {
                        mAudioProxy.setOnNextListener(null);
                        break;
                    }
                    default:
                        break;
                }
            }
        }

        @Override
        public void onPlay() {
            runCallbackContext(EVENT_PLAY, CODE_PLAY, Response.SUCCESS);
        }

        @Override
        public void onPause() {
            runCallbackContext(EVENT_PAUSE, CODE_PAUSE, Response.SUCCESS);
        }

        @Override
        public void onLoadedData() {
            runCallbackContext(EVENT_LOADEDDATA, CODE_LOADEDDATA, Response.SUCCESS);
        }

        @Override
        public void onEnded() {
            runCallbackContext(EVENT_ENDED, CODE_ENDED, Response.SUCCESS);
        }

        @Override
        public void onDurationChange(int currentDuration) {
            runCallbackContext(EVENT_DURATIONCHANGE, CODE_DURATIONCHANGE, Response.SUCCESS);
        }

        @Override
        public void onError() {
            runCallbackContext(EVENT_ERROR, CODE_ERROR, Response.SUCCESS);
        }

        @Override
        public void onTimeUpdateListener() {
            runCallbackContext(EVENT_TIMEUPDATE, CODE_TIMEUPDATA, Response.SUCCESS);
        }

        @Override
        public void onStop() {
            runCallbackContext(EVENT_STOP, CODE_STOP, Response.SUCCESS);
        }

        @Override
        public void onPrevious() {
            runCallbackContext(EVENT_PREVIOUS, CODE_PREVIOUS, Response.SUCCESS);
            getRequest().getNativeInterface().getResidentManager().postRunAShortTime();
        }

        @Override
        public void onNext() {
            runCallbackContext(EVENT_NEXT, CODE_NEXT, Response.SUCCESS);
            getRequest().getNativeInterface().getResidentManager().postRunAShortTime();
        }
    }
}
