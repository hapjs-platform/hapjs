/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.animation;

import android.content.Context;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.util.AttributeSet;
import androidx.annotation.Nullable;

public class AnimationImageView extends androidx.appcompat.widget.AppCompatImageView {

    public AnimationImageView(Context context) {
        super(context);
    }

    public AnimationImageView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public AnimationImageView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        startAnimation();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        stopAnimation();
    }

    private void startAnimation() {
        enableAnimation(true);
    }

    private void stopAnimation() {
        enableAnimation(false);
    }

    private void enableAnimation(boolean enable) {
        Drawable background = getBackground();
        if (background instanceof LayerDrawable) {
            for (int i = 0; i < ((LayerDrawable) background).getNumberOfLayers(); i++) {
                Drawable drawable = ((LayerDrawable) background).getDrawable(i);
                if (drawable instanceof AnimationDrawable) {
                    AnimationDrawable animationDrawable = ((AnimationDrawable) drawable);
                    if (enable) {
                        animationDrawable.start();
                    } else {
                        animationDrawable.stop();
                    }
                }
            }
        }
    }
}
