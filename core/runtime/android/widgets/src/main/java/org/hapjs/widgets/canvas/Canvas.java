/*
 * Copyright (c) 2021-present, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.widgets.canvas;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Build;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import org.hapjs.bridge.ApplicationContext;
import org.hapjs.bridge.annotation.WidgetAnnotation;
import org.hapjs.common.executors.Executors;
import org.hapjs.common.utils.DisplayUtil;
import org.hapjs.common.utils.FloatUtil;
import org.hapjs.common.utils.IntegerUtil;
import org.hapjs.component.Component;
import org.hapjs.component.Container;
import org.hapjs.component.bridge.RenderEventCallback;
import org.hapjs.component.constants.Attributes;
import org.hapjs.runtime.HapEngine;
import org.hapjs.widgets.canvas.canvas2d.CanvasContextRendering2D;
import org.hapjs.widgets.canvas.canvas2d.CanvasView2D;
import org.hapjs.widgets.canvas.image.CanvasImageHelper;
import org.hapjs.widgets.canvas.webgl.WebGLCanvasView;
import org.hapjs.widgets.view.CanvasViewContainer;

@WidgetAnnotation(
        name = Canvas.WIDGET_NAME,
        methods = {
                Canvas.METHOD_TO_TEMP_FILE_PATH,
                Component.METHOD_GET_BOUNDING_CLIENT_RECT,
                Component.METHOD_FOCUS,
                Component.METHOD_TALKBACK_FOCUS,
                Component.METHOD_TALKBACK_ANNOUNCE
        })
public class Canvas extends Component<CanvasViewContainer> {

    public static final String WIDGET_NAME = "canvas";
    // methods
    protected static final String METHOD_TO_TEMP_FILE_PATH = "toTempFilePath";
    private static final String TAG = "Canvas";
    private static final String CALLBACK_KEY_SUCCESS = "success";
    private static final String CALLBACK_KEY_FAIL = "fail";
    private static final String CALLBACK_KEY_COMPLETE = "complete";

    private static final String ARG_X = "x";
    private static final String ARG_Y = "y";
    private static final String ARG_WIDTH = "width";
    private static final String ARG_HEIGHT = "height";
    private static final String ARG_DEST_WIDTH = "destWidth";
    private static final String ARG_DEST_HEIGHT = "destHeight";
    private static final String ARG_FILETYPE = "fileType";
    private static final String ARG_QUALITY = "quality";

    private static final String SUCCESS_DATA_URI = "uri";
    private static final String SUCCESS_DATA_TEMP_FILE_PATH = "tempFilePath";

    private static final int ERASE_COLOR = 0x00000000;
    private final CanvasManager mCanvasManager;
    private CanvasLifecycle mCanvasLifecycle;
    private String mRefId;
    private CanvasView mCanvasView;
    private View.OnLayoutChangeListener mLayoutChangeListener =
            new View.OnLayoutChangeListener() {

                @Override
                public void onLayoutChange(
                        View v,
                        int left,
                        int top,
                        int right,
                        int bottom,
                        int oldLeft,
                        int oldTop,
                        int oldRight,
                        int oldBottom) {
                    CanvasContext context = mCanvasManager.getContext(getPageId(), mRef);
                    if (context == null) {
                        return;
                    }
                    context.updateSize(right - left, bottom - top);
                }
            };

    public Canvas(
            HapEngine hapEngine,
            Context context,
            Container parent,
            int ref,
            RenderEventCallback callback,
            Map<String, Object> savedState) {
        super(hapEngine, context, parent, ref, callback, savedState);
        mRefId = String.valueOf(ref);

        mCanvasManager = CanvasManager.getInstance();
        mCanvasManager.addCanvas(this);
        mCanvasManager.registerActivityLifecycle(callback);
    }

    @Override
    protected CanvasViewContainer createViewImpl() {
        CanvasViewContainer container = new CanvasViewContainer(mContext);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            container.setForceDarkAllowed(false);
        }
        container.setComponent(this);
        prepareCanvasView();
        addCanvasView(container);
        container.addOnLayoutChangeListener(mLayoutChangeListener);
        return container;
    }

    public void prepareCanvasView() {
        if (mCanvasView != null) {
            return;
        }

        // maybe null
        CanvasContext context = mCanvasManager.getContext(getPageId(), mRef);
        if (context == null) {
            return;
        }

        if (context.is2d()) {
            mCanvasView = new CanvasView2D(mContext.getApplicationContext());
            mCanvasView.setComponent(this);
        } else if (context.isWebGL()) {
            mCanvasView = new WebGLCanvasView(mContext.getApplicationContext());
            mCanvasView.setComponent(this);
        }
    }

    public void addCanvasView(CanvasViewContainer canvasContainer) {
        if (mCanvasView != null && canvasContainer != null) {
            View view = mCanvasView.get();
            ViewGroup parent = (ViewGroup) view.getParent();
            if (parent != null) {
                parent.removeView(view);
            }
            canvasContainer.setCanvasView(view);
        }
    }

    public CanvasView getCanvasView() {
        return mCanvasView;
    }

    @Override
    public void destroy() {
        super.destroy();
        if (mHost != null) {
            mHost.removeOnLayoutChangeListener(mLayoutChangeListener);
        }

        if (mCanvasLifecycle != null) {
            mCanvasLifecycle.destroy(mRefId);
        }

        int pageId = getPageId();
        if (pageId != INVALID_PAGE_ID) {
            mCanvasManager.removeCanvas(this);
        }
    }

    public void setCanvasLifecycle(CanvasLifecycle lifecycle) {
        mCanvasLifecycle = lifecycle;
    }

    @Override
    public void invokeMethod(String methodName, final Map<String, Object> args) {
        switch (methodName) {
            case METHOD_TO_TEMP_FILE_PATH:
                Executors.io().execute(() -> toTempFilePath(args));
                return;
            default:
                break;
        }
        super.invokeMethod(methodName, args);
    }

    public int getCanvasWidth() {
        int width = getWidth();
        if (width > 0 && !IntegerUtil.isUndefined(width)) {
            return width;
        }
        CanvasViewContainer hostView = getHostView();
        if (hostView == null) {
            return 0;
        }

        int measuredWidth = hostView.getMeasuredWidth();

        if (measuredWidth <= 0) {
            return 0;
        }
        return measuredWidth;
    }

    public int getCanvasHeight() {
        int height = getHeight();
        if (height > 0 && !IntegerUtil.isUndefined(height)) {
            return height;
        }
        CanvasViewContainer hostView = getHostView();
        if (hostView == null) {
            return 0;
        }

        int measuredHeight = hostView.getMeasuredHeight();

        if (measuredHeight <= 0) {
            return 0;
        }
        return measuredHeight;
    }

    private void toTempFilePath(Map<String, Object> args) {
        CanvasActionHandler actionHandler = CanvasActionHandler.getInstance();
        boolean handleCommandCompleted =
                actionHandler.isHandleCommandCompleted(getPageId(), getRef());
        if (!handleCommandCompleted) {
            actionHandler.addActionHandleCallback(
                    getPageId(),
                    getRef(),
                    new CanvasActionHandler.OnActionHandleCallback() {
                        @Override
                        public void actionHandleComplete(int pageId, int canvasId) {
                            if (pageId == getPageId() && canvasId == getRef()) {
                                CanvasActionHandler.getInstance().removeActionHandleCallback(this);
                                Executors.io().execute(() -> toTempFilePathInner(args));
                            }
                        }
                    });
        } else {
            toTempFilePathInner(args);
        }
    }

    private void toTempFilePathInner(Map<String, Object> args) {
        int canvasWidth = getCanvasWidth();
        int canvasHeight = getCanvasHeight();
        canvasWidth =
                (int) DisplayUtil.getDesignPxByWidth(canvasWidth, mHapEngine.getDesignWidth());
        canvasHeight =
                (int) DisplayUtil.getDesignPxByWidth(canvasHeight, mHapEngine.getDesignWidth());

        if (canvasWidth <= 0 || canvasHeight <= 0) {
            callbackFail(args);
            callbackComplete(args);
            return;
        }

        CanvasContext context = mCanvasManager.getContext(getPageId(), mRef);
        Bitmap bitmap;
        if (context == null || !context.is2d()) {
            bitmap =
                    CanvasImageHelper.getInstance()
                            .createBitmap(getWidth(), getHeight(), Bitmap.Config.ALPHA_8);
            bitmap.eraseColor(Color.TRANSPARENT);
        } else {
            CanvasContextRendering2D contextRendering2D = (CanvasContextRendering2D) context;
            bitmap = contextRendering2D.dumpBitmap();
            if (bitmap == null) {
                bitmap =
                        CanvasImageHelper.getInstance()
                                .createBitmap(getWidth(), getHeight(), Bitmap.Config.ALPHA_8);
                bitmap.eraseColor(Color.TRANSPARENT);
            }
        }

        int x = Attributes.getInt(mHapEngine, args.get("x"), IntegerUtil.UNDEFINED);
        int y = Attributes.getInt(mHapEngine, args.get("y"), IntegerUtil.UNDEFINED);
        int width = Attributes.getInt(mHapEngine, args.get("width"), 0);
        int height = Attributes.getInt(mHapEngine, args.get("height"), 0);
        int destWidth = Attributes.getInt(mHapEngine, args.get("destWidth"), 0);
        int destHeight = Attributes.getInt(mHapEngine, args.get("destHeight"), 0);
        String fileType = Attributes.getString(args.get("fileType"), "png");
        float quality = Attributes.getFloat(mHapEngine, args.get("quality"), 1);

        if (IntegerUtil.isUndefined(x)) {
            x = 0;
        }

        if (IntegerUtil.isUndefined(y)) {
            y = 0;
        }

        if (x >= bitmap.getWidth() || y >= bitmap.getHeight()) {
            callbackFail(args);
            callbackComplete(args);
            return;
        }

        if (width <= 0) {
            width = bitmap.getWidth();
        }

        if (height <= 0) {
            height = bitmap.getHeight();
        }

        Rect rect = new Rect();
        rect.left = x > 0 ? x : 0;
        rect.top = y > 0 ? y : 0;
        rect.right = Math.min(bitmap.getWidth(), x + width);
        rect.bottom = Math.min(bitmap.getHeight(), y + height);

        if (destWidth <= 0) {
            destWidth = rect.width();
        }

        if (destHeight <= 0) {
            destHeight = rect.height();
        }

        int bitmapWidth = bitmap.getWidth();
        int bitmapHeight = bitmap.getHeight();
        int[] imageData = new int[bitmapWidth * bitmapHeight];
        bitmap.getPixels(imageData, 0, bitmapWidth, 0, 0, bitmapWidth, bitmapHeight);
        bitmap.recycle();

        Bitmap ret =
                CanvasImageHelper.getInstance()
                        .createBitmap(rect.width(), rect.height(), Bitmap.Config.ARGB_8888);
        ret.setPixels(imageData, y * bitmapWidth + x, bitmapWidth, 0, 0, rect.width(),
                rect.height());

        float scaleX = destWidth * 1f / ret.getWidth();
        float scaleY = destHeight * 1f / ret.getHeight();
        if (!FloatUtil.floatsEqual(scaleX, 1) || !FloatUtil.floatsEqual(scaleY, 1)) {
            Matrix matrix = new Matrix();
            matrix.setScale(scaleX, scaleY);
            Bitmap newBitmap =
                    CanvasImageHelper.getInstance()
                            .createBitmap(ret, 0, 0, ret.getWidth(), ret.getHeight(), matrix,
                                    false);
            ret.recycle();
            ret = newBitmap;
        }

        Bitmap.CompressFormat format = Bitmap.CompressFormat.PNG;
        if (!TextUtils.isEmpty(fileType) && TextUtils.equals(fileType.toLowerCase(), "jpg")) {
            format = Bitmap.CompressFormat.JPEG;
            ret = transparentToWhite(ret);
        }

        if (quality <= 0 || quality > 1) {
            quality = 1;
        }
        Uri uri = saveBitmap(ret, format, (int) (quality * 100));
        ret.recycle();
        if (uri != null) {
            if (args.containsKey(CALLBACK_KEY_SUCCESS)) {
                ApplicationContext applicationContext = mHapEngine.getApplicationContext();
                String internalUri = applicationContext.getInternalUri(uri);
                Map<String, Object> params = new HashMap<>();
                params.put(SUCCESS_DATA_URI, internalUri);
                params.put(SUCCESS_DATA_TEMP_FILE_PATH, internalUri);
                mCallback.onJsMethodCallback(getPageId(), (String) args.get(CALLBACK_KEY_SUCCESS),
                        params);
            }
        } else {
            callbackFail(args);
        }

        callbackComplete(args);
    }

    private Bitmap transparentToWhite(Bitmap bitmap) {
        Bitmap ret = bitmap.copy(Bitmap.Config.ARGB_8888, true);
        android.graphics.Canvas canvas = new android.graphics.Canvas(ret);
        canvas.drawColor(Color.WHITE);
        canvas.drawBitmap(bitmap, 0, 0, null);
        return ret;
    }

    private Uri saveBitmap(Bitmap bitmap, Bitmap.CompressFormat format, int quality) {
        FileOutputStream outputStream = null;
        try {
            mHapEngine.getApplicationContext().getFilesDir();
            File dir = mHapEngine.getApplicationContext().getFilesDir();
            File canvasDir = new File(dir, "canvas");
            if (!canvasDir.exists()) {
                canvasDir.mkdirs();
            }
            String suffix = format == Bitmap.CompressFormat.JPEG ? ".jpg" : ".png";
            String fileName = mRefId + "-" + System.currentTimeMillis() + suffix;
            File imgFile = new File(canvasDir, fileName);
            if (imgFile.exists()) {
                return null;
            }

            outputStream = new FileOutputStream(imgFile);
            bitmap.compress(format, quality, outputStream);

            Uri uri = Uri.fromFile(imgFile);
            mContext.sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, uri));

            return uri;
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (outputStream != null) {
                try {
                    outputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return null;
    }

    private void callbackFail(Map<String, Object> args) {
        if (args != null && args.containsKey(CALLBACK_KEY_FAIL)) {
            mCallback.onJsMethodCallback(getPageId(), (String) args.get(CALLBACK_KEY_FAIL));
        }
    }

    private void callbackComplete(Map<String, Object> args) {
        if (args != null && args.containsKey(CALLBACK_KEY_COMPLETE)) {
            mCallback.onJsMethodCallback(getPageId(), (String) args.get(CALLBACK_KEY_COMPLETE));
        }
    }

    public interface CanvasLifecycle {
        void destroy(String refId);
    }
}
