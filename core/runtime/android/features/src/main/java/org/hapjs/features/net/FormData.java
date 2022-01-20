/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.features.net;

public class FormData {
    public static final String KEY_DATA_NAME = "name";
    public static final String KEY_DATA_VALUE = "value";

    public String name;
    public String value;

    public FormData(String name, String value) {
        this.name = name;
        this.value = value;
    }
}
