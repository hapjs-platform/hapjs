/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.features.service.wbaccount;

public class WBAuthParams {

    public String appKey;
    public String redirectUri;
    public String scope;

    public WBAuthParams(String appKey, String redirectUri, String scope) {
        this.appKey = appKey;
        this.redirectUri = redirectUri;
        this.scope = scope;
    }
}
