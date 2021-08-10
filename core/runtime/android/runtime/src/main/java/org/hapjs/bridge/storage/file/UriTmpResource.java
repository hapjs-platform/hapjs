/*
 * Copyright (c) 2021-2022, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.bridge.storage.file;

import android.content.Context;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import org.hapjs.runtime.Runtime;

public class UriTmpResource extends TmpResource {
    private Uri mUnderlyingUri;

    public UriTmpResource(String internalUri, Uri underlyingUri) {
        super(internalUri);
        mUnderlyingUri = underlyingUri;
    }

    @Override
    public InputStream openInputStream() throws IOException {
        Context context = Runtime.getInstance().getContext();
        return context.getContentResolver().openInputStream(mUnderlyingUri);
    }

    @Override
    public ResourceInfo get() {
        Context context = Runtime.getInstance().getContext();
        return ResourceInfo.create(context, toUri(), mUnderlyingUri);
    }

    public ResourceInfo get(boolean recursive) {
        return get();
    }

    @Override
    public File getUnderlyingFile() {
        return null;
    }

    @Override
    public Uri getUnderlyingUri() {
        return mUnderlyingUri;
    }

    @Override
    public ParcelFileDescriptor getParcelFileDescriptor() throws IOException {
        Context context = Runtime.getInstance().getContext();
        return context.getContentResolver().openFileDescriptor(mUnderlyingUri, "r");
    }
}
