/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.runtime;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import org.hapjs.common.utils.LogUtils;

public class ExceptionActivity extends AppCompatActivity {
    private static final String ACTION_VIEW_ERROR = "org.hapjs.action.VIEW_ERROR";
    private static final String EXTRA_STACK_TRACE = "stackTrace";

    public static void startExceptionActivity(Context context, Exception e) {
        Intent intent = new Intent();
        intent.setAction(ACTION_VIEW_ERROR);
        intent.putExtra(EXTRA_STACK_TRACE, LogUtils.getStackTrace(e));
        intent.setPackage(context.getPackageName());
        context.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.exception_activity);

        Intent intent = getIntent();
        final String stackTrace = intent.getStringExtra(EXTRA_STACK_TRACE);
        ((TextView) findViewById(R.id.jsExceptionView)).setText(stackTrace);

        View.OnClickListener onClickListener =
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        int id = v.getId();
                        if (id == R.id.btnCopy) {
                            ClipboardManager clipboard =
                                    (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
                            clipboard.setPrimaryClip(ClipData.newPlainText("", stackTrace));
                            Toast.makeText(
                                    ExceptionActivity.this,
                                    R.string.toast_error_detail_copied,
                                    Toast.LENGTH_SHORT)
                                    .show();
                        } else if (id == R.id.btnClose) {
                            finish();
                        }
                    }
                };
        findViewById(R.id.btnCopy).setOnClickListener(onClickListener);
        findViewById(R.id.btnClose).setOnClickListener(onClickListener);
    }
}
