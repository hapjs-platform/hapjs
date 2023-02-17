/*
 * Copyright (c) 2021-2023, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.features;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import org.hapjs.bridge.FeatureExtension;
import org.hapjs.bridge.Request;
import org.hapjs.bridge.Response;
import org.hapjs.bridge.annotation.ActionAnnotation;
import org.hapjs.bridge.annotation.FeatureExtensionAnnotation;
import org.hapjs.bridge.storage.file.InternalUriUtils;
import org.hapjs.common.executors.Executors;
import org.hapjs.common.utils.IconUtils;
import org.hapjs.common.utils.ShortcutManager;
import org.hapjs.common.utils.UriUtils;
import org.hapjs.component.bridge.RenderEventCallback;
import org.hapjs.logging.Source;
import org.hapjs.render.Page;
import org.hapjs.render.PageManager;
import org.hapjs.render.RootView;
import org.hapjs.runtime.Checkable;
import org.hapjs.runtime.CheckableAlertDialog;
import org.hapjs.runtime.DarkThemeUtil;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.ref.WeakReference;

@FeatureExtensionAnnotation(
        name = Shortcut.FEATURE_NAME,
        actions = {
                @ActionAnnotation(name = Shortcut.INSTALL, mode = FeatureExtension.Mode.ASYNC),
                @ActionAnnotation(name = Shortcut.HAS_INSTALLED, mode = FeatureExtension.Mode.ASYNC),
                @ActionAnnotation(
                        name = Shortcut.ATTR_GET_SYSTEM_PROMPT_ENABLED,
                        mode = FeatureExtension.Mode.SYNC,
                        type = FeatureExtension.Type.ATTRIBUTE,
                        access = FeatureExtension.Access.READ,
                        alias = Shortcut.ATTR_SYSTEM_PROMPT_ENABLED_ALIAS),
                @ActionAnnotation(
                        name = Shortcut.ATTR_SET_SYSTEM_PROMPT_ENABLED,
                        mode = FeatureExtension.Mode.SYNC,
                        type = FeatureExtension.Type.ATTRIBUTE,
                        access = FeatureExtension.Access.WRITE,
                        alias = Shortcut.ATTR_SYSTEM_PROMPT_ENABLED_ALIAS)
        })
public class Shortcut extends FeatureExtension {
    protected static final String FEATURE_NAME = "system.shortcut";
    protected static final String INSTALL = "install";
    protected static final String HAS_INSTALLED = "hasInstalled";
    protected static final String PARAM_MESSAGE = "message";
    protected static final String ATTR_SYSTEM_PROMPT_ENABLED_ALIAS = "systemPromptEnabled";
    protected static final String ATTR_GET_SYSTEM_PROMPT_ENABLED = "__getSystemPromptEnabled";
    protected static final String ATTR_SET_SYSTEM_PROMPT_ENABLED = "__setSystemPromptEnabled";
    private static final String PARAM_KEY_NAME = "name";
    private static final String PARAM_KEY_ICON_URL = "iconUrl";
    private static final String PARAM_KEY_PATH = "path";
    private static final String TAG = "Shortcut";
    private static final String ATTR_DEFAULT_PARAMS_KEY = "value";
    private static final int CODE_EXCEED_USE_FREQUENCY = Response.CODE_FEATURE_ERROR + 1;

    private WeakReference<Dialog> mDialogRef;

    @Override
    protected Response invokeInner(final Request request) throws Exception {
        String action = request.getAction();
        if (INSTALL.equals(action)) {
            install(request);
        } else if (HAS_INSTALLED.equals(action)) {
            hasInstalled(request);
        } else if (ATTR_GET_SYSTEM_PROMPT_ENABLED.equals(action)) {
            return getSystemPromptEnabled(request);
        } else if (ATTR_SET_SYSTEM_PROMPT_ENABLED.equals(action)) {
            setSystemPromptEnabled(request);
        }
        return Response.SUCCESS;
    }

    protected void hasInstalled(Request request) throws JSONException {
        final Context context = request.getNativeInterface().getActivity();
        String pkg = request.getApplicationContext().getPackage();
        JSONObject jsonParams = request.getJSONParams();
        final String path = jsonParams == null ? "" : jsonParams.optString(PARAM_KEY_PATH, "");
        boolean hasInstall = ShortcutManager.hasShortcutInstalled(context, pkg, path);
        request.getCallback().callback(new Response(hasInstall));
    }

    private void install(final Request request) throws JSONException {
        final Activity activity = request.getNativeInterface().getActivity();
        String nameTemp = request.getApplicationContext().getName();
        final String pkg = request.getApplicationContext().getPackage();
        final String path = request.getJSONParams() == null ? "" : request.getJSONParams().optString(PARAM_KEY_PATH);
        Uri iconUriTemp = request.getApplicationContext().getIcon();
        final String customMessage = request.getJSONParams().optString(PARAM_MESSAGE);
        if (!TextUtils.isEmpty(path)) {
            //不为空时优先取开发者指定
            nameTemp = request.getJSONParams().optString(PARAM_KEY_NAME);
            if (TextUtils.isEmpty(nameTemp)) {
                Response response =
                        new Response(Response.CODE_GENERIC_ERROR, "name is null");
                request.getCallback().callback(response);
                return;
            }
            String iconUrl = request.getJSONParams().optString(PARAM_KEY_ICON_URL);
            iconUriTemp = tryParseUri(iconUrl, request);
        }
        final String name = nameTemp;
        final Uri iconUri = iconUriTemp;
        if (TextUtils.isEmpty(name) || iconUri == null) {
            Response response =
                    new Response(Response.CODE_GENERIC_ERROR, "app name or app iconUri is null");
            request.getCallback().callback(response);
            return;
        }

        if (isExceedUseFrequencyOfDay(activity, pkg)) {
            Response response =
                    new Response(CODE_EXCEED_USE_FREQUENCY,
                            "Interface call exceeded frequency limit");
            request.getCallback().callback(response);
            return;
        }

        if (isForbidden(activity, pkg)) {
            Response response = new Response(Response.CODE_USER_DENIED, "User forbidden");
            request.getCallback().callback(response);
            return;
        }

        if (ShortcutManager.hasShortcutInstalled(activity, pkg, path)) {
            ShortcutManager.update(activity, pkg, name, iconUri);
            Response response =
                    new Response(
                            Response.CODE_ILLEGAL_REQUEST,
                            "Shortcut already created, please call shortcut.hasInstalled first");
            request.getCallback().callback(response);
            return;
        }

        if (activity.isDestroyed() || activity.isFinishing()) {
            request.getCallback().callback(Response.ERROR);
            return;
        }

        if (!ShortcutManager.checkShortcutNumber(activity, pkg, path)) {
            request.getCallback().callback(new Response(Response.CODE_GENERIC_ERROR, "check Shortcut Number fail"));
            Log.w(TAG, "check Shortcut Number fail:" + "pkg=" + pkg + ",iconUri=" + iconUri + ",name=" + name + ",path=" + path);
            return;
        }

        final String message;
        if (TextUtils.isEmpty(customMessage)) {
            message = getDefaultMessage(activity, name);
        } else {
            message = customMessage;
        }

        final Drawable iconDrawable = IconUtils.getIconDrawable(activity, iconUri);

        activity.runOnUiThread(
                new Runnable() {
                    @Override
                    public void run() {
                        Dialog dialog = mDialogRef == null ? null : mDialogRef.get();
                        if (dialog != null && dialog.isShowing()) {
                            Response response =
                                    new Response(
                                            Response.CODE_TOO_MANY_REQUEST,
                                            "Please wait last request finished.");
                            request.getCallback().callback(response);
                            return;
                        }
                        showCreateDialog(request, activity, pkg, name, message, iconUri,
                                iconDrawable);
                    }
                });
    }

    public Uri tryParseUri(String iconUrl, final Request request) {
        if (TextUtils.isEmpty(iconUrl)) {
            return request.getApplicationContext().getIcon();
        }
        Uri result = null;
        result = UriUtils.computeUri(iconUrl);
        Activity activity = request.getNativeInterface().getActivity();
        if (activity == null || activity.isFinishing() || activity.isDestroyed()) {
            return null;
        }
        RenderEventCallback callback = null;
        if (result == null) {
            View hybridView = activity.findViewById(org.hapjs.runtime.R.id.hybrid_view);
            if (!(hybridView instanceof RootView)) {
                return null;
            }
            callback = ((RootView) hybridView).getRenderEventCallback();
            PageManager pageManager = ((RootView) hybridView).getPageManager();
            if (pageManager == null) {
                return null;
            }
            Page currentPage = pageManager.getCurrPage();
            result = callback.getCache(iconUrl);
        } else if (InternalUriUtils.isInternalUri(result)) {
            if (callback == null) {
                return null;
            }
            result = callback.getUnderlyingUri(iconUrl);
        }
        return result;
    }

    protected void showCreateDialog(
            Request request,
            Activity activity,
            String pkg,
            String name,
            String message,
            Uri iconUri,
            Drawable iconDrawable) {
        if (activity.isFinishing() || activity.isDestroyed()) {
            Log.e(TAG, "activity has finish,can not show dialog!");
            request
                    .getCallback()
                    .callback(
                            new Response(
                                    Response.CODE_GENERIC_ERROR,
                                    "activity has finish,can not show dialog!"));
            return;
        }
        Dialog dialog =
                onCreateDialog(request, activity, pkg, name, message, iconUri, iconDrawable);
        DarkThemeUtil.disableForceDark(dialog);
        mDialogRef = new WeakReference<>(dialog);
        dialog.show();
    }

    protected String getDefaultMessage(Context context, String appName) {
        return context.getString(R.string.features_dlg_shortcut_message, appName);
    }

    private Response getSystemPromptEnabled(Request request) {
        String pkg = request.getApplicationContext().getPackage();
        boolean enabled = ShortcutManager.isSystemPromptEnabledByApp(pkg);
        return new Response(enabled);
    }

    private void setSystemPromptEnabled(Request request) throws JSONException {
        JSONObject params = request.getJSONParams();
        if (params == null) {
            Log.e(TAG, "Failed to set prompt enabled: params is null");
            return;
        }
        boolean enabled = params.getBoolean(ATTR_DEFAULT_PARAMS_KEY);
        String pkg = request.getApplicationContext().getPackage();
        ShortcutManager.enableSystemPromptByApp(pkg, enabled);
    }

    protected boolean isForbidden(Context context, String pkg) {
        return false;
    }

    protected boolean isExceedUseFrequencyOfDay(Context context, String pkg) {
        return false;
    }

    protected Dialog onCreateDialog(
            final Request request,
            final Context context,
            final String pkgName,
            final String name,
            final String message,
            final Uri iconUri,
            Drawable iconDrawable) {
        CheckableAlertDialog dialog = new CheckableAlertDialog(context);
        dialog.setContentView(R.layout.shortcut_dialog_content);
        View customPanel = dialog.findViewById(R.id.customPanel);
        ImageView iconView = customPanel.findViewById(R.id.icon);
        TextView titleView = customPanel.findViewById(R.id.title);
        TextView messageView = customPanel.findViewById(R.id.message);

        iconView.setImageDrawable(iconDrawable);
        titleView.setText(context.getString(R.string.features_dlg_shortcut_title));
        messageView.setText(message);

        dialog.setButton(
                DialogInterface.BUTTON_POSITIVE,
                R.string.features_dlg_shortcut_ok,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Executors.io()
                                .execute(() -> onAccept(request, context, pkgName, name, iconUri));
                    }
                });
        dialog.setButton(
                DialogInterface.BUTTON_NEGATIVE,
                R.string.features_dlg_shortcut_cancel,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        onReject(request, dialog);
                    }
                });
        dialog.setOnCancelListener(
                new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialog) {
                        onReject(request, dialog);
                    }
                });
        return dialog;
    }

    protected void onAccept(
            Request request, Context context, String pkgName, String name, Uri iconUri) {
        String path = "";
        try {
            path = request.getJSONParams() == null ? "" : request.getJSONParams().optString(PARAM_KEY_PATH);
        } catch (JSONException e) {
            Log.e(TAG, "get path error", e);
        }

        if (ShortcutManager.hasShortcutInstalled(context, pkgName)) {
            ShortcutManager.update(context, pkgName, name, iconUri);
            Response response = new Response(Response.CODE_SUCCESS, "Update success");
            request.getCallback().callback(response);
        } else {
            Source source = new Source();
            source.putExtra(Source.EXTRA_SCENE, Source.SHORTCUT_SCENE_API);
            boolean result = ShortcutManager.install(context, pkgName, path, "", name, iconUri, source);
            if (result) {
                request.getCallback().callback(Response.SUCCESS);
            } else {
                request.getCallback()
                        .callback(new Response(Response.CODE_GENERIC_ERROR, "install fail"));
            }
        }
    }

    private void onReject(Request request, DialogInterface dialog) {
        boolean forbidden = false;
        if (dialog instanceof Checkable) {
            forbidden = ((Checkable) dialog).isChecked();
        }
        onReject(request, forbidden);
    }

    protected void onReject(Request request, boolean forbidden) {
        Response response = new Response(Response.CODE_USER_DENIED, "User refuse install.");
        request.getCallback().callback(response);
    }

    @Override
    public void dispose(boolean force) {
        super.dispose(force);
        if (force) {
            Dialog dialog = mDialogRef == null ? null : mDialogRef.get();
            if (dialog != null && dialog.isShowing()) {
                dialog.dismiss();
            }
            mDialogRef = null;
        }
    }

    @Override
    public String getName() {
        return FEATURE_NAME;
    }

    @Override
    public boolean isBuiltInExtension() {
        return true;
    }
}
