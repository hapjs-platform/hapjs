/*
 * Copyright (c) 2023-present, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.hapjs.widgets;

import android.content.Context;
import android.graphics.Color;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.FrameLayout;

import com.facebook.yoga.YogaAlign;
import com.facebook.yoga.YogaEdge;
import com.facebook.yoga.YogaFlexDirection;
import com.facebook.yoga.YogaJustify;

import org.hapjs.bridge.annotation.WidgetAnnotation;
import org.hapjs.common.utils.ColorUtil;
import org.hapjs.component.Component;
import org.hapjs.component.Container;
import org.hapjs.component.bridge.RenderEventCallback;
import org.hapjs.component.constants.Attributes;
import org.hapjs.component.view.ComponentHost;
import org.hapjs.component.view.YogaLayout;
import org.hapjs.component.view.flexbox.PercentFlexboxLayout;
import org.hapjs.model.ConfigInfo;
import org.hapjs.render.css.value.CSSValues;
import org.hapjs.runtime.HapEngine;
import org.hapjs.widgets.view.readerdiv.ReaderLayoutView;
import org.hapjs.widgets.view.readerdiv.ReaderPageView;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.hapjs.widgets.view.readerdiv.ReaderLayoutView.READER_LOG_TAG;
import static org.hapjs.widgets.view.readerdiv.ReaderPageView.SHADER_WIDTH;

@WidgetAnnotation(
        name = ReaderDiv.WIDGET_NAME,
        methods = {
                Component.METHOD_ANIMATE,
                Component.METHOD_REQUEST_FULLSCREEN,
                Component.METHOD_GET_BOUNDING_CLIENT_RECT,
                Component.METHOD_TO_TEMP_FILE_PATH,
                Component.METHOD_FOCUS,
                ReaderDiv.METHOD_PRE_PAGE,
                ReaderDiv.METHOD_NEXT_PAGE,
                ReaderDiv.METHOD_PRELOAD_PAGE,
                ReaderDiv.METHOD_SET_CONTENT,
                ReaderDiv.METHOD_PRELOAD_CONTENT,
                ReaderDiv.METHOD_SET_PAGE_LINE_SPACE,
                ReaderDiv.METHOD_SET_PAGE_COLOR,
                ReaderDiv.METHOD_GET_PAGE_CONTENT
        }
)
public class ReaderDiv extends Container<ReaderLayoutView> {

    protected static final String WIDGET_NAME = "reader-div";

    private YogaFlexDirection mFlexDirection = YogaFlexDirection.ROW;
    private YogaJustify mJustifyContent = YogaJustify.FLEX_START;
    private YogaAlign mAlignItems = YogaAlign.STRETCH;
    private final String TAG = "ReaderDiv";
    private View mAdView = null;
    //method
    protected static final String METHOD_NEXT_PAGE = "goNextPage";
    protected static final String METHOD_PRE_PAGE = "goPrePage";
    protected static final String METHOD_PRELOAD_PAGE = "preLoadPage";
    protected static final String METHOD_PRELOAD_CONTENT = "preLoadContent";
    protected static final String METHOD_SET_CONTENT = "setContent";//设置当前章节文本内容，设置之前需要先获取分段标记，否则返回错误
    protected static final String METHOD_SET_PAGE_COLOR = "setPageColor";//start end有则是当前页，否则是背景色--广告页也需要设置
    protected static final String METHOD_GET_PAGE_CONTENT = "getPageContent";//index  页面参数   start  end 起始终止行数 然后会返回相应内容
    protected static final String METHOD_SET_PAGE_LINE_SPACE = "setLineSpace";//设置行间距 low，normal high
    public static final String READERDIV_LINE_SPACE_TYPE = "type";
    public static final String READERDIV_LINE_SPACE_LOW = "low";
    public static final String READERDIV_LINE_SPACE_NORMAL = "normal";
    public static final String READERDIV_LINE_SPACE_HIGH = "high";
    public static final String READERDIV_PRELOAD_FORWARD = "forward";
    public static final String READERDIV_PRELOAD_READERMODE = "readermode";
    public static final String READERDIV_CALLBACK_MESSAGE = "message";
    public static final String READERDIV_PAGE_LOAD_TYPE = "forward";
    public static final String READERDIV_PAGE_TITLE = "title";
    public static final String READERDIV_PAGE_CONTENT = "content";
    public static final String READERDIV_PAGE_LINECONTENT = "lines";
    public static final String READERDIV_PAGE_COLOR = "pagecolor";
    public static final String READERDIV_PAGE_START = "startline";
    public static final String READERDIV_PAGE_END = "endline";
    public static final String READERDIV_PAGE_INDEX = "pageIndex";
    static final int DEFAULT_FONT_SIZE = 42;
    static final int DEFAULT_MIN_FONT_SIZE = 36;
    static final int DEFAULT_MAX_FONT_SIZE = 60;
    private float mScaleValue = 1.0f;
    static final String DEFAULT_COLOR = "#8a000000";
    private int FONT_SIZE_MIN = -1;
    private int FONT_SIZE_MAX = -1;
    //阅读模式 左右 上线
    private final static String READER_VIEW_MOVE_MODE = "movemode";
    private final static String READER_SECTION_BREAKCHARS = "sectionbreak";
    public final static String READER_VIEW_MODE_TEXT = "text";
    public final static String READER_VIEW_MODE_AD = "ad";
    public final static String READER_PAGE_MODE_HORIZON = "horizontal";
    public final static String READER_PAGE_MODE_VERTICAL = "vertical";

    //每次翻页后返回当前的页面
    private final String READER_PAGE_CHANGE = "pagechange";
    //章节末尾回调
    private final String READER_CHAPTER_END = "chapterend";
    //章节开头回调
    private final String READER_CHAPTER_START = "chapterstart";
    private long mChapterEndTime = -1;
    private long mChapterStartTime = -1;
    private String mReaderMode = READER_VIEW_MODE_TEXT;
    private int mPageColor = Color.WHITE;
    private String mPageContent = "";
    private String mPageTitle = "";
    private String mPageMoveMode = READER_PAGE_MODE_HORIZON;
    private static final String PARAM_SOURCE_PKG = "source_pkg";//启动来源包名
    private static final String PARAM_SOURCE_TYPE = "source_type";//启动来源类型
    private static final String PARAM_APP_PACKAGE = "rpk_package"; // 应用包名
    protected static final String PARAM_RPK_VERSION = "rpk_version";
    protected static final String PARAM_BUTTON_NAME = "button_name";
    protected static final int EVENT_HORIZON_MODE = 4;
    protected static final int EVENT_VERTICAL_MODE = 5;
    private static String source_pkg = "";
    private static String source_type = "";

    public ReaderDiv(HapEngine hapEngine, Context context, Container parent, int ref, RenderEventCallback callback,
                     Map<String, Object> savedState) {
        super(hapEngine, context, parent, ref, callback, savedState);
        boolean isDefaultDesignWidth = mHapEngine.getDesignWidth() == ConfigInfo.DEFAULT_DESIGN_WIDTH;
        if (!isDefaultDesignWidth) {
            float designWidth = mHapEngine.getDesignWidth();
            float defaultWidth = ConfigInfo.DEFAULT_DESIGN_WIDTH;
            mScaleValue = designWidth / defaultWidth;
            if (mScaleValue <= 0) {
                mScaleValue = 1.0f;
            }
        }
        FONT_SIZE_MIN = Attributes.getFoldFontSize(mHapEngine, getPage(), DEFAULT_MIN_FONT_SIZE * mScaleValue + Attributes.Unit.PX, true);
        FONT_SIZE_MAX = Attributes.getFoldFontSize(mHapEngine, getPage(), DEFAULT_MAX_FONT_SIZE * mScaleValue + Attributes.Unit.PX, true);
        setFontSize(Attributes.getFoldFontSize(mHapEngine, getPage(), getDefaultFontSize(), true));
        setFontColor(DEFAULT_COLOR);
    }

    @Override
    protected ReaderLayoutView createViewImpl() {
        if (null != mStyleDomData) {
            Object attributeMap = mStyleDomData.get(Attributes.Style.BACKGROUND_COLOR);
            if (attributeMap instanceof CSSValues) {
                String applyState = getState(Attributes.Style.BACKGROUND_COLOR);
                Object attribute = ((CSSValues) attributeMap).get(applyState);
                if (null != attribute) {
                    String colorStr = Attributes.getString(attribute, "");
                    int color = ColorUtil.getColor(colorStr, Color.WHITE);
                    mPageColor = color;
                } else {
                    Log.w(TAG, "createViewImpl attribute is null.");
                }
            } else {
                Log.w(TAG, "createViewImpl attributeMap is not valid.");
            }
        } else {
            Log.w(TAG, "createViewImpl mStyleDomData is null.");
        }
        String spliteStr = null;
        String readerPageMode = READER_PAGE_MODE_HORIZON;
        if (mAttrsDomData.get(READER_SECTION_BREAKCHARS) instanceof String) {
            spliteStr = (String) mAttrsDomData.get(READER_SECTION_BREAKCHARS);
        }
        if (mAttrsDomData.get(READER_VIEW_MOVE_MODE) instanceof String) {
            readerPageMode = (String) mAttrsDomData.get(READER_VIEW_MOVE_MODE);
        }
        if (TextUtils.isEmpty(readerPageMode) || !(ReaderDiv.READER_PAGE_MODE_HORIZON.equals(readerPageMode)
                || ReaderDiv.READER_PAGE_MODE_VERTICAL.equals(readerPageMode))) {
            readerPageMode = ReaderDiv.READER_PAGE_MODE_HORIZON;
            Log.w(TAG, READER_LOG_TAG + " createViewImpl readerPageMode is not valid : " + readerPageMode);
        }
        ReaderLayoutView readerLayoutView = new ReaderLayoutView(mContext, mPageColor, spliteStr, readerPageMode);
        readerLayoutView.setComponent(this);
        return readerLayoutView;
    }

    public View getAdView() {
        return mAdView;
    }

    @Override
    public void addView(View childView, int index) {
        if (mHost == null || childView == null) {
            return;
        }
        mAdView = childView;
    }

    @Override
    protected boolean setAttribute(String key, Object attribute) {
        switch (key) {
            case READER_SECTION_BREAKCHARS:
                String sectionbreak = Attributes.getString(attribute, "");
                Log.w(TAG, "setAttribute READER_SECTION_BREAKCHARS sectionbreak : " + sectionbreak);
                if (null != mHost && !TextUtils.isEmpty(sectionbreak)) {
                    mHost.setSplitString(sectionbreak);
                }
                return true;
            case Attributes.Style.FONT_SIZE:
                int defaultFontSize = Attributes.getFoldFontSize(mHapEngine, getPage(), getDefaultFontSize(), true);
                int fontSize = Attributes.getFoldFontSize(mHapEngine, getPage(), attribute, defaultFontSize, true);
                if (fontSize <= FONT_SIZE_MIN && FONT_SIZE_MIN != -1) {
                    fontSize = FONT_SIZE_MIN;
                } else if (fontSize >= FONT_SIZE_MAX && FONT_SIZE_MAX != -1) {
                    fontSize = FONT_SIZE_MAX;
                }
                boolean isValid = setFontSize(fontSize);
                if (isValid) {
                    mHost.reCalculatePage(null, true);
                }
                return true;
            case Attributes.Style.COLOR:
                String colorStr = Attributes.getString(attribute, getDefaultColor());
                setFontColor(colorStr);
                return true;
            case Attributes.Style.BACKGROUND_COLOR:
                String bgcolorStr = Attributes.getString(attribute, "");
                int color = ColorUtil.getColor(bgcolorStr, Color.WHITE);
                mPageColor = color;
                if (null != mHost) {
                    mHost.setPageColor(mPageColor);
                }
                if (null != mAdView) {
                    ViewParent viewParent = mAdView.getParent();
                    if (viewParent instanceof PercentFlexboxLayout) {
                        ((PercentFlexboxLayout) viewParent).setBackgroundColor(mPageColor);
                    }
                }
                return true;
            case READER_VIEW_MOVE_MODE:
                String readerPagemode = Attributes.getString(attribute, READER_PAGE_MODE_HORIZON);
                if (null != mHost) {
                    boolean isSuccess = mHost.refreshReaderMoveMode(readerPagemode);
                    if (isSuccess) {
                        mPageMoveMode = readerPagemode;
                    } else {
                        Log.w(TAG, " mPageMoveMode no change, is same mode : " + readerPagemode);
                    }
                }
                return true;
        }
        return super.setAttribute(key, attribute);
    }

    protected String getDefaultColor() {
        return DEFAULT_COLOR;
    }

    @Override
    public void invokeMethod(String methodName, Map<String, Object> args) {
        if (null == mHost) {
            Log.w(TAG, "invokeMethod mHost is null.");
            return;
        }
        //页面切换参数  方向  动画类型
        if (METHOD_NEXT_PAGE.equals(methodName) && ReaderDiv.READER_PAGE_MODE_HORIZON.equals(mPageMoveMode)) {
            if (mHost.isCanMove(ReaderPageView.PageCallback.MOVE_LEFT_TYPE, false, 0)) {
                mHost.switchNextPage(true, true);
            } else {
                Log.w(TAG, READER_LOG_TAG + " invokeMethod isCanMove forward false.");
            }

        } else if (METHOD_PRE_PAGE.equals(methodName) && ReaderDiv.READER_PAGE_MODE_HORIZON.equals(mPageMoveMode)) {
            if (mHost.isCanMove(ReaderPageView.PageCallback.MOVE_RIGHT_TYPE, false, 0)) {
                mHost.switchNextPage(true, false);
            } else {
                Log.w(TAG, READER_LOG_TAG + " invokeMethod isCanMove backword false.");
            }
        } else if (METHOD_PRELOAD_PAGE.equals(methodName)) {
            preLoadAdPage(args);
        } else if (METHOD_SET_CONTENT.equals(methodName)) {
            setPageContent(args);
        } else if (METHOD_PRELOAD_CONTENT.equals(methodName)) {
            if (READER_PAGE_MODE_HORIZON.equals(mPageMoveMode)) {
                preLoadContent(args);
            } else {
                Log.w(TAG, READER_LOG_TAG + " invokeMethod preLoadContent not surpport mPageMoveMode : " + mPageMoveMode);
            }
        } else if (METHOD_SET_PAGE_COLOR.equals(methodName)) {
            setPageColor(args);
        } else if (METHOD_GET_PAGE_CONTENT.equals(methodName)) {
            getPageContent(args);
        } else if (METHOD_SET_PAGE_LINE_SPACE.equals(methodName)) {
            setLineSpace(args);
        } else {
            super.invokeMethod(methodName, args);
        }
    }

    public void setLineSpace(final Map<String, Object> args) {
        if (mHost == null || null == args) {
            Log.w(TAG, "setLineSpace mHost or args is null.");
            return;
        }
        String callbackId = (String) args.get("fail");
        HashMap<String, Object> params = new HashMap<>();
        String lineSpaceType = READERDIV_LINE_SPACE_NORMAL;
        try {
            if (args.get(READERDIV_LINE_SPACE_TYPE) != null) {
                lineSpaceType = (String) args.get(READERDIV_LINE_SPACE_TYPE);
            }
        } catch (Exception e) {
            if (null != callbackId && null != mCallback) {
                params.put(READERDIV_CALLBACK_MESSAGE, "args is not valid. error.");
                mCallback.onJsMethodCallback(getPageId(), callbackId, params);
            }
            Log.w(TAG, "setLineSpace args is not valid. error : " + e.getMessage());
            return;
        }
        if (null != mHost) {
            boolean isSuccess = mHost.setLineHeight(true, lineSpaceType);
            if (isSuccess) {
                if (READER_PAGE_MODE_VERTICAL.equals(mPageMoveMode)) {
                    mHost.setIsNeedVerLayout(true);
                }
                mHost.reCalculatePage(null, true);
                String successcallbackId = (String) args.get("success");
                params.put(READERDIV_CALLBACK_MESSAGE, "setLineSpace success.");
                if (null != successcallbackId && null != mCallback) {
                    mCallback.onJsMethodCallback(getPageId(), successcallbackId, params);
                }
            }
        }
        callbackComplete(args);
    }

    public void getPageContent(final Map<String, Object> args) {
        if (mHost == null || null == args) {
            Log.w(TAG, "getPageContent mHost or args is null.");
            return;
        }
        String callbackId = (String) args.get("fail");
        HashMap<String, Object> params = new HashMap<>();
        int startLine = -1;
        int endLine = -1;
        int pageIndex = -1;
        try {
            if (args.get(READERDIV_PAGE_INDEX) != null) {
                pageIndex = (Integer) args.get(READERDIV_PAGE_INDEX);
            }
            if (args.get(READERDIV_PAGE_START) != null) {
                startLine = (Integer) args.get(READERDIV_PAGE_START);
            }
            if (args.get(READERDIV_PAGE_END) != null) {
                endLine = (Integer) args.get(READERDIV_PAGE_END);
            }
        } catch (Exception e) {
            if (null != callbackId && null != mCallback) {
                params.put(READERDIV_CALLBACK_MESSAGE, "args is not valid. error.");
                mCallback.onJsMethodCallback(getPageId(), callbackId, params);
            }
            Log.w(TAG, "getPageContent args is not valid. error : " + e.getMessage());
            return;
        }
        if (null != mHost) {
            ReaderLayoutView.ReaderPageCallback readerPageCallback = new ReaderLayoutView.ReaderPageCallback() {
                @Override
                public void onPageContent(List<String> content) {
                    List<String> pageContents = content;
                    String callbackId = (String) args.get("success");
                    params.put(READERDIV_CALLBACK_MESSAGE, "pageContents success.");
                    params.put(READERDIV_PAGE_CONTENT, null != pageContents ? pageContents : "");
                    if (null != callbackId && null != mCallback) {
                        mCallback.onJsMethodCallback(getPageId(), callbackId, params);
                    }
                    callbackComplete(args);
                }
            };
            if (startLine == -1 && endLine == -1) {
                mHost.getPageContent(true, pageIndex, 0, 0, readerPageCallback);
            } else {
                mHost.getPageContent(false, pageIndex, startLine, endLine, readerPageCallback);
            }
        }
    }

    public void setPageColor(final Map<String, Object> args) {
        if (mHost == null || null == args) {
            Log.w(TAG, "setPageColor mHost or args is null.");
            return;
        }
        String callbackId = (String) args.get("fail");
        HashMap<String, Object> params = new HashMap<>();
        String pageColor = "";
        int startLine = -1;
        int endLine = -1;
        try {
            if (args.get(READERDIV_PAGE_COLOR) != null) {
                pageColor = (String) args.get(READERDIV_PAGE_COLOR);
            }
            if (args.get(READERDIV_PAGE_START) != null) {
                startLine = (Integer) args.get(READERDIV_PAGE_START);
            }
            if (args.get(READERDIV_PAGE_END) != null) {
                endLine = (Integer) args.get(READERDIV_PAGE_END);
            }
        } catch (Exception e) {
            if (null != callbackId && null != mCallback) {
                params.put(READERDIV_CALLBACK_MESSAGE, "args is not valid.");
                mCallback.onJsMethodCallback(getPageId(), callbackId, params);
            }
            Log.w(TAG, "setPageColor args is not valid. error : " + e.getMessage());
            return;
        }
        if (TextUtils.isEmpty(pageColor)) {
            if (null != callbackId && null != mCallback) {
                params.put(READERDIV_CALLBACK_MESSAGE, "pageColor is empty.");
                mCallback.onJsMethodCallback(getPageId(), callbackId, params);
            }
            return;
        }
        if (null != mHost) {
            int pageColorValue = ColorUtil.getColor(pageColor, Color.WHITE);
            ReaderLayoutView.ReaderPageColorCallback readerPageColorCallback = new ReaderLayoutView.ReaderPageColorCallback() {
                @Override
                public void onPageColor(boolean isSuccess) {
                    if (isSuccess) {
                        String callbackId = (String) args.get("success");
                        params.put(READERDIV_CALLBACK_MESSAGE, "pageColor success.");
                        if (null != callbackId && null != mCallback) {
                            mCallback.onJsMethodCallback(getPageId(), callbackId, params);
                        }
                    } else {
                        if (null != callbackId && null != mCallback) {
                            params.put(READERDIV_CALLBACK_MESSAGE, "args startLine  endLine or pageColor is not valid.");
                            mCallback.onJsMethodCallback(getPageId(), callbackId, params);
                        }
                    }
                    callbackComplete(args);
                }
            };
            if (startLine == -1 && endLine == -1) {
                mPageColor = pageColorValue;
                mHost.setPageColor(true, pageColorValue, 0, 0, readerPageColorCallback);
            } else {
                mHost.setPageColor(false, pageColorValue, startLine, endLine, readerPageColorCallback);
            }
        }
    }

    public String getPageContent() {
        return mPageContent;
    }

    public void setPageContent(final Map<String, Object> args) {
        if (mHost == null || null == args) {
            Log.w(TAG, "setPageContent mHost or args is null.");
            return;
        }
        String pageContent = "";
        String pageTitle = "";
        boolean isForwardPage = true;
        if (args.get(READERDIV_PAGE_CONTENT) != null) {
            pageContent = (String) args.get(READERDIV_PAGE_CONTENT);
        }
        if (args.get(READERDIV_PAGE_TITLE) != null) {
            pageTitle = (String) args.get(READERDIV_PAGE_TITLE);
        }
        if (args.get(READERDIV_PAGE_LOAD_TYPE) != null) {
            isForwardPage = (boolean) args.get(READERDIV_PAGE_LOAD_TYPE);
        }
        String callbackId = (String) args.get("fail");
        HashMap<String, Object> params = new HashMap<>();
        if (TextUtils.isEmpty(pageContent) || TextUtils.isEmpty(pageTitle)) {
            if (null != callbackId && null != mCallback) {
                params.put(READERDIV_CALLBACK_MESSAGE, "content or title is empty.");
                mCallback.onJsMethodCallback(getPageId(), callbackId, params);
            }
            return;
        }
        mPageTitle = pageTitle;
        mPageContent = pageContent;
        if (null != mHost) {
            mHost.initPageDatas(pageTitle, pageContent, new ReaderLayoutView.ReaderPageCallback() {
                @Override
                public void onPageContent(List<String> content) {
                    String callbackSuccessId = (String) args.get("success");
                    params.put(READERDIV_CALLBACK_MESSAGE, "content success.");
                    params.put(READERDIV_PAGE_LINECONTENT, null != content ? content : "");
                    if (null != callbackSuccessId) {
                        mCallback.onJsMethodCallback(getPageId(), callbackSuccessId, params);
                    }
                    callbackComplete(args);
                }
            }, isForwardPage);
        }
    }

    public void preLoadContent(final Map<String, Object> args) {
        if (mHost == null || null == args) {
            Log.w(TAG, "preLoadContent mHost or args is null.");
            return;
        }
        String pageContent = "";
        String pageTitle = "";
        boolean isForward = true;
        if (args.get(READERDIV_PAGE_CONTENT) != null) {
            pageContent = (String) args.get(READERDIV_PAGE_CONTENT);
        }
        if (args.get(READERDIV_PAGE_TITLE) != null) {
            pageTitle = (String) args.get(READERDIV_PAGE_TITLE);
        }
        if (args.get(READERDIV_PAGE_LOAD_TYPE) != null) {
            isForward = (boolean) args.get(READERDIV_PAGE_LOAD_TYPE);
        }

        String callbackId = (String) args.get("fail");
        HashMap<String, Object> params = new HashMap<>();
        if (TextUtils.isEmpty(pageContent) || TextUtils.isEmpty(pageTitle)) {
            if (null != callbackId && null != mCallback) {
                params.put(READERDIV_CALLBACK_MESSAGE, "content or title is empty.");
                mCallback.onJsMethodCallback(getPageId(), callbackId, params);
            }
            return;
        }
        if (null != mHost) {
            mHost.preSetChapter(isForward, pageTitle, pageContent, new ReaderLayoutView.ReaderPageCallback() {
                @Override
                public void onPageContent(List<String> content) {
                    String callbackSuccessId = (String) args.get("success");
                    params.put(READERDIV_CALLBACK_MESSAGE, "content success.");
                    if (null != callbackSuccessId) {
                        mCallback.onJsMethodCallback(getPageId(), callbackSuccessId, params);
                    }
                    callbackComplete(args);
                }
            });
        }
    }

    public void preLoadAdPage(final Map<String, Object> args) {
        if (mHost == null || null == args) {
            Log.w(TAG, "preLoadAdPage mHost or args is null.");
            return;
        }
        if (mHost.isNoLoadAd()) {
            Log.w(TAG, READER_LOG_TAG + " preLoadAdPage isNoLoadAd true");
            return;
        }
        mReaderMode = READER_VIEW_MODE_AD;
        boolean isForward = true;
        if (args.get(READERDIV_PRELOAD_FORWARD) != null) {
            isForward = (Boolean) args.get(READERDIV_PRELOAD_FORWARD);
        }
        if (args.get(READERDIV_PRELOAD_READERMODE) != null) {
            mReaderMode = (String) args.get(READERDIV_PRELOAD_READERMODE);
        }
        String callbackId = (String) args.get("fail");
        HashMap<String, Object> params = new HashMap<>();
        if (!READER_VIEW_MODE_AD.equals(mReaderMode)) {
            if (null != callbackId && null != mCallback) {
                params.put(READERDIV_CALLBACK_MESSAGE, "readermode error not ad, readermode : " + mReaderMode);
                mCallback.onJsMethodCallback(getPageId(), callbackId, params);
            }
            return;
        }
        if (null != mHost) {
            boolean isTextMode = true;
            if (null != mHost.getCurReaderPageView()) {
                isTextMode = mHost.getCurReaderPageView().mIsTextMode;
            }
            //当前是广告页，预加载下一页不能够是广告业    加载过程是  加载当前页 ----   预加载下一页(移除当前页的广告)
            if (!isTextMode && READER_VIEW_MODE_AD.equals(mReaderMode)) {
                Log.w(TAG, READER_LOG_TAG + "readermode is not valid current  ad , readermode : " + mReaderMode);
                callbackId = (String) args.get("fail");
                if (null != callbackId && null != mCallback) {
                    params.put(READERDIV_CALLBACK_MESSAGE, "readermode error current page is already ad , readermode : " + mReaderMode);
                    mCallback.onJsMethodCallback(getPageId(), callbackId, params);
                }
            } else {
                ReaderPageView readerPageView = null;
                int pageIndex = -1;
                int originIndex = -1;
                if (mHost.isReverseRead()) {
                    if (!isForward) {
                        readerPageView = mHost.getNextPageView();
                        originIndex = readerPageView.getPageIndex();
                        pageIndex = readerPageView.getPageIndex() < 0 ? -1 : readerPageView.getPageIndex();
                        readerPageView.setPageIndex(pageIndex);
                    } else {
                        readerPageView = mHost.getPrePageView();
                        originIndex = readerPageView.getPageIndex();
                        boolean isText = readerPageView.isTextMode();
                        pageIndex = readerPageView.getPageIndex() > 0 ? (readerPageView.getPageIndex() - 1) : -1;
                        readerPageView.setPageIndex(pageIndex);
                    }
                } else {
                    if (isForward) {
                        readerPageView = mHost.getNextPageView();
                        originIndex = readerPageView.getPageIndex();
                        pageIndex = readerPageView.getPageIndex() > 0 ? (readerPageView.getPageIndex() - 1) : -1;
                        readerPageView.setPageIndex(pageIndex);
                    } else {
                        readerPageView = mHost.getPrePageView();
                        originIndex = readerPageView.getPageIndex();
                        boolean isText = readerPageView.isTextMode();
                        pageIndex = readerPageView.getPageIndex() < 0 ? -1 : readerPageView.getPageIndex();
                        readerPageView.setPageIndex(pageIndex);
                    }
                }
                if (readerPageView.getPageIndex() == -1) {
                    Log.w(TAG, READER_LOG_TAG + "pageindexlog preLoadAdPage fail pageIndex : " + pageIndex
                            + " isForward : " + isForward
                            + " originIndex : " + originIndex);
                    callbackId = (String) args.get("fail");
                    params.put(READERDIV_CALLBACK_MESSAGE, "readermode fail.");
                } else {
                    Log.w(TAG, READER_LOG_TAG + "pageindexlog preLoadAdPage pageIndex : " + pageIndex
                            + " isForward : " + isForward
                            + " originIndex : " + originIndex);
                    refreshPage(true, null, readerPageView, mReaderMode);
                    callbackId = (String) args.get("success");
                    params.put(READERDIV_CALLBACK_MESSAGE, "readermode success.");
                }
            }

        }
        if (null != callbackId) {
            mCallback.onJsMethodCallback(getPageId(), callbackId, params);
        }
        callbackComplete(args);
    }

    private void callbackComplete(Map<String, Object> args) {
        if (args != null && args.containsKey("complete") && null != mCallback) {
            mCallback.onJsMethodCallback(getPageId(), (String) args.get("complete"));
        }
    }

    private String getDefaultFontSize() {
        return DEFAULT_FONT_SIZE * mScaleValue + Attributes.Unit.PX;
    }

    public boolean setFontSize(int fontSize) {
        boolean isValid = false;
        if (fontSize <= 0) {
            return isValid;
        }
        if (null != mHost) {
            isValid = true;
            mHost.setFontSize(fontSize);
        }
        return isValid;
    }

    public void setFontColor(String fontColor) {
        if (null != mHost) {
            int tmpFontColor = ColorUtil.getColor(fontColor, ColorUtil.getColor(DEFAULT_COLOR));
            mHost.setFontColor(tmpFontColor);
        } else {
            Log.w(TAG, "setFontColor mHost is null. fontColor : " + fontColor);
        }
    }

    /**
     * 预加载和翻页
     * <p>
     * 预加载  加载下一页的广告
     * <p>
     * 翻页是翻到当前页
     * <p>
     * 翻页情况释放前一页的ad-view，当前页不用做remove或者addview
     * 预加载是加载下一页的ad-view
     * <p>
     * <p>
     * isprePage 释放是预加载
     *
     * @param view
     * @param readerMode
     */
    public void refreshPage(boolean isprePage, ViewGroup prePageView, ViewGroup view, String readerMode) {
        ViewGroup readerPageView = view;
        if (readerPageView instanceof ReaderPageView) {
            if (isprePage) {
                if (READER_VIEW_MODE_AD.equals(readerMode)) {
                    if (null != mAdView) {
                        ViewParent viewParent = mAdView.getParent();
                        if (viewParent instanceof ViewGroup) {
                            ((ViewGroup) viewParent).removeView(mAdView);
                        }
                        ((ReaderPageView) readerPageView).setTextMode(false);
                        readerPageView.removeAllViews();
                        PercentFlexboxLayout flexboxLayout = new PercentFlexboxLayout(readerPageView.getContext());
                        flexboxLayout.setComponent(this);
                        flexboxLayout.getYogaNode().setFlexDirection(mFlexDirection);
                        flexboxLayout.getYogaNode().setJustifyContent(mJustifyContent);
                        flexboxLayout.getYogaNode().setAlignItems(mAlignItems);
                        flexboxLayout.getYogaNode().setWidthPercent(100);
                        flexboxLayout.getYogaNode().setHeightPercent(100);
                        flexboxLayout.setBackgroundColor(mPageColor);
                        flexboxLayout.getYogaNode().setMargin(YogaEdge.ALL, SHADER_WIDTH);
                        ViewGroup.LayoutParams frameLp = new ViewGroup.LayoutParams(
                                FrameLayout.LayoutParams.MATCH_PARENT,
                                FrameLayout.LayoutParams.MATCH_PARENT);
                        readerPageView.addView(flexboxLayout, frameLp);
                        ViewGroup.LayoutParams params = new YogaLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT);
                        flexboxLayout.addView(mAdView, params);
                        if (mAdView instanceof ComponentHost) {
                            ((ComponentHost) mAdView).getComponent().onHostViewAttached(readerPageView);
                        }

                    } else {
                        Log.w(TAG, READER_LOG_TAG + "refreshNextPage mAdView is null.");
                    }
                } else {
                    Log.d(TAG, READER_LOG_TAG + "isprePage  true  refreshNextPage next text ");
                    ((ReaderPageView) readerPageView).setTextMode(true);
                }
            } else {
                Log.w(TAG, READER_LOG_TAG + "refreshPage else.");
            }
        }

    }

    private void removeAdContainer(ViewGroup flexboxLayout, View child) {
        if (flexboxLayout instanceof ReaderPageView) {
            for (int i = 0; i < flexboxLayout.getChildCount(); i++) {
                ViewGroup childContainer = (ViewGroup) flexboxLayout.getChildAt(i);
                if (childContainer.getChildCount() > 0 &&
                        childContainer.getChildAt(0).equals(child)) {
                    childContainer.removeView(child);
                    flexboxLayout.removeView(childContainer);
                    break;
                }
            }
        }
    }

    @Override
    protected boolean addEvent(String event) {
        if (TextUtils.isEmpty(event) || mHost == null) {
            return true;
        }
        if (READER_PAGE_CHANGE.equals(event)) {
            mHost.setReaderCallback(new ReaderLayoutView.ReaderCallback() {
                @Override
                public void onPageChange(boolean isForward, int preIndex, int curIndex, int readerIndex, int totalPages, int recycleIndex, boolean isPreAd) {
                    Log.d(TAG, "ReaderCallback  readerIndex: " + readerIndex);
                    HashMap<String, Object> datas = new HashMap<>();
                    datas.put("forward", isForward);
                    datas.put("preIndex", preIndex);
                    datas.put("curIndex", curIndex);
                    datas.put("readerIndex", readerIndex);
                    datas.put("totalPages", totalPages);
                    datas.put("recycleIndex", recycleIndex);
                    datas.put("isPreAd", isPreAd);
                    datas.put("isReverse", null != mHost ? mHost.isReverseRead() : false);
                    mCallback.onJsEventCallback(getPageId(), mRef, READER_PAGE_CHANGE, ReaderDiv.this, datas, null);
                }

                @Override
                public void refreshPageView(ViewGroup prePageView, ViewGroup pageView, String readerMode) {
                    refreshPage(false, prePageView, pageView, readerMode);
                }
            });
            return true;
        } else if (READER_CHAPTER_END.equals(event)) {
            mHost.setReaderEndCallback(new ReaderLayoutView.ReaderChapterEndCallback() {
                @Override
                public void onChapterEnd(String readermode) {
                    if (mChapterEndTime != -1 && ((System.currentTimeMillis() - mChapterEndTime) / 500) < 1) {
                        Log.w(TAG, READER_LOG_TAG + "onChapterEnd  time less than 0.5s.");
                        return;
                    }
                    mChapterEndTime = System.currentTimeMillis();
                    HashMap<String, Object> datas = new HashMap<>();
                    datas.put(READER_VIEW_MOVE_MODE, readermode);
                    mCallback.onJsEventCallback(getPageId(), mRef, READER_CHAPTER_END, ReaderDiv.this, datas, null);
                }
            });
            return true;
        } else if (READER_CHAPTER_START.equals(event)) {
            mHost.setReaderStartCallback(new ReaderLayoutView.ReaderChapterStartCallback() {
                @Override
                public void onChapterStart(String readerMode) {
                    if (mChapterStartTime != -1 && ((System.currentTimeMillis() - mChapterStartTime) / 500) < 1) {
                        Log.w(TAG, READER_LOG_TAG + "onChapterStart  time less than 0.5s.");
                        return;
                    }
                    mChapterStartTime = System.currentTimeMillis();
                    HashMap<String, Object> datas = new HashMap<>();
                    datas.put(READER_VIEW_MOVE_MODE, readerMode);
                    mCallback.onJsEventCallback(getPageId(), mRef, READER_CHAPTER_START, ReaderDiv.this, datas, null);
                }
            });
            return true;
        }

        return super.addEvent(event);
    }

    @Override
    protected boolean removeEvent(String event) {
        if (TextUtils.isEmpty(event) || mHost == null) {
            return true;
        }
        if (READER_PAGE_CHANGE.equals(event)) {
            mHost.setReaderCallback(null);
            return true;
        } else if (READER_CHAPTER_END.equals(event)) {
            mHost.setReaderEndCallback(null);
            return true;
        } else if (READER_CHAPTER_START.equals(event)) {
            mHost.setReaderStartCallback(null);
            return true;
        }
        return super.removeEvent(event);
    }


    @Override
    public void removeView(View child) {
        ViewGroup flexboxLayout = getInnerView();
        if (flexboxLayout instanceof ReaderPageView) {
            for (int i = 0; i < flexboxLayout.getChildCount(); i++) {
                ViewGroup childContainer = (ViewGroup) flexboxLayout.getChildAt(i);
                if (childContainer.getChildCount() > 0 &&
                        childContainer.getChildAt(0).equals(child)) {
                    childContainer.removeView(child);
                    flexboxLayout.removeView(childContainer);
                    break;
                }
            }
        }
    }

    @Override
    public View getChildViewAt(int index) {
        ViewGroup flexboxLayout = getInnerView();
        if (flexboxLayout instanceof ReaderPageView) {
            if (index < 0 || index >= flexboxLayout.getChildCount()) {
                return null;
            }

            ViewGroup childContainer = (ViewGroup) flexboxLayout.getChildAt(index);
            if (childContainer != null && childContainer.getChildCount() == 1) {
                return childContainer.getChildAt(0);
            }
        }
        return null;
    }

    @Override
    public void setFlexDirection(String flexDirectionStr) {
        if (TextUtils.isEmpty(flexDirectionStr)) {
            return;
        }
        YogaFlexDirection flexDirection = YogaFlexDirection.ROW;
        if ("column".equals(flexDirectionStr)) {
            flexDirection = YogaFlexDirection.COLUMN;
        } else if ("row-reverse".equals(flexDirectionStr)) {
            flexDirection = YogaFlexDirection.ROW_REVERSE;
        } else if ("column-reverse".equals(flexDirectionStr)) {
            flexDirection = YogaFlexDirection.COLUMN_REVERSE;
        }
        mFlexDirection = flexDirection;
        ViewGroup flexboxLayout = getInnerView();
        if (flexboxLayout instanceof ReaderPageView) {
            for (int i = 0; i < flexboxLayout.getChildCount(); i++) {
                YogaLayout childContainer = (YogaLayout) flexboxLayout.getChildAt(i);
                childContainer.getYogaNode().setFlexDirection(flexDirection);
                if (childContainer.getYogaNode().isDirty()) {
                    childContainer.requestLayout();
                }
            }
        }
    }

    @Override
    public void setJustifyContent(String justifyContentStr) {
        if (TextUtils.isEmpty(justifyContentStr)) {
            return;
        }

        YogaJustify justifyContent = YogaJustify.FLEX_START;
        if ("flex-end".equals(justifyContentStr)) {
            justifyContent = YogaJustify.FLEX_END;
        } else if ("center".equals(justifyContentStr)) {
            justifyContent = YogaJustify.CENTER;
        } else if ("space-between".equals(justifyContentStr)) {
            justifyContent = YogaJustify.SPACE_BETWEEN;
        } else if ("space-around".equals(justifyContentStr)) {
            justifyContent = YogaJustify.SPACE_AROUND;
        }
        mJustifyContent = justifyContent;

        ViewGroup flexboxLayout = getInnerView();
        if (flexboxLayout instanceof ReaderPageView) {
            for (int i = 0; i < flexboxLayout.getChildCount(); i++) {
                YogaLayout childContainer = (YogaLayout) flexboxLayout.getChildAt(i);
                childContainer.getYogaNode().setJustifyContent(justifyContent);
                if (childContainer.getYogaNode().isDirty()) {
                    childContainer.requestLayout();
                }
            }
        }
    }

    @Override
    public void setAlignItems(String alignItemsStr) {
        if (TextUtils.isEmpty(alignItemsStr)) {
            return;
        }

        YogaAlign alignItems = YogaAlign.STRETCH;
        if ("flex-start".equals(alignItemsStr)) {
            alignItems = YogaAlign.FLEX_START;
        } else if ("flex-end".equals(alignItemsStr)) {
            alignItems = YogaAlign.FLEX_END;
        } else if ("center".equals(alignItemsStr)) {
            alignItems = YogaAlign.CENTER;
        }
        mAlignItems = alignItems;

        ViewGroup flexboxLayout = getInnerView();
        if (flexboxLayout instanceof ReaderPageView) {
            for (int i = 0; i < flexboxLayout.getChildCount(); i++) {
                YogaLayout flexboxContainer = (YogaLayout) flexboxLayout.getChildAt(i);
                flexboxContainer.getYogaNode().setAlignItems(alignItems);
                if (flexboxContainer.getYogaNode().isDirty()) {
                    flexboxContainer.requestLayout();
                }
            }
        }
    }

    @Override
    public ViewGroup getInnerView() {
        ReaderPageView readerPageView = null;
        if (null != mHost) {
            readerPageView = mHost.getCurReaderPageView();
        }
        return readerPageView;
    }

}
