/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.event;

public class ApplicationLaunchEvent implements Event {

    public static final String NAME = "ApplicationLaunchEvent";

    private String mPackageName;

    public ApplicationLaunchEvent(String packageName) {
        mPackageName = packageName;
    }

    @Override
    public String getName() {
        return NAME;
    }

    public String getPackageName() {
        return mPackageName;
    }
}
