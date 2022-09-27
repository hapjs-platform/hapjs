/*
 * Copyright (c) 2022, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.bridge.provider.webview;

import java.util.List;

public interface WebviewSettingProvider {
    String NAME = "WebviewSettingProvider";
    String FUNCTION_GET_ENV = "getEnv";
    String FUNCTION_SCAN = "scan";
    List<String> getJsFunctionNameList();
}
