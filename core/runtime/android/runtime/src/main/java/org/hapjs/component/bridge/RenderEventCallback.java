/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.component.bridge;

import android.net.Uri;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import org.hapjs.component.Component;

public interface RenderEventCallback {

    void onJsEventCallback(
            int pageId,
            int ref,
            String eventName,
            Component component,
            Map<String, Object> params,
            Map<String, Object> attributes);

    void onJsMultiEventCallback(int pageId, List<EventData> events);

    void onJsMethodCallback(int pageId, String callbackId, Object... params);

    void onJsException(Exception exception);

    void addActivityStateListener(ActivityStateListener listener);

    void removeActivityStateListener(ActivityStateListener listener);

    Uri getCache(String resourcePath);

    void onPostRender();

    Uri getUnderlyingUri(String internalPath);

    void loadUrl(String url);

    boolean shouldOverrideUrlLoading(String url, String sourceH5, int pageId);

    File createFileOnCache(String prefix, String suffix) throws IOException;

    void onPageReachTop();

    void onPageReachBottom();

    void onPageScroll(int scrollTop);

    interface EventPostListener {
        void finish();
    }

    class EventData {
        public int pageId;
        public int elementId;
        public String eventName;
        public Map<String, Object> params;
        public Map<String, Object> attributes;

        public EventData(
                int pageId,
                int elementId,
                String eventName,
                Map<String, Object> params,
                Map<String, Object> attributes) {
            this.pageId = pageId;
            this.elementId = elementId;
            this.eventName = eventName;
            this.params = params;
            this.attributes = attributes;
        }
    }
}
