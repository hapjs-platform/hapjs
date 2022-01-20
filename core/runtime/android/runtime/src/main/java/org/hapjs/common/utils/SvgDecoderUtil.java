/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.common.utils;

import android.content.Context;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.PictureDrawable;
import android.util.DisplayMetrics;
import android.util.Log;
import com.caverock.androidsvg.SVG;
import com.facebook.imageformat.ImageFormat;
import com.facebook.imageformat.ImageFormatCheckerUtils;
import com.facebook.imagepipeline.common.ImageDecodeOptions;
import com.facebook.imagepipeline.decoder.ImageDecoder;
import com.facebook.imagepipeline.drawable.DrawableFactory;
import com.facebook.imagepipeline.image.CloseableImage;
import com.facebook.imagepipeline.image.EncodedImage;
import com.facebook.imagepipeline.image.QualityInfo;

/**
 * Defines all classes required to decode and render SVG images.
 */
public class SvgDecoderUtil {

    public static final ImageFormat SVG_FORMAT = new ImageFormat("SVG_FORMAT", "svg");

    private static final String HEADER_TAG = "<svg";
    private static final byte[][] POSSIBLE_HEADER_TAGS = {
            ImageFormatCheckerUtils.asciiBytes("<?xml"), ImageFormatCheckerUtils.asciiBytes("<!--")
    };
    private static final int SIZE = 256;
    private static Context sAppContext;

    public static void setAppContext(Context context) {
        SvgDecoderUtil.sAppContext = context.getApplicationContext();
    }

    public static class SvgFormatChecker implements ImageFormat.FormatChecker {

        public static final byte[] HEADER = ImageFormatCheckerUtils.asciiBytes(HEADER_TAG);

        @Override
        public int getHeaderSize() {
            return SIZE;
        }

        @Override
        public ImageFormat determineFormat(byte[] headerBytes, int headerSize) {
            if (headerSize < getHeaderSize()) {
                return null;
            }
            if (ImageFormatCheckerUtils.startsWithPattern(headerBytes, HEADER)) {
                return SVG_FORMAT;
            }
            for (byte[] possibleHeaderTag : POSSIBLE_HEADER_TAGS) {
                if (ImageFormatCheckerUtils.startsWithPattern(headerBytes, possibleHeaderTag)
                        && ImageFormatCheckerUtils.indexOfPattern(
                        headerBytes, headerBytes.length, HEADER, HEADER.length)
                        > -1) {
                    return SVG_FORMAT;
                }
            }
            return null;
        }
    }

    public static class CloseableSvgImage extends CloseableImage {

        private final SVG mSvg;

        private boolean mClosed = false;

        public CloseableSvgImage(SVG svg) {
            mSvg = svg;
        }

        public SVG getSvg() {
            return mSvg;
        }

        @Override
        public int getSizeInBytes() {
            return 0;
        }

        @Override
        public void close() {
            mClosed = true;
        }

        @Override
        public boolean isClosed() {
            return mClosed;
        }

        @Override
        public int getWidth() {
            return (int) mSvg.getDocumentWidth();
        }

        @Override
        public int getHeight() {
            return (int) mSvg.getDocumentHeight();
        }
    }

    /**
     * Decodes a SVG_FORMAT image
     */
    public static class SvgDecoder implements ImageDecoder {

        private static final String TAG = "SvgDecoder";

        @Override
        public CloseableImage decode(
                EncodedImage encodedImage,
                int length,
                QualityInfo qualityInfo,
                ImageDecodeOptions options) {
            try {
                SVG svg = SVG.getFromInputStream(encodedImage.getInputStream());
                DisplayMetrics dm = sAppContext.getResources().getDisplayMetrics();
                svg.setRenderDPI(dm.densityDpi);
                return new CloseableSvgImage(svg);
            } catch (Throwable e) {
                Log.e(TAG, "svg decoder error", e);
            }
            return null;
        }
    }

    /**
     * SVG drawable factory that creates {@link PictureDrawable}s for SVG images.
     */
    public static class SvgDrawableFactory implements DrawableFactory {

        @Override
        public boolean supportsImageType(CloseableImage image) {
            return image instanceof CloseableSvgImage;
        }

        @Override
        public Drawable createDrawable(CloseableImage image) {
            return new SvgPictureDrawable(((CloseableSvgImage) image).getSvg());
        }
    }

    public static class SvgPictureDrawable extends PictureDrawable {

        private final SVG mSvg;
        private int width = 0;
        private int height = 0;

        public SvgPictureDrawable(SVG svg) {
            super(null);
            mSvg = svg;
        }

        @Override
        protected void onBoundsChange(Rect bounds) {
            super.onBoundsChange(bounds);
            /**
             * SVG显示策略 1、svg有宽高，组件有宽高时，使用svg宽高； 2、svg没宽高，组件有宽高时，优先使用viewBox的宽高，否则使用组件宽高；
             * 3、svg有宽高，组件没宽高时，使用svg宽高，组件也适配svg宽高； 4、svg没宽高，组件也没宽高时，svg和组件都默认512*512宽高；
             * 5、svg只有一个宽或高，根据viewBox计算另一个宽或高，如果无法计算，根据组件宽高比计算，如果还无法计算，取默认值512；
             * 6、svg没有设置viewBox时，使用宽高作为viewBox的宽高。
             */
            final int boundsWidth = bounds.width();
            final int boundsHeight = bounds.height();
            if (boundsWidth != width
                    || boundsHeight != height
                    || width <= 0
                    || height <= 0
                    || boundsWidth <= 1
                    || boundsHeight <= 1) {
                width = (int) mSvg.getDocumentWidth();
                height = (int) mSvg.getDocumentHeight();
                RectF rectF = mSvg.getDocumentViewBox();
                if (width <= 0 && height <= 0) {
                    if (rectF != null && rectF.width() > 0 && rectF.height() > 0) {
                        width = (int) rectF.width();
                        height = (int) rectF.height();
                    } else if (boundsWidth > 1 && boundsHeight > 1) {
                        width = boundsWidth;
                        height = boundsHeight;
                    } else {
                        width = 512;
                        height = 512;
                    }
                } else if (width > 0 && height <= 0) {
                    if (rectF != null && rectF.width() > 0 && rectF.height() > 0) {
                        height = (int) (width * rectF.height() / rectF.width());
                    } else if (boundsWidth > 1 && boundsHeight > 1) {
                        height = width * boundsHeight / boundsWidth;
                    } else {
                        height = 512;
                    }
                } else if (height > 0 && width <= 0) {
                    if (rectF != null && rectF.width() > 0 && rectF.height() > 0) {
                        width = (int) (height * rectF.width() / rectF.height());
                    } else if (boundsWidth > 1 && boundsHeight > 1) {
                        width = height * boundsWidth / boundsHeight;
                    } else {
                        width = 512;
                    }
                }

                mSvg.setDocumentWidth(width);
                mSvg.setDocumentHeight(height);
                setPicture(mSvg.renderToPicture(width, height));
            }
        }
    }
}
