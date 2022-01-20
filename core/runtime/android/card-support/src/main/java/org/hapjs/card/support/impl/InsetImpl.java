/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.card.support.impl;

import android.app.Activity;
import org.hapjs.card.api.Inset;
import org.hapjs.runtime.HapEngine;

public class InsetImpl extends CardImpl implements Inset {

    InsetImpl(Activity activity, HapEngine.Mode mode) {
        super(activity, mode);
    }

    InsetImpl(Activity activity, String uri, HapEngine.Mode mode) {
        super(activity, uri, mode);
    }
}
