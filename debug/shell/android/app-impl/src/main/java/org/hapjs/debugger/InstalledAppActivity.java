/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.debugger;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import java.lang.ref.WeakReference;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import org.hapjs.debugger.app.impl.R;

public class InstalledAppActivity extends AppCompatActivity {
    public static final String RESULT_PKG = "pkg";

    private AsyncTask<Void, Void, List<AppInfo>> mLoadAppTask;
    private InstalledAppAdapter mAdapter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_installed_app);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        init();
    }

    private void init() {
        mAdapter = new InstalledAppAdapter();
        ListView listView = (ListView) findViewById(android.R.id.list);
        listView.setAdapter(mAdapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                AppInfo info = (AppInfo) mAdapter.getItem(position);
                if (info != null) {
                    Intent result = new Intent();
                    result.putExtra(RESULT_PKG, info.packageInfo.packageName);
                    setResult(Activity.RESULT_OK, result);
                } else {
                    setResult(Activity.RESULT_CANCELED);
                }
                finish();
            }
        });

        mLoadAppTask = new LoadAppTask(this);
        mLoadAppTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    private void hideProgress() {
        ProgressBar progressBar = (ProgressBar) findViewById(R.id.progress);
        progressBar.setVisibility(View.GONE);
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (mLoadAppTask != null) {
            mLoadAppTask.cancel(false);
        }
    }

    private static class AppInfo {
        final PackageInfo packageInfo;
        final PackageManager mPackageManager;
        String name;
        Drawable icon;

        AppInfo(PackageInfo info, PackageManager pm) {
            packageInfo = info;
            mPackageManager = pm;
        }

        String loadLabel() {
            if (name == null) {
                CharSequence label = packageInfo.applicationInfo.loadLabel(mPackageManager);
                name = label == null ? null : label.toString();
            }
            return name;
        }

        Drawable loadIcon() {
            if (icon == null) {
                icon = packageInfo.applicationInfo.loadIcon(mPackageManager);
            }
            return icon;
        }
    }

    private static class LoadAppTask extends AsyncTask<Void, Void, List<AppInfo>> {
        private WeakReference<InstalledAppActivity> mActivityRef;

        public LoadAppTask(InstalledAppActivity activity) {
            mActivityRef = new WeakReference<>(activity);
        }

        @Override
        protected List<AppInfo> doInBackground(Void... voids) {
            final InstalledAppActivity activity = mActivityRef.get();
            if (activity != null) {
                final PackageManager pm = activity.getPackageManager();
                List<PackageInfo> infos = pm.getInstalledPackages(0);

                if (infos != null) {
                    List<AppInfo> appInfos = new ArrayList<>(infos.size());
                    for (PackageInfo info : infos) {
                        appInfos.add(new AppInfo(info, pm));
                    }

                    final Collator collactor = Collator.getInstance();
                    Collections.sort(appInfos, new Comparator<AppInfo>() {
                        @Override
                        public int compare(AppInfo o1, AppInfo o2) {
                            String name1 = o1.loadLabel();
                            String name2 = o2.loadLabel();

                            if (name1 == null && name2 == null) {
                                return 0;
                            }
                            if (name1 == null || name2 == null) {
                                return name1 == null ? -1 : 1;
                            }
                            return collactor.compare(name1, name2);
                        }
                    });
                    return appInfos;
                }
            }
            return null;
        }

        @Override
        protected void onPostExecute(List<AppInfo> result) {
            InstalledAppActivity activity = mActivityRef.get();
            if (activity != null) {
                activity.mAdapter.setInstalledApps(result);
                activity.hideProgress();
            }
        }
    }

    private class InstalledAppAdapter extends BaseAdapter {
        private List<AppInfo> mInstalledApps;

        public void setInstalledApps(List<AppInfo> installedApps) {
            mInstalledApps = installedApps;
            notifyDataSetChanged();
        }

        @Override
        public int getCount() {
            return mInstalledApps == null ? 0 : mInstalledApps.size();
        }

        @Override
        public Object getItem(int position) {
            if (position < 0 || position >= getCount()) {
                return null;
            }
            return mInstalledApps.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View itemView = convertView;
            if (itemView == null) {
                itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.installed_app_item, parent, false);
            }

            AppInfo info = (AppInfo) getItem(position);
            if (info != null) {
                Drawable icon = info.loadIcon();
                CharSequence name = info.loadLabel();
                ((ImageView) itemView.findViewById(R.id.icon)).setImageDrawable(icon);
                ((TextView) itemView.findViewById(R.id.name)).setText(name);
            }

            return itemView;
        }
    }
}
