/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.features.ad;

import org.hapjs.bridge.FeatureExtension;

public abstract class BaseAd extends FeatureExtension {

    public static final String ACTION_LOAD = "load";
    public static final String ACTION_SHOW = "show";
    public static final String ACTION_DESTROY = "destroy";

    public static final String ACTION_ON_LOAD = "onLoad";
    public static final String ACTION_OFF_LOAD = "offLoad";

    public static final String ACTION_ON_ERROR = "onError";
    public static final String ACTION_OFF_ERROR = "offError";

    public static final String ACTION_ON_CLOSE = "onClose";
    public static final String ACTION_OFF_CLOSE = "offClose";
}
