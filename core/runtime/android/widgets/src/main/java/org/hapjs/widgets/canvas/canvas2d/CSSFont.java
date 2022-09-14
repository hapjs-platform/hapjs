/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.widgets.canvas.canvas2d;

import android.graphics.Typeface;
import android.text.TextUtils;
import android.util.Log;

public class CSSFont {

    private static final String TAG = "CSSFont";

    private static final String BLANK = " ";

    private static final String FONT_STYLE_NORMAL = "normal";
    private static final String FONT_STYLE_ITALIC = "italic";
    private static final String FONT_STYLE_OBLIQUE = "oblique";

    private static final String FONT_VARIANT_NORMAL = "normal";
    private static final String FONT_VARIANT_SMALL_CAPS = "small-caps";

    private static final String FONT_WEIGHT_NORMAL = "normal";
    private static final String FONT_WEIGHT_BOLD = "bold";
    private static final String FONT_WEIGHT_BOLDER = "bolder";
    private static final String FONT_WEIGHT_LIGHTER = "lighter";
    private static final String FONT_WEIGHT_100 = "100";
    private static final String FONT_WEIGHT_200 = "200";
    private static final String FONT_WEIGHT_300 = "300";
    private static final String FONT_WEIGHT_400 = "400";
    private static final String FONT_WEIGHT_500 = "500";
    private static final String FONT_WEIGHT_600 = "600";
    private static final String FONT_WEIGHT_700 = "700";
    private static final String FONT_WEIGHT_800 = "800";
    private static final String FONT_WEIGHT_900 = "900";
    private int mFontStyle;
    private String mFontVariant;
    private int mFontWeight;
    private float mFontSize;
    private Typeface mFontFamily;
    private Typeface mTypeface;

    private CSSFont(
            FontStyle style, FontVariant variant, FontWeight weight, float size, String family) {
        mFontStyle = style.value();
        mFontVariant = variant.value();
        mFontWeight = weight.value();
        mFontSize = size;

        switch (family) {
            case "sans-serif":
                mFontFamily = Typeface.SANS_SERIF;
                break;
            case "serif":
                mFontFamily = Typeface.SERIF;
                break;
            case "monospace":
                mFontFamily = Typeface.MONOSPACE;
                break;
            default:
                try {
                    mFontFamily = Typeface.create(family, Typeface.NORMAL);
                } catch (Exception e) {
                    Log.e(TAG, "unsupport font-family:" + family);
                }
                if (mFontFamily == null) {
                    mFontFamily = Typeface.SANS_SERIF;
                }
        }

        mTypeface = Typeface.create(mFontFamily, mFontStyle);
    }

    public static CSSFont parse(String font) {
        if (TextUtils.isEmpty(font)) {
            return null;
        }

        FontStyle fontStyle = FontStyle.NORMAL;
        FontVariant fontVariant = FontVariant.NORMAL;
        FontWeight fontWeight = FontWeight.NORMAL;
        float fontSize = 10;
        String fontFamily = "sans-serif";

        String[] fonts = font.split(BLANK);

        int size = fonts.length;
        for (int i = 0; i < size; i++) {
            String s = fonts[i];
            switch (s) {
                case FONT_STYLE_NORMAL:
                    fontStyle = FontStyle.NORMAL;
                    break;
                case FONT_STYLE_OBLIQUE:
                    fontStyle = FontStyle.OBLIQUE;
                    break;
                case FONT_STYLE_ITALIC:
                    fontStyle = FontStyle.ITALIC;
                    break;
                case FONT_VARIANT_SMALL_CAPS:
                    fontVariant = FontVariant.SMALL_CAPS;
                    break;
                case FONT_WEIGHT_100:
                    fontWeight = FontWeight._100_THIN;
                    break;
                case FONT_WEIGHT_200:
                    fontWeight = FontWeight._200_EXTRA_LIGHT;
                    break;
                case FONT_WEIGHT_300:
                    fontWeight = FontWeight._300_LIGHT;
                    break;
                case FONT_WEIGHT_400:
                    fontWeight = FontWeight._400_NORMAL;
                    break;
                case FONT_WEIGHT_500:
                    fontWeight = FontWeight._500_MEDIUM;
                    break;
                case FONT_WEIGHT_600:
                    fontWeight = FontWeight._600_SEMI_BOLD;
                    break;
                case FONT_WEIGHT_700:
                    fontWeight = FontWeight._700_BOLD;
                    break;
                case FONT_WEIGHT_800:
                    fontWeight = FontWeight._800_EXTRA_BOLD;
                    break;
                case FONT_WEIGHT_900:
                    fontWeight = FontWeight._900_BLACK;
                    break;
                case FONT_WEIGHT_BOLD:
                    fontWeight = FontWeight.BOLD;
                    break;
                case FONT_WEIGHT_BOLDER:
                    fontWeight = FontWeight.BOLDER;
                    break;
                case FONT_WEIGHT_LIGHTER:
                    fontWeight = FontWeight.LIGHTER;
                    break;
                default:
                    if (s.endsWith("px") && s.length() > 2) {
                        try {
                            fontSize = Float.parseFloat(s.substring(0, s.length() - 2));
                        } catch (NumberFormatException e) {
                            Log.e(TAG, "parse fontSize error:" + e);
                        }
                    }
                    if (i + 1 == size) {
                        fontFamily = fonts[i];
                    }
                    break;
            }
        }

        if (fontWeight.value() > FontWeight._500_MEDIUM.value()) {
            if (fontStyle == FontStyle.NORMAL) {
                fontStyle = FontStyle.BOLD;
            } else {
                fontStyle = FontStyle.BOLD_ITALIC;
            }
        }

        return new CSSFont(fontStyle, fontVariant, fontWeight, fontSize, fontFamily);
    }

    public Typeface getTypeface() {
        return mTypeface;
    }

    public float getFontSize() {
        return mFontSize;
    }

    /**
     * 文本的字体风格
     */
    enum FontStyle {
        NORMAL(Typeface.NORMAL),
        ITALIC(Typeface.ITALIC),
        OBLIQUE(Typeface.ITALIC),

        // 非web style
        BOLD(Typeface.BOLD),
        BOLD_ITALIC(Typeface.BOLD_ITALIC);

        private int mValue;

        FontStyle(int value) {
            mValue = value;
        }

        public int value() {
            return mValue;
        }
    }

    /**
     * 设置小型大写字母的显示风格，当设置为small_caps后,小写字母会被转换为大写，并且字体相比其余文本字体大小更小
     */
    enum FontVariant {
        NORMAL(FONT_VARIANT_NORMAL),
        SMALL_CAPS(FONT_VARIANT_SMALL_CAPS);

        private String mValue;

        FontVariant(String value) {
            mValue = value;
        }

        public String value() {
            return mValue;
        }
    }

    /**
     * 文本的粗细
     */
    enum FontWeight {
        NORMAL(400),
        BOLD(750),
        BOLDER(700),
        LIGHTER(100),
        _100_THIN(100), // thin
        _200_EXTRA_LIGHT(200), // extra light
        _300_LIGHT(300), // light
        _400_NORMAL(400), // normal
        _500_MEDIUM(500), // medium
        _600_SEMI_BOLD(600), // semi bold
        _700_BOLD(700), // bold
        _800_EXTRA_BOLD(800), // extra bold
        _900_BLACK(900); // black

        private int mWeight;

        FontWeight(int weight) {
            mWeight = weight;
        }

        public int value() {
            return mWeight;
        }
    }
}
