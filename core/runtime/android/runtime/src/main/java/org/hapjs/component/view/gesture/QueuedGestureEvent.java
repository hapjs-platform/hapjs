/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.component.view.gesture;

import org.hapjs.component.bridge.RenderEventCallback;

/*package*/ class QueuedGestureEvent {

    RenderEventCallback.EventData mEvent;
    QueuedGestureEvent mNext;

    /*pacakge*/ QueuedGestureEvent(RenderEventCallback.EventData event) {
        mEvent = event;
    }
}
