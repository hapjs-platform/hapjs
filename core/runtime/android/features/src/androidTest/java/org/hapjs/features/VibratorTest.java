/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.features;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import junit.framework.Assert;
import org.hapjs.bridge.Response;
import org.json.JSONException;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class VibratorTest extends AbstractTest {
    public VibratorTest() {
        super(new Vibrator());
    }

    @Test
    public void testVibrate() throws JSONException {
        Response response = invoke(Vibrator.ACTION_VIBRATE, null);
        Assert.assertEquals(Response.CODE_SUCCESS, response.getCode());
    }
}
