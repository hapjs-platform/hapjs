/*
 * Copyright (c) 2022, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.widgets;

import android.app.Activity;
import android.content.Context;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import org.hapjs.bridge.ApplicationContext;
import org.hapjs.bridge.annotation.WidgetAnnotation;
import org.hapjs.common.executors.Executors;
import org.hapjs.common.utils.ShortcutManager;
import org.hapjs.common.utils.ThreadUtils;
import org.hapjs.component.Component;
import org.hapjs.component.Container;
import org.hapjs.component.bridge.RenderEventCallback;
import org.hapjs.component.constants.Attributes;
import org.hapjs.component.view.gesture.GestureDelegate;
import org.hapjs.component.view.gesture.GestureHost;
import org.hapjs.component.view.gesture.IGesture;
import org.hapjs.logging.LogProvider;
import org.hapjs.logging.RuntimeLogManager;
import org.hapjs.logging.Source;
import org.hapjs.model.AppInfo;
import org.hapjs.runtime.HapEngine;
import org.hapjs.runtime.ProviderManager;
import org.hapjs.widgets.input.Button;

import java.util.HashMap;
import java.util.Map;

import static org.hapjs.logging.LogProvider.KEY_RPK_PACKAGE;
import static org.hapjs.logging.LogProvider.KEY_RPK_VERSION;

@WidgetAnnotation(
        name = ShortcutButton.WIDGET_NAME,
        methods = {
                Component.METHOD_FOCUS,
                Component.METHOD_ANIMATE,
                Component.METHOD_GET_BOUNDING_CLIENT_RECT,
                Component.METHOD_TO_TEMP_FILE_PATH
        }
)

public class ShortcutButton extends Button {

    protected static final String WIDGET_NAME = "shortcut-button";

    private static final String TAG = "ShortcutButton";

    private static final int CREATE_SHORTCUT_SUCCESS = 0;
    private static final int CREATE_SHORTCUT_ALREADY = 1;
    private static final int CREATE_SHORTCUT_FAIL = 2;

    private static final String EVENT_STATUS = "eventStatusCode";
    private static final String EVENT_MESSAGE = "eventMessage";

    private static final int SUCCESS = 0;
    private static final int ERROR = 200;

    public ShortcutButton(HapEngine hapEngine, Context context, Container parent, int ref, RenderEventCallback callback, Map<String, Object> savedState) {
        super(hapEngine, context, parent, ref, callback, savedState);
    }

    @Override
    protected TextView createViewImpl() {
        TextView view = super.createViewImpl();
        reportTraceEvent(RuntimeLogManager.KEY_APP_EVENT_BUTTON_SHOW);
        return view;
    }

    public boolean preConsumeEvent(String eventName, Map<String, Object> data, boolean immediately) {
        boolean isConsume = false;
        if (Attributes.Event.TOUCH_CLICK.equals(eventName)) {
            isConsume = true;
            createShortCut(eventName, data);
        }
        return isConsume;
    }

    private void reportTraceEvent(String type) {
        if (null != mContext) {
            LogProvider logProvider = (LogProvider) ProviderManager.getDefault()
                    .getProvider(LogProvider.NAME);
            if (null != logProvider) {
                AppInfo appInfo = null;
                if (mHapEngine != null) {
                    appInfo = mHapEngine.getApplicationContext().getAppInfo();
                }
                HashMap<String, String> reportParams = new HashMap<>();
                String packageName = null != appInfo ? appInfo.getPackage() : "";
                reportParams.put(KEY_RPK_VERSION, null != appInfo ? appInfo.getVersionCode() + "" : "");
                reportParams.put(KEY_RPK_PACKAGE, packageName);
                logProvider.logCountEvent(packageName, "", type, reportParams);
            }

        }
    }

    private void createShortCut(String eventName, Map<String, Object> data) {
        Executors.io().execute(new Runnable() {
            @Override
            public void run() {
                String rpkPackage = "";
                String rpkName = "";
                Uri rpkIcon;
                ApplicationContext applicationContext;
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
                            fireGestureEvent(CREATE_SHORTCUT_ALREADY, eventName, data, rpkName);
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
                                fireGestureEvent(CREATE_SHORTCUT_SUCCESS, eventName, data, rpkName);
                            } else {
                                if (null != data) {
                                    data.put(EVENT_STATUS, ERROR);
                                    data.put(EVENT_MESSAGE, "createShortCut fail.");
                                } else {
                                    Log.e(TAG, "createShortCut  install fail  data is null.");
                                }
                                Log.e(TAG, "createShortCut  install fail.");
                                fireGestureEvent(CREATE_SHORTCUT_FAIL, eventName, data, rpkName);
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

    private void fireGestureEvent(int createCode, String eventName, Map<String, Object> data, String rpkName) {
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
                reportTraceEvent(RuntimeLogManager.KEY_APP_EVENT_BUTTON_CLICK);
            }
        });
    }
}
