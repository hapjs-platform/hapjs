/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.widgets.view.image.provider;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.SparseArray;
import android.view.View;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import org.hapjs.common.utils.ThreadUtils;

public class TileManager {

    private static final String TAG = "TileManager";

    private static final int TILE_SIZE = 1024;

    private TileProvider mProvider;
    private Rect mViewPort = new Rect();
    private Rect mTileRange = new Rect();
    private Rect mTempTileRect = new Rect();
    private RectF mTempDisplayRect = new RectF();
    private TileDecoder mDecodeManager;
    private TileDecodeHandler mDecodeHandler;
    private WeakReference<View> mAttachViewRef;
    private int mRefreshId = -1;

    private SparseArray<Tile> mDrawingTiles = new SparseArray<>();
    private SparseArray<Tile> mTempDrawingTiles = new SparseArray<>();
    private List<Tile> mTempDecodeTiles = new ArrayList<>();

    public TileManager(View attachView) {
        mProvider = new TileProvider();
        mAttachViewRef = new WeakReference<>(attachView);
        mDecodeHandler = new TileDecodeHandler(Looper.getMainLooper());
        mDecodeManager = new TileDecoder(mDecodeHandler);
        mDecodeManager.setTileProvider(mProvider);
    }

    public static int makeHashKey(Rect rect) {
        if (rect == null) {
            return 0;
        } else {
            int result = 17;
            result = 31 * result + rect.left;
            result = 31 * result + rect.top;
            result = 31 * result + rect.right;
            result = 31 * result + rect.bottom;
            return result;
        }
    }

    private boolean invalidate() {
        View view = getAttachView();
        if (view != null) {
            view.invalidate();
            return true;
        }
        return false;
    }

    public void invalidate(RectF displayRect) {
        layoutTiles(displayRect);
    }

    public void setTileDataStream(InputStream stream) {
        mProvider.setInputStream(stream);
    }

    public void setViewPort(Rect viewPort) {
        mViewPort.set(viewPort);
    }

    public void runDecoder() {
        if (ThreadUtils.isInMainThread()) {
            throw new RuntimeException("start decoder runnable must in work thread!");
        }
        if (mDecodeManager != null) {
            mDecodeManager.createOrRunDecoder();
        }
    }

    public boolean isDecoderRunning() {
        return mDecodeManager.isRunning();
    }

    public void clearUp() {
        mDecodeManager.cancel();
        mDecodeHandler.removeCallbacksAndMessages(null);
        int size = mDrawingTiles.size();
        for (int i = 0; i < size; i++) {
            mDrawingTiles.valueAt(i).recycle();
        }
        mDrawingTiles.clear();
        if (size > 0) {
            invalidate();
        }
    }

    public void draw(Canvas canvas, Paint paint) {
        int size = mDrawingTiles.size();
        if (size == 0) {
            return;
        }
        int saveCount = canvas.getSaveCount();
        for (int i = 0; i < size; i++) {
            Tile tile = mDrawingTiles.valueAt(i);
            if (tile.getRefreshId() == mRefreshId) {
                tile.draw(canvas, paint);
            }
        }
        if (saveCount > 0) {
            canvas.restoreToCount(saveCount);
        }
    }

    private int calculateInSampleSize(RectF displayRect) {
        int inSampleSize = 1;

        final int halfWidth = mProvider.getImageWidth() / 2;
        final int halfHeight = mProvider.getImageHeight() / 2;
        while ((halfWidth / inSampleSize) > displayRect.width()
                || (halfHeight / inSampleSize) > displayRect.height()) {
            inSampleSize *= 2;
        }

        return inSampleSize;
    }

    private void layoutTiles(RectF displayRect) {
        if (displayRect == null) {
            return;
        }
        mDecodeManager.clear();

        int sampleSize = calculateInSampleSize(displayRect);
        float scaleX = mProvider.getImageWidth() / displayRect.width();
        float scaleY = mProvider.getImageHeight() / displayRect.height();
        int left = (int) (mViewPort.left * scaleX);
        int top = (int) (mViewPort.top * scaleY);
        int right = (int) (left + mViewPort.width() * scaleX);
        int bottom = (int) (top + mViewPort.height() * scaleY);
        mTileRange.set(
                floor(left, sampleSize),
                floor(top, sampleSize),
                ceil(right, sampleSize),
                ceil(bottom, sampleSize));
        float displayX = mTileRange.left / scaleX + displayRect.left;
        float displayY = mTileRange.top / scaleY + displayRect.top;

        refreshTiles(
                displayX,
                displayY,
                TILE_SIZE * sampleSize / scaleX,
                TILE_SIZE * sampleSize / scaleY,
                sampleSize);
    }

    private void refreshTiles(
            float displayX, float displayY, float displaySizeX, float displaySizeY,
            int sampleSize) {
        increaseRefreshId();
        float loopX = displayX;
        float loopY = displayY;
        for (int y = mTileRange.top; y < mTileRange.bottom; y += TILE_SIZE * sampleSize) {
            for (int x = mTileRange.left; x < mTileRange.right; x += TILE_SIZE * sampleSize) {

                mTempTileRect.set(x, y, x + TILE_SIZE * sampleSize, y + TILE_SIZE * sampleSize);
                mTempDisplayRect.set(loopX, loopY, loopX + displaySizeX, loopY + displaySizeY);
                int key = makeHashKey(mTempTileRect);

                Tile tile = mDrawingTiles.get(key);
                if (tile != null) {
                    tile.updateDisplayParam(mTempDisplayRect);
                    mTempDrawingTiles.put(key, tile);
                } else {
                    tile = mDecodeManager.getDecodingTile(key);
                    if (tile != null) {
                        tile.updateDisplayParam(mTempDisplayRect);
                    } else {
                        tile = Tile.obtain();
                        tile.updateTileParam(mTempTileRect, sampleSize);
                        tile.updateDisplayParam(mTempDisplayRect);
                        mTempDecodeTiles.add(tile);
                    }
                }
                tile.setRefreshId(mRefreshId);
                loopX += displaySizeX;
            }
            loopX = displayX;
            loopY += displaySizeY;
        }
        int size = mDrawingTiles.size();
        for (int i = 0; i < size; i++) {
            if (mTempDrawingTiles.get(mDrawingTiles.keyAt(i)) == null) {
                Tile tile = mDrawingTiles.valueAt(i);
                tile.recycle();
            }
        }

        mDrawingTiles.clear();
        size = mTempDrawingTiles.size();
        for (int i = 0; i < size; i++) {
            Tile tile = mTempDrawingTiles.valueAt(i);
            mDrawingTiles.put(makeHashKey(tile.getTileRect()), tile);
        }

        if (mDrawingTiles.size() > 0) {
            invalidate();
        }

        for (Tile tile : mTempDecodeTiles) {
            mDecodeManager.put(tile);
        }
        mTempDrawingTiles.clear();
        mTempDecodeTiles.clear();
    }

    private void increaseRefreshId() {
        if (mRefreshId == Integer.MAX_VALUE) {
            mRefreshId = -1;
        }
        mRefreshId++;
    }

    private int floor(int value, int sampleSize) {
        int i = 0;
        double middleValue = 0;
        double desValue = (double) value;
        if (sampleSize > 0) {
            if (desValue > 0) {
                middleValue = desValue / (TILE_SIZE * sampleSize);
                i = (int) Math.floor(middleValue) + 1;
            } else {
                i = 1;
            }
        } else {
            i = 1;
        }
        return (i - 1) * TILE_SIZE * sampleSize;
    }

    private int ceil(int value, int sampleSize) {
        int i = 0;
        double middleValue = 0;
        double desValue = (double) value;
        if (sampleSize > 0) {
            if (desValue > 0) {
                middleValue = desValue / (TILE_SIZE * sampleSize);
                if (middleValue % 1 > 0) {
                    i = (int) Math.floor(middleValue) + 1;
                } else {
                    i = (int) Math.floor(middleValue);
                }
            } else {
                i = 0;
            }
        } else {
            i = 0;
        }
        return i * TILE_SIZE * sampleSize;
    }

    private View getAttachView() {
        View view = mAttachViewRef != null ? mAttachViewRef.get() : null;
        if (view == null) {
            clearUp();
        }
        return view;
    }

    private class TileDecodeHandler extends Handler {

        TileDecodeHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case TileDecoder.MSG_DECODE_SUCCESS:
                    if (msg.obj != null) {
                        Tile tile = (Tile) msg.obj;
                        int tileKey = makeHashKey(tile.getTileRect());
                        mDecodeManager.removeDecodingTile(tileKey);
                        if (tile.isActive()) {
                            mDrawingTiles.put(tileKey, tile);
                            invalidate();
                        } else {
                            tile.recycle();
                        }
                    }
                    break;
                case TileDecoder.MSG_DECODE_FAIL:
                    if (msg.obj != null) {
                        Tile tile = (Tile) msg.obj;
                        int tileKey = makeHashKey(tile.getTileRect());
                        mDecodeManager.removeDecodingTile(tileKey);
                    }
                    break;
                default:
                    break;
            }
        }
    }
}
