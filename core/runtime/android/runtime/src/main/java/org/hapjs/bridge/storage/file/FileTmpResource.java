/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.bridge.storage.file;

import android.net.Uri;
import android.os.ParcelFileDescriptor;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

public class FileTmpResource extends TmpResource {

    private File mFile;

    public FileTmpResource(String internalUri, File file) {
        super(internalUri);
        mFile = file;
    }

    @Override
    public InputStream openInputStream() throws IOException {
        return new FileInputStream(mFile);
    }

    @Override
    public ResourceInfo get() {
        return ResourceInfo.create(toUri(), mFile);
    }

    @Override
    public Uri getUnderlyingUri() {
        return Uri.fromFile(mFile);
    }

    @Override
    public File getUnderlyingFile() {
        return mFile;
    }

    @Override
    public ParcelFileDescriptor getParcelFileDescriptor() throws IOException {
        return ParcelFileDescriptor.open(mFile, ParcelFileDescriptor.MODE_READ_ONLY);
    }
}
