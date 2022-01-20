/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.render.css.media;

public class MediaList {

    // or  ,(comma)
    private MediaQuery[] mMediaQueries;

    public MediaQuery[] getMediaQueries() {
        return mMediaQueries;
    }

    void setMediaQueries(MediaQuery[] mediaQueries) {
        this.mMediaQueries = mediaQueries;
    }

    /**
     * 媒体查询条件符合 返回true 否则false
     *
     * @return
     */
    public boolean getResult() {
        boolean result = false;
        for (MediaQuery mMediaQuery : mMediaQueries) {
            result = result || mMediaQuery.getResult();
        }
        return result;
    }

    /**
     * @return compare result changed
     */
    public boolean updateMediaPropertyInfo(MediaPropertyInfo info) {
        boolean before = getResult();
        for (MediaQuery mMediaQuery : mMediaQueries) {
            mMediaQuery.updateMediaPropertyInfo(info);
        }
        boolean after = getResult();
        return before != after;
    }
}
