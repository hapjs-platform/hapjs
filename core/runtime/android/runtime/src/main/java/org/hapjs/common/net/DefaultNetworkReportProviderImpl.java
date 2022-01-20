/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.common.net;

import android.content.Context;

public class DefaultNetworkReportProviderImpl implements NetworkReportProvider {

    public DefaultNetworkReportProviderImpl(Context context) {
    }

    @Override
    public int getNetworkReportLevel() {
        return NetworkReportManager.REPORT_LEVEL_NONE;
    }

    @Override
    public void reportNetworkAction(String source, String url, int level) {
        if (canReport(level)) {
            // TODO 厂家各自实现
        }
    }

    private boolean canReport(int level) {
        if ((level & getNetworkReportLevel()) != 0) {
            return true;
        }
        return false;
    }
}
