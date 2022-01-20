/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.debugger.fragment;

import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatSpinner;
import androidx.appcompat.widget.SwitchCompat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.hapjs.debug.log.DebuggerLogUtil;
import org.hapjs.debugger.HintSpinnerAdapter;
import org.hapjs.debugger.app.impl.R;
import org.hapjs.debugger.debug.AppDebugManager;
import org.hapjs.debugger.debug.CardDebugManager;
import org.hapjs.debugger.debug.CardHostInfo;
import org.hapjs.debugger.server.Server;
import org.hapjs.debugger.utils.HttpUtils;
import org.hapjs.debugger.utils.PreferenceUtils;

public class CardFragment extends DebugFragment implements AdapterView.OnItemSelectedListener {

    private static final String TAG = "CardFragment";

    private static final int MSG_UPDATE_CARD_HOSTS = 0;
    private static final long CARD_HOST_UPDATE_DELAYED_TIME = 3000;
    private static final long GET_HOSTS_MAX_WAITING_TIME = 3000;

    private static final String KEY_CARD_PATH = "cardPath";

    private TextView mPlatformVersionTv;
    private TextView mPlatformVersionNameTv;
    private TextView mCardHostTv;
    private TextView mCorePlatformTv;
    private TextView mTxtDebuggablePackage;

    private Button mScanInstallBtn;
    private Button mLocalInstallBtn;
    private Button mUpdateOnlineBtn;
    private Button mCardStartDebugBtn;

    private AppCompatSpinner mCardHostSpinner;
    private AppCompatSpinner mCardSpinner;

    private View mCardView;
    private SwitchCompat mUsbDebugSwitch;

    private List<String> mDisplayedCardHosts = new ArrayList<>();
    private List<CardHostInfo> mCardHosts = new ArrayList<>();
    private List<String> mCardList = new ArrayList<>();
    private Map<String, String> mCardMap;
    private ArrayAdapter<String> mArrayAdapter;
    private ArrayAdapter<String> mCardArrayAdapter;
    private CardHostInfo mCardHost;

    private long mLastQueryTime;

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_UPDATE_CARD_HOSTS:
                    refreshHostPlatformView((List<CardHostInfo>) msg.obj);
                    break;
                default:
                    break;
            }
        }
    };

    public CardFragment() {
        super();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = setupViews(inflater, container);
        setupDebug();
        setupServer();
        setupDebuggerLogUtil();
        return view;
    }

    private View setupViews(LayoutInflater inflater, @Nullable ViewGroup container) {
        View view = inflater.inflate(R.layout.content_card, container, false);
        View.OnClickListener clickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (v.getId() == R.id.card_btnScanInstall) {
                    startScanner();
                } else if (v.getId() == R.id.card_btnLocalInstall) {
                    pickPackage();
                } else if (v.getId() == R.id.card_btnUpdateOnline) {
                    updateOnline();
                } else if (v.getId() == R.id.card_btnStartDebugging) {
                    DebuggerLogUtil.resetTraceId();
                    DebuggerLogUtil.logMessage("DEBUGGER_TOUCHED_START");
                    prepareDebugging();
                }
            }
        };

        mCardHostTv = view.findViewById(R.id.card_text_host);
        mCorePlatformTv = view.findViewById(R.id.card_text_platform);
        mPlatformVersionTv = view.findViewById(R.id.card_platform_version_text);
        mPlatformVersionNameTv = view.findViewById(R.id.card_platform_version_name_text);
        mTxtDebuggablePackage = view.findViewById(R.id.card_txtDebuggablePackage);

        mScanInstallBtn = view.findViewById(R.id.card_btnScanInstall);
        mLocalInstallBtn = view.findViewById(R.id.card_btnLocalInstall);
        mUpdateOnlineBtn = view.findViewById(R.id.card_btnUpdateOnline);

        mScanInstallBtn.setOnClickListener(clickListener);
        mLocalInstallBtn.setOnClickListener(clickListener);
        mUpdateOnlineBtn.setOnClickListener(clickListener);

        mCardHostSpinner = view.findViewById(R.id.card_host_spinner);
        mArrayAdapter = new ArrayAdapter<String>(getContext(), R.layout.item_spinner_select, mDisplayedCardHosts);
        mArrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        HintSpinnerAdapter adapter = new HintSpinnerAdapter(mArrayAdapter, R.layout.item_hint, getContext());
        mCardHostSpinner.setAdapter(adapter);
        mCardHostSpinner.setOnItemSelectedListener(this);

        mCardView = view.findViewById(R.id.cards_view);
        mCardSpinner = view.findViewById(R.id.cards_spinner);
        mCardStartDebugBtn = view.findViewById(R.id.card_btnStartDebugging);
        mCardStartDebugBtn.setOnClickListener(clickListener);
        mCardArrayAdapter = new ArrayAdapter<String>(getContext(), R.layout.item_spinner_select, mCardList);
        mCardArrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        HintSpinnerAdapter cardAdapter = new HintSpinnerAdapter(mCardArrayAdapter, R.layout.item_card_hint,
                getContext());
        mCardSpinner.setAdapter(cardAdapter);
        mCardSpinner.setOnItemSelectedListener(this);

        mUsbDebugSwitch = ((SwitchCompat) view.findViewById(R.id.usb_debug_switch));
        mUsbDebugSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                mScanInstallBtn.setVisibility(isChecked ? View.GONE : View.VISIBLE);
                PreferenceUtils.setUseADB(getActivity(), isChecked);
                if (isChecked) {
                    requestReadPhoneStatePermissionIfNeeded();
                }
            }
        });
        mUsbDebugSwitch.setChecked(PreferenceUtils.isUseADB(getActivity()));

        enableDebug(false);

        return view;
    }

    protected void setupDebug() {
        AppDebugManager.getInstance(getContext()).setDebugListener(new AppDebugListener() {
            @Override
            protected void onInstallSuccess(final String pkg) {
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        setDebugPackageView(pkg, true);
                    }
                });
            }
        });
        CardDebugManager.getInstance(getContext()).setDebugListener(new CardDebugManager.CardDebugListener() {

            @Override
            public void onLaunchResult() {
                // ignore
            }

            @Override
            public void onDebugResult() {
                // ignore
            }

            @Override
            public void onError(int code) {
                int resId;
                switch (code) {
                    case CardDebugManager.SERVICE_CODE_UNSUPPORT_DEBUG:
                        resId = R.string.toast_platform_not_support_debug;
                        break;
                    default:
                        resId = R.string.toast_debug_error;
                        break;
                }
                Toast.makeText(getContext(), String.format("%d: %s", code, getString(resId)),
                        Toast.LENGTH_SHORT).show();
            }

            @Override
            public void updateCardHosts(List<CardHostInfo> cardHosts) {
                mHandler.removeMessages(MSG_UPDATE_CARD_HOSTS);

                long nextUpdateDelayed = 0;
                CardHostInfo cardHost = CardHostInfo.fromString(PreferenceUtils.getCardHostPlatform(getContext()));
                if (!cardHosts.contains(cardHost)) {
                    nextUpdateDelayed = mLastQueryTime + CARD_HOST_UPDATE_DELAYED_TIME - SystemClock.elapsedRealtime();
                }

                Message msg = Message.obtain(mHandler, MSG_UPDATE_CARD_HOSTS, cardHosts);
                mHandler.sendMessageDelayed(msg, nextUpdateDelayed);
            }
        });
    }

    @Override
    protected void handleIDERequest(Intent intent) {
        if (intent != null && intent.hasExtra(KEY_CARD_PATH)) {
            PreferenceUtils.setDebugCardPath(getActivity(), intent.getStringExtra(KEY_CARD_PATH));
        }

        super.handleIDERequest(intent);

        if (intent != null && intent.hasExtra(KEY_CARD_PATH)) {
            getActivity().setIntent(null);
        }
    }

    @Override
    public void onStart() {
        super.onStart();

        CardDebugManager.getInstance(getContext()).querySupportedHostPlatforms();
        mLastQueryTime = SystemClock.elapsedRealtime();
    }

    private void refreshHostPlatformView(List<CardHostInfo> cardHosts) {
        if (mCardHost != null && !isCardHostsChanged(cardHosts)) {
            refreshCorePlatformInfo(mCardHost);
            return;
        }

        mCardHosts = cardHosts;
        if (mCardHosts.size() == 0) {
            setCardHost(null);
            enableDebug(false);
            mCardHostTv.setVisibility(View.GONE);
            mCardHostSpinner.setVisibility(View.GONE);
            return;
        }

        mArrayAdapter.clear();
        for (CardHostInfo cardHostInfo : mCardHosts) {
            final PackageManager pm = getContext().getApplicationContext().getPackageManager();
            ApplicationInfo ai;
            try {
                ai = pm.getApplicationInfo(cardHostInfo.archiveHost, 0);
            } catch (final PackageManager.NameNotFoundException e) {
                ai = null;
            }
            String applicationName = (String) (ai != null ? pm.getApplicationLabel(ai) : "unknown: ");
            String displayCardHost = applicationName + " (" + cardHostInfo.archiveHost + ") ";
            mDisplayedCardHosts.add(displayCardHost);
        }
        mArrayAdapter.notifyDataSetChanged();

        if (mCardHosts.size() == 1) {
            //if there is only one platform, we choose it and don't show the Spinner
            CardHostInfo cardHostInfo = mCardHosts.get(0);
            setCardHost(cardHostInfo);
            mCardHostTv.setVisibility(View.VISIBLE);
            mCardHostTv.setText(mDisplayedCardHosts.get(0));
            mCardHostSpinner.setVisibility(View.GONE);
            return;
        } else {
            mCardHostTv.setVisibility(View.GONE);
            mCardHostSpinner.setVisibility(View.VISIBLE);
        }

        CardHostInfo cardHostInfo = CardHostInfo.fromString(PreferenceUtils.getCardHostPlatform(getContext()));
        if (!mCardHosts.contains(cardHostInfo)) {
            String pkg = getContext().getPackageName();
            CardHostInfo debuggerHost = new CardHostInfo(pkg, pkg);
            if (mCardHosts.contains(debuggerHost)) {
                // 优先显示debugger
                cardHostInfo = debuggerHost;
            } else {
                cardHostInfo = mCardHosts.get(0);
            }
        }
        setCardHost(cardHostInfo);

        for (int i = 0; i < mCardHosts.size(); i++) {
            if (cardHostInfo.equals(mCardHosts.get(i))) {
                mCardHostSpinner.setSelection(i + 1, true);
                break;
            }
        }
    }

    private boolean isCardHostsChanged(List<CardHostInfo> cardHosts) {
        if (cardHosts.size() != mCardHosts.size()) {
            return true;
        }
        for (CardHostInfo cardHost : cardHosts) {
            if (!mCardHosts.contains(cardHost)) {
                return true;
            }
        }
        return false;
    }

    private void setCardHost(CardHostInfo cardHost) {
        if (!cardHost.equals(mCardHost)) {
            mCardHost = cardHost;
            enableDebug(false);
            PreferenceUtils.setCardHostPlatform(getContext(), cardHost.toString());
            //now we have a new runtime platform, so unbind previous one
            AppDebugManager.getInstance(getContext()).unbindDebugService();
            refreshCorePlatformInfo(cardHost);
            setDebugPackageView(PreferenceUtils.getDebugCardPackage(getContext()), false);
        }
    }

    private void refreshCorePlatformInfo(CardHostInfo cardHostInfo) {
        final String corePlatform = CardDebugManager.getInstance(getContext()).getCorePlatform(cardHostInfo.archiveHost);
        PreferenceUtils.setPlatformPackage(getContext(), corePlatform);
        if (TextUtils.isEmpty(corePlatform)) {
            mPlatformVersionTv.setVisibility(View.GONE);
            mPlatformVersionNameTv.setVisibility(View.GONE);
            mCorePlatformTv.setVisibility(View.GONE);
            enableDebug(false);
            return;
        }
        try {
            mCorePlatformTv.setVisibility(View.VISIBLE);
            mCorePlatformTv.setText(corePlatform);
            ApplicationInfo appInfo = getContext().getPackageManager()
                    .getApplicationInfo(corePlatform, PackageManager.GET_META_DATA);
            int version = appInfo.metaData.getInt("platformVersion");
            float versionName = appInfo.metaData.getFloat("platformVersionName");

            PackageInfo platformInfo = getActivity().getPackageManager().getPackageInfo(corePlatform, 0);
            String platformVersion = String.format(getString(R.string.text_platform_version_code),
                    String.valueOf(version), platformInfo != null ? String.valueOf(platformInfo.versionCode) : "");
            String platformVersionName = String.format(getString(R.string.text_platform_version_name),
                    String.valueOf(versionName));
            mPlatformVersionNameTv.setVisibility(View.VISIBLE);
            mPlatformVersionNameTv.setText(platformVersionName);
            mPlatformVersionTv.setVisibility(View.VISIBLE);
            mPlatformVersionTv.setText(platformVersion);
            enableDebug(true);
            handleIDERequest(getActivity().getIntent());
            AppDebugManager.setCurrentPlatformVersion(version);
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, "Platform not found: ", e);
            mPlatformVersionNameTv.setVisibility(View.GONE);
            mPlatformVersionTv.setVisibility(View.GONE);
            enableDebug(false);
        }
    }

    private void setDebugPackageView(String pkg, boolean launch) {
        if (TextUtils.isEmpty(pkg)) {
            mCardView.setVisibility(View.GONE);
            return;
        }
        PreferenceUtils.setDebugCardPackage(getContext(), pkg);
        mTxtDebuggablePackage.setText(pkg);

        org.hapjs.debugger.pm.PackageInfo packageInfo = CardDebugManager.getInstance(getContext()).getPackageInfo(pkg);
        mCardMap = packageInfo != null ? packageInfo.getCardInfos() : null;

        if (mCardMap == null || mCardMap.isEmpty()) {
            mCardView.setVisibility(View.GONE);
            if (launch) {
                Toast.makeText(getContext(), R.string.toast_no_card, Toast.LENGTH_SHORT).show();
            }
            return;
        }
        mCardView.setVisibility(View.VISIBLE);
        mCardArrayAdapter.clear();
        mCardSpinner.setVisibility(View.VISIBLE);
        mCardStartDebugBtn.setEnabled(false);
        mCardList.addAll(mCardMap.keySet());
        mCardArrayAdapter.notifyDataSetChanged();

        String path;
        if (mCardList.size() == 1) {
            mCardSpinner.setSelection(1);
            path = mCardMap.get(mCardList.get(0));
        } else {
            path = PreferenceUtils.getDebugCardPath(getContext());
            int index = -1;
            for (int i = 0; i < mCardMap.size(); i++) {
                if (path.equals(mCardMap.get(mCardList.get(i)))) {
                    index = i;
                    break;
                }
            }
            if (index < 0) {
                path = "";
            }
            mCardSpinner.setSelection(index + 1);
        }
        PreferenceUtils.setDebugCardPath(getContext(), path);
        if (TextUtils.isEmpty(path)) {
            return;
        }
        mCardStartDebugBtn.setEnabled(true);
        if (launch) {
            if (isNeedDebug()) {
                DebuggerLogUtil.resetTraceId();
                DebuggerLogUtil.logMessage("DEBUGGER_TOUCHED_START");
                prepareDebugging();
            } else {
                CardDebugManager.getInstance(getContext()).launchCard(pkg, path);
                showCardToastIfNeeded();
            }
            mIsIdeDebug = false;
            mIsToolkitDebug = false;
        }
    }

    private boolean isNeedDebug() {
        return mIsIdeDebug || mIsToolkitDebug;
    }

    private void enableDebug(boolean enable) {
        mScanInstallBtn.setEnabled(enable);
        mLocalInstallBtn.setEnabled(enable);
        mUpdateOnlineBtn.setEnabled(enable);
        if (!enable) {
            mCardView.setVisibility(View.GONE);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        AppDebugManager.getInstance(getContext()).unbindDebugService();
        mHandler.removeMessages(MSG_UPDATE_CARD_HOSTS);
    }

    @Override
    public void startDebugging() {
        String pkg = PreferenceUtils.getDebugCardPackage(getContext());
        String path = PreferenceUtils.getDebugCardPath(getContext());
        String server = HttpUtils.getDebugServer(getActivity());
        if (TextUtils.isEmpty(server)) {
            Toast.makeText(getContext(), R.string.toast_no_server, Toast.LENGTH_SHORT).show();
        }
        CardDebugManager.getInstance(getContext()).startDebugging(pkg, path, server);

        showCardToastIfNeeded();
    }

    private void showCardToastIfNeeded() {
        CardHostInfo cardHostInfo = CardHostInfo.fromString(PreferenceUtils.getCardHostPlatform(getContext()));
        if (cardHostInfo != null && !TextUtils.equals(cardHostInfo.runtimeHost, cardHostInfo.archiveHost)) {
            try {
                PackageManager pm = getContext().getPackageManager();
                ApplicationInfo ai = pm.getApplicationInfo(cardHostInfo.archiveHost, 0);
                CharSequence hostName = pm.getApplicationLabel(ai);
                String tip = getContext().getString(R.string.toast_view_card_in, hostName);
                Toast.makeText(getContext(), tip, Toast.LENGTH_SHORT).show();
            } catch (PackageManager.NameNotFoundException e) {
                Log.w(TAG, "failed to get app info", e);
            }
        }
    }

    @Override
    protected List<Server.PlatformInfo> getAvailablePlatforms() {
        if (mCardHosts.isEmpty()) {
            long waitTime = mLastQueryTime == 0 ? GET_HOSTS_MAX_WAITING_TIME
                    : (GET_HOSTS_MAX_WAITING_TIME - (SystemClock.elapsedRealtime() - mLastQueryTime));
            if (waitTime > 0) {
                Log.i(TAG, "wait for card hosts for " + waitTime + "ms");
                try {
                    Thread.sleep(waitTime);
                } catch (InterruptedException e) {
                    Log.e(TAG, "interrupted while sleeping", e);
                }
            }
        }

        List<String> displayedCardHosts = new ArrayList<>(mDisplayedCardHosts);
        List<CardHostInfo> cardHosts = new ArrayList<>(mCardHosts);
        List<Server.PlatformInfo> platformInfos = new ArrayList<>();
        for (int i = 0; i < cardHosts.size() && i < displayedCardHosts.size(); ++i) {
            try {
                PackageInfo packageInfo = getActivity().getPackageManager()
                        .getPackageInfo(cardHosts.get(i).archiveHost, 0);
                long version = Build.VERSION.SDK_INT >= Build.VERSION_CODES.P
                        ? packageInfo.getLongVersionCode() : packageInfo.versionCode;
                platformInfos.add(new Server.PlatformInfo(cardHosts.get(i).archiveHost, displayedCardHosts.get(i), version));
            } catch (PackageManager.NameNotFoundException e) {
                Log.e(TAG, "failed to get package info for " + cardHosts.get(i).archiveHost, e);
            }
        }

        return platformInfos;
    }

    @Override
    protected boolean onSelectPlatform(String pkg) {
        for (CardHostInfo hostInfo : mCardHosts) {
            if (TextUtils.equals(hostInfo.archiveHost, pkg)) {
                setCardHost(hostInfo);
                return true;
            }
        }
        Log.w(TAG, "platform: " + pkg + " has lost");
        return false;
    }

    @Override
    protected void onUsbDebugDisable() {
        mUsbDebugSwitch.setChecked(false);
        PreferenceUtils.setUseADB(getActivity(), false);
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        if (parent == mCardHostSpinner) {
            if (position < 1) {
                return;
            }
            CardHostInfo cardHostInfo = mCardHosts.get(position - 1);
            setCardHost(cardHostInfo);
        } else if (parent == mCardSpinner) {
            if (position < 1) {
                return;
            }
            PreferenceUtils.setDebugCardPath(getContext(), mCardMap.get(mCardList.get(position - 1)));
            mCardStartDebugBtn.setEnabled(true);
        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {
        //ignore
    }
}
