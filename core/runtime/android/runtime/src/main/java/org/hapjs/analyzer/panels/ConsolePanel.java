/*
 * Copyright (c) 2022, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.analyzer.panels;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.hapjs.analyzer.model.LogPackage;
import org.hapjs.analyzer.monitors.LogcatMonitor;
import org.hapjs.analyzer.tools.AnalyzerThreadManager;
import org.hapjs.analyzer.views.EmptyRecyclerView;
import org.hapjs.common.utils.DisplayUtil;
import org.hapjs.runtime.R;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;

public class ConsolePanel extends CollapsedPanel {
    public static final String NAME = "console";
    private static final int MAX_DISPLAY__COUNT = 2000;
    private LogListAdapter mLogListAdapter;
    private boolean mVisibleComplete;
    private LinkedList<LogPackage> mTmpLogCache = new LinkedList<>();
    private View mClearBtn;
    private EditText mFilterEdit;
    private View mEmptyView;
    private boolean mFilterMode;
    private boolean mFilterModeChanging;

    public ConsolePanel(Context context) {
        super(context, NAME, BOTTOM);
    }

    @Override
    protected int panelLayoutId() {
        return R.layout.layout_analyzer_console;
    }

    @Override
    protected void onCreateFinish() {
        super.onCreateFinish();
        mClearBtn = findViewById(R.id.btn_panel_log_clear);
        mFilterEdit = findViewById(R.id.analyzer_log_filter_edit);
        EmptyRecyclerView logRecyclerView = findViewById(R.id.analyzer_log_list);
        mEmptyView = findViewById(R.id.analyzer_log_empty_view);
        View funcContainer = findViewById(R.id.analyzer_log_func_container);
        configLogRecyclerView(logRecyclerView);
        ImageView lockImageView = findViewById(R.id.btn_panel_log_lock);
        View jsLogBtn = findViewById(R.id.btn_panel_log_type_js);
        View nativeLogBtn = findViewById(R.id.btn_panel_log_type_native);
        View logFilterStyleBtn = findViewById(R.id.btn_panel_log_filter_style);
        RadioGroup logLevelGroup = findViewById(R.id.analyzer_log_level_group);
        View logFilterContainer = findViewById(R.id.analyzer_log_filter_container);
        ViewGroup logFilterContainerParent = findViewById(R.id.analyzer_log_filter_container_parent);
        View btnLogFilter = findViewById(R.id.btn_panel_log_filter);
        View btnLogFilterCancel = findViewById(R.id.btn_panel_log_filter_clear);
        View btnLogFilterCollapse = findViewById(R.id.btn_panel_log_filter_collapse);
        btnLogFilter.setOnClickListener(v -> {
            if (!mFilterMode && !mFilterModeChanging) {
                logFilterContainer.setSelected(false);
                btnLogFilterCancel.setVisibility(TextUtils.isEmpty(mFilterEdit.getText().toString()) ? INVISIBLE : VISIBLE);
                mFilterEdit.requestFocus();
                mFilterMode = true;
                mFilterModeChanging = true;
                btnLogFilterCollapse.setVisibility(VISIBLE);
                btnLogFilterCollapse.setAlpha(0);
                post(() -> {
                    int containerWidth = logFilterContainerParent.getWidth();
                    int widthTo = mFilterEdit.getWidth() + containerWidth - logFilterContainer.getRight();
                    int widthFrom = mFilterEdit.getWidth();
                    ValueAnimator valueAnimator = ValueAnimator.ofInt(widthFrom, widthTo);
                    valueAnimator.setDuration(300);
                    valueAnimator.setRepeatCount(0);
                    valueAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
                    valueAnimator.addUpdateListener(animation -> {
                        int animatedValue = (int) animation.getAnimatedValue();
                        ViewGroup.LayoutParams layoutParams = mFilterEdit.getLayoutParams();
                        layoutParams.width = animatedValue;
                        mFilterEdit.setLayoutParams(layoutParams);
                        btnLogFilterCollapse.setAlpha(animation.getAnimatedFraction());
                        funcContainer.setAlpha(1 - animation.getAnimatedFraction());
                    });
                    valueAnimator.addListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            funcContainer.setVisibility(GONE);
                            mFilterModeChanging = false;
                        }
                    });
                    valueAnimator.start();
                });
                showSoftInput();
            }
        });
        btnLogFilterCollapse.setOnClickListener(v -> {
            if (mFilterMode && !mFilterModeChanging) {
                mFilterMode = false;
                mFilterModeChanging = true;
                btnLogFilterCancel.setVisibility(GONE);
                funcContainer.setVisibility(VISIBLE);
                funcContainer.setAlpha(0);
                ValueAnimator valueAnimator = ValueAnimator.ofInt(mFilterEdit.getWidth(), 0);
                valueAnimator.setDuration(300);
                valueAnimator.setRepeatCount(0);
                valueAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
                valueAnimator.addUpdateListener(animation -> {
                    int animatedValue = (int) animation.getAnimatedValue();
                    ViewGroup.LayoutParams layoutParams = mFilterEdit.getLayoutParams();
                    layoutParams.width = animatedValue;
                    mFilterEdit.setLayoutParams(layoutParams);
                    btnLogFilterCollapse.setAlpha(1 - valueAnimator.getAnimatedFraction());
                    funcContainer.setAlpha(valueAnimator.getAnimatedFraction());
                });
                valueAnimator.addListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        btnLogFilterCollapse.setVisibility(GONE);
                        if (mFilterEdit.getText().toString().length() > 0) {
                            logFilterContainer.setSelected(true);
                        }
                        mFilterModeChanging = false;
                    }
                });
                valueAnimator.start();
                hiddenSoftInput();
            }
        });
        btnLogFilterCancel.setOnClickListener(v -> {
            if (!TextUtils.isEmpty(mFilterEdit.getText().toString())) {
                mFilterEdit.setText("");
            }
        });
        mClearBtn.setOnClickListener(v -> {
            LogcatMonitor logcatMonitor = getLogcatMonitor();
            if (logcatMonitor != null) {
                logcatMonitor.clearLog();
            }
            mLogListAdapter.clearLog();
        });
        lockImageView.setOnClickListener(v -> {
            lockImageView.setSelected(!lockImageView.isSelected());
            mLogListAdapter.setLockLog(lockImageView.isSelected());
        });
        mFilterEdit.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                LogcatMonitor logcatMonitor = getLogcatMonitor();
                if (logcatMonitor != null) {
                    String filter = s.toString().trim();
                    logcatMonitor.setFilter(filter);
                    mLogListAdapter.clearLog();
                }
                btnLogFilterCancel.setVisibility(TextUtils.isEmpty(s.toString()) ? INVISIBLE : VISIBLE);
            }
        });
        jsLogBtn.setSelected(true);
        nativeLogBtn.setSelected(false);
        jsLogBtn.setOnClickListener(v -> {
            jsLogBtn.setSelected(!jsLogBtn.isSelected());
            setJsLogShow(jsLogBtn.isSelected());
        });
        nativeLogBtn.setOnClickListener(v -> {
            nativeLogBtn.setSelected(!nativeLogBtn.isSelected());
            setNativeLogShow(nativeLogBtn.isSelected());
        });
        logLevelGroup.setOnCheckedChangeListener((group, checkedId) -> {
            int level = -1;
            if (R.id.analyzer_log_level_v == checkedId) {
                level = Log.VERBOSE;
            } else if (R.id.analyzer_log_level_d == checkedId) {
                level = Log.DEBUG;
            } else if (R.id.analyzer_log_level_i == checkedId) {
                level = Log.INFO;
            } else if (R.id.analyzer_log_level_w == checkedId) {
                level = Log.WARN;
            } else if (R.id.analyzer_log_level_e == checkedId) {
                level = Log.ERROR;
            }
            if (level != -1) {
                setLogLevel(level);
            }
        });
        setLogLevel(Log.VERBOSE);
        setControlView(findViewById(R.id.btn_analyzer_console_ctl_line));
        addDragShieldView(Collections.singletonList(findViewById(R.id.analyzer_log_list_container)));
        logFilterStyleBtn.setOnClickListener(v -> {
            logFilterStyleBtn.setSelected(!logFilterStyleBtn.isSelected());
            if (logFilterStyleBtn.isSelected()) {
                setLogStyle(LogcatMonitor.TYPE_LOG_STYLE_ANDROID);
            } else {
                setLogStyle(LogcatMonitor.TYPE_LOG_STYLE_WEB);
            }
        });
    }

    private void configLogRecyclerView(EmptyRecyclerView logRecyclerView) {
        GradientDrawable lineDrawable = new GradientDrawable();
        lineDrawable.setColor(Color.TRANSPARENT);
        lineDrawable.setSize(0, DisplayUtil.dip2Pixel(getContext(), 5));
        DividerItemDecoration line = new DividerItemDecoration(getContext(), DividerItemDecoration.VERTICAL);
        line.setDrawable(lineDrawable);
        logRecyclerView.addItemDecoration(line);
        logRecyclerView.setItemAnimator(null);
        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
        logRecyclerView.setLayoutManager(layoutManager);
        LogListAdapter logListAdapter = new LogListAdapter(getContext(), logRecyclerView);
        logRecyclerView.setAdapter(logListAdapter);
        mLogListAdapter = logListAdapter;
        logRecyclerView.setDataSizeChangedCallback(size -> {
            mClearBtn.setEnabled(size > 0);
            if (mEmptyView != null) {
                mEmptyView.setVisibility(size > 0 ? GONE : VISIBLE);
            }
        });
        setUpRecyclerView(logRecyclerView);
    }

    private LogcatMonitor getLogcatMonitor() {
        return getMonitor(LogcatMonitor.NAME);
    }

    private void setJsLogShow(boolean show) {
        LogcatMonitor logcatMonitor = getLogcatMonitor();
        if (logcatMonitor == null) {
            return;
        }
        int logType = logcatMonitor.getLogType();
        if (show) {
            logType |= LogcatMonitor.LOG_JS;
        } else {
            logType &= ~LogcatMonitor.LOG_JS;
        }
        logcatMonitor.setLogType(logType);
        mLogListAdapter.clearLog();
    }

    private void setNativeLogShow(boolean show) {
        LogcatMonitor logcatMonitor = getLogcatMonitor();
        if (logcatMonitor == null) {
            return;
        }
        int logType = logcatMonitor.getLogType();
        if (show) {
            logType |= LogcatMonitor.LOG_NATIVE;
        } else {
            logType &= ~LogcatMonitor.LOG_NATIVE;
        }
        logcatMonitor.setLogType(logType);
        mLogListAdapter.clearLog();
    }

    private void setLogLevel(int level) {
        LogcatMonitor logcatMonitor = getLogcatMonitor();
        if (logcatMonitor != null) {
            logcatMonitor.setLogLevel(level);
            mLogListAdapter.clearLog();
        }
    }

    private void setLogStyle(int style) {
        LogcatMonitor logcatMonitor = getLogcatMonitor();
        if (logcatMonitor == null) {
            return;
        }
        logcatMonitor.setLogStyle(style);
        mLogListAdapter.clearLog();
    }

    @Override
    void onShow() {
        super.onShow();
        LogcatMonitor monitor = getMonitor(LogcatMonitor.NAME);
        if (monitor == null) {
            return;
        }
        AnalyzerThreadManager.getInstance().getMainHandler().post(() -> {
            monitor.setPipeline(data -> addLog(data));
            monitor.start();
        });
    }

    @Override
    protected void onShowAnimationFinished() {
        super.onShowAnimationFinished();
        mVisibleComplete = true;
        if (!mTmpLogCache.isEmpty()) {
            List<LogPackage.LogData> logDatas = new LinkedList<>();
            for (LogPackage p : mTmpLogCache) {
                logDatas.addAll(p.datas);
            }
            mTmpLogCache.clear();
            mLogListAdapter.addLogDatas(0, logDatas);
        }
    }

    @Override
    void onDismiss() {
        super.onDismiss();
        mVisibleComplete = false;
        LogcatMonitor monitor = getMonitor(LogcatMonitor.NAME);
        if (monitor == null) {
            return;
        }
        monitor.setPipeline(null);
        monitor.stop();
        mTmpLogCache.clear();
        mLogListAdapter.clearLog();
        hiddenSoftInput();
    }

    private void hiddenSoftInput() {
        try {
            InputMethodManager inputMethodManager = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
            if (inputMethodManager != null) {
                inputMethodManager.hideSoftInputFromWindow(mFilterEdit.getWindowToken(), 0);
            }
        } catch (Exception e) {
            // ignore
        }
    }

    private void showSoftInput() {
        try {
            InputMethodManager inputMethodManager = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
            if (inputMethodManager != null) {
                inputMethodManager.showSoftInput(mFilterEdit, InputMethodManager.SHOW_IMPLICIT);
            }
        } catch (Exception e) {
            // ignore
        }
    }

    private void addLog(LogPackage logPackage) {
        if (!mVisibleComplete) {
            if (mTmpLogCache.size() >= MAX_DISPLAY__COUNT) {
                mTmpLogCache.removeFirst();
            }
            mTmpLogCache.add(logPackage);
            return;
        }
        mLogListAdapter.addLogDatas(logPackage.position, logPackage.datas);
    }

    private static class LogListAdapter extends RecyclerView.Adapter<LogItemHolder> {

        private Context mContext;
        private LinkedList<LogPackage.LogData> mLogDatas;
        private RecyclerView mRecyclerView;
        private boolean mLockLog = false;

        LogListAdapter(Context context, RecyclerView recyclerView) {
            mContext = context;
            mLogDatas = new LinkedList<>();
            mRecyclerView = recyclerView;
        }

        @NonNull
        @Override
        public LogItemHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(mContext).inflate(R.layout.layout_anayler_log_item, parent, false);
            return new LogItemHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull LogItemHolder holder, int position) {
            LogPackage.LogData logData = mLogDatas.get(position);
            holder.setLogData(logData);
        }

        @Override
        public int getItemCount() {
            return mLogDatas.size();
        }

        void addLogDatas(int position, List<LogPackage.LogData> logDatas) {
            if (logDatas == null || logDatas.isEmpty()) {
                return;
            }
            if (logDatas.size() > MAX_DISPLAY__COUNT) {
                ListIterator<LogPackage.LogData> iterator = logDatas.listIterator(0);
                for (int i = 0, n = logDatas.size() - MAX_DISPLAY__COUNT; i < n; i++) {
                    iterator.next();
                    iterator.remove();
                }
            }
            if (mLogDatas.size() >= MAX_DISPLAY__COUNT - logDatas.size()) {
                ListIterator<LogPackage.LogData> iterator = mLogDatas.listIterator(0);
                for (int i = 0, n = MAX_DISPLAY__COUNT - logDatas.size(); i < n; i++) {
                    iterator.next();
                    iterator.remove();
                }
                notifyItemRangeRemoved(0, MAX_DISPLAY__COUNT - logDatas.size());
            }
            if (position < 0 || position > mLogDatas.size()) {
                position = mLogDatas.size();
            }
            if (logDatas.size() == 1) {
                mLogDatas.add(position, logDatas.get(0));
                notifyItemInserted(position);
                scrollToPosition(mLogDatas.size() - 1);
                return;
            }
            mLogDatas.addAll(position, logDatas);
            notifyItemRangeInserted(position, logDatas.size());
            scrollToPosition(mLogDatas.size() - 1);
        }

        private void scrollToPosition(int position) {
            if (mLockLog) {
                return;
            }
            mRecyclerView.post(() -> mRecyclerView.scrollToPosition(position));
        }

        void setLockLog(boolean lock) {
            mLockLog = lock;
        }

        void clearLog() {
            mLogDatas.clear();
            notifyDataSetChanged();
        }
    }

    private static class LogItemHolder extends RecyclerView.ViewHolder {

        private final TextView mTv;

        LogItemHolder(@NonNull View itemView) {
            super(itemView);
            mTv = itemView.findViewById(R.id.analyzer_log_item_text);
        }

        void setText(String txt) {
            mTv.setText(txt);
        }

        void setTextColor(int color) {
            mTv.setTextColor(color);
        }

        void setLogData(LogPackage.LogData logData) {
            setText(logData.mContent);
            itemView.setSelected(logData.mType == LogPackage.LOG_TYPE_JS);
            switch (logData.mLevel) {
                case Log.VERBOSE:
                    setTextColor(Color.parseColor("#BBBBBB"));
                    break;
                case Log.DEBUG:
                    setTextColor(Color.parseColor("#57BB40"));
                    break;
                case Log.INFO:
                    setTextColor(Color.parseColor("#659ABB"));
                    break;
                case Log.WARN:
                    setTextColor(Color.parseColor("#BBAC2B"));
                    break;
                case Log.ERROR:
                    setTextColor(Color.parseColor("#FF6B68"));
                    break;
                default:
                    setTextColor(Color.parseColor("#BBBBBB"));
            }
        }
    }
}
