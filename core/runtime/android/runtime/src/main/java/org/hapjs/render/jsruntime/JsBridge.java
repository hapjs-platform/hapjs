/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.render.jsruntime;

import android.net.Uri;
import android.os.Environment;
import android.text.TextUtils;
import android.util.Log;
import androidx.annotation.Nullable;
import com.eclipsesource.v8.JavaCallback;
import com.eclipsesource.v8.JavaVoidCallback;
import com.eclipsesource.v8.V8;
import com.eclipsesource.v8.V8Array;
import com.eclipsesource.v8.V8Object;
import java.io.File;
import org.hapjs.common.utils.UriUtils;
import org.hapjs.common.utils.ViewIdUtils;
import org.hapjs.io.AssetSource;
import org.hapjs.io.FileSource;
import org.hapjs.io.JavascriptReader;
import org.hapjs.io.RpkSource;
import org.hapjs.io.TextReader;
import org.hapjs.render.RenderActionPackage;
import org.hapjs.render.RootView;
import org.hapjs.render.action.RenderActionManager;

public class JsBridge {

    private static final String TAG = "JsBridge";

    private JsThread mJsThread;
    private RenderActionManager mRenderActionManager;

    public JsBridge(JsThread jsThread, RenderActionManager renderActionManager) {
        mJsThread = jsThread;
        mRenderActionManager = renderActionManager;
    }

    public void attach(JsBridgeCallback callback) {
        mRenderActionManager.attach(callback);
    }

    void sendRenderActions(RenderActionPackage renderActionPackage) {
        mRenderActionManager.sendRenderActions(renderActionPackage);
    }

    public void register(final V8 v8) {
        v8.registerJavaMethod(
                new JavaCallback() {
                    @Override
                    public Object invoke(V8Object receiver, V8Array parameters) {
                        return readResource(parameters.getString(0));
                    }
                },
                "readResource");
        v8.registerJavaMethod(
                new JavaVoidCallback() {
                    @Override
                    public void invoke(V8Object v8Object, final V8Array v8Array) {
                        if (v8Array == null || v8Array.length() == 0) {
                            return;
                        }
                        final int pageId = Integer.parseInt(v8Array.get(0).toString());
                        final String argsString = v8Array.getString(1);
                        JsUtils.release(v8Array);

                        mRenderActionManager.callNative(pageId, argsString);
                    }
                },
                "callNative");
        v8.registerJavaMethod(
                new JavaCallback() {
                    @Override
                    public Object invoke(V8Object object, V8Array parameters) {
                        if (parameters == null || parameters.length() == 0) {
                            return null;
                        }
                        int ref = Integer.parseInt(parameters.get(0).toString());
                        return ViewIdUtils.getViewId(ref);
                    }
                },
                "getPageElementViewId");
    }

    /**
     * source ????????????<br>
     * 1. assets:///&lt;path&gt; ?????? assets ???????????????eg: assets:///js/module/canvas.js<br>
     * 2. &lt;path&gt; ?????? rpk ??????????????????eg: manifest.json<br>
     * 3. http://... ????????????????????????<br>
     * 4. internal://... ?????????????????????????????? {@link org.hapjs.bridge.storage.file.ResourceFactory} <br>
     * 3 ??? 4 ????????????????????????????????????????????????????????????????????????????????? string ?????? byte array???
     */
    @Nullable
    private Object readResource(String source) {
        Uri uri = UriUtils.computeUri(source);
        RootView rootView = mJsThread.mRootView;
        if (uri == null) {
            // ?????? rpk ???????????????
            String resource =
                    TextReader.get()
                            .read(new RpkSource(rootView.getContext(), rootView.getPackage(),
                                    source));
            if (resource == null) {
                Log.w(TAG, "failed to read resource. source=" + source);
            }
            return resource;
        }
        String scheme = uri.getScheme();
        if (TextUtils.isEmpty(scheme)) {
            Log.w(TAG, "scheme is empty. source=" + source + ", uri=" + uri);
            return null;
        }
        switch (scheme) {
            case "assets":
                // ?????? assets ????????????
                String path = uri.getPath();
                if (TextUtils.isEmpty(path)) {
                    Log.w(TAG, "path is empty. source=" + source + ", uri=" + uri);
                    return null;
                }
                String script = null;
                if (mJsThread.isApplicationDebugEnabled()) {
                    if (path.startsWith("/js")) {
                        String newPath = path.replace("/js", "");
                        File file =
                                new File(Environment.getExternalStorageDirectory(),
                                        "quickapp/assets/js" + newPath);
                        script = JavascriptReader.get().read(new FileSource(file));
                        if (script != null) {
                            Log.d(TAG, String.format("load %s from sdcard success",
                                    file.getAbsolutePath()));
                        }
                    }
                }
                if (script == null) {
                    script =
                            TextReader.get()
                                    .read(new AssetSource(rootView.getContext(),
                                            path.replaceFirst("/", "")));
                    if (script == null) {
                        Log.w(TAG, "failed to read script. source=" + source + ", uri=" + uri);
                    }
                }
                return script;

            default:
                Log.w(TAG, "unsupported scheme. source=" + source + ", scheme=" + scheme);
                return null;
        }
    }

    public interface JsBridgeCallback {
        void onSendRenderActions(RenderActionPackage renderActionPackage);

        void onRenderSkeleton(String packageName, org.json.JSONObject parseResult);
    }
}
