/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.debugger;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatSpinner;
import androidx.appcompat.widget.Toolbar;
import org.hapjs.debugger.app.impl.R;
import org.hapjs.debugger.debug.AppDebugManager;
import org.hapjs.debugger.debug.CardDebugManager;
import org.hapjs.debugger.fragment.DebugFragmentManager;

public class MainActivity extends AppCompatActivity implements AdapterView.OnItemSelectedListener {
    private static final String TAG = "MainActivity";
    private static final String KEY_RPK_ADDRESS = "rpk_address";
    private AppCompatSpinner mModeSpinner;
    private DebugFragmentManager mDebugFragmentManager;
    private ArrayAdapter mModeAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //从近期任务栏启动，Intent携带原来的rpk地址数据,需要清空
        if (isUniversalScan(getIntent()) && isLaunchFromRecentsTask(savedInstanceState)) {
            getIntent().setData(null);
        }
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        mDebugFragmentManager = createDebugFragmentManager();
        mDebugFragmentManager.showDebugFragment(getIntent());

        mModeSpinner = ((AppCompatSpinner) findViewById(R.id.mode_spinner));
        mModeAdapter = new ArrayAdapter<String>(MainActivity.this, R.layout
                .item_spinner_select, mDebugFragmentManager.getNamesArray());
        mModeSpinner.setAdapter(mModeAdapter);
        mModeSpinner.setSelection(mDebugFragmentManager.getMode());
        mModeSpinner.setOnItemSelectedListener(this);
    }

    protected DebugFragmentManager createDebugFragmentManager() {
        return new DebugFragmentManager(this, R.id.container);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mDebugFragmentManager.refreshModeList()) {
            mModeAdapter.clear();
            mModeAdapter.addAll(mDebugFragmentManager.getNamesArray());
            mModeAdapter.notifyDataSetChanged();
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        if (mDebugFragmentManager != null) {
            boolean modeChanged = mDebugFragmentManager.onNewIntent(intent);
            if (modeChanged) {
                mModeSpinner.setSelection(mDebugFragmentManager.getMode());
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        AppDebugManager.getInstance(this).close();
        CardDebugManager.getInstance(this).close();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            Intent intent = new Intent();
            intent.setClass(this, SettingsActivity.class);
            startActivity(intent);
            return true;
        } else if (id == R.id.action_launch_app_test) {
            startActivity(new Intent(this, AppLaunchTestActivity.class));
            return true;
        } else if (id == R.id.action_get_signature) {
            startActivity(new Intent(this, SignatureActivity.class));
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        mDebugFragmentManager.showDebugFragment(position);
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {

    }

    private boolean isLaunchFromRecentsTask(Bundle savedInstanceState) {
        return (savedInstanceState != null) || ((getIntent().getFlags() & Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY)
                == Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY);
    }

    private boolean isUniversalScan(Intent intent) {
        if (intent != null && intent.getData() != null) {
            Uri uri = intent.getData();
            if (!TextUtils.isEmpty(uri.getQueryParameter(KEY_RPK_ADDRESS))) {
                return true;
            }
        }
        return false;
    }

}
