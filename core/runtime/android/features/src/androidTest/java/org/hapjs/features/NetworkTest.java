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
public class NetworkTest extends AbstractTest {
    public NetworkTest() {
        super(new Network());
    }

    @Test
    public void testGetType() throws JSONException {
        Response response = invoke(Network.ACTION_GET_TYPE, null);
        Assert.assertEquals(Response.CODE_SUCCESS, response.getCode());
        JSONObject content = response.toJSON().getJSONObject("content");
        Assert.assertTrue(content.has(Network.KEY_TYPE));
        Assert.assertTrue(content.has(Network.KEY_METERED));
    }

    @Test
    public void testSubscribe() throws JSONException {
        Response response = invoke(Network.ACTION_SUBSCRIBE, null);
        Assert.assertEquals(Response.CODE_SUCCESS, response.getCode());
        JSONObject content = response.toJSON().getJSONObject("content");
        Assert.assertTrue(content.has(Network.KEY_TYPE));
        Assert.assertTrue(content.has(Network.KEY_METERED));

        response = invoke(Network.ACTION_UNSUBSCRIBE, null);
        Assert.assertEquals(Response.CODE_SUCCESS, response.getCode());
    }

    @Test
    public void testUnsubscribe() throws JSONException {
        Response response = invoke(Network.ACTION_UNSUBSCRIBE, null);
        Assert.assertEquals(Response.CODE_SUCCESS, response.getCode());
    }
}
