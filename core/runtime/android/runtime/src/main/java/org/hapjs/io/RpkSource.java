/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.io;

import android.content.Context;
import android.net.Uri;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import org.hapjs.runtime.HapEngine;

public class RpkSource implements Source {
    private Context mContext;
    private String mPkg;
    private String mPath;

    public RpkSource(Context context, String pkg, String path) {
        mContext = context;
        mPkg = pkg;
        mPath = path;
    }

    @Override
    public InputStream open() throws IOException {
        Uri uri = HapEngine.getInstance(mPkg).getResourceManager().getResource(mPath);
        if (uri == null) {
            throw new IOException("resource not found: pkg=" + mPkg + ", path=" + mPath);
        }
        try {
            return mContext.getContentResolver().openInputStream(uri);
        } catch (FileNotFoundException e) {
            throw new IOException("resource not found: pkg=" + mPkg + ", path=" + mPath, e);
        }
    }
}
