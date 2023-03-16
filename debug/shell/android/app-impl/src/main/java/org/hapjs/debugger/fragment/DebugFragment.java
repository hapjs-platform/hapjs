/*
 * Copyright (c) 2021-2022, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.debugger.fragment;

import android.Manifest;
import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.CompoundButton;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ListPopupWindow;
import android.widget.PopupWindow;
import android.widget.RelativeLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;

import org.hapjs.debug.log.DebuggerLogUtil;
import org.hapjs.debugger.AppLaunchTestActivity;
import org.hapjs.debugger.SignatureActivity;
import org.hapjs.debugger.app.impl.R;
import org.hapjs.debugger.debug.AppDebugManager;
import org.hapjs.debugger.pm.PackageInfo;
import org.hapjs.debugger.server.Server;
import org.hapjs.debugger.server.Server.PlatformInfo;
import org.hapjs.debugger.utils.HttpUtils;
import org.hapjs.debugger.utils.PreferenceUtils;

import java.util.List;

public abstract class DebugFragment extends Fragment implements AdapterView.OnItemClickListener {
    protected static final int PERMISSION_CODE_READ_EXTERNAL_STORAGE = 10001;
    protected static final int PERMISSION_CODE_READ_PHONE_STATE = 10002;
    protected static final int REQUEST_CODE_BARCODE = 20001;
    protected static final int REQUEST_CODE_PICK_PACKAGE = 20002;
    // platform version that should use old inspector
    protected static final int OLD_PLATFORM_VERSION = 1010;
    // platform version that doesn't need a debug core
    protected static final int COMBINE_DEBUG_PLATFORM_VERSION = 1050;
    /**
     * 默认IDE对应的平台
     */
    protected static final String DEFAULT_PLATFORM_PACKAGE = "org.hapjs.mockup";

    private View mDebugHintView;
    protected TextView mScanInstallBtn;
    protected TextView mLocalInstallBtn;
    protected TextView mUpdateOnlineBtn;
    protected TextView mStartDebugBtn;
    protected Switch mUsbDebugSwitch;

    private static final String TAG = "DebugFragment";
    private static final String EXTRA_IDE_PATH = "path";
    private static final String EXTRA_IDE_DEBUG = "debug";
    private static final String EXTRA_IDE_IS_ADJUSTED = "isAdjustedIde";
    private static final String SCAN_PARAMS_TARGET = "target";
    private static final String SCAN_TARGET_VALUE_SKELETON = "skeleton";
    protected boolean mIsIdeDebug;
    protected boolean mIsToolkitDebug;
    String mScanTarget;
    private AlertDialog mGetRpkErrorDialog;
    private Uri mPendingPackageUri;
    private Server mServer;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = setupViews(inflater, container);
        setupServer();
        return view;
    }

    protected abstract int getLayoutResources();

    protected abstract void setupDebug();

    protected View setupViews(LayoutInflater inflater, ViewGroup container) {
        View view = inflater.inflate(getLayoutResources(), container, false);
        View.OnClickListener clickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int id = v.getId();
                if (id == R.id.btn_scan_install) {
                    mScanInstallBtn.setText(R.string.btn_scan_installing);
                    startScanner();
                } else if (id == R.id.btn_local_install) {
                    mLocalInstallBtn.setText(R.string.btn_local_installing);
                    pickPackage();
                } else if (id == R.id.btn_update_online) {
                    mUpdateOnlineBtn.setText(R.string.btn_updating_online);
                    updateOnline();
                } else if (id == R.id.btn_start_debugging) {
                    prepareDebugging();
                }
            }
        };

        mScanInstallBtn = view.findViewById(R.id.btn_scan_install);
        mLocalInstallBtn = view.findViewById(R.id.btn_local_install);
        mUpdateOnlineBtn = view.findViewById(R.id.btn_update_online);
        mStartDebugBtn = view.findViewById(R.id.btn_start_debugging);

        mScanInstallBtn.setOnClickListener(clickListener);
        mLocalInstallBtn.setOnClickListener(clickListener);
        mUpdateOnlineBtn.setOnClickListener(clickListener);
        mStartDebugBtn.setOnClickListener(clickListener);

        mDebugHintView = view.findViewById(R.id.debug_hint);
        mDebugHintView.setOnClickListener(v -> showDebugHint(mDebugHintView));

        mUsbDebugSwitch = view.findViewById(R.id.usb_debug_switch);
        mUsbDebugSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                setInstallRpkActionsLayout(isChecked);
                PreferenceUtils.setUseADB(getActivity(), isChecked);
                if (isChecked) {
                    requestReadPhoneStatePermissionIfNeeded();
                }
                setSwitchStateText(view, R.id.usb_state_text, isChecked);
            }
        });
        mUsbDebugSwitch.setChecked(PreferenceUtils.isUseADB(getActivity()));
        setSwitchStateText(view, R.id.usb_state_text, PreferenceUtils.isUseADB(getActivity()));
        view.findViewById(R.id.launch_app_test_layout).setOnClickListener(v -> startLaunchTestActivity());
        view.findViewById(R.id.get_sign_layout).setOnClickListener(v -> startSignatureActivity());
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();

        setupDebug();

        if (!PreferenceUtils.hasShownDebugHint(getActivity())) {
            Handler handler = new Handler();
            handler.post(() -> {
                PopupWindow popupWindow = showDebugHint(mDebugHintView);
                handler.postDelayed(() -> {
                    if (popupWindow.isShowing()) {
                        popupWindow.dismiss();
                    }
                }, 6000);
            });
        }
    }

    protected void setSwitchStateText(View ancestor, int id, boolean enable) {
        ((TextView) ancestor.findViewById(id)).setText(enable ? "ON" : "OFF");
    }

    private void setInstallRpkActionsLayout(boolean isUsbEnable) {
        if (!isUsbEnable) {
            mScanInstallBtn.setVisibility(View.VISIBLE);
            FrameLayout.LayoutParams localInstallParams = (FrameLayout.LayoutParams) mLocalInstallBtn.getLayoutParams();
            localInstallParams.gravity = Gravity.CENTER;
            mLocalInstallBtn.setLayoutParams(localInstallParams);
            FrameLayout.LayoutParams updateOnlineParams = (FrameLayout.LayoutParams) mUpdateOnlineBtn.getLayoutParams();
            updateOnlineParams.gravity = Gravity.END;
            mUpdateOnlineBtn.setLayoutParams(updateOnlineParams);
        } else {
            mScanInstallBtn.setVisibility(View.GONE);
            FrameLayout.LayoutParams localInstallParams = (FrameLayout.LayoutParams) mLocalInstallBtn.getLayoutParams();
            localInstallParams.gravity = Gravity.START;
            mLocalInstallBtn.setLayoutParams(localInstallParams);
            FrameLayout.LayoutParams updateOnlineParams = (FrameLayout.LayoutParams) mUpdateOnlineBtn.getLayoutParams();
            updateOnlineParams.gravity = Gravity.CENTER;
            mUpdateOnlineBtn.setLayoutParams(updateOnlineParams);
        }
    }


    public void onNewIntent(Intent intent) {
        handleIDERequest(intent);
    }

    protected void handleIDERequest(Intent intent) {
        if (intent != null) {
            String path = intent.getStringExtra(EXTRA_IDE_PATH);
            mIsIdeDebug = intent.getBooleanExtra(EXTRA_IDE_DEBUG, false);
            boolean isAdjustedIde = intent.getBooleanExtra(EXTRA_IDE_IS_ADJUSTED, false);

            if (!TextUtils.isEmpty(path)) {
                getActivity().setIntent(null);
                PreferenceUtils.setServer(getActivity(), path);
                PreferenceUtils.setUniversalScan(getActivity(), false);
                String url = isAdjustedIde ? HttpUtils.getNotifyUrl(getActivity()) : HttpUtils.getUpdateUrl(getActivity());
                // 新版本IDE不管是否是USB模式都需要通知toolkit，老版本只在非USB模式时通知
                if (isAdjustedIde || !PreferenceUtils.isUseADB(getActivity())) {
                    new NotifyTask().execute(url);
                }
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mGetRpkErrorDialog != null) {
            mGetRpkErrorDialog.dismiss();
        }
        mServer.stop();
    }

    protected ListPopupWindow handleSpinnerIconClick(ListPopupWindow oldSpinner, ImageView spinnerIcon,
                                                     int anchorRes, List<String> content, int selectedIndex, int arrowDownRes, int arrowUpRes,
                                                     int width, int height, int verticalOffSet) {
        if (oldSpinner != null && oldSpinner.isShowing()) {
            oldSpinner.dismiss();
            spinnerIcon.setImageResource(arrowDownRes);
            return null;
        } else {
            spinnerIcon.setImageResource(arrowUpRes);
            ListPopupWindow spinner = new ListPopupWindow(getActivity());
            spinner.setOnDismissListener(() -> {
                spinnerIcon.setImageResource(arrowDownRes);
            });
            spinner.setBackgroundDrawable(getResources().getDrawable(R.drawable.platform_popup_bg));
            spinner.setAnchorView(getActivity().findViewById(anchorRes));
            spinner.setWidth(width);
            spinner.setHeight(height);
            spinner.setVerticalOffset(verticalOffSet);
            spinner.setOnItemClickListener(this);
            spinner.setModal(true);
            spinner.setAdapter(new BaseAdapter() {
                @Override
                public int getCount() {
                    return content.size();
                }

                @Override
                public Object getItem(int position) {
                    return null;
                }

                @Override
                public long getItemId(int position) {
                    return position;
                }

                @Override
                public View getView(int position, View convertView, ViewGroup parent) {
                    View itemView = convertView;
                    if (itemView == null) {
                        itemView = LayoutInflater.from(getContext()).inflate(R.layout.platform_spinner_item, parent, false);
                    }

                    int paddingTop = (int) getContext().getResources().getDimension(
                            position == 0 ? R.dimen.platform_popup_item_padding_top_first : R.dimen.platform_popup_item_padding_top);
                    int paddingBottom = (int) getContext().getResources().getDimension(
                            position == getCount() - 1 ? R.dimen.platform_popup_item_padding_bottom_last : R.dimen.platform_popup_item_padding_bottom);
                    itemView.setPadding(itemView.getPaddingLeft(), paddingTop, itemView.getPaddingRight(), paddingBottom);

                    boolean selected = position == selectedIndex;
                    itemView.findViewById(R.id.selected_hint_icon).setVisibility(selected ? View.VISIBLE : View.INVISIBLE);
                    int platformTextColor = getContext().getResources().getColor(selected
                            ? R.color.platform_spinner_text_selected_color : R.color.platform_spinner_text_unselected_color);
                    ((TextView) itemView.findViewById(R.id.platform_text)).setTextColor(platformTextColor);
                    ((TextView) itemView.findViewById(R.id.platform_text)).setText(content.get(position));

                    return itemView;
                }
            });
            spinner.show();
            spinner.getListView().setSelector(new ColorDrawable());
            return spinner;
        }
    }

    protected PopupWindow showDebugHint(View debugHintView) {
        String hintText = getDebugHintText();
        if (TextUtils.isEmpty(hintText)) {
            return null;
        }

        PopupWindow popupWindow = new PopupWindow(getActivity());
        popupWindow.setBackgroundDrawable(new ColorDrawable());
        View content = LayoutInflater.from(getActivity()).inflate(R.layout.debug_hint_popup_content, null);
        ((TextView) content.findViewById(R.id.hint_text)).setText(hintText);
        int[] anchorLoc = new int[2];
        debugHintView.getLocationOnScreen(anchorLoc);
        View triangle = content.findViewById(R.id.triangle);
        RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) triangle.getLayoutParams();
        DisplayMetrics displayMetrics = getActivity().getResources().getDisplayMetrics();
        params.rightMargin = displayMetrics.widthPixels - anchorLoc[0] - debugHintView.getWidth() / 2
                - getActivity().getResources().getDrawable(R.drawable.triangle).getIntrinsicWidth() / 2;
        triangle.setLayoutParams(params);
        popupWindow.setOutsideTouchable(true);
        popupWindow.setFocusable(true);
        popupWindow.setContentView(content);
        popupWindow.setHeight(ViewGroup.LayoutParams.WRAP_CONTENT);
        popupWindow.setWidth(ViewGroup.LayoutParams.WRAP_CONTENT);
        popupWindow.showAsDropDown(debugHintView);

        PreferenceUtils.setHasShownDebugHint(getActivity());
        return popupWindow;
    }

    protected abstract String getDebugHintText();

    protected void showUninstallDialog(final String pkg) {
        UninstallDialogFragment fragment = UninstallDialogFragment.newInstance(pkg);
        fragment.show(getActivity().getSupportFragmentManager(), "uninstall");
    }

    protected void showGetRpkErrorDialog() {
        Activity activity = getActivity();
        if (activity == null || activity.isDestroyed()
                || activity.isFinishing()
                || (mGetRpkErrorDialog != null && mGetRpkErrorDialog.isShowing())) {
            return;
        }
        boolean useADB = PreferenceUtils.isUseADB(activity);
        if (mGetRpkErrorDialog == null) {
            mGetRpkErrorDialog = new AlertDialog.Builder(activity)
                    .setPositiveButton(android.R.string.ok, null)
                    .create();
        }
        mGetRpkErrorDialog.setTitle(useADB
                ? R.string.dlg_usb_error_title
                : R.string.dlg_network_error_title);
        String message = "";
        if (useADB) {
            message = getResources().getString(R.string.dlg_usb_error_desc);
        } else {
            message = getResources().getString(R.string.dlg_network_error_desc,
                    HttpUtils.getUpdateUrl(activity));
        }
        mGetRpkErrorDialog.setMessage(message);
        mGetRpkErrorDialog.show();
    }

    protected void setupServer() {
        mServer = new Server();
        mServer.setListener(new Server.ServerListener() {
            @Override
            public void onUpdate(boolean isDebug) {
                if (getActivity() != null) {
                    mIsToolkitDebug = isDebug;
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            updateOnline();
                        }
                    });
                }
            }

            @Override
            public void onUpdateSerialNumber(String serialNumber) {
                if (getActivity() != null) {
                    PreferenceUtils.setSerialNumber(getActivity(), serialNumber);
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            DebuggerLogUtil.logMessage("DEBUGGER_DEBUG_IN_UPDATE_SERIAL_NUMBER");
                            startDebugging();
                        }
                    });
                }
            }

            @Override
            public List<PlatformInfo> onRequestAvailablePlatforms() {
                return getAvailablePlatforms();
            }

            @Override
            public void onSelectPlatform(final String pkg) {
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        DebugFragment.this.onSelectPlatform(pkg);
                    }
                });
            }

            @Override
            public void onDebug() {
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        DebuggerLogUtil.resetTraceId();
                        DebuggerLogUtil.logMessage("DEBUGGER_DEBUG_FROM_REMOTE");
                        DebugFragment.this.prepareDebugging();
                    }
                });
            }
        });
        mServer.start();
    }

    protected void setupDebuggerLogUtil() {
        DebuggerLogUtil.init(getContext().getApplicationContext(), "");
    }

    protected void startLaunchTestActivity() {
        startActivity(new Intent(getActivity(), AppLaunchTestActivity.class));
    }

    protected void startSignatureActivity() {
        startActivity(new Intent(getActivity(), SignatureActivity.class));
    }

    protected void startScanner() {
        Intent intent = new Intent();
        intent.setPackage(PreferenceUtils.getPlatformPackage(getActivity()));
        intent.setAction("org.hapjs.action.SCAN");
        if (getActivity().getPackageManager().resolveActivity(intent, 0) == null) {
            Toast.makeText(getActivity(), R.string.toast_no_barcode_scanner, Toast.LENGTH_SHORT).show();
            return;
        }
        startActivityForResult(intent, REQUEST_CODE_BARCODE);
    }

    protected void pickPackage() {
        mPendingPackageUri = null;

        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("application/*");
        CharSequence title = getString(R.string.dlg_pick_package);
        startActivityForResult(Intent.createChooser(intent, title), REQUEST_CODE_PICK_PACKAGE);
    }

    protected void updateOnline() {
        String url = HttpUtils.getUpdateUrl(getActivity());
        if (TextUtils.isEmpty(url)) {
            Toast.makeText(getActivity(), R.string.toast_no_server, Toast.LENGTH_SHORT).show();
        } else {
            AppDebugManager.getInstance(getActivity()).updateOnline(url);
        }
    }

    protected void prepareDebugging() {
        startDebugging();
    }

    public abstract void startDebugging();

    protected abstract List<PlatformInfo> getAvailablePlatforms();

    protected abstract boolean onSelectPlatform(String pkg);

    protected void requestReadPhoneStatePermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                && PackageManager.PERMISSION_GRANTED != ContextCompat.checkSelfPermission(
                getActivity(), Manifest.permission.READ_PHONE_STATE)) {
            requestPermissions(new String[]{Manifest.permission.READ_PHONE_STATE},
                    PERMISSION_CODE_READ_PHONE_STATE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_CODE_READ_EXTERNAL_STORAGE
                && grantResults[0] == PackageManager.PERMISSION_GRANTED
                && mPendingPackageUri != null) {
            AppDebugManager.getInstance(getActivity()).installLocally(mPendingPackageUri);
            mPendingPackageUri = null;
        } else if (requestCode == PERMISSION_CODE_READ_PHONE_STATE) {
            if (grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(getActivity(), R.string.toast_serial_number_user_denied, Toast.LENGTH_LONG).show();
                onUsbDebugDisable();
            }
        } else {
            Toast.makeText(getActivity(), R.string.toast_permission_fail, Toast.LENGTH_SHORT).show();
        }
    }

    protected void onUsbDebugDisable() {
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE_BARCODE) {
            if (resultCode == Activity.RESULT_OK) {
                String result = data.getStringExtra("SCAN_RESULT");
                if (result == null || result.isEmpty()) {
                    Log.e(TAG, "Scan result is empty");
                    Toast.makeText(getActivity(), R.string.toast_barcode_scanner_fail, Toast.LENGTH_SHORT).show();
                    return;
                }
                // 包管理平台的安装二维码示例：https://rpk.quickapp.cn/api/qrcode/349?raw=
                // ide骨架屏工具的安装二维码示例：http://172.25.40.97:8080?target=skeleton
                Log.i(TAG, "origin scan result is " + result);
                String server = result;
                HttpUtils.UrlEntity urlEntity = HttpUtils.parse(result);
                if (urlEntity != null) {
                    if (urlEntity.params != null && urlEntity.params.size() > 0) {
                        mScanTarget = urlEntity.params.get(SCAN_PARAMS_TARGET);
                        if (!TextUtils.isEmpty(mScanTarget)) {
                            server = urlEntity.baseUrl;
                        }
                    }
                }
                PreferenceUtils.setServer(getActivity(), server);
                PreferenceUtils.setUniversalScan(getActivity(), false);
                String updateUrl = HttpUtils.getUpdateUrl(getActivity());
                Log.i(TAG, "scan result updateUrl = " + updateUrl + ", server = " + server + ", scanTarget = " + mScanTarget);
                AppDebugManager.getInstance(getActivity()).updateOnline(updateUrl);
            }
        } else if (requestCode == REQUEST_CODE_PICK_PACKAGE) {
            if (resultCode == Activity.RESULT_OK) {
                Uri uri = data.getData();
                if ("file".equals(uri.getScheme())
                        && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                        && PackageManager.PERMISSION_GRANTED != ContextCompat.checkSelfPermission(
                        getActivity(), Manifest.permission.READ_EXTERNAL_STORAGE)) {
                    mPendingPackageUri = uri;
                    requestPermissions(
                            new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                            PERMISSION_CODE_READ_EXTERNAL_STORAGE);
                    return;
                }

                AppDebugManager.getInstance(getActivity()).installLocally(uri);
            }
        }

        if (resultCode != Activity.RESULT_OK) {
            resetButtonTextView();
        }
    }

    protected void resetButtonTextView() {
        if (getActivity() != null && !getActivity().isFinishing()) {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mScanInstallBtn.setText(R.string.btn_scan_install);
                    mLocalInstallBtn.setText(R.string.btn_local_install);
                    mUpdateOnlineBtn.setText(R.string.btn_update_online);
                }
            });
        }
    }

    public static class UninstallDialogFragment extends DialogFragment {

        public static final String PARAM_PKG = "package";

        public static UninstallDialogFragment newInstance(String pkg) {
            UninstallDialogFragment fragment = new UninstallDialogFragment();
            Bundle bundle = new Bundle();
            bundle.putString(PARAM_PKG, pkg);
            fragment.setArguments(bundle);
            return fragment;
        }

        @NonNull
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            final String pkg = getArguments().getString(PARAM_PKG);
            return new AlertDialog.Builder(getActivity())
                    .setTitle(R.string.dlg_package_cert_changed_title)
                    .setMessage(R.string.dlg_package_cert_changed_desc)
                    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            AppDebugManager.getInstance(getActivity()).uninstall(pkg);
                        }
                    })
                    .setNegativeButton(android.R.string.cancel, null)
                    .create();
        }
    }

    /**
     * 通知toolkit server调试器已经激活
     */
    private static class NotifyTask extends AsyncTask<String, Void, Void> {

        @Override
        protected Void doInBackground(String... strings) {
            if (strings != null && strings.length > 0) {
                HttpUtils.get(strings[0]);
            }
            return null;
        }
    }

    protected abstract class AppDebugListener implements AppDebugManager.DebugListener {
        @Override
        public void onInstallResult(final String pkg, boolean result, int code) {
            if (result) {
                onInstallSuccess(pkg);
            } else if (code == AppDebugManager.ERROR_CODE_PACKAGE_INCOMPATIBLE) {
                int currentPlatformVersion = AppDebugManager.getCurrentPlatformVersion();
                int minPlatformVersion = -1;
                Activity activity = getActivity();
                PackageInfo pi = AppDebugManager.getInstance(activity).getPackageInfo(pkg);
                if (pi != null) {
                    minPlatformVersion = pi.getMinPlatformVersion();
                }
                String text = activity.getString(
                        R.string.toast_platform_incompatible,
                        currentPlatformVersion,
                        pkg,
                        minPlatformVersion);
                Toast.makeText(activity, text, Toast.LENGTH_SHORT).show();
            } else if (code == AppDebugManager.SERVICE_CODE_CERTIFICATION_MISMATCH) {
                showUninstallDialog(pkg);
            } else {
                String text = getActivity().getString(R.string.toast_install_fail, code);
                Toast.makeText(getActivity(), text, Toast.LENGTH_SHORT).show();
            }
        }

        protected abstract void onInstallSuccess(String pkg);

        @Override
        public void onLaunchResult(boolean result) {
            // ignore
        }

        @Override
        public void onDebugResult(boolean result) {
            // ignore
        }

        @Override
        public void onUninstallResult(boolean result) {
            int res = result ? R.string.toast_uninstall_success : R.string.toast_uninstall_fail;
            Toast.makeText(getActivity(), res, Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onError(final int code) {
            final Activity activity = getActivity();
            if (activity == null) {
                return;
            }
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    switch (code) {
                        case AppDebugManager.ERROR_CODE_PLATFORM_NOT_CHOOSE:
                            Toast.makeText(activity, R.string.toast_platform_not_choose,
                                    Toast.LENGTH_SHORT).show();
                            break;
                        case AppDebugManager.ERROR_CODE_GET_RPK_FILE_FAILED:
                            showGetRpkErrorDialog();
                            break;
                        case AppDebugManager.ERROR_CODE_FAIL_TO_SAVE_LOCAL_FILE:  // fallthrough
                        case AppDebugManager.ERROR_CODE_FAIL_TO_CREATE_TEMP_FILE: // fallthrough
                        case AppDebugManager.ERROR_CODE_GET_PACKAGE_INFO_FAIL: // fallthrough
                        case AppDebugManager.ERROR_CODE_GET_DEBUG_SERVICE_FAIL: // fallthrough
                        case AppDebugManager.ERROR_CODE_INSTALL_GENERIC_ERROR: // fallthrough
                            String text = activity.getString(R.string.toast_install_fail, code);
                            Toast.makeText(activity, text, Toast.LENGTH_SHORT).show();
                            break;
                        case AppDebugManager.ERROR_CODE_PICK_OTHER_FILE_EXCEPT_RPK:
                            String msgText = activity.getString(R.string.toast_please_choose_rpk_file);
                            Toast.makeText(activity, msgText, Toast.LENGTH_SHORT).show();
                            break;
                        default:
                            break;
                    }
                }
            });
        }
    }
}
