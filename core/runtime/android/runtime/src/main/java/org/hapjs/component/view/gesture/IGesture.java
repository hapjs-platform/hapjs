/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.component.view.gesture;

import android.view.MotionEvent;
import androidx.annotation.NonNull;
import org.hapjs.component.Component;

public interface IGesture {
    boolean onTouch(MotionEvent event);

    void registerEvent(String eventType);

    void removeEvent(String eventType);

    void addFrozenEvent(String eventType);

    void removeFrozenEvent(String eventType);

    void updateComponent(@NonNull Component component);

    void setIsWatchingLongPress(boolean isWatchingLongPress);

    void setIsWatchingClick(boolean isWatchingClick);
}
