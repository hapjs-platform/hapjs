/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.features.service.share;

import android.net.Uri;
import android.text.TextUtils;

public class ShareContent {
    public static final int TEXT_IMAGE_STYLE = 0;
    public static final int TEXT_STYLE = 1;
    public static final int IMAGE_STYLE = 2;
    public static final int MUSIC_STYLE = 3;
    public static final int VIDEO_STYLE = 4;

    private int shareType;
    private String title;
    private String summary;
    private String targetUrl;
    private Uri imageUri;
    private String mediaUrl;

    private String appName;
    private String packageName;
    private String weiboKey;
    private String wxKey;
    private String qqKey;

    private String extra;
    private boolean isImgOnline;  //是否需要下载在线图片.

    public boolean getIsImgOnline() {
        return isImgOnline;
    }

    public ShareContent setIsImgOnline(boolean imgOnline) {
        isImgOnline = imgOnline;
        return this;
    }

    public String getTitle() {
        return title;
    }

    public ShareContent setTitle(String title) {
        this.title = title;
        return this;
    }


    public String getAppName() {
        return appName;
    }

    public ShareContent setAppName(String appName) {
        this.appName = appName;
        return this;
    }

    public String getPackageName() {
        return packageName;
    }

    public ShareContent setPackageName(String packageName) {
        this.packageName = packageName;
        return this;
    }

    public String getMediaUrl() {
        return mediaUrl;
    }

    public ShareContent setMediaUrl(String mediaUrl) {
        this.mediaUrl = mediaUrl;
        return this;
    }

    public String getTargetUrl() {
        return targetUrl;
    }

    public ShareContent setTargetUrl(String targetUrl) {
        this.targetUrl = targetUrl;
        return this;
    }

    public String getSummary() {
        return summary;
    }

    public ShareContent setSummary(String summary) {
        this.summary = summary;
        return this;
    }

    public int getShareType() {
        return shareType;
    }

    public ShareContent setShareType(int shareType) {
        this.shareType = shareType;
        return this;
    }

    public Uri getImageUri() {
        return imageUri;
    }

    public ShareContent setImageUri(Uri imageUri) {
        this.imageUri = imageUri;
        return this;
    }

    public String getExtra() {
        return extra;
    }

    public ShareContent setExtra(String extra) {
        this.extra = extra;
        return this;
    }

    public final boolean checkArgs() {
        switch (shareType) {
            case TEXT_STYLE:
                if (TextUtils.isEmpty(title)) {
                    return false;
                }
                break;
            case MUSIC_STYLE:
            case VIDEO_STYLE:
                if (TextUtils.isEmpty(title) || TextUtils.isEmpty(targetUrl) || TextUtils.isEmpty(mediaUrl)) {
                    return false;
                }
                break;
            case IMAGE_STYLE:
                if (imageUri == null) {
                    return false;
                }
                break;
            default:
                if (TextUtils.isEmpty(title) || TextUtils.isEmpty(targetUrl)) {
                    return false;
                }
                break;
        }
        return true;
    }
}