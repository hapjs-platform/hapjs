/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.debugger.debug;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class CardDebugClientReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        CardDebugManager.getInstance(context).handleServerMessage(intent);
    }
}
