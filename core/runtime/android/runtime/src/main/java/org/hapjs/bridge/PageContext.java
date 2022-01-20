/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.bridge;

import android.text.TextUtils;

/**
 * Hybrid invocation context for current page. Hold necessary information needed by feature.
 */
public class PageContext {

    private String id;
    private String url;

    /**
     * Get id.
     *
     * @return id.
     */
    public String getId() {
        return id;
    }

    /**
     * Set id.
     *
     * @param id id.
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * Get url.
     *
     * @return url.
     */
    public String getUrl() {
        return url;
    }

    /**
     * Set url.
     *
     * @param url url.
     */
    public void setUrl(String url) {
        this.url = url;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((id == null) ? 0 : id.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        PageContext other = (PageContext) obj;
        // String compare should use "equals" instead of "=="
        return TextUtils.equals(id, other.id);
    }
}
