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
public class ClipboardTest extends AbstractTest {
    private final String TEXT = String.valueOf(System.currentTimeMillis());

    public ClipboardTest() {
        super(new Clipboard());
    }

    @Test
    public void testSetGet() throws JSONException {
        JSONObject params = new JSONObject();
        params.put(Clipboard.PARAM_KEY_TEXT, TEXT);
        Response response = invoke(Clipboard.ACTION_SET, params);
        Assert.assertEquals(Response.CODE_SUCCESS, response.getCode());

        response = invoke(Clipboard.ACTION_GET, null);
        Assert.assertEquals(Response.CODE_SUCCESS, response.getCode());
        String text =
                response.toJSON().getJSONObject("content").getString(Clipboard.PARAM_KEY_TEXT);
        Assert.assertEquals(TEXT, text);
    }
}
