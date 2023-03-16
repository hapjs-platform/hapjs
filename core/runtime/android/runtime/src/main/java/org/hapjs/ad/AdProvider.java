/*
 * Copyright (c) 2023, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.ad;

import android.content.Context;

import androidx.annotation.NonNull;

import org.hapjs.component.Container;

public interface AdProvider {
    String NAME = "AdProvider";

    AdProxy createAdCustomProxy(@NonNull Context context, @NonNull Container adComponent);
}
