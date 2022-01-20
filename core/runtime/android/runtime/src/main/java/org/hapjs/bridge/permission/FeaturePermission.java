/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.bridge.permission;

public class FeaturePermission {

    private String uri;
    private boolean applySubdomains;
    private boolean forbidden;

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    public boolean isApplySubdomains() {
        return applySubdomains;
    }

    public void setApplySubdomains(boolean applySubdomains) {
        this.applySubdomains = applySubdomains;
    }

    public boolean isForbidden() {
        return forbidden;
    }

    public void setForbidden(boolean forbidden) {
        this.forbidden = forbidden;
    }
}
