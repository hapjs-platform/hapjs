/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.render.skeleton;

import android.text.TextUtils;
import android.util.Log;
import android.util.Xml;
import java.io.IOException;
import java.io.InputStream;
import org.hapjs.render.css.media.CSSMediaParser;
import org.hapjs.render.css.media.MediaList;
import org.hapjs.render.css.media.MediaPropertyInfoImpl;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

/**
 * parse skeleton DSL(.sk file)
 */
public class SkeletonDSLParser {
    private static final String TAG = "SkeletonDSLParser";

    private static String getStringAttr(XmlPullParser xmlResParser, String key, String value) {
        String attr = xmlResParser.getAttributeValue(null, key);
        if (TextUtils.isEmpty(attr)) {
            attr = value;
        }
        return attr;
    }

    private static String getStringAttr(XmlPullParser xmlResParser, String key) {
        return xmlResParser.getAttributeValue(null, key);
    }

    /**
     * DSL parsing rules (the skeleton tag may contain multiple clipPath tags) 1. If a clipPath with
     * media parameters is included, the first successfully matched clipPath tag with media is taken
     * as the result 2. If the clipPath with media parameter is not included, the first clipPath tag
     * will be used as the result
     */
    public static JSONObject parse(InputStream is)
            throws XmlPullParserException, JSONException, IOException {
        long parseStart = System.currentTimeMillis();
        JSONObject result = null;
        JSONObject resultTmp = null;
        JSONArray clipPathTmp = null;
        boolean mediaQueryMatch = false;
        boolean hasSkeletonRoot = false;
        // 解析xml
        XmlPullParser xmlResParser = Xml.newPullParser();
        xmlResParser.setInput(is, "utf-8");
        int eventType = xmlResParser.getEventType();
        while (eventType != XmlPullParser.END_DOCUMENT) {
            switch (eventType) {
                case XmlPullParser.START_TAG:
                    switch (xmlResParser.getName()) {
                        case "skeleton":
                            hasSkeletonRoot = true;
                            break;
                        case "clipPath":
                            if (!hasSkeletonRoot) {
                                throw new IllegalArgumentException(
                                        "LOG_SKELETON Invalid DSL: no root tag of skeleton");
                            }
                            resultTmp = new JSONObject();
                            String mediaString = xmlResParser.getAttributeValue(null, "media");
                            MediaList mediaList = null;
                            if (!TextUtils.isEmpty(mediaString)) {
                                // media query
                                mediaList = CSSMediaParser.parseMediaList(mediaString);
                                mediaList.updateMediaPropertyInfo(new MediaPropertyInfoImpl());
                            }
                            if (mediaList == null || mediaList.getResult()) {
                                if (mediaList != null) {
                                    mediaQueryMatch = true;
                                    Log.d(TAG, "LOG_SKELETON media query match : " + mediaString);
                                } else {
                                    // The current clipPath does not contain the media field, and there is already an
                                    // alternative result, skip
                                    if (result != null) {
                                        clipPathTmp = null;
                                        resultTmp = null;
                                        mediaQueryMatch = false;
                                        break;
                                    }
                                }
                                clipPathTmp = new JSONArray();
                                boolean autoHide = Boolean.valueOf(
                                        getStringAttr(xmlResParser, "autoHide", "true"));
                                resultTmp.put("autoHide", autoHide);
                                String bgColor = getStringAttr(xmlResParser, "color");
                                if (!TextUtils.isEmpty(bgColor)) {
                                    resultTmp.put("bgColor", bgColor);
                                }
                                resultTmp.put("clipPath", clipPathTmp);
                                String widthString = getStringAttr(xmlResParser, "width", "");
                                String heightString = getStringAttr(xmlResParser, "height", "");
                                // Skeleton view screen adaptation
                                if (!TextUtils.isEmpty(widthString)) {
                                    int skeletonWidth = Integer.parseInt(widthString);
                                    resultTmp.put("skeletonWidth", skeletonWidth);
                                    if (!TextUtils.isEmpty(heightString)) {
                                        int skeletonHeight = Integer.parseInt(heightString);
                                        resultTmp.put("skeletonHeight", skeletonHeight);
                                    }
                                } else {
                                    throw new IllegalArgumentException(
                                            "LOG_SKELETON Invalid width or not set in the DSL");
                                }
                            } else {
                                Log.i(TAG, "LOG_SKELETON media doesn't match : " + mediaString);
                                // media does not match, skip parsing all child nodes
                                clipPathTmp = null;
                                resultTmp = null;
                                mediaQueryMatch = false;
                            }
                            break;
                        case "ellipse":
                            if (clipPathTmp != null) {
                                JSONObject ellipse = new JSONObject();
                                ellipse.put("tag", "ellipse");
                                String color =
                                        getStringAttr(xmlResParser, "color",
                                                SkeletonSvgView.DEFAULT_ELE_COLOR);
                                ellipse.put("color", color);
                                int cy = Integer.parseInt(getStringAttr(xmlResParser, "cy", "0"));
                                boolean footer =
                                        Boolean.parseBoolean(
                                                getStringAttr(xmlResParser, "footer", "false"));
                                ellipse.put("footer", footer);
                                ellipse.put("cy", cy);
                                ellipse.put("cx",
                                        Integer.parseInt(getStringAttr(xmlResParser, "cx", "0")));
                                ellipse.put("rx",
                                        Integer.parseInt(getStringAttr(xmlResParser, "rx", "0")));
                                ellipse.put("ry",
                                        Integer.parseInt(getStringAttr(xmlResParser, "ry", "0")));
                                clipPathTmp.put(ellipse);
                            }
                            break;
                        case "circle":
                            if (clipPathTmp != null) {
                                JSONObject circle = new JSONObject();
                                circle.put("tag", "circle");
                                String color =
                                        getStringAttr(xmlResParser, "color",
                                                SkeletonSvgView.DEFAULT_ELE_COLOR);
                                circle.put("color", color);
                                int cy = Integer.parseInt(getStringAttr(xmlResParser, "cy", "0"));
                                boolean footer =
                                        Boolean.parseBoolean(
                                                getStringAttr(xmlResParser, "footer", "false"));
                                circle.put("footer", footer);
                                circle.put("cy", cy);
                                circle.put("cx",
                                        Integer.parseInt(getStringAttr(xmlResParser, "cx", "0")));
                                circle.put("r",
                                        Integer.parseInt(getStringAttr(xmlResParser, "r", "0")));
                                clipPathTmp.put(circle);
                            }
                            break;
                        case "rect":
                            if (clipPathTmp != null) {
                                JSONObject rect = new JSONObject();
                                rect.put("tag", "rect");
                                String color =
                                        getStringAttr(xmlResParser, "color",
                                                SkeletonSvgView.DEFAULT_ELE_COLOR);
                                rect.put("color", color);
                                int y = Integer.parseInt(getStringAttr(xmlResParser, "y", "0"));
                                boolean footer =
                                        Boolean.parseBoolean(
                                                getStringAttr(xmlResParser, "footer", "false"));
                                rect.put("footer", footer);
                                String rxStr = getStringAttr(xmlResParser, "rx");
                                String ryStr = getStringAttr(xmlResParser, "ry");
                                if (!TextUtils.isEmpty(rxStr) && !TextUtils.isEmpty(ryStr)) {
                                    rect.put("rx", Integer.parseInt(rxStr));
                                    rect.put("ry", Integer.parseInt(ryStr));
                                } else {
                                    if (!TextUtils.isEmpty(rxStr)) {
                                        rect.put("rx", Integer.parseInt(rxStr));
                                        rect.put("ry", Integer.parseInt(rxStr));
                                    } else if (!TextUtils.isEmpty(ryStr)) {
                                        rect.put("rx", Integer.parseInt(ryStr));
                                        rect.put("ry", Integer.parseInt(ryStr));
                                    } else {
                                        rect.put("rx", 0);
                                        rect.put("ry", 0);
                                    }
                                }
                                rect.put("y", y);
                                rect.put("x",
                                        Integer.parseInt(getStringAttr(xmlResParser, "x", "0")));
                                rect.put("width", Integer.parseInt(
                                        getStringAttr(xmlResParser, "width", "0")));
                                rect.put("height", Integer.parseInt(
                                        getStringAttr(xmlResParser, "height", "0")));
                                clipPathTmp.put(rect);
                            }
                            break;
                        case "full-screen-img":
                            if (clipPathTmp != null) {
                                JSONObject img = new JSONObject();
                                img.put("tag", "full-screen-img");
                                img.put("localSrc", getStringAttr(xmlResParser, "local-src", ""));
                                img.put("fitStyle",
                                        getStringAttr(xmlResParser, "fit-style", "none"));
                                clipPathTmp.put(img);
                            }
                            break;
                        default:
                    }
                    break;
                case XmlPullParser.END_TAG:
                    if ("clipPath".equals(xmlResParser.getName())) {
                        if (clipPathTmp != null) {
                            if (mediaQueryMatch) {
                                // Media query matches successfully and returns the result directly
                                return resultTmp;
                            } else {
                                // This clipPath does not contain the media field, this ordinary clipPath only takes
                                // the first one as the result
                                if (result == null) {
                                    result = resultTmp;
                                }
                            }
                        }
                    }
                    break;
                default:
                    break;
            }
            eventType = xmlResParser.next();
        }
        Log.d(TAG, "LOG_SKELETON parse skFile time = " + (System.currentTimeMillis() - parseStart));
        return result;
    }
}
