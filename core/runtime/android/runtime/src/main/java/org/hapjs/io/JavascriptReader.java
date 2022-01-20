/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.io;

import android.text.TextUtils;

public class JavascriptReader extends TextReader {
    private static JavascriptReader sInstance;

    protected JavascriptReader() {
    }

    public static TextReader get() {
        if (sInstance == null) {
            sInstance = new JavascriptReader();
        }
        return sInstance;
    }

    @Override
    public String read(Source source) {
        String content = super.read(source);
        if (!TextUtils.isEmpty(content) && content.charAt(content.length() - 1) != '\n') {
            content += '\n';
        }
        return content;
    }
}
