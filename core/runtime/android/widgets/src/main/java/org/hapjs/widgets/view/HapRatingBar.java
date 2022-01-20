/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.widgets.view;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ClipDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.ViewGroup;
import androidx.appcompat.widget.AppCompatRatingBar;
import com.facebook.yoga.YogaNode;
import org.hapjs.common.utils.DisplayUtil;
import org.hapjs.component.Component;
import org.hapjs.component.utils.YogaUtil;
import org.hapjs.component.view.ComponentHost;
import org.hapjs.component.view.gesture.GestureHost;
import org.hapjs.component.view.gesture.IGesture;
import org.hapjs.component.view.helper.StateHelper;
import org.hapjs.component.view.keyevent.KeyEventDelegate;
import org.hapjs.widgets.R;

public class HapRatingBar extends AppCompatRatingBar implements ComponentHost, GestureHost {
    private Component mComponent;
    private KeyEventDelegate mKeyEventDelegate;

    private LayerDrawable mProgressDrawable;
    private Drawable mBackgroundDrawable;
    private Drawable mForegroundDrawable;
    private Drawable mSecondaryDrawable;

    private Bitmap mSampleTile;
    private IGesture mGesture;
    private int mLastWidth = -1;
    private int mLastHeight = -1;

    public HapRatingBar(Context context) {
        super(context);

        Drawable original = getProgressDrawable();
        if (original instanceof LayerDrawable) {
            mProgressDrawable = (LayerDrawable) original;
        }
    }

    @Override
    protected void drawableStateChanged() {
        super.drawableStateChanged();
        StateHelper.onStateChanged(this, mComponent);
    }

    @Override
    public Component getComponent() {
        return mComponent;
    }

    @Override
    public void setComponent(Component component) {
        mComponent = component;
    }

    @Override
    protected synchronized void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        boolean widthDefined = mComponent.isWidthDefined();
        boolean heightDefined = mComponent.isHeightDefined();

        int widthSize;
        int heightSize;
        boolean starDefined = mSampleTile != null;

        if (mSampleTile == null) {
            Resources resources = getResources();
            mBackgroundDrawable = resources.getDrawable(R.drawable.ic_rating_background);
            mForegroundDrawable = resources.getDrawable(R.drawable.ic_rating_foreground);
            mSecondaryDrawable = resources.getDrawable(R.drawable.ic_rating_background);

            mSampleTile = ((BitmapDrawable) mBackgroundDrawable).getBitmap();
        }

        float sampleTileRatio =
                (float) mSampleTile.getWidth() * getNumStars() / mSampleTile.getHeight();
        YogaNode node = YogaUtil.getYogaNode(this);
        if (widthDefined && heightDefined) {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
            return;
        } else if (widthDefined) {
            if (node == null) {
                ViewGroup.LayoutParams params = getLayoutParams();
                widthSize = params.width;
            } else {
                widthSize = Math.round(node.getWidth().value);
            }

            heightSize = Math.round(widthSize / sampleTileRatio);
        } else if (heightDefined) {
            if (node == null) {
                ViewGroup.LayoutParams params = getLayoutParams();
                heightSize = params.height;
            } else {
                heightSize = Math.round(node.getHeight().value);
            }

            widthSize = Math.round(heightSize * sampleTileRatio);
        } else {
            if (starDefined) {
                widthSize = mSampleTile.getWidth() * getNumStars();
                heightSize = mSampleTile.getHeight();
            } else {
                widthSize = mBackgroundDrawable.getIntrinsicWidth() * getNumStars();
                heightSize = mBackgroundDrawable.getIntrinsicHeight();
            }
        }

        widthSize = Math.min(widthSize, DisplayUtil.getScreenWidth(getContext()));
        heightSize = Math.min(heightSize, DisplayUtil.getScreenHeight(getContext()));

        setMeasuredDimension(widthSize, heightSize);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        mLastWidth = w;
        mLastHeight = h;
        refreshRatingDrawable(w, h);
    }

    private void refreshRatingDrawable(int w, int h) {
        if (mSampleTile != null && mProgressDrawable != null && w > 0 && h > 0) {
            float sx = (float) w / getNumStars() / mSampleTile.getWidth();
            float sy = (float) h / mSampleTile.getHeight();

            mProgressDrawable.setDrawableByLayerId(
                    android.R.id.background, tile(mBackgroundDrawable, false, sx, sy));
            mProgressDrawable.setDrawableByLayerId(
                    android.R.id.progress, tile(mForegroundDrawable, true, sx, sy));
            mProgressDrawable.setDrawableByLayerId(
                    android.R.id.secondaryProgress, tile(mSecondaryDrawable, true, sx, sy));

            mProgressDrawable.setBounds(0, 0, w, h);
            // reset rating when progress drawable changed
            float rating = getRating();
            setRating(0);
            setRating(rating);
            postInvalidate();
        }
    }

    public void setStarBackground(Drawable background) {
        mBackgroundDrawable = background;

        if (background instanceof BitmapDrawable) {
            mSampleTile = ((BitmapDrawable) background).getBitmap();
        }
        refreshRatingDrawable(mLastWidth, mLastHeight);
    }

    public void setStarForeground(Drawable foreground) {
        mForegroundDrawable = foreground;

        if (foreground instanceof BitmapDrawable) {
            mSampleTile = ((BitmapDrawable) foreground).getBitmap();
        }
        refreshRatingDrawable(mLastWidth, mLastHeight);
    }

    public void setStarSecondary(Drawable secondary) {
        mSecondaryDrawable = secondary;

        if (secondary instanceof BitmapDrawable) {
            mSampleTile = ((BitmapDrawable) secondary).getBitmap();
        }
        refreshRatingDrawable(mLastWidth, mLastHeight);
    }

    private Drawable tile(Drawable src, boolean clip, float sx, float sy) {
        if (src == null) {
            return new ColorDrawable(Color.TRANSPARENT);
        }

        if (src instanceof BitmapDrawable) {
            BitmapDrawable drawable = (BitmapDrawable) src;
            Bitmap bitmap = drawable.getBitmap();

            Matrix matrix = new Matrix();
            matrix.postScale(sx, sy);

            RectF dstR = new RectF(0, 0, bitmap.getWidth(), bitmap.getHeight());
            RectF deviceR = new RectF();

            matrix.mapRect(deviceR, dstR);

            int neww = Math.round(deviceR.width());
            int newh = Math.round(deviceR.height());

            if (neww <= 0 && getWidth() > 0) {
                sx = Math.max(sx, 1.f / getWidth());
            }
            if (newh <= 0 && getHeight() > 0) {
                sy = Math.max(sy, 1.f / getHeight());
            }
            matrix.setScale(sx, sy);

            Bitmap newBitmap =
                    Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix,
                            true);
            drawable = new BitmapDrawable(getResources(), newBitmap);

            drawable.setTileModeXY(Shader.TileMode.REPEAT, Shader.TileMode.CLAMP);
            if (clip) {
                return new ClipDrawable(drawable, Gravity.START, ClipDrawable.HORIZONTAL);
            } else {
                return drawable;
            }
        }

        return src;
    }

    @Override
    public void setNumStars(int numStars) {
        super.setNumStars(numStars);
        refreshRatingDrawable(mLastWidth, mLastHeight);
    }

    @Override
    public IGesture getGesture() {
        return mGesture;
    }

    @Override
    public void setGesture(IGesture gestureDelegate) {
        mGesture = gestureDelegate;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        boolean result = super.onTouchEvent(event);
        if (mGesture != null) {
            result |= mGesture.onTouch(event);
        }
        return result;
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        boolean result = super.onKeyDown(keyCode, event);
        return onKey(KeyEvent.ACTION_DOWN, keyCode, event, result);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        boolean result = super.onKeyUp(keyCode, event);
        return onKey(KeyEvent.ACTION_UP, keyCode, event, result);
    }

    private boolean onKey(int keyAction, int keyCode, KeyEvent event, boolean result) {
        if (mKeyEventDelegate == null) {
            mKeyEventDelegate = new KeyEventDelegate(mComponent);
        }
        result |= mKeyEventDelegate.onKey(keyAction, keyCode, event);
        return result;
    }
}
