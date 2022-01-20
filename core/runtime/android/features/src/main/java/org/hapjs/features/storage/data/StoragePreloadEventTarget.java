/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.features.storage.data;

import org.hapjs.bridge.annotation.EventTargetAnnotation;
import org.hapjs.event.ApplicationLaunchEvent;
import org.hapjs.event.Event;
import org.hapjs.event.EventTarget;
import org.hapjs.features.storage.data.internal.StorageFactory;
import org.hapjs.runtime.HapEngine;

@EventTargetAnnotation(eventNames = {ApplicationLaunchEvent.NAME})
public class StoragePreloadEventTarget implements EventTarget {

    @Override
    public void invoke(Event event) {
        if (event instanceof ApplicationLaunchEvent) {
            StorageFactory.getInstance()
                    .preload(
                            HapEngine.getInstance(((ApplicationLaunchEvent) event).getPackageName())
                                    .getApplicationContext());
        }
    }
}
