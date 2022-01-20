/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.render.cutout;

import android.content.Context;
import android.graphics.Rect;
import android.view.Window;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import java.util.List;

public class DefaultCutoutProvider implements CutoutProvider {

    @Override
    public boolean isCutoutScreen(@NonNull Context context, @NonNull Window window) {
        return false;
    }

    @Nullable
    @Override
    public List<Rect> getCutoutDisplay(@NonNull Context context, @NonNull Window window) {
        return null;
    }

    @Override
    public int getCutoutHeight(@NonNull Context context, @NonNull Window window) {
        return 0;
    }
}
