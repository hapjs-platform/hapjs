/*
 * Copyright (c) 2021-present, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.widgets.text;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LevelListDrawable;
import android.os.AsyncTask;
import android.text.Html;
import android.text.TextUtils;
import android.util.Log;
import android.widget.FrameLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.hapjs.bridge.annotation.TypeAnnotation;
import org.hapjs.bridge.annotation.WidgetAnnotation;
import org.hapjs.common.utils.DisplayUtil;
import org.hapjs.component.Component;
import org.hapjs.component.Container;
import org.hapjs.component.bridge.RenderEventCallback;
import org.hapjs.component.constants.Attributes;
import org.hapjs.component.view.GestureScrollerView;
import org.hapjs.runtime.HapEngine;

@WidgetAnnotation(
        name = AbstractText.WIDGET_NAME,
        methods = {
                Component.METHOD_ANIMATE,
                Component.METHOD_GET_BOUNDING_CLIENT_RECT,
                Component.METHOD_TO_TEMP_FILE_PATH,
                Component.METHOD_FOCUS,
                Component.METHOD_TALKBACK_FOCUS,
                Component.METHOD_TALKBACK_ANNOUNCE
        },
        types = {@TypeAnnotation(name = HtmlText.TYPE_HTML)})
public class HtmlText extends AbstractText<ScrollView> {
    protected static final String TYPE_HTML = "html";
    private static final String TAG = "HtmlText";
    private static final Pattern IMG_PATTERN =
            Pattern.compile("<img[^>]+src\\s*=\\s*['\"]([^'\"]+)['\"][^>]*>");
    private TextView mTextView;
    private String mText;
    private Html.ImageGetter mImageGetter;
    private Drawable mPlaceholder;
    private int mDefaultLeft;
    private int mDefaultWidth;
    private Map<String, Dimension> mBoundMap;

    public HtmlText(
            HapEngine hapEngine,
            Context context,
            Container parent,
            int ref,
            RenderEventCallback callback,
            Map<String, Object> savedState) {
        super(hapEngine, context, parent, ref, callback, savedState);
    }

    @Override
    protected android.widget.ScrollView createViewImpl() {
        mPlaceholder = new ColorDrawable(Color.WHITE);
        mPlaceholder.setBounds(0, 0, 400, 300);

        mTextView = new TextView(mContext);
        FrameLayout.LayoutParams lp =
                new FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.MATCH_PARENT);
        mTextView.setLayoutParams(lp);

        ScrollView scrollView = new GestureScrollerView(mContext);
        scrollView.addView(mTextView);
        return scrollView;
    }

    @Override
    protected boolean setAttribute(String key, Object attribute) {
        switch (key) {
            case Attributes.Style.VALUE:
                String text = Attributes.getString(attribute, "");
                setText(text);
                return true;
            default:
                break;
        }
        return super.setAttribute(key, attribute);
    }

    public void setText(String text) {
        if (text == null) {
            return;
        }
        mText = text;

        if (mHost == null || mTextView == null) {
            return;
        }

        if (mImageGetter == null) {
            mImageGetter =
                    new Html.ImageGetter() {
                        @Override
                        public Drawable getDrawable(String source) {
                            LevelListDrawable d = new LevelListDrawable();
                            d.addLevel(0, 0, mPlaceholder);
                            d.setBounds(
                                    0, 0, mPlaceholder.getIntrinsicWidth(),
                                    mPlaceholder.getIntrinsicHeight());

                            new LoadImage().execute(source, d);

                            return d;
                        }
                    };
        }
        mTextView.setText(Html.fromHtml(mText, mImageGetter, null));
    }

    private Map<String, Dimension> getBoundsMap() {
        if (TextUtils.isEmpty(mText)) {
            return null;
        }

        Map<String, Dimension> boundMap = new HashMap<>();

        Matcher m = IMG_PATTERN.matcher(mText);
        while (m.find()) {
            String matched = m.group(0);

            int sourceIndex = matched.indexOf(" src=");
            if (sourceIndex == -1) {
                continue;
            }
            int sourceStartIndex = sourceIndex + 5;
            int sourceEndIndex = matched.indexOf(" ", sourceStartIndex);
            if (sourceEndIndex == -1) {
                sourceEndIndex = matched.indexOf(">", sourceStartIndex);
            }
            if (sourceEndIndex == -1) {
                continue;
            }
            String source =
                    matched.substring(sourceStartIndex, sourceEndIndex).replace("'", "")
                            .replace("\"", "");
            if (TextUtils.isEmpty(source)) {
                continue;
            }

            Dimension dimension = new Dimension();
            boundMap.put(source, dimension);

            int widthIndex = matched.indexOf("width=");
            if (widthIndex != -1) {
                int widthStartIndex = widthIndex + 6;
                int widthEndIndex = matched.indexOf(" ", widthStartIndex);
                if (widthEndIndex == -1) {
                    widthEndIndex = matched.indexOf(">", widthStartIndex);
                }
                if (widthEndIndex != -1) {
                    String widthStr =
                            matched.substring(widthStartIndex, widthEndIndex).replace("'", "")
                                    .replace("\"", "");
                    if (!TextUtils.isEmpty(widthStr)) {
                        dimension.mWidth = getWidth(widthStr, -1);
                    }
                }
            }

            int heightIndex = matched.indexOf("height=");
            if (heightIndex != -1) {
                int heightStartIndex = heightIndex + 7;
                int heightEndIndex = matched.indexOf(" ", heightStartIndex);
                if (heightEndIndex == -1) {
                    heightEndIndex = matched.indexOf(">", heightStartIndex);
                }
                if (heightEndIndex != -1) {
                    String heightStr =
                            matched
                                    .substring(heightStartIndex, heightEndIndex)
                                    .replace("'", "")
                                    .replace("\"", "");
                    if (!TextUtils.isEmpty(heightStr)) {
                        dimension.mHeight = getHeight(heightStr, -1);
                    }
                }
            }
        }

        return boundMap;
    }

    private int getWidth(Object value, int defValue) {
        if (value == null || "".equals(value)) {
            return defValue;
        }
        String temp = value.toString().trim();
        if (temp.endsWith("%")) {
            temp = temp.substring(0, temp.indexOf("%"));
            float percent = Attributes.getFloat(mHapEngine, temp) / 100f;
            return Attributes.getInt(mHapEngine, DisplayUtil.getScreenWidth(mContext) * percent);
        }

        return Attributes.getInt(mHapEngine, value, defValue);
    }

    private int getHeight(Object value, int defValue) {
        if (value == null || "".equals(value)) {
            return defValue;
        }
        String temp = value.toString().trim();
        if (temp.endsWith("%")) {
            temp = temp.substring(0, temp.indexOf("%"));
            float percent = Attributes.getFloat(mHapEngine, temp) / 100f;
            return Attributes.getInt(mHapEngine, DisplayUtil.getScreenHeight(mContext) * percent);
        }

        return Attributes.getInt(mHapEngine, value, defValue);
    }

    class LoadImage extends AsyncTask<Object, Void, Bitmap> {

        private String mSource;
        private LevelListDrawable mDrawable;

        @Override
        protected Bitmap doInBackground(Object... params) {
            if (mBoundMap == null) {
                mBoundMap = getBoundsMap();
            }

            mSource = (String) params[0];
            mDrawable = (LevelListDrawable) params[1];
            InputStream is = null;
            try {
                is = new URL(mSource).openStream();
                return BitmapFactory.decodeStream(is);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {

                if (is != null) {
                    try {
                        is.close();
                    } catch (IOException e) {
                        Log.e(TAG, e.toString());
                    }
                }
            }
            return null;
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            if (bitmap != null && mHost != null) {
                BitmapDrawable d = new BitmapDrawable(bitmap);
                mDrawable.addLevel(1, 1, d);

                int hostWidth = mHost.getWidth();
                Dimension dimension = mBoundMap.get(mSource);
                if (dimension == null) {
                    return;
                }
                if (dimension.mWidth > 0 && dimension.mHeight == -1) {
                    int width = dimension.mWidth;
                    int left = (hostWidth - width) / 2;
                    mDrawable.setBounds(
                            left,
                            0,
                            left + width,
                            Math.round(bitmap.getHeight() * ((float) width / bitmap.getWidth())));
                } else if (dimension.mHeight > 0 && dimension.mWidth == -1) {
                    int width =
                            Math.round(bitmap.getWidth()
                                    * ((float) dimension.mHeight / bitmap.getHeight()));
                    int left = (hostWidth - width) / 2;
                    mDrawable.setBounds(left, 0, left + width, dimension.mHeight);
                } else if (dimension.mWidth > 0 && dimension.mHeight > 0) {
                    int left = (hostWidth - dimension.mWidth) / 2;
                    mDrawable.setBounds(left, 0, left + dimension.mWidth, dimension.mHeight);
                } else {
                    mDefaultLeft = Math.round(hostWidth * 0.05f);
                    mDefaultWidth = Math.round(hostWidth * 0.9f);
                    mDrawable.setBounds(
                            mDefaultLeft,
                            0,
                            mDefaultLeft + mDefaultWidth,
                            Math.round(bitmap.getHeight()
                                    * ((float) mDefaultWidth / bitmap.getWidth())));
                }

                mDrawable.setLevel(1);

                CharSequence t = mTextView.getText();
                mTextView.setText(t);
            }
        }
    }

    class Dimension {
        private int mWidth;
        private int mHeight;

        private Dimension() {
            mWidth = -1;
            mHeight = -1;
        }

        @Override
        public String toString() {
            return "width:" + mWidth + ",height:" + mHeight;
        }
    }
}
