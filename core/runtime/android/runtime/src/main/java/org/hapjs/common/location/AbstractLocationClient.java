/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.common.location;

import static org.hapjs.common.location.LocationChangedListener.CODE_RESULT_RECEIVED;

import android.content.Context;

public abstract class AbstractLocationClient implements ILocationClient {

    protected Context mContext;
    protected LocationChangedListener mListener;

    public AbstractLocationClient(Context context) {
        mContext = context;
    }

    protected abstract HapLocation getCacheLocation();

    protected abstract void start();

    protected abstract void stop();

    @Override
    public void subscribe(boolean useCache, LocationChangedListener listener) {
        mListener = listener;
        if (useCache) {
            HapLocation location = getCacheLocation();
            if (location != null && mListener != null) {
                mListener.onLocationChanged(location, CODE_RESULT_RECEIVED);
                return;
            }
        }
        start();
    }

    @Override
    public void unsubscribe() {
        stop();
        mListener = null;
    }
}
