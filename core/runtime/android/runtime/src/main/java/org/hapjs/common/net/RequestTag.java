/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.common.net;

public class RequestTag {
    private String packageName;

    private int requestHashcode;

    public RequestTag(String packageName) {
        this.packageName = packageName;
    }

    public String getPackageName() {
        return packageName;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    public int getRequestHashcode() {
        return requestHashcode;
    }

    public void setRequestHashcode(int hashcode) {
        requestHashcode = hashcode;
    }
}
