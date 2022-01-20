/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.debugger.card;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.NinePatchDrawable;
import android.os.Environment;
import android.util.Log;
import java.io.File;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class CardBackgroundHelper {
    private static final String TAG = "CardBackgroundHelper";

    // bg should be a compiled .9.png
    private static final File BG_FILE = new File(Environment.getExternalStorageDirectory(), "quickapp/card_bg.9.png");

    public static Drawable getBackground(Context context) {
        if (!BG_FILE.isFile()) {
            return null;
        }

        Bitmap bitmap = BitmapFactory.decodeFile(BG_FILE.getAbsolutePath());
        if (bitmap == null) {
            Log.e(TAG, "decode card bg failed");
            return null;
        }

        byte[] chunk = bitmap.getNinePatchChunk();
        if (chunk != null) {
            Rect padding = getPadding(chunk);
            return new NinePatchDrawable(context.getResources(), bitmap, chunk, padding, null);
        } else {
            Log.w(TAG, "no trunk in bitmap");
            return new BitmapDrawable(context.getResources(), bitmap);
        }
    }

    private static Rect getPadding(byte[] chunk) {
        ByteBuffer byteBuffer =
                ByteBuffer.wrap(chunk).order(ByteOrder.nativeOrder());
        // skip data which is not needed.
        byteBuffer.get();
        byteBuffer.get();
        byteBuffer.get();
        byteBuffer.get();
        byteBuffer.getInt();
        byteBuffer.getInt();

        int left = byteBuffer.getInt();
        int right = byteBuffer.getInt();
        int top = byteBuffer.getInt();
        int bottom = byteBuffer.getInt();

        Rect rect = new Rect(left, top, right, bottom);
        return rect;
    }
}
