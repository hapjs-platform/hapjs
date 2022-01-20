/*
 * Copyright (c) 2014-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.stetho.common.android;

import android.content.res.Resources;
import android.view.View;
import javax.annotation.Nullable;

public interface FragmentAccessor<T, U> {
    int NO_ID = 0;

    @Nullable
    U getFragmentManager(T t);

    Resources getResources(T t);

    int getId(T t);

    @Nullable
    String getTag(T t);

    @Nullable
    View getView(T t);

    @Nullable
    U getChildFragmentManager(T t);
}
