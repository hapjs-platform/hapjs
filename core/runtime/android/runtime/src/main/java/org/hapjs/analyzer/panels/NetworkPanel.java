/*
 * Copyright (c) 2022, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.analyzer.panels;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Process;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextPaint;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetBehavior;

import org.hapjs.analyzer.views.EmptyRecyclerView;
import org.hapjs.analyzer.views.NetworkDetailView;
import org.hapjs.analyzer.views.WaterFallView;
import org.hapjs.common.json.JSONObject;
import org.hapjs.common.net.HttpConfig;
import org.hapjs.common.utils.DisplayUtil;
import org.hapjs.common.utils.ThreadUtils;
import org.hapjs.runtime.R;
import org.json.JSONException;

import java.lang.reflect.Method;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import javax.annotation.Nullable;

import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;

public class NetworkPanel extends CollapsedPanel {
    private static final String TAG = "NetworkPanel_log";
    public static final String NAME = "network";
    private static final String METHOD_REQUEST_WILL_BE_SENT = "Network.requestWillBeSent";
    private static final String METHOD_RESPONSE_RECEIVED = "Network.responseReceived";
    private static final String METHOD_GET_RESPONSE_BODY = "Network.getResponseBody";
    private static final String METHOD_DATA_RECEIVED = "Network.dataReceived";
    private static final String METHOD_LOADING_FINISHED = "Network.loadingFinished";
    private static final String METHOD_LOADING_FAILED = "Network.loadingFailed";
    private static final String METHOD_NETWORK_ENABLE = "Network.enable";
    private static final String PARAMS_KEY_TIMESTAMP = "timestamp";
    private static final String PARAMS_KEY_REQ_ID = "requestId";
    private static final String PARAMS_KEY_POST_DATA = "postData";
    private static final String PARAMS_KEY_METHOD = "method";
    private static final String PARAMS_KEY_RES = "response";
    private static final String PARAMS_KEY_REQ = "request";
    private static final String PARAMS_KEY_ID = "id";
    private static final String PARAMS_KEY_DOC_URL = "documentURL";
    private static final String PARAMS_KEY_URL = "url";
    private static final String PARAMS_KEY_TYPE = "type";
    private static final String PARAMS_KEY_STATUS = "status";
    private static final String PARAMS_KEY_STATUS_TEXT = "statusText";
    private static final String STATUS_FAIL_TEXT = "(failed)";
    private static final String PARAMS_KEY_ENCODED = "base64Encoded";
    private static final String PARAMS_KEY_RESULT = "result";
    private static final String PARAMS_KEY_BODY = "body";
    private static final String PARAMS_KEY_DATA_LEN = "encodedDataLength";
    private static final String PARAMS_KEY_PARAMS = "params";
    private static final String HTTP_REQ_METHOD_POST = "POST";
    private static final String EMPTY_STRING = "";
    public static final int NAME_COLOR_DEFAULT = 0xFFBFBFBF;
    public static final int NAME_COLOR_SELECTED = 0xFFFFFFFF;
    public static final int NAME_COLOR_FAIL = 0xFFFF0000;
    public static final int COLOR_RETRY_KILL_PROCESS = 0xFF5290EA;
    private static final int MAX_DATA_SIZE = 50;
    private okhttp3.WebSocket.Factory mWebSocketFactory;
    private WebSocket mWebSocket;
    private NetworkItemListAdapter mAdapter;
    private List<NetworkCacheInfo> mData;
    private Map<String, NetworkCacheInfo> mCacheMap;
    // mWaitForResMap: store those requestIds that need to wait for the responseBody to arrive after being removed from the mCacheMap
    private Map<String, NetworkCacheInfo> mWaitForResMap;
    private long sStartTime = -1;
    private long sLatestTime = -1;
    private NetworkDetailView mDetailView;
    private ViewGroup mDetailTitleContainer;
    private ViewGroup mOverviewTitleContainer;
    private EmptyRecyclerView mNetworkRecyclerView;
    private View mEmptyView;
    private TextView mRetryTextView;
    private ViewGroup mListDetailContainer;
    private int mSelectedPosition = -1;

    public NetworkPanel(Context context) {
        super(context, NAME, BOTTOM);
    }

    @Override
    protected int panelLayoutId() {
        return R.layout.layout_analyzer_network;
    }

    @Override
    protected void onCreateFinish() {
        super.onCreateFinish();
        mData = new ArrayList<>();
        mCacheMap = new HashMap<>();
        mWaitForResMap = new HashMap<>();
        View mBtnDetailBack = findViewById(R.id.network_detail_back_btn);
        mDetailView = findViewById(R.id.network_content_detail);
        mDetailTitleContainer = findViewById(R.id.network_detail_title_container);
        mOverviewTitleContainer = findViewById(R.id.network_overview_title_container);
        mEmptyView = findViewById(R.id.analyzer_log_empty_view);
        mListDetailContainer = findViewById(R.id.analyzer_network_list_detail_container);
        mNetworkRecyclerView = findViewById(R.id.analyzer_network_item_list);
        mBtnDetailBack.setOnClickListener(v -> {
            mDetailView.setVisibility(GONE);
            mAdapter.notifyDataSetChanged();
            mOverviewTitleContainer.setVisibility(VISIBLE);
            mDetailTitleContainer.setVisibility(GONE);
            ViewGroup.LayoutParams layoutParams = mNetworkRecyclerView.getLayoutParams();
            layoutParams.width = ViewGroup.LayoutParams.MATCH_PARENT;
            mNetworkRecyclerView.setLayoutParams(layoutParams);
            if (mSelectedPosition != -1) {
                mAdapter.notifyItemChanged(mSelectedPosition);
                mSelectedPosition = -1;
            }
        });
        mRetryTextView = findViewById(R.id.btn_network_retry_text);
        mRetryTextView.setMovementMethod(LinkMovementMethod.getInstance());
        String prefixString = getContext().getResources().getString(R.string.analyzer_net_init_fail_info);
        String clickableText = getContext().getResources().getString(R.string.analyzer_network_retry_kill_process);
        String suffixText = getContext().getResources().getString(R.string.analyzer_network_retry_reopen);
        String retryText = prefixString + clickableText + suffixText;
        SpannableString spannableString = new SpannableString(retryText);
        int startIndex = prefixString.length();
        int endIndex = prefixString.length() + clickableText.length();
        spannableString.setSpan(new ClickableSpan() {
            @Override
            public void updateDrawState(@NonNull TextPaint ds) {
                ds.setUnderlineText(false);
            }

            @Override
            public void onClick(@Nullable View widget) {
                mRetryTextView.setVisibility(GONE);
                Process.killProcess(Process.myPid());
            }
        }, startIndex, endIndex, 0);
        spannableString.setSpan(new ForegroundColorSpan(COLOR_RETRY_KILL_PROCESS), startIndex, endIndex, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        mRetryTextView.setText(spannableString, TextView.BufferType.SPANNABLE);
        if (!initWebSocket()) {
            showInitFailMessage();
        }
        setControlView(findViewById(R.id.btn_analyzer_network_ctl_line));
        configNetworkRecyclerView();
        addDragShieldView(Arrays.asList(mDetailView, mNetworkRecyclerView));
    }

    private void showInitFailMessage(){
        mListDetailContainer.setVisibility(GONE);
        mRetryTextView.setVisibility(VISIBLE);
        mEmptyView.setVisibility(GONE);
    }

    @Override
    void onShow() {
        super.onShow();
        mAdapter.notifyDataSetChanged();
    }

    @Override
    protected void onShowAnimationFinished() {
        super.onShowAnimationFinished();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mWebSocket != null) {
            mWebSocket.cancel();
            mWebSocket.close(3099, "The client actively shuts down");
            mWebSocket = null;
        }
    }

    public void reset() {
        mData.clear();
        mCacheMap.clear();
        sStartTime = -1;
        sLatestTime = -1;
        mAdapter.notifyDataSetChanged();
    }

    public synchronized boolean initWebSocket() {
        if (mWebSocket != null) {
            Log.d(TAG, "AnalyzerPanel_LOG initWebSocket but mWebSocket exist: ");
            return true;
        }
        String ws = "";
        try {
            Class<?> ReportInspectorInfoClass = Class.forName("org.hapjs.inspector.ReportInspectorInfo");
            Method getInstanceMethod = ReportInspectorInfoClass.getMethod("getInstance");
            Object ReportInspectorInfoInstance = getInstanceMethod.invoke(null);
            Method getWsMethod = ReportInspectorInfoClass.getMethod("getWsUrl");
            Object o = getWsMethod.invoke(ReportInspectorInfoInstance);
            if (o instanceof String) {
                ws = (String) o;
                ws = "ws://" + ws;
            }
            if (mWebSocketFactory == null) {
                mWebSocketFactory = HttpConfig.get().getOkHttpClient();
            }
            if (TextUtils.isEmpty(ws)) {
                Log.e(TAG, "AnalyzerPanel_LOG NetworkPanel initWebSocket fail because ws is empty");
                return false;
            }
            okhttp3.Request request = new okhttp3.Request.Builder().url(ws).build();
            mWebSocket = mWebSocketFactory.newWebSocket(request, new WebSocketListener() {
                @Override
                public void onOpen(WebSocket webSocket, Response response) {
                    super.onOpen(webSocket, response);
                    Log.i(TAG, "AnalyzerPanel_LOG NetworkPanel mWebSocket onOpen: ");
                    try {
                        if (mWebSocket != null) {
                            JSONObject jsonObject = new JSONObject();
                            jsonObject.put(PARAMS_KEY_METHOD, METHOD_NETWORK_ENABLE);
                            jsonObject.put(PARAMS_KEY_ID, 10086);
                            jsonObject.put(PARAMS_KEY_PARAMS, new JSONObject());
                            mWebSocket.send(jsonObject.toString());
                        }
                    } catch (JSONException e) {
                        Log.e(TAG, "AnalyzerPanel_LOG NetworkPanel error ", e);
                    }
                }

                @Override
                public void onMessage(WebSocket webSocket, String text) {
                    super.onMessage(webSocket, text);
                    try {
                        JSONObject jsonObject = new JSONObject(text);
                        if (jsonObject.has(PARAMS_KEY_METHOD) && jsonObject.has(PARAMS_KEY_PARAMS)) {
                            String method = jsonObject.getString(PARAMS_KEY_METHOD);
                            JSONObject paramsJson = jsonObject.getJSONObject(PARAMS_KEY_PARAMS);
                            if (paramsJson.has(PARAMS_KEY_REQ_ID)) {
                                String requestId = paramsJson.getString(PARAMS_KEY_REQ_ID);
                                switch (method) {
                                    case METHOD_REQUEST_WILL_BE_SENT:
                                        if (paramsJson.has(PARAMS_KEY_TIMESTAMP) && paramsJson.has(PARAMS_KEY_REQ)) {
                                            NetworkCacheInfo cacheInfo = new NetworkCacheInfo(requestId);
                                            long sendTime = (long) (paramsJson.getDouble(PARAMS_KEY_TIMESTAMP) * 1000);
                                            cacheInfo.setSentTime(sendTime);
                                            if (sStartTime < 0) {
                                                sStartTime = sendTime;
                                            }
                                            JSONObject requestJson = paramsJson.getJSONObject(PARAMS_KEY_REQ);
                                            if (requestJson.has(PARAMS_KEY_URL) && requestJson.has(PARAMS_KEY_METHOD)) {
                                                String url = requestJson.getString(PARAMS_KEY_URL);
                                                String reqMethod = requestJson.getString(PARAMS_KEY_METHOD);
                                                cacheInfo.setReqUrl(url);
                                                cacheInfo.setReqMethod(reqMethod);
                                                if (TextUtils.equals(reqMethod, HTTP_REQ_METHOD_POST) && requestJson.has(PARAMS_KEY_POST_DATA)) {
                                                    String postData = requestJson.getString(PARAMS_KEY_POST_DATA);
                                                    cacheInfo.setPostData(postData);
                                                }
                                            }
                                            String documentURL = paramsJson.getString(PARAMS_KEY_DOC_URL);
                                            if (!TextUtils.isEmpty(documentURL)) {
                                                Uri uri = Uri.parse(documentURL);
                                                String name = uri.getLastPathSegment();
                                                if (TextUtils.isEmpty(name) || TextUtils.isEmpty(name.trim())) {
                                                    name = uri.getHost();
                                                }
                                                cacheInfo.setName(name);
                                            }
                                            mCacheMap.put(requestId, cacheInfo);
                                        }
                                        break;
                                    case METHOD_RESPONSE_RECEIVED:
                                        if (paramsJson.has(PARAMS_KEY_TIMESTAMP) && mCacheMap.containsKey(requestId)
                                                && paramsJson.has(PARAMS_KEY_RES)
                                                && paramsJson.has(PARAMS_KEY_TYPE)) {
                                            NetworkCacheInfo cacheInfo = mCacheMap.get(requestId);
                                            cacheInfo.setResReceivedTime((long) (paramsJson.getDouble(PARAMS_KEY_TIMESTAMP) * 1000));
                                            JSONObject response = paramsJson.getJSONObject(PARAMS_KEY_RES);
                                            String originType = paramsJson.getString(PARAMS_KEY_TYPE);
                                            cacheInfo.setOriginType(originType);
                                            cacheInfo.setType(simplifyTypeName(originType));
                                            if (response.has(PARAMS_KEY_STATUS)) {
                                                String statusCode = response.getString(PARAMS_KEY_STATUS);
                                                if (Integer.parseInt(statusCode) >= 400) {
                                                    cacheInfo.setStatusError(true);
                                                }
                                                String status = statusCode;
                                                if (response.has(PARAMS_KEY_STATUS_TEXT)) {
                                                    String statusText = response.getString(PARAMS_KEY_STATUS_TEXT);
                                                    status = status + " " + statusText;
                                                }
                                                cacheInfo.setStatus(status);
                                            }
                                        } else {
                                            mCacheMap.remove(requestId);
                                        }
                                        break;
                                    case METHOD_DATA_RECEIVED:
                                        if (paramsJson.has(PARAMS_KEY_TIMESTAMP) && paramsJson.has(PARAMS_KEY_DATA_LEN) && mCacheMap.containsKey(requestId)) {
                                            int encodedDataLength = paramsJson.getInt(PARAMS_KEY_DATA_LEN);
                                            NetworkCacheInfo cacheInfo = mCacheMap.get(requestId);
                                            cacheInfo.setSize(cacheInfo.getSize() + encodedDataLength);
                                            cacheInfo.setDataReceivedTime((long) (paramsJson.getDouble(PARAMS_KEY_TIMESTAMP) * 1000));
                                        } else {
                                            mCacheMap.remove(requestId);
                                        }
                                        break;
                                    case METHOD_LOADING_FINISHED:
                                        if (paramsJson.has(PARAMS_KEY_TIMESTAMP) && mCacheMap.containsKey(requestId)) {
                                            NetworkCacheInfo cacheInfo = mCacheMap.remove(requestId);
                                            long timestamp = (long) (paramsJson.getDouble(PARAMS_KEY_TIMESTAMP) * 1000);
                                            cacheInfo.setEndTime(timestamp);
                                            if (timestamp > sLatestTime) {
                                                sLatestTime = timestamp;
                                            }
                                            if (mWebSocket != null) {
                                                JSONObject jo = new JSONObject();
                                                jo.put(PARAMS_KEY_METHOD, METHOD_GET_RESPONSE_BODY);
                                                jo.put(PARAMS_KEY_ID, Integer.parseInt(requestId));
                                                jo.put(PARAMS_KEY_PARAMS, new JSONObject().put(PARAMS_KEY_REQ_ID, requestId));
                                                mWebSocket.send(jo.toString());
                                                mWaitForResMap.put(requestId, cacheInfo);
                                            }
                                            cacheInfo.setSuccess(true);
                                            cacheInfo.generateTime();
                                            addData(cacheInfo);
                                        }
                                        break;
                                    case METHOD_LOADING_FAILED:
                                        if (paramsJson.has(PARAMS_KEY_TIMESTAMP) && mCacheMap.containsKey(requestId)) {
                                            NetworkCacheInfo cacheInfo = mCacheMap.remove(requestId);
                                            cacheInfo.setEndTime((long) (paramsJson.getDouble(PARAMS_KEY_TIMESTAMP) * 1000));
                                            cacheInfo.setSuccess(false);
                                            cacheInfo.generateTime();
                                            if (TextUtils.equals(cacheInfo.getStatus(), EMPTY_STRING)) {
                                                cacheInfo.setStatus(STATUS_FAIL_TEXT);
                                            }
                                            addData(cacheInfo);
                                        } else {
                                            mCacheMap.remove(requestId);
                                        }
                                        break;
                                    default:
                                        break;
                                }
                            }
                        } else if (jsonObject.has(PARAMS_KEY_ID) && jsonObject.has(PARAMS_KEY_RESULT)) {
                            JSONObject result = jsonObject.getJSONObject(PARAMS_KEY_RESULT);
                            int id = jsonObject.getInt(PARAMS_KEY_ID);
                            NetworkCacheInfo cacheInfo = mWaitForResMap.remove(String.valueOf(id));
                            if (cacheInfo != null) {
                                if (result.has(PARAMS_KEY_ENCODED)) {
                                    boolean base64Encoded = result.getBoolean(PARAMS_KEY_ENCODED);
                                    cacheInfo.setBase64Encoded(base64Encoded);
                                    if (!base64Encoded && result.has(PARAMS_KEY_BODY)) {
                                        String body = result.getString(PARAMS_KEY_BODY);
                                        cacheInfo.setResBody(body);
                                    }
                                }
                            }
                        }
                    } catch (JSONException e) {
                        Log.e(TAG, "AnalyzerPanel_LOG NetworkPanel onMessage parse fail: " , e);
                    }
                }

                @Override
                public void onClosing(WebSocket webSocket, int code, String reason) {
                    super.onClosing(webSocket, code, reason);
                    Log.d(TAG, "AnalyzerPanel_LOG NetworkPanel onClosing: " + code + ", " + reason);
                }

                @Override
                public void onClosed(WebSocket webSocket, int code, String reason) {
                    super.onClosed(webSocket, code, reason);
                    Log.d(TAG, "AnalyzerPanel_LOG NetworkPanel onClosed: " + code + ", " + reason);
                }

                @Override
                public void onFailure(WebSocket webSocket, Throwable t,@Nullable Response response) {
                    super.onFailure(webSocket, t, response);
                    Log.e(TAG, "AnalyzerPanel_LOG NetworkPanel onFailure: ", t);
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "AnalyzerPanel_LOG NetworkPanel initWebSocket error: ", e);
            mWebSocket = null;
            return false;
        }
        Log.d(TAG, "AnalyzerPanel_LOG NetworkPanel initWebSocket");
        return true;
    }

    private void addData(NetworkCacheInfo cacheInfo) {
        if(mData.size() >= MAX_DATA_SIZE){
            while(mData.size() >= MAX_DATA_SIZE){
                mData.remove(0);
            }
        }
        mData.add(cacheInfo);
        // Sort by sendTime
        Collections.sort(mData);
        if (mAdapter != null) {
            ThreadUtils.runOnUiThread(() -> mAdapter.notifyDataSetChanged());
        }
    }

    private void configNetworkRecyclerView() {
        GradientDrawable lineDrawable = new GradientDrawable();
        lineDrawable.setColor(Color.TRANSPARENT);
        lineDrawable.setSize(0, DisplayUtil.dip2Pixel(getContext(), 1));
        mNetworkRecyclerView.setItemAnimator(null);
        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
        mNetworkRecyclerView.setLayoutManager(layoutManager);
        NetworkItemListAdapter adapter = new NetworkItemListAdapter(mDetailView);
        mNetworkRecyclerView.setAdapter(adapter);
        mAdapter = adapter;
        mNetworkRecyclerView.setDataSizeChangedCallback(size -> {
            if (size <= 0 && mRetryTextView.getVisibility() != VISIBLE) {
                mEmptyView.setVisibility(VISIBLE);
            } else if (size > 0) {
                mEmptyView.setVisibility(GONE);
            }
        });
        setUpRecyclerView(mNetworkRecyclerView);
    }

    private class NetworkItemListAdapter extends RecyclerView.Adapter<NetWorkItemHolder> {
        private NetworkDetailView mDetailView;
        private static final int COLOR_SELECTED = 0xFF456FFF;
        private static final int COLOR_GRAY = 0xFF505050;
        private static final int COLOR_DARK = 0xFF333333;

        NetworkItemListAdapter(NetworkDetailView mDetailView) {
            this.mDetailView = mDetailView;
            mDetailView.setOnSlideToBottomListener(() -> {
                if (mBottomSheetBehavior.getState() == BottomSheetBehavior.STATE_COLLAPSED) {
                    mBottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
                }
            });
        }

        @NonNull
        @Override
        public NetWorkItemHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new NetWorkItemHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.layout_anayler_network_item, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull NetWorkItemHolder holder, int position) {
            NetworkCacheInfo networkItemData = mData.get(position);
            if (networkItemData != null) {
                holder.mType.setText(networkItemData.mType);
                TextPaint paint = holder.mName.getPaint();
                if (paint != null) {
                    if (mDetailView.getVisibility() == VISIBLE) {
                        paint.setFlags(paint.getFlags() & ~Paint.UNDERLINE_TEXT_FLAG);
                    } else {
                        paint.setFlags(Paint.UNDERLINE_TEXT_FLAG);
                    }
                    paint.setAntiAlias(true);
                }
                holder.mName.setText(networkItemData.mName);
                holder.mName.setTextColor((networkItemData.isSuccess() && !networkItemData.isStatusError()) ?
                        (mSelectedPosition == position ? NAME_COLOR_SELECTED : NAME_COLOR_DEFAULT) : NAME_COLOR_FAIL);
                holder.mSize.setText(networkItemData.mSizeStr);
                holder.mTime.setText(networkItemData.mTime);
                if (networkItemData.isSuccess() && sStartTime >= 0 && sLatestTime > 0) {
                    long sentTime = networkItemData.getSentTime();
                    long resReceivedTime = networkItemData.getResReceivedTime();
                    long endTime = networkItemData.getEndTime();
                    holder.mWaterFall.updateTime(sStartTime, sLatestTime, sentTime, resReceivedTime, endTime);
                } else {
                    holder.mWaterFall.disabled();
                }
                holder.mContainer.setOnClickListener(v -> {
                    if (mDetailView != null) {
                        networkItemData.setSequenceStartTime(sStartTime);
                        mDetailView.showDetail(networkItemData);
                        if (mDetailView.getVisibility() != View.VISIBLE) {
                            mDetailView.setVisibility(View.VISIBLE);
                            notifyDataSetChanged();
                        }
                        ViewGroup.LayoutParams layoutParams = mNetworkRecyclerView.getLayoutParams();
                        layoutParams.width = DisplayUtil.dip2Pixel(mContext, 92);
                        mNetworkRecyclerView.setLayoutParams(layoutParams);
                        mOverviewTitleContainer.setVisibility(GONE);
                        mDetailTitleContainer.setVisibility(VISIBLE);
                    }
                    if (mSelectedPosition != position) {
                        int oldPosition = mSelectedPosition;
                        mSelectedPosition = position;
                        notifyItemChanged(oldPosition);
                        notifyItemChanged(mSelectedPosition);
                    }
                });
            }
            if (mSelectedPosition == position) {
                holder.mContainer.setBackgroundColor(COLOR_SELECTED);
            } else {
                if (mDetailView.getVisibility() != VISIBLE && position % 2 == 1) {
                    holder.mContainer.setBackgroundColor(COLOR_DARK);
                } else {
                    holder.mContainer.setBackgroundColor(COLOR_GRAY);
                }
            }
        }

        @Override
        public int getItemCount() {
            return mData.size();
        }
    }

    private String simplifyTypeName(String originType) {
        String result;
        switch (originType) {
            case "Image":
                result = "Img";
                break;
            case "Document":
                result = "Doc";
                break;
            case "WebSocket":
                result = "WS";
                break;
            case "Stylesheet":
                result = "CSS";
                break;
            default:
                result = originType;
                break;
        }
        return result;
    }

    private class NetWorkItemHolder extends RecyclerView.ViewHolder {
        ViewGroup mContainer;
        TextView mType;
        TextView mName;
        TextView mSize;
        TextView mTime;
        WaterFallView mWaterFall;

        NetWorkItemHolder(@NonNull View itemView) {
            super(itemView);
            mContainer = itemView.findViewById(R.id.analyzer_network_item_container);
            mName = itemView.findViewById(R.id.analyzer_network_item_name);
            mSize = itemView.findViewById(R.id.analyzer_network_item_size);
            mTime = itemView.findViewById(R.id.analyzer_network_item_time);
            mWaterFall = itemView.findViewById(R.id.analyzer_network_item_waterfall);
            mType = itemView.findViewById(R.id.analyzer_network_item_type);
        }
    }

    public static class NetworkCacheInfo implements Comparable<NetworkCacheInfo>{
        static final String SIZE_GB_SUFFIX = "G";
        static final String SIZE_MB_SUFFIX = "M";
        static final String SIZE_KB_SUFFIX = "K";
        static final String SIZE_B_SUFFIX = "B";
        static final String TIME_SECOND_SUFFIX = "s";
        static final String TIME_MILLIS_SUFFIX = "ms";
        public static final String STRING_UNKNOWN = "unknown";
        private static final long SIZE_GB = 1024 * 1024 * 1024;
        private static final long SIZE_MB = 1024 * 1024;
        private static final long SIZE_KB = 1024;
        private static final long TIME_SECOND = 1000;
        private static DecimalFormat sDecimalOneFormat = new DecimalFormat("0.0");
        private static DecimalFormat sDecimalZeroFormat = new DecimalFormat("0");
        private String mId;
        private String mType = EMPTY_STRING;
        private String mOriginType = EMPTY_STRING;
        private String mStatus = EMPTY_STRING;
        private String mName = STRING_UNKNOWN;
        private int mSize;
        private String mSizeStr = "0B";
        private long mSequenceStartTime = -1; // The earliest request time in the same sequence
        private long mSentTime;
        private long mResReceivedTime;
        private long mDataReceivedTime;
        private long mEndTime;
        private String mTime;
        private String mTimeStalled;
        private String mTimeDownload;
        private boolean mSuccess;
        private boolean mStatusCodeError;
        private String mReqUrl;
        private String mReqMethod;
        private String mPostData = EMPTY_STRING;
        private boolean mBase64Encoded;
        private String mResBody;

        NetworkCacheInfo(String mId) {
            this.mId = mId;
        }

        public String getTime() {
            return mTime;
        }

        public void setTime(String mTime) {
            this.mTime = mTime;
        }

        public String getTimeStalled() {
            return mTimeStalled;
        }

        void setSequenceStartTime(long sequenceStartTime) {
            this.mSequenceStartTime = sequenceStartTime;
        }

        public long getSequenceStartTime() {
            return mSequenceStartTime;
        }

        public String getTimeDownload() {
            return mTimeDownload;
        }

        public String getId() {
            return mId;
        }

        public String getType() {
            return mType;
        }

        public void setType(String mType) {
            this.mType = mType;
        }

        public String getOriginType() {
            return mOriginType;
        }

        public void setOriginType(String originType) {
            this.mOriginType = originType;
        }

        public String getStatus() {
            return mStatus;
        }

        public void setStatus(String mStatus) {
            this.mStatus = mStatus;
        }

        public String getName() {
            return mName;
        }

        public void setName(String name) {
            if (!TextUtils.isEmpty(name) && !TextUtils.isEmpty(name.trim())) {
                this.mName = name;
            }
        }

        public int getSize() {
            return mSize;
        }

        public void setSize(int mSize) {
            this.mSize = mSize;
            this.mSizeStr = byteToString(this.mSize);
        }

        public long getSentTime() {
            return mSentTime;
        }

        void setSentTime(long mSentTime) {
            this.mSentTime = mSentTime;
        }

        long getResReceivedTime() {
            return mResReceivedTime;
        }

        void setResReceivedTime(long mResReceivedTime) {
            this.mResReceivedTime = mResReceivedTime;
        }

        void setDataReceivedTime(long mDataReceivedTime) {
            this.mDataReceivedTime = mDataReceivedTime;
        }

        long getEndTime() {
            return mEndTime;
        }

        void setEndTime(long mEndTime) {
            this.mEndTime = mEndTime;
        }

        public boolean isSuccess() {
            return mSuccess;
        }

        public void setSuccess(boolean mSuccess) {
            this.mSuccess = mSuccess;
        }

        public boolean isStatusError() {
            return mStatusCodeError;
        }

        public void setStatusError(boolean b) {
            this.mStatusCodeError = b;
        }

        void generateTime() {
            if (mEndTime > mSentTime && mSentTime > 0) {
                mTime = timeToString(mEndTime - mSentTime);
                mTimeStalled = mResReceivedTime >= mSentTime ? timeToString(mResReceivedTime - mSentTime) : STRING_UNKNOWN;
                if (mEndTime >= mDataReceivedTime && mDataReceivedTime >= mResReceivedTime && mResReceivedTime >= mSentTime) {
                    mTimeDownload = timeToString(mEndTime - mResReceivedTime);
                } else {
                    mTimeDownload = STRING_UNKNOWN;
                }
            } else {
                mTime = STRING_UNKNOWN;
                mTimeStalled = STRING_UNKNOWN;
                mTimeDownload = STRING_UNKNOWN;
            }
        }

        public String getReqUrl() {
            return mReqUrl;
        }

        void setReqUrl(String reqUrl) {
            this.mReqUrl = reqUrl;
        }

        public String getReqMethod() {
            return mReqMethod;
        }

        void setReqMethod(String reqMethod) {
            this.mReqMethod = reqMethod;
        }

        public String getPostData() {
            return mPostData;
        }

        void setPostData(String postData) {
            this.mPostData = postData;
        }

        public boolean isBase64Encoded() {
            return mBase64Encoded;
        }

        void setBase64Encoded(boolean base64Encoded) {
            this.mBase64Encoded = base64Encoded;
        }

        public String getResBody() {
            return mResBody;
        }

        void setResBody(String resBody) {
            this.mResBody = resBody;
        }

        @Override
        public int compareTo(NetworkCacheInfo obj) {
            if (Objects.equals(this, obj)) {
                return 0;
            }
            if (mSentTime == obj.getSentTime()) {
                return mId.compareTo(obj.mId);
            } else {
                return (int) (mSentTime - obj.getSentTime());
            }
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            NetworkCacheInfo cacheInfo = (NetworkCacheInfo) o;
            return mSentTime == cacheInfo.mSentTime &&
                    Objects.equals(mId, cacheInfo.mId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(mId, mSentTime);
        }

        private static String byteToString(long size) {
            String resultSize;
            DecimalFormat decimalFormat = sDecimalOneFormat;
            if (size / SIZE_GB >= 1) {
                if (size / (float) SIZE_GB >= 100) {
                    decimalFormat = sDecimalZeroFormat;
                }
                resultSize = decimalFormat.format(size / (float) SIZE_GB) + SIZE_GB_SUFFIX;
            } else if (size / SIZE_MB >= 1) {
                if (size / (float) SIZE_MB >= 100) {
                    decimalFormat = sDecimalZeroFormat;
                }
                resultSize = decimalFormat.format(size / (float) SIZE_MB) + SIZE_MB_SUFFIX;
            } else if (size / SIZE_KB >= 1) {
                if (size / (float) SIZE_KB >= 100) {
                    decimalFormat = sDecimalZeroFormat;
                }
                resultSize = decimalFormat.format(size / (float) SIZE_KB) + SIZE_KB_SUFFIX;
            } else {
                resultSize = size + SIZE_B_SUFFIX;
            }
            return resultSize;
        }

        public static String timeToString(long time) {
            String resultSize;
            DecimalFormat decimalFormat = sDecimalOneFormat;
            if (time / TIME_SECOND >= 1) {
                if (time / (float) TIME_SECOND >= 100) {
                    decimalFormat = sDecimalZeroFormat;
                }
                resultSize = decimalFormat.format(time / (float) TIME_SECOND) + TIME_SECOND_SUFFIX;
            } else {
                resultSize = time + TIME_MILLIS_SUFFIX;
            }
            return resultSize;
        }
    }
}
