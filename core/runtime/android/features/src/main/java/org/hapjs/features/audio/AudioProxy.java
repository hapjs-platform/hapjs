/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.features.audio;

import android.content.ComponentName;
import android.content.Context;
import android.net.Uri;

public interface AudioProxy {

    void play();

    void pause();

    void stop(boolean isRemoveNotification);

    Uri getSrc();

    void setSrc(Uri uri);

    float getVolume();

    void setVolume(float volume);

    boolean getLoop();

    void setLoop(boolean loop);

    boolean getMuted();

    void setMuted(boolean muted);

    boolean getAutoPlay();

    void setAutoPlay(boolean autoplay);

    float getCurrentTime();

    void setCurrentTime(float millisecond);

    float getDuration();

    boolean isNotificationEnabled();

    void setNotificationEnabled(boolean enabled);

    int getStreamType();

    void setStreamType(int streamType);

    String getPackage();

    String getTitle();

    void setTitle(String title);

    String getArtist();

    void setArtist(String artist);

    String getCover();

    void setCover(Uri coverUri);

    String getTargetPlaybackState();

    void reloadNotification();

    void setOnPlayListener(OnPlayListener l);

    void setOnPauseListener(OnPauseListener l);

    void setOnLoadedDataListener(OnLoadedDataListener l);

    void setOnEndedListener(OnEndedListener l);

    void setOnDurationChangeListener(OnDurationChangeListener l);

    void setOnErrorListener(OnErrorListener l);

    void setOnTimeUpdateListener(OnTimeUpdateListener l);

    void setOnStopListener(OnStopListener l);

    void setOnPreviousListener(OnPreviousListener l);

    void setOnNextListener(OnNextListener l);

    interface ServiceInfoCallback {
        ComponentName getServiceComponentName(Context context);
    }

    /**
     * event listener *
     */
    interface OnPlayListener {
        void onPlay();
    }

    interface OnPauseListener {
        void onPause();
    }

    interface OnLoadedDataListener {
        void onLoadedData();
    }

    interface OnEndedListener {
        void onEnded();
    }

    interface OnDurationChangeListener {
        void onDurationChange(int currentDuration);
    }

    interface OnErrorListener {
        void onError();
    }

    interface OnTimeUpdateListener {
        void onTimeUpdateListener();
    }

    interface OnStopListener {
        void onStop();
    }

    interface OnPreviousListener {
        void onPrevious();
    }

    interface OnNextListener {
        void onNext();
    }
}
