/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.render.css.media;

import android.util.Log;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.hapjs.common.utils.DisplayUtil;
import org.hapjs.runtime.DarkThemeUtil;
import org.hapjs.runtime.ProviderManager;
import org.hapjs.system.SysOpProvider;

/**
 * @media print { body { font-size: 10pt; } }
 * @media screen { body { font-size: 13px; } }
 * @media screen, print { body { line-height: 1.2; } }
 * @media only screen and (min-device-width: 320px) and (max-device-width: 480px) and (resolution:
 * 150dpi) { body { line-height: 1.4; } }
 * @media (prefers - color - scheme : dark) { .themed { background: white; color: black; } }
 */
public class CSSMediaParser {
    static final String ORIENTATION_PORTRAIT =
            "portrait"; // 可选值：orientation: portrait、orientation:landscape
    static final String ORIENTATION_LANDSCAPE =
            "landscape"; // 可选值：orientation: portrait、orientation:landscape
    static final String PERFERS_COLOR_SCHEME =
            "prefers-color-scheme";
    private static final String TAG = "CSSMediaParser";
    private static final String MEDIA_LIST_REFEX = "(\\s*,\\s*|\\s+or\\s+)";
    private static final String MEDIA_QUERY_REGEX = "\\s+and\\s+";
    private static final String MEDIA_OP_MORE = ">";
    private static final String MEDIA_OP_LESS = "<";
    private static final String MEDIA_OP_MORE_EQUAL = ">=";
    private static final String MEDIA_OP_LESS_EQUAL = "<=";
    private static final String MEDIA_OP_EQUAL_1 = ":";
    private static final String MEDIA_OP_EQUAL_2 = "==";
    private static final String MEDIA_PROPERTY_REGEX = "^\\((.*)\\)$";
    private static final String MEDIA_PROPERTY_REGEX_1 = "(.*)(<=|<)(.*)(<=|<)(.*)";
    private static final String MEDIA_PROPERTY_REGEX_2 = "(.*)(>=|>)(.*)(>=|>)(.*)";
    private static final String MEDIA_PROPERTY_REGEX_3 = "(.*)(>=|<=|<|>|:|==)(.*)";
    // min max
    private static final String HEIGHT = "height";
    // min max
    private static final String WIDTH = "width";
    // min max
    private static final String DEVICE_HEIGHT = "device-height";
    // min max
    private static final String DEVICE_WIDTH = "device-width";
    // min max
    private static final String RESOLUTION = "resolution"; // 设备的分辨率，支持dpi dppx 或者dpcm单位
    // min max
    private static final String ASPECT_RATIO =
            "aspect-ratio"; // 设备中的页面可见区域宽度与高度的比率。 如：aspect-ratio:1/1
    private static final String ORIENTATION =
            "orientation"; // 可选值：orientation: portrait、orientation:landscape
    // 设备主题模式，可选值 light、dark、no-preference（表示用户未指定操作系统主题。其作为布尔值时以false输出）

    /**
     * 解析一条condition eg: only screen and (min-device-width: 320px)
     *
     * @param mediaListText
     * @return
     */
    public static MediaList parseMediaList(String mediaListText) {
        MediaList mediaList = new MediaList();
        String[] mediaQueryTexts = mediaListText.trim().split(MEDIA_LIST_REFEX);
        List<MediaQuery> mediaQueryList = new ArrayList<>();
        MediaQuery mediaQuery;
        for (int i = 0; i < mediaQueryTexts.length; i++) {
            mediaQuery = parseMediaQuery(mediaQueryTexts[i]);
            if (mediaQuery != null) {
                mediaQueryList.add(mediaQuery);
            } else {
                Log.e(TAG, "parseMediaList: parseMediaProperty error, mediaListText = "
                        + mediaListText);
            }
        }
        MediaQuery[] mediaQueries = mediaQueryList.toArray(new MediaQuery[0]);
        mediaList.setMediaQueries(mediaQueries);
        return mediaList;
    }

    private static MediaQuery parseMediaQuery(String mediaQueryText) {
        mediaQueryText = mediaQueryText.trim();
        MediaQuery mediaQuery = new MediaQuery();

        if (mediaQueryText.startsWith("not")) {
            mediaQueryText = mediaQueryText.substring(3);
            mediaQuery.setIsNot(true);
        } else if (mediaQueryText.startsWith("only")) {
            mediaQueryText = mediaQueryText.substring(4); // ignore
        }

        mediaQueryText = mediaQueryText.trim();
        String[] mediaPropertyTexts = mediaQueryText.trim().split(MEDIA_QUERY_REGEX);

        int startOffset = 0;
        String first = mediaPropertyTexts[0].trim();
        if ("screen".equals(first)) {
            startOffset = 1;
        }
        int length = mediaPropertyTexts.length - startOffset;

        List<MediaProperty> mediaPropertyList = new ArrayList<>();
        MediaProperty mediaProperty;
        for (int i = 0; i < length; i++) {
            mediaProperty = parseMediaProperty(mediaPropertyTexts[i + startOffset]);
            if (mediaProperty != null) {
                mediaPropertyList.add(mediaProperty);
            } else {
                Log.e(TAG, "parseMediaQuery: parseMediaProperty error, mediaQueryText = "
                        + mediaQueryText);
            }
        }
        MediaProperty[] mediaProperties = mediaPropertyList.toArray(new MediaProperty[0]);
        mediaQuery.setMediaProperties(mediaProperties);
        return mediaQuery;
    }

    private static MediaProperty parseMediaProperty(String mediaPropertyText) {
        mediaPropertyText = mediaPropertyText.trim();

        Pattern r = Pattern.compile(MEDIA_PROPERTY_REGEX);
        Matcher m = r.matcher(mediaPropertyText);

        if (m.find()) {
            mediaPropertyText = m.group(1);
        }

        r = Pattern.compile(MEDIA_PROPERTY_REGEX_1 + "|" + MEDIA_PROPERTY_REGEX_2);
        m = r.matcher(mediaPropertyText);
        if (m.find()) {
            return parseMediaProperty(m.group(1), m.group(2), m.group(3), m.group(4), m.group(5));
        }

        r = Pattern.compile(MEDIA_PROPERTY_REGEX_3);
        m = r.matcher(mediaPropertyText);
        if (m.find()) {
            return parseMediaProperty(m.group(1), m.group(2), m.group(3));
        }

        return null;
    }

    private static MediaProperty parseMediaProperty(
            String value1, String op1, String name, String op2, String value2) {
        name = name.trim();
        value1 = value1.trim();
        value2 = value2.trim();

        MediaProperty mediaProperty = createMediaProperty(name);
        if (mediaProperty != null) {
            CompareOperator operator =
                    MediaPropertyFactory.createIntCompareOperator(
                            parseInt(value1), reverseOp(parseOp(op1)), parseInt(value2),
                            parseOp(op2));
            mediaProperty.setCompareOperator(operator);
            return mediaProperty;
        } else {
            Log.e(TAG, "parseMediaProperty: not support mediaProperty, name = " + name);
            return null;
        }
    }

    private static MediaProperty parseMediaProperty(String value1, String op1, String value2) {
        value1 = value1.trim();
        value2 = value2.trim();

        int op = parseOp(op1);

        if (op == CompareOperator.MEDIA_OP_EQUAL) {
            if (value1.startsWith("min-")) {
                value1 = value1.substring(4);
                op = CompareOperator.MEDIA_OP_MORE_EQUAL;
            } else if (value1.startsWith("max-")) {
                value1 = value1.substring(4);
                op = CompareOperator.MEDIA_OP_LESS_EQUAL;
            } else if (value2.startsWith("min-")) {
                value2 = value2.substring(4);
                op = CompareOperator.MEDIA_OP_MORE_EQUAL;
            } else if (value2.startsWith("max-")) {
                value2 = value2.substring(4);
                op = CompareOperator.MEDIA_OP_LESS_EQUAL;
            }
        }

        MediaProperty mediaProperty = createMediaProperty(value1);
        String compareTo = value2;
        if (mediaProperty == null) {
            mediaProperty = createMediaProperty(value2);
            compareTo = value1;
            op = reverseOp(op);
        }
        if (mediaProperty == null) {
            return null;
        }

        CompareOperator operator = null;
        switch (mediaProperty.getType()) {
            case MediaProperty.ORIENTATION:
                int orientation = parseOrientation(compareTo);
                operator = MediaPropertyFactory.createSimpleCompareOperator(orientation);
                break;
            case MediaProperty.RESOLUTION:
                int resolution = parseResolution(compareTo);
                operator = MediaPropertyFactory.createIntCompareOperator(resolution, op);
                break;
            case MediaProperty.ASPECT_RATIO:
                int aspectRatio = parseAspectRatio(compareTo);
                operator = MediaPropertyFactory.createIntCompareOperator(aspectRatio, op);
                break;
            case MediaProperty.PREFERS_COLOR_SCHEME:
                SysOpProvider sysOpProvider =
                        ProviderManager.getDefault().getProvider(SysOpProvider.NAME);
                if (sysOpProvider != null && sysOpProvider.allowNightModeInAndroidVersion()) {
                    int perfersColorScheme = parsePrefersColorScheme(compareTo);
                    operator =
                            MediaPropertyFactory.createIntCompareOperator(perfersColorScheme, op);
                }
                break;
            default:
                operator = MediaPropertyFactory.createIntCompareOperator(parseInt(compareTo), op);
        }

        mediaProperty.setCompareOperator(operator);
        return mediaProperty;
    }

    private static int parsePrefersColorScheme(String value) {
        if ("dark".equals(value)) {
            return DarkThemeUtil.THEME_NIGHT_YES;
        } else if ("light".equals(value)) {
            return DarkThemeUtil.THEME_NIGHT_NO;
        } else if ("no-preference".equals(value)) {
            return DarkThemeUtil.THEME_NIGHT_NO;
        }
        return DarkThemeUtil.THEME_NIGHT_NO;
    }

    private static int parseAspectRatio(String value) {
        if (value.contains("/")) {
            String[] a = value.split("/");
            return 100000 * parseInt(a[0]) / parseInt(a[1]);
        }
        return 0;
    }

    // https://developer.mozilla.org/en-US/docs/Web/CSS/resolution
    private static int parseResolution(String value) {
        if (value.endsWith("dpi")) {
            value = value.substring(0, value.length() - 3);
            return parseInt(value);
        }
        if (value.endsWith("dpcm")) {
            value = value.substring(0, value.length() - 4);
            return (int) (parseInt(value) * 2.54); // As 1 inch is 2.54 cm, 1dpcm ≈ 2.54dpi.
        }
        if (value.endsWith("dppx")) {
            value = value.substring(0, value.length() - 4);
            // Baseline density is 160 dpi in Android instead of 96 dpi
            return parseInt(value) * 160; // 1dppx is equivalent to 160dpi
        }
        return 0;
    }

    private static int parseOrientation(String name) {
        switch (name) {
            case ORIENTATION_PORTRAIT:
                return MediaProperty.ORIENTATION_PORTRAIT;
            case ORIENTATION_LANDSCAPE:
                return MediaProperty.ORIENTATION_LANDSCAPE;
            default:
                break;
        }
        return MediaProperty.ORIENTATION_PORTRAIT;
    }

    private static Integer parseInt(String value) {
        try {
            return Integer.valueOf(value.trim());
        } catch (NumberFormatException e) {
            e.printStackTrace();
        }
        return 0;
    }

    private static MediaProperty createMediaProperty(String name) {
        switch (name) {
            case HEIGHT:
                if (DisplayUtil.getHapEngine().isCardMode()
                        && DisplayUtil.getViewPortHeightByDp() == 0) {
                    return MediaPropertyFactory.createInvalidProperty();
                }
                return MediaPropertyFactory.createHeightProperty();
            case WIDTH:
                if (DisplayUtil.getHapEngine().isCardMode()
                        && DisplayUtil.getViewPortWidthByDp() == 0) {
                    return MediaPropertyFactory.createInvalidProperty();
                }
                return MediaPropertyFactory.createWidthProperty();
            case DEVICE_HEIGHT:
                return MediaPropertyFactory.createDeviceHeightProperty();
            case DEVICE_WIDTH:
                return MediaPropertyFactory.createDeviceWidthProperty();
            case RESOLUTION:
                return MediaPropertyFactory.createResolutionProperty();
            case ASPECT_RATIO:
                return MediaPropertyFactory.createAspectRatioProperty();
            case ORIENTATION:
                return MediaPropertyFactory.createOrientationProperty();
            case PERFERS_COLOR_SCHEME:
                return MediaPropertyFactory.createPrefersColorSchemeProperty();
            default:
                break;
        }
        return null;
    }

    private static int parseOp(String op) {
        switch (op) {
            case MEDIA_OP_EQUAL_1:
            case MEDIA_OP_EQUAL_2:
                return CompareOperator.MEDIA_OP_EQUAL;
            case MEDIA_OP_LESS:
                return CompareOperator.MEDIA_OP_LESS;
            case MEDIA_OP_LESS_EQUAL:
                return CompareOperator.MEDIA_OP_LESS_EQUAL;
            case MEDIA_OP_MORE:
                return CompareOperator.MEDIA_OP_MORE;
            case MEDIA_OP_MORE_EQUAL:
                return CompareOperator.MEDIA_OP_MORE_EQUAL;
            default:
                break;
        }
        throw new IllegalStateException();
    }

    private static int reverseOp(int op) {
        switch (op) {
            case CompareOperator.MEDIA_OP_EQUAL:
                return CompareOperator.MEDIA_OP_EQUAL;
            case CompareOperator.MEDIA_OP_MORE:
                return CompareOperator.MEDIA_OP_LESS;
            case CompareOperator.MEDIA_OP_MORE_EQUAL:
                return CompareOperator.MEDIA_OP_LESS_EQUAL;
            case CompareOperator.MEDIA_OP_LESS:
                return CompareOperator.MEDIA_OP_MORE;
            case CompareOperator.MEDIA_OP_LESS_EQUAL:
                return CompareOperator.MEDIA_OP_MORE_EQUAL;
            default:
                break;
        }
        throw new IllegalStateException();
    }
}
