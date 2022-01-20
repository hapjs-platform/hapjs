/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.io;

import android.util.Log;
import java.io.IOException;
import java.io.InputStream;
import org.hapjs.common.utils.FileUtils;

public class TextReader implements Reader<String> {
    private static final String TAG = "TextReader";

    private static TextReader sInstance;

    protected TextReader() {
    }

    public static TextReader get() {
        if (sInstance == null) {
            sInstance = new TextReader();
        }
        return sInstance;
    }

    @Override
    public String read(Source source) {
        try {
            InputStream is = source.open();
            if (is != null) {
                return FileUtils.readStreamAsString(is, true);
            }
        } catch (IOException e) {
            Log.e(TAG, "Fail to read source", e);
        }
        return null;
    }
}
