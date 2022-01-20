/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.widgets.view.swiper;

import org.hapjs.common.utils.IntegerUtil;

public class SwiperAnimation {

    private float mRotateStart = 0.0f;
    private float mRotateEnd = 0.0f;
    private float mRotateXStart = 0.0f;
    private float mRotateXEnd = 0.0f;
    private float mRotateYStart = 0.0f;
    private float mRotateYEnd = 0.0f;

    private float mScaleXStart = 1.0f;
    private float mScaleXEnd = 1.0f;
    private float mScaleYStart = 1.0f;
    private float mScaleYEnd = 1.0f;

    private float mTranslationXStart = 0.0f;
    private float mTranslationXEnd = 0.0f;
    private float mTranslationYStart = 0.0f;
    private float mTranslationYEnd = 0.0f;

    private float mAlphaStart = 1.0f;
    private float mAlphaEnd = 1.0f;

    private float mPivotX = 0.0f;
    private float mPivotY = 0.0f;

    private int mBackgroundColorStart = IntegerUtil.UNDEFINED;
    private int mBackgroundColorEnd = IntegerUtil.UNDEFINED;

    public int getBackgroundColorStart() {
        return mBackgroundColorStart;
    }

    public void setBackgroundColorStart(int backgroundColorStart) {
        mBackgroundColorStart = backgroundColorStart;
    }

    public int getBackgroundColorEnd() {
        return mBackgroundColorEnd;
    }

    public void setBackgroundColorEnd(int backgroundColorEnd) {
        mBackgroundColorEnd = backgroundColorEnd;
    }

    public float getPivotX() {
        return mPivotX;
    }

    public void setPivotX(float pivotX) {
        mPivotX = pivotX;
    }

    public float getPivotY() {
        return mPivotY;
    }

    public void setPivotY(float pivotY) {
        mPivotY = pivotY;
    }

    public float getAlphaStart() {
        return mAlphaStart;
    }

    public void setAlphaStart(float alphaStart) {
        mAlphaStart = alphaStart;
    }

    public float getAlphaEnd() {
        return mAlphaEnd;
    }

    public void setAlphaEnd(float alphaEnd) {
        mAlphaEnd = alphaEnd;
    }

    public float getRotateStart() {
        return mRotateStart;
    }

    public void setRotateStart(float rotateStart) {
        mRotateStart = rotateStart;
    }

    public float getRotateEnd() {
        return mRotateEnd;
    }

    public void setRotateEnd(float rotateEnd) {
        mRotateEnd = rotateEnd;
    }

    public float getRotateXStart() {
        return mRotateXStart;
    }

    public void setRotateXStart(float rotateXStart) {
        mRotateXStart = rotateXStart;
    }

    public float getRotateXEnd() {
        return mRotateXEnd;
    }

    public void setRotateXEnd(float rotateXEnd) {
        mRotateXEnd = rotateXEnd;
    }

    public float getRotateYStart() {
        return mRotateYStart;
    }

    public void setRotateYStart(float rotateYStart) {
        mRotateYStart = rotateYStart;
    }

    public float getRotateYEnd() {
        return mRotateYEnd;
    }

    public void setRotateYEnd(float rotateYEnd) {
        mRotateYEnd = rotateYEnd;
    }

    public float getScaleXStart() {
        return mScaleXStart;
    }

    public void setScaleXStart(float scaleXStart) {
        mScaleXStart = scaleXStart;
    }

    public float getScaleXEnd() {
        return mScaleXEnd;
    }

    public void setScaleXEnd(float scaleXEnd) {
        mScaleXEnd = scaleXEnd;
    }

    public float getScaleYStart() {
        return mScaleYStart;
    }

    public void setScaleYStart(float scaleYStart) {
        mScaleYStart = scaleYStart;
    }

    public float getScaleYEnd() {
        return mScaleYEnd;
    }

    public void setScaleYEnd(float scaleYEnd) {
        mScaleYEnd = scaleYEnd;
    }

    public float getTranslationXStart() {
        return mTranslationXStart;
    }

    public void setTranslationXStart(float translationXStart) {
        mTranslationXStart = translationXStart;
    }

    public float getTranslationXEnd() {
        return mTranslationXEnd;
    }

    public void setTranslationXEnd(float translationXEnd) {
        mTranslationXEnd = translationXEnd;
    }

    public float getTranslationYStart() {
        return mTranslationYStart;
    }

    public void setTranslationYStart(float translationYStart) {
        mTranslationYStart = translationYStart;
    }

    public float getTranslationYEnd() {
        return mTranslationYEnd;
    }

    public void setTranslationYEnd(float translationYEnd) {
        mTranslationYEnd = translationYEnd;
    }
}
