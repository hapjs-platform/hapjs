/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.debugger.utils;

import android.view.View;
import java.util.ArrayList;
import java.util.List;

public class EasterEggUtils {
    private static final int EASTER_EGG_CLICK_TIMES = 5;
    private static final int EASTER_EGG_CLICK_DURATION = 2 * 1000;

    public static boolean addEasterEgg(View view, final EasterEggListener listener) {
        if (view.hasOnClickListeners()) {
            return false;
        }
        view.setOnClickListener(new View.OnClickListener() {
            private List<Long> clickTs = new ArrayList<>();

            @Override
            public void onClick(View v) {
                long currTs = System.currentTimeMillis();
                if (clickTs.size() == EASTER_EGG_CLICK_TIMES) {
                    clickTs.remove(0);
                }
                clickTs.add(currTs);
                if (clickTs.size() == EASTER_EGG_CLICK_TIMES
                        && currTs - clickTs.get(0) < EASTER_EGG_CLICK_DURATION) {
                    listener.onTrigger(v);
                    clickTs.clear();
                }
            }
        });
        return true;
    }

    public interface EasterEggListener {
        void onTrigger(View view);
    }
}
