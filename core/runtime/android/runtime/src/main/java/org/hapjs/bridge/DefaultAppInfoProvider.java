/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.bridge;

import android.content.Context;
import java.io.File;
import org.hapjs.model.AppInfo;
import org.json.JSONObject;

public class DefaultAppInfoProvider implements AppInfoProvider {

    @Override
    public AppInfo create(Context context, String pkg) {
        return AppInfo.create(context, pkg);
    }

    @Override
    public AppInfo fromFile(File manifest) {
        return AppInfo.fromFile(manifest);
    }

    @Override
    public AppInfo parse(JSONObject manifestObject) {
        return AppInfo.parse(manifestObject);
    }
}
