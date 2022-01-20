/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.widgets.view.camera;

import android.net.Uri;

public class CameraData {
    private Uri imageUrl = null;
    private Uri thumbnail = null;
    private int retCode = -1;
    private String msg = "";

    public Uri getUrl() {
        return imageUrl;
    }

    public void setUrl(Uri imageUrl) {
        this.imageUrl = imageUrl;
    }

    public int getRetCode() {
        return retCode;
    }

    public void setRetCode(int retCode) {
        this.retCode = retCode;
    }

    public Uri getThumbnail() {
        return thumbnail;
    }

    public void setThumbnail(Uri thumbnail) {
        this.thumbnail = thumbnail;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }
}
