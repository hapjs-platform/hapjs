/*
 * Copyright (c) 2023, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.widgets.adcustom;

import android.content.Context;

import org.hapjs.bridge.HybridView;

public class AdCustomDataProviderImpl implements AdCustomDataProvider {
    private static final String TAG = "AdCustomDataProviderImpl";

    @Override
    public void onComponentDestroy(String adUnitId, int componentRef) {

    }

    @Override
    public void loadCustomAd(Context context, String adUnitId, int ref, AdCustom adCustom) {
        if (adCustom != null) {
            adCustom.onError(ref, 2000, "not available");
        }
    }

    @Override
    public void init(HybridView hybridView) {

    }

    @Override
    public void onLowMemory() {

    }
}
