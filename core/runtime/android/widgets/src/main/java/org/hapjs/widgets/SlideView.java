/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.widgets;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;
import android.view.ViewGroup;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.hapjs.bridge.annotation.WidgetAnnotation;
import org.hapjs.common.json.JSONArray;
import org.hapjs.common.json.JSONObject;
import org.hapjs.common.utils.ColorUtil;
import org.hapjs.component.Component;
import org.hapjs.component.Container;
import org.hapjs.component.bridge.RenderEventCallback;
import org.hapjs.component.constants.Attributes;
import org.hapjs.runtime.HapEngine;
import org.hapjs.widgets.view.slideview.SlideViewLayout;
import org.json.JSONException;

@WidgetAnnotation(
        name = SlideView.WIDGET_NAME,
        methods = {
                Component.METHOD_ANIMATE,
                Component.METHOD_GET_BOUNDING_CLIENT_RECT,
                Component.METHOD_TO_TEMP_FILE_PATH,
                SlideView.METHOD_OPEN,
                SlideView.METHOD_CLOSE,
                SlideView.METHOD_HIDE_SECONDARY_CONFIRM
        })
public class SlideView extends Container<SlideViewLayout> {

    protected static final String WIDGET_NAME = "slide-view";
    // methods
    static final String METHOD_OPEN = "open";
    static final String METHOD_CLOSE = "close";
    static final String METHOD_HIDE_SECONDARY_CONFIRM = "hideSecondaryConfirm";
    private static final String TAG = WIDGET_NAME;
    // attr
    private static final String EDGE = "edge";
    private static final String ENABLE_SLIDE = "enableslide";
    private static final String IS_OPEN = "isopen";
    private static final String BUTTONS = "buttons";
    private static final String LAYER = "layer";
    private static final String BUTTONS_ID = "id";
    private static final String BUTTONS_BUTTON_WIDTH = "buttonWidth";
    private static final String BUTTONS_ICON = "icon";
    private static final String BUTTONS_ICON_WIDTH = "iconWidth";
    private static final String BUTTONS_ICON_HEIGHT = "iconHeight";
    private static final String BUTTONS_ICON_BACKGROUND_COLOR = "iconBackgroundColor";
    private static final String BUTTONS_TEXT = "text";
    private static final String BUTTONS_TEXT_SIZE = "textSize";
    private static final String BUTTONS_TEXT_COLOR = "textColor";
    private static final String BUTTONS_BACKGROUND_COLOR = "backgroundColor";
    private static final String BUTTONS_BACKGROUND_TYPE = "backgroundType";
    private static final String BUTTONS_SECONDARY_CONFIRM = "secondaryConfirm";
    private static final String BUTTONS_SECONDARY_CONFIRM_TEXT = "text";
    private static final String BUTTONS_SECONDARY_CONFIRM_TEXT_SIZE = "textSize";
    private static final String BUTTONS_SECONDARY_CONFIRM_TEXT_COLOR = "textColor";
    // event
    private static final String EVENT_OPEN = "open";
    private static final String EVENT_CLOSE = "close";
    private static final String EVENT_SLIDE = "slide";
    private static final String EVENT_BUTTON_CLICK = "buttonclick";
    private static final String KEY_OPEN_STATE = "open_state";

    private static final int UNDEFINE = SlideViewLayout.UNDEFINE;

    private boolean mIsSlideListenerRegistered = false;
    private boolean mIsButtonClickListenerRegistered = false;

    private Set<String> mButtonsIdSet;

    public SlideView(
            HapEngine hapEngine,
            Context context,
            Container parent,
            int ref,
            RenderEventCallback callback,
            Map<String, Object> savedState) {
        super(hapEngine, context, parent, ref, callback, savedState);
    }

    @Override
    protected SlideViewLayout createViewImpl() {
        SlideViewLayout slideViewLayout = new SlideViewLayout(mContext);
        slideViewLayout.setComponent(this);

        slideViewLayout.setSwipeListener(
                new SlideViewLayout.SlideListener() {
                    @Override
                    public void onOpened(SlideViewLayout view) {
                        // 无论是否注册open监听，都需要向前端返回isopen的属性值,用以保证属性值的同步
                        getAttrsDomData().put(IS_OPEN, true);
                        Map<String, Object> attrs = new HashMap<>();
                        attrs.put(IS_OPEN, true);
                        mCallback.onJsEventCallback(getPageId(), mRef, EVENT_OPEN, SlideView.this,
                                null, attrs);
                    }

                    @Override
                    public void onClosed(SlideViewLayout view) {
                        // 无论是否注册close监听，都需要向前端返回isopen的属性值,用以保证属性值的同步
                        getAttrsDomData().put(IS_OPEN, false);
                        Map<String, Object> attrs = new HashMap<>();
                        attrs.put(IS_OPEN, false);
                        mCallback.onJsEventCallback(
                                getPageId(), mRef, EVENT_CLOSE, SlideView.this, null, attrs);
                    }

                    @Override
                    public void onSlide(SlideViewLayout view, float slideOffset) {
                        if (mIsSlideListenerRegistered) {
                            Map<String, Object> params = new HashMap<>();
                            params.put("offset", slideOffset);
                            mCallback.onJsEventCallback(
                                    getPageId(), mRef, EVENT_SLIDE, SlideView.this, params, null);
                        }
                    }
                });

        slideViewLayout.setButtonsClickListener(
                (view, id, isSecondaryConfirm) -> {
                    if (mIsButtonClickListenerRegistered) {
                        Map<String, Object> params = new HashMap<>();
                        params.put("id", id);
                        params.put("isSecondaryConfirm", isSecondaryConfirm);
                        mCallback.onJsEventCallback(
                                getPageId(), mRef, EVENT_BUTTON_CLICK, SlideView.this, params,
                                null);
                    }
                });

        return slideViewLayout;
    }

    @Override
    protected boolean setAttribute(String key, Object attribute) {
        switch (key) {
            case EDGE:
                String edge = Attributes.getString(attribute, "right");
                setEdge(edge);
                return true;
            case ENABLE_SLIDE:
                boolean enableSlide = Attributes.getBoolean(attribute, true);
                setEnableSlide(enableSlide);
                return true;
            case IS_OPEN:
                boolean isOpen = Attributes.getBoolean(attribute, false);
                setIsOpen(isOpen);
                return true;
            case BUTTONS:
                String buttonsJSON = Attributes.getString(attribute);
                setButtons(buttonsJSON);
                return true;
            case LAYER:
                String layer = Attributes.getString(attribute, "above");
                setLayer(layer);
                return true;
            default:
                break;
        }
        return super.setAttribute(key, attribute);
    }

    @Override
    protected boolean addEvent(String event) {
        if (TextUtils.isEmpty(event) || mHost == null) {
            return true;
        }

        switch (event) {
            case EVENT_OPEN:
            case EVENT_CLOSE:
                return true;
            case EVENT_SLIDE:
                mIsSlideListenerRegistered = true;
                return true;
            case EVENT_BUTTON_CLICK:
                mIsButtonClickListenerRegistered = true;
                return true;
            default:
                break;
        }

        return super.addEvent(event);
    }

    @Override
    protected boolean removeEvent(String event) {
        if (TextUtils.isEmpty(event) || mHost == null) {
            return true;
        }

        switch (event) {
            case EVENT_OPEN:
            case EVENT_CLOSE:
                return true;
            case EVENT_SLIDE:
                mIsSlideListenerRegistered = false;
                return true;
            case EVENT_BUTTON_CLICK:
                mIsButtonClickListenerRegistered = false;
                return true;
            default:
                break;
        }

        return super.removeEvent(event);
    }

    @Override
    public void invokeMethod(String methodName, Map<String, Object> args) {
        switch (methodName) {
            case METHOD_OPEN:
                boolean openAnimation = true;
                Object openAnimationObj = args.get("animation");
                if (openAnimationObj != null) {
                    try {
                        openAnimation = (boolean) openAnimationObj;
                    } catch (Exception e) {
                        Log.e(TAG,
                                "invokeMethod: method open args animation could't cast to boolean.");
                    }
                }
                open(openAnimation);
                return;
            case METHOD_CLOSE:
                boolean closeAnimation = true;
                Object closeAnimationObj = args.get("animation");
                if (closeAnimationObj != null) {
                    try {
                        closeAnimation = (boolean) closeAnimationObj;
                    } catch (Exception e) {
                        Log.e(TAG,
                                "invokeMethod: method close args animation could't cast to boolean.");
                    }
                }
                close(closeAnimation);
                return;
            case METHOD_HIDE_SECONDARY_CONFIRM:
                hideSecondaryConfirm();
                return;
            default:
                super.invokeMethod(methodName, args);
        }
    }

    private void setEdge(String edge) {
        if (mHost == null) {
            return;
        }
        mHost.setEdge(edge);
    }

    private void setEnableSlide(boolean enableSlide) {
        if (mHost == null) {
            return;
        }
        mHost.setEnableSlide(enableSlide);
    }

    private void setIsOpen(boolean isOpen) {
        if (mHost == null) {
            return;
        }
        mHost.setIsOpen(isOpen);
    }

    private void setLayer(String layer) {
        if (mHost == null) {
            return;
        }
        mHost.setLayer(layer);
    }

    private void setButtons(String buttonsJSON) {
        if (mHost == null || TextUtils.isEmpty(buttonsJSON)) {
            return;
        }
        List<SlideViewLayout.SlideButtonInfo> buttonInfoList = parseButtonJson(buttonsJSON);
        mHost.setButtons(buttonInfoList);
    }

    private List<SlideViewLayout.SlideButtonInfo> parseButtonJson(String buttonsJson) {
        List<SlideViewLayout.SlideButtonInfo> buttonsDataList = new ArrayList<>();
        try {
            mButtonsIdSet = null;
            JSONArray jsonArray = new JSONArray(buttonsJson);
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject jsonObject = jsonArray.getJSONObject(i);
                SlideViewLayout.SlideButtonInfo holder = new SlideViewLayout.SlideButtonInfo();
                for (Iterator<String> keys = jsonObject.keys(); keys.hasNext(); ) {
                    String key = ((String) keys.next()).intern();
                    switch (key) {
                        case BUTTONS_ID:
                            String indexStr = jsonObject.optString(key);
                            holder.id = indexStr.trim();
                            break;
                        case BUTTONS_BUTTON_WIDTH:
                            String buttonWidthStr = jsonObject.optString(key);
                            holder.buttonWidth =
                                    Attributes.getInt(mHapEngine, buttonWidthStr, UNDEFINE);
                            break;
                        case BUTTONS_ICON:
                            String iconStr = jsonObject.optString(key);
                            holder.icon = tryParseUri(iconStr.trim());
                            break;
                        case BUTTONS_ICON_WIDTH:
                            String iconWidthStr = jsonObject.optString(key);
                            holder.iconWidth =
                                    Attributes.getInt(mHapEngine, iconWidthStr, UNDEFINE);
                            break;
                        case BUTTONS_ICON_HEIGHT:
                            String iconHeightStr = jsonObject.optString(key);
                            holder.iconHeight =
                                    Attributes.getInt(mHapEngine, iconHeightStr, UNDEFINE);
                            break;
                        case BUTTONS_ICON_BACKGROUND_COLOR:
                            String iconBackgroundColorStr = jsonObject.optString(key);
                            holder.iconBackgroundColor =
                                    ColorUtil.getColor(iconBackgroundColorStr, UNDEFINE);
                            break;
                        case BUTTONS_TEXT:
                            String textStr = jsonObject.optString(key);
                            holder.text = textStr;
                            break;
                        case BUTTONS_TEXT_SIZE:
                            String textSizeStr = jsonObject.optString(key);
                            holder.textSize =
                                    Attributes.getFontSize(mHapEngine, getPage(), textSizeStr,
                                            UNDEFINE);
                            break;
                        case BUTTONS_TEXT_COLOR:
                            String textColorStr = jsonObject.optString(key);
                            holder.textColor = ColorUtil.getColor(textColorStr, UNDEFINE);
                            break;
                        case BUTTONS_BACKGROUND_COLOR:
                            String backgroundColorStr = jsonObject.optString(key);
                            holder.backgroundColor =
                                    ColorUtil.getColor(backgroundColorStr, UNDEFINE);
                            break;
                        case BUTTONS_BACKGROUND_TYPE:
                            String backgroundTypeStr = jsonObject.optString(key);
                            holder.backgroundType = backgroundTypeStr.trim().toLowerCase();
                            break;
                        case BUTTONS_SECONDARY_CONFIRM:
                            String secondaryConfirmStr = jsonObject.optString(key);
                            holder.secondaryConfirmInfo =
                                    parseSecondaryConfirmJson(secondaryConfirmStr);
                            break;
                        default:
                            break;
                    }
                }
                if (!checkButtonsIdLegal(holder.id)) {
                    throw new IllegalArgumentException(
                            "button's id can not be null and should be unique. button:"
                                    + holder.toString());
                }
                buttonsDataList.add(holder);
            }
        } catch (JSONException e) {
            Log.e(TAG, "parseButtonJson: JSONException=" + e);
            e.printStackTrace();
        }
        return buttonsDataList;
    }

    private SlideViewLayout.SecondaryConfirmInfo parseSecondaryConfirmJson(
            String secondaryConfirmJson) {
        SlideViewLayout.SecondaryConfirmInfo secondaryConfirmInfo =
                new SlideViewLayout.SecondaryConfirmInfo();
        try {
            JSONObject jsonObject = new JSONObject(secondaryConfirmJson);
            for (Iterator<String> keys = jsonObject.keys(); keys.hasNext(); ) {
                String key = ((String) keys.next()).intern();
                switch (key) {
                    case BUTTONS_SECONDARY_CONFIRM_TEXT:
                        String textStr = jsonObject.optString(key);
                        secondaryConfirmInfo.text = textStr;
                        break;
                    case BUTTONS_SECONDARY_CONFIRM_TEXT_SIZE:
                        String textSizeStr = jsonObject.optString(key);
                        secondaryConfirmInfo.textSize =
                                Attributes
                                        .getFontSize(mHapEngine, getPage(), textSizeStr, UNDEFINE);
                        break;
                    case BUTTONS_SECONDARY_CONFIRM_TEXT_COLOR:
                        String textColorStr = jsonObject.optString(key);
                        secondaryConfirmInfo.textColor = ColorUtil.getColor(textColorStr, UNDEFINE);
                        break;
                    default:
                        break;
                }
            }

        } catch (JSONException e) {
            Log.e(TAG, "parseSecondaryConfirmJson: JSONException=" + e);
            e.printStackTrace();
        }
        return secondaryConfirmInfo;
    }

    private boolean checkButtonsIdLegal(String id) {
        if (id == null) {
            return false;
        }
        if (mButtonsIdSet == null) {
            mButtonsIdSet = new HashSet<>();
        } else {
            if (mButtonsIdSet.contains(id)) {
                return false;
            }
        }
        mButtonsIdSet.add(id);
        return true;
    }

    public void open(boolean animation) {
        if (mHost == null) {
            return;
        }
        mHost.open(animation);
    }

    public void close(boolean animation) {
        if (mHost == null) {
            return;
        }
        mHost.close(animation);
    }

    public void hideSecondaryConfirm() {
        if (mHost == null) {
            return;
        }
        mHost.hideSecondaryConfirm();
    }

    @Override
    protected void onSaveInstanceState(Map<String, Object> outState) {
        super.onSaveInstanceState(outState);
        if (mHost == null || outState == null) {
            return;
        }
        outState.put(KEY_OPEN_STATE, mHost.isOpened());
    }

    @Override
    protected void onRestoreInstanceState(Map<String, Object> savedState) {
        super.onRestoreInstanceState(savedState);
        if (mHost == null || savedState == null) {
            return;
        }
        if (savedState.get(KEY_OPEN_STATE) != null) {
            boolean openState = (boolean) savedState.get(KEY_OPEN_STATE);
            if (openState) {
                mHost.open(false);
            } else {
                mHost.close(false);
            }
        }
    }

    @Override
    public ViewGroup getInnerView() {
        return mHost.getMainLayout();
    }
}
