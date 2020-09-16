/*
 * Copyright (c) 2022, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.hapjs.render;

import android.graphics.Typeface;
import android.net.Uri;

public interface FontFamilyProvider {
    String NAME = "fontfamily";

    Typeface getTypefaceFromLocal(Uri fontUri, int style);
}