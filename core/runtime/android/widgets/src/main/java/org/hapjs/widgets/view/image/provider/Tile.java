/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.widgets.view.image.provider;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import androidx.core.util.Pools;
import org.hapjs.common.utils.BitmapUtils;

public class Tile {

    private static final String TAG = "Tile";

    private static final int STATE_ACTIVE = 0x00;
    private static final int STATE_RECYCLE = 0x02;
    private static final int STATE_WHETHER_ACTIVE_MASK = 0x03;
    private static final int STATE_DECODE_SUCCESS = 0x04;
    private static final int STATE_DECODE_FAIL = 0x08;
    private static final int STATE_WHETHER_DECODED_MASK = 0x0c;
    private static final int MAX_POOL_SIZE = 15;

    private static final Pools.SynchronizedPool<Tile> sPool =
            new Pools.SynchronizedPool<>(MAX_POOL_SIZE);
    private static Paint sPaint;

    private Rect mTileRect = new Rect();
    private RectF mDisplayRect = new RectF();
    private TileData mTileData;
    private int mState;
    private int mSampleSize;
    private int mRefreshId = -1;

    public static Tile obtain() {
        Tile instance = sPool.acquire();
        return (instance != null) ? instance : new Tile();
    }

    private static Paint getPaint() {
        if (sPaint == null) {
            sPaint = new Paint();
        }
        return sPaint;
    }

    private boolean isBitmapValidate() {
        return mTileData != null && BitmapUtils.isValidate(mTileData.mBitmap);
    }

    private void setDecoded() {
        mState &= ~STATE_WHETHER_DECODED_MASK;
        if (isBitmapValidate()) {
            mState |= STATE_DECODE_SUCCESS;
        } else {
            mState |= STATE_DECODE_FAIL;
        }
    }

    public void updateDisplayParam(RectF displayRect) {
        mDisplayRect.set(displayRect);
        setActive(true);
    }

    public void updateTileParam(Rect tileRect, int sampleSize) {
        mTileRect.set(tileRect);
        mSampleSize = sampleSize;
    }

    public int getRefreshId() {
        return mRefreshId;
    }

    public void setRefreshId(int refreshId) {
        mRefreshId = refreshId;
    }

    public boolean decode(TileProvider provider) {
        if (provider != null) {
            mTileData = provider.createTile(mTileRect, mSampleSize);
        }
        setDecoded();
        return isBitmapValidate();
    }

    public void recycle() {
        if (mTileData != null) {
            mTileData.recycle();
            mTileData = null;
        }
        setActive(false);
        setDecoded();
        mRefreshId = -1;
        sPool.release(this);
    }

    public boolean draw(Canvas canvas, Paint paint) {
        if (isBitmapValidate()) {
            if (paint == null) {
                paint = getPaint();
            }
            // calculate display size
            float realDisplayWidth =
                    mDisplayRect.width() * mTileData.mValidateWidth * mSampleSize
                            / mTileRect.width();
            float realDisplayHeight =
                    mDisplayRect.height() * mTileData.mValidateHeight * mSampleSize
                            / mTileRect.height();
            mTileData.sDisplayRect.set(
                    mDisplayRect.left,
                    mDisplayRect.top,
                    mDisplayRect.left + realDisplayWidth,
                    mDisplayRect.top + realDisplayHeight);

            mTileData.sSrcTileRect.set(0, 0, mTileData.mValidateWidth, mTileData.mValidateHeight);
            canvas.drawBitmap(mTileData.mBitmap, mTileData.sSrcTileRect, mTileData.sDisplayRect,
                    paint);
            return true;
        }
        return false;
    }

    public Rect getTileRect() {
        return mTileRect;
    }

    public boolean isActive() {
        return (mState & STATE_WHETHER_ACTIVE_MASK) == STATE_ACTIVE;
    }

    private void setActive(boolean active) {
        // clear active bits
        mState &= ~STATE_WHETHER_ACTIVE_MASK;
        if (active) {
            mState |= STATE_ACTIVE;
        } else {
            mState |= STATE_RECYCLE;
        }
    }

    public static class TileData {
        private static final Rect sSrcTileRect = new Rect();
        private static final RectF sDisplayRect = new RectF();
        private static final Pools.SynchronizedPool<TileData> sTileDataPool =
                new Pools.SynchronizedPool<>(MAX_POOL_SIZE);
        private Bitmap mBitmap;
        private int mValidateWidth;
        private int mValidateHeight;

        public static TileData obtain() {
            TileData instance = sTileDataPool.acquire();
            return (instance != null) ? instance : new TileData();
        }

        public void setValidateWidth(int width) {
            mValidateWidth = width;
        }

        public void setValidateHeight(int height) {
            mValidateHeight = height;
        }

        public void setBitmap(Bitmap bitmap) {
            mBitmap = bitmap;
        }

        public void recycle() {
            if (BitmapUtils.isValidate(mBitmap)) {
                TileCache.getInstance().put(mBitmap);
                mBitmap = null;
            }
            sTileDataPool.release(this);
        }
    }
}
