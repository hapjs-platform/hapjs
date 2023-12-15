/*
 * Copyright (c) 2021-present, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.runtime.resource;

import android.content.Context;
import android.net.Uri;
import androidx.annotation.Nullable;

import java.util.List;

public interface ResourceManager {
    @Nullable
    Uri getResource(String resourcePath);

    @Nullable
    Uri getResource(String resourcePath, String page);

    long size(Context context);

    List<String> getFileNameList(Context context, String pkg, String resourcePath);
}
