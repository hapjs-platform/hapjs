/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.debugger;

import android.app.Application;
import android.os.Build;
import org.hapjs.debugger.card.CardHelper;
import org.hapjs.debugger.utils.ProcessUtils;

public class DebuggerApplication extends Application {

    private static Application sContext;

    public static Application getInstance() {
        return sContext;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        sContext = this;
        String process = ProcessUtils.getCurrentProcessName();
        if (!getPackageName().equals(process)
                && CardHelper.isCardProcess(this, process)) {
            CardHelper.initCard(this);
        }
    }
}
