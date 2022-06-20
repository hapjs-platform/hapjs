/*
 * Copyright (c) 2022, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.hapjs.model.videodata;

import java.util.HashMap;

public class VideoCacheManager {
    private final HashMap<Integer, HashMap<String, VideoCacheData>> mCacheVideoData;
    private final HashMap<Integer, Boolean> mCurrentPageObtainPlayer;

    public static VideoCacheManager getInstance() {
        return VideoCacheHolder.INSTANCE;
    }

    private static class VideoCacheHolder {
        final static VideoCacheManager INSTANCE = new VideoCacheManager();
    }

    private VideoCacheManager() {
        mCacheVideoData = new HashMap<Integer, HashMap<String, VideoCacheData>>();
        mCurrentPageObtainPlayer = new HashMap<Integer, Boolean>();
    }

    public void putPageObtainPlayer(Integer pageId, boolean isObtainPlayer) {
        if (null != pageId) {
            mCurrentPageObtainPlayer.put(pageId, isObtainPlayer);
        }
    }

    public Boolean getPageObtainPlayer(Integer pageId) {
        Boolean obtainPlayer = false;
        if (null != pageId) {
            obtainPlayer = mCurrentPageObtainPlayer.get(pageId);
        }
        return obtainPlayer;
    }

    public void putVideoData(Integer pageId, String uri, VideoCacheData videoCacheData) {
        if (null != pageId && null != uri && null != videoCacheData) {
            HashMap<String, VideoCacheData> tmpPageCacheData = mCacheVideoData.get(pageId);
            if (null == tmpPageCacheData) {
                tmpPageCacheData = new HashMap<String, VideoCacheData>();
            }
            tmpPageCacheData.put(uri, videoCacheData);
            mCacheVideoData.put(pageId, tmpPageCacheData);
        }

    }

    public void clearVideoData(Integer pageId) {
        if (null != pageId) {
            HashMap<String, VideoCacheData> tmpPageCacheData = mCacheVideoData.get(pageId);
            if (null != tmpPageCacheData) {
                mCacheVideoData.remove(pageId);
                tmpPageCacheData.clear();
            }
            mCurrentPageObtainPlayer.remove(pageId);
        }
    }

    public void removeCacheVideoData(Integer pageId, String uri) {
        if (null != pageId) {
            HashMap<String, VideoCacheData> tmpPageCacheData = mCacheVideoData.get(pageId);
            if (null != tmpPageCacheData) {
                tmpPageCacheData.remove(uri);
            }
        }
    }

    public void clearAllVideoData() {
        if (null != mCacheVideoData) {
            mCacheVideoData.clear();
        }
        if (null != mCurrentPageObtainPlayer) {
            mCurrentPageObtainPlayer.clear();
        }
    }

    public VideoCacheData getVideoData(Integer pageId, String uri) {
        if (null != pageId && null != uri) {
            HashMap<String, VideoCacheData> tmpPageCacheData = mCacheVideoData.get(pageId);
            if (null != tmpPageCacheData) {
                return tmpPageCacheData.get(uri);
            }
        }
        return null;
    }

}
