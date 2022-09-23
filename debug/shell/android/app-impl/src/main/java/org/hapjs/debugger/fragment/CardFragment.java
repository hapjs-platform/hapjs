/*
 * Copyright (c) 2021-2022, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.debugger.fragment;

import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListPopupWindow;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.vectordrawable.graphics.drawable.AnimatedVectorDrawableCompat;

import org.hapjs.debug.log.DebuggerLogUtil;
import org.hapjs.debugger.app.impl.R;
import org.hapjs.debugger.debug.AppDebugManager;
import org.hapjs.debugger.debug.CardDebugManager;
import org.hapjs.debugger.debug.CardHostInfo;
import org.hapjs.debugger.server.Server;
import org.hapjs.debugger.utils.HttpUtils;
import org.hapjs.debugger.utils.PreferenceUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class CardFragment extends DebugFragment implements AdapterView.OnItemClickListener {

    private static final String TAG = "CardFragment";

    private static final int MSG_UPDATE_CARD_HOSTS = 0;
    private static final long CARD_HOST_UPDATE_DELAYED_TIME = 3000;
    private static final long GET_HOSTS_MAX_WAITING_TIME = 3000;

    private static final String KEY_CARD_PATH = "cardPath";

    private ImageView mLoadHostProgress;
    private View mHostInfoLayout;
    private TextView mPlatformPkgTv;
    private TextView mPlatformVersionTv;
    private TextView mPlatformVersionNameTv;
    private TextView mCardHostNameTv;
    private TextView mCardHostPkgTv;
    private View mNoHostView;

    private ImageView mHostSpinnerIcon;
    private ListPopupWindow mHostSpinner;

    private View mDebugPkgInfoLayout;
    private View mNoDebugPkgView;
    private TextView mDebugCardNameTv;
    private TextView mDebugPkgTv;
    private ImageView mCardSpinnerIcon;
    private ListPopupWindow mCardSpinner;

    private List<String> mDisplayedCardHosts = new ArrayList<>();
    private List<String> mCardHostNames = new ArrayList<>();
    private List<CardHostInfo> mCardHosts = new ArrayList<>();
    private List<String> mCardList = new ArrayList<>();
    private Map<String, String> mCardMap;
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

    @Override
    protected int getLayoutResources() {
        return R.layout.content_card;
    }

    @Override
    protected View setupViews(LayoutInflater inflater, @Nullable ViewGroup container) {
        View view = super.setupViews(inflater, container);

        mLoadHostProgress = view.findViewById(R.id.load_host_progress);
        AnimatedVectorDrawableCompat hostProgress = AnimatedVectorDrawableCompat.create(
                getActivity(), R.drawable.load_card_host_progress);
        mLoadHostProgress.setImageDrawable(hostProgress);
        hostProgress.start();

        mHostInfoLayout = view.findViewById(R.id.host_info_layout);
        mCardHostNameTv = view.findViewById(R.id.host_name);
        mCardHostPkgTv = view.findViewById(R.id.host_pkg);
        mPlatformPkgTv = view.findViewById(R.id.platform_pkg_text);
        mPlatformVersionTv = view.findViewById(R.id.platform_version_text);
        mPlatformVersionNameTv = view.findViewById(R.id.platform_version_name_text);
        mNoHostView = view.findViewById(R.id.no_host_text);

        mHostSpinnerIcon = view.findViewById(R.id.host_spinner_icon);
        mHostSpinnerIcon.setVisibility(View.GONE);
        mHostSpinnerIcon.setOnClickListener(v ->
                mHostSpinner = handleSpinnerIconClick(mHostSpinner, mHostSpinnerIcon,
                        R.id.host_info_primary_layout, mDisplayedCardHosts, getSelectedHostIndex(),
                        R.drawable.arrow_down, R.drawable.arrow_up,
                        (int) getResources().getDimension(R.dimen.platform_popup_width),
                        ListPopupWindow.WRAP_CONTENT,
                        0));

        mDebugPkgInfoLayout = view.findViewById(R.id.debug_pkg_info_layout);
        mNoDebugPkgView = view.findViewById(R.id.txt_no_debuggable_pkg);
        mDebugCardNameTv = view.findViewById(R.id.debug_card);
        mDebugPkgTv = view.findViewById(R.id.debug_pkg_name);
        mCardSpinnerIcon = view.findViewById(R.id.card_spinner_icon);
        mCardSpinnerIcon.setVisibility(View.GONE);
        mCardSpinnerIcon.setOnClickListener(v ->
                mCardSpinner = handleSpinnerIconClick(mCardSpinner, mCardSpinnerIcon,
                        R.id.start_debug_pkg_layout, mCardList, getSelectedCardIndex(),
                        R.drawable.card_arrow_down, R.drawable.card_arrow_up,
                        (int) getResources().getDimension(R.dimen.card_name_popup_width),
                        mCardList.size() <= 4 ? ListPopupWindow.WRAP_CONTENT
                                : (int) getResources().getDimension(R.dimen.card_name_popup_max_height),
                        (int) getResources().getDimension(R.dimen.card_name_popup_vertical_offset)));

        refreshButtons(false);

        mHandler.sendMessageDelayed(Message.obtain(mHandler, MSG_UPDATE_CARD_HOSTS, mCardHosts), 2000);

        return view;
    }

    private int getSelectedHostIndex() {
        return mCardHosts.indexOf(mCardHost);
    }

    private int getSelectedCardIndex() {
        String path = PreferenceUtils.getDebugCardPath(getContext());
        for (int i = 0; i < mCardMap.size(); i++) {
            if (path.equals(mCardMap.get(mCardList.get(i)))) {
                return i;
            }
        }
        return -1;
    }

    protected void setupDebug() {
        AppDebugManager.getInstance(getContext()).setDebugListener(new AppDebugListener() {
            @Override
            protected void onInstallSuccess(final String pkg) {
                resetButtonTextView();
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        setDebugPackageView(pkg, true);
                    }
                });
            }

            @Override
            public void onError(int code) {
                resetButtonTextView();
                super.onError(code);
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
    protected String getDebugHintText() {
        CharSequence hostName = mCardHostNameTv.getText();
        if (TextUtils.isEmpty(hostName)) {
            return null;
        }
        return getContext().getString(R.string.hint_no_response, hostName);
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
            return;
        }

        mDisplayedCardHosts = new ArrayList<>();
        for (CardHostInfo cardHostInfo : mCardHosts) {
            final PackageManager pm = getContext().getApplicationContext().getPackageManager();
            ApplicationInfo ai;
            try {
                ai = pm.getApplicationInfo(cardHostInfo.archiveHost, 0);
            } catch (final PackageManager.NameNotFoundException e) {
                ai = null;
            }
            String applicationName = (String) (ai != null ? pm.getApplicationLabel(ai) : "unknown: ");
            mCardHostNames.add(applicationName);
            String displayCardHost = applicationName + " (" + cardHostInfo.archiveHost + ") ";
            mDisplayedCardHosts.add(displayCardHost);
        }

        if (mCardHosts.size() == 1) {
            //if there is only one platform, we choose it and don't show the Spinner
            CardHostInfo cardHostInfo = mCardHosts.get(0);
            setCardHost(cardHostInfo);
            return;
        } else {
            mHostSpinnerIcon.setVisibility(View.VISIBLE);
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
        mLoadHostProgress.setVisibility(View.GONE);
        if (cardHost == null) {
            refreshButtons(false);
            mHostInfoLayout.setVisibility(View.GONE);
            mNoHostView.setVisibility(View.VISIBLE);
            AlphaAnimation animation = new AlphaAnimation(0.0f, 1.0f);
            animation.setDuration(500);
            mNoHostView.startAnimation(animation);
        } else if (!cardHost.equals(mCardHost)) {
            mCardHost = cardHost;
            PreferenceUtils.setCardHostPlatform(getContext(), cardHost.toString());

            mHostInfoLayout.setVisibility(View.VISIBLE);
            mNoHostView.setVisibility(View.GONE);
            mCardHostNameTv.setText(mCardHostNames.get(mCardHosts.indexOf(cardHost)));
            mCardHostPkgTv.setText(" (" + cardHost.archiveHost + ")");
            mHostSpinnerIcon.setVisibility(mCardHosts.size() == 1 ? View.GONE : View.VISIBLE);
            AlphaAnimation animation = new AlphaAnimation(0.0f, 1.0f);
            animation.setDuration(500);
            mHostInfoLayout.startAnimation(animation);
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
            mPlatformPkgTv.setVisibility(View.GONE);
            mPlatformVersionTv.setVisibility(View.GONE);
            mPlatformVersionNameTv.setVisibility(View.GONE);
            refreshButtons(false);
            return;
        }
        try {
            mPlatformPkgTv.setVisibility(View.VISIBLE);
            mPlatformPkgTv.setText(getContext().getString(R.string.text_framework, corePlatform));
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
            refreshButtons(true);
            handleIDERequest(getActivity().getIntent());
            AppDebugManager.setCurrentPlatformVersion(version);
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, "Platform not found: ", e);
            mPlatformVersionNameTv.setVisibility(View.GONE);
            mPlatformVersionTv.setVisibility(View.GONE);
            mPlatformPkgTv.setVisibility(View.GONE);
            refreshButtons(false);
        }
    }

    private void setDebugPackageView(String pkg, boolean launch) {
        mCardMap = null;
        if (!TextUtils.isEmpty(pkg)) {
            org.hapjs.debugger.pm.PackageInfo packageInfo = CardDebugManager.getInstance(getContext()).getPackageInfo(pkg);
            mCardMap = packageInfo != null ? packageInfo.getCardInfos() : null;
        }

        if (mCardMap == null || mCardMap.isEmpty()) {
            mDebugPkgInfoLayout.setVisibility(View.GONE);
            mNoDebugPkgView.setVisibility(View.VISIBLE);
            if (launch) {
                Toast.makeText(getContext(), R.string.toast_no_card, Toast.LENGTH_SHORT).show();
            }
            return;
        }

        mCardList.clear();
        mCardList.addAll(mCardMap.keySet());

        int index = getSelectedCardIndex();
        String path = mCardMap.get(mCardList.get(index < 0 ? 0 : index));
        PreferenceUtils.setDebugCardPackage(getContext(), pkg);
        PreferenceUtils.setDebugCardPath(getContext(), path);

        mDebugPkgInfoLayout.setVisibility(View.VISIBLE);
        mNoDebugPkgView.setVisibility(View.GONE);
        mDebugPkgTv.setText(pkg);
        setDebugCardName(mCardList.get(index < 0 ? 0 : index));
        mCardSpinnerIcon.setVisibility(mCardList.size() > 1 ? View.VISIBLE : View.GONE);

        mStartDebugBtn.setEnabled(true);

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

    private void setDebugCardName(String cardName) {
        mDebugCardNameTv.setText(cardName);
    }

    private boolean isNeedDebug() {
        return mIsIdeDebug || mIsToolkitDebug;
    }

    private void refreshButtons(boolean hasSelectedPlatform) {
        mScanInstallBtn.setEnabled(hasSelectedPlatform);
        mLocalInstallBtn.setEnabled(hasSelectedPlatform);
        mUpdateOnlineBtn.setEnabled(hasSelectedPlatform);
        refreshStartDebugButton(hasSelectedPlatform);
    }

    private void refreshStartDebugButton(boolean hasSelectedPlatform) {
        mStartDebugBtn.setEnabled(hasSelectedPlatform
                && !TextUtils.isEmpty(PreferenceUtils.getDebugCardPackage(getActivity()))
                && !TextUtils.isEmpty(PreferenceUtils.getDebugCardPath(getActivity())));
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

        List<String> displayedCardHosts = new ArrayList<>(mCardHostNames);
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
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        if (mHostSpinner != null && parent == mHostSpinner.getListView()) {
            CardHostInfo cardHostInfo = mCardHosts.get(position);
            setCardHost(cardHostInfo);
            mHostSpinner.dismiss();
        } else if (mCardSpinner != null && parent == mCardSpinner.getListView()) {
            String cardName = mCardList.get(position);
            PreferenceUtils.setDebugCardPath(getContext(), mCardMap.get(cardName));
            setDebugCardName(cardName);
            mCardSpinner.dismiss();
        }
    }
}
