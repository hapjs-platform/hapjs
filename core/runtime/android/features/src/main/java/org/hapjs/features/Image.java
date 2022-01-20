/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.features;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapRegionDecoder;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.net.Uri;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;
import androidx.exifinterface.media.ExifInterface;
import com.theartofdev.edmodo.cropper.CropImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.hapjs.bridge.FeatureExtension;
import org.hapjs.bridge.LifecycleListener;
import org.hapjs.bridge.NativeInterface;
import org.hapjs.bridge.Request;
import org.hapjs.bridge.Response;
import org.hapjs.bridge.annotation.ActionAnnotation;
import org.hapjs.bridge.annotation.FeatureExtensionAnnotation;
import org.hapjs.common.utils.FileHelper;
import org.hapjs.common.utils.FileUtils;
import org.hapjs.render.jsruntime.serialize.SerializeException;
import org.hapjs.render.jsruntime.serialize.SerializeObject;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

@FeatureExtensionAnnotation(
        name = Image.FEATURE_NAME,
        actions = {
                @ActionAnnotation(name = Image.ACTION_COMPRESS, mode = FeatureExtension.Mode.ASYNC),
                @ActionAnnotation(name = Image.ACTION_GET_INFO, mode = FeatureExtension.Mode.ASYNC),
                @ActionAnnotation(name = Image.ACTION_SET_EXIF_ATTRS, mode = FeatureExtension.Mode.ASYNC),
                @ActionAnnotation(name = Image.ACTION_GET_EXIF_ATTRS, mode = FeatureExtension.Mode.ASYNC),
                @ActionAnnotation(name = Image.ACTION_EDIT, mode = FeatureExtension.Mode.ASYNC),
                @ActionAnnotation(name = Image.ACTION_APPLY_OPERATIONS, mode = FeatureExtension.Mode.ASYNC),
                @ActionAnnotation(name = Image.ACTION_COMPRESS_IMAGE, mode = FeatureExtension.Mode.ASYNC),
                @ActionAnnotation(name = Image.ACTION_GET_IMAGE_INFO, mode = FeatureExtension.Mode.ASYNC),
                @ActionAnnotation(name = Image.ACTION_EDIT_IMAGE, mode = FeatureExtension.Mode.ASYNC)
        })
public class Image extends FeatureExtension {

    protected static final String FEATURE_NAME = "system.image";
    /**
     * @deprecated Use {@link #ACTION_COMPRESS} instead
     */
    @Deprecated
    protected static final String ACTION_COMPRESS_IMAGE = "compressImage";
    /**
     * @deprecated Use {@link #ACTION_GET_INFO} instead
     */
    @Deprecated
    protected static final String ACTION_GET_IMAGE_INFO = "getImageInfo";
    /**
     * @deprecated Use {@link #ACTION_EDIT} instead
     */
    @Deprecated
    protected static final String ACTION_EDIT_IMAGE = "editImage";
    protected static final String ACTION_COMPRESS = "compress";
    protected static final String ACTION_GET_INFO = "getInfo";
    protected static final String ACTION_SET_EXIF_ATTRS = "setExifAttributes";
    protected static final String ACTION_GET_EXIF_ATTRS = "getExifAttributes";
    protected static final String ACTION_EDIT = "edit";
    protected static final String ACTION_APPLY_OPERATIONS = "applyOperations";
    protected static final String ACTION_CROP = "crop";
    protected static final String ACTION_ROTATE = "rotate";
    protected static final String ACTION_SCALE = "scale";
    protected static final String PARAM_URI = "uri";
    protected static final String PARAM_QUALITY = "quality";
    protected static final String PARAM_RATIO = "ratio";
    protected static final String PARAM_FORMAT = "format";
    protected static final String PARAM_OPERATIONS = "operations";
    protected static final String PARAM_ACTION = "action";
    protected static final String PARAM_CROP_X = "x";
    protected static final String PARAM_CROP_Y = "y";
    protected static final String PARAM_CROP_WIDTH = "width";
    protected static final String PARAM_CROP_HEIGHT = "height";
    protected static final String PARAM_ROTATE_DEGREE = "degree";
    protected static final String PARAM_SCALE_X = "scaleX";
    protected static final String PARAM_SCALE_Y = "scaleY";
    protected static final String PARAM_ASPECT_RATIO_X = "aspectRatioX";
    protected static final String PARAM_ASPECT_RATIO_Y = "aspectRatioY";
    protected static final String PARAM_ATTRIBUTES = "attributes";
    protected static final String RESULT_URI = "uri";
    protected static final String RESULT_WIDTH = "width";
    protected static final String RESULT_HEIGHT = "height";
    protected static final String RESULT_SIZE = "size";
    protected static final String RESULT_ATTRIBUTES = "attributes";
    private static final String TAG = "Image";
    private static final String IMAGE_FORMAT_JPEG = "jpeg";
    private static final String IMAGE_FORMAT_PNG = "png";
    private static final String IMAGE_FORMAT_WEBP = "webp";

    private static final Map<String, Bitmap.CompressFormat> FORMAT_MAP = new HashMap<>();
    private static final Map<String, String> EXTENSION_MAP = new HashMap<>();

    static {
        FORMAT_MAP.put(IMAGE_FORMAT_JPEG, Bitmap.CompressFormat.JPEG);
        FORMAT_MAP.put(IMAGE_FORMAT_PNG, Bitmap.CompressFormat.PNG);
        FORMAT_MAP.put(IMAGE_FORMAT_WEBP, Bitmap.CompressFormat.WEBP);
    }

    static {
        EXTENSION_MAP.put(IMAGE_FORMAT_JPEG, ".jpg");
        EXTENSION_MAP.put(IMAGE_FORMAT_PNG, ".png");
        EXTENSION_MAP.put(IMAGE_FORMAT_WEBP, ".webp");
    }

    @Override
    public String getName() {
        return FEATURE_NAME;
    }

    @Override
    protected Response invokeInner(Request request) throws Exception {
        String action = request.getAction();
        Uri underlyingUri = getUnderlyingUri(request);
        if (underlyingUri == null) {
            return null;
        }
        if (ACTION_COMPRESS_IMAGE.equals(action) || ACTION_COMPRESS.equals(action)) {
            compressImage(request, underlyingUri);
        } else if (ACTION_GET_IMAGE_INFO.equals(action) || ACTION_GET_INFO.equals(action)) {
            getImageInfo(request, underlyingUri);
        } else if (ACTION_GET_EXIF_ATTRS.equals(action)) {
            getExifInfo(request, underlyingUri);
        } else if (ACTION_SET_EXIF_ATTRS.equals(action)) {
            setExifInfo(request, underlyingUri);
        } else if (ACTION_EDIT_IMAGE.equals(action) || ACTION_EDIT.equals(action)) {
            editImage(request, underlyingUri);
        } else if (ACTION_APPLY_OPERATIONS.equals(action)) {
            applyOperations(request, underlyingUri);
        }
        return null;
    }

    private void compressImage(Request request, Uri underlyingUri) throws JSONException {
        JSONObject jsonParams = request.getJSONParams();
        double quality = jsonParams.optDouble(PARAM_QUALITY, 75d);
        double ratio = jsonParams.optDouble(PARAM_RATIO, 1d);
        String format = jsonParams.optString(PARAM_FORMAT, IMAGE_FORMAT_JPEG).toLowerCase();

        quality = quality < 0 ? 0 : (quality > 100 ? 100 : quality);

        if (ratio <= 0) {
            Response response =
                    new Response(Response.CODE_ILLEGAL_ARGUMENT,
                            "ratio: " + ratio + "must greater than 0");
            request.getCallback().callback(response);
            return;
        }

        Bitmap.CompressFormat compressFormat = FORMAT_MAP.get(format);
        if (compressFormat == null) {
            Response response =
                    new Response(Response.CODE_ILLEGAL_ARGUMENT, "unknown format: " + format);
            request.getCallback().callback(response);
            return;
        }

        Activity activity = request.getNativeInterface().getActivity();
        Bitmap bitmap;
        BitmapFactory.Options options = obtainOptions(request, underlyingUri);
        if (options == null) {
            return;
        }
        ExifInterface exifInterface = null;
        double expectWidth = options.outWidth / ratio;
        double expectHeight = options.outHeight / ratio;
        try (InputStream in = activity.getContentResolver().openInputStream(underlyingUri)) {
            exifInterface = new ExifInterface(in);
        } catch (IOException e) {
            Response response =
                    getExceptionResponse(request.getAction(), e, Response.CODE_IO_ERROR);
            request.getCallback().callback(response);
            return;
        }
        Bitmap dstBitmap;
        try (InputStream in = activity.getContentResolver().openInputStream(underlyingUri)) {
            options.inJustDecodeBounds = false;
            options.inSampleSize =
                    ratio <= 1 ? 1 :
                            (int) Math.pow(2, (int) Math.floor(Math.log(ratio) / Math.log(2)));
            bitmap = BitmapFactory.decodeStream(in, null, options);
            dstBitmap = scaleAndRotateBitmap(bitmap, exifInterface, expectWidth, expectHeight);
        } catch (IOException e) {
            Response response =
                    getExceptionResponse(request.getAction(), e, Response.CODE_IO_ERROR);
            request.getCallback().callback(response);
            return;
        } catch (OutOfMemoryError e) {
            Response response = new Response(Response.CODE_OOM_ERROR, e.getMessage());
            request.getCallback().callback(response);
            return;
        }

        OutputStream out = null;
        File tmpFile;
        try {
            String extension = EXTENSION_MAP.get(format);
            tmpFile = request.getApplicationContext().createTempFile("compress", extension);
            out = new FileOutputStream(tmpFile);
            dstBitmap.compress(compressFormat, (int) Math.round(quality), out);
        } catch (IOException e) {
            Response response =
                    getExceptionResponse(request.getAction(), e, Response.CODE_IO_ERROR);
            request.getCallback().callback(response);
            return;
        } finally {
            FileUtils.closeQuietly(out);
            dstBitmap.recycle();
            bitmap.recycle();
        }

        String resultUri = request.getApplicationContext().getInternalUri(tmpFile);
        Response response = new Response(makeResult(resultUri));
        request.getCallback().callback(response);
    }

    private void getImageInfo(Request request, Uri underlyingUri) throws JSONException {
        BitmapFactory.Options options = obtainOptions(request, underlyingUri);
        if (options == null) {
            return;
        }
        int width = options.outWidth;
        int height = options.outHeight;
        long size = getFileLength(request.getNativeInterface().getActivity(), underlyingUri);

        JSONObject jsonParams = request.getJSONParams();
        String uri = jsonParams.optString(PARAM_URI);
        JSONObject result = makeResult(uri);
        result.put(RESULT_WIDTH, width);
        result.put(RESULT_HEIGHT, height);
        result.put(RESULT_SIZE, size);

        Response response = new Response(result);
        request.getCallback().callback(response);
    }

    private void setExifInfo(Request request, Uri underlyingUri) throws SerializeException {
        Context context = request.getNativeInterface().getActivity();

        String filePath = null;
        if (ContentResolver.SCHEME_CONTENT.equals(underlyingUri.getScheme())) {
            filePath = FileHelper.getFileFromContentUri(context, underlyingUri);
        } else if (ContentResolver.SCHEME_FILE.equals(underlyingUri.getScheme())) {
            filePath = underlyingUri.getPath();
        }

        SerializeObject reader = request.getSerializeParams();
        if (reader == null) {
            Response response =
                    new Response(Response.CODE_ILLEGAL_ARGUMENT, "serialize params is null");
            request.getCallback().callback(response);
            return;
        }
        String internalUri = reader.optString(PARAM_URI);
        if (TextUtils.isEmpty(filePath)) {
            Response response =
                    new Response(Response.CODE_IO_ERROR, "Uri is read only:" + internalUri);
            request.getCallback().callback(response);
            return;
        }

        SerializeObject attributes = reader.optSerializeObject(PARAM_ATTRIBUTES);
        if (attributes == null) {
            Response response =
                    new Response(Response.CODE_ILLEGAL_ARGUMENT, "attributes NOT found.");
            request.getCallback().callback(response);
            return;
        }

        if (attributes.length() == 0) {
            request.getCallback().callback(Response.SUCCESS);
            return;
        }

        try {
            ExifInterface exifInterface = new ExifInterface(filePath);
            for (String key : attributes.keySet()) {
                String value = attributes.getString(key);
                exifInterface.setAttribute(key, value);
            }
            exifInterface.saveAttributes();

            String uri = reader.optString(PARAM_URI);
            JSONObject result = makeResult(uri);
            request.getCallback().callback(new Response(result));
        } catch (IOException e) {
            Log.e(TAG, "Fail to set exif info by " + underlyingUri, e);
            request.getCallback().callback(getExceptionResponse(request, e));
        }
    }

    private void getExifInfo(Request request, Uri underlyingUri) {
        Context context = request.getNativeInterface().getActivity();

        try (InputStream in = context.getContentResolver().openInputStream(underlyingUri)) {
            JSONObject exifData = new JSONObject();
            ExifInterface exifInterface = new ExifInterface(in);
            Field field = exifInterface.getClass().getDeclaredField("mAttributes");
            field.setAccessible(true);
            HashMap[] attributes = (HashMap[]) field.get(exifInterface);
            if (attributes != null) {
                for (HashMap<String, Object> map : attributes) {
                    for (String key : map.keySet()) {
                        String value = exifInterface.getAttribute(key);
                        if (!TextUtils.isEmpty(value)) {
                            exifData.put(key, value);
                        }
                    }
                }
            }
            SerializeObject reader = request.getSerializeParams();
            if (reader == null) {
                Response response =
                        new Response(Response.CODE_ILLEGAL_ARGUMENT, "serialize params is null");
                request.getCallback().callback(response);
                return;
            }
            String uri = reader.optString(PARAM_URI);
            JSONObject result = makeResult(uri);
            result.put(RESULT_ATTRIBUTES, exifData);
            request.getCallback().callback(new Response(result));
        } catch (Exception e) {
            Log.e(TAG, "Fail to get exif info by " + underlyingUri, e);
            request.getCallback().callback(getExceptionResponse(request, e));
        }
    }

    private void editImage(final Request request, Uri underlyingUri) throws SerializeException {
        final NativeInterface nativeInterface = request.getNativeInterface();
        Activity activity = nativeInterface.getActivity();
        LifecycleListener l =
                new LifecycleListener() {
                    @Override
                    public void onActivityResult(int requestCode, int resultCode, Intent data) {
                        if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {
                            nativeInterface.removeLifecycleListener(this);
                            CropImage.ActivityResult result = CropImage.getActivityResult(data);
                            Response response;
                            if (resultCode == Activity.RESULT_OK) {
                                String resultUri = request.getApplicationContext()
                                        .getInternalUri(result.getUri());
                                response = new Response(makeResult(resultUri));
                            } else if (resultCode == Activity.RESULT_CANCELED) {
                                response = Response.CANCEL;
                            } else if (resultCode
                                    == CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE) {
                                Exception error = result.getError();
                                response = getExceptionResponse(request, error);
                            } else {
                                response = Response.ERROR;
                            }
                            request.getCallback().callback(response);
                        }
                    }
                };
        try {
            File tempFile = request.getApplicationContext().createTempFile("edit", ".jpg");
            CropImage.ActivityBuilder builder =
                    CropImage.activity(underlyingUri)
                            .setAutoZoomEnabled(true)
                            .setOutputUri(Uri.fromFile(tempFile));
            SerializeObject params = request.getSerializeParams();
            boolean hasAspectRatioX = false;
            boolean hasAspectRatioY = false;
            if (params != null) {
                hasAspectRatioX = params.has(PARAM_ASPECT_RATIO_X);
                hasAspectRatioY = params.has(PARAM_ASPECT_RATIO_Y);
            }
            if (hasAspectRatioX ^ hasAspectRatioY) {
                Response response =
                        new Response(
                                Response.CODE_ILLEGAL_ARGUMENT,
                                "BOTH aspectRatioX and aspectRatioY needed.");
                request.getCallback().callback(response);
                return;
            }

            if (hasAspectRatioX && hasAspectRatioY) {
                int aspectRatioX = params.getInt(PARAM_ASPECT_RATIO_X);
                int aspectRatioY = params.getInt(PARAM_ASPECT_RATIO_Y);
                if (aspectRatioX <= 0 || aspectRatioY <= 0) {
                    Response response =
                            new Response(
                                    Response.CODE_ILLEGAL_ARGUMENT,
                                    "Illegal aspectRatio, aspectRatioX:"
                                            + aspectRatioX
                                            + ", aspectRatioY:"
                                            + aspectRatioY);
                    request.getCallback().callback(response);
                    return;
                }
                builder.setAspectRatio(aspectRatioX, aspectRatioY);
            }
            nativeInterface.addLifecycleListener(l);
            builder.start(activity);
        } catch (IOException e) {
            Response response =
                    getExceptionResponse(request.getAction(), e, Response.CODE_IO_ERROR);
            request.getCallback().callback(response);
        }
    }

    private void applyOperations(Request request, Uri underlyingUri) throws JSONException {
        JSONObject jsonParams = request.getJSONParams();
        JSONArray operations = jsonParams.optJSONArray(PARAM_OPERATIONS);
        if (operations == null || operations.length() < 1) {
            compressImage(request, underlyingUri);
            return;
        }

        List<Operation> operationList = new ArrayList<>();
        for (int i = 0; i < operations.length(); i++) {
            JSONObject operation = operations.getJSONObject(i);
            String action = operation.getString(PARAM_ACTION);
            if (ACTION_CROP.equals(action)) {
                double x = operation.optDouble(PARAM_CROP_X, 0);
                double y = operation.optDouble(PARAM_CROP_Y, 0);
                double width = operation.getDouble(PARAM_CROP_WIDTH);
                double height = operation.getDouble(PARAM_CROP_HEIGHT);
                if (width <= 0 || height <= 0) {
                    Response response =
                            new Response(
                                    Response.CODE_ILLEGAL_ARGUMENT,
                                    "crop width " + width + " and crop height " + height
                                            + " must greater than 0");
                    request.getCallback().callback(response);
                    return;
                }
                if (x < 0 || y < 0) {
                    Response response =
                            new Response(
                                    Response.CODE_ILLEGAL_ARGUMENT,
                                    "crop x " + x + " and crop y " + y
                                            + " can not be smaller than 0");
                    request.getCallback().callback(response);
                    return;
                }
                operationList.add(new CropOperation(x, y, width, height));
            } else if (ACTION_ROTATE.equals(action)) {
                double degree = operation.getDouble(PARAM_ROTATE_DEGREE);
                operationList.add(new RotateOperation(degree));
            } else if (ACTION_SCALE.equals(action)) {
                double scaleX = operation.optDouble(PARAM_SCALE_X, 1d);
                double scaleY = operation.optDouble(PARAM_SCALE_Y, 1d);
                if (scaleX <= 0 || scaleY <= 0) {
                    Response response =
                            new Response(
                                    Response.CODE_ILLEGAL_ARGUMENT,
                                    "scaleX " + scaleX + " and scaleY " + scaleY
                                            + " must greater than 0");
                    request.getCallback().callback(response);
                    return;
                }
                operationList.add(new ScaleOperation(scaleX, scaleY));
            } else {
                Response response =
                        new Response(Response.CODE_ILLEGAL_ARGUMENT,
                                "unsupported operation " + action);
                request.getCallback().callback(response);
                return;
            }
        }

        double quality = jsonParams.optDouble(PARAM_QUALITY, 75d);
        String format = jsonParams.optString(PARAM_FORMAT, IMAGE_FORMAT_JPEG).toLowerCase();
        quality = quality < 0 ? 0 : (quality > 100 ? 100 : quality);

        Bitmap.CompressFormat compressFormat = FORMAT_MAP.get(format);
        if (compressFormat == null) {
            Response response =
                    new Response(Response.CODE_ILLEGAL_ARGUMENT, "unknown format: " + format);
            request.getCallback().callback(response);
            return;
        }

        try {
            String resultUri =
                    handleBitmap(request, underlyingUri, operationList, quality, format,
                            compressFormat);
            if (resultUri != null) {
                Response response = new Response(makeResult(resultUri));
                request.getCallback().callback(response);
            } else {
                Response response = new Response(Response.ERROR);
                request.getCallback().callback(response);
            }
        } catch (IOException e) {
            Response response =
                    getExceptionResponse(request.getAction(), e, Response.CODE_IO_ERROR);
            request.getCallback().callback(response);
        } catch (OutOfMemoryError e) {
            Response response = getErrorResponse(request.getAction(), e, Response.CODE_OOM_ERROR);
            request.getCallback().callback(response);
        }
    }

    private String handleBitmap(
            Request request,
            Uri uri,
            List<Operation> operations,
            double quality,
            String format,
            Bitmap.CompressFormat compressFormat)
            throws IOException {
        Bitmap bitmap = null;
        Activity activity = request.getNativeInterface().getActivity();

        BitmapFactory.Options options = new BitmapFactory.Options();
        try (InputStream in = activity.getContentResolver().openInputStream(uri)) {
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeStream(in, null, options);
        }
        int orgWidth = options.outWidth;
        int orgHeight = options.outHeight;

        options.inJustDecodeBounds = false;

        for (Operation operation : operations) {
            if (operation instanceof CropOperation) {
                CropOperation cropOperation = (CropOperation) operation;
                Rect rect =
                        new Rect(
                                (int) Math.round(cropOperation.x),
                                (int) Math.round(cropOperation.y),
                                (int) Math.round(cropOperation.x + cropOperation.width),
                                (int) Math.round(cropOperation.y + cropOperation.height));
                if (bitmap == null) {
                    if (!checkCropParams(cropOperation, request, orgWidth, orgHeight)) {
                        return null;
                    }
                    BitmapRegionDecoder decoder = null;
                    try (InputStream in = activity.getContentResolver().openInputStream(uri)) {
                        decoder = BitmapRegionDecoder.newInstance(in, false);
                        if (decoder != null) {
                            bitmap = decoder.decodeRegion(rect, options);
                        }
                    } finally {
                        if (decoder != null) {
                            decoder.recycle();
                        }
                    }
                } else {
                    if (!checkCropParams(cropOperation, request, bitmap.getWidth(),
                            bitmap.getHeight())) {
                        return null;
                    }
                    bitmap =
                            Bitmap.createBitmap(
                                    bitmap,
                                    (int) Math.round(cropOperation.x),
                                    (int) Math.round(cropOperation.y),
                                    (int) Math.round(cropOperation.width),
                                    (int) Math.round(cropOperation.height));
                }
            } else if (operation instanceof RotateOperation) {
                RotateOperation rotateOperation = (RotateOperation) operation;
                Matrix matrix = new Matrix();
                matrix.setRotate((float) rotateOperation.degree);
                if (bitmap == null) {
                    try (InputStream in = activity.getContentResolver().openInputStream(uri)) {
                        bitmap = BitmapFactory.decodeStream(in, null, options);
                    }
                }
                if (bitmap != null) {
                    bitmap =
                            Bitmap.createBitmap(
                                    bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix,
                                    false);
                }
            } else if (operation instanceof ScaleOperation) {
                ScaleOperation scaleOperation = (ScaleOperation) operation;
                Matrix matrix = new Matrix();
                matrix.setScale((float) scaleOperation.scaleX, (float) scaleOperation.scaleY);
                if (bitmap == null) {
                    try (InputStream in = activity.getContentResolver().openInputStream(uri)) {
                        bitmap = BitmapFactory.decodeStream(in, null, options);
                    }
                }
                if (bitmap != null) {
                    bitmap =
                            Bitmap.createBitmap(
                                    bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix,
                                    false);
                }
            }
        }

        if (bitmap != null) {
            OutputStream out = null;
            File tmpFile;
            try {
                String extension = EXTENSION_MAP.get(format);
                tmpFile = request.getApplicationContext()
                        .createTempFile("applyOperations", extension);
                out = new FileOutputStream(tmpFile);
                bitmap.compress(compressFormat, (int) Math.round(quality), out);
                return request.getApplicationContext().getInternalUri(tmpFile);
            } finally {
                FileUtils.closeQuietly(out);
                bitmap.recycle();
            }
        } else {
            return null;
        }
    }

    private boolean checkCropParams(
            CropOperation cropOperation, Request request, int width, int height) {
        if (Math.round(cropOperation.x) + Math.round(cropOperation.width) > width) {
            Response response =
                    new Response(Response.CODE_ILLEGAL_ARGUMENT,
                            "x + width must be <= bitmap.width()");
            request.getCallback().callback(response);
            return false;
        }
        if (Math.round(cropOperation.y) + Math.round(cropOperation.height) > height) {
            Response response =
                    new Response(Response.CODE_ILLEGAL_ARGUMENT,
                            "y + height must be <= bitmap.height()");
            request.getCallback().callback(response);
            return false;
        }
        return true;
    }

    private BitmapFactory.Options obtainOptions(Request request, Uri underlyingUri) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        Activity activity = request.getNativeInterface().getActivity();
        try (InputStream in = activity.getContentResolver().openInputStream(underlyingUri)) {
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeStream(in, null, options);
        } catch (IOException e) {
            Response response =
                    getExceptionResponse(request.getAction(), e, Response.CODE_IO_ERROR);
            request.getCallback().callback(response);
            return null;
        }
        return options;
    }

    private Bitmap scaleAndRotateBitmap(
            Bitmap src, ExifInterface exifInterface, double expectWidth, double expectHeight) {
        Matrix matrix = new Matrix();
        int width = src.getWidth();
        int height = src.getHeight();
        float sx = (float) (expectWidth / width);
        float sy = (float) (expectHeight / height);
        matrix.postScale(sx, sy);
        int orientation =
                exifInterface.getAttributeInt(
                        ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);

        switch (orientation) {
            case ExifInterface.ORIENTATION_ROTATE_90:
                matrix.postRotate(90);
                break;
            case ExifInterface.ORIENTATION_ROTATE_180:
                matrix.postRotate(180);
                break;
            case ExifInterface.ORIENTATION_ROTATE_270:
                matrix.postRotate(270);
                break;
            default:
                break;
        }

        return Bitmap.createBitmap(src, 0, 0, width, height, matrix, true);
    }

    private JSONObject makeResult(String uri) {
        JSONObject result = new JSONObject();
        try {
            result.put(RESULT_URI, uri);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
        return result;
    }

    private long getFileLength(final Context context, final Uri uri) {
        final String scheme = uri.getScheme();
        String path = null;
        if (ContentResolver.SCHEME_FILE.equals(scheme)) {
            path = uri.getPath();
        } else if (ContentResolver.SCHEME_CONTENT.equals(scheme)) {
            Cursor cursor = null;
            try {
                cursor =
                        context
                                .getContentResolver()
                                .query(uri, new String[] {MediaStore.Images.ImageColumns.DATA},
                                        null, null, null);
                if (null != cursor) {
                    if (cursor.moveToFirst()) {
                        int index = cursor.getColumnIndex(MediaStore.Images.ImageColumns.DATA);
                        if (index > -1) {
                            path = cursor.getString(index);
                        }
                    }
                }
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
            }
        }
        return TextUtils.isEmpty(path) ? -1 : new File(path).length();
    }

    private Uri getUnderlyingUri(Request request) throws JSONException {
        JSONObject jsonParams = request.getJSONParams();
        if (jsonParams == null) {
            Response response = new Response(Response.CODE_ILLEGAL_ARGUMENT, "params is null");
            request.getCallback().callback(response);
            return null;
        }
        String uri = jsonParams.optString(PARAM_URI);
        if (TextUtils.isEmpty(uri)) {
            Response response = new Response(Response.CODE_ILLEGAL_ARGUMENT, "uri can't be empty");
            request.getCallback().callback(response);
            return null;
        }

        Uri underlyingUri = null;
        try {
            underlyingUri = request.getApplicationContext().getUnderlyingUri(uri);
        } catch (IllegalArgumentException e) {
            Response response = new Response(Response.CODE_ILLEGAL_ARGUMENT, "invalid uri: " + uri);
            request.getCallback().callback(response);
            return null;
        }
        if (underlyingUri == null) {
            Response response = new Response(Response.CODE_ILLEGAL_ARGUMENT, "invalid uri: " + uri);
            request.getCallback().callback(response);
            return null;
        }
        return underlyingUri;
    }

    class Operation {
        String action;
    }

    class CropOperation extends Operation {
        double x;
        double y;
        double width;
        double height;

        public CropOperation(double x, double y, double width, double height) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
        }
    }

    class RotateOperation extends Operation {
        double degree;

        public RotateOperation(double degree) {
            this.degree = degree;
        }
    }

    class ScaleOperation extends Operation {
        double scaleX;
        double scaleY;

        public ScaleOperation(double scaleX, double scaleY) {
            this.scaleX = scaleX;
            this.scaleY = scaleY;
        }
    }
}
