/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.debugger;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.textfield.TextInputEditText;
import org.hapjs.debugger.app.impl.R;

public class AppLaunchTestActivity extends AppCompatActivity {
    private static final String TAG = "AppLaunchTestActivity";

    private TextInputEditText mPackageNameEditText;
    private TextInputEditText mPathEditText;
    private TextInputEditText mDeeplinkEditText;

    private Button mLaunchPathBtn;
    private Button mLaunchDeeplinkBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_app_launch_test);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        setupViews();
    }

    private void setupViews() {
        View.OnClickListener onClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (v.getId() == R.id.path_button) {
                    launchQuickApp(mPackageNameEditText.getText().toString(),
                            mPathEditText.getText().toString());
                } else if (v.getId() == R.id.deeplink_button) {
                    launchDeeplink(mDeeplinkEditText.getText().toString());
                }
            }
        };

        mPackageNameEditText = ((TextInputEditText) findViewById(R.id.package_edit_text));
        mPathEditText = ((TextInputEditText) findViewById(R.id.path_edit_text));
        mDeeplinkEditText = ((TextInputEditText) findViewById(R.id.deep_link_edit_text));

        mLaunchPathBtn = ((Button) findViewById(R.id.path_button));
        mLaunchDeeplinkBtn = ((Button) findViewById(R.id.deeplink_button));
        mLaunchPathBtn.setOnClickListener(onClickListener);
        mLaunchDeeplinkBtn.setOnClickListener(onClickListener);
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    private void launchQuickApp(String packageName, String path) {
        if (TextUtils.isEmpty(packageName)) {
            Toast.makeText(this, R.string.toast_no_package, Toast.LENGTH_LONG).show();
            return;
        }
        String url = "hap://app/" + packageName + path;
        launchDeeplink(url);
    }

    private void launchDeeplink(String deeplinkText) {
        if (TextUtils.isEmpty(deeplinkText)) {
            Toast.makeText(this, R.string.toast_no_deeplink, Toast.LENGTH_LONG).show();
            return;
        }

        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(Uri.parse(deeplinkText));
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try {
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
            Log.e(TAG, "launchDeeplink: ", e);
            Toast.makeText(this, R.string.toast_wrong_deeplink, Toast.LENGTH_LONG).show();
        }
    }
}
