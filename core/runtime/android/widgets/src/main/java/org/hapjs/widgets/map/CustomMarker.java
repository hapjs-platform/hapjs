/*
 * Copyright (c) 2021-present, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.widgets.map;

import android.content.Context;
import android.text.TextUtils;
import java.util.Map;
import org.hapjs.bridge.annotation.WidgetAnnotation;
import org.hapjs.component.Component;
import org.hapjs.component.Container;
import org.hapjs.component.bridge.RenderEventCallback;
import org.hapjs.component.constants.Attributes;
import org.hapjs.runtime.HapEngine;
import org.hapjs.widgets.Div;
import org.hapjs.widgets.map.model.BaseMapMarker;
import org.json.JSONException;
import org.json.JSONObject;

@WidgetAnnotation(
        name = CustomMarker.WIDGET_NAME,
        methods = {
                Component.METHOD_GET_BOUNDING_CLIENT_RECT,
                Component.METHOD_FOCUS,
                Component.METHOD_TO_TEMP_FILE_PATH,
                Component.METHOD_TALKBACK_FOCUS,
                Component.METHOD_TALKBACK_ANNOUNCE
        })
public class CustomMarker extends Div {

    public static final String ANCHOR_X = "anchorX";
    public static final String ANCHOR_Y = "anchorY";
    protected static final String WIDGET_NAME = "custommarker";
    // attribute
    private static final String ID = "id";
    private static final String LONGITUDE = "longitude";
    private static final String LATITUDE = "latitude";
    private static final String COORDTYPE = "coordType";
    private static final String CUSTOM_MARKER_ATTRS = "custommarkerattr";
    private BaseMapMarker mBaseMapMarker;

    public CustomMarker(
            HapEngine hapEngine,
            Context context,
            Container parent,
            int ref,
            RenderEventCallback callback,
            Map<String, Object> savedState) {
        super(hapEngine, context, parent, ref, callback, savedState);
        if (!(mParent instanceof org.hapjs.widgets.map.Map)) {
            mCallback.onJsException(
                    new IllegalArgumentException("CustomMarker`s parent component must be map"));
        }
    }

    @Override
    protected boolean setAttribute(String key, Object attribute) {
        switch (key) {
            case CUSTOM_MARKER_ATTRS:
                setCustomMarkerAttr(Attributes.getString(attribute));
                return true;
            default:
                return super.setAttribute(key, attribute);
        }
    }

    public BaseMapMarker getCustomMarkerAttr() {
        return mBaseMapMarker;
    }

    private void setCustomMarkerAttr(String attrs) {
        if (TextUtils.isEmpty(attrs)) {
            return;
        }
        mBaseMapMarker = null;
        try {
            BaseMapMarker baseMapMarker = new BaseMapMarker();
            JSONObject object = new JSONObject(attrs);
            buildBaseMapMarker(object, baseMapMarker);
            mBaseMapMarker = baseMapMarker;
        } catch (JSONException e) {
            mCallback.onJsException(e);
        }
    }

    private void buildBaseMapMarker(JSONObject object, BaseMapMarker baseMapMarker) {
        try {
            baseMapMarker.id = object.optInt(ID, -1);
            if (object.has(LATITUDE)) {
                baseMapMarker.latitude = object.getDouble(LATITUDE);
            } else {
                mCallback.onJsException(
                        new IllegalArgumentException("custommarkerattr` latitude must be defined"));
                baseMapMarker = null;
                return;
            }
            if (object.has(LONGITUDE)) {
                baseMapMarker.longitude = object.getDouble(LONGITUDE);
            } else {
                mCallback.onJsException(
                        new IllegalArgumentException(
                                "custommarkerattr` longitude must be defined"));
                baseMapMarker = null;
                return;
            }
            if (object.has(COORDTYPE)) {
                baseMapMarker.coordType = object.optString(COORDTYPE);
            }
            baseMapMarker.offsetX =
                    Attributes.getInt(mHapEngine,
                            object.optString(ANCHOR_X, BaseMapMarker.DEFAULT_ANCHOR));
            baseMapMarker.offsetY =
                    Attributes.getInt(mHapEngine,
                            object.optString(ANCHOR_Y, BaseMapMarker.DEFAULT_ANCHOR));
        } catch (JSONException e) {
            mCallback.onJsException(e);
        }
    }
}
