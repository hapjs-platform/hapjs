/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.render;

import java.util.concurrent.atomic.AtomicInteger;

public class IdGenerator {

    private static AtomicInteger mIndex = new AtomicInteger(0);

    public static int generatePageId() {
        return mIndex.incrementAndGet();
    }

    public static int generateAppId() {
        return mIndex.incrementAndGet();
    }
}
