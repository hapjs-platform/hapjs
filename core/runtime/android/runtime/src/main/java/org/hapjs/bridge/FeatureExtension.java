/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.bridge;

import java.util.Map;
import org.hapjs.common.resident.ResidentCallback;

/**
 * The interface of hybrid features. Any feature should implements this interface.
 */
public abstract class FeatureExtension extends AbstractExtension implements ResidentCallback {

    private static int sRequestBaseCode = Constants.FEATURE_REQUEST_CODE_BASE;
    private Map<String, String> mParams;

    protected static int getRequestBaseCode() {
        sRequestBaseCode += 100;
        return sRequestBaseCode;
    }

    public Map<String, String> getParams() {
        return mParams;
    }

    public void setParams(Map<String, String> params) {
        mParams = params;
    }

    public String getParam(String key) {
        return mParams == null ? null : mParams.get(key);
    }

    public void dispose(boolean force) {
    }

    @Override
    public ExtensionMetaData getMetaData() {
        return FeatureBridge.getFeatureMap().get(getName());
    }

    @Override
    public void onStopRunningInBackground() {
    }

    public boolean hasShownForegroundNotification() {
        return false;
    }

    public String getForegroundNotificationStopAction() {
        return null;
    }
}
