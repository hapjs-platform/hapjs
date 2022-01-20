/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.widgets.view.image.provider;

import android.os.Handler;
import android.util.SparseArray;
import java.lang.ref.WeakReference;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class TileDecoder {

    public static final int MSG_DECODE_SUCCESS = 1;
    public static final int MSG_DECODE_FAIL = 2;
    private static final String TAG = "TileDecoder";
    private final Object mLock = new Object();
    private TileDecodeRunnable mDecodeRunnable;
    private WeakReference<Handler> mDecodeHandlerRef;
    private WeakReference<TileProvider> mDecodeProviderRef;
    private BlockingQueue<Tile> mDecodeQueue = new LinkedBlockingQueue<>();
    private volatile SparseArray<Tile> mCurrentDecodingTiles = new SparseArray<>();

    public TileDecoder(Handler handler) {
        mDecodeHandlerRef = new WeakReference<>(handler);
    }

    public void createOrRunDecoder() {
        if (mDecodeRunnable == null) {
            mDecodeRunnable = new TileDecodeRunnable();
        }
        if (!mDecodeRunnable.isRunning()) {
            mDecodeRunnable.run();
        }
    }

    public boolean put(Tile tile) {
        return tile != null && mDecodeQueue.offer(tile);
    }

    public void cancel() {
        if (mDecodeRunnable != null) {
            mDecodeRunnable.cancel();
        }
        mDecodeRunnable = null;
        mDecodeQueue.clear();
        synchronized (mLock) {
            mCurrentDecodingTiles.clear();
        }
    }

    public boolean isRunning() {
        return mDecodeRunnable != null && mDecodeRunnable.isRunning();
    }

    public void clear() {
        mDecodeQueue.clear();
    }

    public Tile getDecodingTile(int key) {
        synchronized (mLock) {
            return mCurrentDecodingTiles.get(key);
        }
    }

    public void removeDecodingTile(int key) {
        synchronized (mLock) {
            mCurrentDecodingTiles.remove(key);
        }
    }

    public void setTileProvider(TileProvider provider) {
        mDecodeProviderRef = new WeakReference<>(provider);
    }

    private TileProvider getProvider() {
        return mDecodeProviderRef != null ? mDecodeProviderRef.get() : null;
    }

    private Handler getHandler() {
        return mDecodeHandlerRef != null ? mDecodeHandlerRef.get() : null;
    }

    private class TileDecodeRunnable implements Runnable {

        private static final int STATE_IDLE = -1;
        private static final int STATE_RUNNING = 0;
        private static final int TIME_OUT = 10;
        private volatile int mState = STATE_IDLE;

        public synchronized void cancel() {
            if (mState == STATE_IDLE) {
                return;
            }
            mState = STATE_IDLE;
        }

        public boolean isRunning() {
            return mState == STATE_RUNNING;
        }

        @Override
        public void run() {
            mState = STATE_RUNNING;
            runInternal();
            mState = STATE_IDLE;
        }

        private void runInternal() {
            while (mState == STATE_RUNNING) {
                try {
                    Tile tile = mDecodeQueue.poll(TIME_OUT, TimeUnit.MILLISECONDS);
                    if (tile != null) {
                        if (!tile.isActive()) {
                            continue;
                        }
                        if (mState == STATE_IDLE) {
                            mDecodeQueue.clear();
                            return;
                        }
                        int tileKey = TileManager.makeHashKey(tile.getTileRect());
                        synchronized (mLock) {
                            mCurrentDecodingTiles.put(tileKey, tile);
                        }
                        boolean result = tile.decode(getProvider());
                        Handler handler = getHandler();
                        if (handler != null) {
                            handler
                                    .obtainMessage(result ? MSG_DECODE_SUCCESS : MSG_DECODE_FAIL,
                                            tile)
                                    .sendToTarget();
                        }
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            TileProvider provider = getProvider();
            if (provider != null) {
                provider.release();
            }
            return;
        }
    }
}
