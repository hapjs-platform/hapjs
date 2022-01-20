/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.common.net;

public interface NetworkReportProvider {

    String NAME = "NetworkReport";

    int getNetworkReportLevel();

    void reportNetworkAction(String source, String url, int level);
}
