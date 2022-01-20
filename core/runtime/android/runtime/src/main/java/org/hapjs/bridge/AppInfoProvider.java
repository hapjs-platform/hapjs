/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.bridge;

import android.content.Context;
import java.io.File;
import org.hapjs.model.AppInfo;
import org.json.JSONObject;

public interface AppInfoProvider {
    String NAME = "AppInfoProvider";

    AppInfo create(Context context, String pkg);

    AppInfo fromFile(File manifest);

    AppInfo parse(JSONObject manifestObject);
}
