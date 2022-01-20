/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.bridge.storage;

import android.database.sqlite.SQLiteDatabase;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import org.hapjs.bridge.ApplicationContext;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class ApplicationSettingsTest {
    private static final String APP = "com.example.app";

    private ApplicationContext mApplicationContext;

    @Before
    public void setup() {
        mApplicationContext =
                new ApplicationContext(InstrumentationRegistry.getTargetContext(), APP);
        SQLiteDatabase db =
                new ApplicationSettingsDatabaseHelper(mApplicationContext).getWritableDatabase();
        db.delete(ApplicationSettingsDatabaseHelper.TABLE_SETTINGS, null, null);
        db.close();
    }

    @Test
    public void testBoolean() throws Exception {
        boolean value =
                ApplicationSettings.getInstance(mApplicationContext).getBoolean("Boolean", false);
        Assert.assertTrue(!value);
        boolean result =
                ApplicationSettings.getInstance(mApplicationContext).putBoolean("Boolean", true);
        Assert.assertTrue(result);
        value = ApplicationSettings.getInstance(mApplicationContext).getBoolean("Boolean", false);
        Assert.assertTrue(value);
    }

    @Test
    public void testFloat() throws Exception {
        float value = ApplicationSettings.getInstance(mApplicationContext).getFloat("Float", 0f);
        Assert.assertTrue(value == 0f);
        boolean result = ApplicationSettings.getInstance(mApplicationContext).putFloat("Float", 1f);
        Assert.assertTrue(result);
        value = ApplicationSettings.getInstance(mApplicationContext).getFloat("Float", 0f);
        Assert.assertTrue(value == 1f);
    }

    @Test
    public void testInt() throws Exception {
        int value = ApplicationSettings.getInstance(mApplicationContext).getInt("Int", 0);
        Assert.assertEquals(0, value);
        boolean result = ApplicationSettings.getInstance(mApplicationContext).putInt("Int", 1);
        Assert.assertTrue(result);
        value = ApplicationSettings.getInstance(mApplicationContext).getInt("Int", 0);
        Assert.assertEquals(1, value);
    }

    @Test
    public void testLong() throws Exception {
        long value = ApplicationSettings.getInstance(mApplicationContext).getLong("Long", 0l);
        Assert.assertTrue(value == 0l);
        boolean result = ApplicationSettings.getInstance(mApplicationContext).putLong("Long", 1l);
        Assert.assertTrue(result);
        value = ApplicationSettings.getInstance(mApplicationContext).getLong("Long", 0l);
        Assert.assertTrue(value == 1l);
    }

    @Test
    public void testString() throws Exception {
        String value =
                ApplicationSettings.getInstance(mApplicationContext).getString("String", null);
        Assert.assertNull(value);
        value = ApplicationSettings.getInstance(mApplicationContext).getString("String", "text");
        Assert.assertEquals("text", value);
        boolean result =
                ApplicationSettings.getInstance(mApplicationContext).putString("String", "text");
        Assert.assertTrue(result);
        value = ApplicationSettings.getInstance(mApplicationContext).getString("String", null);
        Assert.assertEquals("text", value);
        result = ApplicationSettings.getInstance(mApplicationContext).putString("String", null);
        Assert.assertTrue(result);
        value = ApplicationSettings.getInstance(mApplicationContext).getString("String", null);
        Assert.assertNull(value);
        value = ApplicationSettings.getInstance(mApplicationContext).getString("String", "text");
        Assert.assertEquals("text", value);
    }
}
