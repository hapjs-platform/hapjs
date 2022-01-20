/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.io;

import android.content.Context;
import android.net.Uri;
import java.io.IOException;
import java.io.InputStream;

public class UriSource implements Source {
    private Context mContext;
    private Uri mUri;

    public UriSource(Context context, Uri uri) {
        mContext = context;
        mUri = uri;
    }

    @Override
    public InputStream open() throws IOException {
        return mContext.getContentResolver().openInputStream(mUri);
    }
}
