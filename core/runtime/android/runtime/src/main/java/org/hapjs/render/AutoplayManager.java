/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.render;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;

public class AutoplayManager {

    private Map<Integer, WeakReference<Autoplay>> mAutoplayMap;

    public void addAutoplay(int ref, Autoplay autoplay) {
        if (ref < 0 || autoplay == null) {
            return;
        }
        if (mAutoplayMap == null) {
            mAutoplayMap = new HashMap();
        }
        mAutoplayMap.put(ref, new WeakReference(autoplay));
    }

    public void removeAutoplay(int ref) {
        if (mAutoplayMap == null) {
            return;
        }
        mAutoplayMap.remove(ref);
    }

    public void startAll() {
        if (mAutoplayMap == null || mAutoplayMap.size() == 0) {
            return;
        }
        for (WeakReference<Autoplay> weakRef : mAutoplayMap.values()) {
            Autoplay autoplay = weakRef.get();
            if (autoplay != null) {
                autoplay.start();
            }
        }
    }

    public void stopAll() {
        if (mAutoplayMap == null || mAutoplayMap.size() == 0) {
            return;
        }
        for (WeakReference<Autoplay> weakRef : mAutoplayMap.values()) {
            Autoplay autoplay = weakRef.get();
            if (autoplay != null) {
                autoplay.stop();
            }
        }
    }
}
