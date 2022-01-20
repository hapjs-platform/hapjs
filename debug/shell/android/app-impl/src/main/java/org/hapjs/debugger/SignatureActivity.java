/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.debugger;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.method.ScrollingMovementMethod;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import org.hapjs.debugger.app.impl.R;
import org.hapjs.debugger.signutils.SignatureUtils;

public class SignatureActivity extends AppCompatActivity implements View.OnClickListener {
    private static final String TAG = "SignatureActivity";

    private static final int REQUEST_CODE_PICK_APK = 0;
    private static final int REQUEST_CODE_PICK_INSTALLED_APP = 1;
    private static final int REQUEST_CODE_PICK_RPK = 2;
    private static final int REQUEST_CODE_PICK_PEM = 3;

    private static final int TYPE_APK = 0;
    private static final int TYPE_RPK = 1;
    private static final int TYPE_PEM = 2;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_signature);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        initViews();
    }

    private void initViews() {
        findViewById(R.id.get_signature).setOnClickListener(this);
        findViewById(R.id.pick_apk).setOnClickListener(this);
        findViewById(R.id.pick_installed_app).setOnClickListener(this);
        findViewById(R.id.pick_pem).setOnClickListener(this);
        findViewById(R.id.pick_rpk).setOnClickListener(this);
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.get_signature) {
            getSignatureByPkgName();
        } else if (v.getId() == R.id.pick_apk) {
            pickApk();
        } else if (v.getId() == R.id.pick_installed_app) {
            pickInstalledApp();
        } else if (v.getId() == R.id.pick_pem) {
            pickPem();
        } else if (v.getId() == R.id.pick_rpk) {
            pickRpk();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_CODE_PICK_APK:
                if (resultCode == Activity.RESULT_OK) {
                    getSignatureFromApk(data.getData());
                }
                break;
            case REQUEST_CODE_PICK_INSTALLED_APP:
                if (resultCode == Activity.RESULT_OK) {
                    String pkg = data.getStringExtra(InstalledAppActivity.RESULT_PKG);
                    getSignatureFromInstalledApp(pkg);
                }
                break;
            case REQUEST_CODE_PICK_PEM:
                if (resultCode == Activity.RESULT_OK) {
                    getSignatureFromPem(data.getData());
                }
                break;
            case REQUEST_CODE_PICK_RPK:
                if (resultCode == Activity.RESULT_OK) {
                    getSignatureFromRpk(data.getData());
                }
                break;
            default:
                super.onActivityResult(requestCode, resultCode, data);
        }
    }

    private void pickApk() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        startActivityForResult(intent, REQUEST_CODE_PICK_APK);
    }

    private void pickInstalledApp() {
        Intent intent = new Intent(this, InstalledAppActivity.class);
        startActivityForResult(intent, REQUEST_CODE_PICK_INSTALLED_APP);
    }

    private void pickPem() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        startActivityForResult(intent, REQUEST_CODE_PICK_PEM);
    }

    private void pickRpk() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        startActivityForResult(intent, REQUEST_CODE_PICK_RPK);
    }

    private void getSignatureByPkgName() {
        EditText pkgName = (EditText) findViewById(R.id.pkg_name);
        Editable pkg = pkgName.getText();
        if (TextUtils.isEmpty(pkg)) {
            Toast.makeText(this, R.string.toast_pkg_name_null, Toast.LENGTH_SHORT).show();
            return;
        }

        PackageInfo info = SignatureUtils.getNativePackageInfo(this, pkg.toString());
        showSignature(info);
    }

    private void getSignatureFromApk(Uri uri) {
        PackageInfo info = SignatureUtils.getNativePackageInfo(this, uri);
        showSignature(info);
    }

    private void getSignatureFromPem(Uri uri) {
        String path = uri.getPath();
        if (path != null) {
            if (path.endsWith(".pem") || path.endsWith(".PEM")) {
                byte[] signature = SignatureUtils.getSignatureFromPem(this, uri);
                showSignature(TYPE_PEM, "PEM", signature);
            } else {
                showSignature(TYPE_PEM, "PEM", null);
            }
        } else {
            showSignature(TYPE_PEM, "PEM", null);
        }
    }

    private void getSignatureFromInstalledApp(String pkg) {
        PackageInfo info = SignatureUtils.getNativePackageInfo(this, pkg);
        showSignature(info);
    }

    private void getSignatureFromRpk(Uri uri) {
        org.hapjs.debugger.pm.PackageInfo pi = SignatureUtils.getHybridPackageInfo(this, uri);
        if (pi != null) {
            showSignature(TYPE_RPK, pi.getName(), pi.getSignature(), pi.getPackage());
        } else {
            Toast.makeText(this, R.string.toast_pkg_not_found, Toast.LENGTH_SHORT).show();
        }
    }

    private void showSignature(PackageInfo info) {
        if (info != null) {
            byte[] signature = info.signatures[0].toByteArray();
            CharSequence label = getPackageManager().getApplicationLabel(info.applicationInfo);
            String appName = TextUtils.isEmpty(label) ? info.packageName : label.toString();
            showSignature(TYPE_APK, appName, signature, info.packageName);
        } else {
            Toast.makeText(this, R.string.toast_pkg_not_found, Toast.LENGTH_SHORT).show();
        }
    }

    private void showSignature(int type, String name, byte[] sign) {
        showSignature(type, name, sign, null);
    }

    private void showSignature(int type, String name, byte[] sign, String packageName) {
        if (sign == null) {
            Toast.makeText(this, R.string.toast_get_signature_fail, Toast.LENGTH_SHORT).show();
            return;
        }

        final String md5 = SignatureUtils.getMd5(sign);
        final String sha256 = SignatureUtils.getSha256(sign);
        final String signStr = Base64.encodeToString(sign, Base64.NO_WRAP);

        View content = LayoutInflater.from(this).inflate(R.layout.signature, null);
        ((TextView) content.findViewById(R.id.digest_md5)).setText(md5);
        ((TextView) content.findViewById(R.id.digest_sha256)).setText(sha256);
        ((TextView) content.findViewById(R.id.sign)).setText(signStr);
        ((TextView) content.findViewById(R.id.sign)).setMovementMethod(new ScrollingMovementMethod());
        content.findViewById(R.id.copy_digest_md5).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ClipboardManager cm = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
                ClipData data = new ClipData("md5", new String[] {"text/plain"}, new ClipData.Item(md5));
                cm.setPrimaryClip(data);
                Toast.makeText(SignatureActivity.this, R.string.toast_copy_to_clipboard_succ, Toast.LENGTH_SHORT).show();
            }
        });
        content.findViewById(R.id.copy_digest_sha256).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ClipboardManager cm = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
                ClipData data = new ClipData("sha-256", new String[] {"text/plain"}, new ClipData.Item(sha256));
                cm.setPrimaryClip(data);
                Toast.makeText(SignatureActivity.this, R.string.toast_copy_to_clipboard_succ, Toast.LENGTH_SHORT).show();
            }
        });
        content.findViewById(R.id.copy_sign).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ClipboardManager cm = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
                ClipData data = new ClipData("sign", new String[] {"text/plain"}, new ClipData.Item(signStr));
                cm.setPrimaryClip(data);
                Toast.makeText(SignatureActivity.this, R.string.toast_copy_to_clipboard_succ, Toast.LENGTH_SHORT).show();
            }
        });

        if (type == TYPE_RPK) {
            final String smsHash = SignatureUtils.getSMSHash(packageName, sign);
            content.findViewById(R.id.sms_hash_view).setVisibility(View.VISIBLE);
            ((TextView) content.findViewById(R.id.sms_hash)).setText(smsHash);
            content.findViewById(R.id.copy_sms_hash).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    ClipboardManager cm = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
                    ClipData data = new ClipData("sms_hash", new String[] {"text/plain"}, new ClipData.Item(smsHash));
                    cm.setPrimaryClip(data);
                    Toast.makeText(SignatureActivity.this, R.string.toast_copy_to_clipboard_succ, Toast.LENGTH_SHORT).show();
                }
            });
        }

        new AlertDialog.Builder(this)
                .setView(content)
                .setTitle(name)
                .setNegativeButton(R.string.btn_close, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                }).show();
    }
}
