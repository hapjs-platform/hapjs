/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.common.net;

import org.hapjs.runtime.ProviderManager;

public class NetworkReportManager {

    public static final int REPORT_LEVEL_NONE = 0;
    public static final int REPORT_LEVEL_NORMAL = 1 << 1;
    public static final int REPORT_LEVEL_IMAGE = 1 << 2;
    public static final int REPORT_LEVEL_SOCKET = 1 << 3;
    public static final String KEY_FONT_FILE_MANAGER = "FontFileManager";
    public static final String KEY_FRESCO = "Fresco";
    public static final String KEY_DEFAULT_NET_LOADER = "DefaultNetLoaderProvider";
    public static final String KEY_VIDEO = "Video";
    private NetworkReportProvider mNetworkReportProvider;

    private NetworkReportManager() {
        mNetworkReportProvider =
                ProviderManager.getDefault().getProvider(NetworkReportProvider.NAME);
    }

    public static NetworkReportManager getInstance() {
        return NetworkReportManager.Holder.INSTANCE;
    }

    public void reportNetwork(String source, String url) {
        reportNetwork(source, url, REPORT_LEVEL_NORMAL);
    }

    public void reportNetwork(String source, String url, int level) {
        if (null != mNetworkReportProvider) {
            mNetworkReportProvider.reportNetworkAction(source, url, level);
        }
    }

    private static class Holder {
        static final NetworkReportManager INSTANCE = new NetworkReportManager();
    }
}
