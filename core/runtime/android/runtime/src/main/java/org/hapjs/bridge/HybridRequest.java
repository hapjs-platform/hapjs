/*
 * Copyright (c) 2021-present, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.bridge;

import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;
import androidx.annotation.Nullable;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class HybridRequest {
    public static final String ACTION_VIEW = "view";
    public static final String ACTION_DEFAULT = ACTION_VIEW;
    public static final String PAGE_PATH_DEFAULT = "/";
    public static final String INTENT_ACTION = "action";
    public static final String INTENT_URI = "uri";
    public static final String INTENT_FROM_EXTERNAL = "fromExternal";
    public static final String SCHEMA = "hap";
    public static final String PARAM_PAGE_ANIMATION = "___PARAM_PAGE_ANIMATION___";
    private static final String TAG = "HybridRequest";
    private static final String APP_URI_PREFIX = SCHEMA + "://app/";
    private static final String CARD_URI_PREFIX = SCHEMA + "://card/";
    private static final String PARAM_PAGE_NAME = "___PARAM_PAGE_NAME___";
    private static final String PARAM_LAUNCH_FLAG = "___PARAM_LAUNCH_FLAG___";
    protected final String mAction;
    protected final String mUriWithoutParams;
    protected final String mPackage;
    protected final Map<String, String> mParams;
    protected final String mFragment;
    protected final boolean mIsDeepLink;
    protected final boolean mFromExternal;
    protected final List<String> mLaunchFlags;
    protected final boolean mAllowThirdPartyCookies;
    protected final boolean mShowLoadingDialog;
    protected final String mUserAgent;
    private boolean mTabRequest = false;

    protected HybridRequest(String action,
                            String uriWithoutParams,
                            String pkg,
                            Map<String, String> params,
                            String fragment,
                            boolean isDeepLink,
                            boolean fromExternal,
                            boolean allowThirdPartCookies,
                            boolean showLoadingDialog,
                            String userAgent,
                            List<String> launchFlags) {
        mAction = action;
        mUriWithoutParams = uriWithoutParams;
        mPackage = pkg;
        mParams = params;
        mFragment = fragment;
        mIsDeepLink = isDeepLink;
        mFromExternal = fromExternal;
        mAllowThirdPartyCookies = allowThirdPartCookies;
        mShowLoadingDialog = showLoadingDialog;
        mUserAgent = userAgent;
        mLaunchFlags = launchFlags;
    }

    protected HybridRequest(Builder builder) {
        this(
                builder.action,
                builder.uri,
                builder.pkg,
                builder.params,
                builder.fragment,
                builder.isDeepLink,
                builder.fromExternal,
                builder.allowThirdPartyCookies,
                builder.showLoadingDialog,
                builder.userAgent,
                builder.launchFlags);
    }

    protected static String buildFullUri(String uri, Map<String, String> params, String fragment) {
        if (params == null || params.isEmpty()) {
            if (TextUtils.isEmpty(fragment)) {
                return uri;
            } else {
                return uri + "#" + fragment;
            }
        } else {
            Uri.Builder out = Uri.parse(uri).buildUpon();
            for (Map.Entry<String, String> entry : params.entrySet()) {
                out.appendQueryParameter(entry.getKey(), String.valueOf(entry.getValue()));
            }
            if (!TextUtils.isEmpty(fragment)) {
                out.encodedFragment(fragment);
            }
            return out.build().toString();
        }
    }

    public Map<String, String> getParams() {
        return mParams;
    }

    protected static Map<String, String> getParams(Uri uri) {
        Set<String> paramNames = uri.getQueryParameterNames();
        if (paramNames != null && !paramNames.isEmpty()) {
            Map<String, String> params = new HashMap<>();
            for (String key : paramNames) {
                if (key == null) {
                    continue;
                }
                String value = uri.getQueryParameter(key);
                if (value == null) {
                    continue;
                }
                params.put(key, value);
            }
            return params;
        }
        return null;
    }

    public boolean isTabRequest() {
        return mTabRequest;
    }

    public void setTabRequest(boolean tabRequest) {
        this.mTabRequest = tabRequest;
    }

    public String getUriWithoutParams() {
        return mUriWithoutParams;
    }

    public String getAction() {
        return mAction;
    }

    public String getUri() {
        Uri parsedUri = Uri.parse(mUriWithoutParams);
        if (parsedUri.isOpaque()) {
            return buildFullUri(mUriWithoutParams, null, mFragment);
        } else {
            return buildFullUri(mUriWithoutParams, mParams, mFragment);
        }
    }

    public String getPackage() {
        return mPackage;
    }

    public String getFragment() {
        return mFragment;
    }

    public boolean isDeepLink() {
        return mIsDeepLink;
    }

    public boolean fromExternal() {
        return mFromExternal;
    }

    public boolean isAllowThirdPartyCookies() {
        return mAllowThirdPartyCookies;
    }

    public boolean isShowLoadingDialog() {
        return mShowLoadingDialog;
    }

    public String getUserAgent() {
        return mUserAgent;
    }

    public List<String> getLaunchFlags() {
        return mLaunchFlags;
    }

    public Map<String, ?> getIntent() {
        Map<String, Object> intent = new HashMap<>();
        intent.put(INTENT_URI, getUri());
        intent.put(INTENT_ACTION, mAction);
        intent.put(INTENT_FROM_EXTERNAL, mFromExternal);
        return intent;
    }

    @Override
    public String toString() {
        return "PageRequest(action=" + getAction() + ", uri=" + getUri() + ")";
    }

    public static class HapRequest extends HybridRequest {
        private final String mPageName;
        private final boolean mIsCard;

        protected HapRequest(String action,
                             String uriWithoutParams,
                             String pkg,
                             Map<String, String> params,
                             String fragment,
                             boolean isDeepLink,
                             boolean fromExternal,
                             String pageName,
                             boolean allowThirdPartyCookies,
                             boolean showLoadingDialog,
                             String userAgent,
                             boolean isCard,
                             List<String> launchFlags) {
            super(action, uriWithoutParams, pkg, params, fragment, isDeepLink, fromExternal,
                    allowThirdPartyCookies, showLoadingDialog, userAgent, launchFlags);
            mPageName = uniformPageName(pageName);
            mIsCard = isCard;
        }

        protected HapRequest(Builder builder, String pageName, boolean isCard) {
            super(builder);
            mPageName = uniformPageName(pageName);
            mIsCard = isCard;
        }

        /* package */
        static HapRequest parse(String fullUri) {
            if (fullUri == null || fullUri.isEmpty()) {
                return null;
            }

            String pkg = null;
            String pagePath = PAGE_PATH_DEFAULT;
            String pageName = null;
            Uri uri = Uri.parse(fullUri);
            List<String> pathSegments = uri.getPathSegments();
            String schema = uri.getScheme();
            if (schema == null) {
                if (fullUri.charAt(0) == '/') {
                    pagePath = uri.getPath();
                } else {
                    pageName = uri.getPath();
                }
            } else {
                if (!fullUri.startsWith(APP_URI_PREFIX) && !fullUri.startsWith(CARD_URI_PREFIX)) {
                    return null;
                }
                if (pathSegments.size() < 1) {
                    return null;
                }
                pkg = pathSegments.get(0);
                pagePath = buildPagePath(pathSegments, 1);
            }

            Map<String, String> params = getParams(uri);
            if (params != null && params.containsKey(PARAM_PAGE_NAME)) {
                pageName = params.remove(PARAM_PAGE_NAME);
            }

            return new HapRequest(
                    ACTION_DEFAULT,
                    pagePath,
                    pkg,
                    params,
                    uri.getFragment(),
                    false,
                    true,
                    pageName,
                    false,
                    false,
                    "",
                    fullUri.startsWith(CARD_URI_PREFIX),
                    Builder.parseLaunchParams(params));
        }

        private static String buildPagePath(List<String> pathSegments, int offset) {
            if (pathSegments.size() > offset) {
                StringBuilder pb = new StringBuilder();
                for (int i = offset; i < pathSegments.size(); ++i) {
                    pb.append('/').append(pathSegments.get(i));
                }
                return pb.toString();
            }
            return PAGE_PATH_DEFAULT;
        }

        private static String uniformPageName(String pageName) {
            return "".equals(pageName) ? null : pageName;
        }

        @Override
        public String getUri() {
            return (mIsCard ? CARD_URI_PREFIX : APP_URI_PREFIX) + mPackage + getFullPath();
        }

        public String getPagePath() {
            return mUriWithoutParams;
        }

        public String getPageName() {
            return mPageName;
        }

        public boolean isCard() {
            return mIsCard;
        }

        public String getFullPath() {
            Map<String, String> params = new HashMap<>();
            if (mParams != null && !mParams.isEmpty()) {
                params.putAll(mParams);
            }
            if (!TextUtils.isEmpty(mPageName)) {
                params.put(PARAM_PAGE_NAME, mPageName);
            }
            if (mLaunchFlags != null && !mLaunchFlags.isEmpty()) {
                params.put(PARAM_LAUNCH_FLAG, Builder.convertLaunchFlagsToString(mLaunchFlags));
            }
            return buildFullUri(mUriWithoutParams, params, mFragment);
        }
    }

    public static class Builder {
        private String action;
        private String uri;
        private String pkg;
        private Map<String, String> params;
        private String fragment;
        private boolean isDeepLink;
        private boolean fromExternal;
        private boolean allowThirdPartyCookies;
        private boolean showLoadingDialog;
        private List<String> launchFlags;
        private String userAgent;

        @Nullable
        private static List<String> parseLaunchParams(Map<String, String> params) {
            if (params != null) {
                if (params.containsKey(PARAM_LAUNCH_FLAG)) {
                    String rawFlag = params.remove(PARAM_LAUNCH_FLAG);
                    if (!TextUtils.isEmpty(rawFlag)) {
                        String[] flagArray = rawFlag.replace(" ", "").split("\\|");
                        return Arrays.asList(flagArray);
                    }
                }
            }
            return null;
        }

        static String convertLaunchFlagsToString(List<String> launchFlags) {
            if (launchFlags == null) {
                return "";
            }
            return TextUtils.join("|", launchFlags);
        }

        public Builder action(String action) {
            this.action = action;
            return this;
        }

        public Builder uri(String uri) {
            this.uri = uri;
            return this;
        }

        public Builder pkg(String pkg) {
            this.pkg = pkg;
            return this;
        }

        public Builder params(Map<String, String> params) {
            this.params = params;
            return this;
        }

        public Builder fragment(String fragment) {
            this.fragment = fragment;
            return this;
        }

        public Builder isDeepLink(boolean isDeepLink) {
            this.isDeepLink = isDeepLink;
            return this;
        }

        public Builder fromExternal(boolean fromExternal) {
            this.fromExternal = fromExternal;
            return this;
        }

        public Builder allowThirdPartyCookies(boolean allowThirdPartyCookies) {
            this.allowThirdPartyCookies = allowThirdPartyCookies;
            return this;
        }

        public Builder showLoadingDialog(boolean showLoadingDialog) {
            this.showLoadingDialog = showLoadingDialog;
            return this;
        }

        public Builder userAgent(String userAgent) {
            this.userAgent = userAgent;
            return this;
        }

        public HybridRequest build() {
            if (action == null) {
                action = ACTION_DEFAULT;
            }
            if (uri == null || uri.isEmpty()) {
                uri = PAGE_PATH_DEFAULT;
            }

            HapRequest hapRequest = HapRequest.parse(uri);
            if (hapRequest == null) {
                Uri parsedUri = Uri.parse(uri);
                String schema = parsedUri.getScheme();
                if (schema == null || schema.isEmpty()) {
                    throw new IllegalArgumentException("uri has no schema, uri=" + uri);
                }

                if (!parsedUri.isOpaque()) {
                    Map<String, String> uriParams = getParams(parsedUri);
                    if (uriParams != null) {
                        if (params == null) {
                            params = uriParams;
                        } else {
                            params.putAll(uriParams);
                        }
                    }
                    this.launchFlags = parseLaunchParams(params);

                    // remove params
                    int uriEnd = uri.indexOf("?");
                    if (uriEnd >= 0) {
                        uri = uri.substring(0, uriEnd);
                    }
                }

                // remove fragment
                int fragmentIndex = uri.indexOf("#");
                if (fragmentIndex >= 0) {
                    uri = uri.substring(0, fragmentIndex);
                }

                setDefaultFragment(parsedUri.getFragment());
            } else {
                String requestPackage = hapRequest.getPackage();
                if (pkg == null) {
                    if (requestPackage == null) {
                        throw new IllegalArgumentException(
                                "pkg can't be null, pkg=" + pkg + ", uri=" + uri);
                    }
                    pkg = requestPackage;
                } else if (requestPackage != null && !requestPackage.equals(pkg)) {
                    Log.d(TAG, "pkg is different with uri, pkg=" + pkg + ", uri=" + uri);
                    // package in uri has higher priority
                    pkg = requestPackage;
                }

                Map<String, String> hapParams = hapRequest.getParams();
                if (hapParams != null && !hapParams.isEmpty()) {
                    if (params == null) {
                        params = hapParams;
                    } else {
                        params.putAll(hapParams);
                    }
                }

                this.launchFlags = parseLaunchParams(params);
                if (this.launchFlags == null) {
                    this.launchFlags = hapRequest.mLaunchFlags;
                }

                uri = hapRequest.getPagePath();
                setDefaultFragment(hapRequest.getFragment());
            }

            if (hapRequest == null) {
                return new HybridRequest(this);
            } else {
                return new HapRequest(this, hapRequest.getPageName(), hapRequest.isCard());
            }
        }

        private void setDefaultFragment(String fragment) {
            // fragment setting has higher priority
            if (TextUtils.isEmpty(this.fragment)) {
                this.fragment = fragment;
            }
        }
    }
}
