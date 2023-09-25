/*
 * Copyright (C) 2023, hapjs.org. All rights reserved.
 */
package org.hapjs.runtime.sandbox;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import org.hapjs.logging.RuntimeLogManager;
import org.hapjs.render.jsruntime.SandboxProvider;
import org.hapjs.runtime.ProviderManager;

public class SandboxLogHelper {
    private static final String TAG = "SandboxLogHelper";

    private static final long SLOW_MESSAGE_THRESHOLD = 100;
    private static final long HEART_BEAT_DELAY = 1000;
    private static final long HEART_BEAT_TIMEOUT_THRESHOLD = 1000;

    public static class PositiveChannelStatHelper {
        private ChannelSender mChannel;
        private Handler mSenderHandler;
        private Handler mMainHandler;
        private boolean mHeartBeatWaiting;

        public PositiveChannelStatHelper(ChannelSender channel, Handler handler) {
            mChannel = channel;
            mSenderHandler = handler;
            mMainHandler = new Handler(Looper.getMainLooper());
        }

        public void scheduleHeartBeat(String pkg) {
            if (mHeartBeatWaiting) {
                return;
            }

            mHeartBeatWaiting = true;
            mSenderHandler.postDelayed(() -> {
                long start = System.currentTimeMillis();
                Runnable recordSlowHeartbeat = () -> {
                    Log.w(TAG, "heartbeat response too slow");
                    RuntimeLogManager.getDefault().recordSandboxMessageSlow(pkg, SandboxIpcMethods.HEART_BEAT, 0,
                            System.currentTimeMillis() - start);
                };
                mMainHandler.postDelayed(recordSlowHeartbeat, HEART_BEAT_TIMEOUT_THRESHOLD);

                debugLog(TAG, "heartbeat starts");
                mChannel.invokeSync(SandboxIpcMethods.HEART_BEAT, boolean.class);
                debugLog(TAG, "heartbeat ends");

                mHeartBeatWaiting = false;
                mMainHandler.removeCallbacks(recordSlowHeartbeat);
                if (System.currentTimeMillis() - start > HEART_BEAT_TIMEOUT_THRESHOLD) {
                    Log.w(TAG, "heartbeat response two slow: " + (System.currentTimeMillis() - start));
                    RuntimeLogManager.getDefault().recordSandboxMessageSlow(pkg, SandboxIpcMethods.HEART_BEAT, 0,
                            System.currentTimeMillis() - start);
                }
            }, HEART_BEAT_DELAY);
        }
    }

    public static void onChannelReceive(String pkg, String method, int dataSize, long startStamp) {
        long now = System.currentTimeMillis();
        long timeCost = now - startStamp;
        if (timeCost > SLOW_MESSAGE_THRESHOLD) {
            RuntimeLogManager.getDefault().recordSandboxMessageSlow(pkg, method, dataSize, timeCost);
        }
    }

    private static void debugLog(String tag, String msg) {
        SandboxProvider sandboxProvider = ProviderManager.getDefault().getProvider(SandboxProvider.NAME);
        if (sandboxProvider != null && sandboxProvider.isDebugLogEnabled()) {
            Log.d(tag, msg);
        }
    }
}
