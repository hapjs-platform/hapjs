/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.launch;

import android.content.Intent;
import android.net.Uri;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class DeepLinkClientTest {
    @Test
    public void testParseParams() throws Exception {
        // 测试hapjs.org
        Intent intent = getIntent("http://hapjs.org/app/com.example.hap");
        String pkg = DeepLinkClient.getInstance().getPackage(intent);
        Assert.assertEquals("com.example.hap", pkg);
        Assert.assertEquals(null, getPath());
        Assert.assertNull(getSource());

        // 测试path
        intent = getIntent("https://hapjs.org/app/com.example.hap/index?abc=123&__SRC__=test");
        pkg = DeepLinkClient.getInstance().getPackage(intent);
        Assert.assertEquals("com.example.hap", pkg);
        Assert.assertEquals("/index?abc=123", getPath());
        Assert.assertEquals("test", getSource());

        // 测试hap
        intent = getIntent("hap://app/com.example.hap");
        pkg = DeepLinkClient.getInstance().getPackage(intent);
        Assert.assertEquals("com.example.hap", pkg);
        Assert.assertEquals(null, getPath());
        Assert.assertNull(getSource());

        // 测试path
        intent = getIntent("hap://app/com.example.hap/index?abc=123&__SRC__=test");
        pkg = DeepLinkClient.getInstance().getPackage(intent);
        Assert.assertEquals("com.example.hap", pkg);
        Assert.assertEquals("/index?abc=123", getPath());
        Assert.assertEquals("test", getSource());

        // 测试fragment
        intent = getIntent("hap://app/com.example.hap/index?abc=123&__SRC__=test#title");
        pkg = DeepLinkClient.getInstance().getPackage(intent);
        Assert.assertEquals("com.example.hap", pkg);
        Assert.assertEquals("/index?abc=123#title", getPath());
        Assert.assertEquals("test", getSource());

        // 测试params
        intent = getIntent(
                "hap://app/com.example.hap?path=%2findex%3fabc%3d123%23title&__SRC__=test");
        pkg = DeepLinkClient.getInstance().getPackage(intent);
        Assert.assertEquals("com.example.hap", pkg);
        Assert.assertEquals("?path=%2findex%3fabc%3d123%23title", getPath().toLowerCase());
        Assert.assertEquals("test", getSource());

        // 测试不支持的host
        intent = getIntent("http://xxx.hapjs.org/app/com.example.hap");
        try {
            DeepLinkClient.getInstance().getPackage(intent);
            Assert.assertTrue(false);
        } catch (IllegalArgumentException e) {
            Assert.assertTrue(true);
        }

        // 测试不支持的host
        intent = getIntent("hap://app2/com.example.hap");
        try {
            DeepLinkClient.getInstance().getPackage(intent);
            Assert.assertTrue(false);
        } catch (IllegalArgumentException e) {
            Assert.assertTrue(true);
        }
    }

    private Intent getIntent(String url) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(Uri.parse(url));
        return intent;
    }

    private String getPath() {
        return DeepLinkClient.getInstance().mPath;
    }

    private String getSource() {
        return DeepLinkClient.getInstance().mSource;
    }
}
