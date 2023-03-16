/*
 * Copyright (c) 2021-2022, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.hapjs.debugger.fragment;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListPopupWindow;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;

import org.hapjs.debug.log.DebuggerLogUtil;
import org.hapjs.debugger.app.impl.R;
import org.hapjs.debugger.debug.AppDebugManager;
import org.hapjs.debugger.server.Server;
import org.hapjs.debugger.utils.AppUtils;
import org.hapjs.debugger.utils.HttpUtils;
import org.hapjs.debugger.utils.PreferenceUtils;

import java.util.ArrayList;
import java.util.List;

public class AppFragment extends DebugFragment implements AdapterView.OnItemClickListener {

    private static final String TAG = "AppFragment";
    private static final String KEY_RPK_ADDRESS = "rpk_address";
    private static final String KEY_PLATFORM_PKG = "platform_pkg";
    private static final String URL_HTTP_PREFIX = "http";
    private static final String URL_HTTPS_PREFIX = "https";
    private static final String KEY_CUSTOM_VALUE = "customValue";
    private static final String SCAN_TARGET_SKELETON = "skeleton";
    private static final int SUPPORT_ANALYZER_PLATFORM_VERSION = 1100;

    private TextView mDebuggablePkgNameTv;
    private TextView mDebuggablePkgTv;
    private View mDebugPkgInfoLayout;
    private View mNoDebugPkgView;

    private TextView mPlatformVersionTv;
    private TextView mPlatformVersionNameTv;
    private TextView mPlatformNameTv;
    private TextView mPlatformPkgTv;
    private TextView mCustomValueTv;

    private Switch mAnalyzerEnableSwitch;
    private View mAnalyzerContainer;

    private ImageView mPlatformSpinnerIcon;
    private ListPopupWindow mPlatformSpinner;

    private List<String> mDisplayedPlatforms = new ArrayList<String>();
    private List<String> mPlatformPackages = new ArrayList<String>();
    private AlertDialog mConfirmDialog;

    private String mPlatformOfLastInstallation;

    public AppFragment() {
        super();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = super.onCreateView(inflater, container, savedInstanceState);
        setupDebuggerLogUtil();
        return view;
    }

    @Override
    protected int getLayoutResources() {
        return R.layout.content_app;
    }

    @Override
    protected View setupViews(LayoutInflater inflater, @Nullable ViewGroup container) {
        View view = super.setupViews(inflater, container);

        mDebuggablePkgNameTv = view.findViewById(R.id.txt_debuggable_pkg_name);
        mDebuggablePkgTv = view.findViewById(R.id.txt_debuggable_pkg);
        mDebugPkgInfoLayout = view.findViewById(R.id.debug_pkg_info_layout);
        mNoDebugPkgView = view.findViewById(R.id.txt_no_debuggable_pkg);

        mPlatformNameTv = view.findViewById(R.id.platform_name);
        mPlatformPkgTv = view.findViewById(R.id.platform_pkg);
        mPlatformVersionTv = view.findViewById(R.id.platform_version_text);
        mPlatformVersionNameTv = view.findViewById(R.id.platform_version_name_text);
        mCustomValueTv = view.findViewById(R.id.custom_value_text);

        mPlatformSpinnerIcon = view.findViewById(R.id.platform_spinner_icon);
        mPlatformSpinnerIcon.setVisibility(View.GONE);
        mPlatformSpinnerIcon.setOnClickListener(v ->
                mPlatformSpinner = handleSpinnerIconClick(mPlatformSpinner, mPlatformSpinnerIcon,
                        R.id.platform_info_primary_layout, mDisplayedPlatforms, getSelectedPlatformIndex(),
                        R.drawable.arrow_down, R.drawable.arrow_up,
                        (int) getResources().getDimension(R.dimen.platform_popup_width),
                        ListPopupWindow.WRAP_CONTENT,
                        0));

        mAnalyzerContainer = view.findViewById(R.id.analyzer_container);
        mAnalyzerEnableSwitch = view.findViewById(R.id.analyzer_enable);
        mAnalyzerEnableSwitch.setChecked(PreferenceUtils.isUseAnalyzer(getActivity()));
        mAnalyzerEnableSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                PreferenceUtils.setUseAnalyzer(getActivity(), isChecked);
                setSwitchStateText(view, R.id.analyzer_state_text, isChecked);
            }
        });
        setSwitchStateText(view, R.id.analyzer_state_text, mAnalyzerEnableSwitch.isChecked());

        return view;
    }

    private int getSelectedPlatformIndex() {
        if (!mPlatformPackages.isEmpty()) {
            String selectedPlatform = PreferenceUtils.getPlatformPackage(getActivity());
            for (int i = 0; i < mPlatformPackages.size(); ++i) {
                if (TextUtils.equals(mPlatformPackages.get(i), selectedPlatform)) {
                    return i;
                }
            }
        }
        return -1;
    }

    @Override
    protected String getDebugHintText() {
        CharSequence platformName = mPlatformNameTv.getText();
        if (TextUtils.isEmpty(platformName)) {
            return null;
        }
        return getContext().getString(R.string.hint_no_response, platformName);
    }

    @Override
    protected void setupDebug() {
        AppDebugManager.getInstance(getActivity()).setDebugListener(new AppDebugListener() {
            @Override
            public void onInstallResult(String pkg, boolean result, int code) {
                resetButtonTextView();
                super.onInstallResult(pkg, result, code);
            }


            @Override
            public void onError(int code) {
                resetButtonTextView();
                super.onError(code);
            }

            @Override
            protected void onInstallSuccess(String pkg) {
                PreferenceUtils.setDebugPackage(getActivity(), pkg);
                mPlatformOfLastInstallation = PreferenceUtils.getPlatformPackage(getActivity());
                getActivity().runOnUiThread(() -> setupDebugPkgViews(mPlatformOfLastInstallation));
                if (isNeedDebug()) {
                    DebuggerLogUtil.resetTraceId();
                    DebuggerLogUtil.logMessage("DEBUGGER_DEBUG_IN_INSTALL_SUCCESS");
                    prepareDebugging();
                } else {
                    AppDebugManager.getInstance(getActivity()).launchPackage(pkg);
                }
                mIsIdeDebug = false;
                mIsToolkitDebug = false;
                mScanTarget = null;
            }
        });
    }

    private boolean isNeedDebug() {
        return mIsIdeDebug || mIsToolkitDebug
                || SCAN_TARGET_SKELETON.equals(mScanTarget);
    }

    @Override
    public void onStart() {
        super.onStart();
        //通用扫码打开快应用时根据platform_pkg参数设置打开快应用的引擎平台
        setupPlatformPackage(getActivity().getIntent());

        mPlatformPackages = AppDebugManager.getInstance(getActivity()).getPlatformPackages();
        mDisplayedPlatforms = new ArrayList<>();
        if (mPlatformPackages.size() == 0) {
            refreshButtons(false);
            mPlatformSpinnerIcon.setVisibility(View.GONE);
            mPlatformNameTv.setVisibility(View.VISIBLE);
            mPlatformNameTv.setMovementMethod(LinkMovementMethod.getInstance());
            mPlatformNameTv.setText(getDebugHintText());
            mPlatformPkgTv.setVisibility(View.GONE);
            setupDebugPkgViews(null);
            return;
        }

        for (String packageName : mPlatformPackages) {
            final PackageManager pm = getActivity().getApplicationContext().getPackageManager();
            ApplicationInfo ai;
            try {
                ai = pm.getApplicationInfo(packageName, 0);
            } catch (final PackageManager.NameNotFoundException e) {
                ai = null;
            }
            String applicationName = (String) (ai != null ? pm.getApplicationLabel(ai) : "unknown");
            String displayPlatform = applicationName + " (" + packageName + ") ";
            mDisplayedPlatforms.add(displayPlatform);
        }

        if (mPlatformPackages.size() == 1) {
            //if there is only one platform, we choose it and don't show the Spinner
            mPlatformSpinnerIcon.setVisibility(View.GONE);
            setupPlatformInfo(mPlatformPackages.get(0));
            PreferenceUtils.setPlatformPackage(getActivity(), mPlatformPackages.get(0));
            refreshButtons(true);
            handleIDERequest(getActivity().getIntent());
            handleUniversalScanRequest(getActivity().getIntent());
            return;
        }

        String platformPackage = PreferenceUtils.getPlatformPackage(getActivity());
        if (TextUtils.isEmpty(platformPackage) || !mPlatformPackages.contains(platformPackage)) {
            if (mPlatformPackages.contains(DEFAULT_PLATFORM_PACKAGE)) {
                platformPackage = DEFAULT_PLATFORM_PACKAGE;
            } else {
                platformPackage = mPlatformPackages.get(0);
            }
            PreferenceUtils.setPlatformPackage(getActivity(), platformPackage);
        }

        if (TextUtils.isEmpty(platformPackage)) {
            refreshButtons(false);
            return;
        }

        mPlatformSpinnerIcon.setVisibility(View.VISIBLE);
        setupPlatformInfo(platformPackage);
        for (int i = 0; i < mPlatformPackages.size(); i++) {
            if (platformPackage.equals(mPlatformPackages.get(i))) {
                resetOnItemSelectedListener(getActivity().getIntent(), platformPackage);
                break;
            }
        }
        if (!mPlatformPackages.contains(platformPackage)) {
            Toast.makeText(getActivity(), R.string.toast_app_not_install, Toast.LENGTH_SHORT).show();
        }
        handleIDERequest(getActivity().getIntent());
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        AppDebugManager.getInstance(getActivity()).unbindDebugService();
        if (mPlatformSpinner != null && mPlatformSpinner.isShowing()) {
            mPlatformSpinner.dismiss();
        }
    }

    private void setupPlatformPackage(Intent intent) {
        if (intent != null && intent.getData() != null) {
            final Uri uri = intent.getData();
            String rpkAddress = uri.getQueryParameter(KEY_RPK_ADDRESS);
            String platformPkg = uri.getQueryParameter(KEY_PLATFORM_PKG);
            if (!TextUtils.isEmpty(rpkAddress) && !TextUtils.isEmpty(platformPkg)) {
                Log.i(TAG, "setupPlatformPackage -- rpk_address:" + rpkAddress);
                PreferenceUtils.setPlatformPackage(getActivity(), platformPkg);
            }
        }
    }

    @Override
    public void onNewIntent(Intent intent) {
        refreshPlatformInfo(intent);
        super.onNewIntent(intent);
    }

    @Override
    public void onStop() {
        super.onStop();
        if (mConfirmDialog != null) {
            mConfirmDialog.dismiss();
        }
    }

    private void refreshPlatformInfo(final Intent intent) {
        if (intent != null && intent.getData() != null) {
            final Uri uri = intent.getData();
            String rpkAddress = uri.getQueryParameter(KEY_RPK_ADDRESS);
            final String platformPkg = uri.getQueryParameter(KEY_PLATFORM_PKG);
            if (!TextUtils.isEmpty(rpkAddress)) {
                if (!TextUtils.isEmpty(platformPkg)) {
                    String curPlatformPkg = PreferenceUtils.getPlatformPackage(getActivity());
                    if (!TextUtils.equals(curPlatformPkg, platformPkg)) {
                        //根据引擎包名重新设置平台信息并解绑旧的DebugService
                        for (int i = 0; i < mPlatformPackages.size(); i++) {
                            if (platformPkg.equals(mPlatformPackages.get(i))) {
                                resetOnItemSelectedListener(intent, platformPkg);
                                mPlatformSpinner.setSelection(i + 1);
                                return;
                            }
                        }
                    }
                }
                handleUniversalScanRequest(intent);
            }
        }
    }

    private void resetOnItemSelectedListener(final Intent intent, final String platformPackage) {
        if (intent != null && intent.getData() != null) {
            final Uri uri = intent.getData();
            String rpkAddress = uri.getQueryParameter(KEY_RPK_ADDRESS);
            if (!TextUtils.isEmpty(rpkAddress)) {
                mPlatformSpinner.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                        Log.i(TAG, "resetOnItemSelectedListener--onItemClick()");
                        PreferenceUtils.setPlatformPackage(getActivity(), platformPackage);
                        setupPlatformInfo(platformPackage);
                        refreshButtons(true);
                        //由于OnItemSelected方法回调时间不确定， 需在调用handleUniversalScanRequest(intent)
                        //方法前先解绑旧的DebugService。
                        AppDebugManager.getInstance(getActivity()).unbindDebugService();
                        mPlatformSpinner.setOnItemClickListener(AppFragment.this);
                        handleUniversalScanRequest(intent);
                        mPlatformSpinner.dismiss();
                    }
                });
            }
        }
    }

    protected void handleUniversalScanRequest(Intent intent) {
        if (intent != null && intent.getData() != null) {
            final Uri uri = intent.getData();
            String rpkAddress = uri.getQueryParameter(KEY_RPK_ADDRESS);
            if (!TextUtils.isEmpty(rpkAddress)) {
                Log.i(TAG, "handleUniversalScanRequest() -- rpk_address:" + rpkAddress);
                if (rpkAddress.toLowerCase().startsWith(URL_HTTP_PREFIX)
                        || rpkAddress.toLowerCase().startsWith(URL_HTTPS_PREFIX)) {
                    intent.setData(null);
                    createConfirmDialog(getActivity(), rpkAddress);
                }
            }
        }
    }

    private void createConfirmDialog(final Activity activity, final String rpkAddress) {
        if (activity.isDestroyed() || activity.isFinishing()
                || (mConfirmDialog != null && mConfirmDialog.isShowing())) {
            return;
        }
        AlertDialog.Builder builder = new AlertDialog
                .Builder(activity)
                .setTitle(R.string.dlg_universal_scan_confirm_title)
                .setPositiveButton(getString(android.R.string.ok),
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                mConfirmDialog.dismiss();
                                PreferenceUtils.setServer(activity, rpkAddress);
                                PreferenceUtils.setUniversalScan(activity, true);
                                AppDebugManager.getInstance(activity).updateOnline(rpkAddress);
                            }
                        })
                .setNegativeButton(getString(android.R.string.cancel),
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                mConfirmDialog.dismiss();
                            }
                        })
                .setOnCancelListener(new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialog) {
                        mConfirmDialog.dismiss();
                    }
                });
        mConfirmDialog = builder.create();
        mConfirmDialog.setCanceledOnTouchOutside(false);
        mConfirmDialog.show();
    }

    private void setupPlatformInfo(String platformPackage) {
        try {
            mPlatformPkgTv.setVisibility(View.VISIBLE);
            mPlatformPkgTv.setText(" (" + platformPackage + ")");

            ApplicationInfo appInfo = getActivity().getPackageManager()
                    .getApplicationInfo(platformPackage, PackageManager.GET_META_DATA);
            CharSequence platformName = getActivity().getPackageManager().getApplicationLabel(appInfo);
            mPlatformNameTv.setVisibility(View.VISIBLE);
            mPlatformNameTv.setText(platformName);

            int version = appInfo.metaData.getInt("platformVersion");
            float versionName = appInfo.metaData.getFloat("platformVersionName");
            String customValue = appInfo.metaData.getString(KEY_CUSTOM_VALUE);

            PackageInfo platformInfo = getActivity().getPackageManager().getPackageInfo(platformPackage, 0);
            String platformVersion = String.format(
                    getActivity().getResources().getString(R.string.text_platform_version_code),
                    String.valueOf(version), platformInfo != null ? String.valueOf(platformInfo.versionCode) : "");
            String platformVersionName = String.format(
                    getActivity().getResources().getString(R.string.text_platform_version_name),
                    String.valueOf(versionName));

            if (!TextUtils.isEmpty(customValue)) {
                mCustomValueTv.setVisibility(View.VISIBLE);
                mCustomValueTv.setText(customValue);
            } else {
                mCustomValueTv.setVisibility(View.GONE);
            }

            if (version < 1000) {
                platformVersionName = String.format(getActivity().getResources().getString(R.string.text_platform_version_name),
                        getActivity().getResources().getString(R.string.text_platform_version_beta));
                mPlatformVersionTv.setVisibility(View.GONE);
            } else {
                mPlatformVersionTv.setVisibility(View.VISIBLE);
                mPlatformVersionTv.setText(platformVersion);
            }
            mPlatformVersionNameTv.setVisibility(View.VISIBLE);
            mPlatformVersionNameTv.setText(platformVersionName);
            AppDebugManager.setCurrentPlatformVersion(version);

            if (version < SUPPORT_ANALYZER_PLATFORM_VERSION) {
                mAnalyzerContainer.setVisibility(View.GONE);
                PreferenceUtils.setUseAnalyzer(getContext(), false);
            } else {
                mAnalyzerContainer.setVisibility(View.VISIBLE);
                PreferenceUtils.setUseAnalyzer(getContext(), mAnalyzerEnableSwitch.isChecked());
            }
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, "Platform not found: ", e);
        }
        setupDebugPkgViews(platformPackage);
    }

    private void setupDebugPkgViews(String selectedPlatform) {
        String pkg = PreferenceUtils.getDebugPackage(getActivity());
        mStartDebugBtn.setEnabled(!TextUtils.isEmpty(selectedPlatform) && !TextUtils.isEmpty(pkg));

        if (!TextUtils.isEmpty(pkg)) {
            mDebugPkgInfoLayout.setVisibility(View.VISIBLE);
            mNoDebugPkgView.setVisibility(View.GONE);

            mDebuggablePkgTv.setText(pkg);
            org.hapjs.debugger.pm.PackageInfo pkgInfo = null;
            if (!TextUtils.isEmpty(selectedPlatform) && selectedPlatform.equals(mPlatformOfLastInstallation)) {
                pkgInfo = AppDebugManager.getInstance(getActivity()).getPackageInfo(pkg);
            }
            if (pkgInfo != null) {
                mDebuggablePkgNameTv.setVisibility(View.VISIBLE);
                mDebuggablePkgNameTv.setText(String.format("%s (%s)",
                        pkgInfo.getName(), pkgInfo.getVersionName()));
            } else {
                mDebuggablePkgNameTv.setVisibility(View.GONE);
            }
        } else {
            mDebugPkgInfoLayout.setVisibility(View.GONE);
            mNoDebugPkgView.setVisibility(View.VISIBLE);
        }
    }

    private void refreshButtons(boolean hasSelectedPlatform) {
        mScanInstallBtn.setEnabled(hasSelectedPlatform);
        mLocalInstallBtn.setEnabled(hasSelectedPlatform);
        mUpdateOnlineBtn.setEnabled(hasSelectedPlatform);
        mStartDebugBtn.setEnabled(hasSelectedPlatform
                && !TextUtils.isEmpty(PreferenceUtils.getDebugPackage(getActivity())));
    }

    @Override
    public void startDebugging() {
        String pkg = PreferenceUtils.getDebugPackage(getActivity());
        if (TextUtils.isEmpty(pkg)) {
            Toast.makeText(getActivity(), R.string.toast_no_debuggable_package, Toast.LENGTH_SHORT).show();
        }
        String server = HttpUtils.getDebugServer(getActivity());
        if (TextUtils.isEmpty(server)) {
            Toast.makeText(getActivity(), R.string.toast_no_server, Toast.LENGTH_SHORT).show();
        }
        String sn = AppUtils.getSerialNumber();
        if (PreferenceUtils.isUseADB(getActivity()) && TextUtils.isEmpty(sn)) {
            Log.i(TAG, "startDebugging: no serial number found, ask to npm server");
            AppDebugManager.getInstance(getActivity()).searchSerialNumber();
            DebuggerLogUtil.logMessage("DEBUGGER_NO_SERIAL_NUMBER");
            return;
        }
        AppDebugManager.getInstance(getActivity()).startDebugging(pkg, server, mScanTarget);
    }

    @Override
    protected List<Server.PlatformInfo> getAvailablePlatforms() {
        List<String> platformPackages = new ArrayList<>(mPlatformPackages);
        List<String> displayPlatforms = new ArrayList<>(mDisplayedPlatforms);
        List<Server.PlatformInfo> platformInfos = new ArrayList<>();
        for (int i = 0; i < platformPackages.size() && i < displayPlatforms.size(); ++i) {
            try {
                PackageInfo packageInfo = getActivity().getPackageManager().getPackageInfo(platformPackages.get(i), 0);
                long version = Build.VERSION.SDK_INT >= Build.VERSION_CODES.P
                        ? packageInfo.getLongVersionCode() : packageInfo.versionCode;
                platformInfos.add(new Server.PlatformInfo(platformPackages.get(i), displayPlatforms.get(i), version));
            } catch (PackageManager.NameNotFoundException e) {
                Log.e(TAG, "failed to get package info for " + platformPackages.get(i), e);
            }
        }
        return platformInfos;
    }

    @Override
    protected boolean onSelectPlatform(String pkg) {
        if (mPlatformPackages.contains(pkg)) {
            String currentPkg = PreferenceUtils.getPlatformPackage(getActivity());
            if (!TextUtils.equals(pkg, currentPkg)) {
                selectPlatform(pkg);
            }
            return true;
        } else {
            Log.w(TAG, "platform: " + pkg + " has lost");
            return false;
        }
    }

    @Override
    protected void onUsbDebugDisable() {
        mUsbDebugSwitch.setChecked(false);
        PreferenceUtils.setUseADB(getActivity(), false);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        Log.i(TAG, "onItemClick()");
        String platformPackage = mPlatformPackages.get(position);
        selectPlatform(platformPackage);
        mPlatformSpinner.dismiss();
    }

    private void selectPlatform(String platformPackage) {
        PreferenceUtils.setPlatformPackage(getActivity(), platformPackage);
        setupPlatformInfo(platformPackage);
        refreshButtons(true);
        //now we have a new runtime platform, so unbind previous one
        AppDebugManager.getInstance(getActivity()).unbindDebugService();
    }
}
