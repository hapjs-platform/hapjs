/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.bridge;

import android.os.SystemClock;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import org.hapjs.common.executors.Executors;

class HostConnectionMessageCache {
    private static final long MAX_CACHE_TIME = 3000;
    private static final long MIN_TRIM_TIME = 1000;

    private WeakHashMap<HybridManager, List<Message>> mMessages = new WeakHashMap<>();

    public synchronized void addMessage(HybridManager hybridManager, Message message) {
        boolean isEmpty = isEmpty();

        List<Message> messages = mMessages.get(hybridManager);
        if (messages == null) {
            messages = new LinkedList<>();
            mMessages.put(hybridManager, messages);
        }
        messages.add(message);

        if (isEmpty) {
            scheduleTrim(MAX_CACHE_TIME);
        }
    }

    private synchronized void scheduleTrim(long delay) {
        Executors.scheduled()
                .executeWithDelay(
                        new Runnable() {

                            @Override
                            public void run() {
                                long nextTrimDelay = trim();
                                if (nextTrimDelay >= 0) {
                                    scheduleTrim(Math.max(MIN_TRIM_TIME, nextTrimDelay));
                                }
                            }
                        },
                        delay);
    }

    private synchronized long trim() {
        long minAddedTime = Long.MAX_VALUE;
        long elapsedRealTime = SystemClock.elapsedRealtime();
        Iterator<Map.Entry<HybridManager, List<Message>>> allMsgItr =
                mMessages.entrySet().iterator();
        while (allMsgItr.hasNext()) {
            Map.Entry<HybridManager, List<Message>> entry = allMsgItr.next();
            List<Message> messages = entry.getValue();
            if (messages != null && !messages.isEmpty()) {
                Iterator<Message> itr = messages.iterator();
                while (itr.hasNext() && elapsedRealTime - itr.next().addedTime > MAX_CACHE_TIME) {
                    itr.remove();
                }
                if (!messages.isEmpty()) {
                    if (minAddedTime > messages.get(0).addedTime) {
                        minAddedTime = messages.get(0).addedTime;
                    }
                }
            }
            if (messages == null || messages.isEmpty()) {
                allMsgItr.remove();
            }
        }
        return minAddedTime == Long.MAX_VALUE ? -1 :
                (elapsedRealTime - minAddedTime - MAX_CACHE_TIME);
    }

    public synchronized List<Message> retriveMessage(HybridManager hybridManager) {
        return mMessages.remove(hybridManager);
    }

    public synchronized void clear() {
        mMessages.clear();
    }

    private boolean isEmpty() {
        for (List<Message> messages : mMessages.values()) {
            if (messages != null && !messages.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    static class Message {
        String content;
        private long addedTime;

        Message(String content) {
            this.content = content;
            this.addedTime = SystemClock.elapsedRealtime();
        }
    }

    static class JsMessage extends Message {

        int code;

        JsMessage(int code, String content) {
            super(content);
            this.code = code;
        }
    }

    static class HostMessage extends Message {

        Callback callback;

        HostMessage(String content, Callback callback) {
            super(content);
            this.callback = callback;
        }
    }
}
