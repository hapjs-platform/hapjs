/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.bridge;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import java.util.Arrays;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class HybridRequestTest {
    @Test
    public void testExtUri() throws Exception {
        String[][] cases =
                new String[][] {
                        new String[] {
                                "http://hapjs.org/app/org.hapjs.example", null, null,
                        },
                        new String[] {
                                "http://hapjs.org/app/org.hapjs.example?key=value", null, null,
                                "key", "value"
                        },
                        new String[] {
                                "http://hapjs.org/app/org.hapjs.example?key=value#fg", null, "fg",
                                "key", "value"
                        },
                        new String[] {
                                "tel:10086", null, null,
                        },
                        new String[] {
                                "tel:10086#fg", null, "fg",
                        },
                        new String[] {
                                "mailto:example@hapjs.org?subject=Hello", null, null,
                        },
                        new String[] {
                                "mailto:example@hapjs.org?subject=Hello#fg", null, "fg",
                        },
                };
        for (String[] kase : cases) {
            checkExtResult(
                    new HybridRequest.Builder().uri(kase[0]).build(),
                    kase[0], // url
                    kase[1], // pkg
                    kase[2], // fragment
                    Arrays.copyOfRange(kase, 3, kase.length) // params
            );
        }
    }

    @Test
    public void testHapUri() throws Exception {
        String[][] cases =
                new String[][] {
                        new String[] {"hap://app/org.hapjs/", "org.hapjs", "/", null},
                        new String[] {"hap://app/org.hapjs", "org.hapjs", "/", null},
                        new String[] {"hap://app/org.hapjs/abc/123", "org.hapjs", "/abc/123", null}
                };
        for (String[] kase : cases) {
            checkHapResult(
                    (HybridRequest.HapRequest) new HybridRequest.Builder().uri(kase[0]).build(),
                    kase[1], // pkg
                    kase[2], // path
                    kase[3] // name
            );
        }
        HybridRequest request = new HybridRequest.Builder().uri("http://org.hapjs").build();
        Assert.assertFalse(request instanceof HybridRequest.HapRequest);
    }

    @Test
    public void testHapPage() throws Exception {
        String[][] cases =
                new String[][] {
                        new String[] {"org.hapjs", null, "org.hapjs", "/", null},
                        new String[] {"org.hapjs", "", "org.hapjs", "/", null},
                        new String[] {"org.hapjs", "/", "org.hapjs", "/", null},
                        new String[] {"org.hapjs", "?", "org.hapjs", "/", null},
                        new String[] {"org.hapjs", "?abc", "org.hapjs", "/", null, "abc", ""},
                        new String[] {"org.hapjs", "?abc=", "org.hapjs", "/", null, "abc", ""},
                        new String[] {"org.hapjs", "?abc=123", "org.hapjs", "/", null, "abc",
                                "123"},
                        new String[] {"org.hapjs", "/abc/2", "org.hapjs", "/abc/2", null},
                        new String[] {"org.hapjs", "/abc/2?abc", "org.hapjs", "/abc/2", null, "abc",
                                ""},
                        new String[] {"org.hapjs", "/abc/2?abc=", "org.hapjs", "/abc/2", null,
                                "abc", ""},
                        new String[] {"org.hapjs", "/abc/2?abc=123", "org.hapjs", "/abc/2", null,
                                "abc", "123"},
                        new String[] {
                                "org.hapjs", "/abc/2?abc=123&cba", "org.hapjs", "/abc/2", null,
                                "abc", "123", "cba", ""
                        },
                        new String[] {
                                "org.hapjs", "/abc/2?abc=123&cba=", "org.hapjs", "/abc/2", null,
                                "abc", "123", "cba", ""
                        },
                        new String[] {
                                "org.hapjs",
                                "/abc/2?abc=123&cba=321",
                                "org.hapjs",
                                "/abc/2",
                                null,
                                "abc",
                                "123",
                                "cba",
                                "321"
                        },
                        new String[] {
                                "org.hapjs",
                                "/abc/2?abc=%25&cba=%E5%8C%97%E4%BA%AC",
                                "org.hapjs",
                                "/abc/2",
                                null,
                                "abc",
                                "%",
                                "cba",
                                "北京"
                        },
                        // Test page name
                        new String[] {"org.hapjs", "abc/2", "org.hapjs", "/", "abc/2"},
                        new String[] {"org.hapjs", "abc/2?abc", "org.hapjs", "/", "abc/2", "abc",
                                ""},
                        new String[] {"org.hapjs", "abc/2?abc=", "org.hapjs", "/", "abc/2", "abc",
                                ""},
                        new String[] {"org.hapjs", "abc/2?abc=123", "org.hapjs", "/", "abc/2",
                                "abc", "123"},
                        new String[] {
                                "org.hapjs", "abc/2?abc=123&cba", "org.hapjs", "/", "abc/2", "abc",
                                "123", "cba", ""
                        },
                        new String[] {
                                "org.hapjs", "abc/2?abc=123&cba=", "org.hapjs", "/", "abc/2", "abc",
                                "123", "cba", ""
                        },
                        new String[] {
                                "org.hapjs",
                                "abc/2?abc=123&cba=321",
                                "org.hapjs",
                                "/",
                                "abc/2",
                                "abc",
                                "123",
                                "cba",
                                "321"
                        },
                        new String[] {
                                "org.hapjs",
                                "abc/2?abc=%25&cba=%E5%8C%97%E4%BA%AC",
                                "org.hapjs",
                                "/",
                                "abc/2",
                                "abc",
                                "%",
                                "cba",
                                "北京"
                        },
                };
        for (String[] kase : cases) {
            checkHapResult(
                    (HybridRequest.HapRequest) new HybridRequest.Builder().pkg(kase[0]).uri(kase[1])
                            .build(),
                    kase[2],
                    kase[3],
                    kase[4],
                    Arrays.copyOfRange(kase, 5, kase.length));
        }
    }

    @Test
    public void testDifferentPackage() throws Exception {
        HybridRequest request1 =
                new HybridRequest.Builder()
                        .pkg("org.sodajs.example1")
                        .uri("hap://app/org.sodajs.example")
                        .build();
        Assert.assertEquals("org.sodajs.example", request1.getPackage());
        HybridRequest request2 =
                new HybridRequest.Builder()
                        .uri("hap://app/org.sodajs.example")
                        .pkg("org.sodajs.example1")
                        .build();
        Assert.assertEquals("org.sodajs.example", request2.getPackage());
    }

    private void checkExtResult(
            HybridRequest request, String url, String pkg, String fragment, String... parameters) {
        Assert.assertEquals(url, request.getUri());
        Assert.assertEquals(pkg, request.getPackage());
        Assert.assertEquals(fragment, request.getFragment());
        int paramsSize = request.getParams() == null ? 0 : request.getParams().size();
        Assert.assertEquals(request.toString(), parameters.length, paramsSize * 2);
        for (int i = 0; i < parameters.length - 1; i += 2) {
            String key = parameters[i];
            String value = parameters[i + 1];
            Assert.assertEquals(request.getUri(), value, request.getParams().get(key));
        }
    }

    private void checkHapResult(
            HybridRequest.HapRequest request,
            String pkg,
            String path,
            String name,
            String... parameters) {
        doCheckHapResult(request, pkg, path, name, parameters);
        // Check toString
        request = (HybridRequest.HapRequest) new HybridRequest.Builder().uri(request.getUri())
                .build();
        doCheckHapResult(request, pkg, path, name, parameters);
    }

    private void doCheckHapResult(
            HybridRequest.HapRequest request,
            String pkg,
            String path,
            String name,
            String... parameters) {
        Assert.assertEquals(pkg, request.getPackage());
        Assert.assertEquals(path, request.getPagePath());
        Assert.assertEquals(name, request.getPageName());
        int paramsSize = request.getParams() == null ? 0 : request.getParams().size();
        Assert.assertEquals(request.toString(), parameters.length, paramsSize * 2);
        for (int i = 0; i < parameters.length - 1; i += 2) {
            String key = parameters[i];
            String value = parameters[i + 1];
            Assert.assertEquals(request.getUri(), value, request.getParams().get(key));
        }
    }
}
