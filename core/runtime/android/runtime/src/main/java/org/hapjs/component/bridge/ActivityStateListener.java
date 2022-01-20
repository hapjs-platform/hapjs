/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.component.bridge;

public interface ActivityStateListener {

    void onActivityCreate();

    void onActivityStart();

    void onActivityResume();

    void onActivityPause();

    void onActivityStop();

    void onActivityDestroy();
}
