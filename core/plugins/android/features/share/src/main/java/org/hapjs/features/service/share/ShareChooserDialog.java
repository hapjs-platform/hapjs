/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.features.service.share;

import android.app.ActivityManager;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.List;
import org.hapjs.common.utils.ThemeUtils;
import org.hapjs.runtime.HybridDialog;
import org.hapjs.runtime.HybridDialogProvider;
import org.hapjs.runtime.ProviderManager;

public final class ShareChooserDialog extends DialogFragment {

    private static final String TAG = "ShareChooserDialog";
    private static final int DEFAULT_COLUMNS = 5;

    private GridView mGrid;
    private ShareListAdapter mAdapter;
    private List<Platform> mPlatForms;
    private String mTitle;
    private ShareChooserListener mShareChooserListener;
    private OnItemClickListener mShareItemListener = new OnItemClickListener();


    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Context context = getActivity();
        LayoutInflater inflater = LayoutInflater.from(context);
        ViewGroup parent = (ViewGroup) inflater.inflate(R.layout.feature_share_chooser, null);

        initialize(parent);

        return createVendorDialog(parent);
    }

    protected Dialog createVendorDialog(ViewGroup parent) {
        HybridDialogProvider provider =
                ProviderManager.getDefault().getProvider(HybridDialogProvider.NAME);
        HybridDialog hybridDialog = provider.createAlertDialog(getActivity(),
                ThemeUtils.getAlertDialogTheme());
        hybridDialog.setView(parent);
        String title = !TextUtils.isEmpty(mTitle) ? mTitle :
                getText(R.string.share_dialog_title).toString();
        hybridDialog.setTitle(title);
        hybridDialog.setButton(DialogInterface.BUTTON_NEGATIVE,
                getActivity().getResources().getString(R.string.share_dialog_cancel),
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        ShareChooserDialog.this.dismissAllowingStateLoss();
                        if (mShareChooserListener != null) {
                            mShareChooserListener.onCancel();
                        }
                    }
                });

        return hybridDialog.createDialog();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        if (getDialog().getWindow() != null) {
            getDialog().getWindow().setGravity(Gravity.BOTTOM);
        }
        return super.onCreateView(inflater, container, savedInstanceState);
    }

    private void initialize(View view) {
        Context context = getActivity();
        mGrid = (GridView) view.findViewById(R.id.share_gird);
        mAdapter = new ShareListAdapter(context);
        mAdapter.setShareList(mPlatForms);
        mGrid.setAdapter(mAdapter);
        mGrid.setOnItemClickListener(mShareItemListener);
        mGrid.setNumColumns(DEFAULT_COLUMNS);

        final ActivityManager am =
                (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        mAdapter.setIconSize(am.getLauncherLargeIconSize());
    }

    public void setPlatForms(List<Platform> platForms) {
        mPlatForms = platForms;
    }

    public void setTitle(String title) {
        mTitle = title;
    }

    public void setShareChooserListener(ShareChooserListener listener) {
        mShareChooserListener = listener;
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
    }


    public interface ShareChooserListener {
        void onSelect(Platform platForm);

        void onCancel();
    }

    private class OnItemClickListener implements AdapterView.OnItemClickListener {
        @Override
        public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
            ShareChooserDialog.this.dismissAllowingStateLoss();
            Platform platForm = mPlatForms.get(position);
            if (mShareChooserListener != null) {
                mShareChooserListener.onSelect(platForm);
            }
        }
    }

    private final class ShareListAdapter extends BaseAdapter {

        private class ViewHolder {
            public TextView text;
            public ImageView icon;

            public ViewHolder(View view) {
                text = (TextView) view.findViewById(R.id.text);
                icon = (ImageView) view.findViewById(R.id.icon);
            }
        }


        private final List<Platform> emptyList = new ArrayList<Platform>();
        private LayoutInflater mInflater;
        private List<Platform> mList = emptyList;
        private int mIconSize;

        public ShareListAdapter(Context context) {
            mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        }

        public void setShareList(List<Platform> shareList) {
            mList = (shareList != null) ? shareList : emptyList;
        }

        public void setIconSize(int size) {
            mIconSize = size;
        }

        @Override
        public int getCount() {
            return mList.size();
        }

        @Override
        public Object getItem(int position) {
            return mList.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view;
            if (convertView == null) {
                view = mInflater.inflate(R.layout.feature_share_list_item, parent, false);

                final ViewHolder holder = new ViewHolder(view);
                view.setTag(holder);

                ViewGroup.LayoutParams lp = holder.icon.getLayoutParams();
                lp.width = lp.height = mIconSize;
            } else {
                view = convertView;
            }
            bindView(view, mList.get(position));
            return view;
        }

        private final void bindView(View view, Platform platForm) {
            final ViewHolder holder = (ViewHolder) view.getTag();
            holder.icon.setImageResource(platForm.mIcon);
            holder.text.setText(platForm.mName);
        }
    }

}
