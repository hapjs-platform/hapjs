/*
 * Copyright (c) 2021-present, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.common.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.hapjs.bridge.AbstractExtension;
import org.hapjs.bridge.Callback;
import org.hapjs.bridge.ExtensionManager;
import org.hapjs.bridge.Response;
import org.hapjs.common.executors.Executors;
import org.hapjs.component.view.MenubarView;
import org.hapjs.logging.Source;
import org.hapjs.model.DisplayInfo;
import org.hapjs.model.MenubarItemData;
import org.hapjs.render.Page;
import org.hapjs.render.PageManager;
import org.hapjs.render.RootView;
import org.hapjs.render.jsruntime.JsThread;
import org.hapjs.runtime.ProviderManager;
import org.hapjs.system.SysOpProvider;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import static org.hapjs.common.utils.FeatureInnerBridge.MENUBAR_JS_CALLBACK;

public class MenubarUtils {
    private static final String TAG = "MenubarUtils";

    public static final String PARAM_PACKAGE = "package";
    public static final String PARAM_PAGE_PATH = "page_path";
    public static final String PARAM_PAGE_PARAMS = "page_params";
    public static final String PARAM_PLATFORMS = "platforms";
    public static final String MENUBAR_HAS_SHORTCUT_INSTALLED = "has_shortcut_installed";
    public static final String IS_FROM_SHARE_BUTTON = "is_from_share_button";
    /**
     * same with share key
     */
    protected static final String PARAM_SHARE_TYPE = "shareType";
    protected static final String PARAM_TITLE = "title";
    protected static final String PARAM_SUMMARY = "summary";
    protected static final String PARAM_IMAGE_PATH = "imagePath";
    public static boolean mIsNeedResumeUpdate = true;
    /**
     * local menubardata is ever update
     */
    public static boolean mIsLocalMenuUpdate = true;

    public static void startShare(
            Map<String, String> shareIdMap,
            Map<String, String> extra,
            RootView rootView,
            ExtensionManager tmpExtentionManager, final ShareCallback shareCallback) {
        if (null == shareIdMap || shareIdMap.size() == 0) {
            Log.e(TAG, "startShare error mShareRpkIconUrl empty or shareIdMap null or shareIdMap"
                    + " is empty share fail or page null.");
            return;
        }
        String title = "";
        String summary = "";
        String imgPath = "";
        String rpkPkg = "";
        String shareParams = "";
        String shareUrl = "";
        String pageParams = "";
        String pagePath = "";
        String sharePlatforms = "";
        boolean isShowCurrentPage = false;
        boolean isFromShareButton = false;
        if (extra != null && !extra.isEmpty()) {
            title = extra.get(DisplayInfo.Style.KEY_MENUBAR_SHARE_TITLE);
            summary = extra.get(DisplayInfo.Style.KEY_MENUBAR_SHARE_DESCRIPTION);
            imgPath = extra.get(DisplayInfo.Style.KEY_MENUBAR_SHARE_ICON);
            rpkPkg = extra.get(PARAM_PACKAGE);
            isShowCurrentPage =
                    (null != extra.get(DisplayInfo.Style.PARAM_SHARE_CURRENT_PAGE)
                            && "true".equals(extra.get(DisplayInfo.Style.PARAM_SHARE_CURRENT_PAGE))
                            ? true
                            : false);
            shareParams = extra.get(DisplayInfo.Style.PARAM_SHARE_PARAMS);
            shareUrl = extra.get(DisplayInfo.Style.PARAM_SHARE_URL);
            pagePath = extra.get(PARAM_PAGE_PATH);
            pageParams = extra.get(PARAM_PAGE_PARAMS);
            sharePlatforms = extra.get(PARAM_PLATFORMS);
            if (extra.containsKey(IS_FROM_SHARE_BUTTON)) {
                isFromShareButton = TextUtils.equals("true", extra.get(IS_FROM_SHARE_BUTTON));
            }
        }
        ExtensionManager extensionManager = null;
        boolean isChimera = false;
        if (null != tmpExtentionManager) {
            extensionManager = tmpExtentionManager;
            isChimera = true;
        } else {
            if (null == rootView) {
                Log.e(TAG, "startShare error root null.");
                return;
            }
            extensionManager = getExtensionManager(rootView);
        }

        if (null != extensionManager) {
            String action = "share";
            JSONObject sharejson = new JSONObject();
            try {
                sharejson.put(PARAM_SHARE_TYPE, 0);
                sharejson.put(PARAM_TITLE, title);
                sharejson.put(PARAM_SUMMARY, summary);
                sharejson.put(PARAM_IMAGE_PATH, imgPath);
                if (!TextUtils.isEmpty(sharePlatforms)) {
                    sharejson.put(PARAM_PLATFORMS, new JSONArray(sharePlatforms));
                }
                JSONObject paramsJson = null;
                boolean isShareParamsValid = false;
                if (!TextUtils.isEmpty(shareParams)) {
                    isShareParamsValid = true;
                    paramsJson = new JSONObject(shareParams);
                } else {
                    paramsJson = new JSONObject();
                }
                String urlMainPath = "";
                if (isChimera) {
                    action = "serviceShare";
                }
                urlMainPath = pagePath;
                JSONObject pageParamsJson;
                if (!TextUtils.isEmpty(pageParams)) {
                    pageParamsJson = new JSONObject(pageParams);
                } else {
                    pageParamsJson = new JSONObject();
                }
                if (isShareParamsValid) {
                    Iterator<String> iterator = paramsJson.keys();
                    String key;
                    String value;
                    while (iterator.hasNext()) {
                        key = iterator.next();
                        value = paramsJson.optString(key);
                        pageParamsJson.put(key, value);
                    }
                }
                String paramsStr = pageParamsJson.toString();
                String encoderParamsStr = URLEncoder.encode(paramsStr, "UTF-8");
                sharejson.put(
                        SysOpProvider.PARAM_SHARE_URL,
                        null != shareIdMap.get(SysOpProvider.PARAM_SHARE_URL)
                                ? (shareIdMap.get(SysOpProvider.PARAM_SHARE_URL)
                                + "?packageName="
                                + rpkPkg
                                + "&path="
                                + (isShowCurrentPage ? urlMainPath : "")
                                + "&shareUrl="
                                + (null != shareUrl ? shareUrl : "")
                                + "&params=")
                                + (isShowCurrentPage ? encoderParamsStr : "")
                                : "");
                sharejson.put(
                        SysOpProvider.PARAM_KEY_QQ,
                        null != shareIdMap.get(SysOpProvider.PARAM_KEY_QQ)
                                ? shareIdMap.get(SysOpProvider.PARAM_KEY_QQ)
                                : "");
                sharejson.put(
                        SysOpProvider.PARAM_KEY_WX,
                        null != shareIdMap.get(SysOpProvider.PARAM_KEY_WX)
                                ? shareIdMap.get(SysOpProvider.PARAM_KEY_WX)
                                : "");
                sharejson.put(
                        SysOpProvider.PARAM_KEY_SINA,
                        null != shareIdMap.get(SysOpProvider.PARAM_KEY_SINA)
                                ? shareIdMap.get(SysOpProvider.PARAM_KEY_SINA)
                                : "");
                sharejson.put(
                        SysOpProvider.PARAM_APPSIGN_KEY,
                        null != shareIdMap.get(SysOpProvider.PARAM_APPSIGN_KEY)
                                ? shareIdMap.get(SysOpProvider.PARAM_APPSIGN_KEY)
                                : "");
                sharejson.put(
                        SysOpProvider.PARAM_PACKAGE_KEY,
                        null != shareIdMap.get(SysOpProvider.PARAM_PACKAGE_KEY)
                                ? shareIdMap.get(SysOpProvider.PARAM_PACKAGE_KEY)
                                : "");
                if (isFromShareButton) {
                    sharejson.put(SysOpProvider.PARAM_MENUBAR_KEY, false);
                    sharejson.put(SysOpProvider.PARAM_SHAREBUTTON_KEY, true);
                } else {
                    sharejson.put(SysOpProvider.PARAM_MENUBAR_KEY, true);
                    sharejson.put(SysOpProvider.PARAM_SHAREBUTTON_KEY, false);
                }
            } catch (JSONException e) {
                Log.e(TAG, "error startToShare msg : " + e.getMessage());
            } catch (UnsupportedEncodingException e) {
                Log.e(TAG, "error EncodingException startShare msg : " + e.getMessage());
            }
            FeatureInnerBridge.invokeWithCallback(extensionManager, "service.share", action, sharejson.toString(), MENUBAR_JS_CALLBACK, -1, new Callback(extensionManager, "-1", AbstractExtension.Mode.ASYNC) {
                @Override
                public void callback(Response response) {
                    ThreadUtils.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (null != response) {
                                Log.d(TAG, "startShare response  code : "
                                        + response.getCode()
                                        + " content : "
                                        + response.getContent());
                                if (null != shareCallback) {
                                    shareCallback.onShareCallback(response);
                                }
                            }
                        }
                    });

                }
            });

        } else {
            Log.e(TAG, "startShare extensionManager null.");
        }
    }

    public static void createShortCut(RootView rootView, MenubarStatusCallback callback) {
        if (null == callback) {
            Log.e(TAG, "createShortCut callback is null.");
            return;
        }
        if (null == rootView) {
            Log.e(TAG, "createShortCut rootView is null.");
            return;
        }

        Executors.io()
                .execute(
                        () -> {
                            Context context = rootView.getContext();
                            String pkg = rootView.getAppInfo().getPackage();
                            String name = rootView.getAppInfo().getName();
                            Uri iconUri = rootView.getAppContext().getIcon();

                            if (TextUtils.isEmpty(name) || iconUri == null) {
                                Response response =
                                        new Response(Response.CODE_GENERIC_ERROR,
                                                "app name or app iconUri is null");
                                callbackInstallResult(response, callback);
                                return;
                            }

                            if (ShortcutManager.hasShortcutInstalled(context, pkg)) {
                                ShortcutManager.update(context, pkg, name, iconUri);
                                Response response =
                                        new Response(Response.CODE_SUCCESS, "Update success");
                                callbackInstallResult(response, callback);
                            } else {
                                Source source = new Source();
                                source.putExtra(Source.EXTRA_SCENE, Source.SHORTCUT_SCENE_API);
                                boolean result = ShortcutManager
                                        .install(context, pkg, name, iconUri, source);
                                if (result) {
                                    callbackInstallResult(Response.SUCCESS, callback);
                                } else {
                                    callbackInstallResult(
                                            new Response(Response.CODE_GENERIC_ERROR,
                                                    "install fail"), callback);
                                }
                            }
                        });
    }

    private static void callbackInstallResult(Response response, MenubarStatusCallback callback) {
        String contentStr = "";
        if (null != response) {
            Object content = response.getContent();
            if (content instanceof String) {
                contentStr = (String) content;
            } else {
                Log.e(TAG, "createShortCut getContent not String.");
            }
            HashMap<String, Object> maps = new HashMap<>();
            maps.put(MENUBAR_HAS_SHORTCUT_INSTALLED,
                    Response.SUCCESS.getContent().equals(contentStr));
            callback.onMenubarStatusCallback(maps);
        }
    }

    public static void isShortCutInstalled(
            Context context,
            String packageName,
            RootView rootView,
            MenubarStatusCallback callback,
            ExtensionManager tmpExtentionManager) {
        if (null == callback) {
            Log.e(TAG, "isShortCutInstalled callback is null.");
            return;
        }
        ExtensionManager extensionManager = null;
        if (null != tmpExtentionManager) {
            extensionManager = tmpExtentionManager;
            boolean hasInstall = false;
            if (null != context && !TextUtils.isEmpty(packageName)) {
                hasInstall = ShortcutManager.hasShortcutInstalled(context, packageName);
            }
            if (null != callback) {
                HashMap<String, Object> maps = new HashMap<>();
                maps.put(MENUBAR_HAS_SHORTCUT_INSTALLED, hasInstall);
                callback.onMenubarStatusCallback(maps);
            }
            return;
        } else {
            if (null == rootView) {
                Log.e(TAG, "isShortCutInstalled rootView is null.");
                return;
            }
            extensionManager = getExtensionManager(rootView);
        }
        if (null != extensionManager) {
            final ExtensionManager curExtensionManager = extensionManager;
            if (null != curExtensionManager) {
                JSONObject emptyJson = new JSONObject();
                FeatureInnerBridge.invokeWithCallback(extensionManager, "system.shortcut", "hasInstalled", emptyJson.toString(), MENUBAR_JS_CALLBACK, -1, new Callback(extensionManager, "-1", AbstractExtension.Mode.ASYNC) {
                    @Override
                    public void callback(Response response) {
                        ThreadUtils.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                boolean hasInstall = false;
                                if (null != callback) {
                                    if (null != response) {
                                        Object content = response.getContent();
                                        if (content instanceof Boolean) {
                                            hasInstall = (Boolean) content;
                                        } else {
                                            Log.e(TAG, "isShortCutInstalled hasInstall not Boolean.");
                                        }
                                        if (null != callback) {
                                            HashMap<String, Object> maps = new HashMap<>();
                                            maps.put(MENUBAR_HAS_SHORTCUT_INSTALLED, hasInstall);
                                            callback.onMenubarStatusCallback(maps);
                                        }

                                    }
                                } else {
                                    Log.e(TAG, "isShortCutInstalled callback is null.");
                                }
                            }
                        });
                    }
                });
            } else {
                Log.e(TAG, "isShortCutInstalled extensionManager null.");
            }
        }
    }

    private static ExtensionManager getExtensionManager(RootView rootView) {
        ExtensionManager extensionManager = null;
        if (null != rootView) {
            JsThread jsThread = rootView.getJsThread();
            if (null != jsThread) {
                extensionManager = jsThread.getBridgeManager();
            } else {
                Log.e(TAG, "getExtensionManager error jsThread null.");
            }
        } else {
            Log.e(TAG, "getExtensionManager error mRootView null.");
        }
        return extensionManager;
    }

    public static void openAboutPage(RootView rootView) {
        PageManager pageManager = null;
        if (null != rootView) {
            pageManager = rootView.getPageManager();
        }
        if (null != pageManager) {
            Page page = pageManager.buildAboutPage();
            pageManager.push(page);
        }
    }

    public static Page buildHomePage(RootView rootView) {
        PageManager pageManager = null;
        Page page = null;
        if (null != rootView) {
            pageManager = rootView.getPageManager();
        }
        if (null != pageManager) {
            page = pageManager.buildHomePage();
        }
        return page;
    }

    public static void goHomePage(RootView rootView) {
        PageManager pageManager = null;
        if (null != rootView) {
            pageManager = rootView.getPageManager();
        }
        if (null != pageManager) {
            List<Page> allPages = pageManager.getPageInfos();
            if (null != allPages) {
                int allPageSize = allPages.size();
                if (allPageSize == 1) {
                    Page homePage = buildHomePage(rootView);
                    if (null != homePage) {
                        pageManager.replace(homePage);
                    } else {
                        Log.e(TAG, "goHomePage error replace homePage null, allPageSize  : "
                                + allPageSize);
                    }
                } else if (allPageSize > 1) {
                    pageManager.clearAll();
                    Page homePage = buildHomePage(rootView);
                    if (null != homePage) {
                        pageManager.push(homePage);
                    } else {
                        Log.e(TAG, "goHomePage error push homePage null, allPageSize  : "
                                + allPageSize);
                    }
                } else {
                    Log.e(TAG, "goHomePage error allPageSize  : " + allPageSize);
                }
            } else {
                Log.e(TAG, "goHomePage allPages is null.");
            }
        } else {
            Log.e(TAG, "goHomePage pageManager is null.");
        }
    }

    public static boolean getMenubarValue(Context context, String key) {
        boolean value = false;
        if (null != context) {
            SharedPreferences sp =
                    context.getSharedPreferences(
                            MenubarView.MENUBAR_DIALOG_SHARE_PREFERENCE_NAME,
                            Context.MODE_MULTI_PROCESS);
            value = sp.getBoolean(key, false);
        }
        return value;
    }

    public static boolean getMenubarValueWithDefault(
            Context context, String key, boolean defaultValue) {
        boolean value = false;
        if (null != context) {
            SharedPreferences sp =
                    context.getSharedPreferences(
                            MenubarView.MENUBAR_DIALOG_SHARE_PREFERENCE_NAME,
                            Context.MODE_MULTI_PROCESS);
            value = sp.getBoolean(key, defaultValue);
        }
        return value;
    }

    public static void setMenubarValue(Context context, String key, boolean value) {
        if (null != context) {
            SharedPreferences sp =
                    context.getSharedPreferences(
                            MenubarView.MENUBAR_DIALOG_SHARE_PREFERENCE_NAME,
                            Context.MODE_MULTI_PROCESS);
            SharedPreferences.Editor editor = sp.edit();
            editor.putBoolean(key, value);
            editor.apply();
        }
    }

    public static void refreshMenubarPointStatus(Context context, String key) {
        if (!TextUtils.isEmpty(key) && null != context) {
            boolean isShow = getMenubarValue(context, key);
            if (isShow) {
                setMenubarValue(context, key, false);
            }
        }
    }

    public static int updateMenubarData(Context context, List<MenubarItemData> datas) {
        int showPointCount = 0;
        if (null == datas || datas.size() == 0 || null == context) {
            Log.e(TAG, "updateMenubarData datas null or datas empty or context null.");
            return showPointCount;
        }
        boolean isPointEverSaved =
                MenubarUtils.getMenubarValue(context, MenubarView.MENUBAR_POINT_EVER_SAVE);
        int allsize = datas.size();
        MenubarItemData itemData = null;
        String key = null;
        boolean isShowPoint = false;
        if (isPointEverSaved) {
            for (int i = 0; i < allsize; i++) {
                itemData = datas.get(i);
                key = itemData.getKey();
                if (null != key) {
                    isShowPoint = getMenubarValue(context, key);
                    itemData.setShowPoint(isShowPoint);
                    if (isShowPoint) {
                        showPointCount++;
                    }
                }
            }
        } else {
            for (int i = 0; i < allsize; i++) {
                itemData = datas.get(i);
                isShowPoint = itemData.isShowPoint();
                key = itemData.getKey();
                if (null != key && isShowPoint) {
                    setMenubarValue(context, key, isShowPoint);
                    showPointCount++;
                }
            }
            setMenubarValue(context, MenubarView.MENUBAR_POINT_EVER_SAVE, true);
        }
        return showPointCount;
    }

    public static boolean getMenuPointStatus(Context hybridContext, Context context) {
        if (null == hybridContext || null == context) {
            Log.e(TAG, "getMenuPointStatus hybridContext or context is null.");
            return false;
        }
        boolean isMenuPointShow = false;
        boolean isVenderPointShow = false;
        boolean isPointEverSaved =
                MenubarUtils.getMenubarValue(hybridContext, MenubarView.MENUBAR_POINT_EVER_SAVE);
        if (isPointEverSaved) {
            isMenuPointShow =
                    MenubarUtils
                            .getMenubarValue(hybridContext, MenubarView.MENUBAR_POINT_MENU_STATUS);
        } else {
            SysOpProvider provider = ProviderManager.getDefault().getProvider(SysOpProvider.NAME);
            if (null != provider) {
                List<MenubarItemData> tmpDatas =
                        provider.getMenuBarData(null, hybridContext, context, null);
                MenubarItemData menubarItemData = null;
                if (null != tmpDatas) {
                    int allSize = tmpDatas.size();
                    for (int i = 0; i < allSize; i++) {
                        menubarItemData = tmpDatas.get(i);
                        if (null != menubarItemData && menubarItemData.isShowPoint()) {
                            isVenderPointShow = true;
                            break;
                        }
                    }
                }
            }
            isMenuPointShow = isVenderPointShow || MenubarUtils.mIsLocalMenuUpdate;
            if (isMenuPointShow) {
                MenubarUtils.setMenubarValue(hybridContext, MenubarView.MENUBAR_POINT_MENU_STATUS,
                        true);
            }
        }
        return isMenuPointShow;
    }

    public static UrlData parse(String url) {
        UrlData urlData = new UrlData();
        if (url == null) {
            return urlData;
        }
        url = url.trim();
        if (TextUtils.isEmpty(url)) {
            return urlData;
        }
        String[] urlParts = url.split("[?]");
        urlData.baseUrl = getNormalizedPagePath(urlParts[0]);
        if (urlParts.length == 1) {
            return urlData;
        }
        String[] params = urlParts[1].split("&");
        if (null != params) {
            urlData.params = new HashMap<>();
            for (String param : params) {
                String[] keyValue = param.split("=");
                if (null != keyValue && keyValue.length == 2 && !TextUtils.isEmpty(keyValue[0])) {
                    urlData.params.put(keyValue[0], keyValue[1]);
                }
            }
        }
        return urlData;
    }

    public static String getNormalizedPagePath(String pagePath) {
        if (TextUtils.isEmpty(pagePath) || "/".equals(pagePath)) {
            return pagePath;
        }
        StringBuilder res = new StringBuilder();
        String[] pathSplit = pagePath.split("/");
        if (null != pathSplit) {
            for (int i = 0; i < pathSplit.length; i++) {
                String segment = pathSplit[i];
                if (!TextUtils.isEmpty(segment)) {
                    res.append(segment);
                    if (i < pathSplit.length - 1) {
                        res.append("/");
                    }
                }
            }
        }
        return res.toString();
    }

    public interface MenubarStatusCallback {
        void onMenubarStatusCallback(HashMap<String, Object> data);
    }

    public static class UrlData {
        public String baseUrl;
        public Map<String, String> params;
    }

    public interface ShareCallback {
        void onShareCallback(Response response);
    }
}
