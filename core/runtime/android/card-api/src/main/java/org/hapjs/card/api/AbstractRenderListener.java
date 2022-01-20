/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.card.api;

public abstract class AbstractRenderListener implements IRenderListener {
    @Override
    public final void onRenderException(int errorCode, String message) {
        onRenderFailed(errorCode, message);
    }
}
