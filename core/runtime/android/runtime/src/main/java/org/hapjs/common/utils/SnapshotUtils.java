/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.common.utils;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import org.hapjs.bridge.HybridView;

public class SnapshotUtils {

    private static final String TAG = "SnapshotUtils";

    public static Bitmap createSnapshot(View host, Canvas canvas, int bgColor) {
        if (host == null || canvas == null || host.getWidth() <= 0 || host.getHeight() <= 0) {
            return null;
        }
        int w = host.getWidth();
        int h = host.getHeight();
        Bitmap bitmap = null;
        try {
            bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        } catch (OutOfMemoryError e) {
            Log.e(TAG, "create bitmap error: " + e.getMessage());
            return null;
        }

        bitmap.eraseColor(bgColor);
        canvas.setBitmap(bitmap);
        canvas.translate(-host.getScrollX(), -host.getScrollY());
        host.draw(canvas);
        canvas.setBitmap(null);
        return bitmap;
    }

    public static Uri saveSnapshot(
            HybridView hybridView, Bitmap snapshot, int ref, String fileType, double quality) {
        if (hybridView == null || snapshot == null) {
            return null;
        }
        FileOutputStream fos = null;
        File snapshotFile = createSnapshotFile(hybridView, ref, fileType);

        Bitmap.CompressFormat format =
                TextUtils.equals("jpg", fileType.toLowerCase())
                        ? Bitmap.CompressFormat.JPEG
                        : Bitmap.CompressFormat.PNG;
        int transformedQuality = 100;
        if (quality > 0 && Double.compare(quality, 1.0) < 0) {
            transformedQuality = (int) (quality * 100);
        }
        try {
            fos = new FileOutputStream(snapshotFile);
            snapshot.compress(format, transformedQuality, fos);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } finally {
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return Uri.fromFile(snapshotFile);
    }

    private static File createSnapshotFile(HybridView hybridView, int ref, String fileType) {
        File dir = hybridView.getHybridManager().getApplicationContext().getCacheDir();
        String realFileType = TextUtils.equals("jpg", fileType.toLowerCase()) ? "jpg" : "png";
        String fileName = ref + "-" + System.currentTimeMillis() + "." + realFileType;
        return new File(dir, fileName);
    }
}
