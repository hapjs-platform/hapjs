/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.features.net;

import android.content.Context;
import android.net.Uri;
import java.io.IOException;
import java.io.InputStream;
import okhttp3.MediaType;
import okhttp3.RequestBody;
import okhttp3.internal.Util;
import okio.BufferedSink;
import okio.Okio;
import okio.Source;

public class FileRequestBody extends RequestBody {
    private MediaType mediaType;
    private InputStream inputStream;
    private Uri uri;
    private Context context;
    private long contentSize;

    public FileRequestBody(MediaType contentType, Uri uri, Context context) {
        this.uri = uri;
        this.mediaType = contentType;
        this.context = context;
        try {
            inputStream = context.getContentResolver().openInputStream(uri);
            this.contentSize = inputStream.available();
        } catch (IOException e) {
            this.contentSize = -1L;
        }
    }

    @Override
    public MediaType contentType() {
        return mediaType;
    }

    @Override
    public long contentLength() {
        return contentSize;
    }

    @Override
    public void writeTo(BufferedSink sink) throws IOException {
        Source source = null;
        try {
            if (inputStream == null) {
                inputStream = context.getContentResolver().openInputStream(uri);
            }
            source = Okio.source(inputStream);
            sink.writeAll(source);
        } finally {
            Util.closeQuietly(source);
            if (inputStream != null) {
                inputStream.close();
            }
            inputStream = null;
        }
    }
}
