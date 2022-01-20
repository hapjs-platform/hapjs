/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.features.storage;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;


import androidx.test.ext.junit.runners.AndroidJUnit4;
import org.hapjs.bridge.ApplicationContext;
import org.hapjs.features.storage.data.internal.LocalStorage;
import org.hapjs.runtime.HapEngine;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Instrumentation test, which will execute on an Android device.
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
@RunWith(AndroidJUnit4.class)
public class LocalStorageTest {
    @Test
    public void testSetGet() {
        ApplicationContext appContext = HapEngine.getInstance("abcdefg").getApplicationContext();
        LocalStorage localStorage = new LocalStorage(appContext);
        String key = "somekey";
        String expectedValue = "somevalue";
        localStorage.set(key, expectedValue);
        String actualValue = localStorage.get(key);
        assertEquals(expectedValue, actualValue);

        expectedValue = "someothervalue";
        localStorage.set(key, expectedValue);
        actualValue = localStorage.get(key);
        assertEquals(expectedValue, actualValue);

        String newKey = "newKey";
        String newExpectedValue = "newValue";
        localStorage.set(newKey, newExpectedValue);
        actualValue = localStorage.get(newKey);
        assertEquals(newExpectedValue, actualValue);

        localStorage.clear();
        assertNull(localStorage.get(key));
        assertNull(localStorage.get(newKey));

        String name = "name";
        String abcdef = "abcdef";
        localStorage.set(name, abcdef);
        assertEquals(abcdef, localStorage.get(name));

        localStorage.set(name, "");
        assertEquals("", localStorage.get(name));

        String age = "age";
        String value = "18";
        localStorage.set(age, value);
        assertEquals(value, localStorage.get(age));

        localStorage.set(age, null);
        assertNull(localStorage.get(age));
    }
}
