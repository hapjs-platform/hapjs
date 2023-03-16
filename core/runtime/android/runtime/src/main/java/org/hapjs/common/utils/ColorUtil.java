/*
 * Copyright (c) 2021-present, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.common.utils;

import android.graphics.Color;
import android.graphics.PixelFormat;
import android.text.TextUtils;
import android.util.Log;
import androidx.core.graphics.ColorUtils;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ColorUtil {

    private static final String TAG = "ColorUtil";
    private static final Map<String, Integer> colorMap = new HashMap<>();
    private static final Pattern RGB_PATTERN = Pattern.compile("^#[0-9a-fA-F]{3,9}$");
    private static final Pattern FUNCTION_RGBA_PATTERN =
            Pattern.compile(
                    "^(rgba?[\\(])([\\s]*[0-9%]+[\\s]*),([\\s]*[0-9%]+[\\s]*),(([\\s]*[0-9%]+[\\s]*),"
                            + ")?([\\s]*[0-9.]+[\\s]*)[\\)]$");
    private static final Pattern FUNCTION_RGB_PATTERN =
            Pattern.compile(
                    "^(rgb[\\(])([\\s]*[0-9%]+[\\s]*),([\\s]*[0-9%]+[\\s]*),([\\s]*[0-9.]+[\\s]*)[\\)]$");

    static {
        colorMap.put("aliceblue", 0XFFF0F8FF);
        colorMap.put("antiquewhite", 0XFFFAEBD7);
        colorMap.put("aqua", 0XFF00FFFF);
        colorMap.put("aquamarine", 0XFF7FFFD4);
        colorMap.put("azure", 0XFFF0FFFF);
        colorMap.put("beige", 0XFFF5F5DC);
        colorMap.put("bisque", 0XFFFFE4C4);
        colorMap.put("black", 0XFF000000);
        colorMap.put("blanchedalmond", 0XFFFFEBCD);
        colorMap.put("blue", 0XFF0000FF);
        colorMap.put("blueviolet", 0XFF8A2BE2);
        colorMap.put("brown", 0XFFA52A2A);
        colorMap.put("burlywood", 0XFFDEB887);
        colorMap.put("cadetblue", 0XFF5F9EA0);
        colorMap.put("chartreuse", 0XFF7FFF00);
        colorMap.put("chocolate", 0XFFD2691E);
        colorMap.put("coral", 0XFFFF7F50);
        colorMap.put("cornflowerblue", 0XFF6495ED);
        colorMap.put("cornsilk", 0XFFFFF8DC);
        colorMap.put("crimson", 0XFFDC143C);
        colorMap.put("cyan", 0XFF00FFFF);
        colorMap.put("darkblue", 0XFF00008B);
        colorMap.put("darkcyan", 0XFF008B8B);
        colorMap.put("darkgoldenrod", 0XFFB8860B);
        colorMap.put("darkgray", 0XFFA9A9A9);
        colorMap.put("darkgreen", 0XFF006400);
        colorMap.put("darkkhaki", 0XFFBDB76B);
        colorMap.put("darkmagenta", 0XFF8B008B);
        colorMap.put("darkolivegreen", 0XFF556B2F);
        colorMap.put("darkorange", 0XFFFF8C00);
        colorMap.put("darkorchid", 0XFF9932CC);
        colorMap.put("darkred", 0XFF8B0000);
        colorMap.put("darksalmon", 0XFFE9967A);
        colorMap.put("darkseagreen", 0XFF8FBC8F);
        colorMap.put("darkslateblue", 0XFF483D8B);
        colorMap.put("darkslategray", 0XFF2F4F4F);
        colorMap.put("darkslategrey", 0XFF2F4F4F);
        colorMap.put("darkturquoise", 0XFF00CED1);
        colorMap.put("darkviolet", 0XFF9400D3);
        colorMap.put("deeppink", 0XFFFF1493);
        colorMap.put("deepskyblue", 0XFF00BFFF);
        colorMap.put("dimgray", 0XFF696969);
        colorMap.put("dimgrey", 0XFF696969);
        colorMap.put("dodgerblue", 0XFF1E90FF);
        colorMap.put("firebrick", 0XFFB22222);
        colorMap.put("floralwhite", 0XFFFFFAF0);
        colorMap.put("forestgreen", 0XFF228B22);
        colorMap.put("fuchsia", 0XFFFF00FF);
        colorMap.put("gainsboro", 0XFFDCDCDC);
        colorMap.put("ghostwhite", 0XFFF8F8FF);
        colorMap.put("gold", 0XFFFFD700);
        colorMap.put("goldenrod", 0XFFDAA520);
        colorMap.put("gray", 0XFF808080);
        colorMap.put("grey", 0XFF808080);
        colorMap.put("green", 0XFF008000);
        colorMap.put("greenyellow", 0XFFADFF2F);
        colorMap.put("honeydew", 0XFFF0FFF0);
        colorMap.put("hotpink", 0XFFFF69B4);
        colorMap.put("indianred", 0XFFCD5C5C);
        colorMap.put("indigo", 0XFF4B0082);
        colorMap.put("ivory", 0XFFFFFFF0);
        colorMap.put("khaki", 0XFFF0E68C);
        colorMap.put("lavender", 0XFFE6E6FA);
        colorMap.put("lavenderblush", 0XFFFFF0F5);
        colorMap.put("lawngreen", 0XFF7CFC00);
        colorMap.put("lemonchiffon", 0XFFFFFACD);
        colorMap.put("lightblue", 0XFFADD8E6);
        colorMap.put("lightcoral", 0XFFF08080);
        colorMap.put("lightcyan", 0XFFE0FFFF);
        colorMap.put("lightgoldenrodyellow", 0XFFFAFAD2);
        colorMap.put("lightgray", 0XFFD3D3D3);
        colorMap.put("lightgrey", 0XFFD3D3D3);
        colorMap.put("lightgreen", 0XFF90EE90);
        colorMap.put("lightpink", 0XFFFFB6C1);
        colorMap.put("lightsalmon", 0XFFFFA07A);
        colorMap.put("lightseagreen", 0XFF20B2AA);
        colorMap.put("lightskyblue", 0XFF87CEFA);
        colorMap.put("lightslategray", 0XFF778899);
        colorMap.put("lightslategrey", 0XFF778899);
        colorMap.put("lightsteelblue", 0XFFB0C4DE);
        colorMap.put("lightyellow", 0XFFFFFFE0);
        colorMap.put("lime", 0XFF00FF00);
        colorMap.put("limegreen", 0XFF32CD32);
        colorMap.put("linen", 0XFFFAF0E6);
        colorMap.put("magenta", 0XFFFF00FF);
        colorMap.put("maroon", 0XFF800000);
        colorMap.put("mediumaquamarine", 0XFF66CDAA);
        colorMap.put("mediumblue", 0XFF0000CD);
        colorMap.put("mediumorchid", 0XFFBA55D3);
        colorMap.put("mediumpurple", 0XFF9370DB);
        colorMap.put("mediumseagreen", 0XFF3CB371);
        colorMap.put("mediumslateblue", 0XFF7B68EE);
        colorMap.put("mediumspringgreen", 0XFF00FA9A);
        colorMap.put("mediumturquoise", 0XFF48D1CC);
        colorMap.put("mediumvioletred", 0XFFC71585);
        colorMap.put("midnightblue", 0XFF191970);
        colorMap.put("mintcream", 0XFFF5FFFA);
        colorMap.put("mistyrose", 0XFFFFE4E1);
        colorMap.put("moccasin", 0XFFFFE4B5);
        colorMap.put("navajowhite", 0XFFFFDEAD);
        colorMap.put("navy", 0XFF000080);
        colorMap.put("oldlace", 0XFFFDF5E6);
        colorMap.put("olive", 0XFF808000);
        colorMap.put("olivedrab", 0XFF6B8E23);
        colorMap.put("orange", 0XFFFFA500);
        colorMap.put("orangered", 0XFFFF4500);
        colorMap.put("orchid", 0XFFDA70D6);
        colorMap.put("palegoldenrod", 0XFFEEE8AA);
        colorMap.put("palegreen", 0XFF98FB98);
        colorMap.put("paleturquoise", 0XFFAFEEEE);
        colorMap.put("palevioletred", 0XFFDB7093);
        colorMap.put("papayawhip", 0XFFFFEFD5);
        colorMap.put("peachpuff", 0XFFFFDAB9);
        colorMap.put("peru", 0XFFCD853F);
        colorMap.put("pink", 0XFFFFC0CB);
        colorMap.put("plum", 0XFFDDA0DD);
        colorMap.put("powderblue", 0XFFB0E0E6);
        colorMap.put("purple", 0XFF800080);
        colorMap.put("rebeccapurple", 0XFF663399);
        colorMap.put("red", 0XFFFF0000);
        colorMap.put("rosybrown", 0XFFBC8F8F);
        colorMap.put("royalblue", 0XFF4169E1);
        colorMap.put("saddlebrown", 0XFF8B4513);
        colorMap.put("salmon", 0XFFFA8072);
        colorMap.put("sandybrown", 0XFFF4A460);
        colorMap.put("seagreen", 0XFF2E8B57);
        colorMap.put("seashell", 0XFFFFF5EE);
        colorMap.put("sienna", 0XFFA0522D);
        colorMap.put("silver", 0XFFC0C0C0);
        colorMap.put("skyblue", 0XFF87CEEB);
        colorMap.put("slateblue", 0XFF6A5ACD);
        colorMap.put("slategray", 0XFF708090);
        colorMap.put("slategrey", 0XFF708090);
        colorMap.put("snow", 0XFFFFFAFA);
        colorMap.put("springgreen", 0XFF00FF7F);
        colorMap.put("steelblue", 0XFF4682B4);
        colorMap.put("tan", 0XFFD2B48C);
        colorMap.put("teal", 0XFF008080);
        colorMap.put("thistle", 0XFFD8BFD8);
        colorMap.put("tomato", 0XFFFF6347);
        colorMap.put("turquoise", 0XFF40E0D0);
        colorMap.put("violet", 0XFFEE82EE);
        colorMap.put("wheat", 0XFFF5DEB3);
        colorMap.put("white", 0XFFFFFFFF);
        colorMap.put("whitesmoke", 0XFFF5F5F5);
        colorMap.put("yellow", 0XFFFFFF00);
        colorMap.put("yellowgreen", 0XFF9ACD32);
        colorMap.put("transparent", 0x00000000);
    }

    private ColorUtil() {
    }

    /**
     * 在JS框架层获取颜色的通用格式（十六进制的字符串）
     *
     * @param color
     * @return
     */
    public static String getColorStr(int color) {
        return String.format("#%06X", (0xFFFFFF & color));
    }

    public static int getColor(String color) {
        return getColor(color, Color.BLACK);
    }

    public static int getColor(String color, int defaultColor) {
        if (TextUtils.isEmpty(color)) {
            return defaultColor;
        }
        color = color.trim(); // remove non visible codes

        int resultColor = defaultColor;
        if (!TextUtils.isEmpty(color) && (color.startsWith("hsla(") || color.startsWith("hsl("))) {
            resultColor = getHslValues(color, defaultColor);
            return resultColor;
        }
        try {
            ColorConvertHandler[] handlers = ColorConvertHandler.values();
            Integer convertedColor;
            for (ColorConvertHandler handler : handlers) {
                convertedColor = handler.handle(color);
                if (convertedColor != null) {
                    resultColor = convertedColor;
                    break;
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "get color error", e);
        }
        return resultColor;
    }

    private static int getHslValues(String hsla, int defaultColor) {
        if (TextUtils.isEmpty(hsla)) {
            return defaultColor;
        }
        float[] hslValues = null;
        String hslaRealString = "";
        // 默认设置不透明
        int alphaValue = 255;
        boolean isHSLA = false;
        boolean isRightValue = true;
        try {
            if ((hsla.startsWith("hsla(") || hsla.startsWith("hsl(")) && hsla.endsWith(")")) {
                hslaRealString = hsla.substring(hsla.indexOf("(") + 1, hsla.length() - 1);
                String[] hslaGroup = hslaRealString.split(",");
                String trimValue = null;
                if (null != hslaGroup && (hslaGroup.length == 4 || hslaGroup.length == 3)) {
                    hslValues = new float[3];
                    trimValue = hslaGroup[0].trim();
                    hslValues[0] = Float.parseFloat(trimValue);
                    if (hslValues[0] < 0 || hslValues[0] > 360) {
                        isRightValue = false;
                    } else {
                        hslValues[0] = hslValues[0] / 360.0f;
                    }
                    trimValue = hslaGroup[1].trim();
                    if (trimValue.endsWith("%")) {
                        hslValues[1] =
                                Float.parseFloat(trimValue.substring(0, trimValue.length() - 1));
                        if (hslValues[1] > 0) {
                            hslValues[1] = hslValues[1] / 100.0f;
                        }
                    } else {
                        isRightValue = false;
                    }
                    trimValue = hslaGroup[2].trim();
                    if (trimValue.endsWith("%")) {
                        hslValues[2] =
                                Float.parseFloat(trimValue.substring(0, trimValue.length() - 1));
                        if (hslValues[2] > 0) {
                            hslValues[2] = hslValues[2] / 100.0f;
                        }
                    } else {
                        isRightValue = false;
                    }
                    if (hslaGroup.length == 4) {
                        trimValue = hslaGroup[3].trim();
                        isHSLA = true;
                        if ((trimValue.endsWith("%"))) {
                            trimValue = trimValue.substring(0, trimValue.length() - 1);
                            alphaValue = (int) (Float.parseFloat(trimValue) / 100 * 255);
                        } else {
                            float alpha = Float.parseFloat(trimValue);
                            if (0f <= alpha && alpha <= 1f) {
                                alphaValue = (int) (alpha * 255);
                            } else {
                                alphaValue = (int) alpha;
                            }
                        }
                    }
                }
            } else {
                isRightValue = false;
            }
        } catch (NumberFormatException e) {
            isRightValue = false;
            Log.e(TAG, "color string is not right : " + hsla);
        }
        if (!isRightValue) {
            Log.e(TAG, "color string is not right : " + hsla);
        }
        int color = defaultColor;
        if (null != hslValues) {
            int[] rgb = hslToRgb(hslValues[0], hslValues[1], hslValues[2]);
            if (null != rgb && rgb.length == 3) {
                if (isHSLA) {
                    color = Color.argb(alphaValue, rgb[0], rgb[1], rgb[2]);
                } else {
                    color = Color.rgb(rgb[0], rgb[1], rgb[2]);
                }
            }
        }
        return color;
    }

    /**
     * Converts an HSL color value to RGB. Conversion formula adapted from
     * http://en.wikipedia.org/wiki/HSL_color_space. Assumes h, s, and l are contained in the set [0,
     * 1] and returns r, g, and b in the set [0, 255].
     *
     * @param h The hue
     * @param s The saturation
     * @param l The lightness
     * @return int array, the RGB representation
     */
    public static int[] hslToRgb(float h, float s, float l) {
        float r;
        float g;
        float b;

        if (s == 0f) {
            r = g = b = l; // achromatic
        } else {
            float q = l < 0.5f ? l * (1 + s) : l + s - l * s;
            float p = 2 * l - q;
            r = hueToRgb(p, q, h + 1f / 3f);
            g = hueToRgb(p, q, h);
            b = hueToRgb(p, q, h - 1f / 3f);
        }
        int[] rgb = {(int) (r * 255), (int) (g * 255), (int) (b * 255)};
        return rgb;
    }

    /**
     * Helper method that converts hue to rgb
     */
    public static float hueToRgb(float p, float q, float t) {
        if (t < 0f) {
            t += 1f;
        }
        if (t > 1f) {
            t -= 1f;
        }
        if (t < 1f / 6f) {
            return p + (q - p) * 6f * t;
        }
        if (t < 1f / 2f) {
            return q;
        }
        if (t < 2f / 3f) {
            return p + (q - p) * (2f / 3f - t) * 6f;
        }
        return p;
    }

    public static String getColorText(int color) {
        return String.format("#%08X", color);
    }

    /**
     * Multiplies the color with the given alpha.
     *
     * @param color color to be multiplied
     * @param alpha value between 0 and 255
     * @return multiplied color
     */
    public static int multiplyColorAlpha(int color, int alpha) {
        if (alpha == 255) {
            return color;
        }
        if (alpha == 0) {
            return color & 0x00FFFFFF;
        }
        alpha = alpha + (alpha >> 7); // make it 0..256
        int colorAlpha = color >>> 24;
        int multipliedAlpha = colorAlpha * alpha >> 8;
        return (multipliedAlpha << 24) | (color & 0x00FFFFFF);
    }

    /**
     * Gets the opacity from a color. Inspired by Android ColorDrawable.
     *
     * @return opacity expressed by one of PixelFormat constants
     */
    public static int getOpacityFromColor(int color) {
        int colorAlpha = color >>> 24;
        if (colorAlpha == 255) {
            return PixelFormat.OPAQUE;
        } else if (colorAlpha == 0) {
            return PixelFormat.TRANSPARENT;
        } else {
            return PixelFormat.TRANSLUCENT;
        }
    }

    /**
     * Returns the grayscale value from a color int.
     *
     * @return a value between 0.0f(black) and 1.0f(white)
     */
    public static float getGrayscaleFromColor(int color) {
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;

        float grayscale = (r * 30 + g * 59 + b * 11) >> 8;

        return (grayscale / 100.f);
    }

    /**
     * Return whether color's alpha value has been set.
     *
     * @return true has alpha value, false otherwise.
     */
    public static boolean hasAlpha(String rawColor) {
        if (FUNCTION_RGB_PATTERN.matcher(rawColor).matches()) {
            return false;
        }
        if (RGB_PATTERN.matcher(rawColor).matches()) {
            if (rawColor.length() == 4 || rawColor.length() == 7) {
                return false;
            }
        }

        return true;
    }

    enum ColorConvertHandler {
        NAMED_COLOR_HANDLER {
            @Override
            Integer handle(String rawColor) {
                return colorMap.get(rawColor);
            }
        },
        RGB_HANDLER {
            @Override
            Integer handle(String rawColor) {
                if (RGB_PATTERN.matcher(rawColor).matches()) {
                    if (rawColor.length() == 4) {
                        // #eee, #333
                        int result = 0;
                        for (int i = 1; i < 4; i++) {
                            result = (result << 8) + 17 * Character.digit(rawColor.charAt(i), 16);
                        }
                        return result | 0xff000000;
                    } else if (rawColor.length() == 7 || rawColor.length() == 9) {
                        // #eeeeee, #333333
                        return Color.parseColor(rawColor);
                    } else {
                        throw new IllegalArgumentException(
                                "ColorConvertHandler invalid color: " + rawColor);
                    }
                }
                return null;
            }
        },
        FUNCTIONAL_RGBA_HANDLER {
            @Override
            Integer handle(String rawColor) {
                return convertFunctionalColor(rawColor);
            }
        };

        /**
         * Parse a functional RGB to #RRGGBB or functional RGBA to #AARRGGBB
         *
         * @param raw functional RGB or functional RGBA
         * @return #RRGGBB or #AARRGGBB
         */
        private static Integer convertFunctionalColor(String raw) {
            Matcher matcher = FUNCTION_RGBA_PATTERN.matcher(raw);
            if (matcher.matches()) {
                boolean alpha = matcher.group(1).startsWith("rgba");
                String[] gradients = new String[alpha ? 4 : 3];
                gradients[0] = matcher.group(2);
                gradients[1] = matcher.group(3);
                if (alpha) {
                    gradients[2] = matcher.group(5);
                    gradients[3] = matcher.group(6);
                } else {
                    gradients[2] = matcher.group(6);
                }
                return parseRGBA(gradients);
            }
            return null;
        }

        /**
         * Parse Functional RGB to RRGGBB mode
         *
         * @param gradients gradients of functional RGB
         * @return RRGGBB color
         */
        private static Integer parseRGBA(String[] gradients) {
            int[] digits = new int[4];
            if (gradients.length == 3) {
                digits[3] = 0xff;
            }
            int percentLoc;
            int value;
            String gradient;
            for (int i = 0; i < gradients.length; i++) {
                gradient = gradients[i].trim();
                if ((percentLoc = gradient.lastIndexOf("%")) != -1) {
                    gradient = gradient.substring(0, percentLoc);
                    value = (int) (Float.parseFloat(gradient) / 100 * 255);
                } else {
                    float temp = Float.parseFloat(gradient);
                    if (0f < temp && temp <= 1f && i == 3) {
                        value = (int) (temp * 255);
                    } else {
                        value = (int) temp;
                    }
                }
                if (value < 0 || value > 255) {
                    throw new IllegalArgumentException(
                            "ColorConvertHandler invalid gradient: " + gradient);
                }
                digits[i] = value;
            }

            int result = digits[3];
            for (int i = 0; i < 3; i++) {
                result = (result << 8) + digits[i];
            }
            return result;
        }

        /**
         * Parse color to #RRGGBB or #AARRGGBB. The parsing algorithm depends on sub-class.
         *
         * @param rawColor color, maybe functional RGB(RGBA), #RGB, keywords color or transparent
         * @return #RRGGBB or #AARRGGBB
         */
        abstract Integer handle(String rawColor);
    }

    //颜色相似度计算
    public static double LabDiff(int first, int second) {
        double[] lab_f = new double[3];
        double[] lab_s = new double[3];
        ColorUtils.colorToLAB(first, lab_f);
        ColorUtils.colorToLAB(second, lab_s);

        //计算CIELAB公式中的L*、a*、b*、C*ab
        double c1, c2;
        c1 = chroma(lab_f[1], lab_f[2]);
        c2 = chroma(lab_s[1], lab_s[2]);

        //计算L‘、a‘、C‘、h‘
        double L1, L2, a1, a2, b1, b2, C1, C2, h1, h2, G;
        double c_avr = (c1 + c2) / 2;
        double c_avr_pow7 = Math.pow(c_avr, 7);
        G = 0.5 * (1 - Math.sqrt(c_avr_pow7 / (c_avr_pow7 + Math.pow(25, 7))));
        L1 = lab_f[0];
        L2 = lab_s[0];
        a1 = (1 + G) * lab_f[1];
        a2 = (1 + G) * lab_s[1];
        b1 = lab_f[2];
        b2 = lab_s[2];
        C1 = chroma(a1, b1);
        C2 = chroma(a2, b2);
        h1 = hueAngle(a1, b1);
        h2 = hueAngle(a2, b2);

        //计算△L‘、△C‘ab、△H‘ab
        double dt_L, dt_C, dt_h, dt_H;
        dt_L = L1 - L2;
        dt_C = C1 - C2;
        dt_h = h1 - h2;
        dt_H = 2 * Math.sqrt(C1 * C2) * Math.sin(Math.PI * dt_h / 360);

        //计算公式中的加权函数SL,SC,SH,T
        double S_l, S_c, S_h, T, L_avr, C_avr, h_avr;
        L_avr = (L1 + L2) / 2;
        C_avr = (C1 + C2) / 2;
        h_avr = (h1 + h2) / 2;
        T = 1 - 0.17 * Math.cos((h_avr - 30) * Math.PI / 180) + 0.24 * Math.cos(2 * h_avr * Math.PI / 180) + 0.32 * Math.cos((3 * h_avr + 6) * Math.PI / 180) - 0.2 * Math.cos((4 * h_avr - 63) * Math.PI / 180);
        S_l = 1 + 0.015 * Math.pow(L_avr - 50, 2) / Math.pow(20 + Math.pow(L_avr - 50, 2), 0.5);
        S_c = 1 + 0.045 * C_avr;
        S_h = 1 + 0.015 * C_avr * T;

        //计算公式中的RT
        double Rt, An, Rc, C_avr_pow7;
        An = 30 * Math.exp(-Math.pow((h_avr - 275) / 25, 2));
        C_avr_pow7 = Math.pow(C_avr, 7);
        Rc = 2 * Math.sqrt(C_avr_pow7 / (C_avr_pow7 + Math.pow(25, 7)));
        Rt = -Math.sin(2 * An * Math.PI / 180) * Rc;

        double dt_E200, K_l, K_c, K_h;
        K_l = K_c = K_h = 1;
        dt_L /= K_l * S_l;
        dt_C /= K_c * S_c;
        dt_H /= K_h * S_h;
        dt_E200 = Math.sqrt(dt_L * dt_L + dt_C * dt_C + dt_H * dt_H + Rt * dt_C * dt_H);
        return dt_E200;
    }

    //计算彩度值
    private static double chroma(double a, double b) {
        double Cab = 0;
        Cab = Math.sqrt(Math.pow(a, 2) + Math.pow(b, 2));
        return Cab;
    }


    //计算色度角
    private static double hueAngle(double a, double b) {
        double angle = 0;
        double angle_ab = 0;
        if (a == 0) {
            return 90;
        }
        angle = (180 / Math.PI) * Math.atan(b / a);
        if (a > 0 && b > 0) {
            angle_ab = angle;
        } else if (a < 0 && b > 0) {
            angle_ab = 180 + angle;
        } else if (a < 0 && b < 0) {
            angle_ab = 180 + angle;
        } else {
            angle_ab = 360 + angle;
        }
        return angle_ab;
    }
}
