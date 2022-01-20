/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.net;

import java.util.Map;

public interface NetLoaderProvider<T> {
    String NAME = "netloader";
    int METHOD_GET = 0;
    int METHOD_POST = 1;

    String getMenubarUrl();

    int getMenubarPostType();

    Map<String, String> getMenubarParams(Map<String, String> preParams);

    // for self params
    void initPrepareParams(Map<String, String> preParams);

    void loadData(
            String baseUrl, Map<String, String> params, DataLoadedCallback<T> callback, int method);

    NetLoadResult<T> loadDataSync(String baseUrl, Map<String, String> params);

    interface DataLoadedCallback<T> {
        void onSuccess(NetLoadResult<T> loadResult);

        void onFailure(NetLoadResult<T> loadResult);
    }
}
