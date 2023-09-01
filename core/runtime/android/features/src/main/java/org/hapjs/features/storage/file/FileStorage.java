/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.features.storage.file;

import android.util.Log;


import com.eclipsesource.v8.V8;
import com.eclipsesource.v8.V8Value;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.util.List;

import org.hapjs.bridge.Response;
import org.hapjs.bridge.storage.file.IResourceFactory;
import org.hapjs.bridge.storage.file.Resource;
import org.hapjs.bridge.storage.file.ResourceInfo;
import org.hapjs.common.utils.FileUtils;
import org.hapjs.render.jsruntime.serialize.JavaSerializeObject;
import org.hapjs.render.jsruntime.serialize.SerializeObject;
import org.hapjs.render.jsruntime.serialize.TypedArrayProxy;
import org.json.JSONException;
import org.json.JSONObject;

public class FileStorage {
    private static final String TAG = "FileStorage";

    public Response move(
            IResourceFactory resourceFactory, String srcInternalUri, String dstInternalUri) {
        Log.v(TAG, "move: srcUri=" + srcInternalUri + ", dstUri=" + dstInternalUri);
        String reason;
        Resource srcRes = resourceFactory.create(srcInternalUri);
        if (srcRes == null) {
            reason = "can not resolve srcUri " + srcInternalUri;
            Log.i(TAG, reason);
            return new Response(Response.CODE_IO_ERROR, reason);
        }
        Resource dstRes = resourceFactory.create(dstInternalUri);
        if (dstRes == null) {
            reason = "can not resolve dstUri" + dstInternalUri;
            Log.i(TAG, reason);
            return new Response(Response.CODE_IO_ERROR, reason);
        }

        File dstFile = dstRes.getUnderlyingFile();
        if (dstFile == null) {
            reason = "dstUri " + dstInternalUri + "'s underlyingFile not found.";
            Log.i(TAG, reason);
            return new Response(Response.CODE_IO_ERROR, reason);
        }

        if (dstInternalUri.endsWith("/")) {
            if (dstFile.exists() && !dstFile.isDirectory()) {
                reason = "dstUri " + dstInternalUri + " exists, but is not a directory";
                return new Response(Response.CODE_IO_ERROR, reason);
            } else {
                dstFile = new File(dstFile, srcRes.getName());
            }
        } else if (dstFile.isDirectory()) {
            dstFile = new File(dstFile, srcRes.getName());
        }
        dstRes = resourceFactory.create(dstFile);

        try {
            srcRes.moveTo(dstRes);
            return new Response(dstRes.toUri());
        } catch (IOException e) {
            return new Response(Response.CODE_IO_ERROR, e.getMessage());
        }
    }

    public Response copy(
            IResourceFactory resourceFactory, String srcInternalUri, String dstInternalUri) {
        Log.v(TAG, "copy: srcUri=" + srcInternalUri + ", dstUri=" + dstInternalUri);
        String reason;
        Resource srcRes = resourceFactory.create(srcInternalUri);
        if (srcRes == null) {
            reason = "can not resolve srcUri" + srcInternalUri;
            Log.i(TAG, reason);
            return new Response(Response.CODE_IO_ERROR, reason);
        }

        Resource dstRes = resourceFactory.create(dstInternalUri);
        if (dstRes == null) {
            reason = "can not resolve dstUri" + dstInternalUri;
            Log.i(TAG, reason);
            return new Response(Response.CODE_IO_ERROR, reason);
        }

        File dstFile = dstRes.getUnderlyingFile();
        if (dstFile == null) {
            reason = "dstUri " + dstInternalUri + "'s underlyingFile not found.";
            Log.i(TAG, reason);
            return new Response(Response.CODE_IO_ERROR, reason);
        }

        if (dstInternalUri.endsWith("/")) {
            if (dstFile.exists() && !dstFile.isDirectory()) {
                reason = "dstUri " + dstInternalUri + " exists, but is not a directory";
                return new Response(Response.CODE_IO_ERROR, reason);
            } else {
                dstFile = new File(dstFile, srcRes.getName());
            }
        } else if (dstFile.isDirectory()) {
            dstFile = new File(dstFile, srcRes.getName());
        }
        dstRes = resourceFactory.create(dstFile);

        try {
            srcRes.copyTo(dstRes);
            return new Response(dstRes.toUri());
        } catch (IOException e) {
            return new Response(Response.CODE_IO_ERROR, e.getMessage());
        }
    }

    public Response list(IResourceFactory resourceFactory, String internalUri) {
        Log.v(TAG, "list: uri=" + internalUri);
        String reason;
        Resource resource = resourceFactory.create(internalUri);
        if (resource == null) {
            reason = "can not resolve uri " + internalUri;
            Log.i(TAG, reason);
            return new Response(Response.CODE_IO_ERROR, reason);
        }
        List<ResourceInfo> resourceInfos = resource.list();
        if (resourceInfos == null) {
            reason = "list " + internalUri + " failed, result is null.";
            Log.i(TAG, reason);
            return new Response(Response.CODE_IO_ERROR, reason);
        }
        return new Response(ResourceInfo.toJson(resourceInfos));
    }

    public Response get(IResourceFactory resourceFactory, String internalUri, boolean recursive) {
        Log.v(TAG, "get: uri=" + internalUri);
        String reason;
        Resource resource = resourceFactory.create(internalUri);
        if (resource == null) {
            reason = "can not resolve uri " + internalUri;
            Log.i(TAG, reason);
            return new Response(Response.CODE_IO_ERROR, reason);
        }

        ResourceInfo resourceInfo = resource.get(recursive);
        if (resourceInfo == null) {
            reason = "get resource info failed by " + internalUri;
            Log.i(TAG, reason);
            return new Response(Response.CODE_IO_ERROR, reason);
        }

        return new Response(resourceInfo.toJsonObject());
    }

    public Response delete(IResourceFactory resourceFactory, String internalUri) {
        Log.v(TAG, "delete: uri=" + internalUri);
        String reason;
        Resource resource = resourceFactory.create(internalUri);
        if (resource == null) {
            reason = "can not resolve uri " + internalUri;
            Log.i(TAG, reason);
            return new Response(Response.CODE_IO_ERROR, reason);
        }
        boolean success = false;
        String errorMessage = "io error";
        try {
            success = resource.delete();
        } catch (IOException e) {
            errorMessage = e.getMessage();
        }

        if (!success) {
            Log.v(TAG, errorMessage);
            return new Response(Response.CODE_IO_ERROR, errorMessage);
        }
        return Response.SUCCESS;
    }

    public Response writeText(
            IResourceFactory resourceFactory,
            String internalUri,
            String text,
            String encoding,
            boolean append) {
        Resource resource = resourceFactory.create(internalUri);
        if (resource == null) {
            return new Response(Response.CODE_IO_ERROR, "Fail to get resource by " + internalUri);
        }
        OutputStream outputStream = null;
        try {
            outputStream = resource.openOutputStream(-1, append);
            if (outputStream == null) {
                return new Response(Response.CODE_IO_ERROR,
                        "Fail to open output stream by " + internalUri);
            }
            outputStream.write(text.getBytes(encoding));
            return Response.SUCCESS;
        } catch (UnsupportedEncodingException e) {
            return new Response(Response.CODE_ILLEGAL_ARGUMENT,
                    "Unsupported Encoding : " + encoding);
        } catch (IOException e) {
            return new Response(Response.CODE_IO_ERROR, e.getMessage());
        } finally {
            FileUtils.closeQuietly(outputStream);
        }
    }

    public Response writeBuffer(
            IResourceFactory resourceFactory,
            String internalUri,
            ByteBuffer data,
            long position,
            boolean append) {
        Resource resource = resourceFactory.create(internalUri);
        if (resource == null) {
            return new Response(Response.CODE_IO_ERROR, "Fail to get resource by " + internalUri);
        }
        OutputStream outputStream = null;
        try {
            outputStream = resource.openOutputStream(position, append);
            if (outputStream == null) {
                return new Response(Response.CODE_IO_ERROR,
                        "Fail to open output stream by " + internalUri);
            }
            byte[] out = new byte[2048];
            int readCount;
            while (data.hasRemaining()) {
                readCount = Math.min(out.length, data.remaining());
                data.get(out, 0, readCount);
                outputStream.write(out, 0, readCount);
            }
            return Response.SUCCESS;
        } catch (IOException e) {
            return new Response(Response.CODE_IO_ERROR, e.getMessage());
        } catch (BufferUnderflowException e) {
            return new Response(Response.CODE_IO_ERROR, e.getMessage());
        } catch (IndexOutOfBoundsException e) {
            return new Response(Response.CODE_IO_ERROR, e.getMessage());
        } finally {
            FileUtils.closeQuietly(outputStream);
        }
    }

    public Response readText(IResourceFactory resourceFactory, String internalUri,
                             String encoding) {
        try {
            Resource resource = resourceFactory.create(internalUri);
            if (resource == null) {
                return new Response(Response.CODE_IO_ERROR,
                        "Fail to get resource by " + internalUri);
            }
            InputStream input = resource.openInputStream();
            String text = FileUtils.readStreamAsString(input, encoding, true);
            JSONObject result = new JSONObject().put(FileStorageFeature.RESULT_TEXT, text);
            return new Response(result);
        } catch (FileNotFoundException e) {
            return new Response(Response.CODE_FILE_NOT_FOUND, e.getMessage());
        } catch (IOException e) {
            return new Response(Response.CODE_IO_ERROR, e.getMessage());
        } catch (OutOfMemoryError e) {
            return new Response(Response.CODE_OOM_ERROR, e.getMessage());
        } catch (JSONException e) {
            return new Response(Response.CODE_IO_ERROR, e.getMessage());
        }
    }

    public Response readArrayBuffer(
            IResourceFactory resourceFactory, String internalUri, int position, int length) {
        try {
            Resource resource = resourceFactory.create(internalUri);
            if (resource == null) {
                return new Response(Response.CODE_IO_ERROR,
                        "Fail to get resource by " + internalUri);
            }
            InputStream input = resource.openInputStream();
            ByteBuffer byteBuffer = FileUtils.readStreamAsBuffer(input, position, length, true);
            SerializeObject result = new JavaSerializeObject();
            result.put(FileStorageFeature.RESULT_BUFFER, new TypedArrayProxy(V8Value.UNSIGNED_INT_8_ARRAY, byteBuffer));
            return new Response(result);
        } catch (FileNotFoundException e) {
            return new Response(Response.CODE_FILE_NOT_FOUND, e.getMessage());
        } catch (IOException e) {
            return new Response(Response.CODE_IO_ERROR, e.getMessage());
        } catch (OutOfMemoryError e) {
            return new Response(Response.CODE_OOM_ERROR, e.getMessage());
        }
    }

    public Response mkDir(IResourceFactory resourceFactory, String internalUri, boolean recursive) {
        Resource resource = resourceFactory.create(internalUri);
        if (resource == null) {
            return new Response(Response.CODE_IO_ERROR, "Fail to get resource by " + internalUri);
        }

        try {
            if (resource.mkDir(recursive)) {
                return Response.SUCCESS;
            } else {
                return new Response(Response.CODE_IO_ERROR, "io error");
            }
        } catch (IOException e) {
            return new Response(Response.CODE_IO_ERROR, e.getMessage());
        }
    }

    public Response rmDir(IResourceFactory resourceFactory, String internalUri, boolean recursive) {
        Resource resource = resourceFactory.create(internalUri);
        if (resource == null) {
            return new Response(Response.CODE_IO_ERROR, "Fail to get resource by " + internalUri);
        }

        try {
            if (resource.rmDir(recursive)) {
                return Response.SUCCESS;
            } else {
                return new Response(Response.CODE_IO_ERROR, "io error");
            }
        } catch (IOException e) {
            return new Response(Response.CODE_IO_ERROR, e.getMessage());
        }
    }

    public Response access(IResourceFactory resourceFactory, String internalUri) {
        Resource resource = resourceFactory.create(internalUri);
        if (resource == null) {
            return new Response(Response.CODE_IO_ERROR, "Fail to get resource by " + internalUri);
        }

        if (resource.access()) {
            return new Response(Response.SUCCESS);
        } else {
            return new Response(Response.CODE_IO_ERROR, "file does not exists");
        }
    }
}
