/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.card.support.utils;

import org.hapjs.card.api.RuntimeErrorListener;

public class CardRuntimeErrorManager {
    private static volatile RuntimeErrorListener sListener;

    public static void setListener(RuntimeErrorListener listener) {
        sListener = listener;
    }

    public static boolean onError(final String url, final Throwable t) {
        RuntimeErrorListener listener = sListener;
        if (listener != null) {
            return listener.onError(url, t);
        }
        return false;
    }
}
