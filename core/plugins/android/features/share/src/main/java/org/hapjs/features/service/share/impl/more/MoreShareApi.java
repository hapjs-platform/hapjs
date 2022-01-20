/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.features.service.share.impl.more;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.text.TextUtils;
import org.hapjs.common.utils.MediaUtils;
import org.hapjs.features.service.share.AbsShareApi;
import org.hapjs.features.service.share.Platform;
import org.hapjs.features.service.share.R;
import org.hapjs.features.service.share.ShareContent;
import org.hapjs.features.service.share.ShareListener;

public class MoreShareApi extends AbsShareApi {

    private static final int REQUEST_SYSTEM_SHARE = 30001;
    private ShareListener mShareListener;

    public MoreShareApi(Activity activity,
                        ShareContent content, Platform media) {
        super(activity, content, media);
    }

    @Override
    protected void onShare(ShareContent content, ShareListener listener) {
        Activity activity = getActicity();
        mShareListener = listener;

        Intent send = new Intent(Intent.ACTION_SEND);

        if (content.getShareType() == ShareContent.IMAGE_STYLE) {
            send.setType("image/jpeg");
            Uri uri = MediaUtils.getMediaContentUri(activity, content.getPackageName(),
                    "image/jpeg", content.getImageUri());
            send.putExtra(Intent.EXTRA_STREAM, uri);
        } else {
            send.setType("text/plain");
            StringBuilder sb = new StringBuilder();
            sb.append(content.getTitle());
            if (!TextUtils.isEmpty(content.getSummary())) {
                sb.append("\n");
                sb.append(content.getSummary());
            }
            if (!TextUtils.isEmpty(content.getMediaUrl())) {
                sb.append("\n");
                sb.append(content.getMediaUrl());
            }
            if (!TextUtils.isEmpty(content.getTargetUrl())) {
                sb.append("\n");
                sb.append(content.getTargetUrl());
            }
            send.putExtra(Intent.EXTRA_TEXT, sb.toString());
        }
        String title = activity.getString(R.string.share_dialog_title);
        Intent intent = Intent.createChooser(send, title);
        activity.startActivityForResult(intent, REQUEST_SYSTEM_SHARE);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (REQUEST_SYSTEM_SHARE == requestCode && mShareListener != null) {
            if (resultCode == Activity.RESULT_OK) {
                mShareListener.onResult(getPlatform());
            } else if (resultCode == Activity.RESULT_CANCELED) {
                mShareListener.onCancel(getPlatform());
            } else {
                mShareListener.onError(getPlatform(), "");
            }
        }
    }

    @Override
    public void release() {
    }
}