/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.card.api.debug;

import android.content.Intent;

public interface CardDebugService {
    void setCardDebugHost(CardDebugHost host);

    void onCreate();

    int onStartCommand(Intent intent, int flags, int startId);

    void onDestroy();

    android.os.IBinder onBind(Intent intent);

    boolean onUnbind(Intent intent);

}
