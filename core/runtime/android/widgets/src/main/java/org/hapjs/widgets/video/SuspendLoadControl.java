/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.widgets.video;

import androidx.annotation.NonNull;
import com.google.android.exoplayer2.DefaultLoadControl;
import com.google.android.exoplayer2.LoadControl;
import com.google.android.exoplayer2.Renderer;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.trackselection.TrackSelectionArray;
import com.google.android.exoplayer2.upstream.Allocator;

public class SuspendLoadControl implements LoadControl {
    private LoadControl mBaseLoadControl;

    private boolean mSuspendBuffering = false;

    public SuspendLoadControl() {
        this(new DefaultLoadControl());
    }

    public SuspendLoadControl(@NonNull LoadControl loadControl) {
        mBaseLoadControl = loadControl;
    }

    @Override
    public void onPrepared() {
        mBaseLoadControl.onPrepared();
    }

    @Override
    public void onTracksSelected(
            Renderer[] renderers, TrackGroupArray trackGroups,
            TrackSelectionArray trackSelections) {
        mBaseLoadControl.onTracksSelected(renderers, trackGroups, trackSelections);
    }

    @Override
    public void onStopped() {
        mBaseLoadControl.onStopped();
    }

    @Override
    public void onReleased() {
        mBaseLoadControl.onReleased();
    }

    @Override
    public Allocator getAllocator() {
        return mBaseLoadControl.getAllocator();
    }

    @Override
    public long getBackBufferDurationUs() {
        return mBaseLoadControl.getBackBufferDurationUs();
    }

    @Override
    public boolean retainBackBufferFromKeyframe() {
        return mBaseLoadControl.retainBackBufferFromKeyframe();
    }

    @Override
    public boolean shouldContinueLoading(long bufferedDurationUs, float playbackSpeed) {
        if (mSuspendBuffering) {
            return false;
        }
        return mBaseLoadControl.shouldContinueLoading(bufferedDurationUs, playbackSpeed);
    }

    @Override
    public boolean shouldStartPlayback(
            long bufferedDurationUs, float playbackSpeed, boolean rebuffering) {
        return mBaseLoadControl.shouldStartPlayback(bufferedDurationUs, playbackSpeed, rebuffering);
    }

    /**
     * 暂停缓冲控制
     *
     * @param suspendBuffering true/false:暂定缓冲/恢复缓冲
     */
    public void setSuspendBuffering(boolean suspendBuffering) {
        mSuspendBuffering = suspendBuffering;
    }
}
