/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.features;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import org.hapjs.bridge.Response;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class SensorTest extends AbstractTest {
    public SensorTest() {
        super(new Sensor());
    }

    @Test
    public void testSubscribeAccelerometer() throws JSONException {
        Response response = invoke(Sensor.ACTION_SUBSCRIBE_ACCELEROMETER, null);
        Assert.assertEquals(Response.CODE_SUCCESS, response.getCode());
        JSONObject content = response.toJSON().getJSONObject("content");
        Assert.assertTrue(content.has(Sensor.PARAM_X));

        response = invoke(Sensor.ACTION_UNSUBSCRIBE_ACCELEROMETER, null);
        Assert.assertEquals(Response.CODE_SUCCESS, response.getCode());
    }

    @Test
    public void testUnsubscribeAccelerometer() throws JSONException {
        Response response = invoke(Sensor.ACTION_UNSUBSCRIBE_ACCELEROMETER, null);
        Assert.assertEquals(Response.CODE_SUCCESS, response.getCode());
    }

    @Test
    public void testSubscribeCompass() throws JSONException {
        Response response = invoke(Sensor.ACTION_SUBSCRIBE_COMPASS, null);
        Assert.assertEquals(Response.CODE_SUCCESS, response.getCode());
        JSONObject content = response.toJSON().getJSONObject("content");
        Assert.assertTrue(content.has(Sensor.PARAM_DIRECTION));

        response = invoke(Sensor.ACTION_UNSUBSCRIBE_COMPASS, null);
        Assert.assertEquals(Response.CODE_SUCCESS, response.getCode());
    }

    @Test
    public void testUnsubscribeCompass() throws JSONException {
        Response response = invoke(Sensor.ACTION_UNSUBSCRIBE_COMPASS, null);
        Assert.assertEquals(Response.CODE_SUCCESS, response.getCode());
    }
}
