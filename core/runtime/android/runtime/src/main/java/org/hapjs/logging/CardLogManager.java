/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.logging;

import android.view.View;
import android.widget.TextView;
import org.hapjs.card.api.LogListener;
import org.hapjs.common.executors.Executors;
import org.hapjs.common.executors.ScheduledExecutor;

public class CardLogManager {

    private static ScheduledExecutor sExecutor = Executors.createSingleThreadExecutor();
    private static volatile LogListener sListener;

    public static void setListener(LogListener listener) {
        sListener = listener;
    }

    public static boolean hasListener() {
        return sListener != null;
    }

    /**
     * 统计click事件
     *
     * @param url  card的url，同一个应用可能会有多张卡片
     * @param view click事件的view
     */
    public static void logClickEvent(String url, String component, View view) {
        if (view == null || sListener == null) {
            return;
        }
        CharSequence text = getText(view);
        sExecutor.execute(
                new Runnable() {
                    @Override
                    public void run() {
                        LogListener listener = sListener;
                        if (listener != null) {
                            listener.onClickEvent(url, component,
                                    text != null ? text.toString() : "");
                        }
                    }
                });
    }

    private static CharSequence getText(View view) {
        if (view == null) {
            return "";
        }
        CharSequence text = view.getContentDescription();
        if (view instanceof TextView) {
            text = ((TextView) view).getText();
        }
        return text;
    }
}
