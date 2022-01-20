/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.card.api.debug;

import android.content.Context;

public interface CardDebugHost {
    boolean launch(Context context, String url);

    String getArchiveHost();

    String getRuntimeHost();
}
