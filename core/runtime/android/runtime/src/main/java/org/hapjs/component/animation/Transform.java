/*
 * Copyright (c) 2021-present, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.component.animation;

import static org.hapjs.component.animation.AnimationParser.TAG_ROTATE;
import static org.hapjs.component.animation.AnimationParser.TAG_ROTATE_X;
import static org.hapjs.component.animation.AnimationParser.TAG_ROTATE_Y;
import static org.hapjs.component.animation.AnimationParser.TAG_SCALE_X;
import static org.hapjs.component.animation.AnimationParser.TAG_SCALE_Y;
import static org.hapjs.component.animation.AnimationParser.TAG_TRANSLATE_X;
import static org.hapjs.component.animation.AnimationParser.TAG_TRANSLATE_Y;
import static org.hapjs.component.animation.AnimationParser.TAG_TRANSLATE_Z;
import android.text.TextUtils;
import android.view.View;
import org.hapjs.component.constants.Attributes;
import org.hapjs.runtime.HapEngine;
import org.json.JSONException;
import org.json.JSONObject;

public class Transform {

    private float mRotate = 0.0f;
    private float mRotateX = 0.0f;
    private float mRotateY = 0.0f;

    private float mScaleX = 1.0f;
    private float mScaleY = 1.0f;

    private float mTranslationX = 0.0f;
    private float mTranslationY = 0.0f;
    private float mTranslationZ = 0.0f;

    private float mTranslationXPercent = Float.NaN;
    private float mTranslationYPercent = Float.NaN;
    private float mTranslationZPercent = Float.NaN;

    public Transform() {
    }

    public static Transform parse(HapEngine engine, Object attrObj) {
        JSONObject jsonObj = toJsonObject(attrObj);
        if (jsonObj == null) {
            return null;
        }
        Transform transform = new Transform();
        double rotate = AnimationParser.getRotate(jsonObj.optString(TAG_ROTATE));
        if (!Double.isNaN(rotate)) {
            transform.setRotate((float) rotate);
        }

        double rotateX = AnimationParser.getRotate(jsonObj.optString(TAG_ROTATE_X));
        if (!Double.isNaN(rotateX)) {
            transform.setRotateX((float) rotateX);
        }

        double rotateY = AnimationParser.getRotate(jsonObj.optString(TAG_ROTATE_Y));
        if (!Double.isNaN(rotateY)) {
            transform.setRotateY((float) rotateY);
        }

        float scaleX = Attributes.getFloat(engine, jsonObj.optString(TAG_SCALE_X), Float.NaN);
        if (!Float.isNaN(scaleX)) {
            transform.setScaleX(scaleX);
        }

        float scaleY = Attributes.getFloat(engine, jsonObj.optString(TAG_SCALE_Y), Float.NaN);
        if (!Float.isNaN(scaleY)) {
            transform.setScaleY(scaleY);
        }

        String translationXStr = jsonObj.optString(TAG_TRANSLATE_X);
        if (!TextUtils.isEmpty(translationXStr)) {
            if (translationXStr.endsWith(Attributes.Unit.PERCENT)) {
                transform.setTranslationXPercent(Attributes.getPercent(translationXStr, Float.NaN));
            } else {
                float translationX = Attributes.getFloat(engine, translationXStr, Float.NaN);
                if (!Float.isNaN(translationX)) {
                    transform.setTranslationX(translationX);
                }
            }
        }

        String translationYStr = jsonObj.optString(TAG_TRANSLATE_Y);
        if (!TextUtils.isEmpty(translationYStr)) {
            if (translationYStr.endsWith(Attributes.Unit.PERCENT)) {
                transform.setTranslationYPercent(Attributes.getPercent(translationYStr, Float.NaN));
            } else {
                float translationY = Attributes.getFloat(engine, translationYStr, Float.NaN);
                if (!Float.isNaN(translationY)) {
                    transform.setTranslationY(translationY);
                }
            }
        }

        String translationZStr = jsonObj.optString(TAG_TRANSLATE_Z);
        if (!TextUtils.isEmpty(translationZStr)) {
            if (translationZStr.endsWith(Attributes.Unit.PERCENT)) {
                transform.setTranslationZPercent(Attributes.getPercent(translationZStr, Float.NaN));
            } else {
                float translationZ = Attributes.getFloat(engine, translationZStr, Float.NaN);
                if (!Float.isNaN(translationZ)) {
                    transform.setTranslationZ(translationZ);
                }
            }
        }
        return transform;
    }

    public static void applyTransform(Transform transform, View view) {
        if (isParamSafe(transform, view)) {
            applyRotate(transform, view);
            applyRotateX(transform, view);
            applyRotateY(transform, view);
            applyScaleX(transform, view);
            applyScaleY(transform, view);
            applyTranslationX(transform, view);
            applyTranslationY(transform, view);
            applyTranslationZ(transform, view);
        }
    }

    public static void applyRotate(Transform transform, View view) {
        if (isParamSafe(transform, view)) {
            view.setRotation(transform.getRotate());
        }
    }

    public static void applyRotateX(Transform transform, View view) {
        if (isParamSafe(transform, view)) {
            view.setRotationX(transform.getRotateX());
        }
    }

    public static void applyRotateY(Transform transform, View view) {
        if (isParamSafe(transform, view)) {
            view.setRotationY(transform.getRotateY());
        }
    }

    public static void applyScaleX(Transform transform, View view) {
        if (isParamSafe(transform, view)) {
            view.setScaleX(transform.getScaleX());
        }
    }

    public static void applyScaleY(Transform transform, View view) {
        if (isParamSafe(transform, view)) {
            view.setScaleY(transform.getScaleY());
        }
    }

    public static void applyTranslationX(Transform transform, View view) {
        if (isParamSafe(transform, view)) {
            view.setTranslationX(transform.getTranslationX());
        }
    }

    public static void applyTranslationY(Transform transform, View view) {
        if (isParamSafe(transform, view)) {
            view.setTranslationY(transform.getTranslationY());
        }
    }

    public static void applyTranslationZ(Transform transform, View view) {
        if (isParamSafe(transform, view)) {
            view.setTranslationZ(transform.getTranslationZ());
        }
    }

    private static boolean isParamSafe(Transform transform, View view) {
        return transform != null && view != null;
    }

    public static JSONObject toJsonObject(Object attrObj) {
        if (attrObj == null || "".equals(attrObj)) {
            return null;
        }
        if (String.valueOf(attrObj).contains("(")) {
            String transform = String.valueOf(attrObj);
            transform = transform.trim();
            int index = 0;
            int length = transform.length();
            JSONObject object = new JSONObject();
            while (index < length) {
                int i = transform.indexOf(")", index);
                String s;
                s = transform.substring(index == 0 ? 0 : index + 1, i + 1);
                int functionIndex = s.indexOf("(");
                int argsIndex = s.indexOf(")");
                try {
                    String key = s.substring(0, functionIndex);
                    String value = s.substring(functionIndex + 1, argsIndex);
                    object.put(key, value);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                index = i + 1;
            }
            return object;
        }
        try {
            return new JSONObject((String) attrObj);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }

    public float getRotate() {
        return mRotate;
    }

    public void setRotate(float rotate) {
        mRotate = rotate;
    }

    public float getRotateX() {
        return mRotateX;
    }

    public void setRotateX(float rotateX) {
        mRotateX = rotateX;
    }

    public float getRotateY() {
        return mRotateY;
    }

    public void setRotateY(float rotateY) {
        mRotateY = rotateY;
    }

    public float getScaleX() {
        return mScaleX;
    }

    public void setScaleX(float scaleX) {
        mScaleX = scaleX;
    }

    public float getScaleY() {
        return mScaleY;
    }

    public void setScaleY(float scaleY) {
        mScaleY = scaleY;
    }

    public float getTranslationX() {
        return mTranslationX;
    }

    public void setTranslationX(float translationX) {
        mTranslationX = translationX;
    }

    public float getTranslationY() {
        return mTranslationY;
    }

    public void setTranslationY(float translationY) {
        mTranslationY = translationY;
    }

    public float getTranslationZ() {
        return mTranslationZ;
    }

    public void setTranslationZ(float translationZ) {
        mTranslationZ = translationZ;
    }

    public float getTranslationXPercent() {
        return mTranslationXPercent;
    }

    public void setTranslationXPercent(float translationXPercent) {
        mTranslationXPercent = translationXPercent;
    }

    public float getTranslationYPercent() {
        return mTranslationYPercent;
    }

    public void setTranslationYPercent(float translationYPercent) {
        mTranslationYPercent = translationYPercent;
    }

    public float getTranslationZPercent() {
        return mTranslationZPercent;
    }

    public void setTranslationZPercent(float translationZPercent) {
        mTranslationZPercent = translationZPercent;
    }
}
