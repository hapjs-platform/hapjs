/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.runtime;

import android.content.Context;
import androidx.multidex.MultiDex;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import org.hapjs.cache.CacheException;
import org.hapjs.cache.CacheStorage;
import org.hapjs.common.utils.FileUtils;

public class FeaturesApplication extends RuntimeApplication {
    public static final String APP_ID = "Helloworld";

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        MultiDex.install(this);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        CacheStorage.getInstance(this).removeCache(APP_ID);
        InputStream in = null;
        try {
            in = getAssets().open("Helloworld.rpk");
            File rpk = new File(getCacheDir(), "Helloworld.rpk");
            FileUtils.saveToFile(in, rpk);
            CacheStorage.getInstance(this).install(APP_ID, rpk.getAbsolutePath());
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (CacheException e) {
            throw new RuntimeException(e);
        } finally {
            FileUtils.closeQuietly(in);
        }
    }
}
