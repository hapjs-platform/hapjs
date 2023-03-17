/*
 * Copyright (c) 2021-present, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.features;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff;
import android.os.Build;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import org.hapjs.bridge.Callback;
import org.hapjs.bridge.FeatureExtension;
import org.hapjs.bridge.LifecycleListener;
import org.hapjs.bridge.Request;
import org.hapjs.bridge.Response;
import org.hapjs.bridge.annotation.ActionAnnotation;
import org.hapjs.bridge.annotation.FeatureExtensionAnnotation;
import org.hapjs.common.utils.ColorUtil;
import org.hapjs.common.utils.ThemeUtils;
import org.hapjs.render.RootView;
import org.hapjs.runtime.DarkThemeUtil;
import org.hapjs.runtime.DefaultHybridDialogProviderImpl;
import org.hapjs.runtime.HybridDialog;
import org.hapjs.runtime.HybridDialogProvider;
import org.hapjs.runtime.ProviderManager;
import org.hapjs.runtime.Runtime;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

@FeatureExtensionAnnotation(
        name = Prompt.FEATURE_NAME,
        actions = {
                @ActionAnnotation(name = Prompt.ACTION_SHOW_TOAST, mode = FeatureExtension.Mode.SYNC),
                @ActionAnnotation(name = Prompt.ACTION_SHOW_DIALOG, mode = FeatureExtension.Mode.ASYNC),
                @ActionAnnotation(name = Prompt.ACTION_SHOW_CONTEXT_MENU, mode = FeatureExtension.Mode.ASYNC),
                @ActionAnnotation(name = Prompt.ACTION_SHOW_LOADING, mode = FeatureExtension.Mode.SYNC),
                @ActionAnnotation(name = Prompt.ACTION_HIDE_LOADING, mode = FeatureExtension.Mode.SYNC)
        })
public class Prompt extends FeatureExtension {
    protected static final String FEATURE_NAME = "system.prompt";
    protected static final String ACTION_SHOW_TOAST = "showToast";
    protected static final String ACTION_SHOW_DIALOG = "showDialog";
    protected static final String ACTION_SHOW_CONTEXT_MENU = "showContextMenu";
    protected static final String ACTION_SHOW_LOADING = "showLoading";
    protected static final String ACTION_HIDE_LOADING = "hideLoading";

    protected static final String PARAM_KEY_MESSAGE = "message";
    protected static final String PARAM_KEY_DURATION = "duration";
    protected static final String PARAM_KEY_TITLE = "title";
    protected static final String PARAM_KEY_BUTTONS = "buttons";
    protected static final String PARAM_KEY_ITEM_LIST = "itemList";
    protected static final String PARAM_KEY_ITEM_COLOR = "itemColor";
    protected static final String PARAM_KEY_TEXT = "text";
    protected static final String PARAM_KEY_COLOR = "color";
    protected static final String PARAM_KEY_INDEX = "index";
    protected static final String PARAM_KEY_LOADING_COLOR = "loadingColor";
    protected static final String PARAM_KEY_LOADING_MASK = "mask";
    protected static final String PARAM_KEY_AUTO_CANCEL = "autocancel";

    private View mLoadingView;
    private WindowManager mWindowManager;
    private Activity mActivity;


    @Override
    protected Response invokeInner(final Request request) throws Exception {
        String action = request.getAction();
        if (ACTION_SHOW_TOAST.equals(action)) {
            showToast(request);
        } else if (ACTION_SHOW_DIALOG.equals(action)) {
            showDialog(request);
        } else if (ACTION_SHOW_LOADING.equals(action)) {
            mActivity = request.getNativeInterface().getActivity();
            showLoading(request);
        } else if (ACTION_HIDE_LOADING.equals(action)) {
            hideLoading(request);
        } else {
            showContextMenu(request);
        }
        return Response.SUCCESS;
    }

    @Override
    public String getName() {
        return FEATURE_NAME;
    }

    private void showToast(Request request) throws JSONException {
        final Activity activity = request.getNativeInterface().getActivity();
        JSONObject params = new JSONObject(request.getRawParams());
        final String message = params.getString(PARAM_KEY_MESSAGE);
        final int duration = params.optInt(PARAM_KEY_DURATION, 0);
        activity.runOnUiThread(
                new Runnable() {
                    @Override
                    public void run() {
                        if (activity.isFinishing()) {
                            return;
                        }
                        if (duration == 1) {
                            Toast.makeText(Runtime.getInstance().getContext(), message,
                                    Toast.LENGTH_LONG).show();
                        } else {
                            Toast.makeText(Runtime.getInstance().getContext(), message,
                                    Toast.LENGTH_SHORT)
                                    .show();
                        }
                    }
                });
    }

    private void showDialog(final Request request) throws JSONException {
        Activity activity = request.getNativeInterface().getActivity();
        if (activity.isFinishing()) {
            request.getCallback().callback(Response.CANCEL);
            return;
        }

        JSONObject params = new JSONObject(request.getRawParams());
        String title = params.optString(PARAM_KEY_TITLE);
        String message = params.optString(PARAM_KEY_MESSAGE);
        boolean autoCancel = params.optBoolean(PARAM_KEY_AUTO_CANCEL, true);
        ButtonItem[] buttonItems = null;
        JSONArray jsonButtons = params.optJSONArray(PARAM_KEY_BUTTONS);
        if (jsonButtons != null) {
            final int N = jsonButtons.length();
            buttonItems = new ButtonItem[N];
            for (int i = 0; i < N; ++i) {
                JSONObject jsonButton = jsonButtons.getJSONObject(i);
                String text = jsonButton.getString(PARAM_KEY_TEXT);
                String color = jsonButton.optString(PARAM_KEY_COLOR);
                buttonItems[i] = new ButtonItem(text, color);
            }
        }

        final ButtonItem[] finalButtonItems = buttonItems;
        activity.runOnUiThread(
                new Runnable() {
                    @Override
                    public void run() {
                        final HybridDialog alertDialog =
                                getAlertDialog(
                                        activity,
                                        ThemeUtils.getAlertDialogTheme(),
                                        getButtonColorArray(finalButtonItems));

                        if (title != null) {
                            alertDialog.setTitle(title);
                        }
                        if (message != null) {
                            alertDialog.setMessage(message);
                        }
                        alertDialog.setCancelable(autoCancel);

                        if (finalButtonItems != null) {
                            if (finalButtonItems.length > 0) {
                                DialogOnClickListener onClickListener =
                                        new DialogOnClickListener(request.getCallback());
                                ButtonItem positiveButton = finalButtonItems[0];
                                alertDialog.setButton(
                                        DialogInterface.BUTTON_POSITIVE, positiveButton.text,
                                        onClickListener);
                                if (finalButtonItems.length > 1) {
                                    ButtonItem negativeButton = finalButtonItems[1];
                                    alertDialog.setButton(
                                            DialogInterface.BUTTON_NEGATIVE, negativeButton.text,
                                            onClickListener);
                                    if (finalButtonItems.length > 2) {
                                        ButtonItem neutralButton = finalButtonItems[2];
                                        alertDialog.setButton(
                                                DialogInterface.BUTTON_NEUTRAL, neutralButton.text,
                                                onClickListener);
                                    }
                                }
                            }
                            // TODO: use button color to set background of button
                        }
                        alertDialog.setOnCancelListener(
                                new DialogOnCancelListener(request.getCallback()));
                        final LifecycleListener lifecycleListener =
                                new LifecycleListener() {
                                    @Override
                                    public void onDestroy() {
                                        alertDialog.dismiss();
                                    }
                                };
                        request.getNativeInterface().addLifecycleListener(lifecycleListener);
                        alertDialog.setOnDismissListener(
                                new DialogInterface.OnDismissListener() {
                                    @Override
                                    public void onDismiss(DialogInterface dialog) {
                                        request.getNativeInterface()
                                                .removeLifecycleListener(lifecycleListener);
                                    }
                                });

                        if (alertDialog instanceof Dialog) {
                            DarkThemeUtil.disableForceDark((Dialog) alertDialog);
                        }
                        alertDialog.show();
                    }
                });
    }

    private HybridDialog getAlertDialog(Activity activity, int theme, String[] colorArray) {
        HybridDialogProvider provider = getQuickAppDialogProvider();
        return provider.createDialogWithButtonColors(activity, theme, colorArray);
    }

    private HybridDialogProvider getQuickAppDialogProvider() {
        HybridDialogProvider provider =
                ProviderManager.getDefault().getProvider(HybridDialogProvider.NAME);
        if (provider == null) {
            provider = new DefaultHybridDialogProviderImpl();
        }
        return provider;
    }

    private String[] getButtonColorArray(ButtonItem[] buttonItems) {
        if (buttonItems != null) {
            String[] colorArray = new String[buttonItems.length];
            for (int i = 0; i < buttonItems.length; i++) {
                colorArray[i] = buttonItems[i].color;
            }
            return colorArray;
        }
        return null;
    }

    protected void setTextColor(AlertDialog dialog, int whichButton, String textColor) {
        if (!TextUtils.isEmpty(textColor)) {
            dialog.getButton(whichButton).setTextColor(ColorUtil.getColor(textColor));
        }
    }

    private void showContextMenu(final Request request) throws JSONException {
        Activity activity = request.getNativeInterface().getActivity();
        if (activity.isFinishing()) {
            request.getCallback().callback(Response.CANCEL);
            return;
        }

        JSONObject params = new JSONObject(request.getRawParams());
        JSONArray jsonItemList = params.getJSONArray(PARAM_KEY_ITEM_LIST);
        final int N = jsonItemList.length();
        final String[] itemList = new String[N];
        for (int i = 0; i < N; ++i) {
            itemList[i] = jsonItemList.getString(i);
        }
        String itemColor = params.optString(PARAM_KEY_ITEM_COLOR);
        activity.runOnUiThread(
                new Runnable() {
                    @Override
                    public void run() {
                        final Dialog alertDialog =
                                getContextMenuDialog(
                                        activity,
                                        ThemeUtils.getAlertDialogTheme(),
                                        createContextMenuAdapter(activity, itemList, itemColor),
                                        new DialogOnClickListener(request.getCallback()));
                        alertDialog.setOnCancelListener(
                                new DialogOnCancelListener(request.getCallback()));
                        final LifecycleListener lifecycleListener =
                                new LifecycleListener() {
                                    @Override
                                    public void onDestroy() {
                                        alertDialog.dismiss();
                                    }
                                };
                        request.getNativeInterface().addLifecycleListener(lifecycleListener);
                        alertDialog.setOnDismissListener(
                                new DialogInterface.OnDismissListener() {
                                    @Override
                                    public void onDismiss(DialogInterface dialog) {
                                        request.getNativeInterface()
                                                .removeLifecycleListener(lifecycleListener);
                                    }
                                });

                if (alertDialog instanceof Dialog) {
                    DarkThemeUtil.disableForceDark((Dialog) alertDialog);
                }
                //无障碍适配
                dialogAccessibilityAdaptation(activity.getApplicationContext(), alertDialog);
                alertDialog.show();
            }
        });
    }

    private Dialog getContextMenuDialog(
            Activity activity,
            int theme,
            ListAdapter adapter,
            DialogInterface.OnClickListener onClickListener) {
        HybridDialogProvider provider = getQuickAppDialogProvider();
        HybridDialog dialog = provider.createAlertDialog(activity, theme);
        dialog.setAdapter(adapter, onClickListener);
        return dialog.createDialog();
    }

    protected ListAdapter createContextMenuAdapter(
            Context context, CharSequence[] objects, String itemColorString) {
        return new CheckedItemAdapter(context, objects, itemColorString);
    }

    private void showLoading(final Request request) throws JSONException {
        final Activity activity = request.getNativeInterface().getActivity();
        if (activity.isFinishing()) {
            return;
        }
        JSONObject params = new JSONObject(request.getRawParams());
        String message = params.optString(PARAM_KEY_MESSAGE);
        String loadingColor = params.optString(PARAM_KEY_LOADING_COLOR);
        boolean loadingMask = params.optBoolean(PARAM_KEY_LOADING_MASK, true);
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (mWindowManager == null) {
                    mWindowManager = (WindowManager) activity.getSystemService(Context.WINDOW_SERVICE);
                }
                if (mLoadingView != null) {
                    mWindowManager.removeView(mLoadingView);
                    mLoadingView = null;
                }
                mLoadingView = LayoutInflater.from(activity).inflate(R.layout.prompt_loading_layout, null);
                if (!TextUtils.isEmpty(loadingColor)) {
                    ProgressBar progressBar = mLoadingView.findViewById(R.id.loading_progress_bar);
                    progressBar
                            .getIndeterminateDrawable()
                            .setColorFilter(ColorUtil.getColor(loadingColor), PorterDuff.Mode.SRC_IN);
                }
                if (!TextUtils.isEmpty(message)) {
                    TextView textView = mLoadingView.findViewById(R.id.loading_text_view);
                    textView.setText(message);
                }
                WindowManager.LayoutParams layoutParams =
                        new WindowManager.LayoutParams(
                                WindowManager.LayoutParams.WRAP_CONTENT,
                                WindowManager.LayoutParams.WRAP_CONTENT,
                                WindowManager.LayoutParams.LAST_SUB_WINDOW,
                                0,
                                PixelFormat.TRANSLUCENT);
                if (loadingMask) {
                    mLoadingView.setFocusableInTouchMode(true);
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        mLoadingView.addOnUnhandledKeyEventListener(
                                new View.OnUnhandledKeyEventListener() {
                                    @Override
                                    public boolean onUnhandledKeyEvent(View v, KeyEvent event) {
                                        return doMaskLoadingKeyEvent(request, event.getKeyCode(), event);
                                    }
                                });
                    } else {
                        mLoadingView.setOnKeyListener(
                                new View.OnKeyListener() {
                                    @Override
                                    public boolean onKey(View v, int keyCode, KeyEvent event) {
                                        return doMaskLoadingKeyEvent(request, keyCode, event);
                                    }
                                });
                    }
                } else {
                    layoutParams.flags =
                            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                                    | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
                }
                layoutParams.gravity = Gravity.CENTER;
                mWindowManager.addView(mLoadingView, layoutParams);
                //无障碍适配
                viewAccessibilityAdaptation(activity, mLoadingView, message);
            }
        });
    }

    private void hideLoading(Request request) {
        final Activity activity = request.getNativeInterface().getActivity();
        if (activity != null) {
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (mLoadingView != null) {
                        if (mWindowManager == null) {
                            mWindowManager =
                                    (WindowManager)
                                            request.getNativeInterface().getActivity()
                                                    .getSystemService(Context.WINDOW_SERVICE);
                        }
                        mWindowManager.removeView(mLoadingView);
                        mLoadingView = null;
                    }
                }
            });
        }
    }

    @Override
    public void dispose(boolean force) {
        super.dispose(force);
        if (mActivity != null) {
            mActivity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (mLoadingView != null && mWindowManager != null) {
                        mWindowManager.removeView(mLoadingView);
                        mLoadingView = null;
                    }
                }
            });
        }
    }

    private Boolean doMaskLoadingKeyEvent(Request request, int keyCode, KeyEvent event) {
        if (event.getAction() == KeyEvent.ACTION_UP
                && keyCode == KeyEvent.KEYCODE_BACK
                && !event.isCanceled()) {
            RootView rootView = request.getNativeInterface().getRootView();
            if (rootView != null && rootView.canGoBack()) {
                rootView.goBack();
                return true;
            } else {
                request.getNativeInterface().getActivity().finish();
            }
        }
        return false;
    }

    private static class ButtonItem {
        String text;
        String color;

        public ButtonItem(String text, String color) {
            this.text = text;
            this.color = color;
        }
    }

    private static class DialogOnClickListener implements DialogInterface.OnClickListener {
        private Callback iCallback;

        public DialogOnClickListener(Callback callback) {
            iCallback = callback;
        }

        @Override
        public void onClick(DialogInterface dialog, int which) {
            int index;
            switch (which) {
                case DialogInterface.BUTTON_POSITIVE:
                    index = 0;
                    break;
                case DialogInterface.BUTTON_NEGATIVE:
                    index = 1;
                    break;
                case DialogInterface.BUTTON_NEUTRAL:
                    index = 2;
                    break;
                default:
                    index = which;
                    break;
            }
            JSONObject result = new JSONObject();
            try {
                result.put(PARAM_KEY_INDEX, index);
            } catch (JSONException e) {
                throw new RuntimeException(e);
            }
            iCallback.callback(new Response(result));
        }
    }

    private static class DialogOnCancelListener implements DialogInterface.OnCancelListener {
        private Callback iCallback;

        public DialogOnCancelListener(Callback callback) {
            iCallback = callback;
        }

        @Override
        public void onCancel(DialogInterface dialog) {
            iCallback.callback(Response.CANCEL);
        }
    }

    private static class CheckedItemAdapter extends ArrayAdapter<CharSequence> {
        private String itemColor;

        public CheckedItemAdapter(Context context, CharSequence[] objects, String itemColorString) {
            super(context, android.R.layout.simple_list_item_1, android.R.id.text1, objects);
            itemColor = itemColorString;
        }

        @NonNull
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view = super.getView(position, convertView, parent);
            TextView text = (TextView) view.findViewById(android.R.id.text1);
            if (!TextUtils.isEmpty(itemColor)) {
                text.setTextColor(ColorUtil.getColor(itemColor));
            }
            return view;
        }
    }

    protected void dialogAccessibilityAdaptation(Context context, Dialog dialog) {
    }

    protected void viewAccessibilityAdaptation(Context context, View view, String contentDesc) {
    }
}
