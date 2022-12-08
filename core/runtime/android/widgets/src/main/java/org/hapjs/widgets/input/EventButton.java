/*
 * Copyright (c) 2022-present, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.hapjs.widgets.input;

import android.app.Activity;
import android.content.Context;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.widget.TextView;
import android.widget.Toast;

import org.hapjs.bridge.ApplicationContext;
import org.hapjs.bridge.annotation.TypeAnnotation;
import org.hapjs.bridge.annotation.WidgetAnnotation;
import org.hapjs.common.executors.Executors;
import org.hapjs.common.utils.ColorUtil;
import org.hapjs.common.utils.ShortcutManager;
import org.hapjs.common.utils.ThreadUtils;
import org.hapjs.component.Component;
import org.hapjs.component.Container;
import org.hapjs.component.bridge.RenderEventCallback;
import org.hapjs.component.constants.Attributes;
import org.hapjs.component.view.gesture.GestureDelegate;
import org.hapjs.component.view.gesture.GestureHost;
import org.hapjs.component.view.gesture.IGesture;
import org.hapjs.logging.Source;
import org.hapjs.model.AppInfo;
import org.hapjs.render.Page;
import org.hapjs.runtime.HapEngine;
import org.hapjs.widgets.R;
import org.hapjs.widgets.view.text.FlexButton;

import java.util.Map;

@WidgetAnnotation(
        name = Edit.WIDGET_NAME,
        types = {
                @TypeAnnotation(name = EventButton.TYPE_EVENT_BUTTON)
        },
        methods = {
                Component.METHOD_FOCUS,
                Component.METHOD_ANIMATE,
                Component.METHOD_GET_BOUNDING_CLIENT_RECT,
                Component.METHOD_TO_TEMP_FILE_PATH,
                Component.METHOD_TALKBACK_FOCUS,
                Component.METHOD_TALKBACK_ANNOUNCE
        }
)
public class EventButton extends Button {

    private final String TAG = "EventButton";
    protected static final String TYPE_EVENT_BUTTON = "eventbutton";
    protected static final String DEFAULT_WIDTH = "128px";
    protected static final String DEFAULT_HEIGHT = "70px";
    private static final int CREATE_SHORTCUT_SUCCESS = 0;
    private static final int CREATE_SHORTCUT_ALREADY = 1;
    private static final int CREATE_SHORTCUT_FAIL = 2;
    private static final String ATTR_EVENT_KEY_TYPE = "eventtype";
    private static final String EVENT_STATUS = "event_status_code";
    private static final String EVENT_MESSAGE = "event_message";
    private static final int SUCCESS = 0;
    private static final int ERROR = 200;
    private String mEventType = "";


    public EventButton(HapEngine hapEngine, Context context, Container parent, int ref, RenderEventCallback callback,
                       Map<String, Object> savedState) {
        super(hapEngine, context, parent, ref, callback, savedState);
    }

    @Override
    protected TextView createViewImpl() {
        final FlexButton button = new FlexButton(mContext);
        button.setComponent(this);
        initDefaultView(button);
        button.setAllCaps(false);
        button.setBackgroundResource(R.drawable.btn_default_bg_selector);
        return button;
    }

    @Override
    protected void initDefaultView(TextView view) {
        Page page = initFontLevel();
        view.setTextSize(TypedValue.COMPLEX_UNIT_PX, Attributes.getFontSize(mHapEngine, page, DEFAULT_FONT_SIZE,this));
        view.setTextColor(ColorUtil.getColor(DEFAULT_COLOR));

        int minWidth = Attributes.getInt(mHapEngine, DEFAULT_WIDTH, this);
        view.setMinWidth(minWidth);
        view.setMinimumWidth(minWidth);

        int minHeight = Attributes.getInt(mHapEngine, DEFAULT_HEIGHT, this);
        view.setMinHeight(minHeight);
        view.setMinimumHeight(minHeight);
    }

    @Override
    public void setBackgroundColor(String colorStr) {
        super.setBackgroundColor(colorStr);

        if (mHost == null) {
            return;
        }

        mHost.setBackground(getOrCreateCSSBackground());
    }

    @Override
    protected int getDefaultVerticalGravity() {
        return Gravity.CENTER_VERTICAL;
    }

    public boolean preConsumeEvent(String eventName, Map<String, Object> data, boolean immediately) {
        boolean isConsume = false;
        if (Attributes.Event.TOUCH_CLICK.equals(eventName) && Attributes.EventType.SHORTCUT.equals(mEventType)) {
            isConsume = true;
            createShortCut(eventName, data, immediately);
        }
        return isConsume;
    }

    public void createShortCut(String eventName, Map<String, Object> data, boolean immediately) {
        Executors.io().execute(new Runnable() {
            @Override
            public void run() {
                String rpkPackage = "";
                String rpkName = "";
                Uri rpkIcon = null;
                ApplicationContext applicationContext = null;
                if (mHapEngine != null) {
                    applicationContext = mHapEngine.getApplicationContext();
                    rpkIcon = applicationContext.getIcon();
                    AppInfo appInfo = applicationContext.getAppInfo();
                    if (appInfo != null) {
                        rpkPackage = appInfo.getPackage();
                        rpkName = appInfo.getName();
                    }
                    if (null != mContext && !TextUtils.isEmpty(rpkName) && !TextUtils.isEmpty(rpkPackage) && null != rpkIcon) {
                        if (ShortcutManager.hasShortcutInstalled(mContext, rpkPackage)) {
                            ShortcutManager.update(mContext, rpkPackage, rpkName, rpkIcon);
                            if (null != data) {
                                data.put(EVENT_STATUS, ERROR);
                                data.put(EVENT_MESSAGE, "already createShortCut.");
                            } else {
                                Log.e(TAG, "createShortCut hasShortcutInstalled  data is null.");
                            }
                            fireGestureEvent(CREATE_SHORTCUT_ALREADY, eventName, data, immediately, rpkName);
                        } else {
                            Source source = new Source();
                            source.putExtra(Source.EXTRA_SCENE, Source.SHORTCUT_SCENE_API);
                            boolean isSuccess = ShortcutManager.install(mContext, rpkPackage, rpkName, rpkIcon, source);
                            if (isSuccess) {
                                if (null != data) {
                                    data.put(EVENT_STATUS, SUCCESS);
                                    data.put(EVENT_MESSAGE, "createShortCut success.");
                                } else {
                                    Log.e(TAG, "createShortCut  install success  data is null.");
                                }
                                fireGestureEvent(CREATE_SHORTCUT_SUCCESS, eventName, data, immediately, rpkName);
                            } else {
                                if (null != data) {
                                    data.put(EVENT_STATUS, ERROR);
                                    data.put(EVENT_MESSAGE, "createShortCut fail.");
                                } else {
                                    Log.e(TAG, "createShortCut  install fail  data is null.");
                                }
                                Log.e(TAG, "createShortCut  install fail.");
                                fireGestureEvent(CREATE_SHORTCUT_FAIL, eventName, data, immediately, rpkName);
                            }
                        }
                    } else {
                        Log.e(TAG, "createShortCut : " + (null == mContext ? (" mContext is null.  ") : "")
                                + (TextUtils.isEmpty(rpkName) ? " rpkName is empty. " : "")
                                + (TextUtils.isEmpty(rpkPackage) ? " rpkPackage is empty. " : "")
                                + (null == rpkIcon ? (" rpkIcon is null.  ") : "")
                        );
                    }
                }
            }
        });

    }

    public void fireGestureEvent(int createCode, String eventName, Map<String, Object> data, boolean immediately, String rpkName) {
        if (mContext instanceof Activity && ((Activity) mContext).isFinishing()) {
            Log.e(TAG, "fireGestureEvent Activity is finishing.");
            return;
        }
        ThreadUtils.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                switch (createCode) {
                    case CREATE_SHORTCUT_ALREADY:
                        if (null != mContext) {
                            String tmpShortCutStr = String.format(mContext.getResources().getString(org.hapjs.runtime.R.string.menubar_added_shortcut_tips), rpkName);
                            Toast.makeText(mContext, tmpShortCutStr, Toast.LENGTH_SHORT).show();
                        }
                        break;
                    case CREATE_SHORTCUT_SUCCESS:
                        break;
                    case CREATE_SHORTCUT_FAIL:
                        break;
                    default:
                        break;
                }
                if (mHost instanceof GestureHost) {
                    GestureHost gestureHost = (GestureHost) mHost;
                    IGesture iGesture = null;
                    if (null != gestureHost) {
                        iGesture = gestureHost.getGesture();
                    }
                    if (iGesture instanceof GestureDelegate) {
                        ((GestureDelegate) iGesture).fireEvent(eventName, data, true);
                    }
                }
            }
        });
    }

    @Override
    protected boolean setAttribute(String key, Object attribute) {
        switch (key) {
            case ATTR_EVENT_KEY_TYPE:
                String enterKeyType = Attributes.getString(attribute);
                setEventKeyType(enterKeyType);
                return true;
        }
        return super.setAttribute(key, attribute);
    }

    private void setEventKeyType(String type) {
        if (mHost == null) {
            return;
        }
        mEventType = type;
    }
}
