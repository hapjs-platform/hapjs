/*
 * Copyright (c) 2022, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.hapjs.bridge.provider.webview;

import java.util.ArrayList;
import java.util.List;

public class WebviewSettingProviderImpl implements WebviewSettingProvider {

    @Override
    public List<String> getJsFunctionNameList() {
        List<String> functionNameList = new ArrayList<>(2);
        functionNameList.add(FUNCTION_GET_ENV);
        functionNameList.add(FUNCTION_SCAN);
        return functionNameList;
    }
}
