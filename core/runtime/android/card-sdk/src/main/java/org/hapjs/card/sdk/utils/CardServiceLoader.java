/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.card.sdk.utils;

import android.content.Context;
import android.util.Log;
import java.io.File;
import org.hapjs.card.api.CardService;
import org.hapjs.card.common.utils.CardClassLoaderHelper;
import org.hapjs.card.common.utils.CardConfigHelper;

public class CardServiceLoader {
    private static final String TAG = "CardServiceLoader";

    private static final String CARD_SERVICE_IMPL_CLASS = "org.hapjs.card.support.impl.CardServiceImpl";

    private static volatile CardService sService;

    public static CardService load(Context context) {
        if (sService == null) {
            synchronized (CardServiceLoader.class) {
                if (sService == null) {
                    if (CardConfigHelper.isLoadFromLocal(context)) {
                        sService = loadLocal();
                    } else {
                        sService = loadRemote(context);
                    }
                }
            }
        }
        return sService;
    }

    public static void clearCardServiceClass() {
        sService = null;
    }

    private static CardService loadLocal() {
        try {
            Class serviceClass = Class.forName(CARD_SERVICE_IMPL_CLASS);
            return (CardService) serviceClass.newInstance();
        } catch (Exception e) {
            Log.w(TAG, "Fail to create local CardService", e);
            return null;
        }
    }

    private static CardService loadRemote(final Context context) {
        File file = new File(context.getCacheDir().getParent(), "code_cache");
        if (!file.exists()) {
            file.mkdir();
        }

        /*if mockup and host app all have card-api and card-apk classes,
         * ensure card-api and card-sdk class use host app load，
         * so use CardClassLoader*/
        ClassLoader platformClassLoader = CardClassLoaderHelper.getClassLoader(context,
                CardServiceLoader.class.getClassLoader(),
                CardConfigHelper.getPlatform(context));
        try {
            Class serviceClass = Class.forName(CARD_SERVICE_IMPL_CLASS, true, platformClassLoader);
            return (CardService) serviceClass.newInstance();
        } catch (Exception e) {
            Log.w(TAG, "Fail to create remote CardService", e);
            return null;
        }
    }
}
