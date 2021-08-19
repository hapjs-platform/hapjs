/*
 * Copyright (c) 2022, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.hapjs.analyzer.views;

import android.content.Context;
import android.net.Uri;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.facebook.binaryresource.FileBinaryResource;
import com.facebook.cache.common.SimpleCacheKey;
import com.facebook.drawee.backends.pipeline.Fresco;
import com.facebook.drawee.drawable.ScalingUtils;
import com.facebook.drawee.generic.GenericDraweeHierarchy;
import com.facebook.drawee.view.SimpleDraweeView;

import org.hapjs.analyzer.panels.NetworkPanel;
import org.hapjs.runtime.R;

import java.io.File;

public class NetworkDetailView extends LinearLayout implements SlideDetectable, View.OnClickListener{
    private static final String PREFIX_METHOD = "- Request Method: ";
    private static final String PREFIX_POST_DATA = "- Post data: ";
    private static final String PREFIX_URL = "- Request URL: ";
    private static final String PREFIX_STATUS = "- Status Code: ";
    private static final String RESPONSE_NOT_AVAILABLE = "This request has no response data available.";
    private static final String RESPONSE_LOAD_FAIL = "Fail to load response data.";
    private static final String EMPTY_STRING = "";
    private static final String METHOD_POST = "POST";
    private static final String ORIGIN_TYPE_IMAGE = "Image";
    private static final String TEXT_UNKNOWN = "unknown";
    private String mCurrentId = String.valueOf(Integer.MAX_VALUE);
    private SlideMonitoredScrollView mScroll;
    private ExpandTextView mReqUrl;
    private TextView mReqMethod;
    private ExpandTextView mReqPostData;
    private TextView mStatusCode;
    private TextView mTimingStart;
    private TextView mTimingTotal;
    private TextView mTimingStalled;
    private TextView mTimingDownload;
    private ExpandTextView mResponseContent;
    private TextView mResponseWarn;
    private TextView mNoPreviewText;
    private SimpleDraweeView mPreviewImage;
    private ViewGroup mGeneralContainer;
    private ViewGroup mTimingContainer;
    private ViewGroup mResponseContainer;
    private ViewGroup mPreviewContainer;
    private View mGeneralMarker;
    private View mTimingMarker;
    private View mResponseMarker;
    private View mPreviewMarker;
    private View mGeneralExpandBtn;
    private View mTimingExpandBtn;
    private View mResponseExpandBtn;
    private View mPreviewExpandBtn;

    public NetworkDetailView(Context context) {
        super(context);
        init();
    }

    public NetworkDetailView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public NetworkDetailView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        View mDetail = inflate(getContext(), R.layout.layout_analyzer_network_detail, this);
        mScroll = mDetail.findViewById(R.id.network_content_detail_scroll);
        mReqUrl = mDetail.findViewById(R.id.network_content_detail_req_url);
        mReqMethod = mDetail.findViewById(R.id.network_content_detail_req_method);
        mReqPostData = mDetail.findViewById(R.id.network_content_detail_req_post_data);
        mStatusCode = mDetail.findViewById(R.id.network_content_detail_status);
        mTimingStart = mDetail.findViewById(R.id.network_detail_timing_start);
        mTimingTotal = mDetail.findViewById(R.id.network_detail_timing_total);
        mTimingDownload = mDetail.findViewById(R.id.network_detail_timing_download);
        mTimingStalled = mDetail.findViewById(R.id.network_detail_timing_stalled);
        mResponseContent = mDetail.findViewById(R.id.network_content_detail_res_content);
        mResponseWarn = mDetail.findViewById(R.id.network_content_detail_res_content_warn);
        mNoPreviewText = mDetail.findViewById(R.id.network_content_detail_no_preview_text);
        mPreviewImage = mDetail.findViewById(R.id.network_content_detail_preview_img);
        mGeneralContainer = mDetail.findViewById(R.id.analyzer_net_detail_general_container);
        mTimingContainer = mDetail.findViewById(R.id.analyzer_net_detail_timing_container);
        mResponseContainer = mDetail.findViewById(R.id.analyzer_net_detail_response_container);
        mPreviewContainer = mDetail.findViewById(R.id.analyzer_net_detail_preview_container);
        mGeneralMarker = mDetail.findViewById(R.id.analyzer_net_detail_general_marker);
        mTimingMarker = mDetail.findViewById(R.id.analyzer_net_detail_timing_marker);
        mResponseMarker = mDetail.findViewById(R.id.analyzer_net_detail_response_marker);
        mPreviewMarker = mDetail.findViewById(R.id.analyzer_net_detail_preview_marker);
        mGeneralExpandBtn = mDetail.findViewById(R.id.analyzer_net_detail_general_expand);
        mTimingExpandBtn = mDetail.findViewById(R.id.analyzer_net_detail_timing_expand);
        mResponseExpandBtn = mDetail.findViewById(R.id.analyzer_net_detail_response_expand);
        mPreviewExpandBtn = mDetail.findViewById(R.id.analyzer_net_detail_preview_expand);
        View mGeneralTitleRow = mDetail.findViewById(R.id.analyzer_net_detail_general_row);
        View mTimingTitleRow = mDetail.findViewById(R.id.analyzer_net_detail_timing_row);
        View mResponseTitleRow = mDetail.findViewById(R.id.analyzer_net_detail_response_row);
        View mPreviewTitleRow = mDetail.findViewById(R.id.analyzer_net_detail_preview_row);
        mGeneralTitleRow.setOnClickListener(this);
        mTimingTitleRow.setOnClickListener(this);
        mResponseTitleRow.setOnClickListener(this);
        mPreviewTitleRow.setOnClickListener(this);
    }

    public void showDetail(NetworkPanel.NetworkCacheInfo data) {
        if (data == null || TextUtils.equals(mCurrentId, data.getId())) {
            return;
        }
        // reset
        mGeneralContainer.setVisibility(GONE);
        mResponseContainer.setVisibility(GONE);
        mPreviewContainer.setVisibility(GONE);
        mGeneralMarker.setSelected(false);
        mResponseMarker.setSelected(false);
        mPreviewMarker.setSelected(false);
        mGeneralExpandBtn.setSelected(false);
        mResponseExpandBtn.setSelected(false);
        mPreviewExpandBtn.setSelected(false);
        mTimingStart.setText(EMPTY_STRING);
        mTimingStalled.setText(EMPTY_STRING);
        mTimingDownload.setText(EMPTY_STRING);
        mTimingTotal.setText(EMPTY_STRING);
        // show general
        String statusCode = data.getStatus();
        String url = data.getReqUrl();
        String status = PREFIX_STATUS + statusCode;
        if (!TextUtils.isEmpty(url)) {
            mGeneralExpandBtn.setSelected(true);
            mGeneralMarker.setSelected(true);
            mGeneralContainer.setVisibility(VISIBLE);
        }
        String reqUrl = PREFIX_URL + url;
        String reqMethod = data.getReqMethod();
        String methodText = PREFIX_METHOD + reqMethod;
        boolean base64Encoded = data.isBase64Encoded();
        mReqUrl.setRenderText(reqUrl, false);
        mReqMethod.setText(methodText);
        if (TextUtils.isEmpty(statusCode)) {
            mStatusCode.setVisibility(GONE);
        } else {
            mStatusCode.setText(status);
            mStatusCode.setVisibility(VISIBLE);
        }
        String postData = data.getPostData();
        if (TextUtils.equals(reqMethod, METHOD_POST) && !TextUtils.isEmpty(postData)) {
            mReqPostData.setRenderText(PREFIX_POST_DATA + postData, false);
            mReqPostData.setVisibility(VISIBLE);
        } else {
            mReqPostData.setVisibility(GONE);
        }
        // show timing
        if (data.getSequenceStartTime() >= 0 && data.getSentTime() >= data.getSequenceStartTime()) {
            mTimingStart.setText(NetworkPanel.NetworkCacheInfo.timeToString(data.getSentTime() - data.getSequenceStartTime()));
        } else {
            mTimingStart.setText(NetworkPanel.NetworkCacheInfo.STRING_UNKNOWN);
        }
        mTimingStalled.setText(data.getTimeStalled());
        mTimingDownload.setText(data.getTimeDownload());
        mTimingTotal.setText(data.getTime());
        mTimingExpandBtn.setSelected(true);
        mTimingMarker.setSelected(true);
        mTimingContainer.setVisibility(VISIBLE);
        // show response
        mResponseWarn.setVisibility(GONE);
        if (!base64Encoded && data.isSuccess()) {
            String resBody = data.getResBody();
            if (!TextUtils.isEmpty(resBody)) {
                if (resBody.length() > ExpandTextView.MAX_AVAILABLE_TEXT_LEN) {
                    resBody = resBody.substring(0, ExpandTextView.MAX_AVAILABLE_TEXT_LEN);
                    mResponseWarn.setVisibility(VISIBLE);
                }
                mResponseContent.setRenderText(resBody, false);
                mResponseMarker.setSelected(true);
            } else {
                mResponseContent.setRenderText(data.isSuccess() ? RESPONSE_NOT_AVAILABLE : RESPONSE_LOAD_FAIL, false);
            }
            // response is not expanded by default
        } else {
            mResponseContent.setRenderText(data.isSuccess() ? RESPONSE_NOT_AVAILABLE : RESPONSE_LOAD_FAIL, false);
        }
        // show preview
        mPreviewImage.setImageURI("");
        mPreviewImage.setVisibility(GONE);
        mNoPreviewText.setVisibility(VISIBLE);
        if (data.isSuccess() && TextUtils.equals(data.getOriginType(), ORIGIN_TYPE_IMAGE)) {
            // load picture from cache
            FileBinaryResource resource = (FileBinaryResource) Fresco.getImagePipelineFactory().getMainFileCache().getResource(new SimpleCacheKey(data.getReqUrl()));
            if (resource != null) {
                File file = resource.getFile();
                mPreviewImage.setImageURI(Uri.fromFile(file));
                GenericDraweeHierarchy hierarchy = mPreviewImage.getHierarchy();
                hierarchy.setActualImageScaleType(ScalingUtils.ScaleType.FIT_START);
                mPreviewImage.setVisibility(VISIBLE);
                mNoPreviewText.setVisibility(GONE);
                mPreviewExpandBtn.setSelected(true);
                mPreviewMarker.setSelected(true);
                mPreviewContainer.setVisibility(VISIBLE);
            }
        }
        mScroll.fullScroll(ScrollView.FOCUS_UP);
        mCurrentId = data.getId();
    }

    public void setOnSlideToBottomListener(OnSlideToBottomListener onCloseListener) {
        this.mScroll.setOnSlideToBottomListener(onCloseListener);
    }

    @Override
    public void onSlideToBottom() {

    }

    @Override
    public boolean isSlideToBottom() {
        return false;
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.analyzer_net_detail_general_row) {
            if (mGeneralExpandBtn.isSelected() && mGeneralContainer.getVisibility() == VISIBLE) {
                mGeneralContainer.setVisibility(GONE);
                mGeneralExpandBtn.setSelected(false);
            } else if (!mGeneralExpandBtn.isSelected() && mGeneralContainer.getVisibility() != VISIBLE) {
                mGeneralContainer.setVisibility(VISIBLE);
                mGeneralExpandBtn.setSelected(true);
            }
        } else if (id == R.id.analyzer_net_detail_timing_row) {
            if (mTimingExpandBtn.isSelected() && mTimingContainer.getVisibility() == VISIBLE) {
                mTimingContainer.setVisibility(GONE);
                mTimingExpandBtn.setSelected(false);
            } else if (!mTimingExpandBtn.isSelected() && mTimingContainer.getVisibility() != VISIBLE) {
                mTimingContainer.setVisibility(VISIBLE);
                mTimingExpandBtn.setSelected(true);
            }
        }
        else if (id == R.id.analyzer_net_detail_response_row) {
            if (mResponseExpandBtn.isSelected() && mResponseContainer.getVisibility() == VISIBLE) {
                mResponseContainer.setVisibility(GONE);
                mResponseExpandBtn.setSelected(false);
            } else if (!mResponseExpandBtn.isSelected() && mResponseContainer.getVisibility() != VISIBLE) {
                mResponseContainer.setVisibility(VISIBLE);
                mResponseExpandBtn.setSelected(true);
            }
        } else if (id == R.id.analyzer_net_detail_preview_row) {
            if (mPreviewExpandBtn.isSelected() && mPreviewContainer.getVisibility() == VISIBLE) {
                mPreviewContainer.setVisibility(GONE);
                mPreviewExpandBtn.setSelected(false);
            } else if (!mPreviewExpandBtn.isSelected() && mPreviewContainer.getVisibility() != VISIBLE) {
                mPreviewContainer.setVisibility(VISIBLE);
                mPreviewExpandBtn.setSelected(true);
            }
        }
    }
}
