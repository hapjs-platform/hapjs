/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.event;

public class EventTargetMetaData {

    private String[] eventNames;
    private String module;

    public EventTargetMetaData(String[] eventNames, String module) {
        this.eventNames = eventNames;
        this.module = module;
    }

    public String[] getEventNames() {
        return eventNames;
    }

    public String getModule() {
        return module;
    }
}
