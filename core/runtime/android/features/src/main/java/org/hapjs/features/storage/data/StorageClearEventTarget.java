/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.features.storage.data;

import android.text.TextUtils;
import org.hapjs.bridge.annotation.EventTargetAnnotation;
import org.hapjs.event.ClearDataEvent;
import org.hapjs.event.Event;
import org.hapjs.event.EventTarget;
import org.hapjs.features.storage.data.internal.StorageFactory;

@EventTargetAnnotation(eventNames = {ClearDataEvent.NAME})
public class StorageClearEventTarget implements EventTarget {

    @Override
    public void invoke(Event event) {
        if (event instanceof ClearDataEvent) {
            String pkg = ((ClearDataEvent) event).getPackageName();
            if (!TextUtils.isEmpty(pkg)) {
                StorageFactory.getInstance().clear(pkg);
            }
        }
    }
}
