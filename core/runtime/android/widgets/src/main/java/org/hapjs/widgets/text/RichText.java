/*
 * Copyright (c) 2021-present, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.widgets.text;

import android.content.Context;
import android.graphics.Bitmap;
import android.text.TextUtils;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.FrameLayout;

import com.facebook.yoga.YogaNode;

import java.util.HashMap;
import java.util.Map;
import org.hapjs.bridge.annotation.WidgetAnnotation;
import org.hapjs.common.utils.ColorUtil;
import org.hapjs.common.utils.FloatUtil;
import org.hapjs.component.Component;
import org.hapjs.component.Container;
import org.hapjs.component.bridge.RenderEventCallback;
import org.hapjs.component.constants.Attributes;
import org.hapjs.component.utils.YogaUtil;
import org.hapjs.component.view.flexbox.PercentFlexboxLayout;
import org.hapjs.component.view.gesture.GestureHost;
import org.hapjs.component.view.gesture.IGesture;
import org.hapjs.component.view.webview.BaseWebViewClient;
import org.hapjs.runtime.HapEngine;
import org.hapjs.widgets.view.BookPagerView;

@WidgetAnnotation(
        name = RichText.WIDGET_NAME,
        methods = {
                RichText.METHOD_ADD_NEXT_CONTENT,
                Component.METHOD_GET_BOUNDING_CLIENT_RECT,
                Component.METHOD_ANIMATE,
                Component.METHOD_TO_TEMP_FILE_PATH,
                Component.METHOD_FOCUS,
                Component.METHOD_TALKBACK_FOCUS,
                Component.METHOD_TALKBACK_ANNOUNCE
        })
public class RichText extends Container<View> {
    protected static final String WIDGET_NAME = "richtext";
    private static final String TAG = "RichText";
    private static final String KEY_TYPE = "type";
    private static final String KEY_SCENE = "scene";
    private static final String TYPE_MIX = "mix";
    private static final String TYPE_HTML = "html";
    private static final String BOOK_SCENE = "book";

    // Event
    public static final String METHOD_ADD_NEXT_CONTENT = "addContent";
    private static final String EVENT_START = "start";
    private static final String EVENT_COMPLETE = "complete";

    private OnStartListener mOnStartListener;
    private OnCompleteListener mOnCompleteListener;

    public RichText(
            HapEngine hapEngine,
            Context context,
            Container parent,
            int ref,
            RenderEventCallback callback,
            Map<String, Object> savedState) {
        super(hapEngine, context, parent, ref, callback, savedState);
    }

    @Override
    protected View createViewImpl() {
        if (getAttrsDomData().containsKey(KEY_SCENE)) {
            String scene = (String) getAttrsDomData().get(KEY_SCENE);
            //翻页图文场景
            if (BOOK_SCENE.equals(scene)) {
                BookPagerView mBookPagerView = new BookPagerView(mContext);
                mBookPagerView.setComponent(this);
                ViewGroup.LayoutParams lp = new ViewGroup.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                mBookPagerView.setLayoutParams(lp);
                mBookPagerView.setBookEventListener(new BookPagerView.BookEventListener() {
                    @Override
                    public void onPageSplitEnd(int totalPage) {
                        HashMap<String, Object> params = new HashMap<>();
                        params.put("totalpage", totalPage);
                        mCallback.onJsEventCallback(getPageId(), mRef, Attributes.Event.PAGE_SPLIT, RichText.this, params, null);
                    }

                    @Override
                    public void onPageChanged(int curPage, int totalPage) {
                        HashMap<String, Object> params = new HashMap<>();
                        params.put("curpage", curPage);
                        params.put("totalpage", totalPage);
                        mCallback.onJsEventCallback(getPageId(), mRef, Attributes.Event.PAGE_CHANGED, RichText.this, params, null);
                    }
                });
                return mBookPagerView;
            }
        } else {
            String type = TYPE_MIX;
            if (getAttrsDomData().containsKey(KEY_TYPE)) {
                String attrType = (String) getAttrsDomData().get(KEY_TYPE);
                if (TYPE_HTML.equals(attrType)) {
                    type = TYPE_HTML;
                }
            }

            switch (type) {
                case TYPE_MIX:
                    PercentFlexboxLayout percentFlexboxLayout = new PercentFlexboxLayout(mContext);
                    percentFlexboxLayout.setComponent(this);
                    return percentFlexboxLayout;
                case TYPE_HTML:
                    WebView webView = new HtmlWebView(mContext);
                    mCallback.addActivityStateListener(this);
                    return webView;
                default:
                    return null;
            }
        }
        return null;
    }

    @Override
    public void invokeMethod(String methodName, Map<String, Object> args) {
        super.invokeMethod(methodName, args);
        if (METHOD_ADD_NEXT_CONTENT.equals(methodName)) {
            if (mHost instanceof BookPagerView) {
                BookPagerView bookPagerView = (BookPagerView) mHost;
                bookPagerView.addOriginText(Attributes.getString(args.get("value")), (int) Attributes.getLong(args.get("index"), -1));
            }
        }
    }

    @Override
    protected boolean setAttribute(String key, Object attribute) {
        switch (key) {
            case Attributes.Style.VALUE:
                String value = Attributes.getString(attribute, "");
                setValue(value);
                return true;
            case Attributes.Style.BACKGROUND_COLOR:
                String colorStr = Attributes.getString(attribute, "transparent");
                setBackgroundColor(colorStr);
                return true;
            default:
                break;
        }
        return super.setAttribute(key, attribute);
    }

    public void setValue(String value) {
        if (value == null || mHost == null) {
            return;
        }

        if (mHost instanceof WebView) {
            WebView webView = (WebView) mHost;
            webView.loadDataWithBaseURL(null, value, "text/html; charset=UTF-8", "UTF-8", null);
        }else if (mHost instanceof BookPagerView) {
            BookPagerView bookPagerView = (BookPagerView) mHost;
            bookPagerView.setOriginText(value);
        }
    }

    public void setBackgroundColor(String colorStr) {
        if (TextUtils.isEmpty(colorStr) || mHost == null) {
            return;
        }
        if (mHost instanceof WebView) {
            WebView webView = (WebView) mHost;
            webView.setBackgroundColor(ColorUtil.getColor(colorStr));
        }
    }

    @Override
    public void onActivityResume() {
        super.onActivityResume();
        if (mHost instanceof WebView) {
            WebView webView = ((WebView) mHost);
            webView.onResume();
        }
    }

    @Override
    public void onActivityPause() {
        super.onActivityPause();
        if (mHost instanceof WebView) {
            WebView webView = ((WebView) mHost);
            webView.onPause();
        }
    }

    @Override
    public void destroy() {
        super.destroy();
        if (mHost instanceof WebView) {
            WebView webView = ((WebView) mHost);
            webView.removeAllViews();
            webView.destroy();
            mHost = null;
            mCallback.removeActivityStateListener(this);
        }
    }

    @Override
    protected boolean addEvent(String event) {
        if (TextUtils.isEmpty(event) || mHost == null) {
            return true;
        }
        if (EVENT_START.equals(event)) {
            if (mHost instanceof HtmlWebView) {
                ((HtmlWebView) mHost)
                        .setOnStartListener(
                                new OnStartListener() {
                                    @Override
                                    public void onStart() {
                                        mCallback.onJsEventCallback(
                                                getPageId(), mRef, EVENT_START, RichText.this, null,
                                                null);
                                    }
                                });
            }
            return true;
        } else if (EVENT_COMPLETE.equals(event)) {
            if (mHost instanceof HtmlWebView) {
                ((HtmlWebView) mHost)
                        .setOnCompleteListener(
                                new OnCompleteListener() {
                                    @Override
                                    public void onComplete() {
                                        mCallback.onJsEventCallback(
                                                getPageId(), mRef, EVENT_COMPLETE, RichText.this,
                                                null, null);
                                    }
                                });
            }
            return true;
        }
        return super.addEvent(event);
    }

    public interface OnStartListener {
        void onStart();
    }

    public interface OnCompleteListener {
        void onComplete();
    }

    private class HtmlWebView extends WebView implements GestureHost {

        private IGesture mGesture;

        public HtmlWebView(Context context) {
            super(context);
            setWebViewClient(
                    new BaseWebViewClient(BaseWebViewClient.WebSourceType.RICH_TEXT) {
                        @Override
                        public boolean shouldOverrideUrlLoading(WebView view, String url) {
                            mCallback.loadUrl(url);
                            return true;
                        }

                        @Override
                        public void onPageStarted(WebView view, String url, Bitmap favicon) {
                            super.onPageStarted(view, url, favicon);
                            if (mOnStartListener != null) {
                                mOnStartListener.onStart();
                            }
                        }

                        @Override
                        public void onPageFinished(WebView view, String url) {
                            super.onPageFinished(view, url);
                            if (mOnCompleteListener != null) {
                                mOnCompleteListener.onComplete();
                            }
                        }
                    });

            WebSettings settings = getSettings();
            settings.setJavaScriptEnabled(false);
            settings.setSavePassword(false);
            settings.setAllowFileAccess(false);
            settings.setAllowUniversalAccessFromFileURLs(false);
            settings.setAllowFileAccessFromFileURLs(false);
            try {
                removeJavascriptInterface("searchBoxJavaBridge_");
                removeJavascriptInterface("accessibility");
                removeJavascriptInterface("accessibilityTraversal");
            } catch (Exception e) {
                Log.e(TAG, "initWebView: ", e);
            }
            setVerticalScrollBarEnabled(false);
        }

        public void setOnStartListener(OnStartListener l) {
            mOnStartListener = l;
        }

        public void setOnCompleteListener(OnCompleteListener l) {
            mOnCompleteListener = l;
        }

        @Override
        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            if (!isHeightDefined()) {
                heightMeasureSpec = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED);
            }

            super.onMeasure(widthMeasureSpec, heightMeasureSpec);

            // height measure spec may be changed in onMeasure, so remeasure it.
            YogaNode node = YogaUtil.getYogaNode(this);
            if (node != null
                    && !FloatUtil.floatsEqual(node.getHeight().value, getMeasuredHeight())) {
                node.setHeight(getMeasuredHeight());
                post(
                        new Runnable() {
                            @Override
                            public void run() {
                                requestLayout();
                            }
                        });
            }
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
    }
}
