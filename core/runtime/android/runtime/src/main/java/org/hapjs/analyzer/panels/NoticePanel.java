/*
 * Copyright (c) 2022, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.analyzer.panels;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.hapjs.analyzer.AnalyzerStatisticsManager;
import org.hapjs.analyzer.model.NoticeMessage;
import org.hapjs.analyzer.views.ExpandTextView;
import org.hapjs.component.Component;
import org.hapjs.render.vdom.VDocument;
import org.hapjs.render.vdom.VElement;
import org.hapjs.runtime.R;

import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

public class NoticePanel extends AbsPanel {
    private static final String NAME = "notice";
    private static final int NOTICES_SHOW_AT_MOST = 150;
    private static final int COLOR_HIGHLIGHT_VIEW = 0x400000FF;
    private Adapter mAdapter;
    private View mMinContent;
    private View mExpandContent;
    private TextView mMinTitleView;
    private TextView mExpandTitleView;
    private HighlightObject mHighlight;
    private List<HighlightObject> mHighlightList = new LinkedList<>();
    private ClipboardManager mClipboardManager;
    private boolean mHasClearOrMinifyManual = false;

    public NoticePanel(Context context) {
        super(context, NAME, TOP);
    }

    @Override
    protected int layoutId() {
        return R.layout.layout_analyzer_notice;
    }

    @Override
    protected void onCreateFinish() {
        super.onCreateFinish();
        mMinContent = findViewById(R.id.analyzer_notice_min);
        mExpandContent = findViewById(R.id.analyzer_notice_expand);
        mMinTitleView = findViewById(R.id.analyzer_notice_min_title);
        mExpandTitleView = findViewById(R.id.btn_analyzer_notice_expand_title);
        mMinContent.setOnClickListener(v -> {
            mMinContent.setVisibility(View.GONE);
            mExpandContent.setVisibility(View.VISIBLE);
        });
        findViewById(R.id.btn_analyzer_notice_copy).setOnClickListener(v -> {
            copy();
        });
        findViewById(R.id.btn_analyzer_notice_clean).setOnClickListener(v -> {
            close(true);
        });
        findViewById(R.id.btn_analyzer_notice_min).setOnClickListener(v -> {
            removeAllNoticeHighlight();
            mExpandContent.setVisibility(View.GONE);
            mMinContent.setVisibility(View.VISIBLE);
            mHasClearOrMinifyManual = true;
        });
        RecyclerView mNoticeRecyclerView = findViewById(R.id.analyzer_notice_list);
        mNoticeRecyclerView.setItemAnimator(null);
        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
        mNoticeRecyclerView.setLayoutManager(layoutManager);
        mAdapter = new Adapter(getContext(), mNoticeRecyclerView);
        mNoticeRecyclerView.setAdapter(mAdapter);
        mAdapter.setOnItemClickListener((holder, position, message) -> {
            AnalyzerStatisticsManager.getInstance().recordNoticeClick(message);
            removeAllNoticeHighlight();
            NoticeMessage.UIAction action = message.getAction();
            NoticeMessage.ClickCallback clickCallback = message.getClickCallback();
            if (action != null) {
                VDocument currentDocument = getAnalyzerContext().getCurrentDocument();
                if (currentDocument == null || currentDocument.getComponent().getPageId() != action.mPageId) {
                    return;
                }
                if (action.mComponentIds != null && !action.mComponentIds.isEmpty()) {
                    for (int id : action.mComponentIds) {
                        VElement element = currentDocument.getElementById(id);
                        if (element == null) {
                            continue;
                        }
                        Component component = element.getComponent();
                        if (component == null) {
                            continue;
                        }
                        if (component.getHostView() != null) {
                            addNoticeHighlightView(component.getHostView(), action.mMaskColor);
                        }
                    }
                } else if (action.mViews != null && !action.mViews.isEmpty()) {
                    List<View> views = action.mViews;
                    for (View view : views) {
                        addNoticeHighlightView(view, action.mMaskColor);
                    }
                }
            }
            if (clickCallback != null) {
                clickCallback.onClick(message);
            }
        });
    }

    public void close(boolean isManual) {
        mAdapter.clearMessage();
        mExpandContent.setVisibility(View.GONE);
        mMinContent.setVisibility(View.GONE);
        dismiss();
        mHasClearOrMinifyManual = isManual;
    }

    private void copy(){
        if (mClipboardManager == null) {
            mClipboardManager = (ClipboardManager) getContext().getSystemService(Context.CLIPBOARD_SERVICE);
        }
        if (mAdapter != null) {
            List<NoticeItem> noticeList = mAdapter.getNoticeList();
            if (noticeList != null && !noticeList.isEmpty()) {
                StringBuilder stringBuilder = new StringBuilder();
                for (int i = 0; i < noticeList.size(); i++) {
                    NoticeItem item = noticeList.get(i);
                    if (item.mNoticeMessage != null) {
                        String message = item.mNoticeMessage.getMessage();
                        stringBuilder.append(i + 1).append('.').append(message).append("\n");
                    }
                }
                ClipData clipData = ClipData.newPlainText("noticeMessage", stringBuilder.toString());
                mClipboardManager.setPrimaryClip(clipData);
                Toast.makeText(getContext(),getContext().getString(R.string.analyzer_notice_copy_success), Toast.LENGTH_SHORT).show();
                AnalyzerStatisticsManager.getInstance().recordCopyNoticeMessage(noticeList.size());
            }
        }
    }

    private void setShowWarningMsgCount(int count) {
        String text = getContext().getResources().getQuantityString(R.plurals.analyzer_number_warning_message, count, count);
        mMinTitleView.setText(text);
        mExpandTitleView.setText(getContext().getResources().getQuantityString(R.plurals.analyzer_notice_expand_tips, count, count));
    }

    private void setShowErrorMsgCount(int count) {
        String text = getContext().getResources().getQuantityString(R.plurals.analyzer_number_error_message, count, count);
        mMinTitleView.setText(text);
        mExpandTitleView.setText(getContext().getResources().getQuantityString(R.plurals.analyzer_notice_expand_tips, count, count));
    }

    private void setShowMsgCount(int warningCount, int errorCount) {
        String warning = getContext().getResources().getQuantityString(R.plurals.analyzer_number_warning_message, warningCount, warningCount);
        String error = getContext().getResources().getQuantityString(R.plurals.analyzer_number_error_message, errorCount, errorCount);
        String tip = getContext().getString(R.string.analyzer_notice_min_warn_error_message_tips, warning, error);
        mMinTitleView.setText(tip);
        mExpandTitleView.setText(getContext().getResources().getQuantityString(R.plurals.analyzer_notice_expand_tips, warningCount + errorCount, warningCount + errorCount));
    }

    @Override
    void onShow() {
        super.onShow();
        if (mHasClearOrMinifyManual) {
            mExpandContent.setVisibility(View.GONE);
            mMinContent.setVisibility(View.VISIBLE);
        } else {
            mExpandContent.setVisibility(View.VISIBLE);
            mMinContent.setVisibility(View.GONE);
        }
    }

    public void pushNoticeMessage(NoticeMessage message) {
        mAdapter.addMessage(message);
        int warningMsgCount = mAdapter.getWarningMsgCount();
        int errorMsgCount = mAdapter.getErrorMsgCount();
        if (warningMsgCount > 0 && errorMsgCount > 0) {
            setShowMsgCount(warningMsgCount, errorMsgCount);
        } else if (warningMsgCount > 0) {
            setShowWarningMsgCount(warningMsgCount);
        } else if (errorMsgCount > 0) {
            setShowErrorMsgCount(errorMsgCount);
        }
    }

    @Override
    void onDismiss() {
        removeAllNoticeHighlight();
    }

    public void addNoticeHighlightView(View view) {
        addNoticeHighlightView(view, COLOR_HIGHLIGHT_VIEW);
    }

    public void addNoticeHighlightView(View view, int color) {
        if (view == null) {
            return;
        }
        HighlightDrawable drawable = new HighlightDrawable(color);
        drawable.attachView(view);
        HighlightObject highlightObject = new HighlightObject();
        highlightObject.mView = view;
        highlightObject.mDrawable = drawable;
        view.getOverlay().add(drawable);
        mHighlightList.add(highlightObject);
    }

    public void removeAllNoticeHighlight() {
        if (mHighlightList != null) {
            for (HighlightObject highlightObject : mHighlightList) {
                highlightObject.mView.getOverlay().remove(highlightObject.mDrawable);
            }
            mHighlightList.clear();
        }
        removeSingleHighLightNoticeView();
    }

    public void highLightSingleNoticeView(View view) {
        highLightSingleNoticeView(view, COLOR_HIGHLIGHT_VIEW);
    }

    public void highLightSingleNoticeView(View view, int color) {
        if (mHighlight != null) {
            View highlightView = mHighlight.mView;
            highlightView.getOverlay().remove(mHighlight.mDrawable);
            mHighlight = null;
        }
        if (view == null) {
            return;
        }
        HighlightDrawable drawable = new HighlightDrawable(color);
        drawable.attachView(view);
        view.getOverlay().add(drawable);
        HighlightObject highlightObject = new HighlightObject();
        highlightObject.mView = view;
        highlightObject.mDrawable = drawable;
        mHighlight = highlightObject;
    }

    public void removeSingleHighLightNoticeView() {
        if (mHighlight != null) {
            mHighlight.mView.getOverlay().remove(mHighlight.mDrawable);
            mHighlight = null;
        }
    }

    private static class HighlightObject {
        View mView;
        HighlightDrawable mDrawable;
    }

    private static class HighlightDrawable extends ColorDrawable {
        HighlightDrawable(int color) {
            super(color);
        }

        void attachView(View view) {
            setBounds(0, 0, view.getMeasuredWidth(), view.getMeasuredHeight());
        }
    }

    private static final class Adapter extends RecyclerView.Adapter<Holder> {

        private Context mContext;
        private List<NoticeItem> mMessages;
        private List<NoticeItem> mWarnMessages;
        private List<NoticeItem> mErrorMessages;
        private RecyclerView mRecyclerView;
        private OnItemClickListener mOnItemClickListener;

        Adapter(Context context, RecyclerView recyclerView) {
            mContext = context;
            mMessages = new LinkedList<>();
            mWarnMessages = new LinkedList<>();
            mErrorMessages = new LinkedList<>();
            mRecyclerView = recyclerView;
        }

        public interface OnItemClickListener {
            void onItemClick(Holder holder, int position, NoticeMessage message);
        }

        void setOnItemClickListener(OnItemClickListener onItemClickListener) {
            mOnItemClickListener = onItemClickListener;
        }

        @NonNull
        @Override
        public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(mContext).inflate(R.layout.layout_anayler_notice_item, parent, false);
            return new Holder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull Holder holder, int position) {
            NoticeItem noticeItem = mMessages.get(position);
            NoticeMessage noticeMessage = noticeItem.mNoticeMessage;
            holder.mTv.setOnClickListener(v -> {
                if (position != RecyclerView.NO_POSITION && mOnItemClickListener != null) {
                    mOnItemClickListener.onItemClick(holder, position, mMessages.get(position).mNoticeMessage);
                }
            });
            holder.mTv.setRenderText(noticeMessage.getMessage(), noticeItem.mExpanded);
            holder.mTv.setCallback(new ExpandTextView.Callback() {
                @Override
                public void onExpand() {
                    noticeItem.mExpanded = true;
                }

                @Override
                public void onCollapse() {
                    noticeItem.mExpanded = false;
                }
            });
            if (TextUtils.equals(noticeMessage.getLevel(), NoticeMessage.LEVEL_COMMON_ERROR)) {
                holder.mDotView.setImageResource(R.drawable.ic_analyzer_notice_dot_error);
            } else {
                holder.mDotView.setImageResource(R.drawable.ic_analyzer_notice_dot_nor);
            }
        }

        @Override
        public int getItemCount() {
            return mMessages.size();
        }

        List<NoticeItem> getNoticeList(){
            return mMessages;
        }

        int getErrorMsgCount() {
            return mErrorMessages.size();
        }

        int getWarningMsgCount() {
            return mWarnMessages.size();
        }

        void addMessage(NoticeMessage message) {
            NoticeItem item = new NoticeItem(message);
            boolean isError = TextUtils.equals(message.getLevel(), NoticeMessage.LEVEL_COMMON_ERROR);
            if (mMessages.contains(item)) {
                if (isError) {
                    int index = mErrorMessages.indexOf(item);
                    if (index > -1 && index < mErrorMessages.size()) {
                        mErrorMessages.set(mErrorMessages.indexOf(item), item);
                    }
                } else {
                    int index = mWarnMessages.indexOf(item);
                    if (index > -1 && index < mWarnMessages.size()) {
                        mWarnMessages.set(mWarnMessages.indexOf(item), item);
                    }
                }
                int index = mMessages.indexOf(item);
                if (index > -1 && index < mMessages.size()) {
                    mMessages.set(mMessages.indexOf(item), item);
                }
                notifyItemChanged(index);
            } else {
                if (mMessages.size() >= NOTICES_SHOW_AT_MOST) {
                    // display 100 notices at most
                    NoticeItem removeItem = mMessages.remove(0);
                    String type = removeItem.mNoticeMessage.getLevel();
                    if (TextUtils.equals(type, NoticeMessage.LEVEL_COMMON_ERROR)) {
                        mErrorMessages.remove(removeItem);
                    } else if (TextUtils.equals(type, NoticeMessage.LEVEL_COMMON_WARN)) {
                        mWarnMessages.remove(removeItem);
                    }
                }
                if (isError) {
                    mErrorMessages.add(item);
                } else {
                    mWarnMessages.add(item);
                }
                mMessages.add(item);
                int position = mMessages.size() - 1;
                notifyItemInserted(position);
                mRecyclerView.post(() -> mRecyclerView.scrollToPosition(position));
            }
        }

        void clearMessage() {
            mMessages.clear();
            mWarnMessages.clear();
            mErrorMessages.clear();
            notifyDataSetChanged();
        }
    }

    private static class Holder extends RecyclerView.ViewHolder {
        private final ExpandTextView mTv;
        private final ImageView mDotView;

        Holder(@NonNull View itemView) {
            super(itemView);
            mDotView = itemView.findViewById(R.id.analyzer_notice_item_dot);
            mTv = itemView.findViewById(R.id.analyzer_notice_item_text);
        }
    }

    static class NoticeItem {
        NoticeMessage mNoticeMessage;
        boolean mExpanded;

        NoticeItem(NoticeMessage message) {
            mNoticeMessage = message;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            NoticeItem that = (NoticeItem) o;
            return Objects.equals(mNoticeMessage, that.mNoticeMessage);
        }

        @Override
        public int hashCode() {
            return Objects.hash(mNoticeMessage);
        }
    }
}
