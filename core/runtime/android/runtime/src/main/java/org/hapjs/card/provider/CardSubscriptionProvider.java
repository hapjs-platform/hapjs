/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.card.provider;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.net.Uri;
import org.hapjs.model.AppInfo;

public interface CardSubscriptionProvider {
    String NAME = "CardSubscriptionProvider";

    int checkState(Context context, AppInfo appInfo, String cardPath) throws Exception;

    boolean addCard(Context context, AppInfo appInfo, String cardPath) throws Exception;

    Dialog createDialog(
            Activity activity,
            String name,
            String description,
            Uri illustration,
            DialogInterface.OnClickListener listener);
}
