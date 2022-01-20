/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.event;

public class ClearDataEvent implements Event {

    public static final String NAME = "ClearDataEvent";

    private String mPackageName;

    public ClearDataEvent(String packageName) {
        mPackageName = packageName;
    }

    public String getPackageName() {
        return mPackageName;
    }

    @Override
    public String getName() {
        return NAME;
    }
}
