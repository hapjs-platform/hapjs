/*
 * Copyright (c) 2021-2022, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.component.view;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.facebook.drawee.view.SimpleDraweeView;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import org.hapjs.bridge.HybridManager;
import org.hapjs.bridge.HybridView;
import org.hapjs.bridge.LifecycleListener;
import org.hapjs.common.utils.FrescoUtils;
import org.hapjs.model.MenubarItemData;
import org.hapjs.render.Display;
import org.hapjs.runtime.ConfigurationManager;
import org.hapjs.runtime.DarkThemeUtil;
import org.hapjs.runtime.HapConfiguration;
import org.hapjs.runtime.R;
import org.hapjs.runtime.RuntimeActivity;

public class BaseTitleDialog extends Dialog implements ConfigurationManager.ConfigurationListener {

    private static final int DEFAULT_MENUBAR_CONTAINER_PADDING = 6;
    private static final int DEFAULT_MENUBAR_CONTAINER_PADDING_FIVE = 7;
    private static final int DEFAULT_MENUBAR_ITEM_PADDING = 12;
    private static final String TAG = "BaseTitleDialog";
    private TextView mCurrentTitleTv = null;
    private TextView mCancleTv = null;
    private SimpleDraweeView mCurrentTitleIcon = null;
    private ImageView mCurrentMenuIcon = null;
    private ImageView mCurrentAboutIcon = null;
    private TextView mCurrentMenuStatusTv = null;
    private LinearLayout mMenubarLeftContainer = null;
    private LinearLayout mMenubarCancelContainer = null;
    private MenuBarClickCallback mMenuBarClickCallback = null;
    private RecyclerView mTopRecyclerView = null;
    private CheckedItemAdapter mTopCheckedItemAdapter = null;
    private List<MenubarItemData> mTopItemDatas = new ArrayList<>();
    private RecyclerView mBottomRecyclerView = null;
    private View mBottomLineView = null;
    private LinearLayout mBottomContainer = null;
    private CheckedItemAdapter mBottomCheckedItemAdapter = null;
    private List<MenubarItemData> mBottomItemDatas = new ArrayList<>();
    private String mLocationStr = "";
    private String mRecordingStr = "";
    private Boolean mIsShowAboutIcon = false;
    private double mHeadTailItemMargin = 0;
    private LifecycleListener mLifecycleListener;

    public BaseTitleDialog(@NonNull Context context) {
        super(context, R.style.HapTheme_Dialog);
        initView(context);
    }

    public BaseTitleDialog(@NonNull Context context, int themeResId) {
        super(context, themeResId);
        initView(context);
    }

    protected BaseTitleDialog(
            @NonNull Context context, boolean cancelable,
            @Nullable OnCancelListener cancelListener) {
        super(context, cancelable, cancelListener);
        initView(context);
    }

    private void initView(Context context) {
        FrescoUtils.initialize(context.getApplicationContext());
        setContentView(R.layout.titlebar_dialog_view);
        mLocationStr = getContext().getResources().getString(R.string.menubar_dlg_using_location);
        mRecordingStr = getContext().getResources().getString(R.string.menubar_dlg_using_record);
    }

    public Context getActivityContext() {
        Context context = getContext();
        if (context instanceof ContextWrapper) {
            context = ((ContextWrapper) context).getBaseContext();
        } else if (context instanceof Activity) {
            return (Activity) context;
        }
        return context;
    }

    public void initDialog(Context context) {
        if (!(context instanceof RuntimeActivity)) {
            Log.e(TAG, "initDialog error: mContext is not an instance of RuntimeActivity.");
            return;
        }
        final RuntimeActivity act = (RuntimeActivity) context;
        initDialogView();
        initTopList(context);
        initBottomList(context);
        HybridView hybridView = act.getHybridView();
        if (null == hybridView) {
            Log.e(TAG, "initDialog hybridView is null.");
            return;
        }
        final HybridManager hybridManager = hybridView.getHybridManager();
        final ConfigurationManager.ConfigurationListener listener = this;
        mLifecycleListener =
                new LifecycleListener() {
                    @Override
                    public void onDestroy() {
                        dismiss();
                        ConfigurationManager.getInstance().removeListener(listener);
                        if (null != hybridManager) {
                            hybridManager.removeLifecycleListener(this);
                        } else {
                            Log.e(TAG, "initDialog onDestroy error hybridManager null.");
                        }
                    }

                    @Override
                    public void onPause() {
                        super.onPause();
                        if (isShowing()) {
                            dismiss();
                        }
                    }
                };
        if (null != hybridManager) {
            hybridManager.addLifecycleListener(mLifecycleListener);
        } else {
            Log.e(TAG, "initDialog hybridManager is null.");
        }
        ConfigurationManager.getInstance().addListener(listener);
    }

    public void clearBaseTitleDialog() {
        if (isShowing()) {
            dismiss();
        }
        Context context = getActivityContext();
        if (!(context instanceof RuntimeActivity)) {
            Log.e(TAG,
                    "clearBaseTitleDialog error: mContext is not an instance of RuntimeActivity.");
            return;
        }
        final RuntimeActivity act = (RuntimeActivity) context;
        final HybridManager hybridManager = act.getHybridView().getHybridManager();
        if (null != hybridManager && null != mLifecycleListener) {
            hybridManager.removeLifecycleListener(mLifecycleListener);
        }
        ConfigurationManager.getInstance().removeListener(this);
    }

    private void initDialogView() {
        mMenubarLeftContainer = this.findViewById(R.id.menubar_dialog_left_container);
        mCurrentTitleTv = this.findViewById(R.id.title_tv);
        mCancleTv = this.findViewById(R.id.titlebar_dialog_cancel_textview);
        if (null != mCancleTv) {
            mCancleTv.setTypeface(Typeface.DEFAULT_BOLD);
        }
        mCurrentTitleIcon = this.findViewById(R.id.title_rpk_icon);
        mCurrentAboutIcon = this.findViewById(R.id.menubar_about_icon);
        mMenubarCancelContainer = this.findViewById(R.id.titlebar_dialog_cancel_layout);
        mCurrentMenuIcon = this.findViewById(R.id.titlebar_dialog_status_image);
        mCurrentMenuStatusTv = this.findViewById(R.id.titlebar_dialog_status_textview);
        mTopRecyclerView = this.findViewById(R.id.titlebar_dialog_toplistview);
        mBottomContainer = this.findViewById(R.id.titlebar_dialog_bottom_layout);
        mBottomRecyclerView = this.findViewById(R.id.titlebar_dialog_bottomlistview);
        mBottomLineView = this.findViewById(R.id.titlebar_dialog_bottom_line);
    }

    private void initTopList(Context context) {
        mTopItemDatas.clear();
        mTopCheckedItemAdapter = new CheckedItemAdapter(getContext(), mTopItemDatas, "");
        mTopRecyclerView.setLayoutManager(
                new LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false));
        mTopRecyclerView.setAdapter(mTopCheckedItemAdapter);
        mMenubarLeftContainer.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (mIsShowAboutIcon) {
                            if (null != mMenuBarClickCallback) {
                                final String aboutStr =
                                        context.getResources()
                                                .getString(R.string.menubar_dlg_about);
                                mMenuBarClickCallback.onMenuBarItemClick(-1, aboutStr, null, null);
                            }
                            dismiss();
                        }
                    }
                });
        mMenubarCancelContainer.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        dismiss();
                    }
                });
        mTopCheckedItemAdapter.setItemClickListner(
                new ItemClickListner() {
                    @Override
                    public void onItemClick(int position, View itemView) {
                        String keyStr = "";
                        String contentStr = "";
                        if (null != mMenuBarClickCallback) {
                            CharSequence key = null;
                            CharSequence content = null;
                            MenubarItemData itemData = null;
                            if (null != mTopCheckedItemAdapter) {
                                List<MenubarItemData> datas = mTopCheckedItemAdapter.getItemDatas();
                                if (null != datas && datas.size() > position) {
                                    itemData = datas.get(position);
                                    key = itemData.getKey();
                                    content = itemData.getName();
                                }
                            }
                            if (null != key) {
                                keyStr = key.toString();
                            }
                            if (null != content) {
                                contentStr = content.toString();
                            }
                            mMenuBarClickCallback
                                    .onMenuBarItemClick(position, contentStr, keyStr, itemData);
                        }
                        dismiss();
                    }
                });
    }

    private void initBottomList(Context context) {
        mBottomItemDatas.clear();
        mBottomCheckedItemAdapter = new CheckedItemAdapter(getContext(), mBottomItemDatas, "");
        mBottomRecyclerView.setLayoutManager(
                new LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false));
        mBottomRecyclerView.setAdapter(mBottomCheckedItemAdapter);
        mBottomCheckedItemAdapter.setItemClickListner(
                new ItemClickListner() {
                    @Override
                    public void onItemClick(int position, View itemView) {
                        String keyStr = "";
                        String contentStr = "";
                        if (null != mMenuBarClickCallback) {
                            CharSequence key = null;
                            CharSequence content = null;
                            MenubarItemData itemData = null;
                            if (null != mBottomCheckedItemAdapter) {
                                List<MenubarItemData> datas =
                                        mBottomCheckedItemAdapter.getItemDatas();
                                if (null != datas && datas.size() > position) {
                                    itemData = datas.get(position);
                                    key = itemData.getKey();
                                    content = itemData.getName();
                                }
                            }
                            if (null != key) {
                                keyStr = key.toString();
                            }
                            if (null != content) {
                                contentStr = content.toString();
                            }
                            mMenuBarClickCallback
                                    .onMenuBarItemClick(position, contentStr, keyStr, itemData);
                        }
                        dismiss();
                    }
                });
    }

    public void showMenuDialog(
            final List<MenubarItemData> topDatas,
            final List<MenubarItemData> bottomDatas,
            MenuBarClickCallback menuBarClickCallback,
            HashMap<String, Object> otherDatas) {

        if (isShowing()) {
            dismiss();
        }
        Integer menuStatus = Display.DISPLAY_STATUS_FINISH;
        String rpkName = "";
        if (null != otherDatas) {
            if (otherDatas.containsKey(MenubarView.MENUBAR_DIALOG_MENU_STATUS)) {
                Object status = otherDatas.get(MenubarView.MENUBAR_DIALOG_MENU_STATUS);
                if (status instanceof Integer) {
                    menuStatus = (Integer) status;
                }
            }
            if (otherDatas.containsKey(MenubarView.MENUBAR_DIALOG_RPK_NAME)) {
                Object name = otherDatas.get(MenubarView.MENUBAR_DIALOG_RPK_NAME);
                if (name instanceof String) {
                    rpkName = (String) name;
                }
            }
            if (otherDatas.containsKey(MenubarView.MENUBAR_DIALOG_SHOW_ABOUT_ICON)) {
                Object showAboutIcon = otherDatas.get(MenubarView.MENUBAR_DIALOG_SHOW_ABOUT_ICON);
                if (showAboutIcon instanceof Boolean) {
                    mIsShowAboutIcon = (Boolean) showAboutIcon;
                }
                if (null != mCurrentAboutIcon) {
                    mCurrentAboutIcon
                            .setVisibility(mIsShowAboutIcon ? View.VISIBLE : View.INVISIBLE);
                }
            }
        }
        if (null != otherDatas && otherDatas.containsKey(MenubarView.MENUBAR_DIALOG_RPK_NAME)) {
            Object name = otherDatas.get(MenubarView.MENUBAR_DIALOG_RPK_NAME);
            if (name instanceof String) {
                rpkName = (String) name;
            }
        }
        mMenuBarClickCallback = menuBarClickCallback;
        initTitleIcon(rpkName, otherDatas);
        notifyDialogStatus(menuStatus);
        int maxSize = Math.max(topDatas.size(), bottomDatas.size());
        initTopDatas(topDatas, maxSize);
        initBottomDatas(bottomDatas, maxSize);
        Window window = getWindow();
        if (window == null) {
            return;
        }
        window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        window.setWindowAnimations(R.style.MenubarAnimationDialog);
        WindowManager.LayoutParams params = window.getAttributes();
        params.height = WindowManager.LayoutParams.WRAP_CONTENT;
        params.width = WindowManager.LayoutParams.MATCH_PARENT;
        window.setGravity(Gravity.BOTTOM);
        DarkThemeUtil.disableForceDark(this);
        updateAboutImageSrc(DarkThemeUtil.isDarkMode(getContext()));
        show();
    }

    private void initTitleIcon(String rpkName, HashMap<String, Object> otherDatas) {
        if (null != mCurrentTitleTv) {
            mCurrentTitleTv.setText(rpkName);
            mCurrentTitleTv.setTypeface(Typeface.DEFAULT_BOLD);
        }
        if (null != mCurrentTitleIcon
                && null != otherDatas
                && otherDatas.containsKey(MenubarView.MENUBAR_DIALOG_RPK_ICON)) {
            Object iconPath = otherDatas.get(MenubarView.MENUBAR_DIALOG_RPK_ICON);
            if (iconPath instanceof Uri) {
                mCurrentTitleIcon.setImageURI((Uri) iconPath);
            }
        }
    }

    private void initTopDatas(final List<MenubarItemData> datas, int maxSize) {
        if (null == datas || datas.size() == 0) {
            Log.e(TAG, "initTopDatas error: datas null or size 0.");
            return;
        }
        if (null != mTopCheckedItemAdapter) {
            mTopItemDatas.clear();
            mTopItemDatas.addAll(datas);
            mTopCheckedItemAdapter.updateDate(datas, maxSize);
        } else {
            mTopItemDatas.clear();
            mTopItemDatas.addAll(datas);
            mTopCheckedItemAdapter = new CheckedItemAdapter(getContext(), mTopItemDatas, "");
        }
    }

    public void updateDatas(int valueTag, int locationTag, int position, Object content) {
        if (locationTag == MenubarItemData.TOP_ITEM_LOCATION_TAG) {
            if (position < mTopItemDatas.size()) {
                updateItemDataByTag(mTopItemDatas.get(position), valueTag, content);
                if (null != mTopCheckedItemAdapter) {
                    mTopCheckedItemAdapter.notifyItemChanged(position);
                } else {
                    Log.e(TAG, "updateDatas mTopCheckedItemAdapter is null.");
                }
            }
        } else {
            if (position < mBottomItemDatas.size()) {
                if (position < mBottomItemDatas.size()) {
                    updateItemDataByTag(mBottomItemDatas.get(position), valueTag, content);
                    if (null != mBottomCheckedItemAdapter) {
                        mBottomCheckedItemAdapter.notifyItemChanged(position);
                    } else {
                        Log.e(TAG, "updateDatas mBottomCheckedItemAdapter is null.");
                    }
                }
            }
        }
    }

    public void notifyDataChange(int locationTag, int position) {
        if (locationTag == MenubarItemData.TOP_ITEM_LOCATION_TAG) {
            if (position < mTopItemDatas.size()) {
                if (null != mTopCheckedItemAdapter) {
                    mTopCheckedItemAdapter.notifyItemChanged(position);
                } else {
                    Log.e(TAG, "notifyDataChange mTopCheckedItemAdapter is null.");
                }
            }
        } else {
            if (position < mBottomItemDatas.size()) {
                if (position < mBottomItemDatas.size()) {
                    if (null != mBottomCheckedItemAdapter) {
                        mBottomCheckedItemAdapter.notifyItemChanged(position);
                    } else {
                        Log.e(TAG, "notifyDataChange mBottomCheckedItemAdapter is null.");
                    }
                }
            }
        }
    }

    private void updateItemDataByTag(MenubarItemData itemData, int valueTag, Object data) {
        if (null == data || null == itemData) {
            Log.e(TAG, "updateItemDataByTag data or itemData is null.");
            return;
        }
        switch (valueTag) {
            case MenubarItemData.NAME_TAG:
                if (data instanceof String) {
                    String name = (String) data;
                    itemData.setName(name);
                }
                break;
            case MenubarItemData.DARWABLE_ID_TAG:
                if (data instanceof Integer) {
                    int drawableId = (Integer) data;
                    itemData.setDrawableId(drawableId);
                }
                break;
            case MenubarItemData.LOCATION_TAG:
                if (data instanceof Integer) {
                    int locationTag = (Integer) data;
                    itemData.setTag(locationTag);
                }
                break;
            case MenubarItemData.SHOW_POINT_VALUE_TAG:
                if (data instanceof Boolean) {
                    boolean isShowPoint = (Boolean) data;
                    itemData.setShowPoint(isShowPoint);
                }
                break;
            default:
                break;
        }
    }

    private void initBottomDatas(final List<MenubarItemData> datas, int maxSize) {
        if (null == datas || datas.size() == 0) {
            Log.e(TAG, "initBottomDatas error: datas null or size 0.");
            if (null != mBottomContainer) {
                mBottomContainer.setVisibility(View.GONE);
            }
            if (null != mBottomLineView) {
                mBottomLineView.setVisibility(View.GONE);
            }
            return;
        }
        mBottomContainer.setVisibility(View.VISIBLE);
        mBottomLineView.setVisibility(View.VISIBLE);
        if (null != mBottomCheckedItemAdapter) {
            mBottomItemDatas.clear();
            mBottomItemDatas.addAll(datas);
            mBottomCheckedItemAdapter.updateDate(datas, maxSize);
        } else {
            mBottomItemDatas.clear();
            mBottomItemDatas.addAll(datas);
            mBottomCheckedItemAdapter = new CheckedItemAdapter(getContext(), mBottomItemDatas, "");
        }
    }

    public void notifyDialogStatus(int menuStatus) {
        StringBuilder builder = new StringBuilder();
        if (menuStatus == Display.DISPLAY_LOCATION_START) {
            builder.append(mLocationStr);
            if (null != mCurrentMenuStatusTv) {
                mCurrentMenuStatusTv.setText(builder.toString());
            }
            if (null != mCurrentMenuIcon) {
                mCurrentMenuIcon.setImageDrawable(
                        getContext().getResources().getDrawable(R.drawable.menubar_map_img));
            }
        } else if (menuStatus == Display.DISPLAY_RECORD_START) {
            builder.append(mRecordingStr);
            if (null != mCurrentMenuStatusTv) {
                mCurrentMenuStatusTv.setText(builder.toString());
            }
            if (null != mCurrentMenuIcon) {
                mCurrentMenuIcon.setImageDrawable(
                        getContext().getResources().getDrawable(R.drawable.menubar_voice_img));
            }
        } else {
            if (null != mCurrentMenuStatusTv) {
                mCurrentMenuStatusTv.setText("");
            }
            if (null != mCurrentMenuIcon) {
                mCurrentMenuIcon.setImageDrawable(null);
            }
        }
    }

    @Override
    public void onConfigurationChanged(HapConfiguration newConfig) {
        if (newConfig.getUiMode() != newConfig.getLastUiMode()) {
            if (AppCompatDelegate.getDefaultNightMode() != AppCompatDelegate.MODE_NIGHT_NO
                    &&
                    AppCompatDelegate.getDefaultNightMode() != AppCompatDelegate.MODE_NIGHT_YES) {
                boolean darkMode = newConfig.getUiMode() == Configuration.UI_MODE_NIGHT_YES;
                updateAboutImageSrc(darkMode);
            }
        }
    }

    private void updateAboutImageSrc(boolean isDarkMode) {
        if (mCurrentAboutIcon != null) {
            mCurrentAboutIcon.setImageResource(
                    isDarkMode ? R.drawable.menubar_about_img_white : R.drawable.menubar_about_img);
        }
    }

    public List<MenubarItemData> getTopItemDatas() {
        return mTopItemDatas;
    }

    public List<MenubarItemData> getBottomItemDatas() {
        return mBottomItemDatas;
    }

    public interface MenuBarClickCallback {
        void onMenuBarItemClick(int position, String content, String keyStr, MenubarItemData data);
    }

    public interface ItemClickListner {
        void onItemClick(int position, View itemView);
    }

    private class CheckedItemAdapter
            extends RecyclerView.Adapter<CheckedItemAdapter.MenubarHolder> {
        private String itemColor;
        private Context context;
        private List<MenubarItemData> itemDatas = new ArrayList<MenubarItemData>();
        private double realWidth = ViewGroup.LayoutParams.WRAP_CONTENT;
        private int itemPadding = 0;
        private ItemClickListner itemClickListner;

        public CheckedItemAdapter(
                Context outContext, List<MenubarItemData> datas, String itemColorString) {
            context = outContext;
            itemDatas.clear();
            if (null != datas) {
                itemDatas.addAll(datas);
            }
            itemColor = itemColorString;
        }

        private void initItemWidth(int size) {

            Context context = getActivityContext();
            if (!(context instanceof Activity)) {
                Log.e(TAG, "initItemWidth error: mContext is not an instance of Activity.");
                return;
            }
            double containerPadding = (DEFAULT_MENUBAR_CONTAINER_PADDING
                    * context.getResources().getDisplayMetrics().density);
            double containerPaddingFive = (DEFAULT_MENUBAR_CONTAINER_PADDING_FIVE
                    * context.getResources().getDisplayMetrics().density);
            double itemPadding = (DEFAULT_MENUBAR_ITEM_PADDING
                    * context.getResources().getDisplayMetrics().density);
            Activity activity = (Activity) context;
            WindowManager manager = activity.getWindowManager();
            DisplayMetrics outMetrics = new DisplayMetrics();
            manager.getDefaultDisplay().getMetrics(outMetrics);
            double width = outMetrics.widthPixels;
            if (size > 5) {
                realWidth = (width - containerPadding) / 4.5;
                mHeadTailItemMargin = containerPadding;
            } else {
                realWidth = (width - 2 * containerPaddingFive) / 5.0;
                mHeadTailItemMargin = containerPaddingFive;
            }
        }

        public void updateDate(List<MenubarItemData> data, int maxSize) {
            itemDatas.clear();
            if (null != data) {
                itemDatas.addAll(data);
            }
            initItemWidth(maxSize);
            notifyDataSetChanged();
        }

        public void setItemClickListner(ItemClickListner clickListner) {
            itemClickListner = clickListner;
        }

        @Override
        public MenubarHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View parentView =
                    LayoutInflater.from(context).inflate(R.layout.dialog_item_view, parent, false);
            int tmpRealWidth = (int) realWidth;
            if (tmpRealWidth != ViewGroup.LayoutParams.WRAP_CONTENT) {
                parentView.setLayoutParams(
                        new LinearLayout.LayoutParams(tmpRealWidth,
                                ViewGroup.LayoutParams.MATCH_PARENT));
            }
            return new MenubarHolder(parentView);
        }

        @Override
        public void onBindViewHolder(MenubarHolder holder, int position) {
            if (position < itemDatas.size()) {
                MenubarItemData menubarItemData = null;
                menubarItemData = itemDatas.get(position);
                if (null != menubarItemData) {
                    holder.textView.setText(menubarItemData.getName());
                    int resId = menubarItemData.getDrawableId();
                    if (resId != 0) {
                        holder.imageView
                                .setImageDrawable(context.getResources().getDrawable(resId));
                    }
                    if (menubarItemData.isShowPoint()) {
                        holder.redPointimageView.setVisibility(View.VISIBLE);
                    } else {
                        holder.redPointimageView.setVisibility(View.GONE);
                    }
                }
            }

            if (itemDatas.size() <= 5) {
                ViewGroup.LayoutParams layoutParams = holder.itemView.getLayoutParams();
                if (layoutParams instanceof RecyclerView.LayoutParams) {
                    if (position == 0) {
                        ((RecyclerView.LayoutParams) layoutParams).leftMargin =
                                (int) mHeadTailItemMargin;
                        ((RecyclerView.LayoutParams) layoutParams).rightMargin = 0;
                    } else if (position == (itemDatas.size() - 1)) {
                        ((RecyclerView.LayoutParams) layoutParams).leftMargin = itemPadding;
                        ((RecyclerView.LayoutParams) layoutParams).rightMargin =
                                (int) mHeadTailItemMargin;
                    } else {
                        ((RecyclerView.LayoutParams) layoutParams).leftMargin = itemPadding;
                        ((RecyclerView.LayoutParams) layoutParams).rightMargin = 0;
                    }

                } else if (layoutParams instanceof LinearLayout.LayoutParams) {
                    if (position == 0) {
                        ((LinearLayout.LayoutParams) layoutParams).leftMargin =
                                (int) mHeadTailItemMargin;
                        ((LinearLayout.LayoutParams) layoutParams).rightMargin = 0;
                    } else if (position == (itemDatas.size() - 1)) {
                        ((LinearLayout.LayoutParams) layoutParams).leftMargin = itemPadding;
                        ((LinearLayout.LayoutParams) layoutParams).rightMargin =
                                (int) mHeadTailItemMargin;
                    } else {
                        ((LinearLayout.LayoutParams) layoutParams).leftMargin = itemPadding;
                        ((LinearLayout.LayoutParams) layoutParams).rightMargin = 0;
                    }
                }
            } else {
                ViewGroup.LayoutParams layoutParams = holder.itemView.getLayoutParams();
                if (layoutParams instanceof RecyclerView.LayoutParams) {
                    if (position == 0) {
                        ((RecyclerView.LayoutParams) layoutParams).leftMargin =
                                (int) mHeadTailItemMargin;
                        ((RecyclerView.LayoutParams) layoutParams).rightMargin = 0;
                    } else if (position == (itemDatas.size() - 1)) {
                        ((RecyclerView.LayoutParams) layoutParams).leftMargin = 0;
                        ((RecyclerView.LayoutParams) layoutParams).rightMargin = 0;
                    } else {
                        ((RecyclerView.LayoutParams) layoutParams).leftMargin = 0;
                        ((RecyclerView.LayoutParams) layoutParams).rightMargin = 0;
                    }

                } else if (layoutParams instanceof LinearLayout.LayoutParams) {
                    if (position == 0) {
                        ((LinearLayout.LayoutParams) layoutParams).leftMargin =
                                (int) mHeadTailItemMargin;
                        ((LinearLayout.LayoutParams) layoutParams).rightMargin = 0;
                    } else if (position == (itemDatas.size() - 1)) {
                        ((LinearLayout.LayoutParams) layoutParams).leftMargin = 0;
                        ((LinearLayout.LayoutParams) layoutParams).rightMargin = 0;
                    } else {
                        ((LinearLayout.LayoutParams) layoutParams).leftMargin = 0;
                        ((LinearLayout.LayoutParams) layoutParams).rightMargin = 0;
                    }
                }
            }
            holder.textView.setTextColor(
                    context.getResources().getColor(R.color.dialog_title_text_color));
            holder.itemView.setOnClickListener(
                    new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            ImageView iv = holder.redPointimageView;
                            if (null != iv && iv.getVisibility() == View.VISIBLE) {
                                iv.setVisibility(View.GONE);
                            }
                            if (null != itemClickListner) {
                                itemClickListner.onItemClick(position, holder.itemView);
                            }
                        }
                    });
        }

        @Override
        public int getItemCount() {
            return itemDatas.size();
        }

        public List<MenubarItemData> getItemDatas() {
            return itemDatas;
        }

        private class MenubarHolder extends RecyclerView.ViewHolder {

            private TextView textView;
            private ImageView imageView;
            private ImageView redPointimageView;

            public MenubarHolder(View itemView) {
                super(itemView);
                imageView = (ImageView) itemView.findViewById(R.id.titlebar_dialog_item_image);
                textView = (TextView) itemView.findViewById(R.id.title_item_tv);
                redPointimageView = (ImageView) itemView.findViewById(R.id.menubar_item_point_iv);
            }
        }
    }
}
