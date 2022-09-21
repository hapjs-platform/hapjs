/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.render.jsruntime.module;

import android.net.Uri;
import android.os.Build;
import android.text.TextUtils;
import android.util.Log;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;

import androidx.annotation.NonNull;

import org.hapjs.bridge.Extension;
import org.hapjs.bridge.HybridRequest;
import org.hapjs.bridge.Request;
import org.hapjs.bridge.Response;
import org.hapjs.common.executors.Executors;
import org.hapjs.common.utils.RouterUtils;
import org.hapjs.bridge.annotation.ActionAnnotation;
import org.hapjs.bridge.annotation.ModuleExtensionAnnotation;
import org.hapjs.common.utils.RouterUtils;
import org.hapjs.model.AppInfo;
import org.hapjs.render.PageManager;
import org.hapjs.render.PageNotFoundException;
import org.hapjs.render.RootView;
import org.hapjs.render.jsruntime.serialize.SerializeException;
import org.hapjs.render.jsruntime.serialize.SerializeObject;
import org.hapjs.runtime.Runtime;

@ModuleExtensionAnnotation(
        name = WebViewModule.NAME,
        actions = {
                @ActionAnnotation(name = WebViewModule.ACTION_LOAD_URL, mode = Extension.Mode.SYNC),
                @ActionAnnotation(name = WebViewModule.ACTION_SET_COOKIE, mode = Extension.Mode.ASYNC)
        }
)

public class WebViewModule extends ModuleExtension {
    private static final String TAG = "WebViewModule";

    protected static final String NAME = "system.webview";
    protected static final String ACTION_LOAD_URL = "loadUrl";
    protected static final String PARAM_URL = "url";
    protected static final String PARAM_ALLOW_THIRD_PARTY_COOKIES = "allowthirdpartycookies";
    protected static final String PARAM_SHOW_LOADING_DIALOG = "showloadingdialog";
    private static final String PARAM_USER_AGENT = "useragent";

    protected static final String ACTION_SET_COOKIE = "setCookie";
    private static final String PARAM_DOMAIN = "domain";
    private static final String PARAM_NAME = "name";
    private static final String PARAM_VALUE = "value";
    private static final String PARAM_PATH = "path";
    private static final String PARAM_MAX_AGE = "max-age";
    private static final String PARAM_EXPIRES = "expires";
    private static final String PARAM_EXTRA = "extra";

    private PageManager mPageManager;

    @Override
    public void attach(RootView rootView, PageManager pageManager, AppInfo appInfo) {
        mPageManager = pageManager;
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public Response invokeInner(Request request) throws Exception {
        String action = request.getAction();
        SerializeObject params = request.getSerializeParams();

        if (ACTION_LOAD_URL.equals(action)) {
            return loadUrl(params);
        } else if (ACTION_SET_COOKIE.equals(action)) {
            setCookie(request);
            return Response.SUCCESS;
        } else {
            return Response.NO_ACTION;
        }
    }

    private Response loadUrl(SerializeObject params)
            throws PageNotFoundException, SerializeException {
        String url = params.getString(PARAM_URL);
        boolean allowThridPartyCookies = params.optBoolean(PARAM_ALLOW_THIRD_PARTY_COOKIES);
        boolean showLoadingDialog = params.optBoolean(PARAM_SHOW_LOADING_DIALOG, false);
        String userAgent = params.optString(PARAM_USER_AGENT, "");
        HybridRequest request = new HybridRequest.Builder()
                .pkg(mPageManager.getAppInfo().getPackage())
                .uri(url)
                .allowThirdPartyCookies(allowThridPartyCookies)
                .showLoadingDialog(showLoadingDialog)
                .userAgent(userAgent)
                .build();
        RouterUtils.push(mPageManager, request);
        return Response.SUCCESS;
    }

    private void setCookie(Request request) {
        if (!CookieManager.getInstance().acceptCookie()) {
            CookieManager.getInstance().setAcceptCookie(true);
        }
        SerializeObject args = null;
        try {
            args = request.getSerializeParams();
        } catch (SerializeException e) {
            Log.e(TAG, "setCookie getSerializeParams error", e);
            setCallback(request, false, e.getMessage());
            return;
        }
        if (args != null) {
            String domain = null;
            if (args.opt(PARAM_DOMAIN) != null) {
                domain = (String) args.opt(PARAM_DOMAIN);
            }
            if (TextUtils.isEmpty(domain)) {
                setCallback(request, false, "params error, domain is null");
                return;
            }

            String name = "";
            if (args.opt(PARAM_NAME) != null) {
                name = (String) args.opt(PARAM_NAME);
            }
            StringBuilder builder = new StringBuilder().append(name).append("=");

            if (args.opt(PARAM_VALUE) != null) {
                String value = (String) args.opt(PARAM_VALUE);
                builder.append(value).append(";");
            }
            builder.append("domain=").append(domain).append(";");

            if (args.opt(PARAM_PATH) != null) {
                String path = (String) args.opt(PARAM_PATH);
                builder.append("path=").append(path).append(";");
            }

            if (args.opt(PARAM_EXPIRES) != null) {
                String expires = (String) args.opt(PARAM_EXPIRES);
                builder.append("expires=").append(expires).append(";");
            }

            if (args.opt(PARAM_MAX_AGE) != null) {
                int maxAge = (int) args.opt(PARAM_MAX_AGE);
                builder.append("max-age=").append(maxAge).append(";");
            }

            if (args.opt(PARAM_EXTRA) != null) {
                String extra = (String) args.opt(PARAM_EXTRA);
                builder.append(extra).append(";");
            }
            Uri uri = Uri.parse(domain);
            // if specifying a value containing the "Secure" attribute, url must use the "https://" scheme.
            // https://developer.android.com/reference/android/webkit/CookieManager
            if (uri != null && TextUtils.isEmpty(uri.getScheme())) {
                domain = "https://" + domain;
            }
            final String setValue = builder.toString();
            final String domainTemp = domain;
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
                Executors.ui().execute(() -> {
                    CookieSyncManager cookieSyncManager = CookieSyncManager.createInstance(Runtime.getInstance().getContext());
                    CookieManager.getInstance().setCookie(domainTemp, setValue);
                    Executors.io().execute(() -> {
                        cookieSyncManager.sync();
                        boolean result = TextUtils.isEmpty(setValue) ? TextUtils.isEmpty(CookieManager.getInstance().getCookie(domainTemp)) : setValue.equals(CookieManager.getInstance().getCookie(domainTemp));
                        setCallback(request, result, "set cookie fail, please check params");
                    });
                });
            } else {
                Executors.ui().execute(() -> {
                    CookieManager.getInstance().setCookie(domainTemp, builder.toString(), result -> {
                        setCallback(request, result, "set cookie fail, please check params");
                    });
                });
                Executors.io().execute(() -> CookieManager.getInstance().flush());
            }
        } else {
            setCallback(request, false, "params is null");
        }
    }

    private void setCallback(@NonNull Request request, boolean result, String errorContent) {
        Response response;
        if (result) {
            response = Response.SUCCESS;
        } else {
            response = new Response(Response.CODE_GENERIC_ERROR, errorContent);
        }
        if (request.getCallback() != null) {
            request.getCallback().callback(response);
        }
    }
}
