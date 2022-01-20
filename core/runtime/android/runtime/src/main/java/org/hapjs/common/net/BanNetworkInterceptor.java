/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.common.net;

import java.io.IOException;
import okhttp3.Interceptor;
import okhttp3.Response;
import org.hapjs.runtime.ProviderManager;

public class BanNetworkInterceptor implements Interceptor {

    @Override
    public Response intercept(Chain chain) throws IOException {
        BanNetworkProvider provider =
                ProviderManager.getDefault().getProvider(BanNetworkProvider.NAME);
        boolean shouldBanNetwork = provider != null && provider.shouldBanNetwork();
        return shouldBanNetwork
                ? provider.getBanNetworkResponse(chain.request())
                : chain.proceed(chain.request());
    }
}
