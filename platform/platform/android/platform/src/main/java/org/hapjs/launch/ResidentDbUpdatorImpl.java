/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.launch;

import android.content.Context;
import org.hapjs.common.resident.ResidentManager.ResidentChangeListener;

public class ResidentDbUpdatorImpl implements ResidentChangeListener {

    @Override
    public void onResidentChange(Context context, String packagename, int residentType) {
        LauncherManager.updateResidentTypeAsync(context, packagename, residentType);
    }
}
