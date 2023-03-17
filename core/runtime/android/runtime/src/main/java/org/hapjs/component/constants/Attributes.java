/*
 * Copyright (c) 2021-present, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.component.constants;

import android.content.res.Configuration;
import android.text.TextUtils;
import android.util.Log;
import org.hapjs.card.sdk.utils.CardThemeUtils;
import org.hapjs.common.utils.DisplayUtil;
import org.hapjs.common.utils.FloatUtil;
import org.hapjs.render.Page;
import org.hapjs.runtime.HapEngine;
import org.hapjs.runtime.ProviderManager;

public class Attributes {

    private static final String TAG = "Attributes";

    private Attributes() {
    }

    public static int getInt(HapEngine hapEngine, Object value) {
        return getInt(hapEngine, value, 0);
    }

    public static int getInt(HapEngine hapEngine, Object value, int defValue) {
        return Math.round(getFloat(hapEngine, value, defValue));
    }

    public static int getFoldInt(HapEngine hapEngine, Object value, int defValue, boolean isUseFold) {
        return Math.round(getFoldFloat(hapEngine, value, defValue, isUseFold));
    }

    public static float getFoldFloat(HapEngine hapEngine, Object value, float defValue, boolean isUseFold) {
        if (value == null || "".equals(value)) {
            return defValue;
        }
        String temp = value.toString().trim();
        if (temp.startsWith(CardThemeUtils.KEY_THEME)) {
            String themeValue = CardThemeUtils.getThemeValue(temp);
            if (!TextUtils.isEmpty(themeValue)) {
                temp = themeValue;
            } else {
                return defValue;
            }
        }
        try {
            //px
            if (temp.endsWith(Unit.PX)) {
                temp = temp.substring(0, temp.length() - Unit.PX.length());
                float result = Float.parseFloat(temp);
                if (hapEngine == null) {
                    return defValue;
                }
                return DisplayUtil.getFoldRealPxByWidth(result, hapEngine.getDesignWidth(), isUseFold);
            }

            //dp
            if (temp.endsWith(Unit.DP)) {
                temp = temp.substring(0, temp.length() - Unit.DP.length());
                float result = Float.parseFloat(temp);
                if (hapEngine == null) {
                    return defValue;
                }
                return DisplayUtil.dip2Pixel(hapEngine.getContext(), (int) result);
            }

            //default
            return Float.parseFloat(temp);
        } catch (Exception e) {
            Log.e(TAG, "Attribute get float error: " + temp, e);
        }
        return defValue;
    }

    public static int getFoldFontSize(HapEngine hapEngine, Page page, Object value, boolean isUseFold) {
        return getFoldFontSize(hapEngine, page, value, 0, isUseFold);
    }

    /**
     * @param hapEngine
     * @param page
     * @param value
     * @param defValue
     * @return
     */
    public static int getFoldFontSize(HapEngine hapEngine, Page page, Object value, int defValue, boolean isUseFold) {
        if (hapEngine == null || page == null) {
            return defValue;
        }
        FontSizeProvider provider = ProviderManager.getDefault().getProvider(FontSizeProvider.NAME);
        float size = provider.getBestFontSize(hapEngine.getContext(), getFoldFloat(hapEngine, value, defValue, isUseFold));
        if (page.isTextSizeAdjustAuto()) {
            Configuration configuration = hapEngine.getContext().getResources().getConfiguration();
            size *= configuration.fontScale;
        }

        return Math.round(size);
    }

    public static float getFloat(HapEngine hapEngine, Object value) {
        return getFloat(hapEngine, value, FloatUtil.UNDEFINED);
    }

    public static float getFloat(HapEngine hapEngine, Object value, float defValue) {
        if (value == null || "".equals(value)) {
            return defValue;
        }
        String temp = value.toString().trim();
        if (temp.startsWith(CardThemeUtils.KEY_THEME)) {
            String themeValue = CardThemeUtils.getThemeValue(temp);
            if (!TextUtils.isEmpty(themeValue)) {
                temp = themeValue;
            } else {
                return defValue;
            }
        }
        try {
            // px
            if (temp.endsWith(Unit.PX)) {
                temp = temp.substring(0, temp.length() - Unit.PX.length());
                float result = Float.parseFloat(temp);
                if (hapEngine == null) {
                    return defValue;
                }
                return DisplayUtil.getRealPxByWidth(result, hapEngine.getDesignWidth());
            }

            // dp
            if (temp.endsWith(Unit.DP)) {
                temp = temp.substring(0, temp.length() - Unit.DP.length());
                float result = Float.parseFloat(temp);
                if (hapEngine == null) {
                    return defValue;
                }
                return DisplayUtil.dip2Pixel(hapEngine.getContext(), (int) result);
            }

            // default
            return Float.parseFloat(temp);
        } catch (Exception e) {
            Log.e(TAG, "Attribute get float error: " + temp, e);
        }
        return defValue;
    }

    public static boolean isSpecificAttributes(String value) {
        if (TextUtils.isEmpty(value)) {
            return false;
        }
        if (value.endsWith(Unit.PX)) {
            return true;
        } else if (value.endsWith(Unit.DP)) {
            return true;
        } else {
            return value.startsWith(CardThemeUtils.KEY_THEME);
        }
    }

    public static double getDouble(Object value) {
        return getDouble(value, Double.NaN);
    }

    public static double getDouble(Object value, double defValue) {
        if (value == null || "".equals(value)) {
            return defValue;
        }
        String temp = value.toString().trim();
        try {
            return Double.parseDouble(temp);
        } catch (Exception e) {
            Log.e(TAG, "Attribute get double error: " + temp, e);
        }
        return defValue;
    }

    public static String getString(Object value) {
        return getString(value, null);
    }

    public static String getString(Object value, String defValue) {
        if (value == null || "".equals(value)) {
            return defValue;
        }

        if (value instanceof String) {
            String value1 = (String) value;
            if (value1.startsWith(CardThemeUtils.KEY_THEME)) {
                String themeValue = CardThemeUtils.getThemeValue(value1);
                if (!TextUtils.isEmpty(themeValue)) {
                    return themeValue;
                }
                return defValue;
            }
            return value1;
        }

        return value.toString();
    }

    public static long getLong(Object value) {
        return getLong(value, 0L);
    }

    public static long getLong(Object value, long defValue) {
        if (value == null || "".equals(value)) {
            return defValue;
        }

        String temp = value.toString().trim();
        try {
            return Long.parseLong(temp);
        } catch (Exception e) {
            Log.e(TAG, "Attribute get long error: " + temp, e);
        }

        return 0;
    }

    public static boolean getBoolean(Object value, Boolean defValue) {
        if (value == null || "".equals(value)) {
            return defValue;
        }
        if (TextUtils.equals("false", value.toString())) {
            return false;
        } else if (TextUtils.equals("true", value.toString())) {
            return true;
        }
        return defValue;
    }

    public static float getPercent(Object value, float defValue) {

        if (value == null || "".equals(value)) {
            return defValue;
        }

        String temp = value.toString().trim();
        if (temp.endsWith(Unit.PERCENT)) {
            temp = temp.substring(0, temp.length() - Unit.PERCENT.length());
        }

        try {
            return Float.parseFloat(temp) / 100;
        } catch (Exception e) {
            Log.e(TAG, "Attribute get percent error: " + temp, e);
        }

        return defValue;
    }

    public static float getEm(Object value, float defValue) {
        if (value == null || "".equals(value)) {
            return defValue;
        }

        String temp = value.toString().trim();
        if (temp.endsWith(Unit.EM)) {
            temp = temp.substring(0, temp.length() - Unit.EM.length());
        }
        try {
            return Float.parseFloat(temp);
        } catch (Exception e) {
            Log.e(TAG, "Attribute get em error: " + temp, e);
        }

        return defValue;
    }

    public static float getCm(Object value, float defValue) {
        if (value == null || "".equals(value)) {
            return defValue;
        }
        String temp = value.toString().trim();
        if (temp.endsWith(Unit.CM)) {
            temp = temp.substring(0, temp.length() - Unit.CM.length());
        }
        try {
            return Float.parseFloat(temp);
        } catch (Exception e) {
            Log.e(TAG, "Attribute get cm error: " + temp, e);
        }

        return defValue;
    }

    public static int getFontSize(HapEngine hapEngine, Page page, Object value) {
        return getFontSize(hapEngine, page, value, 0);
    }

    public static int getFontSize(HapEngine hapEngine, Page page, Object value, int defValue) {
        if (hapEngine == null || page == null) {
            return defValue;
        }
        FontSizeProvider provider = ProviderManager.getDefault().getProvider(FontSizeProvider.NAME);
        float size = provider.getBestFontSize(hapEngine.getContext(), getFloat(hapEngine, value, defValue));
        if (page.isTextSizeAdjustAuto()) {
            Configuration configuration = hapEngine.getContext().getResources().getConfiguration();
            size *= configuration.fontScale;
        }
        return Math.round(size);
    }

    public interface Style {
        String ID = "id";
        String TARGET = "target";

        String WIDTH = "width";
        String HEIGHT = "height";
        String MIN_WIDTH = "minWidth";
        String MIN_HEIGHT = "minHeight";
        String MAX_WIDTH = "maxWidth";
        String MAX_HEIGHT = "maxHeight";
        String AUTO = "auto";
        String NONE = "none";
        String MIN_CONTENT = "minContent";
        String MAX_CONTENT = "maxContent";
        String FIT_CONTENT = "fitContent";

        String PADDING = "padding";
        String PADDING_LEFT = "paddingLeft";
        String PADDING_TOP = "paddingTop";
        String PADDING_RIGHT = "paddingRight";
        String PADDING_BOTTOM = "paddingBottom";

        String MARGIN = "margin";
        String MARGIN_AUTO = "auto";
        String MARGIN_LEFT = "marginLeft";
        String MARGIN_TOP = "marginTop";
        String MARGIN_RIGHT = "marginRight";
        String MARGIN_BOTTOM = "marginBottom";

        String BORDER_WIDTH = "borderWidth";
        String BORDER_LEFT_WIDTH = "borderLeftWidth";
        String BORDER_TOP_WIDTH = "borderTopWidth";
        String BORDER_RIGHT_WIDTH = "borderRightWidth";
        String BORDER_BOTTOM_WIDTH = "borderBottomWidth";

        String BORDER_COLOR = "borderColor";
        String BORDER_LEFT_COLOR = "borderLeftColor";
        String BORDER_TOP_COLOR = "borderTopColor";
        String BORDER_RIGHT_COLOR = "borderRightColor";
        String BORDER_BOTTOM_COLOR = "borderBottomColor";

        String BORDER_STYLE = "borderStyle";

        String BORDER_RADIUS = "borderRadius";
        String BORDER_TOP_LEFT_RADIUS = "borderTopLeftRadius";
        String BORDER_TOP_RIGHT_RADIUS = "borderTopRightRadius";
        String BORDER_BOTTOM_LEFT_RADIUS = "borderBottomLeftRadius";
        String BORDER_BOTTOM_RIGHT_RADIUS = "borderBottomRightRadius";

        String BACKGROUND_COLOR = "backgroundColor";
        String BACKGROUND_IMAGE = "backgroundImage";
        String BACKGROUND_SIZE = "backgroundSize";
        String BACKGROUND_REPEAT = "backgroundRepeat";
        String BACKGROUND_POSITION = "backgroundPosition";
        String BACKGROUND = "background";
        String OPACITY = "opacity";
        String DISPLAY = "display";
        String SHOW = "show";
        String VISIBILITY = "visibility";
        String POSITION = "position";

        String LEFT = "left";
        String TOP = "top";
        String RIGHT = "right";
        String BOTTOM = "bottom";

        String FLEX = "flex";
        String FLEX_GROW = "flexGrow";
        String FLEX_SHRINK = "flexShrink";
        String FLEX_BASIS = "flexBasis";
        String FILTER = "filter";
        String ALIGN_SELF = "alignSelf";
        String FLEX_DIRECTION = "flexDirection";
        String JUSTIFY_CONTENT = "justifyContent";
        String ALIGN_ITEMS = "alignItems";
        String FLEX_WRAP = "flexWrap";
        String ALIGN_CONTENT = "alignContent";
        String VIDEO_FULLSCREEN_CONTAINER = "enablevideofullscreencontainer";
        String OVERFLOW = "overflow";

        String HREF = "href";

        String LINES = "lines";
        String LINE_HEIGHT = "lineHeight";
        String COLOR = "color";
        String FONT_SIZE = "fontSize";
        String FONT_STYLE = "fontStyle";
        String FONT_WEIGHT = "fontWeight";
        String TEXT_DECORATION = "textDecoration";
        String TEXT_ALIGN = "textAlign";
        String PLACEHOLDER = "placeholder";
        String PLACEHOLDER_COLOR = "placeholderColor";
        String TYPE = "type";
        String TEXT_OVERFLOW = "textOverflow";
        String TEXT_INDENT = "textIndent";
        String LETTER_SPACING = "letterSpacing";

        String NAME = "name";
        String VALUE = "value";
        String CONTENT = "content";
        @Deprecated
        String RESIZE_MODE = "resizeMode";
        String OBJECT_FIT = "objectFit";
        String ALT_OBJECT_FIT = "altObjectFit";
        String SRC = "src";
        String SOURCE = "source";
        String ALT = "alt";

        String INDEX = "index";
        String AUTO_PLAY = "autoplay";
        String SCREEN_ORIENTATION = "screenOrientation";
        String ORIENTATION = "orientation";

        String PERCENT = "percent";
        String STROKE_WIDTH = "strokeWidth";

        String MIN = "min";
        String MAX = "max";
        String STEP = "step";
        String ENABLE = "enable";
        String SELECTED_COLOR = "selectedColor";

        String START = "start";
        String END = "end";
        String RANGE = "range";
        String SELECTED = "selected";

        String PROGRESS_COLOR = "progressColor";
        String OFFSET = "offset";
        String REFRESHING = "refreshing";
        String ENABLE_REFRESH = "enableRefresh";

        String ANIMATION_DURATION = "animationDuration";
        String ANIMATION_TIMING_FUNCTION = "animationTimingFunction";
        String ANIMATION_DELAY = "animationDelay";
        String ANIMATION_ITERATION_COUNT = "animationIterationCount";
        String ANIMATION_FILL_MODE = "animationFillMode";
        String ANIMATION_KEYFRAMES = "animationKeyframes";
        String ANIMATION_DIRECTION = "animationDirection";
        String PAGE_ANIMATION_KEYFRAMES = "pageAnimationKeyframes";
        String TRANSFORM = "transform";
        String TRANSFORM_ORIGIN = "transformOrigin";
        String PAGE_ANIMATION_ORIGIN = "pageTransformOrigin";

        String TRANSITION_PROPERTY = "transitionProperty";
        String TRANSITION_DURATION = "transitionDuration";
        String TRANSITION_TIMING_FUNCTION = "transitionTimingFunction";
        String TRANSITION_DELAY = "transitionDelay";
        String ALL = "all";

        String SCROLL_PAGE = "scrollpage";
        String COLUMNS = "columns";
        String FOCUS_BEHAVIOR = "focusbehavior";

        String COLUMN_SPAN = "columnSpan";

        String DIRECTION = "direction";
        String ENABLE_SWIPE = "enableswipe";

        String ACTIVE = "active";
        String DISABLED = "disabled";
        String FOCUSABLE = "focusable";
        String CHECKED = "checked";

        String DESCENDANT_FOCUSABILITY = "descendantfocusability";

        String MODE = "mode";

        String ARIA_LABEL = "ariaLabel";
        String ARIA_UNFOCUSABLE = "ariaUnfocusable";

        String FORCE_DARK = "forcedark";

        String AUTO_FOCUS = "autofocus";
    }

    public interface Unit {
        // when add new unit must check isSpecificAttributes function
        String PX = "px";
        String PERCENT = "%";
        String EM = "em";
        String CM = "cm";
        String DP = "dp";
    }

    public interface TextType {
        String TEXT = "text";
        String HTML = "html";
    }

    public interface InputType {
        String BUTTON = "button";
        String TEXT = "text";
        String CHECK_BOX = "checkbox";
        String DATE = "date";
        String TIME = "time";
        String EMAIL = "email";
        String NUMBER = "number";
        String PASSWORD = "password";
        String TELEPHONE = "tel";
    }

    public interface EventType {
        String SHORTCUT = "shortcut";
    }

    public interface AutoComplete {
        String ON = "on";
        String OFF = "off";
    }

    public interface PickerType {
        String DATE = "date";
        String TIME = "time";
        String TEXT = "text";
    }

    public interface PlayCount {
        String ONCE = "1";
        String INFINITE = "infinite";
    }

    public interface ProgressType {
        String HORIZONTAL = "horizontal";
        String CIRCULAR = "circular";
    }

    public interface EventDispatch {
        // control the dispatch of touch events
        // 不允许组件和父组件拦截点击事件
        String DISALLOW_INTERCEPT = "disallowintercept";
    }

    public interface Event {
        // common
        String CLICK = "click";
        String FOCUS = "focus";
        String BLUR = "blur";
        String LONGPRESS = "longpress";

        String CHANGE = "change";
        String RESIZE = "resize";

        String PAGE_SPLIT = "splitpage";
        String PAGE_CHANGED = "pagechanged";

        // list
        String SCROLL = "scroll";
        String SCROLL_BOTTOM = "scrollbottom";
        String SCROLL_TOP = "scrolltop";
        String SCROLL_END = "scrollend";
        String SCROLL_TOUCH_UP = "scrolltouchup";
        String SELECTED = "selected";

        // refresh
        String REFRESH = "refresh";

        String APPEAR = "appear";
        String DISAPPEAR = "disappear";

        String SWIPE = "swipe";

        String FULLSCREEN_CHANGE = "fullscreenchange";

        // touch
        String TOUCH_START = "touchstart";
        String TOUCH_MOVE = "touchmove";
        String TOUCH_END = "touchend";
        String TOUCH_CANCEL = "touchcancel";
        String TOUCH_CLICK = CLICK;
        String TOUCH_LONG_PRESS = LONGPRESS;

        // KeyEvent
        String KEY_EVENT = "key";
        String KEY_EVENT_PAGE = "pagekey";

        // animation
        String ANIMATION_START = "animationstart";
        String ANIMATION_END = "animationend";
        String ANIMATION_ITERATION = "animationiteration";
    }

    public interface EventParams {
        String IS_FROM_USER = "isFromUser";
    }

    public interface Display {
        String FLEX = "flex";
        String NONE = "none";
    }

    public interface Visibility {
        String VISIBLE = "visible";
        String HIDDEN = "hidden";
    }

    public interface Position {
        String FIXED = "fixed";
        String RELATIVE = "relative";
        String ABSOLUTE = "absolute";
    }

    public interface Align {
        String AUTO = "auto";
        String FLEX_START = "flex-start";
        String CENTER = "center";
        String FLEX_END = "flex-end";
        String STRETCH = "stretch";
        String BASELINE = "baseline";
        String SPACE_BETWEEN = "space-between";
        String SPACE_AROUND = "space-around";
    }

    public interface TextOverflow {
        String CLIP = "clip";
        String ELLIPSIS = "ellipsis";
        String STRING = "string";
    }

    public interface Mode {
        String SCROLLABLE = "scrollable";
        String FIXED = "fixed";
    }

    public interface ImageMode {
        String NONE = "none";
        String CONTAIN = "contain";
        String COVER = "cover";
    }

    public interface RepeatMode {
        // default
        String REPEAT = "repeat";

        String REPEAT_X = "repeat-x";
        String REPEAT_Y = "repeat-y";
        String REPEAT_NONE = "no-repeat";
    }

    public interface ObjectFit {
        String CONTAIN = "contain";
        String COVER = "cover";
        String FILL = "fill";
        String NONE = "none";
        String SCALE_DOWN = "scale-down";
    }

    public interface PositionMode {
        // default
        String TOP_LEFT = "0px 0px";
    }

    public interface PageAnimation {
        String ACTION_OPEN_ENTER = "openEnter";
        String ACTION_CLOSE_ENTER = "closeEnter";
        String ACTION_OPEN_EXIT = "openExit";
        String ACTION_CLOSE_EXIT = "closeExit";

        String SLIDE = "slide";
        // TODO: fade animation need to be defined at the standard conference
        String FADE = "fade";
        String NONE = "none";
    }

    public interface DescendantFocusabilityType {
        String BEFORE = "before";
        String AFTER = "after";
        String BLOCK = "block";
    }

    public interface OverflowType {
        String VISIBLE = "visible";
        String HIDDEN = "hidden";
    }

    public interface FocusBehavior {
        String ALIGNED = "aligned";
        String EDGED = "edged";
    }
}
