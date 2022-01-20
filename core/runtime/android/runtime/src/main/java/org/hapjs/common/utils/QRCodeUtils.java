/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.common.utils;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;
import androidx.annotation.ColorInt;
import androidx.annotation.Nullable;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.Hashtable;

public class QRCodeUtils {
    private static final String TAG = "QRCodeUtils";

    private static final String DEFAULT_CHARACTER_SET = "UTF-8";
    private static final float DEFAULT_SCALE_RATE = 0.2667f;
    private static final int DEFAULT_BITMAP_FILE_QUALITY = 100;

    public static Bitmap createQRCodeBitmapWithLogo(
            String content, int width, int height, Bitmap logoBitmap) {
        Bitmap bitmap = createQRCodeBitmap(content, width, height);
        return addCenterLogo(bitmap, logoBitmap, DEFAULT_SCALE_RATE);
    }

    public static Bitmap createQRCodeBitmap(String content, int width, int height) {
        return createQRCodeBitmap(
                content,
                width,
                height,
                DEFAULT_CHARACTER_SET,
                ErrorCorrectionLevel.M,
                0,
                Color.BLACK,
                Color.WHITE);
    }

    public static Bitmap createQRCodeBitmap(
            String content,
            int width,
            int height,
            @Nullable String characterSet,
            @Nullable ErrorCorrectionLevel errorCorrection,
            int margin,
            @ColorInt int colorBlack,
            @ColorInt int colorWhite) {
        if (TextUtils.isEmpty(content)) {
            return null;
        }

        if (width < 0 || height < 0) {
            return null;
        }

        try {
            Hashtable<EncodeHintType, Object> hints = new Hashtable<>();

            if (!TextUtils.isEmpty(characterSet)) {
                hints.put(EncodeHintType.CHARACTER_SET, characterSet);
            }

            if (errorCorrection != null) {
                hints.put(EncodeHintType.ERROR_CORRECTION, errorCorrection);
            }

            hints.put(EncodeHintType.MARGIN, margin);
            BitMatrix bitMatrix =
                    new QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, width, height, hints);

            int[] pixels = new int[width * height];
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    if (bitMatrix.get(x, y)) {
                        pixels[y * width + x] = colorBlack;
                    } else {
                        pixels[y * width + x] = colorWhite;
                    }
                }
            }

            Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            bitmap.setPixels(pixels, 0, width, 0, 0, width, height);
            return bitmap;
        } catch (WriterException e) {
            Log.e(TAG, "failed to createQRCodeBitmap!", e);
        }

        return null;
    }

    public static Bitmap addCenterLogo(Bitmap src, Bitmap logo, float scaleRate) {
        if (src == null) {
            return null;
        }
        if (logo == null) {
            return src;
        }

        int srcWidth = src.getWidth();
        int srcHeight = src.getHeight();
        int logoWidth = logo.getWidth();
        int logoHeight = logo.getHeight();
        if (srcWidth == 0 || srcHeight == 0) {
            return null;
        }
        if (logoWidth == 0 || logoHeight == 0) {
            return src;
        }

        float scaleFactor = srcWidth * scaleRate / logoWidth;
        Bitmap bitmap = Bitmap.createBitmap(srcWidth, srcHeight, Bitmap.Config.ARGB_8888);
        try {
            Canvas canvas = new Canvas(bitmap);
            canvas.drawBitmap(src, 0, 0, null);
            canvas.scale(scaleFactor, scaleFactor, srcWidth / 2, srcHeight / 2);
            canvas.drawBitmap(logo, (srcWidth - logoWidth) / 2, (srcHeight - logoHeight) / 2, null);
            canvas.save();
            canvas.restore();
        } catch (Exception e) {
            bitmap = null;
            Log.e(TAG, "failed to addCenterLogo!", e);
        }
        return bitmap;
    }

    public static Uri saveBitmapToFile(File saveDir, Bitmap bitmap) {
        if (bitmap == null) {
            Log.e(TAG, "bitmap is null");
            return null;
        }

        if (saveDir == null) {
            Log.e(TAG, "saveDir is null");
            return null;
        }
        FileOutputStream outputStream = null;
        try {
            File qrCodeDir = new File(saveDir, "QuickAppCode");
            if (!qrCodeDir.exists()) {
                qrCodeDir.mkdirs();
            }
            String fileName = "code" + "_" + System.currentTimeMillis() + ".png";
            File imgFile = new File(qrCodeDir, fileName);
            outputStream = new FileOutputStream(imgFile);
            bitmap.compress(Bitmap.CompressFormat.PNG, DEFAULT_BITMAP_FILE_QUALITY, outputStream);
            Uri uri = Uri.fromFile(imgFile);
            return uri;
        } catch (FileNotFoundException e) {
            Log.e(TAG, "failed to saveBitmap!", e);
        } finally {
            FileUtils.closeQuietly(outputStream);
        }
        return null;
    }
}
