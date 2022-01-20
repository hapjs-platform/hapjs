/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.features;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import org.hapjs.annotation.ManualTest;
import org.hapjs.bridge.Response;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class ShareTest extends AbstractTest {
    private static final String DATA = "123abc";

    public ShareTest() {
        super(new Share());
    }

    @Test
    @ManualTest
    public void testShare() throws JSONException {
        JSONObject params = new JSONObject();
        params.put(Share.PARAM_TYPE, "text/plain");
        params.put(Share.PARAM_DATA, DATA);
        Response response = invoke(Share.ACTION_SHARE, params);
        Assert.assertEquals(Response.CODE_CANCEL, response.getCode());
    }
}
