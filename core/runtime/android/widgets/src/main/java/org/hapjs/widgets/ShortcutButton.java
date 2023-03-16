/*
 * Copyright (c) 2022-present, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.widgets;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;
import android.util.TypedValue;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import org.hapjs.bridge.ApplicationContext;
import org.hapjs.bridge.annotation.WidgetAnnotation;
import org.hapjs.common.executors.Executors;
import org.hapjs.common.utils.ColorUtil;
import org.hapjs.common.utils.IntegerUtil;
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
import org.hapjs.logging.Source;
import org.hapjs.component.view.metrics.Metrics;
import org.hapjs.model.AppInfo;
import org.hapjs.model.ConfigInfo;
import org.hapjs.render.Page;
import org.hapjs.runtime.HapEngine;
import org.hapjs.runtime.ProviderManager;
import org.hapjs.widgets.input.Button;
import org.hapjs.widgets.view.swiper.ViewPager;

import java.util.HashMap;
import java.util.Map;

import static org.hapjs.logging.LogProvider.KEY_RPK_PACKAGE;
import static org.hapjs.logging.LogProvider.KEY_RPK_VERSION;
import static org.hapjs.logging.RuntimeLogManager.KEY_APP_EVENT_BUTTON_CLICK;
import static org.hapjs.logging.RuntimeLogManager.KEY_APP_EVENT_BUTTON_OPACITY;
import static org.hapjs.logging.RuntimeLogManager.KEY_APP_EVENT_BUTTON_SHOW;


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
    private static final String DEFAULT_BACKGROUND_COLOR = "#ffd6d7d7";
    private static final String SHORTCUT_BUTTON_TYPE = "type";
    private static final String SHORTCUT_BUTTON_OPACITY = "opacity";
    private static final String SHORTCUT_BUTTON_COMMON_TYPE = "2";

    private static final int SUCCESS = 0;
    private static final int ERROR = 200;
    protected static final String MAX_HEIGHT = "180px";
    protected static final int DEFAULT_WIDTH_VALUE = 128;
    protected static final int DEFAULT_HEIGHT_VALUE = 70;
    protected static final int MAX_HEIGHT_VALUE = 180;
    protected static final float DEFAULT_FONT_SIZE_VALUE = 37.5f;
    boolean mIsLowDesign = false;
    float mScaleValue = 1.0f;


    private int mPreColor = 0;
    private int mCurFontSize;

    public ShortcutButton(HapEngine hapEngine, Context context, Container parent, int ref, RenderEventCallback callback, Map<String, Object> savedState) {
        super(hapEngine, context, parent, ref, callback, savedState);
    }

    @Override
    protected TextView createViewImpl() {
        TextView view = super.createViewImpl();
        HashMap<String, String> reportParams = new HashMap<>();
        reportTraceEvent(KEY_APP_EVENT_BUTTON_SHOW, reportParams);
        return view;
    }

    @Override
    protected void initDefaultView(TextView view) {
        mIsLowDesign = mHapEngine.getDesignWidth() < ConfigInfo.DEFAULT_DESIGN_WIDTH;
        if (mIsLowDesign) {
            float designWidth = mHapEngine.getDesignWidth();
            float defaultWidth = ConfigInfo.DEFAULT_DESIGN_WIDTH;
            mScaleValue = designWidth / defaultWidth;
            if (mScaleValue <= 0) {
                mScaleValue = 1.0f;
            }
        }
        Page page = getPage();
        view.setTextSize(TypedValue.COMPLEX_UNIT_PX, Attributes.getFontSize(mHapEngine, page, DEFAULT_FONT_SIZE_VALUE * mScaleValue + Attributes.Unit.PX));
        view.setTextColor(ColorUtil.getColor(DEFAULT_COLOR));
        int minWidth = Attributes.getInt(mHapEngine, DEFAULT_WIDTH_VALUE * mScaleValue + Attributes.Unit.PX);
        view.setMinWidth(minWidth);
        view.setMinimumWidth(minWidth);
        int minHeight = Attributes.getInt(mHapEngine, DEFAULT_HEIGHT_VALUE * mScaleValue + Attributes.Unit.PX);
        view.setMinHeight(minHeight);
        view.setMinimumHeight(minHeight);
        mCurFontSize = Attributes.getFontSize(mHapEngine, getPage(), DEFAULT_FONT_SIZE_VALUE * mScaleValue + Attributes.Unit.PX);
        int maxHeight = Attributes.getInt(mHapEngine, MAX_HEIGHT_VALUE * mScaleValue + Attributes.Unit.PX);
        view.setMaxHeight(maxHeight);
    }

    @Override
    protected boolean setAttribute(String key, Object attribute) {
        switch (key) {
            case Attributes.Style.VALUE:
            case Attributes.Style.CONTENT:
                //限定文字内容  添桌  桌面  添加
                String text = Attributes.getString(attribute, mContext.getResources().getString(R.string.shortcut_add_shortcut_long));
                if ((!TextUtils.isEmpty(text) && (text.contains(mContext.getResources().getString(R.string.shortcut_add_shortcut_mini)) ||
                        (text.contains(mContext.getResources().getString(R.string.shortcut_add_shortcut_desktop))))) ||
                        mContext.getResources().getString(R.string.shortcut_add_shortcut_long).equalsIgnoreCase(text)) {
                    setText(text);
                } else {
                    setText(mContext.getResources().getString(R.string.shortcut_add_shortcut_long));
                }
                checkValue();
                return true;
            case Attributes.Style.BACKGROUND_COLOR:
                //颜色透明度不能低于0.5 背景色和文本颜色不能相似
                String colorStr_bg = Attributes.getString(attribute, DEFAULT_BACKGROUND_COLOR);
                int color_bg = ColorUtil.getColor(colorStr_bg);
                float alpha_bg = (float) (Color.alpha(color_bg)) / 255;
                try {
                    if (alpha_bg < 0.5) {
                        colorStr_bg = DEFAULT_BACKGROUND_COLOR;
                    } else if (ColorUtil.LabDiff(color_bg, mHost.getCurrentTextColor()) < 20) {
                        if (mPreColor == 0) {
                            mPreColor = color_bg;
                            colorStr_bg = DEFAULT_BACKGROUND_COLOR;
                        } else {
                            if (ColorUtil.LabDiff(color_bg, mPreColor) < 20) {
                                mPreColor = color_bg;
                                colorStr_bg = DEFAULT_BACKGROUND_COLOR;
                            } else {
                                mHost.setTextColor(mPreColor);
                            }
                        }
                    }
                } catch (Exception e) {
                    Log.e(TAG, "set backgroundColor error" + e.getMessage());
                }
                setBackgroundColor(colorStr_bg);
                return true;
            case Attributes.Style.COLOR:
                //颜色透明度不能低于0.5 背景色和文本颜色不能相似
                String colorStr = Attributes.getString(attribute, DEFAULT_COLOR);
                int color = ColorUtil.getColor(colorStr);
                float alpha = (float) (Color.alpha(color)) / 255;
                int bgColor;
                if (getBackgroundColor() == 0) {
                    bgColor = ColorUtil.getColor(DEFAULT_BACKGROUND_COLOR);
                } else {
                    bgColor = getBackgroundColor();
                }
                try {
                    if (alpha < 0.5) {
                        color = ColorUtil.getColor(DEFAULT_COLOR);
                    } else if (ColorUtil.LabDiff(color, bgColor) < 20) {
                        if (mPreColor == 0) {
                            mPreColor = color;
                            color = ColorUtil.getColor(DEFAULT_COLOR);
                        } else {
                            if (ColorUtil.LabDiff(color, mPreColor) < 20) {
                                mPreColor = color;
                                color = ColorUtil.getColor(DEFAULT_COLOR);
                            } else {
                                setBackgroundColor(mPreColor);
                            }
                        }
                    }
                } catch (Exception e) {
                    Log.e(TAG, "set Color error" + e.getMessage());
                }
                mHost.setTextColor(color);
                return true;
            case Attributes.Style.HEIGHT:
                int minHeight = Attributes.getInt(mHapEngine, DEFAULT_HEIGHT_VALUE * mScaleValue + Attributes.Unit.PX);
                String heightstr = Attributes.getString(attribute, DEFAULT_HEIGHT_VALUE * mScaleValue + Attributes.Unit.PX);
                int height = Attributes.getInt(mHapEngine, heightstr);
                int maxHeight = Attributes.getInt(mHapEngine, MAX_HEIGHT_VALUE * mScaleValue + Attributes.Unit.PX);
                if (height < minHeight || TextUtils.isEmpty(heightstr) || heightstr.endsWith(Metrics.PERCENT)) {
                    setHeight(DEFAULT_HEIGHT_VALUE * mScaleValue + Attributes.Unit.PX);
                } else if (height > maxHeight) {
                    setHeight(MAX_HEIGHT_VALUE * mScaleValue + Attributes.Unit.PX);
                } else {
                    setHeight(heightstr);
                }
                checkValue();
                return true;
            case Attributes.Style.WIDTH:
                int minWidth = Attributes.getInt(mHapEngine, DEFAULT_WIDTH_VALUE * mScaleValue + Attributes.Unit.PX);
                String widthstr = Attributes.getString(attribute, DEFAULT_WIDTH_VALUE * mScaleValue + Attributes.Unit.PX);
                int width = Attributes.getInt(mHapEngine, widthstr);
                if (width < minWidth || TextUtils.isEmpty(widthstr) || widthstr.endsWith(Metrics.PERCENT)) {
                    setWidth(DEFAULT_WIDTH_VALUE * mScaleValue + Attributes.Unit.PX);
                } else {
                    setWidth(widthstr);
                }
                checkValue();
                return true;
            case Attributes.Style.FONT_SIZE:
                int defaultFontSize = Attributes.getFontSize(mHapEngine, getPage(), DEFAULT_FONT_SIZE_VALUE * mScaleValue + Attributes.Unit.PX);
                int fontSize = Attributes.getFontSize(mHapEngine, getPage(), attribute, defaultFontSize);
                if (fontSize < defaultFontSize) {
                    fontSize = defaultFontSize;
                }
                setFontSize(fontSize);
                mCurFontSize = fontSize;
                checkValue();
                return true;
            case Attributes.Style.PADDING:
            case Attributes.Style.PADDING_LEFT:
            case Attributes.Style.PADDING_RIGHT:
            case Attributes.Style.PADDING_TOP:
            case Attributes.Style.PADDING_BOTTOM:
            case Attributes.Style.BORDER_WIDTH:
            case Attributes.Style.BORDER_LEFT_WIDTH:
            case Attributes.Style.BORDER_TOP_WIDTH:
            case Attributes.Style.BORDER_RIGHT_WIDTH:
            case Attributes.Style.BORDER_BOTTOM_WIDTH:
                super.setAttribute(key, attribute);
                checkValue();
                return true;
            case Attributes.Style.BACKGROUND:
                return true;
            case Attributes.Style.OPACITY:
                float opacity = Attributes.getFloat(mHapEngine, attribute, 1f);
                setOpacity(opacity);
                HashMap<String, String> reportParams = new HashMap<>();
                reportParams.put(SHORTCUT_BUTTON_OPACITY, opacity + "");
                reportTraceEvent(KEY_APP_EVENT_BUTTON_OPACITY, reportParams);
                return true;
        }
        return super.setAttribute(key, attribute);
    }

    private void checkValue() {
        if (null == mHost) {
            Log.w(TAG, "checkValue mHost is null.");
            return;
        }
        int width = getWidth();
        int height = getHeight();
        if (width == ViewGroup.LayoutParams.WRAP_CONTENT) {
            setWidth(DEFAULT_WIDTH_VALUE * mScaleValue + Attributes.Unit.PX);
            width = getWidth();
        }
        if (height == ViewPager.LayoutParams.WRAP_CONTENT) {
            setHeight(DEFAULT_HEIGHT_VALUE * mScaleValue + Attributes.Unit.PX);
            height = getHeight();
        }
        if (TextUtils.isEmpty(mHost.getText()) || width == IntegerUtil.UNDEFINED || height == IntegerUtil.UNDEFINED || null == mHost) {
            return;
        }
        int fontSize = mCurFontSize;
        float textWidth = 0;
        CharSequence text = mHost.getText();
        if (null == text) {
            text = mContext.getResources().getString(R.string.shortcut_add_shortcut_long);
            mHost.setText(text);
        }
        if (null != mHost.getPaint()) {
            textWidth = mHost.getPaint().measureText(text.toString());
        } else {
            textWidth = text.length() * fontSize;
        }
        float currentLPadding = getPadding(Attributes.Style.PADDING_LEFT);
        float currentRPadding = getPadding(Attributes.Style.PADDING_RIGHT);
        float currentTPadding = getPadding(Attributes.Style.PADDING_TOP);
        float currentBPadding = getPadding(Attributes.Style.PADDING_BOTTOM);
        float currentLBorder = getBorderWidth(Attributes.Style.BORDER_LEFT_WIDTH);
        float currentRBorder = getBorderWidth(Attributes.Style.BORDER_RIGHT_WIDTH);
        float currentTBorder = getBorderWidth(Attributes.Style.BORDER_TOP_WIDTH);
        float currentBBorder = getBorderWidth(Attributes.Style.BORDER_BOTTOM_WIDTH);
        boolean isWidthValid = (width - textWidth - currentLPadding - currentRPadding - currentLBorder - currentRBorder >= 0);
        boolean isHeightValid = (height - fontSize - currentTPadding - currentBPadding - currentTBorder - currentBBorder >= 0);
        if (!isWidthValid || !isHeightValid && null != mHost) {
            fontSize = Attributes.getFontSize(mHapEngine, getPage(), DEFAULT_FONT_SIZE_VALUE * mScaleValue + Attributes.Unit.PX);
            setFontSize(fontSize);
            mCurFontSize = fontSize;
            setBorderWidth(Attributes.Style.BORDER_LEFT_WIDTH, 0);
            setBorderWidth(Attributes.Style.BORDER_RIGHT_WIDTH, 0);
            setBorderWidth(Attributes.Style.BORDER_TOP_WIDTH, 0);
            setBorderWidth(Attributes.Style.BORDER_BOTTOM_WIDTH, 0);
            if (!isWidthValid) {
                setPadding(Attributes.Style.PADDING_LEFT, 0);
                setPadding(Attributes.Style.PADDING_RIGHT, 0);
                if (null != mHost.getPaint()) {
                    textWidth = mHost.getPaint().measureText(text.toString());
                } else {
                    textWidth = text.length() * fontSize;
                }
                String realWidthDpStr = (textWidth / mContext.getResources().getDisplayMetrics().density + 1f) + Attributes.Unit.DP;
                int minWidth = Attributes.getInt(mHapEngine, DEFAULT_WIDTH_VALUE * mScaleValue + Attributes.Unit.PX);
                if (textWidth < minWidth) {
                    realWidthDpStr = DEFAULT_WIDTH_VALUE * mScaleValue + Attributes.Unit.PX;
                }
                setWidth(realWidthDpStr);
            }
            if (!isHeightValid) {
                setPadding(Attributes.Style.PADDING_TOP, 0);
                setPadding(Attributes.Style.PADDING_BOTTOM, 0);
                setHeight(DEFAULT_HEIGHT_VALUE * mScaleValue + Attributes.Unit.PX);
            }
        }
    }

    @Override
    public float getBorderWidth(String position) {
        float borderWidth = super.getBorderWidth(position);
        if (Float.isNaN(borderWidth)) {
            borderWidth = 0;
        }
        return borderWidth;
    }

    public boolean preConsumeEvent(String eventName, Map<String, Object> data, boolean immediately) {
        boolean isConsume = false;
        if (Attributes.Event.TOUCH_CLICK.equals(eventName)) {
            isConsume = true;
            createShortCut(eventName, data);
        }
        return isConsume;
    }

    private void reportTraceEvent(String type, HashMap<String, String> reportParams) {
        if (null != mContext && null != reportParams) {
            LogProvider logProvider = (LogProvider) ProviderManager.getDefault()
                    .getProvider(LogProvider.NAME);
            if (null != logProvider) {
                AppInfo appInfo = null;
                if (mHapEngine != null) {
                    appInfo = mHapEngine.getApplicationContext().getAppInfo();
                }
                String packageName = null != appInfo ? appInfo.getPackage() : "";
                reportParams.put(KEY_RPK_VERSION, null != appInfo ? appInfo.getVersionCode() + "" : "");
                reportParams.put(KEY_RPK_PACKAGE, packageName);
                reportParams.put(SHORTCUT_BUTTON_TYPE, SHORTCUT_BUTTON_COMMON_TYPE);
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
                HashMap<String, String> reportParams = new HashMap<>();
                reportTraceEvent(KEY_APP_EVENT_BUTTON_CLICK, reportParams);
            }
        });
    }
}
