/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.cache;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;


import android.content.Context;
import android.util.Log;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import java.io.File;
import java.util.Locale;
import org.hapjs.common.utils.FileUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Instrumentation test, which will execute on an Android device.
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
@RunWith(AndroidJUnit4.class)
public class CacheTest {

    String[] mNewFiles =
            new String[] {
                    "app.js",
                    "common/1.png",
                    "common/2.png",
                    "page1/page.js",
                    "page1/page.png",
                    "page2/page2.js",
                    "page2/page2.png",
            };
    String[] mNewData =
            new String[] {
                    "12", "21", "12", "12", "11", "11", "11",
            };
    private String mManifest =
            "{\n"
                    + "  \"package\": \"%s\",\n"
                    + "  \"name\": \"demo\",\n"
                    + "  \"versionName\": \"1.0.0\",\n"
                    + "  \"versionCode\": \"1\",\n"
                    + "  \"features\": [\n"
                    + "    { \"name\": \"system.network\" }\n"
                    + "  ],\n"
                    + "  \"permissions\": [\n"
                    + "    { \"origin\": \"*\" }\n"
                    + "  ],\n"
                    + "  \"config\": {\n"
                    + "    \"logLevel\": \"off\"\n"
                    + "  },\n"
                    + "  \"router\": {\n"
                    + "    \"entry\": \"Hello\",\n"
                    + "    \"pages\": {\n"
                    + "      \"Hello\": {\n"
                    + "        \"component\": \"hello\",\n"
                    + "        \"path\": \"/\"\n"
                    + "      }\n"
                    + "    }\n"
                    + "  }\n"
                    + "}\n";
    private String[] mFiles =
            new String[] {
                    "app.js", "common/bg.png", "common/title.png", "hello/hello.js",
                    "hello/hello.png"
            };
    private String[] mData = new String[] {"1", "1", "1", "1", "1"};
    private int mDataLength;
    private int mNewDataLength;

    {
        for (String d : mData) {
            mDataLength += d.length();
        }
    }

    {
        for (String d : mNewData) {
            mNewDataLength += d.length();
        }
    }

    @Test
    public void useAppContext() throws Exception {
        // Context of the app under test.
        Context appContext = InstrumentationRegistry.getTargetContext();

        assertEquals("org.hapjs.runtime.test", appContext.getPackageName());
    }

    @Test
    public void getCache() throws Exception {
        Context appContext = InstrumentationRegistry.getTargetContext();
        CacheStorage cacheStorage = CacheStorage.getInstance(appContext);
        Cache cache = cacheStorage.getCache("1234567890");
        testInstallCache(appContext, cacheStorage, cache);
    }

    @Test
    public void removeCache() throws Exception {
        Context appContext = InstrumentationRegistry.getTargetContext();
        CacheStorage cacheStorage = CacheStorage.getInstance(appContext);
        Cache cache = cacheStorage.getCache("xyz");
        testInstallCache(appContext, cacheStorage, cache);
        cache.remove();
        for (String file : mFiles) {
            testGetResourceNotHit(cache, file, null, false);
        }
        assertEquals(false, cache.ready());
    }

    @Before
    public void cleanCache() {
        Log.d("CacheTest", "cleanCache");
        Context appContext = InstrumentationRegistry.getTargetContext();
        FileUtils.rmRF(Cache.getResourceRootDir(appContext));
    }

    private void testInstallCache(Context appContext, CacheStorage cacheStorage, Cache cache)
            throws Exception {
        assertNotNull(cache);
        assertFalse(cache.ready());
        String manifest = String.format(Locale.US, mManifest, cache.getPackageName());
        PackageHelper.install(
                appContext, cacheStorage, cache.getPackageName(), manifest, mFiles, mData);
        assertTrue(cache.ready());

        for (String file : mFiles) {
            testGetResource(cache, file, null);
        }
        testGetResource(cache, "hello.js", "hello");
        testGetResource(cache, "hello.png", "hello");
        testGetResource(cache, "/common/bg.png", "hello");
        testGetResource(cache, "/common/title.png", "hello");
        testGetNonexistResource(cache);
        assertEquals(mDataLength + manifest.length(), cache.size());
    }

    private void testGetResource(Cache cache, String expectedResourcePath, String pageName)
            throws Exception {

        File file = cache.get(expectedResourcePath, pageName);
        assertEquals(true, cache.ready());
        assertTrue(file.getAbsolutePath().endsWith(expectedResourcePath));
        assertTrue(file.exists());
    }

    private void testGetResourceNotHit(
            Cache cache, String expectedResourcePath, String pageName, boolean cacheReady)
            throws Exception {

        assertEquals(cacheReady, cache.ready());
        File file = null;
        try {
            file = cache.get(expectedResourcePath, pageName);
        } catch (CacheException e) {
            assertEquals(e.getErrorCode(), CacheErrorCode.RESOURCE_UNAVAILABLE);
        }
        assertNull(file);
    }

    private void testGetNonexistResource(Cache cache) throws Exception {
        File file = null;
        try {
            file = cache.get("nonexistfile", null);
        } catch (CacheException e) {
            assertEquals(e.getErrorCode(), CacheErrorCode.RESOURCE_UNAVAILABLE);
        }
        assertNull(file);
    }
}
