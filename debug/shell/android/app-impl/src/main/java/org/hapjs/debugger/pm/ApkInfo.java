/*
 * Copyright (c) 2021-2022, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.debugger.pm;

public class ApkInfo {

    public static final String TYPE_DEBUGGER = "debugger";
    public static final String TYPE_PLATFORM = "platform";

    private static final String DEBUGGER_PKG = "org.hapjs.debugger";
    private static final String MOCKUP_PKG = "org.hapjs.mockup";

    private String type;
    private int versionCode;
    private String url;
    private String name;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public int getVersionCode() {
        return versionCode;
    }

    public void setVersionCode(int versionCode) {
        this.versionCode = versionCode;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPkg() {
        if (TYPE_DEBUGGER.equals(type)) {
            return DEBUGGER_PKG;
        } else if (TYPE_PLATFORM.equals(type)) {
            return MOCKUP_PKG;
        }
        return "";
    }
}