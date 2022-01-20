/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.io;

import android.content.Context;
import java.io.IOException;
import java.io.InputStream;

public class AssetSource implements Source {
    private Context mContext;
    private String mAsset;

    public AssetSource(Context context, String asset) {
        mContext = context;
        mAsset = asset;
    }

    @Override
    public InputStream open() throws IOException {
        return mContext.getAssets().open(mAsset);
    }
}
