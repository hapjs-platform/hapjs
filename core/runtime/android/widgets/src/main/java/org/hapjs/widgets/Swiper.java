/*
 * Copyright (c) 2021-present, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.widgets;

import android.content.Context;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewTreeObserver;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.hapjs.bridge.annotation.WidgetAnnotation;
import org.hapjs.common.utils.ColorUtil;
import org.hapjs.common.utils.FloatUtil;
import org.hapjs.common.utils.IntegerUtil;
import org.hapjs.component.AbstractScrollable;
import org.hapjs.component.Component;
import org.hapjs.component.Container;
import org.hapjs.component.HScrollable;
import org.hapjs.component.Recycler;
import org.hapjs.component.RecyclerDataItem;
import org.hapjs.component.animation.Origin;
import org.hapjs.component.bridge.RenderEventCallback;
import org.hapjs.component.constants.Attributes;
import org.hapjs.render.css.value.CSSValues;
import org.hapjs.runtime.HapEngine;
import org.hapjs.widgets.view.swiper.LoopPagerAdapter;
import org.hapjs.widgets.view.swiper.LoopViewPager;
import org.hapjs.widgets.view.swiper.PageAnimationParser;
import org.hapjs.widgets.view.swiper.SwiperAnimation;
import org.hapjs.widgets.view.swiper.SwiperAnimationParser;
import org.hapjs.widgets.view.swiper.SwiperView;
import org.hapjs.widgets.view.swiper.ViewPager;

@WidgetAnnotation(
        name = Swiper.WIDGET_NAME,
        methods = {
                Swiper.METHOD_SWIPE_TO,
                Component.METHOD_ANIMATE,
                Component.METHOD_GET_BOUNDING_CLIENT_RECT,
                Component.METHOD_TO_TEMP_FILE_PATH,
                Component.METHOD_FOCUS
        })
public class Swiper extends AbstractScrollable<SwiperView>
        implements Recycler, HScrollable, ViewTreeObserver.OnGlobalLayoutListener {

    public static final String DEFAULT_INDICATOR_COLOR = "#7f000000";
    public static final String DEFAULT_INDICATOR_SELECTED_COLOR = "#ff33b4ff";
    public static final String DEFAULT_INDICATOR_SIZE = "20px";

    protected static final String WIDGET_NAME = "swiper";

    // method
    protected static final String METHOD_SWIPE_TO = "swipeTo";

    // attribute
    private static final String INDEX = "index";
    private static final String INTERVAL = "interval";
    private static final String AUTO_PLAY = "autoplay";
    private static final String INDICATOR_ENABLED = "indicator";
    private static final String LOOP = "loop";
    private static final String DURATION = "duration";
    private static final String VERTICAL = "vertical";
    private static final String PREVIOUS_MARGIN = "previousmargin";
    private static final String NEXT_MARGIN = "nextmargin";
    private static final String PAGE_MARGIN = "pagemargin";

    // style
    private static final String INDICATOR_COLOR = "indicatorColor";
    private static final String INDICATOR_SELECTED_COLOR = "indicatorSelectedColor";
    private static final String INDICATOR_SIZE = "indicatorSize";
    private static final String INDICATOR_TOP = "indicatorTop";
    private static final String INDICATOR_LEFT = "indicatorLeft";
    private static final String INDICATOR_RIGHT = "indicatorRight";
    private static final String INDICATOR_BOTTOM = "indicatorBottom";
    private static final String ENABLE_SWIPE = "enableswipe";
    private static final String WIDTH_FRACTION_START = "widthFractionStart";
    private static final String WIDTH_FRACTION_END = "widthFractionEnd";
    private static final String HEIGHT_FRACTION_START = "heightFractionStart";
    private static final String HEIGHT_FRACTION_END = "heightFractionEnd";
    private static final String HEIGHT_RATIO_START = "heightRatioStart";
    private static final String HEIGHT_RATIO_END = "heightRatioEnd";
    private static final String WIDTH_RATIO_START = "widthRatioStart";
    private static final String WIDTH_RATIO_END = "widthRatioEnd";
    private static final float ZERO_OFFSET = 0f;
    private SwiperAnimationParser mSwiperAnimationParser;
    private Map<String, Float> mRatioAndFractionMap;
    private SwiperAnimation mSwiperAnimation;
    private LoopViewPager mViewPager;
    private LoopPagerAdapter mAdapter;
    private LoopViewPager.OnPageChangeCallback mPageChangeListener;
    private int mPageIndex = -1;
    private int mLastIndex = -1;
    private boolean mIndicatorEnabled = true;
    private boolean mSwiperAnimationChanged = false;
    private boolean mHasSetImageBlur = false;

    private List<CachedComponent> mCachedComponentList = new ArrayList<>();

    private Map<String, Object> mCachedAttributes = new LinkedHashMap<>();
    private boolean isVertical;

    private RecyclerItem mRecyclerItem;

    public Swiper(
            HapEngine hapEngine,
            Context context,
            Container parent,
            int ref,
            RenderEventCallback callback,
            Map<String, Object> savedState) {
        super(hapEngine, context, parent, ref, callback, savedState);
        mSwiperAnimation = new SwiperAnimation();
        mSwiperAnimationParser = new SwiperAnimationParser();
        mRatioAndFractionMap = new HashMap<>();
    }

    public static String getWidthFractionStart() {
        return WIDTH_FRACTION_START;
    }

    public static String getWidthFractionEnd() {
        return WIDTH_FRACTION_END;
    }

    public static String getHeightFractionStart() {
        return HEIGHT_FRACTION_START;
    }

    public static String getHeightFractionEnd() {
        return HEIGHT_FRACTION_END;
    }

    public static String getHeightRatioStart() {
        return HEIGHT_RATIO_START;
    }

    public static String getHeightRatioEnd() {
        return HEIGHT_RATIO_END;
    }

    public static String getWidthRatioStart() {
        return WIDTH_RATIO_START;
    }

    public static String getWidthRatioEnd() {
        return WIDTH_RATIO_END;
    }

    @Override
    protected SwiperView createViewImpl() {
        SwiperView swiperView = new SwiperView(mHapEngine, mContext);
        swiperView.setComponent(this);
        mViewPager = swiperView.getViewPager();
        mViewPager.addOnPageChangeListener(
                new ViewPager.SimpleOnPageChangeListener() {
                    @Override
                    public void onPageScrolled(int position, float positionOffset,
                                               int positionOffsetPixels) {
                        if (FloatUtil.floatsEqual(positionOffset, ZERO_OFFSET)
                                && mPageIndex != position) {
                            mPageIndex = position;
                            processAppearanceEvent();
                        }
                    }
                });
        mViewPager.addOnAttachStateChangeListener(
                new View.OnAttachStateChangeListener() {
                    @Override
                    public void onViewAttachedToWindow(View v) {
                        mCallback.addActivityStateListener(Swiper.this);
                        if (mViewPager != null && mViewPager.isAutoScroll()) {
                            mViewPager.startAutoScroll();
                        }
                        if (getRecyclerItem() != null
                                && mAdapter.getContainerDataItem() == null
                                && mHost != null) {
                            mHost.setData(getRecyclerItem());
                        }
                    }

                    @Override
                    public void onViewDetachedFromWindow(View v) {
                        mCallback.removeActivityStateListener(Swiper.this);
                        if (mViewPager != null && mViewPager.isAutoScroll()) {
                            mViewPager.stopAutoScroll();
                        }
                        if (mHost != null) {
                            mHost.setData(null);
                        }
                    }
                });
        mAdapter = swiperView.getAdapter();

        if (getRecyclerItem() != null) {
            swiperView.setData(getRecyclerItem());
            setIndicatorEnabled(mIndicatorEnabled);
        }

        // loop default is true
        swiperView.setLoop(true);

        swiperView.getViewTreeObserver().addOnGlobalLayoutListener(this);
        return swiperView;
    }

    @Override
    public void applyStyles(Map<String, ? extends CSSValues> attrs) {
        super.applyStyles(attrs);
        mHost.updateIndicator();
    }

    @Override
    protected boolean setAttribute(String key, Object attribute) {
        switch (key) {
            case INDEX:
                int index = Attributes.getInt(mHapEngine, attribute, 0);
                setCurrentItem(index);
                return true;
            case INTERVAL:
                long interval = Attributes.getLong(attribute, 3000L);
                setInterval(interval);
                return true;
            case AUTO_PLAY:
                String autoScrollStr = Attributes.getString(attribute, "false");
                setAutoScroll(autoScrollStr);
                return true;
            case INDICATOR_ENABLED:
                boolean indicatorEnabled = Attributes.getBoolean(attribute, true);
                setIndicatorEnabled(indicatorEnabled);
                return true;
            case INDICATOR_SIZE:
                float indicatorSize =
                        Attributes.getFloat(
                                mHapEngine, attribute,
                                Attributes.getFloat(mHapEngine, DEFAULT_INDICATOR_SIZE));
                setIndicatorSize(indicatorSize);
                return true;
            case INDICATOR_COLOR:
                String colorStr = Attributes.getString(attribute, DEFAULT_INDICATOR_COLOR);
                setIndicatorColor(colorStr);
                return true;
            case INDICATOR_SELECTED_COLOR:
                String selectedColorStr =
                        Attributes.getString(attribute, DEFAULT_INDICATOR_SELECTED_COLOR);
                setIndicatorSelectedColor(selectedColorStr);
                return true;
            case LOOP:
                boolean loop = Attributes.getBoolean(attribute, true);
                setLoop(loop);
                return true;
            case DURATION:
                int duration = Attributes.getInt(mHapEngine, attribute, -1);
                setDuration(duration);
                return true;
            case VERTICAL:
                boolean vertical = Attributes.getBoolean(attribute, false);
                setVertical(vertical);
                return true;
            case PREVIOUS_MARGIN:
                String previousMargin = Attributes.getString(attribute);
                setPreviousMargin(previousMargin);
                return true;
            case NEXT_MARGIN:
                String nextMargin = Attributes.getString(attribute);
                setNextMargin(nextMargin);
                return true;
            // TODO hide pageMargin
            /*case PAGE_MARGIN:
            String pageMargin = Attributes.getString(attribute);
            setPageMargin(pageMargin);
            return true;*/
            case INDICATOR_LEFT:
                String indicatorLeft = Attributes.getString(attribute);
                setIndicatorLeft(indicatorLeft);
                return true;
            case INDICATOR_TOP:
                String indicatorTop = Attributes.getString(attribute);
                setIndicatorTop(indicatorTop);
                return true;
            case INDICATOR_RIGHT:
                String indicatorRight = Attributes.getString(attribute);
                setIndicatorRight(indicatorRight);
                return true;
            case INDICATOR_BOTTOM:
                String indicatorBottom = Attributes.getString(attribute);
                setIndicatorBottom(indicatorBottom);
                return true;
            case ENABLE_SWIPE:
                boolean enableswipe = Attributes.getBoolean(attribute, true);
                setEnableSwipe(enableswipe);
                return true;
            case Attributes.Style.ANIMATION_TIMING_FUNCTION:
                String timingFunction = Attributes.getString(attribute, "ease");
                setTimingAnimation(timingFunction);
                return true;
            case Attributes.Style.PAGE_ANIMATION_ORIGIN:
                String originStr = Attributes.getString(attribute, "0px 0px 0");
                mSwiperAnimation = setPageAnimationOrigin(originStr);
                setPageAnimation(mSwiperAnimation);
                mSwiperAnimationChanged = true;
                return true;
            case Attributes.Style.PAGE_ANIMATION_KEYFRAMES:
                String keyFrames = Attributes.getString(attribute, "");
                mSwiperAnimation =
                        mSwiperAnimationParser.parse(
                                mHapEngine, mSwiperAnimation, keyFrames, mRatioAndFractionMap,
                                this);
                setPageAnimation(mSwiperAnimation);
                mSwiperAnimationChanged = true;
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

        if (Attributes.Event.CHANGE.equals(event)) {
            if (mPageChangeListener == null) {
                mPageChangeListener =
                        new LoopViewPager.OnPageChangeCallback() {
                            @Override
                            public void onPageScrolled(
                                    int position, float positionOffset, int positionOffsetPixels) {
                            }

                            @Override
                            public void onPageSelected(int position) {
                                // 动态设置loop时，position会更改导致回调，需要过滤
                                if (position == mLastIndex) {
                                    return;
                                }
                                mLastIndex = position;
                                Map<String, Object> params = new HashMap<>();
                                params.put("index", position);
                                Map<String, Object> attrs = new HashMap<>();
                                attrs.put("index", position);
                                mCallback.onJsEventCallback(
                                        getPageId(), mRef, Attributes.Event.CHANGE, Swiper.this,
                                        params, attrs);
                            }

                            @Override
                            public void onPageScrollStateChanged(int state) {
                            }
                        };
                mViewPager.registerOnPageChangeCallback(mPageChangeListener);
            }
            return true;
        }

        return super.addEvent(event);
    }

    @Override
    protected boolean removeEvent(String event) {
        if (TextUtils.isEmpty(event) || mHost == null) {
            return true;
        }

        if (Attributes.Event.CHANGE.equals(event)) {
            if (mPageChangeListener != null) {
                mViewPager.unregisterOnPageChangeCallback(mPageChangeListener);
                mPageChangeListener = null;
            }
            return true;
        }

        return super.removeEvent(event);
    }

    @Override
    public void addChild(Component child, int index) {
        mChildren.add(child); // will only be called from LoopPagerAdapter
    }

    @Override
    public void removeChild(Component child) {
        mChildren.remove(child);
    }

    @Override
    public View getChildViewAt(int index) {
        return null;
    }

    @Override
    public void invokeMethod(String methodName, Map<String, Object> args) {
        super.invokeMethod(methodName, args);

        if (METHOD_SWIPE_TO.equals(methodName)) {
            Object object = args.get("index");
            if (object instanceof Integer) {
                int index = (int) object;
                setCurrentItem(index);
            } else {
                mCallback.onJsException(
                        new IllegalArgumentException("the param of index must be number"));
            }
        }
    }

    @Override
    public void setClipChildren(boolean clipChildren) {
        super.setClipChildren(clipChildren);
        setClipChildrenInternal(mViewPager, clipChildren);
    }

    @Override
    public void setBackgroundImage(String backgroundImage, boolean setBlur) {
        if (isComponentAdaptiveEnable() && mHasSetImageBlur) {
            return;
        }
        mHasSetImageBlur = true;
        super.setBackgroundImage(backgroundImage, setBlur);
    }

    private void setCurrentItem(int item) {
        if (mViewPager == null) {
            return;
        }
        mViewPager.setCurrentItem(item);
    }

    public void setInterval(long interval) {
        if (mViewPager == null) {
            return;
        }
        mViewPager.setInterval(interval);
    }

    public void setAutoScroll(String autoScrollStr) {
        if (TextUtils.isEmpty(autoScrollStr) || mViewPager == null) {
            return;
        }

        boolean autoScroll = false;
        if ("true".equals(autoScrollStr)) {
            autoScroll = true;
        }

        mViewPager.setAutoScroll(autoScroll);
        if (autoScroll) {
            mViewPager.startAutoScroll();
        } else {
            mViewPager.stopAutoScroll();
        }
    }

    public boolean getIndicatorEnabled() {
        return mIndicatorEnabled;
    }

    public void setIndicatorEnabled(boolean enabled) {
        mIndicatorEnabled = enabled;

        if (mHost == null) {
            return;
        }
        mHost.setIndicatorEnabled(enabled);
    }

    public void setIndicatorSize(float indicatorSize) {
        mHost.setIndicatorSize(indicatorSize);
    }

    public void setIndicatorColor(String colorStr) {
        if (TextUtils.isEmpty(colorStr) || mHost == null) {
            return;
        }

        int color = ColorUtil.getColor(colorStr);
        mHost.setIndicatorColor(color);
    }

    public void setIndicatorSelectedColor(String colorStr) {
        if (TextUtils.isEmpty(colorStr) || mHost == null) {
            return;
        }

        int color = ColorUtil.getColor(colorStr);
        mHost.setIndicatorSelectedColor(color);
    }

    public void setLoop(boolean loop) {
        if (mHost == null) {
            return;
        }

        mHost.setLoop(loop);
    }

    public void setEnableSwipe(boolean enableSwipe) {
        if (mHost == null) {
            return;
        }

        mHost.setEnableSwipe(enableSwipe);
    }

    public void setTimingAnimation(String timingFunction) {
        if (mHost == null) {
            return;
        }

        mHost.setTimingFunction(timingFunction);
    }

    public void setPageAnimation(SwiperAnimation swiperAnimation) {
        if (mHost == null || swiperAnimation == null) {
            return;
        }
        ViewPager.PageTransformer pageTransformer =
                PageAnimationParser.parse(mHost, swiperAnimation);
        mHost.setPageAnimation(pageTransformer);
    }

    @Override
    public void onActivityResume() {
        if (mViewPager != null && mViewPager.isAutoScroll()) {
            mViewPager.startAutoScroll();
        }
    }

    @Override
    public void onActivityStop() {
        if (mViewPager != null) {
            mViewPager.stopAutoScroll();
        }
    }

    @Override
    public void destroy() {
        super.destroy();
        if (mHost != null) {
            mHost.getViewTreeObserver().removeOnGlobalLayoutListener(this);
            mHost.destroy();
        }
        mChildren.clear();
        mCallback.removeActivityStateListener(this);
    }

    private void setPreviousMargin(final String previousMargin) {
        if (mHost == null) {
            return;
        }

        mCachedAttributes.put(PREVIOUS_MARGIN, previousMargin);
        int margin = parseSize(previousMargin);
        if (margin == Integer.MIN_VALUE) {
            mHost
                    .getViewTreeObserver()
                    .addOnGlobalLayoutListener(
                            new ViewTreeObserver.OnGlobalLayoutListener() {
                                @Override
                                public void onGlobalLayout() {
                                    mHost.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                                    setPreviousMargin(previousMargin);
                                }
                            });
            return;
        }

        mHost.setPreviousMargin(margin);
    }

    private void setNextMargin(final String nextMargin) {
        if (mHost == null) {
            return;
        }

        mCachedAttributes.put(NEXT_MARGIN, nextMargin);
        int margin = parseSize(nextMargin);
        if (margin == Integer.MIN_VALUE) {
            mHost
                    .getViewTreeObserver()
                    .addOnGlobalLayoutListener(
                            new ViewTreeObserver.OnGlobalLayoutListener() {
                                @Override
                                public void onGlobalLayout() {
                                    mHost.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                                    setNextMargin(nextMargin);
                                }
                            });
            return;
        }

        mHost.setNextMargin(margin);
    }

    private void setIndicatorLeft(final String indicatorLeft) {
        if (mHost == null) {
            return;
        }

        mCachedAttributes.put(INDICATOR_LEFT, indicatorLeft);
        int left = parseSize(indicatorLeft, true);
        if (left == Integer.MIN_VALUE) {
            mHost
                    .getViewTreeObserver()
                    .addOnGlobalLayoutListener(
                            new ViewTreeObserver.OnGlobalLayoutListener() {
                                @Override
                                public void onGlobalLayout() {
                                    mHost.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                                    setIndicatorLeft(indicatorLeft);
                                }
                            });
            return;
        }
        mHost.setIndicatorLeft(left);
    }

    private void setIndicatorTop(final String indicatorTop) {
        if (mHost == null) {
            return;
        }

        mCachedAttributes.put(INDICATOR_TOP, indicatorTop);
        int top = parseSize(indicatorTop, false);
        if (top == Integer.MIN_VALUE) {
            mHost
                    .getViewTreeObserver()
                    .addOnGlobalLayoutListener(
                            new ViewTreeObserver.OnGlobalLayoutListener() {
                                @Override
                                public void onGlobalLayout() {
                                    mHost.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                                    setIndicatorTop(indicatorTop);
                                }
                            });

            return;
        }
        mHost.setIndicatorTop(top);
    }

    private void setIndicatorRight(final String indicatorRight) {
        if (mHost == null) {
            return;
        }

        mCachedAttributes.put(INDICATOR_RIGHT, indicatorRight);
        int right = parseSize(indicatorRight, true);
        if (right == Integer.MIN_VALUE) {
            mHost
                    .getViewTreeObserver()
                    .addOnGlobalLayoutListener(
                            new ViewTreeObserver.OnGlobalLayoutListener() {
                                @Override
                                public void onGlobalLayout() {
                                    mHost.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                                    setIndicatorRight(indicatorRight);
                                }
                            });
            return;
        }
        mHost.setIndicatorRight(right);
    }

    private void setIndicatorBottom(final String indicatorBottom) {
        if (mHost == null) {
            return;
        }

        mCachedAttributes.put(INDICATOR_BOTTOM, indicatorBottom);
        int bottom = parseSize(indicatorBottom, false);
        if (bottom == Integer.MIN_VALUE) {
            mHost
                    .getViewTreeObserver()
                    .addOnGlobalLayoutListener(
                            new ViewTreeObserver.OnGlobalLayoutListener() {
                                @Override
                                public void onGlobalLayout() {
                                    mHost.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                                    setIndicatorBottom(indicatorBottom);
                                }
                            });
            return;
        }
        mHost.setIndicatorBottom(bottom);
    }

    private void setPageMargin(final String pageMargin) {
        if (mHost == null) {
            return;
        }

        mCachedAttributes.put(PAGE_MARGIN, pageMargin);
        int margin = parseSize(pageMargin);
        if (margin == Integer.MIN_VALUE) {
            mHost
                    .getViewTreeObserver()
                    .addOnGlobalLayoutListener(
                            new ViewTreeObserver.OnGlobalLayoutListener() {
                                @Override
                                public void onGlobalLayout() {
                                    mHost.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                                    setPageMargin(pageMargin);
                                }
                            });
            return;
        }

        mHost.setPageMargin(margin);
    }

    private int parseSize(String size) {
        if (TextUtils.isEmpty(size)) {
            return -1;
        }
        size = size.trim();
        int value;
        if (size.endsWith(Attributes.Unit.PERCENT)) {
            int componentSize;
            if (isVertical) {
                componentSize = mHost.getHeight();
            } else {
                componentSize = mHost.getWidth();
            }

            if (isInvalidSize(componentSize)) {
                value = Integer.MIN_VALUE;
            } else {
                float percent = Attributes.getPercent(size, 0);
                value = Math.round(percent * componentSize);
            }
        } else {
            value = Attributes.getInt(mHapEngine, size, IntegerUtil.UNDEFINED);
            if (value < 0 || value == IntegerUtil.UNDEFINED) {
                value = -1;
            }
        }
        return value;
    }

    private int parseSize(String size, boolean useWidth) {
        if (TextUtils.isEmpty(size)) {
            return -1;
        }
        size = size.trim();
        int value;
        if (size.endsWith(Attributes.Unit.PERCENT)) {
            int componentSize;
            if (useWidth) {
                componentSize = mHost.getWidth();
            } else {
                componentSize = mHost.getHeight();
            }

            if (isInvalidSize(componentSize)) {
                value = Integer.MIN_VALUE;
            } else {
                float percent = Attributes.getPercent(size, 0);
                value = Math.round(percent * componentSize);
            }
        } else {
            value = Attributes.getInt(mHapEngine, size, IntegerUtil.UNDEFINED);
            if (value < 0 || value == IntegerUtil.UNDEFINED) {
                value = -1;
            }
        }
        return value;
    }

    private boolean isInvalidSize(int size) {
        return (size <= 0) || (size == Integer.MAX_VALUE);
    }

    public int getHeight() {
        if (mHost == null) {
            return IntegerUtil.UNDEFINED;
        }
        return mHost.getHeight();
    }

    public int getWidth() {
        if (mHost == null) {
            return IntegerUtil.UNDEFINED;
        }
        return mHost.getWidth();
    }

    private void setVertical(boolean vertical) {
        isVertical = vertical;
        SwiperView swiperView = mHost;
        swiperView.setVertical(vertical);

        Set<String> keys = mCachedAttributes.keySet();
        for (String key : keys) {
            Object attribute = mCachedAttributes.get(key);
            if (attribute != null) {
                setAttribute(key, attribute);
            }
        }
    }

    private void setDuration(int duration) {
        if (mHost == null) {
            return;
        }
        mHost.setDuration(duration);
    }

    private void setRecyclerData(final RecyclerItem recyclerItem) {
        mRecyclerItem = recyclerItem;
        if (mHost != null) {
            mHost.setData(recyclerItem);
            setIndicatorEnabled(mIndicatorEnabled);
        }
    }

    private RecyclerItem getRecyclerItem() {
        return mRecyclerItem;
    }

    private void notifyItemInserted(int index) {
        if (mHost != null) {
            mHost.addIndicatorPoint();
            mAdapter.notifyItemInserted();
        }
    }

    private void notifyItemRemoved(int index) {
        if (mHost != null) {
            mHost.removeIndicatorPoint(index);
            mAdapter.notifyItemRemoved();
            int currentItem = mHost.getViewPager().getCurrentItem();
            if (index == currentItem && index != mHost.getIndicatorCount()) {
                // 删除页是当前页且非最后一页时，不会回调 onPageSelected 方法更新指示器位置，须要直接更新
                mHost.setSelectedIndicator(currentItem);
            }
        }
    }

    @Override
    public RecyclerDataItem.Creator getRecyclerDataItemCreator() {
        return RecyclerDataItemFactory.getInstance();
    }

    @Override
    public void onGlobalLayout() {

        if (mHost != null) {
            mHost.getViewTreeObserver().removeOnGlobalLayoutListener(this);
        }

        if (mSwiperAnimationChanged) {
            int width = getWidth();
            float translateX;
            if (mRatioAndFractionMap != null && mRatioAndFractionMap.size() != 0) {
                if (mRatioAndFractionMap.get(WIDTH_FRACTION_START) != null
                        && mRatioAndFractionMap.get(WIDTH_FRACTION_START) == 0) {
                    translateX = width * mRatioAndFractionMap.get(WIDTH_RATIO_START);
                    mSwiperAnimation.setTranslationXStart(translateX);
                }
                if (mRatioAndFractionMap.get(WIDTH_FRACTION_END) != null
                        && mRatioAndFractionMap.get(WIDTH_FRACTION_END) == 1) {
                    translateX = width * mRatioAndFractionMap.get(WIDTH_RATIO_END);
                    mSwiperAnimation.setTranslationXEnd(translateX);
                }
            }

            int height = getHeight();
            float translateY;
            if (mRatioAndFractionMap != null && mRatioAndFractionMap.size() != 0) {
                if (mRatioAndFractionMap.get(HEIGHT_FRACTION_START) != null
                        && mRatioAndFractionMap.get(HEIGHT_FRACTION_START) == 0) {
                    translateY = height * mRatioAndFractionMap.get(HEIGHT_RATIO_START);
                    mSwiperAnimation.setTranslationYStart(translateY);
                }
                if (mRatioAndFractionMap.get(HEIGHT_FRACTION_END) != null
                        && mRatioAndFractionMap.get(HEIGHT_FRACTION_END) == 1) {
                    translateY = height * mRatioAndFractionMap.get(HEIGHT_RATIO_END);
                    mSwiperAnimation.setTranslationYEnd(translateY);
                }
            }
            setPageAnimation(mSwiperAnimation);
        }
    }

    public SwiperAnimation setPageAnimationOrigin(String transformOrigin) {
        if (mHost == null) {
            return null;
        }
        float originX = Origin.parseOrigin(transformOrigin, Origin.ORIGIN_X, mHost, mHapEngine);
        float originY = Origin.parseOrigin(transformOrigin, Origin.ORIGIN_Y, mHost, mHapEngine);
        if (mSwiperAnimation != null) {
            if (!FloatUtil.isUndefined(originX)) {
                mSwiperAnimation.setPivotX(originX);
            }
            if (!FloatUtil.isUndefined(originY)) {
                mSwiperAnimation.setPivotY(originY);
            }
        }

        return mSwiperAnimation;
    }

    public static class RecyclerItem extends Container.RecyclerItem {

        public RecyclerItem(int ref, ComponentCreator componentCreator) {
            super(ref, componentCreator);
        }

        @Override
        protected void onApplyDataToComponent(Component recycle) {
            super.onApplyDataToComponent(recycle);
            ((Swiper) recycle).setRecyclerData(this);
        }

        @Override
        public void unbindComponent() {
            if (getBoundComponent() != null) {
                ((Swiper) getBoundComponent()).setRecyclerData(null);
            }
            super.unbindComponent();
        }

        @Override
        public void onChildAdded(RecyclerDataItem child, int index) {
            super.onChildAdded(child, index);
            if (getBoundComponent() != null) {
                ((Swiper) getBoundComponent()).notifyItemInserted(index);
            }
        }

        @Override
        public void onChildRemoved(RecyclerDataItem child, int index) {
            super.onChildRemoved(child, index);
            if (getBoundComponent() != null) {
                ((Swiper) getBoundComponent()).notifyItemRemoved(index);
            }
        }

        @Override
        public boolean isSupportTemplate() {
            return false;
        }
    }
}
