/*
 * Copyright (c) 2022, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.render;

import android.graphics.Typeface;
import android.net.Uri;

import org.hapjs.common.utils.FileUtils;

public class DefaultFontFamilyProvider implements FontFamilyProvider {

    private static final String FONTS_PATH = "/system/fonts/";

    @Override
    public Typeface getTypefaceFromLocal(Uri fontUri, int style) {
        if (fontUri == null) {
            return null;
        }
        String filePath = FONTS_PATH + fontUri.getHost() + ".ttf";
        Typeface typeface = null;
        if (FileUtils.isFileExist(filePath)) {
            typeface = Typeface.createFromFile(filePath);
        }
        return typeface;
    }
}
