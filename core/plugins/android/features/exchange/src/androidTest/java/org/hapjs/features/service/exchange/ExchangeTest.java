/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.features.service.exchange;

import android.content.Context;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import junit.framework.Assert;
import org.hapjs.features.service.exchange.common.ExchangeUriProvider;
import org.hapjs.features.service.exchange.common.ExchangeUtils;
import org.json.JSONException;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class ExchangeTest {

    private static String getSign(String pkg) {
        return pkg + ".sign";
    }

    @Test
    public void testExchange() throws JSONException {
        Context appContext = InstrumentationRegistry.getTargetContext();
        ExchangeUriProvider.setProvider(
                new HapUriProvider() {
                    @Override
                    protected String getHapAppSignDigest(Context context, String pkg) {
                        return getSign(pkg);
                    }
                });

        String pkg1 = "pkg1";
        String sign1 = getSign(pkg1);
        String pkg2 = "pkg2";
        String sign2 = getSign(pkg2);
        String key = "test_key";
        String value = "test_value";

        // pkg1 set global value
        boolean result = ExchangeUtils.setGlobalData(appContext, pkg1, key, value);
        Assert.assertTrue(result);

        // pkg2 get global value
        String data = ExchangeUtils.getGlobalData(appContext, pkg2, key);
        Assert.assertEquals(data, value);

        // pkg1 set value
        result = ExchangeUtils.setAppData(appContext, pkg1, key, value, null, null);
        Assert.assertTrue(result);

        // pkg2 get value from pkg1
        Exception exception = null;
        try {
            ExchangeUtils.getAppData(appContext, pkg2, pkg1, sign1, key);
        } catch (Exception e) {
            exception = e;
        }
        Assert.assertNotNull(exception);

        // pkg1 grantPermission to pkg2
        result = ExchangeUtils.grantPermission(appContext, pkg1, pkg2, sign2, key, false);
        Assert.assertTrue(result);

        // pkg2 get value from pkg1
        data = ExchangeUtils.getAppData(appContext, pkg2, pkg1, sign1, key);
        Assert.assertEquals(data, value);

        // pkg1 grant all permission to pkg2
        result = ExchangeUtils.grantPermission(appContext, pkg1, pkg2, sign2, null, false);
        Assert.assertTrue(result);

        // pkg2 get value from pkg1
        data = ExchangeUtils.getAppData(appContext, pkg2, pkg1, sign1, key);
        Assert.assertEquals(data, value);

        // pkg1 revoke permission to pkg2
        result = ExchangeUtils.revokePermission(appContext, pkg1, pkg2, key);
        Assert.assertTrue(result);

        // pkg2 get value from pkg1
        exception = null;
        try {
            ExchangeUtils.getAppData(appContext, pkg2, pkg1, sign1, key);
        } catch (Exception e) {
            exception = e;
        }
        Assert.assertNotNull(exception);

        // pkg1 clear data
        result = ExchangeUtils.clear(appContext, pkg1);
        Assert.assertTrue(result);

        // pkg1 get data
        data = ExchangeUtils.getAppData(appContext, pkg1, pkg1, sign1, key);
        Assert.assertEquals(data, null);
    }
}
