/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.features.storage.file;

import android.text.TextUtils;

import org.hapjs.bridge.ApplicationContext;
import org.hapjs.bridge.FeatureExtension;
import org.hapjs.bridge.Request;
import org.hapjs.bridge.Response;
import org.hapjs.bridge.annotation.ActionAnnotation;
import org.hapjs.bridge.annotation.FeatureExtensionAnnotation;
import org.hapjs.bridge.storage.file.IResourceFactory;
import org.hapjs.render.RootView;
import org.hapjs.render.jsruntime.serialize.SerializeException;
import org.hapjs.render.jsruntime.serialize.SerializeObject;
import org.hapjs.render.jsruntime.serialize.TypedArrayProxy;
import org.json.JSONException;
import org.json.JSONObject;

import java.nio.ByteBuffer;

@FeatureExtensionAnnotation(
        name = FileStorageFeature.FEATURE_NAME,
        residentType = FeatureExtensionAnnotation.ResidentType.USEABLE,
        actions = {
                @ActionAnnotation(name = FileStorageFeature.ACTION_MOVE, mode = FeatureExtension.Mode.ASYNC),
                @ActionAnnotation(name = FileStorageFeature.ACTION_COPY, mode = FeatureExtension.Mode.ASYNC),
                @ActionAnnotation(name = FileStorageFeature.ACTION_LIST, mode = FeatureExtension.Mode.ASYNC),
                @ActionAnnotation(name = FileStorageFeature.ACTION_GET, mode = FeatureExtension.Mode.ASYNC),
                @ActionAnnotation(
                        name = FileStorageFeature.ACTION_DELETE,
                        mode = FeatureExtension.Mode.ASYNC),
                @ActionAnnotation(
                        name = FileStorageFeature.ACTION_WRITE_TEXT,
                        mode = FeatureExtension.Mode.ASYNC),
                @ActionAnnotation(
                        name = FileStorageFeature.ACTION_READ_TEXT,
                        mode = FeatureExtension.Mode.ASYNC),
                @ActionAnnotation(
                        name = FileStorageFeature.ACTION_WRITE_ARRAY_BUFFER,
                        mode = FeatureExtension.Mode.ASYNC,
                        normalize = FeatureExtension.Normalize.RAW),
                @ActionAnnotation(
                        name = FileStorageFeature.ACTION_READ_ARRAY_BUFFER,
                        mode = FeatureExtension.Mode.ASYNC),
                @ActionAnnotation(
                        name = FileStorageFeature.ACTION_MK_DIR,
                        mode = FeatureExtension.Mode.ASYNC),
                @ActionAnnotation(
                        name = FileStorageFeature.ACTION_RM_DIR,
                        mode = FeatureExtension.Mode.ASYNC),
                @ActionAnnotation(
                        name = FileStorageFeature.ACTION_ACCESS,
                        mode = FeatureExtension.Mode.ASYNC),
        })
public class FileStorageFeature extends FeatureExtension {
    public static final String ACTION_MOVE = "move";
    public static final String ACTION_COPY = "copy";
    public static final String ACTION_LIST = "list";
    public static final String ACTION_GET = "get";
    public static final String ACTION_DELETE = "delete";
    public static final String ACTION_WRITE_TEXT = "writeText";
    public static final String ACTION_READ_TEXT = "readText";
    public static final String ACTION_WRITE_ARRAY_BUFFER = "writeArrayBuffer";
    public static final String ACTION_READ_ARRAY_BUFFER = "readArrayBuffer";
    protected static final String FEATURE_NAME = "system.file";
    protected static final String ACTION_MK_DIR = "mkdir";
    protected static final String ACTION_RM_DIR = "rmdir";
    protected static final String ACTION_ACCESS = "access";
    protected static final String PARAMS_TEXT = "text";
    protected static final String PARAMS_POSITION = "position";
    protected static final String PARAMS_LENGTH = "length";
    protected static final String PARAMS_RECURSIVE = "recursive";
    protected static final String PARAMS_APPEND = "append";
    protected static final String PARAMS_URI = "uri";
    protected static final String RESULT_TEXT = "text";
    protected static final String RESULT_BUFFER = "buffer";
    private static final String PARAMS_SRC_URI = "srcUri";
    private static final String PARAMS_DST_URI = "dstUri";
    private static final String PARAMS_ENCODING = "encoding";
    private static final String PARAMS_BUFFER = "buffer";
    protected FileStorage mFileStorage;

    public FileStorageFeature() {
        mFileStorage = createFileStorage();
    }

    protected IResourceFactory getResourceFactory(ApplicationContext applicationContext) {
        return applicationContext.getResourceFactory();
    }

    protected FileStorage createFileStorage() {
        return new FileStorage();
    }

    @Override
    protected Response invokeInner(Request request) throws Exception {
        String action = request.getAction();
        if (ACTION_MOVE.equals(action)) {
            doMove(request);
        } else if (ACTION_COPY.equals(action)) {
            doCopy(request);
        } else if (ACTION_LIST.equals(action)) {
            doList(request);
        } else if (ACTION_GET.equals(action)) {
            doGet(request);
        } else if (ACTION_DELETE.equals(action)) {
            doDelete(request);
        } else if (ACTION_WRITE_TEXT.equals(action)) {
            doWriteText(request);
        } else if (ACTION_READ_TEXT.equals(action)) {
            doReadText(request);
        } else if (ACTION_WRITE_ARRAY_BUFFER.equals(action)) {
            doWriteArrayBuffer(request);
        } else if (ACTION_READ_ARRAY_BUFFER.equals(action)) {
            doReadArrayBuffer(request);
        } else if (ACTION_MK_DIR.equals(action)) {
            doMkDir(request);
        } else if (ACTION_RM_DIR.equals(action)) {
            doRmDir(request);
        } else if (ACTION_ACCESS.equals(action)) {
            doAccess(request);
        }
        return Response.SUCCESS;
    }

    private void doMove(Request request) throws JSONException {
        JSONObject jsonParams = new JSONObject(request.getRawParams());
        String srcUri = jsonParams.optString(PARAMS_SRC_URI);
        if (TextUtils.isEmpty(srcUri)) {
            request
                    .getCallback()
                    .callback(new Response(Response.CODE_ILLEGAL_ARGUMENT,
                            PARAMS_SRC_URI + " not define"));
            return;
        }
        String destUri = jsonParams.optString(PARAMS_DST_URI);
        if (TextUtils.isEmpty(destUri)) {
            request
                    .getCallback()
                    .callback(new Response(Response.CODE_ILLEGAL_ARGUMENT,
                            PARAMS_DST_URI + " not define"));
            return;
        }
        IResourceFactory resourceFactory = getResourceFactory(request.getApplicationContext());
        Response response = mFileStorage.move(resourceFactory, srcUri, destUri);
        request.getCallback().callback(response);
    }

    private void doCopy(Request request) throws JSONException {
        JSONObject jsonParams = new JSONObject(request.getRawParams());
        String srcUri = jsonParams.optString(PARAMS_SRC_URI);
        if (TextUtils.isEmpty(srcUri)) {
            request
                    .getCallback()
                    .callback(new Response(Response.CODE_ILLEGAL_ARGUMENT,
                            PARAMS_SRC_URI + " not define"));
            return;
        }
        String destUri = jsonParams.optString(PARAMS_DST_URI);
        if (TextUtils.isEmpty(destUri)) {
            request
                    .getCallback()
                    .callback(new Response(Response.CODE_ILLEGAL_ARGUMENT,
                            PARAMS_DST_URI + " not define"));
            return;
        }
        IResourceFactory resourceFactory = getResourceFactory(request.getApplicationContext());
        Response response = mFileStorage.copy(resourceFactory, srcUri, destUri);
        request.getCallback().callback(response);
    }

    private void doList(Request request) throws JSONException {
        JSONObject jsonParams = new JSONObject(request.getRawParams());
        String uri = jsonParams.optString(PARAMS_URI);
        if (TextUtils.isEmpty(uri)) {
            request
                    .getCallback()
                    .callback(new Response(Response.CODE_ILLEGAL_ARGUMENT,
                            PARAMS_URI + " not define"));
            return;
        }
        IResourceFactory resourceFactory = getResourceFactory(request.getApplicationContext());
        Response response = mFileStorage.list(resourceFactory, uri);
        request.getCallback().callback(response);
    }

    protected void doGet(Request request) throws Exception {
        JSONObject jsonParams = new JSONObject(request.getRawParams());
        String uri = jsonParams.optString(PARAMS_URI);
        if (TextUtils.isEmpty(uri)) {
            request
                    .getCallback()
                    .callback(new Response(Response.CODE_ILLEGAL_ARGUMENT,
                            PARAMS_URI + " not define"));
            return;
        }
        boolean recursive = jsonParams.optBoolean(PARAMS_RECURSIVE, false);
        IResourceFactory resourceFactory = getResourceFactory(request.getApplicationContext());
        Response response = mFileStorage.get(resourceFactory, uri, recursive);
        request.getCallback().callback(response);
    }

    private void doDelete(Request request) throws JSONException {
        JSONObject jsonParams = new JSONObject(request.getRawParams());
        String uri = jsonParams.optString(PARAMS_URI);
        if (TextUtils.isEmpty(uri)) {
            request
                    .getCallback()
                    .callback(new Response(Response.CODE_ILLEGAL_ARGUMENT,
                            PARAMS_URI + " not define"));
            return;
        }
        IResourceFactory resourceFactory = getResourceFactory(request.getApplicationContext());
        Response response = mFileStorage.delete(resourceFactory, uri);
        request.getCallback().callback(response);
    }

    private void doWriteText(Request request) throws JSONException {
        JSONObject jsonParams = request.getJSONParams();
        String uri = jsonParams.optString(PARAMS_URI);
        String text = jsonParams.optString(PARAMS_TEXT);
        String encoding = jsonParams.optString(PARAMS_ENCODING, "UTF-8");
        boolean append = jsonParams.optBoolean(PARAMS_APPEND, false);

        if (TextUtils.isEmpty(uri)) {
            request
                    .getCallback()
                    .callback(new Response(Response.CODE_ILLEGAL_ARGUMENT,
                            PARAMS_URI + " not define"));
            return;
        }

        if (TextUtils.isEmpty(text)) {
            request
                    .getCallback()
                    .callback(new Response(Response.CODE_ILLEGAL_ARGUMENT,
                            PARAMS_TEXT + " not define"));
            return;
        }
        IResourceFactory resourceFactory = getResourceFactory(request.getApplicationContext());
        Response response = mFileStorage.writeText(resourceFactory, uri, text, encoding, append);
        request.getCallback().callback(response);
    }

    private void doReadText(Request request) throws JSONException {
        JSONObject jsonParams = request.getJSONParams();
        String uri = jsonParams.optString(PARAMS_URI);
        String encoding = jsonParams.optString(PARAMS_ENCODING, "UTF-8");
        if (TextUtils.isEmpty(uri)) {
            request
                    .getCallback()
                    .callback(new Response(Response.CODE_ILLEGAL_ARGUMENT,
                            PARAMS_URI + " not define"));
            return;
        }
        IResourceFactory resourceFactory = getResourceFactory(request.getApplicationContext());
        Response response = mFileStorage.readText(resourceFactory, uri, encoding);
        request.getCallback().callback(response);
    }

    private void doWriteArrayBuffer(Request request) throws SerializeException {
        SerializeObject params = request.getSerializeParams();
        if (params == null) {
            request.getCallback()
                    .callback(new Response(Response.CODE_ILLEGAL_ARGUMENT, "no params"));
            return;
        }
        String uri = params.optString(PARAMS_URI);

        if (TextUtils.isEmpty(uri)) {
            request
                    .getCallback()
                    .callback(new Response(Response.CODE_ILLEGAL_ARGUMENT,
                            PARAMS_URI + " not define"));
            return;
        }

        TypedArrayProxy typedArrayProxy = params.optTypedArrayProxy(PARAMS_BUFFER);
        if (typedArrayProxy == null) {
            request
                    .getCallback()
                    .callback(new Response(Response.CODE_ILLEGAL_ARGUMENT,
                            PARAMS_BUFFER + " not define"));
            return;
        }

        int position = params.optInt(PARAMS_POSITION, 0);
        if (position < 0) {
            request
                    .getCallback()
                    .callback(new Response(Response.CODE_ILLEGAL_ARGUMENT, "Invalid position"));
            return;
        }

        boolean append = params.optBoolean(PARAMS_APPEND, false);
        IResourceFactory resourceFactory = getResourceFactory(request.getApplicationContext());
        ByteBuffer byteBuffer = typedArrayProxy.getBuffer();
        Response response =
                mFileStorage.writeBuffer(resourceFactory, uri, byteBuffer, position,
                        append);
        request.getCallback().callback(response);
    }

    private void doReadArrayBuffer(Request request) throws JSONException {
        JSONObject jsonParams = request.getJSONParams();
        String uri = jsonParams.optString(PARAMS_URI);
        int position = jsonParams.optInt(PARAMS_POSITION);
        int length = jsonParams.optInt(PARAMS_LENGTH, Integer.MAX_VALUE);

        if (TextUtils.isEmpty(uri)) {
            request
                    .getCallback()
                    .callback(new Response(Response.CODE_ILLEGAL_ARGUMENT,
                            PARAMS_URI + " not define"));
            return;
        }
        if (position < 0) {
            request
                    .getCallback()
                    .callback(new Response(Response.CODE_ILLEGAL_ARGUMENT, "Invalid position"));
            return;
        }
        if (length < 0) {
            request
                    .getCallback()
                    .callback(new Response(Response.CODE_ILLEGAL_ARGUMENT, "Invalid length"));
            return;
        }
        IResourceFactory resourceFactory = getResourceFactory(request.getApplicationContext());
        Response response = mFileStorage.readArrayBuffer(resourceFactory, uri, position, length);
        request.getCallback().callback(response);
    }

    private void doMkDir(Request request) throws SerializeException {
        SerializeObject params = request.getSerializeParams();
        if (params == null) {
            request.getCallback()
                    .callback(new Response(Response.CODE_ILLEGAL_ARGUMENT, "no params"));
            return;
        }
        String uri = params.optString(PARAMS_URI);
        if (TextUtils.isEmpty(uri)) {
            request
                    .getCallback()
                    .callback(new Response(Response.CODE_ILLEGAL_ARGUMENT,
                            PARAMS_URI + " not define"));
            return;
        }
        boolean recursive = params.optBoolean(PARAMS_RECURSIVE, false);
        IResourceFactory resourceFactory = getResourceFactory(request.getApplicationContext());
        Response response = mFileStorage.mkDir(resourceFactory, uri, recursive);
        request.getCallback().callback(response);
    }

    private void doRmDir(Request request) throws SerializeException {
        SerializeObject params = request.getSerializeParams();
        if (params == null) {
            request.getCallback()
                    .callback(new Response(Response.CODE_ILLEGAL_ARGUMENT, "no params"));
            return;
        }

        String uri = params.optString(PARAMS_URI);
        if (TextUtils.isEmpty(uri)) {
            request
                    .getCallback()
                    .callback(new Response(Response.CODE_ILLEGAL_ARGUMENT,
                            PARAMS_URI + " not define"));
            return;
        }

        boolean recursive = params.optBoolean(PARAMS_RECURSIVE, false);
        IResourceFactory resourceFactory = getResourceFactory(request.getApplicationContext());
        Response response = mFileStorage.rmDir(resourceFactory, uri, recursive);
        request.getCallback().callback(response);
    }

    private void doAccess(Request request) throws SerializeException {
        SerializeObject params = request.getSerializeParams();
        if (params == null) {
            request.getCallback()
                    .callback(new Response(Response.CODE_ILLEGAL_ARGUMENT, "no params"));
            return;
        }

        String uri = params.optString(PARAMS_URI);
        if (TextUtils.isEmpty(uri)) {
            request
                    .getCallback()
                    .callback(new Response(Response.CODE_ILLEGAL_ARGUMENT,
                            PARAMS_URI + " not define"));
            return;
        }
        IResourceFactory resourceFactory = getResourceFactory(request.getApplicationContext());
        Response response = mFileStorage.access(resourceFactory, uri);
        request.getCallback().callback(response);
    }

    @Override
    public String getName() {
        return FEATURE_NAME;
    }
}
