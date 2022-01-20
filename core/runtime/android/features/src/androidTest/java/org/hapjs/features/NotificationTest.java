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
public class NotificationTest extends AbstractTest {
    public NotificationTest() {
        super(new Notification());
    }

    @Test
    public void testShow() throws JSONException {
        JSONObject params = new JSONObject();
        params.put(Notification.PARAM_CONTENT_TITLE, "title");
        params.put(Notification.PARAM_CONTENT_TEXT, "text");
        JSONObject clickAction = new JSONObject();
        clickAction.put(Notification.PARAM_URI, "http://www.mi.com");
        params.put(Notification.PARAM_CLICK_ACTION, clickAction);
        Response response = invoke(Notification.ACTION_SHOW, params);
        Assert.assertEquals(Response.CODE_SUCCESS, response.getCode());
    }
}
