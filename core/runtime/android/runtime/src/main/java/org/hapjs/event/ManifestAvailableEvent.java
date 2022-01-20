/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.event;

public class ManifestAvailableEvent implements Event {
    public static final String NAME = "ManifestAvailableEvent";

    private String mPackageName;
    private Boolean mIsUpdate;

    public ManifestAvailableEvent(String packageName, boolean isUpdate) {
        mPackageName = packageName;
        mIsUpdate = isUpdate;
    }

    public String getPackageName() {
        return mPackageName;
    }

    public boolean isUpdate() {
        return mIsUpdate;
    }

    @Override
    public String getName() {
        return NAME;
    }
}
