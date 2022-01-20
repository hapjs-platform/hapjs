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
public class GeolocationTest extends AbstractTest {
    public GeolocationTest() {
        super(new Geolocation());
    }

    @Test
    public void testGet() throws JSONException {
        Response response = invoke(Geolocation.ACTION_GET_LOCATION, null);
        Assert.assertEquals(Response.CODE_SUCCESS, response.getCode());
        JSONObject content = response.toJSON().getJSONObject("content");
        Assert.assertTrue(content.has(Geolocation.RESULT_LATITUDE));
        Assert.assertTrue(content.has(Geolocation.RESULT_LONGITUDE));
    }

    @Test
    public void testSubscribe() throws JSONException {
        Response response = invoke(Geolocation.ACTION_SUBSCRIBE, null);
        Assert.assertEquals(Response.CODE_SUCCESS, response.getCode());
        JSONObject content = response.toJSON().getJSONObject("content");
        Assert.assertTrue(content.has(Geolocation.RESULT_LATITUDE));
        Assert.assertTrue(content.has(Geolocation.RESULT_LONGITUDE));

        response = invoke(Geolocation.ACTION_UNSUBSCRIBE, null);
        Assert.assertEquals(Response.CODE_SUCCESS, response.getCode());
    }

    @Test
    public void testUnsubscribe() throws JSONException {
        Response response = invoke(Geolocation.ACTION_UNSUBSCRIBE, null);
        Assert.assertEquals(Response.CODE_SUCCESS, response.getCode());
    }
}
